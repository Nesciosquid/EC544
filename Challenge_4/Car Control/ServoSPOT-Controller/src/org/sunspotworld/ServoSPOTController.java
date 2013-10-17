/*
 * ServoSPOTController.java
 *
 * Created on Jul 19, 2012 9:46:39 PM;
 */

package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.transducers.IAccelerometer3D;

import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.lib.BlinkenLights;
import org.sunspotworld.lib.LedUtils;
import org.sunspotworld.common.Globals; //
import org.sunspotworld.common.TwoSidedArray; 

/**
 * This class is used to control a servo car remotely. This sends values
 * measured by demoboard accelerometer to the servo car.
 * 
 * You must specify buddyAddress, that is the SPOT address on the car to
 * communicate each other.
 * 
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class ServoSPOTController extends MIDlet implements ISwitchListener{

    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private IAccelerometer3D accel = (IAccelerometer3D)Resources.lookup(IAccelerometer3D.class);
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ISwitch sw1 = eDemo.getSwitches()[EDemoBoard.SW1];
    private ISwitch sw2 = eDemo.getSwitches()[EDemoBoard.SW2];
    private int st = 0;
    
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);
        BootloaderListenerService.getInstance().start();  
        
        for (int i = 0; i < myLEDs.size(); i++) {
                        myLEDs.getLED(i).setColor(LEDColor.GREEN);
                        myLEDs.getLED(i).setOn();
                    }
        Utils.sleep(500);
        for (int i = 0; i < myLEDs.size(); i++) {
                        myLEDs.getLED(i).setOff(); 
                    }
        
        BlinkenLights blinker = new BlinkenLights();
        blinker.startPsilon();

        String buddyAddress = getAppProperty("buddyAddress");
        if (buddyAddress == null) {
            throw new RuntimeException("the property buddyAddress must be set in the manifest");
        }
        TwoSidedArray controller = new TwoSidedArray(buddyAddress);

        try {
            controller.startOutput();
         
        //    accel.setRestOffsets();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        blinker.setColor(LEDColor.BLUE);
        while (true) {
            try {
                controller.setVal(0, (int) (accel.getTiltX() * 100));
                controller.setVal(1, st);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Utils.sleep(20);
        }
    }

    public void switchPressed(SwitchEvent sw) {
         if (sw.getSwitch() == sw1) {
             st++;
             if (st > 1) {
                  st = 1;
             }
        } else if (sw.getSwitch() == sw2) {
            st--;
             if(st < -1) {
                 st = -1;
             }
        } else st = 0;
         
     }
    
    public void switchReleased(SwitchEvent sw) {
    // do nothing
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
        LedUtils.setOffAll();
    }
}
