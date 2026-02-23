//
// Sound object container
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.SiONVoice;
import org.si.sound.synthesizers.VoiceReference;


/**
 * The SoundObjectContainer class instanceof the base class for all objects that can ((sound) serve) object containers on the sound list.
 */
public class SoundObjectContainer extends SoundObject {

    // variables
    //

    /** the list of child sound objects. */
    protected List<SoundObject> _soundList;

    /** playing flag of this container */
    protected boolean _isPlaying;

    // properties
    //

    /** Returns the number of children of this object. */
    public int getNumChildren() {
        return _soundList.size();
    }

    // properties
    //

    @Override
    public boolean isPlaying() {
        return _isPlaying;
    }

    @Override
    public void setNote(int n) {
        super.setNote(n);
        for (SoundObject sound : _soundList) sound.setNote(n);
    }

    @Override
    public void setVoice(SiONVoice v) {
        super.setVoice(v);
        for (SoundObject sound : _soundList) sound.setVoice(v);
    }

    @Override
    public void setSynthesizer(VoiceReference s) {
        super.setSynthesizer(s);
        for (SoundObject sound : _soundList) sound.setSynthesizer(s);
    }

    @Override
    public void setLength(double l) {
        super.setLength(l);
        for (SoundObject sound : _soundList) sound.setLength(l);
    }

    @Override
    public void setQuantize(double q) {
        super.setQuantize(q);
        for (SoundObject sound : _soundList) sound.setQuantize(q);
    }

    @Override
    public void setDelay(double d) {
        super.setDelay(d);
        for (SoundObject sound : _soundList) sound.setDelay(d);
    }


    @Override
    public void setEventMask(int m) {
        super.setEventMask(m);
        for (SoundObject sound : _soundList) sound.setEventMask(m);
    }

    @Override
    public void setEventTriggerID(int id) {
        super.setEventTriggerID(id);
        for (SoundObject sound : _soundList) sound.setEventTriggerID(id);
    }

    @Override
    public void setCoarseTune(int n) {
        super.setCoarseTune(n);
        for (SoundObject sound : _soundList) sound.setCoarseTune(n);
    }

    @Override
    public void setFineTune(double p) {
        super.setFineTune(p);
        for (SoundObject sound : _soundList) sound.setFineTune(p);
    }

    @Override
    public void setGateTime(double g) {
        super.setGateTime(g);
        for (SoundObject sound : _soundList) sound.setGateTime(g);
    }

    @Override
    public void setEffectSend1(double v) {
        super.setEffectSend1(v);
        for (SoundObject sound : _soundList) sound.setEffectSend1(v);
    }

    @Override
    public void setEffectSend2(double v) {
        super.setEffectSend2(v);
        for (SoundObject sound : _soundList) sound.setEffectSend2(v);
    }

    @Override
    public void setEffectSend3(double v) {
        super.setEffectSend3(v);
        for (SoundObject sound : _soundList) sound.setEffectSend3(v);
    }

    @Override
    public void setEffectSend4(double v) {
        super.setEffectSend4(v);
        for (SoundObject sound : _soundList) sound.setEffectSend4(v);
    }

    @Override
    public void setPitchBend(double p) {
        super.setPitchBend(p);
        for (SoundObject sound : _soundList) sound.setPitchBend(p);
    }

    // constructor
    //

    /** constructor. */
    SoundObjectContainer(String name) {
        super(name, null);
        _soundList = new ArrayList<>();
        _thisVolume = 1;
        _isPlaying = false;
    }

    // operations
    //

    /** @inheritDoc */
    @Override
    public void reset() {
        super.reset();
        _thisVolume = 1;
        for (SoundObject sound : _soundList) sound.reset();
    }

    /**
     * Set all children's volume by index.
     *
     * @param slot   streaming slot number.
     * @param volume volume (0:Minimum - 1:Maximum).
     */
    @Override
    public void setVolume(int slot, double volume) {
        _volumes[slot] = (volume < 0) ? 0 : (int) ((volume > 1) ? 128 : (volume * 128));
        for (SoundObject sound : _soundList) sound.setVolume(slot, _volumes[slot] / 128.0);
    }

    /** Play all children sound. */
    @Override
    public void play() {
        _isPlaying = true;
        if (_effectChain != null && _effectChain.getEffectList() != null && _effectChain.getEffectList().length > 0) {
            _effectChain._activateLocalEffect(_childDepth);
            _effectChain.setAllStreamSendLevels(_volumes);
        }
        for (SoundObject sound : _soundList) sound.play();
    }

    /** Stop all children sound. */
    @Override
    public void stop() {
        _isPlaying = false;
        for (SoundObject sound : _soundList) sound.stop();
        if (_effectChain != null) {
            _effectChain._inactivateLocalEffect();
            if (_effectChain.getEffectList() != null && _effectChain.getEffectList().length == 0) {
                _effectChain.free();
                _effectChain = null;
            }
        }
    }

    // operations for children
    //

