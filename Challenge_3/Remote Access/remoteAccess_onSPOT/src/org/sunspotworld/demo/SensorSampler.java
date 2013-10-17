/*
 * SensorSampler.java
 *
 * Copyright (c) 2008-2010 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITemperatureInput;
import com.sun.spot.util.Utils;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This application is the 'on SPOT' portion of the SendDataDemo. It
 * periodically samples a sensor value on the SPOT and transmits it to a desktop
 * application (the 'on Desktop' portion of the SendDataDemo) where the values
 * are displayed.
 *
 * @author: Vipul Gupta modified: Ron Goldman
 */
public class SensorSampler extends MIDlet {

    private static final int HOST_PORT = 99;
    private static final int SAMPLE_PERIOD = 3000;  // in milliseconds

    private void setWhite(ITriColorLED[] ledArray) {
        for (int i = 0; i < ledArray.length; i++) {
            ledArray[i].setRGB(255, 255, 255);
        }
        System.out.println("LEDs set to white.");

    }

    private void allOn(ITriColorLED[] ledArray) {
        setWhite(ledArray);
        for (int i = 0; i < ledArray.length; i++) {

            ledArray[i].setOn();
        }
        System.out.println("LEDs turned on.");
    }

    private void allRainbow(ITriColorLED[] ledArray) {
        System.out.println("Taste the rainbow!");
        for (int i = 0; i < ledArray.length; i++) {
            int red = (255 - (int) (((double) i / ledArray.length) * 200)) + 25;
            if (red <= 0) {
                red = 0;
            } else if (red > 255) {
                red = 255;
            }
            int blue = (int) (((double) i / ledArray.length) * 200) + 25;
            if (blue <= 0) {
                blue = 0;
            } else if (blue > 255) {
                blue = 255;
            }
            int green = 0;
            int gOffsetRed = (255 - red) + 40;
            int gOffsetBlue = (255 - blue) + 40;
            if (gOffsetRed < gOffsetBlue) {
                green = gOffsetRed * 2;
            } else {
                green = gOffsetBlue * 2;
            }

            if (green <= 0) {
                green = 0;
            } else if (green > 255) {
                green = 255;
            }
            ledArray[i].setRGB(red, green, blue);
        }
    }
    
    private void allShiftLeft(ITriColorLED[] ledArray){
        LEDColor oldColors[] = new LEDColor[ledArray.length];
        for (int i = 0; i < ledArray.length; i++){
            oldColors[i] = ledArray[i].getColor();
        }
        
        for (int i = 0; i < ledArray.length; i++){
            if (i == 0){
                ledArray[i].setColor(oldColors[(oldColors.length - 1)]);
            }
            else {
                ledArray[i].setColor(oldColors[(i-1)]);
            }    
        }
    }

    private void allPulse(ITriColorLED[] ledArray) {

        allOff(ledArray);
        allRainbow(ledArray);
        for (int i = 0; i < ledArray.length; i++) {
            allShiftLeft(ledArray);
            ledArray[i].setOn();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
        
        for (int i = 0; i < ledArray.length; i++) {
            allShiftLeft(ledArray);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }
       
        
        for (int i = 0; i < ledArray.length; i++) {
            ledArray[i].setOff();
            allShiftLeft(ledArray);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }

        System.out.println("LEDs pulsed.");
    }

    private void allOff(ITriColorLED[] ledArray) {
        for (int i = 0; i < ledArray.length; i++) {
            ledArray[i].setOff();
        }
        System.out.println("LEDs turned off.");
    }

    protected void startApp() throws MIDletStateChangeException {
        RadiogramConnection rCon = null;
        Datagram dg = null;
        //String ourAddress = System.getProperty("IEEE_ADDRESS");
        //ITemperatureInput tempSensor = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);

        ITriColorLED[] leds = new ITriColorLED[8];
        for (int i = 0; i < 8; i++) {
            String new_led_string = "LED" + (i + 1);
            ITriColorLED new_led = (ITriColorLED) Resources.lookup(ITriColorLED.class, new_led_string);
            leds[i] = new_led;
        }

        //System.out.println("Starting sensor sampler application on " + ourAddress + " ...");

        // Listen for downloads/commands over USB connection
        new com.sun.spot.service.BootloaderListenerService().getInstance().start();

        try {
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening
            rCon = (RadiogramConnection) Connector.open("radiogram://:" + HOST_PORT);
            dg = (Radiogram) rCon.newDatagram(rCon.getMaximumLength());
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
            notifyDestroyed();
        }

        System.out.println("Listening on port " + HOST_PORT + "...\n");
        
        try {
        allOn(leds);
        Thread.sleep(50);
        allOff(leds);
        allOn(leds);
        Thread.sleep(50);
        allOff(leds);
        }
        catch (InterruptedException ex){
            System.out.println(ex);
        }

        while (true) {
            try {
                // Get the current time and sensor reading
                //long now = System.currentTimeMillis();
                //double reading = tempSensor.getCelsius();



                rCon.receive(dg);
                String message = dg.readUTF();
                System.out.println("Message received: " + message);
                for (int i = 0; i < leds.length; i++) {
                }

                if (message.equals("on")) {
                    allOn(leds);

                } else if (message.equals("off")) {
                    allOff(leds);
                } else if (message.equals("pulse")) {
                    allPulse(leds);
                }
                else if (message.equals("rainbow")) {
                    allRainbow(leds);
                }
                else if (message.equals("shift")){
                    allShiftLeft(leds);
                }

            } catch (Exception e) {
                System.err.println("Caught " + e + " while collecting/sending sensor sample.");
            }
        }
    }

    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        // Only called if startApp throws any exception other than MIDletStateChangeException
    }
}
