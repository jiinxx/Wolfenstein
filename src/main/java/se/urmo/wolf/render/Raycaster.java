package se.urmo.wolf.render;

import se.urmo.wolf.gfx.Assets;
import se.urmo.wolf.gfx.Texture;
import se.urmo.wolf.world.GameMap;
import se.urmo.wolf.entities.Player;
import se.urmo.wolf.entities.SpriteEntity;
import se.urmo.wolf.entities.AnimatedSpriteEntity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

public class Raycaster {
    private final int W, H;
    private final double[] zBuffer;

    public Raycaster(int width, int height) {
        this.W = width; this.H = height;
        this.zBuffer = new double[W];
    }

    public void render(BufferedImage fb, Graphics2D g2, GameMap map, Player p,
                       List<SpriteEntity> sprites, double timeSeconds,
                       BufferedImage weaponFrame, double bobX, double bobY) {

        final int hudH   = Math.max(1, (int) Math.round(H * 0.15));
        final int viewH  = H - hudH;
        final int hudTop = viewH;

        drawFloorCeiling(fb, p, viewH);
        drawWallsAndDoors(fb, map, p, viewH);
        drawSprites(fb, p, sprites, timeSeconds, viewH);

        drawHelpText(g2, p);
        drawMinimap(g2, map, p, sprites);

        drawWeaponHUD(g2, weaponFrame, bobX, bobY, viewH);

        drawBottomHUD(g2, hudTop, hudH, p);
    }

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
            else { stepX = 1; sideX = (mapX + 1.0 - p.x) * deltaX; }
            if (rayDirY < 0) { stepY = -1; sideY = (p.y - mapY) * deltaY; }
            else { stepY = 1; sideY = (mapY + 1.0 - p.y) * deltaY; }

            boolean hit = false;
            int side = 0, hitType = 0;
            double perpDist = 0.0, wallX = 0.0;

            while (!hit) {
                if (sideX < sideY) { sideX += deltaX; mapX += stepX; side = 0; }
                else { sideY += deltaY; mapY += stepY; side = 1; }

                int t = map.at(mapX, mapY);

                if (t == 1) {
                    hit = true; hitType = 1;
                    if (side == 0) perpDist = (mapX - p.x + (1 - stepX)/2.0) / (rayDirX == 0 ? 1e-6 : rayDirX);
                    else           perpDist = (mapY - p.y + (1 - stepY)/2.0) / (rayDirY == 0 ? 1e-6 : rayDirY);
                    if (perpDist < 1e-6) perpDist = 1e-6;

                    wallX = (side==0) ? p.y + perpDist * rayDirY
                            : p.x + perpDist * rayDirX;
                    wallX -= Math.floor(wallX);
                }
                else if (t == 2) {
                    double prog = map.getDoorProgress(mapX, mapY);
                    if (prog >= 1.0) continue;

                    boolean vertical = map.isDoorVertical(mapX, mapY);
                    int sign = map.getDoorSign(mapX, mapY);

                    if (!vertical) {
                        if (Math.abs(rayDirY) < 1e-9) continue;
                        double planeY = mapY + 0.5;
                        double tRay = (planeY - p.y) / rayDirY;
                        if (tRay <= 0) continue;
                        double xHit = p.x + tRay * rayDirX;

                        double x0 = mapX, xL, xR;
                        if (sign > 0) { xL = x0 + prog; xR = x0 + 1.0; }
                        else          { xL = x0;        xR = x0 + (1.0 - prog); }

                        if (xHit >= xL && xHit < xR) {
                            hit = true; hitType = 2; side = 1;
                            perpDist = tRay;
                            double span = Math.max(1e-6, (xR - xL));
                            wallX = (xHit - xL) / span;
                        }
                    } else {
                        if (Math.abs(rayDirX) < 1e-9) continue;
                        double planeX = mapX + 0.5;
                        double tRay = (planeX - p.x) / rayDirX;
                        if (tRay <= 0) continue;
                        double yHit = p.y + tRay * rayDirY;

                        double y0 = mapY, yL, yR;
                        if (sign > 0) { yL = y0 + prog; yR = y0 + 1.0; }
                        else          { yL = y0;        yR = y0 + (1.0 - prog); }

                        if (yHit >= yL && yHit < yR) {
                            hit = true; hitType = 2; side = 0;
                            perpDist = tRay;
                            double span = Math.max(1e-6, (yR - yL));
                            wallX = (yHit - yL) / span;
                        }
                    }
                }
            }

            int lineHeight = (int) (viewH / perpDist);
            int drawStart = Math.max(0, -lineHeight / 2 + viewH / 2);
            int drawEnd   = Math.min(viewH - 1,  lineHeight / 2 + viewH / 2);

