package com.n380.scorecounter.ui.screens.landscape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R
import com.n380.scorecounter.model.getHistoricalCecchino
import com.n380.scorecounter.model.getHistoricalFenice
import com.n380.scorecounter.model.getHistoricalGambero
import com.n380.scorecounter.model.getHistoricalDittatore
import com.n380.scorecounter.model.getHistoricalPredatore
import com.n380.scorecounter.model.getHistoricalTestaATesta
import com.n380.scorecounter.model.getHistoricalLumaca
import com.n380.scorecounter.model.getHistoricalDiesel
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.formatDate
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.viewmodel.MatchViewModel

/**
 * ====================================================================
 * SCHERMATA DIDATTICA: GlobalStatsScreenLandscape (Statistiche in Orizzontale)
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE DI JETPACK COMPOSE (REATTIVITÀ ORIZZONTALE):
 *
 * 1. ARCHITETTURA A 2 PANNELLI (Row + Weights):
 *    - Pannello Sinistro (weight = 0.36f): Contiene il Titolo, la Tavola contenitiva SurfaceVariant
 *      con Campione Assoluto e Riepilogo Generale, ed il pulsante "Chiudi Statistiche" distanziato in basso.
 *    - Pannello Destro (weight = 0.64f): Contiene una Griglia Adattiva a 2-3 colonne (LazyVerticalGrid)
 *      che mostra tutte le schede dei record dettagliati senza icone doppie.
 * ====================================================================
 */
