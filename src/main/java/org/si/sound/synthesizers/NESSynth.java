// Nintendo Entertainment System (Family Computer) Synthesizer 
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.synthesizers;

import org.si.sion.sequencer.SiMMLEnvelopTable;
import org.si.utils.SLLint;


/**
 * Nintendo Entertainment System (Family Computer) Synthesizer
 */
public class NESSynth extends BasicSynth {

    // variables
    //

    // properties
    //

    /** APU channel number */
    public int getChannelNumber() {
        return _voice.channelNum;
    }

    // constructor
    //

    /** constructor */
    public NESSynth(int channelNumber) {
        super(1, channelNumber, 63, 63, 0);
    }

    // operation
    //

    /**
     * set envelop table
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setEnevlop(Object[] table, int loopPoint, int step) {
        _voice.noteOnAmplitudeEnvelop = _constructEnvelopTable(_voice.noteOnAmplitudeEnvelop, table, loopPoint);
        _voice.noteOnAmplitudeEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    /**
     * set pitch envelop table
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setPitchEnevlop(Object[] table, int loopPoint, int step) {
        _voice.noteOnPitchEnvelop = _constructEnvelopTable(_voice.noteOnPitchEnvelop, table, loopPoint);
        _voice.noteOnPitchEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    /**
     * set note envelop table
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setNoteEnevlop(Object[] table, int loopPoint, int step) {
        _voice.noteOnNoteEnvelop = _constructEnvelopTable(_voice.noteOnNoteEnvelop, table, loopPoint);
        _voice.noteOnNoteEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    /**
     * set tone envelop table
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setToneEnevlop(Object[] table, int loopPoint, int step) {
        _voice.noteOnToneEnvelop = _constructEnvelopTable(_voice.noteOnToneEnvelop, table, loopPoint);
        _voice.noteOnToneEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    /**
     * set envelop table after note off
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setEnevlopNoteOff(Object[] table, int loopPoint, int step) {
        _voice.noteOffAmplitudeEnvelop = _constructEnvelopTable(_voice.noteOffAmplitudeEnvelop, table, loopPoint);
        _voice.noteOffAmplitudeEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    /**
     * set pitch envelop table after note off
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setPitchEnevlopNoteOff(Object[] table, int loopPoint, int step) {
        _voice.noteOffPitchEnvelop = _constructEnvelopTable(_voice.noteOffPitchEnvelop, table, loopPoint);
        _voice.noteOffPitchEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    /**
     * set note envelop table after note off
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setNoteEnevlopNoteOff(Object[] table, int loopPoint, int step) {
        _voice.noteOffNoteEnvelop = _constructEnvelopTable(_voice.noteOffNoteEnvelop, table, loopPoint);
        _voice.noteOffNoteEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    /**
     * set tone envelop table after note off
     *
     * @param table     envelop table, null sets envelop off.
     * @param loopPoint index of looping point, -1 sets loop at tail.
     * @param step      envelop changing step, 1 sets 60fps, 2 sets 30fps...
     */
    public void setToneEnevlopNoteOff(Object[] table, int loopPoint, int step) {
        _voice.noteOffToneEnvelop = _constructEnvelopTable(_voice.noteOffToneEnvelop, table, loopPoint);
        _voice.noteOffToneEnvelopStep = step;
        _voiceUpdateNumber++;
    }

    // private functions
    //
    private SiMMLEnvelopTable _constructEnvelopTable(SiMMLEnvelopTable env, Object[] table, int loopPoint) {
        if (env != null) env.free();
        if (table == null) return null;

        SLLint tail;
        SLLint head, loop;
        int i, imax = table.length;
        head = tail = SLLint.allocList(imax, 0);
        loop = null;
        for (i = 0; i < imax - 1; i++) {
            if (loopPoint == i) loop = tail;
            tail.i = (int) table[i];
            tail = tail.next;
        }
        tail.i = (int) table[i];
        tail.next = loop;

        if (env == null) env = new SiMMLEnvelopTable();
        env.head = head;
        env.tail = tail;
        return env;
    }
}
