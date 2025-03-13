package com.teslasoft.iot.rgbcontroller.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class AbstractPermissionActivity : FragmentActivity() {
    protected lateinit var context: Context
    protected lateinit var PERMISSION_CODE: String
    protected var requestMessage: String = ""
    protected var requestTitle: String = ""
    protected var denyMessage: String = ""

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        if (ContextCompat.checkSelfPermission(this, PERMISSION_CODE) == PackageManager.PERMISSION_GRANTED) {
            setResult(RESULT_OK)
            finish()
        } else if (shouldShowRequestPermissionRationale(PERMISSION_CODE)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(requestTitle)
                .setMessage(requestMessage)
                .setCancelable(false)
                .setPositiveButton("Allow") { _, _ ->
                    requestPermissionLauncher.launch(PERMISSION_CODE)
                }
                .setNegativeButton("No, thanks") { _, _ ->
                    setResult(RESULT_CANCELED)
                    finish()
                }
                .show()
        } else {
            requestPermissionLauncher.launch(PERMISSION_CODE)
        }
    }
}
