package ikachan;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

public final class ItemMenu {
    public static final int MAX_ITEMS = 10;

    private static final int[] ITEM_EQUIP = {
        0x00, 0x01, 0x00, 0x00, 0x00, 0x04,
        0x00, 0x00, 0x02, 0x00, 0x00, 0x08
    };

    public final byte[] code = new byte[MAX_ITEMS];
    public int selectedItem = 0;

    private final Image imgItemBox;
    private final Image imgItem;
    private final Image imgCursor;

    public ItemMenu() {
        imgItemBox = loadOptional("/img/ItemBox.png");
        imgItem    = loadOptional("/img/Item.png");
        imgCursor  = loadOptional("/img/Cursor.png");
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
        for (int i = 0; i < MAX_ITEMS; i++) {
            code[i] = 0;
        }
        selectedItem = 0;
    }

    public boolean checkItem(int itemCode) {
        for (int i = 0; i < MAX_ITEMS; i++) {
            if ((code[i] & 0xFF) == itemCode) {
                return true;
            }
        }
        return false;
    }

    public boolean addItem(int itemCode, MyChar mc) {
        if (itemCode <= 0 || checkItem(itemCode)) {
            return false;
        }
        for (int i = 0; i < MAX_ITEMS; i++) {
            if (code[i] == 0) {
                code[i] = (byte) itemCode;
                if (itemCode < ITEM_EQUIP.length) {
                    mc.equip |= ITEM_EQUIP[itemCode];
                }
                return true;
            }
        }
        return false;
    }

    public boolean subItem(int itemCode, MyChar mc) {
        if (!checkItem(itemCode)) {
            return false;
        }
        for (int i = 0; i < MAX_ITEMS; i++) {
            if ((code[i] & 0xFF) == itemCode) {
                code[i] = 0;
                if (itemCode >= 0 && itemCode < ITEM_EQUIP.length) {
                    mc.equip &= ~ITEM_EQUIP[itemCode];
                }
                return true;
            }
        }
        return false;
    }

    public void move(int keyTrg, EventVM eventVm, AudioManager audio) {
        if ((keyTrg & MyChar.KEY_LEFT) != 0) {
            audio.playSfx(AudioManager.SFX_DASH);
            if (--selectedItem < 0) {
                selectedItem = MAX_ITEMS - 1;
            }
        }
        if ((keyTrg & MyChar.KEY_RIGHT) != 0) {
            audio.playSfx(AudioManager.SFX_DASH);
            if (++selectedItem >= MAX_ITEMS) {
                selectedItem = 0;
            }
        }
        if ((keyTrg & MyChar.KEY_Z) != 0) {
            audio.playSfx(AudioManager.SFX_DASH);
            int c = code[selectedItem] & 0xFF;
            if (c != 0) {
                eventVm.startEvent(2000 + c);
            }
        }
    }

    public void draw(Graphics g, int viewW, int viewH) {
        int boxX = (viewW - 176) / 2;
        int boxY = (viewH / 2) + 8;

        if (imgItemBox != null) {
            g.drawRegion(imgItemBox, 0, 0, 176, 32, Sprite.TRANS_NONE,
                boxX, boxY, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0x000040);
            g.fillRect(boxX, boxY, 176, 32);
            g.setColor(0xFFFFFF);
            g.drawRect(boxX, boxY, 175, 31);
        }

        int itemsX = (viewW - (16 * MAX_ITEMS)) / 2;
        int itemsY = (viewH / 2) + 16;

        if (imgItem != null) {
            int sheetW = imgItem.getWidth();
            for (int i = 0; i < MAX_ITEMS; i++) {
                int c = code[i] & 0xFF;
                if (c > 0) {
                    int sx = (c - 1) * 16;
                    if (sx >= 0 && sx + 16 <= sheetW) {
                        g.drawRegion(imgItem, sx, 0, 16, 16, Sprite.TRANS_NONE,
                            itemsX + (i * 16), itemsY, Graphics.TOP | Graphics.LEFT);
                    }
                }
            }
        }

        if (imgCursor != null) {
            g.drawRegion(imgCursor, 32, 0, 16, 16, Sprite.TRANS_NONE,
                itemsX + (selectedItem * 16), itemsY, Graphics.TOP | Graphics.LEFT);
        }
    }
}