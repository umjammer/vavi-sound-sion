//
// PCM Sample loader/saver
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import org.si.utils.ByteArray;
import org.si.utils.ByteArrayExt;
import org.si.utils.ErrorEvent;
import org.si.utils.Event;
import org.si.utils.EventSupport;

import static org.si.utils.ByteArray.LITTLE_ENDIAN;


/** PCM sample loader/saver */
public class PCMSample {

    // variables
    //

    EventSupport eventSupport = new EventSupport();

    /** You should not change this property into "acid" ! */
    public static String basicInfoChunkID = "sinf";
    /** extended chunk for SiON */
    public String extendedInfoChunkID = "SiON";

    /** flags: 0x01 = oneshot, 0x02 = rootSet, 0x04 = stretch, 0x08 = diskbased */
    public int sampleType;    // int
    /** MIDI note number of base frequency */
    public int baseNote; // short
    /** beat count */
    public int beatCount;    // int
    /** denominator of time signature */
    public int timeSignatureDenominator;   // short
    /** number of time signature */
    public int timeSignatureNumber;   // short
    /** beat per minutes */
    public double bpm;       // float

    /** chunks of wave data */
    protected java.util.Map<String, Object> _waveDataChunks = null;
    /** wave data */
    protected ByteArrayExt _waveData = null;
    /** wave data format ID */
    protected int _waveDataFormatID;
    /** wave data sample rate */
    protected double _waveDataSampleRate;
    /** wave data bit rate */
    protected int _waveDataBitRate;
    /** wave data channel count */
    protected int _waveDataChannels;

    /** converted sample cache */
    protected double[] _cache;
    /** converted sample cache sample rate */
    protected double _cacheSampleRate;
    /** converted sample cache channel count */
    protected int _cacheChannels;
    /** sample rate of output */
    protected double _outputSampleRate;
    /** channel count of output */
    protected int _outputChannels;
    /** bit rate of wave file */
    protected int _outputBitRate;

    /** internal wave sample in 44.1kHz Number */
    protected double[] _samples;
    /** channel count of internal wave sample */
    protected int _channels;
    /** sample rate of internal wave sample */
    protected int _sampleRate;

    /** append position */
    protected int _appendPosition;
    /** extract position */
    protected double _extractPosition;

    // properties
    //

    /** samples in Vector.&lt;Number&gt; with properties of sampleRate and channels. */
    public double[] getSamples() {
        if (_outputSampleRate == _sampleRate && _outputChannels == _channels) {
//trace("get sample from raw sample");
            return _samples;
        }
        if (_outputSampleRate == _cacheSampleRate && _outputChannels == _cacheChannels) {
//trace("get sample from cache");
            return _cache;
        }
        _cacheChannels = _outputChannels;
        _cacheSampleRate = _outputSampleRate;
        _convertSampleRate(_samples, _channels, _sampleRate, _cache, _cacheChannels, _cacheSampleRate, true);
//trace("get sample with convert");
        return _cache;
    }

    /** sample length */
    public int getSampleLength() {
        int sampleLength = _samples.length >> (_channels - 1);
        return (int) (sampleLength * _outputSampleRate / _sampleRate);
    }

    /** sample rate [Hz] */
    public double getSampleRate() {
        return _outputSampleRate;
    }

    public void setSampleRate(double rate) {
        _outputSampleRate = (rate == 0) ? _sampleRate : rate;
    }

    /** channel count, 1 for monaural, 2 for stereo */
    public int getChannels() {
        return _outputChannels;
    }

    public void setChannels(int count) {
        if (count != 1 && count != 2) throw new Error("channel count of 1 or 2 instanceof only avairable.");
        _outputChannels = count;
    }

    /** bit rate, this function instanceof used only for saveWaveByteArray, 8 or 16 instanceof avairable. */
    public int getBitRate() {
        return _outputBitRate;
    }

    public void setBitRate(int rate) {
        if (rate != 8 && rate != 16 && rate != 24 && rate != 32)
            throw new Error("bitRate of " + rate + " is not avairable.");
        _outputBitRate = rate;
    }

