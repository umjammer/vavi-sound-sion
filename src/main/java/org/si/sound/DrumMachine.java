//
// Class for play drum tracks
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.List;

import org.si.sion.SiONData;
import org.si.sion.SiONVoice;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sound.patterns.DrumMachinePresetPattern;
import org.si.sound.patterns.Note;
import org.si.sound.patterns.Sequencer;
import org.si.sound.synthesizers.DrumMachinePresetVoice;


/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_FRAME */
// [Event(name="enterFrame",   type="org.si.sound.events.SoundObjectEvent")]
/** @eventType org.si.sound.events.SoundObjectEvent.ENTER_SEGMENT */
// [Event(name="enterSegment", type="org.si.sound.events.SoundObjectEvent")]

/** Drum machinie provides independent bass drum, snare drum and hihat symbals tracks. */
public class DrumMachine extends MultiTrackSoundObject {

    // static variables
    //
    private DrumMachinePresetVoice _presetVoice = null;
    private DrumMachinePresetPattern _presetPattern = null;

    // variables
    //

    /** bass drum pattern sequencer */
    protected Sequencer _bass;
    /** snare drum pattern sequencer */
    protected Sequencer _snare;
    /** hi-hat cymbal pattern sequencer */
    protected Sequencer _hihat;

    /** Sequence data */
    protected SiONData _data;

    /** bass drum pattern number */
    protected int _bassPatternNumber;
    /** snare drum pattern number */
    protected int _snarePatternNumber;
    /** hi-hat cymbal pattern number */
    protected int _hihatPatternNumber;
    /** bass drum voice number */
    protected int _bassVoiceNumber;
    /** snare drum voice number */
    protected int _snareVoiceNumber;
    /** hi-hat cymbal voice number */
    protected int _hihatVoiceNumber;
    /** Change bass line pattern at the head of segment. */
    protected boolean _changePatternOnSegment;

    // preset pattern list
    private List<List<Note>> bassPatternList;
    private List<List<Note>> snarePatternList;
    private List<List<Note>> hihatPatternList;
    private List<List<Note>> percusPatternList;
    private List<SiONVoice> bassVoiceList;
    private List<SiONVoice> snareVoiceList;
    private List<SiONVoice> hihatVoiceList;
    private List<SiONVoice> percusVoiceList;

    // properties
    //

    /** Preset voices */
    public DrumMachinePresetVoice getPresetVoice() {
        return _presetVoice;
    }

    /** Preset patterns */
    public DrumMachinePresetPattern getPresetPattern() {
        return _presetPattern;
    }

    /** maximum value of basePatternNumber */
    public int getBassPatternNumberMax() {
        return bassPatternList.size();
    }

    /** maximum value of snarePatternNumber */
    public int getSnarePatternNumberMax() {
        return snarePatternList.size();
    }

    /** maximum value of hihatPatternNumber */
    public int getHihatPatternNumberMax() {
        return hihatPatternList.size();
    }

    /** maximum value of baseVoiceNumber */
    public int getBassVoiceNumberMax() {
        return bassVoiceList.size() >> 1;
    }

    /** maximum value of snareVoiceNumber */
    public int getSnareVoiceNumberMax() {
        return snareVoiceList.size() >> 1;
    }

    /** maximum value of hihatVoiceNumber */
    public int getHihatVoiceNumberMax() {
        return hihatVoiceList.size() >> 1;
    }


    /** Sequencer object of bass drum */
    public Sequencer getBass() {
        return _bass;
    }

    /** Sequencer object of snare drum */
    public Sequencer getSnare() {
        return _snare;
    }

    /** Sequencer object of hihat symbal */
    public Sequencer getHihat() {
        return _hihat;
    }

    /** Sequence pattern of bass drum */
    public List<Note> getBassPattern() {
        return _bass.pattern != null ? _bass.pattern : _bass.nextPattern;
    }

    public void setBassPattern(List<Note> pat) {
        if (isPlaying() && _changePatternOnSegment) _bass.nextPattern = pat;
        else _bass.pattern = pat;
    }

    /** Sequence pattern of snare drum */
    public List<Note> getSnarePattern() {
        return _snare.pattern != null ? _snare.pattern : _snare.nextPattern;
    }

