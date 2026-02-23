//
// SiOPM sound module 
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.sion.module.channels.SiOPMChannelManager;
import org.si.utils.SLLint;


/** SiOPM sound module */
public class SiOPMModule {

    // constants
    //

    /** size of stream send */
    public static final int STREAM_SEND_SIZE = 8;
    /** pipe size */
    public static final int PIPE_SIZE = 5;

    // variables
    //

    /** Initial values for operator parameters */
    public SiOPMOperatorParam initOperatorParam;
    /** zero buffer */
    public SLLint zeroBuffer;
    /** output stream */
    public SiOPMStream outputStream;
    /** slot of global mixer */
    public SiOPMStream[] streamSlot;
    /** pcm module volume @default 4 */
    public double pcmVolume;
    /** sampler module volume @default 2 */
    public double samplerVolume;

    private int _bufferLength;  // buffer length
    private int _bitRate;       // bit rate

    // pipes
    private SLLint[] _pipeBuffer;
    private SLLint[][] _pipeBufferPager;

    // properties
    //

    /** Buffer count */
    public double[] getOutput() {
        return outputStream.buffer;
    }

    /** Buffer channel count */
    public int getChannelCount() {
        return outputStream.channels;
    }

    /** Bit rate */
    public int getBitRate() {
        return _bitRate;
    }

    /** Buffer length */
    public int getBufferLength() {
        return _bufferLength;
    }


    // constructor
    //

    /**
     * Default constructor
     *
     * @param busSize Number of mixing buses.
     */
    public SiOPMModule() {
        // initial values
        initOperatorParam = new SiOPMOperatorParam();

        // stream buffer
        outputStream = new SiOPMStream();
        streamSlot = new SiOPMStream[STREAM_SEND_SIZE];

        // zero buffer gives always 0
        zeroBuffer = SLLint.allocRing(1, 0);

        // others
        _bufferLength = 0;
        _pipeBuffer = new SLLint[PIPE_SIZE];
        _pipeBufferPager = new SLLint[PIPE_SIZE][];

        // call at once
        SiOPMChannelManager.initialize(this);
    }

    // operation
    //

    /**
     * Initialize module and all tone generators.
     *
     * @param channelCount ChannelCount
     * @param bitRate      bit rate
     * @param bufferLength Maximum buffer size processing at once.
     */
    public void initialize(int channelCount, int bitRate, int bufferLength) {
        _bitRate = bitRate;

        int i;
        SiOPMStream stream;

        // reset stream slot
        for (i = 0; i < STREAM_SEND_SIZE; i++) streamSlot[i] = null;
        streamSlot[0] = outputStream;

        // reallocate buffer
        if (_bufferLength != bufferLength) {
            _bufferLength = bufferLength;
            outputStream.buffer = new double[bufferLength << 1];
            for (i = 0; i < PIPE_SIZE; i++) {
                SLLint.freeRing(_pipeBuffer[i]);
                _pipeBuffer[i] = SLLint.allocRing(bufferLength, 0);
                _pipeBufferPager[i] = SLLint.createRingPager(_pipeBuffer[i], true);
            }
        }

        pcmVolume = 4;
        samplerVolume = 2;

        // initialize all channels
        SiOPMChannelManager.initializeAllChannels();
    }

    /** Reset. */
    public void reset() {
        // reset all channels
        SiOPMChannelManager.resetAllChannels();
    }

    /** Clear output buffer. */
    public void _beginProcess() {
        outputStream.clear();
    }

    /** Limit output level in the ranged between -1 ~ 1. */
    public void _endProcess() {
        outputStream.limit();
        if (_bitRate != 0) outputStream.quantize(_bitRate);
    }

    /** get pipe buffer */
    public SLLint getPipe(int pipeNum, int index) {
        return _pipeBufferPager[pipeNum][index];
    }
}
