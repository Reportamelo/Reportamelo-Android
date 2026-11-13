package app.reportamelo.communityIssues

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

import app.reportamelo.commons.NetworkMonitor
import app.reportamelo.services.SubscriptionManager
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.communityIssues.helpers.DeepLinkRouter

import app.reportamelo.communityIssues.datamodel.AuthViewModel
import app.reportamelo.communityIssues.datamodel.SettingsStore

val LocalAuthViewModel = compositionLocalOf<AuthViewModel> { AuthViewModel.shared }
val LocalSettingsStore = compositionLocalOf<SettingsStore> { SettingsStore.shared }
val LocalNetworkMonitor = compositionLocalOf<NetworkMonitor> { error("No NetworkMonitor provided") }
val LocalSubscriptionManager = compositionLocalOf<SubscriptionManager> { SubscriptionManager }
val LocalNotificationManager = compositionLocalOf<AppNotificationManager> { AppNotificationManager }
val LocalDeepLinkRouter = compositionLocalOf<DeepLinkRouter> { DeepLinkRouter.shared }

@Composable
fun DependencyProvider(
    authViewModel: AuthViewModel = AuthViewModel.shared,
    settingsStore: SettingsStore = SettingsStore.shared,
    networkMonitor: NetworkMonitor,
    subscriptionManager: SubscriptionManager = SubscriptionManager,
    notificationManager: AppNotificationManager = AppNotificationManager,
    deepLinkRouter: DeepLinkRouter = DeepLinkRouter.shared,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAuthViewModel provides authViewModel,
        LocalSettingsStore provides settingsStore,
        LocalNetworkMonitor provides networkMonitor,
        LocalSubscriptionManager provides subscriptionManager,
        LocalNotificationManager provides notificationManager,
        LocalDeepLinkRouter provides deepLinkRouter
    ) {
        content()
    }
}
