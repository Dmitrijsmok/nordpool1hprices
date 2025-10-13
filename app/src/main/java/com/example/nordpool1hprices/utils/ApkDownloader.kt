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

    // 🔹 MAIN ENTRY POINT
    fun checkAndDownloadUpdate(activity: Activity, apkUrl: String) {
        if (isUpdateAvailable(activity, apkUrl)) {
            Log.d(TAG, "⬇️ Update available, starting download.")
            downloadAndInstall(activity, apkUrl)
        } else {
            Log.d(TAG, "✅ Already up to date. No download needed.")
            Toast.makeText(activity, "App is already up to date.", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔹 VERSION COMPARISON LOGIC
    private fun isUpdateAvailable(context: Context, remoteApkUrl: String): Boolean {
        val remoteVersion = Regex("v(\\d+(?:\\.\\d+)*)").find(remoteApkUrl)?.groupValues?.get(1)
        if (remoteVersion.isNullOrEmpty()) {
            Log.e(TAG, "❌ Failed to parse version from URL: $remoteApkUrl")
            return false
        }

        val currentVersion = context.packageManager
            .getPackageInfo(context.packageName, 0).versionName

        val result = compareVersions(remoteVersion, currentVersion)
        Log.d(TAG, "🔍 Current=$currentVersion, Remote=$remoteVersion, Result=$result")

        return result > 0
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".")
        val parts2 = v2.split(".")
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val p2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    // 🔹 DOWNLOAD AND INSTALL
    fun downloadAndInstall(activity: Activity, apkUrl: String) {
        try {
            val baseName = apkUrl.substringAfterLast("/")
            val downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadsDir, baseName)

            cleanupOldApks(downloadsDir, baseName)

            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Downloading update")
                .setDescription("Fetching latest version…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(targetFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)
            isDownloading = true
            downloadProgress = 0

            Log.d(TAG, "✅ Started download: id=$downloadId → ${targetFile.absolutePath}")
            Toast.makeText(activity, "Downloading update…", Toast.LENGTH_SHORT).show()

            progressJob = CoroutineScope(Dispatchers.IO).launch {
                trackProgress(dm, downloadId)
            }

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        Log.d(TAG, "📦 Download complete: $id")
                        try {
                            activity.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        progressJob?.cancel()
                        isDownloading = false
                        downloadProgress = 100

                        if (targetFile.exists()) {
                            Log.d(TAG, "📁 File ready for install: ${targetFile.absolutePath}")
                            installApk(activity, targetFile)
                        } else {
                            Log.e(TAG, "❌ File missing: ${targetFile.absolutePath}")
                            Toast.makeText(activity, "Download failed: file missing.", Toast.LENGTH_LONG).show()
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
            e.printStackTrace()
            isDownloading = false
            Toast.makeText(activity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 🔹 PROGRESS TRACKER
    private suspend fun trackProgress(dm: DownloadManager, id: Long) {
        val query = DownloadManager.Query().setFilterById(id)
        var running = true
        while (running && coroutineContext.isActive) {
            delay(700)
            val cursor = dm.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if (bytesTotal > 0) {
                    downloadProgress = (bytesDownloaded * 100 / bytesTotal)
                    Log.d(TAG, "⬇️ Progress: $downloadProgress%")
                }
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    running = false
                }
                cursor.close()
            }
        }
        Log.d(TAG, "📈 Progress tracking stopped.")
    }

    // 🔹 INSTALLATION HANDLER
    private fun installApk(activity: Activity, file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = activity.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    Toast.makeText(activity, "Please allow app installs in settings.", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                    activity.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
            Log.d(TAG, "📎 URI obtained: $apkUri")
            Toast.makeText(activity, "Installing update…", Toast.LENGTH_SHORT).show()

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(intent)
            Log.d(TAG, "✅ Installation intent launched.")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Install failed: ${e.message}", e)
            Toast.makeText(activity, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 🔹 CLEANUP OLD APKs
    private fun cleanupOldApks(dir: File?, baseName: String) {
        dir?.listFiles()?.forEach { file ->
            if (file.name.startsWith(baseName.removeSuffix(".apk").substringBefore("-")) && file.name.endsWith(".apk")) {
                Log.d(TAG, "🧹 Deleting old APK: ${file.name}")
                file.delete()
            }
        }
    }
}
