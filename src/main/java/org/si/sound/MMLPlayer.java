//
// Class for sound object playing MML
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.SiONData;
import org.si.sion.sequencer.SiMMLTrack;


/** MML Player provides sequence sound written by MML, and you can control all tracks during playing sequence. */
public class MMLPlayer extends SoundObject {

    // variables
    //

    /** mml text. */
    protected String _mml;

    /** sequence data. */
    protected SiONData _data;

    /** flag that mml text instanceof compiled to data */
    protected boolean _compiled;

    /** current controlling track number */
    protected int _controlTrackNumber;

    /** track muting status */
    protected List<Boolean> _trackMute;

    /** solo track number */
    protected int _soloTrackNumber;

    // properties
    //

    /** MML text */
    public String getMml() {
        return _mml;
    }

    public void setMml(String str) {
        _mml = str != null ? str : "";
        _compiled = false;
        _compile();
    }

    /** sequence data to play */
    public SiONData getData() {
        return _data;
    }

    /** current controling track number */
    public int getControlTrackNumber() {
        return _controlTrackNumber;
    }

    public void setControlTrackNumber(int n) {
        _controlTrackNumber = n;
        if (_tracks != null) {
            int trackNumber = _controlTrackNumber;
            if (trackNumber < 0) trackNumber = 0;
            else if (trackNumber >= _tracks.size()) trackNumber = _tracks.size() - 1;
            _track = _tracks.get(trackNumber);
        }
    }

    /** number of MML playing tracks */
    public int getTrackCount() {
        return (_tracks != null) ? _tracks.size() : 0;
    }

    /** Solo track number, this value reset when call start() method. -1 sets no solo tracks. @default -1 */
    public int getSoloTrackNumber() {
        return _soloTrackNumber;
    }

    public void setSoloTrackNumber(int n) {
        int i;
        if (_soloTrackNumber != n && _tracks != null) {
            _soloTrackNumber = n;
            if (_soloTrackNumber < 0) {
                for (i = 0; i < _tracks.size(); i++) _track.channel.setMute(_trackMute.get(i));
            } else {
                for (i = 0; i < _tracks.size(); i++) {
                    _trackMute.set(i, _track.channel.getMute());
                    _track.channel.setMute(i != _soloTrackNumber);
                }
            }
        }
    }

    @Override
    public int getCoarseTune() {
        return (_track != null) ? _track.noteShift : _noteShift;
    }

    @Override
    public double getFineTune() {
        return (_track != null) ? (_track.pitchShift * 0.015625) : _pitchShift;
    }

    @Override
    public double getGateTime() {
        return (_track != null) ? _track.quantRatio : _gateTime;
    }

    @Override
    public int getEventMask() {
        return (_track != null) ? _track.eventMask : (int) _eventMask;
    }

    @Override
    public boolean getMute() {
        return (_track != null) ? _track.channel.getMute() : _thisMute;
    }

    @Override
    public double getVolume() {
        return (_track != null) ? _track.channel.getMasterVolume() : _thisVolume;
    }

    @Override
    public double getPan() {
        return (_track != null) ? _track.channel.getPan() : _thisPan;
    }

    @Override
    public double getEffectSend1() {
        return (_track != null) ? _track.channel.getStreamSend(1) : (_volumes[1] * 0.0078125);
    }

    @Override
    public double getEffectSend2() {
        return (_track != null) ? _track.channel.getStreamSend(2) : (_volumes[2] * 0.0078125);
    }

    @Override
    public double getEffectSend3() {
        return (_track != null) ? _track.channel.getStreamSend(3) : (_volumes[3] * 0.0078125);
    }

    @Override
    public double getEffectSend4() {
        return (_track != null) ? _track.channel.getStreamSend(4) : (_volumes[4] * 0.0078125);
    }

    @Override
    public double getPitchBend() {
        return (_track != null) ? (_track.getPitchBend() * 0.015625) : _pitchBend;
    }

    // constructor
    //

    /** constructor */
    public MMLPlayer(String mml) {
        super("", null);
        _data = new SiONData();
        this._mml = mml;
        _controlTrackNumber = 0;
        _trackMute = new ArrayList<Boolean>();
    }

    // operations
    //

    /** Play mml data. */
    @Override
    public void play() {
        _compile();
        stop();
        _soloTrackNumber = -1;
        _tracks = _sequenceOn(_data, false, true);
        if (_tracks != null) {
            _trackMute.clear();
            for (int i = 0; i < _tracks.size(); i++) _trackMute.add(false);
            _synthesizer._registerTracks(_tracks);
            int trackNumber = _controlTrackNumber;
            if (trackNumber < 0) trackNumber = 0;
            else if (trackNumber >= _tracks.size()) trackNumber = _tracks.size() - 1;
            _track = _tracks.get(trackNumber);
        }
    }

    /** Stop mml data. */
    @Override
    public void stop() {
        if (_tracks != null) {
            _synthesizer._unregisterTracks(_tracks.get(0), _tracks.size());
            for (SiMMLTrack t : _tracks) t.setDisposable();
            _tracks = null;
            _sequenceOff(false);
        }
        _stopEffect();
    }

    // internal
    //

    /** call this after the update mml */
    protected void _compile() {
        if (getDriver() == null || _compiled) return;
        if (!_mml.isEmpty()) {
            getDriver().compile(_mml, _data);
            name = _data.title;
        } else {
            _data.clear();
            name = "";
        }
        _compiled = true;
    }
}
