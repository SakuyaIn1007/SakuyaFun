package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface UserDao {
    @Query("SELECT * from user where id = :id")
    fun observeUser(id: String): Flow<UserEntity?>

    @Query("SELECT * from user where id = :id")
    fun getByUserId(id: String): UserEntity?

    @Query("SELECT * from user where id in (:userIds)")
    suspend fun getUsersByIds(userIds: List<String>): List<UserEntity>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Upsert
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("UPDATE user set signature = :newSignature where id = :userId")
    suspend fun updateSignature(userId: String, newSignature: String?)

    @Query("UPDATE user set nickname = :newNickname, avatarUrl = :newAvatar where id = :userId")
    suspend fun updateAvatarAndNickname(userId: String, newNickname: String, newAvatar: String?)
    @Query("DELETE from user")
    suspend fun clearAllUsers()
}