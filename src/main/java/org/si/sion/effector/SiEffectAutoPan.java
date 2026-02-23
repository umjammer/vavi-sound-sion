//
// SiOPM effect stereo auto pan
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;

import org.si.utils.SLLNumber;


/** Stereo auto pan. */
public class SiEffectAutoPan extends SiEffectBase {

    // variables
    //

    private boolean _stereo;

    private int _lfoStep;
    private int _lfoResidueStep;
    private SLLNumber _pL;
    SLLNumber _pR;

    // constructor
    //

    /**
     * constructor
     *
     * @param frequency rotation frequency(Hz).
     * @param width     stereo width(0-1). 0 ((auto) sets) pan with keeping stereo.
     */
    public SiEffectAutoPan(double frequency, double width) {
        _pL = SLLNumber.allocRing(256, 0);
        _lfoResidueStep = 0;
        setParameters(frequency, width);
    }

    // operations
    //

    /**
     * set parameter
     *
     * @param frequency rotation frequency(Hz).
     * @param width     stereo width(0-1). 0 ((auto) sets) pan with keeping stereo.
     */
    public void setParameters(double frequency, double width) {
        int i;
        frequency *= 0.5;
        _lfoStep = (int) (172.265625 / frequency);   //44100/256
        if (_lfoStep <= 4) _lfoStep = 4;
        _stereo = false;
        if (width == 0) {
            width = 1;
            _stereo = true;
        }

        // volume table
        width *= 0.01227184630308513; // pi/256
        for (i = -128; i < 128; i++) {
            _pL.n = Math.sin(1.5707963267948965 + i * width);
            _pL = _pL.next;
        }
        // _pR phase shift
        _pR = _pL;
        for (i = 0; i < 128; i++) _pR = _pR.next;
    }

    // override functions
    //

    @Override
    public void initialize() {
        _lfoResidueStep = 0;
        setParameters(1, 1);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] : 1,
                (!Double.isNaN(args[1])) ? args[1] * 0.01 : 1);
    }

    @Override
    public int prepareProcess() {
        return (_stereo) ? 2 : 1;
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T var1, U var2, V var3);
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;

        int i, imax, istep, c, s, l, r;
        TriConsumer<double[], Integer, Integer> proc = (_stereo) ? this::processLFOstereo : this::processLFOmono;
        istep = _lfoResidueStep;
        imax = startIndex + length;
        for (i = startIndex; i < imax - istep; ) {
            proc.accept(buffer, i, istep);
            i += istep;
            istep = _lfoStep << 1;
        }
        proc.accept(buffer, i, imax - i);
        _lfoResidueStep = istep - (imax - i);
        return 2;
    }

    /** */
    public void processLFOmono(double[] buffer, int startIndex, int length) {
        double c = _pL.n, s = _pR.n;
        int i;
        double l, imax = startIndex + length;
        for (i = startIndex; i < imax; ) {
            l = buffer[i];
            buffer[i] = l * c;
            i++;
            buffer[i] = l * s;
            i++;
        }
        _pL = _pL.next;
        _pR = _pR.next;
    }

    public void processLFOstereo(double[] buffer, int startIndex, int length) {
        double c = _pL.n, s = _pR.n;
        int i;
        double l, r, imax = startIndex + length;
        for (i = startIndex; i < imax; i += 2) {
            l = buffer[i];
            r = buffer[i + 1];
            buffer[i] = l * c - r * s;
            buffer[i + 1] = l * s + r * c;
        }
        _pL = _pL.next;
        _pR = _pR.next;
    }
}
