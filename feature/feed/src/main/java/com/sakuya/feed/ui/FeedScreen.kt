package com.sakuya.feed.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.feed.viewmodel.ComposeFeedViewModel
import com.sakuya.feed.viewmodel.ComposeFeedAction
import com.sakuya.feed.viewmodel.ComposeFeedEffect
import com.sakuya.feed.viewmodel.FeedDetailAction
import com.sakuya.feed.viewmodel.FeedDetailEffect
import com.sakuya.feed.viewmodel.FeedPostActionState
import com.sakuya.feed.viewmodel.FeedDetailViewModel
import com.sakuya.feed.viewmodel.PublicAuthorAction
import com.sakuya.feed.viewmodel.PublicAuthorEffect
import com.sakuya.feed.viewmodel.PublicAuthorViewModel
import com.sakuya.model.feed.FeedComment
import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.RelatedNovel
import com.sakuya.ui.component.DynamicTagRow
import com.sakuya.ui.motion.MotionSpec
import com.sakuya.ui.motion.rememberMotionPreferences
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun FeedDetailScreen(
    postId: String? = "recommended-1",
    onBack: () -> Unit = {},
    onBookClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FeedDetailViewModel = hiltViewModel(),
    authorViewModel: PublicAuthorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val authorUiState by authorViewModel.uiState.collectAsState()
    val post = uiState.post
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resolvedPostId = postId ?: "recommended-1"

    // 详情页每次接收新的路由参数时重载动态与评论，避免复用 ViewModel 后展示旧内容。
    LaunchedEffect(resolvedPostId) {
        viewModel.onAction(FeedDetailAction.Load(resolvedPostId))
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is FeedDetailEffect.ShowError) errorMessage = effect.message
        }
    }
    // 正文加载完成后再按作者 ID 请求公开资料，关注按钮因此始终以服务端关系状态为准。
    LaunchedEffect(post?.userId) {
        post?.userId?.takeIf(String::isNotBlank)?.let { authorViewModel.onAction(PublicAuthorAction.LoadProfile(it)) }
    }
    // 关注失败沿用他人主页的 Snackbar 反馈，不用本地回滚状态掩盖服务端错误。
    LaunchedEffect(authorViewModel) {
        authorViewModel.effect.collect { effect ->
            if (effect is PublicAuthorEffect.ShowError) snackbarHostState.showSnackbar(effect.message)
        }
    }

    when {
        uiState.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        post == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMessage ?: "动态不存在", color = MaterialTheme.colorScheme.outline)
        }
        else -> FeedDetailLoadedContent(
            post = requireNotNull(post),
            comments = uiState.comments,
            commentInput = uiState.commentInput,
            isSubmittingComment = uiState.actionState == FeedPostActionState.SUBMITTING_COMMENT,
            isLiking = uiState.actionState == FeedPostActionState.LIKING,
            isFavoriting = uiState.actionState == FeedPostActionState.FAVORITING,
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it },
            listState = listState,
            onBack = onBack,
            onShare = { shareDynamicPost(context, requireNotNull(post)) },
            onBookClick = onBookClick,
            onAuthorClick = onAuthorClick,
            onCommentInputChanged = { viewModel.onAction(FeedDetailAction.UpdateComment(it)) },
            onSubmitComment = { viewModel.onAction(FeedDetailAction.SubmitComment) },
            onToggleLike = { viewModel.onAction(FeedDetailAction.ToggleLike) },
            onToggleFavorite = { viewModel.onAction(FeedDetailAction.ToggleFavorite) },
            isFollowing = authorUiState.profile
                ?.takeIf { it.userId == post.userId }
                ?.isFollowing == true,
            isFollowStateReady = authorUiState.profile?.userId == post.userId && !authorUiState.isLoading,
            isOperatingFollow = authorUiState.isOperatingFollow,
            onToggleFollow = { authorViewModel.onAction(PublicAuthorAction.ToggleFollow) },
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
    }
}

/**
 * FeedDetailLoadedContent.kt
 * 职责说明：负责动态详情的已加载页面布局，并将评论输入与互动统计固定在底部操作栏。
 * 执行流程：FeedDetailScreen 提供已加载状态 -> Scaffold 渲染内容与 BottomBar -> 用户从底部完成评论、点赞与收藏。
 */
