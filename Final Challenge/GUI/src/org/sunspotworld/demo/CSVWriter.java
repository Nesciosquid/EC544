/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.demo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.sql.Timestamp;

/**
 *
 * @author Aaron Heuckroth
 */
public class CSVWriter {
    
    private FileWriter dataOut;
    private String filePath;
    
    public CSVWriter(String new_path)
    {
        dataOut = null;
        filePath = new_path;
    }
    
    public void setFilePath(String new_path)
    {
        filePath = new_path;
    }
    
    public String getFilePath(){
        return filePath;
    }
    
    public void initCSV(String header){
        try{
        dataOut = new FileWriter(filePath);
        dataOut.write(header + System.getProperty("line.separator"));
        dataOut.flush();
        dataOut.close();
        }
        catch (IOException ex){
            System.out.println("IO Exception " + ex + " in initCSV!");
        }
    }

    public void addCSVLine(String line) {
        
        int waitDelay = 3 * 1000;

        try {
            dataOut = new FileWriter(filePath, true);
            dataOut.write(line + System.getProperty("line.separator"));
            dataOut.flush();
            dataOut.close();
        } catch (IOException ex) {
            System.out.println("Error accessing output file:" + filePath);
            for (int i = 0; i < 3; i++) {
                if (dataOut == null) {
                    try {
                        Thread.sleep(waitDelay);
                    } catch (InterruptedException e) {
                        System.out.println("Oh crud, we've been intercepted!");
                    }
                    try {
                        dataOut = new FileWriter(filePath, true);
                    } catch (IOException e2) {
                        System.out.println("Still unable to access the file. Wating " + waitDelay + "milliseconds, then trying " + (2 - i) + " more times...");
                    }
                }
            }
        }
    }

    public void StringsToCSVLine(ArrayList<String> newLine) {
        addCSVLine(convertToRow(newLine));
    }

    public void writeDatapacket(Datapacket data) {
        StringsToCSVLine(data.toStrings());
    }

    public String convertToRow(ArrayList<String> values) {
        String new_row = "";
        for (int i = 0; i < values.size(); i++) {
            if (i == values.size() - 1) { // element is the last in the ArrayList -- the end of the row!
                new_row += values.get(i);
            } else //(any element but last)
            {
                new_row += (values.get(i) + ",");
            }
        }
        return new_row;
    }

    public void generateCSV(ArrayList<ArrayList<String>> lines) {
        int waitDelay = 3 * 1000;

        try {
            dataOut = new FileWriter(filePath);
            
            for (int i = 0; i < lines.size(); i++) {
                StringsToCSVLine(lines.get(i));
                
            }
        } catch (IOException ex) {
            System.out.println("Error accessing output file:" + filePath);
            for (int i = 0; i < 3; i++) {
                if (dataOut == null) {
                    try {
                        Thread.sleep(waitDelay); 
                    } catch (InterruptedException e) {
                        System.out.println("Oh crud, we've been intercepted!");
                    }
                    try {
                        dataOut = new FileWriter(filePath);
                    } catch (IOException e2) {
                        System.out.println("Still unable to access the file. Wating " + waitDelay + "milliseconds, then trying " + (2 - i) + "more times...");
                    }
                }
            }
        }
    }
}
