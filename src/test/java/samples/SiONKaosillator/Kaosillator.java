package samples.SiONKaosillator;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.si.sion.SiONData;
import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.events.SiONEvent;
import org.si.sion.events.SiONTrackEvent;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.utils.Scale;
import org.si.sion.utils.SiONPresetVoice;
import org.si.sound.Arpeggiator;


// SiON KAOSCILLATOR
public class Kaosillator extends JFrame {

    // driver
    public SiONDriver driver = new SiONDriver(2048, 2, 44100, 0);

    // preset voice
    public SiONPresetVoice presetVoice = new SiONPresetVoice();

    // MML data
    public SiONData rythmLoop;

    // control pad
    public ControlPad controlPad;

    // text (using JLabel in Java)
    public JLabel startPortamentHere = new JLabel();

    // arpeggiator
    public Arpeggiator arpeggiator;

    // constructor
    public Kaosillator() {
        super("SiON KAOSCILLATOR");
        
        // compile mml.
        String mml = "t132;";
        mml += "%6@0o3l8$c2cc.c.; %6@1o3$rcrc; %6@2v8l16$[crccrrcc]; %6@3v8o3$[rc8r8];";
        mml += "%6@4v8l16o3$aa<a8>a<ga>ararara<e8>;";
        rythmLoop = driver.compile(mml, null);

        // set voices of "%6@0-4" from preset
        SiONPresetVoice.SiONVoiceList percusVoices = (SiONPresetVoice.SiONVoiceList) presetVoice.get("valsound.percus");
        rythmLoop.setVoice(0, percusVoices.get(0));  // bass drum
        rythmLoop.setVoice(1, percusVoices.get(27)); // snare drum
        rythmLoop.setVoice(2, percusVoices.get(16)); // close hihat
        rythmLoop.setVoice(3, percusVoices.get(21)); // open hihat
        rythmLoop.setVoice(4, (SiONVoice) presetVoice.get("valsound.bass3")); // bass

        // listen click
        driver.addEventListener(SiONEvent.STREAM,    this::_onStream);
        driver.addEventListener(SiONTrackEvent.BEAT, this::_onBeat);

        // arpeggiator setting
        arpeggiator = new Arpeggiator(new Scale("o1Ajap", 5), 1, new double[]{0,1,2,5,4,3});
        arpeggiator.setVoice((SiONVoice) presetVoice.get("valsound.lead32"));
        arpeggiator.setQuantize(4);
        arpeggiator.setVolume(0.3);
        arpeggiator.setNoteQuantize(8);

        // control pad
        controlPad = new ControlPad(320, 320, 0.5, 0.5, 0x101030);
        
        // Layout
        setLayout(null);
        controlPad.setBounds(0, 0, 352, 352); // width+32, height+32
        add(controlPad);

        startPortamentHere.setText("<html><font color='#808080'>Start Portament Here</font></html>");
        startPortamentHere.setBounds(24, 24, 320, 20);
        controlPad.add(startPortamentHere);

        controlPad.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                _onMouseDown(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                _onMouseUp(e);
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(352, 380);
        setVisible(true);

        // play with an argument of resetEffector = false.
        driver.play(rythmLoop, false); startAudioThread();
    }

    private void _onMouseDown(MouseEvent e) {
        // set portament if mouseY < 40
        if (e.getY() < 40) arpeggiator.setPortament(4);
        else arpeggiator.setPortament(0);

        // set pitch
        arpeggiator.setScaleIndex((int)(controlPad.controlX * 32));

        // start arpeggio
        arpeggiator.play();

        // update setup
        if (arpeggiator.getTrack() != null && arpeggiator.getTrack().channel != null) {
            arpeggiator.getTrack().channel.setFilterResonance(3);
            arpeggiator.getTrack().channel.activateFilter(true);
        }
    }

    private void _onMouseUp(MouseEvent e) {
        // stop arpeggio
        arpeggiator.stop();
    }

    private void _onStream(org.si.utils.Event e) {
        _updateArpeggiatorFromPad();
    }

    private void _onBeat(org.si.utils.Event e) {
        javax.swing.SwingUtilities.invokeLater(() -> controlPad.beat(32));
    }

    private void startAudioThread() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, driver.getBufferLength() * 16);
                line.start();

