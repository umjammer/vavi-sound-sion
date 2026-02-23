//
// class for SiOPM tables
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.sion.sequencer.SiMMLVoice;


/** SiOPM table */
public class SiOPMTable {

    // constants
    //
    public static final int ENV_BITS = 10;   // Envelop output a bit size
    public static final int ENV_TIMER_BITS = 24;   // Envelop timer resolution bit size
    public static final int SAMPLING_TABLE_BITS = 10;   // sine wave table entries = 2 ^ SAMPLING_TABLE_BITS = 1024
    public static final int HALF_TONE_BITS = 6;    // halftone resolution    = 2 ^ HALF_TONE_BITS      = 64
    public static final int NOTE_BITS = 7;    // max note value          = 2 ^ NOTE_BITS           = 128
    public static final int NOISE_TABLE_BITS = 15;   // 32k noise
    public static final int LOG_TABLE_RESOLUTION = 256;  // log table resolution    = LOG_TABLE_RESOLUTION for every 1/2 scaling.
    public static final int LOG_VOLUME_BITS = 13;   // _logTable[0] = 2^13 at maximum
    public static final int LOG_TABLE_MAX_BITS = 16;   // _logTable entries
    public static final int FIXED_BITS = 16;   // internal fixed point 16.16
    public static final int PCM_BITS = 20;   // maximum PCM sample length = 2 ^ PCM_BITS = 1048576
    public static final int LFO_FIXED_BITS = 20;   // fixed point for lfo timer
    public static final int CLOCK_RATIO_BITS = 10;   // bits for clock/64/[sampling rate]
    public static final double NOISE_WAVE_OUTPUT = 1;     // -2044 < [noise amplitude] < 2040 -> NOISE_WAVE_OUTPUT=0.25
    public static final double SQUARE_WAVE_OUTPUT = 1;     //
    public static final double OUTPUT_MAX = 0.5;   // maximum output

    public static final int ENV_LSHIFT = ENV_BITS - 7;                     // Shift number from input tl [0,127] to internal value [0,ENV_BOTTOM].
    public static final int ENV_TIMER_INITIAL = (2047 * 3) << CLOCK_RATIO_BITS;   // envelop timer initial value
    public static final int LFO_TIMER_INITIAL = 1 << SiOPMTable.LFO_FIXED_BITS;   // lfo timer initial value
    public static final int PHASE_BITS = SAMPLING_TABLE_BITS + FIXED_BITS; // internal phase is expressed by 10.16 fixed.
    public static final int PHASE_MAX = 1 << PHASE_BITS;
    public static final int PHASE_FILTER = PHASE_MAX - 1;
    public static final int PHASE_SIGN_RSHIFT = PHASE_BITS - 1;
    public static final int SAMPLING_TABLE_SIZE = 1 << SAMPLING_TABLE_BITS;
    public static final int NOISE_TABLE_SIZE = 1 << NOISE_TABLE_BITS;
    public static final int PITCH_TABLE_SIZE = 1 << (HALF_TONE_BITS + NOTE_BITS);
    public static final int NOTE_TABLE_SIZE = 1 << NOTE_BITS;
    public static final int HALF_TONE_RESOLUTION = 1 << HALF_TONE_BITS;
    public static final int LOG_TABLE_SIZE = LOG_TABLE_MAX_BITS * LOG_TABLE_RESOLUTION * 2;   // *2 posi&nega
    public static final int LFO_TABLE_SIZE = 256;                                             // FIXED VALUE !!
    public static final int KEY_CODE_TABLE_SIZE = 128;                                             // FIXED VALUE !!
    public static final int LOG_TABLE_BOTTOM = LOG_VOLUME_BITS * LOG_TABLE_RESOLUTION * 2;      // bottom value of log table = 6656
    public static final int ENV_BOTTOM = (LOG_VOLUME_BITS * LOG_TABLE_RESOLUTION) >> 2;   // minimum gain of envelop = 832
    public static final int ENV_TOP = ENV_BOTTOM - (1 << ENV_BITS);                      // maximum gain of envelop = -192
    public static final int ENV_BOTTOM_SSGEC = 1 << (ENV_BITS - 3);                                 // minimum gain of ssgec envelop = 128

    // pitch table type
    public static final int PT_OPM = 0;
    public static final int PT_PCM = 1;
    public static final int PT_PSG = 2;
    public static final int PT_OPM_NOISE = 3;
    public static final int PT_PSG_NOISE = 4;
    public static final int PT_APU_NOISE = 5;
    public static final int PT_GB_NOISE = 6;
//    public static final int PT_APU_DPCM = 7;
    public static final int PT_MAX = 7;

    // pulse generator type (0-511)
    public static final int PG_SINE = 0;     // sine wave
    public static final int PG_SAW_UP = 1;     // upward saw wave
    public static final int PG_SAW_DOWN = 2;     // downward saw wave
    public static final int PG_TRIANGLE_FC = 3;     // triangle wave quantized by 4bit
    public static final int PG_TRIANGLE = 4;     // triangle wave
    public static final int PG_SQUARE = 5;     // square wave
    public static final int PG_NOISE = 6;     // 32k white noise
    public static final int PG_KNMBSMM = 7;     // knmbsmm wave
    public static final int PG_SYNC_LOW = 8;     // pseudo sync (low freq.)
    public static final int PG_SYNC_HIGH = 9;     // pseudo sync (high freq.)
    public static final int PG_OFFSET = 10;    // offset
    public static final int PG_SAW_VC6 = 11;    // vc6 saw (32 samples saw)
    // ( 12-  15) reserved
    // ( 11-  15) reserved
    public static final int PG_NOISE_WHITE = 16;    // 16k white noise
    public static final int PG_NOISE_PULSE = 17;    // 16k pulse noise
    public static final int PG_NOISE_SHORT = 18;    // fc short noise
    public static final int PG_NOISE_HIPAS = 19;    // high pass noise
    public static final int PG_NOISE_PINK = 20;    // pink noise
    public static final int PG_NOISE_GB_SHORT = 21; // gb short noise
    // ( 22-  23) reserved
    public static final int PG_PC_NZ_16BIT = 24;    // pitch controlable periodic noise
    public static final int PG_PC_NZ_SHORT = 25;    // pitch controlable 93byte noise
    public static final int PG_PC_NZ_OPM = 26;    // pulse noise with OPM noise table
    // ( 27-  31) reserved
    public static final int PG_MA3_WAVE = 32;    // ( 32-  63) MA3 waveforms.  PG_MA3_WAVE+[0,31]
    public static final int PG_PULSE = 64;    // ( 64-  79) square pulse wave. PG_PULSE+[0,15]
    public static final int PG_PULSE_SPIKE = 80;    // ( 80-  95) square pulse wave. PG_PULSE_SPIKE+[0,15]
    // ( 96- 127) reserved
    public static final int PG_RAMP = 128;   // (128- 255) ramp wave. PG_RAMP+[0,127]
    public static final int PG_CUSTOM = 256;   // (256- 383) custom wave table. PG_CUSTOM+[0,127]
    public static final int PG_PCM = 384;   // (384- 511) pcm data. PG_PCM+[0,128]
    public static final int PG_USER_CUSTOM = -1;    // -1 user registered custom wave table
    public static final int PG_USER_PCM = -2;    // -2 user registered pcm data

    public static final int DEFAULT_PG_MAX = 256;   // max value of pgType = 255
    public static final int PG_FILTER = 511;   // pg number loops between 0 and 511

    public static final int WAVE_TABLE_MAX = 128;                // custom wave table max.
    public static final int PCM_DATA_MAX = 128;                // pcm data max.
    public static final int SAMPLER_TABLE_MAX = 4;                  // sampler table max
    public static final int SAMPLER_DATA_MAX = NOTE_TABLE_SIZE;    // sampler data max

