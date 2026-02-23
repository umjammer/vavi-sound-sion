//
// SiOPM FM channel.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module.channels;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWaveBase;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveTable;
import org.si.utils.SLLint;


/**
 * FM sound channel.
 * <p>
 * The calculation of this class instanceof based on OPM emulation (refer from sources of mame, fmgen and x68sound).
 * And it has some extension to simulate other sevral fm sound modules (OPNA, OPLL, OPL2, OPL3, OPX, MA3, MA5, MA7, TSS and DX7).
 * <ul>
 *   <li>steleo output (from TSS,DX7)</li>
 *   <li>key scale level (from OPL3,OPX,MAx)</li>
 *   <li>phase select (from TSS)</li>
 *   <li>fixed frequency (from MAx)</li>
 *   <li>ssgec (from OPNA)</li>
 *   <li>wave shape select (from OPX,MAx,TSS)</li>
 *   <li>custom wave shape (from MAx)</li>
 *   <li>some more algolisms (from OPLx,OPX,MAx,DX7)</li>
 *   <li>decimal multiple (from p-TSS)</li>
 *   <li>feedback from op1-3 (from DX7)</li>
 *   <li>channel independet LFO (from TSS)</li>
 *   <li>low-pass filter envelop (from MAx)</li>
 *   <li>flexible fm connections (from TSS)</li>
 *   <li>ring modulation (from C64?)</li>
 * </ul>
 * </p>
 */
public class SiOPMChannelFM extends SiOPMChannelBase {

    // constants
    //
    private static final int PROC_OP1 = 0;
    private static final int PROC_OP2 = 1;
    private static final int PROC_OP3 = 2;
    private static final int PROC_OP4 = 3;
    private static final int PROC_ANA = 4;
    private static final int PROC_RNG = 5;
    private static final int PROC_SYN = 6;
    private static final int PROC_AFM = 7;
    private static final int PROC_PCM = 8;

    // variables
    //

    /** eg_out threshold to check idling */
    static int idlingThreshold = 5120; // = 256(resolution)*10(2^10=1024)*2(p/n) = volume<1/1024

    // Operators

    /** operators */
    public SiOPMOperator[] operator;
    /** active operator */
    public SiOPMOperator activeOperator;

    // Parameters

    /** count */
    protected int _operatorCount;
    /** algorism */
    protected int _algorism;

    // Processing
    /** process func */
    protected IntConsumer[][] _funcProcessList;
    /** process type */
    protected int _funcProcessType;

    // Pipe

    /** internal pipe0 */
    protected SLLint _pipe0;
    /** internal pipe1 */
    protected SLLint _pipe1;

    // modulation

    /** am depth */
    protected int _am_depth;    // = chip.amd<<(ams-1)
    /** am output level */
    protected int _am_out;
    /** pm depth */
    protected int _pm_depth;    // = chip.pmd<<(pms-1)
    /** pm output level */
    protected int _pm_out;

    // tone generator setting

    /** ENV_TIMER_INITIAL * freq_ratio */
    protected int _eg_timer_initial;
    /** LFO_TIMER_INITIAL * freq_ratio */
    protected int _lfo_timer_initial;

    /** register map type */
    int registerMapType;
    int registerMapChannel;

    // toString
    //

    /** Output parameters. */
    public String toString() {
        String str = "SiOPMChannelFM : operatorCount=";
        str += _operatorCount + "\n";
        str += $("fb ", _inputLevel - 6);
        str += $2("vol", _volumes[0], "pan", _pan - 64);
        if (operator[0] != null) str += operator[0] + "\n";
        if (operator[1] != null) str += operator[1] + "\n";
        if (operator[2] != null) str += operator[2] + "\n";
        if (operator[3] != null) str += operator[3] + "\n";
        return str;
    }

    String $(String p, int i) {
        return "  " + p + "=" + i + "\n";
    }

    String $2(String p, double i, String q, int j) {
        return "  " + p + "=" + i + " / " + q + "=" + j + "\n";
    }

    // constructor
    //

    /** constructor */
    SiOPMChannelFM(SiOPMModule chip) {
        super(chip);

        _funcProcessList = new IntConsumer[][] {
                {this::_proc1op_loff, this::_proc2op, this::_proc3op, this::_proc4op, this::_proc2ana, this::_procring, this::_procsync, this::_proc2op, this::_procpcm_loff},
                {this::_proc1op_lon, this::_proc2op, this::_proc3op, this::_proc4op, this::_proc2ana, this::_procring, this::_procsync, this::_proc2op, this::_procpcm_lon}
        };
        operator = new SiOPMOperator[4];
        operator[0] = _allocFMOperator();
        operator[1] = null;
        operator[2] = null;
        operator[3] = null;
        activeOperator = operator[0];

        _operatorCount = 1;
        _funcProcessType = PROC_OP1;
        _funcProcess = this::_proc1op_loff;

        _pipe0 = SLLint.allocRing(1, 0);
        _pipe1 = SLLint.allocRing(1, 0);

        initialize(null, 0);
    }

    // Chip settings
    //

    /** set chip "PSEUDO" frequency ratio by [%]. 100 means 3.56MHz. This value effects only for envelop and lfo speed. */
    @Override
    public void setFrequencyRatio(int ratio) {
        _freq_ratio = ratio;
        double r = (ratio != 0) ? (100 / ratio) : 1;
        _eg_timer_initial = (int) (SiOPMTable.ENV_TIMER_INITIAL * r);
        _lfo_timer_initial = (int) (SiOPMTable.LFO_TIMER_INITIAL * r);
    }

    // LFO settings
    //

    /**
     * initialize low frequency oscillator. and stop lfo
     *
     * @param waveform        LFO waveform. 0=saw, 1=pulse, 2=triangle, 3=noise. -1 to set customized wave table
     * @param customWaveTable customized wave table, the length instanceof 256 and the values are limited in the range of 0-255. This argument instanceof available when waveform=-1.
     */
    @Override
    public void initializeLFO(int waveform, int[] customWaveTable /* = null */) {
        super.initializeLFO(waveform, customWaveTable);
        _lfoSwitch(false);
        _am_depth = 0;
        _pm_depth = 0;
        _am_out = 0;
        _pm_out = 0;
        if (operator[0] != null) operator[0].setDetune2(0);
        if (operator[1] != null) operator[1].setDetune2(0);
        if (operator[2] != null) operator[2].setDetune2(0);
        if (operator[3] != null) operator[3].setDetune2(0);
    }

    /**
     * Amplitude modulation.
     *
     * @param depth depth = (ams) ? (amd &lt;&lt; (ams-1)) : 0;
     */
    @Override
    public void setAmplitudeModulation(int depth) {
        _am_depth = depth << 2;
        _am_out = (_lfo_waveTable[_lfo_phase] * _am_depth) >> 7 << 3;
        _lfoSwitch(_pm_depth != 0 || _am_depth > 0);
    }

    /**
     * Pitch modulation.
     *
     * @param depth depth = (pms&lt;6) ? (pmd &gt;&gt; (6-pms)) : (pmd &lt;&lt; (pms-5));
     */
    @Override
    public void setPitchModulation(int depth) {
        _pm_depth = depth;
        _pm_out = (((_lfo_waveTable[_lfo_phase] << 1) - 255) * _pm_depth) >> 8;
        _lfoSwitch(_pm_depth != 0 || _am_depth > 0);
        if (_pm_depth == 0) {
            if (operator[0] != null) operator[0].setDetune2(0);
            if (operator[1] != null) operator[1].setDetune2(0);
            if (operator[2] != null) operator[2].setDetune2(0);
            if (operator[3] != null) operator[3].setDetune2(0);
        }
    }

