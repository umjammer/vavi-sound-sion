//
// MML Sequence class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.si.utils.ByteArray;


/** Sequence of 1 sound channel. MMLData > MMLSequenceGroup > MMLSequence > MMLEvent (">" meanse "has a"). */
public class MMLSequence {

    // variables
    //
    /** First MMLEvent. The ID instanceof always MMLEvent.SEQUENCE_HEAD. */
    public MMLEvent headEvent;
    /** Last MMLEvent. The ID instanceof always MMLEvent.SEQUENCE_TAIL and lastEvent.next instanceof always null. */
    public MMLEvent tailEvent;
    /** Is active ? The sequence instanceof skipped to play when this value instanceof false. */
    public boolean isActive;

    // mml string
    private String _mmlString;
    // mml length in resolution unit
    private int _mmlLength;
    // flag for apearance of repeat all command (segno)
    private boolean _hasRepeatAll;

    // Previous sequence in the chain.
    private MMLSequence _prevSequence;
    // Next sequence in the chain.
    private MMLSequence _nextSequence;
    // Is terminal sequence.
    private boolean _isTerminal;

    /** @private [sion sequencer internal] callback functions for Event.INTERNAL_CALL */
    List<Function<Object, MMLEvent>> _callbackInternalCall;
    /** @private [sion sequencer internal] owner data */
    public MMLData _owner;

    // properties
    //

    /** next sequence. */
    public MMLSequence getNextSequence() {
        return (!_nextSequence._isTerminal) ? _nextSequence : null;
    }

    /** MML String, if its cached when its compiling. */
    public String getMmlString() {
        return _mmlString;
    }

    /** MML length, in resolution unit (1920 = whole-tone in default). */
    public int getMmlLength() {
        if (_mmlLength == -1) _updateMMLLength();
        return _mmlLength;
    }

    /** flag for apearance of repeat all command (segno) */
    public boolean hasRepeatAll() {
        if (_mmlLength == -1) _updateMMLLength();
        return _hasRepeatAll;
    }

    // constructor
    //

    /** Constructor. */
    public MMLSequence(boolean term /* = false */) {
        _owner = null;
        headEvent = null;
        tailEvent = null;
        isActive = true;
        _mmlString = "";
        _mmlLength = -1;
        _hasRepeatAll = false;
        _prevSequence = (term) ? this : null;
        _nextSequence = (term) ? this : null;
        _isTerminal = term;
        _callbackInternalCall = new ArrayList<>();
    }

    /** toString returns the event ids. */
    public String toString() {
        if (_isTerminal) return "terminator";
        MMLEvent e = headEvent.next;
        String str = "";
        for (int i = 0; i < 32; i++) {
            str += e.id + " ";
            e = e.next;
            if (e == null) break;
        }
        return str;
    }

    /**
     * Returns ((an) events) Vector.&lt;MMLEvent&gt;.
     *
     * @param lengthLimit maximum length of returning Vector. When this argument set to 0, the Vector includes all events.
     * @param offset      starting index of returning Vector.
     * @param eventID     event id to get. When this argument set to -1, the Vector includes all kind of events.
     */
    public List<MMLEvent> toVector(int lengthLimit, int offset, int eventID) {
        if (headEvent == null) return null;
        MMLEvent e;
        int i = 0;
        List<MMLEvent> result = new ArrayList<>();
        for (e = headEvent.next; e != null && e.id != MMLEvent.SEQUENCE_TAIL; e = e.next) {
            if (eventID == -1 || eventID == e.id) {
                if (i >= offset) result.add(e);
                if (lengthLimit > 0 && i >= lengthLimit) break;
                i++;
            }
        }
        return result;
    }

    /**
     * Create sequence from Vector.&lt;MMLEvent&gt;.
     *
     * @param events event list of the sequence.
     */
    public MMLSequence fromVector(List<MMLEvent> events) {
        initialize();
        for (MMLEvent e : events) push(e);
        return this;
    }

    // operations
    //

    MMLParser mmlParser = new MMLParser();

    /** initialize. */
    public MMLSequence initialize() {
        if (!isEmpty()) {
            headEvent.jump.next = tailEvent;
            mmlParser._freeAllEvents(this);
            _callbackInternalCall.clear();
        }
        headEvent = mmlParser._allocEvent(MMLEvent.SEQUENCE_HEAD, 0, 0);
        tailEvent = mmlParser._allocEvent(MMLEvent.SEQUENCE_TAIL, 0, 0);
        headEvent.next = tailEvent;
        headEvent.jump = headEvent;
        isActive = true;
        return this;
    }

    /** Free. */
    public void free() {
        if (headEvent != null) {
            // disconnect
            headEvent.jump.next = tailEvent;
            mmlParser._freeAllEvents(this);
            _prevSequence = null;
            _nextSequence = null;
        } else if (_isTerminal) {
            _prevSequence = this;
            _nextSequence = this;
        }
        _mmlString = "";
    }

    /** is empty ? */
    public boolean isEmpty() {
        return (headEvent == null);
    }

    /** Pack to ByteArray. */
    public void pack(ByteArray seq) {
        // not available
    }

    /** Unpack from ByteArray. */
    public void unpack(ByteArray seq) {
        // not available
    }

    /**
     * Append new MMLEvent at tail
     *
     * @param id     MML event id.
     * @param data   MML event data.
     * @param length MML event length.
     * @see org.si.sion.sequencer.base.MMLEvent
     */
    public MMLEvent appendNewEvent(int id, int data, int length /* = 0 */) {
        return push(mmlParser._allocEvent(id, data, length));
    }

