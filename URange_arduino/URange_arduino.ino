void setup()
{
  // initialize the serial communication:
  Serial.begin(9600);
  delay(1000); // let sensor start up
}

void loop() {
  char sensorInput[3];
  
  Serial.readBytesUntil('\r', sensorInput, 4);
  Serial.println(sensorInput);
  
  delay(50);
}
