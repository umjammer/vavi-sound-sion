//
// SiON sound font loader
//  Copyright (c) 2011 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils.soundfont;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.si.sion.SiONVoice;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.module.SiOPMWaveTable;
import org.si.sion.sequencer.SiMMLEnvelopTable;
import org.si.sion.utils.Translator;
import org.si.utils.ByteArray;
import org.si.utils.ByteArrayExt;
import org.si.utils.Event;
import org.si.utils.EventDispatcher;
import vavi.net.URLLoader;
import vavi.net.URLLoaderDataFormat;
import vavi.net.URLRequest;


/** Sound font loader. */
public class SiONSoundFontLoader extends EventDispatcher {

    // variables
    //

    /** SiONSoundFont instance. this instance instanceof available after finish loading. */
    public SiONSoundFont soundFont;

    // loaders
    private URLLoader _binloader;

    // properties
    //

    /** loaded size. */
    public double getBytesLoaded() {
        return 0; // Not implemented / Not supported by vavi.net.URLLoader
    }

    /** total size. */
    public double getBytesTotal() {
        return 0; // Not implemented / Not supported by vavi.net.URLLoader
    }

    // constructor
    //

    /** constructor */
    public SiONSoundFontLoader() {
        soundFont = null;
        _binloader = null;
    }

    // operations
    //

    /**
     * load sound font from url
     *
     * @param url             requesting url
     * @param loadAsBinary    load soundfont ((binary) swf) and convert to swf.
     * @param checkPolicyFile check policy file. this argument instanceof ignored when loadAsBinary instanceof true.
     */
    public void load(URLRequest url, boolean loadAsBinary, boolean checkPolicyFile) {
        _binloader = new URLLoader();
        _addAllListeners(_binloader);
        _binloader.dataFormat = URLLoaderDataFormat.BINARY;
        _binloader.load(url);
    }

    /**
     * load sound font from binary
     *
     * @param bytes ByteArray to load from.
     */
    public void loadBytes(ByteArray bytes) {
        _binloader = null;
        int signature = (int) bytes.readUnsignedInt();
        if (signature == 0x0b535743) { // swf
            // SWF loading not implemented in Java
        } else if (signature == 0x04034b50) { // zip
            _analyzeZip(bytes);
        }
    }

    // event handling
    //

    private void _addAllListeners(EventDispatcher dispatcher) {
        dispatcher.addEventListener(Event.COMPLETE, this::_onComplete);
    }

    private void _removeAllListeners() {
        if (_binloader != null) {
            _binloader.removeEventListener(Event.COMPLETE, this::_onComplete);
        }
    }

    private void _onComplete(Object e) {
        _removeAllListeners();
        if (_binloader != null && _binloader.data != null) {
            ByteArray ba = new ByteArray();
            ba.writeBytes(_binloader.data);
            loadBytes(ba);
        } else {
            dispatchEvent(new Event(Event.COMPLETE));
        }
    }

    private void _onError(org.si.utils.ErrorEvent e) {
        _removeAllListeners();
        dispatchEvent(new org.si.utils.ErrorEvent(e.type, false, false, "error"));
    }

    // internal functions
    //

    private void _analyze() {
        // SWF loading not implemented
    }

    private void _analyzeZip(ByteArray bytes) {
        List<ByteArrayExt> fileList = new ByteArrayExt(bytes).expandZipFile();
        int i, imax = fileList.size();
        Map<String, vavi.media.Sound> sounds = new HashMap<>();
        String mml = null;
        vavi.media.Sound snd;
        ByteArrayExt file;
        for (i = 0; i < imax; i++) {
            file = fileList.get(i);
            if (file.name.matches(".*\\.mp3$")) {
                snd = new vavi.media.Sound();
                sounds.put(file.name, snd);
                // snd.loadCompressedDataFromByteArray(file, file.length); // Not implemented yet
            } else if (file.name.matches(".*\\.mml$")) {
                file.position = 0;
                mml = file.readUTFBytes(file.length);
            }
        }
        soundFont = new SiONSoundFont(sounds);
        if (mml != null) {
            _compileSystemCommand(new Translator().extractSystemCommand(mml));
        }
        dispatchEvent(new Event(Event.COMPLETE));
    }

