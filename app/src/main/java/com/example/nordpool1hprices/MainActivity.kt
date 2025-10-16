package com.example.nordpool1hprices

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nordpool1hprices.model.PriceEntry
import com.example.nordpool1hprices.network.PriceRepository
import com.example.nordpool1hprices.network.UpdateInfo
import com.example.nordpool1hprices.network.UpdateRepository
import com.example.nordpool1hprices.ui.*
import com.example.nordpool1hprices.utils.ApkDownloader
import com.example.nordpool1hprices.utils.ApkDownloader.downloadProgress
import com.example.nordpool1hprices.utils.ApkDownloader.isDownloading
import com.example.nordpool1hprices.BuildConfig
import com.example.nordpool1hprices.utils.NotificationPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // ✅ Always create the notification channel (ensures app appears in notification list)
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "nordpool_channel",
                "Nord Pool Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hourly price notifications for Nord Pool"
            }
            manager.createNotificationChannel(channel)
        }

        // Startup notification removed to avoid user disruption

        setContent {
            NordpoolApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NordpoolApp() {
    val currentVersion = BuildConfig.VERSION_NAME
    val context = LocalContext.current
    // val scope = rememberCoroutineScope() // not needed for update check anymore

    var prices by remember { mutableStateOf<List<PriceEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    // ✅ Prune expired persisted bell states on start
    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                NotificationPreferences.prune(context)
            }
        } catch (_: Exception) { }
    }

    // ✅ Check for updates (suspend on IO inside repository, update state on main)
    LaunchedEffect(Unit) {
        try {
            val remoteUrl =
                "https://gitlab.com/dmitrijsmok1/nordpool1hprices-updates/-/raw/main/update.json"
            val info = UpdateRepository.checkForUpdate(remoteUrl)
            if (info != null && info.latestVersion != currentVersion) {
                updateInfo = info
                showUpdateDialog = true
            } else {
                Log.d("UpdateCheck", "✅ App is up to date.")
            }
        } catch (e: Exception) {
            Log.e("UpdateCheck", "⚠️ Failed to check update: ${e.message}")
        }
    }

    // ✅ Load hourly prices
    LaunchedEffect(Unit) {
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
            Log.d("NordpoolApp", "🧩 Loading set to false")
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            version = updateInfo!!.latestVersion,
            changelog = updateInfo!!.changelog,
            onConfirm = {
                showUpdateDialog = false
                val activity = context as? Activity
                if (activity != null) {
                    ApkDownloader.downloadAndInstall(activity, updateInfo!!.apkUrl)
                } else {
                    Toast.makeText(
                        context,
                        "Update failed: unable to access activity context",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (isDownloading) {
        DownloadProgressDialog(progress = downloadProgress)
    }

    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2E7D32).copy(alpha = 0.8f),
                                        Color(0xFF66BB6A).copy(alpha = 0.8f)
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
                navigationIcon = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "About / Menu",
                            tint = Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
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
            val nowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
            val futurePrices = prices.filter { entry ->
                val startDate = parseFlexibleDate(entry.start)
                startDate != null && !startDate.before(nowUtc)
            }.sortedBy { parseFlexibleDate(it.start) }

            Log.d("NordpoolApp", "🧩 Displaying ${futurePrices.size} entries")

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

// === Flexible date parser ===
fun parseFlexibleDate(raw: String): Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm"
    )
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return sdf.parse(raw)
        } catch (_: Exception) {
        }
    }
    return null
}
