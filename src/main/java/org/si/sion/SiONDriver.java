//
// SiON driver
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

import vavi.events.SampleDataEvent;

import org.si.sion.effector.SiEffectModule;
import org.si.sion.events.SiONEvent;
import org.si.sion.events.SiONMIDIEvent;
import org.si.sion.events.SiONTrackEvent;
import org.si.sion.midi.MIDIModule;
import org.si.sion.midi.SMFData;
import org.si.sion.midi.SiONDataConverterSMF;
import org.si.sion.midi.SiONMIDIEventFlag;
import org.si.sion.module.ISiOPMWaveInterface;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMData;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerData;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.module.SiOPMWaveTable;
import org.si.sion.sequencer.SiMMLEnvelopTable;
import org.si.sion.sequencer.SiMMLSequencer;
import org.si.sion.sequencer.SiMMLTable;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.sequencer.SiMMLVoice;
import org.si.sion.sequencer.base.MMLData;
import org.si.sion.sequencer.base.MMLEvent;
import org.si.sion.sequencer.base.MMLSequence;
import org.si.sion.utils.Fader;
import org.si.sion.utils.SiONUtil;
import org.si.sion.utils.soundfont.SiONSoundFont;
import org.si.utils.ByteArray;
import org.si.utils.ErrorEvent;
import org.si.utils.Event;
import org.si.utils.EventDispatcher;
import org.si.utils.IOErrorEvent;
import org.si.utils.SLLint;
import vavi.media.Sound;
import vavi.media.SoundChannel;
import vavi.media.SoundTransform;
import vavi.net.URLRequest;


// Dispatching events
/** @eventType org.si.sion.events.SiONEvent.QUEUE_PROGRESS */
// [Event(name=SiONEvent.QUEUE_PROGRESS,   type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.QUEUE_COMPLETE */
// [Event(name=SiONEvent.QUEUE_COMPLETE,   type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.QUEUE_CANCEL */
// [Event(name=SiONEvent.QUEUE_CANCEL,     type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.STREAM */
// [Event(name=SiONEvent.STREAM,          type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.STREAM_START */
// [Event(name=SiONEvent.STREAM_START,     type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.STREAM_STOP */
// [Event(name=SiONEvent.STREAM_STOP,      type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.FINISH_SEQUENCE */
// [Event(name=SiONEvent.FINISH_SEQUENCE,  type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.FADE_PROGRESS */
// [Event(name=SiONEvent.FADE_PROGRESS,    type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.FADE_IN_COMPLETE */
// [Event(name=SiONEvent.FADE_IN_COMPLETE,  type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONEvent.FADE_OUT_COMPLETE */
// [Event(name=SiONEvent.FADE_OUT_COMPLETE, type="org.si.sion.events.SiONEvent")]
/** @eventType org.si.sion.events.SiONTrackEvent.NOTE_ON_STREAM */
// [Event(name=SiONTrackEvent.NOTE_ON_STREAM,    type="org.si.sion.events.SiONTrackEvent")]
/** @eventType org.si.sion.events.SiONTrackEvent.NOTE_OFF_STREAM */
// [Event(name=SiONTrackEvent.NOTE_OFF_STREAM,   type="org.si.sion.events.SiONTrackEvent")]
/** @eventType org.si.sion.events.SiONTrackEvent.NOTE_ON_FRAME */
// [Event(name=SiONTrackEvent.NOTE_ON_FRAME,     type="org.si.sion.events.SiONTrackEvent")]
/** @eventType org.si.sion.events.SiONTrackEvent.NOTE_OFF_FRAME */
// [Event(name=SiONTrackEvent.NOTE_OFF_FRAME,    type="org.si.sion.events.SiONTrackEvent")]
/** @eventType org.si.sion.events.SiONTrackEvent.BEAT */
// [Event(name=SiONTrackEvent.BEAT,            type="org.si.sion.events.SiONTrackEvent")]
/** @eventType org.si.sion.events.SiONTrackEvent.CHANGE_BPM */
// [Event(name=SiONTrackEvent.CHANGE_BPM,       type="org.si.sion.events.SiONTrackEvent")]


/**
 * SiONDriver class provides the driver of SiON's digital signal processor emulator. SiON's all basic operations are ((SiONDriver) provided)'s properties, methods and events. You can create only one SiONDriver instance in one SWF file, and the error appears when you try to create plural SiONDrivers.<br/>
 *
 * @example 1) The simplest sample. Create new instance and call play with MML string.<br/>
 * <pre>
 * // create driver instance.
 * SiONDriver driver = new SiONDriver();
 * // call play() with mml string whenever you want to play sound.
 * driver.play("t100 l8 [ ccggaag4 ffeeddc4 | [ggffeed4]2 ]2");
 * </pre>
 * @see SiONData
 * @see SiONVoice
 * @see SiONEvent
 * @see SiONTrackEvent
 * @see SiOPMModule
 * @see SiMMLSequencer
 * @see SiEffectModule
 */
public class SiONDriver extends EventDispatcher implements ISiOPMWaveInterface {

    // constants
    //

    /** version number */
    public static final String VERSION = "0.6.6.0";

    /** note-on exception mode "ignore", SiON does not consider about track ID's conflict in noteOn() method (default). */
    public static final int NEM_IGNORE = 0;
    /** note-on exception mode "reject", Reject new note when the track IDs are conflicted. */
    public static final int NEM_REJECT = 1;
    /** note-on exception mode "overwrite", Overwrite current note when the track IDs are conflicted. */
    public static final int NEM_OVERWRITE = 2;
    /** note-on exception mode "shift", Shift the sound timing to next quantize when the track IDs are conflicted. */
    public static final int NEM_SHIFT = 3;

    private static final int NEM_MAX = 4;

    // event listener type
    private static final int NO_LISTEN = 0;
    private static final int LISTEN_QUEUE = 1;
    private static final int LISTEN_PROCESS = 2;

    // time averaging sample count
    private static final int TIME_AVARAGING_COUNT = 8;

    // variables
    //

    /** SiOPM digital signal processor module instance. */
    public SiOPMModule module;

    /** Effector module instance. */
    public SiEffectModule effector;

    /** Sequencer module instance. */
    public SiMMLSequencer sequencer;

    // private:
    // general
    private SiONData _data;         // data to compile or process
    private SiONData _tempData;     // temporary data
    private String _mmlString;      // mml string of previous compiling
    // sound related
    private Sound _sound;                   // sound stream instance
    private SoundChannel _soundChannel;     // sound channel instance
    private SoundTransform _soundTransform; // sound transform
    private Fader _fader;                   // sound fader
    // SiOPM DSP module related
    private int _channelCount;          // module output channels (1 or 2)
    private double _sampleRate;         // module output frequency ratio (44100 or 22050)
    private int _bitRate;               // module output bitrate
    private int _bufferLength;          // module and streaming buffer size (8192, 4096 or 2048)
    private boolean _debugMode;         // true; throw Error, false; throw ErrorEvent
    private boolean _dispatchStreamEvent; // dispatch steam event
    private boolean _dispatchFadingEvent; // dispatch fading event
    private boolean _inStreaming;         // in streaming
    private boolean _preserveStop;        // preserve stop after streaming
    private boolean _suspendStreaming;      // suspend streaming
    private boolean _suspendWhileLoading;   // suspend starting steam while loading
    private Object[] _loadingSoundList;        // loading sound list
    private boolean _isFinishSeqDispatched; // FINISH_SEQUENCE event already dispacthed
    // operation related
    private boolean _autoStop;          // auto stop when the sequence finished
    private int _noteOnExceptionMode;   // track id exception mode
    private boolean _isPaused;          // flag to pause
    private double _position;           // start position [ms]
    private double _masterVolume;       // master volume
    private double _faderVolume;        // fader volume
    private boolean _dispatchChangeBPMEventWhenPositionChanged;
    // background sound
    private Sound _backgroundSound;                 // background Sound
    private double _backgroundLoopPoint;            // loop point (in seconds)
    private int _backgroundFadeOutFrames;           // fading out frames
    private int _backgroundFadeInFrames;            // fading in frames
    private int _backgroundFadeGapFrames;           // fading gap frames
    private int _backgroundTotalFadeFrames;         // total fading in frames
    private SiONVoice _backgroundVoice;             // voice
    private SiOPMWaveSamplerData _backgroundSample; // sampling data
    private SiMMLTrack _backgroundTrack;            // track for background Sound
    private SiMMLTrack _backgroundTrackFadeOut;     // track for background Sound's cross fading
    // queue
    private int _queueInterval;         // interupting interval to execute queued jobs
    private int _queueLength;           // queue length to execute
    private double _jobProgress;        // progression of current job
    private int _currentJob;            // current job 0=no job, 1=compile, 2=render
    private List<SiONDriverJob> _jobQueue = null;   // compiling/rendering jobs queue
    private List<SiONTrackEvent> _trackEventQueue;  // SiONTrackEvents queue
    // timer interruption
    private MMLSequence _timerSequence;     // global sequence
    private MMLEvent _timerIntervalEvent;   // MMLEvent.GLOBAL_WAIT event
    private Runnable _timerCallback;        // callback function
    // rendering
    private double[] _renderBuffer;  // rendering buffer
    private int _renderBufferChannelCount;  // rendering buffer channel count
    private int _renderBufferIndex;         // rendering buffer writing index
    private int _renderBufferSizeMax;       // maximum value of rendering buffer size
    // timers
    private int _timeCompile;           // previous compiling time.
    private int _timeRender;            // previous rendering time.
    private int _timeProcess;           // averge processing time in 1sec.
    private int _timeProcessTotal;      // total processing time in last 8 bufferings.
    private SLLint _timeProcessData;    // processing time data of last 8 bufferings.
    private double _timeProcessAveRatio;// number to averaging _timeProcessTotal
    private int _timePrevStream;        // previous streaming time.
    private double _latency;            // streaming latency [ms]
    private int _prevFrameTime;         // previous frame time
    private int _frameRate;             // frame rate
    // listeners management
    private int _eventListenerPrior;    // event listeners priority
    private int _listenEvent;           // current lintening event
    // MIDI related
    private MIDIModule _midiModule;                 // midi sound module
    private SiONDataConverterSMF _midiConverter;    // SMF data converter

