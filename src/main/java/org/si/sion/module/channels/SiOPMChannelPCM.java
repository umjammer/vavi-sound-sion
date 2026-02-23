//
// SiOPM FM channel.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module.channels;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMStream;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWaveBase;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.utils.SLLint;


/**
 * PCM channel
 */
public class SiOPMChannelPCM extends SiOPMChannelBase {

    // variables
    //

    /** eg_out threshold to check idling */
    static final int idlingThreshold = 5120; // = 256(resolution)*10(2^10=1024)*2(p/n) = volume<1/1024

    // Operators
    /** operator for layer0 */
    public SiOPMOperator operator;

    // Parameters
    /** pcm table */
    protected SiOPMWavePCMTable _pcmTable;
    /** for stereo filter */
    protected double[] _filterVriables2;

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

    // pitch shift for sampling point
    private int _samplePitchShift;
    // volunme of current note
    private double _sampleVolume;
    // pan of current note
    private int _samplePan;
    // output pipe for stereo
    private SLLint _outPipe2;
    // waveFixedBits for PCM
    private static final int PCM_waveFixedBits = 11; // <= Should be 11, This is adhoc solution !

    // toString
    //

    /** Output parameters. */
    public String toString() {
        String str = "SiOPMChannelPCM : \n";
        str += $2("vol", _volumes[0], "pan", _pan - 64);
        str += String.valueOf(operator) + "\n";
        return str;
    }

    String $(int p, int i) {
        return "  " + p + "=" + i + "\n";
    }

    String $2(String p, double i, String q, int j) {
        return "  " + p + "=" + i + " / " + q + "=" + j + "\n";
    }

    // constructor
    //

    /** constructor */
    public SiOPMChannelPCM(SiOPMModule chip) {
        super(chip);

        operator = new SiOPMOperator(chip);
        _filterVriables2 = new double[3];

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
    public void initializeLFO(int waveform, int[] customWaveTable /* = null */) {
        super.initializeLFO(waveform, customWaveTable);
        _lfoSwitch(false);
        _am_depth = 0;
        _pm_depth = 0;
        _am_out = 0;
        _pm_out = 0;
        _pcmTable = null;
        operator.setDetune2(0);
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
            operator.setDetune2(0);
        }
    }

