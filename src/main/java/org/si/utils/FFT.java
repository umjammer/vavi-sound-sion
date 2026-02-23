//
// Optimized Fast Fourier Transform module.
//  Ported and modified by keim. The code is optimized for Flash10. 
//  This soruce code is distributed under BSD-style license (see org.si.license.txt).
// [REMARKS]
//  The optimization reduces the calculation time from 1575[ms] to 268[ms] in my PC.
//  And compare with the simple recursive Cooley-Tukey calculation, the speed is about 10 times faster.
//  
// Original code (written by c)
//  The original source code is free licensed.
//----- < Following text is from original code's readme.txt. > -----
//    Copyright(C) 1996-2001 Takuya OOURA
//    email: ooura@mmm.t.u-tokyo.ac.jp
//    download: http://momonga.t.u-tokyo.ac.jp/~ooura/fft.html
//    You may use, copy, modify this code for any purpose and 
//    without fee. You may distribute this ORIGINAL package.
//----- < up to here > -----
// [NOTES] Now the download site is moved to http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html.
//         And the email address might be unavailable.
//

package org.si.utils;


/**
 * Fast Fourier Transform module optimized for Flash10.
 * <p>
 * Basic usage for complex discrete Fourier transfer.
 * <pre>
 * // variables
 * FFT fft = new FFT(1024);    // specify source data length (must be 2^n)
 *
 * Vector source.&lt;Number&gt; = new Vector.&lt;Number&gt;(1024);     // source vector
 * Vector result.&lt;Number&gt; = new Vector.&lt;Number&gt;(1024);     // result vector
 * Vector magnitude.&lt;Number&gt; = new Vector.&lt;Number&gt;(512);   // result vector for magnitude (half size of source)
 * Vector phase.&lt;Number&gt; = new Vector.&lt;Number&gt;(512);   // result vector for phase (half size of source)
 *
 * // complex source data
 * for (int i=0; i&lt;1024; i+=2) {
 *   source[i]   = (i&lt;512) ? -1 : 1;     // even members for real part
 *   source[i+1] = 0;                    // odd members for imaginal part
 * }
 *
 * // calculation
 * fft.setData(source);                // set source data
 * fft.calcFFT();                      // calculate FFT
 *
 * // get result
 * trace(fft.re);                      // real part of FFT result (length = 512)
 * trace(fft.im);                      // imaginal part of FFT result (length = 512)
 * fft.getData(result);                // recieve combinated result by vector
 * trace(result);                      // combinated result (length = 1024, even for real/odd for imaginal)
 * trace(fft.getMagnitude(magnitude)); // vector for magnitude (length = 512)
 * trace(fft.getPhase(phase));         // vector for phase (length = 512)
 *
 * // reconstruction
 * fft.calcIFFT().scale(1/512);    // calculate Inversed FFT with previous result (re and im).
 * // [REMARKS] The result from calcIFFT(), calcRealIFFT() or calcDCT() is scaled by length/2.
 *
 * // get result (another way to get result vector)
 * Vector reconstructed.&lt;Number&gt; = fft.getData();  // allocate and return result vector (length = 1024)
 *
 * // "fft.setData(source).calcFFT().calcIFFT().scale(2/source.length).getData()" is same as source.
 * trace(reconstructed);
 * </pre>
 * example Basic usage for FFT (for real values) and DCT.
 * <pre>
 * // create FFT module
 * FFT fft = new FFT(1024);    // specify source data length (must be 2^n)
 *
 * // real number source
 * Vector source.&lt;Number&gt; = new Vector.&lt;Number&gt;(1024);
 * for (int i=0; i&lt;1024; i++) {
 *   source[i] = 1-i/512;        // simple real number vector
 * }
 *
 * // calculate and get result
 * fft.setDat(source).calcRealFFT();                   // calculate FFT for real numbers
 * trace(fft.re);                                      // real part of FFT result(length = 512)
 * trace(fft.im);                                      // imaginal part of FFT result (length = 512)
 * trace(fft.setDat(source).calcDCT().getIntensity()); // intensity of DCT result (length = 512)
 * </pre>
 */
public class FFT {

    // variables
    //

