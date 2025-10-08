package se.urmo.wolf.states;

import se.urmo.wolf.Game;
import se.urmo.wolf.Window;
import se.urmo.wolf.core.Input;
import se.urmo.wolf.core.State;
import se.urmo.wolf.core.StateManager;

import java.awt.*;
import java.awt.event.KeyEvent;

public class MenuState implements State {
    private final StateManager sm;

    public MenuState(StateManager sm) { this.sm = sm; }

    @Override public void handleInput(Input input) {
        if (input.wasPressed(KeyEvent.VK_ENTER)) {
            sm.set(new LevelStartState(sm, "maps/map1.txt", "FLOOR 1"));
        }
        if (input.wasPressed(KeyEvent.VK_ESCAPE)) {
            System.exit(0);
        }
    }

    @Override public void update(double dt) {}

    @Override
    public void render(Window window) {
        var g2 = window.getFrameGraphics();
        g2.setColor(new Color(8,12,16));
        g2.fillRect(0,0, Game.WIDTH, Game.HEIGHT);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 36));
        String title = "Wolf-like Java Raycaster";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (Game.WIDTH - tw)/2, Game.HEIGHT/2 - 20);

        g2.setFont(new Font("Consolas", Font.PLAIN, 18));
        String hint = "Press ENTER to start • ESC to quit";
        int hw = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (Game.WIDTH - hw)/2, Game.HEIGHT/2 + 20);
        g2.dispose();
    }
}