package app.reportamelo.communityIssues.ui.screens.tabs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.communityIssues.LocalNotificationManager
import app.reportamelo.communityIssues.LocalSettingsStore
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.EssentialInformationViewModel
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.communityIssues.helpers.DeepLinkRouter
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonStyle
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.screens.subviews.SettingsGroup
import app.reportamelo.communityIssues.ui.screens.subviews.SettingsSwitch
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground

typealias Notifications = app.reportamelo.models.Notifications

@Composable
fun EssentialInformationView(
    modifier: Modifier = Modifier,
    notifications: Notifications = Notifications(app = false, email = false, web = false),
    onNotificationsChanged: (Notifications) -> Unit = {},
    onBack: (() -> Unit)? = null,
    finalStep: () -> Unit = {},
    viewModel: EssentialInformationViewModel = viewModel()
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val settings = runCatching { LocalSettingsStore.current }.getOrDefault(SettingsStore.shared)
    val notificationManager = runCatching { LocalNotificationManager.current }.getOrDefault(AppNotificationManager)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.67f)

    val vmNotifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSavingNotifications by viewModel.isSavingNotifications.collectAsState()
    val buttonMessageRes by viewModel.buttonMessageRes.collectAsState()
    val showErrorAlert by viewModel.showErrorAlert.collectAsState()
    val messageError by viewModel.messageError.collectAsState()
    val messageErrorRes by viewModel.messageErrorRes.collectAsState()
    val deviceToken by notificationManager.deviceTokenFlow.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPushPermissionResult(
            granted = granted,
            onNotificationsChanged = onNotificationsChanged
        )
    }

    LaunchedEffect(Unit) {
        viewModel.inject(
            settings = settings,
            notificationManager = notificationManager,
            initialNotifications = notifications
        )
    }

    LaunchedEffect(deviceToken) {
        if (deviceToken.isNotEmpty() && vmNotifications.app) {
            viewModel.updateDeviceToken()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .radialGradientBackground(topLeading = false, radiusFraction = 0.54f)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = {
                            if (onBack != null) {
                                onBack()
                            } else {
                                LandingViewModel.shared.navigateBack()
                                backDispatcher?.onBackPressed()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(42.dp),
                        shape = CircleShape,
                        color = cardBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.notes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    ThemedButton(
                        message = stringResource(buttonMessageRes),
                        type = ThemedButtonType.SECONDARY,
                        style = ThemedButtonStyle.PROMINENT,
                        isLoading = isLoading,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.configureAccount(
                                finalStep = {
                                    finalStep()
                                    DeepLinkRouter.shared.activeTab.value = 1
                                }
                            )
                        },
                        modifier = Modifier.padding(horizontal = 36.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SettingsGroup(
                    title = stringResource(R.string.notifications),
                    footerText = stringResource(R.string.please_give_me_permission_to_send_you_notifications_about_status_of_your_reports_and_follow_up_you_can_change_these_settings_in_the_app_settings),
                    isLoading = isSavingNotifications
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.push_notifications),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        SettingsSwitch(
                            checked = vmNotifications.app,
                            onCheckedChange = { newValue ->
                                if (newValue) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val alreadyGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (alreadyGranted) {
                                            viewModel.onPushPermissionResult(
                                                granted = true,
                                                onNotificationsChanged = onNotificationsChanged
                                            )
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                                        viewModel.onPushPermissionResult(
                                            granted = enabled,
                                            onNotificationsChanged = onNotificationsChanged
                                        )
                                    }
                                } else {
                                    viewModel.onPushNotificationDisabled(
                                        onNotificationsChanged = onNotificationsChanged
                                    )
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = dividerColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.email_notifications),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        SettingsSwitch(
                            checked = vmNotifications.email,
                            onCheckedChange = { newValue ->
                                viewModel.onEmailNotificationChanged(
                                    enabled = newValue,
                                    onNotificationsChanged = onNotificationsChanged
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showErrorAlert) {
        val resolvedMessage = messageErrorRes?.let { stringResource(it) } ?: messageError
        AlertDialog(
            onDismissRequest = { viewModel.dismissErrorAlert() },
            title = { Text("Error") },
            text = { Text(resolvedMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissErrorAlert() }) {
                    Text("OK")
                }
            }
        )
    }
}
