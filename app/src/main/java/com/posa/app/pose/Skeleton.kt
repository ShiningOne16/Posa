package com.posa.app.pose

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Geometría pura (sin dependencias de Android) para describir y dibujar poses.
 *
 * Convención de ángulos (en grados, en coordenadas de pantalla con y hacia abajo):
 *   0°   = apunta hacia abajo
 *   90°  = apunta a la derecha de la pantalla
 *   -90° = apunta a la izquierda de la pantalla
 *   180° = apunta hacia arriba
 *
 * "Izquierda"/"derecha" en las articulaciones se refiere SIEMPRE al lado de la PANTALLA,
 * no al lado anatómico de la persona. Así la plantilla se ve igual que lo que muestra
 * la cámara, sin importar si es la frontal (espejo) o la trasera.
 */

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(k: Float) = Vec2(x * k, y * k)
    fun length(): Float = hypot(x, y)
    fun mid(o: Vec2) = Vec2((x + o.x) / 2f, (y + o.y) / 2f)
}

fun direction(angleDeg: Float): Vec2 {
    val r = Math.toRadians(angleDeg.toDouble())
    return Vec2(sin(r).toFloat(), cos(r).toFloat())
}

/** Ángulo del vector según la convención de arriba. */
fun angleOf(v: Vec2): Float = Math.toDegrees(atan2(v.x.toDouble(), v.y.toDouble())).toFloat()

