package ikachan;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class PakReader {
    public static final int SLOT_MAP1 = 0;
    public static final int SLOT_NPCHAR = 1;
    public static final int SLOT_EVENT = 2;
    public static final int SLOT_WORDS = 3;
    public static final int SLOT_STAFF = 4;
    public static final int SLOT_FONT = 5;

    private int[] offsets;
    private int[] lengths;

    public PakReader() throws IOException {
        InputStream in = getClass().getResourceAsStream("/data.pak");
        if (in == null) {
            throw new IOException("Missing /data.pak");
        }
        DataInputStream dis = new DataInputStream(in);
        try {
            int magic = dis.readInt();
            if (magic != 0x4950414B) { // "IPAK"
                throw new IOException("Bad data.pak magic");
            }
            int count = dis.readUnsignedShort();
            offsets = new int[count];
            lengths = new int[count];
            for (int i = 0; i < count; i++) {
                offsets[i] = dis.readInt();
                lengths[i] = dis.readInt();
            }
        } finally {
            dis.close();
        }
    }

    public byte[] loadSlot(int slot) throws IOException {
        InputStream in = getClass().getResourceAsStream("/data.pak");
        if (in == null) {
            throw new IOException("Missing /data.pak");
        }
        DataInputStream dis = new DataInputStream(in);
        try {
            int skip = offsets[slot];
            while (skip > 0) {
                long skipped = dis.skip(skip);
                if (skipped <= 0) {
                    if (dis.read() == -1) {
                        throw new IOException("Unexpected EOF in data.pak");
                    }
                    skip--;
                } else {
                    skip -= (int) skipped;
                }
            }
            byte[] buf = new byte[lengths[slot]];
            dis.readFully(buf);
            return buf;
        } finally {
            dis.close();
        }
    }

    public static int readU16BE(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    public static int readS16LE(byte[] b, int off) {
        return (short) ((b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8));
    }

    public static int readS32LE(byte[] b, int off) {
        return (b[off] & 0xFF)
            | ((b[off + 1] & 0xFF) << 8)
            | ((b[off + 2] & 0xFF) << 16)
            | ((b[off + 3] & 0xFF) << 24);
    }
}