    /** chunks of wave file, this property instanceof only available after loadWaveFromByteArray(). */
    public Object getWaveDataChunks() {
        return _waveDataChunks;
    }

    /** wave sample data of original wave file, this property instanceof only available after loadWaveFromByteArray() or saveWaveAsByteArray(). */
    public ByteArray getWaveData() {
        return _waveData;
    }

    /** sample rate of original wave file, this property instanceof only available after loadWaveFromByteArray() or saveWaveAsByteArray(). */
    public double getWaveDataSampleRate() {
        return _waveDataSampleRate;
    }

    /** bit rate of original wave file, this property instanceof only available after loadWaveFromByteArray() or saveWaveAsByteArray(). */
    public int getWaveDataBitRate() {
        return _waveDataBitRate;
    }

    /** channel count of original wave file, this property instanceof only available after loadWaveFromByteArray() or saveWaveAsByteArray(). */
    public int getWaveDataChannels() {
        return _waveDataChannels;
    }

    /** sample rate of internal samples. */
    public double getInternalSampleRate() {
        return _sampleRate;
    }

    /** channel count of internal samples. */
    public int getInternalChannels() {
        return _channels;
    }

    // constructor
    //

    /** constructor */
    public PCMSample(int channels /* = 2 */, int sampleRate /* = 44100 */, double[] samples /* = null */) {
        this._channels = channels;
        this._sampleRate = sampleRate;
        this._samples = samples != null ? samples : null;
        this._cache = null;
        this._cacheSampleRate = 0;
        this._cacheChannels = 0;
        this._outputSampleRate = _sampleRate;
        this._outputChannels = _channels;
        this._outputBitRate = 16;
        this._waveDataChunks = null;
        this._waveData = null;
        this._waveDataFormatID = 1;
        this._waveDataSampleRate = 0;
        this._waveDataBitRate = 0;
        this._waveDataChannels = 0;
        this._extractPosition = 0;
        this._appendPosition = this._samples.length;
        this.sampleType = 0;
        this.baseNote = 69;
        this.beatCount = 0;
        this.timeSignatureDenominator = 4;
        this.timeSignatureNumber = 4;
        this.bpm = 0;
    }

    /** */
    public String toString() {
        String str = "[object PCMSample : ";
        str += "channels=" + _channels;
        str += " / sampleRate=" + _sampleRate;
        str += " / sampleLength=" + getSampleLength();
        str += " / baseNote=" + baseNote;
        str += " / beatCount=" + beatCount;
        str += " / bpm=" + bpm;
        str += " / timeSignature=" + timeSignatureNumber + "/" + timeSignatureDenominator;
        str += "]";
        return str;
    }

    // operations
    //

    /**
     * load sample from Vector.&lt;Number&gt;
     *
     * @param src        source vector of Number.
     * @param srcChannels   channel count of source.
     * @param srcSampleRate sample rate of source.
     * @param linear     exchange sampling rate by linear interpolation, set false to use samples nearest by.
     */
    public PCMSample loadFromVector(double[] src, int srcChannels, double srcSampleRate, boolean linear) {
        _convertSampleRate(src, srcChannels, srcSampleRate, _samples, _channels, _sampleRate, linear);
        return this;
    }

    /**
     * append samples
     *
     * @param src         buffering source. This should be same ((internalSampleRate) format) and internalChannels
     * @param sampleCount sample count to append. 0 appends all samples.
     * @param srcOffset   position (in samples) start appending from.
     */
    public PCMSample appendSamples(double[] src, int sampleCount, int srcOffset) {
        clearCache();
        int i = srcOffset * _channels, len = sampleCount * _channels, ptr, ptrMax;
        if ((len == 0) || ((i + len) > src.length)) len = src.length - i;
        ptrMax = _appendPosition + len;
        if (_samples.length < ptrMax) _samples = new double[ptrMax];
        for (ptr = _appendPosition; ptr < ptrMax; ptr++, i++) _samples[ptr] = src[i];
        _appendPosition = ptrMax;
        return this;
    }

