//
// Track for SiMMLSequencer.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer;

import java.util.function.Function;

import org.si.sion.module.SiOPMTable;
import org.si.sion.module.channels.SiOPMChannelBase;
import org.si.sion.sequencer.base.BeatPerMinutes;
import org.si.sion.sequencer.base.MMLExecutor;
import org.si.sion.sequencer.base.MMLSequence;
import org.si.sion.sequencer.simulator.SiMMLSimulatorBase;
import org.si.utils.SLLint;


/**
 * Track instanceof a musical sequence player for one voice.
 */
public class SiMMLTrack {

    // constants
    //

    /** sweep step finess */
    private static final int SWEEP_FINESS = 128;
    /** Fixed decimal bits. */
    private static final int FIXED_BITS = 16;
    /** Maximum value of _sweep */
    private static final int SWEEP_MAX = 8192 << FIXED_BITS;

    // track id type
    /** track id filter */
    public final static int TRACK_ID_FILTER = 0xffff;
    /** track type filter */
    final static int TRACK_TYPE_FILTER = 0xff0000;

    /** Track Type ID for main MML tracks */
    public static final int MML_TRACK = 0x10000;
    /** Track Type ID for MIDI tracks */
    public static final int MIDI_TRACK = 0x20000;
    /** Track Type ID for tracks created by SiONDriver.noteOn() or SiONDriver.playSound() */
    public static final int DRIVER_NOTE = 0x30000;
    /** Track Type ID for tracks created by SiONDriver.sequenceOn() */
    public static final int DRIVER_SEQUENCE = 0x40000;
    /** Track Type ID for SiONDriver's background soundtracks */
    public static final int DRIVER_BACKGROUND = 0x50000;
    /** Track Type ID for user controlled tracks */
    public static final int USER_CONTROLLED = 0x60000;

    // mask bits for eventMask and @mask command
    /** no event mask */
    public static final int NO_MASK = 0;
    /** mask all volume commands (v,x,&#64;v,"(",")") */
    public static final int MASK_VOLUME = 1;
    /** mask all panning commands (p,&#64;p) */
    public static final int MASK_PAN = 2;
    /** mask all quantize commands (q,&#64;q) */
    public static final int MASK_QUANTIZE = 4;
    /** mask all operator setting commands (s,&#64;al,&#64;fb,i,&#64;,&#64;rr,&#64;tl,&#64;ml,&#64;st,&#64;ph,&#64;fx,&#64;se,&#64;er) */
    public static final int MASK_OPERATOR = 8;
    /** mask all table envelop commands (&#64;&#64;,na,np,nt,nf,_&#64;&#64;,_na,_np,_nt,_nf) */
    public static final int MASK_ENVELOP = 16;
    /** mask all modulation commands (ma,mp) */
    public static final int MASK_MODULATE = 32;
    /** mask all slur and pitch-bending commands (&amp;,&amp;&amp;,*) */
    public static final int MASK_SLUR = 64;

    // _processMode
    private static final int NORMAL = 0;
    private static final int ENVELOP = 2;

    // variables
    //

    /** Sound module's channel controlled by this track. */
    public SiOPMChannelBase channel;

    /** MML sequence executor */
    public MMLExecutor executor;

    /** note shift, set by kt command. */
    public int noteShift = 0;
    /** detune, set by k command. */
    public int pitchShift = 0;
    /** key on delay, set by 2nd argument of &#64;q command. */
    public int keyOnDelay = 0;
    /** quantize ratio, set by q command, the value instanceof between 0-1. */
    public double quantRatio = 0;
    /** quantize count, set by &#64;q command. */
    public int quantCount = 0;
    /** Event mask, set by &#64;mask command. */
    public int eventMask = 0;

    // call back function before noteOn/noteOff
    private Function<SiMMLTrack, Boolean> _callbackBeforeNoteOn = null;
    private Function<SiMMLTrack, Boolean> _callbackBeforeNoteOff = null;
    java.util.function.BiConsumer<Integer, Integer> _callbackUpdateRegister = this::_defaultUpdateRegister;

    // event trriger
    private Function<SiMMLTrack, Boolean> _eventTriggerOn = null;
    private Function<SiMMLTrack, Boolean> _eventTriggerOff = null;
    private int _eventTriggerID;
    private int _eventTriggerTypeOn;
    private int _eventTriggerTypeOff;

    // track ID number
    public int _internalTrackID;
    public int _trackNumber;

    // internal use
    private SiMMLData _mmlData;     // mml data. To get bpm from sequenceOn()s track, or only for reference in other cases.
    private final SiMMLTable _table;      // table
    private int _keyOnCounter;      // key on counter
    private int _keyOnLength;       // key on length
    private boolean _flagNoKeyOn;   // key on flag
    private int _processMode;       // processing mode
    private int _trackStartDelay;   // track delay to start
    private int _trackStopDelay;    // track delay to stop
    private boolean _stopWithReset; // stop with reset
    private boolean _isDisposable;  // flag disposable track
    private int _priority;          // track priority

    // settings
    private SiMMLChannelSetting _channelModuleSetting;  // selected module's setting
    private SiMMLSimulatorBase _simulator;              // simulator

    private int _velocityMode;   // velocity mode
    private int _expressionMode; // expression mode
    private int _velocity;       // velocity[0-256-512]
    private int _expression;     // expression[0-128]
    private int _pitchIndex;     // current pitch index
    private int _pitchBend;      // pitch bend
    private int _voiceIndex;     // tone number
    private int _note;           // note number
    private int _defaultFPS;     // default fps
    /** @private [internal] channel number. */
    public int _channelNumber;
    /** @private [internal] vcommand shift. */
    int _vcommandShift;  // vcommand shift

    // setting
    private final int[] _set_processMode;

