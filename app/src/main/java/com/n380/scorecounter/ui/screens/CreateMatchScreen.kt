package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb // Conversione del colore in valore numerico per il salvataggio nel database
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.ui.components.ColorPickerRow
import com.n380.scorecounter.ui.components.PlayerAtTableCard
import com.n380.scorecounter.ui.components.playerPalette
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ====================================================================
 * SCHERMATA CREAZIONE SFIDA
 * Definizione dei parametri iniziali (titolo, obiettivo, dado) e gestione dei partecipanti.
 * ====================================================================
 */
@OptIn(ExperimentalMaterial3Api::class) // Utilizzo di componenti Material 3 in fase sperimentale (BottomSheet)
@Composable
fun CreateMatchScreen(
    viewModel: MatchViewModel, // Riferimento al ViewModel per la gestione persistente dello stato
    onNavigateToCounter: () -> Unit // Funzione di callback per la navigazione alla schermata successiva
) {
    // STATI LOCALI PER LA CREAZIONE MANUALE DI GIOCATORI
    var newPlayerName by remember { mutableStateOf("") }

    // Inizializzazione del selettore cromatico con Color.Unspecified.
    // Questo valore attiva il disegno del selettore "arcobaleno" per l'assegnazione di un colore casuale.
    var selectedColor by remember { mutableStateOf(Color.Unspecified) }

    // Memorizzazione temporanea del giocatore selezionato per la modifica tramite l'icona matita
    var playerToEdit by remember { mutableStateOf<Player?>(null) }

    // STATI PER LA GESTIONE DEI GIOCATORI RAPIDI (PREFERITI)
    var showFavoritesDialog by remember { mutableStateOf(false) } // Controllo visibilità del pannello inferiore
    var favToEdit by remember { mutableStateOf<String?>(null) }   // Riferimento per la rinomina di un preferito

    // LOGICA DI VALIDAZIONE PER L'AVVIO DELLA SFIDA
    // canStart è vero solo se il titolo non è vuoto e sono presenti giocatori.
    val canStart = viewModel.matchTitle.isNotBlank() && viewModel.players.isNotEmpty()
    // Controllo per la visualizzazione di indicatori di errore in caso di campi obbligatori vuoti.
    var showError by remember { mutableStateOf(false) }

    // STATI PER LA CONFIGURAZIONE DEL DADO
    var showDiceSettingsDialog by remember { mutableStateOf(false) }

    // GESTORE DELLO STATO DEL BOTTOM SHEET
    // skipPartiallyExpanded impostato su true per disabilitare lo stato di ancoraggio intermedio.
    // Questo previene instabilità nel rendering (jittering) quando il contenuto è quasi a tutto schermo.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // GESTIONE STATI MULTI-LAYER:
    // Creiamo un host indipendente per le notifiche del Bottom Sheet.
    // Essendo il Bottom Sheet renderizzato su una "finestra" (Window) di livello
    // superiore (Z-Index maggiore), le notifiche dello Scaffold base verrebbero coperte.
    val sheetSnackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current // Controller per la gestione programmatica della tastiera
    val focusManager = LocalFocusManager.current // Gestore del focus per la rimozione del cursore attivo dalle aree di testo

    // Struttura principale della pagina (Scaffold)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Supporto per la visualizzazione del pattern grafico di sfondo
        snackbarHost = { SnackbarHost(snackbarHostState) },

        // BARRA INFERIORE: Pulsante principale di avvio sfida
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                // Arrotondamento dei soli bordi superiori per un look integrato alla base
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // Protezione dalle occlusioni della barra di navigazione di sistema
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (canStart) {
                                onNavigateToCounter()
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(20.dp),

                        // L'elevazione viene azzerata se il pulsante non è cliccabile per coerenza visiva
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (canStart) 8.dp else 0.dp
                        ),

                        // Gestione cromatica condizionale basata sullo stato di validazione (Regola 12%/38%)
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canStart)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            contentColor = if (canStart)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Inizia",
                            modifier = Modifier.padding(end = 8.dp).size(28.dp)
                        )
                        Text(
                            text = "Inizia Sfida",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // CONTENUTO SCORREVOLE: Configurazione e aggiunta partecipanti
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Nuova Sfida",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // CARD 1: CONFIGURAZIONE REGOLE
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Regole del Gioco",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Pulsante per le impostazioni del dado
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showDiceSettingsDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Filled.Casino, "Dado", modifier = Modifier.size(18.dp).padding(end = 4.dp))
                                Text(
                                    text = "Dado (D${viewModel.diceSides})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // CAMPO: NOME SFIDA
                        OutlinedTextField(
                            value = viewModel.matchTitle,
                            onValueChange = {
                                viewModel.matchTitle = it
                                if (it.isNotBlank()) showError = false 
                            },
                            label = { Text("Nome della sfida") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            isError = showError && viewModel.matchTitle.isBlank(),
                            supportingText = {
                                if (showError && viewModel.matchTitle.isBlank()) {
                                    Text("Il nome della sfida è obbligatorio", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            leadingIcon = {
                                val iconColor = if (showError && viewModel.matchTitle.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                Icon(Icons.Default.VideogameAsset, null, tint = iconColor)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                        )

                        // CAMPO: TRAGUARDO (Filtro input numerico)
                        OutlinedTextField(
                            value = viewModel.targetScore,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    viewModel.targetScore = newValue
                                }
                            },
                            label = { Text("Traguardo (Opzionale)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // SELETTORE TEMATICO RAPIDO
                        val isAnimeTheme = viewModel.matchTitle == "Sfida Anime"
                        val isCarteTheme = viewModel.matchTitle == "Sfida Carte"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.matchTitle = "Sfida Anime"
                                    showError = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (isAnimeTheme) ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                else ButtonDefaults.filledTonalButtonColors(),
                                border =  BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Default.Tv, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Anime")
                            }

                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.matchTitle = "Sfida Carte"
                                    showError = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = if (isCarteTheme) ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                )
                                else ButtonDefaults.filledTonalButtonColors(),
                                border =  BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Default.Style, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Carte")
                            }
                        }
                    }
                }

                // CARD 2: GESTIONE PARTECIPANTI (Preferiti e Manuali)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                            Icon(Icons.Filled.PersonAddAlt1, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Gestione Partecipanti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // SEZIONE GIOCATORI RAPIDI (PREFERITI)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Giocatori Rapidi:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                showFavoritesDialog = true
                            }) {
                                Icon(Icons.Filled.Settings, "Gestisci Giocatori Rapidi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                            }
                        }

                        if (viewModel.favoriteNames.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(end = 16.dp)
                            ) {
                                items(viewModel.favoriteNames) { fav ->
                                    val isAlreadyAtTable = viewModel.players.any { it.name.equals(fav, ignoreCase = true) }

                                    FilterChip(
                                        selected = isAlreadyAtTable,
                                        onClick = {
                                            if (!isAlreadyAtTable) {
                                                val finalColor = if (selectedColor == Color.Unspecified) {
                                                    val usedColors = viewModel.players.map { it.color }
                                                    val availableColors = playerPalette.filter { it.toArgb() !in usedColors }
                                                    if (availableColors.isNotEmpty()) availableColors.random() else playerPalette.random()
                                                } else {
                                                    selectedColor
                                                }
                                                viewModel.addPlayer(fav, finalColor.toArgb())
                                            } else {
                                                // Feedback tattile attivato solo in caso di rimozione dell'ultimo giocatore
                                                if (viewModel.players.size == 1) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                val playerToRemove = viewModel.players.find { it.name.equals(fav, ignoreCase = true) }
                                                playerToRemove?.let { viewModel.removePlayer(it) }
                                            }
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        label = { Text(fav, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isAlreadyAtTable,
                                            borderColor = MaterialTheme.colorScheme.primary, 
                                            selectedBorderColor = Color.Transparent 
                                        )
                                    )
                                }
                            }
                        } else {
                            Text("Nessun giocatore rapido salvato.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // SEZIONE AGGIUNTA MANUALE E COLORE
                        Text("Scegli un colore:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                        ColorPickerRow(
                            selectedColor = selectedColor,
                            onColorSelected = { 
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                selectedColor = it 
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aggiungi manualmente:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPlayerName,
                                onValueChange = { newPlayerName = it },
                                modifier = Modifier.weight(1f).height(64.dp),
                                label = { Text("Nome") },
                                shape = RoundedCornerShape(20.dp),
                                leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                )
                            )

                            val isAddPlayerEnabled = newPlayerName.trim().isNotEmpty() && viewModel.players.none { it.name.equals(newPlayerName.trim(), ignoreCase = true) }

                            Button(
                                onClick = {
                                    if (isAddPlayerEnabled) {
                                        val finalColor = if (selectedColor == Color.Unspecified) {
                                            playerPalette.random()
                                        } else {
                                            selectedColor
                                        }
                                        viewModel.addPlayer(newPlayerName, finalColor.toArgb())
                                        newPlayerName = "" 
                                        selectedColor = Color.Unspecified
                                    }
                                },
                                modifier = Modifier.height(64.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = if (isAddPlayerEnabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    contentColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            ) { Text("Aggiungi") }
                        }
                    }
                }

                // CARD 3: GIOCATORI AL TAVOLO
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Groups, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tavolo Partecipanti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = "${viewModel.players.size}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // EMPTY STATE: Visualizzato se la lista dei partecipanti è vuota
                        if (viewModel.players.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.PersonAdd, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Il tavolo è vuoto!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                                    Text("Aggiungi giocatori per iniziare la sfida.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                                }
                            }
                        } else {
                            // Generazione dinamica delle card per ogni partecipante
                            viewModel.players.forEachIndexed { index, player ->
                                PlayerAtTableCard(
                                    player = player,
                                    isFirst = index == 0,
                                    isLast = index == viewModel.players.size - 1,
                                    onMoveUp = { viewModel.movePlayer(index, index - 1) },
                                    onMoveDown = { viewModel.movePlayer(index, index + 1) },
                                    onEdit = { playerToEdit = player },
                                    onRemove = {
                                        val removedIndex = index
                                        val removedPlayer = player
                                        viewModel.removePlayer(player)

                                        coroutineScope.launch {
                                            launch { delay(3000L); snackbarHostState.currentSnackbarData?.dismiss() }
                                            val result = snackbarHostState.showSnackbar(
                                                "${player.name} rimosso",
                                                "ANNULLA",
                                                duration = SnackbarDuration.Indefinite
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restorePlayer(removedIndex, removedPlayer)
                                            }
                                        }
                                    }
                                )
                                if (index != viewModel.players.size - 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MODALE UNIFICATO: MODIFICA GIOCATORE (Nome e Colore)
    if (playerToEdit != null) {
        var editedName by remember { mutableStateOf(playerToEdit!!.name) }
        var editedColor by remember { mutableIntStateOf(playerToEdit!!.color) }

        AlertDialog(
            onDismissRequest = { playerToEdit = null },
            title = {
                Text(
                    text = "Modifica Giocatore",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Scegli un nuovo colore:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 35.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 20.dp)
                            .heightIn(max = 140.dp)
                    ) {
                        items(playerPalette) { color ->
                            val isSelected = color.toArgb() == editedColor

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { editedColor = color.toArgb() }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nuovo nome") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            playerToEdit = null
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Annulla", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                playerToEdit!!.name = editedName
                                viewModel.updatePlayerColor(playerToEdit!!, editedColor)
                                playerToEdit = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // MODAL BOTTOM SHEET: GESTIONE PREFERITI
    if (showFavoritesDialog) {
        var newFavName by remember { mutableStateOf("") }

        // ====================================================================
        // Invece di far calcolare al BottomSheet la sua altezza in base al contenuto,
        // calcoliamo un'altezza statica e assoluta basata sull'hardware del dispositivo.
        // ====================================================================

        // 1. Otteniamo l'oggetto Configuration che contiene le specifiche fisiche dello schermo
        val configuration = LocalConfiguration.current
        // 2. Estraiamo l'altezza totale dello schermo in Dp (Density-independent Pixels)
        val screenHeight = configuration.screenHeightDp.dp
        // 3. Calcoliamo il nostro 85% in modo matematico e lo salviamo in una costante.
        // Questo numero ora è fisso (es. 720.dp) e non dipende più dai ricalcoli grafici.
        val maxSheetHeight = screenHeight * 0.80f

        ModalBottomSheet(
            onDismissRequest = {showFavoritesDialog = false},
            sheetState = sheetState
        ) {
            // 🧠 Z-INDEX ARCHITECTURE: Uso del Box per la sovrapposizione.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Sostituiamo .fillMaxHeight(0.90f) con .height(maxSheetHeight).
                    // Assegnando un'altezza immutabile, il layout node non ha più bisogno
                    // di chiedere al genitore quanto spazio ha a disposizione durante lo scroll,
                    // annullando totalmente il bug di ricalcolo infinito (Jittering).
                    //.height(maxSheetHeight)
                    .fillMaxHeight(0.80f)
            ) {
                // FOGLIO INFERIORE (Z-Index 0): Il contenuto reale (Testi, input, liste)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Giocatori Rapidi",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            coroutineScope.launch {
                                sheetState.hide()
                                showFavoritesDialog = false
                            }
                        }) {
                            Icon(Icons.Filled.Close, "Chiudi")
                        }
                    }

                    // INSERIMENTO NUOVO PREFERITO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        // Allineamento verticale dei centri logici dei componenti "fratelli"
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newFavName,
                            onValueChange = { newFavName = it },
                            // GEOMETRIA INTERNA (Risoluzione Disallineamento)
                            // Abbiamo rimosso '.height(64.dp)'.
                            // L'OutlinedTextField utilizza una complessa gerarchia di padding interni
                            // per gestire l'etichetta fluttuante e l'icona. Lasciandolo libero di
                            // calcolare la propria altezza, assume lo standard Material di 56.dp,
                            // ripristinando il perfetto allineamento tra Icona e Label.
                            modifier = Modifier.weight(1f),
                            // ==========================================================
                            // Placeholder invece di label = { Text("Nuovo nome") },
                            // Il 'placeholder' rimane confinato all'interno dei bordi visibili
                            // e non richiede a Compose di generare "spazio invisibile" in cima.
                            // Questo riporta l'ingombro logico a coincidere con l'ingombro visivo,
                            // allineando magicamente il componente al bottone adiacente!
                            // ==========================================================
                            placeholder = { Text("Nuovo nome") },
                            shape = RoundedCornerShape(20.dp),
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) }
                        )

                        val isAddFavEnabled = newFavName.trim().isNotEmpty() && viewModel.favoriteNames.none { it.equals(newFavName.trim(), ignoreCase = true) }

                        Button(
                            onClick = {
                                if (isAddFavEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.addFavorite(newFavName.trim())
                                    newFavName = ""
                                }
                            },
                            modifier = Modifier.height(63.dp),//altezza del bottone "Aggiungi" coerente con l'OutlinedTextField
                            shape = RoundedCornerShape(20.dp),
                            border = if (isAddFavEnabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAddFavEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                contentColor = if (isAddFavEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        ) { Text("Aggiungi") }
                    }

                Spacer(modifier = Modifier.height(16.dp))
                
                // AREA ELENCO PREFERITI: Struttura a "Tavolo" coerente con il design system dell'app
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(viewModel.favoriteNames) { index, fav ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // SISTEMA DI RIORDINO
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.moveFavorite(index, index - 1)
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.onSurface)
                                        }

                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.moveFavorite(index, index + 1)
                                            },
                                            enabled = index < viewModel.favoriteNames.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    // Identificativo testuale in grassetto per risalto visivo
                                    Text(
                                        text = fav,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .weight(1f) 
                                            .padding(start = 12.dp) 
                                    )

                                    // Gestione modifiche
                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        favToEdit = fav
                                    }) {
                                        Icon(Icons.Filled.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    // Gestione eliminazione
                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                                        // Salvataggio dei riferimenti puntuali per la closure
                                        val removedIndex = index
                                        val removedFav = fav

                                        // Mutazione dello stato (viene riflessa istantaneamente dalla UI)
                                        viewModel.removeFavorite(fav)

                                        // 🧠 CONCORRENZA UI: Avvio dell'orchestrazione della notifica modale
                                        coroutineScope.launch {
                                            // Chiusura auto-temporizzata di sicurezza
                                            launch {
                                                delay(3000L)
                                                // Utilizziamo lo stato dedicato al Bottom Sheet!
                                                sheetSnackbarHostState.currentSnackbarData?.dismiss()
                                            }

                                            // Invocazione bloccante (suspend): attende input dell'utente o timeout
                                            val result = sheetSnackbarHostState.showSnackbar(
                                                message = "$fav rimosso dai rapidi",
                                                actionLabel = "ANNULLA",
                                                duration = SnackbarDuration.Indefinite
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreFavorite(removedIndex, removedFav)
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Filled.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                }


                // FOGLIO SUPERIORE (Z-Index 1): Il palco per le notifiche (Snackbar)
                // Posizionandolo alla fine del Box, viene disegnato "Sopra" a tutto il resto.
                // Lo ancoriamo visivamente in basso e al centro.
                SnackbarHost(
                    hostState = sheetSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Aggiungiamo padding inferiore per non farla incollare ai margini fisici dello schermo
                        .padding(bottom = 16.dp)
                )
            }
        }
    }

    // MODALE MODIFICA NOME PREFERITO
    if (favToEdit != null) {
        var editedFavName by remember { mutableStateOf(favToEdit!!) }
        AlertDialog(
            onDismissRequest = { favToEdit = null },
            title = {
                Text("Modifica Nome Rapido",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ) },
            text = {
                OutlinedTextField(
                    value = editedFavName,
                    onValueChange = { editedFavName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            favToEdit = null
                        },
                        modifier = Modifier
                            .weight(1f) 
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Annulla", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (editedFavName.isNotBlank()) {
                                viewModel.editFavorite(favToEdit!!, editedFavName.trim())
                                favToEdit = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f) 
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null
        )
    }

    // DIALOG IMPOSTAZIONI DADO
    if (showDiceSettingsDialog) {
        DiceSettingsDialog(
            currentSides = viewModel.diceSides,
            onSidesChanged = { newSides -> viewModel.diceSides = newSides },
            onDismiss = { showDiceSettingsDialog = false }
        )
    }
}
