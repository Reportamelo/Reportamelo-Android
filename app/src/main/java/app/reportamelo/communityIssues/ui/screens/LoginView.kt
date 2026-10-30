package app.reportamelo.communityIssues.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.commons.LoginType
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.LoginViewModel
import app.reportamelo.communityIssues.ui.components.LinksView
import app.reportamelo.models.AuthPayload

@Composable
fun LoginView(
    modifier: Modifier = Modifier,
    onTokenReceived: (AuthPayload, LoginType) -> Unit = { _, _ -> },
    viewModel: LoginViewModel = LoginViewModel.shared
) {
    val context = LocalContext.current
    val disableLoginButtons by viewModel.disableLoginButtons.collectAsState()
    val lastAuthMethod by viewModel.lastAuthMethod.collectAsState()
    val showErrorAlert by viewModel.showErrorAlert.collectAsState()
    val messageError by viewModel.messageError.collectAsState()

    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(messageError) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            shape = RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp, bottomStart = 52.dp, bottomEnd = 52.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Repórtamelo",
                        fontWeight = FontWeight.Medium,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reporta tus problemas y mejora tu comunidad",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.loginWithApple(onTokenReceived) },
                        enabled = !disableLoginButtons,
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Black),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Iniciar sesión con Apple", color = androidx.compose.ui.graphics.Color.White)
                    }
                    if (lastAuthMethod == "apple") {
                        Text("Last login", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    OutlinedButton(
                        onClick = { viewModel.loginWithGoogle(context, onTokenReceived) },
                        enabled = !disableLoginButtons,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Inicia sesión con Google")
                    }
                    if (lastAuthMethod == "google") {
                        Text("Last login", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    OutlinedButton(
                        onClick = { viewModel.loginAsGuest(onTokenReceived) },
                        enabled = !disableLoginButtons,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Iniciar sesión como Invitado")
                    }
                }

                LinksView()
            }
        }
    }
}
