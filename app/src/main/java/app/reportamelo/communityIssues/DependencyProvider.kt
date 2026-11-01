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

val LocalAuthViewModel = compositionLocalOf<AuthViewModel> { error("No AuthViewModel provided") }
val LocalSettingsStore = compositionLocalOf<SettingsStore> { error("No SettingsStore provided") }
val LocalNetworkMonitor = compositionLocalOf<NetworkMonitor> { error("No NetworkMonitor provided") }
val LocalSubscriptionManager = compositionLocalOf<SubscriptionManager> { error("No SubscriptionManager provided") }
val LocalNotificationManager = compositionLocalOf<AppNotificationManager> { error("No NotificationManager provided") }
val LocalDeepLinkRouter = compositionLocalOf<DeepLinkRouter> { error("No DeepLinkRouter provided") }

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
