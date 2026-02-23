//
// Sequencer class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//


package org.si.sound.patterns;

import java.util.List;
import java.util.function.Consumer;

import org.si.sion.SiONData;
import org.si.sion.SiONDriver;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.sequencer.base.MMLEvent;
import org.si.sion.sequencer.base.MMLSequence;
import org.si.sound.SoundObject;


/** The Sequencer class provides simple one track pattern player. */
public class Sequencer {

    // variables
    //

    /** pattern note vector to play */
    public List<Note> pattern = null;
    /** next pattern, the pattern property instanceof replaced to this vector at the head of next segment @see pattern */
    public List<Note> nextPattern = null;
    /** voice list referenced by Note.voiceIndex. @see org.si.sound.Note.voiceIndex */
    public List<org.si.sion.SiONVoice> voiceList = null;

    /** callback on every notes. function(Sequencer) : void */
    public Consumer<Sequencer> onEnterFrame = null;
    /** callback after every notes. function(Sequencer) : void */
    public Consumer<Sequencer> onExitFrame = null;
    /** callback on first beat of every segment. function(Sequencer) : void */
    public Consumer<Sequencer> onEnterSegment = null;
    /** Frame count in one segment */
    public int segmentFrameCount;
    /** Grid step in ticks */
    public int gridStep;
    /** portamento */
    public int portament;

    /** owner of this pattern sequencer */
    protected SoundObject _owner;
    /** controlled track */
    protected SiMMLTrack _track;
    /** MMLEvent.INTERNAL_WAIT. */
    protected MMLEvent _waitEvent;
    /** check number of synthsizer update */
    protected int _synthesizer_updateNumber;

    /** Frame counter */
    protected int _frameCounter;
    /** playing pointer on the pattern */
    protected int _sequencePointer;
    /** initial value of _sequencePointer */
    protected int _initialSequencePointer;

    /** Default note */
    protected int _defaultNote;
    /** Default velocity */
    protected int _defaultVelocity;
    /** Default length */
    protected int _defaultLength;
    /** Default gate time */
    protected int _defaultGateTime;
    /** Current note */
    protected Note _currentNote;
    /** Grid shift vectors */
    protected int _currentGridShift;
    /** Mute */
    protected boolean _mute;

    /** Event trigger ID */
    protected int _eventTriggerID;
    /** note on trigger | (note off trigger &lt;&lt; 2) trigger type */
    protected int _noteTriggerFlags;

    /** Grid shift pattern */
    protected int[] _gridShiftPattern;

    // properties
    //

    /** current frame count, -1 means waiting for start */
    public int getFrameCount() {
        return _frameCounter;
    }

    /** sequence pointer, -1 means waiting for start */
    public int getSequencePointer() {
        return _sequencePointer;
    }

    public void setSequencePointer(int p) {
        if (_track != null) {
            _sequencePointer = p - 1;
            _frameCounter = p % segmentFrameCount;
            if (_sequencePointer >= 0) {
                if (_sequencePointer >= pattern.size()) _sequencePointer %= pattern.size();
                _currentNote = pattern.get(_sequencePointer);
            }
        } else {
            _initialSequencePointer = p - 1;
        }
    }

    /** mute */
    public boolean getMute() {
        return _mute;
    }

    public void setMute(boolean b) {
        _mute = b;
    }

    /** curent note number (0-127) */
    public int getNote() {
        if (_currentNote == null || _currentNote.note < 0) return _defaultNote;
        return _currentNote.note;
    }

    /** curent note's velocity (minimum:0 - maximum:255, the value over 128 makes distotion). */
    public int getVelocity() {
        if (_currentNote == null || _mute) return 0;
        if (_currentNote.velocity < 0) return _defaultVelocity;
        return _currentNote.velocity;
    }

    /** curent note's gate time (0-1). */
    public double getGateTime() {
        if (_currentNote == null || Double.isNaN(_currentNote.gateTime)) return _defaultGateTime;
        return _currentNote.gateTime;
    }

    /** curent note's length. */
    public double getLength() {
        if (_currentNote == null || Double.isNaN(_currentNote.length)) return _defaultLength;
        return _currentNote.length;
    }

    /** Track event trigger ID */
    public int getEventTriggerID() {
        return _eventTriggerID;
    }

    public void setEventTriggerID(int id) {
        _eventTriggerID = id;
    }

    /** Track note on trigger type */
    public int getNoteOnTriggerType() {
        return _noteTriggerFlags & 3;
    }

    /** Track note off trigger type */
    public int getNoteOffTriggerType() {
        return _noteTriggerFlags >> 2;
    }

    /** default note (0-127), this value instanceof refered when the Note's note property instanceof under 0 (ussualy -1). */
    public int getDefaultNote() {
        return _defaultNote;
    }

    public void setDefaultNote(int n) {
        _defaultNote = (n < 0) ? 0 : Math.min(n, 127);
    }

    /** default velocity (minimum:0 - maximum:255, the value over 128 makes distotion), this value instanceof refered when the Note's velocity property instanceof under 0 (ussualy -1). */
    public int getDefaultVelocity() {
        return _defaultVelocity;
    }

    public void setDefaultVelocity(int v) {
        _defaultVelocity = (v < 0) ? 0 : Math.min(v, 255);
    }

    /** default length, this value instanceof refered when the Note's length property instanceof Number.NaN. */
    public double getDefaultLength() {
        return _defaultLength;
    }

