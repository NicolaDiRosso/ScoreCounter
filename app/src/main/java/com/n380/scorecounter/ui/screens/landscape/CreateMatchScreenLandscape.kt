package com.n380.scorecounter.ui.screens.landscape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.CustomSelectableChip
import com.n380.scorecounter.ui.components.FadedRightEdgeWrapper
import com.n380.scorecounter.ui.components.PlayerAtTableCard
import com.n380.scorecounter.ui.components.TonalActionPill
import com.n380.scorecounter.ui.components.playerPalette
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ====================================================================
 * SCHERMATA DIDATTICA: CreateMatchScreenLandscape (Master-Detail Landscape)
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE DI JETPACK COMPOSE (LAYOUT MASTER-DETAIL LANDSCAPE):
 *
 * 1. ARCHITETTURA A 2 PANNELLI BILANCIATI:
 *    Negli smartphone in orizzontale la larghezza dello schermo è abbondante, mentre l'altezza è ridotta.
 *    Dividere la schermata in 3 colonne sottili creava problemi di compressione visiva delle chip e dei testi.
 *    Soluzione UX Pro:
 *    - PANNELLO MASTER (Sinistra - weight = 0.44f): Raccoglie la Configurazione (Regole del Gioco +
 *      Gestione Partecipanti) ed il pulsante "Inizia Sfida" ancorato direttamente in basso a sinistra.
 *    - PANNELLO DETAIL (Destra - weight = 0.50f): Dedica il 100% dell'altezza verticale disponibile al
 *      "Tavolo Partecipanti", permettendo di mostrare da 5 a 8 giocatori contemporaneamente senza compressioni!
 *
 * 2. ERGONOMIA DEL POLLICE SINISTRO (Thumb Ergonomics):
 *    Posizionando il pulsante "Inizia Sfida" in basso a sinistra nel pannello di configurazione,
 *    risulta facilmente raggiungibile con il pollice della mano che impugna il telefono a sinistra.
 *
 * 3. WINDOW INSETS & HARDWARE CUTOUT (displayCutoutPadding):
 *    Evita che i controlli o i testi finiscano sotto il foro/tacca della fotocamera frontale.
 * ====================================================================
 */
