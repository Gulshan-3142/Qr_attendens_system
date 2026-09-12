package com.college.attendance.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceUtils {

    @SuppressLint("HardwareIds")
    fun getUniqueDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrBlank()) {
                androidId
            } else {
                "android_${Build.SERIAL ?: "device"}_${Build.BOARD}"
            }
        } catch (e: Exception) {
            "android_fallback_${Build.MODEL}_${Build.ID}"
        }
    }

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: "Android Device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }.trim()
    }
}
