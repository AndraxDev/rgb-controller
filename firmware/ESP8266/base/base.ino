// Load Wi-Fi library
#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <Arduino_JSON.h>

// Replace with your network credentials
const char* ssid     = "E02R43ETT15";
const char* password = "1234567890";
String serverPath = "http://137.184.46.160/get.php";

// Set web server port number to 80
WiFiServer server(80);

// Decode HTTP GET value
String redString = "0";
String greenString = "0";
String blueString = "0";
int pos1 = 0;
int pos2 = 0;
int pos3 = 0;
int pos4 = 0;

// Variable to store the HTTP req  uest
String header;

// Red, green, and blue pins for PWM control
const int redPin = 13;     // 13 corresponds to GPIO13
const int greenPin = 12;   // 12 corresponds to GPIO12
const int bluePin = 14;    // 14 corresponds to GPIO14

// Setting PWM bit resolution
const int resolution = 256;

// Current time
unsigned long currentTime = millis();
// Previous time
unsigned long previousTime = 0; 
// Define timeout time in milliseconds (example: 2000ms = 2s)
const long timeoutTime = 2000;

void setup() {
  Serial.begin(115200);

  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(bluePin, OUTPUT);
  
  analogWriteRange(resolution);
  analogWrite(redPin, 255);
  analogWrite(greenPin, 255);
  analogWrite(bluePin, 255);
  
  // Connect to Wi-Fi network with SSID and password
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(300);
    analogWrite(redPin, 0);
    analogWrite(greenPin, 0);
    analogWrite(bluePin, 0);
    delay(300);
    analogWrite(redPin, 0);
    analogWrite(greenPin, 255);
    analogWrite(bluePin, 255);
    Serial.print(".");
  }

  analogWrite(redPin, 0);
  analogWrite(greenPin, 0);
  analogWrite(bluePin, 0);
  
  // Print local IP address and start web server
  Serial.println("");
  Serial.println("WiFi connected.");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  server.begin();
}

void loop() {
    WiFiClient client;
    HTTPClient http;

    http.begin(client, serverPath.c_str());

    int httpResponseCode = http.GET();
      
    if (httpResponseCode>0) {
      Serial.print("HTTP Response code: ");
      Serial.println(httpResponseCode);
      String payload = http.getString();
      Serial.println(payload);

      JSONVar svdata = JSON.parse(payload);

      if (JSON.typeof(svdata) == "undefined") {
        Serial.println("Failed to decode server response");
      } else {
        JSONVar keys = svdata.keys();
        Serial.println("Server response decoded");

        redString = JSON.stringify(svdata[keys[0]]);
        greenString = JSON.stringify(svdata[keys[1]]);
        blueString = JSON.stringify(svdata[keys[2]]);

        redString.replace("\"", "");
        greenString.replace("\"", "");
        blueString.replace("\"", "");

        int r = redString.toInt();
        int g = greenString.toInt();
        int b = blueString.toInt();
        
        Serial.println(redString);
        Serial.println(greenString);
        Serial.println(blueString);
  
        analogWrite(redPin, r);
        analogWrite(greenPin, g);
        analogWrite(bluePin, b);
      }
    }
    else {
      Serial.print("Error code: ");
      Serial.println(httpResponseCode);
    }
    // Free resources
    http.end();
}
