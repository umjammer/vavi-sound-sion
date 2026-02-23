//
// Translators
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.si.sion.SiONVoice;
import org.si.sion.effector.SiEffectBase;
import org.si.sion.effector.SiEffectModule;
import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerData;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.sequencer.SiMMLEnvelopTable;
import org.si.sion.sequencer.SiMMLTable;
import org.si.sion.sequencer.SiMMLVoice;
import org.si.utils.SLLint;

import static java.lang.Integer.parseInt;


/** Translator */
public class Translator {

    /** constructor, do nothing. */
    public Translator() {
    }

    // mckc
    //

    /**
     * Translate ppmckc mml to SiOPM mml.
     *
     * @param mckcMML ppmckc MML text.
     * @return translated SiON MML text
     */
    public String mckc(String mckcMML) {
        // If I have motivation ..., or I wish someone who know mck well would do ...
        throw new Error("This instanceof not implemented");
//        return mckcMML;
    }

    // flmml
    //

    /**
     * Translate flMML's mml to SiOPM mml.
     *
     * @param flMML flMML's MML text.
     * @return translated SiON MML text
     */
    public String flmml(String flMML) {
        // If I have motivation ..., or I wish someone who know mck well would do ...
        throw new Error("This instanceof not implemented");
//        return flMML;
    }

    // tsscp
    //

    /**
     * Translate pTSSCP mml to SiOPM mml.
     *
     * @param tsscpMML  TSSCP MML text.
     * @param volumeByX true to translate volume control to SiON MMLs 'x' command, false to translate to SiON MMLs 'v' command.
     * @return translated SiON MML text
     */
    public String tsscp(String tsscpMML, boolean volumeByX) {
        StringBuilder mml;
        String com;
        String str1;
        String str2;
        int i, imax;
        String volUp, volDw;
        Pattern rex, rex_sys, rex_com;
        Matcher res;

        // translate mml
        //
        String noteLetters = "cdefgab";
        int[] noteShift = {0, 2, 4, 5, 7, 9, 11};
        String[] panTable = {"@v0", "p0", "p8", "p4"};
        SiMMLTable table = SiMMLTable.getInstance();
        int charCodeA = 'a';
        int charCodeG = 'g';
        int charCodeR = 'r';
        String hex = "0123456789abcdef";
        int p0, p1, p2, p3, p4;
        boolean reql8;
        int octave;
        boolean revOct;
        int loopOct;
        Object loopMMLBefore, loopMMLContent;
        boolean loopMacro;

        rex = Pattern.compile("(;|(/:|:/|ml|mp|na|ns|nt|ph|@kr|@ks|@ml|@ns|@apn|@[fkimopqsv]?|[klopqrstvx$%<>(){}[\\\\]|_~^/&*]|[a-g][#+\\-]?)\\s*([\\-\\d]*)[,\\s]*([\\-\\d]+)?[,\\s]*([\\-\\d]+)?[,\\s]*([\\-\\d]+)?[,\\s]*([\\-\\d]+)?)|#(FM|[A-Z]+)=?\\s*([^;]*)|([A-Z])(\\(([a-g])([\\-+#]?)\\))?|.");
        rex_sys = Pattern.compile("\\s * ([0 - 9] *)[,=<\\s]*([ ^ >]*)");
        rex_com = Pattern.compile("[{}]");

        volUp = "(";
        volDw = ")";
        mml = new StringBuilder();
        reql8 = true;
        octave = 5;
        revOct = false;
        loopOct = -1;
        loopMacro = false;
        loopMMLBefore = null;
        loopMMLContent = null;
        res = rex.matcher(tsscpMML);
        while (res.matches()) {
            if (res.group(1) != null) {
                if (res.group(1).equals(";")) {
                    mml.append(res.group(0));
                    reql8 = true;
                } else {
                    // mml commands
                    i = res.group(2).charAt(0);
                    if ((charCodeA <= i && i <= charCodeG) || i == charCodeR) {
                        if (reql8) mml.append("l8").append(res.group(0));
                        else mml.append(res.group(0));
                        reql8 = false;
                    } else {
                        switch (res.group(2)) {
                            case "l": {
                                mml.append(res.group(0));
                                reql8 = false;
                            }
                            break;
                            case "/:": {
                                mml.append("[").append(res.group(3));
                            }
                            break;
                            case ":/": {
                                mml.append("]");
                            }
                            break;
                            case "/": {
                                mml.append("|");
                            }
                            break;
                            case "~": {
                                mml.append(volUp).append(res.group(3));
                            }
                            break;
                            case "_": {
                                mml.append(volDw).append(res.group(3));
                            }
                            break;
                            case "q": {
                                mml.append("q").append((Integer.parseInt(res.group(3)) + 1) >> 1);
                            }
                            break;
                            case "@m": {
                                mml.append("@mask").append(Integer.parseInt(res.group(3)));
                            }
                            break;
                            case "ml": {
                                mml.append("@ml").append(Integer.parseInt(res.group(3)));
                            }
                            break;
                            case "p": {
                                mml.append(panTable[Integer.parseInt(res.group(3)) & 3]);
                            }
                            break;
                            case "@p": {
                                mml.append("@p").append(Integer.parseInt(res.group(3)) - 64);
                            }
                            break;
                            case "ph": {
                                mml.append("@ph").append(Integer.parseInt(res.group(3)));
                            }
                            break;
                            case "ns": {
                                mml.append("kt").append(res.group(3));
                            }
                            break;
                            case "@ns": {
                                mml.append("!@ns").append(res.group(3));
                            }
                            break;
                            case "k": {
                                p0 = (int) (Double.parseDouble(res.group(3)) * 4);
                                mml.append("k").append(p0);
                            }
                            break;
                            case "@k": {
                                p0 = (int) (Double.parseDouble(res.group(3)) * 0.768);
                                mml.append("k").append(p0);
                            }
                            break;
                            case "@kr": {
                                p0 = (int) (Double.parseDouble(res.group(3)) * 0.768);
                                mml.append("!@kr").append(p0);
                            }
                            break;
                            case "@ks": {
                                mml.append("@,,,,,,,").append(Integer.parseInt(res.group(3)) >> 5);
                            }
                            break;
                            case "na": {
                                mml.append("!").append(res.group(0));
                            }
                            break;
                            case "o": {
                                mml.append(res.group(0));
                                octave = Integer.parseInt(res.group(3));
                            }
                            break;
                            case "<": {
                                mml.append(res.group(0));
                                octave += (revOct) ? -1 : 1;
                            }
                            break;
                            case ">": {
                                mml.append(res.group(0));
                                octave += (revOct) ? 1 : -1;
                            }
                            break;
                            case "%": {
                                mml.append((res.group(3).equals("6")) ? "%4" : res.group(0));
                            }
                            break;

                            case "@ml": {
                                p0 = Integer.parseInt(res.group(3)) >> 7;
                                p1 = Integer.parseInt(res.group(3)) - (p0 << 7);
                                mml.append("@ml").append(p0).append(",").append(p1);
                            }
                            break;
                            case "mp": {
                                p0 = Integer.parseInt(res.group(3));
                                p1 = Integer.parseInt(res.group(4));
                                p2 = Integer.parseInt(res.group(5));
                                p3 = Integer.parseInt(res.group(6));
                                p4 = Integer.parseInt(res.group(7));
                                if (p3 == 0) p3 = 1;
                                switch (p0) {
                                    case 0:
                                        mml.append("mp0");
                                        break;
                                    case 1:
                                        mml.append("@lfo").append(((p1 / p3) + 1) * 4 * p2).append("mp").append(p1);
                                        break;
                                    default:
                                        mml.append("@lfo").append(((p1 / p3) + 1) * 4 * p2).append("mp0,").append(p1).append(",").append(p0);
                                        break;
                                }
                            }
                            break;
                            case "v": {
                                if (volumeByX) {
                                    p0 = (res.group(3).isEmpty()) ? 40 : ((Integer.parseInt(res.group(3)) << 2) + (Integer.parseInt(res.group(3)) >> 2));
                                    if (res.group(4) != null) {
                                        p1 = (Integer.parseInt(res.group(4)) << 2) + (Integer.parseInt(res.group(4)) >> 2);
                                        p2 = (p1 > 0) ? ((int) (Math.atan(p0 / p1) * 81.48733086305041)) : 128; // 81.48733086305041 = 128/(PI*0.5)
                                        p3 = Math.max(p0, p1);
                                        mml.append("@p").append(p2).append("x").append(p3);
                                    } else {
                                        mml.append("x").append(p0);
                                    }
                                } else {
                                    p0 = (res.group(3).isEmpty()) ? 10 : Integer.parseInt(res.group(3));
                                    if (res.group(4) != null) {
                                        p1 = Integer.parseInt(res.group(4));
                                        p2 = (p1 > 0) ? ((int) (Math.atan(p0 / p1) * 81.48733086305041)) : 128; // 81.48733086305041 = 128/(PI*0.5)
                                        p3 = Math.max(p0, p1);
                                        mml.append("@p").append(p2).append("v").append(p3);
                                    } else {
                                        mml.append("v").append(p0);
                                    }
                                }
                            }
                            break;
                            case "@v": {
                                if (volumeByX) {
                                    p0 = (res.group(3).isEmpty()) ? 40 : (Integer.parseInt(res.group(3)) >> 2);
                                    if (res.group(4) != null) {
                                        p1 = Integer.parseInt(res.group(4)) >> 2;
                                        p2 = (p1 > 0) ? ((int) (Math.atan(p0 / p1) * 81.48733086305041)) : 128; // 81.48733086305041 = 128/(PI*0.5)
                                        p3 = Math.max(p0, p1);
                                        mml.append("@p").append(p2).append("x").append(p3);
                                    } else {
                                        mml.append("x").append(p0);
                                    }
                                } else {
                                    p0 = (res.group(3).isEmpty()) ? 10 : (Integer.parseInt(res.group(3)) >> 4);
                                    if (res.group(4) != null) {
                                        p1 = Integer.parseInt(res.group(4)) >> 4;
                                        p2 = (p1 > 0) ? ((int) (Math.atan(p0 / p1) * 81.48733086305041)) : 128; // 81.48733086305041 = 128/(PI*0.5)
                                        p3 = Math.max(p0, p1);
                                        mml.append("@p").append(p2).append("v").append(p3);
                                    } else {
                                        mml.append("v").append(p0);
                                    }
                                }
                            }
                            break;
                            case "s": {
                                p0 = Integer.parseInt(res.group(3));
                                p1 = Integer.parseInt(res.group(4));
                                mml.append("s").append(table.tss_s2rr[p0 & 255]);
                                if (p1 != 0) mml.append(",").append(p1 * 3);
                            }
                            break;
                            case "@s": {
                                p0 = Integer.parseInt(res.group(3));
                                p1 = Integer.parseInt(res.group(4));
                                p3 = Integer.parseInt(res.group(6));
                                p2 = (Integer.parseInt(res.group(5)) >= 100) ? 15 : (int) (Double.parseDouble(res.group(5)) * 0.09);
                                mml.append((p0 == 0) ? "@,63,0,0,,0" : (
                                        "@," + table.tss_s2ar[p0 & 255] + "," + table.tss_s2dr[p1 & 255] + "," + table.tss_s2sr[p3 & 255] + ",," + p2
                                ));
                            }
                            break;
                            case "{": {
                                i = 1;
                                p0 = res.start() + 1;
                                int scanFrom = p0;
                                do {
                                    Matcher braceMatcher = rex_com.matcher(tsscpMML);
                                    if (!braceMatcher.find(scanFrom)) throw errorTranslation("{{...} ?");
                                    if (braceMatcher.group(0).equals("{")) i++;
                                    else if (braceMatcher.group(0).equals("}")) --i;
                                    res = braceMatcher;
                                    scanFrom = braceMatcher.end();
                                } while (i != 0);
                                mml.append("/*{").append(tsscpMML, p0, res.start()).append("}*/");
                            }
                            break;

                            case "[": {
                                if (loopMMLBefore != null) throw errorTranslation("[[...] ?");
                                loopMacro = false;
                                loopMMLBefore = mml.toString();
                                loopMMLContent = null;
                                mml = new StringBuilder(res.group(3));
                                loopOct = octave;
                            }
                            break;
                            case "|": {
                                if (loopMMLBefore == null) throw errorTranslation("\"|\" can be only in \"[...]\"");
                                loopMMLContent = mml.toString();
                                mml = new StringBuilder();
                            }
                            break;
                            case "]": {
                                if (loopMMLBefore == null) throw errorTranslation("[...]] ?");
                                if (!loopMacro && loopOct == octave) {
                                    if (loopMMLContent != null) mml = new StringBuilder(loopMMLBefore + "[" + loopMMLContent + "|" + mml + "]");
                                    else mml = new StringBuilder(loopMMLBefore + "[" + mml + "]");
                                } else {
                                    if (loopMMLContent != null) mml = new StringBuilder(loopMMLBefore + "![" + loopMMLContent + "!|" + mml + "!]");
                                    else mml = new StringBuilder(loopMMLBefore + "![" + mml + "!]");
                                }
                                loopMMLBefore = null;
                                loopMMLContent = null;
                            }
                            break;

                            case "}":
                                throw errorTranslation("{...}} ?");
                            case "@apn":
                            case "x":
                                break;

                            default: {
                                mml.append(res.group(0));
                            }
                            break;
                        }
                    }
                }
            } else if (res.group(10) != null) {
                // macro expansion
                if (reql8) mml.append("l8").append(res.group(10));
                else mml.append(res.group(10));
                reql8 = false;
                loopMacro = true;
                if (res.group(11) != null) {
                    // note shift
                    i = noteShift[noteLetters.indexOf(res.group(12))];
                    if (res.group(13).equals("+") || res.group(13).equals("#")) i++;
                    else if (res.group(13).equals("-")) i--;
                    mml.append("(").append(i).append(")");
                }
            } else if (res.group(8) != null) {
                // system command
                str1 = res.group(8);
                switch (str1) {
                    case "END": {
                        mml.append("#END");
                    }
                    break;
                    case "OCTAVE": {
                        if (res.group(9).equals("REVERSE")) {
                            mml.append("#REV{octave}");
                            revOct = true;
                        }
                    }
                    break;
                    case "OCTAVEREVERSE": {
                        mml.append("#REV{octave}");
                        revOct = true;
                    }
                    break;
                    case "VOLUME": {
                        if (res.group(9).equals("REVERSE")) {
                            volUp = ")";
                            volDw = "(";
                            mml.append("#REV{volume}");
                        }
                    }
                    break;
                    case "VOLUMEREVERSE": {
                        volUp = ")";
                        volDw = "(";
                        mml.append("#REV{volume}");
                    }
                    break;

                    case "TABLE": {
                        Matcher sysRes = rex_sys.matcher(res.group(9));
                        if (sysRes.find()) {
                            mml.append("#TABLE").append(sysRes.group(1)).append("{").append(sysRes.group(2)).append("}*0.25");
                        }
                    }
                    break;

                    case "WAVB": {
                        Matcher sysRes = rex_sys.matcher(res.group(9));
                        if (!sysRes.find()) {
                            break;
                        }
                        str1 = String.valueOf(sysRes.group(2));
                        mml.append("#WAVB").append(sysRes.group(1)).append("{");
                        for (i = 0; i < 32; i++) {
                            p0 = Integer.parseInt("0x" + str1.substring(i << 1, 2));
                            p0 = (p0 < 128) ? (p0 + 127) : (p0 - 128);
                            mml.append(hex.charAt(p0 >> 4) + hex.charAt(p0 & 15));
                        }
                        mml.append("}");
                    }
                    break;

                    case "FM": {
                        String fmRaw = String.valueOf(res.group(9));
                        Matcher fmMatcher = Pattern.compile("([A-Z])([0-9])?(\\()?", Pattern.CASE_INSENSITIVE).matcher(fmRaw);
                        StringBuilder fmConverted = new StringBuilder();
                        while (fmMatcher.find()) {
                            String numToken = fmMatcher.group(2);
                            int num = (numToken != null && !numToken.isEmpty()) ? Integer.parseInt(numToken) : 3;
                            String suffix = (fmMatcher.group(3) != null) ? (num + "(") : "";
                            fmMatcher.appendReplacement(fmConverted, fmMatcher.group(1).toLowerCase() + suffix);
                        }
                        fmMatcher.appendTail(fmConverted);
                        mml.append("#FM{").append(fmConverted).append("}");
                    }
                    break;

                    case "FINENESS":
                    case "MML":
                        // skip next ";"
                        res = rex.matcher(tsscpMML);
                        break;
                    default: {
                        if (str1.length() == 1) {
                            // macro
                            mml.append("#").append(str1).append("=");
                            reql8 = false;
                        } else {
                            // other system events
                            Matcher sysRes = rex_sys.matcher(res.group(9));
                            if (sysRes.find()) {
                                if (sysRes.group(2).isEmpty()) return "#" + str1 + sysRes.group(1);
                                mml.append("#").append(str1).append(sysRes.group(1)).append("{").append(sysRes.group(2)).append("}");
                            }
                        }
                    }
                    break;
                }
            } else {
                mml.append(res.group(0));
            }
            res = rex.matcher(tsscpMML);
        }
        tsscpMML = mml.toString();

        return tsscpMML;
    }


