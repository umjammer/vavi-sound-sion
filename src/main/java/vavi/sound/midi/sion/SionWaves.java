/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.sequencer.SiMMLTrack;
import vavi.sound.adpcm.ma.MaInputStream;

import static java.lang.System.getLogger;


/**
 * The waves of an MFi or SMAF file - stream PCM and the waves of wave table (WT) voices -
 * played by the PCM channel of SiON (module type 7), the one it has for sampled sound.
 * <p>
 * A stream wave is played as it is: it is resampled to the rate of the driver once, when it
 * arrives, and a track of its own keys it on and off as the file says. A WT wave becomes
 * the {@link SiONVoice} of the voice which plays it, see {@link #wtVoice}.
 * </p>
 * <p>
 * What is heavy here - decoding, resampling and the log transform of SiON - is done where
 * the data arrives, that is in {@link javax.sound.midi.Receiver#send}, not in the audio
 * thread. The methods returning {@link Runnable} give what is left for the audio thread.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-18 nsano initial version <br>
 * @see "vavi.sound.midi.ymf262.NukedWaveTable of vavi-apps-mfiplayer"
 */
class SionWaves {

    private static final Logger logger = getLogger(SionWaves.class.getName());

    /** the note a stream is keyed on at, any will do, it is resampled to be played 1:1 */
    private static final int STREAM_NOTE = 69;

    /** the key a WT voice sounds its sampling rate at */
    private static final int WT_BASE_KEY = 60;

    /**
     * what a WT voice is attenuated by [TL, 0.75dB], a wave is full scale and several of them
     * sum up over it, 8: -6dB, the gain 0.5 of {@code NukedWaveTable}
     */
    private static final int WT_HEADROOM = 8;

    /** the track ids of streams, apart from the ones of the midi module */
    private static final int STREAM_TRACK_ID = 0x8000;

    /** mfi audio channels */
    private static final int AUDIO_CHANNELS = 4;

    /** a stream wave */
    private static class Stream {
        final SiONVoice voice;
        /** 0 ~ 127, -1: the audio channel's, see {@code 43 79 0x 7f 0b} */
        int panpot = -1;
        /** the stream which starts with this one, -1: none, see {@code 43 79 0x 7f 08} */
        int pair = -1;
        /** the track which plays this, allocated when it is first played */
        SiMMLTrack track;

        Stream(SiONVoice voice) {
            this.voice = voice;
        }
    }

    private final SiONDriver driver;

    /** stream id -> stream, touched by the audio thread only */
    private final Map<Integer, Stream> streams = new HashMap<>();

    /** the volume of an mfi audio channel, 0 ~ 127, touched by the audio thread only */
    private final int[] audioVolumes = {127, 127, 127, 127};

    /** the panpot of an mfi audio channel, 0 ~ 127, touched by the audio thread only */
    private final int[] audioPanpots = {64, 64, 64, 64};

    /** wave id -> 16 bit pcm of a WT wave */
    private final Map<Integer, short[]> waves = new HashMap<>();

    SionWaves(SiONDriver driver) {
        this.driver = driver;
    }

    // ---- stream

