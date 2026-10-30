package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.commons.IssueTypes
import app.reportamelo.commons.Severity
import app.reportamelo.communityIssues.controllers.CreateReportViewModel
import app.reportamelo.communityIssues.controllers.InteractionType
import app.reportamelo.communityIssues.datamodel.MatterToSolve
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.helpers.cardStyle
import app.reportamelo.communityIssues.ui.components.wizard.ReportWizardContainer
import app.reportamelo.communityIssues.ui.screens.modals.ReportLimitScreen
import app.reportamelo.communityIssues.ui.screens.subviews.color
import app.reportamelo.communityIssues.ui.theme.Dimensions
import app.reportamelo.repository.UserRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportView(
    modifier: Modifier = Modifier,
    model: ReportDataModel = ReportDataModel.shared,
    controller: CreateReportViewModel = viewModel()
) {
    val state by controller.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedMatter by remember { mutableStateOf<MatterToSolve?>(null) }

    LaunchedEffect(Unit) {
        controller.getReportsCount()
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.dismissReportsLimitSheet()
        }
    }

    LaunchedEffect(state.feedbackTrigger) {
        if (state.feedbackTrigger) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    if (selectedMatter != null) {
        BackHandler {
            selectedMatter = null
        }
        ReportWizardContainer(
            model = model,
            onCompletion = { _, _ ->
                selectedMatter = null
            }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top orange ambient glow matching iOS Color.orange.opacity(0.12).frame(height: 280).blur(radius: 60)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF9500).copy(alpha = 0.16f),
                            Color(0xFFFF9500).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header row: Title + Subtitle on left, circular Filter Menu button on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Report",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "what do you want to report?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    Surface(
                        onClick = { showFilterMenu = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        Text(
                            text = "Issue Type",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        IssueTypes.entries.forEach { issueType ->
                            DropdownMenuItem(
                                text = { Text(issueType.title) },
                                leadingIcon = {
                                    if (state.issueType == issueType) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    }
                                },
                                onClick = {
                                    controller.updateIssueType(issueType)
                                    showFilterMenu = false
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Severity",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        Severity.entries.forEach { severity ->
                            DropdownMenuItem(
                                text = { Text(severity.title) },
                                leadingIcon = {
                                    if (state.severity == severity) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    }
                                },
                                onClick = {
                                    controller.updateSeverity(severity)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Rounded pill Search Bar matching iOS .searchable(text: $controller.searchText, prompt: "Search")
            OutlinedTextField(
                value = state.searchText,
                onValueChange = { controller.updateSearchText(it) },
                placeholder = {
                    Text(
                        text = "Search",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (state.searchText.isNotEmpty()) {
                        IconButton(onClick = { controller.updateSearchText("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            )

            // 2-column grid of MatterToSolve cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 108.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 3),
                verticalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 3),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.filteredMatters,
                    key = { it.id }
                ) { matter ->
                    CardView(
                        matter = matter,
                        onClick = {
                            controller.toggleFeedbackTrigger()
                            model.clear()
                            model.setMatterToSolve(matter)
                            selectedMatter = matter
                        }
                    )
                }
            }
        }

        // CustomBlurryOverlay + ReportLimit modal sheet when limit is reached
        if (state.showReportsLimitSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .blur(16.dp)
            )

            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { false }
            )

            ModalBottomSheet(
                onDismissRequest = { controller.handleSheetDismissal() },
                sheetState = sheetState,
                dragHandle = null
            ) {
                ReportLimitScreen(
                    isGuestUser = UserRepository.shared.isGuestUser(),
                    onDismiss = {
                        controller.handleUserAction(InteractionType.NO_THANKS)
                    },
                    continuationHandler = { interaction ->
                        controller.handleUserAction(interaction)
                    },
                    onShowPaywall = {
                        controller.handleUserAction(InteractionType.PAID_A_SUBSCRIPTION)
                    }
                )
            }
        }
    }
}

@Composable
fun CardView(
    modifier: Modifier = Modifier,
    matter: MatterToSolve,
    onClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(Dimensions.ThemeRadius * 1.4f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(cardShape)
            .clickable(onClick = onClick)
    ) {
        if (matter.imageRes != null) {
            Image(
                painter = painterResource(id = matter.imageRes),
                contentDescription = matter.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .cardStyle(color = matter.severity.color)
        ) {
            Text(
                text = matter.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Dimensions.ThemePadding)
            )
        }
    }
}
