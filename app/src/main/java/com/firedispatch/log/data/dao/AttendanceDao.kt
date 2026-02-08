package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.firedispatch.log.data.entity.Attendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE eventId = :eventId")
    fun getAttendanceByEvent(eventId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId")
    fun getAttendanceByMember(memberId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE eventId = :eventId AND memberId = :memberId")
    suspend fun getAttendance(eventId: Long, memberId: Long): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<Attendance>)

    @Query("DELETE FROM attendance WHERE eventId = :eventId")
    suspend fun deleteAttendanceByEvent(eventId: Long)

    @Query("DELETE FROM attendance")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateEventAttendance(eventId: Long, attendances: List<Attendance>) {
        deleteAttendanceByEvent(eventId)
        insertAttendances(attendances)
    }
}
