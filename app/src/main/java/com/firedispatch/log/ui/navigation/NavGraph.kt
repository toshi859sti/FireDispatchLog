package com.firedispatch.log.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.firedispatch.log.ui.screen.DispatchTableScreen
import com.firedispatch.log.ui.screen.EventEditScreen
import com.firedispatch.log.ui.screen.MemberEditScreen
import com.firedispatch.log.ui.screen.MemberListScreen
import com.firedispatch.log.ui.screen.MenuScreen
import com.firedispatch.log.ui.screen.PdfExportScreen
import com.firedispatch.log.ui.screen.RoleAssignmentScreen
import com.firedispatch.log.ui.screen.RoleMemberCountSettingScreen
import com.firedispatch.log.ui.screen.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Menu.route
    ) {
        composable(Screen.Menu.route) {
            MenuScreen(navController = navController)
        }

        composable(Screen.MemberList.route) {
            MemberListScreen(navController = navController)
        }

        composable(Screen.MemberEdit.route) {
            MemberEditScreen(navController = navController)
        }

        composable(Screen.RoleAssignment.route) {
            RoleAssignmentScreen(navController = navController)
        }

        composable(Screen.RoleMemberCountSetting.route) {
            RoleMemberCountSettingScreen(navController = navController)
        }

        composable(Screen.DispatchTable.route) {
            DispatchTableScreen(navController = navController)
        }

        composable(
            route = Screen.EventEdit.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            EventEditScreen(navController = navController, eventId = eventId)
        }

        composable(Screen.PdfExport.route) {
            PdfExportScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
