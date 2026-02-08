package com.firedispatch.log.data.backup

import android.content.Context
import android.net.Uri
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.repository.EventRepository
import com.firedispatch.log.data.repository.MemberRepository
import com.firedispatch.log.data.repository.RoleRepository
import com.firedispatch.log.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class BackupManager(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val memberRepository = MemberRepository(database.memberDao())
    private val roleRepository = RoleRepository(database.roleAssignmentDao(), database.roleMemberCountDao())
    private val eventRepository = EventRepository(database.eventDao(), database.attendanceDao())
    private val settingsRepository = SettingsRepository(database.appSettingsDao())

    suspend fun exportBackup(uri: Uri, context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val members = memberRepository.allMembers.first()
            val roleAssignments = roleRepository.allRoleAssignments.first()
            val roleMemberCounts = roleRepository.allRoleMemberCounts.first()
            val events = eventRepository.allEvents.first()

            // すべての出席記録を取得
            val allAttendances = mutableListOf<com.firedispatch.log.data.entity.Attendance>()
            events.forEach { event ->
                val attendances = eventRepository.getAttendanceByEvent(event.id).first()
                allAttendances.addAll(attendances)
            }

            // 設定は直接取得できないのでキーごとに取得
            val settings = listOf(
                settingsRepository.getSetting(SettingsRepository.KEY_FISCAL_YEAR).first(),
                settingsRepository.getSetting(SettingsRepository.KEY_ORGANIZATION_NAME).first()
            ).filterNotNull()

            val backupData = BackupData(
                members = members,
                roleAssignments = roleAssignments,
                roleMemberCounts = roleMemberCounts,
                events = events,
                attendances = allAttendances,
                settings = settings
            )

            val json = backupDataToJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            } ?: throw IOException("Failed to open output stream")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(uri: Uri, context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Failed to open input stream")

            val backupData = jsonToBackupData(jsonString)

            // すべてのデータをクリア
            database.clearAllTables()

            // データを復元
            memberRepository.insertMembers(backupData.members)

            backupData.roleAssignments.forEach { assignment ->
                roleRepository.insertRoleAssignment(assignment)
            }

            roleRepository.insertRoleMemberCounts(backupData.roleMemberCounts)

            backupData.events.forEach { event ->
                eventRepository.insertEvent(event)
            }

            backupData.attendances.groupBy { it.eventId }.forEach { (eventId, attendances) ->
                eventRepository.updateEventAttendance(eventId, attendances)
            }

            backupData.settings.forEach { setting ->
                settingsRepository.insertSetting(setting.key, setting.value)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun backupDataToJson(backupData: BackupData): String {
        val json = JSONObject()
        json.put("version", backupData.version)
        json.put("timestamp", backupData.timestamp)

        // Members
        val membersArray = JSONArray()
        backupData.members.forEach { member ->
            val memberJson = JSONObject()
            memberJson.put("id", member.id)
            memberJson.put("name", member.name)
            memberJson.put("phoneNumber", member.phoneNumber)
            memberJson.put("sortKey", member.sortKey)
            membersArray.put(memberJson)
        }
        json.put("members", membersArray)

        // RoleAssignments
        val roleAssignmentsArray = JSONArray()
        backupData.roleAssignments.forEach { assignment ->
            val assignmentJson = JSONObject()
            assignmentJson.put("id", assignment.id)
            assignmentJson.put("roleType", assignment.roleType)
            assignmentJson.put("memberId", assignment.memberId)
            assignmentJson.put("position", assignment.position)
            roleAssignmentsArray.put(assignmentJson)
        }
        json.put("roleAssignments", roleAssignmentsArray)

        // RoleMemberCounts
        val roleCountsArray = JSONArray()
        backupData.roleMemberCounts.forEach { count ->
            val countJson = JSONObject()
            countJson.put("roleType", count.roleType)
            countJson.put("count", count.count)
            roleCountsArray.put(countJson)
        }
        json.put("roleMemberCounts", roleCountsArray)

        // Events
        val eventsArray = JSONArray()
        backupData.events.forEach { event ->
            val eventJson = JSONObject()
            eventJson.put("id", event.id)
            eventJson.put("date", event.date)
            eventJson.put("eventName", event.eventName)
            eventJson.put("allowanceIndex", event.allowanceIndex)
            eventsArray.put(eventJson)
        }
        json.put("events", eventsArray)

        // Attendances
        val attendancesArray = JSONArray()
        backupData.attendances.forEach { attendance ->
            val attendanceJson = JSONObject()
            attendanceJson.put("eventId", attendance.eventId)
            attendanceJson.put("memberId", attendance.memberId)
            attendanceJson.put("attended", attendance.attended)
            attendancesArray.put(attendanceJson)
        }
        json.put("attendances", attendancesArray)

        // Settings
        val settingsArray = JSONArray()
        backupData.settings.forEach { setting ->
            val settingJson = JSONObject()
            settingJson.put("key", setting.key)
            settingJson.put("value", setting.value)
            settingsArray.put(settingJson)
        }
        json.put("settings", settingsArray)

        return json.toString(2)
    }

    private fun jsonToBackupData(jsonString: String): BackupData {
        val json = JSONObject(jsonString)

        val members = mutableListOf<com.firedispatch.log.data.entity.Member>()
        val membersArray = json.getJSONArray("members")
        for (i in 0 until membersArray.length()) {
            val memberJson = membersArray.getJSONObject(i)
            members.add(
                com.firedispatch.log.data.entity.Member(
                    id = memberJson.getLong("id"),
                    name = memberJson.getString("name"),
                    phoneNumber = memberJson.getString("phoneNumber"),
                    sortKey = memberJson.getString("sortKey")
                )
            )
        }

        val roleAssignments = mutableListOf<com.firedispatch.log.data.entity.RoleAssignment>()
        val roleAssignmentsArray = json.getJSONArray("roleAssignments")
        for (i in 0 until roleAssignmentsArray.length()) {
            val assignmentJson = roleAssignmentsArray.getJSONObject(i)
            roleAssignments.add(
                com.firedispatch.log.data.entity.RoleAssignment(
                    id = assignmentJson.getLong("id"),
                    roleType = assignmentJson.getString("roleType"),
                    memberId = assignmentJson.getLong("memberId"),
                    position = assignmentJson.getInt("position")
                )
            )
        }

        val roleMemberCounts = mutableListOf<com.firedispatch.log.data.entity.RoleMemberCount>()
        val roleCountsArray = json.getJSONArray("roleMemberCounts")
        for (i in 0 until roleCountsArray.length()) {
            val countJson = roleCountsArray.getJSONObject(i)
            roleMemberCounts.add(
                com.firedispatch.log.data.entity.RoleMemberCount(
                    roleType = countJson.getString("roleType"),
                    count = countJson.getInt("count")
                )
            )
        }

        val events = mutableListOf<com.firedispatch.log.data.entity.Event>()
        val eventsArray = json.getJSONArray("events")
        for (i in 0 until eventsArray.length()) {
            val eventJson = eventsArray.getJSONObject(i)
            events.add(
                com.firedispatch.log.data.entity.Event(
                    id = eventJson.getLong("id"),
                    date = eventJson.getLong("date"),
                    eventName = eventJson.getString("eventName"),
                    allowanceIndex = eventJson.getInt("allowanceIndex")
                )
            )
        }

        val attendances = mutableListOf<com.firedispatch.log.data.entity.Attendance>()
        val attendancesArray = json.getJSONArray("attendances")
        for (i in 0 until attendancesArray.length()) {
            val attendanceJson = attendancesArray.getJSONObject(i)
            attendances.add(
                com.firedispatch.log.data.entity.Attendance(
                    eventId = attendanceJson.getLong("eventId"),
                    memberId = attendanceJson.getLong("memberId"),
                    attended = attendanceJson.getBoolean("attended")
                )
            )
        }

        val settings = mutableListOf<com.firedispatch.log.data.entity.AppSettings>()
        val settingsArray = json.getJSONArray("settings")
        for (i in 0 until settingsArray.length()) {
            val settingJson = settingsArray.getJSONObject(i)
            settings.add(
                com.firedispatch.log.data.entity.AppSettings(
                    key = settingJson.getString("key"),
                    value = settingJson.getString("value")
                )
            )
        }

        return BackupData(
            version = json.getInt("version"),
            timestamp = json.getLong("timestamp"),
            members = members,
            roleAssignments = roleAssignments,
            roleMemberCounts = roleMemberCounts,
            events = events,
            attendances = attendances,
            settings = settings
        )
    }
}
