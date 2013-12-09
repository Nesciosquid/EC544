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

/**
 * Create beeps and siren sound for the car. Uses ToneGenerator.
 *
 * @author Erik Knechtel <enk@bu.edu> & Abhinav Nair <asnair@bu.edu>
 */
public class Siren extends MIDlet implements ISwitchListener {
    
    boolean cont = true;
    boolean go = false;
    int BEEP_DURATION = 400;
    int BEEP_FREQUENCY [] = {659, 622, 659, 622, 659, 493, 587, 523, 440, 
                             261,329,440,493,329,415,493,523, 329,
                             659, 622, 659, 622, 659, 493, 587, 523, 440, 
                             261,329,440,493,329,523,493,440,
    493, 523, 587, 659, 392, 698, 659, 587,349,659,587, 523, 329, 587, 523, 493} ;
                                            
    int BEEP_WAIT = 500;
    int i = 0;
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");

    public void startApp() throws MIDletStateChangeException {
        sw1.addISwitchListener(this)                   ;
        sw2.addISwitchListener(this);
        
        System.out.println("Running program");
         BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host       
        EDemoBoard eDemo = EDemoBoard.getInstance();
        IOutputPin sirenPin = EDemoBoard.getInstance().getOutputPins()[EDemoBoard.H2];

        ToneGenerator speaker = (ToneGenerator) Resources.lookup(ToneGenerator.class, "speaker");
        speaker = new ToneGenerator(sirenPin);
        while (cont){
            Utils.sleep(BEEP_WAIT);
        if (go){
       for(i=0;i<51;i++){
        System.out.println("Sound started at " + BEEP_FREQUENCY[i] + " Hz");
        speaker.startTone(BEEP_FREQUENCY[i]);
        System.out.println("Sound ended, " + BEEP_DURATION + " milliseconds.");
        Utils.sleep(BEEP_DURATION);
        speaker.stopTone();
        if(i==51)i=0;
       } 
        }
        }
        //notifyDestroyed(); 
    }
    
        public void switchReleased(SwitchEvent sw) {
        // do nothing
    }

    public void switchPressed(SwitchEvent sw) {
        if (sw.getSwitch() == sw1) {
            if (!go){
                go = true;
            }
           
        } else if (sw.getSwitch() == sw2) {
            cont = false;
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}