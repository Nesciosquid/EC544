import java.io.FileWriter;
/**
 * ***********************
 // ZOMG ITS A CAR THING 
 */
/*--Instance variables--*/
/* Car Logic  */
IRDaemon IR_DAEMON = new IRDaemon();
Car theCar;

/* Data Storage Elements */
SmallPoint sp;
BufferedReader reader;
CommandWriter cmd;
ArrayList<CarPoint> allPoints = new ArrayList<CarPoint>();
ArrayList<SmallPoint> allSmallPoints = new ArrayList<SmallPoint>();
ArrayList<WallSegment> allSegments = new ArrayList<WallSegment>();
int currentIndex = 0;
CarPoint currentPoint;
SmallPoint currentSmallPoint;
int csvWidth;
double worldTime;
float worldX;
float worldY;
int lastCornerIndex = 0;
int lastSignalIndex = 0;
float worldTheta;
int leftTurnCount = 0;
int rightTurnCount = 0;
double TAKE_NEXT_TIMEOUT = 8000;
double CORNER_TIMEOUT = 8000;
double next_corner_time;
double take_next_time;

/* UI Elements */
Scrubber timeScrubber;
Scrubber turnScrubber;
String[] readouts;
PFont f;
float fontSize;
float conversionFactor; // reads to pixels
float timeScrubberWidth;
float timeScrubberY;
float timeScrubberInset = 50.0;
Floor theFloor;
int lastDrawnLiveIndex = 0;

Button fast;
Button med;
Button slow;

ArrayList<EventBox> allBoxes = new ArrayList<EventBox>();

/* Car Constants*/
float turnRate = .000400; // .000355 for sunday-corners //radians per millisecond at max turn
float maxVelocity;
float turnFactor;
float sampleTime = 347; // milliseconds
float SNAP_ANGLE = 90;

/* Control Elements */
boolean rotateWorld = true;
boolean automated = false;
boolean reset = false;
boolean live = false;
boolean loop = true;
boolean playback = true;
boolean turnScrubbing = false;
boolean timeScrubbing = false;
int turnSetting = 1500;
boolean waitForTurn = false;
boolean waitForSignal = true;
int speedSetting = 1500;

void setup() {
  reader = createReader("calibration/four_laps_4th_floor - crop.csv");
  //reader = createReader("sunday-corners.csv");
  //reader = createReader("../live.csv");   
  cmd = new CommandWriter("../../commands.csv");

  //size(600,600);
  size(displayWidth, displayHeight);

  timeScrubberInset = 50.0;
  timeScrubberWidth = width - timeScrubberInset * 2;
  timeScrubberY = height - timeScrubberInset * 2;
  smooth();
  frameRate(120);
  csvWidth = 8;
  worldTheta = (float) IRDaemon.degToRadians(0);

  conversionFactor = .5;

  maxVelocity = 37.5;
  turnFactor = ((float) IR_DAEMON.degToRadians((double) 1) / 8);

  readouts = new String[15];
  fontSize = (int) 15.0 ;
  if (fontSize <= 1) {
    fontSize = 1.0;
  }

  timeScrubber = new Scrubber(timeScrubberInset, timeScrubberY, timeScrubberWidth, 45, true);
  turnScrubber = new Scrubber(width / 2 - timeScrubberInset, timeScrubberInset, width / 2 - timeScrubberInset, 45, true);
  //speedScrubber = new Scrubber();
  f = createFont("Arial", fontSize, true);

  background(255);
  checkData();
  if (allPoints.size() >0) {
    if (live == true) {
      currentPoint = allPoints.get(allPoints.size() - 1);
      currentSmallPoint = allSmallPoints.get(allPoints.size() - 1);
    }
  }
  if (loop == false) {
    currentIndex = allPoints.size() - 1;
  }
  theCar = new Car(width / 2, height / 2);
  worldX = theCar.xpos;
  worldY = theCar.ypos;
  theFloor = new Floor();
}

void keyPressed() {
  if (key == 't') {
    if (automated = false){
    automated = true;
    }
    else {
      automated = false;
    }
  }

  if (key == 'c') {
    turnSetting=1500;
    cmd.writeTurn(1500);
  }
  if (key == 's') {
    speedSetting=1500;
    cmd.writeSpeed(1500);
  }
  if (key == 'r') {
    cmd.writeNextRight();
  }
  if (key == 'l') {
    cmd.writeNextLeft();
  }
  if (key == CODED) {
    if (keyCode == LEFT) {
      if (turnSetting == 0) {
        turnSetting = 1500;
      }
      turnSetting += 250;
      if (turnSetting >= 2000) {
        turnSetting = 2000;
      }
      cmd.writeTurn(turnSetting);
    }
    if (keyCode == RIGHT) {
      if (turnSetting == 0) {
        turnSetting = 1500;
      }
      turnSetting -= 250;
      if (turnSetting <= 1000) {
        turnSetting = 1000;
      }
      cmd.writeTurn(turnSetting);
    }
    if (keyCode == CONTROL) {
      turnSetting = 0;
      cmd.writeUnlockTurn();
    }
    if (keyCode == UP) {
      if (speedSetting == 0) {
        speedSetting = 1500;
      } 
      else {
        speedSetting -= 100;
        if (speedSetting <= 1000) {
          speedSetting = 1000;
        }
        cmd.writeSpeed(speedSetting);
      }
    }
    if (keyCode == DOWN) {
      if (speedSetting == 0) {
        speedSetting = 1500;
      }
      {
        speedSetting += 100;
        {
          if (speedSetting >= 2000) {
            speedSetting = 2000;
          }
        }
        cmd.writeSpeed(speedSetting);
      }
    } 
    else if (keyCode == SHIFT) {
      speedSetting = 0;
      cmd.writeUnlockSpeed();
    }
  }
}

public float storeData(String newData) {
  if (newData == "" || newData == null) {
    return 0.0f;
  } 
  else {
    return Float.parseFloat(newData);
  }
}

