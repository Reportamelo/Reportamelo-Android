package app.reportamelo.communityIssues.ui.screens.subviews

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
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
import app.reportamelo.communityIssues.BuildConfig
import app.reportamelo.communityIssues.LocalAuthViewModel
import app.reportamelo.communityIssues.LocalDeepLinkRouter
import app.reportamelo.communityIssues.LocalNetworkMonitor
import app.reportamelo.communityIssues.LocalSubscriptionManager
import app.reportamelo.communityIssues.controllers.SettingsSubViewModel
import app.reportamelo.communityIssues.datamodel.CoordinateSpan
import app.reportamelo.communityIssues.datamodel.SettingsStore
import app.reportamelo.communityIssues.helpers.AppLanguageManager
import app.reportamelo.communityIssues.helpers.AppNotificationManager
import app.reportamelo.communityIssues.ui.components.LinkType
import app.reportamelo.communityIssues.ui.components.LinksView
import app.reportamelo.communityIssues.ui.components.LocalToastState
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.components.ToastType
import app.reportamelo.communityIssues.ui.screens.tabs.CitySelectionMode
import app.reportamelo.communityIssues.ui.screens.tabs.CitySelectionView
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.models.Coordinate
import app.reportamelo.models.FriendlyCityDistribution
import app.reportamelo.repository.CitiesRepository
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.helpers.localized

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
            shadowElevation = 0.5.dp
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

