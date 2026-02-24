package samples.SiONTenorion;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.events.SiONTrackEvent;
import org.si.sion.utils.SiONPresetVoice;


// SiON TENORION
public class Tenorion extends JFrame {

    // driver
    public SiONDriver driver = new SiONDriver(2048, 2, 44100, 0);

    // preset voice
    public SiONPresetVoice presetVoice = new SiONPresetVoice();

    // voices, notes and tracks
    public SiONVoice[] voices = new SiONVoice[16];
    public int[] notes = {36,48,60,72, 43,48,55,60, 65,67,70,72, 77,79,82,84};
    public int[] length = { 1, 1, 1, 1,  1, 1, 1, 1,  4, 4, 4, 4,  4, 4, 4, 4};

    // beat counter
    public int beatCounter;

    // control pad
    public MatrixPad matrixPad;

    // constructor
    public Tenorion() {
        super("SiON TENORION");
        int i;

        // set voices from preset
        SiONPresetVoice.SiONVoiceList percusVoices = (SiONPresetVoice.SiONVoiceList) presetVoice.get("valsound.percus");
        voices[0] = percusVoices.get(0);  // bass drum
        voices[1] = percusVoices.get(27); // snare drum
        voices[2] = percusVoices.get(16); // close hihat
        voices[3] = percusVoices.get(22); // open hihat
        for (i=4; i<8; i++) voices[i] = (SiONVoice) presetVoice.get("valsound.bass18"); // others
        for (i=8; i<16; i++) voices[i] = (SiONVoice) presetVoice.get("sine"); // fill the rest if missing

        // listen
        driver.setBeatCallbackInterval(1);
        driver.addEventListener(SiONTrackEvent.BEAT, this::_onBeat);
        driver.setTimerInterruption(1, this::_onTimerInterruption);

        // control pad
        matrixPad = new MatrixPad();
        add(matrixPad);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        // start streaming
        beatCounter = 0;
        driver.play(null, true); startAudioThread();
    }

    // _onBeat (SiONTrackEvent.BEAT) is called back in each beat at the sound timing.
    private void _onBeat(org.si.utils.Event e) {
        SiONTrackEvent te = (SiONTrackEvent) e;
        int beatId = te.getEventTriggerID() & 15; javax.swing.SwingUtilities.invokeLater(() -> matrixPad.beat(beatId));    }

    // _onTimerInterruption (SiONDriver.setTimerInterruption) is called back in each beat at the buffering timing.
    private void _onTimerInterruption() {
        int beatIndex = beatCounter & 15;
        for (int i=0; i<16; i++) {
            if ((matrixPad.sequences[i] & (1<<beatIndex)) != 0) {
                driver.noteOn(notes[i], voices[i], length[i], 0, 0, 0, true);
            }
        }
        beatCounter++;
    }

    private void startAudioThread() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, driver.getBufferLength() * 4);
                line.start();

                int bufferSize = driver.getBufferLength();
                byte[] out = new byte[bufferSize * 4];

                while (true) {
                    driver.module._beginProcess();
                    driver.effector._beginProcess();
                    driver.sequencer._process();
                    driver.effector._endProcess();
                    driver.module._endProcess();

                    double[] output = driver.module.getOutput();
                    for (int i = 0; i < output.length; i++) {
                        short s = (short) (output[i] * 32767);
                        out[i * 2] = (byte) (s & 0xff);
                        out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
                    }
                    line.write(out, 0, out.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    public static void main(String[] args) {
        new Tenorion();
    }

    static class MatrixPad extends JPanel {
        public int[] sequences = new int[16];
        private BufferedImage buffer = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        private BufferedImage display = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        private int[] padOn;
        private int[] padOff;

        public MatrixPad() {
            setPreferredSize(new Dimension(320, 320));
            padOn  = _createPad(0x303050, 0x6060a0);
            padOff = _createPad(0x303050, 0x202040);

            for (int i=0; i<256; i++) {
                int x = (i & 15) * 20;
                int y = (i >> 4) * 20;
                _copyPad(buffer, padOff, x, y);
                _copyPad(display, padOff, x, y);
            }
            for (int i=0; i<16; i++) sequences[i] = 0;

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    _onClick(e);
                }
            });

            Timer timer = new Timer(33, e -> _onEnterFrame());
            timer.start();
        }

        private int[] _createPad(int border, int face) {
            int[] pix = new int[20 * 20];
            for (int y=0; y<20; y++) {
                for (int x=0; x<20; x++) {
                    if (x==0 || x==19 || y==0 || y==19) pix[y*20+x] = border | 0xFF000000;
                    else if (x>=1 && x<=17 && y>=1 && y<=17) pix[y*20+x] = face | 0xFF000000;
                    else pix[y*20+x] = 0xFF000000;
                }
            }
            return pix;
        }

        private void _copyPad(BufferedImage img, int[] pad, int dx, int dy) {
            img.setRGB(dx, dy, 20, 20, pad, 0, 20);
        }

        private void _onEnterFrame() {
            Graphics2D g = display.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g.drawImage(buffer, 0, 0, null);
            g.dispose();
            repaint();
        }

        private void _onClick(MouseEvent e) {
            int mx = e.getX();
            int my = e.getY();
            if (mx>=0 && mx<320 && my>=0 && my<320) {
                int track = 15 - (my / 20);
                int beat = mx / 20;
                sequences[track] ^= (1 << beat);
                int dx = beat * 20;
                int dy = (15 - track) * 20;
                if ((sequences[track] & (1 << beat)) != 0) _copyPad(buffer, padOn, dx, dy);
                else _copyPad(buffer, padOff, dx, dy);
            }
        }

        public void beat(int beat16th) {
            int dx = beat16th * 20;
            for (int dy=0; dy<320; dy+=20) {
                _copyPad(display, padOn, dx, dy);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(display, 0, 0, null);
        }
    }
}
