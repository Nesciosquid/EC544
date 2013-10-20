/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nodes.practice;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

/**
 *
 * @author Aaron Heuckroth
 */
public class TreeParser implements Runnable {

    DataNode myNode;
    boolean go;
    private CyclicBarrier myBarrier;

    public TreeParser(DataNode node, CyclicBarrier barrier) {
        myNode = node;
        myBarrier = barrier;
        go = true;
    }

    private void cycleStatuses(DataNode node) {
        ArrayList<DataNode> queue = new ArrayList();
        DataNode currentNode;
        queue.add(node);
        while (queue.size() > 0) {
            //currentNode = queue.get(0); // for BREADTH
            currentNode = queue.get(queue.size() - 1); // for BREADTH
            currentNode.cycleStatus();
            try {
                myBarrier.await();
            } catch (InterruptedException ex) {
                System.out.println("Doublecrap!");
            } catch (BrokenBarrierException e) {
            }
            queue.remove(currentNode);
            if (currentNode.isParent()) {
                for (int i = 0; i < currentNode.childNodes.size(); i++) {
                    queue.add(currentNode.childNodes.get(i));
                }
            }
        }
    }

    public void run() {
        while (go) {
            cycleStatuses(myNode);

        }
    }
}
