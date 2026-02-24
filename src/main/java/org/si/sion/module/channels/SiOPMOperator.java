//
// SiOPM operator class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module.channels;

import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWaveTable;
import org.si.utils.SLLint;


/**
 * SiOPM operator class.
 * This operator based on the OPM emulation of MAME, but It's extended in below points,<br/>
 * 1) You can set the phase offest of pulse generator. <br/>
 * 2) You can select the wave form from some wave tables (see class SiOPMTable).<br/>
 * 3) You can set the key scale level.<br/>
 * 4) You can fix the pitch.<br/>
 * 5) You can set the ssgec in OPNA.<br/>
 */
public class SiOPMOperator {

    // constants
    //

    // State of envelop generator.
    public static final int EG_ATTACK = 0;
    public static final int EG_DECAY = 1;
    public static final int EG_SUSTAIN = 2;
    public static final int EG_RELEASE = 3;
    public static final int EG_OFF = 4;

    // waveFixedBits for PCM
    private static final int PCM_waveFixedBits = 11;

    // variables
    //
    // [ IMPORTANT NOTE ] 
    // The access levels of all valiables are set as "internal". 
    // The SiOPMChannelFM accesses these valiables directly only from the wave processing functions to make it faster.
    // Never access these valiables in other classes reason for the maintenances.
    //

    /** @private table */
    SiOPMTable _table;
    /** @private chip */
    SiOPMModule _chip;

    // FM module parameters
    /** @private Attack rate [0,63] */
    int _ar;
    /** @private Decay rate [0,63] */
    int _dr;
    /** @private Sustain rate [0,63] */
    int _sr;
    /** @private Release rate [0,63] */
    int _rr;
    /** @private Sustain level [0,15] */
    int _sl;
    /** @private Total level [0,127] */
    int _tl;
    /** @private Key scaling rate = 5-ks [5,2] */
    int _ks;
    /** @private Key scaling level [0,3] */
    int _ksl;
    /** @private _multiple = (mul) ? (mul&lt;&lt;7) : 64; [64,128,256,384,512...] */
    int _multiple;
    /** @private dt1 [0,7]. */
    int _dt1;
    /** @private dt2 [0,3]. This value instanceof linked with _pitchIndexShift */
    int _dt2;
    /** @private Amp modulation shift [16,0] */
    int _ams;
    /** @private Key code = oct&lt;&lt;4 + note [0,127] */
    int _kc;
    /** @private SSG type envelop control */
    int _ssg_type;
    /** @private Mute [0/SiOPMTable.ENV_BOTTOM] */
    int _mute;
    /** @priavet Envelop reset on attack */
    boolean _erst;

    // pulse generator
    /** @private pulse generator type */
    int _pgType;
    /** @private pitch table type */
    int _ptType;
    /** @private wave table */
    int[] _waveTable;
    /** @private phase shift */
    int _waveFixedBits;
    /** @private phase step shift */
    int _wavePhaseStepShift;
    /** @private pitch table */
    int[] _pitchTable;
    /** @private pitch table index filter */
    int _pitchTableFilter;
    /** @private phase */
    int _phase;
    /** @private phase step */
    int _phase_step;
    /** @private keyOn phase. -1 sets no phase reset. */
    int _keyon_phase;
    /** @private pitch fixed */
    boolean _pitchFixed;
    /** @private dt1 table */
    int[] _dt1Table;

    /** @private pitch index = note * 64 + key fraction */
    int _pitchIndex;
    /** @private pitch index shift. This value instanceof linked with dt2 and detune. */
    int _pitchIndexShift;
    /** @private pitch index shift by pitch modulation. This value instanceof linked with dt2. */
    int _pitchIndexShift2;
    /** @private frequency modulation left-shift. 15 for FM, fb+6 for feedback. */
    int _fmShift;

