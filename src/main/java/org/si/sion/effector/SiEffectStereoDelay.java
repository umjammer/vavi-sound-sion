//
// SiOPM effect stereo long delay
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Stereo long delay effector. The delay time instanceof from 1[ms] to about 1.5[sec]. */
public class SiEffectStereoDelay extends SiEffectBase {

    // variables
    //

    private final int DELAY_BUFFER_BITS = 16;
    private final int DELAY_BUFFER_FILTER = (1 << DELAY_BUFFER_BITS) - 1;

    private final double[][] _delayBuffer;
    private int _pointerRead;
    private int _pointerWrite;
    private double _feedback;
    private double[] _readBufferL;
    private double[] _readBufferR;
    private double _wet;

    // constructor
    //

    /**
     * constructor
     *
     * @param delayTime delay time[ms]. maximum value instanceof about 1500.
     * @param feedback  feedback decay(-1-1). Negative value to invert phase.
     * @param isCross   stereo crossing delay.
     * @param wet       mixing level(0-1).
     */
    public SiEffectStereoDelay(double delayTime, double feedback, boolean isCross, double wet) {
        _delayBuffer = new double[2][];
        _delayBuffer[0] = new double[1 << DELAY_BUFFER_BITS];
        _delayBuffer[1] = new double[1 << DELAY_BUFFER_BITS];
        setParameters(delayTime, feedback, isCross, wet);
    }

    // operation
    //

    /**
     * set parameters
     *
     * @param delayTime delay time[ms]. maximum value instanceof about 1500.
     * @param feedback  feedback decay(-1-1). Negative value to invert phase.
     * @param isCross   stereo crossing delay.
     * @param wet       mixing level(0-1).
     */
    public void setParameters(double delayTime, double feedback, boolean isCross, double wet) {
        int offset = (int) (delayTime * 44.1),
                cross = (isCross) ? 1 : 0;
        if (offset > DELAY_BUFFER_FILTER) offset = DELAY_BUFFER_FILTER;
        _pointerWrite = (_pointerRead + offset) & DELAY_BUFFER_FILTER;
        _feedback = (feedback >= 1) ? 0.9990234375 : (feedback <= -1) ? -0.9990234375 : feedback;
        _readBufferL = _delayBuffer[cross];
        _readBufferR = _delayBuffer[1 - cross];
        _wet = wet;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(250, 0.25, false, 0.25);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] : 250,
                (!Double.isNaN(args[1])) ? (args[1] * 0.01) : 0.25,
                (args[2] == 1),
                (!Double.isNaN(args[3])) ? (args[3] * 0.01) : 1);
    }

    @Override
    public int prepareProcess() {
        int i, imax = 1 << DELAY_BUFFER_BITS;
        double[] buf0 = _delayBuffer[0];
        double[] buf1 = _delayBuffer[1];
        for (i = 0; i < imax; i++) buf0[i] = buf1[i] = 0;
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        int i, imax = startIndex + length;
        double[] writeBufferL = _delayBuffer[0];
        double[] writeBufferR = _delayBuffer[1];
        double n, dry = 1 - _wet;
        for (i = startIndex; i < imax; ) {
            n = _readBufferL[_pointerRead];
            writeBufferL[_pointerWrite] = buffer[i] - n * _feedback;
            buffer[i] *= dry;
            buffer[i] += n * _wet;
            i++;
            n = _readBufferR[_pointerRead];
            writeBufferR[_pointerWrite] = buffer[i] - n * _feedback;
            buffer[i] *= dry;
            buffer[i] += n * _wet;
            i++;
            _pointerWrite = (_pointerWrite + 1) & DELAY_BUFFER_FILTER;
            _pointerRead = (_pointerRead + 1) & DELAY_BUFFER_FILTER;
        }
        return channels;
    }
}
