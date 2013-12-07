
/*IR_DAEMON Constants*/
double CAR_LENGTH = 10.0; // in 'IR' units, whatever those are
double CONFIDENCE_DISTANCE = 20.0; // in 'IR' units
IRDaemon IR_DAEMON = new IRDaemon();
/**************************/
SmallPoint sp;
BufferedReader reader;
Scrubber timeScrubber;
String[] readouts;
ArrayList<CarPoint> allPoints = new ArrayList<CarPoint>();
int lastSize;
PFont f;
int csvWidth;
float maxVelocity;
float conversionFactor; // reads to pixels
float hall_left;
float fontSize;
float hall_right;
CarPoint currentPoint;
int currentIndex;
boolean live;
float scrubber_width;
float scrubber_y;
float scrubber_inset;
boolean playback;
boolean scrubbing;
boolean reset;
boolean loop;
Button fast;
Button med;
Button slow;
Car theCar;
Floor theFloor;
 
void setup() {
  
  currentIndex = 0;
  live = false;
  playback = true;
  scrubbing = false;
  loop = false;
  // Open the file from the createWriter() example
  //reader = createReader("12-6-freakouts.csv");    
  reader = createReader("../live.csv");   
  //size(600,600);
  size(displayWidth, displayHeight);
  scrubber_inset = 50.0;
  scrubber_width = width-scrubber_inset*2;
  scrubber_y = height-scrubber_inset*2;
  smooth();
  frameRate(30);
  hall_left = (width/4);
  hall_right = width - (width/4);
  lastSize = 0;
  csvWidth = 8;

  
  conversionFactor = 1.0 + 2.0 *width / displayWidth;
  maxVelocity = 35.0 * conversionFactor;
  readouts = new String[10];
    fontSize = (int) 4.0 *conversionFactor;
    if (fontSize <= 1){
      fontSize = 1.0;
    }
  
  timeScrubber = new Scrubber();
  f = createFont("Arial",fontSize,true);
  fast = new Button(scrubber_inset + 40 * conversionFactor, height - scrubber_inset * 4, "fast"); 
  med = new Button(scrubber_inset + 25 * conversionFactor, height -  scrubber_inset * 4, "med");
  slow = new Button(scrubber_inset + 10 * conversionFactor, height - scrubber_inset * 4, "slow");
  
  background(255);
  checkData();
  if (loop == false){
    currentIndex = allPoints.size()-1;
  }
  theCar = new Car(width/2, height/2);
  theFloor = new Floor();
}

public float storeData(String newData){
  if (newData == "" || newData == null)
    return 0.0f;
  else return float(newData);
}
 
void draw() {
  if (allPoints.size() != 0)
  {
  checkData();
  if (live == true){
    currentPoint = allPoints.get(allPoints.size()-1);
  }
  else if (playback == true){
    currentPoint = allPoints.get(currentIndex);
    timeScrubber.xpos = scrubber_inset + ((float)currentIndex / allPoints.size())*scrubber_width; 
    if (currentIndex == allPoints.size() -1){
      if (loop == true){
        currentIndex = 0;
        theFloor.resetPosition();
      }
    }
    else
      currentIndex++;
  }
  }
    
  if (currentPoint != null && playback == true){
  theCar.updateCar(currentPoint);
  theFloor.updateFloor(currentPoint, theCar);
  }
  
  fill(255);
  rectMode(CORNERS);
  rect(0,0,width,height);
  
  //printAllPoints();
    theFloor.display();
  theCar.display();
  //rect(hall_right, 0, width, height);
  printData(theCar);
  timeScrubber.display();
  fast.display();
  med.display();
  slow.display();
  }


void printAllPoints() {
  if (allPoints.size() != lastSize){
  for (int i = 0; i < allPoints.size(); i ++){
    allPoints.get(i).printPoint();
  }
  lastSize = allPoints.size();
}
}

CarPoint processNewPoint(CarPoint oldPoint){
  IR_DAEMON.updateReads(IR_DAEMON.getOldVolts(oldPoint.LF), IR_DAEMON.getOldVolts(oldPoint.RF), IR_DAEMON.getOldVolts(oldPoint.LR), IR_DAEMON.getOldVolts(oldPoint.RR));
  IR_DAEMON.setReads((int)oldPoint.LF, (int)oldPoint.RF, (int)oldPoint.LR,(int) oldPoint.RR);
  IR_DAEMON.pickDirection();
  return IR_DAEMON.getCarpoint((int)oldPoint.turn, (int)oldPoint.velocity, false, false);
}

