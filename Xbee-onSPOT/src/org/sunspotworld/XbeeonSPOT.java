/*
 * XbeeonSPOT.java
 *
 * Created on Apr 9, 2013 11:07:13 AM;
 */
<<<<<<< HEAD
=======

>>>>>>> 37d4cf39303143c49ac1123af2d5b567ba8db671
package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
<<<<<<< HEAD
import com.sun.spot.resources.transducers.ITriColorLED;
=======
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.SwitchEvent;
>>>>>>> 37d4cf39303143c49ac1123af2d5b567ba8db671
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.IOException;
<<<<<<< HEAD
import com.sun.spot.io.j2me.radiogram.*;
import java.lang.*;
import java.util.*;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
=======
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.IInputPin;
import java.util.Date;
>>>>>>> 37d4cf39303143c49ac1123af2d5b567ba8db671
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This connects Xbee as a peripheral, extending radio coverage
<<<<<<< HEAD
 *
 * @author Yuting Zhang <ytzhang@bu.edu>
 */

class Beacon{
    String name = "";
    float RSSI = 0.0f;
    float xPos = 0.0f;
    float yPos = 0.0f;
    byte[] address;
    
    public Beacon(String newName, byte[] newAddress, float newX, float newY){
        name = newName;
        xPos = newX;
        yPos = newY;
        address = newAddress;
    }
    
        public Beacon(String newName, byte[] newAddress){
        name = newName;
        address = newAddress;
    }
    
    public void setRSSI(float newRSSI){
        RSSI = newRSSI;
    }
    
    public float getRSSI(){
        return RSSI;
    }
    
    public void setPosition(float newX, float newY){
        xPos = newX;
        yPos = newY;
    }
    
    public String getName(){
        return name;
    }
    
    public byte[] getAddress(){
        return address;
    }
}

public class XbeeonSPOT extends MIDlet {
    
    private static final int HOST_PORT = 99;
    private static final int SAMPLE_PERIOD = 200;  // in milliseconds

