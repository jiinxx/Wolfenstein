package se.urmo.wolf.world;

public class GameMap {
    public final int w, h;
    private final int[][] grid;

    // Door state arrays
    private final double[][] doorProg; // 0..1 open progress
    private final boolean[][] doorOpening;

    public GameMap(int w, int h, int[][] grid) {
        this.w = w; this.h = h; this.grid = grid;
        doorProg = new double[h][w];
        doorOpening = new boolean[h][w];
    }

    public int at(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return 1;
        return grid[y][x];
    }

    public boolean isSolid(int x, int y) {
        int t = at(x, y);
        if (t == 1) return true;
        if (t == 2) {
            // solid if not fully open
            return doorProg[y][x] < 1.0;
        }
        return false;
    }

    public boolean isDoor(int x, int y) { return at(x,y) == 2; }
    public boolean isDoorOpen(int x, int y) { return doorProg[y][x] >= 1.0; }
    public double getDoorProgress(int x, int y) { return doorProg[y][x]; }

    // Determines if door is vertical (slides along Y) by checking walls layout.
    public boolean isDoorVertical(int x, int y) {
        // A vertical door typically sits between east/west walls
        boolean wallN = at(x, y-1) == 1;
        boolean wallS = at(x, y+1) == 1;
        boolean wallE = at(x+1, y) == 1;
        boolean wallW = at(x-1, y) == 1;

        // If walls are N/S, corridor E/W -> vertical=false (horizontal plane).
        // If walls are E/W, corridor N/S -> vertical=true (vertical plane).
        if (wallE || wallW) return true;
        if (wallN || wallS) return false;
        // fallback: choose by corridor openness
        int openNS = (at(x, y-1)==0?1:0) + (at(x, y+1)==0?1:0);
        int openEW = (at(x-1, y)==0?1:0) + (at(x+1, y)==0?1:0);
        return openNS > openEW;
    }

    // Sign indicates slide direction: +1 positive axis, -1 negative
    public int getDoorSign(int x, int y) {
        // Always slide into an adjacent wall if present, else positive
        if (isDoorVertical(x,y)) {
            // slide along Y; pick direction toward a wall if any
            if (at(x, y+1) == 1) return +1; // slide south into wall
            if (at(x, y-1) == 1) return -1; // slide north
            return +1;
        } else {
            if (at(x+1, y) == 1) return +1; // slide east
            if (at(x-1, y) == 1) return -1; // slide west
            return +1;
        }
    }

    public void openDoor(int x, int y) {
        if (!isDoor(x,y)) return;
        doorOpening[y][x] = true;
    }

    public void updateDoors(double dt) {
        double speed = 1.2; // tiles per second (approx)
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            if (grid[y][x] == 2) {
                if (doorOpening[y][x]) {
                    doorProg[y][x] += dt * speed;
                    if (doorProg[y][x] >= 1.0) {
                        doorProg[y][x] = 1.0;
                        doorOpening[y][x] = false;
                    }
                }
            }
        }
    }
}