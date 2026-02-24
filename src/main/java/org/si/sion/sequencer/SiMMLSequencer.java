//
// The SiMMLSequencer operates SiOPMModule by MML.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.si.sion.module.SiOPMChannelParam;
import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMTable;
import org.si.sion.module.SiOPMWavePCMTable;
import org.si.sion.module.SiOPMWaveSamplerTable;
import org.si.sion.sequencer.base.MMLData;
import org.si.sion.sequencer.base.MMLEvent;
import org.si.sion.sequencer.base.MMLExecutorConnector;
import org.si.sion.sequencer.base.MMLParser;
import org.si.sion.sequencer.base.MMLSequence;
import org.si.sion.sequencer.base.MMLSequenceGroup;
import org.si.sion.sequencer.base.MMLSequencer;
import org.si.sion.utils.Translator;


/**
 * The SiMMLSequencer operates SiOPMModule by MML.
 * SiMMLSequencer -> SiMMLTrack -> SiOPMChannelFM -> SiOPMOperator. (-> means "operates")
 */
public class SiMMLSequencer extends MMLSequencer {

    // constants
    //
    private static final int PARAM_MAX = 16;                // maximum parameter count
    private static final int MACRO_SIZE = 26;               // macro size
    private static final int DEFAULT_MAX_TRACK_COUNT = 128; // default maximum limit of track count

    // variables
    //

    /** SiMMLTrack list */
    public List<SiMMLTrack> tracks;

    /** maximum limit of track count */
    public int _maxTrackCount;

    private final SiMMLTable _table;  // table instance

    private Function<SiMMLTrack, Boolean> _callbackEventNoteOn;   // callback function for event trigger "note on"
    private Function<SiMMLTrack, Boolean> _callbackEventNoteOff;  // callback function for event trigger "note off"
    private BiConsumer<Integer, Boolean> _callbackTempoChanged;  // callback function for tempo change event
    private Runnable _callbackTimer = null;         // callback function for timer interruption
    private BiConsumer<Integer, Integer> _callbackBeat = null;          // callback function for beat event
    private BiPredicate<SiMMLData, Object> _callbackParseSysCmd = null;   // callback function for parsing system command

    private final SiOPMModule _module;                // Module instance
    private final MMLExecutorConnector _connector;    // MMLExecutorConnector
    private SiMMLTrack _currentTrack;           // Current processing track
    private final String[] _macroStrings;      // Macro strings
    private int _flagMacroExpanded;            // Expanded macro flag to avoid circular reference
    private final int _envelopEventID;                // Event id of first envelop
    private boolean _macroExpandDynamic;        // Macro expantion mode
    private boolean _enableChangeBPM;           // internal flag enable to change bpm

    private final int[] _p = new int[PARAM_MAX];  // temporary area to get plural parameters
    private int _internalTableIndex = 0;                     // internal table index
    private final List<SiMMLTrack> _freeTracks;                // SiMMLTracks free list
    private boolean _isSequenceFinished;                    // flag sequence finished

    private boolean _dummyProcess;              // play dummy process

    private String _title;                      // Title of the song.
    private int _processedSampleCount;          // Processed sample count

    // properties
    //

    /** Is ready to process ? */
    public boolean isReadyToProcess() {
        return (!tracks.isEmpty());
    }

    /** Song title */
    public String getTitle() {
        return _title;
    }

    /** Processed sample count */
    public int getProcessedSampleCount() {
        return _processedSampleCount;
    }

    /** Is finish buffering ? */
    public boolean isFinished() {
        if (!_isSequenceFinished) return false;
        for (SiMMLTrack trk : tracks) {
            if (!trk.isFinished()) return false;
        }
        return true;
    }

    /** Is finish executing sequence ? */
    public boolean getIsSequenceFinished() {
        return _isSequenceFinished;
    }

    /** Is enable to change BPM ? */
    public boolean isEnableChangeBPM() {
        return _enableChangeBPM;
    }

    /** function called back when parse system command. The function type instanceof function(data:SiMMLData, command) : Boolean. Return false to append the command to SiONData.systemCommands. */
    public void setCallbackOnParsingSystemCommand(BiPredicate<SiMMLData, Object> func) {
        _callbackParseSysCmd = func;
    }

    /** current writing position (16beat count) in streaming buffer */
    public int getStreamWritingBeat() {
        return (int) _globalBeat16;
    }

    /** current writing position in streaming buffer, always less than length of streaming buffer */
    public int getStreamWritingPositionResidue() {
        return _globalBufferIndex;
    }

    /** Current working track */
    public SiMMLTrack getCurrentTrack() {
        return _currentTrack;
    }

    /** SiONTrackEvent.BEAT instanceof called if (beatCount16th &amp; onBeatCallbackFilter) == 0. */
    public void _setBeatCallbackFilter(int filter) {
        _onBeatCallbackFilter = filter;
    }

    /** callback function for timer interruption. */
    public void _setTimerCallback(Runnable func) {
        _callbackTimer = func;
    }

    /** callback function for beat event. changed in SiONDeiver */
    public void _setBeatCallback(BiConsumer<Integer, Integer> func) {
        _callbackBeat = func;
    }

    /** currently in process to change position */
    boolean _isDummyProcess() {
        return _dummyProcess;
    }

    // constructor
    //

