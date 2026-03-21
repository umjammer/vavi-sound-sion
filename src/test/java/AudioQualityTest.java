/*
 * Audio Quality Test - Mathematical comparison of Java SC55 output vs Original C++
 */

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * Mathematical audio quality comparison test.
 * Compares SPI output with original API reference.
 * TODO this test passes. this cannot probe the difference.
 */
class AudioQualityTest {

    /**
     * Read WAV file samples as normalized float array (-1.0 to 1.0)
     */
    static float[] readWavSamples(String path) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(path, "r");

        // Read RIFF header
        byte[] header = new byte[44];
        raf.read(header);

        // Parse format info
        ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int riff = bb.getInt();  // "RIFF"
        int fileSize = bb.getInt();
        int wave = bb.getInt();  // "WAVE"
        int fmt = bb.getInt();   // "fmt "
        int fmtSize = bb.getInt();
        short audioFormat = bb.getShort();
        short numChannels = bb.getShort();
        int sampleRate = bb.getInt();
        int byteRate = bb.getInt();
        short blockAlign = bb.getShort();
        short bitsPerSample = bb.getShort();

        // Skip to data chunk
        int dataMarker = bb.getInt();  // "data"
        int dataSize = bb.getInt();

        System.out.printf("WAV: %s - %d Hz, %d ch, %d bit, %d samples%n",
            path, sampleRate, numChannels, bitsPerSample, dataSize / (bitsPerSample/8) / numChannels);

        // Read samples
        int numSamples = dataSize / (bitsPerSample / 8);
        float[] samples = new float[numSamples];

        byte[] data = new byte[dataSize];
        raf.read(data);
        raf.close();

