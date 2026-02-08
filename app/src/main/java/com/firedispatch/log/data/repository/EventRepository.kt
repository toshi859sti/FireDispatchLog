package com.firedispatch.log.data.repository

import com.firedispatch.log.data.dao.AttendanceDao
import com.firedispatch.log.data.dao.EventDao
import com.firedispatch.log.data.entity.Attendance
import com.firedispatch.log.data.entity.Event
import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao
) {
    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()

    fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<List<Event>> {
        return eventDao.getEventsByDateRange(startDate, endDate)
    }

    suspend fun getEventById(id: Long): Event? {
        return eventDao.getEventById(id)
    }

    suspend fun insertEvent(event: Event): Long {
        return eventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: Event) {
        eventDao.deleteEvent(event)
    }

    fun getAttendanceByEvent(eventId: Long): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceByEvent(eventId)
    }

    fun getAttendanceByMember(memberId: Long): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceByMember(memberId)
    }

    suspend fun getAttendance(eventId: Long, memberId: Long): Attendance? {
        return attendanceDao.getAttendance(eventId, memberId)
    }

    suspend fun insertAttendance(attendance: Attendance) {
        attendanceDao.insertAttendance(attendance)
    }

    suspend fun updateEventAttendance(eventId: Long, attendances: List<Attendance>) {
        attendanceDao.updateEventAttendance(eventId, attendances)
    }

    suspend fun deleteAllEvents() {
        eventDao.deleteAll()
    }

    suspend fun deleteAllAttendances() {
        attendanceDao.deleteAll()
    }
}
