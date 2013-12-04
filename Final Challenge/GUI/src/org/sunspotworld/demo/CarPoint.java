/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.demo;

/**
 *
 * @author Aaron Heuckroth
 */
public class CarPoint {

    public float time;
    public float RF;
    public float LF;
    public float RR;
    public float LR;
    public float RT;
    public float LT;
    public float distRight;
    public float distLeft;
    public float thetaRight;
    public float thetaLeft;
    public float theta;
    public float distance;
    public float turn;
    public float velocity;
    public float targetDist;
    public float startTurn;
    public float stopTurn;
    public float targetTheta;

    public CarPoint() {
    }

    public String toString() {
        String outputLine = time + "," + LF + "," + RF + "," + LR + "," + RR + "," + LT + "," + RT + "," + distRight + "," + distLeft
                + "," + thetaRight + "," + thetaLeft + "," + theta + "," + distance + "," + turn + "," + velocity
                + "," + targetDist + "," + startTurn + "," + stopTurn + "," + targetTheta;
        System.out.println(outputLine);
        return outputLine;
    }

    public void printPoint() {
        System.out.println("Time: " + time + ", RF: " + RF + ", LF:" + LF + ", RR: " + RR + ", LR: " + LR + ", RT: " + RT + ", LT: " + LT);
    }
}