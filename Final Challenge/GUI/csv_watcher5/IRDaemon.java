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

    /*---------------------*/
    // Control booleans
    /*---------------------*/
    final boolean DYNAMIC_HALLWAY_SIZE = false;
    final boolean ALCOVES_SUCK = true;
    final boolean ALCOVES_SUCK_ALTERNATE = false;
    public boolean isColliding = false;
    public boolean takeNextLeft = false;
    public boolean takeNextRight = false;
    public int setSpeed;
    boolean corneringLeft = false;
    boolean corneringRight = false;
    final double FORWARD_TIMEOUT = 1000;
    final double CORNER_TIMEOUT = 4500; // 3000
    final double REVERSE_TIMEOUT = 2500;
    double reverse_finish_time = 0;
    double corner_finish_time = 0;
    double forward_finish_time = 0;
    /*---------------------*/
    // Environmental coefficients
    /*---------------------*/
    final double NEAR_WALL_DISTANCE = 43.0; // 
    final double DEFAULT_HALL_SIZE = 181.0; //
    final double IDEAL_DISTANCE = 75.0; //
    final double CLOSE_DISTANCE = 50.0; // 
    /*---------------------*/
    // Car-specific coefficients 
    /*---------------------*/
    final int TURN_RIGHT_MAX = 1000; // 1000
    final int TURN_LEFT_MAX = 2000; // 2000
    final int TURN_MAX = 500; // 500
    final double SENSOR_DISTANCE = 31.0;// 31.0
    final double CORNER_DISTANCE = -100.0;
    final double MAX_TURN_DISTANCE = 90.0;
    final double MAX_TURN_ANGLE = 25.0;
    final double APPROACH_ANGLE = 25.0;
    final double MAX_TURN_FACTOR = 1.0;
    final double MAX_DISTANCE_FACTOR = 1.0;
    final double MAX_ANGLE_FACTOR = 1.0;
    final double MAX_HALL_CONFIDENCE = 1.25; // if new hall distance is > 1.25 of average, beware!
    final double MAX_CONFIDENCE_ANGLE = 35;
    final double MAX_THETA_CHANGE = degToRadians(7); //5
    /*---------------------*/
    // Confidence calculation coefficients
    /*---------------------*/
    final double CONFIDENCE_DISTANCE = 80.0;
    final double CONF_3 = 1.0;
    final double CONF_2 = 1.5;
    final double CONF_1 = 2.0;
    final int SAMPLE_COUNT = 3; // was 3 for last functional test
    /*---------------------*/
    // Control coefficients
    /*---------------------*/
    private final double STRAIGHT_TOLERANCE = 5.0; // 5.0 in IR units
    /*---------------------*/
    //Sampling data storage variables 
    /*---------------------*/
    public double readingLF; // in IR units
    public double readingRF;
    public double readingLR;
    public double readingRR;
    /*---------------------*/
    //Calculation data storage instance variables
    public double[] recentDists = new double[SAMPLE_COUNT];
    public double[] recentTargets = new double[SAMPLE_COUNT];
    private double[] recentHalls = new double[SAMPLE_COUNT];
    private double[] recentThetas = new double[SAMPLE_COUNT];
    private double avgTheta;
    private double avgHall = DEFAULT_HALL_SIZE;
    private double sdHall = 0.0;
    private double middleDistance = DEFAULT_HALL_SIZE / 2;
    private boolean isLeftPreferred = true;
    private boolean isCoasting = false;
    private double targetDistance = IDEAL_DISTANCE; // 20.0 in IR
    private double targetTheta = 0.0;
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
    public double confidenceLF; // >1 implies reading is very good, <1 implies reading is less good, << 1 implies reading is bad
    public double confidenceRF;
    public double confidenceLR;
    public double confidenceRR;
    public double confidenceAvgL;
    public double confidenceAvgR;
    /*---------------------*/
    // Output storage variables
    /*---------------------*/
    public int turnSuggest;
    public int speedSuggest;

    /*---------------------*/
    // Constructors         
    /*---------------------*/
    public IRDaemon() { // for calculations only
        initRecents();
    }

    /*---------------------*/
    // Pathing decisions
    /*---------------------*/
    public String pickDirection() {
        if (forward_finish_time >= System.currentTimeMillis() && !isColliding) {
            speedSuggest = 1300;
        } else if (reverse_finish_time >= System.currentTimeMillis()) {
            speedSuggest = 1700;
        } else {
            if (isColliding) {
                isColliding = false;
                reverse_finish_time = System.currentTimeMillis() + REVERSE_TIMEOUT / 2;
                speedSuggest = 1700;
                System.out.println("Colliding, going backwards!");
            } else {
                speedSuggest = 1300;

            }
        }

        int L = leftTrust();
        int R = rightTrust();

        if (corneringLeft) {
            System.out.println("Cornering left!");
            if (System.currentTimeMillis() >= corner_finish_time) {
                targetDistance = IDEAL_DISTANCE;
                takeNextLeft = false;
                corner_finish_time = 0;
                corneringLeft = false;
                leftTurn(targetDistance);
                System.out.println("Finished left corner!");
                return "left corner finish";
            } else {
                if (L == 1){
                    targetDistance = CLOSE_DISTANCE;
                    leftTurn(targetDistance);
                }
                targetDistance = CORNER_DISTANCE;
                leftCorner();
                return "cornering left";
            }


        } else if (corneringRight) {
            System.out.println("Cornering right!");
            if (System.currentTimeMillis() >= corner_finish_time) {
                targetDistance = IDEAL_DISTANCE;
                takeNextRight = false;
                corner_finish_time = 0;
                corneringRight = false;
                System.out.println("Finished right corner!");
                rightTurn(targetDistance);
                return "right corner finish";
            } else {
                targetDistance = CORNER_DISTANCE;
                rightCorner();
                return "cornering right";
            }

        } else if (takeNextLeft) {
            System.out.println("IR: Taking next left...");
            isLeftPreferred = true;
            if (L == 0) {
                targetDistance = CORNER_DISTANCE;
                corner_finish_time = System.currentTimeMillis() + CORNER_TIMEOUT;
                reverse_finish_time = System.currentTimeMillis() + REVERSE_TIMEOUT;
                forward_finish_time = System.currentTimeMillis() + FORWARD_TIMEOUT;
                leftCorner();
                corneringLeft = true;
                return "left corner";
            } else {
                targetDistance = CLOSE_DISTANCE;
                if (L == 1) {
                    leftDrift(targetDistance);
                    return "drift left";
                } else {
                    leftTurn(targetDistance);
                    return "turn left";
                }
            }
        } else if (takeNextRight) {
            isLeftPreferred = false;
            System.out.println("IR: Taking next right...");
            if (R == 0) {
                targetDistance = CORNER_DISTANCE;
                reverse_finish_time = System.currentTimeMillis() + REVERSE_TIMEOUT;
                corner_finish_time = System.currentTimeMillis() + CORNER_TIMEOUT;
                forward_finish_time = System.currentTimeMillis() + FORWARD_TIMEOUT;
                rightCorner();
                corneringRight = true;
                return "right corner";
            } else {
                targetDistance = CLOSE_DISTANCE;
                if (R == 1) {
                    rightDrift(targetDistance);
                    return "drift right";
                } else {
                    rightTurn(targetDistance);
                    return "turn right";
                }
            }
        } else if (L == 0 && R == 0) {
            return "unknown";
        } else if (L > R) {
            if (L == 1 && ALCOVES_SUCK) { //alcove on the right, close to right wall
                leftDrift(CLOSE_DISTANCE);
                return "drift left";
            } else {
                isLeftPreferred = true;
                if (R == 0) {
                    targetDistance = CLOSE_DISTANCE;
                } else {
                    targetDistance = IDEAL_DISTANCE;
                }
                leftTurn(targetDistance);
                return "trust left";
            }
        } else if (R > L) {
            isLeftPreferred = false;
            if (R == 1 && ALCOVES_SUCK) {//alcove on the left, close to left wall;
                rightDrift(CLOSE_DISTANCE);
                return "drift right";
            } else {
                if (L == 0) {
                    targetDistance = CLOSE_DISTANCE;
                } else {
                    targetDistance = IDEAL_DISTANCE;
                }
                rightTurn(targetDistance);
                return "trust right";
            }
        } else if (L == 0 && R
                == 0) {
            return "unknown";
        } else {
            if (L >= 2 && R >= 2) {
                if (DYNAMIC_HALLWAY_SIZE) {
                    storeHall(calcHall(distanceAvgL, distanceAvgR));
                }
            }
            if (isLeftPreferred == true) {
                targetDistance = IDEAL_DISTANCE;
                leftTurn(targetDistance);
                return "left preferred";
            } else {
                targetDistance = IDEAL_DISTANCE;
                rightTurn(targetDistance);
            }
            return "right preferred";
        }
    }

    /*---------------------*/
