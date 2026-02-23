//
// tables for SiMML driver
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.channels.SiOPMChannelManager;
import org.si.sion.sequencer.simulator.SiMMLSimulatorAPU;
import org.si.sion.sequencer.simulator.SiMMLSimulatorBase;
import org.si.sion.sequencer.simulator.SiMMLSimulatorFMMA3;
import org.si.sion.sequencer.simulator.SiMMLSimulatorFMOPL3;
import org.si.sion.sequencer.simulator.SiMMLSimulatorFMOPLL;
import org.si.sion.sequencer.simulator.SiMMLSimulatorFMOPM;
import org.si.sion.sequencer.simulator.SiMMLSimulatorFMOPN;
import org.si.sion.sequencer.simulator.SiMMLSimulatorFMOPNA;
import org.si.sion.sequencer.simulator.SiMMLSimulatorFMSiOPM;
import org.si.sion.sequencer.simulator.SiMMLSimulatorGB;
import org.si.sion.sequencer.simulator.SiMMLSimulatorKS;
import org.si.sion.sequencer.simulator.SiMMLSimulatorMA3WaveTable;
import org.si.sion.sequencer.simulator.SiMMLSimulatorNoise;
import org.si.sion.sequencer.simulator.SiMMLSimulatorPCM;
import org.si.sion.sequencer.simulator.SiMMLSimulatorPSG;
import org.si.sion.sequencer.simulator.SiMMLSimulatorPulse;
import org.si.sion.sequencer.simulator.SiMMLSimulatorRamp;
import org.si.sion.sequencer.simulator.SiMMLSimulatorSID;
import org.si.sion.sequencer.simulator.SiMMLSimulatorSampler;
import org.si.sion.sequencer.simulator.SiMMLSimulatorSiOPM;
import org.si.sion.sequencer.simulator.SiMMLSimulatorVRC6;
import org.si.sion.sequencer.simulator.SiMMLSimulatorWT;


/** table for sequencer */
public class SiMMLTable {

    // constants
    //

    // module types (0-11)
    public static final int MT_PSG = SiMMLSimulatorBase.MT_PSG;      // PSG(DCSG)
    public static final int MT_APU = SiMMLSimulatorBase.MT_APU;      // FC pAPU
    public static final int MT_NOISE = SiMMLSimulatorBase.MT_NOISE;    // noise wave
    public static final int MT_MA3 = SiMMLSimulatorBase.MT_MA3;      // MA3 wave form
    public static final int MT_CUSTOM = SiMMLSimulatorBase.MT_CUSTOM;   // SCC / custom wave table
    public static final int MT_ALL = SiMMLSimulatorBase.MT_ALL;      // all pgTypes
    public static final int MT_FM = SiMMLSimulatorBase.MT_FM;       // FM sound module
    public static final int MT_PCM = SiMMLSimulatorBase.MT_PCM;      // PCM
    public static final int MT_PULSE = SiMMLSimulatorBase.MT_PULSE;    // pulse wave
    public static final int MT_RAMP = SiMMLSimulatorBase.MT_RAMP;     // ramp wave
    public static final int MT_SAMPLE = SiMMLSimulatorBase.MT_SAMPLE;   // sampler
    public static final int MT_KS = SiMMLSimulatorBase.MT_KS;       // karplus strong
    public static final int MT_GB = SiMMLSimulatorBase.MT_GB;       // gameboy
    public static final int MT_VRC6 = SiMMLSimulatorBase.MT_VRC6;     // vrc6
    public static final int MT_SID = SiMMLSimulatorBase.MT_SID;      // sid
    public static final int MT_FM_OPM = SiMMLSimulatorBase.MT_FM_OPM;   // YM2151
    public static final int MT_FM_OPN = SiMMLSimulatorBase.MT_FM_OPN;   // YM2203
    public static final int MT_FM_OPNA = SiMMLSimulatorBase.MT_FM_OPNA;  // YM2608
    public static final int MT_FM_OPLL = SiMMLSimulatorBase.MT_FM_OPLL;  // YM2413
    public static final int MT_FM_OPL3 = SiMMLSimulatorBase.MT_FM_OPL3;  // YM3812
    public static final int MT_FM_MA3 = SiMMLSimulatorBase.MT_FM_MA3;   // YMU762
    public static final int MT_MAX = SiMMLSimulatorBase.MT_MAX;

