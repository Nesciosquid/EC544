/*
 * DatabaseDemoHostApplication.java
 *
 * Copyright (c) 2008-2009 Sun Microsystems, Inc.
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
package EC544.main;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.radio.IPacketQualityListener;
import com.sun.spot.peripheral.radio.RadioPacketDispatcher;
import com.sun.spot.peripheral.ota.OTACommandServer;
import com.sun.spot.util.IEEEAddress;
import javax.microedition.io.*;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.util.*;
import java.sql.Timestamp;
import java.io.*;

/**
 * This application is the 'on Desktop' portion of the SendDataDemo. This host
 * application collects sensor samples sent by the 'on SPOT' portion running on
 * neighboring SPOTs and graphs them in a window.
 *
 * @author Vipul Gupta modified Ron Goldman
 */
public class CarDataReceiver {

    // Configuration variables for destination port, sampling interval
    private static final int HOST_PORT = 42;
    private static final int SEND_INTERVAL = 60 * 1000;
    private static final String OUTPUT_FILE = "live.csv";
    private static final String INPUT_LOG = "log_input.csv";
    private static final String OUTPUT_LOG = "log_processed.csv";
    private static final String KEY_FILE = "keys.txt";
    private java.util.Date startDate = new java.util.Date();
    private Timestamp startTime = new Timestamp(startDate.getTime());
    private JTextArea status = new JTextArea();
    private ArrayList<String> acceptedKeys = new ArrayList<String>();
    private ArrayList<Datapacket> data_array = new ArrayList<Datapacket>();
    private ArrayList<ArrayList<String>> data_rows = new ArrayList<ArrayList<String>>();
    private ArrayList<String> header_names = new ArrayList<String>();
    private String logFile_output = ("./logs/" + startTime.toString().replaceAll("[^a-zA-Z0-9]+", "") + " log.csv");
    private CSVWriter logger = new CSVWriter(logFile_output);
    private CSVWriter out = new CSVWriter(OUTPUT_FILE);
    private HashMap columnIDs = new HashMap();
    private HashMap log_columnIDs = new HashMap();
    private ArrayList<String> log_header_names = new ArrayList<String>();
    private HashMap temp_offets = new HashMap();
    private double tp = 0.0;
    private HashMap temp_scales = new HashMap();
    //private ArrayList<CarPoint> outPoints = new ArrayList<CarPoint>();
    private ArrayList<SmallPoint> outPoints = new ArrayList<SmallPoint>();
    private Listener listen = new Listener();
    private int channelIndex;
    private int RECEIVE_PORT_COUNT = 1;
    Datagram[] allDatagrams = new Datagram[RECEIVE_PORT_COUNT];
    RadiogramConnection[] allConnections = new RadiogramConnection[RECEIVE_PORT_COUNT];

    class Listener implements IPacketQualityListener {

        public void notifyPacket(long src, long dest, int rssi, int corr, int lqi, int length) {
            System.out.println("Packet received!: " + System.currentTimeMillis());
        }
    }
    //-----------Set up 

    private void setup() {
        JFrame fr = new JFrame("Send Data Host App");
        JScrollPane sp = new JScrollPane(status);
        fr.add(sp);
        fr.setSize(360, 200);
        fr.validate();
        fr.setVisible(true);
    }

    private void writeLoop() {
        while (true) {

            while (outPoints.size() > 0) {
                System.out.println("in write loop, " + outPoints.size());
                System.out.println("Adding cpoint to CSV");
                out.addCSVLine(outPoints.get(0).toString());
                logger.addCSVLine(outPoints.get(0).toString());
                outPoints.remove(0);

            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("Exception" + e + " in writeLoop");

            }
        }
    }

    private SmallPoint toSmallPoint(Datagram dg, int booleans) {
        SmallPoint sp = null;
        try {
            int LF = dg.readUnsignedByte();
            int RF = dg.readUnsignedByte();
            int LR = dg.readUnsignedByte();
            int RR = dg.readUnsignedByte();
            int setTurn = dg.readUnsignedByte();
            int setSpeed = dg.readUnsignedByte();
            double time = dg.readDouble();
            sp = new SmallPoint(LF, RF, LR, RR, setTurn * 10, setSpeed * 10);
            sp.time = time;
            System.out.println("Booleans in toSmallPoint: " + booleans);
            System.out.println("setTurn in toSmallPoint: " + setTurn + ", setTurn * 10: " + setTurn*10);
            System.out.println("If it was signed, setTurn would be: " + SmallPoint.toUnsignedInt(setTurn));
            System.out.println("If it was unsigned, setTurn would be: " + SmallPoint.convertUnsignedInt(setTurn));
            System.out.println("setSpeed in toSmallPoint: " + setSpeed + ", setSpeed * 10: " + setSpeed*10);
            sp.setBooleans(booleans);
        } catch (IOException ex) {
            System.out.println("IOexception " + ex + " in toSmallPoint!");
        }

        return sp;
    }

