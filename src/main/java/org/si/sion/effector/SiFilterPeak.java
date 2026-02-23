//
// SiOPM Peaking filter
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Peaking EQ. */
public class SiFilterPeak extends SiFilterBase {

    // constructor
    //

    /**
     * constructor.
     *
     * @param freq cutoff frequency[Hz].
     * @param band band width [oct].
     * @param gain gain [dB].
     */
    public SiFilterPeak(double freq, double band, double gain) {
        setParameters(freq, band, 6);
    }

    // operations
    //

    /**
     * set parameters
     *
     * @param freq cutoff frequency[Hz].
     * @param band band width [oct].
     * @param gain gain [dB].
     */
    public void setParameters(double freq, double band, double gain) {
        double A = Math.pow(10, gain * 0.025),
                omg = freq * 0.00014247585730565955, // 2*pi/44100
                cos = Math.cos(omg), sin = Math.sin(omg),
                alp = sin * sinh(0.34657359027997264 * band * omg / sin), // log(2)*0.5
                alpA = alp * A, alpiA = alp / A,
                ia0 = 1 / (1 + alpiA);
        _b1 = _a1 = -2 * cos * ia0;
        _a2 = (1 - alpiA) * ia0;
        _b0 = (1 + alpA) * ia0;
        _b2 = (1 - alpA) * ia0;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(3000, 1, 6);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] : 3000,
                (!Double.isNaN(args[1])) ? args[1] : 1,
                (!Double.isNaN(args[2])) ? args[2] : 6);
    }
}
