//
// SiOPM Karplus-Strong algorism with FM synth.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module.channels;

import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMStream;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.sequencer.SiMMLTable;
import org.si.sion.sequencer.SiMMLVoice;
import org.si.utils.SLLint;


/** Karplus-Strong algorism with FM synth. */
public class SiOPMChannelKS extends SiOPMChannelFM {

    // variables
    //
    private static final int KS_BUFFER_SIZE = 5400;     // 5394 = sampling count of MIDI note number=0

    private static final int KS_SEED_DEFAULT = 0;
    private static final int KS_SEED_FM = 1;
    private static final int KS_SEED_PCM = 2;


    // variables
    //
    private final int[] _ks_delayBuffer;   // delay buffer
    private double _ks_delayBufferIndex;    // delay buffer index
    private int _ks_pitchIndex;             // pitch index
    private double _ks_decay_lpf;           // lpf decay
    private double _ks_decay;               // decay
    private double _ks_mute_decay_lpf;      // lpf decay @mute
    private double _ks_mute_decay;          // decay @mute

    private double _output;                 // output
    private double _decay_lpf;              // lpf decay
    private double _decay;                  // decay
    private double _expression;             // expression

    private int _ks_seedType;               // seed type
    private int _ks_seedIndex;              // seed index

    // toString
    //

    /** Output parameters. */
    public String toString() {
        String str = "SiOPMChannelKS : operatorCount=";
        str += _operatorCount + "\n";
        $("fb ", _inputLevel - 6);
        $2("vol", _volumes[0], "pan", _pan - 64);
        if (operator[0] != null) str += operator[0] + "\n";
        if (operator[1] != null) str += operator[1] + "\n";
        if (operator[2] != null) str += operator[2] + "\n";
        if (operator[3] != null) str += operator[3] + "\n";
        return str;
    }

    String $(String p, int i) {
        return "  " + p + "=" + i + "\n";
    }

    String $2(int p, int i, int q, int j) {
        return "  " + p + "=" + i + " / " + q + "=" + j + "\n";
    }

    // constructor
    //

    /** constructor */
    public SiOPMChannelKS(SiOPMModule chip) {
        super(chip);
        _ks_delayBuffer = new int[KS_BUFFER_SIZE];
    }

    // LFO settings
    //

    @Override
    protected void _lfoSwitch(boolean sw) {
        _lfo_on = 0;
    }

    // parameter setting
    //

    /**
     * Set Karplus Strong parameters
     *
     * @param ar         attack rate of plunk energy
     * @param dr         decay rate of plunk energy
     * @param tl         total level of plunk energy
     * @param fixedPitch plunk noise pitch
     * @param ws         wave shape of plunk
     * @param tension    sustain rate of the tone
     */
    public void setKarplusStrongParam(int ar, int dr, int tl, int fixedPitch, int ws, int tension) {
        if (ws == -1) ws = SiOPMTable.PG_NOISE_PINK;
        _ks_seedType = KS_SEED_DEFAULT;
        setAlgorism(1, 0);
        setFeedBack(0, 0);
        setSiOPMParameters(ar, dr, 0, 63, 15, tl, 0, 0, 1, 0, 0, 0, 0, fixedPitch);
        activeOperator.setPgType(ws);
        activeOperator.setPtType(_table.getWaveTable(activeOperator.getPgType()).defaultPTType);
        setAllReleaseRate(tension);
    }

    // interfaces
    //

    /**
     * Set parameters (&#64; commands 2nd-15th args.). (&#64;alg,ar,dr,tl,fix,ws)
     */
    @Override
    public void setParameters(int[] param) {
        _ks_seedType = (param[0] == Integer.MIN_VALUE) ? 0 : param[0];
        _ks_seedIndex = (param[1] == Integer.MIN_VALUE) ? 0 : param[1];

        switch (_ks_seedType) {
            case KS_SEED_FM:
                if (_ks_seedIndex >= 0 && _ks_seedIndex < SiMMLTable.VOICE_MAX) {
                    SiMMLVoice voice = SiMMLTable.getInstance().getSiMMLVoice(_ks_seedIndex);
                    if (voice != null) setSiOPMChannelParam(voice.channelParam, false, true);
                }
                break;
            case KS_SEED_PCM:
                if (_ks_seedIndex >= 0 && _ks_seedIndex < SiOPMTable.PCM_DATA_MAX) {
                    SiOPMWavePCMTable pcm = _table.getPCMData(_ks_seedIndex);
                    if (pcm != null) setWaveData(pcm);
                }
                break;
            default:
                _ks_seedType = KS_SEED_DEFAULT;
                //setAlgorism(1, 0);
                //setFeedBack(0, 0);
                setSiOPMParameters(param[1], param[2], 0, 63, 15, param[3], 0, 0, 1, 0, 0, 0, 0, param[4]);
                activeOperator.setPgType((param[5] == Integer.MIN_VALUE) ? SiOPMTable.PG_NOISE_PINK : param[5]);
                activeOperator.setPtType(_table.getWaveTable(activeOperator.getPgType()).defaultPTType);
                break;
        }
    }

    /** pgType and ptType (&#64; commands 1st arg except for %6,7) */
    @Override
    public void setType(int pgType, int ptType) {
        _ks_seedType = pgType;
        _ks_seedIndex = 0;
    }

    /** Attack rate */
    @Override
    public void setAllAttackRate(int ar) {
        SiOPMOperator ope = operator[0];
        ope._ar = ar;
        ope._dr = Math.min(ar, 48);
        ope._tl = (ar > 48) ? 0 : (48 - ar);
    }