    // compile sound font from system commands
    private void _compileSystemCommand(List<Map<String, Object>> systemCommands) {
        if (systemCommands == null) return;
        int i, imax = systemCommands.size(), num = 0, bank;
        Map<String, Object> cmd;
        String dat, pfx;
        String commandStr;
        SiMMLEnvelopTable env;
        SiONVoice voice;
        SiOPMWaveSamplerTable samplerTable;
        SiOPMWavePCMTable pcmTable;

        for (i = 0; i < imax; i++) {
            cmd = systemCommands.get(i);
            commandStr = (String) cmd.get("command");
            num = (int) cmd.get("number");
            dat = (String) cmd.get("content");
            pfx = (String) cmd.get("postfix");

            switch (commandStr) {
                // tone settings
                case "#@": {
                    voice = new SiONVoice();
                    Translator.parseParam(voice.channelParam, dat);
                    if (!pfx.isEmpty()) Translator.parseVoiceSetting(voice, pfx, null);
                    soundFont.fmVoices[num] = voice;
                    break;
                }
                case "#OPM@": {
                    voice = new SiONVoice();
                    Translator.parseOPMParam(voice.channelParam, dat);
                    if (!pfx.isEmpty()) Translator.parseVoiceSetting(voice, pfx, null);
                    soundFont.fmVoices[num] = voice;
                    break;
                }
                case "#OPN@": {
                    voice = new SiONVoice();
                    Translator.parseOPNParam(voice.channelParam, dat);
                    if (!pfx.isEmpty()) Translator.parseVoiceSetting(voice, pfx, null);
                    soundFont.fmVoices[num] = voice;
                    break;
                }
                case "#OPL@": {
                    voice = new SiONVoice();
                    Translator.parseOPLParam(voice.channelParam, dat);
                    if (!pfx.isEmpty()) Translator.parseVoiceSetting(voice, pfx, null);
                    soundFont.fmVoices[num] = voice;
                    break;
                }
                case "#OPX@": {
                    voice = new SiONVoice();
                    Translator.parseOPXParam(voice.channelParam, dat);
                    if (!pfx.isEmpty()) Translator.parseVoiceSetting(voice, pfx, null);
                    soundFont.fmVoices[num] = voice;
                    break;
                }
                case "#MA@": {
                    voice = new SiONVoice();
                    Translator.parseMA3Param(voice.channelParam, dat);
                    if (!pfx.isEmpty()) Translator.parseVoiceSetting(voice, pfx, null);
                    soundFont.fmVoices[num] = voice;
                    break;
                }
                case "#AL@": {
                    voice = new SiONVoice();
                    Translator.parseALParam(voice.channelParam, dat);
                    if (!pfx.isEmpty()) Translator.parseVoiceSetting(voice, pfx, null);
                    soundFont.fmVoices[num] = voice;
                    break;
                }

                // parser settings
                case "#FPS": {
                    soundFont.defaultFPS = (num > 0) ? num : ((dat.isEmpty()) ? 60 : Integer.parseInt(dat));
                    break;
                }
                case "#VMODE": {
                    _parseVCommansSubMML(dat);
                    break;
                }

                // tables
                case "#TABLE": {
                    if (num < 0 || num > 254) throw _errorParameterNotValid("#TABLE", String.valueOf(num));
                    env = new SiMMLEnvelopTable().parseMML(dat + pfx);
                    if (env.head == null) throw _errorParameterNotValid("#TABLE", dat);
                    soundFont.envelopes[num] = env;
                    break;
                }
                case "#WAV": {
                    if (num < 0 || num > 255) throw _errorParameterNotValid("#WAV", String.valueOf(num));
                    soundFont.waveTables[num] = _newWaveTable(Translator.parseWAV(dat, pfx));
                    break;
                }
                case "#WAVB": {
                    if (num < 0 || num > 255) throw _errorParameterNotValid("#WAVB", String.valueOf(num));
                    soundFont.waveTables[num] = _newWaveTable(Translator.parseWAVB((dat.isEmpty()) ? pfx : dat));
                    break;
                }

                // pcm voice
                case "#SAMPLER": {
                    if (num < 0 || num > 255) throw _errorParameterNotValid("#SAMPLER", String.valueOf(num));
                    bank = (num >> SiOPMTable.NOTE_BITS) & (SiOPMTable.SAMPLER_TABLE_MAX - 1);
                    num &= (SiOPMTable.NOTE_TABLE_SIZE - 1);
                    if (soundFont.samplerTables[bank] == null) soundFont.samplerTables[bank] = new SiOPMWaveSamplerTable();
                    samplerTable = soundFont.samplerTables[bank];
                    if (!Translator.parseSamplerWave(samplerTable, num, dat, soundFont.sounds))
                        throw _errorParameterNotValid("#SAMPLER", String.valueOf(num));
                    break;
                }
                case "#PCMWAVE": {
                    if (num < 0 || num > 255) throw _errorParameterNotValid("#PCMWAVE", String.valueOf(num));
                    if (soundFont.pcmVoices[num] == null) soundFont.pcmVoices[num] = new SiONVoice();
                    voice = soundFont.pcmVoices[num];
                    if (!(voice.waveData instanceof SiOPMWavePCMTable)) voice.waveData = new SiOPMWavePCMTable();
                    pcmTable = (SiOPMWavePCMTable) voice.waveData;
                    if (!Translator.parsePCMWave(pcmTable, dat, soundFont.sounds))
                        throw _errorParameterNotValid("#PCMWAVE", String.valueOf(num));
                    break;
                }
                case "#PCMVOICE": {
                    if (num < 0 || num > 255) throw _errorParameterNotValid("#PCMVOICE", String.valueOf(num));
                    if (soundFont.pcmVoices[num] == null) soundFont.pcmVoices[num] = new SiONVoice();
                    voice = soundFont.pcmVoices[num];
                    if (!Translator.parsePCMVoice(voice, dat, pfx, soundFont.envelopes))
                        throw _errorParameterNotValid("#PCMVOICE", String.valueOf(num));
                    break;
                }
                default:
                    break;
            }
        }
    }

