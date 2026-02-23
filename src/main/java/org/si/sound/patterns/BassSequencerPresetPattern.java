//
// Preset patterns for BassSequencer
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.patterns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Preset patterns for BassSequencer */
public class BassSequencerPresetPattern {

    // variables
    //

    /** Inner class for named lists to match AS3 dynamic array with 'name' property. */
    public static class PatternList extends ArrayList<List<Note>> {
        public String name;
        public PatternList(String name) {
            this.name = name;
        }
    }

    /** category list. */
    public List<PatternList> categories = new ArrayList<>();

    // constructor
    //

    /** constructor */
    public BassSequencerPresetPattern() {
        _category("bass");
        _pattern("bass1", "A^--A^--A^--A^--");
        _pattern("bass2", "A^--A^--A^--A^AA");
        _pattern("bass3", "A^A^--A^--A^--A^");
        _pattern("bass4", "--A^--A^--A^--A^");
        _pattern("bass5", "A-A-A^^^A^^^A^^^");
        _pattern("bass6", "A^A^----A^A^----");
        _pattern("bass7", "A^A^-A^-A^A^-A^-");
        _pattern("bass8", "A^^A^^--A^^A^^--");
        _pattern("bass9", "A--A-A^aA^A^A^--");
        _pattern("bass10", "A^-AA-A^A-A^A^A^");
        _pattern("bass11", "A^aaA^a-AAA--A--");
        _pattern("bass12", "A^aaA^a-AA----aa");
        _pattern("bass13", "A^H^A^H^A^H^A^H^");
        _pattern("bass14", "A^H-A^H-A^H-A^H-");
        _pattern("bass15", "AAH^AAH^AAH^AAH^");
        _pattern("bass16", "AAH^AAHaAAH^AAHa");
        _pattern("bass17", "AA^AA^AA^AA^A^AA");
        _pattern("bass18", "AA-AA-A-AA-AA-A-");
        _pattern("bass19", "AA^AA-A-AA^AA-A-");
        _pattern("bass20", "AA^AA^A^AA^AA^A^");
        _pattern("bass21", "AAaaAAaaAAaaAAaa");
        _pattern("bass22", "AAAAAAAAAAAAAAAA");
        _pattern("bass23", "A^^A--A^^^------");
        _pattern("bass24", "A^-A^-A^--------");
        _pattern("bass25", "A^-A^-A-A^------");
        _pattern("bass26", "--A^-A^^--A^-A^^");
        _pattern("bass27", "--A^^A--A^^A^^A-");
        _pattern("bass28", "--A^-A^-A^-A^-A^");
        _pattern("bass29", "-AA^--A^-AA^--A^");
        _pattern("bass30", "A^A^------------");
        _pattern("bass31", "A^A^-----------a");
    }

    // internals
    //

    // set pattern
    private PMLParser _pp = new PMLParser(Map.of(
            "A", new Note(33, 128, 1, -1, Double.NaN, null),
            "a", new Note(33, 64, 1, -1, Double.NaN, null),
            "H", new Note(45, 128, 1, -1, Double.NaN, null),
            "h", new Note(45, 64, 1, -1, Double.NaN, null)
    ));

    Map<String, List<List<Note>>> self = new HashMap<>();

    private void _pattern(String key, String pml) {
        List<Note> pattern = Arrays.asList(_pp.parse(pml));
        if (_categoryList != null) {
            _categoryList.add(pattern);
        }
        self.put(key, Collections.singletonList(pattern));
    }

    // register category
    private PatternList _categoryList;

    private void _category(String key) {
        _categoryList = new PatternList(key);
        categories.add(_categoryList);
        // In AS3: this[key] = _categoryList;
        self.put(key, _categoryList);
    }

    public List<List<Note>> get(String key) {
        return self.get(key);
    }
}
