package com.firedispatch.log.data.backup

import com.firedispatch.log.data.entity.AppSettings
import com.firedispatch.log.data.entity.Attendance
import com.firedispatch.log.data.entity.Event
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.entity.RoleMemberCount

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val members: List<Member> = emptyList(),
    val roleAssignments: List<RoleAssignment> = emptyList(),
    val roleMemberCounts: List<RoleMemberCount> = emptyList(),
    val events: List<Event> = emptyList(),
    val attendances: List<Attendance> = emptyList(),
    val settings: List<AppSettings> = emptyList()
)
