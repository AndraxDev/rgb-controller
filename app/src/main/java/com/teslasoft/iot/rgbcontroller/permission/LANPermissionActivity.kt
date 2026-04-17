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

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

class LANPermissionActivity : AbstractPermissionActivity() {
    // This permission is automatically granted on android 16 and below (API 36).
    // However, android.permission.INTERNET is still required to access LAN on older android versions.
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    override fun onCreate(savedInstanceState: Bundle?) {
        context = this
        denyMessage = "You denied nearby devices permission. All local devices will report status as offline until you allow app to discover local network devices."
        requestTitle = "Allow local network permission?"
        requestMessage = "Starting from Android 17, RGB Controller requires LAN permission to connect to the local smart devices. If you deny this permission, apps core functionality will be limited only to the remote devices. Allow LAN access?"
        permissionCode = Manifest.permission.ACCESS_LOCAL_NETWORK
        minSDKVersion = Build.VERSION_CODES.CINNAMON_BUN
        super.onCreate(savedInstanceState)
    }
}
