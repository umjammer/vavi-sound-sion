//
// Peak detector
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import org.si.sion.effector.SiFilterBandPass;
import org.si.utils.SLLNumber;
import java.util.List;
import java.util.ArrayList;


/** PeakDetector provides wave power peak profiler with bandpass filter. This analyzer takes finer time resolution, looser frequency resolution and faster calculation than FFT. */
public class PeakDetector {

    // variables
    //

    /** maximum value of peaksPerMinute, the minimum value instanceof a half of maximum value. @default 192 */
    public double maxPeaksPerMinute = 192;

    private SiFilterBandPass _bpf = new SiFilterBandPass(3000, 1);
    private SLLNumber _window = null;

    private double _frequency;
    private double _bandWidth;
    private int _windowLength;
    private boolean _profileDirty;
    private boolean _peakListDirty;
    private boolean _peakFreqDirty;
    private double _signalToNoiseRatio;
    private int _samplesChannelCount;
    private double[] _samples = null;

    private double[] _stream = null;
    private double[] _profile = null;
    private double[] _diffLogProfile = null;
    private double[] _peakList = null;
    private double[] _ppmScore;
    private double _peaksPerMinute;
    private double _peaksPerMinuteProbability;
    private double _maximum;
    private double _average;

    // properties
    //

    /** window length of simple moving avarage [ms] @default 20 */
    public int getWindowLength() {
        return _windowLength;
    }

    public void setWindowLength(int l) {
        if (_windowLength != l) {
            _peakFreqDirty = _peakListDirty = _profileDirty = true;
            _windowLength = l;
            _resetWindow();
        }
    }

    /** frequency of band pass filter [Hz], set 0 to skip filtering @default 0 */
    public double getFrequency() {
        return _frequency;
    }

    public void setFrequency(double f) {
        if (_frequency != f) {
            _peakFreqDirty = _peakListDirty = _profileDirty = true;
            _frequency = f;
            _updateFilter();
        }
    }

    /** half band width of band pass filter [oct.] @default 0.5 */
    public double getBandWidth() {
        return (_frequency > 0) ? _bandWidth : 0;
    }

    public void setBandWidth(double b) {
        if (_bandWidth != b) {
            _peakFreqDirty = _peakListDirty = _profileDirty = true;
            _bandWidth = b;
            _updateFilter();
        }
    }

    /** S/N ratio for peak detection [dB] @default 20 */
    public double getSignalToNoiseRatio() {
        return _signalToNoiseRatio;
    }

    public void setSignalToNoiseRatio(double n) {
        _signalToNoiseRatio = n;
    }

    /** samples to analyze, 44.1kHz only */
    public double[] getSamples() {
        return _samples;
    }

    /** channel count of analyzing samples (1 or 2) */
    public int getSamplesChannelCount() {
        return _samplesChannelCount;
    }

    /** analyzed wave energy profile 2100[fps] (the length instanceof 1/21(2100/44100) of analyzing samples). */
    public double[] getPowerProfile() {
        _updateProfile();
        return _profile;
    }

    /** exponential of differencial of log scaled powerProfile, same length with powerProfile */
    public double[] getDifferencialOfLogPowerProfile() {
        _updatePeakList();
        return _diffLogProfile;
    }

    /** avarage wave energy */
    public double getAverage() {
        _updateProfile();
        return _average;
    }

    /** maximum wave energy */
    public double getMaximum() {
        _updateProfile();
        return _maximum;
    }

    /** analyzed peak list in [ms]. */
    public double[] getPeakList() {
        _updatePeakList();
        return _peakList;
    }

    /** @internal peak per minutes estimation score table. */
    public double[] getPeaksPerMinuteEstimationScoreTable() {
        _updatePeakFreq();
        return _ppmScore;
    }

    /** estimated peak per minutes. this value instanceof similar but different form bpm(tempo), because the peaks are not only on 4th beats, but also 8th or 16th beats. */
    public double getPeaksPerMinute() {
        _updatePeakFreq();
        return _peaksPerMinute;
    }