    /** Create new sequencer. */
    public SiMMLSequencer(SiOPMModule module,
                          Function<SiMMLTrack, Boolean> eventTriggerOn,
                          Function<SiMMLTrack, Boolean> eventTriggerOff,
                          BiConsumer<Integer, Boolean> tempoChanged) {
        super();

        int i;

        // initialize
        _table = SiMMLTable.getInstance();
        _module = module;
        tracks = new ArrayList<>();
        _freeTracks = new ArrayList<>();
        _processedSampleCount = 0;
        _connector = new MMLExecutorConnector();
        _macroStrings = new String[MACRO_SIZE];
        _callbackEventNoteOn = eventTriggerOn;
        _callbackEventNoteOff = eventTriggerOff;
        _callbackTempoChanged = tempoChanged;
        _currentTrack = null;
        _maxTrackCount = DEFAULT_MAX_TRACK_COUNT;
        _isSequenceFinished = true;
        _dummyProcess = false;

        // pitch
        newMMLEventListener("k", this::_onDetune);
        newMMLEventListener("kt", this::_onKeyTrans);
        newMMLEventListener("!@kr", this::_onRelativeDetune);

        // track setting
        newMMLEventListener("@mask", this::_onEventMask);
        setMMLEventListener(MMLEvent.QUANT_RATIO, this::_onQuantRatio);
        setMMLEventListener(MMLEvent.QUANT_COUNT, this::_onQuantCount);

        // volume
        newMMLEventListener("p", this::_onPan);
        newMMLEventListener("@p", this::_onFinePan);
        newMMLEventListener("@f", this::_onFilter);
        newMMLEventListener("x", this::_onExpression);
        setMMLEventListener(MMLEvent.VOLUME, this::_onVolume);
        setMMLEventListener(MMLEvent.VOLUME_SHIFT, this::_onVolumeShift);
        setMMLEventListener(MMLEvent.FINE_VOLUME, this::_onMasterVolume);
        newMMLEventListener("%v", this::_onVolumeSetting);
        newMMLEventListener("%x", this::_onExpressionSetting);
        newMMLEventListener("%f", this::_onFilterMode);

        // channel setting
        newMMLEventListener("@clock", this::_onClock);
        newMMLEventListener("@al", this::_onAlgorism);
        newMMLEventListener("@fb", this::_onFeedback);
        newMMLEventListener("@r", this::_onRingModulation);
        setMMLEventListener(MMLEvent.MOD_TYPE, this::_onModuleType);
        setMMLEventListener(MMLEvent.INPUT_PIPE, this::_onInput);
        setMMLEventListener(MMLEvent.OUTPUT_PIPE, this::_onOutput);
        newMMLEventListener("%t", this::_setEventTrigger);
        newMMLEventListener("%e", this::_dispatchEvent);

        // operator setting
        newMMLEventListener("i", this::_onSlotIndex);
        newMMLEventListener("@rr", this::_onOpeReleaseRate);
        newMMLEventListener("@tl", this::_onOpeTotalLevel);
        newMMLEventListener("@ml", this::_onOpeMultiple);
        newMMLEventListener("@dt", this::_onOpeDetune);
        newMMLEventListener("@ph", this::_onOpePhase);
        newMMLEventListener("@fx", this::_onOpeFixedNote);
        newMMLEventListener("@se", this::_onOpeSSGEnvelop);
        newMMLEventListener("@er", this::_onOpeEnvelopReset);
        setMMLEventListener(MMLEvent.MOD_PARAM, this::_onOpeParameter);
        newMMLEventListener("s", this::_onSustain);

        // modulation
        newMMLEventListener("@lfo", this::_onLFO);
        newMMLEventListener("mp", this::_onPitchModulation);
        newMMLEventListener("ma", this::_onAmplitudeModulation);

        // envelop
        newMMLEventListener("@fps", this::_onEnvelopFPS);
        _envelopEventID =
                newMMLEventListener("@@", this::_onToneEnv);
        newMMLEventListener("na", this::_onAmplitudeEnv);
        newMMLEventListener("np", this::_onPitchEnv);
        newMMLEventListener("nt", this::_onNoteEnv);
        newMMLEventListener("nf", this::_onFilterEnv);
        newMMLEventListener("_@@", this::_onToneReleaseEnv);
        newMMLEventListener("_na", this::_onAmplitudeReleaseEnv);
        newMMLEventListener("_np", this::_onPitchReleaseEnv);
        newMMLEventListener("_nt", this::_onNoteReleaseEnv);
        newMMLEventListener("_nf", this::_onFilterReleaseEnv);
        newMMLEventListener("!na", this::_onAmplitudeEnvTSSCP);
        newMMLEventListener("po", this::_onPortament);

        // processing events
        _registerProcessEvent();

        setMMLEventListener(MMLEvent.DRIVER_NOTE, this::_onDriverNoteOn);
        setMMLEventListener(MMLEvent.REGISTER, this::_onRegisterUpdate);

        // set initial values of operators
        _module.initOperatorParam.ar = 63;
        _module.initOperatorParam.dr = 0;
        _module.initOperatorParam.sr = 0;
        _module.initOperatorParam.rr = 28;
        _module.initOperatorParam.sl = 0;
        _module.initOperatorParam.tl = 0;
        _module.initOperatorParam.ksr = 0;
        _module.initOperatorParam.ksl = 0;
        _module.initOperatorParam.fmul = 128;
        _module.initOperatorParam.dt1 = 0;
        _module.initOperatorParam.detune = 0;
        _module.initOperatorParam.ams = 1;
        _module.initOperatorParam.phase = 0;
        _module.initOperatorParam.fixedPitch = 0;
        _module.initOperatorParam.modLevel = 5;
        _module.initOperatorParam.setPGType(SiOPMTable.PG_SQUARE);

        // parsers initial settings
        setting.defaultBPM = 120;
        setting.defaultLValue = 4;
        setting.defaultQuantRatio = 6;
        setting.maxQuantRatio = 8;
        setting.setDefaultOctave(5);
        setting.maxVolume = 512;
        setting.defaultVolume = 256;
        setting.maxFineVolume = 128;
        setting.defaultFineVolume = 64;
    }

    // operation for all tracks
    //
    // Free all tracks.
    private void _freeAllTracks() {
        _freeTracks.addAll(tracks);
        tracks.clear();
    }

    /** Reset all tracks. */
    public void _resetAllTracks() {
        for (SiMMLTrack trk : tracks) {
            trk._reset(0);
            trk.setVelocity(setting.defaultVolume);
            trk.quantRatio = (double) setting.defaultQuantRatio / setting.maxQuantRatio;
            trk.quantCount = calcSampleCount(setting.defaultQuantCount);
            trk.channel.setMasterVolume(setting.defaultFineVolume);
        }
        _processedSampleCount = 0;
        _isSequenceFinished = (tracks.isEmpty());
    }

    /** force stop */
    public void _stopSequence() {
        _isSequenceFinished = true;
    }

    // operation for controllable tracks
    //

    /**
     * @param internalTrackID internal track ID to find.
     * @param delay           delay value to find the track sounds at same timing. -1 ignores this value.
     * @return found track instance. Returns null when didnt find.
     * Find active track by internal track ID.
     */
    public SiMMLTrack _findActiveTrack(int internalTrackID, int delay) {
        List<?> result = new ArrayList<>();
        for (SiMMLTrack trk : tracks) {
            if (trk._internalTrackID == internalTrackID && trk.isActive()) {
                if (delay == -1) return trk;
                int diff = trk.getTrackStartDelay() - delay;
                if (-8 < diff && diff < 8) return trk;
            }
        }
        return null;
    }

    /**
     * @param internalTrackID New internal Tracks ID.
     * @param isDisposable    disposable flag
     * @return Returns null when there are no free tracks.
     * Get new controlable track.
     */
    public SiMMLTrack _newControlableTrack(int internalTrackID, boolean isDisposable) {
        int i;
        SiMMLTrack trk;
        for (i = tracks.size() - 1; i >= 0; i--) {
            trk = tracks.get(i);
            if (!trk.isActive()) return _initializeTrack(trk, internalTrackID, isDisposable);
        }

        if (tracks.size() < _maxTrackCount) {
            if (!_freeTracks.isEmpty()) trk = _freeTracks.remove(_freeTracks.size() - 1);
            else trk = null;
            if (trk == null) trk = new SiMMLTrack();
            trk._trackNumber = tracks.size();
            tracks.add(trk);
        } else {
            trk = _findLowestPriorityTrack();
            if (trk == null) return null;
        }

        return _initializeTrack(trk, internalTrackID, isDisposable);
    }

    // initialize track
    private SiMMLTrack _initializeTrack(SiMMLTrack track, int internalTrackID, boolean isDisposable) {
        track._initialize(null, 60, Math.max(internalTrackID, 0), this._callbackEventNoteOn, this._callbackEventNoteOff, isDisposable);
        track._reset(_globalBufferIndex);
        track.channel.setMasterVolume(setting.defaultFineVolume);
        return track;
    }

    // find the lowest priority track
    private SiMMLTrack _findLowestPriorityTrack() {
        int i, p, index = 0, maxPriority = 0;
        for (i = tracks.size() - 1; i >= 0; i--) {
            p = tracks.get(i).getPriority();
            if (p >= maxPriority) {
                index = i;
                maxPriority = p;
            }
        }
        return (maxPriority == 0) ? null : tracks.get(index);
    }

    // compile
    //

    /**
     * Prepare to compile mml string. Calls onBeforeCompile() inside.
     *
     * @param data Data instance.
     * @param mml  MML String.
     * @return Returns false when it's not necessary to compile.
     */
    @Override
    public boolean prepareCompile(MMLData data, String mml) {
        _freeAllTracks();
        return super.prepareCompile(data, mml);
    }

