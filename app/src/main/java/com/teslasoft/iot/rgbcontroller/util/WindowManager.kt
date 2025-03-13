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

package com.teslasoft.iot.rgbcontroller.util

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import java.util.EnumSet
import kotlin.math.roundToInt

class WindowManager {
    companion object {
        enum class Flags {
            STATUS_BAR,
            NAVIGATION_BAR,
            IGNORE_PADDINGS
        }

        fun adjustPaddings(activity: Activity, rootView: View, res: Int, flags: EnumSet<Flags>, customPaddingTop: Int = 0, customPaddingBottom: Int = 0) {
            if (activity.window.decorView.rootWindowInsets == null) {
                Log.w("WindowManager", "[WARNING] You might want to place the call of this method inside onAttachedToWindow or window inset controller ready listener.")
                Handler(Looper.getMainLooper()).postDelayed({
                    adjustPaddingsDelayed(activity, rootView, res, flags, customPaddingTop, customPaddingBottom)
                }, 100)
            } else {
                adjustPaddingsDelayed(
                    activity,
                    rootView,
                    res,
                    flags,
                    customPaddingTop,
                    customPaddingBottom
                )
            }
        }

        private fun adjustPaddingsDelayed(activity: Activity, rootView: View, res: Int, flags: EnumSet<Flags>, customPaddingTop: Int = 0, customPaddingBottom: Int = 0) {
            val view = rootView.findViewById<View?>(res)

            checkNotNull(view) {
                "[Impossible or illegal scenario reached] This is a fucking Exception that appears only when some bugs are flying nearby. Target view is NULL."
            }

            view.setPadding(
                0,
                activity.window.decorView.rootWindowInsets.getInsets(WindowInsets.Type.statusBars()).top * (if (flags.contains(Flags.STATUS_BAR)) 1 else 0) + view.paddingTop * (if (flags.contains(Flags.IGNORE_PADDINGS)) 0 else 1) + dpToPx(activity, customPaddingTop),
                0,
                activity.window.decorView.rootWindowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom * (if (flags.contains(Flags.NAVIGATION_BAR)) 1 else 0) + view.paddingBottom * (if (flags.contains(Flags.IGNORE_PADDINGS)) 0 else 1) + dpToPx(activity, customPaddingBottom)
            )
        }

        private fun getScreenWidth(activity: Activity): Int {
            return activity.resources.displayMetrics.widthPixels
        }

        private fun getScreenHeight(activity: Activity): Int {
            return activity.resources.displayMetrics.heightPixels
        }

        private fun dpToPx(activity: Activity, dp: Int): Int {
            val density = activity.resources.displayMetrics.density
            return (dp.toFloat() * density).roundToInt()
        }
    }
}
