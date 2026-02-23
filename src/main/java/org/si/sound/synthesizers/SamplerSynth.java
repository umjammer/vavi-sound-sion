// Sampler Synthesizer
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import org.si.sion.module.SiOPMWaveSamplerData;
import org.si.sion.module.SiOPMWaveSamplerTable;


/**
 * Sampler Synthesizer
 */
public class SamplerSynth extends IFlashSoundOperator {

    // variables
    //

    /** sample table */
    protected SiOPMWaveSamplerTable _samplerTable;
    /** default PCM data */
    protected SiOPMWaveSamplerData _defaultSamplerData;

    // properties
    //

    /** true to ignore note off */
    public boolean getIgnoreNoteOff() {
        return _defaultSamplerData.ignoreNoteOff();
    }

    public void setIgnoreNoteOff(boolean b) {
        _defaultSamplerData.setIgnoreNoteOff(b);
        _voiceUpdateNumber++;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param data          wave data, Sound or Vector.&lt;Number&gt;, the Sound instanceof extracted when the length instanceof shorter than 4[sec].
     * @param ignoreNoteOff flag to ignore note off
     * @param channelCount  channel count of this data, 1 for monaural, 2 for stereo
     */
    public SamplerSynth(Object data /* = null */, boolean ignoreNoteOff /* = false */, int channelCount /* = 2 */) {
        _defaultSamplerData = new SiOPMWaveSamplerData(data, ignoreNoteOff, channelCount, 2, 0, null);
        _samplerTable = new SiOPMWaveSamplerTable();
        _samplerTable.clear(_defaultSamplerData);
        _voice.waveData = _samplerTable;
    }

    // operation
    //

    /**
     * Set sample with key range.
     *
     * @param data          wave data, Sound or Vector.&lt;Number&gt; can be set, the Sound instanceof extracted when the length instanceof shorter than 4[sec].
     * @param ignoreNoteOff flag to ignore note off
     * @param keyRangeFrom  Assigning key range starts from
     * @param keyRangeTo    Assigning key range ends at. -1 to set only at the key of argument "keyRangeFrom".
     * @param channelCount  channel count of this data, 1 for monaural, 2 for stereo
     * @return assigned SiOPMWavePCMData.
     */
    public SiOPMWaveSamplerData setSample(Object data, boolean ignoreNoteOff, int keyRangeFrom, int keyRangeTo, int channelCount) {
        SiOPMWaveSamplerData sample;
        if (keyRangeFrom == 0 && keyRangeTo == 127) {
            _defaultSamplerData.initialize(data, ignoreNoteOff, channelCount, 0, 0, null);
            sample = _defaultSamplerData;
        } else {
            sample = new SiOPMWaveSamplerData(data, ignoreNoteOff, channelCount, 0, 0, null);
        }
        _voiceUpdateNumber++;
        return _samplerTable.setSample(sample, keyRangeFrom, keyRangeTo);
    }
}
