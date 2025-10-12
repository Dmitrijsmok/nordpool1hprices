package com.example.nordpool1hprices.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.nordpool1hprices.model.PriceEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceChart(prices: List<PriceEntry>) {
    if (prices.isEmpty()) return

    // === Data setup ===
    val latviaTZ = TimeZone.getTimeZone("Europe/Riga")
    val now = Calendar.getInstance(latviaTZ)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = latviaTZ
    }
    val hourFormatter = SimpleDateFormat("HH", Locale.getDefault())

    val firstTime = sdf.parse(prices.first().start)
    val lastTime = sdf.parse(prices.last().end)
    val totalHours = if (firstTime != null && lastTime != null) {
        ((lastTime.time - firstTime.time) / (1000 * 60 * 60)).toInt().coerceAtLeast(1)
    } else prices.size.coerceAtLeast(1)

    val rawMaxPrice = prices.maxOf { it.price }
    val minPrice = prices.minOf { it.price }
    val maxPrice = if (rawMaxPrice > 0.8) 0.8 else rawMaxPrice
    val priceRange = (maxPrice - minPrice).takeIf { it > 0.005 } ?: 0.05

    // === Layout scaling ===
    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp * (configuration.densityDpi / 160f)
    val hourWidth = screenWidthPx / 16f   // about 12–16 hours per screen
    val chartWidth = totalHours * hourWidth

    val scrollState = rememberScrollState()

    // === Center chart on current hour when opened ===
    LaunchedEffect(prices) {
        val baseHour = firstTime?.let { (now.time.time - it.time) / (1000 * 60 * 60f) } ?: 0f
        val nowX = baseHour * hourWidth
        val centerScroll = (nowX - screenWidthPx / 2)
            .toInt()
            .coerceIn(0, (chartWidth - screenWidthPx).toInt())
        scrollState.animateScrollTo(centerScroll)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            // === Fixed Y-axis ===
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
                            6f,
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

            // === Scrollable chart area ===
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (chartWidth > screenWidthPx)
                            Modifier.horizontalScroll(scrollState)
                        else Modifier
                    )
                    .background(Color.White)
            ) {
                Canvas(
                    modifier = Modifier
                        .width((chartWidth / (configuration.densityDpi / 160f)).dp)
                        .height(260.dp)
                        .padding(bottom = 16.dp)
                ) {
                    val w = size.width
                    val h = size.height - 24f // bottom space for labels

                    val priceToY = { price: Double ->
                        h - ((price - minPrice) / priceRange * h).toFloat()
                    }

                    // === Horizontal grid lines ===
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

                    // === Vertical grid + hour labels ===
                    val calendar = Calendar.getInstance(latviaTZ).apply { time = firstTime ?: now.time }
                    for (i in 0..totalHours) {
                        val x = i * hourWidth
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )

                        val hourLabel = hourFormatter.format(calendar.time)
                        drawContext.canvas.nativeCanvas.drawText(
                            hourLabel,
                            x + 6f,
                            h + 36f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 26f
                                isAntiAlias = true
                            }
                        )
                        calendar.add(Calendar.HOUR_OF_DAY, 1)
                    }

                    // === Highlight current hour with translucent bar ===
                    val baseHour =
                        firstTime?.let { (now.time.time - it.time) / (1000 * 60 * 60f) } ?: 0f
                    val barCenterX = baseHour * hourWidth
                    val barWidth = hourWidth * 0.8f
                    val barLeft = barCenterX - barWidth / 2f
                    val barRight = barCenterX + barWidth / 2f

                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        topLeft = Offset(barLeft, 0f),
                        size = androidx.compose.ui.geometry.Size(barWidth, h)
                    )

                    // === Step-style price line ===
                    for (i in 0 until prices.size - 1) {
                        val p1 = prices[i]
                        val p2 = prices[i + 1]
                        val y1 = priceToY(p1.price)
                        val y2 = priceToY(p2.price)
                        val x1 =
                            ((sdf.parse(p1.start)?.time ?: 0) - (firstTime?.time ?: 0)) / (1000 * 60 * 60f) * hourWidth
                        val x2 =
                            ((sdf.parse(p2.start)?.time ?: 0) - (firstTime?.time ?: 0)) / (1000 * 60 * 60f) * hourWidth
                        val color = getColorForPrice(p1.price)
                        drawLine(color, Offset(x1, y1), Offset(x2, y1), 4f, StrokeCap.Butt)
                        drawLine(color, Offset(x2, y1), Offset(x2, y2), 4f, StrokeCap.Butt)
                    }
                }
            }
        }

        // === Footer ===
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
