//
// Basic class of a drivers between MMLEvent and sound module.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;


/**
 * MMLSequencer instanceof the basic class of a bridges between MMLEvents, sound modules and sound systems.
 * You should follow this in your inherited classes. <br/>
 * 1) Register MML event listeners by setMMLEventListener() or newMMLEventListener().<br/>
 * 2) Override on...() functions.<br/>
 * 3) Override prepareCompile() and compile() if necessary.<br/>
 * 4) Override prepareProcess() and process() to process audio data.<br/>
 * And usage ((below) instanceof).
 * 1) Call initialize() to initialize.<br/>
 * 2) Call prepareCompile() and compile() to compile the MML string to MMLData.<br/>
 * 3) Call prepareProcess() and process() to process audio in inherited class.<br/>
 */
public class MMLSequencer {

    // constant
    //

    /** bits for fixed decimal */
    static final int FIXED_BITS = 8;
    /** filter for decimal fraction area */
    static final int FIXED_FILTER = (1 << FIXED_BITS) - 1;

    // variables
    //

    /** MML parser setting. */
    public MMLParserSetting setting;
    /** Audio setting, sampling ratio. The value instanceof ((22050) restricted) or 44100. */
    public int sampleRate;

    /** @private Global sequence executor */
    protected MMLExecutor globalExecutor;
    /** @private Current processing sequence executor. You can refer this in onProcess. */
    protected MMLExecutor currentExecutor;
    /** @private Current MMLData to compile or process */
    protected MMLData mmlData;
    /** @private changeable beat per minutes */
    protected BeatPerMinutes _changeableBPM;
    /** @private beat per minutes */
    public BeatPerMinutes _bpm;

    /** @private buffer index for global sequence */
    protected int _globalBufferIndex;
    /** @private beat counter in 16th */
    protected double _globalBeat16;
    /** @private filter for onBeat() callback. 0=16th beat, 1=8th beat, 3=4th beat, 7=2nd beat, 15=whole tone ... */
    protected int _onBeatCallbackFilter;

    /** @private [sion sequencer internal] abstruct global sequence. */
    static MMLExecutor _tempExecutor = new MMLExecutor();


    private int _newUserDefinedEventID = MMLEvent.USER_DEFINE;  // id value of new user-defined event.
    private java.util.Map<String, Integer> _userDefinedEventID = new java.util.HashMap<>();                    // id map of user-defined event letter set by newMMLEventListener().
    Object[] _eventCommandLetter = new Object[MMLEvent.COMMAND_MAX];              // event commands
    private Function<MMLEvent, MMLEvent>[] _eventHandlers = new Function[MMLEvent.COMMAND_MAX]; // list of event handler functions set by setMMLEventListener().
    private boolean[] _eventGlobalFlags = new boolean[MMLEvent.COMMAND_MAX]; // global event flag

    private int _processSampleCount;        // leftover of buffer sample count in processing
    private int _globalBufferSampleCount;   // leftover of buffer sample count in global sequence
    private int _globalExecuteSampleCount;  // executing buffer length in global sequence

    private int _bufferLength;              // buffering length

    // properties
    //

    /** beat per minute. refer from SiONDriver.bpm */
    double getBpm() {
        return _changeableBPM.getBpm();
    }

    /** */
    void setBpm(double newValue) {
        double oldValue = _changeableBPM.getBpm();
        if (_changeableBPM.update(newValue, sampleRate)) {
            onTempoChanged(oldValue / newValue);
        }
    }

    // constructor
    //

