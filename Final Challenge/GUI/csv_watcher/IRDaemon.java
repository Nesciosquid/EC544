/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.lang.Math.*;

/**
 *
 * @author Aaron Heuckroth
 */
public class IRDaemon {
    // IR sensors

    private boolean isLeftPreferred = true;
    // Trig coefficients
    private boolean dynamicHallwaySize = false;
    private double wallNear = 15.0;
    private double wallFar = 35.0;
    private double defaultHallSize = 42.0;
    private double defaultTheta = 0.0;
    private double defaultTarget = 0.0;
    private double defaultDistance = 0.0;
    private double idealDistance = 20.0;
    private double middleDistance = defaultHallSize / 2;
    private double closeDistance = 20.0; // 17.0 works great
    private double targetDistance = 20.0;
    private double sensorDistance = 10.0;// in IR units
    private double confidenceDistance = 20.0; // in IR units
    private double confidenceHighCutoff = 1.00;
    private double confidenceCutoffStep = 0.30;
    private double straightTolerance = 5.0;
    private double confidenceThresh = .03;
    private double idealTheta = 0.0;
    private int averageSamples = 3;
    private double[] recentHalls = new double[averageSamples];
    private double avgHall = defaultHallSize;
    private double sdHall = 0.0;
    private double[] recentThetas = new double[averageSamples];
    private double avgTheta;
    //Class data variables
    public double[] recentDists = new double[averageSamples];
    public double[] recentTargets = new double[averageSamples];
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
    double maxTurnDistance = 13.5;
    double maxTurnAngle = 20.0;
    double approachAngle = 20.0;
    double maxTurnFactor = 1.0;
    double maxDistFactor = 1.0;
    double maxAngleFactor = 1.0;
    double maxHallConfidence = 1.25; // if new hall distance is > 1.25 of average, beware!
    double maxConfidenceAngle = 30;

    public CarPoint getCarpoint(int turn, int setSpeed, boolean startTurn, boolean stopTurn) {
        CarPoint cp = new CarPoint();
        cp.time = System.currentTimeMillis();
        cp.LF = (float) readingLF;
        cp.RF = (float) readingRF;
        cp.LR = (float) readingLR;
        cp.RR = (float) readingRR;
        cp.distRight = (float) distanceAvgR;
        cp.distLeft = (float) distanceAvgL;
        cp.LT = (float) calcTrust(confidenceLF, confidenceLR);
        cp.RT = (float) calcTrust(confidenceRF, confidenceRR);
        cp.thetaRight = (float) thetaRight;
        cp.thetaLeft = (float) thetaLeft;
        cp.theta = (float) avgTheta;
        cp.distance = (float) avgDist; // arbitrary for simulated data
        cp.turn = (float) turn;
        cp.velocity = (float) setSpeed;
        cp.targetDist = (float) targetDistance;
        if (startTurn) {
            cp.startTurn = 1.0f;
        } else {
            cp.startTurn = 0.0f;
        }
        if (stopTurn) {
            cp.stopTurn = 1.0f;
        } else {
            cp.stopTurn = 0.0f;
        }
        cp.targetTheta = (float) avgTarget;

        return cp;
    }

    public double stDev(double[] data) {
        double diff = 0.0;
        double sum = 0.0;
        double sdSum = 0.0;
        double avg = average(data);
        for (int i = 0; i < data.length; i++) {
            diff += (data[i] - avg);
            sdSum += (diff * diff);
        }
        return (Math.sqrt(sdSum / data.length));
    }

    private double average(double[] data) {
        double sum = 0.0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum / data.length;

    }

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

    public void initRecents() {
        for (int i = 0; i < averageSamples; i++) {
            storeTheta(defaultTheta);
            storeTarget(defaultTarget);
            storeDistance(defaultDistance);
        }
        for (int i = 0; i < recentHalls.length; i++) {
            storeHall(defaultHallSize);
        }

    }

    public int calcIdealTurn(double set_distance, double current_distance, double current_theta) {
        double set_theta = calcIdealTheta(set_distance, current_distance);
        return calcTurn(set_theta, current_theta);
    }

    public void storeTarget(double new_theta) {
        double[] tempTargets = recentTargets;
        for (int i = 0; i < recentTargets.length; i++) {
            if (i == 0) {
                recentTargets[i] = new_theta;
            } else {
                recentTargets[i] = tempTargets[i - 1];
            }
        }
        avgTarget = average(recentTargets);

    }

