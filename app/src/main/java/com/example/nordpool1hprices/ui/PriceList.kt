package com.example.nordpool1hprices.ui

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nordpool1hprices.R
import com.example.nordpool1hprices.model.PriceEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceList(prices: List<PriceEntry>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val now = Calendar.getInstance()
    val nowDate = now.time

    // Group prices by day
    val grouped = prices.groupBy { entry ->
        runCatching {
            val dt = sdf.parse(entry.start)
            if (dt != null) dayKeyFormat.format(dt) else ""
        }.getOrDefault("")
    }

    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val allowedDates = listOf(
        dayKeyFormat.format(today.time),
        dayKeyFormat.format(tomorrow.time)
    )

    // Wider space between Today and Tomorrow columns
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        grouped
            .filterKeys { it in allowedDates }
            .forEach { (dayKey, entriesForDay) ->
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

                    // Filter out past hours for "Today"
                    val validEntries = entriesForDay.filter { entry ->
                        val start = runCatching { sdf.parse(entry.start) }.getOrNull()
                        start != null && !start.before(nowDate)
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        items(validEntries) { entry ->
                            var notify by remember(entry) { mutableStateOf(entry.notify) }

                            val startDate = runCatching { sdf.parse(entry.start) }.getOrNull()
                            val endDate = runCatching { sdf.parse(entry.end) }.getOrNull()

                            if (startDate != null && endDate != null) {
                                val calStart = Calendar.getInstance().apply { time = startDate }

                                val isNow =
                                    calStart.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                            calStart.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                                            calStart.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)

                                val timeRange =
                                    "${hourFormat.format(startDate)} – ${hourFormat.format(endDate)}"

                                val priceStr = String.format("%.3f", entry.price)
                                val parts = priceStr.split(".")
                                val intPart = parts.getOrElse(0) { "0" }
                                val fracPart = parts.getOrElse(1) { "000" }

                                // ✅ Flexible layout: Time | Bell | Price
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 0.dp)
                                        .then(
                                            if (isNow) Modifier.border(
                                                BorderStroke(2.dp, Color.Red)
                                            ).padding(4.dp) else Modifier
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Time (takes most space)
                                    Text(
                                        text = timeRange,
                                        modifier = Modifier.weight(1f),
                                        fontSize = if (isNow) 18.sp else 14.sp
                                    )

                                    // Bell (small fixed size)
                                    IconButton(
                                        onClick = {
                                            notify = !notify
                                            entry.notify = notify
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (notify) R.drawable.ic_bell_filled
                                                else R.drawable.ic_bell_outline
                                            ),
                                            contentDescription = "Notification",
                                            tint = if (notify) Color(0xFFFFA000) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Price (right aligned)
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$intPart.${fracPart.take(2)}",
                                            color = getColorForPrice(entry.price),
                                            fontWeight = if (entry.price < 0.15) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (isNow) 18.sp else 15.sp
                                        )
                                        Text(
                                            text = fracPart.drop(2),
                                            color = getColorForPrice(entry.price),
                                            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Light,
                                            fontSize = if (isNow) 12.sp else 10.sp
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