    // process
    //

    /**
     * @param bufferLength Buffering length of processing samples at once.
     * @param data  Reset all channel parameters.
     * Prepare to process audio.
     */
    @Override
    public void _prepareProcess(MMLData data, int sampleRate, int bufferLength) {
        // initialize all channels
        _freeAllTracks();
        _processedSampleCount = 0;
        _enableChangeBPM = true;

        // call super function (set mmlData/grobalSequence/defaultBPM inside)
        super._prepareProcess(data, sampleRate, bufferLength);

        if (mmlData != null) {
            // initialize all sequence tracks
            SiMMLTrack trk;
            MMLSequence seq = mmlData.sequenceGroup.getHeadSequence();
            int idx = 0, internalTrackID;

            while (seq != null) {
                if (seq.isActive) {
                    SiMMLTrack x = _freeTracks.isEmpty() ? null : _freeTracks.remove(_freeTracks.size() - 1);
                    trk = x != null ? x : new SiMMLTrack();
                    internalTrackID = idx | SiMMLTrack.MML_TRACK;
                    tracks.add(trk._initialize(seq, mmlData.defaultFPS, internalTrackID, _callbackEventNoteOn, _callbackEventNoteOff, true));
                    tracks.get(idx)._trackNumber = idx;
                    idx++;
                }
                seq = seq.getNextSequence();
            }
        }

        // reset
        _resetAllTracks();
    }

    /** Process all tracks. Calls onProcess() inside. This funciton must be called after prepareProcess(). */
    @Override
    public void _process() {
        int bufferingLength, len;
        SiMMLData data;
        boolean finished;

        // prepare buffering
        for (SiMMLTrack trk : tracks) trk.channel.resetChannelBufferStatus();

        // buffering
        finished = true;
        startGlobalSequence();
        do {
            bufferingLength = executeGlobalSequence();
            _enableChangeBPM = false;
            for (SiMMLTrack trk : tracks) {
                _currentTrack = trk;
                len = trk._prepareBuffer(bufferingLength);
                _bpm = trk.getBpmSetting() != null ? trk.getBpmSetting() : _changeableBPM;
                finished = processMMLExecutor(trk.executor, len) && finished;
            }
            _enableChangeBPM = true;
        } while (!isEndGlobalSequence());

        _bpm = _changeableBPM;
        _currentTrack = null;
        _processedSampleCount += _module.getBufferLength();

        _isSequenceFinished = finished;
    }

    /**
     * Dummy process. This funciton must be called after prepareProcess().
     *
     * @param sampleCount dumping sample count. [NOTICE] This value instanceof rounded by a buffer length. Not an exact value.
     */
    public void dummyProcess(int sampleCount) {
        int count, bufCount = sampleCount / _module.getBufferLength();
        if (bufCount == 0) return;

        // register dummy processing events
        _dummyProcess = true;
        _registerDummyProcessEvent();

        // pseudo processing
        for (count = 0; count < bufCount; count++) _process();

        // register standard processing events
        _dummyProcess = false;
        _registerProcessEvent();
    }

    // calculation
    //

    /**
     * calculate length (in sample count).
     *
     * @param beat16 The beat number in 16th calculating from.
     */
    public double calcSampleLength(double beat16) {
        return beat16 * _bpm.samplePerBeat16;
    }

    /**
     * calculate delay (in sample count) quantized by beat.
     *
     * @param sampleOffset Offset in sample count.
     * @param beat16Offset Offset in 16th beat.
     * @param quant        Quantizing beat in 16th. The 0 sets no quantization, 1 sets quantization by 16th, 4 sets quantization by 4th beat.
     */
    public double calcSampleDelay(int sampleOffset, double beat16Offset, double quant) {
        if (quant == 0) return sampleOffset + beat16Offset * _bpm.samplePerBeat16;
        int iBeats = (int) (sampleOffset * _bpm.beat16PerSample + _globalBeat16 + beat16Offset + 0.9999847412109375); //=65535/65536
        if (quant != 1) iBeats = (int) (((int) ((iBeats + quant - 1) / quant)) * quant);
        return (iBeats - _globalBeat16) * _bpm.samplePerBeat16;
    }

    //
    // Internal uses
    //

    // implements
    //

    /** Preprocess mml string */
    @Override
    protected String onBeforeCompile(String mml) {
        int codeA = 'A';
        Pattern comrex = Pattern.compile("/\\*.*?\\*/|//.*?[\\r\\n]+", Pattern.DOTALL);
        Pattern reprex = Pattern.compile("!\\[(\\d*)(.*?)(!\\|(.*?))?!\\\\](\\d*)", Pattern.DOTALL);
        Pattern seqrex = Pattern.compile("[ \\t\\r\\n]*(#([A-Z@\\-]+)(\\+=|=)?)?([^;{]*(\\{.*?})?[^;]*);", Pattern.DOTALL); //}
        Pattern midrex = Pattern.compile("([A-Z])?(?:-([A-Z])?)?");
        StringBuilder expmml;
        int i;
        char str1;
        String str2;
        boolean concat;
        int startID, endID;

        // reset
        _resetParserParameters();

        // remove comments
        mml += "\n";
        mml = comrex.matcher(mml).replaceAll("");

        // format last
        i = mml.length();
        do {
            if (i == 0) return null;
            str1 = mml.charAt(--i);
        } while (" \t\r\n".indexOf(str1) != -1);
        mml = mml.substring(0, i + 1);
        if (str1 != ';') mml += ";";

        // expand macros
        expmml = new StringBuilder();
        Matcher res = seqrex.matcher(mml);
        while (res.find()) {
            // normal sequence
            if (res.group(1) == null) {
                expmml.append(_expandMacro(res.group(4), false)).append(";");
            } else

                // system command
                if (res.group(3) == null) {
                    if (String.valueOf(res.group(2)).equals("END")) {
                        // #END command
                        break;
                    } else
                        // parse system command
                        if (!_parseSystemCommandBefore(String.valueOf(res.group(1)), res.group(4))) {
                            // if the function returns false, parse system command after compiling mml.
                            expmml.append(res.group(0));
                        }
                } else

                // macro definition
                {
                    str2 = String.valueOf(res.group(2));
                    concat = (res.group(3).equals("+="));
                    // parse macro IDs
                    Matcher midres = midrex.matcher(str2);
                    while (midres.find()) {
                        String range = midres.group(0);
                        if (range == null || range.isEmpty()) break;
                        startID = (midres.group(1) != null) ? (midres.group(1).charAt(0) - codeA) : 0;
                        endID = (range.contains("-")) ? ((midres.group(2) != null) ? (midres.group(2).charAt(0) - codeA) : MACRO_SIZE - 1) : startID;
                        for (i = startID; i <= endID; i++) {
                            if (concat) {
                                _macroStrings[i] += (_macroExpandDynamic) ? res.group(4) : _expandMacro(res.group(4), false);
                            } else {
                                _macroStrings[i] = (_macroExpandDynamic) ? res.group(4) : _expandMacro(res.group(4), false);
                            }
                        }
                    }
                }
        }

        // expand repeat
        Matcher matcher = reprex.matcher(expmml.toString());

        StringBuilder sbuf = new StringBuilder();

        while (matcher.find()) {

            String g1 = matcher.group(1);
            String g2 = matcher.group(2);
            String g3 = matcher.group(3);
            String g4 = matcher.group(4);
            String g5 = matcher.group(5);

            int imax;
            if (g1 != null && !g1.isEmpty()) {
                imax = Integer.parseInt(g1) - 1;
            } else if (g5 != null && !g5.isEmpty()) {
                imax = Integer.parseInt(g5) - 1;
            } else {
                imax = 1;
            }

            if (imax > 256) imax = 256;

            str2 = g2;
            if (g3 != null && !g3.isEmpty()) {
                str2 += g4;
            }

            StringBuilder str3 = new StringBuilder(str2.length() * (imax + 1));

            for (i = 0; i < imax; i++) {
                str3.append(str2);
            }

            str3.append(g2);

            matcher.appendReplacement(sbuf, Matcher.quoteReplacement(str3.toString()));
        }

        matcher.appendTail(sbuf);
        expmml = new StringBuilder(sbuf.toString());
        //trace(mml); trace(expmml);
        return expmml.toString();
    }