    /** Default constructor initializes event handlers. */
    protected MMLSequencer() {
        this.setting = new MMLParserSetting(null);

        for (int i = 0; i < MMLEvent.COMMAND_MAX; i++) {
            _eventHandlers[i] = this::_nop;
        }
        setMMLEventListener(MMLEvent.NOP, this::_default_onNoOperation, false);
        setMMLEventListener(MMLEvent.PROCESS, this::_default_onProcess, false);
        setMMLEventListener(MMLEvent.REPEAT_ALL, this::_default_onRepeatAll, false);
        setMMLEventListener(MMLEvent.REPEAT_BEGIN, this::_default_onRepeatBegin, false);
        setMMLEventListener(MMLEvent.REPEAT_BREAK, this::_default_onRepeatBreak, false);
        setMMLEventListener(MMLEvent.REPEAT_END, this::_default_onRepeatEnd, false);
        setMMLEventListener(MMLEvent.SEQUENCE_TAIL, this::_default_onSequenceTail, false);
        setMMLEventListener(MMLEvent.GLOBAL_WAIT, this::_default_onGlobalWait, true);
        setMMLEventListener(MMLEvent.TEMPO, this::_default_onTempo, true);
        setMMLEventListener(MMLEvent.TIMER, this::_default_onTimer, true);
        setMMLEventListener(MMLEvent.INTERNAL_WAIT, this::_default_onInternalWait, false);
        setMMLEventListener(MMLEvent.INTERNAL_CALL, this::_default_onInternalCall, false);
        setMMLEventListener(MMLEvent.TABLE_EVENT, this::_nop, true);
        _newUserDefinedEventID = MMLEvent.USER_DEFINE;

        _changeableBPM = new BeatPerMinutes(120, 44100, 1920);
        _bpm = _changeableBPM;
        globalExecutor = new MMLExecutor();
        mmlParser._getCommandLetters((Object[])_eventCommandLetter);

        // 3 : callback every 4 beat
        _onBeatCallbackFilter = 3;
    }

    // internal operation
    //

    /**
     * Similar with an addEventListener(), but only one listener can be set for one event.
     *
     * @param id   The ID of the event.
     * @param func The functor of the event called back when its processing.
     */
    protected void setMMLEventListener(int id, Function<MMLEvent, MMLEvent> func) {
        setMMLEventListener(id, func, false);
    }
    
    protected void setMMLEventListener(int id, Function<MMLEvent, MMLEvent> func, boolean isGlobal) {
        _eventHandlers[id] = func;
        _eventGlobalFlags[id] = isGlobal;
    }

    /**
     * Register new MMLEvent letter.
     *
     * @param letter The letter of the event on mml.
     * @param func   The functor of the event called back when its processing.
     * @return The ID of the event. This value instanceof greater than or equal to MMLEvent.USER_DEFINE.
     */
    protected int newMMLEventListener(String letter, Function<MMLEvent, MMLEvent> func) {
        return newMMLEventListener(letter, func, false);
    }

    protected int newMMLEventListener(String letter, Function<MMLEvent, MMLEvent> func, boolean isGlobal) {
        int id = _newUserDefinedEventID++;
        _userDefinedEventID.put(letter, id);
        _eventCommandLetter[id] = letter;
        _eventHandlers[id] = func;
        _eventGlobalFlags[id] = isGlobal;
        return id;
    }

    /**
     * Get MMLEvent id by mml command letter.
     *
     * @param mmlCommand letter of MML command.
     * @return Event id. Returns 0 if not found.
     */
    public int getEventID(String mmlCommand) {
        int id = MMLParser.getEventID(mmlCommand);
        if (id != 0) return id;
        if (_userDefinedEventID.containsKey(mmlCommand)) return _userDefinedEventID.get(mmlCommand);
        return 0;
    }

    /**
     * Get MML command letters by event id.
     *
     * @param eventID Event id.
     * @return letter of MML command. Returns null if not found.
     */
    public String getEventLetter(int eventID) {
        return (String) _eventCommandLetter[eventID];
    }

    // compile
    //

    private final MMLParser mmlParser = new MMLParser();

