/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import org.si.sion.SiONDriver;
import org.si.sion.midi.SMFData;
import org.si.utils.ByteArray;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static vavi.sound.SoundUtil.volume;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-02-23 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String midi;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        try {
            java.lang.reflect.Field f = org.si.sion.SiONDriver.class.getDeclaredField("_mutex");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }

Debug.print("volume: " + volume);
    }

    @Test
    void test1() throws Exception {
        SiONDriver driver = new SiONDriver(2048, 2, 44100, 0);
        AtomicBoolean smfFinished = new AtomicBoolean(false);
        boolean isSmf;

        if (midi != null) {
            byte[] bytes = Files.readAllBytes(Paths.get(midi));
            ByteArray byteArray = new ByteArray();
            byteArray.writeBytes(bytes);
            SMFData smfData = new SMFData();
            smfData.loadBytes(byteArray);
            driver.getMidiModule().onFinishSequence = () -> smfFinished.set(true);
            driver.play(smfData, true);
            isSmf = true;
        } else {
            driver.getMidiModule().onFinishSequence = null;
            driver.play("t120 l8 o5 ccggaag4 ffeeddc4 [ggffeed4]2 ccggaag4 ffeeddc4", true);
            isSmf = false;
        }

        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, driver.getBufferLength() * 4);
        volume(line, volume);
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
                } while (isSmf ? !smfFinished.get() : !driver.sequencer.getIsSequenceFinished());
            } catch (Exception e) {
                Debug.printStackTrace(e);
            } finally {
                line.drain();
                line.close();
            }
        });

        Debug.println("Playing... Press Ctrl+C to stop.");
        playback.get();

        Thread.sleep(500);
        executor.shutdown();
        Debug.println("Finished.");
    }
}