    // envelop settings
    private final SLLint[] _set_env_exp;
    private final SLLint[] _set_env_voice;
    private final SLLint[] _set_env_note;
    private final SLLint[] _set_env_pitch;
    private final SLLint[] _set_env_filter;
    private final boolean[] _set_exp_offset;
    private final boolean[] _pns_or;

    private final int[] _set_cnt_exp;
    private final int[] _set_cnt_voice;
    private final int[] _set_cnt_note;
    private final int[] _set_cnt_pitch;
    private final int[] _set_cnt_filter;

    private final SLLint[] _table_env_ma;
    private final SLLint[] _table_env_mp;
    private final int[] _set_sweep_step;
    private final int[] _set_sweep_end;
    private int _env_internval;

    // executing envelop
    private SLLint _env_exp;
    private SLLint _env_voice;
    private SLLint _env_note;
    private SLLint _env_pitch;
    private SLLint _env_filter;

    private int _cnt_exp, _max_cnt_exp;
    private int _cnt_voice, _max_cnt_voice;
    private int _cnt_note, _max_cnt_note;
    private int _cnt_pitch, _max_cnt_pitch;
    private int _cnt_filter, _max_cnt_filter;

    private SLLint _env_mp;
    private SLLint _env_ma;
    private int _sweep_step;
    private int _sweep_end;
    private int _sweep_pitch;
    private int _env_exp_offset;
    private boolean _env_pitch_active;

    private int _residue;   // residue of previous envelop process

    // zero table
    private final SLLint _env_zero_table = SLLint.allocRing(1, 0);

    // properties
    //

    /** track number, this value instanceof unique and set by system, the lower numbered track processes sound first. */
    public int getTrackNumber() {
        return _trackNumber;
    }

    /** track ID, this value instanceof specifyed by user. */
    public int getTrackID() {
        return _internalTrackID & TRACK_ID_FILTER;
    }

    /** track type, this value shows what this track starts by. */
    public int getInternalTrackID() {
        return _internalTrackID;
    }
    public int getTrackTypeID() {
        return _internalTrackID & TRACK_TYPE_FILTER;
    }

    /** event trigger ID. eventTriggerID=-1 means tigger not set. */
    public int getEventTriggerID() {
        return _eventTriggerID;
    }

    /** Note on event trigger type. eventTriggerTypeOn=0 means tigger not set. */
    public int getEventTriggerTypeOn() {
        return _eventTriggerTypeOn;
    }

    /** Note off event trigger type. eventTriggerTypeOff=0 means tigger not set. */
    public int getEventTriggerTypeOff() {
        return _eventTriggerTypeOff;
    }

    /** Note number */
    public int getNote() {
        return _note;
    }

    /** Start delay in sample count. Ussualy this returns 0 except after SiONDriver.noteOn. */
    public int getTrackStartDelay() {
        return _trackStartDelay;
    }

    /** Stop delay in sample count. Ussualy this returns 0 except after SiONDriver.noteOff. */
    public int getTrackStopDelay() {
        return _trackStopDelay;
    }

    /** Is activate ? This function always returns true from not-disposable track. (isActive = !isDisposable || !isFinished) */
    public boolean isActive() {
        return !_isDisposable || executor.pointer != null || !channel.isIdling();
    }

    /** Is this track disposable ? Disposable track will free automatically when finished rendering. */
    public boolean getIsDisposable() {
        return _isDisposable;
    }

    /** Is playing sequence ? */
    public boolean isPlaySequence() {
        return ((_internalTrackID & TRACK_TYPE_FILTER) != DRIVER_NOTE && executor.pointer != null);
    }

    /** Is finish to rendering ? */
    public boolean isFinished() {
        return (executor.pointer == null && channel.isIdling());
    }

    /** velocity(0-256). linked to operator's total level. */
    public int getVelocity() {
        return _velocity;
    }

    public void setVelocity(int v) {
        _velocity = (v < 0) ? 0 : (v > 512) ? 512 : v;
        channel.offsetVolume(_expression, _velocity);
    }

    /** expression(0-128). linked to operator's total level. */
    public int getExpression() {
        return _expression;
    }

    public void setExpression(int x) {
        _expression = (x < 0) ? 0 : (x > 128) ? 128 : x;
        channel.offsetVolume(_expression, _velocity);
    }

    /** master volume(0-128). simple wrapper of channel.masterVolume. */
    public int getMasterVolume() {
        return channel.getMasterVolume();
    }

    public void setMasterVolume(int v) {
        channel.setMasterVolume(v);
    }

    /** effect send level for slot1 (0-128). simple wrapper of channel.setStreamSend. */
    public int getEffectSend1() {
        return (int) channel.getStreamSend(1);
    }

    public void setEffectSend1(int s) {
        channel.setStreamSend(1, (s < 0) ? 0 : (s > 128) ? 1 : s * 0.0078125);
    }

    /** effect send level for slot2 (0-128). simple wrapper of channel.setStreamSend. */
    public int getEffectSend2() {
        return (int) channel.getStreamSend(2);
    }

    public void setEffectSend2(int s) {
        channel.setStreamSend(2, (s < 0) ? 0 : (s > 128) ? 1 : s * 0.0078125);
    }

    /** effect send level for slot3 (0-128). simple wrapper of channel.setStreamSend. */
    public int getEffectSend3() {
        return (int) channel.getStreamSend(3);
    }

    public void setEffectSend3(int s) {
        channel.setStreamSend(3, (s < 0) ? 0 : (s > 128) ? 1 : s * 0.0078125);
    }

    /** effect send level for slot4 (0-128). simple wrapper of channel.setStreamSend. */
    public int getEffectSend4() {
        return (int) channel.getStreamSend(4);
    }

    public void setEffectSend4(int s) {
        channel.setStreamSend(4, (s < 0) ? 0 : (s > 128) ? 1 : s * 0.0078125);
    }

    /** mute */
    public boolean getMute() {
        return channel.getMute();
    }

    public void setMute(boolean b) {
        channel.setMute(b);
    }

