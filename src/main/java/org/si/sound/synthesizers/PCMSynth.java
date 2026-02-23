// Pulse Code Modulation Synthesizer 
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;


/**
 * Pulse Code Modulation Synthesizer
 */
public class PCMSynth extends IFlashSoundOperator {

    // variables
    //

    /** PCM table */
    protected SiOPMWavePCMTable _pcmTable;
    /** default PCM data */
    protected SiOPMWavePCMData _defaultPCMData;
        
    // properties
    //

    // constructor
    //

    /**
     * constructor
     *
     * @param data         wave data, Sound or Vector.&lt;Number&gt; can be set, the Sound instanceof extracted inside.
     * @param samplingNote sampling data's note, this argument allows decimal number.
     * @param channelCount channel count of playing PCM.
     */
    public PCMSynth(Object data, double samplingNote, int channelCount) {
        _defaultPCMData = new SiOPMWavePCMData(data, (int) (samplingNote * 64), channelCount, 0);
        _pcmTable = new SiOPMWavePCMTable();
        _pcmTable.clear(_defaultPCMData);
        _voice.waveData = _pcmTable;
    }

    // operation
    //

    /**
     * Set PCM sample with key range (this feature instanceof not available in currennt version).
     *
     * @param data         wave data, Sound or Vector.&lt;Number&gt; can be set, the Sound instanceof extracted inside.
     * @param samplingNote sampling data's note, this argument allows decimal number.
     * @param keyRangeFrom Assigning key range starts from
     * @param keyRangeTo   Assigning key range ends at. -1 to set only at the key of argument "keyRangeFrom".
     * @param channelCount channel count of this data, 1 for monaural, 2 for stereo
     * @return assigned SiOPMWavePCMData.
     */
    public SiOPMWavePCMData setSample(Object data, double samplingNote, int keyRangeFrom, int keyRangeTo, int channelCount) {
        SiOPMWavePCMData pcmData;
        if (keyRangeFrom == 0 && keyRangeTo == 127) {
            _defaultPCMData.initialize(data, (int) (samplingNote * 64), channelCount, 0);
            pcmData = _defaultPCMData;
        } else {
            pcmData = new SiOPMWavePCMData(data, (int) (samplingNote * 64), channelCount, 0);
        }
        _voiceUpdateNumber++;
        return _pcmTable.setSample(pcmData, keyRangeFrom, keyRangeTo);
    }
}