    /** Postprocess of compile. */
    @Override
    protected void onAfterCompile(MMLSequenceGroup seqGroup) {
        // parse system command after parsing
        MMLSequence seq = seqGroup.getHeadSequence();
        while (seq != null) {
            if (seq.isSystemCommand()) {
                // parse system command
                seq = _parseSystemCommandAfter(seqGroup, seq);
            } else {
                // normal sequence
                seq = seq.getNextSequence();
            }
        }
    }

    /** Callback when table event was found. */
    @Override
    protected void onTableParse(MMLEvent prev, String table) {
        if (prev.id < _envelopEventID || _envelopEventID + 10 < prev.id)
            throw _errorInternalTable();
        Pattern rex = Pattern.compile("\\{([^}]*)\\\\}(.*)", Pattern.DOTALL);
        Matcher res = rex.matcher(table);
        if (res.find()) {
            String dat = res.group(1);
            String pfx = res.group(2);
            SiMMLEnvelopTable env = new SiMMLEnvelopTable().parseMML(dat, pfx, 0);
            if (env.head == null)
                throw _errorParameterNotValid("{..}", dat);
            ((SiMMLData) mmlData).setEnvelopTable(_internalTableIndex, env);
            prev.data = _internalTableIndex;
            _internalTableIndex--;
        }
    }

    /** Processing audio */
    @Override
    protected void onProcess(int sampleLength, MMLEvent e) {
        _currentTrack._buffer(sampleLength);
    }

    /** Callback when the tempo instanceof changed. */
    @Override
    protected void onTempoChanged(double changingRatio) {
        for (SiMMLTrack trk : tracks) {
            if (trk.getBpmSetting() == null) trk.executor._onTempoChanged((int) changingRatio);
        }
        if (_callbackTempoChanged != null) _callbackTempoChanged.accept(_globalBufferIndex, _dummyProcess);
    }

    /** Callback when the timer interruption. */
    @Override
    protected void onTimerInterruption() {
        if (!_dummyProcess && _callbackTimer != null) _callbackTimer.run();
    }


    /** Callback on every 16th beats. */
    @Override
    protected void onBeat(int delaySamples, int beatCounter) {
        if (!_dummyProcess && _callbackBeat != null) _callbackBeat.accept(delaySamples, beatCounter);
    }

    // sub routines for parser
    //

    // Reset parser parameters.
    private void _resetParserParameters() {
        int i;

        // initialize
        _internalTableIndex = 511;
        _title = "";
        setting.octavePolarization = 1;
        setting.volumePolarization = 1;
        setting.defaultQuantRatio = 6;
        setting.maxQuantRatio = 8;
        _macroExpandDynamic = false;
        mmlParser.setKeySign("C");
        for (i = 0; i < _macroStrings.length; i++) {
            _macroStrings[i] = "";
        }
    }