    private byte[] routerAddressA = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0xa1, (byte) 0xa1, (byte) 0x83};
    private byte[] routerAddressB = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0xa1, (byte) 0xa1, (byte) 0x47};
    private byte[] routerAddressC = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0xa1, (byte) 0xa1, (byte) 0x53};
    private byte[] routerAddressD = {(byte) 0x00, (byte) 0x13, (byte) 0xa2, (byte) 0x00, (byte) 0x40, (byte) 0x91, (byte) 0xBC, (byte) 0x5C};
    
    private Beacon[] beacons ={new Beacon("A", routerAddressA), new Beacon("C", routerAddressC)};

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
    
    public void getBeaconRSSI(Beacon targetBeacon, Datagram datag, RadiogramConnection rc){
        clearUART();
        pingRSSI(targetBeacon.getAddress());
        try {
            Utils.sleep(100);
        } catch (Exception e) {
            System.out.println(e);
        }
        if (getResponse(targetBeacon.name)){
            sendRead(datag, rc, targetBeacon.getName(), targetBeacon.getRSSI());
        }
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

    public boolean getResponse(String beaconName) {
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
                return true;
            } else if (eDemo.availableUART() >= 1) {
             System.out.println("Response is wrong size!");
             }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    public boolean getResponse(Beacon targetBeacon){
        try {
            if (eDemo.availableUART() == 20) {
                eDemo.readUART(buffer, 0, buffer.length);
                int DB = buffer[19]&0xFF;
                System.out.println(targetBeacon.getName() + " [19]: " + DB);
                targetBeacon.setRSSI((float)DB);
                return true;
            } else if (eDemo.availableUART() >= 1) {
             System.out.println("Response is wrong size!");

             }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    private void sendRead(Datagram datag, RadiogramConnection rc, String beaconName, float beaconRead){
        try{
            datag.writeUTF("RSSI");
            datag.writeUTF(beaconName);
            datag.writeFloat(beaconRead);
            rc.send(datag);
            Thread.sleep(50);
            }
            catch (Exception e){
                System.out.println(e);
            }
    }
    
    /*private void sendReads(Datagram datag, RadiogramConnection rc){
        for (int i = 0; i < reads.length; i ++){
            datag.reset();
            try{
            datag.writeFloat(reads[i]);
            rc.send(datag);
            Thread.sleep(50);
            }
            catch (Exception e){
                System.out.println(e);
            }
            System.out.println("Sent read [" + i + "]: " + reads[i]);
        }
    }*/

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Starting LPS system.");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        
        RadiogramConnection rCon = null;
        Datagram dg = null;
        
                try {
            // Open up a broadcast connection to the host port
            rCon = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HOST_PORT);
            dg = rCon.newDatagram(50);  // only sending 12 bytes of data
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
        }

        ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");
        eDemo.initUART(9600, 8, 0, 1);
        setBoostOn();
        setPowerHigh();
        while (true) {
            if (sw2.isClosed()) {                  // done when switch is pressed
                //uartSender();
                //pingRSSI(routerAddressA);
                //getRemoteRSSI(routerAddressA, "A");
                //getRemoteRSSI(routerAddressB, "B");
                //getRemoteRSSI(routerAddressC, "C");
                //getRSSI(routerAddressD, "D");
                for (int i = 0; i < beacons.length; i ++){
                    getBeaconRSSI(beacons[i], dg, rCon);
                }
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

=======
 * @author Yuting Zhang <ytzhang@bu.edu>
 */



public class XbeeonSPOT extends MIDlet implements ISwitchListener {

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private ILightSensor light = (ILightSensor)Resources.lookup(ILightSensor.class);
    private IAnalogInput RSSIanalog = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
    private IInputPin digitalRSSI = EDemoBoard.getInstance().getIOPins()[EDemoBoard.D2];
    long sendTime;
long receiveTime;
        ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");

    public void switchPressed(SwitchEvent sw) {
        if (sw.getSwitch() == sw2) {
            uartSender();
    }
    }

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host
        
        for (int i = 0; i < leds.size(); i++) {
                        leds.getLED(i).setRGB(0, 0, 100);
                        leds.getLED(i).setOn();
                    }
        Utils.sleep(100);
        for (int i = 0; i < leds.size(); i++) {
                        leds.getLED(i).setOff(); 
                    }
        
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));

        sw2.addISwitchListener(this);
        eDemo.initUART(9600, 8, 0, 1);
   
        while(true){
        /*if (sw2.isClosed()) {                  // done when switch is pressed
            uartSender();
            Utils.sleep(1000);                  // wait 1 second
        }*/
        byte[] buffer = new byte[20];
        String returnString = "";
        try{
        if(eDemo.availableUART()>1){
            receiveTime = System.currentTimeMillis();
            eDemo.readUART(buffer, 0, buffer.length);
            returnString = returnString + new String(buffer,"US-ASCII").trim();
            System.out.println(returnString);
            System.out.println("Send/receive delay: " + (receiveTime - sendTime));
            System.out.println("RSSI value: " + RSSIanalog);
            int pulse = EDemoBoard.getInstance().getPulse(digitalRSSI, true, 0);
            System.out.println("pulse: " + pulse);
            //for (int i = 0; i < 4; i++) {
            //leds.getLED(i).setRGB(100, 0, 0);
            //leds.getLED(i).setOn();
            //}
            /*Utils.sleep(100);
            for (int i = 0; i < 4; i++) {
                leds.getLED(i).setOff(); 
            }*/
        }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        Utils.sleep(100);
        }
        
    }
    public void uartSender(){
        byte[] snd = new byte[19];
        snd[0] = (byte)0x7E;    //start of api 
        snd[1] = (byte)0x00;    //msb of length
        snd[2] = (byte)0x0F;    //lsb of length
        snd[3] = (byte)0x10;    // api frame for transmit
        snd[4] = (byte)0x01;    // ack
        snd[5] = (byte)0x00;    // 64-bit addr coordinator 
        snd[6] = (byte)0x00;
        snd[7] = (byte)0x00;
        snd[8] = (byte)0x00;
        snd[9] = (byte)0x00;
        snd[10] = (byte)0x00;
        snd[11] = (byte)0xFF;
        snd[12] = (byte)0xFF;
        snd[13] = (byte)0xFF;   //16-bit 
        snd[14] = (byte)0xFE;
        snd[15] = (byte)0x00;
        snd[16] = (byte)0x00;
//        snd[17] = (byte)0x22;
//        snd[18] = (byte)0xD1;
        
        int val = 0;
        try{
            val = light.getValue();
        }catch(IOException ex){}
        System.out.println("val: " + val);
        byte b = (byte)(val & 0xff);
        snd[17] = b;
        System.out.println("b: " + b);
        byte sum = 0;
        for (int ii=3; ii<18;ii++){
            sum +=snd[ii];
        }
 //       byte sumByte = (byte) (0xFF & sum);
        byte chexum = (byte)(0xff-sum);
        snd[18] = chexum;
        
        eDemo.writeUART(snd);
        sendTime = System.currentTimeMillis();
        
        /*for (int i = 4; i < 8; i++) {
            leds.getLED(i).setRGB(0, 100, 0);
            leds.getLED(i).setOn();
        }*/
        Utils.sleep(100);
        /*for (int i = 4; i < 8; i++) {
            leds.getLED(i).setOff(); 
        }*/
            
    }
    
    public void switchReleased(SwitchEvent sw) {
        // do nothing
    }


    
    
    
>>>>>>> 37d4cf39303143c49ac1123af2d5b567ba8db671
    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    /**
<<<<<<< HEAD
     * Called if the MIDlet is terminated by the system. It is not called if
     * MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true the MIDlet must cleanup and release all
     * resources.
=======
     * Called if the MIDlet is terminated by the system.
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true the MIDlet must cleanup and release all resources.
>>>>>>> 37d4cf39303143c49ac1123af2d5b567ba8db671
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}
