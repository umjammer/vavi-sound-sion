//
// MML Sequence group class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

import java.util.ArrayList;
import java.util.List;


/** Group of MMLSequences. MMLData > MMLSequenceGroup > MMLSequence > MMLEvent (">" meanse "has a"). */
public class MMLSequenceGroup {

    // variables
    //

    // terminator
    private MMLSequence _term;

    // owner data
    private MMLData _owner;

    // properties
    //

    /** Get sequence count. */
    public int getSequenceCount() {
        return _sequences.size();
    }

    /** head sequence pointer. */
    public MMLSequence getHeadSequence() {
        return _term.getNextSequence();
    }

    /** Get song length by tick count (1920 for wholetone). */
    public int getTickCount() {
        int ml, tc = 0;
        for (MMLSequence seq : _sequences) {
            ml = seq.getMmlLength();
            if (ml > tc) tc = ml;
        }
        return tc;
    }

    /** does this song have all repeat comand ? */
    public boolean hasRepeatAll() {
        for (MMLSequence seq : _sequences) {
            if (seq.hasRepeatAll()) return true;
        }
        return false;
    }

    // constructor
    //
    public MMLSequenceGroup(MMLData owner) {
        _owner = owner;
        _sequences = new ArrayList<MMLSequence>();
        _term = new MMLSequence(true);
    }

    // operation
    //

    /**
     * Create new sequence group. Why its not create() ???
     *
     * @param headEvent MMLEvnet returned from MMLParser.parse().
     */
    public void alloc(MMLEvent headEvent) {
        // divied into sequences
        MMLSequence seq;
        while (headEvent != null && headEvent.jump != null) {
            if (headEvent.id != MMLEvent.SEQUENCE_HEAD) {
                throw new Error("MMLSequence: Unknown error on dividing sequences. " + headEvent);
            }
            seq = appendNewSequence();          // push new sequence
            headEvent = seq._cutout(headEvent); // cutout sequence
            seq._updateMMLString();             // update mml string
            seq.isActive = true;                // activate
        }
    }

    /** Free all sequences */
    public void free() {
        for (MMLSequence seq : _sequences) {
            seq.free();
            _freeList.add(seq);
        }
        _sequences.clear();
        _term.free();
    }

    /**
     * get sequence
     *
     * @param index The index of sequence.
     */
    public MMLSequence getSequence(int index) {
        if (index >= _sequences.size()) return null;
        return _sequences.get(index);
    }

    // factory
    //

    // allocated sequences
    private List<MMLSequence> _sequences;
    // free list
    private List<MMLSequence> _freeList = new ArrayList<>();

    /** append new sequence */
    public MMLSequence appendNewSequence() {
        MMLSequence seq = _newSequence();
        seq._insertBefore(_term);
        seq.isActive = false;   // inactivate
        return seq;
    }

    /** @private [internal] Allocate new sequence and push sequence chain. */
    MMLSequence _newSequence() {
        MMLSequence seq = (!_freeList.isEmpty()) ? _freeList.remove(_freeList.size() - 1) : new MMLSequence(false);
        seq._owner = _owner;
        _sequences.add(seq);
        return seq;
    }
}
