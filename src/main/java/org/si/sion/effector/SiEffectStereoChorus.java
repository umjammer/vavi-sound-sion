//
// SiOPM effect stereo chorus
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;

/** Stereo chorus effector. */
public class SiEffectStereoChorus extends SiEffectBase {

    // variables
    //
    private static final int DELAY_BUFFER_BITS = 12;
    private static final int DELAY_BUFFER_FILTER = (1 << DELAY_BUFFER_BITS) - 1;

    private final double[] _delayBufferL;
    private final double[] _delayBufferR;
    private int _pointerRead;
    private int _pointerWrite;
    private double _feedback;
    private double _depth;
    private double _wet;

    private int _lfoPhase;
    private int _lfoStep;
    private int _lfoResidueStep;
    private int _phaseInvert;
    private int[] _phaseTable;

    // constructor
    //

    /**
     * constructor.
     *
     * @param delayTime delay time[ms]. maximum value instanceof about 94.
     * @param feedback  feedback ratio(0-1).
     * @param frequency frequency of chorus[Hz].
     * @param depth     depth of chorus.
     * @param wet       wet mixing level(0-1).
     */
    public SiEffectStereoChorus(double delayTime, double feedback, double frequency, double depth, double wet, boolean invertPhase) {
        _delayBufferL = new double[1 << DELAY_BUFFER_BITS];
        _delayBufferR = new double[1 << DELAY_BUFFER_BITS];

        _lfoPhase = 0;
        _lfoResidueStep = 0;
        _pointerRead = 0;
        setParameters(delayTime, feedback, frequency, depth, wet, invertPhase);
    }

    // operation
    //

    /**
     * set parameter
     *
     * @param delayTime delay time[ms]. maximum value instanceof about 94.
     * @param feedback  feedback ratio(0-1).
     * @param frequency frequency of chorus[Hz].
     * @param depth     depth of chorus.
     * @param wet       wet mixing level(0-1).
     */
    public void setParameters(double delayTime, double feedback, double frequency, double depth, double wet, boolean invertPhase) {
        if (frequency == 0 || depth == 0 || delayTime == 0)
            throw new Error("SiEffectStereoChorus; frequency, depth or delay should not be 0.");
        int offset = (int) (delayTime * 44.1);
        int tableSize;
        double dp;
        int i, p;
        if (offset > DELAY_BUFFER_FILTER) offset = DELAY_BUFFER_FILTER;
        _pointerWrite = (_pointerRead + offset) & DELAY_BUFFER_FILTER;
        _feedback = (feedback >= 1) ? 0.9990234375 : (feedback <= -1) ? -0.9990234375 : feedback;
        _depth = (depth >= offset - 4) ? (offset - 4) : depth;
        tableSize = (int) (_depth * 6.283185307179586);
        if (tableSize * frequency > 11025) tableSize = (int) (11025 / frequency);
        if (_phaseTable == null || _phaseTable.length != tableSize) _phaseTable = new int[tableSize];
        dp = 6.283185307179586 / tableSize;
        for (i = 0, p = 0; i < tableSize; i++, p += dp) _phaseTable[i] = (int) (Math.sin(p) * _depth + 0.5);
        _lfoStep = (int) (44100 / (tableSize * frequency));
        if (_lfoStep <= 4) _lfoStep = 4;
        _lfoResidueStep = _lfoStep << 1;
        _wet = wet;
        _phaseInvert = (invertPhase) ? -1 : 1;
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(20, 0.2, 4, 20, 0.5, true);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[0])) ? args[0] : 20,
                (!Double.isNaN(args[1])) ? (args[1] * 0.01) : 0.2,
                (!Double.isNaN(args[2])) ? args[2] : 4,
                (!Double.isNaN(args[3])) ? args[3] : 20,
                (!Double.isNaN(args[4])) ? (args[4] * 0.01) : 0.5,
                (!Double.isNaN(args[5])) ? (args[5] != 0) : true);
    }

    @Override
    public int prepareProcess() {
        _lfoPhase = 0;
        _lfoResidueStep = 0;
        _pointerRead = 0;
        int i, imax = 1 << DELAY_BUFFER_BITS;
        for (i = 0; i < imax; i++) _delayBufferL[i] = _delayBufferR[i] = 0;
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
            _processLFO(buffer, i, istep);
            if (++_lfoPhase == _phaseTable.length) _lfoPhase = 0;
            i += istep;
            istep = _lfoStep << 1;
        }
        _processLFO(buffer, i, imax - i);
        _lfoResidueStep = istep - (imax - i);
        return channels;
    }

    // process inside
    private void _processLFO(double[] buffer, int startIndex, int length) {
        int i, imax = startIndex + length, p;
        double n, m, delayL = _phaseTable[_lfoPhase], delayR = _phaseTable[_lfoPhase] * _phaseInvert,
                dry = 1 - _wet;
        for (i = startIndex; i < imax; ) {
            p = (int) (_pointerRead + delayL) & DELAY_BUFFER_FILTER;
            n = _delayBufferL[p];
            m = buffer[i] - n * _feedback;
            _delayBufferL[_pointerWrite] = m;
            buffer[i] *= dry;
            buffer[i] += n * _wet;
            i++;
            p = (int) (_pointerRead + delayR) & DELAY_BUFFER_FILTER;
            n = _delayBufferR[p];
            m = buffer[i] - n * _feedback;
            _delayBufferR[_pointerWrite] = m;
            buffer[i] *= dry;
            buffer[i] += n * _wet;
            i++;
            _pointerWrite = (_pointerWrite + 1) & DELAY_BUFFER_FILTER;
            _pointerRead = (_pointerRead + 1) & DELAY_BUFFER_FILTER;
        }
    }
}
