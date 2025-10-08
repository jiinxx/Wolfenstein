package se.urmo.wolf.entities;

import se.urmo.wolf.world.GameMap;
import se.urmo.wolf.gfx.DirectionalSprite;
import se.urmo.wolf.gfx.Texture;

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Enemy extends AnimatedSpriteEntity {
    public enum State { IDLE, PATROL, ALERT, CHASE, DYING, DEAD }
    private static final AtomicInteger ID_SRC = new AtomicInteger(1);

    public final int enemyId = ID_SRC.getAndIncrement();
    protected final double spawnX, spawnY;

    protected State state = State.PATROL;

    protected double dirX = 1, dirY = 0;

    protected double viewDistance = 8.0;
    protected double fovCos = Math.cos(Math.toRadians(45));
    private boolean lastLOS = false;

    private int health = 3;

    protected int deathRow1Based = 6;
    protected int deathStartCol1Based = 1;
    protected int deathFrames = 5;
    protected double deathFps = 10.0;

    private double deathTime = 0.0;
    private int deathFrameIndex = 0;
    private BufferedImage deathFinalFrame = null;

    protected int fireRow1Based = 7;
    protected int fireStartCol1Based = 1;
    protected int fireFrames = 3;
    protected double fireFps = 12.0;
    protected int fireHoldRepeats = 4;

    private boolean firing = false;
    private double fireTime = 0.0;
    private int fireFrameIndex = 0;

    protected double fireIntervalMin = 3.0;
    protected double fireIntervalMax = 5.0;
    private double fireCooldown = randBetween(fireIntervalMin, fireIntervalMax);

    protected double alertMemorySeconds = 2.0;
    private double alertTimer = 0.0;
    private double lastKnownPlayerX, lastKnownPlayerY;

    protected double baseHitAt1Tile = 0.65;
    protected double hitFalloffPerTile = 0.12;
    protected double minHitChance = 0.05;
    protected double maxHitChance = 0.95;
    protected double difficultyScale = 1.0;
    protected int shotDamage = 10;

    public Enemy(double x, double y, Texture fallback) {
        super(x, y, fallback);
        this.spawnX = x; this.spawnY = y;
        debugLog("spawned at (%.2f, %.2f)", x, y);
    }
    public Enemy(double x, double y, DirectionalSprite ds) {
        super(x, y, ds);
        this.spawnX = x; this.spawnY = y;
        debugLog("spawned at (%.2f, %.2f)", x, y);
    }

    public State getState() { return state; }
    public boolean isAlive() { return state != State.DYING && state != State.DEAD; }
    public int getHealth() { return health; }

    public boolean takeDamage(int amount, Player shooterView, double timeSeconds) {
        if (!isAlive()) return false;
        health -= Math.max(1, amount);
        markAlert(shooterView.x, shooterView.y, true);
        if (health <= 0) {
            setStateWithLog(State.DYING, "killed");
            deathTime = 0.0;
            deathFrameIndex = 0;
            deathFinalFrame = null;
            firing = false;
            fireTime = 0.0;
            return true;
        }
        return false;
    }

    public abstract void update(double dt, GameMap map, Player player);

    protected void updateCommon(double dt, GameMap map, Player player) {
        if (state == State.DYING) {
            deathTime += dt;
            int idx = (int)Math.floor(deathTime * deathFps);
            if (idx >= deathFrames) {
                idx = deathFrames - 1;
                if (deathFinalFrame == null && dirSprite != null) {
                    int row0 = deathRow1Based - 1;
                    int col0 = (deathStartCol1Based - 1) + idx;
                    deathFinalFrame = dirSprite.frame(row0, col0);
                }
                setStateWithLog(State.DEAD, "death anim complete");
            } else {
                deathFrameIndex = idx;
            }
            return;
        }
        if (!isAlive()) return;

        if (firing) {
            fireTime += dt;
            int virtualFrames = fireFrames + Math.max(0, fireHoldRepeats);
            int idx = (int)Math.floor(fireTime * fireFps);
            if (idx >= virtualFrames) {
                firing = false;
                fireTime = 0.0;
                fireFrameIndex = 0;
                fireCooldown = randBetween(fireIntervalMin, fireIntervalMax);
                debugLog("fire sequence complete; cooldown=%.2fs", fireCooldown);
            } else {
                fireFrameIndex = Math.min(idx, fireFrames - 1);
            }
        } else {
            if (fireCooldown > 0.0) fireCooldown -= dt;
        }

        double prev = alertTimer;
        if (alertTimer > 0.0) { alertTimer -= dt; if (alertTimer < 0) alertTimer = 0; }
        if (prev > 0.0 && alertTimer == 0.0 && !lastLOS) {
            debugLog("alert expired; returning to patrol");
            setStateWithLog(State.PATROL, "alert expired");
        }
    }

    protected boolean tryStartFiring(GameMap map, Player player) {
        if (!isAlive() || state == State.DYING || state == State.DEAD) return false;
        if (firing) return false;
        if (fireCooldown > 0.0) return false;
        if (!canSee(map, player)) return false;

        firing = true;
        fireTime = 0.0;
        fireFrameIndex = 0;

        double dx = player.x - x, dy = player.y - y;
        double dist = Math.hypot(dx, dy);
        double chance = baseHitAt1Tile - hitFalloffPerTile * Math.max(0.0, dist - 1.0);
        chance *= difficultyScale;
        chance = clamp(chance, minHitChance, maxHitChance);

        double roll = ThreadLocalRandom.current().nextDouble();
        boolean hit = roll < chance;
        debugLog("fires at dist=%.2f chance=%.2f roll=%.2f => %s", dist, chance, roll, hit ? "HIT" : "MISS");
        if (hit) player.takeDamage(shotDamage);
        return true;
    }

    protected void markAlert(double px, double py, boolean faceAndChaseNow) {
        lastKnownPlayerX = px; lastKnownPlayerY = py;
        alertTimer = alertMemorySeconds;
        if (faceAndChaseNow) {
            double dx = px - x, dy = py - y, len = Math.hypot(dx, dy);
            if (len > 1e-6) { dirX = dx / len; dirY = dy / len; }
            setStateWithLog(State.CHASE, "alerted");
            fireCooldown = Math.min(fireCooldown, 0.2);
        }
    }

    protected void tryMove(GameMap map, double nx, double ny) {
        if (!isAlive()) return;
        if (!map.isSolid((int)nx, (int)y)) x = nx;
        if (!map.isSolid((int)x, (int)ny)) y = ny;
    }

    protected boolean canSee(GameMap map, Player p) {
        double dx = p.x - x, dy = p.y - y;
        double dist2 = dx*dx + dy*dy;
        if (dist2 > viewDistance*viewDistance) { handleLOS(false, p); return false; }

        double len = Math.sqrt(dist2);
        if (len < 1e-6) { handleLOS(true, p); return true; }
        double ndx = dx / len, ndy = dy / len;
        double facingDot = ndx * dirX + ndy * dirY;
        if (facingDot < fovCos) { handleLOS(false, p); return false; }

        double stepX = dx / len, stepY = dy / len;
        double rx = x, ry = y;
        int steps = (int)Math.ceil(len * 4.0);
        for (int i=0;i<steps;i++) {
            rx += stepX * 0.25; ry += stepY * 0.25;
            if (map.isSolid((int)rx, (int)ry)) { handleLOS(false, p); return false; }
        }
        handleLOS(true, p);
        markAlert(p.x, p.y, false);
        return true;
    }

    private void handleLOS(boolean hasLOS, Player p) {
        if (hasLOS && !lastLOS) {
            debugLog("acquired LOS on player (%.2f, %.2f)", p.x, p.y);
        } else if (!hasLOS && lastLOS) {
            debugLog("lost LOS; memory=%.2fs", alertTimer);
        }
        lastLOS = hasLOS;
    }

    protected boolean isAlerted() { return alertTimer > 0.0; }
    protected double lastKnownX() { return lastKnownPlayerX; }
    protected double lastKnownY() { return lastKnownPlayerY; }
    protected boolean isFiringAnimPlaying() { return firing; }

    @Override protected double facingX(Player p) { return dirX; }
    @Override protected double facingY(Player p) { return dirY; }

    @Override
    public BufferedImage getFrameImage(Player p, double timeSeconds) {
        if (state == State.DEAD && deathFinalFrame != null) return deathFinalFrame;
        if (state == State.DYING) {
            if (dirSprite == null) return texture != null ? texture.img : null;
            int row0 = deathRow1Based - 1;
            int col0 = (deathStartCol1Based - 1) + Math.max(0, Math.min(deathFrames - 1, deathFrameIndex));
            return dirSprite.frame(row0, col0);
        }
        if (firing && dirSprite != null) {
            int row0 = fireRow1Based - 1;
            int col0 = (fireStartCol1Based - 1) + Math.max(0, Math.min(fireFrames - 1, fireFrameIndex));
            return dirSprite.frame(row0, col0);
        }
        return super.getFrameImage(p, timeSeconds);
    }

    protected void setStateWithLog(State newState, String reason) {
        if (this.state != newState) {
            debugLog("state %s -> %s (%s)", this.state, newState, reason);
            this.state = newState;
        }
    }

    protected void debugLog(String fmt, Object... args) {
        System.out.printf("[Guard#%d] %s%n", enemyId, String.format(fmt, args));
    }

    private static double randBetween(double a, double b) {
        if (b < a) { double t = a; a = b; b = t; }
        return ThreadLocalRandom.current().nextDouble(a, b);
    }
    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}