    /**
     * append samples from ByteArray float (2ch/44.1kHz), The internal format should be 2ch/44.1kHz.
     *
     * @param bytes       buffering source. The format should be float vector of 2ch/44.1kHz.
     * @param sampleCount sample count to append. 0 appends all samples.
     */
    public PCMSample appendSamplesFromByteArrayFloat(ByteArray bytes, int sampleCount) {
        if (_channels != 2 || _sampleRate != 44100) throw new Error("The internal format should be 2ch/44.1kHz.");
        clearCache();
        int len = (bytes.length - bytes.position) >> 3, ptr, ptrMax;
        if (sampleCount != 0 && len > sampleCount) len = sampleCount;
        ptrMax = _appendPosition + len * 2;
        if (_samples.length < ptrMax) _samples = new double[ptrMax];
        for (ptr = _appendPosition; ptr < ptrMax; ptr++) _samples[ptr] = bytes.readFloat();
        _appendPosition = ptrMax;
        return this;
    }

    /**
     * extract to Vector.&lt;Number&gt;
     *
     * @param dst
     * @param length
     * @param offset
     * @return
     */
    public double[] extract(double[] dst, int length, int offset) {
        if (offset == -1) offset = (int) _extractPosition;
        if (dst == null) dst = new double[0];
        if (length == 0) length = 999999;

        double[] output = this.getSamples();
        int i, imax = length * _outputChannels, j = offset * _outputChannels;
        if (imax + j > output.length) imax = output.length - j;
        for (i = 0; i < imax; i++, j++) dst[i] = output[j];

        _extractPosition = j >> (_outputChannels - 1);
        return dst;
    }

    /** clear cache and waveData */
    public PCMSample clearCache() {
//        _cache.length = 0;
        _cacheSampleRate = 0;
        _cacheChannels = 0;
        return this;
    }

    /** clear wave data cache */
    public PCMSample clearWaveDataCache() {
        _waveDataChunks = null;
        _waveData = null;
        _waveDataFormatID = 1;
        _waveDataSampleRate = 0;
        _waveDataBitRate = 0;
        _waveDataChannels = 0;
        return this;
    }

    // wave file operations
    //

    /**
     * load from wave file byteArray.
     *
     * @param waveFile ByteArray of wave file.
     */
    public PCMSample loadWaveFromByteArray(ByteArray waveFile) {
        ByteArrayExt bae = (waveFile instanceof ByteArrayExt) ? (ByteArrayExt)waveFile : new ByteArrayExt(waveFile);
        ByteArrayExt content = new ByteArrayExt(null);
        int fileSize;
        Map<String, Object> header;
        ByteArrayExt chunkBAE;
        int sliceCount, i, pos;
        bae.endian = LITTLE_ENDIAN;
        bae.position = 0;
        header = (Map<String, Object>) bae.readChunk(content, 0, null);
        String chunkID = header != null ? (String) header.get("chunkID") : null;
        String listType = header != null ? (String) header.get("listType") : null;
        if (!"RIFF".equals(chunkID) || !"WAVE".equals(listType)) eventSupport.dispatchEvent(new ErrorEvent("Not good wave file"));
        else {
            fileSize = (Integer) header.get("length");
            Map<String, Object> chunks = content.readAllChunks();
            _waveDataChunks = chunks;
            if (!(chunks.containsKey("fmt ") && chunks.containsKey("data")))
                eventSupport.dispatchEvent(new ErrorEvent("Not good wave file"));
            else {
                chunkBAE = (ByteArrayExt) chunks.get("fmt ");
                _waveDataFormatID = chunkBAE.readShort();
                _waveDataChannels = chunkBAE.readShort();
                _waveDataSampleRate = chunkBAE.readInt();
                chunkBAE.readInt();     // no ckeck for bytesPerSecond = _sampleRate*bytesPerSample
                chunkBAE.readShort();   // no ckeck for bytesPerSample = _bitRate*_channels/8
                _waveDataBitRate = chunkBAE.readShort();
                _waveData = (ByteArrayExt) chunks.get("data");

                if (chunks.containsKey(basicInfoChunkID)){
                    chunkBAE = (ByteArrayExt) chunks.get(basicInfoChunkID);
                    sampleType = chunkBAE.readInt();
                    baseNote = chunkBAE.readShort();
                    chunkBAE.readShort();   // _unknown1 = 0x8000
                    chunkBAE.readInt();     // _unknown2 = 0
                    beatCount = chunkBAE.readInt();
                    timeSignatureDenominator = chunkBAE.readShort();
                    timeSignatureNumber = chunkBAE.readShort();
                    bpm = chunkBAE.readFloat();
                }

                _updateSampleFromWaveData();
                eventSupport.dispatchEvent(new Event(Event.COMPLETE));
            }
        }
        return this;
    }

