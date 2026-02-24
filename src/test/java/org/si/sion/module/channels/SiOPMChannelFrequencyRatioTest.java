package org.si.sion.module.channels;

import org.junit.jupiter.api.Test;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SiOPMChannelFrequencyRatioTest {

    @Test
    void fmFrequencyRatioUsesFloatingPointDivision() {
        SiOPMModule module = new SiOPMModule();
        module.initialize(2, 0, 2048);
        SiOPMChannelFM channel = new SiOPMChannelFM(module);
        channel.setFrequencyRatio(133);

        int expectedEg = (int) (SiOPMTable.ENV_TIMER_INITIAL * (100.0 / 133.0));
        int expectedLfo = (int) (SiOPMTable.LFO_TIMER_INITIAL * (100.0 / 133.0));

        assertEquals(expectedEg, channel._eg_timer_initial);
        assertEquals(expectedLfo, channel._lfo_timer_initial);
        assertTrue(channel._eg_timer_initial > 0);
        assertTrue(channel._lfo_timer_initial > 0);
    }

    @Test
    void pcmFrequencyRatioUsesFloatingPointDivision() {
        SiOPMModule module = new SiOPMModule();
        module.initialize(2, 0, 2048);
        SiOPMChannelPCM channel = new SiOPMChannelPCM(module);
        channel.setFrequencyRatio(133);

        int expectedEg = (int) (SiOPMTable.ENV_TIMER_INITIAL * (100.0 / 133.0));
        int expectedLfo = (int) (SiOPMTable.LFO_TIMER_INITIAL * (100.0 / 133.0));

        assertEquals(expectedEg, channel._eg_timer_initial);
        assertEquals(expectedLfo, channel._lfo_timer_initial);
        assertTrue(channel._eg_timer_initial > 0);
        assertTrue(channel._lfo_timer_initial > 0);
    }
}
