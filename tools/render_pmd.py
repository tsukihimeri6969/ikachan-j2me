import array
import glob
import math
import os
import struct
import wave

PMD_DIR = "raw_assets/Pmd"
SFX_RAW_DIR = "raw_assets/sfx_raw"
OUT_WAV_DIR = "build/bgm_wav"

# Synthesis constants (check against Sound.cpp if pitch or balance needs tweaking)
SAMPLE_RATE = 22050
BASE_C_HZ = 65.40639132514966  # C2 when track.octave == 0 (1 << 0 == 1)

# Exact 24-key percussion mapping from InitPiyoPiyo() (sound IDs 472..495)
DRUM_MAP = [
    "BASS1", "BASS1",
    "BASS2", "BASS2",
    "SNARE1", "SNARE1", "SNARE1", "SNARE1",
    "HAT1", "HAT1",
    "HAT2", "HAT2",
    "SYMBAL1", "SYMBAL1", "SYMBAL1", "SYMBAL1",
    "SYMBAL1", "SYMBAL1", "SYMBAL1", "SYMBAL1",
    "SYMBAL1", "SYMBAL1", "SYMBAL1", "SYMBAL1",
]


def volume_to_gain(vol):
    if vol <= 0:
        return 0.0
    # Standard DirectSound millibel curve used by Studio Pixel (300 = 0 dB)
    db = (min(vol, 300) - 300) * 0.08
    return 10.0 ** (db / 20.0)


def load_wav_mono(path, target_rate):
    with wave.open(path, "rb") as wf:
        ch = wf.getnchannels()
        sw = wf.getsampwidth()
        sr = wf.getframerate()
        raw = wf.readframes(wf.getnframes())

    if sw == 1:
        samples = [(b - 128) / 128.0 for b in raw]
    elif sw == 2:
        arr = array.array("h")
        arr.frombytes(raw)
        samples = [s / 32768.0 for s in arr]
    else:
        raise ValueError(f"Unsupported sample width {sw} in {path}")

    if ch > 1:
        samples = [
            sum(samples[i : i + ch]) / ch
            for i in range(0, len(samples), ch)
        ]

    if sr == target_rate or not samples:
        return samples

    ratio = sr / float(target_rate)
    out_len = int(len(samples) / ratio)
    resampled = [0.0] * out_len
    last = len(samples) - 1
    for i in range(out_len):
        pos = i * ratio
        idx = int(pos)
        frac = pos - idx
        if idx >= last:
            resampled[i] = samples[last]
        else:
            resampled[i] = samples[idx] * (1.0 - frac) + samples[idx + 1] * frac
    return resampled


