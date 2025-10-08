package se.urmo.wolf.entities;

import se.urmo.wolf.gfx.Texture;
import se.urmo.wolf.gfx.DirectionalSprite;
import se.urmo.wolf.entities.Player;

import java.awt.image.BufferedImage;

public class SpriteEntity {
    public double x, y;
    public Texture texture;
    public DirectionalSprite dirSprite;

    public SpriteEntity(double x, double y, Texture texture) {
        this.x = x; this.y = y; this.texture = texture;
    }

    public SpriteEntity(double x, double y, DirectionalSprite ds) {
        this.x = x; this.y = y; this.dirSprite = ds;
    }

    public BufferedImage getFrameImage(Player p, double timeSeconds) {
        return texture != null ? texture.img : null;
    }
}