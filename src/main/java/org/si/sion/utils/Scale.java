//
// Scale class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Scale class. */
public class Scale {

    // constants
    //

    /** Scale table of C */
    protected static final int ST_MAJOR = 0x1ab5ab5;
    /** Scale table of Cm */
    protected static final int ST_MINOR = 0x15ad5ad;
    /** Scale table of Chm */
    protected static final int ST_HARMONIC_MINOR = 0x19ad9ad;
    /** Scale table of Cmm */
    protected static final int ST_MELODIC_MINOR = 0x1aadaad;
    /** Scale table of Cp */
    protected static final int ST_PENTATONIC = 0x1295295;
    /** Scale table of Cmp */
    protected static final int ST_MINOR_PENTATONIC = 0x14a94a9;
    /** Scale table of Cb */
    protected static final int ST_BLUE_NOTE = 0x14e94e9;
    /** Scale table of Cd */
    protected static final int ST_DIMINISH = 0x1249249;
    /** Scale table of Ccd */
    protected static final int ST_COMB_DIMINISH = 0x16db6db;
    /** Scale table of Cw */
    protected static final int ST_WHOLE_TONE = 0x1555555;
    /** Scale table of Cc */
    protected static final int ST_CHROMATIC = 0x1ffffff;
    /** Scale table of Csus4 */
    protected static final int ST_PERFECT = 0x10a10a1;
    /** Scale table of Csus47 */
    protected static final int ST_DPERFECT = 0x14a14a1;
    /** Scale table of C5 */
    protected static final int ST_POWER = 0x1081081;
    /** Scale table of Cu */
    protected static final int ST_UNISON = 0x1001001;
    /** Scale table of Cdor */
    protected static final int ST_DORIAN = 0x16ad6ad;
    /** Scale table of Cphr */
    protected static final int ST_PHRIGIAN = 0x15ab5ab;
    /** Scale table of Clyd */
    protected static final int ST_LYDIAN = 0x1ad5ad5;
    /** Scale table of Cmix */
    protected static final int ST_MIXOLYDIAN = 0x16b56b5;
    /** Scale table of Cloc */
    protected static final int ST_LOCRIAN = 0x156b56b;
    /** Scale table of Cgyp */
    protected static final int ST_GYPSY = 0x19b39b3;
    /** Scale table of Cspa */
    protected static final int ST_SPANISH = 0x15ab5ab;
    /** Scale table of Chan */
    protected static final int ST_HANGARIAN = 0x1acdacd;
    /** Scale table of Cjap */
    protected static final int ST_JAPANESE = 0x14a54a5;
    /** Scale table of Cryu */
    protected static final int ST_RYUKYU = 0x18b18b1;

    /** scale table dictionary */
    protected Map<String, Integer> _scaleTableDictionary = new HashMap<>() {{
        put("m", ST_MINOR);
        put("nm", ST_MINOR);
        put("aeo", ST_MINOR);
        put("hm", ST_HARMONIC_MINOR);
        put("mm", ST_MELODIC_MINOR);
        put("p", ST_PENTATONIC);
        put("mp", ST_MINOR_PENTATONIC);
        put("b", ST_BLUE_NOTE);
        put("d", ST_DIMINISH);
        put("cd", ST_COMB_DIMINISH);
        put("w", ST_WHOLE_TONE);
        put("c", ST_CHROMATIC);
        put("sus4", ST_PERFECT);
        put("sus47", ST_DPERFECT);
        put("5", ST_POWER);
        put("u", ST_UNISON);
        put("dor", ST_DORIAN);
        put("phr", ST_PHRIGIAN);
        put("lyd", ST_LYDIAN);
        put("mix", ST_MIXOLYDIAN);
        put("loc", ST_LOCRIAN);
        put("gyp", ST_GYPSY);
        put("spa", ST_SPANISH);
        put("han", ST_HANGARIAN);
        put("jap", ST_JAPANESE);
        put("ryu", ST_RYUKYU);
    }};

    /** note names */
    protected String[] _noteNames = {"C", "C+", "D", "D+", "E", "F", "F+", "G", "G+", "A", "A+", "B"};


    // valiables
    //
    /** scale table */
    protected int _scaleTable;
    /** notes on the scale */
    protected List<Integer> _scaleNotes;
    /** notes on 1octave upper scale */
    protected List<Integer> _tensionNotes;
    /** scale name */
    protected String _scaleName;
    /** default center octave, this applies when there are no octave specification. */
    protected int _defaultCenterOctave;


    // properties
    //