    public static final int VM_LINEAR = 0;  // linear scale
    public static final int VM_DR96DB = 1;  // log scale; dynamic range = 96dB
    public static final int VM_DR64DB = 2;  // log scale; dynamic range = 64dB
    public static final int VM_DR48DB = 3;  // log scale; dynamic range = 48dB
    public static final int VM_DR32DB = 4;  // log scale; dynamic range = 32dB
    public static final int VM_MAX = 5;

    // lfo wave type
    public static final int LFO_WAVE_SAW = 0;
    public static final int LFO_WAVE_SQUARE = 1;
    public static final int LFO_WAVE_TRIANGLE = 2;
    public static final int LFO_WAVE_NOISE = 3;
    public static final int LFO_WAVE_MAX = 8;

    // tables
    //

    /** EG:increment table. This table instanceof based on MAME's opm emulation. */
    public static final int[][] eg_incTables = {    // eg_incTables[19][8]
            /*cycle:              0 1  2 3  4 5  6 7  */
            /* 0*/  {0, 1, 0, 1, 0, 1, 0, 1},  /* rates 00..11 0 (increment by 0 or 1) */
            /* 1*/  {0, 1, 0, 1, 1, 1, 0, 1},  /* rates 00..11 1 */
            /* 2*/  {0, 1, 1, 1, 0, 1, 1, 1},  /* rates 00..11 2 */
            /* 3*/  {0, 1, 1, 1, 1, 1, 1, 1},  /* rates 00..11 3 */
            /* 4*/  {1, 1, 1, 1, 1, 1, 1, 1},  /* rate 12 0 (increment by 1) */
            /* 5*/  {1, 1, 1, 2, 1, 1, 1, 2},  /* rate 12 1 */
            /* 6*/  {1, 2, 1, 2, 1, 2, 1, 2},  /* rate 12 2 */
            /* 7*/  {1, 2, 2, 2, 1, 2, 2, 2},  /* rate 12 3 */
            /* 8*/  {2, 2, 2, 2, 2, 2, 2, 2},  /* rate 13 0 (increment by 2) */
            /* 9*/  {2, 2, 2, 4, 2, 2, 2, 4},  /* rate 13 1 */
            /*10*/  {2, 4, 2, 4, 2, 4, 2, 4},  /* rate 13 2 */
            /*11*/  {2, 4, 4, 4, 2, 4, 4, 4},  /* rate 13 3 */
            /*12*/  {4, 4, 4, 4, 4, 4, 4, 4},  /* rate 14 0 (increment by 4) */
            /*13*/  {4, 4, 4, 8, 4, 4, 4, 8},  /* rate 14 1 */
            /*14*/  {4, 8, 4, 8, 4, 8, 4, 8},  /* rate 14 2 */
            /*15*/  {4, 8, 8, 8, 4, 8, 8, 8},  /* rate 14 3 */
            /*16*/  {8, 8, 8, 8, 8, 8, 8, 8},  /* rates 15 0, 15 1, 15 2, 15 3 (increment by 8) */
            /*17*/  {0, 0, 0, 0, 0, 0, 0, 0}   /* infinity rates for attack and decay(s) */
    };
    /** EG:increment table for attack. This shortcut instanceof based on fmgen (shift=0 means x0). */
    public static final int[][] eg_incTablesAtt = {
            /*cycle:              0 1  2 3  4 5  6 7  */
            /* 0*/  {0, 4, 0, 4, 0, 4, 0, 4},  /* rates 00..11 0 (increment by 0 or 1) */
            /* 1*/  {0, 4, 0, 4, 4, 4, 0, 4},  /* rates 00..11 1 */
            /* 2*/  {0, 4, 4, 4, 0, 4, 4, 4},  /* rates 00..11 2 */
            /* 3*/  {0, 4, 4, 4, 4, 4, 4, 4},  /* rates 00..11 3 */
            /* 4*/  {4, 4, 4, 4, 4, 4, 4, 4},  /* rate 12 0 (increment by 1) */
            /* 5*/  {4, 4, 4, 3, 4, 4, 4, 3},  /* rate 12 1 */
            /* 6*/  {4, 3, 4, 3, 4, 3, 4, 3},  /* rate 12 2 */
            /* 7*/  {4, 3, 3, 3, 4, 3, 3, 3},  /* rate 12 3 */
            /* 8*/  {3, 3, 3, 3, 3, 3, 3, 3},  /* rate 13 0 (increment by 2) */
            /* 9*/  {3, 3, 3, 2, 3, 3, 3, 2},  /* rate 13 1 */
            /*10*/  {3, 2, 3, 2, 3, 2, 3, 2},  /* rate 13 2 */
            /*11*/  {3, 2, 2, 2, 3, 2, 2, 2},  /* rate 13 3 */
            /*12*/  {2, 2, 2, 2, 2, 2, 2, 2},  /* rate 14 0 (increment by 4) */
            /*13*/  {2, 2, 2, 1, 2, 2, 2, 1},  /* rate 14 1 */
            /*14*/  {2, 8, 2, 1, 2, 1, 2, 1},  /* rate 14 2 */
            /*15*/  {2, 1, 1, 1, 2, 1, 1, 1},  /* rate 14 3 */
            /*16*/  {1, 1, 1, 1, 1, 1, 1, 1},  /* rates 15 0, 15 1, 15 2, 15 3 (increment by 8) */
            /*17*/  {0, 0, 0, 0, 0, 0, 0, 0}   /* infinity rates for attack and decay(s) */
    };
    /** EG:table selector. */
    public int[] eg_tableSelector = null;
    /** EG:table to calculate eg_level. */
    public int[][] eg_levelTables = null;
    /** EG:table from sgg_type to eg_levelTables index. */
    public int[][][] eg_ssgTableIndex = null;
    /** EG:timer step. */
    public int[] eg_timerSteps = null;
    /** EG:sl table from 15 to 1024. */
    public int[] eg_slTable = null;
    /** EG:tl table from volume to tl. */
    public int[][] eg_tlTables = null;
    /** EG:tl table from linear volume. */
    public int[] eg_tlTableLine = null;
    /** EG:tl table of tl based. */
    public int[] eg_tlTable96dB = null;
    /** EG:tl table of fmp7 based. */
    public int[] eg_tlTable64dB = null;
    /** EG:tl table from psg volume. */
    public int[] eg_tlTable48dB = null;
    /** EG:tl table from N88 basic v command. */
    public int[] eg_tlTable32dB = null;
    /** EG:tl table from linear-volume to tl. */
    public int[] eg_lv2tlTable = null;

    /** LFO:timer step. */
    public int[] lfo_timerSteps = null;
    /** LFO:lfo modulation table */
    public int[][] lfo_waveTables = null;
    /** LFO:lfo modulation table for chorus */
    public int[] lfo_chorusTables = null;

    /** FILTER: cutoff */
    public double[] filter_cutoffTable = null;
    /** FILTER: resonance */
    public double[] filter_feedbackTable = null;
    /** FILTER: envlop rate */
    public int[] filter_eg_rate = null;

    /** PG:pitch table. */
    public int[][] pitchTable = null;
    /** PG:phase step shift filter. */
    public int[] phaseStepShiftFilter = null;
    /** PG:log table. */
    public int[] logTable = null;
    /** PG:MIDI note number to FM key code. */
    public int[] nnToKC = null;
    /** PG:pitch wave length (in samples) table. */
    public double[] pitchWaveLength = null;
    /** PG:Wave tables without any waves. */
    public SiOPMWaveTable noWaveTable;
    public SiOPMWaveTable noWaveTableOPM;

    /** PG:Sound reference table */
    public Object soundReference = null;
    /** PG:Wave tables */
    public SiOPMWaveTable[] waveTables = null;
    /** PG:Sampler table */
    public SiOPMWaveSamplerTable[] samplerTables = null;
    /** PG:Custom wave tables */
    private SiOPMWaveTable[] _customWaveTables = null;
    /** PG:Overriding custom wave tables */
    public SiOPMWaveTable[] _stencilCustomWaveTables = null;
    /** PG:PCM voice */
    private SiMMLVoice[] _pcmVoices = null;
    /** PG:PCM voice */
    public SiMMLVoice[] _stencilPCMVoices = null;

