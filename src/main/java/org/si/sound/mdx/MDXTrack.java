//
// Track of MDX data
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.mdx;

import java.util.ArrayList;
import java.util.List;

import org.si.utils.ByteArray;


/** Track of MDX data */
public class MDXTrack {

    // variables
    //

    /** sequence */
    public List<MDXEvent> sequence = null;
    /** Return pointer of segno */
    public MDXEvent segnoPointer;
    /** timer B value to set */
    public int timerB;

    /** owner MDXData */
    public MDXData owner;
    /** channel number */
    public int channelNumber;

    // properties
    //

    /** has no data. */
    public boolean hasNoData() {
        return sequence.size() <= 1;
    }

    /** to string. */
    public String toString() {
        String text = "";
        int i, imax = sequence.size();
        for (i = 0; i < imax; i++) text += sequence.get(i) + "\n";
        return text;
    }

    // constructor
    //
    public MDXTrack(MDXData owner, int channelNumber) {
        this.owner = owner;
        this.channelNumber = channelNumber;
        sequence = new ArrayList<MDXEvent>();
        segnoPointer = null;
    }

    // operations
    //

    /** Clear. */
    public MDXTrack clear() {
        sequence.clear();
        segnoPointer = null;
        timerB = -1;
        return this;
    }

    /** Load track from byteArray. */
    public MDXTrack loadBytes(ByteArray bytes) {
        clear();

        int clock = 0, code, v, pos;
        List<MDXEvent> mem = new ArrayList<>();
        boolean exitLoop = false;

        while (!exitLoop && bytes.getBytesAvailable() > 0) {
            pos = bytes.position;
            code = bytes.readUnsignedByte();
            if (code < 0x80) { // rest
                newEvent(pos, mem, MDXEvent.REST, 0, 0, code + 1);
                clock += code + 1;
            } else if (code < 0xe0) { // note
                v = bytes.readUnsignedByte() + 1;
                newEvent(pos, mem, MDXEvent.NOTE, code - 0x80, 0, v);
                clock += v;
            } else {
                switch (code) {
                    // 2 operands
                    case MDXEvent.REGISTER:
                    case MDXEvent.FADEOUT:
                        newEvent(pos, mem, code, bytes.readUnsignedByte(), bytes.readUnsignedByte(), 0);
                        break;
                    // 1 operand
                    case MDXEvent.VOICE:
                    case MDXEvent.PAN:
                    case MDXEvent.VOLUME:
                    case MDXEvent.GATE:
                    case MDXEvent.KEY_ON_DELAY:
                    case MDXEvent.FREQUENCY:
                    case MDXEvent.LFO_DELAY:
                    case MDXEvent.SYNC_SEND:
                        newEvent(pos, mem, code, bytes.readUnsignedByte(), 0, 0);
                        break;
                    // no operands
                    case MDXEvent.VOLUME_DEC:
                    case MDXEvent.VOLUME_INC:
                    case MDXEvent.SLUR:
                    case MDXEvent.SET_PCM8:
                    case MDXEvent.SYNC_WAIT:
                        newEvent(pos, mem, code, 0, 0, 0);
                        break;
                    // 1 WORD
                    case MDXEvent.DETUNE:
                    case MDXEvent.PORTAMENT:
                        newEvent(pos, mem, code, bytes.readShort(), 0, 0); //...short?
                        break;
                    // REPEAT
                    case MDXEvent.REPEAT_BEGIN:
                        newEvent(pos, mem, code, bytes.readUnsignedByte(), bytes.readUnsignedByte(), 0);
                        break;
                    case MDXEvent.REPEAT_END:
                        newEvent(pos, mem, code, pos + bytes.readShort(), 0, 0);  // position of REPEAT_BEGIN
                        break;
                    case MDXEvent.REPEAT_BREAK:
                        newEvent(pos, mem, code, pos + bytes.readShort() + 2, 0, 0); // position of REPEAT_END
                        break;
                    // others
                    case MDXEvent.TIMERB:
                        v = bytes.readUnsignedByte();
                        if (clock == 0) timerB = v;
                        newEvent(pos, mem, code, v, 0, 0);
                        break;
                    case MDXEvent.PITCH_LFO:
                    case MDXEvent.VOLUME_LFO:
                        v = bytes.readUnsignedByte();
                        if (v == 0x80 || v == 0x81) newEvent(pos, mem, code, v, 0, 0);
                        else newEvent(pos, mem, code, v | (bytes.readUnsignedShort() << 8), bytes.readShort(), 0);
                        break;
                    case MDXEvent.OPM_LFO:
                        v = bytes.readUnsignedByte();
                        if (v == 0x80 || v == 0x81) newEvent(pos, mem, code, v << 16, 0, 0);
                        else {
                            v = (v << 16) | (bytes.readUnsignedByte() << 8) | bytes.readUnsignedByte();
                            newEvent(pos, mem, code, v, bytes.readShort(), 0);
                        }
                        break;
                    case MDXEvent.DATA_END: // ...?
                        v = bytes.readShort();
                        newEvent(pos, mem, code, v, 0, 0);
                        if (v > 0 && pos - v + 3 >= 0 && pos - v + 3 < mem.size()) segnoPointer = mem.get(pos - v + 3);
                        else if (v < 0 && pos + v + 3 >= 0 && pos + v + 3 < mem.size()) segnoPointer = mem.get(pos + v + 3);
                        exitLoop = true;
                        break;
                    default:
                        newEvent(pos, mem, MDXEvent.DATA_END, 0, 0, 0);
                        exitLoop = true;
                        break;
                }
            }
        }

//trace("---- ch", channelNumber, "----");
//trace(String(this));
        return this;
    }

    private MDXEvent newEvent(int pos, List<MDXEvent> mem, int type, int data, int data2, int deltaClock) {
        MDXEvent inst = new MDXEvent(type, data, data2, deltaClock);
        sequence.add(inst);
        while (mem.size() <= pos) {
            mem.add(null);
        }
        mem.set(pos, inst);
        return inst;
    }
}