    /** Release rate (s) */
    @Override
    public void setAllReleaseRate(int rr) {
        _ks_decay_lpf = 1 - rr * 0.015625; // 1/64
    }

    // interfaces
    //

    /** pitch = (note &lt;&lt; 6) | (kf &amp; 63) [0,8191] */
    @Override
    public int getPitch() {
        return _ks_pitchIndex;
    }

    @Override
    public void setPitch(int p) {
        _ks_pitchIndex = p;
    }

    /** release rate (&#64;rr) */
    @Override
    public void setRr(int i) {
        _ks_decay_lpf = 1 - i * 0.015625; // 1/64
    }

    /** fixed pitch (&#64;fx) */
    @Override
    public void setFixedPitch(int p) {
        for (int i = 0; i < _operatorCount; i++) operator[i].setFixedPitchIndex(p);
    }

    // volume controls
    //

    /** update all tl offsets of final carriors */
    @Override
    public void offsetVolume(int expression, int velocity) {
        _expression = expression * 0.0078125;
        super.offsetVolume(128, velocity);
    }

    // operation
    //

    /** Initialize. */
    @Override
    public void initialize(SiOPMChannelBase prev, int bufferIndex) {
        _ks_delayBufferIndex = 0;
        _ks_pitchIndex = 0;
        _ks_decay_lpf = 0.875;
        _ks_decay = 0.98;
        _ks_mute_decay_lpf = 0.5;
        _ks_mute_decay = 0.75;

        _output = 0;
        _decay_lpf = _ks_mute_decay_lpf;
        _decay = _ks_mute_decay;
        _expression = 1;

        super.initialize(prev, bufferIndex);

        _ks_seedType = 0;
        _ks_seedIndex = 0;
        setSiOPMParameters(48, 48, 0, 63, 15, 0, 0, 0, 1, 0, 0, 0, -1, 0);
        activeOperator.setPgType(SiOPMTable.PG_NOISE_PINK);
        activeOperator.setPtType(SiOPMTable.PT_PCM);
    }

    /** Reset. */
    @Override
    public void reset() {
        for (int i = 0; i < KS_BUFFER_SIZE; i++) _ks_delayBuffer[i] = 0;
        super.reset();
    }

    /** Note on. */
    @Override
    public void noteOn() {
        _output = 0;
        for (int i = 0; i < KS_BUFFER_SIZE; i++) _ks_delayBuffer[i] *= 0.3;
        _decay_lpf = _ks_decay_lpf;
        _decay = _ks_decay;

        super.noteOn();
    }

    /** Note off. */
    @Override
    public void noteOff() {
        _decay_lpf = _ks_mute_decay_lpf;
        _decay = _ks_mute_decay;
    }

    /** Prepare buffering */
    @Override
    public void resetChannelBufferStatus() {
        _bufferIndex = 0;
        _isIdling = false;
    }

    /** Buffering */
    @Override
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

            // ring modulation
            if (_ringPipe != null) _applyRingModulation(monoOut, len);

            // Karplus-Strong algorism
            _applyKarplusStrong(monoOut, len);

            // State variable filter
            if (_filterOn) _applySVFilter(monoOut, len, null);

            // standard output
            if (_outputMode == OUTPUT_STANDARD && !_mute) {
                if (_hasEffectSend) {
                    for (i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
                        if (_volumes[i] > 0) {
                            stream = _streams[i] != null ? _streams[i] : _chip.streamSlot[i];
                            if (stream != null) stream.write(monoOut, _bufferIndex, len, _volumes[i] * _expression, _pan);
                        }
                    }
                } else {
                    stream = _streams[0] != null ? _streams[0] : _chip.outputStream;
                    stream.write(monoOut, _bufferIndex, len, _volumes[0] * _expression, _pan);
                }
            }
        }

        // update buffer index
        _bufferIndex += len;
    }

    // Karplus-Strong algorism
    private void _applyKarplusStrong(SLLint pointer, int len) {
        int i, t, tmax = SiOPMTable.PITCH_TABLE_SIZE - 1;
        double indexMax;
        t = _ks_pitchIndex + operator[0]._pitchIndexShift + _pm_out;
        if (t < 0) t = 0;
        else if (t > tmax) t = tmax;
        indexMax = _table.pitchWaveLength[t];

        for (i = 0; i < len; i++) {
            // lfo_update();
            _lfo_timer -= _lfo_timer_step;
            if (_lfo_timer < 0) {
                _lfo_phase = (_lfo_phase + 1) & 255;
                t = _lfo_waveTable[_lfo_phase];
                //_am_out = (t * _am_depth) >> 7 << 3;
                _pm_out = (((t << 1) - 255) * _pm_depth) >> 8;
                t = _ks_pitchIndex + operator[0]._pitchIndexShift + _pm_out;
                if (t < 0) t = 0;
                else if (t > tmax) t = tmax;
                indexMax = _table.pitchWaveLength[t];
                _lfo_timer += _lfo_timer_initial;
            }

            // ks_update();
            if (++_ks_delayBufferIndex >= indexMax) _ks_delayBufferIndex %= indexMax;
            _output *= _decay;
            t = (int) (_ks_delayBufferIndex);
            _output += (_ks_delayBuffer[t] - _output) * _decay_lpf + pointer.i;
            _ks_delayBuffer[t] = (int) _output;
            pointer.i = (int) (_output);
            pointer = pointer.next;
        }
    }
}
