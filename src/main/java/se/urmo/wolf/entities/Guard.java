package se.urmo.wolf.entities;

import se.urmo.wolf.world.GameMap;
import se.urmo.wolf.gfx.DirectionalSprite;

public class Guard extends Enemy {
    private double patrolSpeed = 1.5;
    private double chaseSpeed  = 2.0;
    private double fireMaxDistance = 7.0;

    private enum Axis { H, V }
    private boolean patrolInit = false;
    private Axis patrolAxis = Axis.H;
    private double ax, ay, bx, by;
    private int dirSign = +1;
    private final double EPS = 1e-3;

    private State lastState = State.PATROL;

    public Guard(double x, double y, DirectionalSprite sprite) {
        super(x, y, sprite);
        this.walkRows = 5; this.fps = 8.0; this.flipLR = true;

        this.deathRow1Based = 6; this.deathStartCol1Based = 1; this.deathFrames = 5; this.deathFps = 10.0;

        this.fireRow1Based = 7; this.fireStartCol1Based = 1; this.fireFrames = 3; this.fireFps = 12.0; this.fireHoldRepeats = 4;

        this.fireIntervalMin = 3.0; this.fireIntervalMax = 5.0;

        this.alertMemorySeconds = 2.0;

        dirX = 1; dirY = 0;
    }

    @Override
    public void update(double dt, GameMap map, Player player) {
        if (!patrolInit && map != null) { discoverPatrolSegment(map); patrolInit = true; }

        updateCommon(dt, map, player);
        if (!isAlive() || getState() == State.DYING) return;

        boolean sees = canSee(map, player);
        if (sees || isAlerted()) {
            setStateWithLog(State.CHASE, sees ? "LOS" : "alert memory");
        } else if (getState() == State.CHASE || getState() == State.ALERT) {
            setStateWithLog(State.PATROL, "no LOS and memory expired");
        }

        if (lastState != getState()) { onEnterState(getState()); lastState = getState(); }

        switch (getState()) {
            case CHASE -> {
                if (!isFiringAnimPlaying() && withinFireDistance(player)) {
                    tryStartFiring(map, player);
                }
                if (isFiringAnimPlaying()) {
                    faceTowards(player.x, player.y);
                } else {
                    doChase(dt, map, player, sees);
                }
            }
            case PATROL -> {
                if (!patrolInit) doPatrolFallback(dt, map);
                else doPatrolSegment(dt, map);
            }
            default -> {}
        }

        double len = Math.hypot(dirX, dirY);
        if (len > 1e-6) { dirX /= len; dirY /= len; }
    }

    private void onEnterState(State s) {
        if (s == State.PATROL && patrolInit) reattachToSegment("enter PATROL");
    }

    private void discoverPatrolSegment(GameMap map) {
        int sx = (int)Math.floor(spawnX);
        int sy = (int)Math.floor(spawnY);

        int left=sx, right=sx;
        while (!map.isSolid(left - 1, sy)) left--;
        while (!map.isSolid(right + 1, sy)) right++;

        int up=sy, down=sy;
        while (!map.isSolid(sx, up - 1)) up--;
        while (!map.isSolid(sx, down + 1)) down++;

        int horizLen = right - left;
        int vertLen  = down - up;

        if (horizLen >= vertLen) {
            patrolAxis = Axis.H;
            ax = left + 0.5;  ay = sy + 0.5;
            bx = right + 0.5; by = sy + 0.5;
            dirSign = +1;
            dirX = 1; dirY = 0;
            debugLog("patrol segment discovered: H (%.2f,%.2f) <-> (%.2f,%.2f)", ax, ay, bx, by);
        } else {
            patrolAxis = Axis.V;
            ax = sx + 0.5; ay = up + 0.5;
            bx = sx + 0.5; by = down + 0.5;
            dirSign = +1;
            dirX = 0; dirY = 1;
            debugLog("patrol segment discovered: V (%.2f,%.2f) <-> (%.2f,%.2f)", ax, ay, bx, by);
        }
        reattachToSegment("initial snap");
    }

