//
// Sound object
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.List;

import org.si.sion.SiONData;
import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.events.SiONEvent;
import org.si.sion.events.SiONTrackEvent;
import org.si.sion.module.SiOPMModule;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.utils.Fader;
import org.si.sound.core.EffectChain;
import org.si.sound.events.SoundObjectEvent;
import org.si.sound.patterns.Sequencer;
import org.si.sound.synthesizers.VoiceReference;


/** @eventType org.si.sound.events.SoundObjectEvent.NOTE_ON_STREAM */
// [Event(name="noteOnStream",    type="org.si.sound.events.SoundObjectEvent")]
/** @eventType org.si.sound.events.SoundObjectEvent.NOTE_OFF_STREAM */
// [Event(name="noteOffStream",   type="org.si.sound.events.SoundObjectEvent")]
/** @eventType org.si.sound.events.SoundObjectEvent.NOTE_ON_FRAME */
// [Event(name="noteOnFrame",     type="org.si.sound.events.SoundObjectEvent")]
/** @eventType org.si.sound.events.SoundObjectEvent.NOTE_OFF_FRAME */
// [Event(name="noteOffFrame",    type="org.si.sound.events.SoundObjectEvent")]


/**
 * The SoundObject class instanceof the base class of all objects that operates sounds by SiONDriver.
 */
public class SoundObject extends org.si.utils.EventDispatcher {

    // variables
    //

    /** Name. */
    public String name;

    /** Base note of this sound */
    protected int _note;
    /** Synthesizer instance */
    protected VoiceReference _synthesizer;
    /** Synthesizer instance to use SiONVoice */
    protected VoiceReference _voiceReference;
    /** Effect chain instance */
    protected EffectChain _effectChain;
    /** track for noteOn() */
    protected SiMMLTrack _track;
    /** tracks for sequenceOn() */
    protected List<SiMMLTrack> _tracks;
    /** Auto-fader to fade in/out. */
    protected Fader _fader;
    /** Fader volume. */
    protected double _faderVolume;

    /** Sound length uint in 16th beat, 0 sets inifinity length. @default 0. */
    protected double _length;
    /** Sound delay uint in 16th beat. @default 0. */
    protected double _delay;
    /** Synchronizing uint in 16th beat. (0:No synchronization, 1:sync.with 16th, 4:sync.with 4th). @default 0. */
    protected double _quantize;

    /** Note shift in half-tone unit. */
    protected int _noteShift;
    /** Pitch shift in half-tone unit. */
    protected double _pitchShift;
    /** gate ratio (value of 'q' command * 0.125) */
    protected double _gateTime;
    /** Event mask (value of '&#64;mask' command) */
    protected double _eventMask;
    /** Event trigger ID */
    protected int _eventTriggerID;
    /** note on trigger | (note off trigger &lt;&lt; 2) trigger type */
    protected int _noteTriggerFlags;
    /** listening note event trigger */
    protected int _listeningFlags;

    /** volumes for all streams */
    protected int[] _volumes;
    /** total panning of all ancestors */
    protected double _pan;
    /** total mute flag of all ancestors */
    protected boolean _mute;
    /** Pitch bend in half-tone unit. */
    protected double _pitchBend;

    /** parent container */
    protected SoundObjectContainer _parent;
    /** the depth of parent-child chain */
    protected int _childDepth;
    /** volume of this sound object */
    protected double _thisVolume;
    /** panning of this sound object */
    protected double _thisPan;
    /** mute flag of this sound object */
    protected boolean _thisMute;

    /** track id. This value instanceof asigned when its created. */
    protected int _trackID;

    // properties
    //

    /** SiONDriver instrance to operate. this returns null when driver instanceof not created. */
    public SiONDriver getDriver() {
        return SiONDriver.mutex();
    }

    /** parent container. */
    public SoundObjectContainer getParent() {
        return _parent;
    }

    /** is playing ? */
    public boolean isPlaying() {
        return (_track != null);
    }

    /** Base note of this sound */
    public int getNote() {
        return _note;
    }

    public void setNote(int n) {
        _note = n;
    }

