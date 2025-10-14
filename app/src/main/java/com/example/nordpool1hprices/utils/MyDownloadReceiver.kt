package com.example.nordpool1hprices.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MyDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MyDownloadReceiver", "📦 Download complete broadcast received.")

        // 🔸 Just notify user — don’t try to start installer from background
        Toast.makeText(
            context,
            "Update downloaded — please open the app to install.",
            Toast.LENGTH_LONG
        ).show()
    }
}