    /** pannning (-64 - +64) */
    public int getPan() {
        return channel.getPan();
    }

    public void setPan(int p) {
        channel.setPan(p);
    }

    /** pitch bend */
    public int getPitchBend() {
        return _pitchBend;
    }

    public void setPitchBend(int p) {
        _pitchBend = p;
        channel.setPitch(_pitchIndex + _pitchBend);
    }

    /** callback function when update register event appears. */
    public java.util.function.BiConsumer<Integer, Integer> getOnUpdateRegister() {
        return _callbackUpdateRegister;
    }

    public void setOnUpdateRegister(java.util.function.BiConsumer<Integer, Integer> func) {
        _callbackUpdateRegister = (func != null) ? func : this::_defaultUpdateRegister;
    }

    /** velocity table mode */
    public int getVelocityMode() {
        return _velocityMode;
    }

    public void setVelocityMode(int mode) {
        int[][] tlTables = SiOPMTable.getInstance().eg_tlTables;
        _velocityMode = (mode >= 0 && mode < SiOPMTable.VM_MAX) ? mode : SiOPMTable.VM_LINEAR;
        channel.setVolumeTables(tlTables[_velocityMode], tlTables[_expressionMode]);
    }

    /** expression table mode */
    public int getExpressionMode() {
        return _expressionMode;
    }

    public void setExpressionMode(int mode) {
        int[][] tlTables = SiOPMTable.getInstance().eg_tlTables;
        _expressionMode = (mode >= 0 && mode < SiOPMTable.VM_MAX) ? mode : SiOPMTable.VM_LINEAR;
        channel.setVolumeTables(tlTables[_velocityMode], tlTables[_expressionMode]);
    }

    /** Channel number, set by 2nd argument of % command. Usually ((programNumber) same) (except for APU). @see programNumber */
    public int getChannelNumber() {
        return _channelNumber;
    }

    /** Program number, set by 2nd argument of % command and 1st arg. of &#64; command. Usually ((channelNumber) same) (except for APU). @see channelNumber */
    public int getProgramNumber() {
        return _voiceIndex;
    }

    /** output level = &#64;v * v * x. */
    public double getOutputLevel() {
        int vol = channel.getMasterVolume();
        if (vol == 0) return _velocity * _expression * 0.0000152587890625; // 0.5/(128*256);
        return vol * _velocity * _expression * 2.384185791015625e-7;       // 1/(128*128*256)
    }

    /** mml data to play. this value only instanceof available in the track playing mml sequence */
    public SiMMLData getMmlData() {
        return _mmlData;
    }

    /** @private [internal] bpm setting. refer from SiMMLSequencer */
    BeatPerMinutes getBpmSetting() {
        return ((_internalTrackID & TRACK_TYPE_FILTER) != MML_TRACK && _mmlData != null) ? _mmlData._initialBPM : null;
    }

    /** @private [internal] priority number to overwrite when tracks are overflow. */
    public int getPriority() {
        // not-disposable track or sequence playing track always returns highest priority
        if (!_isDisposable || isPlaySequence()) return 0;
        return _priority;
    }

    // constructor
    //
    public SiMMLTrack() {
        _table = SiMMLTable.getInstance();
        executor = new MMLExecutor();

        _mmlData = null;
        _set_processMode = new int[2];

        _set_env_exp = new SLLint[2];
        _set_env_voice = new SLLint[2];
        _set_env_note = new SLLint[2];
        _set_env_pitch = new SLLint[2];
        _set_env_filter = new SLLint[2];
        _pns_or = new boolean[2];
        _set_exp_offset = new boolean[2];
        _set_cnt_exp = new int[2];
        _set_cnt_voice = new int[2];
        _set_cnt_note = new int[2];
        _set_cnt_pitch = new int[2];
        _set_cnt_filter = new int[2];
        _set_sweep_step = new int[2];
        _set_sweep_end = new int[2];
        _table_env_ma = new SLLint[2];
        _table_env_mp = new SLLint[2];
    }

    // interfaces for intaractive operations
    //

    /**
     * Set track callback function. The callback functions are called at the timing of streaming before SiOPMEvent.STREAM event.
     *
     * @param noteOn  Callback function before note on. This function refers this track instance and new pitch (0-8191) as an arguments. When the function returns false, noteOn will be canceled.<br/>
     *                function callbackNoteOn(track:SiMMLTrack) : Boolean { return true; }
     * @param noteOff Callback function before note off. This function refers this track ((an) instance) argument. When the function returns false, noteOff will be canceled.<br/>
     *                function callbackNoteOff(track:SiMMLTrack) : Boolean { return true; }
     */
    public SiMMLTrack setTrackCallback(Function<SiMMLTrack, Boolean> noteOn, Function<SiMMLTrack, Boolean> noteOff) {
        _callbackBeforeNoteOn = noteOn;
        _callbackBeforeNoteOff = noteOff;
        return this;
    }

    /**
     * key on. SiONDriver.noteOn() calls this internally.
     *
     * @param note        Note number
     * @param tickLength  note length in tick count.
     * @param sampleDelay note delay in sample count.
     */
    public SiMMLTrack keyOn(int note, int tickLength /* = 0 */, int sampleDelay /* = 0 */) {
        _trackStartDelay = sampleDelay;
        executor.singleNote(note, tickLength);
        return this;
    }

    /**
     * Force key off. SiONDriver.noteOff() calls this internally.
     *
     * @param sampleDelay   Delay time (in sample count).
     * @param stopWithReset Stop with channel resetting.
     */
    public SiMMLTrack keyOff(int sampleDelay /* = 0 */, boolean stopWithReset /* = false */) {
        _stopWithReset = stopWithReset;
        if (sampleDelay != 0) {
            _trackStopDelay = sampleDelay;
        } else {
            _keyOff();
            _note = -1;
            if (_stopWithReset) channel.reset();
        }
        return this;
    }

