package com.example.nordpool1hprices.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.util.Log
import com.example.nordpool1hprices.utils.checkAndRequestExactAlarmPermission

object NotificationScheduler {

    private fun makeRequestCode(hourText: String, price: String): Int =
        ("$hourText|$price").hashCode()

    fun scheduleNotification(context: Context, triggerTimeMillis: Long, hourText: String, price: String) {
        checkAndRequestExactAlarmPermission(context)

        try {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("hour", hourText)
                putExtra("price", price)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                makeRequestCode(hourText, price),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )

            Log.d("NotificationScheduler", "Notification scheduled for $hourText at $triggerTimeMillis")
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling notification: ${e.message}")
        }
    }

    fun cancelNotification(context: Context, hourText: String, price: String) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                makeRequestCode(hourText, price),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            Log.d("NotificationScheduler", "Notification canceled for $hourText")
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error canceling notification: ${e.message}")
            Toast.makeText(
                context,
                "Error canceling notification: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
