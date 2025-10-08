package se.urmo.wolf.core;

import se.urmo.wolf.Window;

public interface State {
    default void onEnter() {}
    default void onExit() {}
    void handleInput(Input input);
    void update(double dt);
    void render(Window window);
}
