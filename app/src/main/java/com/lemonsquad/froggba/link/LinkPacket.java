package com.lemonsquad.froggba.link;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Binary wire format for Link Cable communication.
 * Supports 2 to 4 players.
 *
 * Header (4 bytes):
 *   [0]: 'F'
 *   [1]: 'G'
 *   [2]: packet type
 *   [3]: player ID (0 = Master, 1..3 = Slave)
 */
public class LinkPacket {

    public static final byte MAGIC_0 = 'F';
    public static final byte MAGIC_1 = 'G';

    public static final byte TYPE_HELLO        = 0x01;
    public static final byte TYPE_HELLO_ACK    = 0x02;
    public static final byte TYPE_TRANSFER_REQ = 0x03;
    public static final byte TYPE_TRANSFER_ACK = 0x04;
    public static final byte TYPE_DISCONNECT   = 0x05;

    public final byte type;
    public final int playerId;
    public final int sessionId;
    public final int sequence;
    public final short localWord;
    public final short[] multiData; // only in resolved transfer (length 4)

    public LinkPacket(byte type, int playerId, int sessionId, int sequence, short localWord, short[] multiData) {
        this.type = type;
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.sequence = sequence;
        this.localWord = localWord;
        this.multiData = multiData;
    }

    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(MAGIC_0);
        buf.put(MAGIC_1);
        buf.put(type);
        buf.put((byte) playerId);
        buf.putInt(sessionId);
        buf.putInt(sequence);
        buf.putShort(localWord);
        if (multiData != null && multiData.length == 4) {
            for (short s : multiData) buf.putShort(s);
        }
        byte[] bytes = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, bytes, 0, bytes.length);
        return bytes;
    }

    public static LinkPacket deserialize(byte[] bytes) {
        if (bytes == null || bytes.length < 14) return null;
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (buf.get() != MAGIC_0 || buf.get() != MAGIC_1) return null;
        byte type = buf.get();
        int playerId = buf.get() & 0xFF;
        int sessionId = buf.getInt();
        int sequence = buf.getInt();
        short localWord = buf.getShort();

        short[] multi = null;
        if (buf.remaining() >= 8) {
            multi = new short[4];
            for (int i = 0; i < 4; ++i) multi[i] = buf.getShort();
        }

        return new LinkPacket(type, playerId, sessionId, sequence, localWord, multi);
    }
}
