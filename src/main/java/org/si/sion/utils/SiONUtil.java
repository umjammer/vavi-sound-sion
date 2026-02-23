//
// SiON Utilities
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWaveTable;
import org.si.utils.ByteArray;
import org.si.utils.SLLNumber;
import vavi.media.Sound;


/** Utilities for SiON */
public class SiONUtil {

    private static final Logger logger = System.getLogger(SiONUtil.class.getName());
    public static int getTimer() {
        return (int) System.currentTimeMillis();
    }

    // PCM data transformation (for PCM Data %7)
    //

    /**
     * logarithmical transformation of Sound data. The transformed data type instanceof Vector.&lt;int&gt;. This data instanceof used for PCM sound module (%7).
     *
     * @param data            The Sound data transforming from.
     * @param dst             The Vector.&lt;int&gt; instance to put result. You can pass null to create new Vector.&lt;int&gt; inside.
     * @param dstChannelCount channel count of destination samples. 0 sets same with srcChannelCount
     * @param sampleMax       The maximum sample count to transform. The length of transformed data instanceof limited by this value.
     * @param startPosition   Start position to extract. -1 to set extraction continuously.
     * @param maximize        maximize input sample
     * @return logarithmical transformed data.
     */
    public static int[] logTrans(Sound data, int[] dst, int dstChannelCount, int sampleMax, int startPosition, boolean maximize) {
        ByteArray wave = new ByteArray();
        int samples = data.extract(wave, sampleMax, startPosition);
        return logTransByteArray(wave, dst, dstChannelCount, maximize);
    }

    /**
     * logarithmical transformation of Vector.&lt;Number&gt; wave data. The transformed data type instanceof Vector.&lt;int&gt;. This data instanceof used for PCM sound module (%7).
     *
     * @param src             The Vector.&lt;Number&gt; wave data transforming from. This usually comes from SiONDriver.render().
     * @param srcChannelCount channel count of source samples.
     * @param dst             The Vector.&lt;int&gt; instance to put result. You can pass null to create new Vector.&lt;int&gt; inside.
     * @param dstChannelCount channel count of destination samples. 0 sets same with srcChannelCount
     * @return logarithmical transformed data.
     */
    public static int[] logTransVector(double[] src, int srcChannelCount /* = 2 */, int[] dst /* = null */, int dstChannelCount /* = 0 */, boolean maximize /* = true */) {
        int i, j;
        double n;
        int imax;
        int logmax = SiOPMTable.LOG_TABLE_BOTTOM;
        if (srcChannelCount == dstChannelCount || dstChannelCount == 0) {
            imax = src.length;
            if (dst == null || dst.length < imax) dst = new int[imax];
            for (i = 0; i < imax; i++) {
                dst[i] = SiOPMTable.calcLogTableIndex(src[i]);
                if (dst[i] < logmax) logmax = dst[i];
            }
        } else if (srcChannelCount == 2) { // dstChannelCount = 1
            imax = src.length >> 1;
            if (dst == null || dst.length < imax) dst = new int[imax];
            for (i = 0, j = 0; i < imax; i++) {
                n = src[j];
                j++;
                n += src[j];
                j++;
                dst[i] = SiOPMTable.calcLogTableIndex(n * 0.5);
                if (dst[i] < logmax) logmax = dst[i];
            }
        } else { // srcChannelCount=1 > dstChannelCount=2
            imax = src.length;
            if (dst == null || dst.length < imax) dst = new int[imax];
            for (i = 0, j = 0; i < imax; i++, j += 2) {
                dst[j + 1] = dst[j] = SiOPMTable.calcLogTableIndex(src[i]);
                if (dst[j] < logmax) logmax = dst[j];
            }
        }
        if (maximize && logmax > 1) _amplifyLogData(dst, logmax);
        return dst;
    }

