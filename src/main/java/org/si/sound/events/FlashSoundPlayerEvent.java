//
// Event for FlashSoundPlayer
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.events;

import java.util.function.Consumer;

import org.si.utils.Event;
import org.si.utils.IOErrorEvent;
import vavi.media.Sound;


/** FlashSoundPlayerEvent instanceof dispatched by FlashSoundPlayer. @see org.si.sound.FlashSoundPlayer */
public class FlashSoundPlayerEvent extends Event{

    // constants
    //

    /** Complete all loading sounds */
    static final String COMPLETE = "fspComplete";

    // properties
    //

    /** Target sound */
    public Sound getSound() {
        return _sound;
    }

    /** keyRangeFrom */
    public int getKeyRangeFrom() {
        return _keyRangeFrom;
    }

    /** keyRangeTo */
    public int getKeyRangeTo() {
        return _keyRangeTo;
    }

    /** */
    Sound _sound;
    /** */
    Consumer<FlashSoundPlayerEvent> _onComplete;
    /** */
    Consumer<FlashSoundPlayerEvent> _onError;
    /** */
    int _keyRangeFrom;
    /** */
    int _keyRangeTo;
    /** */
    public int _startPoint;
    /** */
    public int _endPoint;
    /** */
    public int _loopPoint;

    // functions
    //

    /** */
    public FlashSoundPlayerEvent(Sound sound, Consumer<FlashSoundPlayerEvent> onComplete, Consumer<FlashSoundPlayerEvent> onError, int keyRangeFrom, int keyRangeTo, int startPoint, int endPoint, int loopPoint) {
        super(COMPLETE, false, false);
        this._sound = sound;
        this._onComplete = onComplete;
        this._onError = onError;
        this._keyRangeFrom = keyRangeFrom;
        this._keyRangeTo = keyRangeTo;
        this._startPoint = startPoint;
        this._endPoint = endPoint;
        this._loopPoint = loopPoint;
        _sound.addEventListener(Event.COMPLETE, this::_handleComplete);
        _sound.addEventListener(IOErrorEvent.IO_ERROR, this::_handleError);
    }

    private void _handleComplete(Event e) {
        _sound.removeEventListener(Event.COMPLETE, this::_handleComplete);
        _sound.removeEventListener(IOErrorEvent.IO_ERROR, this::_handleError);
        _onComplete.accept(this);
    }

    private void _handleError(Event e) {
        _sound.removeEventListener(Event.COMPLETE, this::_handleComplete);
        _sound.removeEventListener(IOErrorEvent.IO_ERROR, this::_handleError);
        _onError.accept(this);
    }
}