    // mutex instance
    private static SiONDriver _mutex = null;            // unique instance
    private static boolean _allowPluralDrivers = false; // allow plural drivers

    // properties
    //

    /** Instance of unique SiONDriver. null when new SiONDriver instanceof not created yet. */
    public static SiONDriver mutex() {
        return _mutex;
    }

    // data

    /** MML string (this property instanceof only available during compiling). */
    public String getMmlString() {
        return _mmlString;
    }

    /** Data to compile, render and process. */
    public SiONData getData() {
        return _data;
    }

    /** flash.media.Sound instance to stream SiON's sound. */
    public Sound getSound() {
        return _sound;
    }

    /** flash.media.SoundChannel instance of SiON's sound stream (this property instanceof only available during streaming). */
    public SoundChannel getSoundChannel() {
        return _soundChannel;
    }

    /** Fader to control fade-in/out. You can check activity by "fader.isActive". */
    public Fader getFader() {
        return _fader;
    }

    // sound parameters

    /** The number of sound tracks (this property instanceof only available during streaming). */
    public int getTrackCount() {
        return sequencer.tracks.size();
    }

    /** Streaming buffer length. */
    public int getBufferLength() {
        return _bufferLength;
    }

    /** Sample rate (44100 instanceof only available in current version). */
    public double getSampleRate() {
        return _sampleRate;
    }

    /** bit rate, the value of 0 means the wave instanceof ((float) represented) value[-1 - +1]. */
    public double getBitRate() {
        return _bitRate;
    }

    /** Sound volume. */
    public double getVolume() {
        return _masterVolume;
    }

    public void setVolume(double v) {
        _masterVolume = v;
        // _soundTransform.volume = _masterVolume * _faderVolume;
        // if (_soundChannel != null) _soundChannel.soundTransform = _soundTransform;
    }

    /** Sound panning. */
    public double getPan() {
        // return _soundTransform.pan;
        return 0;
    }

    public void setPan(double p) {
        // _soundTransform.pan = p;
        // if (_soundChannel != null) _soundChannel.soundTransform = _soundTransform;
    }

    // measured times

    /** previous compiling time [ms]. */
    public int getCompileTime() {
        return _timeCompile;
    }

    /** previous rendering time [ms]. */
    public int getRenderTime() {
        return _timeRender;
    }

    /** average processing time in 1sec [ms]. */
    public int getProcessTime() {
        return _timeProcess;
    }

    /** progression of current compiling/rendering (0=start -> 1=finish). */
    public double getJobProgress() {
        return _jobProgress;
    }

    /** progression of all queued jobs (0=start -> 1=finish). */
    public double getJobQueueProgress() {
        if (_queueLength == 0) return 1;
        return (_queueLength - _jobQueue.size() - 1 + _jobProgress) / _queueLength;
    }

    /** streaming latency [ms]. */
    public double getLatency() {
        return _latency;
    }

    /** compiling/rendering jobs queue length. */
    public int getJobQueueLength() {
        return _jobQueue.size();
    }

    // status flags

    /** Is job executing ? */
    public boolean getIsJobExecuting() {
        return (_jobProgress > 0 && _jobProgress < 1);
    }

    /** Is streaming ? */
    public boolean isPlaying() {
        return (_soundChannel != null);
    }

    /** Is paused ? */
    public boolean isPaused() {
        return _isPaused;
    }

    // background sound

    /** background sound */
    public Sound getBackgroundSound() {
        return _backgroundSound;
    }

    /** track for background sound */
    public SiMMLTrack getBackgroundSoundTrack() {
        return _backgroundTrack;
    }

    /** background sound fading out time in seconds */
    public double getBackgroundSoundFadeOutTime() {
        return _backgroundFadeOutFrames * _bufferLength / _sampleRate;
    }

    /** background sound fading in time in seconds */
    public double getBackgroundSoundFadeInTime() {
        return _backgroundFadeInFrames * _bufferLength / _sampleRate;
    }

    /** background sound fading time in seconds */
    public double getBackgroundSoundFadeGapTime() {
        return _backgroundFadeGapFrames * _bufferLength / _sampleRate;
    }

    /** background sound volume @default 0.5 */
    public double getBackgroundSoundVolume() {
        return _backgroundVoice.channelParam.volumes[0];
    }

    public void setBackgroundSoundVolume(double vol) {
        _backgroundVoice.channelParam.volumes[0] = vol;
        if (_backgroundTrack != null) _backgroundTrack.setMasterVolume((int) (vol * 128));
        if (_backgroundTrackFadeOut != null) _backgroundTrackFadeOut.setMasterVolume((int) (vol * 128));
    }

    /** MIDI sound module */
    public MIDIModule getMidiModule() {
        return _midiModule;
    }

    // operation

    /** Get playing position[ms] of current data, or Set initial position of playing data. @default 0 */
    public double getPosition() {
        return sequencer.getProcessedSampleCount() * 1000 / _sampleRate;
    }

    public void setPosition(double pos) {
        _position = pos;
        if (sequencer.isReadyToProcess()) {
            sequencer._resetAllTracks();
            sequencer.dummyProcess((int) (_position * _sampleRate * 0.001));
        }
    }

    // other parameters

    /** The maximum limit of sound tracks. @default 128 */
    public int getMaxTrackCount() {
        return sequencer._maxTrackCount;
    }

    public void setMaxTrackCount(int max) {
        sequencer._maxTrackCount = max;
    }

    /** Beat par minute value of SiON's play. @default 120 */
    public double getBpm() {
        return (sequencer.isReadyToProcess()) ? sequencer._bpm.getBpm() : sequencer.setting.defaultBPM;
    }

    public void setBpm(double t) {
        sequencer.setting.defaultBPM = t;
        if (sequencer.isReadyToProcess()) {
            if (!sequencer.isEnableChangeBPM()) throw errorCannotChangeBPM();
            sequencer._bpm._bpm = t;
        }
    }

    /** Auto stop when the sequence finished or fade-outed. @default false */
    public boolean getAutoStop() {
        return _autoStop;
    }

    public void setAutoStop(boolean mode) {
        _autoStop = mode;
    }

    /** pause while loading sound @default true */
    public boolean getPauseWhileLoading() {
        return _suspendWhileLoading;
    }

    public void setPauseWhileLoading(boolean b) {
        _suspendWhileLoading = b;
    }

    /** Debug mode, true; throw Error / false; throw ErrorEvent when error appears inside. @default false */
    public boolean getDebugMode() {
        return _debugMode;
    }

    public void setDebugMode(boolean mode) {
        _debugMode = mode;
    }

    /**
     * Note on exception mode, this mode instanceof refered when the noteOn() sound's track IDs are conflicted at the same moment. This value have to be SiONDriver.NEM_*. @default NEM_IGNORE.
     *
     * @see #NEM_IGNORE
     * @see #NEM_REJECT
     * @see #NEM_OVERWRITE
     * @see #NEM_SHIFT
     */
    public int getNoteOnExceptionMode() {
        return _noteOnExceptionMode;
    }

    public void setNoteOnExceptionMode(int mode) {
        _noteOnExceptionMode = (0 < mode && mode < NEM_MAX) ? mode : 0;
    }

    /** dispatch CHANGE_BPM Event When position changed @default true */
    public boolean getDispatchChangeBPMEventWhenPositionChanged() {
        return _dispatchChangeBPMEventWhenPositionChanged;
    }

    public void setDispatchChangeBPMEventWhenPositionChanged(boolean b) {
        _dispatchChangeBPMEventWhenPositionChanged = b;
    }

    /** Allow plural drivers <b>[CAUTION] This function instanceof quite experimental</b> and plural drivers require large memory area. */
    public void setAllowPluralDrivers(boolean b) {
        _allowPluralDrivers = b;
    }

    public boolean getAllowPluralDrivers() {
        return _allowPluralDrivers;
    }

    // constructor
    //

