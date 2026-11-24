package com.n380.scorecounter.ui.components

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.hypot

/**
 * Enumerazione degli stili di sfondo disponibili nell'applicazione.
 * - ICONS: Sfondo con griglia decorativa di icone tematiche in filigrana.
 * - SOLID: Sfondo a tinta unita basato sul colore di background del tema attivo.
 */
enum class BackgroundStyle {
    ICONS,
    SOLID;

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