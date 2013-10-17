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
 * @author Kirrei
 */
public class TreeBuilder {

    int xRawOffset = 300;
    double xReduceMultiplier = .5;
    int yOffset = 100;
    int xMax = 1300;
    int xMin = 0;
    int yMax = 700;

    public void assignPositions(DataNode node, double levelCounter) {
        DataNode currentChild;
        int xPosDefault = node.x;
        int yPosDefault = node.y;
        int xOffset = (int) Math.round(xRawOffset * Math.pow(xReduceMultiplier, levelCounter));

        if (node.isParent()) {
            if (node.childNodes.size() > 2) {
                System.out.println("I am too stupid to place more than two children per parent node!");
            } else {
                for (int i = 0; i < node.childNodes.size(); i++) {
                    currentChild = node.childNodes.get(i);
                    if (i == 0) {
                        currentChild.setPosition((xPosDefault - xOffset), (yPosDefault + yOffset));
                    } else //(i == 1)
                    {
                        currentChild.setPosition((xPosDefault + xOffset), (yPosDefault + yOffset));
                    }

                    if (currentChild.childNodes.size() > 0) {
                        assignPositions(currentChild, levelCounter + 1);
                    }
                }
            }
        } else {
            System.out.println("Parent node has no children!");
        }
    }
    
        public int generateRandom(int num){
            Random nums = new Random();
            return nums.nextInt(num);
        }
    
        public void generateBreadthTree(DataNode initialNode, int newNodes){
        int counter = 0;
        initialNode.setID(counter);
        counter++;
        int maxNodes = newNodes;
        int childrenPerNode = 2;
        ArrayList<DataNode> nodeQueue = new ArrayList<>();
        nodeQueue.add(initialNode);
        DataNode currentNode;
        DataNode newNode;
        
        if (newNodes <= 0) {         
            System.out.println("Cannot create tree with less than one node!");
        }    
   
        else {
            nodeQueue.add(initialNode);
            while (counter <= maxNodes) {
                currentNode = nodeQueue.get(0);
                if (generateRandom(8) < 7) {
                    if (currentNode.childNodes.size() < childrenPerNode) {
                        newNode = new DataNode(currentNode.x, currentNode.y, counter, 0);
                        counter++;
                        currentNode.addChild(newNode);
                        System.out.println("adding " + newNode + "as child of " + currentNode + "with status" + newNode.statusID);
                        assignPositions(initialNode, 0);
                        if (newNode.y > yMax || newNode.x > xMax || newNode.x < xMin)
                        {
                            currentNode.remChild(newNode);
                            counter--;
                            maxNodes--;
                            if (nodeQueue.size() > 1){
                                nodeQueue.remove(currentNode);
                            }
                        }
                        else {
                        nodeQueue.add(newNode);
                        }
                    } else { //(currentNode.childNodes.size() = maxNodes) 
                        System.out.println("removing " + currentNode + "from queue");
                        nodeQueue.remove(currentNode);
                    }
                } else {
                    if (nodeQueue.size() > 1){
                    nodeQueue.remove(currentNode);
                    }
                }
                assignPositions(initialNode, 0);
            }
        }
    }
}
