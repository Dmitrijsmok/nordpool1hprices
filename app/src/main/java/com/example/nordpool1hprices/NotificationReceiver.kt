package com.example.nordpool1hprices.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nordpool1hprices.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val price = intent?.getStringExtra("price") ?: "Unknown"
        val hour = intent?.getStringExtra("hour") ?: "Time"

        val channelId = "price_alert_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Price Alert"
            val descriptionText = "Notifies about low electricity price"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "price_channel")
            .setSmallIcon(R.drawable.ic_bell_filled)
            .setContentTitle("Upcoming Low Price")
            .setContentText("A low electricity price is coming up in 10 minutes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        }
    }
