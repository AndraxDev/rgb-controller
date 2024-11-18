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
"""

import time
import flask
from flask_cors import CORS
import tuke_openlab
import threading
from tuke_openlab.lights import Color

openlab = tuke_openlab.Controller(tuke_openlab.production_env())
# openlab = tuke_openlab.Controller(tuke_openlab.simulation_env("")) # Add topic ID to simulate

app = flask.Flask(__name__)

CORS(app)


hw_lock = threading.Lock()
hw = False


@app.route('/', methods=['GET'])
def hello():
    global hw
    try:
        red = flask.request.args.get('r', type=int)
        green = flask.request.args.get('g', type=int)
        blue = flask.request.args.get('b', type=int)

        hardware_anim = flask.request.args.get('a')

        with hw_lock:
            if hardware_anim == '1':
                hw = True
            elif hardware_anim == '0':
                hw = False

        if red < 0 or red > 255:
            return 'Red value must be between 0 and 255'

        if green < 0 or green > 255:
            return 'Green value must be between 0 and 255'

        if blue < 0 or blue > 255:
            return 'Blue value must be between 0 and 255'

        color = Color(red, green, blue)

        openlab.lights.set_colors({i: color for i in range(1, 82)})

        return 'Colors updated'
    except:
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
            if hw:
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


if __name__ == '__main__':
    threading.Thread(target=colors, daemon=True).start()
    app.run(host='0.0.0.0')
