package com.sakuya.profile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.profile.BuildConfig
import com.sakuya.profile.data.mock.ProfileMockData
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileMeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel(){
    private val _userProfile = MutableStateFlow(UserProfile.Companion.empty())
    val userProfile: StateFlow<UserProfile> = _userProfile

    private val _regionList = MutableStateFlow<List<RegionItem>>(emptyList())
    val regionList: StateFlow<List<RegionItem>> = _regionList.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileMeEffect>()
    val effect = _effect.asSharedFlow()
//    初始化
    init{
        loadUserProfile()
        fetchRegionList()
    }
//    读取userProfile的数据
    fun loadUserProfile(){
        viewModelScope.launch {
            userRepository.getUserProfile()
                .onSuccess { profile -> _userProfile.value = profile }
                .onFailure { error ->
                    _effect.emit(ProfileMeEffect.ShowError(error.message ?: "个人资料加载失败"))
                }
        }
    }
//    上传头像
    fun uploadAvatar(uri: Uri){
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            val result = userRepository.uploadAvatar(uri)

            result.onSuccess { newAvatarUrl ->
                val updatedProfile = _userProfile.value.copy(avatarUrl = newAvatarUrl)
                userRepository.updateUserProfile(updatedProfile)
                    .onSuccess { savedProfile ->
                        _userProfile.value = savedProfile
                        _uploadState.value = UploadState.Success(savedProfile.avatarUrl)
                    }
                    .onFailure { error ->
                        _uploadState.value = UploadState.Error(error.message ?: "头像保存失败")
                    }
            }.onFailure { error ->
                _uploadState.value = UploadState.Error(error.message ?: "上传失败")
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
//    拉取地区列表
    fun fetchRegionList(){
        viewModelScope.launch{
            try{
                _regionList.value = if (BuildConfig.DEV_PROFILE_MOCK_DATA) {
                    ProfileMockData.regions
                } else {
                    emptyList()
                }
            }catch (e: Exception){
                _regionList.value = emptyList()
            }
        }
    }

//    UI操作
    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnAvatarClick -> emitEffect(ProfileMeEffect.GoAvatar)

            is ProfileAction.OnNameClick -> emitEffect(ProfileMeEffect.GoName)

            is ProfileAction.OnGenderClick -> emitEffect(ProfileMeEffect.GoGender)

            is ProfileAction.OnRegionClick -> emitEffect(ProfileMeEffect.GoRegion)

            is ProfileAction.OnPhoneClick -> emitEffect(ProfileMeEffect.GoPhone)

            is ProfileAction.OnIdClick -> emitEffect(ProfileMeEffect.GoId)

            is ProfileAction.OnPokeClick -> emitEffect(ProfileMeEffect.GoPoke)

            is ProfileAction.OnSignatureClick -> emitEffect(ProfileMeEffect.GoSignature)

            is ProfileAction.OnRingtoneClick -> emitEffect(ProfileMeEffect.GoRingtone)
            else -> Unit
        }
    }
    private fun emitEffect(effect: ProfileMeEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
    fun onSignatureChanged(value: String){
        _userProfile.update {
            it.copy(signature = value)
        }
    }

    fun updateProfile(profile: UserProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.updateUserProfile(profile)
                .onSuccess { savedProfile ->
                    _userProfile.value = savedProfile
                    onSuccess()
                }
                .onFailure { error ->
                    _effect.emit(ProfileMeEffect.ShowError(error.message ?: "个人资料保存失败"))
                }
        }
    }

}

sealed interface ProfileMeEffect {
    data object GoAvatar : ProfileMeEffect
    data object GoName : ProfileMeEffect
    data object GoGender : ProfileMeEffect
    data object GoRegion : ProfileMeEffect
    data object GoPhone : ProfileMeEffect
    data object GoId : ProfileMeEffect
    data object GoPoke : ProfileMeEffect
    data object GoSignature : ProfileMeEffect
    data object GoRingtone : ProfileMeEffect
    data class ShowError(val message: String) : ProfileMeEffect
}
sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    data class Success(val avatarUrl: String) : UploadState()
    data class Error(val message: String) : UploadState()
}

data class RegionItem(
    val code: String,
    val name: String
)
