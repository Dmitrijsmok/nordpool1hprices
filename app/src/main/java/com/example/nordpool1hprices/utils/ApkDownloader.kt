package com.example.nordpool1hprices.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.coroutineContext

object ApkDownloader {

    private const val TAG = "ApkDownloader"
    private var downloadId: Long = -1L
    private var progressJob: Job? = null

    var isDownloading = false
    var downloadProgress = 0

    // === MAIN ENTRY ===
    fun downloadAndInstall(activity: Activity, apkUrl: String) {
        try {
            val fileName = apkUrl.substringAfterLast("/")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            // 🧹 Cleanup duplicates before new download
            cleanupOldApks(downloadsDir, fileName)

            val targetFile = File(downloadsDir, fileName)

            // 🔹 Prepare request
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("NordPool update")
                .setDescription("Downloading version…")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(targetFile))

            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)
            isDownloading = true
            downloadProgress = 0

            Log.d(TAG, "⬇️ Download started → $apkUrl → ${targetFile.absolutePath}")
            Toast.makeText(activity, "Downloading update…", Toast.LENGTH_SHORT).show()

            // Track progress in background
            progressJob = CoroutineScope(Dispatchers.IO).launch {
                trackProgress(dm, downloadId)
            }

            // Register receiver for completion
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            activity.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        Log.d(TAG, "📦 Download complete (ID=$id)")
                        progressJob?.cancel()
                        isDownloading = false
                        downloadProgress = 100

                        val cursor = dm.query(DownloadManager.Query().setFilterById(id))
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            Log.d(TAG, "📊 Status=$status, reason=$reason")
                            cursor.close()

                            if (status == DownloadManager.STATUS_SUCCESSFUL && targetFile.exists()) {
                                installApk(activity, targetFile)
                            } else {
                                Log.e(TAG, "❌ Download failed (status=$status, reason=$reason)")
                                Toast.makeText(activity, "Download failed (reason=$reason)", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e(TAG, "❌ Query failed — no cursor entry for ID=$id")
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                activity.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Download failed: ${e.message}", e)
            isDownloading = false
            Toast.makeText(activity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // === PROGRESS TRACKER ===
    private suspend fun trackProgress(dm: DownloadManager, id: Long) {
        val query = DownloadManager.Query().setFilterById(id)
        var running = true
        while (running && coroutineContext.isActive) {
            delay(800)
            val cursor = dm.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (bytesTotal > 0) {
                    val progress = (bytesDownloaded * 100 / bytesTotal)
                    if (progress != downloadProgress) {
                        downloadProgress = progress
                        Log.d(TAG, "⬇️ Progress: $downloadProgress%")
                    }
                }
                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    running = false
                }
                cursor.close()
            } else {
                running = false
            }
        }
        Log.d(TAG, "📈 Progress tracking stopped.")
    }

    // === INSTALL HANDLER ===
    private fun installApk(activity: Activity, file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = activity.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    Toast.makeText(activity, "Please allow installs from this app in Settings.", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                    activity.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
            Log.d(TAG, "📎 Install URI: $apkUri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Toast.makeText(activity, "Installing update…", Toast.LENGTH_SHORT).show()
            activity.startActivity(intent)
            Log.d(TAG, "✅ Installation intent launched.")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Install failed: ${e.message}", e)
            Toast.makeText(activity, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // === CLEANUP OLD APKs ===
    private fun cleanupOldApks(dir: File?, baseName: String) {
        dir?.listFiles()?.forEach { file ->
            if (file.name.contains("nordPool1hPrices", ignoreCase = true) && file.name.endsWith(".apk")) {
                file.delete()
                Log.d(TAG, "🧹 Deleted old APK: ${file.name}")
            }
        }
    }
}
