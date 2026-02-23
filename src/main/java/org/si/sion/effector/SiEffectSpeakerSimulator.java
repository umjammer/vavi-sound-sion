//
// Piezoelectric speaker simulator
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Piezoelectric speaker simulator. */
public class SiEffectSpeakerSimulator extends SiEffectBase {

    // variables
    //

    private double _springCoef = 0.96;
    private double _diaphragmPosL, _diaphragmPosR;
    private double _prevL, _prevR;

    // constructor
    //

    /**
     * Constructor.
     *
     * @param hardness hardness of diaphragm (0-1). 0 sets no effect. 1 sets hardest.
     */
    public SiEffectSpeakerSimulator(double hardness) {
        setParameters(hardness);
    }

    /**
     * set parameter
     *
     * @param hardness hardness of diaphragm (0-1). 0 sets no effect. 1 sets hardest.
     */
    public void setParameters(double hardness) {
        _springCoef = 1 - hardness * hardness;
        if (_springCoef < 0.1) _springCoef = 0.1;
    }

    // callback functions
    //

    @Override
    public void initialize() {
        setParameters(0.2);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] * 0.01 : 0.2);
    }

    @Override
    public int prepareProcess() {
        _prevL = _prevR = _diaphragmPosL = _diaphragmPosR = 0;
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        int i, imax = startIndex + length;
        double d;
        for (i = startIndex; i < imax; ) {
            d = buffer[i] - _prevL;
            _diaphragmPosL *= _springCoef;
            _diaphragmPosL += d;
            _prevL = buffer[i];
            buffer[i] = _diaphragmPosL;
            i++;

            d = buffer[i] - _prevR;
            _diaphragmPosR *= _springCoef;
            _diaphragmPosR += d;
            _prevR = buffer[i];
            buffer[i] = _diaphragmPosR;
            i++;
        }
        return channels;
    }
}
