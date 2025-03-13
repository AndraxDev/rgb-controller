package com.teslasoft.iot.rgbcontroller.permission

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

class NearbyPermissionActivity : AbstractPermissionActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        context = this
        denyMessage = "You denied nearby devices permission. All local devices will report status as offline until you allow app to discover local network devices."
        requestTitle = "Allow nearby devices permission?"
        requestMessage = "Starting from Android 16 Beta 3, RGB Controller requires nearby devices permission to connect to the local smart devices. If you deny this permission, apps core functionality will be limited only to the remote devices. Allow nearby access?"
        PERMISSION_CODE = android.Manifest.permission.NEARBY_WIFI_DEVICES
        super.onCreate(savedInstanceState)
    }
}