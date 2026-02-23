//
// NES Emulator
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//------------------------------

package org.si.sound.nsf;


public class NES {

    public CPU cpu = new CPU();
    public APU apu = new APU();
    public PPU ppu = new PPU();
    public PAD pad = new PAD();
    public ROM rom;
    public Mapper map;
    public NESconfig cfg;

    public NES() {
        cpu.nes = this;
    }
}
