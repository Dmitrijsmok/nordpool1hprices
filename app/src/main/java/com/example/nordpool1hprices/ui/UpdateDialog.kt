package com.example.nordpool1hprices.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun UpdateDialog(
    version: String,
    changelog: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .padding(20.dp)
            ) {
                Text(
                    text = "Update Available (v$version)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text(
                    text = changelog,
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Later") }
                    TextButton(onClick = onConfirm) { Text("Update") }
                }
            }
        }
    }
}
