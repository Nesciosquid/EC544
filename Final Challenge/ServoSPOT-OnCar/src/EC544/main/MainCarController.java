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
package EC544.main;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITemperatureInput;
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
import com.sun.squawk.io.BufferedWriter;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

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
public class MainCarController extends MIDlet implements ISwitchListener {

    private static final int SERVO_CENTER_VALUE = 1500; // go straight (wheels, servo1), or stop (motor, servo2)
    private static final int TURN_MAX_VALUE = 2000; // max turn left, max should be 2000
    private static final int TURN_MIN_VALUE = 1000; // max turn right, min should be 1000
    private static final int SPEED_MAX_VALUE = 2000; // go backwards fast
    private static final int SPEED_MIN_VALUE = 1000; // go forward fast
    private static final int SERVO_MAX_VALUE = 2000;
    private static final int SERVO_MIN_VALUE = 1000;
    private static final int SAMPLE_TIME = 50;
    private static final int TURN_HIGH_STEP = 500; //steering step high
    private static final int TURN_LOW_STEP = 20; //steering step low
    private static final int SPEED_HIGH_STEP = 50; //speeding step high
    private static final int SPEED_LOW_STEP = 30; //speeding step low
    private static double CAR_LENGTH = 10.0; // in 'IR' units, whatever those are
    private static double CONFIDENCE_DISTANCE = 20.0; // in 'IR' units
    private static final int HOST_PORT = 42;
    private static double MAX_TRACKING_ANGLE = 30.0;
    private static double CONFIDENCE_HIGH_CUTOFF = 1.00;
    private static double CONFIDENCE_CUTOFF_STEP = .25;
    private static boolean stopTurn = false;
    private static boolean startTurn = false;
    private static int MIN_TURN_DIFF = 10;
    private static boolean REVERSE_LEDS = false;
    private static BufferedWriter writeOut;
    // Devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    //private ISwitch sw = eDemo.getSwitches()[EDemoBoard.SW1];
    private IAnalogInput irRightFront = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput irLeftFront = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput irRightRear = eDemo.getAnalogInputs()[EDemoBoard.A3];
    private IAnalogInput irLeftRear = eDemo.getAnalogInputs()[EDemoBoard.A2];
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
    private long myAddr = 0;
    private int STOP = 0;
    private int BROADCAST_PORT_COUNT = 1;
    private int BROADCAST_POINT_SKIP = 3; // Number of datapoints to skip (send 1 out of N+1)
    RadiogramConnection[] broadcastConnections = new RadiogramConnection[BROADCAST_PORT_COUNT];
    Datagram[] broadcastDatagrams = new Datagram[BROADCAST_PORT_COUNT];
    IRDaemon IR_DAEMON = new IRDaemon(CAR_LENGTH, CONFIDENCE_DISTANCE);
    LEDaemon LED_DAEMON = new LEDaemon(myLEDs, REVERSE_LEDS, CONFIDENCE_HIGH_CUTOFF, CONFIDENCE_CUTOFF_STEP, MAX_TRACKING_ANGLE);

    //Initialize RadiogramConnections.
    //Makes as many as BROADCST_PORT_COUNT specifies. 
    private void initializeConn() {
        String ourAddress = System.getProperty("IEEE_ADDRESS");

        try {
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening
            for (int i = 0; i < BROADCAST_PORT_COUNT; i++) {
                broadcastConnections[i] = (RadiogramConnection) Connector.open("radiogram://broadcast:" + (HOST_PORT + i));
                broadcastDatagrams[i] = broadcastConnections[i].newDatagram(broadcastConnections[i].getMaximumLength());
            }
            System.out.println("Set up xmitConn(s)...");
            System.out.println("Set up dg...");
        } catch (Exception e) {
            System.err.println("Caught " + e + " in rCon initialization.");
            notifyDestroyed();
        }
    }

    //Constructor, currently not used
    public MainCarController() {
    }

    public void switchReleased(SwitchEvent sw) {
        // do nothing
    }

    //Switch 1 cycles the speed. 1500->1000 in 100 increments, then 1500 again. 
    //Remember, 1000 is FAST forward!
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

    //Write a CarPoint object to a datagram. 
    private void writeToDatagram(CarPoint cp, Datagram dg) {
        try {
            dg.reset();
            dg.writeUTF("CarPoint");
            dg.writeFloat(cp.time);
            dg.writeFloat(cp.LF);
            dg.writeFloat(cp.RF);
            dg.writeFloat(cp.LR);
            dg.writeFloat(cp.RR);
            dg.writeFloat(cp.distRight);
            dg.writeFloat(cp.distLeft);
            dg.writeFloat(cp.LT);
            dg.writeFloat(cp.RT);
            dg.writeFloat(cp.thetaRight);
            dg.writeFloat(cp.thetaLeft);
            dg.writeFloat(cp.theta);
            dg.writeFloat(cp.distance);
            dg.writeFloat(cp.turn);
            dg.writeFloat(cp.velocity);
            dg.writeFloat(cp.targetDist);
            dg.writeFloat(cp.startTurn);
            dg.writeFloat(cp.stopTurn);
            dg.writeFloat(cp.targetTheta);
        } catch (IOException ex) {
            System.out.println("IOexception " + ex + "in writeToDatagram.");
        }
    }
    
        private void writeToDatagram(SmallPoint sp, Datagram dg) {
        try {
            dg.reset();
            dg.writeByte(sp.booleansToUnsignedInt());
            dg.writeByte(sp.toUnsignedInt(sp.leftFront));
            dg.writeByte(sp.toUnsignedInt(sp.rightFront));
            dg.writeByte(sp.toUnsignedInt(sp.leftRear));
            dg.writeByte(sp.toUnsignedInt(sp.rightRear));
            dg.writeByte(sp.servoValueToUnsignedInt(sp.setTurn));
            dg.writeByte(sp.servoValueToUnsignedInt(sp.setSpeed));
            dg.writeDouble(sp.time);
 
        } catch (IOException ex) {
            System.out.println("IOexception " + ex + "in writeToDatagram.");
        }
    }

