package com.sakuya.profile.data.repository

import android.content.Context
import android.net.Uri
import com.sakuya.profile.data.remote.ProfileApiService
import com.sakuya.profile.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Multipart
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val apiService: ProfileApiService,
    @ApplicationContext private val context: Context
){
    private var cachedProfile: UserProfile = UserProfile.Companion.empty()

    suspend fun getUserProfile(): UserProfile {
        delay(300)
        return cachedProfile
    }

    suspend fun updateUserProfile(updatedProfile: UserProfile){
        delay(300)
        cachedProfile = updatedProfile
    }

    suspend fun uploadAvatar(fileUri: Uri): Result<String> = withContext(Dispatchers.IO){
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure((Exception("无法打开文件流")))
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.size > 5 * 1024 * 1024) {
                return@withContext Result.failure(Exception("文件不能超过 5MB"))
            }

            val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull(),0,bytes.size)
            val body = MultipartBody.Part.createFormData(
                name = "image",
                filename = "avatar.jpg",
                body = requestFile
            )
            val response = apiService.uploadAvatar(body)
            if(response.isSuccessful && response.body() != null){
                val baseResponse = response.body()!!
                if(baseResponse.isSuccess()){
                    val avatarUrl = baseResponse.data ?: ""
                    if (avatarUrl.isNotEmpty()){
                        Result.success(avatarUrl)
                    }else{
                        Result.failure(Exception(" 服务器返回错误"))
                    }
                }else{
                    Result.failure(Exception(response.body()?.message ?: " 上传失败"))
                }
            }else{
                Result.failure(Exception(response.body()?.message ?: " 服务器返回错误"))
            }
        }catch (e: Exception){
            Result.failure(e)
        }
    }

}