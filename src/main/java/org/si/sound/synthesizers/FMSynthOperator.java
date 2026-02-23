// Operator instance of FMSynth
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import org.si.sion.module.SiOPMOperatorParam;


/** Operator instance of FMSynth */
public class FMSynthOperator {

    // variables
    //
    private final FMSynth _owner;
    private final int _opeIndex;
    private final SiOPMOperatorParam _param;

    // properties
    //

    /** WS; wave shape [0-512]. */
    public int getWs() {
        return _param.pgType;
    }

    public void setWs(int i) {
        if (_param.pgType == i || i < 0 || i > 511) return;
        _param.setPGType(i);
        _owner._voiceUpdateNumber++;
    }

    /** AR; attack rate [0-63]. */
    public int getAr() {
        return _param.ar;
    }

    public void setAr(int i) {
        if (_param.ar == i || i < 0 || i > 63) return;
        _param.ar = i;
        _owner._voiceUpdateNumber++;
    }

    /** DR; decay rate [0-63]. */
    public int getDr() {
        return _param.dr;
    }

    public void setDr(int i) {
        if (_param.dr == i || i < 0 || i > 63) return;
        _param.dr = i;
        _owner._voiceUpdateNumber++;
    }

    /** SR; sustain rate [0-63]. */
    public int getSr() {
        return _param.sr;
    }

    public void setSr(int i) {
        if (_param.sr == i || i < 0 || i > 63) return;
        _param.sr = i;
        _owner._voiceUpdateNumber++;
    }

    /** RR; release rate [0-63]. */
    public int getRr() {
        return _param.rr;
    }

    public void setRr(int i) {
        if (_param.rr == i || i < 0 || i > 63) return;
        _param.rr = i;
        _owner._voiceUpdateNumber++;
    }

    /** SL; sustain level [0-15]. */
    public int getSl() {
        return _param.sl;
    }

    public void setSl(int i) {
        if (_param.sl == i || i < 0 || i > 15) return;
        _param.sl = i;
        _owner._voiceUpdateNumber++;
    }

    /** TL; total level [0-127]. */
    public int getTl() {
        return _param.tl;
    }

    public void setTl(int i) {
        if (_param.tl == i || i < 0 || i > 127) return;
        _param.tl = i;
        _owner._voiceUpdateNumber++;
    }

    /** KSR; sustain level [0-3]. */
    public int getKsr() {
        return _param.ksr;
    }

    public void setKsr(int i) {
        if (_param.ksr == i || i < 0 || i > 3) return;
        _param.ksr = i;
        _owner._voiceUpdateNumber++;
    }

    /** KSL; total level [0-3]. */
    public int getKsl() {
        return _param.ksl;
    }

    public void setKsl(int i) {
        if (_param.ksl == i || i < 0 || i > 3) return;
        _param.ksl = i;
        _owner._voiceUpdateNumber++;
    }

    /** MUL; multiple [0-15]. */
    public int getMul() {
        return _param.getMul();
    }

    public void setMul(int i) {
        if (_param.getMul() == i || i < 0 || i > 15) return;
        _param.setMul(i);
        _owner._voiceUpdateNumber++;
    }

    /** DT1; detune 1 (OPM/OPNA) [0-7]. */
    public int getDt1() {
        return _param.dt1;
    }

    public void setDt1(int i) {
        if (_param.dt1 == i || i < 0 || i > 7) return;
        _param.dt1 = i;
        _owner._voiceUpdateNumber++;
    }

    /** DT2; detune 2 (OPM) [0-3]. */
    public int getDt2() {
        if (_param.detune <= 100) return 0;   // 0
        else if (_param.detune <= 420) return 1;   // 384
        else if (_param.detune <= 550) return 2;   // 500
        return 3;                                  // 608
    }

    public void setDt2(int i) {
        int[] dt2table = {0, 384, 500, 608};
        if (_param.detune == i || i < 0 || i > 3) return;
        _param.detune = dt2table[i];
        _owner._voiceUpdateNumber++;
    }

    /** DET; detune (64 for 1halftone). */
    public int getDet() {
        return _param.detune;
    }

    public void setDet(int i) {
        if (_param.detune == i) return;
        _param.detune = i;
        _owner._voiceUpdateNumber++;
    }

