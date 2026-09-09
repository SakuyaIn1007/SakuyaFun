package com.sakuya.feed.viewmodel

import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.data.notification.UpdateBadgeRepository
import com.sakuya.feed.data.remote.FeedApiService
import com.sakuya.feed.data.repository.FeedRepository
import com.sakuya.feed.data.repository.FollowingFeedRefreshNotifier
import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.profile.PublicProfileDto
import com.sakuya.model.profile.RelationshipUserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PublicAuthorViewModel.kt
 * 职责说明：加载他人公开资料和作者动态，并处理关注以及发起私信操作。
 * 执行流程：页面通过 onAction 提交意图 -> 单一 UiState 更新 -> 一次性错误与私信导航经 effect 下发。
 * 说明：请求失败保留已显示的数据；动态列表错误因需要持久重试入口，保留在 UiState。
 */
data class PublicAuthorUiState(
    val profile: PublicProfileDto? = null,
    val isLoading: Boolean = true,
    val posts: List<DynamicPost> = emptyList(),
    val isLoadingPosts: Boolean = true,
    val isLoadingMorePosts: Boolean = false,
    val canLoadMorePosts: Boolean = true,
    val postsErrorMessage: String? = null,
    val isOperatingFollow: Boolean = false,
    val isOpeningConversation: Boolean = false,
)

/** 他人主页所有用户意图的统一入口。 */
sealed interface PublicAuthorAction {
    data class Load(val userId: String) : PublicAuthorAction
    data class LoadProfile(val userId: String) : PublicAuthorAction
    data object RetryPosts : PublicAuthorAction
    data object LoadMorePosts : PublicAuthorAction
    data object ToggleFollow : PublicAuthorAction
    data object OpenConversation : PublicAuthorAction
}

/** 一次性提示与私信导航事件，不存入可重放的 UiState，防止重组或返回页面时重复触发。 */
sealed interface PublicAuthorEffect {
    data class ShowError(val message: String) : PublicAuthorEffect
    data class OpenConversation(val conversationId: String, val title: String) : PublicAuthorEffect
}

