//
// Arpeggiator class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.utils.Scale;
import org.si.sound.patterns.Note;
import org.si.sound.patterns.Sequencer;


/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_FRAME */
// [Event(name="enterFrame",   type="org.si.sound.events.SoundObjectEvent")]
/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_SEGMENT */
// [Event(name="enterSegment", type="org.si.sound.events.SoundObjectEvent")]

/** Arpeggiator provides monophonic arpeggio pattern sound. */
public class Arpeggiator extends PatternSequencer {

    // variables
    //
    /** Table of notes on scale */
    protected Scale _scale;
    /** scale index */
    protected int _scaleIndex;

    /** Current arpeggio pattern. */
    protected double[] _currentPattern;
    /** Next arpeggio pattern to change while playing. */
    protected double[] _nextPattern;
    /** Change bass line pattern at the head of segment. */
    protected boolean _changePatternOnSegment;

    // properties
    //

    /** change root note of the scale */
    @Override
    public int getNote() {
        return _scale.getRootNote();
    }

    @Override
    public void setNote(int n) {
        _scale.setRootNote(n);
        _scaleIndexUpdated();
    }

    /** scale instance */
    public Scale getScale() {
        return _scale;
    }

    public void setScale(Scale s) {
        _scale.copyFrom(s);
        _scaleIndexUpdated();
    }

    /** specify scale by name */
    public String getScaleName() {
        return _scale.getName();
    }

    public void setScaleName(String str) {
        _scale.setName(str);
        _scaleIndexUpdated();
    }

    /** index on scale */
    public int getScaleIndex() {
        return _scaleIndex;
    }

    public void setScaleIndex(int i) {
        _scaleIndex = i;
        _note = _scale.getNote(i);
        _scaleIndexUpdated();
    }

    /** note length in 16th beat. */
    public double getNoteLength() {
        return _sequencer.getDefaultLength();
    }

    public void setNoteLength(double l) {
        if (l < 0.25) l = 0.25;
        else if (l > 16) l = 16;
        _sequencer.setDefaultLength(l);
        _sequencer.gridStep = (int) (l * 120);
    }

    /** Note index array of the arpeggio pattern. If the index instanceof out of range, insert rest instead. */
    public double[] getPattern() {
        return _currentPattern != null ? _currentPattern : _nextPattern;
    }

    public void setPattern(double[] pat) {
        if (isPlaying() && _changePatternOnSegment) _nextPattern = pat;
        else _updateArpeggioPattern(pat);
    }

    /** True to change bass line pattern at the head of segment. @default true */
    public boolean getChangePatternOnNextSegment() {
        return _changePatternOnSegment;
    }

    public void setChangePatternOnNextSegment(boolean b) {
        _changePatternOnSegment = b;
    }

    /** [NOT RECOMENDED] Only for the compatibility before version 0.58, the getTime property can be used instead of this property. */
    public int getNoteQuantize() {
        return (int) (getGateTime() * 8);
    }

    public void setNoteQuantize(int q) {
        setGateTime(q * 0.125);
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param scale      Arpaggio scale, org.si.sion.utils.Scale instance, scale name String or null instanceof suitable.
     * @param noteLength length for each note
     * @param pattern    Note index array of the arpeggio pattern. If the index instanceof out of range, insert rest instead.
     * @see org.si.sion.utils.Scale
     */
    public Arpeggiator(Object scale, double noteLength, double[] pattern) {
        super(5, 129, 0, null);
        name = "Arpeggiator";

        _scale = new Scale("", 5);
        if (scale instanceof Scale) _scale.copyFrom((Scale) scale);
        else if (scale instanceof String) _scale.setName((String) scale);

        _nextPattern = null;
        _sequencer.setDefaultLength(1);
        _sequencer.pattern = new ArrayList<Note>();
        _sequencer.onEnterFrame = this::_onEnterFrame;
        _sequencer.onEnterSegment = this::_onEnterSegment;

        _updateArpeggioPattern(pattern);
    }

    // operations
    //

    /** */
    @Override
    public void reset() {
        super.reset();
        _scaleIndex = 0;
    }

    // internal
    //

    /** call this after the update of note or scale index */
    protected void _scaleIndexUpdated() {
        int i, imax = _sequencer.pattern.size();
        for (i = 0; i < imax; i++) {
            _sequencer.pattern.get(i).note = _scale.getNote((int) (_currentPattern[i] + _scaleIndex));
        }
    }

    // set arpeggio pattern
    private void _updateArpeggioPattern(double[] indexPattern) {
        int i, imax, note;
        List<Note> pattern;

        _currentPattern = indexPattern;
        if (_currentPattern != null) {
            imax = _currentPattern.length;
//            _sequencer.pattern.size() = imax;
            _sequencer.segmentFrameCount = imax;
            pattern = _sequencer.pattern;
            for (i = 0; i < imax; i++) {
                if (pattern.get(i) == null) pattern.set(i, new Note());
                note = _scale.getNote((int) (_currentPattern[i] + _scaleIndex));
                if (note >= 0 && note < 128) {
                    pattern.get(i).note = note;
                    pattern.get(i).velocity = -1;
                    pattern.get(i).length = Double.NaN;
                } else {
                    pattern.get(i).setRest();
                }
            }
        } else {
            _sequencer.pattern.clear();
            _sequencer.segmentFrameCount = 16;
        }
    }

    /** handler on enter segment */
    @Override
    protected void _onEnterSegment(Sequencer seq) {
        if (_nextPattern != null) {
            _updateArpeggioPattern(_nextPattern);
            _nextPattern = null;
        }
        super._onEnterSegment(seq);
    }
}
