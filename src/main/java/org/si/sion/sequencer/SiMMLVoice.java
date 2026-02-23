//
// Voice data
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMWaveBase;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.module.SiOPMWaveTable;


/**
 * Voice data. This includes SiOPMChannelParam.
 *
 * @see org.si.sion.module.SiOPMChannelParam
 * @see org.si.sion.module.SiOPMOperatorParam
 */
public class SiMMLVoice {
    // variables
    //
    /** chip type */
    public String chipType;

    /** update track parameters, false to update only channel params. @default false(SiMMLVoice), true(SiONVoice) */
    public boolean updateTrackParameters;
    /** update volume, velocity, expression and panning when the voice instanceof set. @default false(ignore volume settings) */
    public boolean updateVolumes;

    /** module type, 1st argument of '%'. @default 0 */
    public int moduleType;
    /** channel number, 2nd argument of '%'. @default 0 */
    public int channelNum;
    /** tone number, 1st argument of '&#64;'. -1;do nothing. @default -1 */
    public int toneNum;
    /** preferable note. -1;no preferable note. @default -1 */
    public int preferableNote;

    /** parameters for FM sound channel. */
    public SiOPMChannelParam channelParam;
    /** wave data. @default null */
    public SiOPMWaveBase waveData;
    /** PMS guitar tension @default 8 */
    public int pmsTension;

    /** default gate time (same as "q" command * 0.125), set Number.NaN to ignore. @default Number.NaN */
    public double defaultGateTime;
    /** [Not implemented in current version] default absolute gate time (((1st) same) argument of "&#64;q" command), set -1 to ignore. @default -1 */
    public int defaultGateTicks;
    /** [Not implemented in current version] default key on delay (((2nd) same) argument "&#64;q" command), set -1 to ignore. @default -1 */
    public int defaultKeyOnDelayTicks;
    /** track pitch shift (same as "k" command). @default 0 */
    public int pitchShift;
    /** track key transpose (same as "kt" command). @default 0 */
    public int noteShift;
    /** portamento. @default 0 */
    public int portamento;
    /** release sweep. 2nd argument of '&#64;rr' and 's'. @default 0 */
    public int releaseSweep;

    /** velocity @default 256 */
    public int velocity;
    /** expression @default 128 */
    public int expression;
    /** velocity table mode (((1st) same) argument of "%v" command). @default 0 */
    public int velocityMode;
    /** velocity table mode (((2nd) same) argument of "%v" command). @default 0 */
    public int vcommandShift;
    /** expression table mode (same as "%x" command). @default 0 */
    public int expressionMode;

    /** amplitude modulation depth. 1st argument of 'ma'. @default 0 */
    public int amDepth;
    /** amplitude modulation depth after changing. 2nd argument of 'ma'. @default 0 */
    public int amDepthEnd;
    /** amplitude modulation changing delay. 3rd argument of 'ma'. @default 0 */
    public int amDelay;
    /** amplitude modulation changing term. 4th argument of 'ma'. @default 0 */
    public int amTerm;
    /** pitch modulation depth. 1st argument of 'mp'. @default 0 */
    public int pmDepth;
    /** pitch modulation depth after changing. 2nd argument of 'mp'. @default 0 */
    public int pmDepthEnd;
    /** pitch modulation changing delay. 3rd argument of 'mp'. @default 0 */
    public int pmDelay;
    /** pitch modulation changing term. 4th argument of 'mp'. @default 0 */
    public int pmTerm;

    /** note on tone envelop table. 1st argument of '&#64;&#64;' @default null */
    public SiMMLEnvelopTable noteOnToneEnvelop;
    /** note on amplitude envelop table. 1st argument of 'na' @default null */
    public SiMMLEnvelopTable noteOnAmplitudeEnvelop;
    /** note on filter envelop table. 1st argument of 'nf' @default null */
    public SiMMLEnvelopTable noteOnFilterEnvelop;
    /** note on pitch envelop table. 1st argument of 'np' @default null */
    public SiMMLEnvelopTable noteOnPitchEnvelop;
    /** note on note envelop table. 1st argument of 'nt' @default null */
    public SiMMLEnvelopTable noteOnNoteEnvelop;
    /** note off tone envelop table. 1st argument of '_&#64;&#64;' @default null */
    public SiMMLEnvelopTable noteOffToneEnvelop;
    /** note off amplitude envelop table. 1st argument of '_na' @default null */
    public SiMMLEnvelopTable noteOffAmplitudeEnvelop;
    /** note off filter envelop table. 1st argument of '_nf' @default null */
    public SiMMLEnvelopTable noteOffFilterEnvelop;
    /** note off pitch envelop table. 1st argument of '_np' @default null */
    public SiMMLEnvelopTable noteOffPitchEnvelop;
    /** note off note envelop table. 1st argument of '_nt' @default null */
    public SiMMLEnvelopTable noteOffNoteEnvelop;

