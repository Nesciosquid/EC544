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
public class ServoSPOTonCar extends MIDlet {

    private static final int SERVO_CENTER_VALUE = 1500;
    private static final int SERVO1_MAX_VALUE = 2000;
    private static final int SERVO1_MIN_VALUE = 1000;
    private static final int SERVO2_MAX_VALUE = 2000;
    private static final int SERVO2_MIN_VALUE = 1000;
    private static final int SERVO1_HIGH = 500; //steering step high
    private static final int SERVO1_LOW = 300; //steering step low
    private static final int SERVO2_HIGH = 50; //speeding step high
    private static final int SERVO2_LOW = 30; //speeding step low
    // Devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    //private ISwitch sw = eDemo.getSwitches()[EDemoBoard.SW1];
    private IAnalogInput irRightfront = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput irLeftfront = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput irFront = eDemo.getAnalogInputs()[EDemoBoard.A2];
    private IAnalogInput irRear = eDemo.getAnalogInputs()[EDemoBoard.A3];
    private ITriColorLED[] leds = eDemo.getLEDs();
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    // 1st servo for left & right direction 
    private Servo servo1 = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    // 2nd servo for forward & backward direction
    private Servo servo2 = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private BlinkenLights progBlinker = new BlinkenLights(0, 3);
    private BlinkenLights velocityBlinker = new BlinkenLights(4, 7);
    private int current1 = SERVO_CENTER_VALUE;
    private int current2 = SERVO_CENTER_VALUE;
    private int step1 = SERVO1_LOW;
    private int step2 = SERVO2_LOW;
    //private int servo1ForwardValue;
    //private int servo2ForwardValue;
    private int servo1Left = SERVO_CENTER_VALUE + SERVO1_LOW;
    private int servo1Right = SERVO_CENTER_VALUE - SERVO1_LOW;
    private int servo2Forward = SERVO_CENTER_VALUE + SERVO2_LOW;
    private int servo2Back = SERVO_CENTER_VALUE - SERVO2_LOW;

    public ServoSPOTonCar() {
    }

    /**
     * BASIC STARTUP CODE *
     */
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();

        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setColor(LEDColor.GREEN);
            myLEDs.getLED(i).setOn();
        }
        Utils.sleep(500);
        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setOff();
        }
        setServoForwardValue();
        progBlinker.startPsilon();
        velocityBlinker.startPsilon();
        // timeout 1000
        TwoSidedArray robot = new TwoSidedArray(getAppProperty("buddyAddress"), Globals.READ_TIMEOUT);
        try {
            robot.startInput();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //sw.addISwitchListener(this);


        velocityBlinker.setColor(LEDColor.BLUE);
        progBlinker.setColor(LEDColor.BLUE);

        boolean error = false;
        while (true) {
            boolean timeoutError = robot.isTimeoutError();
            int st = 0;
            int rl = 0;
            if (!timeoutError) {
                //rl = robot.getVal(0);
                System.out.println("Servo2 value:" + servo2.getValue());
                rl = leftOrRight();
                st = robot.getVal(1);
                if (error) {
                    step1 = SERVO1_LOW;
                    step2 = SERVO2_LOW;
                    velocityBlinker.setColor(LEDColor.BLUE);
                    progBlinker.setColor(LEDColor.BLUE);
                    error = false;
                }
                //System.out.println("Checking distance...");
                //checkDistance();
                //System.out.println("Done checking distance.");
                if (st == 1) {
                    System.out.println("Going forward...");
                    System.out.println();
                    forward();
                } else if (st == -1) {
                    System.out.println("Going backward...");
                    backward();
                } else {
                    stop();
                }
                if (rl > 40) {
                    right();
                } else if (rl < -40) {
                    left();
                } else {
                    goStraight();
                }
            } else {
                velocityBlinker.setColor(LEDColor.RED);
                progBlinker.setColor(LEDColor.RED);
                error = true;
            }
            Utils.sleep(20);
        }
    }

    private void setServoForwardValue() {
        servo1Left = current1 + step1;
        servo1Right = current1 - step1;
        servo2Forward = current2 + step2;
        servo2Back = current2 - step2;
        if (step2 == SERVO2_HIGH) {
            velocityBlinker.setColor(LEDColor.GREEN);
        } else {
            velocityBlinker.setColor(LEDColor.BLUE);
        }
    }

    private void left() {
        //System.out.println("left");
        current1 = servo1.getValue();
        if (current1 + step1 < SERVO1_MAX_VALUE) {
            servo1.setValue(current1 + step1);
            Utils.sleep(10);
        } else {
            servo1.setValue(SERVO1_MAX_VALUE);
            Utils.sleep(10);
        }
    }

    private void right() {
        //System.out.println("right");
        current1 = servo1.getValue();
        if (current1 - step1 > SERVO1_MIN_VALUE) {
            servo1.setValue(current1 - step1);
            Utils.sleep(10);
        } else {
            servo1.setValue(SERVO1_MIN_VALUE);
            Utils.sleep(10);
        }
        //servo2.setValue(0);
    }

    private void stop() {
        //System.out.println("stop");
        //servo1.setValue(0);
        servo2.setValue(SERVO_CENTER_VALUE);
    }

    private void goStraight() {
        //System.out.println("stop");
        //servo1.setValue(0);
        servo1.setValue(SERVO_CENTER_VALUE);
    }

    private void backward() {
        System.out.println("backward");
        //servo2.setValue(SERVO_CENTER_VALUE + step2);
        servo2.setValue(1400);
    }

    private void forward() {
        System.out.println("forward");
        //servo2.setValue(SERVO_CENTER_VALUE - step2);
        servo2.setValue(1600);
    }

    private int leftOrRight() {
        int rl_val = 0;
        double left = getDistance(irLeftfront);
        double right = getDistance(irRightfront);
        if (left < 20) {
            rl_val = 50;
            System.out.println("Setting RL high to turn right");
        } else if (right < 20) {
            rl_val = -50;
            System.out.println("Setting RL low to turn left");

        } else {
            rl_val = 0;
        }
        return rl_val;

    }

    private void checkDistance() {
        if (getDistance(irLeftfront) < 20) {
            myLEDs.getLED(7).setColor(LEDColor.RED);
            System.out.println("Turning right.");
            right();
        } else if (getDistance(irRightfront) < 20) {
            myLEDs.getLED(0).setColor(LEDColor.RED);
            System.out.println("Turning left.");
            left();
        } else {
            goStraight();
            System.out.println("Going straight.");
        }

        /*if(getDistance(irFront) > 10 && getDistance(irFront) < 200){
         myLEDs.getLED(3).setColor(LEDColor.RED);
         step2 = SERVO2_LOW;
         forward();
         }else if(getDistance(irFront) <= 10){
         myLEDs.getLED(3).setColor(LEDColor.RED);
         stop();
         }else {
         step2 = SERVO2_HIGH;
         forward();
         }
         if(getDistance(irRear) > 10 && getDistance(irRear) < 200){
         myLEDs.getLED(4).setColor(LEDColor.RED);
         step2 = SERVO2_LOW;
         backward();
         }else if(getDistance(irFront) <= 10){
         myLEDs.getLED(3).setColor(LEDColor.RED);
         stop();
         }else{
         step2 = SERVO2_HIGH;
         backward();
         }*/
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
