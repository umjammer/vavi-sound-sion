//
// SiMMLTrack Envelop table
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.sequencer;

import org.si.sion.utils.Translator;
import org.si.utils.SLLint;


/** Table envelope data. */
public class SiMMLEnvelopTable {

    // variables
    //

    /** Head element of single linked list. */
    public SLLint head;
    /** Tail element of single linked list. */
    public SLLint tail;

    // constructor
    //

    public SiMMLEnvelopTable() {
        head = null;
        tail = null;
    }

    /**
     * constructor.
     *
     * @param table     envelop table vector.
     * @param loopPoint returning point index of looping. -1 sets no loop.
     */
    public SiMMLEnvelopTable(int[] table, int loopPoint) {
        if (table != null) {
            SLLint loop;
            int i, imax = table.length;
            head = tail = SLLint.allocList(imax, 0);
            loop = null;
            for (i = 0; i < imax - 1; i++) {
                if (loopPoint == i) loop = tail;
                tail.i = table[i];
                tail = tail.next;
            }
            tail.i = table[i];
            tail.next = loop;
        } else {
            head = null;
            tail = null;
        }
    }

    // operations
    //

    /** convert to Vector.&lt;int&gt; */
    public int[] toVector(int length, int min, int max, int[] dst /* = null */) {
        if (dst == null || dst.length < length) dst = new int[length];
        int i, n;
        SLLint ptr = head;
        for (i = 0; i < length; i++) {
            if (ptr != null) {
                n = ptr.i;
                ptr = ptr.next;
            } else {
                n = 0;
            }
            if (n < min) n = min;
            else if (n > max) n = max;
            dst[i] = n;
        }
        return dst;
    }

    /** free */
    public void free() {
        if (head != null) {
            tail.next = null;
            SLLint.freeList(head);
            head = null;
            tail = null;
        }
    }

    /**
     * copy
     *
     * @return this instance
     */
    public SiMMLEnvelopTable copyFrom(SiMMLEnvelopTable src) {
        free();
        if (src.head != null) {
            for (SLLint pSrc = src.head, pDst = null;
                 pSrc != src.tail;
                 pSrc = pSrc.next) {
                SLLint p = SLLint.alloc(pSrc.i);
                if (pDst != null) {
                    pDst.next = p;
                    pDst = p;
                } else {
                    head = p;
                    pDst = head;
                }
            }
        }
        return this;
    }

    public SiMMLEnvelopTable parseMML(String tableNumbers) {
        return parseMML(tableNumbers, "", 0);
    }

    /**
     * parse mml text
     *
     * @param tableNumbers String of table numbers
     * @param postfix      String of postfix
     * @param maxIndex     maximum size of envelop table
     * @return this instance
     */
    public SiMMLEnvelopTable parseMML(String tableNumbers, String postfix, int maxIndex /* = 65536 */) {
        Translator.TableNumbersResult res = Translator.parseTableNumbers(tableNumbers, postfix, maxIndex);
        if (res.head != null) _initialize(res.head, res.tail);
        return this;
    }

    // internal functions
    //

    /** set by pointers. */
    void _initialize(SLLint head_, SLLint tail_) {
        head = head_;
        tail = tail_;
        // looping last data
        if (tail.next == null) tail.next = tail;
    }
}
