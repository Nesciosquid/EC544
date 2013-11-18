/*
 * URange.java
 *  This code sends data from a sunspot to another sunspot using OTA.
 *  Our group will use this to extend the data input surface by having a second
 * sunSPOT receive Ultrasonic Range indications and transmit them, as well as any 
 * other sensors we need. This overcomes the problem of needed 5 analog inputs on a 
 * spot, each of which can only take in 4.
 * Created on Jul 4, 2012 4:35:45 PM;
 */

package org.sunspotworld;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
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

    public static final String BROADCAST_PORT = "37";

    public void startApp() throws MIDletStateChangeException {
        System.out.println("Starting sensor gathering");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        IAnalogInput sensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A3];
        
        DatagramConnection dgConnection = null;
        Datagram dg = null;
        try {
            // specify broadcast_port
            dgConnection = (DatagramConnection) Connector.open("radiogram://broadcast:"+ BROADCAST_PORT);

            dg = dgConnection.newDatagram(50);
            System.out.println("Maxlength for Packet is : " + dgConnection.getMaximumLength());
        } catch (IOException ex) {
            System.out.println("Could not open radiogram broadcast connection");
            ex.printStackTrace();
            return;
        }
        while(true){
            try {
                
                // 9.8mV per inch
                double val = sensor.getVoltage();
                System.out.println("Value in volts: "+ val);  
                double inches = val/0.0098;
                System.out.println("Value in inches: "+ inches);
                // Write the string into the dataGram.
                dg.reset();
                dg.writeLong(ourAddr);
                dg.writeDouble(val);

                //Send DataGram
                dgConnection.send(dg);

                // Sleep for 200 milliseconds.
                Utils.sleep(200); 
            } catch (IOException ex){
                System.err.println("Caught " + ex + " while collecting/sending sensor sample.");
                ex.printStackTrace();
            }
       }
    }

    protected void pauseApp() {}
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {}
    
}
