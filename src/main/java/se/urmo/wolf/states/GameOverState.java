package se.urmo.wolf.states;

import se.urmo.wolf.Game;
import se.urmo.wolf.Window;
import se.urmo.wolf.core.Input;
import se.urmo.wolf.core.State;
import se.urmo.wolf.core.StateManager;

import java.awt.*;
import java.awt.event.KeyEvent;

public class GameOverState implements State {
    private final StateManager sm;
    private final String restartMapPath;
    private double time = 0.0;

    public GameOverState(StateManager sm) { this(sm, null); }
    public GameOverState(StateManager sm, String restartMapPath) {
        this.sm = sm; this.restartMapPath = restartMapPath;
    }

    @Override public void handleInput(Input input) {
        if (input.wasPressed(KeyEvent.VK_ENTER)) {
            sm.set(new LevelStartState(sm, restartMapPath, "FLOOR 1"));
        }
        if (input.wasPressed(KeyEvent.VK_ESCAPE)) {
            sm.set(new MenuState(sm));
        }
    }

    @Override public void update(double dt) { time += dt; }

    @Override
    public void render(Window window) {
        var g2 = window.getFrameGraphics();

        float a = (float)Math.max(0.0, Math.min(1.0, time * 0.8));
        g2.setColor(new Color(10, 10, 14, (int)(220 * a)));
        g2.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        g2.setColor(new Color(240, 70, 70));
        g2.setFont(new Font("Consolas", Font.BOLD, 64));
        String title = "GAME OVER";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (Game.WIDTH - tw) / 2, Game.HEIGHT / 2 - 20);

        g2.setColor(new Color(230, 230, 230));
        g2.setFont(new Font("Consolas", Font.PLAIN, 20));
        String hint1 = "Press ENTER to restart";
        String hint2 = "Press ESC to return to menu";
        int h1w = g2.getFontMetrics().stringWidth(hint1);
        int h2w = g2.getFontMetrics().stringWidth(hint2);
        g2.drawString(hint1, (Game.WIDTH - h1w) / 2, Game.HEIGHT / 2 + 28);
        g2.drawString(hint2, (Game.WIDTH - h2w) / 2, Game.HEIGHT / 2 + 56);

        g2.dispose();
    }
}