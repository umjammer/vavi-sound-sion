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

        return null;
    }

    private MMLEvent _onMIDIEventCallback(Object data) {
        int i, imax = _executors.length;
        SMFExecutor exec;
        int seq, ticks, deltaTime, minDeltaTime;
        ticks = (int) (_waitEvent.length / _resolutionRatio);
        minDeltaTime = _executors[0]._execute(ticks);
        for (i = 1; i < imax; i++) {
            deltaTime = _executors[i]._execute(ticks);
            if (minDeltaTime > deltaTime) minDeltaTime = deltaTime;
        }
        if (minDeltaTime == 65536) _module._onFinishSequence();
        _waitEvent.length = (int) (minDeltaTime * _resolutionRatio);
        return null;
    }
}