    // envelop generator
    /** @private State [EG_ATTACK, EG_DECAY, EG_SUSTAIN, EG_RELEASE, EG_OFF] */
    int _eg_state;
    /** @private Envelop generator updating timer, initialized (2047 * 3) &lt;&lt; CLOCK_RATIO_BITS. */
    int _eg_timer;
    /** @private Timer stepping by samples */
    int _eg_timer_step;
    /** @private Counter rounded on 8. */
    int _eg_counter;
    /** @private Internal sustain level [0,SiOPMTable.ENV_BOTTOM] */
    int _eg_sustain_level;
    /** @private Internal total level [0,1024] = ((tl + f(kc, ksl)) &lt;&lt; 3) + _eg_tl_offset + 192. */
    int _eg_total_level;
    /** @private Internal total level offset by volume [-192,832] */
    int _eg_tl_offset;
    /** @private Internal key scaling rate = _kc >> _ks [0,32] */
    int _eg_key_scale_rate;
    /** @private Internal key scaling level right shift = _ksl[0,1,2,3]->[8,2,1,0] */
    int _eg_key_scale_level_rshift;
    /** @private Envelop generator level [0,1024] */
    int _eg_level;
    /** @private Envelop generator output [0,1024&lt;&lt;3] */
    int _eg_out;
    /** @private SSG envelop control ar switch */
    int _eg_ssgec_ar;
    /** @private SSG envelop control state */
    int _eg_ssgec_state;

    /** @private Increment table picked up from _eg_incTables or _eg_incTablesAtt. */
    int[] _eg_incTable;
    /** @private The level to shift the state to next. */
    int _eg_stateShiftLevel;
    /** @private Next status list */
    int[] _eg_nextState;
    /** @private _eg_level converter */
    int[] _eg_levelTable;
    // Next status table
    private final int[][] _table_nextState = {
            // EG_ATTACK,  EG_DECAY,   EG_SUSTAIN, EG_RELEASE, EG_OFF
            {EG_DECAY, EG_SUSTAIN, EG_OFF, EG_OFF, EG_OFF}, // normal
            {EG_DECAY, EG_SUSTAIN, EG_ATTACK, EG_OFF, EG_OFF}  // ssgev
    };

    // pipes
    /** @private flag that instanceof final carrior. */
    boolean _final;
    /** @private modulator output */
    SLLint _inPipe;
    /** @private base */
    SLLint _basePipe;
    /** @private output */
    SLLint _outPipe;
    /** @private feed back */
    SLLint _feedPipe;

    // for PCM wave
    /** @private channel count */
    int _pcm_channels;
    /** @private start point */
    int _pcm_startPoint;
    /** @private end point */
    int _pcm_endPoint;
    /** @private loop point */
    int _pcm_loopPoint;

    // properties (fm parameters)
    //

    /** Attack rate [0,63] */
    public void setAr(int i) {
        _ar = i & 63;
        _eg_ssgec_ar = (_ssg_type == 8 || _ssg_type == 12) ? ((_ar >= 56) ? 1 : 0) : ((_ar >= 60) ? 1 : 0);
    }

    /** Decay rate [0,63] */
    public void setDr(int i) {
        _dr = i & 63;
    }

    /** Sustain rate [0,63] */
    public void setSr(int i) {
        _sr = i & 63;
    }

    /** Release rate [0,63] */
    public void setRr(int i) {
        _rr = i & 63;
    }

    /** Sustain level [0,15] */
    public void setSl(int i) {
        _sl = i & 15;
        _eg_sustain_level = _table.eg_slTable[i];
    }

    /** Total level [0,127] */
    public void setTl(int i) {
        _tl = (i < 0) ? 0 : Math.min(i, 127);
        _updateTotalLevel();
    }

    /** Key scaling rate [0,3] */
    public void setKs(int i) {
        _ks = 5 - (i & 3);
        _eg_key_scale_rate = _kc >> _ks;
    }

    /** multiple [0,15] */
    public void setMul(int m) {
        m &= 15;
        _multiple = (m != 0) ? (m << 7) : 64;
        _updatePitch();
    }

    /** dt1 [0-7] */
    public void setDt1(int d) {
        _dt1 = d & 7;
        _dt1Table = _table.dt1Table[_dt1];
        _updatePitch();
    }

    /** dt2 [0-3] */
    public void setDt2(int d) {
        _dt2 = d & 3;
        _pitchIndexShift = _table.dt2Table[_dt2];
        _updatePitch();
    }

