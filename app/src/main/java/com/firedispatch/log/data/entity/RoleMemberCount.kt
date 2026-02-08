package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "role_member_counts")
data class RoleMemberCount(
    @PrimaryKey
    val roleType: String, // RoleType.name
    val count: Int
)
