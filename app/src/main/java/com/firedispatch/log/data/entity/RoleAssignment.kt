package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "role_assignments",
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memberId")]
)
data class RoleAssignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roleType: String, // RoleType.name
    val memberId: Long,
    val position: Int = 0 // 同じ役職内での順序（複数名の役職用）
)
