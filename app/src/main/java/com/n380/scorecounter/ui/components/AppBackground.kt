package com.n380.scorecounter.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Enumerazione degli stili di sfondo disponibili nell'applicazione.
 * - ICONS: Sfondo con griglia decorativa di icone tematiche in filigrana.
 * - SOLID: Sfondo a tinta unita basato sul colore di background del tema attivo.
 * - GRADIENT: Bagliore sfumato radiale basato sui colori dinamici Material You.
 * - DOTS: Trama regolare a puntini stile taccuino segnapunti.
 * - GEOMETRIC: Reticolato diagonale a rombi stile plancia da gioco.
 * - WAVES: Onde sinusoidali morbide e rilassanti sul fondo dello schermo.
 * - ANIMATED_ICONS: Griglia di icone tematiche in lento scorrimento diagonale continuo.
 * - ANIMATED_NUMBERS: Numeri e punteggi in filigrana che fluttuano lentamente verso l'alto.
 */
enum class BackgroundStyle {
    ICONS,
    SOLID,
    GRADIENT,
    DOTS,
    GEOMETRIC,
    WAVES,
    ANIMATED_ICONS,
    ANIMATED_NUMBERS;

    companion object {
        /**
         * Converte un intero letto dal DataStore nel corrispondente BackgroundStyle.
         * Fornisce ICONS come piano di riserva (fallback) in caso di valori non validi.
         */
        fun fromInt(value: Int): BackgroundStyle {
            return entries.getOrNull(value) ?: ICONS
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE PRINCIPALE SFONDO APP (AppBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Questo composable funge da selettore centrale per il rendering dello sfondo.
 * Applica sempre il colore solido di base del tema attivo (chiaro, scuro o Material You)
 * e, se lo stile lo prevede, sovrappone il livello decorativo corrispondente.
 */
@Composable
fun AppBackground(
    style: BackgroundStyle = BackgroundStyle.ICONS,
    modifier: Modifier = Modifier
) {
    // Contenitore radice che garantisce sempre il colore di sfondo del tema attivo (Chiaro o Scuro)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (style) {
            BackgroundStyle.ICONS -> PatternedBackground()
            BackgroundStyle.SOLID -> {
                // 🧠 TINTA UNITA: Nessun pattern decorativo viene sovrapposto.
                // Rimane visibile unicamente il colore solido MaterialTheme.colorScheme.background del tema attivo.
            }
            BackgroundStyle.GRADIENT -> AuroraGradientBackground()
            BackgroundStyle.DOTS -> DotGridBackground()
            BackgroundStyle.GEOMETRIC -> GeometricBoardBackground()
            BackgroundStyle.WAVES -> WavesBackground()
            BackgroundStyle.ANIMATED_ICONS -> AnimatedIconsBackground()
            BackgroundStyle.ANIMATED_NUMBERS -> AnimatedNumbersBackground()
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: SFONDO DECORATIVO GLOBALE A ICONE (PatternedBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Disegna una griglia fitta di icone a tema (giochi, carte, coppe, dadi, stelle)
 * con opacità ridotta al 5%, creando un'elegante filigrana di sfondo a scacchiera sfalsata.
 */
@Composable
fun PatternedBackground() {
    // La lista delle nostre icone a tema (Giochi, Anime, Carte, Coppe, Timer, Medaglie, Dadi, ecc.)
    val icons = listOf(
        Icons.Filled.VideogameAsset,   // Controller Classico
        Icons.Filled.Style,            // Carte
        Icons.Filled.Tv,               // Anime/Schermo
        Icons.Filled.EmojiEvents,      // Coppa del vincitore
        Icons.Filled.Timer,            // Cronometro
        Icons.Filled.WorkspacePremium, // Medaglia/Corona

        // ICONE TEMATICHE ESCLUSIVE PER LO SFONDO
        Icons.Filled.Casino,           // Dadi (Perfetto per i giochi da tavolo!)
        Icons.Filled.SportsEsports,    // Controller Moderno (Per tornei e console)
        Icons.Filled.Extension,        // Pezzo di Puzzle (Per giochi di strategia e logica)
        Icons.Filled.Star,             // Stella (Il classico simbolo dei punti)
        Icons.Filled.FlashOn,          // Fulmine (Richiama la combo "On Fire" e la velocità)
    )

    // Parametri geometrici della griglia e dell'inclinazione diagonale
    val iconSize = 36.dp           // Dimensione di ogni singola icona
    val horizontalSpacing = 43.dp  // Spazio orizzontale tra due icone sulla stessa riga
    val verticalSpacing = 15.dp    // Spazio verticale tra una riga e l'altra
    val diagonalAngle = -40f       // Angolo di inclinazione dell'intera trama (in gradi)

    // Usiamo BoxWithConstraints per leggere le dimensioni reali dello schermo (smartphone o tablet)
    // o del riquadro di anteprima, garantendo una copertura totale senza bordi vuoti.
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Calcoliamo la diagonale del contenitore con un margine extra (+ 120.dp):
        // ruotando un rettangolo di 40°, serve un quadrato grande almeno quanto la sua diagonale
        // per coprire anche tutti e quattro gli angoli dello schermo.
        val diagonalDp = (hypot(maxWidth.value, maxHeight.value) + 120f).dp

        // Calcoliamo dinamicamente quante righe e colonne servono per riempire l'intera diagonale
        val rowStep = iconSize.value + verticalSpacing.value
        val colStep = iconSize.value + horizontalSpacing.value
        val rows = ceil(diagonalDp.value / rowStep).toInt().coerceAtLeast(1)
        val cols = ceil(diagonalDp.value / colStep).toInt().coerceAtLeast(1)

        // ---> IL MOTORE CASUALE BLOCCATO <---
        // Creiamo una "mappa" bidimensionale fissa dimensionata su righe e colonne calcolate.
        // Grazie a 'remember(rows, cols)' la griglia non sfarfalla mai durante l'uso dell'app!
        val randomGrid = remember(rows, cols) {
            List(rows) { // Righe calcolate dinamicamente
                List(cols) { // Colonne calcolate dinamicamente
                    icons.random() // Sceglie un'icona totalmente a caso per ogni singola cella
                }
            }
        }

        // Contenitore quadrato grande quanto la diagonale, ruotato di -40 gradi
        Column(
            verticalArrangement = Arrangement.spacedBy(verticalSpacing), // Mette 15 dp di vuoto tra un'icona e l'altra in verticale
            modifier = Modifier
                .requiredSize(diagonalDp) // Forza la dimensione piena senza farsi tagliare dai bordi dello schermo
                .graphicsLayer {
                    rotationZ = diagonalAngle // Ruota l'intera griglia per creare le linee diagonali
                }
        ) {
            for (row in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing), // Mette 43 dp di vuoto in orizzontale
                    // IL TRUCCO: Sfalsiamo leggermente le righe dispari per fare l'effetto "muro di mattoni" sfalsato
                    modifier = if (row % 2 == 0) Modifier else Modifier.padding(start = 36.dp)
                ) {
                    for (col in 0 until cols) {
                        val icon = randomGrid[row][col] // Legge l'icona salvata nella mappa casuale

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            // Usiamo il colore del testo, ma con opacità al 5% (0.05f). Ombra elegantissima!
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer {
                                    // Controtiriamo ogni singola icona (+40 gradi) per mantenerla perfettamente dritta!
                                    rotationZ = -diagonalAngle
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: SFONDO SFUMATO DINAMICO "AURORA" (AuroraGradientBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Sovrappone due bagliori radiali morbidissimi (uno in alto a sinistra con il colore
 * `primary` del tema e uno in basso a destra con il colore `tertiary`), mantenendo
 * l'opacità bassa (10% - 14%) per preservare il contrasto assoluto delle card.
 */
@Composable
fun AuroraGradientBackground() {
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val tertiaryGlow = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.11f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Bagliore superiore sinistro (colore primario del tema Material You)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow, Color.Transparent),
                center = Offset(x = size.width * 0.15f, y = size.height * 0.12f),
                radius = size.maxDimension * 0.75f
            )
        )

        // Bagliore inferiore destro (colore terziario del tema Material You)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryGlow, Color.Transparent),
                center = Offset(x = size.width * 0.85f, y = size.height * 0.88f),
                radius = size.maxDimension * 0.75f
            )
        )
    }
}

/**
 * ====================================================================
 * COMPONENTE: SFONDO TACCUINO A PUNTINI (DotGridBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Disegna direttamente su Canvas una griglia regolare di piccoli puntini
 * (stile taccuino segnapunti / bullet journal) con opacità al 10%.
 * È leggerissimo per la GPU e si adatta sia allo schermo intero che alle miniature.
 */
@Composable
fun DotGridBackground() {
    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Distanza tra un puntino e il successivo (in pixel convertiti da dp)
        val spacingPx = 22.dp.toPx()
        val radiusPx = 1.6.dp.toPx()

        var y = spacingPx / 2f
        while (y < size.height) {
            var x = spacingPx / 2f
            while (x < size.width) {
                drawCircle(
                    color = dotColor,
                    radius = radiusPx,
                    center = Offset(x, y)
                )
                x += spacingPx
            }
            y += spacingPx
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: SFONDO PLANCIA GEOMETRICA (GeometricBoardBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Crea un reticolato diagonale a rombi sottili ispirato alle plance dei giochi
 * da tavolo strategici, disegnando linee incrociate a 45° con opacità al 6%.
 */
@Composable
fun GeometricBoardBackground() {
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stepPx = 34.dp.toPx()
        val strokeWidthPx = 1.dp.toPx()
        val maxSpan = size.width + size.height

        // Linee diagonali discendenti (da in alto a sinistra verso in basso a destra)
        var offset = -size.height
        while (offset < maxSpan) {
            drawLine(
                color = lineColor,
                start = Offset(offset, 0f),
                end = Offset(offset + size.height, size.height),
                strokeWidth = strokeWidthPx
            )
            offset += stepPx
        }

        // Linee diagonali ascendenti (da in basso a sinistra verso in alto a destra)
        offset = 0f
        while (offset < maxSpan) {
            drawLine(
                color = lineColor,
                start = Offset(offset, 0f),
                end = Offset(offset - size.height, size.height),
                strokeWidth = strokeWidthPx
            )
            offset += stepPx
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: SFONDO ONDE ZEN (WavesBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Disegna tre onde sinusoidali morbide e stratificate nella fascia inferiore
 * e centrale dello schermo, usando il colore primario del tema a bassa opacità.
 */
@Composable
fun WavesBackground() {
    val waveColorPrimary = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val waveColorSecondary = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Funzione interna di supporto per disegnare una singola onda sinusoidale piena
        fun drawWave(baseHeightRatio: Float, amplitudeRatio: Float, phaseShift: Float, color: Color) {
            val path = Path()
            val baseY = height * baseHeightRatio
            val amplitude = height * amplitudeRatio

            path.moveTo(0f, height)
            path.lineTo(0f, baseY)

            val step = (width / 40f).coerceAtLeast(4f)
            var x = 0f
            while (x <= width + step) {
                val angle = (x / width) * (2.0 * PI) + phaseShift
                val y = baseY + (sin(angle) * amplitude).toFloat()
                path.lineTo(x, y)
                x += step
            }

            path.lineTo(width, height)
            path.close()
            drawPath(path = path, color = color)
        }

        // Tre strati sovrapposti con sfasamento per dare profondità fluida
        drawWave(baseHeightRatio = 0.45f, amplitudeRatio = 0.05f, phaseShift = 0.0f, color = waveColorSecondary)
        drawWave(baseHeightRatio = 0.65f, amplitudeRatio = 0.06f, phaseShift = 2.1f, color = waveColorSecondary)
        drawWave(baseHeightRatio = 0.82f, amplitudeRatio = 0.05f, phaseShift = 4.2f, color = waveColorPrimary)
    }
}

/**
 * ====================================================================
 * COMPONENTE: SFONDO ICONE ANIMATE (AnimatedIconsBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Versione animata di [PatternedBackground]: fa scorrere lentamente l'intera
 * griglia di icone in diagonale usando `rememberInfiniteTransition`.
 * IL TRUCCO DEL LOOP INFINITO SENZA SCATTI (SEAMLESS LOOP):
 * Trasliamo la griglia esattamente di un periodo multiplo di 2 righe e 2 colonne
 * (`2 * rowStep` e `2 * colStep`), scegliendo le icone in modo periodico:
 * al 100% dell'animazione ogni icona si trova esattamente al posto della gemella
 * successiva, rendendo il riavvio del ciclo completamente invisibile all'occhio!
 */
@Composable
fun AnimatedIconsBackground() {
    val icons = listOf(
        Icons.Filled.VideogameAsset,
        Icons.Filled.Style,
        Icons.Filled.Tv,
        Icons.Filled.EmojiEvents,
        Icons.Filled.Timer,
        Icons.Filled.WorkspacePremium,
        Icons.Filled.Casino,
        Icons.Filled.SportsEsports,
        Icons.Filled.Extension,
        Icons.Filled.Star,
        Icons.Filled.FlashOn,
    )

    val iconSize = 36.dp
    val horizontalSpacing = 43.dp
    val verticalSpacing = 15.dp
    val diagonalAngle = -40f

    val rowStepDp = iconSize + verticalSpacing
    val colStepDp = iconSize + horizontalSpacing

    val density = LocalDensity.current
    val loopXSpanPx = with(density) { (colStepDp * 2).toPx() }
    val loopYSpanPx = with(density) { (rowStepDp * 2).toPx() }

    // Animazione lineare lenta (18 secondi per attraversare 2 celle)
    val infiniteTransition = rememberInfiniteTransition(label = "AnimatedIconsTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "IconsDriftProgress"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Aggiungiamo un margine generoso (+ 280.dp) per coprire sia la diagonale sia lo scorrimento
        val diagonalDp = (hypot(maxWidth.value, maxHeight.value) + 280f).dp

        val rows = (ceil(diagonalDp.value / rowStepDp.value).toInt() + 2).coerceAtLeast(2)
        val cols = (ceil(diagonalDp.value / colStepDp.value).toInt() + 2).coerceAtLeast(2)

        // Griglia periodica (modulo 2 su righe e colonne) per garantire un loop senza salti
        val periodicGrid = remember(rows, cols) {
            List(rows) { r ->
                List(cols) { c ->
                    val index = ((r % 2) * 5 + (c % 2) * 3 + (r / 2) * 2 + (c / 2)) % icons.size
                    icons[index]
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            modifier = Modifier
                .requiredSize(diagonalDp)
                .graphicsLayer {
                    rotationZ = diagonalAngle
                    translationX = -progress * loopXSpanPx
                    translationY = -progress * loopYSpanPx
                }
        ) {
            for (row in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    modifier = if (row % 2 == 0) Modifier else Modifier.padding(start = 36.dp)
                ) {
                    for (col in 0 until cols) {
                        val icon = periodicGrid[row][col]
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer {
                                    rotationZ = -diagonalAngle
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: SFONDO NUMERI FLUTTUANTI (AnimatedNumbersBackground)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Disegna su Canvas nativo (tramite `nativeCanvas.drawText`) una costellazione
 * di numeri e punteggi (+1, 10, 100, +5, 7, 42, ecc.) che salgono lentamente
 * dal basso verso l'alto con un leggero ondeggiamento orizzontale e rotazione
 * armoniosa. Essendo disegnato interamente in un unico passaggio GPU,
 * non crea oggetti Composable figli e garantisce fluidità totale a 60/120 fps.
 */
@Composable
fun AnimatedNumbersBackground() {
    val textColorArgb = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f).toArgb()

    // Animazione continua da 0 a 1 in 24 secondi per un movimento lento e rilassante
    val infiniteTransition = rememberInfiniteTransition(label = "AnimatedNumbersTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "NumbersFloatProgress"
    )

    // Elenco pre-calcolato di 18 particelle numeriche con posizioni, dimensioni e velocità sfalsate
    val particles = remember {
        listOf(
            NumberParticle("+1", 0.08f, 0.12f, 22f, 1, 0.02f, 0f, -12f),
            NumberParticle("10", 0.24f, 0.35f, 28f, 2, 0.025f, 1.1f, 10f),
            NumberParticle("+5", 0.42f, 0.18f, 20f, 1, 0.018f, 2.3f, -8f),
            NumberParticle("100", 0.62f, 0.45f, 30f, 1, 0.022f, 0.7f, 14f),
            NumberParticle("7", 0.80f, 0.22f, 26f, 2, 0.02f, 3.4f, -15f),
            NumberParticle("42", 0.92f, 0.60f, 24f, 1, 0.015f, 1.9f, 9f),
            NumberParticle("+10", 0.14f, 0.68f, 25f, 2, 0.022f, 4.1f, 12f),
            NumberParticle("21", 0.33f, 0.82f, 27f, 1, 0.02f, 2.8f, -10f),
            NumberParticle("1°", 0.52f, 0.74f, 29f, 2, 0.018f, 0.4f, 8f),
            NumberParticle("+2", 0.71f, 0.88f, 21f, 1, 0.025f, 3.7f, -14f),
            NumberParticle("99", 0.86f, 0.92f, 26f, 2, 0.02f, 1.5f, 11f),
            NumberParticle("0", 0.05f, 0.44f, 24f, 1, 0.016f, 5.0f, -6f),
            NumberParticle("+3", 0.28f, 0.56f, 22f, 1, 0.024f, 2.1f, 15f),
            NumberParticle("50", 0.48f, 0.40f, 26f, 2, 0.02f, 4.6f, -11f),
            NumberParticle("8", 0.66f, 0.10f, 23f, 1, 0.021f, 1.3f, 7f),
            NumberParticle("12", 0.77f, 0.54f, 25f, 1, 0.019f, 3.0f, -9f),
            NumberParticle("+1", 0.90f, 0.30f, 20f, 2, 0.023f, 0.9f, 13f),
            NumberParticle("25", 0.18f, 0.94f, 24f, 1, 0.017f, 4.4f, -13f)
        )
    }

    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val scaleFactor = (w.coerceAtLeast(180f) / 360f).coerceIn(0.55f, 1.35f)
        paint.color = textColorArgb

        particles.forEach { p ->
            // Posizione verticale ciclica (sale dal basso verso l'alto e rientra dal fondo)
            val rawY = (p.startYRatio - progress * p.speedMultiplier) % 1f
            val normalizedY = if (rawY < 0f) rawY + 1f else rawY
            val y = normalizedY * (h + 60.dp.toPx()) - 30.dp.toPx()

            // Leggero ondeggiamento orizzontale armonioso
            val sway = sin(progress * 2.0 * PI * p.speedMultiplier + p.phase).toFloat() * (w * p.swayAmplitudeRatio)
            val x = p.xRatio * w + sway

            // Leggera oscillazione dell'angolo di inclinazione
            val angle = p.baseRotation + cos(progress * 2.0 * PI + p.phase).toFloat() * 6f

            paint.textSize = p.fontSizeSp.sp.toPx() * scaleFactor

            drawContext.canvas.nativeCanvas.apply {
                save()
                translate(x, y)
                rotate(angle)
                drawText(p.label, 0f, 0f, paint)
                restore()
            }
        }
    }
}

/**
 * Modello dati immutabile per ciascun numero fluttuante disegnato su Canvas.
 */
private data class NumberParticle(
    val label: String,
    val xRatio: Float,
    val startYRatio: Float,
    val fontSizeSp: Float,
    val speedMultiplier: Int,
    val swayAmplitudeRatio: Float,
    val phase: Float,
    val baseRotation: Float
)