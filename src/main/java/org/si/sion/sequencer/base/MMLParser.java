//
// MML parser class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


/** MML parser class. */
public class MMLParser {

    // tables
    //
    private static final int[][] _keySignatureTable = {
            {0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0},
            {1, 0, 0, 1, 0, 0, 0},
            {1, 0, 0, 1, 1, 0, 0},
            {1, 1, 0, 1, 1, 0, 0},
            {1, 1, 0, 1, 1, 1, 0},
            {1, 1, 1, 1, 1, 1, 0},
            {1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, -1},
            {0, 0, -1, 0, 0, 0, -1},
            {0, 0, -1, 0, 0, -1, -1},
            {0, -1, -1, 0, 0, -1, -1},
            {0, -1, -1, 0, -1, -1, -1},
            {-1, -1, -1, 0, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1}
    };

    // variables
    //

    // setting
    private MMLParserSetting _setting = null;

    // MML string
    private String _mmlString = null;

    // user defined event map.
    private java.util.Map<String, Integer> _userDefinedEventID = null;

    // system event strings
    private String[] _systemEventStrings = new String[32];
    private String[] _sequenceMMLStrings = new String[32];

    // flag list of global event
    private boolean[] _globalEventFlags = null;

    // temporaries
    private static MMLEvent _freeEventChain = null;

    private int _interruptInterval = 0;
    private int _startTime = 0;
    private int _parsingTime = 0;

    private int _staticLength = 0;
    private int _staticOctave = 0;
    private int _staticNoteShift = 0;
    private boolean _isLastEventLength = false;
    private int _systemEventIndex = 0;
    private int _sequenceMMLIndex = 0;
    private int _headMMLIndex = 0;
    private boolean _cacheMMLString = false;

    private int[] _keyScale = {0, 2, 4, 5, 7, 9, 11};
    private int[] _keySignature = _keySignatureTable[0];
    private int[] _keySignatureCustom = new int[7];
    private MMLEvent _terminator = new MMLEvent();
    private MMLEvent _lastEvent = null;
    private MMLEvent _lastSequenceHead = null;
    private List<MMLEvent> _repeatStac = new ArrayList<>();

    // properties
    //

    /** Key signiture for all notes. The letter for key signiture instanceof expressed as /[A-G][+\-#b]?m?/. */
    public void setKeySign(String sign) {
        int note, i;
        String[] list;
        int shift;
        String noteLetters = "cdefgab";
        switch (sign) {
            case "":
            case "C":
            case "Am":
                _keySignature = _keySignatureTable[0];
                break;
            case "G":
            case "Em":
                _keySignature = _keySignatureTable[1];
                break;
            case "D":
            case "Bm":
                _keySignature = _keySignatureTable[2];
                break;
            case "A":
            case "F+m":
            case "F#m":
                _keySignature = _keySignatureTable[3];
                break;
            case "E":
            case "C+m":
            case "C#m":
                _keySignature = _keySignatureTable[4];
                break;
            case "B":
            case "G+m":
            case "G#m":
                _keySignature = _keySignatureTable[5];
                break;
            case "F+":
            case "F#":
            case "D+m":
            case "D#m":
                _keySignature = _keySignatureTable[6];
                break;
            case "C+":
            case "C#":
            case "A+m":
            case "A#m":
                _keySignature = _keySignatureTable[7];
                break;
            case "F":
            case "Dm":
                _keySignature = _keySignatureTable[8];
                break;
            case "B-":
            case "Bb":
            case "Gm":
                _keySignature = _keySignatureTable[9];
                break;
            case "E-":
            case "Eb":
            case "Cm":
                _keySignature = _keySignatureTable[10];
                break;
            case "A-":
            case "Ab":
            case "Fm":
                _keySignature = _keySignatureTable[11];
                break;
            case "D-":
            case "Db":
            case "B-m":
            case "Bbm":
                _keySignature = _keySignatureTable[12];
                break;
            case "G-":
            case "Gb":
            case "E-m":
            case "Ebm":
                _keySignature = _keySignatureTable[13];
                break;
            case "C-":
            case "Cb":
            case "A-m":
            case "Abm":
                _keySignature = _keySignatureTable[14];
                break;
            default:
                for (i = 0; i < 7; i++) {
                    _keySignatureCustom[i] = 0;
                }
                list = sign.split("[\\s,]");
                for (i = 0; i < list.length; i++) {
                    note = noteLetters.indexOf(list[i].charAt(0));
                    if (note == -1) throw errorKeySign(sign);
                    if (list[i].length() > 1) {
                        shift = list[i].charAt(1);
                        _keySignatureCustom[note] = (shift == '+' || shift == '#') ? 1 : (shift == '-' || shift == 'b') ? -1 : 0;
                    } else {
                        _keySignatureCustom[note] = 0;
                    }
                }
                _keySignature = _keySignatureCustom;
        }
    }

