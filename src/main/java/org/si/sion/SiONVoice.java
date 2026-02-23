package org.si.sion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.si.sion.module.ISiOPMWaveInterface;
import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerData;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.module.SiOPMWaveTable;
import org.si.sion.sequencer.SiMMLVoice;
import org.si.sion.utils.Translator;


public class SiONVoice extends SiMMLVoice implements ISiOPMWaveInterface {

    public static final String CHIPTYPE_SIOPM = "";
    public static final String CHIPTYPE_OPL = "OPL";
    public static final String CHIPTYPE_OPM = "OPM";
    public static final String CHIPTYPE_OPN = "OPN";
    public static final String CHIPTYPE_OPX = "OPX";
    public static final String CHIPTYPE_MA3 = "MA3";
    public static final String CHIPTYPE_PMS_GUITAR = "PMSGuitar";
    public static final String CHIPTYPE_ANALOG_LIKE = "AnalogLike";

    public String name;

    public SiONVoice(int moduleType, int channelNum, int ar, int rr, int dt, int connectionType, int ws2, int dt2) {
        initialize();
        this.moduleType = moduleType;
        this.channelNum = channelNum;
        setModuleType(moduleType, channelNum, 0);
        channelParam.operatorParam[0].ar = ar;
        channelParam.operatorParam[0].rr = rr;
        pitchShift = dt;
        if (connectionType >= 0) {
            channelParam.opeCount = 5;
            channelParam.alg = (connectionType <= 2) ? connectionType : 0;
            channelParam.operatorParam[0].setPGType(channelNum);
            channelParam.operatorParam[1].setPGType(ws2);
            channelParam.operatorParam[1].detune = dt2;
        }
    }

    public SiONVoice(int moduleType, int channelNum) {
        this(moduleType, channelNum, 63, 63, 0, -1, 0, 0);
    }

    public SiONVoice() {
        this(5, 0, 63, 63, 0, -1, 0, 0);
    }

    @Override
    public SiONVoice clone() {
        SiONVoice newVoice = new SiONVoice();
        newVoice.copyFrom(this);
        newVoice.name = name;
        return newVoice;
    }

    public void setParam(Object[] args) {
        Translator.setParam(channelParam, args);
        chipType = "";
    }

    public void setParamOPL(Object[] args) {
        Translator.setOPLParam(channelParam, args);
        chipType = "OPL";
    }

    public void setParamOPM(Object[] args) {
        Translator.setOPMParam(channelParam, args);
        chipType = "OPM";
    }

    public void setParamOPN(Object[] args) {
        Translator.setOPNParam(channelParam, args);
        chipType = "OPN";
    }

    public void setParamOPX(Object[] args) {
        Translator.setOPXParam(channelParam, args);
        chipType = "OPX";
    }

    public void setParamMA3(Object[] args) {
        Translator.setMA3Param(channelParam, args);
        chipType = "MA3";
    }

    public void setParamAL(Object[] args) {
        Translator.setALParam(channelParam, args);
        chipType = "AnalogLike";
    }

    public int[] getParam() {
        return Translator.getParam(channelParam);
    }

    public int[] getParamOPL() {
        return Translator.getOPLParam(channelParam);
    }

    public int[] getParamOPM() {
        return Translator.getOPMParam(channelParam);
    }

    public int[] getParamOPN() {
        return Translator.getOPNParam(channelParam);
    }

    public int[] getParamOPX() {
        return Translator.getOPXParam(channelParam);
    }

    public int[] getParamMA3() {
        return Translator.getMA3Param(channelParam);
    }

    public int[] getParamAL() {
        return Translator.getALParam(channelParam);
    }

    public String getMML(int index, String type, boolean appendPostfixMML) {
        if (type == null) type = chipType;
        String mml = switch (type) {
            case "OPL" -> "#OPL@" + index + Translator.mmlOPLParam(channelParam, " ", "\n", name);
            case "OPM" -> "#OPM@" + index + Translator.mmlOPMParam(channelParam, " ", "\n", name);
            case "OPN" -> "#OPN@" + index + Translator.mmlOPNParam(channelParam, " ", "\n", name);
            case "OPX" -> "#OPX@" + index + Translator.mmlOPXParam(channelParam, " ", "\n", name);
            case "MA3" -> "#MA@" + index + Translator.mmlMA3Param(channelParam, " ", "\n", name);
            case "AnalogLike" -> "#AL@" + index + Translator.mmlALParam(channelParam, " ", "\n", name);
            default -> "#@" + index + Translator.mmlParam(channelParam, " ", "\n", name);
        };
        if (appendPostfixMML) {
            String postfix = Translator.mmlVoiceSetting(this);
            if (!postfix.isEmpty()) mml += "\n" + postfix;
        }
        return mml + ";";
    }

