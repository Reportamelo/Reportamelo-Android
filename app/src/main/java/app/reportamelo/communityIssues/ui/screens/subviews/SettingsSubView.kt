package app.reportamelo.communityIssues.ui.screens.subviews

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.communityIssues.LocalAuthViewModel
import app.reportamelo.communityIssues.LocalNetworkMonitor
import app.reportamelo.communityIssues.LocalSubscriptionManager
import app.reportamelo.communityIssues.controllers.SettingsSubViewModel
import app.reportamelo.communityIssues.datamodel.CoordinateSpan
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.communityIssues.ui.components.LinkType
import app.reportamelo.communityIssues.ui.components.LinksView
import app.reportamelo.communityIssues.ui.components.LocalToastState
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.components.ToastType
import app.reportamelo.communityIssues.ui.screens.tabs.CitySelectionView
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.models.Coordinate
import app.reportamelo.models.FriendlyCityDistribution
import app.reportamelo.repository.CitiesRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.launch

private val SecondaryHeaderAndFooterColor = Color(0xFF8E8E93)

@Composable
fun FooterText(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 16.sp
        ),
        color = SecondaryHeaderAndFooterColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 6.dp)
    )
}

@Composable
fun SettingsHeaderView(
    modifier: Modifier = Modifier,
    title: String,
    isLoading: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 18.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            color = SecondaryHeaderAndFooterColor
        )
        if (isLoading) {
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = SecondaryHeaderAndFooterColor
            )
        }
    }
}

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val uncheckedTrackColor = if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC)
    val disabledUncheckedTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

    Switch(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = {
            // Keeps the thumb at the full 24.dp diameter when unchecked so it looks like an active iOS switch
            Spacer(modifier = Modifier.size(SwitchDefaults.IconSize))
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.8f),
            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.8f),
            disabledUncheckedTrackColor = disabledUncheckedTrackColor,
            disabledUncheckedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String,
    footerText: String? = null,
    footerContent: (@Composable () -> Unit)? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        SettingsHeaderView(
            title = title,
            isLoading = isLoading
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBackground,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }

        if (footerText != null) {
            FooterText(text = footerText)
        }

        if (footerContent != null) {
            footerContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubView(
    modifier: Modifier = Modifier,
    subViewName: String = "Settings",
    settingsStore: SettingsStore = SettingsStore.shared,
    notificationManager: AppNotificationManager = AppNotificationManager,
    viewModel: SettingsSubViewModel = remember { SettingsSubViewModel() },
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authViewModel = LocalAuthViewModel.current
    val subscriptionManager = LocalSubscriptionManager.current
    val networkMonitor = LocalNetworkMonitor.current
    val toastState = LocalToastState.current

    val isGuest = remember { UserRepository.shared.isGuestUser() }
    val isPro by subscriptionManager.isPro.collectAsState()
    val isConnected by networkMonitor.isConnected.collectAsState()

    var showMyProfile by remember { mutableStateOf(settingsStore.showMyProfile) }
    var showMyUserNameWhenShare by remember { mutableStateOf(settingsStore.showMyUseNameWhenShare) }
    var enablePushNotifications by remember { mutableStateOf(settingsStore.enablePushNotifications) }
    var enableEmailNotifications by remember { mutableStateOf(settingsStore.enableEmailNotifications) }
    var saveLastLocation by remember { mutableStateOf(settingsStore.saveLastLocation) }
    var useMyCurrentLocation by remember { mutableStateOf(settingsStore.useMyCurrentLocation) }
    var enableAnonymousTelemetry by remember { mutableStateOf(settingsStore.enableAnonymousTelemetry) }

    var selectedCity by remember {
        mutableStateOf(
            CitiesRepository.getSavedSelectedCity()
                ?: authViewModel.selectedCity.value
                ?: FriendlyCityDistribution(
                    cityId = "a67b90f9-1d76-4835-a994-03cd04f1d619",
                    firstLevel = "El Salvador",
                    secondLevel = "San Salvador",
                    thirdLevel = "San Salvador",
                    ZipCode = "1101",
                    legalGroupName = "Distrito de San Salvador",
                    coordinates = Coordinate(lat = 13.6979857, lng = -89.1918083),
                    isCapitalCity = 1,
                    isDepartmentalCapital = 1
                )
        )
    }
    var showCitySelectorSheet by remember { mutableStateOf(false) }

    val updatingPrivacyLoading by viewModel.updatingPrivacySettingsLoading.collectAsState()
    val updatingNotificationLoading by viewModel.updatingNotificationSettingsLoading.collectAsState()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsState()
    val showDeleteAlert by viewModel.showDeleteAlert.collectAsState()
    val showActiveSubscriptionAlert by viewModel.showActiveSubscriptionAlert.collectAsState()

    val isPermissionGranted by notificationManager.isPermissionGrantedFlow.collectAsState()
    val deviceToken by notificationManager.deviceTokenFlow.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationManager.onPermissionResult(granted)
        if (granted && enablePushNotifications) {
            viewModel.updateDeviceToken()
        }
    }

    LaunchedEffect(settingsStore, notificationManager) {
        viewModel.inject(settingsStore, notificationManager)
        notificationManager.checkAuthorizationStatus(context)
    }

    // Mirrors iOS .onChange(of: notificationManager.isPermissionGranted)
    LaunchedEffect(isPermissionGranted) {
        if (enablePushNotifications && isPermissionGranted && deviceToken.isNotEmpty()) {
            viewModel.updateDeviceToken()
        }
    }

    // Mirrors iOS .onChange(of: notificationManager.deviceToken)
    LaunchedEffect(deviceToken) {
        if (enablePushNotifications && isPermissionGranted && deviceToken.isNotEmpty()) {
            viewModel.updateDeviceToken()
        }
    }

    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.67f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Top Bar matching iOS Navigation Bar & MyReportsSubView
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
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
            }

            Text(
                text = subViewName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Location Section
            SettingsGroup(title = "Location") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCitySelectorSheet = true }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "City",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedCity.thirdLevel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Subscription Section
            SettingsGroup(
                title = "Subscription",
                footerText = "You can help us improve the app by supporting us. Thank you!",
                enabled = !isGuest
            ) {
                if (isPro) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reportamelo Pro",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF34C759)
                        )
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SecondaryHeaderAndFooterColor
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = dividerColor
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isGuest) {
                                runCatching {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/account/subscriptions")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Manage Subscription",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SecondaryHeaderAndFooterColor
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isGuest) {
                                // Upgrade action
                            }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reportamelo Pro",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Upgrade",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = dividerColor
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isGuest) {
                                coroutineScope.launch {
                                    subscriptionManager.restorePurchases()
                                    if (subscriptionManager.isPro.value) {
                                        toastState.show(
                                            message = "Your subscription has been restored.",
                                            type = ToastType.Success
                                        )
                                    } else {
                                        toastState.show(
                                            message = "No active subscription was found.",
                                            type = ToastType.Info
                                        )
                                    }
                                }
                            }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Do you have a subscription?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Restore",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Privacy Section
            SettingsGroup(
                title = "Privacy",
                footerText = "You can show or hide your profile and your username when you share your reports and petitions with others.",
                isLoading = updatingPrivacyLoading,
                enabled = !isGuest
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show my profile",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitch(
                        checked = showMyProfile,
                        enabled = !isGuest,
                        onCheckedChange = {
                            showMyProfile = it
                            settingsStore.showMyProfile = it
                            viewModel.updatePrivacySettings()
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
                        text = "Show my user name when I share",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitch(
                        checked = showMyUserNameWhenShare,
                        enabled = !isGuest,
                        onCheckedChange = {
                            showMyUserNameWhenShare = it
                            settingsStore.showMyUseNameWhenShare = it
                            viewModel.updatePrivacySettings()
                        }
                    )
                }
            }

            // Notifications Section
            SettingsGroup(
                title = "Notifications",
                footerText = "You can enable or disable push notifications in order to receive updates when authorities are resolving your report or petition.",
                isLoading = updatingNotificationLoading,
                enabled = !isGuest
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Push notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitch(
                        checked = enablePushNotifications,
                        enabled = !isGuest,
                        onCheckedChange = { newValue ->
                            enablePushNotifications = newValue
                            settingsStore.enablePushNotifications = newValue
                            viewModel.updateNotificationSettings()

                            if (!notificationManager.isPermissionGranted) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationManager.onPermissionResult(true)
                                    viewModel.updateDeviceToken()
                                }
                            } else {
                                viewModel.updateDeviceToken()
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
                        text = "Email notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitch(
                        checked = enableEmailNotifications,
                        enabled = !isGuest,
                        onCheckedChange = {
                            enableEmailNotifications = it
                            settingsStore.enableEmailNotifications = it
                            viewModel.updateNotificationSettings()
                        }
                    )
                }
            }

            // App settings Section
            SettingsGroup(
                title = "App settings",
                enabled = !isGuest,
                footerContent = {
                    LinksView(
                        type = LinkType.Privacy,
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                    )
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Save last location",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitch(
                        checked = saveLastLocation,
                        enabled = !isGuest,
                        onCheckedChange = {
                            saveLastLocation = it
                            settingsStore.saveLastLocation = it
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
                        text = "Use my current location",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitch(
                        checked = useMyCurrentLocation,
                        enabled = !isGuest,
                        onCheckedChange = {
                            useMyCurrentLocation = it
                            settingsStore.useMyCurrentLocation = it
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
                        text = "Anonymous telemetry",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsSwitch(
                        checked = enableAnonymousTelemetry,
                        enabled = !isGuest,
                        onCheckedChange = {
                            enableAnonymousTelemetry = it
                            settingsStore.enableAnonymousTelemetry = it
                        }
                    )
                }
            }

            // Dangerous zone Section
            SettingsGroup(
                title = "Dangerous zone",
                footerText = "Once your account is deleted, there is no going back.",
                enabled = !isGuest
            ) {
                Text(
                    text = "You have control over your data and you can opt to delete your account at any time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "If your are using Login with Apple you have to revoke the access to your account from Apple to delete your access to this app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ThemedButton(
                    message = "Delete my account",
                    onClick = { viewModel.handleDeleteRequest(isPro = isPro) },
                    type = ThemedButtonType.DANGER,
                    isLoading = isDeletingAccount,
                    isEnabled = !isGuest && isConnected
                )
            }

            // About Section
            SettingsGroup(title = "About") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SelectionContainer {
                        Text(
                            text = "v1.0.0",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                letterSpacing = 0.69.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = dividerColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Commit",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SelectionContainer {
                        Text(
                            text = "8466b51",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                letterSpacing = 0.69.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showActiveSubscriptionAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissActiveSubscriptionAlert() },
            title = { Text("Active Subscription Detected") },
            text = {
                Text(
                    "You currently have an active Reportamelo Pro subscription. Deleting your account will NOT automatically cancel your billing with Google Play. Would you like to manage your subscription first, or proceed with deleting your account?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissActiveSubscriptionAlert()
                        runCatching {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/account/subscriptions")
                            )
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Manage Subscription")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.dismissActiveSubscriptionAlert() }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = { viewModel.proceedToDeleteAlert() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Proceed to Delete")
                    }
                }
            }
        )
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAlert() },
            title = { Text("Delete Account?") },
            text = {
                Text("This action is permanent and cannot be undone. All your data will be permanently removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissDeleteAlert()
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAlert() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCitySelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCitySelectorSheet = false }
        ) {
            CitySelectionView(
                selectedCity = selectedCity,
                onCitySelected = { newCity ->
                    selectedCity = newCity
                    CitiesRepository.saveSelectedCity(newCity)
                    settingsStore.cityId = newCity.cityId
                    authViewModel.setSelectedCity(newCity)
                    authViewModel.setCameraPosition(
                        to = newCity.coordinates,
                        latitudeDelta = CoordinateSpan.CITY_SELECTION_LATITUDE_DELTA,
                        longitudeDelta = CoordinateSpan.CITY_SELECTION_LONGITUDE_DELTA
                    )
                    showCitySelectorSheet = false
                }
            )
        }
    }
}
