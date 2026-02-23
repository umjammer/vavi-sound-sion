//
// File Data class for SoundLoader
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils.soundloader;

import java.util.HashMap;
import java.util.Map;

import org.si.sion.midi.SMFData;
import org.si.sion.utils.PCMSample;
import org.si.sion.utils.SoundClass;
import org.si.sion.utils.soundfont.SiONSoundFontLoader;
import org.si.utils.ByteArray;
import org.si.utils.ByteArrayExt;
import org.si.utils.ErrorEvent;
import org.si.utils.Event;
import org.si.utils.EventDispatcher;
import org.si.utils.IOErrorEvent;
import org.si.utils.ProgressEvent;
import org.si.utils.SecurityErrorEvent;
import vavi.display.Bitmap;
import vavi.display.BitmapData;
import vavi.display.Loader;
import vavi.media.Sound;
import vavi.media.SoundLoaderContext;
import vavi.net.URLLoader;
import vavi.net.URLLoaderDataFormat;
import vavi.net.URLRequest;
import vavi.system.LoaderContext;

// Dispatching events
/** @eventType flash.events.Event.COMPLETE */
// [Event(name="complete", type="flash.events.Event")]
/** @eventType flash.events.ErrorEvent.ERROR */
// [Event(name="error",    type="flash.events.ErrorEvent")]
/** @eventType flash.events.ProgressEvent.PROGRESS */
// [Event(name="progress", type="flash.events.ProgressEvent")]

/** File Data class for SoundLoader */
public class SoundLoaderFileData extends EventDispatcher {

    // variables
    //

    /** type converting table */
    public static final Map<String, String> _ext2typeTable = new HashMap<>() {{
        put("mp3", "mp3");
        put("wav", "wav");
        put("mp3bin", "mp3bin");
        put("mid", "mid");
        put("smf", "mid");
        put("swf", "img");
        put("png", "img");
        put("gif", "img");
        put("jpg", "img");
        put("img", "img");
        put("bin", "bin");
        put("txt", "txt");
        put("var", "var");
        put("ssf", "ssf");
        put("ssfpng", "ssfpng");
        put("b2snd", "b2snd");
        put("b2img", "b2img");
    }};

    private String _dataID;
    private Object _content;
    private URLRequest _urlRequest;
    private String _type;
    private boolean _checkPolicyFile;
    private int _bytesLoaded, _bytesTotal;
    private Loader _loader;
    Sound _sound;
    URLLoader _urlLoader;
    SiONSoundFontLoader _fontLoader;
    ByteArray _byteArray;
    private SoundLoader _soundLoader;
    private final org.si.utils.EventListener _onCompleteListener = this::_onComplete;
    private final org.si.utils.EventListener _onProgressListener = this::_onProgress;
    private final org.si.utils.EventListener _onErrorListener = this::_onError;

    public SoundLoaderFileData() {}

    public SoundLoaderFileData(SoundLoader soundLoader, String dataID, URLRequest urlRequest, ByteArray byteArray, String type, boolean checkPolicyFile) {
        _soundLoader = soundLoader;
        _dataID = dataID;
        _urlRequest = urlRequest;
        _byteArray = byteArray;
        _type = type;
        _checkPolicyFile = checkPolicyFile;
    }

    // properties
    //

    /** data id */
    public String getDataID() {
        return _dataID;
    }

    /** loaded data */
    public Object getData() {
        return _content;
    }

    /** url string */
    public String getUrlString() {
        return (_urlRequest != null) ? _urlRequest.url : null;
    }

    /** data type */
    public String getType() {
        return _type;
    }

    /** loaded bytes */
    public int getBytesLoaded() {
        return _bytesLoaded;
    }

    /** total bytes */
    public int getBytesTotal() {
        return _bytesTotal;
    }

    // private functions
    //