    /** Vector for real numbers, you can set data and get result by this valiables, the length instanceof a HALF of the source data length. */
    public double[] re;
    /** Vector for imaginal numbers, you can set data and get result by this valiables, the length instanceof a HALF of the source data length. */
    public double[] im;

    private int _length = 0;
    private double[] _cosTable = null;
    private final int[] _bitrvTemp = new int[256];
    private double[] _waveTabler = null;
    private double[] _waveTablei = null;

    // properties
    //

    /** source data length */
    public int getLength() {
        return _length;
    }

    // constructor
    //

    /** constructor, specify source data length. The length must be 2^n. */
    public FFT(int len) {
        _initialize(len >> 1);
    }

    // calculation (functions from original code)
    //

    /**
     * [Functions from original code] Complex Discrete Fourier Tranform
     *
     * @param isgn 1 for FFT, -1 for IFFT.
     * @param src  data to transform. The length must be ((you) same) passed to constructor.
     */
    public void cdft(int isgn, double[] src) {
        setData(src);
        if (isgn >= 0) calcFFT();
        else calcIFFT();
        getData(src);
    }

    /**
     * [Functions from original code] Real Discrete Fourier Tranform
     *
     * @param isgn 1 for FFT, -1 for IFFT.
     * @param src  data to transform. The length must be ((you) same) passed to constructor.
     */
    public void rdft(int isgn, double[] src) {
        setData(src);
        if (isgn >= 0) calcRealFFT();
        else calcRealIFFT();
        getData(src);
    }

    /**
     * [Functions from original code] Discrete Cosine Tranform
     *
     * @param isgn [ATTENTION] -1 for DCT, 1 for IDCT. Opposite from FFT.
     * @param src  data to transform. The length must be ((you) same) passed to constructor.
     */
    public void ddct(int isgn, double[] src) {
        setData(src);
        if (isgn >= 0) calcIDCT();
        else calcDCT();
        getData(src);
    }

    /**
     * [Functions from original code] Discrete Sine Tranform (has some bugs in current version)
     *
     * @param isgn [ATTENTION] -1 for DST, 1 for IDST. Opposite from FFT.
     * @param src  data to transform. The length must be ((you) same) passed to constructor.
     */
    public void ddst(int isgn, double[] src) {
        int j;
        double xr;

        setData(src);
        if (isgn >= 0) {
            xr = im[_length - 1];
            for (j = _length - 1; j >= 1; j--) {
                im[j] = -re[j] - im[j - 1];
                re[j] -= im[j - 1];
            }
            im[0] = re[0] + xr;
            re[0] -= xr;
            _rftbsub();
            _bitrv2();
            _cftbsub();
            _dstsub();
        } else {
            _dstsub();
            _bitrv2();
            _cftfsub();
            _rftfsub();
            xr = re[0] - im[0];
            re[0] += im[0];
            for (j = 1; j < _length; j++) {
                im[j - 1] = -re[j] - im[j];
                re[j] -= im[j];
            }
            im[_length - 1] = xr;
        }
        getData(src);
    }

    // setter
    //

    /**
     * Set source data, the passed vector instanceof copied to "re" and "im" properties, the length of "src" must be ((you) same) passed to constructor.
     *
     * @return this instance
     */
    public FFT setData(double[] src) {
        int i, i2;
        for (i = 0, i2 = 0; i < _length; i++) {
            re[i] = src[i2];
            i2++;
            im[i] = src[i2];
            i2++;
        }
        return this;
    }

    // getter
    //

    /**
     * Get result. The returned vector instanceof a combination of "re"(even member) and "im"(odd member) properties.
     *
     * @param dst Vector to recieve the result. The length must be ((you) same) passed to constructor. Allocate vector inside when you pass null.
     * @return A combination of "re"(even member) and "im"(odd member) properties.
     */
    public double[] getData(double[] dst) {
        if (dst == null) dst = new double[_length << 1];
        int i, i2;
        for (i = 0, i2 = 0; i < _length; i++) {
            dst[i2] = re[i];
            i2++;
            dst[i2] = im[i];
            i2++;
        }
        return dst;
    }

