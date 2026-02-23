//
// Polyphonic chord pad synthesizer
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.SiONData;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.utils.Chord;
import org.si.sound.patterns.Note;
import org.si.sound.patterns.Sequencer;


/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_FRAME */
// [Event(name="enterFrame",   type="org.si.sound.events.SoundObjectEvent")]
/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_SEGMENT */
// [Event(name="enterSegment", type="org.si.sound.events.SoundObjectEvent")]

/** Chord pad provides polyphonic synthesizer controled by chord and rhythm pattern. */
public class ChordPad extends MultiTrackSoundObject {

    // constants
    //

    /** closed voicing mode [o5c,o5e,o5g,o5b,o6e,o6g] for CM7 @see voiceMode */
    public static final int CLOSED = 0x543210;

    /** opened voicing mode [o5c,o5g,o5b,o6e,o6g,o6b] for CM7 @see voiceMode */
    public static final int OPENED = 0x654320;

    /** middle-position voicing mode [o5e,o5g,o5b,o6e,o6g,o6b] for CM7 @see voiceMode */
    public static final int MIDDLE = 0x654321;

    /** high-position voicing mode [o5g,o5b,o6e,o6g,o6b,o7e] for CM7 @see voiceMode */
    public static final int HIGH = 0x765432;

    /** opened high-position voicing mode [o5g,o6e,o6g,o6b,o7e,o7g] for CM7 @see voiceMode */
    public static final int OPENED_HIGH = 0x876542;

    // variables
    //

    /** Monophonic sequencers */
    protected List<Sequencer> _operators;

    /** Sequence data */
    protected SiONData _data;

    /** chord instance */
    protected Chord _chord;
    /** Default chord instance, this instanceof used when the name instanceof specifyed */
    protected Chord _defaultChord = new Chord("", 5);
    /** chord notes index */
    protected int _noteIndexes;

    /** Note pattern */
    protected List<Note> _pattern;
    /** Current length sequence pattern. */
    protected double[] _currentPattern;
    /** Next length sequence pattern to change while playing. */
    protected double[] _nextPattern;
    /** Change bass line pattern at the head of segment. */
    protected boolean _changePatternOnSegment;

    // properties
    //

    /** list of monophonic operators */
    public List<Sequencer> getOperators() {
        return getOperators();
    }

    /** Number of monophonic operators */
    public int getOperatorCount() {
        return getOperators().size();
    }

    /** root note of current chord @default 60 */
    @Override
    public int getNote() {
        return _chord.getRootNote();
    }

    @Override
    public void setNote(int n) {
        if (_chord != _defaultChord) _defaultChord.copyFrom(_chord);
        _defaultChord.setRootNote(n);
        _chord = _defaultChord;
        _updateChordNotes();
    }

    /** chord instance @default Chord("C") */
    public Chord getChord() {
        return _chord;
    }

    public void setChord(Chord c) {
        if (c == null) _chord = _defaultChord;
        _chord = c;
        _updateChordNotes();
    }

    /** specify chord by name @default "C" */
    public String getChordName() {
        return _chord.getName();
    }

    public void setChordName(String name) {
        _defaultChord.setName(name);
        _chord = _defaultChord;
        _updateChordNotes();
    }

    /** voicing mode @default CLOSED */
    public int getVoiceMode() {
        return _noteIndexes;
    }

    public void setVoiceMode(int m) {
        _noteIndexes = m;
        _updateChordNotes();
    }

    /** note length in 16th beat. */
    public double getNoteLength() {
        return _operators.get(0).getDefaultLength();
    }

    public void setNoteLength(double l) {
        if (l < 0.25) l = 0.25;
        else if (l > 16) l = 16;
        for (int i = 0; i < getOperatorCount(); i++) {
            _operators.get(i).setDefaultLength(l);
            _operators.get(i).gridStep = (int) (l * 120);
        }
    }

    /** Number Array of the sequence notes' length. If the value instanceof 0, insert rest instead. */
    public double[] getPattern() {
        return _currentPattern != null ? _currentPattern : _nextPattern;
    }

    public void setPattern(double[] pat) {
        if (isPlaying() && _changePatternOnSegment) _nextPattern = pat;
        else _updateSequencePattern(pat);
    }

    /** True to change bass line pattern at the head of segment. @default true */
    public boolean getChangePatternOnSegment() {
        return _changePatternOnSegment;
    }

