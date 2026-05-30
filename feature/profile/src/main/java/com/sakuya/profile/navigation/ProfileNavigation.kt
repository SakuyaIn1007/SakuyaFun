package com.sakuya.profile.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.conversation.navigation.CONVERSATION_ROUTE
import com.sakuya.friend.navigation.FRIEND_ROUTE
import com.sakuya.model.extentions.Gender
import com.sakuya.profile.ProfileAction
import com.sakuya.profile.effect.ProfileEffect
import com.sakuya.profile.effect.ProfileMeEffect
import com.sakuya.profile.ui.AvatarEditContent
import com.sakuya.profile.ui.ProfileEditScreen
import com.sakuya.profile.ui.ProfilePrivacyScreen
import com.sakuya.profile.ui.ProfileScreen
import com.sakuya.profile.ui.components.EditGenderContent
import com.sakuya.profile.ui.components.EditRegionContent
import com.sakuya.profile.ui.subpages.MyScreen
import com.sakuya.profile.viewmodel.ProfileMeViewModel
import com.sakuya.profile.viewmodel.ProfileViewModel
import com.sakuya.profile.viewmodel.UploadState
import com.sakuya.profileservices.navigation.PROFILE_ALBUMS_ROUTE
import com.sakuya.profileservices.navigation.PROFILE_CARDS_ROUTE
import com.sakuya.profileservices.navigation.PROFILE_FAVOURITES_ROUTE
import com.sakuya.profileservices.navigation.PROFILE_SETTINGS_ROUTE
import com.sakuya.profileservices.navigation.PROFILE_WALLET_ROUTE
import com.sakuya.profileservices.navigation.profileServicesNavGraph


fun NavGraphBuilder.profileNavGraph(navController: NavHostController) {
    composable(PROFILE_ROUTE) {
        val viewModel: ProfileViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        LaunchedEffect(Unit) {

            viewModel.effect.collect { effect ->

                when (effect) {

                    ProfileEffect.GoMe -> {
                        navController.navigate(PROFILE_ME)
                    }
                    ProfileEffect.GoConversation -> {
                        navController.navigate(CONVERSATION_ROUTE)
                    }
                    ProfileEffect.GoFriends -> {
                        navController.navigate(FRIEND_ROUTE)
                    }
                    ProfileEffect.GoFavourites -> {
                        navController.navigate(PROFILE_FAVOURITES_ROUTE)
                    }
                    ProfileEffect.GoCards -> {
                        navController.navigate(PROFILE_CARDS_ROUTE)
                    }
                    ProfileEffect.GoAlbums -> {
                        navController.navigate(PROFILE_ALBUMS_ROUTE)
                    }

                    ProfileEffect.GoSettings -> {
                        navController.navigate(PROFILE_SETTINGS_ROUTE)
                    }
                    ProfileEffect.GoWallet -> {
                        navController.navigate(PROFILE_WALLET_ROUTE)
                    }
                    else -> Unit
                }
            }
        }
        ProfileScreen(
            profile = profile,
            onAction = viewModel::onAction
        )
    }
//
    composable(PROFILE_ME){

        val viewModel: ProfileMeViewModel = hiltViewModel()

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->

                when(effect){
                    ProfileMeEffect.GoAvatar->{
                        navController.navigate(PROFILE_AVATAR)
                    }

                    ProfileMeEffect.GoName->{
                        navController.navigate(PROFILE_NAME)
                    }

                    else -> Unit
                }
            }
        }

        MyScreen(
            viewModel = viewModel,
            onAction = viewModel::onAction,
            onBack = { navController.popBackStack() }
            )
    }
//    修改头像
    composable(PROFILE_AVATAR){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val uploadState by viewModel.uploadState.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        ProfileEditScreen(
            title = "个人头像",
            onClick = {
                navController.popBackStack()
            },
            onBack = {navController.popBackStack()}
        ){
            AvatarEditContent(
                currentAvatarUrl = userProfile.avatarUrl,
                onImageSelected = { uri ->
                    viewModel.uploadAvatar(uri)
                },
                isUploading = uploadState is UploadState.Loading,
                uploadState = uploadState
            )
        }
    }
//    修改名称
    composable(PROFILE_NAME){

        val viewModel : ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()

        var inputName by remember(profile.nickname) { mutableStateOf(profile.nickname) }

        ProfileEditScreen(
            title = "更改名字",
            onBack = {navController.popBackStack()},
            onSave = {
               // viewModel.onAction(ProfileAction.SaveName(inputName))
                navController.popBackStack()
            }
        ){
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = inputName,
                onValueChange = {inputName = it},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
//    修改铃声
    composable(PROFILE_RINGTONE){

    }
//    修改电话号码
    composable(PROFILE_PHONE){
        val viewmodel: ProfileMeViewModel = hiltViewModel()
        val profile by viewmodel.userProfile.collectAsState()
        ProfileEditScreen(
            title = "更改电话",
            onBack = {navController.popBackStack()},
            onSave = {
                // viewModel.onAction(ProfileAction.SaveName(inputName))
                navController.popBackStack()
            }
        ){
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = profile.phoneNumber ?: "",
                onValueChange = { profile.phoneNumber = it},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
//    修改性别
    composable(PROFILE_GENDER){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        ProfileEditScreen(
            title = "性别",
            onBack = {navController.popBackStack()},
            onSave = {
                // viewModel.onAction(ProfileAction.SaveName(inputName))
                navController.popBackStack()
            }
        ){
            EditGenderContent(
                selected = profile.gender ?: Gender.UNKNOWN,
                onSelect = {newGender ->
                    profile.gender = newGender
                })
        }
    }
//    修改地区
    composable(PROFILE_REGION){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val regionList by viewModel.regionList.collectAsState()
        val profile by viewModel.userProfile.collectAsState()

        var currentSelectedCode by remember {
            mutableStateOf(profile.regionCode ?: "")
        }
        ProfileEditScreen(
            title = "更改名字",
            onBack = {navController.popBackStack()},
            onSave = {
                // viewModel.onAction(ProfileAction.SaveName(inputName))
                navController.popBackStack()
            }
        ){
            EditRegionContent(
                regionList = regionList,
                selectedCode = currentSelectedCode,
                onSelect = {
                    selectedRegion ->
                    currentSelectedCode = selectedRegion.code
                }
                )
        }
    }
//    修改个性签名
    composable(PROFILE_SIGNATURE){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        ProfileEditScreen(
            title = "更改名字",
            onBack = {navController.popBackStack()},
            onSave = {
                // viewModel.onAction(ProfileAction.SaveName(inputName))
                navController.popBackStack()
            }
        ){
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = profile.signature ?: "",
                onValueChange = viewModel::onSignatureChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    profileServicesNavGraph(navController)
}