    /** amplitude modulation enable [t/f] */
    public void setAme(boolean b) {
        _ams = (b) ? 2 : 16;
    }

    /** amplitude modulation shift [t/f] */
    public void setAms(int s) {
        _ams = (s != 0) ? (3 - s) : 16;
    }

    /** Key scaling level [0,3] */
    public void setKsl(int i) {
        _ksl = i;
        // [0,1,2,3]->[8,4,3,2]
        _eg_key_scale_level_rshift = (i == 0) ? 8 : (5 - i);
        _updateTotalLevel();
    }

    /** SSG type envelop control */
    public void setSsgec(int i) {
        if (i > 7) {
            _eg_nextState = _table_nextState[1];
            _ssg_type = i;
            if (_ssg_type > 17) _ssg_type = 9;
        } else {
            _eg_nextState = _table_nextState[0];
            _ssg_type = 0;
        }
    }

    /** Mute */
    public void setMute(boolean b) {
        _mute = (b) ? SiOPMTable.ENV_BOTTOM : 0;
        _updateTotalLevel();
    }

    /** Envelop reset on attack */
    public void setErst(boolean b) {
        _erst = b;
    }


    public int getAr() {
        return _ar;
    }

    public int getDr() {
        return _dr;
    }

    public int getSr() {
        return _sr;
    }

    public int getRr() {
        return _rr;
    }

    public int getSl() {
        return _sl;
    }

    public int getTl() {
        return _tl;
    }

    public int getKs() {
        return 5 - _ks;
    }

    public int getMul() {
        return (_multiple >> 7);
    }

    public int getDt1() {
        return _dt1;
    }

    public int getDt2() {
        return _dt2;
    }

    public boolean getAme() {
        return (_ams != 16);
    }

    public int getAms() {
        return (_ams == 16) ? 0 : (3 - _ams);
    }

    public int getKsl() {
        return _ksl;
    }

    public int getSsgec() {
        return _ssg_type;
    }

    public boolean getMute() {
        return (_mute != 0);
    }

    public boolean getErst() {
        return _erst;
    }

    // properties (other fm parameters)
    //

    /** Key code [0,127] */
    public void setKc(int i) {
        if (_pitchFixed) return;
        _updateKC(i & 127);
        _pitchIndex = ((_kc - (_kc >> 2)) << 6) | (_pitchIndex & 63);
        _updatePitch();
    }

    /** key fraction [0-63] */
    public void setKf(int f) {
        _pitchIndex = (_pitchIndex & 0xffc0) | (f & 63);
        _updatePitch();
    }

    /** F-Number for OPNA. This property resets kf,dt2 and detune. */
    public void setFnum(int f) {
        // dishonest implement.
        _updateKC((f >> 7) & 127);
        _dt2 = 0;
        _pitchIndex = 0;
        _pitchIndexShift = 0;
        _updatePhaseStep((f & 2047) << ((f >> 11) & 7));
    }

    // Get status, but all of them cannot be read.
    public int getKc() {
        return _kc;
    }

    public int getKf() {
        return (_pitchIndex & 63);
    }

    public boolean getPitchFixed() {
        return _pitchFixed;
    }

    // properties (pTSS)
    //

    /** Fixed pitch index. 0 means fixed off. */
    public void setFixedPitchIndex(int i) {
        if (i > 0) {
            _pitchIndex = i;
            _updateKC(_table.nnToKC[(i >> 6) & 127]);
            _updatePitch();
            _pitchFixed = true;
        } else {
            _pitchFixed = false;
        }
    }

    /** pitchIndex = (note &lt;&lt; 6) | (kf &amp; 63) [0,8191] */
    public void setPitchIndex(int i) {
        if (_pitchFixed) return;
        _pitchIndex = i;
        _updateKC(_table.nnToKC[(i >> 6) & 127]);
        _updatePitch();
    }

    /** Detune for pTSS. 1 halftone divides into 64 steps. This property resets dt2. */
    public void setDetune(int d) {
        _dt2 = 0;
        _pitchIndexShift = d;
        _updatePitch();
    }

