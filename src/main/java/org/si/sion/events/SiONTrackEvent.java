//
// Events for SiON Track
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.events;

import org.si.sion.SiONDriver;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.utils.Event;


/** SiON Track Event class. */
public class SiONTrackEvent extends SiONEvent {

    // constants
    //

    /**
     * Dispatch when the note on appears in the sequence with "%t" command.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>true; mute the note</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONData instance. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed in "%t" commands 1st argument.</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType soundTrigger
     */
    public static final String NOTE_ON_STREAM = "noteOnStream";

    /**
     * Dispatch when the note off appears in the sequence with "%t" command.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>true; mute the note</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONData instance. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed in "%t" commands 1st argument.</td></tr>
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
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONData instance. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed in "%t" commands 1st argument.</td></tr>
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
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONData instance. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>track</td><td>SiMMLTrack instance executing sequence.</td></tr>
     * <tr><td>eventTriggerID</td><td>Trigger ID specifyed in "%t" commands 1st argument.</td></tr>
     * <tr><td>note</td><td>Note number.</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType frameTrigger
     */
    public static final String NOTE_OFF_FRAME = "noteOffFrame";

    /**
     * Dispatch on beat while streaming. This event instanceof called in each beat timing on frame. When you want to listen this event, you have to set addEventListener() before SiONDriver.play().
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance playing now.</td></tr>
     * <tr><td>data</td><td>SiONData instance playing now. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null.</td></tr>
     * <tr><td>track</td><td>null</td></tr>
     * <tr><td>eventTriggerID</td><td>Counter in 16th beat.</td></tr>
     * <tr><td>note</td><td>0</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType stream
     */
    public static final String BEAT = "beat";

    /**
     * Dispatch when the bpm changes.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONData instance. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>track</td><td>null</td></tr>
     * <tr><td>eventTriggerID</td><td>null</td></tr>
     * <tr><td>note</td><td>0</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType changeBPM
     */
    public static final String CHANGE_BPM = "changeBPM";

    /**
     * Dispatch when SiONDriver.dispatchUserDefinedTrackEvent() is called.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance.</td></tr>
     * <tr><td>data</td><td>SiONData instance. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * <tr><td>track</td><td>null</td></tr>
     * <tr><td>eventTriggerID</td><td>1st argument of SiONDriver.dispatchUserDefinedTrackEvent()</td></tr>
     * <tr><td>note</td><td>2nd argument of SiONDriver.dispatchUserDefinedTrackEvent()</td></tr>
     * <tr><td>bufferIndex</td><td>Buffering index</td></tr>
     * </table>
     *
     * @eventType changeBPM
     */
    public static final String USER_DEFINED = "userDefined";

    // variables
    //

    /** @private current track */
    protected SiMMLTrack _track;

    /** @private trigger event id */
    protected int _eventTriggerID;

    /** @private note number */
    protected int _note;

    /** @private buffering index */
    protected int _bufferIndex;

    /** @private frame trigger delay */
    protected double _frameTriggerDelay;

    /** @private Delay frame timer */
    protected int _frameTriggerTimer;

    // properties
    //

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

    /** Delay time to dispatch frame trigger event [ms]. */
    public double getFrameTriggerDelay() {
        return _frameTriggerDelay;
    }

    // functions
    //

    /** This event can be created only in the callback function inside. @private */
    public SiONTrackEvent(String type, SiONDriver driver, SiMMLTrack track, int bufferIndex, int note, int id) {
        super(type, driver, null, true);
        _track = track;
        if (track != null) {
            _note = track.getNote();
            _eventTriggerID = track.getEventTriggerID();
            _bufferIndex = track.channel.getBufferIndex();
            _frameTriggerDelay = track.channel.getBufferIndex() / driver.sequencer.sampleRate + driver.getLatency();
            _frameTriggerTimer = (int) _frameTriggerDelay;
        } else {
            _note = note;
            _eventTriggerID = id;
            _bufferIndex = bufferIndex;
            _frameTriggerDelay = bufferIndex / driver.sequencer.sampleRate + driver.getLatency();
            _frameTriggerTimer = (int) _frameTriggerDelay;
        }
    }

    public SiONTrackEvent(String type, SiONDriver driver, SiMMLTrack track) {
        this(type, driver, track, 0, 0, 0);
    }

    public SiONTrackEvent(String type, SiONDriver driver, SiMMLTrack track, int bufferIndex) {
        this(type, driver, track, bufferIndex, 0, 0);
    }

    /** clone. */
    @Override
    public Event clone() {
        SiONTrackEvent event = new SiONTrackEvent(type, _driver, _track);
        event._eventTriggerID = _eventTriggerID;
        event._note = _note;
        event._bufferIndex = _bufferIndex;
        event._frameTriggerDelay = _frameTriggerDelay;
        event._frameTriggerTimer = _frameTriggerTimer;
        return event;
    }

    /** */
    public boolean _decrementTimer(int frameRate) {
        _frameTriggerTimer -= frameRate;
        return (_frameTriggerTimer <= 0);
    }
}
