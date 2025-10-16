package com.example.nordpool1hprices.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.prefDataStore get() = this.dataStore

object NotificationPreferences {
    private val ACTIVE_KEYS = stringSetPreferencesKey("active_notification_keys")

    // Observe active notification keys as Long epoch millis; filters out expired
    fun observe(context: Context): Flow<Set<Long>> =
        context.prefDataStore.data.map { prefs ->
            val set = prefs[ACTIVE_KEYS] ?: emptySet()
            val now = System.currentTimeMillis()
            set.mapNotNull { it.toLongOrNull() }
                .filter { it >= now }
                .toSet()
        }

    suspend fun add(context: Context, startMillis: Long) {
        context.prefDataStore.edit { prefs ->
            val set = prefs[ACTIVE_KEYS]?.toMutableSet() ?: mutableSetOf()
            set.add(startMillis.toString())
            prefs[ACTIVE_KEYS] = set
        }
    }

    suspend fun remove(context: Context, startMillis: Long) {
        context.prefDataStore.edit { prefs ->
            val set = prefs[ACTIVE_KEYS]?.toMutableSet() ?: mutableSetOf()
            set.remove(startMillis.toString())
            prefs[ACTIVE_KEYS] = set
        }
    }

    suspend fun prune(context: Context) {
        context.prefDataStore.edit { prefs ->
            val set = prefs[ACTIVE_KEYS]?.toMutableSet() ?: mutableSetOf()
            if (set.isEmpty()) return@edit
            val now = System.currentTimeMillis()
            val cleaned = set.mapNotNull { it.toLongOrNull() }
                .filter { it >= now }
                .map { it.toString() }
                .toSet()
            prefs[ACTIVE_KEYS] = cleaned
        }
    }
}
