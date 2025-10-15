// paquete base del módulo
package com.example.camera

// dependencias de Android y utilidades
import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// CameraX: selección de cámara y captura de imágenes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController

// Compose: UI, layouts y estilos
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

// carga de imágenes en la galería con Coil
import coil.compose.AsyncImage

// tema de la app
import com.example.camera.ui.theme.CameraTheme

// permisos con accompanist
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// utilidades de E/S y formato
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

// ejecutor principal y conversión a Uri
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

// animaciones para el gradiente
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

// utilidades de layout adicionales
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio

// recorte de esquinas
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

// tarjeta de miniatura
import androidx.compose.material3.Card

// unidades y escalado de contenido
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale

// grilla para la galería
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

// para geometría y dibujo
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawWithCache

// actividad principal: monta el tema y la raíz de la UI
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CameraTheme { App() } }
    }
}

// flujo de permisos y navegación entre pantalla de permiso y cámara
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun App() {
    // estado del permiso de cámara
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)
    // pantalla según estado del permiso
    if (camPerm.status.isGranted) {
        CameraScreen()
    } else {
        PermissionScreen(onGrant = { camPerm.launchPermissionRequest() })
    }
}

// pantalla que solicita permiso con fondo degradado
@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // fondo vertical con colores
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEEF2FF), Color(0xFFE0F7FA), Color(0xFFFFF3E0))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            // título del permiso
            Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            // subtítulo del permiso
            Text(stringResource(R.string.perm_sub))
            Spacer(Modifier.height(24.dp))
            // botón para solicitar el permiso
            Button(onClick = onGrant, shape = CircleShape) {
                Text(stringResource(R.string.perm_cta))
            }
        }
    }
}

// pantalla principal de cámara: visor, controles y galería de sesión
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CameraScreen() {
    // contexto actual
    val context = LocalContext.current
    // viewmodel con estado de fotos
    val vm: MainViewModel = viewModel()

    // controlador de CameraX con uso de captura de imágenes
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    // selector de cámara adelante y atras
    var selector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    // aplica el selector al controlador cuando cambie
    LaunchedEffect(selector) { controller.cameraSelector = selector }

    // colección de fotos guardadas en la sesión
    val photos by vm.photos.collectAsState()
    // altura del visor
    val cameraHeight = (LocalConfiguration.current.screenHeightDp * 0.30f).dp

    // layout vertical principal
    Column(Modifier.fillMaxSize()) {

        // marco animado
        RotatingRainbowFrame(
            modifier = Modifier
                .fillMaxWidth()
                .height(cameraHeight)
                .padding(12.dp)
        ) {

            // previsualización de la cámara
            CameraPreview(controller = controller, modifier = Modifier.fillMaxSize())

            // botón para cambiar de camara
            FilledIconButton(
                onClick = {
                    selector =
                        if (selector == CameraSelector.DEFAULT_BACK_CAMERA)
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else
                            CameraSelector.DEFAULT_BACK_CAMERA
                },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.switch_camera)
                )
            }

            // botón para capturar foto y guardarla
            FilledIconButton(
                onClick = {
                    takeAndSaveToExternal(
                        context = context,
                        controller = controller,
                        onSaved = { uri ->
                            // notifica al VM y muestra toast de confirmación
                            vm.onPhotoSaved(uri)
                            Toast.makeText(
                                context,
                                context.getString(R.string.photo_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = stringResource(R.string.take_photo)
                )
            }
        }

        // estado vacío: mensaje cuando no hay fotos
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_photos))
            }
        } else {
            // grilla 3xN de miniaturas con espaciado uniforme
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // tarjeta por foto con recorte redondeado y crop
                items(photos) { uri ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

// captura y guardado de imagen en almacenamiento privado
//esta en /storage/emulated/0/Android/data/com.example.photobooth/files/Pictures/PhotoBooth/IMG_20251015_123456.jpg
private fun takeAndSaveToExternal(
    context: Context,
    controller: LifecycleCameraController,
    onSaved: (String) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    // base de imágenes de la app
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    // carpeta del proyecto si no existe
    val dir = File(baseDir, "PhotoBooth").apply { if (!exists()) mkdirs() }
    // nombre con timestamp
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    // archivo de salida
    val output = File(dir, "IMG_$name.jpg")

    // opciones de captura hacia archivo
    val options = ImageCapture.OutputFileOptions.Builder(output).build()
    // captura y callback en el ejecutor principal
    controller.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            // exito
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(output.toUri().toString())
            }
            // error
            override fun onError(exception: ImageCaptureException) = onError(exception)
        }
    )
}

// contenedor marco de coloores
@Composable
private fun RotatingRainbowFrame(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    frameWidth: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // animacion para rotar
    val angle by rememberInfiniteTransition(label = "rainbow")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )

    val frac = (angle % 360f) / 360f

    // paleta del "borde"
    val colors = listOf(
        Color(0xFF8E3200),  // dorado
        Color(0xFF7C83FD),  // violeta
        Color(0xFF21C4D6),  // cian
        Color(0xFF00A878),  // verde
        Color(0xFF8E3200)   // dorado (cierre)
    )
    // stops base equiespaciados
    val baseStops = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

    // caja con clip y dibujo de marco antes del contenido
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                // borde y radios
                val r = cornerRadius.toPx()
                val stroke = frameWidth.toPx()
                val inset = stroke / 2f
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // recálculo para simular rotación del color
                val shifted = buildList {
                    for (i in baseStops.indices) {
                        val s = (baseStops[i] + frac)
                        val v = if (s > 1f) s - 1f else s
                        add(v to colors[i])
                    }
                }.sortedBy { it.first }

                val brush = Brush.sweepGradient(
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    colorStops = shifted.toTypedArray()
                )

                // dibujo del borde, las esquinas redondas
                onDrawBehind {
                    drawRoundRect(
                        brush = brush,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(w - 2 * inset, h - 2 * inset),
                        cornerRadius = CornerRadius(r, r),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                    )
                }
            }
            // espacio interno para que el contenido no tape el borde
            .padding(frameWidth)
    ) {
        // contenedor del visor con recorte interior y fondo negro
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(cornerRadius - frameWidth))
                .background(Color.Black)
        ) {
            content()
        }
    }
}
