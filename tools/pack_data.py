import os
import struct

ENTRIES = [
    ("map1", "res/map1.bin"),
    ("npchar", "raw_assets/NPChar.dat"),
    ("event", "raw_assets/Event.ptx"),
    ("words", "raw_assets/Words.ptx"),
    ("staff", "raw_assets/Staff.ptx"),
    ("font", "res/font.bin"),
]

OUT_PAK = "res/data.pak"


def main():
    payloads = []
    for name, path in ENTRIES:
        with open(path, "rb") as f:
            payloads.append((name, f.read()))

    header_size = 4 + 2 + len(payloads) * 8
    offset = header_size
    table = bytearray()
    for name, blob in payloads:
        table.extend(struct.pack(">II", offset, len(blob)))
        print(f"Slot {name:8s}: {len(blob):6d} bytes @ offset {offset}")
        offset += len(blob)

    os.makedirs(os.path.dirname(OUT_PAK), exist_ok=True)
    with open(OUT_PAK, "wb") as f:
        f.write(b"IPAK")
        f.write(struct.pack(">H", len(payloads)))
        f.write(table)
        for _, blob in payloads:
            f.write(blob)

    print(f"\nWrote {OUT_PAK} ({os.path.getsize(OUT_PAK)} bytes total).")

    map_blob = payloads[0][1]
    w, h = struct.unpack_from(">HH", map_blob, 0)
    tile_ids = sorted(set(map_blob[4:]))
    hex_ids = " ".join(f"{t:02X}" for t in tile_ids)
    print(f"Map1 ({w}x{h}) unique tile IDs ({len(tile_ids)}): {hex_ids}")

    npc_blob = payloads[1][1]
    print(f"NPChar.dat length: {len(npc_blob)} bytes (100 slots x {len(npc_blob) // 100} bytes)")
    for idx in range(2):
        chunk = npc_blob[idx * 22 : (idx + 1) * 22]
        print(f"  Slot {idx}: {chunk.hex(' ')}")


if __name__ == "__main__":
    main()