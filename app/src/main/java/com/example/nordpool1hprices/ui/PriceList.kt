package com.example.nordpool1hprices.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nordpool1hprices.R
import com.example.nordpool1hprices.model.PriceEntry
import com.example.nordpool1hprices.notifications.NotificationScheduler.cancelNotification
import com.example.nordpool1hprices.notifications.NotificationScheduler.scheduleNotification
import com.example.nordpool1hprices.utils.NotificationPreferences
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

private const val MINIMUM_DELAY_MILLIS = 5_000 // 5 seconds

@Composable
fun PriceList(prices: List<PriceEntry>) {
    val latviaTZ = TimeZone.getTimeZone("Europe/Riga")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = latviaTZ
    }
    val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = latviaTZ
    }
    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = latviaTZ
    }

    val now = Calendar.getInstance(latviaTZ)
    val nowDate = now.time

    // --- ✅ Group entries per hour and average them ---
    val hourlyEntries: List<PriceEntry> = prices
        .groupBy { entry ->
            val date = runCatching { sdf.parse(entry.start) }.getOrNull()
            if (date != null) {
                val cal = Calendar.getInstance(latviaTZ).apply { time = date }
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dayKeyFormat.format(cal.time) + " " + String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
            } else ""
        }
        .mapNotNull { (key, group) ->
            if (key.isEmpty() || group.isEmpty()) return@mapNotNull null
            val startCal = Calendar.getInstance(latviaTZ)
            startCal.time = sdf.parse(group.first().start) ?: return@mapNotNull null
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val endCal = startCal.clone() as Calendar
            endCal.add(Calendar.HOUR_OF_DAY, 1)

            PriceEntry(
                start = sdf.format(startCal.time),
                end = sdf.format(endCal.time),
                price = group.map { it.price }.average(),
                notify = group.any { it.notify }
            )
        }
        .sortedBy { sdf.parse(it.start) }

    val grouped = hourlyEntries.groupBy {
        runCatching {
            val dt = sdf.parse(it.start)
            if (dt != null) dayKeyFormat.format(dt) else ""
        }.getOrDefault("")
    }

    val today = Calendar.getInstance(latviaTZ)
    val tomorrow = Calendar.getInstance(latviaTZ).apply { add(Calendar.DAY_OF_YEAR, 1) }

    val orderedDays = listOf(
        dayKeyFormat.format(today.time),
        dayKeyFormat.format(tomorrow.time)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        orderedDays.forEach { dayKey ->
            val entriesForDay = grouped[dayKey] ?: return@forEach

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                val headerText = when (dayKey) {
                    dayKeyFormat.format(today.time) -> "Today"
                    dayKeyFormat.format(tomorrow.time) -> "Tomorrow"
                    else -> ""
                }

                if (headerText.isNotEmpty()) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val validEntries = entriesForDay
                    .filter {
                        val end = runCatching { sdf.parse(it.end) }.getOrNull()
                        end != null && !end.before(nowDate)
                    }
                    .sortedBy { sdf.parse(it.start) }

                // Persisted active notifications (epoch millis of start time)
                val activeNotifs by NotificationPreferences.observe(context).collectAsState(initial = emptySet())

                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(validEntries) { entry ->
                        // Separate UI-only state: dropdown open
                        var isMenuOpen by remember(entry) { mutableStateOf(false) }
                        var reminderTime by remember(entry) { mutableStateOf(10) } // Default reminder time: 10 minutes

                        val startDate = runCatching { sdf.parse(entry.start) }.getOrNull()
                        val endDate = runCatching { sdf.parse(entry.end) }.getOrNull()

                        if (startDate != null && endDate != null) {
                            val startMillis = startDate.time
                            val isNotified = activeNotifs.contains(startMillis)
                            val calStart = Calendar.getInstance(latviaTZ).apply { time = startDate }

                            val isNow =
                                calStart.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        calStart.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                                        calStart.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)

                            val timeRange = "${hourFormat.format(startDate)} – ${hourFormat.format(endDate)}"

                            val priceStr = String.format("%.3f", entry.price)
                            val parts = priceStr.split(".")
                            val intPart = parts.getOrElse(0) { "0" }
                            val fracPart = parts.getOrElse(1) { "000" }

                            val context = LocalContext.current

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp, vertical = 0.dp)
                                    .then(
                                        if (isNow) Modifier.border(
                                            BorderStroke(1.dp, Color.Red)
                                        ).padding(6.dp) else Modifier
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 🕒 Time
                                Text(
                                    text = timeRange,
                                    modifier = Modifier.weight(1f),
                                    fontSize = if (isNow) 13.sp else 14.sp
                                )

                                // 🔔 Notification button
                                IconButton(
                                    onClick = {
                                        if (isNotified) {
                                            // Turn off notifications
                                            entry.notify = false
                                            handleNotification(context, false, entry, startDate, timeRange, reminderTime)
                                            scope.launch { NotificationPreferences.remove(context, startMillis) }
                                        } else {
                                            // Open reminder time picker
                                            isMenuOpen = true
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isNotified) R.drawable.ic_bell_filled else R.drawable.ic_bell_outline
                                        ),
                                        contentDescription = "Notification",
                                        tint = if (isNotified) Color(0xFFFFA000) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Reminder Time Picker (decoupled from bell state)
                                DropdownMenu(
                                    expanded = isMenuOpen,
                                    onDismissRequest = { isMenuOpen = false }
                                ) {
                                    listOf(5, 10, 15, 30).forEach { time ->
                                        DropdownMenuItem(
                                            text = { Text("$time minutes before") },
                                            onClick = {
                                                reminderTime = time
                                                // Enable notifications and schedule
                                                entry.notify = true
                                                handleNotification(context, true, entry, startDate, timeRange, reminderTime)
                                                scope.launch { NotificationPreferences.add(context, startMillis) }
                                                isMenuOpen = false
                                            }
                                        )
                                    }
                                }

                                // 💰 Price
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$intPart.${fracPart.take(2)}",
                                        color = getColorForPrice(entry.price),
                                        fontWeight = if (entry.price < 0.15) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isNow) 14.sp else 15.sp
                                    )
                                    Text(
                                        text = fracPart.drop(2),
                                        color = getColorForPrice(entry.price),
                                        fontWeight = if (isNow) FontWeight.Bold else FontWeight.Light,
                                        fontSize = if (isNow) 9.sp else 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleNotification(
    context: Context,
    notify: Boolean,
    entry: PriceEntry,
    startDate: Date,
    timeRange: String,
    reminderTime: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasPermission = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Toast.makeText(
                context,
                "Please enable notifications in Android settings.",
                Toast.LENGTH_LONG
            ).show()

            // Open the app's Notification Settings
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
            return
        }
    }

    val triggerTimeMillis = (startDate.time - reminderTime * 60 * 1000)
        .coerceAtLeast(System.currentTimeMillis() + MINIMUM_DELAY_MILLIS)
    val priceText = String.format("%.3f", entry.price)

    if (notify) {
        Log.d("Notification", "Scheduling notification for $timeRange at $triggerTimeMillis")
        scheduleNotification(
            context,
            triggerTimeMillis,
            timeRange,
            priceText
        )
        Toast.makeText(context, "Notification set for $timeRange", Toast.LENGTH_SHORT).show()
    } else {
        Log.d("Notification", "Canceling notification for $timeRange")
        cancelNotification(
            context,
            timeRange,
            priceText
        )
        Toast.makeText(context, "Notification cancelled", Toast.LENGTH_SHORT).show()
    }
}
