//
// Monophonic synthesizer class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.List;

import org.si.sound.patterns.Note;
import org.si.sound.patterns.Sequencer;
import org.si.sound.synthesizers.VoiceReference;


/**
 * Monophonic synthesizer class provides single voice synthesizer sounding on the beat.
 */
public class MonophonicSynthesizer extends PatternSequencer {

    // variables
    //

    /** note object to sound on the beat */
    private Note _noteObject;

    // properties
    //

    /** current note in the sequence, you cannot change this property. */
    @Override
    public int getNote() {
        return (_track != null) ? _track.getNote() : _sequencer.getNote();
    }

    @Override
    public void setNote(int n) {
        _errorCannotChange("note");
    }

    /** Synchronizing quantizing, uint in 16th beat. (0:No synchronization, 1:sync.with 16th, 4:sync.with 4th). @default 0. */
    @Override
    public double getQuantize() {
        return _quantize;
    }

    @Override
    public void setQuantize(double q) {
        _quantize = q;
        _sequencer.gridStep = (int) (q * 120);
    }

    /** Sound delay, uint in 16th beat. @default 0. */
    @Override
    public double getDelay() {
        return _delay;
    }

    @Override
    public void setDelay(double d) {
        _errorCannotChange("delay");
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param synth synthesizer to play
     */
    MonophonicSynthesizer(VoiceReference synth) {
        super(60, 128, 0, synth);
        name = "MonophonicSynthesizer";
        _noteObject = new Note();
        _sequencer.pattern = List.of (_noteObject) ;
        _sequencer.onExitFrame = this::_onExitFrame;
    }

    // operations
    //

    /** start streaming without any sounds */
    @Override
    public void play() {
        super.play();
    }

    /** stop streaming */
    @Override
    public void stop() {
        super.stop();
    }

    /**
     * note on
     *
     * @param note     note number (0-127)
     * @param velocity velocity (0-128-255)
     * @param length   length (1 = 16th beat length)
     */
    public void noteOn(int note, int velocity, int length) {
        _noteObject.setNote(note, velocity, length, -1, Double.NaN, null);
    }

    /**
     * note off
     */
    public void noteOff() {
        if (_track != null) _track.keyOff(0, false);
    }

    // internal
    //

    /** */
    protected void _onExitFrame(Sequencer seq) {
        _noteObject.setRest();
    }
}