                int bufferSize = driver.getBufferLength();
                byte[] out = new byte[bufferSize * 4];

                while (true) {
                    _updateArpeggiatorFromPad();
                    driver.module._beginProcess();
                    driver.effector._beginProcess();
                    driver.sequencer._process();
                    driver.effector._endProcess();
                    driver.module._endProcess();

                    double[] output = driver.module.getOutput();
                    for (int i = 0; i < output.length; i++) {
                        int v = (int)(output[i] * 32767);
                        if (v > 32767) v = 32767;
                        if (v < -32768) v = -32768;
                        short s = (short) v;
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

    private void _updateArpeggiatorFromPad() {
        arpeggiator.setScaleIndex((int)(controlPad.controlX * 32));
        SiMMLTrack track = arpeggiator.getTrack();
        if (track != null && track.channel != null) {
            int cutoff = (int)((controlPad.controlY - 0.1) * 192);
            if (cutoff > 128) cutoff = 128;
            else if (cutoff < 16) cutoff = 16;
            track.channel.setFilterOffset(cutoff);
        }
    }
    public static void main(String[] args) {
        new Kaosillator();
    }

    static class ControlPad extends JPanel {
        public volatile double controlX;
        public volatile double controlY;
        public volatile boolean isDragging;
        public int color;

        private BufferedImage buffer;
        private double ratX, ratY;
        private double prevX, prevY;
        private double pointerSize = 8;
        private int width, height;

        public ControlPad(int width, int height, double initialX, double initialY, int color) {
            this.width = width;
            this.height = height;
            this.color = color;
            this.controlX = initialX;
            this.controlY = initialY;
            this.ratX = 1.0 / width;
            this.ratY = 1.0 / height;
            
            buffer = new BufferedImage(width + 32, height + 32, BufferedImage.TYPE_INT_RGB);
            prevX = buffer.getWidth() * controlX;
            prevY = buffer.getHeight() * (1 - controlY);

            setBackground(Color.BLACK);
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    _onMouseMove(e);
                }
                @Override
                public void mouseMoved(MouseEvent e) {
                    _onMouseMove(e);
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    isDragging = true;
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    isDragging = false;
                }
            });

            Timer timer = new Timer(33, e_ -> _onEnterFrame());
            timer.start();
        }

        private void _onEnterFrame() {
            int x = (int)((buffer.getWidth() - 32) * controlX + 16);
            int y = (int)((buffer.getHeight() - 32) * (1 - controlY) + 16);
            
            Graphics2D g = buffer.createGraphics();
            // Simple blur effect simulation: darken the existing buffer
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            
            // Draw the line
            g.setStroke(new BasicStroke((float) pointerSize));
            g.setColor(new Color(color | 0xFF000000));
            g.drawLine((int)prevX, (int)prevY, x, y);
            g.dispose();
            
            prevX = x + Math.random();
            prevY = y;
            pointerSize *= 0.96;
            repaint();
        }

        private void _onMouseMove(MouseEvent e) {
            if (isDragging) {
                controlX = (e.getX() - 16) * ratX;
                controlY = 1 - (e.getY() - 16) * ratY;
                if (controlX < 0) controlX = 0;
                else if (controlX > 1) controlX = 1;
                if (controlY < 0) controlY = 0;
                else if (controlY > 1) controlY = 1;
            }
        }

        public void beat(int size) {
            pointerSize = size;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(buffer, 0, 0, null);
            g.setColor(Color.WHITE);
            g.drawRect(16, 16, width, height);
        }
    }
}
