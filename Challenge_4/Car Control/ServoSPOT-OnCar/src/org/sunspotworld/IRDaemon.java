/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.squawk.util.MathUtils;

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
    private double sensorDistance = 10.0;// in IR units
    private double confidenceDistance = 10.0; // in IR units
    
    //Data arrays
    //private ArrayList<double> thetasLeft = new ArrayList<double>(); // in radians
    private double[] thetasRight; 
    private double[] distancesLeft; // in IR units
    private double[] distancesRight; 
    private double[] readingsLF; // in IR units
    private double[] readingsRF;
    private double[] readingsLR; 
    private double[] readingsRR; 
    private double[] confidenceLF; // >1 implies reading is very good, <1 implies reading is less good, << 1 implies reading is bad
    private double[] confidenceRF;
    private double[] confidenceLR;
    private double[] confidenceRR;
    
    public IRDaemon(IAnalogInput RF, IAnalogInput LF, IAnalogInput RR, IAnalogInput LR){
        irRightFront = RF;
        irLeftFront = LF;
        irRightRear = RR;
        irLeftRear = LR;
    }
    
    public IRDaemon(IAnalogInput RF, IAnalogInput LF, IAnalogInput RR, IAnalogInput LR, double distance, double conf){
        irRightFront = RF;
        irLeftFront = LF;
        irRightRear = RR;
        irLeftRear = LR;
        sensorDistance = distance;
        confidenceDistance = conf;
    }
    
    private void readSensors()
    {
        
    }
    
    private double distanceToWall(double reading, double theta) {
        return Math.cos(theta) * reading;
    }
    
    private double getWallAngle(double readingFront, double readingBack) {
    return MathUtils.atan2((readingBack - readingFront), sensorDistance);
    }
    
    private double getConfidence(double reading){
    return reading;
    
    }
}
