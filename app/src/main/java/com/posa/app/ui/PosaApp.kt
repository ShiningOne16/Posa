package com.posa.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PosaApp() {
    val context = LocalContext.current
    fun cameraGranted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(cameraGranted()) }
    var askedOnce by remember { mutableStateOf(false) }

    val permissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result[Manifest.permission.CAMERA] == true || cameraGranted()
        askedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(permissions)
    }

    if (granted) {
        CameraScreen()
    } else {
        PermissionScreen(showRetry = askedOnce, onRequest = { launcher.launch(permissions) })
    }
}

@Composable
private fun PermissionScreen(showRetry: Boolean, onRequest: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = PosaColors.Accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Posa necesita la cámara", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Usamos la cámara para mostrarte la silueta de la pose y detectar tu postura en tiempo real. " +
                    "Las imágenes se analizan en el teléfono y no se envían a ningún servidor.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            if (showRetry) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRequest) { Text("Dar permiso") }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Si lo rechazaste antes, actívalo en Ajustes › Apps › Posa › Permisos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
