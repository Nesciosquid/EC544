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
    
    

    private void parseLog() {
        ArrayList<Datapacket> new_data = new ArrayList<Datapacket>();

        log_columnIDs.put("time", 0);
        log_header_names.add("time");

        new_data = pullDataFromCSV(INPUT_LOG);
        for (int i = 0; i < new_data.size(); i++) {
            Datapacket new_packet = new_data.get(i);
            if (!log_columnIDs.containsKey(new_packet.getAddress())) {
                log_columnIDs.put(new_packet.getAddress(), log_columnIDs.size());
                log_header_names.add(new_packet.getAddress());
            }
        }
        System.out.println(generateOutput(new_data, OUTPUT_LOG, log_columnIDs, log_header_names));
    }

    private ArrayList<Datapacket> pullDataFromCSV(String targetFile) {
        ArrayList<Datapacket> newData = new ArrayList<Datapacket>();
        Timestamp newTime;
        String newAddress;
        double newValue;

        ArrayList<String> lines = new ArrayList<String>();
        String[] currentTokens;
        ArrayList<ArrayList<String>> dataStrings = new ArrayList<ArrayList<String>>();
        ArrayList<String> newDataString;

        String line;
        try {
            BufferedReader dataIn = new BufferedReader(new FileReader(targetFile));
            while ((line = dataIn.readLine()) != null) {
                lines.add(line);
            }
            dataIn.close();
        } catch (IOException e) {
        }
        lines.remove(0);
        for (int i = 0; i < lines.size(); i++) {
            newDataString = new ArrayList<String>();
            currentTokens = lines.get(i).split(",");
            for (int j = 0; j < currentTokens.length; j++) {
                newDataString.add(currentTokens[j]);
            }

            newAddress = newDataString.get(0);
            newTime = Timestamp.valueOf(newDataString.get(1));

            newValue = Double.parseDouble(newDataString.get(2));

            newData.add(new Datapacket(newAddress, newTime, newValue));
        }

        return newData;
    }

    private ArrayList<ArrayList<String>> generateOutput(ArrayList<Datapacket> data_array, String filename, HashMap columns, ArrayList<String> headers) {

        data_rows.clear();
        data_rows.add(headers);

        Datapacket datum;
        ArrayList<String> new_row;

        for (int i = 0; i < data_array.size(); i++) {
            datum = data_array.get(i);
            new_row = new ArrayList<String>();

            for (int j = 0; j < columns.size(); j++) {
                if (j == 0) {
                    new_row.add("" + datum.getTimeString());
                } else {
                    if (j == columns.get(datum.getAddress())) {
                        new_row.add("" + datum.getData());
                    } else {
                        new_row.add("");
                    }
                }
            }
            data_rows.add(new_row);
        }

        CSVWriter outputWriter = new CSVWriter(filename);

        outputWriter.generateCSV(data_rows);
        return data_rows;

    }

    private void readKeys() throws IOException { // exception handling not working correctly --
        String line;          //won't crash the app, but needs to be fixed.
        BufferedReader keysIn = null;
        int waitDelay = 3000;
        try {
            keysIn = new BufferedReader(new FileReader(KEY_FILE));
            acceptedKeys = new ArrayList<String>();
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
    
    public class TempScaler {
        
        double scale;
        double offset;
        
        public TempScaler(){
            scale = 1;
            offset = 0;
        }
        
        public TempScaler(double scal, double off)
        {
            offset = off;
            scale = scal;
        }
        
        public double getScaledTemp(double val){
            return (scale*val + offset);
        }
    }

    public class updater implements Runnable {

        public void run() {
            while (true) {

                generateOutput(data_array, OUTPUT_FILE, columnIDs, header_names);
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                }

            }
        }
    }

    //This is for receiving data and draw
    private void run() throws Exception {
        (new Thread(new updater())).start();
        parseLog();
        RadiogramConnection rCon;
        Radiogram dg;
                
        columnIDs.put("time", 0);
        logger.addCSVLine("address,time,value");
        header_names.add("time");
        
        temp_offets.put("0014.4F01.0000.7811", 50);
        temp_scales.put("0014.4F01.0000.7811", 2);

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
                    double val = dg.readDouble(); // read the sensor value
                    TempScaler newTemp;
                    
                    if (temp_offets.containsKey(node)){
                        
                    }

                    Datapacket new_data = new Datapacket(node, time, val);
                    logger.writeDatapacket(new_data);

                    if (!columnIDs.containsKey(node)) {
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

                } else {
                    System.out.println("Connection from " + node + "ignored, not on whitelist.");
                }
                //generateOutput(data_array, OUTPUT_FILE, columnIDs, header_names);

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
