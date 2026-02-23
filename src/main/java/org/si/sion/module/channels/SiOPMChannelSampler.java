//
// SiOPM Sampler pad channel.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module.channels;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMStream;
import org.si.sion.module.SiOPMWaveBase;
import org.si.sion.module.SiOPMWaveSamplerData;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.utils.ByteArray;


/** Sampler pad channel. */
public class SiOPMChannelSampler extends SiOPMChannelBase {

    // variables
    //

    /** bank number */
    protected int _bankNumber;
    /** wave number */
    protected int _waveNumber;

    /** expression */
    protected double _expression;

    /** sample table */
    protected SiOPMWaveSamplerTable _samplerTable;
    /** sample table */
    protected SiOPMWaveSamplerData _sampleData;
    /** sample index */
    protected int _sampleIndex;
    /** phase reset */
    protected int _sampleStartPhase;

    /** ByteArray to extract */
    protected ByteArray _extractedByteArray;
    /** sample data */
    protected double[] _extractedSample;

    // pan of current note
    private int _samplePan;

    // toString
    //

    /** Output parameters. */
    public String toString() {
        String str = "SiOPMChannelSampler : ";
        $2("vol", _volumes[0] * _expression, "pan", _pan - 64);
        return str;
    }

    String $2(String p, double i, String q, int j) {
         return "  " + p + "=" + i + " / " + q + "=" + j + "\n";
    }

    // constructor
    //

    /** constructor */
    public SiOPMChannelSampler(SiOPMModule chip) {
        super(chip);
        _extractedByteArray = new ByteArray();
        _extractedSample = new double[chip.getBufferLength() * 2];
    }

    // parameter setting
    //

    /**
     * Set by SiOPMChannelParam.
     *
     * @param param      SiOPMChannelParam.
     * @param withVolume Set volume when its true.
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

    /** pgType and ptType (&#64; call from SiMMLChannelSetting.selectTone()/initializeTone()) */
    @Override
    public void setType(int pgType, int ptType) {
        _bankNumber = pgType & 3;
    }

    // interfaces
    //

    /** pitch = (note &lt;&lt; 6) | (kf &amp; 63) [0,8191] */
    @Override
    public int getPitch() {
        return _waveNumber << 6;
    }

    @Override
    public void setPitch(int p) {
        _waveNumber = p >> 6;
    }

    /** Set wave data. */
    @Override
    public void setWaveData(SiOPMWaveBase waveData) {
        _samplerTable = ((SiOPMWaveSamplerTable) waveData);
        _sampleData = ((SiOPMWaveSamplerData) waveData);
    }

    // volume controls
    //

    /** update all tl offsets of final carriors */
    @Override
    public void offsetVolume(int expression, int velocity) {
        _expression = expression * velocity * 0.00006103515625; // 1/16384
    }

    /** phase (&#64;ph) */
    @Override
    public void setPhase(int i) {
        _sampleStartPhase = i;
    }

    // operation
    //

    /** Initialize. */
    @Override
    public void initialize(SiOPMChannelBase prev, int bufferIndex) {
        super.initialize(prev, bufferIndex);
        reset();
    }

    /** Reset. */
    @Override
    public void reset() {
        _isNoteOn = false;
        _isIdling = true;
        _bankNumber = 0;
        _waveNumber = -1;
        _samplePan = 0;

        _samplerTable = _table.samplerTables[0];
        _sampleData = null;

        _sampleIndex = 0;
        _sampleStartPhase = 0;
        _expression = 1;
    }

    /** Note on. */
    @Override
    public void noteOn() {
        if (_waveNumber >= 0) {
            if (_samplerTable != null) _sampleData = _samplerTable.getSample(_waveNumber & 127);
            if (_sampleData != null && _sampleStartPhase != 255) {
                _sampleIndex = _sampleData.getInitialSampleIndex(_sampleStartPhase * 0.00390625); // 1/256
                _samplePan = _pan + _sampleData.getPan();
                if (_samplePan < 0) _samplePan = 0;
                else if (_samplePan > 128) _samplePan = 128;
            }
            _isIdling = (_sampleData == null);
            _isNoteOn = !_isIdling;
        }
    }

    /** Note off. */
    @Override
    public void noteOff() {
        if (_sampleData != null) {
            if (!_sampleData.ignoreNoteOff()) {
                _isNoteOn = false;
                _isIdling = true;
                if (_samplerTable != null) _sampleData = null;
            }
        }
    }