    /** Detune for pitch modulation. This instanceof independent value. */
    public void setDetune2(int d) {
        _pitchIndexShift2 = d;
        _updatePitch();
    }

    /** Fine multiple for pTSS. 128=x1. */
    public void setFmul(int m) {
        _multiple = m;
        _updatePitch();
    }

    /** Phase at keyOn [-1-255]. similar with pTSS. The value of 255 sets no phase reset, -1 sets randamize. */
    public void setKeyOnPhase(int p) {
        if (p == 255) _keyon_phase = -2;
        else if (p == -1) _keyon_phase = -1;
        else _keyon_phase = (p & 255) << (SiOPMTable.PHASE_BITS - 8);
    }

    /** Pulse generator type. */
    public void setPgType(int n) {
        _pgType = n & SiOPMTable.PG_FILTER;
        SiOPMWaveTable waveTable = _table.getWaveTable(_pgType);
        _waveTable = waveTable.wavelet;
        _waveFixedBits = waveTable.fixedBits;
    }

    /** Pitch table type. */
    public void setPtType(int n) {
        _ptType = n;
        _wavePhaseStepShift = (SiOPMTable.PHASE_BITS - _waveFixedBits) & _table.phaseStepShiftFilter[n];
        _pitchTable = _table.pitchTable[n];
        _pitchTableFilter = _pitchTable.length - 1;
    }

    /** Frequency modulation level. 15 instanceof standard modulation. */
    public void setModLevel(int m) {
        _fmShift = (m != 0) ? (m + 10) : 0;
    }

    public int getPitchIndex() {
        return _pitchIndex;
    }

    public int getDetune() {
        return _pitchIndexShift;
    }

    public int getDetune2() {
        return _pitchIndexShift2;
    }

    public int getFmul() {
        return _multiple;
    }

    public int getKeyOnPhase() {
        return (_keyon_phase >= 0) ? (_keyon_phase >> (SiOPMTable.PHASE_BITS - 8)) : (_keyon_phase == -1) ? -1 : 255;
    }

    public int getPgType() {
        return _pgType;
    }

    public int getModLevel() {
        return (_fmShift > 10) ? (_fmShift - 10) : 0;
    }

    /** tl offset [832,-192]. ((expression) controlled) and velocity. */
    void _tlOffset(int i) {
        _eg_tl_offset = i;
        _updateTotalLevel();
    }

    public String toString() {
        String str = "SiOPMOperator : ";
        str += _pgType + "/";
        str += _ar + "/";
        str += _dr + "/";
        str += _sr + "/";
        str += _rr + "/";
        str += _sl + "/";
        str += _tl + "/";
        str += _ks + "/";
        str += _ksl + "/";
        str += getFmul() + "/";
        str += _dt1 + "/";
        str += getDetune() + "/";
        str += _ams + "/";
        str += getSsgec() + "/";
        str += getKeyOnPhase() + "/";
        str += String.valueOf(_pitchFixed);
        return str;
    }

    // constructor
    //

    /** constructor */
    SiOPMOperator(SiOPMModule chip) {
        _table = SiOPMTable.getInstance();
        _chip = chip;
        _feedPipe = SLLint.allocRing(1, 0);
        _eg_incTable = _table.eg_incTables[17];
        _eg_levelTable = _table.eg_levelTables[0];
        _eg_nextState = _table_nextState[0];
    }

    // operations
    //

    /** Initialize. */
    public void initialize() {
        // reset operator connections
        _final = true;
        _inPipe = _chip.zeroBuffer;
        _basePipe = _chip.zeroBuffer;
        _feedPipe.i = 0;

        // reset all parameters
        setSiOPMOperatorParam(_chip.initOperatorParam);

        // reset some other parameters
        _eg_tl_offset = 0;  // The _eg_tl_offset is controled by velocity and expression.
        _pitchIndexShift2 = 0;  // The _pitchIndexShift2 is controled by pitch modulation.
        _pcm_channels = 0;
        _pcm_startPoint = 0;
        _pcm_endPoint = 0;
        _pcm_loopPoint = -1;

        // reset pg and eg status
        reset();
    }

