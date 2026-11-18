package com.n380.scorecounter.ui.screens.landscape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.PlayerScoreCard
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.viewmodel.MatchViewModel

/**
 * ====================================================================
 * SCHERMATA DIDATTICA: CounterScreenLandscape (Segnapunti Orizzontale Rifinito)
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE DI JETPACK COMPOSE (REATTIVITÀ & CONTRASTO):
 *
 * 1. LARGHEZZA SNELLA DEL PANNELLO CONTROLLI (weight = 0.24f):
 *    Invece di occupare il 30% dello schermo con controlli larghi, la colonna sinistra viene snellita al 24%
 *    per lasciare il **76% della larghezza** alla griglia dei giocatori a destra, evitando troncamenti di testo.
 *
 * 2. RIQUADRO UNIFICATO PER TIMER E INFO CON MATERIAL YOU CONTRAST:
 *    - Il cronometro della durata ed il tasto *Info* sono uniti in un **unico riquadro compatto** (`Card`).
 *    - Usando `secondaryContainer` come sfondo e `onSecondaryContainer` come testo/icona, garantiamo
 *      il **100% di contrasto ed accessibilità WCAG** sia in Tema Chiaro che in Tema Scuro!
 *
 * 3. GRIGLIA ADATTIVA E MULTI-DISPOSITIVO (GridCells.Adaptive):
 *    - Smartphone landscape: **2 colonne affiancate** per vedere più giocatori insieme.
 *    - Tablet landscape: **3 o 4 colonne affiancate** automaticamente.
 * ====================================================================
 */
