/*
 * Siren.java
 *  This code causes a small speaker to create sounds for the car.
 * Created on 26 Nov 2013.
 */
package org.sunspotworld;

//import com.sun.spot.sensorboard.io.IOPin;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.sensorboard.peripheral.ToneGenerator;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IOutputPin;
import com.sun.spot.resources.transducers.SensorEvent;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Create beeps and siren sound for the car. Uses IToneGenerator.
 * @author Erik Knechtel <enk@bu.edu>
 */
public class Siren extends MIDlet {

    public void startApp() throws MIDletStateChangeException {
        System.out.println("Running program");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host       
        EDemoBoard eDemo = EDemoBoard.getInstance();
        IOutputPin sirenPin = EDemoBoard.getInstance().getOutputPins()[EDemoBoard.D0];
        double BEEP_FREQUENCY = 5000; // in HZ. All frequencies sound the same, not sure why.
        int BEEP_DURATION = 2000; // in ms

        ToneGenerator speaker = (ToneGenerator) Resources.lookup(ToneGenerator.class, "speaker");
        speaker = new ToneGenerator(sirenPin);

        System.out.println("Sound started at " + BEEP_FREQUENCY + " Hz");
        speaker.startTone(BEEP_FREQUENCY, BEEP_DURATION);
        System.out.println("Sound ended, " + BEEP_DURATION + " milliseconds.");
        //notifyDestroyed(); 
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}