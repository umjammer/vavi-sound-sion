//
// MIDI sound module operator
//  Copyright (c) 2011 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.midi;

import org.si.sion.sequencer.SiMMLTrack;


/** @private MIDI sound module operator */
class MIDIModuleOperator {

    // variables
    //

    MIDIModuleOperator next, prev;
    SiMMLTrack sionTrack = null;
    int length = 0;
    int programNumber;
    int channel;
    int note;
    boolean isNoteOn;
    int drumExcID;

    // constructor
    //

    public MIDIModuleOperator(SiMMLTrack sionTrack) {
        this.sionTrack = sionTrack;
        next = prev = this;
        programNumber = -1;
        channel = -1;
        note = -1;
        isNoteOn = false;
        drumExcID = -1;
    }

    // list operation
    //

    void clear() {
        prev = next = this;
        length = 0;
    }

    void add(MIDIModuleOperator ope) {
        ope.prev = prev;
        ope.next = this;
        prev.next = ope;
        prev = ope;
        length++;
    }

    MIDIModuleOperator pop() {
        if (prev == this) return null;
        MIDIModuleOperator ret = prev;
        prev = prev.prev;
        prev.next = this;
        ret.prev = ret.next = ret;
        length--;
        return ret;
    }

    void unshift(MIDIModuleOperator ope) {
        ope.prev = this;
        ope.next = next;
        next.prev = ope;
        next = ope;
        length++;
    }

    MIDIModuleOperator shift() {
        if (next == this) return null;
        MIDIModuleOperator ret = next;
        next = next.next;
        next.prev = this;
        ret.prev = ret.next = ret;
        length--;
        return ret;
    }

    void remove(MIDIModuleOperator ope) {
        ope.prev.next = ope.next;
        ope.next.prev = ope.prev;
        ope.prev = ope.next = this;
        length--;
    }
}
