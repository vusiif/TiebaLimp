package com.huanchengfly.tieba.post.utils

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Global snapshot coordinator for Preferences DataStore.
 * Eliminates per-property runBlocking disk reads and redundant flow collector subscriptions
 * by maintaining an in-memory cached Preferences snapshot with a single continuous reactive flow.
 */
object PreferenceSnapshotCoordinator {
    private val dataStoreSnapshots = ConcurrentHashMap<DataStore<Preferences>, AtomicReference<Preferences>>()
    private val listeningDataStores = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<DataStore<Preferences>, Boolean>())
    )

    /**
     * Attaches a single continuous collector to observe and mirror DataStore changes into memory.
     */
    fun attach(dataStore: DataStore<Preferences>, scope: CoroutineScope) {
        if (listeningDataStores.add(dataStore)) {
            val ref = dataStoreSnapshots.computeIfAbsent(dataStore) { AtomicReference(null) }
            scope.launch {
                runCatching {
                    dataStore.data.collect { newPrefs ->
                        ref.set(newPrefs)
                    }
                }
            }
        }
    }

    /**
     * Returns current in-memory cached snapshot if available.
     */
    fun getSnapshot(dataStore: DataStore<Preferences>): Preferences? {
        return dataStoreSnapshots[dataStore]?.get()
    }

    /**
     * Retrieves the cached snapshot or synchronously loads all preferences once in a single read.
     * Subsequent queries for any key hit the in-memory snapshot with 0ms and zero disk I/O.
     */
    fun getOrLoadSnapshot(
        dataStore: DataStore<Preferences>,
        scope: CoroutineScope? = null
    ): Preferences {
        val existing = dataStoreSnapshots[dataStore]?.get()
        if (existing != null) return existing

        val ref = dataStoreSnapshots.computeIfAbsent(dataStore) { AtomicReference(null) }
        val current = ref.get()
        if (current != null) return current

        if (scope != null) {
            attach(dataStore, scope)
        }

        val loaded = runBlocking {
            dataStore.data.first()
        }
        ref.set(loaded)
        return loaded
    }

    /**
     * Optimistically updates the in-memory snapshot ahead of async disk serialization.
     */
    fun updateSnapshot(dataStore: DataStore<Preferences>, update: (MutablePreferences) -> Unit) {
        val ref = dataStoreSnapshots[dataStore] ?: return
        val current = ref.get() ?: return
        val mutable = current.toMutablePreferences()
        update(mutable)
        ref.set(mutable.toPreferences())
    }

    @VisibleForTesting
    fun setSnapshotForTesting(dataStore: DataStore<Preferences>, preferences: Preferences?) {
        val ref = dataStoreSnapshots.computeIfAbsent(dataStore) { AtomicReference(null) }
        ref.set(preferences)
    }

    @VisibleForTesting
    fun clear() {
        dataStoreSnapshots.clear()
        listeningDataStores.clear()
    }
}