        ByteBuffer dataBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < numSamples; i++) {
            short sample = dataBuf.getShort();
            samples[i] = sample / 32768.0f;
        }

        return samples;
    }

    /**
     * Calculate RMS (Root Mean Square) of signal
     */
    static double rms(float[] samples) {
        double sum = 0;
        for (float s : samples) {
            sum += s * s;
        }
        return Math.sqrt(sum / samples.length);
    }

    /**
     * Calculate max absolute value
     */
    static double maxAbs(float[] samples) {
        double max = 0;
        for (float s : samples) {
            max = Math.max(max, Math.abs(s));
        }
        return max;
    }

    /**
     * Calculate mean of absolute differences between consecutive samples (measures high-freq content)
     */
    static double meanDelta(float[] samples) {
        if (samples.length < 2) return 0;
        double sum = 0;
        for (int i = 1; i < samples.length; i++) {
            sum += Math.abs(samples[i] - samples[i-1]);
        }
        return sum / (samples.length - 1);
    }

    /**
     * Calculate max delta (measures transients/high-freq content)
     */
    static double maxDelta(float[] samples) {
        if (samples.length < 2) return 0;
        double max = 0;
        for (int i = 1; i < samples.length; i++) {
            max = Math.max(max, Math.abs(samples[i] - samples[i-1]));
        }
        return max;
    }

    /**
     * Remove leading silence (samples below threshold)
     */
    static float[] trimSilence(float[] samples, float threshold) {
        int start = 0;
        for (int i = 0; i < samples.length; i++) {
            if (Math.abs(samples[i]) > threshold) {
                start = i;
                break;
            }
        }
        int end = samples.length;
        for (int i = samples.length - 1; i >= 0; i--) {
            if (Math.abs(samples[i]) > threshold) {
                end = i + 1;
                break;
            }
        }
        float[] trimmed = new float[end - start];
        System.arraycopy(samples, start, trimmed, 0, trimmed.length);
        return trimmed;
    }

    /**
     * Normalize samples to peak = 1.0
     */
    static float[] normalize(float[] samples) {
        double peak = maxAbs(samples);
        if (peak == 0) return samples;
        float[] normalized = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            normalized[i] = (float)(samples[i] / peak);
        }
        return normalized;
    }

    /**
     * Print audio quality metrics
     */
    static void printMetrics(String label, float[] samples) {
        System.out.printf("%n=== %s ===%n", label);
        System.out.printf("  Samples: %d%n", samples.length);
        System.out.printf("  Max amplitude: %.4f%n", maxAbs(samples));
        System.out.printf("  RMS: %.4f%n", rms(samples));
        System.out.printf("  Max delta: %.4f%n", maxDelta(samples));
        System.out.printf("  Mean delta: %.4f%n", meanDelta(samples));
        System.out.printf("  dB level: %.1f dB%n", 20 * Math.log10(maxAbs(samples)));
    }

    /**
     * Per-second RMS comparison to identify when outputs diverge
     */
    @Test
    @DisplayName("Per-second RMS comparison")
    void perSecondComparison() throws Exception {
        String refPath = "tmp/sion_api.wav";
        String spiPath = "tmp/sion_spi.wav";
        if (!Files.exists(Paths.get(refPath)) || !Files.exists(Paths.get(spiPath))) {
            System.out.println("WAV files not found. Run WaveOutTest first.");
            return;
        }

        float[] refSamples = readWavSamples(refPath);
        float[] spiSamples = readWavSamples(spiPath);

        // stereo: 2 samples per frame, 44100 frames per second
        int framesPerSec = 44100;
        int samplesPerSec = framesPerSec * 2;
        int maxSeconds = Math.min(refSamples.length, spiSamples.length) / samplesPerSec;

        System.out.println("\n=== PER-SECOND RMS COMPARISON (raw, not normalized) ===");
        System.out.printf("%-4s  %-10s %-10s %-10s %-10s %-10s %-10s%n",
            "Sec", "API_RMS", "SPI_RMS", "Ratio", "API_Peak", "SPI_Peak", "PeakRatio");

        for (int sec = 0; sec < maxSeconds; sec++) {
            int start = sec * samplesPerSec;
            int end = Math.min(start + samplesPerSec, Math.min(refSamples.length, spiSamples.length));

            double refRmsVal = 0, spiRmsVal = 0;
            double refPeak = 0, spiPeak = 0;
            for (int i = start; i < end; i++) {
                refRmsVal += refSamples[i] * refSamples[i];
                spiRmsVal += spiSamples[i] * spiSamples[i];
                refPeak = Math.max(refPeak, Math.abs(refSamples[i]));
                spiPeak = Math.max(spiPeak, Math.abs(spiSamples[i]));
            }
            int count = end - start;
            refRmsVal = Math.sqrt(refRmsVal / count);
            spiRmsVal = Math.sqrt(spiRmsVal / count);

            double rmsRatio = refRmsVal > 0.0001 ? spiRmsVal / refRmsVal : 0;
            double peakRatio = refPeak > 0.0001 ? spiPeak / refPeak : 0;
            System.out.printf("%-4d  %-10.5f %-10.5f %-10.3f %-10.4f %-10.4f %-10.3f%n",
                sec, refRmsVal, spiRmsVal, rmsRatio, refPeak, spiPeak, peakRatio);
        }
    }

    /**
     * Time-aligned difference using windowed cross-correlation.
     * For each 1-second window, finds the best local alignment,
     * then computes the aligned difference. Creates both a
     * difference WAV file and per-second aligned metrics.
     */
    @Test
    @DisplayName("Create time-aligned difference WAV")
    void createAlignedDifferenceWav() throws Exception {
        String refPath = "tmp/sion_api.wav";
        String spiPath = "tmp/sion_spi.wav";
        if (!Files.exists(Paths.get(refPath)) || !Files.exists(Paths.get(spiPath))) {
            System.out.println("WAV files not found. Run WaveOutTest first.");
            return;
        }

        float[] api = readWavSamples(refPath);
        float[] spi = readWavSamples(spiPath);
        int len = Math.min(api.length, spi.length);

        // Step 1: find global offset using cross-correlation on first loud segment
        // search for first segment with significant audio (after initial silence)
        int searchStart = 44100 * 2 * 3; // start at 3 seconds (skip silence)
        int corrLen = 44100 * 2; // 1 second of stereo data
        int maxShift = 44100; // search ±0.5 seconds (in stereo samples = 1 sec of frames)

        double bestCorr = -1;
        int bestShift = 0;
        for (int shift = -maxShift; shift <= maxShift; shift++) {
            double corr = 0, normA = 0, normB = 0;
            int count = 0;
            for (int i = 0; i < corrLen; i++) {
                int ai = searchStart + i;
                int bi = searchStart + i + shift;
                if (ai >= 0 && ai < api.length && bi >= 0 && bi < spi.length) {
                    corr += api[ai] * spi[bi];
                    normA += api[ai] * api[ai];
                    normB += spi[bi] * spi[bi];
                    count++;
                }
            }
            if (normA > 0 && normB > 0) {
                corr /= Math.sqrt(normA * normB);
                if (corr > bestCorr) {
                    bestCorr = corr;
                    bestShift = shift;
                }
            }
        }
        System.out.printf("Best global alignment: shift SPI by %d samples (%.1f ms), correlation: %.4f%n",
            bestShift, bestShift / 88.2, bestCorr);

        // Step 2: create aligned difference
        float[] diff = new float[len];
        int validLen = 0;
        for (int i = 0; i < len; i++) {
            int si = i + bestShift;
            if (si >= 0 && si < spi.length) {
                diff[i] = api[i] - spi[si];
                validLen = i + 1;
            }
        }

        // compute aligned metrics
        double diffRms = 0, sigRms = 0;
        for (int i = 0; i < validLen; i++) {
            diffRms += diff[i] * diff[i];
            sigRms += api[i] * api[i];
        }
        diffRms = Math.sqrt(diffRms / validLen);
        sigRms = Math.sqrt(sigRms / validLen);
        System.out.printf("Aligned: Signal RMS: %.5f, Diff RMS: %.5f, Ratio: %.4f (%.1f dB)%n",
            sigRms, diffRms, diffRms / sigRms, 20 * Math.log10(diffRms / sigRms));

        // Step 3: write aligned difference WAV (amplified)
        double diffPeak = maxAbs(diff);
        float gain = (float) (0.9 / Math.max(diffPeak, 0.0001));
        System.out.printf("Diff peak: %.5f, gain: %.1fx%n", diffPeak, gain);

        byte[] pcm = new byte[validLen * 2];
        for (int i = 0; i < validLen; i++) {
            short s = (short) Math.max(-32767, Math.min(32767, diff[i] * gain * 32767));
            pcm[i * 2] = (byte) (s & 0xff);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(44100, 16, 2, true, false);
        javax.sound.sampled.AudioInputStream ais = new javax.sound.sampled.AudioInputStream(
            new java.io.ByteArrayInputStream(pcm), fmt, validLen / 2);
        java.io.File outFile = new java.io.File("tmp/sion_diff_aligned.wav");
        javax.sound.sampled.AudioSystem.write(ais, javax.sound.sampled.AudioFileFormat.Type.WAVE, outFile);
        System.out.println("Aligned difference WAV: " + outFile.getAbsolutePath());

        // Step 4: per-second breakdown with local re-alignment
        int framesPerSec = 44100;
        int samplesPerSec = framesPerSec * 2;
        int maxSeconds = validLen / samplesPerSec;
        int localMaxShift = 4410; // ±50ms local re-alignment (in stereo samples)

        System.out.printf("%n%-4s  %-8s  %-10s %-10s %-10s %-8s%n",
            "Sec", "Shift", "DiffRMS", "SigRMS", "Ratio(dB)", "Corr");
        for (int sec = 0; sec < maxSeconds; sec++) {
            int wStart = sec * samplesPerSec;
            int wLen = Math.min(samplesPerSec, validLen - wStart);

            // local cross-correlation to find best alignment for this second
            double bCorr = -1;
            int bShift = 0;
            for (int sh = -localMaxShift; sh <= localMaxShift; sh++) {
                double c = 0, nA = 0, nB = 0;
                for (int i = 0; i < wLen; i++) {
                    int ai = wStart + i;
                    int bi = wStart + i + bestShift + sh;
                    if (ai < api.length && bi >= 0 && bi < spi.length) {
                        c += api[ai] * spi[bi];
                        nA += api[ai] * api[ai];
                        nB += spi[bi] * spi[bi];
                    }
                }
                if (nA > 0 && nB > 0) {
                    c /= Math.sqrt(nA * nB);
                    if (c > bCorr) { bCorr = c; bShift = sh; }
                }
            }

            // compute aligned difference for this second
            double dr = 0, sr = 0;
            for (int i = 0; i < wLen; i++) {
                int ai = wStart + i;
                int bi = wStart + i + bestShift + bShift;
                if (ai < api.length && bi >= 0 && bi < spi.length) {
                    double d = api[ai] - spi[bi];
                    dr += d * d;
                    sr += api[ai] * api[ai];
                }
            }
            dr = Math.sqrt(dr / wLen);
            sr = Math.sqrt(sr / wLen);
            double ratio = sr > 0.0001 ? 20 * Math.log10(dr / sr) : -999;
            System.out.printf("%-4d  %-8d  %-10.5f %-10.5f %-10.1f %-8.4f%n",
                sec, bShift, dr, sr, ratio, bCorr);
        }
    }

    @Test
    @DisplayName("Compare SPI output quality with original API reference")
    void compareAudioQuality() throws Exception {
        // Check if reference file exists
        String refPath = "tmp/sion_api.wav";
        if (!Files.exists(Paths.get(refPath))) {
            System.out.println("Reference file not found: " + refPath);
            System.out.println("Please create reference audio using original API");
            return;
        }

        // Read and analyze reference
        float[] refSamples = readWavSamples(refPath);
        float[] refTrimmed = trimSilence(refSamples, 0.001f);
        float[] refNorm = normalize(refTrimmed);
        printMetrics("REFERENCE (API)", refNorm);

        // Read SPI output
        String spiPath = "tmp/sion_spi.wav";
        if (!Files.exists(Paths.get(spiPath))) {
            System.out.println("SPI output not found: " + spiPath);
            fail("SPI did not produce output");
            return;
        }

        float[] javaSamples = readWavSamples(spiPath);
        float[] javaTrimmed = trimSilence(javaSamples, 0.001f);
        float[] javaNorm = normalize(javaTrimmed);
        printMetrics("SPI OUTPUT", javaNorm);

        // Compare metrics
        System.out.println("\n=== QUALITY COMPARISON ===");

        double refMaxDelta = maxDelta(refNorm);
        double javaMaxDelta = maxDelta(javaNorm);
        double deltaRatio = javaMaxDelta / refMaxDelta;
        System.out.printf("Max Delta Ratio (Java/Ref): %.2f (should be ~1.0)%n", deltaRatio);

        double refMeanDelta = meanDelta(refNorm);
        double javaMeanDelta = meanDelta(javaNorm);
        double meanDeltaRatio = javaMeanDelta / refMeanDelta;
        System.out.printf("Mean Delta Ratio (Java/Ref): %.2f (should be ~1.0)%n", meanDeltaRatio);

        double refRms = rms(refNorm);
        double javaRms = rms(javaNorm);
        double rmsRatio = javaRms / refRms;
        System.out.printf("RMS Ratio (Java/Ref): %.2f (should be ~1.0)%n", rmsRatio);

        // Quality thresholds
        System.out.println("\n=== QUALITY ASSESSMENT ===");

        boolean deltaPassed = deltaRatio > 0.5 && deltaRatio < 2.0;
        System.out.printf("Max Delta Check: %s (%.2f, expect 0.5-2.0)%n",
            deltaPassed ? "PASS" : "FAIL", deltaRatio);

        boolean meanDeltaPassed = meanDeltaRatio > 0.3 && meanDeltaRatio < 3.0;
        System.out.printf("Mean Delta Check: %s (%.2f, expect 0.3-3.0)%n",
            meanDeltaPassed ? "PASS" : "FAIL", meanDeltaRatio);

        boolean rmsPassed = rmsRatio > 0.5 && rmsRatio < 2.0;
        System.out.printf("RMS Check: %s (%.2f, expect 0.5-2.0)%n",
            rmsPassed ? "PASS" : "FAIL", rmsRatio);

        // Overall verdict
        boolean passed = deltaPassed && meanDeltaPassed && rmsPassed;
        System.out.printf("%nOVERALL: %s%n", passed ? "AUDIO QUALITY OK" : "AUDIO QUALITY DEGRADED");

        if (!passed) {
            System.out.println("\nDiagnosis:");
            if (deltaRatio < 0.5) {
                System.out.println("- Low max delta suggests missing high-frequency transients");
            }
            if (meanDeltaRatio < 0.3) {
                System.out.println("- Low mean delta suggests overall lack of high-frequency content");
            }
            if (rmsRatio < 0.5 || rmsRatio > 2.0) {
                System.out.println("- RMS mismatch suggests amplitude/volume problem");
            }
        }
    }
}
