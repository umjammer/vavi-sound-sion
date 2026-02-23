package org.si.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;


public class ByteArray {

    public byte[] buffer;
    public int length;
    public int position;
    public static final int BIG_ENDIAN = 0;
    public static final int LITTLE_ENDIAN = 1;
    public int endian = BIG_ENDIAN;

    public ByteArray() {
        buffer = new byte[32];
        length = 0;
        position = 0;
    }

    public void ensureCapacity(int minCapacity) {
        if (minCapacity > buffer.length) {
            int newCapacity = buffer.length * 2;
            if (newCapacity < minCapacity) newCapacity = minCapacity;
            buffer = Arrays.copyOf(buffer, newCapacity);
        }
    }

    public int readUnsignedByte() {
        if (position >= length) return 0;
        return buffer[position++] & 0xFF;
    }

    public int readByte() {
        if (position >= length) return 0;
        return buffer[position++];
    }

    public int readUnsignedShort() {
        if (position + 2 > length) return 0;
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        if (endian == BIG_ENDIAN) return (ch1 << 8) + ch2;
        else return (ch2 << 8) + ch1;
    }

    public int readShort() {
        return (short) readUnsignedShort();
    }

    public long readUnsignedInt() {
        if (position + 4 > length) return 0;
        long ch1 = readUnsignedByte();
        long ch2 = readUnsignedByte();
        long ch3 = readUnsignedByte();
        long ch4 = readUnsignedByte();
        if (endian == BIG_ENDIAN) return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4;
        else return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1;
    }

    public float readFloat() {
        return Float.intBitsToFloat((int) readUnsignedInt());
    }

    public int readInt() {
        return (int) readUnsignedInt();
    }

    public String readMultiByte(int len, String charsetName) {
        if (position + len > length) len = length - position;
        if (len <= 0) return "";
        String s = new String(buffer, position, len, Charset.forName(charsetName.equals("us-ascii") ? "US-ASCII" : "UTF-8"));
        position += len;
        return s;
    }

    public void position(int p) {
        position = p;
    }

    public int bytesAvailable() {
        return length - position;
    }

    public int getBytesAvailable() {
        return length - position;
    }

    public void clear() {
        position = 0;
        length = 0;
    }

    public String readUTFBytes(int len) {
        String s = new String(buffer, position, len, StandardCharsets.UTF_8);
        position += len;
        return s;
    }

    public void writeByte(int value) {
        ensureCapacity(position + 1);
        buffer[position++] = (byte) value;
        if (position > length) length = position;
    }

    public void writeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        ensureCapacity(position + bytes.length);
        System.arraycopy(bytes, 0, buffer, position, bytes.length);
        position += bytes.length;
        if (position > length) length = position;
    }

    public void writeBytes(ByteArray bytes) {
        if (bytes == null) {
            return;
        }
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(ByteArray bytes, int offset, int len) {
        if (bytes == null || len <= 0 || offset >= bytes.length) {
            return;
        }
        if (offset < 0) {
            offset = 0;
        }
        int available = bytes.length - offset;
        int copyLength = Math.min(len, available);
        if (copyLength <= 0) {
            return;
        }
        ensureCapacity(position + copyLength);
        System.arraycopy(bytes.buffer, offset, buffer, position, copyLength);
        position += copyLength;
        if (position > length) {
            length = position;
        }
    }

    public void readBytes(ByteArray bytes, int offset, int len) {
        bytes.ensureCapacity(offset + len);
        System.arraycopy(this.buffer, this.position, bytes.buffer, offset, len);
        bytes.length = Math.max(bytes.length, offset + len);
        this.position += len;
    }
    public void writeFloat(double v) {
        writeFloat((float) v);
    }

    public void writeFloat(float v) {
        int i = Float.floatToIntBits(v);
        writeInt(i);
    }

    public void writeInt(int v) {
        ensureCapacity(position + 4);
        if (endian == BIG_ENDIAN) {
            buffer[position++] = (byte) (v >>> 24);
            buffer[position++] = (byte) (v >>> 16);
            buffer[position++] = (byte) (v >>> 8);
            buffer[position++] = (byte) (v);
        } else {
            buffer[position++] = (byte) (v);
            buffer[position++] = (byte) (v >>> 8);
            buffer[position++] = (byte) (v >>> 16);
            buffer[position++] = (byte) (v >>> 24);
        }
        if (position > length) {
            length = position;
        }
    }

    public void writeShort(int v) {
        ensureCapacity(position + 2);
        if (endian == BIG_ENDIAN) {
            buffer[position++] = (byte) (v >>> 8);
            buffer[position++] = (byte) (v >>> 0);
        } else {
            buffer[position++] = (byte) (v >>> 0);
            buffer[position++] = (byte) (v >>> 8);
        }
        if (position > length) length = position;
    }

    protected void compress() {
        Deflater deflater = new Deflater();
        deflater.setInput(buffer, 0, length);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(64, length));
        byte[] chunk = new byte[1024];
        while (!deflater.finished()) {
            int n = deflater.deflate(chunk);
            if (n <= 0) {
                break;
            }
            baos.write(chunk, 0, n);
        }
        deflater.end();

        byte[] compressed = baos.toByteArray();
        buffer = Arrays.copyOf(compressed, Math.max(compressed.length, 1));
        length = compressed.length;
        position = length;
    }

    public void writeUnsignedInt(int i) {
        writeInt(i);
    }

    protected void writeMultiByte(String string, String encoding) {
        writeBytes(string.getBytes(Charset.forName(encoding)));
    }
}
