package samples.SiONKeyboard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.si.sion.SiONDriver;
import org.si.sion.SiONVoice;
import org.si.sion.effector.SiEffectStereoChorus;
import org.si.sion.effector.SiEffectStereoDelay;
import org.si.sion.events.SiONTrackEvent;
import org.si.sion.sequencer.SiMMLTrack;
import org.si.sion.utils.SiONPresetVoice;


// SiON Keyboard Sample
public class Keyboard extends JFrame {

    private SiONDriver driver = new SiONDriver(2048, 2, 44100, 0);
    private SiONPresetVoice presetVoice = new SiONPresetVoice();
    private String keyboardKeys = "zsxdcvgbhnjm,l.;/";
    private int[] keyboardCodeTable = new int[17];
    private KeyboardPanel keyboardPanel;
    
    private SiONVoice currentVoice;
    private int keyboardFlag = 0;
    private int octave = 5;

    private double delaySendLevel = 0.25;
    private double chorusSendLevel = 0;

    private JComboBox<String> categSelect;
    private JComboBox<String> voiceSelect;
    private JSlider delaySend;
    private JSlider chorusSend;

    public Keyboard() {
        super("Simple Keyboard");
        
        // initialize parameters
        for (int i = 0; i < 17; i++) keyboardCodeTable[i] = keyboardKeys.charAt(i);
        currentVoice = (SiONVoice) presetVoice.get("sine");

        // UI components
        categSelect = new JComboBox<>();
        for (SiONPresetVoice.SiONVoiceList categ : presetVoice.categories) {
            categSelect.addItem(categ.name);
        }
        categSelect.addActionListener(e -> _onFileSelectorChange());

        voiceSelect = new JComboBox<>();
        voiceSelect.addActionListener(e -> _onChannelSelectorChange());

        delaySend = new JSlider(0, 100, 25);
        delaySend.addChangeListener(e -> changeEffectSend());
        
        chorusSend = new JSlider(0, 100, 0);
        chorusSend.addChangeListener(e -> changeEffectSend());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(4, 2));
        controlPanel.add(new JLabel("Category:"));
        controlPanel.add(categSelect);
        controlPanel.add(new JLabel("Voice:"));
        controlPanel.add(voiceSelect);
        controlPanel.add(new JLabel("Delay:"));
        controlPanel.add(delaySend);
        controlPanel.add(new JLabel("Chorus:"));
        controlPanel.add(chorusSend);

        keyboardPanel = new KeyboardPanel();
        
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(keyboardPanel, BorderLayout.CENTER);

        // effector setting
        SiEffectStereoDelay dly = new SiEffectStereoDelay(200, 0.2, false, 0.25);
        dly.initialize();
        
        SiEffectStereoChorus cho = new SiEffectStereoChorus(20, 0.2, 4, 20, 0.5, true);
        cho.initialize();
        
        driver.effector.initialize();
        driver.effector.connect(1, dly);
        driver.effector.connect(2, cho);

