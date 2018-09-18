package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.manevolent.ts3j.protocol.packet.PacketBodyType.INIT1;

public class PacketBody8Init1 extends PacketBody {
    private byte[] version = new byte[4]; // 4 bytes
    private Step step; // step instance

    public PacketBody8Init1(ProtocolRole role) {
        // sec. 2 of https://github.com/ReSpeak/tsdeclarations/blob/master/ts3protocol.md
        // (The packet header values are set as following for all packets here:)
        super(INIT1, role);
    }

    public PacketBody8Init1(ProtocolRole role, Step step) {
        this(role);

        setStep(step);
    }

    public byte[] getVersion() {
        return version;
    }

    public void setVersion(byte[] version) {
        assertArray("version", version, 4);
        this.version = version;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        if (step == null)
            throw new NullPointerException("step");

        if (step.getRole() != getRole())
            throw new IllegalArgumentException(
                    "Init1 step mismatches expected role: " + step.getRole().name() +
                            " != " + getRole().name()
            );

        this.step = step;
    }

    @Override
    public void write(ByteBuffer buffer) {
        // only the client sends the version
        if (getRole() == ProtocolRole.CLIENT)
            buffer.put(getVersion());

        buffer.put(getStep().getNumber());

        step.write(buffer);
    }

    @Override
    public void read(ByteBuffer buffer) {
        // only the client sends a version
        if (getRole() == ProtocolRole.CLIENT)
            buffer.get(version);

        byte stepNumber = buffer.get();

        Step step;

        switch (stepNumber) {
            case 0:
                step = new Step0();
                break;
            case 1:
                step = new Step1();
                break;
            case 2:
                step = new Step2();
                break;
            case 3:
                step = new Step3();
                break;
            case 4:
                step = new Step4();
                break;
            case 127:
                step = new Step127();
                break;
            default:
                throw new IllegalArgumentException("invalid Init1 step: " + stepNumber);
        }

        step.read(buffer);

        setStep(step);
    }

    @Override
    public int getSize() {
        return 4 + 1 + (step == null ? 0 : step.getSize());
    }

    public static abstract class Step {
        private final byte stepNumber;
        private final ProtocolRole role;

        protected Step(byte stepNumber, ProtocolRole role) {
            this.stepNumber = stepNumber;
            this.role = role;
        }

        public byte getNumber() {
            return stepNumber;
        }

        public ProtocolRole getRole() {
            return role;
        }

        public abstract void read(ByteBuffer byteBuffer);
        public abstract void write(ByteBuffer byteBuffer);
        public abstract int getSize();
    }

    public static class Step0 extends Step {
        private byte[] timestamp = new byte[4]; // 4 bytes
        private byte[] random = new byte[4]; // 4 bytes
        private final byte[] reserved = new byte[8]; // 8 zeros, these are reserved for future use

        public Step0() {
            super((byte) 0, ProtocolRole.CLIENT);
        }

        public byte[] getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            ByteBuffer intBuffer = ByteBuffer.allocate(4);

            intBuffer.order(ByteOrder.BIG_ENDIAN);
            intBuffer.putInt(timestamp);

            setTimestamp(intBuffer.array());
        }

        public void setTimestamp(byte[] timestamp) {
            PacketBody.assertArray("timestamp", timestamp, 4);
            this.timestamp = timestamp;
        }

        public byte[] getRandom() {
            return random;
        }

        public void setRandom(byte[] random) {
            PacketBody.assertArray("random", random, 4);
            this.random = random;
        }

        @Override
        public void read(ByteBuffer byteBuffer) {
            byteBuffer.get(timestamp);
            byteBuffer.get(random);
            byteBuffer.get(reserved);
        }

        @Override
        public void write(ByteBuffer byteBuffer) {
            byteBuffer.put(timestamp);
            byteBuffer.put(random);
            byteBuffer.put(reserved);
        }

