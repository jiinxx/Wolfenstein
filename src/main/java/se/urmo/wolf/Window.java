package se.urmo.wolf;

import se.urmo.wolf.core.Input;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.event.*;

public class Window {
    private final JFrame frame;
    private final Canvas canvas;
    private final BufferedImage framebuffer;
    private final Input input;

    public Window(int width, int height, String title) {
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        canvas = new Canvas();
        canvas.setSize(width, height);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        framebuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        input = new Input();
        canvas.addKeyListener(input);
        canvas.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { input.reset(); }
        });
        canvas.setFocusable(true);
        canvas.requestFocus();
    }

    public boolean isOpen() { return frame.isDisplayable(); }
    public void dispose() { frame.dispose(); }

    public void poll() {
        input.poll();
    }

    public BufferedImage getFramebuffer() { return framebuffer; }

    public Graphics2D getFrameGraphics() {
        return framebuffer.createGraphics();
    }

    public void present() {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) { canvas.createBufferStrategy(2); return; }
        Graphics g = bs.getDrawGraphics();
        g.drawImage(framebuffer, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
        g.dispose();
        bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

    public Input getInput() { return input; }
}
