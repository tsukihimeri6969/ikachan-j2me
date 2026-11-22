import os
import shutil
import subprocess
import sys

SFX_RAW_DIR = "raw_assets/sfx_raw"
BGM_WAV_DIR = "build/bgm_wav"
OUT_SFX_DIR = "res/audio/sfx"
OUT_BGM_DIR = "res/audio/bgm"

DRUM_SAMPLES = {
    "bass1.wav",
    "bass2.wav",
    "hat1.wav",
    "hat2.wav",
    "snare1.wav",
    "symbal1.wav",
}


def encode_amr(src_wav, dst_amr):
    cmd = [
        "ffmpeg",
        "-y",
        "-loglevel",
        "error",
        "-i",
        src_wav,
        "-acodec",
        "libopencore_amrnb",
        "-ar",
        "8000",
        "-ac",
        "1",
        "-ab",
        "12.2k",
        dst_amr,
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        sys.exit(f"ffmpeg failed on {src_wav}:\n{res.stderr}")


def convert_dir(src_dir, dst_dir, skip_set=None):
    if not os.path.isdir(src_dir):
        return 0, 0
    os.makedirs(dst_dir, exist_ok=True)
    count = 0
    total = 0
    for fname in sorted(os.listdir(src_dir)):
        if not fname.lower().endswith(".wav"):
            continue
        if skip_set and fname.lower() in skip_set:
            continue
        src = os.path.join(src_dir, fname)
        out_name = os.path.splitext(fname)[0].lower() + ".amr"
        dst = os.path.join(dst_dir, out_name)
        encode_amr(src, dst)
        size = os.path.getsize(dst)
        total += size
        count += 1
        print(f"{fname:16s} -> {out_name:16s} ({size:6d} bytes)")
    return count, total


def main():
    if not shutil.which("ffmpeg"):
        sys.exit("ffmpeg not found on PATH. Install a full ffmpeg build with libopencore_amrnb.")

    sfx_count, sfx_bytes = convert_dir(SFX_RAW_DIR, OUT_SFX_DIR, skip_set=DRUM_SAMPLES)
    print(f"Encoded {sfx_count} gameplay SFX ({sfx_bytes} bytes total).\n")

    bgm_count, bgm_bytes = convert_dir(BGM_WAV_DIR, OUT_BGM_DIR)
    if bgm_count:
        print(f"Encoded {bgm_count} BGM tracks ({bgm_bytes} bytes total).")
    else:
        print(f"No BGM WAVs in {BGM_WAV_DIR} yet (render the 6 .pmd tracks there next).")


if __name__ == "__main__":
    main()