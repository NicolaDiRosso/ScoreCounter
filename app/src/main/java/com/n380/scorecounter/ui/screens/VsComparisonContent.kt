package com.n380.scorecounter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.n380.scorecounter.R
import com.n380.scorecounter.model.HeadToHeadStats
import com.n380.scorecounter.model.VsAwardType
import com.n380.scorecounter.model.VsPlayerOption
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.formatTime
import java.util.Locale

/**
 * ====================================================================
 * VISTA CONFRONTO DIRETTO "DUELLI" (PORTRAIT + COMPONENTI MODULARI CONDIVISI)
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
 * 2. PERCHÉ UN DIALOGO CON RICERCA INVECE DI UN DROPDOWN MENU:
 *    In Jetpack Compose, inserire un `OutlinedTextField` dentro un `DropdownMenu`
 *    causa problemi di gestione del focus quando compare la tastiera virtuale.
 *    Aprire invece un `AlertDialog` dedicato con campo di ricerca in cima e
 *    `LazyColumn` scorrevole offre un'esperienza fluida sia con 3 amici sia con 50.
 *
 * 3. PREVENZIONE CONFRONTO CON SE STESSI (Smart Swap):
 *    Se per Giocatore 1 scegli il giocatore già assegnato a Giocatore 2
 *    (o viceversa), i due sfidanti si scambiano automaticamente di posto.
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
        // 1. SELETTORE DEI DUE SFIDANTI (P1 <-> P2) CON FINESTRA DI RICERCA
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
                    subtitle = stringResource(R.string.desc_media_punti_vs),
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
                    subtitle = stringResource(R.string.desc_combo_fuoco_vs),
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
 * Testo adattivo resiliente per schede e riquadri compatti:
 * - Misura lo spazio reale disponibile tramite `onTextLayout` prima di disegnare il testo
 *   (`drawWithContent`) e riduce progressivamente `fontSize` e `lineHeight` se rileva un
 *   overflow orizzontale (`didOverflowWidth`) o verticale (`didOverflowHeight`).
 * - Supporta sia righe singole (`maxLines = 1`) sia blocchi multi-riga (`maxLines > 1`),
 *   comunicando al contenitore tramite `onOverflowStateChange` se il testo risulta ancora
 *   troncato anche alla scala minima (`minScaleFactor`), così da attivare la tendina espandibile.
 */
@Composable
private fun VsAdaptiveText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    minScaleFactor: Float = 0.62f,
    onOverflowStateChange: ((Boolean) -> Unit)? = null
) {
    // Ricalcola la scala da capo ogni volta che cambiano testo, stile o numero massimo di righe
    var scaleFactor by remember(text, style, maxLines) { mutableFloatStateOf(1f) }
    var readyToDraw by remember(text, style, maxLines) { mutableStateOf(false) }

    val scaledFontSize = if (style.fontSize.isSpecified) {
        style.fontSize * scaleFactor
    } else {
        TextUnit.Unspecified
    }
    val scaledLineHeight = if (style.lineHeight.isSpecified) {
        style.lineHeight * scaleFactor
    } else {
        TextUnit.Unspecified
    }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            // Disegna solo quando la misura è stabilizzata per evitare sfarfallii visivi
            if (readyToDraw) drawContent()
        },
        style = style.copy(
            fontSize = scaledFontSize,
            lineHeight = scaledLineHeight
        ),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        softWrap = maxLines > 1,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layoutResult ->
            val hasOverflow = layoutResult.didOverflowWidth || layoutResult.didOverflowHeight
            if (hasOverflow && scaleFactor > minScaleFactor) {
                scaleFactor = (scaleFactor * 0.9f).coerceAtLeast(minScaleFactor)
            } else {
                readyToDraw = true
                onOverflowStateChange?.invoke(hasOverflow)
            }
        }
    )
}

/**
 * Card di selezione dei due sfidanti:
 * - Intestazione esplicativa "Scegli due giocatori a confronto"
 * - Due riquadri cliccabili colorati che aprono la finestra di selezione con ricerca
 * - Pulsante centrale per invertire istantaneamente Giocatore 1 e Giocatore 2
 *
 * @param compact Riduce i margini verticali e forza il titolo su una riga in modalità Landscape,
 *                permettendo al tabellone sottostante di restare visibile senza scorrimento.
 */
