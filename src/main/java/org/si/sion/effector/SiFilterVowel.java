//
// SiOPM Vowel filter
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Vowel filter, combination of 6 peaking filters */
public class SiFilterVowel extends SiEffectBase {

    // variables
    //

    public static final int FORMANT_COUNT = 6;
    public SiFilterVowelFormant[] formant;
    public double outputLevel = 1;

    // tap matrix
    private double _t0i0, _t0i1, _t0o0, _t0o1;
    private double _t1i0, _t1i1, _t1o0, _t1o1;
    private double _t2i0, _t2i1, _t2o0, _t2o1;
    private double _t3i0, _t3i1, _t3o0, _t3o1;
    private double _t4i0, _t4i1, _t4o0, _t4o1;
    private double _t5i0, _t5i1, _t5o0, _t5o1;

    private FormantEvent _eventQueue;

    // constructor
    //

    /**
     * constructor
     */
    public SiFilterVowel() {
        SiFilterVowelFormant.initialize();
        formant = new SiFilterVowelFormant[FORMANT_COUNT];
        for (int i = 0; i < FORMANT_COUNT; i++) formant[i] = new SiFilterVowelFormant();
        _eventQueue = null;
        setFilterBand(
                  800, 36, 3,
                 1300, 24, 3,
                 2200, 12, 3,
                 3500,  9, 3,
                 4500,  6, 3,
                 5500,  3, 3);
    }

    // operations
    //

    /** set 1st and 2nd formant with delay */
    public void setVowellFormant(double outputLevel, double formFreq1, int gain1, double formFreq2, int gain2, int delay) {
        int ifreq1 = SiFilterVowelFormant.calcFreqIndex(formFreq1);
        int ifreq2 = SiFilterVowelFormant.calcFreqIndex(formFreq2);
        FormantEvent e = new FormantEvent(delay, outputLevel, ifreq1, gain1, ifreq2, gain2);
        _eventQueue = e.insertTo(_eventQueue);
    }

    /** set all peaking filter */
    public void setFilterBand(int formFreq1 /* = 800  */, int gain1 /* = 36 */, int bandwidth1 /* = 3 */,
                              int formFreq2 /* = 1300 */, int gain2 /* = 24 */, int bandwidth2 /* = 3 */,
                              int formFreq3 /* = 2200 */, int gain3 /* = 12 */, int bandwidth3 /* = 3 */,
                              int formFreq4 /* = 3500 */, int gain4 /* = 9 */, int bandwidth4 /* = 3 */,
                              int formFreq5 /* = 4500 */, int gain5 /* = 6 */, int bandwidth5 /* = 3 */,
                              int formFreq6 /* = 5500 */, int gain6 /* = 3 */, int bandwidth6 /* = 3 */) {
        formant[0].update(SiFilterVowelFormant.calcFreqIndex(formFreq1), bandwidth1, gain1);
        formant[1].update(SiFilterVowelFormant.calcFreqIndex(formFreq2), bandwidth2, gain2);
        formant[2].update(SiFilterVowelFormant.calcFreqIndex(formFreq3), bandwidth3, gain3);
        formant[3].update(SiFilterVowelFormant.calcFreqIndex(formFreq4), bandwidth4, gain4);
        formant[4].update(SiFilterVowelFormant.calcFreqIndex(formFreq5), bandwidth5, gain5);
        formant[5].update(SiFilterVowelFormant.calcFreqIndex(formFreq6), bandwidth6, gain6);
    }

    private int _updateEvent(int time) {
        while (_eventQueue != null && _eventQueue.time == 0) {
            formant[0].update(_eventQueue.ifreq1, 3, _eventQueue.igain1);
            formant[1].update(_eventQueue.ifreq2, 2, _eventQueue.igain2);
            outputLevel = _eventQueue.outputLevel;
            _eventQueue = _eventQueue.next;
        }
        return (_eventQueue != null) ? _eventQueue.updateTime(time) : time;
    }

    // override functions
    //

    @Override
    public void initialize() {
    }

