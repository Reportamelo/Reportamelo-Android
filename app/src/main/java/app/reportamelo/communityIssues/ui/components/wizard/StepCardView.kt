package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.datamodel.ReportStates
import app.reportamelo.communityIssues.ui.theme.Dimensions

@Composable
fun StepCardView(
    modifier: Modifier = Modifier,
    step: ReportStep,
    currentStep: ReportStep,
    metadata: StepsMetadata?,
    state: ReportStates,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val isActive = step == currentStep
    val isPast = step.rawValue < currentStep.rawValue
    // Once submitted (Confirmation step), tapping previous steps is disabled
    val canTapHeader = currentStep != ReportStep.CONFIRMATION &&
        !isActive &&
        (isPast || (state == ReportStates.MODIFYING && step != ReportStep.CONFIRMATION))

    val scale = if (isActive) 1f else if (isPast) 0.96f else 0.98f
    val alpha = when {
        isActive -> 1f
        state == ReportStates.MODIFYING && currentStep != ReportStep.CONFIRMATION -> 1f
        isPast -> 0.75f
        else -> 0.55f
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .then(
                if (canTapHeader) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = Dimensions.ThemeSpacing * 1.5f),
        verticalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing)
    ) {
        if (isActive) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 2)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 2)
                ) {
                    if (metadata != null) {
                        Icon(
                            imageVector = metadata.icon,
                            contentDescription = metadata.title,
                            tint = step.color,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = metadata?.title ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = metadata?.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(modifier = Modifier.padding(top = 6.dp)) {
                        content()
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 2)
            ) {
                if (metadata != null) {
                    Icon(
                        imageVector = metadata.icon,
                        contentDescription = metadata.title,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = metadata?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isPast) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
