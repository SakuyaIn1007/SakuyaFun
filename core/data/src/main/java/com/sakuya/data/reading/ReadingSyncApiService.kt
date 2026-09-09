package com.sakuya.data.reading

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReadingSyncApiService {
    @POST("reading/sync")
    suspend fun sync(@Body request: ReadingSyncRequestDto): Response<BaseResponse<ReadingSyncResponseDto>>
}

data class ReadingSyncRequestDto(
    val deviceId: String,
    val progressChanges: List<ProgressMutationDto>,
    val bookmarkChanges: List<BookmarkMutationDto>,
)

data class ProgressMutationDto(
    val bookId: String,
    val contentType: String,
    val progress: Float,
    val chapterId: String?,
    val chapterIndex: Int,
    val chapterProgress: Float,
    val clientModifiedAt: String,
    val deleted: Boolean,
)

data class BookmarkMutationDto(
    val id: String,
    val bookId: String,
    val title: String,
    val progress: Float,
    val note: String,
    val createdAt: String,
    val chapterId: String?,
    val chapterProgress: Float,
    val clientModifiedAt: String,
    val deleted: Boolean,
)

data class ReadingSyncResponseDto(
    val serverTime: String,
    val progresses: List<ProgressDto> = emptyList(),
    val bookmarks: List<BookmarkDto> = emptyList(),
)

data class ProgressDto(
    val bookId: String,
    val contentType: String,
    val progress: Float,
    val chapterId: String?,
    val chapterIndex: Int,
    val chapterProgress: Float,
    val clientModifiedAt: String,
    val deviceId: String,
)

data class BookmarkDto(
    val id: String,
    val bookId: String,
    val title: String,
    val progress: Float,
    val note: String,
    val createdAt: String,
    val chapterId: String?,
    val chapterProgress: Float,
    val clientModifiedAt: String,
    val deviceId: String,
)