def make_melody_buffers(track):
    octave_mul = 1 << track["octave"]
    length = track["length"]
    wave_tbl = track["wave"]
    env_tbl = track["envelope"]
    gain = volume_to_gain(track["volume"])

    buffers = []
    for key in range(24):
        freq = BASE_C_HZ * octave_mul * (2.0 ** (key / 12.0))
        step = freq * 256.0 / SAMPLE_RATE
        buf = [0.0] * length
        phase = 0.0
        for s in range(length):
            env = env_tbl[(s * 64) // length] / 128.0
            sample = wave_tbl[int(phase) & 0xFF] / 128.0
            buf[s] = sample * env * gain
            phase += step
        buffers.append(buf)
    return buffers


def parse_pmd(path):
    with open(path, "rb") as f:
        data = f.read()

    magic, writable, p_track1, wait, repeat_x, end_x, records = struct.unpack_from(
        "<3sBIIiii", data, 0
    )
    if magic != b"PMD":
        raise ValueError(f"{path}: bad magic {magic!r}")

    tracks = []
    offset = 0x18
    for _ in range(3):
        octave, icon, length, volume, unk1, unk2 = struct.unpack_from(
            "<BB2xIIII", data, offset
        )
        wave_tbl = struct.unpack_from("<256b", data, offset + 0x14)
        env_tbl = struct.unpack_from("<64B", data, offset + 0x114)
        tracks.append({
            "octave": octave,
            "icon": icon,
            "length": length,
            "volume": volume,
            "wave": wave_tbl,
            "envelope": env_tbl,
        })
        offset += 0x154

    (perc_vol,) = struct.unpack_from("<I", data, 0x414)
    offset = 0x418

    rec_arrays = []
    for _ in range(4):
        rec_arrays.append(struct.unpack_from(f"<{records}I", data, offset))
        offset += records * 4

    return {
        "wait": wait,
        "repeat_x": repeat_x,
        "end_x": end_x,
        "records": records,
        "tracks": tracks,
        "perc_vol": perc_vol,
        "rec_arrays": rec_arrays,
    }


def render_song(pmd_path, drum_cache):
    song = parse_pmd(pmd_path)
    stop_x = min(song["end_x"], song["records"])
    if stop_x <= 0:
        stop_x = song["records"]
    repeat_x = max(0, min(song["repeat_x"], stop_x - 1))

    # Build all 96 sound buffers matching IDs 400..495
    sound_bank = []
    for t in song["tracks"]:
        sound_bank.extend(make_melody_buffers(t))

    perc_vol = song["perc_vol"]
    for j, name in enumerate(DRUM_MAP):
        v = perc_vol if (j % 2 == 0) else (70 * perc_vol // 100)
        g = volume_to_gain(v)
        sound_bank.append([s * g for s in drum_cache[name]])

    step_samples = SAMPLE_RATE * (song["wait"] / 1000.0)
    total_samples = int(round(stop_x * step_samples))
    loop_start_sample = int(round(repeat_x * step_samples))
    mix = [0.0] * total_samples

    # Collect triggers per slot so retriggering cuts the previous hit on that same key
    slot_triggers = [[] for _ in range(96)]
    for pos in range(stop_x):
        start_sample = int(round(pos * step_samples))
        for ch in range(4):
            rec = song["rec_arrays"][ch][pos] & 0x00FFFFFF
            if not rec:
                continue
            base_slot = ch * 24
            for key in range(24):
                if (rec >> key) & 1:
                    slot_triggers[base_slot + key].append(start_sample)

    for slot, triggers in enumerate(slot_triggers):
        if not triggers:
            continue
        buf = sound_bank[slot]
        buf_len = len(buf)
        for idx, start in enumerate(triggers):
            next_start = triggers[idx + 1] if idx + 1 < len(triggers) else None
            limit = buf_len
            if next_start is not None and (next_start - start) < limit:
                limit = next_start - start

            end = start + limit
            if end <= total_samples:
                for i in range(limit):
                    mix[start + i] += buf[i]
            else:
                first_part = total_samples - start
                for i in range(first_part):
                    mix[start + i] += buf[i]
                # Wrap tail back into the loop start when the song loops from 0
                if repeat_x == 0:
                    rem = limit - first_part
                    for i in range(rem):
                        dst = (loop_start_sample + i) % total_samples
                        mix[dst] += buf[first_part + i]

    peak = max((abs(s) for s in mix), default=1.0)
    scale = 0.9 / peak if peak > 0.9 else 1.0
    pcm = array.array(
        "h",
        (max(-32767, min(32767, int(s * scale * 32767.0))) for s in mix),
    )

    out_name = os.path.splitext(os.path.basename(pmd_path))[0].lower() + ".wav"
    out_path = os.path.join(OUT_WAV_DIR, out_name)
    with wave.open(out_path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(pcm.tobytes())

    t_info = " | ".join(
        f"T{i}(oct={t['octave']},len={t['length']},vol={t['volume']})"
        for i, t in enumerate(song["tracks"])
    )
    dur = total_samples / float(SAMPLE_RATE)
    print(
        f"{os.path.basename(pmd_path):14s} -> {out_name:14s} "
        f"{dur:5.1f}s | wait={song['wait']}ms loop={repeat_x}..{stop_x}/{song['records']} "
        f"perc={perc_vol} | {t_info}"
    )


def main():
    os.makedirs(OUT_WAV_DIR, exist_ok=True)
    drum_cache = {}
    for name in set(DRUM_MAP):
        wav_path = os.path.join(SFX_RAW_DIR, f"{name}.wav")
        drum_cache[name] = load_wav_mono(wav_path, SAMPLE_RATE)

    for pmd_path in sorted(glob.glob(os.path.join(PMD_DIR, "*.pmd"))):
        render_song(pmd_path, drum_cache)


if __name__ == "__main__":
    main()