    /**
     * Create driver to manage the synthesizer, sequencer and effector. Only one SiONDriver instance can be created.
     *
     * @param bufferLength Buffer size of sound stream. The value of 8192, 4096 or 2048 instanceof available.
     * @param channelCount Channel count. 1(monaural) or 2(stereo) is available.
     * @param sampleRate   Sampling ratio of wave. 44100 instanceof only available in current version.
     * @param bitRate      Bit ratio of wave. 0 means float value [-1 to 1].
     */
    public SiONDriver(int bufferLength /* = 2048 */, int channelCount /* = 2 */, int sampleRate /* = 44100 */, int bitRate /* = 0 */) {
        // check mutex
        if (_mutex != null && !_allowPluralDrivers) throw errorPluralDrivers();

        // check parameters
        if (bufferLength != 2048 && bufferLength != 4096 && bufferLength != 8192)
            throw errorParamNotAvailable("stream buffer", bufferLength);
        if (channelCount != 1 && channelCount != 2) throw errorParamNotAvailable("channel count", channelCount);
        if (sampleRate != 44100) throw errorParamNotAvailable("sampling rate", sampleRate);

        // initialize tables
        Object dummy;
        dummy = SiOPMTable.getInstance(); //initialize(3580000, 1789772.5, 44100) sampleRate;
        dummy = SiMMLTable.getInstance(); //initialize();

        // allocation
        _jobQueue = new ArrayList<SiONDriverJob>();
        module = new SiOPMModule();
        effector = new SiEffectModule(module);
        sequencer = new SiMMLSequencer(module, this::_callbackEventTriggerOn, this::_callbackEventTriggerOff, this::_callbackTempoChanged);
        // _sound = new Sound();
        // _soundTransform = new SoundTransform();
        _fader = new Fader();
        _timerSequence = new MMLSequence(false);
        _loadingSoundList = new Object[0];
        _midiModule = new MIDIModule(16, 16, "gm");
        _midiConverter = new SiONDataConverterSMF(null, _midiModule);

        // initialize
        _tempData = null;
        _channelCount = channelCount;
        _sampleRate = sampleRate; // sampleRate; 44100 is only in current version.
        _bitRate = bitRate;
        _bufferLength = bufferLength;
        _listenEvent = NO_LISTEN;
        _dispatchStreamEvent = false;
        _dispatchFadingEvent = false;
        _preserveStop = false;
        _inStreaming = false;
        _suspendStreaming = false;
        _suspendWhileLoading = true;
        _autoStop = false;
        _noteOnExceptionMode = NEM_IGNORE;
        _debugMode = false;
        _isFinishSeqDispatched = false;
        _dispatchChangeBPMEventWhenPositionChanged = true;
        _timerCallback = null;
        _timerSequence.initialize();
        _timerSequence.appendNewEvent(MMLEvent.REPEAT_ALL, 0, 0);
        _timerSequence.appendNewEvent(MMLEvent.TIMER, 0, 0);
        _timerIntervalEvent = _timerSequence.appendNewEvent(MMLEvent.GLOBAL_WAIT, 0, 0);

        _backgroundSound = null;
        _backgroundLoopPoint = -1;
        _backgroundFadeInFrames = 0;
        _backgroundFadeOutFrames = 0;
        _backgroundFadeGapFrames = 0;
        _backgroundTotalFadeFrames = 0;
        _backgroundVoice = new SiONVoice(); // SiMMLTable.MT_SAMPLE
        _backgroundVoice.updateVolumes = true;
        _backgroundSample = null;
        _backgroundTrack = null;
        _backgroundTrackFadeOut = null;

        _position = 0;
        _masterVolume = 1;
        _faderVolume = 1;
        _masterVolume = 1;
        _faderVolume = 1;
        // _soundTransform.pan = 0;
        // _soundTransform.volume = _masterVolume * _faderVolume;

        _eventListenerPrior = 1;
        _trackEventQueue = new ArrayList<SiONTrackEvent>();

        _queueInterval = 500;
        _jobProgress = 0;
        _currentJob = 0;
        _queueLength = 0;

        _timeCompile = 0;
        _timeProcessTotal = 0;
        _timeProcessData = SLLint.allocRing(TIME_AVARAGING_COUNT, 0);
        _timeProcessAveRatio = _sampleRate / (_bufferLength * TIME_AVARAGING_COUNT);
        _timePrevStream = 0;
        _latency = 0;
        _prevFrameTime = 0;
        _frameRate = 1;

        _mmlString = null;
        _data = null;
        // _soundChannel = null;

        // register sound streaming function 
        // _sound.addEventListener("sampleData", _streaming);

        // set mutex
        _mutex = this;
    }

    // interfaces for data preparation
    //

    /**
     * Compile MML string to SiONData.
     *
     * @param mml  MML string to compile.
     * @param data SiONData to compile. The SiONDriver creates new SiONData instance when this argument instanceof null.
     * @return Compiled data.
     */
    public SiONData compile(String mml, SiONData data) {
        try {
            // stop sound
            stop();

            // compile immediately
            int t = (int)System.currentTimeMillis();
            _prepareCompile(mml, data);
            _jobProgress = sequencer.compile(0);
            _timeCompile = (int)System.currentTimeMillis() - t;
            _mmlString = null;
        } catch (Exception e) {
            // error
            if (_debugMode) throw e;
            else { e.printStackTrace(); }
            // else dispatchEvent(new ErrorEvent("error", false, false, e.message));
        }

        return _data;
    }

    /**
     * Push queue job to compile MML string. Start compiling after calling startQueue.<br/>
     *
     * @param mml  MML string to compile.
     * @param data SiONData to compile.
     * @return Queue length.
     * @see #startQueue
     */
    public int compileQueue(String mml, SiONData data) {
        if (mml == null || data == null) return _jobQueue.size();
        _jobQueue.add(new SiONDriverJob(mml, null, data, 2, false));
        return _jobQueue.size();
    }

    // interfaces for sound rendering
    //

    /**
     * Render wave data from MML string or SiONData. This method may take long time, please consider the using renderQueue() instead.
     *
     * @param data                     SiONData or mml String to play.
     * @param renderBuffer             Rendering target. null to create new buffer. The length of this argument limits the rendering length (except for 0).
     * @param renderBufferChannelCount Channel count of renderBuffer. 2 for stereo and 1 for monaural.
     * @param resetEffector            reset all effectors before play data.
     * @return rendered wave ((Vector) data).&lt;Number&gt;.
     */
    public double[] render(Object data, double[] renderBuffer, int renderBufferChannelCount, boolean resetEffector) {
        try {
            // stop sound
            stop();

            // rendering immediately
            int t = (int)System.currentTimeMillis();
            _prepareRender(data, renderBuffer, renderBufferChannelCount, resetEffector);
            while (true) {
                if (_rendering()) break;
            }
            _timeRender = (int)System.currentTimeMillis() - t;
        } catch (Exception e) {
            // error
            _removeAllEventListeners();
            if (_debugMode) throw e;
            else { e.printStackTrace(); }
            // else dispatchEvent(new ErrorEvent("error", false, false, e.message));
        }

        return _renderBuffer;
    }

    /**
     * Push queue job to render sound. Start rendering after calling startQueue.<br/>
     *
     * @param data                     SiONData or mml String to render.
     * @param renderBuffer             Rendering target. The length of renderBuffer limits rendering length except for 0.
     * @param renderBufferChannelCount Channel count of renderBuffer. 2 for stereo and 1 for monaural.
     * @return Queue length.
     * @see #startQueue
     */
    public int renderQueue(Object data, double[] renderBuffer, int renderBufferChannelCount, boolean resetEffector) {
        if (data == null || renderBuffer == null) return _jobQueue.size();

        if (data instanceof String) {
            SiONData compiled = new SiONData();
            _jobQueue.add(new SiONDriverJob(((String) data), null, compiled, 2, false));
            _jobQueue.add(new SiONDriverJob(null, renderBuffer, compiled, renderBufferChannelCount, resetEffector));
            return _jobQueue.size();
        } else if (data instanceof SiONData) {
            _jobQueue.add(new SiONDriverJob(null, renderBuffer, ((SiONData) data), renderBufferChannelCount, resetEffector));
            return _jobQueue.size();
        }

        RuntimeException e = errorDataIncorrect();
        if (_debugMode) throw e;
        else { e.printStackTrace(); }
        // else dispatchEvent(new ErrorEvent("error", false, false, e.message));
        return _jobQueue.size();
    }

    // interfaces for jobs queue
    //

    /**
     * Execute all elements queued by compileQueue() and renderQueue().
     * After calling this function, the SiONEvent.QUEUE_PROGRESS, SiONEvent.QUEUE_COMPLETE and "error" events will be dispatched.<br/>
     * The SiONEvent.QUEUE_PROGRESS instanceof dispatched when it's executing queued job.<br/>
     * The SiONEvent.QUEUE_COMPLETE instanceof dispatched when finish all queued jobs.<br/>
     * The "error" instanceof dispatched when some error appears during the compile.<br/>
     *
     * @param interval Interupting interval
     * @return Queue length.
     * @see #compileQueue
     * @see #renderQueue
     */
    public int startQueue(int interval) {
        try {
            stop();
            _queueLength = _jobQueue.size();
            if (!_jobQueue.isEmpty()) {
                _queueInterval = interval;
                _executeNextJob();
                _queue_addAllEventListeners();
            }
        } catch (Exception e) {
            // error
            _removeAllEventListeners();
            _cancelAllJobs();
            if (_debugMode) throw e;
            else { e.printStackTrace(); }
            // else dispatchEvent(new ErrorEvent("error", false, false, e.message));
        }
        return _queueLength;
    }

    /**
     * Listen loading status of flash.media.Sound instance.
     * When SiONDriver.pauseWhileLoading instanceof true, SiONDriver starts streaming after all Sound instances passed by this function are loaded.
     *
     * @param sound Sound or SoundLoader instance to listen
     * @param prior listening priority
     * @return return false when the sound instanceof loaded already.
     * @see SiONDriver#getPauseWhileLoading
     * @see SiONDriver##clearLoadingSoundList
     */
    public boolean listenSoundLoadingStatus(Object sound, int prior) {
        return true;
    }

