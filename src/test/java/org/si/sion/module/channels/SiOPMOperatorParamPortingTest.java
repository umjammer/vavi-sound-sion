package org.si.sion.module.channels;

import org.junit.jupiter.api.Test;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


class SiOPMOperatorParamPortingTest {

    @Test
    void setSiOPMOperatorParamUpdatesDerivedEnvelopeFields() {
        SiOPMModule module = new SiOPMModule();
        module.initialize(2, 0, 2048);
        SiOPMOperator operator = new SiOPMOperator(module);
        operator.initialize();

        SiOPMOperatorParam param = new SiOPMOperatorParam();
        param.sl = 12;
        param.tl = 8;
        operator.setSiOPMOperatorParam(param);

        int expectedSl12 = SiOPMTable.getInstance().eg_slTable[12];
        int totalLevelAtTl8 = operator._eg_total_level;
        assertEquals(expectedSl12, operator._eg_sustain_level);

        param.sl = 3;
        param.tl = 96;
        operator.setSiOPMOperatorParam(param);

        int expectedSl3 = SiOPMTable.getInstance().eg_slTable[3];
        assertEquals(expectedSl3, operator._eg_sustain_level);
        assertNotEquals(totalLevelAtTl8, operator._eg_total_level);
    }

    @Test
    void getSiOPMOperatorParamExportsPublicKsrAndAms() {
        SiOPMModule module = new SiOPMModule();
        module.initialize(2, 0, 2048);
        SiOPMOperator operator = new SiOPMOperator(module);
        operator.initialize();

        SiOPMOperatorParam in = new SiOPMOperatorParam();
        in.ksr = 3;
        in.ams = 2;
        operator.setSiOPMOperatorParam(in);

        SiOPMOperatorParam out = new SiOPMOperatorParam();
        operator.getSiOPMOperatorParam(out);

        assertEquals(3, out.ksr);
        assertEquals(2, out.ams);
    }

    @Test
    void channelSetSiOPMParametersAcceptsPublicKsrValue() {
        SiOPMModule module = new SiOPMModule();
        module.initialize(2, 0, 2048);

        SiOPMChannelFM fm = new SiOPMChannelFM(module);
        fm.setSiOPMParameters(63, 0, 0, 20, 10, 40, 3, 2, 4, 5, 0, 2, 0, 0);
        assertEquals(3, fm.activeOperator.getKs());

        SiOPMChannelPCM pcm = new SiOPMChannelPCM(module);
        pcm.setSiOPMParameters(63, 0, 0, 20, 10, 40, 3, 2, 4, 5, 0, 2, 0, 0);
        assertEquals(3, pcm.operator.getKs());
    }
}
