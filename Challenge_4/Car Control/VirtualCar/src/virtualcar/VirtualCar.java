package virtualcar;
import processing.core.*;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import processing.event.*;

public class VirtualCar extends PApplet {

Car a;
Floor b;
float velocity;
float min_target; 
float new_target;
PFont f;
float max_target; 
float hall_left;
float hall_right;
String[] readouts;
String[] data;
int counter;

/*static public void main(String args[]) {
    PApplet.main("virtual_car");
}*/

public void setup(){
  size(displayWidth, displayHeight);
  readouts = new String[10];
  f = createFont("Arial",16,true);
  background(255);
  smooth();
  velocity = 10;
  a = new Car(displayWidth/2, displayHeight/2);
  hall_left = (displayWidth / 4);
  hall_right = displayWidth - (displayWidth / 4);
  new_target = (hall_left + hall_right / 2);
  min_target = hall_left + a.totalWidth;
  max_target = hall_right - a.totalWidth;
  a.setConfidenceDistance(((hall_right) - (hall_left))/5);
  b = new Floor();
  frameRate(30);
}

public void keyPressed() {

  if (key == CODED) {

    if (keyCode == UP) {

      velocity += 1.0f;

    } else if (keyCode == DOWN) {

      velocity -= 1.0f;
    } 

  }
}


public void mouseWheel(MouseEvent event) {
  float e = event.getAmount();
  velocity += -e;
}

public void draw(){
b.display();

  fill(255);
  rectMode(CORNERS);
  rect(0,0,width,height);
  b.setTheta(a.getTheta());
  b.setVelocity(-velocity);
  b.updatePosition();
  b.display();
  float new_theta;
  float new_turn;
  stroke(0);
  fill(50);
  rectMode(CORNERS);
  rect(0, 0, hall_left, height);
  rect(hall_right, 0, width, height);
  if (mousePressed)
  {   
    new_target = mouseX;
    if (new_target < min_target){
      new_target = min_target;
      strokeWeight(10);
      stroke(255,0,0);
      line(new_target, 0, new_target, height);
      strokeWeight(1);
    }
    else if (new_target > max_target){
      new_target = max_target;
      strokeWeight(10);
      stroke(255,0,0);
      line(new_target, 0, new_target, height);
      strokeWeight(1);
    }
  }
    strokeWeight(10);
    stroke(0,50,255,100);
    line(new_target, 0, new_target, height);
    strokeWeight(1);
    new_theta = a.calcTheta();
    new_turn = a.calcTurn(new_theta);
    a.setTarget(new_target);
    a.setTurn(new_turn);
    a.updateFrontWheels();
    a.updateTheta();
    a.updatePosition();
    a.setVelocity(velocity);
    a.calcReads(hall_left, hall_right);
    a.display();
    
    readouts[0] = "Car Position: " + a.xpos;
    readouts[1] = "Target Position: " + new_target;
    readouts[2] = "Car theta: " + degrees(a.theta);
    readouts[3] = "Target theta: " + degrees(new_theta);
    readouts[4] = "Car Velocity: " + a.velocity;  
    readouts[5] = "Car Turn Value: " + a.myTurn;
    readouts[6] = "";
    readouts[7] = "Floor position: " + b.getY();
    readouts[8] = "";
    readouts[9] = "";
    
      textFont(f,16);
      fill(255);
      
         for (int i = 0; i < readouts.length; i++){
                 text(readouts[i], 10, 100+20*i);
         }

}

class Floor{
  float yLimitUp = -(displayHeight*3);
  float yLimitDown = -displayHeight;
  float yReset = -(displayHeight *2);
  float floorWidth = displayWidth;
  float floorHeight = displayHeight * 5;
  float xdist = 100;
  float ydist = 100;
  float velocity = 0;
  float xpos = displayWidth/8;
  float ypos =  yReset;
  int floorStroke = color(150);
  int floorWeight = 1;
  float horizontalRules; 
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
  
  float getY(){
      return ypos;
  }
      void updatePosition(){
            if (xpos >= floorWidth/4){
       xpos = 0+(xpos%floorWidth/4);
    }
            else if (xpos <= -floorWidth/4){
                xpos = 0 - (xpos%floorWidth/4);
            }
            
           if (ypos <= yLimitUp){
       ypos = yReset + (ypos - yLimitUp);
    }
            else if (ypos >= yLimitDown){
                ypos = yReset + (yLimitDown - ypos);
            }
      
      //xpos += sin(theta) * velocity;
      ypos -= cos(theta) * velocity;
    }
  
