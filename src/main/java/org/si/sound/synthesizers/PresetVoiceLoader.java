// synthsizer with SiONPresetVoice
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import java.util.List;

import org.si.sion.SiONVoice;
import org.si.sion.utils.SiONPresetVoice;


/** synthesizer with SiONPresetVoice */
public class PresetVoiceLoader extends VoiceReference {

    // variables
    //

    /** current category's list */
    protected List<SiONVoice> _voiceList;
    /** current voice number */
    protected int _voiceNumber = 0;

    // properties
    //

    /** load voice from current categoly's voice list */
    public int getVoiceNumber() {
        return _voiceNumber;
    }

    public void setVoiceNumber(int i) {
        if (i < 0) i = 0;
        else if (i >= _voiceList.size()) i = _voiceList.size() - 1;
        _voiceNumber = i;
        SiONVoice v = _voiceList.get(_voiceNumber);
        if (_voice != v) _voiceUpdateNumber++;
        _voice = v;
    }

    /** maximum value of voiceNumber */
    public int getVoiceNumberMax() {
        return _voiceList.size();
    }

    // constructor
    //

    /** constructor, set category key to use. */
    public PresetVoiceLoader(String category) {
        SiONPresetVoice presetVoiceList = SiONPresetVoice.getMutex() != null ? SiONPresetVoice.getMutex() : new SiONPresetVoice(0xffff);
        if (!(presetVoiceList.containsKey(category)))
            throw new RuntimeException("PresetVoiceReference; no '" + category + "' categories in SiONPresetVoice.");
        _voiceList = (List<SiONVoice>) presetVoiceList.get(category);
    }
}
