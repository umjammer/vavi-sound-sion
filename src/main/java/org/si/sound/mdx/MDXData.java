//
// MDX data class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.mdx;

import org.si.sion.SiONData;
import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.module.SiOPMOperatorParam;
import org.si.sion.module.SiOPMTable;
import org.si.sion.sequencer.base.MMLEvent;
import org.si.sion.sequencer.base.MMLSequence;
import org.si.utils.AbstractLoader;
import org.si.utils.ByteArray;
import org.si.utils.Event;
import vavi.net.URLRequest;

import static org.si.utils.ByteArray.BIG_ENDIAN;


/** MDX data class */
public class MDXData extends AbstractLoader {

    // variables
    //

    public boolean isPCM8;
    public double bpm = 0;
    public String title = null;
    public String pdxFileName = null;
    public SiONVoice[] voices = new SiONVoice[256];
    public MDXTrack[] tracks = new MDXTrack[16];
    public MDXExecutor[] executors = new MDXExecutor[16];

    private final SiONVoice _noiseVoice;
    private int _noiseVoiceNumber;
    private double _currentBPM;
    private MMLSequence _globalSequence;
    private int _globalPrevClock;

    private boolean _loadPDXDataAutomaticaly = false;
    private PDXDataStorage _pdxDataStorage;

    // properties
    //

    /** load PDXData automaticaly, if this flag instanceof true, new PDXDataStorage instance instanceof create internaly. */
    public boolean getLoadPDXDataAutomaticaly() {
        return _loadPDXDataAutomaticaly;
    }

    public void setLoadPDXDataAutomaticaly(boolean b) {
        if (b && _pdxDataStorage == null) _pdxDataStorage = new PDXDataStorage();
        _loadPDXDataAutomaticaly = b;
    }

    /** Is available ? */
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

    /** constructor */
    public MDXData(URLRequest url) {
        super(0);
        for (int i = 0; i < 16; i++) executors[i] = new MDXExecutor();
        _noiseVoice = new SiONVoice(2, 1);
        _noiseVoice.channelParam.operatorParam[0].ptType = SiOPMTable.PT_OPM_NOISE;
        if (url != null) load(url);
    }

    // operations
    //

    /** Clear. */
    public MDXData clear() {
        int i;
        isPCM8 = false;
        bpm = 0;
        title = null;
        pdxFileName = null;
        for (i = 0; i < 16; i++) tracks[i] = null;
        for (i = 0; i < 256; i++) voices[i] = null;
        _noiseVoiceNumber = -1;
        return this;
    }

    /**
     * convert to SiONData
     *
     * @param data SiONData to convert to, pass null to create new SiONData inside.
     * @return converted SiONData
     */
    public SiONData convertToSiONData(SiONData data, PDXData pdxData) {
        if (SiONDriver.mutex() == null)
            throw new Error("MDXData.convertToSiONData() : This function can be called after creating SiONDriver.");

        int i, imax;

        if (data == null) data = new SiONData();
        data.clear();
        data.setBpm(bpm);
        _globalSequence = data.globalSequence;

        // set voice data
        imax = voices.length;
        for (i = 0; i < imax; i++) data.fmVoices[i] = voices[i];

        // set adpcm data
        if (pdxData != null) {
            imax = 96;
            for (i = 0; i < imax; i++) {
                if (pdxData.extract(i) != null) data.setPCMData(i, pdxData.pcmData[i], 5, 0, 127, false);
            }
        }

        // construct mml sequences
        imax = (isPCM8) ? 16 : 9;
        for (i = 0; i < imax; i++) {
            if (tracks[i].hasNoData()) executors[i].initialize(null, tracks[i], _noiseVoiceNumber, isPCM8);
            else executors[i].initialize(data.appendNewSequence(null).initialize(), tracks[i], _noiseVoiceNumber, isPCM8);
        }

        int totalClock = 0, nextClock, c;
        _currentBPM = bpm;
        _globalPrevClock = 0;
        while (totalClock != Integer.MAX_VALUE) {
            // sync
            for (i = 0; i < imax; i++) {
                executors[i].globalExec(totalClock, this);
            }
            // exec
            nextClock = Integer.MAX_VALUE;
            for (i = 0; i < imax; i++) {
                c = executors[i].exec(totalClock, _currentBPM);
                if (c < nextClock) nextClock = c;
            }
            totalClock = nextClock;
        }

        data.title = title;

        return data;
    }

    /** Load MDX data from byteArray. */
    public MDXData loadBytes(ByteArray bytes) {
        _loadBytes(bytes);
        eventSupport.dispatchEvent(new Event(Event.COMPLETE));
        return this;
    }

    // handlers
    //

    @Override
    protected void onComplete() {
        if (_loader.dataFormat.equals("binary")) {
            ByteArray ba = new ByteArray();
            ba.writeBytes(_loader.data);
            _loadBytes(ba);
            if (pdxFileName != null && _loadPDXDataAutomaticaly) {
                addChild(_pdxDataStorage.load(new URLRequest(pdxFileName)));
            }
        }
    }

