package com.sakuya.profile.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.profile.ProfileAction
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.data.repository.UserRepository
import com.sakuya.profile.effect.ProfileMeEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
            val profile = userRepository.getUserProfile()
            _userProfile.value = profile
        }
    }
//    上传头像
    fun uploadAvatar(uri: Uri){
        viewModelScope.launch(Dispatchers.IO){
            _uploadState.value = UploadState.Loading
           val result = userRepository.uploadAvatar(uri)

            result.onSuccess { newAvatarUrl ->
                _uploadState.value = UploadState.Success(newAvatarUrl)
            }.onFailure { error ->
                _uploadState.value = UploadState.Error(result.exceptionOrNull()?.message ?: "上传失败")
            }
        }
    }
//    拉取地区列表
    fun fetchRegionList(){
        viewModelScope.launch{
            try{

            }catch (e: Exception){

            }
        }
    }

//    UI操作
    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.OnAvatarClick -> emitEffect(ProfileMeEffect.GoAvatar)

            ProfileAction.OnNameClick -> emitEffect(ProfileMeEffect.GoName)

            ProfileAction.OnGenderClick -> emitEffect(ProfileMeEffect.GoGender)

            ProfileAction.OnRegionClick -> emitEffect(ProfileMeEffect.GoRegion)

            ProfileAction.OnPhoneClick -> emitEffect(ProfileMeEffect.GoPhone)

            ProfileAction.OnIdClick -> emitEffect(ProfileMeEffect.GoId)

            ProfileAction.OnSignatureClick -> emitEffect(ProfileMeEffect.GoSignature)

            ProfileAction.OnRingtoneClick -> emitEffect(ProfileMeEffect.GoRingtone)
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