    // module restriction type
    public static final int ENV_TABLE_MAX = 512;
    public static final int VOICE_MAX = 256;

    // variables
    //

    /** module setting table */
    public SiMMLChannelSetting[] channelModuleSetting = null;
    /** module setting table */
    public Object[] effectModuleSetting = null;
    /** module simulators */
    public SiMMLSimulatorBase[] simulators = null;

    /** table from tsscp @s commnd to OPM ar */
    public String[] tss_s2ar = null;
    /** table from tsscp @s commnd to OPM dr */
    public String[] tss_s2dr = null;
    /** table from tsscp @s commnd to OPM sr */
    public String[] tss_s2sr = null;
    /** table from tsscp s commnd to OPM rr */
    public String[] tss_s2rr = null;

    /** table of OPLL preset voices (from virturenes) */
    public int[] presetRegisterYM2413 = {
            0x00000000, 0x00000000, 0x61611e17, 0xf07f0717, 0x13410f0d, 0xced24313, 0x03019904, 0xffc30373,
            0x21611b07, 0xaf634028, 0x22211e06, 0xf0760828, 0x31221605, 0x90710018, 0x21611d07, 0x82811017,
            0x23212d16, 0xc0700707, 0x61211b06, 0x64651818, 0x61610c18, 0x85a07907, 0x23218711, 0xf0a400f7,
            0x97e12807, 0xfff302f8, 0x61100c05, 0xf2c440c8, 0x01015603, 0xb4b22358, 0x61418903, 0xf1f4f013
    };

    /** table of VRC7 preset voices (from virturenes) */
    public int[] presetRegisterVRC7 = {
            0x00000000, 0x00000000, 0x3301090e, 0x94904001, 0x13410f0d, 0xced34313, 0x01121b06, 0xffd20032,
            0x61611b07, 0xaf632028, 0x22211e06, 0xf0760828, 0x66211500, 0x939420f8, 0x21611c07, 0x82811017,
            0x2321201f, 0xc0710747, 0x25312605, 0x644118f8, 0x17212807, 0xff8302f8, 0x97812507, 0xcfc80214,
            0x2121540f, 0x807f0707, 0x01015603, 0xd3b24358, 0x31210c03, 0x82c04007, 0x21010c03, 0xd4d34084
    };

    /** table of VRC7/OPLL preset drums (from virturenes) */
    public int[] presetRegisterVRC7Drums = {
            0x04212800, 0xdff8fff8, 0x23220000, 0xd8f8f8f8, 0x25180000, 0xf8daf855
    };

    /** Preset voice set of OPLL */
    public List<SiMMLVoice> presetVoiceYM2413 = null;
    /** Preset voice set of VRC7 */
    public List<SiMMLVoice> presetVoiceVRC7 = null;
    /** Preset voice set of VRC7/OPLL drum */
    public List<SiMMLVoice> presetVoiceVRC7Drums = null;

