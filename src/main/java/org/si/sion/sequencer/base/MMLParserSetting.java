//
// MML parser setting class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;


/**
 * Information for MMLParser
 *
 * @see org.si.sion.sequencer.base.MMLParser
 */
public class MMLParserSetting {

    // variables
    //

    /** Resolution of note length. 'resolution/4' is a length of a beat. */
    public int resolution;
    private int _mml2nn;
    /** Default value of beat per minutes. */
    public double defaultBPM;

    /** Default value of the l command. */
    public int defaultLValue;
    /** Minimum ratio of the q command. */
    public int minQuantRatio;
    /** Maximum ratio of the q command. */
    public int maxQuantRatio;
    /** Default value of the q command. */
    public int defaultQuantRatio;
    /** Minimum value of the @q command. */
    public int minQuantCount;
    /** Maximum value of the @q command. */
    public int maxQuantCount;
    /** Default value of the @q command. */
    public int defaultQuantCount;
    /** Maximum value of the v command. */
    public int maxVolume;
    /** Default value of the v command. */
    public int defaultVolume;
    /** Maximum value of the @v command. */
    public int maxFineVolume;
    /** Default value of the @v command. */
    public int defaultFineVolume;
    /** Minimum value of the o command. */
    public int minOctave;
    /** Maximum value of the o command. */
    public int maxOctave;
    private int _defaultOctave;

    /** Polarization of the ( and ) command. 1=x68k/-1=pc98. */
    public int volumePolarization;
    /** Polarization of the &lt; and &gt; command. 1=x68k/-1=pc98. */
    public int octavePolarization;

    // properties
    //        

    /** Offset from mml notes to MIDI note numbers. Calculated from defaultOctave. */
    public int getMml2nn() {
        return _mml2nn;
    }

    /** Default value of length in mml event. */
    public int getDefaultLength() {
        return resolution / defaultLValue;
    }

    /** Default value of the o command. */
    public void setDefaultOctave(int o) {
        _defaultOctave = o;
        _mml2nn = 60 - _defaultOctave * 12;
        int octaveLimit = (int) ((128 - _mml2nn) / 12) - 1;
        if (maxOctave > octaveLimit) maxOctave = octaveLimit;
    }

    public int getDefaultOctave() {
        return _defaultOctave;
    }

    // functions
    //

    /**
     * Constructor
     *
     * @param initializer Initializing parameters by Object.
     */
    public MMLParserSetting(Object initializer /* = null */) {
        initialize(initializer);
    }

    /**
     * Initialize. Settings not specified in initializer are ((default) set).
     *
     * @param initializer Initializing parameters by Object.
     */
    public void initialize(Object initializer) {
        resolution = 1920;
        defaultBPM = 120;

        defaultLValue = 4;
        minQuantRatio = 0;
        maxQuantRatio = 8;
        defaultQuantRatio = 10;
        minQuantCount = -192;
        maxQuantCount = 192;
        defaultQuantCount = 0;

        maxVolume = 15;
        defaultVolume = 10;
        maxFineVolume = 127;
        defaultFineVolume = 127;
        minOctave = 0;
        maxOctave = 9;
        setDefaultOctave(5);

        volumePolarization = 1;
        octavePolarization = 1;

        update(initializer);
    }

    /**
     * update. Settings not specifyed in initializer are not changing.
     *
     * @param initializer Initializing parameters by Object.
     */
    public void update(Object initializer) {
        if (initializer == null) return;
        // FIXME: Reflection or Map access is required here.
//        if (initializer.resolution       != undefined) resolution = initializer.resolution;
//        if (initializer.defaultBPM       != undefined) defaultBPM = initializer.defaultBPM;
//
//        if (initializer.defaultLValue     != undefined) defaultLValue = initializer.defaultLValue;
//        if (initializer.minQuantRatio     != undefined) minQuantRatio = initializer.minQuantRatio;
//        if (initializer.maxQuantRatio     != undefined) maxQuantRatio = initializer.maxQuantRatio;
//        if (initializer.defaultQuantRatio != undefined) defaultQuantRatio = initializer.defaultQuantRatio;
//        if (initializer.minQuantCount     != undefined) minQuantCount = initializer.minQuantCount;
//        if (initializer.maxQuantCount     != undefined) maxQuantCount = initializer.maxQuantCount;
//        if (initializer.defaultQuantCount != undefined) defaultQuantCount = initializer.defaultQuantCount;
//
//        if (initializer.maxVolume         != undefined) maxVolume = initializer.maxVolume;
//        if (initializer.defaultVolume     != undefined) defaultVolume = initializer.defaultVolume;
//        if (initializer.maxFineVolume     != undefined) maxFineVolume = initializer.maxFineVolume;
//        if (initializer.defaultFineVolume != undefined) defaultFineVolume = initializer.defaultFineVolume;
//
//        if (initializer.minOctave     != undefined) minOctave = initializer.minOctave;
//        if (initializer.maxOctave     != undefined) maxOctave = initializer.maxOctave;
//        if (initializer.defaultOctave != undefined) defaultOctave = initializer.defaultOctave;
//
//        if (initializer.volumePolarization != undefined) volumePolarization = initializer.volumePolarization;
//        if (initializer.octavePolarization != undefined) octavePolarization = initializer.volumePolarization;
    }
}
