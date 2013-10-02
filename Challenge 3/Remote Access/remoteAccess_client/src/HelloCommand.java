/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.*;

/**
 *
 * @author Aaron Heuckroth
 */
public class HelloCommand {

    public static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    public static final String HELLO_REQUEST = "on";
    public static String command = "";
    public static boolean cont = true;
    public static int port = 8800;
    public static String hostname = "155.41.55.18";
    public static InetSocketAddress addr = new InetSocketAddress(hostname, port);

    public HelloCommand(String message) {
        command = message;
    }

    public static void sendCommand() {

        System.out.println("Sending a request to HelloServer: " + command);
        //ByteBuffer buffer = ByteBuffer.wrap(HELLO_REQUEST.getBytes());

        SocketChannel sc = null;

        try {
            sc = SocketChannel.open();
            sc.configureBlocking(false);
//make sure to call sc.connect() or else 
//calling sc.finishConnect() will throw 
//java.nio.channels.NoConnectionPendingException
            sc.connect(addr);
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
            ByteBuffer buffer = ByteBuffer.wrap(command.getBytes());
            sc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (sc != null) {
                try {
                    if (sc != null) {
                        sc.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