    /** */
    boolean load() {
        // already loaded
        if (_content != null) return false;

        switch (_type) {
            case "mp3":
                _addAllListeners(_sound = new Sound());
                _sound.load(_urlRequest, new SoundLoaderContext(1000, _checkPolicyFile));
                break;
            case "img":
            case "ssfpng":
                _loader = new Loader();
                _addAllListeners(_loader.contentLoaderInfo);
                _loader.load(_urlRequest, new LoaderContext(_checkPolicyFile));
                break;
            case "txt":
                _addAllListeners(_urlLoader = new URLLoader());
                _urlLoader.dataFormat = URLLoaderDataFormat.TEXT;
                _urlLoader.load(_urlRequest);
                break;
            case "mp3bin":
            case "bin":
            case "wav":
            case "mid":
                _addAllListeners(_urlLoader = new URLLoader());
                _urlLoader.dataFormat = URLLoaderDataFormat.BINARY;
                _urlLoader.load(_urlRequest);
                break;
            case "var":
                _addAllListeners(_urlLoader = new URLLoader());
                _urlLoader.dataFormat = URLLoaderDataFormat.VARIABLES;
                _urlLoader.load(_urlRequest);
                break;
            case "ssf":
                _addAllListeners(_fontLoader = new SiONSoundFontLoader());
                _fontLoader.load(_urlRequest, false, false);
                break;
            case "b2snd":
                new SoundClass().loadMP3FromByteArray(_byteArray, this::__loadMP3FromByteArray_onComplete);
                break;
            case "b2img":
                _loader = new Loader();
                _addAllListeners(_loader.contentLoaderInfo);
                _loader.loadBytes(_byteArray);
                break;
            default:
                break;
        }

        return true;
    }

    /** */
    boolean listenLoadingStatus(Object target) {
        _sound = (target instanceof Sound) ? (Sound) target : null;
        _loader = (target instanceof Loader) ? (Loader) target : null;
        _urlLoader = (target instanceof URLLoader) ? (URLLoader) target : null;
        target = _sound != null ? _sound : (_urlLoader != null ? _urlLoader : (_loader != null ? _loader.contentLoaderInfo : null));
        if (target != null) {
            EventDispatcher eTarget = (EventDispatcher) target;
            int targetBytesTotal = getBytesTotal(target);
            int targetBytesLoaded = getBytesLoaded(target);
            if (targetBytesTotal != 0 && targetBytesTotal == targetBytesLoaded) {
                _postProcess();
            } else {
                _addAllListeners(eTarget);
            }
            return true;
        }

        return false;
    }

    private void _addAllListeners(EventDispatcher dispatcher) {
        dispatcher.addEventListener(Event.COMPLETE, _onCompleteListener, false, _soundLoader._eventPriority);
        dispatcher.addEventListener(ProgressEvent.PROGRESS, _onProgressListener, false, _soundLoader._eventPriority);
        dispatcher.addEventListener(IOErrorEvent.IO_ERROR, _onErrorListener, false, _soundLoader._eventPriority);
        dispatcher.addEventListener(SecurityErrorEvent.SECURITY_ERROR, _onErrorListener, false, _soundLoader._eventPriority);
    }