    /** Voice data to play */
    public SiONVoice getVoice() {
        return _synthesizer.getVoice();
    }

    public void setVoice(SiONVoice v) {
        _voiceReference.setVoice(v);
        if (!isPlaying()) _synthesizer = _voiceReference;
    }

    /** Synthesizer to generate sound */
    public VoiceReference getSynthesizer() {
        return _synthesizer;
    }

    public void setSynthesizer(VoiceReference s) {
        if (isPlaying()) throw new RuntimeException("SoundObject: Synthesizer should not be changed during playing.");
        _synthesizer = s != null ? s : _voiceReference;
    }

    /** Sound length in 16th beat, 0 sets inifinity length. @default 0. */
    public double getLength() {
        return _length;
    }

    public void setLength(double l) {
        _length = l;
    }

    /** Synchronizing quantizing, uint in 16th beat. (0:No synchronization, 1:sync.with 16th, 4:sync.with 4th). @default 0. */
    public double getQuantize() {
        return _quantize;
    }

    public void setQuantize(double q) {
        _quantize = q;
    }

    /** Sound delay, uint in 16th beat. @default 0. */
    public double getDelay() {
        return _delay;
    }

    public void setDelay(double d) {
        _delay = d;
    }

    /** Master coarse tuning, 1 for half-tone. */
    public int getCoarseTune() {
        return _noteShift;
    }

    public void setCoarseTune(int n) {
        _noteShift = n;
        if (_track != null) _track.noteShift = _noteShift;
    }

    /** Master fine tuning, 1 for half-tone, you can specify fineTune&lt;-1 or fineTune&gt;1. */
    public double getFineTune() {
        return _pitchShift * 0.015625;
    }

    public void setFineTune(double p) {
        _pitchShift = p;
        if (_track != null) _track.pitchShift = (int) (_pitchShift * 64);
    }

    /** Track gate time (0:Minimum - 1:Maximum). (value of 'q' command * 0.125) */
    public double getGateTime() {
        return _gateTime;
    }

    public void setGateTime(double g) {
        _gateTime = (g < 0) ? 0 : (g > 1) ? 1 : g;
        if (_track != null) _track.quantRatio = _gateTime;
    }

    /** Track event mask. (value of '&#64;mask' command) */
    public int getEventMask() {
        return (int) _eventMask;
    }

    public void setEventMask(int m) {
        _eventMask = m;
        if (_track != null) _track.eventMask = (int) _eventMask;
    }

    /** Track id */
    public int getTrackID() {
        return _trackID;
    }

    /** get track */
    public SiMMLTrack getTrack() {
        return _track;
    }

    /** Track event trigger ID */
    public int getEventTriggerID() {
        return _eventTriggerID;
    }

    public void setEventTriggerID(int id) {
        _eventTriggerID = id;
    }

    /** Track note on trigger type */
    public int getNoteOnTriggerType() {
        return _noteTriggerFlags & 3;
    }

    /** Track note off trigger type */
    public int getNoteOffTriggerType() {
        return _noteTriggerFlags >> 2;
    }

    /** Channel mute, this property can control track after play(). */
    public boolean getMute() {
        return _thisMute;
    }

    public void setMute(boolean m) {
        _thisMute = m;
        _updateMute();
        if (_track != null) _track.channel.setMute(_mute);
    }

    /** Channel volume (0:Minimum - 1:Maximum), this property can control track after play(). */
    public double getVolume() {
        return _thisVolume;
    }

    public void setVolume(double v) {
        _thisVolume = v;
        _updateVolume();
        _limitVolume();
        _updateStreamSend(0, _volumes[0] * 0.0078125);
    }

    /** Channel panning (-1:Left - 0:Center - +1:Right), this property can control track after play(). */
    public double getPan() {
        return _thisPan;
    }

    public void setPan(double p) {
        _thisPan = p;
        _updatePan();
        _limitPan();
        if (_track != null) _track.channel.setPan((int) (_pan * 64));
    }

    /** Channel effect send level for slot 1 (0:Minimum - 1:Maximum), this property can control track after play(). */
    public double getEffectSend1() {
        return _volumes[1] * 0.0078125;
    }

