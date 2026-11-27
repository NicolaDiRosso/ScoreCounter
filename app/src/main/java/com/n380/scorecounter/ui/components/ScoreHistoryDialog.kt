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
    // con `rememberSaveable`. In questo modo, se l'utente si trova sul "Grafico" in Portrait e ruota
    // il dispositivo in Landscape (o viceversa), la scheda attiva viene preservata perfettamente.
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // 🧠 GESTIONE ORIENTAMENTO ADATTIVA:
    // In landscape utilizziamo un layout Master-Detail a 2 colonne (ScoreHistoryDialogLandscape)
    // per garantire che il grafico e il log non vengano mai compressi o tagliati.
    // In portrait utilizziamo il layout verticale standard (ScoreHistoryDialogPortrait).
    if (isLandscape) {
        ScoreHistoryDialogLandscape(
            viewModel = viewModel,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
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
 * LAYOUT ORIZZONTALE (Landscape Master-Detail Split Dialog per Cronologia & Grafico)
 */
@Composable
fun ScoreHistoryDialogLandscape(
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
                    // PANNELLO SINISTRO (~38%): Intestazione, Selettore tab e Pulsanti azione in basso
                    Column(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        style = MaterialTheme.typography.labelMedium
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
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

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
                                    .height(48.dp),
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
                                    .height(48.dp),
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

                    // PANNELLO DESTRO (~62%): Contenitore animato per Log o Grafico a piena altezza
                    Box(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                    ) {
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
                            modifier = Modifier.fillMaxSize(),
                            label = "HistoryLogChartLandscapeTransition"
                        ) { tab ->
                            if (tab == 1) {
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
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
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
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    if (viewModel.scoreActions.isEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = stringResource(R.string.msg_nessuna_mossa_cronologia),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(bottom = 8.dp)
                                        ) {
                                            items(viewModel.scoreActions.reversed()) { action ->
                                                ScoreActionItemRow(action = action)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = dialogSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
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
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
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
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                if (viewModel.scoreActions.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(R.string.msg_nessuna_mossa_cronologia),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        items(viewModel.scoreActions.reversed()) { action ->
                                            ScoreActionItemRow(action = action)
                                        }
                                    }
                                }
                            }
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

// Visualizzazione della singola mossa in formato mini-card giocatore, coerente con lo stile grafico del tavolo da gioco
@Composable
private fun ScoreActionItemRow(
    action: ScoreAction,
    modifier: Modifier = Modifier
) {
    val playerColor = Color(action.playerColor)
    val cornerRadius = 14.dp

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
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
                    .background(playerColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sezione sinistra: nome del giocatore e tempo trascorso della partita
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = action.playerName,
                        style = MaterialTheme.typography.titleMedium,
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
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatTime(action.matchDurationSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }

                // Sezione destra: badge della variazione di punteggio e totale progressivo
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
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
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = deltaColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
