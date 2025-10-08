package se.urmo.wolf.render;

import se.urmo.wolf.gfx.Assets;
import se.urmo.wolf.gfx.Texture;
import se.urmo.wolf.world.GameMap;
import se.urmo.wolf.entities.Player;
import se.urmo.wolf.entities.SpriteEntity;
import se.urmo.wolf.entities.AnimatedSpriteEntity;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

/** 3D-only raycaster: floor/sky, walls/doors (edge slab), sprites. No HUD/UI here. */
public class Raycaster {
    private final int W, H;
    private final double[] zBuffer;

    public Raycaster(int width, int height) {
        this.W = width; this.H = height;
        this.zBuffer = new double[W];
    }

    /** Main entry (world only). */
    public void render(BufferedImage fb, GameMap map, Player p,
                       List<SpriteEntity> sprites, double timeSeconds) {
        drawFloorAndSky(fb, p, H);
        drawWallsAndDoors(fb, map, p, H);
        drawSprites(fb, p, sprites, timeSeconds, H);
    }

    // ------------------------------------------------------------------------
    // Walls & Doors — doors drawn on the OUTER EDGE like walls
    // ------------------------------------------------------------------------
    private void drawWallsAndDoors(BufferedImage fb, GameMap map, Player p, int viewH) {
        for (int x = 0; x < W; x++) {
            double cameraX = 2.0 * x / W - 1.0;
            double rayDirX = p.dirX + p.planeX * cameraX;
            double rayDirY = p.dirY + p.planeY * cameraX;

            int mapX = (int) p.x;
            int mapY = (int) p.y;

            double deltaX = (rayDirX == 0) ? 1e30 : Math.abs(1.0 / rayDirX);
            double deltaY = (rayDirY == 0) ? 1e30 : Math.abs(1.0 / rayDirY);

            int stepX, stepY;
            double sideX, sideY;
            if (rayDirX < 0) { stepX = -1; sideX = (p.x - mapX) * deltaX; }
            else             { stepX = 1;  sideX = (mapX + 1.0 - p.x) * deltaX; }
            if (rayDirY < 0) { stepY = -1; sideY = (p.y - mapY) * deltaY; }
            else             { stepY = 1;  sideY = (mapY + 1.0 - p.y) * deltaY; }

            boolean hit = false;
            int side = 0;         // 0 -> X-side, 1 -> Y-side
            int hitType = 0;      // 1 -> wall, 2 -> door
            double perpDist = 0.0;
            double wallX = 0.0;

            while (!hit) {
                if (sideX < sideY) { sideX += deltaX; mapX += stepX; side = 0; }
                else               { sideY += deltaY; mapY += stepY; side = 1; }

                int t = map.at(mapX, mapY);

                if (t == 1) {
                    // Wall: standard DDA boundary hit
                    hit = true; hitType = 1;
                    perpDist = (side == 0)
                            ? (mapX - p.x + (1 - stepX)/2.0) / (rayDirX == 0 ? 1e-6 : rayDirX)
                            : (mapY - p.y + (1 - stepY)/2.0) / (rayDirY == 0 ? 1e-6 : rayDirY);
                } else if (t == 2) {
                    // Door tile: treat as a SOLID boundary at tile edge (like a wall) unless fully open
                    double prog = map.getDoorProgress(mapX, mapY);
                    if (prog >= 1.0) {
                        // fully open -> not solid; keep marching
                        continue;
                    }
                    hit = true; hitType = 2;
                    perpDist = (side == 0)
                            ? (mapX - p.x + (1 - stepX)/2.0) / (rayDirX == 0 ? 1e-6 : rayDirX)
                            : (mapY - p.y + (1 - stepY)/2.0) / (rayDirY == 0 ? 1e-6 : rayDirY);
                }
                // else: empty -> continue
            }

            if (perpDist < 1e-6) perpDist = 1e-6;

            // Texture column across the hit surface
            wallX = (side == 0) ? p.y + perpDist * rayDirY
                    : p.x + perpDist * rayDirX;
            wallX -= Math.floor(wallX); // 0..1

            int lineHeight = (int) (viewH / perpDist);
            int drawStart = Math.max(0, -lineHeight / 2 + viewH / 2);
            int drawEnd   = Math.min(viewH - 1,  lineHeight / 2 + viewH / 2);

            Texture tex = (hitType == 2 && Assets.DOOR != null) ? Assets.DOOR : Assets.WALL;

            int texX = (int) (wallX * tex.w);
            // Keep flip consistent with wall faces for both walls and doors (since doors are at the edge)
            if (side == 0 && rayDirX > 0) texX = tex.w - texX - 1;
            if (side == 1 && rayDirY < 0) texX = tex.w - texX - 1;
            if (texX < 0) texX = 0; else if (texX >= tex.w) texX = tex.w - 1;

            double step = (double) tex.h / lineHeight;
            double texPos = (drawStart - viewH / 2.0 + lineHeight / 2.0) * step;

            for (int y = drawStart; y <= drawEnd; y++) {
                int texY = (int) texPos;
                if (texY < 0) texY = 0; else if (texY >= tex.h) texY = tex.h - 1;
                texPos += step;

                int color = tex.img.getRGB(texX, texY);

                // Simple side shading on Y-sides (like classic raycasters)
                if (side == 1 && hitType != 2) { // shade walls only; keep doors unshaded if you prefer
                    int a = (color >>> 24) & 0xFF;
                    int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
                    r = (r * 160) / 255; g = (g * 160) / 255; b = (b * 160) / 255;
                    color = (a << 24) | (r << 16) | (g << 8) | b;
                }

                fb.setRGB(x, y, color);
            }

            zBuffer[x] = perpDist;
        }
    }

