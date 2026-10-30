package app.reportamelo.communityIssues.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.reportamelo.communityIssues.ui.screens.tabs.*
import app.reportamelo.communityIssues.ui.screens.subviews.*
import app.reportamelo.communityIssues.ui.screens.attachments.*

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Landing : Screen("landing")
    object Acceptance : Screen("acceptance")
    object CitySelection : Screen("city_selection")
    object UserPersonalization : Screen("user_personalization")
    object EssentialInformation : Screen("essential_information")
    object TabBar : Screen("tab_bar")
    
    // Subviews
    object Settings : Screen("settings")
    object Licenses : Screen("licenses")
    object Comments : Screen("comments")
    object MyReports : Screen("my_reports")
    object ReportsAndViolations : Screen("reports_and_violations")
    object MyPetitions : Screen("my_petitions")
    object CreateReport : Screen("create_report")
    object IssueTimeline : Screen("issue_timeline")
    object AttachmentDetail : Screen("attachment_detail/{url}/{isPdf}") {
        fun createRoute(url: String, isPdf: Boolean) = "attachment_detail/$url/$isPdf"
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(Screen.Welcome.route) {
            // WelcomeView(navController)
        }
        composable(Screen.Landing.route) {
            // LandingView(navController)
        }
        composable(Screen.Acceptance.route) {
            // AcceptanceView(navController)
        }
        composable(Screen.CitySelection.route) {
            // CitySelectionView(navController)
        }
        composable(Screen.UserPersonalization.route) {
            // UserPersonalizationView(navController)
        }
        composable(Screen.EssentialInformation.route) {
            // EssentialInformationView(navController)
        }
        composable(Screen.TabBar.route) {
            // TabBarView(navController)
        }
        composable(Screen.Settings.route) {
            // SettingsSubView(navController)
        }
        composable(Screen.Licenses.route) {
            // LicensesSubView(navController)
        }
        composable(Screen.Comments.route) {
            // CommentsSubView(navController)
        }
        composable(Screen.MyReports.route) {
            // MyReportsSubView(navController)
        }
        composable(Screen.ReportsAndViolations.route) {
            // ReportsAndViolationsCenterView(navController)
        }
        composable(Screen.MyPetitions.route) {
            // MyPetitionsSubView(navController)
        }
        composable(Screen.CreateReport.route) {
            // CreateReportView(navController)
        }
        composable(Screen.IssueTimeline.route) {
            // IssueTimelineView(navController)
        }
        composable(Screen.AttachmentDetail.route) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val isPdf = backStackEntry.arguments?.getString("isPdf")?.toBoolean() ?: false
            AttachmentDetailView(url = url, isPdf = isPdf)
        }
    }
}
