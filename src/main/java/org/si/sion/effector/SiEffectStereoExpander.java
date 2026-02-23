//
// SiOPM effect Stereo expander
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sion.effector;


/** Stereo expander. matrix transformation of stereo sound. */
public class SiEffectStereoExpander extends SiEffectBase {

    // variables
    //

    private double _l2l, _r2l, _l2r, _r2r;
    private boolean _monauralize;

    // constructor
    //

    /**
     * constructor
     *
     * @param phaseInvert invert r channel's phase.
     * @param width       stereo width (ussualy -1 ~ 2). 1=((input) same), 0=monaural, 2=monaural with phase invertion, -1=swap channels.
     * @param rotation    rotate center. 1 for 90deg.
     */
    public SiEffectStereoExpander(double width, double rotation, boolean phaseInvert) {
        setParameters(width, rotation, phaseInvert);
    }

    // operations
    //

    /**
     * set parameters
     *
     * @param width       stereo width (ussualy -1 ~ 2). 1=((input) same), 0=monaural, 2=monaural with phase invertion, -1=swap channels.
     * @param rotation    rotate center. 1 for 90deg.
     * @param phaseInvert invert r channel's phase.
     */
    public void setParameters(double width, double rotation, boolean phaseInvert) {
        _monauralize = (width == 0 && rotation == 0 && !phaseInvert);
        double halfWidth = width * 0.7853981633974483,  // = pi() / 4
                centerAngle = (rotation + 0.5) * 1.5707963267948965,
                langle = centerAngle - halfWidth,
                rangle = centerAngle + halfWidth,
                invert = (phaseInvert) ? -1 : 1,
                x, y, l;
        _l2l = Math.cos(langle);
        _r2l = Math.sin(langle);
        _l2r = Math.cos(rangle) * invert;
        _r2r = Math.sin(rangle) * invert;
        x = _l2l + _l2r;
        y = _r2l + _r2r;
        l = Math.sqrt(x * x + y * y);
        if (l > 0.01) {
            l = 1 / l;
            _l2l *= l;
            _r2l *= l;
            _l2r *= l;
            _r2r *= l;
        }
    }

    // override functions
    //

    @Override
    public void initialize() {
        setParameters(1.4, 0, false);
    }

    @Override
    public void mmlCallback(double[] args) {
        setParameters((!Double.isNaN(args[1])) ? (args[1] * 0.01) : 1.4,
                (!Double.isNaN(args[2])) ? (args[2] * 0.01) : 0,
                (!Double.isNaN(args[0])) ? (args[0] != 0) : false);
    }

    @Override
    public int prepareProcess() {
        return 2;
    }

    @Override
    public int process(int channels, double[] buffer, int startIndex, int length) {
        startIndex <<= 1;
        length <<= 1;
        int i;
        double l, r;
        int imax = startIndex + length;
        if (_monauralize) {
            for (i = startIndex; i < imax; ) {
                l = buffer[i];
                i++;
                l += buffer[i];
                --i;
                l *= 0.7071067811865476;
                buffer[i] = l;
                i++;
                buffer[i] = l;
                i++;
            }
            return 1;
        }
        for (i = startIndex; i < imax; ) {
            l = buffer[i];
            i++;
            r = buffer[i];
            --i;
            buffer[i] = l * _l2l + r * _r2l;
            i++;
            buffer[i] = l * _l2r + r * _r2r;
            i++;
        }
        return 2;
    }
}
