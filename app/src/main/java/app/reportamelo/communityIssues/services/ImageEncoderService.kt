package app.reportamelo.communityIssues.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import app.reportamelo.communityIssues.controllers.ImagePhase
import app.reportamelo.communityIssues.controllers.PhotoUploadTracker
import app.reportamelo.network.ReportsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class ImageEncoderService(
    private val reportsService: ReportsService = ReportsService()
) {

    companion object {
        val shared = ImageEncoderService()

        suspend fun processAndUpload(
            tracker: PhotoUploadTracker,
            reportContainer: String
        ) = shared.processAndUpload(tracker, reportContainer)
    }

    data class OptimizedImageResult(
        val webpData: ByteArray,
        val previewBitmap: Bitmap
    )

    suspend fun optimizeImage(imageBytes: ByteArray): OptimizedImageResult? = withContext(Dispatchers.Default) {
        runCatching {
            val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext null

            val orientedBitmap = fixOrientation(originalBitmap, imageBytes)
            val width = orientedBitmap.width
            val height = orientedBitmap.height
            val maxDimension = max(width, height)

            val (targetWidth, targetHeight) = when {
                maxDimension <= 1000 -> Pair(
                    (width / 1.67f).roundToInt().coerceAtLeast(1),
                    (height / 1.67f).roundToInt().coerceAtLeast(1)
                )
                maxDimension <= 2000 -> Pair(
                    (width * 0.45f).roundToInt().coerceAtLeast(1),
                    (height * 0.45f).roundToInt().coerceAtLeast(1)
                )
                else -> Pair(
                    (width * 0.22f).roundToInt().coerceAtLeast(1),
                    (height * 0.22f).roundToInt().coerceAtLeast(1)
                )
            }

            val scaledBitmap = if (targetWidth != width || targetHeight != height) {
                Bitmap.createScaledBitmap(orientedBitmap, targetWidth, targetHeight, true)
            } else {
                orientedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }

            val encoded = scaledBitmap.compress(compressFormat, 85, outputStream)
            if (!encoded) return@withContext null

            OptimizedImageResult(
                webpData = outputStream.toByteArray(),
                previewBitmap = scaledBitmap
            )
        }.getOrNull()
    }

    private fun fixOrientation(bitmap: Bitmap, imageBytes: ByteArray): Bitmap {
        return runCatching {
            val exif = ExifInterface(ByteArrayInputStream(imageBytes))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    suspend fun processAndUpload(
        tracker: PhotoUploadTracker,
        reportContainer: String
    ) {
        val rawData = tracker.localResource.data
        if (rawData == null || rawData.isEmpty()) {
            tracker.setPhase(ImagePhase.FAILURE)
            return
        }

        tracker.setPhase(ImagePhase.OPTIMIZING)
        tracker.setUploadProgress(0.05f)

        val optimized = optimizeImage(rawData)
        if (optimized == null) {
            tracker.setPhase(ImagePhase.FAILURE)
            return
        }

        tracker.setBitmap(optimized.previewBitmap)
        tracker.setPhase(ImagePhase.UPLOADING)
        tracker.setUploadProgress(0.15f)

        val progressJob = CoroutineScope(Dispatchers.Main).launch {
            var current = 0.15f
            while (isActive && current < 0.88f) {
                delay(120)
                current = (current + 0.08f).coerceAtMost(0.88f)
                if (tracker.phase.value == ImagePhase.UPLOADING) {
                    tracker.setUploadProgress(current)
                }
            }
        }

        try {
            val response = withContext(Dispatchers.IO) {
                reportsService.uploadSinglePicture(
                    reportContainer = reportContainer,
                    imageData = optimized.webpData
                )
            }
            progressJob.cancel()
            tracker.setKey(response.data.key)
            tracker.setName(response.data.name)
            tracker.setUploadProgress(1.0f)
            tracker.setPhase(ImagePhase.SUCCESS)
        } catch (e: Exception) {
            progressJob.cancel()
            tracker.setPhase(ImagePhase.FAILURE)
        }
    }
}
