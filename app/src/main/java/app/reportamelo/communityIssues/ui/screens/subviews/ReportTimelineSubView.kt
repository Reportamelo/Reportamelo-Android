package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.ReportTimelineUiState
import app.reportamelo.communityIssues.controllers.ReportTimelineViewModel
import app.reportamelo.communityIssues.ui.components.IssueTimeLineView
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground

/**
 * Destino que hospeda `IssueTimeLineView`.
 *
 * `IssueTimeLineView` se reutiliza tal cual: aquí se resuelven loading, contenido,
 * vacío y error, y la resolución ya cargada se le pasa como `preloadedResolution`
 * para que no repita la petición.
 */
@Composable
fun ReportTimelineSubView(
    modifier: Modifier = Modifier,
    reportId: String,
    subViewName: String = stringResource(R.string.report_action_timeline),
    onBack: (() -> Unit)? = null,
    viewModel: ReportTimelineViewModel = viewModel(key = "timeline-$reportId") {
        ReportTimelineViewModel(reportId)
    }
) {
    val uiState by viewModel.uiState.collectAsState()

    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .radialGradientBackground(topLeading = true, radiusFraction = 0.54f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (onBack != null) {
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .align(Alignment.CenterStart),
                        shape = CircleShape,
                        color = cardBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Text(
                    text = subViewName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = uiState) {
                    is ReportTimelineUiState.Loading -> TimelineSkeleton()

                    is ReportTimelineUiState.Content -> IssueTimeLineView(
                        reportId = reportId,
                        preloadedResolution = state.resolution
                    )

                    is ReportTimelineUiState.Empty -> TimelineMessage(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.HistoryToggleOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color(0xFF8E8E93)
                            )
                        },
                        message = stringResource(R.string.report_timeline_empty)
                    )

                    is ReportTimelineUiState.Error -> TimelineMessage(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        message = stringResource(R.string.report_timeline_error),
                        action = {
                            Button(onClick = { viewModel.load() }) {
                                Text(text = stringResource(R.string.retry))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineMessage(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    message: String,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(20.dp))
            action()
        }
    }
}

/** Esqueleto con la silueta de la línea de tiempo: nodo + texto, cuatro veces. */
@Composable
private fun TimelineSkeleton(modifier: Modifier = Modifier) {
    val placeholder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(8.dp))
                .background(placeholder)
        )

        repeat(4) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(placeholder)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.45f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(placeholder)
                    )
                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .fillMaxWidth(0.75f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(placeholder)
                    )
                }
            }
        }
    }
}
