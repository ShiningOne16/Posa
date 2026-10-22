package com.posa.app.pose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseMatcherTest {

    private val width = 1080f
    private val height = 2340f

    /** Convierte una figura de plantilla en una "detección" falsa. */
    private fun fakeDetection(sk: Skeleton, backCamera: Boolean): DetectedPose {
        val pairs = listOf(
            Triple(BodyPoint.LEFT_SHOULDER, BodyPoint.RIGHT_SHOULDER, Joint.LEFT_SHOULDER to Joint.RIGHT_SHOULDER),
            Triple(BodyPoint.LEFT_ELBOW, BodyPoint.RIGHT_ELBOW, Joint.LEFT_ELBOW to Joint.RIGHT_ELBOW),
            Triple(BodyPoint.LEFT_WRIST, BodyPoint.RIGHT_WRIST, Joint.LEFT_WRIST to Joint.RIGHT_WRIST),
            Triple(BodyPoint.LEFT_HIP, BodyPoint.RIGHT_HIP, Joint.LEFT_HIP to Joint.RIGHT_HIP),
            Triple(BodyPoint.LEFT_KNEE, BodyPoint.RIGHT_KNEE, Joint.LEFT_KNEE to Joint.RIGHT_KNEE),
            Triple(BodyPoint.LEFT_ANKLE, BodyPoint.RIGHT_ANKLE, Joint.LEFT_ANKLE to Joint.RIGHT_ANKLE),
        )
        val points = mutableMapOf(BodyPoint.NOSE to DetectedPoint(sk[Joint.HEAD], 0.99f))
        for ((personLeft, personRight, screen) in pairs) {
            // Cámara trasera: el lado izquierdo de la persona aparece a la derecha de la pantalla
            val (l, r) = if (backCamera) screen.second to screen.first else screen
            points[personLeft] = DetectedPoint(sk[l], 0.99f)
            points[personRight] = DetectedPoint(sk[r], 0.99f)
        }
        return DetectedPose(points)
    }

    @Test
    fun everySinglePoseMatchesItselfWithBothCameras() {
        for (t in PoseLibrary.all.filter { it.isScorable }) {
            val target = t.layout(width, height, 260f, 620f).first()
            for (back in listOf(false, true)) {
                val r = PoseMatcher.match(t.figures.first(), target, t.framing, fakeDetection(target, back).toScreenPose()!!)
                assertTrue("${t.id} back=$back score=${r.score}", r.matched)
            }
        }
    }

    @Test
    fun wrongArmIsReportedWithThePersonsSide() {
        val t = PoseLibrary.all.first { it.id == "portrait_hands_hips" }
        val target = t.layout(width, height, 260f, 620f).first()
        val wrongSpec = t.figures.first().copy(rightArm = Limb(8f, 4f)) // brazo de la derecha de pantalla caído
        val wrong = t.copy(figures = listOf(wrongSpec)).layout(width, height, 260f, 620f).first()

        val front = PoseMatcher.match(t.figures.first(), target, t.framing, fakeDetection(wrong, backCamera = false).toScreenPose()!!)
        assertFalse(front.matched)
        assertTrue(front.hint, front.hint.contains("derech"))

        val back = PoseMatcher.match(t.figures.first(), target, t.framing, fakeDetection(wrong, backCamera = true).toScreenPose()!!)
        assertFalse(back.matched)
        assertTrue(back.hint, back.hint.contains("izquierd"))
    }

    @Test
    fun tooFarAskToComeCloser() {
        val t = PoseLibrary.all.first { it.id == "fashion_contrapposto" }
        val target = t.layout(width, height, 260f, 620f).first()
        val small = target.transformed(0.5f, Vec2(270f, 600f))
        val r = PoseMatcher.match(t.figures.first(), target, t.framing, fakeDetection(small, false).toScreenPose()!!)
        assertEquals("Acércate a la cámara", r.hint)
    }

    @Test
    fun angleDiffWrapsAround() {
        assertEquals(-20f, angleDiff(170f, -170f), 0.001f)
        assertEquals(20f, angleDiff(-170f, 170f), 0.001f)
        assertEquals(0f, angleDiff(180f, -180f), 0.001f)
    }
}
