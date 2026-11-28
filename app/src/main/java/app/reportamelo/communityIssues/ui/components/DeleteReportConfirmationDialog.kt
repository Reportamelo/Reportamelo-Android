package app.reportamelo.communityIssues.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

/**
 * Confirmación previa al borrado de un reporte.
 *
 * El borrado es pesimista: se confirma aquí y recién entonces se llama al backend.
 * No hay "deshacer" sobre un reporte ya enviado.
 */
@Composable
fun DeleteReportConfirmationDialog(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.report_delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.report_delete_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.report_delete_dialog_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.report_delete_dialog_cancel))
            }
        }
    )
}
