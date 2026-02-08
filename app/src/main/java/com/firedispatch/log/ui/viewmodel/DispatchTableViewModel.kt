package com.firedispatch.log.ui.viewmodel

import android.app.Application
import android.net.Uri
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
import com.firedispatch.log.util.PdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class MemberRow(
    val member: Member,
    val roleType: RoleType
)

data class EventColumn(
    val event: Event,
    val attendanceMap: Map<Long, Boolean> // memberId -> attended
)

data class AttendanceSummary(
    val memberId: Long,
    val attendanceCount: Int,
    val allowanceTotal: Int
)

class DispatchTableViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val memberRepository = MemberRepository(database.memberDao())
    private val roleRepository = RoleRepository(database.roleAssignmentDao(), database.roleMemberCountDao())
    private val eventRepository = EventRepository(database.eventDao(), database.attendanceDao())
    private val settingsRepository = SettingsRepository(database.appSettingsDao())

    private val _memberRows = MutableStateFlow<List<MemberRow>>(emptyList())
    val memberRows: StateFlow<List<MemberRow>> = _memberRows.asStateFlow()

    private val _eventColumns = MutableStateFlow<List<EventColumn>>(emptyList())
    val eventColumns: StateFlow<List<EventColumn>> = _eventColumns.asStateFlow()

    private val _attendanceSummaries = MutableStateFlow<Map<Long, AttendanceSummary>>(emptyMap())
    val attendanceSummaries: StateFlow<Map<Long, AttendanceSummary>> = _attendanceSummaries.asStateFlow()

    private val _showSummary = MutableStateFlow(true)
    val showSummary: StateFlow<Boolean> = _showSummary.asStateFlow()

    private val _showCurrentYearOnly = MutableStateFlow(false)
    val showCurrentYearOnly: StateFlow<Boolean> = _showCurrentYearOnly.asStateFlow()

    private val _selectedEventId = MutableStateFlow<Long?>(null)
    val selectedEventId: StateFlow<Long?> = _selectedEventId.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                memberRepository.allMembers,
                roleRepository.allRoleAssignments,
                eventRepository.allEvents,
                _showCurrentYearOnly
            ) { members, assignments, events, currentYearOnly ->
                // メンバーを役職順に並べる（補助団員を除く）
                val assignmentMap = assignments.associateBy { it.memberId }
                val memberRows = members.mapNotNull { member ->
                    val assignment = assignmentMap[member.id]
                    val roleType = assignment?.let { RoleType.valueOf(it.roleType) }
                    if (roleType != null && roleType != RoleType.HOJODAN) {
                        MemberRow(member, roleType)
                    } else null
                }.sortedBy { it.roleType.order }

                // イベントをフィルタリング
                val filteredEvents = if (currentYearOnly) {
                    filterCurrentFiscalYear(events)
                } else {
                    events
                }

                Pair(memberRows, filteredEvents)
            }.collect { (memberRows, filteredEvents) ->
                _memberRows.value = memberRows

                // 各イベントの出席情報を取得
                val eventColumns = mutableListOf<EventColumn>()
                filteredEvents.forEach { event ->
                    val attendances = getAttendanceForEvent(event.id)
                    val attendanceMap = attendances.associate { it.memberId to it.attended }
                    eventColumns.add(EventColumn(event, attendanceMap))
                }

                _eventColumns.value = eventColumns

                // 集計を計算
                calculateSummaries(memberRows, eventColumns)
            }
        }
    }

    private suspend fun getAttendanceForEvent(eventId: Long): List<Attendance> {
        return eventRepository.getAttendanceByEvent(eventId).first()
    }

    private fun filterCurrentFiscalYear(events: List<Event>): List<Event> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        val fiscalYear = if (currentMonth >= 4) currentYear else currentYear - 1

        val fiscalYearStart = Calendar.getInstance().apply {
            set(fiscalYear, Calendar.APRIL, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val fiscalYearEnd = Calendar.getInstance().apply {
            set(fiscalYear + 1, Calendar.MARCH, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return events.filter { event ->
            event.date in fiscalYearStart..fiscalYearEnd
        }
    }

    private fun calculateSummaries(memberRows: List<MemberRow>, eventColumns: List<EventColumn>) {
        val summaries = mutableMapOf<Long, AttendanceSummary>()

        memberRows.forEach { memberRow ->
            var attendanceCount = 0
            var allowanceTotal = 0

            eventColumns.forEach { eventColumn ->
                val attended = eventColumn.attendanceMap[memberRow.member.id] ?: false
                if (attended) {
                    attendanceCount++
                    allowanceTotal += eventColumn.event.allowanceIndex
                }
            }

            summaries[memberRow.member.id] = AttendanceSummary(
                memberId = memberRow.member.id,
                attendanceCount = attendanceCount,
                allowanceTotal = allowanceTotal
            )
        }

        _attendanceSummaries.value = summaries
    }

    fun toggleShowSummary() {
        _showSummary.value = !_showSummary.value
    }

    fun toggleShowCurrentYearOnly() {
        _showCurrentYearOnly.value = !_showCurrentYearOnly.value
    }

    fun selectEvent(eventId: Long?) {
        _selectedEventId.value = eventId
    }

    fun deleteEvent(eventId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            val event = eventRepository.getEventById(eventId)
            if (event != null) {
                eventRepository.deleteEvent(event)
                _selectedEventId.value = null
                onComplete()
            }
        }
    }

    fun getSelectedEvent(): Event? {
        val eventId = _selectedEventId.value ?: return null
        return _eventColumns.value.find { it.event.id == eventId }?.event
    }

    fun exportPdf(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // 設定から組織名を取得
            val organizationName = settingsRepository.getSettingValue(SettingsRepository.KEY_ORGANIZATION_NAME) ?: "消防団"
            val title = "$organizationName 出動表"

            val result = PdfGenerator.generatePdf(
                context = getApplication(),
                uri = uri,
                title = title,
                memberRows = _memberRows.value,
                eventColumns = _eventColumns.value,
                attendanceSummaries = _attendanceSummaries.value
            )

            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "PDF出力に失敗しました")
            }
        }
    }
}
