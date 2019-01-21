package com.github.manevolent.ts3j.examples.audio;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import static com.github.manevolent.ts3j.examples.audio.TeamspeakFastMixerSink.AUDIO_FORMAT;

public class AudioSender {
    public static void main(String[] args) throws Exception {
        Ts3Debugging.setEnabled(false);

        LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

        LocalIdentity identity = LocalIdentity.generateNew(10);

        client.setIdentity(identity);
        client.setNickname(AudioSender.class.getSimpleName());
        client.setHWID("TestTestTest");

        TeamspeakFastMixerSink sink = new TeamspeakFastMixerSink(
                AUDIO_FORMAT,
                (int) AUDIO_FORMAT.getSampleRate() * AUDIO_FORMAT.getChannels() * 4 /*4=32bit float*/,
                new OpusParameters(
                        20,
                        96000, // 96kbps
                        10, // max complexity
                        0, // 0 expected packet loss
                        false, // no VBR
                        false, // no FEC
                        true // OPUS MUSIC - channel doesn't have to be Opus Music ;)
                )
        );

        client.setMicrophone(sink);

        // -1 to 1 (signed) positions of samples
        float[] thirtySecondsOfAudio = new float[(int) AUDIO_FORMAT.getSampleRate() * AUDIO_FORMAT.getChannels()];

        // Generate a sine wave
        for (int i = 0; i < thirtySecondsOfAudio.length; i ++) {
            thirtySecondsOfAudio[i] = (float) Math.sin(i / 60f);
        }

        sink.start();

        // Write into the sink
        sink.write(thirtySecondsOfAudio, thirtySecondsOfAudio.length);

        client.connect(
                args[0],
                10000L
        );

        sink.drain(); // this will block until the sink is stopped

        System.err.println("Encoder time taken = " + sink.getNanotime() + " ns");
        System.err.println("Audio position = " + sink.getPosition() + " samples sank");
        System.err.println("Encoder position = " + sink.getEncoderPosition() + " samples encoded");
        System.err.println("Network position = " + sink.getNetworkPosition() + " bytes sent");
        System.err.println("Packets encoded = " + sink.getPacketsEncoded() + " packets encoded");
        System.err.println("Packets sent = " + sink.getPacketsSent() + " packets sent");
        System.err.println("Overflow events = " + sink.getOverflows());

        // (resource starvation; can cause lost packets)
        System.err.println("Underflow events = " + sink.getUnderflows());

        client.disconnect("BYE");

        System.exit(0);
    }
}
