package com.n380.scorecounter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
 * Legge lo stile attivo (es. ICONS o SOLID) e dipinge il livello decorativo corrispondente.
 * Se lo stile è SOLID, non disegna nulla, lasciando spazio esclusivamente al colore solido del tema.
 */
@Composable
fun AppBackground(
    style: BackgroundStyle = BackgroundStyle.ICONS
) {
    when (style) {
        BackgroundStyle.ICONS -> PatternedBackground()
        BackgroundStyle.SOLID -> {
            // 🧠 TINTA UNITA: Nessun pattern decorativo viene sovrapposto.
            // Rimane visibile unicamente il colore solido MaterialTheme.colorScheme.background del tema attivo.
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

    // ---> IL MOTORE CASUALE BLOCCATO <---
    // Creiamo una "mappa" bidimensionale fissa. Viene calcolata a caso la prima volta,
    // ma grazie a 'remember' lo schermo non sfarfallerà mai durante la navigazione o l'uso dell'app!
    val randomGrid = remember {
        List(34) { // 34 righe
            List(11) { // 11 colonne
                icons.random() // Sceglie un'icona totalmente a caso per ogni singola cella
            }
        }
    }

    // Un contenitore grande quanto tutto lo schermo
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) { // Mette 15 pixel di vuoto tra un'icona e l'altra in verticale
            for (row in 0..33) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(43.dp), // Mette 43 pixel di vuoto in orizzontale
                    // IL TRUCCO: Sfalsiamo leggermente le righe dispari per fare l'effetto "muro di mattoni" sfalsato
                    modifier = if (row % 2 == 0) Modifier else Modifier.padding(start = 36.dp)
                ) {
                    for (col in 0..10) {
                        val icon = randomGrid[row][col] // Legge l'icona salvata nella mappa casuale

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            // Usiamo il colore del testo, ma con opacità al 5% (0.05f). Ombra elegantissima!
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}