public double speedSetToVelocity(int speedSet) {
  int speedDiff;
  double absVel;
  speedDiff = 1500 - speedSet;
  double flat = Math.abs(speedDiff);
  absVel = (-.0002*flat*flat + .1854*flat);
  if (speedDiff >= 0) {
    return absVel;
  }
  else return -absVel;
}  



void draw() {
  if (allPoints.size() != 0) {
    checkData();
    if (playback == true) {
      timeScrubber.updatePosition((float) currentIndex / allPoints.size());
      turnScrubber.updatePosition(map(turnSetting, 2000, 1000, 0, 1));
      if (currentIndex == allPoints.size() - 1) {
        if (loop == true) {
          currentIndex = 0;
          theFloor.resetPosition();
          //worldX = theCar.xpos;
          //worldY = theCar.ypos;
        }
      } 
      else {
        currentIndex++;
      }
      currentPoint = allPoints.get(currentIndex);
      currentSmallPoint = allSmallPoints.get(currentIndex);
    }
  }

  if (currentIndex == 0) {
    worldTime = currentIndex;
  }

  if (currentPoint != null && playback == true) {
    theCar.updateCar(currentPoint, currentSmallPoint);
    theCar.updateWorldPosition();
    WallSegment a = new WallSegment(worldTheta - theCar.theta, worldX, worldY, theCar.reads[0], theCar.reads[2], theCar.trusts[0], true);
    WallSegment b = new WallSegment(worldTheta - theCar.theta, worldX, worldY, theCar.reads[1], theCar.reads[3], theCar.trusts[1], false);
    allSegments.add(a);
    allSegments.add(b);
    theCar.drawPreviousReads();
    theFloor.updateFloor(currentPoint, theCar);
  }

  fill(255);
  rectMode(CORNERS);
  rect(0, 0, width, height);
  //printAllPoints();
  theFloor.display();
  theCar.display();
  printData(theCar);
  timeScrubber.display();
  turnScrubber.display();
  drawAllBoxes();
}

CarPoint processNewPoint(CarPoint oldPoint) {
  IR_DAEMON.setReads((int) oldPoint.LF, (int) oldPoint.RF, (int) oldPoint.LR, (int) oldPoint.RR);
  IR_DAEMON.pickDirection();
  return IR_DAEMON.getCarpoint((int) oldPoint.turn, (int) oldPoint.velocity, false, false, oldPoint.time);
}

CarPoint processNewPoint(SmallPoint sp) {
  IR_DAEMON.updateReads(IR_DAEMON.getOldVolts((double) sp.leftFront), IR_DAEMON.getOldVolts((double) sp.rightFront), IR_DAEMON.getOldVolts((double) sp.leftRear), IR_DAEMON.getOldVolts((double) sp.rightRear));
  IR_DAEMON.setReads(sp.leftFront, sp.rightFront, sp.leftRear, sp.rightRear);
  IR_DAEMON.pickDirection();
  return IR_DAEMON.getCarpoint((int) sp.setTurn, (int) sp.setSpeed, false, false, sp.time);
}

void drawAllBoxes() {
  for (int i = 0; i < allBoxes.size(); i++) {
    allBoxes.get(i).display();
  }
}

void mousePressed() {
  if (mouseButton == LEFT) {
    if (timeScrubber.overScrubber()) {
      timeScrubbing = true;
      if (playback == true) {
        reset = true;
        playback = false;
        theCar.stop = true;
        theFloor.stop = true;
      }
    } 
    else if (turnScrubber.overScrubber()) {
      turnScrubbing = true;
    }
  } 
  else {
    if (playback == false) {
      playback = true;
      theCar.stop = false;
      theFloor.stop = false;
    } 
    else {
      playback = false;
      theCar.stop = true;
      theFloor.stop = true;
    }
  }
}

void mouseDragged() {
  int lastIndex = currentIndex;
  if (timeScrubbing) {
    playback = false;
    theCar.stop = true;
    theFloor.stop = true;
    timeScrubber.xpos = mouseX;
    if (timeScrubber.xpos < timeScrubberInset) {
      timeScrubber.xpos = timeScrubberInset;
    } 
    else if (timeScrubber.xpos > width - timeScrubberInset) {
      timeScrubber.xpos = width - timeScrubberInset;
    }

    currentIndex = (int) ((float) allPoints.size() * ((float) (timeScrubber.xpos - timeScrubberInset) / timeScrubberWidth));
    if (currentIndex > allPoints.size() - 1) {
      currentIndex = allPoints.size() - 1;
    }
  } 
  else if (turnScrubbing) {
    turnScrubber.xpos = mouseX;
    if (turnScrubber.xpos < turnScrubber.xStart) {
      turnScrubber.xpos = turnScrubber.xStart;
    } 
    else if (turnScrubber.xpos > turnScrubber.xpos + turnScrubber.xSize) {
      turnScrubber.xpos = turnScrubber.xpos + turnScrubber.xSize;
    }
    turnSetting = 2000 - 2 * (int) (1000 * (float) (turnScrubber.xpos - turnScrubber.xStart) / (turnScrubber.xStart + turnScrubber.xSize));
    if (turnSetting < 1000) {
      turnSetting = 1000;
    } 
    else if (turnSetting > 2000) {
      turnSetting = 2000;
    }
  }

  if (lastIndex > currentIndex) {

    forceUpdateCarFloor(true);
  } 
  else if (lastIndex < currentIndex) {
    forceUpdateCarFloor(false);
  }
}

void mouseReleased() {
  if (mouseButton == LEFT) {
    if (reset) {
      playback = true;
      theCar.stop = false;
      theFloor.stop = false;
      reset = false;
    }
    /*if (turnScrubbing) {
     writer.println("turn," + turnSetting);
     writer.flush();
     timeScrubbing = false;
     System.out.println("Sending turn:" + turnSetting);
     }*/
  }
}

