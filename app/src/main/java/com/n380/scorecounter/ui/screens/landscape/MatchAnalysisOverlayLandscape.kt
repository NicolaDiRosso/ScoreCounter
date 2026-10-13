package com.n380.scorecounter.ui.screens.landscape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R
import com.n380.scorecounter.model.MatchRecord
import com.n380.scorecounter.model.getHistoricalCecchino
import com.n380.scorecounter.model.getHistoricalFenice
import com.n380.scorecounter.model.getHistoricalGambero
import com.n380.scorecounter.model.getHistoricalInarrestabile
import com.n380.scorecounter.model.getHistoricalDittatore
import com.n380.scorecounter.model.getHistoricalPredatore
import com.n380.scorecounter.model.getHistoricalTestaATesta
import com.n380.scorecounter.model.getHistoricalLumaca
import com.n380.scorecounter.model.getHistoricalDiesel
import com.n380.scorecounter.ui.components.*

/**
 * ====================================================================
 * COMPONENTE OVERLAY ANALISI PARTITA ORIZZONTALE (MatchAnalysisOverlayLandscape.kt)
 * ====================================================================
 * Design System a 2 Colonne Asimmetriche:
 * - Sinistra (~40%): Intestazione match, Carte dei Premi della Partita + Tasto Ancorato "Chiudi Analisi".
 * - Destra (~60%): Grafico a linee a TUTTA ALTEZZA con cronologia completa.
 * ====================================================================
 */
