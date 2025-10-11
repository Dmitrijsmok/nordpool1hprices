package com.example.nordpool1hprices.utils

import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.io.File

object ApkDownloader {

    // Track progress
    var downloadProgress by mutableStateOf(0)
    var isDownloading by mutableStateOf(false)

    private var downloadId: Long = -1

    fun downloadAndInstall(context: Context, apkUrl: String) {
        try {
            val fileName = apkUrl.substringAfterLast("/")
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Downloading update")
                .setDescription("Fetching the latest version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)

            Toast.makeText(context, "Update download started...", Toast.LENGTH_SHORT).show()
            isDownloading = true
            downloadProgress = 0

            // Register listener for completion
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        isDownloading = false
                        Toast.makeText(ctx, "Download complete. Installing...", Toast.LENGTH_SHORT).show()
                        installApk(ctx!!, fileName)
                        ctx.unregisterReceiver(this)
                    }
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            // Coroutine to check progress
            CoroutineScope(Dispatchers.IO).launch {
                var downloading = true
                while (downloading) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor: Cursor = dm.query(query)
                    if (cursor.moveToFirst()) {
                        val bytesDownloaded =
                            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal =
                            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        if (bytesTotal > 0) {
                            val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
                            downloadProgress = progress
                        }

                        val status =
                            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                        }
                    }
                    cursor.close()
                    delay(800)
                }
            }

        } catch (e: Exception) {
            isDownloading = false
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun installApk(context: Context, fileName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        val uri = Uri.fromFile(file)

        val installIntent = Intent(Intent.ACTION_VIEW)
        installIntent.setDataAndType(uri, "application/vnd.android.package-archive")
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(installIntent)
    }
}
