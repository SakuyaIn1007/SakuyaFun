package com.sakuya.ui.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.sakuya.designsystem.icon.SakuyaIcons

/**
 * AvatarImage.kt
 * 职责说明：全应用统一的头像渲染出口，保证没有头像时各处显示同一张默认图。
 * 执行流程：调用方传入头像地址与尺寸 -> 地址为空直接使用默认头像 -> 地址非空时由 Coil 加载，
 * 加载中与加载失败同样回落到默认头像，因此任何情况下都不会出现空白或破图。
 *
 * 说明：默认头像资源登记在 core:designsystem 的 SakuyaIcons 中；feature 模块无法直接访问
 * designsystem 的 R 类，统一经由本组件渲染，不要再在业务代码里自行拼装首字母头像。
 */
@Composable
fun AvatarImage(
    modifier: Modifier = Modifier,
    /** 服务端返回的头像地址；为空或空白字符串时显示默认头像。 */
    avatarUrl: String? = null,
    /** 头像形状；列表与评论区通常为圆形，资料页可传入圆角矩形。 */
    shape: Shape = CircleShape,
    contentDescription: String? = null,
) {
    // placeholder 覆盖加载中，error 覆盖加载失败，fallback 覆盖地址为空，三者共用同一张默认头像。
    val defaultAvatar = painterResource(SakuyaIcons.Avatar)
    AsyncImage(
        model = avatarUrl?.takeIf { it.isNotBlank() },
        contentDescription = contentDescription,
        placeholder = defaultAvatar,
        error = defaultAvatar,
        fallback = defaultAvatar,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
    )
}
