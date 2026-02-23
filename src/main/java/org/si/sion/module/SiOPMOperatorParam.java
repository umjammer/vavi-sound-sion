//
// SiOPM operator parameters
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;


/**
 * OPM Parameters. This instanceof a member of SiOPMChannelParam.
 *
 * @see org.si.sion.SiONVoice
 * @see org.si.sion.module.SiOPMChannelParam
 */
public class SiOPMOperatorParam {

    // variables
    //

    /** [extension] Pulse generator type [0,511] */
    public int pgType;
    /** [extension] Pitch table type [0,7] */
    public int ptType;

    /** Attack rate [0,63] */
    public int ar;
    /** Decay rate [0,63] */
    public int dr;
    /** Sustain rate [0,63] */
    public int sr;
    /** Release rate [0,63] */
    public int rr;
    /** Sustain level [0,15] */
    public int sl;
    /** [extension] Total level [0,127] */
    public int tl;

    /** Key scaling rate [0,3] */
    public int ksr;
    /** [extension] Key scaling level [0,3] */
    public int ksl;

    /** [extension] Fine multiple [0,...] */
    public int fmul;
    /** dt1 [0,7] */
    public int dt1;
    /** detune */
    public int detune;

    /** Amp modulation shift [0-3] */
    public int ams;
    /** [extension] Initiail phase [0,255]. The value of 255 sets no phase reset. */
    public int phase;
    /** [extension] Fixed pitch. 0 means pitch instanceof not fixed. */
    public int fixedPitch;

    /** mute */
    public boolean mute;
    /** SSG type envelop control */
    public int ssgec;
    /** [extension] Frequency modulation level [0,7]. 5 instanceof standard modulation. */
    public int modLevel;
    /** envelop reset on attack */
    public boolean erst;

    /** multiple [0,15] */
    public void setMul(int m) {
        fmul = (m != 0) ? (m << 7) : 64;
    }

    public int getMul() {
        return (fmul >> 7) & 15;
    }

    /** set pgType and ptType */
    public void setPGType(int type) {
        pgType = type & 511;
        ptType = SiOPMTable.getInstance().getWaveTable(pgType).defaultPTType;
    }

    /** constructor */
    public SiOPMOperatorParam() {
        initialize();
    }

    /** initialize all parameters. */
    public void initialize() {
        pgType = SiOPMTable.PG_SINE;
        ptType = SiOPMTable.PT_OPM;
        ar = 63;
        dr = 0;
        sr = 0;
        rr = 63;
        sl = 0;
        tl = 0;
        ksr = 1;
        ksl = 0;
        fmul = 128;
        dt1 = 0;
        detune = 0;
        ams = 0;
        phase = 0;
        fixedPitch = 0;
        mute = false;
        ssgec = 0;
        modLevel = 5;
        erst = false;
    }

    /** copy all parameters. */
    public void copyFrom(SiOPMOperatorParam org) {
        pgType = org.pgType;
        ptType = org.ptType;
        ar = org.ar;
        dr = org.dr;
        sr = org.sr;
        rr = org.rr;
        sl = org.sl;
        tl = org.tl;
        ksr = org.ksr;
        ksl = org.ksl;
        fmul = org.fmul;
        dt1 = org.dt1;
        detune = org.detune;
        ams = org.ams;
        phase = org.phase;
        fixedPitch = org.fixedPitch;
        mute = org.mute;
        ssgec = org.ssgec;
        modLevel = org.modLevel;
        erst = org.erst;
    }

    /** all parameters in 1line. */
    public String toString() {
        String str = "SiOPMOperatorParam : ";
        str += pgType + "(";
        str += ptType + ") : ";
        str += ar + "/";
        str += dr + "/";
        str += sr + "/";
        str += rr + "/";
        str += sl + "/";
        str += tl + " : ";
        str += ksr + "/";
        str += ksl + " : ";
        str += fmul + "/";
        str += dt1 + "/";
        str += detune + " : ";
        str += ams + "/";
        str += phase + "/";
        str += fixedPitch + " : ";
        str += ssgec + "/";
        str += mute + "/";
        str += String.valueOf(erst);
        return str;
    }
}
