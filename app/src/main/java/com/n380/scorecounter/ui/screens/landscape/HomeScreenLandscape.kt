package com.n380.scorecounter.ui.screens.landscape

import android.content.Context
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import com.n380.scorecounter.R
import com.n380.scorecounter.model.getHistoricalCecchino
import com.n380.scorecounter.model.getHistoricalFenice
import com.n380.scorecounter.model.getHistoricalGambero
import com.n380.scorecounter.model.getHistoricalInarrestabile
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.ScoreChart
import com.n380.scorecounter.ui.components.SlideUpAnimatedVisibility
import com.n380.scorecounter.ui.components.buildMatchShareText
import com.n380.scorecounter.ui.components.formatDate
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.ui.components.launchShareIntent
import com.n380.scorecounter.ui.components.launchShareIntentWithImage
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/**
 * ====================================================================
 * SCHERMATA DIDATTICA: HomeScreenLandscape (Modalità Orizzontale Reattiva)
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE DI JETPACK COMPOSE (SUDDIVISI PER L'INSEGNAMENTO):
 *
 * 1. LAYOUT RESPONSIVE ED ESPANSO (DISPOSIZIONE A 2 COLONNE):
 *    In orientamento Landscape (orizzontale), l'altezza dello schermo scende a ~360-400dp.
 *    I layout verticali a colonna singola soffrono perché gli elementi si schiacciano.
 *    Soluzione: Usiamo una `Row` per creare due pannelli affiancati:
 *    - Pannello Sinistro (weight = 0.38f): Comandi principali (Titolo, 3 Action Cards, Tasto Nuova Sfida).
 *    - Pannello Destro (weight = 0.62f): Tavolo dello storico con griglia di partite a 2 colonne.
 *
 * 2. WINDOW INSETS & HARDWARE CUTOUT (displayCutoutPadding):
 *    Gli smartphone moderni possiedono fori per la fotocamera (punch-hole) o tacche (notch)
 *    posizionati a sinistra, destra o al centro.
 *    Usando `Modifier.displayCutoutPadding()`, Android calcola la coordinata esatta dell'hardware
 *    e sposta la UI lontano dalla lente della fotocamera su QUALSIASI dispositivo!
 *
 * 3. GRIGLIA ADATTIVA E MULTI-DISPOSITIVO (GridCells.Adaptive):
 *    Invece di fissare il numero di colonne, `GridCells.Adaptive(minSize = 220.dp)` dice a Compose:
 *    "Crea quante colonne servono affinché ciascuna sia larga almeno 220dp".
 *    - Su uno smartphone landscape: crea 2 colonne perfette.
 *    - Su un Tablet landscape: crea 3 o 4 colonne in modo dinamico!
 *
 * 4. RIDIMENSIONAMENTO AUTOMATICO DEI TESTI (AutoResizedText):
 *    I testi usano `AutoResizedText` per calcolare lo spazio in Dp disponibile. Se l'utente imposta
 *    un font gigante dalle impostazioni Android, il testo riduce la sua dimensione sp per non tagliarsi.
 * ====================================================================
 */
