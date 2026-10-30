package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import app.reportamelo.communityIssues.controllers.CityFilter
import app.reportamelo.communityIssues.controllers.CitySelectionViewModel
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonStyle
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground
import app.reportamelo.models.FriendlyCityDistribution

enum class CitySelectionMode { MODIFY, STEP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionView(
    modifier: Modifier = Modifier,
    mode: CitySelectionMode = CitySelectionMode.STEP,
    selectedCity: FriendlyCityDistribution? = null,
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
    val listState = rememberLazyListState()

    if (state.showErrorAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
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
                        text = "Select a city",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 36.dp)
                    .padding(top = 16.dp, bottom = if (mode == CitySelectionMode.MODIFY) 16.dp else 12.dp)
            ) {
                ThemedButton(
                    message = if (mode == CitySelectionMode.MODIFY) "Select" else "Next Step",
                    onClick = {
                        val targetCity = selectedCity ?: cities.firstOrNull()
                        if (targetCity != null) {
                            onCitySelected(targetCity)
                            viewModel.updateCity(targetCity) {
                                nextStep()
                            }
                        } else {
                            nextStep()
                        }
                    },
                    type = ThemedButtonType.SECONDARY,
                    style = ThemedButtonStyle.PROMINENT,
                    isLoading = state.isLoading,
                    isEnabled = selectedCity != null || cities.isNotEmpty()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = state.searchText,
                onValueChange = { viewModel.updateSearchText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = CircleShape,
                placeholder = {
                    Text(
                        text = "Search a city",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CityFilter.entries.forEach { filter ->
                    val isSelected = state.searchOptionsSelection == filter
                    val label = when (filter) {
                        CityFilter.CITY -> "District / City"
                        CityFilter.LEGAL -> "Municipality"
                        CityFilter.STATE -> "Department"
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
                        text = "There are no cities that match with your search.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(cities, key = { it.cityId }) { city ->
                        val itemZIndex by remember(listState, city.cityId) {
                            derivedStateOf {
                                calculateStackingZIndex(listState, city.cityId)
                            }
                        }
                        CityCellView(
                            modifier = Modifier
                                .zIndex(itemZIndex)
                                .scrollStackingEffect(listState = listState, itemKey = city.cityId),
                            city = city,
                            isSelected = selectedCity?.cityId == city.cityId,
                            onClick = { onCitySelected(city) }
                        )
                    }
                }
            }
        }
    }
}

private fun calculateStackingZIndex(
    listState: LazyListState,
    itemKey: Any
): Float {
    val layoutInfo = listState.layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == itemKey } ?: return 1f
    val itemSize = itemInfo.size.toFloat().coerceAtLeast(1f)
    val viewportTop = 0f
    val viewportBottom = (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding).toFloat()

    return when {
        itemInfo.offset < viewportTop -> {
            val visibility = ((itemInfo.offset + itemInfo.size - viewportTop) / itemSize).coerceIn(0f, 1f)
            visibility
        }
        itemInfo.offset + itemInfo.size > viewportBottom -> {
            val visibility = ((viewportBottom - itemInfo.offset) / itemSize).coerceIn(0f, 1f)
            visibility
        }
        else -> 1f
    }
}

private fun Modifier.scrollStackingEffect(
    listState: LazyListState,
    itemKey: Any
): Modifier = this.graphicsLayer {
    val layoutInfo = listState.layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == itemKey }
    if (itemInfo == null) {
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        translationY = 0f
        return@graphicsLayer
    }

    val itemSize = itemInfo.size.toFloat().coerceAtLeast(1f)
    val viewportTop = 0f
    val viewportBottom = (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding).toFloat()

    when {
        // Exiting through the top edge of the LazyColumn
        itemInfo.offset < viewportTop -> {
            val visibility = ((itemInfo.offset + itemInfo.size - viewportTop) / itemSize).coerceIn(0f, 1f)
            val targetScale = lerp(0.92f, 1f, visibility)
            scaleX = targetScale
            scaleY = targetScale
            alpha = lerp(0.6f, 1f, visibility)
            translationY = (viewportTop - itemInfo.offset) * 0.45f
        }
        // Entering or exiting through the bottom edge of the LazyColumn
        itemInfo.offset + itemInfo.size > viewportBottom -> {
            val visibility = ((viewportBottom - itemInfo.offset) / itemSize).coerceIn(0f, 1f)
            val targetScale = lerp(0.92f, 1f, visibility)
            scaleX = targetScale
            scaleY = targetScale
            alpha = lerp(0.6f, 1f, visibility)
            translationY = -((itemInfo.offset + itemInfo.size) - viewportBottom) * 0.45f
        }
        else -> {
            scaleX = 1f
            scaleY = 1f
            alpha = 1f
            translationY = 0f
        }
    }
}

@Composable
fun CityCellView(
    modifier: Modifier = Modifier,
    city: FriendlyCityDistribution,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(min = 95.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            BorderStroke(6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.67f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 95.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = city.thirdLevel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = city.legalGroupName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!city.groupingName.isNullOrEmpty()) {
                Text(
                    text = city.groupingName!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