    //Write a CarPoint object into a datagram and send it over a connection.
    private void transmitCarPoint(CarPoint cp, RadiogramConnection rc, Datagram dg) {
        try {
            dg = rc.newDatagram(rc.getMaximumLength());
            writeToDatagram(cp, dg);
            
            //System.out.println("Sending carpoint! Time: " + System.currentTimeMillis());
            rc.send(dg);
        } catch (IOException ex) {
            System.out.println(ex);
            System.out.println("IO exception in while block of transmit loop");
        }
    }

    private void transmitSmallPoint(SmallPoint sp, RadiogramConnection rc, Datagram dg) {
        try {
            dg = rc.newDatagram(rc.getMaximumLength());
            writeToDatagram(sp, dg);
            sp.printPoint();
            //System.out.println("Sending carpoint! Time: " + System.currentTimeMillis());
            rc.send(dg);
        } catch (IOException ex) {
            System.out.println(ex);
            System.out.println("IO exception in while block of transmit loop");
        }
    }

    private void updateIR() {
        try {
            IR_DAEMON.update(irLeftFront.getVoltage(), irRightFront.getVoltage(), irLeftRear.getVoltage(), irRightRear.getVoltage()); //Does a LOT of calculatoins, see IR_DAEMON class!
            LED_DAEMON.changeColors(IR_DAEMON.thetaRight, IR_DAEMON.thetaLeft, IR_DAEMON.confidenceRF, IR_DAEMON.confidenceLF, IR_DAEMON.confidenceRR, IR_DAEMON.confidenceLR);
        } catch (IOException ex) {
            System.out.println("IOException " + ex + " in updateIR()");
        }
    }

    /**
     * This code runs when the app begins, usually on SunSPOT restart. *
     */
    protected void startApp() throws MIDletStateChangeException {
        BootloaderListenerService.getInstance().start();
        initializeConn();
        System.out.println("Hello, world");
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);

        turnHard();

        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.setOn();
            myLEDs.getLED(i).setColor(LEDColor.GREEN);
        }

        Utils.sleep(500);
        LED_DAEMON.setAllOff();
        int broadcastCounter = 0; //Used with BROADCAST_PORT_COUNT
        int skipCounter = 0; // Used to skip sending datapoints to reduce network traffic

        while (STOP != 1) { // Runs this loop until the program ends, currently mapped to sw2

            updateIR();
            String command = IR_DAEMON.pickDirection(); // You MUST call pickDirection(); or IR_DAEMON will give you the wrong response for turnSuggest!
            setTurn = IR_DAEMON.turnSuggest;

            //If the IR_DAEMON can't tell you where to go, you should slow down...
            if (command.equals("unknown")) {
                driveSlow();
            } else {
                drive();
            }
            turnHard();

            //Used for transmitting to different ports, and skipping transmission of datapoints
            if (skipCounter == BROADCAST_POINT_SKIP) {
                //CarPoint cp = IR_DAEMON.getCarpoint(setTurn, setSpeed, startTurn, stopTurn);
                SmallPoint sp = IR_DAEMON.getSmallPoint(setTurn, setSpeed, System.currentTimeMillis());
                //transmitCarPoint(cp, broadcastConnections[broadcastCounter], broadcastDatagrams[broadcastCounter]);
                transmitSmallPoint(sp, broadcastConnections[broadcastCounter], broadcastDatagrams[broadcastCounter]);
                if (broadcastCounter < BROADCAST_PORT_COUNT - 1) {
                    broadcastCounter++;
                } else {
                    broadcastCounter = 0;
                }
                skipCounter = 0;
            } else {
                skipCounter++;
            }

            Utils.sleep(SAMPLE_TIME);
            if (STOP == 1) {
                speedServo.setValue(1500);
            }
        }
    }

    //Update turn servo to approach current setTurn value
    private void updateSteer() {
        updateServo(turnServo, setTurn, TURN_LOW_STEP);
    }

    //sets the turn servo to the current setTurn value
    private void turnHard() {
        setServo(turnServo, setTurn);
    }

    //Update the car's speed to approach the current setSpeed
    private void drive() {
        updateServo(speedServo, setSpeed, SPEED_LOW_STEP);
    }

    //Update the car's speed to approach the slowSpeed setting
    private void driveSlow() {
        updateServo(speedServo, slowSpeed, SPEED_HIGH_STEP);
    }

    //Set servo to the target value.
    //Like updateServo but with 'infinite' acceleration
    private void setServo(Servo target_servo, int target_value) {
        target_servo.setValue(target_value);
    }

    //Update the value of a servo to approach a set point, one acceleration unit at a time
    //Call repeatedly (with delays between) to reach set_point
    private void updateServo(Servo target_servo, int set_point, int acceleration) {
        int currentValue = target_servo.getValue();
        int newValue;

        if (set_point < SERVO_MIN_VALUE) {
            set_point = SERVO_MIN_VALUE;
        } else if (set_point > SERVO_MAX_VALUE) {
            set_point = SERVO_MAX_VALUE;
        }

        if (set_point < currentValue) {
            {
                newValue = currentValue - acceleration;
            }
            if (set_point < newValue) {
                newValue = set_point;
            }
        } else if (set_point > currentValue) {
            newValue = currentValue + acceleration;
            if (set_point > newValue) {
                newValue = set_point;
            }
        } else {
            newValue = set_point;
        }
        target_servo.setValue(newValue);
    }

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