    private CarPoint toCarPoint(Datagram dg) {
        CarPoint cp = new CarPoint();
        try {
            cp.time = dg.readFloat();
            cp.LF = dg.readFloat();
            cp.RF = dg.readFloat();
            cp.LR = dg.readFloat();
            cp.RR = dg.readFloat();
            cp.distRight = dg.readFloat();
            cp.distLeft = dg.readFloat();
            cp.LT = dg.readFloat();
            cp.RT = dg.readFloat();
            cp.thetaRight = dg.readFloat();
            cp.thetaLeft = dg.readFloat();
            cp.theta = dg.readFloat();
            cp.distance = dg.readFloat();
            cp.turn = dg.readFloat();
            cp.velocity = dg.readFloat();
            cp.targetDist = dg.readFloat();
            cp.startTurn = dg.readFloat();
            cp.stopTurn = dg.readFloat();
            cp.targetTheta = dg.readFloat();
            //System.out.println("Got CP datagram:" );
            //cp.printPoint();
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("IOException in toCarPoint");
        }

        return cp;
    }

    //This is for receiving data and draw
    private void run() throws Exception {
        RadioPacketDispatcher.getInstance().registerPacketQualityListener(listen);
        String headerLine = SmallPoint.headerRow();
        logger.initCSV(headerLine);
        out.initCSV(headerLine);
        new Thread() {
            public void run() {
                writeLoop();
            }
        }.start();

        //------ This part does not need to modify. dg is the received package.---------//
        try {
            for (int i = 0; i < RECEIVE_PORT_COUNT; i++) {
                // Open up a server-side broadcast radiogram connection
                // to listen for sensor readings being sent by different SPOTs
                allConnections[i] = (RadiogramConnection) Connector.open("radiogram://:" + (HOST_PORT + i));
                allDatagrams[i] = allConnections[i].newDatagram(allConnections[i].getMaximumLength());
            }

        } catch (Exception e) {
            System.err.println("setUp caught " + e.getMessage());
            throw e;
        }

        status.append("Listening on port " + HOST_PORT + "...\n");
        //----------------------------------------------//
        // Main data collection loop
        //This part is worth attention
        //Hashtable<String, Data> Inf = new Hashtable<String, Data>();

        for (int j = 0; j < RECEIVE_PORT_COUNT; j++) {
            channelIndex = j;
            System.out.println("Channel index:" + j);
            new Thread() {
                public void run() {
                    System.out.println("Started receive loop: " + channelIndex);
                    receiveLoop(allConnections[channelIndex], allDatagrams[channelIndex], channelIndex);

                }
            }.start();
            Thread.sleep(50);
        }

        System.out.println("Test after receive loops");

    }

    private void receiveLoop(RadiogramConnection rc, Datagram dg, int number) {
        System.out.println("Started receive loop" + number);
        while (true) {
            try {
                //System.out.println("Receive loop number: " + number);
                //System.out.println("Repeating while loop took: " + (System.currentTimeMillis() - tp) + " milliseconds.");
                // Read sensor sample received over the radio
                dg = rc.newDatagram(rc.getMaximumLength());
                double startPoint = System.currentTimeMillis();
                rc.receive(dg);
                System.out.println("Got datagram in loop: " + number);
                double timepoint = System.currentTimeMillis();
                String node = dg.getAddress(); // read address of the Spot that sent the datagram
                //String firstLine = "";
                int firstByte = 0;
                if (dg.getLength() != 0) {
                    //firstLine = dg.readUTF();
                    firstByte = dg.readUnsignedByte();
                    System.out.println("Firstbyte: " + firstByte);
                } else {
                    System.out.println("Datagram length = 0!");
                }

                /*if (firstLine.equals("CarPoint")) {
                 CarPoint cp = toCarPoint(dg);
                 //logger.addCSVLine(cp.toString());

                 outPoints.add(cp);

                 }*/

                if (SmallPoint.checkBooleans(firstByte)) {
                    SmallPoint sp = toSmallPoint(dg, firstByte);
                    outPoints.add(sp);
                    sp.printPoint();
                } else {
                    System.out.println("Received packet of unknown type!");
                }


                //System.out.println("Got carpoint, " + outPoints.size() + "in queue:  "+ System.currentTimeMillis());
                System.out.println("Receiving took: " + (System.currentTimeMillis() - tp) + " milliseconds, Processing took: " + (System.currentTimeMillis() - timepoint) + "milliseconds.");
                System.out.println("Datagram received at" + timepoint + ", Datagram size: " + dg.getLength());
                tp = System.currentTimeMillis();

            } catch (Exception e) {
                System.err.println("Caught " + e + " while reading sensor samples.");

            }
        }
    }
//The main method below does not need to modify

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        // register the application's name with the OTA Command server & start OTA running
        OTACommandServer.start("TempsensorHostApplication");

        CarDataReceiver app = new CarDataReceiver();
        app.setup();
        app.run();
    }
}