    public void setEffectSend1(double v) {
        v = (v < 0) ? 0 : (v > 1) ? 1 : v;
        _volumes[1] = (int) (v * 128);
        _updateStreamSend(1, v);
    }

    /** Channel effect send level for slot 2 (0:Minimum - 1:Maximum), this property can control track after play(). */
    public double getEffectSend2() {
        return _volumes[2] * 0.0078125;
    }

    public void setEffectSend2(double v) {
        v = (v < 0) ? 0 : (v > 1) ? 1 : v;
        _volumes[2] = (int) (v * 128);
        _updateStreamSend(2, v);
    }

    /** Channel effect send level for slot 3 (0:Minimum - 1:Maximum), this property can control track after play(). */
    public double getEffectSend3() {
        return _volumes[3] * 0.0078125;
    }

    public void setEffectSend3(double v) {
        v = (v < 0) ? 0 : (v > 1) ? 1 : v;
        _volumes[3] = (int) (v * 128);
        _updateStreamSend(3, v);
    }

    /** Channel effect send level for slot 4 (0:Minimum - 1:Maximum), this property can control track after play(). */
    public double getEffectSend4() {
        return _volumes[4] * 0.0078125;
    }

    public void setEffectSend4(double v) {
        v = (v < 0) ? 0 : (v > 1) ? 1 : v;
        _volumes[4] = (int) (v * 128);
        _updateStreamSend(4, v);
    }

    /** Channel pitch bend, 1 for halftone, this property can control track after play(). */
    public double getPitchBend() {
        return _pitchBend;
    }

    public void setPitchBend(double p) {
        _pitchBend = p;
        if (_track != null) _track.setPitchBend((int) (p * 64));
    }


    /** Array of SiEffectBase to modify this sound object's output. */
    public org.si.sion.effector.SiEffectBase[] getEffectors() {
        return (_effectChain != null) ? _effectChain.getEffectList() : null;
    }

    public void setEffectors(org.si.sion.effector.SiEffectBase[] effectList) {
        if (_effectChain != null) {
            _effectChain.setEffectList(effectList);
        } else {
            if (effectList != null && effectList.length > 0) {
                _effectChain = org.si.sound.core.EffectChain.alloc(effectList);
            }
        }
    }

    // counter to asign unique track id
    private int _uniqueTrackID = 0;

    // constructor
    //

    /** constructor. */
    SoundObject(String name, VoiceReference synth /* = null */) {
        this.name = name != null ? name : "";
        _parent = null;
        _childDepth = 0;
        _voiceReference = new VoiceReference();
        _synthesizer = synth != null ? synth :_voiceReference;
        _effectChain = null;
        _track = null;
        _tracks = null;
        _fader = new Fader(null, 0, 1, 0);
        _volumes = new int[SiOPMModule.STREAM_SEND_SIZE];
        _faderVolume = 1;

        _note = 60;
        _length = 0;
        _delay = 0;
        _quantize = 1;

        _volumes[0] = 64;
        for (int i = 1; i < SiOPMModule.STREAM_SEND_SIZE; i++) _volumes[i] = 0;
        _pan = 0;
        _mute = false;
        _pitchBend = 0;

        _gateTime = 0.75;
        _noteShift = 0;
        _pitchShift = 0;
        _eventMask = 0;
        _eventTriggerID = 0;
        _noteTriggerFlags = 0;
        _listeningFlags = 0;

        _thisVolume = 0.5;
        _thisPan = 0;
        _thisMute = false;

        _trackID = (_uniqueTrackID & 0x7fff) | 0x8000;
        _uniqueTrackID++;
    }

    // settings
    //

    /** Reset */
    public void reset() {
        stop();

        _note = 60;
        _length = 0;
        _delay = 0;
        _quantize = 1;

        _fader.setFade(null, 0, 1, 0);
        _effectChain = null;
        _volumes[0] = 64;
        for (int i = 1; i < SiOPMModule.STREAM_SEND_SIZE; i++) _volumes[i] = 0;
        _faderVolume = 1;
        _pan = 0;
        _mute = false;
        _pitchBend = 0;

        _gateTime = 0.75;
        _noteShift = 0;
        _pitchShift = 0;
        _eventMask = 0;
        _eventTriggerID = 0;
        _noteTriggerFlags = 0;
        _listeningFlags = 0;

        _thisVolume = 0.5;
        _thisPan = 0;
        _thisMute = false;
    }

