//
// SiOPM sound channel manager
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module.channels;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.si.sion.module.SiOPMModule;


/** @private SiOPM sound channel manager */
public class SiOPMChannelManager {

    private static final Logger logger = System.getLogger(SiOPMChannelManager.class.getName());

    // constants
    //

    public static final int CT_CHANNEL_FM = 0;
    public static final int CT_CHANNEL_PCM = 1;
    public static final int CT_CHANNEL_SAMPLER = 2;
    public static final int CT_CHANNEL_KS = 3;
    public static final int CT_MAX = 4;

    // variables
    //

    /** class instance of SiOPMChannelBase */
    private Class<?> _channelClass;
    /** channel type */
    private int _channelType;
    /** terminator */
    private SiOPMChannelBase _term;
    /** channel count */
    private int _length;

    // properties
    //

    /** allocated channel count */
    public int getLength() {
        return _length;
    }

    // constructor
    //

    /** constructor */
    public SiOPMChannelManager(Class<?> channelClass, int channelType) {
        _channelType = channelType;
        _channelClass = channelClass;
        _term = new SiOPMChannelBase(_chip);
        _term._isFree = false;
        _term._next = _term;
        _term._prev = _term;
        _length = 0;
    }

    // operations
    //

    // allocate channels.
    private void _alloc(int count) {
        int i;
        SiOPMChannelBase newInstance;
        int imax = count - _length;
        // allocate new channels
        for (i = 0; i < imax; i++) {
            try {
                newInstance = (SiOPMChannelBase) _channelClass.getDeclaredConstructor(SiOPMModule.class).newInstance(_chip);
                newInstance._channelType = _channelType;
                newInstance._isFree = true;
                newInstance._prev = _term._prev;
                newInstance._next = _term;
                newInstance._prev._next = newInstance;
                newInstance._next._prev = newInstance;
                _length++;
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }

    // get new channel. returns null when the channel count is overflow.
    private SiOPMChannelBase _newChannel(SiOPMChannelBase prev, int bufferIndex) {
        SiOPMChannelBase newChannel = null;
        if (_term._next._isFree) {
            // The head channel is free -> The head will be a new channel.
            newChannel = _term._next;
            newChannel._prev._next = newChannel._next;
            newChannel._next._prev = newChannel._prev;
        } else {
            try {
                // The head channel is active -> channel overflow.
                // create new channel.
                newChannel = (SiOPMChannelBase) _channelClass.getDeclaredConstructor(SiOPMModule.class).newInstance(_chip);
                newChannel._channelType = _channelType;
                _length++;
            } catch (Exception e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        // set newChannel to tail and activate.
        newChannel._isFree = false;
        newChannel._prev = _term._prev;
        newChannel._next = _term;
        newChannel._prev._next = newChannel;
        newChannel._next._prev = newChannel;

        // initialize
        newChannel.initialize(prev, bufferIndex);

        return newChannel;
    }

    // delete channel.
    private void _deleteChannel(SiOPMChannelBase ch) {
        ch._isFree = true;
        ch._prev._next = ch._next;
        ch._next._prev = ch._prev;
        ch._prev = _term;
        ch._next = _term._next;
        ch._prev._next = ch;
        ch._next._prev = ch;
    }

    // initialize all channels
    private void _initializeAll() {
        SiOPMChannelBase ch;
        for (ch = _term._next; ch != _term; ch = ch._next) {
            ch._isFree = true;
            ch.initialize(null, 0);
        }
    }

    // reset all channels
    private void _resetAll() {
        SiOPMChannelBase ch;
        for (ch = _term._next; ch != _term; ch = ch._next) {
            ch._isFree = true;
            ch.reset();
        }
    }

    // factory
    //
    private static SiOPMModule _chip;                               // module instance
    private static SiOPMChannelManager[] _channelManagers;   // manager list

    /** initialize */
    public static void initialize(SiOPMModule chip) {
        _chip = chip;
        _channelManagers = new SiOPMChannelManager[CT_MAX];
        _channelManagers[CT_CHANNEL_FM] = new SiOPMChannelManager(SiOPMChannelFM.class, CT_CHANNEL_FM);
        _channelManagers[CT_CHANNEL_PCM] = new SiOPMChannelManager(SiOPMChannelPCM.class, CT_CHANNEL_PCM);
        _channelManagers[CT_CHANNEL_SAMPLER] = new SiOPMChannelManager(SiOPMChannelSampler.class, CT_CHANNEL_SAMPLER);
        _channelManagers[CT_CHANNEL_KS] = new SiOPMChannelManager(SiOPMChannelKS.class, CT_CHANNEL_KS);
    }

    /** initialize all channels */
    public static void initializeAllChannels() {
        // initialize all channels
        for (SiOPMChannelManager mng : _channelManagers) {
            mng._initializeAll();
        }
    }

    /** reset all channels */
    public static void resetAllChannels() {
        // reset all channels
        for (SiOPMChannelManager mng : _channelManagers) {
            mng._resetAll();
        }
    }

    /** New channel with initializing. */
    public static SiOPMChannelBase newChannel(int type, SiOPMChannelBase prev, int bufferIndex) {
        return _channelManagers[type]._newChannel(prev, bufferIndex);
    }

    /** Free channel. */
    public static void deleteChannel(SiOPMChannelBase channel) {
        _channelManagers[channel._channelType]._deleteChannel(channel);
    }
}
