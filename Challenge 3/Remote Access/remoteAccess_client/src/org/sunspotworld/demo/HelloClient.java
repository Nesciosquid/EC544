/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Aaron Heuckroth
 */
public class HelloClient {
 
    public static final String HELLO_REQUEST = "Hello!";

    public static void main(String[] args) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(false);
//make sure to call sc.connect() or else 
//calling sc.finishConnect() will throw 
//java.nio.channels.NoConnectionPendingException
            sc.connect(new InetSocketAddress(8888));
//if the socket has connected, sc.finishConnect() should 
//return false
            while (!sc.finishConnect()) {
//pretend to do something useful here
                System.out.println("Doing something useful...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Sending a request to HelloServer");
            ByteBuffer buffer = ByteBuffer.wrap(HELLO_REQUEST.getBytes());
            sc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sc != null) {
                try {
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}