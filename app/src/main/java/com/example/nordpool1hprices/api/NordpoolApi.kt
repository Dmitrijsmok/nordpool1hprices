// NordpoolAPI.kt
package com.example.nordpool1hprices

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NordpoolAPI {

    companion object {
        private const val TAG = "NordpoolAPI"

        // ✅ AUTENTIFIKĀCIJAS DATI (no Nordpool piemēra)
        private const val TOKEN_URL = "https://sts.test.nordpoolgroup.com/connect/token" // TEST vide
        // private const val TOKEN_URL = "https://sts.nordpoolgroup.com/connect/token" // PRODUCTION vide
        private const val BASE64_CREDENTIALS = "Y2xpZW50X2F1Y3Rpb25fYXBpOmNsaWVudF9hdWN0aW9uX2FwaQ=="

        // ✅ API ENDPOINTS
        private const val AUCTION_API_BASE = "https://api.test.nordpoolgroup.com/auction/v1" // TEST
        // private const val AUCTION_API_BASE = "https://api.nordpoolgroup.com/auction/v1" // PRODUCTION
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

    suspend fun getDayAheadPrices(): ApiResult {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "🚀 Sākam datu iegūšanu ar autentifikāciju...")

                // 1. Iegūstam Access Token (ar pareizajiem parametriem)
                val token = getAccessToken()
                if (token == null) {
                    Log.e(TAG, "❌ Nevar iegūt access token")
                    return@withContext ApiResult.Error("Autentifikācijas kļūda")
                }

                Log.d(TAG, "✅ Token iegūts!")

                // 2. Izmantojam token, lai iegūtu tirgus datus
                getAuctionDataWithToken(token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 API kļūda: ${e.message}")
            ApiResult.Error("API kļūda: ${e.message}")
        }
    }

    private suspend fun getAccessToken(): String? {
        // Pārbaudām vai token jau ir derīgs
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            Log.d(TAG, "♻️ Izmantojam esošo token")
            return accessToken
        }

        return try {
            Log.d(TAG, "🔑 Iegūstam jaunu access token...")

            // ✅ PAREIZIE PARAMETRI (no Nordpool piemēra)
            val requestBody = "grant_type=password&scope=auction_api"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(requestBody)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", "Basic $BASE64_CREDENTIALS")
                .build()

            Log.d(TAG, "📤 Token request uz: $TOKEN_URL")

            val response = client.newCall(request).execute()
            Log.d(TAG, "📥 Token atbilde: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "✅ Token response: ${responseBody?.take(100)}...")

                val json = JSONObject(responseBody)
                val token = json.getString("access_token")
                val expiresIn = json.getInt("expires_in")

                // Saglabājam token un tā derīguma termiņu
                accessToken = token
                tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000 // -1 minūte drošībai

                Log.d(TAG, "🔑 Token iegūts, derīgs ${expiresIn} sekundes")
                token
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "❌ Token kļūda ${response.code}: $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Token iegūšanas kļūda: ${e.message}")
            null
        }
    }

    private suspend fun getAuctionDataWithToken(token: String): ApiResult {
        return try {
            Log.d(TAG, "📊 Iegūstam tirgus datus...")

            // ✅ AUCTION API ENDPOINTS
            val endpoints = listOf(
                "$AUCTION_API_BASE/prices/dayahead",
                "$AUCTION_API_BASE/marketdata/dayahead",
                "$AUCTION_API_BASE/prices"
            )

            for (endpoint in endpoints) {
                Log.d(TAG, "🔐 Mēģinām: $endpoint")
                val result = makeAuthenticatedRequest(endpoint, token)
                if (result is ApiResult.Success) {
                    Log.d(TAG, "✅ Dati iegūti no: $endpoint")
                    return result
                }
            }

            // Ja neviens endpoints nestrādā
            Log.e(TAG, "❌ Visi autentificētie endpoints nedarbojas")
            ApiResult.Error("Nevar pieslēgties Auction API")

        } catch (e: Exception) {
            Log.e(TAG, "💥 Auction API kļūda: ${e.message}")
            ApiResult.Error("Auction API kļūda: ${e.message}")
        }
    }

    private suspend fun makeAuthenticatedRequest(url: String, token: String): ApiResult {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "📡 API izsaukums: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    Log.d(TAG, "✅ Dati saņemti, parsējam...")
                    parseAuctionData(responseBody)
                } else {
                    ApiResult.Error("Tukša atbilde")
                }
            } else {
                Log.d(TAG, "❌ API kļūda ${response.code} priekš: $url")
                ApiResult.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            ApiResult.Error("Savienojuma kļūda: ${e.message}")
        }
    }

    private fun parseAuctionData(jsonString: String): ApiResult {
        return try {
            Log.d(TAG, "🔍 Parsējam Auction API datus...")
            val json = JSONObject(jsonString)
            val prices = mutableListOf<PriceData>()

            Log.d(TAG, "📋 API atbildes struktūra: ${json.toString().take(500)}...")

            // MĒĢINĀM DAŽĀDAS DATU STRUKTŪRAS
            if (json.has("data")) {
                val dataArray = json.getJSONArray("data")
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    prices.add(PriceData(
                        area = item.optString("area", "LV"),
                        price = "%.2f".format(item.optDouble("price", 0.0)),
                        timeInterval = item.optString("time", "")
                    ))
                }
            }
            else if (json.has("prices")) {
                val pricesArray = json.getJSONArray("prices")
                for (i in 0 until pricesArray.length()) {
                    val item = pricesArray.getJSONObject(i)
                    prices.add(PriceData(
                        area = item.optString("region", "Unknown"),
                        price = "%.2f".format(item.optDouble("value", 0.0)),
                        timeInterval = item.optString("interval", "")
                    ))
                }
            }
            else if (json.has("Results")) {
                val resultsArray = json.getJSONArray("Results")
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    prices.add(PriceData(
                        area = item.optString("Area", "LV"),
                        price = "%.2f".format(item.optDouble("Price", 0.0)),
                        timeInterval = item.optString("Time", "")
                    ))
                }
            }
            else {
                // Ja struktūra nav atpazīta, atgriežam kļūdu
                Log.e(TAG, "❌ Neatpazīta datu struktūra")
                return ApiResult.Error("Neatpazīta API atbildes struktūra")
            }

            if (prices.isNotEmpty()) {
                Log.d(TAG, "✅ Parsēti ${prices.size} cenu ieraksti")
                ApiResult.Success(prices)
            } else {
                Log.e(TAG, "❌ Nav atrasti cenu dati")
                ApiResult.Error("Nav cenu datu")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Datu parsēšanas kļūda: ${e.message}")
            ApiResult.Error("Datu apstrādes kļūda")
        }
    }
}

// Datu klases paliek nemainīgas
data class PriceData(
    val area: String,
    val price: String,
    val timeInterval: String
)

sealed class ApiResult {
    data class Success(val prices: List<PriceData>) : ApiResult()
    data class Error(val message: String) : ApiResult()
}