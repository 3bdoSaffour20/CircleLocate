package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val emailOrPhone: String,
    val pinCode: String = "1234",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isAutoLoginEnabled: Boolean = true,
    val avatarColorHex: String = "#00C853",
    val supabaseUid: String = "",
    val supabaseAccessToken: String = ""
)
