//
// NES Central processing unit
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//------------------------------

package org.si.sound.nsf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public class CPU {

    // register flags
    public static final int CF = 0x01;       // Carry flag
    public static final int ZF = 0x02;       // Zero flag
    public static final int IF = 0x04;       // Irq disabled
    public static final int DF = 0x08;       // Decimal mode flag (NES unused)
    public static final int BF = 0x10;       // Break
    public static final int RF = 0x20;       // Reserved (Always 1)
    public static final int VF = 0x40;       // Overflow
    public static final int NF = 0x80;       // Negative
    public static final int IZN = 0x7d;      // ~(ZF|NF)

    // interruption flags
    public static final int NMI_FLAG     = 0x01;
    public static final int IRQ_FLAG     = 0xfc;
    public static final int IRQ_FRAMEIRQ = 0x04;
    public static final int IRQ_DPCM     = 0x08;
    public static final int IRQ_MAPPER   = 0x10;
    public static final int IRQ_MAPPER2  = 0x20;
    public static final int IRQ_TRIGGER  = 0x40; // one shot(IRQ())
    public static final int IRQ_TRIGGER2 = 0x80; // one shot(IRQ_NotPending())

    // address
    public static final int $NMI= 0xfffa;
    public static final int $RES= 0xfffc;
    public static final int $IRQ= 0xfffe;

    // valiables
    public int A, X, Y, PC, SP, P;
    public int interruptFlag;
    public boolean enableClockedProcess;
    public int wait, totalCycle;
    public int[] ZN_TABLE = new int[256];
    public MMU mmu;
    public NES nes;

    // constructor
    public CPU() {
        mmu = new MMU();
        mmu.onReadPPUport  = this::_onReadPPUPort;
        mmu.onReadCPUport  = this::_onReadCPUPort;
        mmu.onWritePPUport = this::_onWritePPUPort;
        mmu.onWriteCPUport = this::_onWriteCPUPort;
        for (int i=0; i<256; i++) ZN_TABLE[i] = (i==0)?(ZF):(i&NF);
        _zp = mmu.RAM;
    }

    // reset
    public void reset() {
        int i;
        wait = A = X = Y = 0;
        P = ZF|RF;
        SP = 255;
        PC = readW($RES);
        interruptFlag = 0;
        enableClockedProcess = false;
        mmu.reset(0, true);
    }

    // execute
    public int exec(int cycles) {
        int opcode, residue, nmi_request, irq_request, executed, oldCycle=totalCycle;

        for (residue=cycles; residue>0;) {
            if (wait != 0) {
                if (residue < wait) {
                    wait -= residue;
                    if (nes != null && nes.map != null) nes.map.sync(residue);
                    if (nes != null && nes.apu != null) nes.apu.sync(residue);
                    totalCycle += residue;
                    break;
                } else {
                    residue -= wait;
                    totalCycle += wait;
                    wait = 0;
                }
            }

            opcode = read(PC++);
            boolean nmi_req = ((interruptFlag & NMI_FLAG)!=0);
            boolean irq_req = ((interruptFlag & IRQ_FLAG)!=0) && (!nmi_req) && ((P & IF)==0) && (opcode!=0x40);
            interruptFlag &= ~(NMI_FLAG | IRQ_TRIGGER2 | ((irq_req) ? IRQ_TRIGGER : 0));

            executed = operations.get(opcode).get();
            if (nmi_req) executed += _gosub($NMI);
            if (irq_req) executed += _gosub($IRQ);

            if (nes != null && nes.map != null) nes.map.sync(residue);
            residue -= executed;
            totalCycle += executed;
        }

        executed = totalCycle - oldCycle;
        if (nes != null && nes.apu != null) nes.apu.sync(executed);
        return executed;
    }

    // interruptions
    public void setNMI() { interruptFlag |= NMI_FLAG; }
    public void setIRQ(int irqFlag) { interruptFlag |= irqFlag; }
    public void clearIRQ(int irqFlag) { interruptFlag &= ~irqFlag; }

    // memory access
    public int read(int addr) { return mmu.CPU_MEM_BANK[addr>>13].read(addr); }
    public int readW(int addr) { return mmu.CPU_MEM_BANK[addr>>13].readW(addr); }
    public void write(int addr, int data) { mmu.CPU_MEM_BANK[addr>>13].write(addr, data); }

    // I/O port
    private int _onReadPPUPort(int addr) {
        return 0;
    }

    private void _onWritePPUPort(int addr, int data) {
    }

    private int _onReadCPUPort(int addr) {
        return 0;
    }

    private void _onWriteCPUPort(int addr, int data) {
    }

    // operations
    //

    // temporary variables
    private int _ea, _et, _execCycle;
    private int[] _zp; // zero page memory area
    // address
    private int $IM() { return PC++; }
    private int $ZP() { return _ea = (read(PC++)) & 0xff; }
    private int $ZX() { return _ea = (read(PC++) + X) & 0xff; }
    private int $ZY() { return _ea = (read(PC++) + Y) & 0xff; }
    private int $AB() { _ea = readW(PC); PC+=2; return _ea; }
    private int $AX() { _et = readW(PC); PC+=2; return _ea = _et + X; }
    private int $AY() { _et = readW(PC); PC+=2; return _ea = _et + Y; }
    private int $IX() { int i=$ZX(); return _ea=read(i)|(read((i+1)&0xff)<<8); }
    private int $IY() { int i=$ZP(); _et=read(i)|(read((i+1)&0xff)<<8); return _ea = _et + Y; }
    // sub routines
    private void _check(int b, int f) { P&=~f; P|=(b!=0)?f:0; }
    private void _check(boolean b, int f) { P&=~f; P|=(b)?f:0; } // add boolean overload
    private int _checkZN(int data) { data&=0xff; P&=IZN; P|=ZN_TABLE[data]; return data; }
    private void _push(int data) { write(0x100|((SP--)&0xff), data); }
    private int _pop() { return read(0x100|((++SP)&0xff)); }
    private void _wm(int data) { write(_ea, data); } // write memory
    private void _wz(int data) { _zp[_ea] = data;  } // write zero page memory
    private int _cs(int cycle) { return cycle + (((_et&0xff00)!=(_ea&0xff00)) ? 1 : 0); } // check segment
    private int _rj(int data) { _et=PC; _ea=PC+data; PC=_ea; _execCycle+=1; return _cs(0); } // relative jump
    private int _gosub(int v) { _push(PC>>8); _push(PC&0xff); P&=~BF; _push(P); P|=IF; PC=readW(v); return 7; }

    // operating subs
    private void adc(int data) {
        int i = A + data + (P & CF);
        _check(i>0xff, CF);
        _check(((~(A^data))&(A^i)&0x80), VF);
        A = _checkZN(i);
    }
    private void sbc(int data) {
        int i = A - data - (~P & CF);
        _check(((A^data)&(A^i)&0x80), VF);
        _check(i>=0, CF);
        A = _checkZN(i);
    }
    private void and(int data) { A = _checkZN(A&data); }
    private void ora(int data) { A = _checkZN(A|data); }
    private void eor(int data) { A = _checkZN(A^data); }
    private int inc(int data)  { return _checkZN(++data); }
    private int dec(int data)  { return _checkZN(--data); }
    private int asl(int data)  { _check(data&0x80, CF); return _checkZN(data<<1); }
    private int lsr(int data)  { _check(data&0x01, CF); return _checkZN(data>>1); }
    private int rol(int data)  { int c=P&CF;      _check(data&0x80, CF); return _checkZN((data<<1)|c); }
    private int ror(int data)  { int c=(P&CF)<<7; _check(data&0x01, CF); return _checkZN((data>>1)|c); }
    private void bit(int data) { _check((data&A)==0, ZF); _check(data&0x80, NF); _check(data&0x40, VF); }
    private void cmp(int reg, int data) { int i=reg-data; _check(i>=0, CF); _checkZN(i); }
    // operating subs (unofficial)
    private int dcp(int data) { cmp(A, --data); return data; }
    private int isb(int data) { sbc(++data); return data; }
    private void lax(int data) { A = X = _checkZN(data); }
    private int rla(int data) { data = rol(data); A = _checkZN(A&data); return data; }
    private int rra(int data) { data = ror(data); adc(data); return data; }
    private int slo(int data) { _check(data&0x80, CF); data<<=1; A = _checkZN(A|data); return data; }
    private int sre(int data) { _check(data&0x01, CF); data>>=1; A = _checkZN(A^data); return data; }
    private int sh_(int reg, int addr) { SP = A & X; return SP & ((addr>>8)+1) & 0xff; }
    // SHS;sh_(SP=A&X), SHA;sh_(A&X), SHX;sh_(X), SHY;sh_(Y)
    // no operation subs
    private int nop() { return 2; }
    private int dop2() { PC++; return 2; }
    private int dop3() { PC++; return 3; }
    private int dop4() { PC++; return 4; }
    private int top() { PC+=2; return 4; }
    private int err() { return 4; }

    // -------- operation functors
    public Map<Integer, Supplier<Integer>> operations = new HashMap<>() {{
        put(0x69, () -> { adc(read($IM())); return 2; });      // ADC #$??
        put(0x65, () -> { adc(_zp[$ZP()]);  return 3; });      // ADC $??
        put(0x75, () -> { adc(_zp[$ZX()]);  return 4; });      // ADC $??,X
        put(0x6d, () -> { adc(read($AB())); return 4; });      // ADC $????
        put(0x7d, () -> { adc(read($AX())); return _cs(4); }); // ADC $????,X
        put(0x79, () -> { adc(read($AY())); return _cs(4); }); // ADC $????,Y
        put(0x61, () -> { adc(read($IX())); return 6; });      // ADC ($??,X)
        put(0x71, () -> { adc(read($IY())); return _cs(5); }); // ADC ($??),Y (ct=4 in vertualNES)

        put(0xe9, () -> { sbc(read($IM())); return 2; });      // SBC #$??
        put(0xe5, () -> { sbc(_zp[$ZP()]);  return 3; });      // SBC $??
        put(0xf5, () -> { sbc(_zp[$ZX()]);  return 4; });      // SBC $??,X
        put(0xed, () -> { sbc(read($AB())); return 4; });      // SBC $????
        put(0xfd, () -> { sbc(read($AX())); return _cs(4); }); // SBC $????,X
        put(0xf9, () -> { sbc(read($AY())); return _cs(4); }); // SBC $????,Y
        put(0xe1, () -> { sbc(read($IX())); return 6; });      // SBC ($??,X)
        put(0xf1, () -> { sbc(read($IY())); return _cs(5); }); // SBC ($??),Y

        put(0x09, () -> { ora(read($IM())); return 2; });      // ORA #$??
        put(0x05, () -> { ora(_zp[$ZP()]);  return 3; });      // ORA $??
        put(0x15, () -> { ora(_zp[$ZX()]);  return 4; });      // ORA $??,X
        put(0x0d, () -> { ora(read($AB())); return 4; });      // ORA $????
        put(0x1d, () -> { ora(read($AX())); return _cs(4); }); // ORA $????,X
        put(0x19, () -> { ora(read($AY())); return _cs(4); }); // ORA $????,Y
        put(0x01, () -> { ora(read($IX())); return 6; });      // ORA ($??,X)
        put(0x11, () -> { ora(read($IY())); return _cs(5); }); // ORA ($??),Y

        put(0x29, () -> { and(read($IM())); return 2; });      // AND #$??
        put(0x25, () -> { and(_zp[$ZP()]);  return 3; });      // AND $??
        put(0x35, () -> { and(_zp[$ZX()]);  return 4; });      // AND $??,X
        put(0x2d, () -> { and(read($AB())); return 4; });      // AND $????
        put(0x3d, () -> { and(read($AX())); return _cs(4); }); // AND $????,X
        put(0x39, () -> { and(read($AY())); return _cs(4); }); // AND $????,Y
        put(0x21, () -> { and(read($IX())); return 6; });      // AND ($??,X)
        put(0x31, () -> { and(read($IY())); return _cs(5); }); // AND ($??),Y

        put(0x49, () -> { eor(read($IM())); return 2; });      // EOR #$??
        put(0x45, () -> { eor(_zp[$ZP()]);  return 3; });      // EOR $??
        put(0x55, () -> { eor(_zp[$ZX()]);  return 4; });      // EOR $??,X
        put(0x4d, () -> { eor(read($AB())); return 4; });      // EOR $????
        put(0x5d, () -> { eor(read($AX())); return _cs(4); }); // EOR $????,X
        put(0x59, () -> { eor(read($AY())); return _cs(4); }); // EOR $????,Y
        put(0x41, () -> { eor(read($IX())); return 6; });      // EOR ($??,X)
        put(0x51, () -> { eor(read($IY())); return _cs(5); }); // EOR ($??),Y

        put(0x0a, () -> { A=asl(A);            return 2; });      // ASL A
        put(0x06, () -> { _wz(asl(_zp[$ZP()]));  return 5; });      // ASL $??
        put(0x16, () -> { _wz(asl(_zp[$ZX()]));  return 6; });      // ASL $??,X
        put(0x0e, () -> { _wm(asl(read($AB()))); return 6; });      // ASL $????
        put(0x1e, () -> { _wm(asl(read($AX()))); return _cs(6); }); // ASL $????,X (no check in vertualNES)

        put(0x2a, () -> { A=rol(A);            return 2; });      // ROL A
        put(0x26, () -> { _wz(rol(_zp[$ZP()]));  return 5; });      // ROL $??
        put(0x36, () -> { _wz(rol(_zp[$ZX()]));  return 6; });      // ROL $??,X
        put(0x2e, () -> { _wm(rol(read($AB()))); return 6; });      // ROL $????
        put(0x3e, () -> { _wm(rol(read($AX()))); return _cs(6); }); // ROL $????,X (no check in vertualNES)

        put(0x4a, () -> { A=lsr(A);            return 2; });      // LSR A
        put(0x46, () -> { _wz(lsr(_zp[$ZP()]));  return 5; });      // LSR $??
        put(0x56, () -> { _wz(lsr(_zp[$ZX()]));  return 6; });      // LSR $??,X
        put(0x4e, () -> { _wm(lsr(read($AB()))); return 6; });      // LSR $????
        put(0x5e, () -> { _wm(lsr(read($AX()))); return _cs(6); }); // LSR $????,X (no check in vertualNES)

        put(0x6a, () -> { A=ror(A);            return 2; });      // ROR A
        put(0x66, () -> { _wz(ror(_zp[$ZP()]));  return 5; });      // ROR $??
        put(0x76, () -> { _wz(ror(_zp[$ZX()]));  return 6; });      // ROR $??,X
        put(0x6e, () -> { _wm(ror(read($AB()))); return 6; });      // ROR $????
        put(0x7e, () -> { _wm(ror(read($AX()))); return _cs(6); }); // ROR $????,X (no check in vertualNES)

        put(0x24, () -> { bit(_zp[$ZP()]);  return 3; }); // BIT $??
        put(0x2C, () -> { bit(read($AB())); return 4; }); // BIT $????

        put(0xc6, () -> { _wz(dec(_zp[$ZP()]));  return 5; });      // DEC $??
        put(0xd6, () -> { _wz(dec(_zp[$ZX()]));  return 6; });      // DEC $??,X
        put(0xce, () -> { _wm(dec(read($AB()))); return 6; });      // DEC $????
        put(0xde, () -> { _wm(dec(read($AX()))); return _cs(6); }); // DEC $????,X (no check in vertualNES)
        put(0xca, () -> { X=dec(X); return 2; });                 // DEX
        put(0x88, () -> { Y=dec(Y); return 2; });                 // DEY

        put(0xe6, () -> { _wz(inc(_zp[$ZP()]));  return 5; });      // INC $??
        put(0xf6, () -> { _wz(inc(_zp[$ZX()]));  return 6; });      // INC $??,X
        put(0xee, () -> { _wm(inc(read($AB()))); return 6; });      // INC $????
        put(0xfe, () -> { _wm(inc(read($AX()))); return _cs(6); }); // INC $????,X (no check in vertualNES)
        put(0xe8, () -> { X=inc(X); return 2; });                 // INX
        put(0xc8, () -> { X=inc(Y); return 2; });                 // INY

        put(0xa9, () -> { A=_checkZN(read($IM())); return 2; });      // LDA #$??
        put(0xa5, () -> { A=_checkZN(_zp[$ZP()]);  return 3; });      // LDA $??
        put(0xb5, () -> { A=_checkZN(_zp[$ZX()]);  return 4; });      // LDA $??,X
        put(0xad, () -> { A=_checkZN(read($AB())); return 4; });      // LDA $????
        put(0xbd, () -> { A=_checkZN(read($AX())); return _cs(4); }); // LDA $????,X
        put(0xb9, () -> { A=_checkZN(read($AY())); return _cs(4); }); // LDA $????,Y
        put(0xa1, () -> { A=_checkZN(read($IX())); return 6; });      // LDA ($??,X)
        put(0xb1, () -> { A=_checkZN(read($IY())); return _cs(5); }); // LDA ($??),Y

        put(0xa2, () -> { X=_checkZN(read($IM())); return 2; });      // LDX #$??
        put(0xa6, () -> { X=_checkZN(_zp[$ZP()]);  return 3; });      // LDX $??
        put(0xb6, () -> { X=_checkZN(_zp[$ZY()]);  return 4; });      // LDX $??,Y
        put(0xae, () -> { X=_checkZN(read($AB())); return 4; });      // LDX $????
        put(0xbe, () -> { X=_checkZN(read($AY())); return _cs(4); }); // LDX $????,Y

        put(0xa0, () -> { Y=_checkZN(read($IM())); return 2; });      // LDY #$??
        put(0xa4, () -> { Y=_checkZN(_zp[$ZP()]);  return 3; });      // LDY $??
        put(0xb4, () -> { Y=_checkZN(_zp[$ZX()]);  return 4; });      // LDY $??,X
        put(0xac, () -> { Y=_checkZN(read($AB())); return 4; });      // LDY $????
        put(0xbc, () -> { Y=_checkZN(read($AX())); return _cs(4); }); // LDY $????,X

        put(0x85, () -> { _zp[$ZP()] = A; return 3; });      // STA $??
        put(0x95, () -> { _zp[$ZX()] = A; return 4; });      // STA $??,X
        put(0x8d, () -> { write($AB(),A); return 4; });      // STA $????
        put(0x9d, () -> { write($AX(),A); return _cs(4); }); // STA $????,X (no check in vertualNES)
        put(0x99, () -> { write($AY(),A); return _cs(4); }); // STA $????,Y (no check in vertualNES)
        put(0x81, () -> { write($IX(),A); return 6; });      // STA ($??,X)
        put(0x91, () -> { write($IY(),A); return _cs(5); }); // STA ($??),Y (no check in vertualNES)

        put(0x86, () -> { _zp[$ZP()] = X; return 3; }); // STX $??
        put(0x96, () -> { _zp[$ZX()] = X; return 4; }); // STX $??,Y
        put(0x8e, () -> { write($AB(),X); return 4; }); // STX $????

        put(0x84, () -> { _zp[$ZP()] = Y; return 3; }); // STY $??
        put(0x94, () -> { _zp[$ZX()] = Y; return 4; }); // STY $??,X
        put(0x8c, () -> { write($AB(),Y); return 4; }); // STY $????

        put(0xaa, () -> { X=_checkZN(A);  return 2; }); // TAX
        put(0x8a, () -> { A=_checkZN(X);  return 2; }); // TXA
        put(0xa8, () -> { Y=_checkZN(A);  return 2; }); // TAY
        put(0x98, () -> { A=_checkZN(Y);  return 2; }); // TYA
        put(0xba, () -> { X=_checkZN(SP); return 2; }); // TSX
        put(0x9a, () -> { SP=X;           return 2; }); // TXS

        put(0xc9, () -> { cmp(A, read($IM())); return 2; });      // CMP #$??
        put(0xc5, () -> { cmp(A, _zp[$ZP()]);  return 3; });      // CMP $??
        put(0xd5, () -> { cmp(A, _zp[$ZX()]);  return 4; });      // CMP $??,X
        put(0xcd, () -> { cmp(A, read($AB())); return 4; });      // CMP $????
        put(0xdd, () -> { cmp(A, read($AX())); return _cs(4); }); // CMP $????,X
        put(0xd9, () -> { cmp(A, read($AY())); return _cs(4); }); // CMP $????,Y
        put(0xc1, () -> { cmp(A, read($IX())); return 6; });      // CMP ($??,X)
        put(0xd1, () -> { cmp(A, read($IY())); return _cs(5); }); // CMP ($??),Y

        put(0xe0, () -> { cmp(X, read($IM())); return 2; });      // CPX #$??
        put(0xe4, () -> { cmp(X, _zp[$ZP()]);  return 3; });      // CPX $??
        put(0xec, () -> { cmp(X, read($AB())); return 4; });      // CPX $????

        put(0xc0, () -> { cmp(Y, read($IM())); return 2; });      // CPX #$??
        put(0xc4, () -> { cmp(Y, _zp[$ZP()]);  return 3; });      // CPX $??
        put(0xcc, () -> { cmp(Y, read($AB())); return 4; });      // CPX $????

        put(0x90, () -> { int r=read($IM()); return ((P&CF)==0) ? (_rj(r)+3) : 2; }); // BCC
        put(0xb0, () -> { int r=read($IM()); return ((P&CF)!=0) ? (_rj(r)+3) : 2; }); // BCS
        put(0xd0, () -> { int r=read($IM()); return ((P&ZF)==0) ? (_rj(r)+3) : 2; }); // BNE
        put(0xf0, () -> { int r=read($IM()); return ((P&ZF)!=0) ? (_rj(r)+3) : 2; }); // BEQ
        put(0x10, () -> { int r=read($IM()); return ((P&NF)==0) ? (_rj(r)+3) : 2; }); // BPL
        put(0x30, () -> { int r=read($IM()); return ((P&NF)!=0) ? (_rj(r)+3) : 2; }); // BMI
        put(0x50, () -> { int r=read($IM()); return ((P&VF)==0) ? (_rj(r)+3) : 2; }); // BVC
        put(0x70, () -> { int r=read($IM()); return ((P&VF)!=0) ? (_rj(r)+3) : 2; }); // BVS

        put(0x4c, () -> { PC = readW(PC); return 3; }); // JMP $????
        put(0x6c, () -> { int i=readW(PC); _ea=read(i); i=(i&0xff00)|((i+1)&0xff); PC=_ea|(read(i)<<8); return 5; }); // JMP ($????)
        put(0x20, () -> { _ea=readW(PC++); _push(PC>>8); _push(PC&0xff); PC=_ea; return 6; }); // JSR
        put(0x40, () -> { P=_pop()|RF; PC=_pop(); PC|=_pop()<<8; return 6; }); // RTI
        put(0x60, () -> { PC=_pop(); PC|=_pop()<<8; PC++; return 6; });  // RTS

        put(0x18, () -> { P&=~CF; return 2; }); // CLC
        put(0xd8, () -> { P&=~DF; return 2; }); // CLD
        put(0x58, () -> { P&=~IF; return 2; }); // CLI
        put(0xb8, () -> { P&=~VF; return 2; }); // CLV
        put(0x38, () -> { P|=CF;  return 2; }); // SEC
        put(0xf8, () -> { P|=DF;  return 2; }); // SED
        put(0x78, () -> { P|=IF;  return 2; }); // SEI

        put(0x48, () -> { _push(A); return 3; });           // PHA
        put(0x08, () -> { _push(P|BF); return 3; });        // PHP
        put(0x68, () -> { A=_checkZN(_pop()); return 4; }); // PLA
        put(0x28, () -> { P=_pop()|RF; return 4; });        // PLP

        put(0x00, () -> { PC++; _push(PC>>8); _push(PC&0xff); P|=BF; _push(P); P|=IF; PC=readW($IRQ); return 7; }); // BRK
        put(0xea, CPU.this::nop); // NOP

        // ---------- unofficial operations ----------
        put(0x0b, () -> { A=_checkZN(A&read($IM())); _check(P&NF, CF); return 2; }); // ANC #$??
        put(0x2b, () -> { A=_checkZN(A&read($IM())); _check(P&NF, CF); return 2; }); // ANC #$??
        put(0x4b, () -> { int i=read($IM()); i&=A; _check(i&1, CF); A=_checkZN(i>>1); return 2; }); // ASR #$??
        put(0x6b, () -> { A=_checkZN(((read($IM())&A)>>1)|((P&CF)<<7)); _check(A&0x40,CF); _check((A>>6)^(A>>5),VF); return 2; }); // ARR #$??
        put(0x8b, () -> { A=_checkZN((A|0xee)&X&read($IM()));          return 2; }); // ANE #$??
        put(0xc7, () -> { _wz(dcp(_zp[$ZP()]));  return 5; }); // DCP $??
        put(0xd7, () -> { _wz(dcp(_zp[$ZX()]));  return 6; }); // DCP $??,X
        put(0xcf, () -> { _wm(dcp(read($AB()))); return 6; }); // DCP $????
        put(0xdf, () -> { _wm(dcp(read($AX()))); return 7; }); // DCP $????,X
        put(0xdb, () -> { _wm(dcp(read($AY()))); return 7; }); // DCP $????,Y
        put(0xc3, () -> { _wm(dcp(read($IX()))); return 8; }); // DCP ($??,X)
        put(0xd3, () -> { _wm(dcp(read($IY()))); return 8; }); // DCP ($??),Y
        put(0xe7, () -> { _wz(isb(_zp[$ZP()]));  return 5; }); // ISB $??
        put(0xf7, () -> { _wz(isb(_zp[$ZX()]));  return 5; }); // ISB $??,X
        put(0xef, () -> { _wm(isb(read($AB()))); return 5; }); // ISB $????
        put(0xff, () -> { _wm(isb(read($AX()))); return 5; }); // ISB $????,X
        put(0xfb, () -> { _wm(isb(read($AY()))); return 5; }); // ISB $????,Y
        put(0xe3, () -> { _wm(isb(read($IX()))); return 5; }); // ISB ($??,X)
        put(0xf3, () -> { _wm(isb(read($IY()))); return 5; }); // ISB ($??),Y
        put(0xbb, () -> { A=X=SP=_checkZN(SP&read($AY())); return _cs(4); }); // LAS $????,Y
        put(0xa7, () -> { lax(_zp[$ZP()]);  return 3; });      // LAX $??
        put(0xb7, () -> { lax(_zp[$ZY()]);  return 4; });      // LAX $??,Y
        put(0xaF, () -> { lax(read($AB())); return 4; });      // LAX $????
        put(0xbF, () -> { lax(read($AY())); return _cs(4); }); // LAX $????,Y
        put(0xa3, () -> { lax(read($IX())); return 6; });      // LAX ($??,X)
        put(0xb3, () -> { lax(read($IY())); return _cs(5); }); // LAX ($??),Y
        put(0xab, () -> { A=X=_checkZN((A|0xee)&read($IM())); return 2; }); // LXA #$??
        put(0x27, () -> { _wz(rla(_zp[$ZP()]));  return 5; }); // RLA $??
        put(0x37, () -> { _wz(rla(_zp[$ZX()]));  return 6; }); // RLA $??,X
        put(0x2f, () -> { _wm(rla(read($AB()))); return 6; }); // RLA $????
        put(0x3f, () -> { _wm(rla(read($AX()))); return 7; }); // RLA $????,X
        put(0x3b, () -> { _wm(rla(read($AY()))); return 7; }); // RLA $????,Y
        put(0x23, () -> { _wm(rla(read($IX()))); return 8; }); // RLA ($??,X)
        put(0x33, () -> { _wm(rla(read($IY()))); return 8; }); // RLA ($??),Y
        put(0x67, () -> { _wz(rra(_zp[$ZP()]));  return 5; }); // RRA $??
        put(0x77, () -> { _wz(rra(_zp[$ZX()]));  return 6; }); // RRA $??,X
        put(0x6f, () -> { _wm(rra(read($AB()))); return 6; }); // RRA $????
        put(0x7f, () -> { _wm(rra(read($AX()))); return 7; }); // RRA $????,X
        put(0x7b, () -> { _wm(rra(read($AY()))); return 7; }); // RRA $????,Y
        put(0x63, () -> { _wm(rra(read($IX()))); return 8; }); // RRA ($??,X)
        put(0x73, () -> { _wm(rra(read($IY()))); return 8; }); // RRA ($??),Y
        put(0x87, () -> { _zp[$ZP()] = A&X; return 3; }); // SAX $??
        put(0x97, () -> { _zp[$ZY()] = A&X; return 4; }); // SAX $??,Y
        put(0x8f, () -> { write($AB(),A&X); return 4; }); // SAX $????
        put(0x83, () -> { write($IX(),A&X); return 6; }); // SAX ($??,X)
        put(0xcb, () -> { int i=(A&X)-read($IM()); _check(i>=0, CF); X=_checkZN(i); return 2; }); // SBX #$??
        put(0x9f, () -> { _wm(sh_(A&X,read($AY()))); return 5; }); // SHA $????,Y
        put(0x93, () -> { _wm(sh_(A&X,read($IY()))); return 6; }); // SHA ($??),Y
        put(0x9b, () -> { SP=A&X; _wm(sh_(SP,read($AY()))); return 5; }); // SHS $????,Y
        put(0x9e, () -> { _wm(sh_(X,read($AY()))); return 5; }); // SHX $????,Y
        put(0x9c, () -> { _wm(sh_(Y,read($AX()))); return 5; }); // SHY $????,X
        put(0x07, () -> { _wz(slo(_zp[$ZP()]));  return 5; }); // SLO $??
        put(0x17, () -> { _wz(slo(_zp[$ZX()]));  return 6; }); // SLO $??,X
        put(0x0f, () -> { _wm(slo(read($AB()))); return 6; }); // SLO $????
        put(0x1f, () -> { _wm(slo(read($AX()))); return 7; }); // SLO $????,X
        put(0x1b, () -> { _wm(slo(read($AY()))); return 7; }); // SLO $????,Y
        put(0x03, () -> { _wm(slo(read($IX()))); return 8; }); // SLO ($??,X)
        put(0x13, () -> { _wm(slo(read($IY()))); return 8; }); // SLO ($??),Y
        put(0x47, () -> { _wz(sre(_zp[$ZP()]));  return 5; }); // SRE $??
        put(0x57, () -> { _wz(sre(_zp[$ZX()]));  return 6; }); // SRE $??,X
        put(0x4f, () -> { _wm(sre(read($AB()))); return 6; }); // SRE $????
        put(0x5f, () -> { _wm(sre(read($AX()))); return 7; }); // SRE $????,X
        put(0x5b, () -> { _wm(sre(read($AY()))); return 7; }); // SRE $????,Y
        put(0x43, () -> { _wm(sre(read($IX()))); return 8; }); // SRE ($??,X)
        put(0x53, () -> { _wm(sre(read($IY()))); return 8; }); // SRE ($??),Y
        put(0xeb, () -> { sbc(read($IM())); return 2; });      // SBC #$?? (Unofficial)
        put(0x1a, CPU.this::nop);  put(0x3A, CPU.this::nop);  put(0x5A, CPU.this::nop);  put(0x7A, CPU.this::nop);  put(0xDA, CPU.this::nop);  put(0xFA, CPU.this::nop);  // NOP (Unofficial)
        put(0x80, CPU.this::dop2); put(0x82, CPU.this::dop2); put(0x89, CPU.this::dop2); put(0xC2, CPU.this::dop2); put(0xE2, CPU.this::dop2);                  // DOP (CYCLES 2)
        put(0x04, CPU.this::dop3); put(0x44, CPU.this::dop3); put(0x64, CPU.this::dop3);                                                    // DOP (CYCLES 3)
        put(0x14, CPU.this::dop4); put(0x34, CPU.this::dop4); put(0x54, CPU.this::dop4); put(0x74, CPU.this::dop4); put(0xD4, CPU.this::dop4); put(0xF4, CPU.this::dop4); // DOP (CYCLES 4)
        put(0x0c, CPU.this::top);  put(0x1c, CPU.this::top);  put(0x3c, CPU.this::top);  put(0x5c, CPU.this::top);  put(0x7c, CPU.this::top);  put(0xdc, CPU.this::top); put(0xfc, CPU.this::top); // TOP
        put(0x02, CPU.this::err);  put(0x12, CPU.this::err);  put(0x22, CPU.this::err);  put(0x32, CPU.this::err);  put(0x42, CPU.this::err);  put(0x52, CPU.this::err);
        put(0x62, CPU.this::err);  put(0x72, CPU.this::err);  put(0x92, CPU.this::err);  put(0xb2, CPU.this::err);  put(0xd2, CPU.this::err);  put(0xf2, CPU.this::err);  // JAM
    }};
}