    /** Table for dt1 (from fmgen.cpp). */
    public int[][] dt1Table = null;
    /** Table for dt2 (from MAME's opm source). */
    public int[] dt2Table = new int[] {0, 384, 500, 608};

    /** Flags of final oscillator */
    public static final int[][] final_oscilator_flags = {
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {2, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {4, 4, 5, 6, 6, 7, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {8, 8, 8, 8, 10, 14, 14, 15, 9, 13, 8, 9, 9, 0, 0, 0}
    };

    /** int->Number ratio on pulse data */
    public double i2n;
    /** Panning volume table. */
    public double[] panTable = null;

    /** sampling rate */
    public int rate;
    /** fm clock */
    public int clock;
    /** psg clock */
    public double psg_clock;
    /** (clock/64/sampling_rate)&lt;&lt;CLOCK_RATIO_BITS */
    public int clock_ratio;
    /** 44100Hz=0, 22050Hz=1 */
    public int sampleRatePitchShift;

    // static public instance
    //

    /** internal instance, you can access this after creating SiONDriver. */
    public static SiOPMTable _instance = null;

    /** static sigleton instance */
    public static SiOPMTable getInstance() {
        return (_instance != null) ? _instance : (_instance = new SiOPMTable(3580000, 1789772.5, 44100));
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param clock FM module's clock.
     * @param rate  Sampling rate of wave data
     */
    SiOPMTable(int clock, double psg_clock, int rate) {
        _setConstants(clock, psg_clock, rate);
        _createEGTables();
        _createPGTables();
        _createWaveSamples();
        _createLFOTables();
        _createFilterTables();
    }

    // calculate constants
    //
    private void _setConstants(int clock, double psg_clock, int rate) {
        this.clock = clock;
        this.psg_clock = psg_clock;
        this.rate = rate;
        sampleRatePitchShift = (rate == 44100) ? 0 : (rate == 22050) ? 1 : -1;
        if (sampleRatePitchShift == -1)
            throw new Error("SiOPMTable error : Sampling rate (" + rate + ") is not supported.");
        clock_ratio = ((clock / 64) << CLOCK_RATIO_BITS) / rate;

        // int->Number ratio on pulse data
        i2n = OUTPUT_MAX / (double) (1 << LOG_VOLUME_BITS);
    }

    // calculate EG tables
    //
    private void _createEGTables() {
        int i, j, imax, imax2;
        Object[] table;

        // 128 = 64rates + 32ks-rates + 32dummies for dr,sr=0
        eg_timerSteps = new int[128];
        eg_tableSelector = new int[128];

        i = 0;
        for (; i < 44; i++) {                // rate = 0-43
            eg_timerSteps[i] = (1 << (i >> 2)) * clock_ratio;
            eg_tableSelector[i] = (i & 3);
        }
        for (; i < 48; i++) {                // rate = 44-47
            eg_timerSteps[i] = 2047 * clock_ratio;
            eg_tableSelector[i] = (i & 3);
        }
        for (; i < 60; i++) {                // rate = 48-59
            eg_timerSteps[i] = 2047 * clock_ratio;
            eg_tableSelector[i] = i - 44;
        }
        for (; i < 96; i++) {                // rate = 60-95 (rate=60-95 are same as rate=63(maximum))
            eg_timerSteps[i] = 2047 * clock_ratio;
            eg_tableSelector[i] = 16;
        }
        for (; i < 128; i++) {                // rate = 96-127 (dummies for ar,dr,sr=0)
            eg_timerSteps[i] = 0;
            eg_tableSelector[i] = 17;
        }

        // table for ssgenv
        imax = (1 << ENV_BITS);
        imax2 = imax >> 2;
        eg_levelTables = new int[7][];
        for (i = 0; i < 7; i++) {
            eg_levelTables[i] = new int[imax];
        }
        for (i = 0; i < imax2; i++) {
            eg_levelTables[0][i] = i;           // normal table
            eg_levelTables[1][i] = i << 2;        // ssg positive
            eg_levelTables[2][i] = 512 - (i << 2);  // ssg negative
            eg_levelTables[3][i] = 512 + (i << 2);  // ssg positive + offset
            eg_levelTables[4][i] = 1024 - (i << 2); // ssg negative + offset
            eg_levelTables[5][i] = 0;           // ssg fixed at max
            eg_levelTables[6][i] = 1024;        // ssg fixed at min
        }
        for (; i < imax; i++) {
            eg_levelTables[0][i] = i;           // normal table
            eg_levelTables[1][i] = 1024;        // ssg positive
            eg_levelTables[2][i] = 0;           // ssg negative
            eg_levelTables[3][i] = 1024;        // ssg positive + offset
            eg_levelTables[4][i] = 512;         // ssg negative + offset
            eg_levelTables[5][i] = 0;           // ssg fixed at max
            eg_levelTables[6][i] = 1024;        // ssg fixed at min
        }

        eg_ssgTableIndex = new int[10][][];
        //[[w/ ar], [w/o ar]]
        eg_ssgTableIndex[0] = new int[][] {{3, 3, 3}, {1, 3, 3}};   // ssgec=8
        eg_ssgTableIndex[1] = new int[][] {{1, 6, 6}, {1, 6, 6}};   // ssgec=9
        eg_ssgTableIndex[2] = new int[][] {{2, 1, 2}, {1, 2, 1}};   // ssgec=10
        eg_ssgTableIndex[3] = new int[][] {{2, 5, 5}, {1, 5, 5}};   // ssgec=11
        eg_ssgTableIndex[4] = new int[][] {{4, 4, 4}, {2, 4, 4}};   // ssgec=12
        eg_ssgTableIndex[5] = new int[][] {{2, 5, 5}, {2, 5, 5}};   // ssgec=13
        eg_ssgTableIndex[6] = new int[][] {{1, 2, 1}, {2, 1, 2}};   // ssgec=14
        eg_ssgTableIndex[7] = new int[][] {{1, 6, 6}, {2, 6, 6}};   // ssgec=15
        eg_ssgTableIndex[8] = new int[][] {{1, 1, 1}, {1, 1, 1}};   // ssgec=8+
        eg_ssgTableIndex[9] = new int[][] {{2, 2, 2}, {2, 2, 2}};   // ssgec=12+

        // sl(15) -> sl(1023)
        eg_slTable = new int[16];
        for (i = 0; i < 15; i++) {
            eg_slTable[i] = i << 5;
        }
        eg_slTable[15] = 31 << 5;

        //var n88vtable= [104, 40, 37, 34, 32, 29, 26, 24, 21, 18, 16, 13, 10, 8, 5, 2, 0];
        eg_tlTables = new int[VM_MAX][];
        eg_tlTables[VM_LINEAR] = eg_tlTableLine = new int[513];
        eg_tlTables[VM_DR96DB] = eg_tlTable96dB = new int[513];
        eg_tlTables[VM_DR64DB] = eg_tlTable64dB = new int[513];
        eg_tlTables[VM_DR48DB] = eg_tlTable48dB = new int[513];
        eg_tlTables[VM_DR32DB] = eg_tlTable32dB = new int[513];

        // v(0-256) -> total_level(832- 0). translate linear volume to log scale gain.
        eg_tlTableLine[0] = eg_tlTable96dB[0] = eg_tlTable48dB[0] = eg_tlTable32dB[0] = ENV_BOTTOM;
        for (i = 1; i < 257; i++) {
            // 0.00390625 = 1/256
            eg_tlTableLine[i] = calcLogTableIndex(i * 0.00390625) >> (LOG_VOLUME_BITS - ENV_BITS);
            eg_tlTable96dB[i] = (256 - i) * 4;                       //  (n/2)<<ENV_LSHIFT
            eg_tlTable64dB[i] = (int) ((256 - i) * 2.6666666666666667); // ((n/2)<<ENV_LSHIFT)*2/3
            eg_tlTable48dB[i] = (256 - i) * 2;                       // ((n/2)<<ENV_LSHIFT)*1/2
            eg_tlTable32dB[i] = (int) ((256 - i) * 1.333333333333333);  // ((n/2)<<ENV_LSHIFT)*1/3
        }
        // v(257-448) -> total_level(0 - -192). distortion.
        for (i = 1; i < 193; i++) {
            j = i + 256;
            eg_tlTableLine[j] = eg_tlTable96dB[j] = eg_tlTable64dB[j] = eg_tlTable48dB[j] = eg_tlTable32dB[j] = -i;
        }
        // v(449-512) -> total_level=-192. distortion.
        for (i = 1; i < 65; i++) {
            j = i + 448;
            eg_tlTableLine[j] = eg_tlTable96dB[j] = eg_tlTable64dB[j] = eg_tlTable48dB[j] = eg_tlTable32dB[j] = ENV_TOP;
        }

        // table from linear volume to tl
        eg_lv2tlTable = new int[129];
        for (i = 0; i < 129; i++) {
            eg_lv2tlTable[i] = calcLogTableIndex(i * 0.0078125) >> (LOG_VOLUME_BITS - ENV_BITS + ENV_LSHIFT);
        }

        // panning volume table
        panTable = new double[129];
        for (i = 0; i < 129; i++) {
            panTable[i] = Math.sin(i * 0.01227184630308513);  // 0.01227184630308513 = PI*0.5/128
        }
    }

    // calculate PG tables
    //
    private void _createPGTables() {
        // multipurpose
        int i, imax, j, jmax, iv = 0;
        double p, dp, n, v;
        int[] table;

        // MIDI Note Number -> Key Code table
        //
        nnToKC = new int[NOTE_TABLE_SIZE];
        for (i = 0, j = 0; j < NOTE_TABLE_SIZE; i++, j = i - (i >> 2)) {
            nnToKC[j] = (i < 16) ? i : (i >= KEY_CODE_TABLE_SIZE) ? (KEY_CODE_TABLE_SIZE - 1) : (i - 16);
        }

        // pitch table
        //
        imax = HALF_TONE_RESOLUTION * 12;   // 12=1octave
        jmax = PITCH_TABLE_SIZE;
        dp = 1d / imax;

        // wave length table
        pitchWaveLength = new double[PITCH_TABLE_SIZE];
        n = rate / 8.175798915643707;  // = 5393.968278209282@44.1kHz sampling count @ MIDI note number = 0
        for (i = 0, p = 0; i < imax; i++, p += dp) {
            v = Math.pow(2, -p) * n;
            for (j = i; j < jmax; j += imax) {
                pitchWaveLength[j] = v;
                v *= 0.5;
            }
        }

        // phase step tables
        pitchTable = new int[PT_MAX][];
        phaseStepShiftFilter = new int[PT_MAX];

        // OPM
        table = new int[PITCH_TABLE_SIZE];
        n = 8.175798915643707 * PHASE_MAX / rate;    // dphase @ MIDI note number = 0
        for (i = 0, p = 0; i < imax; i++, p += dp) {
            v = Math.pow(2, p) * n;
            for (j = i; j < jmax; j += imax) {
                table[j] = (int) (v);
                v *= 2;
            }
        }
        pitchTable[PT_OPM] = table;
        phaseStepShiftFilter[PT_OPM] = 0;

        // PCM
        // dphase = pitchTablePCM[pitchIndex] >> (table_size (= PHASE_BITS - waveTable.fixedBits))
        table = new int[PITCH_TABLE_SIZE];
        n = 0.01858136117191752 * PHASE_MAX;     // dphase @ MIDI note number = 0/ o0c=0.01858136117191752 -> o5a=1
        for (i = 0, p = 0; i < imax; i++, p += dp) {
            v = Math.pow(2, p) * n;
            for (j = i; j < jmax; j += imax) {
                table[j] = (int) (v);
                v *= 2;
            }
        }
        pitchTable[PT_PCM] = table;
        phaseStepShiftFilter[PT_PCM] = 0xffffffff;

        // PSG(table_size = 16)
        table = new int[PITCH_TABLE_SIZE];
        n = psg_clock * (PHASE_MAX >> 4) / rate;
        for (i = 0, p = 0; i < imax; i++, p += dp) {
            // 8.175798915643707 = [frequency @ MIDI note number = 0]
            // 130.8127826502993 = 8.175798915643707 * 16
            v = psg_clock / (Math.pow(2, p) * 130.8127826502993);
            for (j = i; j < jmax; j += imax) {
                // register value
                iv = (int) (v + 0.5);
                if (iv > 4096) iv = 4096;
                table[j] = (int) (n / iv);
                v *= 0.5;
            }
        }
        pitchTable[PT_PSG] = table;
        phaseStepShiftFilter[PT_PSG] = 0;
            
/*
            // APU DPCM period table
            int[] fc_df = {428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 85, 72, 54];
            imax  = 16<<HALF_TONE_BITS;
            table = new ArrayList<int>(imax, true);
            n = psg_clock/(rate*0.018581361171917529);
            for (i=0; i<16; i++) {
                iv = Math.log(n / fc_df[i]) * 1.4426950408889633 * PHASE_MAX / rate;
                for (j=0; j<HALF_TONE_RESOLUTION; j++) {
                    table[(i<<HALF_TONE_BITS)+j] = iv;
                }
            }
            pitchTable[PT_APU_DPCM] = table;
            phaseStepShiftFilter[PT_APU_DPCM] = 0xffffffff;
*/

        // Noise period tables.
        //
        // OPM noise period table.
        // noise_phase_shift = pitchTable[PT_OPM_NOISE][noiseFreq] >> (PHASE_BITS - waveTable.fixedBits).
        imax = 32 << HALF_TONE_BITS;
        table = new int[imax];
        n = PHASE_MAX * clock_ratio;    // clock_ratio = ((clock/64)/rate) << CLOCK_RATIO_BITS
        for (i = 0; i < 31; i++) {
            iv = ((int) (n / ((32 - i) * 0.5))) >> CLOCK_RATIO_BITS;
            for (j = 0; j < HALF_TONE_RESOLUTION; j++) {
                table[(i << HALF_TONE_BITS) + j] = iv;
            }
        }
        for (i = 31 << HALF_TONE_BITS; i < imax; i++) {
            table[i] = iv;
        }
        pitchTable[PT_OPM_NOISE] = table;
        phaseStepShiftFilter[PT_OPM_NOISE] = 0xffffffff;

        // PSG noise period table.
        table = new int[imax];
        // noise_phase_shift = ((1<<PHASE_BIT)  /  ((nf/(clock/16))[sec]  /  (1/44100)[sec])) >> (PHASE_BIT - waveTable.fixedBits)
        n = (double) (PHASE_MAX * clock) / (rate * 16);
        for (i = 0; i < 32; i++) {
            iv = (int) (n / i);
            for (j = 0; j < HALF_TONE_RESOLUTION; j++) {
                table[(i << HALF_TONE_BITS) + j] = iv;
            }
        }
        pitchTable[PT_PSG_NOISE] = table;
        phaseStepShiftFilter[PT_PSG_NOISE] = 0xffffffff;

        // APU noise period table
        int[] fc_nf = {4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068};
        imax = 16 << HALF_TONE_BITS;
        table = new int[imax];
        // noise_phase_shift = ((1<<PHASE_BIT)  /  ((nf/clock)[sec]  /  (1/44100)[sec])) >> (PHASE_BIT - waveTable.fixedBits)
        n = PHASE_MAX * psg_clock / rate;
        for (i = 0; i < 16; i++) {
            iv = (int) (n / fc_nf[i]);
            for (j = 0; j < HALF_TONE_RESOLUTION; j++) {
                table[(i << HALF_TONE_BITS) + j] = iv;
            }
        }
        pitchTable[PT_APU_NOISE] = table;
        phaseStepShiftFilter[PT_APU_NOISE] = 0xffffffff;

        // Game boy noise period
        int[] gb_nf = {
                2, 4, 8, 12, 16, 20, 24, 28, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512, 640, 768, 896, 1024, 1280, 1536, 1792,
                2048, 2560, 3072, 3584, 4096, 5120, 6144, 7168, 8192, 10240, 12288, 14336, 16384, 20480, 24576, 28672,
                32768, 40960, 49152, 57344, 65536, 81920, 98304, 114688, 131072, 163840, 196608, 229376, 262144, 327680, 393216, 458752
        };
        imax = 64 << HALF_TONE_BITS;
        table = new int[imax];
        // noise_phase_shift = ((1<<PHASE_BIT)  /  ((nf/clock)[sec]  /  (1/44100)[sec])) >> (PHASE_BIT - waveTable.fixedBits)
        n = (double) (PHASE_MAX * 1048576) / rate; // gb clock = 1048576
        for (i = 0; i < 64; i++) {
            iv = (int) (n / gb_nf[i]);
            for (j = 0; j < HALF_TONE_RESOLUTION; j++) {
                table[(i << HALF_TONE_BITS) + j] = iv;
            }
        }
        pitchTable[PT_GB_NOISE] = table;
        phaseStepShiftFilter[PT_GB_NOISE] = 0xffffffff;

        // dt1 table
        //
        // dt1 table from X68Sound.dll
        int[][] fmgen_dt1 = {  //[4][32]
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 8, 8},
                {1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 9, 10, 11, 12, 13, 14, 16, 16, 16, 16},
                {2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 20, 22, 22, 22, 22}
        };
        dt1Table = new int[8][];
        for (i = 0; i < 4; i++) {
            dt1Table[i] = new int[KEY_CODE_TABLE_SIZE];
            dt1Table[i + 4] = new int[KEY_CODE_TABLE_SIZE];
            for (j = 0; j < KEY_CODE_TABLE_SIZE; j++) {
                iv = (fmgen_dt1[i][j >> 2] * 64 * clock_ratio) >> CLOCK_RATIO_BITS;
                dt1Table[i][j] = iv;
                dt1Table[i + 4][j] = -iv;
            }
        }

        // log table
        //
        logTable = new int[LOG_TABLE_SIZE * 3];  // *3(2more zerofillarea) 16*256*2*3 = 24576
        i = (-ENV_TOP) << 3;                                 // start at -ENV_TOP
        imax = i + LOG_TABLE_RESOLUTION * 2;                    // *2(posi&nega)
        jmax = LOG_TABLE_SIZE;
        dp = 1. / LOG_TABLE_RESOLUTION;
        for (p = dp; i < imax; i += 2, p += dp) {
            v = Math.pow(2, LOG_VOLUME_BITS - p);  // v=2^(LOG_VOLUME_BITS-1/256) at maximum (i=2)
            for (j = i; j < jmax; j += LOG_TABLE_RESOLUTION * 2) {
                iv = (int) (v);
                logTable[j] = iv;
                logTable[j + 1] = -iv;
                v *= 0.5;
            }
        }
        // satulation area
        imax = (-ENV_TOP) << 3;
        iv = logTable[imax];
        for (i = 0; i < imax; i += 2) {
            logTable[i] = iv;
            logTable[i + 1] = -iv;
        }
        // zero fill area
        imax = logTable.length;
        for (i = jmax; i < imax; i++) {
            logTable[i] = 0;
        }
    }

    // calculate wave samples
    //
    private void _createWaveSamples() {
        // multipurpose
        int i, imax, imax2, imax3, imax4, j, jmax;
        double p, dp, n, v;
        int iv, prev = 0, o;
        int[] table1, table2;

        // allocate table list
        noWaveTable = SiOPMWaveTable.alloc(new int[] {calcLogTableIndex(1)}, PT_PCM);
        noWaveTableOPM = SiOPMWaveTable.alloc(new int[] {calcLogTableIndex(1)}, PT_OPM);
        waveTables = new SiOPMWaveTable[DEFAULT_PG_MAX];
        samplerTables = new SiOPMWaveSamplerTable[SAMPLER_TABLE_MAX];
        _customWaveTables = new SiOPMWaveTable[WAVE_TABLE_MAX];
        _pcmVoices = new SiMMLVoice[PCM_DATA_MAX];

        // clear all tables
        //
        for (i = 0; i < DEFAULT_PG_MAX; i++) waveTables[i] = noWaveTable;
        for (i = 0; i < WAVE_TABLE_MAX; i++) _customWaveTables[i] = null;
        for (i = 0; i < PCM_DATA_MAX; i++) _pcmVoices[i] = null;
        for (i = 0; i < SAMPLER_TABLE_MAX; i++) samplerTables[i] = (new SiOPMWaveSamplerTable()).clear(null);

        _stencilCustomWaveTables = null;
        _stencilPCMVoices = null;

        // sine wave table
        //--
        table1 = new int[SAMPLING_TABLE_SIZE];
        dp = 6.283185307179586 / SAMPLING_TABLE_SIZE;
        imax = SAMPLING_TABLE_SIZE >> 1;
        imax2 = SAMPLING_TABLE_SIZE;
        for (i = 0, p = dp * 0.5; i < imax; i++, p += dp) {
            iv = calcLogTableIndex(Math.sin(p));
            table1[i] = iv;   // positive index
            table1[i + imax] = iv + 1; // negative value index
        }
        waveTables[PG_SINE] = SiOPMWaveTable.alloc(table1, 0);

        // saw wave tables
        //--
        table1 = new int[SAMPLING_TABLE_SIZE];
        table2 = new int[SAMPLING_TABLE_SIZE];
        dp = 1. / imax;
        for (i = 0, p = dp * 0.5; i < imax; i++, p += dp) {
            iv = calcLogTableIndex(p);
            table1[i] = iv;   // positive
            table1[imax2 - i - 1] = iv + 1; // negative
            table2[imax - i - 1] = iv;   // positive
            table2[imax + i] = iv + 1; // negative
        }
        waveTables[PG_SAW_UP] = SiOPMWaveTable.alloc(table1, 0);
        waveTables[PG_SAW_DOWN] = SiOPMWaveTable.alloc(table2, 0);
        table1 = new int[32];
        for (i = 0, p = -0.96875; i < 32; i++, p += 0.0625) {
            table1[i] = calcLogTableIndex(p);
        }
        waveTables[PG_SAW_VC6] = SiOPMWaveTable.alloc(table1, 0);

        // triangle wave tables
        //--
        // triangle wave
        table1 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE >> 2;
        imax2 = SAMPLING_TABLE_SIZE >> 1;
        imax4 = SAMPLING_TABLE_SIZE;
        dp = 1 / imax;
        for (i = 0, p = dp * 0.5; i < imax; i++, p += dp) {
            iv = calcLogTableIndex(p);
            table1[i] = iv;   // positive index
            table1[imax2 - i - 1] = iv;   // positive index
            table1[imax2 + i] = iv + 1; // negative value index
            table1[imax4 - i - 1] = iv + 1; // negative value index
        }
        waveTables[PG_TRIANGLE] = SiOPMWaveTable.alloc(table1, 0);

        // fc triangle wave
        table1 = new int[32];
        for (i = 1, p = 0.125; i < 8; i++, p += 0.125) {
            iv = calcLogTableIndex(p);
            table1[i] = iv;
            table1[15 - i] = iv;
            table1[15 + i] = iv + 1;
            table1[32 - i] = iv + 1;
        }
        table1[0] = LOG_TABLE_BOTTOM;
        table1[15] = LOG_TABLE_BOTTOM;
        table1[23] = 3;
        table1[24] = 3;
        waveTables[PG_TRIANGLE_FC] = SiOPMWaveTable.alloc(table1, 0);

        // square wave tables
        //
        // 50% square wave
        iv = calcLogTableIndex(SQUARE_WAVE_OUTPUT);
        waveTables[PG_SQUARE] = SiOPMWaveTable.alloc(new int[] {iv, iv + 1}, 0);

        // pulse wave tables
        //
        // pulse wave
        // NOTE: The resolution of duty ratio is twice than pAPU. [pAPU pulse wave table] = waveTables[PG_PULSE+duty*2].
        table2 = waveTables[PG_SQUARE].wavelet;
        for (j = 0; j < 16; j++) {
            table1 = new int[16];
            for (i = 0; i < 16; i++) {
                table1[i] = (i < j) ? table2[0] : table2[1];
            }
            waveTables[PG_PULSE + j] = SiOPMWaveTable.alloc(table1, 0);
        }

        // spike pulse
        iv = calcLogTableIndex(0);
        for (j = 0; j < 16; j++) {
            table1 = new int[32];
            imax = j << 1;
            for (i = 0; i < imax; i++) {
                table1[i] = (i < j) ? table2[0] : table2[1];
            }
            for (; i < 32; i++) {
                table1[i] = iv;
            }
            waveTables[PG_PULSE_SPIKE + j] = SiOPMWaveTable.alloc(table1, 0);
        }

        // wave from konami bubble system
        //
        int[] wav = {-80, -112, -16, 96, 64, 16, 64, 96, 32, -16, 64, 112, 80, 0, 32, 48, -16, -96, 0, 80, 16, -64, -48, -16, -96, -128, -80, 0, -48, -112, -80, -32};
        table1 = new int[32];
        for (i = 0; i < 32; i++) {
            table1[i] = calcLogTableIndex(wav[i] / 128.);
        }
        waveTables[PG_KNMBSMM] = SiOPMWaveTable.alloc(table1, 0);

        // pseudo sync
        //
        table1 = new int[SAMPLING_TABLE_SIZE];
        table2 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE;
        dp = 1. / imax;
        for (i = 0, p = dp * 0.5; i < imax; i++, p += dp) {
            iv = calcLogTableIndex(p);
            table1[i] = iv + 1;   // negative
            table2[i] = iv;     // positive
        }
        waveTables[PG_SYNC_LOW] = SiOPMWaveTable.alloc(table1, 0);
        waveTables[PG_SYNC_HIGH] = SiOPMWaveTable.alloc(table2, 0);

        // noise tables
        //
        // white noise
        // pulse noise. NOTE: Dishonest impelementation. Details are shown in MAME or VirtuaNes source.
        table1 = new int[NOISE_TABLE_SIZE];
        table2 = new int[NOISE_TABLE_SIZE];
        imax = NOISE_TABLE_SIZE;
        iv = calcLogTableIndex(NOISE_WAVE_OUTPUT);
        n = NOISE_WAVE_OUTPUT / 32768;
        j = 1;                          // 15bit LFSR
        for (i = 0; i < imax; i++) {
            j = (((j << 13) ^ (j << 14)) & 0x4000) | (j >> 1);
            table1[i] = calcLogTableIndex((j & 0x7fff) * n * 2 - 1);
            table2[i] = (j & 1) != 0 ? iv : (iv + 1);
        }
        waveTables[PG_NOISE_WHITE] = SiOPMWaveTable.alloc(table1, PT_PCM);
        waveTables[PG_NOISE_PULSE] = SiOPMWaveTable.alloc(table2, PT_PCM);
        waveTables[PG_PC_NZ_OPM] = SiOPMWaveTable.alloc(table2, PT_OPM_NOISE);
        waveTables[PG_NOISE] = waveTables[PG_NOISE_WHITE];

        // fc short noise. NOTE: Dishonest impelementation. 93*11=1023 aprox.-> 1024.
        table1 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE;
        iv = calcLogTableIndex(NOISE_WAVE_OUTPUT);
        j = 1;                          // 15bit LFSR
        for (i = 0; i < imax; i++) {
            j = (((j << 8) ^ (j << 14)) & 0x4000) | (j >> 1);
            table1[i] = (j & 1) != 0 ? iv : (iv + 1);
        }
        waveTables[PG_NOISE_SHORT] = SiOPMWaveTable.alloc(table1, PT_PCM);

        // gb short noise. NOTE:
        table1 = new int[128];
        iv = calcLogTableIndex(NOISE_WAVE_OUTPUT);
        j = 0xffff;                      // 16bit LFSR
        o = 0;
        for (i = 0; i < 128; i++) {
            j += j + (((j >> 6) ^ (j >> 5)) & 1);
            o ^= j & 1;
            table1[i] = (o & 1) != 0 ? iv : (iv + 1);
        }
        waveTables[PG_NOISE_GB_SHORT] = SiOPMWaveTable.alloc(table1, PT_PCM);

        // periodic noise
        table1 = new int[16];
        table1[0] = calcLogTableIndex(SQUARE_WAVE_OUTPUT);
        for (i = 1; i < 16; i++) {
            table1[i] = LOG_TABLE_BOTTOM;
        }
        waveTables[PG_PC_NZ_16BIT] = SiOPMWaveTable.alloc(table1, 0);

        // high passed white noise
        table1 = new int[NOISE_TABLE_SIZE];
        table2 = waveTables[PG_NOISE_WHITE].wavelet;
        imax = NOISE_TABLE_SIZE;
        j = (-ENV_TOP) << 3;
        n = 16 / (double) (1 << LOG_VOLUME_BITS);
        p = 0.0625;
        v = (logTable[table2[0] + j] - logTable[table2[NOISE_TABLE_SIZE - 1] + j]) * p;
        table1[0] = calcLogTableIndex(v * n);
        for (i = 1; i < imax; i++) {
            imax2 = table2[i] + j;
            imax3 = table2[i - 1] + j;
            v = (v + logTable[imax2] - logTable[imax3]) * p;
            table1[i] = calcLogTableIndex(v * n);
        }
        waveTables[PG_NOISE_HIPAS] = SiOPMWaveTable.alloc(table1, PT_PCM);

        // pink noise
        double b0 = 0, b1 = 0, b2 = 0;
        table1 = new int[NOISE_TABLE_SIZE];
        table2 = waveTables[PG_NOISE_WHITE].wavelet;
        imax = NOISE_TABLE_SIZE;
        j = (-ENV_TOP) << 3;
        n = 0.125 / (double) (1 << LOG_VOLUME_BITS);
        for (i = 0; i < imax; i++) {
            imax2 = table2[i] + j;
            v = logTable[imax2];
            b0 = 0.99765 * b0 + v * 0.0990460;
            b1 = 0.96300 * b1 + v * 0.2965164;
            b2 = 0.57000 * b2 + v * 1.0526913;
            table1[i] = calcLogTableIndex((b0 + b1 + b2 + v * 0.1848) * n);
        }
        waveTables[PG_NOISE_PINK] = SiOPMWaveTable.alloc(table1, PT_PCM);

        // periodic noise
        table1 = new int[16];
        table1[0] = calcLogTableIndex(SQUARE_WAVE_OUTPUT);
        for (i = 1; i < 16; i++) {
            table1[i] = LOG_TABLE_BOTTOM;
        }
        waveTables[PG_PC_NZ_16BIT] = SiOPMWaveTable.alloc(table1, 0);

        // pitch controlable noise
        table1 = waveTables[PG_NOISE_SHORT].wavelet;
        table2 = new int[SAMPLING_TABLE_SIZE];
        for (j = 0; j < SAMPLING_TABLE_SIZE; j++) {
            i = j * 11;
            imax = Math.min((i + 11), SAMPLING_TABLE_SIZE);
            for (; i < imax; i++) {
                table2[i] = table1[j];
            }
        }
        waveTables[PG_PC_NZ_SHORT] = SiOPMWaveTable.alloc(table2, 0);

        // ramp wave tables
        //
        // ramp wave
        imax = SAMPLING_TABLE_SIZE;
        imax2 = SAMPLING_TABLE_SIZE >> 1;
        imax4 = SAMPLING_TABLE_SIZE >> 2;
        for (j = 1; j < 60; j++) {
            iv = imax4 >> (j >> 3);
            iv -= (iv * (j & 7)) >> 4;
            if (prev == iv) {
                waveTables[PG_RAMP + 64 - j] = waveTables[PG_RAMP + 65 - j];
                waveTables[PG_RAMP + 64 + j] = waveTables[PG_RAMP + 63 + j];
                continue;
            }
            prev = iv;

            table1 = new int[SAMPLING_TABLE_SIZE];
            table2 = new int[SAMPLING_TABLE_SIZE];
            imax3 = imax2 - iv;
            dp = 1. / imax3;
            for (i = 0, p = dp * 0.5; i < imax3; i++, p += dp) {
                iv = calcLogTableIndex(p);
                table1[i] = iv;   // positive value
                table1[imax - i - 1] = iv + 1; // negative value
                table2[imax2 + i] = iv + 1; // negative value
                table2[imax2 - i - 1] = iv;   // positive value
            }
            dp = 1. / (imax2 - imax3);
            for (; i < imax2; i++, p -= dp) {
                iv = calcLogTableIndex(p);
                table1[i] = iv;   // positive value
                table1[imax - i - 1] = iv + 1; // negative value
                table2[imax2 + i] = iv + 1; // negative value
                table2[imax2 - i - 1] = iv;   // positive value
            }
            waveTables[PG_RAMP + 64 - j] = SiOPMWaveTable.alloc(table1, 0);
            waveTables[PG_RAMP + 64 + j] = SiOPMWaveTable.alloc(table2, 0);
        }
        for (j = 0; j < 5; j++) waveTables[PG_RAMP + j] = waveTables[PG_SAW_UP];
        for (j = 124; j < 128; j++) waveTables[PG_RAMP + j] = waveTables[PG_SAW_DOWN];
        waveTables[PG_RAMP + 64] = waveTables[PG_TRIANGLE];


        // MA3 wave tables
        //--
        // waveform 0-5 = sine wave
        waveTables[PG_MA3_WAVE] = waveTables[PG_SINE];
        __exp_ma3_waves(0);
        // waveform 8-13 = bi-triangle modulated sine ?
        table2 = waveTables[PG_SINE].wavelet;
        table1 = new int[SAMPLING_TABLE_SIZE];
        j = 0;
        for (i = 0; i < SAMPLING_TABLE_SIZE; i++) {
            table1[i] = table2[i + j];
            j += 1 - (((i >> (SAMPLING_TABLE_BITS - 3)) + 1) & 2); // triangle wave
        }
        waveTables[PG_MA3_WAVE + 8] = SiOPMWaveTable.alloc(table1, 0);
        __exp_ma3_waves(8);
        // waveform 16-21 = triangle wave
        waveTables[PG_MA3_WAVE + 16] = waveTables[PG_TRIANGLE];
        __exp_ma3_waves(16);
        // waveform 24-29 = saw wave
        waveTables[PG_MA3_WAVE + 24] = waveTables[PG_SAW_UP];
        __exp_ma3_waves(24);
        // waveform 6 = square
        waveTables[PG_MA3_WAVE + 6] = waveTables[PG_SQUARE];
        // waveform 14 = half square
        iv = calcLogTableIndex(1);
        waveTables[PG_MA3_WAVE + 14] = SiOPMWaveTable.alloc(new int[] {iv, LOG_TABLE_BOTTOM}, 0);
        // waveform 22 = twice of half square
        waveTables[PG_MA3_WAVE + 22] = SiOPMWaveTable.alloc(new int[] {iv, LOG_TABLE_BOTTOM, iv, LOG_TABLE_BOTTOM}, 0);
        // waveform 30 = quarter square
        waveTables[PG_MA3_WAVE + 30] = SiOPMWaveTable.alloc(new int[] {iv, LOG_TABLE_BOTTOM, LOG_TABLE_BOTTOM, LOG_TABLE_BOTTOM}, 0);

        // waveform 7 ???
        table1 = new int[SAMPLING_TABLE_SIZE];
        dp = 6.283185307179586 / SAMPLING_TABLE_SIZE;
        imax = SAMPLING_TABLE_SIZE >> 2;
        imax2 = SAMPLING_TABLE_SIZE >> 1;
        imax4 = SAMPLING_TABLE_SIZE;
        for (i = 0, p = dp * 0.5; i < imax; i++, p += dp) {
            iv = calcLogTableIndex(1 - Math.sin(p));
            table1[i] = iv;   // positive index
            table1[i + imax] = LOG_TABLE_BOTTOM;
            table1[i + imax2] = LOG_TABLE_BOTTOM;
            table1[imax4 - i - 1] = iv + 1; // negative value index
        }
        waveTables[PG_MA3_WAVE + 7] = SiOPMWaveTable.alloc(table1, 0);
        // waveform 15,23,31 = custom waveform 0-2 (not available)
        waveTables[PG_MA3_WAVE + 15] = noWaveTable;
        waveTables[PG_MA3_WAVE + 23] = noWaveTable;
        waveTables[PG_MA3_WAVE + 31] = noWaveTable;
    }

    // expand MA3 waveforms
    private void __exp_ma3_waves(int index) {
        // multipurpose
        int i, imax;
        int[] table1, table2;

        // basic waveform
        table2 = waveTables[PG_MA3_WAVE + index].wavelet;

        // waveform 1
        table1 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE >> 1;
        for (i = 0; i < imax; i++) {
            table1[i] = table2[i];
            table1[i + imax] = LOG_TABLE_BOTTOM;
        }
        waveTables[PG_MA3_WAVE + index + 1] = SiOPMWaveTable.alloc(table1, 0);

        // waveform 2
        table1 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE >> 1;
        for (i = 0; i < imax; i++) {
            table1[i] = table2[i];
            table1[i + imax] = table2[i];
        }
        waveTables[PG_MA3_WAVE + index + 2] = SiOPMWaveTable.alloc(table1, 0);

        // waveform 3
        table1 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE >> 2;
        for (i = 0; i < imax; i++) {
            table1[i] = table2[i];
            table1[i + imax] = LOG_TABLE_BOTTOM;
            table1[i + imax * 2] = table2[i];
            table1[i + imax * 3] = LOG_TABLE_BOTTOM;
        }
        waveTables[PG_MA3_WAVE + index + 3] = SiOPMWaveTable.alloc(table1, 0);

        // waveform 4
        table1 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE >> 1;
        for (i = 0; i < imax; i++) {
            table1[i] = table2[i << 1];
            table1[i + imax] = LOG_TABLE_BOTTOM;
        }
        waveTables[PG_MA3_WAVE + index + 4] = SiOPMWaveTable.alloc(table1, 0);

        // waveform 5
        table1 = new int[SAMPLING_TABLE_SIZE];
        imax = SAMPLING_TABLE_SIZE >> 2;
        for (i = 0; i < imax; i++) {
            table1[i] = table2[i << 1];
            table1[i + imax] = table1[i];
            table1[i + imax * 2] = LOG_TABLE_BOTTOM;
            table1[i + imax * 3] = LOG_TABLE_BOTTOM;
        }
        waveTables[PG_MA3_WAVE + index + 5] = SiOPMWaveTable.alloc(table1, 0);
    }

    // calculate LFO tables
    //
    private void _createLFOTables() {
        int i, t, s;
        int[] table, table2;

        // LFO timer steps
        // This calculation is hybrid between fmgen and x68sound.dll, and extend as 20bit fixed dicimal.
        lfo_timerSteps = new int[256];
        for (i = 0; i < 256; i++) {
            t = 16 + (i & 15);  // linear interpolation for 4LSBs
            s = 15 - (i >> 4);  // log-scale shift for 4HSBs
            lfo_timerSteps[i] = ((t << (LFO_FIXED_BITS - 4)) * clock_ratio / (8 << s)) >> CLOCK_RATIO_BITS; // 4 from fmgen, 8 from x68sound.
        }

        lfo_waveTables = new int[LFO_WAVE_MAX][];    // [0, 255]

        // LFO_TABLE_SIZE = 256 cannot be changed !!
        // saw wave
        table = new int[256];
        table2 = new int[256];
        for (i = 0; i < 256; i++) {
            table[i] = 255 - i;
            table2[i] = i;
        }
        lfo_waveTables[LFO_WAVE_SAW] = table;
        lfo_waveTables[LFO_WAVE_SAW + 4] = table2;

        // pulse wave
        table = new int[256];
        table2 = new int[256];
        for (i = 0; i < 256; i++) {
            table[i] = (i < 128) ? 255 : 0;
            table2[i] = 255 - table[i];
        }
        lfo_waveTables[LFO_WAVE_SQUARE] = table;
        lfo_waveTables[LFO_WAVE_SQUARE + 4] = table2;

        // triangle wave
        table = new int[256];
        table2 = new int[256];
        for (i = 0; i < 64; i++) {
            t = i << 1;
            table[i] = t + 128;
            table[127 - i] = t + 128;
            table[128 + i] = 126 - t;
            table[255 - i] = 126 - t;
        }
        for (i = 0; i < 256; i++) {
            table2[i] = 255 - table[i];
        }
        lfo_waveTables[LFO_WAVE_TRIANGLE] = table;
        lfo_waveTables[LFO_WAVE_TRIANGLE + 4] = table2;

        // noise wave
        table = new int[256];
        table2 = new int[256];
        for (i = 0; i < 256; i++) {
            table[i] = (int) (Math.random() * 255);
            table2[i] = 255 - table[i];
        }
        lfo_waveTables[LFO_WAVE_NOISE] = table;
        lfo_waveTables[LFO_WAVE_NOISE + 4] = table2;

        // lfo table for chorus
        table = new int[256];
        for (i = 0; i < 256; i++) {
            table[i] = (i - 128) * (i - 128);
        }
        lfo_chorusTables = table;
    }

    // calculate filter tables
    //
    private void _createFilterTables() {
        int i;
        double shift, liner;

        filter_cutoffTable = new double[129];
        filter_feedbackTable = new double[129];
        for (i = 0; i < 128; i++) {
            filter_cutoffTable[i] = i * i * 0.00006103515625; //0.00006103515625 = 1/(128*128)
            filter_feedbackTable[i] = 1.0 + 1.0 / (1.0 - filter_cutoffTable[i]); // ???
        }
        filter_cutoffTable[128] = 1;
        filter_feedbackTable[128] = filter_feedbackTable[128];

        // 2.36514 = 3 / ((clock/64)/rate)
        filter_eg_rate = new int[64];
        filter_eg_rate[0] = 0;
        for (i = 1; i < 60; i++) {
            shift = 1 << (14 - (i >> 2));
            liner = (i & 3) * 0.125 + 0.5;
            filter_eg_rate[i] = (int) (2.36514 * shift * liner + 0.5);
        }
        for (; i < 64; i++) {
            filter_eg_rate[i] = 1;
        }
    }

    // calculation
    //

    /** calculate log table index from Number[-1,1]. */
    public static int calcLogTableIndex(double n) {
        // 369.3299304675746 = 256/log(2)
        // 0.0001220703125 = 1/(2^13)
        if (n < 0) {
            return (n < -0.0001220703125) ? ((((int) (Math.log(-n) * -369.3299304675746 + 0.5) + 1) << 1) + 1) : LOG_TABLE_BOTTOM;
        } else {
            return (n > 0.0001220703125) ? (((int) (Math.log(n) * -369.3299304675746 + 0.5) + 1) << 1) : LOG_TABLE_BOTTOM;
        }
    }

    // wave tables
    //

    /** Reset all user tables */
    public void resetAllUserTables() {
        // [NOTE] We should free allocated memory area in the environment without garbege collectors.
        int i;
        SiOPMWavePCMTable pcm;

        // Reset wave tables
        for (i = 0; i < WAVE_TABLE_MAX; i++) {
            if (_customWaveTables[i] != null) {
                _customWaveTables[i].free();
                _customWaveTables[i] = null;
            }
        }
        for (i = 0; i < PCM_DATA_MAX; i++) {
            if (_pcmVoices[i] != null) {
                pcm = (SiOPMWavePCMTable) _pcmVoices[i].waveData;
                if (pcm != null) pcm._free();
                _pcmVoices[i] = null;
            }
        }
        _stencilCustomWaveTables = null;
        _stencilPCMVoices = null;
    }

    /** Register wave table. */
    public SiOPMWaveTable registerWaveTable(int index, int[] table) {
        // register wave table
        SiOPMWaveTable newWaveTable = SiOPMWaveTable.alloc(table, 0);
        index &= WAVE_TABLE_MAX - 1;
        _customWaveTables[index] = newWaveTable;

        // update PG_MA3_WAVE waveform 15,23,31.
        if (index < 3) {
            // index=0,1,2 are same as PG_MA3 waveform 15,23,31.
            waveTables[15 + index * 8 + PG_MA3_WAVE] = newWaveTable;
        }

        return newWaveTable;
    }

    /** Register Sampler data. */
    public SiOPMWaveSamplerData registerSamplerData(int index, Object table, boolean ignoreNoteOff, int pan, int srcChannelCount, int channelCount) {
        int bank = (index >> NOTE_BITS) & (SAMPLER_TABLE_MAX - 1);
        return samplerTables[bank].setSample(new SiOPMWaveSamplerData(table, ignoreNoteOff, pan, srcChannelCount, channelCount, null), index & (SAMPLER_DATA_MAX - 1), -1);
    }

    /** set global PCM wave table. call from SiONDriver.setPCMWave() */
    public SiMMLVoice _setGlobalPCMVoice(int index, SiMMLVoice voice) {
        // register PCM data
        index &= PCM_DATA_MAX - 1;
        if (_pcmVoices[index] == null) _pcmVoices[index] = new SiMMLVoice();
        _pcmVoices[index].copyFrom(voice);
        return _pcmVoices[index];
    }

    /** get global PCM wave table. call from SiONDriver.setPCMWave() */
    public SiMMLVoice _getGlobalPCMVoice(int index) {
        // register PCM data
        index &= PCM_DATA_MAX - 1;
        if (_pcmVoices[index] == null) {
            _pcmVoices[index] = new SiMMLVoice()._newBlankPCMVoice(index);
        }
        return _pcmVoices[index];
    }

    /** get wave table from a list of SiONDriver and SiONData */
    public SiOPMWaveTable getWaveTable(int index) {
        if (index < PG_CUSTOM) return waveTables[index];
        if (index < PG_PCM) {
            index -= PG_CUSTOM;
            if (_stencilCustomWaveTables != null && _stencilCustomWaveTables[index] != null) return _stencilCustomWaveTables[index];
            return _customWaveTables[index] != null ? _customWaveTables[index] : noWaveTableOPM;
        }
        return noWaveTable;
    }

    /** get PCM data from a list of SiONDriver and SiONData */
    public SiOPMWavePCMTable getPCMData(int index) {
        index &= PCM_DATA_MAX - 1;
        if (_stencilPCMVoices != null && _stencilPCMVoices[index] != null) return (SiOPMWavePCMTable) _stencilPCMVoices[index].waveData;
        return (_pcmVoices[index] == null) ? null : (SiOPMWavePCMTable) _pcmVoices[index].waveData;
    }
}