    /**
     * A stream wave, see {@code vavi.sound.mobile.MobileExclusive#wave}.
     *
     * @param format 1, 0x82: 4 bit yamaha adpcm, 0: 8/16 bit signed pcm, 4: 16 bit, 5: offset binary pcm
     * @param channels 1, 2
     * @param bits 4 for adpcm, 8 or 16 for pcm
     * @param data a stereo adpcm wave is L then R, a stereo pcm one interleaved
     * @return what the audio thread does, null when it is not supported
     */
    Runnable setStream(int id, int format, int channels, int bits, int samplingRate, byte[] data) {
        if (samplingRate < 1 || channels < 1 || channels > 2) {
logger.log(Level.WARNING, "stream wave not supported: No.%d, %dHz, %d ch".formatted(id, samplingRate, channels));
            return null;
        }
        short[][] pcm = new short[channels][];
        switch (format) {
            case 1, 0x82 -> {
                int half = data.length / channels;
                for (int c = 0; c < channels; c++) {
                    pcm[c] = decodeAdpcm(data, half * c, half);
                }
            }
            case 0, 4, 5 -> {
                int bytes = bits > 8 ? 2 : 1;
                int frames = data.length / bytes / channels;
                for (int c = 0; c < channels; c++) {
                    pcm[c] = new short[frames];
                }
                for (int i = 0; i < frames; i++) {
                    for (int c = 0; c < channels; c++) {
                        int p = (i * channels + c) * bytes;
                        int sample = bytes == 2 ? ((data[p] & 0xff) << 8) | (data[p + 1] & 0xff) : (data[p] & 0xff) << 8;
                        if (format == 5) {
                            sample ^= 0x8000; // offset binary to 2's complement
                        }
                        pcm[c][i] = (short) sample;
                    }
                }
            }
            default -> {
logger.log(Level.WARNING, "stream wave format not supported: No.%d, %02x".formatted(id, format));
                return null;
            }
        }

        // played 1:1, the rate of a stream never changes and SiON does not interpolate
        double[] wave = resample(pcm, samplingRate, driver.getSampleRate());
        SiONVoice voice = new SiONVoice();
        SiOPMWavePCMData pcmData = voice.setPCMWave(0, wave, STREAM_NOTE, 0, 127, channels, channels);
logger.log(Level.DEBUG, "stream wave: No.%d, %02x, %dHz, %d ch, %d samples".formatted(id, format, samplingRate, channels, pcmData.getSampleCount()));

        return () -> {
            Stream old = streams.put(id, new Stream(voice));
            if (old != null) {
                Stream stream = streams.get(id);
                stream.panpot = old.panpot;
                stream.pair = old.pair;
                stream.track = old.track;
                if (stream.track != null) {
                    stream.track.keyOff(0, true);
                }
            }
        };
    }

    /**
     * Starts a stream, see {@code vavi.sound.mobile.MobileExclusive#on}. On the audio thread.
     *
     * @param velocity 0 ~ 127
     * @param audioChannel 0 ~ 3, else none
     */
    void streamOn(int id, int velocity, int audioChannel) {
        Stream stream = streams.get(id);
        if (stream == null) {
logger.log(Level.DEBUG, "stream on for no stream: " + id);
            return;
        }
        start(id, stream, velocity, audioChannel);
        if (stream.pair >= 0) {
            Stream pair = streams.get(stream.pair);
            if (pair != null) {
                start(stream.pair, pair, velocity, audioChannel);
            }
        }
    }

    private void start(int id, Stream stream, int velocity, int audioChannel) {
        if (stream.track == null) {
            stream.track = driver.newUserControlableTrack(STREAM_TRACK_ID | id);
        }
        SiMMLTrack track = stream.track;
        track.keyOff(0, true); // the same stream again starts over
        stream.voice.updateTrackVoice(track);
        boolean hasChannel = audioChannel >= 0 && audioChannel < AUDIO_CHANNELS;
        track.setVelocity(velocity * 2);
        track.setExpression(hasChannel ? audioVolumes[audioChannel] : 127);
        int panpot = stream.panpot >= 0 ? stream.panpot : hasChannel ? audioPanpots[audioChannel] : 64;
        track.channel.setPan(panpot - 64);
        track.keyOn(STREAM_NOTE, 0, 0);
logger.log(Level.DEBUG, "stream on: No.%d, velocity: %d, audio channel: %d".formatted(id, velocity, audioChannel));
    }

    /** Stops a stream, and the one paired with it. On the audio thread. */
    void streamOff(int id) {
        Stream stream = streams.get(id);
        if (stream == null) {
            return;
        }
        if (stream.track != null) {
            stream.track.keyOff(0, false);
        }
        if (stream.pair >= 0 && streams.get(stream.pair) instanceof Stream pair && pair.track != null) {
            pair.track.keyOff(0, false);
        }
logger.log(Level.DEBUG, "stream off: No.%d".formatted(id));
    }

