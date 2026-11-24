package com.huanchengfly.tieba.post

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.utils.DataStoreDelegates
import com.huanchengfly.tieba.post.utils.DataStorePreferenceOwner
import com.huanchengfly.tieba.post.utils.PreferenceSnapshotCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreferenceSnapshotCoordinatorTest {

    private class FakeDataStore(
        initialPreferences: Preferences = mutablePreferencesOf()
    ) : DataStore<Preferences> {
        val stateFlow = MutableStateFlow(initialPreferences)

        override val data: Flow<Preferences> = stateFlow

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(stateFlow.value)
            stateFlow.value = updated
            return updated
        }
    }

    private class TestPreferenceOwner(
        override val preferencesDataStore: DataStore<Preferences>,
        override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
    ) : DataStorePreferenceOwner {
        var testInt by DataStoreDelegates.int(defaultValue = 42, key = "test_int")
        var testString by DataStoreDelegates.string(defaultValue = "default_str", key = "test_str")
        var testBool by DataStoreDelegates.boolean(defaultValue = false, key = "test_bool")
    }

    @Before
    fun setUp() {
        PreferenceSnapshotCoordinator.clear()
    }

    @After
    fun tearDown() {
        PreferenceSnapshotCoordinator.clear()
    }

    @Test
    fun testInstantInMemorySnapshotRead() {
        val initialPrefs = mutablePreferencesOf(
            intPreferencesKey("test_int") to 100,
            stringPreferencesKey("test_str") to "custom_value",
            booleanPreferencesKey("test_bool") to true
        )
        val fakeDataStore = FakeDataStore(initialPrefs)
        val owner = TestPreferenceOwner(fakeDataStore)

        // Seed snapshot
        PreferenceSnapshotCoordinator.setSnapshotForTesting(fakeDataStore, initialPrefs)

        // Reading via delegates must return snapshot values instantly
        assertEquals(100, owner.testInt)
        assertEquals("custom_value", owner.testString)
        assertTrue(owner.testBool)

        // Directly querying the coordinator
        val snapshot = PreferenceSnapshotCoordinator.getSnapshot(fakeDataStore)
        assertEquals(100, snapshot?.get(intPreferencesKey("test_int")))
    }

    @Test
    fun testFallbackToDefaultWhenKeyMissing() {
        val emptyPrefs = mutablePreferencesOf()
        val fakeDataStore = FakeDataStore(emptyPrefs)
        val owner = TestPreferenceOwner(fakeDataStore)

        PreferenceSnapshotCoordinator.setSnapshotForTesting(fakeDataStore, emptyPrefs)

        assertEquals(42, owner.testInt)
        assertEquals("default_str", owner.testString)
        assertFalse(owner.testBool)
    }

    @Test
    fun testOptimisticSnapshotUpdate() {
        val initialPrefs = mutablePreferencesOf(
            intPreferencesKey("test_int") to 10
        )
        val fakeDataStore = FakeDataStore(initialPrefs)
        val owner = TestPreferenceOwner(fakeDataStore)

        PreferenceSnapshotCoordinator.setSnapshotForTesting(fakeDataStore, initialPrefs)
        assertEquals(10, owner.testInt)

        // Set value via property delegate
        owner.testInt = 999

        // Memory snapshot must be updated immediately
        assertEquals(999, owner.testInt)
        val snapshot = PreferenceSnapshotCoordinator.getSnapshot(fakeDataStore)
        assertEquals(999, snapshot?.get(intPreferencesKey("test_int")))
    }

    @Test
    fun testClearResetsCoordinator() {
        val initialPrefs = mutablePreferencesOf(
            stringPreferencesKey("test_str") to "hello"
        )
        val fakeDataStore = FakeDataStore(initialPrefs)

        PreferenceSnapshotCoordinator.setSnapshotForTesting(fakeDataStore, initialPrefs)
        assertEquals("hello", PreferenceSnapshotCoordinator.getSnapshot(fakeDataStore)?.get(stringPreferencesKey("test_str")))

        PreferenceSnapshotCoordinator.clear()
        assertNull(PreferenceSnapshotCoordinator.getSnapshot(fakeDataStore))
    }
}
