package se.urmo.wolf.states;

import se.urmo.wolf.Game;
import se.urmo.wolf.Window;
import se.urmo.wolf.core.Input;
import se.urmo.wolf.core.State;
import se.urmo.wolf.core.StateManager;

import se.urmo.wolf.world.*;
import se.urmo.wolf.entities.*;
import se.urmo.wolf.render.Raycaster;
import se.urmo.wolf.render.MinimapRenderer;
import se.urmo.wolf.render.HudRenderer;
import se.urmo.wolf.gfx.Assets;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PlayState implements State {
    private final StateManager sm;

    private GameMap map;
    private final Player player;
    private final List<SpriteEntity> sprites = new ArrayList<>();
    private final Raycaster raycaster;

    private final MinimapRenderer minimapRenderer = new MinimapRenderer();
    private final HudRenderer hudRenderer = new HudRenderer();

    private String currentMapPath = "maps/map1.txt";
    private double spawnX, spawnY;

    private double timeSeconds = 0.0;
    private boolean showMinimap = true;
    private Input inputRef;

    private double shootCooldown = 0.25;
    private double shootTimer = 0.0;
    private static final double SHOOT_FOV_DEG = 7.5;
    private static final double SHOOT_MAX_DIST = 12.0;

    private static final double[] HANDGUN_FRAME_T = {0.05, 0.10, 0.18};
    private double weaponAnimElapsed = 0.0;
    private boolean weaponAnimating = false;

    private double weaponBobTime = 0.0;
    private double bobIntensity = 0.0;
    private static final double BOB_SPEED = 6.0;
    private static final double BOB_EASE = 4.0;
    private static final double BOB_AMP_X = 6.0;
    private static final double BOB_AMP_Y = 4.0;
    private double prevPX, prevPY;

    private boolean deathSequence = false;
    private double deathTimer = 0.0;
    private static final double FLASH_IN = 0.08;
    private static final double FLASH_HOLD = 0.15;
    private static final double FLASH_OUT = 0.60;
    private static final double DEATH_PAUSE = FLASH_IN + FLASH_HOLD + FLASH_OUT + 0.35;
    private float deathFlashAlpha = 0f;

    public PlayState(StateManager sm) {
        this.sm = sm;
        this.player = new Player(1, 1, null);
        loadLevel(currentMapPath);
        this.prevPX = player.x;
        this.prevPY = player.y;
        this.raycaster = new Raycaster(Game.WIDTH, Game.HEIGHT);
    }

    public void setLevelPath(String path) {
        if (path != null && !path.isEmpty()) this.currentMapPath = path;
    }

    private void loadLevel(String path) {
        this.currentMapPath = path;

        MapData data = MapLoader.load(path);
        this.map = data.map;

        player.setMap(this.map);
        this.spawnX = data.playerStartX;
        this.spawnY = data.playerStartY;
        player.respawn(spawnX, spawnY);

        sprites.clear();
        for (var s : data.spawns) {
            if (s.type == MapData.Spawn.Type.GUARD) {
                sprites.add(new Guard(s.x, s.y, Assets.GUARD_WALK));
            }
        }
    }

    @Override
    public void handleInput(Input input) {
        this.inputRef = input;

        if (input.wasPressed(KeyEvent.VK_ESCAPE)) {
            sm.set(new MenuState(sm));
            return;
        }
        if (deathSequence) return;

        if (input.wasPressed(KeyEvent.VK_ENTER)) tryOpenDoorInFront();
        if (input.wasPressed(KeyEvent.VK_SPACE)) tryShoot();
        if (input.wasPressed(KeyEvent.VK_M)) showMinimap = !showMinimap;
    }

    private void tryOpenDoorInFront() {
        double reach = 1.0;
        int tx = (int) Math.floor(player.x + player.dirX * reach);
        int ty = (int) Math.floor(player.y + player.dirY * reach);
        double cx = tx + 0.5, cy = ty + 0.5;
        double dist2 = (cx - player.x) * (cx - player.x) + (cy - player.y) * (cy - player.y);
        if (dist2 > (1.6 * 1.6)) return;
        if (map.isDoor(tx, ty)) map.openDoor(tx, ty);
    }

    private void tryShoot() {
        if (shootTimer > 0) return;
        shootTimer = shootCooldown;

        weaponAnimating = true;
        weaponAnimElapsed = 0.0;

        Enemy target = acquireTarget();
        if (target != null) {
            target.takeDamage(1, player, timeSeconds);
        }
    }

    private Enemy acquireTarget() {
        Enemy best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        double px = player.x, py = player.y;
        double dxView = player.dirX, dyView = player.dirY;
        double viewLen = Math.hypot(dxView, dyView);
        double cosThresh = Math.cos(Math.toRadians(SHOOT_FOV_DEG));

        for (SpriteEntity s : sprites) {
            if (!(s instanceof Enemy e)) continue;
            if (!e.isAlive()) continue;

            double dx = e.x - px, dy = e.y - py;
            double dist = Math.hypot(dx, dy);
            if (dist > SHOOT_MAX_DIST) continue;

            double dot = (dx * dxView + dy * dyView) / (dist * (viewLen == 0 ? 1 : viewLen));
            if (dot < cosThresh) continue;
            if (!hasLineOfSight(px, py, e.x, e.y)) continue;

            double aimErr = Math.acos(Math.max(-1, Math.min(1, dot)));
            double score = aimErr * 10.0 + dist * 0.01;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private boolean hasLineOfSight(double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return true;

        double stepX = dx / len, stepY = dy / len;
        double rx = ax, ry = ay;
        int steps = (int) Math.ceil(len * 4.0);
        for (int i = 0; i < steps; i++) {
            rx += stepX * 0.25;
            ry += stepY * 0.25;
            if (map.isSolid((int) rx, (int) ry)) return false;
        }
        return true;
    }

    @Override
    public void update(double dt) {
        timeSeconds += dt;

        if (deathSequence) {
            deathTimer += dt;
            updateDeathFlashAlpha(deathTimer);

            if (deathTimer >= DEATH_PAUSE) {
                if (player.getLives() > 0) {
                    loadLevel(currentMapPath);
                    resetAfterRespawn();
                } else {
                    sm.set(new GameOverState(sm, currentMapPath));
                }
            }
            return;
        }

        if (shootTimer > 0) shootTimer -= dt;
        if (inputRef != null) player.update(dt, inputRef);

        for (var s : sprites) {
            if (s instanceof Enemy e) e.update(dt, map, player);
        }
        map.updateDoors(dt);

        if (weaponAnimating) {
            weaponAnimElapsed += dt;
            if (weaponAnimElapsed >= HANDGUN_FRAME_T[2]) {
                weaponAnimating = false;
            }
        }

        double dx = player.x - prevPX;
        double dy = player.y - prevPY;
        double moved = Math.hypot(dx, dy);
        prevPX = player.x;
        prevPY = player.y;

        if (moved > 1e-5) {
            weaponBobTime += dt * BOB_SPEED;
            bobIntensity = Math.min(1.0, bobIntensity + dt * BOB_EASE);
        } else {
            bobIntensity = Math.max(0.0, bobIntensity - dt * BOB_EASE);
        }

        if (player.isDead()) startDeathSequence();
    }

    private void startDeathSequence() {
        if (deathSequence) return;
        deathSequence = true;
        deathTimer = 0.0;
        weaponAnimating = false;
        weaponAnimElapsed = 0.0;
        if (player.getLives() > 0) player.loseLife();
    }

    private void updateDeathFlashAlpha(double t) {
        if (t < FLASH_IN) {
            deathFlashAlpha = (float) (t / FLASH_IN);
        } else if (t < FLASH_IN + FLASH_HOLD) {
            deathFlashAlpha = 1f;
        } else if (t < FLASH_IN + FLASH_HOLD + FLASH_OUT) {
            double tt = (t - FLASH_IN - FLASH_HOLD) / FLASH_OUT;
            deathFlashAlpha = (float) Math.max(0.0, 1.0 - tt);
        } else {
            deathFlashAlpha = 0f;
        }
    }

    private void resetAfterRespawn() {
        deathSequence = false;
        deathTimer = 0.0;
        deathFlashAlpha = 0f;
        shootTimer = 0.0;
        weaponAnimating = false;
        weaponAnimElapsed = 0.0;
        prevPX = player.x;
        prevPY = player.y;
    }

    @Override
    public void render(Window window) {
        var g2 = window.getFrameGraphics();
        BufferedImage weaponFrame = currentWeaponFrame();

        // World (3D)
        raycaster.render(window.getFramebuffer(), map, player, sprites, timeSeconds);

        // Overlays
        if (showMinimap) {
            minimapRenderer.render(g2, map, player, sprites, 8, 8, 160, 6);
        }
        hudRenderer.render(g2, player);

        // Weapon (drawn as an overlay; bobbing)
        renderWeapon(g2, weaponFrame, weaponBobOffsetX(), weaponBobOffsetY());

        // Death flash
        if (deathSequence && deathFlashAlpha > 0f) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.SrcOver.derive(deathFlashAlpha));
            g2.setColor(new Color(200, 20, 20));
            g2.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
            g2.setComposite(old);
        }

        g2.dispose();
    }

    private double weaponBobOffsetX() {
        return Math.sin(weaponBobTime) * BOB_AMP_X * bobIntensity;
    }

    private double weaponBobOffsetY() {
        return Math.cos(weaponBobTime * 2.0) * BOB_AMP_Y * bobIntensity;
    }

    private void renderWeapon(Graphics2D g2, BufferedImage weaponFrame, double bobX, double bobY) {
        if (weaponFrame == null) return;

        int imgW = weaponFrame.getWidth();
        int imgH = weaponFrame.getHeight();

        double scale = 2.0;
        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);

        int x = (int) ((Game.WIDTH - drawW) / 2 + bobX);
        int y = (int) ((Game.HEIGHT - drawH - 8) + bobY);

        g2.drawImage(weaponFrame, x, y, drawW, drawH, null);
    }

    private BufferedImage currentWeaponFrame() {
        if (!weaponAnimating) return Assets.HANDGUN_READY;
        double t = weaponAnimElapsed;
        if (t < HANDGUN_FRAME_T[0]) return Assets.HANDGUN_SHOT1;
        if (t < HANDGUN_FRAME_T[1]) return Assets.HANDGUN_SHOT2;
        return Assets.HANDGUN_READY;
    }
}
