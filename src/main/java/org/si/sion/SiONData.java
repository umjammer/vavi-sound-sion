//
// SiON data
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion;

import org.si.sion.module.ISiOPMWaveInterface;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerData;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.sequencer.SiMMLData;
import org.si.as3.media.Sound;


public class SiONData extends SiMMLData implements ISiOPMWaveInterface {

    public SiONData() {
    }

    @Override
    public SiOPMWavePCMData setPCMWave(int index, Object data, double samplingNote, int keyRangeFrom, int keyRangeTo, int srcChannelCount, int channelCount) {
        SiOPMWavePCMTable pcmTable = (SiOPMWavePCMTable) _getPCMVoice(index).waveData;
        if (pcmTable != null) {
            SiOPMWavePCMData pcmData = new SiOPMWavePCMData(data, (int) (samplingNote * 64), srcChannelCount, channelCount);
            pcmTable.setSample(pcmData, keyRangeFrom, keyRangeTo);
            return pcmData;
        }
        return null;
    }

    @Override
    public SiOPMWaveSamplerData setSamplerWave(int index, Object data, boolean ignoreNoteOff, int pan, int srcChannelCount, int channelCount) {
        int bank = (index >> SiOPMTable.NOTE_BITS) & (SiOPMTable.SAMPLER_TABLE_MAX - 1);
        SiOPMWaveSamplerTable table = samplerTables[bank];
        SiOPMWaveSamplerData sampleData = new SiOPMWaveSamplerData(data, ignoreNoteOff, pan, srcChannelCount, channelCount, null);
        table.setSample(sampleData, index & (SiOPMTable.NOTE_TABLE_SIZE - 1), 0);
        return sampleData;
    }

    public void setPCMVoice(int index, SiONVoice voice) {
        pcmVoices[index & (pcmVoices.length - 1)] = voice;
    }

    public void setSamplerTable(int bank, SiOPMWaveSamplerTable table) {
        samplerTables[bank & (samplerTables.length - 1)] = table;
    }

    public SiOPMWavePCMData setPCMData(int index, double[] data, int samplingOctave /* = 5 */, int keyRangeFrom /* = 0 */, int keyRangeTo /* = 127 */, boolean isSourceDataStereo /* = false */) {
        return setPCMWave(index, data, samplingOctave * 12 + 8, keyRangeFrom, keyRangeTo, (isSourceDataStereo) ? 2 : 1, 0);
    }

    public SiOPMWavePCMData setPCMSound(int index, Sound sound, int samplingOctave /* = 5 */, int keyRangeFrom /* = 0 */, int keyRangeTo /* = 127 */) {
        return setPCMWave(index, sound, samplingOctave * 12 + 8, keyRangeFrom, keyRangeTo, 1, 0);
    }

    public SiOPMWaveSamplerData setSamplerData(int index, double[] data, boolean ignoreNoteOff, int channelCount) {
        return setSamplerWave(index, data, ignoreNoteOff, 0, channelCount, 0);
    }

    public SiOPMWaveSamplerData setSamplerSound(int index, Sound sound, boolean ignoreNoteOff, int channelCount) {
        return setSamplerWave(index, sound, ignoreNoteOff, 0, channelCount, 0);
    }
}
