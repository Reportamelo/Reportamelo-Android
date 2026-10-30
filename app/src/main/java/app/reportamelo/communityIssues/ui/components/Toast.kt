package app.reportamelo.communityIssues.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*


import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ToastType {
    Error, Info, Warning, Success;
    
    val icon: ImageVector
        get() = when (this) {
            Error -> Icons.Default.Cancel
            Info -> Icons.Default.Info
            Warning -> Icons.Default.Warning
            Success -> Icons.Filled.CheckCircle
        }
        
    val tintColor: Color
        get() = when (this) {
            Error -> Color.Red
            Info -> Color.Blue
            Warning -> Color(0xFFFFA500)
            Success -> Color.Green
        }
}

@Stable
class ToastState {
    var message by mutableStateOf("")
    var type by mutableStateOf(ToastType.Info)
    var isShowing by mutableStateOf(false)

    suspend fun show(message: String, type: ToastType = ToastType.Info) {
        this.message = message
        this.type = type
        isShowing = true
        delay(3000)
        hide()
    }

    fun hide() {
        isShowing = false
    }
}

val LocalToastState = compositionLocalOf { ToastState() }

@Composable
fun ToastProvider(content: @Composable () -> Unit) {
    val toastState = remember { ToastState() }

    CompositionLocalProvider(LocalToastState provides toastState) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            
            AnimatedVisibility(
                visible = toastState.isShowing,
                enter = slideInVertically(animationSpec = spring()) { -it } + fadeIn() + scaleIn(initialScale = 0.9f),
                exit = slideOutVertically(animationSpec = spring()) { -it } + fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
                    .zIndex(100f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shadowElevation = 10.dp,
                    modifier = Modifier.clickable { toastState.hide() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = toastState.type.icon,
                            contentDescription = null,
                            tint = toastState.type.tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = toastState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
