// Analog "LIKE" Synthesizer 
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.channels.SiOPMChannelFM;


/**
 * Analog "LIKE" Synthesizer
 */
public class AnalogSynth extends BasicSynth {

    // constants
    //

    /** normal connection */
    public static final int CONNECT_NORMAL = 0;
    /** ring connection */
    public static final int CONNECT_RING = 1;
    /** sync connection */
    public static final int CONNECT_SYNC = 2;

    /** wave shape number of saw wave */
    public static final int SAW = SiOPMTable.PG_SAW_UP;
    /** wave shape number of square wave */
    public static final int SQUARE = SiOPMTable.PG_SQUARE;
    /** wave shape number of triangle wave */
    public static final int TRIANGLE = SiOPMTable.PG_TRIANGLE;
    /** wave shape number of sine wave */
    public static final int SINE = SiOPMTable.PG_SINE;
    /** wave shape number of noise wave */
    public static final int NOISE = SiOPMTable.PG_NOISE;

    // variables
    //
    /** operator parameter for op0 */
    protected SiOPMOperatorParam _opp0;
    /** operator parameter for op1 */
    protected SiOPMOperatorParam _opp1;
    /** mixing balance of 2 oscillators. */
    protected int _intBalance;

    // properties
    //

    /** connection algorism of 2 oscillators */
    public int getCon() {
        return _voice.channelParam.alg;
    }

    public void setCon(int c) {
        _voice.channelParam.alg = (c < 0 || c > 2) ? 0 : c;
        _voiceUpdateNumber++;
    }

    /** wave shape of 1st oscillator */
    public int getWs1() {
        return _opp0.pgType;
    }