    /**
     * logarithmical transformation of ByteArray wave data. The transformed datas type instanceof Vector.&lt;int&gt;. This data instanceof used for PCM sound module (%7).
     *
     * @param src             The ByteArray wave data transforming from. This instanceof ussualy from Sound.extract().
     * @param dst             The Vector.&lt;int&gt; instance to put result. You can pass null to create new Vector.&lt;int&gt; inside.
     * @param dstChannelCount channel count of destination samples. 0 sets same with srcChannelCount
     * @return logarithmical transformed data.
     */
    public static int[] logTransByteArray(ByteArray src, int[] dst, int dstChannelCount, boolean maximize) {
        int i, imax, logmax = SiOPMTable.LOG_TABLE_BOTTOM;

        src.position = 0;
        if (dstChannelCount == 2) {
            imax = src.length >> 2;
            if (dst == null || dst.length < imax) dst = new int[imax];
            for (i = 0; i < imax; i++) {
                dst[i] = SiOPMTable.calcLogTableIndex(src.readFloat());
                if (dst[i] < logmax) logmax = dst[i];
            }
        } else {
            imax = src.length >> 3;
            if (dst == null || dst.length < imax) dst = new int[imax];
            for (i = 0; i < imax; i++) {
                dst[i] = SiOPMTable.calcLogTableIndex((src.readFloat() + src.readFloat()) * 0.5);
                if (dst[i] < logmax) logmax = dst[i];
            }
        }

        if (maximize && logmax > 1) _amplifyLogData(dst, logmax);
        return dst;
    }

    // amplift log data
    private static void _amplifyLogData(int[] src, int gain) {
        int i, imax = src.length;
        gain &= ~1;
        for (i = 0; i < imax; i++) src[i] -= gain;
    }

    // wave data
    //

    /**
     * put Sound.extract() result into Vector.&lt;Number&gt;. This data instanceof used for sampler module (%10).
     *
     * @param src             The Sound data extracting from.
     * @param dst             The Vector.&lt;Number&gt; instance to put result. You can pass null to create new Vector.&lt;Number&gt; inside.
     * @param dstChannelCount channel count of extracted data. 1 for monaural, 2 for stereo.
     * @param length          The maximum sample count to extract. The length of returning vector instanceof limited by this value.
     * @param startPosition   Start position to extract. -1 to set extraction continuously.
     * @return extracted data.
     */
    public static double[] extract(Sound src, double[] dst, int dstChannelCount, int length, int startPosition) {
        ByteArray wave = new ByteArray();
        int i;
        int imax;
        src.extract(wave, length, startPosition);
        wave.position = 0;

        if (dstChannelCount == 2) {
            // stereo
            imax = wave.length >> 2;
            if (dst == null || dst.length < imax) dst = new double[imax];
            for (i = 0; i < imax; i++) {
                dst[i] = wave.readFloat();
            }
        } else {
            // monaural
            imax = wave.length >> 3;
            if (dst == null || dst.length < imax) dst = new double[imax];
            for (i = 0; i < imax; i++) {
                dst[i] = (wave.readFloat() + wave.readFloat()) * 0.6;
            }
        }
        return dst;
    }

    /**
     * extract 2a03's DPCM data.<br/>
     * DPCM frequency table = [
     * 0=k14o2e,
     * 1=k18o2f+,
     * 2=k13o2g+,
     * 3=k16o2a,
     * 4=k13o2b,
     * 5=k16o3c+,
     * 6=k17o3d+,
     * 7=k14o3e,
     * 8=k18o3f+,
     * 9=k16o3a,
     * 10=k20o3b,
     * 11=k7o4c+,
     * 12=k24o4e,
     * 13=k13o4g+,
     * 14=k5o4b,
     * 15=k4o5e]
     *
     * @param src             The DPCM ByteArray data extracting from.
     * @param initValue       initial value of $4011.
     * @param dst             The Vector.&lt;Number&gt; instance to put result. You can pass null to create new Vector.&lt;Number&gt; inside.
     * @param dstChannelCount channel count of extracted data. 1 for monaural, 2 for stereo.
     * @return extracted data.
     */
    public double[] extractDPCM(ByteArray src, int initValue, double[] dst, int dstChannelCount) {
        int data, i, imax, j, sample, output;

        imax = src.length * dstChannelCount * 8;
        if (dst == null || dst.length < imax) dst = new double[imax];

        output = initValue;
        src.position = 0;
        for (i = 0; i < imax; ) {
            data = src.readUnsignedByte();
            for (j = 7; j >= 0; --j) {
                if (((data >> j) & 1) != 0) if (output < 126) output += 2;
                else if (output > 1) output -= 2;
                sample = (int) ((output - 64) * 0.015625);
                dst[i] = sample;
                i++;
                if (dstChannelCount == 2) {
                    dst[i] = sample;
                    i++;
                }
            }
        }

        return dst;
    }

