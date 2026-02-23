package org.si.sound.mdx;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.SiONDriver;
import org.si.sion.sequencer.SiMMLSequencer;
import org.si.sion.sequencer.base.MMLEvent;
import org.si.sion.sequencer.base.MMLSequence;


/** @private [internal] */
class MDXExecutor {

    MDXTrack mdxtrack;
    MMLSequence mmlseq;
    int clock;
    int pointer;
    int pointerMax;

    int noiseVoiceNumber;
    MMLEvent lastRestMML;
    MMLEvent lastNoteMML;
    int voiceID;
    int adpcmID;
    int anFreq;
    List<MMLEvent> repeatStac;
    int lfoDelay;
    int lfofq;
    int lfows;
    int mp;
    int ma;
    int gateTime;
    boolean waitSync;
    int volume;
    boolean fineVolumeFlag;
    boolean isPCM8;

    private final int[] _panTable = {4, 0, 8, 4};
    private final int[] _freqTable = {26, 31, 38, 43, 50};
    private final int[] _volTable = {85, 87, 90, 93, 95, 98, 101, 103, 106, 109, 111, 114, 117, 119, 122, 125};
    private final int[] _volTablePCM8 = {2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 64, 80};
    private int[] _tlTable;

    private int eventIDFadeOut;
    private int eventIDPan;
    private int eventIDExp;
    private int eventIDPShift;
    private int eventIDLFO;
    private int eventIDAMod;
    private int eventIDPMod;
    private int eventIDIndex;

    public MDXExecutor() {
        int i;
        if (_tlTable == null) {
            _tlTable = new int[128];
            for (i = 0; i < 128; i++) _tlTable[127 - i] = ((1 << (i >> 3)) * (8 + (i & 7))) >> 12;
            for (i = 0; i < 16; i++) _volTable[i] = _tlTable[127 - _volTable[i]];
        }
    }

    void initialize(MMLSequence mmlseq, MDXTrack mdxtrack, int noiseVoiceNumber, boolean isPCM8) {
        this.mmlseq = mmlseq;
        this.mdxtrack = mdxtrack;
        this.noiseVoiceNumber = noiseVoiceNumber;
        this.isPCM8 = isPCM8;
        clock = 0;
        pointer = 0;
        pointerMax = mdxtrack.sequence.size();

        lastRestMML = null;
        lastNoteMML = null;
        voiceID = 0;
        adpcmID = -1;
        anFreq = (mdxtrack.channelNumber < 8) ? -1 : 4;
        repeatStac = new ArrayList<>();
        lfoDelay = 0;
        lfofq = 0;
        lfows = 2;
        mp = ma = 0;
        gateTime = 0;
        waitSync = false;
        volume = 8;
        fineVolumeFlag = false;

        if (mmlseq != null) {
            SiMMLSequencer sequencer = SiONDriver.mutex().sequencer;
            eventIDFadeOut = sequencer.getEventID("@fadeout");
            eventIDExp = sequencer.getEventID("x");
            eventIDPan = sequencer.getEventID("p");
            eventIDPShift = sequencer.getEventID("k");
            eventIDLFO = sequencer.getEventID("@lfo");
            eventIDAMod = sequencer.getEventID("ma");
            eventIDPMod = sequencer.getEventID("mp");
            eventIDIndex = sequencer.getEventID("i");

            if (mdxtrack.channelNumber < 8) {
                mmlseq.appendNewEvent(MMLEvent.MOD_TYPE, 6, 0); // use FM voice
                mmlseq.appendNewEvent(MMLEvent.QUANT_RATIO, 8, 0);
            } else {
                mmlseq.appendNewEvent(MMLEvent.MOD_TYPE, 7, 0); // use PCM voice
                mmlseq.appendNewEvent(MMLEvent.QUANT_RATIO, 8, 0);
                mmlseq.appendNewEvent(eventIDPShift, 40, 0);
            }
        }
    }

