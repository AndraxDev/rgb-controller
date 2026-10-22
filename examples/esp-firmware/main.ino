/**************************************************************************
 * ESP Crystal lamp firmware version 2.0

 * Copyright (c) 2022-2026 Dmytro Ostapenko. All rights reserved.
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
const char* ssid     = "<SSID>";
const char* password = "<PASSWORD>";

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

  // configure LED PWM resolution/range and set pins to LOW
  analogWriteRange(resolution);
  analogWrite(redPin, 0);
  analogWrite(greenPin, 0);
  analogWrite(bluePin, 0);

  // Connect to Wi-Fi network with SSID and password
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(300);
    analogWrite(redPin, 0xFF);
    analogWrite(greenPin, 0xFF);
    analogWrite(bluePin, 0xFF);
    delay(300);
    analogWrite(redPin, 0);
    analogWrite(greenPin, 0);
    analogWrite(bluePin, 0);
    Serial.print(".");
  }

  analogWrite(redPin, 0xFF);
  analogWrite(greenPin, 0xFF);
  analogWrite(bluePin, 0xFF);

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
            client.println("<!DOCTYPE html>");
            client.println("<html lang='en'>");
            client.println("<head>");
            client.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            client.println("<script src='https://cdnjs.cloudflare.com/ajax/libs/jscolor/2.0.4/jscolor.min.js'></script>");
            client.println("<title>Crystal Lamp</title>");
            client.println("<style>button {flex-grow:1;padding:12px 16px;outline:0;font-size:16px;border:none;border-radius:999pc;background-color:#424242;color:#fff;transition:.3s;cursor:pointer;-webkit-tap-highlight-color:transparent;}button:hover{background-color:#fff;color:#000000;transition:.3s;}a{text-decoration:none;transition:.3s;color:#3492eb;}a:hover{color:#ffffff;transition:.3s;}</style>");
            client.println("</head>");
            client.println("<body style='font-family:sans-serif;padding:0;margin:0;overflow:hidden;user-select:none;color-scheme:dark;background-color:#121212;'>");
            client.println("<div style='display:flex;flex-direction:column;align-items:center;justify-content:center;height:100lvh;max-width:360px;margin:auto;overflow:hidden;'>");
            client.println("<div style='background-color:#323232;padding:20px;width:calc(100% - 48px);display:flex;flex-direction:column;align-items:center;justify-content:center;border-radius:36px;'>");
            client.println("<h1 style='font-size:24px;width:100%;text-align:center;margin:0;color:#cecece;'>Crystal Lamp</h1>");
            client.println("<div style='width:100%;padding-top:20px;'>");
            client.println("<input style='width:calc(100% - 32px);padding:12px 16px;font-size:16px;border:none;outline:none;border-radius:999pc;' class='jscolor {onFineChange:`update(this)`}' id='rgb'>");
            client.println("</div>");
            client.println("<div style='width:100%;padding-top:20px;display:flex;flex-direction:row;align-items:center;justify-content:space-between;gap:20px;'>");
            client.println("<button onclick='javascript:setColor(onUrl)'>On</button>");
            client.println("<button onclick='javascript:setColor(offUrl)'>Off</button>");
            client.println("<button onclick='javascript:setColor(animationUrl)'>Animate</button>");
            client.println("</div>");
            client.println("<p style='margin-bottom:0;margin-top:20px;font-size:14px;color:#cecece;'>Firmware version: 2.0</p>");
            client.println("<p style='margin-bottom:0;margin-top:6px;font-size:14px;color:#cecece;'>(C) 2022-2026 <a href='https://andrax.dev/' target='_blank'>AndraxDev</a></p>");
            client.println("</div>");
            client.println("</div>");
            client.println("<script>");
            client.println("const hostname = window.location.hostname;");
            client.println("const port = window.location.port;");
            client.println("const baseUrl = 'http://' + hostname + ':' + port;");
            client.println("const offUrl = baseUrl + '?r0g0b0a0&';");
            client.println("const onUrl = baseUrl + '?r255g255b255a0&';");
            client.println("const animationUrl = baseUrl + '?r255g0b0a1&';");
            client.println("function update(picker) {");
            client.println("document.getElementById('rgb').innerHTML = Math.round(picker.rgb[0]) + ', ' +  Math.round(picker.rgb[1]) + ', ' + Math.round(picker.rgb[2]);");
            client.println("let l = '?r' + Math.round(picker.rgb[0]) + 'g' +  Math.round(picker.rgb[1]) + 'b' + Math.round(picker.rgb[2]) + '&';");
            client.println("setColor(baseUrl + l);");
            client.println("}");
            client.println("function setColor(url) {");
            client.println("fetch(url).then(() => {}).catch(() => {});");
            client.println("}");
            client.println("</script>");
            client.println("</body>");
            client.println("</html>");

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

    analogWrite(redPin, 0xFF - intToColor(rr));
    analogWrite(greenPin, 0xFF - intToColor(gg));
    analogWrite(bluePin, 0xFF - intToColor(bb));

    rr++;
    gg++;
    bb++;
  } else {
    gR = redString.toInt();
    gG = greenString.toInt();
    gB = blueString.toInt();

    analogWrite(redPin, 0xFF - gR);
    analogWrite(greenPin, 0xFF - gG);
    analogWrite(bluePin, 0xFF - gB);
  }

  delay(20);
}

int intToColor(int i) {
  if (i >= 0 && i <= 255) return i;
  if (i >= 256 && i <= 767) return 255;
  if (i >= 768 && i <= 1023) return 768 - abs(255 - i);
  else return 0;
}
