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
public class SendDataDemoGuiHostApplication {
    // Broadcast port on which we listen for sensor samples

    private java.util.Date startDate = new java.util.Date();
    private Timestamp startTime = new Timestamp(startDate.getTime());
    private static final int HOST_PORT = 99;
    private JTextArea status;
    private long[] addresses = new long[3];
    //private static final Set<String> ACCEPTED_KEYS = new HashSet<String>(Arrays.asList(new String[]{"0014.4F01.0000.4372", "0014.4F01.0000.00AE", "0014.4F01.0000.7811", "0014.4F01.0000.3560", "0014.4F01.0000.35EC"}));
    private ArrayList<String> ACCEPTED_KEYS = new ArrayList<String>();
    private DataWindow[] plots = new DataWindow[3];
    private static final int SEND_INTERVAL = 60 * 1000;
    private ArrayList<Data> data_array = new ArrayList<Data>();
    private ArrayList<String> datapoints;
    //private ObjectOutputStream outone = null;
    //private FileOutputStream outtwo = null;
    //private File file = null;
    private BufferedWriter dataOut = null;
    private BufferedReader keysIn = null;

    private class Data {

        private String address = "";
        private int data;
        private Timestamp timestamp;

        public Data() {
            timestamp = new Timestamp(0);
            data = 0;
            address = "N/A";
        }

        public String getAddress() {
            return address;
        }

        public long getTime() {
            return timestamp.getTime();
        }

        public int getData() {
            return data;
        }

        public String getTimeString() {
            return timestamp.toString();
        }

        public void setAddress(String new_address) {
            address = new_address;
        }

        public void setTime(long time) {
            timestamp.setTime(time);
        }

        public void setData(int data) {
            this.data = data;
        }
    }

