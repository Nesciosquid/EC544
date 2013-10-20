package nodes.practice;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

import javax.swing.JFrame;
import javax.swing.JPanel;

/** Displays a window and delegates drawing to DrawGraphics. */
public class SimpleDraw extends JPanel implements Runnable {    
    private static final long serialVersionUID = -7469734580960165754L;
    private boolean animate = true;
    private final int FRAME_DELAY = 20; // 50 ms = 20 FPS
    public static final int WIDTH = 1300;
    public static final int HEIGHT = 700;
    private DrawGraphics draw;
    private CyclicBarrier myBarrier;

    
    public SimpleDraw(DrawGraphics drawer, CyclicBarrier barrier) {
        this.draw = drawer;
        this.myBarrier = barrier;
    }
    

    /** Paint callback from Swing. Draw graphics using g. */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Enable anti-aliasing for better looking graphics
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        draw.draw(g2);
    }

    /** Enables periodic repaint calls. */
    public synchronized void start() {
        animate = true;
    }

    /** Pauses animation. */
    public synchronized void stop() {
        animate = false;
    }

    private synchronized boolean animationEnabled() {
        return animate;
    }

    @Override
    public void run() {
        while (true) {
            if (animationEnabled()) {
                repaint();
            }

            try {
                Thread.sleep(FRAME_DELAY);
                myBarrier.await();
                }
             catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (BrokenBarrierException e){
            }
            
            
        }
    }

    public static void main(String args[]) {
        
        final CyclicBarrier cb = new CyclicBarrier(2, new Runnable(){
            public void run(){
            }
        });
        
        final SimpleDraw content = new SimpleDraw(new DrawGraphics(), cb);
        final TreeParser parser = new TreeParser(content.draw.getNode(), cb);

        JFrame frame = new JFrame("Graphics!");
       
        Color bgColor = Color.white;
        frame.setBackground(bgColor);
        content.setBackground(bgColor);
//        content.setSize(WIDTH, HEIGHT);
//        content.setMinimumSize(new Dimension(WIDTH, HEIGHT));
        content.setPreferredSize(new Dimension(WIDTH, HEIGHT));
//        frame.setSize(WIDTH, HEIGHT);
        frame.setContentPane(content);
        frame.setResizable(false);
        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
            public void windowDeiconified(WindowEvent e) { content.start(); }
            public void windowIconified(WindowEvent e) { content.stop(); }
        });

        new Thread(content).start();
        new Thread(parser).start();

        frame.setVisible(true);
    }
} 