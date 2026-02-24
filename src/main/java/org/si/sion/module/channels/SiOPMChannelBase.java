//
// SiOPM sound channel base class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module.channels;

import java.util.function.IntConsumer;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMStream;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWaveBase;
import org.si.utils.SLLint;


/**
 * SiOPM sound channel base class. <br/>
 * The SiOPM sound channels generate wave data and write it into streaming buffer.
 */
public class SiOPMChannelBase {

    // constants
    //

    /** standard output */
    public static final int OUTPUT_STANDARD = 0;
    /** overwrite pipe */
    public static final int OUTPUT_OVERWRITE = 1;
    /** add to pipe */
    public static final int OUTPUT_ADD = 2;

    /** no input from pipe */
    public static final int INPUT_ZERO = 0;
    /** input from pipe */
    public static final int INPUT_PIPE = 1;
    /** input from feedback */
    public static final int INPUT_FEEDBACK = 2;

    /** low pass filter */
    public static final int FILTER_LP = 0;
    /** band pass filter */
    public static final int FILTER_BP = 1;
    /** high pass filter */
    public static final int FILTER_HP = 2;

    // LPF envelop status
    private static final int EG_ATTACK = 0;
    private static final int EG_DECAY1 = 1;
    private static final int EG_DECAY2 = 2;
    private static final int EG_SUSTAIN = 3;
    private static final int EG_RELEASE = 4;
    private static final int EG_OFF = 5;

    // variables
    //

    /** table */
    protected SiOPMTable _table;
    /** chip */
    protected SiOPMModule _chip;
    /** functor to process */
    protected IntConsumer _funcProcess = this::_nop;
    /** note on flag */
    protected boolean _isNoteOn;

    // Pipe buffer
    /** buffering index */
    protected int _bufferIndex;
    /** input level */
    protected int _inputLevel;
    /** ringmod level */
    protected double _ringmodLevel;
    /** input level */
    protected int _inputMode;
    /** output mode */
    protected int _outputMode;
    /** in pipe */
    protected SLLint _inPipe;
    /** ringmod pipe */
    protected SLLint _ringPipe;
    /** base pipe */
    protected SLLint _basePipe;
    /** out pipe */
    protected SLLint _outPipe;

    // volume and stream
    /** stream */
    protected SiOPMStream[] _streams;
    /** volume */
    protected double[] _volumes;
    /** idling flag */
    protected boolean _isIdling;
    /** pan */
    protected int _pan;
    /** effect send flag */
    protected boolean _hasEffectSend;
    /** mute */
    protected boolean _mute;
    /** veocity table */
    protected int[] _veocityTable;
    /** expression table */
    protected int[] _expressionTable;

    // LPFilter
    /** filter switch */
    protected boolean _filterOn;
    /** filter type */
    protected int _filterType;
    /** cutoff frequency */
    protected int _cutoff;
    /** cutoff frequency */
    protected int _cutoff_offset;
    /** resonance */
    protected double _resonance;
    /** filter Variables */
    protected double[] _filterVriables;
    /** eg step residue */
    protected int _prevStepRemain;
    /** eg step */
    protected int _filter_eg_step;
    /** eg phase shift l. */
    protected int _filter_eg_next;
    /** eg direction */
    protected int _filter_eg_cutoff_inc;
    /** eg state */
    protected int _filter_eg_state;
    /** eg rate */
    protected int[] _filter_eg_time;
    /** eg level */
    protected int[] _filter_eg_cutoff;

    // Low frequency oscillator
    /** frequency ratio */
    protected int _freq_ratio;
    /** lfo switch */
    protected int _lfo_on;
    /** lfo timer */
    protected int _lfo_timer;
    /** lfo timer step */
    protected int _lfo_timer_step;
    /** lfo step buffer */
    protected int _lfo_timer_step_;
    /** lfo phase */
    protected int _lfo_phase;
    /** lfo wave table */
    protected int[] _lfo_waveTable;
    /** lfo wave shape */
    protected int _lfo_waveShape;

    // constructor
    //

    /** finalructor @param chip Managing SiOPMModule. */
    public SiOPMChannelBase(SiOPMModule chip) {
        _table = SiOPMTable.getInstance();
        _chip = chip;
        _isFree = true;

        _filterVriables = new double[3];
        _streams = new SiOPMStream[SiOPMModule.STREAM_SEND_SIZE];
        _volumes = new double[SiOPMModule.STREAM_SEND_SIZE];
        _filter_eg_time = new int[6];
        _filter_eg_cutoff = new int[6];
    }

