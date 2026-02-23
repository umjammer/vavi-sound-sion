//
// SiOPM effect table
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Tables used in effectors. */
public class SiEffectTable {

    /** sin table */
    public double[] sinTable;

    /** constructor. */
    public SiEffectTable() {
        int i;
        sinTable = new double[384];

        for (i = 0; i < 384; i++) sinTable[i] = Math.sin(i * 0.02454369260617026); // pi / 128
    }

    /** instance */
    private static SiEffectTable _instance = null;

    /** static initializer */
    public static SiEffectTable getInstance() {
        if (_instance == null) _instance = new SiEffectTable();
        return _instance;
    }
}
