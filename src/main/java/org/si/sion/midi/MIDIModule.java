//
// MIDI sound module
//  Copyright (c) 2011 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.midi;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.effector.SiEffectAutoPan.TriConsumer;
import org.si.sion.effector.SiEffectBase;
import org.si.sion.effector.SiEffectStereoChorus;
import org.si.sion.effector.SiEffectStereoDelay;
import org.si.sion.effector.SiEffectStereoReverb;
import org.si.sion.events.SiONMIDIEvent;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.module.channels.SiOPMChannelBase;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.utils.SiONPresetVoice;
import org.si.sion.utils.soundfont.SiONSoundFont;
import org.si.utils.ByteArray;


/** MIDI sound module */
public class MIDIModule {

    // constant
    //

    /** General MIDI mode */
    public final String GM_MODE = "GMmode";
    /** Roland GS system exclusive mode */
    public final String GS_MODE = "GSmode";
    /** YAMAHA XG system exclusive mode */
    public final String XG_MODE = "XGmode";

    // variables
    //

    /** voice set for GM 128 voices. */
    public SiONVoice[] voiceSet;
    /** voice set for drum track. */
    public SiONVoice[] drumVoiceSet;
    /** MIDI channels */
    public MIDIModuleChannel[] midiChannels;

    /** NRPN callback, should be function(channelNum, nrpn, dataEntry) : void. */
    public TriConsumer<Integer, Integer, Integer> onNRPN = null;
    /** System exclusive callback, should be function(channelNum, bytes:ByteArray) : void. */
    public BiConsumer<Integer, ByteArray> onSysEx = null;
    /** Finish sequence callback, should be function() : void. */
    public Runnable onFinishSequence = null;

    // core 
    private SiONDriver _sionDriver = null;
    private int _polyphony;
    // operators 
    private final MIDIModuleOperator _freeOperators;
    private final MIDIModuleOperator _activeOperators;
    // drum track related 
    private final int[] _drumExclusiveGroupID;
    private final MIDIModuleOperator[] _drumExclusiveOperator;
    private final int[] _drumNoteOffAvailable;
    // effector related 
    private final SiEffectBase[][] _effectorSet;
    // MIDI event related 
    private int _dataEntry;
    private int _rpnNumber;
    private boolean _isNRPN;
    private int _portOffset;
    private int _portNumber;
    private String _systemExclusiveMode;
    // others 
    private int _dispatchFlags = 0;
    private SiONPresetVoice _internalPreset = null;
    private double _drumVelocityBoost = 1.0;

    // properties
    //

    /** polyphony */
    public int getPolyphony() {
        return _polyphony;
    }

    public void setPolyphony(int poly) {
        _polyphony = poly;
    }

    /** MIDI channel count, port number instanceof reset when channel count instanceof changed. */
    public int getMidiChannelCount() {
        return midiChannels.length;
    }

    public void setMidiChannelCount(int count) {
        midiChannels = new MIDIModuleChannel[count];
        for (int ch = 0; ch < count; ch++) {
            if (midiChannels[ch] == null) midiChannels[ch] = new MIDIModuleChannel();
            midiChannels[ch].eventTriggerID = ch;
        }
        _portOffset = 0;
    }

    /** free operator count */
    public int getFreeOperatorCount() {
        return _freeOperators.length;
    }

    /** active operator count */
    public int getActiveOperatorCount() {
        return _activeOperators.length;
    }

    /** port number */
    public int getPortNumber() {
        return _portNumber;
    }

    public void setPortNumber(int portNum) {
        _portNumber = portNum;
        if (midiChannels.length > (portNum << 4) + 15) _portOffset = portNum << 4;
        else _portOffset = (midiChannels.length - 15) >> 4;
    }

    /** System exclusive mode */
    public String getSystemExclusiveMode() {
        return _systemExclusiveMode;
    }

    public void setSystemExclusiveMode(String mode) {
        _systemExclusiveMode = mode;
    }

    /** drum velocity boost for drum mode channels */
    public double getDrumVelocityBoost() {
        return _drumVelocityBoost;
    }

    public void setDrumVelocityBoost(double boost) {
        _drumVelocityBoost = (boost < 0) ? 0 : boost;
    }

    // constructor
    //