    //-----------This method is fine. Don't have to modify this. 
    private void setup() {
        JFrame fr = new JFrame("Send Data Host App");
        status = new JTextArea();
        JScrollPane sp = new JScrollPane(status);
        fr.add(sp);
        fr.setSize(360, 200);
        fr.validate();
        fr.setVisible(true);
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = 0;
            plots[i] = null;
        }
    }
    //-----------------
    //This method tells the app to determine which window to draw the value

    private void generateOutput(ArrayList<Data> data_array, String filename) {

        try {
            dataOut = null;
            dataOut = new BufferedWriter(new FileWriter(filename));
        } catch (IOException ex) {
            System.out.println("Error accessing output file.");
            for (int i = 0; i < 3; i++) {
                if (dataOut == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Oh crud, we've been intercepted!");
                    }
                    try {
                        dataOut = new BufferedWriter(new FileWriter(filename));
                    } catch (IOException e2) {
                        System.out.println("Still unable to access the file. Trying " + (2 - i) + "more times...");
                    }
                }

            }
        }
        datapoints = new ArrayList<String>();

        Hashtable moteIDs = new Hashtable();
        Data datum = new Data();
        ArrayList<String> header_names= new ArrayList<String>();
        String header_row = "";
        header_names.add("time");

        for (int i = 0; i < data_array.size(); i++) {
            datum = data_array.get(i);
            if (!moteIDs.containsKey(datum.getAddress())) {
                moteIDs.put(datum.getAddress(), (i + 1));
                header_names.add(datum.getAddress());
            }
        }
        
        for (int i = 0; i< header_names.size(); i++)
        {
            if (i == header_names.size()-1){
                header_row += header_names.get(i);
            }
            else {
                header_row += header_names.get(i) + ",";
            }
        }

        int csv_width = (moteIDs.size() + 1);

        

        datapoints.add(header_row);
        
        String new_row;

        for (int i = 0; i < data_array.size(); i++) {
            new_row = "";
            datum = data_array.get(i);   

            String[] datapoint = new String[(csv_width)];
            for (int j = 0; j < (csv_width); j++) {
                if (j == 0) {
                    datapoint[j] = datum.getTimeString();
                } else {
                    if (j == moteIDs.get(datum.getAddress())) {
                        datapoint[j] = ("" + datum.getData());
                    } else {
                        datapoint[j] = "";
                    }

                }
            }
            

            for (int k = 0; k < datapoint.length; k++) {

                if (k == 0) {
                    new_row += (datapoint[k] + ",");
                } else if (k == csv_width - 1) {
                    new_row += datapoint[k];
                } else {
                    new_row += (datapoint[k] + ",");
                }
            }
            datapoints.add(new_row);
        }
        try { 
            for (int i =0; i < datapoints.size(); i++)
            {
            dataOut.write(datapoints.get(i));
            dataOut.newLine();
            }
         dataOut.close();
        }
        catch (IOException ex3)
        {
        }

    }
    
    private void addCSVLine(String newLine, String filePath)
    {
        try {
        dataOut = new BufferedWriter(new FileWriter(filePath, true));
        }
        catch (IOException ex)
        {
            
        }
    }

    private DataWindow findPlot(long addr) {
        for (int i = 0; i < addresses.length; i++) {
            if (addresses[i] == 0) {
                String ieee = IEEEAddress.toDottedHex(addr);
                status.append("Received packet from SPOT: " + ieee + "\n");
                addresses[i] = addr;
                plots[i] = new DataWindow(ieee);
                final int ii = i;
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        plots[ii].setVisible(true);
                    }
                });
                return plots[i];
            }
            if (addresses[i] == addr) {
                return plots[i];
            }
        }
        return plots[0];
    }

    //This is for receiving data and draw
    private void run() throws Exception {

        RadiogramConnection rCon;
        Radiogram dg;
        //file = new File("Data.txt");
        String dataName = ("./logs/"+startTime.toString().replaceAll("[^a-zA-Z0-9]+", "") + " log.csv");
        String dataName2 = "current_output.csv";
        //outtwo = new FileOutputStream(file);
        //outone = new ObjectOutputStream(outtwo);
        //create the average window
        //String averageWindowName = "Average Temperature starting from: " + startTime.toString();
        //DataWindow averagewindow = new DataWindow(averageWindowName);
        //averagewindow.setVisible(true);
        //-----------------------

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

        status.append("Listening...\n");
        //----------------------------------------------//
        // Main data collection loop
        //This part is worth attention
        Hashtable<String, Data> Inf = new Hashtable<String, Data>();

        dataOut = new BufferedWriter(new FileWriter(dataName, true));
        dataOut.write("address,time,value");
        dataOut.newLine();
        dataOut.close();


        while (true) {
            try {
                // Read sensor sample received over the radio
                rCon.receive(dg);
                String node = dg.getAddress();
                
                String line = null;
                keysIn = new BufferedReader(new FileReader("keys.txt"));
                ACCEPTED_KEYS.clear();
                
                while ((line = keysIn.readLine()) != null){
                    ACCEPTED_KEYS.add(line);
                }
                keysIn.close();
                
                if (ACCEPTED_KEYS.contains(node)) {
                    //DataWindow dw = findPlot(dg.getAddressAsLong());
                    long time = dg.readLong();      // read time of the reading
                    int val = (int) dg.readDouble();         // read the sensor value
                    //dw.addData(time, val);

                    Data new_data = new Data();
                    new_data.setAddress(node);
                    new_data.setTime(time);
                    new_data.setData(val);

                    data_array.add(new_data);
                    dataOut = new BufferedWriter(new FileWriter(dataName, true));
                    dataOut.write(new_data.getAddress() + "," + new_data.getTimeString() + "," + new_data.getData());
                    dataOut.newLine();
                    dataOut.close();

                    generateOutput(data_array, dataName2);

                    /*int sum = 0;
                    int validCount = 0;
                    int average = 0;

                    for (Data aPoint : Inf.values()) {
                        if (aPoint.getTime() >= time - (SEND_INTERVAL * 2)) {
                            sum += aPoint.getData();
                            validCount++;
                        }
                    }

                    if (validCount > 0) {
                        average = sum / validCount;
                    }

                    averagewindow.addData(time, average);*/
                }
                else {
                    System.out.println("Connection from " + node + "ignored, not on whitelist.");
                }
                generateOutput(data_array, dataName2);

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
        OTACommandServer.start("SendDataDemo-GUI");

        SendDataDemoGuiHostApplication app = new SendDataDemoGuiHostApplication();
        app.setup();
        app.run();
    }
}
