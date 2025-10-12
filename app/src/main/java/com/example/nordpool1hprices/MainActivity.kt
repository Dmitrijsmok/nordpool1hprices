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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material.icons.filled.Menu
import com.example.nordpool1hprices.model.PriceEntry
import com.example.nordpool1hprices.network.PriceRepository
import com.example.nordpool1hprices.network.UpdateInfo
import com.example.nordpool1hprices.network.UpdateRepository
import com.example.nordpool1hprices.ui.*
import com.example.nordpool1hprices.utils.ApkDownloader
import com.example.nordpool1hprices.utils.ApkDownloader.downloadProgress
import com.example.nordpool1hprices.utils.ApkDownloader.isDownloading
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
    val scope = rememberCoroutineScope()

    // === State ===
    var prices by remember { mutableStateOf<List<PriceEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    // === Remote update check ===
    LaunchedEffect(Unit) {
        val remoteUrl =
            "https://gitlab.com/dmitrijsmok1/nordpool1hprices-updates/-/raw/main/update.json"
        val info = UpdateRepository.checkForUpdate(remoteUrl)
        if (info != null && info.latestVersion != currentVersion) {
            updateInfo = info
            showUpdateDialog = true
        }
    }

    // === Update dialog ===
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

    // === Download progress dialog ===
    if (isDownloading) {
        DownloadProgressDialog(progress = downloadProgress)
    }

    // === Fetch prices ===
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
                Toast.makeText(context, "Error fetching prices: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            } finally {
                loading = false
            }
        }
    }

    // === About screen ===
    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
        return
    }

    // === Main UI ===
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
                            imageVector = Icons.Default.Menu, // ☰ three stripes
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
                startDate?.after(nowUtc) == true
            }.sortedBy { parseFlexibleDate(it.start) }

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
                    // === Chart ===
                    PriceChart(futurePrices)

                    // === Resolution toggle placeholder ===
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 12.dp),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        Text(text = "Resolution:", color = Color.Gray, fontSize = 13.sp)
//                        Button(
//                            onClick = { /* TODO: toggle between 1h / 15min */ },
//                            modifier = Modifier.padding(start = 8.dp),
//                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))
//                        ) {
//                            Text("1h / 15min")
//                        }
//                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    PriceList(futurePrices)
                }

                // === Version label ===
//                Text(
//                    text = "v$currentVersion",
//                    color = Color.Gray.copy(alpha = 0.8f),
//                    fontSize = 12.sp,
//                    modifier = Modifier
//                        .align(Alignment.BottomEnd)
//                        .padding(end = 10.dp, bottom = 6.dp)
//                )
            }
        }
    }
}

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