    /** note on tone envelop tablestep. 2nd argument of '&#64;&#64;' @default 1 */
    public int noteOnToneEnvelopStep;
    /** note on amplitude envelop tablestep. 2nd argument of 'na' @default 1 */
    public int noteOnAmplitudeEnvelopStep;
    /** note on filter envelop tablestep. 2nd argument of 'nf' @default 1 */
    public int noteOnFilterEnvelopStep;
    /** note on pitch envelop tablestep. 2nd argument of 'np' @default 1 */
    public int noteOnPitchEnvelopStep;
    /** note on note envelop tablestep. 2nd argument of 'nt' @default 1 */
    public int noteOnNoteEnvelopStep;
    /** note off tone envelop tablestep. 2nd argument of '_&#64;&#64;' @default 1 */
    public int noteOffToneEnvelopStep;
    /** note off amplitude envelop tablestep. 2nd argument of '_na' @default 1 */
    public int noteOffAmplitudeEnvelopStep;
    /** note off filter envelop tablestep. 2nd argument of '_nf' @default 1 */
    public int noteOffFilterEnvelopStep;
    /** note off pitch envelop tablestep. 2nd argument of '_np' @default 1 */
    public int noteOffPitchEnvelopStep;
    /** note off note envelop tablestep. 2nd argument of '_nt' @default 1 */
    public int noteOffNoteEnvelopStep;

    // properties
    //

    /** FM voice flag */
    public boolean getIsFMVoice() {
        return (moduleType == 6);
    }

    /** PCM voice flag */
    public boolean getIsPCMVoice() {
        return (waveData instanceof SiOPMWavePCMTable || waveData instanceof SiOPMWavePCMData);
    }

    /** Sampler voice flag */
    public boolean getIsSamplerVoice() {
        return (waveData instanceof SiOPMWaveSamplerTable);
    }

    /** wave table voice flag */
    public boolean getIsWaveTableVoice() {
        return (waveData instanceof SiOPMWaveTable);
    }

    /** suitability to register in %6 voices */
    public boolean isSuitableForFMVoice() {
        return updateTrackParameters || (SiMMLTable.isSuitableForFMVoice(moduleType) && waveData == null);
    }

    /**
     * set moduleType, channelNum, toneNum and 0th operator's pgType simultaneously.
     *
     * @param moduleType Channel module type
     * @param channelNum Channel number. For %2-11, this value instanceof ((1st) same) argument of '_&#64;'.
     * @param toneNum    Tone number. Ussualy, this argument instanceof used only in %0;PSG and %1;APU.
     */
    public void setModuleType(int moduleType, int channelNum, int toneNum) {
        this.moduleType = moduleType;
        this.channelNum = channelNum;
        this.toneNum = toneNum;
        int pgType = SiMMLTable.getPGType(moduleType, channelNum, toneNum);
        if (pgType != -1) channelParam.operatorParam[0].setPGType(pgType);
    }


    // constrctor
    //

    /** constructor. */
    public SiMMLVoice() {
        channelParam = new SiOPMChannelParam();
        initialize();
    }


    // setting
    //

