package com.example.nordpool1hprices.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.util.Log
import android.os.Build
import android.provider.Settings
import com.example.nordpool1hprices.utils.checkAndRequestExactAlarmPermission

object NotificationScheduler {

    private fun checkAndRequestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    context,
                    "Please enable exact alarms in system settings.",
                    Toast.LENGTH_LONG
                ).show()

                // Open the Alarms and Reminders settings
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        }
    }

    fun scheduleNotification(context: Context, triggerTimeMillis: Long, hourText: String, price: String) {
        checkAndRequestExactAlarmPermission(context)

        try {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("hour", hourText)
                putExtra("price", price)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                triggerTimeMillis.toInt(),
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

    fun cancelNotification(context: Context, triggerTimeMillis: Long) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                triggerTimeMillis.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            Log.d("NotificationScheduler", "Notification canceled for $triggerTimeMillis")
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