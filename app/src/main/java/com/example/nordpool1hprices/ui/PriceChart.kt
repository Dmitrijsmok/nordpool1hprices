package com.example.nordpool1hprices.ui

import androidx.compose.foundation.Canvas
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
import com.example.nordpool1hprices.model.PriceEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceChart(prices: List<PriceEntry>) {
    if (prices.isEmpty()) return

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
    val hourWidth = 60f // pixels per hour (controls chart density)
    val chartWidth = totalHours * hourWidth

    val scrollState = rememberScrollState()

    // Auto-scroll near current hour
    LaunchedEffect(prices) {
        val targetScroll = (currentHour * hourWidth - 100).toInt().coerceAtLeast(0)
        scrollState.scrollTo(targetScroll)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "24h price trend",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        // ✅ Scrollable chart box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth.dp) // dynamic width based on data
                    .height(240.dp)
                    .padding(bottom = 8.dp, top = 4.dp)
            ) {
                val w = size.width
                val h = size.height

                val priceToY = { price: Double ->
                    h - ((price - minPrice) / priceRange * h).toFloat()
                }

                // ✅ Horizontal grid lines (price levels)
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
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.2f", priceLabel),
                        8f,
                        y - 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 26f
                            isAntiAlias = true
                        }
                    )
                }

                // ✅ Vertical grid lines + hour labels
                val calendar = Calendar.getInstance(latviaTZ)
                for (i in 0..totalHours) {
                    val x = i * hourWidth
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )

                    if (i < totalHours) {
                        calendar.time = now.time
                        calendar.add(Calendar.HOUR_OF_DAY, i)
                        val hourLabel = hourFormatter.format(calendar.time)
                        drawContext.canvas.nativeCanvas.drawText(
                            hourLabel,
                            x + 4f,
                            h - 6f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 24f
                                isAntiAlias = true
                            }
                        )
                    }
                }

                // ✅ Draw vertical line for current time
                val currentHourFraction = currentHour + (currentMinute / 60f)
                val nowX = currentHourFraction * hourWidth
                drawLine(
                    color = Color(0xFF363636),
                    start = Offset(nowX, 0f),
                    end = Offset(nowX, h),
                    strokeWidth = 3f
                )

                // ✅ Step-style price chart
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
        }

        // ✅ Min/Max footer labels
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
