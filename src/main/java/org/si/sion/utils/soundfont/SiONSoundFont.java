//
// SiON sound font loader
//  Copyright (c) 2011 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils.soundfont;

import org.si.sion.SiONData;
import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.module.SiOPMWaveTable;
import org.si.sion.sequencer.SiMMLEnvelopTable;
import org.si.sion.sequencer.SiMMLTable;


/** SiON Sound font class. */
public class SiONSoundFont {

    // variables
    //

    /** all loaded Sound instances, access them by id */
    public Object sounds;

    /** all SiMMLEnvelopTable instances */
    public SiMMLEnvelopTable[] envelopes = new SiMMLEnvelopTable[SiMMLTable.ENV_TABLE_MAX];

    /** all SiOPMWaveTable instances */
    public SiOPMWaveTable[] waveTables = new SiOPMWaveTable[SiOPMTable.WAVE_TABLE_MAX];

    /** all fm voice instances */
    public SiONVoice[] fmVoices = new SiONVoice[SiMMLTable.VOICE_MAX];

    /** all pcm voice instances */
    public SiONVoice[] pcmVoices = new SiONVoice[SiOPMTable.PCM_DATA_MAX];

    /** all sampler table instances */
    public SiOPMWaveSamplerTable[] samplerTables = new SiOPMWaveSamplerTable[SiOPMTable.SAMPLER_TABLE_MAX];

    /** default FPS */
    public double defaultFPS = 60;
    /** default velocity mode */
    public int defaultVelocityMode = 0;
    /** default expression mode */
    public int defaultExpressionMode = 0;
    /** default v command shift */
    public int defaultVCommandShift = 4;

    // constructor
    //

    /** constructor */
    public SiONSoundFont(Object sounds) {
        this.sounds = sounds != null ? sounds : new java.util.HashMap<>();
    }

    /**
     * apply sound font to SiONData or SiONDriver.
     *
     * @param data               SiONData to apply this font. null to set SiONDriver.
     * @param pcmVoiceOffset     index offset for pcmVoices
     * @param samplerTableOffset index offset for samplerTable
     * @param fmVoiceOffset      index offset for fmVoices
     * @param waveTableOffset    index offset for waveTables
     * @param envelopeOffset     index offset for envelopes
     */
    public void apply(SiONData data, int pcmVoiceOffset, int samplerTableOffset, int fmVoiceOffset, int waveTableOffset, int envelopeOffset) {
        int i;
        if (data != null) {
            for (i = 0; i < pcmVoices.length; i++) if (pcmVoices[i] != null) data.pcmVoices[pcmVoiceOffset + i] = pcmVoices[i];
            for (i = 0; i < samplerTables.length; i++)
                if (samplerTables[i] != null) data.samplerTables[samplerTableOffset + i] = samplerTables[i];
            for (i = 0; i < fmVoices.length; i++) if (fmVoices[i] != null) data.fmVoices[fmVoiceOffset + i] = fmVoices[i];
            for (i = 0; i < waveTables.length; i++)
                if (waveTables[i] != null) data.waveTables[waveTableOffset + i] = waveTables[i];
            for (i = 0; i < envelopes.length; i++) if (envelopes[i] != null) data.envelopes[envelopeOffset + i] = envelopes[i];
            data.defaultFPS = (int)defaultFPS;
            data.defaultVelocityMode = defaultVelocityMode;
            data.defaultExpressionMode = defaultExpressionMode;
            data.defaultVCommandShift = defaultVCommandShift;
        } else {
            SiONDriver driver = SiONDriver.mutex();
            if (driver != null) {
                for (i = 0; i < pcmVoices.length; i++)
                    if (pcmVoices[i] != null) driver.setPCMVoice(pcmVoiceOffset + i, pcmVoices[i]);
                for (i = 0; i < samplerTables.length; i++)
                    if (samplerTables[i] != null) driver.setSamplerTable(samplerTableOffset + i, samplerTables[i]);
                for (i = 0; i < fmVoices.length; i++) if (fmVoices[i] != null) driver.setVoice(fmVoiceOffset + i, fmVoices[i]);
                for (i = 0; i < waveTables.length; i++) {
                    if (waveTables[i] != null) {
                        SiOPMTable._instance.registerWaveTable
                        (waveTableOffset + i, new SiOPMWaveTable().copyFrom(waveTables[i]).wavelet);
                    }
                }
                for (i = 0; i < envelopes.length; i++) {
                    if (envelopes[i] != null) {
                        SiMMLTable.registerMasterEnvelopTable(envelopeOffset + i, new SiMMLEnvelopTable(null, 0).copyFrom(envelopes[i]));
                    }
                }
            }
        }
    }
}