        // event listeners
        driver.addEventListener(SiONTrackEvent.NOTE_ON_FRAME, this::_onNoteOnFrame);
        driver.addEventListener(SiONTrackEvent.NOTE_OFF_FRAME, this::_onNoteOffFrame);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                _onKeyDown(e);
            }
            @Override
            public void keyReleased(KeyEvent e) {
                _onKeyUp(e);
            }
        });

        // initial voice setup
        _onFileSelectorChange();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(400, 450);
        setVisible(true);

        // start stream
        driver.play(null, false); startAudioThread();
        
        // request focus for key listener
        requestFocus();
    }

    private void _onFileSelectorChange() {
        int index = categSelect.getSelectedIndex();
        if (index < 0) return;
        _refleshChannelSelector(presetVoice.categories.get(index));
        _onChannelSelectorChange();
    }

    private void _refleshChannelSelector(List<SiONVoice> categolyList) {
        voiceSelect.removeAllItems();
        for (int i = 0; i < categolyList.size(); i++) {
            voiceSelect.addItem((i + 1) + ": " + categolyList.get(i).name);
        }
        voiceSelect.setSelectedIndex(0);
    }

    private void _onChannelSelectorChange() {
        int categIndex = categSelect.getSelectedIndex();
        int voiceIndex = voiceSelect.getSelectedIndex();
        if (categIndex < 0 || voiceIndex < 0) return;
        currentVoice = presetVoice.categories.get(categIndex).get(voiceIndex);
    }

    private void changeEffectSend() {
        delaySendLevel = delaySend.getValue() * 0.01;
        chorusSendLevel = chorusSend.getValue() * 0.01;
        for (SiMMLTrack trk : driver.sequencer.tracks) {
            if (trk != null && trk.channel != null) {
                trk.channel.setStreamSend(1, delaySendLevel);
                trk.channel.setStreamSend(2, chorusSendLevel);
            }
        }
    }

    private void _onNoteOnFrame(org.si.utils.Event e) {
        SiONTrackEvent te = (SiONTrackEvent) e;
        int key = te.getNote() - octave * 12;
        while (key < 0) key += 12;
        while (key > 16) key -= 12;
        int k = key; javax.swing.SwingUtilities.invokeLater(() -> keyboardPanel.setKeyOn(k, true));
    }

    private void _onNoteOffFrame(org.si.utils.Event e) {
        SiONTrackEvent te = (SiONTrackEvent) e;
        int key = te.getNote() - octave * 12;
        while (key < 0) key += 12;
        while (key > 16) key -= 12;
        int k = key; javax.swing.SwingUtilities.invokeLater(() -> keyboardPanel.setKeyOn(k, false));
    }

    private void _allNoteOff() {
        int baseNote = octave * 12;
        for (int i = 0; i < 17; i++) driver.noteOff(i + baseNote, 0, 0, 0, false);
    }

    private void _onKeyDown(KeyEvent e) {
        char charCode = e.getKeyChar();
        for (int i = 0; i < 17; i++) {
            if (keyboardCodeTable[i] == charCode) {
                int flag = 1 << i;
                if ((keyboardFlag & flag) == 0) {
                    keyboardFlag |= flag;
                    SiMMLTrack trk = driver.noteOn(i + octave * 12, currentVoice, 0, 0, 0, 0, true);
                    if (trk != null && trk.channel != null) {
                        trk.channel.setStreamSend(1, delaySendLevel);
                        trk.channel.setStreamSend(2, chorusSendLevel);
                    }
                }
                return;
            }
        }

        switch (charCode) {
            case 'w':
                _allNoteOff();
                if (++octave > 7) octave = 7;
                break;
            case 'q':
                _allNoteOff();
                if (--octave < 2) octave = 2;
                break;
        }
    }

    private void _onKeyUp(KeyEvent e) {
        char charCode = e.getKeyChar();
        for (int i = 0; i < 17; i++) {
            if (keyboardCodeTable[i] == charCode) {
                int flag = 1 << i;
                if ((keyboardFlag & flag) != 0) {
                    keyboardFlag &= ~flag;
                    driver.noteOff(i + octave * 12, 0, 0, 0, false);
                }
                return;
            }
        }
    }

    class KeyboardPanel extends JPanel {
        private boolean[] keyOnVisible = new boolean[17];
        private int[] wk = {0,1,0,1,0,0,1,0,1,0,1,0,0,1,0,1,0};

        public KeyboardPanel() {
            setPreferredSize(new Dimension(320, 200));
            setBackground(Color.LIGHT_GRAY);
        }

        public void setKeyOn(int key, boolean on) {
            if (key >= 0 && key < 17) {
                keyOnVisible[key] = on;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            int x = 12;
            int y = 20;
            
            // Draw white keys first
            for (int k = 0; k < 17; k++) {
                if (wk[k] == 0) {
                    int kx = getXPosition(k);
                    if (keyOnVisible[k]) {
                        g2.setColor(new Color(0x000080));
                    } else {
                        g2.setColor(Color.WHITE);
                    }
                    g2.fillRect(kx, y, 30, 120);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(kx, y, 30, 120);
                    g2.drawString("" + keyboardKeys.charAt(k), kx + 10, y + 115);
                }
            }
            
            // Draw black keys
            for (int k = 0; k < 17; k++) {
                if (wk[k] == 1) {
                    int kx = getXPosition(k);
                    if (keyOnVisible[k]) {
                        g2.setColor(new Color(0x8080FF));
                    } else {
                        g2.setColor(new Color(0x404040));
                    }
                    g2.fillRect(kx, y, 20, 80);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(kx, y, 20, 80);
                    g2.setColor(Color.WHITE);
                    g2.drawString("" + keyboardKeys.charAt(k), kx + 5, y + 75);
                }
            }
            
            g2.setColor(Color.BLACK);
            g2.drawString("< q", 10, y + 140);
            g2.drawString("w >", 280, y + 140);
            g2.drawString("Octave: " + octave, 120, y + 140);
        }
        
        private int getXPosition(int k) {
            int x = 12;
            int pk = 0;
            for (int i = 0; i <= k; i++) {
                if (i > 0) {
                    x += (wk[i] == pk) ? 30 : 15;
                }
                pk = wk[i];
            }
            // Adjustment for black keys overlap
            if (wk[k] == 1) {
                return x - 10;
            }
            return x;
        }
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
        new Keyboard();
    }
}
