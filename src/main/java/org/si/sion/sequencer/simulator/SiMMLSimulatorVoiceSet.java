//
// class for SiMML sequencer setting
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.simulator;

import org.si.sion.module.SiOPMTable;
import org.si.sion.sequencer.SiMMLChannelSetting;


/** set of voice setting for SiMMLSimulators */
public class SiMMLSimulatorVoiceSet {

    // variables
    //

    public SiMMLSimulatorVoice[] voices;
    public int initVoiceIndex;

    // constructor
    //

    public SiMMLSimulatorVoiceSet(int length) {
        this(length, -1, -1);
    }

    /** offset > -1 sets all voice instances */
    public SiMMLSimulatorVoiceSet(int length, int offset /* = -1 */, int channelType /* = -1 */) {
        this.initVoiceIndex = 0;
        this.voices = new SiMMLSimulatorVoice[length];
        if (offset != -1) {
            int i, ptType;
            if (channelType == -1) channelType = SiMMLChannelSetting.SELECT_TONE_FM;
            for (i = 0; i < length; i++) {
                ptType = SiOPMTable.getInstance().getWaveTable(i + offset).defaultPTType;
                this.voices[i] = new SiMMLSimulatorVoice(i + offset, ptType, channelType);
            }
        }
    }
}
