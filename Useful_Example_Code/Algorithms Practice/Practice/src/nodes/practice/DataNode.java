/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nodes.practice;

import java.util.ArrayList;
import java.util.Random;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 *
 * @author Aaron Heuckroth
 */
public class DataNode {

    int x;
    int y;
    Color currentColor;
    Color inactiveColor = Color.RED;
    Color testingColor = Color.ORANGE;
    Color incorrectColor = Color.YELLOW;
    Color correctColor = Color.GREEN;
    ArrayList<DataNode> childNodes = new ArrayList<>();
    String myStatus = "inactive";
    int myID;
    int statusID;
    public int radius = 20;

    public DataNode(int startX, int startY, int identity, int status) {
        x = startX;
        y = startY;
        myID = identity;
        statusID = status;
    }

    public boolean isParent() {
        if (childNodes.size() > 0) {
            return true;
        } else //childNodes.size() <=0
        {
            return false;
        }
    }

    public void addChild(DataNode newNode) {
        childNodes.add(newNode);
    }
    
    public void remChild(DataNode newNode) {
        childNodes.remove(newNode);
    }

    public void setPosition(int xPos, int yPos) {
        x = xPos;
        y = yPos;
    }

    public void setID(int ID) {
        myID = ID;
    }

    private void updateColor() {
        if (statusID == 3) {
            currentColor = correctColor;
        } else if (statusID == 1) {
            currentColor = testingColor;
        } else if (statusID == 2) {
            currentColor = incorrectColor;
        } else {
            currentColor = inactiveColor;
        }
    }

    public void setStatus(int status) {
        statusID = status;
        updateColor();
    }

    public void cycleStatus() {
        if (statusID < 3) {
            statusID++;
        } else { //(node.statusID = 3)
            statusID = 0;
        }
    }
    
    private void drawTag(Graphics surface){
        surface.setColor(Color.BLACK);
        surface.fillOval(x-1-radius, y-7-radius, radius, radius);
        surface.setColor(Color.WHITE);
        surface.drawString("" + IDtoString(), x-18, y-13);
    }
    
    private void drawNode(Graphics surface){
        surface.setColor(currentColor);
        surface.fillOval(x-radius, y-radius, radius * 2, radius * 2);
        surface.setColor(Color.BLACK);
        ((Graphics2D) surface).setStroke(new BasicStroke(4.0f));
        surface.drawOval(x-radius, y-radius, radius * 2, radius * 2);
    }
    
    private String IDtoString()
    {
        if (myID < 10)
        {
            return "0" + myID;
        }
        else 
            return ""+myID;
    }
    public void draw(Graphics surface) {
        // Draw the object
        updateColor();
        drawNode(surface);
        drawTag(surface);
        }
    

    public void drawTree(Graphics surface) {
        if (isParent()) {
            for (int i =0; i < childNodes.size(); i++)
            {
                surface.setColor(Color.BLACK);
                ((Graphics2D) surface).setStroke(new BasicStroke(3.0f));
                surface.drawLine(x, y, childNodes.get(i).x, childNodes.get(i).y);
            }
        }
        draw(surface);
        DataNode childNode;
        
        if (isParent()) {
            for (int i = 0; i < childNodes.size(); i++) {
                childNode = childNodes.get(i);
                childNode.drawTree(surface);
            }
        }
    }
}
