package se.urmo.wolf.gfx;

import java.awt.image.BufferedImage;

public class Texture {
    public final BufferedImage img;
    public final int w, h;

    public Texture(BufferedImage img) {
        this.img = img;
        this.w = img.getWidth();
        this.h = img.getHeight();
    }

    public int sample(int x, int y) {
        x &= (w - 1);
        y &= (h - 1);
        return img.getRGB(x, y) & 0xFFFFFF;
    }
}