package com.example.nordpool1hprices.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nordpool1hprices.model.PriceEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceChart(prices: List<PriceEntry>) {
    if (prices.isEmpty()) return

    // === Data setup ===
    val rawMaxPrice = prices.maxOf { it.price }
    val minPrice = prices.minOf { it.price }
    val maxPrice = if (rawMaxPrice > 0.8) 0.8 else rawMaxPrice
    val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01)

    val latviaTZ = TimeZone.getTimeZone("Europe/Riga")
    val now = Calendar.getInstance(latviaTZ)
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    val hourFormatter = SimpleDateFormat("HH", Locale.getDefault())

    val totalHours = prices.size.coerceAtLeast(1)
    val hourWidth = 60f // Adjust zoom
    val chartWidth = totalHours * hourWidth

    val scrollState = rememberScrollState()

    // === Auto-scroll to current hour on start ===
    LaunchedEffect(prices) {
        val targetScroll = (currentHour * hourWidth - 100).toInt().coerceAtLeast(0)
        scrollState.scrollTo(targetScroll)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // === Chart Title ===
        Text(
            text = "24h price trend",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // === Fixed Y-axis (price labels) ===
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val h = size.height
                    for (i in 0..5) {
                        val priceLabel = minPrice + i * (priceRange / 5)
                        val y = h - ((priceLabel - minPrice) / priceRange * h).toFloat()
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.2f", priceLabel),
                            4f,
                            y,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 26f
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }

            // === Scrollable chart with X-axis labels ===
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .background(Color.White)
            ) {
                // === Chart Canvas ===
                Canvas(
                    modifier = Modifier
                        .width(chartWidth.dp)
                        .height(220.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    val priceToY = { price: Double ->
                        h - ((price - minPrice) / priceRange * h).toFloat()
                    }

                    // Horizontal grid lines
                    for (i in 0..5) {
                        val priceLabel = minPrice + i * (priceRange / 5)
                        val y = priceToY(priceLabel)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    // Vertical grid lines
                    for (i in 1..totalHours) {
                        val x = i * hourWidth
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }

                    // Current hour vertical line
                    val currentHourFraction = currentHour + (currentMinute / 60f)
                    val nowX = currentHourFraction * hourWidth
                    drawLine(
                        color = Color(0xFF363636),
                        start = Offset(nowX, 0f),
                        end = Offset(nowX, h),
                        strokeWidth = 3f
                    )

                    // Step-style chart line
                    for (i in 0 until prices.size - 1) {
                        val p1 = prices[i]
                        val p2 = prices[i + 1]
                        val x1 = i * hourWidth
                        val x2 = (i + 1) * hourWidth
                        val y1 = priceToY(p1.price)
                        val y2 = priceToY(p2.price)
                        val color = getColorForPrice(p1.price)

                        drawLine(
                            color = color,
                            start = Offset(x1, y1),
                            end = Offset(x2, y1),
                            strokeWidth = 4f,
                            cap = StrokeCap.Butt
                        )
                        drawLine(
                            color = color,
                            start = Offset(x2, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 4f,
                            cap = StrokeCap.Butt
                        )
                    }
                }

                // === X-axis labels (below chart) ===
                Row(
                    modifier = Modifier
                        .width(chartWidth.dp)
                        .height(30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val calendar = Calendar.getInstance(latviaTZ)
                    for (i in 1..totalHours) { // start from 1 to skip "00"
                        calendar.time = now.time
                        calendar.add(Calendar.HOUR_OF_DAY, i)
                        val hourLabel = hourFormatter.format(calendar.time)
                        Text(
                            text = hourLabel,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.width(hourWidth.dp)
                        )
                    }
                }
            }
        }

        // === Min/Max footer ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: %.3f €".format(minPrice),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = "Max: %.3f €".format(maxPrice),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD32F2F)
            )
        }
    }
}