    // Effector
    //
    // parse effector MML string
    //

    /**
     * Parse effector mml and return an array of SiEffectBase.
     *
     * @param mml     Effector MML text.
     * @param postfix postfix text.
     * @return An array of SiEffectBase.
     */
    public SiEffectBase[] parseEffectorMML(String mml, String postfix) {
        List<SiEffectBase> ret;
        Matcher res;
        Pattern rex = Pattern.compile(" ([a - zA - Z_] + |,)\\s * ([.\\-\\d]+)?");
        int i;
        String cmd = "";
        int argc = 0;
        double[] args = new double[16];

        // clear
        ret = new ArrayList<>();
        _clearArgs(args);

        // parse mml
        res = rex.matcher(mml);
        while (res.matches()) {
            if (res.group(1).equals(",")) {
                args[argc++] = Double.parseDouble(res.group(2));
            } else {
                _connectEffect(argc, args, cmd, ret);
                cmd = res.group(1);
                _clearArgs(args);
                args[0] = Double.parseDouble(res.group(2));
                argc = 1;
            }
            res = rex.matcher(mml);
        }
        _connectEffect(argc, args, cmd, ret);

        return ret.toArray(SiEffectBase[]::new);
    }

    // connect new effector
    void _connectEffect(int argc, double[] args, String cmd, List<SiEffectBase> ret) {
        if (argc == 0) return;
        SiEffectBase e = SiEffectModule.getInstance(cmd);
        if (e != null) {
            e.mmlCallback(args);
            ret.add(e);
        }
    }

    // clear arguments
    void _clearArgs(double[] args) {
        for (int i = 0; i < 16; i++) args[i] = Double.NaN;
    }

    // FM parameters
    //

    // parse MML string
    //

    /** parse inside of #&#64;{..}; */
    public static SiOPMChannelParam parseParam(SiOPMChannelParam param, String dataString) {
        return _setParamByArray(param, _splitDataString(param, dataString, 3, 15, "#@"));
    }

    /** parse inside of #OPL&#64;{..}; */
    public static SiOPMChannelParam parseOPLParam(SiOPMChannelParam param, String dataString) {
        return _setOPLParamByArray(param, _splitDataString(param, dataString, 2, 11, "#OPL@"));
    }

    /** parse inside of #OPM&#64;{..}; */
    public static SiOPMChannelParam parseOPMParam(SiOPMChannelParam param, String dataString) {
        return _setOPMParamByArray(param, _splitDataString(param, dataString, 2, 11, "#OPM@"));
    }

    /** parse inside of #OPN&#64;{..}; */
    public static SiOPMChannelParam parseOPNParam(SiOPMChannelParam param, String dataString) {
        return _setOPNParamByArray(param, _splitDataString(param, dataString, 2, 10, "#OPN@"));
    }

    /** parse inside of #OPX&#64;{..}; */
    public static SiOPMChannelParam parseOPXParam(SiOPMChannelParam param, String dataString) {
        return _setOPXParamByArray(param, _splitDataString(param, dataString, 2, 12, "#OPX@"));
    }

    /** parse inside of #MA&#64;{..}; */
    public static SiOPMChannelParam parseMA3Param(SiOPMChannelParam param, String dataString) {
        return _setMA3ParamByArray(param, _splitDataString(param, dataString, 2, 12, "#MA@"));
    }

    /** parse inside of #AL&#64;{..}; */
    public static SiOPMChannelParam parseALParam(SiOPMChannelParam param, String dataString) {
        return _setALParamByArray(param, _splitDataString(param, dataString, 9, 0, "#AL@"));
    }

    // set by Array
    //

    /** set inside of #&#64;{..}; */
    public static SiOPMChannelParam setParam(SiOPMChannelParam param, Object[] data) {
        return _setParamByArray(_checkOpeCount(param, data.length, 3, 15, "#@"), data);
    }

    /** set inside of #OPL&#64;{..}; */
    public static SiOPMChannelParam setOPLParam(SiOPMChannelParam param, Object[] data) {
        return _setOPLParamByArray(_checkOpeCount(param, data.length, 2, 11, "#OPL@"), data);
    }

    /** set inside of #OPM&#64;{..}; */
    public static SiOPMChannelParam setOPMParam(SiOPMChannelParam param, Object[] data) {
        return _setOPMParamByArray(_checkOpeCount(param, data.length, 2, 11, "#OPM@"), data);
    }

    /** set inside of #OPN&#64;{..}; */
    public static SiOPMChannelParam setOPNParam(SiOPMChannelParam param, Object[] data) {
        return _setOPNParamByArray(_checkOpeCount(param, data.length, 2, 10, "#OPN@"), data);
    }

    /** set inside of #OPX&#64;{..}; */
    public static SiOPMChannelParam setOPXParam(SiOPMChannelParam param, Object[] data) {
        return _setOPXParamByArray(_checkOpeCount(param, data.length, 2, 12, "#OPX@"), data);
    }