    /**
     * dispatch EventTrigger
     *
     * @param noteOn noteOn event or noteOff event
     * @return returns false when the event instanceof prevented
     */
    public boolean dispatchEventTrigger(boolean noteOn) {
        if (noteOn) {
            if (_callbackBeforeNoteOn != null) {
                Boolean res = _callbackBeforeNoteOn.apply(this);
                return (res != null) ? res : true;
            }
        } else {
            if (_callbackBeforeNoteOff != null) {
                Boolean res = _callbackBeforeNoteOff.apply(this);
                return (res != null) ? res : true;
            }
        }
        return false;
    }

    /**
     * Play sequence.
     *
     * @param seq          Sequence to play.
     * @param sampleLength sequence playing time.
     * @param sampleDelay  Delaying time (in sample count).
     */
    public SiMMLTrack sequenceOn(MMLSequence seq, int sampleLength, int sampleDelay) {
        _trackStartDelay = sampleDelay;
        _trackStopDelay = sampleLength;
        _mmlData = (seq != null) ? (((SiMMLData) seq._owner)) : null;
        executor.initialize(seq);
        return this;
    }

    /**
     * Force stop sequence.
     *
     * @param sampleDelay   Delay time (in sample count).
     * @param stopWithReset Stop with channel resetting.
     */
    public SiMMLTrack sequenceOff(int sampleDelay, boolean stopWithReset) {
        _stopWithReset = stopWithReset;
        if (sampleDelay != 0) {
            _trackStopDelay = sampleDelay;
        } else {
            executor.clear();
            if (_stopWithReset) channel.reset();
        }
        return this;
    }

    /**
     * Limit key on length.
     *
     * @param stopDelay delay to key-off.
     */
    public void limitLength(int stopDelay) {
        int length = stopDelay - _trackStartDelay;
        if (length < _keyOnLength) {
            _keyOnLength = length;
            _keyOnCounter = _keyOnLength;
        }
    }

    /** Set this track disposable. */
    public void setDisposable() {
        _isDisposable = true;
    }

    // interfaces for mml command
    //

    /**
     * Set note immediately.
     * The calling path : SiONDriver.noteOn() -> SiMMLTrack.keyOn() -> executor.singleNote() ->(waiting for MMLEvent.DRIVER_NOTE)
     * -> SiMMLSequencer._onDriverNoteOn() -> SiMMLTrack.setNote() -> SiMMLTrack._mmlKeyOn().
     *
     * @param note         note number.
     * @param sampleLength length in sample count. 0 sets no key off (=weak slur).
     * @param slur         ((slur) set).
     */
    public void setNote(int note, int sampleLength, boolean slur /* = false */) {
        // play with key off when quantRatio == 0 or sampleLength != 0
        if ((quantRatio == 0 || sampleLength > 0) && !slur) {
            _keyOnLength = (int) (sampleLength * quantRatio) - quantCount - keyOnDelay;
            if (_keyOnLength < 1) _keyOnLength = 1;
        } else {
            // no key off
            _keyOnLength = 0;
        }
        _mmlKeyOn(note);
        _flagNoKeyOn = slur;
    }

    /**
     * Set pitch bending.
     *
     * @param noteFrom   Note number bending from.
     * @param tickLength length of pitch bending.
     */
    public void setPitchBend(int noteFrom, int tickLength) {
        executor.bendingFrom(noteFrom, tickLength);
    }

    /**
     * Channel module type (%) and select tone (1st argument of '_&#64;').
     *
     * @param type       Channel module type
     * @param channelNum Channel number. For %2-11, this value instanceof ((1st) same) argument of '_&#64;'. channel number of -1 ignores all voice settings by selectVoice.
     * @param toneNum    Tone number. Ussualy, this argument instanceof used only in %0;PSG and %1;APU.
     */
    public void setChannelModuleType(int type, int channelNum, int toneNum /* = Integer.MIN_VALUE */) {
        // change module type
        _channelModuleSetting = _table.channelModuleSetting[type];
        //_simulator = _table.simulators[type];

        // reset operator pgType, set SiMMLTrack._channelNumber inside
        _voiceIndex = _channelModuleSetting.initializeTone(this, channelNum, channel.getBufferIndex());
        //_voiceIndex = _simulator.initializeTone(this, channelNum, channel.bufferIndex);

        // select tone
        if (toneNum >= 0) {
            _voiceIndex = toneNum;
            _channelModuleSetting.selectTone(this, toneNum);
            //_simulator.selectTone(this, toneNum);
        }
    }

    /**
     * portamento (po).
     *
     * @param frame portamento changing time in frame count.
     */
    public void setPortament(int frame) {
        _set_sweep_step[1] = frame;
        if (frame != 0) {
            _pns_or[1] = true;
            _envelopOn(1);
        } else {
            _envelopOff(1);
        }
    }

    /**
     * set event trigger (%t)
     *
     * @param id          Event trigger ID of this track. This value can be refered from SiONTrackEvent.eventTriggerID.
     * @param noteOnType  Dispatching event type at note on. 0=no events, 1=NOTE_ON_FRAME, 2=NOTE_ON_STREAM, 3=both.
     * @param noteOffType Dispatching event type at note off. 0=no events, 1=NOTE_OFF_FRAME, 2=NOTE_OFF_STREAM, 3=both.
     * @see org.si.sion.events.SiONTrackEvent
     */
    public void setEventTrigger(int id, int noteOnType, int noteOffType) {
        _eventTriggerID = id;
        _eventTriggerTypeOn = noteOnType;
        _eventTriggerTypeOff = noteOffType;
        _callbackBeforeNoteOn = (noteOnType != 0) ? _eventTriggerOn : null;
        _callbackBeforeNoteOff = (noteOffType != 0) ? _eventTriggerOff : null;
    }