    /**
     * Get intensity (re^2+im^2).
     *
     * @param dst Vector to recieve intensities. The length must be HALF of the source data length. Allocate vector inside when you pass null.
     * @return Vector of intensities, same ((passed) vector) by the arugument when its not null.
     */
    public double[] getIntensity(double[] dst) {
        int i;
        double x, y;
        if (dst == null) dst = new double[_length];
        for (i = 0; i < _length; i++) {
            x = re[i];
            y = im[i];
            dst[i] = x * x + y * y;
        }
        return dst;
    }

    /**
     * Get magnitude (sqrt(re^2+im^2)).
     *
     * @param dst The vector to recieve magnitudes. The length must be HALF of the source data length. Allocate vector inside when you pass null.
     * @return Vector of magnitudes, same ((passed) vector) by the arugument when its not null.
     */
    public double[] getMagnitude(double[] dst) {
        int i;
        double x, y;
        if (dst == null) dst = new double[_length];
        for (i = 0; i < _length; i++) {
            x = re[i];
            y = im[i];
            dst[i] = Math.sqrt(x * x + y * y);
        }
        return dst;
    }

    /**
     * Get phase (atan2(re^2+im^2)).
     *
     * @param dst The vector to recieve phases. The length must be HALF of the source data length. Allocate vector inside when you pass null.
     * @return Vector of phases, same ((passed) vector) by the arugument when its not null.
     */
    public double[] getPhase(double[] dst) {
        if (dst == null) dst = new double[_length];
        for (int i = 0; i < _length; i++) {
            dst[i] = Math.atan2(im[i], re[i]);
        }
        return dst;
    }

    // calculator
    //

    /**
     * Scaling data.
     *
     * @param n factor
     * @return this instance
     */
    public FFT scale(double n) {
        for (int i = 0; i < _length; i++) {
            re[i] *= n;
            im[i] *= n;
        }
        return this;
    }

    /**
     * Calculate Fast Fourier Transform with complex numbers.
     *
     * @return this instance
     */
    public FFT calcFFT() {
        _bitrv2();
        _cftfsub();
        return this;
    }

    /**
     * Calculate Inversed Fast Fourier Transform with complex numbers.
     *
     * @return this instance
     */
    public FFT calcIFFT() {
        _bitrv2conj();
        _cftbsub();
        return this;
    }

    /**
     * Calculate Fast Fourier Transform with real numbers.
     *
     * @return this instance
     */
    public FFT calcRealFFT() {
        _bitrv2();
        _cftfsub();
        _rftfsub();
        double xi = re[0] - im[0];
        re[0] += im[0];
        im[0] = xi;
        return this;
    }

    /**
     * Calculate Inversed Fast Fourier Transform with real numbers.
     *
     * @return this instance
     */
    public FFT calcRealIFFT() {
        im[0] = 0.5 * (re[0] - im[0]);
        re[0] -= im[0];
        _rftbsub();
        _bitrv2();
        _cftbsub();
        return this;
    }

    /**
     * Calculate Discrete Cosine Transform
     *
     * @return this instance
     */
    public FFT calcDCT() {
        int j, dj = _length - 1;
        double xr;
        xr = im[dj];
        for (j = _length - 1; j >= 1; j--) {
            dj = j - 1;
            im[j] = re[j] - im[dj];
            re[j] += im[dj];
        }
        im[0] = re[0] - xr;
        re[0] += xr;
        _rftbsub();
        _bitrv2();
        _cftbsub();
        _dctsub();
        return this;
    }

    /**
     * Calculate Inversed Discrete Cosine Transform
     *
     * @return this instance
     */
    public FFT calcIDCT() {
        int j, dj;
        double xr;
        _dctsub();
        _bitrv2();
        _cftfsub();
        _rftfsub();
        xr = re[0] - im[0];
        re[0] += im[0];
        dj = 0;
        for (j = 1; j < _length; j++) {
            im[dj] = re[j] - im[j];
            re[j] += im[j];
            dj = j;
        }
        im[dj] = xr;
        return this;
    }

