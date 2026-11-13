package app.reportamelo.communityIssues.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

@Composable
fun StepsIndicator(
    numberOfSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (numberOfSteps > 0) currentStep.toFloat() / numberOfSteps.toFloat() else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.55f),
        label = "progress"
    )

    Column(
        modifier = modifier
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.step_lld_of_lld, currentStep, numberOfSteps),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
