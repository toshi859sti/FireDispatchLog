package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_sub_categories")
data class AccountSubCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentId: Long,      // 親科目のID
    val sortOrder: Int = 0,
    val name: String,
    val isEditable: Int = 1, // 補助科目は常に1（編集可）
    val outputOrder: Int = 0
)
