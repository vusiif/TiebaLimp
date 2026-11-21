package com.huanchengfly.tieba.post

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.huanchengfly.tieba.post.utils.DataStoreDelegates
import com.huanchengfly.tieba.post.utils.DataStorePreferenceOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreDelegatesTest {
    private class FakeDataStore : DataStore<Preferences> {
        private var prefs: Preferences = emptyPreferences()
        private val _flow = MutableStateFlow(prefs)
        override val data: Flow<Preferences> = _flow

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            prefs = transform(prefs)
            _flow.value = prefs
            return prefs
        }
    }

    private class TestPreferenceHolder(
        override val preferencesDataStore: DataStore<Preferences> = FakeDataStore(),
        override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    ) : DataStorePreferenceOwner {
        var testInt by DataStoreDelegates.int(defaultValue = 42)
        var testString by DataStoreDelegates.string(defaultValue = "hello")
        var testNullableString by DataStoreDelegates.string(defaultValue = null)
        var testBoolean by DataStoreDelegates.boolean(defaultValue = true)
        var testFloat by DataStoreDelegates.float(defaultValue = 3.14f)
        var testLong by DataStoreDelegates.long(defaultValue = 1000L)

        var customKeyInt by DataStoreDelegates.int(defaultValue = 10, key = "custom_int_key")
    }

    @Test
    fun testDefaultValues() {
        val holder = TestPreferenceHolder()
        assertEquals(42, holder.testInt)
        assertEquals("hello", holder.testString)
        assertNull(holder.testNullableString)
        assertTrue(holder.testBoolean)
        assertEquals(3.14f, holder.testFloat, 0.001f)
        assertEquals(1000L, holder.testLong)
        assertEquals(10, holder.customKeyInt)
    }

    @Test
    fun testSetAndGetValues() {
        val holder = TestPreferenceHolder()
        holder.testInt = 99
        assertEquals(99, holder.testInt)

        holder.testString = "world"
        assertEquals("world", holder.testString)

        holder.testNullableString = "not_null"
        assertEquals("not_null", holder.testNullableString)

        holder.testBoolean = false
        assertFalse(holder.testBoolean)

        holder.testFloat = 2.718f
        assertEquals(2.718f, holder.testFloat, 0.001f)

        holder.testLong = 99999L
        assertEquals(99999L, holder.testLong)

        holder.customKeyInt = 200
        assertEquals(200, holder.customKeyInt)
    }
}
