package se.urmo.wolf.gfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class Assets {
    public static Texture WALL;
    public static Texture DOOR;
    public static Texture FLOOR;
    public static Texture SKY;

    public static BufferedImage HANDGUN_READY;
    public static BufferedImage HANDGUN_SHOT1;
    public static BufferedImage HANDGUN_SHOT2;

    public static DirectionalSprite GUARD_WALK;
    public static DirectionalSprite SS_WALK;

    private Assets() {}

    public static void init() {
        try {
            // walls.png: 64x64 tiles, 8x8 grid, 1px outer padding and 1px spacing
            BufferedImage wallsSheet = ImageIO.read(Assets.class.getResource("/textures/walls.png"));
            BufferedImage wallTile = sliceWithSpacing(wallsSheet, 0, 0, 64, 64, 1, 1);
            BufferedImage doorTile = sliceWithSpacing(wallsSheet, 0, 7, 64, 64, 1, 1);
            WALL = new Texture(wallTile);
            DOOR = new Texture(doorTile);

            // floor/sky
            BufferedImage floorImg = ImageIO.read(Assets.class.getResource("/textures/floor.png"));
            BufferedImage skyImg   = ImageIO.read(Assets.class.getResource("/textures/sky.png"));
            FLOOR = new Texture(floorImg);
            SKY   = new Texture(skyImg);

            // weapons.png: 128x128 frames, spacing=1, NO outer padding
            BufferedImage weapons = ImageIO.read(Assets.class.getResource("/textures/weapons.png"));
            HANDGUN_READY = sliceWithSpacing(weapons, 2, 7, 128, 128, 1, 0);
            HANDGUN_SHOT1 = sliceWithSpacing(weapons, 3, 7, 128, 128, 1, 0);
            HANDGUN_SHOT2 = sliceWithSpacing(weapons, 4, 7, 128, 128, 1, 0);

            // enemies: guard.png spacing=1; ss.png spacing=0
            BufferedImage guardSheet = ImageIO.read(Assets.class.getResource("/textures/guard.png"));
            GUARD_WALK = new DirectionalSprite(guardSheet, 128, 128, 7, 8, 1);

            BufferedImage ssSheet = ImageIO.read(Assets.class.getResource("/textures/ss.png"));
            SS_WALK = new DirectionalSprite(ssSheet, 128, 128, 7, 8, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage sliceWithSpacing(BufferedImage sheet, int col, int row,
                                                  int w, int h, int spacing, int outerPad) {
        int x = outerPad + col * (w + spacing);
        int y = outerPad + row * (h + spacing);
        if (x < 0 || y < 0 || x + w > sheet.getWidth() || y + h > sheet.getHeight()) {
            return new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        }
        return sheet.getSubimage(x, y, w, h);
    }
}