    /** Reset. */
    public void reset() {
        _eg_shiftState(EG_OFF);
        _eg_out = (_eg_levelTable[_eg_level] + _eg_total_level) << 3;
        _eg_timer = SiOPMTable.ENV_TIMER_INITIAL;
        _eg_counter = 0;
        _eg_ssgec_state = 0;
        _phase = 0;
    }

    /** Set parameters by SiOPMOperatorParam */
    public void setSiOPMOperatorParam(SiOPMOperatorParam param) {
        setPgType(param.pgType);
        setPtType(param.ptType);

        if (param.phase == 255) _keyon_phase = -2;
        else if (param.phase == -1) _keyon_phase = -1;
        else _keyon_phase = (param.phase & 255) << (SiOPMTable.PHASE_BITS - 8);

        _ar = param.ar & 63;
        _dr = param.dr & 63;
        _sr = param.sr & 63;
        _rr = param.rr & 63;
        _ks = 5 - (param.ksr & 3);
        _ksl = param.ksl & 3;
        _ams = (param.ams != 0) ? (3 - param.ams) : 16;
        _multiple = param.fmul;
        _fmShift = (param.modLevel & 7) + 10;
        _dt1 = param.dt1 & 7;
        _dt1Table = _table.dt1Table[_dt1];
        _pitchIndexShift = param.detune;
        setSsgec(param.ssgec);
        _mute = (param.mute) ? SiOPMTable.ENV_BOTTOM : 0;
        _erst = param.erst;

        // fixed pitch
        if (param.fixedPitch == 0) {
            //_pitchIndex = 3840;
            //_updateKC(_table.nnToKC[(_pitchIndex>>6)&127]);
            _pitchFixed = false;
        } else {
            _pitchIndex = param.fixedPitch;
            _updateKC(_table.nnToKC[(_pitchIndex >> 6) & 127]);
            _pitchFixed = true;
        }
        // key scale level
        _eg_key_scale_level_rshift = (_ksl == 0) ? 8 : (5 - _ksl);
        // ar for ssgec
        _eg_ssgec_ar = (_ssg_type == 8 || _ssg_type == 12) ? ((_ar >= 56) ? 1 : 0) : ((_ar >= 60) ? 1 : 0);
        // sl/tl require recalculating EG sustain and total levels.
        setSl(param.sl & 15);
        setTl(param.tl);

        _updatePitch();
    }

    /** Get parameters by SiOPMOperatorParam */
    public void getSiOPMOperatorParam(SiOPMOperatorParam param) {
        param.pgType = _pgType;
        param.ptType = _ptType;

        param.ar = _ar;
        param.dr = _dr;
        param.sr = _sr;
        param.rr = _rr;
        param.sl = _sl;
        param.tl = _tl;
        param.ksr = getKs();
        param.ksl = _ksl;
        param.fmul = getFmul();
        param.dt1 = _dt1;
        param.detune = getDetune();
        param.ams = getAms();
        param.ssgec = getSsgec();
        param.phase = getKeyOnPhase();
        param.modLevel = (_fmShift > 10) ? (_fmShift - 10) : 0;
        param.erst = _erst;
    }

    /** Set Wave table data. */
    public void setWaveTable(SiOPMWaveTable waveTable) {
        _pgType = SiOPMTable.PG_USER_CUSTOM; // -1
        _waveTable = waveTable.wavelet;
        _waveFixedBits = waveTable.fixedBits;
        setPtType(waveTable.defaultPTType);
    }

    /** Set PCM data. */
    public void setPCMData(SiOPMWavePCMData pcmData) {
        if (pcmData != null && pcmData.wavelet != null) {
            _pgType = SiOPMTable.PG_USER_PCM; // -2
            _waveTable = pcmData.wavelet;
            _waveFixedBits = PCM_waveFixedBits;
            _pcm_channels = pcmData.channelCount;
            _pcm_startPoint = pcmData.getStartPoint();
            _pcm_endPoint = pcmData.getEndPoint();
            _pcm_loopPoint = pcmData.getLoopPoint();
            _keyon_phase = _pcm_startPoint << PCM_waveFixedBits;
            setPtType(SiOPMTable.PT_PCM);
        } else {
            // quick initialization for SiOPMChannelPCM
            _pcm_endPoint = _pcm_loopPoint = 0;
            _pcm_loopPoint = -1;
        }
    }

