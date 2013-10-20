/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;

/**
 *
 * @author Aaron Heuckroth
 */
public class ClientConsole {

    public static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    public static boolean cont = true;
    public static String command = "";
    public static HelloCommand new_command = null;
    public static void main(String[] args) {
        
        System.out.println("Enter commands: on/off/quit");
        while (cont) {
            try {
                command = in.readLine();
            } catch (IOException ex) {
                System.out.println(ex);
            }
            if (command.equals("quit")) {
                cont = false;
            }
            else if (command != null){

            new_command = new HelloCommand(command);
            new_command.sendCommand();
        }
    }
}
}
