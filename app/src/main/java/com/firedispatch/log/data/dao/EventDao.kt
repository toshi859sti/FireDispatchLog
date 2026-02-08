package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.firedispatch.log.data.entity.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE date >= :startDate AND date <= :endDate ORDER BY date")
    fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