    /** Note on. */
    public void noteOn() {
        if (_keyon_phase >= 0) _phase = _keyon_phase;
        else if (_keyon_phase == -1) _phase = (int) (Math.random() * SiOPMTable.PHASE_MAX);
        _eg_ssgec_state = -1;
        _eg_shiftState(EG_ATTACK);
        _eg_out = (_eg_levelTable[_eg_level] + _eg_total_level) << 3;
    }

    /** Note off. */
    public void noteOff() {
        _eg_shiftState(EG_RELEASE);
        _eg_out = (_eg_levelTable[_eg_level] + _eg_total_level) << 3;
    }

    /** Set pipes. */
    void _setPipes(SLLint outPipe, SLLint modPipe /* = null */, boolean finalOsc /* = false */) {
        _final = finalOsc;
        _basePipe = (outPipe == modPipe) ? _chip.zeroBuffer : outPipe;
        _outPipe = outPipe;
        _inPipe = modPipe != null ? modPipe : _chip.zeroBuffer;
        _fmShift = 15;
    }

    // internal operations
    //

    /** Update envelop generator. This code instanceof only for testing. */
    void eg_update() {
        _eg_timer -= _eg_timer_step;
        if (_eg_timer < 0) {
            if (_eg_state == EG_ATTACK) {
                if (_eg_incTable[_eg_counter] > 0) {
                    _eg_level -= 1 + (_eg_level >> _eg_incTable[_eg_counter]);
                    if (_eg_level <= 0) _eg_shiftState(_eg_nextState[_eg_state]);
                }
            } else {
                _eg_level += _eg_incTable[_eg_counter];
                if (_eg_level >= _eg_stateShiftLevel) _eg_shiftState(_eg_nextState[_eg_state]);
            }
            _eg_out = (_eg_levelTable[_eg_level] + _eg_total_level) << 3;
            _eg_counter = (_eg_counter + 1) & 7;
            _eg_timer += SiOPMTable.ENV_TIMER_INITIAL;
        }
    }

    /** @private Update pulse generator. This code instanceof only for testing. */
    void pg_update() {
        _phase += _phase_step;
        int p = ((_phase + (_inPipe.i << _fmShift)) & SiOPMTable.PHASE_FILTER) >> _waveFixedBits;
        int l = _waveTable[p];
        l += _eg_out; // + (channel._am_out<<2>>_ams);
        _feedPipe.i = _table.logTable[l];
        _outPipe.i = _feedPipe.i + _basePipe.i;
    }