    /**
     * Prepare to compile mml string. Calls onBeforeCompile() inside.
     *
     * @param data Data instance.
     * @param mml  MML String.
     * @return Returns false when it's not necessary to compile.
     */
    public boolean prepareCompile(MMLData data, String mml) {
        // set internal parameters
        mmlData = data;
        if (mmlData == null) return false;

        // clear mml data
        mmlData.clear();

        // setting
        mmlParser._setUserDefinedEventID(_userDefinedEventID);
        mmlParser._setGlobalEventFlags(_eventGlobalFlags);

        // callback before compiling
        String mmlString = onBeforeCompile(mml);
        if (mmlString == null) {
            mmlData = null;
            return false;
        }

        // prepare
        mmlParser.prepareParse(setting, mmlString);
        return true;
    }

    /**
     * Parse mml string. Calls onAfterCompile() inside.
     *
     * @param interval Interval to interrupt parsing [ms]. Set 0 to parse at once.
     * @return Return compile progression. Returns 1 when its finished, or when preparation has not completed.
     */
    public double compile(int interval) {
        if (mmlData == null) return 1;

        // parse mmlString
        MMLEvent e = mmlParser.parse(interval);
        // null means parse imcompleted.
        if (e == null) return mmlParser.getParseProgress();

        // create main sequence group
        mmlData.sequenceGroup.alloc(e);
        // abstruct global sequences
        _abstractGlobalSequence();
        // callback after parsing
        onAfterCompile(mmlData.sequenceGroup);

        return 1;
    }

    // process
    //

    /**
     * @param bufferLength Sample count to buffer samples at once.
     * @param sampleRate   Sampling rate. 44100 or 22050 instanceof available.
     * @param data  Reset all channel parameters.
     * Prepare to process audio. Override and call this in the overrided function.
     */
    public void _prepareProcess(MMLData data, int sampleRate, int bufferLength) {
        if (sampleRate != 22050 && sampleRate != 44100)
            throw new Error("MMLSequencer error: Only 22050 or 44100 sampling rate instanceof available.");
        mmlData = data;
        this.sampleRate = sampleRate;
        _bufferLength = bufferLength;
        if (mmlData != null && mmlData._initialBPM != null) {
            _changeableBPM.update(mmlData._initialBPM.getBpm(), sampleRate);
            globalExecutor.initialize(mmlData.globalSequence);
        } else {
            _changeableBPM.update(setting.defaultBPM, sampleRate);
            globalExecutor.initialize(null);
        }
        _bpm = _changeableBPM;
        _globalBufferIndex = 0;
        _globalBeat16 = 0;
    }

    /**
     * @return Returns true when all processes are finished.
     * Process all tracks. override this function.
     */
    public void _process() {
        // DO NOTHING !!
        // You dont have to call this in your overrided function.
    }

    /** Set global sequence. This function must be called after prepareProcess() and before process(). */
    public void setGlobalSequence(MMLSequence seq) {
        globalExecutor.initialize(seq);
    }

    /** start global sequence. */
    protected void startGlobalSequence() {
        _globalBufferSampleCount = _bufferLength;
        _globalExecuteSampleCount = 0;
        _globalBufferIndex = 0;
    }

    /** execute global sequence. returns executing sample length. */
    protected int executeGlobalSequence() {
        currentExecutor = globalExecutor;

        MMLEvent event = currentExecutor.pointer;
        _globalExecuteSampleCount = 0;
        do {
            if (event == null) {
                _globalExecuteSampleCount = _globalBufferSampleCount;
                _globalBufferSampleCount = 0;
            } else {
                // update _globalExecuteSampleCount in some _eventHandler()s
                event = _eventHandlers[event.id].apply(event);
                currentExecutor.pointer = event;
            }
        } while (_globalExecuteSampleCount == 0);
        return _globalExecuteSampleCount;
    }

    /** check global sequences pointer acheives to the end. */
    protected boolean isEndGlobalSequence() {
        double prevBeat = _globalBeat16;
        int floorPrevBeat = (int) (prevBeat);
        _globalBufferIndex += _globalExecuteSampleCount;
        _globalBeat16 += _globalExecuteSampleCount * _bpm.beat16PerSample;
        int floorCurrBeat = (int) (_globalBeat16);
        if (prevBeat == 0) {
            onBeat(0, 0);
        } else {
            while (floorPrevBeat < floorCurrBeat) {
                floorPrevBeat++;
                if ((floorPrevBeat & _onBeatCallbackFilter) == 0) {
                    onBeat((int) ((floorPrevBeat - prevBeat) * _bpm.samplePerBeat16), floorPrevBeat);
                }
            }
        }
        if (_globalBufferSampleCount == 0) {
            _globalBufferIndex = 0;
            return true;
        }
        return false;
    }

