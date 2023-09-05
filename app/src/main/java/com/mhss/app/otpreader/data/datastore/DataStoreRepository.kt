package com.mhss.app.otpreader.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore(name = "preferences")

class DataStoreRepository(
    private val context: Context
) {

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { settings ->
                if (settings[key] != value)
                    settings[key] = value
            }
        }
    }

    fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { preferences -> preferences[key] ?: defaultValue }
    }

    companion object {
        val PACKAGES = stringSetPreferencesKey("packages")
        val CONTAINS = stringSetPreferencesKey("contains")
    }

}