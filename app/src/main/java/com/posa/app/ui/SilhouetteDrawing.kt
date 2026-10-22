package com.posa.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.posa.app.pose.Joint
import com.posa.app.pose.ScreenPose
import com.posa.app.pose.Segment
import com.posa.app.pose.SegmentStatus
import com.posa.app.pose.Skeleton
import com.posa.app.pose.Vec2

private fun Vec2.toOffset() = Offset(x, y)

/** Grosor de cada parte (en alturas de cuerpo). */
private val limbWidths = listOf(
    Triple(Joint.NECK, Joint.HEAD, 0.05f),
    Triple(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, 0.066f),
    Triple(Joint.LEFT_ELBOW, Joint.LEFT_WRIST, 0.052f),
    Triple(Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW, 0.066f),
    Triple(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, 0.052f),
    Triple(Joint.LEFT_HIP, Joint.LEFT_KNEE, 0.09f),
    Triple(Joint.LEFT_KNEE, Joint.LEFT_ANKLE, 0.068f),
    Triple(Joint.RIGHT_HIP, Joint.RIGHT_KNEE, 0.09f),
    Triple(Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE, 0.068f),
)

/**
 * Dibuja el cuerpo de una figura como formas redondeadas. [grow] engorda todas las formas
 * (en píxeles), lo que permite dibujar primero un contorno y luego el relleno encima.
 */
fun DrawScope.drawFigureShape(sk: Skeleton, color: Color, grow: Float = 0f, blendMode: BlendMode = BlendMode.SrcOver) {
    val u = sk.unit
    // Torso: polígono hombros–caderas con esquinas redondeadas
    val torso = Path().apply {
        moveTo(sk[Joint.LEFT_SHOULDER].x, sk[Joint.LEFT_SHOULDER].y)
        lineTo(sk[Joint.RIGHT_SHOULDER].x, sk[Joint.RIGHT_SHOULDER].y)
        lineTo(sk[Joint.RIGHT_HIP].x, sk[Joint.RIGHT_HIP].y)
        lineTo(sk[Joint.LEFT_HIP].x, sk[Joint.LEFT_HIP].y)
        close()
    }
    drawPath(torso, color, style = Fill, blendMode = blendMode)
    drawPath(
        torso, color,
        style = Stroke(width = 0.05f * u + 2 * grow, join = StrokeJoin.Round),
        blendMode = blendMode,
    )
    for ((a, b, w) in limbWidths) {
        drawLine(
            color = color,
            start = sk[a].toOffset(),
            end = sk[b].toOffset(),
            strokeWidth = w * u + 2 * grow,
            cap = StrokeCap.Round,
            blendMode = blendMode,
        )
    }
    drawCircle(color, radius = sk.headRadius + grow, center = sk[Joint.HEAD].toOffset(), blendMode = blendMode)
}

/**
 * Silueta guía: contorno sólido + relleno translúcido uniforme.
 * Debe dibujarse dentro de una capa con CompositingStrategy.Offscreen para que
 * BlendMode.Src reemplace el interior sin que las partes superpuestas se oscurezcan.
 */
fun DrawScope.drawSilhouette(skeletons: List<Skeleton>, outline: Color, fill: Color, outlineWidth: Float) {
    for (sk in skeletons) drawFigureShape(sk, outline, grow = outlineWidth)
    for (sk in skeletons) drawFigureShape(sk, fill, blendMode = BlendMode.Src)
}

fun statusColor(status: SegmentStatus?): Color = when (status) {
    SegmentStatus.GOOD -> PosaColors.Match
    SegmentStatus.CLOSE -> PosaColors.Close
    SegmentStatus.OFF -> PosaColors.Off
    else -> Color.White.copy(alpha = 0.7f)
}

/** Esqueleto detectado por ML Kit, coloreado según qué tan bien coincide cada parte. */
fun DrawScope.drawDetectedSkeleton(
    pose: ScreenPose,
    statuses: Map<Segment, SegmentStatus>,
    strokeWidth: Float,
    minConfidence: Float,
) {
    fun point(j: Joint) = pose.joints[j]?.takeIf { it.confidence >= minConfidence }?.position?.toOffset()

    // Líneas de hombros y caderas
    listOf(Joint.LEFT_SHOULDER to Joint.RIGHT_SHOULDER, Joint.LEFT_HIP to Joint.RIGHT_HIP).forEach { (a, b) ->
        val pa = point(a); val pb = point(b)
        if (pa != null && pb != null) {
            drawLine(Color.White.copy(alpha = 0.7f), pa, pb, strokeWidth * 0.7f, cap = StrokeCap.Round)
        }
    }
    for (seg in Segment.entries) {
        val pa = point(seg.from) ?: continue
        val pb = point(seg.to) ?: continue
        drawLine(statusColor(statuses[seg]), pa, pb, strokeWidth, cap = StrokeCap.Round)
    }
    for ((joint, p) in pose.joints) {
        if (p.confidence < minConfidence || joint == Joint.NECK || joint == Joint.HIP_CENTER) continue
        drawCircle(Color.White, radius = strokeWidth * 0.75f, center = p.position.toOffset())
    }
}
