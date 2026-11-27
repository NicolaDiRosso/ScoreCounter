package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R
import com.n380.scorecounter.model.PlayerRecord
import com.n380.scorecounter.model.ScoreAction
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.launch

// ====================================================================
// DIALOGO CRONOLOGIA PUNTEGGI E ANNULLA ULTIMA MOSSA
// ====================================================================
// Finestra modale Material 3 che presenta l'elenco cronologico di tutte le variazioni
// di punteggio registrate durante la partita in corso, permettendo l'annullamento dell'ultima mossa.
@Composable
fun ScoreHistoryDialog(
    viewModel: MatchViewModel,
    onDismissRequest: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 🧠 STATO CONDIVISO SOLLEVATO (State Hoisting):
    // Solleviamo `selectedTabIndex` nel componente genitore `ScoreHistoryDialog` e lo memorizziamo
    // con `rememberSaveable`. In questo modo, se l'utente si trova sul "Grafico" in Portrait,
    // ruota in Landscape (dove entrambi i pannelli sono visibili insieme) e poi torna in Portrait,
    // la scheda attiva in verticale viene preservata perfettamente.
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // 🧠 GESTIONE ORIENTAMENTO ADATTIVA:
    // In landscape utilizziamo un layout Master-Detail a 2 colonne (ScoreHistoryDialogLandscape)
    // senza selettore a schede: il log scorrevole occupa direttamente la colonna sinistra (38%)
    // e il grafico l'intera colonna destra (62%).
    // In portrait utilizziamo il layout verticale standard con switch Storico/Grafico (ScoreHistoryDialogPortrait).
    if (isLandscape) {
        ScoreHistoryDialogLandscape(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest
        )
    } else {
        ScoreHistoryDialogPortrait(
            viewModel = viewModel,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
            onDismissRequest = onDismissRequest
        )
    }
}

/**
 * LAYOUT ORIZZONTALE (Landscape Split View simultanea: Storico a sinistra 38% + Grafico a destra 62%)
 *
 * Eliminando il selettore a schede in orizzontale, l'utente può consultare contemporaneamente
 * l'elenco delle mosse e l'andamento delle curve, vedendo aggiornarsi entrambi in tempo reale
 * quando preme il pulsante "Annulla".
 */
@Composable
fun ScoreHistoryDialogLandscape(
    viewModel: MatchViewModel,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dialogSnackbarHostState = remember { SnackbarHostState() }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // PANNELLO SINISTRO (~38%): Intestazione in alto, Storico azioni al centro e Pulsanti in basso
                    Column(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Intestazione compatta della cronologia
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            AutoResizedText(
                                text = stringResource(R.string.titolo_cronologia_punteggi),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Elenco scorrevole dello storico direttamente visibile nello spazio centrale
                        ScoreHistoryLogBox(
                            scoreActions = viewModel.scoreActions,
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        // Pulsanti Annulla e Chiudi ancorati in basso a sinistra
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val undoneAction = viewModel.undoLastScoreAction()
                                    if (undoneAction != null) {
                                        coroutineScope.launch {
                                            dialogSnackbarHostState.currentSnackbarData?.dismiss()
                                            val result = dialogSnackbarHostState.showSnackbar(
                                                message = context.getString(R.string.msg_mossa_annullata),
                                                actionLabel = context.getString(R.string.btn_annulla_undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreScoreAction(undoneAction)
                                            }
                                        }
                                    }
                                },
                                enabled = viewModel.scoreActions.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.btn_annulla_mossa),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onDismissRequest()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.btn_chiudi_cronologia),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // PANNELLO DESTRO (~62%): Grafico dell'andamento punti sempre visibile a piena altezza
                    ScoreHistoryChartBox(
                        viewModel = viewModel,
                        contentPadding = 12.dp,
                        cornerRadius = 18.dp,
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                    )
                }

                // Posizionato in basso a destra sopra il grafico per non coprire i tasti Annulla/Chiudi a sinistra
                SnackbarHost(
                    hostState = dialogSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 24.dp)
                )
            }
        }
    }
}

/**
 * LAYOUT VERTICALE (Portrait)
 */