    /**
     * Clear all listening sound list registered by SiONDriver.listenLoadingStatus().
     */
    public void clearSoundLoadingList() {
        _loadingSoundList = new Object[0];
    }

    /**
     * Set hash table of Sound instance referred from #SAMPLER and #PCMWAVE commands. You have to set this table BEFORE compile mml.
     */
    public void setSoundReferenceTable(Object soundReferenceTable) {
        SiOPMTable.getInstance().soundReference = soundReferenceTable != null ? soundReferenceTable : new Object();
    }

    // interfaces for sound streaming
    //

    /**
     * Play SiONData or MML string.
     *
     * @param data          SiONData, mml String, Sound object, mp3 file URLRequest or SMFData object to play. You can pass null when resume after pause or streaming without any data.
     * @param resetEffector reset all effectors before play data.
     * @return SoundChannel instance to play data. This instance instanceof ((soundChannel) same) property.
     * @see #_soundChannel
     */
    // public Object play(Object data, boolean resetEffector) {
    public Object play(Object data /* = null */, boolean resetEffector /* = true */) {
        try {
            if (_isPaused) {
                _isPaused = false;
            } else {
                // stop sound
                stop();

                // preparation
                _prepareProcess(data, resetEffector);

                // initialize
                _timeProcessTotal = 0;
                for (int i = 0; i < TIME_AVARAGING_COUNT; i++) {
                    _timeProcessData.i = 0;
                    _timeProcessData = _timeProcessData.next;
                }
                _isPaused = false;
                _isFinishSeqDispatched = (data == null);

                // start streaming
                _suspendStreaming = true;
                // _soundChannel = _sound.play();
                // _soundChannel.soundTransform = _soundTransform;
                _process_addAllEventListeners();
            }
        } catch (Exception e) {
            // error
            if (_debugMode) throw e;
            else { e.printStackTrace(); }
            // else dispatchEvent(new ErrorEvent("error", false, false, e.message));
        }

        // return _soundChannel;
        return null;
    }

    /** Stop sound. */
    public void stop() {
        if (_soundChannel != null) {
            if (_inStreaming) {
                _preserveStop = true;
            } else {
                stopBackgroundSound();
                _removeAllEventListeners();
                _preserveStop = false;
                _soundChannel.stop();
                _soundChannel = null;
                _latency = 0;
                _fader.stop();
                _faderVolume = 1;
                _isPaused = false;
                _soundTransform.volume = _masterVolume;
                sequencer._stopSequence();

                // dispatch streaming stop event
                // dispatchEvent(new SiONEvent(SiONEvent.STREAM_STOP, this));
            }
        }
    }

    /** Reset signal processor. The effector and sequencer will not be reset. If you want to reset all, call SiONDriver.stop() instead. */
    public void reset() {
        sequencer._resetAllTracks();
    }

    /** Pause sound. You can resume it by resume() or play(). @see resume() @see play() */
    public void pause() {
        _isPaused = true;
    }

    /** Resume sound. ((play) same)() after pause(). @see pause() */
    public void resume() {
        _isPaused = false;
    }

    public void setBackgroundSound(Sound sound) {
        setBackgroundSound(sound, 0.5, -1);
    }

    /**
     * Play ((a) Sound) background.
     *
     * @param sound     Sound instance to play background.
     * @param mixLevel  Mixing level (0-1), this value ((backgroundSoundVolume) same).
     * @param loopPoint loop point in second. -1 sets no loop
     * @see #_setBackgroundSound
     */
    public void setBackgroundSound(Sound sound, double mixLevel, double loopPoint) {
        // backgroundSoundVolume = mixLevel;
        _backgroundLoopPoint = loopPoint;
        _setBackgroundSound(sound);
    }

    /** Stop background sound. */
    public void stopBackgroundSound() {
        _setBackgroundSound(null);
    }

    /**
     * set fading time of background sound
     *
     * @param fadeInTime  fade in time [sec]. positive value only
     * @param fadeOutTime fade out time [sec]. positive value only
     * @param gapTime     gap between 2 sound [sec]. You can specify negative values to play with cross fading.
     */
    public void setBackgroundSoundFadeTime(double fadeInTime, double fadeOutTime, double gapTime) {
        double t2f = _sampleRate / _bufferLength;
        _backgroundFadeInFrames = (int)(fadeInTime * t2f);
        _backgroundFadeOutFrames = (int)(fadeOutTime * t2f);
        _backgroundFadeGapFrames = (int)(gapTime * t2f);
        _backgroundTotalFadeFrames = _backgroundFadeOutFrames + _backgroundFadeInFrames + _backgroundFadeGapFrames;
    }

    /**
     * Fade in all sound played by SiONDriver. You can set this method before calling play().
     *
     * @param time Fading time [second].
     */
    public void fadeIn(double time) {
        _fader.setFade(this::_fadeVolume, (double) 0, 1.0, (int) (time * _sampleRate / _bufferLength));
        _dispatchFadingEvent = false; // (hasEventListener(SiONEvent.FADE_PROGRESS));
    }

    /**
     * Fade out all sound played by SiONDriver.
     *
     * @param time Fading time [second].
     */
    public void fadeOut(double time) {
        _fader.setFade(this::_fadeVolume, 1.0, (double) 0, (int) (time * _sampleRate / _bufferLength));
        _dispatchFadingEvent = false; // (hasEventListener(SiONEvent.FADE_PROGRESS));
    }

    /**
     * Set timer interruption.
     *
     * @param length16th Interupting interval in 16th beat.
     * @param callback   Callback function. the Type instanceof function():void.
     */
    public void setTimerInterruption(double length16th, Runnable callback) {
        _timerIntervalEvent.length = (int)(length16th * sequencer.setting.resolution * 0.0625);
        _timerCallback = (length16th > 0) ? callback : null;
    }

    /**
     * Set callback interval of SiONTrackEvent.BEAT.
     *
     * @param length16th Interval in 16th beat. 2^n instanceof only available(1,2,4,8,16....).
     */
    public void setBeatCallbackInterval(double length16th) {
        int filter = 1;
        while (length16th > 1.5) {
            filter <<= 1;
            length16th *= 0.5;
        }
        sequencer._setBeatCallbackFilter(filter - 1);
    }

    /**
     * Force dispatch stream event. The SiONEvent.STREAM instanceof dispatched only when the event listener instanceof set BEFORE calling play(). You can let SiONDriver to dispatch SiONEvent.STREAM event by this function.
     *
     * @param dispatch Set true to force dispatching. Or set false to not dispatching if there are no listeners.
     */
    public void forceDispatchStreamEvent(boolean dispatch) {
        _dispatchStreamEvent = dispatch || (hasEventListener(SiONEvent.STREAM));
    }

    // Interface for public data registration
    //

    /**
     * Set wave table data refered by %4.
     *
     * @param index wave table number.
     * @param table wave shape vector ranges in -1 to 1.
     */
    public SiOPMWaveTable setWaveTable(int index, double[] table) {
        int len;
        int bits = -1;
        for (len = table.length; len > 0; len >>= 1) bits++;
        if (bits < 2) return null;
        int[] waveTable = SiONUtil.logTransVector(table, 1, null, 0, true);
        waveTable = new int[1 << bits];
        return SiOPMTable._instance.registerWaveTable(index, waveTable);
    }

    /**
     * Set PCM wave data ordered by %7.
     *
     * @param index           PCM data number.
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instance instanceof extracted internally, the maximum length to extract instanceof SiOPMWavePCMData.maxSampleLengthFromSound[samples].
     * @param samplingNote    Sampling wave's original note number, this allows decimal number
     * @param keyRangeFrom    Assigning key range starts from (not implemented in current version)
     * @param keyRangeTo      Assigning key range ends at (not implemented in current version)
     * @param srcChannelCount channel count of source data, 1 for monaural, 2 for stereo.
     * @param channelCount    channel count of this data, 1 for monaural, 2 for stereo, 0 sets same with srcChannelCount.
     * @see SiOPMWavePCMData#maxSampleLengthFromSound
     * @see #render
     */
    public SiOPMWavePCMData setPCMWave(int index, Object data, double samplingNote, int keyRangeFrom, int keyRangeTo, int srcChannelCount, int channelCount) {
        SiMMLVoice pcmVoice = SiOPMTable._instance._getGlobalPCMVoice(index & (SiOPMTable.PCM_DATA_MAX - 1));
        SiOPMWavePCMTable pcmTable = (SiOPMWavePCMTable) pcmVoice.waveData;
        return pcmTable.setSample(new SiOPMWavePCMData(data, (int) (samplingNote * 64), srcChannelCount, channelCount), keyRangeFrom, keyRangeTo);
    }

    /**
     * Set sampler wave data referred by %10.
     *
     * @param index           note number. 0-127 for bank0, 128-255 for bank1.
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instanceof extracted when the length instanceof shorter than SiOPMWaveSamplerData.extractThreshold[msec].
     * @param ignoreNoteOff   True to set ignoring note off.
     * @param pan             pan of this sample [-64 - 64].
     * @param srcChannelCount channel count of source data, 1 for monaural, 2 for stereo.
     * @param channelCount    channel count of this data, 1 for monaural, 2 for stereo, 0 sets same with srcChannelCount.
     * @return created data instance
     * @see SiOPMWaveSamplerData#extractThreshold
     * @see #render
     */
    @Override
    public SiOPMWaveSamplerData setSamplerWave(int index, Object data, boolean ignoreNoteOff, int pan, int srcChannelCount, int channelCount) {
        return SiOPMTable._instance.registerSamplerData(index, data, ignoreNoteOff, pan, srcChannelCount, channelCount);
    }

