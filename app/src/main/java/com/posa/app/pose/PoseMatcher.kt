package com.posa.app.pose

import kotlin.math.abs
import kotlin.math.ln

/** Puntos anatómicos (lado de la PERSONA) tal como los entrega el detector. */
enum class BodyPoint {
    NOSE,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE,
}

data class DetectedPoint(val position: Vec2, val confidence: Float)

/** Pose detectada en coordenadas de la vista de cámara (píxeles). */
class DetectedPose(val points: Map<BodyPoint, DetectedPoint>)

/**
 * Pose detectada expresada con lados de PANTALLA (igual que las plantillas).
 * [personLeftOnScreenLeft] indica si el lado izquierdo de la persona aparece a la izquierda
 * de la pantalla (cámara frontal en espejo) o a la derecha (cámara trasera).
 */
class ScreenPose(
    val joints: Map<Joint, DetectedPoint>,
    val personLeftOnScreenLeft: Boolean,
)

fun DetectedPose.toScreenPose(): ScreenPose? {
    val ls = points[BodyPoint.LEFT_SHOULDER] ?: return null
    val rs = points[BodyPoint.RIGHT_SHOULDER] ?: return null
    val leftOnLeft = ls.position.x <= rs.position.x

    fun side(personLeft: BodyPoint, personRight: BodyPoint, screenLeft: Boolean): DetectedPoint? =
        if (screenLeft == leftOnLeft) points[personLeft] else points[personRight]

    fun midpoint(a: DetectedPoint?, b: DetectedPoint?): DetectedPoint? {
        if (a == null || b == null) return null
        return DetectedPoint(a.position.mid(b.position), minOf(a.confidence, b.confidence))
    }

    val joints = buildMap<Joint, DetectedPoint> {
        fun put(j: Joint, p: DetectedPoint?) { if (p != null) this[j] = p }
        put(Joint.HEAD, points[BodyPoint.NOSE])
        put(Joint.LEFT_SHOULDER, side(BodyPoint.LEFT_SHOULDER, BodyPoint.RIGHT_SHOULDER, true))
        put(Joint.RIGHT_SHOULDER, side(BodyPoint.LEFT_SHOULDER, BodyPoint.RIGHT_SHOULDER, false))
        put(Joint.LEFT_ELBOW, side(BodyPoint.LEFT_ELBOW, BodyPoint.RIGHT_ELBOW, true))
        put(Joint.RIGHT_ELBOW, side(BodyPoint.LEFT_ELBOW, BodyPoint.RIGHT_ELBOW, false))
        put(Joint.LEFT_WRIST, side(BodyPoint.LEFT_WRIST, BodyPoint.RIGHT_WRIST, true))
        put(Joint.RIGHT_WRIST, side(BodyPoint.LEFT_WRIST, BodyPoint.RIGHT_WRIST, false))
        put(Joint.LEFT_HIP, side(BodyPoint.LEFT_HIP, BodyPoint.RIGHT_HIP, true))
        put(Joint.RIGHT_HIP, side(BodyPoint.LEFT_HIP, BodyPoint.RIGHT_HIP, false))
        put(Joint.LEFT_KNEE, side(BodyPoint.LEFT_KNEE, BodyPoint.RIGHT_KNEE, true))
        put(Joint.RIGHT_KNEE, side(BodyPoint.LEFT_KNEE, BodyPoint.RIGHT_KNEE, false))
        put(Joint.LEFT_ANKLE, side(BodyPoint.LEFT_ANKLE, BodyPoint.RIGHT_ANKLE, true))
        put(Joint.RIGHT_ANKLE, side(BodyPoint.LEFT_ANKLE, BodyPoint.RIGHT_ANKLE, false))
        put(Joint.NECK, midpoint(ls, rs))
        put(Joint.HIP_CENTER, midpoint(points[BodyPoint.LEFT_HIP], points[BodyPoint.RIGHT_HIP]))
    }
    return ScreenPose(joints, leftOnLeft)
}

enum class SegmentStatus { GOOD, CLOSE, OFF, MISSING }

