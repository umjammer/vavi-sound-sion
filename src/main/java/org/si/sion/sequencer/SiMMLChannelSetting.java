//
// class for SiMML sequencer setting
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer;

import org.si.sion.module.SiOPMTable;
import org.si.sion.module.channels.SiOPMChannelBase;
import org.si.sion.module.channels.SiOPMChannelManager;
import org.si.sion.sequencer.base.MMLSequence;


/** SiOPM channel setting */
public class SiMMLChannelSetting {

    // constants
    //

    public static final int SELECT_TONE_NOP = 0;
    public static final int SELECT_TONE_NORMAL = 1;
    public static final int SELECT_TONE_FM = 2;

    // variables
    //

    public int type;
    int _selectToneType;
    int[] _pgTypeList;
    int[] _ptTypeList;
    int _initVoiceIndex;
    int[] _voiceIndexTable;
    int _channelType;
    boolean _isSuitableForFMVoice;
    int _defaultOpeCount;
    private SiOPMTable _table;

    // constructor
    //
    SiMMLChannelSetting(int type, int offset, int length, int step, int channelCount) {
        int i, idx;
        _table = SiOPMTable.getInstance();
        _pgTypeList = new int[length];
        _ptTypeList = new int[length];
        for (i = 0, idx = offset; i < length; i++, idx += step) {
            _pgTypeList[i] = idx;
            _ptTypeList[i] = _table.getWaveTable(idx).defaultPTType;
        }
        _voiceIndexTable = new int[channelCount];
        for (i = 0; i < channelCount; i++) {
            _voiceIndexTable[i] = i;
        }

        this._initVoiceIndex = 0;
        this.type = type;
        _channelType = SiOPMChannelManager.CT_CHANNEL_FM;
        _selectToneType = SELECT_TONE_NORMAL;
        _defaultOpeCount = 1;
        _isSuitableForFMVoice = true;
    }

    // tone setting
    //

    /**
     * initialize tone by channel number.
     * call from SiMMLTrack::reset()/setChannelModuleType().
     * call from "%" MML command
     */
    int initializeTone(SiMMLTrack track, int chNum, int bufferIndex) {
        // update channel instance
        if (track.channel == null) {
            // create new channel
            track.channel = SiOPMChannelManager.newChannel(_channelType, null, bufferIndex);
        } else if (track.channel.getChannelType() != _channelType) {
            // change channel type
            SiOPMChannelBase prev = track.channel;
            track.channel = SiOPMChannelManager.newChannel(_channelType, prev, bufferIndex);
            SiOPMChannelManager.deleteChannel(prev);
        } else {
            // initialize channel
            track.channel.initialize(track.channel, bufferIndex);
            track._resetVolumeOffset();
        }

        // initialize
        // voiceIndex = chNum except for PSG, APU and analog
        int voiceIndex = _initVoiceIndex;
        int chNumRestrict = chNum;
        if (0 <= chNum && chNum < _voiceIndexTable.length) voiceIndex = _voiceIndexTable[chNum];
        else chNumRestrict = 0;
        // track has channel number include -1.
        track._channelNumber = (chNum < 0) ? -1 : chNum;
        // channel requires restricted channel number
        track.channel.setChannelNumber(chNumRestrict);
        track.channel.setAlgorism(_defaultOpeCount, 0);
        selectTone(track, voiceIndex);

        // return voice index
        return (chNum == -1) ? -1 : voiceIndex;
    }

    /**
     * select tone by tone number.
     * call from initializeTone(), SiMMLTrack::setChannelModuleType()/_bufferEnvelop()/_keyOn()/_setChannelParameters().
     * call from "%" and "&#64;" MML command
     */
    MMLSequence selectTone(SiMMLTrack track, int voiceIndex) {
        if (voiceIndex == -1) return null;

        SiMMLVoice voice;

        switch (_selectToneType) {
            case SELECT_TONE_NORMAL:
                if (voiceIndex < 0 || voiceIndex >= _pgTypeList.length) voiceIndex = _initVoiceIndex;
                track.channel.setType(_pgTypeList[voiceIndex], _ptTypeList[voiceIndex]);
                break;
            case SELECT_TONE_FM: // %6
                if (voiceIndex < 0 || voiceIndex >= SiMMLTable.VOICE_MAX) voiceIndex = 0;
                voice = SiMMLTable.getInstance().getSiMMLVoice(voiceIndex);
                if (voice != null) {
                    if (voice.updateTrackParameters) {
                        voice.updateTrackVoice(track);
                        return null;
                    } else {
                        // this module changes only channel params, not track params.
                        track.channel.setSiOPMChannelParam(voice.channelParam, false, false);
                        track._resetVolumeOffset();
                        return (voice.channelParam.initSequence.isEmpty()) ? null : voice.channelParam.initSequence;
                    }
                }
                break;
            default:
                break;
        }
        return null;
    }
}