    // Expand macro.
    private String _expandMacro(Object m, boolean recursive /* = false */) {
        if (!recursive) _flagMacroExpanded = 0;
        if (m == null) return "";
        int charCodeA = 'A';
        Pattern p = Pattern.compile("([A-Z])(\\(([-\\d]+)\\))?");
        Matcher matcher = p.matcher(String.valueOf(m));
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String arg1 = matcher.group(1);
            String arg2 = matcher.group(2);
            String arg3 = matcher.group(3);

            int t = 0, i, f;
            i = arg1.charAt(0) - charCodeA;
            f = 1 << i;
            if (_flagMacroExpanded != 0 && f != 0) throw new RuntimeException("Circular reference " + m);
            if (_macroStrings[i] != null && !_macroStrings[i].isEmpty()) {
                String replacement;
                if (arg2 != null && !arg2.isEmpty()) {
                    if (arg3 != null && !arg3.isEmpty()) t = Integer.parseInt(arg3);
                    replacement = "!@ns" + t + ((_macroExpandDynamic) ? _expandMacro(_macroStrings[i], true) : _macroStrings[i]) + "!@ns" + (-t);
                } else {
                    replacement = (_macroExpandDynamic) ? _expandMacro(_macroStrings[i], true) : _macroStrings[i];
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // system command parser
    //

    // Parse system command before parsing mml. returns false when it hasnt parsed.
    private boolean _parseSystemCommandBefore(String cmd, String prm) {
        int i;
        SiOPMChannelParam param;
        SiMMLEnvelopTable env;
        Object commandObject;

        // separating
        Pattern rex = Pattern.compile("\\s * (\\d *)\\s * (\\{(. * ?)\\\\})?(. *)");
        Matcher res = rex.matcher(prm);

        // abstracting
        int num = Integer.parseInt(res.group(1));                       // number before {...} block
        boolean noData = (res.group(2) == null);             // true when no {...} block
        String dat = (noData) ? "" : String.valueOf(res.group(3));    // data string (inside of {...} block)
        String pfx = String.valueOf(res.group(4));                    // postfix string

        // executing
        switch (cmd) {
            // tone settings
            case "#@": {
                __parseToneParam(Translator::parseParam, num, dat, pfx);
                return true;
            }
            case "#OPM@": {
                __parseToneParam(Translator::parseOPMParam, num, dat, pfx);
                return true;
            }
            case "#OPN@": {
                __parseToneParam(Translator::parseOPNParam, num, dat, pfx);
                return true;
            }
            case "#OPL@": {
                __parseToneParam(Translator::parseOPLParam, num, dat, pfx);
                return true;
            }
            case "#OPX@": {
                __parseToneParam(Translator::parseOPXParam, num, dat, pfx);
                return true;
            }
            case "#MA@": {
                __parseToneParam(Translator::parseMA3Param, num, dat, pfx);
                return true;
            }
            case "#AL@": {
                __parseToneParam(Translator::parseALParam, num, dat, pfx);
                return true;
            }

            // parser settings
            case "#TITLE": {
                mmlData.title = (noData) ? pfx : dat;
                return true;
            }
            case "#FPS": {
                mmlData.defaultFPS = (num > 0) ? num : ((noData) ? 60 : Integer.parseInt(dat));
                return true;
            }
            case "#SIGN": {
                mmlParser.setKeySign((noData) ? pfx : dat);
                return true;
            }
            case "#MACRO": {
                if (noData) dat = pfx;
                if (dat.equals("dynamic")) _macroExpandDynamic = true;
                else if (Objects.equals(dat, "static")) _macroExpandDynamic = false;
                else throw _errorParameterNotValid("#MACRO", dat);
                return true;
            }
            case "#QUANT": {
                if (num > 0) {
                    setting.maxQuantRatio = num;
                    setting.defaultQuantRatio = (int) (num * 0.75);
                }
                return true;
            }
            case "#TMODE": {
                _parseTCommansSubMML(dat);
                return true;
            }
            case "#VMODE": {
                _parseVCommansSubMML(dat);
                return true;
            }
            case "#REV": {
                if (noData) dat = pfx;
                switch (dat) {
                    case "" -> {
                        setting.octavePolarization = -1;
                        setting.volumePolarization = -1;
                    }
                    case "octave" -> setting.octavePolarization = -1;
                    case "volume" -> setting.volumePolarization = -1;
                    default -> throw _errorParameterNotValid("#REVERSE", dat);
                }
                return true;
            }

            // tables
            case "#TABLE": {
                if (num < 0 || num > 254) throw _errorParameterNotValid("#TABLE", String.valueOf(num));
                env = new SiMMLEnvelopTable().parseMML(dat, pfx, 65536);
                if (env.head == null) throw _errorParameterNotValid("#TABLE", dat);
                ((SiMMLData) mmlData).setEnvelopTable(num, env);
                return true;
            }
            case "#WAV": {
                if (num < 0 || num > 255) throw _errorParameterNotValid("#WAV", String.valueOf(num));
                ((SiMMLData) mmlData).setWaveTable(num, Translator.parseWAV(dat, pfx));
                return true;
            }
            case "#WAVB": {
                if (num < 0 || num > 255) throw _errorParameterNotValid("#WAVB", String.valueOf(num));
                ((SiMMLData) mmlData).setWaveTable(num, Translator.parseWAVB((noData) ? pfx : dat));
                return true;
            }

            // pcm voice
            case "#SAMPLER": {
                if (num < 0 || num > 255) throw _errorParameterNotValid("#SAMPLE", String.valueOf(num));
                if (!__setSamplerWave(num, dat)) __setAsCommandObject(cmd, num, dat, pfx);
                return true;
            }
            case "#PCMWAVE": {
                if (num < 0 || num > 255) throw _errorParameterNotValid("#PCMWAVE", String.valueOf(num));
                if (!__setPCMWave(num, dat)) __setAsCommandObject(cmd, num, dat, pfx);
                return true;
            }
            case "#PCMVOICE": {
                if (num < 0 || num > 255) throw _errorParameterNotValid("#PCMVOICE", String.valueOf(num));
                if (!__setPCMVoice(num, dat, pfx)) __setAsCommandObject(cmd, num, dat, pfx);
                return true;
            }

            // system command after parsing
            case "#FM":
                return false;

            // currently not suported
            case "#WAVEXP":
            case "#PCMB":
            case "#PCMC":
                throw _errorSystemCommand("#" + cmd + " is not supported currently.");

                // user defined system commands ?
            default:
                __setAsCommandObject(cmd, num, dat, pfx);
                return true;
        }

//        throw _errorUnknown("_parseSystemCommandBefore()");
    }

    void __parseToneParam(BiConsumer<SiOPMChannelParam, String> func, int num, String dat, String pfx) {
        SiOPMChannelParam param = ((SiMMLData) mmlData)._getSiOPMChannelParam(num);
        func.accept(param, dat);
        if (!pfx.isEmpty()) __parseInitSequence(param, pfx);
    }

    void __setAsCommandObject(String cmd, int num, String dat, String pfx) {
        Map<String, Object> commandObject = Map.of("command", cmd, "number", num, "content", dat, "postfix", pfx);
        if (_callbackParseSysCmd == null || !_callbackParseSysCmd.test(((SiMMLData) mmlData), commandObject)) {
            mmlData.getSystemCommands().add(commandObject);
        }
    }

    // Parse inside of #TMODE{...}
    private void _parseTCommansSubMML(String dat) {
        Pattern tcmdrex = Pattern.compile("(unit | timerb | fps) = ? ([\\d.]*)");
        Matcher res = tcmdrex.matcher(dat);
        double num = Double.parseDouble(res.group(2));
        if (Double.isNaN(num)) num = 0;
        switch (String.valueOf(res.group(1))) {
            case "unit":
                mmlData.tcommandMode = MMLData.TCOMMAND_BPM;
                mmlData.tcommandResolution = (num > 0) ? 1 / num : 1;
                break;
            case "timerb":
                mmlData.tcommandMode = MMLData.TCOMMAND_TIMERB;
                mmlData.tcommandResolution = ((num > 0) ? num : 4000) * 1.220703125;
                break;
            case "fps":
                mmlData.tcommandMode = MMLData.TCOMMAND_FRAME;
                mmlData.tcommandResolution = (num > 0) ? num * 60 : 3600;
                break;
        }
    }

    // Parse inside of #VMODE{...}
    private void _parseVCommansSubMML(String dat) {
        Pattern tcmdrex = Pattern.compile("(n88 | mdx | psg | mck | tss | %[xv])(\\d *)(\\s *,?\\s * (\\d ?))");
        Matcher res;
        int num, i;
        while ((res = tcmdrex.matcher(dat)).matches()) {
            switch (String.valueOf(res.group(1))) {
                case "%v":
                    i = Integer.parseInt(res.group(2));
                    mmlData.defaultVelocityMode = (i >= 0 && i < SiOPMTable.VM_MAX) ? i : 0;
                    i = (!res.group(4).isEmpty()) ? Integer.parseInt(res.group(4)) : 4;
                    mmlData.defaultVCommandShift = (i >= 0 && i < 8) ? i : 0;
                    break;
                case "%x":
                    i = Integer.parseInt(res.group(2));
                    mmlData.defaultExpressionMode = (i >= 0 && i < SiOPMTable.VM_MAX) ? i : 0;
                    break;
                case "n88":
                case "mdx":
                    mmlData.defaultVelocityMode = SiOPMTable.VM_DR32DB;
                    mmlData.defaultExpressionMode = SiOPMTable.VM_DR48DB;
                    break;
                case "psg":
                    mmlData.defaultVelocityMode = SiOPMTable.VM_DR48DB;
                    mmlData.defaultExpressionMode = SiOPMTable.VM_DR48DB;
                    break;
                default: // mck/tss
                    mmlData.defaultVelocityMode = SiOPMTable.VM_LINEAR;
                    mmlData.defaultExpressionMode = SiOPMTable.VM_LINEAR;
                    break;
            }
        }
    }

    // Parse system command after parsing mml.
    private MMLSequence _parseSystemCommandAfter(MMLSequenceGroup seqGroup, MMLSequence syscmd) {
        String letter = syscmd.getSystemCommand();
        Pattern rex = Pattern.compile("#(FM)[{ \\t\\r\\n]*([ ^}]*)");
        Matcher res = rex.matcher(letter);

        // skip system command
        MMLSequence seq = syscmd._removeFromChain();

        // parse command
        if (res.matches()) {
            switch (res.group(1)) {
                case "FM":
                    if (res.group(2) == null) throw _errorSystemCommand(letter);
                    _connector.parse(res.group(2));
                    seq = _connector.connect(seqGroup, seq);
                    break;
                default:
                    throw _errorSystemCommand(letter);
            }
        }

        return seq.getNextSequence();
    }

    // system command parser subs
    //

    MMLParser mmlParser = new MMLParser();

    // parse initializing sequence, called by __splitDataString()
    private void __parseInitSequence(SiOPMChannelParam param, String mml) {
        MMLSequence seq = param.initSequence;
        MMLEvent prev;
        MMLEvent e;

        mmlParser.prepareParse(setting, mml);
        e = mmlParser.parse(0);

        if (e != null && e.next != null) {
            seq._cutout(e);
            for (prev = seq.headEvent; prev.next != null; prev = e) {
                e = prev.next;
                // initializing sequence cannot include procssing events
                if (e.length != 0) throw _errorInitSequence(mml);
                // initializing sequence cannot include % and @.
                if (e.id == MMLEvent.MOD_TYPE || e.id == MMLEvent.MOD_PARAM) throw _errorInitSequence(mml);
                // parse table event
                if (e.id == MMLEvent.TABLE_EVENT) {
                    callOnTableParse(prev);
                    e = prev;
                }
            }
        }
    }

    private boolean __setSamplerWave(int index, String dat) {
        if (SiOPMTable.getInstance().soundReference == null) return false;
        int bank = (index >> SiOPMTable.NOTE_BITS) & (SiOPMTable.SAMPLER_TABLE_MAX - 1);
        index &= (SiOPMTable.NOTE_TABLE_SIZE - 1);
        SiOPMWaveSamplerTable table = ((SiMMLData) mmlData).samplerTables[bank];
        return Translator.parseSamplerWave(table, index, dat, SiOPMTable.getInstance().soundReference);
    }

    private boolean __setPCMWave(int index, String dat) {
        if (SiOPMTable.getInstance().soundReference == null) return false;
        SiOPMWavePCMTable table = ((SiOPMWavePCMTable) ((SiMMLData) mmlData)._getPCMVoice (index).waveData);
        if (table == null) return false;
        return Translator.parsePCMWave(table, dat, SiOPMTable.getInstance().soundReference);
    }

    private boolean __setPCMVoice(int index, String dat, String pfx) {
        if (SiOPMTable.getInstance().soundReference == null) return false;
        SiMMLVoice voice = ((SiMMLData) mmlData)._getPCMVoice (index);
        if (voice == null) return false;
        return Translator.parsePCMVoice(voice, dat, pfx, ((SiMMLData) mmlData).envelopes);
    }

    // event handlers
    //

    // register process events
    private void _registerProcessEvent() {
        setMMLEventListener(MMLEvent.NOP, this::_default_onNoOperation);
        setMMLEventListener(MMLEvent.PROCESS, this::_default_onProcess);
        setMMLEventListener(MMLEvent.REST, this::_onRest);
        setMMLEventListener(MMLEvent.NOTE, this::_onNote);
        setMMLEventListener(MMLEvent.SLUR, this::_onSlur);
        setMMLEventListener(MMLEvent.SLUR_WEAK, this::_onSlurWeak);
        setMMLEventListener(MMLEvent.PITCHBEND, this::_onPitchBend);
    }

    // register dummy process events
    private void _registerDummyProcessEvent() {
        setMMLEventListener(MMLEvent.NOP, this::_nop);
        setMMLEventListener(MMLEvent.PROCESS, this::_dummy_onProcess);
        setMMLEventListener(MMLEvent.REST, this::_dummy_onProcessEvent);
        setMMLEventListener(MMLEvent.NOTE, this::_dummy_onProcessEvent);
        setMMLEventListener(MMLEvent.SLUR, this::_dummy_onProcessEvent);
        setMMLEventListener(MMLEvent.SLUR_WEAK, this::_dummy_onProcessEvent);
        setMMLEventListener(MMLEvent.PITCHBEND, this::_dummy_onProcessEvent);
    }

    // dummy process event
    private MMLEvent _dummy_onProcessEvent(MMLEvent e) {
        return currentExecutor._publishProessingEvent(e);
    }

    // processing events
    //

    // rest
    private MMLEvent _onRest(MMLEvent e) {
        _currentTrack._onRestEvent();
        return currentExecutor._publishProessingEvent(e);
    }

    // note
    private MMLEvent _onNote(MMLEvent e) {
        _currentTrack._onNoteEvent(e.data, calcSampleCount(e.length));
        return currentExecutor._publishProessingEvent(e);
    }

    // SiONDriver.noteOn()
    private MMLEvent _onDriverNoteOn(MMLEvent e) {
        _currentTrack.setNote(e.data, calcSampleCount(e.length), false);
        return currentExecutor._publishProessingEvent(e);
    }

    // &
    private MMLEvent _onSlur(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_SLUR) != 0) {
            _currentTrack._changeNoteLength(calcSampleCount(e.length));
        } else {
            _currentTrack._onSlur();
        }
        return currentExecutor._publishProessingEvent(e);
    }

    // &&
    private MMLEvent _onSlurWeak(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_SLUR) != 0) {
            _currentTrack._changeNoteLength(calcSampleCount(e.length));
        } else {
            _currentTrack._onSlurWeak();
        }
        return currentExecutor._publishProessingEvent(e);
    }

    // *
    private MMLEvent _onPitchBend(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_SLUR) != 0) {
            _currentTrack._changeNoteLength(calcSampleCount(e.length));
        } else {
            if (e.next == null || e.next.id != MMLEvent.NOTE) return e.next;  // check next note
            int term = calcSampleCount(e.length);                         // changing time
            _currentTrack._onPitchBend(e.next.data, term);                    // pitch bending
        }
        return currentExecutor._publishProessingEvent(e);
    }

    // driver track events
    //

    // quantize ratio
    private MMLEvent _onQuantRatio(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_QUANTIZE) != 0) return e.next;  // check mask
        _currentTrack.quantRatio = e.data / setting.maxQuantRatio;              // quantize ratio
        return e.next;
    }

    // quantize count
    private MMLEvent _onQuantCount(MMLEvent e) {
        e = e.getParameters(_p, 2);
        _p[0] = (_p[0] == Integer.MIN_VALUE) ? 0 : (_p[0] * setting.resolution / setting.maxQuantCount);
        _p[1] = (_p[1] == Integer.MIN_VALUE) ? 0 : (_p[1] * setting.resolution / setting.maxQuantCount);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_QUANTIZE) != 0) return e.next;  // check mask
        _currentTrack.quantCount = calcSampleCount(_p[0]);           // quantize count
        _currentTrack.keyOnDelay = calcSampleCount(_p[1]);           // key on delay
        return e.next;
    }

    // @mask
    private MMLEvent _onEventMask(MMLEvent e) {
        _currentTrack.eventMask = (e.data != Integer.MIN_VALUE) ? e.data : 0;
        return e.next;
    }

    // k
    private MMLEvent _onDetune(MMLEvent e) {
        _currentTrack.pitchShift = (e.data == Integer.MIN_VALUE) ? 0 : e.data;
        return e.next;
    }

    // kt
    private MMLEvent _onKeyTrans(MMLEvent e) {
        _currentTrack.noteShift = (e.data == Integer.MIN_VALUE) ? 0 : e.data;
        return e.next;
    }

    // !@kr
    private MMLEvent _onRelativeDetune(MMLEvent e) {
        _currentTrack.pitchShift += (e.data == Integer.MIN_VALUE) ? 0 : e.data;
        return e.next;
    }

    // envelop events
    //

    // @fps
    private MMLEvent _onEnvelopFPS(MMLEvent e) {
        int frame = (e.data == Integer.MIN_VALUE || e.data == 0) ? 60 : e.data;
        if (frame > 1000) frame = 1000;
        _currentTrack.setEnvelopFPS(frame);
        return e.next;
    }

    // @@
    private MMLEvent _onToneEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setToneEnvelop(1, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }

    // na
    private MMLEvent _onAmplitudeEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setAmplitudeEnvelop(1, _table.getEnvelopTable(idx), _p[1], false);
        return e.next;
    }

    // !na
    private MMLEvent _onAmplitudeEnvTSSCP(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setAmplitudeEnvelop(1, _table.getEnvelopTable(idx), _p[1], true);
        return e.next;
    }

    // np
    private MMLEvent _onPitchEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setPitchEnvelop(1, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }

    // nt
    private MMLEvent _onNoteEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setNoteEnvelop(1, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }

    // nf
    private MMLEvent _onFilterEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setFilterEnvelop(1, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }

    // _@@
    private MMLEvent _onToneReleaseEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setToneEnvelop(0, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }

    // _na
    private MMLEvent _onAmplitudeReleaseEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setAmplitudeEnvelop(0, _table.getEnvelopTable(idx), _p[1], false);
        return e.next;
    }

    // _np
    private MMLEvent _onPitchReleaseEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setPitchEnvelop(0, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }

    // _nt
    private MMLEvent _onNoteReleaseEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setNoteEnvelop(0, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }

    // _nf
    private MMLEvent _onFilterReleaseEnv(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_ENVELOP) != 0) return e.next;   // check mask
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 1;
        int idx = (_p[0] >= 0 && _p[0] < 255) ? _p[0] : -1;
        _currentTrack.setFilterEnvelop(0, _table.getEnvelopTable(idx), _p[1]);
        return e.next;
    }


    // internal table envelop events
    //
    // @f
    private MMLEvent _onFilter(MMLEvent e) {
        e = e.getParameters(_p, 10);
        int cut = (_p[0] == Integer.MIN_VALUE) ? 128 : _p[0],
                res = (_p[1] == Integer.MIN_VALUE) ? 0 : _p[1],
                ar = (_p[2] == Integer.MIN_VALUE) ? 0 : _p[2],
                dr1 = (_p[3] == Integer.MIN_VALUE) ? 0 : _p[3],
                dr2 = (_p[4] == Integer.MIN_VALUE) ? 0 : _p[4],
                rr = (_p[5] == Integer.MIN_VALUE) ? 0 : _p[5],
                dc1 = (_p[6] == Integer.MIN_VALUE) ? 128 : _p[6],
                dc2 = (_p[7] == Integer.MIN_VALUE) ? 64 : _p[7],
                sc = (_p[8] == Integer.MIN_VALUE) ? 32 : _p[8],
                rc = (_p[9] == Integer.MIN_VALUE) ? 128 : _p[9];
        _currentTrack.channel.setSVFilter(cut, res, ar, dr1, dr2, rr, dc1, dc2, sc, rc);
        return e.next;
    }

    // %f
    private MMLEvent _onFilterMode(MMLEvent e) {
        _currentTrack.channel.setFilterType(e.data);
        return e.next;
    }

    // @lfo[cycle_frames],[ws]
    private MMLEvent _onLFO(MMLEvent e) {
        // get parameters
        e = e.getParameters(_p, 2);
        if (_p[1] > 7 && _p[1] < 255) { // custom table
            SiMMLEnvelopTable env = _table.getEnvelopTable(_p[1]);
            if (env != null) _currentTrack.channel.initializeLFO(-1, env.toVector(256, 0, 255, null));
            else _currentTrack.channel.initializeLFO(SiOPMTable.LFO_WAVE_TRIANGLE, null);
        } else {
            _currentTrack.channel.initializeLFO((_p[1] == Integer.MIN_VALUE) ? SiOPMTable.LFO_WAVE_TRIANGLE : _p[1], null);
        }
        _currentTrack.channel.setLFOCycleTime((_p[0] == Integer.MIN_VALUE) ? 333 : _p[0] * 1000 / 60);
        return e.next;
    }

    // mp [depth],[end_depth],[delay],[term]
    private MMLEvent _onPitchModulation(MMLEvent e) {
        e = e.getParameters(_p, 4);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_MODULATE) != 0) return e.next;   // check mask
        if (_p[0] == Integer.MIN_VALUE) _p[0] = 0;
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        if (_p[2] == Integer.MIN_VALUE) _p[2] = 0;
        if (_p[3] == Integer.MIN_VALUE) _p[3] = 0;
        _currentTrack.setModulationEnvelop(true, _p[0], _p[1], _p[2], _p[3]);
        return e.next;
    }

    // ma [depth],[end_depth],[delay],[term]
    private MMLEvent _onAmplitudeModulation(MMLEvent e) {
        e = e.getParameters(_p, 4);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_MODULATE) != 0) return e.next;   // check mask
        if (_p[0] == Integer.MIN_VALUE) _p[0] = 0;
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        if (_p[2] == Integer.MIN_VALUE) _p[2] = 0;
        if (_p[3] == Integer.MIN_VALUE) _p[3] = 0;
        _currentTrack.setModulationEnvelop(false, _p[0], _p[1], _p[2], _p[3]);
        return e.next;
    }

    // po [term]
    private MMLEvent _onPortament(MMLEvent e) {
        if (e.data == Integer.MIN_VALUE) e.data = 0;
        _currentTrack.setPortament(e.data);
        return e.next;
    }

    // i/o events
    //
    // v
    private MMLEvent _onVolume(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_VOLUME) != 0) return e.next;  // check mask
        _currentTrack._mmlVCommand(e.data);                                   // velocity (data<<3 = 16->128)
        return e.next;
    }

    // (, )
    private MMLEvent _onVolumeShift(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_VOLUME) != 0) return e.next;  // check mask
        _currentTrack._mmlVShift(e.data);                                     // velocity (data<<3 = 16->128)
        return e.next;
    }

    // %v
    private MMLEvent _onVolumeSetting(MMLEvent e) {
        e = e.getParameters(_p, SiOPMModule.STREAM_SEND_SIZE);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_VOLUME) != 0) return e.next;  // check mask
        _currentTrack._vcommandShift = (_p[1] == Integer.MIN_VALUE) ? 4 : _p[1];
        _currentTrack.setVelocityMode((_p[0] == Integer.MIN_VALUE) ? 0 : _p[0]);
        return e.next;
    }

    // x
    private MMLEvent _onExpression(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_VOLUME) != 0) return e.next; // check mask
        int x = (e.data == Integer.MIN_VALUE) ? 128 : e.data;                // default value = 128
        _currentTrack.setExpression(x);                                        // expression
        return e.next;
    }

    // %x
    private MMLEvent _onExpressionSetting(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_VOLUME) != 0) return e.next;  // check mask
        _currentTrack.setExpressionMode((e.data == Integer.MIN_VALUE) ? 0 : e.data);
        return e.next;
    }

    // @v
    private MMLEvent _onMasterVolume(MMLEvent e) {
        e = e.getParameters(_p, SiOPMModule.STREAM_SEND_SIZE);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_VOLUME) != 0) return e.next;    // check mask
        _currentTrack.channel.setAllStreamSendLevels(_p);                       // master volume
        return e.next;
    }

    // p
    private MMLEvent _onPan(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_PAN) != 0) return e.next;            // check mask
        _currentTrack.channel.setPan((e.data == Integer.MIN_VALUE) ? 0 : (e.data << 4) - 64);  // pan
        return e.next;
    }

    // @p
    private MMLEvent _onFinePan(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_PAN) != 0) return e.next;      // check mask
        _currentTrack.channel.setPan((e.data == Integer.MIN_VALUE) ? 0 : (e.data));  // pan
        return e.next;
    }

    // @i
    private MMLEvent _onInput(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if (_p[0] == Integer.MIN_VALUE) _p[0] = 5;
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        _currentTrack.channel.setInput(_p[0], _p[1]);
        return e.next;
    }

    // @o
    private MMLEvent _onOutput(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if (_p[0] == Integer.MIN_VALUE) _p[0] = 2;
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        _currentTrack.channel.setOutput(_p[0], _p[1]);
        return e.next;
    }

    // @r
    private MMLEvent _onRingModulation(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if (_p[0] == Integer.MIN_VALUE) _p[0] = 4;
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        _currentTrack.channel.setRingModulation(_p[0], _p[1]);
        return e.next;
    }

    // sound channel events
    //

    // %
    private MMLEvent _onModuleType(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if (_p[0] < 0 || _p[0] >= SiMMLTable.MT_MAX) _p[0] = SiMMLTable.MT_ALL;
        _currentTrack.setChannelModuleType(_p[0], _p[1], Integer.MIN_VALUE);
        return e.next;
    }

    // %t
    private MMLEvent _setEventTrigger(MMLEvent e) {
        e = e.getParameters(_p, 3);
        int id = (_p[0] != Integer.MIN_VALUE) ? _p[0] : 0;
        int typeOn = (_p[1] != Integer.MIN_VALUE) ? _p[1] : 1;
        int typeOff = (_p[2] != Integer.MIN_VALUE) ? _p[2] : 1;
        _currentTrack.setEventTrigger(id, typeOn, typeOff);
        return e.next;
    }

    // %e
    private MMLEvent _dispatchEvent(MMLEvent e) {
        e = e.getParameters(_p, 2);
        int id = (_p[0] != Integer.MIN_VALUE) ? _p[0] : 0;
        int typeOn = (_p[1] != Integer.MIN_VALUE) ? _p[1] : 1;
        _currentTrack.dispatchNoteOnEvent(id, typeOn);
        return e.next;
    }

    // @clock
    private MMLEvent _onClock(MMLEvent e) {
        _currentTrack.channel.setFrequencyRatio((e.data == Integer.MIN_VALUE) ? 100 : (e.data));
        return e.next;
    }

    // @al
    private MMLEvent _onAlgorism(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        int cnt = (_p[0] != Integer.MIN_VALUE) ? _p[0] : 0;
        int alg = (_p[1] != Integer.MIN_VALUE) ? _p[1] : _table.alg_init[cnt];
        _currentTrack.channel.setAlgorism(cnt, alg);
        return e.next;
    }

    // @
    private MMLEvent _onOpeParameter(MMLEvent e) {
        e = e.getParameters(_p, PARAM_MAX);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        MMLSequence seq = _currentTrack._setChannelParameters(_p);
        if (seq != null) {
            seq.connectBefore(e.next);
            return seq.headEvent.next;
        }
        return e.next;
    }

    // @fb
    private MMLEvent _onFeedback(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        int fb = (_p[0] != Integer.MIN_VALUE) ? _p[0] : 0;
        int fbc = (_p[1] != Integer.MIN_VALUE) ? _p[1] : 0;
        _currentTrack.channel.setFeedBack(fb, fbc);
        return e.next;
    }

    // i
    private MMLEvent _onSlotIndex(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        _currentTrack.channel.setActiveOperatorIndex((e.data == Integer.MIN_VALUE) ? 4 : e.data);
        return e.next;
    }

    // @rr
    private MMLEvent _onOpeReleaseRate(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        if (_p[0] != Integer.MIN_VALUE) _currentTrack.channel.setRr(_p[0]);
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        _currentTrack.setReleaseSweep(_p[1]);
        return e.next;
    }

    // @tl
    private MMLEvent _onOpeTotalLevel(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        _currentTrack.channel.setTl((e.data == Integer.MIN_VALUE) ? 0 : e.data);
        return e.next;
    }

    // @ml
    private MMLEvent _onOpeMultiple(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        if (_p[0] == Integer.MIN_VALUE) _p[0] = 0;
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        _currentTrack.channel.setFmul((_p[0] << 7) + _p[1]);
        return e.next;
    }

    // @dt
    private MMLEvent _onOpeDetune(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        _currentTrack.channel.setDetune((e.data == Integer.MIN_VALUE) ? 0 : e.data);
        return e.next;
    }

    // @ph
    private MMLEvent _onOpePhase(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;     // check mask
        int phase = (e.data == Integer.MIN_VALUE) ? 0 : e.data;
        _currentTrack.channel.setPhase(phase);                            // -1 = 255
        return e.next;
    }

    // @fx
    private MMLEvent _onOpeFixedNote(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        if (_p[0] == Integer.MIN_VALUE) _p[0] = 0;
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        _currentTrack.channel.setFixedPitch((_p[0] << 6) + _p[1]);
        return e.next;
    }

    // @se
    private MMLEvent _onOpeSSGEnvelop(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        _currentTrack.channel.setSsgec((e.data == Integer.MIN_VALUE) ? 0 : e.data);
        return e.next;
    }

    // @er
    private MMLEvent _onOpeEnvelopReset(MMLEvent e) {
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        _currentTrack.channel.setErst(e.data == 1);
        return e.next;
    }

    // s
    private MMLEvent _onSustain(MMLEvent e) {
        e = e.getParameters(_p, 2);
        if ((_currentTrack.eventMask & SiMMLTrack.MASK_OPERATOR) != 0) return e.next;      // check mask
        if (_p[0] != Integer.MIN_VALUE) _currentTrack.channel.setAllReleaseRate(_p[0]);
        if (_p[1] == Integer.MIN_VALUE) _p[1] = 0;
        _currentTrack.setReleaseSweep(_p[1]);
        return e.next;
    }

    // register event
    private MMLEvent _onRegisterUpdate(MMLEvent e) {
        e = e.getParameters(_p, 2);
        _currentTrack._callbackUpdateRegister.accept(_p[0], _p[1]);
        return e.next;
    }

    // errors
    //
    private RuntimeException _errorSyntax(String str) {
        return new RuntimeException("SiMMLSequencer error : Syntax error. " + str);
    }

    private RuntimeException _errorOutOfRange(String cmd, int n) {
        return new RuntimeException("SiMMLSequencer error : Out of range. '" + cmd + "' = " + n);
    }

    private RuntimeException _errorToneParameterNotValid(String cmd, int chParam, int opParam) {
        return new RuntimeException("SiMMLSequencer error : Parameter count instanceof not valid in '" + cmd + "'. " + chParam + " parameters for channel and " + opParam + " parameters for each operator.");
    }

    private RuntimeException _errorParameterNotValid(String cmd, String param) {
        return new RuntimeException("SiMMLSequencer error : Parameter not valid. '" + param + "' in " + cmd);
    }

    private RuntimeException _errorInternalTable() {
        return new RuntimeException("SiMMLSequencer error : Internal table instanceof available only for envelop commands.");
    }

    private RuntimeException _errorCircularReference(String mcr) {
        return new RuntimeException("SiMMLSequencer error : Circular reference in dynamic macro. " + mcr);
    }

    private RuntimeException _errorInitSequence(String mml) {
        return new RuntimeException("SiMMLSequencer error : Initializing sequence cannot include note, rest, '%' nor '@'. " + mml);
    }

    private RuntimeException _errorSystemCommand(String str) {
        return new RuntimeException("SiMMLSequencer error : System command error. " + str);
    }

    private RuntimeException _errorUnknown(String str) {
        return new RuntimeException("SiMMLSequencer error : Unknown. " + str);
    }
}