    /**
     * MIDI sound module emulator
     *
     * @param polyphony        polyphony
     * @param midiChannelCount MIDI channel count
     */
    public MIDIModule(int polyphony, int midiChannelCount, String systemExclusiveMode) {
        int slot, i;

        // allocation
        _systemExclusiveMode = systemExclusiveMode;
        _polyphony = polyphony;
        _freeOperators = new MIDIModuleOperator(null);
        _activeOperators = new MIDIModuleOperator(null);

        voiceSet = new SiONVoice[128];
        drumVoiceSet = new SiONVoice[128];
        _drumExclusiveGroupID = new int[128];
        _drumExclusiveOperator = new MIDIModuleOperator[16];
        _drumNoteOffAvailable = new int[128];
        _effectorSet = new SiEffectBase[8][];
        for (slot = 0; slot < 8; slot++) _effectorSet[slot] = null;

        // initialize
        _effectorSet[1] = new SiEffectBase[] {new SiEffectStereoReverb(0.7, 0.4, 0.8, 1)};
        _effectorSet[2] = new SiEffectBase[] {new SiEffectStereoChorus(20, 0.1, 4, 20, 1, true)};
        _effectorSet[3] = new SiEffectBase[] {new SiEffectStereoDelay(250, 0.25, false, 1)};
        setDrumExclusiveGroup(1, new int[] {42, 44, 46}); // hi-hat group
        setDrumExclusiveGroup(2, new int[] {80, 81});    // triangle group
        enableDrumNoteOff(new int[] {71, 72}, true);          // samba whistle

        // alloc channels
        setMidiChannelCount(midiChannelCount);

        // load preset voices
        _internalPreset = SiONPresetVoice.getMutex();
        if (_internalPreset == null || _internalPreset.get("svmidi") == null || _internalPreset.get("svmidi.drum") == null) {
            _internalPreset = new SiONPresetVoice(SiONPresetVoice.INCLUDE_WAVETABLE | SiONPresetVoice.INCLUDE_SINGLE_DRUM);
        }
        resetVoiceSet();
    }

    // operations
    //

    /** this function is called first of all sequences */
    public boolean _initialize(boolean useMIDIModuleEffector) {
        int i;
        MIDIModuleOperator ope;
        _sionDriver = SiONDriver.mutex();
        if (_sionDriver == null) return false;

        resetAllChannels();
        _freeOperators.clear();
        _activeOperators.clear();
        for (i = 0; i < _polyphony; i++) {
            _freeOperators.add(new MIDIModuleOperator(_sionDriver.newUserControlableTrack(i)));
        }
        for (i = 0; i < 16; i++) {
            _drumExclusiveOperator[i] = null;
        }

        _dataEntry = 0;
        _rpnNumber = 0;
        _isNRPN = false;
        _portOffset = 0;

        if (useMIDIModuleEffector) {
            for (i = 0; i < 8; i++) {
                if (_effectorSet[i] != null) _sionDriver.effector.setEffectorList(i, _effectorSet[i]);
            }
        }

        _dispatchFlags = _sionDriver._checkMIDIEventListeners();

        return true;
    }

    /**
     * Set drum voice by sampler table
     *
     * @param table sampler table class, ussualy get from SiONSoundFont
     * @see SiONSoundFont
     */
    public void setDrumSamplerTable(SiOPMWaveSamplerTable table) {
        SiONVoice voice = new SiONVoice();
        int i;
        voice.setSamplerTable(table);
        for (i = 0; i < 128; i++) drumVoiceSet[i] = voice;
    }

    /**
     * set exclusive drum voice. Voice that has same groupID stops each other when it sounds.
     *
     * @param groupID      0 means no group. 1-15 are available.
     * @param voiceNumbers list of voice number that have same groupID
     */
    public void setDrumExclusiveGroup(int groupID, int[] voiceNumbers) {
        for (int voiceNumber : voiceNumbers) _drumExclusiveGroupID[voiceNumber] = groupID;
    }

    /**
     * set default effector set.
     *
     * @param slot         slot to set
     * @param effectorList Array of inherit class of SiEffectBase
     */
    public void setDefaultEffector(int slot, SiEffectBase[] effectorList) {
        _effectorSet[slot] = effectorList;
    }

    /**
     * enable drum note off. default value instanceof false
     *
     * @param voiceNumbers list of voice number that enables note off
     */
    public void enableDrumNoteOff(int[] voiceNumbers, boolean enable /* = true */) {
        for (int voiceNumber : voiceNumbers) _drumNoteOffAvailable[voiceNumber] = (enable) ? 1 : 0;
    }

