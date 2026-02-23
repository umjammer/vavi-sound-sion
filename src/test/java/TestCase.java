/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import org.si.sion.SiONDriver;
import org.si.sion.midi.SMFData;
import org.si.utils.ByteArray;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-02-23 nsano initial version <br>
 */
public class TestCase {

    public static void main(String[] args) throws Exception {
        SiONDriver driver = new SiONDriver(2048, 2, 44100, 0);

        if (args.length > 0) {
            byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
            ByteArray byteArray = new ByteArray();
            byteArray.writeBytes(bytes);
            SMFData smfData = new SMFData();
            smfData.loadBytes(byteArray);
            driver.play(smfData, true);
        } else {
            driver.play("t120 l8 o5 ccggaag4 ffeeddc4 [ggffeed4]2 ccggaag4 ffeeddc4", true);
        }

        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, driver.getBufferLength() * 4);
        line.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                int bufferSize = driver.getBufferLength();
                byte[] out = new byte[bufferSize * 4];
                while (!driver.sequencer.getIsSequenceFinished()) {
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
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                line.drain();
                line.close();
            }
        });

        System.out.println("Playing... Press Ctrl+C to stop.");
        while (!driver.sequencer.getIsSequenceFinished()) {
            Thread.sleep(100);
        }

        Thread.sleep(500);
        executor.shutdown();
        System.out.println("Finished.");
    }
}
