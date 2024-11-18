/**************************************************************************
 * ESP Crystal lamp firmware

 * Copyright (c) 2022-2024 Dmytro Ostapenko. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

// Load Wi-Fi library
#include <ESP8266WiFi.h>
#include <math.h>

int intToColor(int i);

// Replace with your network credentials
const char* ssid     = "SSID";
const char* password = "PASSWORD";

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
int pos5 = 0;

// Variable to store the HTTP request
String header;

// Red, green, and blue pins for PWM control
const int redPin = 14;     // 13 corresponds to GPIO13
const int greenPin = 12;   // 12 corresponds to GPIO12
const int bluePin = 13;    // 14 corresponds to GPIO14

const int redPin2 = 5;     // 13 corresponds to GPIO13
const int greenPin2 = 4;   // 12 corresponds to GPIO12
const int bluePin2 = 0;    // 14 corresponds to GPIO14

int rr = 0;
int gg = 1024;
int bb = 512;

int isAnimating = 0;

// Setting PWM bit resolution
const int resolution = 256;

// Current time
unsigned long currentTime = millis();
// Previous time
unsigned long previousTime = 0;
// Define timeout time in milliseconds (example: 2000ms = 2s)
const long timeoutTime = 2000;

int gR = 255;
int gG = 0;
int gB = 33;

void setup() {
  Serial.begin(115200);

  // Normal version
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(bluePin, OUTPUT);
  pinMode(redPin2, OUTPUT);
  pinMode(greenPin2, OUTPUT);
  pinMode(bluePin2, OUTPUT);

  // configure LED PWM resolution/range and set pins to LOW
  analogWriteRange(resolution);
  analogWrite(redPin, 0);
  analogWrite(greenPin, 0);
  analogWrite(bluePin, 0);
  analogWrite(redPin2, 0);
  analogWrite(greenPin2, 0);
  analogWrite(bluePin2, 0);

  // Connect to Wi-Fi network with SSID and password
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    // delay(300);
    // analogWrite(greenPin, 255);
    // analogWrite(greenPin2, 255);
    delay(300);
    // analogWrite(greenPin, 0);
    // analogWrite(greenPin2, 0);
    Serial.print(".");
  }

  analogWrite(redPin, 255);
  analogWrite(greenPin, 0);
  analogWrite(bluePin, 33);
  analogWrite(redPin2, 255);
  analogWrite(greenPin2, 0);
  analogWrite(bluePin2, 33);

  // Print local IP address and start web server
  Serial.println("");
  Serial.println("WiFi connected.");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  server.begin();
  // server.enableCORS(true);
}

void loop() {
  WiFiClient client = server.available();   // Listen for incoming clients

  if (client) {                             // If a new client connects,
    currentTime = millis();
    previousTime = currentTime;
    Serial.println("New Client.");          // print a message out in the serial port
    String currentLine = "";                // make a String to hold incoming data from the client
    while (client.connected() && currentTime - previousTime <= timeoutTime) {            // loop while the client's connected
      currentTime = millis();
      if (client.available()) {             // if there's bytes to read from the client,
        char c = client.read();             // read a byte, then
        Serial.write(c);                    // print it out the serial monitor
        header += c;
        if (c == '\n') {                    // if the byte is a newline character
          // if the current line is blank, you got two newline characters in a row.
          // that's the end of the client HTTP request, so send a response:
          if (currentLine.length() == 0) {
            // HTTP headers always start with a response code (e.g. HTTP/1.1 200 OK)
            // and a content-type so the client knows what's coming, then a blank line:
            client.println("HTTP/1.1 200 OK");
            client.println("Content-type:text/html");
            client.println("Connection: close");
            client.println("Access-Control-Allow-Origin: *");
            client.println();

            // Display the HTML web page
            client.println("<!DOCTYPE html><html>");
            client.println("<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            client.println("<link rel=\"icon\" href=\"data:,\">");
            client.println("<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css\">");
            client.println("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jscolor/2.0.4/jscolor.min.js\"></script>");
            client.println("</head><body><div class=\"container\"><div class=\"row\"><h1>ESP Color Picker</h1></div>");
            client.println("<a class=\"btn btn-primary btn-lg\" href=\"#\" id=\"change_color\" role=\"button\">Change Color</a> ");
            client.println("<input class=\"jscolor {onFineChange:'update(this)'}\" id=\"rgb\"></div>");
            client.println("<script>function update(picker) {document.getElementById('rgb').innerHTML = Math.round(picker.rgb[0]) + ', ' +  Math.round(picker.rgb[1]) + ', ' + Math.round(picker.rgb[2]);");
            client.println("document.getElementById(\"change_color\").href=\"?r\" + Math.round(picker.rgb[0]) + \"g\" +  Math.round(picker.rgb[1]) + \"b\" + Math.round(picker.rgb[2]) + \"&\";}</script></body></html>");
            // The HTTP response ends with another blank line
            client.println();

            // Request sample: /?r201g32b255&
            // Red = 201 | Green = 32 | Blue = 255
            if(header.indexOf("GET /?r") >= 0) {
              pos1 = header.indexOf('r');
              pos2 = header.indexOf('g');
              pos3 = header.indexOf('b');
              pos4 = header.indexOf('a');
              pos5 = header.indexOf('&');
              redString = header.substring(pos1+1, pos2);
              greenString = header.substring(pos2+1, pos3);
              blueString = header.substring(pos3+1, pos4);

              isAnimating = header.substring(pos4+1, pos5).toInt();
            }
            // Break out of the while loop
            break;
          } else { // if you got a newline, then clear currentLine
            currentLine = "";
          }
        } else if (c != '\r') {  // if you got anything else but a carriage return character,
          currentLine += c;      // add it to the end of the currentLine
        }
      }
    }
    // Clear the header variable
    header = "";
    // Close the connection
    client.stop();
    Serial.println("Client disconnected.");
    Serial.println("");
  }

  if (isAnimating) {
    if (rr >= 1536) rr = 0;
    if (gg >= 1536) gg = 0;
    if (bb >= 1536) bb = 0;

    delay(20);

    // analogWrite(redPin, 255 - intToColor(rr));
    // analogWrite(greenPin, 255 - intToColor(gg));
    // analogWrite(bluePin, 255 - intToColor(bb));

    // Normal version
    analogWrite(redPin, intToColor(rr));
    analogWrite(greenPin, intToColor(gg));
    analogWrite(bluePin, intToColor(bb));
    analogWrite(redPin2, intToColor(rr));
    analogWrite(greenPin2, intToColor(gg));
    analogWrite(bluePin2, intToColor(bb));

    rr++;
    gg++;
    bb++;
  } else {
    // analogWrite(redPin, 255 - redString.toInt());
    // analogWrite(greenPin, 255 - greenString.toInt());
    // analogWrite(bluePin, 255 - blueString.toInt());

    // Normal version
    // if (gR != redString.toInt() || gG != greenString.toInt() || gB != blueString.toInt()) {
      analogWrite(redPin, redString.toInt());
      analogWrite(greenPin, greenString.toInt());
      analogWrite(bluePin, blueString.toInt());
      analogWrite(redPin2, redString.toInt());
      analogWrite(greenPin2, greenString.toInt());
      analogWrite(bluePin2, blueString.toInt());
    //}

    gR = redString.toInt();
    gG = greenString.toInt();
    gB = blueString.toInt();
  }

  delay(20);
}

int intToColor(int i) {
  if (i >= 0 && i <= 255) return i;
  if (i >= 256 && i <= 767) return 255;
  if (i >= 768 && i <= 1023) return 768 - abs(255 - i);
  else return 0;
}