    public void setSnarePattern(List<Note> pat) {
        if (isPlaying() && _changePatternOnSegment) _snare.nextPattern = pat;
        else _snare.pattern = pat;
    }

    /** Sequence pattern of hihat symbal */
    public List<Note> getHihatPattern() {
        return _hihat.pattern != null ? _hihat.pattern : _hihat.nextPattern;
    }

    public void setHihatPattern(List<Note> pat) {
        if (isPlaying() && _changePatternOnSegment) _hihat.nextPattern = pat;
        else _hihat.pattern = pat;
    }

    /** bass drum pattern number. */
    public int getBassPatternNumber() {
        return _bassPatternNumber;
    }

    public void setBassPatternNumber(int index) {
        if (index < 0 || index >= bassPatternList.size()) return;
        _bassPatternNumber = index;
        setBassPattern(bassPatternList.get(index));
    }


    /** snare drum pattern number. */
    public int getSnarePatternNumber() {
        return _snarePatternNumber;
    }

    public void setSnarePatternNumber(int index) {
        if (index < 0 || index >= snarePatternList.size()) return;
        _snarePatternNumber = index;
        if (_changePatternOnSegment) _snare.nextPattern = snarePatternList.get(index);
        else _snare.pattern = snarePatternList.get(index);
    }


    /** hi-hat cymbal pattern number. */
    public int getHihatPatternNumber() {
        return _hihatPatternNumber;
    }

    public void setHihatPatternNumber(int index) {
        if (index < 0 || index >= hihatPatternList.size()) return;
        _hihatPatternNumber = index;
        if (_changePatternOnSegment) _hihat.nextPattern = hihatPatternList.get(index);
        else _hihat.pattern = hihatPatternList.get(index);
    }


    /** bass drum pattern number. */
    public int getBassVoiceNumber() {
        return _bassVoiceNumber >> 1;
    }

    public void setBassVoiceNumber(int index) {
        index <<= 1;
        if (index < 0 || index >= bassVoiceList.size()) return;
        _bassVoiceNumber = index;
        _bass.voiceList = List.of(bassVoiceList.get(index), bassVoiceList.get(index + 1));
    }


    /** snare drum pattern number. */
    public int getSnareVoiceNumber() {
        return _snareVoiceNumber >> 1;
    }

    public void setSnareVoiceNumber(int index) {
        index <<= 1;
        if (index < 0 || index >= snareVoiceList.size()) return;
        _snareVoiceNumber = index;
        _snare.voiceList = List.of(snareVoiceList.get(index), snareVoiceList.get(index + 1));
    }


    /** hi-hat cymbal pattern number. */
    public int getHihatVoiceNumber() {
        return _hihatVoiceNumber >> 1;
    }

    public void setHihatVoiceNumber(int index) {
        index <<= 1;
        if (index < 0 || index >= hihatVoiceList.size()) return;
        _hihatVoiceNumber = index;
        _hihat.voiceList = List.of(hihatVoiceList.get(index), hihatVoiceList.get(index + 1));
    }

    /** bass drum volume (0-1) */
    public double getBassVolume() {
        return _bass.getDefaultVelocity() * 0.00392156862745098;
    }

    public void setBassVolume(double n) {
        if (n < 0) n = 0;
        else if (n > 1) n = 1;
        _bass.setDefaultVelocity((int) (n * 255));
    }

    /** snare drum volume (0-1) */
    public double getSnareVolume() {
        return _snare.getDefaultVelocity() * 0.00392156862745098;
    }

    public void setSnareVolume(double n) {
        if (n < 0) n = 0;
        else if (n > 1) n = 1;
        _snare.setDefaultVelocity((int) (n * 255));
    }

    /** hihat symbal volume (0-1) */
    public double getHihatVolume() {
        return _hihat.getDefaultVelocity() * 0.00392156862745098;
    }

    public void setHihatVolume(double n) {
        if (n < 0) n = 0;
        else if (n > 1) n = 1;
        _hihat.setDefaultVelocity((int) (n * 255));
    }

    /** True to change bass line pattern at the head of segment. @default true */
    public boolean getChangePatternOnNextSegment() {
        return _changePatternOnSegment;
    }

