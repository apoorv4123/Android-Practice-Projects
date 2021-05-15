package com.example.camerax

import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.Executor

//const val PERMISSION_REQUEST_CAMERA = 123

class MainActivity : AppCompatActivity(), Executor {

    override fun execute(command: Runnable?) {
        command?.run()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Start your work for camera
            textureView.post { 
                startCamera()
            }
        } else {
            // Ask for permission
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.CAMERA),
                1234
            )
        }

    }

    private fun startCamera() {

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setTargetAspectRatio(AspectRatio.RATIO_16_9)// optional
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)

        btnSave.setOnClickListener {
            val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(file, this, object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    Log.i("IMAGECAPTURE", "Image Captured ${file.absolutePath}")
                }

                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    Log.i("IMAGECAPTURE", "Error Capturing $message")
                }
            })
        }

        var previewConfig = PreviewConfig.Builder().apply {
//            setTargetResolution(Size(1080, 1080))java.lang.IllegalArgumentException: Cannot use both setTargetResolution and setTargetAspectRatio on the same config.
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setLensFacing(CameraX.LensFacing.BACK)// To set default lens
        }.build()

        val preview = Preview(previewConfig)

        btnSwap.setOnClickListener {
            Log.i("LENSSWAPPED", "Lens Swapped")
            if (previewConfig.lensFacing == CameraX.LensFacing.BACK) {
                previewConfig = PreviewConfig.Builder().apply {
                    setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    setLensFacing(CameraX.LensFacing.FRONT)
                }.build()
                preview = Preview(previewConfig)
            } else {
                previewConfig = PreviewConfig.Builder().apply {
                    setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    setLensFacing(CameraX.LensFacing.BACK)
                }.build()
                preview = Preview(previewConfig)
            }
        }
        preview.setOnPreviewOutputUpdateListener {
            // textureView.parent gives us the whole screen
            val parent = textureView.parent as ViewGroup// get current root of the textureView
            parent.removeView(textureView)
            parent.addView(textureView, 0)
            updateTransform()// make this function for handling rotation & other transformations if there
//            textureView.surfaceTexture = it.surfaceTexture

            textureView.setSurfaceTexture(it.surfaceTexture)
            // The preview we're getting, we need to update that preview's surface texture to our own textureView
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Get the x & Y coordinates of center
        val centerX = textureView.width / 2f
        val centerY = textureView.height / 2f

        val rotationDegrees = when (textureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        textureView.setTransform(matrix)
    }

}
