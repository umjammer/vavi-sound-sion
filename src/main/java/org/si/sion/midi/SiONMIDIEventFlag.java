//
// SiON MIDI internal namespace
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.midi;


public class SiONMIDIEventFlag {

    /** dispatch flag for SiONMIDIEvent.NOTE_ON */
    public static final int NOTE_ON = 1;
    /** dispatch flag for SiONMIDIEvent.NOTE_OFF */
    public static final int NOTE_OFF = 2;
    /** dispatch flag for SiONMIDIEvent.CONTROL_CHANGE */
    public static final int CONTROL_CHANGE = 4;
    /** dispatch flag for SiONMIDIEvent.PROGRAM_CHANGE */
    public static final int PROGRAM_CHANGE = 8;
    /** dispatch flag for SiONMIDIEvent.PITCH_BEND */
    public static final int PITCH_BEND = 16;
    /** Flag for all */
    public static final int ALL = 31;
}
