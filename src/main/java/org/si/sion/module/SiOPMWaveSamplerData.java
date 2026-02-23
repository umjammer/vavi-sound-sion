//
// class for SiOPM samplers wave
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.sion.sequencer.SiMMLTable;
import org.si.sion.utils.PeakDetector;
import org.si.sion.utils.SiONUtil;
import org.si.utils.SLLNumber;
import vavi.media.Sound;


/** SiOPM samplers wave data */
public class SiOPMWaveSamplerData extends SiOPMWaveBase {

    // constant
    //

    /** maximum length limit to extract Sound [ms] */
    public int extractThreshold = 4000;

    // variables
    //

    // Sound data
    private Sound _soundData;
    // Wave data
    private double[] _waveData;
    // channel count of this data
    private int _channelCount;
    // pan
    private int _pan;
    // extraction flag
    private boolean _isExtracted;
    // wave starting position in sample count.
    private int _startPoint;
    // wave end position in sample count.
    private int _endPoint;
    // wave looping position in sample count. -1 means no repeat.
    private int _loopPoint;
    // flag to slice after loading
    private boolean _sliceAfterLoading;
    // flag to ignore note off
    private boolean _ignoreNoteOff;
    // peak list for time stretch
    private double[] _peakList;

    // properties
    //

    /** Sound data */
    public Sound getSoundData() {
        return _soundData;
    }

    /** Wave data */
    public double[] getWaveData() {
        return _waveData;
    }

    /** channel count of this data. */
    public int getChannelCount() {
        return _channelCount;
    }

    /** pan [-64 - 64] */
    public int getPan() {
        return _pan;
    }

    /** Sammple length */
    public int getLength() {
        if (_isExtracted) return (_waveData.length >> (_channelCount - 1));
        // if (_soundData != null && _soundData instanceof vavi.media.Sound) return (int)(SiONUtil.getSampleLength((vavi.media.Sound)_soundData));
        return 0;
    }

    /** Is extracted ? */
    public boolean isExtracted() {
        return _isExtracted;
    }


    /** flag to ignore note off. set true to ignore note off (one shot voice). this flag instanceof only available for non-loop samples. */
    public boolean ignoreNoteOff() {
        return _ignoreNoteOff;
    }

    public void setIgnoreNoteOff(boolean b) {
        _ignoreNoteOff = (_loopPoint == -1) && b;
    }


    /** wave starting position in sample count. you can set this property by slice(). @see #slice() */
    public int getStartPoint() {
        return _startPoint;
    }

    /** wave end position in sample count. you can set this property by slice(). @see #slice() */
    public int getEndPoint() {
        return _endPoint;
    }

    /** wave looping position in sample count. -1 means no repeat. you can set this property by slice(). @see #slice() */
    public int getLoopPoint() {
        return _loopPoint;
    }