@Composable
fun ScoreHistoryDialogPortrait(
    viewModel: MatchViewModel,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dialogSnackbarHostState = remember { SnackbarHostState() }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (selectedTabIndex == 1) Icons.AutoMirrored.Filled.ShowChart else Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            AutoResizedText(
                                text = stringResource(R.string.titolo_cronologia_punteggi),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = selectedTabIndex == 0,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onTabSelected(0)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                AutoResizedText(
                                    text = stringResource(R.string.tab_log),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            SegmentedButton(
                                selected = selectedTabIndex == 1,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onTabSelected(1)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                AutoResizedText(
                                    text = stringResource(R.string.tab_grafico),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300)) togetherWith
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth },
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        ) + fadeOut(animationSpec = tween(300))
                            } else {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(300)) togetherWith
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> fullWidth },
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        ) + fadeOut(animationSpec = tween(300))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        label = "HistoryLogChartPortraitTransition"
                    ) { tab ->
                        if (tab == 1) {
                            ScoreHistoryChartBox(
                                viewModel = viewModel,
                                contentPadding = 10.dp,
                                cornerRadius = 16.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ScoreHistoryLogBox(
                                scoreActions = viewModel.scoreActions,
                                compact = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val undoneAction = viewModel.undoLastScoreAction()
                                if (undoneAction != null) {
                                    coroutineScope.launch {
                                        dialogSnackbarHostState.currentSnackbarData?.dismiss()
                                        val result = dialogSnackbarHostState.showSnackbar(
                                            message = context.getString(R.string.msg_mossa_annullata),
                                            actionLabel = context.getString(R.string.btn_annulla_undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreScoreAction(undoneAction)
                                        }
                                    }
                                }
                            },
                            enabled = viewModel.scoreActions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_annulla_mossa),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_chiudi_cronologia),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                SnackbarHost(
                    hostState = dialogSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 82.dp)
                )
            }
        }
    }
}

/**
 * Riquadro condiviso (principio DRY) che contiene l'elenco cronologico delle variazioni punteggio
 * oppure lo stato vuoto se non è ancora stata registrata alcuna azione nella partita.
 */
@Composable
private fun ScoreHistoryLogBox(
    scoreActions: List<ScoreAction>,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        if (scoreActions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(if (compact) 40.dp else 56.dp)
                )
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
                Text(
                    text = stringResource(R.string.msg_nessuna_mossa_cronologia),
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 8.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(scoreActions.reversed()) { action ->
                    ScoreActionItemRow(
                        action = action,
                        compact = compact
                    )
                }
            }
        }
    }
}

/**
 * Riquadro condiviso (principio DRY) che adatta lo stato live dei giocatori della partita in corso
 * in `List<PlayerRecord>` e renderizza il componente vettoriale `ScoreChart`.
 */
@Composable
private fun ScoreHistoryChartBox(
    viewModel: MatchViewModel,
    contentPadding: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    // Ricalcola la lista dei record solo quando cambiano i punteggi o il numero di mosse registrate
    val currentPlayersAsRecords = remember(viewModel.players.map { it.score }, viewModel.scoreActions.size) {
        viewModel.players.map { player ->
            PlayerRecord(
                name = player.name,
                score = player.score,
                scoreHistory = player.scoreHistory.toList(),
                fireComboCount = player.fireComboCount,
                color = player.color
            )
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            ScoreChart(
                players = currentPlayersAsRecords,
                durationSeconds = viewModel.matchDurationSeconds,
                isDetailed = true,
                showTimeZoom = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Visualizzazione della singola mossa in formato mini-card giocatore, coerente con lo stile grafico del tavolo da gioco
@Composable
private fun ScoreActionItemRow(
    action: ScoreAction,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val playerColor = Color(action.playerColor)
    val cornerRadius = if (compact) 12.dp else 14.dp

    // Contenitore principale a card con sfondo tonalmente integrato e bordo colorato del giocatore
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = playerColor.copy(alpha = 0.28f)
        ),
        border = BorderStroke(1.dp, playerColor.copy(alpha = 0.50f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicatore verticale di accento sul lato sinistro della card con il colore puro del giocatore
            Box(
                modifier = Modifier
                    .width(if (compact) 5.dp else 6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
                    .background(playerColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (compact) 10.dp else 12.dp,
                        vertical = if (compact) 6.dp else 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sezione sinistra: nome del giocatore e tempo trascorso della partita
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = action.playerName,
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(if (compact) 12.dp else 13.dp)
                        )
                        Text(
                            text = formatTime(action.matchDurationSeconds),
                            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }

                // Sezione destra: badge della variazione di punteggio e totale progressivo
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp)
                ) {
                    val isPositive = action.delta > 0
                    val deltaText = if (isPositive) "+${action.delta}" else action.delta.toString()
                    val deltaColor = if (isPositive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

                    // Badge squadrato ad alto contrasto per garantire leggibilita impeccabile su qualsiasi colore di sfondo
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, deltaColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = deltaText,
                            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = deltaColor,
                            modifier = Modifier.padding(horizontal = if (compact) 7.dp else 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.lbl_punteggio_totale, action.scoreAfter),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}