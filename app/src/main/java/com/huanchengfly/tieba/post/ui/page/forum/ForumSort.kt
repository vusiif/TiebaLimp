package com.huanchengfly.tieba.post.ui.page.forum

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.utils.appPreferences

fun getSortType(
    context: Context,
    forumName: String,
): Int {
    val defaultSortType = context.appPreferences.defaultSortType?.toIntOrNull() ?: 0
    return context.dataStore.getInt("${forumName}_sort_type", defaultSortType)
}

suspend fun setSortType(
    context: Context,
    forumName: String,
    sortType: Int,
) {
    context.dataStore.edit {
        it[intPreferencesKey("${forumName}_sort_type")] = sortType
    }
}
