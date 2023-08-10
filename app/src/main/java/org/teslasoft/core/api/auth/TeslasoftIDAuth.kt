package org.teslasoft.core.api.auth

import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.teslasoft.iot.rgbcontroller.R

class TeslasoftIDAuth : FragmentActivity() {
    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    permit()
                } else {
                    MaterialAlertDialogBuilder(this)
                            .setTitle("Teslasoft Core")
                            .setMessage("Failed to sign in because this app do not have permission.")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                this.setResult(RESULT_CANCELED)
                                finish()
                            }.show()
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teslasoft_id)

        askAuthPermission()
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        try {
            if (result.resultCode >= 20) {
                val intent = Intent()
                intent.putExtra("account_id", result.data?.getStringExtra("account_id"))
                intent.putExtra("signature", result.data?.getStringExtra("signature"))
                this.setResult(result.resultCode, intent)
                finish()
            } else if (result.resultCode == 3 || result.resultCode == 4) {
                this.setResult(result.resultCode)
                finish()
            } else {
                MaterialAlertDialogBuilder(this).setTitle("Teslasoft Core")
                        .setMessage("This app requires one or more Teslasoft Core features that are currently unavailable.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                            this.setResult(RESULT_CANCELED)
                            finish()
                        }.show()
            }
        } catch (e: Exception) {
            if (result.resultCode == 3 || result.resultCode == 4) {
                this.setResult(result.resultCode)
                finish()
            } else {
                this.setResult(result.resultCode)
                MaterialAlertDialogBuilder(this).setTitle("App sync")
                        .setMessage("To use your account with multiple apps you need Teslasoft Core.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                            this.setResult(RESULT_CANCELED)
                            finish()
                        }.show()
            }
        }
    }

    private fun permit() {
        try {
            val apiIntent = Intent()
            apiIntent.component = ComponentName(
                    "com.teslasoft.libraries.support",
                    "org.teslasoft.core.api.account.AccountPickerActivity"
            )
            activityResultLauncher.launch(apiIntent)
        } catch (_: Exception) { /* unused */ }
    }

    private fun askAuthPermission() {
        if (ContextCompat.checkSelfPermission(
                        this, "org.teslasoft.core.permission.AUTHENTICATE_ACCOUNTS"
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            permit()
        } else {
            MaterialAlertDialogBuilder(this).setTitle("Teslasoft Core")
                    .setMessage("To use this feature please allow this app to use Teslasoft Core.")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        requestPermissionLauncher.launch("org.teslasoft.core.permission.AUTHENTICATE_ACCOUNTS")
                    }.setNegativeButton("No thanks") { _: DialogInterface?, _: Int ->
                        this.setResult(RESULT_CANCELED)
                        finish()
                    }.show()
        }
    }
}