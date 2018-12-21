package com.github.manevolent.ts3j.examples.audio;

import com.sun.jna.ptr.PointerByReference;
import net.tomp2p.opuswrapper.Opus;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class OpusDecoder implements AutoCloseable {
    private final PointerByReference decoder;
    private final int sampleRate, frameSize, channels;

    public OpusDecoder(int sampleRate, int frameSize, int channels) {
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;

        IntBuffer errorBuffer = IntBuffer.allocate(1);

        decoder = Opus.INSTANCE.opus_decoder_create(
                sampleRate,
                channels,
                errorBuffer
        );

        OpusUtil.checkError("opus_decoder_create", errorBuffer.get());
    }

    public int reset() {
        return OpusUtil.checkError(
                "opus_decoder_ctl/OPUS_RESET_STATE",
                Opus.INSTANCE.opus_decoder_ctl(decoder, Opus.OPUS_RESET_STATE)
        );
    }

    public int setDecoderValue(int field, Object... values) {
        return OpusUtil.checkError(
                "opus_decoder_ctl/" + field,
                Opus.INSTANCE.opus_decoder_ctl(decoder, field, values)
        );
    }

    public int decodePLC(float[] floats) {
        return decode(null, floats);
    }

    public int decode(byte[] packet, float[] floats) {
        if (floats == null)
            throw new NullPointerException("floats");
        else if (floats.length < frameSize * channels)
            throw new IllegalArgumentException(floats.length + " < " + frameSize * channels);

        int result = Opus.INSTANCE.opus_decode_float(
                decoder,
                packet,
                packet == null ? 0 : packet.length,
                FloatBuffer.wrap(floats),
                frameSize,
                0
        );

        OpusUtil.checkError("opus_decode_float", result);

        return result;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    @Override
    public void close() {
        Opus.INSTANCE.opus_decoder_destroy(decoder);
    }

    public static String getVersion() {
        return Opus.INSTANCE.opus_get_version_string();
    }
}
