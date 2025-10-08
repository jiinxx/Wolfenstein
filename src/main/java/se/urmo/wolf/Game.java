package se.urmo.wolf;

import se.urmo.wolf.core.StateManager;
import se.urmo.wolf.gfx.Assets;
import se.urmo.wolf.states.LevelStartState;
import se.urmo.wolf.states.MenuState;

public class Game {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final double FIXED_DT = 1.0 / 60.0;

    private final Window window;
    private final StateManager sm;

    public Game() {
        Assets.init();
        window = new Window(WIDTH, HEIGHT, "Wolf-like Java Raycaster");
        sm = new StateManager();
        // Start at LevelStartState (floor 1)
        sm.set(new LevelStartState(sm, "maps/map1.txt", "FLOOR 1"));
    }

    public void run() {
        long prev = System.nanoTime();
        double acc = 0.0;
        while (window.isOpen()) {
            long now = System.nanoTime();
            double dt = (now - prev) / 1e9;
            prev = now;
            acc += dt;

            // Input
            window.poll();

            // Fixed update
            while (acc >= FIXED_DT) {
                sm.handleInput(window.getInput());
                sm.update(FIXED_DT);
                acc -= FIXED_DT;
            }

            // Render
            sm.render(window);
            window.present();
        }
        window.dispose();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}