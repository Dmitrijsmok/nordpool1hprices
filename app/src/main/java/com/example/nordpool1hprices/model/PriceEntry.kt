package com.example.nordpool1hprices.model

data class PriceEntry(
    val start: String,
    val end: String,
    val price: Double,
    var notify: Boolean = false // 👈 New flag to track notification toggle in UI
)
