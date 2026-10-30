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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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

private val AvatarColorPalette = listOf(
    Color.fromHex("ec9458"),
    Color.fromHex("f2784b"),
    Color.fromHex("eb9532"),
    Color.fromHex("f4b350"),
    Color.fromHex("6bb9f0"),
    Color.fromHex("59abe3"),
    Color.fromHex("4183d7"),
    Color.fromHex("52b3d9"),
    Color.fromHex("a2ded0"),
    Color.fromHex("87d37c"),
    Color.fromHex("90c695"),
    Color.fromHex("68c3a3"),
    Color.fromHex("aea8d3"),
    Color.fromHex("be90d4"),
    Color.fromHex("9b59b6"),
    Color.fromHex("d2527f"),
    Color.fromHex("f62459"),
    Color.fromHex("e26a6a")
)

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
    val selectedOption by profile.selectedAvatarOptionView.collectAsState()
    val selectedColorHex by profile.selectedAvatarColor.collectAsState()

    val avatarBgColor = remember(selectedColorHex) {
        if (selectedColorHex == ProfileDataModel.DEFAULT_AVATAR_COLOR) {
            Color(0xFFEC6B00)
        } else {
            Color.fromHex(selectedColorHex)
        }
    }

    val fallbackText = remember(userName, selectedOption) {
        when (selectedOption) {
            AvatarCreatedFrom.INITIALS -> profile.getInitials()
            AvatarCreatedFrom.MONOGRAM -> profile.getMonogram()
            else -> userName.take(1).ifEmpty { "t" }
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
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (isGuest) Color(0xFFE5E5EA) else avatarBgColor),
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
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isGuest -> {
                    Image(
                        painter = painterResource(id = R.drawable.user_b),
                        contentDescription = "Guest Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                avatarURL != null &&
                    selectedOption != AvatarCreatedFrom.MONOGRAM &&
                    selectedOption != AvatarCreatedFrom.INITIALS -> {
                    SubcomposeAsyncImage(
                        model = avatarURL.toString(),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            AvatarMonogramCircle(
                                text = fallbackText,
                                backgroundColor = avatarBgColor
                            )
                        },
                        error = {
                            AvatarMonogramCircle(
                                text = fallbackText,
                                backgroundColor = avatarBgColor
                            )
                        }
                    )
                }
                else -> {
                    AvatarMonogramCircle(
                        text = fallbackText,
                        backgroundColor = avatarBgColor
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
                contentDescription = "Edit Profile Picture",
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
        var activeTab by remember {
            mutableStateOf(
                if (selectedOption == AvatarCreatedFrom.INITIALS) {
                    AvatarCreatedFrom.INITIALS
                } else {
                    AvatarCreatedFrom.MONOGRAM
                }
            )
        }
        var currentColor by remember {
            mutableStateOf(Color.fromHex(selectedColorHex))
        }

        ModalBottomSheet(
            onDismissRequest = { profile.setShowPicker(false) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Customize Avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val previewText = if (activeTab == AvatarCreatedFrom.INITIALS) {
                    profile.getInitials()
                } else {
                    profile.getMonogram()
                }

                MonogramView(
                    text = previewText,
                    backgroundColor = currentColor,
                    mode = MonogramMode.PREVIEW
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = activeTab == AvatarCreatedFrom.MONOGRAM,
                        onClick = { activeTab = AvatarCreatedFrom.MONOGRAM },
                        label = { Text("Monogram") },
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = activeTab == AvatarCreatedFrom.INITIALS,
                        onClick = { activeTab = AvatarCreatedFrom.INITIALS },
                        label = { Text("Initials") },
                        leadingIcon = {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    AssistChip(
                        onClick = { galleryLauncher.launch("image/*") },
                        label = { Text("Library") },
                        leadingIcon = {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    AssistChip(
                        onClick = { cameraLauncher.launch(null) },
                        label = { Text("Camera") },
                        leadingIcon = {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                ColorGrid(
                    selectedColor = currentColor,
                    colors = AvatarColorPalette,
                    onColorSelected = { color ->
                        currentColor = color
                        profile.setSelectedAvatarColor(color.toHex())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                )

                ThemedButton(
                    message = "Save",
                    type = ThemedButtonType.PRIMARY,
                    style = ThemedButtonStyle.NORMAL,
                    isLoading = isUploading,
                    onClick = {
                        profile.setSelectedAvatarOptionView(activeTab)
                        profile.setSelectedAvatarColor(currentColor.toHex())
                        val generatedBitmap = profile.createAvatarBitmap(
                            text = previewText,
                            backgroundColorInt = currentColor.toArgb()
                        )
                        profile.uploadProfilePicture(generatedBitmap, activeTab)
                        profile.setShowPicker(false)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
                contentDescription = "Profile Image",
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
                contentDescription = "Edit Profile",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