    /**
     * dispatch note on event once (%e)
     *
     * @param id         Event trigger ID of this track. This value can be refered from SiONTrackEvent.eventTriggerID.
     * @param noteOnType Dispatching event type at note on. 0=no events, 1=NOTE_ON_FRAME, 2=NOTE_ON_STREAM, 3=both.
     * @see org.si.sion.events.SiONTrackEvent
     */
    public void dispatchNoteOnEvent(int id, int noteOnType) {
        if (noteOnType != 0) {
            int currentTID = _eventTriggerID;
            int currentType = _eventTriggerTypeOn;
            _eventTriggerID = id;
            _eventTriggerTypeOn = noteOnType;
            if (_eventTriggerOn != null) _eventTriggerOn.apply(this);
            _eventTriggerID = currentTID;
            _eventTriggerTypeOn = currentType;
        }
    }

    /**
     * set envelop step (&#64;fps)
     *
     * @param fps Frame par second
     */
    public void setEnvelopFPS(int fps) {
        _env_internval = SiOPMTable.getInstance().rate / fps;
    }

    /**
     * release sweep (2nd argument of "s")
     *
     * @param sweep sweeping speed
     */
    public void setReleaseSweep(int sweep) {
        _set_sweep_step[0] = sweep << FIXED_BITS;
        _set_sweep_end[0] = (sweep < 0) ? 0 : SWEEP_MAX;
        if (sweep != 0) {
            _pns_or[0] = true;
            _envelopOn(0);
        } else {
            _envelopOff(0);
        }
    }

    /**
     * amplitude/pitch modulation envelop (ma, mp)
     *
     * @param isPitchMod The command is 'ma' or 'mp'.
     * @param depth      start modulation depth (((1st) same) argument)
     * @param end_depth  end modulation depth (((2nd) same) argument)
     * @param delay      changing delay (((3rd) same) argument)
     * @param term       changing term (((4th) same) argument)
     */
    public void setModulationEnvelop(boolean isPitchMod, int depth, int end_depth, int delay, int term) {
        // select table
        SLLint[] table = (isPitchMod) ? _table_env_mp : _table_env_ma;

        // free previous table
        if (table[1] != null) SLLint.freeList(table[1]);

        if ((0 <= depth && depth < end_depth) || (depth < 0 && depth > end_depth)) {
            // make table and envelop on
            table[1] = _makeModulationTable(depth, end_depth, delay, term);
            _envelopOn(1);
        } else {
            // free table and envelop off
            table[1] = null;
            if (isPitchMod) channel.setPitchModulation(depth);
            else channel.setAmplitudeModulation(depth);
            _envelopOff(1);
        }
    }

    /**
     * set tone envelop (&#64;&#64;, _&#64;&#64;)
     *
     * @param noteOn 1 for normal envelop, 0 for not-off envelop.
     * @param table  table SiMMLEnvelopTable
     * @param step   envelop speed (((2nd) same) argument)
     */
    public void setToneEnvelop(int noteOn, SiMMLEnvelopTable table, int step) {
        if (table == null || step == 0) {
            _set_env_voice[noteOn] = null;
            _envelopOff(noteOn);
        } else {
            _set_env_voice[noteOn] = table.head;
            _set_cnt_voice[noteOn] = step;
            _envelopOn(noteOn);
        }
    }

    /**
     * set amplitude envelop (na, _na)
     *
     * @param noteOn 1 for normal envelop, 0 for not-off envelop.
     * @param table  table SiMMLEnvelopTable
     * @param step   envelop speed (((2nd) same) argument)
     * @param offset true for relative control (!na command), false for absolute control.
     */
    public void setAmplitudeEnvelop(int noteOn, SiMMLEnvelopTable table, int step, boolean offset /* = false */) {
        if (table == null || step == 0) {
            _set_env_exp[noteOn] = null;
            _envelopOff(noteOn);
        } else {
            _set_env_exp[noteOn] = table.head;
            _set_cnt_exp[noteOn] = step;
            _set_exp_offset[noteOn] = offset;
            _envelopOn(noteOn);
        }
    }

    /**
     * set filter envelop (nf, _nf)
     *
     * @param noteOn 1 for normal envelop, 0 for not-off envelop.
     * @param table  table SiMMLEnvelopTable
     * @param step   envelop speed (((2nd) same) argument)
     */
    public void setFilterEnvelop(int noteOn, SiMMLEnvelopTable table, int step) {
        if (table == null || step == 0) {
            _set_env_filter[noteOn] = null;
            _envelopOff(noteOn);
        } else {
            _set_env_filter[noteOn] = table.head;
            _set_cnt_filter[noteOn] = step;
            _envelopOn(noteOn);
        }
    }

    /**
     * set pitch envelop (np, _np)
     *
     * @param noteOn 1 for normal envelop, 0 for not-off envelop.
     * @param table  table SiMMLEnvelopTable
     * @param step   envelop speed (((2nd) same) argument)
     */
    public void setPitchEnvelop(int noteOn, SiMMLEnvelopTable table, int step) {
        if (table == null || step == 0) {
            _set_env_pitch[noteOn] = _env_zero_table;
            _envelopOff(noteOn);
        } else {
            _set_env_pitch[noteOn] = table.head;
            _set_cnt_pitch[noteOn] = step;
            _pns_or[noteOn] = true;
            _envelopOn(noteOn);
        }
    }

    /**
     * set note envelop (nt, _nt)
     *
     * @param noteOn 1 for normal envelop, 0 for not-off envelop.
     * @param table  table SiMMLEnvelopTable
     * @param step   envelop speed (((2nd) same) argument)
     */
    public void setNoteEnvelop(int noteOn, SiMMLEnvelopTable table, int step) {
        if (table == null || step == 0) {
            _set_env_note[noteOn] = _env_zero_table;
            _envelopOff(noteOn);
        } else {
            _set_env_note[noteOn] = table.head;
            _set_cnt_note[noteOn] = step;
            _pns_or[noteOn] = true;
            _envelopOn(noteOn);
        }
    }

    //
    // Internal uses
    //

    // initialize / reset
    //