    // interfaces
    //

    /** Set by SiOPMChannelParam. */
    public void setSiOPMChannelParam(SiOPMChannelParam param, boolean withVolume, boolean withModulation) {
    }

    /** Get SiOPMChannelParam. */
    public void getSiOPMChannelParam(SiOPMChannelParam param) {
    }

    /** Set wave data. */
    public void setWaveData(SiOPMWaveBase waveData) {
    }

    /** channel number (2nd argument of %) */
    public void setChannelNumber(int channelNum) {
    }

    /** algorism (&#64;al) */
    public void setAlgorism(int cnt, int alg) {
    }

    /** feedback (&#64;fb) */
    public void setFeedBack(int fb, int fbc) {
    }

    /** parameters (&#64; call from SiMMLTrack._setChannelParameters()) */
    public void setParameters(int[] param) {
    }

    /** pgType and ptType (&#64; call from SiMMLChannelSetting.selectTone()/initializeTone()) */
    public void setType(int pgType, int ptType) {
    }

    /** Attack rate */
    public void setAllAttackRate(int ar) {
    }

    /** Release rate (s) */
    public void setAllReleaseRate(int rr) {
    }

    /** Master volume (0-128) */
    public int getMasterVolume() {
        return (int) (_volumes[0] * 128);
    }

    public void setMasterVolume(int v) {
        v = (v < 0) ? 0 : Math.min(v, 128);
        _volumes[0] = v * 0.0078125;     // 0.0078125 = 1/128
    }

    /**
     * Pan (-64-64 left=-64, center=0, right=64).<br/>
     * [left volume]  = cos((pan+64)/128*PI*0.5) * volume;<br/>
     * [right volume] = sin((pan+64)/128*PI*0.5) * volume;
     */
    public int getPan() {
        return _pan - 64;
    }

    public void setPan(int p) {
        _pan = (p < -64) ? 0 : (p > 64) ? 128 : (p + 64);
    }

    /** Mute */
    public boolean getMute() {
        return _mute;
    }

    public void setMute(boolean m) {
        _mute = m;
    }

    /** active operator index (i). */
    public void setActiveOperatorIndex(int i) {
    }

    /** Release rate (&#64;rr) */
    public void setRr(int r) {
    }

    /** total level (&#64;tl) */
    public void setTl(int i) {
    }

    /** fine multiple (&#64;ml) */
    public void setFmul(int i) {
    }

    /** phase (&#64;ph) */
    public void setPhase(int i) {
    }

    /** detune (&#64;dt) */
    public void setDetune(int i) {
    }

    /** fixed pitch (&#64;fx) */
    public void setFixedPitch(int i) {
    }

    /** ssgec (&#64;se) */
    public void setSsgec(int i) {
    }

    /** envelop reset (&#64;er) */
    public void setErst(boolean b) {
    }

    /** pitch */
    public int getPitch() {
        return 0;
    }

    public void setPitch(int i) {
    }

    /** buffer index */
    public int getBufferIndex() {
        return _bufferIndex;
    }

    /** is this channel note on ? */
    public boolean isNoteOn() {
        return _isNoteOn;
    }

    /** Is idling ? */
    public boolean isIdling() {
        return _isIdling;
    }

    /** Is filter active ? */
    public boolean isFilterActive() {
        return _filterOn;
    }

    /** filter mode */
    public int getFilterType() {
        return _filterType;
    }

    public void setFilterType(int mode) {
        _filterType = (mode < 0 || mode > 2) ? 0 : mode;
    }

    // volume control
    //

    /**
     * set all stream send levels by Vector.&lt;int&gt;.
     *
     * @param param Vector.&lt;int&gt;(8) of all volumes[0-128].
     */
    public void setAllStreamSendLevels(int[] param) {
        int i, imax = SiOPMModule.STREAM_SEND_SIZE, v;
        for (i = 0; i < imax; i++) {
            v = param[i];
            _volumes[i] = (v != Integer.MIN_VALUE) ? (v * 0.0078125) : 0;
        }
        for (_hasEffectSend = false, i = 1; i < imax; i++) {
            if (_volumes[i] > 0) _hasEffectSend = true;
        }
    }

