package com.github.manevolent.ts3j.examples.audio;

import com.sun.jna.ptr.PointerByReference;
import net.tomp2p.opuswrapper.Opus;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Thread-safe opus encoder
 */
public class OpusEncoder implements AutoCloseable {
    private static final int[] OPUS_PERMITTED_SAMPLE_RATES =
            { 8000, 12000, 16000, 24000, 48000 };

    private static final int[] OPUS_PERMITTED_FRAME_SIZES =
            { 120, 240, 480, 960, 1920, 2880 };

    private static final int[] OPUS_PERMITTED_CHANNEL_COUNTS =
            { 1, 2 };

    private final PointerByReference encoder;
    private final int sampleRate, frameSize, channels;
    private final int expectedByteSize;
    private final boolean bigEndian;

    private final ShortBuffer sourceShortBuffer;
    private final ByteBuffer sourceByteBuffer;
    private final ByteBuffer targetBuffer;

    private final Object encoderLock = new Object();

    private boolean closed = false;

    public OpusEncoder(int sampleRate, int frameSize, int channels, boolean bigEndian) {
        this(sampleRate, frameSize, channels, bigEndian, 4096);
    }

    public OpusEncoder(int sampleRate, int frameSize,
                       int channels, boolean bigEndian,
                       int maxPacketLength) {
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        this.bigEndian = bigEndian;
        this.expectedByteSize = frameSize * channels * 2;

        if (!ArrayUtils.contains(OPUS_PERMITTED_FRAME_SIZES, frameSize))
            throw new IllegalArgumentException("Invalid Opus frame size: " + frameSize);

        if (!ArrayUtils.contains(OPUS_PERMITTED_SAMPLE_RATES, sampleRate))
            throw new IllegalArgumentException("Invalid Opus sample rate: " + sampleRate);

        if (!ArrayUtils.contains(OPUS_PERMITTED_CHANNEL_COUNTS, channels))
            throw new IllegalArgumentException("Invalid Opus channel count: " + channels);

        this.sourceShortBuffer = ShortBuffer.allocate(frameSize * channels);
        this.sourceByteBuffer = ByteBuffer.allocate(frameSize * channels * 2);
        this.sourceByteBuffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        this.targetBuffer = ByteBuffer.allocate(maxPacketLength);

        IntBuffer errorBuffer = IntBuffer.allocate(1);

        encoder = Opus.INSTANCE.opus_encoder_create(
                sampleRate,
                channels,
                Opus.OPUS_APPLICATION_AUDIO,
                errorBuffer
        );

        if (encoder == null) throw new NullPointerException("encoder");

        OpusUtil.checkError(
                "opus_encoder_create",
                errorBuffer.get()
        );
    }

    public void reset() {
        synchronized (encoderLock) {
            OpusUtil.checkError(
                    "opus_encoder_ctl/OPUS_RESET_STATE",
                    Opus.INSTANCE.opus_encoder_ctl(encoder, Opus.OPUS_RESET_STATE)
            );
        }
    }

    public int setEncoderValue(int field, Object... values) {
        synchronized (encoderLock) {
            if (closed) throw new IllegalStateException("encoder closed");

            return OpusUtil.checkError(
                    "opus_encoder_ctl/" + field,
                    Opus.INSTANCE.opus_encoder_ctl(encoder, field, values)
            );
        }
    }

    public byte[] encode(float[] floats, int len) {
        if (len != getFrameSize() * getChannels())
            throw new IllegalArgumentException(len + " != " + getFrameSize() * getChannels());

        targetBuffer.clear();

        int result;
        synchronized (encoderLock) {
            if (closed) throw new IllegalStateException("encoder closed");

            if (encoder == null) throw new IllegalStateException("encoder is null");

            result = Opus.INSTANCE.opus_encode_float(
                    encoder,
                    floats,
                    frameSize, // this is STATIC.
                    targetBuffer,
                    targetBuffer.capacity()
            );
        }

        OpusUtil.checkError(
                "opus_encode_float," +
                        " ch=" + channels +
                        " smprate=" + sampleRate +
                        " rate=" + sampleRate +
                        " len=" + len + "/" + floats.length + ", frame_size=" + frameSize +
                        ", max_data_bytes=" + targetBuffer.capacity(),
                result
        );

        byte[] encoded = new byte[result];
        targetBuffer.get(encoded);

        return encoded;
    }

    public byte[] encode(float[] floats) {
        return encode(floats, floats.length);
    }

    public byte[] encode(byte[] pcm) {
        if (pcm.length != expectedByteSize)
            throw new IllegalArgumentException(pcm.length + " != " + expectedByteSize);

        // Clear source and input the PCM samples
        sourceByteBuffer.clear();
        sourceByteBuffer.put(pcm);
        sourceByteBuffer.flip();

        sourceShortBuffer.clear();
        while (sourceByteBuffer.position() < sourceByteBuffer.limit())
            sourceShortBuffer.put(sourceByteBuffer.getShort());

        // Clear target
        sourceShortBuffer.flip();
        targetBuffer.clear();

        int result;

        synchronized (encoderLock) {
            if (closed) throw new IllegalStateException("encoder closed");

            result = Opus.INSTANCE.opus_encode(
                    encoder,
                    sourceShortBuffer,
                    frameSize, // this is STATIC.
                    targetBuffer,
                    targetBuffer.capacity()
            );
        }

        OpusUtil.checkError("opus_encode", result);

        byte[] encoded = new byte[result];
        targetBuffer.get(encoded);

        return encoded;
    }

    public int getFrameSize() { return frameSize; }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    @Override
    public void close() {
        synchronized (encoderLock) {
            if (closed) throw new IllegalStateException("already closed");
            Opus.INSTANCE.opus_encoder_destroy(encoder);
            closed = true;
        }
    }
}