@Composable
private fun FeedDetailLoadedContent(
    post: DynamicPost,
    comments: List<FeedComment>,
    commentInput: String,
    isSubmittingComment: Boolean,
    isLiking: Boolean,
    isFavoriting: Boolean,
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onBookClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onCommentInputChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFollowing: Boolean,
    isFollowStateReady: Boolean,
    isOperatingFollow: Boolean,
    onToggleFollow: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val titleItemIndex =
        (if (post.imageColors.isNotEmpty()) 1 else 0) +
            (if (post.relatedNovel != null) 1 else 0) +
            1 // The author row precedes the title.
    val showCollapsedBar by remember(selectedSection, listState, titleItemIndex) {
        derivedStateOf { selectedSection == 0 && listState.firstVisibleItemIndex > titleItemIndex }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().dismissKeyboardOnTap(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            DynamicDetailTopBar(
                post = post,
                selectedSection = selectedSection,
                showAuthor = showCollapsedBar,
                onSectionSelected = onSectionSelected,
                onBack = onBack,
                onShare = onShare,
                isFollowing = isFollowing,
                isFollowStateReady = isFollowStateReady,
                isOperatingFollow = isOperatingFollow,
                onToggleFollow = onToggleFollow,
            )
        },
        bottomBar = {
            FeedInteractionBottomBar(
                post = post,
                commentInput = commentInput,
                isSubmittingComment = isSubmittingComment,
                isLiking = isLiking,
                isFavoriting = isFavoriting,
                onCommentInputChanged = onCommentInputChanged,
                onSubmitComment = onSubmitComment,
                onToggleLike = onToggleLike,
                onToggleFavorite = onToggleFavorite,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (selectedSection == 0) {
            DynamicDetailContent(
                post = post,
                comments = comments,
                commentInput = commentInput,
                isSubmittingComment = isSubmittingComment,
                isLiking = isLiking,
                isFavoriting = isFavoriting,
                listState = listState,
                onBookClick = onBookClick,
                onAuthorClick = onAuthorClick,
                onCommentInputChanged = onCommentInputChanged,
                onSubmitComment = onSubmitComment,
                onToggleLike = onToggleLike,
                onToggleFavorite = onToggleFavorite,
                isFollowing = isFollowing,
                isFollowStateReady = isFollowStateReady,
                isOperatingFollow = isOperatingFollow,
                onToggleFollow = onToggleFollow,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            DynamicCommentList(comments = comments, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun DynamicDetailTopBar(
    post: DynamicPost,
    selectedSection: Int,
    showAuthor: Boolean,
    onSectionSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    isFollowing: Boolean,
    isFollowStateReady: Boolean,
    isOperatingFollow: Boolean,
    onToggleFollow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        if (showAuthor) {
            DetailTopBarAuthor(
                post = post,
                isFollowing = isFollowing,
                isFollowStateReady = isFollowStateReady,
                isOperatingFollow = isOperatingFollow,
                onToggleFollow = onToggleFollow,
                modifier = Modifier.weight(1f),
            )
        } else {
            DetailSectionTabs(
                selectedSection = selectedSection,
                onSectionSelected = onSectionSelected,
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "转发")
        }
    }
}

/**
 * 动态分享职责：将动态标题、正文摘要与可识别的动态链接/ID 组装为纯文本，并交由 Android 系统选择分享目标。
 * 执行流程：点击详情页转发按钮 -> 构造 ACTION_SEND Intent -> 检查是否存在接收应用 -> 打开系统选择器或提示不可分享。
 */
private fun shareDynamicPost(context: Context, post: DynamicPost) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, post.title)
        putExtra(Intent.EXTRA_TEXT, buildDynamicShareText(post))
    }

    if (sendIntent.resolveActivity(context.packageManager) == null) {
        Toast.makeText(context, "未找到可分享的应用", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        context.startActivity(Intent.createChooser(sendIntent, "分享动态"))
    } catch (_: ActivityNotFoundException) {
        // 分享目标在点击后被卸载等边界情况也给予相同的明确反馈。
        Toast.makeText(context, "未找到可分享的应用", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 负责生成便于接收方阅读和识别的分享文本。
 * 执行流程：清理正文空白 -> 截取摘要避免文本过长 -> 拼接自定义动态链接与原始 ID，供支持该协议的客户端或人工检索使用。
 */
private fun buildDynamicShareText(post: DynamicPost): String {
    val summary = post.content.replace(Regex("\\s+"), " ").trim().let { content ->
        if (content.length > DYNAMIC_SHARE_SUMMARY_MAX_LENGTH) {
            "${content.take(DYNAMIC_SHARE_SUMMARY_MAX_LENGTH)}…"
        } else {
            content
        }
    }
    val dynamicLink = "sakuyain://feed/${Uri.encode(post.id)}"

    return buildString {
        append(post.title)
        if (summary.isNotBlank()) append("\n\n").append(summary)
        append("\n\n动态链接：").append(dynamicLink)
        append("\n动态 ID：").append(post.id)
    }
}

private const val DYNAMIC_SHARE_SUMMARY_MAX_LENGTH = 160

@Composable
private fun DetailSectionTabs(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        listOf("正文", "评论").forEachIndexed { index, title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selectedSection == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onSectionSelected(index) }.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun DetailTopBarAuthor(
    post: DynamicPost,
    isFollowing: Boolean,
    isFollowStateReady: Boolean,
    isOperatingFollow: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(post.authorColor)),
            contentAlignment = Alignment.Center,
        ) {
            Text(post.authorInitial, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(8.dp))
        Text(post.authorName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        FollowButton(
            isFollowing = isFollowing,
            isStateReady = isFollowStateReady,
            isOperating = isOperatingFollow,
            onClick = onToggleFollow,
        )
    }
}

@Composable
private fun DynamicDetailContent(
    post: DynamicPost,
    comments: List<FeedComment>,
    commentInput: String,
    isSubmittingComment: Boolean,
    isLiking: Boolean,
    isFavoriting: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onBookClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onCommentInputChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFollowing: Boolean,
    isFollowStateReady: Boolean,
    isOperatingFollow: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().dismissKeyboardOnTap(),
        state = listState,
    ) {
        if (post.imageColors.isNotEmpty()) {
            item { DynamicImagePager(post.imageColors) }
        }
        post.relatedNovel?.let { novel ->
            item {
                RelatedNovelCard(
                    novel = novel,
                    onClick = { onBookClick(novel.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        item {
            DetailAuthorRow(
                post = post,
                onAuthorClick = onAuthorClick,
                isFollowing = isFollowing,
                isFollowStateReady = isFollowStateReady,
                isOperatingFollow = isOperatingFollow,
                onToggleFollow = onToggleFollow,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        item {
            Text(
                text = post.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        item {
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        item { DynamicTagRow(post.tags, Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        item {
            Text(
                text = FeedTimeFormat.format(post.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
        item {
            Text(
                text = "评论 ${post.commentCount}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
        item { DynamicCommentList(comments = comments, showTitle = false) }
    }
}

/**
 * 动态详情底部固定操作栏。
 * 执行流程：输入框通过键盘完成动作提交评论；点赞、收藏和评论图标展示实时数量，点赞与收藏操作交给 ViewModel。
 */
@Composable
private fun FeedInteractionBottomBar(
    post: DynamicPost,
    commentInput: String,
    isSubmittingComment: Boolean,
    isLiking: Boolean,
    isFavoriting: Boolean,
    onCommentInputChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedBasicTextInput(
                value = commentInput,
                onValueChange = onCommentInputChanged,
                placeholder = { Text("来说点什么吧！") },
                singleLine = true,
                enabled = !isSubmittingComment,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (commentInput.isNotBlank() && !isSubmittingComment) onSubmitComment() },
                ),
                modifier = Modifier
                    .weight(1f)
                ,
            )
            Spacer(modifier = Modifier.width(10.dp))
            FeedInteractionIcon(
                icon = Icons.Default.ThumbUp,
                count = post.likeCount,
                selected = post.isLiked,
                enabled = !isLiking && !isFavoriting,
                contentDescription = "点赞",
                onClick = onToggleLike,
            )
            FeedInteractionIcon(
                icon = Icons.Default.Star,
                count = post.favoriteCount,
                selected = post.isFavorited,
                enabled = !isLiking && !isFavoriting,
                contentDescription = "收藏",
                onClick = onToggleFavorite,
            )
            FeedInteractionIcon(
                icon = null,
                count = post.commentCount,
                selected = false,
                enabled = false,
                contentDescription = "评论数",
                onClick = {},
            )
        }
    }
}

/** 底部栏图标统一按“图标在上、数量在下”渲染，避免互动数据分散在正文中。 */
@Composable
private fun FeedInteractionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val motionPreferences = rememberMotionPreferences()
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(if (motionPreferences.animationsEnabled) MotionSpec.QUICK_DURATION_MILLIS else 0),
        label = "feed-interaction-color",
    )
    val selectionScale by animateFloatAsState(
        targetValue = if (selected) MotionSpec.INTERACTIVE_SCALE else 1f,
        animationSpec = tween(if (motionPreferences.animationsEnabled) MotionSpec.QUICK_DURATION_MILLIS else 0),
        label = "feed-interaction-scale",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.scale(selectionScale),
                    tint = tint,
                )
            } else {
                CommentBubbleIcon(
                    color = tint,
                )
            }
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

/** 轻量自绘评论气泡，保持与参考模板的图标语义一致，且不依赖额外图标包。 */
@Composable
private fun CommentBubbleIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val bubbleHeight = size.height * 0.72f
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(size.width, bubbleHeight),
            cornerRadius = CornerRadius(size.width * 0.18f),
        )
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.30f, bubbleHeight - 1f)
                lineTo(size.width * 0.44f, size.height)
                lineTo(size.width * 0.50f, bubbleHeight - 1f)
                close()
            },
            color = color,
        )
    }
}

@Composable
private fun DynamicImagePager(imageColors: List<Long>) {
    val pagerState = rememberPagerState(pageCount = { imageColors.size })
    var previewPage by remember { mutableStateOf<Int?>(null) }
    Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            // 点击缩略内容只更新预览状态；全屏展示由下方 Dialog 统一处理。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(imageColors[page]))
                    .clickable { previewPage = page },
                contentAlignment = Alignment.Center,
            ) {
                Text("阅读日常", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
            }
        }
        Surface(
            color = Color.Black.copy(alpha = 0.58f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${imageColors.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
    previewPage?.let { page ->
        Dialog(onDismissRequest = { previewPage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(imageColors[page]))
                    .clickable { previewPage = null },
                contentAlignment = Alignment.Center,
            ) {
                Text("阅读日常", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun RelatedNovelCard(
    novel: RelatedNovel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 关联书籍卡片只派发点击事件，具体导航由上层统一处理。
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(width = 48.dp, height = 64.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(novel.title.take(2), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("正在评价 / 讨论", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(novel.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 2.dp))
                Text("${novel.author} · ${novel.description}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * DetailAuthorRow.kt
 * 职责说明：展示动态作者入口，并把关注点击交给详情页复用的 PublicAuthorViewModel。
 * 执行流程：作者资料接口提供初始关注状态 -> 点击后按钮进入禁用状态 -> 服务端成功值回写 UI，失败由页面 Snackbar 提示。
 */
@Composable
private fun DetailAuthorRow(
    post: DynamicPost,
    onAuthorClick: (String) -> Unit,
    isFollowing: Boolean,
    isFollowStateReady: Boolean,
    isOperatingFollow: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(post.authorColor)).clickable { onAuthorClick(post.userId) }, contentAlignment = Alignment.Center) {
            Text(post.authorInitial, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(10.dp))
        Text(post.authorName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).clickable { onAuthorClick(post.userId) })
        FollowButton(
            isFollowing = isFollowing,
            isStateReady = isFollowStateReady,
            isOperating = isOperatingFollow,
            onClick = onToggleFollow,
        )
    }
}

/** 关注按钮只渲染上层状态，不再用 remember 制造与服务端关系不一致的本地状态。 */
@Composable
private fun FollowButton(
    isFollowing: Boolean,
    isStateReady: Boolean,
    isOperating: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = isStateReady && !isOperating,
        colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Text(if (isOperating) "处理中" else if (isFollowing) "已关注" else "关注")
    }
}

@Composable
private fun DynamicCommentList(
    comments: List<FeedComment>,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth().dismissKeyboardOnTap()) {
        if (showTitle) {
            Text("评论 ${comments.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
        }
        comments.forEach { comment ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                    Text(comment.authorInitial, color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(comment.authorName, style = MaterialTheme.typography.labelLarge)
                    Text(comment.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                    Text(comment.publishedAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Preview 使用静态动态和空回调，确保系统分享逻辑不会依赖预览环境中的 Android Activity。 */
@Preview(showBackground = true)
@Composable
private fun FeedDetailLoadedContentPreview() {
    SakuyaInAndroidTheme {
        FeedDetailLoadedContent(
            post = DynamicPost(
                id = "preview-post",
                authorName = "月见草",
                authorInitial = "月",
                authorColor = 0xFF8B7BBE,
                title = "雨天读完《山茶文具店》",
                content = "像收到一封温柔的信，想把这份安静分享给同样喜欢阅读的人。",
                tags = listOf("读书", "随笔"),
                publishedAt = "刚刚",
            ),
            comments = emptyList(),
            commentInput = "",
            isSubmittingComment = false,
            isLiking = false,
            isFavoriting = false,
            selectedSection = 0,
            onSectionSelected = {},
            listState = rememberLazyListState(),
            onBack = {},
            onShare = {},
            onBookClick = {},
            onAuthorClick = {},
            onCommentInputChanged = {},
            onSubmitComment = {},
            onToggleLike = {},
            onToggleFavorite = {},
            isFollowing = false,
            isFollowStateReady = true,
            isOperatingFollow = false,
            onToggleFollow = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeFeedScreen(
    onPublished: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: ComposeFeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ComposeFeedEffect.Published -> onPublished()
                is ComposeFeedEffect.ShowError -> errorMessage = effect.message
            }
        }
    }
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("发表动态") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.onAction(ComposeFeedAction.Publish) },
                        enabled = uiState.content.isNotBlank() && !uiState.isPublishing,
                    ) { Text(if (uiState.isPublishing) "发布中" else "发布") }
                },
            )
        },
    ) { innerPadding ->
    Column(Modifier.fillMaxSize().dismissKeyboardOnTap().background(MaterialTheme.colorScheme.background).padding(innerPadding).padding(20.dp)) {
        FeedBasicTextInput(value = uiState.title, onValueChange = { viewModel.onAction(ComposeFeedAction.UpdateTitle(it)) }, placeholder = { Text("标题（可选）") }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), singleLine = true)
        FeedBasicTextInput(
            value = uiState.content,
            onValueChange = { viewModel.onAction(ComposeFeedAction.UpdateContent(it)) },
            placeholder = { Text("分享此刻的想法…") },
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            minLines = 6,
            enabled = !uiState.isPublishing,
        )
        FeedBasicTextInput(value = uiState.topicInput, onValueChange = { viewModel.onAction(ComposeFeedAction.UpdateTopic(it)) }, placeholder = { Text("话题（空格分隔）") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true)
        errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
    }
}

/**
 * FeedBasicTextInput.kt
 * 职责说明：统一动态模块所有输入框的 BasicTextField 外观与交互。
 * 执行流程：调用方提供状态与事件 -> BasicTextField 接收输入 -> decorationBox 绘制圆角容器和占位文案。
 */
@Composable
private fun FeedBasicTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    enabled: Boolean = true,

    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        textStyle = TextStyle(
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            Box { if (value.isBlank()) Box(Modifier.align(Alignment.CenterStart)) { placeholder() }; innerTextField() }
        }
    )
}

/**
 * 点击输入框以外区域时统一清除焦点并收起系统输入法。
 * 执行流程：空白区域收到轻触事件 -> FocusManager 清除输入焦点 -> KeyboardController 隐藏软键盘。
 */
@Composable
private fun Modifier.dismissKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return pointerInput(Unit) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
            keyboardController?.hide()
        })
    }
}
