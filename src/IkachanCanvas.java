package ikachan;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;

public final class IkachanCanvas extends GameCanvas implements Runnable {
    private static final int MODE_OPENING  = 0;
    private static final int MODE_GAMEPLAY = 1;
    private static final int MODE_INTRO    = 5;
    private static final int MODE_LOAD     = 6;
    private static final int MODE_STAFF    = 7;

    private static final int RAW_LEFT  = 1;
    private static final int RAW_RIGHT = 2;
    private static final int RAW_UP    = 4;
    private static final int RAW_DOWN  = 8;
    private static final int RAW_Z     = 16;

    private final IkachanMIDlet midlet;
    private volatile boolean running;
    private volatile boolean paused;

    private MapEngine map;
    private MyChar player;
    private EffectSystem effects;
    private NpCharSystem npcs;
    private ItemMenu itemMenu;
    private BossIronhead boss;
    private EventVM eventVm;
    private OpeningScreen opening;
    private byte[] rawNpcData;
    private AudioManager audio;
    private BitmapFont font;

    private Image landscapeBuf;
    private Graphics landscapeG;
    private boolean landscapeMode = false;
    private boolean menuOpen = false;

    private int mode = MODE_OPENING;
    private int nextModeAfterLoad = MODE_INTRO;
    private boolean gameplayMusicStarted = false;

    private int heldRaw = 0;
    private int prevGameKey = 0;
    private int fps = 0;
    private int frameCounter = 0;
    private long lastFpsMs = 0L;
    private String errMsg = null;

    public IkachanCanvas(IkachanMIDlet midlet) {
        super(false);
        this.midlet = midlet;
        loadAssets();
    }

    private void loadAssets() {
        try {
            PakReader pak = new PakReader();
            map = new MapEngine(pak.loadSlot(PakReader.SLOT_MAP1));
            rawNpcData = pak.loadSlot(PakReader.SLOT_NPCHAR);
            byte[] wordsData = pak.loadSlot(PakReader.SLOT_WORDS);
            byte[] staffData = pak.loadSlot(PakReader.SLOT_STAFF);
            font = new BitmapFont(pak.loadSlot(PakReader.SLOT_FONT));
            eventVm = new EventVM(pak.loadSlot(PakReader.SLOT_EVENT));
            opening = new OpeningScreen(wordsData, staffData);
            audio = new AudioManager();
            effects = new EffectSystem();
            player = new MyChar();
            itemMenu = new ItemMenu();
            boss = new BossIronhead();
            npcs = new NpCharSystem();
            npcs.loadAll(rawNpcData);
            eventVm.initFlagsAndItems();
            snapCameraToPlayer();

            mode = MODE_OPENING;
            audio.playBgm(3);
        } catch (Throwable t) {
            errMsg = "ERR: " + t.getMessage();
        }
    }

    private int getViewWidth() {
        int w = getWidth();
        if (w <= 0) {
            w = 240;
        }
        return landscapeMode ? 320 : w;
    }

    private int getViewHeight() {
        int h = getHeight();
        if (h <= 0) {
            h = 320;
        }
        return landscapeMode ? 240 : h;
    }

    private void snapCameraToPlayer() {
        if (map == null || player == null) {
            return;
        }
        int vw = getViewWidth();
        int vh = getViewHeight();
        map.frameMode = MapEngine.FRAME_MODE_MYCHAR;
        map.frameX = player.x - (vw << 9) + 0x2000;
        map.frameY = player.y - (vh << 9) + 0x2000;
        map.clampFrame(vw, vh);
    }

    private void toggleLandscape() {
        if (!landscapeMode && landscapeBuf == null) {
            landscapeBuf = Image.createImage(320, 240);
            landscapeG = landscapeBuf.getGraphics();
        }
        landscapeMode = !landscapeMode;
        if (opening != null) {
            opening.resetPixelX(getViewWidth());
        }
        if (map != null) {
            map.clampFrame(getViewWidth(), getViewHeight());
        }
    }

