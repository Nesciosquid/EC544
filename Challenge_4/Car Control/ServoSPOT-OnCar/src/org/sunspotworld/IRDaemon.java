/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.squawk.util.MathUtils;
import java.io.IOException;

/**
 *
 * @author Aaron Heuckroth
 */
public class IRDaemon {
    // IR sensors

    private IAnalogInput irRightFront;
    private IAnalogInput irLeftFront;
    private IAnalogInput irRightRear;
    private IAnalogInput irLeftRear;
    // Trig coefficients
    private double wallNear = 15.0;
    private double wallFar = 35.0;
    private double idealDistance = 19.0;
    private double closeDistance = 15.0;
    private double sensorDistance = 10.0;// in IR units
    private double confidenceDistance = 20.0; // in IR units
    private double confidenceHighCutoff = 1.00;
    private double confidenceCutoffStep = 0.30;
    private double straightTolerance = 5.0;
    private double confidenceThresh = .03;
    private double idealTheta = 0.0;
    private double[] recentThetas = new double[]{0.0, 0.0, 0.0};
    private double avgTheta;
    //Class data variables
    public double[] recentDists = new double[]{0.0, 0.0, 0.0};
    public double[] recentTargets = new double[]{0.0, 0.0, 0.0};
    public double avgTarget;
    public double avgDist;
    public double thetaLeft; // angle of car in radians
    public double thetaRight; // > 0, turned right; < 0, turned left
    public double distanceLF; // distance left-front sensor to wall in IR units
    public double distanceRF;
    public double distanceLR;
    public double distanceRR;
    public double distanceAvgL;
    public double distanceAvgR;
    public double readingLF; // in IR units
    public double readingRF;
    public double readingLR;
    public double readingRR;
    public double confidenceLF; // >1 implies reading is very good, <1 implies reading is less good, << 1 implies reading is bad
    public double confidenceRF;
    public double confidenceLR;
    public double confidenceRR;
    public double confidenceAvgL;
    public double confidenceAvgR;
    public int turnSuggest = 1500;
    public int speedSuggest = 1500;
    int turnRightMax = 1000;
    int turnLeftMax = 2000;
    //double turnDistance = 30.0;
    double maxTurnDistance = 20;
    double maxTurnAngle = 20.0;
    double approachAngle = 20.0;
    double maxTurnFactor = 1.0;
    double maxDistFactor = 1.0;
    double maxAngleFactor = 1.0;
    double maxConfidenceAngle = 45;

    public double toRadians(double theta_deg) {
        return theta_deg * .0174532925;
    }

    public double calcIdealTheta(double set_distance, double current_distance) {//29,30
        double distanceDifference = current_distance - set_distance;//31
        double distanceRatio = distanceDifference / maxTurnDistance;
        if (distanceRatio > maxDistFactor) {
            distanceRatio = maxDistFactor;
        } else if (distanceRatio < -maxDistFactor) {
            distanceRatio = -maxDistFactor;
        }
        double thetaMagnitude = toRadians(approachAngle * distanceRatio); // convert to radians for consistency
        idealTheta = thetaMagnitude;
        return thetaMagnitude;
    }

    public int calcTurn(double set_theta_rad, double current_theta_rad) {
        double thetaDiff = toDegrees(set_theta_rad) - toDegrees(current_theta_rad);
        double thetaRatio = thetaDiff / maxTurnAngle;
        if (thetaRatio > maxAngleFactor) {
            thetaRatio = maxAngleFactor;
        } else if (thetaRatio < -maxAngleFactor) {
            thetaRatio = -maxAngleFactor;
        }
        int turn = (int) (1500 - (500 * thetaRatio));
        return turn;
    }

    public int calcIdealTurn(double set_distance, double current_distance, double current_theta) {
        double set_theta = calcIdealTheta(set_distance, current_distance);
        return calcTurn(set_theta, current_theta);
    }

    public void storeTarget(double new_theta) {
        double[] tempTargets = recentTargets;
        double targetSum = 0.0;

        for (int i = 0; i < recentTargets.length; i++) {
            if (i == 0) {
                recentTargets[i] = new_theta;
            } else {
                recentTargets[i] = tempTargets[i - 1];
            }
            targetSum += recentTargets[i];
        }
        avgTarget = (targetSum / recentTargets.length);

    }