    /**
     * Set pcm voice
     *
     * @param index PCM data number.
     * @param voice pcm voice to set, ussualy from SiONSoundFont
     * @see SiONSoundFont
     */
    public void setPCMVoice(int index, SiONVoice voice) {
        SiOPMTable._instance._setGlobalPCMVoice(index & (SiOPMTable.PCM_DATA_MAX - 1), voice);
    }

    /**
     * Set sampler table
     *
     * @param bank  bank number
     * @param table sampler table class, ussualy from SiONSoundFont
     * @see SiONSoundFont
     */
    public void setSamplerTable(int bank, SiOPMWaveSamplerTable table) {
        SiOPMTable._instance.samplerTables[bank & (SiOPMTable.SAMPLER_TABLE_MAX - 1)] = table;
    }

    /** [NOT RECOMMENDED] This function instanceof for a compatibility with previous versions, please use setPCMWave instead of this function. @see #setPCMWave(). */
    public SiOPMWavePCMData setPCMData(int index, double[] data, int samplingOctave, int keyRangeFrom, int keyRangeTo, boolean isSourceDataStereo) {
        return setPCMWave(index, data, samplingOctave * 12 + 9, keyRangeFrom, keyRangeTo, (isSourceDataStereo) ? 2 : 1, 0);
    }

    /** [NOT RECOMMENDED] This function instanceof for a compatibility with previous versions, please use setPCMWave instead of this function. @see #setPCMWave(). */
    public SiOPMWavePCMData setPCMSound(int index, Sound sound, int samplingOctave, int keyRangeFrom, int keyRangeTo) {
        return setPCMWave(index, sound, samplingOctave * 12 + 9, keyRangeFrom, keyRangeTo, 1, 0);
    }

    /** [NOT RECOMMENDED] This function instanceof for a compatibility with previous versions, please use setSamplerWave instead of this function. @see #setSamplerWave(). */
    public SiOPMWaveSamplerData setSamplerData(int index, double[] data, boolean ignoreNoteOff, int channelCount) {
        return setSamplerWave(index, data, ignoreNoteOff, 0, channelCount, 0);
    }

    /** [NOT RECOMMENDED] This function instanceof for a compatibility with previous versions, please use setSamplerWave instead of this function. @see #setSamplerWave(). */
    public SiOPMWaveSamplerData setSamplerSound(int index, Sound sound, boolean ignoreNoteOff, int channelCount) {
        return setSamplerWave(index, sound, ignoreNoteOff, 0, channelCount, 0);
    }

    /**
     * Set envelop table data refered by &#64;&#64;,na,np,nt,nf,_&#64;&#64;,_na,_np,_nt and _nf.
     *
     * @param index     envelop table number.
     * @param table     envelop table vector.
     * @param loopPoint returning point index of looping. -1 sets no loop.
     */
    public void setEnvelopTable(int index, int[] table, int loopPoint) {
        SiMMLTable.registerMasterEnvelopTable(index, new SiMMLEnvelopTable(table, loopPoint));
    }

    /**
     * Set wave table data refered by %6.
     *
     * @param index wave table number.
     * @param voice voice to register.
     */
    public void setVoice(int index, SiONVoice voice) {
        if (!voice.isSuitableForFMVoice()) throw errorNotGoodFMVoice();
        SiMMLTable.registerMasterVoice(index, voice);
    }

    /**
     * Clear all of WaveTables, FM Voices, EnvelopTables, Sampler waves and PCM waves.
     *
     * @see #setWaveTable
     * @see #setVoice
     * @see #setEnvelopTable
     * @see #setSamplerWave
     * @see #setPCMWave
     */
    public void clearAllUserTables() {
        SiOPMTable.getInstance().resetAllUserTables();
        SiMMLTable.getInstance().resetAllUserTables();
    }

    // Interface for interactivity
    //

    /**
     * Play sound registered in sampler table (registered by setSamplerData()), ((noteOn) same)(note, new SiONVoice(10), ...).
     *
     * @param sampleNumber sample number [0-127].
     * @param length       note length in 16th beat. 0 sets no note off, this means you should call noteOff().
     * @param delay        note on delay units in 16th beat.
     * @param quant        quantize in 16th beat. 0 sets no quantization. 4 sets quantization by 4th beat.
     * @param trackID      new tracks id (0-65535).
     * @param isDisposable use disposable track. The disposable track will free automatically when finished rendering.
     *                     This means you should not keep a dieposable track in your code perpetually.
     *                     If you want to keep track, set this argument false. And after using, SiMMLTrack::setDisposal() to disposed by system.<br/>
     *                     [REMARKS] Not disposable track instanceof kept perpetually in the system while streaming, this may causes critical performance loss.
     * @return SiMMLTrack to play the note.
     */
    public SiMMLTrack playSound(int sampleNumber, double length, double delay, double quant, int trackID, boolean isDisposable) {
        int internalTrackID = (trackID & SiMMLTrack.TRACK_ID_FILTER) | SiMMLTrack.DRIVER_NOTE;
        SiMMLTrack mmlTrack = null;
        double delaySamples = sequencer.calcSampleDelay(0, delay, quant);

        // check track id exception
        if (_noteOnExceptionMode != NEM_IGNORE) {
            // find a track sounds at same timing
            mmlTrack = sequencer._findActiveTrack(internalTrackID, (int)delaySamples);
            if (_noteOnExceptionMode == NEM_REJECT && mmlTrack != null) return null; // reject
            else if (_noteOnExceptionMode == NEM_SHIFT) { // shift timing
                int step = (int) sequencer.calcSampleLength(quant);
                while (mmlTrack != null) {
                    delaySamples += step;
                    mmlTrack = sequencer._findActiveTrack(internalTrackID, (int)delaySamples);
                }
            }
        }

        if (mmlTrack == null) mmlTrack = sequencer._newControlableTrack(internalTrackID, isDisposable);
        if (mmlTrack != null) {
            mmlTrack.setChannelModuleType(10, 0, 0);
            mmlTrack.keyOn(sampleNumber, (int)(length * sequencer.setting.resolution * 0.0625), (int)delaySamples);
        }
        return mmlTrack;
    }

    /**
     * Note on. This function only instanceof available after play(). The NOTE_ON_STREAM event instanceof dispatched inside.
     *
     * @param note         note number [0-127].
     * @param voice        SiONVoice to play note. You can specify null, but it sets only a default square wave.
     * @param length       note length in 16th beat. 0 sets no note off, this means you should call noteOff().
     * @param delay        note on delay units in 16th beat.
     * @param quant        quantize in 16th beat. 0 sets no quantization. 4 sets quantization by 4th beat.
     * @param trackID      new tracks id (0-65535).
     * @param isDisposable use disposable track. The disposable track will free automatically when finished rendering.
     *                     This means you should not keep a dieposable track in your code perpetually.
     *                     If you want to keep track, set this argument false. And after using, call SiMMLTrack::setDisposal() to disposed by system.<br/>
     *                     [REMARKS] Not disposable track instanceof kept in the system perpetually while streaming, this may causes critical performance loss.
     * @return SiMMLTrack to play the note.
     */
    public SiMMLTrack noteOn(int note, SiONVoice voice, double length, double delay, double quant, int trackID, boolean isDisposable) {
        int internalTrackID = (trackID & SiMMLTrack.TRACK_ID_FILTER) | SiMMLTrack.DRIVER_NOTE;
        SiMMLTrack mmlTrack = null;
        double delaySamples = sequencer.calcSampleDelay(0, delay, quant);

        // check track id exception
        if (_noteOnExceptionMode != NEM_IGNORE) {
            // find a track sounds at same timing
            mmlTrack = sequencer._findActiveTrack(internalTrackID, (int)delaySamples);
            if (_noteOnExceptionMode == NEM_REJECT && mmlTrack != null) return null; // reject
            else if (_noteOnExceptionMode == NEM_SHIFT) { // shift timing
                int step = (int) sequencer.calcSampleLength(quant);
                while (mmlTrack != null) {
                    delaySamples += step;
                    mmlTrack = sequencer._findActiveTrack(internalTrackID, (int)delaySamples);
                }
            }
        }

        if (mmlTrack == null) mmlTrack = sequencer._newControlableTrack(internalTrackID, isDisposable);
        if (mmlTrack != null) {
            if (voice != null) voice.updateTrackVoice(mmlTrack);
            mmlTrack.keyOn(note, (int)(length * sequencer.setting.resolution * 0.0625), (int)delaySamples);
        }
        return mmlTrack;
    }

