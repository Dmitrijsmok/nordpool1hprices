package com.example.nordpool1hprices.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.nordpool1hprices.model.PriceEntry
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PriceChart(prices: List<PriceEntry>) {
    if (prices.isEmpty()) return

    val now = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    val maxPrice = prices.maxOf { it.price }
    val minPrice = prices.minOf { it.price }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Price Trend",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            val w = size.width
            val h = size.height

            val hourWidth = w / 24f
            val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01)
            val priceToY = { price: Double -> h - ((price - minPrice) / priceRange * h).toFloat() }

            for (i in 0..5) {
                val priceLabel = minPrice + i * (priceRange / 5)
                val y = priceToY(priceLabel)
                drawLine(Color.LightGray, Offset(0f, y), Offset(w, y), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.2f", priceLabel),
                    0f, y - 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 24f
                    }
                )
            }

            for (i in 0..24) {
                val x = i * hourWidth
                drawLine(Color.LightGray, Offset(x, 0f), Offset(x, h), 1f)
                if (i < 24) {
                    val hourLabel = LocalTime.of(i, 0).format(DateTimeFormatter.ofPattern("HH"))
                    drawContext.canvas.nativeCanvas.drawText(
                        hourLabel,
                        x + 4f,
                        h - 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 24f
                        }
                    )
                }
            }

            prices.forEachIndexed { i, entry ->
                if (i >= prices.size - 1) return@forEachIndexed
                val p1 = prices[i]
                val p2 = prices[i + 1]
                val x1 = i * hourWidth
                val x2 = (i + 1) * hourWidth
                val y1 = priceToY(p1.price)
                val y2 = priceToY(p2.price)
                val color = getColorForPrice(p1.price)

                drawLine(color, Offset(x1, y1), Offset(x2, y1), 4f, cap = StrokeCap.Butt)
                drawLine(color, Offset(x2, y1), Offset(x2, y2), 4f, cap = StrokeCap.Butt)

                val startDate = runCatching { sdf.parse(p1.start) }.getOrNull()
                val cal = Calendar.getInstance().apply { time = startDate ?: Date() }

                if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    && cal.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)
                ) {
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(x1, y1 - 10),
                        size = androidx.compose.ui.geometry.Size(hourWidth, 20f),
                        style = Stroke(width = 2f)
                    )
                }
            }
            // 🕒 Current time vertical line
            val currentHour = LocalTime.now().hour
            val xNow = currentHour * (size.width / 24f)

            drawLine(
                color = Color(0xFF363636),
                start = Offset(xNow, 0f),
                end = Offset(xNow, size.height),
                strokeWidth = 2f
            )

// Optional: small circle marker on current price
            val currentEntry = prices.getOrNull(currentHour)
            if (currentEntry != null) {
                val yNow = priceToY(currentEntry.price)
                drawCircle(
                    color = Color.Red,
                    radius = 6f,
                    center = Offset(xNow, yNow)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Min: %.3f €".format(minPrice), style = MaterialTheme.typography.bodySmall)
            Text(text = "Max: %.3f €".format(maxPrice), style = MaterialTheme.typography.bodySmall)
        }
    }
}