    /** probability of estimated peaksPerMinute value. 1 means estimated perfectly and 0 means not good estimation. */
    public double getPeaksPerMinuteProbability() {
        _updatePeakFreq();
        return _peaksPerMinuteProbability;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param frequency          frequency of band pass filter [Hz], set 0 to skip filtering
     * @param bandWidth          half band width of band pass filter [oct.]
     * @param windowLength       window length of simple moving avarage [ms]
     * @param signalToNoiseRatio S/N ratio for peak detection [dB]
     */
    public PeakDetector(double frequency, double bandWidth, int windowLength, double signalToNoiseRatio /* = 20 */) {
        _frequency = frequency;
        _bandWidth = bandWidth;
        _windowLength = windowLength;
        _signalToNoiseRatio = signalToNoiseRatio;
        _updateFilter();
        _resetWindow();
        _profileDirty = true;
        _peakListDirty = true;
        _peakFreqDirty = true;
        _average = 0;
    }

    // methods
    //

    /**
     * set analyzing source samples
     *
     * @param samples      analyzing source
     * @param channelCount channel count of analyzing source
     * @param isStreaming  true to continuous data with previous analyze
     * @return this instance
     */
    public PeakDetector setSamples(double[] samples, int channelCount, boolean isStreaming) {
        _peakFreqDirty = _peakListDirty = _profileDirty = true;
        _samples = samples;
        _samplesChannelCount = channelCount;
        if (!isStreaming) _resetWindow();
        return this;
    }

    /**
     * calcuate peak inetncity
     *
     * @param peakPosition    peak positoin [ms]
     * @param integrateLength integration length [ms]
     */
    public double calcPeakIntencity(double peakPosition, double integrateLength) {
        int i, n;
        int imin = (int) (peakPosition * 2.1 + 0.5);
        int imax = (int) ((peakPosition + integrateLength) * 2.1 + 0.5);
        _updateProfile();
        if (imin > _profile.length) imin = _profile.length;
        if (imax > _profile.length) imax = _profile.length;
        for (n = 0, i = imin; i < imax; i++) n += _profile[i];
        return n;
    }

    /**
     * merage peak list
     *
     * @param arrayOfPeakList  Array of peakList(Vector.&lt;Number&gt; type) to marge
     * @param singlePeakLength time distance to merge near ((1) peaks) peak
     * @return merged peak list
     */
    public double[] mergePeakList(Object[] arrayOfPeakList, double singlePeakLength) {
        int listIndex, peakListCount, i;
        double currentPosition, nextPeakPosition;
        int nextPeakHolder;
        double[] list;
        int[] idx;
        peakListCount = arrayOfPeakList.length;
        idx = new int[peakListCount];
        List<Double> merged = new ArrayList<>();

        for (i = 0; i < peakListCount; i++) idx[i] = 0;
        currentPosition = -singlePeakLength;
        while (true) {
            nextPeakPosition = 99999999;
            nextPeakHolder = -1;
            for (listIndex = 0; listIndex < peakListCount; listIndex++) {
                list = (double[]) arrayOfPeakList[listIndex];
                if (idx[listIndex] < list.length && list[idx[listIndex]] < nextPeakPosition) {
                    nextPeakPosition = list[idx[listIndex]];
                    nextPeakHolder = listIndex;
                }
            }
            if (nextPeakHolder != -1) {
                idx[nextPeakHolder]++;
                if (nextPeakPosition - currentPosition >= singlePeakLength) {
                    merged.add(nextPeakPosition);
                    currentPosition = nextPeakPosition;
                }
            } else break; // finished
        }

        return merged.stream().mapToDouble(d -> d).toArray();
    }

    // internals
    //

    // reset window buffer
    private void _resetWindow() {
        if (_window != null) SLLNumber.freeRing(_window);
        _window = SLLNumber.allocRing((int) (_windowLength * 2.1 + 0.5), 0);
    }

    // update filter parameters
    private void _updateFilter() {
        if (_frequency > 0) {
            _bpf.initialize();
            _bpf.setParameters(_frequency, _bandWidth);
        }
    }

    // update power prof.
    private void _updateProfile() {
        if (_profileDirty && _samples != null) {
            int imax, i, ix2, ix42, pow;
            double n;

            // copy samples to working area (_stream)
            imax = _samples.length;
            if (_samplesChannelCount == 1) { // monaural input
                if (_stream == null || _stream.length < imax * 2) _stream = new double[imax * 2];
                for (ix2 = i = 0; i < imax; i++) {
                    _stream[ix2] = _samples[i];
                    ix2++;
                    _stream[ix2] = _samples[i];
                    ix2++;
                }
            } else { // stereo input
                if (_stream == null || _stream.length < imax) _stream = new double[imax];
                for (i = 0; i < imax; ) {
                    n = _samples[i];
                    i++;
                    n += _samples[i];
                    i--;
                    n *= 0.5;
                    _stream[i] = n;
                    i++;
                    _stream[i] = n;
                    i++;
                }
            }

            // filtering
            if (_frequency > 0) {
                _bpf.prepareProcess();
                _bpf.process(1, _stream, 0, _stream.length >> 1);
            }

            // calculate power profile
            imax = (_stream.length - 41) / 42;
            if (_profile == null || _profile.length < imax) _profile = new double[imax];
            pow = 0;
            _average = 0;
            _maximum = 0;
            for (i = ix42 = 0; i < imax; i++) {
                // 44100/21 = 2100fps
                _window.n = _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                _window.n += _stream[ix42] * _stream[ix42];
                ix42 += 2;
                pow += _window.n;
                _window = _window.next;
                pow -= _window.n;
                _profile[i] = pow;
                _average += pow;
                if (_maximum < pow) _maximum = pow;
            }
            _average /= imax;

            _profileDirty = false;
        }
    }

    // update DLP and peakList
    private void _updatePeakList() {
        _updateProfile();
        if (_peakListDirty && _profile != null && _profile.length > 0) {
            int imax = _profile.length,
                    wnd = (int) (_windowLength * 2.1 + 0.5),
                    i, i1, prevPoint;
            double thres = _maximum * 0.001,
                    snr = Math.pow(10, _signalToNoiseRatio * 0.1),
                    decay = Math.pow(2, -1.0 / wnd),
                    n, envelope;

            if (_diffLogProfile == null || _diffLogProfile.length < imax) _diffLogProfile = new double[imax];
            _diffLogProfile[0] = 0;
            for (i = 1; i < imax; i++) {
                i1 = i - 1;
                _diffLogProfile[i] = (_profile[i1] > thres) ? (_profile[i] / _profile[i1] - 1) : 0;
            }

            List<Double> peakListBuilder = new ArrayList<>();
            envelope = 0;
            prevPoint = 0;
            for (i = wnd; i < imax; i++) {
                if (_diffLogProfile[i] > envelope) {
                    n = _diffLogProfile[i - wnd];
                    if (n <= 0) n = 0.001;
                    n = _diffLogProfile[i] / n;
                    if (n > snr) {
                        if (i - prevPoint < wnd) {
                            peakListBuilder.set(peakListBuilder.size() - 1, i / 2.1);
                        } else {
                            peakListBuilder.add(i / 2.1);
                        }
                        prevPoint = i;
                        envelope = _diffLogProfile[i];
                    }
                }
                envelope *= decay;
            }
            _peakList = peakListBuilder.stream().mapToDouble(d -> d).toArray();
            _peakListDirty = false;
        }
    }

    // update peak frequency
    private void _updatePeakFreq() {
        _updatePeakList();
        if (_peakFreqDirty && _profile.length > 0) {
            int i, j, highScoreFrames, total, frm, score;
            _ppmScore = calcPeaksPerMinuteEstimationScoreTable(_peakList, _ppmScore);
            _estimatePeaksPerMinuteFromScoreTable();
            _peakFreqDirty = false;
        }
    }

    // estimate peaks per minute from score table
    private void _estimatePeaksPerMinuteFromScoreTable() {
        int highScoreFrames, i, imax, j, frm;
        double thres;
        double pmin, pmax;
        // find highest score
        for (highScoreFrames = 100, i = 101; i < 2000; i++) {
            if (_ppmScore[i] > _ppmScore[highScoreFrames]) highScoreFrames = i;
        }
        // move finding peak to less than 200ppm (630frames)
        while (highScoreFrames < 630) highScoreFrames *= 2;
        // move to peak top
        while (_ppmScore[highScoreFrames] < _ppmScore[highScoreFrames + 1]) highScoreFrames++;
        while (_ppmScore[highScoreFrames] < _ppmScore[highScoreFrames - 1]) highScoreFrames--;
        // calculate cross point of [peak height] * 0.7
        thres = _ppmScore[highScoreFrames] * 0.7;
        pmin = 0;
        imax = highScoreFrames - 100;
        for (i = highScoreFrames; i > imax; i--) {
            if (_ppmScore[i] < thres) {
                pmin = i + (thres - _ppmScore[i]) / (_ppmScore[i + 1] - _ppmScore[i]);
                break;
            }
        }
        pmax = 0;
        imax = highScoreFrames + 100;
        for (i = highScoreFrames; i < imax; i++) {
            if (_ppmScore[i] < thres) {
                pmax = i + (_ppmScore[i - 1] - thres) / (_ppmScore[i - 1] - _ppmScore[i]);
                break;
            }
        }
        // calcualte peak top again and translate to peaks per minute value
        if (pmin != 0 && pmax != 0) _peaksPerMinute = (highScoreFrames > 0) ? (2100 * 60 / ((pmax + pmin) * 0.5)) : 0;
        else _peaksPerMinute = (highScoreFrames > 0) ? (2100. * 60 / highScoreFrames) : 0;
        // move range into maxPeaksPerMinute
        double minPeaksPerMinute = maxPeaksPerMinute * 0.5;
        while (_peaksPerMinute >= maxPeaksPerMinute) _peaksPerMinute *= 0.5;
        while (_peaksPerMinute < minPeaksPerMinute) _peaksPerMinute *= 2;
        // integrate peaks to calculate probability
        _peaksPerMinuteProbability = 0;
        for (i = 0; i < 10; i++) {
            frm = (int) (highScoreFrames * _probCheck[i]);
            if (frm > 2100) break;
            for (j = -22; j < 23; j++) _peaksPerMinuteProbability += _ppmScore[frm + j];
        }
    }

    private static final double[] _probCheck = {0.25, 0.5, 1, 2, 3, 4, 5, 6, 7, 8};

    /**
     * @param peakList   peal list [ms]
     * @param scoreTable score table instance to set, null to create new table.
     * @return score table
     * @internal caclulate peak frequency estimation scores
     */
    public double[] calcPeaksPerMinuteEstimationScoreTable(double[] peakList, double[] scoreTable) {
        int i, j, k, s, peakCount, peakDist;
        double dist, dist2, scale, scoreTotal;
        if (scoreTable == null) scoreTable = new double[2124];

        // clear score table
        for (i = 0; i < 2124; i++) scoreTable[i] = 0;

        // calculate scores
        peakCount = peakList.length;
        for (i = 0; i < peakCount; i++) {
            for (j = i + 1; j < peakCount; j++) {
                dist = peakList[j] - peakList[i];
                if (dist < 48) continue;
                if (dist > 1000) break;
                scale = 1;
                for (k = j + 1; k < peakCount; k++) {
                    dist2 = (peakList[k] - peakList[j]) / dist + 0.1;
                    dist2 -= (int) (dist2);
                    if (dist2 < 0.2) {
                        dist2 -= 0.1;
                        if (dist2 < 0) dist2 = -dist2;
                        scale += _normalDist[(int) (dist2 * 20)] * 0.01;
                    }
                }
                peakDist = (int) (dist * 2.1 + 0.5);
                scoreTable[peakDist] += _normalDist[0] * scale;
                for (k = 1; k < 20; k++) {
                    s = peakDist + k;
                    scoreTable[s] += _normalDist[k] * scale;
                    s = peakDist - k;
                    scoreTable[s] += _normalDist[k] * scale;
                }
            }
        }

        // normalize
        for (scoreTotal = 0, i = 0; i < 2124; i++) scoreTotal += scoreTable[i];
        for (scale = 1 / scoreTotal, i = 0; i < 2124; i++) scoreTable[i] *= scale;
        return scoreTable;
    }

    private static final int[] _normalDist = {100, 99, 95, 89, 81, 73, 63, 53, 44, 35, 28, 21, 16, 11, 8, 6, 4, 2, 2, 1};
}
