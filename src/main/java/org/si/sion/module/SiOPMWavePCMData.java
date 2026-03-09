//
// class for SiOPM PCM data
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.sion.sequencer.SiMMLTable;
import org.si.sion.utils.SiONUtil;
import org.si.as3.media.Sound;

/** PCM data class */
public class SiOPMWavePCMData extends SiOPMWaveBase {

    // variables
    //

    /** maximum sampling length when converted from Sound instance */
    public int maxSampleLengthFromSound = 1048576;

    /** wave data */
    public int[] wavelet;
    /** channel count */
    public int channelCount;

    /** sampling pitch (noteNumber*64) */
    public int samplingPitch;

    /** wave starting position in sample count. */
    private int _startPoint;
    /** wave end position in sample count. */
    private int _endPoint;
    /** wave looping position in sample count. -1 means no repeat. */
    private int _loopPoint;
    /** flag to slice after loading */
    private boolean _sliceAfterLoading;

    // sin table
    private double[] _sin = null;

    // properties
    //

    /** Sampling data's length */
    public int getSampleCount() {
        return (wavelet != null) ? (wavelet.length >> (channelCount - 1)) : 0;
    }

    /** Sampling data's octave */
    public int getSamplingOctave() {
        return (int) (samplingPitch * 0.001272264631043257);
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

    // constructor
    //

    /**
     * Constructor.
     *
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instance instanceof extracted internally.
     * @param samplingPitch   sampling data's original pitch (noteNumber*64)
     * @param srcChannelCount channel count of source data, this argument instanceof only available when data type instanceof Vector.&lt;Number&gt;.
     * @param channelCount    channel count of this data, 0 sets same with srcChannelCount
     */
    public SiOPMWavePCMData(Object data, int samplingPitch, int srcChannelCount, int channelCount) {
        super(SiMMLTable.MT_PCM);
        if (data != null) initialize(data, samplingPitch, srcChannelCount, channelCount);
    }

    // operations
    //

    /**
     * Initializer.
     *
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instance instanceof extracted internally.
     * @param samplingPitch   sampling data's original note
     * @param srcChannelCount channel count of source data, this argument instanceof only available when data type instanceof Vector.&lt;Number&gt;.
     * @param channelCount    channel count of this data, 0 sets same with srcChannelCount
     * @return this instance.
     */
    public SiOPMWavePCMData initialize(Object data, int samplingPitch, int srcChannelCount, int channelCount) {
        _sliceAfterLoading = false;
        srcChannelCount = (srcChannelCount == 1) ? 1 : 2;
        if (channelCount == 0) channelCount = srcChannelCount;
        this.channelCount = (channelCount == 1) ? 1 : 2;
        if (data instanceof Sound) {
            _listenSoundLoadingEvents((Sound) data);
        } else if (data instanceof double[]) {
            wavelet = SiONUtil.logTransVector(((double[]) data), srcChannelCount, null, this.channelCount, false);
        } else if (data instanceof int[]) {
            wavelet = ((int[]) data);
        } else if (data == null) {
            wavelet = null;
        } else {
            throw new Error("SiOPMWavePCMData; not suitable data type");
        }
        this.samplingPitch = samplingPitch;

        _startPoint = 0;
        _endPoint = this.getSampleCount() - 1;
        _loopPoint = -1;
        return this;
    }

    /**
     * Slicer setting. You can cut samples and set repeating.
     *
     * @param startPoint slicing point to start data. The negative value skips head silence.
     * @param endPoint   slicing point to end data, The negative value calculates from the end.
     * @param loopPoint  slicing point to repeat data, -1 sets no repeat, other negative value sets loop tail samples
     * @return this instance.
     */
    public SiOPMWavePCMData slice(int startPoint, int endPoint, int loopPoint) {
        _startPoint = startPoint;
        _endPoint = endPoint;
        _loopPoint = loopPoint;
        if (!isSoundLoading()) _slice();
        else _sliceAfterLoading = true;
        return this;
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
     * Loop tail samples, this function updates endPoint and loopPoint. This function instanceof called from slice() when loopPoint &lt; -1.
     *
     * @param sampleCount looping sample count.
     * @param tailMargin  margin for end point. sample count from tail of wave data (consider mp3's end gap).
     * @param crossFade   using short cross fading to reduce sample step noise while looping.
     * @see #slice
     */
    public SiOPMWavePCMData loopTailSamples(int sampleCount, int tailMargin /* = 0 */, boolean crossFade /* = true */) {
        _endPoint = _seekEndGap() - tailMargin;
        if (_endPoint < _startPoint + sampleCount) {
            if (_endPoint < _startPoint) _endPoint = _startPoint;
            _loopPoint = _startPoint;
            return this;
        }
        _loopPoint = _endPoint - sampleCount;

        if (crossFade && _loopPoint > _startPoint + sampleCount) {
            int i, j, t, idx0, idx1, li0, li1;
            int[] log = SiOPMTable.getInstance().logTable;
            int envtop = (-SiOPMTable.ENV_TOP) << 3;
            double i2n = 1 / (double) (1 << SiOPMTable.LOG_VOLUME_BITS);
            int offset = _loopPoint << (channelCount - 1);
            int imax = sampleCount << (channelCount - 1);
            int dt = (int) (1.5707963267948965 / imax);
            if (_sin.length != imax) {
                _sin = new double[imax];
                for (i = 0, t = 0; i < imax; i++, t += dt) _sin[i] = Math.sin(t);
            }
            for (i = 0; i < imax; i++) {
                idx0 = offset + i;
                idx1 = idx0 - imax;
                li0 = wavelet[idx0] + envtop;
                li1 = wavelet[idx1] + envtop;
                j = imax - 1 - i;
                wavelet[idx0] = SiOPMTable.calcLogTableIndex((log[li0] * _sin[j] + log[li1] * _sin[i]) * i2n);
            }
        }

        return this;
    }

    // seek mp3 head gap
    private int _seekHeadSilence() {
        int i, imax = wavelet.length, threshold = SiOPMTable.LOG_TABLE_BOTTOM - SiOPMTable.LOG_TABLE_RESOLUTION * 14; // 1/128
        for (i = 0; i < imax; i++) if (wavelet[i] < threshold) break;
        return i >> (channelCount - 1);
    }

    // seek mp3 end gap
    private int _seekEndGap() {
        int i, threshold = SiOPMTable.LOG_TABLE_BOTTOM - SiOPMTable.LOG_TABLE_RESOLUTION * 2; // 1/4096
        for (i = wavelet.length - 1; i > 0; --i) if (wavelet[i] < threshold) break;
        return (i >> (channelCount - 1)) - 100; // 100 = 1 cycle margin
    }

    /** */
    protected void _onSoundLoadingComplete(Sound sound) {
        wavelet = SiONUtil.logTrans(sound, null, channelCount, maxSampleLengthFromSound, 0, false);
        if (_sliceAfterLoading) _slice();
        _sliceAfterLoading = false;
    }

    private void _slice() {
        // start point
        if (_startPoint < 0) _startPoint = _seekHeadSilence();
        if (_loopPoint < -1) {
            // set loop infinitly
            if (_endPoint >= 0) {
                loopTailSamples(-_loopPoint, 0, true);
                if (_startPoint >= _endPoint) _endPoint = _startPoint;
            } else {
                loopTailSamples(-_loopPoint, -_endPoint, true);
            }
        } else {
            // end point
            int waveletLengh = getSampleCount();
            if (_endPoint < 0) _endPoint = _seekEndGap() + _endPoint;
            else if (_endPoint < _startPoint) _endPoint = _startPoint;
            else if (waveletLengh < _endPoint) _endPoint = waveletLengh - 1;
            // loop point
            if (_loopPoint != -1 && _loopPoint < _startPoint) _loopPoint = _startPoint;
            else if (_endPoint < _loopPoint) _loopPoint = -1;
        }
    }
}
