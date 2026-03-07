package org.si.sion.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.si.sion.SiONData;
import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.midi.SMFData;
import org.si.sion.utils.soundloader.SoundLoader;
import org.si.sion.utils.soundloader.SoundLoaderFileData;
import org.si.utils.ErrorEvent;
import org.si.utils.Event;
import org.si.utils.ProgressEvent;
import org.si.as3.media.Sound;
import org.si.as3.net.URLRequest;


/**
 * Add System command for MMLTalks (#LOADSOUND, #PRESET)
 */
public class MMLTalksParser {

    // variables
    //

    /** preset voice set to execute #PRESET */
    public SiONPresetVoice presetVoices = null;
    /** allow to play MIDI file by #LOADSOUND */
    public boolean allowMIDIFile = false;

    private SiONDriver _sionDriver = null;
    private SiONData _sionData = null;
    private SMFData _smfData = null;
    private String _mmlString = null;
    private SoundLoader _soundLoader = null;
    private Consumer<ErrorEvent> _errorHandler = null;
    private Consumer<Object> _completeHandler = null;
    private Consumer<ProgressEvent> _progressHandler = null;

    // event handler
    //
    private void _onError(Event e) {
        if (e instanceof ErrorEvent ee) {
            if (_errorHandler != null) _errorHandler.accept(ee);
        }
    }

    private void _onProgress(Event e) {
        if (e instanceof ProgressEvent pe) {
            if (_progressHandler != null) _progressHandler.accept(pe);
        }
    }

    // commands
    //

    /**
     * call this first after create new SiONDriver
     *
     * @param driver     SiONDriver instance to play.
     * @param onError    compile or loading error event hondler
     * @param onProgress loading progression event hondler
     */
    public void initialize(SiONDriver driver, Consumer<ErrorEvent> onError, Consumer<ProgressEvent> onProgress) {
        _sionDriver = driver;
        _soundLoader = new SoundLoader(0, false, true, true);
        _soundLoader.addEventListener(Event.COMPLETE, this::_onCompleteAllLoading);
        _soundLoader.addEventListener(ErrorEvent.ERROR, this::_onError);
        _soundLoader.addEventListener(ProgressEvent.PROGRESS, this::_onProgress);
        _errorHandler = onError;
        _progressHandler = onProgress;
    }

    /**
     * set url to loading resource if it needs.
     *
     * @param url URL of the resource to load
     */
    public SoundLoaderFileData setURL(String url) {
        return _soundLoader.setURL(new URLRequest(url));
    }

    /**
     * compile MML. This function instanceof asynchronous. You have to play data in onComplete function
     *
     * @param mml        MML string
     * @param onComplete compile complete event hondler, function(data):void
     * @param data       SiONData to receive compiled data
     */
    public void compile(String mml, Consumer<Object> onComplete, SiONData data) {
        _mmlString = mml;
        _completeHandler = onComplete;
        _sionData = data;
        _parseMTSystemCommandBeforeCompile(mml);
    }

    // internals
    //
    // callback while system command parsing. you have to copmle MML after all sound loaded.
    private void _parseMTSystemCommandBeforeCompile(String mml) {
        List<Map<String, Object>> cmds = Translator.extractSystemCommand(mml);
        Map<String, Object> cmd;
        int i;
        String url;

        // list all Sound requires loading
        for (i = 0; i < cmds.size(); i++) {
            cmd = cmds.get(i);
            switch ((String) cmd.get("command")) {
                case "#LOADSOUND":
                    // data
                    url = (String) cmd.get("content");
                    _soundLoader.setURL(new URLRequest(url));
                    break;
            }
        }

        // load all
        _soundLoader.loadAll();
    }

    // analyze MMLTalks system commands.
    private void _parseMTSystemCommandAfterCompile(SiONData data) {
        SiONVoice voice;

        for (Map<String, Object> cmd : data.getSystemCommands()) {
            switch ((String) cmd.get("command")) {
                case "#PRESET@":
                    if (presetVoices == null) presetVoices = new SiONPresetVoice();
                    voice = (SiONVoice) presetVoices.get((String) cmd.get("content"));
                    if (voice instanceof SiONVoice) data.setVoice((int) cmd.get("number"), voice);
                    break;
            }
        }
    }

    // on complete all
    private void _onCompleteAllLoading(Event e) {
        SiONData data;
        Map<String, Object> soundHash = new HashMap<>();

        // construct sound hash table and font list
        _smfData = null;
        Map<String, Object> loadedHash = _soundLoader.getHash();
        for (String key : loadedHash.keySet()) {
            Object obj = loadedHash.get(key);
            if (obj instanceof Sound) {
                soundHash.put(key, obj);
            } else if (obj instanceof SMFData) {
                _smfData = (SMFData) obj;
            }
        }

        // set sound hash & compile
        _sionDriver.setSoundReferenceTable(soundHash);
        _sionDriver.compile(_mmlString, _sionData);
        data = _sionData != null ? _sionData : _sionDriver.getData();
        _parseMTSystemCommandAfterCompile(data);
        _mmlString = null;
        if (allowMIDIFile && _smfData != null) {
            _sionDriver.getMidiModule().resetVoiceSet();
            for (int i = 0; i < 128; i++) {
                if (data.fmVoices[i] != null) _sionDriver.getMidiModule().voiceSet[i].copyFrom(data.fmVoices[i]);
            }
            for (int i = 0; i < 60; i++) {
                if (data.fmVoices[i + 128] != null) _sionDriver.getMidiModule().drumVoiceSet[i].copyFrom(data.fmVoices[i + 128]);
            }
            _completeHandler.accept(_smfData);
        } else {
            _completeHandler.accept(data);
        }
    }
}
