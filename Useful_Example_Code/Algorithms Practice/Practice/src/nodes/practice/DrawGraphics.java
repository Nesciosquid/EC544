package nodes.practice;

import java.util.ArrayList;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics;

public class DrawGraphics {
    DataNode node1;
    DataNode node2;
    DataNode node3;
    DataNode node4;
    DataNode node5;
    DataNode node6;
    BouncingBox box;
    
    ArrayList<DataNode> nodeList = new ArrayList<>();
    
    /** Initializes this class for drawing. */
    public DrawGraphics() {
        TreeBuilder builder1 = new TreeBuilder();
        node1 = new DataNode(650, 50, 0, 0);
        builder1.generateBreadthTree(node1, 50);
        builder1.assignPositions(node1, 0);

    }
    
    public DataNode getNode(){
        return node1;
    }

    /** Draw the contents of the window on surface. Called 20 times per second. */
    public void draw(Graphics surface) {
            node1.drawTree(surface);
            
            
            
  
    }
} 