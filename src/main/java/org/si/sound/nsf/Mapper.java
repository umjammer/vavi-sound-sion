//
// Mapper class
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//------------------------------

package org.si.sound.nsf;

import java.util.Map;


public class Mapper {

    public int bank3WRAM = 0;

    public Mapper() {
    }

    public void write(int addr, int data) {
    }

    public int readLow(int addr) {
        int a = (addr & 8191) + (bank3WRAM << 13), i = a >> 2, s = (a & 3) << 3;
        return (MMU.$.WRAM[i] >> s) & 0xff;
    }

    public void writeLow(int addr, int data) {
        int a = (addr & 8191) + (bank3WRAM << 13), i = a >> 2, s = (a & 3) << 3;
        MMU.$.WRAM[i] = (MMU.$.WRAM[i] & ~(255 << s)) | (data << s);
    }

    public int ExCmdRead(int cmd) {
        return 0x00;
    }

    public void ExCmdWrite(int cmd, int data) {
    }

    public int ExRead(int addr) {
        return 0;
    }

    public void ExWrite(int addr, int data) {
    }

    public void sync(int cycles) {
    }

    public void HSync(int scanline) {
    }

    public void VSync() {
    }

    public void PPU_Latch(int addr) {
    }

    public void PPU_ChrLatch(int addr) {
    }

    public void PPU_ExtLatchX(int x) {
    }

    public Object PPU_ExtLatch(int addr) {
        return Map.of("chr_l", 0, "chr_h", 0, "attr", 0);
    }
}
