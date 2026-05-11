package packets.packetcapture.pconstructor;

import packets.Packet;
import packets.PacketType;
import packets.packetcapture.PacketProcessor;
import packets.packetcapture.encryption.RC4;
import packets.packetcapture.encryption.TickAligner;
import packets.reader.BufferReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet constructor sending the TCP packets to the stream constructor that in turn sends the
 * ordered packets to the rotmg constructor. The packets are then sent back to be decrypted with
 * an RC4 cipher. If a tick packet is sent an aligner is used to check the RC4 alignment using a
 * simple increment from the previous tick packet. If a new session packet is received then it
 * resets the cipher.
 */
public class PacketConstructor {

    private final RC4 rc4Cipher;
    private final PacketProcessor packetProcessor;
    private final ROTMGPacketConstructor rotmgConst;
    private final TickAligner tickAligner;
    private final List<byte[]> pendingEncryptedPackets = new ArrayList<>();
    private static final int MAX_PENDING_PACKET_BYTES = 2_000_000;
    private int pendingPacketBytes;
    private int pendingPayloadBytes;
    private int firstSyncCandidatePayloadOffset = -1;
    private int pendingMapInfoPackets;

    /**
     * Packet constructor with specific cipher.
     *
     * @param pp Parent class to send constructed packets back too.
     * @param r  The cipher used to decode packets.
     */
    public PacketConstructor(PacketProcessor pp, RC4 r) {
        packetProcessor = pp;
        rc4Cipher = r;
        rotmgConst = new ROTMGPacketConstructor(this);
        tickAligner = new TickAligner(rc4Cipher);
    }

    /**
     * Build method to send the packets retrieved by the sniffer for constructing.
     *
     * @param data Raw packet data incoming from the net tap.
     */
    public void build(byte[] data) {
        rotmgConst.build(data);
    }

    /**
     * Rotmg packets constructed by the rotmg constructor are sent back after they
     * are correctly assembled. If the cipher is correctly aligned then the packets
     * are decrypted and sent to the packet processor. If the cipher isn't aligned
     * then the Tick packets are used to re-align the cipher.
     *
     * @param encryptedData Encrypted packets for aligning cipher and decryption.
     */
    public void packetReceived(ByteBuffer encryptedData) {
        try {
            byte[] encryptedPacket = encryptedData.array().clone();
            int size = encryptedData.getInt();
            int type = Byte.toUnsignedInt(encryptedData.get());
            if (!tickAligner.isSynced() && !tickAligner.hasFirstSyncCandidate() && isSyncPacket(type)) {
                firstSyncCandidatePayloadOffset = pendingPayloadBytes;
            }

            boolean sync = tickAligner.checkRC4Alignment(encryptedData, size, type);

            if (sync) {
                processEncryptedPacket(encryptedPacket);
            } else {
                addPendingPacket(encryptedPacket, type);
                if (tickAligner.isSynced()) {
                    replayPendingPackets();
                } else if (isSyncPacket(type) && !tickAligner.hasFirstSyncCandidate()) {
                    firstSyncCandidatePayloadOffset = -1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPendingPacket(byte[] encryptedPacket, int type) {
        pendingEncryptedPackets.add(encryptedPacket);
        pendingPacketBytes += encryptedPacket.length;
        pendingPayloadBytes += encryptedPacket.length - 5;
        if (type == PacketType.MAPINFO.getIndex()) {
            pendingMapInfoPackets++;
        }
        if (pendingPacketBytes > MAX_PENDING_PACKET_BYTES) {
            clearPendingPackets(false);
        }
    }

    private void replayPendingPackets() {
        int startOffset = tickAligner.getLastSyncOffset() - firstSyncCandidatePayloadOffset;
        if (startOffset < 0) {
            clearPendingPackets(false);
            return;
        }

        rc4Cipher.reset();
        rc4Cipher.skip(startOffset);
        List<byte[]> packets = new ArrayList<>(pendingEncryptedPackets);
        clearPendingPackets(false);

        for (byte[] packet : packets) {
            processEncryptedPacket(packet);
        }
    }

    private void processEncryptedPacket(byte[] encryptedPacket) {
        ByteBuffer packetData = ByteBuffer.wrap(encryptedPacket).order(ByteOrder.BIG_ENDIAN);
        int size = packetData.getInt();
        int type = Byte.toUnsignedInt(packetData.get());
        rc4Cipher.decrypt(5, packetData);
        packetProcessor.processPackets(type, size, packetData);
    }

    private boolean recoverPendingMapInfoFromSessionStart() {
        boolean recovered = false;
        for (byte[] encryptedPacket : pendingEncryptedPackets) {
            ByteBuffer header = ByteBuffer.wrap(encryptedPacket).order(ByteOrder.BIG_ENDIAN);
            int type = Byte.toUnsignedInt(header.get(4));
            if (type != PacketType.MAPINFO.getIndex()) {
                continue;
            }

            byte[] decryptedPacket = encryptedPacket.clone();
            RC4 probe = rc4Cipher.fork();
            probe.reset();
            probe.decrypt(5, decryptedPacket);

            if (!isValidMapInfoPacket(decryptedPacket)) {
                continue;
            }

            ByteBuffer packetData = ByteBuffer.wrap(decryptedPacket).order(ByteOrder.BIG_ENDIAN);
            int recoveredSize = packetData.getInt();
            int recoveredType = Byte.toUnsignedInt(packetData.get());
            packetProcessor.processPackets(recoveredType, recoveredSize, packetData);
            recovered = true;
        }
        return recovered;
    }

    private boolean isValidMapInfoPacket(byte[] decryptedPacket) {
        try {
            ByteBuffer packetData = ByteBuffer.wrap(decryptedPacket).order(ByteOrder.BIG_ENDIAN);
            packetData.getInt();
            int type = Byte.toUnsignedInt(packetData.get());
            if (type != PacketType.MAPINFO.getIndex()) {
                return false;
            }

            Packet packet = PacketType.getPacket(type).factory();
            packet.deserialize(new BufferReader(packetData));
            return packetData.position() == packetData.capacity();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSyncPacket(int type) {
        return type == PacketType.NEWTICK.getIndex() || type == PacketType.MOVE.getIndex();
    }

    private void clearPendingPackets(boolean recoverMapInfo) {
        if (recoverMapInfo && pendingMapInfoPackets > 0) {
            recoverPendingMapInfoFromSessionStart();
        }

        pendingEncryptedPackets.clear();
        pendingPacketBytes = 0;
        pendingPayloadBytes = 0;
        firstSyncCandidatePayloadOffset = -1;
        pendingMapInfoPackets = 0;
    }

    /**
     * Reset method to reset both cipher and the aligner tick counter when a reset packet is received.
     */
    public void reset() {
        rc4Cipher.reset();
        tickAligner.reset();
        rotmgConst.reset();
        clearPendingPackets(true);
    }

    /**
     * Reset when starting the sniffer. Given the program can start at any time then any packet which
     * follows a non-max packet will most likely contain the rotmg-packet header which contains the
     * packet size. If ignoring this flag, any random MTU(maximum transmission unit packet) packet in
     * a sequence of concatenated packets could produce a random packet size from its first 4 bytes
     * resulting in a de-sync.
     */
    public void startResets() {
        reset();
    }
}
