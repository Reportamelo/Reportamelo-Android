package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import app.reportamelo.commons.addressValidator
import app.reportamelo.commons.descriptionValidator
import app.reportamelo.commons.titleValidator
import app.reportamelo.communityIssues.datamodel.ReportDataModel
import app.reportamelo.communityIssues.ui.components.TextInput
import kotlinx.coroutines.delay

@Composable
fun DetailsView(
    modifier: Modifier = Modifier,
    model: ReportDataModel
) {
    val report by model.report.collectAsState()
    val titlePlaceholder by model.titlePlaceholder.collectAsState()
    val descriptionPlaceholder by model.descriptionPlaceholder.collectAsState()
    val isLoadingAddress by model.isLoadingAddress.collectAsState()

    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(250)
        runCatching { titleFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextInput(
            value = report.title,
            onValueChange = { model.updateTitle(it) },
            label = "Title of the issue",
            placeholder = titlePlaceholder,
            validators = titleValidator,
            onValidChange = { model.setTitleValid(it) },
            singleLine = true,
            focusRequester = titleFocusRequester
        )

        TextInput(
            value = report.description,
            onValueChange = { model.updateDescription(it) },
            label = "Please describe the issue",
            placeholder = descriptionPlaceholder,
            validators = descriptionValidator,
            onValidChange = { model.setDescriptionValid(it) },
            singleLine = false,
            minLines = 2,
            maxLines = 5
        )

        TextInput(
            value = report.address,
            onValueChange = { model.updateAddress(it) },
            label = "Please let us know where the issue is.",
            placeholder = "Address",
            validators = addressValidator,
            onValidChange = { model.setAddressValid(it) },
            isDisabled = isLoadingAddress,
            singleLine = false,
            minLines = 2,
            maxLines = 4
        )
    }
}
