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

package com.teslasoft.iot.rgbcontroller.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.elevation.SurfaceColors
import com.teslasoft.iot.rgbcontroller.R
import org.teslasoft.core.api.network.RequestNetwork
import org.teslasoft.core.api.network.RequestNetworkController

class DeviceListAdapter(
        private val context: FragmentActivity,
        private val data: List<String>,
        private var onDeviceActionsClicked: OnDeviceActionsClicked
) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view, context, onDeviceActionsClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface OnDeviceActionsClicked {
        fun onEditClicked(deviceId: String)
        fun onRemoveClicked(deviceId: String)
        fun onDeviceClicked(deviceId: String)
    }

    class ViewHolder(view: View, private var context: FragmentActivity, private var onDeviceActionsClicked: OnDeviceActionsClicked) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val status: TextView = view.findViewById(R.id.status)
        val edit: MaterialButton = view.findViewById(R.id.edit)
        val remove: MaterialButton = view.findViewById(R.id.remove)
        val ui: ConstraintLayout = view.findViewById(R.id.ui)

        private var protocol: String? = null
        private var hostname: String? = null
        private var port: String? = null
        private var cmd: String? = null
        private var devName: String? = null
        private var getter: String? = null

        fun bind(deviceId: String) {
            ui.backgroundTintList = ColorStateList.valueOf(SurfaceColors.SURFACE_2.getColor(context))

            try {
                val deviceSettings: SharedPreferences = context.getSharedPreferences("settings_$deviceId", Context.MODE_PRIVATE)
                protocol = deviceSettings.getString("protocol", null)
                hostname = deviceSettings.getString("hostname", null)
                port = deviceSettings.getString("port", null)
                cmd = deviceSettings.getString("cmd", null)
                devName = deviceSettings.getString("name", null)
                getter = deviceSettings.getString("getter", null)

                name.text = devName

                initLogic(edit, remove, status, ui, deviceId)
            } catch (_: Exception) {
                ui.visibility = View.GONE
            }
        }

        private fun check(protocol: String?, hostname: String?, port: String?, availabilityApi: RequestNetwork, availabilityApiListener: RequestNetwork.RequestListener) {
            val handler = Handler(Looper.getMainLooper())

            handler.postDelayed({
                availabilityApi.startRequestNetwork(
                        RequestNetworkController.Companion.GET,
                        "${protocol}://${hostname}:${port}", "A",
                        availabilityApiListener
                )

                check(protocol, hostname, port, availabilityApi, availabilityApiListener)
            }, 3000)
        }

        private fun initLogic(edit: MaterialButton, remove: MaterialButton, status: TextView, ui: ConstraintLayout, deviceId: String) {
            edit.setOnClickListener {
                onDeviceActionsClicked.onEditClicked(deviceId)
            }

            remove.setOnClickListener {
                onDeviceActionsClicked.onRemoveClicked(deviceId)
            }

            ui.setOnClickListener {
                onDeviceActionsClicked.onDeviceClicked(deviceId)
            }

            val availabilityApi = RequestNetwork(context)

            val availabilityApiListener = object : RequestNetwork.RequestListener {
                @SuppressLint("SetTextI18n")
                override fun onResponse(tag: String, response: String) {
                    status.text = "Online"
                    status.setTextColor(context.getColor(R.color.success))
                }

                @SuppressLint("SetTextI18n")
                override fun onErrorResponse(tag: String, message: String) {
                    status.text = "Offline"
                    status.setTextColor(context.getColor(R.color.error))
                }
            }

            check(protocol, hostname, port, availabilityApi, availabilityApiListener)
        }
    }
}
