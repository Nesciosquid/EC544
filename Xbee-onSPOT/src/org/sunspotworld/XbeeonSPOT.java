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
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.IOException;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.IInputPin;
import java.util.Date;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This connects Xbee as a peripheral, extending radio coverage
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
