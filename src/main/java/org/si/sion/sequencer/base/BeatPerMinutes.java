//
// Beat per minutes data
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

/** Beat per minutes class, Calculates BPM-releated numbers automatically. */
public class BeatPerMinutes {

    /** 16th beat per sample */
    public double beat16PerSample;
    /** sample per 16th beat */
    public double samplePerBeat16;
    /** tick per sample */
    public double tickPerSample;
    /** @private [internal] sample per tick in FIXED unit. */
    double _samplePerTick;
    // beat per minutes
    public double _bpm = 0;
    // sample rate
    private int _sampleRate = 0;
    // tick resolution
    private int _resolution;

    /** beat per minute. */
    public double getBpm() {
        return _bpm;
    }

    /** sampling rate */
    public int getSampleRate() {
        return _sampleRate;
    }

    /** constructor. */
    public BeatPerMinutes(double bpm, int sampleRate, int resolution /* = 1920 */) {
        _resolution = resolution;
        update(bpm, sampleRate);
    }

    /** update */
    public boolean update(double beatPerMinutes, int sampleRate) {
        if (beatPerMinutes < 1) beatPerMinutes = 1;
        else if (beatPerMinutes > 511) beatPerMinutes = 511;
        if (beatPerMinutes != _bpm || sampleRate != _sampleRate) {
            _bpm = beatPerMinutes;
            _sampleRate = sampleRate;
            tickPerSample = _resolution * _bpm / (_sampleRate * 240);
            beat16PerSample = _bpm / (_sampleRate * 15); // 60/4
            samplePerBeat16 = 1 / beat16PerSample;
            _samplePerTick = (int) ((1 / tickPerSample) * (1 << MMLSequencer.FIXED_BITS));
            return true;
        }
        return false;
    }
}
