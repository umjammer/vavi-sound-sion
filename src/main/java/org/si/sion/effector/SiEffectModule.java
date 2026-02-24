//
// SiON Effect Module
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.si.sion.module.SiOPMModule;


/** Effect Module. */
public class SiEffectModule {

    // constant
    //

    // variables
    //
    private final SiOPMModule _module;
    private final List<SiEffectStream> _freeEffectStreams;
    private final List<SiEffectStream> _localEffects;
    private final SiEffectStream[] _globalEffects;
    private final SiEffectStream _masterEffect;
    private int _globalEffectCount;
    private static final Map<String, EffectorInstances> _effectorInstances = new HashMap<>();

    // properties
    //

    /** Number of global effect */
    public int getGlobalEffectCount() {
        return _globalEffectCount;
    }

    /** effector slot 0 */
    public void setSlot0(SiEffectBase[] list) {
        setEffectorList(0, list);
    }

    /** effector slot 1 */
    public void setSlot1(SiEffectBase[] list) {
        setEffectorList(1, list);
    }

    /** effector slot 2 */
    public void setSlot2(SiEffectBase[] list) {
        setEffectorList(2, list);
    }

    /** effector slot 3 */
    public void setSlot3(SiEffectBase[] list) {
        setEffectorList(3, list);
    }

    /** effector slot 4 */
    public void setSlot4(SiEffectBase[] list) {
        setEffectorList(4, list);
    }

    /** effector slot 5 */
    public void setSlot5(SiEffectBase[] list) {
        setEffectorList(5, list);
    }

    /** effector slot 6 */
    public void setSlot6(SiEffectBase[] list) {
        setEffectorList(6, list);
    }

    /** effector slot 7 */
    public void setSlot7(SiEffectBase[] list) {
        setEffectorList(7, list);
    }

    // constructor
    //

    /** Constructor. */
    public SiEffectModule(SiOPMModule module) {
        _module = module;
        _freeEffectStreams = new ArrayList<>();
        _localEffects = new ArrayList<>();
        _globalEffects = new SiEffectStream[SiOPMModule.STREAM_SEND_SIZE];
        _masterEffect = new SiEffectStream(_module, _module.outputStream);
        _globalEffects[0] = _masterEffect;
        _globalEffectCount = 0;

        // initialize table
        SiEffectTable dummy = SiEffectTable.getInstance();

        // register default effectors
        register("ws", SiEffectWaveShaper.class);
        register("eq", SiEffectEqualiser.class);
        register("delay", SiEffectStereoDelay.class);
        register("reverb", SiEffectStereoReverb.class);
        register("chorus", SiEffectStereoChorus.class);
        register("autopan", SiEffectAutoPan.class);
        register("ds", SiEffectDownSampler.class);
        register("speaker", SiEffectSpeakerSimulator.class);
        register("comp", SiEffectCompressor.class);
        register("dist", SiEffectDistortion.class);
        register("stereo", SiEffectStereoExpander.class);
        register("vowel", SiFilterVowel.class);

        register("lf", SiFilterLowPass.class);
        register("hf", SiFilterHighPass.class);
        register("bf", SiFilterBandPass.class);
        register("nf", SiFilterNotch.class);
        register("pf", SiFilterPeak.class);
        register("af", SiFilterAllPass.class);
        register("lb", SiFilterLowBoost.class);
        register("hb", SiFilterHighBoost.class);

        register("nlf", SiCtrlFilterLowPass.class);
        register("nhf", SiCtrlFilterHighPass.class);
    }

    // operations
    //

    /**
     * Initialize all effectors. This function instanceof called from SiONDriver.play() with the 2nd argment true.
     * When you want to connect effectors by code, you have to call this first, then call connect() and SiONDriver.play() with the 2nd argment false.
     */
    public void initialize() {
        int i;

        // local effects
        for (SiEffectStream es : _localEffects) {
            es.free();
            _freeEffectStreams.add(es);
        }
        _localEffects.clear();

        // global effects
        for (i = 1; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
            if (_globalEffects[i] != null) {
                _globalEffects[i].free();
                _freeEffectStreams.add(_globalEffects[i]);
                _globalEffects[i] = null;
            }
        }
        _globalEffectCount = 0;

        // master effect
        _masterEffect.initialize(0);
        _globalEffects[0] = _masterEffect;
    }

    /** reset all buffers */
    public void _reset() {
        int i;

        // local effects
        for (SiEffectStream es : _localEffects) es.reset();

        // global effects
        for (i = 1; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
            if (_globalEffects[i] != null) _globalEffects[i].reset();
        }

        // master effect
        _masterEffect.reset();
        _globalEffects[0] = _masterEffect;
    }

    /** prepare for processing. */
    public void _prepareProcess() {
        int slot, channelCount, slotMax = _localEffects.size();

        // do nothing on local effect

        // global effect (slot1-slot7)
        _globalEffectCount = 0;
        for (slot = 1; slot < SiOPMModule.STREAM_SEND_SIZE; slot++) {
            _module.streamSlot[slot] = null; // reset module's stream slot
            if (_globalEffects[slot] != null) {
                channelCount = _globalEffects[slot].prepareProcess();
                if (channelCount > 0) {
                    _module.streamSlot[slot] = _globalEffects[slot]._stream;
                    _globalEffectCount++;
                }
            }
        }

        // master effect (slot0)
        _masterEffect.prepareProcess();
    }

