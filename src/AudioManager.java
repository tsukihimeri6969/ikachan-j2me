package ikachan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;

public final class AudioManager {
    public static final int SFX_DASH     = 0;
    public static final int SFX_HITHEAD  = 1;
    public static final int SFX_CRASH    = 2;
    public static final int SFX_ITEM     = 3;
    public static final int SFX_LEVELUP  = 4;
    public static final int SFX_LIFEUP   = 5;
    public static final int SFX_OUCH     = 6;
    public static final int SFX_DEAD     = 7;
    public static final int SFX_READY    = 8;
    public static final int SFX_GO       = 9;
    public static final int SFX_MESSAGE  = 10;
    public static final int SFX_YESNO    = 11;
    public static final int SFX_SAVE     = 12;
    public static final int SFX_NODMG    = 13;
    public static final int SFX_BOSSOUCH = 14;
    public static final int SFX_QUAKE    = 15;
    public static final int SFX_WIN      = 16;

    private static final String[] SFX_NAMES = {
        "dash", "hithead", "crash", "item", "levelup", "lifeup",
        "ouch", "dead", "ready", "go", "message", "yesno",
        "save", "nodmg", "bossouch", "quake", "win"
    };

    private static final String[] BGM_NAMES = {
        "ikachan", "magirete", "buriki", "mizuno", "quake", "tidepool"
    };

    private final byte[][] sfxData = new byte[SFX_NAMES.length][];
    private Player sfxPlayer;
    private Player bgmPlayer;
    private int currentBgm = -1;

    public AudioManager() {
        for (int i = 0; i < SFX_NAMES.length; i++) {
            sfxData[i] = readResource("/audio/sfx/" + SFX_NAMES[i] + ".amr");
        }
    }

    private byte[] readResource(String path) {
        InputStream in = getClass().getResourceAsStream(path);
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] tmp = new byte[512];
            int n;
            while ((n = in.read(tmp)) != -1) {
                out.write(tmp, 0, n);
            }
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public void playSfx(int id) {
        if (id < 0 || id >= sfxData.length || sfxData[id] == null) {
            return;
        }
        stopSfx();
        try {
            sfxPlayer = Manager.createPlayer(new ByteArrayInputStream(sfxData[id]), "audio/amr");
            sfxPlayer.realize();
            sfxPlayer.prefetch();
            sfxPlayer.start();
        } catch (Throwable t) {
            stopSfx();
        }
    }

    public void stopSfx() {
        if (sfxPlayer != null) {
            try {
                sfxPlayer.stop();
            } catch (Throwable ignored) {
            }
            try {
                sfxPlayer.close();
            } catch (Throwable ignored) {
            }
            sfxPlayer = null;
        }
    }

    public void playBgm(int track) {
        if (track < 0 || track >= BGM_NAMES.length) {
            return;
        }
        if (currentBgm == track && bgmPlayer != null) {
            return;
        }
        stopBgm();
        try {
            InputStream in = getClass().getResourceAsStream("/audio/bgm/" + BGM_NAMES[track] + ".amr");
            if (in == null) {
                return;
            }
            bgmPlayer = Manager.createPlayer(in, "audio/amr");
            bgmPlayer.realize();
            bgmPlayer.prefetch();
            bgmPlayer.setLoopCount(-1);
            bgmPlayer.start();
            currentBgm = track;
        } catch (Throwable t) {
            stopBgm();
        }
    }

    public void toggleBgm(int track) {
        if (bgmPlayer != null) {
            stopBgm();
        } else {
            playBgm(track);
        }
    }

    public void stopBgm() {
        currentBgm = -1;
        if (bgmPlayer != null) {
            try {
                bgmPlayer.stop();
            } catch (Throwable ignored) {
            }
            try {
                bgmPlayer.close();
            } catch (Throwable ignored) {
            }
            bgmPlayer = null;
        }
    }

    public void stopAll() {
        stopSfx();
        stopBgm();
    }
}