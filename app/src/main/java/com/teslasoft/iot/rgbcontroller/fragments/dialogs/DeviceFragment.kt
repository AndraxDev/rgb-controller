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

package com.teslasoft.iot.rgbcontroller.fragments.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.teslasoft.iot.rgbcontroller.R

class DeviceFragment : DialogFragment() {

    private var listener: DeviceChangedListener? = null
    private var alistener: DeviceAddedListener? = null

    private var context: Context? = null

    private lateinit var fieldProtocol: TextInputEditText
    private lateinit var fieldHostname: TextInputEditText
    private lateinit var fieldPort: TextInputEditText
    private lateinit var fieldCmd: TextInputEditText
    private lateinit var fieldName: TextInputEditText
    private lateinit var fieldGetter: TextInputEditText

    private var errorProtocol = true
    private var errorHostname = true
    private var errorPort = true
    private var errorCmd = true
    private var errorName = true

    private lateinit var builder: AlertDialog.Builder

    companion object {
        fun newInstance(
                deviceId: String, protocol: String, hostname: String, port: String,
                command: String, name: String, getter: String
        ): DeviceFragment {
            val deviceFragment = DeviceFragment()
            val args = Bundle()
            args.putString("device_id", deviceId)
            args.putString("protocol", protocol)
            args.putString("hostname", hostname)
            args.putString("port", port)
            args.putString("command", command)
            args.putString("name", name)
            args.putString("getter", getter)
            deviceFragment.arguments = args
            return deviceFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = activity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_device_info, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        builder = MaterialAlertDialogBuilder(requireActivity())
        val view = layoutInflater.inflate(R.layout.fragment_device_info, null)

        initUI(view)

        fieldProtocol.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                errorProtocol = if (s.toString().trim().isEmpty()) {
                    fieldProtocol.error = getString(R.string.required_field)
                    true
                } else {
                    fieldProtocol.error = null
                    false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fieldHostname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                errorHostname = if (s.toString().trim().isEmpty()) {
                    fieldHostname.error = getString(R.string.required_field)
                    true
                } else {
                    fieldHostname.error = null
                    false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fieldPort.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                errorPort = if (s.toString().trim().isEmpty()) {
                    fieldPort.error = getString(R.string.required_field)
                    true
                } else {
                    try {
                        val i = s.toString().trim().toInt()
                        if (i > 65536 || i < 0) {
                            fieldPort.error = getString(R.string.invalid_value)
                            true
                        } else {
                            fieldPort.error = null
                            false
                        }
                    } catch (_: Exception) {
                        fieldPort.error = getString(R.string.invalid_value)
                        true
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fieldCmd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                errorCmd = if (s.toString().trim().isEmpty()) {
                    fieldCmd.error = getString(R.string.required_field)
                    true
                } else {
                    if (s.toString().contains("{_r}") && s.toString().contains("{_g}") && s.toString().contains("{_b}")) {
                        fieldCmd.error = null
                        false
                    } else {
                        fieldCmd.error = getString(R.string.cmd_missing_params)
                        true
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fieldName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                errorName = if (s.toString().trim().isEmpty()) {
                    fieldName.error = getString(R.string.required_field)
                    true
                } else {
                    fieldName.error = null
                    false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fieldCmd.setOnTouchListener { v, event ->
            v.performClick()
            if (event.action == MotionEvent.ACTION_UP && event.rawX >= (fieldCmd.right - fieldCmd.compoundDrawables[2].bounds.width())) {
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(resources.getString(R.string.title_help))
                        .setMessage(HtmlCompat.fromHtml(resources.getString(R.string.text_help), HtmlCompat.FROM_HTML_MODE_LEGACY))
                        .setPositiveButton(resources.getString(R.string.btn_close)) { _, _ -> }
                        .show()
                true
            } else {
                false
            }
        }

        builder.setView(view)
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ -> validateForm() }
                .setNegativeButton("Cancel") { _, _ ->
                    alistener?.onCancelled()
                }

        return builder.create()
    }

    private fun initUI(view: View) {
        fieldProtocol = view.findViewById(R.id.field_protocol)
        fieldHostname = view.findViewById(R.id.field_hostname)
        fieldPort = view.findViewById(R.id.field_port)
        fieldCmd = view.findViewById(R.id.field_cmd)
        fieldName = view.findViewById(R.id.field_name)
        fieldGetter = view.findViewById(R.id.field_getter)

        val dialogTitle: TextView = view.findViewById(R.id.dialog_title)

        arguments?.let {
            if (it.getString("hostname").isNullOrEmpty()) {
                dialogTitle.setText(R.string.text_add_device)
            } else {
                dialogTitle.setText(R.string.text_edit_device_info)
            }
        } ?: run {
            dialogTitle.setText(R.string.text_add_device)
        }

        val settings = requireActivity().getSharedPreferences("settings_" + arguments?.getString("device_id"), Context.MODE_PRIVATE)

        fieldProtocol.setText(settings.getString("protocol", ""))
        fieldHostname.setText(settings.getString("hostname", ""))
        fieldPort.setText(settings.getString("port", ""))
        fieldCmd.setText(settings.getString("cmd", ""))
        fieldName.setText(settings.getString("name", ""))
        fieldGetter.setText(settings.getString("getter", ""))

        errorProtocol = fieldProtocol.text.toString().trim().isEmpty()
        errorHostname = fieldHostname.text.toString().trim().isEmpty()
        errorPort = fieldPort.text.toString().trim().isEmpty()
        errorCmd = fieldCmd.text.toString().trim().isEmpty()
        errorName = fieldName.text.toString().trim().isEmpty()
    }

    fun validateForm() {
        arguments?.let {
            if (errorProtocol || errorHostname || errorPort || errorCmd || errorName) {
                Toast.makeText(context, "Form error", Toast.LENGTH_SHORT).show()
                val deviceFragment = newInstance(
                        it.getString("device_id")!!,
                        fieldProtocol.text.toString(),
                        fieldHostname.text.toString(),
                        fieldPort.text.toString(),
                        fieldCmd.text.toString(),
                        fieldName.text.toString(),
                        fieldGetter.text.toString()
                )
                deviceFragment.show(requireActivity().supportFragmentManager.beginTransaction(), "DeviceDialog")
            } else {
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                val settings = requireActivity().getSharedPreferences("settings_${it.getString("device_id")}", Context.MODE_PRIVATE)
                val editor = settings.edit()
                editor.putString("hostname", fieldHostname.text.toString())
                editor.putString("port", fieldPort.text.toString())
                editor.putString("protocol", fieldProtocol.text.toString())
                editor.putString("cmd", fieldCmd.text.toString())
                editor.putString("getter", fieldGetter.text.toString())
                editor.putString("red", "0")
                editor.putString("green", "0")
                editor.putString("blue", "0")
                editor.putString("enabled", "false")
                editor.putString("name", fieldName.text.toString())
                editor.apply()

                try {
                    listener?.deviceInfoChanged(fieldName.text.toString())
                } catch (_: Exception) {}

                try {
                    alistener?.onSuccess()
                } catch (_: Exception) {}
            }
        } ?: run {
            try {
                alistener?.onFailed()
            } catch (_: Exception) {}
        }
    }

    fun interface DeviceChangedListener {
        fun deviceInfoChanged(devName: String)
    }

    interface DeviceAddedListener {
        fun onSuccess()
        fun onFailed()
        fun onCancelled() {}
    }

    fun setDeviceAddedListener(listener: DeviceAddedListener) {
        this.alistener = listener
    }

    fun setDeviceChangedListener(listener: DeviceChangedListener) {
        this.listener = listener
    }
}
