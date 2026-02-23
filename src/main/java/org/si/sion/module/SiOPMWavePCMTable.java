//
// class for SiOPM PCM data
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.sion.sequencer.SiMMLTable;


/** PCM data class */
public class SiOPMWavePCMTable extends SiOPMWaveBase {

    // variables
    //

    /** PCM wave data assign table for each note. */
    public SiOPMWavePCMData[] _table;
    /** volume table */
    public double[] _volumeTable;
    /** pan table */
    public int[] _panTable;

    // constructor
    //

    /** Constructor */
    public SiOPMWavePCMTable() {
        super(SiMMLTable.MT_PCM);
        _table = new SiOPMWavePCMData[SiOPMTable.NOTE_TABLE_SIZE];
        _volumeTable = new double[SiOPMTable.NOTE_TABLE_SIZE];
        _panTable = new int[SiOPMTable.NOTE_TABLE_SIZE];
        clear(null);
    }

    // operations
    //

    /**
     * Clear all of the table.
     *
     * @param pcmData SiOPMWavePCMData to fill layer0's pcm.
     * @return this instance
     */
    public SiOPMWavePCMTable clear(SiOPMWavePCMData pcmData) {
        int i;
        for (i = 0; i < SiOPMTable.NOTE_TABLE_SIZE; i++) {
            _table[i] = pcmData;
            _volumeTable[i] = 1;
            _panTable[i] = 0;
        }
        return this;
    }

    /**
     * Set sample data.
     *
     * @param pcmData      assignee SiOPMWavePCMData
     * @param keyRangeFrom Assigning key range starts from
     * @param keyRangeTo   Assigning key range ends at. -1 to set only at the key of argument "keyRangeFrom".
     * @return assigned PCM data (((pcmData) same) ((the) passed) 1st argument.)
     */
    public SiOPMWavePCMData setSample(SiOPMWavePCMData pcmData, int keyRangeFrom, int keyRangeTo) {
        if (keyRangeFrom < 0) keyRangeFrom = 0;
        if (keyRangeTo > 127) keyRangeTo = 127;
        if (keyRangeTo == -1) keyRangeTo = keyRangeFrom;
        if (keyRangeFrom > 127 || keyRangeTo < 0 || keyRangeTo < keyRangeFrom)
            throw new Error("SiOPMWavePCMTable error; Invalid key range");
        for (int i = keyRangeFrom; i <= keyRangeTo; i++) _table[i] = pcmData;
        return pcmData;
    }

    /**
     * update key scale volume
     *
     * @param centerNoteNumber note number of volume changing center
     * @param keyRange         key range of volume changing notes
     * @param volumeRange      range of volume changing (128 for full volouming)
     * @return this instance
     */
    public SiOPMWavePCMTable setKeyScaleVolume(int centerNoteNumber, double keyRange, double volumeRange) {
        volumeRange *= 0.0078125;
        int imin = (int) (centerNoteNumber - keyRange * 0.5);
        double imax = centerNoteNumber + keyRange * 0.5;
        double v;
        double dv = (keyRange == 0) ? volumeRange : (volumeRange / keyRange);
        int i;
        if (volumeRange > 0) {
            v = 1 - volumeRange;
            for (i = 0; i < imin; i++) _volumeTable[i] = v;
            for (; i < imax; i++, v += dv) _volumeTable[i] = v;
            for (; i < SiOPMTable.NOTE_TABLE_SIZE; i++) _volumeTable[i] = 1;
        } else {
            v = 1;
            for (i = 0; i < imin; i++) _volumeTable[i] = 1;
            for (; i < imax; i++, v += dv) _volumeTable[i] = v;
            v = 1 + volumeRange;
            for (; i < SiOPMTable.NOTE_TABLE_SIZE; i++) _volumeTable[i] = v;
        }
        return this;
    }

    /**
     * update key scale panning
     *
     * @param centerNoteNumber note number of panning center
     * @param keyRange         key range of panning notes
     * @param panWidth         panning width for all of key range (128 for full panning)
     * @return this instance
     */
    public SiOPMWavePCMTable setKeyScalePan(int centerNoteNumber, double keyRange, double panWidth) {
        double imin = centerNoteNumber - keyRange * 0.5, imax = centerNoteNumber + keyRange * 0.5,
                p = -panWidth * 0.5, dp = (keyRange == 0) ? panWidth : (panWidth / keyRange);
        int i;
        for (i = 0; i < imin; i++) _panTable[i] = (int) p;
        for (; i < imax; i++, p += dp) _panTable[i] = (int) p;
        for (p = panWidth * 0.5; i < SiOPMTable.NOTE_TABLE_SIZE; i++) _panTable[i] = (int) p;
        return this;
    }

    /** free all */
    void _free() {
        for (int i = 0; i < SiOPMTable.NOTE_TABLE_SIZE; i++) {
            //if (_table[i]) _table[i].free();
            _table[i] = null;
        }
    }
}
