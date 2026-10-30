package app.reportamelo.communityIssues.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.reportamelo.communityIssues.LocalAuthViewModel
import app.reportamelo.communityIssues.LocalSettingsStore
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.ui.screens.tabs.AcceptanceView
import app.reportamelo.communityIssues.ui.screens.tabs.CitySelectionView

@Composable
fun WelcomeView(
    modifier: Modifier = Modifier,
    landingViewModel: LandingViewModel = LandingViewModel.shared
) {
    val authViewModel = LocalAuthViewModel.current
    val settingsStore = LocalSettingsStore.current

    val isLoggedIn by landingViewModel.isLoggedIn.collectAsState()
    val isGuest by landingViewModel.isGuest.collectAsState()
    val selectedCity by authViewModel.selectedCity.collectAsState()
    val landingSelectedCity by landingViewModel.selectedCity.collectAsState()
    val presentAlert by landingViewModel.presentAlert.collectAsState()
    val alertTitle by landingViewModel.alertTitle.collectAsState()
    val alertMessage by landingViewModel.message.collectAsState()

    var showGuestCitySelection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        landingViewModel.inject(settingsStore)
        landingViewModel.checkStatus(authViewModel)
    }

    if (presentAlert) {
        AlertDialog(
            onDismissRequest = { landingViewModel.dismissAlert() },
            title = { Text(alertTitle) },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { landingViewModel.dismissAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoggedIn) {
            TabBarView()
        } else if (isGuest) {
            if (selectedCity != null) {
                TabBarView()
            } else {
                if (showGuestCitySelection) {
                    CitySelectionView(
                        selectedCity = landingSelectedCity,
                        onCitySelected = { city ->
                            landingViewModel.setSelectedCity(city)
                        },
                        nextStep = {
                            landingViewModel.completeGuestOnboarding(authViewModel)
                        }
                    )
                } else {
                    AcceptanceView(
                        nextStep = {
                            showGuestCitySelection = true
                        }
                    )
                }
            }
        } else {
            LandingView(landingViewModel = landingViewModel)
        }
    }
}