    /**
     * save wave ((byteArray) file).
     *
     * @return waveFile ByteArray of wave file.
     */
    public ByteArray saveWaveAsByteArray() {
        int bytesPerSample = (_outputBitRate * _outputChannels) >> 3;

        ByteArrayExt waveFile = new ByteArrayExt(null);
        ByteArrayExt content = new ByteArrayExt(null);
        ByteArray fmt = new ByteArray();

        // convert sampling rate, channels and bitrate
        if (_waveDataChannels != _outputChannels || _waveDataSampleRate != _outputSampleRate || _waveDataBitRate != _outputBitRate) {
            _updateWaveDataFromSamples();
        }

        // write wave file
        fmt.endian = LITTLE_ENDIAN;
        fmt.writeShort(1);
        fmt.writeShort(_outputChannels);
        fmt.writeInt((int) _outputSampleRate);
        fmt.writeInt((int) (_outputSampleRate * bytesPerSample));
        fmt.writeShort(bytesPerSample);
        fmt.writeShort(_outputBitRate);
        content.endian = LITTLE_ENDIAN;
        content.writeChunk("fmt ", fmt, null);
        content.writeChunk("data", _waveData, null);
        waveFile.endian = LITTLE_ENDIAN;
        waveFile.writeChunk("RIFF", content, "WAVE");
        return waveFile;
    }

    // utilities
    //

    /**
     * Try to read mysterious "strc" chunk.
     *
     * @param strcChunk strc chunk data.
     * @return positions
     */
    public int[] readSTRCChunk(ByteArray strcChunk) {
        if (strcChunk == null) return null;
        int i, imax;
        List<Integer> positions = new ArrayList<>();
        strcChunk.readInt(); // always 28
        imax = strcChunk.readInt();
        strcChunk.readInt(); // either 25 (0x19) or 65 (0x41)
        strcChunk.readInt(); // either 10 (0x0A) or 5 (0x05) linked to prev data ?
        strcChunk.readInt(); // always 1 (0x01)
        strcChunk.readInt(); // either 0, 1 or 10
        strcChunk.readInt(); // have seen values 2,3,4 and 5
        for (i = 0; i < imax; i++) {
            strcChunk.readInt(); // either 0 or 2
            strcChunk.readInt(); // random?
            positions.add(strcChunk.readInt());
            strcChunk.readInt(); // sample position of this slice
            strcChunk.readInt();
            strcChunk.readInt(); // sp2?
            strcChunk.readInt(); // data3
            strcChunk.readInt(); // random?
        }
        return IntStream.range(0, positions.size()).toArray();
    }

    // privates
    //

    // convert sampling rate and channel count
    private void _convertSampleRate(double[] src, int srcch, double srcsr, double[] dst, int dstch, double dstsr, boolean linear) {
        int flag, dstStep = (int) (srcsr / dstsr);
        if (dstStep == 1) linear = false;

        dst = new double[(int) (src.length * dstch * dstsr / (srcch * srcsr))];
//trace("convertSampleRate:", srcch, srcsr, src.length, dstch, dstsr, dst.length);

        flag = (srcch == 2) ? 1 : 0;
        flag |= (dstch == 2) ? 2 : 0;
        flag |= (linear) ? 4 : 0;
        _lvfunctions[flag].accept(src, dst, dstStep, 0);
    }

    @FunctionalInterface
    public interface QuadConsumer<T, U, V, W> {
        void accept(T var1, U var2, V var3, W var4);
    }