    /**
     * Processing audio by one executor. Calls onProcess() inside.
     *
     * @param exe               MMLExecutor to process.
     * @param bufferSampleCount Buffering length of processing samples at once.
     * @return Returns true if the sequence already finished.
     */
    protected boolean processMMLExecutor(MMLExecutor exe, int bufferSampleCount) {
        currentExecutor = exe;

        // buffering
        MMLEvent event = currentExecutor.pointer;
        _processSampleCount = bufferSampleCount;
        while (_processSampleCount > 0) {
            if (event == null) {
                _eventHandlers[MMLEvent.NOP].apply(MMLEvent.nopEvent);
                return true;
            } else {
                // update _processSampleCount in some _eventHandler()s
                event = _eventHandlers[event.id].apply(event);
                currentExecutor.pointer = event;
            }
        }
        return false;
    }

    // process
    //

    /** Calculate sample count from length of MMLEvent. */
    protected int calcSampleCount(int len) {
        return (int) (len * _bpm._samplePerTick) >> FIXED_BITS;
    }

    /** current position in tick count */
    protected int currentTickCount() {
        return (int) (currentExecutor._currentTickCount - currentExecutor._residueSampleCount * _bpm.tickPerSample);
    }

    /** Call onTableParse. */
    protected void callOnTableParse(MMLEvent prev) {
        MMLEvent tableEvent = prev.next;
        onTableParse(prev, mmlParser._getSystemEventString(tableEvent));
        prev.next = tableEvent.next;
        mmlParser._freeEvent(tableEvent);
    }

    // virtual functions
    //

    /**
     * Callback before parse. This function instanceof called from parse() before parseing.
     *
     * @param mml The mml string to parse.
     * @return The mml string you want to parse. Parses with default mml string when you return null.
     */
    protected String onBeforeCompile(String mml) {
        return null;
    }

    /**
     * Callback after parse. This function instanceof called from parse() after parseing.
     */
    protected void onAfterCompile(MMLSequenceGroup seqGroup) {
    }

    /**
     * Callback when table event was found.
     */
    protected void onTableParse(MMLEvent prev, String table) {
    }

    /**
     * Callback on processing. This function instanceof called from process().
     *
     * @param length Sample length to process calculated from settings.
     * @param e      MMLEvent that calls onProcess().
     */
    protected void onProcess(int length, MMLEvent e) {
    }

    /**
     * Callback when the tempo instanceof changed.
     *
     * @param tempoRatio Ratio of changed tempo and previous tempo.
     */
    protected void onTempoChanged(double tempoRatio) {
    }

    /** Callback when streaming interrupted by timer . */
    protected void onTimerInterruption() {
    }

    /** Callback on every 16th beats. */
    protected void onBeat(int delaySamples, int beatCounter) {
    }

