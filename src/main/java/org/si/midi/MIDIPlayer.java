//
// MIDI file player class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.midi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.si.sion.SiONDriver;
import org.si.sion.events.SiONEvent;
import org.si.sion.midi.SMFData;
import org.si.utils.Event;
import org.si.utils.EventListener;
import vavi.net.URLRequest;


/** MIDI player */
public class MIDIPlayer {

    // variables
    //

    static private Map<String, SMFData> _cache = new HashMap<>(); // * = {}
    static private SiONDriver _driver = null;
    static private SMFData _nextData = null;
    static private boolean _fadeOut = false;
    static private boolean _isPlaying = false;
    static private SMFData _smfData = null;
    static public Consumer<SMFData> onFinishSequence = null;

    static public SMFData getSmfData() {
        return _smfData;
    }

    // properties
    //

    /** Playing position [sec] */
    static public double getPosition() {
        return getDriver().getPosition() * 0.001;
    }

    static public void setPosition(double pos) {
        getDriver().setPosition((int) (pos * 1000));
    }

    /** Playing volume [0-1] */
    public double getVolume() {
        return getDriver().getVolume();
    }

    public void setVolume(double v) {
        getDriver().setVolume(v);
    }

    /** tempo */
    static public double getTempo() {
        return getDriver().getBpm();
    }

    /** CPU loading [%] */
    static public double getCpuLoading() {
        return getDriver().getProcessTime() * 0.1;
    }

    /** Is paused ? */
    static public boolean isPaused() {
        return getDriver().isPaused();
    }

    /** Is playing ? */
    static public boolean isPlaying() {
        return getDriver().isPlaying();
    }

    /** SiON driver to play */
    static public SiONDriver getDriver() {
        if (_driver == null) {
            _driver = SiONDriver.mutex() != null ? SiONDriver.mutex() : new SiONDriver(4096, 2, 44100, 0);
        }
        return _driver;
    }

    // constructor
    //

    /** */
    MIDIPlayer() {
    }

    // operations
    //

    /**
     * play MIDI file
     *
     * @param url        MIDI file's URL
     * @param fadeInTime fade in time [second]
     * @return SMFData object to play
     */
    static public SMFData play(String url, double fadeInTime) {
        SMFData smfData = load(url);
        if (smfData != null && !smfData.isAvailable()) return smfData;

        _smfData = smfData;
        _play(smfData, fadeInTime);
        return smfData;
    }

    static public SMFData play(String url) {
        return play(url, 0);
    }

    /**
     * stop
     *
     * @param fadeOutTime fade out time [second]
     */
    static public void stop(double fadeOutTime) {
        if (fadeOutTime > 0) {
            _fadeOut = true;
            getDriver().fadeOut(fadeOutTime);
            getDriver().addEventListener(SiONEvent.FADE_OUT_COMPLETE, evt -> MIDIPlayer._stopWithFadeOut((SiONEvent) evt));
        } else {
            getDriver().stop();
        }
    }

    static public void stop() {
        stop(0);
    }

    /**
     * pause
     *
     * @param fadeOutTime fade out time [second]
     */
    static public void pause(double fadeOutTime) {
        if (fadeOutTime > 0) {
            getDriver().fadeOut(fadeOutTime);
            getDriver().addEventListener(SiONEvent.FADE_OUT_COMPLETE, evt -> MIDIPlayer._pauseWithFadeOut((SiONEvent) evt));
        } else {
            getDriver().pause();
        }
    }

    static public void pause() {
        pause(0);
    }

    /**
     * resume pausing
     *
     * @param fadeInTime fade in time [second]
     */
    static public void resume(double fadeInTime) {
        if (fadeInTime > 0) getDriver().fadeIn(fadeInTime);
        getDriver().resume();
    }

    static public void resume() {
        resume(0);
    }

    /**
     * load MIDI file without sounding
     *
     * @param url MIDI file's URL
     * @return SMFData object to load
     */
    static public SMFData load(String url) {
        SMFData smfData = _cache.get(url);
        if (smfData == null) {
            smfData = new SMFData();
            smfData.load(new URLRequest(url));
            _cache.put(url, smfData);
        }
        return smfData;
    }

    // handler
    //
    static private void _play(SMFData smfData, double fadeInTime) {
        if (isPlaying() && _fadeOut) {
            _nextData = smfData;
            getDriver().addEventListener(SiONEvent.STREAM_STOP, evt -> MIDIPlayer._playNextData((SiONEvent) evt));
        } else {
            _isPlaying = true;
            SiONDriver sionDriver = getDriver();
            if (sionDriver != null)
                sionDriver.addEventListener(SiONEvent.FINISH_SEQUENCE, (EventListener) evt -> _onFinishSequence(evt));
            getDriver().play(smfData, true);
            getDriver().fadeIn(fadeInTime);
        }
    }

    static private void _waitAndPlay(Object e) {
        _play((SMFData) ((Event) e).target, 0);
    }

    static private void _pauseWithFadeOut(SiONEvent e) {
        getDriver().removeEventListener(SiONEvent.FADE_OUT_COMPLETE, (EventListener) evt -> MIDIPlayer._pauseWithFadeOut((SiONEvent) evt));
        getDriver().pause();
    }

    static private void _stopWithFadeOut(SiONEvent e) {
        _fadeOut = false;
        getDriver().removeEventListener(SiONEvent.FADE_OUT_COMPLETE, evt -> MIDIPlayer._stopWithFadeOut((SiONEvent) evt));
        getDriver().stop();
    }

    static private void _playNextData(SiONEvent e) {
        getDriver().removeEventListener(SiONEvent.STREAM_STOP, evt -> MIDIPlayer._playNextData((SiONEvent) evt));
        if (_nextData.isAvailable()) _play(_nextData, 0);
        else _nextData.addEventListener(Event.COMPLETE, evt -> MIDIPlayer._waitAndPlay(evt));
        _nextData = null;
    }

    static private void _onFinishSequence(Object e) {
        _isPlaying = false;
        SiONDriver sionDriver = getDriver();
        if (sionDriver != null)
            sionDriver.removeEventListener(SiONEvent.FINISH_SEQUENCE, (EventListener) evt -> _onFinishSequence(evt));
        SMFData smfData = getSmfData();
        if (smfData != null && onFinishSequence != null) {
            onFinishSequence.accept(getSmfData());
        }
    }
}
