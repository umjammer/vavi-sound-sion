//
// MML event class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;


/** MML event. */
public class MMLEvent {

    // constants
    //

    // event id for default mml commands
    public static final int NOP = 0;
    public static final int PROCESS = 1;
    public static final int REST = 2;
    public static final int NOTE = 3;
//    public static final int LENGTH= 4;
//    public static final int TEI= 5;
//    public static final int OCTAVE= 6;
//    public static final int OCTAVE_SHIFT= 7;
    public static final int KEY_ON_DELAY = 8;
    public static final int QUANT_RATIO = 9;
    public static final int QUANT_COUNT = 10;
    public static final int VOLUME = 11;
    public static final int VOLUME_SHIFT = 12;
    public static final int FINE_VOLUME = 13;
    public static final int SLUR = 14;
    public static final int SLUR_WEAK = 15;
    public static final int PITCHBEND = 16;
    public static final int REPEAT_BEGIN = 17;
    public static final int REPEAT_BREAK = 18;
    public static final int REPEAT_END = 19;
    public static final int MOD_TYPE = 20;
    public static final int MOD_PARAM = 21;
    public static final int INPUT_PIPE = 22;
    public static final int OUTPUT_PIPE = 23;
    public static final int REPEAT_ALL = 24;
    public static final int PARAMETER = 25;
    public static final int SEQUENCE_HEAD = 26;
    public static final int SEQUENCE_TAIL = 27;
    public static final int SYSTEM_EVENT = 28;
    public static final int TABLE_EVENT = 29;
    public static final int GLOBAL_WAIT = 30;
    public static final int TEMPO = 31;
    public static final int TIMER = 32;
    public static final int REGISTER = 33;
    public static final int DEBUG_INFO = 34;
    public static final int INTERNAL_CALL = 35;
    public static final int INTERNAL_WAIT = 36;
    public static final int DRIVER_NOTE = 37;

    /** Event id for the first user defined command. */
    public static final int USER_DEFINE = 64;

    /** Maximum value of event id. */
    public static final int COMMAND_MAX = 128;

    // variables
    //

    /** NOP event */
    public static MMLEvent nopEvent = new MMLEvent().initialize(MMLEvent.NOP, 0, 0);

    /** Event ID. */
    public int id = 0;
    /** Event data. */
    public int data = 0;
    /** Processing length. */
    public int length = 0;
    /** Next event pointer in an event chain. */
    public MMLEvent next;
    /** Pointer referred by repeating. */
    public MMLEvent jump;

    // functions
    //

    /** Constructor */
    public MMLEvent() {
    }

    /** Constructor */
    public MMLEvent(int id, int data, int length) {
        if (id > 1) initialize(id, data, length);
    }

    /** Format as "#id; data" */
    public String toString() {
        return "#" + id + "; " + data;
    }

    /**
     * Initializes
     *
     * @param id   Event ID.
     * @param data Event data. Recommend that the value &lt;= 0xffffff.
     */
    public MMLEvent initialize(int id, int data, int length) {
        this.id = id & 0x7f;
        this.data = data;
        this.length = length;
        this.next = null;
        this.jump = null;
        return this;
    }

    /**
     * Get ((an) parameters) array.
     *
     * @param param  Reference to get parameters.
     * @param length Max parameters count to get.
     * @return The last parameter event.
     */
    public MMLEvent getParameters(int[] param, int length) {
        int i;
        MMLEvent e = this;

        i = 0;
        while (i < length) {
            param[i] = e.data;
            i++;
            if (e.next == null || e.next.id != PARAMETER) break;
            e = e.next;
        }
        while (i < length) {
            param[i] = Integer.MIN_VALUE;
            i++;
        }
        return e;
    }

    /** free this event to reuse. */
    public void free() {
        if (next == null) MMLParser._freeEvent(this);
    }

    /** Pack to int. */
    public int pack() {
        return 0;
    }

    /** Unpack from int. */
    public void unpack(int d) {
    }
}
