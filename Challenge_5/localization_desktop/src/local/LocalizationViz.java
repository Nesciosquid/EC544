/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import processing.core.*;

/**
 *
 * @author Kirrei
 */
public class LocalizationViz extends PApplet {

    boolean jitter;
    boolean offset_mouse;
    float inset;
    float defaultRadius;
    float innerWidth;
    float innterHeight;
    PFont f;
    public static float targetX;
    public static float targetY;
    public static float targetXdev;
    public static float targetYdev;
    boolean wait;
    float mouseOffset;
    public static float[] Xguesses;
    public static float[] Yguesses;
    public static float[] Xstdevs;
    public static int guesses;
    float mousePointX;
    float mousePointY;
    float[] Ystdevs;
    static Beacon[] beacons;
    static int guessCount;
    float mOff;
    public static float[] reads;
    public static boolean isSimulating;

    public void setup() {
        smooth();
        isSimulating = false;
        reads = new float[4];
        wait = false;
        beacons = new Beacon[3];
        jitter = false;
        offset_mouse = false;
        frameRate(60);
        size(800, 800);
        guesses = 5;
        mousePointX = 0.0f;
        mousePointY = 0.0f;
        guessCount = 0;
        Xguesses = new float[guesses];
        Yguesses = new float[guesses];
        targetX = 0.0f;
        targetY = 0.0f;
        targetXdev = 0.0f;
        targetYdev = 0.0f;
        inset = 200;
        defaultRadius = 150.0f;
        innerWidth = width - inset;
        innterHeight = height - inset;
        mOff = 50.0f;
        mouseOffset = 0.0f;
        f = createFont("Arial", 32, true);
        textFont(f);
        background(255);
        stroke(0);
        noFill();
        beacons[0] = new Beacon(inset, inset, "C");
        beacons[1] = new Beacon(inset+250, inset, "A");
        beacons[2] = new Beacon(inset, inset+250, "E");
        //beacons[2] = new Beacon(inset, innerWidth+inset/2, "D");
        //beacons[3] = new Beacon(innerWidth, innerWidth, "D");
    }

    public static float calcDistance(float x1, float y1, float x2, float y2) {
        float xsum = x1 - x2;
        float ysum = y1 - y2;
        float xsq = xsum * xsum;
        float ysq = ysum * ysum;
        return (float) Math.sqrt(ysq + xsq);
    }

    public static void forceGuess() {
        if (guessCount >= guesses){
            guessCount = 0;
        }
        displayBeacons();
        guess(beacons[0].myIntersections);
    }

    public static void setReads(float[] newReads) {
        for (int i = 0; i < newReads.length; i++) {
            reads[i] = newReads[i];
        }
        updateBeacons();
    }
    
    public static void setRead(String name, float newRead){
        for (int i =0; i < beacons.length; i++){
            if (beacons[i].myName.equals(name)){
                reads[i] = newRead;
            }
        }
        updateBeacons();
    }

    public static void setTarget(float newX, float newY, float newXDev, float newYDev) {
        targetX = newX;
        targetY = newY;
        targetXdev = newXDev;
        targetYdev = newYDev;
    }

    public static void updateIntersections() {
        beacons[0].clearIntersections();
        for (int i = 0; i < beacons.length; i++) {
            for (int j = i + 1; j < beacons.length; j++) {
                beacons[0].getIntersections(beacons[i], beacons[j]);
            }
        }
    }

    public void keyPressed() {
        if (key == CODED) {
            if (keyCode == UP) {
                if (jitter == true) {
                    jitter = false;
                } else {
                    jitter = true;
                    offset_mouse = true;
                }
            } else if (keyCode == DOWN) {
                if (offset_mouse == true) {
                    offset_mouse = false;
                } else {
                    offset_mouse = true;
                }
            }
        }
    }