    /**
     * set stream buffer.
     *
     * @param streamNum stream number[0-7]. The streamNum of 0 means master stream.
     * @param stream    stream buffer instance. Set null to ((default) set).
     */
    public void setStreamBuffer(int streamNum, SiOPMStream stream) {
        _streams[streamNum] = stream;
    }

    /**
     * set stream send.
     *
     * @param streamNum stream number[0-7]. The streamNum of 0 means master volume.
     * @param volume    send level[0-1].
     */
    public void setStreamSend(int streamNum, double volume) {
        _volumes[streamNum] = volume;
        if (streamNum == 0) return;
        if (volume > 0) _hasEffectSend = true;
        else {
            int i, imax = SiOPMModule.STREAM_SEND_SIZE;
            for (_hasEffectSend = false, i = 1; i < imax; i++) {
                if (_volumes[i] > 0) _hasEffectSend = true;
            }
        }
    }

    /**
     * get stream send.
     *
     * @param streamNum stream number[0-7]. The streamNum of 0 means master volume.
     * @return send level[0-1].
     */
    public double getStreamSend(int streamNum) {
        return _volumes[streamNum];
    }

    /** offset volume, controlled by SiMMLTrack. */
    public void offsetVolume(int expression, int velocity) {
    }

    // LFO control
    //

    /** set chip "PSEUDO" frequency ratio by [%] (&#64;clock). */
    public void setFrequencyRatio(int ratio) {
        _freq_ratio = ratio;
    }

    /**
     * initialize LFO (&#64;lfo).
     *
     * @param waveform        waveform number, -1 to set customized wave table
     * @param customWaveTable customized wave table, the length instanceof 256 and the values are limited in the range of 0-255. This argument instanceof available when waveform=-1.
     */
    public void initializeLFO(int waveform, int[] customWaveTable /* = null */) {
        if (waveform == -1 && customWaveTable != null && customWaveTable.length == 256) {
            _lfo_waveShape = -1;
            _lfo_waveTable = customWaveTable;
        } else {
            _lfo_waveShape = (0 <= waveform && waveform <= SiOPMTable.LFO_WAVE_MAX) ? waveform : SiOPMTable.LFO_WAVE_TRIANGLE;
            _lfo_waveTable = _table.lfo_waveTables[_lfo_waveShape];
        }
        _lfo_timer = 1;
        _lfo_timer_step_ = _lfo_timer_step = 0;
        _lfo_phase = 0;
    }

    /** set LFO cycle time (&#64;lfo). */
    public void setLFOCycleTime(double ms) {
        _lfo_timer = 0;
        // 0.17294117647058824 = 44100/(1000*255)
        _lfo_timer_step_ = _lfo_timer_step = (int) (SiOPMTable.LFO_TIMER_INITIAL / (ms * 0.17294117647058824)) << _table.sampleRatePitchShift;

        //set OPM LFO frequency
        //_lfo_timer = 0;
        //_lfo_timer_step_ = _lfo_timer_step = _table.lfo_timerSteps[freq & 255];
    }

    /** amplitude modulation (ma) */
    public void setAmplitudeModulation(int depth) {
    }

    /** pitch modulation (mp) */
    public void setPitchModulation(int depth) {
    }

    // filter control
    //

    /** Filter activation */
    public void activateFilter(boolean b) {
        _filterOn = b;
    }