    /** lfo on/off */
    protected void _lfoSwitch(boolean sw) {
        _lfo_on = sw ? 1 : 0;
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
    public void setSiOPMChannelParam(SiOPMChannelParam param, boolean withVolume, boolean withModulation) {
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
        //setFeedBack(param.fb, param.fbc);
        if (withModulation) {
            initializeLFO(param.lfoWaveShape, null);
            _lfo_timer = (param.lfoFreqStep > 0) ? 1 : 0;
            _lfo_timer_step_ = _lfo_timer_step = param.lfoFreqStep;
            setAmplitudeModulation(param.amd);
            setPitchModulation(param.pmd);
        }
        setFilterType(param.filterType);
        setSVFilter(param.cutoff, param.resonance, param.far, param.fdr1, param.fdr2, param.frr, param.fdc1, param.fdc2, param.fsc, param.frc);
        operator.setSiOPMOperatorParam(param.operatorParam[0]);
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
        param.opeCount = 1;
        param.alg = 0;
        param.fb = 0;
        param.fbc = 0;
        param.lfoWaveShape = _lfo_waveShape;
        param.lfoFreqStep = _lfo_timer_step_;
        param.amd = _am_depth;
        param.pmd = _pm_depth;
        operator.getSiOPMOperatorParam(param.operatorParam[0]);
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
        SiOPMOperator ope = operator;
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
     * Set wave data. (called from setType())
     *
     * @param waveData SiOPMWavePCMTable to set.
     */
    @Override
    public void setWaveData(SiOPMWaveBase waveData) {
        SiOPMWavePCMData pcm;
        if (waveData instanceof SiOPMWavePCMTable) {
            _pcmTable = ((SiOPMWavePCMTable) waveData);
            pcm = _pcmTable._table[60];
        } else {
            _pcmTable = null;
            pcm = ((SiOPMWavePCMData) waveData);
        }
        if (pcm != null) _samplePitchShift = pcm.samplingPitch - 4416;
        operator.setPCMData(pcm);
    }

    /** set channel number (2nd argument of %) */
    @Override
    public void setChannelNumber(int channelNum) {
        registerMapChannel = channelNum;
    }

    /** set register */
    @Override
    public void setRegister(int addr, int data) {
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
    }

    /**
     * Set feedback(&#64;fb). Do nothing.
     *
     * @param fb  Feedback level. Ussualy in the range of 0-7.
     * @param fbc Feedback connection. Operator index which feeds back its output.
     */
    @Override
    public void setFeedBack(int fb, int fbc) {
    }

    /** Set parameters (&#64; command). */
    @Override
    public void setParameters(int[] param) {
        setSiOPMParameters(param[1], param[2], param[3], param[4], param[5],
                param[6], param[7], param[8], param[9], param[10],
                param[11], param[12], param[13], param[14]);
    }

    /** pgType and ptType (&#64;). call from SiMMLChannelSetting.selectTone() */
    @Override
    public void setType(int pgType, int ptType) {
        SiOPMWavePCMTable pcmTable = _table.getPCMData(pgType);
        if (pcmTable != null) {
            setWaveData(pcmTable);
        } else {
            _samplePitchShift = 0;
            operator.setPCMData(null);
        }
    }

    /** Attack rate */
    @Override
    public void setAllAttackRate(int ar) {
        operator._ar = ar;
    }

    /** Release rate (s) */
    @Override
    public void setAllReleaseRate(int rr) {
        operator._rr = rr;
    }

    // interfaces
    //

    /** pitch = (note &lt;&lt; 6) | (kf &amp; 63) [0,8191] */
    @Override
    public int getPitch() {
        return operator._pitchIndex + _samplePitchShift;
    }

    @Override
    public void setPitch(int p) {
        if (_pcmTable != null) {
            int note = p >> 6;
            SiOPMWavePCMData pcm = _pcmTable._table[note];
            if (pcm != null) {
                _samplePitchShift = pcm.samplingPitch - 4416; //69*64
                _sampleVolume = _pcmTable._volumeTable[note];
                _samplePan = _pcmTable._panTable[note];
            }
            operator.setPCMData(pcm);
        }
        operator.setPitchIndex(p - _samplePitchShift);
    }

    /** active operator index (i) */
    @Override
    public void setActiveOperatorIndex(int i) {
    }

    /** release rate (&#64;rr) */
    @Override
    public void setRr(int i) {
        operator._rr = i;
    }

    /** total level (&#64;tl) */
    @Override
    public void setTl(int i) {
        operator._tl = i;
    }

    /** fine multiple (&#64;ml) */
    @Override
    public void setFmul(int i) {
        operator.setFmul(i);
    }

    /** phase  (&#64;ph) */
    @Override
    public void setPhase(int i) {
        operator._keyon_phase = i;
    }

    /** detune (&#64;dt) */
    @Override
    public void setDetune(int i) {
        operator.setDetune(i);
    }

    /** fixed pitch (&#64;fx) */
    @Override
    public void setFixedPitch(int i) {
        operator.setFixedPitchIndex(i);
    }

    /** ssgec (&#64;se) */
    @Override
    public void setSsgec(int i) {
        operator.setSsgec(i);
    }

    /** envelop reset (&#64;er) */
    @Override
    public void setErst(boolean b) {
        operator.setErst(b);
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
        operator._tlOffset(tl);
    }

    // operation
    //

    /** Initialize. */
    @Override
    public void initialize(SiOPMChannelBase prev, int bufferIndex) {
        // initialize operators
        operator.initialize();
        _isNoteOn = false;
        registerMapType = 0;
        registerMapChannel = 0;
        _outPipe2 = _chip.getPipe(3, bufferIndex);
        _filterVriables2[0] = _filterVriables2[1] = _filterVriables2[2] = 0;
        _samplePitchShift = 0;
        _sampleVolume = 1;
        _samplePan = 0;

        // initialize sound channel
        super.initialize(prev, bufferIndex);
    }

    /** Reset. */
    @Override
    public void reset() {
        // reset all operators
        operator.reset();
        _isNoteOn = false;
        _isIdling = true;
    }

    /** Note on. */
    @Override
    public void noteOn() {
        // operator note on
        operator.noteOn();
        _isNoteOn = true;
        _isIdling = false;
        super.noteOn();
    }

    /** Note off. */
    @Override
    public void noteOff() {
        // operator note off
        operator.noteOff();
        _isNoteOn = false;
        super.noteOff();
    }

    /** Prepare buffering */
    @Override
    public void resetChannelBufferStatus() {
        _bufferIndex = 0;

        // check idling flag
        _isIdling = operator._eg_out > idlingThreshold && operator._eg_state != SiOPMOperator.EG_ATTACK;
    }

    /** Buffering */
    @Override
    public void buffer(int len) {
        if (_isIdling) {
            _nop(len);
        } else {
            _proc(len, operator, false, true);
        }
        _bufferIndex += len;
    }

    /** No process (default functor of _funcProcess). */
    @Override
    protected void _nop(int len) {
        // rotate output buffer
        _outPipe = _chip.getPipe(4, (_bufferIndex + len) & (_chip.getBufferLength() - 1));
        _outPipe2 = _chip.getPipe(3, (_bufferIndex + len) & (_chip.getBufferLength() - 1));
    }

    //
    // Internal uses
    //

    // process 1 operator
    //
    private void _proc(int len, SiOPMOperator ope, boolean mix, boolean finalOutput) {
        int t, l, i, n;
        int[] log = _table.logTable;
        int phase_filter = SiOPMTable.PHASE_FILTER;
        SLLint op = _outPipe;
        SLLint op2 = _outPipe2;
        SLLint bp = _outPipe;
        SLLint bp2 = _outPipe2;
        if (!mix) bp = bp2 = _chip.zeroBuffer;

        if (ope._pcm_channels == 1) {
            // MONAURAL
            //
            if (ope._pcm_endPoint > 0) {
                // buffering
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
                            if (ope._eg_level >= ope._eg_stateShiftLevel)
                                ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                        }
                        ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                        ope._eg_counter = (ope._eg_counter + 1) & 7;
                        ope._eg_timer += _eg_timer_initial;
                    }

                    // pg_update();
                    //
                    ope._phase += ope._phase_step;
                    t = ope._phase >>> PCM_waveFixedBits;
                    if (t >= ope._pcm_endPoint) {
                        if (ope._pcm_loopPoint == -1) {
                            ope._eg_shiftState(SiOPMOperator.EG_OFF);
                            ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                            for (; i < len; i++) {
                                op.i = 0;
                                op = op.next;
                            }
                            break;
                        } else {
                            t -= ope._pcm_endPoint - ope._pcm_loopPoint;
                            ope._phase -= (ope._pcm_endPoint - ope._pcm_loopPoint) << PCM_waveFixedBits;
                        }
                    }
                    l = ope._waveTable[t];
                    l += ope._eg_out + (_am_out >> ope._ams);

                    // output and increment pointers
                    //
                    op.i = log[l] + bp.i;
                    op = op.next;
                    bp = bp.next;
                }
            } else {
                // no operation
                for (i = 0; i < len; i++) {
                    op.i = bp.i;
                    op = op.next;
                    bp = bp.next;
                }
            }

            if (finalOutput) {
                // streaming
                if (!_mute) _mwrite(_outPipe, len);
                // update pointers
                _outPipe = op;
            }
        } else {
            // STEREO
            //
            if (ope._pcm_endPoint > 0) {
                // buffering
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
                            if (ope._eg_level >= ope._eg_stateShiftLevel)
                                ope._eg_shiftState(ope._eg_nextState[ope._eg_state]);
                        }
                        ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                        ope._eg_counter = (ope._eg_counter + 1) & 7;
                        ope._eg_timer += _eg_timer_initial;
                    }

