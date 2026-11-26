package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R
import com.n380.scorecounter.model.HeadToHeadStats
import com.n380.scorecounter.model.VsAwardType
import com.n380.scorecounter.model.VsPlayerOption
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.formatTime
import java.util.Locale

/**
 * ====================================================================
 * VISTA CONFRONTO DIRETTO "VS" (PORTRAIT + COMPONENTI MODULARI CONDIVISI)
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE DI JETPACK COMPOSE:
 *
 * 1. RIUTILIZZO DEI COMPONENTI (Principio DRY - Don't Repeat Yourself):
 *    Le card `VsPlayerSelectorCard`, `VsScoreboardCard`, `VsDualMetricCard`,
 *    `VsBiggestMarginCard` e `VsAwardsComparisonCard` sono definite come
 *    funzioni `@Composable` pubbliche e indipendenti dal contenitore.
 *    In Portrait vengono impilate in una `LazyColumn` singola; in Landscape
 *    (`VsComparisonContentLandscape.kt`) vengono ridistribuite su 2 pannelli
 *    senza duplicare nemmeno una riga di codice grafico!
 *
 * 2. PREVENZIONE CONFRONTO CON SE STESSI (Smart Swap):
 *    Se nel menu a tendina del Giocatore 1 scegli il giocatore già selezionato
 *    come Giocatore 2 (o viceversa), i due sfidanti si scambiano automaticamente
 *    di posto, evitando l'errore logico "Nicola vs Nicola".
 * ====================================================================
 */
