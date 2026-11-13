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
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

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
        LinkType.Full -> stringResource(R.string.links_full)
        LinkType.TermsAndConditions -> stringResource(R.string.links_terms_and_conditions)
        LinkType.Privacy -> stringResource(R.string.links_privacy)
        LinkType.Policies -> stringResource(R.string.links_policies)
        LinkType.EndUserLicenseAgreement -> stringResource(R.string.links_eula)
    }
    
    val links = mapOf(
        stringResource(R.string.link_terms_of_service) to "https://reportamelo.app/legal/terms",
        stringResource(R.string.link_privacy_policy) to "https://reportamelo.app/legal/privacy",
        stringResource(R.string.link_eula) to "https://reportamelo.app/legal/eula",
        stringResource(R.string.link_end_user_license_agreement) to "https://reportamelo.app/legal/eula",
        stringResource(R.string.link_policies) to "https://reportamelo.app/legal/policies"
    )

    val annotatedString = buildAnnotatedString {
        append(text)
        
        val linkStyle = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline
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
