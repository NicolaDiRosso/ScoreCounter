package com.n380.scorecounter.ui.screens

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
    // Calcolo del punteggio massimo corrente per la corona del leader
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
            // BLOCCO SUPERIORE SCORREVOLE (Titolo, Riquadro Timer/Info e Tools)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TITOLO DELLA SFIDA
                AutoResizedText(
                    text = if (viewModel.matchTitle.isEmpty()) stringResource(R.string.titolo_sfida_default) else viewModel.matchTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp)
                )

                // ------------------------------------------------------------
                // 🎨 RIQUADRO UNIFICATO: CRONOMETRO + TASTO INFO INTEGRATI
                // ------------------------------------------------------------
                // Usiamo secondaryContainer / onSecondaryContainer per garantire
                // contrasto perfetto sia in Tema Chiaro che in Tema Scuro!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // CRONOMETRO DURATA
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatTime(viewModel.matchDurationSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // TASTO INFO INTEGRATO
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShowInfoDialogChange(true)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(R.string.desc_info_icon),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

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
                        onShowResetDialogChange(true)
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
                        viewModel.pauseTimer()
                        onShowEndMatchDialogChange(true)
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
        // 📋 PANNELLO DI DESTRA (0.76f larghezza): GRIGLIA ADATTIVA GIOCATORI
        // ====================================================================
        Card(
            modifier = Modifier
                .weight(0.76f) // 🧠 UX FIX: Allargato al 76% per dare massimo respiro ai nomi
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = viewModel.players,
                    key = { player -> player.name }
                ) { player ->
                    PlayerScoreCard(
                        player = player,
                        isLeader = maxScore > 0 && player.score == maxScore,
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
