/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.sion;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.si.sion.SiONVoice;
import org.si.sion.midi.MIDIModule;
import vavi.sound.mobile.MobileExclusive;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.midi.VaviMidiDeviceProvider.MANUFACTURER_ID;
import static vavi.sound.mobile.MobileExclusive.MIDI_SYSEX_FUNCTION_ID_PACKED;


/**
 * The voices and waves an MFi or SMAF file sends, as SiON ones.
 * <p>
 * The exclusives are the MA-3 / MA-5 ones, which {@code vavi-sound} sends for an MFi file
 * too, see {@code vavi.sound.mfi.vavi.sequencer.YamahaMfiExclusive}:
 * </p>
 * <ul>
 *  <li>an FM voice becomes a {@code #MA@} voice of SiON, the FM module of which has the MA-3
 *      algorithms and wave shapes, see {@link org.si.sion.utils.Translator#setMA3Param}</li>
 *  <li>a wave table (WT) voice, its wave, and a stream wave are played by the PCM channel of
 *      SiON, see {@link SionWaves}</li>
 * </ul>
 * <p>
 * {@link #process} runs where a message arrives and does what is heavy there, the
 * {@link Runnable} it returns is for the audio thread, where the midi module is touched.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-18 nsano initial version <br>
 * @see "vavi.sound.midi.ymf262.YamahaVoices of vavi-apps-mfiplayer"
 */
class SionYamahaVoices {

    private static final Logger logger = getLogger(SionYamahaVoices.class.getName());

    /** the bank select MSB of a SMAF melody channel */
    static final int MELODY_BANK = 0x7c;

    /** the bank select MSB of a SMAF drum channel */
    static final int DRUM_BANK = 0x7d;

    /** YAMAHA */
    private static final int YAMAHA = 0x43;

    /** the voice type of a {@code 43 79 0x 7f 01} exclusive */
    private static final int VOICE_TYPE_FM = 0;

    /** the voice type of a {@code 43 79 0x 7f 01} exclusive */
    private static final int VOICE_TYPE_PCM = 1;

    private final MIDIModule midiModule;

    private final SionWaves waves;

    /** the melody voices of a smaf bank by (bank LSB, program), a voice set has no bank */
    private final Map<Integer, SiONVoice> smafMelodies = new ConcurrentHashMap<>();

    /** a WT voice which came before its wave */
    private record PendingVoice(int bankMSB, int bankLSB, int program, int drumNote, byte[] image) {}

    /** the WT voices waiting for their waves, touched where messages arrive only */
    private final List<PendingVoice> pendingVoices = new ArrayList<>();

    SionYamahaVoices(MIDIModule midiModule, SionWaves waves) {
        this.midiModule = midiModule;
        this.waves = waves;
    }

    /**
     * Takes an exclusive, when it is one of a voice or a wave.
     * <pre>
     *  45 7f &lt;encode87(43 ... f7)&gt; f7          a smaf exclusive, packed
     *  45 7f &lt;encode87(45 xx 1x ... f7)&gt; f7    a stream, see {@link MobileExclusive}
     *  43 79 06 7f ...                        an MA-3 exclusive as it is
     * </pre>
     *
     * @param data the exclusive, without its leading {@code 0xf0}
     * @return what the audio thread does, null when it is none of the above
     */
    Runnable process(byte[] data) {
        if (data.length < 2) {
            return null;
        }
        if ((data[0] & 0xff) == MANUFACTURER_ID && (data[1] & 0xff) == MIDI_SYSEX_FUNCTION_ID_PACKED) {
            byte[] sysex = MobileExclusive.unpack(data);
logger.log(Level.TRACE, "smaf sysex:\n%s".formatted(StringUtil.getDump(sysex, 32)));
            return processSmafExclusive(sysex);
        }
        if ((data[0] & 0xff) == YAMAHA && data.length >= 6 &&
                (data[1] & 0xff) == 0x79 && (data[2] & 0xff) == 0x06 && (data[3] & 0xff) == 0x7f) {
            // an MA-3 exclusive is 7 bit safe, a midi file may carry it as it is
            return processSmafExclusive(data);
        }
        return null;
    }

    /**
     * An 8 bit smaf exclusive, or a vavi one of {@link MobileExclusive}.
     *
     * @param sysex 0: manufacturer id ... last: 0xf7
     * @return what the audio thread does, a no-op one when it is not handled
     */
    private Runnable processSmafExclusive(byte[] sysex) {
        Runnable r = null;
        if (sysex.length >= 3) {
            if ((sysex[0] & 0xff) == MANUFACTURER_ID) {
                r = processMobileExclusive(sysex);
            } else if ((sysex[0] & 0xff) == YAMAHA) {
                r = processYamahaExclusive(sysex);
            }
        }
        return r != null ? r : () -> {};
    }