    /** Parsing progression (0-1). */
    public double getParseProgress() {
        if (_mmlString != null) {
            // we can't get lastIndex easily with matcher, returning 0
            return 0;
        }
        return 0;
    }

    // constructor
    //

    /** constructor do nothing. */
    public MMLParser() {
    }

    // allocator
    //

    /** Free all events in the sequence. */
    void _freeAllEvents(MMLSequence seq) {
        if (seq.headEvent == null) return;

        // connect to free list
        seq.tailEvent.next = _freeEventChain;

        // update head of free list
        _freeEventChain = seq.headEvent;

        // clear
        seq.headEvent = null;
        seq.tailEvent = null;
    }

    /** Free event. */
    static MMLEvent _freeEvent(MMLEvent e) {
        MMLEvent next = e.next;
        e.next = _freeEventChain;
        _freeEventChain = e;
        return next;
    }

    /** allocate event */
    static MMLEvent _allocEvent(int id, int data, int length) {
        if (_freeEventChain != null) {
            MMLEvent e = _freeEventChain;
            _freeEventChain = _freeEventChain.next;
            return e.initialize(id, data, length);
        }
        return (new MMLEvent()).initialize(id, data, length);
    }

    // setting
    //

    /** Set map of user defined ids. */
    void _setUserDefinedEventID(java.util.Map<String, Integer> map) {
        if (_userDefinedEventID != map) {
            _userDefinedEventID = map;
            _mmlRegExp = null;
        }
    }

    /** Set array of global event flags. */
    void _setGlobalEventFlags(boolean[] flags) {
        _globalEventFlags = flags;
    }

    // public operation
    //

    /* Add new event. */
    public MMLEvent addMMLEvent(int id, int data, int length /* = 0 */, boolean noteOption /* false */) {
        if (!noteOption) {
            // Make channel data chain
            if (id == MMLEvent.SEQUENCE_HEAD) {
                _lastSequenceHead.jump = _lastEvent;
                _lastSequenceHead = _pushMMLEvent(id, data, length);
                _initialize_track();
            } else
                // Concatinate REST event
                if (id == MMLEvent.REST && _lastEvent.id == MMLEvent.REST) {
                    _lastEvent.length += length;
                } else {
                    _pushMMLEvent(id, data, length);
                    // seqHead.data is the count of global events
                    if (_globalEventFlags[id]) _lastSequenceHead.data++;
                }
        } else {
            // note option event is inserted after NOTE .
            if (_lastEvent.id == MMLEvent.NOTE) {
                length = _lastEvent.length;
                _lastEvent.length = 0;
                _pushMMLEvent(id, data, length);
            } else {
                // Error when there is no NOTE before SLUR event.
                throw errorSyntax("* or &");
            }
        }

        _isLastEventLength = false;
        return _lastEvent;
    }