    /**
     * Note off. This function only instanceof available after play(). The NOTE_OFF_STREAM event instanceof dispatched inside.
     *
     * @param note            note number [-1-127]. The value of -1 ignores note number.
     * @param trackID         track id to note off.
     * @param delay           note off delay units in 16th beat.
     * @param quant           quantize in 16th beat. 0 sets no quantization. 4 sets quantization by 4th beat.
     * @param stopImmediately stop sound with reseting channel's process
     * @return All SiMMLTracks switched key off.
     */
    public List<SiMMLTrack> noteOff(int note, int trackID, double delay, double quant, boolean stopImmediately) {
        int internalTrackID = (trackID & SiMMLTrack.TRACK_ID_FILTER) | SiMMLTrack.DRIVER_NOTE;
        int delaySamples = (int)sequencer.calcSampleDelay(0, delay, quant);
        int n;
        List<SiMMLTrack> tracks = new ArrayList<SiMMLTrack>();
        for (SiMMLTrack mmlTrack : sequencer.tracks) {
            if (mmlTrack.getInternalTrackID() == internalTrackID) {
                if (note == -1 || (note == mmlTrack.getNote() && mmlTrack.channel.isNoteOn())) {
                    mmlTrack.keyOff(delaySamples, stopImmediately);
                    tracks.add(mmlTrack);
                } else if (mmlTrack.executor.getNoteWaitingFor() == note) {
                    // if this track is waiting for starting sound ...
                    mmlTrack.keyOn(note, 1, delaySamples);
                    tracks.add(mmlTrack);
                }
            }
        }
        return tracks;
    }

    /**
     * Play sequences with synchronizing. This function only instanceof available after play().
     *
     * @param data         The SiONData including sequences. This data instanceof used only for sequences. The system ignores wave, envelop and voice data.
     * @param voice        SiONVoice to play sequence. The voice setting in the sequence has priority.
     * @param length       note length in 16th beat. 0 sets no note off, this means you should call noteOff().
     * @param delay        note on delay units in 16th beat.
     * @param quant        quantize in 16th beat. 0 sets no quantization. 4 sets quantization by 4th beat.
     * @param trackID      new tracks id (0-65535).
     * @param isDisposable use disposable track. The disposable track will free automatically when finished rendering.
     *                     This means you should not keep a dieposable track in your code perpetually.
     *                     If you want to keep track, set this argument false. And after using, call SiMMLTrack::setDisposal() to disposed by system.<br/>
     *                     [REMARKS] Not disposable track instanceof kept in the system perpetually while streaming, this may causes critical performance loss.
     * @return list of SiMMLTracks to play sequence.
     */
    public List<SiMMLTrack> sequenceOn(SiONData data, SiONVoice voice, double length, double delay, double quant, int trackID, boolean isDisposable) {
        int internalTrackID = (trackID & SiMMLTrack.TRACK_ID_FILTER) | SiMMLTrack.DRIVER_SEQUENCE;
        SiMMLTrack mmlTrack;
        List<SiMMLTrack> tracks = new ArrayList<SiMMLTrack>();
        MMLSequence seq = data.sequenceGroup.getHeadSequence();
        int delaySamples = (int)sequencer.calcSampleDelay(0, delay, quant);
        int lengthSamples = (int)sequencer.calcSampleLength(length);

        // create new sequence tracks
        while (seq != null) {
            if (seq.isActive) {
                mmlTrack = sequencer._newControlableTrack(internalTrackID, isDisposable);
                mmlTrack.sequenceOn(seq, lengthSamples, delaySamples);
                if (voice != null) voice.updateTrackVoice(mmlTrack);
                tracks.add(mmlTrack);
            }
            seq = seq.getNextSequence();
        }
        return tracks;
    }

    /**
     * Stop the sequences with synchronizing. This function only instanceof available after play().
     *
     * @param trackID       tracks id to stop.
     * @param delay         sequence off delay units in 16th beat.
     * @param quant         quantize in 16th beat. 0 sets no quantization. 4 sets quantization by 4th beat.
     * @param stopWithReset stop sound with reseting channel's process
     * @return list of SiMMLTracks stopped to play sequence.
     */
    public List<SiMMLTrack> sequenceOff(int trackID, double delay, double quant, boolean stopWithReset) {
        int internalTrackID = (trackID & SiMMLTrack.TRACK_ID_FILTER) | SiMMLTrack.DRIVER_SEQUENCE;
        int delaySamples = (int)sequencer.calcSampleDelay(0, delay, quant);
        SiMMLTrack stoppedTrack = null;
        List<SiMMLTrack> tracks = new ArrayList<SiMMLTrack>();
        for (SiMMLTrack mmlTrack : sequencer.tracks) {
            if (mmlTrack.getInternalTrackID() == internalTrackID) {
                mmlTrack.sequenceOff(delaySamples, stopWithReset);
                tracks.add(mmlTrack);
            }
        }
        return tracks;
    }

    /**
     * Create new user controllable track. This function only instanceof available after play().
     *
     * @return new user controllable track. This track instanceof NOT disposable.
     * @trackID new user controllable track's ID.
     */
    public SiMMLTrack newUserControlableTrack(int trackID) {
        int internalTrackID = (trackID & SiMMLTrack.TRACK_ID_FILTER) | SiMMLTrack.USER_CONTROLLED;
        return sequencer._newControlableTrack(internalTrackID, false);
    }

    /**
     * dispatch SiONTrackEvent.USER_DEFINED event with latency delay
     *
     * @param eventTriggerID SiONTrackEvent.eventTriggerID
     * @param note           SiONTrackEvent.note
     */
    public void dispatchUserDefinedTrackEvent(int eventTriggerID, int note) {
        SiONTrackEvent event = new SiONTrackEvent(SiONTrackEvent.USER_DEFINED, this, null, sequencer.getStreamWritingPositionResidue(), note, eventTriggerID);
        _trackEventQueue.add(event);
    }

    //
    // Internal uses
    //

    // callback for event trigger
    //

    // call back when sound streaming
    private boolean _callbackEventTriggerOn(SiMMLTrack track) {
        return _publishEventTrigger(track, track.getEventTriggerTypeOn(), SiONTrackEvent.NOTE_ON_FRAME, SiONTrackEvent.NOTE_ON_STREAM);
    }

    // call back when sound streaming
    private boolean _callbackEventTriggerOff(SiMMLTrack track) {
        return _publishEventTrigger(track, track.getEventTriggerTypeOff(), SiONTrackEvent.NOTE_OFF_FRAME, SiONTrackEvent.NOTE_OFF_STREAM);
    }

    // publish event trigger
    private boolean _publishEventTrigger(SiMMLTrack track, int type, String frameEvent, String streamEvent) {
        SiONTrackEvent event;
        if ((type & 1) != 0) { // frame event. dispatch later
            event = new SiONTrackEvent(frameEvent, this, track);
            _trackEventQueue.add(event);
        }
        if ((type & 2) != 0) { // sound event. dispatch immediately
            event = new SiONTrackEvent(streamEvent, this, track);
            // dispatchEvent(event);
            return true; // !(event.isDefaultPrevented());
        }
        return true;
    }

    // call back when tempo changed
    private void _callbackTempoChanged(int bufferIndex, boolean isDummy) {
        if (isDummy && _dispatchChangeBPMEventWhenPositionChanged) {
            // dispatchEvent(new SiONTrackEvent(SiONTrackEvent.CHANGE_BPM, this, null, bufferIndex));
        } else {
            SiONTrackEvent event = new SiONTrackEvent(SiONTrackEvent.CHANGE_BPM, this, null, bufferIndex);
            _trackEventQueue.add(event);
        }
    }

    // call back on beat
    private void _callbackBeat(int bufferIndex, int beatCounter) {
        SiONTrackEvent event = new SiONTrackEvent(SiONTrackEvent.BEAT, this, null, bufferIndex, 0, beatCounter);
        _trackEventQueue.add(event);
    }

    // operate event listener
    //

    // add all event listeners
    private void _queue_addAllEventListeners() {
        if (_listenEvent != NO_LISTEN) throw errorDriverBusy(LISTEN_QUEUE);
        // addEventListener(Event.ENTER_FRAME, _queue_onEnterFrame, false, _eventListenerPrior);
        _listenEvent = LISTEN_QUEUE;
    }

    // add all event listeners
    private void _process_addAllEventListeners() {
        if (_listenEvent != NO_LISTEN) throw errorDriverBusy(LISTEN_PROCESS);
        // addEventListener(Event.ENTER_FRAME, _process_onEnterFrame, false, _eventListenerPrior);
        /*
        if (hasEventListener(SiONTrackEvent.BEAT)) sequencer._setBeatCallback(_callbackBeat);
        else sequencer._setBeatCallback(null);
        _dispatchStreamEvent = (hasEventListener(SiONEvent.STREAM));
        _prevFrameTime = SiONUtil.getTimer();
        */
        _listenEvent = LISTEN_PROCESS;
    }

    // remove all event listeners
    private void _removeAllEventListeners() {
        switch (_listenEvent) {
            case LISTEN_QUEUE:
                // removeEventListener(Event.ENTER_FRAME, _queue_onEnterFrame);
                break;
            case LISTEN_PROCESS:
                // removeEventListener(Event.ENTER_FRAME, _process_onEnterFrame);
                sequencer._setBeatCallback(null);
                _dispatchStreamEvent = false;
                break;
        }
        _listenEvent = NO_LISTEN;
    }

    // handler for Sound COMPLETE/IO_ERROR Event 
    private void _onSoundEvent(Object e) {
    }

    // parse
    //
    // parse system command on SiONDriver
    private boolean _parseSystemCommand(List<Map<String, Object>> systemCommands) {
        int id, wcol;
        boolean effectSet = false;
        if (systemCommands != null) {
            for (Map<String, Object> cmd : systemCommands) {
                switch ((String) cmd.get("command")) {
                    case "#EFFECT":
                        effectSet = true;
                        effector.parseMML((int) cmd.get("number"), (String) cmd.get("content"), (String) cmd.get("postfix"));
                        break;
                    case "#WAVCOLOR":
                    case "#WAVC":
                        wcol = Integer.parseInt((String) cmd.get("content"), 16);
                        setWaveTable((int) cmd.get("number"), SiONUtil.waveColor(wcol, 0, null));
                        break;
                }
            }
        }
        return effectSet;
    }

