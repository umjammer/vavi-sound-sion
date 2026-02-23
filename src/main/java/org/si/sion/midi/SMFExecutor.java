//
// Standard MIDI File player class
//  Copyright (c) 2011 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.midi;

import org.si.sion.SiONDriver;


/** Standard MIDI File executor */
public class SMFExecutor {

    // variables
    //

    private int _pointer = 0;
    private int _residueTicks = 0;
    private SMFTrack _track = null;
    private MIDIModule _module = null;

    // properties
    //

    // constructor
    //

    public SMFExecutor() {
    }

    // operations
    //

    /** */
    void _initialize(SMFTrack track, MIDIModule module) {
        _track = track;
        _module = module;
        _pointer = 0;
        _residueTicks = (!_track.sequence.isEmpty()) ? _track.sequence.get(0).deltaTime : -1;
    }

    /** */
    int _execute(int ticks) {
        if (_residueTicks == -1) return 65536;

        SMFEvent event = _track.sequence.get(_pointer);
        int channel, v;

        while (ticks >= _residueTicks) {
            ticks -= _residueTicks;
            channel = event.type & 15;

            if ((event.type & 0xff00) != 0) {
                // META event
                switch (event.type) {
                    case SMFEvent.META_TEMPO:
                        SiONDriver.mutex().setBpm(event.value);
                        break;
                    case SMFEvent.META_PORT:
                        _module.setPortNumber(event.value);
                        break;
                    case SMFEvent.META_TRACK_END:
                        _residueTicks = -1;
                        return 65536;
                }
            } else {
                // MIDI event
                switch (event.type & 0xf0) {
                    case SMFEvent.PROGRAM_CHANGE:
                        _module.programChange(channel, event.value);
                        break;
                    case SMFEvent.CHANNEL_PRESSURE:
                        _module.channelAfterTouch(channel, event.value);
                        break;
                    case SMFEvent.NOTE_OFF:
                        _module.noteOff(channel, event.getNote(), event.getVelocity());
                        break;
                    case SMFEvent.NOTE_ON:
                        v = event.getVelocity();
                        if (v > 0) _module.noteOn(channel, event.getNote(), v);
                        else _module.noteOff(channel, event.getNote(), v);
                        break;
                    //case SMFEvent.KEY_PRESSURE:
                    case SMFEvent.CONTROL_CHANGE:
                        _module.controlChange(channel, event.value >> 16, event.value & 0x7f);
                        break;
                    case SMFEvent.PITCH_BEND:
                        _module.pitchBend(channel, event.value);
                        break;
                    case SMFEvent.SYSTEM_EXCLUSIVE:
                        _module.systemExclusive(channel, event.byteArray);
                        break;
                }
            }

            // increment pointer
            if (++_pointer == _track.sequence.size()) {
                _residueTicks = -1;
                return 65536;
            }
            event = _track.sequence.get(_pointer);
            _residueTicks = event.deltaTime;
        }

        _residueTicks -= ticks;
        return _residueTicks;
    }
}