    /** Buffering */
    @Override
    public void buffer(int len) {
        int i, imax, vol, residue, processed;
        SiOPMStream stream;
        if (_isIdling || _sampleData == null || _mute) {
            //_nop(len);
        } else {
            if (_sampleData.isExtracted()) {
                // stream extracted data
                for (residue = len, i = 0; residue > 0; ) {
                    // copy to buffer
                    processed = (_sampleIndex + residue < _sampleData.getEndPoint()) ? residue : (_sampleData.getEndPoint() - _sampleIndex);
                    if (_hasEffectSend) {
                        for (i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
                            if (_volumes[i] > 0) {
                                stream = _streams[i] != null ? _streams[i] : _chip.streamSlot[i];
                                if (stream != null) {
                                    vol = (int) (_volumes[i] * _expression * _chip.samplerVolume);
                                    stream.writeVectorNumber(_sampleData.getWaveData(), _sampleIndex, _bufferIndex, processed, vol, _samplePan, _sampleData.getChannelCount());
                                }
                            }
                        }
                    } else {
                        stream = _streams[0] != null ? _streams[0] : _chip.outputStream;
                        vol = (int) (_volumes[0] * _expression * _chip.samplerVolume);
                        stream.writeVectorNumber(_sampleData.getWaveData(), _sampleIndex, _bufferIndex, processed, vol, _samplePan, _sampleData.getChannelCount());
                    }
                    _sampleIndex += processed;

                    // processed samples are not enough == achieves to the end
                    residue -= processed;
                    if (residue > 0) {
                        if (_sampleData.getLoopPoint() >= 0) {
                            // loop
                            if (_sampleData.getLoopPoint() > _sampleData.getStartPoint()) _sampleIndex = _sampleData.getLoopPoint();
                            else _sampleIndex = _sampleData.getStartPoint();
                        } else {
                            // end (note off)
                            _isIdling = true;
                            if (_samplerTable != null) _sampleData = null;
                            //_nop(len - processed);
                            break;
                        }
                    }
                }
            } else {
                // stream Sound data with extracting
                for (residue = len, i = 0, imax = 0; residue > 0; ) {
                    // extract a part
                    _extractedByteArray.length = 0;
                    processed = _sampleData.getSoundData().extract(_extractedByteArray, residue, _sampleIndex << 1);
                    _sampleIndex += processed >> 1;
                    if (_sampleIndex > _sampleData.getEndPoint()) processed -= _sampleIndex - _sampleData.getEndPoint();

                    // copy to vector
                    imax += processed << 1;
                    _extractedByteArray.position = 0;
                    for (; i < imax; i++) {
                        _extractedSample[i] = _extractedByteArray.readFloat();
                    }

                    // processed samples are not enough == achieves to the end
                    residue -= processed;
                    if (residue > 0) {
                        if (_sampleData.getLoopPoint() >= 0) {
                            // loop
                            if (_sampleData.getLoopPoint() > _sampleData.getStartPoint()) _sampleIndex = _sampleData.getLoopPoint();
                            else _sampleIndex = _sampleData.getStartPoint();
                        } else {
                            // end (note off)
                            _isIdling = true;
                            if (_samplerTable != null) _sampleData = null;
                            //_nop(len - processed);
                            break;
                        }
                    }
                }
                processed = len - residue;

                // copy to buffer
                if (_hasEffectSend) {
                    for (i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
                        if (_volumes[i] > 0) {
                            stream = _streams[i] != null ? _streams[i] : _chip.streamSlot[i];
                            if (stream != null) {
                                vol = (int) (_volumes[i] * _expression * _chip.samplerVolume);
                                stream.writeVectorNumber(_extractedSample, 0, _bufferIndex, processed, vol, _samplePan, 2);
                            }
                        }
                    }
                } else {
                    stream = _streams[0] != null ? _streams[0] : _chip.outputStream;
                    vol = (int) (_volumes[0] * _expression * _chip.samplerVolume);
                    stream.writeVectorNumber(_extractedSample, 0, _bufferIndex, processed, vol, _samplePan, 2);
                }
            }
        }

        // update buffer index
        _bufferIndex += len;
    }

    /** Buffering without processnig */
    @Override
    public void nop(int len) {
        //_nop(len);
        _bufferIndex += len;
    }
}
