package com.github.manevolent.ts3j.examples.audio;

import com.github.manevolent.ffmpeg4j.*;
import com.github.manevolent.ffmpeg4j.filter.audio.FFmpegAudioResampleFilter;
import com.github.manevolent.ffmpeg4j.source.FFmpegAudioSourceSubstream;
import com.github.manevolent.ffmpeg4j.stream.source.FFmpegSourceStream;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.github.manevolent.ts3j.examples.audio.TeamspeakFastMixerSink.AUDIO_FORMAT;

public class MusicPlayer {
    public static void main(String[] args) throws Exception {
        Ts3Debugging.setEnabled(false);

        // Open file
        FFmpeg.register();
        InputStream inputStream = new FileInputStream("bensound-ukulele.mp3");
        FFmpegInput input = new FFmpegInput(inputStream);
        FFmpegSourceStream stream = input.open(FFmpeg.getInputFormatByName("mp3"));
        FFmpegAudioSourceSubstream audioSourceSubstream =
                (FFmpegAudioSourceSubstream) stream.registerStreams()
                .stream()
                .filter(x -> x.getMediaType() == MediaType.AUDIO)
                .findFirst().orElse(null);

        if (audioSourceSubstream == null) throw new NullPointerException("ya dun goofed, bud");

        // Teamspeak start up
        LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();
        LocalIdentity identity = LocalIdentity.generateNew(10);
        client.setIdentity(identity);
        client.setNickname(MusicPlayer.class.getSimpleName());
        client.setHWID("TestTestTest");

        // Create a sink
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

        sink.start();

        client.connect(
                "your.teamspeak.server",
                10000L
        );

        int bufferSize = AUDIO_FORMAT.getChannels() * (int) AUDIO_FORMAT.getSampleRate(); // Just to keep it orderly
        FFmpegAudioResampleFilter resampleFilter = new FFmpegAudioResampleFilter(
                audioSourceSubstream.getFormat(),
                new AudioFormat(
                        (int) AUDIO_FORMAT.getSampleRate(),
                        AUDIO_FORMAT.getChannels(),
                        FFmpeg.guessFFMpegChannelLayout(AUDIO_FORMAT.getChannels())
                ),
                bufferSize
        );

        Queue<AudioFrame> frameQueue = new LinkedBlockingQueue<>();
        AudioFrame currentFrame = null;
        int frameOffset = 0; // offset within current frame

        long wake = System.nanoTime();
        long delay = 150 * 1_000_000; // 50ms interval
        long sleep;
        while (true) {
            int available = sink.availableInput();

            if (available > 0) {
                if (currentFrame == null || frameOffset >= currentFrame.getLength()) {
                    if (frameQueue.peek() == null) {
                        try {
                            frameQueue.addAll(resampleFilter.apply(audioSourceSubstream.next()));
                        } catch (EOFException ex) {
                            // flush currentFrame
                            break;
                        }
                    }

                    currentFrame = frameQueue.remove();
                    frameOffset = 0;
                }

                int write = Math.min(currentFrame.getLength() - frameOffset, available);

                sink.write(
                        currentFrame.getSamples(),
                        frameOffset,
                        write
                );

                frameOffset += write;

                continue;
            }

            wake += delay;
            sleep = (wake - System.nanoTime()) / 1_000_000;

            if (sleep > 0) Thread.sleep(sleep);
        }

        System.err.println("Draining...");

        sink.drain(); // make sure TS gets everything

        client.disconnect("BYE");

        System.err.println("Encoder time taken = " + sink.getNanotime() + " ns");
        System.err.println("Audio position = " + sink.getPosition() + " samples sank");
        System.err.println("Encoder position = " + sink.getEncoderPosition() + " samples encoded");
        System.err.println("Network position = " + sink.getNetworkPosition() + " bytes sent");
        System.err.println("Packets encoded = " + sink.getPacketsEncoded() + " packets encoded");
        System.err.println("Packets sent = " + sink.getPacketsSent() + " packets sent");
        System.err.println("Overflow events = " + sink.getOverflows());

        // (resource starvation; can cause lost packets)
        System.err.println("Underflow events = " + sink.getUnderflows());

        System.exit(0);
    }
}
