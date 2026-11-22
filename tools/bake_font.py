import argparse
import os
import struct
import sys
from PIL import Image, ImageDraw, ImageFont

PTX_FILES = [
    "raw_assets/Event.ptx",
    "raw_assets/Words.ptx",
    "raw_assets/Staff.ptx",
]
OUT_FONT_BIN = "res/font.bin"
OUT_PREVIEW = "build/font_preview.png"
MSGBOX_PNG = "res/img/Msgbox.png"

FONT_CANDIDATES = [
    "tools/msgothic.ttc",
    "tools/misaki_gothic.ttf",
    r"C:\Windows\Fonts\msgothic.ttc",
    r"C:\Windows\Fonts\BIZ-UDGothicR.ttc",
    r"C:\Windows\Fonts\YuGothM.ttc",
    r"C:\Windows\Fonts\meiryo.ttc",
]

GLYPH_H = 12


def find_font(explicit_path):
    if explicit_path:
        if not os.path.exists(explicit_path):
            sys.exit(f"Font not found: {explicit_path}")
        return explicit_path
    for path in FONT_CANDIDATES:
        if os.path.exists(path):
            return path
    sys.exit(
        "No Japanese font found automatically. Pass one explicitly with: "
        "python tools/bake_font.py --font path/to/font.ttf"
    )


def collect_sjis_codes(paths):
    codes = set(range(0x20, 0x7F))
    for path in paths:
        with open(path, "rb") as f:
            data = f.read()
        i = 0
        n = len(data)
        while i < n:
            b1 = data[i]
            if (0x20 <= b1 <= 0x7E) or (0xA1 <= b1 <= 0xDF):
                codes.add(b1)
                i += 1
            elif ((0x81 <= b1 <= 0x9F) or (0xE0 <= b1 <= 0xFC)) and (i + 1 < n):
                b2 = data[i + 1]
                if (0x40 <= b2 <= 0x7E) or (0x80 <= b2 <= 0xFC):
                    codes.add((b1 << 8) | b2)
                    i += 2
                else:
                    i += 1
            else:
                i += 1
    return sorted(codes)


def render_glyph(font, ch, adv_w):
    scratch = Image.new("1", (16, 16), 0)
    draw = ImageDraw.Draw(scratch)
    draw.fontmode = "1"
    draw.text((0, 0), ch, fill=1, font=font)

    bbox = scratch.getbbox()
    cell = Image.new("1", (12, GLYPH_H), 0)
    if bbox is not None:
        left, top, right, bottom = bbox
        y_shift = 0
        if bottom > GLYPH_H:
            y_shift = max(-top, GLYPH_H - bottom)
        x_shift = 0
        if right > adv_w:
            x_shift = max(-left, adv_w - right)
        cell.paste(scratch, (x_shift, y_shift))

    rows = bytearray()
    for y in range(GLYPH_H):
        mask = 0
        for x in range(12):
            if cell.getpixel((x, y)):
                mask |= (0x8000 >> x)
        rows.extend(struct.pack(">H", mask))
    return bytes(rows), cell


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--font", help="Path to a .ttf/.ttc/.otf Japanese font")
    parser.add_argument("--size", type=int, default=12, help="Pixel size (default: 12)")
    args = parser.parse_args()

    font_path = find_font(args.font)
    font = ImageFont.truetype(font_path, args.size, index=0)
    codes = collect_sjis_codes(PTX_FILES)

    os.makedirs(os.path.dirname(OUT_FONT_BIN), exist_ok=True)
    os.makedirs(os.path.dirname(OUT_PREVIEW), exist_ok=True)

    valid_codes = []
    widths = []
    bitmaps = []
    cells = []

    half_count = 0
    full_count = 0

    for code in codes:
        raw = bytes([code]) if code <= 0xFF else bytes([code >> 8, code & 0xFF])
        try:
            ch = raw.decode("cp932")
        except UnicodeDecodeError:
            continue
        adv_w = 6 if code <= 0xFF else 12
        if adv_w == 6:
            half_count += 1
        else:
            full_count += 1
        bmp, cell = render_glyph(font, ch, adv_w)
        valid_codes.append(code)
        widths.append(adv_w)
        bitmaps.append(bmp)
        cells.append(cell)

    # font.bin layout (all big-endian):
    #   uint16 glyphCount
    #   uint8  glyphHeight (12)
    #   uint8  bytesPerGlyph (24)
    #   uint16[glyphCount] sorted CP932 codes
    #   uint8[glyphCount]  advance widths (6 or 12)
    #   uint8[glyphCount * 24] 1-bit row masks (12 x uint16 per glyph)
    with open(OUT_FONT_BIN, "wb") as f:
        f.write(struct.pack(">HBB", len(valid_codes), GLYPH_H, GLYPH_H * 2))
        for code in valid_codes:
            f.write(struct.pack(">H", code))
        f.write(bytes(widths))
        for bmp in bitmaps:
            f.write(bmp)

    cols = 32
    rows_grid = (len(cells) + cols - 1) // cols
    pad = 2
    step = 12 + pad
    preview = Image.new("RGB", (cols * step + pad, rows_grid * step + pad), (12, 20, 48))
    for idx, cell in enumerate(cells):
        gx = (idx % cols) * step + pad
        gy = (idx // cols) * step + pad
        for y in range(GLYPH_H):
            for x in range(12):
                if cell.getpixel((x, y)):
                    preview.putpixel((gx + x, gy + y), (255, 255, 255))
    preview.save(OUT_PREVIEW)

    bin_size = os.path.getsize(OUT_FONT_BIN)
    print(f"Font used: {font_path} @ {args.size}px")
    print(f"Baked {len(valid_codes)} glyphs ({half_count} half-width, {full_count} full-width)")
    print(f"Wrote {OUT_FONT_BIN} ({bin_size} bytes) and {OUT_PREVIEW}")

    if os.path.exists(MSGBOX_PNG):
        with Image.open(MSGBOX_PNG) as mb:
            print(f"Msgbox.png dimensions: {mb.size[0]}x{mb.size[1]}")


if __name__ == "__main__":
    main()