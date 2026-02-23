//
// SMF Track chunk
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

import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.si.utils.ByteArray;


/** SMF Track chunk */
public class SMFTrack {

    // variables
    //

    /** sequence */
    public Vector<SMFEvent> sequence = new Vector<SMFEvent>();
    /** total time in MIDI clock */
    public int totalTime;

    // parent SMFData
    private SMFData _smfData;
    // for exiting loop
    private boolean _exitLoop;
    // track index (start from 1)
    private int _trackIndex;

    // properties
    //

    /** track index (start from 1) */
    public int getTrackIndex() {
        return _trackIndex;
    }

    /** toString */
    @Override
    public String toString() {
        String text = totalTime + "\n";

        for (int i = 0; i < sequence.size(); i++) {
            text += sequence.get(i).toString() + "\n";
        }

        return text;
    }

    // constructor
    //

    /** constructor */
    SMFTrack(SMFData smfData, int index, ByteArray bytes) {
        _trackIndex = index + 1;
        _smfData = smfData;

        int eventType, code, value;
        int deltaTime, time = 0;

        _exitLoop = false;
        eventType = -1;
        bytes.position = 0;

        while (bytes.getBytesAvailable() > 0 && !_exitLoop) {
            deltaTime = _readVariableLength(bytes, 0);
            time += deltaTime;

            code = bytes.readUnsignedByte();
            if (!_readMetaEvent(code, bytes, deltaTime, time))
                if (!_readSystemExclusive(code, bytes, deltaTime, time)) {
                    if ((code & 0x80) != 0) {
                        eventType = code;
                    } else {
                        if (eventType == -1) throw _errorIncorrectData();
                        bytes.position--;
                    }

                    value = 0;
                    switch (eventType & 0xf0) {
                        case SMFEvent.PROGRAM_CHANGE:
                        case SMFEvent.CHANNEL_PRESSURE:
                            value = bytes.readUnsignedByte();
                            break;
                        case SMFEvent.NOTE_OFF:
                        case SMFEvent.NOTE_ON:
                        case SMFEvent.KEY_PRESSURE:
                        case SMFEvent.CONTROL_CHANGE:
                            value = (bytes.readUnsignedByte() << 16) | bytes.readUnsignedByte();
                            break;
                        case SMFEvent.PITCH_BEND:
                            value = (bytes.readUnsignedByte() | (bytes.readUnsignedByte() << 7)) - 8192;
                            break;
                    }

                    sequence.add(new SMFEvent(eventType, value, deltaTime, time));
                }
        }

        totalTime = time;
    }

    // read meta event
    private boolean _readMetaEvent(int eventType, ByteArray bytes, int deltaTime, int time) {
        if (eventType != SMFEvent.META) return false;

        SMFEvent event;
        int value;
        String text;
        int metaEventType = bytes.readUnsignedByte() | 0xff00;
        int len = _readVariableLength(bytes, 0);

        if ((metaEventType & 0x00f0) == 0) {
            // meta text data
            event = new SMFEvent(metaEventType, len, deltaTime, time);
            try {
                // Assuming Shift-JIS equivalent here
                byte[] textBytes = new byte[len];
                for (int i = 0; i < len; i++) textBytes[i] = (byte) bytes.readUnsignedByte();
                text = new String(textBytes, "Shift-JIS");
            } catch (UnsupportedEncodingException e) {
                text = "";
                Logger.getLogger(SMFTrack.class.getName()).log(Level.SEVERE, null, e);
            }
            event.setText(text);
            switch (metaEventType) {
                case SMFEvent.META_TEXT:
                    _smfData.text = text;
                    break;
                case SMFEvent.META_TITLE:
                    if (_smfData.title == null) _smfData.title = text;
                    break;
                case SMFEvent.META_AUTHOR:
                    if (_smfData.author == null) _smfData.author = text;
                    break;
            }
            sequence.add(event);
        } else {
            switch (metaEventType) {
                case SMFEvent.META_TEMPO:
                    value = (bytes.readUnsignedByte() << 16) | bytes.readUnsignedShort();
                    // [usec/beat] => [beats/minute]
                    event = new SMFEvent(SMFEvent.META_TEMPO, 60000000 / value, deltaTime, time);
                    if (_smfData.bpm == 0) _smfData.bpm = event.value;
                    sequence.add(event);
                    break;
                case SMFEvent.META_TIME_SIGNATURE:
                    value = (bytes.readUnsignedByte() << 16) | (1 << bytes.readUnsignedByte());
                    event = new SMFEvent(SMFEvent.META_TIME_SIGNATURE, value, deltaTime, time);
                    if (_smfData.signature_d == 0) {
                        _smfData.signature_n = value >> 16;
                        _smfData.signature_d = value & 0xffff;
                    }
                    bytes.position += 2; // skip clocks per tick and 32nd notes per 24 MIDI clocks
                    sequence.add(event);
                    break;
                case SMFEvent.META_PORT:
                    value = bytes.readUnsignedByte();
                    break;
                case SMFEvent.META_TRACK_END:
                    _exitLoop = true;
                    break;
                default:
                    bytes.position += len;
                    break;
            }
        }
        return true;
    }

    // read system exclusive data
    private boolean _readSystemExclusive(int eventType, ByteArray bytes, int deltaTime, int time) {
        if (eventType != SMFEvent.SYSTEM_EXCLUSIVE && eventType != SMFEvent.SYSTEM_EXCLUSIVE_SHORT) return false;

        int i, b;
        SMFEvent event = new SMFEvent(eventType, 0, deltaTime, time);
        int len = _readVariableLength(bytes, 0);

        // read sysex bytes
        event.byteArray = new ByteArray();
        event.byteArray.writeByte(0xf0); // start
        for (i = 0; i < len; i++) {
            b = bytes.readUnsignedByte();
            event.byteArray.writeByte(b);
        }

        sequence.add(event);

        return true;
    }

    // read variable length
    private int _readVariableLength(ByteArray bytes, int time) {
        int t = bytes.readUnsignedByte();
        time += t & 0x7F;
        return ((t & 0x80) != 0) ? _readVariableLength(bytes, time << 7) : time;
    }

    // error
    //
    private RuntimeException _errorIncorrectData() {
        return new RuntimeException("The SMF File is not good.");
    }
}
