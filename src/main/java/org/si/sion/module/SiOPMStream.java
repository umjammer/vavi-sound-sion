//
// Stream buffer class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.module;

import org.si.utils.ByteArray;
import org.si.utils.SLLint;


/** Stream buffer class */
public class SiOPMStream {

    // variables
    //

    /** number of channels */
    public int channels = 2;
    /** stream buffer */
    public double[] buffer = null;

    // coefficient of volume/panning
    private double[] _panTable;
    private double _i2n;

    // constructor
    //

    /** constructor */
    public SiOPMStream() {
        SiOPMTable st = SiOPMTable.getInstance();
        _panTable = st.panTable;
        _i2n = st.i2n;
    }

    // operation
    //

    /** clear buffer */
    public void clear() {
        int i, imax = buffer.length;
        for (i = 0; i < imax; i++) {
            buffer[i] = 0;
        }
    }

    /** limit buffered signals between -1 and 1 */
    public void limit() {
        double n;
        int i, imax = buffer.length;
        for (i = 0; i < imax; i++) {
            n = buffer[i];
            if (n < -1) buffer[i] = -1;
            else if (n > 1) buffer[i] = 1;
        }
    }

    /** Quantize buffer by bit rate. */
    public void quantize(int bitRate) {
        int i, imax = buffer.length;
        int r = 1 << bitRate, ir = 2 / r;
        for (i = 0; i < imax; i++) {
            buffer[i] = ((int) (buffer[i] * r) >> 1) * ir;
        }
    }

    /** write buffer by org.si.utils.SLLint */
    public void write(SLLint pointer, int start, int len, double vol, int pan) {
        int i;
        double n;
        int imax = (start + len) << 1;
        vol *= _i2n;
        if (channels == 2) {
            // stereo
            double volL = _panTable[128 - pan] * vol;
            double volR = _panTable[pan] * vol;
            for (i = start << 1; i < imax; ) {
                n = pointer.i;
                buffer[i] += n * volL;
                i++;
                buffer[i] += n * volR;
                i++;
                pointer = pointer.next;
            }
        } else if (channels == 1) {
            // monaural
            for (i = start << 1; i < imax; ) {
                n = (double) (pointer.i) * vol;
                buffer[i] += n;
                i++;
                buffer[i] += n;
                i++;
                pointer = pointer.next;
            }
        }
    }

    /** write stereo buffer by 2 pipes */
    public void writeStereo(SLLint pointerL, SLLint pointerR, int start, int len, double vol, int pan) {
        int i;
        double n;
        int imax = (start + len) << 1;
        vol *= _i2n;

        if (channels == 2) {
            // stereo
            double volL = _panTable[128 - pan] * vol,
                    volR = _panTable[pan] * vol;
            for (i = start << 1; i < imax; ) {
                buffer[i] += (double) (pointerL.i) * volL;
                i++;
                buffer[i] += (double) (pointerR.i) * volR;
                i++;
                pointerL = pointerL.next;
                pointerR = pointerR.next;
            }
        } else if (channels == 1) {
            // monaural
            vol *= 0.5;
            for (i = start << 1; i < imax; ) {
                n = (double) (pointerL.i + pointerR.i) * vol;
                buffer[i] += n;
                i++;
                buffer[i] += n;
                i++;
                pointerL = pointerL.next;
                pointerR = pointerR.next;
            }
        }
    }

    /** write buffer by Vector.&lt;Number&gt; */
    public void writeVectorNumber(double[] pointer, int startPointer, int startBuffer, int len, double vol, int pan, int sampleChannelCount) {
        int i, j;
        double n;
        int jmax;
        double volL, volR;

        if (channels == 2) {
            if (sampleChannelCount == 2) {
                // stereo data to stereo buffer
                volL = _panTable[128 - pan] * vol;
                volR = _panTable[pan] * vol;
                jmax = (startPointer + len) << 1;
                for (j = startPointer << 1, i = startBuffer << 1; j < jmax; ) {
                    buffer[i] += pointer[j] * volL;
                    j++;
                    i++;
                    buffer[i] += pointer[j] * volR;
                    j++;
                    i++;
                }
            } else {
                // monaural data to stereo buffer
                volL = _panTable[128 - pan] * vol * 0.707;
                volR = _panTable[pan] * vol * 0.707;
                jmax = startPointer + len;
                for (j = startPointer, i = startBuffer << 1; j < jmax; j++) {
                    n = pointer[j];
                    buffer[i] += n * volL;
                    i++;
                    buffer[i] += n * volR;
                    i++;
                }
            }
        } else if (channels == 1) {
            if (sampleChannelCount == 2) {
                // stereo data to monaural buffer
                jmax = (startPointer + len) << 1;
                vol *= 0.5;
                for (j = startPointer << 1, i = startBuffer << 1; j < jmax; ) {
                    n = pointer[j];
                    j++;
                    n += pointer[j];
                    j++;
                    n *= vol;
                    buffer[i] += n;
                    i++;
                    buffer[i] += n;
                    i++;
                }
            } else {
                // monaural data to monaural buffer
                jmax = startPointer + len;
                for (j = startPointer, i = startBuffer << 1; j < jmax; j++) {
                    n = pointer[j] * vol;
                    buffer[i] += n;
                    i++;
                    buffer[i] += n;
                    i++;
                }
            }
        }
    }

    /** write buffer by ByteArray (stereo only). */
    public void writeByteArray(ByteArray bytes, int start, int len, double vol) {
        int i;
        double n;
        int imax = (start + len) << 1;
        int initPosition = bytes.position;

        if (channels == 2) {
            for (i = start << 1; i < imax; i++) {
                buffer[i] += bytes.readFloat() * vol;
            }
        } else if (channels == 1) {
            // stereo data to monaural buffer
            vol *= 0.6;
            for (i = start << 1; i < imax; ) {
                n = (bytes.readFloat() + bytes.readFloat()) * vol;
                buffer[i] += n;
                i++;
                buffer[i] += n;
                i++;
            }
        }

        bytes.position = initPosition;
    }
}
