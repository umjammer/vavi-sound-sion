//
// SiOPM High booster
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** High booster. */
public class SiFilterHighBoost extends SiFilterBase {

    // constructor
    //

    /**
     * constructor.
     *
     * @param freq  shelfing frequency[Hz].
     * @param slope slope, 1 for steepest slope.
     * @param gain  gain [dB].
     */
    public SiFilterHighBoost(double freq, double slope, double gain) {
        setParameters(freq, slope, gain);
    }

    // operations
    //

    /**
     * set parameters
     *
     * @param freq  shelfing frequency[Hz].
     * @param slope slope, 1 for steepest slope.
     * @param gain  gain [dB].
     */
    public void setParameters(double freq, double slope, double gain) {
        if (slope < 1) slope = 1;
        double A = Math.pow(10, gain * 0.025),
                omg = freq * 0.00014247585730565955, // 2 * pi / 44100
                cos = Math.cos(omg), sin = Math.sin(omg),
                alp = sin * 0.5 * Math.sqrt((A + 1 / A) * (1 / slope - 1) + 2),
                alpsA2 = alp * Math.sqrt(A) * 2,
                ia0 = 1 / ((A + 1) - (A - 1) * cos + alpsA2);
        _a1 = 2 * ((A - 1) - (A + 1) * cos) * ia0;
        _a2 = ((A + 1) - (A - 1) * cos - alpsA2) * ia0;
        _b0 = ((A + 1) + (A - 1) * cos + alpsA2) * A * ia0;
        _b1 = -2 * ((A - 1) + (A + 1) * cos) * A * ia0;
        _b2 = ((A + 1) + (A - 1) * cos - alpsA2) * A * ia0;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(5500, 1, 6);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] : 5500,
                (!Double.isNaN(args[1])) ? args[1] : 1,
                (!Double.isNaN(args[2])) ? args[2] : 6);
    }
}
