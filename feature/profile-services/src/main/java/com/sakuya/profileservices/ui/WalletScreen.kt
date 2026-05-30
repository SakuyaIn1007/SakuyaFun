package com.sakuya.profileservices.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.profileservices.data.GroupedWallet
import com.sakuya.profileservices.data.WalletItem
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme


/*
1.卡片内存放网格布局item的内容实现
普通Column + Row:
使用LazyColumn 遍历卡片内容，
用Row实现类似网格布局的内容。


*/



@Composable
fun WalletScreen(){

}
@Composable
fun WalletContent(
    modifier: Modifier = Modifier,
    groups: List<GroupedWallet>,
    onBack:() -> Unit = {}
){
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(
            title = "钱包",
            onBack = onBack
        )
        OutlinedCard(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                WalletContentItem(
                    text = "收付款",
                    iconRes = SakuyaIcons.Wallet,
                    modifier = Modifier.weight(1f)
                )
                WalletContentItem(
                    text = "零钱",
                    iconRes = SakuyaIcons.Wallet,
                    modifier = Modifier.weight(1f)
                )
            }
            }
        GroupedWalletGrid(groups)
    }
}

@Composable
fun GroupedWalletGrid(
    groups: List<GroupedWallet>,
    onWalletClick: (WalletItem) -> Unit = {}
){
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach{ group ->
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults
                        .elevatedCardColors
                            (containerColor = MaterialTheme.colorScheme.inverseOnSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = group.groupName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    WalletGridContent(
                        wallets = group.wallets,
                        onWalletClick = onWalletClick
                    )
                }
            }
        }
    }
}

@Composable
fun WalletGridContent(
    wallets: List<WalletItem>,
    onWalletClick: (WalletItem) -> Unit = {},
    columnsCount: Int = 4
){
    val actualColumns = if (columnsCount == -1) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        maxOf(2,(screenWidth / 90.dp).toInt())
    }else{
        columnsCount
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ){

        wallets.chunked(actualColumns).forEach { rowWallets ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Start
                ){
                    rowWallets.forEach { wallet ->
                        WalletContentItem(
                            text = wallet.text,
                            iconRes = wallet.iconRes,
                            onClick = {onWalletClick(wallet)},
                            modifier = Modifier.weight(1f)
                        )
                    }

//              通过计算剩余的格子数来填充剩余的格子进行占位
                val emptySlots = actualColumns - rowWallets.size
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                }
        }

    }

}

@Composable
fun WalletContentItem(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: ()-> Unit = {},
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
            .clickable{ onClick()}
            .fillMaxHeight()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = text,
            modifier = Modifier.size(36.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text
            , style = MaterialTheme.typography.labelLarge
            , color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WalletItemPreview() {
    SakuyaInAndroidTheme(true) {
        WalletContentItem(
            text = "银行卡",
            iconRes = SakuyaIcons.Wallet,
            modifier = Modifier
        )
    }

}
@Preview(showBackground = true)
@Composable
fun WalletPreview(){
    SakuyaInAndroidTheme(true) {
        WalletContent(
            modifier = Modifier,
            listOf(
                GroupedWallet(
                    groupName = "金融理财",
                    listOf(WalletItem(SakuyaIcons.Favorites,"234"),WalletItem(SakuyaIcons.Cards,"123"),WalletItem(SakuyaIcons.Favorites,"234"))
                ),
                GroupedWallet(
                    groupName = "生活服务",
                    listOf(WalletItem(SakuyaIcons.Cards,"123"),WalletItem(SakuyaIcons.Favorites,"234"),WalletItem(SakuyaIcons.Cards,"123"),WalletItem(SakuyaIcons.Favorites,"234"),WalletItem(SakuyaIcons.Cards,"123"))
                )

            )
        )
    }
}
