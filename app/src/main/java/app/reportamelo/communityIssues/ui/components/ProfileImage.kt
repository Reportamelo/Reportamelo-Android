package app.reportamelo.communityIssues.ui.components

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.commons.AvatarCreatedFrom
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.datamodel.ProfileDataModel
import app.reportamelo.communityIssues.helpers.fromHex
import app.reportamelo.communityIssues.helpers.toHex
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.res.stringResource

@Composable
fun CustomBlurryOverlay(
    modifier: Modifier = Modifier,
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (show) {
        val isDark = isSystemInDarkTheme()
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    if (isDark) Color.Black.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.08f)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDismiss()
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileImage(
    modifier: Modifier = Modifier,
    profile: ProfileDataModel,
    isPro: Boolean = false
) {
    val context = LocalContext.current
    val profileImage by profile.profileImage.collectAsState()
    val userName by profile.userName.collectAsState()
    val isUploading by profile.isUploading.collectAsState()
    val showPicker by profile.showPicker.collectAsState()
    val isGuest by profile.isGuest.collectAsState()
    val avatarURL by profile.avatarURL.collectAsState()
    val googleAvatarURL by profile.googleAvatarURL.collectAsState()
    val selectedOption by profile.selectedAvatarOptionView.collectAsState()
    val selectedColorHex by profile.selectedAvatarColor.collectAsState()

    LaunchedEffect(Unit) {
        profile.setDefaultAvatar()
    }

    val monogramColor = remember(selectedColorHex) {
        Color.fromHex(selectedColorHex)
    }

    val googleFallbackColor = Color(0xFFEC6B00)

    val mainAvatarBgColor = remember(selectedOption, monogramColor) {
        when (selectedOption) {
            AvatarCreatedFrom.MONOGRAM, AvatarCreatedFrom.INITIALS -> monogramColor
            else -> googleFallbackColor
        }
    }

    val fallbackText = remember(userName, selectedOption) {
        when (selectedOption) {
            AvatarCreatedFrom.INITIALS -> profile.getInitials().ifEmpty { "TE" }
            AvatarCreatedFrom.MONOGRAM -> profile.getMonogram().ifEmpty { "T" }
            else -> userName.trim().take(1).lowercase().ifEmpty { "t" }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bitmap != null) {
                    profile.setSelectedAvatarOptionView(AvatarCreatedFrom.PHOTO)
                    profile.uploadProfilePicture(bitmap, AvatarCreatedFrom.PHOTO)
                    profile.setShowPicker(false)
                }
            } catch (_: Exception) {
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            profile.setSelectedAvatarOptionView(AvatarCreatedFrom.CAMERA)
            profile.uploadProfilePicture(bitmap, AvatarCreatedFrom.CAMERA)
            profile.setShowPicker(false)
        }
    }

    Box(
        modifier = modifier.size(130.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (isGuest) Color(0xFFE5E5EA) else mainAvatarBgColor)
                .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.7f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                isUploading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                profileImage != null -> {
                    Image(
                        bitmap = profileImage!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.profile_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isGuest -> {
                    Image(
                        painter = painterResource(id = R.drawable.user_b),
                        contentDescription = stringResource(R.string.guest_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                avatarURL != null &&
                    selectedOption != AvatarCreatedFrom.MONOGRAM &&
                    selectedOption != AvatarCreatedFrom.INITIALS -> {
                    SubcomposeAsyncImage(
                        model = avatarURL.toString(),
                        contentDescription = stringResource(R.string.profile_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            AvatarMonogramCircle(
                                text = fallbackText,
                                backgroundColor = mainAvatarBgColor
                            )
                        },
                        error = {
                            AvatarMonogramCircle(
                                text = fallbackText,
                                backgroundColor = mainAvatarBgColor
                            )
                        }
                    )
                }
                else -> {
                    AvatarMonogramCircle(
                        text = fallbackText,
                        backgroundColor = mainAvatarBgColor
                    )
                }
            }
        }

        // Edit pencil button at BottomEnd (matching iOS overlay(alignment: .bottomTrailing))
        val editBgColor = if (isGuest) Color(0xFF48484A) else Color(0xFFF57C00)
        val editIconTint = if (isGuest) Color.White.copy(alpha = 0.55f) else Color.White
        val editBorderColor = if (isGuest) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.85f)

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = (-2).dp)
                .size(34.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(editBgColor)
                .border(width = 1.dp, color = editBorderColor, shape = CircleShape)
                .clickable(enabled = !isGuest) {
                    profile.setShowPicker(true)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.edit_profile_picture),
                tint = editIconTint,
                modifier = Modifier.size(16.dp)
            )
        }

        if (isPro) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "Pro",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (showPicker && !isGuest) {
        var currentView by remember {
            mutableStateOf(AvatarCreatedFrom.OPTIONS_SELECTOR)
        }
        var currentColor by remember(selectedColorHex) {
            mutableStateOf(Color.fromHex(selectedColorHex))
        }
        val isDark = isSystemInDarkTheme()

        ModalBottomSheet(
            onDismissRequest = { profile.setShowPicker(false) },
            scrimColor = Color.Transparent,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9FB)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentView) {
                    AvatarCreatedFrom.OPTIONS_SELECTOR -> {
                        AvatarSheetHeaderView(
                            title = stringResource(R.string.create_your_avatar_from),
                            onClose = { profile.setShowPicker(false) }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        val googleInitial = userName.trim().take(1).lowercase().ifEmpty { "t" }
                        val monogramText = profile.getMonogram().ifEmpty { "T" }
                        val initialsText = profile.getInitials().ifEmpty { "TE" }

                        // Row 1: Google Auth, Monogram, Initials
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top
                        ) {
                            AvatarOptionItem(
                                label = "Google Auth",
                                isSelected = selectedOption == AvatarCreatedFrom.GOOGLE_AUTH,
                                onClick = {
                                    profile.applyGoogleAvatar {
                                        profile.setShowPicker(false)
                                    }
                                }
                            ) {
                                val targetUrl = googleAvatarURL ?: avatarURL
                                if (targetUrl != null) {
                                    SubcomposeAsyncImage(
                                        model = targetUrl.toString(),
                                        contentDescription = stringResource(R.string.google_auth_avatar),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = {
                                            AvatarPreviewCircle(
                                                text = googleInitial,
                                                backgroundColor = googleFallbackColor,
                                                fontWeight = FontWeight.Normal
                                            )
                                        },
                                        error = {
                                            AvatarPreviewCircle(
                                                text = googleInitial,
                                                backgroundColor = googleFallbackColor,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    )
                                } else {
                                    AvatarPreviewCircle(
                                        text = googleInitial,
                                        backgroundColor = googleFallbackColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            AvatarOptionItem(
                                label = stringResource(R.string.monogram),
                                isSelected = selectedOption == AvatarCreatedFrom.MONOGRAM,
                                onClick = {
                                    currentView = AvatarCreatedFrom.MONOGRAM
                                }
                            ) {
                                AvatarPreviewCircle(
                                    text = monogramText,
                                    backgroundColor = currentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AvatarOptionItem(
                                label = stringResource(R.string.initials),
                                isSelected = selectedOption == AvatarCreatedFrom.INITIALS,
                                onClick = {
                                    currentView = AvatarCreatedFrom.INITIALS
                                }
                            ) {
                                AvatarPreviewCircle(
                                    text = initialsText,
                                    backgroundColor = currentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Row 2: Photo, Camera, empty slot (3-column alignment matching iOS LazyVGrid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top
                        ) {
                            AvatarOptionItem(
                                label = stringResource(R.string.photo),
                                isSelected = selectedOption == AvatarCreatedFrom.PHOTO,
                                onClick = {
                                    galleryLauncher.launch("image/*")
                                }
                            ) {
                                AvatarIconPreviewCircle(
                                    icon = Icons.Outlined.PhotoLibrary,
                                    contentDescription = stringResource(R.string.photo),
                                    backgroundColor = googleFallbackColor
                                )
                            }

                            AvatarOptionItem(
                                label = stringResource(R.string.camera),
                                isSelected = selectedOption == AvatarCreatedFrom.CAMERA,
                                onClick = {
                                    cameraLauncher.launch(null)
                                }
                            ) {
                                AvatarIconPreviewCircle(
                                    icon = Icons.Outlined.CameraAlt,
                                    contentDescription = stringResource(R.string.camera),
                                    backgroundColor = googleFallbackColor
                                )
                            }

                            Spacer(modifier = Modifier.width(88.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    AvatarCreatedFrom.MONOGRAM, AvatarCreatedFrom.INITIALS -> {
                        val activeOption = currentView
                        val previewText = if (activeOption == AvatarCreatedFrom.INITIALS) {
                            profile.getInitials().ifEmpty { "TE" }
                        } else {
                            profile.getMonogram().ifEmpty { "T" }
                        }

                        AvatarSheetHeaderView(
                            title = stringResource(R.string.edit_your_monogram),
                            onClose = {
                                currentView = AvatarCreatedFrom.OPTIONS_SELECTOR
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        MonogramView(
                            text = previewText,
                            backgroundColor = currentColor,
                            mode = MonogramMode.PREVIEW
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        ColorGrid(
                            selectedColor = currentColor,
                            colors = profile.backgroundColors,
                            onColorSelected = { color ->
                                currentColor = color
                                profile.setSelectedAvatarColor(color.toHex())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        ThemedButton(
                            message = stringResource(R.string.save),
                            type = ThemedButtonType.OUTLINE,
                            style = ThemedButtonStyle.NORMAL,
                            isLoading = isUploading,
                            onClick = {
                                profile.setSelectedAvatarOptionView(activeOption)
                                profile.setSelectedAvatarColor(currentColor.toHex())
                                val generatedBitmap = profile.createAvatarBitmap(
                                    text = previewText,
                                    backgroundColorInt = currentColor.toArgb()
                                )
                                profile.uploadProfilePicture(generatedBitmap, activeOption)
                                profile.setShowPicker(false)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun AvatarSheetHeaderView(
    modifier: Modifier = Modifier,
    title: String,
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = if (isDark) Color.White else Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AvatarOptionItem(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier
            .width(88.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(86.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer white ring with soft shadow (matching Screenshot 3)
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.08f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                    content = content
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(24.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF0088FF))
                        .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.selected_text),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AvatarPreviewCircle(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun AvatarIconPreviewCircle(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun AvatarMonogramCircle(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun ProfileImage(
    modifier: Modifier = Modifier,
    url: String?,
    isUploading: Boolean = false,
    isPro: Boolean = false,
    onEditClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (!url.isNullOrEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.profile_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
        }

        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.background, CircleShape)
                .size(36.dp)
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.edit_profile),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
