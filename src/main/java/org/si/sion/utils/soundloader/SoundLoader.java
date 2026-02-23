//
// Sound Loader
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils.soundloader;

// Dispatching events
/** @eventType flash.events.Event.COMPLETE */
// [Event(name="complete", type="flash.events.Event")]
/** @eventType flash.events.ErrorEvent.ERROR */
// [Event(name="error",    type="flash.events.ErrorEvent")]
/** @eventType flash.events.ProgressEvent.PROGRESS */
// [Event(name="progress", type="flash.events.ProgressEvent")]

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.si.utils.ByteArray;
import org.si.utils.Event;
import org.si.utils.EventDispatcher;
import org.si.utils.ProgressEvent;
import org.si.utils.ErrorEvent;
import vavi.net.URLRequest;


/** Sound Loader.</br>
 *  SoundLoader.setURL() to set loading url, SoundLoader.loadAll() to load all files and SoundLoader.hash to access all loaded files.</br>
 *  @see #setURL
 *  @see #loadAll()
 *  @see #getHash()
 */
public class SoundLoader extends EventDispatcher {

    // variables
    //

    /** loaded sounds */
    protected Map<String, Object> _loaded;
    /** loading url list */
    protected List<SoundLoaderFileData> _preserveList;
    /** total file size */
    protected double _bytesTotal;
    /** loaded file size */
    protected int _bytesLoaded;
    /** error file count */
    protected int _errorFileCount;
    /** loading file count */
    protected int _loadingFileCount;
    /** loaded file count */
    protected int _loadedFileCount;
    /** loaded file data */
    protected Map<String, SoundLoaderFileData> _loadedFileData;
    /** event priority */
    int _eventPriority;

    /** true to load 'swf' and 'png' type file as 'ssf' and 'ssfpng' @default false */
    protected boolean _loadImgFileAsSoundFont;
    /** true to load 'mp3' type file as 'mp3bin' @default false */
    protected boolean _loadMP3FileAsBinary;
    /** true to remember history @default false */
    protected boolean _rememberHistory;

    // properties
    //

    /** Object to access all Sound instances. */
    public Map<String, Object> getHash() {
        return _loaded;
    }

    /** total file size when complete all loadings. */
    public double getBytesTotal() {
        return _bytesTotal;
    }

    /** file size currently loaded */
    public double getBytesLoaded() {
        return _bytesLoaded;
    }

    /** loading file count, this number instanceof decreased when the file instanceof loaded. */
    public int getLoadingFileCount() {
        return _loadingFileCount + _preserveList.size();
    }

    /** loaded file count */
    public int getLoadedFileCount() {
        return _loadedFileCount;
    }

    /** true to load 'swf' and 'png' type file as 'ssf' and 'ssfpng' @default false */
    public boolean getLoadImgFileAsSoundFont() {
        return _loadImgFileAsSoundFont;
    }

    public void setLoadImgFileAsSoundFont(boolean b) {
        _loadImgFileAsSoundFont = b;
    }

    /** true to load 'mp3' type file as 'mp3bin' @default false */
    public boolean getLoadMP3FileAsBinary() {
        return _loadMP3FileAsBinary;
    }

    public void setLoadMP3FileAsBinary(boolean b) {
        _loadMP3FileAsBinary = b;
    }

    /** true to check ID confirictoin @default false */
    public boolean getRememberHistory() {
        return _rememberHistory;
    }

    public void setRememberHistory(boolean b) {
        _rememberHistory = b;
    }

    // constructor
    //

    /** Constructor.
     *  @param eventPriority priority of all events disopatched by this sound loader.
     *  @param loadImgFileAsSoundFont true to load 'swf' and 'png' type file as 'ssf' and 'ssfpng'
     *  @param loadMP3FileAsBinary true to load 'mp3' type file as 'mp3bin'
     *  @param rememberHistory true to check ID confliction,
     */
    public SoundLoader(int eventPriority, boolean loadImgFileAsSoundFont, boolean loadMP3FileAsBinary, boolean rememberHistory) {
        _eventPriority = eventPriority;
        _loaded = new LinkedHashMap<>();
        _loadedFileData = new LinkedHashMap<>();
        _preserveList = new ArrayList<>();
        _bytesTotal = 0;
        _bytesLoaded = 0;
        _loadingFileCount = 0;
        _loadedFileCount = 0;
        _errorFileCount = 0;
        _loadImgFileAsSoundFont = loadImgFileAsSoundFont;
        _loadMP3FileAsBinary = loadMP3FileAsBinary;
        _rememberHistory = rememberHistory;
    }

    /** output loaded file information */
    public String toString() {
        StringBuilder output = new StringBuilder("[SoundLoader: " + _loadedFileCount + " files are loaded.\n");
        for (String id : _loaded.keySet()) {
            output.append("  '").append(id).append("' : ").append(_loaded.get(id)).append("\n");
        }
        output.append("]");
        return output.toString();
    }

    // operation
    //