@Composable
fun CreateMatchScreenLandscape(
    viewModel: MatchViewModel,
    newPlayerName: String,
    onNewPlayerNameChange: (String) -> Unit,
    playerToEdit: Player?,
    onPlayerToEditChange: (Player?) -> Unit,
    showError: Boolean,
    onShowErrorChange: (Boolean) -> Unit,
    canStart: Boolean,
    isNavigating: Boolean,
    onIsNavigatingChange: (Boolean) -> Unit,
    onShowFavoritesDialog: () -> Unit,
    onNavigateToCounter: () -> Unit,
    haptic: HapticFeedback,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    // Stringhe di errore pre-estratte dal contesto Composable
    val errTitoloGiocatori = stringResource(R.string.err_titolo_e_giocatori)
    val errSoloTitolo = stringResource(R.string.err_solo_titolo)
    val errSoloGiocatori = stringResource(R.string.err_solo_giocatori)

    Row(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding() // 📸 Protezione hardware fotocamera frontale
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ====================================================================
        // 🏛️ PANNELLO MASTER (Sinistra - weight = 0.44f): CONFIGURAZIONE & AVVIO
        // ====================================================================
        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // INTESTAZIONE TITOLO SCHERMATA
            AutoResizedText(
                text = stringResource(R.string.titolo_nuova_sfida),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            // CARD UNIFICATA DI CONFIGURAZIONE (Regole + Gestione Partecipanti)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    // --------------------------------------------------------
                    // SEZIONE 1: REGOLE DEL GIOCO
                    // --------------------------------------------------------
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.MenuBook,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizedText(
                            text = stringResource(R.string.titolo_regole_gioco),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Input Nome Sfida
                    OutlinedTextField(
                        value = viewModel.matchTitle,
                        onValueChange = {
                            viewModel.matchTitle = it
                            if (it.isNotBlank()) onShowErrorChange(false)
                        },
                        label = { Text(stringResource(R.string.hint_nome_sfida)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = showError && viewModel.matchTitle.isBlank(),
                        supportingText = if (showError && viewModel.matchTitle.isBlank()) {
                            {
                                Text(
                                    stringResource(R.string.err_nome_sfida_obbligatorio),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null,
                        leadingIcon = {
                            val iconColor = if (showError && viewModel.matchTitle.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            Icon(Icons.Default.VideogameAsset, null, tint = iconColor)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )

                    // Titoli Recenti
                    if (viewModel.matchTitleHistory.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                            AutoResizedText(
                                text = stringResource(R.string.label_titoli_recenti),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            FadedRightEdgeWrapper(modifier = Modifier.fillMaxWidth()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(end = 16.dp)
                                ) {
                                    items(viewModel.matchTitleHistory) { recentTitle ->
                                        val isSelected = viewModel.matchTitle == recentTitle
                                        CustomSelectableChip(
                                            text = recentTitle,
                                            isSelected = isSelected,
                                            onClick = {
                                                haptic.performHapticFeedback(
                                                    if (isSelected) HapticFeedbackType.LongPress else HapticFeedbackType.Confirm
                                                )
                                                if (isSelected) {
                                                    viewModel.matchTitle = ""
                                                } else {
                                                    viewModel.matchTitle = recentTitle
                                                    onShowErrorChange(false)
                                                }
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Input Traguardo Punti
                    OutlinedTextField(
                        value = viewModel.targetScore,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                viewModel.targetScore = newValue
                            }
                        },
                        label = {
                            Text(
                                stringResource(R.string.hint_traguardo),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    // --------------------------------------------------------
                    // SEZIONE 2: GESTIONE PARTECIPANTI
                    // --------------------------------------------------------
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.PersonAddAlt1,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizedText(
                            text = stringResource(R.string.titolo_gestione_partecipanti),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Giocatori Rapidi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_giocatori_rapidi),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TonalActionPill(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onShowFavoritesDialog()
                            }
                        ) {
                            Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_gestisci),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (viewModel.favoriteNames.isNotEmpty()) {
                        FadedRightEdgeWrapper(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(end = 16.dp)
                            ) {
                                items(viewModel.favoriteNames) { fav ->
                                    val isAlreadyAtTable = viewModel.players.any { it.name.equals(fav, ignoreCase = true) }
                                    CustomSelectableChip(
                                        text = fav,
                                        isSelected = isAlreadyAtTable,
                                        onClick = {
                                            if (!isAlreadyAtTable) {
                                                val usedColors = viewModel.players.map { it.color }
                                                val availableColors = playerPalette.filter { it.toArgb() !in usedColors }
                                                val finalColor = if (availableColors.isNotEmpty()) availableColors.random() else playerPalette.random()
                                                viewModel.addPlayer(fav, finalColor.toArgb())
                                            } else {
                                                if (viewModel.players.size == 1) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                val playerToRemove = viewModel.players.find { it.name.equals(fav, ignoreCase = true) }
                                                playerToRemove?.let { viewModel.removePlayer(it) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.msg_nessun_giocatore_rapido),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Aggiunta Manuale
                    Text(
                        stringResource(R.string.label_aggiungi_manualmente),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlayerName,
                            onValueChange = onNewPlayerNameChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.hint_nome_giocatore),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        val isAddPlayerEnabled = newPlayerName.trim().isNotEmpty() && viewModel.players.none { it.name.equals(newPlayerName.trim(), ignoreCase = true) }

                        Button(
                            onClick = {
                                if (isAddPlayerEnabled) {
                                    val usedColors = viewModel.players.map { it.color }
                                    val availableColors = playerPalette.filter { it.toArgb() !in usedColors }
                                    val finalColor = if (availableColors.isNotEmpty()) availableColors.random() else playerPalette.random()
                                    viewModel.addPlayer(newPlayerName.trim(), finalColor.toArgb())
                                    onNewPlayerNameChange("")
                                }
                            },
                            modifier = Modifier.fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                contentColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        ) {
                            AutoResizedText(stringResource(R.string.btn_aggiungi), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // PULSANTE "INIZIA SFIDA" ANCORATO IN BASSO A SINISTRA
            // ----------------------------------------------------------------
            Button(
                onClick = {
                    if (isNavigating) return@Button
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                    val isTitleValid = viewModel.matchTitle.isNotBlank()
                    val arePlayersPresent = viewModel.players.isNotEmpty()

                    if (isTitleValid && arePlayersPresent) {
                        viewModel.addTitleToHistory(viewModel.matchTitle)
                        onIsNavigatingChange(true)
                        onNavigateToCounter()
                    } else {
                        onShowErrorChange(true)
                        val errorMessage = when {
                            !isTitleValid && !arePlayersPresent -> errTitoloGiocatori
                            !isTitleValid -> errSoloTitolo
                            else -> errSoloGiocatori
                        }
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = errorMessage,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (canStart) 6.dp else 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canStart) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    contentColor = if (canStart) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.desc_inizia_icon),
                    modifier = Modifier.padding(end = 8.dp).size(24.dp)
                )
                Text(
                    text = stringResource(R.string.btn_inizia_sfida),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ====================================================================
        // 📋 PANNELLO DETAIL (Destra - weight = 0.56f): TAVOLO PARTECIPANTI A TUTTA ALTEZZA
        // ====================================================================
        Card(
            modifier = Modifier
                .weight(0.56f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Intestazione con Contatore
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Groups, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    AutoResizedText(
                        text = stringResource(R.string.titolo_tavolo_partecipanti),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${viewModel.players.size}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Contenuto Tavolo (Empty State o Lista Giocatori)
                if (viewModel.players.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            AutoResizedText(
                                stringResource(R.string.msg_tavolo_vuoto),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                            AutoResizedText(
                                stringResource(R.string.desc_tavolo_vuoto),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = viewModel.players,
                            key = { _, player -> player.name }
                        ) { index, player ->
                            val msgRimozione = stringResource(R.string.msg_giocatore_rimosso, player.name)
                            val btnAnnullaUndo = stringResource(R.string.btn_annulla_undo)

                            PlayerAtTableCard(
                                player = player,
                                isFirst = index == 0,
                                isLast = index == viewModel.players.size - 1,
                                onMoveUp = { viewModel.movePlayer(index, index - 1) },
                                onMoveDown = { viewModel.movePlayer(index, index + 1) },
                                onEdit = { onPlayerToEditChange(player) },
                                onRemove = {
                                    if (!isNavigating) {
                                        val removedIndex = index
                                        val removedPlayer = player
                                        viewModel.removePlayer(player)
                                        coroutineScope.launch {
                                            launch { delay(3000L); snackbarHostState.currentSnackbarData?.dismiss() }
                                            val result = snackbarHostState.showSnackbar(
                                                message = msgRimozione,
                                                actionLabel = btnAnnullaUndo,
                                                duration = SnackbarDuration.Indefinite
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restorePlayer(removedIndex, removedPlayer)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