@Composable
fun VsComparisonContent(
    availablePlayers: List<VsPlayerOption>,
    selectedPlayer1: String?,
    onPlayer1Selected: (String) -> Unit,
    selectedPlayer2: String?,
    onPlayer2Selected: (String) -> Unit,
    stats: HeadToHeadStats?,
    modifier: Modifier = Modifier
) {
    // CASO 1: Meno di 2 giocatori registrati nello storico globale
    if (availablePlayers.size < 2 || selectedPlayer1 == null || selectedPlayer2 == null || stats == null) {
        VsEmptyStateCard(
            title = stringResource(R.string.titolo_vs_pochi_giocatori),
            description = stringResource(R.string.desc_vs_pochi_giocatori),
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. SELETTORE DEI DUE SFIDANTI (P1 <-> P2)
        item {
            VsPlayerSelectorCard(
                availablePlayers = availablePlayers,
                selectedPlayer1 = selectedPlayer1,
                onPlayer1Selected = onPlayer1Selected,
                selectedPlayer2 = selectedPlayer2,
                onPlayer2Selected = onPlayer2Selected
            )
        }

        // CASO 2: Entrambi i giocatori esistono nello storico, ma non hanno mai giocato insieme
        if (stats.sharedMatchesCount == 0) {
            item {
                VsEmptyStateCard(
                    title = stringResource(R.string.titolo_vs_nessuna_sfida),
                    description = stringResource(
                        R.string.desc_vs_nessuna_sfida,
                        stats.player1Name,
                        stats.player2Name
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // 2. TABELLONE PRINCIPALE SCONTRI DIRETTI + BARRA PROPORZIONALE
            item {
                VsScoreboardCard(stats = stats)
            }

            // 3. MEDIA PUNTI NEGLI SCONTRI DIRETTI
            item {
                VsDualMetricCard(
                    title = stringResource(R.string.titolo_media_punti_vs),
                    p1Name = stats.player1Name,
                    p1Color = stats.player1Color,
                    p1Value = String.format(Locale.getDefault(), "%.1f", stats.p1AverageScore),
                    p2Name = stats.player2Name,
                    p2Color = stats.player2Color,
                    p2Value = String.format(Locale.getDefault(), "%.1f", stats.p2AverageScore),
                    p1Highlights = stats.p1AverageScore > stats.p2AverageScore,
                    p2Highlights = stats.p2AverageScore > stats.p1AverageScore
                )
            }

            // 4. VITTORIE ASSOLUTE AL TAVOLO (1° POSTO NELLE SFIDE CONDIVISE)
            item {
                VsDualMetricCard(
                    title = stringResource(R.string.titolo_vittorie_tavolo_vs),
                    subtitle = stringResource(R.string.desc_vittorie_tavolo_vs),
                    p1Name = stats.player1Name,
                    p1Color = stats.player1Color,
                    p1Value = "${stats.p1OverallMatchWins}",
                    p2Name = stats.player2Name,
                    p2Color = stats.player2Color,
                    p2Value = "${stats.p2OverallMatchWins}",
                    p1Highlights = stats.p1OverallMatchWins > stats.p2OverallMatchWins,
                    p2Highlights = stats.p2OverallMatchWins > stats.p1OverallMatchWins
                )
            }

            // 5. SCARTO RECORD NEGLI SCONTRI DIRETTI
            item {
                VsBiggestMarginCard(stats = stats)
            }

            // 6. COMBO ON FIRE NELLE SFIDE CONDIVISE
            item {
                VsDualMetricCard(
                    title = stringResource(R.string.titolo_combo_fuoco_vs),
                    p1Name = stats.player1Name,
                    p1Color = stats.player1Color,
                    p1Value = "${stats.p1OnFireTotal}",
                    p2Name = stats.player2Name,
                    p2Color = stats.player2Color,
                    p2Value = "${stats.p2OnFireTotal}",
                    p1Highlights = stats.p1OnFireTotal > stats.p2OnFireTotal,
                    p2Highlights = stats.p2OnFireTotal > stats.p1OnFireTotal
                )
            }

            // 7. TABELLA COMPARATIVA DEI PREMI CONQUISTATI NELLE PARTITE INSIEME
            item {
                VsAwardsComparisonCard(stats = stats)
            }
        }
    }
}

/**
 * Card con i due selettori a tendina colorati e il pulsante centrale di scambio rapido.
 */
@Composable
fun VsPlayerSelectorCard(
    availablePlayers: List<VsPlayerOption>,
    selectedPlayer1: String,
    onPlayer1Selected: (String) -> Unit,
    selectedPlayer2: String,
    onPlayer2Selected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val p1Option = availablePlayers.firstOrNull { it.name.equals(selectedPlayer1, ignoreCase = true) }
    val p2Option = availablePlayers.firstOrNull { it.name.equals(selectedPlayer2, ignoreCase = true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selettore Giocatore 1
            VsSinglePlayerDropdown(
                label = stringResource(R.string.label_giocatore_1),
                selectedName = p1Option?.name ?: selectedPlayer1,
                selectedColor = p1Option?.color ?: 0xFF2196F3.toInt(),
                availablePlayers = availablePlayers,
                onSelect = { chosenName ->
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    if (chosenName.equals(selectedPlayer2, ignoreCase = true)) {
                        // Scambio automatico se si seleziona l'avversario attuale
                        onPlayer2Selected(selectedPlayer1)
                    }
                    onPlayer1Selected(chosenName)
                },
                modifier = Modifier.weight(1f)
            )

            // Pulsante centrale di scambio rapido P1 <-> P2
            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val temp = selectedPlayer1
                    onPlayer1Selected(selectedPlayer2)
                    onPlayer2Selected(temp)
                },
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = stringResource(R.string.desc_scambia_giocatori),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Selettore Giocatore 2
            VsSinglePlayerDropdown(
                label = stringResource(R.string.label_giocatore_2),
                selectedName = p2Option?.name ?: selectedPlayer2,
                selectedColor = p2Option?.color ?: 0xFFF44336.toInt(),
                availablePlayers = availablePlayers,
                onSelect = { chosenName ->
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    if (chosenName.equals(selectedPlayer1, ignoreCase = true)) {
                        // Scambio automatico se si seleziona l'avversario attuale
                        onPlayer1Selected(selectedPlayer2)
                    }
                    onPlayer2Selected(chosenName)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Singolo selettore a tendina (Ancora + DropdownMenu) con pallino identificativo del colore giocatore.
 *
 * 🧠 TEORIA COMPOSE (Box + DropdownMenu):
 * In Material 3, un `DropdownMenu` deve essere inserito all'interno di un `Box` insieme
 * al componente cliccabile che funge da "ancora", così Compose sa esattamente dove
 * ancorare la tendina sullo schermo.
 */
@Composable
private fun VsSinglePlayerDropdown(
    label: String,
    selectedName: String,
    selectedColor: Int,
    availablePlayers: List<VsPlayerOption>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(selectedColor).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColor))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availablePlayers.forEach { option ->
                val isCurrent = option.name.equals(selectedName, ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(option.color))
                            )
                            Text(
                                text = option.name,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option.name)
                    }
                )
            }
        }
    }
}

/**
 * Tabellone principale degli scontri diretti: punteggio grande P1 vs P2,
 * barra proporzionale colorata e riepilogo partite/tempo condiviso.
 */
@Composable
fun VsScoreboardCard(
    stats: HeadToHeadStats,
    modifier: Modifier = Modifier
) {
    val p1Color = Color(stats.player1Color)
    val p2Color = Color(stats.player2Color)
    // 🧠 PROTEZIONE DIVISIONE PER ZERO (`coerceAtLeast(1)`):
    // Garantisce matematicamente che il denominatore sia sempre almeno 1.
    val total = stats.sharedMatchesCount.coerceAtLeast(1)
    val p1Pct = ((stats.p1DirectWins.toFloat() / total) * 100).toInt()
    val p2Pct = ((stats.p2DirectWins.toFloat() / total) * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.titolo_scontri_diretti),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Riga Punteggio Principale: [Nome + Vittorie P1]  VS  [Vittorie P2 + Nome]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lato Giocatore 1
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(p1Color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizedText(
                            text = stats.player1Name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${stats.p1DirectWins}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (stats.p1DirectWins >= stats.p2DirectWins) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "$p1Pct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badge Centrale "VS" + eventuali pareggi
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    if (stats.draws > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (stats.draws == 1) {
                                stringResource(R.string.label_un_pareggio_vs)
                            } else {
                                stringResource(R.string.label_pareggi_vs, stats.draws)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lato Giocatore 2
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(p2Color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizedText(
                            text = stats.player2Name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${stats.p2DirectWins}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (stats.p2DirectWins >= stats.p1DirectWins) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "$p2Pct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🧠 BARRA PROPORZIONALE CON WEIGHT DINAMICI (P1 | Pareggi | P2):
            // In Compose `Modifier.weight()` accetta solo valori > 0f; per questo renderizziamo
            // ogni segmento della barra esclusivamente se il rispettivo contatore è > 0.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            ) {
                if (stats.p1DirectWins > 0) {
                    Box(
                        modifier = Modifier
                            .weight(stats.p1DirectWins.toFloat())
                            .fillMaxHeight()
                            .background(p1Color)
                    )
                }
                if (stats.draws > 0) {
                    Box(
                        modifier = Modifier
                            .weight(stats.draws.toFloat())
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    )
                }
                if (stats.p2DirectWins > 0) {
                    Box(
                        modifier = Modifier
                            .weight(stats.p2DirectWins.toFloat())
                            .fillMaxHeight()
                            .background(p2Color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Partite giocate insieme + Tempo totale condiviso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (stats.sharedMatchesCount == 1) {
                            stringResource(R.string.label_una_sfida_insieme)
                        } else {
                            stringResource(R.string.label_sfide_insieme, stats.sharedMatchesCount)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (stats.totalSharedSeconds > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTime(stats.totalSharedSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card comparativa a due colonne per una metrica numerica (es. Media punti, Vittorie al tavolo, Combo).
 */
@Composable
fun VsDualMetricCard(
    title: String,
    p1Name: String,
    p1Color: Int,
    p1Value: String,
    p2Name: String,
    p2Color: Int,
    p2Value: String,
    p1Highlights: Boolean,
    p2Highlights: Boolean,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Valore P1 (allineato a sinistra)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(p1Color))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = p1Name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = p1Value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (p1Highlights) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Valore P2 (allineato a destra)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = p2Name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(p2Color))
                        )
                    }
                    Text(
                        text = p2Value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (p2Highlights) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Card dedicata allo scarto record registrato nello stesso match tra i due giocatori.
 */
@Composable
fun VsBiggestMarginCard(
    stats: HeadToHeadStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.titolo_scarto_massimo_vs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (stats.biggestMargin > 0 && stats.biggestMarginWinnerName != null && stats.biggestMarginMatchTitle != null) {
                    Text(
                        text = stringResource(
                            R.string.label_ottenuto_da_in,
                            stats.biggestMarginWinnerName,
                            stats.biggestMarginMatchTitle
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(R.string.msg_nessuno_scarto_vs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "+${stats.biggestMargin} pt",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Tabella comparativa dei premi conquistati da P1 e P2 nelle sole partite condivise.
 */
@Composable
fun VsAwardsComparisonCard(
    stats: HeadToHeadStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.titolo_premi_condivisi_vs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (stats.awardsComparison.isEmpty()) {
                Text(
                    text = stringResource(R.string.msg_nessun_premio_vs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Intestazione con i nomi dei due sfidanti sopra le rispettive colonne numeriche
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stats.player1Name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(stats.player1Color),
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stats.player2Name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(stats.player2Color),
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                stats.awardsComparison.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${row.p1Count}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (row.p1Count > row.p2Count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = getAwardLabel(row.type),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${row.p2Count}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (row.p2Count > row.p1Count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Converte l'identificativo puro `VsAwardType` nell'etichetta localizzata con emoji.
 */
@Composable
private fun getAwardLabel(type: VsAwardType): String {
    return when (type) {
        VsAwardType.CECCHINO -> "🎯 ${stringResource(R.string.premio_cecchino)}"
        VsAwardType.INARRESTABILE -> "🔥 ${stringResource(R.string.premio_inarrestabile)}"
        VsAwardType.GAMBERO -> "🦞 ${stringResource(R.string.premio_gambero)}"
        VsAwardType.FENICE -> "🦅 ${stringResource(R.string.premio_fenice)}"
        VsAwardType.DITTATORE -> "👑 ${stringResource(R.string.premio_dittatore)}"
        VsAwardType.PREDATORE -> "🐺 ${stringResource(R.string.premio_predatore)}"
        VsAwardType.TESTA_A_TESTA -> "🤝 ${stringResource(R.string.premio_testa_a_testa)}"
        VsAwardType.LUMACA -> "🐌 ${stringResource(R.string.premio_lumaca)}"
        VsAwardType.DIESEL -> "🚂 ${stringResource(R.string.premio_diesel)}"
    }
}

/**
 * Stato vuoto pulito per quando mancano giocatori o sfide condivise.
 */
@Composable
fun VsEmptyStateCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}