    /**
     * Get MMLEvent id by mml command letter.
     *
     * @param mmlCommand letter of MML command.
     * @return Event id. Returns 0 if not found.
     */
    public static int getEventID(String mmlCommand) {
        switch (mmlCommand) {
            case "c":
            case "d":
            case "e":
            case "f":
            case "g":
            case "a":
            case "b":
                return MMLEvent.NOTE;
            case "r":
                return MMLEvent.REST;
            case "q":
                return MMLEvent.QUANT_RATIO;
            case "@q":
                return MMLEvent.QUANT_COUNT;
            case "v":
                return MMLEvent.VOLUME;
            case "@v":
                return MMLEvent.FINE_VOLUME;
            case "%":
                return MMLEvent.MOD_TYPE;
            case "@":
                return MMLEvent.MOD_PARAM;
            case "@i":
                return MMLEvent.INPUT_PIPE;
            case "@o":
                return MMLEvent.OUTPUT_PIPE;
            case "(":
            case ")":
                return MMLEvent.VOLUME_SHIFT;
            case "&":
                return MMLEvent.SLUR;
            case "&&":
                return MMLEvent.SLUR_WEAK;
            case "*":
                return MMLEvent.PITCHBEND;
            case ",":
                return MMLEvent.PARAMETER;
            case "$":
                return MMLEvent.REPEAT_ALL;
            case "[":
                return MMLEvent.REPEAT_BEGIN;
            case "]":
                return MMLEvent.REPEAT_END;
            case "|":
                return MMLEvent.REPEAT_BREAK;
            case "t":
                return MMLEvent.TEMPO;
        }
        return 0;
    }

    /** get command letters. */
    static void _getCommandLetters(Object[] list) {
        list[MMLEvent.NOTE] = "c";
        list[MMLEvent.REST] = "r";
        list[MMLEvent.QUANT_RATIO] = "q";
        list[MMLEvent.QUANT_COUNT] = "@q";
        list[MMLEvent.VOLUME] = "v";
        list[MMLEvent.FINE_VOLUME] = "@v";
        list[MMLEvent.MOD_TYPE] = "%";
        list[MMLEvent.MOD_PARAM] = "@";
        list[MMLEvent.INPUT_PIPE] = "@i";
        list[MMLEvent.OUTPUT_PIPE] = "@o";
        list[MMLEvent.VOLUME_SHIFT] = "(";
        list[MMLEvent.SLUR] = "&";
        list[MMLEvent.SLUR_WEAK] = "&&";
        list[MMLEvent.PITCHBEND] = "*";
        list[MMLEvent.PARAMETER] = ",";
        list[MMLEvent.REPEAT_ALL] = "$";
        list[MMLEvent.REPEAT_BEGIN] = "[";
        list[MMLEvent.REPEAT_END] = "]";
        list[MMLEvent.REPEAT_BREAK] = "|";
        list[MMLEvent.TEMPO] = "t";
    }

    /** get system event string */
    String _getSystemEventString(MMLEvent e) {
        return _systemEventStrings[e.data];
    }

    /** get sequence mml string */
    String _getSequenceMML(MMLEvent e) {
        return (e.length == -1) ? "" : _sequenceMMLStrings[e.length];
    }

    // push event
    private MMLEvent _pushMMLEvent(int id, int data, int length) {
        _lastEvent.next = _allocEvent(id, data, length);
        _lastEvent = _lastEvent.next;
        return _lastEvent;
    }

    // register system event string
    private int _regSystemEventString(String str) {
        if (_systemEventStrings.length <= _systemEventIndex)
            _systemEventStrings = new String[_systemEventStrings.length * 2];
        _systemEventStrings[_systemEventIndex++] = str;
        return _systemEventIndex - 1;
    }

    // register sequence MML string
    private int _regSequenceMMLStrings(String str) {
        if (_sequenceMMLStrings.length <= _sequenceMMLIndex)
            _sequenceMMLStrings = new String[_sequenceMMLStrings.length * 2];
        _sequenceMMLStrings[_sequenceMMLIndex++] = str;
        return _sequenceMMLIndex - 1;
    }

    // regular expression
    //
    private final int REX_WHITESPACE = 1;
    private final int REX_SYSTEM = 2;
    private final int REX_COMMAND = 3;
    private final int REX_NOTE = 4;
    private final int REX_SHIFT_NOTE = 5;
    private final int REX_USER_EVENT = 6;
    private final int REX_EVENT = 7;
    private final int REX_TABLE = 8;
    private final int REX_PARAM = 9;
    private final int REX_PERIOD = 10;
    private Pattern _mmlRegExp = null;

