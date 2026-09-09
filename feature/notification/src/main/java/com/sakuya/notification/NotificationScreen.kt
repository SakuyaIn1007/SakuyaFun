package com.sakuya.notification

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sakuya.model.notification.*
import com.sakuya.ui.component.AppSecondaryTopBar

/** 统一通知中心：下拉刷新、未读强调、全部已读，并把目标跳转交给导航层。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(items:List<AppNotification>,refreshing:Boolean,hasMore:Boolean,onBack:()->Unit,onRefresh:()->Unit,onReadAll:()->Unit,onLoadMore:()->Unit,onOpen:(AppNotification)->Unit){
    Scaffold(topBar={AppSecondaryTopBar(title="新通知",onBack=onBack,actions={TextButton(onClick=onReadAll,enabled=items.any{it.readAt==null}){Text("全部已读")}})}){padding->
        PullToRefreshBox(isRefreshing=refreshing,onRefresh=onRefresh,modifier=Modifier.fillMaxSize().padding(padding)){
            if(items.isEmpty()&&!refreshing)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("暂无通知",color=MaterialTheme.colorScheme.outline)}
            else LazyColumn(Modifier.fillMaxSize()){items(items,key={it.id}){item->NotificationRow(item,onOpen);HorizontalDivider(Modifier.padding(start=72.dp))};if(hasMore)item{TextButton(onClick=onLoadMore,modifier=Modifier.fillMaxWidth().padding(8.dp)){Text("加载更多")}}}
        }
    }
}

@Composable private fun NotificationRow(item:AppNotification,onOpen:(AppNotification)->Unit){
    val unread=item.readAt==null
    Row(Modifier.fillMaxWidth().background(if(unread)MaterialTheme.colorScheme.primaryContainer.copy(alpha=.18f)else MaterialTheme.colorScheme.surface).clickable{onOpen(item)}.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
        Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.secondaryContainer,modifier=Modifier.size(40.dp)){Box(contentAlignment=Alignment.Center){Icon(item.type.icon(),null,Modifier.size(22.dp))}}
        Spacer(Modifier.width(16.dp));Column(Modifier.weight(1f)){Row(verticalAlignment=Alignment.CenterVertically){Text(item.title,style=MaterialTheme.typography.titleSmall,fontWeight=if(unread)FontWeight.Bold else FontWeight.Medium,modifier=Modifier.weight(1f));if(unread)Badge()};Text(item.content,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(item.createdAt.replace("T"," ").take(16),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.outline,modifier=Modifier.padding(top=4.dp))}
    }
}
private fun NotificationType.icon():ImageVector=when(this){NotificationType.FEED_COMMENT,NotificationType.FEED_REPLY->Icons.Default.Comment;NotificationType.FEED_LIKE->Icons.Default.Favorite;NotificationType.FEED_FAVORITE->Icons.Default.Bookmark;NotificationType.NEW_FOLLOWER->Icons.Default.PersonAdd;NotificationType.FRIEND_REQUEST->Icons.Default.GroupAdd;NotificationType.FRIEND_ACCEPTED->Icons.Default.HowToReg;NotificationType.CHAPTER_UPDATE->Icons.Default.Bookmark}
