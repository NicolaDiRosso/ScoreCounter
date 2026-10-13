package com.n380.scorecounter.ui.screens

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.drawToBitmap
import com.n380.scorecounter.R
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.model.PlayerRecord
import com.n380.scorecounter.ui.components.*
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.launch

/**
 * ====================================================================
 * SCHERMATA RISULTATI ORIZZONTALE (ResultsScreenLandscape.kt)
 * ====================================================================
 * Design System a 2 Colonne Asimmetriche:
 * - Sinistra (~35%): Classifica ordinata dei giocatori + Dock con Tasto Condividi e Torna alla Home.
 * - Destra (~65%): Grafico dell'andamento punteggi e griglia dei Premi della Partita.
 * ====================================================================
 */
@Composable
fun ResultsScreenLandscape(
    viewModel: MatchViewModel,
    rankedPlayers: List<Player>,
    cecchinoStat: Pair<Player, Int>?,
    inarrestabileStat: Pair<Player, Int>?,
    gamberoStat: Pair<Player, Int>?,
    feniceStat: Pair<Player, Int>?,
    showAwardsInfoDialog: Boolean,
    onShowAwardsInfoDialogChange: (Boolean) -> Unit,
    isClosing: Boolean,
    onIsClosingChange: (Boolean) -> Unit,
    showConfetti: Boolean,
    onShowConfettiChange: (Boolean) -> Unit,
    onNavigateHome: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 📸 RIFERIMENTO VIEW PER CONDIVISIONE BITMAP
    var chartView by remember { mutableStateOf<View?>(null) }
    var showChartInfoDialog by remember { mutableStateOf(false) }

    val fallbackTitle = stringResource(R.string.sfida_senza_nome)

    Box(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // 🧠 PROTEZIONE STATUS BAR: Impedisce che l'orologio e le notifiche coprano la UI
                .displayCutoutPadding() // 🧠 PROTEZIONE NOTCH: Evita che fotocamere fisiche o fori coprano la UI
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ====================================================================
            // COLONNA SINISTRA (40% LARGHEZZA): CLASSIFICA & BOTTONI D'AZIONE
            // ====================================================================
            Column(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // INTESTAZIONE E CLASSIFICA GIOCATORI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Titolo Match e Tempo
                        AutoResizedText(
                            text = viewModel.matchTitle.ifEmpty { stringResource(R.string.titolo_sfida_default) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        if (viewModel.matchDurationSeconds > 0) {
                            AutoResizedText(
                                text = stringResource(R.string.label_tempo_gioco, formatTime(viewModel.matchDurationSeconds)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Lista Classifica Scorrevole
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rankedPlayers.forEachIndexed { index, player ->
                                PlayerResultCard(
                                    player = player,
                                    position = index + 1
                                )
                            }
                        }
                    }
                }

                // DOCK BOTTONI D'AZIONE ANCORATO IN BASSO A SINISTRA
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TASTO CONDIVIDI (20% W)
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val finalTitle = viewModel.matchTitle.ifEmpty { fallbackTitle }
                                val playersData = rankedPlayers.map { Pair(it.name, it.score) }

                                val mappedCecchino = cecchinoStat?.let { Pair(it.first.name, it.second) }
                                val mappedInarrestabile = inarrestabileStat?.let { Pair(it.first.name, it.second) }
                                val mappedGambero = gamberoStat?.let { Pair(it.first.name, it.second) }
                                val mappedFenice = feniceStat?.let { Pair(it.first.name, it.second) }

                                val shareText = buildMatchShareText(
                                    context = context,
                                    title = finalTitle,
                                    durationSeconds = viewModel.matchDurationSeconds,
                                    timestamp = System.currentTimeMillis(),
                                    rankedPlayersData = playersData,
                                    cecchinoData = mappedCecchino,
                                    inarrestabileData = mappedInarrestabile,
                                    gamberoData = mappedGambero,
                                    feniceData = mappedFenice
                                )

                                coroutineScope.launch {
                                    try {
                                        val bitmap = chartView?.drawToBitmap()
                                        if (bitmap != null) {
                                            launchShareIntentWithImage(context, shareText, bitmap)
                                        } else {
                                            launchShareIntent(context, shareText)
                                        }
                                    } catch (e: Exception) {
                                        launchShareIntent(context, shareText)
                                    }
                                }
                            },
                            modifier = Modifier.weight(0.20f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.Share, stringResource(R.string.desc_condividi), modifier = Modifier.size(24.dp))
                        }

                        // TASTO TORNA ALLA HOME (80% W)
                        Button(
                            onClick = {
                                if (!isClosing) {
                                    onIsClosingChange(true)
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onNavigateHome()
                                }
                            },
                            modifier = Modifier.weight(0.80f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Home, null, modifier = Modifier.padding(end = 6.dp).size(22.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_torna_home),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ====================================================================
            // COLONNA DESTRA (60% LARGHEZZA): GRAFICO & PREMI DELLA PARTITA
            // ====================================================================
            Card(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // INTESTAZIONE GRAFICO + ICONA INFO
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AutoResizedText(
                            text = stringResource(R.string.titolo_andamento_punteggi),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showChartInfoDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Info, stringResource(R.string.desc_info_grafico), tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    val recordsForChart = viewModel.players.map {
                        PlayerRecord(it.name, it.score, it.scoreHistory.toList(), it.fireComboCount, it.color)
                    }

                    // CONTAINER DEL GRAFICO CON FOTOCAMERA BITMAP
                    Card(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ComposeView(ctx).apply {
                                    chartView = this
                                    setContent {
                                        ScoreChart(players = recordsForChart, modifier = Modifier.fillMaxSize().padding(12.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { view ->
                                (view as ComposeView).setContent {
                                    ScoreChart(players = recordsForChart, modifier = Modifier.fillMaxSize().padding(12.dp))
                                }
                            }
                        )
                    }

                    // AREA PREMI PARTITA
                    if (cecchinoStat != null || inarrestabileStat != null || gamberoStat != null || feniceStat != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_premi_partita),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onShowAwardsInfoDialogChange(true)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Info, stringResource(R.string.desc_info_premi), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // GRIGLIA PREMI 2 COLONNE
                        val activeAwards = listOfNotNull(
                            cecchinoStat?.let { Pair("cecchino", it) },
                            inarrestabileStat?.let { Pair("inarrestabile", it) },
                            gamberoStat?.let { Pair("gambero", it) },
                            feniceStat?.let { Pair("fenice", it) }
                        )

                        activeAwards.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { award ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        when (award.first) {
                                            "cecchino" -> AwardCard(
                                                icon = "🎯",
                                                title = stringResource(R.string.premio_cecchino),
                                                playerName = award.second.first.name,
                                                playerColorInt = award.second.first.color,
                                                valueText = stringResource(R.string.label_punti_positivi, award.second.second)
                                            )
                                            "inarrestabile" -> AwardCard(
                                                icon = "🔥",
                                                title = stringResource(R.string.premio_inarrestabile),
                                                playerName = award.second.first.name,
                                                playerColorInt = award.second.first.color,
                                                valueText = stringResource(R.string.label_combo, award.second.second)
                                            )
                                            "gambero" -> AwardCard(
                                                icon = "🦞",
                                                title = stringResource(R.string.premio_gambero),
                                                playerName = award.second.first.name,
                                                playerColorInt = award.second.first.color,
                                                valueText = stringResource(R.string.label_punti_negativi, award.second.second)
                                            )
                                            "fenice" -> AwardCard(
                                                icon = "🦅",
                                                title = stringResource(R.string.premio_fenice),
                                                playerName = award.second.first.name,
                                                playerColorInt = award.second.first.color,
                                                valueText = stringResource(R.string.label_punti_positivi, award.second.second)
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ESPLOSIONE CORIANDOLI
        if (showConfetti) {
            ConfettiExplosion(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.error
                ),
                onAnimationFinished = { onShowConfettiChange(false) }
            )
        }

        // POPUP INFORMATIVO SULLA LETTURA DEL GRAFICO (Standardized Surface Cards)
        if (showChartInfoDialog) {
            Dialog(
                onDismissRequest = { }, // 🧠 BLOCCO CHIUSURA ACCIDENTALE
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .fillMaxHeight(0.90f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 12.dp,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.titolo_guida_grafico),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_guida_punti), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_guida_punti), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_guida_tempo), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_guida_tempo), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_guida_pallini), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_guida_pallini), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_guida_sorpassi), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_guida_sorpassi), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                showChartInfoDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            AutoResizedText(stringResource(R.string.btn_ho_capito), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        // POPUP INFORMATIVO SUI PREMI (Standardized Surface Cards)
        if (showAwardsInfoDialog) {
            Dialog(
                onDismissRequest = { }, // 🧠 BLOCCO CHIUSURA ACCIDENTALE
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .fillMaxHeight(0.90f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 12.dp,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.titolo_guida_premi),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_cecchino_guida), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_cecchino), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_inarrestabile_guida), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_inarrestabile), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_gambero_guida), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_gambero), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.titolo_fenice_guida), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.msg_fenice), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onShowAwardsInfoDialogChange(false)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            AutoResizedText(stringResource(R.string.btn_ho_capito), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
