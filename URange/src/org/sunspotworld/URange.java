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
 * @author  Erik Knechtel
 */
public class URange extends MIDlet {

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Ultrasonic MaxSonar in action...");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));

        IAnalogInput sensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];

   while(true){
        try {
                /* Note that the ultrasonic range finder using specular reflections,
             * which require that the object it is locating have some part of its surface
             * oriented parallel with the sensor. So testing this with a flat notebook, 
             * if the notebook is tilted then the range finder cannot see it and detects 
             * infinite distance. With a person or other round object, there will always
             * be some reflection back to the sensor so this problem is avoided.
             * -Erik
             */
                double vcc = 3.3; // Sensor can handle 3.3 from the spot, OR 5V from the battery pack.
                double v_measured = sensor.getVoltage();
                double v_perInch = vcc/512;
                double range = v_measured/v_perInch;
                System.out.println("Range: "+ range);  
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
