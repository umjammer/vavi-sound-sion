//
// SiON Effect serial connector
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;

import java.util.ArrayList;
import java.util.List;

import org.si.sion.module.SiOPMModule;
import org.si.sion.module.SiOPMStream;


/** SiON Effector stream. */
public class SiEffectStream {

    // variables
    //

    /** effector chain */
    public List<SiEffectBase> chain = new ArrayList<>();

    /** @private [internal] streaming buffer */
    SiOPMStream _stream;
    /** @private [internal] depth. deeper stream execute first. */
    int _depth;

    // module
    private final SiOPMModule _module;
    // panning
    private int _pan;
    // has effect send
    private boolean _hasEffectSend;
    // streaming level
    private final double[] _volumes = new double[SiOPMModule.STREAM_SEND_SIZE];
    // output streams
    private final SiOPMStream[] _outputStreams = new SiOPMStream[SiOPMModule.STREAM_SEND_SIZE];

    // properties
    //

    /** stream buffer */
    public SiOPMStream getStream() {
        return _stream;
    }

    /** panning of output (-64:L - 0:C - 64:R). */
    public int getPan() {
        return _pan - 64;
    }

    public void setPan(int p) {
        _pan = p + 64;
        if (_pan < 0) _pan = 0;
        else if (_pan > 128) _pan = 128;
    }

    /** flag to write output stream directly */
    boolean getOutputDirectly() {
        return (!_hasEffectSend && _volumes[0] == 1 && _pan == 64);
    }

    // constructor
    //

    /** Constructor, you should not create new EffectStream, you may call SiEffectModule.newLocalEffect() for these purpose. */
    // the 2nd argument is for MasterEffect to operate master output.
    public SiEffectStream(SiOPMModule module, SiOPMStream stream /* = null */) {
        _depth = 0;
        _module = module;
        _stream = stream != null ? stream : new SiOPMStream();
    }

    // setting
    //

    /**
     * set all stream send levels by Vector.&lt;int&gt;.
     *
     * @param param Vector.&lt;int&gt;(8) of all volumes[0-128].
     */
    public void setAllStreamSendLevels(int[] param) {
        int i, imax = SiOPMModule.STREAM_SEND_SIZE, v;
        for (i = 0; i < imax; i++) {
            v = param[i];
            _volumes[i] = (v != Integer.MIN_VALUE) ? (v * 0.0078125) : 0;
        }
        for (_hasEffectSend = false, i = 1; i < imax; i++) {
            if (_volumes[i] > 0) _hasEffectSend = true;
        }
    }

    /**
     * set stream send.
     *
     * @param streamNum stream number[0-7]. The streamNum of 0 means master volume.
     * @param volume    send level[0-1].
     */
    public void setStreamSend(int streamNum, double volume) {
        _volumes[streamNum] = volume;
        if (streamNum == 0) return;
        if (volume > 0) _hasEffectSend = true;
        else {
            int i, imax = SiOPMModule.STREAM_SEND_SIZE;
            for (_hasEffectSend = false, i = 1; i < imax; i++) {
                if (_volumes[i] > 0) _hasEffectSend = true;
            }
        }
    }

    /**
     * get stream send.
     *
     * @param streamNum stream number[0-7]. The streamNum of 0 means master volume.
     * @return send level[0-1].
     */
    public double getStreamSend(int streamNum) {
        return _volumes[streamNum];
    }

    // operations
    //

    /** initialize, called when allocated */
    public void initialize(int depth) {
        free();
        reset();
        for (int i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
            _volumes[i] = 0;
            _outputStreams[i] = null;
        }
        _volumes[0] = 1;
        _pan = 64;
        _hasEffectSend = false;
        _depth = depth;
    }

    /** reset all parameters except for effector chain, called when effector module instanceof initialized */
    public void reset() {
        if (_stream.buffer == null || _stream.buffer.length != (_module.getBufferLength() << 1)) {
            _stream.buffer = new double[_module.getBufferLength() << 1];
        }
        _stream.clear();
    }

    /** free all effector chain, called when effector module instanceof initialized */
    public void free() {
        for (SiEffectBase e : chain) e._isFree = true;
        chain.clear();
    }

    /**
     * connect to another stream
     *
     * @param output stream connect to.
     */
    public void connectTo(SiOPMStream output) {
        _outputStreams[0] = output;
    }

    /** prepare for process */
    public int prepareProcess() {
        if (chain.isEmpty()) return 0;
        _stream.channels = chain.get(0).prepareProcess();
        for (int i = 1; i < chain.size(); i++) chain.get(i).prepareProcess();
        return _stream.channels;
    }

