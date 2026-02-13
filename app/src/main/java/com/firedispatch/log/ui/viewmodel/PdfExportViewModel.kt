package com.firedispatch.log.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.repository.AccountingRepository
import com.firedispatch.log.data.repository.EventRepository
import com.firedispatch.log.data.repository.MemberRepository
import com.firedispatch.log.data.repository.RoleRepository
import com.firedispatch.log.data.repository.SettingsRepository
import com.firedispatch.log.util.PdfExportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PdfExportViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val memberRepository = MemberRepository(database.memberDao())
    private val roleRepository = RoleRepository(database.roleAssignmentDao(), database.roleMemberCountDao())
    private val eventRepository = EventRepository(database.eventDao(), database.attendanceDao())
    private val settingsRepository = SettingsRepository(database.appSettingsDao())
    private val accountingRepository = AccountingRepository(
        database.fiscalYearDao(),
        database.accountCategoryDao(),
        database.transactionDao()
    )

    fun generateMemberListPdf(onSuccess: (Intent) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val members = memberRepository.allMembers.first()
                val roleAssignments = roleRepository.allRoleAssignments.first()
                val organizationName = settingsRepository.getSettingValue(SettingsRepository.KEY_ORGANIZATION_NAME) ?: "消防団"

                val result = PdfExportGenerator.generateMemberListPdf(
                    context = getApplication(),
                    organizationName = organizationName,
                    members = members,
                    roleAssignments = roleAssignments
                )

                if (result.isSuccess) {
                    val pdfFile = result.getOrNull()
                    if (pdfFile != null) {
                        val intent = PdfExportGenerator.openPdfWithIntent(getApplication(), pdfFile)
                        onSuccess(intent)
                    } else {
                        onError("PDFファイルの生成に失敗しました")
                    }
                } else {
                    onError(result.exceptionOrNull()?.message ?: "PDF生成に失敗しました")
                }
            } catch (e: Exception) {
                onError(e.message ?: "エラーが発生しました")
            }
        }
    }

    fun generateAttendanceTablePdf(onSuccess: (Intent) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val members = memberRepository.allMembers.first()
                val roleAssignments = roleRepository.allRoleAssignments.first()
                val events = eventRepository.allEvents.first()
                val organizationName = settingsRepository.getSettingValue(SettingsRepository.KEY_ORGANIZATION_NAME) ?: "消防団"

                // 出動情報を取得
                val eventColumnsData = mutableListOf<Triple<Long, String, Map<Long, Boolean>>>()
                events.forEach { event ->
                    val attendances = eventRepository.getAttendanceByEvent(event.id).first()
                    val attendanceMap = attendances.associate { it.memberId to it.attended }
                    eventColumnsData.add(Triple(event.id, event.eventName, attendanceMap))
                }

                val result = PdfExportGenerator.generateAttendanceTablePdf(
                    context = getApplication(),
                    organizationName = organizationName,
                    members = members,
                    roleAssignments = roleAssignments,
                    events = events,
                    eventColumnsData = eventColumnsData
                )

                if (result.isSuccess) {
                    val pdfFile = result.getOrNull()
                    if (pdfFile != null) {
                        val intent = PdfExportGenerator.openPdfWithIntent(getApplication(), pdfFile)
                        onSuccess(intent)
                    } else {
                        onError("PDFファイルの生成に失敗しました")
                    }
                } else {
                    onError(result.exceptionOrNull()?.message ?: "PDF生成に失敗しました")
                }
            } catch (e: Exception) {
                onError(e.message ?: "エラーが発生しました")
            }
        }
    }

    fun generateAllowancePdf(
        allowancePerAttendance: Int,
        onSuccess: (Intent) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val members = memberRepository.allMembers.first()
                val roleAssignments = roleRepository.allRoleAssignments.first()
                val events = eventRepository.allEvents.first()
                val organizationName = settingsRepository.getSettingValue(SettingsRepository.KEY_ORGANIZATION_NAME) ?: "消防団"

                // 各団員の手当指数を計算
                val allowanceIndexMap = mutableMapOf<Long, Int>()
                members.forEach { member ->
                    var totalIndex = 0
                    events.forEach { event ->
                        val attendances = eventRepository.getAttendanceByEvent(event.id).first()
                        if (attendances.any { it.memberId == member.id && it.attended }) {
                            totalIndex += event.allowanceIndex
                        }
                    }
                    allowanceIndexMap[member.id] = totalIndex
                }

                val result = PdfExportGenerator.generateAllowancePdf(
                    context = getApplication(),
                    organizationName = organizationName,
                    members = members,
                    roleAssignments = roleAssignments,
                    allowanceIndexMap = allowanceIndexMap,
                    allowancePerAttendance = allowancePerAttendance
                )

                if (result.isSuccess) {
                    val pdfFile = result.getOrNull()
                    if (pdfFile != null) {
                        val intent = PdfExportGenerator.openPdfWithIntent(getApplication(), pdfFile)
                        onSuccess(intent)
                    } else {
                        onError("PDFファイルの生成に失敗しました")
                    }
                } else {
                    onError(result.exceptionOrNull()?.message ?: "PDF生成に失敗しました")
                }
            } catch (e: Exception) {
                onError(e.message ?: "エラーが発生しました")
            }
        }
    }

    fun generateAccountingSummaryPdf(onSuccess: (Intent) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // アクティブな年度を取得
                val activeFiscalYear = accountingRepository.activeFiscalYear.first()
                if (activeFiscalYear == null) {
                    onError("アクティブな年度が設定されていません")
                    return@launch
                }

                // 取引データを取得
                val transactions = accountingRepository.getTransactionsByFiscalYear(activeFiscalYear.id).first()

                // 科目データを取得
                val incomeCategories = accountingRepository.getCategoriesByType(1).first()
                val expenseCategories = accountingRepository.getCategoriesByType(0).first()
                val allCategories = incomeCategories + expenseCategories

                // 組織名を取得
                val organizationName = settingsRepository.getSettingValue(SettingsRepository.KEY_ORGANIZATION_NAME) ?: "消防団"

                val result = PdfExportGenerator.generateAccountingSummaryPdf(
                    context = getApplication(),
                    organizationName = organizationName,
                    fiscalYear = activeFiscalYear,
                    transactions = transactions,
                    categories = allCategories,
                    accountingRepository = accountingRepository
                )

                if (result.isSuccess) {
                    val pdfFile = result.getOrNull()
                    if (pdfFile != null) {
                        val intent = PdfExportGenerator.openPdfWithIntent(getApplication(), pdfFile)
                        onSuccess(intent)
                    } else {
                        onError("PDFファイルの生成に失敗しました")
                    }
                } else {
                    onError(result.exceptionOrNull()?.message ?: "PDF生成に失敗しました")
                }
            } catch (e: Exception) {
                onError(e.message ?: "エラーが発生しました")
            }
        }
    }

    fun generateTransactionListPdf(onSuccess: (Intent) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // アクティブな年度を取得
                val activeFiscalYear = accountingRepository.activeFiscalYear.first()
                if (activeFiscalYear == null) {
                    onError("アクティブな年度が設定されていません")
                    return@launch
                }

                // 取引データを取得（日付順にソート）
                val transactions = accountingRepository.getTransactionsByFiscalYear(activeFiscalYear.id).first()
                    .sortedBy { it.date }

                // 科目データを取得
                val incomeCategories = accountingRepository.getCategoriesByType(1).first()
                val expenseCategories = accountingRepository.getCategoriesByType(0).first()
                val allCategories = incomeCategories + expenseCategories

                // 補助科目データを取得（マップ化）
                val subCategoriesMap = mutableMapOf<Long, String>()
                allCategories.forEach { category ->
                    val subCategories = accountingRepository.getSubCategoriesByParent(category.id).first()
                    subCategories.forEach { subCategory ->
                        subCategoriesMap[subCategory.id] = subCategory.name
                    }
                }

                // 組織名を取得
                val organizationName = settingsRepository.getSettingValue(SettingsRepository.KEY_ORGANIZATION_NAME) ?: "消防団"

                val result = PdfExportGenerator.generateTransactionListPdf(
                    context = getApplication(),
                    organizationName = organizationName,
                    fiscalYear = activeFiscalYear,
                    transactions = transactions,
                    categories = allCategories,
                    subCategoriesMap = subCategoriesMap
                )

                if (result.isSuccess) {
                    val pdfFile = result.getOrNull()
                    if (pdfFile != null) {
                        val intent = PdfExportGenerator.openPdfWithIntent(getApplication(), pdfFile)
                        onSuccess(intent)
                    } else {
                        onError("PDFファイルの生成に失敗しました")
                    }
                } else {
                    onError(result.exceptionOrNull()?.message ?: "PDF生成に失敗しました")
                }
            } catch (e: Exception) {
                onError(e.message ?: "エラーが発生しました")
            }
        }
    }
}