    private Pattern createRegExp(boolean reset) {
        if (_mmlRegExp == null) {
            // user defined event letters
            String uderex = (_userDefinedEventID != null && !_userDefinedEventID.isEmpty()) ?
                    String.join("|", _userDefinedEventID.keySet()) : "a";

            String rex;
            rex = "(\\s+)";                                            // whitespace (res[1])
            rex += "|(#[^;]*)";                                         // system (res[2])
            rex += "|(";                                                // --all-- (res[3])
            rex += "([a-g])([\\-+#]?)";                                 // note (res[4],[5])
            rex += "|(" + uderex + ")";                                 // module events (res[6])
            rex += "|(@[qvio]?|&&|!@ns|[rlqovt^<>()\\[\\]/|$%&*,;])";   // default events (res[7])
            rex += "|(\\{.*?\\}[0-9]*\\*?[\\-0-9.]*\\+?[\\-0-9.]*)";    // table event (res[8])
            rex += ")\\s*(-?[0-9]*)";                                    // parameter (res[9])
            rex += "\\s*(\\.*)";                                        // periods (res[10])
            _mmlRegExp = Pattern.compile(rex);
        }

        // reset last index
        // if (reset) _mmlRegExp.lastIndex = 0; // Pattern in java doesn't have lastIndex
        return _mmlRegExp;
    }

    // parser
    //

    /**
     * Prepare to parse.
     *
     * @param mml MML String.
     * @return Returns head MMLEvent. The return value of null means no head event.
     */
    public void prepareParse(MMLParserSetting setting, String mml) {
        // set internal parameters
        _setting = setting;
        _mmlString = mml;
        _parsingTime = (int) System.currentTimeMillis();
        // create RegExp
        createRegExp(true);
        // initialize
        _initialize();
    }

    /**
     * Parse mml string.
     *
     * @param interrupt Interrupting interval [ms]. 0 means no interruption. The interrupt appears between each sequence.
     * @return Returns head MMLEvent. The return value of null means no head event.
     */
    public MMLEvent parse(int interrupt /* = 0 */) {
        int shift, note, halt;
        Pattern rex;
        java.util.regex.Matcher res;
        int mml2nn = _setting.getMml2nn();
        char codeC = "c".charAt(0);

        // set interrupting interval
        _interruptInterval = interrupt;
        _startTime = (int) System.currentTimeMillis();

        // regular expression
        rex = createRegExp(false);

        // parse
        halt = 0;
        res = rex.matcher(_mmlString);
        while (res.find() && !res.group(0).isEmpty()) {
            // skip comments
            if (res.group(REX_WHITESPACE) == null) {
                if (res.group(REX_NOTE) != null) {
                    // note events.
                    note = res.group(REX_NOTE).charAt(0) - codeC;
                    if (note < 0) note += 7;
                    shift = _keySignature[note];
                    switch (res.group(REX_SHIFT_NOTE)) {
                        case "+":
                        case "#":
                            shift++;
                            break;
                        case "-":
                            shift--;
                            break;
                    }
                    _note(_keyScale[note] + shift + mml2nn, __calcLength(res), __period(res));
                } else if (res.group(REX_USER_EVENT) != null) {
                    // user defined events.
                    if (! _userDefinedEventID.containsKey(res.group(REX_USER_EVENT)))
                        throw errorUnknown("REX_USER_EVENT");
                    addMMLEvent(_userDefinedEventID.get(res.group(REX_USER_EVENT)), __param(res, 0), 0, false);
                } else if (res.group(REX_EVENT) != null) {
                    // default events.
                    switch (res.group(REX_EVENT)) {
                        case "r":
                            _rest(__calcLength(res), __period(res));
                            break;
                        case "l":
                            _length(__calcLength(res), __period(res));
                            break;
                        case "^":
                            _tie(__calcLength(res), __period(res));
                            break;
                        case "o":
                            _octave(__param(res, _setting.getDefaultOctave()));
                            break;
                        case "q":
                            _quant(__param(res, _setting.defaultQuantRatio));
                            break;
                        case "@q":
                            _at_quant(__param(res, _setting.defaultQuantCount));
                            break;
                        case "v":
                            _volume(__param(res, _setting.defaultVolume));
                            break;
                        case "@v":
                            _at_volume(__param(res, _setting.defaultFineVolume));
                            break;
                        case "%":
                            _mod_type(__param(res, 0));
                            break;
                        case "@":
                            _mod_param(__param(res, 0));
                            break;
                        case "@i":
                            _input(__param(res, 0));
                            break;
                        case "@o":
                            _output(__param(res, 0));
                            break;
                        case "(":
                            _volumeShift(__param(res, 1));
                            break;
                        case ")":
                            _volumeShift(-__param(res, 1));
                            break;
                        case "<":
                            _octaveShift(__param(res, 1));
                            break;
                        case ">":
                            _octaveShift(-__param(res, 1));
                            break;
                        case "&":
                            _slur();
                            break;
                        case "&&":
                            _slurweak();
                            break;
                        case "*":
                            _portament();
                            break;
                        case ",":
                            _parameter(__param(res, 0));
                            break;
                        case ";":
                            halt = _end_sequence() ? 1 : 0;
                            break;
                        case "$":
                            _repeatPoint();
                            break;
                        case "[":
                            _repeatBegin(__param(res, 2));
                            break;
                        case "]":
                            _repeatEnd(__param(res, 0));
                            break;
                        case "|":
                            _repeatBreak();
                            break;
                    }
                } else if (res.group(REX_SYSTEM) != null) {
                    // system command is only available at the top of the channel sequence.
                    if (_lastEvent.id != MMLEvent.SEQUENCE_HEAD) throw errorSyntax(res.group(0));
                    // add system event
                    addMMLEvent(MMLEvent.SYSTEM_EVENT, _regSystemEventString(res.group(REX_SYSTEM)), 0, false);
                } else if (res.group(REX_TABLE) != null) {
                    // add table event
                    addMMLEvent(MMLEvent.TABLE_EVENT, _regSystemEventString(res.group(REX_TABLE)), 0, false);
                } else {
                    // syntax error
                    throw errorSyntax(res.group(0));
                }
            } // Close if (res.group(REX_WHITESPACE) == null)
            // halt
            if (halt != 0) return null;
        }

        // calculate parsing time
        _parsingTime = (int) System.currentTimeMillis() - _parsingTime;

        // clear terminator
        MMLEvent headEvent = _terminator.next;
        _terminator.next = null;
        return headEvent;
    }