    public int setByMML(String mml) {
        initialize();
        Pattern rexNum = Pattern.compile("(#[A-Z]*@)\\s*(\\d+)\\s*\\{(.*?)\\\\}(.*?);", Pattern.DOTALL);
        Matcher res = rexNum.matcher(mml);
        if (res.find()) {
            String cmd = res.group(1);
            int voiceIndex = Integer.parseInt(res.group(2));
            String prm = res.group(3);
            String pfx = res.group(4);

            switch (cmd) {
                case "#@": {
                    Translator.parseParam(channelParam, prm);
                    chipType = "";
                }
                break;
                case "#OPL@": {
                    Translator.parseOPLParam(channelParam, prm);
                    chipType = "OPL";
                }
                break;
                case "#OPM@": {
                    Translator.parseOPMParam(channelParam, prm);
                    chipType = "OPM";
                }
                break;
                case "#OPN@": {
                    Translator.parseOPNParam(channelParam, prm);
                    chipType = "OPN";
                }
                break;
                case "#OPX@": {
                    Translator.parseOPXParam(channelParam, prm);
                    chipType = "OPX";
                }
                break;
                case "#MA@": {
                    Translator.parseMA3Param(channelParam, prm);
                    chipType = "MA3";
                }
                break;
                case "#AL@": {
                    Translator.parseALParam(channelParam, prm);
                    chipType = "AnalogLike";
                }
                break;
                default:
                    return -1;
            }
            Translator.parseVoiceSetting(this, pfx, null);

            Pattern rexNam = Pattern.compile("^.*?(//\\s*(.+?))?[\n\r]");
            Matcher resNam = rexNam.matcher(prm);
            name = (resNam.find() && resNam.group(2) != null) ? resNam.group(2) : "";
            return voiceIndex;
        }
        return -1;
    }

    @Override
    public void initialize() {
        super.initialize();
        name = "";
        updateTrackParameters = true;
    }

    public SiOPMWaveTable setWaveTable(double[] data) {
        int i, imax = data.length;
        int[] table = new int[imax];
        for (i = 0; i < imax; i++) table[i] = SiOPMTable.calcLogTableIndex(data[i]);
        waveData = SiOPMWaveTable.alloc(table, 0);
        moduleType = 4;
        return (SiOPMWaveTable) waveData;
    }

    public SiOPMWavePCMData setPCMVoice(Object data, int samplingNote, int srcChannelCount, int channelCount) {
        moduleType = 7;
        waveData = new SiOPMWavePCMData(data, samplingNote, srcChannelCount, channelCount);
        return (SiOPMWavePCMData) waveData;
    }

    public SiOPMWaveSamplerData setMP3Voice(Object wave, boolean ignoreNoteOff, int channelCount) {
        moduleType = 10;
        return (SiOPMWaveSamplerData) (waveData = new SiOPMWaveSamplerData(wave, ignoreNoteOff, 0, 2, channelCount, null));
    }

    @Override
    public SiOPMWavePCMData setPCMWave(int index, Object data, double samplingNote, int keyRangeFrom, int keyRangeTo, int srcChannelCount, int channelCount) {
        if (moduleType != 7 || channelNum != index) waveData = null;
        moduleType = 7;
        channelNum = index;
        SiOPMWavePCMTable pcmTable = (waveData instanceof SiOPMWavePCMTable) ? (SiOPMWavePCMTable) waveData : new SiOPMWavePCMTable();
        SiOPMWavePCMData pcmData = new SiOPMWavePCMData(data, (int)(samplingNote * 64), srcChannelCount, channelCount);
        pcmTable.setSample(pcmData, keyRangeFrom, keyRangeTo);
        waveData = pcmTable;
        return pcmData;
    }

