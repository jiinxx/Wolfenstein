package se.urmo.wolf.states;

import se.urmo.wolf.Game;
import se.urmo.wolf.Window;
import se.urmo.wolf.core.Input;
import se.urmo.wolf.core.State;
import se.urmo.wolf.core.StateManager;

import java.awt.*;
import java.awt.event.KeyEvent;

public class LevelStartState implements State {
    private final StateManager sm;
    private final String mapPath;
    private final String floorText;

    private static final double FADE_IN  = 0.60;
    private static final double HOLD     = 0.80;
    private static final double FADE_OUT = 0.40;
    private static final double TOTAL    = FADE_IN + HOLD + FADE_OUT;

    private double t = 0.0;
    private boolean done = false;

    public LevelStartState(StateManager sm, String mapPath, String floorText) {
        this.sm = sm;
        this.mapPath = (mapPath == null || mapPath.isEmpty()) ? "maps/map1.txt" : mapPath;
        this.floorText = (floorText == null || floorText.isEmpty()) ? "FLOOR 1" : floorText;
    }

    @Override public void handleInput(Input input) {
        if (input.wasPressed(KeyEvent.VK_ESCAPE)) { sm.set(new MenuState(sm)); return; }
        if (input.wasPressed(KeyEvent.VK_ENTER)) { startGameplay(); }
    }

    @Override public void update(double dt) {
        if (done) return;
        t += dt;
        if (t >= TOTAL) startGameplay();
    }

    private void startGameplay() {
        if (done) return;
        done = true;
        PlayState play = new PlayState(sm);
        play.setLevelPath(mapPath);
        sm.set(play);
    }

    @Override
    public void render(Window window) {
        var g2 = window.getFrameGraphics();

        g2.setColor(new Color(8, 10, 16));
        g2.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        float alpha;
        if (t < FADE_IN) alpha = (float)(t / FADE_IN);
        else if (t < FADE_IN + HOLD) alpha = 1f;
        else if (t < TOTAL) alpha = (float)Math.max(0.0, 1.0 - (t - FADE_IN - HOLD) / FADE_OUT);
        else alpha = 0f;

        String title = floorText;
        String subtitle = "Get psyched!";

        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));

        g2.setColor(new Color(230, 240, 255));
        g2.setFont(new Font("Consolas", Font.BOLD, 52));
        int tw = g2.getFontMetrics().stringWidth(title);
        int tx = (Game.WIDTH - tw) / 2;
        int ty = Game.HEIGHT / 2 - 12;
        g2.drawString(title, tx, ty);

        g2.setColor(new Color(160, 200, 255));
        g2.setFont(new Font("Consolas", Font.PLAIN, 24));
        int sw = g2.getFontMetrics().stringWidth(subtitle);
        g2.drawString(subtitle, (Game.WIDTH - sw) / 2, ty + 40);

        g2.setComposite(AlphaComposite.SrcOver);

        g2.setColor(new Color(255, 255, 255, 120));
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        String hint = "Press ENTER to skip • ESC for menu";
        int hw = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (Game.WIDTH - hw) / 2, Game.HEIGHT - 28);

        g2.dispose();
    }
}