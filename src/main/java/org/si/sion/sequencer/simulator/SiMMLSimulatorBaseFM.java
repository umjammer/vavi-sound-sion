//
// Base class of all FM sound chip simulator
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.simulator;

import org.si.sion.module.SiOPMTable;
import org.si.sion.sequencer.SiMMLTable;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.sequencer.SiMMLVoice;
import org.si.sion.sequencer.base.MMLSequence;


/** Base class of all FM sound chip simulator */
public class SiMMLSimulatorBaseFM extends SiMMLSimulatorBase {

    SiMMLSimulatorBaseFM(int type, int channelCount) {
        super(type, channelCount, new SiMMLSimulatorVoiceSet(512, SiOPMTable.PG_SINE, -1), false);
    }

    @Override
    public MMLSequence selectTone(SiMMLTrack track, int voiceIndex) {
        return _selectFMTone(track, voiceIndex);
    }

    /** */
    protected MMLSequence _selectFMTone(SiMMLTrack track, int voiceIndex) {
        if (voiceIndex == -1) return null;

        SiMMLVoice voice;

        if (voiceIndex < 0 || voiceIndex >= SiMMLTable.VOICE_MAX) voiceIndex = 0;
        voice = SiMMLTable.getInstance().getSiMMLVoice(voiceIndex);
        if (voice != null) {
            if (voice.updateTrackParameters) {
                voice.updateTrackVoice(track);
                return null;
            } else {
                // this module changes only channel params, not track params.
                track.channel.setSiOPMChannelParam(voice.channelParam, false, false);
                track._resetVolumeOffset();
                return (voice.channelParam.initSequence.isEmpty()) ? null : voice.channelParam.initSequence;
            }
        }

        return null;
    }
}
