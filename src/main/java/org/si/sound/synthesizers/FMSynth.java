// Frequency Modulation Synthesizer
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;


/**
 * Frequency Modulation Synthesizer
 */
public class FMSynth extends BasicSynth {

    // variables
    //

    /** FM Operators vector [m1,c1,m2,c2] */
    public FMSynthOperator[] operators;

    // properties
    //

    /** ALG; connection algorism [0-15]. */
    public int getAlg() {
        return _voice.channelParam.alg;
    }

    public void setAlg(int i) {
        if (_voice.channelParam.alg == i || i < 0 || i > 15) return;
        _voice.channelParam.alg = i;
        _voiceUpdateNumber++;
    }

    /** FB; feedback [0-7]. */
    public int getFb() {
        return _voice.channelParam.fb;
    }

    public void setFb(int i) {
        if (_voice.channelParam.fb == i || i < 0 || i > 7) return;
        _voice.channelParam.fb = i;
        _voiceUpdateNumber++;
    }

    /** FBC; feedback connection [0-3]. */
    public int getFbc() {
        return _voice.channelParam.fbc;
    }

    public void setFbc(int i) {
        if (_voice.channelParam.fbc == i || i < 0 || i > 3) return;
        _voice.channelParam.fbc = i;
        _voiceUpdateNumber++;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param channelNumber pseudo channel number.
     */
    public FMSynth(int channelNumber) {
        super(5, channelNumber, 63, 63, 0);
        operators = new FMSynthOperator[4];
        for (int i = 0; i < 4; i++) operators[i] = new FMSynthOperator(this, i);
    }

    // operation
    //
}