    // ------------------------------------------------------------------------
    // Floor & Sky (clamped sampling + correct top row)
    // ------------------------------------------------------------------------
    private void drawFloorAndSky(BufferedImage fb, Player p, int viewH) {
        Texture floor = Assets.FLOOR, sky = Assets.SKY;
        if (floor == null || sky == null) return;

        double rx0 = p.dirX - p.planeX, ry0 = p.dirY - p.planeY;
        double rx1 = p.dirX + p.planeX, ry1 = p.dirY + p.planeY;

        for (int y = viewH / 2; y < viewH; y++) {
            double ppx = y - viewH / 2.0;
            if (ppx == 0) continue; // exact horizon

            double rowDist = (0.5 * viewH) / ppx;

            double stepX = rowDist * (rx1 - rx0) / W;
            double stepY = rowDist * (ry1 - ry0) / W;
            double fx = p.x + rowDist * rx0;
            double fy = p.y + rowDist * ry0;

            for (int x2 = 0; x2 < W; x2++) {
                int cellX = (int) fx, cellY = (int) fy;
                double tx = (fx - cellX) * floor.w;
                double ty = (fy - cellY) * floor.h;

                int u = (int) tx; if (u < 0) u = 0; else if (u >= floor.w) u = floor.w - 1;
                int v = (int) ty; if (v < 0) v = 0; else if (v >= floor.h) v = floor.h - 1;

                fb.setRGB(x2, y, floor.img.getRGB(u, v));

                int yTop = (viewH - 1) - y;
                if (yTop >= 0) {
                    int uu = (u >= sky.w) ? sky.w - 1 : u;
                    int vv = (v >= sky.h) ? sky.h - 1 : v;
                    fb.setRGB(x2, yTop, sky.img.getRGB(uu, vv));
                }

                fx += stepX; fy += stepY;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Sprites
    // ------------------------------------------------------------------------
    private void drawSprites(BufferedImage fb, Player p, List<SpriteEntity> sprites,
                             double timeSeconds, int viewH) {
        if (sprites == null || sprites.isEmpty()) return;

        sprites.sort(Comparator.comparingDouble(s -> -dist2(p.x, p.y, s.x, s.y)));

        for (SpriteEntity s : sprites) {
            double sx = s.x - p.x;
            double sy = s.y - p.y;

            double invDet = 1.0 / (p.planeX * p.dirY - p.dirX * p.planeY);
            double tx = invDet * ( p.dirY * sx - p.dirX * sy);
            double ty = invDet * (-p.planeY * sx + p.planeX * sy);
            if (ty <= 0.0001) continue;

            int screenX = (int) ((W / 2.0) * (1 + tx / ty));

            int spriteH = Math.abs((int) (viewH / ty));
            int drawStartY = Math.max(0, -spriteH / 2 + viewH / 2);
            int drawEndY   = Math.min(viewH - 1,  spriteH / 2 + viewH / 2);

            int spriteW = spriteH;
            int drawStartX = Math.max(0, -spriteW / 2 + screenX);
            int drawEndX   = Math.min(W - 1,  spriteW / 2 + screenX);

            BufferedImage img =
                    (s instanceof AnimatedSpriteEntity as) ? as.getFrameImage(p, timeSeconds)
                            : (s.texture != null ? s.texture.img : null);
            if (img == null) continue;

            int tw = img.getWidth(), th = img.getHeight();

            for (int stripe = drawStartX; stripe <= drawEndX; stripe++) {
                int texX = (int) ((stripe - (-spriteW / 2.0 + screenX)) * tw / (double) spriteW);
                if (texX < 0) texX = 0; else if (texX >= tw) texX = tw - 1;

                if (ty > 0 && stripe >= 0 && stripe < W && ty < zBuffer[stripe]) {
                    double step = (double) th / spriteH;
                    double tp = (drawStartY - viewH / 2.0 + spriteH / 2.0) * step;

                    for (int y = drawStartY; y <= drawEndY; y++) {
                        int texY = (int) tp; tp += step;
                        if (texY < 0) texY = 0; else if (texY >= th) texY = th - 1;

                        int argb = img.getRGB(texX, texY);
                        int a = (argb >>> 24) & 0xFF;
                        if (a < 10) continue;
                        fb.setRGB(stripe, y, argb);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Utils
    // ------------------------------------------------------------------------
    private static double dist2(double ax, double ay, double bx, double by) {
        double dx = ax - bx, dy = ay - by;
        return dx * dx + dy * dy;
    }
}