        @Override
        public int getSize() {
            return 4 + 4 + 8;
        }
    }

    public static class Step1 extends Step {
        private byte[] serverStuff = new byte[16]; // 16 bytes
        private byte[] a0reversed = new byte[4]; // 4 bytes / The bytes from [A0] in reversed order

        public Step1() {
            super((byte) 1, ProtocolRole.SERVER);
        }


        public byte[] getA0reversed() {
            return a0reversed;
        }

        public void setA0reversed(byte[] a0reversed) {
            PacketBody.assertArray("a0reversed", a0reversed, 4);
            this.a0reversed = a0reversed;
        }

        public byte[] getServerStuff() {
            return serverStuff;
        }

        public void setServerStuff(byte[] serverStuff) {
            PacketBody.assertArray("serverStuff", serverStuff, 16);
            this.serverStuff = serverStuff;
        }

        @Override
        public void read(ByteBuffer byteBuffer) {
            byteBuffer.get(serverStuff);
            byteBuffer.get(a0reversed);
        }

        @Override
        public void write(ByteBuffer byteBuffer) {
            byteBuffer.put(serverStuff);
            byteBuffer.put(a0reversed);
        }

        @Override
        public int getSize() {
            return 16 + 4;
        }
    }

    public static class Step2 extends Step {
        private byte[] serverStuff = new byte[16]; // 16 bytes
        private byte[] a0reversed = new byte[4]; // 4 bytes / The bytes from [A0] in reversed order

        public Step2() {
            super((byte) 2, ProtocolRole.CLIENT);
        }


        public byte[] getA0reversed() {
            return a0reversed;
        }

        public void setA0reversed(byte[] a0reversed) {
            PacketBody.assertArray("a0reversed", a0reversed, 4);
            this.a0reversed = a0reversed;
        }

        public byte[] getServerStuff() {
            return serverStuff;
        }

        public void setServerStuff(byte[] serverStuff) {
            PacketBody.assertArray("serverStuff", serverStuff, 16);
            this.serverStuff = serverStuff;
        }

        @Override
        public void read(ByteBuffer byteBuffer) {
            byteBuffer.get(serverStuff);
            byteBuffer.get(a0reversed);
        }

        @Override
        public void write(ByteBuffer byteBuffer) {
            byteBuffer.put(serverStuff);
            byteBuffer.put(a0reversed);
        }

        @Override
        public int getSize() {
            return 16 + 4;
        }
    }

    public static class Step3 extends Step {
        private byte[] x = new byte[64]; // x
        private byte[] n = new byte[64]; // n
        private int level;
        private byte[] serverStuff = new byte[100]; // must be a lot of really important server stuff lol

        public Step3() {
            super((byte) 3, ProtocolRole.SERVER);
        }

        public byte[] getX() {
            return x;
        }

        public void setX(byte[] x) {
            PacketBody.assertArray("x", x, 64);
            this.x = x;
        }

        public byte[] getN() {
            return n;
        }

        public void setN(byte[] n) {
            PacketBody.assertArray("n", n, 64);
            this.n = n;
        }

        public byte[] getServerStuff() {
            return serverStuff;
        }

        public void setServerStuff(byte[] serverStuff) {
            PacketBody.assertArray("serverStuff", serverStuff, 100);
            this.serverStuff = serverStuff;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        @Override
        public void read(ByteBuffer byteBuffer) {
            byteBuffer.get(x);
            byteBuffer.get(n);
            level = byteBuffer.getInt();
            byteBuffer.get(serverStuff);
        }

        @Override
        public void write(ByteBuffer byteBuffer) {
            byteBuffer.put(x);
            byteBuffer.put(n);
            byteBuffer.putInt(level);
            byteBuffer.put(serverStuff);
        }

        @Override
        public int getSize() {
            return 64 + 64 + 4 + 100;
        }
    }

    public static class Step4 extends Step {
        private byte[] x = new byte[64]; // x
        private byte[] n = new byte[64]; // n
        private int level;
        private byte[] serverStuff = new byte[100]; // a2 echo from step 3
        private byte[] y = new byte[64]; // y 0-paddded from the left
        private byte[] clientivcommand = new byte[0]; // clientivcommand

        public Step4() {
            super((byte) 4, ProtocolRole.CLIENT);
        }

        public byte[] getX() {
            return serverStuff;
        }

        public void setX(byte[] x) {
            PacketBody.assertArray("x", x, 64);
            this.x = x;
        }

        public byte[] getN() {
            return serverStuff;
        }

        public void setN(byte[] n) {
            PacketBody.assertArray("n", n, 64);
            this.n = n;
        }

        public byte[] getServerStuff() {
            return serverStuff;
        }

        public void setServerStuff(byte[] serverStuff) {
            PacketBody.assertArray("serverStuff", serverStuff, 100);
            this.serverStuff = serverStuff;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public byte[] getY() {
            return y;
        }

        public void setY(byte[] y) {
            PacketBody.assertArray("y", y, 64);
            this.y = y;
        }

        public byte[] getClientIVCommand() {
            return clientivcommand;
        }

        public void setClientIVcommand(byte[] clientivcommand) {
            this.clientivcommand = clientivcommand;
        }

        @Override
        public void read(ByteBuffer byteBuffer) {
            byteBuffer.get(x);
            byteBuffer.get(n);
            level = byteBuffer.getInt();
            byteBuffer.get(serverStuff);
            byteBuffer.get(y);

            clientivcommand = new byte[byteBuffer.remaining()];
            byteBuffer.get(clientivcommand);
        }

        @Override
        public void write(ByteBuffer byteBuffer) {
            byteBuffer.put(x);
            byteBuffer.put(n);
            byteBuffer.putInt(level);
            byteBuffer.put(serverStuff);
            byteBuffer.put(y);

            byteBuffer.put(clientivcommand);
        }

        @Override
        public int getSize() {
            return 64 + 64 + 4 + 100 + 64 + clientivcommand.length;
        }
    }

    /**
     * Retry
     */
    public static class Step127 extends Step {
        public Step127() {
            super((byte)127, ProtocolRole.SERVER);
        }

        @Override
        public void read(ByteBuffer byteBuffer) {

        }

        @Override
        public void write(ByteBuffer byteBuffer) {

        }

        @Override
        public int getSize() {
            return 0;
        }
    }
}
