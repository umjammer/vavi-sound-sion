// Programmable Sound Generator Synthesizer 
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.channels.SiOPMChannelFM;


/**
 * Programmable Sound Generator Synthesizer
 */
public class PSGSynth extends BasicSynth {

    // variables
    //

    /** PSG channel mode (0=mute, 1=PSG, 2=noise, 3=PSG+noise). */
    protected int _channelMode;
    /** PSG channel gain (*2db) (0=0db, 7=14db, 15=mute) */
    protected int _channelTL;
    /** SSG envelop controling rate */
    protected int _evelopRate;
    /** operator parameter for op0 */
    protected SiOPMOperatorParam _opp0;
    /** operator parameter for op1 */
    protected SiOPMOperatorParam _opp1;

    // properties
    //

    /** PSG channel number */
    public int getChannelNumber() {
        return _voice.channelNum;
    }

    /** PSG channel mode (0=mute, 1=PSG, 2=noise, 3=PSG+noise). */
    public int getChannelMode() {
        return _channelMode;
    }

    public void setChannelMode(int mode) {
        _opp0.mute = ((mode & 1) == 1);
        _opp1.mute = ((mode & 2) == 2);
        int i, imax = _tracks.size();
        SiOPMChannelFM ch;
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelFM) _tracks.get(i).channel);
            if (ch != null) {
                ch.operator[0].setMute(_opp0.mute);
                ch.operator[1].setMute(_opp1.mute);
            }
        }
    }

    /** noise frequency */
    public int getNoiseFreq() {
        return _opp1.fixedPitch >> 6;
    }

    public void setNoiseFreq(int nf) {
        _opp1.fixedPitch = (nf << 6) + 1;
        if (!_opp1.mute) {
            int i, imax = _tracks.size();
            SiOPMChannelFM ch;
            for (i = 0; i < imax; i++) {
                ch = ((SiOPMChannelFM) _tracks.get(i).channel);
                if (ch != null) ch.operator[1].setFixedPitchIndex(_opp1.fixedPitch);
            }
        }
    }

    /** PSG channel gain (*2db) (0=0db, 7=14db, 15=mute) */
    public int getChannelGain() {
        return (_channelTL > 37) ? 15 : (int) (_channelTL * 0.375 + 0.5);
    }

    public void setChannelGain(int g) {
        _channelTL = (g >= 15) ? 127 : (int) (g * 2.6666666666666667 + 0.5);
        _opp1.tl = _opp0.tl = _channelTL;
        if (_opp0.ssgec == 0) {
            int i, imax = _tracks.size();
            SiOPMChannelFM ch;
            for (i = 0; i < imax; i++) {
                ch = ((SiOPMChannelFM) _tracks.get(i).channel);
                if (ch != null) {
                    ch.operator[0].setTl(_opp0.tl);
                    ch.operator[1].setTl(_opp0.tl);
                }
            }
        }
    }

    /**
     * SSG Envelop control mode, only 8-17 are variable, 0-7 ((no) set) envelop.
     * The ssgec number of 16th and 17th are the extension of SiOPM.
     */
    public int getEnvelopControlMode() {
        return _opp0.ssgec;
    }

    public void setEnvelopControlMode(int ecm) {
        if (ecm < 8) { // no envelop
            _opp1.ssgec = _opp0.ssgec = 0;
            _opp1.dr = _opp0.dr = 0;
            _opp1.tl = _opp0.tl = _channelTL;
        } else { // envelop control
            _opp1.ssgec = _opp0.ssgec = ecm;
            _opp1.dr = _opp0.dr = _evelopRate;
            _opp1.tl = _opp0.tl = 0;
        }
        _voiceUpdateNumber++;
    }

    /** envelop frequency ... currently dishonesty. */
    public int getEnvelopFreq() {
        return _evelopRate << 2;
    }

    public void setEnvelopFreq(int ef) {
        _evelopRate = ef >> 2;
        if (_opp0.ssgec != 0) {
            _opp1.dr = _opp0.dr = _evelopRate;
            _voiceUpdateNumber++;
        }
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param channelNumber pseudo channel number.
     */
    public PSGSynth(int channelNumber) {
        super(0, channelNumber, 63, 63, 0);
        _opp0 = _voice.channelParam.operatorParam[0];
        _opp1 = _voice.channelParam.operatorParam[1];
        _voice.channelParam.opeCount = 2;
        _voice.channelParam.alg = 1;
        _opp0.pgType = SiOPMTable.PG_SQUARE;
        _opp0.ptType = SiOPMTable.PT_PSG;
        _opp1.pgType = SiOPMTable.PG_NOISE;
        _opp1.ptType = SiOPMTable.PT_PSG_NOISE;
        _opp1.fixedPitch = 1;
        _opp1.mute = true;
    }
}
