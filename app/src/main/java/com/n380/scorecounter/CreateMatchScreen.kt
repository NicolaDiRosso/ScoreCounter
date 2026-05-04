package com.n380.scorecounter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb // Serve per convertire il Colore visivo in un numero da salvare nel Database
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ====================================================================
 * SCHERMATA CREAZIONE SFIDA
 * Qui l'utente imposta le regole (nome, traguardo, dado) e aggiunge i giocatori al tavolo.
 * ====================================================================
 */
@OptIn(ExperimentalMaterial3Api::class) // Consente l'uso di componenti UI sperimentali come il BottomSheet
@Composable
fun CreateMatchScreen(
    viewModel: MatchViewModel, // Il "Cervello" che conserva i dati tra una schermata e l'altra
    onNavigateToCounter: () -> Unit // La funzione per cambiare pagina
) {
    // ---> STATI DEL GIOCATORE MANUALE <---
    var newPlayerName by remember { mutableStateOf("") }

    // DIDATTICA: Inizializziamo il selettore con 'Color.Unspecified'.
    // Questa è una parola d'ordine che in SharedUtils fa disegnare il pallino "Arcobaleno" (colore casuale).
    var selectedColor by remember { mutableStateOf(Color.Unspecified) }

    // Memorizza quale giocatore l'utente vuole modificare cliccando sull'icona della matita
    var playerToEdit by remember { mutableStateOf<Player?>(null) }

    // ---> STATI DEI GIOCATORI RAPIDI (PREFERITI) <---
    var showFavoritesDialog by remember { mutableStateOf(false) } // Apre/Chiude il pannello inferiore
    var favToEdit by remember { mutableStateOf<String?>(null) }   // Per rinominare un preferito

    // ---> LOGICA DI SICUREZZA (La porta d'ingresso) <---
    // canStart diventa VERO (true) solo se il titolo non è vuoto E c'tè almeno un giocatore al tavolo.
    val canStart = viewModel.matchTitle.isNotBlank() && viewModel.players.isNotEmpty()
    // showError diventa VERO se l'utente è un "furbetto" e prova a cliccare Inizia Sfida senza aver compilato i dati.
    var showError by remember { mutableStateOf(false) }

    // ---> STATI DEL DADO <---
    var showDiceSettingsDialog by remember { mutableStateOf(false) }


    // ---> IL MOTORE DEL BOTTOM SHEET <---
    // Ricorda lo stato del pannello che scivola dal basso (se è aperto, mezzo aperto o chiuso)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Scaffold è l'impalcatura della pagina (Sfondo, Contenuto, Barra in basso)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Permette di vedere la grafica a pattern sullo sfondo!
        snackbarHost = { SnackbarHost(snackbarHostState) },

        // ====================================================================
        // LA BARRA INFERIORE (BottomBar) - Il grande pulsante d'avvio
        // ====================================================================
        bottomBar = {
            // ---> IL DOCK "EXPRESSIVE" <---
            // Avvolgiamo il pulsante nella Surface semi-trasparente che abbiamo usato
            // nelle altre schermate. Questo crea il "cassetto" ancorato al fondo.
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                // Arrotondiamo solo i bordi superiori per un look da "Bottom Sheet"
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // Protezione dalla barra di sistema
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // La tua logica di sicurezza originale rimane intatta
                            if (canStart) {
                                onNavigateToCounter()
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            // Portiamo l'altezza a 72.dp per coerenza con gli altri tasti principali
                            .height(72.dp),
                        shape = RoundedCornerShape(20.dp),

                        // ELEVAZIONE: Si azzera se il pulsante è disabilitato per dare senso di piattezza
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (canStart) 8.dp else 0.dp
                        ),

                        // LOGICA COLORI (Regola 12%/38%):
                        // Manteniamo la tua ottima gestione della visibilità condizionale
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
                        // Cambiamo l'icona da 'Add' a 'PlayArrow' per indicare l'avvio
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Inizia",
                            modifier = Modifier.padding(end = 8.dp).size(28.dp)
                        )
                        Text(
                            text = "Inizia Sfida",
                            // Usiamo lo stile HeadlineSmall come nel pulsante Home
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // ====================================================================
        // IL CORPO DELLA SCHERMATA (Lista Scorrevole)
        // ====================================================================
        // LazyColumn fa sì che la pagina possa scorrere dall'alto verso il basso.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp) // Cuscinetto in fondo prima della BottomBar
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // ---> IL TITOLO DELLA PAGINA <---
                Text(
                    text = "Nuova Sfida",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // ====================================================================
                // CARD 1: REGOLE DEL GIOCO (Nome, Tema, Punteggio e Dado)
                // ====================================================================
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // Riga d'intestazione: Titolo a sinistra, Dado a destra
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, // Separa gli elementi agli estremi
                            verticalAlignment = Alignment.CenterVertically // Li allinea al centro verticale
                        ) {
                            Text(
                                text = "Regole del Gioco",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // ---> IL BOTTONE DEL DADO <---
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDiceSettingsDialog = true // Apre il popup del dado
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                // Aggiungiamo l'icona e inseriamo un testo per chiarire a cosa serve
                                Icon(Icons.Filled.Casino, "Dado", modifier = Modifier.size(18.dp).padding(end = 4.dp))
                                Text(
                                    text = "Dado (D${viewModel.diceSides})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // ---> NOME DELLA SFIDA <---
                        OutlinedTextField(
                            value = viewModel.matchTitle,
                            onValueChange = {
                                viewModel.matchTitle = it
                                if (it.isNotBlank()) showError = false // Spegne l'errore rosso se l'utente inizia a scrivere
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nome della Sfida") },
                            placeholder = { Text("Es. Sfida Epica") },
                            shape = RoundedCornerShape(20.dp),
                            // Mostra il bordo rosso se showError è vero E il campo è ancora vuoto
                            isError = showError && viewModel.matchTitle.isBlank(),
                            supportingText = {
                                if (showError && viewModel.matchTitle.isBlank()) {
                                    Text("Il nome della sfida è obbligatorio", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            leadingIcon = {
                                val iconColor = if (showError && viewModel.matchTitle.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                Icon(Icons.Filled.VideogameAsset, null, tint = iconColor)
                            }
                        )

                        // ---> BOTTONI DEI TEMI RAPIDI <---
                        val isAnimeTheme = viewModel.matchTitle == "Sfida Anime"
                        val isCarteTheme = viewModel.matchTitle == "Sfida Carte"

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // BOTTONE TEMA ANIME
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.matchTitle = "Sfida Anime"
                                },
                                modifier = Modifier.weight(1f), // Metà larghezza esatta
                                shape = RoundedCornerShape(20.dp),
                                colors = if (isAnimeTheme) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                else ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                border = if (isAnimeTheme) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Filled.Tv, null, modifier = Modifier.padding(end = 8.dp))
                                Text("Anime")
                            }

                            // BOTTONE TEMA CARTE
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.matchTitle = "Sfida Carte"
                                },
                                modifier = Modifier.weight(1f), // Metà larghezza esatta
                                shape = RoundedCornerShape(20.dp),
                                colors = if (isCarteTheme) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                else ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                border = if (isCarteTheme) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Filled.Style, null, modifier = Modifier.padding(end = 8.dp))
                                Text("Carte")
                            }
                        }

                        // ---> TRAGUARDO PUNTEGGIO <---
                        OutlinedTextField(
                            value = viewModel.targetScore,
                            onValueChange = { newValue ->
                                // Blocca la tastiera permettendo di inserire SOLO numeri
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    viewModel.targetScore = newValue
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Traguardo (Opzionale)") },
                            placeholder = { Text("Es. 50") },
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }

                // ====================================================================
                // CARD 2: GESTIONE PARTECIPANTI (Giocatori Rapidi e Manuali)
                // ====================================================================
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Gestione Partecipanti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )

                        // --- 1. SEZIONE GIOCATORI RAPIDI (PREFERITI) ---
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ---> MODIFICA ESTETICA: Uniformiamo lo stile <---
                            // Usiamo labelLarge e il colore primario per allinearlo visivamente a "Scegli un colore"
                            Text(
                                text = "Giocatori Rapidi:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Tasto Ingranaggio per aprire la gestione dei preferiti

                            // Tasto Ingranaggio per aprire la gestione dei preferiti
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showFavoritesDialog = true
                            }) {
                                Icon(Icons.Filled.Settings, "Gestisci Giocatori Rapidi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                            }
                        }

                        // Crea i bottoncini dei preferiti scorrevoli
                        if (viewModel.favoriteNames.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(end = 32.dp) // Taglia il quarto bottone a metà visivamente
                            ) {
                                items(viewModel.favoriteNames) { fav ->
                                    val isAlreadyAtTable = viewModel.players.any { it.name.equals(fav, ignoreCase = true) }

                                    OutlinedButton(
                                        onClick = {
                                            if (!isAlreadyAtTable) {
                                                //haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                                // ---> NUOVA LOGICA MULTICOLOR INTELLIGENTE (Colori Univoci) <---
                                                val finalColor = if (selectedColor == Color.Unspecified) {

                                                    // 1. Estraiamo tutti i colori già assegnati ai giocatori attualmente al tavolo
                                                    // (Estraiamo il valore numerico 'color' dal database)
                                                    val usedColors = viewModel.players.map { it.color }

                                                    // 2. Filtriamo la tavolozza: teniamo SOLO i colori che NON sono presenti nella lista 'usedColors'
                                                    val availableColors = playerPalette.filter { it.toArgb() !in usedColors }

                                                    // 3. Estraiamo un colore a caso da quelli rimasti.
                                                    // CONTROLLO DI SICUREZZA: Se abbiamo finito i colori liberi (es. 8 giocatori e 8 colori),
                                                    // l'app crasherebbe. Quindi, se la lista è vuota, peschiamo a caso da tutta la tavolozza.
                                                    if (availableColors.isNotEmpty()) {
                                                        availableColors.random()
                                                    } else {
                                                        playerPalette.random()
                                                    }

                                                } else {
                                                    // Se l'utente ha toccato un pallino colorato specifico, usiamo quello ignorando i duplicati
                                                    selectedColor
                                                }

                                                viewModel.addPlayer(fav, finalColor.toArgb())
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        // Se già al tavolo, facciamo sembrare il pulsante esaurito/trasparente
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (!isAlreadyAtTable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        border = BorderStroke(1.dp, if (!isAlreadyAtTable) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) { Text(fav) }
                                }
                            }
                        } else {
                            Text("Nessun giocatore rapido salvato.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // --- 2. DIVISORE ---
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(bottom = 1.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(5.dp))

                        // --- 3. SEZIONE INSERIMENTO MANUALE E SCELTA COLORE ---

                        // Sotto-titolo 1: Il Colore
                        Text("Scegli un colore:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                        // La Tavolozza importata da SharedUtils
                        ColorPickerRow(
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // ---> VALUTAZIONE UX: Respirare <---
                        // Aggiungiamo un piccolo spazio per separare visivamente i pallini dal campo di testo sottostante
                        Spacer(modifier = Modifier.height(4.dp))

                        // Sotto-titolo 2: Il Nome
                        // ---> VALUTAZIONE UX: Testo più conciso <---
                        // Accorciamo la frase per non appesantire la UI, mantenendo lo stile rigorosamente blu (primary)
                        Text("Aggiungi manualmente:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                        Spacer(modifier = Modifier.height(8.dp)) // Altro piccolo respiro prima di scrivere

                        // Riga dell'inserimento manuale (Testo + Tasto Aggiungi)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPlayerName,
                                onValueChange = { newPlayerName = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Nome") },
                                shape = RoundedCornerShape(20.dp),
                                leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) }
                            )

                            // Verifica se si può aggiungere (il campo non deve essere vuoto e il nome non deve già esistere)
                            val isAddPlayerEnabled = newPlayerName.trim().isNotEmpty() && viewModel.players.none { it.name.equals(newPlayerName.trim(), ignoreCase = true) }

                            Button(
                                onClick = {
                                    if (isAddPlayerEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                        // ---> LOGICA MULTICOLOR (Inserimento Manuale) <---
                                        // Stessa logica di prima: Unspecified = colore a caso.
                                        val finalColor = if (selectedColor == Color.Unspecified) {
                                            playerPalette.random()
                                        } else {
                                            selectedColor
                                        }

                                        viewModel.addPlayer(newPlayerName, finalColor.toArgb())
                                        newPlayerName = "" // Svuota il campo di testo

                                        // Riposizioniamo la selezione sul Pallino Arcobaleno per il prossimo giocatore!
                                        selectedColor = Color.Unspecified
                                    }
                                },
                                modifier = Modifier.padding(top = 6.dp).height(56.dp),
                                // ---> DIDATTICA UX: Bottone "Spento" con la regola del 12% / 38%
                                border = if (isAddPlayerEnabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    contentColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            ) { Text("Aggiungi") }
                        }
                    }
                }

                // ====================================================================
                // CARD 3: IL TAVOLO (Chi sta per giocare)
                // ====================================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Text(
                            text = "Giocatori al Tavolo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )

                        if (viewModel.players.isEmpty()) {
                            // STATO VUOTO: Illustrazione e testo
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.Group,
                                    contentDescription = "Vuoto",
                                    modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text("Il tavolo è vuoto!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                Text("Aggiungi qualcuno per iniziare la sfida.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                            }
                        } else {
                            // STATO PIENO: Generiamo una riga (Mini-Card) per ogni giocatore
                            viewModel.players.forEachIndexed { index, player ->
                                // =============================================================================
                                // Richiamiamo la funzione che abbiamo costruito nel file PlayerCardComponents
                                // =============================================================================
                                PlayerAtTableCard(
                                    player = player,
                                    isFirst = index == 0, // Vero se è il primo della lista
                                    isLast = index == viewModel.players.size - 1, // Vero se è l'ultimo
                                    onMoveUp = { viewModel.movePlayer(index, index - 1) },
                                    onMoveDown = { viewModel.movePlayer(index, index + 1) },
                                    onEdit = { playerToEdit = player },
                                    onRemove = {
                                        val removedIndex = index
                                        val removedPlayer = player
                                        viewModel.removePlayer(player)

                                        // Logica della Snackbar per l'annullamento (Undo)
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
                                    },
                                    onColorChange = { newColorArgb ->
                                        // Questa funzione fa aggiornare il colore salvato nel ViewModel
                                        viewModel.updatePlayerColor(player, newColorArgb)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ====================================================================
    // I 3 POPUP (Dialogs & BottomSheet)
    // ====================================================================

    // 1. POPUP MODIFICA NOME GIOCATORE AL TAVOLO
    // 1. POPUP MODIFICA NOME GIOCATORE AL TAVOLO
    if (playerToEdit != null) {
        var editedName by remember { mutableStateOf(playerToEdit!!.name) }
        AlertDialog(
            onDismissRequest = { playerToEdit = null },
            // Aggiungiamo il grassetto al titolo per renderlo più elegante
            title = { Text("Modifica Nome", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            // ---> GRAFICA PULSANTI AFFIANCATI <---
            // Mettiamo tutto dentro confirmButton per forzare la riga al 100% della larghezza
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Spazio esatto tra i due bottoni
                ) {

                    // TASTO ANNULLA (Sinistra)
                    OutlinedButton(
                        onClick = {
                            playerToEdit = null
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm) },
                        modifier = Modifier
                            .weight(1f) // Prende metà spazio
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // Usiamo colori neutri (outline e onSurface) come hai richiesto, niente rosso!
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Annulla", color = MaterialTheme.colorScheme.onSurface)
                    }

                    // TASTO SALVA (Destra)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (editedName.isNotBlank()) {
                                playerToEdit!!.name = editedName
                                playerToEdit = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f) // Prende l'altra metà dello spazio
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // Usiamo il PrimaryContainer (lo stesso azzurro/blu chiaro del tuo screenshot)
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            },
            // Avendo messo entrambi i pulsanti dentro confirmButton, diciamo ad Android
            // di spegnere il pulsante "Annulla" invisibile di default.
            dismissButton = null
        )
    }

    // 2. BOTTOM SHEET: GESTIONE PREFERITI
    if (showFavoritesDialog) {
        var newFavName by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showFavoritesDialog = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.93f).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Giocatori Rapidi", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { showFavoritesDialog = false }) { Icon(Icons.Filled.Close, "Chiudi") }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFavName,
                        onValueChange = { newFavName = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Nuovo nome") },
                        shape = RoundedCornerShape(16.dp)
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
                        modifier = Modifier.height(56.dp).padding(top = 6.dp),
                        border = if (isAddFavEnabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAddFavEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            contentColor = if (isAddFavEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) { Text("Aggiungi") }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                // La lista scorrevole dei preferiti nel popup
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {

                    // Usiamo itemsIndexed per avere sia la posizione (index) che il nome (fav)
                    itemsIndexed(viewModel.favoriteNames) { index, fav ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ---> SISTEMA DI RIORDINO (IDENTICO AL TAVOLO) <---
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Freccia SU
                                    IconButton(
                                        onClick = { viewModel.moveFavorite(index, index - 1) },
                                        // Disabilitato se è il primo elemento (non può andare più su di 0)
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.onSurface)
                                    }

                                    // Freccia GIÙ
                                    IconButton(
                                        onClick = { viewModel.moveFavorite(index, index + 1) },
                                        // Disabilitato se è l'ultimo elemento della lista
                                        enabled = index < viewModel.favoriteNames.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                // Nome del giocatore preferito
                                Text(
                                    text = fav,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f) // Occupa tutto lo spazio centrale
                                        .padding(start = 12.dp) // Lo stacca dalle frecce
                                )

                                // Tasto Modifica (Matita)
                                IconButton(onClick = { favToEdit = fav }) {
                                    Icon(Icons.Filled.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
                                }

                                // Tasto Elimina (Cestino)
                                IconButton(onClick = { viewModel.removeFavorite(fav) }) {
                                    Icon(Icons.Filled.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // (Sotto-Popup di modifica per un singolo preferito)
    if (favToEdit != null) {
        var editedFavName by remember { mutableStateOf(favToEdit!!) }
        AlertDialog(
            onDismissRequest = { favToEdit = null },
            title = { Text("Modifica Nome Rapido") },
            text = { OutlinedTextField(value = editedFavName, onValueChange = { editedFavName = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) },
            confirmButton = {
                Button(onClick = {
                    if (editedFavName.isNotBlank()) {
                        viewModel.editFavorite(favToEdit!!, editedFavName.trim())
                        favToEdit = null
                    }
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { favToEdit = null }) { Text("Annulla") } }
        )
    }

    // 3. POPUP: IMPOSTAZIONI DEL DADO (Esternalizzato in DiceComponents.kt)
    if (showDiceSettingsDialog) {
        DiceSettingsDialog(
            currentSides = viewModel.diceSides,
            onSidesChanged = { newSides -> viewModel.diceSides = newSides },
            onDismiss = { showDiceSettingsDialog = false }
        )
    }
}