    private int mapKeyBit(int keyCode) {
        if (keyCode == KEY_NUM4) return RAW_LEFT;
        if (keyCode == KEY_NUM6) return RAW_RIGHT;
        if (keyCode == KEY_NUM2) return RAW_UP;
        if (keyCode == KEY_NUM8) return RAW_DOWN;
        if (keyCode == KEY_NUM5 || keyCode == KEY_NUM9) return RAW_Z;
        int action = getGameAction(keyCode);
        if (action == LEFT) return RAW_LEFT;
        if (action == RIGHT) return RAW_RIGHT;
        if (action == UP) return RAW_UP;
        if (action == DOWN) return RAW_DOWN;
        if (action == FIRE) return RAW_Z;
        return 0;
    }

    private int getGameKeyMask() {
        int r = heldRaw;
        int out = (r & RAW_Z) != 0 ? MyChar.KEY_Z : 0;
        if (!landscapeMode) {
            if ((r & RAW_LEFT) != 0)  out |= MyChar.KEY_LEFT;
            if ((r & RAW_RIGHT) != 0) out |= MyChar.KEY_RIGHT;
            if ((r & RAW_UP) != 0)    out |= MyChar.KEY_UP;
            if ((r & RAW_DOWN) != 0)  out |= MyChar.KEY_DOWN;
            return out;
        }
        if ((r & RAW_UP) != 0)    out |= MyChar.KEY_LEFT;
        if ((r & RAW_DOWN) != 0)  out |= MyChar.KEY_RIGHT;
        if ((r & RAW_RIGHT) != 0) out |= MyChar.KEY_UP;
        if ((r & RAW_LEFT) != 0)  out |= MyChar.KEY_DOWN;
        return out;
    }

    public void start() {
        setFullScreenMode(true);
        running = true;
        lastFpsMs = System.currentTimeMillis();
        new Thread(this).start();
    }

    public void pause() {
        paused = true;
        heldRaw = 0;
    }

    public void resume() {
        setFullScreenMode(true);
        paused = false;
    }

    public void stop() {
        running = false;
        if (audio != null) {
            audio.stopAll();
        }
    }

    protected void keyPressed(int keyCode) {
        if (keyCode == KEY_STAR) {
            stop();
            midlet.notifyDestroyed();
            return;
        }
        if (keyCode == KEY_POUND) {
            toggleLandscape();
            return;
        }
        if (keyCode == KEY_NUM1 && audio != null) {
            audio.toggleBgm(0);
            return;
        }
        if (keyCode == KEY_NUM7 && mode == MODE_GAMEPLAY && eventVm != null && eventVm.mode == 0) {
            menuOpen = !menuOpen;
            if (audio != null) {
                audio.playSfx(AudioManager.SFX_DASH);
            }
            return;
        }
        if (keyCode == KEY_NUM3 && player != null) {
            player.equip ^= 3;
            return;
        }
        if (keyCode == KEY_NUM0 && player != null) {
            player.init();
            if (effects != null) effects.clear();
            if (npcs != null) npcs.loadAll(rawNpcData);
            if (itemMenu != null) itemMenu.init();
            if (boss != null) boss.init();
            if (eventVm != null) {
                eventVm.init();
                eventVm.initFlagsAndItems();
            }
            menuOpen = false;
            gameplayMusicStarted = false;
            snapCameraToPlayer();
            return;
        }
        int bit = mapKeyBit(keyCode);
        if (bit != 0) {
            heldRaw |= bit;
        }
    }

    protected void keyReleased(int keyCode) {
        int bit = mapKeyBit(keyCode);
        if (bit != 0) {
            heldRaw &= ~bit;
        }
    }

