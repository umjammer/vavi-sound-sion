// Physical Modeling Guitar Synthesizer 
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import org.si.sion.module.channels.SiOPMChannelKS;


/**
 * Physical Modeling Guitar Synthesizer
 */
public class PMGuitarSynth extends BasicSynth {

    // variables
    //

    /** plunk velocity [0-1]. */
    protected double _plunkVelocity;

    /** tl offset by attack rate. */
    protected double _tlOffsetByAR;

    // properties
    //

    /** string tension [0-1]. */
    public double getTension() {
        return _voice.pmsTension * 0.015873015873015872;
    }

    public void setTensoin(double t) {
        _voice.pmsTension = (int) (t * 63);
        _voiceUpdateNumber++;
        int i, imax = _tracks.size();
        SiOPMChannelKS ch;
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelKS) _tracks.get(i).channel);
            if (ch != null) ch.setAllReleaseRate(_voice.pmsTension);
        }
    }

    /** plunk velocity [0-1]. */
    public double getPlunkVelocity() {
        return _plunkVelocity;
    }

    public void setPlunkVelocity(double v) {
        _plunkVelocity = (v < 0) ? 0 : (v > 1) ? 1 : v;
        _voice.channelParam.operatorParam[0].tl = (_plunkVelocity == 0) ? 127 : (int) (_plunkVelocity * 64 - _tlOffsetByAR);
        _voiceUpdateNumber++;
    }


    /** wave shape of plunk noise. @default 20 (SiOPMTable.PG_NOISE_PINK) */
    public int getSeedWaveShape() {
        return _voice.channelParam.operatorParam[0].pgType;
    }

    public void setSeedWaveShape(int ws) {
        _voice.channelParam.operatorParam[0].setPGType(ws);
        _voiceUpdateNumber++;
    }

    /** pitch of plunk noise. @default 68 */
    public int getSeedPitch() {
        return _voice.channelParam.operatorParam[0].fixedPitch;
    }

    public void setSeedPitch(int p) {
        _voice.channelParam.operatorParam[0].fixedPitch = p;
        _voiceUpdateNumber++;
    }

    /** attack time of plunk noise (0-1). */
    @Override
    public double getAttackTime() {
        int iar = _voice.channelParam.operatorParam[0].ar;
        return (iar > 48) ? 0 : (1 - (iar - 16) * 0.03125);
    }

    @Override
    public void setAttackTime(double n) {
        int iar = (int) (((1 - n) * 32) + 16);
        _tlOffsetByAR = n * 16;
        _voice.channelParam.operatorParam[0].ar = iar;
        _voice.channelParam.operatorParam[0].tl = (_plunkVelocity == 0) ? 127 : (int) (_plunkVelocity * 64 - _tlOffsetByAR);
        _voiceUpdateNumber++;
    }

    /** release time of guitar synthesizer instanceof equal to (1-tension). */
    @Override
    public double getReleaseTime() {
        return 1 - _voice.pmsTension * 0.015625;
    }

    @Override
    public void setReleaseTime(double n) {
        _voice.pmsTension = (int) (64 - n * 64);
        if (_voice.pmsTension < 0) _voice.pmsTension = 0;
        else if (_voice.pmsTension > 63) _voice.pmsTension = 63;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param tension sustain rate of the tone
     */
    public PMGuitarSynth(double tension) {
        super(5, 0, 63, 63, 0);
        _voice.setPMSGuitar(48, 48, 0, 68, 20, (int) (tension * 63));
        setAttackTime(0);
        setPlunkVelocity(1);
    }

    // operation
    //

    /**
     * Set all parameters of phisical modeling synth guitar voice.
     *
     * @param ar         attack rate of plunk energy
     * @param dr         decay rate of plunk energy
     * @param tl         total level of plunk energy
     * @param fixedPitch plunk noise pitch
     * @param ws         wave shape of plunk
     * @param tension    sustain rate of the tone
     */
    public PMGuitarSynth setPMSGuitar(int ar, int dr, int tl, int fixedPitch, int ws, int tension) {
        _voice.setPMSGuitar(ar, dr, tl, fixedPitch, ws, tension);
        _voiceUpdateNumber++;
        return this;
    }
}
