package com.sakuya.notification

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.sakuya.model.notification.*
import com.sakuya.navigation.*

/** 只把受控目标类型映射到既有路由；目标缺失时留在通知中心并给出提示。 */
fun NavGraphBuilder.notificationNavGraph(navController:NavHostController){composable(FNOTICE_ROUTE){val vm:NotificationViewModel=hiltViewModel();val items by vm.notifications.collectAsState();val refreshing by vm.refreshing.collectAsState();val hasMore by vm.hasMore.collectAsState();val context=LocalContext.current;LaunchedEffect(Unit){vm.effect.collect{Toast.makeText(context,it,Toast.LENGTH_SHORT).show()}};NotificationScreen(items,refreshing,hasMore,{navController.popBackStack()},vm::refresh,vm::markAllRead,vm::loadMore){item->vm.open(item){value->when(value.targetType){NotificationTargetType.FEED_POST->navController.navigate(feedDetailRoute(value.targetId));NotificationTargetType.USER->value.targetUserId?.let{navController.navigate(feedAuthorRoute(it))};NotificationTargetType.FRIEND_REQUEST->navController.navigate(FRIEND_REQUESTS_ROUTE);NotificationTargetType.BOOK_CHAPTER->navController.navigate(NotificationTargetRouteMapper.route(value.type,value.targetId,value.targetUserId))}}}}}
