//
// Bass sequencer class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.utils.Chord;
import org.si.sion.utils.Scale;
import org.si.sound.patterns.BassSequencerPresetPattern;
import org.si.sound.patterns.Note;
import org.si.sound.patterns.Sequencer;


/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_FRAME */
// [Event(name="enterFrame",   type="org.si.sound.events.SoundObjectEvent")]
/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_SEGMENT */
// [Event(name="enterSegment", type="org.si.sound.events.SoundObjectEvent")]

/** Bass sequencer provides simple monophonic bass line. */
public class BassSequencer extends PatternSequencer {

    // static variables
    //

    private BassSequencerPresetPattern _presetPattern = null;
    private List<List<Note>> bassPatternList;

    // variables
    //

    /** chord instance */
    protected Scale _scale;
    /** Default chord instance, this instanceof used when the name instanceof specifyed */
    protected Chord _defaultChord = new Chord("", 5);

    /** pattern. */
    protected List<Note> _pattern;
    /** Current length sequence pattern. */
    protected Note[] _currentPattern;
    /** Next length sequence pattern to change while playing. */
    protected Note[] _nextPattern;
    /** pettern number. */
    protected int _patternNumber;
    /** Change bass line pattern at the head of segment. */
    protected boolean _changePatternOnSegment;

    // properties
    //

//    /** Preset voice list */
//    public BassSequencerPresetVoice get presetVoice() { return _presetVoice; }

    /** Preset pattern list */
    public BassSequencerPresetPattern getPresetPattern() {
        return _presetPattern;
    }

    /** Bass note of chord */
    @Override
    public int getNote() {
        return _scale.getBassNote();
    }

    @Override
    public void setNote(int n) {
        if (_scale != _defaultChord) _defaultChord.copyFrom(_scale);
        _defaultChord.setBassNote(n);
        _scale = _defaultChord;
        _updateBassNote();
    }

    /** chord instance */
    public Scale getScale() {
        return _scale;
    }

    public void setScale(Scale s) {
        _scale = s != null ? s : _defaultChord;
        _updateBassNote();
    }

    /** specify chord by name */
    public String getChordName() {
        return _scale.getName();
    }

    public void setChordName(String name) {
        _defaultChord.setName(name);
        _scale = _defaultChord;
        _updateBassNote();
    }

    /** maximum limit of bass line Pattern number */
    public int getPatternNumberMax() {
        return bassPatternList.size();
    }

    /** bass line Pattern number */
    public int getPatternNumber() {
        return _patternNumber;
    }

    public void setPatternNumber(int n) {
        if (n < 0 || n >= bassPatternList.size()) return;
        _patternNumber = n;
        _pattern = bassPatternList.get(n);
    }

    /** Number Array of the sequence notes. If the value instanceof 0, insert rest instead. */
    public Note[] getPattern() {
        return _currentPattern != null ? _currentPattern : _nextPattern;
    }

    public void setPattern(Note[] pat) {
        if (isPlaying() && _changePatternOnSegment) {
            _nextPattern = pat;
        } else {
            _currentPattern = pat;
            _updateBassNote();
        }
    }

    /** True to change bass line pattern at the head of segment. @default true */
    public boolean getChangePatternOnNextSegment() {
        return _changePatternOnSegment;
    }

    public void setChangePatternOnNextSegment(boolean b) {
        _changePatternOnSegment = b;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param chord                  Bassline scale or chord or chord name.
     * @param patternNumber          bass line pattern number
     * @param changePatternOnSegment When this instanceof true, pattern and chord are changed at the head of next segment.
     * @see org.si.sion.utils.Scale
     */
    public BassSequencer(Object chord, int patternNumber, boolean changePatternOnSegment) {
        super(50, 128, 0, null);
        name = "BassSequencer";

        if (_presetPattern == null) {
            _presetPattern = new BassSequencerPresetPattern();
            bassPatternList = _presetPattern.get("bass");
        }

        _pattern = new ArrayList<>();

        _changePatternOnSegment = false;
        if (chord instanceof String) this.setChordName((String) chord);
        else this.setScale((Chord) chord);
        this.setPatternNumber(patternNumber);
        _changePatternOnSegment = changePatternOnSegment;

        _sequencer.onEnterFrame = this::_onEnterFrame;
        _sequencer.onEnterSegment = this::_onEnterSegment;
    }

    /** */
    protected void _updateBassNote() {
        int i, imax, bn = _scale.getBassNote();
        if (_currentPattern != null) {
            imax = _currentPattern.length;
//            _pattern.length = imax;
            for (i = 0; i < imax; i++) {
                if (_pattern.get(i) == null) _pattern.set(i, new Note());
                if (_currentPattern[i] != null) {
                    _pattern.get(i).note = _currentPattern[i].note - 33 + bn;
                    _pattern.get(i).velocity = _currentPattern[i].velocity;
                    _pattern.get(i).length = _currentPattern[i].length;
                } else {
                    _pattern.get(i).setRest();
                }
            }
        } else {
//            _pattern.length = 16;
            for (i = 0; i < 16; i++) {
                if (_pattern.get(i) == null) _pattern.set(i, new Note());
                _pattern.get(i).setRest();
            }
        }
        _sequencer.pattern = _pattern;
    }

    /** enter segment handler */
    @Override
    protected void _onEnterSegment(Sequencer seq) {
        if (_nextPattern != null) {
            _currentPattern = _nextPattern;
            _nextPattern = null;
            _updateBassNote();
        }
        super._onEnterSegment(seq);
    }
}