    private void storeDistance(double new_dist) {
        double[] tempDists = recentDists;
        double distSum = 0.0;
        if (new_dist < 0) { // new dist is a left value
            for (int i = 0; i < recentDists.length; i++) {
                if (i == 0) {
                    recentDists[i] = new_dist;
                } else {
                    if (tempDists[i - 1] < 0) { // old dist is a left value
                        recentDists[i] = tempDists[i - 1]; // add as next element 
                    } else {
                        recentDists[i] = new_dist; // re-add new element
                    }
                }
                distSum += recentDists[i];

            }
        } else {
            for (int i = 0; i < recentDists.length; i++) {
                if (i == 0) {
                    recentDists[i] = new_dist;
                } else {
                    if (tempDists[i - 1] < 0) {
                        recentDists[i] = new_dist;
                    } else {
                        recentDists[i] = tempDists[i - 1];
                    }
                }
                distSum += recentDists[i];
            }
        }
        avgDist = (distSum / recentDists.length);
    }

    private void storeTheta(double new_theta) {
        double[] tempThetas = recentThetas;
        double thetaSum = 0.0;
        for (int i = 0; i < recentThetas.length; i++) {
            if (i == 0) {
                recentThetas[i] = new_theta;
            } else {
                recentThetas[i] = tempThetas[i - 1];
            }
            thetaSum += recentThetas[i];
        }
        avgTheta = (thetaSum / recentThetas.length);

    }

    public int calcTurnFromDistance(double current_distance, double desired_distance) {
        //System.out.println(current_distance + " away, want to be " + desired_distance);
        int turn;
        double distanceDifference = Math.abs(Math.abs(current_distance) - Math.abs(desired_distance));
        //System.out.println("Distance Difference: " + distanceDifference);
        double turnRatio = distanceDifference / maxTurnDistance;
        double turnFactor = turnRatio * turnRatio; // squared, so that larger difference == exponentially larger turn

        //System.out.println("Turn factor: " + turnFactor);
        if (turnFactor > maxTurnFactor) {
            turnFactor = maxTurnFactor; // max is 2.0
            //System.out.println("Turn factor clipped to: " + turnFactor );
        }
        turn = 50 + (int) (450 * turnFactor); // 
        //System.out.println("Calculated turn:" + turn);
        return turn;
    }

    public int calcTurnFromAngle(double current_angle_rad, double desired_angle_rad) {
        double currentAngle = Math.abs(toDegrees(current_angle_rad));
        double desiredAngle = Math.abs(toDegrees(desired_angle_rad));
        //System.out.println(currentAngle + " is current angle, want to be " + desiredAngle);

        int turn;
        double angleDifference = Math.abs(currentAngle - desiredAngle);
        //       System.out.println("Angle Difference: " + angleDifference);

        double turnRatio = (angleDifference / maxTurnAngle);
        double turnFactor = turnRatio * turnRatio; // squared, so that larger difference == exponentially larger turn

        //System.out.println("Turn factor: " + turnFactor);
        if (turnFactor > maxTurnFactor) {
            turnFactor = maxTurnFactor;
            //              System.out.println("Turn factor clipped to: " + turnFactor );

        }
        turn = 50 + (int) (450 * turnFactor);
        //    System.out.println("Calculated turn:" + turn);
        System.out.println("Calculated ideal turn of: " + turn);
        return turn;
    }

    public IRDaemon() { // for calculations only
    }

    public IRDaemon(IAnalogInput RF, IAnalogInput LF, IAnalogInput RR, IAnalogInput LR) {
        irRightFront = RF;
        irLeftFront = LF;
        irRightRear = RR;
        irLeftRear = LR;
    }

    public IRDaemon(IAnalogInput RF, IAnalogInput LF, IAnalogInput RR, IAnalogInput LR, double distance, double conf) {
        irRightFront = RF;
        irLeftFront = LF;
        irRightRear = RR;
        irLeftRear = LR;
        sensorDistance = distance;
        confidenceDistance = conf;
    }

    public void update() {
        readSensors();
        updateAngles();
        updateDistance();
        updateConfidence();
    }

    private double getDistance(IAnalogInput analog) {
        double volts = 0;
        try {
            volts = analog.getVoltage();
        } catch (IOException e) {
            System.err.println(e);
        }
        return 18.67 / (volts + 0.167);
    }

    private double toDegrees(double thetaRad) {
        return thetaRad * 57.2957795;
    }

    private void readSensors() {
        readingLF = getDistance(irLeftFront);
        readingRF = getDistance(irRightFront);
        readingLR = getDistance(irLeftRear);
        readingRR = getDistance(irRightRear);
    }

    private void updateAngles() {
        thetaRight = calcAngle(readingRF, readingRR);
        thetaLeft = -calcAngle(readingLF, readingLR);
    }

