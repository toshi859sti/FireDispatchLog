package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // Unix timestamp
    val eventName: String,
    val allowanceIndex: Int // 1-4
)
