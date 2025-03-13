/**************************************************************************
 * Copyright (c) 2022-2025 Dmytro Ostapenko. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

package com.teslasoft.iot.rgbcontroller.fragments.tabs

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teslasoft.iot.rgbcontroller.data.Color
import com.teslasoft.iot.rgbcontroller.R
import com.teslasoft.iot.rgbcontroller.util.StateManager
import com.teslasoft.iot.rgbcontroller.util.WindowManager
import org.teslasoft.core.api.network.RequestNetwork
import org.teslasoft.core.api.network.RequestNetworkController
import top.defaults.colorpicker.ColorPickerView
import java.lang.reflect.Type
import java.util.Base64
import java.util.EnumSet
import java.util.Locale
import kotlin.math.abs

class ControllerFragment : Fragment() {
    private lateinit var fieldRed: TextInputEditText
    private lateinit var fieldGreen: TextInputEditText
    private lateinit var fieldBlue: TextInputEditText
    private lateinit var btnPower: FloatingActionButton
    private lateinit var ui: ConstraintLayout
    private lateinit var loadingScreen: LinearLayout
    private lateinit var disabler: LinearLayout
    private lateinit var colorPicker: ColorPickerView
    private lateinit var animation: CheckBox
    private lateinit var fieldAnimation: TextInputEditText
    private lateinit var fieldAnimEndpoint: TextInputEditText
    private lateinit var fieldHwPredefined: TextInputEditText
    private lateinit var btnSetPredefined: MaterialButton
    private lateinit var btnOpenFile: MaterialButton
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var hardwareAnimation: CheckBox
    private lateinit var context: Context
    private lateinit var activityTitle: TextView
    private lateinit var noSelected: ConstraintLayout
    private lateinit var content: ScrollView
    private lateinit var stepNote: TextView

    private lateinit var deviceId: String
    private var hostname: String? = null
    private var port: String? = null
    private var protocol: String? = null
    private var cmd: String? = null
    private var getter: String? = null
    private var animationCmd = "/animation"

    private var errorRed = false
    private var errorGreen = false
    private var errorBlue = false
    private var formError = false
    private var isLoading = false
    private var isAnimating = false
    private var isPoweredOn = true

    private lateinit var api: RequestNetwork
    private lateinit var syncProvider: RequestNetwork

    private val syncListener = object : RequestNetwork.RequestListener {
        override fun onResponse(tag: String, response: String) {
            val gson = Gson()
            try {
                val type: Type = TypeToken.get(Color::class.java).type
                val sr: Color = gson.fromJson(response, type)
                val red = sr.red
                val green = sr.green
                val blue = sr.blue

                fieldRed.setText(red)
                fieldGreen.setText(green)
                fieldBlue.setText(blue)

                val settings = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
                settings.edit().apply {
                    putString("red", red)
                    putString("green", green)
                    putString("blue", blue)
                    apply()
                }
            } catch (_: Exception) {}
        }

        override fun onErrorResponse(tag: String, message: String) { /* ignored */ }
    }

    private val animationRequestListener = object : RequestNetwork.RequestListener {
        override fun onResponse(tag: String, response: String) { /* ignored */ }

        override fun onErrorResponse(tag: String, message: String) {
            MaterialAlertDialogBuilder(context)
                    .setTitle("Error")
                    .setMessage("An error occurred while trying to send the animation command to the device: $message")
                    .setPositiveButton("OK", null)
                    .show()
        }
    }

    private val apiListener = object : RequestNetwork.RequestListener {
        override fun onResponse(tag: String, response: String) {
            loadingScreen.visibility = View.GONE
            isLoading = false
        }

        override fun onErrorResponse(tag: String, message: String) {
            loadingScreen.visibility = View.GONE
            isLoading = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = requireActivity()
        deviceId = StateManager.Companion.getSelectedDeviceId() ?: "null"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_color_picker, container, false)
    }

    private val mGetContent: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            readFileFromUri(uri)
        }
    }

    private fun readFileFromUri(uri: Uri) {
        try {
            requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                val size = inputStream.available()
                val bytes = ByteArray(size)
                inputStream.read(bytes)
                val content = String(bytes)

                if (validateFile(content)) {
                    fieldAnimation.setText(content)
                }
            }
        } catch (_: Exception) {
            MaterialAlertDialogBuilder(context)
                    .setTitle("Error")
                    .setMessage("An error occurred while trying to read the file.")
                    .setPositiveButton("OK", null)
                    .show()
        }
    }

    private fun validateFile(content: String): Boolean {
        val lines = content.split("\n")
        lines.forEachIndexed { lineNumber, line ->
            val data = line.split(" ")
            when (data[0]) {
                "p" -> {
                    try {
                        val intValue = data[1].toInt()
                        if (intValue < -1 || intValue == 0) {
                            showError("Syntax error: Invalid value at ${lineNumber + 1}:3: Expected a positive integer or -1, but got ${data[1]}.")
                            return false
                        }
                    } catch (e: Exception) {
                        showError("Validation error: Static check failed at ${lineNumber + 1}. Exception: ${e.message}")
                        return false
                    }
                }
                "c" -> {
                    try {
                        data.drop(2).forEachIndexed { i, value ->
                            val intValue = value.toInt()
                            if (intValue < 0 || intValue > 255) {
                                showError("Syntax error: Invalid value at ${lineNumber + 1}:${2 + value.length * 3}: Expected a value between 0 and 255, but got $value.")
                                return false
                            }
                        }
                    } catch (e: Exception) {
                        showError("Validation error: Static check failed at ${lineNumber + 1}. Exception: ${e.message}")
                        return false
                    }
                }
                "t" -> {
                    try {
                        val intValue = data[1].toInt()
                        if (intValue <= 0) {
                            showError("Syntax error: Invalid value at ${lineNumber + 1}:3: Expected a positive integer, but got ${data[1]}.")
                            return false
                        }
                    } catch (e: Exception) {
                        showError("Validation error: Static check failed at ${lineNumber + 1}. Exception: ${e.message}")
                        return false
                    }
                }
                "s" -> {}
                else -> {
                    showError("Syntax error: Unexpected junk '${data[0]}' at ${lineNumber + 1}:1. Expected 'p', 'c', 't' or 's'.")
                    return false
                }
            }
        }
        return true
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(context)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
    }

    private fun launchFileIntent() {
        mGetContent.launch("*/*")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fieldRed = view.findViewById(R.id.field_red)
        fieldGreen = view.findViewById(R.id.field_green)
        fieldBlue = view.findViewById(R.id.field_blue)
        btnPower = view.findViewById(R.id.btn_power)
        activityTitle = view.findViewById(R.id.activity_title)
        colorPicker = view.findViewById(R.id.color_picker)
        animation = view.findViewById(R.id.animation)
        hardwareAnimation = view.findViewById(R.id.hardware_animation)
        ui = view.findViewById(R.id.ui)
        loadingScreen = view.findViewById(R.id.loading_screen)
        disabler = view.findViewById(R.id.disabler)
        fieldAnimation = view.findViewById(R.id.field_animation)
        fieldAnimEndpoint = view.findViewById(R.id.field_anim_endpoint)
        fieldHwPredefined = view.findViewById(R.id.field_hw_predefined)
        btnSetPredefined = view.findViewById(R.id.btn_set_predefined)
        btnOpenFile = view.findViewById(R.id.btn_load_file)
        btnStart = view.findViewById(R.id.btn_start)
        btnStop = view.findViewById(R.id.btn_stop)
        noSelected = view.findViewById(R.id.no_selection)
        content = view.findViewById(R.id.content)
        stepNote = view.findViewById(R.id.step_note)

        ui.setBackgroundColor(SurfaceColors.SURFACE_0.getColor(requireActivity()))
        stepNote.backgroundTintList = ColorStateList.valueOf(SurfaceColors.SURFACE_2.getColor(requireActivity()))

        disabler.setOnClickListener { }
        loadingScreen.visibility = View.GONE
        syncProvider = RequestNetwork(requireActivity())

        enableEdgeToEdge(view)

        fieldAnimEndpoint.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                animationCmd = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        animationCmd = fieldAnimEndpoint.text.toString()

        btnOpenFile.setOnClickListener { launchFileIntent() }
        btnSetPredefined.setOnClickListener {
            api.startRequestNetwork(
                    RequestNetworkController.Companion.GET,
                    "$protocol://$hostname:$port${getCmd(cmd, null, null, null ,fieldHwPredefined.text.toString())}",
                    "A",
                    apiListener
            )
        }

        btnStart.setOnClickListener {
            val base64animation = Base64.getEncoder().encodeToString(fieldAnimation.text.toString().toByteArray())
            api.startRequestNetwork(
                    RequestNetworkController.Companion.GET,
                    "$protocol://$hostname:$port$animationCmd$base64animation",
                    "A",
                    animationRequestListener
            )
        }

        btnStop.setOnClickListener {
            api.startRequestNetwork(
                    RequestNetworkController.Companion.GET,
                    "$protocol://$hostname:$port$animationCmd",
                    "A",
                    animationRequestListener
            )
        }

        preInit()
    }

    fun preInit() {
        deviceId = StateManager.Companion.getSelectedDeviceId() ?: "null"
        if (deviceId.toString() == "null" || deviceId.toString() == "" || deviceId.toString() == "[null]") {
            noSelected.visibility = View.VISIBLE
            content.visibility = View.GONE
        } else {
            try {
                noSelected.visibility = View.GONE
                content.visibility = View.VISIBLE
                val settings: SharedPreferences = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
                hostname = settings.getString("hostname", null)
                port = settings.getString("port", null)
                protocol = settings.getString("protocol", null)
                cmd = settings.getString("cmd", null)
                getter = settings.getString("getter", null)
                fieldRed.setText(settings.getString("red", "0"))
                fieldGreen.setText(settings.getString("green", "0"))
                fieldBlue.setText(settings.getString("blue", "0"))
                activityTitle.text = settings.getString("name", null)

                initialize()
            } catch (_: Exception) {
                noSelected.visibility = View.VISIBLE
                content.visibility = View.GONE
            }
        }
    }

    private fun initialize() {
        if (hostname == null || port == null || protocol == null) {
            ui.visibility = View.GONE
            btnPower.visibility = View.GONE
        } else {
            postInit()
        }
    }

    private fun initSettings() {
        try {
            val settings: SharedPreferences = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
            val xr = settings.getString("red", null)
            val xg = settings.getString("green", null)
            val xb = settings.getString("blue", null)

            val color = "FF%s%s%s".format(
                    Integer.parseInt(xr ?: "").toString(16).padStart(2, '0'),
                    Integer.parseInt(xg ?: "").toString(16).padStart(2, '0'),
                    Integer.parseInt(xb ?: "").toString(16).padStart(2, '0')
            )

            val c = color.toLong(16)
            colorPicker.setInitialColor(c.toInt())

            syncProvider.startRequestNetwork(
                    RequestNetworkController.Companion.GET,
                    "$protocol://$hostname:$port$getter",
                    "A",
                    syncListener
            )
        } catch (_: Exception) {
            colorPicker.setInitialColor(0xFF00FFFF.toInt())
        }
    }

    private fun postInit() {
        initSettings()

        fieldRed.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateField(s, "red") { errorRed = it }
                validateForm()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fieldGreen.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateField(s, "green") { errorGreen = it }
                validateForm()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fieldBlue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateField(s, "blue") { errorBlue = it }
                validateForm()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        initLogic()
    }

    private fun validateField(s: CharSequence?, fieldName: String, errorSetter: (Boolean) -> Unit) {
        val field = when (fieldName) {
            "red" -> fieldRed
            "green" -> fieldGreen
            else -> fieldBlue
        }
        when {
            s.isNullOrEmpty() -> {
                errorSetter(true)
                field.error = getString(R.string.required_field)
            }
            else -> {
                field.error = parseColorValue(s.toString().trim())
                errorSetter(field.error != null)
            }
        }
    }

    private fun parseColorValue(value: String): String? {
        return try {
            val intValue = value.toInt()
            when {
                intValue !in 0..255 -> getString(R.string.invalid_value)
                else -> null
            }
        } catch (_: Exception) {
            getString(R.string.invalid_value)
        }
    }

    private fun initLogic() {
        colorPicker.subscribe { color, fromUser, _ ->
            if (!isLoading && !formError && !isAnimating && fromUser) {
                val r = Integer.toHexString(color).substring(2, 4)
                val g = Integer.toHexString(color).substring(4, 6)
                val b = Integer.toHexString(color).substring(6, 8)

                fieldRed.setText(Integer.valueOf(r, 16).toString())
                fieldGreen.setText(Integer.valueOf(g, 16).toString())
                fieldBlue.setText(Integer.valueOf(b, 16).toString())

                val settings: SharedPreferences = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
                settings.edit().apply {
                    putString("red", fieldRed.text.toString().trim())
                    putString("green", fieldGreen.text.toString().trim())
                    putString("blue", fieldBlue.text.toString().trim())
                    apply()
                }

                if (isPoweredOn) {
                    api.startRequestNetwork(
                            RequestNetworkController.Companion.GET,
                            "$protocol://$hostname:$port${getCmd(cmd, null, null, null, null)}",
                            "A",
                            apiListener
                    )
                    isLoading = true
                }
            }
        }

        animation.setOnCheckedChangeListener { _, isChecked -> isAnimating = isChecked }

        hardwareAnimation.setOnCheckedChangeListener { _, isChecked ->
            api.startRequestNetwork(
                    RequestNetworkController.Companion.GET,
                    "$protocol://$hostname:$port${getCmd(cmd, null, null, null, null)}",
                    "A",
                    apiListener
            )
            isLoading = true

            if (isChecked) {
                disabler.visibility = View.VISIBLE
                disabler.alpha = 0f
                disabler.animate().alpha(0.5f).setDuration(200).withEndAction { disabler.alpha = 0.5f }
            } else {
                disabler.alpha = 0.5f
                disabler.animate().alpha(0f).setDuration(200).withEndAction { disabler.visibility = View.GONE }
            }
        }

        animate(0, 1024, 512)

        api = RequestNetwork(requireActivity())

        try {
            val settings: SharedPreferences = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
            if (settings.getString("enabled", null) == "true") {
                enableLeds()
            } else {
                disableLeds()
            }
        } catch (_: Exception) {
            enableLeds()
        }

        btnPower.setOnClickListener { toggle() }
    }

    private fun validateForm() {
        if (!isAnimating) {
            formError = errorRed || errorGreen || errorBlue
            if (!formError) {
                try {
                    val color = String.Companion.format(
                            Locale.US,
                            "FF%02X%02X%02X",
                            Integer.parseInt(fieldRed.text.toString().trim()),
                            Integer.parseInt(fieldGreen.text.toString().trim()),
                            Integer.parseInt(fieldBlue.text.toString().trim())
                    )
                    val c = color.toLong(16)
                    colorPicker.setInitialColor(c.toInt())

                    api.startRequestNetwork(
                            RequestNetworkController.Companion.GET,
                            "$protocol://$hostname:$port${getCmd(cmd, null, null, null, null)}",
                            "A",
                            apiListener
                    )
                    isLoading = true
                } catch (_: Exception) {}
            }
        }
    }

    private fun animate(r: Int, g: Int, b: Int) {
        var tR = r
        var tG = g
        var tB = b

        if (tR >= 1536) tR = 0
        if (tG >= 1536) tG = 0
        if (tB >= 1536) tB = 0

        Handler(Looper.getMainLooper()).postDelayed({
            animate(tR + 4, tG + 4, tB + 4)
            if (isAnimating && isPoweredOn) {
                updateFieldsAndColorPicker(tR, tG, tB)
            }
        }, 50)
    }

    private fun updateFieldsAndColorPicker(tR: Int, tG: Int, tB: Int) {
        fieldRed.setText(intToColor(tR).toString())
        fieldGreen.setText(intToColor(tG).toString())
        fieldBlue.setText(intToColor(tB).toString())

        api.startRequestNetwork(
                RequestNetworkController.Companion.GET,
                "$protocol://$hostname:$port${getCmd(cmd, null, null, null, null)}",
                "A",
                apiListener
        )
        isLoading = true

        val color = String.Companion.format(
                Locale.US,
                "FF%02X%02X%02X",
                Integer.parseInt(fieldRed.text.toString().trim()),
                Integer.parseInt(fieldGreen.text.toString().trim()),
                Integer.parseInt(fieldBlue.text.toString().trim())
        )
        val xc = color.toLong(16)
        colorPicker.setInitialColor(xc.toInt())

        val settings: SharedPreferences = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
        settings.edit().apply {
            putString("red", fieldRed.text.toString().trim())
            putString("green", fieldGreen.text.toString().trim())
            putString("blue", fieldBlue.text.toString().trim())
            apply()
        }
    }

    private fun getCmd(cmd: String?, r: String?, g: String?, b: String?, a: String?): String {
        val rOut = cmd?.replace("{_r}", r ?: fieldRed.text.toString().trim())
        val gOut = rOut?.replace("{_g}", g ?: fieldGreen.text.toString().trim())
        val ha = a ?: if (hardwareAnimation.isChecked) "1" else "0"

        return gOut?.replace("{_b}", b ?: fieldBlue.text.toString().trim())?.replace("{_a}", ha) ?: ""
    }

    private fun intToColor(i: Int): Int {
        return when (i) {
            in 0..255 -> i
            in 256..767 -> 255
            in 768..1023 -> 768 - abs(255 - i)
            else -> 0
        }
    }

    private fun toggle() {
        if (isPoweredOn) {
            disableLeds()
            if (!formError) {
                api.startRequestNetwork(
                    RequestNetworkController.Companion.GET,
                    "$protocol://$hostname:$port${getCmd(cmd, "0", "0", "0", null)}",
                    "A",
                    apiListener
                )
                isLoading = true
            }
            updateSettings("false")
        } else {
            enableLeds()
            if (!formError) {
                api.startRequestNetwork(
                    RequestNetworkController.Companion.GET,
                    "$protocol://$hostname:$port${getCmd(cmd, null, null, null, null)}",
                    "A",
                    apiListener
                )
                isLoading = true
            }
            updateSettings("true")
        }
    }

    private fun enableLeds() {
        isPoweredOn = true
        btnPower.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.light_green)

        fieldRed.isEnabled = true
        fieldGreen.isEnabled = true
        fieldBlue.isEnabled = true

        animation.isEnabled = true
        colorPicker.isEnabled = true
        hardwareAnimation.isEnabled = true
        hardwareAnimation.alpha = 1.0f

        disabler.visibility = View.VISIBLE
        disabler.alpha = 0.5f
        disabler.animate().alpha(0f).setDuration(200).withEndAction { disabler.visibility = View.GONE }
    }

    private fun disableLeds() {
        isPoweredOn = false
        btnPower.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.light_red)

        fieldRed.isEnabled = false
        fieldGreen.isEnabled = false
        fieldBlue.isEnabled = false

        animation.isEnabled = false
        hardwareAnimation.isEnabled = false
        colorPicker.isEnabled = false

        hardwareAnimation.isChecked = false
        hardwareAnimation.alpha = 0.5f

        disabler.visibility = View.VISIBLE
        disabler.alpha = 0f
        disabler.animate().alpha(0.5f).setDuration(200).withEndAction { disabler.alpha = 0.5f }
    }

    private fun updateSettings(enabled: String) {
        val settings: SharedPreferences = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
        settings.edit().apply {
            putString("enabled", enabled)
            apply()
        }
    }

    private fun enableEdgeToEdge(view: View) {
        WindowManager.Companion.adjustPaddings(requireActivity(), view, R.id.ab, EnumSet.of(WindowManager.Companion.Flags.STATUS_BAR, WindowManager.Companion.Flags.IGNORE_PADDINGS))
        WindowManager.Companion.adjustPaddings(requireActivity(), view, R.id.no_selection, EnumSet.of(WindowManager.Companion.Flags.STATUS_BAR, WindowManager.Companion.Flags.IGNORE_PADDINGS))
    }
}
