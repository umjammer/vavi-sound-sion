//
// Down sampler
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Down sampler. */
public class SiEffectDownSampler extends SiEffectBase {

    // variables
    //

    private int _freqShift = 0;
    private double _bitConv0 = 1;
    private double _bitConv1 = 1;
    private int _channelCount = 2;

    // constructor
    //

    /**
     * Constructor.
     *
     * @param freqShift    frequency shift 0=44.1kHz, 1=22.05kHz, 2=11.025kHz.
     * @param bitRate      bit rate of the sample
     * @param channelCount channel count 1=monaural, 2=stereo
     */
    public SiEffectDownSampler(int freqShift, int bitRate, int channelCount) {
        setParameters(freqShift, bitRate, channelCount);
    }

    /**
     * set parameter
     *
     * @param freqShift    frequency shift 0=44.1kHz, 1=22.05kHz, 2=11.025kHz.
     * @param bitRate      bit rate of the sample
     * @param channelCount channel count 1=monaural, 2=stereo
     */
    public void setParameters(int freqShift, int bitRate, int channelCount) {
        _freqShift = freqShift;
        _bitConv0 = 1 << bitRate;
        _bitConv1 = 1 / _bitConv0;
        _channelCount = channelCount;
    }

    // callback functions
    //

    @Override
    public void initialize() {
        setParameters(0, 16, 2);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? (int) args[0] : 0,
                (!Double.isNaN(args[1])) ? (int) args[1] : 16,
                (!Double.isNaN(args[2])) ? (int) args[2] : 2);
    }

    @Override
    public int prepareProcess() {
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        int i, j, jmax;
        double bc0, l, r;
        int imax = startIndex + length;
        if (_channelCount == 1) {
            switch (_freqShift) {
                case 0:
                    bc0 = 0.5 * _bitConv0;
                    for (i = startIndex; i < imax; ) {
                        l = buffer[i];
                        i++;
                        l += buffer[i];
                        i--;
                        l = ((int) (l * bc0)) * _bitConv1;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                    }
                    break;
                case 1:
                    bc0 = 0.25 * _bitConv0;
                    for (i = startIndex; i < imax; ) {
                        l = buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i -= 3;
                        l = ((int) (l * bc0)) * _bitConv1;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                    }
                    break;
                case 2:
                    bc0 = 0.125 * _bitConv0;
                    for (i = startIndex; i < imax; ) {
                        l = buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        l += buffer[i];
                        i -= 7;
                        l = ((int) (l * bc0)) * _bitConv1;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = l;
                        i++;
                    }
                    break;
                default:
                    jmax = 2 << _freqShift;
                    bc0 = (1 / jmax) * _bitConv0;
                    for (i = startIndex; i < imax; ) {
                        for (j = 0, l = 0; j < jmax; j++, i++) {
                            l += buffer[i];
                        }
                        i -= jmax;
                        l = ((int) (l * bc0)) * _bitConv1;
                        for (j = 0; j < jmax; j++, i++) {
                            buffer[i] = l;
                        }
                    }
                    break;
            }
        } else {
            switch (_freqShift) {
                case 0:
                    for (i = startIndex; i < imax; i++) {
                        buffer[i] = ((int) (buffer[i] * _bitConv0)) * _bitConv1;
                    }
                    break;
                case 1:
                    bc0 = 0.5 * _bitConv0;
                    for (i = startIndex; i < imax; ) {
                        l = buffer[i];
                        i++;
                        r = buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        r += buffer[i];
                        i -= 3;
                        l = ((int) (l * bc0)) * _bitConv1;
                        r = ((int) (r * bc0)) * _bitConv1;
                        buffer[i] = l;
                        i++;
                        buffer[i] = r;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = r;
                        i++;
                    }
                    break;
                case 2:
                    bc0 = 0.25 * _bitConv0;
                    for (i = startIndex; i < imax; ) {
                        l = buffer[i];
                        i++;
                        r = buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        r += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        r += buffer[i];
                        i++;
                        l += buffer[i];
                        i++;
                        r += buffer[i];
                        i -= 7;
                        l = ((int) (l * bc0)) * _bitConv1;
                        r = ((int) (r * bc0)) * _bitConv1;
                        buffer[i] = l;
                        i++;
                        buffer[i] = r;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = r;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = r;
                        i++;
                        buffer[i] = l;
                        i++;
                        buffer[i] = r;
                        i++;
                    }
                    break;
                default:
                    jmax = 1 << _freqShift;
                    bc0 = (1 / jmax) * _bitConv0;
                    for (i = startIndex; i < imax; ) {
                        for (j = 0, l = 0, r = 0; j < jmax; j++, i++) {
                            l += buffer[i];
                            r += buffer[i];
                        }
                        i -= jmax;
                        l = ((int) (l * bc0)) * _bitConv1;
                        r = ((int) (r * bc0)) * _bitConv1;
                        for (j = 0; j < jmax; j++) {
                            buffer[i] = l;
                            i++;
                            buffer[i] = r;
                            i++;
                        }
                    }
                    break;
            }
        }
        return _channelCount;
    }
}
