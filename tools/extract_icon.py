import io
import os
import struct
import sys
from PIL import Image
import pefile

EXE_PATH = "raw_assets/Ikachan.exe"
OUT_PNG = "res/icon.png"


def build_ico_bytes(pe, group_entry):
    grp_data = pe.get_data(
        group_entry.data.struct.OffsetToData,
        group_entry.data.struct.Size,
    )
    reserved, res_type, count = struct.unpack_from("<HHH", grp_data, 0)
    if res_type != 1 or count == 0:
        raise ValueError("Invalid RT_GROUP_ICON resource")

    rt_icon_dir = None
    for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
        if entry.id == 3:  # RT_ICON
            rt_icon_dir = entry.directory
            break
    if rt_icon_dir is None:
        raise ValueError("RT_ICON directory not found")

    icon_blobs = {}
    for icon_entry in rt_icon_dir.entries:
        lang_entry = icon_entry.directory.entries[0]
        raw = pe.get_data(
            lang_entry.data.struct.OffsetToData,
            lang_entry.data.struct.Size,
        )
        icon_blobs[icon_entry.id] = raw

    ico_header = struct.pack("<HHH", 0, 1, count)
    dir_entries = bytearray()
    payloads = bytearray()
    cur_offset = 6 + count * 16

    pos = 6
    for _ in range(count):
        if pos + 14 > len(grp_data):
            break
        w, h, colors, res_b, planes, bpp, img_size, icon_id = struct.unpack_from(
            "<BBBBHHIH", grp_data, pos
        )
        pos += 14
        blob = icon_blobs.get(icon_id)
        if blob is None:
            continue
        dir_entries.extend(
            struct.pack("<BBBBHHII", w, h, colors, res_b, planes, bpp, len(blob), cur_offset)
        )
        payloads.extend(blob)
        cur_offset += len(blob)

    return ico_header + bytes(dir_entries) + bytes(payloads)


def main():
    if not os.path.exists(EXE_PATH):
        sys.exit(f"Missing {EXE_PATH}")

    pe = pefile.PE(EXE_PATH)
    if not hasattr(pe, "DIRECTORY_ENTRY_RESOURCE"):
        sys.exit("EXE has no resource directory")

    group_dir = None
    for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
        if entry.id == 14:  # RT_GROUP_ICON
            group_dir = entry.directory
            break

    if group_dir is None:
        sys.exit("No RT_GROUP_ICON found in Ikachan.exe")

    first_group = group_dir.entries[0].directory.entries[0]
    ico_bytes = build_ico_bytes(pe, first_group)

    img = Image.open(io.BytesIO(ico_bytes))
    best = img.convert("RGBA")

    # Series 40 MIDlet list icons work best at 16x16 or 24x24
    if best.width > 32 or best.height > 32:
        best = best.resize((16, 16), Image.Resampling.NEAREST)

    os.makedirs(os.path.dirname(OUT_PNG), exist_ok=True)
    best.save(OUT_PNG, format="PNG", optimize=True)
    print(f"Saved {OUT_PNG} ({best.width}x{best.height}, {os.path.getsize(OUT_PNG)} bytes)")


if __name__ == "__main__":
    main()