SmallPoint processLineToSmallPoint(String[] currentLine) {
  int LF = 0;
  int RF = 0;
  int LR = 0;
  int RR = 0;
  int turn = 0;
  int speed = 0;
  int booleans = 0;
  double time = 0.0;
  time = Double.parseDouble(currentLine[0]);
  LF = Integer.parseInt(currentLine[1]);
  RF = Integer.parseInt(currentLine[2]);
  LR = Integer.parseInt(currentLine[3]);
  RR = Integer.parseInt(currentLine[4]);
  turn = Integer.parseInt(currentLine[5]);
  speed = Integer.parseInt(currentLine[6]);
  booleans = (int)Float.parseFloat(currentLine[7]);


  sp = new SmallPoint(LF, RF, LR, RR, turn, speed);
  sp.setBooleans(booleans);
  sp.time = time;
  return sp;
}

CarPoint processLineToCarpoint(String[] currentLine) {
  CarPoint cp = new CarPoint();
  for (int i = 0; i < currentLine.length; i++) {
    switch (i) {
    case 0:
      cp.time = storeData(currentLine[i]);
    case 1:
      cp.LF = storeData(currentLine[i]);
    case 2:
      cp.RF = storeData(currentLine[i]);
    case 3:
      cp.LR = storeData(currentLine[i]);
    case 4:
      cp.RR = storeData(currentLine[i]);
    case 5:
      cp.LT = storeData(currentLine[i]);
    case 6:
      cp.RT = storeData(currentLine[i]);
    case 7:
      cp.distRight = storeData(currentLine[i]);
    case 8:
      cp.distLeft = storeData(currentLine[i]);
    case 9:
      cp.thetaRight = storeData(currentLine[i]);
    case 10:
      cp.thetaLeft = storeData(currentLine[i]);
    case 11:
      cp.theta = storeData(currentLine[i]);
    case 12:
      cp.distance = storeData(currentLine[i]);
    case 13:
      cp.turn = storeData(currentLine[i]);
    case 14:
      cp.velocity = storeData(currentLine[i]);
    case 15:
      cp.targetDist = storeData(currentLine[i]);
    case 16:
      cp.startTurn = storeData(currentLine[i]);
    case 17:
      cp.stopTurn = storeData(currentLine[i]);
    case 18:
      cp.targetTheta = storeData(currentLine[i]);
    }
  }
  return cp;
}

void checkData() {
  String thisLine;
  String output;
  String[] currentLine;

  try {
    while ( (thisLine = reader.readLine ()) != null) {
      output = "";
      currentLine = split(thisLine, ",");
      if (currentLine[0].equals("time")) {
        System.out.print("Header:");
        for (int i = 0; i < currentLine.length; i++) {
          output += currentLine[i];
          if (i != currentLine.length - 1) {
            output += ",";
          }
        }
        System.out.println(output);
      } 
      else {
        if (currentLine.length == csvWidth) {
          //allPoints.add(processLineToCarpoint(currentLine));
          SmallPoint current = processLineToSmallPoint(currentLine);
          allSmallPoints.add(current);
          allPoints.add(processNewPoint(current));
        }
      }
    }
  } 
  catch (IOException e) {
  }
}

void drawVerticalLine(float xpos, color c) {
  strokeWeight(10);
  fill(c);
  stroke(c);
  line(xpos, displayHeight, xpos, 0);
  strokeWeight(1);
}

void calcWorldTheta() {
  worldTheta = leftTurnCount * radians(90) + rightTurnCount * radians(-90);
}

public void forceUpdateCarFloor(boolean reverse) {
  boolean stopReset = theCar.stop;
  currentPoint = allPoints.get(currentIndex);
  currentSmallPoint = allSmallPoints.get(currentIndex);
  theCar.stop = false;
  theFloor.stop = false;
  if (!reverse) {
    theCar.updateCar(currentPoint, currentSmallPoint);
    theCar.updateWorldPosition();
    theFloor.updateFloor(currentPoint, theCar);
    WallSegment a = new WallSegment(worldTheta - theCar.theta, worldX, worldY, theCar.reads[0], theCar.reads[2], theCar.trusts[0], true);
    WallSegment b = new WallSegment(worldTheta - theCar.theta, worldX, worldY, theCar.reads[1], theCar.reads[3], theCar.trusts[1], false);
    allSegments.add(a);
    allSegments.add(b);
  } 
  else {
    theCar.updateCarBackwards(currentPoint);
    theFloor.updateFloorBackwards(currentPoint, theCar);
  }
  theCar.stop = stopReset;
  theFloor.stop = stopReset;
}

public void mouseWheel(MouseEvent event) {
  float e = event.getAmount();
  if (e < 0) {
    if (currentIndex < allPoints.size() - 1) {
      currentIndex++;
      forceUpdateCarFloor(false);
    }
  } 
  else if (e > 0) {
    if (currentIndex > 0) {
      currentIndex--;
      forceUpdateCarFloor(true);
    }
  }

  timeScrubber.xpos = timeScrubberInset + ((float) currentIndex / allPoints.size()) * timeScrubberWidth;
}

