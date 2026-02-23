//
// SiMML data
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.module.SiOPMWaveTable;
import org.si.sion.sequencer.base.MMLData;


/** SiMML data class. */
public class SiMMLData extends MMLData {

    // variables
    //

    /** envelope tables */
    public SiMMLEnvelopTable[] envelopes;

    /** wave tables */
    public SiOPMWaveTable[] waveTables;

    /** FM voice data */
    public SiMMLVoice[] fmVoices;

    /** pcm data (log-transformed) */
    public SiMMLVoice[] pcmVoices;

    /** wave data */
    public SiOPMWaveSamplerTable[] samplerTables;

    // properties
    //

    /** [NOT RECOMMENDED] This property instanceof for the compatibility of previous versions, please use fmVoices instead of this. @see #fmVoices */
    public SiMMLVoice[] getVoices() {
        return fmVoices;
    }

    // constructor
    //

    /** constructor. */
    public SiMMLData() {
        envelopes = new SiMMLEnvelopTable[SiMMLTable.ENV_TABLE_MAX];
        waveTables = new SiOPMWaveTable[SiOPMTable.WAVE_TABLE_MAX];
        fmVoices = new SiMMLVoice[SiMMLTable.VOICE_MAX];
        pcmVoices = new SiMMLVoice[SiOPMTable.PCM_DATA_MAX];
        samplerTables = new SiOPMWaveSamplerTable[SiOPMTable.SAMPLER_TABLE_MAX];
        for (int i = 0; i < SiOPMTable.SAMPLER_TABLE_MAX; i++) {
            samplerTables[i] = new SiOPMWaveSamplerTable();
        }
    }

    // operations
    //

    /** Clear all parameters and free all sequence groups. */
    @Override
    public void clear() {
        super.clear();

        int i;
        SiOPMWavePCMTable pcm;
        for (i = 0; i < SiMMLTable.ENV_TABLE_MAX; i++) envelopes[i] = null;
        for (i = 0; i < SiMMLTable.VOICE_MAX; i++) fmVoices[i] = null;
        for (i = 0; i < SiOPMTable.WAVE_TABLE_MAX; i++) {
            if (waveTables[i] != null) {
                waveTables[i].free();
                waveTables[i] = null;
            }
        }
        for (i = 0; i < SiOPMTable.PCM_DATA_MAX; i++) {
            if (pcmVoices[i] != null) {
                pcm = ((SiOPMWavePCMTable) pcmVoices[i].waveData);
                pcmVoices[i] = null;
            }
        }
    }

    /**
     * Set envelope table data refered by &#64;&#64;,na,np,nt,nf,_&#64;&#64;,_na,_np,_nt and _nf.
     *
     * @param index    envelope table number.
     * @param envelope envelope table.
     */
    public void setEnvelopTable(int index, SiMMLEnvelopTable envelope) {
        if (index >= 0 && index < SiMMLTable.ENV_TABLE_MAX) envelopes[index] = envelope;
    }

    /**
     * Set wave table data refered by %6.
     *
     * @param index wave table number.
     * @param voice voice to register.
     */
    public void setVoice(int index, SiMMLVoice voice) {
        if (index >= 0 && index < SiMMLTable.VOICE_MAX) {
            if (!voice.isSuitableForFMVoice()) throw errorNotGoodFMVoice();
            fmVoices[index] = voice;
        }
    }

    /**
     * Set wave table data refered by %4.
     *
     * @param index wave table number.
     * @param data  Vector.&lt;Number&gt; wave shape data ranged from -1 to 1.
     * @return created data instance
     */
    public SiOPMWaveTable setWaveTable(int index, double[] data) {
        index &= SiOPMTable.WAVE_TABLE_MAX - 1;
        int i, imax = data.length;
        int[] table = new int[imax];
        for (i = 0; i < imax; i++) table[i] = SiOPMTable.calcLogTableIndex(data[i]);
        waveTables[index] = SiOPMWaveTable.alloc(table, 0);
        return waveTables[index];
    }

    // internal function
    //

    /** Get channel parameter */
    SiOPMChannelParam _getSiOPMChannelParam(int index) {
        SiMMLVoice v = new SiMMLVoice();
        v.channelParam = new SiOPMChannelParam();
        fmVoices[index] = v;
        return v.channelParam;
    }

    /** Get CPM SiMMLVoice */
    protected SiMMLVoice _getPCMVoice(int index) {
        index &= (SiOPMTable.PCM_DATA_MAX - 1);
        if (pcmVoices[index] == null) {
            pcmVoices[index] = new SiMMLVoice();
            return pcmVoices[index]._newBlankPCMVoice(index);
        }
        return pcmVoices[index];
    }

    /** register all tables. called from SiMMLTrack._prepareBuffer(). */
    void _registerAllTables() {
        /**/ // currently bank2,3 are not avairable
        SiOPMTable._instance.samplerTables[0].stencil = samplerTables[0];
        SiOPMTable._instance.samplerTables[1].stencil = samplerTables[1];
        SiOPMTable._instance._stencilCustomWaveTables = waveTables;
        SiOPMTable._instance._stencilPCMVoices = pcmVoices;
        SiMMLTable._instance._stencilEnvelops = envelopes;
        SiMMLTable._instance._stencilVoices = fmVoices;
    }

    // error
    //
    private RuntimeException errorNotGoodFMVoice() {
        return new RuntimeException("SiONDriver error; Cannot register the voice.");
    }
}
