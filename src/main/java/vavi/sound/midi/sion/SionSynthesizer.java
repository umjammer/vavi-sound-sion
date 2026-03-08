/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sion;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import vavi.sound.midi.sion.SionSoundbank.SionInstrument;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * SionSynthesizer.
 * <p>
 * Uses SiON's MIDIModule for MIDI event handling and directly drives the
 * SiOPM processing pipeline for audio generation. The audio thread uses
 * SourceDataLine's blocking write for natural backpressure — no manual
 * sample-counting or busy-wait timing is needed.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/03/06 umjammer initial version <br>
 */
public class SionSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(SionSynthesizer.class.getName());

    static {
        try {
            try (InputStream is = SionSynthesizer.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound-sion/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String version;

    /** the device information */
    protected static final Info info =
        new Info("SiON MA3 MIDI Synthesizer",
                            "vavi",
                            "SiON Software synthesizer for MA3",
                            "Version " + version) {};

    private long timestamp;

    private volatile boolean isOpen;

    private final AudioFormat audioFormat = new AudioFormat(44100, 16, 2, true, false);

    private SourceDataLine line;

    private SiONDriver driver;

    private MIDIModule midiModule;

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

        driver = new SiONDriver(2048, audioFormat.getChannels(), (int) audioFormat.getSampleRate(), 0);

        // initialize the processing pipeline (module, sequencer, effector)
        driver.play(null, true);

        // initialize MIDI module — allocates polyphony operator pool with tracks
        driver.initializeMidiModule(true);
        midiModule = driver.getMidiModule();

        isOpen = true;

        initAudioLine();
        executor.submit(this::audioLoop);
    }

    /** audio thread */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    /** open and start the SourceDataLine */
    private void initAudioLine() throws MidiUnavailableException {
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
logger.log(Level.DEBUG, line.getClass().getName());
            line.addLineListener(event -> logger.log(Level.DEBUG, "Line: " + event.getType()));

            // use driver's buffer length * 4 bytes per frame (16-bit stereo) as line buffer
            line.open(audioFormat, driver.getBufferLength() * 4);
            line.start();
        } catch (LineUnavailableException e) {
            throw (MidiUnavailableException) new MidiUnavailableException().initCause(e);
        }
    }

    private long start;

    /**
     * Audio generation loop.
     * <p>
     * Processes exactly one bufferLength chunk per iteration through the SiOPM
     * pipeline, then writes the result to the SourceDataLine. The line.write()
     * call blocks when the line buffer is full, providing natural backpressure
     * and eliminating the need for manual timing.
     */
    private void audioLoop() {
        start = System.currentTimeMillis();
        timestamp = start;

        int bufferLength = driver.getBufferLength();
        // output is interleaved L/R doubles, length = bufferLength * 2
        byte[] out = new byte[bufferLength * 4]; // 16-bit stereo = 4 bytes per sample frame
        int loopCount = 0;
logger.log(Level.INFO, "audioLoop STARTED, bufferLength=" + bufferLength + ", out.length=" + out.length);

        while (isOpen) {
            loopCount++;
            try {
                // run the SiOPM processing pipeline for one buffer chunk
                driver.module._beginProcess();
                driver.effector._beginProcess();
                driver.sequencer._process();
                driver.effector._endProcess();
                driver.module._endProcess();

                // convert double output [-1..1] to 16-bit little-endian PCM
                double[] output = driver.module.getOutput();
                double maxAbs = 0;
                for (int i = 0; i < output.length; i++) {
                    double v = output[i];
                    if (Math.abs(v) > maxAbs) maxAbs = Math.abs(v);
                    short s = (short) (v * 32767);
                    out[i * 2] = (byte) (s & 0xff);
                    out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                }
                if (maxAbs > 0.001) {
                    logger.log(Level.INFO, "audio output: maxAbs=%.6f, tracks=%d".formatted(maxAbs, driver.sequencer.tracks.size()));
                }
                if (loopCount % 50 == 1) { // periodic debug
                    for (int t = 0; t < driver.sequencer.tracks.size(); t++) {
                        var trk = driver.sequencer.tracks.get(t);
                        logger.log(Level.INFO, "loop[%d] track[%d]: active=%b, ptr=%s, ch_noteOn=%b, ch_idling=%b".formatted(
                            loopCount, t, trk.isActive(), trk.executor.pointer,
                            trk.channel.isNoteOn(), trk.channel.isIdling()));
                    }
                }

                // blocking write — natural backpressure from the audio device
                line.write(out, 0, out.length);

                timestamp = System.currentTimeMillis();

            } catch (Exception e) {
                if (!isOpen) break; // normal shutdown
                logger.log(Level.INFO, "Audio processing error: " + e.getMessage(), e);
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
        return (timestamp - start) * 1000;
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
                                midiModule.noteOff(channel, data1, 0);
                            } else {
logger.log(Level.INFO, "[%d] NOTE_ON ch: %d, note: %d, vel: %d".formatted(timeStamp, channel, data1, data2));
                                midiModule.noteOn(channel, data1, data2);
                                // check all tracks for noteOn state
                                for (int t = 0; t < driver.sequencer.tracks.size(); t++) {
                                    var trk = driver.sequencer.tracks.get(t);
                                    if (trk.channel.isNoteOn() || trk.executor.pointer != null) {
                                        logger.log(Level.INFO, "  -> track[%d]: ptr=%s, noteOn=%b, idling=%b, note=%d".formatted(
                                            t, trk.executor.pointer, trk.channel.isNoteOn(), trk.channel.isIdling(), trk.getNote()));
                                    }
                                }
                            }
                        }
                        case ShortMessage.NOTE_OFF ->
                            midiModule.noteOff(channel, data1, data2);
                        case ShortMessage.PROGRAM_CHANGE ->
                            midiModule.programChange(channel, data1);
                        case ShortMessage.CONTROL_CHANGE ->
                            midiModule.controlChange(channel, data1, data2);
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
                case SysexMessage sysexMessage -> {
                    byte[] data = sysexMessage.getData();
logger.log(Level.DEBUG, "sysex: %02X\n%s".formatted(sysexMessage.getStatus(), StringUtil.getDump(data, 32)));
                    switch (data[0]) {
                        case 0x7f -> { // Universal Realtime
                            int c = data[1]; // 0x7f: Disregards channel
                            // Sub-ID, Sub-ID2
                            if (data[2] == 0x04 && data[3] == 0x01) { // Device Control / Master Volume
                                float gain = ((data[4] & 0x7f) | ((data[5] & 0x7f) << 7)) / 16383f;
logger.log(Level.DEBUG, "sysex volume: gain: %3.0f".formatted(gain * 127));
                                volume(line, gain);
                            }
                        }
                    }
                }
                default -> {}
            }
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
