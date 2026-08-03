package com.sakuya.profile.ui

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sakuya.profile.viewmodel.UploadState
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min


@Composable
fun AvatarEditContent(
    currentAvatarUrl: String,
    onImageSelected: (Uri) -> Unit,
    isUploading: Boolean = false,
    uploadState: UploadState = UploadState.Idle
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var croppingUri by remember { mutableStateOf<Uri?>(null) }
    var cropScale by remember { mutableStateOf(1f) }
    var cropOffset by remember { mutableStateOf(Offset.Zero) }
    var cropBoxSizePx by remember { mutableStateOf(0) }
    var isCropping by remember { mutableStateOf(false) }
    val actionsEnabled = !isUploading && uploadState !is UploadState.Loading
            && !isCropping

    fun startCrop(uri: Uri) {
        croppingUri = uri
        cropScale = 1f
        cropOffset = Offset.Zero
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { startCrop(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { startCrop(it) }
    }

    val openGallery = {
        if (actionsEnabled) {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    val openCamera = {
        if (actionsEnabled) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "avatar_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            if (uri != null) {
                cameraUri = uri
                takePictureLauncher.launch(uri)
            }
        }
    }

    val recentPhotos = remember { mutableStateListOf<Uri>() }
    var isLoadingPhotos by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingPhotos = true
        val uris = queryRecentPhotos(context)
        recentPhotos.clear()
        recentPhotos.addAll(uris)
        isLoadingPhotos = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (croppingUri != null) {
            AvatarCropContent(
                imageUri = croppingUri!!,
                scale = cropScale,
                offset = cropOffset,
                isCropping = isCropping,
                onScaleChange = { cropScale = it },
                onOffsetChange = { cropOffset = it },
                onSizeChanged = { cropBoxSizePx = it },
                onCancel = {
                    croppingUri = null
                    cropOffset = Offset.Zero
                    cropScale = 1f
                },
                onConfirm = {
                    val sourceUri = croppingUri ?: return@AvatarCropContent
                    isCropping = true
                    scope.launch {
                        val croppedUri = createCroppedAvatarUri(
                            context = context,
                            sourceUri = sourceUri,
                            cropScale = cropScale,
                            cropOffset = cropOffset,
                            cropBoxSizePx = cropBoxSizePx
                        )
                        isCropping = false
                        croppingUri = null
                        cropOffset = Offset.Zero
                        cropScale = 1f
                        croppedUri?.let(onImageSelected)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = currentAvatarUrl,
                        contentDescription = "当前头像",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "头像",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            when {
                isUploading || uploadState is UploadState.Loading -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "正在保存...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                uploadState is UploadState.Success -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "头像保存成功",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
                uploadState is UploadState.Error -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uploadState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        ActionItem(
            text = "拍照",
            enabled = actionsEnabled,
            onClick = openCamera
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        ActionItem(
            text = "从手机相册选择",
            enabled = actionsEnabled,
            onClick = openGallery
        )

        if (recentPhotos.isNotEmpty()) {
            HorizontalDivider(
                thickness = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(recentPhotos, key = { it }) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(enabled = actionsEnabled) { startCrop(uri) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoadingPhotos -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "正在加载图片...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = "暂无图片",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击上方按钮拍照或从相册选择",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarCropContent(
    imageUri: Uri,
    scale: Float,
    offset: Offset,
    isCropping: Boolean,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onSizeChanged: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cropBoxDp = 300.dp
    val maxDragPx = with(density) { 150.dp.toPx() } * scale

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "调整头像",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(cropBoxDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { onSizeChanged(min(it.width, it.height)) }
                .pointerInput(scale) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val next = offset + dragAmount
                        onOffsetChange(
                            Offset(
                                x = next.x.coerceIn(-maxDragPx, maxDragPx),
                                y = next.y.coerceIn(-maxDragPx, maxDragPx)
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "裁剪头像",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "拖动图片调整位置",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "缩放",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            valueRange = 1f..3f,
            enabled = !isCropping
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                enabled = !isCropping,
                onClick = onCancel
            ) {
                Text(text = "取消")
            }
            TextButton(
                enabled = !isCropping,
                onClick = onConfirm
            ) {
                if (isCropping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "使用")
                }
            }
        }
    }
}

private suspend fun createCroppedAvatarUri(
    context: Context,
    sourceUri: Uri,
    cropScale: Float,
    cropOffset: Offset,
    cropBoxSizePx: Int
): Uri? = withContext(Dispatchers.IO) {
    val source = decodeBitmap(context, sourceUri) ?: return@withContext null
    val zoom = cropScale.coerceIn(1f, 3f)
    val cropSide = (min(source.width, source.height) / zoom).toInt().coerceAtLeast(1)
    val boxSize = cropBoxSizePx.takeIf { it > 0 } ?: 1
    val centerX = (source.width / 2f - cropOffset.x / boxSize * cropSide)
        .coerceIn(cropSide / 2f, source.width - cropSide / 2f)
    val centerY = (source.height / 2f - cropOffset.y / boxSize * cropSide)
        .coerceIn(cropSide / 2f, source.height - cropSide / 2f)
    val left = (centerX - cropSide / 2f).toInt().coerceIn(0, max(0, source.width - cropSide))
    val top = (centerY - cropSide / 2f).toInt().coerceIn(0, max(0, source.height - cropSide))
    val crop = Bitmap.createBitmap(source, left, top, cropSide, cropSide)
    val output = Bitmap.createBitmap(AVATAR_OUTPUT_SIZE, AVATAR_OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
    Canvas(output).drawBitmap(
        crop,
        null,
        android.graphics.Rect(0, 0, AVATAR_OUTPUT_SIZE, AVATAR_OUTPUT_SIZE),
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    )
    val file = File(context.cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { stream ->
        output.compress(Bitmap.CompressFormat.JPEG, 92, stream)
    }
    if (crop != source) crop.recycle()
    output.recycle()
    source.recycle()
    Uri.fromFile(file)
}

private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(context.contentResolver, uri)
            ) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    } catch (e: Exception) {
        null
    }
}

private const val AVATAR_OUTPUT_SIZE = 512

private fun queryRecentPhotos(context: android.content.Context): List<Uri> {
    val uris = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bundle = android.os.Bundle().apply {
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, 30)
                putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, 0)
                putStringArray(
                    android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Images.Media.DATE_ADDED)
                )
                putInt(
                    android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
            }
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                bundle,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    uris.add(
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    )
                }
            }
        } else {
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                var count = 0
                while (cursor.moveToNext() && count < 30) {
                    val id = cursor.getLong(idColumn)
                    uris.add(
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    )
                    count++
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return uris
}

@Composable
private fun ActionItem(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun AvatarEditPreview() {
    SakuyaInAndroidTheme(true) {
        AvatarEditContent(
            currentAvatarUrl = "",
            onImageSelected = {}
        )
    }
}
