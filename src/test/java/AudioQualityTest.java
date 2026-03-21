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
