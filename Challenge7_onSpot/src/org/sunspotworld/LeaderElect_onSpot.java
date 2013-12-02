/*
 * EC544 Challenge 7, Leader Election
 *A combination of the "Discovery" sample project and code written by Aaron 
 * in Processing. 
 * 
 We implement the Bully algorithm, however we look for lowest ID, rather than highest.

 * @author Aaron Heuckroth, Erik Knechtel, Abhinav Nair
 *
 * Created on Nov 19, 2013;
 */
package org.sunspotworld;

import java.util.*;
import java.util.Date;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
//import com.sun.spot.peripheral.radio.routing.RoutingPolicyManager;
//import com.sun.spot.peripheral.radio.mhrp.aodv.AODVManager;
//import com.sun.spot.peripheral.radio.shrp.SingleHopManager;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.resources.Resources;
import com.sun.squawk.util.MathUtils;
import com.sun.spot.resources.transducers.ILed;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Routines to turn multiple SPOTs broadcasting their information and discover
 * peers in the same PAN
 *
 * The SPOT uses the LEDs to display its status as follows: LED 0: Red = missed
 * an expected packet Green = received a packet
 *
 * LED 1-5: Leader displayed count of connected spots in green
 *
 * LED 6: display TiltChange flag when sw1 is pressed Blue = false Green = true
 *
 * LED 7: Red = right tilt Blue = left tilt
 *
 * Press right witch to change neighbors' tilt state: by sending out tilt change
 * flag in the datagram SW1 = change neighbors' tilt
 *
 * Switch 2 right now is used to adjust transmitting power
 *
 * Note: Each group need to use their own channels to avoid interference from
 * others channel 26 is default Assignment of channels to groups 1- 10: Valid
 * range is 11 to 26, Group 1: 11 ?C 12 Group 2: 13 ?C 14 Group 3: 15 ?C 16
 * Group 4: 17 ?C 18 Group 6: 19 ?C 20 Group 7: 21 ?C 22 Group 8: 23 ?C 24 Group
 * 9: 25 Group 10: 26
 *
 * Assignment of port numbers: (0-31 are reserved for system use) 32 ?C 255 are
 * valid. Group 1: Ports 110-119 Group 2: Ports 120-129 Group 3: Ports 130-139
 * Group 4: Ports 140-149 Group 6: Ports 150-159 Group 7: Ports 160-169 Group 8:
 * Ports 170-179 Group 9: Ports 180-189 Group 10: Ports 190-199
 *
 *
 * @author Yuting Zhang date: Nov 28,2012 Note: this work is base on Radio
 * Strength demo from Ron Goldman
 */
public class LeaderElect_onSpot extends MIDlet implements ISwitchListener {

    private static final String VERSION = "1.0";
    // CHANNEL_NUMBER  default as 26, each group set their own correspondingly
    private static final int CHANNEL_NUMBER = IProprietaryRadio.DEFAULT_CHANNEL;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String BROADCAST_PORT = "42";
    private static final int PACKETS_PER_SECOND = 1;
    private static final int PACKET_INTERVAL = 3000 / PACKETS_PER_SECOND;
    //   private static AODVManager aodv = AODVManager.getInstance();
    private int channel = CHANNEL_NUMBER;
    private int power = 32;                             // Start with max transmit power
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ITriColorLED statusLED = leds.getLED(0);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
//    private ILightSensor light = (ILightSensor)Resources.lookup(ILightSensor.class);
    private LEDColor red = new LEDColor(50, 0, 0);
    private LEDColor green = new LEDColor(0, 50, 0);
    private LEDColor blue = new LEDColor(0, 0, 50);
    private LEDColor white = new LEDColor(100, 100, 100);
    private LEDColor purple = new LEDColor(10, 0, 10);
    private LEDColor brown = new LEDColor(60, 30, 0); // Ugh, who the hell likes brown?
    private LEDColor yellow = new LEDColor(20, 30, 0);
    private double Xtilt;
    private boolean xmitDo = true;
    private boolean recvDo = true;
    private boolean mainDo = true;
    private boolean processDo = true;
    private boolean ledsInUse = false;
    private long myAddr = 0; // own MAC addr (ID)
    private long leader = 0;  // leader MAC addr 
    private long follower = 0; // follower MAC addr
    private long TimeStamp;
    private int tilt = 0; // initialized as 0, right == 1, left == -1
    private boolean tiltchange = false;
    private final long PROCESS_SLEEP = 100;
    // End of Yuting's variables
    // From Node class from processing
        /*  Instance variables for bully leader election */
    String myTilt = "none";
    boolean hasLeader = false;
    boolean isLeader = false;
    boolean infected = false;
    boolean awaitingVictory = false;
    boolean neighborChange = true;
    boolean isNominated = false;
    long nominationTimeout = 5000;
    long leaderTimeout = 2000;
    long leadershipTimeout = 8000;
    long supplicateTimeout = 4000;
    long oldMessageTimeout = 10000;
    long cleanupTimeout = 10000;
    long moteTimeout = 8000;
    long timeToCleanup = 0;
    long timeToVictory = 0;
    long timeToPanic = 0;
    long timeToSupplicate = 0;
    /* -------------------------- */

