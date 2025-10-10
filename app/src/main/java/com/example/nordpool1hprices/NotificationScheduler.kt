package com.example.nordpool1hprices.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*

object NotificationScheduler {
    fun scheduleNotification(context: Context, triggerTimeMillis: Long, hourText: String, price: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("hour", hourText)
            putExtra("price", price)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (triggerTimeMillis / 1000).toInt(), // Unique ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
    }
}
