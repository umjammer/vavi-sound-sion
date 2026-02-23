// Wave Table Synthesizer 
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import java.util.Map;

import org.si.sion.module.SiOPMWaveTable;
import org.si.sion.module.channels.SiOPMChannelFM;
import org.si.sion.utils.SiONUtil;


/**
 * Wave Table Synthesizer
 */
public class WaveTableSynth extends BasicSynth {

    // constants
    //

    /** single layer mode */
    public static final String SINGLE = "single";
    /** detuned layer mode with double operators */
    public static final String DETUNE = "detune";
    /** detune layer mode with triple operators */
    public static final String TRIPLE_DETUNE = "tripleDetune";
    /** layered by closed power code (2operators) */
    public static final String POWER = "power";
    /** layered by closed detuned power code (4operators) */
    public static final String DETUNE_POWER = "detunePower";
    /** layered by closed sus4 code (3operators) */
    public static final String SUS4 = "sus4";
    /** layered by closed sus2 code (3operators) */
    public static final String SUS2 = "sus2";

    /** operator settings */
    protected Map<String, int[]> _operatorSetting = Map.of(
            "single", new int[] {0}, "detune", new int[] {0, 0}, "tripleDetune", new int[] {0, 0, 0},
            "power", new int[] {0, 5}, "detunePower", new int[] {5, 0, 5, 0}, "sus4", new int[] {0, 5, 7},
            "sus2", new int[] {0, 7, 14}
    );

    // variables
    //

    /** wavelet */
    protected double[] _wavelet;
    /** wave table */
    protected SiOPMWaveTable _waveTable;
    /** wave color */
    protected int _waveColor;
    /** layering type */
    protected String _layerType;
    /** layering type */
    protected int[] _operatorPitch;
    /** layering detune */
    protected int _layerDetune;

    // properties
    //

    /** wave data. */
    public double[] getWavelet() {
        return _wavelet;
    }

    /** wave color. */
    public int getColor() {
        return _waveColor;
    }

    public void setColor(int c) {
        _waveColor = c;
        SiONUtil.waveColor(_waveColor, 0, _wavelet);
        updateWavelet();
    }

    /** layering type */
    public String getLayerType() {
        return _layerType;
    }

    public void setLayerType(String t) {
        _operatorPitch = _operatorSetting.get(t);
        if (_operatorPitch == null) throw _errorNoLayerType(t);
        _layerType = t;
        int i, det, imax = _operatorPitch.length;
        _voice.channelParam.opeCount = imax;
        _voice.channelParam.alg = new int[] {0, 1, 5, 7}[imax];
        for (i = 0, det = -((_layerDetune * (imax - 1)) >> 1); i < imax; i++, det += _layerDetune) {
            _voice.channelParam.operatorParam[i].detune = (_operatorPitch[i] << 6) + det;
        }
        _voiceUpdateNumber++;
    }

    /** layer detune, 1 = halftone */
    public int getLayerDetune() {
        return _layerDetune;
    }

    public void setLayerDetune(int d) {
        _layerDetune = d;
        int i, det, imax = _operatorPitch.length;
        for (i = 0, det = -((_layerDetune * (imax - 1)) >> 1); i < imax; i++, det += _layerDetune) {
            _voice.channelParam.operatorParam[i].detune = (_operatorPitch[i] << 6) + det;
        }
        SiOPMChannelFM ch;
        int op, opMax = _operatorPitch.length;
        imax = _tracks.size();
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelFM) _tracks.get(i).channel);
            if (ch != null) {
                for (op = 0; op < opMax; op++) ch.operator[op].setDetune(_voice.channelParam.operatorParam[i].detune);
            }
        }
    }

    // constructor
    //

    /** constructor */
    public WaveTableSynth(String layerType, int waveColor) {
        _wavelet = new double[1024];
        _waveTable = new SiOPMWaveTable();
        _voice.waveData = _waveTable;
        _layerDetune = 4;
        this.setLayerType(layerType);
        this.setColor(waveColor);
    }

    // operation
    //

    /** update wavelet */
    public void updateWavelet() {
        _waveTable.initialize(SiONUtil.logTransVector(_wavelet, 1, _waveTable.wavelet, 0, true), 0);
        int i, imax = _tracks.size();
        SiOPMChannelFM ch;
        for (i = 0; i < imax; i++) {
            ch = ((SiOPMChannelFM) _tracks.get(i).channel);
            if (ch != null) ch.setWaveData(_waveTable);
        }
    }

    // errors
    //

    // no layer type error
    private RuntimeException _errorNoLayerType(String type) {
        return new RuntimeException("WaveTableSynth; no layer type '" + type + "'");
    }
}
