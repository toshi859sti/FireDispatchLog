package com.firedispatch.log.data.backup

import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.data.entity.AppSettings
import com.firedispatch.log.data.entity.Attendance
import com.firedispatch.log.data.entity.Event
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.entity.RoleMemberCount
import com.firedispatch.log.data.entity.Transaction

data class BackupData(
    val version: Int = 2, // バージョンを2に更新（会計データ追加）
    val timestamp: Long = System.currentTimeMillis(),
    val members: List<Member> = emptyList(),
    val roleAssignments: List<RoleAssignment> = emptyList(),
    val roleMemberCounts: List<RoleMemberCount> = emptyList(),
    val events: List<Event> = emptyList(),
    val attendances: List<Attendance> = emptyList(),
    val settings: List<AppSettings> = emptyList(),
    // 会計データ
    val fiscalYears: List<FiscalYear> = emptyList(),
    val accountCategories: List<AccountCategory> = emptyList(),
    val accountSubCategories: List<AccountSubCategory> = emptyList(),
    val transactions: List<Transaction> = emptyList()
)