    /** whether there is a stream, on the audio thread */
    boolean hasStream(int id) {
        return streams.containsKey(id);
    }

    /**
     * The stream panpot, {@code 43 79 0x 7f 0b id pp dd}. On the audio thread.
     *
     * @param mode 0: dd, 1: clear, 2: off (center)
     */
    void setStreamPanpot(int id, int mode, int panpot) {
        Stream stream = streams.get(id);
        if (stream == null) {
logger.log(Level.DEBUG, "stream panpot for no stream: " + id);
            return;
        }
        stream.panpot = switch (mode) {
            case 0 -> panpot & 0x7f;
            case 2 -> 64;
            default -> -1;
        };
    }

    /**
     * The stream pair, {@code 43 79 0x 7f 08 cl id1 id2}: a start of either starts both.
     * On the audio thread.
     *
     * @param cancel false: pair, true: cancel
     */
    void setStreamPair(boolean cancel, int id1, int id2) {
        Stream stream1 = streams.get(id1);
        Stream stream2 = streams.get(id2);
        if (stream1 == null || stream2 == null || id1 == id2) {
logger.log(Level.DEBUG, "stream pair for no stream: " + id1 + ", " + id2);
            return;
        }
        stream1.pair = cancel ? -1 : id2;
        stream2.pair = cancel ? -1 : id1;
    }

    /** the volume of an MFi audio channel, 0 ~ 127, on the audio thread */
    void setAudioVolume(int channel, int volume) {
        if (channel >= 0 && channel < AUDIO_CHANNELS) {
            audioVolumes[channel] = volume;
        }
    }

    /** the panpot of an MFi audio channel, 0 ~ 127, on the audio thread */
    void setAudioPanpot(int channel, int panpot) {
        if (channel >= 0 && channel < AUDIO_CHANNELS) {
            audioPanpots[channel] = panpot;
        }
    }

    /** stops every stream, on the audio thread */
    void close() {
        for (Stream stream : streams.values()) {
            if (stream.track != null) {
                stream.track.keyOff(0, true);
            }
        }
    }

    // ---- wave table

    /**
     * Registers the wave a WT voice plays.
     *
     * @param waveId what the {@code RM, WaveID} byte of a voice refers to
     * @param adpcm 4 bit adpcm
     */
    synchronized void setWave(int waveId, byte[] adpcm) {
        waves.put(waveId, decodeAdpcm(adpcm, 0, adpcm.length));
logger.log(Level.DEBUG, "wave table wave: No." + waveId + ", " + adpcm.length + " bytes adpcm");
    }

    /** Forgets a wave, {@code 43 79 0x 7f 04}. */
    synchronized void removeWave(int waveId) {
        waves.remove(waveId);
    }

