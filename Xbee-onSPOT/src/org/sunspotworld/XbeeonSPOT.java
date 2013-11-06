/*
 * XbeeonSPOT.java
 *
 * Created on Apr 9, 2013 11:07:13 AM;
 */
package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.lang.*;
import java.util.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This connects Xbee as a peripheral, extending radio coverage
 *
 * @author Yuting Zhang <ytzhang@bu.edu>
 */
public class XbeeonSPOT extends MIDlet {

    private byte[] routerAddressA = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0xa1, (byte) 0xa1, (byte) 0x83};
    private byte[] routerAddressB = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0xa1, (byte) 0xa1, (byte) 0x47};
    private byte[] routerAddressC = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0xa1, (byte) 0xa1, (byte) 0x53};
    private byte[] routerAddressD = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0x91, (byte) 0xBC, (byte) 0x5C};

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private ILightSensor light = (ILightSensor) Resources.lookup(ILightSensor.class);
    byte[] buffer = new byte[20];
    byte[] returnAddress = new byte[8];
    String returnString = "";
    String returnHex = "";

    public void writeOutUART() {
        try {
            eDemo.readUART(buffer, 0, buffer.length * 5);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void clearUART() {

        try {
            if (eDemo.availableUART() > 1) {
                writeOutUART();
            }
            if (eDemo.availableUART() > 1) {
                System.out.println("UART Still not clear, trying again!");
                clearUART();
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void getRemoteRSSI(byte[] address, String beaconName) {
        //System.out.println("Clearing " + beaconName);
        clearUART();
        //System.out.println("Pinging " + beaconName);
        pingRSSI(address);
        try {
            Utils.sleep(100);
        } catch (Exception e) {
            System.out.println(e);
        }
        //System.out.println("Reading " + beaconName);
        getResponse(beaconName);
    }
    
    public void getLocalRSSI(){
         //byte[] addressBytes = address.getBytes();
        byte[] snd = new byte[9];
        snd[0] = (byte) 0x7E;    //start of api 
        snd[1] = (byte) 0x00;    //msb of length
        snd[2] = (byte) 0x04;    //lsb of length
        snd[3] = (byte) 0x08;
        snd[4] = (byte) 0x01;
        snd[5] = (byte) 0x44;
        snd[6] = (byte) 0x42;
        snd[7] = (byte) 0x70;

        eDemo.writeUART(snd);
    }

    public void getResponse(String beaconName) {
        try {
            if (eDemo.availableUART() == 20) {
                eDemo.readUART(buffer, 0, buffer.length);
                //printBytes(buffer);

                returnString = returnString + new String(buffer, "US-ASCII").trim();

                //System.out.println(returnString);
                //System.out.println(moteName + ": " + bytesToHexString(buffer, true));
                int DB = buffer[19]&0xFF;
                //System.out.println("DB (Hex): " + dbHex);
                System.out.println(beaconName + " [19]: " + DB);
            } else if (eDemo.availableUART() >= 1) {
             eDemo.readUART(buffer, 0, buffer.length);
             System.out.println("Response is wrong size!");

             }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        /*for (int i = 0; i < leds.size(); i++) {
         leds.getLED(i).setRGB(0, 0, 100);
         leds.getLED(i).setOn();
         }
         Utils.sleep(50);
         for (int i = 0; i < leds.size(); i++) {
         leds.getLED(i).setOff(); 
         }*/

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));

        ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");
        eDemo.initUART(9600, 8, 0, 1);
        setBoostOn();
        setPowerHigh();
        while (true) {
            if (sw2.isClosed()) {                  // done when switch is pressed
                //uartSender();
                //pingRSSI(routerAddressA);
                getRemoteRSSI(routerAddressA, "A");
                getRemoteRSSI(routerAddressB, "B");
                getRemoteRSSI(routerAddressC, "C");
                //getRSSI(routerAddressD, "D");
            }
            Utils.sleep(500);
        }
    }

    public String bytesToHexString(byte[] bytes, boolean spaces) {
        String[] byteStrings = new String[bytes.length];
        String stringOut = "";
        int currentByte;
        for (int i = 0; i < bytes.length; i++) {
            stringOut += Integer.toHexString(bytes[i]);
            if (spaces == true) {
                stringOut += " ";
            }
        }
        return stringOut;
        // prints "FF 00 01 02 03 "
    }

    public void printBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            System.out.println(bytes[i]);
        }
    }

    public void pingRSSI(byte[] address) {
        //byte[] addressBytes = address.getBytes();
        byte[] snd = new byte[19];
        snd[0] = (byte) 0x7E;    //start of api 
        snd[1] = (byte) 0x00;    //msb of length
        snd[2] = (byte) 0x0F;    //lsb of length
        snd[3] = (byte) 0x17;    // api frame for transmit
        snd[4] = (byte) 0x01;    // ack
        snd[5] = address[0];   // 64-bit addr coordinator 
        snd[6] = address[1];   // 64-bit addr coordinator 
        snd[7] = address[2];   // 64-bit addr coordinator 
        snd[8] = address[3];   // 64-bit addr coordinator 
        snd[9] = address[4];   // 64-bit addr coordinator 
        snd[10] = address[5];   // 64-bit addr coordinator 
        snd[11] = address[6];   // 64-bit addr coordinator 
        snd[12] = address[7];   // 64-bit addr coordinator 
        snd[13] = (byte) 0xFF;   //16-bit
        snd[14] = (byte) 0xFE;
        snd[15] = (byte) 0x02;  // magic byte makes the command work
        snd[16] = (byte) 0x44; // "D"
        snd[17] = (byte) 0x42; // "B" (DB command requests RSSI from target XBEE)

        byte sum = 0;
        for (int i = 3; i < snd.length - 1; i++) {
            sum += snd[i];
        }
        //       byte sumByte = (byte) (0xFF & sum);
        byte chexum = (byte) (0xff - sum);
        snd[18] = chexum;

        //printBytes(snd);
        //System.out.println("Hex String sent: " + bytesToHexString(snd, true));
        //System.out.println("Hex String Addres: " + bytesToHexString(address, true));
        //System.out.println("Man String Address manual: " + bytesToHexString(routerAddressABytes, true));
        eDemo.writeUART(snd);
        /*
         for (int i = 4; i < 8; i++) {
         leds.getLED(i).setRGB(0, 100, 0);
         leds.getLED(i).setOn();
         }
         Utils.sleep(100);
         for (int i = 4; i < 8; i++) {
         leds.getLED(i).setOff(); 
         }*/

    }

    public void setBoostOff() {
        //byte[] addressBytes = address.getBytes();
        byte[] snd = new byte[9];
        snd[0] = (byte) 0x7E;    //start of api 
        snd[1] = (byte) 0x00;    //msb of length
        snd[2] = (byte) 0x05;    //lsb of length
        snd[3] = (byte) 0x08;
        snd[4] = (byte) 0x00;
        snd[5] = (byte) 0x50;
        snd[6] = (byte) 0x4D;
        snd[7] = (byte) 0x00;
        snd[8] = (byte) 0x58;

        eDemo.writeUART(snd);

        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setRGB(100, 50, 50);
            leds.getLED(i).setOn();
        }
        Utils.sleep(100);
        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setOff();
        }
    }

    public void setBoostOn() {
        //byte[] addressBytes = address.getBytes();
        byte[] snd = new byte[9];
        snd[0] = (byte) 0x7E;    //start of api 
        snd[1] = (byte) 0x00;    //msb of length
        snd[2] = (byte) 0x05;    //lsb of length
        snd[3] = (byte) 0x08;
        snd[4] = (byte) 0x00;
        snd[5] = (byte) 0x50;
        snd[6] = (byte) 0x4D;
        snd[7] = (byte) 0x01;
        snd[8] = (byte) 0x59;

        eDemo.writeUART(snd);

        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setRGB(100, 50, 50);
            leds.getLED(i).setOn();
        }
        Utils.sleep(100);
        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setOff();
        }
    }

    public void setPowerHigh() {
        //byte[] addressBytes = address.getBytes();
        byte[] snd = new byte[9];
        snd[0] = (byte) 0x7E;    //start of api 
        snd[1] = (byte) 0x00;    //msb of length
        snd[2] = (byte) 0x05;    //lsb of length
        snd[3] = (byte) 0x08;
        snd[4] = (byte) 0x01;
        snd[5] = (byte) 0x50;
        snd[6] = (byte) 0x4c;
        snd[7] = (byte) 0x04;
        snd[8] = (byte) 0x56;

        eDemo.writeUART(snd);

        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setRGB(100, 50, 50);
            leds.getLED(i).setOn();
        }
        Utils.sleep(100);
        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setOff();
        }

    }

    public void setPowerLow() {
        //byte[] addressBytes = address.getBytes();
        byte[] snd = new byte[9];
        snd[0] = (byte) 0x7E;    //start of api 
        snd[1] = (byte) 0x00;    //msb of length
        snd[2] = (byte) 0x05;    //lsb of length
        snd[3] = (byte) 0x08;
        snd[4] = (byte) 0x01;
        snd[5] = (byte) 0x50;
        snd[6] = (byte) 0x4c;
        snd[7] = (byte) 0x00;
        snd[8] = (byte) 0x5A;

        eDemo.writeUART(snd);

        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setRGB(100, 50, 50);
            leds.getLED(i).setOn();
        }
        Utils.sleep(100);
        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setOff();
        }

    }

    public void uartSender() {
        byte[] snd = new byte[19];
        snd[0] = (byte) 0x7E;    //start of api 
        snd[1] = (byte) 0x00;    //msb of length
        snd[2] = (byte) 0x0F;    //lsb of length
        snd[3] = (byte) 0x10;    // api frame for transmit
        snd[4] = (byte) 0x01;    // ack
        snd[5] = (byte) 0x00;    // 64-bit addr coordinator 
        snd[6] = (byte) 0x00;
        snd[7] = (byte) 0x00;
        snd[8] = (byte) 0x00;
        snd[9] = (byte) 0x00;
        snd[10] = (byte) 0x00;
        snd[11] = (byte) 0xFF;
        snd[12] = (byte) 0xFF;
        snd[13] = (byte) 0xFF;   //16-bit
        snd[14] = (byte) 0xFE;
        snd[15] = (byte) 0x00;
        snd[16] = (byte) 0x00;
//        snd[17] = (byte)0x22;
//        snd[18] = (byte)0xD1;

        int val = 0;
        try {
            val = light.getValue();
        } catch (IOException ex) {
        }
        byte b = (byte) (val & 0xff);
        snd[17] = b;
        byte sum = 0;
        for (int ii = 3; ii < 18; ii++) {
            sum += snd[ii];
        }
        //       byte sumByte = (byte) (0xFF & sum);
        byte chexum = (byte) (0xff - sum);
        snd[18] = chexum;

        eDemo.writeUART(snd);

        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setRGB(0, 100, 0);
            leds.getLED(i).setOn();
        }
        Utils.sleep(100);
        for (int i = 4; i < 8; i++) {
            leds.getLED(i).setOff();
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
    }
}