    // return next events clock
    int exec(int totalClock, double bpm) {
        if (mmlseq == null) return Integer.MAX_VALUE;

        MDXEvent e = null;
        MMLEvent me;
        int v, l;

        while (clock <= totalClock && pointer < pointerMax && !waitSync) {
            e = mdxtrack.sequence.get(pointer);
            if (mdxtrack.segnoPointer == e) mmlseq.appendNewEvent(MMLEvent.REPEAT_ALL, 0, 0);

            if (e.type < 0x80) {
                lastNoteMML = null;
                if (lastRestMML != null) lastRestMML.length += e.deltaClock * 10;
                else lastRestMML = mmlseq.appendNewEvent(MMLEvent.REST, 0, e.deltaClock * 10);
            } else if (e.type < 0xe0) {
                lastRestMML = null;
                if (mdxtrack.channelNumber < 8 && anFreq == -1) { // FM
                    if (lastNoteMML != null && lastNoteMML.data == e.data + 15) lastNoteMML.length = e.deltaClock * 10;
                    else mmlseq.appendNewEvent(MMLEvent.NOTE, e.data + 15, e.deltaClock * 10);
                } else if (mdxtrack.channelNumber == 7) { // FM/Noise
                    mmlseq.appendNewEvent(MMLEvent.NOTE, anFreq, e.deltaClock * 10);
                } else { // ADPCM
                    if (adpcmID != e.data) {
                        adpcmID = e.data;
                        mmlseq.appendNewEvent(MMLEvent.MOD_PARAM, adpcmID, 0);
                    }
                    mmlseq.appendNewEvent(MMLEvent.NOTE, _freqTable[anFreq], e.deltaClock * 10);
                }
                lastNoteMML = null;
            } else {
                lastNoteMML = lastRestMML = null;
                switch (e.type) {
                    case MDXEvent.PORTAMENT: // ...?
                        if (mdxtrack.sequence.get(pointer + 1).type == MDXEvent.SLUR) pointer++;
                        if (mdxtrack.sequence.get(pointer + 1).type != MDXEvent.NOTE) break;
                        v = e.data;
                        pointer++;
                        e = mdxtrack.sequence.get(pointer);
                        mmlseq.appendNewEvent(MMLEvent.NOTE, e.data + 15, 0);
                        mmlseq.appendNewEvent(MMLEvent.PITCHBEND, 0, e.deltaClock * 10);
                        lastNoteMML = mmlseq.appendNewEvent(MMLEvent.NOTE, e.data + 15 + (v * e.deltaClock + 8192) / 16384, 0);
                        break;
                    case MDXEvent.REGISTER:
                        mmlseq.appendNewEvent(MMLEvent.REGISTER, e.data, 0);
                        mmlseq.appendNewEvent(MMLEvent.PARAMETER, e.data2, 0);
                        break;
                    case MDXEvent.FADEOUT: {
                        mmlseq.appendNewEvent(eventIDFadeOut, e.data2, 0);
                    }
                    break;
                    case MDXEvent.VOICE:
                        if (mdxtrack.channelNumber < 8) { // ...?
                            voiceID = e.data;
                            mmlseq.appendNewEvent(MMLEvent.MOD_PARAM, voiceID, 0);
                        }
                        break;
                    case MDXEvent.PAN:
                        if (e.data == 0) {
                            mmlseq.appendNewEvent(eventIDExp, 0, 0);
                        } else {
                            _vol();
                            mmlseq.appendNewEvent(eventIDPan, _panTable[e.data], 0);
                        }
                        break;
                    case MDXEvent.VOLUME:
                        if (e.data < 16) {
                            volume = e.data;
                            fineVolumeFlag = false;
                        } else {
                            volume = e.data & 127;
                            fineVolumeFlag = true;
                        }
                        _vol();
                        break;
                    case MDXEvent.VOLUME_DEC:
                        if (--volume == 0) volume = 0;
                        _vol();
                        break;
                    case MDXEvent.VOLUME_INC:
                        l = (fineVolumeFlag) ? 127 : 15;
                        if (++volume == l) volume = l;
                        _vol();
                        break;
                    case MDXEvent.GATE:
                        if (e.data < 9) {
                            gateTime = e.data;
                            mmlseq.appendNewEvent(MMLEvent.QUANT_RATIO, gateTime, 0);
                            mmlseq.appendNewEvent(MMLEvent.QUANT_COUNT, 0, 0);
                        } else {
                            gateTime = -(256 - e.data) * 10;
                            mmlseq.appendNewEvent(MMLEvent.QUANT_RATIO, 8, 0);
                            mmlseq.appendNewEvent(MMLEvent.QUANT_COUNT, -gateTime, 0);
                        }
                        break;
                    case MDXEvent.KEY_ON_DELAY: {
                        mmlseq.appendNewEvent(MMLEvent.KEY_ON_DELAY, e.data * 10, 0);
                    }
                    break;
                    case MDXEvent.SLUR:
                        if (mdxtrack.sequence.get(pointer + 1).type == MDXEvent.NOTE) {
                            pointer++;
                            e = mdxtrack.sequence.get(pointer);
                            mmlseq.appendNewEvent(MMLEvent.NOTE, e.data + 15, 0);
                            mmlseq.appendNewEvent(MMLEvent.SLUR, 0, e.deltaClock * 10);
                        }
                        break;
                    case MDXEvent.REPEAT_BEGIN:
                        repeatStac.add(0, mmlseq.appendNewEvent(MMLEvent.REPEAT_BEGIN, e.data, 0));
                        break;
                    case MDXEvent.REPEAT_BREAK:
                        me = mmlseq.appendNewEvent(MMLEvent.REPEAT_BREAK, 0, 0);
                        me.jump = repeatStac.get(0);
                        break;
                    case MDXEvent.REPEAT_END:
                        me = mmlseq.appendNewEvent(MMLEvent.REPEAT_END, 0, 0);
                        me.jump = repeatStac.remove(0);
                        me.jump.jump = me;
                        break;
                    case MDXEvent.DETUNE:
                        mmlseq.appendNewEvent(eventIDPShift, e.data, 0);
                        break;
                    case MDXEvent.LFO_DELAY:
                        lfoDelay = (int) (e.data * 75 / bpm);
                        if (mp > 0) _mod(eventIDPMod, mp, lfows, lfofq);
                        if (ma > 0) _mod(eventIDAMod, ma, lfows, lfofq);
                        break;
                    case MDXEvent.PITCH_LFO:
                        if ((e.data & 0x80) != 0) {
                            if ((e.data & 0xff) == 0x80) mmlseq.appendNewEvent(eventIDPMod, 0, 0);
                            else _mod(eventIDPMod, mp, lfows, lfofq);
                        } else {
                            l = e.data >> 8;
                            mp = ((e.data2 >> (((e.data & 4) == 0) ? 8 : 0)) * l) >> 1;
                            _mod(eventIDPMod, mp, e.data & 3, (int) (l * 75 / bpm * ((lfows != 0) ? 2 : 1)));
                        }
                        break;
                    case MDXEvent.VOLUME_LFO:
                        /* ... 
                        if ((e.data & 0x80) != 0) {
                            if ((e.data & 0xff) == 0x80) mmlseq.appendNewEvent(eventIDAMod, 0);
                            else _mod(eventIDAMod, ma, lfows, lfofq);
                        } else {
                            l = e.data>>8;
                            ma = (e.data2 * l) >> 1;
                            _mod(eventIDAMod, ma, e.data&3, l*75/bpm * ((lfows)?2:1));
                        }
                        */
                        break;
                    case MDXEvent.FREQUENCY:
                        if (mdxtrack.channelNumber == 7) {
                            if ((e.data & 128) != 0) {
                                if (noiseVoiceNumber != -1) {
                                    mmlseq.appendNewEvent(MMLEvent.MOD_PARAM, noiseVoiceNumber, 0);
                                    anFreq = e.data & 31;
//trace("noiz!!");
                                }
                            } else {
                                mmlseq.appendNewEvent(MMLEvent.MOD_PARAM, voiceID, 0);
                                anFreq = -1;
                            }
                        } else if (mdxtrack.channelNumber >= 8) {
                            anFreq = e.data;
                        }
                        break;
                    case MDXEvent.SYNC_WAIT:
//trace("wait", clock);
                        waitSync = true;
                        break;
                    case MDXEvent.SYNC_SEND:
//trace("send", clock);
                    case MDXEvent.TIMERB:
                    case MDXEvent.DATA_END:
                    case MDXEvent.SET_PCM8:
                        // do nothing
                        break;
                    case MDXEvent.OPM_LFO:
                    default:
                        // not supported
                        break;
                }
            }

            clock += e.deltaClock;
            pointer++;
        }

        return (pointer >= pointerMax || waitSync) ? Integer.MAX_VALUE : clock;
    }

