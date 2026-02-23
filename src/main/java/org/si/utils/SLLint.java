//
// Singly linked list of int
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.utils;


/** Singly linked list of int. */
public class SLLint {

    // variables
    //

    /** int data */
    public int i = 0;
    /** Next pointer of list */
    public SLLint next = null;

    // free list
    private static SLLint _freeList = null;

    // constructor
    //

    /** Constructor */
    public SLLint(int i) {
        this.i = i;
    }

    // allocator
    //

    /** Allocator */
    public static SLLint alloc(int i) {
        SLLint ret;
        if (_freeList != null) {
            ret = _freeList;
            _freeList = _freeList.next;
            ret.i = i;
            ret.next = null;
        } else {
            ret = new SLLint(i);
        }
        return ret;
    }

    /** Allocator of linked list */
    public static SLLint allocList(int size, int defaultData /* = 0 */) {
        SLLint ret = alloc(defaultData);
        SLLint elem = ret;
        for (int i = 1; i < size; i++) {
            elem.next = alloc(defaultData);
            elem = elem.next;
        }
        return ret;
    }

    /** Allocator of ring-linked list */
    public static SLLint allocRing(int size, int defaultData) {
        SLLint ret = alloc(defaultData);
        SLLint elem = ret;
        for (int i = 1; i < size; i++) {
            elem.next = alloc(defaultData);
            elem = elem.next;
        }
        elem.next = ret;
        return ret;
    }

    /** Ring-linked list with initial values. */
    public SLLint newRing(double... args) {
        int size = args.length;
        SLLint ret = alloc((int) args[0]);
        SLLint elem = ret;
        for (int i = 1; i < size; i++) {
            elem.next = alloc((int) args[i]);
            elem = elem.next;
        }
        elem.next = ret;
        return ret;
    }

    // deallocator
    //

    /** Deallocator */
    public void free(SLLint elem) {
        elem.next = _freeList;
        _freeList = elem;
    }

    /** Deallocator of linked list */
    public static void freeList(SLLint firstElem) {
        if (firstElem == null) return;
        SLLint lastElem = firstElem;
        while (lastElem.next != null) {
            lastElem = lastElem.next;
        }
        lastElem.next = _freeList;
        _freeList = firstElem;
    }

    /** Deallocator of ring-linked list */
    public static void freeRing(SLLint firstElem) {
        if (firstElem == null) return;
        SLLint lastElem = firstElem;
        while (lastElem.next == firstElem) {
            lastElem = lastElem.next;
        }
        lastElem.next = _freeList;
        _freeList = firstElem;
    }

    // create pager
    //

    /** Create pager of linked list */
    public SLLint[] createListPager(SLLint firstElem, boolean fixedSize) {
        if (firstElem == null) return null;
        SLLint elem;
        int i, size;
        for (size = 1, elem = firstElem; elem.next != null; elem = elem.next) {
            size++;
        }
        SLLint[] pager = new SLLint[size];
        elem = firstElem;
        for (i = 0; i < size; i++) {
            pager[i] = elem;
            elem = elem.next;
        }
        return pager;
    }

    /** Create pager of ring-linked list */
    public static SLLint[] createRingPager(SLLint firstElem, boolean fixedSize) {
        if (firstElem == null) return null;
        SLLint elem;
        int i, size;
        for (size = 1, elem = firstElem; elem.next != firstElem; elem = elem.next) {
            size++;
        }
        SLLint[] pager = new SLLint[size];
        elem = firstElem;
        for (i = 0; i < size; i++) {
            pager[i] = elem;
            elem = elem.next;
        }
        return pager;
    }
}
