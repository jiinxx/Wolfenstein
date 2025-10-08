package se.urmo.wolf.render;

import se.urmo.wolf.entities.Enemy;
import se.urmo.wolf.entities.SpriteEntity;
import se.urmo.wolf.entities.Player;
import se.urmo.wolf.world.GameMap;

import java.awt.*;
import java.util.List;

public final class MinimapRenderer {

    // Draw a cropped, zoomed minimap centered around the player.
    public void render(Graphics2D g2,
                       GameMap map,
                       Player player,
                       List<SpriteEntity> sprites,
                       int x, int y,          // top-left screen position
                       int sizePx,            // square size in pixels, e.g., 160
                       int radiusTiles) {     // how many tiles from player to show (span = 2*radius + 1)

        // Backing panel
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(Color.black);
        g2.fillRoundRect(x - 2, y - 2, sizePx + 4, sizePx + 4, 10, 10);
        g2.setComposite(oldComp);

        int mw = map.w, mh = map.h;
        int span = radiusTiles * 2 + 1;

        int centerX = (int) Math.floor(player.x);
        int centerY = (int) Math.floor(player.y);

        int startX = Math.max(0, Math.min(mw - span, centerX - radiusTiles));
        int startY = Math.max(0, Math.min(mh - span, centerY - radiusTiles));

        double s = (double) sizePx / span;

        // Tiles
        for (int my = 0; my < span; my++) {
            for (int mx = 0; mx < span; mx++) {
                int gx = startX + mx;
                int gy = startY + my;

                int tileX = (int) (x + mx * s);
                int tileY = (int) (y + my * s);
                int tileW = (int) Math.max(1, Math.ceil(s));
                int tileH = tileW;

                int v = map.at(gx, gy);
                if (v == 0) {
                    g2.setColor(new Color(60, 60, 60));      // floor
                } else if (v == 1) {
                    g2.setColor(new Color(200, 200, 200));   // wall
                } else if (v == 2) {
                    g2.setColor(new Color(140, 180, 220));   // door
                } else {
                    g2.setColor(new Color(100, 100, 100));   // other
                }
                g2.fillRect(tileX, tileY, tileW, tileH);
            }
        }

        // Player marker + facing / FOV
        int px = (int) (x + (player.x - startX) * s);
        int py = (int) (y + (player.y - startY) * s);

        g2.setColor(Color.green);
        g2.fillOval(px - 3, py - 3, 6, 6);

        int lx = (int) (px + player.dirX * 12);
        int ly = (int) (py + player.dirY * 12);
        g2.drawLine(px, py, lx, ly);

        int fovLx = (int) (px + (player.dirX + player.planeX) * 10);
        int fovLy = (int) (py + (player.dirY + player.planeY) * 10);
        int fovRx = (int) (px + (player.dirX - player.planeX) * 10);
        int fovRy = (int) (py + (player.dirY - player.planeY) * 10);
        g2.drawLine(px, py, fovLx, fovLy);
        g2.drawLine(px, py, fovRx, fovRy);

        // Enemies / sprites in the cropped region
        g2.setColor(Color.red);
        for (var sEnt : sprites) {
            int exTile = (int) Math.floor(sEnt.x);
            int eyTile = (int) Math.floor(sEnt.y);
            if (exTile >= startX && exTile < startX + span && eyTile >= startY && eyTile < startY + span) {
                int ex = (int) (x + (sEnt.x - startX) * s);
                int ey = (int) (y + (sEnt.y - startY) * s);
                // Emphasize enemies slightly
                if (sEnt instanceof Enemy) {
                    g2.fillRect(ex - 2, ey - 2, 4, 4);
                } else {
                    g2.fillRect(ex - 1, ey - 1, 2, 2);
                }
            }
        }

        // Border
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawRoundRect(x - 2, y - 2, sizePx + 4, sizePx + 4, 10, 10);
    }
}
