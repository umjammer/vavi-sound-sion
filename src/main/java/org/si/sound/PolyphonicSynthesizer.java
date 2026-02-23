//
// Polyphonic synthesizer class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.sequencer.SiMMLTrack;
import org.si.sound.synthesizers.VoiceReference;


/**
 * Polyphonic synthesizer class provides synthesizer with multitracks.
 */
public class PolyphonicSynthesizer extends MultiTrackSoundObject {

    // variables
    //

    // properties
    //

    // constructor
    //

    /**
     * constructor
     *
     * @param synth synthesizer to play
     */
    PolyphonicSynthesizer(VoiceReference synth) {
        super("PolyphonicSynthesizer", synth);
    }

    // operations
    //

    /** Reset */
    @Override
    public void reset() {
        super.reset();
    }

    /** start streaming without any sounds */
    @Override
    public void play() {
        _stopAllTracks();
        _tracks = new ArrayList<>();
    }

    /** stop all tracks */
    @Override
    public void stop() {
        _stopAllTracks();
    }

    /**
     * note on
     *
     * @param note     note number (0-128)
     * @param velocity velocity (0-128-255)
     * @param length   length (1 = 16th beat length)
     */
    public void noteOn(int note, int velocity, int length) {
        if (_tracks != null) {
            _length = length;
            _note = note;
            _track = _noteOn(_note, false);
            if (_track != null) _synthesizer._registerTrack(_track);
            _track.setVelocity(velocity);
            _tracks.add(_track);
        }
    }

    /**
     * note off
     *
     * @param note note number to sound off (0-127)
     */
    public void noteOff(int note, boolean stopWithReset) {
        List<SiMMLTrack> noteOffTracks = _noteOff(note, stopWithReset);
        for (SiMMLTrack t : noteOffTracks) {
            _synthesizer._unregisterTracks(t, 1);
            t.setDisposable();
        }
    }

    // internal
    //
}
