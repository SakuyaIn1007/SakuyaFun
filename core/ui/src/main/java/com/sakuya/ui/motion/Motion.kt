package com.sakuya.ui.motion

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Motion.kt
 * 职责说明：
 * 1. 统一 Sakuya Compose 界面的时长、缓动曲线与可访问性动效开关。
 * 2. 将系统动画缩放偏好转换为 UI 可消费的只读 MotionPreferences。
 * 3. 提供状态切换、显隐和按压缩放等语义化入口，避免业务页面散落动画参数。
 *
 * 执行流程：读取系统动画缩放 -> rememberMotionPreferences 计算是否启用动效
 * -> 页面组件依据偏好选择过渡或直接落到终态。该文件只服务 UI 渲染，不参与 ViewModel 或数据层状态。
 */
object MotionSpec {
    const val QUICK_DURATION_MILLIS = 150
    const val STANDARD_DURATION_MILLIS = 220
    const val EMPHASIZED_DURATION_MILLIS = 280
    const val PRESS_SCALE = 0.96f
    const val INTERACTIVE_SCALE = 1.04f
}

/** UI 层只读动效偏好；系统关闭动画时仅保留状态本身，不保留过渡过程。 */
@Immutable
data class MotionPreferences(
    val animationsEnabled: Boolean,
)

/**
 * 读取系统动画缩放；值为 0 时代表用户要求减少动画，所有非必要动效应直接显示最终状态。
 */
@Composable
fun rememberMotionPreferences(): MotionPreferences {
    val context = LocalContext.current
    // 读取系统“动画时长缩放”设置，不需要业务层保存额外偏好。
    val durationScale = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    return remember(durationScale) {
        MotionPreferences(animationsEnabled = durationScale > 0f)
    }
}

/**
 * 通用内容状态容器。
 * targetState 改变时使用淡入淡出与尺寸过渡；关闭系统动画后以 None 过渡即时切换，
 * 用于 loading/content/empty/error 等互斥页面状态，避免整页闪烁。
 */
@Composable
fun <T> MotionContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val preferences = rememberMotionPreferences()
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            if (preferences.animationsEnabled) {
                androidx.compose.animation.fadeIn(
                    animationSpec = tween(MotionSpec.STANDARD_DURATION_MILLIS, easing = FastOutSlowInEasing),
                ) togetherWith androidx.compose.animation.fadeOut(
                    animationSpec = tween(MotionSpec.QUICK_DURATION_MILLIS, easing = FastOutSlowInEasing),
                )
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "motion-content",
        content = { state -> content(state) },
    )
}

/** 用于错误提示、编辑工具栏等短内容的显隐过渡。 */
@Composable
fun MotionVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val preferences = rememberMotionPreferences()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (preferences.animationsEnabled) {
            androidx.compose.animation.fadeIn(tween(MotionSpec.QUICK_DURATION_MILLIS)) +
                androidx.compose.animation.expandVertically(tween(MotionSpec.STANDARD_DURATION_MILLIS))
        } else {
            EnterTransition.None
        },
        exit = if (preferences.animationsEnabled) {
            androidx.compose.animation.fadeOut(tween(MotionSpec.QUICK_DURATION_MILLIS)) +
                androidx.compose.animation.shrinkVertically(tween(MotionSpec.QUICK_DURATION_MILLIS))
        } else {
            ExitTransition.None
        },
        label = "motion-visibility",
        content = { content() },
    )
}

/**
 * 首次加载使用静态骨架，明确表达等待状态且不产生循环动画。
 * 业务页可按内容密度指定行数，数据到达后由 MotionContent 切换为真实内容。
 */
@Composable
fun StaticLoadingSkeleton(
    modifier: Modifier = Modifier,
    rowCount: Int = 4,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(rowCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == rowCount - 1) 0.68f else 1f)
                    .height(if (index == 0) 112.dp else 64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    ),
            )
        }
    }
}

/**
 * 给已有可点击组件附加轻量按压反馈。
 * 点击语义仍由调用方的 Button、clickable 或 IconButton 提供，因此不会破坏无障碍信息。
 */
fun Modifier.motionPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier = composed {
    val preferences = rememberMotionPreferences()
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (enabled && preferences.animationsEnabled && pressed) MotionSpec.PRESS_SCALE else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(if (preferences.animationsEnabled) MotionSpec.QUICK_DURATION_MILLIS else 0),
        label = "motion-press-scale",
    )
    scale(scale)
}