    // internal functions
    //

    // parse length. The return value of Integer.MIN_VALUE means abbreviation.
    private int __calcLength (java.util.regex.Matcher res) {
        String paramStr = res.group(REX_PARAM);
        if (paramStr == null || paramStr.length() == 0) return Integer.MIN_VALUE;
        int len = Integer.parseInt(paramStr);
        if (len == 0) return 0;
        int iLength = _setting.resolution / len;
        if (iLength < 1 || iLength > _setting.resolution) throw errorRangeOver("length", 1, _setting.resolution);
        return iLength;
    }

    // parse param.
    private int __param (java.util.regex.Matcher res, int defaultValue){
        String paramStr = res.group(REX_PARAM);
        return (paramStr != null && !paramStr.isEmpty()) ? Integer.parseInt(paramStr) : defaultValue;
    }

    // parse periods.
    private int __period (java.util.regex.Matcher res) {
        String periodStr = res.group(REX_PERIOD);
        return periodStr != null ? periodStr.length() : 0;
    }

    // initialize before parse
    private void _initialize() {
        // free all remains
        MMLEvent e = _terminator.next;
        while (e != null) {
            e = _freeEvent(e);
        }

        // initialize tempraries
        _systemEventIndex = 0;                                            // system event index
        _sequenceMMLIndex = 0;                                            // sequence mml index
        _lastEvent = _terminator;                                  // clear event chain
        _lastSequenceHead = _pushMMLEvent(MMLEvent.SEQUENCE_HEAD, 0, 0);  // add first event (SEQUENCE_HEAD).
        if (_cacheMMLString) addMMLEvent(MMLEvent.DEBUG_INFO, -1, 0, false);
        _initialize_track();
    }

    // initialize before starting new track.
    private void _initialize_track() {
        _staticLength = _setting.getDefaultLength();    // initialize l command value
        _staticOctave = _setting.getDefaultOctave();    // initialize o command value
        _staticNoteShift = 0;                         // initialize note shift
        _isLastEventLength = false;                     // initialize l command flag
        _repeatStac.clear();                         // clear repeating pointer stac
        // _headMMLIndex = _mmlRegExp.lastIndex;      // mml index of sequence head (not easily available with Java Matcher)
    }

