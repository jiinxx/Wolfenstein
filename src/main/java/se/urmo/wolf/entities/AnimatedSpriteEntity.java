package se.urmo.wolf.entities;

import se.urmo.wolf.gfx.DirectionalSprite;

import java.awt.image.BufferedImage;

public abstract class AnimatedSpriteEntity extends SpriteEntity {
    // Animation config
    protected int walkRows = 1;   // how many rows used for walking loop (top rows)
    protected double fps = 8.0;   // walking frames per second
    protected boolean flipLR = true; // enable 8-direction selection (0..7)

    protected AnimatedSpriteEntity(double x, double y, DirectionalSprite ds) {
        super(x, y, ds);
    }
    protected AnimatedSpriteEntity(double x, double y, se.urmo.wolf.gfx.Texture tex) {
        super(x, y, tex);
    }

    // Subclasses provide their current facing vector for directional selection
    protected double facingX(Player p) { return 1; }
    protected double facingY(Player p) { return 0; }

    @Override
    public BufferedImage getFrameImage(Player p, double timeSeconds) {
        if (dirSprite == null) return super.getFrameImage(p, timeSeconds);

        // Determine direction octant from facing
        double fx = facingX(p), fy = facingY(p);
        double ang = Math.atan2(fy, fx); // -pi..pi
        if (ang < 0) ang += Math.PI * 2.0;
        // 8 directions around the circle
        int dir = (int)Math.round( (ang / (Math.PI * 2.0)) * 8.0 ) & 7;

        // Simple walk row selection: use first walkRows as animation loop.
        int framesPerRow = dirSprite.getCols();
        int row = Math.min(walkRows - 1, Math.max(0, (int)( (timeSeconds * fps) % Math.max(1, walkRows) )));
        // Column is direction
        int col = dir;

        BufferedImage f = dirSprite.getFrame(row, col);
        return f != null ? f : super.getFrameImage(p, timeSeconds);
    }
}