    /** Clear output buffer. */
    public void _beginProcess() {
        int slot, leLength = _localEffects.size();

        // local effect
        for (slot = 0; slot < leLength; slot++) {
            _localEffects.get(slot)._stream.clear();
        }

        // global effect (slot1-slot7)
        for (slot = 1; slot < SiOPMModule.STREAM_SEND_SIZE; slot++) {
            if (_globalEffects[slot] != null) _globalEffects[slot]._stream.clear();
        }

        // do nothing on master effect
    }

    /** processing. */
    public void _endProcess() {
        int i, slot, leLength = _localEffects.size();
        double[] buffer;
        SiEffectStream effect;
        int bufferLength = _module.getBufferLength();
        double[] output = _module.getOutput();
        int imax = output.length;

        // local effect
        for (slot = 0; slot < leLength; slot++) {
            _localEffects.get(slot).process(0, bufferLength, true);
        }

        // global effect (slot1-slot7)
        for (slot = 1; slot < SiOPMModule.STREAM_SEND_SIZE; slot++) {
            effect = _globalEffects[slot];
            if (effect != null) {
                if (effect.getOutputDirectly()) {
                    effect.process(0, bufferLength, false);
                    buffer = effect._stream.buffer;
                    for (i = 0; i < imax; i++) output[i] += buffer[i];
                } else {
                    effect.process(0, bufferLength, true);
                }
            }
        }

        // master effect (slot0)
        _masterEffect.process(0, bufferLength, false);
    }

    // effector instance manager
    //

    /**
     * Register effector class
     *
     * @param name Effector name.
     * @param cls  SiEffectBase based class.
     */
    public void register(String name, Class<? extends SiEffectBase> cls) {
        _effectorInstances.put(name, new EffectorInstances(cls));
    }

    /**
     * Get effector instance by name
     *
     * @param name Effector name in mml.
     */
    public static SiEffectBase getInstance(String name) {
        if (!(_effectorInstances.containsKey(name))) return null;

        EffectorInstances factory = _effectorInstances.get(name);
        for (SiEffectBase effect : factory._instances) {
            if (effect._isFree) {
                effect._isFree = false;
                effect.initialize();
                return effect;
            }
        }
        SiEffectBase effect;
        try {
            effect = factory._classInstance.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        factory._instances.add(effect);

        effect._isFree = false;
        effect.initialize();
        return effect;
    }

    // effector connection
    //

    /**
     * Clear effector slot.
     *
     * @param slot Effector slot number.
     */
    public void clear(int slot) {
        if (slot == 0) {
            _masterEffect.initialize(0);
        } else {
            if (_globalEffects[slot] != null) _freeEffectStreams.add(_globalEffects[slot]);
            _globalEffects[slot] = null;
        }
    }

    /**
     * Get effector list of specified slot
     *
     * @param slot Effector slot number.
     * @return Vector of Effector list.
     */
    public List<SiEffectBase> getEffectorList(int slot) {
        if (_globalEffects[slot] == null) return null;
        return _globalEffects[slot].chain;
    }

    /**
     * Set effector list of specified slot
     *
     * @param slot Effector slot number.
     * @param list Effector list to set
     */
    public void setEffectorList(int slot, SiEffectBase[] list) {
        SiEffectStream es = _globalEffector(slot);
        es.chain = Arrays.asList(list);
        es.prepareProcess();
    }

    /**
     * Connect effector to the global/master slot.
     *
     * @param slot     Effector slot number.
     * @param effector Effector instance.
     */
    public void connect(int slot, SiEffectBase effector) {
        _globalEffector(slot).chain.add(effector);
        effector.prepareProcess();
    }

    /**
     * Parse MML for global/master effectors
     *
     * @param slot    Effector slot number.
     * @param mml     MML string.
     * @param postfix Postfix string.
     */
    public void parseMML(int slot, String mml, String postfix) {
        _globalEffector(slot).parseMML(slot, mml, postfix);
    }

    /** Create new local effector connector. deeper effectors executes first. */
    public SiEffectStream newLocalEffect(int depth, List<SiEffectBase> list) {
        SiEffectStream inst = _allocStream(depth);
        inst.chain = list;
        inst.prepareProcess();
        if (depth == 0) {
            _localEffects.add(inst);
            return inst;
        } else {
            for (int slot = _localEffects.size() - 1; slot >= 0; --slot) {
                if (_localEffects.get(slot)._depth >= depth) {
                    _localEffects.add(slot, inst);
                    return inst;
                }
            }
        }
        _localEffects.add(0, inst);
        return inst;
    }


    /** Delete local effector connector */
    public void deleteLocalEffect(SiEffectStream inst) {
        int i = _localEffects.indexOf(inst);
        if (i != -1) _localEffects.remove(i);
        _freeEffectStreams.add(inst);
    }


    // get and alloc SiEffectStream if its null
    private SiEffectStream _globalEffector(int slot) {
        if (_globalEffects[slot] == null) {
            SiEffectStream es = _allocStream(0);
            _globalEffects[slot] = es;
            _module.streamSlot[slot] = es._stream;
            _globalEffectCount++;
        }
        return _globalEffects[slot];
    }

    // functor
    //
    private SiEffectStream _allocStream(int depth) {
        SiEffectStream x = _freeEffectStreams.isEmpty() ? null : _freeEffectStreams.remove(_freeEffectStreams.size() - 1);
        SiEffectStream es = x != null ? x : new SiEffectStream(_module, null);
        es.initialize(depth);
        return es;
    }
}

// effector instance manager
class EffectorInstances {

    public List<SiEffectBase> _instances = new ArrayList<>();
    public Class<? extends SiEffectBase> _classInstance;

    EffectorInstances(Class<? extends SiEffectBase> cls) {
        _classInstance = cls;
    }
}
