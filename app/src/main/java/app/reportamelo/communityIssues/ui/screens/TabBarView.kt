package app.reportamelo.communityIssues.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import app.reportamelo.repository.UserRepository

@Preview
@Composable
fun TabBarView(
    modifier: Modifier = Modifier,
    landingViewModel: LandingViewModel = LandingViewModel.shared
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isGuest by landingViewModel.isGuest.collectAsState()
    val authViewModel = LocalAuthViewModel.current
    val isGuestSession = isGuest || UserRepository.shared.isGuestUser()
    val activeTab by DeepLinkRouter.shared.activeTab.collectAsState()

    LaunchedEffect(activeTab) {
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

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        bottomBar = {
            FloatingBottomBar(
                currentRoute = currentRoute,
                isGuestSession = isGuestSession,
                onSelectTab = { route, tabIndex ->
                    DeepLinkRouter.shared.activeTab.value = tabIndex
                    navigateToTab(navController, route)
                }
            )
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = "issues",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("issues") { MapExplorerView() }
            composable("insights") { InsightsView() }
            composable("profile") {
                UserProfileScreen(
                    authViewModel = authViewModel,
                    landingViewModel = landingViewModel
                )
            }
            composable("add") { CreateReportView() }
        }
    }
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
            shadowElevation = 6.dp,
            border = BorderStroke(0.5.dp, barBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillTabBarItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Map,
                    label = "Issues",
                    selected = currentRoute == "issues",
                    enabled = true,
                    onClick = { onSelectTab("issues", 1) }
                )
                PillTabBarItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AutoAwesome,
                    label = "Insights",
                    selected = currentRoute == "insights",
                    enabled = !isGuestSession,
                    onClick = { onSelectTab("insights", 3) }
                )
                PillTabBarItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    label = "Profile",
                    selected = currentRoute == "profile",
                    enabled = true,
                    onClick = { onSelectTab("profile", 4) }
                )
            }
        }

        // Detached circular FAB (+) button on the right matching iOS prominent/search tab
        val isAddSelected = currentRoute == "add"
        FloatingActionButton(
            onClick = { onSelectTab("add", 5) },
            shape = CircleShape,
            containerColor = barSurfaceColor,
            contentColor = if (isAddSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier
                .size(64.dp)
                .border(0.5.dp, barBorderColor, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
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
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                }
            )
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
