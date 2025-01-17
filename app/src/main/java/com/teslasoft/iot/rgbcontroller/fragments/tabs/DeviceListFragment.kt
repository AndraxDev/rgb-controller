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

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.teslasoft.iot.rgbcontroller.adapters.DeviceListAdapter
import com.teslasoft.iot.rgbcontroller.MainActivity
import com.teslasoft.iot.rgbcontroller.R
import com.teslasoft.iot.rgbcontroller.util.StateManager
import com.teslasoft.iot.rgbcontroller.util.WindowManager
import com.teslasoft.iot.rgbcontroller.fragments.dialogs.DeviceFragment
import java.util.EnumSet

class DeviceListFragment : Fragment(), DeviceListAdapter.OnDeviceActionsClicked {
    private lateinit var empty: LinearLayout
    private lateinit var listDev: RecyclerView
    private lateinit var btnAdd: ExtendedFloatingActionButton
    private lateinit var ui: View

    private lateinit var adapter: DeviceListAdapter
    private val ids = ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_devices, container, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empty = view.findViewById(R.id.empty)
        listDev = view.findViewById(R.id.list_dev)
        btnAdd = view.findViewById(R.id.btn_add)
        ui = view.findViewById(R.id.ui)

        ui.setBackgroundColor(SurfaceColors.SURFACE_0.getColor(requireActivity()))

        btnAdd.backgroundTintList = ColorStateList.valueOf(SurfaceColors.SURFACE_3.getColor(requireActivity()))

        listDev.layoutManager = LinearLayoutManager(requireActivity())
        adapter = DeviceListAdapter(requireActivity(), ids, this)
        listDev.adapter = adapter
        adapter.notifyDataSetChanged()

        enableEdgeToEdge(view)

        initSettings()
        initUX()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initSettings() {
        var i = 1

        ids.clear()
        while (true) {
            val deviceSettings = requireActivity().getSharedPreferences("settings_${i}", Context.MODE_PRIVATE)

            if (deviceSettings.getString("name", "").isNullOrEmpty()) {
                break
            }

            if (deviceSettings.getString("hostname", null) != null) {
                ids.add(i.toString())
            }

            i++
        }

        if (ids.isEmpty()) {
            empty.visibility = View.VISIBLE
        } else {
            empty.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }

    private fun initUX() {
        btnAdd.setOnClickListener {
            val deviceFragment = DeviceFragment.Companion.newInstance(getAvailableDeviceId().toString(), "", "", "", "", "", "")
            deviceFragment.isCancelable = false
            deviceFragment.show(parentFragmentManager.beginTransaction(), "DeviceDialog")

            deviceFragment.setDeviceAddedListener(object : DeviceFragment.DeviceAddedListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onSuccess() {
                    ids.add((getAvailableDeviceId() - 1).toString())
                    adapter.notifyDataSetChanged()

                    empty.visibility = if (ids.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onFailed() {
                    Toast.makeText(requireActivity(), R.string.device_add_failed, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun getAvailableDeviceId(): Int {
        var e = 1

        while (true) {
            val settings = requireActivity().getSharedPreferences("settings_${e}", Context.MODE_PRIVATE)
            if (settings.getString("hostname", "").isNullOrEmpty()) {
                break
            }
            e++
        }

        return e
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onEditClicked(deviceId: String) {
        val deviceSettings: SharedPreferences = requireActivity().getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
        val protocol = deviceSettings.getString("protocol", null)
        val hostname = deviceSettings.getString("hostname", null)
        val port = deviceSettings.getString("port", null)
        val cmd = deviceSettings.getString("cmd", null)
        val devName = deviceSettings.getString("name", null)
        val getter = deviceSettings.getString("getter", null)

        val deviceFragment = DeviceFragment.Companion.newInstance(deviceId, protocol.toString(), hostname.toString(), port.toString(), cmd.toString(), devName.toString(), getter.toString())
        deviceFragment.isCancelable = false
        deviceFragment.setDeviceChangedListener {
            initSettings()
        }
        deviceFragment.show(parentFragmentManager.beginTransaction(), "DeviceDialog")
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onRemoveClicked(deviceId: String) {
        val deviceSettings: SharedPreferences = requireActivity().getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Confirm")
            .setMessage("Do you really want to remove this device?")
            .setPositiveButton("Remove") { _, _ ->
                with(deviceSettings.edit()) {
                    remove("hostname")
                    remove("port")
                    remove("protocol")
                    remove("cmd")
                    remove("getter")
                    remove("name")
                    apply()
                }

                ids.remove(deviceId)

                try {
                    if (ids.contains(StateManager.Companion.getSelectedDeviceId().toString())) {
                        StateManager.Companion.setSelectedDeviceId(null)
                    }
                } catch (_: Exception) {}

                if (ids.isEmpty() || ids.contains(StateManager.Companion.getSelectedDeviceId().toString())) {
                    StateManager.Companion.setSelectedDeviceId(null)
                    empty.visibility = View.VISIBLE
                    listDev.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                } else {
                    empty.visibility = View.GONE
                    listDev.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }
            }

            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDeviceClicked(deviceId: String) {
        StateManager.Companion.setSelectedDeviceId(deviceId)
        (context as MainActivity).openController()
    }

    private fun enableEdgeToEdge(view: View) {
        WindowManager.Companion.adjustPaddings(requireActivity(), view, R.id.ui, EnumSet.of(WindowManager.Companion.Flags.STATUS_BAR, WindowManager.Companion.Flags.IGNORE_PADDINGS))
    }
}