    /* Instance variables, used to spoof wireless networking */
    // Node[] myNeighbors;
        /* ---------------------------- */

    /* instance variables for message transmission */
    Date d; // used to create 'unique-ish' message IDs
    IEEEAddress myIEEE = new IEEEAddress(System.getProperty("IEEE_ADDRESS"));
    long myAddress = myIEEE.asLong();
    int moteCount = 0;
    //public int myAddress;
    public Message[] availableMessages;
    public Message[] outgoingMessages;
    public Message lastInfection; // used for storing the infection message for re-transmission
    String myState = "right"; // oscillates randomly, should be set by accelerometer rotation
    boolean lock = false; // toggled by "Button 1"
    Hashtable seenMessages = new Hashtable();
    Hashtable seenMotes = new Hashtable();
    /* --------------------- */

    //X and Y values are Processing-only
//        public Node(int newAddress, float newX, float newY) {
//            myAddress = newAddress;
//            updateColor();
//            unlock();
//        }
    public long getAddress() {
        return myAddress;
    }

    /*Called during Process Nodes loop in Processing, used in bully leader election. 
     Should be called at the beginning of the SunSPOT wake cycle.
     Checks state of leadership -- whether leader is present, whether awaiting election results, etc.*/
    public void checkLeaderStatus() {
        if (neighborChange == true) {
            nominateSelf();
            neighborChange = false;
        }

        // This node is the leader. 
        if (isLeader) {
            // Send another victory message to re-establish leadership over other nodes.
            if (System.currentTimeMillis() >= timeToPanic) {
                announceVictory();
            }
        } // This node is not the leader.
        else {
            if (System.currentTimeMillis() >= timeToSupplicate) {
                supplicate();
            }

            //This node has a leader.
            if (hasLeader) {
                //Leader has not contacted this node in too long. Remove leader.
                if (System.currentTimeMillis() >= timeToPanic) {
                    hasLeader = false;
                    timeToPanic = 0;
                }
            } //This node has no leader.
            else {

                //This node has nominated itself for election and is waiting for responses.
                if (isNominated) {
                    //The election has timed out, and this node delcares itself the victor.
                    if (System.currentTimeMillis() >= timeToVictory) {
                        announceVictory();
                    }
                } //This node has conceded, and is awaiting another node's victory.
                else if (awaitingVictory) {

                    //No other node has claimed victory in too long, so this node nominates itself again.
                    if (System.currentTimeMillis() >= timeToVictory) {
                        nominateSelf();
                    }
                } //No leaders or elections, so this node initiates an election, nominating itself.
                else {
                    nominateSelf();
                }
            }
        }
    }

    public class moteTracker {

        long myTime;

        public moteTracker(long time) {
            myTime = time;
        }

        public long getTime() {
            return myTime;
        }
    }

    //Read through available (received) messages and take actions based on them.
    public void processMessages() {

        if (availableMessages != null) {
            for (int i = 0; i < availableMessages.length; i++) {
                readMessage(availableMessages[i]);
            }
            availableMessages = null;
        }
                if (myState.equals("infected")) {
            outgoingMessages = storeMessage(outgoingMessages, lastInfection);
        }

    }