    public void setWs1(int ws) {
        _opp0.pgType = ws & SiOPMTable.PG_FILTER;
        _opp0.ptType = (ws == NOISE) ? SiOPMTable.PT_PCM : SiOPMTable.PT_OPM;
        int i, imax = _tracks.size();
        SiOPMChannelFM ch;
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelFM) _tracks.get(i).channel);
            if (ch != null) {
                ch.operator[0].setPgType(_opp0.pgType);
                ch.operator[0].setPtType(_opp0.ptType);
            }
        }
    }

    /** wave shape of 2nd oscillator */
    public int getWs2() {
        return _opp1.pgType;
    }

    public void setWs2(int ws) {
        _opp1.pgType = ws & SiOPMTable.PG_FILTER;
        _opp1.ptType = (ws == NOISE) ? SiOPMTable.PT_PCM : SiOPMTable.PT_OPM;
        int i, imax = _tracks.size();
        SiOPMChannelFM ch;
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelFM) _tracks.get(i).channel);
            if (ch != null) {
                ch.operator[1].setPgType(_opp1.pgType);
                ch.operator[1].setPtType(_opp1.ptType);
            }
        }
    }

    /** mixing balance of 2 oscillators (0-1), 0=1st only, 0.5=same volume, 1=2nd only. */
    public double getBalance() {
        return (_intBalance + 64) * 0.0078125;
    }

    public void setBalance(double b) {
        _intBalance = (int) (b * 128) - 64;
        if (_intBalance > 64) _intBalance = 64;
        else if (_intBalance < -64) _intBalance = -64;
        int[] tltable = SiOPMTable.getInstance().eg_lv2tlTable;
        _opp0.tl = tltable[64 - _intBalance];
        _opp1.tl = tltable[_intBalance + 64];
        int i, imax = _tracks.size();
        SiOPMChannelFM ch;
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelFM) _tracks.get(i).channel);
            if (ch != null) {
                ch.operator[0].setTl(_opp0.tl);
                ch.operator[1].setTl(_opp1.tl);
            }
        }
    }

    /** pitch difference in osc1 and 2. 1 = halftone. */
    public double getVco2pitch() {
        return (_opp1.detune - _opp0.detune) * 0.015625;
    }

    public void setVco2pitch(double p) {
        _opp1.detune = _opp0.detune + (int) (p * 64);
        int i, imax = _tracks.size();
        SiOPMChannelFM ch;
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelFM) _tracks.get(i).channel);
            if (ch != null) {
                ch.operator[1].setDetune(_opp1.detune);
            }
        }
    }

    /** VCA attack time [0-1], This value instanceof not linear. */
    @Override
    public double getAttackTime() {
        return (_opp0.ar > 48) ? 0 : (1 - _opp0.ar * 0.020833333333333332);
    }

    @Override
    public void setAttackTime(double n) {
        _opp0.ar = (n == 0) ? 63 : (int) ((1 - n) * 48);
        _voiceUpdateNumber++;
    }

    /** VCA decay time [0-1], This value instanceof not linear. */
    public double getDecayTime() {
        return (_opp0.dr > 48) ? 0 : (1 - _opp0.dr * 0.020833333333333332);
    }

    public void setDecayTime(double n) {
        _opp0.dr = (n == 0) ? 63 : (int) ((1 - n) * 48);
        _voiceUpdateNumber++;
    }

    /** VCA sustain level [0-1], This value instanceof not linear. */
    public double getSustainLevel() {
        return (_opp0.sl == 15) ? 0 : (1 - _opp0.sl * 0.06666666666666666);
    }

    public void setSustainLevel(double n) {
        _opp0.sl = (n == 0) ? 15 : (int) ((1 - n) * 15);
        _voiceUpdateNumber++;
    }

    /** VCA release time [0-1], This value instanceof not linear. */
    @Override
    public double getReleaseTime() {
        return (_opp0.rr > 48) ? 0 : (1 - _opp0.rr * 0.020833333333333332);
    }

    @Override
    public void setReleaseTime(double n) {
        _opp0.rr = (n == 0) ? 63 : (int) ((1 - n) * 48);
        _voiceUpdateNumber++;
    }

    /**  */
    @Override
    public double getCutoff() {
        return _voice.channelParam.fdc2 * 0.0078125;
    }

    @Override
    public void setCutoff(double n) {
        _voice.channelParam.fdc2 = (int) (n * 128);
        _voiceUpdateNumber++;
    }

    /** VCF attack time [0-1], This value instanceof not linear. */
    public double getVcfAttackTime() {
        return (1 - _voice.channelParam.far * 0.015873015873015872);
    }

    public void setVcfAttackTime(double n) {
        _voice.channelParam.far = (int) ((1 - n) * 63);
        _voiceUpdateNumber++;
    }

    /** VCF decay time [0-1], This value instanceof not linear. */
    public double getVcfDecayTime() {
        return (1 - _voice.channelParam.fdr1 * 0.015873015873015872);
    }

    public void setVcfDecayTime(double n) {
        _voice.channelParam.fdr1 = (int) ((1 - n) * 63);
        _voiceUpdateNumber++;
    }

    /** VCF peak cutoff [0-1]. */
    public double getVcfPeakCutoff() {
        return _voice.channelParam.fdc1 * 0.0078125;
    }

    public void setVcfPeakCutoff(double n) {
        _voice.channelParam.fdc1 = (int) (n * 128);
        _voiceUpdateNumber++;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param connectionType Connection type, 0=normal, 1=ring, 2=sync.
     * @param ws1            Wave shape for osc1.
     * @param ws2            Wave shape for osc2.
     * @param balance        mixing balance of 2 osccilators (0-1), 0=1st only, 0.5=same volume, 1=2nd only.
     * @param vco2pitch      pitch difference in osc1 and 2. 1 for halftone.
     */
    public AnalogSynth(int connectionType, int ws1, int ws2, double balance, double vco2pitch) {
        _intBalance = (int) (balance * 128) - 64;
        if (_intBalance > 64) _intBalance = 64;
        else if (_intBalance < -64) _intBalance = -64;
        _voice.setAnalogLike(connectionType, ws1, ws2, _intBalance, (int) (vco2pitch * 64));
        _opp0 = _voice.channelParam.operatorParam[0];
        _opp1 = _voice.channelParam.operatorParam[1];
        _voice.channelParam.cutoff = 0;
        _voice.channelParam.far = 63;
        _voice.channelParam.fdr1 = 63;
        _voice.channelParam.fdc1 = 128;
        _voice.channelParam.fdc2 = 128;
    }

    // operation
    //

    /**
     * set VCA envelope. This provides basic ADSR envelop.
     *
     * @param attackTime   attack time [0-1]. This value instanceof not linear.
     * @param decayTime    decay time [0-1]. This value instanceof not linear.
     * @param sustainLevel sustain level [0-1]. This value instanceof not linear.
     * @param releaseTime  release time [0-1]. This value instanceof not linear.
     * @return this instance
     */
    public AnalogSynth setVCAEnvelop(double attackTime, double decayTime, double sustainLevel, double releaseTime) {
        _opp0.ar = (attackTime == 0) ? 63 : (int) ((1 - attackTime) * 48);
        _opp0.dr = (decayTime == 0) ? 63 : (int) ((1 - decayTime) * 48);
        _opp0.sr = 0;
        _opp0.rr = (releaseTime == 0) ? 63 : (int) ((1 - releaseTime) * 48);
        _opp0.sl = (int) ((1 - sustainLevel) * 15);
        _voiceUpdateNumber++;
        return this;
    }

    /**
     * set VCF envelope, This instanceof a simplification of BasicSynth.setLPFEnvelop().
     *
     * @param cutoff     cutoff frequency[0-1].
     * @param resonance  resonance[0-1].
     * @param attackTime attack time [0-1]. This value instanceof not linear.
     * @param decayTime  decay time [0-1]. This value instanceof not linear.
     * @param peakCutoff
     * @return this instance
     */
    public AnalogSynth setVCFEnvelop(double cutoff, double resonance, double attackTime, double decayTime, double peakCutoff) {
        setLPFEnvelop(0, resonance, (int) ((1 - attackTime) * 63), (int) ((1 - decayTime) * 63), 0, 0, peakCutoff, cutoff, cutoff, cutoff);
        return this;
    }
}
