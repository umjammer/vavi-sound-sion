//
// SiOPM Notch filter
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Notch. */
public class SiFilterNotch extends SiFilterBase {

    // constructor
    //

    /**
     * constructor.
     *
     * @param freq cutoff frequency[Hz].
     * @param band band width [oct].
     */
    public SiFilterNotch(double freq, double band) {
        setParameters(freq, band);
    }

    // operations
    //

    /**
     * set parameters
     *
     * @param freq cutoff frequency[Hz].
     * @param band band width [oct].
     */
    public void setParameters(double freq, double band) {
        double omg = freq * 0.00014247585730565955, // 2*pi/44100
                cos = Math.cos(omg), sin = Math.sin(omg),
                alp = sin * sinh(0.34657359027997264 * band * omg / sin), // log(2)*0.5
                ia0 = 1 / (1 + alp);
        _b1 = _a1 = -2 * cos * ia0;
        _a2 = (1 - alp) * ia0;
        _b1 = -(1 + cos) * ia0;
        _b2 = _b0 = 1;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(3000, 1);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] : 3000,
                (!Double.isNaN(args[1])) ? args[1] : 1);
    }
}