    // Read a message, and only take action if it hasn't been seen before.
    public void readMessage(Message incomingMessage) {
        if (incomingMessage != null) {
            String messageID = incomingMessage.getID();
            String issuerID = "" + incomingMessage.getIssuer();
            TimeStamp = System.currentTimeMillis();
            String timeStampString = "" + TimeStamp;
            long tooLate = incomingMessage.getTime() + oldMessageTimeout;
            if (!seenMessages.containsKey(messageID)){
                seenMessages.put(messageID, incomingMessage);
                executeCommand(incomingMessage);
            }
        
            else {
                System.out.println("message ignored!");
            }
            if (incomingMessage.getIssuer() != myAddress) {
                if (seenMotes.containsKey(issuerID)) {
                    seenMotes.remove(issuerID);
                }
                seenMotes.put(issuerID, timeStampString);
            }
        }
    }

    // Determines whether this node 'wins' against another node for leader election
    public boolean doIWin(long challengerAddress) {
        if (myAddress < challengerAddress) {
            return true;
        } else {
            return false;
        }
    }

    //Process commands from a message and carry out appropriate tasks.
    public void executeCommand(Message newMessage) {
        String command = newMessage.getCommand();
        long issuerID = newMessage.getIssuer();
        //if (myAddress != issuerID) {
            //Another node has called an election.
            if (command.equals("elect")) {

                //This is not a message from myself!
                // This node should win against the node that sent the current message.
                //This message is not forwarded.
                if (doIWin(issuerID)) {

                    //If this node has already nominated itself, re-send another nomination message, but do not re-start election timeouts.
                    if (isNominated) {
                        writeMessage("elect");
                    } //This node is not yet nominated for election, and should nominate itself.
                    else {
                        nominateSelf();
                    }
                } //This node is lower-priority than the node that sent the current message. 
                //This node concedes and forwards the election message to its neighbors.
                else {
                    concede();
                    forwardMessage(newMessage);
                }
            }


            //Another node has claimed victory.
            if (command.equals("victory")) {
                //This node should win over the node that sent the current message.
                //This message is not forwarded.
                if (doIWin(issuerID)) {
                    nominateSelf();
                } //Accept new leader and forward victory message to neighbors.
                else if (!hasLeader){
                    allHailOurGloriousLeader();
                    forwardMessage(newMessage);
                }
                else {
                    forwardMessage(newMessage);
                }
            }

            //Leader has instructed this node to set its state to 'tilted left.'
            // Set and lock state.
            //Forward this message to neighbors.
            if (command.equals("left")) {
                myState = "left";
                lock = true;
                forwardMessage(newMessage);
            }

            //Leader has instructed this node to set its state to 'tilted right'
            // Set and lock state.
            //Forward this message to neighbors.
            if (command.equals("right")) {
                myState = "right";
                lock = true;
                forwardMessage(newMessage);
            }

            //This should not be used!
            if (command.equals("none")) {
                myState = "none";
                lock = true;
                forwardMessage(newMessage);
            }

            if (command.equals("supplicate")) {
                forwardMessage(newMessage);
            }
        

            //This node has received an 'infection' message from another infected, non-leader node.
            //Change state to infected, lock state, and set lastInfection message for repeated broadcasts to neighbors.
            if (command.equals("infected")) {
                if (!isLeader && !myState.equals("infected")) {
                    myState = "infected";
                    lock = true;
                    lastInfection = newMessage;
                }
            //}
            //Whatever state this node ended up in, update the color.
            //updateColor();
        }
    }
    

    //Queue message for sending later, so that it will be forwarded to neighbors.
    //For SunSPOTs, this would just be a broadcast (with old message ID)
    public void forwardMessage(Message newMessage) {
        outgoingMessages = storeMessage(outgoingMessages, newMessage);
        seenMessages.put(newMessage.getID(), newMessage);
    }

    //Broadcast all messages queued for broadcasting to neighbors.
//        public void broadcastMessage(Message newMessage) {
//            if (myNeighbors != null) {
//                for (int i = 0; i < myNeighbors.length; i++) {
//                    sendMessage(myNeighbors[i], newMessage);
//                }
//            }
//        }
    //Gives Message[] arrays resizeable functionality.
    //To resize an existing array, you want to call: existingArray = someNode.storeMessage(existingArray, someMessage);
    //Returns an array with the new message added to the end of the existing array.
    public Message[] storeMessage(Message[] existingMessages, Message newMessage) {
        Message[] temp;
        int size;
        if (existingMessages == null) {
            temp = new Message[1];
            temp[0] = newMessage;
            return temp;
        } else {
            size = existingMessages.length + 1;
            temp = new Message[size];
            for (int i = 0; i < temp.length; i++) {
                if (i < existingMessages.length) {
                    temp[i] = existingMessages[i];
                } else {
                    temp[i] = newMessage;
                }
            }
            return temp;
        }
    }