    /** lfo on/off */
    protected void _lfoSwitch(boolean sw) {
        _lfo_on = sw ? 1 : 0;
        _funcProcess = _funcProcessList[_lfo_on][_funcProcessType];
        _lfo_timer_step = (sw) ? _lfo_timer_step_ : 0;
    }

    // parameter setting
    //

    /**
     * Set by SiOPMChannelParam.
     *
     * @param param          SiOPMChannelParam.
     * @param withVolume     Set volume when its true.
     * @param withModulation Set modulation when its true.
     */
    @Override
    public void setSiOPMChannelParam(SiOPMChannelParam param, boolean withVolume, boolean withModulation /* = true */) {
        int i;
        if (param.opeCount == 0) return;

        if (withVolume) {
            int imax = SiOPMModule.STREAM_SEND_SIZE;
            for (i = 0; i < imax; i++) _volumes[i] = param.volumes[i];
            for (_hasEffectSend = false, i = 1; i < imax; i++) if (_volumes[i] > 0) _hasEffectSend = true;
            _pan = param.pan;
        }
        setFrequencyRatio(param.fratio);
        setAlgorism(param.opeCount, param.alg);
        setFeedBack(param.fb, param.fbc);
        if (withModulation) {
            initializeLFO(param.lfoWaveShape, null);
            _lfo_timer = (param.lfoFreqStep > 0) ? 1 : 0;
            _lfo_timer_step_ = _lfo_timer_step = param.lfoFreqStep;
            setAmplitudeModulation(param.amd);
            setPitchModulation(param.pmd);
        }
        setFilterType(param.filterType);
        setSVFilter(param.cutoff, param.resonance, param.far, param.fdr1, param.fdr2, param.frr, param.fdc1, param.fdc2, param.fsc, param.frc);
        for (i = 0; i < _operatorCount; i++) {
            operator[i].setSiOPMOperatorParam(param.operatorParam[i]);
        }
    }

    /**
     * Get SiOPMChannelParam.
     *
     * @param param SiOPMChannelParam.
     */
    @Override
    public void getSiOPMChannelParam(SiOPMChannelParam param) {
        int i, imax = SiOPMModule.STREAM_SEND_SIZE;
        for (i = 0; i < imax; i++) param.volumes[i] = _volumes[i];
        param.pan = _pan;
        param.fratio = _freq_ratio;
        param.opeCount = _operatorCount;
        param.alg = _algorism;
        param.fb = 0;
        param.fbc = 0;
        for (i = 0; i < _operatorCount; i++) {
            if (_inPipe == operator[i]._feedPipe) {
                param.fb = _inputLevel - 6;
                param.fbc = i;
                break;
            }
        }
        param.lfoWaveShape = _lfo_waveShape;
        param.lfoFreqStep = _lfo_timer_step_;
        param.amd = _am_depth;
        param.pmd = _pm_depth;
        for (i = 0; i < _operatorCount; i++) {
            operator[i].getSiOPMOperatorParam(param.operatorParam[i]);
        }
    }

    /**
     * Set sound by 14 basic params. The value of Integer.MIN_VALUE means not to change.
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
    public void setSiOPMParameters(int ar, int dr, int sr, int rr, int sl, int tl, int ksr, int ksl, int mul, int dt1, int detune, int ams, int phase, int fixNote) {
        SiOPMOperator ope = activeOperator;
        if (ar != Integer.MIN_VALUE) ope._ar = ar;
        if (dr != Integer.MIN_VALUE) ope._dr = dr;
        if (sr != Integer.MIN_VALUE) ope._sr = sr;
        if (rr != Integer.MIN_VALUE) ope._rr = rr;
        if (sl != Integer.MIN_VALUE) ope._sl = sl;
        if (tl != Integer.MIN_VALUE) ope._tl = tl;
        if (ksr != Integer.MIN_VALUE) ope._ks = ksr;
        if (ksl != Integer.MIN_VALUE) ope._ksl = ksl;
        if (mul != Integer.MIN_VALUE) ope.setMul(mul);
        if (dt1 != Integer.MIN_VALUE) ope._dt1 = dt1;
        if (detune != Integer.MIN_VALUE) ope.setDetune(detune);
        if (ams != Integer.MIN_VALUE) ope.setAms(ams);
        if (phase != Integer.MIN_VALUE) ope.setKeyOnPhase(phase);
        if (fixNote != Integer.MIN_VALUE) ope.setFixedPitchIndex(fixNote << 6);
    }

    /**
     * Set wave data.
     *
     * @param waveData SiOPMWavePCMTable to set.
     */
    @Override
    public void setWaveData(SiOPMWaveBase waveData) {
        SiOPMWavePCMData pcmData = ((SiOPMWavePCMData) waveData);
        if (waveData instanceof SiOPMWavePCMTable)
            pcmData = ((SiOPMWavePCMTable) waveData)._table[60];

        if (pcmData != null && pcmData.wavelet != null) {
            _updateOperatorCount(1);
            _funcProcessType = PROC_PCM;
            _funcProcess = _funcProcessList[_lfo_on][_funcProcessType];
            activeOperator.setPCMData(pcmData);
            setErst(true);
        } else if (waveData instanceof SiOPMWaveTable) {
            SiOPMWaveTable waveTable = ((SiOPMWaveTable) waveData);
            if (waveTable.wavelet != null) {
                operator[0].setWaveTable(waveTable);
                if (operator[1] != null) operator[1].setWaveTable(waveTable);
                if (operator[2] != null) operator[2].setWaveTable(waveTable);
                if (operator[3] != null) operator[3].setWaveTable(waveTable);
            }
        }
    }

    /** set channel number (2nd argument of %) */
    @Override
    public void setChannelNumber(int channelNum) {
        registerMapChannel = channelNum;
    }

    /** set register */
    @Override
    public void setRegister(int addr, int data) {
        switch (registerMapType) {
            case 0:
                _setByOPMRegister(addr, data);
                break;
            case 1:
            default:
                _setBy2A03Register(addr, data);
                break;
        }
    }

    // 2A03 register value
    private void _setBy2A03Register(int addr, int data) {
    }

    // OPM register value
    private int _pmd = 0, _amd = 0;