    /**
     * Set volume by index.
     *
     * @param slot   streaming slot number.
     * @param volume volume (0:Minimum - 1:Maximum).
     */
    public void setVolume(int slot, double volume) {
        _volumes[slot] = (volume < 0) ? 0 : (int) ((volume > 1) ? 128 : (volume * 128));
    }

    /**
     * Set fading in.
     *
     * @param time fading time[sec].
     */
    public void fadeIn(double time) {
        SiONDriver drv = getDriver();
        if (drv != null) {
            if (!_fader.isActive()) {
                drv.addEventListener(SiONEvent.STREAM, _onStreamListener);
                drv.forceDispatchStreamEvent(false);
            }
            _fader.setFade(this::_fadeVolume, 0, 1, (int) (time * drv.getSampleRate() / drv.getBufferLength()));
        }
    }

    /**
     * Set fading out.
     *
     * @param time fading time[sec].
     */
    public void fadeOut(double time) {
        SiONDriver drv = getDriver();
        if (drv != null) {
            if (!_fader.isActive()) {
                drv.addEventListener(SiONEvent.STREAM, _onStreamListener);
                drv.forceDispatchStreamEvent(false);
            }
            _fader.setFade(this::_fadeVolume, 1, 0, (int) (time * drv.getSampleRate() / drv.getBufferLength()));
        }
    }

    // operations
    //

    /** Play sound. */
    public void play() {
        stop();
        _track = _noteOn(_note, false);
        if (_track != null) _synthesizer._registerTrack(_track);
    }

    /** Stop sound. */
    public void stop() {
        if (_track != null) {
            _synthesizer._unregisterTracks(_track, 1);
            _track.setDisposable();
            _track = null;
            _noteOff(-1, false);
        }
        _stopEffect();
    }

    // operations
    //

    /**
     * @param note         playing note
     * @param isDisposable disposable flag.
     * @return playing track
     * driver.noteOn.
     */
    protected SiMMLTrack _noteOn(int note, boolean isDisposable) {
        if (getDriver() == null) return null;
        SiONVoice voice = _synthesizer.getVoice();
        EffectChain topEC = _topEffectChain();
        SiMMLTrack track = getDriver().noteOn(note, voice, _length, _delay, _quantize, _trackID, isDisposable);
        _addNoteEventListeners();
        if (_effectChain != null) {
            _effectChain._activateLocalEffect(_childDepth);
            _effectChain.setAllStreamSendLevels(_volumes);
        }
        if (topEC != null) {
            track.channel.setMasterVolume(128);
            track.channel.setStreamBuffer(0, topEC.getStreamingBuffer());
        } else {
            track.channel.setAllStreamSendLevels(_volumes);
        }
        track.channel.setPan((int) (_pan * 64));
        track.channel.setMute(_mute);
        track.setPitchBend((int) (_pitchBend * 64));
        track.noteShift = _noteShift;
        track.pitchShift = (int) (_pitchShift * 64);
        if (voice != null && Double.isNaN(voice.defaultGateTime)) track.quantRatio = _gateTime;
        return track;
    }

    /**
     * @param stopWithReset stop sound wit resetting channels process
     * @return stopped track list
     * driver.noteOff()
     */
    protected List<SiMMLTrack> _noteOff(int note, boolean stopWithReset) {
        if (getDriver() == null) return null;
        _removeNoteEventListeners();
        //if (_effectChain) _effectChain._inactivateLocalEffect();
        return getDriver().noteOff(note, _trackID, _delay, _quantize, stopWithReset);
    }

