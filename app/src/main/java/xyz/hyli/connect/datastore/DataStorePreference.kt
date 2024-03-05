package xyz.hyli.connect.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

operator fun <V> Preferences.get(preference: DataStorePreference<V>) = this[preference.key]

open class DataStorePreference<V>(
    private val dataStore: DataStore<Preferences>,
    val key: Preferences.Key<V>,
    open val default: V?
) {

    suspend fun set(block: suspend V?.(Preferences) -> V?): Preferences =
        dataStore.edit { preferences ->
            val value = block(preferences[key] ?: default, preferences)
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }

    suspend fun set(value: V?): Preferences = set { value }

    fun asFlow(fallback: V? = default): Flow<V?> =
        dataStore.data.map { it[key] ?: fallback }

    fun asLiveData(fallback: V? = default): LiveData<V?> = asFlow(fallback).asLiveData()

    suspend fun get(fallback: V? = default): V? = asFlow(fallback).first()

    suspend fun getOrDefault(): V = get() ?: throw IllegalStateException("No default value")

    fun getBlocking(fallback: V? = default): V? = runBlocking { get(fallback) }

    suspend fun reset() = set(default)
}