data class MatchResult(
    /** 0..1 */
    val score: Float,
    val segments: Map<Segment, SegmentStatus>,
    val hint: String,
    /** Hay suficientes puntos del cuerpo visibles para comparar. */
    val visible: Boolean,
) {
    /** Coincide: buena puntuación y ninguna parte del cuerpo claramente fuera de lugar. */
    val matched: Boolean
        get() = visible && score >= PoseMatcher.MATCH_THRESHOLD &&
            segments.values.none { it == SegmentStatus.OFF }
}

object PoseMatcher {
    const val MIN_CONFIDENCE = 0.5f
    const val MATCH_THRESHOLD = 0.8f

    private const val GOOD_DEG = 20f
    private const val CLOSE_DEG = 35f

    fun segmentsFor(framing: Framing): List<Segment> =
        if (framing == Framing.HALF_BODY) Segment.entries.filterNot { it.isLeg } else Segment.entries

    /**
     * Compara la pose detectada con la figura objetivo.
     * @param spec definición de la figura (ángulos objetivo).
     * @param target la misma figura ya colocada en píxeles de pantalla (para encuadre).
     */
    fun match(spec: FigureSpec, target: Skeleton, framing: Framing, pose: ScreenPose): MatchResult {
        val required = segmentsFor(framing)
        val statuses = LinkedHashMap<Segment, SegmentStatus>()
        val diffs = LinkedHashMap<Segment, Float>()

        for (seg in required) {
            val a = pose.joints[seg.from]
            val b = pose.joints[seg.to]
            if (a == null || b == null || a.confidence < MIN_CONFIDENCE || b.confidence < MIN_CONFIDENCE) {
                statuses[seg] = SegmentStatus.MISSING
                continue
            }
            val v = b.position - a.position
            if (v.length() < 1f) {
                statuses[seg] = SegmentStatus.MISSING
                continue
            }
            val d = angleDiff(spec.targetAngle(seg), angleOf(v))
            diffs[seg] = d
            statuses[seg] = when {
                abs(d) <= GOOD_DEG -> SegmentStatus.GOOD
                abs(d) <= CLOSE_DEG -> SegmentStatus.CLOSE
                else -> SegmentStatus.OFF
            }
        }

        val visibleRatio = diffs.size.toFloat() / required.size
        val torsoVisible = diffs.containsKey(Segment.TORSO)
        if (visibleRatio < 0.6f || !torsoVisible) {
            val hint = when {
                pose.joints.isEmpty() -> "Colócate dentro de la silueta"
                framing == Framing.FULL_BODY && torsoVisible -> "Aléjate: que se vea todo tu cuerpo"
                else -> "Colócate dentro de la silueta"
            }
            return MatchResult(0f, statuses, hint, visible = false)
        }

        // Puntuación de la pose: promedio (el torso pesa más) mezclado con la peor parte,
        // para que un solo brazo fuera de lugar se note en la puntuación.
        var weighted = 0f
        var weights = 0f
        var worstScore = 1f
        for ((seg, d) in diffs) {
            val w = if (seg == Segment.TORSO) 1.5f else 1f
            val s = (1f - (abs(d) - 12f) / 38f).coerceIn(0f, 1f)
            weighted += w * s
            weights += w
            worstScore = minOf(worstScore, s)
        }
        val mean = if (weights > 0f) weighted / weights else 0f
        val poseScore = 0.65f * mean + 0.35f * worstScore

        // Encuadre: tamaño y posición horizontal respecto a la silueta
        val dNeck = pose.joints.getValue(Joint.NECK).position
        val dHip = pose.joints.getValue(Joint.HIP_CENTER).position
        val tNeck = target[Joint.NECK]
        val tHip = target[Joint.HIP_CENTER]
        val targetTorso = (tNeck - tHip).length().coerceAtLeast(1f)
        val ratio = ((dNeck - dHip).length() / targetTorso).coerceAtLeast(0.01f)
        val logRatio = ln(ratio)
        val offset = (dNeck.mid(dHip).x - tNeck.mid(tHip).x) / targetTorso
        val sizeScore = (1f - (abs(logRatio) - 0.15f) / 0.35f).coerceIn(0f, 1f)
        val posScore = (1f - (abs(offset) - 0.35f) / 0.8f).coerceIn(0f, 1f)
        val framingScore = sizeScore * posScore

        val score = 0.75f * poseScore + 0.25f * framingScore

        val hint = when {
            ratio < 0.75f -> "Acércate a la cámara"
            ratio > 1.33f -> "Aléjate un poco"
            abs(offset) > 0.6f -> {
                // Hay que moverse hacia el lado de pantalla contrario al desplazamiento
                val towardScreenLeft = offset > 0f
                "Muévete hacia tu ${personSideFor(towardScreenLeft, pose, feminine = true)}"
            }
            else -> {
                val worst = diffs.maxByOrNull { abs(it.value) }
                if (worst != null && abs(worst.value) > GOOD_DEG + 2f) {
                    limbHint(worst.key, spec.targetAngle(worst.key), spec.targetAngle(worst.key) - worst.value, pose)
                } else if (score >= MATCH_THRESHOLD && statuses.values.none { it == SegmentStatus.OFF }) {
                    "¡Perfecto! Mantén la pose"
                } else {
                    "Casi… ajusta poco a poco"
                }
            }
        }
        return MatchResult(score.coerceIn(0f, 1f), statuses, hint, visible = true)
    }