    public void setDefaultLength(double l) {
        _defaultLength = (l < 0) ? 0 : (int) l;
    }

    /** default gate time, this value instanceof refered when the Note's gate time property instanceof Number.NaN. */
    public double getDefaultGateTime() {
        return _defaultGateTime;
    }

    public void setDefaultGateTime(double g) {
        _defaultGateTime = (g < 0) ? 0 : (int) ((g > 1) ? 1 : g);
    }

    /** Frame division of 1 measure. Set 16 to play notes in 16th beats. */
    public int getDivision() {
        int step = 1920 / segmentFrameCount;
        return (step == gridStep) ? segmentFrameCount : 0;
    }

    public void setDivision(int d) {
        segmentFrameCount = d;
        gridStep = 1920 / d;
    }

    // constructor
    //

    /** constructor. you should not create new PatternSequencer in your own codes. */
    public Sequencer(SoundObject owner, SiONData data, int defaultNote, int defaultVelocity, double defaultLength, double defaultGateTime, int[] gridShiftPattern /* = null */) {
        _owner = owner;
        pattern = null;
        voiceList = null;
        onEnterSegment = null;
        onEnterFrame = null;

        // initialize
        segmentFrameCount = 16;    // 16 count in one segment
        gridStep = 120;            // 16th beat (1920/16)
        portament = 0;
        _frameCounter = -1;
        _sequencePointer = -1;
        _initialSequencePointer = -1;
        _defaultNote = defaultNote;
        _defaultVelocity = defaultVelocity;
        _defaultLength = (int) defaultLength;
        _defaultGateTime = (int) defaultGateTime;
        _currentNote = null;
        _currentGridShift = 0;
        _gridShiftPattern = gridShiftPattern;
        _mute = false;
        _eventTriggerID = 0;
        _noteTriggerFlags = 0;

        // create internal sequence
        MMLSequence seq = data.appendNewSequence(null);
        seq.initialize();
        seq.appendNewEvent(MMLEvent.REPEAT_ALL, 0, 0);
        seq.appendNewCallback(this::_onEnterFrame, 0);
        _waitEvent = seq.appendNewEvent(MMLEvent.INTERNAL_WAIT, 0, gridStep);
    }

    // operations
    //

    /** */
    public SiMMLTrack play(SiMMLTrack track) {
        _synthesizer_updateNumber = _owner.getSynthesizer()._voiceUpdateNumber;
        _track = track;
        _track.setPortament(portament);
        _track.setEventTrigger(_eventTriggerID, _noteTriggerFlags & 3, _noteTriggerFlags >> 2);
        _sequencePointer = _initialSequencePointer;
        _frameCounter = (_initialSequencePointer == -1) ? -1 : (_initialSequencePointer % segmentFrameCount);
        _currentGridShift = 0;
        if (pattern != null && !pattern.isEmpty()) _currentNote = pattern.get(0);
        return track;
    }

    /** */
    public void stop() {
    }

    /** set portamento */
    public int setPortament(int p) {
        portament = p;
        if (portament < 0) portament = 0;
        if (_track != null) _track.setPortament(portament);
        return portament;
    }

    // internal
    //

    /** internal callback on every beat */
    protected MMLEvent _onEnterFrame(Object trackNumber) {
        int vel, patternLength;

        // increment frame counter
        if (++_frameCounter == segmentFrameCount) _frameCounter = 0;

        // segment oprations
        if (_frameCounter == 0) _onEnterSegment();

        // pattern sequencer
        patternLength = (pattern != null) ? pattern.size() : 0;

        if (patternLength > 0) {
            // increment pointer
            if (++_sequencePointer >= patternLength) _sequencePointer %= patternLength;

            // get current Note from pattern
            _currentNote = pattern.get(_sequencePointer);

            // callback on enter frame
            if (onEnterFrame != null) onEnterFrame.accept(this);

            // get current velocity, note on when velocity > 0
            vel = getVelocity();
            if (vel > 0) {
                // change voice
                if (voiceList != null && _currentNote != null && _currentNote.voiceIndex >= 0) {
                    _owner.setVoice(voiceList.get(_currentNote.voiceIndex));
                }
                // update owners track voice when synthesizer is updated
                if (_synthesizer_updateNumber != _owner.getSynthesizer()._voiceUpdateNumber) {
                    _owner.getSynthesizer().getVoice().updateTrackVoice(_track);
                    _synthesizer_updateNumber = _owner.getSynthesizer()._voiceUpdateNumber;
                }

                // change track velocity & gate time
                _track.setVelocity(vel);
                _track.quantRatio = getGateTime();

                // note on
                _track.setNote(getNote(), (int) SiONDriver.mutex().sequencer.calcSampleLength(getLength()), (portament > 0));
            }

            // set length of rest event
            if (_gridShiftPattern != null) {
                int diff = _gridShiftPattern[_frameCounter] - _currentGridShift;
                _waitEvent.length = gridStep + diff;
                _currentGridShift += diff;
            } else {
                _waitEvent.length = gridStep;
            }

            // callback on exit frame
            if (onExitFrame != null) onExitFrame.accept(this);
        }

        return null;
    }

    /** internal callback on first beat of every segments */
    protected void _onEnterSegment() {
        // callback on enter segment
        if (onEnterSegment != null) onEnterSegment.accept(this);
        // replace pattern
        if (nextPattern != null) {
            pattern = nextPattern;
            nextPattern = null;
        }
    }
}