    @Override
    public SiOPMWaveSamplerData setSamplerWave(int index, Object data, boolean ignoreNoteOff, int pan, int srcChannelCount, int channelCount) {
        moduleType = 10;
        SiOPMWaveSamplerTable samplerTable = (waveData instanceof SiOPMWaveSamplerTable) ? (SiOPMWaveSamplerTable) waveData : new SiOPMWaveSamplerTable();
        SiOPMWaveSamplerData sampleData = new SiOPMWaveSamplerData(data, ignoreNoteOff, pan, srcChannelCount, channelCount, null);
        samplerTable.setSample(sampleData, index & (SiOPMTable.NOTE_TABLE_SIZE - 1), 0);
        waveData = samplerTable;
        return sampleData;
    }

    public SiONVoice setSamplerTable(SiOPMWaveSamplerTable table) {
        moduleType = 10;
        waveData = table;
        return this;
    }

    public SiONVoice setPMSGuitar(int ar, int dr, int tl, int fixedPitch, int ws, int tension) {
        moduleType = 11;
        channelNum = 1;
        setParam(new Object[] {1, 0, 0, ws, ar, dr, 0, 63, 15, tl, 0, 0, 1, 0, 0, 0, 0, fixedPitch});
        pmsTension = tension;
        chipType = "PMSGuitar";
        return this;
    }

    public SiONVoice setAnalogLike(int connectionType, int ws1, int ws2, int balance, int vco2pitch) {
        channelParam.opeCount = 5;
        channelParam.alg = (connectionType >= 0 && connectionType <= 3) ? connectionType : 0;
        channelParam.operatorParam[0].setPGType(ws1);
        channelParam.operatorParam[1].setPGType(ws2);

        if (balance > 64) balance = 64;
        else if (balance < -64) balance = -64;

        int[] tltable = SiOPMTable.getInstance().eg_lv2tlTable;
        if (tltable != null && tltable.length > 128) {
            channelParam.operatorParam[0].tl = tltable[64 - balance];
            channelParam.operatorParam[1].tl = tltable[balance + 64];
        }

        channelParam.operatorParam[0].detune = 0;
        channelParam.operatorParam[1].detune = vco2pitch;

        chipType = "AnalogLike";

        return this;
    }

    public SiONVoice setEnvelop(int ar, int dr, int sr, int rr, int sl, int tl) {
        for (int i = 0; i < 4; i++) {
            SiOPMOperatorParam opp = channelParam.operatorParam[i];
            opp.ar = ar;
            opp.dr = dr;
            opp.sr = sr;
            opp.rr = rr;
            opp.sl = sl;
            opp.tl = tl;
        }
        return this;
    }

    public SiONVoice setFilterEnvelop(int filterType, int cutoff, int resonance, int far, int fdr1, int fdr2, int frr, int fdc1, int fdc2, int fsc /* = 32 */, int frc /* = 128 */) {
        channelParam.filterType = filterType;
        channelParam.cutoff = cutoff;
        channelParam.resonance = resonance;
        channelParam.far = far;
        channelParam.fdr1 = fdr1;
        channelParam.fdr2 = fdr2;
        channelParam.frr = frr;
        channelParam.fdc1 = fdc1;
        channelParam.fdc2 = fdc2;
        channelParam.fsc = fsc;
        channelParam.frc = frc;
        return this;
    }

    public SiONVoice setLPFEnvelop(int cutoff, int resonance, int far, int fdr1, int fdr2, int frr, int fdc1, int fdc2, int fsc, int frc) {
        return setFilterEnvelop(0, cutoff, resonance, far, fdr1, fdr2, frr, fdc1, fdc2, fsc, frc);
    }

    public SiONVoice setAmplitudeModulation(int depth, int end_depth, int delay, int term) {
        channelParam.amd = amDepth = depth;
        amDepthEnd = end_depth;
        amDelay = delay;
        amTerm = term;
        return this;
    }

    public SiONVoice setPitchModulation(int depth, int end_depth, int delay, int term) {
        channelParam.pmd = pmDepth = depth;
        pmDepthEnd = end_depth;
        pmDelay = delay;
        pmTerm = term;
        return this;
    }
}