    /**
     * Scale name.
     * The regular expression of name is /(o[0-9])?([A-Ga-g])([+#\-])?([a-z0-9]+)?/.<br/>
     * The 1st letter means center octave. default octave = 5 (when omit).<br/>
     * The 2nd letter means root note.<br/>
     * The 3rd letter (option) means note shift sign. "+" and "#" shift +1, "-" shifts -1.<br/>
     * The 4th letters (option) means ((follows) scale).<br/>
     * <table>
     * <tr><th>the 3rd letters</th><th>scale</th></tr>
     * <tr><td>(no matching), ion</td><td>Major scale</td></tr>
     * <tr><td>m, nm, aeo</td><td>Natural minor scale</td></tr>
     * <tr><td>hm</td><td>Harmonic minor scale</td></tr>
     * <tr><td>mm</td><td>Melodic minor scale</td></tr>
     * <tr><td>p</td><td>Pentatonic scale</td></tr>
     * <tr><td>mp</td><td>Minor pentatonic scale</td></tr>
     * <tr><td>b</td><td>Blue note scale</td></tr>
     * <tr><td>d</td><td>Diminish scale</td></tr>
     * <tr><td>cd</td><td>Combination of diminish scale</td></tr>
     * <tr><td>w</td><td>Whole tone scale</td></tr>
     * <tr><td>c</td><td>Chromatic scale</td></tr>
     * <tr><td>sus4</td><td>table of sus4 chord</td></tr>
     * <tr><td>sus47</td><td>table of sus47 chord</td></tr>
     * <tr><td>5</td><td>Power chord</td></tr>
     * <tr><td>u</td><td>Unison (octave scale)</td></tr>
     * <tr><td>dor</td><td>Dorian mode</td></tr>
     * <tr><td>phr</td><td>Phrigian mode</td></tr>
     * <tr><td>lyd</td><td>Lydian mode</td></tr>
     * <tr><td>mix</td><td>Mixolydian mode</td></tr>
     * <tr><td>loc</td><td>Locrian mode</td></tr>
     * <tr><td>gyp</td><td>Gypsy scale</td></tr>
     * <tr><td>spa</td><td>Spanish scale</td></tr>
     * <tr><td>han</td><td>Hangarian scale</td></tr>
     * <tr><td>jap</td><td>Japanese scale (Ritsu mode)</td></tr>
     * <tr><td>ryu</td><td>Japanese scale (Ryukyu mode)</td></tr>
     * </table>
     * If you want to set "G sharp harmonic minor scale", name = "G+hm".
     */
    public String getName() {
        return _noteNames[_scaleNotes.get(0) % 12] + _scaleName;
    }

    public void setName(String str) {
        if (str == null || str.isEmpty()) {
            _scaleName = "";
            _scaleTable = ST_MAJOR;
            this.setRootNote(_defaultCenterOctave * 12);
            return;
        }

        Pattern rex = Pattern.compile("(o[0 - 9]) ? ([A - Ga - g])([+#\\-b])?([a - z0 - 9] +)?");
        Matcher mat = rex.matcher(str);
        int i;
        if (mat.matches()) {
            _scaleName = str;
            int note = new int[] {9, 11, 0, 2, 4, 5, 7}[String.valueOf(mat.group(2)).toLowerCase().charAt(0) - 'a'];
            if (mat.find(3)) {
                if (mat.group(3).equals("+") || mat.group(3).equals("#")) note++;
                else if (mat.group(3).equals("-")) note--;
            }
            if (note < 0) note += 12;
            else if (note > 11) note -= 12;
            if (mat.group(1) != null) note += (int) (mat.group(1).charAt(1)) * 12;
            else note += _defaultCenterOctave * 12;

            if (mat.group(4) != null) {
                if (!(_scaleTableDictionary.containsKey(mat.group(4))))throw _errorInvalidScaleName(str);
                _scaleTable = _scaleTableDictionary.get(mat.group(4));
                _scaleName = mat.group(4);
            } else {
                _scaleTable = ST_MAJOR;
                _scaleName = "";
            }
            this.setRootNote(note);
        } else {
            throw _errorInvalidScaleName(str);
        }
    }

    /** center octave */
    public int getCenterOctave() {
        return _scaleNotes.get(0) / 12;
    }

    public void setCenterOctave(int oct) {
        _defaultCenterOctave = oct;
        int prevoct = _scaleNotes.get(0) / 12;
        if (prevoct == oct) return;
        int i, offset = (oct - prevoct) * 12;
        for (i = 0; i < _scaleNotes.size(); i++) _scaleNotes.set(i, _scaleNotes.get(i) + offset);
        for (i = 0; i < _tensionNotes.size(); i++) _tensionNotes.set(i, _tensionNotes.get(i) + offset);
    }

    /** root note number */
    public int getRootNote() {
        return _scaleNotes.get(0);
    }

