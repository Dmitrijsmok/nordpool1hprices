package com.example.nordpool1hprices.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MyDownloadReceiver", "📦 Download complete broadcast received.")

        // ✅ Simply trigger ApkDownloader again — it will find & install automatically
        val activity = context as? android.app.Activity ?: return
        ApkDownloader.checkAndDownloadUpdate(activity,
            "https://gitlab.com/dmitrijsmok1/nordpool1hprices-updates/-/raw/main/nordPool1hPrices-v1.8.1.apk")
    }
}
