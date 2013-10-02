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
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.util.IEEEAddress;
import javax.microedition.io.*;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.util.*;
import java.sql.Timestamp;
import java.io.*;
import java.nio.channels.*;

/**
 * This application is the 'on Desktop' portion of the SendDataDemo. This host
 * application collects sensor samples sent by the 'on SPOT' portion running on
 * neighboring SPOTs and graphs them in a window.
 *
 * @author Vipul Gupta modified Ron Goldman
 */
public class RemoteAccessCommandServer {

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
    private HelloServer Server = null;
    private BufferedReader buffIn = new BufferedReader(new InputStreamReader(System.in));

    //-----------Set up 
    private void setup() {
        JFrame fr = new JFrame("Send Data Host App");
        JScrollPane sp = new JScrollPane(status);
        fr.add(sp);
        fr.setSize(360, 200);
        fr.validate();
        fr.setVisible(true);
    }

    public class ServerThread implements Runnable {

        public void run() {
            Server = new HelloServer();
            Server.run();
        }
    }

    //This is for receiving data and draw
    private void run() throws Exception {
        (new Thread(new ServerThread())).start();
        RadiogramConnection rCon = null;
        Datagram dg = null;


        //------ This part does not need to modify. dg is the received package.---------//
        try {
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening
            rCon = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HOST_PORT);
            dg = rCon.newDatagram(50);  // only sending 12 bytes of data
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
        }

        status.append("Ready to transmit signals on " + HOST_PORT + "...\n");

        System.out.println("Enter commands: on, off");

        while (true) {
            if (Server != null && dg != null && rCon != null) {
                if (Server.getCommands() != null) {
                    ArrayList<String> commands = Server.getCommands();
                    if (commands.size() > 0) {
                        System.out.println("Commands list:");

                        for (int i = 0; i < commands.size(); i++) {
                            System.out.println("You just entered: " + commands.get(i) + "\n");

                            dg.reset();
                            dg.writeUTF(commands.get(i));
                            rCon.send(dg);
                            Thread.sleep(50);

                            System.out.println("Sent message:" + commands.get(i) + "\n");
                        }
                        Server.clearCommands();
                    }
                }
            }
            Thread.sleep(250);
        }
        //Thread.sleep(3000);





    }

    //----------------------------------------------//
    // Main data collection loop
    //This part is worth attention
    //Hashtable<String, Data> Inf = new Hashtable<String, Data>();
//The main method below does not need to modify
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        // register the application's name with the OTA Command server & start OTA running
        OTACommandServer.start("TempsensorHostApplication");

        RemoteAccessCommandServer app = new RemoteAccessCommandServer();
        app.setup();
        app.run();
    }
}
