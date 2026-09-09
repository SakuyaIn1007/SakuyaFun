package com.sakuya.feed.viewmodel

import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.feed.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedComment
import com.sakuya.model.feed.FeedCommentDraft
import com.sakuya.model.feed.FeedPage
import com.sakuya.model.feed.FeedReport
import kotlinx.coroutines.launch

/**
 * FeedViewModel.kt
 * 职责说明：保留 Feed ViewModel 包的说明入口；具体页面状态已按时间线、详情、发布三个职责拆分。
 * 执行流程：各 Screen 仅依赖对应的专属 ViewModel，避免不同页面的状态与副作用相互耦合。
 */

/**
 * 动态详情页的状态与业务逻辑。
 * 执行流程：FeedDetailScreen 将路由参数封装为 Load Action -> Repository 获取正文和评论 -> UiState 或 Effect 回传页面。
 * 架构说明：MVI 风格，统一经 onAction 接收意图，一次性提示经 effect 通道下发。
 */
data class FeedDetailUiState(
    val post: DynamicPost? = null,
    val comments: List<FeedComment> = emptyList(),
    val isLoading: Boolean = true,
    val commentInput: String = "",
    val actionState: FeedPostActionState = FeedPostActionState.IDLE,
)

/** 详情页所有会修改服务端数据的操作共用该状态，UI 可统一禁用重复点击并展示错误。 */
enum class FeedPostActionState { IDLE, LIKING, FAVORITING, SUBMITTING_COMMENT, REPORTING }

sealed interface FeedDetailAction {
    data class Load(val postId: String) : FeedDetailAction
    data class UpdateComment(val content: String) : FeedDetailAction
    data object SubmitComment : FeedDetailAction
    data object ToggleLike : FeedDetailAction
    data object ToggleFavorite : FeedDetailAction
    data class Report(val report: FeedReport) : FeedDetailAction
}

sealed interface FeedDetailEffect {
    data class ShowError(val message: String) : FeedDetailEffect
    data object CommentPublished : FeedDetailEffect
    data object ReportSubmitted : FeedDetailEffect
}

@HiltViewModel class FeedDetailViewModel @Inject constructor(private val repository: FeedRepository) : BaseMviViewModel<FeedDetailUiState, FeedDetailAction, FeedDetailEffect>(
    initialState = FeedDetailUiState(),
) {

    override fun onAction(action: FeedDetailAction) {
        when (action) {
            is FeedDetailAction.Load -> load(action.postId)
            is FeedDetailAction.UpdateComment -> updateState { it.copy(commentInput = action.content) }
            FeedDetailAction.SubmitComment -> submitComment()
            FeedDetailAction.ToggleLike -> updateLike()
            FeedDetailAction.ToggleFavorite -> updateFavorite()
            is FeedDetailAction.Report -> report(action.report)
        }
    }

    private fun load(postId: String) = viewModelScope.launch {
        updateState { FeedDetailUiState(isLoading = true) }
        val post = repository.getPost(postId)
        val comments = repository.getComments(postId, page = 0, pageSize = 20)
        updateState {
            FeedDetailUiState(
                post = post.getOrNull(),
                comments = comments.getOrDefault(FeedPage(emptyList())).items,
                isLoading = false,
            )
        }
        (post.exceptionOrNull() ?: comments.exceptionOrNull())?.let { error ->
            sendEffect(FeedDetailEffect.ShowError(error.message ?: "动态详情加载失败"))
        }
    }

    private fun submitComment() = performAction(FeedPostActionState.SUBMITTING_COMMENT) { post ->
        repository.createComment(post.id, FeedCommentDraft(currentState.commentInput)).onSuccess { comment ->
            updateState { state ->
                state.copy(
                    post = post.copy(commentCount = post.commentCount + 1),
                    comments = state.comments + comment,
                    commentInput = "",
                )
            }
            sendEffect(FeedDetailEffect.CommentPublished)
        }.map { Unit }
    }

    private fun updateLike() = performAction(FeedPostActionState.LIKING) { post ->
        repository.setLiked(post.id, !post.isLiked).onSuccess { updated ->
            updateState { it.copy(post = updated) }
        }.map { Unit }
    }

    private fun updateFavorite() = performAction(FeedPostActionState.FAVORITING) { post ->
        repository.setFavorited(post.id, !post.isFavorited).onSuccess { updated ->
            updateState { it.copy(post = updated) }
        }.map { Unit }
    }

    private fun report(report: FeedReport) = performAction(FeedPostActionState.REPORTING) { post ->
        repository.report(post.id, report).onSuccess { sendEffect(FeedDetailEffect.ReportSubmitted) }.map { Unit }
    }

    /** 所有互动操作收口在此：先写入进行中状态，失败时仅发送副作用，避免覆盖已展示的详情数据。 */
    private fun performAction(
        actionState: FeedPostActionState,
        request: suspend (DynamicPost) -> Result<Unit>,
    ) = viewModelScope.launch {
        val post = currentState.post ?: return@launch
        if (currentState.actionState != FeedPostActionState.IDLE) return@launch
        updateState { it.copy(actionState = actionState) }
        request(post).onFailure { error ->
            sendEffect(FeedDetailEffect.ShowError(error.message ?: "动态操作失败"))
        }
        updateState { it.copy(actionState = FeedPostActionState.IDLE) }
    }
}
