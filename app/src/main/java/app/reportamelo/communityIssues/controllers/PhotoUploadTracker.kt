package app.reportamelo.communityIssues.controllers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class MediaResourceType { PHOTO }

data class BasicMetadata(val deviceOrientation: String = "portrait")

data class MediaResources(
    val type: MediaResourceType = MediaResourceType.PHOTO,
    val data: ByteArray?,
    val metadata: BasicMetadata = BasicMetadata()
)

enum class ImagePhase(val description: String) {
    OPTIMIZING("Optimizing"),
    UPLOADING("Uploading"),
    SUCCESS("Uploaded"),
    FAILURE("Failed"),
    DELETING("Deleting")
}

class PhotoUploadTracker {
    val id: String = UUID.randomUUID().toString()

    private val _key = MutableStateFlow("")
    val key: StateFlow<String> = _key.asStateFlow()

    val localResource: MediaResources

    private val _url = MutableStateFlow<Uri?>(null)
    val url: StateFlow<Uri?> = _url.asStateFlow()

    private val _phase = MutableStateFlow(ImagePhase.OPTIMIZING)
    val phase: StateFlow<ImagePhase> = _phase.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0.0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _isExisting = MutableStateFlow(false)
    val isExisting: StateFlow<Boolean> = _isExisting.asStateFlow()

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val _aspectRatio = MutableStateFlow(0.75f)
    val aspectRatio: StateFlow<Float> = _aspectRatio.asStateFlow()

    constructor(
        localResource: MediaResources,
        isExistingParam: Boolean = false,
        initialBitmap: Bitmap? = null
    ) {
        this.localResource = localResource
        _isExisting.value = isExistingParam
        val resolvedBitmap = initialBitmap ?: localResource.data?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
        setBitmap(resolvedBitmap)
    }

    constructor(
        nameParam: String = "",
        rawBytes: ByteArray? = null,
        bitmapParam: Bitmap? = null,
        aspectRatioParam: Float = 0.75f
    ) : this(
        localResource = MediaResources(data = rawBytes),
        isExistingParam = false,
        initialBitmap = bitmapParam
    ) {
        _name.value = nameParam
        if (aspectRatioParam > 0f) {
            _aspectRatio.value = aspectRatioParam.coerceIn(0.45f, 1.8f)
        }
    }

    constructor(
        keyParam: String,
        nameParam: String,
        urlParam: String?,
        isExistingParam: Boolean = true
    ) {
        _key.value = keyParam
        _name.value = nameParam
        _url.value = urlParam?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        _phase.value = ImagePhase.SUCCESS
        _uploadProgress.value = 1.0f
        _isExisting.value = isExistingParam
        this.localResource = MediaResources(
            type = MediaResourceType.PHOTO,
            data = null,
            metadata = BasicMetadata(deviceOrientation = "portrait")
        )
    }

    fun setKey(value: String) {
        _key.value = value
    }

    fun setUrl(value: Uri?) {
        _url.value = value
    }

    fun setPhase(value: ImagePhase) {
        _phase.value = value
    }

    fun setUploadProgress(value: Float) {
        _uploadProgress.value = value.coerceIn(0f, 1f)
    }

    fun setName(value: String) {
        _name.value = value
    }

    fun setBitmap(value: Bitmap?) {
        _bitmap.value = value
        if (value != null && value.width > 0 && value.height > 0) {
            _aspectRatio.value = (value.width.toFloat() / value.height.toFloat()).coerceIn(0.45f, 1.8f)
        }
    }

    fun setAspectRatio(value: Float) {
        if (value > 0f) {
            _aspectRatio.value = value.coerceIn(0.45f, 1.8f)
        }
    }
}
