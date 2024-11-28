import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class Shapes {

    public static void makePyramid(Graphics2D g2d, BufferedImage image, int roundNum) {
        // list to hold the pyramid's triangles
        ArrayList<Triangle> triangleList = new ArrayList<>();

        // define the pyramid's triangular faces using vertices
        triangleList.add(new Triangle(
                new Vertex(100, 100, 100),  
                new Vertex(-100, -100, 100), 
                new Vertex(-100, 100, -100), 
                Color.white));               

        triangleList.add(new Triangle(
                new Vertex(100, 100, 100), 
                new Vertex(-100, -100, 100), 
                new Vertex(100, -100, -100),  
                Color.red));                 

        triangleList.add(new Triangle(
                new Vertex(-100, 100, -100), 
                new Vertex(100, -100, -100),  
                new Vertex(100, 100, 100), 
                Color.green));               

        triangleList.add(new Triangle(
                new Vertex(-100, 100, -100), 
                new Vertex(100, -100, -100),  
                new Vertex(-100, -100, 100), 
                Color.blue));              

        // subdivide triangles for smoothing based on roundNum
        for (int i = 0; i < roundNum; i++) {
            triangleList = round(triangleList);
        }

        // transformation matrices for horizontal and vertical rotation
        double hTheta = Math.toRadians(Viewer.horizontalSlider.getValue());
        Matrix horizontalTransform = new Matrix(new double[]{
                Math.cos(hTheta), 0, -Math.sin(hTheta),
                0, 1, 0,
                Math.sin(hTheta), 0, Math.cos(hTheta)
        });

        double vTheta = Math.toRadians(Viewer.verticalSlider.getValue());
        Matrix verticalTransform = new Matrix(new double[]{
                1, 0, 0,
                0, Math.cos(vTheta), -Math.sin(vTheta),
                0, Math.sin(vTheta), Math.cos(vTheta)
        });

        // combine transformations
        Matrix newMatrix = horizontalTransform.multiply(verticalTransform);

        // initialize z-buffer for depth management
        double[] zBuffer = new double[image.getWidth() * image.getHeight()];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);

        // render each triangle
        for (Triangle triangle : triangleList) {
            // transform vertices
            Vertex vertex_one = newMatrix.transform(triangle.vertex_one);
            Vertex vertex_two = newMatrix.transform(triangle.vertex_two);
            Vertex vertex_three = newMatrix.transform(triangle.vertex_three);

            // translate vertices to image coordinates
            vertex_one.x += (double) image.getWidth() / 2;
            vertex_one.y += (double) image.getHeight() / 2;
            vertex_two.x += (double) image.getWidth() / 2;
            vertex_two.y += (double) image.getHeight() / 2;
            vertex_three.x += (double) image.getWidth() / 2;
            vertex_three.y += (double) image.getHeight() / 2;

            // calculate normals for lighting/shading
            Vertex ab = new Vertex(vertex_two.x - vertex_one.x, vertex_two.y - vertex_one.y, vertex_two.z - vertex_one.z);
            Vertex ac = new Vertex(vertex_three.x - vertex_one.x, vertex_three.y - vertex_one.y, vertex_three.z - vertex_one.z);
            Vertex norm = new Vertex(
                    ab.y * ac.z - ab.z * ac.y,
                    ab.z * ac.x - ab.x * ac.z,
                    ab.x * ac.y - ab.y * ac.x
            );

            // normalize the normal vector
            double normLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
            norm.x /= normLength;
            norm.y /= normLength;
            norm.z /= normLength;

            // calculate the cosine angle for shading
            double cosAngle = Math.abs(norm.z);

            // determine bounding box for the triangle
            int minX = (int) Math.max(0, Math.ceil(Math.min(vertex_one.x, Math.min(vertex_two.x, vertex_three.x))));
            int maxX = (int) Math.min(image.getWidth() - 1, Math.floor(Math.max(vertex_one.x, Math.max(vertex_two.x, vertex_three.x))));
            int minY = (int) Math.max(0, Math.ceil(Math.min(vertex_one.y, Math.min(vertex_two.y, vertex_three.y))));
            int maxY = (int) Math.min(image.getHeight() - 1, Math.floor(Math.max(vertex_one.y, Math.max(vertex_two.y, vertex_three.y))));

            // rasterize the triangle
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    Vertex p = new Vertex(x, y, 0); // current pixel vertex

                    // check if the pixel is inside the triangle
                    boolean V1 = sameSide(vertex_one, vertex_two, vertex_three, p);
                    boolean V2 = sameSide(vertex_two, vertex_three, vertex_one, p);
                    boolean V3 = sameSide(vertex_three, vertex_one, vertex_two, p);

                    if (V3 && V2 && V1) {
                        // calculate depth for z-buffer
                        double depth = vertex_one.z + vertex_two.z + vertex_three.z;
                        int zIndex = y * image.getWidth() + x; // calculate index in the z-buffer

                        // check for depth test
                        if (zBuffer[zIndex] < depth) {
                            // set pixel color based on shading
                            image.setRGB(x, y, getShade(triangle.color, cosAngle).getRGB());
                            zBuffer[zIndex] = depth; // update z-buffer
                        }
                    }
                }
            }
        }

        // draw the final image
        g2d.drawImage(image, 0, 0, null);
    }

    private static boolean sameSide(Vertex A, Vertex B, Vertex C, Vertex p) {
        // create vectors from point A to B, A to C, and A to P
        Vertex V1V2 = new Vertex(B.x - A.x, B.y - A.y, B.z - A.z); 
        Vertex V1V3 = new Vertex(C.x - A.x, C.y - A.y, C.z - A.z); 
        Vertex V1P = new Vertex(p.x - A.x, p.y - A.y, p.z - A.z);   

        // calculate the cross products to determine the relative orientation
        double V1V2CrossV1V3 = V1V2.x * V1V3.y - V1V3.x * V1V2.y; 
        double V1V2CrossP = V1V2.x * V1P.y - V1P.x * V1V2.y;    

        // if the signs of the cross products are the same (or zero), then p is on the same side of line AB as C
        return V1V2CrossV1V3 * V1V2CrossP >= 0;
    }

    private static Color getShade(Color color, double shade) {
        // convert the RGB components to linear color space using gamma correction
        double redLinear = Math.pow(color.getRed(), 2.2) * shade; 
        double greenLinear = Math.pow(color.getGreen(), 2.2) * shade; 
        double blueLinear = Math.pow(color.getBlue(), 2.2) * shade;  

        // convert the linear color values back to gamma-corrected space
        int red = (int) Math.pow(redLinear, 1 / 2.2);   
        int green = (int) Math.pow(greenLinear, 1 / 2.2); 
        int blue = (int) Math.pow(blueLinear, 1 / 2.2);   

        // create and return a new Color object with the adjusted RGB values
        return new Color(red, green, blue);
    }

    private static ArrayList<Triangle> round(ArrayList<Triangle> triangles) {
        // this list will hold the subdivided triangles
        ArrayList<Triangle> result = new ArrayList<>();

        // loop through each triangle in the provided list
        for (Triangle triangle : triangles) {
            // calculate the midpoints of each edge of the triangle
            Vertex mid_one = new Vertex(
                    (triangle.vertex_one.x + triangle.vertex_two.x) / 2,
                    (triangle.vertex_one.y + triangle.vertex_two.y) / 2,
                    (triangle.vertex_one.z + triangle.vertex_two.z) / 2
            );

            Vertex mid_two = new Vertex(
                    (triangle.vertex_two.x + triangle.vertex_three.x) / 2,
                    (triangle.vertex_two.y + triangle.vertex_three.y) / 2,
                    (triangle.vertex_two.z + triangle.vertex_three.z) / 2
            );

            Vertex mid_three = new Vertex(
                    (triangle.vertex_one.x + triangle.vertex_three.x) / 2,
                    (triangle.vertex_one.y + triangle.vertex_three.y) / 2,
                    (triangle.vertex_one.z + triangle.vertex_three.z) / 2
            );

            // create 4 new triangles from the original triangle and its midpoints
            result.add(new Triangle(triangle.vertex_one, mid_one, mid_three, triangle.color)); 
            result.add(new Triangle(triangle.vertex_two, mid_one, mid_two, triangle.color));   
            result.add(new Triangle(triangle.vertex_three, mid_two, mid_three, triangle.color)); 
            result.add(new Triangle(mid_one, mid_two, mid_three, triangle.color));           
        }

        // normalize the vertices of the new triangles to unit length
        for (Triangle triangle : result) {
            for (Vertex v : new Vertex[]{triangle.vertex_one, triangle.vertex_two, triangle.vertex_three}) {
                // calculate the length of the vertex from the origin and normalize it
                double length = Math.sqrt((v.x * v.x + v.y * v.y + v.z * v.z) / 30000);
                v.x /= length; 
                v.y /= length; 
                v.z /= length; 
            }
        }

        // return the list of new triangles
        return result;
    }

    public static void makeCube(Graphics2D g2d, BufferedImage image) {
        ArrayList<Triangle> triangleList = new ArrayList<>();

        // define the 8 vertices of the cube
        Vertex v0 = new Vertex(-100, -100, -100); 
        Vertex v1 = new Vertex(100, -100, -100);  
        Vertex v2 = new Vertex(100, 100, -100);  
        Vertex v3 = new Vertex(-100, 100, -100);  
        Vertex v4 = new Vertex(-100, -100, 100);  
        Vertex v5 = new Vertex(100, -100, 100); 
        Vertex v6 = new Vertex(100, 100, 100);  
        Vertex v7 = new Vertex(-100, 100, 100);  

        // create triangles for each face of the cube
        triangleList.add(new Triangle(v4, v5, v6, Color.red));  
        triangleList.add(new Triangle(v4, v6, v7, Color.red));  

        triangleList.add(new Triangle(v0, v1, v2, Color.green)); 
        triangleList.add(new Triangle(v0, v2, v3, Color.green));  

        triangleList.add(new Triangle(v0, v3, v7, Color.blue));  
        triangleList.add(new Triangle(v0, v7, v4, Color.blue));   

        triangleList.add(new Triangle(v1, v2, v6, Color.yellow)); 
        triangleList.add(new Triangle(v1, v6, v5, Color.yellow));

        triangleList.add(new Triangle(v3, v2, v6, Color.cyan));
        triangleList.add(new Triangle(v3, v6, v7, Color.cyan));   

        triangleList.add(new Triangle(v0, v1, v5, Color.magenta)); 
        triangleList.add(new Triangle(v0, v5, v4, Color.magenta)); 

        // get rotation angles for transformations based on user input
        double hTheta = Math.toRadians(Viewer.horizontalSlider.getValue());
        Matrix horizontalTransform = new Matrix(new double[]{
                Math.cos(hTheta), 0, -Math.sin(hTheta),
                0, 1, 0,
                Math.sin(hTheta), 0, Math.cos(hTheta)
        });

        double vTheta = Math.toRadians(Viewer.verticalSlider.getValue());
        Matrix verticalTransform = new Matrix(new double[]{
                1, 0, 0,
                0, Math.cos(vTheta), -Math.sin(vTheta),
                0, Math.sin(vTheta), Math.cos(vTheta)
        });

        // combine horizontal and vertical transformations
        Matrix newMatrix = horizontalTransform.multiply(verticalTransform);

        // initialize Z-buffer for depth management
        double[] zBuffer = new double[image.getWidth() * image.getHeight()];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);

        // process each triangle to render them
        for (Triangle triangle : triangleList) {
            // transform the vertices of the triangle
            Vertex vertex_one = newMatrix.transform(triangle.vertex_one);
            Vertex vertex_two = newMatrix.transform(triangle.vertex_two);
            Vertex vertex_three = newMatrix.transform(triangle.vertex_three);

            // center the transformed vertices in the image
            vertex_one.x += (double) image.getWidth() / 2;
            vertex_one.y += (double) image.getHeight() / 2;
            vertex_two.x += (double) image.getWidth() / 2;
            vertex_two.y += (double) image.getHeight() / 2;
            vertex_three.x += (double) image.getWidth() / 2;
            vertex_three.y += (double) image.getHeight() / 2;

            // compute the normal vector for lighting calculations
            Vertex ab = new Vertex(vertex_two.x - vertex_one.x, vertex_two.y - vertex_one.y, vertex_two.z - vertex_one.z);
            Vertex ac = new Vertex(vertex_three.x - vertex_one.x, vertex_three.y - vertex_one.y, vertex_three.z - vertex_one.z);
            Vertex norm = new Vertex(
                    ab.y * ac.z - ab.z * ac.y,
                    ab.z * ac.x - ab.x * ac.z,
                    ab.x * ac.y - ab.y * ac.x
            );

            // normalize the normal vector
            double normLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
            norm.x /= normLength;
            norm.y /= normLength;
            norm.z /= normLength;

            // calculate the cosine angle for shading
            double cosAngle = Math.abs(norm.z);

            // calculate the bounding box of the triangle for rasterization
            int minX = (int) Math.max(0, Math.ceil(Math.min(vertex_one.x, Math.min(vertex_two.x, vertex_three.x))));
            int maxX = (int) Math.min(image.getWidth() - 1, Math.floor(Math.max(vertex_one.x, Math.max(vertex_two.x, vertex_three.x))));
            int minY = (int) Math.max(0, Math.ceil(Math.min(vertex_one.y, Math.min(vertex_two.y, vertex_three.y))));
            int maxY = (int) Math.min(image.getHeight() - 1, Math.floor(Math.max(vertex_one.y, Math.max(vertex_two.y, vertex_three.y))));

            // rasterize the triangle within its bounding box
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    Vertex p = new Vertex(x, y, 0);

                    // check if point p is inside the triangle
                    boolean V1 = sameSide(vertex_one, vertex_two, vertex_three, p);
                    boolean V2 = sameSide(vertex_two, vertex_three, vertex_one, p);
                    boolean V3 = sameSide(vertex_three, vertex_one, vertex_two, p);

                    // if the point is inside the triangle, compute depth and update the Z-buffer
                    if (V3 && V2 && V1) {
                        double depth = vertex_one.z + vertex_two.z + vertex_three.z;
                        int zIndex = y * image.getWidth() + x;
                        if (zBuffer[zIndex] < depth) {
                            // set pixel color based on shading
                            image.setRGB(x, y, getShade(triangle.color, cosAngle).getRGB());
                            zBuffer[zIndex] = depth; // update Z-buffer
                        }
                    }
                }
            }
        }

        // draw the rendered image to the Graphics context
        g2d.drawImage(image, 0, 0, null);
    }
}