    /**
     * SVFilter envelop (&#64;f).
     *
     * @param cutoff    initial cutoff (0-128).
     * @param resonance resonance (0-9).
     * @param ar        attack rate (0-63).
     * @param dr1       decay rate 1 (0-63).
     * @param dr2       decay rate 2 (0-63).
     * @param rr        release rate (0-63).
     * @param dc1       decay cutoff level 1 (0-128).
     * @param dc2       decay cutoff level 2 (0-128).
     * @param sc        sustain cutoff level (0-128).
     * @param rc        release cutoff level (0-128).
     */
    public void setSVFilter(int cutoff, int resonance, int ar, int dr1, int dr2, int rr, int dc1, int dc2, int sc, int rc) {
        _filter_eg_cutoff[EG_ATTACK] = (cutoff < 0) ? 0 : Math.min(cutoff, 128);
        _filter_eg_cutoff[EG_DECAY1] = (dc1 < 0) ? 0 : Math.min(dc1, 128);
        _filter_eg_cutoff[EG_DECAY2] = (dc2 < 0) ? 0 : Math.min(dc2, 128);
        _filter_eg_cutoff[EG_SUSTAIN] = (sc < 0) ? 0 : Math.min(sc, 128);
        _filter_eg_cutoff[EG_RELEASE] = 0;
        _filter_eg_cutoff[EG_OFF] = (rc < 0) ? 0 : Math.min(rc, 128);
        _filter_eg_time[EG_ATTACK] = _table.filter_eg_rate[ar & 63];
        _filter_eg_time[EG_DECAY1] = _table.filter_eg_rate[dr1 & 63];
        _filter_eg_time[EG_DECAY2] = _table.filter_eg_rate[dr2 & 63];
        _filter_eg_time[EG_SUSTAIN] = Integer.MAX_VALUE;
        _filter_eg_time[EG_RELEASE] = _table.filter_eg_rate[rr & 63];
        _filter_eg_time[EG_OFF] = Integer.MAX_VALUE;

        int res = (resonance < 0) ? 0 : Math.min(resonance, 9);
        _resonance = (1 << (9 - res)) * 0.001953125;   // 0.001953125=1/512

        _filterOn = (cutoff < 128 || resonance > 0 || ar > 0 || rr > 0);
    }

    /** set SVFilter resonance (0-9). */
    public void setFilterResonance(int i) {
        int res = (i < 0) ? 0 : Math.min(i, 9);
        _resonance = (1 << (9 - res)) * 0.001953125;   // 0.001953125=1/512
    }

    /** set SVFilter cutoff frequency (0-128). */
    public void setFilterOffset(int i) {
        _cutoff_offset = i - 128;
    }

    // connection control
    //

    /**
     * Set input pipe (&#64;i).
     *
     * @param level     Input level. The value for a standard FM sound module instanceof 5.
     * @param pipeIndex Input pipe index (0-3).
     */
    public void setInput(int level, int pipeIndex) {
        // pipe index
        pipeIndex &= 3;

        // set pipe
        if (level > 0) {
            _inPipe = _chip.getPipe(pipeIndex, _bufferIndex);
            _inputMode = INPUT_PIPE;
            _inputLevel = level + 10;
        } else {
            _inPipe = _chip.zeroBuffer;
            _inputMode = INPUT_ZERO;
            _inputLevel = 0;
        }
    }

    /**
     * Set ring modulation pipe (&#64;r).
     *
     * @param level Input level(0-8).
     * @param pipeIndex Input pipe index (0-3).
     */
    public void setRingModulation(int level, int pipeIndex) {
        int i;

        // pipe index
        pipeIndex &= 3;

        // ring modulation level
        _ringmodLevel = level * 4 / (double) (1 << SiOPMTable.LOG_VOLUME_BITS);

        // set pipe
        _ringPipe = (level > 0) ? _chip.getPipe(pipeIndex, _bufferIndex) : null;
    }

    /**
     * Set output pipe (&#64;o).
     *
     * @param outputMode Output mode. 0=standard stereo out, 1=overwrite pipe. 2=add pipe.
     * @param pipeIndex  Output stream/pipe index (0-3).
     */
    public void setOutput(int outputMode, int pipeIndex) {
        int i;
        boolean flagAdd;

        // pipe index
        pipeIndex &= 3;

        // set pipe
        if (outputMode == OUTPUT_STANDARD) {
            pipeIndex = 4;      // pipe[4] is used.
            flagAdd = false;    // ovewrite mode
        } else {
            flagAdd = (outputMode == OUTPUT_ADD);  // ovewrite/additional mode
        }

        // output mode
        _outputMode = outputMode;

        // set output pipe
        _outPipe = _chip.getPipe(pipeIndex, _bufferIndex);

        // set base pipe
        _basePipe = (flagAdd) ? (_outPipe) : (_chip.zeroBuffer);
    }

    /**
     * set velocity and expression tables
     *
     * @param vtable volume table (length = 513)
     * @param xtable expression table (length = 513)
     */
    public void setVolumeTables(int[] vtable, int[] xtable) {
        _veocityTable = vtable;
        _expressionTable = xtable;
    }

    // operations
    //

