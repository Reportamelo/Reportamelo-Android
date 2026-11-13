package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.commons.userNameValidator
import app.reportamelo.commons.utils.userAlias
import app.reportamelo.communityIssues.controllers.LandingViewModel
import app.reportamelo.communityIssues.datamodel.ProfileDataModel
import app.reportamelo.communityIssues.datamodel.UserNameAvailabilityStatus
import app.reportamelo.communityIssues.datamodel.UserNameState
import app.reportamelo.communityIssues.datamodel.UserPersonalizationDataModel
import app.reportamelo.communityIssues.ui.components.CustomBlurryOverlay
import app.reportamelo.communityIssues.ui.components.ProfileImage
import app.reportamelo.communityIssues.ui.components.TextInput
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonStyle
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPersonalizationView(
    modifier: Modifier = Modifier,
    userPersonalizationDataModel: UserPersonalizationDataModel = LandingViewModel.shared.userPersonalizationDataModel.collectAsState().value,
    profile: ProfileDataModel = LandingViewModel.shared.profile.collectAsState().value,
    onBack: (() -> Unit)? = null,
    onNextStep: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    val userName by userPersonalizationDataModel.userName.collectAsState()
    val name by userPersonalizationDataModel.name.collectAsState()
    val availability by userPersonalizationDataModel.userNameAvailability.collectAsState()
    val usernameState by userPersonalizationDataModel.usernameState.collectAsState()
    val userNameErrorMessage by userPersonalizationDataModel.userNameErrorMessage.collectAsState()
    val isLoading by userPersonalizationDataModel.isLoading.collectAsState()
    val isFormValid by userPersonalizationDataModel.isFormValidFlow.collectAsState()
    val showErrorAlert by userPersonalizationDataModel.showErrorAlert.collectAsState()
    val messageError by userPersonalizationDataModel.messageError.collectAsState()

    val showPicker by profile.showPicker.collectAsState()

    LaunchedEffect(Unit) {
        userPersonalizationDataModel.loadUserInfo()
        profile.refreshFromRepository()
    }

    LaunchedEffect(name, userName) {
        val displayForAvatar = name.ifBlank { userName }
        if (displayForAvatar.isNotBlank()) {
            profile.setUserName(displayForAvatar)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .radialGradientBackground(topLeading = true, radiusFraction = 0.54f),
        containerColor = Color.Transparent,
        topBar = {
            UserPersonalizationTopBar(
                title = stringResource(R.string.personalize_your_profile),
                onBack = onBack
            )
        },
        bottomBar = {
            if (!showPicker) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 36.dp, vertical = 20.dp)
                ) {
                    ThemedButton(
                        message = stringResource(R.string.next_step),
                        type = ThemedButtonType.SECONDARY,
                        style = ThemedButtonStyle.NORMAL,
                        isLoading = isLoading,
                        isEnabled = isFormValid,
                        onClick = {
                            focusManager.clearFocus()
                            userPersonalizationDataModel.updateUsername(onSuccess = onNextStep)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (showPicker) 30.dp else 0.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileImage(profile = profile)

                    Spacer(modifier = Modifier.height(16.dp))

                    val displayName = name.ifBlank { userName }
                    if (displayName.isNotEmpty()) {
                        Text(
                            text = displayName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = userAlias(userName),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    TextInput(
                        value = userName,
                        onValueChange = { newValue ->
                            userPersonalizationDataModel.onUserNameChanged(newValue)
                        },
                        label = stringResource(R.string.user_name),
                        placeholder = stringResource(R.string.choose_a_username),
                        validators = userNameValidator,
                        onValidChange = { valid ->
                            userPersonalizationDataModel.setIsUserNameValid(valid)
                        },
                        isDisabled = usernameState == UserNameState.UPDATED
                    )

                    UserNameAvailabilityIndicator(
                        availability = availability,
                        errorMessage = userNameErrorMessage,
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            CustomBlurryOverlay(
                show = showPicker,
                onDismiss = {
                    profile.setShowPicker(false)
                    focusManager.clearFocus()
                }
            )
        }
    }

    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { userPersonalizationDataModel.dismissError() },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(messageError) },
            confirmButton = {
                TextButton(onClick = { userPersonalizationDataModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun UserPersonalizationTopBar(
    modifier: Modifier = Modifier,
    title: String,
    onBack: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF2C2C2E)
                        else Color.White.copy(alpha = 0.85f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun UserNameAvailabilityIndicator(
    modifier: Modifier = Modifier,
    availability: UserNameAvailabilityStatus,
    errorMessage: String
) {
    when (availability) {
        UserNameAvailabilityStatus.AVAILABLE -> {
            Text(
                text = stringResource(R.string.available),
                fontSize = 13.sp,
                color = Color(0xFF34C759),
                modifier = modifier
            )
        }
        UserNameAvailabilityStatus.LOADING -> {
            CircularProgressIndicator(
                modifier = modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        UserNameAvailabilityStatus.ERROR -> {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = modifier
                )
            }
        }
        UserNameAvailabilityStatus.UNTOUCHED -> Unit
    }
}
