package com.sakuya.profile.navigation

import android.widget.Toast
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.navigation.CONVERSATION_ROUTE
import com.sakuya.navigation.FRIEND_ROUTE
import com.sakuya.navigation.LIBRARY_ROUTE
import com.sakuya.navigation.PROFILE_ALBUMS_ROUTE
import com.sakuya.navigation.PROFILE_AVATAR
import com.sakuya.navigation.PROFILE_CARDS_ROUTE
import com.sakuya.navigation.PROFILE_FAVOURITES_ROUTE
import com.sakuya.navigation.PROFILE_GENDER
import com.sakuya.navigation.PROFILE_ID
import com.sakuya.navigation.PROFILE_ME
import com.sakuya.navigation.PROFILE_NAME
import com.sakuya.navigation.PROFILE_PHONE
import com.sakuya.navigation.PROFILE_POKE
import com.sakuya.navigation.PROFILE_PRIVACY_ROUTE
import com.sakuya.navigation.PROFILE_READING_HISTORY
import com.sakuya.navigation.PROFILE_REGION
import com.sakuya.navigation.PROFILE_RINGTONE
import com.sakuya.navigation.PROFILE_ROUTE
import com.sakuya.navigation.PROFILE_SETTINGS_ROUTE
import com.sakuya.navigation.READER_ARG_BOOK_ID
import com.sakuya.navigation.READER_ARG_PATH
import com.sakuya.navigation.READER_BASE_ROUTE
import com.sakuya.navigation.PROFILE_SIGNATURE
import com.sakuya.navigation.PROFILE_WALLET_ROUTE
import com.sakuya.model.extentions.Gender
import com.sakuya.profile.ui.AvatarEditContent
import com.sakuya.profile.ui.ProfileEditScreen
import com.sakuya.profile.ui.ProfilePrivacyScreen
import com.sakuya.profile.ui.ProfileReadingHistoryScreen
import com.sakuya.profile.ui.ProfileScreen
import com.sakuya.profile.ui.components.EditGenderContent
import com.sakuya.profile.ui.components.EditRegionContent
import com.sakuya.profile.viewmodel.RegionItem
import com.sakuya.profile.ui.subpages.MyScreen
import com.sakuya.profile.viewmodel.ProfileEffect
import com.sakuya.profile.viewmodel.ProfileMeEffect
import com.sakuya.profile.viewmodel.ProfileMeViewModel
import com.sakuya.profile.viewmodel.ProfileViewModel
import com.sakuya.profile.viewmodel.UploadState
import com.sakuya.profileservices.navigation.profileServicesNavGraph


