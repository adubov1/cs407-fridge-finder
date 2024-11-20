package com.cs407.fridgefinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Locale

class ScanCameraActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private var imageCapture: ImageCapture? = null
    private var photosLeft = 10
    private val photosList = mutableListOf<FridgePhoto>()

    private val reviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> { finish() }
            RESULT_CANCELED -> {
                val remainingPhotos = result.data?.getStringArrayListExtra("remaining_photos")
                if (remainingPhotos != null) {
                    photosList.clear()
                    photosList.addAll(remainingPhotos.map { FridgePhoto(it) })
                    photosLeft = 10 - photosList.size
                    updatePhotosLeftCounter()
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this,
                "You must allow Fridge Finder to use your camera",
                Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan_camera)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        outputDirectory = File(cacheDir, "camera").apply {
            mkdirs()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        findViewById<ImageButton>(R.id.cameraButton).setOnClickListener {
            takePhoto()
        }

        findViewById<ImageButton>(R.id.doneButton).setOnClickListener {
            if (photosList.isEmpty()) {
                Toast.makeText(this, "Please take at least one photo", Toast.LENGTH_SHORT).show()
            } else {
                navigateToReview()
            }
        }

        updatePhotosLeftCounter()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun updatePhotosLeftCounter() {
        findViewById<TextView>(R.id.numPhotosLeft).text =
            getString(R.string.num_photos_left, photosLeft, 10)
    }

    private fun takePhoto() {
        if ((photosLeft - 1) <= 0) {
            Toast.makeText(this, "Maximum photos reached", Toast.LENGTH_SHORT).show()
            return
        }

        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    photosList.add(FridgePhoto(photoFile.absolutePath))
                    photosLeft--
                    updatePhotosLeftCounter()
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@ScanCameraActivity,
                        "Photo capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

                preview.surfaceProvider = findViewById<PreviewView>(R.id.cameraPreview).surfaceProvider

            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed",
                    Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun navigateToReview() {
        val intent = Intent(this, PhotoReviewActivity::class.java)
        intent.putStringArrayListExtra("photos",
            ArrayList(photosList.map { it.uri }))
        reviewLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        if (isFinishing) {
            outputDirectory.listFiles()?.forEach { it.delete() }
            outputDirectory.delete()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}