/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 *
 * @author Aaron Heuckroth
 */
public class HelloServer {
    

    private static final String HELLO_REPLY = "Hello World!";

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.wrap(HELLO_REPLY.getBytes());
        ServerSocketChannel ssc = null;
        try {
            ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(8888));
            ssc.configureBlocking(false);
            while (true) {
                SocketChannel sc = ssc.accept();
                // if sc == null, that means there is no connection yet
                // do something else
                if (sc == null) {
//pretend to do something useful here
                    System.out.println("Doing something useful....");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }

                } else { // received an incoming connection
                    System.out.println("Received an incoming connection from "
                            + sc.socket().getRemoteSocketAddress());

                    printRequest(sc);
                    buffer.rewind();
                    sc.write(buffer);
                    sc.close();

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

    private static void printRequest(SocketChannel sc) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(
                sc.socket().getInputStream());
        WritableByteChannel wbc = Channels.newChannel(System.out);
        ByteBuffer b = ByteBuffer.allocate(8); // read 8 bytes 
        while (rbc.read(b) != -1) {
            b.flip();
            while (b.hasRemaining()) {
                wbc.write(b);
            }
            b.clear();
        }
    }
}
    