    // note
    //

    // note
    private void _note(int note, int iLength, int period) {
        note += _staticOctave * 12 + _staticNoteShift;
        if (note < 0) {
            //throw errorNoteOutofRange(note);
            note = 0;
        } else if (note > 127) {
            //throw errorNoteOutofRange(note);
            note = 127;
        }
        addMMLEvent(MMLEvent.NOTE, note, __calcLength(iLength, period), false);
    }

    // rest
    private void _rest(int iLength, int period) {
        addMMLEvent(MMLEvent.REST, 0, __calcLength(iLength, period), false);
    }

    // length operation
    //

    // length
    private void _length(int iLength, int period) {
        _staticLength = __calcLength(iLength, period);
        _isLastEventLength = true;
    }

    // tie
    private void _tie(int iLength, int period) {
        if (_isLastEventLength) {
            _staticLength += __calcLength(iLength, period);
        } else if (_lastEvent.id == MMLEvent.REST || _lastEvent.id == MMLEvent.NOTE) {
            _lastEvent.length += __calcLength(iLength, period);
        } else {
            throw errorSyntax("tie command");
        }
    }

    // slur
    private void _slur() {
        addMMLEvent(MMLEvent.SLUR, 0, 0, true);
    }

    // weak slur
    private void _slurweak() {
        addMMLEvent(MMLEvent.SLUR_WEAK, 0, 0, true);
    }

    // portamento
    private void _portament() {
        addMMLEvent(MMLEvent.PITCHBEND, 0, 0, true);
    }

    // gate time
    private void _quant(int param) {
        if (param < _setting.minQuantRatio || param > _setting.maxQuantRatio) {
            throw errorRangeOver("q", _setting.minQuantRatio, _setting.maxQuantRatio);
        }
        addMMLEvent(MMLEvent.QUANT_RATIO, param, 0, false);
    }

    // absolute gate time
    private void _at_quant(int param) {
        if (param < _setting.minQuantCount || param > _setting.maxQuantCount) {
            throw errorRangeOver("@q", _setting.minQuantCount, _setting.maxQuantCount);
        }
        addMMLEvent(MMLEvent.QUANT_COUNT, param, 0, false);
    }

    // calculate length
    private int __calcLength(int iLength, int period) {
        // set default value
        if (iLength == Integer.MIN_VALUE) iLength = _staticLength;
        // extension by period
        int len = iLength;
        while (period > 0) {
            iLength += len >> (period--);
        }
        return iLength;
    }

    // pitch operation
    //
    // octave
    private void _octave(int param) {
        if (param < _setting.minOctave || param > _setting.maxOctave) {
            throw errorRangeOver("o", _setting.minOctave, _setting.maxOctave);
        }
        _staticOctave = param;
    }

    // octave shift
    private void _octaveShift(int param) {
        param *= _setting.octavePolarization;
        _staticOctave += param;
    }

    // note shift
    private void _noteShift(int param) {
        _staticNoteShift += param;
    }

    // volume
    private void _volume(int param) {
        if (param < 0 || param > _setting.maxVolume) {
            throw errorRangeOver("v", 0, _setting.maxVolume);
        }
        addMMLEvent(MMLEvent.VOLUME, param, 0, false);
    }

    // fine volume
    private void _at_volume(int param) {
        if (param < 0 || param > _setting.maxFineVolume) {
            throw errorRangeOver("@v", 0, _setting.maxFineVolume);
        }
        addMMLEvent(MMLEvent.FINE_VOLUME, param, 0, false);
    }

    // volume shift
    private void _volumeShift(int param) {
        param *= _setting.volumePolarization;
        if (_lastEvent.id == MMLEvent.VOLUME_SHIFT || _lastEvent.id == MMLEvent.VOLUME) {
            _lastEvent.data += param;
        } else {
            addMMLEvent(MMLEvent.VOLUME_SHIFT, param, 0, false);
        }
    }

    // repeating
    //

    // repeat point
    private void _repeatPoint() {
        addMMLEvent(MMLEvent.REPEAT_ALL, 0, 0, false);
    }

