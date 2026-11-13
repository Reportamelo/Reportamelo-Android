package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.communityIssues.controllers.ImagePhase
import app.reportamelo.communityIssues.controllers.ReportController
import app.reportamelo.communityIssues.datamodel.Report
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.datamodel.ReportStates
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

// Clearance for TabBarView's FloatingBottomBar
private val TabBarClearance = 84.dp

@Composable
fun ReportWizardContainer(
    modifier: Modifier = Modifier,
    model: ReportDataModel,
    onCompletion: (String, String) -> Unit,
    showCancelButton: Boolean = false,
    reportToModify: Report? = null
) {
    val controller: ReportController = viewModel()
    val scrollState = rememberScrollState()
    val steps = ReportStep.entries

    val currentStep by controller.currentStep.collectAsState()
    val report by model.report.collectAsState()
    val locator by model.locator.collectAsState()
    val isDifferentLocation by model.isDifferentLocation.collectAsState()
    val uploadTrackers by model.uploadTrackers.collectAsState()
    val isTitleValid by model.isTitleValid.collectAsState()
    val isDescriptionValid by model.isDescriptionValid.collectAsState()
    val isAddressValid by model.isAddressValid.collectAsState()

    val isLoading by controller.isLoading.collectAsState()
    val reportId by controller.reportId.collectAsState()
    val shareableLink by controller.shareableLink.collectAsState()
    val presentAlert by controller.presentAlert.collectAsState()
    val alertMessage by controller.alertMessage.collectAsState()

    // Observe each tracker's phase reactively so the Next button in Step 2 updates immediately when uploads finish
    val trackerPhases = uploadTrackers.map { tracker ->
        tracker.phase.collectAsState().value
    }

    val isLocationStepReadyToContinue = isDifferentLocation && locator.cityId.isNotEmpty()
    val isMediaStepReadyToContinue = uploadTrackers.isNotEmpty() &&
        trackerPhases.all { it == ImagePhase.SUCCESS }
    val isDetailsStepReadyToContinue = isTitleValid && isDescriptionValid && isAddressValid

    val disableButton = when (currentStep) {
        ReportStep.LOCATION -> !isLocationStepReadyToContinue
        ReportStep.MEDIA -> !isMediaStepReadyToContinue
        ReportStep.DETAILS -> !isDetailsStepReadyToContinue
        ReportStep.CONFIRMATION -> false
    }

    val stepYOffsets = remember { mutableStateMapOf<ReportStep, Int>() }

    // Mirrors iOS safeAreaInset(edge: .bottom): the footer rides on top of the keyboard and the
    // scroll viewport ends at the footer, so the focused input is brought into view above both
    val density = LocalDensity.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val restingFooterInset =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TabBarClearance
    val isKeyboardRaised = imeBottom > restingFooterInset
    var footerHeight by remember { mutableStateOf(0.dp) }
    var isMapTouchActive by remember { mutableStateOf(false) }

    LaunchedEffect(reportToModify) {
        if (reportToModify != null) {
            model.prepareForModification(reportToModify)
        }
        controller.startReporting(model)
    }

    LaunchedEffect(currentStep) {
        val targetY = stepYOffsets[currentStep] ?: 0
        scrollState.animateScrollTo((targetY - 24).coerceAtLeast(0))
    }

    if (presentAlert) {
        AlertDialog(
            onDismissRequest = { controller.dismissAlert() },
            title = { Text(stringResource(R.string.status_update)) },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { controller.dismissAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    val gradientColor by animateColorAsState(
        targetValue = currentStep.color.copy(alpha = 0.07f),
        label = "stepGlowColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top ambient glow matching iOS ReportWizardContainer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(gradientColor, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = if (isKeyboardRaised) footerHeight else 0.dp)
        ) {
            // Top bar matching iOS navigation header (Back button on left, centered title + Step X of 4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (currentStep != ReportStep.CONFIRMATION) {
                    Surface(
                        onClick = {
                            model.clear()
                            onCompletion("", "CANCEL")
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (showCancelButton) {
                                    Icons.Default.Close
                                } else {
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                },
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = controller.getNavigationTitle(report.reportState),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.step_lld_of_4, currentStep.rawValue),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Vertical Wizard Accordion List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState, enabled = !isMapTouchActive)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .padding(bottom = if (isKeyboardRaised) 12.dp else 180.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                stepYOffsets[step] = coords.positionInParent().y.roundToInt()
                            }
                    ) {
                        StepCardView(
                            step = step,
                            currentStep = currentStep,
                            metadata = stepsMetadata[step.metadataKey],
                            state = report.reportState,
                            onClick = {
                                if (currentStep != ReportStep.CONFIRMATION) {
                                    if (report.reportState == ReportStates.NEW) {
                                        if (step.rawValue < currentStep.rawValue) {
                                            controller.setStep(step)
                                        }
                                    } else {
                                        if (step.rawValue < ReportStep.CONFIRMATION.rawValue) {
                                            controller.setStep(step)
                                        }
                                    }
                                }
                            }
                        ) {
                            when (step) {
                                ReportStep.LOCATION -> LocationStepView(
                                    model = model,
                                    onMapTouchChanged = { isMapTouchActive = it }
                                )
                                ReportStep.MEDIA -> MediaStepView(model = model)
                                ReportStep.DETAILS -> DetailsView(model = model)
                                ReportStep.CONFIRMATION -> ConfirmationView(
                                    id = reportId,
                                    url = shareableLink
                                )
                            }
                        }

                        if (index < steps.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Wizard Footer positioned cleanly above the FloatingBottomBar
        WizardFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { footerHeight = with(density) { it.height.toDp() } },
            bottomInset = maxOf(imeBottom, restingFooterInset),
            currentStep = currentStep,
            buttonMessage = controller.getButtonMessage(),
            disableButton = disableButton,
            isLoading = isLoading,
            onNext = {
                if (currentStep.rawValue < ReportStep.DETAILS.rawValue) {
                    controller.goNext()
                } else if (currentStep == ReportStep.DETAILS) {
                    controller.submitReport(
                        model = model,
                        attachments = uploadTrackers
                    ) {
                        controller.goNext()
                    }
                }
            },
            onDone = {
                val finalId = reportId
                model.clear()
                onCompletion(finalId, "SUCCESS")
            }
        )
    }
}

@Composable
fun WizardFooter(
    modifier: Modifier = Modifier,
    bottomInset: Dp,
    currentStep: ReportStep,
    buttonMessage: String,
    disableButton: Boolean,
    isLoading: Boolean,
    onNext: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(bottom = bottomInset)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (currentStep.rawValue < ReportStep.CONFIRMATION.rawValue) {
            ThemedButton(
                message = buttonMessage,
                onClick = onNext,
                type = ThemedButtonType.SECONDARY,
                isEnabled = !disableButton,
                isLoading = isLoading
            )
        } else {
            ThemedButton(
                message = stringResource(R.string.done),
                onClick = onDone,
                type = ThemedButtonType.SECONDARY
            )
        }
    }
}
