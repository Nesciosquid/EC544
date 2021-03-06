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

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;

import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.IIOPin;
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
    private static final int BLINK_TIME = 500;
    private static final int TURN_LOW_STEP = 20; //steering step low
    private static final int SPEED_HIGH_STEP = 50; //speeding step high
    private static final int SPEED_LOW_STEP = 30; //speeding step low
    private static final int HOST_PORT = 99;
    private static final int RECEIVE_PORT = 98;
    private static double MAX_TRACKING_ANGLE = 30.0;
    private static double CONFIDENCE_HIGH_CUTOFF = 1.00;
    private static double CONFIDENCE_CUTOFF_STEP = .25;
    private static boolean REVERSE_LEDS = false;
    public boolean forceDaemon = false;
    private static BufferedWriter writeOut;
    // Devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    //private ISwitch sw = eDemo.getSwitches()[EDemoBoard.SW1];
    private IAnalogInput irRightFront = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput irLeftFront = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput irRightRear = eDemo.getAnalogInputs()[EDemoBoard.A3];
    private IAnalogInput irLeftRear = eDemo.getAnalogInputs()[EDemoBoard.A2];
    IAnalogInput front = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A4];
    private ISwitch sw1 = eDemo.getSwitches()[EDemoBoard.SW1];
    private ISwitch sw2 = eDemo.getSwitches()[EDemoBoard.SW2];
    private ITriColorLED[] leds = eDemo.getLEDs();
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private IIOPin headLightL = eDemo.getIOPins()[EDemoBoard.D0];
    private IIOPin headLightR = eDemo.getIOPins()[EDemoBoard.D1];
    private IIOPin rearLightL = eDemo.getIOPins()[EDemoBoard.D2];
    private IIOPin rearLightR = eDemo.getIOPins()[EDemoBoard.D3];
    // 1st servo for left & right direction 
    private Servo turnServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    // 2nd servo for forward & backward direction
    private Servo speedServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    //private int servo1ForwardValue;
    //private int servo2ForwardValue;
    private boolean recvDo = true;
    private boolean lightsDo = true;
    private boolean lockTurn = true;
    private boolean lockSpeed = true;
    private boolean isColliding = false;
    private boolean takeNextRight = false;
    private boolean takeNextLeft = false;
    private boolean honking = false;
    private static int setSpeed = 1500;
    private static double setDistance = 75.0; // cm
    private static int setTurn = 1500;
    private static int slowSpeed = 1350;
    private final int SAMPLE_COUNT = 1; //used to average reads or skip broadcasting
    private final int BROADCAST_SKIP = 4;
    private double[] ultra_samples = new double[SAMPLE_COUNT];
    private double[] LF_samples = new double[SAMPLE_COUNT];
    private double[] RF_samples = new double[SAMPLE_COUNT];
    private double[] LR_samples = new double[SAMPLE_COUNT];
    private double[] RR_samples = new double[SAMPLE_COUNT];
    private int stopCar = 0;
    private final int BROADCAST_PORT_COUNT = 1;
    RadiogramConnection[] broadcastConnections = new RadiogramConnection[BROADCAST_PORT_COUNT];
    Datagram[] broadcastDatagrams = new Datagram[BROADCAST_PORT_COUNT];
    IRDaemon IR_DAEMON = new IRDaemon();
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

    public void alertSound() {
        // siren code goes here
    }

    //Constructor, currently not used
    public MainCarController() {
    }

    private double ultrasonicToCent(double volts) {
        return 363.2 * volts - 7;
    }

    private void processCommand(String command, int value) {
        if (command.equals("speed")) {
            setSpeed = value;
            lockSpeed = true;
            if (value == 0) {
                lockSpeed = false;
                setSpeed = 1500;
            }
            System.out.println("Speed set to" + setSpeed);
            drive();
        } else if (command.equals("turn")) {
            if (value == 0) {
                lockTurn = false;
            } else {
                setTurn = value;
                System.out.println("Set turn value to: " + setTurn);
                turnHard();
                lockTurn = true;
            }
            turnHard();
            System.out.println("Turn set to" + setTurn);
        } else if (command.equals("takeNextRight")) {
            takeNextRight = true;
        } else if (command.equals("takeNextLeft")) {
            takeNextLeft = true;
        } else if (command.equals("honk")) {
            honking = true;
        } else if (command.equals("stop")) {
            setSpeed = 1500;
        } else if (command.equals("setDistance")) {
            setDistance = value;
        }
        forceDaemon = true;
    }

    private void lightLoop() {
        boolean lightStrobe = true;
        headLightL.setAsOutput(true);
        headLightR.setAsOutput(true);
        rearLightL.setAsOutput(true);
        rearLightR.setAsOutput(true);
        while (lightsDo) {
            if (takeNextLeft) {
                if (lightStrobe) {
                    rearLightL.setHigh();
                    headLightL.setHigh();
                    lightStrobe = false;
                } else {
                    rearLightL.setLow();
                    headLightL.setLow();
                    lightStrobe = true;
                }
            } else if (takeNextRight) {
                if (lightStrobe) {
                    rearLightR.setHigh();
                    headLightR.setHigh();
                    lightStrobe = false;
                } else {
                    rearLightR.setLow();
                    headLightR.setLow();
                    lightStrobe = true;
                }
            } else if (setSpeed > 1500) {
                rearLightR.setHigh();
                rearLightL.setHigh();
            } else {
                rearLightR.setLow();
                rearLightL.setLow();
                headLightR.setHigh();
                headLightL.setHigh();
            }
            Utils.sleep(BLINK_TIME);
        }
    }

    private void receiveLoop() {
        RadiogramConnection rcvConn = null;
        while (recvDo) {
            try {
                rcvConn = (RadiogramConnection) Connector.open("radiogram://:" + RECEIVE_PORT);
                while (recvDo) {
                    try {
                        Radiogram rdg = (Radiogram) rcvConn.newDatagram(rcvConn.getMaximumLength());
                        rdg.reset();
                        rcvConn.receive(rdg);           // listen for a packet
                        // Append this message to the incoming message queue "availableMessages"
                        try {
                            String firstLine = rdg.readUTF();
                            System.out.println("Received datagram type: " + firstLine);
                            if (firstLine.equals("triggered")) {
                                // do interesting things with beacon code!
                            } else if (firstLine.equals("command")) {
                                String newCommand = rdg.readUTF();
                                System.out.println("Command type: " + newCommand);
                                int newValue = Integer.parseInt(rdg.readUTF());
                                System.out.println("Command value: " + newValue);
                                processCommand(newCommand, newValue);
                            }
                        } catch (IOException ex) {
                            System.out.println("IOException in recvLoop, reading first line of packet!");
                            System.out.println(ex);
                        }
                    } catch (TimeoutException tex) {        // timeout - display no packet received
                        System.out.println(tex);
                    }
                }
            } catch (IOException ex) {
                System.out.println("IO exception in while block of receiving loop");
            } finally {
                if (rcvConn != null) {
                    try {
                        rcvConn.close();
                    } catch (IOException ex) {
                        System.out.println("IO exception in finally block of receiving loop.");
                    }
                }
            }
        }
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
            stopCar = 1;
            speedServo.setValue(1500);

        }
    }

    //Write a CarPoint object to a datagram. 
    private void writeToDatagram(CarPoint cp, Datagram dg) {
        try {
            dg.reset();
            dg.writeUTF("CarPoint");
            dg.writeDouble(cp.time);
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
            //sp.printPoint();
            //System.out.println("Sending carpoint! Time: " + System.currentTimeMillis());
            rc.send(dg);
        } catch (IOException ex) {
            System.out.println(ex);
            System.out.println("IO exception in while block of transmit loop");
        }
    }

    private double calcAverage(double[] values) {
        double sum = 0.0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum / values.length;
    }

    private void sampleSensors() {
        try {
            for (int i = 0; i < SAMPLE_COUNT; i++) {
                LF_samples[i] = irLeftFront.getVoltage();
                RF_samples[i] = irRightFront.getVoltage();
                LR_samples[i] = irLeftRear.getVoltage();
                RR_samples[i] = irRightRear.getVoltage();
                ultra_samples[i] = IR_DAEMON.getDistance(front.getVoltage());
                try {
                    Thread.sleep(SAMPLE_TIME);
                } catch (InterruptedException ex) {
                    System.out.println("Interrupted exception in sampleSensors()" + ex);
                }
            }
            double ultra_avg = calcAverage(ultra_samples);
            double LF_avg = calcAverage(LF_samples);
            double RF_avg = calcAverage(RF_samples);
            double LR_avg = calcAverage(LR_samples);
            double RR_avg = calcAverage(RR_samples);
            IR_DAEMON.updateReads(LF_avg, RF_avg, LR_avg, RR_avg);
            LED_DAEMON.changeColors(IR_DAEMON.thetaRight, IR_DAEMON.thetaLeft, IR_DAEMON.confidenceRF, IR_DAEMON.confidenceLF, IR_DAEMON.confidenceRR, IR_DAEMON.confidenceLR);

            if (ultra_avg <= 70 && !isColliding) {
                isColliding = true;
                alertSound();
            } else if (ultra_avg >= 80) {
                isColliding = false;
            }


        } catch (IOException ex) {
            System.out.println("IOException in sampleSensors()! " + ex);
        }
    }

    //Deprecated
    private void updateIR() {
        try {
            IR_DAEMON.updateReads(irLeftFront.getVoltage(), irRightFront.getVoltage(), irLeftRear.getVoltage(), irRightRear.getVoltage()); //Does a LOT of calculatoins, see IR_DAEMON class!
            LED_DAEMON.changeColors(IR_DAEMON.thetaRight, IR_DAEMON.thetaLeft, IR_DAEMON.confidenceRF, IR_DAEMON.confidenceLF, IR_DAEMON.confidenceRR, IR_DAEMON.confidenceLR);
        } catch (IOException ex) {
            System.out.println("IOException " + ex + " in updateIR()");
        }
    }

    private void takeSuggestions() {
        if (!forceDaemon) {
            if (!lockTurn) {

                setTurn = IR_DAEMON.turnSuggest;
            }
            if (!lockSpeed) {
                setSpeed = IR_DAEMON.speedSuggest;
            }
            isColliding = IR_DAEMON.isColliding;
            takeNextRight = IR_DAEMON.takeNextRight;
            takeNextLeft = IR_DAEMON.takeNextLeft;
        }
    }

    private void oldSampleLoop() {
        int broadcastCounter = 0; //Used with BROADCAST_PORT_COUNT
        int skipCounter = 0; // Used to skip sending datapoints to reduce network traffic
        int skipCount = SAMPLE_COUNT - 1;
        while (stopCar != 1) { // Runs this loop until the program ends, currently mapped to sw2

            sampleSensors();
            String command = IR_DAEMON.pickDirection(); // You MUST call pickDirection(); or IR_DAEMON will give you the wrong response for turnSuggest!
            if (!lockTurn) {
                setTurn = IR_DAEMON.turnSuggest;
            }

            //If the IR_DAEMON can't tell you where to go, you should slow down...
            if (command.equals("unknown")) {
                driveSlow();
            } else {
                drive();
            }
            turnHard();

            //Used for transmitting to different ports, and skipping transmission of datapoints
            if (skipCounter == skipCount) {
                SmallPoint sp = IR_DAEMON.getSmallPoint(setTurn, setSpeed, System.currentTimeMillis());
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
            if (stopCar == 1) {
                speedServo.setValue(1500);
            }
        }
    }

    private void sampleLoop() {
        while (stopCar != 1) { // Runs this loop until the program ends, currently mapped to sw2
            for (int i = 0; i < BROADCAST_SKIP; i++) {
                sampleSensors(); // takes IR readings and updates IR_DAEMON\
                IR_DAEMON.updateState(isColliding, takeNextLeft, takeNextRight, setSpeed);
                forceDaemon = false;
                String command = IR_DAEMON.pickDirection(); // You MUST call pickDirection() or IR_DAEMON will give you the wrong response for turnSuggest!
                takeSuggestions();
                if (takeNextLeft) {
                    System.out.println("Taking next left!");
                }
                if (takeNextRight) {
                    System.out.println("Taking next right!");
                }

                //If the IR_DAEMON can't tell you where to go, you should slow down...
                if (command.equals("unknown") && setSpeed != 1500) {
                    driveSlow();
                } else {
                    drive();
                }

                if (stopCar == 1) {
                    speedServo.setValue(1500);
                }

                turnHard();
            }
            SmallPoint sp = IR_DAEMON.getSmallPoint(setTurn, setSpeed, System.currentTimeMillis());
            transmitSmallPoint(sp, broadcastConnections[0], broadcastDatagrams[0]);


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

        turnHard(); // sets steering to center for startup

        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.setOn();
            myLEDs.getLED(i).setColor(LEDColor.GREEN);
        }

        Utils.sleep(500);
        LED_DAEMON.setAllOff();


        new Thread() {
            public void run() {
                sampleLoop();
            }
        }.start();


        new Thread() {
            public void run() {
                receiveLoop();
            }
        }.start();

        new Thread() {
            public void run() {
                lightLoop();
            }
        }.start();

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
        if (!lockSpeed) {
            updateServo(speedServo, slowSpeed, SPEED_HIGH_STEP);
        }
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
