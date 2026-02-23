//
// class for SiOPM samplers wave table
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.sion.sequencer.SiMMLTable;


/** SiOPM samplers wave table */
public class SiOPMWaveSamplerTable extends SiOPMWaveBase {

    // valiables
    //

    /** Stencil table, search sample in stencil table before seaching this instances table. */
    public SiOPMWaveSamplerTable stencil;

    // SiOPMWaveSamplerData table to refer from sampler channel.
    private SiOPMWaveSamplerData[] _table;

    // constructor
    //

    /**
     * constructor
     */
    public SiOPMWaveSamplerTable() {
        super(SiMMLTable.MT_SAMPLE);
        _table = new SiOPMWaveSamplerData[SiOPMTable.SAMPLER_DATA_MAX];
        stencil = null;
        clear(null);
    }

    // operations
    //

    /**
     * Clear all of the table.
     *
     * @param sampleData SiOPMWaveSamplerData to fill with.
     * @return this instance
     */
    public SiOPMWaveSamplerTable clear(SiOPMWaveSamplerData sampleData /* = null */) {
        for (int i = 0; i < SiOPMTable.SAMPLER_DATA_MAX; i++) _table[i] = sampleData;
        return this;
    }

    /**
     * Set sample data.
     *
     * @param sample       assignee SiOPMWaveSamplerData
     * @param keyRangeFrom Assigning key range starts from
     * @param keyRangeTo   Assigning key range ends at. -1 to set only at the key of argument "keyRangeFrom".
     * @return assigned SiOPMWaveSamplerData (((sample) same) ((the) passed) 1st argument).
     */
    public SiOPMWaveSamplerData setSample(SiOPMWaveSamplerData sample, int keyRangeFrom, int keyRangeTo /* = -1 */) {
        if (keyRangeFrom < 0) keyRangeFrom = 0;
        if (keyRangeTo > 127) keyRangeTo = 127;
        if (keyRangeTo == -1) keyRangeTo = keyRangeFrom;
        if (keyRangeFrom > 127 || keyRangeTo < 0 || keyRangeTo < keyRangeFrom)
            throw new Error("SiOPMWaveSamplerTable error; Invalid key range");
        for (int i = keyRangeFrom; i <= keyRangeTo; i++) _table[i] = sample;
        return sample;
    }

    /**
     * Get sample data.
     *
     * @param sampleNumber Sample number (0-127).
     * @return assigned SiOPMWaveSamplerData
     */
    public SiOPMWaveSamplerData getSample(int sampleNumber) {
        if (stencil != null) return stencil._table[sampleNumber] != null ? stencil._table[sampleNumber] : _table[sampleNumber];
        return _table[sampleNumber];
    }

    /** free all */
    public void _free() {
        for (int i = 0; i < SiOPMTable.SAMPLER_DATA_MAX; i++) {
            //if (_table[i]) _table[i].free();
            _table[i] = null;
        }
    }
}
