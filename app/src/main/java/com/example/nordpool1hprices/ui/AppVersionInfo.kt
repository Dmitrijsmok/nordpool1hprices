package com.example.nordpool1hprices.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import android.content.pm.PackageManager

@Composable
fun AppVersionInfo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }

    // Small, grey label in the bottom-right corner
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Text(
            text = "v$versionName",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier
        )
    }
}