    /** Nombre del lado de la persona que corresponde a un lado de la pantalla. */
    private fun personSideFor(screenLeft: Boolean, pose: ScreenPose, feminine: Boolean): String {
        val isPersonLeft = screenLeft == pose.personLeftOnScreenLeft
        return when {
            isPersonLeft && feminine -> "izquierda"
            isPersonLeft -> "izquierdo"
            feminine -> "derecha"
            else -> "derecho"
        }
    }

    private fun limbHint(seg: Segment, targetAngle: Float, currentAngle: Float, pose: ScreenPose): String {
        val t = direction(targetAngle)
        val c = direction(currentAngle)
        val dy = t.y - c.y // negativo = hay que subir
        val outwardSign = if (seg.isScreenLeft) -1f else 1f
        val outward = (t.x - c.x) * outwardSign // positivo = alejar del cuerpo
        val m = personSideFor(seg.isScreenLeft, pose, feminine = false)
        val f = personSideFor(seg.isScreenLeft, pose, feminine = true)

        if (seg == Segment.TORSO) {
            val dx = t.x - c.x
            return if (abs(dx) < 0.08f) {
                "Endereza la espalda"
            } else {
                // dx > 0: el pecho debe ir hacia la derecha de la pantalla
                "Inclina el torso hacia tu ${personSideFor(screenLeft = dx < 0f, pose = pose, feminine = true)}"
            }
        }

        val vertical = abs(dy) >= abs(outward)
        val up = dy < 0f
        val out = outward > 0f
        return when (seg) {
            Segment.LEFT_UPPER_ARM, Segment.RIGHT_UPPER_ARM -> when {
                vertical && up -> "Sube el codo $m"
                vertical -> "Baja el codo $m"
                out -> "Separa el codo $m del cuerpo"
                else -> "Acerca el codo $m al cuerpo"
            }
            Segment.LEFT_FOREARM, Segment.RIGHT_FOREARM -> when {
                vertical && up -> "Sube la mano $f"
                vertical -> "Baja la mano $f"
                out -> "Lleva la mano $f hacia afuera"
                else -> "Lleva la mano $f hacia adentro"
            }
            Segment.LEFT_THIGH, Segment.RIGHT_THIGH -> when {
                vertical && up -> "Sube la rodilla $f"
                vertical -> "Baja la rodilla $f"
                out -> "Abre la pierna $f"
                else -> "Cierra la pierna $f"
            }
            Segment.LEFT_SHIN, Segment.RIGHT_SHIN -> when {
                vertical && up -> "Dobla más la rodilla $f"
                vertical -> "Estira la pierna $f"
                out -> "Mueve el pie $m hacia afuera"
                else -> "Mueve el pie $m hacia adentro"
            }
            Segment.TORSO -> "Endereza la espalda"
        }
    }
}