    /** reset all channels */
    public void resetAllChannels() {
        for (int ch = 0; ch < midiChannels.length; ch++) {
            midiChannels[ch].reset();
            if ((ch & 15) == 9) midiChannels[ch].drumMode = 1;
        }
    }

    /** reset voice set to default */
    public void resetVoiceSet() {
        for (int i = 0; i < 128; i++) {
            voiceSet[i] = ((SiONPresetVoice.SiONVoiceList) _internalPreset.get("svmidi")).get(i);
        }
        for (int i = 0; i < 60; i++) {
            if (!Boolean.parseBoolean(System.getProperty("org.si.sion.midi.gm", "false"))) {
                drumVoiceSet[i + 24] = ((SiONPresetVoice.SiONVoiceList) _internalPreset.get("svmidi.drum")).get(i);
            }
        }
    }

    /** note on */
    public void noteOn(int channelNum, int note, int velocity) {
        channelNum += _portOffset;
        MIDIModuleChannel midiChannel = midiChannels[channelNum];
        SiONVoice voice;
        MIDIModuleOperator ope;
        SiMMLTrack track = null;
        SiOPMChannelBase channel;
        int drumExcID = 0,
                sionTrackNote = note;

        if (!midiChannel.mute) {

            // get operator
            if (midiChannel.activeOperatorCount >= midiChannel.maxOperatorCount) {
                ope = null;
                for (ope = _activeOperators.next; ope != _activeOperators; ope = ope.next) {
                    if (ope.channel == channelNum) {
                        _activeOperators.remove(ope);
                        break;
                    }
                }
                if (ope == null || ope == _activeOperators) {
                    ope = _activeOperators.shift();
                }
            } else {
                ope = _freeOperators.shift();
                if (ope == null) ope = _activeOperators.shift();
            }

            if (ope == null) {
                return;
            }

            if (ope.isNoteOn) {
                ope.sionTrack.dispatchEventTrigger(false);
                midiChannels[ope.channel].activeOperatorCount--;
            }

            // voice setting
            if (midiChannel.drumMode == 0) {
                if (ope.programNumber != midiChannel.programNumber) {
                    ope.programNumber = midiChannel.programNumber;
                    voice = voiceSet[ope.programNumber];
                    if (voice != null) {
                        ope.sionTrack.quantRatio = 1;
                        voice.updateTrackVoice(ope.sionTrack);
                    } else {
                        _freeOperators.add(ope);
                        return;
                    }
                }
            } else {
                ope.programNumber = -1;
                voice = drumVoiceSet[note];
                if (voice != null) {
                    drumExcID = _drumExclusiveGroupID[note];
                    sionTrackNote = (voice.preferableNote == -1) ? 60 : voice.preferableNote;
                    if (drumExcID > 0) {
                        MIDIModuleOperator excOpe = _drumExclusiveOperator[drumExcID];
                        if (excOpe != null && excOpe.drumExcID == drumExcID) {
                            if (excOpe.isNoteOn) _noteOffOperator(excOpe);
                            excOpe.sionTrack.keyOff(0, true);
                        }
                        _drumExclusiveOperator[drumExcID] = ope;
                    }
                    ope.sionTrack.quantRatio = 1;
                    voice.updateTrackVoice(ope.sionTrack);
                } else {
                    _freeOperators.add(ope);
                    return;
                }
            }

            // operator settings
            track = ope.sionTrack;
            channel = track.channel;

            track.noteShift = midiChannel.masterCoarseTune;
            track.pitchShift = midiChannel.masterFineTune;
            track.setPitchBend((midiChannel.pitchBend * midiChannel.pitchBendSensitivity) >> 7); // (* 64 / 8192)
            track.setPortament(midiChannel.portamentoTime);
            track.setEventTrigger(midiChannel.eventTriggerID, midiChannel.eventTriggerTypeOn, midiChannel.eventTriggerTypeOff);
            int trackVelocity = (int) ((velocity * 1.5) + 64);
            double drumGain = 1;
            if (midiChannel.drumMode != 0) {
                trackVelocity = (int) (trackVelocity * _drumVelocityBoost);
                drumGain = _drumVelocityBoost;
            }
            track.setVelocity((trackVelocity < 0) ? 0 : Math.min(trackVelocity, 512));
            channel.setAllStreamSendLevels(midiChannel._sionVolumes);
            if (drumGain != 1) {
                for (int streamNum = 0; streamNum < midiChannel._sionVolumes.length; streamNum++) {
                    channel.setStreamSend(streamNum, channel.getStreamSend(streamNum) * drumGain);
                }
            }
            channel.setPan(midiChannel.pan);
            channel.setLFOCycleTime(midiChannel.modulationCycleTime);
            channel.setPitchModulation(midiChannel.modulation >> 2);            // width = 32
            channel.setAmplitudeModulation(midiChannel.channelAfterTouch >> 2); // width = 32
            track.keyOn(sionTrackNote, 0, 0);

            ope.isNoteOn = true;
            ope.note = note;
            ope.channel = channelNum;
            ope.drumExcID = drumExcID;
            _activeOperators.add(ope);
            midiChannel.activeOperatorCount++;
        } // if (!midiChannel.mute)

        if ((_dispatchFlags & midiChannel.sionMIDIEventType & SiONMIDIEventFlag.NOTE_ON) != 0) {
            _sionDriver._dispatchMIDIEvent (SiONMIDIEvent.NOTE_ON, track, channelNum, note, velocity);
        }
    }

