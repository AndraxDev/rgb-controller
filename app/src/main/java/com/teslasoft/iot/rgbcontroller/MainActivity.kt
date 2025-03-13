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

package com.teslasoft.iot.rgbcontroller

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.elevation.SurfaceColors
import com.teslasoft.iot.rgbcontroller.fragments.tabs.AboutFragment
import com.teslasoft.iot.rgbcontroller.fragments.tabs.ControllerFragment
import com.teslasoft.iot.rgbcontroller.fragments.tabs.DeviceListFragment

class MainActivity : FragmentActivity() {
    private lateinit var containerFragment: ConstraintLayout
    private lateinit var navigator: BottomNavigationView
    private var deviceListFragment: DeviceListFragment? = null
    private var controllerFragment: ControllerFragment? = null
    private var aboutFragment: AboutFragment? = null

    private var selectedTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedTab != 0) {
                    selectedTab = 0
                    loadFragment(deviceListFragment ?: return)
                    navigator.selectedItemId = R.id.page_devices
                } else {
                    finish()
                }
            }
        })

        if (savedInstanceState !== null) {
            init()
        }
    }

    private fun initUI() {
        containerFragment = findViewById(R.id.fragment_container)
        navigator = findViewById(R.id.bottom_navigation)
        navigator.setBackgroundColor(SurfaceColors.SURFACE_3.getColor(this))

        window.setBackgroundDrawable(ColorDrawable(SurfaceColors.SURFACE_0.getColor(this)))
    }

    private fun init() {
        initUI()
        initNavigator()
        deviceListFragment = DeviceListFragment()
        controllerFragment = ControllerFragment()
        aboutFragment = AboutFragment()
        loadFragmentFromTab(selectedTab)
    }

    private fun loadFragmentFromTab(tab: Int) {
        when (tab) {
            0 -> loadFragment(deviceListFragment ?: return)
            1 -> loadFragment(controllerFragment ?: return)
            2 -> loadFragment(aboutFragment ?: return)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).replace(R.id.fragment_container, fragment).commit()
    }

    private fun initNavigator() {
        navigator.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_devices -> {
                    loadFragment(deviceListFragment ?: return@setOnItemSelectedListener false)
                    selectedTab = 0
                    true
                }
                R.id.page_controller -> {
                    loadFragment(controllerFragment ?: return@setOnItemSelectedListener false)
                    selectedTab = 1
                    true
                }
                R.id.page_settings -> {
                    loadFragment(aboutFragment ?: return@setOnItemSelectedListener false)
                    selectedTab = 2
                    true
                }
                else -> false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", selectedTab)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedTab = savedInstanceState.getInt("tab")
        loadFragmentFromTab(selectedTab)
    }

    fun openController() {
        selectedTab = 1
        loadFragment(controllerFragment ?: return)
        navigator.selectedItemId = R.id.page_controller
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        init()
    }
}
