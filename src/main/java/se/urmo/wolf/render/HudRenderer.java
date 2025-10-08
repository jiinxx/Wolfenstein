package se.urmo.wolf.render;

import se.urmo.wolf.Game;
import se.urmo.wolf.entities.Player;

import java.awt.*;

public final class HudRenderer {

    public void render(Graphics2D g2, Player player) {
        // Basic text HUD (health/lives). Extend as you like.
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(8, Game.HEIGHT - 34, 200, 26, 10, 10);

        g2.setColor(Color.white);
        g2.drawString("Health: " + player.getHealth() + "%", 18, Game.HEIGHT - 17);
        g2.drawString("Lives: " + player.getLives(), 120, Game.HEIGHT - 17);
    }
}
