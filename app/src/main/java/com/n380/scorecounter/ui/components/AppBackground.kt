package com.n380.scorecounter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
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
 */
enum class BackgroundStyle {
    ICONS,
    SOLID,
    GRADIENT,
    DOTS,
    GEOMETRIC,
    WAVES;

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