package com.sakuya.profileservices.data.remote

import android.content.res.Resources
import androidx.annotation.DrawableRes
import java.util.Date

data class UserFavourites(
    val userId: String,
    val favourites: List<FavouritesItemData>
)

data class FavouritesItemData(
    val title: String,
    val imageUrl: String,
    val resources: String,
    val date: Date
)