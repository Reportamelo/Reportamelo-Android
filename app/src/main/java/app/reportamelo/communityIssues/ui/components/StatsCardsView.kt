package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.ui.theme.Dimensions
import app.reportamelo.models.MonthlyInsightsResponse

@Composable
fun StatsCardsView(
    modifier: Modifier = Modifier,
    insights: MonthlyInsightsResponse,
    onNavigateToReports: () -> Unit,
    onNavigateToPetitions: () -> Unit,
    enableOther: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.ThemePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimensions.ThemeCardCornerRadius))
                    .clickable { onNavigateToReports() },
                description = "I've reported",
                title = insights.totalReports.toString(),
                trend = "Issues this month",
                timeframe = ""
            )

            if (enableOther) {
                StatCard(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimensions.ThemeCardCornerRadius))
                        .clickable { onNavigateToPetitions() },
                    description = "I've created",
                    title = insights.totalPetitions.toString(),
                    trend = "Petitions this month",
                    timeframe = "for the last 6 months"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimensions.ThemeCardCornerRadius)),
                description = "I've commented on",
                title = insights.totalComments.toString(),
                trend = "Reports",
                timeframe = "for the last 6 months"
            )

            if (enableOther) {
                StatCard(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimensions.ThemeCardCornerRadius)),
                    description = "I've signed on",
                    title = insights.totalSignatures.toString(),
                    trend = "Petitions",
                    timeframe = "for the last 6 months"
                )
            }
        }
    }
}
