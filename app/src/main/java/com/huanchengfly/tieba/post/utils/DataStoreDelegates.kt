package com.huanchengfly.tieba.post.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getFloat
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getLong
import com.huanchengfly.tieba.post.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
 * Property delegates for DataStore preferences with in-memory caching and reactive flow observation.
 */
object DataStoreDelegates {
    fun int(
        defaultValue: Int = 0,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Int> = object : ReadWriteProperty<DataStorePreferenceOwner, Int> {
        private var prefValue = defaultValue
        private var initialized = false

        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Int {
            val finalKey = key ?: property.name
            if (!initialized) {
                initialized = true
                prefValue = thisRef.preferencesDataStore.getInt(finalKey, defaultValue)
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.data
                        .map { it[intPreferencesKey(finalKey)] }
                        .distinctUntilChanged()
                        .collect {
                            prefValue = it ?: defaultValue
                        }
                }
            }
            return prefValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Int
        ) {
            prefValue = value
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[intPreferencesKey(key ?: property.name)] = value
                }
            }
        }
    }

    fun string(
        defaultValue: String? = null,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, String?> = object : ReadWriteProperty<DataStorePreferenceOwner, String?> {
        private var prefValue = defaultValue
        private var initialized = false

        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): String? {
            val finalKey = key ?: property.name
            if (!initialized) {
                initialized = true
                prefValue = thisRef.preferencesDataStore.getString(finalKey)
                    ?: defaultValue
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.data
                        .map { it[stringPreferencesKey(finalKey)] }
                        .distinctUntilChanged()
                        .collect {
                            prefValue = it ?: defaultValue
                        }
                }
            }
            return prefValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: String?
        ) {
            prefValue = value
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    if (value == null) {
                        it.remove(stringPreferencesKey(key ?: property.name))
                    } else {
                        it[stringPreferencesKey(key ?: property.name)] = value
                    }
                }
            }
        }
    }

    fun float(
        defaultValue: Float = 0F,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Float> = object : ReadWriteProperty<DataStorePreferenceOwner, Float> {
        private var prefValue = defaultValue
        private var initialized = false

        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Float {
            val finalKey = key ?: property.name
            if (!initialized) {
                initialized = true
                prefValue =
                    thisRef.preferencesDataStore.getFloat(finalKey, defaultValue)
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.data
                        .map { it[floatPreferencesKey(finalKey)] }
                        .distinctUntilChanged()
                        .collect {
                            prefValue = it ?: defaultValue
                        }
                }
            }
            return prefValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Float
        ) {
            prefValue = value
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[floatPreferencesKey(key ?: property.name)] = value
                }
            }
        }
    }

    fun long(
        defaultValue: Long = 0L,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Long> = object : ReadWriteProperty<DataStorePreferenceOwner, Long> {
        private var prefValue = defaultValue
        private var initialized = false

        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Long {
            val finalKey = key ?: property.name
            if (!initialized) {
                initialized = true
                prefValue =
                    thisRef.preferencesDataStore.getLong(finalKey, defaultValue)
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.data
                        .map { it[longPreferencesKey(finalKey)] }
                        .distinctUntilChanged()
                        .collect {
                            prefValue = it ?: defaultValue
                        }
                }
            }
            return prefValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Long
        ) {
            prefValue = value
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[longPreferencesKey(key ?: property.name)] = value
                }
            }
        }
    }

    fun boolean(
        defaultValue: Boolean = false,
        key: String? = null
    ): ReadWriteProperty<DataStorePreferenceOwner, Boolean> = object : ReadWriteProperty<DataStorePreferenceOwner, Boolean> {
        private var prefValue = defaultValue
        private var initialized = false

        override fun getValue(thisRef: DataStorePreferenceOwner, property: KProperty<*>): Boolean {
            val finalKey = key ?: property.name
            if (!initialized) {
                initialized = true
                prefValue =
                    thisRef.preferencesDataStore.getBoolean(finalKey, defaultValue)
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.data
                        .map { it[booleanPreferencesKey(finalKey)] }
                        .distinctUntilChanged()
                        .collect {
                            prefValue = it ?: defaultValue
                        }
                }
            }
            return prefValue
        }

        override fun setValue(
            thisRef: DataStorePreferenceOwner,
            property: KProperty<*>,
            value: Boolean
        ) {
            prefValue = value
            thisRef.coroutineScope.launch {
                thisRef.preferencesDataStore.edit {
                    it[booleanPreferencesKey(key ?: property.name)] = value
                }
            }
        }
    }
}
