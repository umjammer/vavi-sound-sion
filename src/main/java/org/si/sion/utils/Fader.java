//
// Fader class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.utils;

import java.util.function.Consumer;


/** Fader class. */
public class Fader {

    // variables
    //

    // end value
    private double _end = 0;
    // increment step
    private double _step = 0;
    // counter
    private int _counter = 0;
    // value
    private double _value = 0;
    // callback function
    private Consumer<Double> _callback = null;

    // properties
    //

    /** is active. */
    public boolean isActive() {
        return (_counter > 0);
    }

    /** is incrementation, */
    public boolean getIsIncrement() {
        return (_step > 0);
    }

    /** controling value. */
    public double getValue() {
        return _value;
    }

    // constructor
    //

    /**
     * constructor.
     *
     * @param valueFrom The starting value.
     * @param valueTo   The value chaging to.
     * @param frames    Changing frames.
     */
    public Fader(Consumer<Double> callback, double valueFrom, double valueTo, int frames) {
        setFade(callback, valueFrom, valueTo, frames);
    }
    
    public Fader() {
        setFade(null, 0, 0, 0);
    }

    // operations
    //

    /**
     * set fading values
     *
     * @param valueFrom The starting value.
     * @param valueTo   The value chaging to.
     * @param frames    Changing frames.
     * @return this instance.
     */
    public Fader setFade(Consumer<Double> callback, double valueFrom, double valueTo, int frames) {
        _value = valueFrom;
        if (frames == 0 || callback == null) {
            _counter = 0;
            return this;
        }
        _callback = callback;
        _end = valueTo;
        _step = (valueTo - valueFrom) / frames;
        _counter = frames;
        _callback.accept(_value);
        return this;
    }

    /**
     * Execute
     *
     * @return Activation changing. returns true when the execution instanceof finished.
     */
    public boolean execute() {
        if (_counter > 0) {
            _value += _step;
            if (--_counter == 0) {
                _value = _end;
                _callback.accept(_end);
                return true;
            } else {
                _callback.accept(_value);
            }
        }
        return false;
    }

    /** Stop fading */
    public void stop() {
        _counter = 0;
    }
}
