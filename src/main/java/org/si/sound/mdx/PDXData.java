//
// PDX data class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.mdx;

import org.si.sion.utils.SiONUtil;
import org.si.utils.AbstractLoader;
import org.si.utils.ByteArray;

import static org.si.utils.ByteArray.BIG_ENDIAN;


/** PDX data class */
public class PDXData extends AbstractLoader {

    // variables
    //

    /** PDX file name */
    public String fileName = "";
    /** ADPCM data */
    public ByteArray[] adpcmData;
    /** extracted PCM data */
    public double[][] pcmData;

    // constructor
    //

    /** constructor */
    public PDXData() {
        super(0);
        adpcmData = new ByteArray[96];
        pcmData = new double[96][];
    }

    // operations
    //

    /** Clear. */
    public PDXData clear() {
        fileName = "";
        for (int i = 0; i < 96; i++) {
            adpcmData[i] = null;
            pcmData[i] = null;
        }
        return this;
    }

    /**
     * Load PDX data from byteArray.
     *
     * @param bytes      ByteArray of PDX data
     * @param extractAll extract all ADPCM data to PCM data
     */
    public PDXData loadBytes(ByteArray bytes, boolean extractAll) {
        int offset, length;

        clear();
        bytes.endian = BIG_ENDIAN;

        for (int i = 0; i < 96; i++) {
            bytes.position = i * 8;
            offset = (int) bytes.readUnsignedInt();
            length = (int) bytes.readUnsignedInt();
            if (offset != 0 && length != 0) {
                adpcmData[i] = new ByteArray();
                bytes.position = offset;
                bytes.readBytes(adpcmData[i], 0, length);
                if (extractAll) pcmData[i] = SiONUtil.extractYM2151ADPCM(adpcmData[i], null, 1);
            }
        }

        return this;
    }

    /**
     * extract adpcm data
     *
     * @param noteNumber note number to extract.
     * @return extracted PCM data (monaural). returns null when the ADPCM data instanceof not assigned on specifyed note number.
     */
    public double[] extract(int noteNumber) {
        if (pcmData[noteNumber] != null) return pcmData[noteNumber];
        if (adpcmData[noteNumber] == null) return null;
        pcmData[noteNumber] = SiONUtil.extractYM2151ADPCM(adpcmData[noteNumber], null, 1);
        return pcmData[noteNumber];
    }
}