    /** AMS; Amp modulation shift [0-3]. */
    public int getAms() {
        return _param.ams;
    }

    public void setAms(int i) {
        if (_param.ams == i || i < 0 || i > 3) return;
        _param.ams = i;
        _owner._voiceUpdateNumber++;
    }

    /** PH; Key on phase [0-255]. */
    public int getPh() {
        return _param.phase;
    }

    public void setPh(int i) {
        if (_param.phase == i || i < 0 || i > 255) return;
        _param.phase = i;
        _owner._voiceUpdateNumber++;
    }

    /** FN; fixed note [0-127]. */
    public int getFn() {
        return _param.fixedPitch >> 6;
    }

    public void setFn(int i) {
        int fp = i << 6;
        if (_param.fixedPitch == fp || i < 0 || i > 127) return;
        _param.fixedPitch = fp;
        _owner._voiceUpdateNumber++;
    }

    /** mute; mute [t/f]. */
    public boolean getMute() {
        return _param.mute;
    }

    public void setMute(boolean b) {
        if (_param.mute == b) return;
        _param.mute = b;
        _owner._voiceUpdateNumber++;
    }

    /** SSGEC; SSG type envelop control [0-17]. */
    public int getSsgec() {
        return _param.ssgec;
    }

    public void setSsgec(int i) {
        if (_param.ssgec == i || i < 0 || i > 17) return;
        _param.ssgec = i;
        _owner._voiceUpdateNumber++;
    }

    /** ERST; envelop reset on attack [t/f]. */
    public boolean getErst() {
        return _param.erst;
    }

    public void setErst(boolean b) {
        if (_param.erst == b) return;
        _param.erst = b;
        _owner._voiceUpdateNumber++;
    }

    // constructor
    //

    /** Constructor, But you cannot create new instance of this class. */
    public FMSynthOperator(FMSynth owner, int opeIndex) {
        _owner = owner;
        _opeIndex = opeIndex;
        _param = owner.getVoice().channelParam.operatorParam[opeIndex];
    }

    // operation
    //

    /**
     * Set all 15 FM parameters. The value of Integer.MIN_VALUE does not change.
     *
     * @param ar      Attack rate [0-63].
     * @param dr      Decay rate [0-63].
     * @param sr      Sustain rate [0-63].
     * @param rr      Release rate [0-63].
     * @param sl      Sustain level [0-15].
     * @param tl      Total level [0-127].
     * @param ksr     Key scaling [0-3].
     * @param ksl     key scale level [0-3].
     * @param mul     Multiple [0-15].
     * @param dt1     Detune 1 [0-7].
     * @param detune  Detune.
     * @param ams     Amplitude modulation shift [0-3].
     * @param phase   Phase [0-255].
     * @param fixNote Fixed note number [0-127].
     */
    public void setAllParameters(int ws, int ar, int dr, int sr, int rr, int sl, int tl, int ksr, int ksl, int mul, int dt1, int detune, int ams, int phase, int fixNote) {
        if (ws != Integer.MIN_VALUE) _param.setPGType(ws & 511);
        if (ar != Integer.MIN_VALUE) _param.ar = ar & 63;
        if (dr != Integer.MIN_VALUE) _param.dr = dr & 63;
        if (sr != Integer.MIN_VALUE) _param.sr = sr & 63;
        if (rr != Integer.MIN_VALUE) _param.rr = rr & 63;
        if (sl != Integer.MIN_VALUE) _param.sl = sl & 15;
        if (tl != Integer.MIN_VALUE) _param.tl = tl & 127;
        if (ksr != Integer.MIN_VALUE) _param.ksr = ksr & 3;
        if (ksl != Integer.MIN_VALUE) _param.ksl = ksl & 3;
        if (mul != Integer.MIN_VALUE) _param.setMul(mul & 15);
        if (dt1 != Integer.MIN_VALUE) _param.dt1 = dt1 & 7;
        if (detune != Integer.MIN_VALUE) _param.detune = detune;
        if (ams != Integer.MIN_VALUE) _param.ams = ams & 3;
        if (phase != Integer.MIN_VALUE) _param.phase = phase & 255;
        if (fixNote != Integer.MIN_VALUE) _param.fixedPitch = (fixNote & 127) << 6;
        _owner._voiceUpdateNumber++;
    }
}
