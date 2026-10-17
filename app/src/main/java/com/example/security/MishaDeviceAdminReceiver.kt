package com.example.security

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MishaDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("MishaDeviceAdmin", "Device Admin authorization granted.")
        Toast.makeText(context, "Misha AI: Device Lock capability enabled.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i("MishaDeviceAdmin", "Device Admin authorization revoked.")
        Toast.makeText(context, "Misha AI: Device Lock capability disabled.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, MishaDeviceAdminReceiver::class.java)
        }
    }
}
