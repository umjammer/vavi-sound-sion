//
// Note object used in PatternSequencer
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.patterns;


/** Note object used in PatternSequencer. */
public class Note {

    // variables
    //

    /** Note number[-1-127], -1 (or all negatives) sets playing with sequencers default note. */
    public int note = 0;
    /** Velocity[-1-128], -1 (or all negatives) sets playing with sequencers default velocity, 0 sets no note (rest). */
    public int velocity = 0;
    /** Length in 16th beat [16 for whole tone], Double.NaN sets playing with sequencers default length. */
    public double length = 0;
    /**
     * Voice index referring from PatternSequencer.voiceList, -1 (or all negatives) sets no voice changing.
     *
     * @see si.org.sound.PatternSequencer#voiceList
     */
    public int voiceIndex = -1;
    /** Gate time of this note, Double.NaN sets playing with swquencers default gateTime. */
    public double gateTime = Double.NaN;
    /** Any information */
    public Object data = null;

    // constructor
    //

    public Note() {
        this(-1, 0, Double.NaN, -1, Double.NaN, null);
    }

    /**
     * constructor
     *
     * @param note       Note number[-1-127], -1 sets playing with sequencer's default note.
     * @param velocity   Velocity[-1-128], -1 sets playing with sequencer's default velocity, 0 sets no note (rest).
     * @param length     Length in 16th beat [16 for whole tone], Number.NaN sets playing with sequencers default length.
     * @param voiceIndex Voice index referring from PatternSequencer.voiceList, -1 (or all negatives) sets no voice changing.
     * @param gateTime   Gate time of this note[0-1], Number.NaN sets playing with sequencers default gateTime.
     * @param data       Any information you want.
     * @see si.org.sound.PatternSequencer#voiceList
     */
    public Note(int note, int velocity, double length, int voiceIndex, double gateTime, Object data /* null */) {
        this.note = note;
        this.velocity = velocity;
        this.length = length;
        this.voiceIndex = voiceIndex;
        this.gateTime = gateTime;
        this.data = data;
    }

    // operations
    //

    /**
     * Set note.
     *
     * @param note       Note number[-1-127], -1 sets playing with sequencer's default note.
     * @param velocity   Velocity[-1-128], -1 sets playing with sequencer's default velocity, 0 sets no note (rest).
     * @param length     Length in 16th beat [16 for whole tone], Number.NaN sets playing with sequencers default length.
     * @param voiceIndex Voice index refering from PatternSequencer.voiceList, -1 (or all negatives) sets no voice changing. @see si.org.sound.PatternSequencer.voiceList.
     * @param gateTime   Gate time of this note[0-1], Number.NaN sets playing with swquencers default gateTime.
     * @param data       Any information you want.
     * @return this instance.
     */
    public Note setNote(int note, int velocity, double length, int voiceIndex /* = -1 */, double gateTime /* Double.NaN */, Object data /* = null */) {
        this.note = note;
        this.velocity = velocity;
        this.length = length;
        this.voiceIndex = voiceIndex;
        this.gateTime = gateTime;
        this.data = data;
        return this;
    }

    /** ((rest) Set). */
    public Note setRest() {
        velocity = 0;
        return this;
    }

    /**
     * Copy from another note.
     *
     * @param src source note instance.
     * @return this instance
     */
    public Note copyFrom(Note src) {
        String key;
        if (src == null) return setRest();
        note = src.note;
        velocity = src.velocity;
        length = src.length;
        voiceIndex = src.voiceIndex;
        gateTime = src.gateTime;
        if (src.data != null) {
            data = src.data; // Shallow copy
        } else {
            data = null;
        }
        return this;
    }

    /**
     * return new instance copied parameters from this note.
     *
     * @return new clone instance
     */
    @Override
    public Note clone() {
        return new Note().copyFrom(this);
    }
}