    private void updateDistance() {
        distanceLF = calcDistance(readingLF, thetaLeft);
        distanceLR = calcDistance(readingLR, thetaLeft);
        distanceRF = calcDistance(readingRF, thetaRight);
        distanceRR = calcDistance(readingRR, thetaRight);
        distanceAvgL = (distanceLF + distanceLR) / 2;
        distanceAvgR = (distanceRF + distanceRR) / 2;
    }

    public void report() {
        System.out.println("--------");
        System.out.println("Theta Left (deg): " + toDegrees(thetaLeft));
        System.out.println("Theta Right (deg): " + toDegrees(thetaRight));
        System.out.println("Theta average: " + toDegrees(avgTheta));
        System.out.println("Distance from left wall: " + -distanceAvgL);
        System.out.println("Distance from right wall: " + distanceAvgR);
        System.out.println("Confidence in left: " + confidenceAvgL);
        System.out.println("Confidence in right: " + confidenceAvgR);
        //System.out.println("Estimated hall size:" + (distanceAvgL + distanceAvgR));
        //System.out.println("Direction you should go: " + pickDirection());
        System.out.println("Average distance: " + avgDist);
        //System.out.println("Right confidence difference: " + Math.abs(confidenceRF - confidenceRR));
        //System.out.println("Left confidence difference: " + Math.abs(confidenceLF - confidenceLR));
        System.out.println("Calculated target theta: " + toDegrees(idealTheta));
        System.out.println("Calculated target turn: " + turnSuggest);
        System.out.println("--------");

    }

    private int calcTrust(double conf1, double conf2) {
        
        double step4 = confidenceHighCutoff;
        double step3 = step4 - confidenceCutoffStep;
        double step2 = step3 - confidenceCutoffStep;
        double step1 = step2 - confidenceCutoffStep;
                System.out.println("Step4, Step3, Step2, Step1: " + step4 + ", " + step3 + ", " + step2 + ", " + step1);
        int trust;
        double lesser;
        if (conf1 < conf2) {
            lesser = conf1;
        } else {
            lesser = conf2;
        }
        if (lesser > step4) {
            return 4;
        } else if (lesser > step3) {
            return 3;
        } else if (lesser > step2) {
            return 2;
        } else if (lesser > step1) {
            return 1;
        } else// (lesser <= (cut - (step*3))) {
        {
            return 0;
        }
    }

    private int leftTrust() {
        return calcTrust(confidenceLF, confidenceLR);
    }

    private int rightTrust() {
        return calcTrust(confidenceRF, confidenceRR);
    }

