//
// SiOPM effect stereo reverb
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Stereo reverb effector. */
public class SiEffectStereoReverb extends SiEffectBase {

    // variables
    //

    private final int DELAY_BUFFER_BITS = 13;
    private final int DELAY_BUFFER_FILTER = (1 << DELAY_BUFFER_BITS) - 1;

    private double[] _delayBufferL, _delayBufferR;
    private int _pointerRead0, _pointerRead1, _pointerRead2;
    private int _pointerWrite;
    private double _feedback0, _feedback1, _feedback2;
    private double _wet;

    // constructor
    //

    /**
     * constructor
     *
     * @param delay1   long delay(0-1).
     * @param delay2   short delay(0-1).
     * @param feedback feedback decay(-1-1). Negative value to invert phase.
     * @param wet      mixing level(0-1).
     */
    public SiEffectStereoReverb(double delay1, double delay2, double feedback, double wet) {
        _delayBufferL = new double[1 << DELAY_BUFFER_BITS];
        _delayBufferR = new double[1 << DELAY_BUFFER_BITS];
        setParameters(delay1, delay2, feedback, wet);
    }

    // operation
    //

    /**
     * set parameters
     *
     * @param delay1   long delay(0-1).
     * @param delay2   short delay(0-1).
     * @param feedback feedback decay(-1-1). Negative value to invert phase.
     * @param wet      mixing level(0-1).
     */
    public void setParameters(double delay1, double delay2, double feedback, double wet) {
        if (delay1 < 0.01) delay1 = 0.01;
        else if (delay1 > 0.99) delay1 = 0.99;
        if (delay2 < 0.01) delay2 = 0.01;
        else if (delay2 > 0.99) delay2 = 0.99;
        _pointerWrite = (_pointerRead0 + DELAY_BUFFER_FILTER) & DELAY_BUFFER_FILTER;
        _pointerRead1 = (int) (_pointerRead0 + DELAY_BUFFER_FILTER * (1 - delay1)) & DELAY_BUFFER_FILTER;
        _pointerRead2 = (int) (_pointerRead0 + DELAY_BUFFER_FILTER * (1 - delay2)) & DELAY_BUFFER_FILTER;
        if (feedback > 0.99) feedback = 0.99;
        else if (feedback < -0.99) feedback = -0.99;
        _feedback0 = feedback * 0.2;
        _feedback1 = feedback * 0.3;
        _feedback2 = feedback * 0.5;
        _wet = wet;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(0.7, 0.4, 0.8, 0.3);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? (args[0] * 0.01) : 0.7,
                (!Double.isNaN(args[1])) ? (args[1] * 0.01) : 0.4,
                (!Double.isNaN(args[2])) ? (args[2] * 0.01) : 0.8,
                (!Double.isNaN(args[3])) ? (args[3] * 0.01) : 1);
    }

    @Override
    public int prepareProcess() {
        int i, imax = 1 << DELAY_BUFFER_BITS;
        for (i = 0; i < imax; i++) _delayBufferL[i] = _delayBufferR[i] = 0;
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        int i, m, imax = startIndex + length;
        double n, dry = 1 - _wet;
        for (i = startIndex; i < imax; ) {
            n = _delayBufferL[_pointerRead0] * _feedback0;
            n += _delayBufferL[_pointerRead1] * _feedback1;
            n += _delayBufferL[_pointerRead2] * _feedback2;
            _delayBufferL[_pointerWrite] = buffer[i] - n;
            buffer[i] *= dry;
            buffer[i] += n * _wet;
            i++;
            n = _delayBufferR[_pointerRead0] * _feedback0;
            n += _delayBufferR[_pointerRead1] * _feedback1;
            n += _delayBufferR[_pointerRead2] * _feedback2;
            _delayBufferR[_pointerWrite] = buffer[i] - n;
            buffer[i] *= dry;
            buffer[i] += n * _wet;
            i++;
            _pointerWrite = (_pointerWrite + 1) & DELAY_BUFFER_FILTER;
            _pointerRead0 = (_pointerRead0 + 1) & DELAY_BUFFER_FILTER;
            _pointerRead1 = (_pointerRead1 + 1) & DELAY_BUFFER_FILTER;
            _pointerRead2 = (_pointerRead2 + 1) & DELAY_BUFFER_FILTER;
        }
        return channels;
    }
}
