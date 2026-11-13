package app.reportamelo.communityIssues.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.reportamelo.communityIssues.LocalAuthViewModel
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.helpers.DeepLinkRouter
import app.reportamelo.communityIssues.ui.screens.modals.UserProfileScreen
import app.reportamelo.communityIssues.ui.screens.tabs.CreateReportView
import app.reportamelo.communityIssues.ui.screens.tabs.InsightsView
import app.reportamelo.communityIssues.ui.screens.tabs.MapExplorerView
import app.reportamelo.communityIssues.ui.theme.CommunityIssueReporterTheme
import app.reportamelo.repository.UserRepository
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

// Left-to-right order of the tabs, used to pick the slide direction of the transitions
private val tabOrder = listOf("issues", "insights", "profile", "add")
private val pillTabs = tabOrder.take(3)

@Composable
fun TabBarView(
    modifier: Modifier = Modifier,
    landingViewModel: LandingViewModel = LandingViewModel.shared
) {
    val isPreview = LocalInspectionMode.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var previewRoute by remember { mutableStateOf("issues") }
    val currentRoute = if (isPreview) previewRoute else navBackStackEntry?.destination?.route
    val isGuest by landingViewModel.isGuest.collectAsState()
    val isGuestSession = isGuest || UserRepository.shared.isGuestUser()
    val activeTab by DeepLinkRouter.shared.activeTab.collectAsState()

    LaunchedEffect(activeTab) {
        if (!isPreview) {
            val targetRoute = when (activeTab) {
                1 -> "issues"
                3 -> "insights"
                4 -> "profile"
                5 -> "add"
                else -> null
            }
            if (targetRoute != null && currentRoute != null && currentRoute != targetRoute) {
                navigateToTab(navController, targetRoute)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        bottomBar = {
            FloatingBottomBar(
                currentRoute = currentRoute,
                isGuestSession = isGuestSession,
                onSelectTab = { route, tabIndex ->
                    if (isPreview) {
                        previewRoute = route
                    } else {
                        DeepLinkRouter.shared.activeTab.value = tabIndex
                        navigateToTab(navController, route)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (currentRoute) {
                            "insights" -> Icons.Default.AutoAwesome
                            "profile" -> Icons.Default.Person
                            "add" -> Icons.Default.Add
                            else -> Icons.Default.Map
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (currentRoute) {
                            "insights" -> "Insights Preview"
                            "profile" -> "Profile Preview"
                            "add" -> "Create Report Preview"
                            else -> "Map Explorer Preview"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = "issues",
                // Opaque backdrop so the window never shows through while two tabs cross-fade
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                composable("issues") { MapExplorerView() }
                composable("insights") { InsightsView() }
                composable("profile") {
                    UserProfileScreen(
                        landingViewModel = landingViewModel
                    )
                }
                composable("add") { CreateReportView() }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = tabOrder.indexOf(initialState.destination.route)
    val to = tabOrder.indexOf(targetState.destination.route)
    return if (to >= from) 1 else -1
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterTransition(): EnterTransition {
    val direction = tabDirection()
    return slideInHorizontally(
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        ),
        initialOffsetX = { fullWidth -> direction * fullWidth / 8 }
    ) + fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 60))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitTransition(): ExitTransition {
    val direction = tabDirection()
    return slideOutHorizontally(
        animationSpec = tween(durationMillis = 160),
        targetOffsetX = { fullWidth -> -direction * fullWidth / 8 }
    ) + fadeOut(animationSpec = tween(durationMillis = 160))
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    currentRoute: String?,
    isGuestSession: Boolean,
    onSelectTab: (route: String, tabIndex: Int) -> Unit
) {
    val barSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val barBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left pill-shaped navigation bar with Issues, Insights, Profile
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shape = CircleShape,
            color = barSurfaceColor,
            shadowElevation = 2.dp,
            border = BorderStroke(0.5.dp, barBorderColor)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                // Sliding selection indicator, like the thumb of an iOS segmented control
                val itemWidth = maxWidth / pillTabs.size
                val selectedIndex = pillTabs.indexOf(currentRoute)
                var lastPillIndex by remember { mutableIntStateOf(0) }
                if (selectedIndex >= 0 && selectedIndex != lastPillIndex) {
                    SideEffect { lastPillIndex = selectedIndex }
                }
                val indicatorIndex = if (selectedIndex >= 0) selectedIndex else lastPillIndex
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * indicatorIndex,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                    label = "tabIndicatorOffset"
                )
                // The indicator hides while the detached "add" button is the active destination
                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (currentRoute == "add") 0f else 1f,
                    label = "tabIndicatorAlpha"
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(itemWidth)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = indicatorAlpha }
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            CircleShape
                        )
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillTabBarItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Map,
                        label = stringResource(R.string.issues),
                        selected = currentRoute == "issues",
                        enabled = true,
                        onClick = { onSelectTab("issues", 1) }
                    )
                    PillTabBarItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoAwesome,
                        label = stringResource(R.string.insights),
                        selected = currentRoute == "insights",
                        enabled = !isGuestSession,
                        onClick = { onSelectTab("insights", 3) }
                    )
                    PillTabBarItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Person,
                        label = stringResource(R.string.profile),
                        selected = currentRoute == "profile",
                        enabled = true,
                        onClick = { onSelectTab("profile", 4) }
                    )
                }
            }
        }

        // Detached circular FAB (+) button on the right matching iOS prominent/search tab
        val isAddSelected = currentRoute == "add"
        val addContentColor by animateColorAsState(
            targetValue = if (isAddSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            label = "addContentColor"
        )
        FloatingActionButton(
            onClick = { onSelectTab("add", 5) },
            shape = CircleShape,
            containerColor = barSurfaceColor,
            contentColor = addContentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            modifier = Modifier
                .size(64.dp)
                .border(0.5.dp, barBorderColor, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun PillTabBarItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "tabContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun UserProfileView(modifier: Modifier = Modifier) {
    UserProfileScreen(modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun TabBarViewPreview() {
    CommunityIssueReporterTheme {
        TabBarView()
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingBottomBarPreview() {
    CommunityIssueReporterTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 16.dp)
        ) {
            FloatingBottomBar(
                currentRoute = "issues",
                isGuestSession = false,
                onSelectTab = { _, _ -> }
            )
        }
    }
}