    /** note off */
    public void noteOff(int channelNum, int note, int velocity) {
        channelNum += _portOffset;

        MIDIModuleOperator ope;
        int i = 0;
        for (ope = _activeOperators.next; ope != _activeOperators; ope = ope.next) {
            if (ope.note == note && ope.channel == channelNum && ope.isNoteOn) {
                _noteOffOperator(ope);
                return;
            }
        }
    }

    private void _noteOffOperator(MIDIModuleOperator ope) {
        int channelNum = ope.channel, note = ope.note;
        MIDIModuleChannel midiChannel = midiChannels[channelNum];
        if (!midiChannel.mute) {
            if (midiChannel.sustainPedal) ope.sionTrack.dispatchEventTrigger(false);
            else if (midiChannel.drumMode == 0 || _drumNoteOffAvailable[note] != 0) ope.sionTrack.keyOff(0, false);
            ope.isNoteOn = false;
            ope.note = -1;
            ope.channel = -1;
            midiChannel.activeOperatorCount--;
            _activeOperators.remove(ope);
            _freeOperators.add(ope);
        }

        if ((_dispatchFlags & midiChannel.sionMIDIEventType & SiONMIDIEventFlag.NOTE_OFF) != 0) {
            _sionDriver._dispatchMIDIEvent (SiONMIDIEvent.NOTE_OFF, ope.sionTrack, channelNum, note, 0);
        }
    }

    /** program change */
    public void programChange(int channelNum, int programNumber) {
        channelNum += _portOffset;
        MIDIModuleChannel midiChannel = midiChannels[channelNum];
        midiChannel.programNumber = programNumber;

        if ((_dispatchFlags & midiChannel.sionMIDIEventType & SiONMIDIEventFlag.PROGRAM_CHANGE) != 0) {
            _sionDriver._dispatchMIDIEvent
            (SiONMIDIEvent.PROGRAM_CHANGE, null, channelNum, 0, programNumber);
        }
    }

    /** channel after touch */
    public void channelAfterTouch(int channelNum, int value) {
        channelNum += _portOffset;
        MIDIModuleChannel midiChannel = midiChannels[channelNum];
        midiChannel.channelAfterTouch = value;

        for (MIDIModuleOperator ope = _activeOperators.next; ope != _activeOperators; ope = ope.next) {
            if (ope.channel == channelNum) {
                ope.sionTrack.channel.setAmplitudeModulation(midiChannel.channelAfterTouch >> 2);
            }
        }
    }

    /** pitch bned */
    public void pitchBend(int channelNum, int bend) {
        channelNum += _portOffset;
        MIDIModuleChannel midiChannel = midiChannels[channelNum];
        midiChannel.pitchBend = bend;

        for (MIDIModuleOperator ope = _activeOperators.next; ope != _activeOperators; ope = ope.next) {
            if (ope.channel == channelNum) {
                ope.sionTrack.setPitchBend((midiChannel.pitchBend * midiChannel.pitchBendSensitivity) >> 7); // (* 64 / 8192)
            }
        }
        if ((_dispatchFlags & midiChannel.sionMIDIEventType & SiONMIDIEventFlag.PITCH_BEND) != 0) {
            _sionDriver._dispatchMIDIEvent (SiONMIDIEvent.PITCH_BEND, null, channelNum, 0, bend);
        }
    }

