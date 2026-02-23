//
// Pattern sequencer class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.si.sion.SiONData;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sound.patterns.Sequencer;
import org.si.sound.synthesizers.VoiceReference;


/**
 * Pattern sequencer class provides simple one track pattern player.
 * The sequence pattern instanceof ((Vector) represented).&lt;Note&gt;.
 * <p>
 * Simple usage
 * <pre>
 * // create new instance
 * PatternSequencer ps = new PatternSequencer();
 *
 * // set sequence pattern by Note vector
 * Vector pat.&lt;Note&gt; = new Vector.&lt;Note&gt;();
 * pat.add(new Note(60, 64, 1));  // note C
 * pat.add(new Note(62, 64, 1));  // note D
 * pat.add(new Note(64, 64, 2));  // note E with length of 2
 * pat.add(null);                 // rest; null means no operation
 * pat.add(new Note(62, 64, 2));  // note D with length of 2
 * pat.add(new Note().setRest()); // rest; Note.setRest() method set no operation
 *
 * // PatternSequencer.sequencer is the sound player
 * ps.sequencer.pattern = pat;
 *
 * // play sequence "l16 $cde8d8" in MML
 * ps.play();
 * </pre>
 * @see org.si.sound.patterns.Note
 */
public class PatternSequencer extends SoundObject {

    // variables
    //

    /** Sequencer instance */
    protected Sequencer _sequencer;
    /** Sequence data */
    protected SiONData _data;

    /** */
    protected Function<Object, Object> _callbackEnterFrame = null;
    /** */
    protected Function<Object, Object> _callbackEnterSegment = null;

    // properties
    //

    /** the Sequencer instance belonging to this PatternSequencer, where the sequence pattern appears. */
    public Sequencer getSequencer() {
        return _sequencer;
    }

    /** portamento */
    public int getPortament() {
        return _sequencer.portament;
    }

    public void setPortament(int p) {
        _sequencer.setPortament(p);
    }

    /** current note in the sequence, you cannot change this property. */
    @Override
    public int getNote() {
        return _sequencer.getNote();
    }

    @Override
    public void setNote(int n) {
        throw _errorCannotChange("note");
    }

    /** current length in the sequence, you cannot change this property. */
    @Override
    public double getLength() {
        return _sequencer.getLength();
    }

    @Override
    public void setLength(double l) {
        throw _errorCannotChange("length");
    }

    /** current length in the sequence, you cannot change this property. */
    @Override
    public void setGateTime(double g) {
        _sequencer.setDefaultGateTime(_gateTime = (g < 0) ? 0 : (g > 1) ? 1 : g);
        //if (_track) _track.quantRatio = _gateTime;
    }

    /** callback on enter frame */
    public Function<Object, Object> getOnEnterFrame() {
        return _callbackEnterFrame;
    }

    public void setOnEnterFrame(Function<Object, Object> f) {
        _callbackEnterFrame = f;
    }

    /** callback on enter segment */
    public Function<Object, Object> getOnEnterSegment() {
        return _callbackEnterSegment;
    }

    public void setOnEnterSegment(Function<Object, Object> f) {
        _callbackEnterSegment = f;
    }

    /** callback on exit frame */
    public Consumer<Sequencer> getOnExitFrame() {
        return _sequencer.onExitFrame;
    }

    public void setOnExitFrame(Consumer<Sequencer> f) {
        _sequencer.onExitFrame = f;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param defaultNote     Default note, this value instanceof referenced when Note.note property is -1.
     * @param defaultVelocity Default velocity, this value instanceof referenced when Note.velocity property is -1.
     * @param defaultLength   Default length, this value instanceof referenced when Note.length property instanceof Number.NaN.
     * @param synth           synthesizer to play
     */
    PatternSequencer(int defaultNote /* = 50 */, int defaultVelocity /* = 128 */, double defaultLength /* 0 */, VoiceReference synth /* = null */) {
        super("PatternSequencer", synth);
        _data = new SiONData();
        _sequencer = new Sequencer(this, _data, defaultNote, defaultVelocity, defaultLength, Double.NaN, null);
        _sequencer.onEnterFrame = this::_onEnterFrame;
        _sequencer.onEnterSegment = this::_onEnterSegment;
    }

    // operations
    //

    /** start sequence */
    @Override
    public void play() {
        stop();
        List<SiMMLTrack> list = _sequenceOn(_data, false, false);
        if (!list.isEmpty()) {
            _track = _sequencer.play(list.get(0));
            _synthesizer._registerTrack(_track);
        }
    }

    /** stop sequence */
    @Override
    public void stop() {
        if (_track != null) {
            _sequencer.stop();
            _synthesizer._unregisterTracks(_track, 1);
            _track.setDisposable();
            _track = null;
            _sequenceOff(true);
        }
        _stopEffect();
    }

    // internal
    //

    /** handler on enter segment */
    @Override
    protected void _onEnterFrame(Sequencer seq) {
        if (_callbackEnterFrame != null) _callbackEnterFrame.apply(seq);
        super._onEnterFrame(seq);
    }

    /** handler on enter segment */
    @Override
    protected void _onEnterSegment(Sequencer seq) {
        if (_callbackEnterSegment != null) _callbackEnterSegment.apply(seq);
        super._onEnterSegment(seq);
    }
}
