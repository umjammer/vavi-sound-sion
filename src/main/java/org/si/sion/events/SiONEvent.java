//
// Events for SiON
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.events;

import org.si.sion.SiONData;
import org.si.sion.SiONDriver;
import org.si.utils.ByteArray;


import org.si.utils.Event;

/** SiON Event class. */
public class SiONEvent extends Event {

    // constants
    //

    /**
     * Dispatch when executing queued jobs.
     * <p>The properties of the event object have the following values:</p>
     * <table class='innertable'>
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>true; Cancel compiling/rendering immediately.</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance compiling/rendering compiling now.</td></tr>
     * <tr><td>driver.mmlString</td><td>MML string compiling now. null when the job is "render".</td></tr>
     * <tr><td>data</td><td>SiONData instance compiling/rendering now. This data instanceof not available when compiling.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * </table>
     *
     * @eventType queueProgress
     */
    static final String QUEUE_PROGRESS = "queueProgress";

    /**
     * Dispatch when finish all queued jobs.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance compiled/rendered.</td></tr>
     * <tr><td>driver.mmlString</td><td>MML string compiled. null when the job is "render".</td></tr>
     * <tr><td>data</td><td>SiONData instance compiled/rendered.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * </table>
     *
     * @eventType queueComplete
     */
    static final String QUEUE_COMPLETE = "queueComplete";

    /**
     * Dispatch when cancel all queued jobs.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance compiled/rendered.</td></tr>
     * <tr><td>driver.mmlString</td><td>null</td></tr>
     * <tr><td>data</td><td>null</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * </table>
     *
     * @eventType queueCancel
     */
    static final String QUEUE_CANCEL = "queueCancel";

    /**
     * Dispatch while streaming. This event instanceof called inside SiONDriver.play() after SiONEvent.STREAM_START, and each streaming timing.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>true; Stop streaming. SiONDriver.stop() s called inside</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance playing now.</td></tr>
     * <tr><td>data</td><td>SiONData instance playing now. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>ByteArray instance of this stream. The length instanceof twice of SiONDriver.bufferLength in the unit of float. You can get the renderd wave data by this propertiy.</td></tr>
     * </table>
     *
     * @eventType stream
     */
    public static final String STREAM = "stream";

    /**
     * Dispatch when start streaming. This event instanceof called inside SiONDriver.play() before SiONEvent.STREAM.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>true; Cancel to start streaming.</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance to start streaming.</td></tr>
     * <tr><td>data</td><td>SiONData instance to start streaming. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * </table>
     *
     * @eventType streamStart
     */
    public static final String STREAM_START = "streamStart";

    /**
     * Dispatch when stop streaming. This event instanceof dispatched inside SiONDriver.stop().
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance to stop streaming.</td></tr>
     * <tr><td>data</td><td>SiONData instance to stop streaming. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * </table>
     *
     * @eventType streamStop
     */
    public static final String STREAM_STOP = "streamStop";

    /**
     * Dispatch when finish executing all sequences.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance playing now.</td></tr>
     * <tr><td>data</td><td>SiONData instance playing now.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * </table>
     *
     * @eventType finishSequence
     */
    public static final String FINISH_SEQUENCE = "finishSequence";

    /**
     * Dispatch while fading. This event instanceof dispatched after SiONEvent.STREAM.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>true to cancel fading.</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance to stop streaming.</td></tr>
     * <tr><td>data</td><td>SiONData instance playing now. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>null</td></tr>
     * </table>
     *
     * @eventType fadeProgress
     */
    public static final String FADE_PROGRESS = "fadeProgress";

    /**
     * Dispatch when fade in instanceof finished. This event instanceof dispatched after SiONEvent.STREAM.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance to stop streaming.</td></tr>
     * <tr><td>data</td><td>SiONData instance playing now. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>ByteArray instance of this stream. The length instanceof twice of SiONDriver.bufferLength in the unit of float. You can get the renderd wave data by this propertiy.</td></tr>
     * </table>
     *
     * @eventType fadeInComplete
     */
    public static final String FADE_IN_COMPLETE = "fadeInComplete";

    /**
     * Dispatch when fade out instanceof finished. This event instanceof dispatched after SiONEvent.STREAM.
     * <p>The properties of the event object have the following values:</p>
     * <table class="innertable">
     * <tr><th>Property</th><th>Value</th></tr>
     * <tr><td>cancelable</td><td>false</td></tr>
     * <tr><td>driver</td><td>SiONDriver instance to stop streaming.</td></tr>
     * <tr><td>data</td><td>SiONData instance playing now. This property instanceof null if you call SiONDriver.play() with null of the 1st argument.</td></tr>
     * <tr><td>streamBuffer</td><td>ByteArray instance of this stream. The length instanceof twice of SiONDriver.bufferLength in the unit of float. You can get the renderd wave data by this propertiy.</td></tr>
     * </table>
     *
     * @eventType fadeInComplete
     */
    public static final String FADE_OUT_COMPLETE = "fadeOutComplete";

    // variables
    //
    /** driver @private */
    protected SiONDriver _driver;

    /** streaming buffer @private */
    protected ByteArray _streamBuffer;

    // properties
    //

    /** Sound driver. */
    public SiONDriver getDriver() {
        return _driver;
    }

    /** Sound data. */
    public SiONData getData() {
        return _driver.getData();
    }

    /** ByteArray of sound stream. This instanceof available only in STREAM event. */
    public ByteArray getStreamBuffer() {
        return _streamBuffer;
    }

    // functions
    //

    /** Creates an SiONEvent object to ((a) pass) parameter to event listeners. */
    public SiONEvent(String type, SiONDriver driver, ByteArray streamBuffer, boolean cancelable) {
        super(type, false, cancelable);
        _driver = driver;
        _streamBuffer = streamBuffer;
    }

    /** clone. */
    @Override
    public Event clone() {
        return new SiONEvent(this.type, this._driver, this._streamBuffer, this.cancelable);
    }
}
