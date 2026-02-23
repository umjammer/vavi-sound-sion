//
// SiOPM effect Hard Distortion
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Hard Distortion. */
public class SiEffectDistortion extends SiEffectBase {

    // constant
    //
    protected static final double THRESHOLD = 0.0000152587890625;

    // variables
    //
    private double _preScale, _limit;
    private boolean _filterEnable;
    private double _a1, _a2, _b0, _b1, _b2;
    private double _in1, _in2, _out1, _out2;

    // constructor
    //

    /**
     * constructor
     *
     * @param preGain  PreGain (dB).
     * @param postGain PostGain (dB).
     * @param lpfFreq  Low pass filter frequency (Hz).
     * @param lpfSlope Low pass filter slope (oct/6dB).
     */
    public SiEffectDistortion(double preGain, double postGain, double lpfFreq, double lpfSlope) {
        setParameters(preGain, postGain, 2400, 1);
    }

    // operations
    //

    /**
     * set parameters
     *
     * @param preGain  PreGain (dB).
     * @param postGain PostGain (dB).
     * @param lpfFreq  Low pass filter frequency (Hz).
     * @param lpfSlope Low pass filter slope (oct/6dB).
     */
    public void setParameters(double preGain, double postGain, double lpfFreq, double lpfSlope) {
        double postScale = Math.pow(2, -postGain / 6);
        _preScale = Math.pow(2, -preGain / 6) * postScale;
        _limit = postScale;
        _filterEnable = (lpfFreq > 0);
        if (_filterEnable) {
            double omg = lpfFreq * 0.00014247585730565955; // 2*pi/44100
            double cos = Math.cos(omg), sin = Math.sin(omg);
            double ang = 0.34657359027997264 * lpfSlope * omg / sin;
            double alp = sin * (Math.exp(ang) - Math.exp(-ang)) * 0.5; // log(2)*0.5
            double ia0 = 1 / (1 + alp);
            _a1 = -2 * cos * ia0;
            _a2 = (1 - alp) * ia0;
            _b1 = (1 - cos) * ia0;
            _b2 = _b0 = _b1 * 0.5;
        }
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(-60, 18, 2400, 1);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] : -60,
                (!Double.isNaN(args[1])) ? args[1] : 18,
                (!Double.isNaN(args[2])) ? args[2] : 2400,
                (!Double.isNaN(args[3])) ? args[3] : 1);
    }

    @Override
    public int prepareProcess() {
        _in1 = _in2 = _out1 = _out2 = 0;
        return 1;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        if (_out1 < THRESHOLD) _out2 = _out1 = 0;
        int i;
        double n, out;
        int imax = startIndex + length;
        if (_filterEnable) {
            for (i = startIndex; i < imax; i++) {
                n = buffer[i];
                n *= _preScale;
                if (n < -_limit) n = -_limit;
                else if (n > _limit) n = _limit;
                out = _b0 * n + _b1 * _in1 + _b2 * _in2 - _a1 * _out1 - _a2 * _out2;
                _in2 = _in1;
                _in1 = n;
                _out2 = _out1;
                _out1 = out;
                buffer[i] = out;
                i++;
                buffer[i] = out;
            }
        } else {
            for (i = startIndex; i < imax; i++) {
                n = buffer[i];
                n *= _preScale;
                if (n < -_limit) n = -_limit;
                else if (n > _limit) n = _limit;
                buffer[i] = n;
                i++;
                buffer[i] = n;
            }
        }
        return 1;
    }
}