    /**
     * extract ADPCM data (YM2151). this algorism instanceof from x68ksound.dll's source code.
     * _freqTable= [26, 31, 38, 43, 50];
     *
     * @param src             The ADPCM ByteArray data extracting from.
     * @param dst             The Vector.&lt;Number&gt; instance to put result. You can pass null to create new Vector.&lt;Number&gt; inside.
     * @param dstChannelCount channel count of extracted data. 1 for monaural, 2 for stereo.
     * @return extracted data.
     */
    public static double[] extractYM2151ADPCM(ByteArray src, double[] dst /* = null */, int dstChannelCount /* = 1 */) {
        int data, r, i, imax, pcm = 0;
        double sample;
        int InpPcm = 0, InpPcm_prev = 0, scale = 0, output = 0;

        // chaging ratio table
        int[] crTable = {1, 3, 5, 7, 9, 11, 13, 15, -1, -3, -5, -7, -9, -11, -13, -15};
        // from x68ksound.dll source
        int[] dltLTBL = {
                16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66,
                73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
                337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552
        };
        int[] DCT = {-1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8};

        imax = src.length * dstChannelCount * 2;
        if (dst == null || dst.length < imax) dst = new double[imax];

        for (i = 0; i < imax; ) {
            data = src.readUnsignedByte();

            r = data & 0x0f;
            pcm += (dltLTBL[scale] * crTable[r]) >> 3;
            scale += DCT[r];
            if (pcm < -2048) pcm = -2048;
            else if (pcm > 2047) pcm = 2047;
            if (scale < 0) scale = 0;
            else if (scale > 48) scale = 48;
            InpPcm = (pcm & 0xfffffffc) << 8;
            output = ((InpPcm << 9) - (InpPcm_prev << 9) + 459 * output) >> 9;
            InpPcm_prev = InpPcm;
            sample = (int) (output * 0.0000019073486328125);
            dst[i] = sample;
            i++;
            if (dstChannelCount == 2) {
                dst[i] = sample;
                i++;
            }

            r = (data >> 4) & 0x0f;
            pcm += (dltLTBL[scale] * crTable[r]) >> 3;
            scale += DCT[r];
            if (pcm < -2048) pcm = -2048;
            else if (pcm > 2047) pcm = 2047;
            if (scale < 0) scale = 0;
            else if (scale > 48) scale = 48;
            InpPcm = (pcm & 0xfffffffc) << 8;
            output = ((InpPcm << 9) - (InpPcm_prev << 9) + 459 * output) >> 9;
            InpPcm_prev = InpPcm;
            sample = output * 0.0000019073486328125;
            dst[i] = sample;
            i++;
            if (dstChannelCount == 2) {
                dst[i] = sample;
                i++;
            }
        }

        return dst;
    }

