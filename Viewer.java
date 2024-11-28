import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Viewer {
    public static JFrame frame = new JFrame();
    public static JSlider horizontalSlider = new JSlider(0, 360, 180);
    public static JSlider verticalSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
    public static String currentShape = "";
    public static double zoomFactor = 1.0; 

    public static void main(String[] args) {
        frame.setSize(500, 500);
        frame.add(horizontalSlider, BorderLayout.SOUTH);
        frame.add(verticalSlider, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton pyramidButton = new JButton("Pyramid");
        JButton sphereButton = new JButton("Sphere");
        JButton cubeButton = new JButton("Cube");

        buttonPanel.add(pyramidButton);
        buttonPanel.add(sphereButton);
        buttonPanel.add(cubeButton);

        frame.add(buttonPanel, BorderLayout.NORTH);

        JPanel render = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.black);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // center the rendering on the panel
                g2d.translate(getWidth() / 2, getHeight() / 2);  
                g2d.scale(zoomFactor, zoomFactor);
                g2d.translate(-getWidth() / 2, -getHeight() / 2); 

                BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                switch (currentShape) {
                    case "pyramid":
                        Shapes.makePyramid(g2d, image, 0);
                        break;
                    case "sphere":
                        Shapes.makePyramid(g2d, image, 4);
                        break;
                    case "cube":
                        Shapes.makeCube(g2d, image);
                }
            }
        };

        // buttons to switch between shapes
        cubeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentShape = "cube";  // switch to cube rendering
                render.repaint();
            }
        });

        pyramidButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentShape = "pyramid";  // switch to pyramid rendering
                render.repaint();
            }
        });

        sphereButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentShape = "sphere";  // switch to sphere rendering
                render.repaint();
            }
        });

        // variables to track mouse dragging for rotation
        final int[] initialX = new int[1];
        final int[] initialY = new int[1];
        final int[] lastHorizontal = {0};
        final int[] lastVertical = {0};
        final boolean[] dragging = {false};

        // handle events for rotation (dragging mouse) and zooming
        render.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                double y = (double) 180 / render.getHeight();
                double x = (double) 180 / render.getWidth();

                if (!dragging[0]) {
                    initialX[0] = e.getX();
                    initialY[0] = e.getY();
                    dragging[0] = true;
                }

                horizontalSlider.setValue(lastHorizontal[0] + (int) (-(e.getX() - initialX[0]) * x));
                verticalSlider.setValue(lastVertical[0] + (int) (-(e.getY() - initialY[0]) * y));

                render.repaint();
            }
        });

        render.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragging[0] = false;
            }

            public void mouseReleased(MouseEvent e) {
                lastHorizontal[0] = horizontalSlider.getValue();
                lastVertical[0] = verticalSlider.getValue();
                dragging[0] = false;
            }
        });

        render.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                double scaleFactor = 1.1; // 10% scale

                // zoom in or out based on the scroll direction
                if (notches < 0) {
                    zoomFactor *= scaleFactor;
                } else {
                    zoomFactor /= scaleFactor; 
                }

                render.repaint(); 
            }
        });

        horizontalSlider.addChangeListener(event -> render.repaint());
        verticalSlider.addChangeListener(event -> render.repaint());

        frame.add(render);
        frame.setVisible(true);
    }
}
