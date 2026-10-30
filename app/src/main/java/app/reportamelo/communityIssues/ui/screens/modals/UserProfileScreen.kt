package app.reportamelo.communityIssues.ui.screens.modals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reportamelo.commons.ViewOptions
import app.reportamelo.commons.utils.userAlias
import app.reportamelo.communityIssues.LocalAuthViewModel
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.datamodel.AuthViewModel
import app.reportamelo.communityIssues.datamodel.ProfileDataModel
import app.reportamelo.communityIssues.ui.components.LinkType
import app.reportamelo.communityIssues.ui.components.LinksView
import app.reportamelo.communityIssues.ui.components.ProfileImage
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonStyle
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.screens.subviews.CommentsSubView
import app.reportamelo.communityIssues.ui.screens.subviews.LicensesSubView
import app.reportamelo.communityIssues.ui.screens.subviews.MyReportsSubView
import app.reportamelo.communityIssues.ui.screens.subviews.ReportsAndViolationsCenterView
import app.reportamelo.communityIssues.ui.screens.subviews.SettingsSubView
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.launch

enum class ProfileDestinations {
    SETTINGS, LICENSES, COMMENTS, REPORTS, MODERATION, SIGN_PETITIONS
}

data class ProfileOption(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val destination: ProfileDestinations? = null
)

/**
 * Mirrors `disableCellButton(_ id: String) -> Bool` in iOS `UserProfileView.swift`.
 * When the current session is a Guest user (`UserRepository.shared.isGuestUser()`),
 * all profile cells are disabled EXCEPT `"op:settings"` and `"op:licenses"`.
 */
fun disableCellButton(
    id: String,
    userRepository: UserRepository = UserRepository.shared
): Boolean {
    if (userRepository.isGuestUser()) {
        return !(id == "op:settings" || id == "op:licenses")
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = LocalAuthViewModel.current,
    landingViewModel: LandingViewModel = LandingViewModel.shared,
    userRepository: UserRepository = UserRepository.shared,
    isGuest: Boolean? = null,
    userName: String? = null,
    userAlias: String? = null,
    onNavigate: ((ProfileDestinations) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var currentDestination by remember { mutableStateOf<ProfileDestinations?>(null) }

    val profileDataModel by landingViewModel.profile.collectAsState()
    val landingIsGuest by landingViewModel.isGuest.collectAsState()

    val effectiveIsGuest = isGuest ?: (userRepository.isGuestUser() || landingIsGuest)
    val effectiveUserName = userName ?: userRepository.getName()
    val effectiveUserAlias = userAlias ?: userAlias(userRepository.getUsername())

    LaunchedEffect(effectiveIsGuest, effectiveUserName) {
        profileDataModel.refreshFromRepository()
        profileDataModel.setUserName(effectiveUserName)
        profileDataModel.setIsGuest(effectiveIsGuest)
    }

    if (currentDestination != null) {
        BackHandler {
            currentDestination = null
        }
        when (currentDestination) {
            ProfileDestinations.REPORTS -> {
                MyReportsSubView(
                    modifier = modifier,
                    subViewName = "My Reports",
                    mode = ViewOptions.LIST_AND_MODIFY,
                    onBack = { currentDestination = null }
                )
            }
            ProfileDestinations.COMMENTS -> {
                CommentsSubView(
                    modifier = modifier,
                    subViewName = "My Comments",
                    mode = ViewOptions.LIST,
                    onBack = { currentDestination = null }
                )
            }
            ProfileDestinations.SETTINGS -> {
                SettingsSubView(
                    modifier = modifier,
                    subViewName = "Settings",
                    onBack = { currentDestination = null }
                )
            }
            ProfileDestinations.MODERATION -> {
                ReportsAndViolationsCenterView(
                    modifier = modifier,
                    onBack = { currentDestination = null }
                )
            }
            ProfileDestinations.LICENSES -> {
                LicensesSubView(
                    modifier = modifier,
                    subViewName = "Licenses",
                    onBack = { currentDestination = null }
                )
            }
            ProfileDestinations.SIGN_PETITIONS, null -> {
                currentDestination = null
            }
        }
        return
    }

    val options = remember {
        listOf(
            ProfileOption(
                id = "op:reports",
                title = "My Reports",
                icon = Icons.Default.MarkChatUnread,
                color = Color(0xFF007AFF),
                destination = ProfileDestinations.REPORTS
            ),
            ProfileOption(
                id = "op:comments",
                title = "My Comments",
                icon = Icons.Default.ChatBubble,
                color = Color(0xFFFF9500),
                destination = ProfileDestinations.COMMENTS
            ),
            ProfileOption(
                id = "op:settings",
                title = "Settings",
                icon = Icons.Default.Settings,
                color = Color(0xFF8E8E93),
                destination = ProfileDestinations.SETTINGS
            ),
            ProfileOption(
                id = "op:moderationCenter",
                title = "Moderation Center",
                icon = Icons.Default.HowToReg,
                color = Color(0xFFFF3B30),
                destination = ProfileDestinations.MODERATION
            ),
            ProfileOption(
                id = "op:licenses",
                title = "Licenses",
                icon = Icons.Default.Description,
                color = Color(0xFF34C759),
                destination = ProfileDestinations.LICENSES
            )
        )
    }

    val isDark = isSystemInDarkTheme()
    val baseBackgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF6F6F8)
    val cardBackgroundColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9F7BF6).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(0f, 0f),
                    radius = 850f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar with centered "Profile" title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            }

            // Scrollable Profile Header & Options Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                ProfileImage(
                    profile = profileDataModel
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = effectiveUserName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = effectiveUserAlias,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Inset grouped rounded card for Profile Options
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = cardBackgroundColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        options.forEachIndexed { index, option ->
                            val isDisabled = if (effectiveIsGuest) {
                                !(option.id == "op:settings" || option.id == "op:licenses")
                            } else {
                                disableCellButton(option.id, userRepository)
                            }

                            ProfileCellView(
                                option = option,
                                isDisabled = isDisabled,
                                onClick = {
                                    option.destination?.let { destination ->
                                        onNavigate?.invoke(destination)
                                        currentDestination = destination
                                    }
                                }
                            )

                            if (index < options.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Section: Log Out button & Terms/Privacy/EULA/Policies links
            // Mirrors iOS .safeAreaInset(edge: .bottom) positioned above FloatingBottomBar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(
                        top = 8.dp,
                        bottom = if (onDismiss == null) 90.dp else 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemedButton(
                    message = "Log Out",
                    type = ThemedButtonType.OUTLINE,
                    style = ThemedButtonStyle.NORMAL,
                    isLoading = isLoading,
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                userRepository.logout()
                            } catch (_: Exception) {
                                // Ephemeral guest sessions (30m) or offline states still log out locally
                            } finally {
                                isLoading = false
                                landingViewModel.logout(authViewModel)
                                onLogout?.invoke()
                            }
                        }
                    }
                )

                LinksView(
                    type = LinkType.Full,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileCellView(
    modifier: Modifier = Modifier,
    option: ProfileOption,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isDisabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .alpha(if (isDisabled) 0.42f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(option.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.title,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = option.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(20.dp)
        )
    }
}