    @SuppressWarnings({"rowtypes", "unchecked"})
    private final QuadConsumer<double[], double[], Integer, Integer>[] _lvfunctions = List.of(
            this::_lvmmn,
            this::_lvsmn,
            this::_lvmsn,
            this::_lvssn,
            this::_lvmml,
            this::_lvsml,
            this::_lvmsl,
            (QuadConsumer<double[], double[], Integer, Integer>) this::_lvssl
    ).toArray(QuadConsumer[]::new);

    private void _lvmmn(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length, iptr;
        for (i = 0; i < imax; i++, ptr += step) {
            iptr = ptr;
            dst[i] = src[iptr];
        }
    }

    private void _lvmsn(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length, iptr;
        for (i = 0; i < imax; i++, ptr += step) {
            iptr = ptr;
            dst[i] = src[iptr];
            i++;
            dst[i] = src[iptr];
        }
    }

    private void _lvsmn(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length, iptr;
        double n;
        for (i = 0; i < imax; i++, ptr += step) {
            iptr = ptr * 2;
            n = src[iptr];
            iptr++;
            n += src[iptr];
            dst[i] = n * 0.5;
        }
    }

    private void _lvssn(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length, iptr;
        for (i = 0; i < imax; i++, ptr += step) {
            iptr = ptr * 2;
            dst[i] = src[iptr];
            iptr++;
            i++;
            dst[i] = src[iptr];
        }
    }

    private void _lvmml(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length - 1, istep = 1 / step,
                iptr0, iptr1 = ptr;
        double t;
        for (i = 0; i < imax; i++) {
            iptr0 = iptr1;
            t = ((double) ptr - iptr0) * istep;
            iptr1 = ptr += step;
            dst[i] = src[iptr0] * (1 - t) + src[iptr1] * t;
        }
        dst[imax] = src[iptr1];
    }

    private void _lvmsl(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length - 2, istep = 1 / step,
                iptr0, iptr1 = ptr;
        double t, n;
        for (i = 0; i < imax; i++) {
            iptr0 = iptr1;
            t = ((double) ptr - iptr0) * istep;
            iptr1 = ptr += step;
            n = src[iptr0] * (1 - t) + src[iptr1] * t;
            dst[i] = n;
            i++;
            dst[i] = n;
        }
        dst[imax] = src[iptr1];
        dst[imax + 1] = src[iptr1];
    }

    private void _lvsml(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length - 1;
        double istep = 0.5 / step;
        int iptr0, iptr1 = ptr;
        double t, n;
        int pl0, pl1 = 0;
        for (i = 0; i < imax; i++) {
            iptr0 = iptr1;
            t = ((double) ptr - iptr0) * istep;
            iptr1 = ptr += step;
            pl0 = iptr0 * 2;
            pl1 = iptr1 * 2;
            n = src[pl0] * (0.5 - t) + src[pl1] * t;
            pl0++;
            pl1++;
            n += src[pl0] * (0.5 - t) + src[pl1] * t;
            dst[i] = n;
        }
        dst[imax] = (src[pl1] + src[pl1 - 1]) * 0.5;
    }

    private void _lvssl(double[] src, double[] dst, int step, int ptr) {
        int i = 0, imax = dst.length - 2, istep = 1 / step,
                iptr0, iptr1 = ptr;
        double t, n;
        int pl0, pl1 = 0;
        for (i = 0; i < imax; i++) {
            iptr0 = iptr1;
            t = ((double) ptr - iptr0) * istep;
            iptr1 = ptr += step;
            pl0 = iptr0 * 2;
            pl1 = iptr1 * 2;
            dst[i] = src[pl0] * (1 - t) + src[pl1] * t;
            pl0++;
            pl1++;
            i++;
            dst[i] = src[pl0] * (1 - t) + src[pl1] * t;
        }
        dst[imax] = src[pl1 - 1];
        dst[imax + 1] = src[pl1];
    }