    /** peak list only available for extracted data */
    public double[] getPeakList() {
        return _peakList;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instanceof extracted when the length instanceof shorter than SiOPMWaveSamplerData.extractThreshold[msec].
     * @param ignoreNoteOff   flag to ignore note off
     * @param pan             pan of this sample [-64 - 64].
     * @param srcChannelCount channel count of source data, this argument instanceof only available when data type instanceof Vector.&lt;Number&gt;.
     * @param channelCount    channel count of this data, 0 sets same with srcChannelCount
     * @param peakList        peak list for time stretching
     */
    public SiOPMWaveSamplerData(Object data, boolean ignoreNoteOff, int pan, int srcChannelCount /* = 2 */, int channelCount /* = 0 */, double[] peakList /* = null */) {
        super(SiMMLTable.MT_SAMPLE);
        if (data != null) initialize(data, ignoreNoteOff, pan, srcChannelCount, channelCount, peakList);
    }

    // operations
    //

    /**
     * initialize
     *
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instanceof extracted when the length instanceof shorter than SiOPMWaveSamplerData.extractThreshold[msec].
     * @param ignoreNoteOff   flag to ignore note off
     * @param pan             pan of this sample.
     * @param srcChannelCount channel count of source data, this argument instanceof only available when data type instanceof Vector.&lt;Number&gt;.
     * @param channelCount    channel count of this data, 0 sets same with srcChannelCount. This argument instanceof ignored when the data instanceof not extracted.
     * @return this instance.
     * @see #extractThreshold
     */
    public SiOPMWaveSamplerData initialize(Object data, boolean ignoreNoteOff, int pan, int srcChannelCount /* = 2 */, int channelCount /* = 0 */, double[] peakList /* = null */) {
        _sliceAfterLoading = false;
        srcChannelCount = (srcChannelCount == 1) ? 1 : 2;
        if (channelCount == 0) channelCount = srcChannelCount;
        this._channelCount = (channelCount == 1) ? 1 : 2;
        if (data instanceof double[]) {
            this._soundData = null;
            this._waveData = _transChannel((double[]) data, srcChannelCount, _channelCount);
            _isExtracted = true;
        } else if (data instanceof Sound) {
            _listenSoundLoadingEvents((Sound) data);
        } else if (data == null) {
            this._soundData = null;
            this._waveData = null;
            _isExtracted = false;
        } else {
            throw new Error("SiOPMWaveSamplerData; not suitable data type");
        }

        this._startPoint = 0;
        this._endPoint = getLength();
        this._loopPoint = -1;
        this._peakList = peakList;
        this._ignoreNoteOff = ignoreNoteOff;
        this._pan = pan;
        return this;
    }


    /**
     * Slicer setting. You can cut samples and set repeating.
     *
     * @param startPoint slicing point to start data.The negative value skips head silence.
     * @param endPoint   slicing point to end data. The negative value plays whole data.
     * @param loopPoint  slicing point to repeat data. The negative value sets no repeat.
     * @return this instance.
     */
    public SiOPMWaveSamplerData slice(int startPoint, int endPoint, int loopPoint) {
        _startPoint = startPoint;
        _endPoint = endPoint;
        _loopPoint = loopPoint;
        if (isSoundLoading()) _sliceAfterLoading = true;
        else _slice();
        return this;
    }

    /** extract Sound data. The sound data shooter than extractThreshold instanceof already extracted. [CAUTION] Long sound takes long time to extract and consumes large memory area. @see extractThreshold */
    public void extract() {
        if (_isExtracted) return;
        this._waveData = SiONUtil.extract(this._soundData, null, _channelCount, getLength(), 0);
        _isExtracted = true;
    }

    /**
     * Get initial sample index.
     *
     * @param phase Starting phase, ratio from start point to end point(0-1).
     */
    public int getInitialSampleIndex(double phase) {
        return (int) (_startPoint * (1 - phase) + _endPoint * phase);
    }

    /**
     * construct peak list,
     */
    public PeakDetector constructPeakList() {
        if (!_isExtracted) throw new Error("constructPeakList instanceof only available for extracted data");
        PeakDetector pd = new PeakDetector(0.005, 0.5, 0, 0.1);
        pd.setSamples(_waveData, _channelCount, false);
        _peakList = pd.getPeakList();
        return pd;
    }


    // seek head silence
    private int _seekHeadSilence() {
        if (_waveData != null) {
            int i = 0, imax = _waveData.length, ms;
            SLLNumber msWindow = SLLNumber.allocRing(22, 0); // 0.5ms
            if (_channelCount == 1) {
                ms = 0;
                for (i = 0; i < imax; i++) {
                    ms -= msWindow.n;
                    msWindow = msWindow.next;
                    msWindow.n = _waveData[i] * _waveData[i];
                    ms += msWindow.n;
                    if (ms > 0.0011) break;
                }
            } else {
                ms = 0;
                for (i = 0; i < imax; ) {
                    ms -= msWindow.n;
                    msWindow = msWindow.next;
                    msWindow.n = _waveData[i] * _waveData[i];
                    i++;
                    msWindow.n += _waveData[i] * _waveData[i];
                    i++;
                    ms += msWindow.n;
                    if (ms > 0.0022) break;
                }
                i >>= 1;
            }
            SLLNumber.freeRing(msWindow);
            return i - 22;
        }
        return (_soundData != null) ? SiONUtil.getHeadSilence(_soundData, 0.05) : 0;
    }

    // seek mp3 end gap
    private int _seekEndGap() {
        if (_waveData != null) {
            int i;
            double ms;
            if (_channelCount == 1) {
                for (i = _waveData.length - 1; i >= 0; i--) {
                    if (_waveData[i] * _waveData[i] > 0.0001) break;
                }
            } else {
                for (i = _waveData.length - 1; i >= 0; ) {
                    ms = _waveData[i] * _waveData[i];
                    i--;
                    ms += _waveData[i] * _waveData[i];
                    i--;
                    if (ms > 0.0002) break;
                }
                i >>= 1;
            }
            int len = getLength();
            return (i > len - 1152) ? i : (len - 1152);
        }
        return (_soundData != null) ? (getLength() - SiONUtil.getEndGap(_soundData, 0.05, 2304)) : 0;
    }

    // change channel count as needed
    private double[] _transChannel(double[] src, int srcChannelCount, int channelCount) {
        int i, j, imax;
        double[] dst;
        if (srcChannelCount == channelCount) return src;
        if (srcChannelCount == 1) { // 1->2
            imax = src.length;
            dst = new double[imax << 1];
            for (i = 0, j = 0; i < imax; i++, j += 2) dst[j + 1] = dst[j] = src[i];
        } else { // 2->1
            imax = src.length >> 1;
            dst = new double[imax];
            for (i = 0, j = 0; i < imax; i++, j += 2) dst[i] = (src[j] + src[j + 1]) * 0.5;
        }
        return dst;
    }

    /** */
    protected void _onSoundLoadingComplete(Sound sound) {
        this._soundData = sound;
        if (this._soundData.length <= extractThreshold) {
            this._waveData = SiONUtil.extract(this._soundData, null, _channelCount, extractThreshold * 45, 0);
            _isExtracted = true;
        } else {
            this._waveData = null;
            _isExtracted = false;
        }
        if (_sliceAfterLoading) _slice();
        _sliceAfterLoading = false;
    }

    private void _slice() {
        if (_startPoint < 0) _startPoint = _seekHeadSilence();
        if (_loopPoint < 0) _loopPoint = -1;
        if (_endPoint < 0) _endPoint = _seekEndGap();
        if (_endPoint < _loopPoint) _loopPoint = -1;
        if (_endPoint < _startPoint) {
           int len = getLength();
           _endPoint = len - 1;
        }
    }
}