    void _vol() {
        if (mdxtrack.channelNumber < 8)
            mmlseq.appendNewEvent(eventIDExp, (fineVolumeFlag) ? _tlTable[volume] : _volTable[volume], 0);
        else mmlseq.appendNewEvent(eventIDExp, (fineVolumeFlag) ? (127 - volume) : _volTable[volume], 0);
    }

    void _mod(int eventID, int data, int ws, int fq) {
        if (lfows != ws || lfofq != fq) {
            lfofq = fq;
            lfows = ws;
            mmlseq.appendNewEvent(eventIDLFO, lfofq, 0);
            mmlseq.appendNewEvent(MMLEvent.PARAMETER, lfows, 0);
        }
        if (lfoDelay > 0) {
            mmlseq.appendNewEvent(eventID, 0, 0);
            mmlseq.appendNewEvent(MMLEvent.PARAMETER, data, 0);
            mmlseq.appendNewEvent(MMLEvent.PARAMETER, lfoDelay, 0);
        } else {
            mmlseq.appendNewEvent(eventID, data, 0);
        }
    }

    void globalExec(int totalClock, MDXData data) {
        MDXEvent e;
        boolean syncWaitSync = waitSync;
        int syncClock = clock;
        int syncPointer = pointer;
        while (syncClock <= totalClock && syncPointer < pointerMax && !syncWaitSync) {
            e = mdxtrack.sequence.get(syncPointer);
            switch (e.type) {
                case MDXEvent.SYNC_SEND:
                    data.onSyncSend(e.data, syncClock);
                    break;
                case MDXEvent.TIMERB:
                    data.onTimerB(e.data, syncClock);
                    break;
                case MDXEvent.SYNC_WAIT:
                    syncWaitSync = true;
                    break;
            }
            syncClock += e.deltaClock;
            syncPointer++;
        }
    }

    void sync(int currentClock) {
//trace(currentClock, clock);
        if (currentClock > clock) {
            mmlseq.appendNewEvent(MMLEvent.REST, 0, (currentClock - clock) * 10);
            clock = currentClock;
        }
        waitSync = false;
    }
}
