//
// Chord class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Chord class. */
public class Chord extends Scale {

    // constants
    //

    /** Chord table of C */
    protected static final int CT_MAJOR = 0x1091091;
    /** Chord table of Cm */
    protected static final int CT_MINOR = 0x1089089;
    /** Chord table of C7 */
    protected static final int CT_7TH = 0x0490491;
    /** Chord table of Cm7 */
    protected static final int CT_MIN7 = 0x0488489;
    /** Chord table of CM7 */
    protected static final int CT_MAJ7 = 0x0890891;
    /** Chord table of CmM7 */
    protected static final int CT_MM7 = 0x0888889;
    /** Chord table of C9 */
    protected static final int CT_9TH = 0x0484491;
    /** Chord table of Cm9 */
    protected static final int CT_MIN9 = 0x0484489;
    /** Chord table of CM9 */
    protected static final int CT_MAJ9 = 0x0884891;
    /** Chord table of CmM9 */
    protected static final int CT_MM9 = 0x0884889;
    /** Chord table of Cadd9 */
    protected static final int CT_ADD9 = 0x1084091;
    /** Chord table of Cmadd9 */
    protected static final int CT_MINADD9 = 0x1084089;
    /** Chord table of C69 */
    protected static final int CT_69TH = 0x1204211;
    /** Chord table of Cm69 */
    protected static final int CT_MIN69 = 0x1204209;
    /** Chord table of Csus4 */
    protected static final int CT_SUS4 = 0x10a10a1;
    /** Chord table of Csus47 */
    protected static final int CT_SUS47 = 0x04a04a1;
    /** Chord table of Cdim */
    protected static final int CT_DIM = 0x1489489;
    /** Chord table of Carg */
    protected static final int CT_AUG = 0x1111111;

    /** chord table dictionary */
    protected Map<String, Integer> _chordTableDictionary = new HashMap<>() {{
        put("m", CT_MINOR);
        put("7", CT_7TH);
        put("m7", CT_MIN7);
        put("M7", CT_MAJ7);
        put("mM7", CT_MM7);
        put("9", CT_9TH);
        put("m9", CT_MIN9);
        put("M9", CT_MAJ9);
        put("mM9", CT_MM9);
        put("add9", CT_ADD9);
        put("madd9", CT_MINADD9);
        put("69", CT_69TH);
        put("m69", CT_MIN69);
        put("sus4", CT_SUS4);
        put("sus47", CT_SUS47);
        put("dim", CT_DIM);
        put("arg", CT_AUG);
    }};

    // variables
    //

    /** bass note offset from root */
    protected int _bassNoteOffset;

    // properties
    //

    /**
     * Chord name.
     * The regular expression of name is /(o[0-9])?([A-Ga-g])([+#\-])?([a-z0-9]+)?(,[0-9]+[+#\-]?)?(,[0-9]+[+#\-]?)?/.<br/>
     * The 1st letter means center octave. default octave = 5 (when omit).<br/>
     * The 2nd letter means root note.<br/>
     * The 3rd letter (option) means note shift sign. "+" and "#" shift +1, "-" shifts -1.<br/>
     * The 4th letters (option) means ((follows) chord).<br/>
     * <table>
     * <tr><th>the 3rd letters</th><th>chord</th></tr>
     * <tr><td>(no matching), maj</td><td>Major chord</td></tr>
     * <tr><td>m</td><td>Minor chord</td></tr>
     * <tr><td>7</td><td>7th chord</td></tr>
     * <tr><td>m7</td><td>Minor 7th chord</td></tr>
     * <tr><td>M7</td><td>Major 7th chord</td></tr>
     * <tr><td>mM7</td><td>Minor major 7th chord</td></tr>
     * <tr><td>9</td><td>9th chord</td></tr>
     * <tr><td>m9</td><td>Minor 9th chord</td></tr>
     * <tr><td>M9</td><td>Major 9th chord</td></tr>
     * <tr><td>mM9</td><td>Minor major 9th chord</td></tr>
     * <tr><td>add9</td><td>Add 9th chord</td></tr>
     * <tr><td>madd9</td><td>Minor add 9th chord</td></tr>
     * <tr><td>69</td><td>6,9th chord</td></tr>
     * <tr><td>m69</td><td>Minor 6,9th chord</td></tr>
     * <tr><td>sus4</td><td>Sus4 chord</td></tr>
     * <tr><td>sus47</td><td>Sus4 7th chord</td></tr>
     * <tr><td>dim</td><td>Diminish chord</td></tr>
     * <tr><td>arg</td><td>Augment chord</td></tr>
     * The 5th and 6th letters (option) means tension notes.<br/>
     * </table>
     * If you want to set "F sharp minor 7th", chordName = "F+m7".
     */
    @Override
    public String getName() {
        int rn = _scaleNotes.get(0) % 12;
        if (_bassNoteOffset == 0) return _noteNames[rn] + _scaleName;
        return _noteNames[rn] + _scaleName + "/" + _noteNames[(rn + _bassNoteOffset) % 12];
    }