    /** @private Shift envelop generator state. */
    void _eg_shiftState(int state) {
        int r;

        switch (state) {
            case EG_ATTACK:
                // update ssgec_state
                if (++_eg_ssgec_state == 3) _eg_ssgec_state = 1;
                if (_ar + _eg_key_scale_rate < 62) {
                    if (_erst) _eg_level = SiOPMTable.ENV_BOTTOM;
                    _eg_state = EG_ATTACK;
                    r = (_ar != 0) ? (_ar + _eg_key_scale_rate) : 96;
                    _eg_incTable = _table.eg_incTablesAtt[_table.eg_tableSelector[r]];
                    _eg_timer_step = _table.eg_timerSteps[r];
                    _eg_levelTable = _table.eg_levelTables[0];
                    break;
                }
                // fail through
            case EG_DECAY:
                if (_eg_sustain_level != 0) {
                    _eg_state = EG_DECAY;
                    if (_ssg_type != 0) {
                        _eg_level = 0;
                        _eg_stateShiftLevel = _eg_sustain_level >> 2;
                        if (_eg_stateShiftLevel > SiOPMTable.ENV_BOTTOM_SSGEC)
                            _eg_stateShiftLevel = SiOPMTable.ENV_BOTTOM_SSGEC;
                        _eg_levelTable = _table.eg_levelTables[_table.eg_ssgTableIndex[_ssg_type - 8][_eg_ssgec_ar][_eg_ssgec_state]];
                    } else {
                        _eg_level = 0;
                        _eg_stateShiftLevel = _eg_sustain_level;
                        _eg_levelTable = _table.eg_levelTables[0];
                    }
                    r = (_dr != 0) ? (_dr + _eg_key_scale_rate) : 96;
                    _eg_incTable = _table.eg_incTables[_table.eg_tableSelector[r]];
                    _eg_timer_step = _table.eg_timerSteps[r];
                    break;
                }
                // fail through
            case EG_SUSTAIN: {   // catch all
                _eg_state = EG_SUSTAIN;
                if (_ssg_type != 0) {
                    _eg_level = _eg_sustain_level >> 2;
                    _eg_stateShiftLevel = SiOPMTable.ENV_BOTTOM_SSGEC;
                    _eg_levelTable = _table.eg_levelTables[_table.eg_ssgTableIndex[_ssg_type - 8][_eg_ssgec_ar][_eg_ssgec_state]];
                } else {
                    _eg_level = _eg_sustain_level;
                    _eg_stateShiftLevel = SiOPMTable.ENV_BOTTOM;
                    _eg_levelTable = _table.eg_levelTables[0];
                }
                r = (_sr != 0) ? (_sr + _eg_key_scale_rate) : 96;
                _eg_incTable = _table.eg_incTables[_table.eg_tableSelector[r]];
                _eg_timer_step = _table.eg_timerSteps[r];
                break;
            }

            case EG_RELEASE:
                if (_eg_level < SiOPMTable.ENV_BOTTOM) {
                    _eg_state = EG_RELEASE;
                    _eg_stateShiftLevel = SiOPMTable.ENV_BOTTOM;
                    r = _rr + _eg_key_scale_rate;
                    _eg_incTable = _table.eg_incTables[_table.eg_tableSelector[r]];
                    _eg_timer_step = _table.eg_timerSteps[r];
                    _eg_levelTable = _table.eg_levelTables[(_ssg_type != 0) ? 1 : 0];
                    break;
                }
                // fail through
            case EG_OFF:
            default:
                // catch all
                _eg_state = EG_OFF;
                _eg_level = SiOPMTable.ENV_BOTTOM;
                _eg_stateShiftLevel = SiOPMTable.ENV_BOTTOM + 1;
                _eg_incTable = _table.eg_incTables[17];     // 17 = all zero
                _eg_timer_step = _table.eg_timerSteps[96];  // 96 = all zero
                _eg_levelTable = _table.eg_levelTables[0];
                break;
        }
    }

    // Internal update key code
    private void _updateKC(int i) {
        // kc
        _kc = i;
        // ksr
        _eg_key_scale_rate = _kc >> _ks;
        // ksl
        _updateTotalLevel();
    }

    // Internal update phase step
    private void _updatePitch() {
        int n = (_pitchIndex + _pitchIndexShift + _pitchIndexShift2) & _pitchTableFilter;
        _updatePhaseStep(_pitchTable[n] >> _wavePhaseStepShift);
    }

    // Internal update phase step
    private void _updatePhaseStep(int ps) {
        _phase_step = ps;
        _phase_step += _dt1Table[_kc];
        _phase_step *= _multiple;
        _phase_step >>= (7 - _table.sampleRatePitchShift);  // 44kHz:1/128, 22kHz:1/256
    }

    // Internal update total level
    private void _updateTotalLevel() {
        _eg_total_level = ((_tl + (_kc >> _eg_key_scale_level_rshift)) << SiOPMTable.ENV_LSHIFT) + _eg_tl_offset + _mute;
        if (_eg_total_level > SiOPMTable.ENV_BOTTOM) _eg_total_level = SiOPMTable.ENV_BOTTOM;
        _eg_total_level -= SiOPMTable.ENV_TOP;       // table index +192.
        _eg_out = (_eg_levelTable[_eg_level] + _eg_total_level) << 3;
    }
}
