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

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.teslasoft.iot.rgbcontroller.R
import com.teslasoft.iot.rgbcontroller.util.WindowManager
import java.util.EnumSet

class AboutFragment : Fragment() {
    private lateinit var btnUsedLibs: MaterialButton
    private lateinit var btnPrivacy: MaterialButton
    private lateinit var btnTerms: MaterialButton
    private lateinit var appIcon: ImageView
    private lateinit var appVersion: TextView
    private lateinit var appDesc: TextView
    private lateinit var ui: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnUsedLibs = view.findViewById(R.id.btn_used_libs)
        btnPrivacy = view.findViewById(R.id.btn_privacy)
        btnTerms = view.findViewById(R.id.btn_tos)
        appDesc = view.findViewById(R.id.app_desc)
        appIcon = view.findViewById(R.id.app_icon)
        appVersion = view.findViewById(R.id.app_version)

        appIcon.setImageResource(R.mipmap.ic_launcher_round)

        ui = view.findViewById(R.id.ui)
        ui.setBackgroundColor(SurfaceColors.SURFACE_0.getColor(requireActivity()))
        appDesc.backgroundTintList = ColorStateList.valueOf(SurfaceColors.SURFACE_2.getColor(requireActivity()))

        enableEdgeToEdge(view)

        val pm = requireActivity().packageManager
        try {
            appVersion.text = getString(R.string.text_version).plus(" ").plus(pm.getPackageInfo(requireActivity().packageName, 0).versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            throw IllegalStateException(e)
        }

        val eggCounter = intArrayOf(0)

        appVersion.setOnClickListener {
            if (eggCounter[0] == 0) {
                Handler(Looper.getMainLooper()).postDelayed({ eggCounter[0] = 0 }, 1500)
            }

            if (eggCounter[0] == 4) {
                eggCounter[0] = 0

                try {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.component = ComponentName(
                            "com.teslasoft.libraries.support",
                            "org.teslasoft.core.easter.JarvisPlatLogo"
                    )
                    startActivity(intent)
                } catch (_: Exception) {
                    /* Open easter egg */
                    Toast.makeText(requireActivity(), "Easter egg found!", Toast.LENGTH_SHORT).show()
                }
            }

            eggCounter[0]++
        }

        btnUsedLibs.setOnClickListener {
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.title_used_libs)
                    .setMessage("- Android SDK\n" +
                            "- Kotlin\n" +
                            "- AndroidX\n" +
                            "- com.github.duanhong169.colorpicker\n" +
                            "- OKHTTPP3\n" +
                            "- Android Material\n" +
                            "- AndroidX constraint layout")
                    .setPositiveButton(getString(R.string.btn_close)) { _, _ -> }
                    .show()
        }

        btnPrivacy.setOnClickListener {
            val intent = Intent().apply {
                data = Uri.parse("https://teslasoft.org/privacy")
                action = Intent.ACTION_VIEW
            }
            startActivity(intent)
        }

        btnTerms.setOnClickListener {
            val intent = Intent().apply {
                data = Uri.parse("https://teslasoft.org/tos")
                action = Intent.ACTION_VIEW
            }
            startActivity(intent)
        }
    }

    private fun enableEdgeToEdge(view: View) {
        WindowManager.Companion.adjustPaddings(requireActivity(), view, R.id.ui, EnumSet.of(WindowManager.Companion.Flags.STATUS_BAR, WindowManager.Companion.Flags.IGNORE_PADDINGS))
    }
}
