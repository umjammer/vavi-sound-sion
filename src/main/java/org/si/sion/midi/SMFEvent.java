//
// SMF event
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.si.utils.ByteArray;


/** SMF event */
public class SMFEvent {

    // constant
    //
    public static final int NOTE_OFF = 0x80;
    public static final int NOTE_ON = 0x90;
    public static final int KEY_PRESSURE = 0xa0;
    public static final int CONTROL_CHANGE = 0xb0;
    public static final int PROGRAM_CHANGE = 0xc0;
    public static final int CHANNEL_PRESSURE = 0xd0;
    public static final int PITCH_BEND = 0xe0;
    public static final int SYSTEM_EXCLUSIVE = 0xf0;
    public static final int SYSTEM_EXCLUSIVE_SHORT = 0xf7;
    public static final int META = 0xff;

    public static final int META_SEQNUM = 0xff00;
    public static final int META_TEXT = 0xff01;
    public static final int META_AUTHOR = 0xff02;
    public static final int META_TITLE = 0xff03;
    public static final int META_INSTRUMENT = 0xff04;
    public static final int META_LYLICS = 0xff05;
    public static final int META_MARKER = 0xff06;
    public static final int META_CUE = 0xff07;
    public static final int META_PROGRAM_NAME = 0xff08;
    public static final int META_DEVICE_NAME = 0xff09;
    public static final int META_CHANNEL = 0xff20;
    public static final int META_PORT = 0xff21;
    public static final int META_TRACK_END = 0xff2f;
    public static final int META_TEMPO = 0xff51;
    public static final int META_SMPTE_OFFSET = 0xff54;
    public static final int META_TIME_SIGNATURE = 0xff58;
    public static final int META_KEY_SIGNATURE = 0xff59;
    public static final int META_SEQUENCER_SPEC = 0xff7f;

    public static final int CC_BANK_SELECT_MSB = 0;
    public static final int CC_BANK_SELECT_LSB = 32;
    public static final int CC_MODULATION = 1;
    public static final int CC_PORTAMENTO_TIME = 5;
    public static final int CC_DATA_ENTRY_MSB = 6;
    public static final int CC_DATA_ENTRY_LSB = 38;
    public static final int CC_VOLUME = 7;
    public static final int CC_BALANCE = 8;
    public static final int CC_PANPOD = 10;
    public static final int CC_EXPRESSION = 11;
    public static final int CC_SUSTAIN_PEDAL = 64;
    public static final int CC_PORTAMENTO = 65;
    public static final int CC_SOSTENUTO_PEDAL = 66;
    public static final int CC_SOFT_PEDAL = 67;
    public static final int CC_RESONANCE = 71;
    public static final int CC_RELEASE_TIME = 72;
    public static final int CC_ATTACK_TIME = 73;
    public static final int CC_CUTOFF_FREQ = 74;
    public static final int CC_DECAY_TIME = 75;
    public static final int CC_PROTAMENTO_CONTROL = 84;
    public static final int CC_REVERB_SEND = 91;
    public static final int CC_CHORUS_SEND = 93;
    public static final int CC_DELAY_SEND = 94;
    public static final int CC_NRPN_LSB = 98;
    public static final int CC_NRPN_MSB = 99;
    public static final int CC_RPN_LSB = 100;
    public static final int CC_RPN_MSB = 101;

    public static final int RPN_PITCHBEND_SENCE = 0;
    public static final int RPN_FINE_TUNE = 1;
    public static final int RPN_COARSE_TUNE = 2;

    static private final String[] _noteText = new String[] {"c ", "c+", "d ", "d+", "e ", "f ", "f+", "g ", "g+", "a ", "a+", "b "};

    // variables
    //
    public int type = 0;
    public int value = 0;
    public ByteArray byteArray = null;

    public int deltaTime = 0;
    public int time = 0;

    // properties
    //

    /** channel */
    public int getChannel() {
        return (type >= SYSTEM_EXCLUSIVE) ? 0 : (type & 0x0f);
    }

    /** note */
    public int getNote() {
        return value >> 16;
    }

    /** velocity */
    public int getVelocity() {
        return value & 0x7f;
    }

    /** text data */
    public String getText() {
        if (byteArray != null) {
            try {
                return byteArray.readUTFBytes(byteArray.getBytesAvailable());
            } catch (Exception ex) {
                Logger.getLogger(SMFEvent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return "";
    }

    public void setText(String str) {
        if (byteArray == null) byteArray = new ByteArray();
        try {
            byte[] bytes = str.getBytes("UTF-8");
            byteArray.writeBytes(bytes);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SMFEvent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** toString */
    @Override
    public String toString() {
        if ((type & 0xff00) != 0) {
            switch (type & 0xf0) {
                case META_TEMPO:
                    return "bpm(" + value + ")";
            }
        } else {
            String ret = "ch" + (type & 15) + ":";
            switch (type & 0xf0) {
                case NOTE_ON:
                    return ret + "ON(" + getNote() + ") " + getVelocity();
                case NOTE_OFF:
                    return ret + "OF(" + getNote() + ") " + getVelocity();
                case CONTROL_CHANGE:
                    return ret + "CC(" + (value >> 16) + ") " + (value & 0xffff);
                case PROGRAM_CHANGE:
                    return ret + "PC(" + value + ") ";
                case SYSTEM_EXCLUSIVE:
                    String text = "SX:";
                    if (byteArray != null) {
                        byteArray.position = 0;
                        while (byteArray.getBytesAvailable() > 0) {
                            text += Integer.toHexString(byteArray.readUnsignedByte()) + " ";
                        }
                    }
                    return ret + text;
            }
            return ret + "#" + Integer.toHexString(type) + "(" + value + ")";
        }

        return "#" + Integer.toHexString(type) + "(" + value + ")";
    }

    // constructor
    //
    public SMFEvent(int type, int value, int deltaTime, int time) {
        this.type = type;
        this.value = value;
        this.deltaTime = deltaTime;
        this.time = time;
    }
}
