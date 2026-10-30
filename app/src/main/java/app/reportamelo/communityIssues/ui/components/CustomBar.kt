package app.reportamelo.communityIssues.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

private const val SEARCH_TRANSITION_DURATION_MS = 300

@Composable
fun CustomTabBar(
    modifier: Modifier = Modifier,
    items: List<String>,
    searchHint: String = "Cities, Reports or petitions",
    selection: String,
    onSelectionChange: (String) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    onSearchActivated: (Boolean) -> Unit,
    onLocationTap: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isKeyboardActive by remember { mutableStateOf(false) }

    val barSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val barBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    // Linearly/smoothly interpolate expansion progress (0f = collapsed, 1f = expanded)
    val expandProgress by animateFloatAsState(
        targetValue = if (isSearchExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = SEARCH_TRANSITION_DURATION_MS,
            easing = FastOutSlowInEasing
        ),
        label = "SearchExpandProgress"
    )

    val clearButtonTrailingSpace by animateDpAsState(
        targetValue = if (isSearchExpanded && isKeyboardActive) 57.dp else 0.dp,
        animationSpec = tween(
            durationMillis = SEARCH_TRANSITION_DURATION_MS,
            easing = FastOutSlowInEasing
        ),
        label = "ClearButtonTrailingSpace"
    )

    val clearButtonAlpha by animateFloatAsState(
        targetValue = if (isSearchExpanded && isKeyboardActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = SEARCH_TRANSITION_DURATION_MS,
            easing = FastOutSlowInEasing
        ),
        label = "ClearButtonAlpha"
    )

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            if (scrollState.value != 0) {
                scrollState.animateScrollTo(
                    value = 0,
                    animationSpec = tween(
                        durationMillis = SEARCH_TRANSITION_DURATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        } else {
            isKeyboardActive = false
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(isKeyboardActive) {
        onSearchActivated(isKeyboardActive)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        // 16.dp start padding + 16.dp end padding + 12.dp spacing + 60.dp minimize button = 104.dp
        val fitSearchBarWidth = (maxWidth - 104.dp).coerceAtLeast(60.dp)
        val currentSearchBarWidth = lerp(60.dp, fitSearchBarWidth, expandProgress)
        // Location button (45.dp) + spacing (12.dp) = 57.dp when collapsed, 0.dp when expanded
        val locationSlotWidth = lerp(57.dp, 0.dp, expandProgress)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .horizontalScroll(scrollState, enabled = !isSearchExpanded)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location Button slot (collapses smoothly as search expands)
            if (locationSlotWidth > 0.5.dp) {
                Box(
                    modifier = Modifier
                        .width(locationSlotWidth)
                        .height(45.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Surface(
                        shape = CircleShape,
                        color = barSurfaceColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 4.dp,
                        border = BorderStroke(0.5.dp, barBorderColor),
                        modifier = Modifier
                            .size(45.dp)
                            .graphicsLayer {
                                alpha = (1f - expandProgress).coerceIn(0f, 1f)
                                val scale = lerp(1.dp, 0.7.dp, expandProgress).value
                                scaleX = scale
                                scaleY = scale
                            },
                        onClick = {
                            if (!isSearchExpanded) {
                                onLocationTap?.invoke()
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Location",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Search Bar
            Box(
                modifier = Modifier
                    .width(currentSearchBarWidth)
                    .height(45.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = barSurfaceColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 4.dp,
                    border = BorderStroke(0.5.dp, barBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(end = clearButtonTrailingSpace)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSearchExpanded) {
                                onSearchExpandedChange(true)
                            } else {
                                focusRequester.requestFocus()
                            }
                        }
                ) {
                    val iconBoxWidth = lerp(60.dp, 40.dp, expandProgress)
                    val leadingPadding = lerp(0.dp, 5.dp, expandProgress)
                    val trailingPadding = lerp(0.dp, 15.dp, expandProgress)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = leadingPadding, end = trailingPadding)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(iconBoxWidth)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (isSearchExpanded || expandProgress > 0.05f) {
                            BasicTextField(
                                value = searchText,
                                onValueChange = onSearchTextChange,
                                singleLine = true,
                                enabled = isSearchExpanded,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        isKeyboardActive = focusState.isFocused
                                    }
                                    .graphicsLayer {
                                        alpha = expandProgress.coerceIn(0f, 1f)
                                    },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchText.isEmpty()) {
                                            Text(
                                                text = searchHint,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // Trailing circular clear/dismiss button when keyboard is active (matching iOS CustomBar.swift)
                if (clearButtonAlpha > 0.01f) {
                    Surface(
                        shape = CircleShape,
                        color = barSurfaceColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 4.dp,
                        border = BorderStroke(0.5.dp, barBorderColor),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(45.dp)
                            .graphicsLayer {
                                alpha = clearButtonAlpha
                            },
                        onClick = {
                            onSearchTextChange("")
                            focusManager.clearFocus()
                            isKeyboardActive = false
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Filter Chips and Minimize Button (pushed linearly as Search Bar expands/collapses)
            Row(
                modifier = Modifier.padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selection == item
                    val isFirst = index == 0 && isSearchExpanded

                    if (isFirst) {
                        // Minimize Button (60x45 capsule with trailing 12.dp padding matching iOS)
                        Surface(
                            shape = CircleShape,
                            color = barSurfaceColor,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shadowElevation = 4.dp,
                            border = BorderStroke(0.5.dp, barBorderColor),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .width(60.dp)
                                .height(45.dp),
                            onClick = {
                                isKeyboardActive = false
                                focusManager.clearFocus()
                                onSearchExpandedChange(false)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Minimize",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        val chipBackgroundColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            barSurfaceColor
                        }
                        val chipContentColor = if (isSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            shape = CircleShape,
                            color = chipBackgroundColor,
                            contentColor = chipContentColor,
                            shadowElevation = 4.dp,
                            border = BorderStroke(0.5.dp, barBorderColor),
                            modifier = Modifier.height(45.dp),
                            enabled = !isSearchExpanded,
                            onClick = {
                                onSelectionChange(if (selection == item) "" else item)
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 15.dp)
                            ) {
                                Text(
                                    text = item,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

