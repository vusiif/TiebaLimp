package com.huanchengfly.tieba.post.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Interface representing an owner of DataStore preferences and a coroutine scope for async persistence.
 */
interface DataStorePreferenceOwner {
    val preferencesDataStore: DataStore<Preferences>
    val coroutineScope: CoroutineScope
}

/**
 * Property delegates for DataStore preferences with unified in-memory snapshot caching.
 * Eliminates redundant per-property flow subscriptions and runBlocking disk reads.
 */
object DataStoreDelegates {
    fun int(
        defaultValue: Int = 0,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Int> = object : ReadWriteProperty<DataStorePreferenceOwner, Int> {
        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Int {
            val finalKey = key ?: property.name
            val prefKey = intPreferencesKey(finalKey)
            val snapshot = PreferenceSnapshotCoordinator.getOrLoadSnapshot(
                thisRef.preferencesDataStore,
                thisRef.coroutineScope
            )
            return snapshot[prefKey] ?: defaultValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Int
        ) {
            val finalKey = key ?: property.name
            val prefKey = intPreferencesKey(finalKey)
            PreferenceSnapshotCoordinator.updateSnapshot(thisRef.preferencesDataStore) {
                it[prefKey] = value
            }
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[prefKey] = value
                }
            }
        }
    }

    fun string(
        defaultValue: String? = null,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, String?> = object : ReadWriteProperty<DataStorePreferenceOwner, String?> {
        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): String? {
            val finalKey = key ?: property.name
            val prefKey = stringPreferencesKey(finalKey)
            val snapshot = PreferenceSnapshotCoordinator.getOrLoadSnapshot(
                thisRef.preferencesDataStore,
                thisRef.coroutineScope
            )
            return snapshot[prefKey] ?: defaultValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: String?
        ) {
            val finalKey = key ?: property.name
            val prefKey = stringPreferencesKey(finalKey)
            PreferenceSnapshotCoordinator.updateSnapshot(thisRef.preferencesDataStore) {
                if (value == null) {
                    it.remove(prefKey)
                } else {
                    it[prefKey] = value
                }
            }
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    if (value == null) {
                        it.remove(prefKey)
                    } else {
                        it[prefKey] = value
                    }
                }
            }
        }
    }

    fun float(
        defaultValue: Float = 0F,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Float> = object : ReadWriteProperty<DataStorePreferenceOwner, Float> {
        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Float {
            val finalKey = key ?: property.name
            val prefKey = floatPreferencesKey(finalKey)
            val snapshot = PreferenceSnapshotCoordinator.getOrLoadSnapshot(
                thisRef.preferencesDataStore,
                thisRef.coroutineScope
            )
            return snapshot[prefKey] ?: defaultValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Float
        ) {
            val finalKey = key ?: property.name
            val prefKey = floatPreferencesKey(finalKey)
            PreferenceSnapshotCoordinator.updateSnapshot(thisRef.preferencesDataStore) {
                it[prefKey] = value
            }
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[prefKey] = value
                }
            }
        }
    }

    fun long(
        defaultValue: Long = 0L,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Long> = object : ReadWriteProperty<DataStorePreferenceOwner, Long> {
        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Long {
            val finalKey = key ?: property.name
            val prefKey = longPreferencesKey(finalKey)
            val snapshot = PreferenceSnapshotCoordinator.getOrLoadSnapshot(
                thisRef.preferencesDataStore,
                thisRef.coroutineScope
            )
            return snapshot[prefKey] ?: defaultValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Long
        ) {
            val finalKey = key ?: property.name
            val prefKey = longPreferencesKey(finalKey)
            PreferenceSnapshotCoordinator.updateSnapshot(thisRef.preferencesDataStore) {
                it[prefKey] = value
            }
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[prefKey] = value
                }
            }
        }
    }

    fun boolean(
        defaultValue: Boolean = false,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Boolean> = object : ReadWriteProperty<DataStorePreferenceOwner, Boolean> {
        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Boolean {
            val finalKey = key ?: property.name
            val prefKey = booleanPreferencesKey(finalKey)
            val snapshot = PreferenceSnapshotCoordinator.getOrLoadSnapshot(
                thisRef.preferencesDataStore,
                thisRef.coroutineScope
            )
            return snapshot[prefKey] ?: defaultValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Boolean
        ) {
            val finalKey = key ?: property.name
            val prefKey = booleanPreferencesKey(finalKey)
            PreferenceSnapshotCoordinator.updateSnapshot(thisRef.preferencesDataStore) {
                it[prefKey] = value
            }
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[prefKey] = value
                }
            }
        }
    }
}
