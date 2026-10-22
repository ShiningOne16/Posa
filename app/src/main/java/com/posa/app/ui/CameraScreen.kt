package com.posa.app.ui

import android.media.MediaActionSound
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Timer10
import androidx.compose.material.icons.rounded.Timer3
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.posa.app.camera.PoseCamera
import com.posa.app.camera.loadThumbnail
import com.posa.app.camera.openPhoto
import com.posa.app.pose.DetectedPose
import com.posa.app.pose.MatchResult
import com.posa.app.pose.PoseCategory
import com.posa.app.pose.PoseLibrary
import com.posa.app.pose.PoseMatcher
import com.posa.app.pose.PoseTemplate
import com.posa.app.pose.ScreenPose
import com.posa.app.pose.layout
import com.posa.app.pose.toScreenPose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tiempo que hay que sostener la pose para el disparo automático. */
private const val AUTO_HOLD_SECONDS = 1.2f

/** Espacio reservado bajo la barra superior para la pastilla de indicaciones. */
private val HINT_AREA = 84.dp

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val camera = remember { PoseCamera(context) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val shutterSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }

    // ── Opciones ──
    var category by remember { mutableStateOf(PoseCategory.PORTRAIT) }
    var template by remember { mutableStateOf(PoseLibrary.all.first()) }
    var useFront by remember { mutableStateOf(false) }
    var showSilhouette by remember { mutableStateOf(true) }
    var showSkeleton by remember { mutableStateOf(true) }
    var autoCapture by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var showTips by remember { mutableStateOf(false) }

    // ── Estado en vivo ──
    var screenPose by remember { mutableStateOf<ScreenPose?>(null) }
    var match by remember { mutableStateOf<MatchResult?>(null) }
    var smoothScore by remember { mutableFloatStateOf(0f) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    var lastThumb by remember { mutableStateOf<ImageBitmap?>(null) }
    val flash = remember { Animatable(0f) }
    val lastFrameMs = remember { longArrayOf(0L) }
    val cooldownUntil = remember { longArrayOf(0L) }

    // ── Geometría de la pantalla ──
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var topAreaBottom by remember { mutableFloatStateOf(0f) }
    var bottomPanelTop by remember { mutableFloatStateOf(0f) }

    val targets = remember(template, rootSize, topAreaBottom, bottomPanelTop) {
        val w = rootSize.width.toFloat()
        val h = rootSize.height.toFloat()
        val safeBottom = if (bottomPanelTop > 0f) h - bottomPanelTop else h * 0.25f
        template.layout(w, h, safeTop = topAreaBottom, safeBottom = safeBottom)
    }

    LaunchedEffect(template) {
        match = null
        smoothScore = 0f
        holdProgress = 0f
    }

    fun shoot() {
        if (capturing) return
        capturing = true
        shutterSound.play(MediaActionSound.SHUTTER_CLICK)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            flash.snapTo(0.85f)
            flash.animateTo(0f, tween(durationMillis = 350))
        }
        camera.takePhoto(
            onPhotoSaved = { uri ->
                capturing = false
                cooldownUntil[0] = SystemClock.elapsedRealtime() + 2500L
                lastPhoto = uri
                if (uri != null) scope.launch { lastThumb = loadThumbnail(context, uri) }
            },
            onPhotoError = { e ->
                capturing = false
                Toast.makeText(context, "No se pudo guardar la foto: ${e.message}", Toast.LENGTH_LONG).show()
            },
        )
    }

    fun startCapture() {
        if (capturing || countdown != null) return
        if (timerSeconds == 0) {
            shoot()
        } else {
            scope.launch {
                for (s in timerSeconds downTo 1) {
                    countdown = s
                    delay(1000)
                }
                countdown = null
                shoot()
            }
        }
    }

    // Se llama en cada fotograma analizado (en el hilo principal)
    val onFrame: (DetectedPose?) -> Unit = { detected ->
        val sp = detected?.toScreenPose()
        screenPose = sp

        val now = SystemClock.elapsedRealtime()
        val dt = (now - lastFrameMs[0]).coerceIn(0L, 250L) / 1000f
        lastFrameMs[0] = now

        val result = if (sp != null && template.isScorable && targets.isNotEmpty()) {
            PoseMatcher.match(template.figures.first(), targets.first(), template.framing, sp)
        } else {
            null
        }
        match = result
        smoothScore += ((result?.score ?: 0f) - smoothScore) * 0.25f

        val canHold = autoCapture && result?.matched == true && !capturing &&
            countdown == null && now >= cooldownUntil[0]
        holdProgress = if (canHold) {
            (holdProgress + dt / AUTO_HOLD_SECONDS).coerceAtMost(1f)
        } else {
            (holdProgress - dt * 2f).coerceAtLeast(0f)
        }
        if (holdProgress >= 1f) {
            holdProgress = 0f
            shoot()
        }
    }
    val currentOnFrame by rememberUpdatedState(onFrame)

    DisposableEffect(Unit) {
        camera.start(lifecycleOwner, previewView) { currentOnFrame(it) }
        onDispose {
            camera.release()
            shutterSound.release()
        }
    }
    LaunchedEffect(useFront) { camera.setFrontCamera(useFront) }

    val matched = match?.matched == true
    val outlineColor by animateColorAsState(
        targetValue = if (matched) PosaColors.Match else PosaColors.Accent,
        label = "outline",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { rootSize = it }
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Silueta guía
        if (showSilhouette && targets.isNotEmpty()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawSilhouette(
                    skeletons = targets,
                    outline = outlineColor,
                    fill = Color.White.copy(alpha = 0.16f),
                    outlineWidth = 2.5.dp.toPx(),
                )
            }
        }

        // Esqueleto detectado
        val sp = screenPose
        if (showSkeleton && sp != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawDetectedSkeleton(
                    pose = sp,
                    statuses = match?.segments ?: emptyMap(),
                    strokeWidth = 4.dp.toPx(),
                    minConfidence = PoseMatcher.MIN_CONFIDENCE,
                )
            }
        }

        // Destello al tomar la foto
        if (flash.value > 0f) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }

        // Cuenta regresiva
        countdown?.let {
            Text(
                text = "$it",
                color = Color.White,
                fontSize = 140.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ── Parte superior ──
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            TopBar(
                modifier = Modifier.onGloballyPositioned {
                    topAreaBottom = it.boundsInRoot().bottom + with(density) { HINT_AREA.toPx() }
                },
                showTips = showTips,
                onTips = { showTips = !showTips },
                showSilhouette = showSilhouette,
                onSilhouette = { showSilhouette = !showSilhouette },
                showSkeleton = showSkeleton,
                onSkeleton = { showSkeleton = !showSkeleton },
                timerSeconds = timerSeconds,
                onTimer = { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 } },
                autoCapture = autoCapture,
                onAuto = {
                    if (!template.isScorable && !autoCapture) {
                        Toast.makeText(context, "El modo Auto funciona con poses individuales", Toast.LENGTH_SHORT).show()
                    }
                    autoCapture = !autoCapture
                },
            )
            Spacer(Modifier.height(8.dp))
            HintPill(template = template, match = match, smoothScore = smoothScore, hasPerson = sp != null)
            AnimatedVisibility(
                visible = showTips,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                TipsCard(template)
            }
        }

        // ── Parte inferior ──
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { bottomPanelTop = it.boundsInRoot().top }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            CategoryRow(
                selected = category,
                onSelect = {
                    category = it
                    template = PoseLibrary.byCategory(it).first()
                },
            )
            Spacer(Modifier.height(10.dp))
            PoseStrip(
                poses = PoseLibrary.byCategory(category),
                selected = template,
                onSelect = { template = it },
            )
            Spacer(Modifier.height(14.dp))
            ShutterRow(
                thumbnail = lastThumb,
                onGallery = { lastPhoto?.let { openPhoto(context, it) } },
                onShutter = { startCapture() },
                holdProgress = holdProgress,
                autoCapture = autoCapture,
                busy = capturing || countdown != null,
                onFlip = {
                    if (camera.hasCamera(!useFront)) useFront = !useFront
                    else Toast.makeText(context, "Este teléfono no tiene otra cámara", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

// ───────────────────────── Componentes ─────────────────────────

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    active: Boolean = false,
    size: Int = 44,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (active) PosaColors.Accent else PosaColors.Scrim)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (active) Color.Black else Color.White)
    }
}

