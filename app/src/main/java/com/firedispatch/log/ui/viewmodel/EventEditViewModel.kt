package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.Attendance
import com.firedispatch.log.data.entity.Event
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.model.RoleType
import com.firedispatch.log.data.repository.EventRepository
import com.firedispatch.log.data.repository.MemberRepository
import com.firedispatch.log.data.repository.RoleRepository
import com.firedispatch.log.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class AttendanceMember(
    val member: Member,
    val roleType: RoleType,
    var attended: Boolean = false
)

class EventEditViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val memberRepository = MemberRepository(database.memberDao())
    private val roleRepository = RoleRepository(database.roleAssignmentDao(), database.roleMemberCountDao())
    private val eventRepository = EventRepository(database.eventDao(), database.attendanceDao())
    private val settingsRepository = SettingsRepository(database.appSettingsDao())

    private val _eventId = MutableStateFlow<Long>(-1)
    private val _date = MutableStateFlow(Calendar.getInstance().timeInMillis)
    val date: StateFlow<Long> = _date.asStateFlow()

    private val _eventName = MutableStateFlow("")
    val eventName: StateFlow<String> = _eventName.asStateFlow()

    private val _allowanceIndex = MutableStateFlow(1)
    val allowanceIndex: StateFlow<Int> = _allowanceIndex.asStateFlow()

    private val _attendanceMembers = MutableStateFlow<List<AttendanceMember>>(emptyList())
    val attendanceMembers: StateFlow<List<AttendanceMember>> = _attendanceMembers.asStateFlow()

    private val _fiscalYearStart = MutableStateFlow(0L)
    val fiscalYearStart: StateFlow<Long> = _fiscalYearStart.asStateFlow()

    private val _fiscalYearEnd = MutableStateFlow(0L)
    val fiscalYearEnd: StateFlow<Long> = _fiscalYearEnd.asStateFlow()

    init {
        loadFiscalYearRange()
    }

    private fun loadFiscalYearRange() {
        viewModelScope.launch {
            val fiscalYearStr = settingsRepository.getSettingValue(SettingsRepository.KEY_FISCAL_YEAR)
            val fiscalYear = fiscalYearStr?.toIntOrNull() ?: run {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                if (currentMonth >= 4) currentYear else currentYear - 1
            }

            _fiscalYearStart.value = Calendar.getInstance().apply {
                set(fiscalYear, Calendar.APRIL, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            _fiscalYearEnd.value = Calendar.getInstance().apply {
                set(fiscalYear + 1, Calendar.MARCH, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
    }

    fun loadEvent(eventId: Long) {
        _eventId.value = eventId

        if (eventId == -1L) {
            // 新規作成
            loadMembers()
        } else {
            // 既存編集
            viewModelScope.launch {
                val event = eventRepository.getEventById(eventId)
                if (event != null) {
                    _date.value = event.date
                    _eventName.value = event.eventName
                    _allowanceIndex.value = event.allowanceIndex

                    loadMembersWithAttendance(eventId)
                }
            }
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            combine(
                memberRepository.allMembers,
                roleRepository.allRoleAssignments
            ) { members, assignments ->
                val assignmentMap = assignments.associateBy { it.memberId }

                members.mapNotNull { member ->
                    val assignment = assignmentMap[member.id]
                    val roleType = assignment?.let { RoleType.valueOf(it.roleType) }
                    if (roleType != null && roleType != RoleType.HOJODAN) {
                        AttendanceMember(member, roleType, false)
                    } else null
                }.sortedBy { it.roleType.order }
            }.collect { attendanceMembers ->
                _attendanceMembers.value = attendanceMembers
            }
        }
    }

    private fun loadMembersWithAttendance(eventId: Long) {
        viewModelScope.launch {
            combine(
                memberRepository.allMembers,
                roleRepository.allRoleAssignments,
                eventRepository.getAttendanceByEvent(eventId)
            ) { members, assignments, attendances ->
                val assignmentMap = assignments.associateBy { it.memberId }
                val attendanceMap = attendances.associate { it.memberId to it.attended }

                members.mapNotNull { member ->
                    val assignment = assignmentMap[member.id]
                    val roleType = assignment?.let { RoleType.valueOf(it.roleType) }
                    if (roleType != null && roleType != RoleType.HOJODAN) {
                        AttendanceMember(
                            member,
                            roleType,
                            attendanceMap[member.id] ?: false
                        )
                    } else null
                }.sortedBy { it.roleType.order }
            }.collect { attendanceMembers ->
                _attendanceMembers.value = attendanceMembers
            }
        }
    }

    fun setDate(timestamp: Long) {
        _date.value = timestamp
    }

    fun setEventName(name: String) {
        _eventName.value = name
    }

    fun setAllowanceIndex(index: Int) {
        _allowanceIndex.value = index.coerceIn(1, 3)
    }

    fun toggleAttendance(memberId: Long) {
        val currentList = _attendanceMembers.value.toMutableList()
        val index = currentList.indexOfFirst { it.member.id == memberId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(attended = !currentList[index].attended)
            _attendanceMembers.value = currentList
        }
    }

    fun saveEvent(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val event = Event(
                id = if (_eventId.value == -1L) 0 else _eventId.value,
                date = _date.value,
                eventName = _eventName.value,
                allowanceIndex = _allowanceIndex.value
            )

            val savedEventId = if (_eventId.value == -1L) {
                eventRepository.insertEvent(event)
            } else {
                eventRepository.updateEvent(event)
                _eventId.value
            }

            // 出席情報を保存
            val attendances = _attendanceMembers.value.map { attendanceMember ->
                Attendance(
                    eventId = savedEventId,
                    memberId = attendanceMember.member.id,
                    attended = attendanceMember.attended
                )
            }

            eventRepository.updateEventAttendance(savedEventId, attendances)

            onSuccess()
        }
    }

    fun isValid(): Boolean {
        return _eventName.value.isNotBlank()
    }
}
