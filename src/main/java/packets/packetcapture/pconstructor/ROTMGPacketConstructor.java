package packets.packetcapture.pconstructor;

import util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Rotmg packet constructor appending bytes into a packet based on the size at the header of each sequence.
 */
public class ROTMGPacketConstructor {

    private static final int HEADER_SIZE = 5;
    private static final int MAX_PACKET_SIZE = 200000;

    private final PacketConstructor packetConstructor;
    private byte[] bytes = new byte[8192];
    private int index;

    /**
     * ROMGPacketConstructor needing the PacketConstructor class to send correctly stitched packets.
     *
     * @param pc PacketConstructor class needed to send correctly stitched packets.
     */
    public ROTMGPacketConstructor(PacketConstructor pc) {
        packetConstructor = pc;
    }

    /**
     * Build method used to stitch individual bytes in the data in the TCP packets according to
     * specified size at the header of the data.
     * Only start listen after the next packet less than MTU(maximum transmission unit packet) is received.
     *
     * @param data TCP packet with the data inside.
     */
    public void build(byte[] data) {
        if (data == null || data.length == 0) return;

        if (!ensureCapacity(index + data.length)) return;
        System.arraycopy(data, 0, bytes, index, data.length);
        index += data.length;
        drainPackets();
    }

    private void drainPackets() {
        while (index >= HEADER_SIZE) {
            int packetSize = Util.decodeInt(bytes);

            if (!isPlausiblePacket(packetSize)) {
                shiftLeft(1);
                continue;
            }

            if (index < packetSize) {
                return;
            }

            byte[] realmPacket = Arrays.copyOfRange(bytes, 0, packetSize);
            shiftLeft(packetSize);
            ByteBuffer packetData = ByteBuffer.wrap(realmPacket).order(ByteOrder.BIG_ENDIAN);
            packetConstructor.packetReceived(packetData);
        }
    }

    private boolean isPlausiblePacket(int packetSize) {
        return packetSize >= HEADER_SIZE && packetSize <= MAX_PACKET_SIZE;
    }

    private boolean ensureCapacity(int required) {
        if (required <= bytes.length) return true;

        int newSize = bytes.length;
        while (newSize < required && newSize < MAX_PACKET_SIZE) {
            newSize <<= 1;
        }
        if (newSize < required) {
            Util.printLogs("Oversize packet construction.");
            reset();
            return false;
        }

        bytes = Arrays.copyOf(bytes, newSize);
        return true;
    }

    private void shiftLeft(int count) {
        int remaining = index - count;
        if (remaining > 0) {
            System.arraycopy(bytes, count, bytes, 0, remaining);
        }
        index = remaining;
    }

    /**
     * Resets the byte index and the packet size.
     */
    public void reset() {
        index = 0;
    }
}
