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
    private double idealDistance = 20.0;
    private double sensorDistance = 10.0;// in IR units
    private double confidenceDistance = 10.0; // in IR units
    private double confidenceHighCutoff = 0.60;
    private double confidenceCutoffStep = 0.15;
    private double straightTolerance = 5.0;
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
    double maxTurnDistance = 10.0;
    double maxTurnAngle = 20.0;
    double approachAngle = 20.0;
    double maxTurnFactor = 1.0;
    double maxDistFactor = 1.0;
    double maxAngleFactor = 1.0;

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
        System.out.println("Distance from left wall: " + -distanceAvgL);
        System.out.println("Distance from right wall: " + distanceAvgR);
        System.out.println("Confidence in left: " + confidenceAvgL);
        System.out.println("Confidence in right: " + confidenceAvgR);
        System.out.println("Estimated hall size:" + (distanceAvgL + distanceAvgR));
        System.out.println("Direction you should go: " + pickDirection());
        System.out.println("Average distance: " + avgDist);
        System.out.println("Right confidence difference: " + Math.abs(confidenceRF - confidenceRR));
        System.out.println("Left confidence difference: " + Math.abs(confidenceLF - confidenceLR));
        System.out.println("Calculated target theta: " + idealTheta);
        System.out.println("Calculated target turn: " + turnSuggest);
        System.out.println("--------");

    }

    private int calcTrust(double conf1, double conf2) {
        double step = confidenceCutoffStep;
        double cut = confidenceHighCutoff;
        int trust;
        double lesser;
        if (conf1 < conf2) {
            lesser = conf1;
        } else {
            lesser = conf2;
        }

        if (lesser > cut) {
            return 4;
        } else if (lesser <= (cut) && lesser > (cut - (step * 1))) {
            return 3;
        } else if (lesser <= (cut - (step * 1)) && lesser > (cut - (step * 2))) {
            return 2;
        } else if (lesser <= (cut - (step * 2)) && lesser > (cut - (step * 3))) {
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

    public String pickTarget() {

        boolean ignoreDistance = false;
        double theta;
        double distance;
        double distance_diff = 0.0;
        int L = leftTrust();
        int R = rightTrust();
        if (L == 0 && R == 0) {
            speedSuggest = 1400;
            return "unknown";
        }

        if ((L - R) >= 2) {
            theta = thetaLeft;
            distance = -distanceAvgL;
        } else if ((R - L) >= 2) {
            theta = thetaRight;
            distance = distanceAvgR;
        } else if (R == 0 && L >= 1) {
            //return "slight-left";
            theta = thetaLeft;
            distance = distanceAvgL;
            if (isFarFromWall(distance)) {
                turnSuggest = 1500 + calcTurnFromDistance(distance, idealDistance);
                //return "tiny-left";
                return "too far from left wall";
            }
        } else if (L == 0 && R >= 1) {
            //return "slight-right";
            theta = thetaRight;
            distance = distanceAvgR;
            if (isFarFromWall(distance)) {
                turnSuggest = 1500 - calcTurnFromDistance(distance, idealDistance);
                return "too far from right wall";
            }
        } else if (Math.abs(L - R) < 2) {
            distance_diff = distanceAvgR - distanceAvgL;
            theta = (thetaRight + thetaLeft) / 2;
            if (Math.abs(distance_diff) <= 5) {
                ignoreDistance = true;
                distance = distanceAvgR; // simper than converting left value, fix later
            } else if (distance_diff > 5) {
                distance = -distanceAvgL;
                if (distance == 0) {
                    distance = -.1;
                }
            } else {
                distance = distanceAvgR;
                if (distance == 0) {
                    distance = .1;
                }
            }
        } else {
            speedSuggest = 1400;
            return "unknown";
        }

        if (isCloseToWall(distance) && !ignoreDistance) {
            if (distance < 0) {

                turnSuggest = 1500 - calcTurnFromDistance(distance, idealDistance);
                //return "slight-right";
                return "too close to left wall";

            } else {
                turnSuggest = 1500 + calcTurnFromDistance(distance, idealDistance);
                //return "slight-left";
                return "too close to right wall";

            }
        }

        if (!isStraight(theta)) {
            if (isTurnedLeft(theta)) {
                turnSuggest = 1500 - calcTurnFromAngle(theta, idealTheta);
                //return "slight-right";
                return "turned to the left, correcting right";
            } else if (isTurnedRight(theta)) {
                turnSuggest = 1500 + calcTurnFromAngle(theta, idealTheta);
                //return "slight-left";
                return "turned to the right, correcting left";
            } else {
                speedSuggest = 1400;
            }
            return "unknown";

        } else {
            turnSuggest = 1500;
            return "straight";
        }
    }

    public String pickDirection() {

        boolean ignoreDistance = false;
        double theta;
        double distance;
        double distance_diff = 0.0;
        int L = leftTrust();
        int R = rightTrust();
        if (L == 0 && R == 0) {
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "unknown";
        }

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
            storeTarget(calcIdealTheta(-idealDistance, avgDist));
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust left";

        } else if (L == 0 && R >= 1) {
            //return "slight-right";
            storeTheta(thetaRight);
            storeDistance(distanceAvgR);
            storeTarget(calcIdealTheta(idealDistance, avgDist));
            turnSuggest = calcTurn(avgTarget, avgTheta);
            return "trust right";

        } else if (Math.abs(L - R) < 2) {
            storeTheta((thetaRight + thetaLeft) / 2);
            double left_off = Math.abs(distanceAvgL - idealDistance);
            double right_off = Math.abs(distanceAvgR - idealDistance);
            if (left_off > right_off) {
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
    }

    private void updateConfidence() {
        confidenceLF = calcConfidence(readingLF);
        confidenceLR = calcConfidence(readingLR);
        confidenceRF = calcConfidence(readingRF);
        confidenceRR = calcConfidence(readingRR);
        confidenceAvgL = (confidenceLF + confidenceLR) / 2;
        confidenceAvgR = (confidenceRF + confidenceRR) / 2;
    }

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
