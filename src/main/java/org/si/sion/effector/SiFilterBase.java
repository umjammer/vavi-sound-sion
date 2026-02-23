//
// SiOPM filters based on RBJ cockbook
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** filters based on RBJ cockbook. */
public class SiFilterBase extends SiEffectBase {

    // constant
    //
    protected static final double THRESHOLD = 0.0000152587890625;

    // variables
    //
    protected double _a1, _a2, _b0, _b1, _b2;
    private double _in1L, _in2L, _out1L, _out2L;
    private double _in1R, _in2R, _out1R, _out2R;

    // Math calculation
    //

    /** hyperbolic sinh. */
    protected double sinh(double n) {
        return (Math.exp(n) - Math.exp(-n)) * 0.5;
    }

    // constructor
    //

    /** constructor */
    public SiFilterBase() {
    }

    // override functions
    //

    @Override
    public int prepareProcess() {
        _in1L = _in2L = _out1L = _out2L = _in1R = _in2R = _out1R = _out2R = 0;
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        if (_out1L < THRESHOLD) _out2L = _out1L = 0;
        if (_out1R < THRESHOLD) _out2R = _out1R = 0;

        int i;
        double input, output;
        int imax = startIndex + length;
        if (channels == 2) {
            for (i = startIndex; i < imax; ) {
                input = buffer[i];
                output = _b0 * input + _b1 * _in1L + _b2 * _in2L - _a1 * _out1L - _a2 * _out2L;
                if (output > 1) output = 1;
                else if (output < -1) output = -1;
                _in2L = _in1L;
                _in1L = input;
                _out2L = _out1L;
                _out1L = output;
                buffer[i] = output;
                i++;

                input = buffer[i];
                output = _b0 * input + _b1 * _in1R + _b2 * _in2R - _a1 * _out1R - _a2 * _out2R;
                if (output > 1) output = 1;
                else if (output < -1) output = -1;
                _in2R = _in1R;
                _in1R = input;
                _out2R = _out1R;
                _out1R = output;
                buffer[i] = output;
                i++;
            }
        } else {
            for (i = startIndex; i < imax; ) {
                input = buffer[i];
                output = _b0 * input + _b1 * _in1L + _b2 * _in2L - _a1 * _out1L - _a2 * _out2L;
                if (output > 1) output = 1;
                else if (output < -1) output = -1;
                _in2L = _in1L;
                _in1L = input;
                _out2L = _out1L;
                _out1L = output;
                buffer[i] = output;
                i++;
                buffer[i] = output;
                i++;
            }
        }
        return channels;
    }
}
