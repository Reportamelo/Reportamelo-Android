package app.reportamelo.communityIssues.helpers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomToolBar(
    isPrimaryActionVisible: Boolean,
    navBackButtonHidden: Boolean = true,
    title: String?,
    subtitle: String?,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    primaryAction: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Petitions",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            if (!navBackButtonHidden) {
                leading()
            }
        },
        actions = {
            trailing()
            
            AnimatedVisibility(
                visible = isPrimaryActionVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                primaryAction()
            }
        }
    )
}
