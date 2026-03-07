//
// Standard MIDI File class
//  modified by keim.
//  This soruce code is distributed under BSD-style license (see org.si.license.txt).
//
// Original code
//  url; http://wonderfl.net/code/0aad6e9c1c5f5a983c6fce1516ea501f7ea7dfaa
//  Copyright (c) 2010 nemu90kWw All rights reserved.
//  The original code is distributed under MIT license.
//  (see http://www.opensource.org/licenses/mit-license.php).
//

package org.si.sion.midi;

import java.util.Vector;

import org.si.utils.ByteArray;
import org.si.utils.Event;
import org.si.utils.EventDispatcher;
import org.si.as3.net.URLLoader;
import org.si.as3.net.URLLoaderDataFormat;
import org.si.as3.net.URLRequest;


/** Standard MIDI File class */
public class SMFData extends EventDispatcher {

    // variables
    //

    /** Standard MIDI file format (0,1 o 2) */
    public int format;
    /** track count */
    public int numTracks;
    /** resolution [ticks/whole tone] */
    public int resolution;
    /** initial tempo */
    public int bpm = 0;
    /** text information */
    public String text = "";
    /** title string */
    public String title = null;
    /** author infomation */
    public String author = null;
    /** numerator of signiture */
    public int signature_n = 0;
    /** denominator of signiture */
    public int signature_d = 0;
    /** song length [measures] */
    public double measures = 0;
    /** SMF tracks */
    public Vector<SMFTrack> tracks = new Vector<>();

    private URLLoader _urlLoader;

    // properties
    //

    /** Is available ? */
    public boolean isAvailable() {
        return (numTracks > 0);
    }

    /** to string. */
    @Override
    public String toString() {
        String text = "";
        text += "format : SMF" + format + "\n";
        text += "numTracks : " + numTracks + "\n";
        text += "resolution : " + (resolution >> 2) + "\n";
        text += "title : " + title + "\n";
        text += "author : " + author + "\n";
        text += "signature : " + signature_n + "/" + signature_d + "\n";
        text += "BPM : " + bpm + "\n";
        return text;
    }

    // constructor
    //

    /** constructor */
    public SMFData() {
        clear();
    }

    // operations
    //

    /** Clear. */
    public SMFData clear() {
        format = 0;
        numTracks = 0;
        resolution = 0;
        bpm = 0;
        text = null;
        title = null;
        author = null;
        signature_n = 0;
        signature_d = 0;
        measures = 0;
        tracks.clear();

        return this;
    }

    /**
     * Load SMF file. This function dispatches Event.COPMLETE when finish loading
     *
     * @param url URL of SMF file
     */
    public void load(URLRequest url) {
        ByteArray byteArray = new ByteArray();
        _urlLoader = new URLLoader();
        _urlLoader.dataFormat = URLLoaderDataFormat.BINARY;
        _urlLoader.addEventListener(Event.COMPLETE, this::_onComplete);
        _urlLoader.addEventListener("progress", this::_onProgress);
        _urlLoader.addEventListener("ioError", this::_onError);
        _urlLoader.addEventListener("securityError", this::_onError);
        _urlLoader.load(url);
    }

    /**
     * Load SMF data from byteArray. This function dispatches Event.COPMLETE but returns data immediately.
     *
     * @param bytes SMF file binary
     */
    public SMFData loadBytes(ByteArray bytes) {
        bytes.position = 0;
        clear();

        int tr, len;
        ByteArray temp = new ByteArray();
        while (bytes.getBytesAvailable() > 0) {
            String type = bytes.readMultiByte(4, "us-ascii");
            switch (type) {
                case "MThd":
                    bytes.position += 4;
                    format = bytes.readUnsignedShort();
                    numTracks = bytes.readUnsignedShort();
                    resolution = bytes.readUnsignedShort() << 2;
                    break;
                case "MTrk":
                    len = bytes.readInt(); // readUnsignedInt equivalent for sizes typically small enough
                    bytes.readBytes(temp, 0, len);
                    tracks.add(new SMFTrack(this, tracks.size(), temp));
                    break;
                default:
                    len = bytes.readInt(); // readUnsignedInt
                    bytes.position += len;
                    break;
            }
        }

        if (text == null) text = "";
        if (title == null) title = "";
        if (author == null) author = "";

        if (resolution > 0) {
            len = 0;
            for (tr = 0; tr < tracks.size(); tr++) {
                if (len < tracks.get(tr).totalTime) len = tracks.get(tr).totalTime;
            }
            measures = (double) len / resolution;
        }

        dispatchEvent(new Event(Event.COMPLETE));

        return this;
    }

    // internal use
    //

    private void _onProgress(Object e) {
        dispatchEvent(((Event) e).clone());
    }

    private void _onComplete(Object e) {
        _removeAllListeners();
        byte[] data = ((URLLoader) ((Event) e).target).data;
        if (data != null) {
            org.si.utils.ByteArray byteArray = new org.si.utils.ByteArray();
            byteArray.writeBytes(data);
            byteArray.position(0);
            loadBytes(byteArray);
        }
    }

    private void _onError(Object e) {
        _removeAllListeners();
        dispatchEvent(((Event) e).clone());
    }

    private void _removeAllListeners() {
        _urlLoader.removeEventListener(Event.COMPLETE, this::_onComplete);
        _urlLoader.removeEventListener("progress", this::_onProgress);
        _urlLoader.removeEventListener("ioError", this::_onError);
        _urlLoader.removeEventListener("securityError", this::_onError);
    }
}
