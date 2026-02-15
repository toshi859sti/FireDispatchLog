package com.firedispatch.log.ui.navigation

sealed class Screen(val route: String) {
    object Menu : Screen("menu")
    object MemberList : Screen("member_list")
    object MemberEdit : Screen("member_edit")
    object RoleAssignment : Screen("role_assignment")
    object RoleMemberCountSetting : Screen("role_member_count_setting")
    object DispatchTable : Screen("dispatch_table")
    object EventEdit : Screen("event_edit/{eventId}") {
        fun createRoute(eventId: Long = -1) = "event_edit/$eventId"
    }
    object PdfExport : Screen("pdf_export")
    object Settings : Screen("settings")
    object AccountingMenu : Screen("accounting_menu")
    object FiscalYear : Screen("fiscal_year")
    object AccountCategory : Screen("account_category")
    object TransactionEntry : Screen("transaction_entry")
    object LedgerView : Screen("ledger_view")
    object OpeningBalance : Screen("opening_balance")
    object BackgroundColorSetting : Screen("background_color_setting")
    object PresetEditor : Screen("preset_editor/{presetId}") {
        fun createRoute(presetId: Long? = null) = "preset_editor/${presetId ?: "new"}"
    }
}
