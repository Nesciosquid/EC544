/*
 * Siren.java
 *  This code causes a small speaker to create sounds for the car.
 * Created on 26 Nov 2013.
 * 
 * Updated 9 Dec 2013: Added base code for a car unlock sound when a datagram
 * is received with the message "triggered". 
 * TODO: actual audio testing to determine beeps.
 */
package org.sunspotworld;

//import com.sun.spot.sensorboard.io.IOPin;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.sensorboard.peripheral.ToneGenerator;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IOutputPin;
import com.sun.spot.resources.transducers.SensorEvent;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

/**
 * Create beeps and siren sound for the car. Uses ToneGenerator.
 *
 * @author Erik Knechtel <enk@bu.edu> & Abhinav Nair <asnair@bu.edu>
 */
public class Siren extends MIDlet implements ISwitchListener {

    boolean cont = true;
    boolean go = false;
    int BEEP_DURATION = 400;
    int BEEP_FREQUENCY[] = {659, 622, 659, 622, 659, 493, 587, 523, 440,
        261, 329, 440, 493, 329, 415, 493, 523, 329,
        659, 622, 659, 622, 659, 493, 587, 523, 440,
        261, 329, 440, 493, 329, 523, 493, 440, 493,
        523, 587, 659, 392, 698, 659, 587, 349, 659,
        587, 523, 329, 587, 523, 493};
    int BEEP_WAIT = 500;
    int CAR_UNLOCK_FREQUENCY[] = {};  //this has to be figured out with the buzzer
    int CAR_UNLOCK_WAIT = 300;
    int i = 0;
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");
    private boolean recvDo = true;
    private static final int HOST_PORT = 42;
    private static final int RECEIVE_PORT = 41;
    RadiogramConnection broadcastConnection = new RadiogramConnection;
    Datagram broadcastDatagram = new Datagram;

    //Initialize RadiogramConnections.
    //Makes one, that should be enough, rather than an array.
    private void initializeConn() {
        String ourAddress = System.getProperty("IEEE_ADDRESS");

        try {
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening

            broadcastConnection = (RadiogramConnection) Connector.open("radiogram://broadcast:" + (HOST_PORT + i));
            broadcastDatagram = broadcastConnection.newDatagram(broadcastConnection.getMaximumLength());

            System.out.println("Set up xmitConn(s)...");
            System.out.println("Set up dg...");
        } catch (Exception e) {
            System.err.println("Caught " + e + " in rCon initialization.");
            notifyDestroyed();
        }
    }

    public void startApp() throws MIDletStateChangeException {
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);

        System.out.println("Running program");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host       
        EDemoBoard eDemo = EDemoBoard.getInstance();
        IOutputPin sirenPin = EDemoBoard.getInstance().getOutputPins()[EDemoBoard.H2];

        ToneGenerator speaker = (ToneGenerator) Resources.lookup(ToneGenerator.class, "speaker");
        speaker = new ToneGenerator(sirenPin);
        while (cont) {
            Utils.sleep(BEEP_WAIT);
            if (go) {
                for (i = 0; i < 51; i++) {
                    System.out.println("Sound started at " + BEEP_FREQUENCY[i] + " Hz");
                    speaker.startTone(BEEP_FREQUENCY[i]);
                    System.out.println("Sound ended, " + BEEP_DURATION + " milliseconds.");
                    Utils.sleep(BEEP_DURATION);
                    speaker.stopTone();
                    if (i == 51) {
                        i = 0;
                    }
                }
            }
        }
    }

    public void switchReleased(SwitchEvent sw) {
        // do nothing
    }

    public void switchPressed(SwitchEvent sw) {
        if (sw.getSwitch() == sw1) {
            if (!go) {
                go = true;
            }

        } else if (sw.getSwitch() == sw2) {
            cont = false;
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
                                // Play car unlock sound!
                                // This part can be copy-pasted from above, with two alterations:
                                // 1) array size will be different (probably just two beeps)
                                // 2) variable names will be CAR_UNLOCK_FREQUENCY and CAR_UNLOCK_WAIT

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

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}