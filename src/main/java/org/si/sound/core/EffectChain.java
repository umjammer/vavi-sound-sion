//
// Effector chain class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.si.sion.SiONDriver;
import org.si.sion.effector.SiEffectBase;
import org.si.sion.effector.SiEffectStream;
import org.si.sion.module.SiOPMStream;


/** Effector chain class. This class manages local effector chain of SoundObject. */
public class EffectChain {

    // variables
    //

    /** Stream buffer of local effect */
    protected SiEffectStream _effectStream;
    /** Effect list */
    protected SiEffectBase[] _effectList;

    // properties
    //

    /** Is processing effect ? */
    public boolean getIsActive() {
        return (_effectStream != null);
    }

    /** effector list */
    public SiEffectBase[] getEffectList() {
        return _effectList;
    }

    public void setEffectList(SiEffectBase[] list) {
        _effectList = list;
        if (_effectStream != null) {
            _effectStream.chain = Arrays.asList(_effectList);
        }
    }

    /** streaming buffer */
    public SiOPMStream getStreamingBuffer() {
        return (_effectStream != null) ? _effectStream.getStream() : null;
    }

    // constructor
    //

    /** @private constructor, you should not create new EffectChain instance. */
    EffectChain(SiEffectBase... list) {
        _effectStream = null;
        _effectList = list != null ? list : new SiEffectBase[0];
    }

    // operations
    //

    /** @private [internal] activate local effect. deeper effectors executes first. */
    public void _activateLocalEffect(int depth) {
        if (_effectStream != null) return;
        SiONDriver driver = SiONDriver.mutex();
        if (driver != null) {
            _effectStream = driver.effector.newLocalEffect(depth, java.util.Arrays.asList(_effectList));
        }
    }

    /** @private [internal] inactivate local effect */
    public void _inactivateLocalEffect() {
        if (_effectStream == null) return;
        SiONDriver driver = SiONDriver.mutex();
        if (driver != null) {
            driver.effector.deleteLocalEffect(_effectStream);
            _effectStream = null;
        }
    }

    /** set all stream send levels by Vector.&lt;int&gt;(8) (0-128) */
    public void setAllStreamSendLevels(int[] volumes) {
        if (_effectStream == null) return;
        _effectStream.setAllStreamSendLevels(volumes);
    }

    /** set stream send level by (double)(0-1) */
    public void setStreamSend(int slot, double volume) {
        if (_effectStream == null) return;
        _effectStream.setStreamSend(slot, volume);
    }

    /** connect to another chain */
    public void connectTo(EffectChain ec) {
        if (_effectStream == null) return;
        _effectStream.connectTo(ec.getStreamingBuffer());
    }

    // factory
    //
    private static final List<EffectChain> _freeList = new ArrayList<>();

    /** allocate new EffectChain */
    public static EffectChain alloc(SiEffectBase[] effectList) {
        if (effectList == null || effectList.length == 0) return null;
        EffectChain ec = _freeList.isEmpty() ? new EffectChain() : _freeList.remove(_freeList.size() - 1);
        ec.setEffectList(effectList);
        return ec;
    }

    public static void free(EffectChain ec) {
        if (ec != null) {
            ec.setEffectList(null);
            _freeList.add(ec);
        }
    }

    public void free() {
        EffectChain.free(this);
    }
}