    /**
     * The voice a WT voice is, on the PCM channel of SiON.
     * <pre>
     *      | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
     *  + 0 |             Fs(H)             |  the rate the wave sounds key 60 at
     *  + 1 |             Fs(L)             |
     *  + 2 |      panpot       |   ?   |P E|
     *  + 3 |  lfo  |           ?           |
     *  + 4 |      S R      |xof|   |sus|   |
     *  + 5 |      R R      |      D R      |
     *  + 6 |      A R      |      S L      |
     *  + 7 |          T L          |   ?   |
     *  + 8 | ? |  dam  |eam| ? |  dvb  |evb|
     *  + 9 |       (wave address)          |
     *  +10 |                               |
     *  +11 |             LP(H)             |  loop point [sample]
     *  +12 |             LP(L)             |
     *  +13 |             EP(H)             |  end point [sample]
     *  +14 |             EP(L)             |
     *  +15 |R M|         ...WaveID         |  RM = 1: a preset (rom) wave
     * </pre>
     * <p>
     * The rates are the MA-3 FM ones, 4 bit, which SiON takes 6 bit the way {@code #MA@}
     * does. The panpot of a voice is not taken, the midi module pans every note by its
     * channel. A preset (rom) wave is not here.
     * </p>
     *
     * @param image the 16 byte VM35 PCM voice
     * @param drum a drum voice always sounds at key 60
     * @return null when the wave of it is not here
     */
    synchronized SiONVoice wtVoice(byte[] image, boolean drum) {
        if (image.length < 16) {
logger.log(Level.WARNING, "wave table voice is too short: " + image.length);
            return null;
        }
        int samplingRate = ((image[0] & 0xff) << 8) | (image[1] & 0xff);
        int sr = (image[4] >> 4) & 0x0f;
        int rr = (image[5] >> 4) & 0x0f;
        int dr = image[5] & 0x0f;
        int ar = (image[6] >> 4) & 0x0f;
        int sl = image[6] & 0x0f;
        int tl = (image[7] >> 2) & 0x3f;
        int loopPoint = ((image[11] & 0xff) << 8) | (image[12] & 0xff);
        int endPoint = ((image[13] & 0xff) << 8) | (image[14] & 0xff);
        boolean romWave = (image[15] & 0x80) != 0;
        int waveId = image[15] & 0x7f;

        short[] pcm = romWave ? null : waves.get(waveId);
        if (pcm == null || samplingRate == 0) {
logger.log(Level.DEBUG, "wave table voice without its wave: %s%d".formatted(romWave ? "rom " : "", waveId));
            return null;
        }
        double[] wave = new double[pcm.length];
        for (int i = 0; i < pcm.length; i++) {
            wave[i] = pcm[i] / 32768.0;
        }
        SiONVoice voice = new SiONVoice();
        double samplingNote = WT_BASE_KEY + 12 * Math.log(driver.getSampleRate() / (double) samplingRate) / Math.log(2);
        SiOPMWavePCMData pcmData = voice.setPCMWave(0, wave, samplingNote, 0, 127, 1, 1);
        int end = Math.min(endPoint, pcm.length - 1);
        pcmData.slice(0, end, loopPoint < end ? loopPoint : -1);
        voice.setEnvelop(ar << 2, dr << 2, sr << 2, rr << 2, sl, Math.min(tl + WT_HEADROOM, 127));
        if (drum) {
            voice.preferableNote = WT_BASE_KEY;
        }
        return voice;
    }

    // ----

    /** 4 bit yamaha adpcm to 16 bit pcm */
    static short[] decodeAdpcm(byte[] adpcm, int offset, int length) {
        try (MaInputStream in = new MaInputStream(new ByteArrayInputStream(adpcm, offset, length), ByteOrder.LITTLE_ENDIAN)) {
            byte[] bytes = in.readAllBytes();
            short[] pcm = new short[bytes.length / 2];
            for (int i = 0; i < pcm.length; i++) {
                pcm[i] = (short) ((bytes[i * 2] & 0xff) | (bytes[i * 2 + 1] << 8));
            }
            return pcm;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Linear interpolation to another rate.
     *
     * @return [-1, 1), interleaved when stereo
     */
    static double[] resample(short[][] pcm, int from, double to) {
        int channels = pcm.length;
        double step = from / to;
        int frames = (int) ((pcm[0].length - 1) / step) + 1;
        double[] wave = new double[frames * channels];
        for (int i = 0; i < frames; i++) {
            double position = i * step;
            int index = (int) position;
            double fraction = position - index;
            for (int c = 0; c < channels; c++) {
                short[] p = pcm[c];
                int next = Math.min(index + 1, p.length - 1);
                wave[i * channels + c] = (p[index] + (p[next] - p[index]) * fraction) / 32768.0;
            }
        }
        return wave;
    }
}
