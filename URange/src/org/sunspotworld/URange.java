/*
 * URange.java
 *
 * Created on Jul 4, 2012 4:35:45 PM;
 */

package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
//import com.sun.spot.resources.transducers.IIOPin;
import com.sun.spot.resources.transducers.IAnalogInput;
//import com.sun.spot.sensorboard.io.AnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

import java.io.IOException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 * 
 * The manifest specifies this class as MIDlet-1, which means it will
 * be selected for execution.
 * @author  Yuting Zhang <ytzhang@bu.edu>
 */
public class URange extends MIDlet {

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));

        IAnalogInput sensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
        ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
        ITriColorLED led = leds.getLED(0);
        led.setRGB(100,0,0);                    // set color to moderate red
 //       while (sw1.isOpen()) {
   while(true){
        try {
           //    led.setOn();                        // Blink LED
           //    Utils.sleep(250);
                double val = sensor.getVoltage();
     
                System.out.println("Value: "+ val);  
           //     led.setOff();
                Utils.sleep(1000);
            } catch (IOException ex){
                ex.printStackTrace();
            }
          //  Utils.sleep(100);
     //       notifyDestroyed();
        }
     }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system.
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true the MIDlet must cleanup and release all resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}
