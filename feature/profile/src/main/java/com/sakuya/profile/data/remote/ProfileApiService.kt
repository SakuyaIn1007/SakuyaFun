package com.sakuya.profile.data.remote

import com.sakuya.model.network.BaseResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT

interface ProfileApiService {
    @GET("profile")
    suspend fun getProfile(): Response<BaseResponse<ProfileDto>>

    @PUT("profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<BaseResponse<ProfileDto>>

    @Multipart
    @POST("profile/upload/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<String>>
}

data class ProfileDto(
    val userId: String,
    val avatarUrl: String = "",
    val nickname: String,
    val signature: String? = null,
    val gender: Int? = null,
    val birthday: String? = null,
    val regionCode: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val pokeText: String? = null,
    val ringtoneName: String? = null,
    val privacySettings: PrivacySettingsDto? = null,
    val followingCount: Long = 0,
    val followerCount: Long = 0,
)

data class PrivacySettingsDto(
    val canBeAddedByStrangers: Boolean = true,
    val showProfileToStrangers: Boolean = true,
    val muteMessagesFromUnknown: Boolean = false
)

data class UpdateProfileRequest(
    val avatarUrl: String,
    val nickname: String,
    val signature: String?,
    val gender: Int?,
    val birthday: String?,
    val regionCode: String?,
    val phoneNumber: String?,
    val email: String?,
    val pokeText: String?,
    val ringtoneName: String?,
    val privacySettings: PrivacySettingsDto
)