    // Parse inside of #VMODE{...}
    private void _parseVCommansSubMML(String dat) {
        Pattern tcmdrex = Pattern.compile("(n88|mdx|psg|mck|tss|%[xv])(\\d*)(\\s*,?\\s*(\\d?))");
        Matcher res = tcmdrex.matcher(dat);
        Object num;
        int i;
        while (res.find()) {
            switch (res.group(1)) {
                case "%v":
                    i = Integer.parseInt(res.group(2).isEmpty() ? "0" : res.group(2));
                    soundFont.defaultVelocityMode = (i >= 0 && i < SiOPMTable.VM_MAX) ? i : 0;
                    i = (res.group(4) != null && !res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 4;
                    soundFont.defaultVCommandShift = (i >= 0 && i < 8) ? i : 0;
                    break;
                case "%x":
                    i = Integer.parseInt(res.group(2).isEmpty() ? "0" : res.group(2));
                    soundFont.defaultExpressionMode = (i >= 0 && i < SiOPMTable.VM_MAX) ? i : 0;
                    break;
                case "n88":
                case "mdx":
                    soundFont.defaultVelocityMode = SiOPMTable.VM_DR32DB;
                    soundFont.defaultExpressionMode = SiOPMTable.VM_DR48DB;
                    break;
                case "psg":
                    soundFont.defaultVelocityMode = SiOPMTable.VM_DR48DB;
                    soundFont.defaultExpressionMode = SiOPMTable.VM_DR48DB;
                    break;
                default: // mck/tss
                    soundFont.defaultVelocityMode = SiOPMTable.VM_LINEAR;
                    soundFont.defaultExpressionMode = SiOPMTable.VM_LINEAR;
                    break;
            }
        }
    }

    // Set wave table data refered by %4
    private SiOPMWaveTable _newWaveTable(double[] data) {
        int i, imax = data.length;
        int[] table = new int[imax];
        for (i = 0; i < imax; i++) table[i] = SiOPMTable.calcLogTableIndex(data[i]);
        return SiOPMWaveTable.alloc(table, imax);
    }

    private RuntimeException _errorParameterNotValid(String cmd, String param) {
        return new RuntimeException("SiMMLSequencer error : Parameter not valid. '" + param + "' in " + cmd);
    }
}