    public void run() {
        Graphics screenG = getGraphics();
        while (running) {
            long t0 = System.currentTimeMillis();
            if (!paused) {
                update();
                renderFrame(screenG);
                flushGraphics();
                frameCounter++;
                long now = System.currentTimeMillis();
                if (now - lastFpsMs >= 1000L) {
                    fps = frameCounter;
                    frameCounter = 0;
                    lastFpsMs = now;
                }
            }
            long elapsed = System.currentTimeMillis() - t0;
            long wait = 20L - elapsed;
            if (wait < 1L) {
                wait = 1L;
            }
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void enterGameplay() {
        mode = MODE_GAMEPLAY;
        if (eventVm != null) {
            eventVm.fadeMask = false;
        }
        snapCameraToPlayer();
        if (!gameplayMusicStarted) {
            gameplayMusicStarted = true;
            audio.playBgm(0);
        }
    }

    private void update() {
        if (map == null || player == null || audio == null || effects == null
            || npcs == null || itemMenu == null || boss == null || eventVm == null
            || opening == null) {
            return;
        }

        int vw = getViewWidth();
        int vh = getViewHeight();
        int key = getGameKeyMask();
        int keyTrg = key & ~prevGameKey;
        prevGameKey = key;

        if (mode == MODE_OPENING) {
            if ((keyTrg & MyChar.KEY_Z) != 0) {
                eventVm.fadeWait = 0;
                eventVm.fadeMode = EventVM.FADE_OUT;
            }
            if (eventVm.updateFade(map, effects, vw, vh)) {
                mode = MODE_LOAD;
                nextModeAfterLoad = MODE_INTRO;
                eventVm.startEvent(1);
            }
            return;
        }

        if (mode == MODE_LOAD) {
            int res = eventVm.update(key, keyTrg, player, itemMenu, npcs,
                rawNpcData, boss, map, audio);
            if (res == 2) {
                eventVm.fadeWait = 0;
                eventVm.fadeMode = EventVM.FADE_IN;
                nextModeAfterLoad = MODE_GAMEPLAY;
            } else if (res == 1) {
                stop();
                midlet.notifyDestroyed();
                return;
            } else if (res == 3) {
                mode = MODE_STAFF;
                audio.playBgm(5);
                return;
            }
            if (eventVm.mode == 0) {
                mode = nextModeAfterLoad;
                if (mode == MODE_INTRO) {
                    audio.playBgm(1);
                    eventVm.startEvent(2);
                } else if (mode == MODE_GAMEPLAY) {
                    enterGameplay();
                }
            }
            eventVm.updateFade(map, effects, vw, vh);
            return;
        }

        if (mode == MODE_INTRO) {
            if (eventVm.mode != 0) {
                int res = eventVm.update(key, keyTrg, player, itemMenu, npcs,
                    rawNpcData, boss, map, audio);
                if (res == 3) {
                    mode = MODE_STAFF;
                    audio.playBgm(5);
                    return;
                }
            }
            if ((keyTrg & MyChar.KEY_Z) != 0) {
                eventVm.init();
                enterGameplay();
            }
            return;
        }

        if (mode == MODE_STAFF) {
            if ((keyTrg & MyChar.KEY_Z) != 0) {
                stop();
                midlet.notifyDestroyed();
            }
            return;
        }

        if (eventVm.mode == 0) {
            if (menuOpen) {
                itemMenu.move(keyTrg, eventVm, audio);
            } else {
                if (player.cond) {
                    player.act(key, keyTrg, audio, effects);
                    if (player.unit != 2) {
                        int evt = npcs.hitMyChar(player, audio, effects);
                        if (evt >= 0) {
                            eventVm.startEvent(evt);
                        }
                        player.hitMap(map, audio, effects);
                    }
                }
                if (player.dead) {
                    player.dead = false;
                    eventVm.startEvent(6);
                }
                if (player.y < -0x8000 && !eventVm.msgBox) {
                    eventVm.msgBox = true;
                    eventVm.startEvent(4000);
                }
                npcs.hitMap(map);
                npcs.act(player);
                boss.act(player, effects);
                boss.hitMyChar(player, eventVm, audio, effects);
            }
        } else {
            if (player.unit == 2) {
                player.act(0, 0, audio, effects);
            }
            int res = eventVm.update(key, keyTrg, player, itemMenu, npcs,
                rawNpcData, boss, map, audio);
            if (res == 1) {
                stop();
                midlet.notifyDestroyed();
                return;
            } else if (res == 3) {
                mode = MODE_STAFF;
                audio.playBgm(5);
                return;
            }
        }

        if (!menuOpen || eventVm.mode != 0) {
            effects.update(map);
        }

        int tx = player.x;
        int ty = player.y;
        if (map.frameMode == MapEngine.FRAME_MODE_NPCHAR
            && map.frameNpc >= 0 && map.frameNpc < NpCharSystem.MAX_NPCS) {
            tx = npcs.x[map.frameNpc];
            ty = npcs.y[map.frameNpc];
        } else if (map.frameMode == MapEngine.FRAME_MODE_BOSS) {
            tx = boss.x;
            ty = boss.y;
        }
        map.moveFrameToTarget(tx, ty, vw, vh);
        eventVm.updateFade(map, effects, vw, vh);
    }

    private void renderFrame(Graphics screenG) {
        if (landscapeMode && landscapeBuf != null) {
            drawScreen(landscapeG, 320, 240);
            screenG.drawRegion(landscapeBuf, 0, 0, 320, 240,
                Sprite.TRANS_ROT90, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else {
            drawScreen(screenG, getViewWidth(), getViewHeight());
        }
    }

    private void drawScreen(Graphics g, int vw, int vh) {
        if (errMsg != null) {
            g.setColor(0x000020);
            g.fillRect(0, 0, vw, vh);
            if (font != null) {
                font.drawAscii(g, errMsg, 4, 8, 0xFFFF6666);
            } else {
                g.setColor(0xFFFFFF);
                g.drawString(errMsg, 4, 16, Graphics.TOP | Graphics.LEFT);
            }
            return;
        }

        if (mode == MODE_OPENING) {
            opening.updateAndDrawOpening(g, vw, vh);
            eventVm.drawFade(g, vw, vh);
            return;
        }

        if (mode == MODE_LOAD) {
            g.setColor(0x000000);
            g.fillRect(0, 0, vw, vh);
            eventVm.drawOverlay(g, font, vw, vh);
            eventVm.drawFade(g, vw, vh);
            return;
        }

        if (mode == MODE_INTRO) {
            opening.drawIntro(g, font, vw, vh);
            eventVm.drawOverlay(g, font, vw, vh);
            eventVm.drawFade(g, vw, vh);
            return;
        }

        if (mode == MODE_STAFF) {
            opening.drawStaff(g, font, vw, vh);
            return;
        }

        map.putBack(g, vw, vh);
        map.putMapBack(g, vw, vh);
        map.putMapVector(g, vw, vh);
        npcs.draw(g, map, vw, vh);
        if (player.unit != 2) {
            boss.draw(g, map, vw, vh);
        }
        if (player.cond && ((player.shock & 1) == 0 || eventVm.msgBox)) {
            player.putMyChar(g, map);
        }
        map.putMapFront(g, vw, vh);
        effects.draw(g, map, vw, vh);
        eventVm.drawFade(g, vw, vh);
        player.putMyStatus(g);
        if (menuOpen) {
            itemMenu.draw(g, vw, vh);
        }
        eventVm.drawOverlay(g, font, vw, vh);

        if (font != null && !eventVm.msgBox && !menuOpen) {
            String dbg = "FPS:" + fps + " EQ:" + player.equip + " 7:ITEM";
            font.drawAscii(g, dbg, 8, vh - 16, 0xFF000000);
            font.drawAscii(g, dbg, 7, vh - 17, 0xFFFFFFFF);
        }
    }
}