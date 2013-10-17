/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.demo;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

/**
 *
 * @author Aaron Heuckroth
 */
public class HelloServer {

    private static BufferedReader in = null;
    private static WritableByteChannel wbc = null;
    private static final String HELLO_REPLY = "Hello World!";
    private ArrayList<String> commands = new ArrayList<String>();

    public BufferedReader getReader() {
        return in;
    }
    
    public void clearCommands(){
        commands = new ArrayList<String>();
        System.out.println("Commands cleared:");
        System.out.println(commands);
    }
    
    public ArrayList<String> getCommands(){
        return commands;
    }
    
    public class ClientConnection{
        
    }
    
    public void run() {
        ByteBuffer buffer = ByteBuffer.wrap(HELLO_REPLY.getBytes());
        ServerSocketChannel ssc = null;
        try {
            ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(8800));
            ssc.configureBlocking(false);
            while (true) {
                SocketChannel sc = ssc.accept();

                if (sc != null) {
                    System.out.println("Received an incoming connection from "
                            + sc.socket().getRemoteSocketAddress());

                    ReadableByteChannel rbc = Channels.newChannel(
                            sc.socket().getInputStream());
                    in = new BufferedReader(Channels.newReader(rbc, "UTF-8"));
                    while (sc.isConnected()){
                    String new_command = in.readLine();
                    if (new_command != null){
                    commands.add(new_command);
                    System.out.println("Command received: " + new_command);
                    System.out.println("Commands Array: " + commands);
                    }
                    else 
                    {
                        sc.close();
                    }
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();



        } finally {
            if (ssc != null) {
                try {
                    ssc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
