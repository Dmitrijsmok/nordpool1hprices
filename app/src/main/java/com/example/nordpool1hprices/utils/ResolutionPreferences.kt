package com.example.nordpool1hprices.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object ResolutionPreferences {
    private val RESOLUTION_KEY = stringPreferencesKey("price_resolution")

    fun getResolution(context: Context): Flow<String> =
        context.dataStore.data.map { prefs -> prefs[RESOLUTION_KEY] ?: "1h" }

    suspend fun saveResolution(context: Context, value: String) {
        context.dataStore.edit { prefs -> prefs[RESOLUTION_KEY] = value }
    }
}