    // jobs queue
    //

    // cancel
    private void _cancelAllJobs() {
        _data = null;
        _mmlString = null;
        _currentJob = 0;
        _jobProgress = 0;
        _jobQueue.clear();
        _queueLength = 0;
        _removeAllEventListeners();
        // dispatchEvent(new SiONEvent(SiONEvent.QUEUE_CANCEL, this, null));
    }

    // next job
    private boolean _executeNextJob() {
        _data = null;
        _mmlString = null;
        _currentJob = 0;
        if (_jobQueue.isEmpty()) {
            _queueLength = 0;
            _removeAllEventListeners();
            // dispatchEvent(new SiONEvent(SiONEvent.QUEUE_COMPLETE, this, null));
            return true;
        }

        SiONDriverJob queue = _jobQueue.remove(0);
        if (queue.mml != null) _prepareCompile(queue.mml, queue.data);
        else _prepareRender(queue.data, queue.buffer, queue.channelCount, queue.resetEffector);
        return false;
    }

    // on enterFrame
    private void _queue_onEnterFrame() {
        try {
            SiONEvent event;
            int t = (int)System.currentTimeMillis();

            switch (_currentJob) {
                case 1: // compile
                    _jobProgress = sequencer.compile(_queueInterval);
                    _timeCompile += (int)System.currentTimeMillis() - t;
                    break;
                case 2: // render
                    _jobProgress += (1 - _jobProgress) * 0.5;
                    while ((int)System.currentTimeMillis() - t <= _queueInterval) {
                        if (_rendering()) {
                            _jobProgress = 1;
                            break;
                        }
                    }
                    _timeRender += (int)System.currentTimeMillis() - t;
                    break;
            }

            // finish job
            if (_jobProgress == 1) {
                // finish all jobs
                if (_executeNextJob()) return;
            }

            // progress
            // event = new SiONEvent(SiONEvent.QUEUE_PROGRESS, this, null, true);
            // dispatchEvent(event);
            // if (event.isDefaultPrevented()) _cancelAllJobs();   // canceled
        } catch (Exception e) {
            // error
            _removeAllEventListeners();
            _cancelAllJobs();
            if (_debugMode) throw new RuntimeException(e);
            else e.printStackTrace();
        }
    }

    // compile
    //

    // prepare to compile
    private void _prepareCompile(String mml, SiONData data) {
        if (data != null) data.clear();
        _data = (data != null) ? data : new SiONData();
        _mmlString = mml;
        sequencer.prepareCompile(_data, _mmlString);
        _jobProgress = 0.01;
        _timeCompile = 0;
        _currentJob = 1;
    }

    // render
    //

    // prepare for rendering
    private void _prepareRender(Object data, double[] renderBuffer, int renderBufferChannelCount, boolean resetEffector) {
        // same preparation as streaming
        _prepareProcess(data, resetEffector);

        // prepare rendering buffer
        _renderBuffer = (renderBuffer != null) ? renderBuffer : new double[0];
        _renderBufferChannelCount = (renderBufferChannelCount == 2) ? 2 : 1;
        _renderBufferSizeMax = _renderBuffer.length;
        _renderBufferIndex = 0;

        // initialize parameters
        _jobProgress = 0.01;
        _timeRender = 0;
        _currentJob = 2;
    }

    // rendering @return true when finished rendering.
    private boolean _rendering() {
        int i, j, imax, extention;
        double[] output = module.getOutput();
        boolean finished = false;

        // processing
        module._beginProcess();
        effector._beginProcess();
        sequencer._process();
        effector._endProcess();
        module._endProcess();

        // limit rendering length
        imax = _bufferLength << 1;
        extention = _bufferLength << (_renderBufferChannelCount - 1);
        if (_renderBufferSizeMax != 0 && _renderBufferSizeMax < _renderBufferIndex + extention) {
            extention = _renderBufferSizeMax - _renderBufferIndex;
            finished = true;
        }

        // extend buffer
        if (_renderBuffer.length < _renderBufferIndex + extention) {
            double[] newBuf = new double[_renderBufferIndex + extention];
            System.arraycopy(_renderBuffer, 0, newBuf, 0, extention + _renderBufferIndex);
            _renderBuffer = newBuf;
        }

        // copy output
        if (_renderBufferChannelCount == 2) {
            for (i = 0, j = _renderBufferIndex; i < imax; i++, j++) {
                _renderBuffer[j] = output[i];
            }
        } else {
            for (i = 0, j = _renderBufferIndex; i < imax; i += 2, j++) {
                _renderBuffer[j] = output[i];
            }
        }

        // incerement index
        _renderBufferIndex += extention;

        return (finished || (_renderBufferSizeMax == 0 && sequencer.isFinished()));
    }

    // process
    //

    // prepare for processing
    private void _prepareProcess(Object data, boolean resetEffector) {
        if (data != null) {
            if (data instanceof String) { // mml
                // compile mml and play
                _tempData = _tempData != null ? _tempData : new SiONData();
                _data = compile(((String) data), _tempData);
            } else if (data instanceof SiONData) {
                // type check and play
                _data = (SiONData) data;
            } else if (data instanceof Sound) {
                // play data as background sound
                setBackgroundSound((Sound) data);
            } else if (data instanceof URLRequest) {
                // load sound from url
                Sound sound = new Sound((URLRequest) data, null);
                setBackgroundSound(sound);
            } else if (data instanceof SMFData) {
                // MIDI file
                _midiConverter.setSmfData((SMFData) data);
                _midiConverter.useMIDIModuleEffector = resetEffector;
                _data = _midiConverter;
            } else {
                // not good data type
                throw errorDataIncorrect();
            }
        }

        // THESE FUNCTIONS ORDER IS VERY IMPORTANT !!
        module.initialize(_channelCount, _bitRate, _bufferLength);      // initialize DSP
        module.reset();                                                 // reset all channels
        if (resetEffector) effector.initialize();                       // initialize (or reset) effectors
        else effector._reset();
        sequencer._prepareProcess(_data, (int) _sampleRate, _bufferLength);   // set sequencer tracks (should be called after module.reset())
        if (_data != null)
            _parseSystemCommand(_data.getSystemCommands());           // parse #EFFECT command (should be called after effector._reset())
        effector._prepareProcess();                                     // set effector connections
        _trackEventQueue.clear();                                    // clear event que

        // set position
        if (_data != null && _position > 0) {
            sequencer.dummyProcess((int) (_position * _sampleRate * 0.001));
        }

        // start background sound
        if (_backgroundSound != null) {
            _startBackgroundSound();
        }

        // set timer interruption
        if (_timerCallback != null) {
            sequencer.setGlobalSequence(_timerSequence); // set timer interruption
            sequencer._setTimerCallback(_timerCallback);
        }
    }

    // on enterFrame
    private void _process_onEnterFrame(Event e) {
        // frame rate
        int t = (int)System.currentTimeMillis();
        _frameRate = t - _prevFrameTime;
        _prevFrameTime = t;

        // _suspendStreaming = true when first streaming
        if (_suspendStreaming) {
            _onSuspendStream();
        } else {
            // preserve stop
            if (_preserveStop) stop();

            // frame trigger
            if (!_trackEventQueue.isEmpty()) {
                List<SiONTrackEvent> nextQueue = new ArrayList<>();
                for (SiONTrackEvent te : new ArrayList<>(_trackEventQueue)) {
                    if (te._decrementTimer(_frameRate)) {
                        dispatchEvent(te);
                    } else {
                        nextQueue.add(te);
                    }
                }
                _trackEventQueue = nextQueue;
            }
        }
    }

    // _trackEventQueue filter

    // suspend starting stream
    private void _onSuspendStream() {
        // reset suspending
        _suspendStreaming = _suspendWhileLoading && (_loadingSoundList.length > 0);

        if (!_suspendStreaming) {
            // dispatch streaming start event
            SiONEvent event = new SiONEvent(SiONEvent.STREAM_START, this, null, true);
            dispatchEvent(event);
            if (event.isDefaultPrevented()) stop();   // canceled
        }
    }

