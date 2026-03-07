//
// BPM analyzer
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import org.si.as3.media.Sound;


/** BPMAnalyzer analyzes beat per minute value of music */
public class BPMAnalyzer {

    // variables
    //

    /** filter banks, 5000Hz, 2400Hz, 100Hz @ default. */
    public PeakDetector[] filterbanks;

    private int _bpm;
    private double _bpmProbability;
    private int _pickedupCount;
    private final int[] _pickedupBPMList = new int[10];
    private final double[] _pickedupBPMProbabilityList = new double[10];
    private int _snapShotIndex;

    // properties
    //

    /** estimated bpm */
    public int getBpm() {
        return _bpm;
    }

    /** estimated bpm's probability */
    public double getBpmProbability() {
        return _bpmProbability;
    }

    /** number of picked up point */
    public int getPickedupCount() {
        return _pickedupCount;
    }

    /** picked up bpm list */
    public int[] getPickedupBPMList() {
        return _pickedupBPMList;
    }

    /** picked up bpm's probability list */
    public double[] getPickedupBPMProbabilityList() {
        return _pickedupBPMProbabilityList;
    }

    /** starting position that has maximum probability */
    public double getSnapShotPosition() {
        return _snapShotIndex * 0.000022675736961451247;
    } // 1/44100

    // constructor
    //

    /**
     * constructor
     *
     * @param filterbankCount Number of filter bank for analysis (1-4).
     */
    public BPMAnalyzer(int filterbankCount) {
        if (filterbankCount < 1 || filterbankCount > 4) filterbankCount = 4;
        filterbanks = new PeakDetector[filterbankCount];
        filterbanks[0] = new PeakDetector(5000, 0.50, 25, 20);
        if (filterbankCount > 1) filterbanks[1] = new PeakDetector(2400, 0.50, 25, 20);
        if (filterbankCount > 2) filterbanks[2] = new PeakDetector(100, 0.50, 40, 20);
        if (filterbankCount > 3) filterbanks[3] = new PeakDetector(0, 0.5, 20, 20);
    }

    // methods
    //

    /**
     * estimate BPM from Sound
     *
     * @param sound                       sound to analyze
     * @param rememberFilterbanksSnapShot remember filterbanks status that has the biggest probability
     * @return estimated bpm value
     */
    public int estimateBPM(Sound sound, boolean rememberFilterbanksSnapShot) {
        int pickupIndex, pickupStep, i, maxProb, thres;
        double[] probs = _pickedupBPMProbabilityList;
        int[] bpms = _pickedupBPMList;
        double[] scores;

        _pickedupCount = (int) (sound.length / 20000);
        if (_pickedupCount == 0) _pickedupCount = 1;
        else if (_pickedupCount > 10) _pickedupCount = 10;
        scores = new double[100];

        pickupStep = (int) ((sound.length - _pickedupCount * 4000) * 44.1 / (_pickedupCount + 1));
        if (pickupStep < 0) pickupStep = 0;
        maxProb = 0;

        for (pickupIndex = pickupStep, i = 0; i < _pickedupCount; i++, pickupIndex += 176400 + pickupStep) {
            _estimateBPMFromSamples(SiONUtil.extract(sound, null, 1, 176400, pickupIndex), 1);
            probs[i] = _bpmProbability;
            bpms[i] = _bpm;
            if (maxProb < _bpmProbability) {
                maxProb = (int) _bpmProbability;
                _snapShotIndex = pickupIndex;
            }
        }
        _bpmProbability = maxProb;

        thres = (int) (maxProb * 0.75);
        for (i = 0; i < _pickedupCount; i++) {
            if (probs[i] > thres && 100 <= bpms[i] && bpms[i] < 200) scores[bpms[i] - 100] += probs[i];
        }
        maxProb = 0;
        for (i = 0; i < 100; i++) {
            if (maxProb < scores[i]) {
                maxProb = (int) scores[i];
                _bpm = i + 100;
            }
        }

        if (rememberFilterbanksSnapShot)
            _estimateBPMFromSamples(SiONUtil.extract(sound, null, 1, 176400, _snapShotIndex), 1);

        return _bpm;
    }

    /**
     * estimate BPM from samples
     *
     * @param sample   samples to analyze
     * @param channels channel count of samples
     * @return estimated bpm value
     */
    public int estimateBPMFromSamples(double[] sample, int channels) {
        _pickedupCount = 0;
        _estimateBPMFromSamples(sample, channels);
        return _bpm;
    }

    // internal
    //
    // estimate BPM from samples
    private void _estimateBPMFromSamples(double[] sample, int channels) {
        PeakDetector pd1;
        PeakDetector pd2;
        int pmp, pmr, bpm;
        int i, banksCount = filterbanks.length;

        // set samples to filter banks
        for (i = 0; i < banksCount; i++) filterbanks[i].setSamples(sample, channels, false);

        // pick up 1st and 2nd acculate filterbanks
        if (banksCount > 1) {
            // pick up 2 banks
            if (filterbanks[0].getPeaksPerMinuteProbability() < filterbanks[1].getPeaksPerMinuteProbability()) {
                pd1 = filterbanks[1];
                pd2 = filterbanks[0];
            } else {
                pd1 = filterbanks[0];
                pd2 = filterbanks[1];
            }
            for (i = 2; i < banksCount; i++) {
                if (pd2.getPeaksPerMinuteProbability() < filterbanks[i].getPeaksPerMinuteProbability()) {
                    if (pd1.getPeaksPerMinuteProbability() < filterbanks[i].getPeaksPerMinuteProbability()) {
                        pd2 = pd1;
                        pd1 = filterbanks[i];
                    } else {
                        pd2 = filterbanks[i];
                    }
                }
            }
            // estimate bpm
            pmp = (int) (pd1.getPeaksPerMinuteProbability() / pd2.getPeaksPerMinuteProbability());
            pmr = (int) (pd1.getPeaksPerMinute() / pd2.getPeaksPerMinute());
            if (pmp > 1.333 || pmr > 1.1 || pmr < 0.9) bpm = (int) pd1.getPeaksPerMinute();
            else bpm = (int) ((pd1.getPeaksPerMinute() + pd2.getPeaksPerMinute()) * 0.5);
            _bpm = (int) (bpm + 0.5);
            _bpmProbability = pd1.getPeaksPerMinuteProbability();
        } else {
            // only one bank
            _bpm = (int) filterbanks[0].getPeaksPerMinute();
            _bpmProbability = filterbanks[0].getPeaksPerMinuteProbability();
        }
    }
}