@HiltViewModel
class PublicAuthorViewModel @Inject constructor(
    private val api: FeedApiService,
    private val feedRepository: FeedRepository,
    private val followingFeedRefreshNotifier: FollowingFeedRefreshNotifier,
    private val updateBadges: UpdateBadgeRepository,
) : BaseMviViewModel<PublicAuthorUiState, PublicAuthorAction, PublicAuthorEffect>(
    initialState = PublicAuthorUiState(),
) {
    private var targetUserId: String = ""
    private var nextPostsPage: Int? = 0
    private var hasLoadedFirstPostsPage: Boolean = false
    private var hasMarkedAuthorRead: Boolean = false

    override fun onAction(action: PublicAuthorAction) {
        when (action) {
            is PublicAuthorAction.Load -> load(action.userId)
            is PublicAuthorAction.LoadProfile -> loadProfile(action.userId)
            PublicAuthorAction.RetryPosts -> retryPosts()
            PublicAuthorAction.LoadMorePosts -> loadMorePosts()
            PublicAuthorAction.ToggleFollow -> toggleFollow()
            PublicAuthorAction.OpenConversation -> openConversation()
        }
    }

    /** 主页入口会清理上一位作者的状态，再并行启动资料与动态首页请求。 */
    private fun load(userId: String) {
        if (userId.isBlank()) {
            updateState { PublicAuthorUiState(isLoading = false, isLoadingPosts = false, canLoadMorePosts = false) }
            sendEffect(PublicAuthorEffect.ShowError("用户信息无效"))
            return
        }
        targetUserId = userId
        nextPostsPage = 0
        hasLoadedFirstPostsPage = false
        hasMarkedAuthorRead = false
        updateState { PublicAuthorUiState() }
        loadProfile(userId)
        loadAuthorPosts(page = 0, refresh = true)
    }

    /** 动态详情页只需关注状态时调用此方法，避免额外请求作者动态列表。 */
    private fun loadProfile(userId: String) = viewModelScope.launch {
        updateState { it.copy(isLoading = true) }
        val response = runCatching { api.getPublicProfile(userId) }.getOrElse {
            updateState { it.copy(isLoading = false) }
            sendEffect(PublicAuthorEffect.ShowError("主页加载失败"))
            return@launch
        }
        val profile = response.body()?.data
        if (response.isSuccessful && response.body()?.code == 200 && profile != null) {
            updateState { it.copy(profile = profile, isLoading = false) }
            markAuthorReadIfReady()
        } else {
            updateState { it.copy(isLoading = false) }
            sendEffect(PublicAuthorEffect.ShowError(response.body()?.message ?: "主页加载失败"))
        }
    }

    private fun retryPosts() {
        if (currentState.posts.isEmpty()) loadAuthorPosts(page = 0, refresh = true)
        else nextPostsPage?.let { loadAuthorPosts(page = it, refresh = false) }
    }

    private fun loadMorePosts() {
        val page = nextPostsPage ?: return
        if (currentState.isLoadingPosts || currentState.isLoadingMorePosts) return
        loadAuthorPosts(page = page, refresh = false)
    }

    /**
     * 作者动态分页不写入首页 Room 流缓存。
     * 失败时保留已加载页：首页失败显示整页重试，加载更多失败只在列表底部重试。
     */
    private fun loadAuthorPosts(page: Int, refresh: Boolean) = viewModelScope.launch {
        if (targetUserId.isBlank()) return@launch
        updateState { state ->
            if (refresh) state.copy(isLoadingPosts = true, isLoadingMorePosts = false, postsErrorMessage = null)
            else state.copy(isLoadingMorePosts = true, postsErrorMessage = null)
        }
        feedRepository.getAuthorFeed(targetUserId, page, POSTS_PAGE_SIZE).onSuccess { result ->
            nextPostsPage = result.nextPage
            if (page == 0) hasLoadedFirstPostsPage = true
            updateState { state ->
                state.copy(
                    posts = if (refresh) result.items else (state.posts + result.items).distinctBy(DynamicPost::id),
                    isLoadingPosts = false,
                    isLoadingMorePosts = false,
                    canLoadMorePosts = result.nextPage != null,
                    postsErrorMessage = null,
                )
            }
            markAuthorReadIfReady()
        }.onFailure { error ->
            updateState { state ->
                state.copy(
                    isLoadingPosts = false,
                    isLoadingMorePosts = false,
                    postsErrorMessage = error.message ?: "作者动态加载失败",
                )
            }
        }
    }

    private fun toggleFollow() {
        val profile = currentState.profile ?: return
        if (currentState.isOperatingFollow) return
        viewModelScope.launch {
            updateState { it.copy(isOperatingFollow = true) }
            val response = runCatching {
                if (profile.isFollowing) api.unfollowUser(profile.userId) else api.followUser(profile.userId)
            }.getOrElse {
                updateState { it.copy(isOperatingFollow = false) }
                sendEffect(PublicAuthorEffect.ShowError("关注状态更新失败"))
                return@launch
            }
            val following = response.body()?.data?.isFollowing
            if (response.isSuccessful && response.body()?.code == 200 && following != null) {
                val followerDelta = when {
                    profile.isFollowing == following -> 0L
                    following -> 1L
                    else -> -1L
                }
                updateState { state ->
                    state.copy(
                        profile = state.profile?.copy(
                            isFollowing = following,
                            followerCount = (profile.followerCount + followerDelta).coerceAtLeast(0L),
                        ),
                        isOperatingFollow = false,
                    )
                }
                followingFeedRefreshNotifier.emitRelationshipChanged()
                updateBadges.refreshFollowing()
            } else {
                updateState { it.copy(isOperatingFollow = false) }
                sendEffect(PublicAuthorEffect.ShowError(response.body()?.message ?: "关注状态更新失败"))
            }
        }
    }

    /**
     * 私信执行流程：锁定按钮 -> 调用服务端幂等直聊接口 -> 校验会话标识 -> 解锁并发送导航事件。
     * 同一时刻只允许一个请求；失败时保留主页内容并通过 effect 交给 Snackbar 反馈。
     */
    private fun openConversation() {
        val profile = currentState.profile ?: return
        if (currentState.isOpeningConversation) return
        // 在启动协程前同步加锁，确保同一帧内的连续点击也只能创建一个网络请求。
        updateState { it.copy(isOpeningConversation = true) }
        viewModelScope.launch {
            val response = try {
                api.getOrCreateDirectConversation(profile.userId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                updateState { it.copy(isOpeningConversation = false) }
                sendEffect(PublicAuthorEffect.ShowError("私信会话打开失败，请稍后重试"))
                return@launch
            }
            val body = response.body()
            val conversation = body?.data
            if (response.isSuccessful && body?.code == 200 && !conversation?.id.isNullOrBlank()) {
                updateState { it.copy(isOpeningConversation = false) }
                sendEffect(
                    PublicAuthorEffect.OpenConversation(
                        conversationId = requireNotNull(conversation).id,
                        title = conversation.title.ifBlank { profile.nickname },
                    )
                )
            } else {
                updateState { it.copy(isOpeningConversation = false) }
                sendEffect(PublicAuthorEffect.ShowError(body?.message ?: "私信会话打开失败，请稍后重试"))
            }
        }
    }

    /** 只有主页资料和动态首页都成功展示后才清除该作者红点，失败页不会误标已读。 */
    private suspend fun markAuthorReadIfReady() {
        val profile = currentState.profile ?: return
        if (!hasMarkedAuthorRead && hasLoadedFirstPostsPage && profile.isFollowing && profile.userId == targetUserId) {
            updateBadges.markAuthorRead(profile.userId).onSuccess { hasMarkedAuthorRead = true }
        }
    }

    private companion object { const val POSTS_PAGE_SIZE = 10 }
}

/**
 * PublicRelationshipViewModel.kt
 * 职责说明：读取某个他人主页的关注或粉丝列表。
 * 执行流程：列表页按 type 请求对应 /profiles/{userId} 子路径；403 等业务错误保留在 UiState 供整页重试。
 * 架构说明：MVI 风格；列表错误是持久的页面条件（空列表 + 重试入口），故保留在状态而非 effect。
 */
data class PublicRelationshipUiState(
    val users: List<RelationshipUserDto> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val operatingUserId: String? = null,
    val errorMessage: String? = null,
)

/** 关注/粉丝列表页所有用户意图的统一入口。 */
sealed interface PublicRelationshipAction {
    data class Load(val userId: String, val followers: Boolean) : PublicRelationshipAction
    data object Retry : PublicRelationshipAction
    data object LoadMore : PublicRelationshipAction
    data class ToggleFollow(val user: RelationshipUserDto) : PublicRelationshipAction
}

@HiltViewModel
class PublicRelationshipViewModel @Inject constructor(
    private val api: FeedApiService,
    private val followingFeedRefreshNotifier: FollowingFeedRefreshNotifier,
) : BaseMviViewModel<PublicRelationshipUiState, PublicRelationshipAction, Unit>(
    initialState = PublicRelationshipUiState(),
) {

    private var targetUserId = ""
    private var showFollowers = false
    private var nextPage: Int? = 0

    override fun onAction(action: PublicRelationshipAction) {
        when (action) {
            is PublicRelationshipAction.Load -> load(action.userId, action.followers)
            PublicRelationshipAction.Retry -> retry()
            PublicRelationshipAction.LoadMore -> loadMore()
            is PublicRelationshipAction.ToggleFollow -> toggleFollow(action.user)
        }
    }

    /** 切换主页或列表类型时清空旧数据，避免短暂显示上一个用户的关系记录。 */
    private fun load(userId: String, followers: Boolean) {
        targetUserId = userId
        showFollowers = followers
        nextPage = 0
        loadPage(page = 0, refresh = true)
    }

    private fun retry() {
        if (currentState.users.isEmpty()) loadPage(page = 0, refresh = true)
        else nextPage?.let { loadPage(page = it, refresh = false) }
    }

    private fun loadMore() {
        val page = nextPage ?: return
        if (currentState.isLoading || currentState.isLoadingMore) return
        loadPage(page, refresh = false)
    }

    /** 关注操作按服务端返回值更新单行状态，失败时保留原列表供用户重试。 */
    private fun toggleFollow(user: RelationshipUserDto) = viewModelScope.launch {
        if (currentState.operatingUserId != null) return@launch
        updateState { it.copy(operatingUserId = user.userId, errorMessage = null) }
        val response = runCatching {
            if (user.isFollowing) api.unfollowUser(user.userId) else api.followUser(user.userId)
        }.getOrElse {
            updateState { it.copy(operatingUserId = null, errorMessage = "关注状态更新失败") }
            return@launch
        }
        val updated = response.body()?.data
        if (response.isSuccessful && response.body()?.code == 200 && updated != null) {
            followingFeedRefreshNotifier.emitRelationshipChanged()
            updateState { state ->
                state.copy(
                    users = state.users.map { current -> if (current.userId == updated.userId) updated else current },
                    operatingUserId = null,
                )
            }
        } else {
            updateState { state ->
                state.copy(
                    operatingUserId = null,
                    errorMessage = response.body()?.message ?: "关注状态更新失败",
                )
            }
        }
    }

    private fun loadPage(page: Int, refresh: Boolean) = viewModelScope.launch {
        if (targetUserId.isBlank()) {
            updateState { PublicRelationshipUiState(isLoading = false, canLoadMore = false, errorMessage = "用户信息无效") }
            return@launch
        }
        updateState { state ->
            if (refresh) PublicRelationshipUiState(isLoading = true)
            else state.copy(isLoadingMore = true, errorMessage = null)
        }
        val response = runCatching {
            if (showFollowers) api.getFollowerUsers(targetUserId, page, PAGE_SIZE)
            else api.getFollowingUsers(targetUserId, page, PAGE_SIZE)
        }.getOrElse {
            updateState { it.copy(isLoading = false, isLoadingMore = false, errorMessage = "列表加载失败") }
            return@launch
        }
        val body = response.body()
        val data = body?.data
        if (response.isSuccessful && body?.code == 200 && data != null) {
            nextPage = data.nextPage
            updateState { state ->
                state.copy(
                    users = if (refresh) data.users else state.users + data.users,
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = data.nextPage != null,
                )
            }
        } else {
            updateState { state ->
                state.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = body?.message ?: "列表加载失败",
                )
            }
        }
    }

    private companion object { const val PAGE_SIZE = 20 }
}