@Composable
fun HomeScreenLandscape(
    viewModel: MatchViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToStats: () -> Unit,
    onShowDonationDialog: () -> Unit,
    onShowAboutDialog: () -> Unit,
    onShowBackupDialog: () -> Unit,
    onShowSettingsDialog: () -> Unit,
    expandedMatchIndex: Int,
    onExpandedMatchIndexChange: (Int) -> Unit,
    timeFormatter: SimpleDateFormat,
    haptic: HapticFeedback,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    showNewMatchButton: Boolean,
    modifier: Modifier = Modifier
) {
    // 🧠 TEORIA: La Row dispone gli elementi da sinistra a destra (Asse X).
    Row(
        modifier = modifier
            .fillMaxSize() // Occupa l'intero display del dispositivo
            // 🧠 TEORIA STATUS BARS PADDING:
            // .statusBarsPadding() garantisce che la UI non finisca sotto l'orologio e la batteria in alto.
            .statusBarsPadding()
            // 🧠 TEORIA NAVIGATION BARS PADDING:
            // .navigationBarsPadding() garantisce che la UI non finisca sotto la barra bianca dei gesti Android in basso.
            .navigationBarsPadding()
            // 📸 TEORIA PROTEZIONE FOTOCAMERA / PUNCH-HOLE:
            // .displayCutoutPadding() interroga il WindowManager di Android per ottenere il rettangolo esatto del foro
            // o tacca della fotocamera (a sinistra, a destra o al centro). Aggiunge il margine necessario solo sul lato
            // dove si trova la lente, spostando i pulsanti al sicuro ed evitando qualsiasi sovrapposizione!
            .displayCutoutPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp) // Distanza tra pannello sinistro e destro
    ) {
        // ====================================================================
        // 🏛️ PANNELLO DI SINISTRA: TITOLO, TAVOLA CONTENITIVA E NUOVA SFIDA
        // ====================================================================
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TITOLO DELLA PAGINA "Storico Sfide"
            // 🧠 DESIGN ORIZZONTALE LANDSCAPE:
            // Nel pannello di sinistra della vista landscape, tutte le schede azione
            // (Statistiche Globali, Sostieni ScoreCounter, Info e Backup & Ripristino)
            // sono già chiaramente visibili ed estese all'interno della tavola contenitiva qui sotto.
            // Di conseguenza, l'intestazione rimane pulita con il solo titolo principale.
            AutoResizedText(
                text = stringResource(R.string.titolo_storico_sfide),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 2.dp)
            )

            // ------------------------------------------------------------
            // TAVOLA CONTENITIVA (SURFACE VARIANT 24dp): RAGGRUPPA LE 3 SCHEDE AZIONE
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
                    // CARD AZIONE 1: STATISTICHE GLOBALI
                    // ------------------------------------------------------------
                    if (viewModel.history.isNotEmpty()) {
                        HomeLandscapeActionCard(
                            icon = Icons.Filled.BarChart,
                            title = stringResource(R.string.title_statistiche_landscape),
                            subtitle = stringResource(R.string.subtitle_statistiche_landscape),
                            containerColor = MaterialTheme.colorScheme.surface,
                            iconBackgroundColor = MaterialTheme.colorScheme.primary,
                            iconTintColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToStats()
                            }
                        )
                    }

                    // ------------------------------------------------------------
                    // CARD AZIONE 2: IMPOSTAZIONI E PREFERENZE
                    // ------------------------------------------------------------
                    HomeLandscapeActionCard(
                        icon = Icons.Filled.Settings,
                        title = stringResource(R.string.titolo_impostazioni_dialog),
                        subtitle = stringResource(R.string.desc_impostazioni_dialog),
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconBackgroundColor = MaterialTheme.colorScheme.primary,
                        iconTintColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onShowSettingsDialog()
                        }
                    )

                    // ------------------------------------------------------------
                    // CARD AZIONE 3: SOSTIENI SCORECOUNTER (DONAZIONE)
                    // ------------------------------------------------------------
                    HomeLandscapeActionCard(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.title_sostieni_landscape),
                        subtitle = stringResource(R.string.subtitle_sostieni_landscape),
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconBackgroundColor = MaterialTheme.colorScheme.tertiary,
                        iconTintColor = MaterialTheme.colorScheme.onTertiary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onShowDonationDialog()
                        }
                    )

                    // ------------------------------------------------------------
                    // CARD AZIONE 3: INFO APP & FEEDBACK
                    // ------------------------------------------------------------
                    HomeLandscapeActionCard(
                        icon = Icons.AutoMirrored.Filled.Help,
                        title = stringResource(R.string.title_info_landscape),
                        subtitle = stringResource(R.string.subtitle_info_landscape),
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconBackgroundColor = MaterialTheme.colorScheme.primary,
                        iconTintColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onShowAboutDialog()
                        }
                    )

                    // ------------------------------------------------------------
                    // CARD AZIONE 4: BACKUP E RIPRISTINO DATI
                    // ------------------------------------------------------------
                    HomeLandscapeActionCard(
                        icon = Icons.Filled.Storage,
                        title = stringResource(R.string.titolo_backup_dialog),
                        subtitle = stringResource(R.string.desc_backup_dialog),
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconBackgroundColor = MaterialTheme.colorScheme.secondary,
                        iconTintColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onShowBackupDialog()
                        }
                    )
                }
            }

            // ------------------------------------------------------------
            // PULSANTE + NUOVA SFIDA (Ancorato in Basso nel Pannello Sinistro)
            // ------------------------------------------------------------
            SlideUpAnimatedVisibility(
                visible = showNewMatchButton
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearMatch()
                        onNavigateToCreate()
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
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(22.dp)
                    )
                    AutoResizedText(
                        text = stringResource(R.string.btn_nuova_sfida),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ====================================================================
        // 📋 PANNELLO DI DESTRA: TAVOLO STORICO CON GRIGLIA MATCH A 2 COLONNE
        // ====================================================================
        // 🧠 TEORIA WEIGHT: Modifier.weight(0.62f) assegna il rimanente 62% di larghezza a questo contenitore.
        Card(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            HomeHistoryGridTableContent(
                viewModel = viewModel,
                timeFormatter = timeFormatter,
                haptic = haptic,
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                onExpandMatch = onExpandedMatchIndexChange
            )
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE DIDATTICO: HomeLandscapeActionCard
 * ====================================================================
 * Una Card d'azione orizzontale elegante che mostra una pillola circolare con l'icona
 * a sinistra, affiancata da Titolo e Sottotitolo adattivi a destra.
 * ====================================================================
 */
@Composable
private fun HomeLandscapeActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconBackgroundColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 🧠 TEORIA BORDURA MATERIAL YOU:
    // border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)):
    // Aggiunge un bordo sottile ed elegante attorno alla scheda che utilizza il colore 'outline'
    // del sistema Material 3 (Material You), garantendo distinzione visiva e coerenza col tema attivo.
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = 2.dp,//bordo dei 3 pulsanti (statistiche, sostieni e Info)
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pillola dell'icona
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconBackgroundColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Colonna con Titolo e Sottotitolo (AutoResizedText previene tagli indesiderati)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                AutoResizedText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AutoResizedText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE DIDATTICO: HomeHistoryGridTableContent
 * ====================================================================
 * 🧠 TEORIA GRIGLIE COMPOSE (LazyVerticalGrid & GridCells.Adaptive):
 * `LazyVerticalGrid` ordina gli elementi in una griglia multi-colonna invece che in una sola colonna.
 *
 * Usando `GridCells.Adaptive(minSize = 220.dp)`:
 * - Se lo spazio disponibile nel pannello è 480dp, crea 2 colonne da 240dp.
 * - Se lo spazio su un Tablet è 900dp, crea 3 o 4 colonne automaticamente!
 * ====================================================================
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHistoryGridTableContent(
    viewModel: MatchViewModel,
    timeFormatter: SimpleDateFormat,
    haptic: HapticFeedback,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onExpandMatch: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // 🧠 TEORIA MEMOIZATION / CACHE (remember):
    // Ricalcola la lista filtrata solo quando cambia searchQuery o viewModel.history, risparmiando cicli CPU durante lo scroll.
    val filteredHistory = remember(searchQuery, viewModel.history) {
        if (searchQuery.isBlank()) {
            viewModel.history
        } else {
            viewModel.history.filter { match ->
                val titleMatches = match.title.contains(searchQuery, ignoreCase = true)
                val playerMatches = match.allPlayers.any { player ->
                    player.name.contains(searchQuery, ignoreCase = true)
                }
                titleMatches || playerMatches
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Tocco a vuoto per chiudere la tastiera
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        // --------------------------------------------------------------------
        // BARRA DI RICERCA RIDIMENSIONATA ED ARMONIOSA
        // --------------------------------------------------------------------
        if (viewModel.history.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 2.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                placeholder = {
                    AutoResizedText(
                        text = stringResource(R.string.hint_cerca_sfida),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.desc_cerca_icon),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.btn_cancella),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }

        // STATI VUOTI E GRIGLIA DEI MATCH
        if (viewModel.history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .padding(bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    AutoResizedText(
                        text = stringResource(R.string.titolo_cronologia_vuota),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    AutoResizedText(
                        text = stringResource(R.string.titolo_nessun_risultato),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // 🧠 TEORIA GRIDCELLS.ADAPTIVE:
            // GridCells.Adaptive(minSize = 220.dp) calcola dinamicamente la larghezza delle colonne.
            // Se lo schermo è stretto (smartphone) crea 2 colonne, se è largo (tablet) crea 3 o 4 colonne!
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHistory) { record ->
                    var expanded by remember { mutableStateOf(false) }

                    // 📸 RIFERIMENTO VIEW PER CONDIVISIONE BITMAP
                    var chartViewByCard by remember { mutableStateOf<View?>(null) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    expanded = !expanded
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onExpandMatch(viewModel.history.indexOf(record))
                                }
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AutoResizedText(
                                    text = record.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 6.dp)
                                )
                                if (record.durationSeconds > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Timer, null,
                                            modifier = Modifier.size(14.dp).padding(end = 2.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatTime(record.durationSeconds),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AutoResizedText(
                                    text = stringResource(R.string.label_vincitore, record.winnerName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            AnimatedVisibility(visible = expanded) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                                    record.allPlayers.forEachIndexed { index, playerRecord ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (index == 0) "🏆 ${playerRecord.name}" else "${index + 1}. ${playerRecord.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                                color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${playerRecord.score} pt",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    // ------------------------------------------------------------
                                    // GRAFICO DELL'ANDAMENTO PUNTEGGI (SCORE CHART)
                                    // ------------------------------------------------------------
                                    val validHistory = record.allPlayers.any {
                                        (it.scoreHistory ?: emptyList()).size > 1
                                    }
                                    if (validHistory) {
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // 🧠 TEORIA AREA TATTILE & OVERLAY GRAFICO:
                                        // Racchiudiamo titolo del grafico e mini-grafico in una Column con combinedClickable.
                                        // Toccando il grafico si innesca onExpandMatch, che imposta expandedMatchIndex aprendo
                                        // il grafico a schermo intero (MatchAnalysisOverlay)!
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .combinedClickable(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onExpandMatch(viewModel.history.indexOf(record))
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onExpandMatch(viewModel.history.indexOf(record))
                                                    }
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.titolo_andamento_punteggi),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    imageVector = Icons.Filled.Fullscreen,
                                                    contentDescription = stringResource(R.string.desc_espandi_grafico),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // 🧠 TEORIA ANDROID VIEW WRAPPER PER CATTURA BITMAP:
                                            // Per poter fotografare il grafico come immagine (Bitmap) da condividere,
                                            // avvolgiamo ScoreChart in una ComposeView salvandone il riferimento in chartViewByCard.
                                            AndroidView(
                                                factory = { ctx ->
                                                    ComposeView(ctx).apply {
                                                        chartViewByCard = this
                                                        setContent {
                                                            ScoreChart(
                                                                players = record.allPlayers,
                                                                modifier = Modifier
                                                                    .height(100.dp)
                                                                    .fillMaxWidth()
                                                                    .padding(top = 4.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                update = { view ->
                                                    (view as ComposeView).setContent {
                                                        ScoreChart(
                                                            players = record.allPlayers,
                                                            modifier = Modifier
                                                                .height(100.dp)
                                                                .fillMaxWidth()
                                                                .padding(top = 4.dp)
                                                        )
                                                    }
                                                }
                                            )
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                                    }

                                    // ------------------------------------------------------------
                                    // DATA/ORA PARTITA E PULSANTI AZIONE (CONDIVIDI ED ELIMINA)
                                    // ------------------------------------------------------------
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 🧠 TEORIA BLOCCO DATA/ORA (AutoResizedText con Weight):
                                        // AutoResizedText con Modifier.weight(1f) garantisce che la data ed orario
                                        // (es. "Alle 14:30 del 22/09/2026") occupino tutto lo spazio disponibile a sinistra
                                        // senza mai spingere o nascondere i pulsanti di condivisione ed eliminazione a destra!
                                        AutoResizedText(
                                            text = stringResource(
                                                R.string.label_data_ora_partita,
                                                timeFormatter.format(Date(record.timestamp)),
                                                if (record.timestamp > 0L) formatDate(record.timestamp) else ""
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val playersData = record.allPlayers.map { Pair(it.name, it.score) }
                                                    val mappedCecchino = record.getHistoricalCecchino()?.let { Pair(it.first.name, it.second) }
                                                    val mappedInarrestabile = record.getHistoricalInarrestabile()?.let { Pair(it.first.name, it.second) }
                                                    val mappedGambero = record.getHistoricalGambero()?.let { Pair(it.first.name, it.second) }
                                                    val mappedFenice = record.getHistoricalFenice()?.let { Pair(it.first.name, it.second) }

                                                    val shareText = buildMatchShareText(
                                                        context = context,
                                                        title = record.title,
                                                        durationSeconds = record.durationSeconds,
                                                        timestamp = record.timestamp,
                                                        rankedPlayersData = playersData,
                                                        cecchinoData = mappedCecchino,
                                                        inarrestabileData = mappedInarrestabile,
                                                        gamberoData = mappedGambero,
                                                        feniceData = mappedFenice
                                                    )

                                                    coroutineScope.launch {
                                                        try {
                                                            val bitmap = chartViewByCard?.drawToBitmap()
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
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Share,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val removedIndex = viewModel.history.indexOf(record)
                                                    viewModel.deleteMatch(record)

                                                    coroutineScope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            context.getString(R.string.msg_partita_eliminata),
                                                            context.getString(R.string.btn_annulla_undo),
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restoreMatch(removedIndex, record)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