public void printData(Car newCar) {

  readouts[0] = "Car Position: " + newCar.distance;
  readouts[1] = "Target Position: " + newCar.targetX;
  readouts[2] = "Car theta: " + degrees(theCar.theta)+ ", radians: " + (theCar.theta);
  readouts[3] = "Speed set: " + speedSetting;
  readouts[4] = "Car Speed Set: " + newCar.mySpeedSet + ", " + speedSetToVelocity((int)newCar.mySpeedSet);
  readouts[5] = "Car Turn Value: " + newCar.myTurn;
  readouts[6] = "Car Velocity: " + newCar.velocity;
  readouts[7] = "Timepoint: " + currentPoint.time;
  readouts[8] = "World theta: " + worldTheta;
  readouts[9] = "Car tracked theta: " + newCar.myWorldTheta;
  readouts[10] = "Next Left: " + currentSmallPoint.takeNextLeft + ", cornering left: " + currentSmallPoint.corneringLeft;
  readouts[11] = "Next Right: " + currentSmallPoint.takeNextRight + ", cornering Right: " + currentSmallPoint.corneringRight;
  readouts[12] = "Current Smallpoint is a smallpoint: " + currentSmallPoint.isASmallPoint();
  readouts[13] = "Automated turn selection: " + automated;
  readouts[14] = "";

  stroke(0);
  fill(50, 50, 50, 150);
  rectMode(CORNERS);
  rect(10, 10, 400, (50 + readouts.length * 20), 15);
  textFont(f, fontSize);
  fill(255);

  for (int i = 0; i < readouts.length; i++) {
    text(readouts[i], 15, 30 + 20* i);
  }
}

public class Scrubber {

  public float xStart;
  public float yStart;
  public boolean isHorizontal;
  public float xpos;
  public float ypos;
  public float xSize;
  public float ySize;
  public float roundness = 10;
  public float dialXSize;
  public float dialYSize;

  public Scrubber(float newX, float newY, float newXSize, float newYSize, boolean horiz) {
    xStart = newX;
    yStart = newY;
    xSize = newXSize;
    ySize = newYSize;
    isHorizontal = horiz;
    xpos = xStart;
    ypos = yStart;
    if (horiz) {
      dialXSize = ySize / 3;
      dialYSize = ySize;
    } 
    else {
      dialXSize = xSize;
      dialYSize = xSize / 3;
    }
  }

  void updatePosition(float percentage) {
    if (isHorizontal) {
      xpos = xStart + percentage * xSize;
    } 
    else {
      ypos = xStart + percentage * ySize;
    }
  }

  boolean overScrubberBar() {
    if (mouseX >= xStart && mouseX <= xStart + xSize && mouseY >= yStart && mouseY <= yStart + ySize) {
      return true;
    } 
    else {
      return false;
    }
  }

  boolean overScrubber() {
    if (mouseX >= xpos - dialXSize / 2 && mouseX <= xpos + dialXSize / 2 && mouseY >= ypos - dialYSize / 2 && mouseY <= ypos + dialYSize / 2) {
      return true;
    } 
    else {
      return false;
    }
  }

  public void display() {

    strokeWeight(3);
    stroke(0);
    if (isHorizontal) {
      line(xStart, yStart, xStart + xSize, yStart);
      line(xStart, yStart - ySize / 2, xStart, yStart + ySize);
      line(xStart + xSize, yStart - ySize / 2, xStart + xSize, yStart + ySize);
    } 
    else {
      line(xStart, yStart, xStart, yStart + ySize);
      line(xStart + xSize / 2, yStart, xStart - xSize / 2, yStart);
      line(xStart + xSize, yStart + ySize, xStart - xSize, yStart + ySize);
    }

    strokeWeight(1);
    stroke(0);
    fill(255);
    rectMode(CENTER);
    rect(xpos, ypos, dialXSize, dialYSize, roundness);
  }
}

public class Button {

  public float xpos;
  public float ypos;
  public float size = 10.0 * conversionFactor;
  public String name;

  public Button(float x, float y, String newName) {
    name = newName;
    xpos = x;
    ypos = y;
  }

  public void display() {
    stroke(0);
    fill(255);
    rectMode(CENTER);
    rect(xpos, ypos, size, size * 2 / 3, size / 8);
    stroke(0);
    fill(0);
    text(name, xpos - size / 3, ypos + size / 8);
  }

  public boolean isOver() {
    if (mouseX >= xpos - size / 2 && mouseX <= xpos + size / 2) {
      if (mouseY >= ypos - size / 3 && mouseY <= ypos + size / 3) {
        return true;
      } 
      else {
        return false;
      }
    } 
    else {
      return false;
    }
  }
}

class Wheel {

  class WheelStripe {

    float stripeWidth;
    float stripeLength;
    float xpos;
    float ypos;
    float stripeWeight = 2;
    int stripeStroke = color(60);

    WheelStripe(float newX, float newY, float newWidth, float newLength) {
      xpos = newX;
      ypos = newY;
      stripeWidth = newWidth;
      stripeLength = newLength;
    }

    public void move(float newVelocity, float maxY, float minY) {
      float velocity = newVelocity * conversionFactor;
      float max = maxY - stripeWeight;
      float min = minY + stripeWeight;
      ypos = ypos - velocity / 5;
      if (ypos > max) {
        ypos = min + (ypos - max);
      } 
      else if (ypos < min) {
        ypos = max + (ypos - min);
      }
    }

    public void display() {
      stroke(stripeStroke);
      strokeWeight(stripeWeight);
      line(xpos - stripeWidth / 2, ypos, xpos + stripeWidth / 2, ypos);
      stroke(0);
      strokeWeight(1);
    }
  }

  WheelStripe[] stripes;
  boolean stop = false;
  float velocity = 10;
  float xpos;
  float ypos;
  float centerTurn = 1500;
  float turnAmount = 1500; // 1000 == full right
  float turnTheta;
  float wheelSize = 10.0f; //IR units
  float wheelLength = (wheelSize * conversionFactor) * 6 / 5;
  float wheelWidth = (wheelSize * conversionFactor) * 4 / 5;
  float maxTheta = radians(30.0f);
  float wheelWeight = 2;
  int wheelColor = color(150);
  int wheelStroke = color(0);

  Wheel(float newX, float newY) {
    xpos = newX;
    ypos = newY;
    updateTheta();
    //stripes[0] = new WheelStripe(xpos, ypos, wheelWidth, wheelLength);
    initStripes();
  }

  void setVelocity(float newVel) {
    velocity = newVel;
  }

  int calcStripes() {
    return (int) Math.floor(wheelLength / (wheelWeight * 6));
  }