    /** */
    @Override
    public void mmlCallback(double[] args) {
        outputLevel = (!Double.isNaN(args[0])) ? (args[0] * 0.01) : 1;
        setFilterBand((!Double.isNaN(args[1])) ? (int) args[1] : 800,
                (!Double.isNaN(args[2])) ? (int) args[2] : 30, 3,
                (!Double.isNaN(args[3])) ? (int) args[3] : 1300,
                (!Double.isNaN(args[4])) ? (int) args[4] : 24, 3,
                (!Double.isNaN(args[5])) ? (int) args[5] : 2200,
                (!Double.isNaN(args[6])) ? (int) args[6] : 12, 3,
                (!Double.isNaN(args[7])) ? (int) args[7] : 3500,
                (!Double.isNaN(args[8])) ? (int) args[8] : 9, 3,
                (!Double.isNaN(args[9])) ? (int) args[9] : 4500,
                (!Double.isNaN(args[10])) ? (int) args[10] : 6, 3,
                (!Double.isNaN(args[11])) ? (int) args[11] : 5500,
                (!Double.isNaN(args[12])) ? (int) args[12] : 6, 3);
    }

    @Override
    public int prepareProcess() {
        _t0i0 = _t0i1 = _t0o0 = _t0o1 = 0;
        _t1i0 = _t1i1 = _t1o0 = _t1o1 = 0;
        _t2i0 = _t2i1 = _t2o0 = _t2o1 = 0;
        _t3i0 = _t3i1 = _t3o0 = _t3o1 = 0;
        _t4i0 = _t4i1 = _t4o0 = _t4o1 = 0;
        _t5i0 = _t5i1 = _t5o0 = _t5o1 = 0;
        return 1;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        int i, imax, istep;
        imax = startIndex + length;
        for (i = startIndex; i < imax; ) {
            istep = _updateEvent(length);
            processLFO(buffer, i, istep);
            i += istep;
            length -= istep;
        }
        return 1;
    }

    /** */
    protected void processLFO(double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        int i, imax = startIndex + length;
        double input, output;
        double f1ab1 = formant[0].ab1, f1a2 = formant[0].a2, f1b0 = formant[0].b0, f1b2 = formant[0].b2,
                f2ab1 = formant[1].ab1, f2a2 = formant[1].a2, f2b0 = formant[1].b0, f2b2 = formant[1].b2,
                f3ab1 = formant[2].ab1, f3a2 = formant[2].a2, f3b0 = formant[2].b0, f3b2 = formant[2].b2,
                f4ab1 = formant[3].ab1, f4a2 = formant[3].a2, f4b0 = formant[3].b0, f4b2 = formant[3].b2,
                f5ab1 = formant[4].ab1, f5a2 = formant[4].a2, f5b0 = formant[4].b0, f5b2 = formant[4].b2,
                f6ab1 = formant[5].ab1, f6a2 = formant[5].a2, f6b0 = formant[5].b0, f6b2 = formant[5].b2;
        for (i = startIndex; i < imax; ) {
            input = buffer[i];
            output = f1b0 * input + f1ab1 * _t0i0 + f1b2 * _t0i1 - f1ab1 * _t0o0 - f1a2 * _t0o1;
            _t0i1 = _t0i0;
            _t0i0 = input;
            _t0o1 = _t0o0;
            _t0o0 = input = output;
            output = f2b0 * input + f2ab1 * _t1i0 + f2b2 * _t1i1 - f2ab1 * _t1o0 - f2a2 * _t1o1;
            _t1i1 = _t1i0;
            _t1i0 = input;
            _t1o1 = _t1o0;
            _t1o0 = input = output;
            output = f3b0 * input + f3ab1 * _t2i0 + f3b2 * _t2i1 - f3ab1 * _t2o0 - f3a2 * _t2o1;
            _t2i1 = _t2i0;
            _t2i0 = input;
            _t2o1 = _t2o0;
            _t2o0 = input = output;
            output = f4b0 * input + f4ab1 * _t3i0 + f4b2 * _t3i1 - f4ab1 * _t3o0 - f4a2 * _t3o1;
            _t3i1 = _t3i0;
            _t3i0 = input;
            _t3o1 = _t3o0;
            _t3o0 = input = output;
            output = f5b0 * input + f5ab1 * _t4i0 + f5b2 * _t4i1 - f5ab1 * _t4o0 - f5a2 * _t4o1;
            _t4i1 = _t4i0;
            _t4i0 = input;
            _t4o1 = _t4o0;
            _t4o0 = input = output;
            output = f6b0 * input + f6ab1 * _t5i0 + f6b2 * _t5i1 - f6ab1 * _t5o0 - f6a2 * _t5o1;
            _t5i1 = _t5i0;
            _t5i0 = input;
            _t5o1 = _t5o0;
            _t5o0 = input = output;
            output *= outputLevel;
            if (output < -1) output = -1;
            else if (output > 1) output = 1;
            buffer[i] = output;
            i++;
            buffer[i] = output;
            i++;
        }
    }
}

