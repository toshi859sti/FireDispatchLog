package com.firedispatch.log.data.backup

import android.content.Context
import android.net.Uri
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.repository.AccountingRepository
import com.firedispatch.log.data.repository.BackgroundColorRepository
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
    private val accountingRepository = AccountingRepository(
        database.fiscalYearDao(),
        database.accountCategoryDao(),
        database.transactionDao()
    )
    private val backgroundColorRepository = BackgroundColorRepository(database.backgroundColorDao())

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

            // 会計データを取得
            val fiscalYears = accountingRepository.allFiscalYears.first()
            val incomeCategories = accountingRepository.getCategoriesByType(1).first()
            val expenseCategories = accountingRepository.getCategoriesByType(0).first()
            val accountCategories = incomeCategories + expenseCategories

            val allSubCategories = mutableListOf<com.firedispatch.log.data.entity.AccountSubCategory>()
            accountCategories.forEach { category ->
                val subCategories = accountingRepository.getSubCategoriesByParent(category.id).first()
                allSubCategories.addAll(subCategories)
            }

            val allTransactions = accountingRepository.allTransactions.first()

            // 背景色設定を取得
            val backgroundColorPresets = backgroundColorRepository.getAllPresets().first()
            val screenBackgroundMappings = backgroundColorRepository.getAllMappings().first()

            val backupData = BackupData(
                members = members,
                roleAssignments = roleAssignments,
                roleMemberCounts = roleMemberCounts,
                events = events,
                attendances = allAttendances,
                settings = settings,
                fiscalYears = fiscalYears,
                accountCategories = accountCategories,
                accountSubCategories = allSubCategories,
                transactions = allTransactions,
                backgroundColorPresets = backgroundColorPresets,
                screenBackgroundMappings = screenBackgroundMappings
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

            // 会計データを復元
            accountingRepository.insertFiscalYears(backupData.fiscalYears)
            accountingRepository.insertCategories(backupData.accountCategories)
            accountingRepository.insertSubCategories(backupData.accountSubCategories)
            accountingRepository.insertTransactions(backupData.transactions)

            // 背景色設定を復元
            backupData.backgroundColorPresets.forEach { preset ->
                backgroundColorRepository.insertPreset(preset)
            }
            backupData.screenBackgroundMappings.forEach { mapping ->
                backgroundColorRepository.setScreenMapping(mapping.screenName, mapping.presetId)
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

        // FiscalYears
        val fiscalYearsArray = JSONArray()
        backupData.fiscalYears.forEach { fiscalYear ->
            val fiscalYearJson = JSONObject()
            fiscalYearJson.put("id", fiscalYear.id)
            fiscalYearJson.put("year", fiscalYear.year)
            fiscalYearJson.put("startDate", fiscalYear.startDate)
            fiscalYearJson.put("endDate", fiscalYear.endDate)
            fiscalYearJson.put("carryOver", fiscalYear.carryOver)
            fiscalYearJson.put("isActive", fiscalYear.isActive)
            fiscalYearsArray.put(fiscalYearJson)
        }
        json.put("fiscalYears", fiscalYearsArray)

        // AccountCategories
        val categoriesArray = JSONArray()
        backupData.accountCategories.forEach { category ->
            val categoryJson = JSONObject()
            categoryJson.put("id", category.id)
            categoryJson.put("sortOrder", category.sortOrder)
            categoryJson.put("name", category.name)
            categoryJson.put("isIncome", category.isIncome)
            categoryJson.put("isEditable", category.isEditable)
            categoryJson.put("outputOrder", category.outputOrder)
            categoriesArray.put(categoryJson)
        }
        json.put("accountCategories", categoriesArray)

        // AccountSubCategories
        val subCategoriesArray = JSONArray()
        backupData.accountSubCategories.forEach { subCategory ->
            val subCategoryJson = JSONObject()
            subCategoryJson.put("id", subCategory.id)
            subCategoryJson.put("parentId", subCategory.parentId)
            subCategoryJson.put("sortOrder", subCategory.sortOrder)
            subCategoryJson.put("name", subCategory.name)
            subCategoryJson.put("isEditable", subCategory.isEditable)
            subCategoryJson.put("outputOrder", subCategory.outputOrder)
            subCategoriesArray.put(subCategoryJson)
        }
        json.put("accountSubCategories", subCategoriesArray)

        // Transactions
        val transactionsArray = JSONArray()
        backupData.transactions.forEach { transaction ->
            val transactionJson = JSONObject()
            transactionJson.put("id", transaction.id)
            transactionJson.put("fiscalYearId", transaction.fiscalYearId)
            transactionJson.put("isIncome", transaction.isIncome)
            transactionJson.put("date", transaction.date)
            transactionJson.put("categoryId", transaction.categoryId)
            transactionJson.put("subCategoryId", transaction.subCategoryId ?: JSONObject.NULL)
            transactionJson.put("amount", transaction.amount)
            transactionJson.put("memo", transaction.memo)
            transactionJson.put("createdAt", transaction.createdAt)
            transactionsArray.put(transactionJson)
        }
        json.put("transactions", transactionsArray)

        // BackgroundColorPresets
        val presetsArray = JSONArray()
        backupData.backgroundColorPresets.forEach { preset ->
            val presetJson = JSONObject()
            presetJson.put("id", preset.id)
            presetJson.put("name", preset.name)
            presetJson.put("color1", preset.color1)
            presetJson.put("color2", preset.color2)
            presetJson.put("color3", preset.color3)
            presetsArray.put(presetJson)
        }
        json.put("backgroundColorPresets", presetsArray)

        // ScreenBackgroundMappings
        val mappingsArray = JSONArray()
        backupData.screenBackgroundMappings.forEach { mapping ->
            val mappingJson = JSONObject()
            mappingJson.put("screenName", mapping.screenName)
            mappingJson.put("presetId", mapping.presetId)
            mappingsArray.put(mappingJson)
        }
        json.put("screenBackgroundMappings", mappingsArray)

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

        // 会計データ（バージョン2以降のみ）
        val fiscalYears = mutableListOf<com.firedispatch.log.data.entity.FiscalYear>()
        if (json.has("fiscalYears")) {
            val fiscalYearsArray = json.getJSONArray("fiscalYears")
            for (i in 0 until fiscalYearsArray.length()) {
                val fiscalYearJson = fiscalYearsArray.getJSONObject(i)
                fiscalYears.add(
                    com.firedispatch.log.data.entity.FiscalYear(
                        id = fiscalYearJson.getLong("id"),
                        year = fiscalYearJson.getInt("year"),
                        startDate = fiscalYearJson.getLong("startDate"),
                        endDate = fiscalYearJson.getLong("endDate"),
                        carryOver = fiscalYearJson.getInt("carryOver"),
                        isActive = fiscalYearJson.getInt("isActive")
                    )
                )
            }
        }

        val accountCategories = mutableListOf<com.firedispatch.log.data.entity.AccountCategory>()
        if (json.has("accountCategories")) {
            val categoriesArray = json.getJSONArray("accountCategories")
            for (i in 0 until categoriesArray.length()) {
                val categoryJson = categoriesArray.getJSONObject(i)
                accountCategories.add(
                    com.firedispatch.log.data.entity.AccountCategory(
                        id = categoryJson.getLong("id"),
                        sortOrder = categoryJson.getInt("sortOrder"),
                        name = categoryJson.getString("name"),
                        isIncome = categoryJson.getInt("isIncome"),
                        isEditable = categoryJson.getInt("isEditable"),
                        outputOrder = categoryJson.getInt("outputOrder")
                    )
                )
            }
        }

        val accountSubCategories = mutableListOf<com.firedispatch.log.data.entity.AccountSubCategory>()
        if (json.has("accountSubCategories")) {
            val subCategoriesArray = json.getJSONArray("accountSubCategories")
            for (i in 0 until subCategoriesArray.length()) {
                val subCategoryJson = subCategoriesArray.getJSONObject(i)
                accountSubCategories.add(
                    com.firedispatch.log.data.entity.AccountSubCategory(
                        id = subCategoryJson.getLong("id"),
                        parentId = subCategoryJson.getLong("parentId"),
                        sortOrder = subCategoryJson.getInt("sortOrder"),
                        name = subCategoryJson.getString("name"),
                        isEditable = subCategoryJson.getInt("isEditable"),
                        outputOrder = subCategoryJson.getInt("outputOrder")
                    )
                )
            }
        }

        val transactions = mutableListOf<com.firedispatch.log.data.entity.Transaction>()
        if (json.has("transactions")) {
            val transactionsArray = json.getJSONArray("transactions")
            for (i in 0 until transactionsArray.length()) {
                val transactionJson = transactionsArray.getJSONObject(i)
                transactions.add(
                    com.firedispatch.log.data.entity.Transaction(
                        id = transactionJson.getLong("id"),
                        fiscalYearId = transactionJson.getLong("fiscalYearId"),
                        isIncome = transactionJson.getInt("isIncome"),
                        date = transactionJson.getLong("date"),
                        categoryId = transactionJson.getLong("categoryId"),
                        subCategoryId = if (transactionJson.isNull("subCategoryId")) null else transactionJson.getLong("subCategoryId"),
                        amount = transactionJson.getInt("amount"),
                        memo = transactionJson.getString("memo"),
                        createdAt = transactionJson.getLong("createdAt")
                    )
                )
            }
        }

        // 背景色設定（バージョン3以降のみ）
        val backgroundColorPresets = mutableListOf<com.firedispatch.log.data.entity.BackgroundColorPreset>()
        if (json.has("backgroundColorPresets")) {
            val presetsArray = json.getJSONArray("backgroundColorPresets")
            for (i in 0 until presetsArray.length()) {
                val presetJson = presetsArray.getJSONObject(i)
                backgroundColorPresets.add(
                    com.firedispatch.log.data.entity.BackgroundColorPreset(
                        id = presetJson.getLong("id"),
                        name = presetJson.getString("name"),
                        color1 = presetJson.getString("color1"),
                        color2 = presetJson.getString("color2"),
                        color3 = presetJson.getString("color3")
                    )
                )
            }
        }

        val screenBackgroundMappings = mutableListOf<com.firedispatch.log.data.entity.ScreenBackgroundMapping>()
        if (json.has("screenBackgroundMappings")) {
            val mappingsArray = json.getJSONArray("screenBackgroundMappings")
            for (i in 0 until mappingsArray.length()) {
                val mappingJson = mappingsArray.getJSONObject(i)
                screenBackgroundMappings.add(
                    com.firedispatch.log.data.entity.ScreenBackgroundMapping(
                        screenName = mappingJson.getString("screenName"),
                        presetId = mappingJson.getLong("presetId")
                    )
                )
            }
        }

        return BackupData(
            version = json.getInt("version"),
            timestamp = json.getLong("timestamp"),
            members = members,
            roleAssignments = roleAssignments,
            roleMemberCounts = roleMemberCounts,
            events = events,
            attendances = attendances,
            settings = settings,
            fiscalYears = fiscalYears,
            accountCategories = accountCategories,
            accountSubCategories = accountSubCategories,
            transactions = transactions,
            backgroundColorPresets = backgroundColorPresets,
            screenBackgroundMappings = screenBackgroundMappings
        )
    }
}