    public void setChangePatternOnNextSegment(boolean b) {
        _changePatternOnSegment = b;
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param bassPatternNumber  bass drum pattern number
     * @param snarePatternNumber snare drum pattern number
     * @param hihatPatternNumber hihat symbal pattern number
     * @param bassVoiceNumber    bass drum voice number
     * @param snareVoiceNumber   snare drum voice number
     * @param hihatVoiceNumber   hihat symbal voice number
     */
    public DrumMachine(int bassPatternNumber, int snarePatternNumber, int hihatPatternNumber, int bassVoiceNumber, int snareVoiceNumber, int hihatVoiceNumber) {
        super("DrumMachine", null);

        if (_presetVoice == null) {
            _presetVoice = new DrumMachinePresetVoice();
            _presetPattern = new DrumMachinePresetPattern();
            bassPatternList = _presetPattern.get("bass");
            snarePatternList = _presetPattern.get("snare");
            hihatPatternList = _presetPattern.get("hihat");
            percusPatternList = _presetPattern.get("percus");
            bassVoiceList = _presetVoice.get("bass");
            snareVoiceList = _presetVoice.get("snare");
            hihatVoiceList = _presetVoice.get("hihat");
            percusVoiceList = _presetVoice.get("percus");
        }

        _data = new SiONData();
        _bass = new Sequencer(this, _data, 36, 255, 1, 0, null);
        _snare = new Sequencer(this, _data, 68, 160, 1, 0, null);
        _hihat = new Sequencer(this, _data, 68, 128, 1, 0, null);
        this._bassVoiceNumber = bassVoiceNumber;
        this._snareVoiceNumber = snareVoiceNumber;
        this._hihatVoiceNumber = hihatVoiceNumber;
        _changePatternOnSegment = true;

        setPatternNumbers(bassPatternNumber, snarePatternNumber, hihatPatternNumber);
    }

    // operation
    //

    /** play drum sequence */
    @Override
    public void play() {
        int tn;
        Sequencer seq;

        stop();
        _tracks = _sequenceOn(_data, false, false);
        if (_tracks != null && _tracks.size() == 3) {
            _synthesizer._registerTracks(_tracks);
            _bass.play(_tracks.get(0));
            _snare.play(_tracks.get(1));
            _hihat.play(_tracks.get(2));
            if (_tracks.get(0).getTrackNumber() < _tracks.get(1).getTrackNumber()) {
                tn = (_tracks.get(0).getTrackNumber() < _tracks.get(2).getTrackNumber()) ? 0 : 2;
            } else {
                tn = (_tracks.get(1).getTrackNumber() < _tracks.get(2).getTrackNumber()) ? 1 : 2;
            }
            seq = switch (tn) {
                case 0 -> _bass;
                case 1 -> _snare;
                default -> _hihat;
            };
            seq.onEnterFrame = this::_onEnterFrame;
            seq.onEnterSegment = this::_onEnterSegment;
        } else {
            throw new Error("unknown error");
        }
    }

    /** stop sequence */
    @Override
    public void stop() {
        if (_tracks != null) {
            _bass.stop();
            _snare.stop();
            _hihat.stop();
            _synthesizer._unregisterTracks(_tracks.get(0), _tracks.size());
            for (SiMMLTrack t : _tracks) t.setDisposable();
            _tracks = null;
            _sequenceOff(false);
            _bass.onEnterFrame = null;
            _snare.onEnterFrame = null;
            _hihat.onEnterFrame = null;
            _bass.onEnterSegment = null;
            _snare.onEnterSegment = null;
            _hihat.onEnterSegment = null;
        }
        _stopEffect();
    }

    // configure
    //

    /**
     * Set all pattern indices
     *
     * @param bassPatternNumber  bass drum pattern index
     * @param snarePatternNumber snare drum pattern index
     * @param hihatPatternNumber hihat symbal pattern index
     */
    public DrumMachine setPatternNumbers(int bassPatternNumber, int snarePatternNumber, int hihatPatternNumber) {
        this._bassPatternNumber = bassPatternNumber;
        this._snarePatternNumber = snarePatternNumber;
        this._hihatPatternNumber = hihatPatternNumber;
        return this;
    }
}
