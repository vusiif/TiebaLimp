package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import com.huanchengfly.tieba.post.utils.PreferenceSnapshotCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

object DataStoreConst {
    const val DATA_STORE_NAME = "app_preferences"
}

private val dataStoreInstance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    preferencesDataStore(
        name = DataStoreConst.DATA_STORE_NAME,
        produceMigrations = { context ->
            listOf(
                SharedPreferencesMigration(context, "settings"),
                object : DataMigration<Preferences> {
                    override suspend fun cleanUp() {}

                    override suspend fun migrate(currentData: Preferences): Preferences {
                        return currentData.toMutablePreferences().apply {
                            set(stringPreferencesKey("dark_theme"), "grey_dark")
                        }.toPreferences()
                    }

                    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
                        return currentData[stringPreferencesKey("dark_theme")] == "dark"
                    }
                }
            )
        }
    )
}

val Context.dataStore: DataStore<Preferences> by dataStoreInstance

@Composable
fun <T> rememberPreferenceAsMutableState(
    key: Preferences.Key<T>,
    defaultValue: T
): MutableState<T> {
    val dataStore = LocalContext.current.dataStore
    val coroutineScope = rememberCoroutineScope()
    val persistedValue by rememberPreferenceAsState(key, defaultValue)
    val state = remember { mutableStateOf(defaultValue) }

    LaunchedEffect(persistedValue) {
        state.value = persistedValue
    }

    return remember(key, dataStore) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(newVal) {
                    state.value = newVal
                    coroutineScope.launch {
                        dataStore.edit { it[key] = newVal }
                    }
                }

            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
fun <T> rememberPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    val dataStore = LocalContext.current.dataStore
    val preferenceFlow = remember(dataStore, key, defaultValue) {
        dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }
    return preferenceFlow.collectAsState(initial = defaultValue)
}

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun <T> DataStore<Preferences>.collectPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    return data.map { it[key] ?: defaultValue }.collectAsState(initial = defaultValue)
}

fun DataStore<Preferences>.putString(key: String, value: String? = null) {
    dataStoreScope.launch {
        edit {
            if (value == null) {
                it.remove(stringPreferencesKey(key))
            } else {
                it[stringPreferencesKey(key)] = value
            }
        }
    }
}

fun DataStore<Preferences>.putBoolean(key: String, value: Boolean) {
    dataStoreScope.launch {
        edit {
            it[booleanPreferencesKey(key)] = value
        }
    }
}

fun DataStore<Preferences>.putInt(key: String, value: Int) {
    dataStoreScope.launch {
        edit {
            it[intPreferencesKey(key)] = value
        }
    }
}

private inline fun <T> DataStore<Preferences>.readPreference(
    key: Preferences.Key<T>,
    defaultValue: T
): T {
    val snapshot = PreferenceSnapshotCoordinator.getSnapshot(this)
    if (snapshot != null) {
        return snapshot[key] ?: defaultValue
    }
    var resultValue = defaultValue
    runBlocking {
        data.first {
            resultValue = it[key] ?: resultValue
            true
        }
    }
    return resultValue
}

fun DataStore<Preferences>.getInt(key: String, defaultValue: Int): Int =
    readPreference(intPreferencesKey(key), defaultValue)

fun DataStore<Preferences>.getString(key: String): String? {
    val prefKey = stringPreferencesKey(key)
    val snapshot = PreferenceSnapshotCoordinator.getSnapshot(this)
    if (snapshot != null) {
        return snapshot[prefKey]
    }
    var resultValue: String? = null
    runBlocking {
        data.first {
            resultValue = it[prefKey]
            true
        }
    }
    return resultValue
}

fun DataStore<Preferences>.getString(key: String, defaultValue: String): String =
    readPreference(stringPreferencesKey(key), defaultValue)

fun DataStore<Preferences>.getStringSet(
    key: String,
    defaultValues: MutableSet<String>? = null
): MutableSet<String>? {
    val prefKey = stringSetPreferencesKey(key)
    val snapshot = PreferenceSnapshotCoordinator.getSnapshot(this)
    if (snapshot != null) {
        return snapshot[prefKey]?.toMutableSet() ?: defaultValues
    }
    var resultValue = defaultValues
    runBlocking {
        data.first {
            resultValue = it[prefKey]?.toMutableSet() ?: resultValue
            true
        }
    }
    return resultValue
}

fun DataStore<Preferences>.getBoolean(key: String, defaultValue: Boolean): Boolean =
    readPreference(booleanPreferencesKey(key), defaultValue)

fun DataStore<Preferences>.getFloat(key: String, defaultValue: Float): Float =
    readPreference(floatPreferencesKey(key), defaultValue)

fun DataStore<Preferences>.getLong(key: String, defaultValue: Long): Long =
    readPreference(longPreferencesKey(key), defaultValue)

class DataStorePreference : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        dataStoreScope.launch {
            App.INSTANCE.dataStore.edit {
                if (value == null) {
                    it.remove(stringPreferencesKey(key))
                } else {
                    it[stringPreferencesKey(key)] = value
                }
            }
        }
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        dataStoreScope.launch {
            App.INSTANCE.dataStore.edit {
                if (values == null) {
                    it.remove(stringSetPreferencesKey(key))
                } else {
                    it[stringSetPreferencesKey(key)] = values
                }
            }
        }
    }

    override fun putInt(key: String, value: Int) {
        dataStoreScope.launch {
            App.INSTANCE.dataStore.edit {
                it[intPreferencesKey(key)] = value
            }
        }
    }

    override fun putLong(key: String, value: Long) {
        dataStoreScope.launch {
            App.INSTANCE.dataStore.edit {
                it[longPreferencesKey(key)] = value
            }
        }
    }

    override fun putFloat(key: String, value: Float) {
        dataStoreScope.launch {
            App.INSTANCE.dataStore.edit {
                it[floatPreferencesKey(key)] = value
            }
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        dataStoreScope.launch {
            App.INSTANCE.dataStore.edit {
                it[booleanPreferencesKey(key)] = value
            }
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        return App.INSTANCE.dataStore.getString(key) ?: defValue
    }

    override fun getStringSet(
        key: String,
        defValues: MutableSet<String>?
    ): MutableSet<String>? {
        return App.INSTANCE.dataStore.getStringSet(key, defValues)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return App.INSTANCE.dataStore.getInt(key, defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return App.INSTANCE.dataStore.getLong(key, defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return App.INSTANCE.dataStore.getFloat(key, defValue)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return App.INSTANCE.dataStore.getBoolean(key, defValue)
    }
}