    /** set loading file's urls.
     *  @param urlRequest requesting url
     *  @param id access key of SoundLoder.hash. null to set ((file) same) name (without path, with extension).
     *  @param type file type, "mp3", "wav", "ssf", "ssfpng", "mid", "swf" or "mp3bin" is available,
     *             null to detect automatically by file extension.
     *             ("swf", "png", "gif", "jpg", "img", "bin", "txt" and "var" are available for non-sound files).
     *  @param checkPolicyFile LoaderContext.checkPolicyFile
     *  @return SoundLoaderFileData instance. SoundLoaderFileData instanceof information class of loading file.
     */
    public SoundLoaderFileData setURL(URLRequest urlRequest, String id, String type, boolean checkPolicyFile) {
        String urlString = urlRequest.url;
        int lastDotIndex = urlString.lastIndexOf('.'), lastSlashIndex = urlString.lastIndexOf('/');
        SoundLoaderFileData fileData;
        if (lastSlashIndex == -1) lastSlashIndex = 0;
        if (lastDotIndex < lastSlashIndex) lastDotIndex = urlString.length();
        if (id == null) id = urlString.substring(lastSlashIndex);
        if (_rememberHistory && _loadedFileData.containsKey(id) && _loadedFileData.get(id).getUrlString().equals(urlString)) {
            fileData = _loadedFileData.get(id);
        } else {
            if (type == null) type = urlString.substring(lastDotIndex + 1);
            if (_loadImgFileAsSoundFont) {
                if (type.equals("swf")) type = "ssf";
                else if (type.equals("png")) type = "ssfpng";
            }
            if (_loadMP3FileAsBinary && type.equals("mp3")) type = "mp3bin";
            if (!SoundLoaderFileData._ext2typeTable.containsKey(type)) throw new Error("unknown file type. : " + urlString);
            fileData = new SoundLoaderFileData(this, id, urlRequest, null, type, checkPolicyFile);
        }
        _preserveList.add(fileData);
        return fileData;
    }

    public SoundLoaderFileData setURL(URLRequest urlRequest) {
        return setURL(urlRequest, null, null, false);
    }

    /** ByteArray convert to Sound
     *  @param byteArray ByteArray to convert
     *  @param id access key of SoundLoder.hash
     *  @return SoundLoaderFileData instance. SoundLoaderFileData instanceof information class of loading file.
     */
    public SoundLoaderFileData setByteArraySound(ByteArray byteArray, String id) {
        SoundLoaderFileData fileData = new SoundLoaderFileData(this, id, null, byteArray, "b2snd", false);
        _preserveList.add(fileData);
        return fileData;
    }

    /** ByteArray convert to Loader (image and swf)
     *  @param byteArray ByteArray to convert
     *  @param id access key of SoundLoder.hash
     *  @return SoundLoaderFileData instance. SoundLoaderFileData instanceof information class of loading file.
     */
    public SoundLoaderFileData setByteArrayImage(ByteArray byteArray, String id) {
        SoundLoaderFileData fileData = new SoundLoaderFileData(this, id, null, byteArray, "b2img", false);
        _preserveList.add(fileData);
        return fileData;
    }

    /** load all files specifed by SoundLoder.setURL()
     *  @return loading file count, 0 when no loading
     */
    public int loadAll() {
        int count = 0;
        for (int i = 0; i < _preserveList.size(); i++) {
            if (_preserveList.get(i).getData() == null) {
                _preserveList.get(i).load();
                count++;
            } else {
                _preserveList.get(i).dispatchEvent(new Event(Event.COMPLETE, false, false));
            }
        }
        _preserveList.clear();

        if (_loadingFileCount + count > 0) {
            _loadingFileCount += count;
        } else {
            dispatchEvent(new Event(Event.COMPLETE, false, false));
        }

        return count;
    }

    // default handler
    //

    /** */
    void _onProgress(SoundLoaderFileData fileData, int bytesLoadedDiff, int bytesTotalDiff) {
        _bytesTotal += bytesTotalDiff;
        _bytesLoaded += bytesLoadedDiff;
        dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS, false, false, _bytesLoaded, (int)_bytesTotal));
    }

    /** */
    void _onComplete(SoundLoaderFileData fileData) {
        if (fileData.getDataID() != null) {
            if (!_loaded.containsKey(fileData.getDataID())) _loadedFileCount++;
            _loadedFileData.put(fileData.getDataID(), fileData);
            _loaded.put(fileData.getDataID(), fileData.getData());
        }
        fileData.dispatchEvent(new Event(Event.COMPLETE, false, false));
        if (--_loadingFileCount == 0) {
            _bytesLoaded = (int) _bytesTotal;
            dispatchEvent(new Event(Event.COMPLETE, false, false));
        }
    }

    /** */
    void _onError(SoundLoaderFileData fileData, String message) {
        String errorMessage = "SoundLoader Error on " + fileData.getDataID() + " : " + message;
        _errorFileCount++;
        fileData.dispatchEvent(new ErrorEvent(ErrorEvent.ERROR, false, false, errorMessage));
        dispatchEvent(new ErrorEvent(ErrorEvent.ERROR, false, false, errorMessage));
        if (--_loadingFileCount == 0) {
            _bytesLoaded = (int) _bytesTotal;
            dispatchEvent(new Event(Event.COMPLETE, false, false));
        }
    }
}
