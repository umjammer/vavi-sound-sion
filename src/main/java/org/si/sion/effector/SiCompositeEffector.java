//
// SiON effect basic class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Composite effector class. */
public class SiCompositeEffector extends SiEffectBase {

    // variables
    //
    private SiEffectBase[][] _effectorSlot = null;
    private double[][] _buffer = null;
    private double[] _sendLevel = null;
    private double[] _mixLevel = null;

    // properties
    //

    /** effector slot 0 */
    public void setSlot0(SiEffectBase[] list) {
        _effectorSlot[0] = list;
    }
        
    /** effector slot 1 */
    public void setSlot1(SiEffectBase[] list) {
        _effectorSlot[1] = list;
    }

    /** effector slot 2 */
    public void setSlot2(SiEffectBase[] list) {
        _effectorSlot[2] = list;
    }

    /** effector slot 3 */
    public void setSlot3(SiEffectBase[] list) {
        _effectorSlot[3] = list;
    }

    /** effector slot 4 */
    public void setSlot4(SiEffectBase[] list) {
        _effectorSlot[4] = list;
    }

    /** effector slot 5 */
    public void setSlot5(SiEffectBase[] list) {
        _effectorSlot[5] = list;
    }

    /** effector slot 6 */
    public void setSlot6(SiEffectBase[] list) {
        _effectorSlot[6] = list;
    }

    /** effector slot 7 */
    public void setSlot7(SiEffectBase[] list) {
        _effectorSlot[7] = list;
    }

    /** dry level */
    public void setDry(double n) {
        _sendLevel[0] = n;
    }

    /** master output level */
    public void setMasterVolume(double n) {
        _mixLevel[0] = n;
    }

    // constructor
    //

    /** Constructor. do nothing. */
    public SiCompositeEffector() {
    }

    // callback functions
    //

    /** set effect input/output level of one slot */
    public void setLevel(int slotNum, double inputLevel, double outputLevel) {
        _sendLevel[slotNum] = inputLevel;
        _mixLevel[slotNum] = outputLevel;
    }

    @Override
    public void initialize() {
        _effectorSlot = new SiEffectBase[8][];
        _buffer = new double[8][];
        _sendLevel = new double[8];
        _mixLevel = new double[8];
        for (int i = 0; i < 8; i++) {
            _effectorSlot[i] = null;
            _buffer[i] = null;
            _mixLevel[i] = _sendLevel[i] = 1;
        }
    }

    @Override
    public void mmlCallback(double[] args) {
    }

    @Override
    public int prepareProcess() {
        int i, imax, slotNum;
        SiEffectBase[] list;
        for (slotNum = 0; slotNum < 8; slotNum++) {
            if (_effectorSlot[slotNum] != null) {
                list = _effectorSlot[slotNum];
                imax = list.length;
                for (i = 0; i < imax; i++) list[i].prepareProcess();
            }
        }
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        int i, j, imax, slotNum, ch;
        SiEffectBase[] list;
        double[] str = null;
        double lvl;
        for (slotNum = 1; slotNum < 8; slotNum++) {
            if (_effectorSlot[slotNum] != null) {
                str = _buffer[slotNum];
                lvl = _sendLevel[slotNum];
                if (str.length < buffer.length) str = new double[buffer.length];
                for (i = 0, j = startIndex; i < length; i++, j++) str[j] = buffer[j] * lvl;
            }
        }
        lvl = _sendLevel[0];
        for (i = 0, j = startIndex; i < length; i++, j++) buffer[j] *= lvl;
        for (slotNum = 1; slotNum < 8; slotNum++) {
            if (_effectorSlot[slotNum] != null) {
                ch = channels;
                list = _effectorSlot[slotNum];
                imax = list.length;
                for (i = 0; i < imax; i++) ch = list[i].process(ch, _buffer[slotNum], startIndex, length); // TODO
                lvl = _mixLevel[slotNum];
                for (i = 0, j = startIndex; i < length; i++, j++) buffer[j] += str[j] * lvl;
            }
        }
        if (_effectorSlot[0] != null) {
            list = _effectorSlot[0];
            imax = list.length;
            for (i = 0; i < imax; i++) channels = list[i].process(channels, buffer, startIndex, length);
            if (_mixLevel[0] != 1) {
                lvl = _mixLevel[0];
                for (i = 0, j = startIndex; i < length; i++, j++) buffer[j] *= lvl;
            }
        }

        return channels;
    }
}