    /**
     * The exclusives a stream wave, its start and stop travel as.
     * <pre>
     *  45 ff 10 id fm ch bt sh sl &lt;data&gt; f7   a stream wave
     *  45 ff 11 id vv ch [g2 g1 g0] f7       start a stream
     *  45 ff 12 id f7                        stop a stream
     *  45 ff 13 ch vv f7                     volume of an audio channel
     *  45 ff 14 ch pp f7                     panpot of an audio channel
     *     ~~
     *     +--- the mfi / smaf function id the exclusive came under
     * </pre>
     * An MFi machine dependent message ({@code 45 01 ...}) is none of these and is not taken.
     */
    private Runnable processMobileExclusive(byte[] sysex) {
        return switch (sysex[2] & 0xff) {
            case MobileExclusive.WAVE -> sysex.length < 11 ? null :
                    waves.setStream(sysex[3] & 0x7f, sysex[4] & 0xff, sysex[5] & 0xff, sysex[6] & 0xff,
                            ((sysex[7] & 0xff) << 8) | (sysex[8] & 0xff), Arrays.copyOfRange(sysex, 9, sysex.length - 1));
            case MobileExclusive.ON -> sysex.length < 7 ? null :
                    () -> waves.streamOn(sysex[3] & 0x7f, sysex[4] & 0x7f, sysex[5] & 0x7f);
            case MobileExclusive.OFF -> sysex.length < 5 ? null :
                    () -> waves.streamOff(sysex[3] & 0x7f);
            case MobileExclusive.VOLUME -> sysex.length < 6 ? null :
                    () -> waves.setAudioVolume(sysex[3] & 0x7f, sysex[4] & 0x7f);
            case MobileExclusive.PANPOT -> sysex.length < 6 ? null :
                    () -> waves.setAudioPanpot(sysex[3] & 0x7f, sysex[4] & 0x7f);
            default -> {
logger.log(Level.DEBUG, "vavi exclusive %02x %02x unhandled".formatted(sysex[1] & 0xff, sysex[2] & 0xff));
                yield null;
            }
        };
    }

    /**
     * <pre>
     *  43 79 06 7f 01 mm ll pc dn vt &lt;7 bit encoded voice&gt; f7   MA-3 voice
     *  43 79 07 7f 01 mm ll pc dn vt &lt;voice&gt; f7                 MA-5 voice
     *  43 79 0x 7f nn ...                                      MA-3 / MA-5 others, see {@link #processMa35Message}
     *  43 05 00 ii &lt;4 bit adpcm&gt; f7                            a wave of a WT voice (EXWV)
     *  43 05 02 bb pp &lt;16 byte VM35 PCM voice&gt; f7               a WT voice (EXVO)
     *             mm: bank MSB, ll: bank LSB, pc: program, dn: drum note, vt: voice type
     * </pre>
     * A VMA (MA-1 / MA-2) voice, {@code 43 03 ...}, is not taken.
     */
    private Runnable processYamahaExclusive(byte[] sysex) {
        int id = sysex[1] & 0xff;
        if (id == 0x79 && sysex.length >= 6 && (sysex[3] & 0xff) == 0x7f) {
            boolean ma3 = (sysex[2] & 0xff) == 0x06;
            if ((sysex[4] & 0xff) != 0x01) {
                return processMa35Message(sysex, ma3);
            }
            if (sysex.length < 11) {
                return null;
            }
            int bankMSB = sysex[5] & 0xff;
            int bankLSB = sysex[6] & 0xff;
            int program = sysex[7] & 0xff;
            int drumNote = sysex[8] & 0xff;
            int voiceType = sysex[9] & 0xff;
            byte[] image = ma3 ? decodeMa3(sysex, 10, sysex.length - 1) : Arrays.copyOfRange(sysex, 10, sysex.length - 1);
            boolean drum = bankMSB == DRUM_BANK || (bankMSB != MELODY_BANK && drumNote != 0);
            return switch (voiceType) {
                case VOICE_TYPE_FM -> {
                    SiONVoice voice = fmVoice(image);
                    if (voice == null) yield null;
logger.log(Level.DEBUG, "voice: FM, bank: %02x/%02x, program: %d, drum note: %d".formatted(bankMSB, bankLSB, program, drumNote));
                    yield () -> setVoice(bankMSB, bankLSB, program, drum ? drumNote : -1, voice);
                }
                case VOICE_TYPE_PCM -> wtVoice(new PendingVoice(bankMSB, bankLSB, program, drum ? drumNote : -1, image));
                default -> {
logger.log(Level.DEBUG, "voice type %02x unhandled".formatted(voiceType));
                    yield null;
                }
            };
        } else if (id == 0x05 && (sysex[2] & 0xff) == 0x00 && sysex.length > 5) {
            return setWave(sysex[3] & 0xff, Arrays.copyOfRange(sysex, 4, sysex.length - 1));
        } else if (id == 0x05 && (sysex[2] & 0xff) == 0x02 && sysex.length >= 22) {
            int bank = sysex[3] & 0xff;
            int program = sysex[4] & 0xff;
            boolean drum = (bank & 0x80) != 0;
            // a drum bank has no note byte, the program is the note
            return wtVoice(new PendingVoice(drum ? DRUM_BANK : MELODY_BANK, bank & 0x7f, program, drum ? program : -1,
                    Arrays.copyOfRange(sysex, 5, sysex.length - 1)));
        }
logger.log(Level.DEBUG, "yamaha exclusive unhandled:\n%s".formatted(StringUtil.getDump(sysex, 16)));
        return null;
    }