    /** algorism table for OPM/OPN. */
    public int[][] alg_opm = {
            {0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 1, 1, 1, 1, 0, 1, 1, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 1, 2, 3, 3, 4, 3, 5, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1}
    };
    /** algorism table for OPL3 */
    public int[][] alg_opl = {
            {0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 3, 2, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 4, 8, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
    };
    /** algorism table for MA3 */
    public int[][] alg_ma3 = {
            {0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 1, 1, 1, 0, 1, 1, 1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, 5, 2, 0, 3, 2, 2, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, 7, 2, 0, 4, 8, 9, -1, -1, -1, -1, -1, -1, -1, -1}
    };
    /** algorism table for OPX. LSB4 instanceof the flag of feedback connection. */
    public int[][] alg_opx = {
            {0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 16, 1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 16, 1, 2, 3, 19, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1},
            {0, 16, 1, 2, 3, 19, 4, 20, 8, 11, 6, 22, 5, 9, 12, 7}
    };
    /** initial connection */
    public int[] alg_init = {0, 1, 5, 7};

    // Master envelop tables list
    private SiMMLEnvelopTable[] _masterEnvelops = null;
    // Master voices list
    private SiMMLVoice[] _masterVoices = null;
    /** @private [internal] Stencil envelop tables list */
    SiMMLEnvelopTable[] _stencilEnvelops = null;
    /** @private [internal] Stencil voices list */
    SiMMLVoice[] _stencilVoices = null;

    // static public instance
    //

    /** internal instance, you can access this after creating SiONDriver. */
    public static SiMMLTable _instance = null;

    /** singleton instance */
    public static SiMMLTable getInstance() {
        return _instance != null ? _instance : (_instance = new SiMMLTable());
    }

    // constructor
    //

    /** constructor */
    public SiMMLTable() {
        int i, j;

        // Channel module setting
        SiMMLChannelSetting ms;
        channelModuleSetting = new SiMMLChannelSetting[MT_MAX];
        channelModuleSetting[MT_PSG] = new SiMMLChannelSetting(MT_PSG, SiOPMTable.PG_SQUARE, 3, 1, 4);   // PSG
        channelModuleSetting[MT_APU] = new SiMMLChannelSetting(MT_APU, SiOPMTable.PG_PULSE, 11, 2, 4);   // FC pAPU
        channelModuleSetting[MT_NOISE] = new SiMMLChannelSetting(MT_NOISE, SiOPMTable.PG_NOISE_WHITE, 16, 1, 16);  // noise
        channelModuleSetting[MT_MA3] = new SiMMLChannelSetting(MT_MA3, SiOPMTable.PG_MA3_WAVE, 32, 1, 32);  // MA3
        channelModuleSetting[MT_CUSTOM] = new SiMMLChannelSetting(MT_CUSTOM, SiOPMTable.PG_CUSTOM, 256, 1, 256); // SCC / custom wave table
        channelModuleSetting[MT_ALL] = new SiMMLChannelSetting(MT_ALL, SiOPMTable.PG_SINE, 512, 1, 512); // all pgTypes
        channelModuleSetting[MT_FM] = new SiMMLChannelSetting(MT_FM, SiOPMTable.PG_SINE, 1, 1, 1);   // FM sound module
        channelModuleSetting[MT_PCM] = new SiMMLChannelSetting(MT_PCM, SiOPMTable.PG_PCM, 128, 1, 128); // PCM
        channelModuleSetting[MT_PULSE] = new SiMMLChannelSetting(MT_PULSE, SiOPMTable.PG_PULSE, 32, 1, 32);  // pulse
        channelModuleSetting[MT_RAMP] = new SiMMLChannelSetting(MT_RAMP, SiOPMTable.PG_RAMP, 128, 1, 128); // ramp
        channelModuleSetting[MT_SAMPLE] = new SiMMLChannelSetting(MT_SAMPLE, 0, 4, 1, 4);   // sampler. this is based on SiOPMChannelSampler
        channelModuleSetting[MT_KS] = new SiMMLChannelSetting(MT_KS, 0, 3, 1, 3);   // karplus strong (0-2 to choose seed generator algrism)
        channelModuleSetting[MT_GB] = new SiMMLChannelSetting(MT_GB, SiOPMTable.PG_PULSE, 11, 2, 4);   // Gameboy
        channelModuleSetting[MT_VRC6] = new SiMMLChannelSetting(MT_VRC6, SiOPMTable.PG_PULSE, 9, 1, 3);   // VRC6
        channelModuleSetting[MT_SID] = new SiMMLChannelSetting(MT_SID, SiOPMTable.PG_PULSE, 12, 1, 3);   // SID

        // PSG setting
        ms = channelModuleSetting[MT_PSG];
        ms._pgTypeList[0] = SiOPMTable.PG_SQUARE;
        ms._pgTypeList[1] = SiOPMTable.PG_NOISE_PULSE;
        ms._pgTypeList[2] = SiOPMTable.PG_PC_NZ_16BIT;
        ms._ptTypeList[0] = SiOPMTable.PT_PSG;
        ms._ptTypeList[1] = SiOPMTable.PT_PSG_NOISE;
        ms._ptTypeList[2] = SiOPMTable.PT_PSG;
        ms._voiceIndexTable[0] = 0;
        ms._voiceIndexTable[1] = 0;
        ms._voiceIndexTable[2] = 0;
        ms._voiceIndexTable[3] = 1;
        // APU setting
        ms = channelModuleSetting[MT_APU];
        ms._pgTypeList[8] = SiOPMTable.PG_TRIANGLE_FC;
        ms._pgTypeList[9] = SiOPMTable.PG_NOISE_PULSE;
        ms._pgTypeList[10] = SiOPMTable.PG_NOISE_SHORT;
        for (i = 0; i < 9; i++) {
            ms._ptTypeList[i] = SiOPMTable.PT_PSG;
        }
        for (i = 9; i < 11; i++) {
            ms._ptTypeList[i] = SiOPMTable.PT_APU_NOISE;
        }
        ms._initVoiceIndex = 1;
        ms._voiceIndexTable[0] = 4;
        ms._voiceIndexTable[1] = 4;
        ms._voiceIndexTable[2] = 8;
        ms._voiceIndexTable[3] = 9;
        // GB setting
        ms = channelModuleSetting[MT_GB];
        ms._pgTypeList[8] = SiOPMTable.PG_CUSTOM;
        ms._pgTypeList[9] = SiOPMTable.PG_NOISE_PULSE;
        ms._pgTypeList[10] = SiOPMTable.PG_NOISE_GB_SHORT;
        for (i = 0; i < 9; i++) {
            ms._ptTypeList[i] = SiOPMTable.PT_PSG;
        }
        for (i = 9; i < 11; i++) {
            ms._ptTypeList[i] = SiOPMTable.PT_GB_NOISE;
        }
        ms._initVoiceIndex = 1;
        ms._voiceIndexTable[0] = 4;
        ms._voiceIndexTable[1] = 4;
        ms._voiceIndexTable[2] = 8;
        ms._voiceIndexTable[3] = 9;
        // VRC6 setting
        ms = channelModuleSetting[MT_VRC6];
        ms._pgTypeList[8] = SiOPMTable.PG_SAW_VC6;
        ms._ptTypeList[8] = SiOPMTable.PT_PSG;
        ms._initVoiceIndex = 1;
        ms._voiceIndexTable[0] = 7;
        ms._voiceIndexTable[1] = 7;
        ms._voiceIndexTable[2] = 8;
        // SID setting
        ms = channelModuleSetting[MT_SID];
        ms._pgTypeList[8] = SiOPMTable.PG_TRIANGLE;
        ms._pgTypeList[9] = SiOPMTable.PG_SAW_UP;
        ms._pgTypeList[10] = SiOPMTable.PG_SAW_VC6;
        ms._pgTypeList[11] = SiOPMTable.PG_NOISE_PULSE;
        for (i = 0; i < 11; i++) {
            ms._ptTypeList[i] = SiOPMTable.PT_PSG;
        }
        ms._ptTypeList[11] = SiOPMTable.PT_OPM_NOISE;
        ms._initVoiceIndex = 1;
        ms._voiceIndexTable[0] = 7;
        ms._voiceIndexTable[1] = 7;
        ms._voiceIndexTable[2] = 7;
        // FM setting
        channelModuleSetting[MT_FM]._selectToneType = SiMMLChannelSetting.SELECT_TONE_FM;
        channelModuleSetting[MT_FM]._isSuitableForFMVoice = false;
        // PCM setting
        channelModuleSetting[MT_PCM]._channelType = SiOPMChannelManager.CT_CHANNEL_PCM;
        channelModuleSetting[MT_PCM]._isSuitableForFMVoice = false;
        // Sampler
        //channelModuleSetting[MT_SAMPLE]._selectToneType = SiMMLChannelSetting.SELECT_TONE_NOP;
        channelModuleSetting[MT_SAMPLE]._channelType = SiOPMChannelManager.CT_CHANNEL_SAMPLER;
        channelModuleSetting[MT_SAMPLE]._isSuitableForFMVoice = false;
        // Karplus strong
        channelModuleSetting[MT_KS]._channelType = SiOPMChannelManager.CT_CHANNEL_KS;
        channelModuleSetting[MT_KS]._isSuitableForFMVoice = false;


        // simulators setting
        simulators = new SiMMLSimulatorBase[MT_MAX];
        simulators[MT_PSG] = new SiMMLSimulatorPSG();          // PSG(DCSG)
        simulators[MT_APU] = new SiMMLSimulatorAPU();          // FC pAPU
        simulators[MT_NOISE] = new SiMMLSimulatorNoise();        // noise wave
        simulators[MT_MA3] = new SiMMLSimulatorMA3WaveTable(); // MA3 wave form
        simulators[MT_CUSTOM] = new SiMMLSimulatorWT();           // SCC / custom wave table
        simulators[MT_ALL] = new SiMMLSimulatorSiOPM();        // all pgTypes
        simulators[MT_FM] = new SiMMLSimulatorFMSiOPM();      // FM sound module
        simulators[MT_PCM] = new SiMMLSimulatorPCM();          // PCM
        simulators[MT_PULSE] = new SiMMLSimulatorPulse();        // pulse wave
        simulators[MT_RAMP] = new SiMMLSimulatorRamp();         // ramp wave
        simulators[MT_SAMPLE] = new SiMMLSimulatorSampler();      // sampler
        simulators[MT_KS] = new SiMMLSimulatorKS();           // karplus strong
        simulators[MT_GB] = new SiMMLSimulatorGB();           // gameboy
        simulators[MT_VRC6] = new SiMMLSimulatorVRC6();         // vrc6
        simulators[MT_SID] = new SiMMLSimulatorSID();          // sid
        simulators[MT_FM_OPM] = new SiMMLSimulatorFMOPM();        // YM2151
        simulators[MT_FM_OPN] = new SiMMLSimulatorFMOPN();        // YM2203
        simulators[MT_FM_OPNA] = new SiMMLSimulatorFMOPNA();       // YM2608
        simulators[MT_FM_OPLL] = new SiMMLSimulatorFMOPLL();       // YM2413
        simulators[MT_FM_OPL3] = new SiMMLSimulatorFMOPL3();       // YM3812
        simulators[MT_FM_MA3] = new SiMMLSimulatorFMMA3();        // YMU762

        // setup OPLL default voices
        presetVoiceYM2413 = _setupYM2413DefaultVoices(presetRegisterYM2413);
        presetVoiceVRC7 = _setupYM2413DefaultVoices(presetRegisterVRC7);
        presetVoiceVRC7Drums = _setupYM2413DefaultVoices(presetRegisterVRC7Drums);

        // tables
        _masterEnvelops = new SiMMLEnvelopTable[ENV_TABLE_MAX];
        for (i = 0; i < ENV_TABLE_MAX; i++) _masterEnvelops[i] = null;
        _masterVoices = new SiMMLVoice[VOICE_MAX];
        for (i = 0; i < VOICE_MAX; i++) _masterVoices[i] = null;

        if (tss_s2ar == null) {
            int[] i_tss_s2ar = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            tss_s2ar = new String[i_tss_s2ar.length];
            for (j = 0; j < i_tss_s2ar.length; j++) tss_s2ar[j] = String.valueOf(i_tss_s2ar[j]);
            
            int[] i_tss_s2dr = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            tss_s2dr = new String[i_tss_s2dr.length];
            for (j = 0; j < i_tss_s2dr.length; j++) tss_s2dr[j] = String.valueOf(i_tss_s2dr[j]);
            
            int[] i_tss_s2sr = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            tss_s2sr = new String[i_tss_s2sr.length];
            for (j = 0; j < i_tss_s2sr.length; j++) tss_s2sr[j] = String.valueOf(i_tss_s2sr[j]);
            
            int[] i_tss_s2rr = new int[] {15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15};
            tss_s2rr = new String[i_tss_s2rr.length];
            for (j = 0; j < i_tss_s2rr.length; j++) tss_s2rr[j] = String.valueOf(i_tss_s2rr[j]);
        }
    }

    int[] _logTable(int start, int step, int v0, int v255) {
        int[] vector = new int[256];
        int imax, j, t, dt, i;

        t = start << 16;
        dt = step << 16;
        for (i = 1, j = 1; j <= 8; j++) {
            for (imax = 1 << j; i < imax; i++) {
                vector[i] = t >> 16;
                t += dt;
            }
            dt >>= 1;
        }
        vector[0] = v0;
        vector[255] = v255;

        return vector;
    }

    private List<SiMMLVoice> _setupYM2413DefaultVoices(int[] registerMap) {
        List<SiMMLVoice> voices = new ArrayList<SiMMLVoice>(registerMap.length >> 1);
        int i, i2;
        for (i = i2 = 0; i < registerMap.length / 2; i++, i2 += 2) {
            voices.add(_dumpYM2413Register(new SiMMLVoice(), registerMap[i2], registerMap[i2 + 1]));
        }
        return voices;
    }

    private SiMMLVoice _dumpYM2413Register(SiMMLVoice voice, int u0, int u1) {
        int i;
        SiOPMChannelParam param = voice.channelParam;
        SiOPMOperatorParam opp0 = param.operatorParam[0];
        SiOPMOperatorParam opp1 = param.operatorParam[1];
        voice.moduleType = 6;
        voice.channelNum = 0;
        voice.toneNum = -1;
        voice.chipType = "OPL";
        param.fratio = 133;
        param.opeCount = 2;
        param.alg = 0;

        opp0.ams = ((u0 >> 31) & 1) << 1;  //(dump[0]>>7)&1 ;
        opp1.ams = ((u0 >> 23) & 1) << 1;  //(dump[1]>>7)&1 ;
        //opp0.PM = (u0>>30)&1;  //(dump[0]>>6)&1 ;
        //opp1.PM = (u0>>22)&1;  //(dump[1]>>6)&1 ;
        opp0.ksr = ((u0 >> 28) & 1) << 1;  //(dump[0]>>4)&1 ;
        opp1.ksr = ((u0 >> 20) & 1) << 1;  //(dump[1]>>4)&1 ;
        i = (u0 >> 24) & 15; //(dump[0])&15 ;
        opp0.setMul((i == 11 || i == 13) ? (i - 1) : (i == 14) ? (i + 1) : i);
        i = (u0 >> 16) & 15; //(dump[1])&15 ;
        opp1.setMul((i == 11 || i == 13) ? (i - 1) : (i == 14) ? (i + 1) : i);
        opp0.ksl = (u0 >> 14) & 3;  //(dump[2]>>6)&3 ;
        opp1.ksl = (u0 >> 6) & 3;  //(dump[3]>>6)&3 ;
        param.fb = (u0 >> 0) & 7;   //(dump[3])&7 ;
        opp0.setPGType(SiOPMTable.PG_MA3_WAVE + ((u0 >> 3) & 1));  //(dump[3]>>3)&1 ;
        opp1.setPGType(SiOPMTable.PG_MA3_WAVE + ((u0 >> 4) & 1));  //(dump[3]>>4)&1 ;
        opp0.ar = ((u1 >> 28) & 15) << 2; //(dump[4]>>4)&15 ;
        opp1.ar = ((u1 >> 20) & 15) << 2; //(dump[5]>>4)&15 ;
        opp0.dr = ((u1 >> 24) & 15) << 2; //(dump[4])&15 ;
        opp1.dr = ((u1 >> 16) & 15) << 2; //(dump[5])&15 ;
        opp0.sl = (u1 >> 12) & 15; //(dump[6]>>4)&15 ;
        opp1.sl = (u1 >> 4) & 15; //(dump[7]>>4)&15 ;
        opp0.rr = ((u1 >> 8) & 15) << 2; //(dump[6])&15 ;
        opp1.rr = ((u1 >> 0) & 15) << 2; //(dump[7])&15 ;
        opp0.sr = (((u0 >> 29) & 1) != 0) ? 0 : opp0.rr;  //EG=(dump[0]>>5)&1 ;
        opp1.sr = (((u0 >> 21) & 1) != 0) ? 0 : opp1.rr;  //EG=(dump[1]>>5)&1 ;
        opp0.tl = (u0 >> 8) & 63; //(dump[2])&63 ;
        opp1.tl = 0;

        return voice;
    }


    // operations
    //

    /** reset all user tables */
    public void resetAllUserTables() {
        int i;
        for (i = 0; i < ENV_TABLE_MAX; i++) {
            if (_masterEnvelops[i] != null) {
                _masterEnvelops[i].free();
                _masterEnvelops[i] = null;
            }
        }
        for (i = 0; i < VOICE_MAX; i++) {
            _masterVoices[i] = null;
        }
    }


    /**
     * Register envelop table.
     *
     * @param index table number refered by &#64;&#64;,na,np,nt,nf,_&#64;&#64;,_na,_np,_nt and _nf.
     * @param table envelop table.
     */
    public static void registerMasterEnvelopTable(int index, SiMMLEnvelopTable table) {
        if (index >= 0 && index < ENV_TABLE_MAX) getInstance()._masterEnvelops[index] = table;
    }

    /**
     * Register voice data.
     *
     * @param index voice parameter number refered by %6.
     * @param voice voice.
     */
    public static void registerMasterVoice(int index, SiMMLVoice voice) {
        if (index >= 0 && index < VOICE_MAX) getInstance()._masterVoices[index] = voice;
    }

    /**
     * Get Envelop table.
     *
     * @param index table number.
     */
    public SiMMLEnvelopTable getEnvelopTable(int index) {
        if (index < 0 || index >= ENV_TABLE_MAX) return null;
        if (_stencilEnvelops != null && _stencilEnvelops[index] != null) return _stencilEnvelops[index];
        return _masterEnvelops[index];
    }

    /**
     * Get voice data.
     *
     * @param index voice parameter number.
     */
    public SiMMLVoice getSiMMLVoice(int index) {
        if (index < 0 || index >= VOICE_MAX) return null;
        if (_stencilVoices != null && _stencilVoices[index] != null) return _stencilVoices[index];
        return _masterVoices[index];
    }

    /**
     * get 0th operators pgType number from moduleType, channelNum and toneNum.
     *
     * @param moduleType Channel module type
     * @param channelNum Channel number. For %2-11, this value instanceof ((1st) same) argument of '_&#64;'.
     * @param toneNum    Tone number. Ussualy, this argument instanceof used only in %0;PSG and %1;APU.
     * @return pgType value, or -1 when moduleType == 6(FM) or 7(PCM).
     */
    public static int getPGType(int moduleType, int channelNum, int toneNum) {
        SiMMLChannelSetting ms = getInstance().channelModuleSetting[moduleType];

        if (ms._selectToneType == org.si.sion.sequencer.SiMMLChannelSetting.SELECT_TONE_NORMAL) {
            if (toneNum == -1 && channelNum >= 0 && channelNum < ms._voiceIndexTable.length)
                toneNum = ms._voiceIndexTable[channelNum];
            if (toneNum < 0 || toneNum >= ms._pgTypeList.length) toneNum = ms._initVoiceIndex;
            return ms._pgTypeList[toneNum];
        }

        return -1;
    }

    /**
     * get 0th operators pgType number from moduleType, channelNum and toneNum.
     *
     * @param moduleType Channel module type
     * @return pgType value, or -1 when moduleType == 6(FM) or 7(PCM).
     */
    public static boolean isSuitableForFMVoice(int moduleType) {
        return getInstance().channelModuleSetting[moduleType]._isSuitableForFMVoice;
    }
}
