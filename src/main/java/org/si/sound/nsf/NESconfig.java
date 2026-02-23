//
// NES configuration
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//------------------------------


package org.si.sound.nsf;

public class NESconfig {

    public NESconfig NTSC = new NESconfig(1789772.5, 262, 1364, 1024, 340, 4, 29830, 60);
    public NESconfig PAL = new NESconfig(1662607.125, 312, 1278, 960, 318, 2, 33252, 50);

    public double cpuClock, frameRate, framePeriod, totalScanlines;
    public int scanlineCycles, hDrawCycles, hBlankCycles, scanlineEndCycles;
    public int frameCycles, frameIrqCycles;

    public NESconfig(double cl, int sl, int slc, int hdc, int hbc, int sec, int fic, int fr) {
        cpuClock = cl;
        totalScanlines = sl;
        scanlineCycles = slc;
        hDrawCycles = hdc;
        hBlankCycles = hbc;
        scanlineEndCycles = sec;
        frameCycles = sl * slc;
        frameIrqCycles = fic;
        frameRate = fr;
        framePeriod = 1000 / fr;
    }
}