class FormantEvent {

    public FormantEvent next;
    public int ifreq1, igain1, ifreq2, igain2;
    public double outputLevel;
    public int time;

    FormantEvent(int time, double outputLevel, int ifreq1, int igain1, int ifreq2, int igain2) {
        this.time = time;
        this.outputLevel = outputLevel;
        this.ifreq1 = ifreq1;
        this.igain1 = igain1;
        this.ifreq2 = ifreq2;
        this.igain2 = igain2;
    }

    public FormantEvent insertTo(FormantEvent list) {
        if (list == null) return this;
        if (this.time < list.time) {
            this.next = list;
            return this;
        }
        FormantEvent e = list;
        while (e.next != null) {
            if (e.time <= this.time && this.time < e.next.time) {
                this.next = e.next;
                e.next = this;
                break;
            }
            e = e.next;
        }
        e.next = this;
        return list;
    }

    public int updateTime(int prog) {
        if (prog > time) prog = time;
        for (FormantEvent e = this; e != null; e = e.next) e.time -= prog;
        return prog;
    }
}


class SiFilterVowelFormant {

    private static double[][] _alphaTable = null;
    private static double[] _cosTable = null;
    private static double[] _gainTable = null;
    private static double[] _ibandList = {0.25, 0.5, 0.75, 1, 1.5, 2, 3, 4};

    public static void initialize() {
        if (_alphaTable == null) {
            int iband, ifreq, igain, freq;
            double band;
            double[] table;
            double omg, cos, sin, angh;
            _alphaTable = new double[8][];
            for (iband = 0; iband < 8; iband++) {
                _alphaTable[iband] = table = new double[1024];
                band = _ibandList[iband];
                for (ifreq = 0, freq = 50; ifreq < 1024; ifreq++, freq *= 1.0218971486541166) { // 2^(1/32)
                    omg = freq * 0.00014247585730565955; // 2*pi/44100
                    sin = Math.sin(omg);
                    angh = 0.34657359027997264 * band * omg / sin; // log(2)*0.5
                    table[ifreq] = sin * (Math.exp(angh) - Math.exp(-angh)) * 0.5; // sin * sinh(angh)
                }
            }
            _cosTable = new double[1024];
            for (ifreq = 0, freq = 50; ifreq < 1024; ifreq++, freq *= 1.0218971486541166) { // 2^(1/32)
                _cosTable[ifreq] = Math.cos(freq * 0.00014247585730565955);
            }
            _gainTable = new double[128];
            for (igain = 0; igain < 128; igain++) {
                _gainTable[igain] = Math.pow(10, (igain - 32) * 0.025);
            }
        }
    }

    public static int calcFreqIndex(double frequency) {
        int ifreq = (int) ((Math.log(frequency) * 1.4426950408889633 - 5.643856189774724) * 32); // * 1/loge(2) - log2(50)
        if (ifreq < 0) return 0;
        if (ifreq > 1023) return 1023;
        return ifreq;
    }

    public double ab1, a2, b0, b2;

    public SiFilterVowelFormant() {
        clear();
    }

    public void clear() {
        b0 = 1;
        ab1 = a2 = b2 = 0;
    }

    /**
     * update filtering parameters
     *
     * @param ifreq frequency index. (0=50[Hz], 32=100[Hz], 64=200[Hz] ...1024=12800[Hz])
     * @param iband band width index. (0=0.125oct, 1=0.25oct, ...7=16oct)
     * @param gain  gain. ( -32dB - 96dB)
     */
    public void update(int ifreq, int iband, int gain) {
        gain += 32;
        if (gain < 0) gain = 0;
        else if (gain > 127) gain = 127;
        double alp = _alphaTable[iband][ifreq],
                A = _gainTable[gain],
                alpA = alp * A,
                alpiA = alp / A,
                ia0 = 1 / (1 + alpiA);
        ab1 = -2 * _cosTable[ifreq] * ia0;
        a2 = (1 - alpiA) * ia0;
        b0 = (1 + alpA) * ia0;
        b2 = (1 - alpA) * ia0;
    }
}