            Texture tex = (hitType == 2) ? Assets.DOOR : Assets.WALL;
            int texX = (int) (wallX * tex.w);

            if (hitType != 2) {
                if (side == 0 && rayDirX > 0) texX = tex.w - texX - 1;
                if (side == 1 && rayDirY < 0) texX = tex.w - texX - 1;
            }

            double step = (double) tex.h / lineHeight;
            double texPos = (drawStart - viewH / 2.0 + lineHeight / 2.0) * step;

            for (int y = drawStart; y <= drawEnd; y++) {
                int texY = (int) texPos & (tex.h - 1);
                texPos += step;

                int color = tex.img.getRGB(texX, texY);

                if (side == 1 && hitType != 2) {
                    int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
                    r = (r * 160) / 255; g = (g * 160) / 255; b = (b * 160) / 255;
                    color = (r << 16) | (g << 8) | b;
                }
                fb.setRGB(x, y, color);
            }

            zBuffer[x] = perpDist;
        }
    }

    private void drawFloorCeiling(BufferedImage fb, Player p, int viewH) {
        Texture floor = Assets.FLOOR, sky = Assets.SKY;

        double rx0 = p.dirX - p.planeX, ry0 = p.dirY - p.planeY;
        double rx1 = p.dirX + p.planeX, ry1 = p.dirY + p.planeY;

        for (int y = viewH / 2 + 1; y < viewH; y++) {
            double ppx = y - viewH / 2.0;
            double rowDist = (0.5 * viewH) / (ppx == 0 ? 0.0001 : ppx);

            double stepX = rowDist * (rx1 - rx0) / W;
            double stepY = rowDist * (ry1 - ry0) / W;
            double fx = p.x + rowDist * rx0;
            double fy = p.y + rowDist * ry0;

            for (int x = 0; x < W; x++) {
                int cellX = (int) fx, cellY = (int) fy;
                double tx = (fx - cellX) * floor.w;
                double ty = (fy - cellY) * floor.h;

                int u = ((int) tx) & (floor.w - 1);
                int v = ((int) ty) & (floor.h - 1);

                fb.setRGB(x, y, floor.sample(u, v));

                int uu = u & (sky.w - 1), vv = v & (sky.h - 1);
                int yCeil = viewH - y;
                if (yCeil >= 0 && yCeil < viewH) {
                    fb.setRGB(x, yCeil, sky.sample(uu, vv));
                }

                fx += stepX; fy += stepY;
            }
        }
    }

    private void drawSprites(BufferedImage fb, Player p, List<SpriteEntity> sprites, double timeSeconds, int viewH) {
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

                if (ty > 0 && stripe >= 0 && stripe < W && ty < zBuffer[stripe]) {
                    double step = (double) th / spriteH;
                    double tp = (drawStartY - viewH / 2.0 + spriteH / 2.0) * step;

                    for (int y = drawStartY; y <= drawEndY; y++) {
                        int texY = (int) tp; tp += step;
                        if (texY < 0 || texY >= th) continue;

                        int argb = img.getRGB(Math.max(0, Math.min(tw - 1, texX)), texY);
                        int a = (argb >>> 24) & 0xFF;
                        if (a < 10) continue;
                        fb.setRGB(stripe, y, argb);
                    }
                }
            }
        }
    }

    private void drawHelpText(Graphics2D g2, Player p) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        g2.drawString("W/S: move  A/D: rotate  Shift: sprint  Enter: open door  Space: fire  Esc: quit", 10, 18);
        g2.drawString(String.format("Pos: (%.2f, %.2f)", p.x, p.y), 10, 36);
    }

    private void drawMinimap(Graphics2D g, GameMap map, Player p, List<SpriteEntity> sprites) {
        int cell = 10, pad = 10, x0 = pad, y0 = pad + 50;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x0 - 4, y0 - 4, map.w * cell + 8, map.h * cell + 8, 8, 8);

        for (int my = 0; my < map.h; my++) {
            for (int mx = 0; mx < map.w; mx++) {
                int t = map.at(mx, my);
                Color c;
                if (t == 1) c = new Color(60, 60, 60);
                else if (t == 2 && !map.isDoorOpen(mx, my)) c = new Color(40, 120, 200);
                else c = new Color(200, 200, 200);
                g.setColor(c);
                g.fillRect(x0 + mx * cell, y0 + my * cell, cell, cell);
            }
        }

        g.setColor(new Color(80, 160, 255));
        if (sprites != null) for (var s : sprites) {
            int sx = x0 + (int) (s.x * cell), sy = y0 + (int) (s.y * cell);
            g.fillOval(sx - 3, sy - 3, 6, 6);
        }

        int px = x0 + (int) (p.x * cell), py = y0 + (int) (p.y * cell);
        g.setColor(Color.RED);
        g.fillOval(px - 3, py - 3, 6, 6);
        g.drawLine(px, py, px + (int) (p.dirX * 12), py + (int) (p.dirY * 12));
    }

    private void drawWeaponHUD(Graphics2D g2, BufferedImage weaponFrame, double bobX, double bobY, int viewH) {
        if (weaponFrame == null) return;

        int imgW = weaponFrame.getWidth();
        int imgH = weaponFrame.getHeight();

        double scale = 2.0;
        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);

        int x = (int) ((W - drawW) / 2 + bobX);
        int y = (int) (viewH - drawH - 8 + bobY);

        g2.drawImage(weaponFrame, x, y, drawW, drawH, null);
    }

    private void drawBottomHUD(Graphics2D g2, int hudTop, int hudH, Player p) {
        Color bg = new Color(22, 28, 38, 235);
        Color fg = new Color(235, 240, 248);
        Color accent = new Color(70, 120, 200);

        g2.setColor(bg);
        g2.fillRect(0, hudTop, W, hudH);

        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawLine(0, hudTop, W, hudTop);

        final int sections = 6;
        int secW = W / sections;

        Font labelFont = new Font("Consolas", Font.PLAIN, 14);
        Font valueFont = new Font("Consolas", Font.BOLD, 20);
        g2.setFont(labelFont);

        String floor  = "1";
        String score  = "0";
        String lives  = (p != null) ? String.valueOf(p.getLives()) : "3";
        String health = (p != null) ? (p.getHealth() + "%") : "100%";
        String ammo   = "8";

        for (int i = 0; i < sections; i++) {
            if (i > 0) {
                g2.setColor(accent);
                int vx = i * secW;
                g2.drawLine(vx, hudTop + 6, vx, hudTop + hudH - 6);
            }
        }

        int labelY = hudTop + 18;
        int valueY = hudTop + hudH / 2 + 8;

        drawHudLabelValue(g2, "FLOOR",  floor,  0, secW, labelY, valueY, fg, labelFont, valueFont);
        drawHudLabelValue(g2, "SCORE",  score,  1, secW, labelY, valueY, fg, labelFont, valueFont);
        drawHudLabelValue(g2, "LIVES",  lives,  2, secW, labelY, valueY, fg, labelFont, valueFont);
        drawHudLabelValue(g2, "HEALTH", health, 3, secW, labelY, valueY, fg, labelFont, valueFont);
        drawHudLabelValue(g2, "AMMO",   ammo,   4, secW, labelY, valueY, fg, labelFont, valueFont);

        int ix = 5 * secW;
        int innerW = (5 == sections - 1) ? (W - ix) : secW;
        int cx = ix + innerW / 2;
        int cy = hudTop + hudH / 2 + 4;

        BufferedImage icon = Assets.HANDGUN_READY != null ? Assets.HANDGUN_READY : null;
        if (icon != null) {
            int iw = icon.getWidth();
            int ih = icon.getHeight();
            double maxH = hudH - 14.0;
            double scale = maxH / ih;
            int drawW = (int) Math.round(iw * scale);
            int drawH = (int) Math.round(ih * scale);
            int dx = cx - drawW / 2;
            int dy = cy - drawH / 2;
            g2.drawImage(icon, dx, dy, drawW, drawH, null);
        } else {
            g2.setColor(new Color(255, 255, 255, 60));
            int boxW = (int) (secW * 0.6);
            int boxH = (int) (hudH * 0.6);
            g2.drawRect(cx - boxW / 2, cy - boxH / 2, boxW, boxH);
        }
    }

    private void drawHudLabelValue(Graphics2D g2, String label, String value,
                                   int sectionIndex, int secW,
                                   int labelY, int valueY,
                                   Color fg, Font labelFont, Font valueFont) {
        int x0 = sectionIndex * secW;
        g2.setFont(labelFont);
        g2.setColor(new Color(220, 230, 245));
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, x0 + (secW - lw) / 2, labelY);

        g2.setFont(valueFont);
        g2.setColor(fg);
        int vw = g2.getFontMetrics().stringWidth(value);
        g2.drawString(value, x0 + (secW - vw) / 2, valueY);
    }

    private static double dist2(double ax, double ay, double bx, double by) {
        double dx = ax - bx, dy = ay - by;
        return dx * dx + dy * dy;
    }
}