    private void _setByOPMRegister(int addr, int data) {
        int i, v, pms, ams;
        SiOPMOperator op;
        int channel = registerMapChannel;

        if (addr < 0x20) {  // Module parameter
            switch (addr) {
                case 15: // NOIZE:7 FREQ:4-0 for channel#7
                    if (channel == 7 && _operatorCount == 4 && (data & 128) != 0) {
                        operator[3].setPgType(SiOPMTable.PG_NOISE_PULSE);
                        operator[3].setPtType(SiOPMTable.PT_OPM_NOISE);
                        operator[3].setPitchIndex(((data & 31) << 6) + 2048);
                    }
                    break;
                case 24: // LFO FREQ:7-0 for all 8 channels
                    v = _table.lfo_timerSteps[data];
                    _lfo_timer = (v > 0) ? 1 : 0;
                    _lfo_timer_step_ = _lfo_timer_step = v;
                    break;
                case 25: // A(0)/P(1):7 DEPTH:6-0 for all 8 channels
                    if ((data & 128) != 0) _amd = data & 127;
                    else _pmd = data & 127;
                    break;
                case 27: // LFO WS:10 for all 8 channels
                    initializeLFO(data & 3, null);
                    break;
            }
        } else {
            if (channel == (addr & 7)) {
                if (addr < 0x40) {
                    // Channel parameter
                    switch ((addr - 0x20) >> 3) {
                        case 0: // L:7 R:6 FB:5-3 ALG:2-0
                            v = data >> 6;
                            setAlgorism(4, data & 7);
                            setFeedBack((data >> 3) & 7, 0);
                            _volumes[0] = (v != 0) ? 0.5 : 0;
                            _pan = (v == 1) ? 128 : (v == 2) ? 0 : 64;
                            break;
                        case 1: // KC:6-0
                            for (i = 0; i < 4; i++) operator[i].setKc(data & 127);
                            break;
                        case 2: // KF:6-0
                            for (i = 0; i < 4; i++) operator[i].setKf(data & 127);
                            break;
                        case 3: // PMS:6-4 AMS:10
                            pms = (data >> 4) & 7;
                            ams = (data) & 3;
                            if ((data & 128) != 0) setPitchModulation((pms < 6) ? (_pmd >> (6 - pms)) : (_pmd << (pms - 5)));
                            else setAmplitudeModulation((ams > 0) ? (_amd << (ams - 1)) : 0);
                            break;
                    }
                } else {
                    // Operator parameter
                    op = operator[new int[] {0, 2, 1, 3}[(addr >> 3) & 3]]; // [3,1,2,0]
                    switch ((addr - 0x40) >> 5) {
                        case 0: // DT1:6-4 MUL:3-0
                            op._dt1 = (data >> 4) & 7;
                            op.setMul((data) & 15);
                            break;
                        case 1: // TL:6-0
                            op._tl = data & 127;
                            break;
                        case 2: // KS:76 AR:4-0
                            op._ks = (data >> 6) & 3;
                            op._ar = (data & 31) << 1;
                            break;
                        case 3: // AMS:7 DR:4-0
                            op._ams = ((data >> 7) & 1) << 1;
                            op._dr = (data & 31) << 1;
                            break;
                        case 4: // DT2:76 SR:4-0
                            op.setDetune(new int[] {0, 384, 500, 608}[(data >> 6) & 3]);
                            op._sr = (data & 31) << 1;
                            break;
                        case 5: // SL:7-4 RR:3-0
                            op._sl = (data >> 4) & 15;
                            op._rr = (data & 15) << 2;
                            break;
                    }
                }
            }
        }
    }

    // interfaces
    //

    /**
     * Set algorism (&#64;al)
     *
     * @param cnt Operator count.
     * @param alg Algolism number of the operator's connection.
     */
    @Override
    public void setAlgorism(int cnt, int alg) {
        switch (cnt) {
            case 2:
                _algorism2(alg);
                break;
            case 3:
                _algorism3(alg);
                break;
            case 4:
                _algorism4(alg);
                break;
            case 5:
                _analog(alg);
                break;
            default:
                _algorism1(alg);
                break;
        }
    }

    /**
     * Set feedback(&#64;fb). This also initializes the input mode(&#64;i).
     *
     * @param fb  Feedback level. Ussualy in the range of 0-7.
     * @param fbc Feedback connection. Operator index which feeds back its output.
     */
    @Override
    public void setFeedBack(int fb, int fbc) {
        if (fb > 0) {
            // connect feedback pipe
            if (fbc < 0 || fbc >= _operatorCount) fbc = 0;
            _inPipe = operator[fbc]._feedPipe;
            _inPipe.i = 0;
            _inputLevel = fb + 6;
            _inputMode = INPUT_FEEDBACK;
        } else {
            // no feedback
            _inPipe = _chip.zeroBuffer;
            _inputLevel = 0;
            _inputMode = INPUT_ZERO;
        }
    }

    /** Set parameters (&#64; command). */
    @Override
    public void setParameters(int[] param) {
        setSiOPMParameters(param[1], param[2], param[3], param[4], param[5],
                param[6], param[7], param[8], param[9], param[10],
                param[11], param[12], param[13], param[14]);
    }

    /** pgType and ptType (&#64;) */
    @Override
    public void setType(int pgType, int ptType) {
        if (pgType >= SiOPMTable.PG_PCM) {
            SiOPMWavePCMTable pcm = _table.getPCMData(pgType - SiOPMTable.PG_PCM);
            // the ptType is set by setWaveData()
            if (pcm != null) setWaveData(pcm);
        } else {
            activeOperator.setPgType(pgType);
            activeOperator.setPtType(ptType);
            _funcProcess = _funcProcessList[_lfo_on][_funcProcessType];
        }
    }

    /** Attack rate */
    @Override
    public void setAllAttackRate(int ar) {
        int i;
        SiOPMOperator ope;
        for (i = 0; i < _operatorCount; i++) {
            ope = operator[i];
            if (ope._final) ope._ar = ar;
        }
    }

    /** Release rate (s) */
    @Override
    public void setAllReleaseRate(int rr) {
        int i;
        SiOPMOperator ope;
        for (i = 0; i < _operatorCount; i++) {
            ope = operator[i];
            if (ope._final) ope._rr = rr;
        }
    }

    // interfaces
    //

    /** pitch = (note &lt;&lt; 6) | (kf &amp; 63) [0,8191] */
    @Override
    public int getPitch() {
        return operator[_operatorCount - 1].getPitchIndex();
    }

    @Override
    public void setPitch(int p) {
        for (int i = 0; i < _operatorCount; i++) {
            operator[i].setPitchIndex(p);
        }
    }

    /** active operator index (i) */
    @Override
    public void setActiveOperatorIndex(int i) {
        int opeIndex = (i < 0) ? 0 : (i >= _operatorCount) ? (_operatorCount - 1) : i;
        activeOperator = operator[opeIndex];
    }

    /** release rate (&#64;rr) */
    @Override
    public void setRr(int i) {
        activeOperator._rr = i;
    }

    /** total level (&#64;tl) */
    @Override
    public void setTl(int i) {
        activeOperator._tl = i;
    }

    /** fine multiple (&#64;ml) */
    @Override
    public void setFmul(int i) {
        activeOperator.setFmul(i);
    }

    /** phase  (&#64;ph) */
    @Override
    public void setPhase(int i) {
        activeOperator.setKeyOnPhase(i);
    }

    /** detune (&#64;dt) */
    @Override
    public void setDetune(int i) {
        activeOperator.setDetune(i);
    }

    /** fixed pitch (&#64;fx) */
    @Override
    public void setFixedPitch(int i) {
        activeOperator.setFixedPitchIndex(i);
    }

    /** ssgec (&#64;se) */
    @Override
    public void setSsgec(int i) {
        activeOperator.setSsgec(i);
    }

    /** envelop reset (&#64;er) */
    @Override
    public void setErst(boolean b) {
        for (int i = 0; i < _operatorCount; i++) operator[i].setErst(b);
    }

    // volume controls
    //

    /** update all tl offsets of final carriors */
    @Override
    public void offsetVolume(int expression, int velocity) {
        int i;
        SiOPMOperator ope;
        int tl, x = expression << 1;
        tl = _expressionTable[x] + _veocityTable[velocity];
        for (i = 0; i < _operatorCount; i++) {
            ope = operator[i];
            if (ope._final) ope._tlOffset(tl);
            else ope._tlOffset(0);
        }
    }

