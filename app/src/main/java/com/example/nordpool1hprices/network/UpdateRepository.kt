package com.example.nordpool1hprices.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateRepository {
    suspend fun checkForUpdate(remoteUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(remoteUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(jsonText)

                val latestVersion = json.getString("latestVersion")
                val changelog = json.optString("changelog", "")
                val apkUrl = json.getString("apkUrl")

                UpdateInfo(latestVersion, changelog, apkUrl)
            } else {
                Log.e("UpdateRepository", "Error: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("UpdateRepository", "Update check failed", e)
            null
        }
    }
}

data class UpdateInfo(
    val latestVersion: String,
    val changelog: String,
    val apkUrl: String
)
