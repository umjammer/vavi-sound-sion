//
// To create flash.media.Sound class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import java.util.Arrays;
import java.util.function.Function;

import org.si.utils.ByteArray;
import vavi.media.Sound;


/**
 * Refer from http://www.flashcodersbrighton.org/wordpress/?p=9
 *
 * @modified Kei Mesuda
 */
public final class SoundClass {

    private static final int[] _header = { // little endian
            0x09535746, 0xFFFFFFFF, 0x5f050078, 0xa00f0000, 0x010c0000, 0x08114400, 0x43000000, 0xffffff02,
            0x000b15bf, 0x00010000, 0x6e656353, 0x00312065, 0xc814bf00, 0x00000000, 0x00000000, 0x002e0010,
            0x08000000, 0x756f530a, 0x6c43646e, 0x00737361, 0x616c660b, 0x6d2e6873, 0x61696465, 0x756f5305,
            0x4f06646e, 0x63656a62, 0x76450f74, 0x44746e65, 0x61707369, 0x65686374, 0x6c660c72, 0x2e687361,
            0x6e657665, 0x05067374, 0x16021601, 0x16011803, 0x07050007, 0x03070102, 0x05020704, 0x03060507,
            0x00020000, 0x00020000, 0x00020000, 0x02010100, 0x01000408, 0x01000000, 0x04010102, 0x00030001,
            0x06050101, 0x4730d003, 0x01010000, 0x06070601, 0x49d030d0, 0x00004700, 0x01010202, 0x30d01f05,
            0x035d0065, 0x5d300366, 0x30046604, 0x0266025d, 0x66025d30, 0x1d005802, 0x01681d1d, 0xbf000047,
            0xFFFFFF03, 0x3f0001FF  // The last byte of "3f" means 44.1kHz/16bit/stereo
    };
    private static final int[] _footer = { // little endian
            0x000f133f, 0x00010000, 0x6f530001, 0x43646e75, 0x7373616c, 0x0f0b4400, 0x40000000
    };

    private static final int[] _bitRateList = {
            0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0, 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0
    };
    private static final int[] _frequencyList = {
            44100, 48000, 32000, 0
    };

    /**
     * load Sound class from mp3 data.
     *
     * @param bytes        source byteArray.
     * @param onComplete callback function when finished to create. the format instanceof function(sound:Sound) : void
     */
    public void loadMP3FromByteArray(ByteArray bytes, Function<Object, Object> onComplete) {
        if (bytes == null || onComplete == null) {
            return;
        }
        Sound sound = new Sound();
        byte[] src = Arrays.copyOf(bytes.buffer, bytes.length);
        sound.loadBytes(src);
        onComplete.apply(sound);
    }

    /**
     * load Sound class from PCM data.
     *
     * @param src        source byteArray.
     * @param onComplete callback function when finished to create. the format instanceof function(sound:Sound) : void
     * @param compressed compressed flag, true = mp3, false = raw wave.
     * @param sampleRate sampling rate
     * @param bitRate    bit rate
     * @param channels   channels
     */
    public void loadPCMFromByteArray(ByteArray src, Function<Object, Object> onComplete, boolean compressed, int sampleRate, int bitRate, int channels) {
        if (src == null || onComplete == null) {
            return;
        }

        Sound sound = new Sound();
        if (compressed) {
            int payloadOffset = Math.min(6, src.length);
            byte[] payload = new byte[src.length - payloadOffset];
            System.arraycopy(src.buffer, payloadOffset, payload, 0, payload.length);
            sound.loadBytes(payload);
            onComplete.apply(sound);
            return;
        }

        int sourceChannels = Math.max(1, channels);
        int bytesPerSample = Math.max(1, bitRate / 8);
        int frameSize = bytesPerSample * sourceChannels;
        if (frameSize <= 0) {
            onComplete.apply(sound);
            return;
        }

        int frameCount = src.length / frameSize;
        double[] samples = new double[frameCount * sourceChannels];
        int p = 0;
        for (int i = 0; i < frameCount * sourceChannels; i++) {
            if (bitRate == 16) {
                int lo = src.buffer[p++] & 0xff;
                int hi = src.buffer[p++] << 8;
                samples[i] = (short) (hi | lo) / 32768.0;
            } else if (bitRate == 8) {
                samples[i] = ((src.buffer[p++] & 0xff) - 128) / 128.0;
            } else {
                samples[i] = 0;
                p += bytesPerSample;
            }
        }

        sound.setStereoSamples(samples, sourceChannels, sampleRate);
        onComplete.apply(sound);
    }

    void _write(int[] vu, ByteArray bytes) {
        for (int j : vu) bytes.writeUnsignedInt(j);
    }

    /**
     * create Sound class from Vector of Number (44.1kHz stereo only).
     *
     * @param samples    The Vector.&lt;Number&gt; wave data creating from. The LRLR type 44.1kHz stereo data.
     * @param onComplete callback function when finished to create. the format instanceof function(sound:Sound) : void
     */
    public void create(double[] samples, Function<Object, Object> onComplete) {
        if (onComplete == null) {
            return;
        }
        Sound sound = new Sound();
        sound.setStereoSamples(samples, 2, 44100f);
        onComplete.apply(sound);
    }
}
