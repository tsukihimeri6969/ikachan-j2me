package ikachan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.rms.RecordStore;

public final class EventVM {
    public static final int FLAG_BYTES = 32;

    public static final int FADE_NONE = 0;
    public static final int FADE_QUAKE = 1;
    public static final int FADE_OUT = 2;
    public static final int FADE_IN = 3;
    public static final int FADE_QUAKE2 = 4;

    private static final int MAX_LINE_CHARS = 48;
    private static final String SAVE_STORE = "IkaRec";
    private static final int SAVE_MAGIC = 0x494B4133;

    public final byte[] flags = new byte[FLAG_BYTES];

    public int mode = 0;
    public boolean msgBox = false;
    public int eventNo = 0;
    public int pRead = 0;
    public int aniCursor = 0;
    public int wait = 0;
    public int typeWait = 4;
    public int line = 0;
    public final int[] yPosLine = { 0, 20 };
    public boolean selectNo = false;

    public boolean fadeMask = false;
    public int fadeMode = FADE_NONE;
    public int fadeWait = 0;

    public int lastResult = 0;

    private final byte[] data;
    private final int size;

    private final int[][] lineCodes = new int[2][MAX_LINE_CHARS];
    private final int[] lineLen = new int[2];

    private final Image imgMsgBox;
    private final Image imgCursor;
    private final Image imgYesNo;
    private final Image imgFade;

    public EventVM(byte[] rawScript) {
        data = normalizeText(rawScript);
        size = data.length;
        imgMsgBox = loadOptional("/img/Msgbox.png");
        imgCursor = loadOptional("/img/Cursor.png");
        imgYesNo  = loadOptional("/img/YesNo.png");
        imgFade   = loadOptional("/img/Fade.png");
        init();
    }

