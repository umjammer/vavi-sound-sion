/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sion;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.si.sion.SiONDriver;
import org.si.sion.midi.MIDIModule;
import org.si.sion.sequencer.base.MMLEvent;
import org.si.sion.sequencer.base.MMLSequence;
import org.si.utils.ByteArray;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;
import vavi.sound.midi.sion.SionSoundbank.SionInstrument;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.midi.sion.SionMidiDeviceProvider.version;


/**
 * SionSynthesizer.
 * <p>
 * Uses SiON's MIDIModule for MIDI event handling and directly drives the
 * SiOPM processing pipeline for audio generation. The audio thread uses
 * SourceDataLine's blocking write for natural backpressure — no manual
 * sample-counting or busy-wait timing is needed.
 * </p><p>
 * system property
 *  <li>{@code org.si.sion.bufferSize} ... midi buffer size, default 256</li>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/03/06 umjammer initial version <br>
 */
public class SionSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(SionSynthesizer.class.getName());

    /** the device information */
    protected static final Info info =
        new Info("SiON MA3 MIDI Synthesizer",
                            "vavi",
                            "SiON Software synthesizer for MA3",
                            "Version " + version) {};

    private long frames;

    private volatile boolean isOpen;

    private final AudioFormat audioFormat = new AudioFormat(44100, 16, 2, true, false);

    private SourceDataLine line;

    private SiONDriver driver;

    private MIDIModule midiModule;

    /** the stream pcm and wave table waves of an MFi or SMAF file */
    private SionWaves waves;

    /** the voices an MFi or SMAF file sends */
    private SionYamahaVoices yamahaVoices;

    /** bank select MSB of a channel, touched by the audio thread only */
    private final int[] bankMSBs = new int[16];

    /** bank select LSB of a channel, touched by the audio thread only */
    private final int[] bankLSBs = new int[16];

    /** the notes of a channel which started a stream, touched by the audio thread only */
    private final int[][] streamNotes = new int[16][128];

    /** the listener's volume, universal master volume, 0 ~ 1. touched by the audio thread only */
    private double hostGain = 1;

    /** the song's volume, mfi master volume, 0 ~ 1. touched by the audio thread only */
    private double songGain = 1;

    /**
     * the mfi master volume the universal master volume following is the song's one of,
     * -1: none. touched by the audio thread only
     */
    private int songVolume = -1;

    /** the listener's volume scaled by the song's, see {@code vavi.sound.midi.faith.FaithSynthesizer} */
    private void applyGain() {
        driver.setVolume(hostGain * songGain);
    }

    /**
     * Set the BPM for the internal SiON sequencer.
     * <p>
     * Not needed for this synthesizer to keep time: the Java Sequencer handles tempo, and
     * {@link #onSpiCallback} turns the time between events into MML ticks with the BPM of
     * the driver, which then turns them back into samples with that same BPM, so it cancels
     * out. Changing it only changes the tick the waits are rounded to (1920 a whole note).
     * </p>
     *
     * @param bpm beats per minute
     */
    public void setBpm(double bpm) {
        if (driver != null) {
            driver.setBpm(bpm);
logger.log(Level.DEBUG, "BPM set to " + bpm);
        }
    }

    /** Minimum note duration in microseconds to prevent zero-length notes */
    private static final long MIN_NOTE_DURATION_MICROS = 5000; // 5ms

    /** Timestamped MIDI event for sub-buffer positioning */
    private record TimestampedEvent(long microseconds, long sequence, Runnable action)
            implements Comparable<TimestampedEvent> {
        @Override
        public int compareTo(TimestampedEvent other) {
            int cmp = Long.compare(microseconds, other.microseconds);
            return cmp != 0 ? cmp : Long.compare(sequence, other.sequence);
        }
    }

    /** Monotonic sequence counter for preserving event order within same timestamp */
    private final AtomicLong eventSequence = new AtomicLong();

    /** Queued MIDI events ordered by timestamp for sub-buffer positioning. */
    private final PriorityBlockingQueue<TimestampedEvent> midiEvents = new PriorityBlockingQueue<>();

    /** Tracks noteOn timestamps per channel/note for minimum duration enforcement */
    private final long[][] noteOnTimestamps = new long[16][128];

    /** GLOBAL_WAIT event whose length is set by the callback */
    private MMLEvent spiWaitEvent;

    /** Fractional MML tick accumulator for sub-buffer timing rounding */
    private double spiMmlTickError = 0;

    // ----

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpen()) {
logger.log(Level.WARNING, "already open: " + hashCode());
            return;
        }

        int bufSize = Integer.getInteger("org.si.sion.bufferSize", 256);
        driver = new SiONDriver(bufSize, audioFormat.getChannels(), (int) audioFormat.getSampleRate(), 0);

        // initialize the processing pipeline (module, sequencer, effector)
        driver.play(null, true);

        // initialize MIDI module — allocates polyphony operator pool with tracks
        driver.initializeMidiModule(true);
        midiModule = driver.getMidiModule();

        waves = new SionWaves(driver);
        yamahaVoices = new SionYamahaVoices(midiModule, waves);

        // install global sequence — events must fire inside _process() because
        // keyOn() sets executor.pointer which is consumed by processMMLExecutor()
        setupGlobalSequence();

        isOpen = true;

        initAudioLine();
        executor.submit(this::audioLoop);
    }

    /** audio thread */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.MAX_PRIORITY);
        return thread;
    });

    /** open and start the SourceDataLine */
    private void initAudioLine() throws MidiUnavailableException {
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
logger.log(Level.TRACE, line.getClass().getName());
            line.addLineListener(event -> logger.log(Level.DEBUG, "Line: " + event.getType()));

            // use a large enough line buffer to prevent underruns (e.g. 8192 frames)
            line.open(audioFormat, 8192 * 4);
            line.start();
        } catch (LineUnavailableException e) {
            throw (MidiUnavailableException) new MidiUnavailableException().initCause(e);
        }
    }

    /**
     * Set up a global sequence so that MIDI events fire inside _process().
     * This is required because keyOn() sets executor.pointer to a DRIVER_NOTE
     * event that must be consumed by processMMLExecutor() within the same
     * _process() call.
     */
    private void setupGlobalSequence() {
        MMLSequence seq = new MMLSequence(false);
        seq.initialize();
        seq.appendNewEvent(MMLEvent.REPEAT_ALL, 0, 0);
        seq.appendNewCallback(this::onSpiCallback, 0);
        spiWaitEvent = seq.appendNewEvent(MMLEvent.GLOBAL_WAIT, 0, 0);
        driver.sequencer.setGlobalSequence(seq);
    }

    /**
     * Global sequence callback: processes MIDI events with sub-buffer timing.
     * <p>
     * Events are timestamped (from the Java Sequencer's dispatch time) and
     * spaced within the buffer using GLOBAL_WAIT. This gives short notes
     * actual audio duration instead of collapsing to zero when note-on and
     * note-off fall in the same buffer.
     */
    private MMLEvent onSpiCallback(Object data) {
        TimestampedEvent evt = midiEvents.peek();
        if (evt == null) {
            // no events — keep polling with minimal wait
            spiWaitEvent.length = 1;
            return null;
        }

        // process all events at the same timestamp
        long currentTimestamp = evt.microseconds;
        while (evt != null && evt.microseconds <= currentTimestamp) {
            midiEvents.poll();
            evt.action.run();
            evt = midiEvents.peek();
        }

        // calculate wait to next event using timestamp delta
        if (evt != null) {
            long deltaMicros = evt.microseconds - currentTimestamp;
            // convert microseconds to MML ticks:
            // SiON resolution = 1920 ticks/whole-note, 240 = 60s * 4 beats/whole-note
            double mmlTicks = deltaMicros * 1920.0 * driver.getBpm() / 240_000_000.0 + spiMmlTickError;
            int intTicks = (int) mmlTicks;
            if (intTicks < 1 && deltaMicros > 0) intTicks = 1;
            spiWaitEvent.length = Math.max(1, intTicks);
            spiMmlTickError = mmlTicks - spiWaitEvent.length;
        } else {
            // no more events — minimal wait, keep polling
            spiWaitEvent.length = 1;
            spiMmlTickError = 0;
        }

        return null;
    }

    /**
     * Audio generation loop.
     * <p>
     * Each iteration processes one bufferLength chunk (256 samples ≈ 5.8 ms).
     * MIDI events fire inside _process() via the global sequence callback.
     */
    private void audioLoop() {
        frames = 0;

        int bufferLength = driver.getBufferLength();
        // output is interleaved L/R doubles, length = bufferLength * 2
        byte[] out = new byte[bufferLength * 4]; // 16-bit stereo = 4 bytes per sample frame
        int loopCount = 0;
logger.log(Level.TRACE, "audioLoop STARTED, bufferLength=" + bufferLength + ", out.length=" + out.length);

        while (isOpen) {
            loopCount++;
            try {
                // reset buffer index so that channel.getBufferIndex() returns 0
                for (int t = 0; t < driver.sequencer.tracks.size(); t++) {
                    var trk = driver.sequencer.tracks.get(t);
                    if (trk.channel != null) {
                        trk.channel.resetChannelBufferStatus();
                    }
                }

                // run the SiOPM processing pipeline
                driver.module._beginProcess();
                driver.effector._beginProcess();
                driver.sequencer._process();
                driver.effector._endProcess();
                driver.module._endProcess();

                // convert double output [-1..1] to 16-bit little-endian PCM
                double[] output = driver.module.getOutput();
                double volume = driver.getVolume();
                double maxAbs = 0;
                for (int i = 0; i < output.length; i++) {
                    double v = output[i] * volume;
                    if (Math.abs(v) > maxAbs) maxAbs = Math.abs(v);
                    short s = (short) (Math.clamp(v, -1, 1) * 32767);
                    out[i * 2] = (byte) (s & 0xff);
                    out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                }
                if (maxAbs > 0.001) {
                    logger.log(Level.TRACE, "audio output: maxAbs=%.6f, tracks=%d".formatted(maxAbs, driver.sequencer.tracks.size()));
                }
                if (loopCount % 350 == 1) { // periodic debug (adjusted for smaller buffer)
                    int noteOnCount = 0;
                    for (int t = 0; t < driver.sequencer.tracks.size(); t++) {
                        var trk = driver.sequencer.tracks.get(t);
                        if (trk.channel.isNoteOn()) noteOnCount++;
                    }
                    // per-channel volume and activity snapshot with effect send levels
                    StringBuilder chInfo = new StringBuilder();
                    for (int ch = 0; ch < 16; ch++) {
                        var mc = midiModule.midiChannels[ch];
                        if (mc.activeOperatorCount > 0 || mc.getMasterVolume() != 100) {
                            chInfo.append("ch%d[v=%d,e=%d,prg=%d,act=%d,drm=%d,fx=%d/%d/%d] ".formatted(
                                ch, mc.getMasterVolume(), mc.getExpression(),
                                mc.programNumber, mc.activeOperatorCount, mc.drumMode,
                                mc.getEffectSendLevel(1), mc.getEffectSendLevel(2), mc.getEffectSendLevel(3)));
                        }
                    }
                    double sec = (double) frames / audioFormat.getSampleRate();
                    logger.log(Level.TRACE, "loop[%d] t=%.1fs free=%d, active=%d, noteOn=%d, maxAbs=%.3f\n  %s".formatted(
                        loopCount, sec, midiModule.getFreeOperatorCount(), midiModule.getActiveOperatorCount(),
                        noteOnCount, maxAbs, chInfo));
                }

                // blocking write — natural backpressure from the audio device
                line.write(out, 0, out.length);

                frames += bufferLength;

            } catch (Exception e) {
                if (!isOpen) break; // normal shutdown
                logger.log(Level.TRACE, "Audio processing error: " + e.getMessage(), e);
            }
        }
    }

    @Override
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void close() {
        isOpen = false;
        for (int i = 0; i < receivers.size(); i++) receivers.get(i).close();
        line.drain();
        line.close();
        executor.shutdown();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public long getMicrosecondPosition() {
        return (long) ((frames / (double) audioFormat.getSampleRate()) * 1_000_000);
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new SionReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return receivers;
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new MidiUnavailableException("No transmitter available");
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.emptyList();
    }

    @Override
    public int getMaxPolyphony() {
        return midiModule != null ? midiModule.getPolyphony() : 16;
    }

    @Override
    public long getLatency() {
        return 33;
    }

    @Override
    public MidiChannel[] getChannels() {
        return null;
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        return null;
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        return soundbank instanceof SionInstrument;
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** remembers the bank of a channel, a SMAF drum bank makes it a drum channel. on the audio thread */
    private void bankSelect(int channel, int control, int value) {
        if (control == 0) {
            bankMSBs[channel] = value;
            if (value == SionYamahaVoices.DRUM_BANK) {
                midiModule.midiChannels[channel].drumMode = 1;
            } else if (value == SionYamahaVoices.MELODY_BANK) {
                midiModule.midiChannels[channel].drumMode = 0;
            }
        } else if (control == 32) {
            bankLSBs[channel] = value;
        }
    }

    /**
     * A note of key 0 ~ 12 or 92 ~ 110 on a SMAF drum channel starts stream {@code key + 1}
     * or {@code key - 78}, see {@code Note_ON3} of the MA-3 driver (mammfcnv.c). On the audio thread.
     *
     * @return false when the note is not a stream one
     */
    private boolean streamNoteOn(int channel, int note, int velocity) {
        if (bankMSBs[channel] != SionYamahaVoices.DRUM_BANK) {
            return false;
        }
        int id = note <= 12 ? note + 1 : note >= 92 && note <= 110 ? note - 78 : 0;
        if (id == 0 || !waves.hasStream(id)) {
            return false;
        }
        waves.streamOn(id, velocity, -1);
        streamNotes[channel][note] = id;
        return true;
    }

    /** @return false when the note did not start a stream */
    private boolean streamNoteOff(int channel, int note) {
        int id = streamNotes[channel][note];
        if (id == 0) {
            return false;
        }
        streamNotes[channel][note] = 0;
        waves.streamOff(id);
        return true;
    }

    private final List<Receiver> receivers = new ArrayList<>();

    /** MIDI receiver that dispatches to MIDIModule */
    private class SionReceiver implements MidiDeviceReceiver {

        private boolean isOpen;

        public SionReceiver() {
            receivers.add(this);
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isOpen) throw new IllegalStateException("Receiver is not open");

            // use Sequencer's timestamp if available, otherwise capture wall clock
            long eventMicros = timeStamp >= 0 ? timeStamp : System.nanoTime() / 1000;

            // enforce minimum note duration: push noteOff forward if too close to noteOn
            if (message instanceof ShortMessage sm) {
                int cmd = sm.getCommand();
                int ch = sm.getChannel();
                int note = sm.getData1();
                int vel = sm.getData2();
                if (cmd == ShortMessage.NOTE_ON && vel > 0) {
                    noteOnTimestamps[ch][note] = eventMicros;
                } else if (cmd == ShortMessage.NOTE_OFF || (cmd == ShortMessage.NOTE_ON && vel == 0)) {
                    long onTime = noteOnTimestamps[ch][note];
                    if (onTime > 0 && eventMicros - onTime < MIN_NOTE_DURATION_MICROS) {
                        eventMicros = onTime + MIN_NOTE_DURATION_MICROS;
                    }
                }
            }

            // the voices and waves of an MFi or SMAF file are decoded here, not in the audio thread
            Runnable yamaha = message instanceof SysexMessage sysex ? yamahaVoices.process(sysex.getData()) : null;

            midiEvents.add(new TimestampedEvent(eventMicros, eventSequence.getAndIncrement(), () -> {
                switch (message) {
                    case ShortMessage shortMessage -> {
                        int channel = shortMessage.getChannel();
                        int command = shortMessage.getCommand();
                        int data1 = shortMessage.getData1();
                        int data2 = shortMessage.getData2();

                        switch (command) {
                            case ShortMessage.NOTE_ON -> {
                                if (data2 == 0) {
                                    // velocity 0 = note off per MIDI spec
                                    if (!streamNoteOff(channel, data1)) {
                                        midiModule.noteOff(channel, data1, 0);
                                    }
                                } else if (streamNoteOn(channel, data1, data2)) {
logger.log(Level.TRACE, "[%d] NOTE_ON ch:%d note:%d vel:%d (stream)".formatted(timeStamp, channel, data1, data2));
                                } else {
                                    {
                                    var mc = midiModule.midiChannels[channel];
                                    logger.log(Level.TRACE, "[%d] NOTE_ON ch:%d note:%d vel:%d vol:%d exp:%d prg:%d drm:%d".formatted(
                                        timeStamp, channel, data1, data2, mc.getMasterVolume(), mc.getExpression(), mc.programNumber, mc.drumMode));
                                    }
                                    midiModule.noteOn(channel, data1, data2);
                                }
                            }
                            case ShortMessage.NOTE_OFF -> {
logger.log(Level.TRACE, "[%d] NOTE_OFF ch: %d, note: %d, vel: %d".formatted(timeStamp, channel, data1, data2));
                                if (!streamNoteOff(channel, data1)) {
                                    midiModule.noteOff(channel, data1, data2);
                                }
                            }
                            case ShortMessage.PROGRAM_CHANGE -> {
                                logger.log(Level.TRACE, "[%d] PROG_CHG ch:%d prg:%d".formatted(timeStamp, channel, data1));
                                yamahaVoices.programChange(bankMSBs[channel], bankLSBs[channel], data1);
                                midiModule.programChange(channel, data1);
                            }
                            case ShortMessage.CONTROL_CHANGE -> {
                                logger.log(Level.TRACE, "[%d] CC ch:%d cc#%d val:%d".formatted(timeStamp, channel, data1, data2));
                                midiModule.controlChange(channel, data1, data2);
                                bankSelect(channel, data1, data2);
                            }
                            case ShortMessage.PITCH_BEND -> {
                                // combine data1 (LSB) and data2 (MSB) into 14-bit value, centered at 8192
                                int bend = (data2 << 7) | data1;
                                midiModule.pitchBend(channel, bend - 8192);
                            }
                            case ShortMessage.CHANNEL_PRESSURE ->
                                midiModule.channelAfterTouch(channel, data1);
                            default ->
logger.log(Level.DEBUG, "unhandled command: %02X ch: %d, d1: %d, d2: %d".formatted(command, channel, data1, data2));
                        }
                    }
                    case SysexMessage sysex -> {
                        byte[] data = sysex.getData();
logger.log(Level.TRACE, "sysex: %02X\n%s".formatted(sysex.getStatus(), StringUtil.getDump(data, 32)));
                        int sub = MfiValueExclusive.sub(sysex.getMessage());
                        if (sub == MfiValueExclusive.MASTER_VOLUME && data.length >= 4) {
                            // vavi's mark: the universal master volume following is the song's (mfi 0xb0)
                            songVolume = data[3] & 0x7f;
                            songGain = songVolume / 127d;
logger.log(Level.DEBUG, "song volume: %d".formatted(songVolume));
                            applyGain();
                        } else if ((data[0] & 0xff) == 0x7f && data.length >= 6 && data[2] == 0x04 && data[3] == 0x01) {
                            // Universal Realtime, Device Control / Master Volume, the listener's
                            // unless it is the song's the mark above has taken already
                            if (songVolume >= 0 && data[4] == 0 && data[5] == songVolume) {
                                songVolume = -1;
                            } else {
                                hostGain = ((data[4] & 0x7f) | ((data[5] & 0x7f) << 7)) / 16383d;
logger.log(Level.DEBUG, "sysex volume: gain: %3.0f".formatted(hostGain * 127));
                                applyGain();
                            }
                        } else if (sub >= 0) {
                            // the other mfi values, nothing SiON takes
                        } else if (yamaha != null) {
                            yamaha.run();
                        } else {
                            // forward all SysEx to MIDIModule for GM/GS/XG Reset and drum mode handling
                            ByteArray bytes = new ByteArray();
                            bytes.writeByte(0xf0); // prepend status byte stripped by SysexMessage.getData()
                            for (byte b : data) bytes.writeByte(b & 0xff);
                            bytes.position = 0;
logger.log(Level.DEBUG, "forwarding sysex to MIDIModule (%d bytes)".formatted(bytes.length));
                            midiModule.systemExclusive(0, bytes);
                        }
                    }
                    default -> {}
                }
            }));
        }

        @Override
        public void close() {
            isOpen = false;
            receivers.remove(this);
        }

        @Override
        public MidiDevice getMidiDevice() {
            return SionSynthesizer.this;
        }
    }
}
