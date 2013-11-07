Beacon a;
Beacon b;
Beacon c;
Beacon d;
float aRead;
float bRead;
float cRead;
float dRead;
boolean jitter;
boolean offset_mouse;
float inset;
float defaultRadius;
float innerWidth;
float innterHeight;
PFont f;
float mouseOffset;
float[] Xguesses;
float[] Yguesses;
float[] Xstdevs;
float[] Ystdevs;
int guessCount;
float mOff;

void setup(){
  jitter = false;
  offset_mouse = false;
  //frameRate(1);
  size(700,700);
  Xguesses = new float[30];
  Yguesses= new float[30];
  Xstdevs= new float[30];
  Ystdevs= new float[30];
  guessCount = 0;
  inset = 200;
  defaultRadius = 150.0;
  innerWidth = width - inset;
  innterHeight = height-inset;
  mOff = 50.0;
  mouseOffset = 0.0;
  f = createFont("Arial",32,true);
  background(255);
  stroke(0);
  noFill();
  a = new Beacon(inset, inset, "A");
  b = new Beacon(innerWidth, inset, "B");
  c = new Beacon(inset, innerWidth, "C");
  d = new Beacon(innerWidth, innerWidth, "D");
  a.display();
}

public float calcDistance(float x1, float y1, float x2, float y2){
  float xsum = x1-x2;
  float ysum = y1-y2;
  float xsq = xsum * xsum;
  float ysq = ysum * ysum;
  return (float)Math.sqrt(ysq+xsq);
}

void rally(){
  a.getIntersections(a,b);
  a.getIntersections(a,c);
  a.getIntersections(a,d);
  a.getIntersections(b,c);
  a.getIntersections(b,d);
  a.getIntersections(c,d);
  a.drawInts(a.myIntersections);
}

void keyPressed() {
  if (key == CODED) {
    if (keyCode == UP) {
      if (jitter == true){
        jitter = false;
      }
      else {
        jitter = true;
        offset_mouse = true;
      }
    } else if (keyCode == DOWN) {
      if (offset_mouse == true){
        offset_mouse = false;
      }
      else {
        offset_mouse = true;
      }
  } 
}
}

