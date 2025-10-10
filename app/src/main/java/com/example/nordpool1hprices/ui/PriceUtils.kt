package com.example.nordpool1hprices.ui

import androidx.compose.ui.graphics.Color

fun getColorForPrice(price: Double): Color {
    return when {
        price < 0.10 -> Color(0xFF2E7D32) // Dark Green
        price < 0.15 -> Color(0xFF66BB6A) // Light Green
        price < 0.20 -> Color(0xFFFFA000) // Orange
        else -> Color(0xFFD32F2F)         // Red
    }
}
