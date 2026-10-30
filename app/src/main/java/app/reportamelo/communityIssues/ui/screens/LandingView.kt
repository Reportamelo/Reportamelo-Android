package app.reportamelo.communityIssues.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.reportamelo.communityIssues.LocalAuthViewModel
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.ui.screens.tabs.CitySelectionView
import app.reportamelo.communityIssues.ui.screens.tabs.EssentialInformationView
import app.reportamelo.communityIssues.ui.screens.tabs.Notifications
import app.reportamelo.communityIssues.ui.screens.tabs.UserPersonalizationView

enum class LandingNavigation {
    Login,
    SelectCity,
    PersonalizeProfile,
    EssentialInfo
}

@Composable
fun LandingView(
    modifier: Modifier = Modifier,
    landingViewModel: LandingViewModel = LandingViewModel.shared
) {
    val navController = rememberNavController()
    val authViewModel = LocalAuthViewModel.current

    val currentPath by landingViewModel.path.collectAsState()
    val selectedCity by landingViewModel.selectedCity.collectAsState()

    LaunchedEffect(currentPath) {
        if (currentPath.isNotEmpty()) {
            navController.navigate(currentPath.last().name)
        }
    }

    NavHost(
        navController = navController,
        startDestination = LandingNavigation.Login.name,
        modifier = modifier
    ) {
        composable(LandingNavigation.Login.name) {
            LoginView(
                onTokenReceived = { payload, type ->
                    landingViewModel.handleLogin(payload, type, authViewModel)
                }
            )
        }
        composable(LandingNavigation.SelectCity.name) {
            CitySelectionView(
                selectedCity = selectedCity,
                onCitySelected = { city ->
                    landingViewModel.setSelectedCity(city)
                },
                nextStep = {
                    navController.navigate(LandingNavigation.PersonalizeProfile.name)
                }
            )
        }
        composable(LandingNavigation.PersonalizeProfile.name) {
            UserPersonalizationView(
                onNextStep = {
                    navController.navigate(LandingNavigation.EssentialInfo.name)
                }
            )
        }
        composable(LandingNavigation.EssentialInfo.name) {
            var notifications by remember { mutableStateOf(Notifications(app = false, email = false, web = false)) }
            EssentialInformationView(
                notifications = notifications,
                onNotificationsChanged = { notifications = it },
                finalStep = {
                    landingViewModel.completeUserOnboarding(
                        notifications = app.reportamelo.models.Notifications(
                            app = notifications.app,
                            email = notifications.email,
                            web = notifications.web
                        ),
                        appState = authViewModel
                    )
                }
            )
        }
    }
}
