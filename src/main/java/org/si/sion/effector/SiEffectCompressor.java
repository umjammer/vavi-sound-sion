//
// SiOPM effect Compressor
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;

import org.si.utils.SLLNumber;


/** Compressor. */
public class SiEffectCompressor extends SiEffectBase {

    // variables
    //

    private SLLNumber _windowRMSList = null;
    private int _windowSamples;
    private double _windowRMSTotal;
    private double _windwoRMSAveraging;
    private double _threshold2;  // threshold^2
    private double _attRate;     // attack rate  (per sample decay)
    private double _relRate;     // release rate (per sample decay)
    private double _maxGain;     // max gain
    private double _mixingLevel; // mixing level
    private double _gain;        // gain

    // constructor
    //

    /**
     * constructor
     *
     * @param thres   threshold(0-1).
     * @param wndTime window to calculate gain[ms].
     * @param attTime attack time [ms/6db].
     * @param relTime release time [ms/-6db].
     * @param maxGain max gain [db].
     */
    public SiEffectCompressor(double thres, double wndTime, double attTime, double relTime, double maxGain, double mixingLevel) {
        setParameters(thres, wndTime, attTime, relTime, maxGain, mixingLevel);
    }

    // operation
    //

    /**
     * set parameters.
     *
     * @param thres       threshold(0-1).
     * @param wndTime     window to calculate gain[ms].
     * @param attTime     attack time [ms/6db].
     * @param relTime     release time [ms/-6db].
     * @param maxGain     max gain [db].
     * @param mixingLevel output level.
     */
    public void setParameters(double thres, double wndTime, double attTime, double relTime, double maxGain, double mixingLevel) {
        _threshold2 = thres * thres;
        _windowSamples = (int) (wndTime * 44.1);
        _windwoRMSAveraging = 1 / _windowSamples;
        _attRate = (attTime == 0) ? 0.5 : (Math.pow(2, -1 / (attTime * 44.1)));
        _relRate = (relTime == 0) ? 2.0 : (Math.pow(2, 1 / (relTime * 44.1)));
        _maxGain = Math.pow(2, -maxGain / 6);
        _mixingLevel = mixingLevel;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(0.7, 50, 20, 20, -6, 0.5);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] * 0.01 : 0.7,
                (!Double.isNaN(args[1])) ? args[1] : 50,
                (!Double.isNaN(args[2])) ? args[2] : 20,
                (!Double.isNaN(args[3])) ? args[3] : 20,
                (!Double.isNaN(args[4])) ? -args[4] : -6,
                (!Double.isNaN(args[5])) ? args[5] * 0.01 : 0.5);
    }

    @Override
    public int prepareProcess() {
        if (_windowRMSList != null) SLLNumber.freeRing(_windowRMSList);
        _windowRMSList = SLLNumber.allocRing(_windowSamples, 0);
        _windowRMSTotal = 0;
        _gain = 2;
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;

        int i, imax = startIndex + length;
        double l, r, rms2;
        for (i = startIndex; i < imax; i++) {
            l = buffer[i];
            i++;
            r = buffer[i];
            --i;
            _windowRMSList = _windowRMSList.next;
            _windowRMSTotal -= _windowRMSList.n;
            _windowRMSList.n = l * l + r * r;
            _windowRMSTotal += _windowRMSList.n;
            rms2 = _windowRMSTotal * _windwoRMSAveraging;
            _gain *= (rms2 > _threshold2) ? _attRate : _relRate;
            if (_gain > _maxGain) _gain = _maxGain;

            l *= _gain;
            r *= _gain;
            l = (l > 1) ? 1 : (l < -1) ? -1 : l;
            r = (r > 1) ? 1 : (r < -1) ? -1 : r;
            buffer[i] = l * _mixingLevel;
            i++;
            buffer[i] = r * _mixingLevel;
        }
        return channels;
    }
}
