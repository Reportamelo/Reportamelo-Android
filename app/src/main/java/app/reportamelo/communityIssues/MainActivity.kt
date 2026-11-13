package app.reportamelo.communityIssues

import android.os.Bundle
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.util.Consumer
import app.reportamelo.communityIssues.ui.theme.CommunityIssueReporterTheme
import app.reportamelo.communityIssues.ui.screens.WelcomeView
import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.NetworkMonitor
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.AppLanguageManager
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.communityIssues.helpers.DeepLinkRouter
import app.reportamelo.network.KeychainService
import app.reportamelo.services.SubscriptionManager

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(applicationContext)
        KeychainService.init(applicationContext)
        SubscriptionManager.init(applicationContext)
        SettingsStore.shared.applyTelemetrySetting()
        AppNotificationManager.checkAuthorizationStatus(applicationContext)
        enableEdgeToEdge()
        setContent {
            val networkMonitor = remember { NetworkMonitor(applicationContext) }
            
            CommunityIssueReporterTheme {
                DependencyProvider(
                    networkMonitor = networkMonitor
                ) {
                    // Start navigation logic (WelcomeView hosts NavController)
                    WelcomeView()
                    
                    // Handle deep links similarly to iOS onOpenURL
                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { intent ->
                            intent.data?.let { uri ->
                                // GIDSignIn equivalent or DeepLinkRouter
                                DeepLinkRouter.shared.handleIncomingURL(uri)
                            }
                        }
                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }
                }
            }
        }
    }
}
