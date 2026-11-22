package ikachan;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public final class MapEngine {
    public static final int FRAME_MODE_MYCHAR = 0;
    public static final int FRAME_MODE_NPCHAR = 1;
    public static final int FRAME_MODE_BOSS = 2;

    public final int width;
    public final int height;
    public final byte[] data;

    public int frameMode = FRAME_MODE_MYCHAR;
    public int frameNpc = 0;
    public int frameX = 0;
    public int frameY = 0;

    private int vecFx = 0;

    private final Image backImg;
    private final Image[] tileSheets = new Image[8];

    public MapEngine(byte[] mapBlob) {
        width = PakReader.readU16BE(mapBlob, 0);
        height = PakReader.readU16BE(mapBlob, 2);
        data = new byte[width * height];
        System.arraycopy(mapBlob, 4, data, 0, data.length);

        backImg = loadOptional("/img/Back.png");
        tileSheets[1] = loadOptional("/img/Prtfilt.png");
        tileSheets[2] = loadOptional("/img/PrtItem.png");
        tileSheets[3] = loadOptional("/img/PrtDir.png");
        tileSheets[4] = loadOptional("/img/PrtBlock.png");
        tileSheets[5] = loadOptional("/img/PrtDmg.png");
        tileSheets[7] = loadOptional("/img/PrtSnack.png");
    }

    private Image loadOptional(String path) {
        try {
            return Image.createImage(path);
        } catch (Throwable t) {
            return null;
        }
    }

    public int getTile(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) {
            return 0;
        }
        return data[tx + width * ty] & 0xFF;
    }

    public void setTile(int tx, int ty, int tile) {
        if (tx >= 0 && ty >= 0 && tx < width && ty < height) {
            data[tx + width * ty] = (byte) tile;
        }
    }

    public void moveFrameToTarget(int tx, int ty, int viewW, int viewH) {
        int centerX = frameX + (viewW << 9) - 0x2000;
        int centerY = frameY + (viewH << 9) - 0x2000;
        frameX += (tx - centerX) / 16;
        frameY += (ty - centerY) / 16;
        clampFrame(viewW, viewH);
    }

    public void clampFrame(int viewW, int viewH) {
        int maxX = (width - ((viewW + 15) / 16)) << 14;
        int maxY = (height - ((viewH + 15) / 16)) << 14;
        if (maxX < 0) {
            maxX = 0;
        }
        if (maxY < 0) {
            maxY = 0;
        }
        if (frameX < 0) {
            frameX = 0;
        } else if (frameX > maxX) {
            frameX = maxX;
        }
        if (frameY < 0) {
            frameY = 0;
        } else if (frameY > maxY) {
            frameY = maxY;
        }
    }

    public void putBack(Graphics g, int viewW, int viewH) {
        if (backImg == null) {
            g.setColor(0x081838);
            g.fillRect(0, 0, viewW, viewH);
            return;
        }
        int bw = ((viewW + 63) / 64) * 2 + 3;
        int bh = ((viewH + 63) / 64) * 2 + 3;
        int ox = ((2 * (frameX >> 10)) / 3) % 64;
        int oy = ((2 * (frameY >> 10)) / 3) % 64;
        int total = bw * bh;

        for (int i = 0; i < total; i++) {
            int dx = ((i % bw) << 5) - ox;
            int dy = ((i / bw) << 5) - oy;
            int sx = (i & 1) << 5;
            if (sx + 32 <= backImg.getWidth() && 32 <= backImg.getHeight()) {
                g.drawRegion(backImg, sx, 0, 32, 32, Sprite.TRANS_NONE,
                    dx, dy, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    public void putMapBack(Graphics g, int viewW, int viewH) {
        int camPxX = frameX >> 10;
        int camPxY = frameY >> 10;
        int startX = camPxX >> 4;
        int startY = camPxY >> 4;
        int endX = startX + ((viewW + 15) / 16) + 1;
        int endY = startY + ((viewH + 15) / 16) + 1;
        if (startX < 0) startX = 0;
        if (startY < 0) startY = 0;
        if (endX > width) endX = width;
        if (endY > height) endY = height;

        for (int y = startY; y < endY; y++) {
            int rowOff = width * y;
            int dy = (y << 4) - camPxY;
            for (int x = startX; x < endX; x++) {
                int tile = data[rowOff + x] & 0xFF;
                if (tile == 0) {
                    continue;
                }
                int group = tile >> 5;
                if (group == 0 || group == 2 || group == 4 || group == 5 || group == 7) {
                    Image sheet = tileSheets[group];
                    int dx = (x << 4) - camPxX;
                    if (sheet != null) {
                        int sx = (tile & 31) << 4;
                        if (sx + 16 <= sheet.getWidth() && 16 <= sheet.getHeight()) {
                            g.drawRegion(sheet, sx, 0, 16, 16, Sprite.TRANS_NONE,
                                dx, dy, Graphics.TOP | Graphics.LEFT);
                        }
                    } else if (group == 4) {
                        g.setColor(0x3A5A78);
                        g.fillRect(dx, dy, 16, 16);
                    }
                }
            }
        }
    }

    public void putMapFront(Graphics g, int viewW, int viewH) {
        Image sheet = tileSheets[1];
        if (sheet == null) {
            return;
        }
        int sheetW = sheet.getWidth();
        int sheetH = sheet.getHeight();
        int camPxX = frameX >> 10;
        int camPxY = frameY >> 10;
        int startX = camPxX >> 4;
        int startY = camPxY >> 4;
        int endX = startX + ((viewW + 15) / 16) + 1;
        int endY = startY + ((viewH + 15) / 16) + 1;
        if (startX < 0) startX = 0;
        if (startY < 0) startY = 0;
        if (endX > width) endX = width;
        if (endY > height) endY = height;

        for (int y = startY; y < endY; y++) {
            int rowOff = width * y;
            int dy = (y << 4) - camPxY;
            for (int x = startX; x < endX; x++) {
                int tile = data[rowOff + x] & 0xFF;
                if (tile != 0 && (tile >> 5) == 1) {
                    int sx = (tile & 31) << 4;
                    if (sx + 16 <= sheetW && 16 <= sheetH) {
                        g.drawRegion(sheet, sx, 0, 16, 16, Sprite.TRANS_NONE,
                            (x << 4) - camPxX, dy, Graphics.TOP | Graphics.LEFT);
                    }
                }
            }
        }
    }

    public void putMapVector(Graphics g, int viewW, int viewH) {
        vecFx += 2;
        if (vecFx >= 16) {
            vecFx = 0;
        }

        Image sheet = tileSheets[3];
        if (sheet == null) {
            return;
        }
        int sheetW = sheet.getWidth();
        int sheetH = sheet.getHeight();
        int camPxX = frameX >> 10;
        int camPxY = frameY >> 10;
        int startX = camPxX >> 4;
        int startY = camPxY >> 4;
        int endX = startX + ((viewW + 15) / 16) + 1;
        int endY = startY + ((viewH + 15) / 16) + 1;
        if (startX < 0) startX = 0;
        if (startY < 0) startY = 0;
        if (endX > width) endX = width;
        if (endY > height) endY = height;

        for (int y = startY; y < endY; y++) {
            int rowOff = width * y;
            int dy = (y << 4) - camPxY;
            for (int x = startX; x < endX; x++) {
                int tile = data[rowOff + x] & 0xFF;
                if (tile != 0 && (tile >> 5) == 3) {
                    int dir = tile & 31;
                    int sx;
                    int sy;
                    if (dir == 0) {
                        sx = vecFx;
                        sy = 0;
                    } else if (dir == 1) {
                        sx = 16 - vecFx;
                        sy = 0;
                    } else if (dir == 2) {
                        sx = 0;
                        sy = vecFx;
                    } else if (dir == 3) {
                        sx = 0;
                        sy = 16 - vecFx;
                    } else {
                        continue;
                    }
                    if (sx >= 0 && sy >= 0 && sx + 16 <= sheetW && sy + 16 <= sheetH) {
                        g.drawRegion(sheet, sx, sy, 16, 16, Sprite.TRANS_NONE,
                            (x << 4) - camPxX, dy, Graphics.TOP | Graphics.LEFT);
                    }
                }
            }
        }
    }
}