    //Nominate this node for election by sending out election message and changing leadership state variables.
    public void nominateSelf() {
        writeMessage("elect");
        isNominated = true;
        awaitingVictory = false;
        timeToVictory = System.currentTimeMillis() + nominationTimeout;
    }

    //Make this node concede the election by changing leadership state variables, stopping victory timeout, starting victory timeout.
    public void concede() {
        isNominated = false;
        isLeader = false;
        awaitingVictory = true;
        timeToVictory = System.currentTimeMillis() + nominationTimeout;
    }

    //Make this node announce itself as leader by changing leadership state variables, stopping victory timeout, and starting leader timeout.
    //Send out victory message to other nodes.
    public void announceVictory() {
        isNominated = false;
        timeToVictory = 0;
        isLeader = true;

        //If this node was infected before it became leader, cure it.
        if (myState.equals("infected")) {
            lock = false;
            updateTilt();
        }
        hasLeader = true;
        timeToPanic = System.currentTimeMillis() + leaderTimeout;
        writeMessage("victory");
    }

    public void supplicate() {
        timeToSupplicate = System.currentTimeMillis() + supplicateTimeout;
        writeMessage("supplicate");
    }

    //Accept another node as leader by changing leadership state variables, starting leadership timeout, and ending victory timeout.
    public void allHailOurGloriousLeader() {
        timeToVictory = 0;
        awaitingVictory = false;
        isNominated = false;
        timeToPanic = System.currentTimeMillis() + leadershipTimeout;
        timeToSupplicate = System.currentTimeMillis() + supplicateTimeout;
        hasLeader = true;
        isLeader = false;
    }

    //Returns an array with the node at the specified index removed
    //To remove a node from an existing array, call: existingArray = someNode.removeNode(existingArrray, someIndex);
    public Message[] removeMessage(Message[] messages, int index) {
        Message[] temp = null;
        if (messages != null) {
            if (messages.length > 0) {
                int size = messages.length - 1;
                int j = 0;
                temp = new Message[size];
                for (int i = 0; i < messages.length; i++) {
                    if (i != index) {
                        temp[j] = messages[i];

                        j++;
                    }
                }
            }
        }
        return temp;
    }

    //Create a new message with a given command.
    //Uses this Node's ID and date to generate a 'unique-ish' message ID
    public void writeMessage(String command) {
        forwardMessage(composeMessage(command));
    }

    public Message composeMessage(String command) {
        d = new Date();
        String messageID = "" + myAddress + "-" + d.getTime();
        Message newMessage = new Message(messageID, command, myAddress);
        return newMessage;
    }

    class Message {

        String content;
        String myID;
        long issuerID;
        long creationTime;

        Message(String newID, String newContent, long issuer) {
            content = newContent;
            myID = newID;
            issuerID = issuer;
            creationTime = System.currentTimeMillis();
        }

        Message(String newID, String newContent, long issuer, long time) {
            content = newContent;
            myID = newID;
            issuerID = issuer;
            creationTime = time;
        }

        public String getID() {
            return myID;
        }

        public long getIssuer() {
            return issuerID;
        }

        public long getTime() {
            return creationTime;
        }

        public String getCommand() {
            return content;
        }
    }

    public void lock() {
        lock = true;
    }

    public void unlock() {
        lock = false;
    }

    public void lockToggle() {
        if (lock == false) {
            lock = true;
        } else {
            lock = false;
        }
    }

    public void LEDloop() {
    }

