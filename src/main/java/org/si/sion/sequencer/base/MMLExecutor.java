//
// MML Sequence executor class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

import org.si.utils.SLLint;


/** MMLExecutor has MMLSequence and executing pointer. One track has one executor, and sequencer also has one for global sequence. */
public class MMLExecutor {

    // variables
    //

    /** Current MMLEvent to process */
    public MMLEvent pointer;

    // MMLSequence to execute.
    private MMLSequence _sequence;
    // Repeating count by segno
    private int _endRepeatCounter;
    // Repeating point
    private MMLEvent _repeatPoint;
    // event to process
    private MMLEvent _processEvent;
    // pitchbend event
    private MMLEvent _bendFrom;
    // pitchbend event
    private MMLEvent _bendEvent;
    // note event
    private MMLEvent _noteEvent;

    /** current position in tick count. */
    int _currentTickCount;
    /** the stac of counters to operate repeatings. refer from MMLSequencer. */
    SLLint _repeatCounter;
    /** the leftover of processing sample count. refer from MMLSequencer. */
    int _residueSampleCount;
    /** the decimal fraction part of processing sample count. */
    int _decimalFractionSampleCount;

    // properties
    //

    /** Repeating count by segno */
    public int getEndRepeatCount() {
        return _endRepeatCounter;
    }

    /** Executing MMLSequence */
    public MMLSequence getSequence() {
        return _sequence;
    }

    /** Current event */
    public MMLEvent getCurrentEvent() {
        return (pointer == _processEvent) ? pointer.jump : pointer;
    }

    /** Note that wait for note on execution ? -1 for not waiting */
    public int getNoteWaitingFor() {
        return (pointer == _noteEvent) ? _noteEvent.data : -1;
    }


    // constructor
    //

    /** Constructor. */
    public MMLExecutor() {
        _sequence = null;
        pointer = null;
        _endRepeatCounter = 0;
        _repeatPoint = null;
        _processEvent = MMLParser._allocEvent(MMLEvent.PROCESS, 0, 0);
        _noteEvent = MMLParser._allocEvent(MMLEvent.DRIVER_NOTE, 0, 0);
        _bendFrom = MMLParser._allocEvent(MMLEvent.NOTE, 0, 0);
        _bendEvent = MMLParser._allocEvent(MMLEvent.PITCHBEND, 0, 0);
        _bendFrom.next = _bendEvent;
        _bendEvent.next = _noteEvent;
        _repeatCounter = null;
        _currentTickCount = 0;
        _residueSampleCount = 0;
        _decimalFractionSampleCount = 0;
    }

    // operations
    //

    /**
     * Initialize.
     *
     * @param seq Sequence to execute. Sets the pointer at the head of this sequence, when this argument instanceof not null.
     */
    public void initialize(MMLSequence seq) {
        clear();
        if (seq != null) {
            _sequence = seq;
            pointer = seq.headEvent.next;
        }
    }

    /** Clear contents. */
    public void clear() {
        pointer = null;
        _sequence = null;
        _endRepeatCounter = 0;
        _repeatPoint = null;
        SLLint.freeList(_repeatCounter);
        _repeatCounter = null;
        _currentTickCount = 0;
        _residueSampleCount = 0;
        _decimalFractionSampleCount = 0;
    }

    /** Reset pointer to sequence head */
    public void resetPointer() {
        if (_sequence != null) {
            pointer = _sequence.headEvent.next;
            _endRepeatCounter = 0;
            _repeatPoint = null;
            SLLint.freeList(_repeatCounter);
            _repeatCounter = null;
            _currentTickCount = 0;
            _residueSampleCount = 0;
            _decimalFractionSampleCount = 0;
        }
    }

    /** stop execute sequence */
    public void stop() {
        if (pointer != null) {
            if (pointer == _processEvent) _processEvent.jump = new MMLEvent().initialize(MMLEvent.NOP, 0, 0);
            else pointer = null;
        }
    }

    /**
     * execute single note.
     *
     * @param note       Note number.
     * @param tickLength length in tick count. The argument of 0 sets no key off.
     */
    public void singleNote(int note, int tickLength) {
        _noteEvent.next = null;
        _noteEvent.data = note;
        _noteEvent.length = tickLength;
        pointer = _noteEvent;

        _sequence = null;
        _endRepeatCounter = 0;
        _repeatPoint = null;
        SLLint.freeList(_repeatCounter);
        _repeatCounter = null;
        _currentTickCount = 0;
    }

    /**
     * pitch bending, this function only instanceof avilable after calling singleNote().
     *
     * @param note       Note number bending to.
     * @param tickLength length of bending.
     * @return success    or failure
     */
    public boolean bendingFrom(int note, int tickLength) {
        if (pointer != _noteEvent || tickLength == 0) return false;
        if (_noteEvent.length != 0) {
            if (tickLength < _noteEvent.length) tickLength = _noteEvent.length - 1;
            _noteEvent.length -= tickLength;
        }
        _bendFrom.length = 0;
        _bendFrom.data = note;
        _bendEvent.length = tickLength;
        pointer = _bendFrom;
        return true;
    }


    /**
     * @param e Current event
     * Publish processing event. You should return this function's return in the event handler of NOTE and REST.
     */
    public MMLEvent _publishProessingEvent(MMLEvent e) {
        if (e.length > 0) {
            //_processEvent.data   = 0;
            //_processEvent.next   = null;
            _currentTickCount += e.length;
            _processEvent.length = e.length;
            _processEvent.jump = e;
            return _processEvent;
        }
        return e.next;
    }

    // callback
    //

    /** callback onTempoChanged. */
    public void _onTempoChanged(int changingRatio) {
        if (_residueSampleCount < 0) changingRatio = 1 / changingRatio;
        _residueSampleCount *= changingRatio;
        _decimalFractionSampleCount *= changingRatio;
    }

    /** callback onRepeatAll. */
    MMLEvent _onRepeatAll(MMLEvent e) {
        _repeatPoint = e.next;
        return e.next;
    }

    /** callback onRepeatBegin. */
    MMLEvent _onRepeatBegin(MMLEvent e) {
        SLLint counter = SLLint.alloc(e.data);
        counter.next = _repeatCounter;
        _repeatCounter = counter;
        return e.next;
    }

    /** callback onRepeatBreak. */
    MMLEvent _onRepeatBreak(MMLEvent e) {
        if (_repeatCounter.i == 1) {
            SLLint counter = _repeatCounter.next;
            SLLint.freeList(_repeatCounter);
            _repeatCounter = counter;
            // Jump to repeatStart.repeatEnd.next
            return e.jump.jump.next;
        }
        return e.next;
    }

    /** callback onRepeatEnd. */
    MMLEvent _onRepeatEnd(MMLEvent e) {
        if (--_repeatCounter.i == 0) {
            SLLint counter = _repeatCounter.next;
            SLLint.freeList(_repeatCounter);
            _repeatCounter = counter;
            return e.next;
        }
        // Jump to repeatStart.next
        return e.jump.next;
    }

    /** callback onSequenceTail. */
    MMLEvent _onSequenceTail(MMLEvent e) {
        _endRepeatCounter++;
        return _repeatPoint;
    }
}