  void initStripes() {
    stripes = new WheelStripe[calcStripes()];
    float yStart = 0 - (wheelLength / 2) + wheelWeight + 1;
    for (int i = 0; i < stripes.length; i++) {
      stripes[i] = new WheelStripe(0, (yStart + (wheelWeight * 7) * i), wheelWidth - 1, wheelLength - 1);
    }
  }

  void moveStripes() {
    for (int i = 0; i < stripes.length; i++) {
      if (!stop) {
        stripes[i].move(velocity, 0 + wheelLength / 2, 0 - wheelLength / 2);
      }
      stripes[i].display();
    }
  }

  float getTurn() {
    return turnAmount;
  }

  float getX() {
    return xpos;
  }

  float getY() {
    return ypos;
  }

  float getTheta() {
    return turnTheta;
  }

  void updateTheta() {
    float turnDiff = (centerTurn - turnAmount); //-500 for right, 500 left
    turnTheta = (float) (turnDiff / 500) * maxTheta;
  }

  void updateTurn(float turn) {
    turnAmount = turn;
    updateTheta();
  }

  void display() {

    rectMode(CENTER);
    noStroke();
    fill(wheelColor);
    rect(0, 0, wheelWidth, wheelLength, 5, 5, 5, 5);
    moveStripes();
    noFill();
    strokeWeight(wheelWeight);
    stroke(wheelStroke);
    rect(0, 0, wheelWidth, wheelLength, 5, 5, 5, 5);
    strokeWeight(1);
    stroke(0);
  }
}

class Car {
  private boolean sawCorner;
  private float myWorldTheta;
  public boolean stop = false;
  public float[] reads = new float[4];
  public int[] trusts = new int[4];
  public float velocity = 15.0f;
  public float mySpeedSet;
  public float targetX = 400.0f;
  public float leftWallDistance = -20.0f;
  public float rightWallDistance = 20.0f;
  public float distance;
  Wheel[] wheels = new Wheel[4];
  IRSensor[] sensors = new IRSensor[4];
  public float theta = radians(0);
  public float thetaLeft;
  public float thetaRight;
  public float xpos;
  public float ypos;
  public float myTurn;
  public float mySize = 31.0; // in IR units, measured top sensor area
  float topLength = mySize * conversionFactor;
  float topWidth = mySize * conversionFactor / 2;
  float totalWidth = mySize * conversionFactor * 3 / 4;
  float totalLength = mySize * conversionFactor * 5 / 4;
  float startTurn;
  float stopTurn;
  int topColor = color(50);
  int topStroke = 0;
  int totalColor = color(86, 155, 105);
  int totalStroke = color(0);

  Car(float x, float y) {
    xpos = x;
    ypos = y;
    wheels[0] = new Wheel(-totalWidth / 2, -totalLength / 2);
    wheels[1] = new Wheel(totalWidth / 2, -totalLength / 2);
    wheels[2] = new Wheel(-totalWidth / 2, totalLength / 2);
    wheels[3] = new Wheel(totalWidth / 2, totalLength / 2);
    sensors[0] = new IRSensor(-topWidth / 2, -topLength / 2, radians(180)); //LF
    sensors[1] = new IRSensor(topWidth / 2, -topLength / 2, radians(0)); //RF
    sensors[2] = new IRSensor(-topWidth / 2, topLength / 2, radians(180)); //LR
    sensors[3] = new IRSensor(topWidth / 2, topLength / 2, radians(0)); //RR
  }

  void setTarget(float x) {
    targetX = x;
  }

  float getTheta() {
    return theta;
  }

  void setVelocity(float new_vel) {
    velocity = new_vel;
    for (int i = 0; i < wheels.length; i++) {
      wheels[i].setVelocity(new_vel);
    }
  }

  public void updateCar(CarPoint newPoint, SmallPoint smallPoint) {
    reads[0] = newPoint.LF;
    reads[1] = newPoint.RF;
    reads[2] = newPoint.LR;
    reads[3] = newPoint.RR;
    trusts[0] = (int) newPoint.LT;
    trusts[2] = (int) newPoint.LT;
    trusts[1] = (int) newPoint.RT;
    trusts[3] = (int) newPoint.RT;
    leftWallDistance = newPoint.distRight;
    rightWallDistance = newPoint.distLeft;
    thetaRight = newPoint.thetaRight;
    thetaLeft = newPoint.thetaLeft;
    theta = newPoint.theta;
    distance = newPoint.distance;
    setTurn(newPoint.turn);
    mySpeedSet = newPoint.velocity;
    setVelocity((float)speedSetToVelocity((int)mySpeedSet));
    targetX = newPoint.targetDist;
    startTurn = newPoint.startTurn;
    stopTurn = newPoint.stopTurn;

    updateFrontWheels();
    //updatePosition();
  }

  public void updateCarBackwards(CarPoint newPoint) {
    reads[0] = newPoint.LF;
    reads[1] = newPoint.RF;
    reads[2] = newPoint.LR;
    reads[3] = newPoint.RR;
    trusts[0] = (int) newPoint.LT;
    trusts[2] = (int) newPoint.LT;
    trusts[1] = (int) newPoint.RT;
    trusts[3] = (int) newPoint.RT;
    leftWallDistance = newPoint.distRight;
    rightWallDistance = newPoint.distLeft;
    thetaRight = newPoint.thetaRight;
    thetaLeft = newPoint.thetaLeft;
    theta = newPoint.theta;
    distance = newPoint.distance;
    setTurn(newPoint.turn);
    mySpeedSet = newPoint.velocity;
    setVelocity(calcVelocity(mySpeedSet));
    targetX = newPoint.targetDist;
    startTurn = newPoint.startTurn;
    stopTurn = newPoint.stopTurn;

    updateFrontWheels();
    //updatePositionBackwards();
  }

