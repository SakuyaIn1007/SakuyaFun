package com.sakuya.profileservices.data

import androidx.annotation.DrawableRes

data class WalletItem(
    @DrawableRes val iconRes: Int,
    val text: String
)

data class GroupedWallet(
    val groupName: String,
    val wallets: List<WalletItem>
)