    private byte[] normalizeText(byte[] raw) {
        if (raw == null) {
            return new byte[0];
        }
        int len = 0;
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] != 0x0D) {
                len++;
            }
        }
        byte[] out = new byte[len];
        int p = 0;
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] != 0x0D) {
                out[p++] = raw[i];
            }
        }
        return out;
    }

    private Image loadOptional(String path) {
        try {
            return Image.createImage(path);
        } catch (IOException e) {
            return null;
        }
    }

    public void init() {
        mode = 0;
        msgBox = false;
        eventNo = 0;
        pRead = 0;
        aniCursor = 0;
        wait = 0;
        typeWait = 4;
        line = 0;
        yPosLine[0] = 0;
        yPosLine[1] = 20;
        selectNo = false;
        lineLen[0] = 0;
        lineLen[1] = 0;
        fadeMask = false;
        fadeMode = FADE_NONE;
        fadeWait = 0;
        lastResult = 0;
    }

    public void initFlagsAndItems() {
        for (int i = 0; i < FLAG_BYTES; i++) {
            flags[i] = 0;
        }
    }

    public void setFlag(int id) {
        if (id >= 0 && id < (FLAG_BYTES * 8)) {
            flags[id >> 3] |= (1 << (id & 7));
        }
    }

    public void cutFlag(int id) {
        if (id >= 0 && id < (FLAG_BYTES * 8)) {
            flags[id >> 3] &= ~(1 << (id & 7));
        }
    }

    public boolean getFlag(int id) {
        if (id < 0 || id >= (FLAG_BYTES * 8)) {
            return false;
        }
        return (flags[id >> 3] & (1 << (id & 7))) != 0;
    }

    public void startEvent(int id) {
        mode = 1;
        typeWait = 4;
        eventNo = id;
    }

    private boolean jumpEventScript() {
        for (pRead = 0; pRead < size; pRead++) {
            if (data[pRead] == '#' && (pRead + 1) < size && data[pRead + 1] == 'e') {
                pRead += 2;
                int no = getEventScriptNo();
                while (pRead < size && data[pRead] != '\n') {
                    pRead++;
                }
                if (pRead < size) {
                    pRead++;
                }
                if (no == eventNo) {
                    return true;
                }
                if (no > eventNo) {
                    return false;
                }
            }
        }
        return false;
    }

    private int getEventScriptNo() {
        if (pRead + 4 > size) {
            pRead = size;
            return 0;
        }
        int b = 0;
        b += (data[pRead++] - '0') * 1000;
        b += (data[pRead++] - '0') * 100;
        b += (data[pRead++] - '0') * 10;
        b += (data[pRead++] - '0');
        return b;
    }

    private boolean isCommand(char c1, char c2) {
        return (pRead + 2) < size
            && data[pRead] == '<'
            && data[pRead + 1] == (byte) c1
            && data[pRead + 2] == (byte) c2;
    }

    private void clearTextLines() {
        line = 0;
        yPosLine[0] = 0;
        yPosLine[1] = 20;
        lineLen[0] = 0;
        lineLen[1] = 0;
    }

    private void appendGlyph(int code, int key, AudioManager audio) {
        msgBox = true;
        wait = ((key & MyChar.KEY_Z) != 0) ? 0 : typeWait;
        audio.playSfx(AudioManager.SFX_MESSAGE);
        int slot = line & 1;
        if (lineLen[slot] < MAX_LINE_CHARS) {
            lineCodes[slot][lineLen[slot]++] = code;
        }
    }

    public int update(int key, int keyTrg, MyChar mc, ItemMenu itemMenu,
                      NpCharSystem npcs, byte[] rawNpcData, BossIronhead boss,
                      MapEngine map, AudioManager audio) {
        if (mode == 0) {
            return 0;
        }

        if (mode == 1) {
            if (jumpEventScript()) {
                wait = 0;
                clearTextLines();
                mode = 2;
            } else {
                mode = 0;
            }
            return 0;
        }

        if (mode == 2) {
            if (--wait >= 0) {
                return 0;
            }
        } else if (mode == 4) {
            yPosLine[0] -= 4;
            yPosLine[1] -= 4;
            if (yPosLine[1] != 0) {
                if (yPosLine[0] == 0) {
                    lineLen[1] = 0;
                    yPosLine[1] = 20;
                    line++;
                    mode = 2;
                }
            } else {
                lineLen[0] = 0;
                yPosLine[0] = 20;
                line++;
                mode = 2;
            }
            return 0;
        } else if (mode == 5) {
            if ((keyTrg & MyChar.KEY_Z) != 0) {
                mode = 4;
            }
            return 0;
        } else if (mode == 6) {
            if ((keyTrg & MyChar.KEY_Z) != 0) {
                wait = 0;
                clearTextLines();
                mode = 2;
            }
            return 0;
        } else if (mode == 7) {
            if ((keyTrg & MyChar.KEY_Z) != 0) {
                mode = 0;
                msgBox = false;
            }
            return 0;
        } else if (mode == 8) {
            mode = 0;
            msgBox = false;
            return 0;
        } else if (mode == 9) {
            if ((keyTrg & MyChar.KEY_LEFT) != 0) {
                selectNo = false;
                audio.playSfx(AudioManager.SFX_READY);
            }
            if ((keyTrg & MyChar.KEY_RIGHT) != 0) {
                selectNo = true;
                audio.playSfx(AudioManager.SFX_READY);
            }
            if ((keyTrg & MyChar.KEY_Z) != 0) {
                audio.playSfx(AudioManager.SFX_DASH);
                if (selectNo) {
                    pRead += 4;
                    wait = 0;
                    clearTextLines();
                    mode = 2;
                } else {
                    eventNo = getEventScriptNo();
                    mode = 1;
                }
            }
            return 0;
        } else {
            return 0;
        }

        while (pRead < size) {
            int b1 = data[pRead] & 0xFF;

            if (b1 == '~') {
                pRead++;
                if (pRead >= size) {
                    break;
                }
                b1 = data[pRead] & 0xFF;
            }

            if ((b1 & 0x80) != 0) {
                if (((b1 >= 0x81 && b1 <= 0x9F) || (b1 >= 0xE0 && b1 <= 0xFC)) && (pRead + 1) < size) {
                    int b2 = data[pRead + 1] & 0xFF;
                    appendGlyph((b1 << 8) | b2, key, audio);
                    pRead += 2;
                } else {
                    appendGlyph(b1, key, audio);
                    pRead++;
                }
                return 0;
            }

            if ((b1 >= 'A' && b1 <= 'z') || (b1 >= '0' && b1 <= '9')
                || b1 == ',' || b1 == '.' || b1 == '/') {
                appendGlyph(b1, key, audio);
                pRead++;
                return 0;
            }

            if (b1 == '+') {
                pRead += 2;
                wait = 0;
                if (((line & 1) != 0) || yPosLine[1] != 0) {
                    if (((line & 1) != 1) || yPosLine[0] != 0) {
                        line++;
                    } else {
                        mode = 5;
                    }
                } else {
                    mode = 5;
                }
                return 0;
            }

            if (b1 == '{') {
                pRead += 2;
                mode = 6;
                return 0;
            }

            if (isCommand('f', '+')) {
                pRead += 3;
                setFlag(getEventScriptNo());
                return 0;
            }
            if (isCommand('f', '-')) {
                pRead += 3;
                cutFlag(getEventScriptNo());
                return 0;
            }
            if (isCommand('f', 'j')) {
                pRead += 3;
                int flg = getEventScriptNo();
                if (getFlag(flg)) {
                    pRead++;
                    eventNo = getEventScriptNo();
                    mode = 1;
                } else {
                    pRead += 5;
                }
                return 0;
            }
            if (isCommand('i', '+')) {
                audio.playSfx(AudioManager.SFX_ITEM);
                pRead += 3;
                itemMenu.addItem(getEventScriptNo(), mc);
                return 0;
            }
            if (isCommand('i', '-')) {
                pRead += 3;
                itemMenu.subItem(getEventScriptNo(), mc);
                return 0;
            }
            if (isCommand('i', 'j')) {
                pRead += 3;
                int code = getEventScriptNo();
                if (itemMenu.checkItem(code)) {
                    pRead++;
                    eventNo = getEventScriptNo();
                    mode = 1;
                } else {
                    pRead += 5;
                }
                return 0;
            }
            if (isCommand('p', 'c')) {
                pRead += 3;
                int tx = getEventScriptNo();
                pRead++;
                int ty = getEventScriptNo();
                pRead++;
                int tile = getEventScriptNo();
                map.setTile(tx, ty, tile);
                return 0;
            }
            if (isCommand('c', 'm')) {
                pRead += 3;
                int z = getEventScriptNo();
                pRead++;
                int tx = getEventScriptNo();
                pRead++;
                int ty = getEventScriptNo();
                if (z >= 0 && z < NpCharSystem.MAX_NPCS) {
                    int dx = tx - ((npcs.x[z] / 16) / 0x400);
                    int dy = ty - ((npcs.y[z] / 16) / 0x400);
                    npcs.x[z] = tx << 14;
                    npcs.y[z] = ty << 14;
                    npcs.tgtX[z] += dx << 14;
                    npcs.tgtY[z] += dy << 14;
                }
                return 0;
            }
            if (isCommand('s', 'e')) {
                pRead += 3;
                int sid = getEventScriptNo();
                audio.playSfx(sid);
                return 0;
            }
            if (isCommand('w', 'a')) {
                pRead += 3;
                wait = getEventScriptNo();
                return 0;
            }
            if (isCommand('e', 'k')) {
                pRead += 3;
                fadeWait = 0;
                fadeMode = FADE_QUAKE;
                return 0;
            }
            if (isCommand('l', 'e')) {
                pRead += 3;
                fadeWait = 0;
                fadeMode = FADE_QUAKE2;
                return 0;
            }
            if (isCommand('f', 'o')) {
                pRead += 3;
                fadeWait = 0;
                fadeMode = FADE_OUT;
                return 0;
            }
            if (isCommand('f', 'i')) {
                pRead += 3;
                fadeWait = 0;
                fadeMode = FADE_IN;
                return 0;
            }
            if (isCommand('m', 'a')) {
                pRead += 3;
                fadeMask = true;
                return 0;
            }
            if (isCommand('n', 'm')) {
                pRead += 3;
                fadeMask = false;
                return 0;
            }
            if (isCommand('y', 'n')) {
                pRead += 3;
                selectNo = false;
                mode = 9;
                audio.playSfx(AudioManager.SFX_YESNO);
                return 0;
            }
            if (isCommand('m', 'c')) {
                pRead += 3;
                msgBox = false;
                return 0;
            }
            if (isCommand('d', 's')) {
                audio.playSfx(AudioManager.SFX_SAVE);
                saveRecord(mc, itemMenu, map, npcs);
                pRead += 3;
                return 0;
            }
            if (isCommand('d', 'l')) {
                audio.playSfx(AudioManager.SFX_SAVE);
                boolean ok = loadRecord(mc, itemMenu, map, npcs);
                pRead += 3;
                if (ok) {
                    mc.noEvent = 100;
                    lastResult = 2;
                    return 2;
                }
                lastResult = 1;
                return 1;
            }
            if (isCommand('n', 'l')) {
                npcs.loadAll(rawNpcData);
                mc.noEvent = 100;
                pRead += 3;
                return 0;
            }
            if (isCommand('c', 'e')) {
                npcs.reloadEnemies(rawNpcData);
                boss.init();
                pRead += 3;
                return 0;
            }
            if (isCommand('e', '+')) {
                mc.life = MyChar.MYC_LIFE[mc.level];
                pRead += 3;
                audio.playSfx(AudioManager.SFX_LIFEUP);
                return 0;
            }
            if (isCommand('g', 'e')) {
                mc.unit = 2;
                mc.direct = 2;
                fadeMode = FADE_NONE;
                pRead += 3;
                audio.playSfx(AudioManager.SFX_LIFEUP);
                return 0;
            }
            if (isCommand('r', 'e')) {
                mc.carry = 0;
                mc.cond = true;
                mc.dead = false;
                mc.unit = 0;
                mc.aniNo = 0;
                mc.aniWait = 0;
                mc.shock = 0;
                mc.noEvent = 100;
                mc.ym = 0;
                mc.xm = 0;
                mc.life = MyChar.MYC_LIFE[mc.level];
                pRead += 3;
                return 0;
            }
            if (isCommand('e', 'c')) {
                mc.carry++;
                pRead += 3;
                audio.playSfx(AudioManager.SFX_ITEM);
                return 0;
            }
            if (isCommand('e', 'j')) {
                pRead += 3;
                if (mc.carry == getEventScriptNo()) {
                    pRead++;
                    eventNo = getEventScriptNo();
                    mode = 1;
                } else {
                    pRead += 5;
                }
                return 0;
            }
            if (isCommand('f', 't')) {
                mc.life = MyChar.MYC_LIFE[mc.level];
                pRead += 3;
                map.frameMode = getEventScriptNo();
                pRead++;
                map.frameNpc = getEventScriptNo();
                return 0;
            }
            if (isCommand('b', 'o')) {
                pRead += 3;
                getEventScriptNo();
                boss.actNo = 1;
                return 0;
            }
            if (isCommand('p', 'p')) {
                pRead += 3;
                return 0;
            }
            if (isCommand('p', 's') || isCommand('p', 'f')) {
                pRead += 3;
                audio.stopBgm();
                return 0;
            }
            if (isCommand('p', 'd')) {
                pRead += 3;
                int track = getEventScriptNo();
                audio.playBgm(track);
                return 0;
            }
            if (isCommand('p', 'h') || isCommand('p', 'n')) {
                pRead += 3;
                return 0;
            }
            if (isCommand('t', 'e')) {
                mode = 7;
                return 0;
            }
            if (isCommand('e', 'n')) {
                mode = 8;
                return 0;
            }
            if (isCommand('e', 'x')) {
                lastResult = 1;
                return 1;
            }
            if (b1 == '<' && (pRead + 1) < size && data[pRead + 1] == '*') {
                lastResult = 3;
                return 3;
            }

            pRead++;
        }

        mode = 8;
        return 0;
    }

    public boolean updateFade(MapEngine map, EffectSystem fx, int viewW, int viewH) {
        if (fadeMode == FADE_NONE) {
            fadeWait = 0;
            return false;
        }

        int fadeW = (viewW + 15) / 16;
        int fadeH = (viewH + 15) / 16;
        int fadeTime = fadeW + fadeH + 15;

        if (fadeMode == FADE_QUAKE) {
            if (++fadeWait <= 60) {
                map.frameX += fx.random(-10, 10) << 10;
                map.frameY += fx.random(-10, 10) << 10;
                spawnQuakeDebris(map, fx, viewW, viewH);
            }
            return false;
        }

        if (fadeMode == FADE_QUAKE2) {
            if ((++fadeWait % 4) == 0) {
                map.frameX += fx.random(-1, 1) << 10;
                map.frameY += fx.random(-1, 1) << 10;
                spawnQuakeDebris(map, fx, viewW, viewH);
            }
            return false;
        }

        if (fadeMode == FADE_OUT || fadeMode == FADE_IN) {
            if (++fadeWait > fadeTime) {
                fadeMode = FADE_NONE;
                return true;
            }
        }
        return false;
    }

    private void spawnQuakeDebris(MapEngine map, EffectSystem fx, int viewW, int viewH) {
        int cx = map.frameX + (viewW << 9);
        int cy = map.frameY + (viewH << 9);
        int rx = (viewW / 2) + 53;
        int ry = (viewH / 2) + 40;
        fx.addSpawner(EffectSystem.TYPE_BUBBLE, 0, 2, cx, cy, 0, 0, 0, 0, rx, ry);
        fx.addSpawner(EffectSystem.TYPE_STAR, 0, 2, cx, cy, 0, 0, 0, 0, rx, ry);
    }

    public void drawFade(Graphics g, int viewW, int viewH) {
        if (fadeMode == FADE_OUT || fadeMode == FADE_IN) {
            if (imgFade != null) {
                int fadeW = (viewW + 15) / 16;
                int fadeH = (viewH + 15) / 16;
                for (int y = 0; y < fadeH; y++) {
                    int dy = ((fadeH - 1) - y) * 16;
                    for (int x = 0; x < fadeW; x++) {
                        int frame = fadeWait - y - x;
                        if (frame < 0) frame = 0;
                        if (frame > 15) frame = 15;
                        int tile = (fadeMode == FADE_OUT) ? frame : (15 - frame);
                        g.drawRegion(imgFade, tile * 16, 0, 16, 16, Sprite.TRANS_NONE,
                            x * 16, dy, Graphics.TOP | Graphics.LEFT);
                    }
                }
            }
        }
        if (fadeMask) {
            g.setColor(0x000000);
            g.fillRect(0, 0, viewW, viewH);
        }
    }

    public void drawOverlay(Graphics g, BitmapFont font, int viewW, int viewH) {
        if (msgBox) {
            aniCursor++;
            int boxW = (viewW >= 304) ? 300 : (viewW - 8);
            int boxH = 48;
            int boxX = (viewW >= 304) ? ((viewW / 2) - 152) : ((viewW - boxW) / 2);
            int boxY = viewH - 56;

            if (imgMsgBox != null) {
                if (boxW >= 300) {
                    g.drawRegion(imgMsgBox, 0, 0, 300, 48, Sprite.TRANS_NONE,
                        boxX, boxY, Graphics.TOP | Graphics.LEFT);
                } else {
                    int leftW = boxW / 2;
                    int rightW = boxW - leftW;
                    g.drawRegion(imgMsgBox, 0, 0, leftW, 48, Sprite.TRANS_NONE,
                        boxX, boxY, Graphics.TOP | Graphics.LEFT);
                    g.drawRegion(imgMsgBox, 300 - rightW, 0, rightW, 48, Sprite.TRANS_NONE,
                        boxX + leftW, boxY, Graphics.TOP | Graphics.LEFT);
                }
            } else {
                g.setColor(0x000040);
                g.fillRect(boxX, boxY, boxW, boxH);
                g.setColor(0xFFFFFF);
                g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
            }

            if (font != null) {
                int textX = (viewW >= 304) ? ((viewW / 2) - 136) : (boxX + 10);
                int textY = boxY + 6;
                int clipW = (viewW >= 304) ? 272 : (boxW - 20);

                int oldCx = g.getClipX();
                int oldCy = g.getClipY();
                int oldCw = g.getClipWidth();
                int oldCh = g.getClipHeight();
                g.setClip(textX, textY, clipW, 36);

                drawTextLine(g, font, 0, textX, textY + yPosLine[0]);
                drawTextLine(g, font, 1, textX, textY + yPosLine[1]);

                g.setClip(oldCx, oldCy, oldCw, oldCh);
            }

            if ((mode == 5 || mode == 6 || mode == 7) && imgCursor != null) {
                int f = (aniCursor >> 2) & 7;
                int sx;
                int sy;
                if (f == 0 || f == 3) {
                    sx = 56; sy = 16;
                } else if (f == 1 || f == 2) {
                    sx = 48; sy = 16;
                } else if (f == 4) {
                    sx = 64; sy = 16;
                } else if (f == 5) {
                    sx = 72; sy = 16;
                } else if (f == 6) {
                    sx = 64; sy = 0;
                } else {
                    sx = 72; sy = 0;
                }
                int curX = (viewW >= 304) ? ((viewW / 2) + 132) : (boxX + boxW - 14);
                g.drawRegion(imgCursor, sx, sy, 8, 16, Sprite.TRANS_NONE,
                    curX, viewH - 26, Graphics.TOP | Graphics.LEFT);
            }
        }

        if (mode == 9 && imgYesNo != null) {
            int selX = ((viewW / 2) - 81) + (selectNo ? 96 : 0);
            int selY = (viewH / 2) - 27;
            g.drawRegion(imgYesNo, 48, 0, 64, 32, Sprite.TRANS_NONE,
                selX, selY, Graphics.TOP | Graphics.LEFT);
            g.drawRegion(imgYesNo, 0, 0, 48, 16, Sprite.TRANS_NONE,
                (viewW / 2) - 72, (viewH / 2) - 20, Graphics.TOP | Graphics.LEFT);
            g.drawRegion(imgYesNo, 0, 16, 48, 16, Sprite.TRANS_NONE,
                (viewW / 2) + 24, (viewH / 2) - 20, Graphics.TOP | Graphics.LEFT);
        }
    }

    private void drawTextLine(Graphics g, BitmapFont font, int slot, int x, int y) {
        int len = lineLen[slot];
        int cx = x;
        for (int i = 0; i < len; i++) {
            int code = lineCodes[slot][i];
            font.drawGlyph(g, code, cx + 1, y + 1, 0xFF0000FF);
            cx += font.drawGlyph(g, code, cx, y, 0xFFFFFFFF);
        }
    }

    public boolean saveRecord(MyChar mc, ItemMenu itemMenu, MapEngine map, NpCharSystem npcs) {
        RecordStore rs = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(22000);
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(SAVE_MAGIC);
            dos.writeInt(mc.x);
            dos.writeInt(mc.y);
            dos.writeShort(mc.life);
            dos.writeShort(mc.exp);
            dos.writeByte(mc.level);
            dos.writeByte(mc.equip);
            dos.write(flags, 0, FLAG_BYTES);
            dos.write(itemMenu.code, 0, ItemMenu.MAX_ITEMS);
            dos.writeInt(map.width);
            dos.writeInt(map.height);
            dos.write(map.data, 0, map.data.length);
            for (int i = 0; i < NpCharSystem.MAX_NPCS; i++) {
                dos.writeInt(npcs.x[i]);
                dos.writeInt(npcs.y[i]);
                dos.writeInt(npcs.tgtX[i]);
                dos.writeInt(npcs.tgtY[i]);
                dos.writeInt(npcs.xm[i]);
                dos.writeInt(npcs.ym[i]);
            }
            dos.flush();
            byte[] blob = baos.toByteArray();

            rs = RecordStore.openRecordStore(SAVE_STORE, true);
            if (rs.getNumRecords() == 0) {
                rs.addRecord(blob, 0, blob.length);
            } else {
                rs.setRecord(1, blob, 0, blob.length);
            }
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.closeRecordStore();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public boolean loadRecord(MyChar mc, ItemMenu itemMenu, MapEngine map, NpCharSystem npcs) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(SAVE_STORE, false);
            if (rs.getNumRecords() == 0) {
                return false;
            }
            byte[] blob = rs.getRecord(1);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(blob));
            if (dis.readInt() != SAVE_MAGIC) {
                return false;
            }
            mc.x = dis.readInt();
            mc.y = dis.readInt();
            mc.life = dis.readShort();
            mc.exp = dis.readShort();
            mc.level = dis.readByte();
            mc.equip = dis.readByte() & 0xFF;
            dis.readFully(flags, 0, FLAG_BYTES);
            dis.readFully(itemMenu.code, 0, ItemMenu.MAX_ITEMS);
            int w = dis.readInt();
            int h = dis.readInt();
            if (w * h == map.data.length) {
                dis.readFully(map.data, 0, map.data.length);
            }
            for (int i = 0; i < NpCharSystem.MAX_NPCS; i++) {
                npcs.x[i] = dis.readInt();
                npcs.y[i] = dis.readInt();
                npcs.tgtX[i] = dis.readInt();
                npcs.tgtY[i] = dis.readInt();
                npcs.xm[i] = dis.readInt();
                npcs.ym[i] = dis.readInt();
            }
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.closeRecordStore();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}