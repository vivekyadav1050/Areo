package com.example.cse227.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Utility to generate and persist a unique, stable tablet identifier (deviceKey).
 * Ensures that the same tablet retains its registered identity across app restarts
 * without attempting to access forbidden hardware MAC addresses.
 */
object DeviceKeyGenerator {

    private const val PREF_NAME = "ap20_device_key_prefs"
    private const val KEY_DEVICE_KEY = "stable_device_key"

    fun getOrCreateDeviceKey(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var deviceKey = prefs.getString(KEY_DEVICE_KEY, null)
        if (deviceKey.isNullOrBlank()) {
            deviceKey = "TABLET-" + UUID.randomUUID().toString().uppercase()
            prefs.edit().putString(KEY_DEVICE_KEY, deviceKey).apply()
        }
        return deviceKey
    }
}