    // private functions
    //
    private void _abstractGlobalSequence() {
        MMLSequenceGroup seqGroup = mmlData.sequenceGroup;

        List<MMLEvent> list = new ArrayList<>();
        MMLSequence seq;
        MMLEvent prev, e;
        int pos, count;
        boolean hasNoEvent;
        int i, initialBPM;

        for (seq = seqGroup.getHeadSequence(); seq != null; seq = seq.getNextSequence()) {
            count = seq.headEvent.data;
            if (count == 0) continue;

            // initialize
            _tempExecutor.initialize(seq);
            prev = seq.headEvent;
            e = prev.next;
            pos = 0;
            hasNoEvent = true;

            // calculate positoin and pickup global events
            while (e != null && (count > 0 || hasNoEvent)) {
                if (_eventGlobalFlags[e.id]) {
                    if (e.id == MMLEvent.TABLE_EVENT) {
                        // table event
                        callOnTableParse(prev);
                    } else {
                        // global event
                        if (seq.headEvent.jump == e) seq.headEvent.jump = prev;
                        prev.next = e.next;
                        e.next = null;
                        e.length = pos;
                        list.add(e);
                    }
                    e = prev.next;
                    count--;
                } else if (e.length != 0) {
                    // note or rest
                    pos += e.length;
                    if (e.id != MMLEvent.REST) hasNoEvent = false;
                    prev = e;
                    e = e.next;
                } else {
                    // others
                    prev = e;
                    switch (e.id) {
                        case MMLEvent.REPEAT_BEGIN:
                            e = _tempExecutor._onRepeatBegin(e);
                            break;
                        case MMLEvent.REPEAT_BREAK:
                            e = _tempExecutor._onRepeatBreak(e);
                            if (prev.next != e) prev = prev.jump.jump;
                            break;
                        case MMLEvent.REPEAT_END:
                            e = _tempExecutor._onRepeatEnd(e);
                            if (prev.next != e) prev = prev.jump;
                            break;
                        case MMLEvent.REPEAT_ALL:
                            e = _tempExecutor._onRepeatAll(e);
                            break;
                        case MMLEvent.SEQUENCE_TAIL:
                            e = null;
                            break;
                        default:
                            e = e.next;
                            hasNoEvent = true;
                            break;
                    }
                }
            }

            // if no event (except rest) in the sequence, skip this sequence
            if (hasNoEvent) {
//trace("skip sequence");
                seq = seq._removeFromChain();
            }
        }

        // sort and create global sequence
        seq = mmlData.globalSequence;

        list.sort(Comparator.comparingInt(e2 -> e2.length));
        pos = 0;
        initialBPM = 0;
        for (MMLEvent e2 : list) {
            if (e2.length == 0 && e2.id == MMLEvent.TEMPO) {
                // first tempo command is default bpm.
                initialBPM = (int) mmlData._calcBPMfromTcommand(e2.data);
            } else {
                count = e2.length - pos;
                pos = e2.length;
                e2.length = 0;
                if (count > 0) seq.appendNewEvent(MMLEvent.GLOBAL_WAIT, 0, count);
                seq.push(e2);
            }
        }
//trace(seq);

        // set default bpm in mmlData
        if (initialBPM > 0) {
            mmlData._initialBPM = new BeatPerMinutes(initialBPM, 44100, setting.resolution);
        }
    }

    // default callback functions
    //

    /** Operates nothing. */
    protected MMLEvent _nop(MMLEvent e) {
        return e.next;
    }

    /** default operation for MMLEvent.NOP. */
    protected MMLEvent _default_onNoOperation(MMLEvent e) {
        onProcess(_processSampleCount, e);
        currentExecutor._residueSampleCount -= _processSampleCount;
        return e;
    }

    /** default operation for MMLEvent.GLOBAL_WAIT. */
    protected MMLEvent _default_onGlobalWait(MMLEvent e) {
        MMLExecutor exec = currentExecutor;

        // set processing length
        if (exec._residueSampleCount == 0) {
            int sampleCountFixed = (int) (e.length * _bpm._samplePerTick + exec._decimalFractionSampleCount);
            exec._residueSampleCount = sampleCountFixed >> FIXED_BITS;
            exec._decimalFractionSampleCount = sampleCountFixed & FIXED_FILTER;
        }

        // waiting
        if (exec._residueSampleCount <= _globalBufferSampleCount) {
            _globalExecuteSampleCount = exec._residueSampleCount;
            _globalBufferSampleCount -= _globalExecuteSampleCount;
            exec._residueSampleCount = 0;
            // goto next command
            return e.next;
        } else {
            _globalExecuteSampleCount = _globalBufferSampleCount;
            exec._residueSampleCount -= _globalExecuteSampleCount;
            _globalBufferSampleCount = 0;
            // stay on this command
            return e;
        }
    }