    public void writeToDatagram(Message newMessage, Datagram newDatagram) {

        try {
            newDatagram.reset();
            newDatagram.writeUTF(newMessage.getID());
            newDatagram.writeLong(newMessage.getIssuer());
            newDatagram.writeUTF(newMessage.getCommand());
            newDatagram.writeLong(newMessage.getTime());
        } catch (IOException ex) {
            System.out.println("IO exception in writeToDatagram");
        }

    }

//    /**
//     * Loop to continually broadcast message. message format
//     * (long)myAddr,(long)follower,(long)leader,(long)TimeStamp,(int)tilt,
//     * (boolean)tiltchang,(int)power,(int)count
//     */
//    private void xmitLoop() {
//    //private void sendMessage(Message newMessage)
//        ILed led = Spot.getInstance().getGreenLed();
//        RadiogramConnection txConn = null;
//        xmitDo = true;
//        while (xmitDo) {
//            try {
//                txConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
//                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
//                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
//                long count = 0;
//                boolean ledOn = false;
//                while (xmitDo) {
//                    led.setOn();
//                    TimeStamp = System.currentTimeMillis();
//                    Xtilt = accel.getTiltX();
//                    if (Xtilt > 0) {
//                        tilt = 1;
//                        leds.getLED(7).setColor(red);
//                        leds.getLED(7).setOn();
//                    } else if (Xtilt < 0) {
//                        tilt = -1;
//                        leds.getLED(7).setColor(blue);
//                        leds.getLED(7).setOn();
//                    }
//                    count++;
//                    if (count >= Long.MAX_VALUE) {
//                        count = 0;
//                    }
//                    xdg.reset();
//                    xdg.writeLong(myAddr); // own MAC address
//                    xdg.writeLong(leader); // own leader's MAC address
//                    xdg.writeLong(follower); // own follower's MAC address
//                    xdg.writeLong(TimeStamp); // current timestamp
//                    //                  xdg.writeInt(STEER); // locl STEER amount following leader 
//                    //                  xdg.writeInt(SPEED); // local SPEED amount following leader
//                    xdg.write(tilt); //local tilt
//                    xdg.writeBoolean(tiltchange); //tiltchange flag if sw1 is pressed 
//                    xdg.writeInt(power); // own power
//                    xdg.writeLong(count); // local count
//                    txConn.send(xdg);
//                    led.setOff();
//                    long delay = (TimeStamp + PACKET_INTERVAL - System.currentTimeMillis()) - 2;
//                    if (delay > 0) {
//                        pause(delay);
//                    }
//                    leds.getLED(7).setOff();
//                }
//            } catch (IOException ex) {
//                // ignore
//            } finally {
//                if (txConn != null) {
//                    try {
//                        txConn.close();
//                    } catch (IOException ex) {
//                    }
//                }
//            }
//        }
//    }
    /**
     * Loop to continually broadcast message. message format
     * (long)myAddr,(long)follower,(long)leader,(long)TimeStamp,(int)tilt,
     * (boolean)tiltchang,(int)power,(int)count
     */
    //Iterate through outging message array, sending to all neighbor-nodes.
    //If infected, also send along the stored infection message.
    public void broadcastMessages(Datagram dataOut, RadiogramConnection rc) {
        try {
            if (outgoingMessages != null) {
                for (int i = 0; i < outgoingMessages.length; i++) {
                    TimeStamp = System.currentTimeMillis();
                    writeToDatagram(outgoingMessages[i], dataOut);
                    rc.send(dataOut);
                    //long delay = (TimeStamp + PACKET_INTERVAL - System.currentTimeMillis()) - 2;
                    long delay = 50;
                    if (delay > 0) {
                        pause(delay);
                    }
                }
                outgoingMessages = null;
            }
        } catch (IOException ex) {
            System.out.println(ex);
            System.out.println("IO Exception in Broadcast Messages");
        }
    }

    private Message datagramToMessage(Datagram dataIn) {
        Message newMessage = null;
        try {
            System.out.println("Datagram sender address: " + dataIn.getAddress());
            String mID = dataIn.readUTF();
            System.out.println("got message ID: " + mID);
            long mIssuer = dataIn.readLong();
            System.out.println("got issuer ID..." + mIssuer);
            String mCommand = dataIn.readUTF();
            long mTime = dataIn.readLong();
            System.out.println("got issue time: " + mTime);
            newMessage = new Message(mID, mCommand, mIssuer, mTime);
            System.out.println("got command..." + mCommand);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("IO Exception in DatagramToMessage");
        } finally {
            return newMessage;
        }
    }

    public void pressButton1() {
        if (isLeader) {
            lockToggle();
        } else if (myState != "infected") {
            lockToggle();
        }
    }

