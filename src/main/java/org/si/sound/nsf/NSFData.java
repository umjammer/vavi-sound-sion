//
// NSF data class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.nsf;

import org.si.utils.ByteArray;


/** NSF data class */
public class NSFData {

    // variables
    //

    public int version;
    public int songCount;
    public int startSongID;
    public int loadAddress;
    public int initAddress;
    public int playAddress;
    public String title;
    public String artist;
    public String copyright;
    public int speedNRSC;
    public int speedPAL;
    public int NTSC_PALbits;
    public int[] bankSwitch = new int[8];
    public int extraChipFlag;
    public int reserved;

    // properties
    //

    /** Is avaiblable ? */
    public boolean isAvailable() {
        return false;
    }

    /** to string. */
    public String toString() {
        String text = "";
        return text;
    }

    // constructor
    //
    public NSFData() {
    }

    // operations
    //

    /** Clear. */
    public NSFData clear() {
        return this;
    }

    /** Load NSF data from byteArray. */
    public NSFData loadBytes(ByteArray bytes) {
        bytes.position = 0;
        clear();

        if (!bytes.readMultiByte(4, "us-ascii").equals("NESM")) return this;
        bytes.position = 5;
        version = bytes.readUnsignedByte();
        songCount = bytes.readUnsignedByte();
        startSongID = bytes.readUnsignedByte();
        loadAddress = bytes.readUnsignedShort();
        initAddress = bytes.readUnsignedShort();
        playAddress = bytes.readUnsignedShort();

        title = bytes.readMultiByte(32, "us-ascii"); //shift_jis
        artist = bytes.readMultiByte(32, "us-ascii"); //shift_jis
        copyright = bytes.readMultiByte(32, "us-ascii"); //shift_jis

        speedNRSC = bytes.readUnsignedShort();
        for (int i = 0; i < 8; i++) bankSwitch[i] = bytes.readUnsignedByte();
        speedPAL = bytes.readUnsignedShort();
        NTSC_PALbits = bytes.readUnsignedByte();
        extraChipFlag = bytes.readUnsignedByte();
        reserved = (int) bytes.readUnsignedInt();
        bytes.position = 128;

        return this;
    }
}
