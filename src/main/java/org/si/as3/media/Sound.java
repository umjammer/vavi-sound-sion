package org.si.as3.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.si.utils.ByteArray;
import org.si.utils.Event;
import org.si.utils.EventDispatcher;
import org.si.utils.IOErrorEvent;
import org.si.utils.ProgressEvent;
import org.si.as3.net.URLRequest;


public class Sound extends EventDispatcher {

    public int bytesLoaded = 0;
    public int bytesTotal = 0;
    public double length = 0;

    private float[] stereoSamples = new float[0];
    private float sampleRate = 44100f;
    private int extractPosition;

    public Sound() {
    }

    public Sound(URLRequest url, SoundLoaderContext context) {
        load(url, context);
    }

    public void load(URLRequest url, SoundLoaderContext context) {
        if (url == null || url.url == null) {
            dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, "URL is null"));
            return;
        }

        dispatchEvent(new Event(Event.OPEN));

        try {
            byte[] data = readAllBytes(url.url);
            loadBytes(data);
        } catch (Exception e) {
            dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, e.getMessage()));
        }
    }

    public void loadBytes(byte[] data) {
        if (data == null) {
            dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, "data is null"));
            return;
        }
        bytesTotal = data.length;
        bytesLoaded = data.length;
        dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS, false, false, bytesLoaded, bytesTotal));
        decodeAudio(data);
        dispatchEvent(new Event(Event.COMPLETE));
    }

    public int extract(ByteArray extractedByteArray, int sampleCount, int startPosition) {
        if (extractedByteArray == null || stereoSamples.length == 0) {
            return 0;
        }
        if (sampleCount <= 0) {
            return 0;
        }

        int start = startPosition;
        if (start < 0) {
            start = extractPosition;
        }

        int totalFrames = stereoSamples.length >> 1;
        if (start >= totalFrames) {
            return 0;
        }
        int frames = Math.min(sampleCount, totalFrames - start);
        int sampleIndex = start << 1;
        for (int i = 0; i < frames; i++) {
            extractedByteArray.writeFloat(stereoSamples[sampleIndex++]);
            extractedByteArray.writeFloat(stereoSamples[sampleIndex++]);
        }
        extractPosition = start + frames;
        return frames;
    }

    public void setStereoSamples(double[] samples, int channels, float sampleRate) {
        if (samples == null || samples.length == 0) {
            stereoSamples = new float[0];
            this.sampleRate = sampleRate;
            this.length = 0;
            return;
        }
        this.sampleRate = sampleRate;
        int frameCount = samples.length / Math.max(channels, 1);
        stereoSamples = new float[frameCount * 2];
        int src = 0;
        int dst = 0;
        for (int i = 0; i < frameCount; i++) {
            float left;
            float right;
            if (channels <= 1) {
                left = (float) samples[src++];
                right = left;
            } else {
                left = (float) samples[src++];
                right = (float) samples[src++];
                src += channels - 2;
            }
            stereoSamples[dst++] = left;
            stereoSamples[dst++] = right;
        }
        bytesTotal = bytesLoaded = stereoSamples.length * Float.BYTES;
        length = (frameCount * 1000.0) / this.sampleRate;
        extractPosition = 0;
    }

    private void decodeAudio(byte[] data) {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
            AudioFormat srcFormat = source.getFormat();
            int channels = Math.max(1, srcFormat.getChannels());
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    srcFormat.getSampleRate(),
                    16,
                    channels,
                    channels * 2,
                    srcFormat.getSampleRate(),
                    false
            );
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(pcmFormat, source)) {
                byte[] pcmBytes = pcm.readAllBytes();
                int frames = pcmBytes.length / (channels * 2);
                stereoSamples = new float[frames * 2];
                int p = 0;
                int s = 0;
                for (int i = 0; i < frames; i++) {
                    float left = 0;
                    float right = 0;
                    for (int ch = 0; ch < channels; ch++) {
                        int lo = pcmBytes[p++] & 0xff;
                        int hi = pcmBytes[p++] << 8;
                        float value = (short) (hi | lo) / 32768f;
                        if (ch == 0) {
                            left = value;
                        } else if (ch == 1) {
                            right = value;
                        }
                    }
                    if (channels == 1) {
                        right = left;
                    }
                    stereoSamples[s++] = left;
                    stereoSamples[s++] = right;
                }
                sampleRate = pcmFormat.getSampleRate();
                length = (frames * 1000.0) / sampleRate;
                extractPosition = 0;
            }
        } catch (Exception e) {
            stereoSamples = new float[0];
            sampleRate = 44100f;
            length = 0;
            extractPosition = 0;
        }
    }

    private byte[] readAllBytes(String location) throws IOException {
        if (location.contains("://")) {
            try (InputStream is = new URL(location).openStream()) {
                return is.readAllBytes();
            }
        }

        Path path;
        try {
            path = Path.of(URI.create(location));
        } catch (IllegalArgumentException e) {
            path = Path.of(location);
        }
        return Files.readAllBytes(path);
    }
}
