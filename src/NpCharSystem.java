package ikachan;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public final class NpCharSystem {
    public static final int MAX_NPCS = 100;

    private static final String[] SHEET_NAMES = {
        "Hari", "Isogin", "Kani", "Sleep", "Chibi",
        "Hoshi", "Dum", "Carry", "Juel", "Ufo"
    };

    private static final int[] NPC_DAMAGE  = { 1, 0, 2, 4, 4, 2, 0, 0, 0, 0 };
    private static final int[] NPC_DEFENSE = { 1, 4, 2, 9, 3, 2, 0, 0, 0, 0 };
    private static final int[] NPC_EXP     = { 1, 0, 3, 0, 0, 3, 0, 0, 0, 0 };

    private static final int[] OFF_X = { 0, 1, 0, 1 };
    private static final int[] OFF_Y = { 0, 0, 1, 1 };
    private static final int[] FLAG_1 = { 0, 0, 1, 1 };
    private static final int[] FLAG_2 = { 1 | 2, 4 | 2, 8 | 1, 8 | 4 };

    public final int[] cond = new int[MAX_NPCS];
    public final int[] type = new int[MAX_NPCS];
    public final int[] codeChar = new int[MAX_NPCS];
    public final int[] codeEvent = new int[MAX_NPCS];
    public final int[] actWait = new int[MAX_NPCS];
    public final int[] actNo = new int[MAX_NPCS];
    public final int[] aniNo = new int[MAX_NPCS];
    public final int[] aniWait = new int[MAX_NPCS];
    public final int[] direct = new int[MAX_NPCS];
    public final int[] flag = new int[MAX_NPCS];
    public final boolean[] airborne = new boolean[MAX_NPCS];
    public final int[] x = new int[MAX_NPCS];
    public final int[] y = new int[MAX_NPCS];
    public final int[] xm = new int[MAX_NPCS];
    public final int[] ym = new int[MAX_NPCS];
    public final int[] tgtX = new int[MAX_NPCS];
    public final int[] tgtY = new int[MAX_NPCS];

    private final Image[] sheets = new Image[SHEET_NAMES.length];

    public NpCharSystem() {
        for (int i = 0; i < SHEET_NAMES.length; i++) {
            sheets[i] = loadOptional("/img/" + SHEET_NAMES[i] + ".png");
        }
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
        for (int i = 0; i < MAX_NPCS; i++) {
            cond[i] = 0;
            type[i] = 0;
            codeChar[i] = 0;
            codeEvent[i] = 0;
            actWait[i] = 0;
            actNo[i] = 0;
            aniNo[i] = 0;
            aniWait[i] = 0;
            direct[i] = 0;
            flag[i] = 0;
            airborne[i] = false;
            x[i] = 0;
            y[i] = 0;
            xm[i] = 0;
            ym[i] = 0;
            tgtX[i] = 0;
            tgtY[i] = 0;
        }
    }

    public void loadAll(byte[] data) {
        init();
        if (data == null) {
            return;
        }
        int count = data.length / 22;
        if (count > MAX_NPCS) {
            count = MAX_NPCS;
        }
        for (int i = 0; i < count; i++) {
            readSlot(data, i, false);
        }
    }

    public void reloadEnemies(byte[] data) {
        if (data == null) {
            return;
        }
        int count = data.length / 22;
        if (count > MAX_NPCS) {
            count = MAX_NPCS;
        }
        for (int i = 0; i < count; i++) {
            readSlot(data, i, true);
        }
    }

    private void readSlot(byte[] data, int i, boolean enemiesOnly) {
        int off = i * 22;
        int eType = data[off + 1] & 0xFF;
        if (enemiesOnly && eType != 2) {
            return;
        }
        cond[i] = data[off] & 0xFF;
        type[i] = eType;
        codeChar[i] = data[off + 2];
        codeEvent[i] = PakReader.readS16LE(data, off + 3);
        direct[i] = data[off + 5];
        x[i] = PakReader.readS32LE(data, off + 6);
        y[i] = PakReader.readS32LE(data, off + 10);
        tgtX[i] = PakReader.readS32LE(data, off + 14);
        tgtY[i] = PakReader.readS32LE(data, off + 18);
        actWait[i] = 0;
        actNo[i] = 0;
        aniNo[i] = 0;
        aniWait[i] = 0;
        xm[i] = 0;
        ym[i] = 0;
        flag[i] = 0;
        airborne[i] = false;
    }

    public void act(MyChar mc) {
        for (int i = 0; i < MAX_NPCS; i++) {
            if (cond[i] == 0) {
                continue;
            }
            int cc = codeChar[i];
            if (cc == 0) {
                act00(i, mc);
            } else if (cc == 1 || cc == 7) {
                act01(i, mc);
            } else if (cc == 2) {
                act02(i, mc);
            } else if (cc == 3) {
                act03(i, mc);
            } else if (cc == 4 || cc == 5) {
                act04(i, mc);
            } else if (cc == 6 || cc == 9) {
                act05(i);
            } else if (cc == 8) {
                act06(i);
            }
        }
    }

    private void act00(int i, MyChar mc) {
        if (actNo[i] == 0) {
            if (x[i] > tgtX[i]) xm[i] -= 6;
            if (x[i] < tgtX[i]) xm[i] += 6;
            if (y[i] > tgtY[i]) ym[i] -= 4;
            if (y[i] < tgtY[i]) ym[i] += 4;

            if (xm[i] < 0) direct[i] = 0;
            if (xm[i] > 0) direct[i] = 1;

            if (++aniWait[i] > 60) {
                aniWait[i] = 0;
                if (++aniNo[i] > 1) {
                    aniNo[i] = 0;
                }
            }

            if (xm[i] > 0x400) xm[i] = 0x400;
            if (xm[i] < -0x400) xm[i] = -0x400;
            if (ym[i] > 0x400) ym[i] = 0x400;
            if (ym[i] < -0x400) ym[i] = -0x400;

            x[i] += xm[i];
            y[i] += ym[i];

            if (actWait[i] > 0) {
                actWait[i]--;
            }

            if (actWait[i] == 0 && (mc.equip & 4) == 0
                && (x[i] - 0xA000) < mc.x && (x[i] + 0xA000) > mc.x
                && (y[i] - 0xA000) < mc.y && (y[i] + 0xA000) > mc.y) {
                if (type[i] == 2) {
                    actNo[i] = 1;
                }
                actWait[i] = 300;
            }
        } else if (actNo[i] == 1) {
            if (x[i] > mc.x) direct[i] = 0;
            if (x[i] < mc.x) direct[i] = 1;

            if (++aniWait[i] > 2) {
                aniWait[i] = 0;
                if (++aniNo[i] > 3) {
                    aniNo[i] = 2;
                }
            }

            if (--actWait[i] <= 200) {
                actNo[i] = 0;
                aniNo[i] = 0;
            }
        }
    }

    private void act01(int i, MyChar mc) {
        aniWait[i]++;
        if (ym[i] < 0x800) {
            ym[i] += 20;
        }
        y[i] += ym[i];

        if (x[i] < mc.x) direct[i] = 1;
        if (x[i] > mc.x) direct[i] = 0;

        if (aniWait[i] > 40) {
            aniWait[i] = 0;
            if (++aniNo[i] > 1) {
                aniNo[i] = 0;
            }
        }
    }

    private void act02(int i, MyChar mc) {
        if (actNo[i] == 0) {
            if (x[i] < tgtX[i] - 0x10000) direct[i] = 1;
            if (x[i] > tgtX[i] + 0x10000) direct[i] = 0;

            if (direct[i] == 0 && xm[i] > -0x200) xm[i] -= 16;
            if (direct[i] == 1 && xm[i] < 0x200) xm[i] += 16;

            if (!airborne[i]) {
                actWait[i]++;
            }

            if (actWait[i] > 80) {
                actWait[i] = 0;
                if (type[i] != 2 || (mc.equip & 4) != 0) {
                    ym[i] = -1024;
                } else {
                    ym[i] = -1536;
                    actNo[i] = 1;
                }
            }

            if (++aniWait[i] > 10) {
                aniWait[i] = 0;
                if (++aniNo[i] > 1) {
                    aniNo[i] = 0;
                }
            }

            if (ym[i] < 0x800) {
                ym[i] += 20;
            }
            x[i] += xm[i];
            y[i] += ym[i];

            if (airborne[i]) {
                aniNo[i] = 2;
            }
        } else if (actNo[i] == 1) {
            if (++aniWait[i] > 2) {
                aniWait[i] = 0;
                if (++aniNo[i] > 3) {
                    aniNo[i] = 2;
                }
            }

            if (x[i] < tgtX[i] - 0x10000) direct[i] = 1;
            if (x[i] > tgtX[i] + 0x10000) direct[i] = 0;

            if (direct[i] == 0 && xm[i] > -0x200) xm[i] -= 16;
            if (direct[i] == 1 && xm[i] < 0x200) xm[i] += 16;

            if (ym[i] < 0x800) {
                ym[i] += 20;
            }
            x[i] += xm[i];
            y[i] += ym[i];

            if (ym[i] > 0) {
                actNo[i] = 0;
            }
        }
    }

    private void act03(int i, MyChar mc) {
        if (ym[i] < 0x800) {
            ym[i] += 20;
        }
        y[i] += ym[i];

        if (actNo[i] == 0) {
            if (actWait[i] > 0) {
                actWait[i]--;
            }
            if ((x[i] - 0x8000) >= mc.x || (x[i] + 0x8000) <= mc.x
                || (y[i] - 0x8000) >= mc.y || y[i] <= mc.y) {
                if (++aniWait[i] > 5) {
                    aniWait[i] = 0;
                    if (--aniNo[i] < 0) {
                        aniNo[i] = 0;
                    }
                }
            } else if (++aniWait[i] > 2) {
                aniWait[i] = 0;
                if (++aniNo[i] > 3) {
                    aniNo[i] = 3;
                }
            }
        }
    }

    private void act04(int i, MyChar mc) {
        if (x[i] > mc.x) xm[i] -= 24;
        if (x[i] < mc.x) xm[i] += 24;
        if (y[i] > mc.y) ym[i] -= 16;
        if (y[i] < mc.y) ym[i] += 16;

        if (x[i] > mc.x) direct[i] = 0;
        if (x[i] < mc.x) direct[i] = 1;
        if ((x[i] - 0x8000) < mc.x && (x[i] + 0x8000) > mc.x
            && (y[i] - 0x8000) < mc.y && (y[i] + 0x8000) > mc.y) {
            direct[i] = 2;
        }
        actNo[i] = ((y[i] + 0x2000) > mc.y) ? 1 : 0;

        if (++aniWait[i] > 8) {
            aniWait[i] = 0;
            if (++aniNo[i] > 1) {
                aniNo[i] = 0;
            }
        }

        if (xm[i] > 0x400) xm[i] = 0x400;
        if (xm[i] < -0x400) xm[i] = -0x400;
        if (ym[i] > 0x400) ym[i] = 0x400;
        if (ym[i] < -0x400) ym[i] = -0x400;

        x[i] += xm[i];
        y[i] += ym[i];
    }

    private void act05(int i) {
        if (ym[i] < 0x800) {
            ym[i] += 20;
        }
        y[i] += ym[i];
    }

    private void act06(int i) {
        if (actNo[i] == 0) {
            if (x[i] > tgtX[i]) xm[i] -= 12;
            if (x[i] < tgtX[i]) xm[i] += 12;
            if (y[i] > tgtY[i]) ym[i] -= 8;
            if (y[i] < tgtY[i]) ym[i] += 8;

            if (xm[i] < 0) direct[i] = 0;
            if (xm[i] > 0) direct[i] = 1;

            if (++aniWait[i] > 30) {
                aniWait[i] = 0;
                if (++aniNo[i] > 1) {
                    aniNo[i] = 0;
                }
            }

            if (xm[i] > 0x800) xm[i] = 0x800;
            if (xm[i] < -0x800) xm[i] = -0x800;
            if (ym[i] > 0x800) ym[i] = 0x800;
            if (ym[i] < -0x800) ym[i] = -0x800;

            x[i] += xm[i];
            y[i] += ym[i];
        }
    }

    private int judgeHitBlock(int i, int tx, int ty, int flg) {
        if ((flg & 1) != 0 && (flg & 2) != 0) {
            if ((x[i] / 0x400) < (tx * 16 + 15) && (y[i] / 0x400) < (ty * 16 + 12)) {
                x[i] = (tx * 16 + 15) << 10;
                xm[i] = 0;
                flag[i] |= 1;
            }
            if ((y[i] / 0x400) < (ty * 16 + 15) && (x[i] / 0x400) < (tx * 16 + 12)) {
                y[i] = (ty * 16 + 16) << 10;
                ym[i] = 0;
                flag[i] |= 2;
            }
        }

        if ((flg & 4) != 0 && (flg & 2) != 0) {
            if (((x[i] + 0x3FF) / 0x400) > (tx * 16 - 14) && (y[i] / 0x400) < (ty * 16 + 12)) {
                x[i] = (tx * 16 - 14) << 10;
                xm[i] = 0;
                flag[i] |= 4;
            }
            if ((y[i] / 0x400) < (ty * 16 + 15) && (x[i] / 0x400) > (tx * 16 - 12)) {
                y[i] = (ty * 16 + 16) << 10;
                ym[i] = 0;
                flag[i] |= 2;
            }
        }

        if ((flg & 1) != 0 && (flg & 8) != 0) {
            if ((x[i] / 0x400) < (tx * 16 + 15) && (y[i] / 0x400) > (ty * 16 - 12)) {
                x[i] = (tx * 16 + 15) << 10;
                xm[i] = 0;
                flag[i] |= 1;
            }
            if ((y[i] / 0x400) >= (ty * 16 - 16) && (x[i] / 0x400) < (tx * 16 + 12)) {
                airborne[i] = false;
                y[i] = (ty * 16 - 16) << 10;
                if (ym[i] > 0) ym[i] = 0;
                flag[i] |= 8;
            }
        }

        if ((flg & 4) != 0 && (flg & 8) != 0) {
            if (((x[i] + 0x3FF) / 0x400) > (tx * 16 - 14) && (y[i] / 0x400) > (ty * 16 - 12)) {
                x[i] = (tx * 16 - 14) << 10;
                xm[i] = 0;
                flag[i] |= 4;
            }
            if ((y[i] / 0x400) >= (ty * 16 - 16) && (x[i] / 0x400) > (tx * 16 - 12)) {
                airborne[i] = false;
                y[i] = (ty * 16 - 16) << 10;
                if (ym[i] > 0) ym[i] = 0;
                flag[i] |= 8;
            }
        }
        return flag[i];
    }

    public void hitMap(MapEngine map) {
        for (int i = 0; i < MAX_NPCS; i++) {
            if (cond[i] == 0) {
                continue;
            }
            int tx = (x[i] / 0x400) / 16;
            int ty = (y[i] / 0x400) / 16;
            int v4 = 2;
            flag[i] = 0;

            for (int j = 0; j < 4; j++) {
                int cx = tx + OFF_X[j];
                int cy = ty + OFF_Y[j];
                int part = map.getTile(cx, cy);
                if ((part < 0x80 || part >= 0xA0) && (part < 0xE0 || part >= 0x100)) {
                    v4 -= FLAG_1[j];
                } else if ((judgeHitBlock(i, cx, cy, FLAG_2[j]) & 8) == 0) {
                    v4 -= FLAG_1[j];
                }
            }

            if (v4 <= 0) {
                airborne[i] = true;
            }
        }
    }

    public int hitMyChar(MyChar mc, AudioManager audio, EffectSystem fx) {
        int triggeredEvent = -1;
        if (!mc.cond) {
            return -1;
        }

        for (int i = 0; i < MAX_NPCS; i++) {
            if (cond[i] == 0) {
                continue;
            }
            boolean touch = false;
            int t = type[i];

            if (t != 3 && (t != 2 || mc.shock != 0)) {
                if (t == 0 && mc.noEvent == 0
                    && mc.x < x[i] + 0x1000 && mc.x > x[i] - 0x1000
                    && mc.y < y[i] + 0x1000 && mc.y > y[i] - 0x1000) {
                    triggeredEvent = codeEvent[i];
                    mc.noEvent = 100;
                    continue;
                }
            } else {
                if (mc.x < (x[i] + 0x3400) && mc.x > (x[i] + 0x2000)
                    && mc.y < (y[i] + 0x3000) && mc.y > (y[i] - 0x3000)) {
                    mc.x = x[i] + 0x3400;
                    mc.xm = 0;
                    mc.flag |= 1;
                    touch = true;
                }
                if (mc.y < (y[i] + 0x3400) && mc.y > (y[i] + 0x2000)
                    && mc.x < (x[i] + 0x3000) && mc.x > (x[i] - 0x3000)) {
                    if (mc.ym < -100) {
                        audio.playSfx(AudioManager.SFX_HITHEAD);
                    }
                    mc.y = y[i] + 0x3400;
                    mc.ym = 0;
                    mc.flag |= 2;
                    touch = true;
                }
                if (mc.x > (x[i] - 0x3400) && (mc.x + 0x3FF) < (x[i] - 0x2000)
                    && mc.y < (y[i] + 0x3000) && mc.y > (y[i] - 0x3000)) {
                    mc.x = x[i] - 0x3400;
                    mc.xm = 0;
                    mc.flag |= 4;
                    touch = true;
                }
                if (mc.y >= (y[i] - 0x3400) && mc.y < (y[i] - 0x2000)
                    && mc.x > (x[i] - 0x3000) && mc.x < (x[i] + 0x3000)) {
                    mc.airborne = false;
                    mc.y = y[i] - 0x3400;
                    if (mc.ym > 0) {
                        mc.ym = 0;
                    }
                    mc.flag |= 8;
                    touch = true;
                }
            }

            if (touch && mc.noEvent == 0) {
                int cc = codeChar[i];
                if (cc < 0 || cc >= NPC_DAMAGE.length) {
                    cc = 0;
                }
                if (t == 2) {
                    if (actNo[i] == 1) {
                        if (mc.x < x[i]) mc.xm = -0x400;
                        if (mc.x > x[i]) mc.xm = 0x400;
                        mc.damage(NPC_DAMAGE[cc], audio, fx);
                    }

                    if ((mc.flag != 0 && mc.unit == 1 && mc.flag != 8)
                        || ((mc.flag & 2) != 0 && (mc.equip & 1) != 0)) {
                        if (NPC_DEFENSE[cc] > mc.level) {
                            if (mc.noEvent == 0) {
                                audio.playSfx(AudioManager.SFX_NODMG);
                            }
                            mc.noEvent = 100;
                        } else {
                            audio.playSfx(AudioManager.SFX_WIN);
                            int gain = NPC_EXP[cc];
                            mc.exp += gain;
                            fx.addSpawner(EffectSystem.TYPE_DAMAGE, gain + 10, 1,
                                x[i] + 0x2000, y[i] - 0x1000, 0, 0, 0, 0, 1, 0);

                            cond[i] = 0;
                            fx.addSpawner(EffectSystem.TYPE_STAR, 0, 6,
                                x[i] + 0x2000, y[i] + 0x2000,
                                -0x800, 0x800, -0x800, -0x200, 8, 8);

                            triggeredEvent = codeEvent[i];
                            mc.noEvent = 100;
                        }
                    }
                } else if (t == 3) {
                    triggeredEvent = codeEvent[i];
                    mc.noEvent = 100;
                }
            }
        }
        return triggeredEvent;
    }

    public void draw(Graphics g, MapEngine map, int viewW, int viewH) {
        int fx = map.frameX / 0x400;
        int fy = map.frameY / 0x400;

        for (int i = 0; i < MAX_NPCS; i++) {
            if (cond[i] == 0) {
                continue;
            }
            int cc = codeChar[i];
            if (cc < 0 || cc >= sheets.length) {
                continue;
            }
            Image sheet = sheets[cc];
            if (sheet == null) {
                continue;
            }

            int dx = (x[i] / 0x400) - fx;
            int dy = (y[i] / 0x400) - fy;

            if (cc == 0 || cc == 8) {
                int idx = (direct[i] * 4) + aniNo[i];
                if (idx >= 0 && idx < 8) {
                    drawClipped(g, sheet, (idx & 3) * 16, (idx >> 2) * 16, 16, 16, dx, dy, viewW, viewH);
                }
            } else if (cc == 1 || cc == 6) {
                int idx = (direct[i] * 2) + aniNo[i];
                if (idx >= 0 && idx < 4) {
                    int sx = 16 * ((tgtX[i] - x[i]) / 0x4000);
                    drawClipped(g, sheet, sx, idx * 16, 16, 16, dx, dy, viewW, viewH);
                }
            } else if (cc == 2) {
                int idx = ((direct[i] * 4) + aniNo[i]) & 3;
                drawClipped(g, sheet, (idx & 1) * 16, (idx >> 1) * 16, 16, 16, dx, dy, viewW, viewH);
            } else if (cc == 3) {
                int idx = aniNo[i];
                if (idx >= 0 && idx < 4) {
                    drawClipped(g, sheet, (idx & 1) * 20, (idx >> 1) * 20, 20, 20, dx - 2, dy - 4, viewW, viewH);
                }
            } else if (cc == 4) {
                int idx = (direct[i] * 2) + aniNo[i];
                if (idx >= 0 && idx < 6) {
                    drawClipped(g, sheet, (idx >> 1) * 8, (idx & 1) * 8, 8, 8, dx + 4, dy + 4, viewW, viewH);
                }
            } else if (cc == 5) {
                int idx = (direct[i] * 2) + aniNo[i];
                if (idx >= 0 && idx < 6) {
                    drawClipped(g, sheet, (idx >> 1) * 16, (idx & 1) * 16, 16, 16, dx, dy, viewW, viewH);
                }
            } else if (cc == 7) {
                int idx = (direct[i] * 2) + aniNo[i];
                if (idx >= 0 && idx < 4) {
                    drawClipped(g, sheet, (idx >> 1) * 30, (idx & 1) * 20, 30, 20, dx - 6, dy - 2, viewW, viewH);
                }
            } else if (cc == 9) {
                drawClipped(g, sheet, 0, 0, 32, 32, dx - 8, dy - 16, viewW, viewH);
            }
        }
    }

    private void drawClipped(Graphics g, Image sheet, int sx, int sy, int w, int h,
                             int dx, int dy, int viewW, int viewH) {
        if (dx + w <= 0 || dx >= viewW || dy + h <= 0 || dy >= viewH) {
            return;
        }
        if (sx < 0 || sy < 0 || sx + w > sheet.getWidth() || sy + h > sheet.getHeight()) {
            return;
        }
        g.drawRegion(sheet, sx, sy, w, h, Sprite.TRANS_NONE,
            dx, dy, Graphics.TOP | Graphics.LEFT);
    }
}