    // begin repeating
    private void _repeatBegin(int rep) {
        if (rep < 1 || rep > 65535) throw errorRangeOver("[", 1, 65535);
        addMMLEvent(MMLEvent.REPEAT_BEGIN, rep, 0, false);
        _repeatStac.add(0, _lastEvent);
    }

    // break repeating
    private void _repeatBreak() {
        if (_repeatStac.size() == 0) throw errorStacUnderflow("|");
        addMMLEvent(MMLEvent.REPEAT_BREAK, 0, 0, false);
        _lastEvent.jump = (MMLEvent) _repeatStac.get(0);
    }

    // end repeating
    private void _repeatEnd(int rep) {
        if (_repeatStac.size() == 0) throw errorStacUnderflow("]");
        addMMLEvent(MMLEvent.REPEAT_END, 0, 0, false);
        MMLEvent beginEvent = (MMLEvent) _repeatStac.remove(0);
        _lastEvent.jump = beginEvent;   // rep_end.jump   = rep_start
        beginEvent.jump = _lastEvent;   // rep_start.jump = rep_end

        // update repeat count
        if (rep != Integer.MIN_VALUE) {
            if (rep < 1 || rep > 65535) throw errorRangeOver("]", 1, 65535);
            beginEvent.data = rep;
        }
    }

    // others
    //

    // module type
    private void _mod_type(int param) {
        addMMLEvent(MMLEvent.MOD_TYPE, param, 0, false);
    }

    // module parameters
    private void _mod_param(int param) {
        addMMLEvent(MMLEvent.MOD_PARAM, param, 0, false);
    }

    // set input pipe
    private void _input(int param) {
        addMMLEvent(MMLEvent.INPUT_PIPE, param, 0, false);
    }

    // set output pipe
    private void _output(int param) {
        addMMLEvent(MMLEvent.OUTPUT_PIPE, param, 0, false);
    }

    // pural parameters
    private void _parameter(int param) {
        addMMLEvent(MMLEvent.PARAMETER, param, 0, false);
    }

    // sequence change
    private boolean _end_sequence() {
        if (_lastEvent.id != MMLEvent.SEQUENCE_HEAD) {
            if (_lastSequenceHead.next != null && _lastSequenceHead.next.id == MMLEvent.DEBUG_INFO) {
                // memory sequence MMLs id in _lastSequenceHead.next.data
                // _headMMLIndex is not properly initialized with Java matchers.
                // _lastSequenceHead.next.data = _regSequenceMMLStrings(_mmlString.substring(_headMMLIndex, _mmlRegExp.lastIndex));
            }
            addMMLEvent(MMLEvent.SEQUENCE_HEAD, 0, 0, false);
            if (_cacheMMLString) addMMLEvent(MMLEvent.DEBUG_INFO, -1, 0, false);
            // Returns true when it has to interrupt.
            if (_interruptInterval == 0) return false;
            return (_interruptInterval < (System.currentTimeMillis() - _startTime));
        }
        return false;
    }

    // tempo
    private void _tempo(int t) {
        addMMLEvent(MMLEvent.TEMPO, t, 0, false);
    }

    // errors
    //
    public RuntimeException errorUnknown(String n) {
        return new RuntimeException("MMLParser Error : Unknown error #" + n + ".");
    }

    public RuntimeException errorNoteOutofRange(int note) {
        return new RuntimeException("MMLParser Error : Note #" + note + " is out of range.");
    }

    public RuntimeException errorSyntax(String syn) {
        return new RuntimeException("MMLParser Error : Syntax error '" + syn + "'.");
    }

    public RuntimeException errorRangeOver(String cmd, int min, int max) {
        return new RuntimeException("MMLParser Error : The parameter of '" + cmd + "' command must ragne from " + min + " to " + max + ".");
    }

    public RuntimeException errorStacUnderflow(String cmd) {
        return new RuntimeException("MMLParser Error : The stac of '" + cmd + "' command instanceof underflow.");
    }

    public RuntimeException errorStacOverflow(String cmd) {
        return new RuntimeException("MMLParser Error : The stac of '" + cmd + "' command instanceof overflow.");
    }

    public RuntimeException errorKeySign(String ksign) {
        return new RuntimeException("MMLParser Error : Cannot recognize '" + ksign + "' as a key signiture.");
    }
}
