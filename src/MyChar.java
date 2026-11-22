package ikachan;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public final class MyChar {
    public static final int KEY_LEFT  = 1;
    public static final int KEY_RIGHT = 2;
    public static final int KEY_UP    = 4;
    public static final int KEY_DOWN  = 8;
    public static final int KEY_Z     = 16;

    public static final int MAX_LEVEL = 6;
    public static final short[] MYC_LIFE = { 4, 8, 12, 18, 26, 34, 62 };
    public static final short[] MYC_EXP  = { 8, 28, 52, 74, 102, 360, 852 };

    private static final int[] SWIM_XM = { -0x200, 0x200, 0 };
    private static final int[] SWIM_YM = { -0x200, -0x200, -0x2D4 };
    private static final int[] DASH_XM = { -0xC00, 0xC00, 0 };
    private static final int[] DASH_YM = { 0, 0, -0xC00 };

    private static final int[] OFF_X = { 0, 1, 0, 1 };
    private static final int[] OFF_Y = { 0, 0, 1, 1 };
    private static final int[] FLAG_1 = { 0, 0, 1, 1 };
    private static final int[] FLAG_2 = { 1 | 2, 4 | 2, 8 | 1, 8 | 4 };

    public boolean cond;
    public boolean dead;
    public int unit;
    public int equip;
    public int x;
    public int y;
    public int xm;
    public int ym;
    public int aniNo;
    public int aniWait;
    public int shock;
    public int noEvent;
    public int life;
    public int exp;
    public int level;
    public int swimWait;
    public int dashWait;
    public int direct;
    public boolean airborne;
    public int flag;
    public int carry;

    private final Image imgHelmet;
    private final Image imgNormal;
    private final Image imgShip;
    private final Image imgStatus;
    private final Image imgFigure;

    public MyChar() {
        imgHelmet = loadOptional("/img/MyChar.png");
        imgNormal = loadOptional("/img/MyChar2.png");
        imgShip   = loadOptional("/img/MyChar3.png");
        imgStatus = loadOptional("/img/Status.png");
        imgFigure = loadOptional("/img/Figure.png");
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
        cond = true;
        equip = 0;
        dead = false;
        level = 0;
        life = MYC_LIFE[0];
        exp = 0;
        x = 0xA0000;
        y = 0x1A0000;
        xm = 0;
        ym = 0;
        airborne = true;
        aniWait = 0;
        aniNo = 0;
        direct = 0;
        flag = 0;
        unit = 0;
        shock = 0;
        noEvent = 100;
        dashWait = 0;
        swimWait = 0;
        carry = 0;
    }

    public void damage(int dmg, AudioManager audio, EffectSystem fx) {
        if (shock == 0) {
            shock = 100;
            life -= dmg;
            if (life < 0) {
                life = 0;
            }

            fx.addSpawner(EffectSystem.TYPE_DAMAGE, 10 - dmg, 1,
                x + 0x2000, y - 0x1000, 0, 0, 0, 0, 1, 0);

            if (life != 0) {
                audio.playSfx(AudioManager.SFX_OUCH);
            } else {
                audio.playSfx(AudioManager.SFX_DEAD);
                cond = false;
                dead = true;
                fx.addSpawner(EffectSystem.TYPE_STAR, 0, 30,
                    x + 0x2000, y + 0x2000, -0xC00, 0xC00, -0xC00, 0x200, 1, 0);
            }
        }
    }

    public void act(int key, int keyTrg, AudioManager audio, EffectSystem fx) {
        if (!cond) {
            return;
        }
        if (unit == 0) {
            actNormal(key, keyTrg, audio, fx);
        } else if (unit == 1) {
            actDash(fx);
        } else if (unit == 2) {
            actShip(fx);
        }
    }

    private void spawnDirectedBubble(EffectSystem fx, int bxm, int bym) {
        int bx;
        int by;
        if (direct == 0) {
            bx = x + 14336;
            by = y + 12288;
        } else if (direct == 1) {
            bx = x + 2048;
            by = y + 12288;
        } else {
            bx = x + 9216;
            by = y + 14336;
        }
        fx.addCaret(EffectSystem.TYPE_BUBBLE, bx, by, bxm, bym, 0);
    }

    private void actNormal(int key, int keyTrg, AudioManager audio, EffectSystem fx) {
        direct = 2;
        if ((key & KEY_LEFT) != 0) {
            direct = 0;
        }
        if ((key & KEY_RIGHT) != 0) {
            direct = 1;
        }

        if ((keyTrg & KEY_Z) != 0 && swimWait == 0) {
            audio.playSfx(AudioManager.SFX_DASH);
            spawnDirectedBubble(fx, -SWIM_XM[direct], -SWIM_YM[direct]);
            swimWait = 8;
            aniNo = 1;
            if (aniWait < 100) {
                aniWait += 10;
            }
            xm += SWIM_XM[direct];
            ym += SWIM_YM[direct];
        }

        if ((key & KEY_Z) != 0 && (equip & 2) != 0) {
            if (dashWait == 31) {
                audio.playSfx(AudioManager.SFX_READY);
            }
            if (++dashWait > 32) {
                dashWait = 32;
            }
        } else if (dashWait == 32) {
            audio.playSfx(AudioManager.SFX_GO);
            spawnDirectedBubble(fx, DASH_XM[direct] / -8, DASH_YM[direct] / -8);
            xm = DASH_XM[direct];
            ym = DASH_YM[direct];
            unit = 1;
        } else {
            dashWait = 0;
        }

        if (aniWait <= 0) {
            aniNo = 0;
        } else {
            aniWait--;
        }

        if (swimWait > 0) {
            swimWait--;
        }

        if (dashWait == 32) {
            aniNo = 2;
        }

        if (ym < 0x800) {
            ym += 20;
        }

        if (ym < -0x800) ym = -0x800;
        if (ym > 0x800)  ym = 0x800;
        if (xm < -0x800) xm = -0x800;
        if (xm > 0x800)  xm = 0x800;

        if (xm > 0) xm -= 8;
        if (xm < 0) xm += 8;

        if (!airborne) {
            if (xm > 0) xm -= 24;
            if (xm < 0) xm += 24;
        }

        if (xm >= 8 || xm <= -8) {
            x += xm;
        }
        y += ym;

        if (shock > 0) {
            shock--;
        }
        if (noEvent > 0) {
            noEvent--;
        }
    }

    private void actDash(EffectSystem fx) {
        if (--dashWait <= 0) {
            unit = 0;
        }
        if ((dashWait % 4) == 0) {
            int bxm = (DASH_XM[direct] / -8) + fx.random(-0x200, 0x200);
            int bym = (DASH_YM[direct] / -8) + fx.random(-0x200, 0x200);
            fx.addCaret(EffectSystem.TYPE_BUBBLE, x + 0x2000, y + 0x2000, bxm, bym, 0);
        }
        x += xm;
        y += ym;
        aniNo = 3;
        if (shock > 0) {
            shock--;
        }
    }

    private void actShip(EffectSystem fx) {
        fx.addSpawner(EffectSystem.TYPE_STAR, 0, 1,
            x + 0x2000, y + 0x6000, -0xC00, 0xC00, 0, 0xC00, 1, 0);
        xm = 0;
        ym -= 16;
        y += ym;
        aniNo = 3;
        if (shock != 0) {
            shock = 0;
        }
    }

    private int judgeHitBlock(int tx, int ty, int flg, AudioManager audio) {
        if ((flg & 1) != 0 && (flg & 2) != 0) {
            if ((x / 0x400) < (tx * 16 + 15) && (y / 0x400) < (ty * 16 + 12)) {
                if (xm < -0x400) audio.playSfx(AudioManager.SFX_HITHEAD);
                x = (tx * 16 + 15) << 10;
                xm = 0;
                flag |= 1;
            }
            if ((y / 0x400) < (ty * 16 + 15) && (x / 0x400) < (tx * 16 + 12)) {
                if (ym < -0x400) audio.playSfx(AudioManager.SFX_HITHEAD);
                y = (ty * 16 + 16) << 10;
                ym = 0;
                flag |= 2;
            }
        }

        if ((flg & 4) != 0 && (flg & 2) != 0) {
            if (((x + 0x3FF) / 0x400) > (tx * 16 - 14) && (y / 0x400) < (ty * 16 + 12)) {
                if (xm < -0x400) audio.playSfx(AudioManager.SFX_HITHEAD);
                x = (tx * 16 - 14) << 10;
                xm = 0;
                flag |= 4;
            }
            if ((y / 0x400) < (ty * 16 + 15) && (x / 0x400) > (tx * 16 - 12)) {
                if (ym < -0x400) audio.playSfx(AudioManager.SFX_HITHEAD);
                y = (ty * 16 + 16) << 10;
                ym = 0;
                flag |= 2;
            }
        }

        if ((flg & 1) != 0 && (flg & 8) != 0) {
            if ((x / 0x400) < (tx * 16 + 15) && (y / 0x400) > (ty * 16 - 12)) {
                if (xm < -0x400) audio.playSfx(AudioManager.SFX_HITHEAD);
                x = (tx * 16 + 15) << 10;
                xm = 0;
                flag |= 1;
            }
            if ((y / 0x400) >= (ty * 16 - 16) && (x / 0x400) < (tx * 16 + 12)) {
                airborne = false;
                y = (ty * 16 - 16) << 10;
                if (ym > 0) ym = 0;
                flag |= 8;
            }
        }

        if ((flg & 4) != 0 && (flg & 8) != 0) {
            if (((x + 0x3FF) / 0x400) > (tx * 16 - 14) && (y / 0x400) > (ty * 16 - 12)) {
                if (xm > 0x400) audio.playSfx(AudioManager.SFX_HITHEAD);
                x = (tx * 16 - 14) << 10;
                xm = 0;
                flag |= 4;
            }
            if ((y / 0x400) >= (ty * 16 - 16) && (x / 0x400) > (tx * 16 - 12)) {
                airborne = false;
                y = (ty * 16 - 16) << 10;
                if (ym > 0) ym = 0;
                flag |= 8;
            }
        }
        return flag;
    }

    private int judgeHitSnack(int tx, int ty, int flg, MapEngine map,
                              AudioManager audio, EffectSystem fx) {
        flag = 0;

        if ((flg & 1) != 0 && (flg & 2) != 0) {
            if ((x / 0x400) < (tx * 16 + 15) && (y / 0x400) < (ty * 16 + 12)) {
                if (unit != 1) xm = 0;
                x = (tx * 16 + 15) << 10;
                flag |= 1;
            }
            if ((y / 0x400) < (ty * 16 + 15) && (x / 0x400) < (tx * 16 + 12)) {
                if (unit != 1) ym = 0;
                y = (ty * 16 + 16) << 10;
                flag |= 2;
            }
        }

        if ((flg & 4) != 0 && (flg & 2) != 0) {
            if (((x + 0x3FF) / 0x400) > (tx * 16 - 14) && (y / 0x400) < (ty * 16 + 12)) {
                if (unit != 1) xm = 0;
                x = (tx * 16 - 14) << 10;
                flag |= 4;
            }
            if ((y / 0x400) < (ty * 16 + 15) && (x / 0x400) > (tx * 16 - 12)) {
                if (unit != 1) ym = 0;
                y = (ty * 16 + 16) << 10;
                flag |= 2;
            }
        }

        if ((flg & 1) != 0 && (flg & 8) != 0) {
            if ((x / 0x400) < (tx * 16 + 15) && (y / 0x400) > (ty * 16 - 12)) {
                if (unit != 1) xm = 0;
                x = (tx * 16 + 15) << 10;
                flag |= 1;
            }
            if ((y / 0x400) >= (ty * 16 - 16) && (x / 0x400) < (tx * 16 + 12)) {
                if (unit != 1) {
                    airborne = false;
                    if (ym > 0) ym = 0;
                }
                y = (ty * 16 - 16) << 10;
                flag |= 8;
            }
        }

        if ((flg & 4) != 0 && (flg & 8) != 0) {
            if (((x + 0x3FF) / 0x400) > (tx * 16 - 14) && (y / 0x400) > (ty * 16 - 12)) {
                if (unit != 1) xm = 0;
                x = (tx * 16 - 14) << 10;
                flag |= 4;
            }
            if ((y / 0x400) >= (ty * 16 - 16) && (x / 0x400) > (tx * 16 - 12)) {
                if (unit != 1) {
                    airborne = false;
                    if (ym > 0) ym = 0;
                }
                y = (ty * 16 - 16) << 10;
                flag |= 8;
            }
        }

        if ((unit == 1 && flag != 8 && flag != 0) || ((equip & 1) != 0 && (flag & 2) != 0)) {
            audio.playSfx(AudioManager.SFX_CRASH);
            map.setTile(tx, ty, 0);
            fx.addSpawner(EffectSystem.TYPE_BUBBLE, 0, 5,
                (tx * 16 + 8) << 10, (ty * 16 + 8) << 10,
                -0x400, 0x400, -0x400, 0x400, 8, 8);
        }
        return flag;
    }

    private void judgeHitDamage(int tx, int ty, AudioManager audio, EffectSystem fx) {
        int px = x / 0x400;
        int py = y / 0x400;
        if (px < (tx * 16 + 10) && px > (tx * 16 - 10) && py < (ty * 16 + 10) && py > (ty * 16 - 10)) {
            damage(3, audio, fx);
        }
    }

    private void judgeHitVector(int tx, int ty, int type) {
        int px = x / 0x400;
        int py = y / 0x400;
        if (px < (tx * 16 + 8) && px >= (tx * 16 - 8) && py < (ty * 16 + 8) && py >= (ty * 16 - 8)) {
            if (type == 0x60) {
                xm -= 50;
            } else if (type == 0x61) {
                xm += 50;
            } else if (type == 0x62) {
                ym -= 50;
            } else if (type == 0x63) {
                ym += 50;
            }
        }
    }

    private void judgeHitItem(int tx, int ty, MapEngine map,
                              AudioManager audio, EffectSystem fx) {
        int px = x / 0x400;
        int py = y / 0x400;
        if (px < (tx * 16 + 8) && px > (tx * 16 - 8) && py < (ty * 16 + 8) && py > (ty * 16 - 8)) {
            map.setTile(tx, ty, 0);
            audio.playSfx(AudioManager.SFX_ITEM);
            life++;
            exp++;
            if (life > MYC_LIFE[level]) {
                life = MYC_LIFE[level];
            }
            fx.addSpawner(EffectSystem.TYPE_DAMAGE, 11, 1,
                x + 0x2000, y - 0x1000, 0, 0, 0, 0, 1, 0);
            fx.addSpawner(EffectSystem.TYPE_STAR, 0, 4,
                x + 0x2000, y + 0x2000, -0x800, 0x800, -0x800, 0, 1, 0);
        }
    }

    public void hitMap(MapEngine map, AudioManager audio, EffectSystem fx) {
        if (!cond) {
            return;
        }
        int tx = (x / 0x400) / 16;
        int ty = (y / 0x400) / 16;
        int v19 = 4;
        flag = 0;

        for (int i = 0; i < 4; i++) {
            int cx = tx + OFF_X[i];
            int cy = ty + OFF_Y[i];
            int part = map.getTile(cx, cy);

            if (part < 0x80 || part >= 0xA0) {
                v19 -= FLAG_1[i];
            } else if ((judgeHitBlock(cx, cy, FLAG_2[i], audio) & 8) == 0) {
                v19 -= FLAG_1[i];
            }

            if (part >= 0x60 && part < 0x80) {
                judgeHitVector(cx, cy, part);
            }

            if (part >= 0xA0 && part < 0xC0) {
                judgeHitDamage(cx, cy, audio, fx);
            } else if (part >= 0x40 && part < 0x60) {
                judgeHitItem(cx, cy, map, audio, fx);
            } else if (part < 0xE0 || part >= 0x100) {
                v19 -= FLAG_1[i];
            } else if ((judgeHitSnack(cx, cy, FLAG_2[i], map, audio, fx) & 8) == 0) {
                v19 -= FLAG_1[i];
            }
        }

        if (exp >= MYC_EXP[level]) {
            exp -= MYC_EXP[level];
            level++;
            if (level > MAX_LEVEL) {
                level = MAX_LEVEL;
            } else {
                audio.playSfx(AudioManager.SFX_LEVELUP);
            }
            fx.addSpawner(EffectSystem.TYPE_LEVELUP, 0, 1,
                x + 0x2000, y - 0x1000, 0, 0, 0, 0, 1, 0);
        }

        if (v19 <= 0) {
            airborne = true;
        }
    }

    public void putMyChar(Graphics g, MapEngine map) {
        if (!cond) {
            return;
        }
        if ((shock & 2) != 0) {
            return;
        }
        int dx = (x / 0x400) - (map.frameX / 0x400);
        int dy = (y / 0x400) - (map.frameY / 0x400);

        if ((equip & 8) != 0) {
            if (imgShip != null) {
                g.drawRegion(imgShip, aniNo * 40, direct * 40, 40, 40,
                    Sprite.TRANS_NONE, dx - 12, dy - 12, Graphics.TOP | Graphics.LEFT);
            }
        } else {
            Image sheet = ((equip & 1) != 0) ? imgHelmet : imgNormal;
            if (sheet != null) {
                g.drawRegion(sheet, aniNo * 16, direct * 16, 16, 16,
                    Sprite.TRANS_NONE, dx, dy, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    public void putMyStatus(Graphics g) {
        if (imgStatus != null) {
            g.drawRegion(imgStatus, 0, 0, 88, 32, Sprite.TRANS_NONE,
                8, 8, Graphics.TOP | Graphics.LEFT);
        }
        putNumber(g, 16, 8, level);
        putNumber(g, 16, 16, exp);
        putNumber(g, 48, 16, MYC_EXP[level]);
        putNumber(g, 16, 24, life);
        putNumber(g, 48, 24, MYC_LIFE[level]);
    }

    private void putNumber(Graphics g, int x, int y, int val) {
        if (imgFigure == null) {
            return;
        }
        if (val < 0) val = 0;
        if (val > 999) val = 999;

        int dw = imgFigure.getWidth() / 10;
        int dh = imgFigure.getHeight();
        int d0 = val / 100;
        int d1 = (val / 10) % 10;
        int d2 = val % 10;

        if (d0 > 0) {
            g.drawRegion(imgFigure, d0 * dw, 0, dw, dh, Sprite.TRANS_NONE,
                x, y, Graphics.TOP | Graphics.LEFT);
        }
        if (d0 > 0 || d1 > 0) {
            g.drawRegion(imgFigure, d1 * dw, 0, dw, dh, Sprite.TRANS_NONE,
                x + dw, y, Graphics.TOP | Graphics.LEFT);
        }
        g.drawRegion(imgFigure, d2 * dw, 0, dw, dh, Sprite.TRANS_NONE,
            x + (dw << 1), y, Graphics.TOP | Graphics.LEFT);
    }
}