//
// basic class sfor SiOPM wave data
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.utils.Event;
import vavi.media.Sound;

/** basic class for SiOPM wave data */
public class SiOPMWaveBase {

    /** module type */
    public int moduleType;
    // loading target
    protected Sound _loadingTarget;

    /** constructor */
    SiOPMWaveBase(int moduleType) {
        this.moduleType = moduleType;
    }

    /** @private listen sound loading events */
    protected void _listenSoundLoadingEvents(Sound sound) {
        if (sound.bytesTotal == 0 || sound.bytesTotal > sound.bytesLoaded) {
            _loadingTarget = sound;
            // sound.addEventListener(Event.COMPLETE, this::_cmp);
            // sound.addEventListener(ErrorEvent.ERROR, this::_err);
            // sound.addEventListener(SecurityErrorEvent.SECURITY_ERROR, this::_err);
        } else {
            _onSoundLoadingComplete(sound);
        }
    }

    /** */
    protected boolean isSoundLoading() {
        return (_loadingTarget != null);
    }

    /** @private complete event handler */
    protected void _onSoundLoadingComplete(Sound sound) {
    }

    // event handlers
    private void _cmp(Event e) {
        _onSoundLoadingComplete(_loadingTarget);
        _removeAllListeners();
    }

    private void _err(Event e) {
        _removeAllListeners();
    }

    private void _removeAllListeners() {
        // _loadingTarget.removeEventListener(Event.COMPLETE, this::_cmp);
        // _loadingTarget.removeEventListener(ErrorEvent.ERROR, this::_err);
        // _loadingTarget.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, this::_err);
        _loadingTarget = null;
    }
}
