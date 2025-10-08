package se.urmo.wolf.entities;

import se.urmo.wolf.core.Input;
import se.urmo.wolf.world.GameMap;

import java.awt.event.KeyEvent;

public class Player {
    public double x, y;
    public double dirX = 1, dirY = 0;
    public double planeX = 0, planeY = 0.66;

    private GameMap map;

    private double moveSpeed = 3.0;
    private double sprintMult = 1.5;
    private double rotSpeed   = Math.toRadians(120);

    private int health = 100;
    private boolean dead = false;
    private int lives = 3;

    public Player(double x, double y, GameMap map) { this.x=x; this.y=y; this.map=map; }
    public void setMap(GameMap map) { this.map = map; }

    public void update(double dt, Input input) {
        if (dead) return;

        double ang = 0.0;
        if (input.isDown(KeyEvent.VK_A)) ang -= rotSpeed * dt;
        if (input.isDown(KeyEvent.VK_D)) ang += rotSpeed * dt;
        if (ang != 0.0) {
            double cos = Math.cos(ang), sin = Math.sin(ang);
            double ndx = dirX * cos - dirY * sin;
            double ndy = dirX * sin + dirY * cos;
            dirX = ndx; dirY = ndy;
            double npx = planeX * cos - planeY * sin;
            double npy = planeX * sin + planeY * cos;
            planeX = npx; planeY = npy;
        }

        double speed = moveSpeed * (input.isDown(KeyEvent.VK_SHIFT) ? sprintMult : 1.0);
        double step = speed * dt;
        double moveX = 0, moveY = 0;
        if (input.isDown(KeyEvent.VK_W)) { moveX += dirX * step; moveY += dirY * step; }
        if (input.isDown(KeyEvent.VK_S)) { moveX -= dirX * step; moveY -= dirY * step; }

        if (moveX != 0 || moveY != 0) {
            double nx = x + moveX, ny = y + moveY;
            if (map != null) {
                if (!map.isSolid((int)nx, (int)y)) x = nx;
                if (!map.isSolid((int)x, (int)ny)) y = ny;
            } else { x = nx; y = ny; }
        }
    }

    public int getHealth() { return health; }
    public boolean isDead() { return dead; }
    public boolean takeDamage(int percent) {
        if (dead) return false;
        int dmg = Math.max(0, percent);
        health = Math.max(0, health - dmg);
        if (health == 0) dead = true;
        return dead;
    }
    public void heal(int percent) {
        if (dead) return;
        health = Math.min(100, health + Math.max(0, percent));
    }
    public int getLives() { return lives; }
    public int loseLife() { if (lives>0) lives--; return lives; }
    public void respawn(double sx, double sy) { x=sx; y=sy; health=100; dead=false; }
}