    /**
     * Append new Callback function
     *
     * @param func The function to call. (function(int) : MMLEvent)
     * @param data The value to pass to the ((an) callback) argument
     */
    public MMLEvent appendNewCallback(Function<Object, MMLEvent> func, int data) {
        _callbackInternalCall.add(func);
        return push(mmlParser._allocEvent(MMLEvent.INTERNAL_CALL, _callbackInternalCall.size() - 1, data));
    }

    /**
     * Prepend new MMLEvent at head
     *
     * @param id     MML event id.
     * @param data   MML event data.
     * @param length MML event length.
     * @see org.si.sion.sequencer.base.MMLEvent
     */
    public MMLEvent prependNewEvent(int id, int data, int length /* = 0 */) {
        return unshift(mmlParser._allocEvent(id, data, length));
    }

    /**
     * Add MMLEvent at tail.
     *
     * @param e event to be pushed.
     * @return added event, ((an) same) argument.
     */
    public MMLEvent push(MMLEvent e) {
        // connect event at tail
        headEvent.jump.next = e;
        e.next = tailEvent;
        headEvent.jump = e;
        return e;
    }

    /**
     * Remove MMLEvent from tail.
     *
     * @return removed MML event. You should call MMLEvent.free() after using this event.
     */
    public MMLEvent pop() {
        if (headEvent.jump == headEvent) return null;
        for (MMLEvent e = headEvent.next; e != null; e = e.next) {
            if (e.next == headEvent.jump) {
                MMLEvent ret = e.next;
                e.next = tailEvent;
                headEvent.jump = e;
                ret.next = null;
                return ret;
            }
        }
        return null;
    }

    /**
     * Add MMLEvent at head.
     *
     * @param e event to be pushed.
     * @return added event, ((an) same) argument.
     */
    public MMLEvent unshift(MMLEvent e) {
        // connect event at head
        e.next = headEvent.next;
        headEvent.next = e;
        if (headEvent.jump == headEvent) headEvent.jump = e;
        return e;
    }

    /**
     * Remove MMLEvent from head.
     *
     * @return removed MML event. You should call MMLEvent.free() after using this event.
     */
    public MMLEvent shift() {
        if (headEvent.jump == headEvent) return null;
        MMLEvent ret = headEvent.next;
        headEvent.next = ret.next;
        ret.next = null;
        return ret;
    }

    /**
     * connect 2 sequences temporarily, this function doesnt change tail pointer, so you have to call connectBefore(null) after using this connection.
     *
     * @param secondHead head event of second sequence, null to set tail ((default) event).
     * @return this instance
     */
    public MMLSequence connectBefore(MMLEvent secondHead) {
        // simply connect first tail to second head.
        headEvent.jump.next = (secondHead != null) ? secondHead : tailEvent;
        return this;
    }

    /** is system command */
    public boolean isSystemCommand() {
        return (headEvent.next.id == MMLEvent.SYSTEM_EVENT);
    }

    /** get system command */
    public String getSystemCommand() {
        return mmlParser._getSystemEventString(headEvent.next);
    }

    /** @private [sion sequencer internal] cutout MMLSequence */
    public MMLEvent _cutout(MMLEvent head) {
        MMLEvent last = head.jump; // last event of this sequence
        MMLEvent next = last.next; // head of next sequence

        // cut out
        headEvent = head;
        tailEvent = mmlParser._allocEvent(MMLEvent.SEQUENCE_TAIL, 0, 0);
        last.next = tailEvent;  // append tailEvent at last

        return next;
    }

    /** @private [internal] update mml string */
    void _updateMMLString() {
        if (headEvent.next.id == MMLEvent.DEBUG_INFO) {
            _mmlString = mmlParser._getSequenceMML(headEvent.next);
            headEvent.length = 0;
        }
    }

    /** @private [internal] insert before */
    void _insertBefore(MMLSequence next) {
        _prevSequence = next._prevSequence;
        _nextSequence = next;
        _prevSequence._nextSequence = this;
        _nextSequence._prevSequence = this;
    }

    /** @private [internal] insert after */
    void _insertAfter(MMLSequence prev) {
        _prevSequence = prev;
        _nextSequence = prev._nextSequence;
        _prevSequence._nextSequence = this;
        _nextSequence._prevSequence = this;
    }

    /** @private [sion sequencer internal] remove from chain. @return previous sequence. */
    public MMLSequence _removeFromChain() {
        MMLSequence ret = _prevSequence;
        _prevSequence._nextSequence = _nextSequence;
        _nextSequence._prevSequence = _prevSequence;
        _prevSequence = null;
        _nextSequence = null;
        return (ret == this) ? null : ret;
    }

    // calculate mml length
    private void _updateMMLLength() {
        MMLExecutor exec = MMLSequencer._tempExecutor;
        MMLEvent e = headEvent.next;
        int length = 0;
        _hasRepeatAll = false;
        exec.initialize(this);
        while (e != null) {
            if (e.length != 0) {
                // note or rest
                length += e.length;
                e = e.next;
            } else {
                // others
                switch (e.id) {
                    case MMLEvent.REPEAT_BEGIN:
                        e = exec._onRepeatBegin(e);
                        break;
                    case MMLEvent.REPEAT_BREAK:
                        e = exec._onRepeatBreak(e);
                        break;
                    case MMLEvent.REPEAT_END:
                        e = exec._onRepeatEnd(e);
                        break;
                    case MMLEvent.REPEAT_ALL:
                        e = null;
                        _hasRepeatAll = true;
                        break;
                    case MMLEvent.SEQUENCE_TAIL:
                        e = null;
                        break;
                    default:
                        e = e.next;
                        break;
                }
            }
        }

        _mmlLength = length;
    }
}