    private void reattachToSegment(String reason) {
        double oldX = x, oldY = y;
        if (patrolAxis == Axis.H) {
            double minX = Math.min(ax, bx), maxX = Math.max(ax, bx);
            x = clamp(x, minX, maxX); y = ay;
            double dA = Math.abs(x - ax), dB = Math.abs(bx - x);
            dirSign = (dB < dA) ? +1 : -1;
            dirX = dirSign; dirY = 0;
        } else {
            double minY = Math.min(ay, by), maxY = Math.max(ay, by);
            y = clamp(y, minY, maxY); x = ax;
            double dA = Math.abs(y - ay), dB = Math.abs(by - y);
            dirSign = (dB < dA) ? +1 : -1;
            dirX = 0; dirY = dirSign;
        }
        debugLog("reattach to segment (%s): (%.2f,%.2f) -> (%.2f,%.2f), dir=%s",
                reason, oldX, oldY, x, y, dirSign > 0 ? "+B" : "-A");
    }

    private boolean withinFireDistance(Player p) {
        double dx = p.x - x, dy = p.y - y;
        return (dx*dx + dy*dy) <= (fireMaxDistance * fireMaxDistance);
    }

    private void doChase(double dt, GameMap map, Player p, boolean hasLOSNow) {
        double tx = hasLOSNow ? p.x : lastKnownX();
        double ty = hasLOSNow ? p.y : lastKnownY();
        double dx = tx - x, dy = ty - y;
        double len = Math.hypot(dx, dy);
        if (len > 1e-6) { dx/=len; dy/=len; }
        dirX = dx; dirY = dy;
        double step = chaseSpeed * dt;
        tryMove(map, x + dx*step, y + dy*step);
    }

    private void doPatrolSegment(double dt, GameMap map) {
        double step = patrolSpeed * dt;
        double oldX = x, oldY = y;
        boolean flipped = false;

        if (patrolAxis == Axis.H) {
            y = ay; dirX = dirSign; dirY = 0;
            double nx = x + dirSign * step;
            tryMove(map, nx, y);
            double moved = Math.abs(x - oldX);
            if (moved < step * 0.25) {
                dirSign *= -1; dirX = dirSign;
                debugLog("patrol blocked on H; flip dir -> %s", dirSign > 0 ? "+B" : "-A");
                flipped = true;
            }
            double minX = Math.min(ax, bx), maxX = Math.max(ax, bx);
            if (x <= minX + EPS) {
                x = minX;
                if (!flipped && dirSign < 0) { dirSign = +1; dirX = dirSign; debugLog("patrol H hit A-end; flip dir -> +B"); }
            } else if (x >= maxX - EPS) {
                x = maxX;
                if (!flipped && dirSign > 0) { dirSign = -1; dirX = dirSign; debugLog("patrol H hit B-end; flip dir -> -A"); }
            }
        } else {
            x = ax; dirX = 0; dirY = dirSign;
            double ny = y + dirSign * step;
            tryMove(map, x, ny);
            double moved = Math.abs(y - oldY);
            if (moved < step * 0.25) {
                dirSign *= -1; dirY = dirSign;
                debugLog("patrol blocked on V; flip dir -> %s", dirSign > 0 ? "+B" : "-A");
                flipped = true;
            }
            double minY = Math.min(ay, by), maxY = Math.max(ay, by);
            if (y <= minY + EPS) {
                y = minY;
                if (!flipped && dirSign < 0) { dirSign = +1; dirY = dirSign; debugLog("patrol V hit A-end; flip dir -> +B"); }
            } else if (y >= maxY - EPS) {
                y = maxY;
                if (!flipped && dirSign > 0) { dirSign = -1; dirY = dirSign; debugLog("patrol V hit B-end; flip dir -> -A"); }
            }
        }
    }

    private void doPatrolFallback(double dt, GameMap map) {
        double step = patrolSpeed * dt;
        double nx = x + dirX*step, ny = y + dirY*step;
        double oldX = x, oldY = y;
        tryMove(map, nx, ny);
        double moved = Math.hypot(x-oldX, y-oldY);
        if (moved < step * 0.25) { dirX=-dirX; dirY=-dirY; debugLog("fallback patrol turn-around at (%.2f, %.2f)", x, y); }
    }

    private void faceTowards(double tx, double ty) {
        double dx = tx - x, dy = ty - y;
        double len = Math.hypot(dx, dy);
        if (len > 1e-6) { dirX = dx/len; dirY = dy/len; }
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}