@Composable
private fun languageLabel(languageTag: String): String {
    return if (languageTag == AppLanguageManager.SYSTEM_DEFAULT) {
        stringResource(R.string.system_default)
    } else {
        AppLanguageManager.displayName(languageTag)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubView(
    modifier: Modifier = Modifier,
    subViewName: String = stringResource(R.string.settings),
    settingsStore: SettingsStore = SettingsStore.shared,
    notificationManager: AppNotificationManager = AppNotificationManager,
    viewModel: SettingsSubViewModel = remember { SettingsSubViewModel() },
    onBack: (() -> Unit)? = null,
    onAccountDeleted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authViewModel = LocalAuthViewModel.current
    val subscriptionManager = LocalSubscriptionManager.current
    val networkMonitor = LocalNetworkMonitor.current
    val toastState = LocalToastState.current
    val router = LocalDeepLinkRouter.current

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
    var isSelectingCity by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

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

    if (isSelectingCity) {
        BackHandler { isSelectingCity = false }
        CitySelectionView(
            modifier = modifier,
            mode = CitySelectionMode.MODIFY,
            selectedCity = selectedCity,
            onBack = { isSelectingCity = false },
            onCitySelected = { newCity ->
                selectedCity = newCity
            },
            nextStep = {
                CitiesRepository.saveSelectedCity(selectedCity)
                settingsStore.cityId = selectedCity.cityId
                authViewModel.setSelectedCity(selectedCity)
                authViewModel.setCameraPosition(
                    to = selectedCity.coordinates,
                    latitudeDelta = CoordinateSpan.CITY_SELECTION_LATITUDE_DELTA,
                    longitudeDelta = CoordinateSpan.CITY_SELECTION_LONGITUDE_DELTA
                )
                isSelectingCity = false
                onBack?.invoke()
                if (router.activeTab.value == 1) {
                    router.activeTab.value = 0
                }
                router.activeTab.value = 1
            }
        )
        return
    }

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
                            contentDescription = stringResource(R.string.back),
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
            SettingsGroup(title = stringResource(R.string.location)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSelectingCity = true }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.city),
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

            // Language Section
            SettingsGroup(title = stringResource(R.string.language)) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageMenu = true }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = languageLabel(selectedLanguage),
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
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        viewModel.supportedLanguages.forEach { languageTag ->
                            DropdownMenuItem(
                                text = { Text(languageLabel(languageTag)) },
                                onClick = {
                                    showLanguageMenu = false
                                    // Android 13+ recreates the activity by itself after the change
                                    if (viewModel.updateLanguage(languageTag) &&
                                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                                    ) {
                                        context.findActivity()?.recreate()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Subscription Section
            SettingsGroup(
                title = stringResource(R.string.subscription),
                footerText = stringResource(R.string.you_can_help_us_to_improve_the_app_by_supporting_us),
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
                            text = stringResource(R.string.reportamelo_pro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF34C759)
                        )
                        Text(
                            text = stringResource(R.string.active),
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
                            text = stringResource(R.string.manage_subscription),
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
                            text = stringResource(R.string.reportamelo_pro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.upgrade),
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
                                            message = localized(R.string.your_subscription_has_been_restored, "Your subscription has been restored."),
                                            type = ToastType.Success
                                        )
                                    } else {
                                        toastState.show(
                                            message = localized(R.string.no_active_subscription_was_found, "No active subscription was found."),
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
                            text = stringResource(R.string.do_you_have_a_subscription),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.restore),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Debug-only mock so flows that require an active subscription can be tested
                if (BuildConfig.DEBUG) {
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
                            text = "Mock Pro subscription (debug)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        SettingsSwitch(
                            checked = isPro,
                            enabled = !isGuest,
                            onCheckedChange = { subscriptionManager.setMockIsPro(it) }
                        )
                    }
                }
            }

            // Privacy Section
            SettingsGroup(
                title = stringResource(R.string.privacy),
                footerText = stringResource(R.string.you_can_show_or_hide_your_profile_and_your_username_when_you_share_your_reports_and_petitions_with_others),
                isLoading = updatingPrivacyLoading,
                enabled = !isGuest
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_my_profile),
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
                        text = stringResource(R.string.show_my_user_name_when_i_share),
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
                title = stringResource(R.string.notifications),
                footerText = stringResource(R.string.you_can_enable_or_disable_push_notifications_in_order_to_receive_updates_when_authorities_are_resolving_your_report_or_petition),
                isLoading = updatingNotificationLoading,
                enabled = !isGuest
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
                        text = stringResource(R.string.email_notifications),
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
                title = stringResource(R.string.app_settings),
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
                        text = stringResource(R.string.save_last_location),
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
                        text = stringResource(R.string.use_my_current_location),
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
                        text = stringResource(R.string.anonymous_telemetry),
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
                title = stringResource(R.string.dangerous_zone),
                footerText = stringResource(R.string.once_your_account_is_deleted_there_is_no_going_back),
                enabled = !isGuest
            ) {
                Text(
                    text = stringResource(R.string.you_have_control_over_your_data_and_you_can_opt_to_delete_your_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.if_your_are_using_login_with_apple_you_have_to_revoke_the_access_to_your_account_from_apple_to_delete_your_access_to_this_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ThemedButton(
                    message = stringResource(R.string.delete_my_account),
                    onClick = { viewModel.handleDeleteRequest(isPro = isPro) },
                    type = ThemedButtonType.DANGER,
                    isLoading = isDeletingAccount,
                    isEnabled = !isGuest && isConnected
                )
            }

            // About Section
            SettingsGroup(title = stringResource(R.string.about)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.version),
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
            title = { Text(stringResource(R.string.active_subscription_detected)) },
            text = {
                Text(
                    stringResource(R.string.you_currently_have_an_active_reportamelo_pro_subscription_deleting_you)
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
                    Text(stringResource(R.string.manage_subscription))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.dismissActiveSubscriptionAlert() }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = { viewModel.proceedToDeleteAlert() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.proceed_to_delete))
                    }
                }
            }
        )
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAlert() },
            title = { Text(stringResource(R.string.delete_account_text)) },
            text = {
                Text(stringResource(R.string.this_action_is_permanent_and_cannot_be_undone_all_your_data_will_be_pe))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissDeleteAlert()
                        viewModel.deleteAccount(
                            onDeleted = { onAccountDeleted?.invoke() },
                            onFailed = {
                                coroutineScope.launch {
                                    toastState.show(
                                        message = localized(R.string.we_couldn_t_delete_your_account_please_try_again, "We couldn't delete your account. Please try again."),
                                        type = ToastType.Error
                                    )
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAlert() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
