package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.utils.SwrCacheManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PersonalizedRepository {
    private const val KEY_PERSONALIZED_FEED = "personalized_feed_first_page"

    /**
     * 个性推荐
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    fun personalizedFlow(loadType: Int, page: Int = 1): Flow<PersonalizedResponse> {
        val baseFlow = TiebaApi.getInstance()
            .personalizedProtoFlow(loadType, page)
            .map { response ->
                val liveThreadIds =
                    response.data_?.thread_list?.filter { it.ala_info != null }?.map { it.id }
                        ?: emptyList()
                response.copy(
                    data_ = response.data_?.copy(
                        thread_list = response.data_.thread_list.filter { !liveThreadIds.contains(it.id) },
                        thread_personalized = response.data_.thread_personalized.filter {
                            !liveThreadIds.contains(
                                it.tid
                            )
                        }
                    )
                )
            }

        return if (loadType == 1 && page == 1) {
            SwrCacheManager.flowWithSwr(
                key = KEY_PERSONALIZED_FEED,
                fetcher = { baseFlow },
                serializer = { it.encode() },
                deserializer = { PersonalizedResponse.ADAPTER.decode(it) },
                ttlMillis = 5 * 60 * 1000L,
                staleTolerateMillis = 7 * 24 * 60 * 60 * 1000L,
                forceRefresh = true
            )
        } else {
            baseFlow
        }
    }
}