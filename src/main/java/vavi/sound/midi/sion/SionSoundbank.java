/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sion;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;
import com.sun.media.sound.ModelPatch;
import com.sun.media.sound.SimpleInstrument;

import static java.lang.System.getLogger;


/**
 * SionSoundbank.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/03/08 umjammer initial version <br>
 */
public class SionSoundbank implements Soundbank {

    private static final Logger logger = getLogger(SionSoundbank.class.getName());

    /** */
    private final List<Instrument> instruments = new ArrayList<>();

    public SionSoundbank() {
    }

    @Override
    public String getName() {
        return "NukedSoundbank";
    }

    @Override
    public String getVersion() {
        return SionSynthesizer.info.getVersion();
    }

    @Override
    public String getVendor() {
        return SionSynthesizer.info.getVendor();
    }

    @Override
    public String getDescription() {
        return "Soundbank for NukedSynthesizer";
    }

    @Override
    public SoundbankResource[] getResources() {
        return getInstruments();
    }

    @Override
    public Instrument[] getInstruments() {
        return instruments.toArray(Instrument[]::new);
    }

    @Override
    public Instrument getInstrument(Patch patch) {
        for (Instrument instrument : instruments) {
            if (instrument.getPatch().getProgram() == patch.getProgram() &&
                    instrument.getPatch().getBank() == patch.getBank()) {
logger.log(Level.DEBUG, "request for: " + patch);
                return instrument;
            }
        }
logger.log(Level.DEBUG, "no instrument for: " + patch);
        return null;
    }

    /** */
    public static class SionInstrument extends SimpleInstrument {
        final Object data;
        protected SionInstrument(int bank, int program, boolean percussion, Object instrument) {
            setPatch(new ModelPatch(bank, program, percussion));
            this.name = (percussion ?  "p." : "") + bank + "." + program;
            this.data = instrument;
        }

        @Override
        public Class<Object> getDataClass() {
            return Object.class;
        }

        @Override
        public Object getData() {
            return data;
        }
    }
}