  public float frontCenterX() {
    float tangent = topLength / 2;
    float dx = sin(theta) * tangent;
    float newx = xpos + dx;
    return newx;
  }

  public float frontCenterY() {
    float tangent = topLength / 2;
    float dy = cos(theta) * tangent;
    float newy = ypos - dy;
    return newy;
  }

  public float backCenterX() {
    float tangent = topLength / 2;
    float dx = sin(theta) * tangent;
    float newx = xpos - dx;
    return newx;
  }

  public float backCenterY() {
    float tangent = topLength / 2;
    float dy = cos(theta) * tangent;
    float newy = ypos + dy;
    return newy;
  }

  public float calcVelocity(float speedSet) {
    float speedDiff;
    float speedRatio;
    speedDiff = speedSet - 1500;
    speedRatio = speedDiff / 500;
    return -speedRatio * maxVelocity;
  }

  void updateWorldPosition() {
    if (!currentSmallPoint.viewed) {
      //System.out.println(thisSampleTime);
      double timeDiff;
      if (currentIndex == 0) {
        timeDiff = 340;
      }
      else {
        timeDiff = allSmallPoints.get(currentIndex).time - allSmallPoints.get(currentIndex-1).time;
      }
      if (rotateWorld) {
        float diff = myTurn - 1500;
        float ratio = diff / 500;
        //float thetaTwo = ratio * (velocity * turnFactor);
        //double thetaTwo = ratio * turnRate * sampleTime * (velocity / 26.25);
        double thetaTwo = ratio * turnRate * timeDiff * (velocity / 26.25);
        if (velocity >= 0) {
          myWorldTheta += (float) thetaTwo;
        } 
        else {
          myWorldTheta += (float) thetaTwo;
        }
        //float turns = myWorldTheta / (radians(SNAP_ANGLE));
        //int turnCount = (int)Math.round(turns);
        //worldTheta = turnCount * radians(SNAP_ANGLE); 
        //worldTheta = myWorldTheta; // when not snapping to 90-degree angles
      }
      if (currentIndex > 0 && currentSmallPoint.time >= take_next_time) {
        if (currentSmallPoint.takeNextLeft && !allSmallPoints.get(currentIndex-1).takeNextLeft) {
          allBoxes.add(new EventBox(worldTheta, worldX+theCar.xpos, worldY+theCar.ypos, false, "takeNextLeft"));
          take_next_time = currentSmallPoint.time + TAKE_NEXT_TIMEOUT;
        }
        if (currentSmallPoint.takeNextRight && !allSmallPoints.get(currentIndex-1).takeNextRight) {
          allBoxes.add(new EventBox(worldTheta, worldX+theCar.xpos, worldY+theCar.ypos, false, "takeNextRight"));
          take_next_time = currentSmallPoint.time + TAKE_NEXT_TIMEOUT;
        }
      }

      if (currentIndex != 0 && currentSmallPoint.time >= next_corner_time) {
        if (currentSmallPoint.corneringLeft && !allSmallPoints.get(currentIndex-1).corneringLeft) {
          leftTurnCount ++;
          calcWorldTheta();
          allBoxes.add(new EventBox(worldTheta, worldX+theCar.xpos, worldY+theCar.ypos, true, "cornerRight"));
          next_corner_time = currentSmallPoint.time + CORNER_TIMEOUT;
        }
        if (currentSmallPoint.corneringRight && !allSmallPoints.get(currentIndex-1).corneringRight) {
          rightTurnCount --;
          calcWorldTheta();
          allBoxes.add(new EventBox(worldTheta, worldX+theCar.xpos, worldY+theCar.ypos, true, "cornerLeft"));
          next_corner_time = currentSmallPoint.time + CORNER_TIMEOUT;
        }
      }

      worldTime = currentPoint.time;
      worldY -= cos(theta-worldTheta) * velocity*(timeDiff/1000) * conversionFactor;
      worldX += sin(theta-worldTheta) * velocity*(timeDiff/1000) * conversionFactor;
    }
    currentSmallPoint.viewed = true;
  }


  void updatePosition() {
    if (xpos > width + totalWidth) {
      xpos = 0 + (xpos - (width + totalWidth));
    }
    if (ypos > height + totalLength) {
      ypos = 0 + (ypos - height - totalLength);
    }
    if (xpos < 0) {
      xpos = width + xpos + totalWidth;
    }
    if (ypos < 0) {
      ypos = height + ypos + totalLength;
    }

    //xpos += sin(theta) * velocity;
    //ypos -= cos(theta) * velocity;
    if (distance < 0) {
      //xpos = hall_left - distance * conversionFactor;
      xpos = width / 2 + (-width / 4 - distance * conversionFactor);
    } 
    else {
      xpos = width / 2 - (-width / 4 - distance * conversionFactor);
    }
  }

  void updatePositionBackwards() {
    if (xpos > width + totalWidth) {
      xpos = 0 + (xpos - (width + totalWidth));
    }
    if (ypos > height + totalLength) {
      ypos = 0 + (ypos - height - totalLength);
    }
    if (xpos < 0) {
      xpos = width + xpos + totalWidth;
    }
    if (ypos < 0) {
      ypos = height + ypos + totalLength;
    }

    //xpos += sin(theta) * velocity;
    //ypos -= cos(theta) * velocity;
    if (distance < 0) {
      //xpos = hall_left - distance * conversionFactor;
      xpos = width / 2 - (-width / 4 - distance * conversionFactor);
    } 
    else {
      xpos = width / 2 + (-width / 4 - distance * conversionFactor);
    }
  }

  void setXY(float x, float y) {
    xpos = x;
    ypos = y;
  }

  void setX(float x) {
    xpos = x;
  }

  void setY(float y) {
    ypos = y;
  }

  void setTheta(float theta_rad) {
    theta = theta_rad;
  }

  void setTurn(float newTurn) {
    myTurn = newTurn;
  }