    //Simualte pressing SunSPOT button 2
    public void pressButton2() {
        if (!isLeader) {
            myState = "infected";
            lock = true;
        }
        Message sendMe = composeMessage(myState);
        if (myState.equals("infected")) {
            lastInfection = sendMe;
        } else {
            forwardMessage(sendMe);
        }
    }

    private void updateLights() {
        if (myState.equals("left")) {
            leds.getLED(7).setColor(green);
            leds.getLED(7).setOn();
        } else if (myState.equals("right")) {
            leds.getLED(7).setColor(blue);
            leds.getLED(7).setOn();
        } else if (myState.equals("infected")) {
            leds.getLED(7).setColor(red);
            leds.getLED(7).setOn();
        }

        if (isLeader) {
            leds.getLED(5).setColor(white);
            leds.getLED(5).setOn();
        } else if (!hasLeader) {
            leds.getLED(5).setColor(purple);
            leds.getLED(5).setOn();
        } else {
            leds.getLED(5).setOff();
        }

        if (lock) {
            leds.getLED(6).setColor(brown);
            leds.getLED(6).setOn();
        } else {
            leds.getLED(6).setOff();
        }

        displayLEDBinary(moteCount);

    }

    private void updateTilt() {
        try {
            Xtilt = accel.getTiltX();
            if (Xtilt < 0) {
                myTilt = "left";
                leds.getLED(7).setColor(red);
                leds.getLED(7).setOn();
            } else {
                myTilt = "right";
                leds.getLED(7).setColor(blue);
                leds.getLED(7).setOn();
            }
        } catch (IOException myButt) {
            System.out.println(myButt);
            System.out.println("IOException in my butt.");
        }
        if (!lock) {
            myState = myTilt;
        }
    }

    private void countMotes() {
        Enumeration moteKeys = seenMotes.keys();
        long lastMoteTime;
        long maxTime;
        TimeStamp = System.currentTimeMillis();
        while (moteKeys.hasMoreElements()) {
            String thisKey = (String) moteKeys.nextElement();
            lastMoteTime = Long.parseLong((String) seenMotes.get(thisKey));
            maxTime = lastMoteTime + moteTimeout;
            if (maxTime <= TimeStamp) {
                seenMotes.remove(thisKey);
            }
        }
        moteCount = seenMotes.size() + 1;
    }

    private void processLoop() {
        while (processDo) {
            checkLeaderStatus();
            processMessages();
            cleanup();
            updateTilt();
            updateLights();
            pause(PROCESS_SLEEP);
        }
    }

    private void cleanOldMessages() {
        if (System.currentTimeMillis() > timeToCleanup) {
            Enumeration messageKeys = seenMessages.keys();
            TimeStamp = System.currentTimeMillis();
            while (messageKeys.hasMoreElements()) {
                String thisID = (String) messageKeys.nextElement();
                Message thisMessage = (Message) seenMessages.get(thisID);
                long tooLate = thisMessage.getTime() + oldMessageTimeout;
                if (tooLate < System.currentTimeMillis()){
                    seenMessages.remove(thisID);
                }
            }
        }
    }

    private void cleanup() {
        countMotes();
        //cleanOldMessages();
    }

