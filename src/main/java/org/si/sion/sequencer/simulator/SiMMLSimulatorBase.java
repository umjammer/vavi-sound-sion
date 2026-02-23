//
// class for SiMML sequencer setting
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.simulator;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.module.channels.SiOPMChannelBase;
import org.si.sion.module.channels.SiOPMChannelManager;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.sequencer.base.MMLSequence;


/** Base class of all module simulators which control "SiMMLTrack" (not SiOPMChannel) to simulate various modules. */
public class SiMMLSimulatorBase {

    // constants
    //

    /* module type */
    public static final int MT_PSG = 0;  // PSG(DCSG)
    public static final int MT_APU = 1;  // FC pAPU
    public static final int MT_NOISE = 2;  // noise wave
    public static final int MT_MA3 = 3;  // MA3 wave form
    public static final int MT_CUSTOM = 4;  // SCC / custom wave table
    public static final int MT_ALL = 5;  // all pgTypes
    public static final int MT_FM = 6;  // FM sound module
    public static final int MT_PCM = 7;  // PCM
    public static final int MT_PULSE = 8;  // pulse wave
    public static final int MT_RAMP = 9;  // ramp wave
    public static final int MT_SAMPLE = 10; // sampler
    public static final int MT_KS = 11; // karplus strong
    public static final int MT_GB = 12; // gameboy
    public static final int MT_VRC6 = 13; // vrc6
    public static final int MT_SID = 14; // sid
    public static final int MT_FM_OPM = 15; // YM2151
    public static final int MT_FM_OPN = 16; // YM2203
    public static final int MT_FM_OPNA = 17; // YM2608
    public static final int MT_FM_OPLL = 18; // YM2413
    public static final int MT_FM_OPL3 = 19; // YM3812
    public static final int MT_FM_MA3 = 20; // YMU762
    public static final int MT_MAX = 21;

    // variables
    //
    /** module type */
    public int type;

    /** Default table converting from MML voice number to SiOPM pgType */
    protected SiMMLSimulatorVoiceSet _defaultVoiceSet;
    /** Tables converting from MML voice number to SiOPM pgType for each channel, if the table instanceof different for each channel */
    protected List<SiMMLSimulatorVoiceSet> _channelVoiceSet;
    /** This simulator can be ((the) used) FM voice's source wave or not */
    protected boolean _isSuitableForFMVoice;
    /** Default operator count */
    protected int _defaultOpeCount;

    // constructor
    //
    protected SiMMLSimulatorBase(int type, int channelCount, SiMMLSimulatorVoiceSet defaultVoiceSet) {
        this(type, channelCount, defaultVoiceSet, false);
    }

    protected SiMMLSimulatorBase(int type, int channelCount, SiMMLSimulatorVoiceSet defaultVoiceSet, boolean isSuitableForFMVoice) {
        this.type = type;
        this._isSuitableForFMVoice = isSuitableForFMVoice;
        this._defaultOpeCount = 1;
        this._channelVoiceSet = new ArrayList<>(channelCount);
        this._defaultVoiceSet = defaultVoiceSet;
    }

    // tone setting
    //

    /**
     * initialize tone by channel number.
     * call from SiMMLTrack::reset()/setChannelModuleType().
     * call from "%" MML command
     */
    public int initializeTone(SiMMLTrack track, int chNum, int bufferIndex) {
        // initialize
        int restrictedChNum = chNum;
        SiMMLSimulatorVoiceSet voiceSet = _defaultVoiceSet;
        if (0 <= chNum && chNum < _channelVoiceSet.size() && _channelVoiceSet.get(chNum) != null)
            voiceSet = _channelVoiceSet.get(chNum);
        else restrictedChNum = 0;

        // update channel instance in SiMMLTrack
        _updateChannelInstance(track, bufferIndex, voiceSet);

        // track setup
        track._channelNumber = (chNum < 0) ? -1 : chNum; // track has channel number include -1.
        track.channel.setChannelNumber(restrictedChNum);   // channel requires restrticted channel number
        track.channel.setAlgorism(_defaultOpeCount, 0);  //

        selectTone(track, voiceSet.initVoiceIndex);

        // return voice index
        return (chNum == -1) ? -1 : voiceSet.initVoiceIndex;
    }

    /**
     * select tone by tone number.
     * call from initializeTone(), SiMMLTrack::setChannelModuleType()/_bufferEnvelop()/_keyOn()/_setChannelParameters().
     * call from "%" and "&#64;" MML command
     */
    public MMLSequence selectTone(SiMMLTrack track, int voiceIndex) {
        return _selectSingleWaveTone(track, voiceIndex);
    }

    /** */
    protected MMLSequence _selectSingleWaveTone(SiMMLTrack track, int voiceIndex) {
        if (voiceIndex == -1) return null;

        int chNum = track.getChannelNumber();
        SiMMLSimulatorVoiceSet voiceSet = _defaultVoiceSet;
        if (chNum >= 0 && chNum < _channelVoiceSet.size() && _channelVoiceSet.get(chNum) != null)
            voiceSet = _channelVoiceSet.get(chNum);
        if (voiceIndex < 0 || voiceIndex >= voiceSet.voices.length) voiceIndex = voiceSet.initVoiceIndex;
        SiMMLSimulatorVoice voice = voiceSet.voices[voiceIndex];
        track.channel.setType(voice.pgType, voice.ptType);

        return null;
    }

    /** */
    protected void _updateChannelInstance(SiMMLTrack track, int bufferIndex, SiMMLSimulatorVoiceSet voiceSet) {
        SiMMLSimulatorVoice defaultVoice = voiceSet.voices[voiceSet.initVoiceIndex];
        int defaultChannelType = defaultVoice.channelType;

        // update channel instance
        if (track.channel == null) {
            // create new channel
            track.channel = SiOPMChannelManager.newChannel(defaultChannelType, null, bufferIndex);
        } else if (track.channel.getChannelType() != defaultChannelType) {
            // change channel type
            SiOPMChannelBase prev = track.channel;
            track.channel = SiOPMChannelManager.newChannel(defaultChannelType, prev, bufferIndex);
            SiOPMChannelManager.deleteChannel(prev);
        } else {
            // initialize channel
            track.channel.initialize(track.channel, bufferIndex);
            track._resetVolumeOffset();
        }
    }
}