CarPoint processNewPoint(SmallPoint sp){
  IR_DAEMON.updateReads(IR_DAEMON.getOldVolts((double)sp.leftFront), IR_DAEMON.getOldVolts((double)sp.rightFront), IR_DAEMON.getOldVolts((double)sp.leftRear), IR_DAEMON.getOldVolts((double)sp.rightRear));
  IR_DAEMON.setReads(sp.leftFront, sp.rightFront, sp.leftRear, sp.rightRear);
  IR_DAEMON.pickDirection();
  return IR_DAEMON.getCarpoint((int)sp.setTurn, (int)sp.setSpeed, false, false);
}

void mousePressed() {
  if (mouseButton == LEFT){
  if (overScrubber()){
    scrubbing = true;
    if (playback == true){
      reset = true;
      playback = false;
      theCar.stop = true;
      theFloor.stop = true;
    }
    
  }
  else if (fast.isOver()){
    frameRate(100);
  }
  else if (med.isOver()){
    frameRate(30);
  }
  else if (slow.isOver()){
    frameRate(20);
  }
    
  
  else{
    if (loop == true){
      loop = false;
    }
    else loop = true;
  }
}
else {
  if (playback == false){
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

void mouseDragged(){
  if (scrubbing){
    playback = false;
    theCar.stop = true;
    theFloor.stop = true;
    timeScrubber.xpos = mouseX;
    if (timeScrubber.xpos < scrubber_inset){
      timeScrubber.xpos = scrubber_inset;
    }
    else if (timeScrubber.xpos > width - scrubber_inset){
      timeScrubber.xpos = width - scrubber_inset;
    }
    int lastIndex = currentIndex;
    currentIndex = (int)((float)allPoints.size()*((float)(timeScrubber.xpos - scrubber_inset)/scrubber_width));
    if (currentIndex > allPoints.size()-1){
      currentIndex = allPoints.size()-1;
    }
  }
  if (lastIndex > currentIndex){
    
  forceUpdateCarFloorBackwards();
  }
  else {
    forceUpdateCarFloor();
  }
} 
  

void mouseReleased() {
  if (mouseButton == LEFT){
    if (reset){
  playback = true; 
  theCar.stop = false;
  theFloor.stop = false;
  reset = false;
}
  scrubbing = false;
  }
}


boolean overScrubber(){
  if (mouseX >= timeScrubber.xpos - timeScrubber.size/3 && mouseX <= timeScrubber.xpos + timeScrubber.size/3){
    if (mouseY >= timeScrubber.ypos - timeScrubber.size/2 && mouseY <= timeScrubber.ypos + timeScrubber.size/2){
      return true;
    }
    else return false;
  }
  else return false;
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
                    booleans = Integer.parseInt(currentLine[7]);
            
        
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
            while ((thisLine = reader.readLine()) != null) {
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
                } else {
                    if (currentLine.length == csvWidth) {
                        //allPoints.add(processLineToCarpoint(currentLine));
                        SmallPoint current = processLineToSmallPoint(currentLine);
                        allPoints.add(processNewPoint(current));
                        }
                    }
                }
            
        } catch (IOException e) {
        }
    }
  void drawVerticalLine(float xpos, color c){
     strokeWeight(10);
     fill(c);
     stroke(c);
     line(xpos,displayHeight, xpos, 0);
     strokeWeight(1);
  }
  
  public void forceUpdateCarFloor(boolean reverse){
      boolean stopReset = theCar.stop;
  currentPoint = allPoints.get(currentIndex);
  theCar.stop = false;
  theFloor.stop = false;
  if (!reverse){   
  theCar.updateCar(currentPoint);
  theFloor.updateFloor(currentPoint, theCar);
  }
  else{
    theCar.updateCarBackwards(currentPoint);
    theFloor.updateFloorBackwards(currentPoint, theCar);
  }
  theCar.stop = stopReset;
  theFloor.stop = stopReset;
  }


public void mouseWheel(MouseEvent event) {
  float e = event.getAmount();
  if (e < 0){
      if (currentIndex < allPoints.size()-1){
        currentIndex++;
      }
}
  else if (e > 0){
      if (currentIndex > 0){
        currentIndex--;
  }
}
  forceUpdateCarFloor();
  timeScrubber.xpos = scrubber_inset + ((float)currentIndex / allPoints.size())*scrubber_width; 
}

public void printData(Car newCar){
  
    
    readouts[0] = "Car Position: " + newCar.distance;
    readouts[1] = "Target Position: " + newCar.targetX;
    readouts[2] = "Car theta: " + degrees(newCar.theta);
    readouts[3] = "Target theta: " + degrees((float)IR_DAEMON.avgTarget);
    readouts[4] = "Car Speed Set: " + newCar.mySpeedSet;  
    readouts[5] = "Car Turn Value: " + newCar.myTurn;
    readouts[6] = "Car Velocity: " + newCar.velocity;
    readouts[7] = "Scroll up/down to move frame-by-frame.";
    readouts[8] = "Left click and drag to scrub through timepoints.";
    readouts[9] = "Right click to start/stop playback.";
    
      
  stroke(0);
  fill(50, 50, 50, 100);
  rectMode(CORNERS);
    rect(10*conversionFactor, 10*conversionFactor, 100*conversionFactor, (15+readouts.length*5)*conversionFactor, 15);
      textFont(f,fontSize);
      fill(255);
      
         for (int i = 0; i < readouts.length; i++){
                 text(readouts[i], 15*conversionFactor, 15*conversionFactor+5*conversionFactor*i);
         }
         }
         
public class Scrubber{
public float xpos = scrubber_inset;
public float ypos = scrubber_y;
public float size = conversionFactor * 15.0;

public Scrubber(){
}

public void display(){
  strokeWeight(3);
  stroke(0);
  line(scrubber_inset, scrubber_y+scrubber_inset/2, scrubber_inset, scrubber_y-scrubber_inset/2);
  line(width-scrubber_inset, scrubber_y+scrubber_inset/2, width-scrubber_inset, scrubber_y-scrubber_inset/2);
  line(scrubber_inset, scrubber_y, width-scrubber_inset, scrubber_y);
  strokeWeight(2);
  stroke(255);
  line(scrubber_inset, scrubber_y+scrubber_inset/2, scrubber_inset, scrubber_y-scrubber_inset/2);
  line(width-scrubber_inset, scrubber_y+scrubber_inset/2, width-scrubber_inset, scrubber_y-scrubber_inset/2);
  line(scrubber_inset, scrubber_y, width-scrubber_inset, scrubber_y);
  
  strokeWeight(1);
  stroke(0);
  fill(255);
  rectMode(CENTER);
  rect(xpos,ypos,size*2/3, size,10);
}
}

public class Button{
  public float xpos;
  public float ypos;
  public float size = 10.0 * conversionFactor;
  public String name;
  
  public Button(float x, float y, String newName){
    name = newName;
    xpos = x;
    ypos = y;
  }
  
  public void display(){
    stroke(0);
    fill(255);
    rectMode(CENTER);
    rect(xpos, ypos, size, size*2/3,size/8);
    stroke(0);
    fill(0);
    text(name, xpos-size/3, ypos+size/8);
  }


  public boolean isOver(){
    if (mouseX >= xpos - size/2 && mouseX <= xpos + size/2){
      if (mouseY >= ypos - size/3 && mouseY <= ypos + size/3){
        return true;
      }
      else return false;
    }
    else return false;
  }

}

class Wheel{
  class WheelStripe{
  float stripeWidth;
  float stripeLength;
  float xpos;
  float ypos;
  float stripeWeight = 2;
  int stripeStroke = color(60);
  
  WheelStripe(float newX, float newY, float newWidth, float newLength){
    xpos = newX;
    ypos = newY;
    stripeWidth = newWidth;
    stripeLength = newLength;
}

  public void move(float velocity, float maxY, float minY){
    float max = maxY - stripeWeight;
    float min = minY + stripeWeight;
    ypos = ypos - velocity/5;
    if (ypos > max){
      ypos = min + (ypos-max);
    }
    else if (ypos < min){
      ypos = max + (ypos - min);
  }
  }
  
  public void display(){
    stroke(stripeStroke);
    strokeWeight(stripeWeight);
    line(xpos-stripeWidth/2, ypos, xpos+stripeWidth/2, ypos);
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
  float wheelLength = (wheelSize * conversionFactor) * 6/5;
  float wheelWidth = (wheelSize * conversionFactor) * 4/5;
  float maxTheta = radians(30.0f);
  float wheelWeight = 2;
  int wheelColor = color(150);
  int wheelStroke = color(0);
  
  Wheel(float newX, float newY){
    xpos = newX;
    ypos = newY;
    updateTheta();
    //stripes[0] = new WheelStripe(xpos, ypos, wheelWidth, wheelLength);
    initStripes();
  }
  
  void setVelocity(float newVel){
      velocity = newVel;
  }
  
  int calcStripes(){
    return (int)Math.floor(wheelLength/(wheelWeight * 6));
  }
  

  
  void initStripes(){
    stripes = new WheelStripe[calcStripes()];
    float yStart = 0 - (wheelLength/2) + wheelWeight + 1;
    for (int i = 0; i < stripes.length; i++){
      stripes[i] = new WheelStripe(0, (yStart + (wheelWeight *7) * i), wheelWidth-1, wheelLength-1);
    }
  }
  
  void moveStripes(){
    for (int i = 0; i < stripes.length; i++){
      if (!stop){
      stripes[i].move(velocity, 0+wheelLength/2, 0-wheelLength/2);
      }
      stripes[i].display();
    }
  }
    
  float getTurn(){
    return turnAmount;
  }
  
  float getX(){
    return xpos;
  }
  
  float getY(){
    return ypos;
  }
  
  float getTheta(){
    return turnTheta;
  }
  
  void updateTheta(){
    float turnDiff = (centerTurn - turnAmount); //-500 for right, 500 left
    turnTheta = (float)(turnDiff / 500) * maxTheta;
  }
  
  void updateTurn(float turn){
    turnAmount = turn;
    updateTheta();
  }
  
  void display(){
    
    rectMode(CENTER);
    noStroke();
    fill(wheelColor);
    rect(0,0,wheelWidth, wheelLength,5,5,5,5);
    moveStripes();
    noFill();
    strokeWeight(wheelWeight);
    stroke(wheelStroke);
    rect(0,0,wheelWidth, wheelLength,5,5,5,5);
    strokeWeight(1);
    stroke(0);
    
  }
}

class Car{
  public boolean stop = false;
  public float[] reads =  new float[4];
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
  float topLength = mySize*conversionFactor;
  float topWidth = mySize*conversionFactor / 2;
  float totalWidth = mySize*conversionFactor * 3/4;
  float totalLength = mySize * conversionFactor * 5/4;
  float startTurn;
  float stopTurn;
  int topColor = color(50);
  int topStroke = 0;
  int totalColor = color(86, 155, 105);
  int totalStroke = color(0);
  
    Car(float x, float y){
      xpos = x;
      ypos = y;
      wheels[0] = new Wheel(-totalWidth/2, -totalLength/2); 
      wheels[1] = new Wheel(totalWidth/2, -totalLength/2);
      wheels[2] = new Wheel(-totalWidth/2, totalLength/2);
      wheels[3] = new Wheel(totalWidth/2, totalLength/2);
      sensors[0] = new IRSensor(-topWidth/2, -topLength/2, radians(180)); //LF
      sensors[1] = new IRSensor(topWidth/2, -topLength/2, radians(0)); //RF
      sensors[2] = new IRSensor(-topWidth/2, topLength/2, radians(180)); //LR
      sensors[3] = new IRSensor(topWidth/2, topLength/2, radians(0)); //RR
      
    }
    
    void setTarget(float x){
      targetX = x;
    }
    
    float getTheta(){
        return theta;
    }
    
    void setVelocity(float new_vel){
      velocity = new_vel;
      for (int i = 0; i < wheels.length; i ++){
          wheels[i].setVelocity(new_vel);
      }
    }
    
    public void updateCar(CarPoint newPoint){
       reads[0] = newPoint.LF;
       reads[1] = newPoint.RF;
       reads[2] = newPoint.LR;
       reads[3] = newPoint.RR;
       trusts[0] = (int)newPoint.LT;
       trusts[2] = (int)newPoint.LT;
       trusts[1] = (int)newPoint.RT;
       trusts[3] = (int)newPoint.RT;
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
       //updatePosition();
    }
    
public float frontCenterX(){
    float tangent = topLength/2;
    float dx = sin(theta)*tangent;
    float newx = xpos + dx;
    return newx;
}

public float frontCenterY(){
    float tangent = topLength/2;
    float dy = cos(theta)*tangent;
    float newy = ypos - dy;
    return newy;
}

public float backCenterX(){
    float tangent = topLength/2;
    float dx = sin(theta)*tangent;
    float newx = xpos - dx;
    return newx;
}

public float backCenterY(){
    float tangent = topLength/2;
    float dy = cos(theta)*tangent;
    float newy = ypos + dy;
    return newy;
}

public float calcVelocity(float speedSet){
      float speedDiff;
      float speedRatio;
      speedDiff = speedSet - 1500;
      speedRatio = speedDiff / 500;
      return -speedRatio * maxVelocity;
    }

    
    void updatePosition(){
            if (xpos > width+totalWidth){
       xpos = 0+(xpos-(width+totalWidth));
    }
      if (ypos > height+totalLength){
        ypos = 0+(ypos-height-totalLength);}
      if (xpos < 0){
        xpos = width + xpos + totalWidth;
      }
      if (ypos < 0){
        ypos = height + ypos+totalLength;
      }
    
      //xpos += sin(theta) * velocity;
      //ypos -= cos(theta) * velocity;
           if (distance < 0){
       //xpos = hall_left - distance * conversionFactor;
        xpos = width/2 + (-width/4 - distance*conversionFactor);
      }
      else {
        xpos = width/2 - (-width/4 - distance*conversionFactor);
      }
    }
    
        void updatePositionBackwards(){
            if (xpos > width+totalWidth){
       xpos = 0+(xpos-(width+totalWidth));
    }
      if (ypos > height+totalLength){
        ypos = 0+(ypos-height-totalLength);}
      if (xpos < 0){
        xpos = width + xpos + totalWidth;
      }
      if (ypos < 0){
        ypos = height + ypos+totalLength;
      }
    
      //xpos += sin(theta) * velocity;
      //ypos -= cos(theta) * velocity;
           if (distance < 0){
       //xpos = hall_left - distance * conversionFactor;
        xpos = width/2 - (-width/4 - distance*conversionFactor);
      }
      else {
        xpos = width/2 + (-width/4 - distance*conversionFactor);
      }
    }
    
    void setXY(float x, float y){
      xpos = x;
      ypos = y;
    }
    
    void setX(float x){
      xpos = x;
    }
    
    void setY(float y){
      ypos = y;
    }
    
    void setTheta(float theta_rad){
      theta = theta_rad;
    }
    
    void setTurn(float newTurn){
      myTurn = newTurn;
    }
    
    void updateFrontWheels(){
      for (int i = 0; i < 2; i ++){
        wheels[i].updateTurn(myTurn);
             }
    }
      
    void drawWheels(){
      for (int i = 0; i < wheels.length; i++){
        Wheel currentWheel = wheels[i];
        currentWheel.stop = stop;
        pushMatrix();
        translate(currentWheel.getX(), currentWheel.getY());
        rotate(currentWheel.getTheta());
        currentWheel.display();
        popMatrix();
      }
    }
    
    void drawSensors(){
      for (int i = 0; i < sensors.length; i++){
        IRSensor currentSensor = sensors[i];
        pushMatrix();
        translate(currentSensor.getX(), currentSensor.getY());
        rotate(currentSensor.getTheta());
        currentSensor.shootBeam(reads[i], trusts[i]);
        if (i == 1){
          currentSensor.drawWall(reads[i], reads[i+2], trusts[i], mySize*conversionFactor);
        }
        else if (i == 0){
          currentSensor.drawWall(reads[i], reads[i+2], trusts[i], -mySize*conversionFactor);
        }
        currentSensor.display();
        popMatrix();
        pushMatrix();
        translate(currentSensor.getX(), currentSensor.getY());
        currentSensor.writeRead(reads[i], trusts[i]);
        popMatrix();
      }
    }
      
    
    void display(){
      
      pushMatrix();
      translate(xpos, ypos);
      rotate(theta);
      rectMode(CENTER);
      fill(totalColor);
      stroke(totalStroke);
      strokeWeight(2);
      rect(0,0, totalWidth, totalLength);
            strokeWeight(1);

      fill(topColor);
      stroke(topStroke);
      rect(0,0, topWidth, topLength);
      drawWheels();
      drawSensors();
      popMatrix();
    }
}

class IRSensor{
    
  float xpos;
  float ypos;
  float sensorSize = 2.0f;
  float beamSplashSize = sensorSize * conversionFactor * 1.5;
  float sensorWidth = sensorSize * conversionFactor;
  float sensorLength = sensorSize * conversionFactor * 1.5;
  float theta = radians(0);
  int goodColor = color(0,255,0,150);
  int mediumColor = color(255,243,3,150);
  int badColor = color(255,184,3,150);
  int reallyBadColor = color(232,0,0,150);
  int noColor = color(0,0,0,0);
  int sensorStroke = color(255);
  int sensorColor = color(0);
  PFont f;
  
  IRSensor(float newX, float newY, float newTheta){
    xpos = newX;
    ypos = newY;
    theta = newTheta;
    f = createFont("Arial",fontSize*2,true);
  }
  
  
    float getX(){
    return xpos;
  }
  
  float getY(){
    return ypos;
  }
  
  float getTheta(){
    return theta;
  }
  
  void writeRead(float read, int trust){
      int textColor = calcColor(trust);
      fill(textColor);
          if (textColor == noColor){
        noStroke();
        noFill();
    }
      text((int)read,xpos,ypos);
  }
  
  int calcColor(int trust){
          if (trust == 3){
      return goodColor;
    }
      else if (trust == 2){
        return mediumColor;
      }
        else if (trust == 1){
         return badColor;
        }
          else if(trust == 0){
              return reallyBadColor;
          }
            else {
              return noColor;
            }
  }
  
  void shootBeam(float value, int trust){
    int beamColor = calcColor(trust);
    stroke(beamColor);
    fill(beamColor);
    if (beamColor == noColor){
        noStroke();
        noFill();
    }
    line(0,0,value*conversionFactor,0);
    strokeWeight(0);
    ellipseMode(CENTER);
    ellipse(value*conversionFactor, 0, beamSplashSize, beamSplashSize);
  }
  
    void drawWall(float valueF, float valueR, int trust, float carSize){
    int beamColor = calcColor(trust);
    stroke(beamColor);
    fill(beamColor);
    if (beamColor == noColor){
        noStroke();
        noFill();
    }
    strokeWeight(3);
    
    line(valueF*conversionFactor, 0, valueR*conversionFactor, carSize);
    strokeWeight(1);
  }
    
  
  void display(){
    
    rectMode(CENTER);
    strokeWeight(2);
    stroke(sensorStroke);
    
    fill(sensorColor);
    rect(0,0,sensorWidth, sensorLength);
    strokeWeight(1);
  }
    
}

class Floor{
  public boolean stop = false;
  float floorWidth = width * 5;
  float floorHeight = height * 100;
  float xdist = 31.0 * conversionFactor;
  float ydist = 31.0 * conversionFactor;
  float velocity = 0;
  float xpos = -floorWidth/2;
  float ypos = -floorHeight/2;
  int floorStroke = color(150);
  int floorWeight = 1;
  float horizontalRules; 
  color floorColor = color(50);
  float verticalRules;
  float theta;
  
  
  Floor(){
            horizontalRules = (floorHeight / ydist);

      verticalRules = (floorWidth / xdist);
  }
  
    float getTheta(){
    return theta;
  }
    
    void setTheta(float newTheta){
        theta = newTheta;
    }
  
  void setVelocity(float vel){
      velocity = vel;
  }
  
  void resetPosition(){
    xpos = -floorWidth/2;
    ypos = -floorHeight/2;
  }
  
  float getY(){
      return ypos;
  }
      void updatePosition(){
      
      ypos += cos(theta) * velocity ;
      xpos -= sin(theta) * velocity ;
    }
    
          void updatePositionBackwards(){
      
      ypos += cos(theta) * velocity ;
      xpos -= sin(theta) * velocity ;
    }
  
  
  public void updateFloor(CarPoint newPoint, Car newCar){
    setTheta(newPoint.theta);
    setVelocity(newCar.velocity);
    if (!stop){
    updatePosition();
    }
  }
  
    public void updateFloorBackwards(CarPoint newPoint, Car newCar){
    setTheta(newPoint.theta);
    setVelocity(newCar.velocity);
    if (!stop){
    updatePositionBackwards();
    }
  }
  
  void display(){
      pushMatrix();
      translate(xpos, ypos);
      fill(50);
      stroke(floorStroke);
      strokeWeight(floorWeight);
      for (int i = 0; i <horizontalRules; i++ ){
          line(0, i*ydist, floorWidth, i*ydist);
      
          //System.out.println("Drawing floor rule: 0 " + i*ydist + " to " + floorWidth + " " +i*ydist);
      }
      for (int i = 0; i <verticalRules; i++){
          line(i*xdist, 0, i*xdist, floorHeight);
      }
      strokeWeight(1);
      popMatrix();
  }
}

/* Paste classes from SunSPOT code here! */


