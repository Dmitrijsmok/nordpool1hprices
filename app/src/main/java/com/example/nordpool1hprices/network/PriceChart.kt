package com.example.nordpool1hprices.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.nordpool1hprices.model.PriceEntry
import java.time.format.DateTimeFormatter
import java.time.LocalTime

// ✅ IMPORT SHARED COLOR FUNCTION
import com.example.nordpool1hprices.ui.getColorForPrice

@Composable
fun PriceChart(prices: List<PriceEntry>) {
    if (prices.isEmpty()) return

    val maxPrice = prices.maxOf { it.price }
    val minPrice = prices.minOf { it.price }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Price Trend",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)) {
            val w = size.width
            val h = size.height

            val hourWidth = w / 24f
            val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01)
            val priceToY = { price: Double ->
                h - ((price - minPrice) / priceRange * h).toFloat()
            }

            // Draw horizontal price grid lines
            for (i in 0..5) {
                val priceLabel = minPrice + i * (priceRange / 5)
                val y = priceToY(priceLabel)
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.2f", priceLabel),
                    0f,
                    y - 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 24f
                    }
                )
            }

            // Draw vertical time grid lines and hour labels
            for (i in 0..24) {
                val x = i * hourWidth
                drawLine(
                    color = Color.LightGray,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
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

            // Draw step-line chart
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Min: %.3f €".format(minPrice), style = MaterialTheme.typography.bodySmall)
            Text(text = "Max: %.3f €".format(maxPrice), style = MaterialTheme.typography.bodySmall)
        }
    }
}