    /** Initialize. */
    public void initialize(SiOPMChannelBase prev, int bufferIndex) {
        // volume
        int i, imax = SiOPMModule.STREAM_SEND_SIZE;
        if (prev != null) {
            for (i = 0; i < imax; i++) {
                _volumes[i] = prev._volumes[i];
                _streams[i] = prev._streams[i];
            }
            _pan = prev._pan;
            _hasEffectSend = prev._hasEffectSend;
            _mute = prev._mute;
            _veocityTable = prev._veocityTable;
            _expressionTable = prev._expressionTable;
        } else {
            _volumes[0] = 0.5;
            _streams[0] = null;
            for (i = 1; i < imax; i++) {
                _volumes[i] = 0;
                _streams[i] = null;
            }
            _pan = 64;
            _hasEffectSend = false;
            _mute = false;
            _veocityTable = _table.eg_tlTableLine;
            _expressionTable = _table.eg_tlTableLine;
        }

        // buffer index
        _isNoteOn = false;
        _isIdling = true;
        _bufferIndex = bufferIndex;

        // LFO
        initializeLFO(SiOPMTable.LFO_WAVE_TRIANGLE, null);
        setLFOCycleTime(333);
        setFrequencyRatio(100);

        // Connection
        setInput(0, 0);
        setRingModulation(0, 0);
        setOutput(OUTPUT_STANDARD, 0);

        // LPFilter
        _filterVriables[0] = _filterVriables[1] = _filterVriables[2] = 0;
        _cutoff_offset = 0;
        _filterType = FILTER_LP;
        setSVFilter(128, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        shiftSVFilterState(EG_OFF);
    }

    /** Reset */
    public void reset() {
        _isNoteOn = false;
        _isIdling = true;
    }

    /** Note on */
    public void noteOn() {
        _lfo_phase = 0;     // reset lfo phase
        if (_filterOn) {    // reset envelop
            resetSVFilterState();
            shiftSVFilterState(EG_ATTACK);
        }
        _isNoteOn = true;
    }

    /** Note off */
    public void noteOff() {
        if (_filterOn) {    // shift filters status
            shiftSVFilterState(EG_RELEASE);
        }
        _isNoteOn = false;
    }

    /** set register */
    public void setRegister(int addr, int data) {

    }

    // processing
    //

    /** reset channel buffering status */
    public void resetChannelBufferStatus() {
        _bufferIndex = 0;
    }

    /** Buffering */
    public void buffer(int len) {
        int i;
        SiOPMStream stream;

        if (_isIdling) {
            // idling process
            _nop(len);
        } else {
            // preserve _outPipe
            SLLint monoOut = _outPipe;

            // processing (update _outPipe inside)
            _funcProcess.accept(len);

            // ring modulation / LPFilter
            if (_ringPipe != null) _applyRingModulation(monoOut, len);
            if (_filterOn) _applySVFilter(monoOut, len, null);

            // standard output
            if (_outputMode == OUTPUT_STANDARD && !_mute) {
                if (_hasEffectSend) {
                    for (i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
                        if (_volumes[i] > 0) {
                            stream = (_streams[i] != null) ? _streams[i] : _chip.streamSlot[i];
                            if (stream != null) stream.write(monoOut, _bufferIndex, len, _volumes[i], _pan);
                        }
                    }
                } else {
                    stream = (_streams[0] != null) ? _streams[0] : _chip.outputStream;
                    stream.write(monoOut, _bufferIndex, len, _volumes[0], _pan);
                }
            }
        }

        // update buffer index
        _bufferIndex += len;
    }

    /** Buffering without processnig */
    public void nop(int len) {
        _nop(len);
        _bufferIndex += len;
    }

    /** ring modulation */
    protected void _applyRingModulation(SLLint pointer, int len) {
        int i;
        SLLint rp = _ringPipe;
        for (i = 0; i < len; i++) {
            pointer.i *= (int) (rp.i * _ringmodLevel);
            rp = rp.next;
            pointer = pointer.next;
        }
        _ringPipe = rp;
    }

    /** state variable filter */
    protected void _applySVFilter(SLLint pointer, int len, double[] variables /* = null */) {
        int i, imax, step, out;
        double cut, fb;

        // initialize
        if (variables == null) variables = _filterVriables;
        out = _cutoff + _cutoff_offset;
        if (out < 0) out = 0;
        else if (out > 128) out = 128;
        cut = _table.filter_cutoffTable[out];
        fb = _resonance;// * _table.filter_feedbackTable[out];

        // previous setting
        step = _prevStepRemain;

        while (len >= step) {
            // processing
            for (i = 0; i < step; i++) {
                variables[2] = (double) (pointer.i) - variables[0] - variables[1] * fb;
                variables[1] += variables[2] * cut;
                variables[0] += variables[1] * cut;
                pointer.i = (int) (variables[_filterType]);
                pointer = pointer.next;
            }
            len -= step;

            // change cutoff and shift state
            _cutoff += _filter_eg_cutoff_inc;
            out = _cutoff + _cutoff_offset;
            if (out < 0) out = 0;
            else if (out > 128) out = 128;
            cut = _table.filter_cutoffTable[out];
            fb = _resonance;// * _table.filter_feedbackTable[out];
            if (_cutoff == _filter_eg_next) shiftSVFilterState(_filter_eg_state + 1);

            // next step
            step = _filter_eg_step;
        }

        // process remains
        for (i = 0; i < len; i++) {
            variables[2] = (double) (pointer.i) - variables[0] - variables[1] * fb;
            variables[1] += variables[2] * cut;
            variables[0] += variables[1] * cut;
            pointer.i = (int) (variables[_filterType]);
            pointer = pointer.next;
        }

        // next setting
        _prevStepRemain = _filter_eg_step - len;
    }

    /** reset SVFilter */
    protected void resetSVFilterState() {
        _cutoff = _filter_eg_cutoff[EG_ATTACK];
    }

    /** shift SVFilter state */
    protected void shiftSVFilterState(int state) {
        switch (state) {
            case EG_ATTACK:
                if (__shift(state)) break;
                state++;
                // fail through
            case EG_DECAY1:
                if (__shift(state)) break;
                state++;
                // fail through
            case EG_DECAY2:
                if (__shift(state)) break;
                state++;
                // fail through
            case EG_SUSTAIN:
                // catch all
                _filter_eg_state = EG_SUSTAIN;
                _filter_eg_step = Integer.MAX_VALUE;
                _filter_eg_next = _cutoff + 1;
                _filter_eg_cutoff_inc = 0;
                break;
            case EG_RELEASE:
                if (__shift(state)) break;
                state++;
                // fail through
            case EG_OFF:
                // catch all
                _filter_eg_state = EG_OFF;
                _filter_eg_step = Integer.MAX_VALUE;
                _filter_eg_next = _cutoff + 1;
                _filter_eg_cutoff_inc = 0;
                break;
        }
        _prevStepRemain = _filter_eg_step;
    }

    boolean __shift (int state) {
        if (_filter_eg_time[state] == 0) return false;
        _filter_eg_state = state;
        _filter_eg_step = _filter_eg_time[state];
        _filter_eg_next = _filter_eg_cutoff[state + 1];
        _filter_eg_cutoff_inc = (_cutoff < _filter_eg_next) ? 1 : -1;
        return (_cutoff != _filter_eg_next);
    }

    /** No process (default functor of _funcProcess). */
    protected void _nop(int len) {
        int i;
        SLLint p;

        // rotate output buffer
        if (_outputMode == OUTPUT_STANDARD) {
            _outPipe = _chip.getPipe(4, (_bufferIndex + len) & (_chip.getBufferLength() - 1));
        } else {
            for (p = _outPipe, i = 0; i < len; i++) p = p.next;
            _outPipe = p;
            _basePipe = (_outputMode == OUTPUT_ADD) ? p : _chip.zeroBuffer;
        }

        // rotate input buffer when connected by @i
        if (_inputMode == INPUT_PIPE) {
            for (p = _inPipe, i = 0; i < len; i++) p = p.next;
            _inPipe = p;
        }

        // rotate ring buffer
        if (_ringPipe != null) {
            for (p = _ringPipe, i = 0; i < len; i++) p = p.next;
            _ringPipe = p;
        }
    }

    // for channel manager operation [internal use]
    //

    /** DLL of channels */
    boolean _isFree = true;
    /** DLL of channels */
    int _channelType = -1;
    /** DLL of channels */
    SiOPMChannelBase _next = null;
    /** DLL of channels */
    SiOPMChannelBase _prev = null;

    /** channel type */
    public int getChannelType() {
        return _channelType;
    }
}