                    // pg_update();
                    //
                    ope._phase += ope._phase_step;
                    t = ope._phase >>> PCM_waveFixedBits;
                    if (t >= ope._pcm_endPoint) {
                        if (ope._pcm_loopPoint == -1) {
                            ope._eg_shiftState(SiOPMOperator.EG_OFF);
                            ope._eg_out = (ope._eg_levelTable[ope._eg_level] + ope._eg_total_level) << 3;
                            for (; i < len; i++) {
                                op.i = 0;
                                op2.i = 0;
                                op = op.next;
                                op2 = op2.next;
                            }
                            break;
                        } else {
                            t -= ope._pcm_endPoint - ope._pcm_loopPoint;
                            ope._phase -= (ope._pcm_endPoint - ope._pcm_loopPoint) << PCM_waveFixedBits;
                        }
                    }

                    // output and increment pointers
                    //
                    // left
                    t <<= 1;
                    l = ope._waveTable[t];
                    l += ope._eg_out + (_am_out >> ope._ams);
                    op.i = bp.i;
                    op.i += log[l];
                    op = op.next;
                    bp = bp.next;
                    // right
                    t++;
                    l = ope._waveTable[t];
                    l += ope._eg_out + (_am_out >> ope._ams);
                    op2.i = bp2.i;
                    op2.i += log[l];
                    op2 = op2.next;
                    bp2 = bp2.next;
                }
            } else {
                // no operation
                for (i = 0; i < len; i++) {
                    op.i = bp.i;
                    op = op.next;
                    bp = bp.next;
                    op2.i = bp2.i;
                    op2 = op2.next;
                    bp2 = bp2.next;
                }
            }

            if (finalOutput) {
                // streaming
                if (!_mute) _swrite(_outPipe, _outPipe2, len);
                // update pointers
                _outPipe = op;
                _outPipe2 = op2;
            }
        }
    }

    // monaural stream writing with filtering
    private void _mwrite(SLLint input, int len) {
        int i;
        SiOPMStream stream;
        int vol = (int) (_sampleVolume * _chip.pcmVolume), pan = _pan + _samplePan;
        if (pan < 0) pan = 0;
        else if (pan > 128) pan = 128;

        if (_filterOn) _applySVFilter(input, len, null);
        if (_hasEffectSend) {
            for (i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
                if (_volumes[i] > 0) {
                    stream = _streams[i] != null ? _streams[i] : _chip.streamSlot[i];
                    if (stream != null) stream.write(input, _bufferIndex, len, _volumes[i] * vol, pan);
                }
            }
        } else {
            stream = _streams[0] != null ? _streams[0] : _chip.outputStream;
            stream.write(input, _bufferIndex, len, _volumes[0] * vol, pan);
        }
    }

    // stereo stream writing with filtering
    private void _swrite(SLLint inputL, SLLint inputR, int len) {
        int i;
        SiOPMStream stream;
        int vol = (int) (_sampleVolume * _chip.pcmVolume), pan = _pan + _samplePan;
        if (pan < 0) pan = 0;
        else if (pan > 128) pan = 128;

        if (_filterOn) {
            _applySVFilter(inputL, len, _filterVriables);
            _applySVFilter(inputR, len, _filterVriables2);
        }
        if (_hasEffectSend) {
            for (i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
                if (_volumes[i] > 0) {
                    stream = _streams[i] != null ? _streams[i] : _chip.streamSlot[i];
                    if (stream != null) stream.writeStereo(inputL, inputR, _bufferIndex, len, _volumes[i] * vol, pan);
                }
            }
        } else {
            stream = _streams[0] != null ? _streams[0] : _chip.outputStream;
            stream.writeStereo(inputL, inputR, _bufferIndex, len, _volumes[0] * vol, pan);
        }
    }
}
