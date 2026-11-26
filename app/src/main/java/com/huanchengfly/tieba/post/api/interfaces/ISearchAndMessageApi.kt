package com.huanchengfly.tieba.post.api.interfaces

import com.huanchengfly.tieba.post.api.ForumSortType
import com.huanchengfly.tieba.post.api.SearchThreadFilter
import com.huanchengfly.tieba.post.api.SearchThreadOrder
import com.huanchengfly.tieba.post.api.models.*
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.ForumRecommendResponse
import com.huanchengfly.tieba.post.api.models.protos.forumRuleDetail.ForumRuleDetailResponse
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.api.models.protos.getBawuInfo.GetBawuInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.getForumDetail.GetForumDetailResponse
import com.huanchengfly.tieba.post.api.models.protos.getHistoryForum.GetHistoryForumResponse
import com.huanchengfly.tieba.post.api.models.protos.getLevelInfo.GetLevelInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.getMemberInfo.GetMemberInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.getUserInfo.GetUserInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponse
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorResponse
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.api.models.protos.profile.ProfileResponse
import com.huanchengfly.tieba.post.api.models.protos.searchSug.SearchSugResponse
import com.huanchengfly.tieba.post.api.models.protos.threadList.ThreadListResponse
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListResponse
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponse
import com.huanchengfly.tieba.post.api.models.protos.userPost.UserPostResponse
import com.huanchengfly.tieba.post.api.models.web.ForumBean
import com.huanchengfly.tieba.post.api.models.web.ForumHome
import com.huanchengfly.tieba.post.api.models.web.HotMessageListBean
import com.huanchengfly.tieba.post.api.retrofit.ApiResult
import com.huanchengfly.tieba.post.models.DislikeBean
import com.huanchengfly.tieba.post.models.MyInfoBean
import com.huanchengfly.tieba.post.models.PhotoInfoBean
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import retrofit2.Call
import java.io.File

interface ISearchAndMessageApi {
    /**
     * 吧内搜索
     *
     * @param keyword 搜索关键词
     * @param forumName 搜索吧名
     * @param onlyThread 是否仅搜索主题贴
     * @param sortMode 排序模式（1 = 时间倒序，2 = 相关性排序）
     * @param page 分页页码（从 1 开始）
     * @param pageSize 每页贴数（默认 30）
     */
    fun searchPost(
        keyword: String,
        forumName: String,
        onlyThread: Boolean = false,
        sortMode: Int = 1,
        page: Int = 1,
        pageSize: Int = 30
    ): Call<SearchPostBean>
    /**
     * 吧内搜索（异步）
     *
     * @param keyword 搜索关键词
     * @param forumName 搜索吧名
     * @param onlyThread 是否仅搜索主题贴
     * @param sortMode 排序模式（1 = 时间倒序，2 = 相关性排序）
     * @param page 分页页码（从 1 开始）
     * @param pageSize 每页贴数（默认 30）
     */
    fun searchPostAsync(
        keyword: String,
        forumName: String,
        onlyThread: Boolean = false,
        sortMode: Int = 1,
        page: Int = 1,
        pageSize: Int = 30
    ): Deferred<ApiResult<SearchPostBean>>
    /**
     * 搜索用户
     *
     * @param keyword 搜索关键词
     */
    fun searchUser(
        keyword: String,
    ): Call<SearchUserBean>
    /**
     * 搜索用户
     *
     * @param keyword 搜索关键词
     */
    fun searchUserFlow(
        keyword: String,
    ): Flow<SearchUserBean>
    /**
     * 消息提醒数
     *
     * **需登录**
     */
    fun msg(): Call<MsgBean>
    /**
     * 消息提醒数
     *
     * **需登录**
     */
    fun msgFlow(): Flow<MsgBean>
    /**
     * 回复我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun replyMe(
        page: Int = 1
    ): Call<MessageListBean>
    /**
     * 回复我的消息列表（异步）
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun replyMeAsync(
        page: Int = 1
    ): Deferred<ApiResult<MessageListBean>>
    /**
     * 回复我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     * @return Flow
     */
    fun replyMeFlow(
        page: Int = 1
    ): Flow<MessageListBean>
    /**
     * 提到我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun atMe(
        page: Int = 1
    ): Call<MessageListBean>
    /**
     * 提到我的消息列表（异步）
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun atMeAsync(
        page: Int = 1
    ): Deferred<ApiResult<MessageListBean>>
    /**
     * 提到我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     * @return Flow
     */
    fun atMeFlow(
        page: Int = 1
    ): Flow<MessageListBean>
    /**
     * 赞我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun agreeMe(
        page: Int = 1
    ): Call<MessageListBean>
    /**
     * 搜索吧
     *
     * @param keyword 关键词
     */
    fun searchForum(
        keyword: String,
    ): Call<SearchForumBean>
    /**
     * 搜索吧
     *
     * @param keyword 关键词
     */
    fun searchForumFlow(
        keyword: String,
    ): Flow<SearchForumBean>
    /**
     * 搜索贴
     *
     * @param keyword 关键词
     * @param page 分页页码
     * @param order 排序设置 [com.huanchengfly.tieba.post.api.SearchThreadOrder]
     * @param filter 过滤设置 [com.huanchengfly.tieba.post.api.SearchThreadFilter]
     */
    fun searchThread(
        keyword: String,
        page: Int,
        order: SearchThreadOrder,
        filter: SearchThreadFilter,
    ): Call<SearchThreadBean>
    /**
     * 搜索主题贴
     *
     * @param keyword 关键词
     * @param page 分页页码
     * @param sort 结果排序（0 = 旧贴在前，2 = 相关程度，5 = 新贴在前）
     */
    fun searchThreadFlow(
        keyword: String,
        page: Int,
        sort: Int,
    ): Flow<SearchThreadBean>
    /**
     * 吧内搜索
     *
     * @param keyword 搜索关键词
     * @param forumName 搜索吧名
     * @param forumId 搜索吧 ID
     * @param sortType 排序模式（1 = 时间倒序，2 = 相关性排序）
     * @param filterType 过滤（1 = 全部，2 = 仅看主题贴）
     * @param page 分页页码（从 1 开始）
     * @param pageSize 每页贴数（默认 20）
     */
    fun searchPostFlow(
        keyword: String,
        forumName: String,
        forumId: Long,
        sortType: Int = 1,
        filterType: Int = 1,
        page: Int = 1,
        pageSize: Int = 20,
    ): Flow<SearchThreadBean>
    /**
     * 搜索联想
     *
     * @param keyword 关键词
     * @param isForum 是否为吧
     */
    fun searchSuggestionsFlow(
        keyword: String,
        isForum: Boolean = false,
    ): Flow<SearchSugResponse>
}
