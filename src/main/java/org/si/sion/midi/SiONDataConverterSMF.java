//
// MIDI sound module
//  Copyright (c) 2011 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.midi;

import org.si.sion.SiONData;
import org.si.sion.sequencer.base.MMLEvent;


/** Standard MIDI File converter */
public class SiONDataConverterSMF extends SiONData {

    // variables
    //

    /** use MIDI modules effector */
    public boolean useMIDIModuleEffector = true;

    private SMFData _smfData;
    private MIDIModule _module;
    private final MMLEvent _waitEvent;
    private SMFExecutor[] _executors = null;
    private double _resolutionRatio = 1;
    private double _mmlTickError = 0;
    private double _midiTickError = 0;

    // properties
    //

    /** Standard MIDI file data to play */
    public SMFData getSmfData() {
        return _smfData;
    }

    public void setSmfData(SMFData data) {
        _smfData = data;
        if (_smfData != null) {
            setBpm(_smfData.bpm);
            _resolutionRatio = 1920. / _smfData.resolution;
        } else {
            setBpm(120);
            _resolutionRatio = 1;
        }
    }

    /** MIDI sound module object to play */
    public MIDIModule getMidiModule() {
        return _module;
    }

    public void setMidiModule(MIDIModule module) {
        _module = module;
    }

    // constructor
    //

    /** Pass SMFData and MIDIModule */
    public SiONDataConverterSMF(SMFData smfData, MIDIModule midiModule) {
        super();
        _smfData = smfData;
        _module = midiModule;

        if (_smfData != null) {
            setBpm(_smfData.bpm);
            _resolutionRatio = 1920. / _smfData.resolution;
        } else {
            setBpm(120);
            _resolutionRatio = 1;
        }

        globalSequence.initialize();
        globalSequence.appendNewCallback(this::_onMIDIInitialize, 0);
        globalSequence.appendNewEvent(MMLEvent.REPEAT_ALL, 0, 0);
        globalSequence.appendNewCallback(this::_onMIDIEventCallback, 0);
        _waitEvent = globalSequence.appendNewEvent(MMLEvent.GLOBAL_WAIT, 0, 0);
    }

    // operations
    //
    private MMLEvent _onMIDIInitialize(Object data) {
        int i, imax;

        // initialize module
        _module._initialize(useMIDIModuleEffector);

        // initialize executors
        _executors = new SMFExecutor[imax = _smfData.tracks.size()];
        for (i = 0; i < imax; i++) {
            if (_executors[i] == null) _executors[i] = new SMFExecutor();
            _executors[i]._initialize(_smfData.tracks.get(i), _module);
        }

        // initialize interval
        _waitEvent.length = 0;
        _mmlTickError = 0;
        _midiTickError = 0;

        return null;
    }

    private MMLEvent _onMIDIEventCallback(Object data) {
        int i, imax = _executors.length;
        int ticks, deltaTime, minDeltaTime;
        boolean allFinished = true;

        double midiTicks = (_waitEvent.length / _resolutionRatio) + _midiTickError;
        ticks = (int) midiTicks;
        _midiTickError = midiTicks - ticks;

        minDeltaTime = Integer.MAX_VALUE;
        for (i = 0; i < imax; i++) {
            deltaTime = _executors[i]._execute(ticks);
            if (deltaTime != SMFExecutor.END_OF_TRACK) {
                allFinished = false;
                if (minDeltaTime > deltaTime) minDeltaTime = deltaTime;
            }
        }

        if (allFinished) {
            _module._onFinishSequence();
            _waitEvent.length = 0;
            return null;
        }

        double mmlTicks = (minDeltaTime * _resolutionRatio) + _mmlTickError;
        _waitEvent.length = (int) mmlTicks;
        _mmlTickError = mmlTicks - _waitEvent.length;
        if (_waitEvent.length == 0 && minDeltaTime > 0) {
            _waitEvent.length = 1;
            _mmlTickError = 0;
        }
        return null;
    }
}