    // update samples from wave data
    private void _updateSampleFromWaveData() {
//trace("_updateSampleFromWaveData");
        int byteRate = _waveDataBitRate >> 3;
        if (_waveDataChannels == _channels && _waveDataSampleRate == _sampleRate) {
            _samples = new double[_waveData.length / byteRate];
            _w2vfunctions[byteRate - 1].accept (_waveData, _samples);
        } else {
            _cache = new double[_waveData.length / byteRate];
            _cacheChannels = _waveDataChannels;
            _cacheSampleRate = _waveDataSampleRate;
            _w2vfunctions[byteRate - 1].accept(_waveData, _cache);
            _convertSampleRate(_cache, _cacheChannels, _cacheSampleRate, _samples, _channels, _sampleRate, true);
            clearCache();
        }
    }

    // convert wave to vector
    @SuppressWarnings({"rowtypes", "unchecked"})
    private BiConsumer<ByteArray, double[]>[] _w2vfunctions = List.of(
            this::_w2v8,
            this::_w2v16,
            this::_w2v24,
            (BiConsumer<ByteArray, double[]>) this::_w2v32
    ).toArray(BiConsumer[]::new);

    private void _w2v8(ByteArray wav, double[] dst) {
        double unq = 1. / (1 << (_waveDataBitRate - 1)), imax = dst.length;
        for (int i = 0; i < imax; i++) dst[i] = (wav.readUnsignedByte() - 128) * unq;
    }

    private void _w2v16(ByteArray wav, double[] dst) {
        double unq = 1. / (1 << (_waveDataBitRate - 1)), imax = dst.length;
        for (int i = 0; i < imax; i++) dst[i] = wav.readShort() * unq;
    }

    private void _w2v24(ByteArray wav, double[] dst) {
        double unq = 1. / (1 << (_waveDataBitRate - 1)), imax = dst.length;
        for (int i = 0; i < imax; i++) dst[i] = (_waveData.readByte() + (_waveData.readShort() << 8)) * unq;
    }

    private void _w2v32(ByteArray wav, double[] dst) {
        double unq = 1. / (1 << (_waveDataBitRate - 1)), imax = dst.length;
        for (int i = 0; i < imax; i++) dst[i] = _waveData.readInt() * unq;
    }

    // convert raw data to samples
    private void _updateWaveDataFromSamples() {
//trace("_updateWaveDataFromSamples");
        int byteRate = _outputBitRate >> 3;
        var output = this.getSamples();
        _waveData = _waveData != null ? _waveData : new ByteArrayExt(null);
        _waveDataSampleRate = _outputSampleRate;
        _waveDataBitRate = _outputBitRate;
        _waveDataChannels = _outputChannels;

        // initialize
        _waveData.clear();
        _waveData.length = output.length * byteRate;
        _waveData.position = 0;

        // convert
        _v2wfunctions[byteRate - 1].accept(output, _waveData);
    }

    // convert vector tp wave
    private BiConsumer<double[], ByteArray>[] _v2wfunctions = List.of(
            this::_v2w8,
            this::_v2w16,
            this::_v2w24,
            (BiConsumer<double[], ByteArray>) this::_v2w32
    ).toArray(BiConsumer[]::new);

    private void _v2w8(double[] src, ByteArray wav) {
        double qn = (1 << (_waveDataBitRate - 1)) - 1, imax = src.length;
        for (int i = 0; i < imax; i++) wav.writeByte((int) (src[i] * qn + 128));
    }

    private void _v2w16(double[] src, ByteArray wav) {
        double qn = (1 << (_waveDataBitRate - 1)) - 1, imax = src.length;
        for (int i = 0; i < imax; i++) wav.writeShort((short) (src[i] * qn));
    }

    private void _v2w24(double[] src, ByteArray wav) {
        double n, qn = (1 << (_waveDataBitRate - 1)) - 1, imax = src.length;
        for (int i = 0; i < imax; i++) {
            n = src[i] * qn;
            wav.writeByte((int) n);
            wav.writeShort((int) n >> 8);
        }
    }

    private void _v2w32(double[] src, ByteArray wav) {
        double qn = (1 << (_waveDataBitRate - 1)) - 1, imax = src.length;
        for (int i = 0; i < imax; i++) wav.writeInt((int) (src[i] * qn));
    }
}
