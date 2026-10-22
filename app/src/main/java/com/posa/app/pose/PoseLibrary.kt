package com.posa.app.pose

/*
 * Biblioteca de poses. Cada figura se define por ángulos (ver Skeleton.kt):
 *   0 = abajo, 90 = derecha de pantalla, -90 = izquierda, 180 = arriba.
 *   torso < 180 inclina el pecho hacia la derecha de la pantalla; > 180 hacia la izquierda.
 *   headTilt negativo inclina la cabeza hacia la derecha de la pantalla.
 *
 * Para agregar una pose nueva, copia una existente y ajusta los ángulos.
 */
object PoseLibrary {

    private val relaxedLeft = Limb(-8f, -4f)
    private val relaxedRight = Limb(8f, 4f)
    private val leftOnHip = Limb(-38f, 40f)
    private val rightOnHip = Limb(38f, -40f)
    private val leftCrossed = Limb(-12f, 85f)
    private val rightCrossed = Limb(12f, -85f)

    val all: List<PoseTemplate> = listOf(
        // ───────────── Retrato (medio cuerpo) ─────────────
        PoseTemplate(
            id = "portrait_three_quarter",
            name = "Tres cuartos",
            category = PoseCategory.PORTRAIT,
            framing = Framing.HALF_BODY,
            figures = listOf(
                FigureSpec(
                    torso = 176f, headTilt = -8f, turn = 0.7f,
                    leftArm = Limb(-6f, -2f),
                    rightArm = Limb(35f, -40f),
                )
            ),
            tips = listOf(
                "Gira el cuerpo unos 45° respecto a la cámara",
                "Adelanta un poco la frente y baja la barbilla",
                "Relaja los hombros y respira",
            ),
        ),
        PoseTemplate(
            id = "portrait_chin",
            name = "Mano en la barbilla",
            category = PoseCategory.PORTRAIT,
            framing = Framing.HALF_BODY,
            figures = listOf(
                FigureSpec(
                    headTilt = 6f, turn = 0.85f,
                    leftArm = Limb(-4f, 80f),
                    rightArm = Limb(-5f, 192f),
                )
            ),
            tips = listOf(
                "Apoya la barbilla suavemente, sin empujar la cara",
                "El otro brazo sostiene el codo",
                "Mira un poco por encima de la cámara",
            ),
        ),
        PoseTemplate(
            id = "portrait_hands_hips",
            name = "Manos en la cintura",
            category = PoseCategory.PORTRAIT,
            framing = Framing.HALF_BODY,
            figures = listOf(FigureSpec(leftArm = leftOnHip, rightArm = rightOnHip)),
            tips = listOf(
                "Separa los codos del cuerpo para marcar la cintura",
                "Pecho abierto y hombros abajo",
                "Sonríe también con la mirada",
            ),
        ),
        PoseTemplate(
            id = "portrait_arms_crossed",
            name = "Brazos cruzados",
            category = PoseCategory.PORTRAIT,
            framing = Framing.HALF_BODY,
            figures = listOf(
                FigureSpec(turn = 0.9f, headTilt = -4f, leftArm = leftCrossed, rightArm = rightCrossed)
            ),
            tips = listOf(
                "Cruza los brazos sin apretarlos",
                "Gira levemente los hombros",
                "Una sonrisa suaviza la pose",
            ),
        ),
        PoseTemplate(
            id = "portrait_hair",
            name = "Mano en el pelo",
            category = PoseCategory.PORTRAIT,
            framing = Framing.HALF_BODY,
            figures = listOf(
                FigureSpec(
                    headTilt = 10f, turn = 0.9f,
                    leftArm = relaxedLeft,
                    rightArm = Limb(150f, 275f),
                )
            ),
            tips = listOf(
                "Lleva la mano al pelo con los dedos relajados",
                "Inclina la cabeza hacia el lado contrario",
                "Codo hacia afuera, no hacia la cámara",
            ),
        ),

        // ───────────── Moda (cuerpo completo) ─────────────
        PoseTemplate(
            id = "fashion_contrapposto",
            name = "Peso en una pierna",
            category = PoseCategory.FASHION,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    torso = 177f, headTilt = 5f,
                    leftArm = Limb(-6f, -2f),
                    rightArm = Limb(14f, -12f),
                    leftLeg = Limb(-2f, 0f),
                    rightLeg = Limb(-4f, 12f),
                )
            ),
            tips = listOf(
                "Carga el peso en una pierna y relaja la otra",
                "Dobla un poco la rodilla libre hacia adentro",
                "Mano en el bolsillo sin hundirla del todo",
            ),
        ),
        PoseTemplate(
            id = "fashion_walk",
            name = "Caminando",
            category = PoseCategory.FASHION,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    leftArm = Limb(-14f, -6f),
                    rightArm = Limb(6f, 14f),
                    leftLeg = Limb(2f, -4f),
                    rightLeg = Limb(8f, 6f),
                )
            ),
            tips = listOf(
                "Camina hacia la cámara cruzando un poco los pies",
                "Activa el modo Auto para captar el momento",
                "Mentón paralelo al suelo y mirada al frente",
            ),
        ),
        PoseTemplate(
            id = "fashion_cross_legs",
            name = "Piernas cruzadas",
            category = PoseCategory.FASHION,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    torso = 183f, headTilt = -4f,
                    leftArm = leftOnHip,
                    rightArm = relaxedRight,
                    leftLeg = Limb(10f, 24f),
                    rightLeg = Limb(0f, -3f),
                )
            ),
            tips = listOf(
                "Cruza una pierna por delante a la altura de los tobillos",
                "Apoya el peso en la pierna de atrás",
                "Una mano en la cadera da forma a la silueta",
            ),
        ),
        PoseTemplate(
            id = "fashion_arms_up",
            name = "Brazos arriba",
            category = PoseCategory.FASHION,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    leftArm = Limb(-150f, -165f),
                    rightArm = Limb(150f, 165f),
                    leftLeg = Limb(-8f, -4f),
                    rightLeg = Limb(8f, 4f),
                )
            ),
            tips = listOf(
                "Ideal para saltos: usa el temporizador o el modo Auto",
                "Estira los brazos por completo",
                "¡Sonrisa grande!",
            ),
        ),
        PoseTemplate(
            id = "fashion_lean",
            name = "Recargado",
            category = PoseCategory.FASHION,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    torso = 172f, headTilt = 6f, turn = 0.85f,
                    leftArm = Limb(-20f, 76f),
                    rightArm = Limb(8f, -88f),
                    leftLeg = Limb(-10f, -10f),
                    rightLeg = Limb(2f, -14f),
                )
            ),
            tips = listOf(
                "Apóyate con el hombro en una pared o poste",
                "Cruza la pierna de afuera sobre la otra",
                "Relaja la cara y mira hacia un lado",
            ),
        ),

        // ───────────── Pareja ─────────────
        PoseTemplate(
            id = "couple_side_hug",
            name = "Abrazo lateral",
            category = PoseCategory.COUPLE,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    x = -0.12f, torso = 176f, headTilt = -10f, turn = 0.85f,
                    leftArm = relaxedLeft, rightArm = Limb(15f, 80f),
                ),
                FigureSpec(
                    x = 0.12f, scale = 0.95f, torso = 184f, headTilt = 10f, turn = 0.85f,
                    leftArm = Limb(-15f, -80f), rightArm = relaxedRight,
                ),
            ),
            tips = listOf(
                "Júntense de lado, cadera con cadera",
                "Inclinen la cabeza el uno hacia el otro",
                "La mano en la cintura de la pareja, sin apretar",
            ),
        ),
        PoseTemplate(
            id = "couple_hold_hands",
            name = "De la mano",
            category = PoseCategory.COUPLE,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    x = -0.2f, headTilt = -6f, turn = 0.45f,
                    leftArm = Limb(10f, 45f), rightArm = Limb(25f, 60f),
                    leftLeg = Limb(-6f, -4f), rightLeg = Limb(3f, 0f),
                ),
                FigureSpec(
                    x = 0.2f, scale = 0.95f, headTilt = 6f, turn = 0.45f,
                    leftArm = Limb(-25f, -60f), rightArm = Limb(-10f, -45f),
                    leftLeg = Limb(-3f, 0f), rightLeg = Limb(6f, 4f),
                ),
            ),
            tips = listOf(
                "Pónganse de perfil, uno frente al otro",
                "Tómense de las manos a la altura de la cintura",
                "Mírense o rían juntos: lo espontáneo sale mejor",
            ),
        ),
        PoseTemplate(
            id = "couple_back_to_back",
            name = "Espalda con espalda",
            category = PoseCategory.COUPLE,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    x = -0.1f, torso = 176f, headTilt = 8f, turn = 0.55f,
                    leftArm = Limb(-16f, 81f), rightArm = Limb(8f, -89f),
                    leftLeg = Limb(-10f, -4f), rightLeg = Limb(-2f, 0f),
                ),
                FigureSpec(
                    x = 0.1f, scale = 0.96f, torso = 184f, headTilt = -8f, turn = 0.55f,
                    leftArm = Limb(-8f, 89f), rightArm = Limb(16f, -81f),
                    leftLeg = Limb(2f, 0f), rightLeg = Limb(10f, 4f),
                ),
            ),
            tips = listOf(
                "Apoyen las espaldas una contra otra",
                "Crucen los brazos y giren la cabeza hacia la cámara",
                "Actitud segura: barbilla un poco arriba",
            ),
        ),

        // ───────────── Grupo ─────────────
        PoseTemplate(
            id = "group_three_staggered",
            name = "Trío escalonado",
            category = PoseCategory.GROUP,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(
                    x = -0.27f, y = -0.01f, scale = 0.95f, turn = 0.75f, headTilt = -4f,
                    leftArm = leftOnHip, rightArm = relaxedRight,
                ),
                FigureSpec(
                    x = 0f, y = 0.03f, scale = 1.04f,
                    leftArm = leftCrossed, rightArm = rightCrossed,
                ),
                FigureSpec(
                    x = 0.27f, y = -0.01f, scale = 0.95f, turn = 0.75f, headTilt = 4f,
                    leftArm = relaxedLeft, rightArm = rightOnHip,
                ),
            ),
            tips = listOf(
                "La persona protagonista o más alta va al centro",
                "Los de los lados giran el cuerpo hacia el centro",
                "Sin huecos: hombros casi tocándose",
                "Si tú también sales, usa el temporizador de 10 s",
            ),
        ),
        PoseTemplate(
            id = "group_four_shoulders",
            name = "Abrazados en fila",
            category = PoseCategory.GROUP,
            framing = Framing.FULL_BODY,
            figures = listOf(
                FigureSpec(x = -0.36f, scale = 0.97f, turn = 0.9f, leftArm = leftOnHip, rightArm = Limb(115f, 92f)),
                FigureSpec(x = -0.12f, scale = 1.0f, turn = 0.9f, leftArm = relaxedLeft, rightArm = Limb(115f, 92f)),
                FigureSpec(x = 0.12f, scale = 0.98f, turn = 0.9f, leftArm = relaxedLeft, rightArm = Limb(115f, 92f)),
                FigureSpec(x = 0.36f, scale = 0.95f, turn = 0.9f, leftArm = relaxedLeft, rightArm = rightOnHip),
            ),
            tips = listOf(
                "Pasen el brazo por los hombros de quien está al lado",
                "Alternar alturas se ve más dinámico",
                "Cuenten hasta tres y rían: usa el temporizador",
            ),
        ),
    )

    fun byCategory(category: PoseCategory) = all.filter { it.category == category }
}
