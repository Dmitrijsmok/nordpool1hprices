package com.example.nordpool1hprices.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class PackageInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra("android.content.pm.extra.STATUS", -1)
        val msg = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE")

        Log.d("PackageInstallReceiver", "Install result: status=$status, msg=$msg")

        when (status) {
            0 -> Toast.makeText(context, "✅ Update installed successfully", Toast.LENGTH_LONG).show()
            1 -> Toast.makeText(context, "❌ Install blocked by policy", Toast.LENGTH_LONG).show()
            3 -> Toast.makeText(context, "⚠️ User cancelled install", Toast.LENGTH_LONG).show()
            else -> Toast.makeText(context, "❌ Install failed: $msg", Toast.LENGTH_LONG).show()
        }
    }
}
