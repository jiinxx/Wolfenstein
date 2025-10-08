package se.urmo.wolf.core;

import se.urmo.wolf.Window;

public class StateManager {
    private State current;

    public void set(State next) {
        if (current != null) current.onExit();
        current = next;
        if (current != null) current.onEnter();
    }

    public void handleInput(Input input) { if (current != null) current.handleInput(input); }
    public void update(double dt) { if (current != null) current.update(dt); }
    public void render(Window window) { if (current != null) current.render(window); }
}