    /**
     * @param data         sequence data
     * @param isDisposable disposable flag
     * @param applyLength
     * @return vector of playing tracks
     * driver.sequenceOn()
     */
    protected List<SiMMLTrack> _sequenceOn(SiONData data, boolean isDisposable, boolean applyLength /* = true */) {
        if (getDriver() == null) return null;
        double len = (applyLength) ? _length : 0;
        SiONVoice voice = _synthesizer.getVoice();
        EffectChain topEC = _topEffectChain();
        var list = getDriver().sequenceOn(data, voice, len, _delay, _quantize, _trackID, isDisposable);
        int ps = (int) (_pitchShift * 64), pb = (int) (_pitchBend * 64);
        _addNoteEventListeners();
        if (_effectChain != null) {
            _effectChain._activateLocalEffect(_childDepth);
            _effectChain.setAllStreamSendLevels(_volumes);
        }
        for (SiMMLTrack track : list) {
            if (topEC != null) {
                track.channel.setMasterVolume(128);
                track.channel.setStreamBuffer(0, topEC.getStreamingBuffer());
            } else {
                track.channel.setAllStreamSendLevels(_volumes);
            }
            track.channel.setPan((int) (_pan * 64));
            track.channel.setMute(_mute);
            track.setPitchBend(pb);
            track.noteShift = _noteShift;
            track.pitchShift = ps;
            track.setEventTrigger(_eventTriggerID, _noteTriggerFlags & 3, _noteTriggerFlags >> 2);
            if (voice != null && Double.isNaN(voice.defaultGateTime)) track.quantRatio = _gateTime;
        }
        return list;
    }

    /**
     * @param stopWithReset stop sound wit resetting channels process
     * @return stopped track list
     * driver.sequenceOff()
     */
    protected List<SiMMLTrack> _sequenceOff(boolean stopWithReset) {
        if (getDriver() == null) return null;
        _removeNoteEventListeners();
        //if (_effectChain) _effectChain._inactivateLocalEffect();
        return getDriver().sequenceOff(_trackID, 0, _quantize, stopWithReset);
    }

    /** free effect chain if the effect list instanceof empty */
    protected void _stopEffect() {
        if (_effectChain != null && _effectChain.getEffectList() != null && _effectChain.getEffectList().length == 0) {
            org.si.sound.core.EffectChain.free(_effectChain);
            _effectChain = null;
        }
    }

    /** update stream send level */
    protected void _updateStreamSend(int streamNum, double level) {
        if (_track != null) {
            if (_effectChain != null) _effectChain.setStreamSend(streamNum, level);
            else _track.channel.setStreamSend(streamNum, level);
        }
    }

    private final org.si.utils.EventListener _onTrackEventListener = this::_onTrackEvent;

    /** add event trigger listeners */
    protected void _addNoteEventListeners() {
        if (_listeningFlags != 0) return;
        SiONDriver drv = getDriver();
        _noteTriggerFlags = 0;
        if (hasEventListener(SoundObjectEvent.NOTE_ON_FRAME)) {
            drv.addEventListener(SiONTrackEvent.NOTE_ON_FRAME, _onTrackEventListener);
            _noteTriggerFlags |= 1;
        }
        if (hasEventListener(SoundObjectEvent.NOTE_ON_STREAM)) {
            drv.addEventListener(SiONTrackEvent.NOTE_ON_STREAM, _onTrackEventListener);
            _noteTriggerFlags |= 2;
        }
        if (hasEventListener(SoundObjectEvent.NOTE_OFF_FRAME)) {
            drv.addEventListener(SiONTrackEvent.NOTE_OFF_FRAME, _onTrackEventListener);
            _noteTriggerFlags |= 4;
        }
        if (hasEventListener(SoundObjectEvent.NOTE_OFF_STREAM)) {
            drv.addEventListener(SiONTrackEvent.NOTE_OFF_STREAM, _onTrackEventListener);
            _noteTriggerFlags |= 8;
        }
        _listeningFlags = _noteTriggerFlags;
    }

