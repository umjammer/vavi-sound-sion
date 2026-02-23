//
// SiOPM filter controlable
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;

import org.si.sion.module.SiOPMTable;
import org.si.sion.sequencer.SiMMLTable;
import org.si.utils.SLLint;


/** controllable filter base class. */
public class SiCtrlFilterBase extends SiEffectBase {

    // variables
    //

    /** */
    static protected SLLint _incEnvelopTable = null;
    /** */
    static protected SLLint _decEnvelopTable = null;
    /** */
    protected double _p0r;
    /** */
    protected double _p1r;
    /** */
    protected double _p0l;
    /** */
    protected double _p1l;
    /** */
    protected int _cutIndex;
    /** */
    protected double _res;
    /** */
    protected SiOPMTable _table;

    private SLLint _ptrCut;
    private SLLint _ptrRes;
    private int _lfoStep;
    private int _lfoResidueStep;

    // properties
    //

    /** cutoff */
    public double getCutoff() {
        return _cutIndex * 0.0078125;
    }

    public void setCutoff(double n) {
        _cutIndex = (int) (getCutoff() * 128);
        if (_cutIndex > 128) _cutIndex = 128;
        else if (_cutIndex < 0) _cutIndex = 0;
    }

    /** resonance */
    public double getResonance() {
        return _res;
    }

    public void setResonance(double n) {
        _res = getResonance();
        if (_res > 1) _res = 1;
        else if (_res < 0) _res = 0;
    }

    // constructor
    //

    /** constructor */
    public SiCtrlFilterBase() {
        if (_incEnvelopTable == null) {
            _incEnvelopTable = SLLint.allocList(129, 0);
            _decEnvelopTable = SLLint.allocList(129, 0);
            SLLint ptrit = _incEnvelopTable,
                    ptrdt = _decEnvelopTable;
            for (int i = 0; i < 129; i++) {
                ptrit.i = i;
                ptrdt.i = 128 - i;
                ptrit = ptrit.next;
                ptrdt = ptrdt.next;
            }
        }
    }

    // operation
    //

    /**
     * set parameters
     *
     * @param cut table index for cutoff(0-255). 255 to set no tables.
     * @param res table index for resonance(0-255). 255 to set no tables.
     * @param fps Envelop speed (0.001-1000)[Frame per second].
     */
    public void setParameters(int cut, int res, double fps) {
        _table = SiOPMTable.getInstance();
        SiMMLTable simml = SiMMLTable.getInstance();
        _ptrCut = (cut >= 0 && cut < 255 && simml.getEnvelopTable(cut) != null) ? simml.getEnvelopTable(cut).head : null;
        _ptrRes = (res >= 0 && res < 255 && simml.getEnvelopTable(res) != null) ? simml.getEnvelopTable(res).head : null;
        _cutIndex = (_ptrCut != null) ? _ptrCut.i : 128;
        _res = (_ptrRes != null) ? (_ptrRes.i * 0.007751937984496124) : 0;    // 0.007751937984496124=1/129
        _lfoStep = (int) (44100 / fps);
        if (_lfoStep <= 44) _lfoStep = 44;
        _lfoResidueStep = _lfoStep << 1;
    }

    /**
     * control cutoff and resonance manualy.
     *
     * @param cutoff    cutoff(0-1).
     * @param resonance resonance(0-1).
     */
    public void control(double cutoff, double resonance) {
        _lfoStep = 2048;
        _lfoResidueStep = 4096;

        if (cutoff > 1) cutoff = 1;
        else if (cutoff < 0) cutoff = 0;
        _cutIndex = (int) (cutoff * 128);

        if (resonance > 1) resonance = 1;
        else if (resonance < 0) resonance = 0;
        _res = resonance;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(255, 255, 20);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? (int) (args[0]) : 255,
                (!Double.isNaN(args[1])) ? (int) (args[1]) : 255,
                (!Double.isNaN(args[2])) ? (int) (args[2]) : 20);
    }

    @Override
    public int prepareProcess() {
        _lfoResidueStep = 0;
        _p0r = _p1r = _p0l = _p1l = 0;
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;

        int i, imax, istep, c, s, l, r;
        istep = _lfoResidueStep;
        imax = startIndex + length;
        for (i = startIndex; i < imax - istep; ) {
            processLFO(buffer, i, istep);
            if (_ptrCut != null) {
                _ptrCut = _ptrCut.next;
                _cutIndex = (_ptrCut != null) ? _ptrCut.i : 128;
            }
            if (_ptrRes != null) {
                _ptrRes = _ptrRes.next;
                _res = (_ptrRes != null) ? (_ptrRes.i * 0.007751937984496124) : 0;
            }
            i += istep;
            istep = _lfoStep << 1;
        }
        processLFO(buffer, i, imax - i);
        _lfoResidueStep = istep - (imax - i);
        return channels;
    }

    /** */
    protected void processLFO(double[] buffer, int startIndex, int length) {
    }
}
