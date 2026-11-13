package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.OTP_LENGTH
import app.reportamelo.communityIssues.controllers.sanitizeOtp
import app.reportamelo.communityIssues.services.OtpStatus

/**
 * Alphanumeric OTP input, rendered as two groups of cells separated by a dash (ABC - 123).
 * A single hidden text field backs all the cells, so pasting and deleting work as in a regular input.
 */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    status: OtpStatus = OtpStatus.IDLE,
    enabled: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    var isFocused by remember { mutableStateOf(false) }
    val description = stringResource(R.string.otp_verification_code)

    BasicTextField(
        // The cursor always stays at the end, cells are filled left to right
        value = TextFieldValue(value, selection = TextRange(value.length)),
        onValueChange = { onValueChange(sanitizeOtp(it.text)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { contentDescription = description },
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(OTP_LENGTH) { index ->
                    if (index == OTP_LENGTH / 2) {
                        Text(
                            text = "–",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                    OtpCell(
                        char = value.getOrNull(index),
                        isActive = enabled && isFocused && index == value.length.coerceAtMost(OTP_LENGTH - 1),
                        status = status,
                        enabled = enabled,
                        // Shrinks on narrow screens, never grows past the cell size
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    )
}

@Composable
private fun OtpCell(
    char: Char?,
    isActive: Boolean,
    status: OtpStatus,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        status == OtpStatus.ERROR -> MaterialTheme.colorScheme.error
        status == OtpStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(48.dp)
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(if (isActive) 2.dp else 1.dp, borderColor, shape)
    ) {
        Text(
            text = char?.toString() ?: "",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
        )
    }
}
