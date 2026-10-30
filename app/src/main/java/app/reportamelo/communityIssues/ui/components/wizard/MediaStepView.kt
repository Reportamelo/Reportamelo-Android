package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.ui.components.PhotoChooser
import app.reportamelo.communityIssues.ui.theme.Dimensions

@Composable
fun MediaStepView(
    modifier: Modifier = Modifier,
    model: ReportDataModel
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 3)
    ) {
        PhotoChooser(model = model)
    }
}
