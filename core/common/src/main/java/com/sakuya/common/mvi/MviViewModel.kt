package com.sakuya.common.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * MviViewModel.kt
 * 职责说明：定义全项目统一的 MVI 契约与基类，消除各模块手写的 MutableStateFlow/Effect 通道样板。
 * 执行流程：UI 通过 onAction 提交用户意图 -> ViewModel 更新单一不可变 UiState -> 一次性副作用经 effect 通道下发。
 * 约定：
 * 1. UiState 只承载可重放的页面状态；Snackbar、导航等一次性事件必须走 effect。
 * 2. effect 使用 Channel(BUFFERED) 语义：只投递一次、进程重建不重放。
 * 3. 异步操作期间的状态只通过 updateState 修改，禁止在 ViewModel 外直接修改状态。
 */
interface MviViewModel<S : Any, A, E> {
    val uiState: StateFlow<S>
    val effect: Flow<E>

    /** UI 侧所有用户意图的统一入口。 */
    fun onAction(action: A)
}

abstract class BaseMviViewModel<S : Any, A, E>(
    initialState: S,
) : ViewModel(), MviViewModel<S, A, E> {

    private val _uiState = MutableStateFlow(initialState)
    final override val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    final override val effect: Flow<E> = _effect.receiveAsFlow()

    protected val currentState: S get() = _uiState.value

    /** 以不可变方式更新当前状态；重复值不会触发下游重发。 */
    protected fun updateState(transform: (S) -> S) {
        _uiState.update(transform)
    }

    /** 投递一次性副作用；无订阅者时事件被丢弃，避免页面重建后重放旧提示。 */
    protected fun sendEffect(value: E) {
        _effect.trySend(value)
    }
}
