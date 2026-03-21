/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import org.si.sion.SiONDriver;
import org.si.sion.midi.SMFData;
import org.si.utils.ByteArray;
import vavi.sound.midi.sion.SionSynthesizer;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;
import static vavix.util.DelayedWorker.later;


/**
 * WaveOutTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-19 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
@EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
class WaveOutTest {

    static {
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
        System.setProperty("javax.sound.midi.Synthesizer", "#SiON MA3 MIDI Synthesizer");
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static long time = 120 * 1000;

    @Property
    String midi = "src/test/resources/test.mid";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("org.si.sion.allowPluralDrivers", "true");
    }

    @BeforeAll
    static void setupAll() {
        System.setProperty("javax.sound.sampled.SourceDataLine", "#WaveOut Mixer");
    }

    @Test
    @DisplayName("via spi")
    void test() throws Exception {
Debug.println(midi);

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        assertEquals(SionSynthesizer.class, synthesizer.getClass());
        synthesizer.open();
Debug.println("synthesizer: " + synthesizer);

        Sequencer sequencer = MidiSystem.getSequencer(false);
        Receiver receiver = synthesizer.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        sequencer.open();
Debug.println("sequencer: " + sequencer + ", " + sequencer.getClass().getName());

        Path file = Paths.get(midi);

        Sequence seq = MidiSystem.getSequence(new BufferedInputStream(Files.newInputStream(file)));

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        sequencer.setSequence(seq);
        sequencer.addMetaEventListener(mel);
Debug.println("START");
        sequencer.start();

        Thread.sleep(time);
        sequencer.stop();
Debug.println("STOP");
        sequencer.removeMetaEventListener(mel);
        sequencer.close();

        synthesizer.close();

        Files.move(Path.of(System.getProperty("vavi.sound.sampled.misc.waveout")), Path.of("tmp", "sion_spi.wav"), StandardCopyOption.REPLACE_EXISTING);
Debug.println("END");
    }

    @Test
    @DisplayName("via api")
    void test1() throws Exception {
Debug.println(midi);
        SiONDriver driver = new SiONDriver(2048, 2, 44100, 0);
        AtomicBoolean smfFinished = new AtomicBoolean(false);

        byte[] bytes = Files.readAllBytes(Paths.get(midi));
        ByteArray byteArray = new ByteArray();
        byteArray.writeBytes(bytes);
        SMFData smfData = new SMFData();
        smfData.loadBytes(byteArray);
        driver.getMidiModule().onFinishSequence = () -> smfFinished.set(true);
        driver.play(smfData, true);

        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, driver.getBufferLength() * 4);
        line.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> playback = executor.submit(() -> {
            int bufferSize = driver.getBufferLength();
            byte[] out = new byte[bufferSize * 4];
            try {
                do {
                    driver.module._beginProcess();
                    driver.effector._beginProcess();
                    driver.sequencer._process();
                    driver.effector._endProcess();
                    driver.module._endProcess();

                    double[] output = driver.module.getOutput();
                    for (int i = 0; i < output.length; i++) {
                        short s = (short) (output[i] * 32767);
                        // Little endian
                        out[i * 2] = (byte) (s & 0xff);
                        out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                    }
                    line.write(out, 0, out.length);
                } while (!later(time).come() && !smfFinished.get());
            } catch (Exception e) {
Debug.printStackTrace(e);
            } finally {
                line.drain();
                line.close();
            }
        });

        playback.get();

        Thread.sleep(500);
        executor.shutdown();
Debug.println("Finished.");

        Files.move(Path.of(System.getProperty("vavi.sound.sampled.misc.waveout")), Path.of("tmp", "sion_api.wav"), StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterAll
    static void tearDownAll() {
        System.setProperty("javax.sound.sampled.SourceDataLine", "");
    }
}
