// Voice reference
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import java.util.List;

import org.si.sion.SiONVoice;
import org.si.sion.sequencer.SiMMLTrack;


/** Voice reference, basic class of all synthesizers. */
public class VoiceReference {

    // variables
    //

    /** Instance of voice setting */
    SiONVoice _voice = null;

    /** require voice update number */
    public int _voiceUpdateNumber;

    // properties
    //

    /** voice setting */
    public SiONVoice getVoice() {
        return _voice;
    }

    public void setVoice(SiONVoice v) {
        if (_voice != v) _voiceUpdateNumber++;
        _voice = v;
    }

    // constructor
    //

    /** constructor */
    public VoiceReference() {
        _voiceUpdateNumber = 0;
    }

    // operation
    //

    /** register single track */
    public void _registerTrack(SiMMLTrack track) {
    }

    /** register prural tracks */
    public void _registerTracks(List<SiMMLTrack> tracks) {
    }

    /** unregister tracks */
    public void _unregisterTracks(SiMMLTrack firstTrack, int count /* = 1 */) {
    }
}
