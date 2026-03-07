//
// Flash Media Sound player class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import org.si.sion.SiONVoice;
import org.si.sound.events.FlashSoundPlayerEvent;
import org.si.sound.synthesizers.SamplerSynth;
import org.si.sound.synthesizers.VoiceReference;
import org.si.as3.net.URLRequest;
import org.si.as3.media.Sound;
import org.si.as3.media.SoundLoaderContext;
import org.si.utils.ProgressEvent;
import org.si.utils.Event;
import org.si.utils.IOErrorEvent;

/** @eventType flash.events.Event */
// [Event(name="fspComplete", type="org.si.sound.events.FlashSoundPlayerEvent")]
/** @eventType flash.events.Event */
// [Event(name="open",     type="flash.events.Event")]
/** @eventType flash.events.Event */
// [Event(name="id3",      type="flash.events.Event")]
/** @eventType flash.events.IOErrorEvent */
// [Event(name="ioError",  type="flash.events.IOErrorEvent")]
/** @eventType flash.events.ProgressEvent */
// [Event(name="progress", type="flash.events.ProgressEvent")]

/** FlashSoundPlayer provides advanced operations of Sound class (in flash media package). */
public class FlashSoundPlayer extends PatternSequencer {

    // variables
    //

    /** sound instance to play */
    protected Sound _soundData = null;

    /** is sound data available to play ? */
    protected boolean _isSoundDataAvailable;

    /** synthsizer to play sound */
    protected SamplerSynth _flashSoundOperator;

    /** playing mode, 0=stopped, 1=wait for loading, 2=((single) play) note, 3=play by pattern sequencer */
    protected int _playingMode;

    /** waiting loading event count */
    protected int _createdEventCount;

    /** completed loading event count */
    protected int _completedEventCount;

    // properties
    //

    /** the Sequencer instance belonging to this PatternSequencer, where the sequence pattern appears. */
    public Sound getSoundData() {
        return _soundData;
    }

    public void setSoundData(Sound s) {
        _soundData = s;
        if (_soundData == null || (_soundData.bytesTotal > 0 && _soundData.bytesLoaded == _soundData.bytesTotal))
            _setSoundData(_soundData, 0, 127, 0, -1, -1);
        else _addLoadingJob(_soundData, 0, 127, 0, -1, -1);
    }

    /** is playing ? */
    @Override
    public boolean isPlaying() {
        return (_playingMode != 0);
    }

    /** is sound data available to play ? */
    public boolean getIsSoundDataAvailable() {
        return _isSoundDataAvailable;
    }

    /** Voice data to play, You cannot change the voice of this sound object. */
    @Override
    public SiONVoice getVoice() {
        return _synthesizer.getVoice();
    }

    @Override
    public void setVoice(SiONVoice v) {
        throw new Error("FlashSoundPlayer; You cannot change voice of this sound object.");
    }

    /** Synthesizer to generate sound, You cannot change the synthesizer of this sound object */
    @Override
    public VoiceReference getSynthesizer() {
        return _synthesizer;
    }

