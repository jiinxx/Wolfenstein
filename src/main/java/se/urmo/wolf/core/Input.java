package se.urmo.wolf.core;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class Input extends KeyAdapter {
    private final boolean[] down = new boolean[256];
    private final boolean[] pressed = new boolean[256];
    private final boolean[] consumed = new boolean[256];

    public void reset() {
        Arrays.fill(down, false);
        Arrays.fill(pressed, false);
        Arrays.fill(consumed, false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode() & 0xFF;
        if (!down[k]) { pressed[k] = true; }
        down[k] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode() & 0xFF;
        down[k] = false;
        consumed[k] = false;
    }

    public void poll() {
        Arrays.fill(pressed, false);
    }

    public boolean isDown(int keyCode) { return down[keyCode & 0xFF]; }
    public boolean wasPressed(int keyCode) {
        int k = keyCode & 0xFF;
        if (pressed[k] && !consumed[k]) {
            consumed[k] = true;
            return true;
        }
        return false;
    }
}