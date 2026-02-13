package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fiscal_years")
data class FiscalYear(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val year: Int,           // 例: 2025（2025年度）
    val startDate: Long,     // Unix timestamp（ミリ秒）
    val endDate: Long,       // Unix timestamp（ミリ秒）
    val carryOver: Int = 0,  // 繰越金（円単位）
    val isActive: Int = 0    // 1=現在の年度, 0=それ以外
)
