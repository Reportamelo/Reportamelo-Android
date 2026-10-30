package app.reportamelo.communityIssues.datamodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reportamelo.commons.AppPreferences
import app.reportamelo.commons.AvatarCreatedFrom
import app.reportamelo.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AvatarSource {
    CAMERA, LIBRARY, EMOJI, COLLECTIBLE, MONOGRAM, INITIALS
}

class ProfileDataModel(
    private val userRepository: UserRepository = UserRepository.shared
) : ViewModel() {

    companion object {
        const val KEY_SELECTED_AVATAR_OPTION = "selected_avatar_option"
        const val KEY_SELECTED_AVATAR_COLOR = "selected_avatar_color"
        const val KEY_AVATAR_URL = "avatar_url"
        const val DEFAULT_AVATAR_COLOR = "ec9458"
    }

    private val _profileImage = MutableStateFlow<Bitmap?>(null)
    val profileImage: StateFlow<Bitmap?> = _profileImage.asStateFlow()

    private val _userName = MutableStateFlow(userRepository.getName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _showPicker = MutableStateFlow(false)
    val showPicker: StateFlow<Boolean> = _showPicker.asStateFlow()

    private val _isGuest = MutableStateFlow(userRepository.isGuestUser())
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _avatarURL = MutableStateFlow<Uri?>(userRepository.getAvatar())
    val avatarURL: StateFlow<Uri?> = _avatarURL.asStateFlow()

    private val _selectedAvatarOptionView = MutableStateFlow(loadSavedAvatarOption())
    val selectedAvatarOptionView: StateFlow<AvatarCreatedFrom> = _selectedAvatarOptionView.asStateFlow()

    private val _selectedAvatarColor = MutableStateFlow(
        AppPreferences.getString(KEY_SELECTED_AVATAR_COLOR) ?: DEFAULT_AVATAR_COLOR
    )
    val selectedAvatarColor: StateFlow<String> = _selectedAvatarColor.asStateFlow()

    init {
        refreshFromRepository()
    }

    private fun loadSavedAvatarOption(): AvatarCreatedFrom {
        val raw = AppPreferences.getString(KEY_SELECTED_AVATAR_OPTION) ?: return AvatarCreatedFrom.GOOGLE_AUTH
        return AvatarCreatedFrom.entries.firstOrNull {
            it.rawValue.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
        } ?: AvatarCreatedFrom.GOOGLE_AUTH
    }

    fun refreshFromRepository() {
        _isGuest.value = userRepository.isGuestUser()
        _userName.value = userRepository.getName()
        _avatarURL.value = userRepository.getAvatar()
        _selectedAvatarOptionView.value = loadSavedAvatarOption()
        _selectedAvatarColor.value = AppPreferences.getString(KEY_SELECTED_AVATAR_COLOR) ?: DEFAULT_AVATAR_COLOR
        if (_isGuest.value) {
            _profileImage.value = null
        }
    }

    fun setSelectedAvatarOptionView(option: AvatarCreatedFrom) {
        _selectedAvatarOptionView.value = option
        AppPreferences.setString(KEY_SELECTED_AVATAR_OPTION, option.rawValue)
    }

    fun setSelectedAvatarColor(colorHex: String) {
        _selectedAvatarColor.value = colorHex
        AppPreferences.setString(KEY_SELECTED_AVATAR_COLOR, colorHex)
    }

    fun setShowPicker(show: Boolean) {
        _showPicker.value = show
    }

    fun setIsGuest(guest: Boolean) {
        _isGuest.value = guest
    }

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setProfileImage(bitmap: Bitmap?) {
        _profileImage.value = bitmap
    }

    fun setDefaultAvatar() {
        if (_selectedAvatarOptionView.value == AvatarCreatedFrom.GOOGLE_AUTH) {
            val url = userRepository.getProfilePictureURL()
            if (url != null) {
                AppPreferences.setString(KEY_AVATAR_URL, url.toString())
                _avatarURL.value = url
            }
        }
    }

    fun applyGoogleAvatar(completion: () -> Unit) {
        val url = userRepository.getProfilePictureURL() ?: return
        _avatarURL.value = url
        completion()
    }

    fun uploadProfilePicture(image: Bitmap, from: AvatarCreatedFrom) {
        _isUploading.value = true
        viewModelScope.launch {
            try {
                val urlString = userRepository.changeAvatar(image, from)
                _avatarURL.value = userRepository.getAvatar() ?: Uri.parse(urlString)
                _profileImage.value = image
                userRepository.refresh()
            } catch (e: Exception) {
                _profileImage.value = image
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun createAvatarBitmap(text: String, backgroundColorInt: Int, sizePx: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColorInt
            style = Paint.Style.FILL
        }
        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = sizePx * 0.42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val yPos = radius - textBounds.exactCenterY()
        canvas.drawText(text, radius, yPos, textPaint)
        return bitmap
    }

    fun getMonogram(): String {
        val currentName = _userName.value.ifBlank { userRepository.getName() }
        return currentName.take(1).uppercase()
    }

    fun getInitials(): String {
        val currentName = _userName.value.ifBlank { userRepository.getName() }.trim()
        val components = currentName.split(" ").filter { it.isNotEmpty() }
        return if (components.size >= 2) {
            val first = components[0].take(1)
            val second = components[1].take(1)
            "$first$second".uppercase()
        } else {
            currentName.take(2).uppercase()
        }
    }
}
