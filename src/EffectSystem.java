package ikachan;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public final class EffectSystem {
    public static final int MAX_CARETS = 120;
    public static final int MAX_SPAWNERS = 10;

    public static final int TYPE_STAR = 0;
    public static final int TYPE_BUBBLE = 1;
    public static final int TYPE_DAMAGE = 2;
    public static final int TYPE_LEVELUP = 3;

    private final boolean[] cCond = new boolean[MAX_CARETS];
    private final int[] cType = new int[MAX_CARETS];
    private final int[] cX = new int[MAX_CARETS];
    private final int[] cY = new int[MAX_CARETS];
    private final int[] cXm = new int[MAX_CARETS];
    private final int[] cYm = new int[MAX_CARETS];
    private final int[] cAniNo = new int[MAX_CARETS];
    private final int[] cAniWait = new int[MAX_CARETS];

    private final boolean[] sCond = new boolean[MAX_SPAWNERS];
    private final int[] sType = new int[MAX_SPAWNERS];
    private final int[] sAniNo = new int[MAX_SPAWNERS];
    private final int[] sNum = new int[MAX_SPAWNERS];
    private final int[] sX = new int[MAX_SPAWNERS];
    private final int[] sY = new int[MAX_SPAWNERS];
    private final int[] sMoveRight = new int[MAX_SPAWNERS];
    private final int[] sMoveLeft = new int[MAX_SPAWNERS];
    private final int[] sMoveDown = new int[MAX_SPAWNERS];
    private final int[] sMoveUp = new int[MAX_SPAWNERS];
    private final int[] sRandX = new int[MAX_SPAWNERS];
    private final int[] sRandY = new int[MAX_SPAWNERS];

    private final Image[] sheets = new Image[4];
    private int rngState = 0x13579BDF;

    public EffectSystem() {
        sheets[TYPE_STAR]    = loadOptional("/img/Star.png");
        sheets[TYPE_BUBBLE]  = loadOptional("/img/Bubble.png");
        sheets[TYPE_DAMAGE]  = loadOptional("/img/Damage.png");
        sheets[TYPE_LEVELUP] = loadOptional("/img/LevelUp.png");
    }

    private Image loadOptional(String path) {
        try {
            return Image.createImage(path);
        } catch (IOException e) {
            return null;
        }
    }

    public void clear() {
        for (int i = 0; i < MAX_CARETS; i++) {
            cCond[i] = false;
        }
        for (int i = 0; i < MAX_SPAWNERS; i++) {
            sCond[i] = false;
        }
    }

    public int random(int min, int max) {
        if (max <= min) {
            return min;
        }
        rngState = rngState * 1103515245 + 12345;
        int r = (rngState >>> 1) & 0x3FFFFFFF;
        return min + (r % (max - min + 1));
    }

    public boolean addCaret(int type, int x, int y, int xm, int ym, int aniNo) {
        for (int i = 0; i < MAX_CARETS; i++) {
            if (!cCond[i]) {
                cCond[i] = true;
                cType[i] = type;
                cX[i] = x;
                cY[i] = y;
                cXm[i] = xm;
                cYm[i] = ym;
                cAniNo[i] = aniNo;
                cAniWait[i] = 0;
                return true;
            }
        }
        return false;
    }

    public void addSpawner(int type, int aniNo, int num, int x, int y,
                           int moveLeft, int moveRight, int moveUp, int moveDown,
                           int randX, int randY) {
        for (int i = 0; i < MAX_SPAWNERS; i++) {
            if (!sCond[i]) {
                sCond[i] = true;
                sType[i] = type;
                sAniNo[i] = aniNo;
                sNum[i] = num;
                sX[i] = x;
                sY[i] = y;
                sMoveLeft[i] = moveLeft;
                sMoveRight[i] = moveRight;
                sMoveUp[i] = moveUp;
                sMoveDown[i] = moveDown;
                sRandX[i] = randX;
                sRandY[i] = randY;
                return;
            }
        }
    }

    public void update(MapEngine map) {
        for (int i = 0; i < MAX_SPAWNERS; i++) {
            if (sCond[i]) {
                sCond[i] = false;
                int count = sNum[i];
                for (int j = 0; j < count; j++) {
                    int cx = sX[i] + (random(-sRandX[i], sRandX[i]) << 10);
                    int cy = sY[i] + (random(-sRandY[i], sRandY[i]) << 10);
                    int cxm = random(sMoveLeft[i], sMoveRight[i]);
                    int cym = random(sMoveUp[i], sMoveDown[i]);
                    addCaret(sType[i], cx, cy, cxm, cym, sAniNo[i]);
                }
            }
        }

        int maxX = map.width << 14;
        int maxY = map.height << 14;

        for (int i = 0; i < MAX_CARETS; i++) {
            if (!cCond[i]) {
                continue;
            }
            int type = cType[i];
            if (type == TYPE_STAR) {
                cYm[i] += 32;
                cX[i] += cXm[i];
                cY[i] += cYm[i];
                if (++cAniWait[i] > 8) {
                    cAniWait[i] = 0;
                    if (++cAniNo[i] > 5) {
                        cCond[i] = false;
                        continue;
                    }
                }
            } else if (type == TYPE_BUBBLE) {
                cYm[i] -= 16;
                cX[i] += cXm[i];
                cY[i] += cYm[i];
                if (++cAniWait[i] > 32) {
                    cAniWait[i] = 0;
                    if (++cAniNo[i] > 5) {
                        cCond[i] = false;
                        continue;
                    }
                }
            } else if (type == TYPE_DAMAGE) {
                cY[i] -= 342;
                if (++cAniWait[i] > 72) {
                    cCond[i] = false;
                    continue;
                }
            } else if (type == TYPE_LEVELUP) {
                cY[i] -= 342;
                if (++cAniWait[i] > 3) {
                    cAniWait[i] = 0;
                    if (++cAniNo[i] > 24) {
                        cCond[i] = false;
                        continue;
                    }
                }
            }

            if (cX[i] > maxX || cX[i] < 0 || cY[i] > maxY || cY[i] < 0) {
                cCond[i] = false;
            }
        }
    }

    public void draw(Graphics g, MapEngine map, int viewW, int viewH) {
        int fx = map.frameX;
        int fy = map.frameY;

        for (int i = 0; i < MAX_CARETS; i++) {
            if (!cCond[i]) {
                continue;
            }
            int type = cType[i];
            Image sheet = sheets[type];
            if (sheet == null) {
                continue;
            }

            int dx = (cX[i] - fx) / 0x400;
            int dy = (cY[i] - fy) / 0x400;

            if (type == TYPE_LEVELUP) {
                int drawX = dx - 32;
                int drawY = dy - 4;
                if (drawX + 64 <= 0 || drawX >= viewW || drawY + 16 <= 0 || drawY >= viewH) {
                    continue;
                }
                int sy = (cAniNo[i] & 1) << 4;
                g.drawRegion(sheet, 0, sy, 64, 16, Sprite.TRANS_NONE,
                    drawX, drawY, Graphics.TOP | Graphics.LEFT);
            } else {
                int drawX = dx - 4;
                int drawY = dy - 4;
                if (drawX + 8 <= 0 || drawX >= viewW || drawY + 8 <= 0 || drawY >= viewH) {
                    continue;
                }
                int ani = cAniNo[i];
                if (ani < 0 || ani >= 20) {
                    continue;
                }
                int sx = (ani % 10) << 3;
                int sy = (ani / 10) << 3;
                if (sx + 8 <= sheet.getWidth() && sy + 8 <= sheet.getHeight()) {
                    g.drawRegion(sheet, sx, sy, 8, 8, Sprite.TRANS_NONE,
                        drawX, drawY, Graphics.TOP | Graphics.LEFT);
                }
            }
        }
    }
}