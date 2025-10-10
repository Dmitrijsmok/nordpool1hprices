package com.example.nordpool1hprices.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private const val API_DATE_FORMAT = "yyyy-MM-dd"
    private const val DISPLAY_DATE_FORMAT = "dd.MM.yyyy"
    private const val DISPLAY_TIME_FORMAT = "HH:mm"

    fun formatDateForApi(date: Date): String {
        return SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault()).format(date)
    }

    fun formatDateForDisplay(date: Date): String {
        return SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault()).format(date)
    }

    fun formatTimeForDisplay(date: Date): String {
        return SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.getDefault()).format(date)
    }

    fun parseApiTimestamp(timestamp: String): Date {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(timestamp) ?: Date()
    }

    fun getTodayDate(): String {
        return formatDateForApi(Date())
    }

    fun getPreviousDay(date: String): String {
        val sdf = SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault())
        val currentDate = sdf.parse(date) ?: return date
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return sdf.format(calendar.time)
    }

    fun getNextDay(date: String): String {
        val sdf = SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault())
        val currentDate = sdf.parse(date) ?: return date
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return sdf.format(calendar.time)
    }
}