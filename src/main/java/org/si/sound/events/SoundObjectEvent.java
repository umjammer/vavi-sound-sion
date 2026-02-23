//
// SoundObjectEvent
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.events;

import org.si.sion.events.SiONTrackEvent;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sound.SoundObject;


/** SoundObjectEvent instanceof dispatched by all SoundObjects. @see org.si.sound.SoundObject */
public class SoundObjectEvent extends org.si.utils.Event {

    // constants
    // 

    /**
     * Dispatch when the note on appears.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>soundObject</td><td>Target SoundObject.</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed by setEventTrigger().</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType soundTrigger
     */
    public static final String NOTE_ON_STREAM = "noteOnStream";

    /**
     * Dispatch when the note off appears in the sequence.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>soundObject</td><td>Target SoundObject.</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed by setEventTrigger().</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType soundTrigger
     */
    public static final String NOTE_OFF_STREAM = "noteOffStream";

    /**
     * Dispatch when the sound starts.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>soundObject</td><td>Target SoundObject.</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed by setEventTrigger().</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType frameTrigger
     */
    public static final String NOTE_ON_FRAME = "noteOnFrame";

    /**
     * Dispatch when the sound ends.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>soundObject</td><td>Target SoundObject.</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed by setEventTrigger().</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType frameTrigger
     */
    public static final String NOTE_OFF_FRAME = "noteOffFrame";

    /**
     * Dispatch in each frame in PatternSequencer.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>soundObject</td><td>Target SoundObject.</td></tr>
     * <tr><td>track</td><td>null. no meanings.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed by setEventTrigger().</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>bufferIndex</td><td>0. no meanings</td></tr>
     * </table>
     *
     * @eventType sequencerTrigger
     */
    public static final String ENTER_FRAME = "soundObjectEnterFrame";

    /**
     * Dispatch in each segment in PatternSequencer.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>soundObject</td><td>Target SoundObject.</td></tr>
     * <tr><td>track</td><td>null. no meanings.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed by setEventTrigger().</td></tr>
     * <tr><td>note</td><td>0. no meanings</td></tr>
     * <tr><td>bufferIndex</td><td>0. no meanings</td></tr>
     * </table>
     *
     * @eventType sequencerTrigger
     */
    public static final String ENTER_SEGMENT = "soundObjectEnterSegment";

    // variables
    // 

    /** target sound object */
    public SoundObject _soundObject;

    /** current track */
    public SiMMLTrack _track;

    /** trigger event id */
    public int _eventTriggerID;

    /** note number */
    public int _note;

    /** buffering index */
    public int _bufferIndex;

    // properties
    // 

    /** Target sound object */
    public SoundObject getSoundObject() {
        return _soundObject;
    }

    /** Sequencer track instance. */
    public SiMMLTrack getTrack() {
        return _track;
    }

    /** Trigger ID. */
    public int getEventTriggerID() {
        return _eventTriggerID;
    }

    /** Note number. */
    public int getNote() {
        return _note;
    }

    /** Buffering index. */
    public int getBufferIndex() {
        return _bufferIndex;
    }

    // functions
    // 

    /** */
    public SoundObjectEvent(String type, SoundObject soundObject, SiONTrackEvent trackEvent) {
        super(type, false, false);
        _soundObject = soundObject;
        if (trackEvent != null) {
            _track = trackEvent.getTrack();
            _eventTriggerID = trackEvent.getEventTriggerID();
            _note = trackEvent.getNote();
            _bufferIndex = trackEvent.getBufferIndex();
        }
    }
}