  void display(){
      pushMatrix();
      translate(xpos, ypos);
      fill(50,50,50);
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

class IRSensor{
    
  float xpos;
  float ypos;
  float sensorWidth = 10.0f;
  float sensorLength = 15.0f;
  float theta = radians(0);
  int goodColor = color(0,255,0,150);
  int mediumColor = color(255,243,3,125);
  int badColor = color(255,184,3,100);
  int reallyBadColor = color(232,0,0,50);
  int sensorStroke = color(255);
  int sensorColor = color(0);
  
  IRSensor(float newX, float newY, float newTheta){
    xpos = newX;
    ypos = newY;
    theta = newTheta;
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
  
  void shootBeam(float value, int trust){
    if (trust == 4){
      stroke(goodColor);
      fill(goodColor);
    }
      else if (trust == 3){
        stroke(mediumColor);
        fill(mediumColor);
      }
        else if (trust == 2){
          stroke(badColor);
          fill(badColor);
        }
          else if(trust == 1){
            stroke(reallyBadColor);
            fill(reallyBadColor);
          }
            else {
              noStroke();
              noFill();
            }
            strokeWeight(4);
    line(0,0,value,0);
    strokeWeight(0);
    ellipseMode(CENTER);
    ellipse(value, 0, 15, 15);
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
      ypos = min;
    }
    else if (ypos < min){
      ypos = max;
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
  
class Wheel{
  WheelStripe[] stripes;
  float velocity = 10;
  float xpos;
  float ypos;
  float centerTurn = 1500;
  float turnAmount = 1500; // 1000 == full right
  float turnTheta;
  float wheelLength = 30.0f;
  float wheelWidth = 20.0f;
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
      stripes[i].move(velocity, 0+wheelLength/2, 0-wheelLength/2);
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
    float turnDiff = (turnAmount - centerTurn); //-500 for right, 500 left
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
    
  float confidence_distance = 200.0f;
  float[] reads =  new float[4];
  int[] trusts = new int[4];
  float velocity = 15.0f;
  float targetX = 400.0f;
  float leftWallDistance = -20.0f;
  float rightWallDistance = 20.0f;
  Wheel[] wheels = new Wheel[4];
  IRSensor[] sensors = new IRSensor[4];
  float theta = radians(0);
  float xpos;
  float ypos;
  float myTurn;
  float topLength = 100.0f;
  float maxAngle = 35.0f;
  float turnLength = 250.0f;
  float topWidth = 50.0f;
  float totalWidth = 75.0f;
  float totalLength = 125.0f;
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
      sensors[0] = new IRSensor(-topWidth/2, -topLength/2, radians(180));
      sensors[1] = new IRSensor(topWidth/2, -topLength/2, radians(0));
      sensors[2] = new IRSensor(-topWidth/2, topLength/2, radians(180));
      sensors[3] = new IRSensor(topWidth/2, topLength/2, radians(0));
      
    }
    
    void setConfidenceDistance(float conf){
        confidence_distance = conf;
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

public void markMidFront(){
    stroke(100,200,200);
    noFill();
    strokeWeight(4);
    ellipseMode(CENTER);
    ellipse(frontCenterX(), frontCenterY(), 15, 15);
}

public void markMidBack(){
    stroke(100,200,200);
    noFill();
    strokeWeight(4);
    ellipseMode(CENTER);
    ellipse(backCenterX(), backCenterY(), 15, 15);
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

public int calcTrust(float value){
    float ratio = confidence_distance/Math.abs(value);
    if (ratio > 1.0)
            return 4;
    else if (ratio > .75)
        return 3;
    else if (ratio > .50)
        return 2;
    else if (ratio > .25)
        return 1;
    else 
        return 0;
}
    
    float calcTheta(){
      float posdiff = targetX - xpos;
      float diff_ratio = posdiff / turnLength;
      if (diff_ratio > 1.0f){
        diff_ratio = 1.0f;
      }
      else if (diff_ratio < -1.0f){
        diff_ratio = -1.0f;
      }
      if (velocity > 0){
        
      return radians(maxAngle) * diff_ratio;
      }
      else 
      return radians(maxAngle) * diff_ratio;
       }
   
   
   float calcTurn(float theta_rad){
     float thetadiff;
     if (velocity > 0 ){
     thetadiff = theta_rad - theta;
     }
     else thetadiff = theta_rad + theta;
     float theta_ratio = thetadiff / radians(maxAngle);
     if (theta_ratio > 1.0f){
       theta_ratio = 1.0f;
     }
     else if (theta_ratio < -1.0f){
       theta_ratio = -1.0f;
     }
     return 1500 + (500 * theta_ratio);
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
    
      xpos += sin(theta) * velocity;
      //ypos -= cos(theta) * velocity;
    }
    
      void updateTheta(){
     float newt =  ((wheels[0].getTheta() / 45.0f) * velocity) / 2;
      
    theta = (theta +newt) % TWO_PI;}
    
    void setXY(float x, float y){
      xpos = x;
      ypos = y;
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
        pushMatrix();
        translate(currentWheel.getX(), currentWheel.getY());
        rotate(currentWheel.getTheta());
        currentWheel.display();
        popMatrix();
      }
    }
    
    void calcReads(float leftHall, float rightHall){
        reads[0] = calcRead(frontCenterX(), leftHall, theta, topWidth/2);
        reads[1] = calcRead(frontCenterX(), rightHall, theta, topWidth/2);
        reads[2] = calcRead(backCenterX(), leftHall, theta, topWidth/2);
        reads[3] = calcRead(backCenterX(), rightHall, theta, topWidth/2);
        calcTrusts();
    }
    
    void calcTrusts(){
        trusts[0] = calcTrust(reads[0]);
        trusts[1] = calcTrust(reads[1]);
        trusts[2] = calcTrust(reads[2]);
        trusts[3] = calcTrust(reads[3]);
    }
    
    
    float calcRead(float startx, float endx, float theta, float offset){
        float readLength = abs((startx - endx)) / cos(theta);
        return (readLength) - offset;
    }
    
    void drawSensors(){
      for (int i = 0; i < sensors.length; i++){
        IRSensor currentSensor = sensors[i];
        pushMatrix();
        translate(currentSensor.getX(), currentSensor.getY());
        rotate(currentSensor.getTheta());
        currentSensor.shootBeam(reads[i], trusts[i]);
        currentSensor.display();
        
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
}

