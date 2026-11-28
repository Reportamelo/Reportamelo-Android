package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.CityFilter
import app.reportamelo.communityIssues.controllers.CitySelectionViewModel
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.ui.theme.CommunityIssueReporterTheme
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground
import app.reportamelo.models.FriendlyCityDistribution

enum class CitySelectionMode {
    MODIFY,
    STEP;

    companion object {
        val modify = MODIFY
        val step = STEP
    }
}

/**
 * Room left under the list in [CitySelectionMode.MODIFY], where the screen is hosted inside the
 * tab bar container and the floating tab bar overlaps the bottom of the content. Same clearance
 * used by [CreateReportView].
 */
private val FloatingTabBarClearance = 108.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionView(
    modifier: Modifier = Modifier,
    mode: CitySelectionMode = CitySelectionMode.STEP,
    selectedCity: FriendlyCityDistribution? = null,
    onBack: (() -> Unit)? = null,
    onCitySelected: (FriendlyCityDistribution) -> Unit = {},
    nextStep: () -> Unit = {},
    viewModel: CitySelectionViewModel = remember {
        CitySelectionViewModel(LandingViewModel.shared.countryCode.value)
    }
) {
    val state by viewModel.state.collectAsState()
    val cities = remember(state.friendlyCities) {
        state.friendlyCities.documents?.sortedByDescending { it.isCapitalCity ?: 0 } ?: emptyList()
    }

    if (state.showErrorAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(state.messageError) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .radialGradientBackground(topLeading = true, radiusFraction = 0.54f),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.select_a_city),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        Surface(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        // Only the top inset is applied to the column: the list scrolls edge-to-edge under the
        // navigation bar / floating tab bar and reserves that space through its contentPadding.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Rounded pill search bar, styled like the one in CreateReportView
            OutlinedTextField(
                value = state.searchText,
                onValueChange = { viewModel.updateSearchText(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_a_city),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        // Shifts the icon inward within the 48dp slot: more room from the pill edge, less gap to the text
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = state.searchText.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { viewModel.updateSearchText("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CityFilter.entries.forEach { filter ->
                    val isSelected = state.searchOptionsSelection == filter
                    val label = when (filter) {
                        CityFilter.CITY -> stringResource(R.string.district_city)
                        CityFilter.LEGAL -> stringResource(R.string.municipality)
                        CityFilter.STATE -> stringResource(R.string.department)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.handleSearchOptionsSelectionChange(
                                state.searchOptionsSelection,
                                filter
                            )
                        },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }

            if (cities.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.there_are_no_cities_that_match_with_your_search),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val bottomPadding = if (mode == CitySelectionMode.MODIFY) {
                    FloatingTabBarClearance
                } else {
                    paddingValues.calculateBottomPadding() + 16.dp
                }
                // Material 3 Expressive segmented list. Single-action: tapping a row commits the
                // city (save + continue), so there is no separate confirm button.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    itemsIndexed(cities, key = { _, city -> city.cityId }) { index, city ->
                        CityListItem(
                            city = city,
                            index = index,
                            count = cities.size,
                            isCurrent = city.cityId == selectedCity?.cityId,
                            isSaving = city.cityId == state.pendingCityId,
                            onClick = {
                                viewModel.updateCity(city) {
                                    // Parent only hears about a city once it has been saved,
                                    // and before nextStep() since some parents read it there.
                                    onCitySelected(city)
                                    nextStep()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One row of the segmented city list. [index] and [count] pick the segmented shape: the first and
 * last rows get the large outer corners, the middle ones the small inner corners.
 */
@Composable
fun CityListItem(
    modifier: Modifier = Modifier,
    city: FriendlyCityDistribution,
    index: Int,
    count: Int,
    isCurrent: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit
) {
    val supportingText = remember(city.legalGroupName, city.groupingName) {
        listOfNotNull(city.legalGroupName, city.groupingName?.takeIf { it.isNotBlank() })
            .joinToString(separator = "\n")
    }
    val colors = if (isCurrent) {
        ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            supportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            trailingContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    } else {
        ListItemDefaults.segmentedColors()
    }

    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier.semantics { selected = isCurrent },
        colors = colors,
        supportingContent = { Text(supportingText) },
        trailingContent = when {
            isSaving -> {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }
            isCurrent -> {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.selected_text)
                    )
                }
            }
            else -> null
        }
    ) {
        Text(
            text = city.thirdLevel,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CitySelectionViewStepPreview() {
    CommunityIssueReporterTheme {
        CitySelectionView(
            mode = CitySelectionMode.STEP
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CitySelectionViewModifyPreview() {
    CommunityIssueReporterTheme {
        CitySelectionView(
            mode = CitySelectionMode.MODIFY,
            onBack = {}
        )
    }
}
