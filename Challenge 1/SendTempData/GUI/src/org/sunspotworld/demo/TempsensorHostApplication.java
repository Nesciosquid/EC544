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
    private static final int HOST_PORT = 99;
    private static final int SEND_INTERVAL = 60 * 1000;
    private static final String OUTPUT_FILE = "current_output.csv";
    private static final String KEY_FILE = "keys.txt";
    
    private java.util.Date startDate = new java.util.Date();
    private Timestamp startTime = new Timestamp(startDate.getTime());
    private JTextArea status = new JTextArea();
    private ArrayList<String> acceptedKeys = new ArrayList<String>();
    private ArrayList<Datapacket> data_array = new ArrayList<Datapacket>();
    private ArrayList<ArrayList<String>> data_rows = new ArrayList<ArrayList<String>>();
    private ArrayList<String> seen_motes;
    private ArrayList<String> header_names = new ArrayList<String>();
    private BufferedReader keysIn = null;
    private String logFile = ("./logs/" + startTime.toString().replaceAll("[^a-zA-Z0-9]+", "") + " log.csv");
    private CSVWriter logger = new CSVWriter(logFile);
    private CSVWriter outputWriter = new CSVWriter(OUTPUT_FILE);
    private BufferedWriter dataOut;
    private HashMap columnIDs = new HashMap();

    //-----------Set up 
    private void setup() {
        JFrame fr = new JFrame("Send Data Host App");
        JScrollPane sp = new JScrollPane(status);
        fr.add(sp);
        fr.setSize(360, 200);
        fr.validate();
        fr.setVisible(true);
    }

    private void generateOutput(ArrayList<Datapacket> data_array, String filename) {

        data_rows.clear();
        data_rows.add(header_names);
        
        Datapacket datum = new Datapacket();
        ArrayList<String> new_row = new ArrayList<String>();

        for (int i = 0; i < data_array.size(); i++) {
            datum = data_array.get(i);
            new_row = new ArrayList<String>();

            for (int j = 0; j < columnIDs.size(); j++) {
                if (j == 0) {
                    new_row.add(datum.getTimeString());
                } else {
                    if (j == columnIDs.get(datum.getAddress())) {
                        new_row.add("" + datum.getData());
                    } else {
                        new_row.add("");
                    }
                }
            }
            data_rows.add(new_row);
        }
           outputWriter.generateCSV(data_rows);
       
       outputWriter.generateCSV(data_rows);

    }

    private void readKeys() throws IOException { // exception handling not working correctly --
        String line;          //won't crash the app, but needs to be fixed.
        keysIn = null;
        int waitDelay = 3000;
        try {
            keysIn = new BufferedReader(new FileReader(KEY_FILE));
            acceptedKeys.clear();
            while ((line = keysIn.readLine()) != null) {
                acceptedKeys.add(line);
            }
            keysIn.close();

        } catch (IOException ex) {
            System.out.println("Error accessing input file:" + KEY_FILE);
            for (int i = 0; i < 3; i++) {
                if (keysIn == null) {
                    try {
                        Thread.sleep(waitDelay);
                    } catch (InterruptedException e) {
                        System.out.println("Oh crud, we've been intercepted!");
                    }
                    try {
                        keysIn = new BufferedReader(new FileReader(KEY_FILE));
                    } catch (IOException e2) {
                        System.out.println("Still unable to access the file.");
                        System.out.println("Wating " + waitDelay + "milliseconds, then trying " + (2 - i) + " more times...");
                    }
                }
                throw ex;
            }
            
        }

    }

    //This is for receiving data and draw
    private void run() throws Exception {

        RadiogramConnection rCon;
        Radiogram dg;
        columnIDs.put("time", 0);
        logger.addCSVLine("address,time,value");
        header_names.add("time");

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
            try {
                // Read sensor sample received over the radio
                rCon.receive(dg);
                String node = dg.getAddress(); // read address of the Spot that sent the datagram

                readKeys();

                if (acceptedKeys.contains(node)) {
                    //DataWindow dw = findPlot(dg.getAddressAsLong());
                    long time = dg.readLong();      // read time of the reading
                    int val = (int) dg.readDouble(); // read the sensor value

                    Datapacket new_data = new Datapacket(node, time, val);
                    logger.writeDatapacket(new_data);
                    
                    if (!columnIDs.containsKey(node))
                    {
                            columnIDs.put(node, columnIDs.size());
                            header_names.add(node);
                            System.out.println("Adding " + node + " to list of column IDs as position " + columnIDs.get(node));
                            System.out.println("New list of column ids:");
                            System.out.println(columnIDs);
                            System.out.println("Header names should match.");
                            System.out.println("New list of header names:");
                            System.out.println(header_names);
                    }
                    data_array.add(new_data);
                    generateOutput(data_array, OUTPUT_FILE);

                } else {
                    System.out.println("Connection from " + node + "ignored, not on whitelist.");
                }
                generateOutput(data_array, OUTPUT_FILE);

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