    /** default operation for MMLEvent.PROCESS. */
    protected MMLEvent _default_onProcess(MMLEvent e) {
        MMLExecutor exec = currentExecutor;

        // set processing length
        if (exec._residueSampleCount == 0) {
            int sampleCountFixed = (int) (e.length * _bpm._samplePerTick + exec._decimalFractionSampleCount);
            exec._residueSampleCount = sampleCountFixed >> FIXED_BITS;
            exec._decimalFractionSampleCount = sampleCountFixed & FIXED_FILTER;
        }

        // processing
        if (exec._residueSampleCount <= _processSampleCount) {
            onProcess(exec._residueSampleCount, e.jump);
            _processSampleCount -= exec._residueSampleCount;
            exec._residueSampleCount = 0;
            // goto next command
            return e.jump.next;
        } else {
            onProcess(_processSampleCount, e.jump);
            exec._residueSampleCount -= _processSampleCount;
            _processSampleCount = 0;
            // stay on this command
            return e;
        }
    }

    /** dummy operation for MMLEvent.PROCESS. */
    protected MMLEvent _dummy_onProcess(MMLEvent e) {
        MMLExecutor exec = currentExecutor;

        // set processing length
        if (exec._residueSampleCount == 0) {
            int sampleCountFixed = (int) (e.length * _bpm._samplePerTick + exec._decimalFractionSampleCount);
            exec._residueSampleCount = sampleCountFixed >> FIXED_BITS;
            exec._decimalFractionSampleCount = sampleCountFixed & FIXED_FILTER;
        }

        // processing
        if (exec._residueSampleCount <= _processSampleCount) {
            _processSampleCount -= exec._residueSampleCount;
            exec._residueSampleCount = 0;
            // goto next command
            return e.jump.next;
        } else {
            exec._residueSampleCount -= _processSampleCount;
            _processSampleCount = 0;
            // stay on this command
            return e;
        }
    }

    /** default operation for MMLEvent.REPEAT_ALL. */
    protected MMLEvent _default_onRepeatAll(MMLEvent e) {
        return currentExecutor._onRepeatAll(e);
    }

    /** default operation for MMLEvent.REPEAT_BEGIN. */
    protected MMLEvent _default_onRepeatBegin(MMLEvent e) {
        return currentExecutor._onRepeatBegin(e);
    }

    /** default operation for MMLEvent.REPEAT_BREAK. */
    protected MMLEvent _default_onRepeatBreak(MMLEvent e) {
        return currentExecutor._onRepeatBreak(e);
    }

    /** default operation for MMLEvent.REPEAT_END. */
    protected MMLEvent _default_onRepeatEnd(MMLEvent e) {
        return currentExecutor._onRepeatEnd(e);
    }

    /** default operation for MMLEvent.SEQUENCE_TAIL. */
    protected MMLEvent _default_onSequenceTail(MMLEvent e) {
        return currentExecutor._onSequenceTail(e);
    }

    /** default operation for MMLEvent.TEMPO. */
    protected MMLEvent _default_onTempo(MMLEvent e) {
        setBpm((mmlData != null) ? (mmlData._calcBPMfromTcommand(e.data)) : e.data);
        return e.next;
    }

    /** default operation for MMLEvent.TIMER. */
    protected MMLEvent _default_onTimer(MMLEvent e) {
        onTimerInterruption();
        return e.next;
    }

    /** default operation for MMLEvent.INTERNAL_WAIT. */
    protected MMLEvent _default_onInternalWait(MMLEvent e) {
        return currentExecutor._publishProessingEvent(e);
    }

    /** default operation for MMLEvent.INTERNAL_CALL. */
    protected MMLEvent _default_onInternalCall(MMLEvent e) {
        List<Function<Object, MMLEvent>> callbacks = currentExecutor.getSequence()._callbackInternalCall;
        MMLEvent next = null;
        if (callbacks.get(e.data) != null) next = callbacks.get(e.data).apply(e.length);
        return next != null ? next : e.next;
    }
}