  void updateFrontWheels() {
    for (int i = 0; i < 2; i++) {
      wheels[i].updateTurn(myTurn);
    }
  }

  void drawPreviousReads() {
    pushMatrix();
    translate(xpos, ypos);
    float segLength;
    IRSensor currentSensor;
    for (int i = 0; i < allSegments.size(); i++) {
      WallSegment segment = allSegments.get(i);
      pushMatrix();
      translate(segment.oldX - worldX, segment.oldY - worldY);
      rotate(-(segment.oldTheta));

      if (segment.isLeft) {
        currentSensor = sensors[0];
        segLength = -mySize * conversionFactor;
      } 
      else {
        currentSensor = sensors[1];
        segLength = mySize * conversionFactor;
      }
      translate(currentSensor.getX(), currentSensor.getY());
      rotate(currentSensor.getTheta());
      currentSensor.drawWall(segment.oldF, segment.oldR, segment.oldTrust, segLength);
      popMatrix();
    }
    popMatrix();
  }

  void drawWheels() {
    for (int i = 0; i < wheels.length; i++) {
      Wheel currentWheel = wheels[i];
      currentWheel.stop = stop;
      pushMatrix();
      translate(currentWheel.getX(), currentWheel.getY());
      rotate(currentWheel.getTheta());
      currentWheel.display();
      popMatrix();
    }
  }

  void drawSensors() {
    for (int i = 0; i < sensors.length; i++) {
      IRSensor currentSensor = sensors[i];
      pushMatrix();
      translate(currentSensor.getX(), currentSensor.getY());
      rotate(currentSensor.getTheta());
      currentSensor.shootBeam(reads[i], trusts[i]);
      currentSensor.display();
      popMatrix();

      pushMatrix();
      translate(currentSensor.getX(), currentSensor.getY());
      currentSensor.writeRead(reads[i], trusts[i]);
      popMatrix();
    }
  }

  void display() {

    pushMatrix();
    translate(xpos, ypos);
    rotate(theta - worldTheta);
    rectMode(CENTER);
    fill(totalColor);
    stroke(totalStroke);
    strokeWeight(2);
    rect(0, 0, totalWidth, totalLength);
    strokeWeight(1);

    fill(topColor);
    stroke(topStroke);
    rect(0, 0, topWidth, topLength);
    drawWheels();
    drawSensors();
    popMatrix();
    drawPreviousReads();
  }
}

class IRSensor {

  float xpos;
  float ypos;
  float sensorSize = 2.0f;
  float beamSplashSize = sensorSize * conversionFactor * 1.5;
  float sensorWidth = sensorSize * conversionFactor;
  float sensorLength = sensorSize * conversionFactor * 1.5;
  float theta = radians(0);
  int goodColor = color(0, 0, 255, 150);
  int mediumColor = color(0, 255, 0, 100);
  int badColor = color(255, 184, 3, 100);
  int reallyBadColor = color(232, 0, 0, 10);
  int noColor = color(0, 0, 0, 0);
  int sensorStroke = color(255);
  int sensorColor = color(0);
  PFont f;

  IRSensor(float newX, float newY, float newTheta) {
    xpos = newX;
    ypos = newY;
    theta = newTheta;
    f = createFont("Arial", fontSize * 2, true);
  }

  float getX() {
    return xpos;
  }

  float getY() {
    return ypos;
  }

  float getTheta() {
    return theta;
  }

  void writeRead(float read, int trust) {
    int textColor = calcColor(trust);
    fill(textColor);
    if (textColor == noColor) {
      noStroke();
      noFill();
    }
    text((int) read, xpos, ypos);
  }

  int calcColor(int trust) {
    if (trust == 3) {
      return goodColor;
    } 
    else if (trust == 2) {
      return mediumColor;
    } 
    else if (trust == 1) {
      return badColor;
    } 
    else if (trust == 0) {
      return reallyBadColor;
    } 
    else {
      return noColor;
    }
  }

  void shootBeam(float value, int trust) {
    int beamColor = calcColor(trust);
    stroke(beamColor);
    fill(beamColor);
    if (beamColor == noColor) {
      noStroke();
      noFill();
    }
    line(0, 0, value * conversionFactor, 0);
    strokeWeight(0);
    ellipseMode(CENTER);
    ellipse(value * conversionFactor, 0, beamSplashSize, beamSplashSize);
  }

  void drawWall(float valueF, float valueR, int trust, float carSize) {
    int beamColor = calcColor(trust);
    stroke(beamColor);
    fill(beamColor);
    if (beamColor == noColor) {
      noStroke();
      noFill();
    }
    strokeWeight(5 * (10 - trust * trust)*conversionFactor);

    line(valueF * conversionFactor, 0, valueR * conversionFactor, carSize);
    strokeWeight(1);
  }

  void display() {

    rectMode(CENTER);
    strokeWeight(2);
    stroke(sensorStroke);

    fill(sensorColor);
    rect(0, 0, sensorWidth, sensorLength);
    strokeWeight(1);
  }
}

class Floor {

  public boolean stop = false;
  float floorWidth = width * 5;
  float floorHeight = height * 100;
  float xdist = 31.0 * conversionFactor;
  float ydist = 31.0 * conversionFactor;
  float velocity = 0;
  float xpos = -floorWidth / 2;
  float ypos = -floorHeight / 2;
  int floorStroke = color(150);
  int floorWeight = 1;
  float horizontalRules;
  color floorColor = color(50);
  float verticalRules;
  float theta;

  Floor() {
    horizontalRules = (floorHeight / ydist);

    verticalRules = (floorWidth / xdist);
  }

  float getTheta() {
    return theta;
  }

  void setTheta(float newTheta) {
    theta = newTheta;
  }

  void setVelocity(float vel) {
    velocity = vel;
  }

  void resetPosition() {
    xpos = -floorWidth / 2;
    ypos = -floorHeight / 2;
  }

  float getY() {
    return ypos;
  }

