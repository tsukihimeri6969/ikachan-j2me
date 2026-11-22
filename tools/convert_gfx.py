import os
import struct
from PIL import Image

PBM_DIR = "raw_assets/Pbm"
OUT_IMG_DIR = "res/img"
OUT_MAP_PATH = "res/map1.bin"

# Full-screen surfaces drawn without black color-key transparency
OPAQUE_SHEETS = {"back.pbm"}

os.makedirs(OUT_IMG_DIR, exist_ok=True)
os.makedirs(os.path.dirname(OUT_MAP_PATH), exist_ok=True)


def pack_map(path):
    img = Image.open(path)
    if img.mode != "P":
        raise ValueError(f"Expected indexed BMP for Map1.pbm, got mode {img.mode}")
    w, h = img.size
    tile_bytes = img.tobytes()
    with open(OUT_MAP_PATH, "wb") as f:
        f.write(struct.pack(">HH", w, h))
        f.write(tile_bytes)
    unique_tiles = len(set(tile_bytes))
    print(f"Map1.pbm -> {OUT_MAP_PATH} ({w}x{h} tiles, {len(tile_bytes) + 4} bytes, {unique_tiles} unique tile IDs)")


def convert_pbm_to_png(src_path, dst_path, opaque=False):
    img = Image.open(src_path)
    if img.mode == "1":
        img = img.convert("P")
    elif img.mode != "P":
        img = img.convert("P", palette=Image.ADAPTIVE, colors=256)

    if opaque:
        img.save(dst_path, format="PNG", optimize=True)
        return

    pal = img.getpalette() or []
    num_colors = len(pal) // 3
    black_indices = [i for i in range(num_colors) if pal[i * 3 : i * 3 + 3] == [0, 0, 0]]

    if not black_indices:
        img.save(dst_path, format="PNG", optimize=True)
        return

    # Move pure black to palette index 0 so PNG tRNS stays a 1-byte entry for Nokia S40
    first_black = black_indices[0]
    remap = list(range(256))
    for b in black_indices:
        remap[b] = 0
    if first_black != 0:
        remap[0] = first_black
        pal[0:3], pal[first_black * 3 : first_black * 3 + 3] = (
            pal[first_black * 3 : first_black * 3 + 3],
            pal[0:3],
        )

    px = img.tobytes()
    remapped = bytes(remap[b] for b in px)
    out = Image.frombytes("P", img.size, remapped)
    out.putpalette(pal)
    out.save(dst_path, format="PNG", optimize=True, transparency=0)


total_png_bytes = 0
png_count = 0

for fname in sorted(os.listdir(PBM_DIR)):
    if not fname.lower().endswith(".pbm"):
        continue
    src = os.path.join(PBM_DIR, fname)
    if fname.lower() == "map1.pbm":
        pack_map(src)
        continue
    out_name = os.path.splitext(fname)[0] + ".png"
    dst = os.path.join(OUT_IMG_DIR, out_name)
    is_opaque = fname.lower() in OPAQUE_SHEETS
    convert_pbm_to_png(src, dst, opaque=is_opaque)
    size = os.path.getsize(dst)
    total_png_bytes += size
    png_count += 1
    print(f"{fname:20s} -> {out_name:20s} ({size:6d} bytes){' [opaque]' if is_opaque else ''}")

print(f"\nConverted {png_count} PNG sheets ({total_png_bytes} bytes total).")