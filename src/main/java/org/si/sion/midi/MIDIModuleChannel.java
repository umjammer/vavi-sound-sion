//
// MIDI sound module operator
//  Copyright (c) 2011 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.midi;


/** MIDI sound module channel */
public class MIDIModuleChannel {

    // variables
    //

    /** active operator count of this channel */
    public int activeOperatorCount;
    /** maximum operator limit of this channel */
    public int maxOperatorCount;

    /** Drum mode. 0=normal part, 1~=drum part */
    public int drumMode;
    /** Mute */
    public boolean mute;
    /** Program number (0-127) */
    public int programNumber;
    /** Pannig (-64~63) */
    public int pan;
    /** Modulation (0-127) */
    public int modulation;
    /** Pitch bend value (-8192~8191) */
    public int pitchBend;
    /** channel after touch (0-127) */
    public int channelAfterTouch;
    /** Sustain pedal */
    public boolean sustainPedal;
    /** Portamento */
    public boolean portamento;
    /** Portamento time */
    public int portamentoTime;
    /** Master fine tune (-64~63) */
    public int masterFineTune;
    /** Master coarse tune (-64~63) */
    public int masterCoarseTune;
    /** Pitch bend sensitivity */
    public int pitchBendSensitivity;
    /** Modulation cycle time */
    public int modulationCycleTime;

    /** event trigger ID */
    public int eventTriggerID;
    /** dispatching event trigger type of NOTE_ON */
    public int eventTriggerTypeOn;
    /** dispatching event trigger type of NOTE_OFF */
    public int eventTriggerTypeOff;
    /** dispatching event flag of SiONMIDIEvent, conbination of SiONMIDIEventFlag */
    public int sionMIDIEventType;

    /** bank number */
    public int bankNumber;

    /** */
    int[] _sionVolumes = new int[8];
    /** */
    int[] _effectSendLevels = new int[8];

    private int _expression;
    private int _masterVolume;

    // properties
    //

    /** master volume (0-127) */
    public int getMasterVolume() {
        return _masterVolume;
    }

    public void setMasterVolume(int v) {
        _masterVolume = v;
        _updateVolumes();
    }

    /** expression (0-127) */
    public int getExpression() {
        return _expression;
    }

    public void setExpression(int e) {
        _expression = e;
        _updateVolumes();
    }

    // update all volumes of SiON tracks
    private void _updateVolumes() {
        int v = (_masterVolume * _expression + 64) >> 7;
        _sionVolumes[0] = _effectSendLevels[0] = v;
        for (int i = 1; i < 8; i++) {
            _sionVolumes[i] = (v * _effectSendLevels[i] + 64) >> 7;
        }
    }

    // constructor
    //

    /** */
    public MIDIModuleChannel() {
        mute = false;
        eventTriggerID = 0;
        eventTriggerTypeOn = 0;
        eventTriggerTypeOff = 0;
        sionMIDIEventType = SiONMIDIEventFlag.ALL;
        reset();
    }

    // operations
    //

    /** reset this channel */
    public void reset() {
        activeOperatorCount = 0;
        maxOperatorCount = 1024;

        //mute = false;
        drumMode = 0;
        programNumber = 0;
        _expression = 127;
        _masterVolume = 64;
        pan = 0;
        modulation = 0;
        pitchBend = 0;
        channelAfterTouch = 0;
        sustainPedal = false;
        portamento = false;
        portamentoTime = 0;
        masterFineTune = 0;
        masterCoarseTune = 0;
        pitchBendSensitivity = 2;
        modulationCycleTime = 180;

        bankNumber = 0;

        _sionVolumes[0] = _masterVolume;
        _effectSendLevels[0] = _masterVolume;
        for (int i = 1; i < 8; i++) {
            _sionVolumes[i] = 0;
            _effectSendLevels[i] = 0;
        }
    }

    /**
     * get effect send level
     *
     * @param slotNumber effect slot number (1-8)
     * @return effect send level
     */
    public int getEffectSendLevel(int slotNumber) {
        return _effectSendLevels[slotNumber];
    }

    /**
     * set effect send level
     *
     * @param slotNumber effect slot number (1-8)
     * @param level      effect send level (0-127)
     */
    public void setEffectSendLevel(int slotNumber, int level) {
        _effectSendLevels[slotNumber] = level;
        _sionVolumes[slotNumber] = (_effectSendLevels[0] * _effectSendLevels[slotNumber] + 64) >> 7;
    }

    /**
     * set event trigger of this channel
     *
     * @param id          Event trigger ID of this track. This value can be refered from SiONTrackEvent.eventTriggerID.
     * @param noteOnType  Dispatching event type at note on. 0=no events, 1=NOTE_ON_FRAME, 2=NOTE_ON_STREAM, 3=both.
     * @param noteOffType Dispatching event type at note off. 0=no events, 1=NOTE_OFF_FRAME, 2=NOTE_OFF_STREAM, 3=both.
     * @see org.si.sion.events.SiONTrackEvent
     */
    public void setEventTrigger(int id, int noteOnType, int noteOffType) {
        eventTriggerID = id;
        eventTriggerTypeOn = noteOnType;
        eventTriggerTypeOff = noteOffType;
    }
}