    // on sampleData
    private void _streaming(SampleDataEvent e) {
        ByteArray buffer = e.data;
        int extracted;
        double[] output = module.getOutput();
        int imax, i;
        SiONEvent event;

        // calculate latency (0.022675736961451247 = 1/44.1)
        if (_soundChannel != null) {
            _latency = e.position * 0.022675736961451247 - _soundChannel.position;
        }

        try {
            // set streaming flag
            _inStreaming = true;

            if (_isPaused || _suspendStreaming) {
                // fill silence
                _fillzero(e.data);
            } else {
                // process starting time
                int t = SiONUtil.getTimer();

                // processing
                module._beginProcess();
                effector._beginProcess();
                sequencer._process();
                effector._endProcess();
                module._endProcess();

                // calculate average of processing time
                _timePrevStream = t;
                _timeProcessTotal -= _timeProcessData.i;
                _timeProcessData.i = SiONUtil.getTimer() - t;
                _timeProcessTotal += _timeProcessData.i;
                _timeProcessData = _timeProcessData.next;
                _timeProcess = (int) (_timeProcessTotal * _timeProcessAveRatio);

                // write samples
                imax = output.length;
                for (i = 0; i < imax; i++) buffer.writeFloat(output[i]);

                // dispatch streaming event
                if (_dispatchStreamEvent) {
                    event = new SiONEvent(SiONEvent.STREAM, this, buffer, true);
                    dispatchEvent(event);
                    if (event.isDefaultPrevented()) stop();   // canceled
                }

                // dispatch finishSequence event
                if (!_isFinishSeqDispatched && sequencer.getIsSequenceFinished()) {
                    dispatchEvent(new SiONEvent(SiONEvent.FINISH_SEQUENCE, this, null, false));
                    _isFinishSeqDispatched = true;
                }

                // fading
                if (_fader.execute()) {
                    String eventType = (_fader.getIsIncrement()) ? SiONEvent.FADE_IN_COMPLETE : SiONEvent.FADE_OUT_COMPLETE;
                    dispatchEvent(new SiONEvent(eventType, this, buffer, false));
                    if (_autoStop && !_fader.getIsIncrement()) stop();
                } else {
                    // auto stop
                    if (_autoStop && sequencer.getIsSequenceFinished()) stop();
                }
            }

            // reset streaming flag
            _inStreaming = false;

        } catch (Exception ex) {
            // error
            _removeAllEventListeners();
            if (_debugMode) throw ex;
            else dispatchEvent(new ErrorEvent("error", false, false, ex.getMessage()));
        }
    }

    // fill zero
    private void _fillzero(ByteArray buffer) {
        int i, imax = _bufferLength;
        for (i = 0; i < imax; i++) {
            buffer.writeFloat(0);
            buffer.writeFloat(0);
        }
    }

    // MIDI related
    //

    /** @private dispatch SiONMIDIEvent call from MIDIModule */
    public int _checkMIDIEventListeners() {
        return ((hasEventListener(SiONMIDIEvent.NOTE_ON)) ? SiONMIDIEventFlag.NOTE_ON : 0) |
                ((hasEventListener(SiONMIDIEvent.NOTE_OFF)) ? SiONMIDIEventFlag.NOTE_OFF : 0) |
                ((hasEventListener(SiONMIDIEvent.CONTROL_CHANGE)) ? SiONMIDIEventFlag.CONTROL_CHANGE : 0) |
                ((hasEventListener(SiONMIDIEvent.PROGRAM_CHANGE)) ? SiONMIDIEventFlag.PROGRAM_CHANGE : 0) |
                ((hasEventListener(SiONMIDIEvent.PITCH_BEND)) ? SiONMIDIEventFlag.PITCH_BEND : 0);
    }

    /** @private dispatch SiONMIDIEvent call from MIDIModule */
    public void _dispatchMIDIEvent(String type, SiMMLTrack track, int channelNumber, int note, int data) {
        SiONMIDIEvent event = new SiONMIDIEvent(type, this, track, channelNumber, sequencer.getStreamWritingPositionResidue(), note, data);
        _trackEventQueue.add(event);
    }

    // operations
    //

    // volume fading
    private void _fadeVolume(double v) {
        _faderVolume = v;
        _soundTransform.volume = _masterVolume * _faderVolume;
        if (_soundChannel != null) _soundChannel.soundTransform = _soundTransform;
        if (_dispatchFadingEvent) {
            SiONEvent event = new SiONEvent(SiONEvent.FADE_PROGRESS, this, null, true);
            dispatchEvent(event);
            if (event.isDefaultPrevented()) _fader.stop();   // canceled
        }
    }

    // background sound
    //

    // 1st internal entry point (pass null to stop sound)
    private void _setBackgroundSound(Sound sound) {
        if (sound != null) {
            if (sound.bytesTotal == 0 || sound.bytesLoaded != sound.bytesTotal) {
                sound.addEventListener("complete", this::_onBackgroundSoundLoaded);
                sound.addEventListener("ioError", this::_errorBackgroundSound);
            } else {
                _backgroundSound = sound;
                if (isPlaying()) _startBackgroundSound();
            }
        } else {
            // stop
            _backgroundSound = null;
            if (isPlaying()) _startBackgroundSound();
        }
    }

    // on loaded
    private void _onBackgroundSoundLoaded(Event e) {
        _backgroundSound = (Sound) e.target;
        if (isPlaying()) _startBackgroundSound();
    }

    // start
    private void _startBackgroundSound() {
        // frame index of start and end fading
        int startFrame, endFrame;

        // currently fading out -> stop fade out track
        if (_backgroundTrackFadeOut != null) {
            _backgroundTrackFadeOut.setDisposable();
            _backgroundTrackFadeOut.keyOff(0, true);
            _backgroundTrackFadeOut = null;
        }
        // background sound is playing now -> fade out
        if (_backgroundTrack != null) {
            _backgroundTrackFadeOut = _backgroundTrack;
            _backgroundTrack = null;
            startFrame = 0;
        } else {
            // no fadeout
            startFrame = _backgroundFadeOutFrames + _backgroundFadeGapFrames;
        }

        if (_backgroundSound != null) {
            // play sound with fade in
            _backgroundSample = new SiOPMWaveSamplerData(_backgroundSound, true, 0, 2, 2, null);
            _backgroundVoice.waveData = _backgroundSample;
            if (_backgroundLoopPoint != -1) {
                _backgroundSample.slice(-1, -1, (int) (_backgroundLoopPoint * 44100));
            }
            _backgroundTrack = sequencer._newControlableTrack(SiMMLTrack.DRIVER_BACKGROUND, false);
            _backgroundTrack.setExpression(128);
            _backgroundVoice.updateTrackVoice(_backgroundTrack);
            _backgroundTrack.keyOn(60, 0, (_backgroundFadeOutFrames + _backgroundFadeGapFrames) * _bufferLength);
            endFrame = _backgroundTotalFadeFrames;
        } else {
            // no new sound
            _backgroundSample = null;
            _backgroundVoice.waveData = null;
            _backgroundLoopPoint = -1;
            endFrame = _backgroundFadeOutFrames + _backgroundFadeGapFrames;
        }

        // set fader
        if (endFrame - startFrame > 0) {
            _fader.setFade(this::_fadeBackgroundSound, startFrame, endFrame, endFrame - startFrame);
        } else {
            // stop fade out immediately
            if (_backgroundTrackFadeOut != null) {
                _backgroundTrackFadeOut.setDisposable();
                _backgroundTrackFadeOut.keyOff(0, true);
                _backgroundTrackFadeOut = null;
            }
        }
    }

    // error
    private void _errorBackgroundSound(Event e) {
        _backgroundSound = null;
        throw errorSoundLoadingFailure();
    }

    // background sound cross fading
    private void _fadeBackgroundSound(double v) {
        double fo = 0, fi = 0;
        if (_backgroundTrackFadeOut != null) {
            if (_backgroundFadeOutFrames > 0) {
                fo = 1 - v / _backgroundFadeOutFrames;
                if (fo < 0) fo = 0;
                else if (fo > 1) fo = 1;
            } else {
                fo = 0;
            }
            _backgroundTrackFadeOut.setExpression((int) (fo * 128));
        }
        if (_backgroundTrack != null) {
            if (_backgroundFadeInFrames > 0) {
                fi = 1 - (_backgroundTotalFadeFrames - v) / _backgroundFadeInFrames;
                if (fi < 0) fi = 0;
                else if (fi > 1) fi = 1;
            } else {
                fi = 1;
            }
            _backgroundTrack.setExpression((int) (fi * 128));
        }
        if (_backgroundTrackFadeOut != null && (fo == 0 || fi == 1)) {
            _backgroundTrackFadeOut.setDisposable();
            _backgroundTrackFadeOut.keyOff(0, true);
            _backgroundTrackFadeOut = null;
        }
    }

    // errors
    //
    private RuntimeException errorPluralDrivers() {
        return new RuntimeException("SiONDriver error; Cannot create pulral SiONDrivers.");
    }

    private RuntimeException errorParamNotAvailable(String param, double num) {
        return new RuntimeException("SiONDriver error; Parameter not available. " + param + num);
    }

    private RuntimeException errorDataIncorrect() {
        return new RuntimeException("SiONDriver error; data incorrect in play() or render().");
    }

    private RuntimeException errorDriverBusy(int execID) {
        String[] states = new String[] {"???", "compiling", "streaming", "rendering"};
        return new RuntimeException("SiONDriver error: Driver busy. Call " + states[execID] + " while " + states[_listenEvent] + ".");
    }

    private RuntimeException errorCannotChangeBPM() {
        return new RuntimeException("SiONDriver error: Cannot change bpm while rendering (SiONTrackEvent.NOTE_*_STREAM).");
    }

    private RuntimeException errorNotGoodFMVoice() {
        return new RuntimeException("SiONDriver error; Cannot register the voice.");
    }

    private RuntimeException errorCannotListenLoading() {
        return new RuntimeException("SiONDriver error; the class not available for listenSoundLoadingStatus");
    }

    private RuntimeException errorSoundLoadingFailure() {
        return new RuntimeException("SiONDriver error; fail to load the sound file");
    }
}

class SiONDriverJob {

    public String mml;
    public double[] buffer;
    public SiONData data;
    public int channelCount;
    public boolean resetEffector;

    public SiONDriverJob(String mml_, double[] buffer_, SiONData data_, int channelCount_, boolean resetEffector_) {
        mml = mml_;
        buffer = buffer_;
        data = data_ != null ? data_ : new SiONData();
        channelCount = channelCount_;
        resetEffector = resetEffector_;
    }
}