    /**
     * The MA-3 / MA-5 messages other than a voice.
     * <pre>
     *  43 79 vv 7f nn ...
     *        ~~    ~~
     *        |     +--- message
     *        +--------- 06: MA-3, 7 bit data, 07: MA-5, 8 bit data
     *
     *  nn
     *  03 id ff &lt;wave&gt;       wave (SetWave), a 4 bit adpcm wave table wave, ff: 00
     *  04 id                 detach wave
     *  08 cl id1 id2         stream pair, cl 00: pair, 01: cancel
     *  0b id pp dd           stream panpot, pp 00: dd, 01: clear, 02: off
     * </pre>
     */
    private Runnable processMa35Message(byte[] sysex, boolean ma3) {
        switch (sysex[4] & 0xff) {
            case 0x03 -> {
                if (sysex.length < 9) break;
                int format = sysex[6] & 0xff;
                if (format != 0) {
logger.log(Level.WARNING, "wave table wave of format %02x not supported, No.%d".formatted(format, sysex[5] & 0x7f));
                    break;
                }
                return setWave(sysex[5] & 0x7f, ma3 ? decodeMa3(sysex, 7, sysex.length - 1) : Arrays.copyOfRange(sysex, 7, sysex.length - 1));
            }
            case 0x04 -> waves.removeWave(sysex[5] & 0x7f);
            case 0x08 -> {
                if (sysex.length < 9) break;
                return () -> waves.setStreamPair(sysex[5] != 0, sysex[6] & 0x7f, sysex[7] & 0x7f);
            }
            case 0x0b -> {
                if (sysex.length < 9) break;
                return () -> waves.setStreamPanpot(sysex[5] & 0x7f, sysex[6] & 0x7f, sysex[7] & 0x7f);
            }
            default -> logger.log(Level.DEBUG, "MA-3/5 message %02x unhandled".formatted(sysex[4] & 0xff));
        }
        return null;
    }

    /**
     * A WT voice, which waits for its wave when it is not here yet.
     *
     * @return what the audio thread does, null when it waits
     */
    private Runnable wtVoice(PendingVoice pending) {
        SiONVoice voice = waves.wtVoice(pending.image, pending.drumNote >= 0);
        if (voice == null) {
            pendingVoices.add(pending);
            return null;
        }
logger.log(Level.DEBUG, "voice: WT, bank: %02x/%02x, program: %d, drum note: %d".formatted(pending.bankMSB, pending.bankLSB, pending.program, pending.drumNote));
        return () -> setVoice(pending.bankMSB, pending.bankLSB, pending.program, pending.drumNote, voice);
    }

    /**
     * A wave of WT voices, and the voices which waited for it.
     *
     * @return what the audio thread does
     */
    private Runnable setWave(int waveId, byte[] adpcm) {
        waves.setWave(waveId, adpcm);
        List<Runnable> installs = new ArrayList<>();
        for (Iterator<PendingVoice> i = pendingVoices.iterator(); i.hasNext(); ) {
            PendingVoice pending = i.next();
            if ((pending.image[15] & 0xff) == waveId) { // not a rom wave, and this one
                i.remove();
                Runnable install = wtVoice(pending);
                if (install != null) {
                    installs.add(install);
                }
            }
        }
        return () -> installs.forEach(Runnable::run);
    }

    /**
     * Sounds a voice as the patch it is for, on the audio thread.
     * <p>
     * A melody voice is the program's whichever bank it is in - the voice set has no bank -
     * and the one of a smaf bank is also kept for {@link #programChange}.
     * </p>
     *
     * @param drumNote -1 for a melody voice
     */
    private void setVoice(int bankMSB, int bankLSB, int program, int drumNote, SiONVoice voice) {
        if (drumNote >= 0) {
            midiModule.drumVoiceSet[drumNote & 0x7f] = voice;
        } else {
            if (bankMSB == MELODY_BANK) {
                smafMelodies.put((bankLSB << 8) | program, voice);
            }
            midiModule.setVoice(program & 0x7f, voice);
        }
    }

