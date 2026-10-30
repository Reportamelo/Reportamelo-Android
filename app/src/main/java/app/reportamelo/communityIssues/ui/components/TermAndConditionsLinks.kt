package app.reportamelo.communityIssues.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

enum class LinkType {
    TermsAndConditions, Full, Privacy, Policies, EndUserLicenseAgreement
}

private val SecondaryLinkTextColor = Color(0xFF8E8E93)

@Composable
fun LinksView(
    modifier: Modifier = Modifier,
    type: LinkType = LinkType.TermsAndConditions
) {
    val context = LocalContext.current
    
    val text = when (type) {
        LinkType.Full -> "You can review our Terms of Service, Privacy Policy, EULA and Policies."
        LinkType.TermsAndConditions -> "By continuing, you agree to our Terms of Service and Privacy Policy."
        LinkType.Privacy -> "If you have doubts about how we use your data, please review our Privacy Policy."
        LinkType.Policies -> "If you have doubts, please review our Policies."
        LinkType.EndUserLicenseAgreement -> "If you have doubts, please review our End User License Agreement."
    }
    
    val annotatedString = buildAnnotatedString {
        append(text)
        
        val linkStyle = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline
        )
        
        val links = mapOf(
            "Terms of Service" to "https://reportamelo.app/legal/terms",
            "Privacy Policy" to "https://reportamelo.app/legal/privacy",
            "EULA" to "https://reportamelo.app/legal/eula",
            "End User License Agreement" to "https://reportamelo.app/legal/eula",
            "Policies" to "https://reportamelo.app/legal/policies"
        )
        
        links.forEach { (phrase, url) ->
            val startIndex = text.indexOf(phrase)
            if (startIndex >= 0) {
                val endIndex = startIndex + phrase.length
                addStyle(linkStyle, startIndex, endIndex)
                addStringAnnotation(tag = "URL", annotation = url, start = startIndex, end = endIndex)
            }
        }
    }
    
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = (-0.1).sp,
            color = SecondaryLinkTextColor
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                    context.startActivity(intent)
                }
        }
    )
}
