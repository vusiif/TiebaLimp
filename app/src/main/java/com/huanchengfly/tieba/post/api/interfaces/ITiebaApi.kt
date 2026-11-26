package com.huanchengfly.tieba.post.api.interfaces

/**
 * Unified Tieba API contract composed of domain-specific interfaces.
 */
interface ITiebaApi :
    IExploreApi,
    IForumApi,
    IThreadApi,
    IUserApi,
    ISearchAndMessageApi
