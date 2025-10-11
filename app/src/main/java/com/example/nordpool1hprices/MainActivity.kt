package com.example.nordpool1hprices

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import com.example.nordpool1hprices.network.UpdateRepository
import com.example.nordpool1hprices.network.UpdateInfo
import com.example.nordpool1hprices.ui.UpdateDialog
import com.example.nordpool1hprices.utils.ApkDownloader
import com.example.nordpool1hprices.ui.DownloadProgressDialog
import com.example.nordpool1hprices.utils.ApkDownloader.isDownloading
import com.example.nordpool1hprices.utils.ApkDownloader.downloadProgress
import com.example.nordpool1hprices.BuildConfig
fun parseFlexibleDate(raw: String): Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",   // ISO 8601 with timezone
        "yyyy-MM-dd'T'HH:mm:ss",      // ISO 8601 without timezone
        "yyyy-MM-dd HH:mm:ss",        // Space separated
        "yyyy-MM-dd HH:mm"            // Just in case (short form)
    )

    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return sdf.parse(raw)
        } catch (_: Exception) { }
    }
    return null
}

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
    val currentVersion = BuildConfig.VERSION_NAME
    // ✅ Declare your prices list here
    var prices by remember { mutableStateOf<List<PriceEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // ✅ Check for updates once when app starts
    LaunchedEffect(Unit) {

        // ⚠️ Use the link to your update.json, NOT the .apk
        val remoteUrl =
            "https://gitlab.com/dmitrijsmok1/nordpool1hprices-updates/-/raw/main/update.json"
        val info = UpdateRepository.checkForUpdate(remoteUrl)
        if (info != null && info.latestVersion != currentVersion) {
            updateInfo = info
            showUpdateDialog = true
        }
    }

    // ✅ Show update dialog when new version available
    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            version = updateInfo!!.latestVersion,
            changelog = updateInfo!!.changelog,
            onConfirm = {
                showUpdateDialog = false
                ApkDownloader.downloadAndInstall(context, updateInfo!!.apkUrl)
            },
            onDismiss = { showUpdateDialog = false }
        )
    }
// ✅ Show download progress UI
    if (isDownloading) {
        DownloadProgressDialog(progress = downloadProgress)
    }

    // ✅ Fetch prices
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                Log.d("NordpoolApp", "⏳ Fetching hourly prices...")
                prices = PriceRepository.getHourlyPrices()
                Log.d("NordpoolApp", "✅ Received ${prices.size} entries")

                if (prices.isEmpty()) {
                    Toast.makeText(context, "No data from API!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("NordpoolApp", "❌ Failed to fetch prices", e)
                Toast.makeText(context, "Error fetching prices: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loading = false
            }
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
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val nowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time

            val futurePrices = prices.filter { entry ->
                val startDate = parseFlexibleDate(entry.start)
                startDate?.after(nowUtc) == true
            }.sortedBy { parseFlexibleDate(it.start) }

            Log.d("NordpoolApp", "✅ After filter: ${futurePrices.size} entries remain")

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
// ✅ Version label pinned to bottom-right
                Text(
                    text = "v$currentVersion",
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
