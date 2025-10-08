package se.urmo.wolf.gfx;

import java.awt.image.BufferedImage;

public class DirectionalSprite {
    private final BufferedImage sheet;
    private final int frameW, frameH, rows, cols, spacing;
    private final BufferedImage[][] frames;

    public DirectionalSprite(BufferedImage sheet, int frameW, int frameH, int rows, int cols, int spacing) {
        this.sheet = sheet;
        this.frameW = frameW;
        this.frameH = frameH;
        this.rows = rows;
        this.cols = cols;
        this.spacing = spacing;
        this.frames = new BufferedImage[rows][cols];
        extract();
    }

    private void extract() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * (frameW + spacing);
                int y = r * (frameH + spacing);
                if (x + frameW <= sheet.getWidth() && y + frameH <= sheet.getHeight()) {
                    frames[r][c] = sheet.getSubimage(x, y, frameW, frameH);
                } else {
                    frames[r][c] = new BufferedImage(frameW, frameH, BufferedImage.TYPE_INT_ARGB);
                }
            }
        }
    }

    public BufferedImage getFrame(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return null;
        return frames[row][col];
    }

    // Convenience for old code
    public BufferedImage frame(int row, int col) {
        return getFrame(row, col);
    }

    public int getCols() {
        return cols;
    }
}