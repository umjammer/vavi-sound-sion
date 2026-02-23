//
// SiOPM effect controlable HPF
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** controllable HPF. */
public class SiCtrlFilterHighPass extends SiCtrlFilterBase {

    /**
     * constructor.
     *
     * @param cutoff    cutoff(0-1).
     * @param resonance resonance(0-1).
     */
    public SiCtrlFilterHighPass(double cutoff, double resonance) {
        initialize();
        control(cutoff, resonance);
    }

    @Override
    protected void processLFO(double[] buffer, int startIndex, int length) {
        int i, imax = startIndex + length;
        double n;
        double cut = _table.filter_cutoffTable[_cutIndex];
        double fb = _res * _table.filter_feedbackTable[_cutIndex];
        for (i = startIndex; i < imax; ) {
            n = buffer[i];
            _p0l += cut * (n - _p0l + fb * (_p0l - _p1l));
            _p1l += cut * (_p0l - _p1l);
            buffer[i] = n - _p0l;
            i++;
            n = buffer[i];
            _p0r += cut * (n - _p0r + fb * (_p0r - _p1r));
            _p1r += cut * (_p0r - _p1r);
            buffer[i] = n - _p0r;
            i++;
        }
    }
}
