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
package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.*;

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
public class TempsensorHostApplication {

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
    private HashMap temp_scales = new HashMap();

    //-----------Set up 
    private void setup() {
        JFrame fr = new JFrame("Send Data Host App");
        JScrollPane sp = new JScrollPane(status);
        fr.add(sp);
        fr.setSize(360, 200);
        fr.validate();
        fr.setVisible(true);
    }
    
    private CarPoint toCarPoint(Datagram dg){
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
        }
        catch (IOException e){
            System.out.println(e);
            System.out.println("IOException in toCarPoint");
        }
        
        return cp;
    }

    //This is for receiving data and draw
    private void run() throws Exception {
        RadiogramConnection rCon;
        Radiogram dg;
        String headerLine = "time,LF,RF,LR,RR,LT,RT,distRight,distLeft,thetaRight,thetaLeft,theta,distance,turn,velocity,targetDist,startTurn,stopTurn,targetTheta";
        logger.initCSV(headerLine);
        out.initCSV(headerLine);

        //------ This part does not need to modify. dg is the received package.---------//
        try {
            // Open up a server-side broadcast radiogram connection
            // to listen for sensor readings being sent by different SPOTs
            rCon = (RadiogramConnection) Connector.open("radiogram://:" + HOST_PORT);
            dg = (Radiogram) rCon.newDatagram(rCon.getMaximumLength());
        } catch (Exception e) {
            System.err.println("setUp caught " + e.getMessage());
            throw e;
        }

        status.append("Listening on port " + HOST_PORT + "...\n");
        //----------------------------------------------//
        // Main data collection loop
        //This part is worth attention
        //Hashtable<String, Data> Inf = new Hashtable<String, Data>();

        while (true) {
            System.out.println("In the while loop");
            try {
                // Read sensor sample received over the radio
                rCon.receive(dg);
                String node = dg.getAddress(); // read address of the Spot that sent the datagram
                String firstLine = dg.readUTF();
                if(firstLine.equals("CarPoint")){
                    CarPoint cp = toCarPoint(dg);
                    
                    logger.addCSVLine(cp.toString());
                    out.addCSVLine(cp.toString());
                }
                else System.out.println("Packet of unknown type: " + firstLine);
            } catch (Exception e) {
                System.err.println("Caught " + e + " while reading sensor samples.");
                throw e;
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

        TempsensorHostApplication app = new TempsensorHostApplication();
        app.setup();
        app.run();
    }
}