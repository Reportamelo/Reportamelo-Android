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
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

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
                description = stringResource(R.string.i_ve_reported),
                title = insights.totalReports.toString(),
                trend = stringResource(R.string.issues_this_month),
                timeframe = ""
            )

            if (enableOther) {
                StatCard(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimensions.ThemeCardCornerRadius))
                        .clickable { onNavigateToPetitions() },
                    description = stringResource(R.string.i_ve_created),
                    title = insights.totalPetitions.toString(),
                    trend = stringResource(R.string.petitions_this_month),
                    timeframe = stringResource(R.string.for_the_last_6_months)
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
                description = stringResource(R.string.i_ve_commented_on),
                title = insights.totalComments.toString(),
                trend = stringResource(R.string.reports),
                timeframe = stringResource(R.string.for_the_last_6_months)
            )

            if (enableOther) {
                StatCard(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimensions.ThemeCardCornerRadius)),
                    description = stringResource(R.string.i_ve_signed_on),
                    title = insights.totalSignatures.toString(),
                    trend = stringResource(R.string.petitions),
                    timeframe = stringResource(R.string.for_the_last_6_months)
                )
            }
        }
    }
}
