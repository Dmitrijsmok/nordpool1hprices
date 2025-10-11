package com.example.nordpool1hprices.utils

import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.File

object ApkDownloader {

    // 🧭 UI state
    var downloadProgress by mutableStateOf(0)
    var isDownloading by mutableStateOf(false)

    private var downloadId: Long = -1L
    private var receiver: BroadcastReceiver? = null

    fun downloadAndInstall(context: Context, apkUrl: String) {
        try {
            val fileName = apkUrl.substringAfterLast("/")
            val destinationFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )

            // Remove old file
            if (destinationFile.exists()) destinationFile.delete()

            // 🔽 Setup DownloadManager request
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Downloading update")
                .setDescription("Fetching the latest version…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)

            Toast.makeText(context, "Update download started…", Toast.LENGTH_SHORT).show()
            isDownloading = true
            downloadProgress = 0

            // 🧩 BroadcastReceiver for completion
            receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        isDownloading = false
                        Toast.makeText(ctx, "Download complete. Installing…", Toast.LENGTH_SHORT).show()
                        ctx?.let { installApk(it, destinationFile) }

                        try {
                            ctx?.unregisterReceiver(this)
                            receiver = null
                        } catch (_: Exception) {}
                    }
                }
            }

            // ✅ Register receiver safely for API 24–34+
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            // 🔁 Coroutine: track download progress
            CoroutineScope(Dispatchers.IO).launch {
                var downloading = true
                while (downloading) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor: Cursor = dm.query(query)
                    if (cursor.moveToFirst()) {
                        val bytesDownloaded = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )
                        val bytesTotal = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )

                        if (bytesTotal > 0) {
                            downloadProgress = (bytesDownloaded * 100L / bytesTotal).toInt()
                        }

                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )
                        if (status == DownloadManager.STATUS_SUCCESSFUL ||
                            status == DownloadManager.STATUS_FAILED
                        ) {
                            downloading = false
                        }
                    }
                    cursor.close()
                    delay(800)
                }
            }

        } catch (e: Exception) {
            isDownloading = false
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // ✅ Secure installation flow for API 24–34+
    private fun installApk(context: Context, apkFile: File) {
        try {
            // 🔒 Android 8+ requires permission to install from unknown sources
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Please allow app installations", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // 📦 Use FileProvider to serve APK URI
            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)

        } catch (e: Exception) {
            Toast.makeText(context, "Installation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
