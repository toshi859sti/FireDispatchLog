package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fiscalYearId: Long,
    val isIncome: Int,           // 1=収入, 0=支出
    val date: Long,              // Unix timestamp（ミリ秒）
    val categoryId: Long,
    val subCategoryId: Long? = null,  // nullable（補助科目がない場合）
    val amount: Int,             // 円単位
    val memo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
