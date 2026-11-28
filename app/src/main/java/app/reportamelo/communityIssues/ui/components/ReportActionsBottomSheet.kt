package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reportamelo.commons.IssueStatus
import app.reportamelo.communityIssues.R

/** Acciones del menú contextual de un reporte. Espejo de `reportContextMenu` en iOS. */
enum class ReportAction {
    OPEN_IN_MAPS,
    COPY_ID,
    SHARE_REPORT,
    TIMELINE,
    DELETE
}

/**
 * Estatus en los que el ciudadano todavía puede borrar su reporte. En cualquier otro
 * el reporte ya entró al flujo de la institución y la fila se omite por completo.
 */
val DELETABLE_REPORT_STATUSES: Set<IssueStatus> =
    setOf(IssueStatus.REJECTED, IssueStatus.RECTIFICATION)

fun canDeleteReport(status: IssueStatus): Boolean = status in DELETABLE_REPORT_STATUSES

/**
 * Acciones disponibles para un reporte, en el orden en que se muestran.
 * `DELETE` solo aparece cuando el estatus lo permite (se oculta, no se deshabilita).
 */
fun reportActionsFor(status: IssueStatus): List<ReportAction> = buildList {
    add(ReportAction.OPEN_IN_MAPS)
    add(ReportAction.COPY_ID)
    add(ReportAction.SHARE_REPORT)
    add(ReportAction.TIMELINE)
    if (canDeleteReport(status)) add(ReportAction.DELETE)
}

private val ReportAction.icon: ImageVector
    get() = when (this) {
        ReportAction.OPEN_IN_MAPS -> Icons.Default.Map
        ReportAction.COPY_ID -> Icons.Default.ContentCopy
        ReportAction.SHARE_REPORT -> Icons.Default.Share
        ReportAction.TIMELINE -> Icons.Default.History
        ReportAction.DELETE -> Icons.Default.DeleteOutline
    }

private val ReportAction.labelRes: Int
    get() = when (this) {
        ReportAction.OPEN_IN_MAPS -> R.string.report_action_open_in_maps
        ReportAction.COPY_ID -> R.string.report_action_copy_id
        ReportAction.SHARE_REPORT -> R.string.report_action_share
        ReportAction.TIMELINE -> R.string.report_action_timeline
        ReportAction.DELETE -> R.string.report_action_delete
    }

/**
 * Menú de acciones por reporte. Composable de presentación: no conoce red ni navegación,
 * solo emite la acción elegida.
 *
 * A diferencia del `UIContextMenu` de iOS, el sheet tapa la tarjeta con un scrim, así que
 * lleva un encabezado que identifica el reporte sobre el que se va a actuar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportActionsBottomSheet(
    modifier: Modifier = Modifier,
    reportTitle: String,
    reportId: String?,
    status: IssueStatus,
    // El contenido es corto: no debe tener estado "medio abierto".
    sheetState: SheetState = rememberBottomSheetState(
        SheetValue.Hidden,
        setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
    onDismissRequest: () -> Unit,
    onAction: (ReportAction) -> Unit
) {
    val actions = reportActionsFor(status)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp)
        ) {
            // Encabezado: sin él, con el scrim encima, no se sabe sobre qué tarjeta se actúa.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp)
                    .semantics { heading() },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = reportTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.id_s, reportId.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            actions.forEach { action ->
                // Delete va al final, separada por su propio divider. Si no está, tampoco se dibuja.
                if (action == ReportAction.DELETE) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }

                ReportActionRow(
                    action = action,
                    onClick = { onAction(action) }
                )
            }
        }
    }
}

@Composable
private fun ReportActionRow(
    modifier: Modifier = Modifier,
    action: ReportAction,
    onClick: () -> Unit
) {
    val isDestructive = action == ReportAction.DELETE
    val contentColor: Color = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val label = stringResource(action.labelRes)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}
