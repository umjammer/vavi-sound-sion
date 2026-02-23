//
// SiOPM effect wave shaper
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Stereo wave shaper. */
public class SiEffectWaveShaper extends SiEffectBase {

    // variables
    //

    private int _coefficient;
    private double _outputLevel;

    // constructor
    //

    /**
     * constructor
     *
     * @param distortion  distortion(0-1).
     * @param outputLevel output level(0-1).
     */
    public SiEffectWaveShaper(double distortion, double outputLevel) {
        setParameters(distortion, outputLevel);
    }

    // operations
    //

    /**
     * set parameters
     *
     * @param distortion  distortion(0-1).
     * @param outputLevel output level(0-1).
     */
    public void setParameters(double distortion, double outputLevel) {
        if (distortion >= 1) distortion = 0.9999847412109375; // 65535 / 65536
        _coefficient = (int) (2 * distortion / (1 - distortion));
        _outputLevel = outputLevel;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(0.5, 1.0);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] * 0.01 : 0.5,
                (!Double.isNaN(args[1])) ? args[1] * 0.01 : 1.0);
    }

    @Override
    public int prepareProcess() {
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        int i;
        double n, c1 = (1 + _coefficient) * _outputLevel;
        int imax = startIndex + length;
        if (channels == 2) {
            for (i = startIndex; i < imax; i++) {
                n = buffer[i];
                buffer[i] = c1 * n / (1 + _coefficient * ((n < 0) ? -n : n));
            }
        } else {
            for (i = startIndex; i < imax; ) {
                n = buffer[i];
                n = c1 * n / (1 + _coefficient * ((n < 0) ? -n : n));
                buffer[i] = n;
                i++;
                buffer[i] = n;
                i++;
            }
        }
        return channels;
    }
}
