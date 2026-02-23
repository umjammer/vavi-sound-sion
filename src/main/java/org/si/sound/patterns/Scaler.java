//
// Pattern generator on scale
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.patterns;

import org.si.sion.utils.Scale;
    

/** Pattern generator on scale */
public class Scaler {

    // variables
    //

    /** scale instance */
    protected Scale _scale;
    /** pattern of scale indexes */
    protected int[] _scaleIndexPattern;
    /** scale index shift */
    protected int _scaleIndexShift;

    private Note[] self;

    // properties
    //

    /** pattern of scale indexes */
    public int[] getPattern() {
        return _scaleIndexPattern;
    }

    public void setPattern(int[] p) {
        if (p == null) {
            self = null;
            return;
        }
        _scaleIndexPattern = p;
        int i, imax = _scaleIndexPattern.length;
        if (self.length < imax) {
            int old = self.length;
            self = new Note[imax];
            for (i = old; i < imax; i++) {
                self[i] = new Note();
            }
        }
//        self.length = imax;
        for (i = 0; i < imax; i++) {
            self[i].note = _scale.getNote(_scaleIndexPattern[i] + _scaleIndexShift);
        }
    }

    /** scale instance */
    public Scale getScale() {
        return _scale;
    }

    public void setScale(Scale s) {
        if (_scale == s) return;
        _scale = (s != null) ? s : new Scale("", 5);
        int i, imax = _scaleIndexPattern.length;
        for (i = 0; i < imax; i++) {
            self[i].note = _scale.getNote(_scaleIndexPattern[i] + _scaleIndexShift);
        }
    }

    /** scale index shift */
    public int getScaleIndex() {
        return _scaleIndexShift;
    }

    public void setScaleIndex(int s) {
        if (_scaleIndexShift == s) return;
        _scaleIndexShift = s;
        int i, imax = self.length;
        for (i = 0; i < imax; i++) {
            self[i].note = _scale.getNote(_scaleIndexPattern[i] + _scaleIndexShift);
        }
    }

    // constructor
    //

    /**
     * constructor
     *
     * @param scale Scale instance.
     */
    public Scaler(Scale scale, int[] pattern) {
        super();
        _scaleIndexShift = 0;
        _scale = scale != null ? scale : new Scale("", 5);
        this.setPattern(pattern);
    }
}
