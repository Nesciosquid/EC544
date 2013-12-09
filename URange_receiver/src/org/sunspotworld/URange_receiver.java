/*
 * EC544 Challenge 8, URange receiving code
 * This code recieves the Ultrasonic range broadcast from URange.java,
 * which has to be running on another spot.
 * 
 * Based off the code from Challenge7_onSpot
 * 
 * @author Erik Knechtel
 * @date 2 Dec 2013
 */
package org.sunspotworld;

import java.util.*;
import java.util.Date;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.resources.Resources;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class URange_receiver extends MIDlet{

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
    private double Xtilt;
    private boolean xmitDo = true;
    private boolean recvDo = true;
    private boolean mainDo = true;
    private boolean processDo = true;
    private boolean ledsInUse = false;

    private final long PROCESS_SLEEP = 100;
    // End of Yuting's variables

    /* instance variables for message transmission */
    Date d; // used to create 'unique-ish' message IDs
    IEEEAddress myIEEE = new IEEEAddress(System.getProperty("IEEE_ADDRESS"));
    long myAddress = myIEEE.asLong();

    public long getAddress() {
        return myAddress;
    }

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
                        System.out.println("Received a packet specifying range of " + rdg.getData());
                        
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
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
    }

    /**
     * Main application run loop.
     */
    private void run() {
        System.out.println("Radio Signal Strength Test (version " + VERSION + ")");
        System.out.println("Packet interval = " + PACKET_INTERVAL + " msec");

        new Thread() {
            public void run() {
                recvLoop();
            }
        }.start();                      // spawn a thread to receive packets
//        new Thread() {
//            public void run() {
//                processLoop();
//            }
//        }.start();
        while (mainDo) {
//            if (sw1.isClosed() && sw2.isClosed()) {
                mainDo = false;
                processDo = false;
                xmitDo = false;
                recvDo = false;
//            }
        }
    }

    /**
     * MIDlet call to start our application.
     */
    protected void startApp() throws MIDletStateChangeException {
        // Listen for downloads/commands over USB connection
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