    // operation
    //

    /** Initialize. */
    @Override
    public void initialize(SiOPMChannelBase prev, int bufferIndex) {
        // initialize operators
        _updateOperatorCount(1);
        operator[0].initialize();
        _isNoteOn = false;
        registerMapType = 0;
        registerMapChannel = 0;

        // initialize sound channel
        super.initialize(prev, bufferIndex);
    }

    /** Reset. */
    @Override
    public void reset() {
        // reset all operators
        for (int i = 0; i < _operatorCount; i++) {
            operator[i].reset();
        }
        _isNoteOn = false;
        _isIdling = true;
    }

    /** Note on. */
    @Override
    public void noteOn() {
        // operator note on
        for (int i = 0; i < _operatorCount; i++) {
            operator[i].noteOn();
        }
        _isNoteOn = true;
        _isIdling = false;
        super.noteOn();
    }

    /** Note off. */
    @Override
    public void noteOff() {
        // operator note off
        for (int i = 0; i < _operatorCount; i++) {
            operator[i].noteOff();
        }
        _isNoteOn = false;
        super.noteOff();
    }

    /** Prepare buffering */
    @Override
    public void resetChannelBufferStatus() {
        _bufferIndex = 0;

        // check idling flag
        int i;
        SiOPMOperator ope;
        _isIdling = true;
        for (i = 0; i < _operatorCount; i++) {
            ope = operator[i];
            if (ope._final && (ope._eg_out < idlingThreshold || ope._eg_state == SiOPMOperator.EG_ATTACK)) {
                _isIdling = false;
                break;
            }
        }
    }

    //
    // Internal uses
    //

    // processing operator x1
    //

