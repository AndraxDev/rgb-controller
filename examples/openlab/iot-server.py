# Copyright (c) 2024 Dmytro Ostapenko. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
OpenLab IoT server for controlling the lights. Compatible with Teslasoft RGB Controller.
(download at: https://play.google.com/store/apps/details?id=com.teslasoft.iot.rgbcontroller)

Version: 1.3
"""

import time
import flask
from flask_cors import CORS
import tuke_openlab
import threading
import base64
from tuke_openlab.lights import Color

# openlab = tuke_openlab.Controller(tuke_openlab.production_env())
openlab = tuke_openlab.Controller(tuke_openlab.simulation_env("")) # Add topic ID to simulate

app = flask.Flask(__name__)

CORS(app)


hw_lock = threading.Lock()
hw = 0


@app.route('/lights_on', methods=['GET'])
def lights_on():
    global hw
    with hw_lock:
        hw = 0 # Disable hardware animations
    openlab.lights.turn_on()
    return 'lights on'


@app.route('/lights_off', methods=['GET'])
def lights_off():
    global hw
    with hw_lock:
        hw = 0 # Disable hardware animations
    openlab.lights.turn_off()
    return 'lights off'


@app.route('/animation', methods=['GET'])
def run_anim():
    global hw
    animation = flask.request.args.get('data')

    if animation is None or animation == "":
        with hw_lock:
            hw = 0
        return 'No animation provided, stopping existing animation'

    animation_decoded = base64.b64decode(animation).decode('ascii')

    with open('temp', 'w') as f:
        f.write(animation_decoded)

        with hw_lock:
            hw = 3
            threading.Thread(target=run_custom_animation, args=('temp',), daemon=True).start()

    return 'animation started'


@app.route('/', methods=['GET'])
def hello():
    global hw
    try:
        red = flask.request.args.get('r', type=int)
        green = flask.request.args.get('g', type=int)
        blue = flask.request.args.get('b', type=int)

        hardware_anim = flask.request.args.get('a')

        with hw_lock:
            hw = int(hardware_anim)

        if red < 0 or red > 255:
            return 'Red value must be between 0 and 255'

        if green < 0 or green > 255:
            return 'Green value must be between 0 and 255'

        if blue < 0 or blue > 255:
            return 'Blue value must be between 0 and 255'

        color = Color(red, green, blue)

        openlab.lights.set_colors({i: color for i in range(1, 82)})

        return 'Colors updated'
    except Exception:
        return 'Hello, world!'


def colors():
    mod = 0
    while True:
        color_map = {}
        for k in range(1, 28):
            color = color_shift(mod + k*10)
            for b in range(0, 3):
                color_map[k + b*27] = color

        with hw_lock:
            if hw == 1:
                openlab.lights.set_colors(color_map)

        time.sleep(0.1)
        mod += 2

        if mod > 768:
            mod = 0


def color_shift(val):
    if val > 768:
        val = abs(val - 768)

    r, g, b = 0, 0, 0

    if val < 128:
        r = val*2
        b = 255
    elif val < 256:
        r = 255
        b = 255 - (val - 128)*2
    elif val < 384:
        r = 255
        g = (val - 256)*2
    elif val < 512:
        r = 255 - (val - 384)*2
        g = 255
    elif val < 640:
        g = 255
        b = (val - 512)*2
    else:
        g = 255 - (val - 640)*2
        b = 255

    return Color(r, g, b)


def police_disco():
    while True:
        with hw_lock:
            if hw == 2:
                openlab.lights.set_colors({i: Color(255, 0, 0) for i in range(1, 82)})
                time.sleep(0.1)
                openlab.lights.set_colors({i: Color(0, 0, 0) for i in range(1, 82)})
                time.sleep(0.1)
                openlab.lights.set_colors({i: Color(255, 0, 0) for i in range(1, 82)})
                time.sleep(0.1)
                openlab.lights.set_colors({i: Color(0, 0, 0) for i in range(1, 82)})
                time.sleep(0.1)
                openlab.lights.set_colors({i: Color(0, 0, 255) for i in range(1, 82)})
                time.sleep(0.1)
                openlab.lights.set_colors({i: Color(0, 0, 0) for i in range(1, 82)})
                time.sleep(0.1)
                openlab.lights.set_colors({i: Color(0, 0, 255) for i in range(1, 82)})
                time.sleep(0.1)
                openlab.lights.set_colors({i: Color(0, 0, 0) for i in range(1, 82)})


def read_anim_file(path):
    with open(path, 'r') as f:
        anim = []
        idx = -1
        for line in f:
            data = line.strip().split(' ')
            if data[0] == 'p':
                try:
                    if int(data[1]) < -1 or int(data[1]) == 0:
                        raise ValueError('Repeat count must be a positive integer, or -1 for infinite loop')

                    anim.append({'r': int(data[1]), 'p': []})
                    idx += 1
                except Exception:
                    raise ValueError('Invalid anim file')
            elif data[0] == 'c':
                try:
                    if data[idx] is None:
                        raise ValueError('Anim file is corrupted. Missing part information')
                except Exception:
                    raise ValueError('Anim file is corrupted. Missing part information')

                try:
                    dl = False
                    obj = {}
                    if data[1].__contains__("-"):
                        ids = data[1].split('-')
                        obj['from'] = int(ids[0])
                        obj['to'] = int(ids[1])
                    elif data[1].__contains__(","):
                        ids = data[1].split(',')
                        for l in range(0, len(ids)):
                            obj1 = {'from': int(ids[l]), 'to': int(ids[l]), 'r': int(data[2]), 'g': int(data[3]),
                                    'b': int(data[4]), 'type': 'color'}
                            anim[idx]['p'].append(obj1)
                        dl = True
                    else:
                        obj['from'] = int(data[1])
                        obj['to'] = int(data[1])

                    if int(data[2]) < 0 or int(data[2]) > 255:
                        raise ValueError('Red value must be between 0 and 255')

                    if int(data[3]) < 0 or int(data[3]) > 255:
                        raise ValueError('Green value must be between 0 and 255')

                    if int(data[4]) < 0 or int(data[4]) > 255:
                        raise ValueError('Blue value must be between 0 and 255')

                    if not dl:
                        obj['r'] = int(data[2])
                        obj['g'] = int(data[3])
                        obj['b'] = int(data[4])
                        obj['type'] = 'color'
                        anim[idx]['p'].append(obj)
                except Exception:
                    raise ValueError('Invalid anim file')
            elif data[0] == 't':
                try:
                    obj = {'type': 'pause', 'value': int(data[1])}
                    anim[idx]['p'].append(obj)
                except Exception:
                    raise ValueError('Invalid anim file')
            elif data[0] == 's':
                try:
                    obj = {'type': 'end'}
                    anim[idx]['p'].append(obj)
                except Exception:
                    raise ValueError('Invalid anim file')
            else:
                print("Warning: Unexpected junk found: " + data[0])

        print("DEBUG:")
        print(anim)

        return anim


def run_animation(animation, test = False):
    for frame in animation:
        with hw_lock:
            if hw != 3 and test is False:
                return
        if frame['r'] == -1:
            while True:
                with hw_lock:
                    if hw != 3 and test is False:
                        return
                run_animation_part(frame['p'], test)
        else:
            for _ in range(1, frame['r']+1):
                with hw_lock:
                    if hw != 3 and test is False:
                        return
                run_animation_part(frame['p'], test)


def run_animation_part(part, test = False):
    stack = {}
    global hw
    for command in part:
        if command['type'] == 'color':
            for j in range(command['from'], command['to'] + 1):
                stack[j] = Color(command['r'], command['g'], command['b'])
        elif command['type'] == 'pause':
            with hw_lock:
                if hw == 3 or test is True:
                    openlab.lights.set_colors(stack)
                else:
                    return
            stack = {}
            time.sleep(command['value']/1000)
        elif command['type'] == 'end':
            with hw_lock:
                if hw == 3 or test is True:
                    openlab.lights.set_colors(stack)
                else:
                    return
            stack = {}


def run_custom_animation(path, test = False):
    animation = read_anim_file(path)
    run_animation(animation, test)


def test_animation():
    run_custom_animation('anim-test3.txt', True)


if __name__ == '__main__':
    openlab.lights.turn_on()
    threading.Thread(target=colors, daemon=True).start()
    threading.Thread(target=police_disco, daemon=True).start()
    app.run(host='0.0.0.0')
    # test_animation()
