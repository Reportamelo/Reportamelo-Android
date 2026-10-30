package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.reportamelo.commons.Validator
import app.reportamelo.communityIssues.ui.theme.DarkInputBackground
import app.reportamelo.communityIssues.ui.theme.DarkInputBorder
import app.reportamelo.communityIssues.ui.theme.LightInputBackground
import app.reportamelo.communityIssues.ui.theme.LightInputBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    validators: List<Validator> = emptyList(),
    onValidChange: (Boolean) -> Unit = {},
    errorMessage: String? = null,
    isError: Boolean = false,
    isDisabled: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val isDark = isSystemInDarkTheme()
    var hasInteracted by remember { mutableStateOf(false) }

    val failedValidatorMessage = remember(value, validators) {
        if (validators.isEmpty()) {
            null
        } else {
            validators.firstOrNull { !it.fn(value) }?.message
        }
    }

    LaunchedEffect(value, validators) {
        if (validators.isNotEmpty()) {
            onValidChange(failedValidatorMessage == null)
        }
    }

    val showValidationError = (hasInteracted && failedValidatorMessage != null) || isError
    val resolvedErrorMessage = failedValidatorMessage ?: errorMessage

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDisabled) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            },
            modifier = Modifier.padding(start = 12.dp)
        )

        val fieldModifier = if (focusRequester != null) {
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        } else {
            Modifier.fillMaxWidth()
        }

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                hasInteracted = true
                onValueChange(newValue)
            },
            modifier = fieldModifier,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            isError = showValidationError,
            enabled = !isDisabled,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) DarkInputBackground else LightInputBackground,
                unfocusedContainerColor = if (isDark) DarkInputBackground else LightInputBackground,
                disabledContainerColor = (if (isDark) DarkInputBackground else LightInputBackground).copy(alpha = 0.6f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isDark) DarkInputBorder else LightInputBorder,
                errorBorderColor = MaterialTheme.colorScheme.error,
                disabledBorderColor = (if (isDark) DarkInputBorder else LightInputBorder).copy(alpha = 0.5f)
            )
        )

        if (showValidationError && !resolvedErrorMessage.isNullOrEmpty()) {
            Text(
                text = resolvedErrorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
