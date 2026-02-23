//
// Memory management unit
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.nsf;

import org.si.utils.ByteArray;


public class MMU {

    public int[] RAM = new int[2048];    // internal RAM;      2k
    public int[] WRAM = new int[128 * 256]; // Working RAM;     128k
    public int[] DRAM = new int[40 * 256];  // RAM of disk sys;  40k
    public int[] ERAM = new int[32 * 256];  // RAM of exp.unit;  32k
    public int[] CRAM = new int[32 * 256];  // Ch.pattern RAM;   32k
    public int[] VRAM = new int[4 * 256];  // name and attr.;    4k
    public int[] SPRAM = new int[256];    // Sprite RAM;      256b
    public int[] BGPAL = new int[16];     // BG Pallete;       16b
    public int[] SPPAL = new int[16];     // Sprite Pallete;   16b
    public ByteArray PROM;  // ROM pointer
    public ByteArray VROM;  // VROM pointer

    public interface ReadPort { int read(int addr); }
    public interface WritePort { void write(int addr, int data); }
    public ReadPort onReadPPUport, onReadCPUport;
    public WritePort onWritePPUport, onWriteCPUport;

    public MMUBank[] CPU_MEM_BANK = new MMUBank[8];
    static public MMU $;      // unique instance

    public MMU() {
        $ = this;
        CPU_MEM_BANK[0] = new MMUBankRAM();     // $0000-1ffff: internal RAM
        CPU_MEM_BANK[1] = new PPUIOPort();      // $2000-3ffff: I/O port for PPU
        CPU_MEM_BANK[2] = new CPUIOPort();      // $4000-5ffff: I/O port for APU,DMA,PAD etc..
        CPU_MEM_BANK[3] = new MMUBankROMLow();  // $6000-7ffff: ROM area (low address)
        for (int i = 4; i < 8; i++) CPU_MEM_BANK[i] = new MMUBankROM(); // $8000-fffff: ROM area
    }

    public void reset(int ram, boolean clearWRAM) {
        int i;
        for (i = 0; i < RAM.length; i++) RAM[i] = ram;
        if (clearWRAM) for (i = 0; i < WRAM.length; i++) WRAM[i] = 0xff;
        for (i = 0; i < DRAM.length; i++) DRAM[i] = 0;
        for (i = 0; i < ERAM.length; i++) ERAM[i] = 0;
        for (i = 0; i < CRAM.length; i++) CRAM[i] = 0;
        for (i = 0; i < VRAM.length; i++) VRAM[i] = 0;
        for (i = 0; i < SPRAM.length; i++) SPRAM[i] = 0;
        for (i = 0; i < BGPAL.length; i++) BGPAL[i] = 0;
        for (i = 0; i < SPPAL.length; i++) SPPAL[i] = 0;
    }
}

class MMUBank {
    //  bank types
    public static final int ROM = 0x00;
    public static final int RAM = 0xff;
    public static final int DRAM = 0x01;
    public static final int MAPPER = 0x80;
    //  variables
    public int type;

    NES nes = new NES(); // TODO

    //  functions
    public MMUBank(int type) {
        this.type = type;
    }

    public int read(int addr) {
        return 0;
    }

    public int readW(int addr) {
        return read(addr) | (read(addr + 1) << 8);
    }

    public void write(int addr, int data) {
    }
}

class MMUBankRAM extends MMUBank {

    public MMUBankRAM() {
        super(MMUBank.RAM);
    }

    @Override
    public int read(int addr) {
        int i = addr & 2047;
        return MMU.$.RAM[i];
    }

    public void write(int addr, int data) {
        int i = addr & 2047;
        MMU.$.RAM[i] = data;
    }
}

class PPUIOPort extends MMUBank {

    public PPUIOPort() {
        super(-1);
    }

    @Override
    public int read(int addr) {
        return MMU.$.onReadPPUport != null ? MMU.$.onReadPPUport.read(addr & 7) : 0;
    }

    public void write(int addr, int data) {
        if (MMU.$.onWritePPUport != null) MMU.$.onWritePPUport.write(addr, data);
    }
}

class CPUIOPort extends MMUBank {

    public CPUIOPort() {
        super(-1);
    }

    @Override
    public int read(int addr) {
        return (addr < 0x4020) ? (MMU.$.onReadCPUport != null ? MMU.$.onReadCPUport.read(addr & 31) : 0) : nes.map.ExRead(addr);
    }

    public void write(int addr, int data) {
        if (addr < 0x4020) {
            if (MMU.$.onWriteCPUport != null) MMU.$.onWriteCPUport.write(addr, data);
        } else {
            nes.map.ExWrite(addr, data);
        }
    }
}

class MMUBankROM extends MMUBank {

    //  variables
    public int offset = 0;

    //  functions
    public MMUBankROM() {
        super(MMUBank.MAPPER);
    }

    @Override
    public int read(int addr) {
        MMU.$.PROM.position = (addr & 8191) + offset;
        return MMU.$.PROM.readUnsignedByte();
    }

    public void write(int addr, int data) {
        nes.map.write(addr, data);
    }
}

class MMUBankROMLow extends MMUBankROM {

    @Override
    public int read(int addr) {
        return nes.map.readLow(addr);
    }

    public void write(int addr, int data) {
        nes.map.writeLow(addr, data);
    }
}
