# RGB controller

A simple app to control your lighting IoT devices

### Screenshots

<div align = "center">
	<img src="https://teslasoft.org/rgb-controller/1.png" width="200"/>
	<img src="https://teslasoft.org/rgb-controller/2.png" width="200"/>
	<img src="https://teslasoft.org/rgb-controller/3.png" width="200"/>
</div>

### Tested devices:
- ESP8266
- OpenLab MQTT Lights at Technical University of Kosice

### Usage:
```
{PROTOCOL}://{HOSTNAME}:{PORT}{CMD}
```

### Command line:
- {_r} - Param for red color
- {_g} - Param for green color
- {_b} - Param for blue color
- {_a} - Enable hardware animation (optional param)

### Examples of command line:

```
/?script.py?red={_r}&green={_g}&blue={_b}&animation={_a}&
```

```
/?r{_r}&g{_g}b{_b}a{_a}&
```

> **Warning**  
> {_color} placeholders are case-sensitive
### Available values for each color

```
0-255
```

### Supported OS:
- Android 9 - Android 15
- Windows subsystem for Android

### How to connect?

1) Connect to the same Wi-Fi network with IoT device
2) Enter info about device and fill the command line
3) Use color picker or color fields to set colors

### How many colors are supported?

```
16,777,216
```

### Some examples

> See "examples" folder

### License

```
Copyright (c) 2022-2024 Dmytro Ostapenko. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
