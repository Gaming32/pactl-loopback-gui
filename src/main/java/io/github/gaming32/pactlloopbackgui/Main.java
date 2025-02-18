package io.github.gaming32.pactlloopbackgui;

import io.github.gaming32.pactlloopbackgui.gui.MainPanel;

import javax.swing.*;

public class Main {
    public static final String TITLE = "PulseAudio Loopback GUI";

    public static void main(String[] args) {
        final var frame = new JFrame(TITLE);
        final var panel = new MainPanel();
        frame.add(panel);
        frame.pack();

        new Timer(2000, e -> panel.refresh()).start();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