    /**
     * extract ADPCM data (YM2608)
     *
     * @param src             The ADPCM ByteArray data extracting from.
     * @param dst             The Vector.&lt;Number&gt; instance to put result. You can pass null to create new Vector.&lt;Number&gt; inside.
     * @param dstChannelCount channel count of extracted data. 1 for monaural, 2 for stereo.
     * @return extracted data.
     */
    public double[] extractYM2608ADPCM(ByteArray src, double[] dst /* = null */, int dstChannelCount /* = 1 */) {
        int data, r0, r1, i, imax;
        double sample;
        int predRate = 127, output = 0;

        // changing ratio table
        int[] crTable = {
        1, 3, 5, 7, 9, 11, 13, 15, -1, -3, -5, -7, -9, -11, -13, -15};
        // prediction updating table
        int[] puTable = {
        57, 57, 57, 57, 77, 102, 128, 153, 57, 57, 57, 57, 77, 102, 128, 153};

        imax = src.length * dstChannelCount * 2;
        if (dst == null || dst.length < imax) dst = new double[imax];

        for (i = 0; i < imax; ) {
            data = src.readUnsignedByte();
            r0 = data & 0x0f;
            r1 = (data >> 4) & 0x0f;

            predRate *= crTable[r0];
            predRate >>= 3;
            output += predRate;
            sample = output * 0.000030517578125;
            dst[i] = sample;
            i++;
            if (dstChannelCount == 2) {
                dst[i] = sample;
                i++;
            }
            predRate *= puTable[r0];
            predRate >>= 6;
            if (predRate > 0) {
                if (predRate < 127) predRate = 127;
                else if (predRate > 24576) predRate = 24576;
            } else {
                if (predRate > -127) predRate = -127;
                else if (predRate < -24576) predRate = -24576;
            }

            predRate *= crTable[r1];
            predRate >>= 3;
            output += predRate;
            sample = output * 0.000030517578125;
            dst[i] = sample;
            i++;
            if (dstChannelCount == 2) {
                dst[i] = sample;
                i++;
            }
            predRate *= puTable[r1];
            predRate >>= 6;
            if (predRate > 0) {
                if (predRate < 127) predRate = 127;
                else if (predRate > 24576) predRate = 24576;
            } else {
                if (predRate > -127) predRate = -127;
                else if (predRate < -24576) predRate = -24576;
            }
        }

        for (i = 0; i < imax; i++) {
            if (dst[i] < -1) dst[i] = -1;
            else if (dst[i] > 1) dst[i] = 1;
        }

        return dst;
    }

    // calculation
    //

    /**
     * Calculate sample length from 16th beat.
     *
     * @param bpm    Beat per minuits.
     * @param beat16 Count of 16th beat.
     * @return sample length.
     */
    public static double calcSampleLength(double bpm, double beat16) {
        // 661500 = 44100*60/4
        return beat16 * 661500 / bpm;
    }

    /**
     * Check silent length at the head of Sound.
     *
     * @param src          source Sound
     * @param rmsThreshold threshold level to detect sound.
     * @return silent length in sample count.
     */
    public static int getHeadSilence(Sound src, double rmsThreshold) {
        ByteArray wave = new ByteArray();
        int i, imax, extracted;
        double l, r, ms;
        int sp = 0;
        SLLNumber msWindow = SLLNumber.allocRing(22, 0); // 0.5ms

        rmsThreshold *= rmsThreshold;
        rmsThreshold *= 22;

        imax = 1152;
        ms = 0;
        for (extracted = 0; imax == 1152; extracted += 1152) {
            wave.length = 0;
            imax = src.extract(wave, 1152, sp);
            wave.position = 0;
            for (i = 0; i < imax; i++) {
                l = wave.readFloat();
                r = wave.readFloat();
                ms -= msWindow.n;
                msWindow = msWindow.next;
                msWindow.n = l * l + r * r;
                ms += msWindow.n;
                if (ms >= rmsThreshold) return extracted + i - 22;
            }
            sp = -1;
        }

        SLLNumber.freeRing(msWindow);

        return extracted;
    }

    /**
     * Get end gap of Sound
     *
     * @param src          source Sound
     * @param rmsThreshold threshold level to detect sound.
     * @param maxLength    maximum length to search [sample count]. ussually mp3's end gap instanceof less than 1152.
     * @return silent length in sample count.
     */
    public static int getEndGap(Sound src, double rmsThreshold, int maxLength) {
        ByteArray wave = new ByteArray();
        double[] ms = new double[1152];
        int i, imax, extracted;
        double l, r;
        int sp;

        rmsThreshold *= rmsThreshold;
        sp = (int) (src.length * 44.1) - 1152;

        for (extracted = 0; extracted < maxLength; extracted += imax) {
            imax = src.extract(wave, 1152, sp);
            wave.position = 0;
            for (i = 0; i < imax; i++) {
                l = wave.readFloat();
                r = wave.readFloat();
                ms[i] = l * l + r * r;
            }
            for (i = imax - 1; i >= 0; --i) {
                if (ms[i] >= rmsThreshold) {
                    extracted += i;
                    logger.log(Level.TRACE, "extracted: " + extracted);
                    return (extracted < maxLength) ? extracted : maxLength;
                }
            }
            sp -= 1152;
            if (sp < 0) break;
        }

        return maxLength;
    }

