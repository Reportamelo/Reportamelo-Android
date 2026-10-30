package app.reportamelo.communityIssues.ui.screens.tabs

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.components.WebBrowserView
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground
import java.util.UUID

data class URLItem(val id: UUID = UUID.randomUUID(), val url: Uri)
data class LegalDocument(val id: UUID = UUID.randomUUID(), val url: Uri, val documentName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptanceView(
    modifier: Modifier = Modifier,
    nextStep: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var selectedURLItem by remember { mutableStateOf<URLItem?>(null) }
    var accepted by remember { mutableStateOf(false) }
    val buttonMessage = "Next"

    val legalLinks = remember {
        listOf(
            LegalDocument(url = Uri.parse("https://reportamelo.app/legal/eula"), documentName = "EULA"),
            LegalDocument(url = Uri.parse("https://reportamelo.app/legal/terms"), documentName = "Terms"),
            LegalDocument(url = Uri.parse("https://reportamelo.app/legal/privacy"), documentName = "Privacy"),
            LegalDocument(url = Uri.parse("https://reportamelo.app/legal/policies"), documentName = "Policies")
        )
    }

    val primaryTextColor = if (isDark) Color.White else Color.Black
    val cardBackgroundColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val dividerColor = if (isDark) {
        Color(0xFF545458).copy(alpha = 0.6f)
    } else {
        Color(0xFF3C3C43).copy(alpha = 0.18f)
    }
    val chevronColor = if (isDark) {
        Color(0xFFEBEBF5).copy(alpha = 0.3f)
    } else {
        Color(0xFF3C3C43).copy(alpha = 0.3f)
    }
    val acceptanceBoxBackground = if (isDark) {
        Color.Black.copy(alpha = 0.321f)
    } else {
        Color.White.copy(alpha = 0.95f)
    }

    selectedURLItem?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedURLItem = null },
            sheetState = sheetState,
            dragHandle = null
        ) {
            WebBrowserView(
                url = item.url.toString(),
                onDismiss = { selectedURLItem = null }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .radialGradientBackground(topLeading = true, radiusFraction = 0.54f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Text(
                text = "Legal Documents",
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.37.sp,
                color = primaryTextColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp)
                    .padding(horizontal = 16.dp)
            )

            Text(
                text = "We take your privacy seriously, \nplease take time to read our documents.",
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.31).sp,
                color = primaryTextColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Inset-grouped style card for legal document links
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBackgroundColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    legalLinks.forEachIndexed { index, link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp)
                                .clickable { selectedURLItem = URLItem(url = link.url) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = link.documentName,
                                fontSize = 16.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.31).sp,
                                color = primaryTextColor
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = chevronColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (index < legalLinks.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Acceptance checkbox container anchored at the bottom above the Next button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 36.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(acceptanceBoxBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        accepted = !accepted
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AcceptanceCheckboxIcon(checked = accepted)

                Text(
                    text = "I have read and accept the documents",
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.31).sp,
                    color = primaryTextColor
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom safe area inset button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 36.dp)
                    .padding(top = 0.dp, bottom = 12.dp)
            ) {
                ThemedButton(
                    message = buttonMessage,
                    onClick = nextStep,
                    type = ThemedButtonType.SECONDARY,
                    isEnabled = accepted
                )
            }
        }
    }
}

@Composable
private fun AcceptanceCheckboxIcon(
    modifier: Modifier = Modifier,
    checked: Boolean
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val uncheckedColor = Color(0xFF8E8E93)
    val shape = RoundedCornerShape(5.dp)

    val containerColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkbox_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else uncheckedColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkbox_border"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(shape)
            .background(containerColor)
            .border(width = 1.75.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