@Composable
fun VsPlayerSelectorCard(
    availablePlayers: List<VsPlayerOption>,
    selectedPlayer1: String,
    onPlayer1Selected: (String) -> Unit,
    selectedPlayer2: String,
    onPlayer2Selected: (String) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val p1Option = availablePlayers.firstOrNull { it.name.equals(selectedPlayer1, ignoreCase = true) }
    val p2Option = availablePlayers.firstOrNull { it.name.equals(selectedPlayer2, ignoreCase = true) }

    // 🧠 STATO MODALE (0 = chiuso, 1 = scelta Giocatore 1, 2 = scelta Giocatore 2):
    // Usiamo un singolo intero al posto di due booleani separati per garantire che
    // non possano mai aprirsi due finestre contemporaneamente.
    var pickingForSlot by remember { mutableIntStateOf(0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            // Intestazione chiara per comunicare subito l'interattività dei selettori
            VsAdaptiveText(
                text = stringResource(R.string.titolo_scegli_giocatori_vs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Riquadro cliccabile Giocatore 1
                VsPlayerSlotBox(
                    label = stringResource(R.string.label_giocatore_1),
                    selectedName = p1Option?.name ?: selectedPlayer1,
                    selectedColor = p1Option?.color ?: 0xFF2196F3.toInt(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pickingForSlot = 1
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
                    modifier = Modifier.size(if (compact) 34.dp else 38.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = stringResource(R.string.desc_scambia_giocatori),
                        modifier = Modifier.size(if (compact) 18.dp else 20.dp)
                    )
                }

                // Riquadro cliccabile Giocatore 2
                VsPlayerSlotBox(
                    label = stringResource(R.string.label_giocatore_2),
                    selectedName = p2Option?.name ?: selectedPlayer2,
                    selectedColor = p2Option?.color ?: 0xFFF44336.toInt(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pickingForSlot = 2
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Finestra modale con casella di ricerca ed elenco giocatori
    if (pickingForSlot == 1 || pickingForSlot == 2) {
        val isSlot1 = pickingForSlot == 1
        val currentSlotName = if (isSlot1) selectedPlayer1 else selectedPlayer2
        val otherSlotName = if (isSlot1) selectedPlayer2 else selectedPlayer1

        VsPlayerPickerDialog(
            dialogTitle = if (isSlot1) {
                stringResource(R.string.titolo_dialog_scegli_g1)
            } else {
                stringResource(R.string.titolo_dialog_scegli_g2)
            },
            availablePlayers = availablePlayers,
            currentSelectedName = currentSlotName,
            otherSlotName = otherSlotName,
            onDismiss = { pickingForSlot = 0 },
            onPlayerChosen = { chosenName ->
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                if (isSlot1) {
                    if (chosenName.equals(selectedPlayer2, ignoreCase = true)) {
                        onPlayer2Selected(selectedPlayer1)
                    }
                    onPlayer1Selected(chosenName)
                } else {
                    if (chosenName.equals(selectedPlayer1, ignoreCase = true)) {
                        onPlayer1Selected(selectedPlayer2)
                    }
                    onPlayer2Selected(chosenName)
                }
                pickingForSlot = 0
            }
        )
    }
}

/**
 * Riquadro singolo cliccabile che mostra l'etichetta ("Giocatore 1/2"),
 * una barra verticale colorata sul bordo sinistro, il nome del giocatore attivo
 * e la freccia a tendina.
 */
@Composable
private fun VsPlayerSlotBox(
    label: String,
    selectedName: String,
    selectedColor: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(selectedColor).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicatore cromatico verticale sul bordo sinistro al posto del puntino accanto al nome
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(selectedColor))
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                VsAdaptiveText(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    minScaleFactor = 0.58f
                )
                Spacer(modifier = Modifier.height(1.dp))
                VsAdaptiveText(
                    text = selectedName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    minScaleFactor = 0.6f
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Finestra di dialogo Material 3 per selezionare un giocatore con barra di ricerca istantanea.
 *
 * 🧠 TEORIA COMPOSE (`remember(availablePlayers, searchQuery)` & `heightIn`):
 * - `remember(availablePlayers, searchQuery)`: ricalcola il filtro testuale solo quando
 *   l'utente digita una lettera o cambia lo storico, evitando filtraggi inutili ad ogni frame.
 * - `Modifier.heightIn(max = 280.dp)`: limita l'altezza massima della `LazyColumn` interna al
 *   dialogo in modo che i pulsanti in basso restino sempre visibili anche su schermi piccoli o
 *   in orientamento orizzontale.
 */
@Composable
private fun VsPlayerPickerDialog(
    dialogTitle: String,
    availablePlayers: List<VsPlayerOption>,
    currentSelectedName: String,
    otherSlotName: String,
    onDismiss: () -> Unit,
    onPlayerChosen: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredPlayers = remember(availablePlayers, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            availablePlayers
        } else {
            availablePlayers.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        title = {
            AutoResizedText(
                text = dialogTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Casella di ricerca istantanea per filtrare l'elenco quando ci sono molti giocatori
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.hint_cerca_giocatore),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )

                if (filteredPlayers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.msg_nessun_giocatore_trovato),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredPlayers,
                            key = { it.name.lowercase() }
                        ) { option ->
                            val isCurrentSlot = option.name.equals(currentSelectedName, ignoreCase = true)
                            val isOtherSlot = option.name.equals(otherSlotName, ignoreCase = true)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        onPlayerChosen(option.name)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCurrentSlot) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = if (isCurrentSlot) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(option.color))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = option.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isCurrentSlot) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isCurrentSlot) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (option.totalMatchesPlayed == 1) {
                                                    stringResource(R.string.label_una_partita_giocatore_vs)
                                                } else {
                                                    stringResource(
                                                        R.string.label_partite_giocatore_vs,
                                                        option.totalMatchesPlayed
                                                    )
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isCurrentSlot) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else if (isOtherSlot) {
                                        Icon(
                                            imageVector = Icons.Filled.SwapHoriz,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_annulla_dialog_vs),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        dismissButton = null
    )
}

/**
 * Tabellone principale degli scontri diretti: punteggio grande P1 vs P2,
 * barra proporzionale colorata e riepilogo partite/tempo condiviso.
 *
 * @param compact Adatta dimensioni dei numeri e spaziature per il pannello sinistro in Landscape.
 */
@Composable
fun VsScoreboardCard(
    stats: HeadToHeadStats,
    compact: Boolean = false,
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
                .padding(if (compact) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VsAdaptiveText(
                text = stringResource(R.string.titolo_scontri_diretti),
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

            // Riga Punteggio Principale: [Nome + Vittorie P1]  VS  [Vittorie P2 + Nome]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lato Giocatore 1 (simmetrico: Nome -> Capsula colore -> Punteggio -> Percentuale)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VsAdaptiveText(
                        text = stats.player1Name,
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(p1Color)
                    )
                    Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
                    Text(
                        text = "${stats.p1DirectWins}",
                        style = (if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium)
                            .copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Black,
                        color = if (stats.p1DirectWins >= stats.p2DirectWins) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "$p1Pct%",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
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
                            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(
                                horizontal = if (compact) 10.dp else 12.dp,
                                vertical = if (compact) 4.dp else 6.dp
                            )
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

                // Lato Giocatore 2 (speculare al Giocatore 1)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VsAdaptiveText(
                        text = stats.player2Name,
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(p2Color)
                    )
                    Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
                    Text(
                        text = "${stats.p2DirectWins}",
                        style = (if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium)
                            .copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Black,
                        color = if (stats.p2DirectWins >= stats.p1DirectWins) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "$p2Pct%",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))

            // 🧠 BARRA PROPORZIONALE CON WEIGHT DINAMICI (P1 | Pareggi | P2):
            // In Compose `Modifier.weight()` accetta solo valori > 0f; per questo renderizziamo
            // ogni segmento della barra esclusivamente se il rispettivo contatore è > 0.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 10.dp else 12.dp)
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

            Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

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
                    VsAdaptiveText(
                        text = if (stats.sharedMatchesCount == 1) {
                            stringResource(R.string.label_una_sfida_insieme)
                        } else {
                            stringResource(R.string.label_sfide_insieme, stats.sharedMatchesCount)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
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
                        VsAdaptiveText(
                            text = formatTime(stats.totalSharedSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card comparativa a due colonne per una metrica numerica (es. Media punti, Vittorie al tavolo, Combo).
 * 🧠 ECCELLENZA UX/UI DESIGN & ACCESSIBILITÀ MATERIAL 3:
 * 1. DISPOSIZIONE SIMMETRICA CENTRATA (Column con Alignment.CenterHorizontally):
 *    Ogni pillola centra orizzontalmente e verticalmente nome, capsula colore e valore numerico,
 *    eliminando spazi vuoti sul lato destro e rendendo le due metà perfettamente speculari.
 * 2. CIFRE TABELLARI (`fontFeatureSettings = "tnum"`):
 *    Garantisce che numeri con cifre diverse (es. `5,5` vs `12,0`) occupino una larghezza
 *    visiva bilanciata senza sbilanciare l'allineamento centrale.
 * 3. GERARCHIA VISIVA NON-INTERATTIVA (Anti-Button Confusion):
 *    Per evitare che le pillole statistiche vengano scambiate per pulsanti cliccabili
 *    (come "Chiudi statistiche"), evitiamo riempimenti pieni `primaryContainer` e usiamo
 *    una sfumatura traslucida (`alpha = 0.15f`) del colore personale del giocatore vincitore
 *    abbinata a un bordo del suo stesso colore (`alpha = 0.75f`).
 * 4. TESTI RESILIENTI E TENDINA ESPANDIBILE SU OVERFLOW:
 *    Titolo e sottotitolo si ridimensionano automaticamente; se con caratteri di sistema
 *    molto grandi il sottotitolo dovesse eccedere le 2 righe, compare un pulsante a freccia
 *    che espande il testo completo senza alterare l'altezza base delle card affiancate.
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
    var hasSubtitleOverflow by remember(subtitle) { mutableStateOf(false) }
    var isExpanded by remember(subtitle) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (hasSubtitleOverflow) {
                    Modifier.clickable { isExpanded = !isExpanded }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VsAdaptiveText(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasSubtitleOverflow) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    VsAdaptiveText(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        onOverflowStateChange = { hasSubtitleOverflow = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // IntrinsicSize.Max assicura che entrambe le pillole abbiano la stessa altezza
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pillola Giocatore 1
                VsPlayerMetricPill(
                    playerName = p1Name,
                    playerColor = p1Color,
                    value = p1Value,
                    isHighlighted = p1Highlights,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Pillola Giocatore 2
                VsPlayerMetricPill(
                    playerName = p2Name,
                    playerColor = p2Color,
                    value = p2Value,
                    isHighlighted = p2Highlights,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Tendina espandibile visibile solo se il sottotitolo ha richiesto troncamento
            if (subtitle != null) {
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Singola pillola comparativa centrata: mostra nome, indicatore cromatico sottile e valore grande.
 * Quando `isHighlighted` è attivo, applica una velatura del colore del giocatore (`alpha = 0.15f`)
 * e un bordo definito nello stesso colore, mantenendo il testo ad alto contrasto su `onSurface`.
 */
@Composable
private fun VsPlayerMetricPill(
    playerName: String,
    playerColor: Int,
    value: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(playerColor)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) {
            accentColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        },
        border = if (isHighlighted) {
            BorderStroke(1.5.dp, accentColor.copy(alpha = 0.75f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VsAdaptiveText(
                text = playerName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Capsula colore centrata sotto il nome: più marcata sul vincitore, più discreta sull'altro
            Box(
                modifier = Modifier
                    .width(if (isHighlighted) 28.dp else 20.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isHighlighted) 1f else 0.6f))
            )

            Spacer(modifier = Modifier.height(6.dp))

            VsAdaptiveText(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Black,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                },
                maxLines = 1
            )
        }
    }
}

/**
 * Card dedicata allo scarto record registrato nello stesso match tra i due giocatori.
 *
 * Allinea la propria struttura visiva alle card comparative (`VsDualMetricCard`):
 * - Intestazione in alto: titolo (`⚡ Scarto record`) e sottotitolo adattivo su 2 righe
 *   (`Massima differenza punti in una partita`).
 * - Riquadro `Surface` centrato in basso: mostra il distacco numerico (`+X pt`), la barretta
 *   del colore del giocatore detentore del record e il riferimento all'incontro (`Da X in 'Y'`),
 *   mantenendo altezza e proporzioni uniformi con la card affiancata in Landscape.
 * - Se il titolo della partita o il sottotitolo superano lo spazio disponibile (es. font di
 *   sistema ingrandito), compare automaticamente una freccia per espandere il dettaglio completo.
 */
@Composable
fun VsBiggestMarginCard(
    stats: HeadToHeadStats,
    modifier: Modifier = Modifier
) {
    val recordHolderColor = when {
        stats.biggestMarginWinnerName.equals(stats.player1Name, ignoreCase = true) -> Color(stats.player1Color)
        stats.biggestMarginWinnerName.equals(stats.player2Name, ignoreCase = true) -> Color(stats.player2Color)
        else -> MaterialTheme.colorScheme.primary
    }
    val hasRecord = stats.biggestMargin > 0

    val subtitleText = stringResource(R.string.desc_scarto_massimo_vs)
    val detailText = if (hasRecord && stats.biggestMarginWinnerName != null && stats.biggestMarginMatchTitle != null) {
        stringResource(
            R.string.label_ottenuto_da_in,
            stats.biggestMarginWinnerName,
            stats.biggestMarginMatchTitle
        )
    } else {
        stringResource(R.string.msg_nessuno_scarto_vs)
    }

    var hasSubtitleOverflow by remember(subtitleText) { mutableStateOf(false) }
    var hasDetailOverflow by remember(detailText) { mutableStateOf(false) }
    val canExpand = hasSubtitleOverflow || hasDetailOverflow
    var isExpanded by remember(subtitleText, detailText) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (canExpand) {
                    Modifier.clickable { isExpanded = !isExpanded }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Intestazione identica a VsDualMetricCard: titolo + sottotitolo descrittivo
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VsAdaptiveText(
                        text = stringResource(R.string.titolo_scarto_massimo_vs),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (canExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                VsAdaptiveText(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    onOverflowStateChange = { hasSubtitleOverflow = it }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Surface inferiore coordinato con le pillole delle altre metriche
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (hasRecord) {
                    recordHolderColor.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                },
                border = BorderStroke(
                    width = if (hasRecord) 1.5.dp else 1.dp,
                    color = if (hasRecord) {
                        recordHolderColor.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VsAdaptiveText(
                        text = if (hasRecord) "+${stats.biggestMargin} pt" else "0 pt",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Black,
                        color = if (hasRecord) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        },
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Capsula colore del giocatore detentore dello scarto record
                    Box(
                        modifier = Modifier
                            .width(if (hasRecord) 28.dp else 20.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasRecord) recordHolderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    VsAdaptiveText(
                        text = detailText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (hasRecord) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (hasRecord) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        onOverflowStateChange = { hasDetailOverflow = it }
                    )
                }
            }

            // Tendina espandibile con il testo integrale in caso di overflow
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(6.dp))
                    if (hasSubtitleOverflow) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            VsAdaptiveText(
                text = stringResource(R.string.titolo_premi_condivisi_vs),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (stats.awardsComparison.isEmpty()) {
                Text(
                    text = stringResource(R.string.msg_nessun_premio_vs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Intestazione con i nomi dei due sfidanti e capsula colore sopra le rispettive colonne
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VsAdaptiveText(
                            text = stats.player1Name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color(stats.player1Color))
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        modifier = Modifier.width(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VsAdaptiveText(
                            text = stats.player2Name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color(stats.player2Color))
                        )
                    }
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
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Black,
                            color = if (row.p1Count > row.p2Count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(64.dp),
                            textAlign = TextAlign.Center
                        )

                        VsAdaptiveText(
                            text = getAwardLabel(row.type),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${row.p2Count}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Black,
                            color = if (row.p2Count > row.p1Count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(64.dp),
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