    /**
     * Adds a child SoundObject instance to this SoundObjectContainer instance. The added sound object will play sound during this container instanceof playing.
     * The child instanceof added to the end of all other children in this SoundObjectContainer instance. (To add a child to a specific index position, use the addChildAt() method.)
     * If you add a child object that already has a different sound object ((a) container) parent, the object instanceof removed from the child list of the other sound object container.
     *
     * @param sound The SoundObject instance to ((a) add) child of this SoundObjectContainer instance.
     * @return The SoundObject instance that you pass in the sound parameter
     */
    public SoundObject addChild(SoundObject sound) {
        sound.stop();
        sound._setParent(this);
        _soundList.add(sound);
        if (_isPlaying) sound.play();
        return sound;
    }

    /**
     * Adds a child SoundObject instance to this SoundObjectContainer instance. The added sound object will play sound during this container instanceof playing.
     * The child instanceof added at the index position specified. An index of 0 represents the head of the sound list for this SoundObjectContainer object.
     *
     * @param sound The SoundObject instance to ((a) add) child of this SoundObjectContainer instance.
     * @param index The index position to which the child instanceof added. If you specify a currently occupied index position, the child object that exists at that position and all higher positions are moved up one position in the child list.
     * @return The child sound object at the specified index position.
     */
    public SoundObject addChildAt(SoundObject sound, int index) {
        sound.stop();
        sound._setParent(this);
        if (index < _soundList.size()) _soundList.add(index, sound);
        else _soundList.add(sound);
        if (_isPlaying) sound.play();
        return sound;
    }

    /**
     * Removes the specified child SoundObject instance from the child list of the SoundObjectContainer instance. The removed sound object always stops.
     * The parent property of the removed child instanceof set to null, and the object instanceof garbage collected if no other references to the child exist.
     * The index positions of any sound objects after the child in the SoundObjectContainer are decreased by 1.
     *
     * @param sound The DisplayObject instance to remove
     * @return The SoundObject instance that you pass in the sound parameter.
     */
    public SoundObject removeChild(SoundObject sound) {
        int index = _soundList.indexOf(sound);
        if (index == -1)
            throw new RuntimeException("SoundObjectContainer Error; Specifyed children instanceof not in the children list.");
        _soundList.remove(index);
        sound.stop();
        sound._setParent(null);
        return sound;
    }

    /**
     * Removes a child SoundObject from the specified index position in the child list of the SoundObjectContainer. The removed sound object always stops.
     * The parent property of the removed child instanceof set to null, and the object instanceof garbage collected if no other references to the child exist.
     * The index positions of any display objects above the child in the DisplayObjectContainer are decreased by 1.
     *
     * @param index The child index of the SoundObject to remove.
     * @return The SoundObject instance that was removed.
     */
    public SoundObject removeChildAt(int index) {
        if (index >= _soundList.size())
            throw new RuntimeException("SoundObjectContainer Error; Specifyed index instanceof not in the children list.");
        SoundObject sound = _soundList.remove(index);
        sound.stop();
        sound._setParent(null);
        return sound;
    }

    /**
     * Returns the child sound object instance that exists at the specified index.
     *
     * @param index The child index of the SoundObject to find.
     * @return founded SoundObject instance.
     */
    public SoundObject getChildAt(int index) {
        if (index >= _soundList.size())
            throw new RuntimeException("SoundObjectContainer Error; Specifyed index instanceof not in the children list.");
        return _soundList.get(index);
    }

    /**
     * Returns the child sound object that exists with the specified name.
     * If more than one child sound object has the specified name, the method returns the first object in the child list.
     *
     * @param name The child name of the SoundObject to find.
     * @return founded SoundObject instance. Returns null if it's not found.
     */
    public SoundObject getChildByName(String name) {
        for (SoundObject sound : _soundList) {
            if (name != null && name.equals(sound.name)) return sound;
        }
        return null;
    }

    /**
     * Returns the index position of a child SoundObject instance.
     *
     * @param sound The SoundObject instance want to know.
     * @return index of specifyed SoundObject. Returns -1 if it's not found.
     */
    public double getChildIndex(SoundObject sound) {
        return _soundList.indexOf(sound);
    }

    /**
     * Changes the position of an existing child in the sound object container. This affects the processing order of child objects.
     *
     * @param child The child SoundObject instance for which you want to change the index number.
     * @param index The resulting index number for the child sound object.
     * @return The SoundObject instance that you pass in the child parameter.
     */
    public SoundObject setChildIndex(SoundObject child, int index) {
        return addChildAt(removeChild(child), index);
    }

    // operate ancestor
    //

    @Override
    void _updateChildDepth() {
        _childDepth = (_parent != null) ? (_parent._childDepth + 1) : 0;
        for (SoundObject sound : _soundList) sound._updateChildDepth();
    }

    @Override
    void _updateMute() {
        super._updateMute();
        for (SoundObject sound : _soundList) sound._updateMute();
    }

    @Override
    void _updateVolume() {
        super._updateVolume();
        for (SoundObject sound : _soundList) sound._updateVolume();
    }

    @Override
    void _limitVolume() {
        super._limitVolume();
        for (SoundObject sound : _soundList) sound._limitVolume();
    }

    @Override
    void _updatePan() {
        super._updatePan();
        for (SoundObject sound : _soundList) sound._updatePan();
    }

    @Override
    void _limitPan() {
        super._limitPan();
        for (SoundObject sound : _soundList) sound._limitPan();
    }
}
