package ikachan;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public final class OpeningScreen {
    private int titleY;
    private int borderX;
    private int borderY;
    private int pixelX;

    private final Image imgOpening;
    private final Image imgStaff;
    private final byte[] wordsData;
    private final byte[] staffData;

    public OpeningScreen(byte[] wordsData, byte[] staffData) {
        this.wordsData = wordsData;
        this.staffData = staffData;
        imgOpening = loadOptional("/img/Opening.png");
        imgStaff   = loadOptional("/img/Staff.png");
        init();
    }

    private Image loadOptional(String path) {
        try {
            return Image.createImage(path);
        } catch (IOException e) {
            return null;
        }
    }

    public void init() {
        titleY = -83;
        borderX = -128;
        borderY = -64;
        pixelX = 320;
    }

    public void resetPixelX(int viewW) {
        pixelX = viewW;
    }

    public void updateAndDrawOpening(Graphics g, int viewW, int viewH) {
        if (pixelX > viewW + 16 && titleY == -83) {
            pixelX = viewW;
        }

        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, viewW, viewH);

        if (imgOpening != null) {
            int sw = imgOpening.getWidth();
            int sh = imgOpening.getHeight();

            // Title logo (48, 0, 200, 64)
            drawRegionSafe(g, imgOpening, 48, 0, 200, 64,
                (viewW / 2) - 95, titleY, sw, sh);

            // Crab (248, 0, 72, 72)
            drawRegionSafe(g, imgOpening, 248, 0, 72, 72,
                (viewW / 2) - 28, (viewH / 2) + 17, sw, sh);

            // Studio Pixel banner (48, 64, 200, 24)
            drawRegionSafe(g, imgOpening, 48, 64, 200, 24,
                pixelX, viewH - 35, sw, sh);

            // Scrolling side borders: rcLeft (0,0,48,64) and rcRight (0,64,48,64)
            int count = ((viewH + 63) / 64) + 1;
            int leftDx = borderX / 2;
            int rightDx = (viewW - 48) - (borderX / 2);
            for (int i = 0; i < count; i++) {
                int dy = borderY + (i << 6);
                drawRegionSafe(g, imgOpening, 0, 0, 48, 64, leftDx, dy, sw, sh);
                drawRegionSafe(g, imgOpening, 0, 64, 48, 64, rightDx, dy, sw, sh);
            }

            if (pixelX <= ((viewW / 2) - 96)) {
                if (((borderY / 32) % 2) != 0) {
                    drawRegionSafe(g, imgOpening, 248, 72, 72, 8,
                        (viewW / 2) - 32, (viewH / 2) + 6, sw, sh);
                }
            }
        }

        borderY++;
        if (titleY < 45) {
            titleY++;
        }
        if (borderY >= 0) {
            borderY -= 64;
        }
        if ((borderX / 2) < 0) {
            borderX++;
        }
        if (pixelX > ((viewW / 2) - 96)) {
            pixelX -= 2;
        }
    }

    public void drawIntro(Graphics g, BitmapFont font, int viewW, int viewH) {
        g.setColor(0x00FFFF);
        g.fillRect(0, 0, viewW, viewH);
        if (font != null && wordsData != null && wordsData.length > 0) {
            font.drawSjisBlock(g, wordsData, 0, wordsData.length,
                12, 16, viewW - 24, viewH - 48, 0xFF000000);
            font.drawAscii(g, "5/9:NEXT", viewW - 60, viewH - 18, 0xFF000000);
        }
    }

    public void drawStaff(Graphics g, BitmapFont font, int viewW, int viewH) {
        g.setColor(0x000000);
        g.fillRect(0, 0, viewW, viewH);
        if (imgStaff != null) {
            int sw = imgStaff.getWidth();
            int sh = imgStaff.getHeight();
            int dx = (viewW - sw) / 2;
            int dy = (viewH - sh) / 2;
            if (dx < 0) dx = 0;
            if (dy < 0) dy = 0;
            g.drawRegion(imgStaff, 0, 0, sw, sh, Sprite.TRANS_NONE,
                dx, dy, Graphics.TOP | Graphics.LEFT);
        }
        if (font != null && staffData != null && staffData.length > 0) {
            font.drawSjisBlock(g, staffData, 0, staffData.length,
                10, 12, viewW - 20, viewH - 24, 0xFFFFFFFF);
        }
    }

    private void drawRegionSafe(Graphics g, Image img, int sx, int sy, int w, int h,
                                int dx, int dy, int imgW, int imgH) {
        if (sx < 0 || sy < 0 || sx + w > imgW || sy + h > imgH) {
            return;
        }
        g.drawRegion(img, sx, sy, w, h, Sprite.TRANS_NONE,
            dx, dy, Graphics.TOP | Graphics.LEFT);
    }
}