    private void storeDistance(double new_dist) {
        double[] tempDists = recentDists;
        if (new_dist < 0) { // new dist is a left value
            for (int i = 0; i < recentDists.length; i++) {
                if (i == 0) {
                    recentDists[i] = new_dist;
                } else {
                    if (tempDists[i - 1] < 0) { // old dist is a left value
                        recentDists[i] = tempDists[i - 1]; // add as next element 
                    } else {
                        recentDists[i] = -(avgHall - tempDists[i - 1]);
                    }
                }
            }
        } else {
            for (int i = 0; i < recentDists.length; i++) {
                if (i == 0) {
                    recentDists[i] = new_dist;
                } else {
                    if (tempDists[i - 1] < 0) {
                        recentDists[i] = (avgHall + tempDists[i - 1]);
                    } else {
                        recentDists[i] = tempDists[i - 1];
                    }
                }
            }
        }
        avgDist = average(recentDists);
    }

    private double calcHall(double left_distance, double right_distance) {
        return (left_distance + right_distance);
    }

    private void updateMiddle() {
        middleDistance = avgHall;
    }

    private void storeTheta(double new_theta) {
        double[] tempThetas = recentThetas;
        for (int i = 0; i < recentThetas.length; i++) {
            if (i == 0) {
                recentThetas[i] = new_theta;
            } else {
                recentThetas[i] = tempThetas[i - 1];
            }
        }
        avgTheta = average(recentThetas);
    }

    private void storeHall(double new_hall) {
        double[] tempHalls = recentHalls;
        for (int i = 0; i < recentHalls.length; i++) {
            if (i == 0) {
                recentHalls[i] = new_hall;
            } else {
                recentHalls[i] = tempHalls[i - 1];
            }
        }
        avgHall = average(recentHalls);
        updateMiddle();
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
        //System.out.println("Calculated ideal turn of: " + turn);
        return turn;
    }

    public IRDaemon() { // for calculations only
        initRecents();
    }

    public IRDaemon(double distance, double conf) {
        sensorDistance = distance;
        confidenceDistance = conf;
        initRecents();
    }

    public void update(double LF_volts, double RF_volts, double LR_volts, double RR_volts) {
        readingLF = getDistance(LF_volts);
        readingRF = getDistance(RF_volts);
        readingLR = getDistance(LR_volts);
        readingRR = getDistance(RR_volts);
        
        updateAngles();
        updateDistance();
        updateConfidence();
    }
    
    public double getVolts(double old_distance_value) {
        return (18.67 / old_distance_value) -0.167;
    }


    private double getDistance(double volts) {
        return 18.67 / (volts + 0.167);
    }

    private double toDegrees(double thetaRad) {
        return thetaRad * 57.2957795;
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

        System.out.println("Distance from left wall: " + -distanceAvgL + ", confidence: " + confidenceAvgL);

        System.out.println("Distance from right wall: " + distanceAvgR + ", confidence: " + confidenceAvgR);
        System.out.println("Average distance: " + avgDist);
        //System.out.println("Average hall size:" + avgHall);
        //System.out.println("Estimated hall size: " + calcHall(distanceAvgL, distanceAvgR));
        System.out.println("Theta Left (deg): " + toDegrees(thetaLeft));
        System.out.println("Theta Right (deg): " + toDegrees(thetaRight));
        System.out.println("Theta average: " + toDegrees(avgTheta));
        System.out.println("Calculated target theta: " + toDegrees(idealTheta));
        //System.out.println("Calculated target turn: " + turnSuggest);
    }

    private int calcTrust(double conf1, double conf2) {

        double step4 = confidenceHighCutoff;
        double step3 = step4 - confidenceCutoffStep;
        double step2 = step3 - confidenceCutoffStep;
        double step1 = step2 - confidenceCutoffStep;
        //System.out.println("Step4, Step3, Step2, Step1: " + step4 + ", " + step3 + ", " + step2 + ", " + step1);
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
        int trust = calcTrust(confidenceLF, confidenceLR);
        //System.out.println("Left trust:" + trust);
        return trust;

    }

