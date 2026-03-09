//
// Extended ByteArray
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.si.as3.display.BitmapData;


/** Extended ByteArray, png image serialize, IFF chunk structure, FileReference operations. */
public class ByteArrayExt extends ByteArray {

    // variables
    //
    private int[] crc32 = null;

    /** name of this ByteArray */
    public String name = null;

    // constructor
    //

    /** constructor */
    public ByteArrayExt(ByteArray copyFrom /* = null */) {
        super();
        if (copyFrom != null) {
            this.writeBytes(copyFrom);
            this.endian = copyFrom.endian;
            this.position = 0;
        }
    }

    // bitmap data operations
    //

    /**
     * translate from BitmapData
     *
     * @param bmd BitmapData translating from.
     * @return this instance
     */
    public ByteArrayExt fromBitmapData(BitmapData bmd) {
        int x, y, i, w = bmd.width, h = bmd.height, len, p;
        this.clear();
        len = bmd.getPixel(w - 1, h - 1);
        for (y = 0, i = 0; y < h && i < len; y++)
            for (x = 0; x < w && i < len; x++, i++) {
                p = bmd.getPixel(x, y);
                this.writeByte(p >>> 16);
                if (++i >= len) break;
                this.writeByte(p >>> 8);
                if (++i >= len) break;
                this.writeByte(p);
            }
        this.position = 0;
        return this;
    }

    /**
     * translate to BitmapData
     *
     * @param width       ((BitmapData) same)'s constructor, set 0 to calculate automatically.
     * @param height      ((BitmapData) same)'s constructor, set 0 to calculate automatically.
     * @param transparent ((BitmapData) same)'s constructor.
     * @param fillColor   ((BitmapData) same)'s constructor.
     * @return translated BitmapData
     */
    public BitmapData toBitmapData(int width, int height, boolean transparent, int fillColor) {
        int x = 0, y, reqh;
        BitmapData bmd;
        int len = this.length, p;
        if (width == 0) width = (((int) (Math.sqrt(len) + 65535. / 65536)) + 15) & (~15);
        reqh = (((int) ((double) len / width + 65535. / 65536)) + 15) & (~15);
        if (height == 0 || reqh > height) height = reqh;
        bmd = new BitmapData(width, height, transparent, fillColor);
        this.position = 0;
        for (y = 0; y < height; y++)
            for (x = 0; x < width; x++) {
                if (this.getBytesAvailable() < 3) break;
                bmd.setPixel32(x, y, 0xff000000 | ((this.readUnsignedShort() << 8) | this.readUnsignedByte()));
            }
        p = 0xff000000;
        if (this.bytesAvailable() > 0) p |= this.readUnsignedByte() << 16;
        if (this.bytesAvailable() > 0) p |= this.readUnsignedByte() << 8;
        if (this.bytesAvailable() > 0) p |= this.readUnsignedByte();
        bmd.setPixel32(x, y, p);
        this.position = 0;
        bmd.setPixel32(x, y, 0xff000000 | this.length);
        return bmd;
    }

    /**
     * translate to 24bit png data
     *
     * @param width  png file width, set 0 to calculate automatically.
     * @param height png file height, set 0 to calculate automatically.
     * @return ByteArrayExt of PNG data
     */
    public ByteArrayExt toPNGData(int width, int height) {
        int i, imax, reqh, pixels = (this.length + 2) / 3, y;
        ByteArrayExt png = new ByteArrayExt(null);
        ByteArray header = new ByteArray();
        ByteArray content = new ByteArray();
        // settings
        if (width == 0) width = (((int) (Math.sqrt(pixels) + 65535. / 65536)) + 15) & (~15);
        reqh = (((int) ((double) pixels / width + 65535. / 65536)) + 15) & (~15);
        if (height == 0 || reqh > height) height = reqh;
        header.writeInt(width);  // width
        header.writeInt(height); // height
        header.writeUnsignedInt(0x08020000); // 24bit RGB
        header.writeByte(0);
        imax = pixels - width;
        for (y = 0, i = 0; i < imax; i += width, y++) {
            content.writeByte(0);
            content.writeBytes(this, i * 3, width * 3);
        }
        content.writeByte(0);
        content.writeBytes(this, i * 3, this.length - i * 3);
        imax = (i + width) * 3;
        for (i = this.length; i < imax; i++) content.writeByte(0);
        imax = width * 3 + 1;
        for (y++; y < height; y++) for (i = 0; i < imax; i++) content.writeByte(0);
        i = this.length;
        content.position -= 3;
        content.writeByte(i >>> 16);
        content.writeByte(i >>> 8);
        content.writeByte(i);
        content.compress();

        // write png data
        png.writeUnsignedInt(0x89504e47);
        png.writeUnsignedInt(0x0D0A1A0A);
        png_writeChunk(0x49484452, header, png);
        png_writeChunk(0x49444154, content, png);
        png_writeChunk(0x49454E44, new ByteArray(), png);
        png.position = 0;

        return png;
    }

    // write png chunk
    void png_writeChunk(int type, ByteArray data, ByteArrayExt png) {
        png.writeUnsignedInt(data.length);
        int crcStartAt = png.position;
        png.writeUnsignedInt(type);
        png.writeBytes(data);
        png.writeUnsignedInt(calculateCRC32(png, crcStartAt, png.position - crcStartAt));
    }

    // IFF chunk operations
    //