    /**
     * A melody channel of a smaf bank sounds the voice of that bank, not of the program only.
     * On the audio thread, before the program change goes to the midi module.
     *
     * @param bankMSB the bank select MSB of the channel
     * @param bankLSB the bank select LSB of the channel
     */
    void programChange(int bankMSB, int bankLSB, int program) {
        if (bankMSB == MELODY_BANK) {
            SiONVoice voice = smafMelodies.get((bankLSB << 8) | program);
            if (voice != null && midiModule.voiceSet[program] != voice) {
                midiModule.setVoice(program, voice);
            }
        }
    }

    /**
     * A VM35 (MA-3 / MA-5) FM voice as a {@code #MA@} one of SiON.
     * <pre>
     *          | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
     * Global+0 |            drumKey            |
     * Global+1 |       panpot      | - |  B O  |
     * Global+2 |  lfo  |pe |   -   |    alg    |
     *
     *      Op+0 |      S R      |xof| - |sus|ksr|
     *      Op+1 |      R R      |      D R      |
     *      Op+2 |      A R      |      S L      |
     *      Op+3 |          T L          |  ksl  |
     *      Op+4 | - |  dam  |eam| - |  dvb  |evb|
     *      Op+5 |     multi     | - |    D T    |
     *      Op+6 |        W S        |    F B    |
     *
     * #MA@ alg[0-15], fb[0-7],
     *      (ws[0-31], ar[0-15], dr[0-15], sr[0-15], rr[0-15], sl[0-15], tl[0-63], ksr[0,1], ksl[0-3], mul[0-15], dt1[0-7], ams[0-3]) x operators
     * </pre>
     * <p>
     * The algorithm numbers are the same, 0 and 1 being 2 operators and 2 ~ 7 being 4. The
     * feedback is the first operator's, the only one SiON has. The vibrato and XOF, SUS of
     * an operator, the panpot and the basic octave of a voice are not taken.
     * </p>
     *
     * @param image the VM5 voice image, the 7 bit encoding of MA-3 undone
     * @return null when it does not read
     * @see "vavi.sound.yamaha.smaf.voice.VM35FMVoice of vavi-sound-ma"
     */
    static SiONVoice fmVoice(byte[] image) {
        if (image.length < 3) {
            return null;
        }
        int drumKey = image[0] & 0x7f;
        int alg = image[2] & 0x07;
        int operators = alg < 2 ? 2 : 4;
        if (image.length < 3 + 7 * operators) {
logger.log(Level.WARNING, "FM voice is too short: %d bytes for %d operators".formatted(image.length, operators));
            return null;
        }
        Object[] params = new Object[2 + 12 * operators];
        params[0] = alg;
        params[1] = image[3 + 6] & 0x07; // fb of the first operator
        for (int op = 0; op < operators; op++) {
            int p = 3 + 7 * op;
            int q = 2 + 12 * op;
            boolean eam = (image[p + 4] & 0x10) != 0;
            params[q] = (image[p + 6] & 0xff) >> 3;       // ws
            params[q + 1] = (image[p + 2] & 0xff) >> 4;   // ar
            params[q + 2] = image[p + 1] & 0x0f;          // dr
            params[q + 3] = (image[p] & 0xff) >> 4;       // sr
            params[q + 4] = (image[p + 1] & 0xff) >> 4;   // rr
            params[q + 5] = image[p + 2] & 0x0f;          // sl
            params[q + 6] = (image[p + 3] & 0xff) >> 2;   // tl
            params[q + 7] = image[p] & 0x01;              // ksr
            params[q + 8] = image[p + 3] & 0x03;          // ksl
            params[q + 9] = (image[p + 5] & 0xff) >> 4;   // mul
            params[q + 10] = image[p + 5] & 0x07;         // dt
            params[q + 11] = eam ? (image[p + 4] >> 5) & 0x03 : 0; // ams
        }
        try {
            SiONVoice voice = new SiONVoice();
            voice.setParamMA3(params);
            voice.preferableNote = drumKey;
            return voice;
        } catch (IllegalArgumentException e) {
logger.log(Level.WARNING, "FM voice not taken: " + e.getMessage());
            return null;
        }
    }

    /**
     * The 7 bit encoding of the MA-3 exclusives: a flag byte holds the 8th bits of the up to
     * 7 bytes after it, the first one in bit 6.
     *
     * @param from inclusive
     * @param to exclusive
     * @see "Decode_7bitData of mammfcnv.c"
     */
    static byte[] decodeMa3(byte[] data, int from, int to) {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        for (int i = from; i < to; i += 8) {
            int flags = data[i] & 0xff;
            for (int j = 1; j < 8 && i + j < to; j++) {
                decoded.write((((flags >> (7 - j)) & 0x01) << 7) | (data[i + j] & 0x7f));
            }
        }
        return decoded.toByteArray();
    }
}