    /** @private [internal] initialize track. [NOTE] Have to call reset() after this. */
    SiMMLTrack _initialize(MMLSequence seq, int fps, int internalTrackID, Function<SiMMLTrack, Boolean> eventTriggerOn, Function<SiMMLTrack, Boolean> eventTriggerOff, boolean isDisposable) {
        _internalTrackID = internalTrackID;
        _isDisposable = isDisposable;
        _defaultFPS = fps;
        _eventTriggerOn = eventTriggerOn;
        _eventTriggerOff = eventTriggerOff;
        _eventTriggerID = -1;
        _eventTriggerTypeOn = 0;
        _eventTriggerTypeOff = 0;
        _mmlData = (seq != null) ? (((SiMMLData) seq._owner)) : null;
        executor.initialize(seq);

        return this;
    }

    /** @private [internal] reset track. */
    void _reset(int bufferIndex) {
        int i;

        // channel module setting
        _channelModuleSetting = _table.channelModuleSetting[SiMMLTable.MT_PSG];
        _simulator = _table.simulators[SiMMLTable.MT_PSG];
        _channelNumber = 0;

        // initialize channel by channel settings
        if (_mmlData != null) {
            _vcommandShift = _mmlData.defaultVCommandShift;
            _velocityMode = _mmlData.defaultVelocityMode;
            _expressionMode = _mmlData.defaultExpressionMode;
        } else {
            _vcommandShift = 4;
            _velocityMode = SiOPMTable.VM_LINEAR;
            _expressionMode = SiOPMTable.VM_LINEAR;
        }
        _velocity = 256;
        _expression = 128;
        _pitchBend = 0;
        _note = -1;
        channel = null;
        _voiceIndex = _channelModuleSetting.initializeTone(this, Integer.MIN_VALUE, bufferIndex);
        //_voiceIndex = _simulator.initializeTone(this, Integer.MIN_VALUE, bufferIndex);
        int[][] tlTables = SiOPMTable.getInstance().eg_tlTables;
        channel.setVolumeTables(tlTables[_velocityMode], tlTables[_expressionMode]);

        // initialize parameters
        noteShift = 0;
        pitchShift = 0;
        _keyOnCounter = 0;
        _keyOnLength = 0;
        _flagNoKeyOn = false;
        _processMode = NORMAL;
        _trackStartDelay = 0;
        _trackStopDelay = 0;
        _stopWithReset = false;
        keyOnDelay = 0;
        quantRatio = 1;
        quantCount = 0;
        eventMask = NO_MASK;
        _env_pitch_active = false;
        _pitchIndex = 0;
        _sweep_pitch = 0;
        _env_exp_offset = 0;
        setEnvelopFPS(_defaultFPS);
        _callbackBeforeNoteOn = null;
        _callbackBeforeNoteOff = null;
        _callbackUpdateRegister = this::_defaultUpdateRegister;
        _residue = 0;
        _priority = 0;
        _env_exp = null;
        _env_voice = null;
        _env_note = _env_zero_table;
        _env_pitch = _env_zero_table;
        _env_filter = null;
        _env_ma = null;
        _env_mp = null;

        // reset envelop tables
        for (i = 0; i < 2; i++) {
            _set_processMode[i] = NORMAL;
            _set_env_exp[i] = null;
            _set_env_voice[i] = null;
            _set_env_note[i] = _env_zero_table;
            _set_env_pitch[i] = _env_zero_table;
            _set_env_filter[i] = null;
            _pns_or[i] = false;
            _set_exp_offset[i] = false;
            _set_cnt_exp[i] = 1;
            _set_cnt_voice[i] = 1;
            _set_cnt_note[i] = 1;
            _set_cnt_pitch[i] = 1;
            _set_cnt_filter[i] = 1;
            _set_sweep_step[i] = 0;
            _set_sweep_end[i] = 0;
            _table_env_ma[i] = null;
            _table_env_mp[i] = null;
        }

        // reset pointer
        executor.resetPointer();
    }

    /** @private [internal] reset volume offset. */
    public void _resetVolumeOffset() {
        channel.offsetVolume(_expression, _velocity);
    }

    // processing
    //

    /** @private [internal] prepare buffer. this instanceof called from SiMMLSequencer.process()/dummyProcess(). */
    int _prepareBuffer(int bufferingLength) {
        // register all tables
        if (_mmlData != null) _mmlData._registerAllTables();
        else { // clear all stencil tables
            SiOPMTable._instance.samplerTables[0].stencil = null;
            SiOPMTable._instance._stencilCustomWaveTables = null;
            SiOPMTable._instance._stencilPCMVoices = null;
            _table._stencilEnvelops = null;
            _table._stencilVoices = null;
        }

        // no delay, usually
        if (_trackStartDelay == 0) {
            return bufferingLength;
        }

        // wait for starting sound
        if (bufferingLength <= _trackStartDelay) {
            _trackStartDelay -= bufferingLength;
            return 0;
        }

        // start sound in this frame
        int len = bufferingLength - _trackStartDelay;
        channel.nop(_trackStartDelay);
        _trackStartDelay = 0;

        _priority++;

        return len;
    }

    /** @private [internal] buffering */
    void _buffer(int length) {
        // check track stopping
        boolean trackStop = false;
        int trackStopResume = 0;
        if (_trackStopDelay > 0) {
            if (_trackStopDelay > length) {
                _trackStopDelay -= length;
            } else {
                trackStopResume = length - _trackStopDelay;
                trackStop = true;
                length = _trackStopDelay;
                _trackStopDelay = 0;
            }
        }

        // buffeirng
        if (_keyOnCounter == 0) {
            // no status changing
            $(length);
        } else if (_keyOnCounter > length) {
            // decrement _keyOnCounter
            $(length);
            _keyOnCounter -= length;
        } else {
            // process -> toggle key -> process
            length -= _keyOnCounter;
            $(_keyOnCounter);
            _toggleKey();
            if (length > 0) $(length);
        }

        // track stopped
        if (trackStop) {
            if (executor.pointer != null) {
                executor.stop();
                if (_stopWithReset) {
                    _keyOff();
                    _note = -1;
                    channel.reset();
                }
            } else if (channel.isNoteOn()) {
                _keyOff();
                _note = -1;
                if (_stopWithReset) {
                    channel.reset();
                }
            }
            if (trackStopResume > 0) $(trackStopResume);
        }
    }

