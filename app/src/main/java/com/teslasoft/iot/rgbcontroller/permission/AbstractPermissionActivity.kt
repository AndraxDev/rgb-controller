/**************************************************************************
 * Copyright (c) 2022-2026 Dmytro Ostapenko. All rights reserved.
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

package com.teslasoft.iot.rgbcontroller.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class AbstractPermissionActivity : FragmentActivity() {
    protected lateinit var context: Context
    protected lateinit var permissionCode: String
    protected var requestMessage: String = ""
    protected var requestTitle: String = ""
    protected var denyMessage: String = ""
    protected var minSDKVersion = 1

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < minSDKVersion) {
            // Permission is not required or android build on user device does not support permission or requested system feature, skip the check.
            setResult(RESULT_OK)
            finish()
        }

        context = this

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                setResult(RESULT_OK)
                finish()
            } else {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Permission denied")
                    .setMessage(denyMessage)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                    .show()
            }
        }
        askNotificationPermission()
    }

    /**
     * Method to check and request the necessary permission using a dialog if the permission is not granted.
     */
    private fun askNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, permissionCode) == PackageManager.PERMISSION_GRANTED) {
            setResult(RESULT_OK)
            finish()
        } else if (shouldShowRequestPermissionRationale(permissionCode)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(requestTitle)
                .setMessage(requestMessage)
                .setCancelable(false)
                .setPositiveButton("Allow") { _, _ ->
                    requestPermissionLauncher.launch(permissionCode)
                }
                .setNegativeButton("No, thanks") { _, _ ->
                    setResult(RESULT_CANCELED)
                    finish()
                }
                .show()
        } else {
            requestPermissionLauncher.launch(permissionCode)
        }
    }
}