fun NavGraphBuilder.profileNavGraph(navController: NavHostController) {
    composable(PROFILE_ROUTE) {
        val viewModel: ProfileViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->

                when (effect) {

                    ProfileEffect.NavigateToMe -> {
                        navController.navigate(PROFILE_ME)
                    }
                    ProfileEffect.NavigateToConversation -> {
                        navController.navigate(CONVERSATION_ROUTE)
                    }
                    ProfileEffect.NavigateToFriends -> {
                        navController.navigate(FRIEND_ROUTE)
                    }
                    ProfileEffect.NavigateToBookshelf -> {
                        navController.navigate(LIBRARY_ROUTE)
                    }
                    ProfileEffect.NavigateToReadingHistory -> {
                        navController.navigate(PROFILE_READING_HISTORY)
                    }
                    ProfileEffect.NavigateToFavourites -> {
                        navController.navigate(PROFILE_FAVOURITES_ROUTE)
                    }
                    ProfileEffect.NavigateToCards -> {
                        navController.navigate(PROFILE_CARDS_ROUTE)
                    }
                    ProfileEffect.NavigateToAlbums -> {
                        navController.navigate(PROFILE_ALBUMS_ROUTE)
                    }

                    ProfileEffect.NavigateToSettings -> {
                        navController.navigate(PROFILE_SETTINGS_ROUTE)
                    }
                    ProfileEffect.NavigateToPrivacy -> {
                        navController.navigate(PROFILE_PRIVACY_ROUTE)
                    }
                    ProfileEffect.NavigateToWallet -> {
                        navController.navigate(PROFILE_WALLET_ROUTE)
                    }
                    is ProfileEffect.showToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        ProfileScreen(
            profile = profile,
            onAction = viewModel::onAction,
            onOpenReader = { bookId, filePath ->
                navController.navigate(
                    "$READER_BASE_ROUTE?$READER_ARG_BOOK_ID=${Uri.encode(bookId)}&$READER_ARG_PATH=${Uri.encode(filePath)}"
                )
            }
        )
    }
//
    composable(PROFILE_ME){

        val viewModel: ProfileMeViewModel = hiltViewModel()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->

                when(effect){
                    ProfileMeEffect.GoAvatar->{
                        navController.navigate(PROFILE_AVATAR)
                    }

                    ProfileMeEffect.GoName->{
                        navController.navigate(PROFILE_NAME)
                    }

                    ProfileMeEffect.GoGender -> {
                        navController.navigate(PROFILE_GENDER)
                    }

                    ProfileMeEffect.GoRegion -> {
                        navController.navigate(PROFILE_REGION)
                    }

                    ProfileMeEffect.GoPhone -> {
                        navController.navigate(PROFILE_PHONE)
                    }

                    ProfileMeEffect.GoId -> {
                        navController.navigate(PROFILE_ID)
                    }

                    ProfileMeEffect.GoPoke -> {
                        navController.navigate(PROFILE_POKE)
                    }

                    ProfileMeEffect.GoSignature -> {
                        navController.navigate(PROFILE_SIGNATURE)
                    }

                    ProfileMeEffect.GoRingtone -> {
                        navController.navigate(PROFILE_RINGTONE)
                    }
                    is ProfileMeEffect.ShowError -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        MyScreen(
            viewModel = viewModel,
            onAction = viewModel::onAction,
            onBack = { navController.popBackStack() }
            )
    }
    composable(PROFILE_READING_HISTORY) {
        ProfileReadingHistoryScreen(
            onBack = { navController.popBackStack() }
        )
    }
    composable(PROFILE_PRIVACY_ROUTE) {
        val viewModel: ProfileViewModel = hiltViewModel()
        ProfilePrivacyScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
//    修改头像
    composable(PROFILE_AVATAR){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val uploadState by viewModel.uploadState.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        LaunchedEffect(Unit) {
            viewModel.resetUploadState()
        }
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
            showSave = true,
            saveEnabled = inputName.isNotBlank(),
            onSave = {
                viewModel.updateProfile(profile.copy(nickname = inputName.trim())) {
                    navController.popBackStack()
                }
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
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        val ringtoneOptions = remember {
            listOf(
                RegionItem("默认铃声", "默认铃声"),
                RegionItem("清晨", "清晨"),
                RegionItem("星河", "星河"),
                RegionItem("静音", "静音")
            )
        }
        var selectedRingtone by remember(profile.ringtoneName) {
            mutableStateOf(profile.ringtoneName ?: "默认铃声")
        }
        ProfileEditScreen(
            title = "电话铃声",
            onBack = { navController.popBackStack() },
            showSave = true,
            onSave = {
                viewModel.updateProfile(profile.copy(ringtoneName = selectedRingtone)) {
                    navController.popBackStack()
                }
            }
        ) {
            EditRegionContent(
                regionList = ringtoneOptions,
                selectedCode = selectedRingtone,
                onSelect = { selectedRingtone = it.code }
            )
        }
    }
//    修改电话号码
    composable(PROFILE_PHONE){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        var inputPhone by remember(profile.phoneNumber) {
            mutableStateOf(profile.phoneNumber ?: "")
        }
        val phoneValid = inputPhone.isBlank() || Regex("^1\\d{10}$").matches(inputPhone)
        ProfileEditScreen(
            title = "更改电话",
            onBack = {navController.popBackStack()},
            showSave = true,
            saveEnabled = phoneValid,
            onSave = {
                viewModel.updateProfile(profile.copy(phoneNumber = inputPhone.trim())) {
                    navController.popBackStack()
                }
            }
        ){
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = inputPhone,
                onValueChange = { inputPhone = it.filter(Char::isDigit).take(11) },
                modifier = Modifier.fillMaxWidth()
            )
            if (!phoneValid) {
                Text(
                    text = "请输入 11 位手机号",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
//    修改性别
    composable(PROFILE_GENDER){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        var selectedGender by remember(profile.gender) {
            mutableStateOf(profile.gender ?: Gender.UNKNOWN)
        }
        ProfileEditScreen(
            title = "性别",
            onBack = {navController.popBackStack()},
            showSave = true,
            onSave = {
                viewModel.updateProfile(profile.copy(gender = selectedGender)) {
                    navController.popBackStack()
                }
            }
        ){
            EditGenderContent(
                selected = selectedGender,
                onSelect = {newGender ->
                    selectedGender = newGender
                })
        }
    }
//    修改地区
    composable(PROFILE_REGION){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val regionList by viewModel.regionList.collectAsState()
        val profile by viewModel.userProfile.collectAsState()

        var currentSelectedCode by remember(profile.regionCode) {
            mutableStateOf(profile.regionCode ?: "")
        }
        ProfileEditScreen(
            title = "地区",
            onBack = {navController.popBackStack()},
            showSave = true,
            saveEnabled = currentSelectedCode.isNotBlank(),
            onSave = {
                viewModel.updateProfile(profile.copy(regionCode = currentSelectedCode)) {
                    navController.popBackStack()
                }
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
//    ID详情
    composable(PROFILE_ID) {
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        ProfileEditScreen(
            title = "ID",
            onBack = { navController.popBackStack() }
        ) {
            Text(
                text = profile.userId,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
            Text(
                text = "ID 是账号的唯一标识，不能修改。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "复制 ID",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(profile.userId))
                        Toast
                            .makeText(context, "已复制 ID", Toast.LENGTH_SHORT)
                            .show()
                    }
            )
        }
    }
//    修改戳一下
    composable(PROFILE_POKE) {
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        var inputPoke by remember(profile.pokeText) {
            mutableStateOf(profile.pokeText ?: "")
        }
        ProfileEditScreen(
            title = "戳一下",
            onBack = { navController.popBackStack() },
            showSave = true,
            saveEnabled = inputPoke.length <= 20,
            onSave = {
                viewModel.updateProfile(profile.copy(pokeText = inputPoke.trim())) {
                    navController.popBackStack()
                }
            }
        ) {
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = inputPoke,
                onValueChange = { inputPoke = it.take(20) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "别人戳你时会看到这句话。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
//    修改个性签名
    composable(PROFILE_SIGNATURE){
        val viewModel: ProfileMeViewModel = hiltViewModel()
        val profile by viewModel.userProfile.collectAsState()
        var inputSignature by remember(profile.signature) {
            mutableStateOf(profile.signature ?: "")
        }
        ProfileEditScreen(
            title = "个性签名",
            onBack = {navController.popBackStack()},
            showSave = true,
            saveEnabled = inputSignature.length <= 40,
            onSave = {
                viewModel.updateProfile(profile.copy(signature = inputSignature.trim())) {
                    navController.popBackStack()
                }
            }
        ){
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = inputSignature,
                onValueChange = { inputSignature = it.take(40) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${inputSignature.length}/40",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    profileServicesNavGraph(navController)
}
