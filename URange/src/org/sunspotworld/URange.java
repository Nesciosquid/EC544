/*
 * URange.java
 *
 * Created on Jul 4, 2012 4:35:45 PM;
 * Modified Oct 29, 2013
 */

package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IInputPin;
//import com.sun.spot.resources.transducers.IAnalogInput;
//import com.sun.spot.sensorboard.io.AnalogInput;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.io.PinDescriptor;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.Utils;

import java.util.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

// Serial communication (RS-232)
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;


/**
 * Important usage notes:
 * 1) When powering up, keep all objects clear in front of the sensors for at least 
 *  14". They need this clear field of view to calibrate.
 * 2) The code below must be changed to the appropriate Vcc level, 3.3V or 5V.
 * 3) If the car is moved from inside to outside while powered on, cold weather 
 *  will cause reduced up-close sensitivity.
 * 
 * Serial: TX connected to D1, RX connected to D0.
 * @author  Erik Knechtel
 */

/* Note that the ultrasonic range finder uses specular reflections,
* which require that the object it is locating have some part of its surface
* oriented parallel with the sensor. So testing this with a flat notebook, 
* if the notebook is tilted then the range finder cannot see it and detects 
* infinite distance. With a person or other round object, there will always
* be some reflection back to the sensor so this problem is avoided.
*/

public class URange extends MIDlet {

    private IInputPin rxPin = EDemoBoard.getInstance().getIOPins()[EDemoBoard.D0];
    private IInputPin txPin = EDemoBoard.getInstance().getIOPins()[EDemoBoard.D1];
    private EDemoBoard demoBoard = EDemoBoard.getInstance();
    
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Ultrasonic MaxSonar in action...");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host
        demoBoard.initUART(9600, true);
        while(true){
            
            System.out.println("In while loop");
            String returnString = "";
            byte[] buffer = new byte[64];
            
            try{
                System.out.println("Trying to read:");
                if (demoBoard.availableUART()>0){
                    System.out.println("UART available.");
                    demoBoard.readUART(buffer, 0, buffer.length);
                    returnString = returnString + new String(buffer, "US-ASCII").trim();
                    System.out.println(returnString);
                    System.out.println("Finished getting string");
                }
            } 
            catch(IOException e) {
                if(e.getMessage().equals("empty uart buffer"))
                    Utils.sleep(100);
                else
                    System.out.println(e);
            }
            try{
                Thread.sleep(1000);
            }
            catch (InterruptedException ie){
            }
        }
    }
  
    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}