    /** set inside of #MA&#64;{..}; */
    public static SiOPMChannelParam setMA3Param(SiOPMChannelParam param, Object[] data) {
        return _setMA3ParamByArray(_checkOpeCount(param, data.length, 2, 12, "#MA@"), data);
    }

    /** set inside of #AL&#64;{..}; */
    public static SiOPMChannelParam setALParam(SiOPMChannelParam param, Object[] data) {
        if (data.length != 9) throw errorToneParameterNotValid("#AL@", 9, 0);
        return _setALParamByArray(param, data);
    }

    // internal functions
    //
    // split dataString of #@ macro
    private static String[] _splitDataString(SiOPMChannelParam param, String dataString, int chParamCount, int opParamCount, String cmd) {
        String[] data;
        int i;

        // parse parameters
        if (dataString.isEmpty()) {
            param.opeCount = 0;
        } else {
            Pattern comrex = Pattern.compile("/\\*.*?\\*/|//.*?[\\r\\n]+");
            data = dataString.replace(comrex.pattern(), "").replace(" ^[^\\d\\-.]+ |[^\\d\\-.]+$ ", "").split("[^\\d\\-.]+ ");
            for (i = 1; i < 5; i++) {
                if (data.length == chParamCount + opParamCount * i) {
                    param.opeCount = i;
                    return data;
                }
            }
            throw errorToneParameterNotValid(cmd, chParamCount, opParamCount);
        }
        return null;
    }

    // check param.opeCount
    private static SiOPMChannelParam _checkOpeCount(SiOPMChannelParam param, int dataLength, int chParamCount, int opParamCount, String cmd) {
        int opeCount = (dataLength - chParamCount) / opParamCount;
        if (opeCount > 4 || opeCount * opParamCount + chParamCount != dataLength)
            throw errorToneParameterNotValid(cmd, chParamCount, opParamCount);
        param.opeCount = opeCount;
        return param;
    }