@Composable
fun MatchAnalysisOverlayLandscape(
    match: MatchRecord,
    showAwardsInfoDialog: Boolean,
    onShowAwardsInfoDialogChange: (Boolean) -> Unit,
    showChartInfoDialog: Boolean,
    onShowChartInfoDialogChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // CALCOLO PREMI STORICI
    val storiciCecchino = remember(match) { match.getHistoricalCecchino() }
    val storiciInarrestabile = remember(match) { match.getHistoricalInarrestabile() }
    val storiciGambero = remember(match) { match.getHistoricalGambero() }
    val storiciFenice = remember(match) { match.getHistoricalFenice() }
    val storiciDittatore = remember(match) { match.getHistoricalDittatore() }
    val storiciPredatore = remember(match) { match.getHistoricalPredatore() }
    val storiciTestaATesta = remember(match) { match.getHistoricalTestaATesta() }
    val storiciLumaca = remember(match) { match.getHistoricalLumaca() }
    val storiciDiesel = remember(match) { match.getHistoricalDiesel() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            PatternedBackground()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // 🧠 PROTEZIONE STATUS BAR: Evita sovrapposizioni con l'orologio di sistema
                    .displayCutoutPadding() // 🧠 PROTEZIONE NOTCH: Evita che fotocamere fisiche o fori coprano la UI
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ====================================================================
                // COLONNA SINISTRA (40% LARGHEZZA): PREMI PARTITA & TASTO CHIUDI ANCORATO
                // ====================================================================
                Column(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // TITOLO DELLA PAGINA "Analisi Partita" (In alto a sinistra, sopra e fuori la prima colonna)
                    // 🧠 STILE UNIFORME: Usiamo 'headlineMedium' ed 'AutoResizedText' per rispecchiare esattamente
                    // la dimensione e la prominenza visiva del titolo 'Storico Sfide' in HomeScreenLandscape.kt!
                    AutoResizedText(
                        text = stringResource(R.string.titolo_analisi_partita),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp, start = 2.dp)
                    )

                    // TAVOLO PREMI E HEADER MATCH
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
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Header Titolo Match e Vincitore
                            Column(horizontalAlignment = Alignment.Start) {
                                AutoResizedText(
                                    text = match.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.label_vinta_da_con_punti, match.winnerName, match.winningScore),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            // Sezione Premi Partita
                            if (storiciCecchino != null || storiciInarrestabile != null || storiciGambero != null || storiciFenice != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
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
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Info, stringResource(R.string.desc_info_premi), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // CARTE PREMI STORICI
                                storiciCecchino?.let { (player, punti) ->
                                    AwardCard(
                                        icon = "🎯",
                                        title = stringResource(R.string.premio_cecchino),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_punti_positivi, punti)
                                    )
                                }

                                storiciInarrestabile?.let { (player, combo) ->
                                    AwardCard(
                                        icon = "🔥",
                                        title = stringResource(R.string.premio_inarrestabile),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_combo, combo)
                                    )
                                }

                                storiciGambero?.let { (player, punti) ->
                                    AwardCard(
                                        icon = "🦞",
                                        title = stringResource(R.string.premio_gambero),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_punti_negativi, punti)
                                    )
                                }

                                storiciFenice?.let { (player, punti) ->
                                    AwardCard(
                                        icon = "🦅",
                                        title = stringResource(R.string.premio_fenice),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_punti_positivi, punti)
                                    )
                                }

                                storiciDittatore?.let { (player, turni) ->
                                    AwardCard(
                                        icon = "👑",
                                        title = stringResource(R.string.premio_dittatore),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_turni, turni)
                                    )
                                }

                                storiciPredatore?.let { (player, sorpassi) ->
                                    AwardCard(
                                        icon = "🐺",
                                        title = stringResource(R.string.premio_predatore),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_sorpassi, sorpassi)
                                    )
                                }

                                storiciTestaATesta?.let { (p1, p2) ->
                                    AwardCard(
                                        icon = "🤝",
                                        title = stringResource(R.string.premio_testa_a_testa),
                                        playerName = stringResource(R.string.label_testa_a_testa_valore, p1.name, p2.name),
                                        playerColorInt = p1.color,
                                        valueText = "" // Nessun valore extra numerico da mostrare
                                    )
                                }

                                storiciLumaca?.let { (player, variazioni) ->
                                    AwardCard(
                                        icon = "🐌",
                                        title = stringResource(R.string.premio_lumaca),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_variazioni, variazioni)
                                    )
                                }

                                storiciDiesel?.let { (player, percentuale) ->
                                    AwardCard(
                                        icon = "🚂",
                                        title = stringResource(R.string.premio_diesel),
                                        playerName = player.name,
                                        playerColorInt = player.color,
                                        valueText = stringResource(R.string.label_percentuale, percentuale)
                                    )
                                }
                            }

                            // Footer Durata
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Durata",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                AutoResizedText(
                                    text = stringResource(R.string.label_durata_totale, formatTime(match.durationSeconds)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    // TASTO CHIUDI ANALISI ANCORATO IN BASSO A SINISTRA
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.desc_chiudi_icon),
                                modifier = Modifier.padding(end = 6.dp).size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_chiudi_analisi),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ====================================================================
                // COLONNA DESTRA (60% LARGHEZZA): GRAFICO SCORECHART A TUTTA ALTEZZA
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header Sezione Grafico
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShowChart,
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
                                    onShowChartInfoDialogChange(true)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = stringResource(R.string.desc_info_grafico),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // GRAFICO A TUTTA ALTEZZA
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            ScoreChart(
                                players = match.allPlayers,
                                isDetailed = true,
                                durationSeconds = match.durationSeconds,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }

            // POPUP GUIDA GRAFICO (Standardized Surface Cards)
            if (showChartInfoDialog) {
                Dialog(
                    onDismissRequest = { },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .fillMaxHeight(0.90f),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.titolo_guida_grafico),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.titolo_guida_punti), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.msg_guida_punti), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                    }
                                }
                                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.titolo_guida_tempo), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.msg_guida_tempo), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                    }
                                }
                                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.titolo_guida_pallini), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.msg_guida_pallini), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                    }
                                }
                                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(stringResource(R.string.titolo_guida_sorpassi), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.msg_guida_sorpassi), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Justify)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onShowChartInfoDialogChange(false)
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

            // POPUP GUIDA PREMI (Standardized Surface Cards)
            if (showAwardsInfoDialog) {
                AwardsGuideDialogLandscape(
                    onDismissRequest = {
                        onShowAwardsInfoDialogChange(false)
                    }
                )
            }
        }
    }
}
