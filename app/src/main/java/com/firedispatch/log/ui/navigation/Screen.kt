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
}
