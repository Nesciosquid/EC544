/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to 
 * deal in the Software without restriction, including without limitation the 
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 * sell copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 **/
package org.sunspotworld;

import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.Utils;
import java.io.IOException;
import com.sun.squawk.util.MathUtils;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.common.Globals;
import org.sunspotworld.common.TwoSidedArray;
import org.sunspotworld.lib.BlinkenLights;

/**
 * This class is used to move a servo car consisting of two servos - one for
 * left wheel and the other for right wheel. To combine these servos properly,
 * this servo car moves forward/backward, turn right/left and rotate
 * clockwise/counterclockwise.
 *
 * The current implementation has 3 modes and you can change these "moving mode"
 * by pressing sw1. Mode 1 is "Normal" mode moving the car according to the tilt
 * of the remote controller. Mode 2 is "Reverse" mode moving the car in a
 * direction opposite to Mode 1. Mode 3 is "Rotation" mode only rotating the car
 * clockwise or counterclockwise according to the tilt.
 *
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class ServoSPOTonCar extends MIDlet implements ISwitchListener {

    private static final int SERVO_CENTER_VALUE = 1500; // go straight (wheels, servo1), or stop (motor, servo2)
    private static final int TURN_MAX_VALUE = 2000; // max turn right, max should be 2000
    private static final int TURN_MIN_VALUE = 1000; // max turn left, min should be 1000
    private static final int SPEED_MAX_VALUE = 2000; // go backwards fast
    private static final int SPEED_MIN_VALUE = 1000; // go forward fast
    private static final int SERVO_MAX_VALUE = 2000;
    private static final int SERVO_MIN_VALUE = 1000;
    private static final int TURN_HIGH_STEP = 500; //steering step high
    private static final int TURN_LOW_STEP = 50; //steering step low
    private static final int SPEED_HIGH_STEP = 50; //speeding step high
    private static final int SPEED_LOW_STEP = 30; //speeding step low
    private static double CAR_LENGTH = 10.0; // in 'IR' units, whatever those are
    private static double CONFIDENCE_DISTANCE = 12.0; // in 'IR' units
    private static double MAX_TRACKING_ANGLE = 30.0;
    private static double CONFIDENCE_HIGH_CUTOFF = .60;
    private static double CONFIDENCE_CUTOFF_STEP = .15;
    private static boolean REVERSE_LEDS = false;
    // Devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    //private ISwitch sw = eDemo.getSwitches()[EDemoBoard.SW1];
    private IAnalogInput irRightFront = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput irLeftFront = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput irRightRear = eDemo.getAnalogInputs()[EDemoBoard.A2];
    private IAnalogInput irLeftRear = eDemo.getAnalogInputs()[EDemoBoard.A3];
    private ISwitch sw1 = eDemo.getSwitches()[EDemoBoard.SW1];
    private ISwitch sw2 = eDemo.getSwitches()[EDemoBoard.SW2];
    private ITriColorLED[] leds = eDemo.getLEDs();
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    // 1st servo for left & right direction 
    private Servo turnServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    // 2nd servo for forward & backward direction
    private Servo speedServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private BlinkenLights progBlinker = new BlinkenLights(0, 3);
    private BlinkenLights velocityBlinker = new BlinkenLights(4, 7);
    private int current1 = SERVO_CENTER_VALUE;
    private int current2 = SERVO_CENTER_VALUE;
    private int step1 = TURN_LOW_STEP;
    private int step2 = SPEED_LOW_STEP;
    private int direction = 0;
    private double sensor_distance = 31.75; // cm
    //private int servo1ForwardValue;
    //private int servo2ForwardValue;
    private static int setSpeed = 1500;
    private static int setTurn = 1500;
    private static int slowSpeed = 1400;
    private int servo1Left = SERVO_CENTER_VALUE + TURN_LOW_STEP;
    private int servo1Right = SERVO_CENTER_VALUE - TURN_LOW_STEP;
    private int servo2Forward = SERVO_CENTER_VALUE + SPEED_LOW_STEP;
    private int servo2Back = SERVO_CENTER_VALUE - SPEED_LOW_STEP;
    private int default_turn_cycles = 5;
    private int STOP = 0;
   

    public ServoSPOTonCar() {
    }

    public void switchReleased(SwitchEvent sw) {
        // do nothing
    }

    public void switchPressed(SwitchEvent sw) {
        if (sw.getSwitch() == sw1) {
            if (setSpeed > 1000) {
                setSpeed -= 100;
            } else {
                setSpeed = 1500;
            }

        } else if (sw.getSwitch() == sw2) {
            STOP = 1;
            speedServo.setValue(1500);

        }
    }

    /**
     * BASIC STARTUP CODE *
     */
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);
        IRDaemon d = new IRDaemon(irRightFront, irLeftFront, irRightRear, irLeftRear, CAR_LENGTH, CONFIDENCE_DISTANCE);
        LEDaemon l = new LEDaemon(myLEDs, REVERSE_LEDS, CONFIDENCE_HIGH_CUTOFF, CONFIDENCE_CUTOFF_STEP, MAX_TRACKING_ANGLE);
        BootloaderListenerService.getInstance().start();

        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.setOn();
            myLEDs.getLED(i).setColor(LEDColor.GREEN);
        }
        Utils.sleep(500);
        l.setAllOff();

        //velocityBlinker.setColor(LEDColor.BLUE);
        //progBlinker.setColor(LEDColor.BLUE);


        boolean error = false;
        while (STOP != 1) {
            //boolean timeoutError = robot.isTimeoutError();
            //int st = 0;
            int rl = 0;
            //if (!timeoutError) {
            //rl = robot.getVal(0);
            //rl = leftOrRight();
            d.update();
            //d.report();
            l.changeColors(d.thetaRight, d.thetaLeft, d.confidenceRF, d.confidenceLF, d.confidenceRR, d.confidenceLR);
            String command = d.pickDirection();
            setTurn = d.turnSuggest;
            //System.out.println("setting turn to: setTurn");
            //System.out.println("Turn servo set to: " + turnServo.getValue());
            //System.out.println(command);
            if (command == "unknown"){
                driveSlow();
            }
            /*else if (command == "straight"){
                setTurn = SERVO_CENTER_VALUE;
            }
            else if (command == "left"){
                setTurn = SERVO_MAX_VALUE;
            }
            
            else if (command == "slight-left"){
                setTurn = 1750;
            }
            
            else if (command == "slight-right"){
                setTurn = 1250;
            }
            
            else if (command == "tiny-left"){
                setTurn = 1600;
            }
            
            else if (command == "tiny-right"){
                setTurn = 1400;
            }
            
            else if (command == "right"){
                setTurn = SERVO_MIN_VALUE;
            }*/
            
            
            
            //st = robot.getVal(1);
            /*if (error) {
             step1 = SERVO1_LOW_STEP;
             step2 = SERVO2_LOW_STEP;
             //velocityBlinker.setColor(LEDColor.BLUE);
             //progBlinker.setColor(LEDColor.BLUE);
             error = false;
             }*/
            
            steer();
            if (command == "unknown"){
                driveSlow();
            }
            else{
            drive();}
            /*} else {
             velocityBlinker.setColor(LEDColor.RED);
             progBlinker.setColor(LEDColor.RED);
             error = true;
             }*/
            Utils.sleep(50);
            if (STOP == 1) {
                speedServo.setValue(1500);
            }
        }
    }

    private void setServoForwardValue() {
        servo1Left = current1 + step1;
        servo1Right = current1 - step1;
        servo2Forward = current2 + step2;
        servo2Back = current2 - step2;
        if (step2 == SPEED_HIGH_STEP) {
            velocityBlinker.setColor(LEDColor.GREEN);
        } else {
            velocityBlinker.setColor(LEDColor.BLUE);
        }
    }

    private void offsetLeft() {
        System.out.println("offsetting left...");
        for (int i = 0; i < getTurnLength(); i++) {
            forward();
            left();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        int fixTurn = (int) (getTurnLength() / 2);
        for (int i = 0; i < fixTurn; i++) {
            forward();
            right();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        goStraight();

    }

    private int getTurnLength() {
        int speed_diff = 1500 - setSpeed; // max speed = 500; min speed = 100;
        int speed_diff_importance = speed_diff / 500; // max speed = 1; min speed = 1/5;
        int turn_length_adjust = (int) (speed_diff_importance * (default_turn_cycles / 2));
        return default_turn_cycles - turn_length_adjust;
    }

    private void offsetRight() {
        System.out.println("Offsetting right...");

        for (int i = 0; i < getTurnLength(); i++) {
            forward();
            right();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        int fixTurn = (int) (getTurnLength() / 2);
        for (int i = 0; i < fixTurn; i++) {
            forward();
            left();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        goStraight();

    }

    private void left() {
        System.out.println("left");
        current1 = turnServo.getValue();
        if (current1 + step1 < TURN_MAX_VALUE) {
            turnServo.setValue(current1 + step1);
            Utils.sleep(10);
        } else {
            turnServo.setValue(TURN_MAX_VALUE);
            Utils.sleep(10);
        }
    }

    private void right() {
        System.out.println("right");
        current1 = turnServo.getValue();
        if (current1 - step1 > TURN_MIN_VALUE) {
            turnServo.setValue(current1 - step1);
            Utils.sleep(10);
        } else {
            turnServo.setValue(TURN_MIN_VALUE);
            Utils.sleep(10);
        }
        //servo2.setValue(0);
    }

    private void stop() {
        //System.out.println("stop");
        //servo1.setValue(0);
        speedServo.setValue(SERVO_CENTER_VALUE);
    }

    private void goStraight() {
        //System.out.println("stop");
        //servo1.setValue(0);
        turnServo.setValue(SERVO_CENTER_VALUE);
    }
    
    private void steer(){
        updateServo(turnServo, setTurn, TURN_LOW_STEP);
    }
    
    private void drive(){
        updateServo(speedServo, setSpeed, SPEED_LOW_STEP);
    }
    
    private void driveSlow(){
        updateServo(speedServo, slowSpeed, SPEED_HIGH_STEP);
    }
    private void updateServo(Servo target_servo, int target_value, int acceleration) {
        int currentValue = target_servo.getValue();
        int newValue;

        if (target_value < SERVO_MIN_VALUE) {
            target_value = SERVO_MIN_VALUE;
        } else if (target_value > SERVO_MAX_VALUE) {
            target_value = SERVO_MAX_VALUE;
        }

        if (target_value < currentValue) {
            {
                newValue = currentValue - acceleration;
            }
            if (target_value < newValue) {
                newValue = target_value;
            }
        } else if (target_value > currentValue) {
            newValue = currentValue + acceleration;
            if (target_value > newValue) {
                newValue = target_value;
            }
        } else {
            newValue = target_value;
        }
        target_servo.setValue(newValue);
    }

    private void forward() {
        speedServo.setValue(setSpeed);
    }

    private int leftOrRight() {
        int rl_val = 0;
        double LF = getDistance(irLeftFront);
        double LR = getDistance(irLeftRear);
        double RF = getDistance(irRightFront);
        double RR = getDistance(irRightRear);
        double averageLeft = (LF + LR) / 2;
        double averageRight = (RF + RR) / 2;

        if (averageLeft < averageRight) { // closer to left wall, so use left sensors
            double left_diff = LF - LR;
            double left_diff_importance = Math.abs(left_diff) / averageLeft;
            if (averageLeft > 30) { // If the left wall is closer, but farther than half the hall away, turn toward it. (Alcove on right!)
                rl_val = 0;
            } else {
                //System.out.println("Left diff importance" + left_diff_importance);
                if (left_diff_importance > .05) { // significant difference between front and back sensors
                    if (left_diff > 0) { // LR is closer to wall than LF
                        if (averageLeft < 15) {
                            rl_val = 0; // go forward -- you might be too close to correct angle!
                        } else {
                            rl_val = -50; // adjust the steering left to straighten out.
                        }
                    } else if (left_diff < 0) // LF is closer to wall than LR
                    {
                        rl_val = 50; // Set RL high to turn right
                    }
                } else if (averageLeft < 15) {
                    offsetRight(); // Continue going straight (for now!) Should replace with an offset-position method!
                } else {
                    rl_val = 0;
                }
            }

        } else if (averageRight < averageLeft) {
            double right_diff = RF - RR;
            double right_diff_importance = Math.abs(right_diff) / averageRight;
            if (averageRight > 30) { // If the right wall is closer, but farther than half the hall away, don't turn. (Alcove on left!)
                rl_val = 0;
            } else {
                //System.out.println("right difference" + right_diff_importance);
                if (right_diff_importance > .05) { // significant difference between front and back sensors
                    if (right_diff > 0) { // RR is closer to wall than RF
                        if (averageRight < 15) {
                            rl_val = 0; // go forward -- you might be too close to correct angle!
                        } else {
                            rl_val = 50; // turn right to straighten out
                        }
                    } else if (right_diff < 0) // LF is closer to wall than LR
                    {
                        rl_val = -50; // Set RL low to turn left
                    }
                } else if (averageRight < 15) {
                    offsetLeft();
                } else {
                    rl_val = 0; // Continue going straight (for now!) Should replace with an offset-position method!
                }
            }
        }
        return rl_val;
    }

    public double getDistance(IAnalogInput analog) {
        double volts = 0;
        try {
            volts = analog.getVoltage();
        } catch (IOException e) {
            System.err.println(e);
        }
        return 18.67 / (volts + 0.167);
    }

    /*public void switchPressed(SwitchEvent sw) {
     step1 = (step1 == SERVO1_HIGH) ? SERVO1_LOW : SERVO1_HIGH;
     step2 = (step2 == SERVO2_HIGH) ? SERVO2_LOW : SERVO2_HIGH;
     setServoForwardValue();
     }
    
     public void switchReleased(SwitchEvent sw) {
     // do nothing
     }*/
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system. I.e. if startApp throws
     * any exception other than MIDletStateChangeException, if the isolate
     * running the MIDlet is killed with Isolate.exit(), or if VM.stopVM() is
     * called.
     *
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true when this method is called, the MIDlet must
     * cleanup and release all resources. If false the MIDlet may throw
     * MIDletStateChangeException to indicate it does not want to be destroyed
     * at this time.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setOff();
        }
    }
}