    /** update track's voice paramters */
    public SiMMLTrack updateTrackVoice(SiMMLTrack track) {
        // synthesizer modules
        switch (moduleType) {
            case 6:  // Registered FM voice (%6)
                track.setChannelModuleType(6, channelNum, 0);
                break;
            case 11: // PMS Guitar (%11)
                track.setChannelModuleType(11, 1, 0);
                track.channel.setSiOPMChannelParam(channelParam, false, false);
                track.channel.setAllReleaseRate(pmsTension);
                if (getIsPCMVoice()) track.channel.setWaveData(waveData);
                break;
            default: // other sound modules
                if (waveData != null) {
                    // voice with wave data
                    track.setChannelModuleType(waveData.moduleType, -1, 0);
                    track.channel.setSiOPMChannelParam(channelParam, updateVolumes, false);
                    track.channel.setWaveData(waveData);
                } else {
                    track.setChannelModuleType(moduleType, channelNum, toneNum);
                    track.channel.setSiOPMChannelParam(channelParam, updateVolumes, false);
                }
                break;
        }

        // track settings
        //if (defaultKeyOnDelayTicks  > 0) track.defaultKeyOnDelayTicks = defaultKeyOnDelayTicks -> samplecount;
        //if (defaultGateTicks > 0) track.quantCount = defaultGateTicks -> samplecount;
        if (!Double.isNaN(defaultGateTime)) track.quantRatio = defaultGateTime;
        track.pitchShift = pitchShift;
        track.noteShift = noteShift;
        track._vcommandShift = vcommandShift;
        track.setVelocityMode(velocityMode);
        track.setExpressionMode(expressionMode);
        if (updateVolumes) {
            track.setVelocity(velocity);
            track.setExpression(expression);
        }

        track.setPortament(portamento);
        track.setReleaseSweep(releaseSweep);
        track.setModulationEnvelop(false, amDepth, amDepthEnd, amDelay, amTerm);
        track.setModulationEnvelop(true, pmDepth, pmDepthEnd, pmDelay, pmTerm);
        {
            track.setToneEnvelop(1, noteOnToneEnvelop, noteOnToneEnvelopStep);
            track.setAmplitudeEnvelop(1, noteOnAmplitudeEnvelop, noteOnAmplitudeEnvelopStep, false);
            track.setFilterEnvelop(1, noteOnFilterEnvelop, noteOnFilterEnvelopStep);
            track.setPitchEnvelop(1, noteOnPitchEnvelop, noteOnPitchEnvelopStep);
            track.setNoteEnvelop(1, noteOnNoteEnvelop, noteOnNoteEnvelopStep);
            track.setToneEnvelop(0, noteOffToneEnvelop, noteOffToneEnvelopStep);
            track.setAmplitudeEnvelop(0, noteOffAmplitudeEnvelop, noteOffAmplitudeEnvelopStep, false);
            track.setFilterEnvelop(0, noteOffFilterEnvelop, noteOffFilterEnvelopStep);
            track.setPitchEnvelop(0, noteOffPitchEnvelop, noteOffPitchEnvelopStep);
            track.setNoteEnvelop(0, noteOffNoteEnvelop, noteOffNoteEnvelopStep);
        }
        return track;
    }


    /** [NOT RECOMENDED] this function instanceof only for compatibility of previous versions */
    public SiMMLTrack setTrackVoice(SiMMLTrack track) {
        return updateTrackVoice(track);
    }


    // operation
    //

    /** initializer */
    public void initialize() {
        chipType = "";

        updateTrackParameters = false;
        updateVolumes = false;

        moduleType = 5;
        channelNum = -1;
        toneNum = -1;
        preferableNote = -1;

        channelParam.initialize();
        waveData = null;
        pmsTension = 8;

        defaultGateTime = Double.NaN;
        defaultGateTicks = -1;
        defaultKeyOnDelayTicks = -1;
        pitchShift = 0;
        noteShift = 0;
        portamento = 0;
        releaseSweep = 0;

        velocity = 256;
        expression = 128;
        vcommandShift = 4;
        velocityMode = 0;
        expressionMode = 0;

        amDepth = 0;
        amDepthEnd = 0;
        amDelay = 0;
        amTerm = 0;
        pmDepth = 0;
        pmDepthEnd = 0;
        pmDelay = 0;
        pmTerm = 0;

        noteOnToneEnvelop = null;
        noteOnAmplitudeEnvelop = null;
        noteOnFilterEnvelop = null;
        noteOnPitchEnvelop = null;
        noteOnNoteEnvelop = null;
        noteOffToneEnvelop = null;
        noteOffAmplitudeEnvelop = null;
        noteOffFilterEnvelop = null;
        noteOffPitchEnvelop = null;
        noteOffNoteEnvelop = null;

        noteOnToneEnvelopStep = 1;
        noteOnAmplitudeEnvelopStep = 1;
        noteOnFilterEnvelopStep = 1;
        noteOnPitchEnvelopStep = 1;
        noteOnNoteEnvelopStep = 1;
        noteOffToneEnvelopStep = 1;
        noteOffAmplitudeEnvelopStep = 1;
        noteOffFilterEnvelopStep = 1;
        noteOffPitchEnvelopStep = 1;
        noteOffNoteEnvelopStep = 1;
    }


