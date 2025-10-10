package com.example.nordpool1hprices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nordpool1hprices.model.PriceEntry
import com.example.nordpool1hprices.network.PriceRepository
import com.example.nordpool1hprices.ui.PriceChart
import com.example.nordpool1hprices.ui.PriceList
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    LaunchedEffect(Unit) {
        scope.launch {
            prices = PriceRepository.getHourlyPrices()
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nord Pool – 1h Prices") })
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
            // Use SimpleDateFormat to parse and compare dates
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val now = Date()

            // Filter out past hours (before this hour)
            val futurePrices = prices.filter { entry ->
                val startDate = runCatching { sdf.parse(entry.start) }.getOrNull()
                if (startDate != null) {
                    // Compare startDate to now, include startDate >= now
                    !startDate.before(now)
                } else {
                    false
                }
            }.sortedBy { entry ->
                runCatching { sdf.parse(entry.start) }.getOrNull() ?: Date(Long.MAX_VALUE)
            }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                PriceChart(futurePrices)
                Spacer(modifier = Modifier.height(16.dp))
                PriceList(futurePrices)
            }
        }
    }
}
