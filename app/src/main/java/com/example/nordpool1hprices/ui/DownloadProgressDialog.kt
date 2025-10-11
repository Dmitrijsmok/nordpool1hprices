package com.example.nordpool1hprices.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadProgressDialog(progress: Int) {
    AlertDialog(
        onDismissRequest = { /* Block dismissal while downloading */ },
        title = { Text("Downloading update...") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "$progress% completed")
            }
        },
        confirmButton = {}
    )
}
