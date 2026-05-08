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
import androidx.compose.ui.graphics.toArgb // Serve per convertire il Colore visivo in un numero da salvare nel Database
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    val keyboardController = LocalSoftwareKeyboardController.current//evochiamo il controllore della tastiera
    val focusManager = LocalFocusManager.current // Recuperiamo il gestore del focus, ci serve altrimenti anche se chiudiamo la tastiera la text area rimane sempre su OnFocus (quindi attiva)

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
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
            //contentPadding = PaddingValues(bottom = 32.dp) // Cuscinetto in fondo prima della BottomBar
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

                // ==============================================================
                // --- CARD 1: REGOLE DEL GIOCO ---
                // ==============================================================
                // 🧠 TEORIA COMPOSE: Usiamo la Card come contenitore principale.
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // ==========================================================
                        // 🧠 UX: Intestazione Iconica e Bottone Dado
                        // ==========================================================
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                /*Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Impostazioni Regole",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )*/
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Regole del Gioco",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // ---> IL BOTTONE DEL DADO <---
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

                        // ==========================================================
                        // CAMPO DI TESTO: NOME SFIDA
                        // ==========================================================
                        OutlinedTextField(
                            // FIX: Richiamiamo correttamente il viewModel!
                            value = viewModel.matchTitle,
                            onValueChange = {
                                viewModel.matchTitle = it
                                if (it.isNotBlank()) showError = false // Spegne l'errore rosso
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
                            // ==========================================================
                            // UX FIX: CHIUSURA TASTIERA
                            // ==========================================================
                            singleLine = true, // Impedisce di andare a capo
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), // Mostra il tasto "Fatto/Spunta"
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() } // Azione: abbassa la tastiera
                            )
                        )
                        /**
                         * ============================================================================
                         * Le 'ImeAction' (Azioni della tastiera in basso a destra)
                         * ============================================================================
                         * L'ImeAction cambia l'icona del tasto di conferma della tastiera Android
                         * per far capire all'utente cosa succederà premendolo:
                         *
                         * 1. ImeAction.Default : Il classico tasto "Invio" (va a capo riga).
                         * 2. ImeAction.Done    : Mostra "✔️" (Fatto). Significa "Ho finito, chiudi la tastiera".
                         * 3. ImeAction.Next    : Mostra "➡️" (Avanti). Passa automaticamente al campo di testo successivo.
                         * 4. ImeAction.Search  : Mostra "🔍" (Cerca). Avvia una ricerca globale.
                         * 5. ImeAction.Send    : Mostra "✈️" (Invia). Perfetto per le app di messaggistica.
                         * 6. ImeAction.Go      : Mostra "🚀" (Vai). Esegue l'input immediato (es. aprire un link nel browser).
                         * 7. ImeAction.Previous: Torna al campo di testo precedente.
                         * * 👉 Ricorda: per farle funzionare bene, usa quasi sempre 'singleLine = true' nel TextField!
                         */



                        // ==========================================================
                        // CAMPO DI TESTO: TRAGUARDO (Solo Numeri + Chiusura Smart)
                        // ==========================================================
                        OutlinedTextField(
                            // FIX: Richiamiamo correttamente il viewModel!
                            value = viewModel.targetScore,
                            onValueChange = { newValue ->
                                // Accetta solo numeri o campo vuoto
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    viewModel.targetScore = newValue
                                }
                            },
                            label = { Text("Traguardo (Opzionale)") },
                            modifier = Modifier.fillMaxWidth(),
                            // ==========================================================
                            // UX FIX: CHIUSURA TASTIERA
                            // ==========================================================
                            singleLine = true, // Impedisce di andare a capo
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ), // Mostra il tasto "Fatto/Spunta"

                            // Si attiva il comando quando l'utente preme il tasto spunta
                            keyboardActions = KeyboardActions(
                                onDone = {

                                    /*Trucco per il futuro non utilizzato qui:
                                    Esiste il comando:
                                    keyboardController?.hide() 1.
                                    Esso nasconde la tastiera fisica quando si preme il pulsante in basso a destra di conferma
                                    In questo caso non ci serve perchè usando il comando clearFocus Jetpack Compose
                                    capisce da solo che deve togliere non solo il focus sulla casella ma anche la tastiera aperta.
                                    Quindi, questo codice potrebbe tornare utile quando bisogna togliere solo la testiera ma non il focus.
                                    */

                                    // Eseguiamo il comando
                                    focusManager.clearFocus()  // 2. Togliamo il cursore e il bordo attivo (Focus)
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        )


                        Spacer(modifier = Modifier.height(16.dp))

                        // ==========================================================
                        // 🧠 UI: SELEZIONE TEMA RAPIDO tramite TonalButton
                        // ==========================================================
                        val isAnimeTheme = viewModel.matchTitle == "Sfida Anime"
                        val isCarteTheme = viewModel.matchTitle == "Sfida Carte"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Tasto TEMA ANIME
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
                                // 🧠 FIX VISIBILITÀ: Aggiungiamo un bordo con il colore primario quando il tasto NON è selezionato.
                                border =  BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Default.Tv, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Anime")
                            }

                            // Tasto TEMA CARTE
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
                                // 🧠 FIX VISIBILITÀ: Aggiungiamo un bordo con il colore primario quando il tasto NON è selezionato.
                                border =  BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Default.Style, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Carte")
                            }
                        }
                    }
                }

                // ====================================================================
                // --- CARD 2: GESTIONE PARTECIPANTI (Giocatori Rapidi e Manuali) ---
                // ====================================================================
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // ==========================================================
                        // 🧠 UX: Intestazione con Icona
                        // ==========================================================
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

                        // --- 1. SEZIONE GIOCATORI RAPIDI (PREFERITI) ---
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
                            // Tasto Ingranaggio per aprire la gestione dei preferiti
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                showFavoritesDialog = true
                            }) {
                                Icon(Icons.Filled.Settings, "Gestisci Giocatori Rapidi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                            }
                        }

                        // ==========================================================
                        // 🧠 UX/UI: GIOCATORI RAPIDI (FilterChips)
                        // Trasformiamo i vecchi bottoni giganti in "Pillole" eleganti.
                        // ==========================================================
                        if (viewModel.favoriteNames.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(end = 16.dp)
                            ) {
                                // FIX: Usiamo 'favoriteNames' dal ViewModel!
                                items(viewModel.favoriteNames) { fav ->
                                    // STATE: Controlliamo se è già al tavolo leggendo dal ViewModel
                                    val isAlreadyAtTable = viewModel.players.any { it.name.equals(fav, ignoreCase = true) }

                                    // ==========================================================
                                    // Dimensione Chip e Doppia Vibrazione
                                    // ==========================================================
                                    // 🎓 NUOVA LEZIONE: Dimensione Chip e Vibrazione

                                    // 2. Non scriviamo NESSUN comando di vibrazione (haptic) qui dentro!
                                    //    Il telefono vibra già da solo quando si preme un Chip.
                                    //    Scriverlo a mano causava il fastidioso "doppio colpo".
                                    // ==========================================================
                                    FilterChip(
                                        selected = isAlreadyAtTable,
                                        onClick = {
                                            if (!isAlreadyAtTable) {
                                                // --- LOGICA AGGIUNTA (GIÀ PRESENTE) ---
                                                val finalColor = if (selectedColor == Color.Unspecified) {
                                                    val usedColors = viewModel.players.map { it.color }
                                                    val availableColors = playerPalette.filter { it.toArgb() !in usedColors }
                                                    if (availableColors.isNotEmpty()) availableColors.random() else playerPalette.random()
                                                } else {
                                                    selectedColor
                                                }
                                                viewModel.addPlayer(fav, finalColor.toArgb())
                                            } else {
                                                // ==========================================================
                                                // 🧠 FIX DEFINITIVO: VIBRAZIONE INTELLIGENTE (Smart Haptic)
                                                // ==========================================================
                                                // Se c'è un solo giocatore al tavolo, sappiamo che l'animazione
                                                // dell'Empty State annullerà la vibrazione di sistema.
                                                // Quindi, in questo caso specifico, la forziamo a mano!
                                                if (viewModel.players.size == 1) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                // Se ci sono più giocatori (> 1), NON mettiamo nessun haptic manuale
                                                // così non si crea la fastidiosa doppia vibrazione.

                                                // Cerchiamo l'oggetto Player corrispondente al nome della chip
                                                val playerToRemove = viewModel.players.find { it.name.equals(fav, ignoreCase = true) }

                                                // Se lo troviamo, chiamiamo il metodo del viewModel per eliminarlo
                                                playerToRemove?.let {
                                                    viewModel.removePlayer(it)
                                                }
                                            }
                                        },
                                        // Ingrandiamo il riquadro in modo sicuro!
                                        // 1. Usiamo 'defaultMinSize' per dare al chip un'altezza minima
                                        //    più grande (48.dp), rendendolo molto più comodo da premere.
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        label = { Text(fav, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        // 🧠 FIX VISIBILITÀ: Forziamo il bordo a prendere il colore primario del sistema.
                                        // Questo li renderà visibilissimi e super eleganti anche sui temi più scuri!
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isAlreadyAtTable,
                                            borderColor = MaterialTheme.colorScheme.primary, // <--- ECCO LA MAGIA QUI
                                            selectedBorderColor = Color.Transparent // Quando è premuto (tutto colorato), togliamo il bordo
                                        )
                                    )

                                }
                            }
                        } else {
                            Text("Nessun giocatore rapido salvato.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // --- 2. DIVISORE ---
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- 3. SEZIONE INSERIMENTO MANUALE E SCELTA COLORE ---
                        Text("Scegli un colore:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                        // La Tavolozza importata da SharedUtils
                        ColorPickerRow(
                            selectedColor = selectedColor,
                            onColorSelected = { 
                                // 🧠 UX: Vibrazione di conferma quando si tocca un colore
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                selectedColor = it 
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done,
                                ),
                                        keyboardActions = KeyboardActions(
                                        onDone = {
                                            // 🧠 UX FIX DEFINITIVO: Usiamo SOLO clearFocus().
                                            // Togliendo il focus, Android capisce da solo che deve chiudere
                                            // la tastiera, eseguendo un'unica animazione fluida e perfetta!
                                            focusManager.clearFocus()
                                        }
                                        )
                            )


                            // Verifica di sicurezza (Nomi unici)
                            val isAddPlayerEnabled = newPlayerName.trim().isNotEmpty() && viewModel.players.none { it.name.equals(newPlayerName.trim(), ignoreCase = true) }

                            // 2. PULSANTE AGGIUNGI
                            Button(
                                onClick = {
                                    if (isAddPlayerEnabled) {
                                        //haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
                                modifier = Modifier.padding(top = 6.dp).height(63.dp),shape = RoundedCornerShape(20.dp),
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
                // --- CARD 3: IL TAVOLO (Chi sta per giocare) ---
                // ====================================================================
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), // Padding bottom per staccarlo dalla BottomBar del pulsante Inizia
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // 🧠 UX: Intestazione con Icona + Badge Contatore
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

                            // 🧠 COMPONENTE M3: Il Badge!
                            // Legge la grandezza della lista ufficiale 'viewModel.players'
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = "${viewModel.players.size}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // ==========================================================
                        // 🧠 TEORIA UX/UI: L'EMPTY STATE ("Stato Vuoto")
                        // ==========================================================
                        if (viewModel.players.isEmpty()) {
                            // La Surface crea un "buco" visivo usando un colore più scuro (surface)
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
                            // ==========================================================
                            // STATO PIENO: Generiamo una riga per ogni giocatore
                            // 🧠 FIX: Leggiamo direttamente da 'viewModel.players'
                            // ==========================================================
                            viewModel.players.forEachIndexed { index, player ->
                                PlayerAtTableCard(
                                    player = player,
                                    isFirst = index == 0,
                                    isLast = index == viewModel.players.size - 1,

                                    // ---> DELEGAZIONE AL VIEWMODEL (State Hoisting) <---
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
                                    /*onColorChange = { newColorArgb ->
                                        viewModel.updatePlayerColor(player, newColorArgb)
                                    }*/
                                )
                                // Piccolo spazio extra tra una card e l'altra per farle respirare
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

    // ====================================================================
    // I 3 POPUP (Dialogs & BottomSheet)
    // ====================================================================


    // ====================================================================
    // 1. POPUP UNIFICATO: MODIFICA NOME E COLORE GIOCATORE
    // ====================================================================
    if (playerToEdit != null) {
        // Inizializzazione dello stato locale per gestire le modifiche senza intaccare il database fino al "Salva"
        var editedName by remember { mutableStateOf(playerToEdit!!.name) }
        var editedColor by remember { mutableIntStateOf(playerToEdit!!.color) }

        AlertDialog(
            onDismissRequest = { playerToEdit = null }, // Chiude il modale se si clicca fuori
            title = {
                Text(
                    text = "Modifica Giocatore",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                // Column organizza gli elementi in verticale seguendo l'ordine di scrittura
                Column(modifier = Modifier.fillMaxWidth()) {

                    // Etichetta descrittiva per la sezione colori
                    Text(
                        text = "Scegli un nuovo colore:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 🎨 GRIGLIA COLORI (Spostata in alto e rimpicciolita)
                    LazyVerticalGrid(
                        // GridCells.Adaptive adatta il numero di colonne alla larghezza disponibile
                        columns = GridCells.Adaptive(minSize = 35.dp),//con 35.dp faccio distribuire i colori su 2 colonne
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // Spazio orizzontale tra le icone
                        verticalArrangement = Arrangement.spacedBy(12.dp),   // Spazio verticale tra le righe
                        // heightIn imposta un limite massimo di altezza per evitare che la griglia spinga fuori il resto
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 20.dp)
                            .heightIn(max = 140.dp)
                    ) {
                        // Iterazione sulla palette predefinita definita nel ViewModel o nei componenti
                        items(playerPalette) { color ->
                            // Verifica se il colore corrente è quello attualmente selezionato
                            val isSelected = color.toArgb() == editedColor

                            Box(
                                modifier = Modifier
                                    .size(36.dp) // Dimensione ridotta per un look più raffinato
                                    .clip(RoundedCornerShape(12.dp)) // Arrotondamento coerente con lo stile app
                                    .background(color)
                                    // Disegna un bordo visibile solo se l'elemento è selezionato (feedback visivo)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    // Gestione dell'input: aggiorna lo stato locale e attiva il feedback tattile
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        editedColor = color.toArgb()
                                    }
                            )
                        }
                    }

                    // CAMPO DI TESTO PER IL NOME (Spostato sotto la griglia)
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it }, // Aggiorna la stringa temporanea ad ogni digitazione
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nuovo nome") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true, // Impedisce la creazione di nuove righe (fondamentale per ImeAction)
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        // keyboardActions intercetta la pressione del tasto di conferma sulla tastiera
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus() // Rimuove il cursore e abbassa la tastiera
                        })
                    )
                }
            },
            confirmButton = {
                // Row allinea i pulsanti d'azione orizzontalmente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pulsante per annullare l'operazione senza salvare i cambiamenti
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

                    // Pulsante per confermare e persistere le modifiche nel ViewModel/Database
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            // Validazione: procediamo al salvataggio solo se il nome non è vuoto
                            if (editedName.isNotBlank()) {
                                // Aggiornamento dei campi dell'oggetto Player originale
                                playerToEdit!!.name = editedName
                                viewModel.updatePlayerColor(playerToEdit!!, editedColor)

                                playerToEdit = null // Chiude il modale resettando la variabile di stato
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // Uso di PrimaryContainer per coerenza cromatica con il resto dell'interfaccia
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null, // Disattivato per usare la gestione personalizzata nella Row sopra
            shape = RoundedCornerShape(24.dp) // Arrotondamento massimo per il contenitore del Dialog
        )
    }

    // 2. BOTTOM SHEET: GESTIONE PREFERITI
    if (showFavoritesDialog) {
        var newFavName by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showFavoritesDialog = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.93f).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Text("Giocatori Rapidi",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        showFavoritesDialog = false }) {
                        Icon(Icons.Filled.Close, "Chiudi")}
                }

                // ==========================================================
                // RIGA INSERIMENTO NUOVO PREFERITO
                // 🧠 UI UNIFORMATA: Usiamo la stessa logica di altezza e curvatura
                // usata nella sezione "Aggiungi manualmente".
                // ==========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. CAMPO DI TESTO (Nome Rapido)
                    OutlinedTextField(
                        value = newFavName,
                        onValueChange = { newFavName = it },
                        // 🧠 FIX PROPORZIONI: Copiamo la "quadra" trovata dall'utente.
                        // Rimuoviamo l'altezza fissa lasciando che il componente respiri.
                        modifier = Modifier.weight(1f),
                        label = { Text("Nuovo nome") },
                        // 🎨 DESIGN: Stondatura a 20.dp per coerenza totale.
                        shape = RoundedCornerShape(20.dp),
                        // 🧠 AGGIUNTA ICONA: Inseriamo l'icona della persona per coerenza
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    val isAddFavEnabled = newFavName.trim().isNotEmpty() && viewModel.favoriteNames.none { it.equals(newFavName.trim(), ignoreCase = true) }

                    // 2. PULSANTE AGGIUNGI PREFERITO
                    Button(
                        onClick = {
                            if (isAddFavEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                viewModel.addFavorite(newFavName.trim())
                                newFavName = ""
                            }
                        },
                        // 🧠 FIX PROPORZIONI: Copiamo l'altezza di 63.dp e il padding top di 6.dp
                        modifier = Modifier.padding(top = 6.dp).height(64.dp),
                        // 🎨 DESIGN: Stondatura a 20.dp.
                        shape = RoundedCornerShape(20.dp),
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
                                        onClick = {
                                            // 🧠 UX: Aggiungiamo un feedback tattile leggero (Medium) per gli spostamenti
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.moveFavorite(index, index - 1)
                                        },
                                        // Disabilitato se è il primo elemento (non può andare più su di 0)
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.onSurface)
                                    }

                                    // Freccia GIÙ
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.moveFavorite(index, index + 1)
                                        },
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
                                IconButton(onClick = {
                                    // 🧠 UX: Vibrazione di conferma azione
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    favToEdit = fav
                                }) {
                                    Icon(Icons.Filled.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
                                }

                                // Tasto Elimina (Cestino)
                                IconButton(onClick = {
                                    // 🧠 UX: Vibrazione di conferma azione
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.removeFavorite(fav)
                                }) {
                                    Icon(Icons.Filled.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ====================================================================
    // SOTTO-POPUP: MODIFICA NOME GIOCATORE RAPIDO
    // 🧠 DESIGN CONSISTENCY: Usiamo lo stesso identico stile del popup
    // "Modifica Nome" usato per i giocatori al tavolo.
    // ====================================================================
    if (favToEdit != null) {
        var editedFavName by remember { mutableStateOf(favToEdit!!) }
        AlertDialog(
            onDismissRequest = { favToEdit = null },
            // Aggiungiamo il grassetto al titolo per renderlo più elegante e coerente
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
                            // 🧠 UX: Vibrazione anche per l'annullamento
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            favToEdit = null
                        },
                        modifier = Modifier
                            .weight(1f) // 🧠 TEORIA UX: .weight(1f) divide lo spazio a metà esatta col bottone accanto
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // Usiamo colori neutri per il tasto secondario
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Annulla", color = MaterialTheme.colorScheme.onSurface)
                    }

                    // TASTO SALVA (Destra)
                    Button(
                        onClick = {
                            // 🧠 UX: Vibrazione di conferma salvataggio
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (editedFavName.isNotBlank()) {
                                viewModel.editFavorite(favToEdit!!, editedFavName.trim())
                                favToEdit = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f) // L'altra metà dello spazio
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // Usiamo il PrimaryContainer (lo stesso azzurro/blu chiaro del resto dell'app)
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            },
            // Spegniamo il dismissButton nativo perché abbiamo integrato "Annulla" nella Row
            dismissButton = null
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