    // internal function
    //
    // initializer
    private void _initialize(int len) {
        // length = 2^n && length >= 8
        int l;
        for (l = 8; l < len; ) l <<= 1;
        len = l;
        int tableLength = len >> 2;

        // wave table
        _waveTabler = new double[tableLength];
        _waveTablei = new double[tableLength];
        int i, imax = len >> 3;
        double dt = 6.283185307179586 / len;
        _waveTabler[0] = 1;
        _waveTablei[0] = 0;
        _waveTabler[imax] = _waveTablei[imax] = Math.cos(0.7853981633974483);
        for (i = 1; i < imax; i++) {
            _waveTablei[tableLength - i] = _waveTabler[i] = Math.cos(i * dt);
            _waveTabler[tableLength - i] = _waveTablei[i] = Math.sin(i * dt);
        }
        // bit scrambling
        re = _waveTabler;
        im = _waveTablei;
        _length = tableLength;
        _bitrv2();

        // cosine table
        imax = len << 1;
        _cosTable = new double[imax];
        dt = 1.5707963267948965 / imax;
        for (i = 0; i < imax; i++) _cosTable[i] = Math.cos(i * dt) * 0.5;

        // allocate calculation area
        re = new double[len];
        im = new double[len];
        _length = len;
    }

    // bit reverse
    private void _bitrv2() {
        int j, j1, k, k1;
        double xr, xi, yr, yi;

        _bitrvTemp[0] = 0;
        int l = _length, m = 1;
        while ((m << 2) < l) {
            l >>= 1;
            for (j = 0; j < m; j++) _bitrvTemp[m + j] = _bitrvTemp[j] + l;
            m <<= 1;
        }

        if ((m << 2) == l) {
            for (k = 0; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = j + _bitrvTemp[k];
                    k1 = k + _bitrvTemp[j];
                    xr = re[j1];
                    xi = im[j1];
                    yr = re[k1];
                    yi = im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 += m + m;
                    xr = re[j1];
                    xi = im[j1];
                    yr = re[k1];
                    yi = im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 -= m;
                    xr = re[j1];
                    xi = im[j1];
                    yr = re[k1];
                    yi = im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 += m + m;
                    xr = re[j1];
                    xi = im[j1];
                    yr = re[k1];
                    yi = im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                }
                j1 = k + m + _bitrvTemp[k];
                k1 = j1 + m;
                xr = re[j1];
                xi = im[j1];
                yr = re[k1];
                yi = im[k1];
                re[j1] = yr;
                im[j1] = yi;
                re[k1] = xr;
                im[k1] = xi;
            }
        } else {
            for (k = 1; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = j + _bitrvTemp[k];
                    k1 = k + _bitrvTemp[j];
                    xr = re[j1];
                    xi = im[j1];
                    yr = re[k1];
                    yi = im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 += m;
                    xr = re[j1];
                    xi = im[j1];
                    yr = re[k1];
                    yi = im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                }
            }
        }
    }

    // bit reverse (conjugation)
    private void _bitrv2conj() {
        int j, j1, k, k1;
        double xr, xi, yr, yi;

        _bitrvTemp[0] = 0;
        int l = _length, m = 1;
        while ((m << 2) < l) {
            l >>= 1;
            for (j = 0; j < m; j++) _bitrvTemp[m + j] = _bitrvTemp[j] + l;
            m <<= 1;
        }

        if ((m << 2) == l) {
            for (k = 0; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = j + _bitrvTemp[k];
                    k1 = k + _bitrvTemp[j];
                    xr = re[j1];
                    xi = -im[j1];
                    yr = re[k1];
                    yi = -im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 += m + m;
                    xr = re[j1];
                    xi = -im[j1];
                    yr = re[k1];
                    yi = -im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 -= m;
                    xr = re[j1];
                    xi = -im[j1];
                    yr = re[k1];
                    yi = -im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 += m + m;
                    xr = re[j1];
                    xi = -im[j1];
                    yr = re[k1];
                    yi = -im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                }
                k1 = k + _bitrvTemp[k];
                im[k1] = -im[k1];
                j1 = k1 + m;
                k1 = j1 + m;
                xr = re[j1];
                xi = -im[j1];
                yr = re[k1];
                yi = -im[k1];
                re[j1] = yr;
                im[j1] = yi;
                re[k1] = xr;
                im[k1] = xi;
                k1 += m;
                im[k1] = -im[k1];
            }
        } else {
            im[0] = -im[0];
            im[m] = -im[m];
            for (k = 1; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = j + _bitrvTemp[k];
                    k1 = k + _bitrvTemp[j];
                    xr = re[j1];
                    xi = -im[j1];
                    yr = re[k1];
                    yi = -im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                    j1 += m;
                    k1 += m;
                    xr = re[j1];
                    xi = -im[j1];
                    yr = re[k1];
                    yi = -im[k1];
                    re[j1] = yr;
                    im[j1] = yi;
                    re[k1] = xr;
                    im[k1] = xi;
                }
                k1 = k + _bitrvTemp[k];
                im[k1] = -im[k1];
                im[k1 + m] = -im[k1 + m];
            }
        }
    }

    // sub routines
    //
    private void _cftfsub() {
        int j0, j1, j2, j3, l;
        double x0r, x1r, x2r, x3r,
                x0i, x1i, x2i, x3i;

        _cft1st();
        l = 4;
        while ((l << 2) < _length) {
            _cftmdl(l);
            l <<= 2;
        }

        if ((l << 2) == _length) {
            for (j0 = 0; j0 < l; j0++) {
                j1 = j0 + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = re[j0] + re[j1];
                x0i = im[j0] + im[j1];
                x1r = re[j0] - re[j1];
                x1i = im[j0] - im[j1];
                x2r = re[j2] + re[j3];
                x2i = im[j2] + im[j3];
                x3r = re[j2] - re[j3];
                x3i = im[j2] - im[j3];
                re[j0] = x0r + x2r;
                im[j0] = x0i + x2i;
                re[j2] = x0r - x2r;
                im[j2] = x0i - x2i;
                re[j1] = x1r - x3i;
                im[j1] = x1i + x3r;
                re[j3] = x1r + x3i;
                im[j3] = x1i - x3r;
            }
        } else {
            for (j0 = 0; j0 < l; j0++) {
                j1 = j0 + l;
                x0r = re[j0] - re[j1];
                x0i = im[j0] - im[j1];
                re[j0] += re[j1];
                im[j0] += im[j1];
                re[j1] = x0r;
                im[j1] = x0i;
            }
        }
    }

    private void _cftbsub() {
        int j0, j1, j2, j3, l;
        double x0r, x1r, x2r, x3r,
                x0i, x1i, x2i, x3i;

        _cft1st();
        l = 4;
        while ((l << 2) < _length) {
            _cftmdl(l);
            l <<= 2;
        }

        if ((l << 2) == _length) {
            for (j0 = 0; j0 < l; j0++) {
                j1 = j0 + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = re[j0] + re[j1];
                x0i = -im[j0] - im[j1];
                x1r = re[j0] - re[j1];
                x1i = -im[j0] + im[j1];
                x2r = re[j2] + re[j3];
                x2i = im[j2] + im[j3];
                x3r = re[j2] - re[j3];
                x3i = im[j2] - im[j3];
                re[j0] = x0r + x2r;
                im[j0] = x0i - x2i;
                re[j2] = x0r - x2r;
                im[j2] = x0i + x2i;
                re[j1] = x1r - x3i;
                im[j1] = x1i - x3r;
                re[j3] = x1r + x3i;
                im[j3] = x1i + x3r;
            }
        } else {
            for (j0 = 0; j0 < l; j0++) {
                j1 = j0 + l;
                x0r = re[j0] - re[j1];
                x0i = -im[j0] + im[j1];
                re[j0] += re[j1];
                im[j0] = -im[j0] - im[j1];
                re[j1] = x0r;
                im[j1] = x0i;
            }
        }
    }

    private void _cft1st() {
        int j0, j1, j2, j3, k1, k2;
        double wk1r, wk2r, wk3r, x0r, x1r, x2r, x3r,
                wk1i, wk2i, wk3i, x0i, x1i, x2i, x3i;

        x0r = re[0] + re[1];
        x0i = im[0] + im[1];
        x1r = re[0] - re[1];
        x1i = im[0] - im[1];
        x2r = re[2] + re[3];
        x2i = im[2] + im[3];
        x3r = re[2] - re[3];
        x3i = im[2] - im[3];
        re[0] = x0r + x2r;
        im[0] = x0i + x2i;
        re[2] = x0r - x2r;
        im[2] = x0i - x2i;
        re[1] = x1r - x3i;
        im[1] = x1i + x3r;
        re[3] = x1r + x3i;
        im[3] = x1i - x3r;
        wk1r = _waveTabler[1];
        x0r = re[4] + re[5];
        x0i = im[4] + im[5];
        x1r = re[4] - re[5];
        x1i = im[4] - im[5];
        x2r = re[6] + re[7];
        x2i = im[6] + im[7];
        x3r = re[6] - re[7];
        x3i = im[6] - im[7];
        re[4] = x0r + x2r;
        im[4] = x0i + x2i;
        re[6] = x2i - x0i;
        im[6] = x0r - x2r;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        re[5] = wk1r * (x0r - x0i);
        im[5] = wk1r * (x0r + x0i);
        x0r = x3i + x1r;
        x0i = x3r - x1i;
        re[7] = wk1r * (x0i - x0r);
        im[7] = wk1r * (x0i + x0r);
        k1 = 0;
        for (j0 = 8; j0 < _length; j0 += 4) {
            j1 = j0 + 1;
            j2 = j1 + 1;
            j3 = j2 + 1;
            k1++;
            k2 = 2 * k1;
            wk2r = _waveTabler[k1];
            wk2i = _waveTablei[k1];
            wk1r = _waveTabler[k2];
            wk1i = _waveTablei[k2];
            wk3r = wk1r - 2 * wk2i * wk1i;
            wk3i = 2 * wk2i * wk1r - wk1i;
            x0r = re[j0] + re[j1];
            x0i = im[j0] + im[j1];
            x1r = re[j0] - re[j1];
            x1i = im[j0] - im[j1];
            x2r = re[j2] + re[j3];
            x2i = im[j2] + im[j3];
            x3r = re[j2] - re[j3];
            x3i = im[j2] - im[j3];
            re[j0] = x0r + x2r;
            im[j0] = x0i + x2i;
            x0r -= x2r;
            x0i -= x2i;
            re[j2] = wk2r * x0r - wk2i * x0i;
            im[j2] = wk2r * x0i + wk2i * x0r;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            re[j1] = wk1r * x0r - wk1i * x0i;
            im[j1] = wk1r * x0i + wk1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            re[j3] = wk3r * x0r - wk3i * x0i;
            im[j3] = wk3r * x0i + wk3i * x0r;

            k2++;
            wk1r = _waveTabler[k2];
            wk1i = _waveTablei[k2];
            wk3r = wk1r - 2 * wk2r * wk1i;
            wk3i = 2 * wk2r * wk1r - wk1i;
            j0 += 4;
            j1 = j0 + 1;
            j2 = j1 + 1;
            j3 = j2 + 1;
            x0r = re[j0] + re[j1];
            x0i = im[j0] + im[j1];
            x1r = re[j0] - re[j1];
            x1i = im[j0] - im[j1];
            x2r = re[j2] + re[j3];
            x2i = im[j2] + im[j3];
            x3r = re[j2] - re[j3];
            x3i = im[j2] - im[j3];
            re[j0] = x0r + x2r;
            im[j0] = x0i + x2i;
            x0r -= x2r;
            x0i -= x2i;
            re[j2] = -wk2i * x0r - wk2r * x0i;
            im[j2] = -wk2i * x0i + wk2r * x0r;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            re[j1] = wk1r * x0r - wk1i * x0i;
            im[j1] = wk1r * x0i + wk1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            re[j3] = wk3r * x0r - wk3i * x0i;
            im[j3] = wk3r * x0i + wk3i * x0r;
        }
    }

    private void _cftmdl(int l) {
        int j0, j1, j2, j3, k, k1, k2, m, m2;
        double wk1r, wk2r, wk3r, x0r, x1r, x2r, x3r,
                wk1i, wk2i, wk3i, x0i, x1i, x2i, x3i;

        m = l << 2;
        for (j0 = 0; j0 < l; j0++) {
            j1 = j0 + l;
            j2 = j1 + l;
            j3 = j2 + l;
            x0r = re[j0] + re[j1];
            x0i = im[j0] + im[j1];
            x1r = re[j0] - re[j1];
            x1i = im[j0] - im[j1];
            x2r = re[j2] + re[j3];
            x2i = im[j2] + im[j3];
            x3r = re[j2] - re[j3];
            x3i = im[j2] - im[j3];
            re[j0] = x0r + x2r;
            im[j0] = x0i + x2i;
            re[j2] = x0r - x2r;
            im[j2] = x0i - x2i;
            re[j1] = x1r - x3i;
            im[j1] = x1i + x3r;
            re[j3] = x1r + x3i;
            im[j3] = x1i - x3r;
        }
        wk1r = _waveTabler[1];
        for (j0 = m; j0 < l + m; j0++) {
            j1 = j0 + l;
            j2 = j1 + l;
            j3 = j2 + l;
            x0r = re[j0] + re[j1];
            x0i = im[j0] + im[j1];
            x1r = re[j0] - re[j1];
            x1i = im[j0] - im[j1];
            x2r = re[j2] + re[j3];
            x2i = im[j2] + im[j3];
            x3r = re[j2] - re[j3];
            x3i = im[j2] - im[j3];
            re[j0] = x0r + x2r;
            im[j0] = x0i + x2i;
            re[j2] = x2i - x0i;
            im[j2] = x0r - x2r;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            re[j1] = wk1r * (x0r - x0i);
            im[j1] = wk1r * (x0r + x0i);
            x0r = x3i + x1r;
            x0i = x3r - x1i;
            re[j3] = wk1r * (x0i - x0r);
            im[j3] = wk1r * (x0i + x0r);
        }
        k1 = 0;
        m2 = 2 * m;
        for (k = m2; k < _length; k += m2) {
            k1 += 1;
            k2 = 2 * k1;
            wk2r = _waveTabler[k1];
            wk2i = _waveTablei[k1];
            wk1r = _waveTabler[k2];
            wk1i = _waveTablei[k2];
            wk3r = wk1r - 2 * wk2i * wk1i;
            wk3i = 2 * wk2i * wk1r - wk1i;
            for (j0 = k; j0 < l + k; j0++) {
                j1 = j0 + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = re[j0] + re[j1];
                x0i = im[j0] + im[j1];
                x1r = re[j0] - re[j1];
                x1i = im[j0] - im[j1];
                x2r = re[j2] + re[j3];
                x2i = im[j2] + im[j3];
                x3r = re[j2] - re[j3];
                x3i = im[j2] - im[j3];
                re[j0] = x0r + x2r;
                im[j0] = x0i + x2i;
                x0r -= x2r;
                x0i -= x2i;
                re[j2] = wk2r * x0r - wk2i * x0i;
                im[j2] = wk2r * x0i + wk2i * x0r;
                x0r = x1r - x3i;
                x0i = x1i + x3r;
                re[j1] = wk1r * x0r - wk1i * x0i;
                im[j1] = wk1r * x0i + wk1i * x0r;
                x0r = x1r + x3i;
                x0i = x1i - x3r;
                re[j3] = wk3r * x0r - wk3i * x0i;
                im[j3] = wk3r * x0i + wk3i * x0r;
            }
            k2++;
            wk1r = _waveTabler[k2];
            wk1i = _waveTablei[k2];
            wk3r = wk1r - 2 * wk2r * wk1i;
            wk3i = 2 * wk2r * wk1r - wk1i;
            for (j0 = k + m; j0 < l + (k + m); j0++) {
                j1 = j0 + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = re[j0] + re[j1];
                x0i = im[j0] + im[j1];
                x1r = re[j0] - re[j1];
                x1i = im[j0] - im[j1];
                x2r = re[j2] + re[j3];
                x2i = im[j2] + im[j3];
                x3r = re[j2] - re[j3];
                x3i = im[j2] - im[j3];
                re[j0] = x0r + x2r;
                im[j0] = x0i + x2i;
                x0r -= x2r;
                x0i -= x2i;
                re[j2] = -wk2i * x0r - wk2r * x0i;
                im[j2] = -wk2i * x0i + wk2r * x0r;
                x0r = x1r - x3i;
                x0i = x1i + x3r;
                re[j1] = wk1r * x0r - wk1i * x0i;
                im[j1] = wk1r * x0i + wk1i * x0r;
                x0r = x1r + x3i;
                x0i = x1i - x3r;
                re[j3] = wk3r * x0r - wk3i * x0i;
                im[j3] = wk3r * x0i + wk3i * x0r;
            }
        }
    }

    private void _rftfsub() {
        int j, k, kk, m, ctLength = _cosTable.length;
        double wkr, wki, xr, xi, yr, yi;

        m = _length >> 1;
        kk = 0;
        for (j = 1; j < m; j++) {
            k = _length - j;
            kk += 4;
            wkr = 0.5 - _cosTable[ctLength - kk];
            wki = _cosTable[kk];
            xr = re[j] - re[k];
            xi = im[j] + im[k];
            yr = wkr * xr - wki * xi;
            yi = wkr * xi + wki * xr;
            re[j] -= yr;
            im[j] -= yi;
            re[k] += yr;
            im[k] -= yi;
        }
    }

    private void _rftbsub() {
        int j, k, kk, m, ctLength = _cosTable.length;
        double wkr, wki, xr, xi, yr, yi;

        im[0] = -im[0];
        m = _length >> 1;
        kk = 0;
        for (j = 1; j < m; j++) {
            k = _length - j;
            kk += 4;
            wkr = 0.5 - _cosTable[ctLength - kk];
            wki = _cosTable[kk];
            xr = re[j] - re[k];
            xi = im[j] + im[k];
            yr = wkr * xr + wki * xi;
            yi = wkr * xi - wki * xr;
            re[j] -= yr;
            im[j] = yi - im[j];
            re[k] += yr;
            im[k] = yi - im[k];
        }
        im[m] = -im[m];
    }

    private void _dctsub() {
        int j, k, kk, ikk, m;
        double wkr, wki, xr;
        int ctLength = _cosTable.length;

        m = _length >> 1;
        k = _length - 1;
        kk = 1;
        ikk = ctLength - 1;
        wkr = _cosTable[kk] - _cosTable[ikk];
        wki = _cosTable[kk] + _cosTable[ikk];
        xr = wki * im[0] - wkr * im[k];
        im[0] = wkr * im[0] + wki * im[k];
        im[k] = xr;
        for (j = 1; j < m; j++) {
            k = _length - j;
            kk++;
            ikk--;
            wkr = _cosTable[kk] - _cosTable[ikk];
            wki = _cosTable[kk] + _cosTable[ikk];
            xr = wki * re[j] - wkr * re[k];
            re[j] = wkr * re[j] + wki * re[k];
            re[k] = xr;
            k--;
            kk++;
            ikk--;
            wkr = _cosTable[kk] - _cosTable[ikk];
            wki = _cosTable[kk] + _cosTable[ikk];
            xr = wki * im[j] - wkr * im[k];
            im[j] = wkr * im[j] + wki * im[k];
            im[k] = xr;
        }
        re[m] *= 0.7071067811865476; // cos(pi/4)
    }

    private void _dstsub() {
        int j, k, kk, ikk, m;
        double wkr, wki, xr;
        int ctLength = _cosTable.length;

        m = _length >> 1;
        k = _length - 1;
        kk = 1;
        ikk = ctLength - 1;
        wkr = _cosTable[kk] - _cosTable[ikk];
        wki = _cosTable[kk] + _cosTable[ikk];
        xr = wki * im[k] - wkr * im[0];
        im[k] = wkr * im[k] + wki * im[0];
        im[0] = xr;
        for (j = 1; j < m; j++) {
            k = _length - j;
            kk++;
            ikk--;
            wkr = _cosTable[kk] - _cosTable[ikk];
            wki = _cosTable[kk] + _cosTable[ikk];
            xr = wki * re[k] - wkr * re[j];
            re[k] = wkr * re[k] + wki * re[j];
            re[j] = xr;
            k--;
            kk++;
            ikk--;
            wkr = _cosTable[kk] - _cosTable[ikk];
            wki = _cosTable[kk] + _cosTable[ikk];
            xr = wki * im[k] - wkr * im[j];
            im[k] = wkr * im[k] + wki * im[j];
            im[j] = xr;
        }
        re[m] *= 0.7071067811865476; // cos(pi/4)
    }
}