    /** remove event trigger listeners */
    protected void _removeNoteEventListeners() {
        if (_listeningFlags == 0) return;
        SiONDriver drv = getDriver();
        if ((_listeningFlags & 1) != 0) drv.removeEventListener(SiONTrackEvent.NOTE_ON_FRAME, _onTrackEventListener);
        if ((_listeningFlags & 2) != 0) drv.removeEventListener(SiONTrackEvent.NOTE_ON_STREAM, _onTrackEventListener);
        if ((_listeningFlags & 4) != 0) drv.removeEventListener(SiONTrackEvent.NOTE_OFF_FRAME, _onTrackEventListener);
        if ((_listeningFlags & 8) != 0) drv.removeEventListener(SiONTrackEvent.NOTE_OFF_STREAM, _onTrackEventListener);
        _listeningFlags = 0;
    }

    /** handler for note event */
    protected void _onTrackEvent(Object e_) {
        SiONTrackEvent e = (SiONTrackEvent) e_;
        if (e.getTrack().getTrackID() == _trackID) {
            dispatchEvent(new SoundObjectEvent(e.type, this, e));
        }
    }

    // internals
    //

    // top effect chain
    EffectChain _topEffectChain() {
        return _effectChain != null ? _effectChain : ((_parent != null) ? _parent._topEffectChain() : null);
    }

    /** */
    void _setParent(SoundObjectContainer parent) {
        if (_parent != null) _parent.removeChild(this);
        _parent = parent;
        _updateChildDepth();
        _updateMute();
        _updateVolume();
        _limitVolume();
        _updatePan();
        _limitPan();
    }

    /** */
    void _updateChildDepth() {
        _childDepth = (_parent != null) ? (_parent._childDepth + 1) : 0;
    }

    /** */
    void _updateMute() {
        if (_parent != null) _mute = _parent._mute || _thisMute;
        else _mute = _thisMute;
    }

    /** */
    void _updateVolume() {
        if (_parent != null) _volumes[0] = (int) (_parent._volumes[0] * _thisVolume * _faderVolume);
        else _volumes[0] = (int) (_thisVolume * _faderVolume * 128);
    }

    /** */
    void _limitVolume() {
        if (_volumes[0] < 0) _volumes[0] = 0;
        else if (_volumes[0] > 128) _volumes[0] = 128;
    }

    /** */
    void _updatePan() {
        if (_parent != null) _pan = (_parent._pan + _thisPan) * 0.5;
        else _pan = _thisPan;
    }

    /** */
    void _limitPan() {
        if (_pan < -1) _pan = -1;
        else if (_pan > 1) _pan = 1;
    }

    private final org.si.utils.EventListener _onStreamListener = this::_onStream;

    /** Handler for SiONEvent.STREAM */
    protected void _onStream(Object e_) {
        // SiONEvent e = (SiONEvent) e_;
        if (_fader.execute()) {
            getDriver().removeEventListener(SiONEvent.STREAM, _onStreamListener);
            getDriver().forceDispatchStreamEvent(false);
        }
    }

    /** call from fader */
    protected void _fadeVolume(double v) {
        _faderVolume = v;
        _updateVolume();
        _updateStreamSend(0, _volumes[0] * 0.0078125);
    }

    /** on enter frame */
    protected void _onEnterFrame(Sequencer seq) {
        if (hasEventListener("soundObjectEnterFrame")) {
            SoundObjectEvent event = new SoundObjectEvent("soundObjectEnterFrame", this, null);
            event._note = _note;
            event._eventTriggerID = _eventTriggerID;
            dispatchEvent(event);
        }
    }

    /** on enter segment */
    protected void _onEnterSegment(Sequencer seq) {
        if (hasEventListener("soundObjectEnterSegment")) {
            SoundObjectEvent event = new SoundObjectEvent("soundObjectEnterSegment", this, null);
            event._eventTriggerID = _eventTriggerID;
            dispatchEvent(event);
        }
    }

    // errors
    //

    /** not available */
    protected static RuntimeException _errorNotAvailable(String str) {
        return new RuntimeException("SoundObject; " + str + " method instanceof not available in this object.");
    }

    /** Cannot change */
    protected static RuntimeException _errorCannotChange(String str) {
        return new RuntimeException("SoundObject; You can not change " + str + " property in this object.");
    }
}