    // processing inside
    void $(int procLen) {
        switch (_processMode) {
            case NORMAL:
                channel.buffer(procLen);
                break;
            case ENVELOP:
                _residue = _bufferEnvelop(procLen, _residue);
                break;
        }
    }

    // buffering with table envelops
    private int _bufferEnvelop(int length, int step) {
        int x;

        while (length >= step) {
            // processing
            if (step > 0) channel.buffer(step);

            // change expression
            if (_env_exp != null && --_cnt_exp == 0) {
                x = _env_exp_offset + _env_exp.i;
                if (x < 0) {
                    x = 0;
                } else if (x > 128) {
                    x = 128;
                }
                channel.offsetVolume(x, _velocity);
                _env_exp = _env_exp.next;
                _cnt_exp = _max_cnt_exp;
            }

            // change pitch/note
            if (_env_pitch_active) {
                channel.setPitch(_env_pitch.i + (_env_note.i << 6) + (_sweep_pitch >> FIXED_BITS));
                // pitch envelop
                if (--_cnt_pitch == 0) {
                    _env_pitch = _env_pitch.next;
                    _cnt_pitch = _max_cnt_pitch;
                }
                // note envelop
                if (--_cnt_note == 0) {
                    _env_note = _env_note.next;
                    _cnt_note = _max_cnt_note;
                }
                // sweep
                _sweep_pitch += _sweep_step;
                if (_sweep_step > 0) {
                    if (_sweep_pitch > _sweep_end) {
                        _sweep_pitch = _sweep_end;
                        _sweep_step = 0;
                    }
                } else {
                    if (_sweep_pitch < _sweep_end) {
                        _sweep_pitch = _sweep_end;
                        _sweep_step = 0;
                    }
                }
            }

            // change filter
            if (_env_filter != null && --_cnt_filter == 0) {
                channel.setFilterOffset(_env_filter.i);
                _env_filter = _env_filter.next;
                _cnt_filter = _max_cnt_filter;
            }

            // change tone
            if (_env_voice != null && --_cnt_voice == 0) {
                _channelModuleSetting.selectTone(this, _env_voice.i);
                //_simulator.selectTone(this, _env_voice.i);
                _env_voice = _env_voice.next;
                _cnt_voice = _max_cnt_voice;
            }

            // change modulations
            if (_env_ma != null) {
                channel.setAmplitudeModulation(_env_ma.i);
                _env_ma = _env_ma.next;
            }
            if (_env_mp != null) {
                channel.setPitchModulation(_env_mp.i);
                _env_mp = _env_mp.next;
            }

            // index increment
            length -= step;
            step = _env_internval;
        }

        // rest process
        if (length > 0) channel.buffer(length);

        // next rest length
        return _env_internval - length;
    }

    // key on/off
    //

    // toggle note
    private void _toggleKey() {
        if (channel.isNoteOn()) {
            _keyOff();
        } else {
            _keyOn();
        }
    }

    // note on
    private void _keyOn() {
        // callback
        if (_callbackBeforeNoteOn != null) {
            if (_callbackBeforeNoteOn.apply(this) != null) return;
        }

        // change pitch
        int oldPitch = channel.getPitch();
        _pitchIndex = ((_note + noteShift) << 6) + pitchShift;
        channel.setPitch(_pitchIndex + _pitchBend);

        // note on
        if (!_flagNoKeyOn) {
            // reset previous envelop
            if (_processMode == ENVELOP) {
                channel.offsetVolume(_expression, _velocity);
                _channelModuleSetting.selectTone(this, _voiceIndex);
                //_simulator.selectTone(this, _voiceIndex);
                channel.setFilterOffset(128);
            }
            // previous note off
            if (channel.isNoteOn()) {
                // callback
                if (_callbackBeforeNoteOff != null) _callbackBeforeNoteOff.apply(this);
                channel.noteOff();
            }
            // update process
            _updateProcess(1);
            // note on
            channel.noteOn();
        } else {
            // portamento
            if (_set_sweep_step[1] > 0) {
                channel.setPitch(oldPitch);
                _sweep_step = ((_pitchIndex - oldPitch) << FIXED_BITS) / _set_sweep_step[1];
                _sweep_end = _pitchIndex << FIXED_BITS;
                _sweep_pitch = oldPitch << FIXED_BITS;
            } else {
                _sweep_pitch = channel.getPitch() << FIXED_BITS;
            }
            // try to set envelop off
            _envelopOff(1);
        }

        _flagNoKeyOn = false;

        // set key on counter
        _keyOnCounter = _keyOnLength;
    }

    // note off
    private void _keyOff() {
        // callback
        if (_callbackBeforeNoteOff != null) {
            if (_callbackBeforeNoteOff.apply(this) != null) return;
        }

        // note off
        channel.noteOff();
        // no key off after this
        _keyOnCounter = 0;
        // update process
        _updateProcess(0);
        // priority down
        _priority += 32;
    }