// Housekeeping 
    /*---------------------*/
    public void initRecents() {
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            storeTheta(0);
            storeTarget(0);
            storeDistance(0);
        }
        for (int i = 0; i < recentHalls.length; i++) {
            storeHall(DEFAULT_HALL_SIZE);
        }

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

    private void updateMiddle() {
        middleDistance = avgHall;
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

    private void storeTheta(double new_theta) {
        double[] tempThetas = recentThetas;
        double oldAvg = average(tempThetas);
        double maxTheta = oldAvg + MAX_THETA_CHANGE;
        double minTheta = oldAvg - MAX_THETA_CHANGE;

        if (new_theta >= maxTheta) {
            new_theta = maxTheta;
            //System.out.println("New theta too big!");
        } else if (new_theta <= minTheta) {
            new_theta = minTheta;
            //System.out.println("New theta too small!");

        }

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

    private void ruinLeftConfidence() {
        confidenceAvgL = 5.0;
        confidenceLF = 5.0;
        confidenceLR = 5.0;
    }

    private void ruinRightConfidence() {
        confidenceAvgR = 5.0;
        confidenceRF = 5.0;
        confidenceRR = 5.0;
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

    /*---------------------*/
    // Input
    /*---------------------*/
    public void updateReads(double LF_volts, double RF_volts, double LR_volts, double RR_volts) {
        readingLF = getDistance(LF_volts);
        readingRF = getDistance(RF_volts);
        readingLR = getDistance(LR_volts);
        readingRR = getDistance(RR_volts);

        updateAngles();
        updateDistance();
        updateConfidence();
    }

    public void updateState(boolean collide, boolean leftTurn, boolean rightTurn, int speed) {
        isColliding = collide;
        takeNextLeft = leftTurn;
        takeNextRight = rightTurn;
        setSpeed = speed;
    }
    
        public void updateState(boolean collide, boolean leftTurn, boolean rightTurn, int speed, boolean cornerLeft, boolean cornerRight) {
        isColliding = collide;
        takeNextLeft = leftTurn;
        takeNextRight = rightTurn;
        setSpeed = speed;
        corneringLeft = cornerLeft;
        corneringRight = cornerRight;
    }

    /*---------------------*/
    // Output
    /*---------------------*/
    public CarPoint getCarpoint(int turn, int setSpeed, boolean startTurn, boolean stopTurn, double time) {
        CarPoint cp = new CarPoint();
        cp.time = time;
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

    //Assumes all boolean smallPoint variables are false.
    public SmallPoint getSmallPoint(int turnSetting, int speedSetting, double time) {
        SmallPoint out = new SmallPoint(readingLF, readingRF, readingLR, readingRR, turnSetting, speedSetting);
        out.isColliding = isColliding;
        out.takeNextLeft = takeNextLeft;
        out.takeNextRight = takeNextRight;
        out.corneringLeft = corneringLeft;
        out.corneringRight = corneringRight;
        out.time = time;
        if (!out.isASmallPoint()) {
            System.out.println("Created an invalid SmallPoint!" + out.booleansToUnsignedInt());
        }
        return out;
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
        System.out.println("Calculated target theta: " + toDegrees(targetTheta));
        //System.out.println("Calculated target turn: " + turnSuggest);
    }

    /*---------------------*/
    // Static Calculations
    /*---------------------*/
    public static double stDev(double[] data) {
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

    private static double average(double[] data) {
        double sum = 0.0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum / data.length;
    }

    public static double degToRadians(double theta_deg) {
        return theta_deg * .0174532925;
    }

    //Used to convert old distance calculations back to raw reads, from "IR" values
    public static double getOldVolts(double old_distance_value) {
        return (18.67 / old_distance_value) - 0.167;
    }

    //3rd-order polynomial approximation of volts -> centimeter conversion for analog IR readings
    // Contains magic numbers, don't go changin' them willy-nilly!
    public static double getDistance(double volts) {
        double oldVal = 18.67 / (volts + 0.167); // Old method!
        double newVal = 16.2537 * Math.pow(volts, 4)
                - 129.893 * Math.pow(volts, 3)
                + 382.268 * Math.pow(volts, 2)
                - 512.611 * volts + 306.439;
        //System.out.println("Old distance: " + oldVal + ", new distance: " + newVal);
        return newVal; // convert to inches because we suck
    }

    private static double toDegrees(double thetaRad) {
        return thetaRad * 57.2957795;
    }

    /*---------------------*/
    // Internal Calculations
    /*---------------------*/
    public double calcIdealTheta(double set_distance, double current_distance) {//29,30
        double distanceDifference = current_distance - set_distance;//31
        double distanceRatio = distanceDifference / MAX_TURN_DISTANCE;
        if (distanceRatio > MAX_DISTANCE_FACTOR) {
            distanceRatio = MAX_DISTANCE_FACTOR;
        } else if (distanceRatio < -MAX_DISTANCE_FACTOR) {
            distanceRatio = -MAX_DISTANCE_FACTOR;
        }
        double thetaMagnitude = degToRadians(APPROACH_ANGLE * distanceRatio); // convert to radians for consistency
        targetTheta = thetaMagnitude;
        return thetaMagnitude;
    }

    public int calcTurn(double set_theta_rad, double current_theta_rad) {
        double thetaDiff = toDegrees(set_theta_rad) - toDegrees(current_theta_rad);
        double thetaRatio = thetaDiff / MAX_TURN_ANGLE;
        if (thetaRatio > MAX_ANGLE_FACTOR) {
            thetaRatio = MAX_ANGLE_FACTOR;
        } else if (thetaRatio < -MAX_ANGLE_FACTOR) {
            thetaRatio = -MAX_ANGLE_FACTOR;
        }
        int turn;
        if (setSpeed <= 1500) {
            turn = (int) (1500 - (TURN_MAX * thetaRatio));
        } else {
            turn = (int) (1500 + (TURN_MAX * thetaRatio));
        }
        return turn;
    }

    public int calcIdealTurn(double set_distance, double current_distance, double current_theta) {
        double set_theta = calcIdealTheta(set_distance, current_distance);
        return calcTurn(set_theta, current_theta);
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

    private int calcTurnFromDistance(double current_distance, double desired_distance) {
        //System.out.println(current_distance + " away, want to be " + desired_distance);
        int turn;
        double distanceDifference = Math.abs(Math.abs(current_distance) - Math.abs(desired_distance));
        //System.out.println("Distance Difference: " + distanceDifference);
        double turnRatio = distanceDifference / MAX_TURN_DISTANCE;
        double turnFactor = turnRatio * turnRatio; // squared, so that larger difference == exponentially larger turn

        //System.out.println("Turn factor: " + turnFactor);
        if (turnFactor > MAX_TURN_FACTOR) {
            turnFactor = MAX_TURN_FACTOR; // max is 2.0
            //System.out.println("Turn factor clipped to: " + turnFactor );
        }
        turn = 50 + (int) (450 * turnFactor); // 
        //System.out.println("Calculated turn:" + turn);
        return turn;
    }

    private int calcTurnFromAngle(double current_angle_rad, double desired_angle_rad) {
        double currentAngle = Math.abs(toDegrees(current_angle_rad));
        double desiredAngle = Math.abs(toDegrees(desired_angle_rad));
        //System.out.println(currentAngle + " is current angle, want to be " + desiredAngle);

        int turn;
        double angleDifference = Math.abs(currentAngle - desiredAngle);
        //       System.out.println("Angle Difference: " + angleDifference);

        double turnRatio = (angleDifference / MAX_TURN_ANGLE);
        double turnFactor = turnRatio * turnRatio; // squared, so that larger difference == exponentially larger turn

        //System.out.println("Turn factor: " + turnFactor);
        if (turnFactor > MAX_TURN_FACTOR) {
            turnFactor = MAX_TURN_FACTOR;
            //              System.out.println("Turn factor clipped to: " + turnFactor );

        }
        turn = 50 + (int) (450 * turnFactor);
        //    System.out.println("Calculated turn:" + turn);
        //System.out.println("Calculated ideal turn of: " + turn);
        return turn;
    }

    private int calcTrust(double confFront, double confBack) {
        double lesser;
        if (confFront <= confBack) {
            lesser = confFront;
        } else {
            lesser = confBack;
        }

        if (lesser < CONF_3) {
            return 3;
        } else if (lesser < CONF_2) {
            return 2;
        }
        if (lesser < CONF_1) {
            return 1;
        } else {
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

    //Turn toward right wall (since distance will be high), but do not update theta average
    public void rightDrift(double set_distance) {
        //Do not store theta -- don't trust the right value!
        storeDistance(distanceAvgR);
        storeTarget(calcIdealTheta(set_distance, avgDist));
        turnSuggest = calcTurn(avgTarget, avgTheta);
    }

    //Turn toward right wall (since distance will be high), but do not update theta average
    public void leftDrift(double set_distance) {
        set_distance = -set_distance;
        //Do not store theta -- don't trust the right value!
        storeDistance(distanceAvgL);
        storeTarget(calcIdealTheta(set_distance, avgDist));
        turnSuggest = calcTurn(avgTarget, avgTheta);

    }

    public void leftCorner() {
        storeDistance(distanceAvgL);
        if (setSpeed > 1500) {
            turnSuggest = (int) (TURN_RIGHT_MAX);
        } else {
            turnSuggest = (int) (TURN_LEFT_MAX);
        }

    }

    public void rightCorner() {
        storeDistance(distanceAvgR);
        if (setSpeed > 1500) {
            turnSuggest = (int) (TURN_LEFT_MAX);
        } else {
            turnSuggest = (int) (TURN_RIGHT_MAX);
        }

    }

    public void rightTurn(double set_distance) {
        //System.out.println("Turning right, set distance: " + set_distance);
        storeTheta(thetaRight);
        storeDistance(distanceAvgR);
        storeTarget(calcIdealTheta(set_distance, avgDist));
        turnSuggest = calcTurn(avgTarget, avgTheta);
    }

    private double calcConfidence(double reading) {
        return reading / CONFIDENCE_DISTANCE;
    }

    private double calcDistance(double reading, double theta) {
        return Math.cos(theta) * reading;
    }

    private double calcAngle(double readingFront, double readingBack) {
        return Math.atan2((readingBack - readingFront), SENSOR_DISTANCE);
    }

    /*---------------------*/
    // State-checking booleans
    /*---------------------*/
    private boolean isCloseToWall(double distance) {
        if ((Math.abs(distance) < NEAR_WALL_DISTANCE)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isFarFromWall(double distance) {
        if ((Math.abs(distance) > NEAR_WALL_DISTANCE)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isStraight(double theta_rad) {
        double theta = toDegrees(theta_rad);
        if (Math.abs(theta) <= STRAIGHT_TOLERANCE) {
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

    private boolean isTooBig(double theta_rad) {
        double theta_deg = toDegrees(theta_rad);
        if (theta_deg > MAX_CONFIDENCE_ANGLE || theta_deg < -MAX_CONFIDENCE_ANGLE) {
            return true;
        } else {
            return false;
        }

    }

    private boolean isWayTooBig(double theta_rad) {
        double theta_deg = toDegrees(theta_rad);
        if (theta_deg > MAX_CONFIDENCE_ANGLE + 15 || theta_deg < -MAX_CONFIDENCE_ANGLE - 15) {
            return true;
        } else {
            return false;
        }
    }

    public void setReads(int newLF, int newRF, int newLR, int newRR) {
        readingLF = newLF;
        readingRF = newRF;
        readingRR = newRR;
        readingLR = newLR;
        updateAngles();
        updateDistance();
        updateConfidence();

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
}

