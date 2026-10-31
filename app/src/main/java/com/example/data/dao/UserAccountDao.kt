package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC")
    fun getAllUsersFlow(): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC")
    suspend fun getAllUsers(): List<UserAccount>

    @Query("SELECT * FROM user_accounts ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getLastActiveUser(): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE emailOrPhone = :emailOrPhone LIMIT 1")
    suspend fun getUserByEmailOrPhone(emailOrPhone: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Update
    suspend fun updateUser(user: UserAccount)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    @Query("DELETE FROM user_accounts")
    suspend fun clearAll()
}