    public void setRootNote(int note) {
        _scaleNotes.clear();
        _tensionNotes.clear();
        int i;
        for (i = 0; i < 12; i++) if ((_scaleTable & (1 << i)) != 0) _scaleNotes.add(i + note);
        for (; i < 24; i++) if ((_scaleTable & (1 << i)) != 0) _tensionNotes.add(i + note);
    }

    /** bass note number */
    public int getBassNote() {
        return _scaleNotes.get(0);
    }

    public void setBassNote(int note) {
        setRootNote(note);
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param scaleName           scale name.
     * @param defaultCenterOctave default center octave, this apply when there are no octave specification.
     * @see #_scaleName
     */
    public Scale(String scaleName /* = "" */, int defaultCenterOctave /* = 5 */) {
        _scaleNotes = new ArrayList<>();
        _tensionNotes = new ArrayList<>();
        _defaultCenterOctave = defaultCenterOctave;
        this.setName(scaleName);
    }

    /**
     * set scale table manually.
     *
     * @param name     name of this scale.
     * @param rootNote root note of this scale.
     * @table Boolean table of available note on this scale. The length instanceof 12. The index of 0 instanceof root note.
     * @example If you want to set "F Japanese scale (1 2 4 5 b7)".<br/>
     * <pre>
     * Object[] table = [1,0,1,0,0,1,0,1,0,0,1,0];  // c,d,f,g,b- is available on "C japanese scale".
     * scale.setScaleTable("Fjap", 65, table);       // 65="F"s note number
     * </pre>
     */
    public void setScaleTable(String name, int rootNote, int[] table) {
        _scaleName = name;
        int i, imax = (table.length < 25) ? table.length : 25;
        _scaleTable = 0;
        for (i = 0; i < imax; i++) if (table[i] != 0) _scaleTable |= (1 << i);
        this.setRootNote(rootNote);
    }

    // operations
    //

    /**
     * check note availability on this scale.
     *
     * @param note MIDI note number (0-127).
     * @return Returns true if the note instanceof on this scale.
     */
    public boolean check(int note) {
        note -= _scaleNotes.get(0);
        if (note < 0) note = (note + 144) % 12;
        else if (note > 24) note = ((note - 12) % 12) + 12;
        return ((_scaleTable & (1 << note)) != 0);
    }

    /**
     * shift note to the nearest note on this scale.
     *
     * @param note MIDI note number (0-127).
     * @return Returns shifted note. if the note instanceof on this scale, no shift.
     */
    public int shift(int note) {
        int n = note - _scaleNotes.get(0);
        if (n < 0) n = (n + 144) % 12;
        else if (n > 23) n = ((n - 12) % 12) + 12;
        if ((_scaleTable & (1 << n)) != 0) return note;
        int up, dw;
        for (up = n + 1; up < 24 && (_scaleTable & (1 << up)) == 0; ) up++;
        for (dw = n - 1; dw >= 0 && (_scaleTable & (1 << dw)) == 0; ) dw--;
        return note - n + (((n - dw) <= (up - n)) ? dw : up);
    }

    /** get scale index from note. */
    public int getScaleIndex(int note) {
        return 0;
    }

    /**
     * get note by index on this scale.
     *
     * @param index index on this scale. You can specify both posi and nega values.
     * @return MIDI note number on this scale.
     */
    public int getNote(int index) {
        int imax = _scaleNotes.size(), octaveShift = 0;
        if (index < 0) {
            octaveShift = (index - imax + 1) / imax;
            index -= octaveShift * imax;
            return _scaleNotes.get(index) + octaveShift * 12;
        }
        if (index < imax) {
            return _scaleNotes.get(index);
        }

        index -= imax;
        imax = _tensionNotes.size();
        if (index < imax) {
            return _tensionNotes.get(index);
        }

        octaveShift = index / imax;
        index -= octaveShift * imax;
        return _tensionNotes.get(index) + octaveShift * 12;
    }

    /**
     * copy from another scale
     *
     * @param src another Scale instance copy from
     */
    public Scale copyFrom(Scale src) {
        _scaleName = src._scaleName;
        _scaleTable = src._scaleTable;
        int i, imax = src._scaleNotes.size();
//        _scaleNotes.length = imax;
        for (i = 0; i < imax; i++) {
            _scaleNotes.set(i, src._scaleNotes.get(i));
        }
        imax = src._tensionNotes.size();
//        _tensionNotes.length = imax;
        for (i = 0; i < imax; i++) {
            _tensionNotes.set(i, src._tensionNotes.get(i));
        }
        return this;
    }

    // errors
    //

    /** Invalid scale name error */
    protected Error _errorInvalidScaleName(String name) {
        return new Error("Scale; Invalid scale name. '" + name + "'");
    }
}