    // #@
    // alg[0-15], fb[0-7], fbc[0-3],
    // (ws[0-511], ar[0-63], dr[0-63], sr[0-63], rr[0-63], sl[0-15], tl[0-127], ksr[0-3], ksl[0-3], mul[], dt1[0-7], detune[], ams[0-3], phase[-1-255], fixedNote[0-127]) x operator_count
    private static SiOPMChannelParam _setParamByArray(SiOPMChannelParam param, Object[] data) {
        if (param.opeCount == 0) return param;

        param.alg = (int) data[0];
        param.fb = (int) data[1];
        param.fbc = (int) data[2];
        int dataIndex = 3;
        double n;
        int i;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            opp.setPGType(((int) data[dataIndex++]) & 511); // 1
            opp.ar = ((int) data[dataIndex++]) & 63;   // 2
            opp.dr = ((int) data[dataIndex++]) & 63;   // 3
            opp.sr = ((int) data[dataIndex++]) & 63;   // 4
            opp.rr = ((int) data[dataIndex++]) & 63;   // 5
            opp.sl = ((int) data[dataIndex++]) & 15;   // 6
            opp.tl = ((int) data[dataIndex++]) & 127;  // 7
            opp.ksr = ((int) data[dataIndex++]) & 3;    // 8
            opp.ksl = ((int) data[dataIndex++]) & 3;    // 9
            n = (double) data[dataIndex++];
            opp.fmul = (n == 0) ? 64 : (int) (n * 128);      // 10
            opp.dt1 = ((int) data[dataIndex++]) & 7;    // 11
            opp.detune = (int) data[dataIndex++];        // 12
            opp.ams = ((int) data[dataIndex++]) & 3;    // 13
            i = (int) data[dataIndex++];
            opp.phase = (i == -1) ? i : (i & 255);           // 14
            opp.fixedPitch = (((int) data[dataIndex++]) & 127) << 6;  // 15
        }
        return param;
    }

    // #OPL@
    // alg[0-5], fb[0-7],
    // (ws[0-7], ar[0-15], dr[0-15], rr[0-15], egt[0,1], sl[0-15], tl[0-63], ksr[0,1], ksl[0-3], mul[0-15], ams[0-3]) x operator_count
    private static SiOPMChannelParam _setOPLParamByArray(SiOPMChannelParam param, Object[] data) {
        if (param.opeCount == 0) return param;

        int alg = SiMMLTable.getInstance().alg_opl[param.opeCount - 1][(int) data[0] & 15];
        if (alg == -1) throw errorParameterNotValid("#OPL@ algorism", data[0]);

        param.fratio = 133;
        param.alg = alg;
        param.fb = (int) data[1];
        int dataIndex = 2, i;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            opp.setPGType(SiOPMTable.PG_MA3_WAVE + ((int) data[dataIndex++] & 31));    // 1
            opp.ar = (((int) data[dataIndex++]) << 2) & 63;   // 2
            opp.dr = (((int) data[dataIndex++]) << 2) & 63;   // 3
            opp.rr = (((int) data[dataIndex++]) << 2) & 63;   // 4
            // egt=0;decay tone / egt=1;holding tone           5
            opp.sr = (((int) data[dataIndex++]) != 0) ? 0 : opp.rr;
            opp.sl = ((int) data[dataIndex++]) & 15;          // 6
            opp.tl = ((int) data[dataIndex++]) & 63;          // 7
            opp.ksr = (((int) data[dataIndex++]) << 1) & 3;      // 8
            opp.ksl = ((int) data[dataIndex++]) & 3;           // 9
            i = ((int) data[dataIndex++]) & 15;                // 10
            opp.setMul((i == 11 || i == 13) ? (i - 1) : (i == 14) ? (i + 1) : i);
            opp.ams = ((int) data[dataIndex++]) & 3;           // 11
            // multiple
        }
        return param;
    }

    // #OPM@
    // alg[0-7], fb[0-7],
    // (ar[0-31], dr[0-31], sr[0-31], rr[0-15], sl[0-15], tl[0-127], ks[0-3], mul[0-15], dt1[0-7], dt2[0-3], ams[0-3]) x operator_count
    private static SiOPMChannelParam _setOPMParamByArray(SiOPMChannelParam param, Object[] data) {
        if (param.opeCount == 0) return param;

        int alg = SiMMLTable.getInstance().alg_opm[param.opeCount - 1][((int) data[0]) & 15];
        if (alg == -1) throw errorParameterNotValid("#OPN@ algorism", data[0]);

        param.alg = alg;
        param.fb = (int) data[1];
        int dataIndex = 2;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            opp.ar = (((int) data[dataIndex++]) << 1) & 63;       // 1
            opp.dr = (((int) data[dataIndex++]) << 1) & 63;       // 2
            opp.sr = (((int) data[dataIndex++]) << 1) & 63;       // 3
            opp.rr = ((((int) data[dataIndex++]) << 2) + 2) & 63; // 4
            opp.sl = ((int) data[dataIndex++]) & 15;              // 5
            opp.tl = ((int) data[dataIndex++]) & 127;             // 6
            opp.ksr = ((int) data[dataIndex++]) & 3;               // 7
            opp.setMul(((int) data[dataIndex++]) & 15);              // 8
            opp.dt1 = ((int) data[dataIndex++]) & 7;               // 9
            opp.detune = SiOPMTable.getInstance().dt2Table[((int) data[dataIndex++]) & 3];    // 10
            opp.ams = ((int) data[dataIndex++]) & 3;               // 11
        }
        return param;
    }

    // #OPN@
    // alg[0-7], fb[0-7],
    // (ar[0-31], dr[0-31], sr[0-31], rr[0-15], sl[0-15], tl[0-127], ks[0-3], mul[0-15], dt1[0-7], ams[0-3]) x operator_count
    private static SiOPMChannelParam _setOPNParamByArray(SiOPMChannelParam param, Object[] data) {
        if (param.opeCount == 0) return param;

        int alg = SiMMLTable.getInstance().alg_opm[param.opeCount - 1][((int) data[0]) & 15];
        if (alg == -1) throw errorParameterNotValid("#OPN@ algorism", data[0]);

        param.alg = alg;
        param.fb = (int) data[1];
        int dataIndex = 2;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            opp.ar = (((int) data[dataIndex++]) << 1) & 63;       // 1
            opp.dr = (((int) data[dataIndex++]) << 1) & 63;       // 2
            opp.sr = (((int) data[dataIndex++]) << 1) & 63;       // 3
            opp.rr = ((((int) data[dataIndex++]) << 2) + 2) & 63; // 4
            opp.sl = ((int) data[dataIndex++]) & 15;              // 5
            opp.tl = ((int) data[dataIndex++]) & 127;             // 6
            opp.ksr = ((int) data[dataIndex++]) & 3;               // 7
            opp.setMul(((int) data[dataIndex++]) & 15);              // 8
            opp.dt1 = ((int) data[dataIndex++]) & 7;               // 9
            opp.ams = ((int) data[dataIndex++]) & 3;               // 10
        }
        return param;
    }

    // #OPX@
    // alg[0-15], fb[0-7],
    // (ws[0-7], ar[0-31], dr[0-31], sr[0-31], rr[0-15], sl[0-15], tl[0-127], ks[0-3], mul[0-15], dt1[0-7], detune[], ams[0-3]) x operator_count
    private static SiOPMChannelParam _setOPXParamByArray(SiOPMChannelParam param, Object[] data) {
        if (param.opeCount == 0) return param;

        int alg = SiMMLTable.getInstance().alg_opx[param.opeCount - 1][((int) data[0]) & 15];
        if (alg == -1) throw errorParameterNotValid("#OPX@ algorism", data[0]);

        param.alg = (alg & 15);
        param.fb = (int) data[1];
        param.fbc = (alg & 16) != 0 ? 1 : 0;
        int dataIndex = 2, i;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            i = (int) data[dataIndex++];
            opp.setPGType((i < 7) ? (SiOPMTable.PG_MA3_WAVE + (i & 7)) : (SiOPMTable.PG_CUSTOM + (i - 7)));    // 1
            opp.ar = (((int) data[dataIndex++]) << 1) & 63;       // 2
            opp.dr = (((int) data[dataIndex++]) << 1) & 63;       // 3
            opp.sr = (((int) data[dataIndex++]) << 1) & 63;       // 4
            opp.rr = ((((int) data[dataIndex++]) << 2) + 2) & 63; // 5
            opp.sl = ((int) data[dataIndex++]) & 15;              // 6
            opp.tl = ((int) data[dataIndex++]) & 127;             // 7
            opp.ksr = ((int) data[dataIndex++]) & 3;               // 8
            opp.setMul(((int) data[dataIndex++]) & 15);              // 9
            opp.dt1 = ((int) data[dataIndex++]) & 7;               // 10
            opp.detune = ((int) data[dataIndex++]);                // 11
            opp.ams = ((int) data[dataIndex++]) & 3;               // 12
        }
        return param;
    }

    // #MA@
    // alg[0-15], fb[0-7],
    // (ws[0-31], ar[0-15], dr[0-15], sr[0-15], rr[0-15], sl[0-15], tl[0-63], ksr[0,1], ksl[0-3], mul[0-15], dt1[0-7], ams[0-3]) x operator_count
    private static SiOPMChannelParam _setMA3ParamByArray(SiOPMChannelParam param, Object[] data) {
        if (param.opeCount == 0) return param;

        int alg = SiMMLTable.getInstance().alg_ma3[param.opeCount - 1][((int) data[0]) & 15];
        if (alg == -1) throw errorParameterNotValid("#MA@ algorism", data[0]);

        param.fratio = 133;
        param.alg = alg;
        param.fb = (int) data[1];
        int dataIndex = 2, i;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            opp.setPGType(SiOPMTable.PG_MA3_WAVE + (((int) data[dataIndex++]) & 31)); // 1
            opp.ar = (((int) data[dataIndex++]) << 2) & 63;   // 2
            opp.dr = (((int) data[dataIndex++]) << 2) & 63;   // 3
            opp.sr = (((int) data[dataIndex++]) << 2) & 63;   // 4
            opp.rr = (((int) data[dataIndex++]) << 2) & 63;   // 5
            opp.sl = ((int) data[dataIndex++]) & 15;          // 6
            opp.tl = ((int) data[dataIndex++]) & 63;          // 7
            opp.ksr = (((int) data[dataIndex++]) << 1) & 3;      // 8
            opp.ksl = ((int) data[dataIndex++]) & 3;           // 9
            i = ((int) data[dataIndex++]) & 15;                // 10
            opp.setMul((i == 11 || i == 13) ? (i - 1) : (i == 14) ? (i + 1) : i);
            opp.dt1 = ((int) data[dataIndex++]) & 7;           // 11
            opp.ams = ((int) data[dataIndex++]) & 3;           // 12
        }
        return param;
    }

    // #AL@
    // con[0-2], ws1[0-511], ws2[0-511], balance[-64-+64], vco2pitch[]
    // ar[0-63], dr[0-63], sl[0-15], rr[0-63]
    private static SiOPMChannelParam _setALParamByArray(SiOPMChannelParam param, Object[] data) {
        SiOPMOperatorParam opp0 = param.operatorParam[0];
        SiOPMOperatorParam opp1 = param.operatorParam[1];
        int[] tltable = SiOPMTable.getInstance().eg_lv2tlTable;
        int connectionType = (int) data[0];
        int balance = (int) data[3];
        param.opeCount = 5;
        param.alg = (connectionType >= 0 && connectionType <= 2) ? connectionType : 0;
        opp0.setPGType((int) data[1]);
        opp1.setPGType((int) data[2]);
        if (balance > 64) balance = 64;
        else if (balance < -64) balance = -64;
        opp0.tl = tltable[64 - balance];
        opp1.tl = tltable[balance + 64];
        opp0.detune = 0;
        opp1.detune = (int) data[4];

        opp0.ar = ((int) data[5]) & 63;
        opp0.dr = ((int) data[6]) & 63;
        opp0.sr = 0;
        opp0.rr = ((int) data[8]) & 15;
        opp0.sl = ((int) data[7]) & 63;

        return param;
    }

    // get by Array
    //

    /** get number list inside of #&#64;{..}; */
    public static int[] getParam(SiOPMChannelParam param) {
        if (param.opeCount == 0) return null;
        List<Integer> res = new ArrayList<>(List.of(param.alg, param.fb, param.fbc));
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            res.addAll(List.of(opp.pgType, opp.ar, opp.dr, opp.sr, opp.rr, opp.sl, opp.tl, opp.ksr, opp.ksl, opp.getMul(), opp.dt1, opp.detune, opp.ams, opp.phase, opp.fixedPitch >> 6));
        }
        return res.stream().mapToInt(re -> re).toArray();
    }

    /** get number list inside of #OPL&#64;{..}; */
    public static int[] getOPLParam(SiOPMChannelParam param) {
        if (param.opeCount == 0) return null;
        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opl);
        if (alg == -1)
            throw errorParameterNotValid("#OPL@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);
        List<Integer> res = new ArrayList<>(List.of(alg, param.fb));
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            int ws = _pgTypeMA3(opp.pgType);
            int egt = (opp.sr == 0) ? 1 : 0;
            int tl = Math.min(opp.tl, 63);
            if (ws == -1) throw errorParameterNotValid("#OPL@", "SiOPM ws" + opp.pgType);
            res.addAll(List.of(ws, opp.ar >> 2, opp.dr >> 2, opp.rr >> 2, egt, opp.sl, tl, opp.ksr >> 1, opp.ksl, opp.getMul(), opp.ams));
        }
        return res.stream().mapToInt(re -> re).toArray();
    }

    /** get number list inside of #OPM&#64;{..}; */
    public static int[] getOPMParam(SiOPMChannelParam param) {
        if (param.opeCount == 0) return null;
        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opm);
        if (alg == -1)
            throw errorParameterNotValid("#OPM@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);
        List<Integer> res = new ArrayList<>(List.of(alg, param.fb));
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            int dt2 = _dt2OPM(opp.detune);
            res.addAll(List.of(opp.ar >> 1, opp.dr >> 1, opp.sr >> 1, opp.rr >> 2, opp.sl, opp.tl, opp.ksr, opp.getMul(), opp.dt1, dt2, opp.ams));
        }
        return res.stream().mapToInt(re -> re).toArray();
    }

    /** get number list inside of #OPN&#64;{..}; */
    public static int[] getOPNParam(SiOPMChannelParam param) {
        if (param.opeCount == 0) return null;
        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opm);
        if (alg == -1)
            throw errorParameterNotValid("#OPN@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);
        List<Integer> res = new ArrayList<>(List.of(alg, param.fb));
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            res.addAll(List.of(opp.ar >> 1, opp.dr >> 1, opp.sr >> 1, opp.rr >> 2, opp.sl, opp.tl, opp.ksr, opp.getMul(), opp.dt1, opp.ams));
        }
        return res.stream().mapToInt(re -> re).toArray();
    }

    /** get number list inside of #OPX&#64;{..}; */
    public static int[] getOPXParam(SiOPMChannelParam param) {
        if (param.opeCount == 0) return null;
        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opx);
        if (alg == -1)
            throw errorParameterNotValid("#OPX@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);
        List<Integer> res = new ArrayList<>(List.of(alg, param.fb));
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            int ws = _pgTypeMA3(opp.pgType);
            if (ws == -1) throw errorParameterNotValid("#OPX@", "SiOPM ws" + opp.pgType);
            res.addAll(List.of(ws, opp.ar >> 1, opp.dr >> 1, opp.sr >> 1, opp.rr >> 2, opp.sl, opp.tl, opp.ksr, opp.getMul(), opp.dt1, opp.detune, opp.ams));
        }
        return res.stream().mapToInt(re -> re).toArray();
    }

    /** get number list inside of #MA&#64;{..}; */
    public static int[] getMA3Param(SiOPMChannelParam param) {
        if (param.opeCount == 0) return null;
        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_ma3);
        if (alg == -1)
            throw errorParameterNotValid("#MA@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);
        List<Integer> res = new ArrayList<>(List.of(alg, param.fb));
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            int ws = _pgTypeMA3(opp.pgType);
            int tl = Math.min(opp.tl, 63);
            if (ws == -1) throw errorParameterNotValid("#MA@", "SiOPM ws" + opp.pgType);
            res.addAll(List.of(ws, opp.ar >> 2, opp.dr >> 2, opp.sr >> 2, opp.rr >> 2, opp.sl, tl, opp.ksr >> 1, opp.ksl, opp.getMul(), opp.dt1, opp.ams));
        }
        return res.stream().mapToInt(re -> re).toArray();
    }

    /** get number list inside of #AL&#64;{..}; */
    public static int[] getALParam(SiOPMChannelParam param) {
        if (param.opeCount != 5) return null;
        SiOPMOperatorParam opp0 = param.operatorParam[0];
        SiOPMOperatorParam opp1 = param.operatorParam[1];
        return new int[] {
                param.alg, opp0.pgType, opp1.pgType, _balanceAL(opp0.tl, opp1.tl), opp1.detune, opp0.ar, opp0.dr, opp0.sl, opp0.rr
        };
    }

    // reconstruct MML string from channel parameters
    //

    /**
     * reconstruct mml text of #&#64;{..}.
     *
     * @param param     SiOPMChannelParam for MML reconstruction
     * @param separator String to separate each number
     * @param lineEnd   String to separate line end
     * @param comment   comment text inserting after 'fbc' number
     * @return text formatted as "{..}".
     */
    public static String mmlParam(SiOPMChannelParam param, String separator, String lineEnd, String comment) {
        if (param.opeCount == 0) return "";

        StringBuilder mml = new StringBuilder();
        Map<String, Integer> res = _checkDigit(param);
        mml.append("{");
        mml.append(param.alg).append(separator);
        mml.append(param.fb).append(separator);
        mml.append(param.fbc);
        if (comment != null) {
            if (lineEnd.equals("\n")) mml.append(" // ").append(comment);
            else mml.append("/* ").append(comment).append(" */");
        }
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            mml.append(lineEnd);
            mml.append(_str(opp.pgType, res.get("ws"))).append(separator);
            mml.append(_str(opp.ar, 2)).append(separator);
            mml.append(_str(opp.dr, 2)).append(separator);
            mml.append(_str(opp.sr, 2)).append(separator);
            mml.append(_str(opp.rr, 2)).append(separator);
            mml.append(_str(opp.sl, 2)).append(separator);
            mml.append(_str(opp.tl, res.get("tl"))).append(separator);
            mml.append(opp.ksr).append(separator);
            mml.append(opp.ksl).append(separator);
            mml.append(_str(opp.getMul(), 2)).append(separator);
            mml.append(opp.dt1).append(separator);
            mml.append(_str(opp.detune, res.get("dt"))).append(separator);
            mml.append(opp.ams).append(separator);
            mml.append(_str(opp.phase, res.get("ph"))).append(separator);
            mml.append(_str(opp.fixedPitch >> 6, res.get("fn")));
        }
        mml.append("}");

        return mml.toString();
    }

    /**
     * reconstruct mml text of #OPL&#64;{..};
     *
     * @param param     SiOPMChannelParam for MML reconstruction
     * @param separator String to separate each number
     * @param lineEnd   String to separate line end
     * @param comment   comment text inserting after 'fbc' number
     * @return text formatted as "{..}".
     */
    public static String mmlOPLParam(SiOPMChannelParam param, String separator, String lineEnd, String comment) {
        if (param.opeCount == 0) return "";

        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opl);
        if (alg == -1)
            throw errorParameterNotValid("#OPL@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);

        StringBuilder mml = new StringBuilder();
        Map<String, Integer> res = _checkDigit(param);
        mml.append("{").append(alg).append(separator).append(param.fb);
        if (comment != null) {
            if (lineEnd.equals("\n")) mml.append(" // ").append(comment);
            else mml.append("/* ").append(comment).append(" */");
        }

        int pgType, tl;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            mml.append(lineEnd);
            pgType = _pgTypeMA3(opp.pgType);
            if (pgType == -1) throw errorParameterNotValid("#OPL@", "SiOPM ws" + opp.pgType);
            mml.append(pgType).append(separator);              // ws
            mml.append(_str(opp.ar >> 2, 2)).append(separator);        // ar
            mml.append(_str(opp.dr >> 2, 2)).append(separator);        // dr
            mml.append(_str(opp.rr >> 2, 2)).append(separator);        // rr
            mml.append((opp.sr == 0) ? "1" : "0").append(separator); // egt
            mml.append(_str(opp.sl, 2)).append(separator);                 // sl
            mml.append(_str(Math.min(opp.tl, 63), 2)).append(separator);  // tl
            mml.append(opp.ksr >> 1).append(separator);              // ksr
            mml.append(opp.ksl).append(separator);                 // ksl
            mml.append(_str(opp.getMul(), 2)).append(separator);                // mul
            mml.append(opp.ams);                             // ams
        }
        mml.append("}");

        return mml.toString();
    }

    /**
     * reconstruct mml text of #OPM&#64;{..};
     *
     * @param param     SiOPMChannelParam for MML reconstruction
     * @param separator String to separate each number
     * @param lineEnd   String to separate line end
     * @param comment   comment text inserting after 'fbc' number
     * @return text formatted as "{..}".
     */
    public static String mmlOPMParam(SiOPMChannelParam param, String separator, String lineEnd, String comment) {
        if (param.opeCount == 0) return "";

        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opm);
        if (alg == -1)
            throw errorParameterNotValid("#OPM@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);

        StringBuilder mml = new StringBuilder();
        Map<String, Integer> res = _checkDigit(param);
        mml.append("{").append(alg).append(separator).append(param.fb);
        if (comment != null) {
            if (lineEnd.equals("\n")) mml.append(" // ").append(comment);
            else mml.append("/* ").append(comment).append(" */");
        }

        int pgType, tl;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            mml.append(lineEnd);
            // if (opp.pgType != 0) throw errorParameterNotValid("#OPM@", "SiOPM ws" + String(opp.pgType));
            mml.append(_str(opp.ar >> 1, 2)).append(separator);        // ar
            mml.append(_str(opp.dr >> 1, 2)).append(separator);        // dr
            mml.append(_str(opp.sr >> 1, 2)).append(separator);        // sr
            mml.append(_str(opp.rr >> 2, 2)).append(separator);        // rr
            mml.append(_str(opp.sl, 2)).append(separator);             // sl
            mml.append(_str(opp.tl, res.get("tl"))).append(separator);        // tl
            mml.append(opp.ksl).append(separator);             // ksl
            mml.append(_str(opp.getMul(), 2)).append(separator);            // mul
            mml.append(opp.dt1).append(separator);             // dt1
            mml.append(_dt2OPM(opp.detune)).append(separator); // dt2
            mml.append(opp.ams);                         // ams
        }
        mml.append("}");

        return mml.toString();
    }

    /**
     * reconstruct mml text of #OPN&#64;{..};
     *
     * @param param     SiOPMChannelParam for MML reconstruction
     * @param separator String to separate each number
     * @param lineEnd   String to separate line end
     * @param comment   comment text inserting after 'fbc' number
     * @return text formatted as "{..}".
     */
    public static String mmlOPNParam(SiOPMChannelParam param, String separator, String lineEnd, String comment) {
        if (param.opeCount == 0) return "";

        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opm);
        if (alg == -1)
            throw errorParameterNotValid("#OPN@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);

        StringBuilder mml = new StringBuilder();
        Map<String, Integer> res = _checkDigit(param);
        mml.append("{").append(alg).append(separator).append(param.fb);
        if (comment != null) {
            if (lineEnd.equals("\n")) mml.append(" // ").append(comment);
            else mml.append("/* ").append(comment).append(" */");
        }

        int pgType, tl;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            mml.append(lineEnd);
            // if (opp.pgType != 0) throw errorParameterNotValid("#OPN@", "SiOPM ws" + String(opp.pgType));
            mml.append(_str(opp.ar >> 1, 2)).append(separator);    // ar
            mml.append(_str(opp.dr >> 1, 2)).append(separator);    // dr
            mml.append(_str(opp.sr >> 1, 2)).append(separator);    // sr
            mml.append(_str(opp.rr >> 2, 2)).append(separator);    // rr
            mml.append(_str(opp.sl, 2)).append(separator);         // sl
            mml.append(_str(opp.tl, res.get("tl"))).append(separator);    // tl
            mml.append(opp.ksl).append(separator);         // ksl
            mml.append(_str(opp.getMul(), 2)).append(separator);        // mul
            mml.append(opp.dt1).append(separator);         // dt1
            mml.append(opp.ams);                     // ams
        }
        mml.append("}");

        return mml.toString();
    }

    /**
     * reconstruct mml text of #OPX&#64;{..};
     *
     * @param param     SiOPMChannelParam for MML reconstruction
     * @param separator String to separate each number
     * @param lineEnd   String to separate line end
     * @param comment   comment text inserting after 'fbc' number
     * @return text formatted as "{..}".
     */
    public static String mmlOPXParam(SiOPMChannelParam param, String separator, String lineEnd, String comment) {
        if (param.opeCount == 0) return "";

        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_opx);
        if (alg == -1)
            throw errorParameterNotValid("#OPX@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);

        StringBuilder mml = new StringBuilder();
        Map<String, Integer> res = _checkDigit(param);
        mml.append("{").append(alg).append(separator).append(param.fb);
        if (comment != null) {
            if (lineEnd.equals("\n")) mml.append(" // ").append(comment);
            else mml.append("/* ").append(comment).append(" */");
        }

        int pgType, tl;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            mml.append(lineEnd);
            pgType = _pgTypeMA3(opp.pgType);
            if (pgType == -1) throw errorParameterNotValid("#OPX@", "SiOPM ws" + opp.pgType);
            mml.append(pgType).append(separator);              // ws
            mml.append(_str(opp.ar >> 1, 2)).append(separator);        // ar
            mml.append(_str(opp.dr >> 1, 2)).append(separator);        // dr
            mml.append(_str(opp.sr >> 1, 2)).append(separator);        // sr
            mml.append(_str(opp.rr >> 2, 2)).append(separator);        // rr
            mml.append(_str(opp.sl, 2)).append(separator);             // sl
            mml.append(_str(opp.tl, res.get("tl"))).append(separator);        // tl
            mml.append(opp.ksl).append(separator);             // ksl
            mml.append(_str(opp.getMul(), 2)).append(separator);            // mul
            mml.append(opp.dt1).append(separator);             // dt1
            mml.append(_str(opp.detune, res.get("dt"))).append(separator);    // det
            mml.append(opp.ams);                         // ams
        }
        mml.append("}");

        return mml.toString();
    }

    /**
     * reconstruct mml text of #MA&#64;{..};
     *
     * @param param     SiOPMChannelParam for MML reconstruction
     * @param separator String to separate each number
     * @param lineEnd   String to separate line end
     * @param comment   comment text inserting after 'fbc' number
     * @return text formatted as "{..}".
     */
    public static String mmlMA3Param(SiOPMChannelParam param, String separator, String lineEnd, String comment) {
        if (param.opeCount == 0) return "";

        int alg = _checkAlgorism(param.opeCount, param.alg, SiMMLTable.getInstance().alg_ma3);
        if (alg == -1)
            throw errorParameterNotValid("#MA@ alg", "SiOPM opc" + param.opeCount + "/alg" + param.alg);

        StringBuilder mml = new StringBuilder();
        Map<String, Integer> res = _checkDigit(param);
        mml.append("{").append(alg).append(separator).append(param.fb);
        if (comment != null) {
            if (lineEnd.equals("\n")) mml.append(" // ").append(comment);
            else mml.append("/* ").append(comment).append(" */");
        }

        int pgType, tl;
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            mml.append(lineEnd);
            pgType = _pgTypeMA3(opp.pgType);
            if (pgType == -1) throw errorParameterNotValid("#MA@", "SiOPM ws" + opp.pgType);
            mml.append(_str(pgType, 2)).append(separator);                 // ws
            mml.append(_str(opp.ar >> 2, 2)).append(separator);            // ar
            mml.append(_str(opp.dr >> 2, 2)).append(separator);            // dr
            mml.append(_str(opp.sr >> 2, 2)).append(separator);            // sr
            mml.append(_str(opp.rr >> 2, 2)).append(separator);            // rr
            mml.append(_str(opp.sl, 2)).append(separator);                 // sl
            mml.append(_str(Math.min(opp.tl, 63), 2)).append(separator);  // tl
            mml.append(opp.ksr >> 1).append(separator);              // ksr
            mml.append(opp.ksl).append(separator);                 // ksl
            mml.append(_str(opp.getMul(), 2)).append(separator);                // mul
            mml.append(opp.dt1).append(separator);                 // dt1
            mml.append(opp.ams);                             // ams
        }
        mml.append("}");

        return mml.toString();
    }

    /**
     * reconstruct mml text of #AL&#64;{..};
     *
     * @param param     SiOPMChannelParam for MML reconstruction
     * @param separator String to separate each number
     * @param lineEnd   String to separate line end
     * @param comment   comment text inserting after 'fbc' number
     * @return text formatted as "{..}".
     */
    public static String mmlALParam(SiOPMChannelParam param, String separator, String lineEnd, String comment) {
        if (param.opeCount != 5) return null;

        SiOPMOperatorParam opp0 = param.operatorParam[0];
        SiOPMOperatorParam opp1 = param.operatorParam[1];
        String mml = "";
        mml += "{" + param.alg + separator;
        mml += opp0.pgType + separator;
        mml += opp1.pgType + separator;
        mml += _balanceAL(opp0.tl, opp1.tl) + separator;
        mml += opp1.detune + separator;
        if (comment != null) {
            if (lineEnd.equals("\n")) mml += " // " + comment;
            else mml += "/* " + comment + " */";
        }
        mml += lineEnd + opp0.ar + separator;
        mml += opp0.dr + separator;
        mml += opp0.sl + separator;
        mml += String.valueOf(opp0.rr);
        mml += "}";

        return mml;
    }

    // extract system command from mml
    //

    /**
     * extract system command from mml
     *
     * @param mml mml text
     * @return extracted command list. the mml of "#CMD1{cont}pfx;" is converted to the Object as {command:"CMD", number:1, content:"cont", postfix:"pfx"}.
     */
    public static List<Map<String, Object>> extractSystemCommand(String mml) {
        Pattern comrex = Pattern.compile("/\\*.*?\\*/|//.*?[\\r\\n]+");
        Pattern seqrex = Pattern.compile("(#[A-Z@]+)([^;{]*(\\{.*?})?[^;]*);");
        Pattern prmrex = Pattern.compile("\\s*(\\d*)\\s*(\\{(.*?)\\\\})?(.*)");
        Matcher res, res2;
        String cmd;
        int num;
        String dat, pfx;
        List<Map<String, Object>> cmds = new ArrayList<>();

        // remove comments
        mml += "\n";
        mml = mml.replace(comrex.pattern(), "") + ";";

        // parse system command
        while ((res = seqrex.matcher(mml)).matches()) {
            cmd = String.valueOf(res.group(1));
            if (!res.group(2).isEmpty()) {
//                prmrex.lastIndex = 0; // TODO
                res2 = prmrex.matcher(res.group(2));
                num = Integer.parseInt(res2.group(1));
                dat = (res2.group(2) == null) ? "" : String.valueOf(res2.group(3));
                pfx = String.valueOf(res2.group(4));
            } else {
                num = 0;
                dat = "";
                pfx = "";
            }
            cmds.add(Map.of("command", cmd, "number", num, "content", dat, "postfix", pfx));
        }
        return cmds;
    }

    // Voice parameters (filter, lfo, portamento, gate time, sweep)
    //

    /**
     * parse voice setting mml
     *
     * @param voice     voice to update
     * @param mml       setting mml
     * @param envelopes envelope list to pickup envelope
     * @return ((argument) same) of 'voice'.
     */
    public static SiMMLVoice parseVoiceSetting(SiMMLVoice voice, String mml, SiMMLEnvelopTable[] envelopes) {
        int i, j;
        String cmd = "(%[fvx]|@[fpqv]|@er|@lfo|kt?|m[ap]|_?@@|_?n[aptf]|po|p|q|s|x|v)";
        StringBuilder ags = new StringBuilder("(-?\\d*)");
        for (i = 0; i < 10; i++) ags.append("(\\s*,\\s*(-?\\d*))?");
        Pattern rex = Pattern.compile(cmd + ags);
        Matcher res = rex.matcher(mml);
        SiOPMChannelParam param = voice.channelParam;
        while (res.matches()) {
            switch (res.group(1)) {
                case "@f":
                    param.cutoff = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 128;
                    param.resonance = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 0;
                    param.far = (!res.group(6).isEmpty()) ? Integer.parseInt(res.group(6)) : 0;
                    param.fdr1 = (!res.group(8).isEmpty()) ? Integer.parseInt(res.group(8)) : 0;
                    param.fdr2 = (!res.group(10).isEmpty()) ? Integer.parseInt(res.group(10)) : 0;
                    param.frr = (!res.group(12).isEmpty()) ? Integer.parseInt(res.group(12)) : 0;
                    param.fdc1 = (!res.group(14).isEmpty()) ? Integer.parseInt(res.group(14)) : 128;
                    param.fdc2 = (!res.group(16).isEmpty()) ? Integer.parseInt(res.group(16)) : 64;
                    param.fsc = (!res.group(18).isEmpty()) ? Integer.parseInt(res.group(18)) : 32;
                    param.frc = (!res.group(20).isEmpty()) ? Integer.parseInt(res.group(20)) : 128;
                    break;
                case "@lfo":
                    param.setLfoFrame((!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 30);
                    param.lfoWaveShape = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : SiOPMTable.LFO_WAVE_TRIANGLE;
                    break;
                case "ma":
                    voice.amDepth = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    voice.amDepthEnd = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 0;
                    voice.amDelay = (!res.group(6).isEmpty()) ? Integer.parseInt(res.group(6)) : 0;
                    voice.amTerm = (!res.group(8).isEmpty()) ? Integer.parseInt(res.group(8)) : 0;
                    param.amd = voice.amDepth;
                    break;
                case "mp":
                    voice.pmDepth = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    voice.pmDepthEnd = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 0;
                    voice.pmDelay = (!res.group(6).isEmpty()) ? Integer.parseInt(res.group(6)) : 0;
                    voice.pmTerm = (!res.group(8).isEmpty()) ? Integer.parseInt(res.group(8)) : 0;
                    param.pmd = voice.pmDepth;
                    break;
                case "po":
                    voice.portamento = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 30;
                    break;
                case "q":
                    voice.defaultGateTime = (!res.group(2).isEmpty()) ? (Integer.parseInt(res.group(2)) * 0.125) : Double.NaN;
                    break;
                case "s":
                    //[releaseRate] = (res.group(2) != "") ? int(res.group(2)) : 0;
                    voice.releaseSweep = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 0;
                    break;

                case "%f":
                    voice.channelParam.filterType = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    break;
                case "@er":
                    for (i = 0; i < 4; i++) voice.channelParam.operatorParam[i].erst = (!res.group(2).equals("1"));
                    break;
                case "k":
                    voice.pitchShift = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    break;
                case "kt":
                    voice.noteShift = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    break;

                case "@v":
                    voice.channelParam.volumes[0] = (!res.group(2).isEmpty()) ? (Integer.parseInt(res.group(2)) * 0.0078125) : 0.5;
                    voice.channelParam.volumes[1] = (!res.group(4).isEmpty()) ? (Integer.parseInt(res.group(4)) * 0.0078125) : 0;
                    voice.channelParam.volumes[2] = (!res.group(6).isEmpty()) ? (Integer.parseInt(res.group(6)) * 0.0078125) : 0;
                    voice.channelParam.volumes[3] = (!res.group(8).isEmpty()) ? (Integer.parseInt(res.group(8)) * 0.0078125) : 0;
                    voice.channelParam.volumes[4] = (!res.group(10).isEmpty()) ? (Integer.parseInt(res.group(10)) * 0.0078125) : 0;
                    voice.channelParam.volumes[5] = (!res.group(12).isEmpty()) ? (Integer.parseInt(res.group(12)) * 0.0078125) : 0;
                    voice.channelParam.volumes[6] = (!res.group(14).isEmpty()) ? (Integer.parseInt(res.group(14)) * 0.0078125) : 0;
                    voice.channelParam.volumes[7] = (!res.group(16).isEmpty()) ? (Integer.parseInt(res.group(16)) * 0.0078125) : 0;
                    break;
                case "p":
                    voice.channelParam.pan = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) * 16 : 64;
                    break;
                case "@p":
                    voice.channelParam.pan = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 64;
                    break;
                case "v":
                    voice.velocity = (!res.group(2).isEmpty()) ? (Integer.parseInt(res.group(2)) << voice.vcommandShift) : 256;
                    break;
                case "x":
                    voice.expression = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 128;
                    break;

                case "%v":
                    voice.velocityMode = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    voice.vcommandShift = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 4;
                    break;
                case "%x":
                    voice.expressionMode = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    break;
                case "@q":
                    voice.defaultGateTicks = (!res.group(2).isEmpty()) ? Integer.parseInt(res.group(2)) : 0;
                    voice.defaultKeyOnDelayTicks = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 0;
                    break;

                case "@@":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null && i >= 0 && i < 255) {
                        voice.noteOnToneEnvelop = envelopes[i];
                        voice.noteOnToneEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "na":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOnAmplitudeEnvelop = envelopes[i];
                        voice.noteOnAmplitudeEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "np":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOnPitchEnvelop = envelopes[i];
                        voice.noteOnPitchEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "nt":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOnNoteEnvelop = envelopes[i];
                        voice.noteOnNoteEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "nf":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOnFilterEnvelop = envelopes[i];
                        voice.noteOnFilterEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "_@@":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOffToneEnvelop = envelopes[i];
                        voice.noteOffToneEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "_na":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOffAmplitudeEnvelop = envelopes[i];
                        voice.noteOffAmplitudeEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "_np":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOffPitchEnvelop = envelopes[i];
                        voice.noteOffPitchEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "_nt":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOffNoteEnvelop = envelopes[i];
                        voice.noteOffNoteEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
                case "_nf":
                    i = Integer.parseInt(res.group(2));
                    if (envelopes != null  && i >= 0 && i < 255) {
                        voice.noteOffFilterEnvelop = envelopes[i];
                        voice.noteOffFilterEnvelopStep = (Integer.parseInt(res.group(4)) > 0) ? Integer.parseInt(res.group(4)) : 1;
                    }
                    break;
            }
            res = rex.matcher(mml);
        }
        return voice;
    }

    /** reconstruct voice setting mml (except for channel operator parameters and envelopes) */
    public static String mmlVoiceSetting(SiMMLVoice voice) {
        StringBuilder mml = new StringBuilder();
        SiOPMChannelParam param = voice.channelParam;
        int i;
        if (voice.channelParam.filterType > 0) mml.append("%f").append(voice.channelParam.filterType);
        if (param.cutoff < 128 || param.resonance > 0 || param.far > 0 || param.frr > 0) {
            mml.append("@f").append(param.cutoff).append(",").append(param.resonance);
            if (param.far > 0 || param.frr > 0) {
                mml.append(",").append(param.far).append(",").append(param.fdr1).append(",").append(param.fdr2).append(",").append(param.frr);
                mml.append(",").append(param.fdc1).append(",").append(param.fdc2).append(",").append(param.fsc).append(",").append(param.frc);
            }
        }
        if (voice.amDepth > 0 || voice.amDepthEnd > 0 || param.amd > 0 || voice.pmDepth > 0 || voice.pmDepthEnd > 0 || param.pmd > 0) {
            int lfo = param.getLfoFrame(), ws = param.lfoWaveShape;
            if (lfo != 30 || ws != SiOPMTable.LFO_WAVE_TRIANGLE) {
                mml.append("@lfo").append(lfo);
                if (ws != SiOPMTable.LFO_WAVE_TRIANGLE) mml.append(",").append(ws);
            }
            if (voice.amDepth > 0 || voice.amDepthEnd > 0) {
                mml.append("ma").append(voice.amDepth);
                if (voice.amDepthEnd > 0) mml.append(",").append(voice.amDepthEnd);
                if (voice.amDelay > 0 || voice.amTerm > 0) mml.append(",").append(voice.amDelay);
                if (voice.amTerm > 0) mml.append(",").append(voice.amTerm);
            } else if (param.amd > 0) {
                mml.append("ma").append(param.amd);
            }
            if (voice.pmDepth > 0 || voice.pmDepthEnd > 0) {
                mml.append("mp").append(voice.pmDepth);
                if (voice.pmDepthEnd > 0) mml.append(",").append(voice.pmDepthEnd);
                if (voice.pmDelay > 0 || voice.pmTerm > 0) mml.append(",").append(voice.pmDelay);
                if (voice.pmTerm > 0) mml.append(",").append(voice.pmTerm);
            } else if (param.pmd > 0) {
                mml.append("mp").append(param.pmd);
            }
        }
        if (voice.velocityMode != 0 || voice.vcommandShift != 4) {
            mml.append("%v").append(voice.velocityMode).append(",").append(voice.vcommandShift);
        }
        if (voice.expressionMode != 0) mml.append("%x").append(voice.expressionMode);
        if (voice.portamento > 0) mml.append("po").append(voice.portamento);
        if (!Double.isNaN(voice.defaultGateTime)) mml.append("q").append((int) (voice.defaultGateTime * 8));
        if (voice.defaultGateTicks > 0 || voice.defaultKeyOnDelayTicks > 0) {
            mml.append("@q").append(voice.defaultGateTicks).append(",").append(voice.defaultKeyOnDelayTicks);
        }
        if (voice.releaseSweep > 0) mml.append("s,").append(voice.releaseSweep);
        if (voice.channelParam.operatorParam[0].erst) mml.append("@er1");
        if (voice.pitchShift != 0) mml.append("k").append(voice.pitchShift);
        if (voice.noteShift != 0) mml.append("kt").append(voice.noteShift);
        if (voice.updateVolumes) {
            int ch = (voice.channelParam.volumes[0] == 0.5) ? 0 : 1;
            for (i = 1; i < 8; i++) if (voice.channelParam.volumes[i] != 0) ch = i + 1;
            if (i != 0) {
                mml.append("@v");
                if (voice.channelParam.volumes[0] != 0.5)
                    mml.append((int) (voice.channelParam.volumes[0] * 128));
                for (i = 1; i < ch; i++) {
                    if (voice.channelParam.volumes[i] != 0)
                        mml.append(",").append((int) (voice.channelParam.volumes[i] * 128));
                }
            }
            if (voice.channelParam.pan != 64) {
                if ((voice.channelParam.pan & 15) != 0) mml.append("@p").append(voice.channelParam.pan - 64);
                else mml.append("p").append(voice.channelParam.pan >> 4);
            }
            if (voice.velocity != 256) mml.append("v").append(voice.velocity >> voice.vcommandShift);
            if (voice.expression != 128) mml.append("@v").append(voice.expression);
        }

        return mml.toString();
    }

    // envelop table
    //

    public static class TableNumbersResult {
        public SLLint head;
        public SLLint tail;
        public int length;
        public boolean repeated;
        public TableNumbersResult(SLLint h, SLLint t, int l, boolean r) { this.head=h; this.tail=t; this.length=l; this.repeated=r; }
    }

    /**
     * parse mml of envelop and wave table numbers.
     *
     * @param tableNumbers String of table numbers
     * @param postfix      String of postfix
     * @param maxIndex     maximum size of envelop table
     * @return this instance
     */
    public static TableNumbersResult parseTableNumbers(String tableNumbers, String postfix, int maxIndex) {
        int index = 0, i, imax, j, v, ti0, ti1;
        double tr, t, s;
        int jmax;
        SLLint last, rep;
        Pattern regexp;
        Matcher res;
        String[] array;
        int[] itpl = null;
        List<SLLint> loopStac = new ArrayList<>();
        SLLint tempNumberList = SLLint.alloc(0);
        SLLint loopHead, loopTail, l;

        // initialize
        last = tempNumberList;
        rep = null;

        // magnification
        regexp = Pattern.compile(" (\\d +)?(\\*(- ?[\\d.]+))?(([+-]) ([\\d.]+))?");
        res = regexp.matcher(postfix);
        jmax = (res.group(1) != null) ? Integer.parseInt(res.group(1)) : 1;
        double r = (res.group(2) != null) ? Double.parseDouble(res.group(3)) : 1;
        double o = (res.group(4) != null) ? res.group(5).equals("+") ? Double.parseDouble(res.group(6)) : -Double.parseDouble(res.group(6)) : 0;

        // res.group(1);(n..),m {res.group(2);n.., res.group(3);m} / res.group(4);n / res.group(5);|[] / res.group(6); ]n
        regexp = Pattern.compile("\\s * (\\(\\s * ([,\\-\\d\\s]+)\\)[,\\s]*(\\d +))|(- ?\\d +)|(\\||\\[|\\](\\d *))");
        res = regexp.matcher(tableNumbers);
        while (res.matches() && index < maxIndex) {
            if (res.group(1) != null) {
                // interpolation "(res.group(2)..),res.group(3)"
                array = String.valueOf(res.group(2)).split( "[,\\s]+ ");
                imax = Integer.parseInt(res.group(3));
                if (imax < 2 || array.length < 1) throw errorParameterNotValid("Table MML", tableNumbers);
                itpl = new int[array.length];
                for (i = 0; i < itpl.length; i++) {
                    itpl[i] = Integer.parseInt(array[i]);
                }
                if (itpl.length > 1) {
                    t = 0;
                    s = (double) (itpl.length - 1) / imax;
                    for (i = 0; i < imax && index < maxIndex; i++) {
                        ti0 = (int) t;
                        ti1 = ti0 + 1;
                        tr = t - (double) (ti0);
                        v = (int) (itpl[ti0] * (1 - tr) + itpl[ti1] * tr + 0.5);
                        v = (int) (v * r + o + 0.5);
                        for (j = 0; j < jmax; j++, index++) {
                            last.next = SLLint.alloc(v);
                            last = last.next;
                        }
                        t += s;
                    }
                } else {
                    // repeat
                    v = (int) (itpl[0] * r + o + 0.5);
                    for (i = 0; i < imax && index < maxIndex; i++) {
                        for (j = 0; j < jmax; j++, index++) {
                            last.next = SLLint.alloc(v);
                            last = last.next;
                        }
                    }
                }
            } else if (res.group(4) != null) {
                // single number
                v = (int) (Integer.parseInt(res.group(4)) * r + o + 0.5);
                for (j = 0; j < jmax; j++) {
                    last.next = SLLint.alloc(v);
                    last = last.next;
                }
                index++;
            } else if (res.group(5) != null) {
                switch (res.group(5)) {
                    case "|": // repeat point
                        rep = last;
                        break;
                    case "[": // begin loop
                        loopStac.add(last);
                        break;
                    default: // end loop "]n"
                        if (loopStac.isEmpty()) throw errorParameterNotValid("Table MML's Loop", tableNumbers);
                        loopHead = loopStac.remove(loopStac.size() - 1).next;
                        if (loopHead == null) throw errorParameterNotValid("Table MML's Loop", tableNumbers);
                        loopTail = last;
                        for (j = Integer.parseInt(res.group(6)) != 0 ? Integer.parseInt(res.group(6)) : 2; j > 0; --j) {
                            for (l = loopHead; l != loopTail.next; l = l.next) {
                                last.next = SLLint.alloc(l.i);
                                last = last.next;
                            }
                        }
                        break;
                }
            } else {
                // unknown error
                throw errorUnknown("@parseWav()");
            }
            res = regexp.matcher(tableNumbers);
        }

        //for(var e:SLLint=tempNumberList.next; e!=null; e=e.next) { trace(e.i); }

        if (rep != null) last.next = rep.next;
        return new TableNumbersResult(tempNumberList.next, last, index, rep != null);
    }

    // wave table mml parser
    //

    /**
     * parse #WAV data
     *
     * @param tableNumbers number string of #WAV command.
     * @param postfix      postfix string of #WAV command.
     * @return vector of Number in the range of [-1,1]
     */
    public static double[] parseWAV(String tableNumbers, String postfix) {
        int i, imax, v;

        TableNumbersResult res = Translator.parseTableNumbers(tableNumbers, postfix, 1024);
        SLLint num = res.head;
        for (imax = 2; imax < 1024; imax <<= 1) {
            if (imax >= res.length) break;
        }

        List<Double> wav = new ArrayList<>(imax);
        for (i = 0; i < imax && num != null; i++) {
            v = (int)((num.i + 0.5) * 0.0078125);
            wav.add((double) ((v > 1) ? 1 : Math.max(v, -1)));
            num = num.next;
        }
        for (; i < imax; i++) {
            wav.set(i, 0.);
        }

        return IntStream.range(0, imax).mapToDouble(wav::get).toArray();
    }

    /**
     * parse #WAVB data
     *
     * @param hex hex string of #WAVB command.
     * @return vector of Number in the range of [-1,1]
     */
    public static double[] parseWAVB(String hex) {
        int ub, i, imax;
        double[] wav;
        hex = hex.replace( "\\s + ", "");
        imax = hex.length() >> 1;
        wav = new double[imax];
        for (i = 0; i < imax; i++) {
            ub = parseInt(hex.substring(i << 1, 2), 16);
            wav[i] = (ub < 128) ? (ub * 0.0078125) : ((ub - 256) * 0.0078125);
        }
        return wav;
    }

    // pcm mml parser
    //

    /**
     * parse mml text of sampler wave setting (#SAMPLER system command).
     *
     * @param table           table to set sampler wave
     * @param noteNumber      note number to set sample
     * @param mml             comma separated text of #SAMPLER system command
     * @param soundReferTable reference table of Sound instances.
     * @return true when success to find wave from soundReferTable.
     */
    public static boolean parseSamplerWave(SiOPMWaveSamplerTable table, int noteNumber, String mml, Object soundReferTable) {
        String[] args = mml.split( "\\s *,\\s * ");
        String waveID = String.valueOf(args[0]);
        boolean ignoreNoteOff = (args[1] != null && !args[1].isEmpty()) ? Boolean.getBoolean(args[1]) : false;
        int pan = (args[2] != null && !args[2].isEmpty()) ? Integer.parseInt(args[2]) : 0,
                channelCount = (args[3] != null && !args[3].isEmpty()) ? Integer.parseInt(args[3]) : 2,
                startPoint = (args[4] != null && !args[4].isEmpty()) ? Integer.parseInt(args[4]) : -1,
                endPoint = (args[5] != null && !args[5].isEmpty()) ? Integer.parseInt(args[5]) : -1,
                loopPoint = (args[6] != null && !args[6].isEmpty()) ? Integer.parseInt(args[6]) : -1;
        if (soundReferTable instanceof Map<?, ?> tableMap && tableMap.containsKey(waveID)) {
            Object wave = tableMap.get(waveID);
            SiOPMWaveSamplerData sample = new SiOPMWaveSamplerData(wave, ignoreNoteOff, pan, 2, channelCount, null);
            sample.slice(startPoint, endPoint, loopPoint);
            table.setSample(sample, noteNumber, -1);
            return true;
        }
        return false;
    }

    /**
     * parse mml text of pcm wave setting (#PCMWAVE system command).
     *
     * @param table           table to set PCM wave
     * @param mml             comma separated values of #PCMWAVE system command
     * @param soundReferTable reference table of Sound instances.
     * @return true when success to find wave from soundReferTable.
     */
    public static boolean parsePCMWave(SiOPMWavePCMTable table, String mml, Object soundReferTable) {
        String[] args = mml.split( "\\s *,\\s * ");
        String waveID = String.valueOf(args[0]);
        int samplingNote = (args[1] != null && !args[1].isEmpty()) ? Integer.parseInt(args[1]) : 69,
                keyRangeFrom = (args[2] != null && !args[2].isEmpty()) ? Integer.parseInt(args[2]) : 0,
                keyRangeTo = (args[3] != null && !args[3].isEmpty()) ? Integer.parseInt(args[3]) : 127,
                channelCount = (args[4] != null && !args[4].isEmpty()) ? Integer.parseInt(args[4]) : 2,
                startPoint = (args[5] != null && !args[5].isEmpty()) ? Integer.parseInt(args[5]) : -1,
                endPoint = (args[6] != null && !args[6].isEmpty()) ? Integer.parseInt(args[6]) : -1,
                loopPoint = (args[7] != null && !args[7].isEmpty()) ? Integer.parseInt(args[7]) : -1;
        if (soundReferTable instanceof Map<?, ?> tableMap && tableMap.containsKey(waveID)) {
            Object wave = tableMap.get(waveID);
            SiOPMWavePCMData sample = new SiOPMWavePCMData(wave, samplingNote * 64, 2, channelCount);
            sample.slice(startPoint, endPoint, loopPoint);
            table.setSample(sample, keyRangeFrom, keyRangeTo);
            return true;
        }
        return false;
    }

    /**
     * parse mml text of pcm voice setting (#PCMVOICE system command)
     *
     * @param voice     SiMMLVoice to update parameters
     * @param mml       comma separated values of #PCMVOICE system command
     * @param postfix   postfix of #PCMVOICE system command
     * @param envelopes envelope list to pickup envelope
     * @return true when success to update parameters
     */
    public static boolean parsePCMVoice(SiMMLVoice voice, String mml, String
            postfix, SiMMLEnvelopTable[] envelopes) {
        SiOPMWavePCMTable table = ((SiOPMWavePCMTable) voice. waveData);
        if (table == null) return false;
        String[] args = mml.split( "\\s *,\\s * ");
        int volumeNoteNumber = (args[0] != null && !args[0].isEmpty()) ? Integer.parseInt(args[0]) : 64,
                volumeKeyRange = (args[1] != null && !args[1].isEmpty()) ? Integer.parseInt(args[1]) : 0,
                volumeRange = (args[2] != null && !args[2].isEmpty()) ? Integer.parseInt(args[2]) : 0,
                panNoteNumber = (args[3] != null && !args[3].isEmpty()) ? Integer.parseInt(args[3]) : 64,
                panKeyRange = (args[4] != null && !args[4].isEmpty()) ? Integer.parseInt(args[4]) : 0,
                panWidth = (args[5] != null && !args[5].isEmpty()) ? Integer.parseInt(args[5]) : 0,
                dr = (args[7] != null && !args[7].isEmpty()) ? Integer.parseInt(args[7]) : 0,
                sr = (args[8] != null && !args[8].isEmpty()) ? Integer.parseInt(args[8]) : 0,
                rr = (args[9] != null && !args[9].isEmpty()) ? Integer.parseInt(args[9]) : 63,
                sl = (args[10] != null && !args[10].isEmpty()) ? Integer.parseInt(args[10]) : 0;
        SiOPMOperatorParam opp = voice.channelParam.operatorParam[0];
        opp.ar = (args[6] != null && !args[6].isEmpty()) ? Integer.parseInt(args[6]) : 63;
        opp.dr = (args[7] != null && !args[7].isEmpty()) ? Integer.parseInt(args[7]) : 0;
        opp.sr = (args[8] != null && !args[8].isEmpty()) ? Integer.parseInt(args[8]) : 0;
        opp.rr = (args[9] != null && !args[9].isEmpty()) ? Integer.parseInt(args[9]) : 63;
        opp.sl = (args[10] != null && !args[10].isEmpty()) ? Integer.parseInt(args[10]) : 0;
        table.setKeyScaleVolume(volumeNoteNumber, volumeKeyRange, volumeRange);
        table.setKeyScalePan(panNoteNumber, panKeyRange, panWidth);
        parseVoiceSetting(voice, postfix, envelopes);
        return true;
    }

    // register data
    //

    /**
     * set SiONVoice list by OPM register data
     *
     * @param regData   int vector of register data.
     * @param address   address of the first data in regData
     * @param enableLFO flag to enable LFO parameters
     * @param voiceSet  voice list to set parameters. When this argument instanceof null, returning voices are allocated inside.
     * @return voice list pick up values from register data.
     */
    public List<SiONVoice> setOPMVoicesByRegister(int[] regData, int address, boolean enableLFO, List<SiONVoice> voiceSet) {
        int i, imax, value, index, v, ams, pms;
        SiOPMChannelParam chp;
        SiOPMOperatorParam opp;
        int opi, _pmd = 0, _amd = 0;
        int [] opia = {0, 2, 1, 3};
        SiOPMTable table = SiOPMTable.getInstance();

        // initialize result voice list
        voiceSet = voiceSet != null ? voiceSet : new ArrayList<>();
        for (opi = 0; opi < 8; opi++) {
            if (voiceSet.get(opi) != null) voiceSet.get(opi).initialize();
            else voiceSet.set(opi, new SiONVoice());
            voiceSet.get(opi).channelParam.opeCount = 4;
            voiceSet.get(opi).chipType = SiONVoice.CHIPTYPE_OPM;
        }

        // pick up parameters from register data
        imax = regData.length;
        for (i = 0; i < imax; i++, address++) {
            value = regData[i];
            chp = voiceSet.get(address & 7).channelParam;

            // Module parameter
            if (address < 0x20) {
                switch (address) {
                    case 1:  // TEST:7-2 LFO RESET:1
                        break;
                    case 8:  // (KEYON) MUTE:7 OP0:6 OP1:5 OP2:4 OP3:3 CH:2-0
                        break;
                    case 15: // NOIZE:7 FREQ:4-0
                        if ((value & 128) != 0) {
                            voiceSet.get(7).channelParam.operatorParam[3].setPGType(SiOPMTable.PG_NOISE_PULSE);
                            voiceSet.get(7).channelParam.operatorParam[3].fixedPitch = ((value & 31) << 6) + 2048;
                        }
                        break;
                    case 16: // TIMER AH:7-0
                        break;
                    case 17: // TIMER AL:10
                        break;
                    case 18: // TIMER B :7-0
                        break;
                    case 19: // TIMER FUNC ?
                        break;
                    case 24: // LFO FREQ:7-0
                        if (enableLFO) {
                            v = table.lfo_timerSteps[value];
                            for (opi = 0; opi < 8; opi++) {
                                voiceSet.get(opi).channelParam.lfoFreqStep = v;
                            }
                        }
                        break;
                    case 25: // A(0)/P(1):7 DEPTH:6-0
                        if (enableLFO) {
                            if ((value & 128) != 0) _pmd = value & 127;
                            else _amd = value & 127;
                        }
                        break;
                    case 27: // LFO WS:10
                        if (enableLFO) {
                            v = value & 3;
                            for (opi = 0; opi < 8; opi++) {
                                voiceSet.get(opi).channelParam.lfoWaveShape = v;
                            }
                        }
                        break;
                }
            } else

                // Channel parameter
                if (address < 0x40) {
                    switch ((address - 0x20) >> 3) {
                        case 0: // L:7 R:6 FB:5-3 ALG:2-0
                            v = value >> 6;
                            chp.volumes[0] = (v != 0) ? 0.5 : 0;
                            chp.pan = (v == 1) ? 128 : (v == 2) ? 0 : 64;
                            chp.fb = (value >> 3) & 7;
                            chp.alg = (value) & 7;
                            break;
                        case 1: // KC:6-0
                            // channel.kc = value & 127
                            break;
                        case 2: // KF:6-0
                            // channel.keyFraction = value & 127
                            break;
                        case 3: // PMS:6-4 AMS:10
                            if (enableLFO) {
                                pms = (value >> 4) & 7;
                                ams = (value) & 3;
                                chp.pmd = (pms < 6) ? (_pmd >> (6 - pms)) : (_pmd << (pms - 5));
                                chp.amd = (ams > 0) ? (_amd << (ams - 1)) : 0;
                            }
                            break;
                    }
                } else

                // Operator parameter
                {
                    index = opia[(address >> 3) & 3];
                    opp = chp.operatorParam[index];
                    switch ((address - 0x40) >> 5) {
                        case 0: // DT1:6-4 MUL:3-0
                            opp.dt1 = (value >> 4) & 7;
                            opp.setMul((value) & 15);
                            break;
                        case 1: // TL:6-0
                            opp.tl = value & 127;
                            break;
                        case 2: // KS:76 AR:4-0
                            opp.ksr = (value >> 6) & 3;
                            opp.ar = (value & 31) << 1;
                            break;
                        case 3: // AMS:7 DR:4-0
                            opp.ams = ((value >> 7) & 1) << 1;
                            opp.dr = (value & 31) << 1;
                            break;
                        case 4: // DT2:76 SR:4-0
                            opp.detune = table.dt2Table[(value >> 6) & 3];
                            opp.sr = (value & 31) << 1;
                            break;
                        case 5: // SL:7-4 RR:3-0
                            opp.sl = (value >> 4) & 15;
                            opp.rr = (value & 15) << 2;
                            break;
                    }
                }
        }

        return voiceSet;
    }

    // internal functions
    //
    // int to string with 0 filling
    private static String _str(int v, int length) {
        if (v >= 0) return ("0000" + v).substring(-length);
        return "-" + ("0000" + -v).substring(-length + 1);
    }

    // check parameters digit
    private static Map<String, Integer> _checkDigit(SiOPMChannelParam param) {
        Map<String, Integer> res = Map.of("ws", 1, "tl", 2, "dt", 1, "ph", 1, "fn", 1);
        for (int opeIndex = 0; opeIndex < param.opeCount; opeIndex++) {
            SiOPMOperatorParam opp = param.operatorParam[opeIndex];
            res.put("ws", Math.max(res.get("ws"), String.valueOf(opp.pgType).length()));
            res.put("tl", Math.max(res.get("tl"), String.valueOf(opp.tl).length()));
            res.put("dt", Math.max(res.get("dt"), String.valueOf(opp.detune).length()));
            res.put("ph", Math.max(res.get("ph"), String.valueOf(opp.phase).length()));
            res.put("fn", Math.max(res.get("fn"), String.valueOf(opp.fixedPitch >> 6).length()));
        }
        return res;
    }

    // translate algorithm by algorithm list, return index in the list.
    private static int _checkAlgorism(int oc, int al, int[][] algList) {
        int[] list = algList[oc - 1];
        for (int i = 0; i < list.length; i++) if (al == list[i]) return i;
        return -1;
    }

    // translate pgType to MA3 valid.
    private static int _pgTypeMA3(int pgType) {
        int ws = pgType - SiOPMTable.PG_MA3_WAVE;
        if (ws >= 0 && ws <= 31) return ws;
        return switch (pgType) {
            case 0 -> 0;   // sin
            case 1, 2, 128, 255 -> 24;  // saw
            case 4, 192, 191 -> 16;  // triangle
            case 5, 72 -> 6;
            default ->   // square
                    -1;
        };
    }

    // find nearest dt2 value
    private static int _dt2OPM(int detune) {
        if (detune <= 100) return 0;   // 0
        else if (detune <= 420) return 1;   // 384
        else if (detune <= 550) return 2;   // 500
        return 3;                           // 608
    }

    // find nearest balance value from opp0.tl and opp1.tl
    private static int _balanceAL(int tl0, int tl1) {
        if (tl0 == tl1) return 0;
        if (tl0 == 0) return -64;
        if (tl1 == 0) return 64;
        int[] tltable = SiOPMTable.getInstance().eg_lv2tlTable;
        int i;
        for (i = 1; i < 128; i++) if (tl0 >= tltable[i]) return i - 64;
        return 64;
    }

    // errors
    //
    public static RuntimeException errorToneParameterNotValid(String cmd, int chParam, int opParam) {
        return new RuntimeException("Translator error : Parameter count instanceof not valid in '" + cmd + "'. " + chParam + " parameters for channel and " + opParam + " parameters for each operator.");
    }

    public static RuntimeException errorParameterNotValid(String cmd, Object param) {
        return new RuntimeException("Translator error : Parameter not valid. '" + param + "' in " + cmd);
    }

    public RuntimeException errorTranslation(String str) {
        return new RuntimeException("Translator Error : mml error. '" + str + "'");
    }

    public static RuntimeException errorUnknown(String str) {
        return new RuntimeException("Translator error : Unknown. " + str);
    }
}