Intersect[] sort(Intersect[] oldInts, float avgx, float avgy){
  Intersect[] ints = new Intersect[oldInts.length];
  for (int i = 0; i < oldInts.length; i ++){
    ints[i] = oldInts[i];
  }
    boolean swapped = true;
    int j = 0;
    Intersect tmp;
    while (swapped) {
        swapped = false;
        j++;
        for (int i = 0; i < ints.length - j; i++) {
           float curDist = calcDistance(ints[i].xpos,ints[i].ypos,avgx,avgy);
           float nextDist = calcDistance(ints[i+1].xpos,ints[i+1].ypos,avgx,avgy);
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

float average(float[] array){
  float sum  = 0.0;
  for (int i = 0; i<array.length; i++){
    sum += array[i];
  }
  return sum/array.length;
}

float stDev(float[] array){
  float avg = average(array);
  float sum = 0.0;
  for (int i = 0; i < array.length; i ++){
    sum += Math.pow((array[i] - avg),2);
  }
  return (float)Math.sqrt(sum/array.length);
}
    

void guess(Intersect[] ints){
  float ysum = 0.0f;
  float xsum = 0.0f;
  float yavg = 0.0f;
  float xavg = 0.0f;
  
  for (int i = 0; i < ints.length; i++){
    xsum += ints[i].xpos;
    ysum += ints[i].ypos;
}
  
  xavg = xsum/ints.length;
  yavg = ysum/ints.length;

  
  fill(0,0,255);
  //ellipse(xavg,yavg,25,25);
  //text("All Avg",xavg+20,yavg-20);
  
  xsum = 0.0;
  ysum = 0.0;

  Intersect[] better = sort(ints,xavg,yavg);
    for (int i = 0; i < better.length/2; i++){
    xsum += better[i].xpos;
    ysum += better[i].ypos;
}
    xavg = xsum/(ints.length/2);
  yavg = ysum/(ints.length/2);
  
  fill(0);
  
  //ellipse(xavg,yavg, 25,25);
  //text("Best Half Avg",xavg+20,yavg-20);

    Intersect[] best = new Intersect[ints.length/2];
      float bestXSum = 0.0;
  float bestYSum = 0.0;
  float bestXDevSum = 0.0;
  float bestYDevSum = 0.0;
  
  for (int i = 0; i < ints.length; i+=2){
    float one = calcDistance(ints[i].xpos,ints[i].ypos,xavg,yavg);
    float two = calcDistance(ints[i+1].xpos,ints[i+1].ypos,xavg,yavg);
    if (one <= two){
      best[i/2] = ints[i];
    }
    else {
      best[i/2] = ints[i+1];
    }
    bestXSum += best[i/2].xpos;
    bestXDevSum += Math.pow((best[i/2].xpos-xavg),2);

    bestYSum += best[i/2].ypos;
    best[i/2].display(color(255,0,0));
    bestYDevSum += Math.pow((best[i/2].ypos-yavg),2);

  }
  
  float bestXavg = bestXSum / best.length;
  float bestYavg = bestYSum / best.length; 
  float bestXDev = (float)Math.sqrt(bestXDevSum / best.length);
  float bestYDev = (float)Math.sqrt(bestYDevSum / best.length);
  
  Xguesses[guessCount] = bestXavg;
  Yguesses[guessCount] = bestYavg;
  Xstdevs[guessCount] = bestXDev;
  Ystdevs[guessCount] = bestYDev;
  
  guessCount++;
  noStroke();
  fill(0,255,0,100);
  if (jitter){
      fill(0,255,0,255);
  ellipse(average(Xguesses),average(Yguesses),stDev(Xguesses),stDev(Yguesses));}
  else {
      ellipse(average(Xguesses),average(Yguesses),25,25);
}
  //text("Best 1/2 avg",average(Xguesses),average(Yguesses));
}

void updateCircles(){
  a.setRadius(aRead);
  b.setRadius(bRead);
  c.setRadius(cRead);
  d.setRadius(dRead);
}

void simulate(){
  if (offset_mouse){
    mouseOffset = mOff;
  }
  else {
    mouseOffset = 0.0;
  }
  
  if (!jitter){
  a.setRadius(a.calcDistance(mouseX, mouseY)+mouseOffset);
  b.setRadius(b.calcDistance(mouseX, mouseY)+mouseOffset);
  c.setRadius(c.calcDistance(mouseX, mouseY)+mouseOffset);
  d.setRadius(d.calcDistance(mouseX, mouseY)+mouseOffset);
}
  
  else{
  
  a.setRadius(a.calcDistance(mouseX, mouseY)+random(-mouseOffset,mouseOffset));
  b.setRadius(b.calcDistance(mouseX, mouseY)+random(-mouseOffset,mouseOffset));
  c.setRadius(c.calcDistance(mouseX, mouseY)+random(-mouseOffset,mouseOffset));
  d.setRadius(d.calcDistance(mouseX, mouseY)+random(-mouseOffset,mouseOffset));}

}

void draw(){
  if (guessCount >= 30){
    guessCount = 0;
  }
  a.clearIntersections();
  background(255);
  noFill();
  stroke(0);
  rect(inset, inset, innerWidth-inset, innerWidth-inset);
  simulate();
  a.display();
  b.display();
  c.display();
  d.display();
  if (mousePressed)
  {
  rally();
  guess(a.myIntersections);
}
  textFont(f, 32);
  fill(0);
  //text(a.getDistance(a,b),width/2, inset/2);
}

class Intersect{
  public String myName;
    public String parentNames;
  public float xpos;
  public float ypos;
  public float size = 10.0f;
  public float myColor = (0);
  public float myFill = (255);

  public Intersect(float newX, float newY, String newName, String newParents){
    xpos = newX;
    ypos = newY;
    myName = newName;
    parentNames = newParents;
  }
  
    void display(int c){
    stroke(0);
    fill(c);
    ellipse(xpos, ypos, size, size);
    stroke(0);
    fill(0);
    textFont(f, 16);
    //text(myName, xpos+10, ypos-10);
}
  
  void display(){
    stroke(myColor);
    fill(myFill);
    ellipse(xpos, ypos, size, size);
    stroke(0);
    fill(0);
    textFont(f, 16);
    //text(myName, xpos+10, ypos-10);
}
}

class Beacon{
  public Intersect[] myIntersections;
  public String myName;
  public float xpos;
  public float ypos;
  public float radius;
  public int myColor = color(255);
  
  public float getDistance(Beacon A, Beacon B){
   return A.calcDistance(B.xpos, B.ypos);
}

public boolean isIntersecting(Beacon A, Beacon B){
  double D = getDistance(A,B);
  if ((A.radius + B.radius) >= D){
    return true;
  }
  else {
  return false;
  }
  }

public Intersect[] getIntersections(Beacon A, Beacon B){
  Intersect[] inters;
    float x1 = A.xpos;
  float x2 = B.xpos;
  float y1 = A.ypos;
  float y2 = B.ypos;
  float r1 = A.radius;
  float r2 = B.radius;
  double D = getDistance(A,B);
  if (isIntersecting(A,B)){
    double d1 = (Math.pow(r1,2) - Math.pow(r2,2) + Math.pow(D,2)) / (2*D);
    double h = Math.sqrt(r1*r1 - d1*d1);
    double x3 = x1 + (d1 * (x2-x1)) / D;
    double y3 = y1 + (d1 * (y2-y1)) / D;
    float x4 = (float)(x3 + (h * (y2-y1)) / D);
    float y4 = (float)(y3 - (h * (x2-x1)) / D);
    float x5 = (float)(x3 - (h * (y2-y1)) / D);
    float y5 = (float)(y3 + (h * (x2-x1)) / D);
    inters = new Intersect[2];
    inters[0] = new Intersect(x4, y4, A.myName+B.myName+"1", (A.myName+B.myName));
    inters[1] = new Intersect(x5, y5, A.myName+B.myName+"2", (A.myName+B.myName));
  }
  else{
    inters = new Intersect[0];
  }
    myIntersections = storeIntersections(myIntersections, inters);
    return inters;
}

public Intersect[] storeIntersections(Intersect[] existingInters, Intersect[] newInters){
  Intersect[] temp;
  int size = newInters.length + existingInters.length;
    temp = new Intersect[size];
    for (int i = 0; i < temp.length; i++){
      if (i < existingInters.length){
        temp[i] = existingInters[i];
      }
      else 
        temp[i] = newInters[i-existingInters.length];
    }
return temp;  
}


public void drawInts(Intersect[] inters){
  Intersect temp;
  for (int i = 0; i < inters.length; i ++){
     inters[i].display();
  }
}

public void clearIntersections(){
  myIntersections = new Intersect[0];
}
    

public void drawIntersections(Beacon A, Beacon B){
  float x1 = A.xpos;
  float x2 = B.xpos;
  float y1 = A.ypos;
  float y2 = B.ypos;
  float r1 = A.radius;
  float r2 = B.radius;
  double D = getDistance(A,B);
  if ((r1 + r2) >= D){
    double d1 = (Math.pow(r1,2) - Math.pow(r2,2) + Math.pow(D,2)) / (2*D);
    double h = Math.sqrt(r1*r1 - d1*d1);
    double x3 = x1 + (d1 * (x2-x1)) / D;
    double y3 = y1 + (d1 * (y2-y1)) / D;
    float x4 = (float)(x3 + (h * (y2-y1)) / D);
    float y4 = (float)(y3 - (h * (x2-x1)) / D);
    float x5 = (float)(x3 - (h * (y2-y1)) / D);
    float y5 = (float)(y3 + (h * (x2-x1)) / D);
    stroke(0);
    fill(0);
    
    textFont(f, 16);
  fill(0);    
    text(A.myName + ":" + B.myName + "1", x4+10, y4-10);
    ellipse(x4,y4,10,10);
    text((A.myName + ":" + B.myName + "2"), x5+10, y5-10);
    ellipse(x5,y5,10,10);
  }
}

//public float calcH(Beacon A, Beacon B, float
  
  public Beacon(float newX, float newY, float newR, color c, String name){
    myIntersections = new Intersect[0];
    xpos = newX;
    ypos = newY;
    radius = newR;
    myColor = c;
    myName = name;
  }
  
    public Beacon(float newX, float newY, String name){
    xpos = newX;
    ypos = newY;
    radius = defaultRadius;
    myColor = color(0);
    myName = name;
        myIntersections = new Intersect[0];

  }

public float calcDistance(float x, float y){
  float xsum = xpos-x;
  float ysum = ypos-y;
  float xsq = xsum * xsum;
  float ysq = ysum * ysum;
  return (float)Math.sqrt(ysq+xsq);
}

public void setRadius(float r){
  radius = r;
}

public void display(){
  textFont(f, 32);
  fill(0);
  text(myName, xpos+30, ypos + 30);
  stroke(0);
  fill(0);
  ellipse(xpos,ypos,10,10);
  fill(myColor);
  if (myColor == color(0)){
    noFill();
  }
  ellipse(xpos, ypos, radius*2, radius*2);
}
}