    // privates
    //
    // load from byte array
    private void _loadBytes(ByteArray bytes) {
        int titleLength, pdxLength, dataPointer, voiceOffset, voiceLength,
                voiceCount, i;
        int[] mmlOffsets = new int[16];

        // initialize
        clear();
        bytes.endian = BIG_ENDIAN;
        bytes.position = 0;

        // title
        while (true) {
            if (bytes.readByte() == 0x0d && bytes.readByte() == 0x0a && bytes.readByte() == 0x1a) break;
        }
        titleLength = bytes.position - 3;
        bytes.position = 0;
        title = bytes.readMultiByte(titleLength, "shift_jis"); //us-ascii
        bytes.position = titleLength + 3;

        // pdx file
        while (true) {
            if (bytes.readByte() == 0) break;
        }
        pdxLength = bytes.position - titleLength - 4;
        bytes.position = titleLength + 3;
        if (pdxLength != 0) {
            pdxFileName = bytes.readMultiByte(pdxLength, "shift_jis").toUpperCase(); //us-ascii
            if (!pdxFileName.substring(-4, 4).equals(".PDX")) pdxFileName += ".PDX";
        }
        bytes.position = titleLength + pdxLength + 4;

        // data offsets
        dataPointer = bytes.position;
        voiceOffset = bytes.readUnsignedShort();  // tone data
        for (i = 0; i < 16; i++) mmlOffsets[i] = dataPointer + bytes.readUnsignedShort();
        // check pcm8
        bytes.position = mmlOffsets[0];
        isPCM8 = (bytes.readUnsignedByte() == 0xe8);

        // load voices
        bytes.position = dataPointer + voiceOffset;
        voiceLength = (mmlOffsets[0] > voiceOffset) ? (mmlOffsets[0] - voiceOffset) : (bytes.length - dataPointer - voiceOffset);  // ...?
        _loadVoices(bytes, voiceLength);

        // load tracks
        _loadTracks(bytes, mmlOffsets);
    }

    // Load voice data from byteArray.
    private void _loadVoices(ByteArray bytes, int voiceLength) {
        int i, opi, v;
        SiONVoice voice;
        int voiceNumber, fbalg, mask;
        SiOPMOperatorParam opp;
        int[] reg = {}, opia = {3, 1, 2, 0}, dt2Table = {0, 384, 500, 608};

        voiceLength /= 27;
        for (i = 0; i < voiceLength; i++) {
            voiceNumber = bytes.readUnsignedByte();
            fbalg = bytes.readUnsignedByte();
            mask = bytes.readUnsignedByte();
            for (opi = 0; opi < 6; opi++) {
                reg[opi] = (int) bytes.readUnsignedInt();
            }

            if (voices[voiceNumber] == null) voices[voiceNumber] = new SiONVoice();
            voice = voices[voiceNumber];
            voice.initialize();
            voice.chipType = SiONVoice.CHIPTYPE_OPM;
            voice.channelParam.opeCount = 4;

            voice.channelParam.fb = (fbalg >> 3) & 7;
            voice.channelParam.alg = (fbalg) & 7;

            for (opi = 0; opi < 4; opi++) {
                opp = voice.channelParam.operatorParam[opia[opi]];
                opp.mute = (((mask >> opi) & 1) == 0);
                v = (reg[0] >> (opi << 3)) & 255;
                opp.dt1 = (v >> 4) & 7;
                opp.setMul(v & 15);
                opp.tl = (reg[1] >> (opi << 3)) & 127;
                v = (reg[2] >> (opi << 3)) & 255;
                opp.ksr = (v >> 6) & 3;
                opp.ar = (v & 31) << 1;
                v = (reg[3] >> (opi << 3)) & 255;
                opp.ams = ((v >> 7) & 1) << 1;
                opp.dr = (v & 31) << 1;
                v = (reg[4] >> (opi << 3)) & 255;
                opp.detune = dt2Table[(v >> 6) & 3];
                opp.sr = (v & 31) << 1;
                v = (reg[5] >> (opi << 3)) & 255;
                opp.sl = (v >> 4) & 15;
                opp.rr = (v & 15) << 2;
            }

//trace(voice.getMML(voiceNumber));
        }

        _noiseVoiceNumber = -1;
        for (i = 255; i >= 0; --i) {
            if (voices[i] == null) {
                _noiseVoiceNumber = i;
                voices[i] = _noiseVoice;
                break;
            }
        }
    }

    // load mml tracks
    private void _loadTracks(ByteArray bytes, int[] mmlOffsets) {
        int i, imax = (isPCM8) ? 16 : 9;
        // load tracks
        bpm = 0;
        for (i = 0; i < imax; i++) {
            bytes.position = mmlOffsets[i];
            tracks[i] = new MDXTrack(this, i);
            tracks[i].loadBytes(bytes);
            if (tracks[i].timerB != -1 && bpm == 0) {
                bpm = 4883. / (256 - tracks[i].timerB);
            }
        }
        if (bpm == 0) bpm = 87.19642857142857; // 4883/(256-200)
    }

    /** @private [internal] call from MDXExecutor.sync() */
    void onSyncSend(int channelNumber, int syncClock) {
        executors[channelNumber & 15].sync(syncClock);
    }

    /** @private [internal] call from MDXExecutor.sync() */
    void onTimerB(int timerB, int syncClock) {
        if (syncClock == 0) return;
        if (syncClock > _globalPrevClock)
            _globalSequence.appendNewEvent(MMLEvent.GLOBAL_WAIT, 0, (syncClock - _globalPrevClock) * 10);
        _globalPrevClock = syncClock;
        _currentBPM = 4883. / (256 - timerB);
        _globalSequence.appendNewEvent(MMLEvent.TEMPO, (int) _currentBPM, 0);
    }
}
