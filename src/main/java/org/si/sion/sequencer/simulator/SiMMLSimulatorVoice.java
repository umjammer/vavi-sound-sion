//
// class for SiMML sequencer setting
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.simulator;

import org.si.sion.sequencer.SiMMLChannelSetting;


/** @private vioce setting for SiMMLSimulators */
public class SiMMLSimulatorVoice {

    // variables
    //
    public int pgType;
    public int ptType;
    public int channelType;

    // constructor
    //
    public SiMMLSimulatorVoice(int pgType, int ptType) {
        this(pgType, ptType, -1);
    }

    public SiMMLSimulatorVoice(int pgType, int ptType, int channelType) {
        this.pgType = pgType;
        this.ptType = ptType;
        this.channelType = (channelType == -1) ? SiMMLChannelSetting.SELECT_TONE_FM : channelType;
    }
}