    private void _removeAllListeners() {
        EventDispatcher dispatcher = _sound != null ? _sound : (_urlLoader != null ? _urlLoader : (_fontLoader != null ? _fontLoader : (_loader != null ? _loader.contentLoaderInfo : null)));
        if (dispatcher == null) {
            return;
        }
        dispatcher.removeEventListener(Event.COMPLETE, _onCompleteListener);
        dispatcher.removeEventListener(ProgressEvent.PROGRESS, _onProgressListener);
        dispatcher.removeEventListener(IOErrorEvent.IO_ERROR, _onErrorListener);
        dispatcher.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, _onErrorListener);
    }

    private void _onProgress(Event e) {
        if (e instanceof ProgressEvent pe) {
            dispatchEvent(pe.clone());
            _soundLoader._onProgress(this, pe.bytesLoaded - _bytesLoaded, pe.bytesTotal - _bytesTotal);
            _bytesLoaded = pe.bytesLoaded;
            _bytesTotal = pe.bytesTotal;
        }
    }
    private void _onComplete(Event e) {
        _removeAllListeners();
        int loaded = getBytesLoaded(e.target);
        int total = getBytesTotal(e.target);
        _soundLoader._onProgress(this, loaded - _bytesLoaded, total - _bytesTotal);
        _bytesLoaded = loaded;
        _bytesTotal = total;
        _postProcess();
    }

    private void _postProcess() {
        String currentBICID;
        PCMSample pcmSample;
        SMFData smfData;

        switch (_type) {
            case "mp3":
                _content = _sound;
                _soundLoader._onComplete(this);
                break;
            case "wav":
                currentBICID = PCMSample.basicInfoChunkID;
                PCMSample.basicInfoChunkID = "acid";
                ByteArray wavData = new ByteArray();
                wavData.writeBytes(_urlLoader.data);
                pcmSample = new PCMSample(2, 44100, null).loadWaveFromByteArray(wavData);
                PCMSample.basicInfoChunkID = currentBICID;
                _content = pcmSample;
                _soundLoader._onComplete(this);
                break;
            case "mid":
                ByteArray ba = new ByteArray();
                ba.writeBytes(_urlLoader.data);
                smfData = new SMFData().loadBytes(ba);
                _content = smfData;
                _soundLoader._onComplete(this);
                break;
            case "mp3bin":
                ByteArray mp3Data = new ByteArray();
                mp3Data.writeBytes(_urlLoader.data);
                new SoundClass().loadMP3FromByteArray(mp3Data, this::__loadMP3FromByteArray_onComplete);
                break;
            case "ssf":
                _content = _fontLoader.soundFont;
                _soundLoader._onComplete(this);
                break;
            case "ssfpng":
                _convertBitmapDataToSoundFont(((Bitmap) _loader.content).bitmapData);
                break;

            // for ordinary purpose
            case "img":
            case "b2img":
                _content = _loader.content;
                _soundLoader._onComplete(this);
                break;
            case "txt":
            case "bin":
            case "var":
                _content = _urlLoader.data;
                _soundLoader._onComplete(this);
                break;
        }
    }

    private void _onError(Event e) {
        if (e instanceof ErrorEvent) {
            _removeAllListeners();
            __errorCallback((ErrorEvent) e);
        }
    }

    private Object __loadMP3FromByteArray_onComplete(Object soundObj) {
        Sound sound = (Sound) soundObj;
        _content = sound;
        _soundLoader._onComplete(this);
        return null;
    }

    private void _convertBitmapDataToSoundFont(BitmapData bitmap) {
        ByteArrayExt bitmap2bytes = new ByteArrayExt(null); // convert BitmapData to ByteArray
        _loader = null;
        _fontLoader = new SiONSoundFontLoader();            // convert ByteArray to SWF and SWF to soundList
        _fontLoader.addEventListener(Event.COMPLETE, this::__convertB2SF_onComplete);
        _fontLoader.addEventListener(IOErrorEvent.IO_ERROR, e -> __errorCallback((ErrorEvent) e));
        _fontLoader.loadBytes(bitmap2bytes.fromBitmapData(bitmap));
    }

    private void __convertB2SF_onComplete(Event e) {
        _content = _fontLoader.soundFont;
        _soundLoader._onComplete(this);
    }

    private void __errorCallback(ErrorEvent e) {
        _soundLoader._onError(this, e.toString());
    }

    private int getBytesLoaded(Object target) {
        if (target instanceof Sound sound) {
            return sound.bytesLoaded;
        }
        if (target instanceof URLLoader loader) {
            return loader.bytesLoaded;
        }
        if (target instanceof vavi.display.LoaderInfo info) {
            return info.bytesLoaded;
        }
        return _bytesLoaded;
    }

    private int getBytesTotal(Object target) {
        if (target instanceof Sound sound) {
            return sound.bytesTotal;
        }
        if (target instanceof URLLoader loader) {
            return loader.bytesTotal;
        }
        if (target instanceof vavi.display.LoaderInfo info) {
            return info.bytesTotal;
        }
        return _bytesTotal;
    }
}
