// Basic Synthesizer 
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//


package org.si.sound.synthesizers;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.SiONVoice;
import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.sequencer.SiMMLTrack;


/** Basic Synthesizer */
public class BasicSynth extends VoiceReference {

    // variables
    //

    /** tracks to control */
    protected List<SiMMLTrack> _tracks;

    // properties
    //

    @Override
    public void setVoice(SiONVoice v) {
        _voice.copyFrom(v); // copy from passed voice
        _voiceUpdateNumber++;
    }

    /** low-pass filter cutoff(0-1). */
    public double getCutoff() {
        return _voice.channelParam.cutoff * 0.0078125;
    }

    public void setCutoff(double c) {
        int i, imax = _tracks.size();
        SiOPMChannelParam p = _voice.channelParam;
        p.cutoff = (c <= 0) ? 0 : (c >= 1) ? 128 : (int) (c * 128);
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setSVFilter(p.cutoff, p.resonance, p.far, p.fdr1, p.fdr2, p.frr, p.fdc1, p.fdc2, p.fsc, p.frc);
        }
    }

    /** low-pass filter resonance(0-1). */
    public double getResonance() {
        return _voice.channelParam.resonance * 0.1111111111111111;
    }

    public void setResonance(double r) {
        int i, imax = _tracks.size();
        SiOPMChannelParam p = _voice.channelParam;
        p.resonance = (r <= 0) ? 0 : (r >= 1) ? 9 : (int) (r * 9);
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setSVFilter(p.cutoff, p.resonance, p.far, p.fdr1, p.fdr2, p.frr, p.fdc1, p.fdc2, p.fsc, p.frc);
        }
    }

    /** filter type (0:lowpass, 1:bandpass, 2:highpass) */
    public int getFilterType() {
        return _voice.channelParam.filterType;
    }

    public void setFilterType(int t) {
        int i, imax = _tracks.size();
        _voice.channelParam.filterType = t;
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setFilterType(t);
        }
    }

    /** modulation (low-frequency oscillator) wave shape, 0=saw, 1=square, 2=triangle, 3=random. */
    public int getLfoWaveShape() {
        return _voice.channelParam.lfoWaveShape;
    }

    public void setLfoWaveShape(int type) {
        _voice.channelParam.lfoWaveShape = type;
        _voiceUpdateNumber++;
    }

    /** modulation (low-frequency oscillator) cycle frames. */
    public int getLfoCycleFrames() {
        return _voice.channelParam.getLfoFrame();
    }

    public void setLfoCycleFrames(int frame) {
        _voice.channelParam.setLfoFrame(frame);
        int i, imax = _tracks.size(), ms = frame * 1000 / 60;
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setLFOCycleTime(ms);
        }
    }

    /** amplitude modulation. */
    public int getAmplitudeModulation() {
        return _voice.amDepth;
    }

    public void setAmplitudeModulation(int m) {
        _voice.channelParam.amd = _voice.amDepth = m;
        int i, imax = _tracks.size();
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setAmplitudeModulation(m);
        }
    }

    /** pitch modulation. */
    public int getPitchModulation() {
        return _voice.pmDepth;
    }

    public void setPitchModulation(int m) {
        _voice.channelParam.pmd = _voice.pmDepth = m;
        int i, imax = _tracks.size();
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setPitchModulation(m);
        }
    }

    /** attack rate (0-1), lower value makes attack slow. */
    public double getAttackTime() {
        int iar = _voice.channelParam.operatorParam[_voice.channelParam.opeCount - 1].ar;
        return (iar > 48) ? 0 : (1 - iar * 0.020833333333333332);
    }

    public void setAttackTime(double n) {
        int flg = SiOPMTable.final_oscilator_flags[_voice.channelParam.opeCount][_voice.channelParam.alg];
        int iar = (n == 0) ? 63 : (int) ((1 - n) * 48);
        if ((flg & 1) != 0) _voice.channelParam.operatorParam[0].ar = iar;
        if ((flg & 2) != 0) _voice.channelParam.operatorParam[1].ar = iar;
        if ((flg & 4) != 0) _voice.channelParam.operatorParam[2].ar = iar;
        if ((flg & 8) != 0) _voice.channelParam.operatorParam[3].ar = iar;
        int i, imax = _tracks.size();
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setAllAttackRate(iar);
        }
    }

    /** release rate (0-1), lower value makes release slow. */
    public double getReleaseTime() {
        int irr = _voice.channelParam.operatorParam[_voice.channelParam.opeCount - 1].rr;
        return (irr > 48) ? 0 : (1 - irr * 0.020833333333333332);
    }

    public void setReleaseTime(double n) {
        int flg = SiOPMTable.final_oscilator_flags[_voice.channelParam.opeCount][_voice.channelParam.alg];
        int irr = (n == 0) ? 63 : (int) ((1 - n) * 48);
        if ((flg & 1) != 0) _voice.channelParam.operatorParam[0].rr = irr;
        if ((flg & 2) != 0) _voice.channelParam.operatorParam[1].rr = irr;
        if ((flg & 4) != 0) _voice.channelParam.operatorParam[2].rr = irr;
        if ((flg & 8) != 0) _voice.channelParam.operatorParam[3].rr = irr;
        int i, imax = _tracks.size();
        for (i = 0; i < imax; i++) {
            _tracks.get(i).channel.setAllReleaseRate(irr);
        }
    }

    // constructor
    //

    protected BasicSynth() {
        this(5, 0, 63, 63, 0);
    }

    /**
     * constructor.
     *
     * @param moduleType Module type. 1st argument of '%'.
     * @param channelNum Channel number. 2nd argument of '%'.
     * @param ar         Attack rate (0-63).
     * @param rr         Release rate (0-63).
     * @param dt         pitchShift (64=1halftone).
     */
    public BasicSynth(int moduleType, int channelNum, int ar, int rr, int dt) {
        _voice = new SiONVoice(moduleType, channelNum, ar, rr, dt, -1, 0, 0);
        _tracks = new ArrayList<>();
    }

    // operations
    //

    /**
     * set filter envelop (same as '&#64;f' command in MML).
     *
     * @param cutoff    LP filter cutoff (0-1)
     * @param resonance LP filter resonance (0-1)
     * @param far       LP filter attack rate (0-63)
     * @param fdr1      LP filter decay rate 1 (0-63)
     * @param fdr2      LP filter decay rate 2 (0-63)
     * @param frr       LP filter release rate (0-63)
     * @param fdc1      LP filter decay cutoff 1 (0-1)
     * @param fdc2      LP filter decay cutoff 2 (0-1)
     * @param fsc       LP filter sustain cutoff (0-1)
     * @param frc       LP filter release cutoff (0-1)
     */
    public void setFilterEnvelop(int filterType, double cutoff, double resonance, int far, int fdr1, int fdr2, int frr, double fdc1, double fdc2, double fsc, double frc) {
        _voice.setFilterEnvelop(filterType, (int) (cutoff * 128), (int) (resonance * 9), far, fdr1, fdr2, frr, (int) (fdc1 * 128), (int) (fdc2 * 128), (int) (fsc * 128), (int) (frc * 128));
        _voiceUpdateNumber++;
    }

    /**
     * [Please use setFilterEnvelop instead of this function]. This function instanceof for compatibility of old versions.
     *
     * @param cutoff    LP filter cutoff (0-1)
     * @param resonance LP filter resonance (0-1)
     * @param far       LP filter attack rate (0-63)
     * @param fdr1      LP filter decay rate 1 (0-63)
     * @param fdr2      LP filter decay rate 2 (0-63)
     * @param frr       LP filter release rate (0-63)
     * @param fdc1      LP filter decay cutoff 1 (0-1)
     * @param fdc2      LP filter decay cutoff 2 (0-1)
     * @param fsc       LP filter sustain cutoff (0-1)
     * @param frc       LP filter release cutoff (0-1)
     */
    public void setLPFEnvelop(double cutoff, double resonance, int far, int fdr1, int fdr2, int frr, double fdc1, double fdc2, double fsc, double frc) {
        setFilterEnvelop(0, cutoff, resonance, far, fdr1, fdr2, frr, fdc1, fdc2, fsc, frc);
    }

    /**
     * Set amplitude modulation parameters (same as "ma" command in MML).
     *
     * @param depth     start modulation depth (((1st) same) argument)
     * @param end_depth end modulation depth (((2nd) same) argument)
     * @param delay     changing delay (((3rd) same) argument)
     * @param term      changing term (((4th) same) argument)
     */
    public void setAmplitudeModulation(int depth, int end_depth, int delay, int term) {
        _voice.setAmplitudeModulation(depth, end_depth, delay, term);
        _voiceUpdateNumber++;
    }

    /**
     * Set amplitude modulation parameters (same as "mp" command in MML).
     *
     * @param depth     start modulation depth (((1st) same) argument)
     * @param end_depth end modulation depth (((2nd) same) argument)
     * @param delay     changing delay (((3rd) same) argument)
     * @param term      changing term (((4th) same) argument)
     */
    public void setPitchModulation(int depth, int end_depth, int delay, int term) {
        _voice.setPitchModulation(depth, end_depth, delay, term);
        _voiceUpdateNumber++;
    }

    // internals
    //

    /** register single track */
    @Override
    public void _registerTrack(SiMMLTrack track) {
        _tracks.add(track);
    }

    /** register plural tracks */
    @Override
    public void _registerTracks(List<SiMMLTrack> tracks) {
        int i0 = _tracks.size(), imax = tracks.size(), i;
//        _tracks.length = i0 + imax;
        for (i = 0; i < imax; i++) _tracks.set(i0 + i, tracks.get(i));
    }

    /** unregister tracks */
    @Override
    public void _unregisterTracks(SiMMLTrack firstTrack, int count) {
        int index = _tracks.indexOf(firstTrack);
        if (index >= 0) _tracks.subList(index, index + count).clear();
    }
}
