package ikachan;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.Graphics;

public final class BitmapFont {
    private final int glyphCount;
    private final int glyphHeight;
    private final int bytesPerGlyph;
    private final int[] codes;
    private final byte[] widths;
    private final byte[] bitmaps;
    private final int[] scratch;

    public BitmapFont(byte[] fontData) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fontData));
        glyphCount = dis.readUnsignedShort();
        glyphHeight = dis.readUnsignedByte();
        bytesPerGlyph = dis.readUnsignedByte();

        codes = new int[glyphCount];
        for (int i = 0; i < glyphCount; i++) {
            codes[i] = dis.readUnsignedShort();
        }
        widths = new byte[glyphCount];
        dis.readFully(widths);

        bitmaps = new byte[glyphCount * bytesPerGlyph];
        dis.readFully(bitmaps);
        scratch = new int[12 * glyphHeight];
    }

    private int findGlyph(int code) {
        int lo = 0;
        int hi = glyphCount - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int v = codes[mid];
            if (v == code) {
                return mid;
            }
            if (v < code) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    public int drawGlyph(Graphics g, int code, int x, int y, int argb) {
        int idx = findGlyph(code);
        if (idx < 0) {
            return (code > 0xFF) ? 12 : 6;
        }
        int adv = widths[idx] & 0xFF;
        if (code == 0x20 || code == 0x8140) {
            return adv;
        }

        int base = idx * bytesPerGlyph;
        int p = 0;
        for (int row = 0; row < glyphHeight; row++) {
            int mask = ((bitmaps[base + (row << 1)] & 0xFF) << 8)
                | (bitmaps[base + (row << 1) + 1] & 0xFF);
            for (int col = 0; col < adv; col++) {
                scratch[p++] = ((mask & (0x8000 >> col)) != 0) ? argb : 0x00000000;
            }
        }
        g.drawRGB(scratch, 0, adv, x, y, adv, glyphHeight, true);
        return adv;
    }

    public void drawAscii(Graphics g, String s, int x, int y, int argb) {
        int cx = x;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            cx += drawGlyph(g, s.charAt(i) & 0xFF, cx, y, argb);
        }
    }

    public void drawSjisBlock(Graphics g, byte[] data, int off, int len,
                              int x, int y, int maxW, int maxH, int argb) {
        int cx = x;
        int cy = y;
        int end = off + len;
        if (end > data.length) {
            end = data.length;
        }
        int i = off;
        while (i < end && (cy + glyphHeight) <= (y + maxH)) {
            int b1 = data[i] & 0xFF;
            if (b1 == 0x0D) {
                i++;
                continue;
            }
            if (b1 == 0x0A) {
                cx = x;
                cy += glyphHeight + 2;
                i++;
                continue;
            }
            int code;
            int adv;
            if (((b1 >= 0x81 && b1 <= 0x9F) || (b1 >= 0xE0 && b1 <= 0xFC)) && (i + 1 < end)) {
                int b2 = data[i + 1] & 0xFF;
                code = (b1 << 8) | b2;
                adv = 12;
                i += 2;
            } else {
                code = b1;
                adv = 6;
                i++;
            }
            if (cx + adv > x + maxW) {
                cx = x;
                cy += glyphHeight + 2;
                if ((cy + glyphHeight) > (y + maxH)) {
                    break;
                }
            }
            cx += drawGlyph(g, code, cx, cy, argb);
        }
    }
}