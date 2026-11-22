package ikachan;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public final class BossIronhead {
    private static final int[] RC_X = { 0, 0, 32, 0, 0, 0, 32, 32, 32, 32 };
    private static final int[] RC_Y = { 0, 72, 0, 24, 48, 24, 0, 24, 48, 24 };

    private static final int HIT_LEFT   = 0x4000;
    private static final int HIT_TOP    = 0x0800;
    private static final int HIT_RIGHT  = 0xC000;
    private static final int HIT_BOTTOM = 0x5800;

    public boolean cond;
    public int life;
    public int shock;
    public int codeEvent;
    public int defeatEvent;
    public int actWait;
    public int actNo;
    public int aniNo;
    public int aniWait;
    public int direct;
    public int x;
    public int y;
    public int xm;
    public int ym;
    public int tgtX;
    public int tgtY;

    private final Image imgIronhead;

    public BossIronhead() {
        imgIronhead = loadOptional("/img/Ironhead.png");
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
        life = 15;
        shock = 0;
        codeEvent = 1000;
        defeatEvent = 1001;
        actWait = 0;
        actNo = 0;
        aniNo = 2;
        aniWait = 0;
        direct = 0;
        x = 0x20C000;
        y = 0x168000;
        xm = 0;
        ym = 0;
        tgtX = 0x218000;
        tgtY = 0x16C000;
    }

    public void act(MyChar mc, EffectSystem fx) {
        if (!cond) {
            return;
        }
        if (shock > 0) {
            shock--;
        }

        if (actNo == 0) {
            if (x > tgtX) xm -= 6;
            if (x < tgtX) xm += 6;
            if (y > tgtY) ym -= 4;
            if (y < tgtY) ym += 4;

            if (xm < 0 && direct == 1) {
                direct = 0;
                fx.addSpawner(EffectSystem.TYPE_BUBBLE, 0, 5,
                    x + 0xC000, y + 0x3000, 0x200, 0x400, -0x400, 0x400, 16, 12);
            }
            if (xm > 0 && direct == 0) {
                direct = 1;
                fx.addSpawner(EffectSystem.TYPE_BUBBLE, 0, 5,
                    x + 0x4000, y + 0x3000, -0x400, -0x200, -0x400, 0x400, 16, 12);
            }

            if (++aniWait > 8) {
                aniWait = 0;
                if (++aniNo > 9) {
                    aniNo = 2;
                }
            }

            if (xm > 0x800) xm = 0x800;
            if (xm < -0x800) xm = -0x800;
            if (ym > 0x800) ym = 0x800;
            if (ym < -0x800) ym = -0x800;

            x += xm;
            y += ym;
        } else if (actNo == 1) {
            if (x > mc.x) xm -= 8;
            if (x < mc.x) xm += 8;
            if (y > mc.y) ym -= 8;
            if (y < mc.y) ym += 8;

            if (actWait > 0) {
                actWait--;
            }
            if (actWait <= 0 && y < mc.y && (y + 0x2000) > mc.y) {
                actWait = 400;
                ym = 0;
                actNo = 2;
                xm = ((x + 0x6000) <= mc.x) ? 0x4000 : -0x4000;
            }

            if (xm < 0 && direct == 1) direct = 0;
            if (xm > 0 && direct == 0) direct = 1;

            if (++aniWait > 4) {
                aniWait = 0;
                aniNo++;
            }
            if (aniNo > 9) {
                aniNo = 2;
            }

            if (xm > 0x800) xm = 0x800;
            if (xm < -0x800) xm = -0x800;
            if (ym > 0x800) ym = 0x800;
            if (ym < -0x800) ym = -0x800;

            x += xm;
            y += ym;
        } else if (actNo == 2) {
            if (xm > 0) xm -= 8;
            if (xm < 0) xm += 8;
            if (xm < 8 && xm > -8) {
                actNo = 1;
            }

            if (actWait > 0) {
                actWait--;
            }

            if ((actWait % 5) == 0) {
                fx.addSpawner(EffectSystem.TYPE_STAR, 0, 1,
                    x + 0x8000, y + 0x3000, -xm, -xm, -0x244, 0x400, 16, 12);
            }

            aniNo = 2;
            x += xm;
            y += ym;
        }
    }

    private void damage(int dmg, AudioManager audio, EffectSystem fx) {
        if (shock == 0) {
            shock = 100;
            life -= dmg;
            if (life < 0) {
                life = 0;
            }
            fx.addSpawner(EffectSystem.TYPE_DAMAGE, 10 - dmg, 1,
                x + 0x2000, y - 0x1000, 0, 0, 0, 0, 1, 0);
            audio.playSfx(AudioManager.SFX_BOSSOUCH);
        }
    }

    public void hitMyChar(MyChar mc, EventVM eventVm, AudioManager audio, EffectSystem fx) {
        if (!cond || !mc.cond) {
            return;
        }

        int flg = 0;
        boolean touch = false;

        if (mc.x < (x + HIT_RIGHT) && mc.x > (x + HIT_RIGHT - 0x1400)
            && mc.y < (y + HIT_BOTTOM - 0x1000) && (mc.y + 0x4000) > (y + HIT_TOP + 0x1000)) {
            mc.x = x + HIT_RIGHT;
            mc.xm = 0;
            flg |= 1;
            touch = true;
        }
        if (mc.y < (y + HIT_BOTTOM) && mc.y > (y + HIT_BOTTOM - 0x1400)
            && mc.x < (x + HIT_RIGHT - 0x1000) && (mc.x + 0x4000) > (x + HIT_LEFT + 0x1000)) {
            if (mc.ym < -100) {
                audio.playSfx(AudioManager.SFX_HITHEAD);
            }
            mc.y = y + HIT_BOTTOM;
            mc.ym = 0;
            flg |= 2;
            touch = true;
        }
        if ((mc.x + 0x4000) > (x + HIT_LEFT) && (mc.x + 0x4000) < (x + HIT_LEFT + 0x1400)
            && mc.y < (y + HIT_BOTTOM - 0x1000) && (mc.y + 0x4000) > (y + HIT_TOP + 0x1000)) {
            mc.x = x + HIT_LEFT - 0x4000;
            mc.xm = 0;
            flg |= 4;
            touch = true;
        }
        if ((mc.y + 0x4000) > (y + HIT_TOP) && (mc.y + 0x4000) < (y + HIT_TOP + 0x1400)
            && mc.x < (x + HIT_RIGHT - 0x1000) && (mc.x + 0x4000) > (x + HIT_LEFT + 0x1000)) {
            mc.airborne = false;
            mc.y = y + HIT_TOP - 0x4000;
            if (mc.ym > 0) {
                mc.ym = 0;
            }
            flg |= 8;
            touch = true;
        }

        if (touch) {
            if (actNo == 0) {
                if (mc.noEvent == 0) {
                    eventVm.startEvent(codeEvent);
                    mc.noEvent = 100;
                }
            } else if (actNo == 1) {
                if (shock == 0) {
                    if ((flg & 2) != 0) {
                        ym = -0x400;
                        mc.ym = 0x400;
                        damage((mc.unit == 1) ? 3 : 1, audio, fx);
                    } else if (mc.unit == 1) {
                        if (mc.direct == 0) xm = -0x400;
                        if (mc.direct == 1) xm = 0x400;
                        damage(3, audio, fx);
                    }
                }
            } else if (actNo == 2) {
                if ((flg & 2) != 0) {
                    ym = -0x400;
                    mc.ym = 0x400;
                    damage((mc.unit == 1) ? 3 : 1, audio, fx);
                } else if (mc.shock == 0) {
                    if (mc.x < x + 0x6000) mc.xm = -0x400;
                    if (mc.x > x + 0x6000) mc.xm = 0x400;
                    mc.damage(3, audio, fx);
                }
            }

            if (actNo != 0 && life == 0) {
                eventVm.startEvent(defeatEvent);
                mc.noEvent = 100;
                actNo = 0;
                shock = 0;
                xm = 0;
                ym = 0;
            }
        }
    }

    public void draw(Graphics g, MapEngine map, int viewW, int viewH) {
        if (!cond || imgIronhead == null) {
            return;
        }
        int dx = (x / 0x400) - (map.frameX / 0x400);
        int dy = (y / 0x400) - (map.frameY / 0x400);
        if (dx + 64 <= 0 || dx >= viewW || dy + 24 <= 0 || dy >= viewH) {
            return;
        }

        int headIdx = (actNo == 2) ? (actWait & 1) : 0;
        int bodyIdx = (aniNo >= 0 && aniNo < RC_X.length) ? aniNo : 2;

        int hSx = RC_X[headIdx] + (direct != 0 ? 64 : 0);
        int hSy = RC_Y[headIdx];
        int bSx = RC_X[bodyIdx] + (direct != 0 ? 64 : 0);
        int bSy = RC_Y[bodyIdx];

        if (direct != 0) {
            g.drawRegion(imgIronhead, hSx, hSy, 32, 24, Sprite.TRANS_NONE,
                dx + 32, dy, Graphics.TOP | Graphics.LEFT);
            g.drawRegion(imgIronhead, bSx, bSy, 32, 24, Sprite.TRANS_NONE,
                dx, dy, Graphics.TOP | Graphics.LEFT);
        } else {
            g.drawRegion(imgIronhead, hSx, hSy, 32, 24, Sprite.TRANS_NONE,
                dx, dy, Graphics.TOP | Graphics.LEFT);
            g.drawRegion(imgIronhead, bSx, bSy, 32, 24, Sprite.TRANS_NONE,
                dx + 32, dy, Graphics.TOP | Graphics.LEFT);
        }
    }
}