/* IR Proxmity sensor code
 * 
 * According to work we did, the polynomail fit is:
 * -0.00005x^3+0.0003x^2-0.0501x+3.0987, which gave us an R^2 of 0.94843 over about
 * 12 data points
 * 
 *  But that didn't work out so we're using the polynomial from Sparkfun, which is great.
 * Also included is the old equation from Yuting, 18.67/(vol+0.167)
 *
 * Created on 5 Dec 2013, Erik Knechtel + Abhinav Nair;
 */
package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.squawk.util.MathUtils;
import java.io.IOException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 *
 * The manifest specifies this class
 *
 * @author Yuting Zhang <ytzhang@bu.edu>
 */
public class proximity extends MIDlet {

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private IAnalogInput proximity = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A2];
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));

        for (int i = 0; i < leds.size(); i++) {
            leds.getLED(i).setColor(LEDColor.GREEN);
            leds.getLED(i).setOn();
        }
        Utils.sleep(500);
        for (int i = 0; i < leds.size(); i++) {
            leds.getLED(i).setOff();
        }
        Utils.sleep(500);
        while (true) {
            try {
                double vol = proximity.getVoltage();
                System.out.println("Output voltage = " + vol + " V");
                // Have to assume distance of greater than 15cm! See datasheet Fig 2
                
                //double dis = -0.00005 * MathUtils.pow(vol,3) + 0.0003 * MathUtils.pow(vol,2) - 0.0501 * vol + 3.0987;
                double oldDis = 18.67/(vol+0.167);
                double dis = 16.2537 * MathUtils.pow(vol,4) - 129.893 * MathUtils.pow(vol,3) + 382.268 * MathUtils.pow(vol,2) - 512.611 * vol + 306.439;                
                System.out.println("NewDistance = " + dis + " cm");
                System.out.println("OldDistance = " + oldDis + " cm");
                Utils.sleep(500);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (sw1.isClosed()) {
                notifyDestroyed();                      // cause the MIDlet to exit
            }
        }


    }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system. It is not called if
     * MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true the MIDlet must cleanup and release all
     * resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        for (int i = 0; i < leds.size(); i++) {
            leds.getLED(i).setOff();
        }
    }

//    // The pow function does the math operation for x^y.
//    public double pow(double x, double y) {
//        int den = 1024; //declare the denominator to be 1024  
//        /*
//         * Conveniently 2^10=1024, so taking the square root 10 times will yield
//         * our estimate for n.��In our example n^3=8^2n^1024 = 8^683.
//         */
//        int num = (int) (y * den); // declare numerator
//        int iterations;
//        iterations = 10;
//        double n = Double.MAX_VALUE; /*
//         * we initialize our estimate, setting it to max
//         */
//        while (n >= Double.MAX_VALUE && iterations > 1) {
//            /*
//             * ��We try to set our estimate equal to the right hand side of the
//             * equation (e.g., 8^2048).��If this number is too large, we will
//             * have to rescale.
//             */
//            n = x;
//            for (int i = 1; i < num; i++) {
//                n *= x;
//            }
//            /*
//             * here, we handle the condition where our starting point is too
//             * large
//             */
//            if (n >= Double.MAX_VALUE) {
//                iterations--;
//                den = (int) (den / 2);
//                num = (int) (y * den); //redefine the numerator
//            }
//        }
//        /**
//         * ***********************************************
//         ** We now have an appropriately sized right-hand-side. * Starting with
//         * this estimate for n, we proceed.
//         * ************************************************
//         */
//        for (int i = 0; i < iterations; i++) {
//            n = Math.sqrt(n);
//        }
//        // Return our estimate
//        return n;
//    }
}