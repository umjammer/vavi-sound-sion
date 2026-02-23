//
//  MMLExecutor connector.
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.si.sion.module.channels.SiOPMChannelBase;


/** MML executor connector. this class instanceof used for #FM connection. */
public class MMLExecutorConnector {

    // variables
    //

    private int _sequenceCount;    // sequence count
    private int _executorCount;    // executor count
    private MECElement _firstElem; // first information of connection

    // properties
    //

    /** The count of require executor. */
    public int getExecutorCount() {
        return _executorCount;
    }

    /** The count of require sequence. */
    public int getSequenceCount() {
        return _sequenceCount;
    }

    // constructor
    //
    public MMLExecutorConnector() {
        _firstElem = null;
        _executorCount = 0;
        _sequenceCount = 0;
    }

    // operation
    //

    /** Free all elements. */
    public void clear() {
        _firstElem = null;
        _executorCount = 0;
        _sequenceCount = 0;
    }

    /** Parse connection formula. */
    public void parse(String form) {
        int i, imax;
        MECElement prev = null;
        MECElement elem;
        String alp = "abcdefghijklmnopqrstuvwxyz";
        Pattern rex = Pattern.compile("(\\()?([a-zA-Z])([0-7])?(\\)+)?");

        // initialize
        clear();

        // parse
        Matcher res = rex.matcher(form);
        while (res.matches()) {
            // get current oscillator number
            i = alp.indexOf(res.group(2).toLowerCase());
            if (_sequenceCount <= i) _sequenceCount = i + 1;
            _executorCount++;
            elem = MECElement.alloc(i);
            if (res.group(3) != null) elem.modulation = Integer.parseInt(res.group(3));
            else elem.modulation = 5;

            // modulation start "("
            if (res.group(1) != null) {
                if (prev == null) throw _errorWrongFormula("'(' in " + form);
                prev.firstChild = elem;
                elem.parent = prev;
            } else {
                if (prev != null) {
                    prev.next = elem;
                    elem.parent = prev.parent;
                } else {
                    _firstElem = elem;
                }
            }

            // modulation end ")+"
            if (res.group(4) != null) {
                imax = String.valueOf(res.group(4)).length();
                for (i = 0; i < imax; i++) {
                    if (elem.parent == null) throw _errorWrongFormula("')' in " + form);
                    elem = elem.parent;
                }
            }
            prev = elem;

            res = rex.matcher(form);
        }

        if (prev == null || prev.parent != null) {
            throw _errorWrongFormula(form);
        }
    }

    /** Connect executors. */
    public MMLSequence connect(MMLSequenceGroup seqGroup, MMLSequence prev) {
        // create sequence list
        List<MMLSequence> seqList = new ArrayList<>(_sequenceCount);
        for (int i = 0; i < _sequenceCount; i++) {
            if (prev.getNextSequence() == null) throw _errorSequenceNotEnough();
            seqList.set(i, prev.getNextSequence());
            prev.getNextSequence()._removeFromChain();
        }

        // set executors connections
        _connect(_firstElem, false, -1, seqGroup, seqList, prev);

        return prev;
    }

    // connection sub
    void _connect(MECElement elem, boolean firstOsc, int outPipe, MMLSequenceGroup seqGroup, List<MMLSequence> seqList, MMLSequence prev) {
        int inPipe = 0;
        // modulator before carrior
        if (elem.firstChild != null) {
            inPipe = outPipe + ((firstOsc) ? 0 : 1);
            _connect(elem.firstChild, true, inPipe, seqGroup, seqList, prev);
        }

        // assign sequence to executor
        MMLSequence preprocess = seqGroup._newSequence();
        preprocess.initialize();
//trace("#FM "+elem.number+";");

        // out pipe
        if (outPipe != -1) {
            preprocess.appendNewEvent(MMLEvent.OUTPUT_PIPE, (firstOsc) ? SiOPMChannelBase.OUTPUT_OVERWRITE : SiOPMChannelBase.OUTPUT_ADD, 0);
            preprocess.appendNewEvent(MMLEvent.PARAMETER, outPipe, 0);
//trace(" @o"+((firstOsc)?'1,':'2,')+outPipe);
        } else {
            preprocess.appendNewEvent(MMLEvent.OUTPUT_PIPE, SiOPMChannelBase.OUTPUT_STANDARD, 0);
            preprocess.appendNewEvent(MMLEvent.PARAMETER, 0, 0);
//trace(" @o0,0");
        }

        // in pipe
        if (elem.firstChild != null) {
            preprocess.appendNewEvent(MMLEvent.INPUT_PIPE, elem.modulation, 0);
            preprocess.appendNewEvent(MMLEvent.PARAMETER, inPipe, 0);
//trace(" @i"+elem.modulation+","+inPipe);
        } else {
            preprocess.appendNewEvent(MMLEvent.INPUT_PIPE, 0, 0);
            preprocess.appendNewEvent(MMLEvent.PARAMETER, 0, 0);
//trace(" @i0,0");
        }

        // connect preprocess and main sequence
        preprocess.connectBefore(seqList.get(elem.number).headEvent.next);
        // connect preprocess on sequence chain
        preprocess._insertAfter(prev);
        prev = preprocess;
//trace(preprocess);

        // next oscillator
        if (elem.next != null) _connect(elem.next, false, outPipe, seqGroup, seqList, prev);
    }

    // errors
    //
    private RuntimeException _errorWrongFormula(String form) {
        return new RuntimeException("MMLExecutorConnector error : Wrong connection formula. " + form);
    }

    private RuntimeException _errorSequenceNotEnough() {
        return new RuntimeException("MMLExecutorConnector error: Not enough sequences to connect.");
    }
}

// MMLExecutorConnector element class
class MECElement {

    public int number;
    public int modulation;
    public MECElement parent = null;
    public MECElement next = null;
    public MECElement firstChild = null;

    void MECElement() {
    }

    public MECElement initialize(int num) {
        number = num;
        parent = null;
        next = null;
        firstChild = null;
        modulation = 3;
        return this;
    }

    // Factory
    private static final List<MECElement> _freeList = new ArrayList<>();

    public void free(MECElement elem) {
        _freeList.add(elem);
    }

    public static MECElement alloc(int number) {
        var x = _freeList.remove(_freeList.size() - 1);
        MECElement elem = x != null ? x : new MECElement();
        return elem.initialize(number);
    }
}