    /** copy all parameters */
    public void copyFrom(SiMMLVoice src) {
        chipType = src.chipType;

        updateTrackParameters = src.updateTrackParameters;
        updateVolumes = src.updateVolumes;

        moduleType = src.moduleType;
        channelNum = src.channelNum;
        toneNum = src.toneNum;
        preferableNote = src.preferableNote;
        channelParam.copyFrom(src.channelParam);

        waveData = src.waveData;
        pmsTension = src.pmsTension;

        defaultGateTime = src.defaultGateTime;
        defaultGateTicks = src.defaultGateTicks;
        defaultKeyOnDelayTicks = src.defaultKeyOnDelayTicks;
        pitchShift = src.pitchShift;
        noteShift = src.noteShift;
        portamento = src.portamento;
        releaseSweep = src.releaseSweep;

        velocity = src.velocity;
        expression = src.expression;
        vcommandShift = src.vcommandShift;
        velocityMode = src.velocityMode;
        expressionMode = src.expressionMode;

        amDepth = src.amDepth;
        amDepthEnd = src.amDepthEnd;
        amDelay = src.amDelay;
        amTerm = src.amTerm;
        pmDepth = src.pmDepth;
        pmDepthEnd = src.pmDepthEnd;
        pmDelay = src.pmDelay;
        pmTerm = src.pmTerm;

        if (src.noteOnToneEnvelop != null) noteOnToneEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOnToneEnvelop);
        if (src.noteOnAmplitudeEnvelop != null)
            noteOnAmplitudeEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOnAmplitudeEnvelop);
        if (src.noteOnFilterEnvelop != null) noteOnFilterEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOnFilterEnvelop);
        if (src.noteOnPitchEnvelop != null) noteOnPitchEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOnPitchEnvelop);
        if (src.noteOnNoteEnvelop != null) noteOnNoteEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOnNoteEnvelop);
        if (src.noteOffToneEnvelop != null) noteOffToneEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOffToneEnvelop);
        if (src.noteOffAmplitudeEnvelop != null)
            noteOffAmplitudeEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOffAmplitudeEnvelop);
        if (src.noteOffFilterEnvelop != null) noteOffFilterEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOffFilterEnvelop);
        if (src.noteOffPitchEnvelop != null) noteOffPitchEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOffPitchEnvelop);
        if (src.noteOffNoteEnvelop != null) noteOffNoteEnvelop = new SiMMLEnvelopTable(null, 0).copyFrom(src.noteOffNoteEnvelop);

        noteOnToneEnvelopStep = src.noteOnToneEnvelopStep;
        noteOnAmplitudeEnvelopStep = src.noteOnAmplitudeEnvelopStep;
        noteOnFilterEnvelopStep = src.noteOnFilterEnvelopStep;
        noteOnPitchEnvelopStep = src.noteOnPitchEnvelopStep;
        noteOnNoteEnvelopStep = src.noteOnNoteEnvelopStep;
        noteOffToneEnvelopStep = src.noteOffToneEnvelopStep;
        noteOffAmplitudeEnvelopStep = src.noteOffAmplitudeEnvelopStep;
        noteOffFilterEnvelopStep = src.noteOffFilterEnvelopStep;
        noteOffPitchEnvelopStep = src.noteOffPitchEnvelopStep;
        noteOffNoteEnvelopStep = src.noteOffNoteEnvelopStep;
    }


    /** ((blank) set) pcm voice */
    public SiMMLVoice _newBlankPCMVoice(int channelNum) {
        SiOPMWavePCMTable pcmTable = new SiOPMWavePCMTable();
        this.moduleType = 7;
        this.channelNum = channelNum;
        this.waveData = pcmTable;
        return this;
    }
}
