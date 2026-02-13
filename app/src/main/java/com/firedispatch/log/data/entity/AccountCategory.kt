package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_categories")
data class AccountCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sortOrder: Int = 0,
    val name: String,
    val isIncome: Int,       // 1=収入, 0=支出
    val isEditable: Int = 1, // 1=編集可, 0=固定
    val outputOrder: Int = 0
)
