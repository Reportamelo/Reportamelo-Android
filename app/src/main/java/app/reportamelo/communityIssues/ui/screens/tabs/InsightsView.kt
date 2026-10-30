package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.communityIssues.controllers.InsightsNavigation
import app.reportamelo.communityIssues.controllers.InsightsViewModel
import app.reportamelo.communityIssues.ui.components.StatsCardsView
import app.reportamelo.communityIssues.ui.screens.subviews.InsightsCalendarView
import app.reportamelo.communityIssues.ui.screens.subviews.MyPetitionsSubView
import app.reportamelo.communityIssues.ui.screens.subviews.MyReportsSubView
import app.reportamelo.communityIssues.ui.screens.subviews.SimpleView

@Composable
fun InsightsView(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = viewModel()
) {
    val navigationPath by viewModel.navigationPath.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        viewModel.getInsights()
    }

    BackHandler(enabled = navigationPath.isNotEmpty()) {
        viewModel.navigateBack()
    }

    val currentDestination = navigationPath.lastOrNull()
    if (currentDestination != null) {
        when (currentDestination) {
            InsightsNavigation.MyReports -> {
                MyReportsSubView(
                    modifier = modifier,
                    subViewName = "My reports",
                    onBack = { viewModel.navigateBack() }
                )
            }
            InsightsNavigation.MyPetitions -> {
                MyPetitionsSubView(
                    subViewName = "My petitions"
                )
            }
            is InsightsNavigation.Activity -> {
                SimpleView(
                    modifier = modifier,
                    title = "ActivityListView",
                    selectedDay = insights.recentActivity[currentDestination.date],
                    dateFormatter = currentDestination.date,
                    onBack = { viewModel.navigateBack() }
                )
            }
            InsightsNavigation.NoActivity -> {
                SimpleView(
                    modifier = modifier,
                    title = "NoActivityView",
                    onBack = { viewModel.navigateBack() }
                )
            }
            is InsightsNavigation.ReportNav -> {
                SimpleView(
                    modifier = modifier,
                    title = currentDestination.report.title,
                    onBack = { viewModel.navigateBack() }
                )
            }
            is InsightsNavigation.PetitionNav -> {
                SimpleView(
                    modifier = modifier,
                    title = currentDestination.petition.title,
                    onBack = { viewModel.navigateBack() }
                )
            }
        }
        return
    }

    val gradientAlpha = if (isDark) 0.421f else 0.09123f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top vertical gradient matching Color.theme.secondary.opacity(colorScheme == .dark ? 0.421 : 0.09123).frame(height: 400)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = gradientAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Insights",
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            StatsCardsView(
                insights = insights,
                onNavigateToReports = {
                    viewModel.navigateTo(InsightsNavigation.MyReports)
                },
                onNavigateToPetitions = {
                    viewModel.navigateTo(InsightsNavigation.MyPetitions)
                }
            )

            Spacer(modifier = Modifier.height(36.dp))

            InsightsCalendarView(
                activityData = insights.recentActivity,
                onNavigate = { destination ->
                    viewModel.navigateTo(destination)
                }
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
