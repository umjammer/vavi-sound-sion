//
// MDX event class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.mdx;


/** MDX event */
public class MDXEvent {

    // constant
    //

    public static final int REST = 0x00;
    public static final int NOTE = 0x80;
    public static final int TIMERB = 0xff;
    public static final int REGISTER = 0xfe;
    public static final int VOICE = 0xfd;
    public static final int PAN = 0xfc;
    public static final int VOLUME = 0xfb;
    public static final int VOLUME_DEC = 0xfa;
    public static final int VOLUME_INC = 0xf9;
    public static final int GATE = 0xf8;
    public static final int SLUR = 0xf7;
    public static final int REPEAT_BEGIN = 0xf6;
    public static final int REPEAT_END = 0xf5;
    public static final int REPEAT_BREAK = 0xf4;
    public static final int DETUNE = 0xf3;
    public static final int PORTAMENT = 0xf2;
    public static final int DATA_END = 0xf1;
    public static final int KEY_ON_DELAY = 0xf0;
    public static final int SYNC_SEND = 0xef;
    public static final int SYNC_WAIT = 0xee;
    public static final int FREQUENCY = 0xed;
    public static final int PITCH_LFO = 0xec;
    public static final int VOLUME_LFO = 0xeb;
    public static final int OPM_LFO = 0xea;
    public static final int LFO_DELAY = 0xe9;
    public static final int SET_PCM8 = 0xe8;
    public static final int FADEOUT = 0xe7;

    private static final String[] _noteText = {"c ", "c+", "d ", "d+", "e ", "f ", "f+", "g ", "g+", "a ", "a+", "b "};

    // variables
    //
    public int type = 0;
    public int data = 0;
    public int data2 = 0;
    public int deltaClock = 0;

    // properties
    //

    /** toString */
    public String toString() {
        int i;
        switch (type) {
            case REST:
                return "r ;" + deltaClock;
            case NOTE:
                i = (data + 15) % 12;
                return "o" + (((data + 15) / 12) >> 0) + _noteText[i] + ";" + deltaClock;
            case GATE:
                return "q" + data;
            case DETUNE:
                return "k" + (data >> 8);
            case REPEAT_BEGIN:
                return "[" + data;
            case REPEAT_BREAK:
                return "|";
            case REPEAT_END:
                return "]";
            case PORTAMENT:
                return "po";
            case SLUR:
                return "&";
            case VOICE:
                return "@" + data;
            case PAN:
                return "p" + data;
            case VOLUME:
                return (data < 16) ? "v" + data : "@v" + (data & 127);
            case LFO_DELAY:
                return "LFO_delay" + data;
            case PITCH_LFO:
                return "LFO" + (data & 255) + " mp" + (data >> 8) + "," + (data2);
            case VOLUME_LFO:
                return "LFO" + (data & 255) + " ma" + (data >> 8) + "," + (data2);
            case FREQUENCY:
                return "FREQ" + data;
            case TIMERB:
                return "TIMER_B " + data;
            case SET_PCM8:
                return "PCM8";
            default:
                return "#" + type + "; " + String.valueOf(data);
        }
    }

    // constructor
    //
    public MDXEvent(int type, int data, int data2, int deltaClock) {
        this.type = type;
        this.data = data;
        this.data2 = data2;
        this.deltaClock = deltaClock;
    }
}
