package com.example.cameratest
// Android imports
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle

// Activity and permissions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// CameraX imports
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView

// Compose layout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Material 3
import androidx.compose.material3.Button
import androidx.compose.material3.Text

// Compose state
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Compose UI
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            // 1. Check camera permission
            var allowed by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            // 2. Store captured photograph and messages
            var photo by remember {
                mutableStateOf<Bitmap?>(null)
            }

            var message by remember {
                mutableStateOf("")
            }

            // 3. Create CameraX controller
            val camera = remember {
                LifecycleCameraController(this@MainActivity)
            }

            // 4. Request camera permission
            val permission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->

                allowed = granted

                if (!granted) {
                    message = "Camera permission denied"
                }
            }

            // 5. Connect CameraX to Activity lifecycle
            LaunchedEffect(allowed) {

                if (allowed) {
                    try {
                        camera.bindToLifecycle(this@MainActivity)
                    } catch (e: Exception) {
                        message = "Camera unavailable"
                    }
                }
            }

            // 6. Main screen layout
            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 80.dp)
                ) {

                    // 7. Show permission button if needed
                    if (!allowed) {

                        Button(
                            onClick = {
                                permission.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        ) {
                            Text("Allow Camera")
                        }

                    } else {

                        // 8. Display live CameraX preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clipToBounds()
                        ) {

                            AndroidView(
                                factory = { context ->

                                    PreviewView(context).apply {
                                        implementationMode =
                                            PreviewView.ImplementationMode.COMPATIBLE

                                        controller = camera
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 9. Display captured photograph
                        photo?.let { bitmap ->

                            Spacer(
                                modifier = Modifier.height(24.dp)
                            )

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }
                    }

                    // Display status messages
                    if (message.isNotEmpty()) {
                        Text(message)
                    }
                }

                // 10. Fixed button at bottom centre
                if (allowed) {

                    Button(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),

                        onClick = {

                            message = ""

                            // 11. Capture photograph
                            camera.takePicture(
                                ContextCompat.getMainExecutor(
                                    this@MainActivity
                                ),

                                object : ImageCapture.OnImageCapturedCallback() {

                                    override fun onCaptureSuccess(
                                        image: ImageProxy
                                    ) {
                                        try {

                                            // 12. Convert image to Bitmap
                                            val bitmap = image.toBitmap()

                                            // Correct image rotation
                                            val rotation =
                                                image.imageInfo.rotationDegrees

                                            val matrix = Matrix().apply {
                                                postRotate(rotation.toFloat())
                                            }

                                            // Store rotated photograph
                                            photo = Bitmap.createBitmap(
                                                bitmap,
                                                0,
                                                0,
                                                bitmap.width,
                                                bitmap.height,
                                                matrix,
                                                true
                                            )

                                            message = "Photo captured"

                                        } finally {

                                            // 13. Release image resources
                                            image.close()
                                        }
                                    }

                                    // Handle capture errors
                                    override fun onError(
                                        exception: ImageCaptureException
                                    ) {
                                        message = "Photo capture failed"
                                    }
                                }
                            )
                        }
                    ) {
                        Text("Take Photo")
                    }
                }
            }
        }
    }
}