    @Override
    public void setName(String str) {
        if (str == null || str.isEmpty()) {
            _scaleName = "";
            _scaleTable = CT_MAJOR;
            this.setRootNote(60);
            return;
        }

        Pattern rex = Pattern.compile("(o[0-9])?([A-Ga-g])([+#\\-b])?([adgimMsru4679]+)?(,([0-9]+[+#\\-]?))?(,([0-9]+[+#\\-]?))?");
        Matcher mat = rex.matcher(str);
        int i;
        if (mat.matches()) {
            _scaleName = str;
            int note = List.of(9, 11, 0, 2, 4, 5, 7).get(String.valueOf(mat.group(2)).toLowerCase().charAt(0) - 'a');
            if (mat.group(3) != null) {
                if (mat.group(3).equals("+") || mat.group(3).equals("#")) note++;
                else if (mat.group(3).equals("-")) note--;
            }
            if (note < 0) note += 12;
            else if (note > 11) note -= 12;
            if (mat.group(1) != null) {
                int oct = Character.digit(mat.group(1).charAt(1), 10);
                if (oct < 0) throw _errorInvalidChordName(str);
                note += oct * 12;
            } else {
                note += 60;
            }

            if (mat.group(4) != null) {
                if (!(_chordTableDictionary.containsKey(mat.group(4))))throw _errorInvalidChordName(str);
                _scaleTable = _chordTableDictionary.get(mat.group(4));
                _scaleName = mat.group(4);
            } else {
                _scaleTable = CT_MAJOR;
                _scaleName = "";
            }
            this.setRootNote(note);
        } else {
            throw _errorInvalidChordName(str);
        }
    }

    /** bass note number, lowest note of "On Chord". */
    @Override
    public int getBassNote() {
        return _scaleNotes.get(0) + _bassNoteOffset;
    }

    @Override
    public void setBassNote(int note) {
        _bassNoteOffset = note - _scaleNotes.get(0);
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param chordName           chord name.
     * @param defaultCenterOctave default center octave, this applies when there are no octave specification.
     * @see #getName
     */
    public Chord(String chordName /* = "" */, int defaultCenterOctave /* = 5 */) {
        super("", defaultCenterOctave);
        this.setName(chordName);
        _bassNoteOffset = 0;
    }

    // operations
    //

    /**
     * copy from another chord
     *
     * @param src another Chord instance copy from
     */
    @Override
    public Scale copyFrom(Scale src) {
        super.copyFrom(src);
        if (src instanceof Chord) {
            _bassNoteOffset = ((Chord) src)._bassNoteOffset;
        }
        return this;
    }

    // errors
    //

    /** Invalid chord name error */
    protected RuntimeException _errorInvalidChordName(String name) {
        return new RuntimeException("Chord; Invalid chord name. '" + name + "'");
    }
}
