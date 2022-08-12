# RGB controller

A simple app to control your lighting IoT devices

### Screenshots

<img src="https://teslasoft.org/rgb-controller/1.png" width="300"/> <img src="https://teslasoft.org/rgb-controller/2.png" width="300"/> <img src="https://teslasoft.org/rgb-controller/3.png" width="300"/> 

### Tested devices:
- ESP8266

### Usage:
```
{PROTOCOL}://{HOSTNAME}:{PORT}{CMD}
```

### Command line:
- {_r} - Param for red color
- {_g} - Param for green color
- {_b} - Param for blue color

### Examples of command line:

```
/?script.py?red={_r}&green={_g}&blue={_b}
```

```
/?r{_r}&g{_g}b{_b}&
```

> **Warning**  
> {_color} placeholders are case-sensitive

### Available values for each color

```
0-255
```

### Supported OS:
- Android 9 - Android 12
- Android subsystem for Windows

### How to connect?

1) Connect to the same wifi network with IoT device
2) Enter info about device and fill the command line
3) Use color picker or color fields to set colors

### How many colors are supported?

```
16,777,216
```