    private int rightTrust() {
        int trust = calcTrust(confidenceRF, confidenceRR);
        //System.out.println("Right trust:" + trust);
        return trust;
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

    public boolean isCloseEnough(double confidence1, double confidence2) {
        double diff = Math.abs(confidence1 - confidence2);
        if (diff < confidenceThresh) {
            return true;
        } else {
            return false;
        }
    }

    public void leftTurn(double set_distance) {
        //System.out.println("Turning left, set distance: " + set_distance + ", flipping to " + -set_distance);
        set_distance = -set_distance;
        //System.out.println("recents: " + recentDists);
        //System.out.println("averages: " + avgDist);
        //System.out.println("storing left value:" );
        storeTheta(thetaLeft);
        //System.out.println("recents: " + recentDists);
        //System.out.println("averages: " + avgDist);
        storeDistance(-distanceAvgL);
        storeTarget(calcIdealTheta(set_distance, avgDist));
        turnSuggest = calcTurn(avgTarget, avgTheta);
    }

    public void rightTurn(double set_distance) {
        //System.out.println("Turning right, set distance: " + set_distance);
        storeTheta(thetaRight);
        storeDistance(distanceAvgR);
        storeTarget(calcIdealTheta(set_distance, avgDist));
        turnSuggest = calcTurn(avgTarget, avgTheta);
    }

    public String pickDirection() {

        boolean ignoreDistance = false;
        double theta;
        double distance;
        double distance_diff = 0.0;
        int L = leftTrust();
        int R = rightTrust();
        if (L == 0 && R == 0) {
            return "unknown";
        } else if (L > R) {
            isLeftPreferred = true;
            if (R == 0) {
                targetDistance = closeDistance;
            } else {
                targetDistance = idealDistance;
            }
            leftTurn(targetDistance);
            return "trust left";
        } else if (R > L) {
            isLeftPreferred = false;
            if (L == 0) {
                targetDistance = closeDistance;
            } else {
                targetDistance = idealDistance;
            }
            rightTurn(targetDistance);
            return "trust right";
        } else if (L == 0 && R == 0) {
            return "unknown";
        } else {
            if (L >= 2 && R >= 2) {
                if (dynamicHallwaySize) {
                    storeHall(calcHall(distanceAvgL, distanceAvgR));
                }
            }
            if (isLeftPreferred == true) {
                targetDistance = idealDistance;
                leftTurn(targetDistance);
                return "left preferred";
            } else {
                targetDistance = idealDistance;
                rightTurn(targetDistance);
            }
            return "right preferred";
        }
    }

    private boolean isTooBig(double theta_rad) {
        double theta_deg = toDegrees(theta_rad);
        if (theta_deg > maxConfidenceAngle || theta_deg < -maxConfidenceAngle) {
            return true;
        } else {
            return false;
        }

    }

    private boolean isWayTooBig(double theta_rad) {
        double theta_deg = toDegrees(theta_rad);
        if (theta_deg > maxConfidenceAngle + 15 || theta_deg < -maxConfidenceAngle - 15) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isTooDifferent(double currentValue, double[] recentValues) {
        double sd = stDev(recentValues);
        if (sd == 0) {
            sd = average(recentValues) / 4; // in case the arrays have dummy values!
        }

        if (currentValue > (average(recentValues) + sd * 2) || currentValue < (average(recentValues) - sd * 2)) {
            return true;
        } else {
            return false;
        }
    }

    private void ruinLeftConfidence() {
        confidenceAvgL = 0.0;
        confidenceLF = 0.0;
        confidenceLR = 0.0;
    }

    private void ruinRightConfidence() {
        confidenceAvgR = 0.0;
        confidenceRF = 0.0;
        confidenceRR = 0.0;
    }

    private void updateConfidence() {
        confidenceLF = calcConfidence(readingLF);
        confidenceLR = calcConfidence(readingLR);
        confidenceRF = calcConfidence(readingRF);
        confidenceRR = calcConfidence(readingRR);
        confidenceAvgL = (confidenceLF + confidenceLR) / 2;
        confidenceAvgR = (confidenceRF + confidenceRR) / 2;
        double newHallSize = calcHall(distanceAvgL, distanceAvgR);
        //if (isTooDifferent(newHallSize, recentHalls)) {
        //    System.out.println("Hall size change detected! Expect " + avgHall + ", saw " + newHallSize);
        /*    if (isTooDifferent(thetaLeft, recentThetas) && !isTooDifferent(thetaRight, recentThetas)) {
         System.out.println("ThetaLeft ("+thetaLeft+") different from avgTheta("+avgTheta+")");
         ruinLeftConfidence();
         }
         else {
         if (isTooDifferent(thetaRight, recentThetas) && !isTooDifferent(thetaLeft, recentThetas)) {
         System.out.println("ThetaRight ("+thetaRight+") different from avgTheta("+avgTheta+")");

         ruinRightConfidence();
         }
         }
         */
        if (isTooBig(thetaLeft) && !isTooBig(thetaRight) || isWayTooBig(thetaLeft)) {
            //System.out.println("thetaLeft too big, thetaRight might be OK: " + toDegrees(thetaLeft) + " vs. " + maxConfidenceAngle);
            ruinLeftConfidence();
        } else if (isTooBig(thetaRight) && !isTooBig(thetaLeft) || isWayTooBig(thetaRight)) {
            //System.out.println("ThetaRight too big, thetaLeft might be OK"  + toDegrees(thetaRight) + " vs. " + maxConfidenceAngle);
            ruinRightConfidence();
        }
        //}
    }

    private double calcConfidence(double reading) {
        return confidenceDistance / reading;
    }

    private double calcDistance(double reading, double theta) {
        return Math.cos(theta) * reading;
    }

    private double calcAngle(double readingFront, double readingBack) {
        return Math.atan2((readingBack - readingFront), sensorDistance);
    }

    private double getConfidence(double reading) {
        return reading;
    }
}