    private void _updateProcess(int keyOn) {
        // prepare next process
        _processMode = _set_processMode[keyOn];

        if (_processMode == ENVELOP) {
            // set envelop tables
            _env_exp = _set_env_exp[keyOn];
            _env_voice = _set_env_voice[keyOn];
            _env_note = _set_env_note[keyOn];
            _env_pitch = _set_env_pitch[keyOn];
            _env_filter = _set_env_filter[keyOn];
            // set envelop counters
            _max_cnt_exp = _set_cnt_exp[keyOn];
            _max_cnt_voice = _set_cnt_voice[keyOn];
            _max_cnt_note = _set_cnt_note[keyOn];
            _max_cnt_pitch = _set_cnt_pitch[keyOn];
            _max_cnt_filter = _set_cnt_filter[keyOn];
            _cnt_exp = 1;
            _cnt_voice = 1;
            _cnt_note = 1;
            _cnt_pitch = 1;
            _cnt_filter = 1;
            // set modulation envelops
            _env_ma = _table_env_ma[keyOn];
            _env_mp = _table_env_mp[keyOn];
            // set sweep
            _sweep_step = (keyOn != 0) ? 0 : _set_sweep_step[keyOn];
            _sweep_end = (keyOn != 0) ? 0 : _set_sweep_end[keyOn];
            // set pitch values
            _sweep_pitch = channel.getPitch() << FIXED_BITS;
            _env_exp_offset = (_set_exp_offset[keyOn]) ? _expression : 0;
            _env_pitch_active = _pns_or[keyOn];
            // activate filter
            if (!channel.isFilterActive()) channel.activateFilter(_env_filter != null);
            // reset index
            _residue = 0;
        }
    }

    // event handlers
    //

    /** @private [internal] handler for MMLEvent.REST. */
    void _onRestEvent() {
        _flagNoKeyOn = false;
    }

    /** @private [internal] handler for MMLEvent.NOTE. */
    void _onNoteEvent(int note, int length) {
        _keyOnLength = (int) (length * quantRatio) - quantCount - keyOnDelay;
        if (_keyOnLength < 1) _keyOnLength = 1;
        _mmlKeyOn(note);
    }

    /** @private [internal] Slur without next notes key on. This have to be called just after keyOn(). */
    void _onSlur() {
        _flagNoKeyOn = true;
        _keyOnCounter = 0;
    }

    /** @private [internal] Slur with next notes key on. This have to be called just after keyOn(). */
    void _onSlurWeak() {
        _keyOnCounter = 0;
    }

    /**
     * @param nextNote The 2nd note to intergradate.
     * @param term     bending time in sample count.
     * @private [internal] Set pitch bend (and slur) immediately. This function called from pitchBend() and '*' command.
     */
    void _onPitchBend(int nextNote, int term) {
        int startPitch = channel.getPitch();
        int endPitch = (((nextNote + noteShift) << 6) | (startPitch & 63)) + pitchShift;
        _onSlur();
        if (startPitch == endPitch) return;

        _sweep_step = ((endPitch - startPitch) << FIXED_BITS) * _env_internval / term;
        _sweep_end = endPitch << FIXED_BITS;
        _sweep_pitch = startPitch << FIXED_BITS;
        _env_pitch_active = true;
        _env_note = _set_env_note[1];
        _env_pitch = _set_env_pitch[1];

        _processMode = ENVELOP;
    }

    /** change note length. call from SiMMLSequence._onSlur()/_onSlurWeek() when it's masked. */
    void _changeNoteLength(int length) {
        _keyOnCounter = (int) (length * quantRatio) - quantCount - keyOnDelay;
        if (_keyOnCounter < 1) _keyOnCounter = 1;
    }

    /** @praivate [internal use] Channel parameters (&#64;) */
    MMLSequence _setChannelParameters(int[] param) {
        MMLSequence ret = null;
        if (param[0] != Integer.MIN_VALUE) {
            ret = _channelModuleSetting.selectTone(this, param[0]);
            //ret = _simulator.selectTone(this, param[0]);
            _voiceIndex = param[0];
        }
        channel.setParameters(param);
        return ret;
    }

    /** mml v command */
    void _mmlVCommand(int v) {
        _velocity = v << _vcommandShift;
    }

    /** mml v command */
    void _mmlVShift(int v) {
        _velocity += v << _vcommandShift;
    }

    // update register
    private void _defaultUpdateRegister(int addr, int data) {
        channel.setRegister(addr, data);
    }

    // mml key on
    private void _mmlKeyOn(int note) {
        _note = note;
        _trackStartDelay = 0;
        if (keyOnDelay != 0) {
            _keyOff();
            _keyOnCounter = keyOnDelay;
        } else {
            _keyOn();   // if _keyOnLength=0 -> _keyOnCounter=0 -> No key off
        }
    }

    // internal functions
    //

    // envelop off
    private void _envelopOff(int noteOn) {
        // update (pitch || note || sweep)
        if (_set_sweep_step[noteOn] == 0 &&
                _set_env_pitch[noteOn] == _env_zero_table &&
                _set_env_note[noteOn] == _env_zero_table) {
            _pns_or[noteOn] = false;
        }

        // all envelops are off -> update processMode
        if (!_pns_or[noteOn] &&
                _table_env_ma[noteOn] == null &&
                _table_env_mp[noteOn] == null &&
                _set_env_exp[noteOn] == null &&
                _set_env_filter[noteOn] == null &&
                _set_env_voice[noteOn] == null) {
            _set_processMode[noteOn] = NORMAL;
        }
    }

    // envelop on
    private void _envelopOn(int noteOn) {
        _set_processMode[noteOn] = ENVELOP;
    }

    // make modulation table
    private SLLint _makeModulationTable(int depth, int end_depth, int delay, int term) {
        // initialize
        SLLint list = SLLint.allocList(delay + term + 1, 0);
        int i, step;
        SLLint elem;

        // delay
        elem = list;
        if (delay != 0) {
            for (i = 0; i < delay; i++, elem = elem.next) {
                elem.i = depth;
            }
        }
        // changing
        if (term != 0) {
            depth <<= FIXED_BITS;
            step = ((end_depth << FIXED_BITS) - depth) / term;
            for (i = 0; i < term; i++, elem = elem.next) {
                elem.i = (depth >> FIXED_BITS);
                depth += step;
            }
        }
        // last data
        elem.i = end_depth;

        return list;
    }
}