    /** control change */
    public void controlChange(int channelNum, int controlerNumber, int data) {
        channelNum += _portOffset;
        MIDIModuleChannel midiChannel = midiChannels[channelNum];

        switch (controlerNumber) {
            case SMFEvent.CC_BANK_SELECT_MSB:
                midiChannel.bankNumber = (data & 0x7f) << 7;
                // XG USE_FOR_RYTHM_PART support
                if (_systemExclusiveMode.equals(XG_MODE)) {
                    if ((data & 0x7f) == 127) midiChannel.drumMode = 1;
                    else if (channelNum != 9) midiChannel.drumMode = 0;
                }
                break;
            case SMFEvent.CC_BANK_SELECT_LSB:
                midiChannel.bankNumber |= data & 0x7f;
                break;

            case SMFEvent.CC_MODULATION:
                midiChannel.modulation = data;
                $(ope -> ope.sionTrack.channel.setPitchModulation(midiChannel.modulation >> 2), channelNum);
            break;
            case SMFEvent.CC_PORTAMENTO_TIME:
                midiChannel.portamentoTime = data;
                $(ope -> ope.sionTrack.setPortament(midiChannel.portamentoTime), channelNum);
            break;

            case SMFEvent.CC_VOLUME:
                midiChannel.setMasterVolume(data);
                $(ope -> ope.sionTrack.channel.setAllStreamSendLevels(midiChannel._sionVolumes), channelNum);
            break;
            //case SMFEvent.CC_BALANCE:
            case SMFEvent.CC_PANPOD:
                midiChannel.pan = data - 64;
                $(ope -> ope.sionTrack.channel.setPan(midiChannel.pan), channelNum);
            break;
            case SMFEvent.CC_EXPRESSION:
                midiChannel.setExpression(data);
                $(ope -> ope.sionTrack.channel.setAllStreamSendLevels(midiChannel._sionVolumes), channelNum);
            break;

            case SMFEvent.CC_SUSTAIN_PEDAL:
                midiChannel.sustainPedal = (data > 64);
                break;
            case SMFEvent.CC_PORTAMENTO:
                midiChannel.portamento = (data > 64);
                break;
//            case SMFEvent.CC_SOSTENUTO_PEDAL:
//            case SMFEvent.CC_SOFT_PEDAL:
//            case SMFEvent.CC_RESONANCE:
//            case SMFEvent.CC_RELEASE_TIME:
//            case SMFEvent.CC_ATTACK_TIME:
//            case SMFEvent.CC_CUTOFF_FREQ:
//            case SMFEvent.CC_DECAY_TIME:
//            case SMFEvent.CC_PROTAMENTO_CONTROL:
            case SMFEvent.CC_REVERB_SEND:
                midiChannel.setEffectSendLevel(1, data);
                $(ope -> ope.sionTrack.channel.setAllStreamSendLevels(midiChannel._sionVolumes), channelNum);
            break;
            case SMFEvent.CC_CHORUS_SEND:
                midiChannel.setEffectSendLevel(2, data);
                $(ope -> ope.sionTrack.channel.setAllStreamSendLevels(midiChannel._sionVolumes), channelNum);
            break;
            case SMFEvent.CC_DELAY_SEND:
                midiChannel.setEffectSendLevel(3, data);
                $(ope -> ope.sionTrack.channel.setAllStreamSendLevels(midiChannel._sionVolumes), channelNum);
            break;

            case SMFEvent.CC_NRPN_MSB:
                _rpnNumber = (data & 0x7f) << 7;
                break;
            case SMFEvent.CC_NRPN_LSB:
                _rpnNumber |= (data & 0x7f);
                _isNRPN = true;
                break;
            case SMFEvent.CC_RPN_MSB:
                _rpnNumber = (data & 0x7f) << 7;
                break;
            case SMFEvent.CC_RPN_LSB:
                _rpnNumber |= (data & 0x7f);
                _isNRPN = false;
                break;
            case SMFEvent.CC_DATA_ENTRY_MSB:
                _dataEntry = (data & 0x7f) << 7;
                if (!_isNRPN) _onRPN(midiChannel);
                else if (onNRPN != null) onNRPN.accept(channelNum, _rpnNumber, _dataEntry);
                break;
            case SMFEvent.CC_DATA_ENTRY_LSB:
                _dataEntry |= (data & 0x7f);
                if (!_isNRPN) _onRPN(midiChannel);
                else if (onNRPN != null) onNRPN.accept(channelNum, _rpnNumber, _dataEntry);
                break;
        }

        if ((_dispatchFlags & midiChannel.sionMIDIEventType & SiONMIDIEventFlag.CONTROL_CHANGE) != 0) {
            _sionDriver._dispatchMIDIEvent
            (SiONMIDIEvent.CONTROL_CHANGE, null, channelNum, controlerNumber, data);
        }
    }