    public static Intersect[] sortInts(Intersect[] oldInts, float avgx, float avgy) {
        Intersect[] ints = new Intersect[oldInts.length];
        for (int i = 0; i < oldInts.length; i++) {
            ints[i] = oldInts[i];
        }
        boolean swapped = true;
        int j = 0;
        Intersect tmp;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < ints.length - j; i++) {
                float curDist = calcDistance(ints[i].xpos, ints[i].ypos, avgx, avgy);
                float nextDist = calcDistance(ints[i + 1].xpos, ints[i + 1].ypos, avgx, avgy);
                if (curDist > nextDist) {
                    tmp = ints[i];
                    ints[i] = ints[i + 1];
                    ints[i + 1] = tmp;
                    swapped = true;
                }
            }
        }
        return ints;
    }

    public static void updateBeacons() {
        for (int i = 0; i < beacons.length; i++) {
            if (!isSimulating){
            beacons[i].setRadius(reads[i]*75); // reads are in meters, convert 1 meter to 50 pixels
            }
            else 
                beacons[i].setRadius(reads[i]);
        }
        updateIntersections();
    }

    public void mousePressed() {
        mousePointX = mouseX;
        mousePointY = mouseY;
        if (mouseButton == LEFT) {
            wait = false;
        }
        if (mouseButton == RIGHT) {
            wait = true;
        }
    }

    public static float average(float[] array) {
        float sum = 0f;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum / array.length;
    }

    public static float stDev(float[] array) {
        float avg = average(array);
        float sum = 0.0f;
        for (int i = 0; i < array.length; i++) {
            sum += Math.pow((array[i] - avg), 2);
        }
        return (float) Math.sqrt(sum / array.length);
    }

    static void displayBeacons() {
        for (int i = 0; i < beacons.length; i++) {
            beacons[i].display();
        }
        for (int i = 0; i < beacons.length; i++) {
            beacons[i].drawInts();
        }
    }

    void simulate() {
        if (offset_mouse) {
            mouseOffset = mOff;
        } else {
            mouseOffset = 0.0f;
        }

        if (!jitter) {
            float newRadius = 0.0f;
            for (int i = 0; i < beacons.length; i++) {
                newRadius = beacons[i].calcDistance(mousePointX, mousePointY) + mouseOffset;
                if (newRadius > 0) {
                    reads[i] = newRadius;
                } else {
                    reads[i] = 0.0f;
                }
            }
        } else {
            float newRadius = 0.0f;
            for (int i = 0; i < beacons.length; i++) {
                newRadius = beacons[i].calcDistance(mousePointX, mousePointY) + random(-mouseOffset, mouseOffset);
                if (newRadius > 0) {
                    reads[i] = newRadius;
                } else {
                    reads[i] = 0.0f;

                }
            }
        }
        updateBeacons();
    }

    public static void guess(Intersect[] ints) {
        if (guessCount >= guesses) {
            guessCount = 0;
        }
        float ysum = 0.0f;
        float xsum = 0.0f;
        float yavg = 0.0f;
        float xavg = 0.0f;

        for (int i = 0; i < ints.length; i++) {
            xsum += ints[i].xpos;
            ysum += ints[i].ypos;
        }

        xavg = xsum / ints.length;
        yavg = ysum / ints.length;

    //ellipse(xavg,yavg,25,25);
        //text("All Avg",xavg+20,yavg-20);
        xsum = 0.0f;
        ysum = 0.0f;

        Intersect[] better = sortInts(ints, xavg, yavg);
        for (int i = 0; i < better.length / 2; i++) {
            xsum += better[i].xpos;
            ysum += better[i].ypos;
        }
        xavg = xsum / (ints.length / 2);
        yavg = ysum / (ints.length / 2);

  //ellipse(xavg,yavg, 25,25);
        //text("Best Half Avg",xavg+20,yavg-20);
        Intersect[] best = new Intersect[ints.length / 2];
        float bestXSum = 0.0f;
        float bestYSum = 0.0f;
        float bestXDevSum = 0.0f;
        float bestYDevSum = 0.0f;

        for (int i = 0; i < ints.length; i += 2) {
            float one = calcDistance(ints[i].xpos, ints[i].ypos, xavg, yavg);
            float two = calcDistance(ints[i + 1].xpos, ints[i + 1].ypos, xavg, yavg);
            if (one <= two) {
                best[i / 2] = ints[i];
            } else {
                best[i / 2] = ints[i + 1];
            }

            bestXSum += best[i / 2].xpos;
            bestXDevSum += Math.pow((best[i / 2].xpos - xavg), 2);
            bestYSum += best[i / 2].ypos;
            best[i / 2].displayRed();
            bestYDevSum += Math.pow((best[i / 2].ypos - yavg), 2);

        }

        float bestXavg = bestXSum / best.length;
        float bestYavg = bestYSum / best.length;

        Xguesses[guessCount] = bestXavg;
        Yguesses[guessCount] = bestYavg;

        targetX = average(Xguesses);
        targetY = average(Yguesses);
        targetXdev = stDev(Xguesses);
        targetYdev = stDev(Yguesses);
        guessCount++;
    }

    void displayTarget() {
        noStroke();
        fill(0, 50, 200, 150);
        if (jitter) {
            ellipse(targetX, targetY, targetXdev, targetYdev);
        } //  ellipse(average(Xguesses),average(Yguesses),25,25);}
        else {
            ellipse(average(Xguesses), average(Yguesses), 25, 25);
        }
  //text("Best 1/2 avg",average(Xguesses),average(Yguesses));
    }

    void displayStage() {
        background(255);
        noFill();
        stroke(0);
        rect(inset, inset, innerWidth - inset, innerWidth - inset);
    }

    public void draw() {
        if (wait == false) {
            if (guessCount >= guesses) {
                guessCount = 0;
            }
            displayStage();
            if (isSimulating) {
                simulate(); // not real data!\
                displayBeacons();
                guess(beacons[0].myIntersections);
            }
            displayBeacons();
            forceGuess();
            displayTarget();
        }
    }

    class Intersect {

        public String myName;
        public String parentNames;
        public float xpos;
        public float ypos;
        public float size = 10.0f;
        public float myColor = (0);
        public float myFill = (255);

        public Intersect(float newX, float newY, String newName, String newParents) {
            xpos = newX;
            ypos = newY;
            myName = newName;
            parentNames = newParents;
        }

        void display(int c) {
            stroke(0);
            fill(c);
            ellipse(xpos, ypos, size, size);
            stroke(0);
            fill(0);
            textFont(f, 16);
    //text(myName, xpos+10, ypos-10);
        }

        void display() {
            stroke(myColor);
            fill(myFill);
            ellipse(xpos, ypos, size, size);
            stroke(0);
            fill(0);
            textFont(f, 16);
    //text(myName, xpos+10, ypos-10);
        }

        void displayRed() {
            stroke(myColor);
            fill(color(255, 0, 0));
            ellipse(xpos, ypos, size, size);
            stroke(0);
            fill(0);
            textFont(f, 16);
    //text(myName, xpos+10, ypos-10);
        }
    }

    class Beacon {

        public Intersect[] myIntersections;
        public String myName;
        public float xpos;
        public float ypos;
        public float radius;
        public int myColor = color(255);

        public float getDistance(Beacon A, Beacon B) {
            return A.calcDistance(B.xpos, B.ypos);
        }

        public boolean isIntersecting(Beacon A, Beacon B) {
            double D = getDistance(A, B);
            if ((A.radius + B.radius) < D || D < Math.abs(A.radius - B.radius)) {
                return false;
            } else {
                return true;
            }
        }

        public void getIntersections(Beacon A, Beacon B) {
            Intersect[] inters;
            float x1 = A.xpos;
            float x2 = B.xpos;
            float y1 = A.ypos;
            float y2 = B.ypos;
            float r1 = A.radius;
            float r2 = B.radius;
            double D = getDistance(A, B);
            if (isIntersecting(A, B)) {
                double d1 = (Math.pow(r1, 2) - Math.pow(r2, 2) + Math.pow(D, 2)) / (2 * D);
                double h = Math.sqrt(r1 * r1 - d1 * d1);
                double x3 = x1 + (d1 * (x2 - x1)) / D;
                double y3 = y1 + (d1 * (y2 - y1)) / D;
                float x4 = (float) (x3 + (h * (y2 - y1)) / D);
                float y4 = (float) (y3 - (h * (x2 - x1)) / D);
                float x5 = (float) (x3 - (h * (y2 - y1)) / D);
                float y5 = (float) (y3 + (h * (x2 - x1)) / D);
                inters = new Intersect[2];
                inters[0] = new Intersect(x4, y4, A.myName + B.myName + "1", (A.myName + B.myName));
                inters[1] = new Intersect(x5, y5, A.myName + B.myName + "2", (A.myName + B.myName));
                myIntersections = storeIntersections(myIntersections, inters);
            }
        }

        public Intersect[] storeIntersections(Intersect[] existingInters, Intersect[] newInters) {
            Intersect[] temp;
            int size = newInters.length + existingInters.length;
            temp = new Intersect[size];
            for (int i = 0; i < temp.length; i++) {
                if (i < existingInters.length) {
                    temp[i] = existingInters[i];
                } else {
                    temp[i] = newInters[i - existingInters.length];
                }
            }
            return temp;
        }

        public void drawInts() {
            for (int i = 0; i < myIntersections.length; i++) {
                myIntersections[i].display();
            }
        }

        public void clearIntersections() {
            myIntersections = new Intersect[0];
        }

        public void drawIntersections(Beacon A, Beacon B) {
            float x1 = A.xpos;
            float x2 = B.xpos;
            float y1 = A.ypos;
            float y2 = B.ypos;
            float r1 = A.radius;
            float r2 = B.radius;
            double D = getDistance(A, B);
            if ((r1 + r2) >= D) {
                double d1 = (Math.pow(r1, 2) - Math.pow(r2, 2) + Math.pow(D, 2)) / (2 * D);
                double h = Math.sqrt(r1 * r1 - d1 * d1);
                double x3 = x1 + (d1 * (x2 - x1)) / D;
                double y3 = y1 + (d1 * (y2 - y1)) / D;
                float x4 = (float) (x3 + (h * (y2 - y1)) / D);
                float y4 = (float) (y3 - (h * (x2 - x1)) / D);
                float x5 = (float) (x3 - (h * (y2 - y1)) / D);
                float y5 = (float) (y3 + (h * (x2 - x1)) / D);
                stroke(0);
                fill(0);

                textFont(f, 16);
                fill(0);
                text(A.myName + ":" + B.myName + "1", x4 + 10, y4 - 10);
                ellipse(x4, y4, 10, 10);
                text((A.myName + ":" + B.myName + "2"), x5 + 10, y5 - 10);
                ellipse(x5, y5, 10, 10);
            }
        }

//public float calcH(Beacon A, Beacon B, float
        public Beacon(float newX, float newY, float newR, int c, String name) {
            myIntersections = new Intersect[0];
            xpos = newX;
            ypos = newY;
            radius = newR;
            myColor = c;
            myName = name;
        }

        public Beacon(float newX, float newY, String name) {
            xpos = newX;
            ypos = newY;
            radius = defaultRadius;
            myColor = color(0);
            myName = name;
            myIntersections = new Intersect[0];

        }

        public float calcDistance(float x, float y) {
            float xsum = xpos - x;
            float ysum = ypos - y;
            float xsq = xsum * xsum;
            float ysq = ysum * ysum;
            return (float) Math.sqrt(ysq + xsq);
        }

        public void setRadius(float r) {
            radius = r;
        }

        public void display() {
            textFont(f, 32);
            fill(0);
            text(myName, xpos + 30, ypos + 30);
            stroke(0);
            fill(0);
            ellipse(xpos, ypos, 10, 10);
            fill(myColor);
            if (myColor == color(0)) {
                noFill();
            }
            ellipse(xpos, ypos, radius * 2, radius * 2);
        }
    }

    /**
     * @param args the command line arguments
     */
}