    /** write IFF chunk */
    public void writeChunk(String chunkID, ByteArray data, String listType /* = null */) {
        boolean isList = (chunkID.equals("RIFF") || chunkID.equals("LIST"));
        int len = ((data != null) ? data.length : 0) + ((isList) ? 4 : 0);
        this.writeMultiByte((chunkID + "    ").substring(0, 4), "us-ascii");
        this.writeInt(len);
        if (isList) {
            if (listType != null) this.writeMultiByte((listType + "    ").substring(0, 4), "us-ascii");
            else this.writeMultiByte("    ", "us-ascii");
        }
        if (data != null) {
            this.writeBytes(data);
            if ((len & 1) != 0) this.writeByte(0);
        }
    }

    /** read (or search) IFF chunk from current position. */
    public Object readChunk(ByteArray bytes, int offset, String searchChunkID) {
        String id, type = null;
        int len;
        while (this.bytesAvailable() > 0) {
            id = this.readMultiByte(4, "us-ascii");
            len = this.readInt();
            if (searchChunkID == null || searchChunkID.equals(id)) {
                if (id.equals("RIFF") || id.equals("LIST")) {
                    type = this.readMultiByte(4, "us-ascii");
                    this.readBytes(bytes, offset, len - 4);
                } else {
                    this.readBytes(bytes, offset, len);
                }
                if ((len & 1) != 0) this.readByte();
                bytes.endian = this.endian;
                return Map.of("chunkID", id, "length", len, "listType", type);
            }
            this.position += len + (len & 1);
        }
        return null;
    }

    /** read all IFF chunks from current position. */
    public Map<String, Object> readAllChunks() {
        Map<String, Object> ret = new HashMap<>();
        ByteArrayExt pickup;
        while (true) {
            pickup = new ByteArrayExt(null);
            Object headerObj = readChunk(pickup, 0, null);
            if (headerObj == null) break;
            Map<String, Object> header = (Map<String, Object>) headerObj;
            String chunkID = (String) header.get("chunkID");
            if (ret.containsKey(chunkID)) {
                Object existing = ret.get(chunkID);
                if (existing instanceof List) {
                    ((List<ByteArrayExt>) existing).add(pickup);
                } else {
                    List<ByteArrayExt> list = new ArrayList<>();
                    list.add((ByteArrayExt) existing);
                    list.add(pickup);
                    ret.put(chunkID, list);
                }
            } else {
                ret.put(chunkID, pickup);
            }
        }
        return ret;
    }

    // URL operations
    //

    /**
     * load from URL
     */
    public void load(String url, Consumer<Object> onComplete, Consumer<Object> onCancel, Consumer<Object> onError) {
        // Not implemented in Java stub
    }

    // FileReference operations
    //

    /**
     * Call FileReference::browse().
     */
    public void browse(Consumer<Object> onComplete, Consumer<Object> onCancel, Consumer<Object> onError, String fileFilterName, String extensions) {
        // Not implemented in Java stub
    }

    /**
     * Call FileReference::save().
     */
    public void save(String defaultFileName, Consumer<Object> onComplete, Consumer<Object> onCancel, Consumer<Object> onError) {
        // Not implemented in Java stub
    }

    // zip file operations
    //

    /**
     * Expand zip file including plural files.
     *
     * @return List of ByteArrayExt
     */
    public List<ByteArrayExt> expandZipFile() {
        ByteArray bytes = new ByteArray();
        String fileName = "";
        ByteArrayExt bae;
        List<ByteArrayExt> result = new ArrayList<>();
        int flNameLength, xfldLength, compSize, compMethod, signature;

        bytes.endian = ByteArray.LITTLE_ENDIAN;
        this.endian = ByteArray.LITTLE_ENDIAN;
        this.position = 0;
        while (this.position < this.length) {
            this.readBytes(bytes, 0, 30);
            bytes.position = 0;
            signature = (int)bytes.readUnsignedInt();
            if (signature != 0x04034b50) break; // chech signature
            bytes.position = 8;
            compMethod = bytes.readByte();
            bytes.position = 26;
            flNameLength = bytes.readShort();
            bytes.position = 28;
            xfldLength = bytes.readShort();

            this.readBytes(bytes, 30, flNameLength + xfldLength);
            bytes.position = 30;
            fileName = bytes.readUTFBytes(flNameLength);
            bytes.position = 18;
            compSize = (int)bytes.readUnsignedInt();

            bae = new ByteArrayExt(null);
            this.readBytes(bae, 0, compSize);
            // Compression is not fully ported to Java
            bae.name = fileName;
            result.add(bae);
        }

        return result;
    }

    // utilities
    //

    /** calculate crc32 chuck sum */
    public int calculateCRC32(ByteArray byteArray, int offset, int length) {
        int i, j, c, currentPosition;
        if (crc32 == null) {
            crc32 = new int[256];
            for (i = 0; i < 256; i++) {
                for (c = i, j = 0; j < 8; j++) c = (c & 1) != 0 ? 0xedb88320 ^ (c >>> 1) : (c >>> 1);
                crc32[i] = c;
            }
        }

        if (length == 0) length = byteArray.length;
        currentPosition = byteArray.position;
        byteArray.position = offset;
        for (c = 0xffffffff, i = 0; i < length; i++) {
            j = (c ^ byteArray.readUnsignedByte()) & 255;
            c >>>= 8;
            c ^= crc32[j];
        }
        byteArray.position = currentPosition;

        return c ^ 0xffffffff;
    }
}
