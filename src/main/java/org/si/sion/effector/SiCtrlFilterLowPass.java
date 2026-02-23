//
// SiOPM effect controlable LPF
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** controllable LPF. */
public class SiCtrlFilterLowPass extends SiCtrlFilterBase {

    /**
     * constructor.
     *
     * @param cutoff    cutoff(0-1).
     * @param resonance resonance(0-1).
     */
    public SiCtrlFilterLowPass(double cutoff, double resonance) {
        initialize();
        control(cutoff, resonance);
    }

    @Override
    protected void processLFO(double[] buffer, int startIndex, int length) {
        int i, n, imax = startIndex + length;
        double cut = _table.filter_cutoffTable[_cutIndex];
        double fb = _res * _table.filter_feedbackTable[_cutIndex];
        for (i = startIndex; i < imax; ) {
            _p0l += cut * (buffer[i] - _p0l + fb * (_p0l - _p1l));
            _p1l += cut * (_p0l - _p1l);
            buffer[i] = _p1l;
            i++;
            _p0r += cut * (buffer[i] - _p0r + fb * (_p0r - _p1r));
            _p1r += cut * (_p0r - _p1r);
            buffer[i] = _p1r;
            i++;
        }
    }
}
