//
// Events for SiON Track
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.events;

import org.si.sion.SiONDriver;
import org.si.sion.midi.MIDIModule;
import org.si.sion.midi.MIDIModuleChannel;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.utils.Event;


/** SiON MIDI Event class. */
public class SiONMIDIEvent extends SiONTrackEvent {

    // constants
    //

    /**
     * Dispatch when the note on appears in MIDI data.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONDataConverterSMF instance.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * <tr><td>eventTriggerID</td><td>MIDI channel Number</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance to play</td></tr>
     * <tr><td>midiModule</td><td>MIDIModule instance to play</td></tr>
     * <tr><td>midiChannel</td><td>MIDIModuleChannel instance to play</td></tr>
     * <tr><td>midiChannelNumber</td><td>MIDI channel Number</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>value</td><td>velocity.</td></tr>
     * <tr><td>controllerNumber</td><td>((note) same) prop.</td></tr>
     * </table>
     *
     * @eventType soundTrigger
     */
    public static final String NOTE_ON = "midiNoteOn";

    /**
     * Dispatch when the note off appears in MIDI data.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONDataConverterSMF instance.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * <tr><td>eventTriggerID</td><td>MIDI channel Number</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance to play</td></tr>
     * <tr><td>midiModule</td><td>MIDIModule instance to play</td></tr>
     * <tr><td>midiChannel</td><td>MIDIModuleChannel instance to play</td></tr>
     * <tr><td>midiChannelNumber</td><td>MIDI channel Number</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>value</td><td>always 0.</td></tr>
     * <tr><td>controllerNumber</td><td>((note) same) prop.</td></tr>
     * </table>
     *
     * @eventType soundTrigger
     */
    public static final String NOTE_OFF = "midiNoteOff";

    /**
     * Dispatch when the control change command appears in MIDI data.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONDataConverterSMF instance.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * <tr><td>eventTriggerID</td><td>MIDI channel Number</td></tr>
     * <tr><td>track</td><td>null</td></tr>
     * <tr><td>midiModule</td><td>MIDIModule instance to play</td></tr>
     * <tr><td>midiChannel</td><td>MIDIModuleChannel instance to play</td></tr>
     * <tr><td>midiChannelNumber</td><td>MIDI channel Number</td></tr>
     * <tr><td>note</td><td>((controllerNumber) same) prop.</td></tr>
     * <tr><td>value</td><td>data value for the controller</td></tr>
     * <tr><td>controllerNumber</td><td>Controller number</td></tr>
     * </table>
     *
     * @eventType frameTrigger
     */
    public static final String CONTROL_CHANGE = "midiControlChange";

    /**
     * Dispatch when the program change command appears in MIDI data.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONDataConverterSMF instance.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * <tr><td>eventTriggerID</td><td>MIDI channel Number</td></tr>
     * <tr><td>track</td><td>null</td></tr>
     * <tr><td>midiModule</td><td>MIDIModule instance to play</td></tr>
     * <tr><td>midiChannel</td><td>MIDIModuleChannel instance to play</td></tr>
     * <tr><td>midiChannelNumber</td><td>MIDI channel Number</td></tr>
     * <tr><td>note</td><td>always 0</td></tr>
     * <tr><td>value</td><td>program number</td></tr>
     * <tr><td>controllerNumber</td><td>always 0</td></tr>
     * </table>
     *
     * @eventType frameTrigger
     */
    public static final String PROGRAM_CHANGE = "midiProgramChange";

    /**
     * Dispatch when the pitch bend command appears in MIDI data.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONDataConverterSMF instance.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * <tr><td>eventTriggerID</td><td>MIDI channel Number</td></tr>
     * <tr><td>track</td><td>null</td></tr>
     * <tr><td>midiModule</td><td>MIDIModule instance to play</td></tr>
     * <tr><td>midiChannel</td><td>MIDIModuleChannel instance to play</td></tr>
     * <tr><td>midiChannelNumber</td><td>MIDI channel Number</td></tr>
     * <tr><td>note</td><td>always 0</td></tr>
     * <tr><td>value</td><td>bend value</td></tr>
     * <tr><td>controllerNumber</td><td>always 0</td></tr>
     * </table>
     *
     * @eventType stream
     */
    public static final String PITCH_BEND = "midiPitchBend";

    // variables
    //

    // 2nd value
    private final int _2ndValue;
    // midi channel
    private final MIDIModuleChannel _midiChannel;

    // properties
    //

    /** controller number of CONTROL_CHANGE, ((SiONTrackEvent) same)'s note */
    public int getControllerNumber() {
        return _note;
    }

    /** data(CONTROL_CHANGE), program number(PROGRAM_CHANGE), bendvalue(PITCH_BEND) or velocity(NOTE events). */
    public int getValue() {
        return _2ndValue;
    }

    /** MIDI sound module to play */
    public MIDIModule getMidiModule() {
        return _driver.getMidiModule();
    }

    /** MIDI channel instance */
    public MIDIModuleChannel getMidiChannel() {
        return _midiChannel;
    }

    /** MIDI channel Number, ((SiON) same) event trigger ID. */
    public int getMidiChannelNumber() {
        return _eventTriggerID;
    }

    // functions
    //

    /** This event can be created only in the callback function inside. @private */
    public SiONMIDIEvent(String type, SiONDriver driver, SiMMLTrack track, int channelNumber, int bufferIndex, int note, int value) {
        super(type, driver, track, bufferIndex, note, channelNumber);
        _midiChannel = _driver.getMidiModule().midiChannels[channelNumber];
        _eventTriggerID = channelNumber;
        _note = note;
        _2ndValue = value;
    }

    @Override
    public Event clone() {
        SiONMIDIEvent event = new SiONMIDIEvent(type, _driver, _track, getMidiChannelNumber(), _bufferIndex, _note, _2ndValue);
        event._bufferIndex = _bufferIndex;
        event._frameTriggerDelay = _frameTriggerDelay;
        event._frameTriggerTimer = _frameTriggerTimer;
        return event;
    }
}
