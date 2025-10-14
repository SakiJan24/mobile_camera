package com.example.camera

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.camera.ui.theme.CameraTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.core.net.toUri



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CameraTheme { App() } }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun App() {
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)
    if (camPerm.status.isGranted) {
        CameraScreen()
    } else {
        PermissionScreen(onGrant = { camPerm.launchPermissionRequest() })
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEEF2FF), Color(0xFFE0F7FA), Color(0xFFFFF3E0))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.perm_sub))
            Spacer(Modifier.height(24.dp))
            Button(onClick = onGrant, shape = CircleShape) {
                Text(stringResource(R.string.perm_cta))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CameraScreen() {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel()


    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    var selector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    LaunchedEffect(selector) { controller.cameraSelector = selector }

    val photos by vm.photos.collectAsState()
    val cameraHeight = (LocalConfiguration.current.screenHeightDp * 0.30f).dp

    Column(Modifier.fillMaxSize()) {

        Box(
            Modifier
                .fillMaxWidth()
                .height(cameraHeight)
                .padding(12.dp)
        ) {
            CameraPreview(controller = controller, modifier = Modifier.fillMaxSize())


            FilledIconButton(
                onClick = {
                    selector = if (selector == CameraSelector.DEFAULT_BACK_CAMERA)
                        CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = stringResource(R.string.switch_camera))
            }


            FilledIconButton(
                onClick = {
                    takeAndSaveToExternal(
                        context = context,
                        controller = controller,
                        onSaved = { uri ->
                            vm.onPhotoSaved(uri)
                            Toast.makeText(context, context.getString(R.string.photo_saved), Toast.LENGTH_SHORT).show()
                        },
                        onError = { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Camera, contentDescription = stringResource(R.string.take_photo))
            }
        }

        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_photos))
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                items(photos) { uri ->
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun takeAndSaveToExternal(
    context: Context,
    controller: LifecycleCameraController,
    onSaved: (String) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val dir = File(baseDir, "PhotoBooth").apply { if (!exists()) mkdirs() }
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val output = File(dir, "IMG_$name.jpg")

    val options = ImageCapture.OutputFileOptions.Builder(output).build()
    controller.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(output.toUri().toString())
            }
            override fun onError(exception: ImageCaptureException) = onError(exception)
        }
    )
}
