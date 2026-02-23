//
// Singly linked list of Number
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.utils;


/** Singly linked list of Number. */
public class SLLNumber {

    // variables
    //

    /** Number data */
    public double n = 0;
    /** Nest pointer of list */
    public SLLNumber next = null;

    // free list
    private static SLLNumber _freeList = null;

    // constructor
    //

    /** Constructor */
    public SLLNumber(double n) {
        this.n = n;
    }

    // allocator
    //

    /** Allocator */
    public static SLLNumber alloc(double n) {
        SLLNumber ret;
        if (_freeList != null) {
            ret = _freeList;
            _freeList = _freeList.next;
            ret.n = n;
            ret.next = null;
        } else {
            ret = new SLLNumber(n);
        }
        return ret;
    }

    /** Allocator of linked list */
    public SLLNumber allocList(int size, double defaultData /* = 0 */) {
        SLLNumber ret = alloc(defaultData);
        SLLNumber elem = ret;
        for (int i = 1; i < size; i++) {
            elem.next = alloc(defaultData);
            elem = elem.next;
        }
        return ret;
    }

    /** Allocator of ring-linked list */
    public static SLLNumber allocRing(int size, double defaultData /* = 0 */) {
        SLLNumber ret = alloc(defaultData);
        SLLNumber elem = ret;
        for (int i = 1; i < size; i++) {
            elem.next = alloc(defaultData);
            elem = elem.next;
        }
        elem.next = ret;
        return ret;
    }

    /** Ring-linked list with initial values. */
    public SLLNumber newRing(double... args) {
        int size = args.length;
        SLLNumber ret = alloc(args[0]);
        SLLNumber elem = ret;
        for (int i = 1; i < size; i++) {
            elem.next = alloc(args[i]);
            elem = elem.next;
        }
        elem.next = ret;
        return ret;
    }

    // deallocator
    //

    /** Deallocator */
    public void free(SLLNumber elem) {
        elem.next = _freeList;
        _freeList = elem;
    }

    /** Deallocator of linked list */
    public void freeList(SLLNumber firstElem) {
        if (firstElem == null) return;
        SLLNumber lastElem = firstElem;
        while (lastElem.next != null) {
            lastElem = lastElem.next;
        }
        lastElem.next = _freeList;
        _freeList = firstElem;
    }

    /** Deallocator of ring-linked list */
    public static void freeRing(SLLNumber firstElem) {
        if (firstElem == null) return;
        SLLNumber lastElem = firstElem;
        while (lastElem.next == firstElem) {
            lastElem = lastElem.next;
        }
        lastElem.next = _freeList;
        _freeList = firstElem;
    }

    // create pager
    //

    /** Create pager of linked list */
    public SLLNumber[] createListPager(SLLNumber firstElem, boolean fixedSize) {
        if (firstElem == null) return null;
        SLLNumber elem;
        int i, size;
        for (size = 1, elem = firstElem; elem.next != null; elem = elem.next) {
            size++;
        }
        SLLNumber[] pager = new SLLNumber[size];
        elem = firstElem;
        for (i = 0; i < size; i++) {
            pager[i] = elem;
            elem = elem.next;
        }
        return pager;
    }

    /** Create pager of ring-linked list */
    public SLLNumber[] createRingPager(SLLNumber firstElem, boolean fixedSize) {
        if (firstElem == null) return null;
        SLLNumber elem;
        int i, size;
        for (size = 1, elem = firstElem; elem.next != firstElem; elem = elem.next) {
            size++;
        }
        SLLNumber[] pager = new SLLNumber[size];
        elem = firstElem;
        for (i = 0; i < size; i++) {
            pager[i] = elem;
            elem = elem.next;
        }
        return pager;
    }
}
