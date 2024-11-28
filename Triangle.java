import java.awt.*;

public class Triangle {
    public Vertex vertex_one; 
    public Vertex vertex_two;   
    public Vertex vertex_three; 
    public Color color;         

    public Triangle(Vertex vertex_one, Vertex vertex_two, Vertex vertex_three, Color color) {
        this.vertex_one = vertex_one;    
        this.vertex_two = vertex_two;     
        this.vertex_three = vertex_three;
        this.color = color;             
    }
}
