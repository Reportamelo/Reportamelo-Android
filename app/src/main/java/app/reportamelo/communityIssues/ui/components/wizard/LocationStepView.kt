package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reportamelo.communityIssues.datamodel.LocatorDAO
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.ui.components.MiniMapLocator
import app.reportamelo.communityIssues.ui.screens.modals.MapPickerScreen
import app.reportamelo.communityIssues.ui.theme.Dimensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LocationStepView(
    modifier: Modifier = Modifier,
    model: ReportDataModel,
    onMapTouchChanged: (Boolean) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val report by model.report.collectAsState()
    val currentLocator by model.locator.collectAsState()
    var showMapPickerModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniMapLocator(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimensions.ThemeRadius * 2))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(Dimensions.ThemeRadius * 2)
                ),
            coordinate = report.coordinate,
            onExpandMap = { currentCenter ->
                model.updateCoordinate(currentCenter)
                showMapPickerModal = true
            },
            onCoordinateChanged = { newCoordinate ->
                model.updateCoordinate(newCoordinate)
                if (currentLocator.cityId.isEmpty()) {
                    model.updateLocator(
                        LocatorDAO.defaultLocator(
                            lat = newCoordinate.lat,
                            lng = newCoordinate.lng
                        )
                    )
                }
                model.setDifferentLocation(true)
            },
            onLoadingChanged = { isLoading ->
                model.setLoadingAddress(isLoading)
            },
            onMapTouchChanged = onMapTouchChanged,
            onChange = { newCoordinate, resolvedLocator ->
                model.updateCoordinate(newCoordinate)
                model.updateLocator(resolvedLocator)
                model.setDifferentLocation(true)
            }
        )

        Text(
            text = "Please pan the map to show us where the issue is.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showMapPickerModal) {
        Dialog(
            onDismissRequest = { showMapPickerModal = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            MapPickerScreen(
                initialCoordinate = report.coordinate,
                model = model,
                onDismiss = { showMapPickerModal = false },
                onConfirm = { confirmedCoordinate, confirmedLocator ->
                    model.updateCoordinate(confirmedCoordinate)
                    model.updateLocator(confirmedLocator)
                    model.setDifferentLocation(true)
                    showMapPickerModal = false
                    // Resolve latest address asynchronously in case the user confirmed mid-pan
                    CoroutineScope(Dispatchers.Main).launch {
                        model.setLoadingAddress(true)
                        val resolved = LocatorDAO.findPlace(
                            context,
                            confirmedCoordinate.lat,
                            confirmedCoordinate.lng
                        )
                        model.updateLocator(resolved)
                        model.setLoadingAddress(false)
                    }
                }
            )
        }
    }
}