    // without lfo_update()
    private void _proc1op_loff(int len) {
        int t, l, i, n;
        SiOPMOperator ope = operator[0];
        int[] log = _table.logTable;
        int phase_filter = SiOPMTable.PHASE_FILTER;

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // eg_update();
            //
            ope._eg_timer -= ope._eg_timer_step;
            if (ope._eg_timer < 0) {
                if (ope._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope._eg_incTable[ope._eg_counter];
                    if (t > 0) {
                        ope._eg_level -= 1 + (ope._eg_level >> t);
                        if (ope._eg_level <= 0) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                    }
                } else {
                    ope._eg_level += ope._eg_incTable[ope._eg_counter];
                    if (ope._eg_level >= ope._eg_stateShiftLevel) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                }
                ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                ope._eg_counter = (ope._eg_counter + 1) & 7;
                ope._eg_timer += _eg_timer_initial;
            }

            // pg_update();
            //
            ope._phase += ope._phase_step;
            t = ((ope._phase + (ip.i << _inputLevel)) & phase_filter) >> ope._waveFixedBits;
            l = ope._waveTable[t];
            l += ope._eg_out;
            t = log[l];
            ope._feedPipe.i = t;

            // output and increment pointers
            //
            op.i = t + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    // with lfo_update()
    private void _proc1op_lon(int len) {
        int t, l, i, n;
        SiOPMOperator ope = operator[0];
        int[] log = _table.logTable;
        int phase_filter = SiOPMTable.PHASE_FILTER;

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;

        for (i = 0; i < len; i++) {
            // lfo_update();
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // eg_update();
            //
            ope._eg_timer -= ope._eg_timer_step;
            if (ope._eg_timer < 0) {
                if (ope._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope._eg_incTable[ope._eg_counter];
                    if (t > 0) {
                        ope._eg_level -= 1 + (ope._eg_level >> t);
                        if (ope._eg_level <= 0) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                    }
                } else {
                    ope._eg_level += ope._eg_incTable[ope._eg_counter];
                    if (ope._eg_level >= ope._eg_stateShiftLevel) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                }
                ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                ope._eg_counter = (ope._eg_counter + 1) & 7;
                ope._eg_timer += _eg_timer_initial;
            }

            // pg_update();
            //
            ope._phase += ope._phase_step;
            t = ((ope._phase + (ip.i << _inputLevel)) & phase_filter) >> ope._waveFixedBits;
            l = ope._waveTable[t];
            l += ope._eg_out + (_am_out >> ope._ams);
            t = log[l];
            ope._feedPipe.i = t;

            // output and increment pointers
            //
            op.i = t + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    // processing operator x2
    //
    // This inline expansion makes execution faster.
    private void _proc2op(int len) {
        int i, t, l, n;
        int phase_filter = SiOPMTable.PHASE_FILTER;
        int[] log = _table.logTable;

        SiOPMOperator ope0 = operator[0];
        SiOPMOperator ope1 = operator[1];

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // clear pipes
            //
            _pipe0.i = 0;

            // lfo
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope0.setDetune2(_pm_out);
                ope1.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // operator[0]
            //
            // eg_update();
            ope0._eg_timer -= ope0._eg_timer_step;
            if (ope0._eg_timer < 0) {
                if (ope0._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope0._eg_incTable[ope0._eg_counter];
                    if (t > 0) {
                        ope0._eg_level -= 1 + (ope0._eg_level >> t);
                        if (ope0._eg_level <= 0) ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                    }
                } else {
                    ope0._eg_level += ope0._eg_incTable[ope0._eg_counter];
                    if (ope0._eg_level >= ope0._eg_stateShiftLevel)
                        ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                }
                ope0._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope0._eg_total_level) << 3;
                ope0._eg_counter = (ope0._eg_counter + 1) & 7;
                ope0._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope0._phase += ope0._phase_step;
            t = ((ope0._phase + (ip.i << _inputLevel)) & phase_filter) >> ope0._waveFixedBits;
            l = ope0._waveTable[t];
            l += ope0._eg_out + (_am_out >> ope0._ams);
            t = log[l];
            ope0._feedPipe.i = t;
            ope0._outPipe.i = t + ope0._basePipe.i;

            // operator[1]
            //
            // eg_update();
            ope1._eg_timer -= ope1._eg_timer_step;
            if (ope1._eg_timer < 0) {
                if (ope1._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope1._eg_incTable[ope1._eg_counter];
                    if (t > 0) {
                        ope1._eg_level -= 1 + (ope1._eg_level >> t);
                        if (ope1._eg_level <= 0) ope1._eg_shiftState(ope1._eg_nextState[ope1._eg_state]);
                    }
                } else {
                    ope1._eg_level += ope1._eg_incTable[ope1._eg_counter];
                    if (ope1._eg_level >= ope1._eg_stateShiftLevel)
                        ope1._eg_shiftState(ope1._eg_nextState[ope1._eg_state]);
                }
                ope1._eg_out = (ope1._eg_levelTable[ope1._eg_level] + ope1._eg_total_level) << 3;
                ope1._eg_counter = (ope1._eg_counter + 1) & 7;
                ope1._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope1._phase += ope1._phase_step;
            t = ((ope1._phase + (ope1._inPipe.i << ope1._fmShift)) & phase_filter) >> ope1._waveFixedBits;
            l = ope1._waveTable[t];
            l += ope1._eg_out + (_am_out >> ope1._ams);
            t = log[l];
            ope1._feedPipe.i = t;
            ope1._outPipe.i = t + ope1._basePipe.i;

            // output and increment pointers
            //
            op.i = _pipe0.i + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }


    // processing operator x3
    //
    // This inline expansion makes execution faster.
    private void _proc3op(int len) {
        int i, t, l, n;
        int phase_filter = SiOPMTable.PHASE_FILTER;
        int[] log = _table.logTable;

        SiOPMOperator ope0 = operator[0];
        SiOPMOperator ope1 = operator[1];
        SiOPMOperator ope2 = operator[2];

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // clear pipes
            //
            _pipe0.i = 0;
            _pipe1.i = 0;

            // lfo
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope0.setDetune2(_pm_out);
                ope1.setDetune2(_pm_out);
                ope2.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // operator[0]
            //
            // eg_update();
            ope0._eg_timer -= ope0._eg_timer_step;
            if (ope0._eg_timer < 0) {
                if (ope0._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope0._eg_incTable[ope0._eg_counter];
                    if (t > 0) {
                        ope0._eg_level -= 1 + (ope0._eg_level >> t);
                        if (ope0._eg_level <= 0) ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                    }
                } else {
                    ope0._eg_level += ope0._eg_incTable[ope0._eg_counter];
                    if (ope0._eg_level >= ope0._eg_stateShiftLevel)
                        ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                }
                ope0._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope0._eg_total_level) << 3;
                ope0._eg_counter = (ope0._eg_counter + 1) & 7;
                ope0._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope0._phase += ope0._phase_step;
            t = ((ope0._phase + (ip.i << _inputLevel)) & phase_filter) >> ope0._waveFixedBits;
            l = ope0._waveTable[t];
            l += ope0._eg_out + (_am_out >> ope0._ams);
            t = log[l];
            ope0._feedPipe.i = t;
            ope0._outPipe.i = t + ope0._basePipe.i;

            // operator[1]
            //
            // eg_update();
            ope1._eg_timer -= ope1._eg_timer_step;
            if (ope1._eg_timer < 0) {
                if (ope1._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope1._eg_incTable[ope1._eg_counter];
                    if (t > 0) {
                        ope1._eg_level -= 1 + (ope1._eg_level >> t);
                        if (ope1._eg_level <= 0) ope1._eg_shiftState(ope1._eg_nextState[ope1._eg_state]);
                    }
                } else {
                    ope1._eg_level += ope1._eg_incTable[ope1._eg_counter];
                    if (ope1._eg_level >= ope1._eg_stateShiftLevel)
                        ope1._eg_shiftState(ope1._eg_nextState[ope1._eg_state]);
                }
                ope1._eg_out = (ope1._eg_levelTable[ope1._eg_level] + ope1._eg_total_level) << 3;
                ope1._eg_counter = (ope1._eg_counter + 1) & 7;
                ope1._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope1._phase += ope1._phase_step;
            t = ((ope1._phase + (ope1._inPipe.i << ope1._fmShift)) & phase_filter) >> ope1._waveFixedBits;
            l = ope1._waveTable[t];
            l += ope1._eg_out + (_am_out >> ope1._ams);
            t = log[l];
            ope1._feedPipe.i = t;
            ope1._outPipe.i = t + ope1._basePipe.i;

            // operator[2]
            //
            // eg_update();
            ope2._eg_timer -= ope2._eg_timer_step;
            if (ope2._eg_timer < 0) {
                if (ope2._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope2._eg_incTable[ope2._eg_counter];
                    if (t > 0) {
                        ope2._eg_level -= 1 + (ope2._eg_level >> t);
                        if (ope2._eg_level <= 0) ope2._eg_shiftState(ope2._eg_nextState[ope2._eg_state]);
                    }
                } else {
                    ope2._eg_level += ope2._eg_incTable[ope2._eg_counter];
                    if (ope2._eg_level >= ope2._eg_stateShiftLevel)
                        ope2._eg_shiftState(ope2._eg_nextState[ope2._eg_state]);
                }
                ope2._eg_out = (ope2._eg_levelTable[ope2._eg_level] + ope2._eg_total_level) << 3;
                ope2._eg_counter = (ope2._eg_counter + 1) & 7;
                ope2._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope2._phase += ope2._phase_step;
            t = ((ope2._phase + (ope2._inPipe.i << ope2._fmShift)) & phase_filter) >> ope2._waveFixedBits;
            l = ope2._waveTable[t];
            l += ope2._eg_out + (_am_out >> ope2._ams);
            t = log[l];
            ope2._feedPipe.i = t;
            ope2._outPipe.i = t + ope2._basePipe.i;

            // output and increment pointers
            //
            op.i = _pipe0.i + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    // processing operator x4
    //

    // This inline expansion makes execution faster.
    private void _proc4op(int len) {
        int i, t, l, n;
        int phase_filter = SiOPMTable.PHASE_FILTER;
        int[] log = _table.logTable;

        SiOPMOperator ope0 = operator[0];
        SiOPMOperator ope1 = operator[1];
        SiOPMOperator ope2 = operator[2];
        SiOPMOperator ope3 = operator[3];

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // clear pipes
            //
            _pipe0.i = 0;
            _pipe1.i = 0;

            // lfo
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope0.setDetune2(_pm_out);
                ope1.setDetune2(_pm_out);
                ope2.setDetune2(_pm_out);
                ope3.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // operator[0]
            //
            // eg_update();
            ope0._eg_timer -= ope0._eg_timer_step;
            if (ope0._eg_timer < 0) {
                if (ope0._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope0._eg_incTable[ope0._eg_counter];
                    if (t > 0) {
                        ope0._eg_level -= 1 + (ope0._eg_level >> t);
                        if (ope0._eg_level <= 0) ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                    }
                } else {
                    ope0._eg_level += ope0._eg_incTable[ope0._eg_counter];
                    if (ope0._eg_level >= ope0._eg_stateShiftLevel)
                        ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                }
                ope0._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope0._eg_total_level) << 3;
                ope0._eg_counter = (ope0._eg_counter + 1) & 7;
                ope0._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope0._phase += ope0._phase_step;
            t = ((ope0._phase + (ip.i << _inputLevel)) & phase_filter) >> ope0._waveFixedBits;
            l = ope0._waveTable[t];
            l += ope0._eg_out + (_am_out >> ope0._ams);
            t = log[l];
            ope0._feedPipe.i = t;
            ope0._outPipe.i = t + ope0._basePipe.i;

            // operator[1]
            //
            // eg_update();
            ope1._eg_timer -= ope1._eg_timer_step;
            if (ope1._eg_timer < 0) {
                if (ope1._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope1._eg_incTable[ope1._eg_counter];
                    if (t > 0) {
                        ope1._eg_level -= 1 + (ope1._eg_level >> t);
                        if (ope1._eg_level <= 0) ope1._eg_shiftState(ope1._eg_nextState[ope1._eg_state]);
                    }
                } else {
                    ope1._eg_level += ope1._eg_incTable[ope1._eg_counter];
                    if (ope1._eg_level >= ope1._eg_stateShiftLevel)
                        ope1._eg_shiftState(ope1._eg_nextState[ope1._eg_state]);
                }
                ope1._eg_out = (ope1._eg_levelTable[ope1._eg_level] + ope1._eg_total_level) << 3;
                ope1._eg_counter = (ope1._eg_counter + 1) & 7;
                ope1._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope1._phase += ope1._phase_step;
            t = ((ope1._phase + (ope1._inPipe.i << ope1._fmShift)) & phase_filter) >> ope1._waveFixedBits;
            l = ope1._waveTable[t];
            l += ope1._eg_out + (_am_out >> ope1._ams);
            t = log[l];
            ope1._feedPipe.i = t;
            ope1._outPipe.i = t + ope1._basePipe.i;

            // operator[2]
            //
            // eg_update();
            ope2._eg_timer -= ope2._eg_timer_step;
            if (ope2._eg_timer < 0) {
                if (ope2._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope2._eg_incTable[ope2._eg_counter];
                    if (t > 0) {
                        ope2._eg_level -= 1 + (ope2._eg_level >> t);
                        if (ope2._eg_level <= 0) ope2._eg_shiftState(ope2._eg_nextState[ope2._eg_state]);
                    }
                } else {
                    ope2._eg_level += ope2._eg_incTable[ope2._eg_counter];
                    if (ope2._eg_level >= ope2._eg_stateShiftLevel)
                        ope2._eg_shiftState(ope2._eg_nextState[ope2._eg_state]);
                }
                ope2._eg_out = (ope2._eg_levelTable[ope2._eg_level] + ope2._eg_total_level) << 3;
                ope2._eg_counter = (ope2._eg_counter + 1) & 7;
                ope2._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope2._phase += ope2._phase_step;
            t = ((ope2._phase + (ope2._inPipe.i << ope2._fmShift)) & phase_filter) >> ope2._waveFixedBits;
            l = ope2._waveTable[t];
            l += ope2._eg_out + (_am_out >> ope2._ams);
            t = log[l];
            ope2._feedPipe.i = t;
            ope2._outPipe.i = t + ope2._basePipe.i;

            // operator[3]
            //
            // eg_update();
            ope3._eg_timer -= ope3._eg_timer_step;
            if (ope3._eg_timer < 0) {
                if (ope3._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope3._eg_incTable[ope3._eg_counter];
                    if (t > 0) {
                        ope3._eg_level -= 1 + (ope3._eg_level >> t);
                        if (ope3._eg_level <= 0) ope3._eg_shiftState(ope3._eg_nextState[ope3._eg_state]);
                    }
                } else {
                    ope3._eg_level += ope3._eg_incTable[ope3._eg_counter];
                    if (ope3._eg_level >= ope3._eg_stateShiftLevel)
                        ope3._eg_shiftState(ope3._eg_nextState[ope3._eg_state]);
                }
                ope3._eg_out = (ope3._eg_levelTable[ope3._eg_level] + ope3._eg_total_level) << 3;
                ope3._eg_counter = (ope3._eg_counter + 1) & 7;
                ope3._eg_timer += _eg_timer_initial;
            }
            // pg_update();
            ope3._phase += ope3._phase_step;
            t = ((ope3._phase + (ope3._inPipe.i << ope3._fmShift)) & phase_filter) >> ope3._waveFixedBits;
            l = ope3._waveTable[t];
            l += ope3._eg_out + (_am_out >> ope3._ams);
            t = log[l];
            ope3._feedPipe.i = t;
            ope3._outPipe.i = t + ope3._basePipe.i;

            // output and increment pointers
            //
            op.i = _pipe0.i + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    // processing PCM
    //
    private void _procpcm_loff(int len) {
        int t, l, i, n;
        SiOPMOperator ope = operator[0];
        int[] log = _table.logTable;
        int phase_filter = SiOPMTable.PHASE_FILTER;

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // eg_update();
            //
            ope._eg_timer -= ope._eg_timer_step;
            if (ope._eg_timer < 0) {
                if (ope._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope._eg_incTable[ope._eg_counter];
                    if (t > 0) {
                        ope._eg_level -= 1 + (ope._eg_level >> t);
                        if (ope._eg_level <= 0) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                    }
                } else {
                    ope._eg_level += ope._eg_incTable[ope._eg_counter];
                    if (ope._eg_level >= ope._eg_stateShiftLevel) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                }
                ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                ope._eg_counter = (ope._eg_counter + 1) & 7;
                ope._eg_timer += _eg_timer_initial;
            }

            // pg_update();
            //
            ope._phase += ope._phase_step;
            t = (ope._phase + (ip.i << _inputLevel)) >>> ope._waveFixedBits;
            if (t >= ope._pcm_endPoint) {
                if (ope._pcm_loopPoint == -1) {
                    ope._eg_shiftState(SiOPMOperator.EG_OFF);
                    ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                    for (; i < len; i++) {
                        op.i = bp.i;
                        ip = ip.next;
                        bp = bp.next;
                        op = op.next;
                    }
                    break;
                } else {
                    t -= ope._pcm_endPoint - ope._pcm_loopPoint;
                    ope._phase -= (ope._pcm_endPoint - ope._pcm_loopPoint) << ope._waveFixedBits;
                }
            }
            l = ope._waveTable[t];
            l += ope._eg_out;
            t = log[l];
            ope._feedPipe.i = t;

            // output and increment pointers
            //
            op.i = t + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    private void _procpcm_lon(int len) {
        int t, l, i, n;
        SiOPMOperator ope = operator[0];
        int[] log = _table.logTable;
        int phase_filter = SiOPMTable.PHASE_FILTER;

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;

        for (i = 0; i < len; i++) {
            // lfo_update();
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // eg_update();
            //
            ope._eg_timer -= ope._eg_timer_step;
            if (ope._eg_timer < 0) {
                if (ope._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope._eg_incTable[ope._eg_counter];
                    if (t > 0) {
                        ope._eg_level -= 1 + (ope._eg_level >> t);
                        if (ope._eg_level <= 0) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                    }
                } else {
                    ope._eg_level += ope._eg_incTable[ope._eg_counter];
                    if (ope._eg_level >= ope._eg_stateShiftLevel) ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                }
                ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                ope._eg_counter = (ope._eg_counter + 1) & 7;
                ope._eg_timer += _eg_timer_initial;
            }

            // pg_update();
            //
            ope._phase += ope._phase_step;
            t = (ope._phase + (ip.i << _inputLevel)) >>> ope._waveFixedBits;
            if (t >= ope._pcm_endPoint) {
                if (ope._pcm_loopPoint == -1) {
                    ope._eg_shiftState(SiOPMOperator.EG_OFF);
                    ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                    for (; i < len; i++) {
                        op.i = bp.i;
                        ip = ip.next;
                        bp = bp.next;
                        op = op.next;
                    }
                    break;
                } else {
                    t -= ope._pcm_endPoint - ope._pcm_loopPoint;
                    ope._phase -= (ope._pcm_endPoint - ope._pcm_loopPoint) << ope._waveFixedBits;
                }
            }
            l = ope._waveTable[t];
            l += ope._eg_out + (_am_out >> ope._ams);
            t = log[l];
            ope._feedPipe.i = t;

            // output and increment pointers
            //
            op.i = t + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    // analog like processing (w/ ring and sync)
    //
    private void _proc2ana(int len) {
        int i, t, out0, out1, l, n;
        int phase_filter = SiOPMTable.PHASE_FILTER;
        int[] log = _table.logTable;
        SiOPMOperator ope0 = operator[0];
        SiOPMOperator ope1 = operator[1];

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // lfo
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope0.setDetune2(_pm_out);
                ope1.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // envelop
            //
            ope0._eg_timer -= ope0._eg_timer_step;
            if (ope0._eg_timer < 0) {
                if (ope0._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope0._eg_incTable[ope0._eg_counter];
                    if (t > 0) {
                        ope0._eg_level -= 1 + (ope0._eg_level >> t);
                        if (ope0._eg_level <= 0) ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                    }
                } else {
                    ope0._eg_level += ope0._eg_incTable[ope0._eg_counter];
                    if (ope0._eg_level >= ope0._eg_stateShiftLevel)
                        ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                }
                ope0._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope0._eg_total_level) << 3;
                ope1._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope1._eg_total_level) << 3;
                ope0._eg_counter = (ope0._eg_counter + 1) & 7;
                ope0._eg_timer += _eg_timer_initial;
            }

            // operator[0]
            //
            ope0._phase += ope0._phase_step;
            t = ((ope0._phase + (ip.i << _inputLevel)) & phase_filter) >> ope0._waveFixedBits;
            l = ope0._waveTable[t];
            l += ope0._eg_out + (_am_out >> ope0._ams);
            out0 = log[l];

            // operator[1] with op0s envelop and ams
            //
            ope1._phase += ope1._phase_step;
            t = (ope1._phase & phase_filter) >> ope1._waveFixedBits;
            l = ope1._waveTable[t];
            l += ope1._eg_out + (_am_out >> ope0._ams);
            out1 = log[l];

            // output and increment pointers
            //
            ope0._feedPipe.i = out0;
            op.i = out0 + out1 + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    private void _procring(int len) {
        int i, t, out0, l, n;
        int phase_filter = SiOPMTable.PHASE_FILTER;
        int [] log = _table.logTable;
        SiOPMOperator ope0 = operator[0];
        SiOPMOperator ope1 = operator[1];

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // lfo
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope0.setDetune2(_pm_out);
                ope1.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // envelop
            //
            ope0._eg_timer -= ope0._eg_timer_step;
            if (ope0._eg_timer < 0) {
                if (ope0._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope0._eg_incTable[ope0._eg_counter];
                    if (t > 0) {
                        ope0._eg_level -= 1 + (ope0._eg_level >> t);
                        if (ope0._eg_level <= 0) ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                    }
                } else {
                    ope0._eg_level += ope0._eg_incTable[ope0._eg_counter];
                    if (ope0._eg_level >= ope0._eg_stateShiftLevel)
                        ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                }
                ope0._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope0._eg_total_level) << 3;
                ope1._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope1._eg_total_level) << 3;
                ope0._eg_counter = (ope0._eg_counter + 1) & 7;
                ope0._eg_timer += _eg_timer_initial;
            }

            // operator[0]
            //
            ope0._phase += ope0._phase_step;
            t = ((ope0._phase + (ip.i << _inputLevel)) & phase_filter) >> ope0._waveFixedBits;
            l = ope0._waveTable[t];

            // operator[1] with op0s envelop and ams
            //
            ope1._phase += ope1._phase_step;
            t = (ope1._phase & phase_filter) >> ope1._waveFixedBits;
            l += ope1._waveTable[t];
            l += ope1._eg_out + (_am_out >> ope0._ams);
            out0 = log[l];

            // output and increment pointers
            //
            ope0._feedPipe.i = out0;
            op.i = out0 + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    private void _procsync(int len) {
        int i, t, out0, out1, l, n;
        int phase_filter = SiOPMTable.PHASE_FILTER;
        int[] log = _table.logTable;
        int phase_overflow = SiOPMTable.PHASE_MAX;

        SiOPMOperator ope0 = operator[0];
        SiOPMOperator ope1 = operator[1];

        // buffering
        SLLint ip = _inPipe;
        SLLint bp = _basePipe;
        SLLint op = _outPipe;
        for (i = 0; i < len; i++) {
            // lfo
            //
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                _am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                ope0.setDetune2(_pm_out);
                ope1.setDetune2(_pm_out);
                _lfo_timer += _lfo_timer_initial;
            }

            // envelop
            //
            ope0._eg_timer -= ope0._eg_timer_step;
            if (ope0._eg_timer < 0) {
                if (ope0._eg_state == SiOPMOperator.EG_ATTACK) {
                    t = ope0._eg_incTable[ope0._eg_counter];
                    if (t > 0) {
                        ope0._eg_level -= 1 + (ope0._eg_level >> t);
                        if (ope0._eg_level <= 0) ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                    }
                } else {
                    ope0._eg_level += ope0._eg_incTable[ope0._eg_counter];
                    if (ope0._eg_level >= ope0._eg_stateShiftLevel)
                        ope0._eg_shiftState(ope0._eg_nextState[ope0._eg_state]);
                }
                ope0._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope0._eg_total_level) << 3;
                ope1._eg_out = (ope0._eg_levelTable[ope0._eg_level] + ope1._eg_total_level) << 3;
                ope0._eg_counter = (ope0._eg_counter + 1) & 7;
                ope0._eg_timer += _eg_timer_initial;
            }

            // operator[0]
            //
            ope0._phase += ope0._phase_step + (ip.i << _inputLevel);
            if ((ope0._phase & phase_overflow) != 0) ope1._phase = ope1._keyon_phase;
            ope0._phase = ope0._phase & phase_filter;

            // operator[1] with op0s envelop and ams
            //
            ope1._phase += ope1._phase_step;
            t = (ope1._phase & phase_filter) >> ope1._waveFixedBits;
            l = ope1._waveTable[t];
            l += ope1._eg_out + (_am_out >> ope0._ams);
            out0 = log[l];

            // output and increment pointers
            //
            ope0._feedPipe.i = out0;
            op.i = out0 + bp.i;
            ip = ip.next;
            bp = bp.next;
            op = op.next;
        }

        // update pointers
        _inPipe = ip;
        _basePipe = bp;
        _outPipe = op;
    }

    // internal operations
    //

    /** Update LFO. This code instanceof only for testing. */
    void _lfo_update() {
        _lfo_timer -= _lfo_timer_step;
        if (_lfo_timer < 0) {
            _lfo_phase = (_lfo_phase + 1) & 255;
            _am_out = (_lfo_waveTable[_lfo_phase] * _am_depth) >> 7 << 3;
            _pm_out = (((_lfo_waveTable[_lfo_phase] << 1) - 255) * _pm_depth) >> 8;
            if (operator[0] != null) operator[0].setDetune2(_pm_out);
            if (operator[1] != null) operator[1].setDetune2(_pm_out);
            if (operator[2] != null) operator[2].setDetune2(_pm_out);
            if (operator[3] != null) operator[3].setDetune2(_pm_out);
            _lfo_timer += _lfo_timer_initial;
        }
    }

    // update operator count.
    private void _updateOperatorCount(int cnt) {
        int i;

        // change operator instances
        if (_operatorCount < cnt) {
            // allocate and initialize new operators
            for (i = _operatorCount; i < cnt; i++) {
                operator[i] = _allocFMOperator();
                operator[i].initialize();
            }
        } else if (_operatorCount > cnt) {
            // free old operators
            for (i = cnt; i < _operatorCount; i++) {
                _freeFMOperator(operator[i]);
                operator[i] = null;
            }
        }

        // update count
        _operatorCount = cnt;
        _funcProcessType = cnt - 1;
        // select processing function
        _funcProcess = _funcProcessList[_lfo_on][_funcProcessType];

        // default active operator is the last one.
        activeOperator = operator[_operatorCount - 1];

        // reset feed back
        if (_inputMode == INPUT_FEEDBACK) {
            setFeedBack(0, 0);
        }
    }

    // alg operator=1
    private void _algorism1(int alg) {
        _updateOperatorCount(1);
        _algorism = alg;
        operator[0]._setPipes(_pipe0, null, true);
    }

    // alg operator=2
    private void _algorism2(int alg) {
        _updateOperatorCount(2);
        _algorism = alg;
        switch (_algorism) {
            case 0: // OPL3/MA3:con=0, OPX:con=0, 1(fbc=1)
                // o1(o0)
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, true);
                break;
            case 1: // OPL3/MA3:con=1, OPX:con=2
                // o0+o1
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe0, null, true);
                break;
            case 2: // OPX:con=3
                // o0+o1(o0)
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe0, _pipe0, true);
                operator[1]._basePipe = _pipe0;
                break;
            default:
                // o0+o1
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe0, null, true);
                break;
        }
    }

    // alg operator=3
    private void _algorism3(int alg) {
        _updateOperatorCount(3);
        _algorism = alg;
        switch (_algorism) {
            case 0: // OPX:con=0, 1(fbc=1)
                // o2(o1(o0))
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, false);
                operator[2]._setPipes(_pipe0, _pipe0, true);
                break;
            case 1: // OPX:con=2
                // o2(o0+o1)
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, null, false);
                operator[2]._setPipes(_pipe0, _pipe0, true);
                break;
            case 2: // OPX:con=3
                // o0+o2(o1)
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe1, null, false);
                operator[2]._setPipes(_pipe0, _pipe1, true);
                break;
            case 3: // OPX:con=4, 5(fbc=1)
                // o1(o0)+o2
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, true);
                operator[2]._setPipes(_pipe0, null, true);
                break;
            case 4:
                // o1(o0)+o2(o0)
                operator[0]._setPipes(_pipe1, null, false);
                operator[1]._setPipes(_pipe0, _pipe1, true);
                operator[2]._setPipes(_pipe0, _pipe1, true);
                break;
            case 5: // OPX:con=6
                // o0+o1+o2
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe0, null, true);
                operator[2]._setPipes(_pipe0, null, true);
                break;
            case 6: // OPX:con=7
                // o0+o1(o0)+o2
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, true);
                operator[1]._basePipe = _pipe0;
                operator[2]._setPipes(_pipe0, null, true);
                break;
            default:
                // o0+o1+o2
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe0, null, true);
                operator[2]._setPipes(_pipe0, null, true);
                break;
        }
    }

    // alg operator=4
    private void _algorism4(int alg) {
        _updateOperatorCount(4);
        _algorism = alg;
        switch (_algorism) {
            case 0: // OPL3:con=0, MA3:con=4, OPX:con=0, 1(fbc=1)
                // o3(o2(o1(o0)))
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, false);
                operator[2]._setPipes(_pipe0, _pipe0, false);
                operator[3]._setPipes(_pipe0, _pipe0, true);
                break;
            case 1: // OPX:con=2
                // o3(o2(o0+o1))
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, null, false);
                operator[2]._setPipes(_pipe0, _pipe0, false);
                operator[3]._setPipes(_pipe0, _pipe0, true);
                break;
            case 2: // MA3:con=3, OPX:con=3
                // o3(o0+o2(o1))
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe1, null, false);
                operator[2]._setPipes(_pipe0, _pipe1, false);
                operator[3]._setPipes(_pipe0, _pipe0, true);
                break;
            case 3: // OPX:con=4, 5(fbc=1)
                // o3(o1(o0)+o2)
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, false);
                operator[2]._setPipes(_pipe0, null, false);
                operator[3]._setPipes(_pipe0, _pipe0, true);
                break;
            case 4: // OPL3:con=1, MA3:con=5, OPX:con=6, 7(fbc=1)
                // o1(o0)+o3(o2)
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, true);
                operator[2]._setPipes(_pipe1, null, false);
                operator[3]._setPipes(_pipe0, _pipe1, true);
                break;
            case 5: // OPX:con=12
                // o1(o0)+o2(o0)+o3(o0)
                operator[0]._setPipes(_pipe1, null, false);
                operator[1]._setPipes(_pipe0, _pipe1, true);
                operator[2]._setPipes(_pipe0, _pipe1, true);
                operator[3]._setPipes(_pipe0, _pipe1, true);
                break;
            case 6: // OPX:con=10, 11(fbc=1)
                // o1(o0)+o2+o3
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, true);
                operator[2]._setPipes(_pipe0, null, true);
                operator[3]._setPipes(_pipe0, null, true);
                break;
            case 7: // MA3:con=2, OPX:con=15
                // o0+o1+o2+o3
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe0, null, true);
                operator[2]._setPipes(_pipe0, null, true);
                operator[3]._setPipes(_pipe0, null, true);
                break;
            case 8: // OPL3:con=2, MA3:con=6, OPX:con=8
                // o0+o3(o2(o1))
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe1, null, false);
                operator[2]._setPipes(_pipe1, _pipe1, false);
                operator[3]._setPipes(_pipe0, _pipe1, true);
                break;
            case 9: // OPL3:con=3, MA3:con=7, OPX:con=13
                // o0+o2(o1)+o3
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe1, null, false);
                operator[2]._setPipes(_pipe0, _pipe1, true);
                operator[3]._setPipes(_pipe0, null, true);
                break;
            case 10: // for DX7 emulation
                // o3(o0+o1+o2)
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, null, false);
                operator[2]._setPipes(_pipe0, null, false);
                operator[3]._setPipes(_pipe0, _pipe0, true);
                break;
            case 11: // OPX:con=9
                // o0+o3(o1+o2)
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe1, null, false);
                operator[2]._setPipes(_pipe1, null, false);
                operator[3]._setPipes(_pipe0, _pipe1, true);
                break;
            case 12: // OPX:con=14
                // o0+o1(o0)+o3(o2)
                operator[0]._setPipes(_pipe0, null, false);
                operator[1]._setPipes(_pipe0, _pipe0, true);
                operator[1]._basePipe = _pipe0;
                operator[2]._setPipes(_pipe1, null, false);
                operator[3]._setPipes(_pipe0, _pipe1, true);
                break;
            default:
                // o0+o1+o2+o3
                operator[0]._setPipes(_pipe0, null, true);
                operator[1]._setPipes(_pipe0, null, true);
                operator[2]._setPipes(_pipe0, null, true);
                operator[3]._setPipes(_pipe0, null, true);
                break;
        }
    }

    // analog like operation
    private void _analog(int alg) {
        _updateOperatorCount(2);
        operator[0]._setPipes(_pipe0, null, true);
        operator[1]._setPipes(_pipe0, null, true);

        _algorism = (alg >= 0 && alg <= 3) ? alg : 0;
        _funcProcessType = PROC_ANA + _algorism;
        _funcProcess = _funcProcessList[_lfo_on][_funcProcessType];
    }

    // SiOPMOperator factory
    //

    // Free list for SiOPMOperator
    private List<SiOPMOperator> _freeOperators = new ArrayList<SiOPMOperator>();

    /** Alloc operator instance WITHOUT initializing. Call from SiOPMChannelFM. */
    protected SiOPMOperator _allocFMOperator() {
        var x = _freeOperators.remove(_freeOperators.size() - 1);
        return x != null ? x : new SiOPMOperator(_chip);
    }

    /** Free operator instance. Call from SiOPMChannelFM. */
    protected void _freeFMOperator(SiOPMOperator osc) {
        _freeOperators.add(osc);
    }
}
