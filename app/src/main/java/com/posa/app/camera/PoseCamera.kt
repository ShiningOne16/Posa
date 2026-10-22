package com.posa.app.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.posa.app.pose.BodyPoint
import com.posa.app.pose.DetectedPoint
import com.posa.app.pose.DetectedPose
import com.posa.app.pose.Vec2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cámara (vista previa + captura) con detección de pose en tiempo real.
 *
 * Usa [MlKitAnalyzer] con COORDINATE_SYSTEM_VIEW_REFERENCED: CameraX convierte los puntos
 * de ML Kit directamente a coordenadas del PreviewView (incluyendo rotación y espejo de la
 * cámara frontal), así que se pueden dibujar tal cual encima de la vista previa.
 */
class PoseCamera(private val context: Context) {

    val controller = LifecycleCameraController(context)

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private val mainExecutor = ContextCompat.getMainExecutor(context)

    fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView, onPose: (DetectedPose?) -> Unit) {
        val ratio169 = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        controller.setPreviewResolutionSelector(ratio169)
        controller.setImageCaptureResolutionSelector(ratio169)
        controller.setImageAnalysisResolutionSelector(ratio169)
        controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
        controller.imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY

        controller.setImageAnalysisAnalyzer(
            mainExecutor,
            MlKitAnalyzer(
                listOf(detector),
                ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                mainExecutor,
            ) { result ->
                val pose: Pose? = result.getValue(detector)
                onPose(pose?.toDetectedPose())
            }
        )

        previewView.controller = controller
        controller.bindToLifecycle(lifecycleOwner)
    }

    fun setFrontCamera(front: Boolean) {
        controller.cameraSelector =
            if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }

    fun hasCamera(front: Boolean): Boolean = runCatching {
        controller.hasCamera(if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA)
    }.getOrDefault(true)

    /** Toma una foto y la guarda en Imágenes/Posa de la galería. */
    fun takePhoto(onPhotoSaved: (Uri?) -> Unit, onPhotoError: (ImageCaptureException) -> Unit) {
        val name = "Posa_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Posa")
            }
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()

        controller.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onPhotoSaved(outputFileResults.savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                onPhotoError(exception)
            }
        })
    }

    fun release() {
        controller.clearImageAnalysisAnalyzer()
        controller.unbind()
        detector.close()
    }
}

private val landmarkMap = mapOf(
    PoseLandmark.NOSE to BodyPoint.NOSE,
    PoseLandmark.LEFT_SHOULDER to BodyPoint.LEFT_SHOULDER,
    PoseLandmark.RIGHT_SHOULDER to BodyPoint.RIGHT_SHOULDER,
    PoseLandmark.LEFT_ELBOW to BodyPoint.LEFT_ELBOW,
    PoseLandmark.RIGHT_ELBOW to BodyPoint.RIGHT_ELBOW,
    PoseLandmark.LEFT_WRIST to BodyPoint.LEFT_WRIST,
    PoseLandmark.RIGHT_WRIST to BodyPoint.RIGHT_WRIST,
    PoseLandmark.LEFT_HIP to BodyPoint.LEFT_HIP,
    PoseLandmark.RIGHT_HIP to BodyPoint.RIGHT_HIP,
    PoseLandmark.LEFT_KNEE to BodyPoint.LEFT_KNEE,
    PoseLandmark.RIGHT_KNEE to BodyPoint.RIGHT_KNEE,
    PoseLandmark.LEFT_ANKLE to BodyPoint.LEFT_ANKLE,
    PoseLandmark.RIGHT_ANKLE to BodyPoint.RIGHT_ANKLE,
)

private fun Pose.toDetectedPose(): DetectedPose? {
    if (allPoseLandmarks.isEmpty()) return null
    val points = HashMap<BodyPoint, DetectedPoint>()
    for ((type, bodyPoint) in landmarkMap) {
        val lm = getPoseLandmark(type) ?: continue
        points[bodyPoint] = DetectedPoint(Vec2(lm.position.x, lm.position.y), lm.inFrameLikelihood)
    }
    return DetectedPose(points)
}
