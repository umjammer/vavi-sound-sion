//
// Preset voices for DrumMachine
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.si.sion.SiONVoice;


/** Preset voices for DrumMachine, this class can also be synthesizer. */
public class DrumMachinePresetVoice extends VoiceReference {

    // constants
    //

    /** voice type bass drum */
    public static final String BASS = "bass";
    /** voice type snare drum */
    public static final String SNARE = "snare";
    /** voice type hi-hat cymbal */
    public static final String HIHAT = "hihat";
    /** voice type other percussion's */
    public static final String PERCUSSION = "percus";

    // variables
    //
    
    /** Inner class for named lists to match AS3 dynamic array with 'name' property. */
    public static class VoiceList extends ArrayList<SiONVoice> {
        public String name;
        public VoiceList(String name) {
            this.name = name;
        }
    }

    /** categoly list. */
    public List<VoiceList> categories = new ArrayList<>();

    // voice reference
    private List<SiONVoice> _voiceList;

    // variables
    //

    /** bass drum pattern number. */
    public void setType(String typeString) {
        _voiceList = self.get(typeString);
    }

    /** bass drum pattern number. */
    public void setIndex(int index) {
        index <<= 1;
        if (_voiceList == null || index < 0 || index >= _voiceList.size()) return;
        _voice = _voiceList.get(index);
    }

    // constructor
    //

    /** constructor */
    public DrumMachinePresetVoice() {
        // bass drums
        _category("bass");
        _percuss1op("bass1", "1 operator bass drum (sine)", 0, 0, 0, 63, 28, -128);
        _percuss1op("bass1w", "1 operator bass drum (sine) weak", 0, 4, 0, 63, 28, -128);
        _percuss1op("bass2", "1 operator bass drum (triangle)", 0, 0, 3, 63, 36, -32, 80, 0);
        _percuss1op("bass2w", "1 operator bass drum (triangle) weak", 0, 4, 3, 63, 36, -32, 80, 0);
        _percuss1op("bass3", "1 operator bass drum (pulse)", 0, 20, 5, 63, 32, -128);
        _percuss1op("bass3w", "1 operator bass drum (pulse) weak", 0, 24, 5, 63, 32, -128);
        _percuss1op("bass4", "1 operator bass drum (pulse)", 0, 20, 5, 63, 32, -128, 52, 1);
        _percuss1op("bass4w", "1 operator bass drum (pulse) weak", 0, 24, 5, 63, 32, -128, 52, 1);
        _percuss1op("bass5", "1 operator bass drum (saw)", 0, 16, 1, 63, 36, -32, 96);
        _percuss1op("bass5w", "1 operator bass drum (saw) weak", 0, 20, 1, 63, 36, -32, 96);
        _percuss1op("bass6", "1 operator bass drum (noise)", 12, 28, 17, 63, 36, 0);
        _percuss1op("bass6w", "1 operator bass drum (noise) weak", 12, 32, 17, 63, 36, 0);

        // snare drums
        _category("snare");
        _percuss1op("snare1", "1 operator snare drum", 68, 4, 17, 63, 32, 0, 64, 1);
        _percuss1op("snare1w", "1 operator snare drum weak", 68, 8, 17, 63, 34, 0, 64, 1);
        _percuss1op("snare2", "1 operator snare drum", 68, 8, 17, 63, 32, 0, 80, 1);
        _percuss1op("snare2w", "1 operator snare drum weak", 68, 12, 17, 63, 36, 0, 80, 1);
        _percuss1op("snare3", "1 operator snare drum", 48, 10, 19, 63, 32, 0);
        _percuss1op("snare3w", "1 operator snare drum weak", 48, 14, 19, 63, 36, 0);
        _percuss1op("snare4", "1 operator snare drum", 68, 12, 17, 63, 32, 0);
        _percuss1op("snare4w", "1 operator snare drum weak", 68, 16, 17, 63, 36, 0);
        _percuss1op("snare5", "1 operator snare drum", 68, 0, 20, 63, 28, 0, 96, 1);
        _percuss1op("snare5w", "1 operator snare drum weak", 68, 4, 20, 63, 32, 0, 96, 1);
        _percuss1op("snare5", "1 operator snare drum", 68, 4, 16, 63, 28, 0, 54, 4);
        _percuss1op("snare5w", "1 operator snare drum weak", 68, 8, 16, 63, 32, 0, 54, 4);

        // closed hihats
        _category("hihat");
        _percuss1op("closedhh1", "1 operator closed hi-hat", 68, 6, 19, 63, 44, 0);
        _percuss1op("openedhh1", "1 operator opened hi-hat", 68, 10, 19, 63, 28, 0);
        _percuss1op("closedhh1", "1 operator closed hi-hat", 68, 0, 19, 63, 44, 0, 80);
        _percuss1op("openedhh1", "1 operator opened hi-hat", 68, 4, 19, 63, 28, 0, 80);
        _percuss1op("closedhh2", "1 operator closed hi-hat", 88, 0, 24, 63, 44, 0);
        _percuss1op("openedhh2", "1 operator opened hi-hat", 88, 6, 24, 63, 28, 0);
        _percuss1op("closedhh3", "1 operator closed hi-hat", 96, 4, 25, 63, 44, 0);
        _percuss1op("openedhh3", "1 operator opened hi-hat", 96, 12, 25, 63, 28, 0);

        // symbals
        _category("symbal");
        _percuss1op("symbal1", "1 operator crash symbal", 68, 8, 16, 48, 24, 0);

        // others
        _category("percus");

        _voiceList = self.get("bass");
    }

    // internals
    //

    // create new 1operator percussive voice
    private void _percuss1op(String key, String name, int note, int tl, int ws, int ar, int rr, int sw) {
        _percuss1op(key, name, note, tl, ws, ar, rr, sw, 128, 0);
    }

    private void _percuss1op(String key, String name, int note, int tl, int ws, int ar, int rr, int sw, int cut) {
        _percuss1op(key, name, note, tl, ws, ar, rr, sw, cut, 0);
    }

    private void _percuss1op(String key, String name, int note, int tl, int ws, int ar, int rr, int sw, int cut, int res) {
        SiONVoice voice = new SiONVoice(5, ws, ar, rr, 0, -1, 0, 0);
        voice.defaultGateTime = 0;
        if (voice.channelParam.operatorParam.length > 0) {
            voice.channelParam.operatorParam[0].fixedPitch = note << 6;
            voice.channelParam.operatorParam[0].tl = tl;
        }
        voice.releaseSweep = sw;
        voice.setFilterEnvelop(0, cut, res, 0, 0, 0, 0, 0, 0, 0, 128);
        voice.name = name;
        if (_categoryList != null) {
            _categoryList.add(voice);
        }
        self.put(key, Collections.singletonList(voice));
    }

    // register category
    private VoiceList _categoryList;

    Map<String, List<SiONVoice>> self = new HashMap<>();

    private void _category(String key) {
        _categoryList = new VoiceList(key);
        categories.add(_categoryList);
        self.put(key, _categoryList);
    }

    public List<SiONVoice> get(String key) {
        return self.get(key);
    }
}