  void updatePosition() {

    ypos = -floorWidth / 2 - worldY;
    xpos = -floorWidth / 2 - worldX;
  }

  void updatePositionBackwards() {

    ypos += cos(theta) * velocity;
    xpos -= sin(theta) * velocity;
  }

  public void updateFloor(CarPoint newPoint, Car newCar) {
    setTheta(newPoint.theta);
    setVelocity(newCar.velocity);
    if (!stop) {
      updatePosition();
    }
  }

  public void updateFloorBackwards(CarPoint newPoint, Car newCar) {
    setTheta(newPoint.theta);
    setVelocity(newCar.velocity);
    if (!stop) {
      updatePositionBackwards();
    }
  }

  void display() {
    pushMatrix();
    translate(xpos, ypos);
    fill(50);
    stroke(floorStroke);
    strokeWeight(floorWeight);
    for (int i = 0; i < horizontalRules; i++) {
      line(0, i * ydist, floorWidth, i * ydist);

      //System.out.println("Drawing floor rule: 0 " + i*ydist + " to " + floorWidth + " " +i*ydist);
    }
    for (int i = 0; i < verticalRules; i++) {
      line(i * xdist, 0, i * xdist, floorHeight);
    }
    strokeWeight(1);
    popMatrix();
  }
}

class WallSegment {

  public float oldTheta;
  public float oldX;
  public float oldY;
  public int oldTrust;
  public boolean isLeft;
  public float oldR;
  public float oldF;

  WallSegment(float theta, float x, float y, float F, float R, int trust, boolean left) {
    oldTheta = theta;
    oldX = x;
    oldY = y;
    oldR = R;
    oldF = F;
    oldTrust = trust;
    isLeft = left;
  }
}

class CommandWriter {
  FileWriter writer;
  File myFile;

  public CommandWriter(String fileName) {
    myFile = new File(dataPath(fileName));
    if (!myFile.exists()) {
      createFile(myFile);
    }
  }

  /**
   * Creates a new file including all subfolders
   */
  void createFile(File f) {
    File parentDir = f.getParentFile();
    try {
      parentDir.mkdirs(); 
      f.createNewFile();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }    

  void writeCommand(String command, int value) {
    try {
      writer = new FileWriter(myFile, true);
      writer.write(command+","+value+"\r\n");
      writer.flush();
      writer.close();
    }
    catch (IOException ex) {
      System.out.println("IOException " + ex + "while writing commands.");
    }
  }

  void writeTurn(int value) {
    writeCommand("turn", value);
  }

  void writeSpeed(int value) {
    writeCommand("speed", value);
  }

  void writeNextLeft() {
    writeCommand("takeNextLeft", 0);
  }

  void writeNextRight() {
    writeCommand("takeNextRight", 0);
  }

  void writeUnlockTurn() {
    writeCommand("turn", 0);
  }

  void writeUnlockSpeed() {
    writeCommand("speed", 0);
  }
}

class EventBox {
  float myXpos;
  float myYpos;
  float myWorldX;
  float myWorldY;
  float myWorldTheta;
  float largeSide = 100.0 * conversionFactor;
  float smallSide = 100.0 * conversionFactor;
  float Ysize;
  float Xsize;
  String command;
  boolean excited;
  boolean turn;
  color turnOutline = color(150, 0, 0, 200);
  color turnCenter = color(225, 0, 0, 100);
  color exciteOutline = color(0, 150, 0, 200);
  color exciteCenter = color(0, 225, 0, 100);
  color outline = color(0, 0, 150, 200);
  color center = color(0, 0, 225, 100);
  int value;

  public EventBox (float oldTheta, float oldX, float oldY) {
    myWorldX = oldX;
    myWorldY = oldY;
    myWorldTheta = oldTheta;

    if (leftTurnCount != rightTurnCount) {
      Ysize = largeSide;
      Xsize = smallSide;
    }
    else {
      Ysize = smallSide;
      Xsize = largeSide;
    }
  }

  public EventBox (float oldTheta, float oldX, float oldY, boolean isATurn, String newCommand) {
    turn = isATurn;
    myWorldX = oldX;
    myWorldY = oldY;
    myWorldTheta = oldTheta;
    Ysize = smallSide;
    command = newCommand;
    Xsize = largeSide;
  }

  public void updateXY() {
    myXpos = myWorldX - worldX;
    myYpos = myWorldY - worldY;
  }

  public boolean isTouching(float x, float y) {
    updateXY();
    if (x >= myXpos - Xsize / 2 && x <= myXpos + Ysize / 2) {
      if (y >= myYpos - Ysize / 2 && y <= myYpos + Ysize / 2) {
        return true;
      } 
      else {
        return false;
      }
    }
    return false;
  }

  public void display() {
    updateXY();
    pushMatrix();
    translate(myXpos, myYpos);
    rotate(myWorldTheta - worldTheta);

    if (isTouching(mouseX, mouseY) || isTouching(theCar.xpos, theCar.ypos)) {
      excited = true;
    }
    else {
      excited = false;
    }
    if (excited && automated && currentSmallPoint.time >= next_corner_time) {
      if (command.equals("takeNextLeft")) {
        cmd.writeNextLeft();
              take_next_time = currentSmallPoint.time + TAKE_NEXT_TIMEOUT;

      }
      else if (command.equals("takeNextRight")) {
        cmd.writeNextRight();
              take_next_time = currentSmallPoint.time + TAKE_NEXT_TIMEOUT;

      }
    }
    popMatrix();

    pushMatrix();
    rectMode(CENTER);
    if (excited) {
      stroke(exciteOutline);
      fill(exciteCenter);
    }
    else if (turn) {
      stroke(turnOutline);
      fill(turnCenter);
    }
    else {
      stroke(outline);
      fill(center);
    }
    rect(myXpos, myYpos, Xsize, Ysize);
    popMatrix();
  }
}

