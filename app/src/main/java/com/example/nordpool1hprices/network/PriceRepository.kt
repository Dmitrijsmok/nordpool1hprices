package com.example.nordpool1hprices.network

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.example.nordpool1hprices.model.PriceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader

object PriceRepository {

    private val client = OkHttpClient()

    suspend fun getHourlyPrices(): List<PriceEntry> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://nordpool.didnt.work/nordpool-lv-1h.csv")
            .build()

        val response = client.newCall(request).execute()
        val csvData = response.body?.string() ?: return@withContext emptyList()

        val rows = csvReader().readAllWithHeader(csvData.byteInputStream())

        return@withContext rows.mapNotNull { row: Map<String, String> ->
            val start = row["ts_start"] ?: return@mapNotNull null
            val end = row["ts_end"] ?: return@mapNotNull null
            val price = row["price"]?.toDoubleOrNull() ?: return@mapNotNull null
            PriceEntry(start = start, end = end, price = price)
    }
    }
}
