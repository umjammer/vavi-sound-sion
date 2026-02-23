//
// class for SiOPM wave table
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.sequencer.SiMMLTable;


/** SiOPM wave table */
public class SiOPMWaveTable extends SiOPMWaveBase {

    public int[] wavelet;
    public int fixedBits;
    public int defaultPTType;

    /** create new SiOPMWaveTable instance. */
    public SiOPMWaveTable() {
        super(SiMMLTable.MT_CUSTOM);
        this.wavelet = null;
        this.fixedBits = 0;
        this.defaultPTType = 0;
    }

    /**
     * initialize
     *
     * @param wavelet       wave table in log scale.
     * @param defaultPTType default pitch table type.
     */
    public SiOPMWaveTable initialize(int[] wavelet, int defaultPTType /* = 0 */) {
        int len, bits = 0;
        for (len = wavelet.length >> 1; len != 0; len >>= 1) bits++;

        this.wavelet = wavelet;
        this.fixedBits = SiOPMTable.PHASE_BITS - bits;
        this.defaultPTType = defaultPTType;

        return this;
    }

    /**
     * copy
     *
     * @return this instance
     */
    public SiOPMWaveTable copyFrom(SiOPMWaveTable src) {
        int i, imax = src.wavelet.length;
        this.wavelet = new int[imax];
        for (i = 0; i < imax; i++) this.wavelet[i] = src.wavelet[i];
        this.fixedBits = src.fixedBits;
        this.defaultPTType = src.defaultPTType;

        return this;
    }

    /** free. */
    public void free() {
        _freeList.add(this);
    }

    private static final List<SiOPMWaveTable> _freeList = new ArrayList<>();

    /** allocate. */
    public static SiOPMWaveTable alloc(int[] wavelet, int defaultPTType /* = 0 */) {
        SiOPMWaveTable newInstance = _freeList.isEmpty() ? new SiOPMWaveTable() : _freeList.remove(_freeList.size() - 1);
        return newInstance.initialize(wavelet, defaultPTType);
    }
}