@Composable
private fun TopBar(
    modifier: Modifier,
    showTips: Boolean, onTips: () -> Unit,
    showSilhouette: Boolean, onSilhouette: () -> Unit,
    showSkeleton: Boolean, onSkeleton: () -> Unit,
    timerSeconds: Int, onTimer: () -> Unit,
    autoCapture: Boolean, onAuto: () -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Posa",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        RoundButton(Icons.Rounded.Lightbulb, "Consejos", active = showTips, onClick = onTips)
        RoundButton(
            if (showSilhouette) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            "Mostrar silueta", active = showSilhouette, onClick = onSilhouette,
        )
        RoundButton(Icons.Rounded.AccessibilityNew, "Mostrar esqueleto", active = showSkeleton, onClick = onSkeleton)
        RoundButton(
            when (timerSeconds) {
                3 -> Icons.Rounded.Timer3
                10 -> Icons.Rounded.Timer10
                else -> Icons.Rounded.TimerOff
            },
            "Temporizador", active = timerSeconds > 0, onClick = onTimer,
        )
        RoundButton(Icons.Rounded.AutoAwesome, "Disparo automático", active = autoCapture, onClick = onAuto)
    }
}

@Composable
private fun HintPill(template: PoseTemplate, match: MatchResult?, smoothScore: Float, hasPerson: Boolean) {
    val matched = match?.matched == true
    val hint = when {
        !template.isScorable -> template.tips.first()
        match != null -> match.hint
        hasPerson -> "Ponte de frente y que se vea tu cuerpo"
        else -> "Colócate dentro de la silueta"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (matched) PosaColors.Match.copy(alpha = 0.9f) else PosaColors.Panel)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (template.isScorable) {
                CircularProgressIndicator(
                    progress = { smoothScore.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    color = if (matched) Color.Black else PosaColors.Accent,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeWidth = 4.dp,
                )
                Text(
                    "${(smoothScore * 100).toInt()}",
                    color = if (matched) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = PosaColors.Accent)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = template.name.uppercase() +
                    if (!template.isScorable) " · ${template.figures.size} personas" else "",
                color = if (matched) Color.Black.copy(alpha = 0.7f) else PosaColors.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = hint,
                color = if (matched) Color.Black else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TipsCard(template: PoseTemplate) {
    Column(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PosaColors.Panel)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Consejos para esta pose", color = PosaColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        template.tips.forEach { tip ->
            Row {
                Text("•  ", color = PosaColors.Accent, fontSize = 15.sp)
                Text(tip, color = Color.White, fontSize = 15.sp)
            }
        }
        if (!template.isScorable) {
            Text(
                "La detección en vivo sigue a una sola persona, así que en poses de pareja o grupo " +
                    "la silueta funciona como guía visual.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun CategoryRow(selected: PoseCategory, onSelect: (PoseCategory) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PoseCategory.entries.forEach { cat ->
            FilterChip(
                selected = cat == selected,
                onClick = { onSelect(cat) },
                label = { Text(cat.label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = PosaColors.Scrim,
                    labelColor = Color.White,
                    selectedContainerColor = PosaColors.Accent,
                    selectedLabelColor = Color.Black,
                ),
            )
        }
    }
}

@Composable
private fun PoseStrip(poses: List<PoseTemplate>, selected: PoseTemplate, onSelect: (PoseTemplate) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(poses, key = { it.id }) { pose ->
            PoseCard(pose, isSelected = pose.id == selected.id, onClick = { onSelect(pose) })
        }
    }
}

@Composable
private fun PoseCard(pose: PoseTemplate, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 76.dp, height = 92.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) PosaColors.Accent.copy(alpha = 0.22f) else Color(0x33FFFFFF))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) PosaColors.Accent else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                )
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val figures = pose.layout(size.width, size.height, safeTop = 0f, safeBottom = 0f)
                val color = if (isSelected) PosaColors.Accent else Color.White
                figures.forEach { drawFigureShape(it, color) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            pose.name,
            color = if (isSelected) PosaColors.Accent else Color.White,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShutterRow(
    thumbnail: ImageBitmap?,
    onGallery: () -> Unit,
    onShutter: () -> Unit,
    holdProgress: Float,
    autoCapture: Boolean,
    busy: Boolean,
    onFlip: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Última foto
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(PosaColors.Scrim)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable(enabled = thumbnail != null, onClick = onGallery),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(thumbnail, contentDescription = "Última foto", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
            }
        }

        // Disparador con anillo de "sostén la pose"
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .clickable(enabled = !busy, onClick = onShutter),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2
                drawCircle(Color.White, radius = size.minDimension / 2 - inset, style = Stroke(stroke))
                if (holdProgress > 0f) {
                    drawArc(
                        color = PosaColors.Match,
                        startAngle = -90f,
                        sweepAngle = 360f * holdProgress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                drawCircle(
                    if (busy) Color.White.copy(alpha = 0.5f) else Color.White,
                    radius = size.minDimension / 2 - 10.dp.toPx(),
                )
            }
            if (autoCapture) {
                Text("AUTO", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        RoundButton(Icons.Rounded.Cameraswitch, "Cambiar cámara", size = 52, onClick = onFlip)
    }
}
