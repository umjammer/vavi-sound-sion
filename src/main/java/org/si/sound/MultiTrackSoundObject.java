//
// Multi track Sound object
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//


package org.si.sound;

import org.si.sion.sequencer.SiMMLTrack;
import org.si.sound.synthesizers.VoiceReference;


/**
 * The MultiTrackSoundObject class instanceof the base class for all objects that can control plural tracks.
 */
public class MultiTrackSoundObject extends SoundObject {

    // variables
    //

    /** mask for tracks operation. */
    protected int _trackOperationMask;

    // properties
    //

    /** Returns the number of tracks. */
    public int getTrackCount() {
        return (_tracks != null) ? _tracks.size() : 0;
    }

    // properties
    //

    @Override
    public boolean isPlaying() {
        return (_tracks != null);
    }


    @Override
    public void setCoarseTune(int n) {
        super.setCoarseTune(n);
        if (_tracks != null) {
            int i, f, imax = _tracks.size();
            for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                if ((f & 1) == 0) _tracks.get(i).noteShift = _noteShift;
            }
        }
    }

    @Override
    public void setFineTune(double p) {
        super.setFineTune(p);
        if (_tracks != null) {
            int i, f, imax = _tracks.size(), ps = (int) (_pitchShift * 64);
            for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                if ((f & 1) == 0) _tracks.get(i).pitchShift = ps;
            }
        }
    }

    @Override
    public void setGateTime(double g) {
        super.setGateTime(g);
        if (_tracks != null) {
            int i, f, imax = _tracks.size();
            for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                if ((f & 1) == 0) _tracks.get(i).quantRatio = _gateTime;
            }
        }
    }

    @Override
    public void setEventMask(int m) {
        super.setEventMask(m);
        if (_tracks != null) {
            int i, f, imax = _tracks.size();
            for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                if ((f & 1) == 0) _tracks.get(i).eventMask = (int) _eventMask;
            }
        }
    }

    @Override
    public void setMute(boolean m) {
        super.setMute(m);
        if (_tracks != null) {
            int i, f, imax = _tracks.size();
            for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                if ((f & 1) == 0) _tracks.get(i).channel.setMute(_mute);
            }
        }
    }

    @Override
    public void setPan(double p) {
        super.setPan(p);
        if (_tracks != null) {
            int i, f, imax = _tracks.size();
            for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                if ((f & 1) == 0) _tracks.get(i).channel.setPan((int) (_pan * 64));
            }
        }
    }

    @Override
    public void setPitchBend(double p) {
        super.setPitchBend(p);
        if (_tracks != null) {
            int i, f, pb = (int) (p * 64), imax = _tracks.size();
            for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                if ((f & 1) == 0) _tracks.get(i).setPitchBend(pb);
            }
        }
    }

    // constructor
    //

    /** constructor */
    MultiTrackSoundObject(String name, VoiceReference synth /* = null */) {
        super(name, synth);
        _tracks = null;
        _trackOperationMask = 0;
    }

    // operations
    //

    /** Reset */
    @Override
    public void reset() {
        super.reset();
        _trackOperationMask = 0;
    }

    /** you cannot call play() in MultiTrackSoundObject. */
    @Override
    public void play() {
        throw _errorNotAvailable("play()");
    }


    /** you cannot call stop() in MultiTrackSoundObject. */
    @Override
    public void stop() {
        throw _errorNotAvailable("stop()");
    }


    /** Stop all sound belonging to this sound object. */
    protected void _stopAllTracks() {
        if (_tracks != null) {
            for (SiMMLTrack t : _tracks) {
                _synthesizer._unregisterTracks(t, 1);
                t.setDisposable();
            }
            _tracks = null;
        }
        _stopEffect();
    }

    /** update stream send level */
    @Override
    protected void _updateStreamSend(int streamNum, double level) {
        if (_tracks != null) {
            if (_effectChain != null) _effectChain.setStreamSend(streamNum, level);
            else {
                int i, f, imax = _tracks.size();
                for (i = 0, f = _trackOperationMask; i < imax; i++, f >>= 1) {
                    if ((f & 1) == 0) _tracks.get(i).channel.setStreamSend(streamNum, level);
                }
            }
        }
    }
}