    private void xmitLoop() {

        RadiogramConnection xmitConn = null;
        while (xmitDo) {
            try {
                while (outgoingMessages != null) {
                    xmitConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
                    xmitConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                    Datagram xdg = xmitConn.newDatagram(xmitConn.getMaximumLength());
                    broadcastMessages(xdg, xmitConn);
                }
            } catch (IOException ex) {
                System.out.println(ex);
                System.out.println("IO exception in while block of transmit loop");
            } finally {
                if (xmitConn != null) {
                    try {
                        xmitConn.close();
                    } catch (IOException ex) {
                        System.out.println("IO exception in finally block of transmit loop");
                    }
                }
            }
        }
    }

//      /**
//     * Loop to receive packets and discover peers information
//     * (long)srcAddr,(long)srcLeader,(long)srcFollow,(long)srcTime,(int)srcSTEER,(int)srcSPEED
//     *
//     * [TO DO] sort out leader-follower by their MAC address order very most
//     * leader needs to know himself as the leader, then launch movement very
//     * most follower needs to know himself as the last
//     */
//    private void recvLoop() {
//        ILed led = Spot.getInstance().getRedLed();
//        RadiogramConnection rcvConn = null;
//        recvDo = true;
//        int nothing = 0;
//        while (recvDo) {
//            try {
//                rcvConn = (RadiogramConnection) Connector.open("radiogram://:" + BROADCAST_PORT);
//                rcvConn.setTimeout(PACKET_INTERVAL - 5);
//                Radiogram rdg = (Radiogram) rcvConn.newDatagram(rcvConn.getMaximumLength());
//                long count = 0;
//                boolean ledOn = false;
//                while (recvDo) {
//                    try {
//                        rdg.reset();
//                        rcvConn.receive(rdg);           // listen for a packet
//
//                        led.setOn();
//                        statusLED.setColor(green);
//                        statusLED.setOn();
//                        long srcAddr = rdg.readLong(); // src MAC address
//                        long srcLeader = rdg.readLong(); // src's leader
//                        long srcFollow = rdg.readLong(); // src's follow
//                        long srcTime = rdg.readLong(); // src's timestamp
//                        int srcTilt = rdg.readInt(); // src's STEER
//                        boolean srcTiltChange = rdg.readBoolean(); // src's SPEED
//                        int pow = rdg.readInt();
//
//                        String srcID = IEEEAddress.toDottedHex(srcAddr);
//
//
//                        nothing = 0;
//                        led.setOff();
//
//                        /**
//                         * [TO DO] insert script to sort leader-follower
//                         * relation neighbors change tilt according to
//                         * sreTiltChangeFlag
//                         */
//                    } catch (TimeoutException tex) {        // timeout - display no packet received
//                        statusLED.setColor(red);
//                        statusLED.setOn();
//                        nothing++;
//                        if (nothing > 2 * PACKETS_PER_SECOND && !ledsInUse) {
//                            for (int ledint = 0; ledint <= 7; ledint++) { // if nothing received eventually turn off LEDs
//                                leds.getLED(ledint).setOff();
//                            }
//                        }
//                    }
//                }
//            } catch (IOException ex) {
//                // ignore
//            } finally {
//                if (rcvConn != null) {
//                    try {
//                        rcvConn.close();
//                    } catch (IOException ex) {
//                    }
//                }
//            }
//        }
//    }
    private void recvLoop() {
        RadiogramConnection rcvConn = null;
        while (recvDo) {
            try {
                rcvConn = (RadiogramConnection) Connector.open("radiogram://:" + BROADCAST_PORT);
                rcvConn.setTimeout(PACKET_INTERVAL - 5); //lol, 2995
                Radiogram rdg = (Radiogram) rcvConn.newDatagram(rcvConn.getMaximumLength());
                while (recvDo) {
                    try {
                        rcvConn.receive(rdg);           // listen for a packet
                        // Append this message to the incoming message queue "availableMessages"
                        availableMessages = storeMessage(availableMessages, datagramToMessage(rdg));
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

    /**
     * Pause for a specified time.
     *
     * @param time the number of milliseconds to pause
     */
    private void pause(long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch (InterruptedException ex) {
            System.out.println(ex);
        }
    }

    /**
     * Initialize any needed variables.
     */
    private void initialize() {
        myAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        //statusLED.setColor(red);     // Red = not active
        //statusLED.setOn();
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
        //    AODVManager rp = Spot.getInstance().
    }

    /**
     * Main application run loop.
     */
    private void run() {
        System.out.println("Radio Signal Strength Test (version " + VERSION + ")");
        System.out.println("Packet interval = " + PACKET_INTERVAL + " msec");

        new Thread() {
            public void run() {
                xmitLoop();
            }
        }.start();                      // spawn a thread to transmit packets
        new Thread() {
            public void run() {
                recvLoop();
            }
        }.start();                      // spawn a thread to receive packets
        new Thread() {
            public void run() {
                processLoop();
            }
        }.start();
        while (mainDo) {
            if (sw1.isClosed() && sw2.isClosed()) {
                mainDo = false;
                processDo = false;
                xmitDo = false;
                recvDo = false;
                turnLEDsOff();
            }
        }
    }

    private void displayLEDBinary(int count) {
        if (count <= 0) {
            for (int i = 0; i < 5; i++) {
                leds.getLED(i).setOff();
            }
        } else if (count > 31) {
            for (int i = 0; i < 5; i++) {
                leds.getLED(i).setColor(yellow);
                leds.getLED(i).setOn();
            }
        } else {
            for (int i = 0; i < 5; i++) {
                int curCount = (int) (count / (MathUtils.pow(2, i)));
                if (curCount % 2 == 1) {
                    leds.getLED(4 - i).setColor(yellow);
                    leds.getLED(4 - i).setOn();
                } else {
                    leds.getLED(4 - i).setOff();
                }
            }
        }
    }

    /**
     * Display a number (base 2) in LEDs 1-7
     *
     * @param val the number to display
     * @param col the color to display in LEDs
     */
    private void turnLEDsOff() {
        for (int i = 0; i < 8; i++) {
            leds.getLED(i).setOff();
        }
    }

    private void displayNumber(int val, LEDColor col) {
        for (int i = 0, mask = 1; i < 7; i++, mask <<= 1) {
            leds.getLED(7 - i).setColor(col);
            leds.getLED(7 - i).setOn((val & mask) != 0);
        }
    }

    /**
     * Loop waiting for user to press a switch.
     * <p>
     * Since ISwitch.waitForChange() doesn't really block we can loop on both
     * switches ourself.
     * <p>
     * Detect when either switch is pressed by displaying the current value.
     * After 1 second, if it is still pressed start cycling through values every
     * 0.5 seconds. After cycling through 4 new values speed up the cycle time
     * to every 0.3 seconds. When cycle reaches the max value minus one revert
     * to slower cycle speed. Ignore other switch transitions for now.
     *
     */
    public void switchReleased(SwitchEvent sw) {
        // do nothing
    }

    public void switchPressed(SwitchEvent sw) {
        if (sw.getSwitch() == sw1) {
            pressButton1();
            System.out.println("Button1 pressed!");
        } else if (sw.getSwitch() == sw2) {
            pressButton2();
            System.out.println("Button2 pressed!");
        }
    }

//    /**
//     * Loop waiting for user to press a switch.
//     * <p>
//     * Since ISwitch.waitForChange() doesn't really block we can loop on both
//     * switches ourself.
//     * <p>
//     * Detect when either switch is pressed by displaying the current value.
//     * After 1 second, if it is still pressed start cycling through values every
//     * 0.5 seconds. After cycling through 4 new values speed up the cycle time
//     * to every 0.3 seconds. When cycle reaches the max value minus one revert
//     * to slower cycle speed. Ignore other switch transitions for now.
//     *
//     */
//    private void respondToSwitches() {
//        while (true) {
//            pause(100);         // check every 0.1 seconds
//            if (sw1.isClosed()) {
//                if (tiltchange) {
//                    tiltchange = false;
//                    leds.getLED(6).setColor(green);
//                    leds.getLED(6).setOn();
//                    pause(50);
//                } else {
//                    tiltchange = true;
//                    leds.getLED(6).setColor(blue);
//                    leds.getLED(6).setOn();
//                    pause(50);
//                }
//
//                pause(1000);    // wait 1.0 second
//                if (sw1.isClosed()) {
//                }
//                pause(1000);    // wait 1.0 second
//                displayNumber(0, blue);
//            }
//            if (sw2.isClosed()) {
//                int cnt = 0;
//                ledsInUse = true;
//                displayNumber(power, red);
//                pause(1000);    // wait 1.0 second
//                if (sw2.isClosed()) {
//                    while (sw2.isClosed()) {
//                        power++;
//                        if (power > 30) {
//                            cnt = 0;
//                        }
//                        if (power > 32) {
//                            power = 0;
//                        }
//                        displayNumber(power, red);
//                        cnt++;
//                        pause(cnt < 5 ? 500 : 300);    // wait 0.5 second
//                    }
//                    Spot.getInstance().getRadioPolicyManager().setOutputPower(power - 32);
//                }
//                pause(1000);    // wait 1.0 second
//                displayNumber(0, blue);
//            }
//            ledsInUse = false;
//
//        }
//    }
    /**
     * MIDlet call to start our application.
     */
    protected void startApp() throws MIDletStateChangeException {
        // Listen for downloads/commands over USB connection
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);
        new com.sun.spot.service.BootloaderListenerService().getInstance().start();
        initialize();
        run();
    }

    /**
     * This will never be called by the Squawk VM.
     */
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system.
     *
     * @param unconditional If true the MIDlet must cleanup and release all
     * resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}