@Composable
fun GlobalStatsScreenLandscape(
    viewModel: MatchViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isClosing by remember { mutableStateOf(false) }

    // --------------------------------------------------------------------
    // MATEMATICA E CALCOLO DEI RECORD STORICI
    // --------------------------------------------------------------------
    val totalMatches = viewModel.history.size
    val totalSeconds = viewModel.history.sumOf { it.durationSeconds }
    val winsMap = viewModel.history.groupingBy { it.winnerName }.eachCount()
    val bestPlayer = winsMap.maxByOrNull { it.value }
    val highestScoreRecord = viewModel.history.maxByOrNull { it.winningScore }
    val longestMatch = viewModel.history.filter { it.durationSeconds > 0 }.maxByOrNull { it.durationSeconds }

    val dictatorMatch = viewModel.history
        .filter { it.allPlayers.size >= 2 }
        .maxByOrNull { match -> match.allPlayers[0].score - match.allPlayers[1].score }

    val dictatorMargin = if (dictatorMatch != null) {
        dictatorMatch.allPlayers[0].score - dictatorMatch.allPlayers[1].score
    } else 0

    val allHistoricalPlayers = viewModel.history.flatMap { it.allPlayers }
    val fireStatsMap = allHistoricalPlayers
        .groupBy { it.name }
        .mapValues { entry -> entry.value.sumOf { it.fireComboCount } }
    val topArsonist = fireStatsMap.maxByOrNull { it.value }

    val globalSniper = viewModel.history
        .mapNotNull { match -> match.getHistoricalCecchino()?.let { Triple(match, it.first, it.second) } }
        .maxByOrNull { it.third }

    val globalCrab = viewModel.history
        .mapNotNull { match -> match.getHistoricalGambero()?.let { Triple(match, it.first, it.second) } }
        .maxByOrNull { it.third }

    val globalPhoenix = viewModel.history
        .mapNotNull { match -> match.getHistoricalFenice()?.let { Triple(match, it.first, it.second) } }
        .maxByOrNull { it.third }

    val globalDictator = viewModel.history
        .mapNotNull { match -> match.getHistoricalDittatore()?.let { Triple(match, it.first, it.second) } }
        .maxByOrNull { it.third }

    val globalPredator = viewModel.history
        .mapNotNull { match -> match.getHistoricalPredatore()?.let { Triple(match, it.first, it.second) } }
        .maxByOrNull { it.third }

    val globalNeckAndNeck = viewModel.history
        .mapNotNull { match -> match.getHistoricalTestaATesta()?.let { Triple(match, it.first, it.second) } }
        .maxByOrNull { it.second.score }

    val globalSnail = viewModel.history
        .mapNotNull { match -> match.getHistoricalLumaca()?.let { Triple(match, it.first, it.second) } }
        .minByOrNull { it.third }

    val globalDiesel = viewModel.history
        .mapNotNull { match -> match.getHistoricalDiesel()?.let { Triple(match, it.first, it.second) } }
        .maxByOrNull { it.third }

    // Stati di espansione locale per le card dei record
    var isExpRecord by remember { mutableStateOf(false) }
    var isExpInfinita by remember { mutableStateOf(false) }
    var isExpDittatore by remember { mutableStateOf(false) }
    var isExpPiromane by remember { mutableStateOf(false) }
    var isExpSniper by remember { mutableStateOf(false) }
    var isExpCrab by remember { mutableStateOf(false) }
    var isExpPhoenix by remember { mutableStateOf(false) }
    var isExpDictator by remember { mutableStateOf(false) }
    var isExpPredator by remember { mutableStateOf(false) }
    var isExpNeckAndNeck by remember { mutableStateOf(false) }
    var isExpSnail by remember { mutableStateOf(false) }
    var isExpDiesel by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding() // 📸 Protezione hardware fotocamera frontale
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ====================================================================
        // 🏛️ PANNELLO DI SINISTRA: TITOLO, TAVOLA CONTENITIVA E TASTO CHIUDI
        // ====================================================================
        Column(
            modifier = Modifier
                .weight(0.36f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TITOLO DELLA PAGINA "Statistiche Partite"
            AutoResizedText(
                text = stringResource(R.string.titolo_statistiche_partite),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 2.dp)
            )

            // ------------------------------------------------------------
            // TAVOLA CONTENITIVA (SURFACE VARIANT 24dp): RAGGRUPPA CAMPIONE & RIEPILOGO
            // ------------------------------------------------------------
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ------------------------------------------------------------
                    // CARD 1: CAMPIONE ASSOLUTO (Sfondo Primario + Animazione Shimmer)
                    // ------------------------------------------------------------
                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_campione_land")
                    val translateAnim by infiniteTransition.animateFloat(
                        initialValue = -500f,
                        targetValue = 2000f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 4500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmer_translation_campione_land"
                    )
                    val shimmerColor = MaterialTheme.colorScheme.primary
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            shimmerColor.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        start = Offset(translateAnim, translateAnim),
                        end = Offset(translateAnim + 400f, translateAnim + 400f)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = shimmerBrush)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Medaglia gigante in sottofondo
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .size(85.dp)
                                    .align(Alignment.CenterEnd)
                                    .offset(x = 10.dp)
                            )

                            Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                                AutoResizedText(
                                    text = stringResource(R.string.titolo_campione_assoluto),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                AutoResizedText(
                                    text = bestPlayer?.key ?: stringResource(R.string.nessuno),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                Surface(
                                    modifier = Modifier.padding(top = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.label_vittorie, bestPlayer?.value ?: 0),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ------------------------------------------------------------
                    // CARD 2: RIEPILOGO GENERALE (Partite Giocate e Tempo Totale)
                    // ------------------------------------------------------------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.titolo_riepilogo_generale),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Style,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    AutoResizedText(
                                        text = stringResource(R.string.label_partite_giocate),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                                Text(
                                    text = "$totalMatches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Timer,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    AutoResizedText(
                                        text = stringResource(R.string.label_tempo_giocato),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                                Text(
                                    text = formatTime(totalSeconds),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------
            // PULSANTE × CHIUDI STATISTICHE (Ancorato in Basso con Spaziatura Pulita)
            // ------------------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Button(
                    onClick = {
                        if (!isClosing) {
                            isClosing = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        }
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
                        text = stringResource(R.string.btn_chiudi_statistiche),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ====================================================================
        // 📋 PANNELLO DI DESTRA: GRIGLIA DEI RECORD A 2 COLONNE (SENZA ICONE DOPPIE)
        // ====================================================================
        Card(
            modifier = Modifier
                .weight(0.64f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. RECORD DI PUNTI
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_record_punti),
                        isExpanded = isExpRecord,
                        onToggleExpand = { isExpRecord = !isExpRecord }
                    ) {
                        if (highestScoreRecord != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = highestScoreRecord.winnerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, highestScoreRecord.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (isExpRecord) Int.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${highestScoreRecord.winningScore} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpRecord) {
                                Text(
                                    text = stringResource(R.string.label_data_record, formatDate(highestScoreRecord.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 2. LA PARTITA INFINITA
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_partita_infinita),
                        isExpanded = isExpInfinita,
                        onToggleExpand = { isExpInfinita = !isExpInfinita }
                    ) {
                        if (longestMatch != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = longestMatch.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = if (isExpInfinita) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                                )
                                Text(
                                    text = formatTime(longestMatch.durationSeconds),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = stringResource(R.string.label_vinta_da, longestMatch.winnerName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedVisibility(visible = isExpInfinita) {
                                Text(
                                    text = stringResource(R.string.label_giocata_il, formatDate(longestMatch.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessuna_partita_cronometrata), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 3. IL DITTATORE
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_dittatore),
                        isExpanded = isExpDittatore,
                        onToggleExpand = { isExpDittatore = !isExpDittatore }
                    ) {
                        if (dictatorMatch != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dictatorMatch.winnerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = stringResource(R.string.label_ha_dominato_in, dictatorMatch.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (isExpDittatore) Int.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "+$dictatorMargin pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpDittatore) {
                                Text(
                                    text = stringResource(R.string.label_data_dominio, formatDate(dictatorMatch.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_dominio), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 4. IL PIROMANE
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_piromane),
                        isExpanded = isExpPiromane,
                        onToggleExpand = { isExpPiromane = !isExpPiromane }
                    ) {
                        if (topArsonist != null && topArsonist.value > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topArsonist.key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${topArsonist.value}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpPiromane) {
                                Text(
                                    text = stringResource(R.string.msg_record_storico),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_piromane), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 5. MIGLIOR CECCHINO
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_miglior_cecchino),
                        isExpanded = isExpSniper,
                        onToggleExpand = { isExpSniper = !isExpSniper }
                    ) {
                        if (globalSniper != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalSniper.second.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "+${globalSniper.third} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpSniper) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_miglior_cecchino),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalSniper.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalSniper.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 6. IL RE DEI GAMBERI
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_re_gamberi),
                        isExpanded = isExpCrab,
                        onToggleExpand = { isExpCrab = !isExpCrab }
                    ) {
                        if (globalCrab != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalCrab.second.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "-${globalCrab.third} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            AnimatedVisibility(visible = isExpCrab) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_re_gamberi),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalCrab.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalCrab.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 7. FENICE SUPREMA
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_fenice_suprema),
                        isExpanded = isExpPhoenix,
                        onToggleExpand = { isExpPhoenix = !isExpPhoenix }
                    ) {
                        if (globalPhoenix != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalPhoenix.second.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "+${globalPhoenix.third} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpPhoenix) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_fenice_suprema),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalPhoenix.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalPhoenix.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 8. DITTATORE ASSOLUTO
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_dittatore_assoluto),
                        isExpanded = isExpDictator,
                        onToggleExpand = { isExpDictator = !isExpDictator }
                    ) {
                        if (globalDictator != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalDictator.second.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${globalDictator.third} turni",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpDictator) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_dittatore_assoluto),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalDictator.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalDictator.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 9. PREDATORE ALFA
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_predatore_alfa),
                        isExpanded = isExpPredator,
                        onToggleExpand = { isExpPredator = !isExpPredator }
                    ) {
                        if (globalPredator != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalPredator.second.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${globalPredator.third} sorp.",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpPredator) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_predatore_alfa),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalPredator.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalPredator.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 10. TESTA A TESTA EPICO
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_testa_a_testa_epico),
                        isExpanded = isExpNeckAndNeck,
                        onToggleExpand = { isExpNeckAndNeck = !isExpNeckAndNeck }
                    ) {
                        if (globalNeckAndNeck != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_testa_a_testa_valore, globalNeckAndNeck.second.name, globalNeckAndNeck.third.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${globalNeckAndNeck.second.score} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpNeckAndNeck) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_testa_a_testa_epico),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalNeckAndNeck.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalNeckAndNeck.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 11. LUMACA D'ORO
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_lumaca_d_oro),
                        isExpanded = isExpSnail,
                        onToggleExpand = { isExpSnail = !isExpSnail }
                    ) {
                        if (globalSnail != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalSnail.second.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${globalSnail.third} var.",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpSnail) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_lumaca_d_oro),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalSnail.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalSnail.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 12. MOTORE DIESEL
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_motore_diesel),
                        isExpanded = isExpDiesel,
                        onToggleExpand = { isExpDiesel = !isExpDiesel }
                    ) {
                        if (globalDiesel != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalDiesel.second.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${globalDiesel.third}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = isExpDiesel) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(R.string.desc_motore_diesel),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_in_partita, globalDiesel.first.title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_giocata_il, formatDate(globalDiesel.first.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * COMPONENTE SCHEDA RECORD PULITA
 */
@Composable
private fun GlobalStatCardClean(
    title: String,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggleExpand != null) Modifier.clickable {
                    onToggleExpand()
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (onToggleExpand != null) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
