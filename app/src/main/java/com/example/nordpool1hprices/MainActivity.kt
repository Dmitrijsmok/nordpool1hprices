package com.example.nordpool1hprices

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nordpool1hprices.model.PriceEntry
import com.example.nordpool1hprices.network.PriceRepository
import com.example.nordpool1hprices.ui.PriceChart
import com.example.nordpool1hprices.ui.PriceList
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        setContent {
            NordpoolApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NordpoolApp() {
    var prices by remember { mutableStateOf<List<PriceEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Fetch prices
    LaunchedEffect(Unit) {
        scope.launch {
            prices = PriceRepository.getHourlyPrices()
            loading = false
        }
    }

    Scaffold(
        topBar = {
            // ✅ Centered gradient title bar
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2E7D32).copy(alpha = 0.8f), // dark green
                                        Color(0xFF66BB6A).copy(alpha = 0.8f)  // light green
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nord Pool – 1h Prices",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val now = Date()

            val futurePrices = prices.filter { entry ->
                val startDate = runCatching { sdf.parse(entry.start) }.getOrNull()
                startDate?.let { !it.before(now) } ?: false
            }.sortedBy { entry ->
                runCatching { sdf.parse(entry.start) }.getOrNull() ?: Date(Long.MAX_VALUE)
            }

            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    PriceChart(futurePrices)
                    Spacer(modifier = Modifier.height(16.dp))
                    PriceList(futurePrices)
                }

                // ✅ Version label pinned to bottom-right
                Text(
                    text = "v1.4",
                    color = Color.Gray.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 6.dp)
                )
            }
        }
    }
}
