/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.demo;
import java.sql.Timestamp;
import java.util.*;

/**
 *
 * @author Aaron Heuckroth
 */
public class Datapacket {

        private String address = "";
        private Timestamp timestamp;
        private int value;
        private ArrayList<String> dataStrings = new ArrayList<String>();

        public Datapacket() {
            timestamp = new Timestamp(0);
            value = 0;
            address = "N/A";
        }
        
        public Datapacket(String addr, long time, int val)
        {
            timestamp = new Timestamp(time);
            value = val;
            address = addr;
        }
        
        public Datapacket(String addr, Timestamp time, int val)
        {
            timestamp = time;
            value = val;
            address = addr;
        }

        public String getAddress() {
            return address;
        }

        public ArrayList<String> toStrings() {
            dataStrings.clear();
            dataStrings.add(""+address);
            dataStrings.add(""+timestamp);
            dataStrings.add(""+value);
            return dataStrings;
        }
        
        public long getTime() {
            return timestamp.getTime();
        }

        public int getData() {
            return value;
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
            this.value = data;
        }
    }