    void $(Consumer<MIDIModuleOperator> func, int channelNum) {
        for (MIDIModuleOperator ope = _activeOperators.next; ope != _activeOperators; ope = ope.next) {
            if (ope.channel == channelNum) func.accept(ope);
        }
    }

    /** system exclusive */
    public void systemExclusive(int channelNum, ByteArray bytes) {
        if (checkByteArray(bytes, _GM_RESET, 0)) {
            _systemExclusiveMode = GM_MODE;
            resetAllChannels();
        } else if (checkByteArray(bytes, _GS_RESET, 0)) {
            _systemExclusiveMode = GS_MODE;
            resetAllChannels();
        } else if (checkByteArray(bytes, _XG_RESET, 0)) {
            _systemExclusiveMode = XG_MODE;
            resetAllChannels();
        } else if (checkByteArray(bytes, _GS_EXIT, 0)) {
            _systemExclusiveMode = "";
        }
        // GS USE_FOR_RYTHM_PART support
        else if (_systemExclusiveMode.equals(GS_MODE)) {
            if (checkByteArray(bytes, _GS_UFRP_CMD, 0)) {
                int trackNum = bytes.readUnsignedByte(),
                        c0x15 = bytes.readUnsignedByte(),
                        mapNum = bytes.readUnsignedByte();
                if ((trackNum & 0xf0) != 0x10 || c0x15 != 0x15 || mapNum > 2) return;
                trackNum = (trackNum & 15) + _portOffset;
                if (trackNum < midiChannels.length) midiChannels[trackNum].drumMode = mapNum;
            }
        }
        if (onSysEx != null) onSysEx.accept(channelNum, bytes);
    }

    private static final int[] _GM_RESET = new int[] {0xf0, 0x7e, 0x7f, 0x09, 0x01, 0xf7};
    private static final int[] _GS_RESET = new int[] {0xf0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x00, 0x7f, 0x00, 0x41, 0xf7};
    private static final int[] _GS_EXIT = new int[] {0xf0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x00, 0x7f, 0x7f, 0x41, 0xf7};
    private static final int[] _XG_RESET = new int[] {0xf0, 0x43, 0x10, 0x4c, 0x00, 0x00, 0x7e, 0x00, 0xf7};
    private static final int[] _GS_UFRP_CMD = new int[] {0xf0, 0x41, 0x10, 0x42, 0x12, 0x40};

    /** check ByteArray pattern by unsigned byte */
    public boolean checkByteArray(ByteArray bytes, int[] checkPattern, int position) {
        if (position != -1) bytes.position = position;
        int i, imax = checkPattern.length;
        for (i = 0; i < imax; i++) {
            int ch = bytes.readUnsignedByte();
            if (checkPattern[i] != ch) return false;
        }
        return true;
    }

    /** */
    void _onFinishSequence() {
        if (onFinishSequence != null) onFinishSequence.run();
    }

    private void _onRPN(MIDIModuleChannel midiChannel) {
        switch (_rpnNumber) {
            case SMFEvent.RPN_PITCHBEND_SENCE:
                midiChannel.pitchBendSensitivity = _dataEntry >> 7;
                break;
            case SMFEvent.RPN_FINE_TUNE:
                midiChannel.masterFineTune = (_dataEntry >> 7) - 64;
                break;
            case SMFEvent.RPN_COARSE_TUNE:
                midiChannel.masterCoarseTune = (_dataEntry >> 7) - 64;
                break;
        }
    }
}