/** Diferencia firmada a - b en el rango [-180, 180]. */
fun angleDiff(a: Float, b: Float): Float {
    var d = (a - b) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

enum class Joint {
    HEAD, NECK,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    HIP_CENTER,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE,
}

/** Segmentos del cuerpo que se comparan y se dibujan. */
enum class Segment(val from: Joint, val to: Joint, val isLeg: Boolean = false) {
    TORSO(Joint.HIP_CENTER, Joint.NECK),
    LEFT_UPPER_ARM(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW),
    LEFT_FOREARM(Joint.LEFT_ELBOW, Joint.LEFT_WRIST),
    RIGHT_UPPER_ARM(Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW),
    RIGHT_FOREARM(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST),
    LEFT_THIGH(Joint.LEFT_HIP, Joint.LEFT_KNEE, isLeg = true),
    LEFT_SHIN(Joint.LEFT_KNEE, Joint.LEFT_ANKLE, isLeg = true),
    RIGHT_THIGH(Joint.RIGHT_HIP, Joint.RIGHT_KNEE, isLeg = true),
    RIGHT_SHIN(Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE, isLeg = true);

    val isScreenLeft: Boolean get() = name.startsWith("LEFT")
}

/** Ángulos de una extremidad: segmento superior (brazo/muslo) e inferior (antebrazo/pantorrilla). */
data class Limb(val upper: Float, val lower: Float)

/**
 * Descripción compacta de una figura. Las longitudes están en "alturas de cuerpo":
 * una figura de pie mide aproximadamente 1.0 de la coronilla a los tobillos.
 */
data class FigureSpec(
    val leftArm: Limb = Limb(-8f, -4f),
    val rightArm: Limb = Limb(8f, 4f),
    val leftLeg: Limb = Limb(-3f, 0f),
    val rightLeg: Limb = Limb(3f, 0f),
    /** Dirección cadera → cuello. 180 = recto. */
    val torso: Float = 180f,
    /** Inclinación de la cabeza respecto al torso. */
    val headTilt: Float = 0f,
    /** Ancho de hombros/cadera: 1 = de frente, ~0.45 = de perfil. */
    val turn: Float = 1f,
    /** Posición del centro de la cadera relativa a la composición (en alturas de cuerpo). */
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
)

object Proportions {
    const val TORSO = 0.30f
    const val NECK_TO_HEAD = 0.085f
    const val HEAD_RADIUS = 0.055f
    const val HALF_SHOULDER = 0.105f
    const val HALF_HIP = 0.065f
    const val UPPER_ARM = 0.155f
    const val FOREARM = 0.145f
    const val THIGH = 0.235f
    const val SHIN = 0.235f
}

class Skeleton(val joints: Map<Joint, Vec2>, val headRadius: Float, val unit: Float) {
    operator fun get(j: Joint): Vec2 = joints.getValue(j)

    fun transformed(scale: Float, offset: Vec2) = Skeleton(
        joints.mapValues { (_, p) -> p * scale + offset },
        headRadius * scale,
        unit * scale,
    )
}

fun FigureSpec.toSkeleton(): Skeleton {
    val s = scale
    val turnClamped = turn.coerceIn(0.3f, 1f)
    val hip = Vec2(x, y)
    val up = direction(torso)
    val perp = Vec2(-up.y, up.x) // hacia la derecha de la pantalla cuando el torso está recto
    val neck = hip + up * (Proportions.TORSO * s)

    val ls = neck - perp * (Proportions.HALF_SHOULDER * turnClamped * s)
    val rs = neck + perp * (Proportions.HALF_SHOULDER * turnClamped * s)
    val lh = hip - perp * (Proportions.HALF_HIP * turnClamped * s)
    val rh = hip + perp * (Proportions.HALF_HIP * turnClamped * s)

    val head = neck + direction(torso + headTilt) * (Proportions.NECK_TO_HEAD * s)

    val le = ls + direction(leftArm.upper) * (Proportions.UPPER_ARM * s)
    val lw = le + direction(leftArm.lower) * (Proportions.FOREARM * s)
    val re = rs + direction(rightArm.upper) * (Proportions.UPPER_ARM * s)
    val rw = re + direction(rightArm.lower) * (Proportions.FOREARM * s)

    val lk = lh + direction(leftLeg.upper) * (Proportions.THIGH * s)
    val la = lk + direction(leftLeg.lower) * (Proportions.SHIN * s)
    val rk = rh + direction(rightLeg.upper) * (Proportions.THIGH * s)
    val ra = rk + direction(rightLeg.lower) * (Proportions.SHIN * s)

    return Skeleton(
        mapOf(
            Joint.HEAD to head, Joint.NECK to neck,
            Joint.LEFT_SHOULDER to ls, Joint.RIGHT_SHOULDER to rs,
            Joint.LEFT_ELBOW to le, Joint.RIGHT_ELBOW to re,
            Joint.LEFT_WRIST to lw, Joint.RIGHT_WRIST to rw,
            Joint.HIP_CENTER to hip,
            Joint.LEFT_HIP to lh, Joint.RIGHT_HIP to rh,
            Joint.LEFT_KNEE to lk, Joint.RIGHT_KNEE to rk,
            Joint.LEFT_ANKLE to la, Joint.RIGHT_ANKLE to ra,
        ),
        headRadius = Proportions.HEAD_RADIUS * s,
        unit = s,
    )
}

/** Ángulo objetivo de cada segmento (lo que se compara con la pose detectada). */
fun FigureSpec.targetAngle(segment: Segment): Float = when (segment) {
    Segment.TORSO -> torso
    Segment.LEFT_UPPER_ARM -> leftArm.upper
    Segment.LEFT_FOREARM -> leftArm.lower
    Segment.RIGHT_UPPER_ARM -> rightArm.upper
    Segment.RIGHT_FOREARM -> rightArm.lower
    Segment.LEFT_THIGH -> leftLeg.upper
    Segment.LEFT_SHIN -> leftLeg.lower
    Segment.RIGHT_THIGH -> rightLeg.upper
    Segment.RIGHT_SHIN -> rightLeg.lower
}

enum class Framing { FULL_BODY, HALF_BODY }

enum class PoseCategory(val label: String) {
    PORTRAIT("Retrato"),
    FASHION("Moda"),
    COUPLE("Pareja"),
    GROUP("Grupo"),
}

data class PoseTemplate(
    val id: String,
    val name: String,
    val category: PoseCategory,
    val framing: Framing,
    val figures: List<FigureSpec>,
    val tips: List<String>,
) {
    /** La detección de ML Kit sigue a una sola persona, así que solo se puntúan poses individuales. */
    val isScorable: Boolean get() = figures.size == 1
}

data class Rect2(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

/**
 * Coloca las figuras de una plantilla dentro de un lienzo de [width] x [height] píxeles,
 * dejando espacio para la barra superior y los controles inferiores.
 */
fun PoseTemplate.layout(width: Float, height: Float, safeTop: Float, safeBottom: Float): List<Skeleton> {
    val skeletons = figures.map { it.toSkeleton() }
    if (skeletons.isEmpty() || width <= 0f || height <= 0f) return emptyList()

    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    for (sk in skeletons) {
        for ((joint, p) in sk.joints) {
            val isLegJoint = joint == Joint.LEFT_KNEE || joint == Joint.RIGHT_KNEE ||
                joint == Joint.LEFT_ANKLE || joint == Joint.RIGHT_ANKLE
            if (framing == Framing.HALF_BODY && isLegJoint) continue
            val r = if (joint == Joint.HEAD) sk.headRadius else 0.03f * sk.unit
            minX = minOf(minX, p.x - r); maxX = maxOf(maxX, p.x + r)
            minY = minOf(minY, p.y - r); maxY = maxOf(maxY, p.y + r)
        }
        if (framing == Framing.HALF_BODY) {
            // Incluye un poco por debajo de la cadera para encuadrar a medio cuerpo
            maxY = maxOf(maxY, sk[Joint.HIP_CENTER].y + 0.10f * sk.unit)
        }
    }
    val content = Rect2(minX, minY, maxX, maxY)

    val areaTop = safeTop + height * 0.03f
    val areaBottom = if (framing == Framing.HALF_BODY) height - safeBottom * 0.7f else height - safeBottom
    val areaHeight = (areaBottom - areaTop).coerceAtLeast(height * 0.3f)
    val areaWidth = width * if (framing == Framing.HALF_BODY) 0.8f else 0.9f

    val scale = minOf(areaHeight / content.height, areaWidth / content.width)
    val offsetX = width / 2f - (content.left + content.width / 2f) * scale
    val offsetY = if (framing == Framing.HALF_BODY) {
        areaTop - content.top * scale
    } else {
        areaTop + (areaHeight - content.height * scale) / 2f - content.top * scale
    }
    return skeletons.map { it.transformed(scale, Vec2(offsetX, offsetY)) }
}
