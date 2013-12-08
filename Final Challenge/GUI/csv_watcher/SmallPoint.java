/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Aaron Heuckroth
 */
public class SmallPoint {

    /* To transmit SmallPoints as Datagrams, send booleansToUnsignedInt() first.
     * then send leftFront, rightFront, leftRear, rightRear as unsigned ints
     * using toUnsignedInt(). Next, send setTurn and setSpeed using servoValueToUnsignedInt().
     * Last, send the timestamp as a double.
     */
    //RF - LR are sensor readings IN CENTIMETERS
    static int SMALLPOINT_INT_BYTES = 6;
    public double time;
    public int leftFront;
    public int rightFront;
    public int leftRear;
    public int rightRear;
    //setTurn and setSpeed are the servo values 
    public int setTurn; // servo value / 10;
    public int setSpeed; // servo value / 10;
    //booleans to determine car state 
    public boolean isColliding = false;
    public boolean isCornering = false;
    public boolean isHonking = false;
    public boolean isParking = false;
    public boolean isLocked = false;
    public boolean bool5 = false;
    //timestamp, must be a double. :(

    public SmallPoint(double LF, double RF, double LR, double RR, int turn, int speed) {
        leftFront = readToInt(LF);
        rightFront = readToInt(RF);
        leftRear = readToInt(LR);
        rightRear = readToInt(RR);
        setTurn = turn;
        setSpeed = speed;
        time = System.currentTimeMillis();
    }

    public SmallPoint(int LF, int RF, int LR, int RR, int turn, int speed) {
        leftFront = LF;
        rightFront = RF;
        leftRear = LR;
        rightRear = RR;
        setTurn = turn;
        setSpeed = speed;
        time = System.currentTimeMillis();
    }

    public static boolean checkBooleans(int unsigned) {
        if ((unsigned / 128) > 0) {
            if ((unsigned - 128 / 64) > 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public static String headerRow(){
      return "time,LF,RF,LR,RR,setTurn,setSpeed,booleans";
    }

    public void setBooleans(int unsignedBooleans) {
        if (unsignedBooleans / 128 > 0) {
            unsignedBooleans -= 128;
            //No boolean, used to identify SmallPoints
        }
        if (unsignedBooleans / 64 > 0) {
            unsignedBooleans -= 64;
            //No boolean, used to identify SmallPoints
        }
        if (unsignedBooleans / 32 > 0) {
            unsignedBooleans -= 32;
            isColliding = true;
        }
        if (unsignedBooleans / 16 > 0) {
            unsignedBooleans -= 16;
            isCornering = true;
        }
        if (unsignedBooleans / 8 > 0) {
            unsignedBooleans -= 8;
            isHonking = true;
        }
        if (unsignedBooleans / 4 > 0) {
            unsignedBooleans -= 4;
            isParking = true;
        }
        if (unsignedBooleans / 2 > 0) {
            unsignedBooleans -= 2;
            isLocked = true;
        }
        if (unsignedBooleans / 1 > 0) {
            unsignedBooleans -= 1;
            bool5 = true;
        }
    }

    public boolean isASmallPoint() { //checks to see if first two bits are '11'
        int unsigned = convertUnsignedInt(booleansToUnsignedInt());
        return checkBooleans(unsigned);
    }

    //Should be in the range 100 to 200.
    public void setTurn(int turn) {
        setTurn = turn;
    }
    
    public static int convertUnsignedInt(int unsigned){
        if (unsigned < 0){
            return 127 - unsigned;
        }
        else return unsigned;
    }

    public void setSpeed(int speed) {
        setSpeed = speed;
    }

    public static int servoValueToUnsignedInt(int setTurn) {
         int out = 0;
         out = toUnsignedInt(setTurn / 10);
        return toUnsignedInt(setTurn / 10);
    }

    public int booleansToUnsignedInt() {
        int out = 0 + 128 + 64; // two bits of byte will be 1 and 1, for datagram recognition
        if (isColliding == true) {
            out += 1;
        }
        if (isCornering == true) {
            out += 2;
        }

        if (isHonking == true) {
            out += 4;
        }
        if (isParking == true) {
            out += 8;
        }
        if (isLocked == true) {
            out += 16;
        }
        if (bool5 == true) {
            out += 32;
        }
        return toUnsignedInt(out);
    }

    public static int readToInt(double read) {
        int smallRead = 0;
        smallRead = (int) read;
        if (smallRead > 255) {
            smallRead = 255;
        } else if (smallRead < 00) {
            smallRead = 0;
        }
        return smallRead;
    }

    public static int toUnsignedInt(int v) {
        if (v > 127) {
            return 127 - v;
        } else if (v <= 0) {
            return 0;
        } else {
            return v;
        }
    }

    public String toString() {
        String outputLine = time + "," + leftFront + "," + rightFront + "," + leftRear + "," + rightRear + "," + setTurn + "," + setSpeed + "," + booleansToUnsignedInt();
        return outputLine;
    }

    public void printPoint() {
        System.out.println("Time: " + time + ", RF: " + rightFront + ", LF:" + leftFront + ", RR: " + rightRear + ", LR: " + leftRear + ", setTurn: " + setTurn + ", setSpeed: " + setSpeed + ", Booleans: " + convertUnsignedInt(booleansToUnsignedInt()));
    }
}
