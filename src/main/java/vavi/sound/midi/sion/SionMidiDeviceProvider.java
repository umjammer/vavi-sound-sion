/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sion;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * SionMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260308 nsano initial version <br>
 */
public class SionMidiDeviceProvider extends MidiDeviceProvider {

    private static final Logger logger = getLogger(SionMidiDeviceProvider.class.getName());

    /** YAMAHA */
    public final static int MANUFACTURER_ID = 0x43;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] {
            SionSynthesizer.info
    };

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /** */
    @Override
    public MidiDevice getDevice(MidiDevice.Info info)
        throws IllegalArgumentException {

        if (info == SionSynthesizer.info) {
logger.log(Level.DEBUG, "★1 info: " + info);
            SionSynthesizer synthesizer = new SionSynthesizer();
            return synthesizer;
        } else {
logger.log(Level.DEBUG, "★1 here: " + info);
            throw new IllegalArgumentException();
        }
    }
}
