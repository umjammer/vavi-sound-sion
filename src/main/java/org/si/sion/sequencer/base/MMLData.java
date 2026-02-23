//
// MML data class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/** MML data class. MMLData > MMLSequenceGroup > MMLSequence > MMLEvent (">" meanse "has a"). */
public class MMLData {

    // constants
    //

    /** specify tcommand argument by BPM */
    public static final int TCOMMAND_BPM = 0;
    /** specify tcommand argument by OPNA's TIMERB with 48ticks/beat */
    public static final int TCOMMAND_TIMERB = 1;
    /** specify tcommand argument by frame count */
    public static final int TCOMMAND_FRAME = 2;

    // variables
    //

    /** Sequence group */
    public MMLSequenceGroup sequenceGroup;
    /** Global sequence */
    public MMLSequence globalSequence;

    /** default FPS */
    public int defaultFPS;
    /** Title */
    public String title;
    /** Author */
    public String author;
    /** mode of t command */
    public int tcommandMode;
    /** resolution of t command */
    public double tcommandResolution;
    /** default velocity command shift */
    public int defaultVCommandShift;
    /** default velocity mode */
    public int defaultVelocityMode;
    /** default expression mode */
    public int defaultExpressionMode;

    /** default BPM of this data */
    public BeatPerMinutes _initialBPM;
    /** system commands that can not be parsed by system */
    List<Map<String, Object>> _systemCommands;

    // properties
    //

    /** sequence count */
    public int getSequenceCount() {
        return sequenceGroup.getSequenceCount();
    }

    /** Beat per minutes, set 0 when this data depends on driver's BPM. */
    public void setBpm(double t) {
        _initialBPM = (t > 0) ? (new BeatPerMinutes(t, 44100, 512)) : null;
    }

    public double getBpm() {
        return (_initialBPM != null) ? _initialBPM.getBpm() : 0;
    }

    /**
     * system commands that can not be parsed. Examples are for mml string "#ABC5{def}ghi;".<br/>
     * the array elements are Object, and it has following properties.<br/>
     * <ul>
     * <li>command: command name. this always starts with "#". ex) command = "#ABC"</li>
     * <li>number:  number after command. ex) number = 5</li>
     * <li>content: content inside {...}. ex) content = "def"</li>
     * <li>postfix: number after command. ex) postfix = "ghi"</li>
     * </ul>
     */
    public List<Map<String, Object>> getSystemCommands() {
        return _systemCommands;
    }

    /** Get song length by tick count (1920 for wholetone). */
    public int getTickCount() {
        return sequenceGroup.getTickCount();
    }

    /** does this song have all repeat comand ? */
    public boolean getHasRepeatAll() {
        return sequenceGroup.hasRepeatAll();
    }

    // constructor
    //
    public MMLData() {
        sequenceGroup = new MMLSequenceGroup(this);
        globalSequence = new MMLSequence(false);

        _initialBPM = null;
        tcommandMode = TCOMMAND_BPM;
        tcommandResolution = 1;
        defaultVCommandShift = 4;
        defaultVelocityMode = 0;
        defaultExpressionMode = 0;
        defaultFPS = 60;
        title = "";
        author = "";
        _systemCommands = new ArrayList<>();
    }

    // operation
    //

    /** Clear all parameters and free all sequence groups. */
    public void clear() {
        int i, imax;

        sequenceGroup.free();
        globalSequence.free();

        _initialBPM = null;
        tcommandMode = TCOMMAND_BPM;
        tcommandResolution = 1;
        defaultVelocityMode = 0;
        defaultExpressionMode = 0;
        defaultFPS = 60;
        title = "";
        author = "";
        _systemCommands = new ArrayList<>();

        globalSequence.initialize();
    }

    /**
     * Append new sequence.
     *
     * @param sequence event list for new sequence. when null, create empty sequence.
     * @return created sequence
     */
    public MMLSequence appendNewSequence(List<MMLEvent> sequence /* = null */) {
        MMLSequence seq = sequenceGroup.appendNewSequence();
        if (sequence != null) seq.fromVector(sequence);
        return seq;
    }

    /**
     * Get sequence.
     *
     * @param index The index of sequence
     */
    public MMLSequence getSequence(int index) {
        return sequenceGroup.getSequence(index);
    }

    /** @private calculate bpm from t command parameter */
    double _calcBPMfromTcommand(int param) {
        switch (tcommandMode) {
            case TCOMMAND_BPM:
                return param * tcommandResolution;
            case TCOMMAND_FRAME:
                return (param != 0) ? (tcommandResolution / param) : 120;
            case TCOMMAND_TIMERB:
                return (param >= 0 && param < 256) ? (tcommandResolution / (256 - param)) : 120;
        }
        return 0;
    }
}
