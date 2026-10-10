package com.n380.scorecounter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
 *    - Pannello Sinistro (weight = 0.36f): Contiene il Titolo, la Card del Campione Assoluto,
 *      il Riepilogo Generale delle partite/tempo ed il pulsante "Chiudi Statistiche" distanziato in basso.
 *    - Pannello Destro (weight = 0.64f): Contiene una Griglia Adattiva a 2-3 colonne (LazyVerticalGrid)
 *      che mostra tutte le schede dei record dettagliati senza icone doppie (gli emoji sono già nei titoli).
 *
 * 2. WINDOW INSETS & HARDWARE CUTOUT (displayCutoutPadding):
 *    Garantisce che il titolo o le schede a sinistra non finiscano mai sotto il foro
 *    o la tacca (punch-hole/notch) della fotocamera frontale dello smartphone.
 *
 * 3. GRIGLIA ADATTIVA E MULTI-DISPOSITIVO (GridCells.Adaptive):
 *    `GridCells.Adaptive(minSize = 220.dp)` organizza le schede dei record in 2 colonne sugli smartphone
 *    e in 3 o 4 colonne sui tablet in modo fluido e dinamico.
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
    // MATEMATICA E CALCOLO DEI RECORD STORICI (STESSA LOGICA DEL PORTRAIT)
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
        .mapNotNull { match -> match.getHistoricalCecchino()?.let { Pair(match, it) } }
        .maxByOrNull { it.second.second }

    val globalCrab = viewModel.history
        .mapNotNull { match -> match.getHistoricalGambero()?.let { Pair(match, it) } }
        .maxByOrNull { it.second.second }

    val globalPhoenix = viewModel.history
        .mapNotNull { match -> match.getHistoricalFenice()?.let { Pair(match, it) } }
        .maxByOrNull { it.second.second }

    // Stati di espansione locale per le card dei record
    var isExpRecord by remember { mutableStateOf(false) }
    var isExpInfinita by remember { mutableStateOf(false) }
    var isExpDittatore by remember { mutableStateOf(false) }
    var isExpPiromane by remember { mutableStateOf(false) }

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
        // 🏛️ PANNELLO DI SINISTRA: TITOLO, CAMPIONE, RIEPILOGO E TASTO CHIUDI
        // ====================================================================
        Column(
            modifier = Modifier
                .weight(0.36f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween // Distanzia armoniosamente il blocco superiore dal tasto Chiudi
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TITOLO DELLA PAGINA
                AutoResizedText(
                    text = stringResource(R.string.titolo_statistiche_partite),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )

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

            // ------------------------------------------------------------
            // PULSANTE × CHIUDI STATISTICHE (Ancorato in Basso con Spaziatura)
            // ------------------------------------------------------------
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
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
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
                AutoResizedText(
                    text = stringResource(R.string.btn_chiudi_statistiche),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
                // 1. RECORD DI PUNTI (La stringa R.string.titolo_record_punti contiene già l'emoji 🔝)
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

                // 2. LA PARTITA INFINITA (La stringa R.string.titolo_partita_infinita contiene già l'emoji ⏳)
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

                // 3. IL DITTATORE (La stringa R.string.titolo_dittatore contiene già l'emoji 👑)
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

                // 4. IL PIROMANE (La stringa R.string.titolo_piromane contiene già l'emoji 🔥)
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

                // 5. MIGLIOR CECCHINO (La stringa R.string.titolo_miglior_cecchino contiene già l'emoji 🎯)
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_miglior_cecchino)
                    ) {
                        if (globalSniper != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalSniper.second.first.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "+${globalSniper.second.second} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = stringResource(R.string.label_in_partita, globalSniper.first.title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 6. IL RE DEI GAMBERI (La stringa R.string.titolo_re_gamberi contiene già l'emoji 🦞)
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_re_gamberi)
                    ) {
                        if (globalCrab != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalCrab.second.first.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "-${globalCrab.second.second} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = stringResource(R.string.label_in_partita, globalCrab.first.title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(stringResource(R.string.msg_nessun_record), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 7. FENICE SUPREMA (La stringa R.string.titolo_fenice_suprema contiene già l'emoji 🦅)
                item {
                    GlobalStatCardClean(
                        title = stringResource(R.string.titolo_fenice_suprema)
                    ) {
                        if (globalPhoenix != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = globalPhoenix.second.first.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "+${globalPhoenix.second.second} pt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = stringResource(R.string.label_in_partita, globalPhoenix.first.title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
 * ====================================================================
 * COMPONENTE DIDATTICO: GlobalStatCardClean
 * ====================================================================
 * Card pulita per mostrare un record singolo senza icone doppiamente inserite.
 * Il titolo contiene già gli emoji del tema (es. 🔝, ⏳, 👑, 🔥, 🎯, 🦞, 🦅).
 * ====================================================================
 */
@Composable
private fun GlobalStatCardClean(
    title: String,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggleExpand != null) Modifier.clickable { onToggleExpand() } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onToggleExpand != null) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