    @Override
    public void setSynthesizer(VoiceReference s) {
        throw new Error("FlashSoundPlayer; You cannot change synthesizer of this sound object.");
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param soundData flash.media.Sound instance to control.
     */
    public FlashSoundPlayer(Sound soundData) {
        super(68, 128, 0, null);
        name = "FlashSoundPlayer";
        _isSoundDataAvailable = false;
        _playingMode = 0;
        _flashSoundOperator = new SamplerSynth(null, false, 2);
        _synthesizer = _flashSoundOperator;
        _createdEventCount = 0;
        _completedEventCount = 0;
        this.setSoundData(soundData);
    }

    // operations
    //

    /** start sequence */
    @Override
    public void play() {
        _playingMode = 1;
        if (_isSoundDataAvailable) _playSound();
    }

    /** stop sequence */
    @Override
    public void stop() {
        switch (_playingMode) {
            case 2:
                if (_track != null) {
                    _synthesizer._unregisterTracks(_track, 1);
                    _track.setDisposable();
                    _track = null;
                    _noteOff(-1, false);
                }
                _stopEffect();
                break;
            case 3:
                super.stop();
                break;
        }
        _playingMode = 0;
    }

    /**
     * load sound from url, this method instanceof the simplificaion of setSoundData(new Sound(url, context)).
     *
     * @private url ((Sound) same).load
     * @private context ((Sound) same).load
     */
    public void load(URLRequest url, SoundLoaderContext context) {
        _soundData = new Sound(url, context);
        _addLoadingJob(_soundData, 0, 127, 0, -1, -1);
    }

    /**
     * Set flash sound instance with key range.
     *
     * @param sound        Sound instance to assign
     * @param keyRangeFrom Assigning key range starts from
     * @param keyRangeTo   Assigning key range ends at. -1 to set only at the key of argument "keyRangeFrom".
     * @param startPoint   slicing point to start data.
     * @param endPoint     slicing point to end data. The negative value plays whole data.
     * @param loopPoint    slicing point to repeat data. -1 means no repeat
     */
    public void setSoundData(Sound sound, int keyRangeFrom, int keyRangeTo, int startPoint, int endPoint, int loopPoint) {
        if (sound.bytesLoaded == sound.bytesTotal)
            _setSoundData(sound, keyRangeFrom, keyRangeTo, startPoint, endPoint, loopPoint);
        else _addLoadingJob(sound, keyRangeFrom, keyRangeTo, startPoint, endPoint, loopPoint);
    }

    // internal
    //
    private void _setSoundData(Sound sound, int keyRangeFrom, int keyRangeTo, int startPoint, int endPoint, int loopPoint) {
        _isSoundDataAvailable = true;
        _flashSoundOperator.setSample(sound, false, keyRangeFrom, keyRangeTo, 2).slice(startPoint, endPoint, loopPoint);
        if (_createdEventCount == _completedEventCount && _playingMode == 1) _playSound();
    }

    private void _playSound() {
        if (_sequencer.pattern != null) {
            // play by PatternSequencer
            _playingMode = 3;
            super.play();
        } else {
            // play as single note
            _playingMode = 2;
            stop();
            _track = _noteOn(_note, false);
            if (_track != null) _synthesizer._registerTrack(_track);
        }
    }

    private void _addLoadingJob(Sound sound, int keyRangeFrom, int keyRangeTo, int startPoint, int endPoint, int loopPoint) {
        FlashSoundPlayerEvent event = new FlashSoundPlayerEvent(sound, this::_onComplete, this::_onError, keyRangeFrom, keyRangeTo, startPoint, endPoint, loopPoint);
        _createdEventCount++;
        sound.addEventListener(Event.ID3, this::_onID3);
        sound.addEventListener(Event.OPEN, this::_onOpen);
        sound.addEventListener(ProgressEvent.PROGRESS, this::_onProgress);
    }

    private void _removeAllEventListeners(FlashSoundPlayerEvent event) {
        _completedEventCount++;
        event.getSound().removeEventListener(Event.ID3, this::_onID3);
        event.getSound().removeEventListener(Event.OPEN, this::_onOpen);
        event.getSound().removeEventListener(ProgressEvent.PROGRESS, this::_onProgress);
    }

    private void _onComplete(FlashSoundPlayerEvent event) {
        _removeAllEventListeners(event);
        dispatchEvent(event);
        _setSoundData(event.getSound(), event.getKeyRangeFrom(), event.getKeyRangeTo(), event._startPoint, event._endPoint, event._loopPoint);
    }

    private void _onError(FlashSoundPlayerEvent event) {
        _removeAllEventListeners(event);
        dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, "IOError during loading Sound."));
    }

    private void _onID3(Event event) {
        dispatchEvent(new Event(Event.ID3));
    }

    private void _onOpen(Event event) {
        dispatchEvent(new Event(Event.OPEN));
    }

    private void _onProgress(Event event) {
        if (event instanceof ProgressEvent) {
            dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS, false, false, _completedEventCount, _createdEventCount));
        }
    }
}
