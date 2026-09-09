package com.sakuya.profile.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sakuya.data.local.dao.UserDao
import com.sakuya.data.local.entity.UserEntity
import com.sakuya.profile.data.remote.ProfileApiService
import com.sakuya.profile.data.remote.PrivacySettingsDto
import com.sakuya.profile.data.remote.ProfileDto
import com.sakuya.profile.data.remote.UpdateProfileRequest
import com.sakuya.profile.model.PrivacySettings
import com.sakuya.profile.model.UserProfile
import com.sakuya.model.extentions.Gender
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ProfileApiService,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
){
    suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProfile()
            val body = response.body()
            val data = body?.data
            if (!response.isSuccessful || body == null || !body.isSuccess() || data == null) {
                error(body?.message ?: "获取个人资料失败（HTTP ${response.code()}）")
            }
            val profile = data.toDomainModel()
            userDao.upsertUser(profile.toEntity())
            Result.success(profile)
        } catch (error: Exception) {
            val cached = userDao.getCurrentUser()?.toDomainModel()
            if (cached != null) Result.success(cached) else Result.failure(error)
        }
    }

    suspend fun updateUserProfile(updatedProfile: UserProfile): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.updateProfile(updatedProfile.toUpdateRequest())
                val body = response.body()
                val data = body?.data
                if (!response.isSuccessful || body == null || !body.isSuccess() || data == null) {
                    error(body?.message ?: "保存个人资料失败（HTTP ${response.code()}）")
                }
                data.toDomainModel().also { savedProfile ->
                    userDao.upsertUser(savedProfile.toEntity())
                }
            }
        }

    suspend fun uploadAvatar(fileUri: Uri): Result<String> = withContext(Dispatchers.IO){
        try {
            val size = context.contentResolver.query(
                fileUri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) cursor.getLong(sizeIndex) else null
            }

            if (size != null && size > MAX_AVATAR_BYTES) {
                return@withContext Result.failure(Exception("文件不能超过 5MB"))
            }

            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure((Exception("无法打开文件流")))
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.size > MAX_AVATAR_BYTES) {
                return@withContext Result.failure(Exception("文件不能超过 5MB"))
            }

            val mimeType = context.contentResolver.getType(fileUri) ?: "image/jpeg"
            val fileName = queryDisplayName(fileUri) ?: "avatar.jpg"
            val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull(), 0, bytes.size)
            val body = MultipartBody.Part.createFormData(
                name = "image",
                filename = fileName,
                body = requestFile
            )
            val response = apiService.uploadAvatar(body)

            if (response.isSuccessful && response.body() != null) {
                val baseResponse = response.body()!!
                if (baseResponse.isSuccess()) {
                    val avatarUrl = baseResponse.data ?: ""
                    if (avatarUrl.isNotEmpty()) {
                        Result.success(avatarUrl)
                    } else {
                        Result.failure(Exception("服务器未返回头像地址"))
                    }
                } else {
                    Result.failure(Exception(response.body()?.message ?: "上传失败"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "服务器返回错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }

    private companion object {
        const val MAX_AVATAR_BYTES = 5 * 1024 * 1024
    }
}

private fun ProfileDto.toDomainModel() = UserProfile(
    userId = userId,
    avatarUrl = avatarUrl,
    nickname = nickname,
    signature = signature,
    gender = gender?.let { value -> Gender.entries.firstOrNull { it.value == value } },
    birthday = birthday,
    regionCode = regionCode,
    phoneNumber = phoneNumber,
    email = email,
    pokeText = pokeText,
    ringtoneName = ringtoneName,
    privacySettings = privacySettings?.toDomainModel() ?: PrivacySettings(),
    followingCount = followingCount,
    followerCount = followerCount,
)

private fun PrivacySettingsDto.toDomainModel() = PrivacySettings(
    canBeAddedByStrangers = canBeAddedByStrangers,
    showProfileToStrangers = showProfileToStrangers,
    muteMessagesFromUnknown = muteMessagesFromUnknown
)

private fun UserProfile.toEntity() = UserEntity(
    id = userId,
    nickname = nickname,
    avatarUrl = avatarUrl,
    signature = signature,
    gender = gender,
    birthday = birthday,
    regionCode = regionCode,
    phoneNumber = phoneNumber,
    email = email,
    pokeText = pokeText,
    ringtoneName = ringtoneName,
    canBeAddedByStrangers = privacySettings.canBeAddedByStrangers,
    showProfileToStrangers = privacySettings.showProfileToStrangers,
    muteMessagesFromUnknown = privacySettings.muteMessagesFromUnknown
)

private fun UserEntity.toDomainModel() = UserProfile(
    userId = id,
    avatarUrl = avatarUrl.orEmpty(),
    nickname = nickname,
    signature = signature,
    gender = gender,
    birthday = birthday,
    regionCode = regionCode,
    phoneNumber = phoneNumber,
    email = email,
    pokeText = pokeText,
    ringtoneName = ringtoneName,
    privacySettings = PrivacySettings(
        canBeAddedByStrangers = canBeAddedByStrangers,
        showProfileToStrangers = showProfileToStrangers,
        muteMessagesFromUnknown = muteMessagesFromUnknown
    ),
    followingCount = 0,
    followerCount = 0,
)

private fun UserProfile.toUpdateRequest() = UpdateProfileRequest(
    avatarUrl = avatarUrl,
    nickname = nickname,
    signature = signature,
    gender = gender?.value,
    birthday = birthday,
    regionCode = regionCode,
    phoneNumber = phoneNumber,
    email = email,
    pokeText = pokeText,
    ringtoneName = ringtoneName,
    privacySettings = PrivacySettingsDto(
        canBeAddedByStrangers = privacySettings.canBeAddedByStrangers,
        showProfileToStrangers = privacySettings.showProfileToStrangers,
        muteMessagesFromUnknown = privacySettings.muteMessagesFromUnknown
    )
)
