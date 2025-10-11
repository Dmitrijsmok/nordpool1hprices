package com.example.nordpool1hprices.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object NotificationScheduler {

    /** ✅ Schedule notification for given time */
    fun scheduleNotification(context: Context, triggerTimeMillis: Long, hourText: String, price: String) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("hour", hourText)
                putExtra("price", price)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (triggerTimeMillis / 1000).toInt(), // Unique ID based on time
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // ✅ Android 12+ (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    Log.d("NotificationScheduler", "Exact alarm scheduled successfully.")
                } else {
                    Log.w("NotificationScheduler", "Exact alarms not permitted, requesting permission.")
                    Toast.makeText(
                        context,
                        "Please allow exact alarms for notifications to work properly.",
                        Toast.LENGTH_LONG
                    ).show()

                    val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            } else {
                // ✅ Older Android versions
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d("NotificationScheduler", "Alarm scheduled for older Android version.")
            }

        } catch (e: SecurityException) {
            Log.e("NotificationScheduler", "SecurityException while scheduling alarm", e)
            Toast.makeText(context, "Unable to schedule alarm: permission issue", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling notification", e)
            Toast.makeText(context, "Failed to schedule notification", Toast.LENGTH_SHORT).show()
        }
    }

    /** ❌ Cancel previously scheduled notification for given time */
    fun cancelNotification(context: Context, triggerTimeMillis: Long) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (triggerTimeMillis / 1000).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            Log.d("NotificationScheduler", "Cancelled notification for trigger time: $triggerTimeMillis")
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error cancelling notification", e)
        }
    }
}