    /** processing */
    public int process(int startIndex, int length, boolean writeInStream /* = true */) {
        int i, imax;
        SiEffectBase effect;
        SiOPMStream stream;
        double[] buffer = _stream.buffer;
        int channels = _stream.channels;
        double[] bufferArray = new double[buffer.length];
        for (i = 0; i < buffer.length; i++) bufferArray[i] = buffer[i];
        imax = chain.size();
        for (i = 0; i < imax; i++) {
            channels = chain.get(i).process(channels, bufferArray, startIndex, length);
        }
        for (i = 0; i < buffer.length; i++) buffer[i] = bufferArray[i];

        // write in stream buffer
        if (writeInStream) {
            if (_hasEffectSend) {
                for (i = 0; i < SiOPMModule.STREAM_SEND_SIZE; i++) {
                    if (_volumes[i] > 0) {
                        stream = _outputStreams[i] != null ? _outputStreams[i] : _module.streamSlot[i];
                        if (stream != null)
                            stream.writeVectorNumber(buffer, startIndex, startIndex, length, _volumes[i], _pan, 2);
                    }
                }
            } else {
                stream = _outputStreams[0] != null ? _outputStreams[0] : _module.outputStream;
                stream.writeVectorNumber(buffer, startIndex, startIndex, length, _volumes[0], _pan, 2);
            }
        }

        return channels;
    }

    // effector connection
    //

    /**
     * Parse MML for effector
     *
     * @param mml     MML string.
     * @param postfix Postfix string.
     */
    public void parseMML(int slot, String mml, String postfix) {
        String cmd = "";
        double[] args = new double[16];
        int[] argc = {0};

        java.util.regex.Pattern rexMML = java.util.regex.Pattern.compile("([a-zA-Z]+)([,0-9.\\-]+)?");
        java.util.regex.Pattern rexPost = java.util.regex.Pattern.compile("([@a-zA-Z]+)([,0-9.\\-]+)?");

        Runnable _clearArgs = () -> {
            for (int i = 0; i < 16; i++) args[i] = Double.NaN;
            argc[0] = 0;
        };

        Runnable _connectEffect = () -> {
            if (argc[0] == 0) return;
            // SiEffectBase e = SiEffectModule.getInstance(cmd); // FIXME: getInstance is not static
            SiEffectBase e = null; // Temporary fix to allow compilation
            if (e != null) {
                List<Double> argsList = new ArrayList<>();
                for (int j = 0; j < argc[0]; j++) argsList.add(args[j]);
                e.mmlCallback(argsList.stream().mapToDouble(aDouble -> aDouble).toArray());
                chain.add(e);
            }
        };

        String _cmd = cmd;
        Runnable _setVolume = () -> {
            double v;
            if (argc[0] == 0) return;
            switch (_cmd) {
                case "p":
                    _pan = (((int) (args[0])) << 4) - 64;
                    break;
                case "@p":
                    _pan = (int) (args[0]);
                    break;
                case "@v":
                    v = (int) (args[0]) * 0.0078125;
                    setStreamSend(0, (v < 0) ? 0 : (v > 1) ? 1 : v);
                    int lim = argc[0];
                    if (lim + slot >= SiOPMModule.STREAM_SEND_SIZE) lim = SiOPMModule.STREAM_SEND_SIZE - slot - 1;
                    for (int i = 1; i < lim; i++) {
                        v = (int) (args[i]) * 0.0078125;
                        setStreamSend(i + slot, (v < 0) ? 0 : (v > 1) ? 1 : v);
                    }
                    break;
            }
        };

        // clear
        initialize(0);
        _clearArgs.run();

        // parse mml
        if (mml != null) {
            java.util.regex.Matcher resMML = rexMML.matcher(mml);
            while (resMML.find()) {
                if (resMML.group(1).equals(",")) {
                    args[argc[0]++] = Double.parseDouble(resMML.group(2));
                } else {
                    _connectEffect.run();
                    _clearArgs.run();
                    cmd = resMML.group(1);
                    if (resMML.group(2) != null) {
                        args[0] = Double.parseDouble(resMML.group(2));
                        argc[0] = 1;
                    }
                }
            }
        }
        _connectEffect.run();
        _clearArgs.run();

        // parse postfix
        if (postfix != null) {
            java.util.regex.Matcher resPost = rexPost.matcher(postfix);
            while (resPost.find()) {
                if (resPost.group(1).equals(",")) {
                    args[argc[0]++] = Double.parseDouble(resPost.group(2));
                } else {
                    _setVolume.run();
                    _clearArgs.run();
                    cmd = resPost.group(1);
                    if (resPost.group(2) != null) {
                        args[0] = Double.parseDouble(resPost.group(2));
                        argc[0] = 1;
                    }
                }
            }
        }
        _setVolume.run();
    }
}
