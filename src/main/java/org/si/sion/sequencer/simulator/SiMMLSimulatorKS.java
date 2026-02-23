//
// class for physical modeling guitar simulator
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.simulator;

import org.si.sion.module.SiOPMTable;


/** Physical modeling guitar simulator */
public class SiMMLSimulatorKS extends SiMMLSimulatorBase {

    public SiMMLSimulatorKS() {
        super(MT_KS, 1, new SiMMLSimulatorVoiceSet(512, SiOPMTable.PG_SINE, -1), false);
    }
}