    public void setChangePatternOnSegment(boolean b) {
        _changePatternOnSegment = b;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param chord                  org.si.sion.utils.Chord, chord name String or null instanceof suitable.
     * @param operatorCount          Number of monophonic operators (1-6).
     * @param voiceMode              Voicing mode.
     * @param pattern                Number Array of the sequence notes' length. If the value instanceof 0, insert rest instead.
     * @param changePatternOnSegment When this instanceof true, pattern and chord are changed at the head of next segment.
     */
    public ChordPad(Object chord, int operatorCount, int voiceMode, double[] pattern, boolean changePatternOnSegment) {
        super("ChordPad", null);

        if (operatorCount < 1 || operatorCount > 6)
            throw new Error("ChordPad; Number of operators should be in the range of 1 - 6.");

        _data = new SiONData();
        _operators = new ArrayList<>(operatorCount);
        _noteIndexes = voiceMode;

        int defaultVelocity = 256 / operatorCount;
        if (defaultVelocity > 128) defaultVelocity = 128;
        for (int i = 0; i < operatorCount; i++) {
            _operators.set(i, new Sequencer(this, _data, 60, defaultVelocity, 1, Double.NaN, null));
        }

        if (chord instanceof Chord) {
            _chord = ((Chord) chord);
        } else {
            _chord = _defaultChord;
            if (chord instanceof String) {
                _chord.setName((String) chord);
            }
        }

        _nextPattern = null;
        _pattern = new ArrayList<Note>();
        _changePatternOnSegment = changePatternOnSegment;

        _updateChordNotes();
        _updateSequencePattern(pattern);
    }

    // configure
    //

    // operations
    //

    /** play drum sequence */
    @Override
    public void play() {
        int i, imax = _operators.size(), opn;
        stop();
        _tracks = _sequenceOn(_data, false, false);
        if (_tracks != null && _tracks.size() == imax) {
            _synthesizer._registerTracks(_tracks);
            for (i = 0, opn = 0; i < imax; i++) {
                if (_tracks.get(opn).getTrackNumber() > _tracks.get(i).getTrackNumber()) opn = i;
                _operators.get(i).play(_tracks.get(i));
            }
            _operators.get(opn).onEnterFrame = this::_onEnterFrame;
            _operators.get(opn).onEnterSegment = this::_onEnterSegment;
        } else {
            throw new Error("unknown error");
        }
    }

    /** stop sequence */
    @Override
    public void stop() {
        if (_tracks != null) {
            for (int i = 0; i < _operators.size(); i++) {
                _operators.get(i).stop();
                _operators.get(i).onEnterFrame = null;
                _operators.get(i).onEnterSegment = null;
            }
            _synthesizer._unregisterTracks(_tracks.get(0), _tracks.size());
            for (SiMMLTrack t : _tracks) t.setDisposable();
            _tracks = null;
            _sequenceOff(false);
        }
        _stopEffect();
    }

    // internals
    //

    /** update chord notes */
    protected void _updateChordNotes() {
        int i, imax = _operators.size(), noteIndex;
        for (i = 0; i < imax; i++) {
            _operators.get(i).setDefaultNote(_chord.getNote((_noteIndexes >> (i << 2)) & 15));
        }
    }

    /** update sequence pattern */
    protected void _updateSequencePattern(double[] lengthPattern) {
        int i, imax = 0;

        _currentPattern = lengthPattern;
        if (_currentPattern != null) {
            imax = _currentPattern.length;
//            _pattern.length = imax;
            for (i = 0; i < imax; i++) {
                if (_pattern.get(i) == null) _pattern.set(i, new Note());
                if (lengthPattern[i] == 0) _pattern.get(i).setRest();
                else _pattern.get(i).setNote(-1, -1, _currentPattern[i], -1, Double.NaN, null);
            }
            imax = _operators.size();
            for (i = 0; i < imax; i++) {
                _operators.get(i).pattern = _pattern;
            }
        } else {
            for (i = 0; i < imax; i++) {
                _operators.get(i).pattern = null;
            }
        }
    }

    /** on enter segment */
    @Override
    protected void _onEnterSegment(Sequencer seq) {
        if (_nextPattern != null) {
            _updateSequencePattern(_nextPattern);
            _nextPattern = null;
        }
        super._onEnterSegment(seq);
    }
}