    /**
     * Detect distance[ms] of 2 peaks, [estimated bpm] = 60000/getPeakDistance().
     *
     * @param sample stereo samples, the length must be grater than 59136*2(stereo).
     * @return distance[ms] of 2 peaks.
     */
    public double getPeakDistance(double[] sample) {
        int i, j, k, idx;
        double n, m, envAccum;

        // 461.9375 = 59128/128, 59128 = length for 2 beats on bpm=89.5
        if (_envelop == null) _envelop = new double[462];
        if (_xcorr == null) _xcorr = new double[113];

        // calculate envelop
        m = envAccum = 0;
        for (i = 0, idx = 0; i < 462; i++) {
            for (n = 0, j = 0; j < 128; j++, idx += 2) n += sample[idx];
            m += n;
            envAccum *= 0.875;
            envAccum += m * m;
            _envelop[i] = envAccum;
            m = n;
        }

        // calculate cross correlation and find peak index
        for (i = 0, idx = 0; i < 113; i++) {
            for (n = 0, j = 0, k = 113 + i; j < 226; j++, k++) n += _envelop[j] * _envelop[k];
            _xcorr[i] = n;
            if (_xcorr[idx] < n) idx = i;
        }

        // caluclate bpm 2.9024943310657596 = 128/44.1
        return (113 + idx) * 2.9024943310657596;
    }

    private double[] _envelop = null;
    private double[] _xcorr = null;

    // wave table
    //

    /**
     * create Wave table Vector from wave color.
     *
     * @param color    wave color value
     * @param waveType wave type (the voice number of '%5')
     * @param dst      returning Vector.&lt;Number&gt;. if null, allocate new Vector inside.
     */
    public static double[] waveColor(int color, int waveType /* = 0 */, double[] dst /* = null */) {
        if (dst == null) dst = new double[SiOPMTable.SAMPLING_TABLE_SIZE];
        int len, bits = 0;
        for (len = dst.length >> 1; len != 0; len >>= 1) bits++;
        dst = new double[1 << bits];
        bits = SiOPMTable.PHASE_BITS - bits;

        int i, imax, j, gain, mul;
        double n, nmax;
        double[] bars = new double[7];
        int [] barr = new int[] {1, 2, 3, 4, 5, 6, 8};
        int[] log = SiOPMTable.getInstance().logTable;
        SiOPMWaveTable waveTable = SiOPMTable.getInstance().getWaveTable(waveType + (color >>> 28));
        int[] wavelet = waveTable.wavelet;
        int fixedBits = waveTable.fixedBits,
                filter = SiOPMTable.PHASE_FILTER, envtop = (-SiOPMTable.ENV_TOP) << 3,
                index, step = SiOPMTable.PHASE_MAX >> bits;

        for (i = 0; i < 7; i++, color >>= 4) bars[i] = (color & 15) * 0.0625;

        imax = SiOPMTable.PHASE_MAX;
        nmax = 0;

        for (i = 0; i < imax; i += step) {
            j = i >> bits;
            dst[j] = 0;
            for (mul = 0; mul < 7; mul++) {
                index = (((i * barr[mul]) & filter) >> fixedBits);
                gain = wavelet[index] + envtop;
                dst[j] += log[gain] * bars[mul];
            }
            n = (dst[j] < 0) ? -dst[j] : dst[j];
            if (nmax < n) nmax = n;
        }

        if (nmax < 8192) nmax = 8192;
        n = 1 / nmax;
        imax = dst.length;
        for (i = 0; i < imax; i++) dst[i] *= n;
        return dst;
    }
}
