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

// Serial code examples from:
// http://www.csc.kth.se/utbildning/kth/kurser/DH2400/interak08/6-Comm.html
// http://www.csc.kth.se/utbildning/kth/kurser/DH2400/interak08/6-SpotComm.java

public class URange extends MIDlet {

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Ultrasonic MaxSonar in action...");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

    private void run()throws IOException {
        byteUartCommunication();
    }
    
    private void byteUartCommunication(){
        EDemoBoard.getInstance().initUART(9600, false);
        while(true){
            try{
                System.out.print((char)EDemoBoard.getInstance().receiveUART());
            }catch(IOException e){
                if(e.getMessage().equals("empty uart buffer"))
                    Utils.sleep(100);
                else
                    System.out.println(e);
            }
        }
    }
    
    private void streamUartCommunication() throws IOException {
        InputStream is= Connector.openInputStream("edemoserial://usart?baudrate=9600");
        byte[]buffer= new byte[256];
        int length=-1;
        while((length=is.read(buffer))!=-1){
            System.out.write(buffer, 0, length);
        }
    }
    
        //IAnalogInput sensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
        
   while(true){
        try {
                /* Note that the ultrasonic range finder uses specular reflections,
             * which require that the object it is locating have some part of its surface
             * oriented parallel with the sensor. So testing this with a flat notebook, 
             * if the notebook is tilted then the range finder cannot see it and detects 
             * infinite distance. With a person or other round object, there will always
             * be some reflection back to the sensor so this problem is avoided.
             * -Erik
             */
            
            // Analog usage:
//                double vcc = 3.3; // Change this depending on power supply
//                double v_measured = sensor.getVoltage();
//                double v_perInch = vcc/512;
//                double range = v_measured/v_perInch;
//                System.out.println("Range: "+ range);  
//           //     led.setOff();
//                Utils.sleep(1000);
            // End of Analog usage code.
            
            // Serial usage: tie BW pin low
            
            // End of Serial usage code.
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