    private boolean isCloseToWall(double distance) {
        if ((Math.abs(distance) < wallNear)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isFarFromWall(double distance) {
        if ((Math.abs(distance) > wallNear)) {
            return true;
        } else {
            return false;
        }


    }

    private boolean isStraight(double theta_rad) {
        double theta = toDegrees(theta_rad);
        if (Math.abs(theta) <= straightTolerance) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isTurnedLeft(double theta_rad) {
        double theta = toDegrees(theta_rad);
        if (theta >= 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isTurnedRight(double theta_rad) {
        double theta = toDegrees(theta_rad);
        if (theta <= 0) {
            return false;
        } else {
            return true;
        }
    }
    
    public boolean isCloseEnough(double confidence1, double confidence2){
        double diff = Math.abs(confidence1 - confidence2);
        if (diff < confidenceThresh)
        {
            return true;
        }
        else return false;
    }

    public String pickDirection() {

        boolean ignoreDistance = false;
        double theta;
        double distance;
        double distance_diff = 0.0;
        int L = leftTrust();
        System.out.println("Left trust: " + L);
        int R = rightTrust();
        System.out.println("Right trust: " + R);
        if (L == 0 && R == 0) {
            //turnSuggest = calcTurn(avgTarget, avgTheta);
            return "unknown";
        } 
        else if (L > R) {
            storeTheta(thetaLeft);
            storeDistance(-distanceAvgL);
                        if (R == 0){
            storeTarget(calcIdealTheta(-closeDistance, avgDist));
            }
            else {
            storeTarget(calcIdealTheta(-idealDistance, avgDist));
            }
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust left";
        } else if ( R > L) {
            storeTheta(thetaRight);
            storeDistance(distanceAvgR);
            if (L == 0){
            storeTarget(calcIdealTheta(closeDistance, avgDist));
            }
            else {
            storeTarget(calcIdealTheta(idealDistance, avgDist));
            }
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust right";
        } else if (L == 0 && R == 0)
        {
            //turnSuggest = calcTurn(avgTarget, avgTheta);
            return "unknown";
        }
        else {
            if (confidenceAvgL > confidenceAvgR || isCloseEnough(confidenceAvgL, confidenceAvgR)){
                storeTheta(thetaLeft);
                storeDistance(-distanceAvgR);
                storeTarget(calcIdealTheta(-idealDistance, avgDist));
                turnSuggest = calcTurn(avgTarget, avgTheta);
                return "equal trust, use left";
            }
            else {
            storeTheta(thetaRight);
            storeDistance(distanceAvgR);
            storeTarget(calcIdealTheta(idealDistance, avgDist));
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "equal trust, use right";
            }
                
        }
    }

        
        /*
        if ((L - R) >= 2) {
            storeTheta(thetaLeft);
            storeDistance(-distanceAvgL);
            storeTarget(calcIdealTheta(-idealDistance, avgDist));
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust left";
        } else if ((R - L) >= 2) {
            storeTheta(thetaRight);
            storeDistance(distanceAvgR);
            storeTarget(calcIdealTheta(idealDistance, avgDist));
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust right";
        } else if (R == 0 && L >= 1) {
            //return "slight-left";
            storeTheta(thetaLeft);
            storeDistance(-distanceAvgL);
            storeTarget(calcIdealTheta(-closeDistance, avgDist));
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust left";

        } else if (L == 0 && R >= 1) {
            //return "slight-right";
            storeTheta(thetaRight);
            storeDistance(distanceAvgR);
            storeTarget(calcIdealTheta(closeDistance, avgDist));
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust right";

        } else if (Math.abs(L - R) < 2) {
            storeTheta((thetaRight + thetaLeft) / 2);
            if (confidenceAvgL > confidenceAvgR) {
                storeDistance(-distanceAvgL);
                storeTarget(calcIdealTheta(-idealDistance, avgDist));
                turnSuggest = calcTurn(avgTarget, avgTheta);
                return "Trusting both, left wall closer";
            } else {
                storeDistance(distanceAvgR);
                storeTarget(calcIdealTheta(idealDistance, avgDist));
                turnSuggest = calcTurn(avgTarget, avgTheta);
                return "Trusting both, right wall closer";
            }
        } else {
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "unknown";
        }
    }*/
    private boolean isTooBig(double theta_rad){
        theta_rad = toDegrees(theta_rad);
        if (theta_rad > maxConfidenceAngle){
            return true;
        }
        else
            return false;
    
    }
    
    private boolean isTooFarOff(double theta_rad){
        theta_rad = toDegrees(theta_rad);
        if (Math.abs(theta_rad - avgTheta) > 40)
        {
            return true;
        }
        else 
            return false;
    }
    
    private void updateConfidence() {
        confidenceLF = calcConfidence(readingLF);
        confidenceLR = calcConfidence(readingLR);
        confidenceRF = calcConfidence(readingRF);
        confidenceRR = calcConfidence(readingRR);
        confidenceAvgL = (confidenceLF + confidenceLR) / 2;
        confidenceAvgR = (confidenceRF + confidenceRR) / 2;
        if (isTooBig(thetaRight))
        {
            confidenceAvgR = 0.0;
            confidenceRF = 0.0;
            confidenceRR = 0.0;
        }
        else if (isTooBig(thetaLeft)){
            confidenceAvgL = 0.0;
            confidenceLF = 0.0;
            confidenceLR = 0.0;
        }
        /*else {

        if (confidenceAvgL > confidenceAvgR && isTooFarOff(thetaRight)){
            confidenceAvgR = 0.0;
            confidenceRF = 0.0;
            confidenceRR = 0.0;
        }
        else if (!isTooFarOff(thetaRight) && confidenceAvgR > confidenceAvgL){
            confidenceAvgL = 0.0;
            confidenceLF = 0.0;
            confidenceLR = 0.0;
        }
    }*/
        
    }
        // sanity check

    /*private double calcConfidence(double reading) {
     double new_confidence = (confidenceDistance / reading);
     return new_confidence * new_confidence; // Where the Hell is our Math.pow(double, int)?
     }*/
    private double calcConfidence(double reading) {
        return confidenceDistance / reading;
    }

    private double calcDistance(double reading, double theta) {
        return Math.cos(theta) * reading;
    }

    private double calcAngle(double readingFront, double readingBack) {
        return MathUtils.atan2((readingBack - readingFront), sensorDistance);
    }

    private double getConfidence(double reading) {
        return reading;
    }
}