@Composable
fun CounterScreenLandscape(
    viewModel: MatchViewModel,
    onNavigateToResults: () -> Unit,
    onNavigateHome: () -> Unit,
    showResetDialog: Boolean,
    onShowResetDialogChange: (Boolean) -> Unit,
    showEndMatchDialog: Boolean,
    onShowEndMatchDialogChange: (Boolean) -> Unit,
    showInfoDialog: Boolean,
    onShowInfoDialogChange: (Boolean) -> Unit,
    showTimerDialog: Boolean,
    onShowTimerDialogChange: (Boolean) -> Unit,
    showDiceDialog: Boolean,
    onShowDiceDialogChange: (Boolean) -> Unit,
    diceResult: Int,
    onDiceResultChange: (Int) -> Unit,
    diceRollCount: Int,
    onDiceRollCountChange: (Int) -> Unit,
    playerForManualEdit: Player?,
    onPlayerForManualEditChange: (Player?) -> Unit,
    haptic: HapticFeedback,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Calcolo del punteggio rilevante per il leader in base al criterio di vittoria attivo
    val winningScore = if (viewModel.isLowestScoreWins) {
        viewModel.players.minOfOrNull { it.score } ?: 0
    } else {
        viewModel.players.maxOfOrNull { it.score } ?: 0
    }
    val maxScore = viewModel.players.maxOfOrNull { it.score } ?: 0

    Row(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding() // 📸 Protezione hardware fotocamera
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ====================================================================
        // 🏛️ COLONNA DI SINISTRA (0.24f larghezza): TITOLO, TIMER/INFO & AZIONI
        // ====================================================================
        Column(
            modifier = Modifier
                .weight(0.24f) // 🧠 UX FIX: Ridotta larghezza al 24% per allargare la griglia a destra
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // BLOCCO SUPERIORE SCORREVOLE (Titolo, Badge del tempo e Tools)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TITOLO DELLA SFIDA A LARGHEZZA PIENA PER MASSIMO RESPIRO
                AutoResizedText(
                    text = if (viewModel.matchTitle.isEmpty()) stringResource(R.string.titolo_sfida_default) else viewModel.matchTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp)
                )

                if (viewModel.targetScore.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.desc_obbiettivo, viewModel.targetScore),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                // ------------------------------------------------------------
                // CARD STRUMENTI (Timer e Dado)
                // ------------------------------------------------------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // RIGA SUPERIORE DEGLI STRUMENTI: Tasto Info (metà larghezza) + Badge del tempo di gioco (metà larghezza)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TASTO INFO (Larghezza dimezzata con weight(1f))
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onShowInfoDialogChange(true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.desc_info_icon),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // BADGE TEMPO DI GIOCO (Larghezza dimezzata con weight(1f))
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                    )
                                    AutoResizedText(
                                        text = formatTime(viewModel.matchDurationSeconds),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // TASTO TIMER
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShowTimerDialogChange(true)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Timer, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_timer),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // TASTO DADO
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDiceResultChange((1..viewModel.diceSides).random())
                                onDiceRollCountChange(diceRollCount + 1)
                                onShowDiceDialogChange(true)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Casino, null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_dado),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------------
            // AZIONI PARTITA IMPILATE IN BASSO (Azzera & Concludi)
            // ------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TASTO AZZERA
                OutlinedButton(
                    onClick = {
                        if (viewModel.isNavigationLocked || showEndMatchDialog) return@OutlinedButton
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDiceRollCountChange(diceRollCount + 1)
                        if (viewModel.confirmQuickActions) {
                            onShowResetDialogChange(true)
                        } else {
                            viewModel.resetScoresWithUndo()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(18.dp)
                    )
                    AutoResizedText(
                        text = stringResource(R.string.btn_azzera),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // TASTO CONCLUDI / FINE MATCH
                Button(
                    enabled = viewModel.canEndMatch,
                    onClick = {
                        if (viewModel.isNavigationLocked || showResetDialog) return@Button
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (viewModel.confirmQuickActions) {
                            viewModel.pauseTimer()
                            onShowEndMatchDialogChange(true)
                        } else {
                            if (viewModel.lockNavigation()) {
                                viewModel.pauseTimer()
                                viewModel.saveCurrentMatch()
                                onNavigateToResults()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (viewModel.canEndMatch) 6.dp else 0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(18.dp)
                    )
                    AutoResizedText(
                        text = stringResource(R.string.btn_fine_match),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ====================================================================
        // 📋 PANNELLO DI DESTRA (0.76f larghezza): TAVOLO RESPONSIVO ADATTIVO
        // ====================================================================
        Card(
            modifier = Modifier
                .weight(0.76f) // 🧠 Allargato al 76% per dare massimo respiro e armonia al tavolo
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            // 🧠 LOGICA DIDATTICA (Ordinamento Dinamico Classifica):
            // Verifichiamo se l'impostazione "Ordinamento dinamico classifica" è attiva:
            // 1. Ordine discendente per punteggio (compareByDescending { it.score })
            // 2. A parità di punteggio, l'ultimo ad aver segnato sale in cima (thenByDescending { it.lastScoreUpdateTime })
            val displayedPlayers = if (viewModel.dynamicLeaderboardSorting) {
                viewModel.players.sortedWith(
                    if (viewModel.isLowestScoreWins) {
                        compareBy<Player> { it.score }
                            .thenByDescending { it.lastScoreUpdateTime }
                    } else {
                        compareByDescending<Player> { it.score }
                            .thenByDescending { it.lastScoreUpdateTime }
                    }
                )
            } else {
                viewModel.players
            }

            val tablePadding = 12.dp // Margine perimetrale uniforme sui 4 lati del tavolo da gioco
            val spacingBetweenCards = 10.dp // Spaziatura uniforme tra le varie card e righe

            // 🧠 ARCHITETTURA RESPONSIVA ADATTIVA (BoxWithConstraints):
            // Misuriamo l'altezza e la larghezza reali a disposizione all'interno del tavolo.
            // Calcoliamo matematicamente la suddivisione delle righe e delle colonne per riempire
            // l'intero spazio verticale disponibile tramite Modifier.weight(1f), eliminando qualsiasi
            // vuoto asimmetrico inferiore ed adattando la scala dei pulsanti e del punteggio.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(tablePadding)
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight
                val playerCount = displayedPlayers.size

                // 🧠 RILEVAZIONE SCHERMO AMPIO (Tablet o Foldable aperto in orizzontale):
                // Se la larghezza a disposizione del tavolo è >= 800.dp, possiamo sfruttare 3 o 4 colonne.
                val isWideDisplay = availableWidth >= 800.dp

                // 🧠 CALCOLO DINAMICO DEL NUMERO DI COLONNE:
                // - 1 giocatore: 1 colonna singola che riempie il tavolo
                // - Smartphone landscape standard: 2 colonne affiancate bilanciate
                // - Tablet ampi landscape: 3 colonne (per 5-6 giocatori) o 4 colonne (per 7-8 giocatori)
                val numColumns = when {
                    playerCount <= 1 -> 1
                    isWideDisplay && playerCount in 5..6 -> 3
                    isWideDisplay && playerCount in 7..8 -> 4
                    else -> 2
                }

                // 🧠 CALCOLO DEL NUMERO DI RIGHE E DELL'ALTEZZA DISPONIBILE PER CIASCUNA RIGA:
                val numRows = (playerCount + numColumns - 1) / numColumns
                val totalSpacing = spacingBetweenCards * (numRows - 1).coerceAtLeast(0)
                val rowHeight = if (numRows > 0) (availableHeight - totalSpacing) / numRows else availableHeight

                // 🧠 SOGLIA MODALITÀ ESPANSA vs COMPATTA (Altezza Minima Ergonomica):
                // Una card espansa con nome in alto e controlli gamepad in basso necessita di almeno 110.dp di altezza.
                // - rowHeight >= 110.dp (1-4 giocatori su smartphone, o fino a 8 su tablet): Modalità Espansa (Tier 2 o Tier 3)
                // - rowHeight < 110.dp (5-8 giocatori su smartphone): Modalità Compatta che riempie l'altezza assegnata
                val isExpandedMode = rowHeight >= 110.dp

                // 🧠 VERIFICA ADATTAMENTO SENZA SCORRIMENTO (Zero Vuoti Inferiori):
                // Fino a 8 giocatori (o con altezza riga >= 46.dp), distribuiamo lo spazio con weight(1f).
                // Con 9 o più giocatori (o schermi minuscoli), attiviamo la griglia con scorrimento fluido.
                val fitsWithoutScroll = playerCount in 1..8 && rowHeight >= 46.dp

                if (fitsWithoutScroll) {
                    // 🌟 DISTRIBUZIONE UNIFORME DELLO SPAZIO (ZERO VUOTI INFERIORI):
                    // Suddividiamo i giocatori in blocchi di righe (chunked).
                    // Assegnando Modifier.weight(1f) a ogni riga, queste occupano esattamente
                    // tutta l'altezza del tavolo, espandendosi proporzionalmente ed eliminando il vuoto inferiore!
                    val playerRows = displayedPlayers.chunked(numColumns)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                    ) {
                        playerRows.forEach { rowPlayers ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                            ) {
                                rowPlayers.forEach { player ->
                                    key(player.name) {
                                        val isLeader = if (viewModel.isLowestScoreWins) player.score == winningScore else (winningScore > 0 && player.score == winningScore)
                                        PlayerScoreCard(
                                            player = player,
                                            isLeader = isLeader,
                                            positiveStep = viewModel.longPressPositiveStep,
                                            negativeStep = viewModel.longPressNegativeStep,
                                            totalPlayers = playerCount,
                                            isExpandedMode = isExpandedMode,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            onScoreChange = { amount ->
                                                viewModel.updatePlayerScore(player, amount)
                                            },
                                            onScoreClick = {
                                                onPlayerForManualEditChange(player)
                                            }
                                        )
                                    }
                                }

                                // 🧠 BILANCIAMENTO VISIVO PER RIGHE INCOMPLETE:
                                // Se l'ultima riga ha meno partecipanti rispetto al numero di colonne (es. 3 giocatori in 2 colonne),
                                // inseriamo uno o più Spacer con weight(1f) per mantenere le colonne simmetriche
                                // evitando che l'ultima card diventi sproporzionatamente larga.
                                repeat(numColumns - rowPlayers.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // 📊 SCORRIMENTO DINAMICO AD ALTA DENSITÀ (9+ GIOCATORI):
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 220.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(spacingBetweenCards),
                        verticalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                    ) {
                        items(
                            items = displayedPlayers,
                            key = { player -> player.name }
                        ) { player ->
                            val isLeader = if (viewModel.isLowestScoreWins) player.score == winningScore else (winningScore > 0 && player.score == winningScore)
                            PlayerScoreCard(
                                player = player,
                                isLeader = isLeader,
                                positiveStep = viewModel.longPressPositiveStep,
                                negativeStep = viewModel.longPressNegativeStep,
                                totalPlayers = displayedPlayers.size,
                                isExpandedMode = false,
                                modifier = Modifier.fillMaxWidth(),
                                onScoreChange = { amount ->
                                    viewModel.updatePlayerScore(player, amount)
                                },
                                onScoreClick = {
                                    onPlayerForManualEditChange(player)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
