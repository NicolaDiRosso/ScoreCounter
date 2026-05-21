package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.SheetValue // Controllo degli stati del BottomSheet per inibire gesture native
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged // Gestore dello stato di focus dei componenti
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb // Conversione cromatica per persistenza dati
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource // Import aggiunto per la traduzione dinamica delle stringhe
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R // Import del file R (Resources) per accedere all'ID delle traduzioni
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.ColorPickerRow
import com.n380.scorecounter.ui.components.PlayerAtTableCard
import com.n380.scorecounter.ui.components.playerPalette
import com.n380.scorecounter.viewmodel.MatchViewModel
import com.n380.scorecounter.ui.components.CustomSelectableChip
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
    // STATI LOCALI: GESTIONE GIOCATORI MANUALE
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

    // BLOCCO ANTI-CRASH (State Lock)
    // Variabile di Stato Booleana: funge da lucchetto per prevenire le Condizioni di Corsa (Race Conditions).
    var isNavigating by remember { mutableStateOf(false) }

    // STATI: CONFIGURAZIONE DADO
    var showDiceSettingsDialog by remember { mutableStateOf(false) }

    // FLAG DI AUTORIZZAZIONE CHIUSURA
    // Stato di controllo per la transizione verso 'Hidden'. Impedisce la chiusura tramite swipe native.
    var canDismissSheet by remember { mutableStateOf(false) }

    // GESTORE STATO BOTTOM SHEET
    // skipPartiallyExpanded = true: Inibisce lo stato di espansione intermedia.
    // confirmValueChange: Restituendo 'false' per 'Hidden' si blocca il gesto di chiusura verso il basso (swipe-to-dismiss).
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden || canDismissSheet
        }
    )

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

    // ====================================================================
    // ESTRAZIONE STRINGHE DI ERRORE PRE-ONCLICK (Regola Composable Context)
    // Estraiamo le stringhe tradotte qui, fuori dal bottone, per poterle usare liberamente in onClick
    // ====================================================================
    val errTitoloGiocatori = stringResource(R.string.err_titolo_e_giocatori) // Recupero testo tradotto per errore combinato
    val errSoloTitolo = stringResource(R.string.err_solo_titolo) // Recupero testo tradotto per errore titolo vuoto
    val errSoloGiocatori = stringResource(R.string.err_solo_giocatori) // Recupero testo tradotto per errore tavolo vuoto

    // ARCHITETTURA PAGINA (Scaffold)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Supporto per la visualizzazione del pattern grafico sottostante
        snackbarHost = { SnackbarHost(snackbarHostState) },//prepara lo spazio per le notifiche a comparsa

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
                        // Proprietà Lambda: Eseguita al tocco del pulsante "Inizia Sfida"
                        onClick = {
                            // 1. CONTROLLO LUCCHETTO
                            // Costrutto Logico: Se isNavigating è true, interrompe istantaneamente la funzione.
                            if (isNavigating) return@Button

                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                            // 2. LETTURA STATO ATTUALE
                            val isTitleValid = viewModel.matchTitle.isNotBlank()
                            val arePlayersPresent = viewModel.players.isNotEmpty()

                            // 3. DECISIONE (Bivio Logico)
                            if (isTitleValid && arePlayersPresent) {
                                // SALVATAGGIO CRONOLOGIA: Aggiunge il titolo corrente alla lista dei recenti
                                viewModel.addTitleToHistory(viewModel.matchTitle)

                                // CHIUSURA LUCCHETTO: Sigilliamo l'app per prevenire Race Conditions
                                isNavigating = true
                                onNavigateToCounter()
                            } else {
                                // ATTIVAZIONE ALLARMI VISIVI (Colora i bordi di rosso)
                                showError = true

                                // Variabile Locale: Determina il messaggio tramite l'Espressione Condizionale 'when'
                                val errorMessage = when {
                                    !isTitleValid && !arePlayersPresent -> errTitoloGiocatori // Sostituzione con la variabile tradotta
                                    !isTitleValid -> errSoloTitolo // Sostituzione con la variabile tradotta
                                    else -> errSoloGiocatori // Sostituzione con la variabile tradotta
                                }

                                // Esecuzione Asincrona: Mostra la notifica a comparsa (Snackbar)
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
                            contentDescription = stringResource(R.string.desc_inizia_icon), // Sostituzione testo icona tradotto
                            modifier = Modifier.padding(end = 8.dp).size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_inizia_sfida), // Sostituzione con testo del bottone tradotto
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // ====================================================================
        // COLUMN STATICA
        // Con una Column l'architettura base NON scorre.
        // Il titolo resterà incollato in alto.
        // ====================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {

            // 1. IL TITOLO (Ora è fisso e fuori dal tavolo scorrevole)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.titolo_nuova_sfida),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp) // Ridotto un po' il margine
            )

            // ====================================================================
            // 2. IL GRANDE TAVOLO CONTENITIVO (La Card Unica)
            // ====================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // IL COMANDO WEIGHT
                    // 'weight(1f)' dice a questa Card: "Prenditi tutto lo spazio verticale
                    // che avanza tra il Titolo qui sopra e il Dock dei bottoni in basso".
                    .weight(1f)
                    .padding(bottom = 16.dp), // Impedisce che si incolli al dock inferiore
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp), // Stessa stondatura massiccia delle altre pagine
            ) {
                // ====================================================================
                // 3. L'AREA SCORREVOLE INTERNA
                // ====================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // SCORRIMENTO MANUALE
                        // Invece di usare una LazyColumn, usiamo verticalScroll su una Column.
                        // Questo è perfetto per moduli di inserimento dati (come questo) dove gli
                        // elementi sono pochi e non c'è bisogno di riciclarli dinamicamente in memoria.
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp), // Padding interno per distaccare le scritte dai bordi della Card

                    // Distanzia automaticamente le 3 sezioni in modo uniforme!
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // SEZIONE 1: REGOLE E DADO
                    Card(
                        // 1. La spaziatura è gestita dallo 'spacedBy' della Colonna madre
                        modifier = Modifier.fillMaxWidth(),
                        // 2. SMUSSATURA RIDOTTA: Una card interna deve avere angoli leggermente più piccoli
                        // di quella esterna (20.dp interno vs 24.dp esterno) per essere geometricamente piacevole.
                        shape = RoundedCornerShape(20.dp),
                        // 3. CONTRASTO COLORI: Usiamo 'surface' (colore pulito) per staccare dal 'surfaceVariant' del tavolo.
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        // 4. BORDO DELICATO: Usiamo 'outline' invece di 'primary' per non rendere l'interfaccia troppo pesante
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.MenuBook,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AutoResizedText(
                                        text = stringResource(R.string.titolo_regole_gioco), // Sostituzione titolo sezione tradotto
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                // SPAZIATORE DI SICUREZZA:
                                // Aggiungiamo un gap fisso di 10.dp.
                                // se il titolo dovesse diventare troppo lungo (es. su schermi piccoli),
                                // l'AutoResizedText inizierà a rimpicciolirsi PRIMA di toccare il bottone del dado,
                                // garantendo che ci sia sempre questo spazio minimo tra i due.
                                Spacer(modifier = Modifier.width(10.dp))

                                // Pulsante per le impostazioni del dado
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        showDiceSettingsDialog = true
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 0.dp
                                    ),
                                    modifier = Modifier.height(44.dp), // Aumentata l'altezza per migliore touch target
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Casino,
                                        stringResource(R.string.desc_dado_icon),
                                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                    ) // Sostituzione testo descrittivo tradotto
                                    AutoResizedText(
                                        text = stringResource(
                                            R.string.label_dado,
                                            viewModel.diceSides
                                        ), // Formattazione stringa dinamica in base alle risorse (%d)
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // INPUT: TITOLO SFIDA
                            OutlinedTextField(
                                value = viewModel.matchTitle,
                                onValueChange = {
                                    viewModel.matchTitle = it
                                    // Se l'utente scrive qualcosa, rimuoviamo l'eventuale segnale di errore rosso
                                    if (it.isNotBlank()) showError = false
                                },
                                label = { Text(stringResource(R.string.hint_nome_sfida)) }, // Sostituzione etichetta campo tradotta
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                isError = showError && viewModel.matchTitle.isBlank(),
                                // GESTIONE SPAZIO DINAMICO: Se non c'è errore, impostiamo supportingText a null per far "collassare"
                                // lo spazio vuoto inferiore e permettere ai titoli recenti di stare più vicini al box.
                                supportingText = if (showError && viewModel.matchTitle.isBlank()) {
                                    {
                                        Text(
                                            stringResource(R.string.err_nome_sfida_obbligatorio),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } // Sostituzione errore sotto input tradotto
                                } else null,
                                leadingIcon = {
                                    val iconColor =
                                        if (showError && viewModel.matchTitle.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    Icon(Icons.Default.VideogameAsset, null, tint = iconColor)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                            )

                            // CRONOLOGIA TITOLI RECENTI (SISTEMA DI SUGGERIMENTO RAPIDO)
                            // La sezione viene mantenuta sempre visibile per garantire stabilita' al layout ed evitare spostamenti 
                            // improvvisi degli elementi (layout shift) durante l'interazione con l'interfaccia utente.
                            if (viewModel.matchTitleHistory.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                                ) {
                                    AutoResizedText(
                                        text = stringResource(R.string.label_titoli_recenti),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Cicliamo i titoli salvati nel ViewModel per permettere una selezione immediata da parte dell'utente.
                                        items(viewModel.matchTitleHistory) { recentTitle ->
                                            // Valutazione dello stato derivato:
                                            // Verifichiamo se il titolo di questo specifico elemento dell'elenco (recentTitle)
                                            // coincide con la stringa attualmente registrata nel ViewModel (matchTitle).
                                            // Questo parametro booleano guiderà il rendering del componente (stile pieno vs stile vuoto).
                                            val isSelected = viewModel.matchTitle == recentTitle

                                            // Richiamo del componente UI custom centralizzato.
                                            // L'astrazione grafica (colori, bordi, padding) è gestita internamente in SharedUtils.kt,
                                            // qui passiamo esclusivamente i dati e i comportamenti di business logic (Principio DRY).
                                            CustomSelectableChip(
                                                text = recentTitle,
                                                isSelected = isSelected,
                                                onClick = {
                                                    // Gestione dinamica del feedback tattile (Micro-interazione):
                                                    // - LongPress (vibrazione lunga) se l'utente sta deselezionando un elemento già attivo.
                                                    // - Confirm (vibrazione breve) se l'utente sta effettuando una nuova selezione.
                                                    haptic.performHapticFeedback(
                                                        if (isSelected) HapticFeedbackType.LongPress
                                                        else HapticFeedbackType.Confirm
                                                    )

                                                    // Logica di Toggle (Interruttore):
                                                    // Il click altera direttamente la "Single Source of Truth" (il ViewModel).
                                                    // Se l'elemento cliccato era già quello attivo, si svuota il campo (deselezione).
                                                    // Altrimenti, viene sovrascritto col nuovo valore.
                                                    // Qualsiasi mutazione di viewModel.matchTitle provocherà l'immediata
                                                    // Ricomposizione (Recomposition) di tutti i nodi UI che la osservano.
                                                    if (isSelected) {
                                                        viewModel.matchTitle = ""
                                                    } else {
                                                        viewModel.matchTitle = recentTitle
                                                        showError = false // Azzera eventuali flag di errore visivo per input mancante
                                                    }

                                                    // Rimuove l'ancoraggio (focus) dal TextField principale e chiude
                                                    // contestualmente l'eventuale tastiera software aperta.
                                                    focusManager.clearFocus()
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Spaziatore di sicurezza per mantenere le proporzioni verticali costanti nel caso in cui la cronologia sia vuota.
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // INPUT: PUNTEGGIO OBIETTIVO (Filtro numerico)
                            OutlinedTextField(
                                value = viewModel.targetScore,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        viewModel.targetScore = newValue
                                    }
                                },
                                label = { Text(stringResource(R.string.hint_traguardo)) }, // Sostituzione label tradotta
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
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                            )
                        }
                    }

                    // SEZIONE 2: PARTECIPANTI (Input e Preferiti)
                    Card(
                        // 1. La spaziatura è gestita dallo 'spacedBy' della Colonna madre
                        modifier = Modifier.fillMaxWidth(),
                        // 2. SMUSSATURA RIDOTTA: Una card interna deve avere angoli leggermente più piccoli
                        // di quella esterna (20.dp interno vs 24.dp esterno) per essere geometricamente piacevole.
                        shape = RoundedCornerShape(20.dp),
                        // 3. CONTRASTO COLORI: Usiamo 'surface' (colore pulito) per staccare dal 'surfaceVariant' del tavolo.
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        // 4. BORDO DELICATO: Usiamo 'outline' invece di 'primary' per non rendere l'interfaccia troppo pesante
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PersonAddAlt1,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.titolo_gestione_partecipanti), // Sostituzione titolo sezione
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // GIOCATORI RAPIDI: Selezione da elenco preferiti
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_giocatori_rapidi), // Sostituzione label
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showFavoritesDialog = true
                                }) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        stringResource(R.string.desc_gestisci_rapidi_icon),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(25.dp)
                                    ) // Traduzione label accessibilità
                                }
                            }

                            if (viewModel.favoriteNames.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(end = 16.dp)
                                ) {
                                    items(viewModel.favoriteNames) { fav ->
                                        // Scansione iterativa della lista giocatori attuale:
                                        // L'operatore '.any {}' attraversa l'array e restituisce true non appena
                                        // trova ALMENO UN elemento che soddisfa la condizione.
                                        // Viene utilizzato 'ignoreCase = true' per prevenire duplicati logici
                                        // (es. "Marco" e "marco" sono considerati lo stesso giocatore).
                                        val isAlreadyAtTable = viewModel.players.any {
                                            it.name.equals(fav, ignoreCase = true)
                                        }

                                        // Riutilizzo dello stesso identico componente dei titoli.
                                        // Questo garantisce che i comportamenti tattili, le animazioni e gli stati visivi
                                        // rimangano intrinsecamente coerenti in tutto l'ecosistema dell'app.
                                        CustomSelectableChip(
                                            text = fav,
                                            isSelected = isAlreadyAtTable,
                                            onClick = {
                                                // Bivio logico in base alla presenza o meno del giocatore al tavolo
                                                if (!isAlreadyAtTable) {
                                                    // CASO A: INSERIMENTO (Il giocatore non è seduto al tavolo)

                                                    // Algoritmo di auto-assegnazione del colore a basso conflitto:
                                                    val finalColor = if (selectedColor == Color.Unspecified) {
                                                        // 1. Estrazione in un nuovo array di tutti i codici colore attualmente in uso.
                                                        val usedColors = viewModel.players.map { it.color }
                                                        // 2. Filtraggio della palette master: si tengono solo i colori NON presenti in usedColors.
                                                        val availableColors = playerPalette.filter { it.toArgb() !in usedColors }
                                                        // 3. Fallback: se ci sono colori intonsi se ne pesca uno, altrimenti
                                                        // la palette è esaurita e si pesca randomicamente accettando il duplicato visivo.
                                                        if (availableColors.isNotEmpty()) availableColors.random() else playerPalette.random()
                                                    } else {
                                                        // Bypass dell'algoritmo se l'utente ha esplicitamente selezionato un colore dal ColorPicker.
                                                        selectedColor
                                                    }

                                                    // Passaggio della richiesta di istanziazione al ViewModel.
                                                    viewModel.addPlayer(fav, finalColor.toArgb())

                                                } else {
                                                    // CASO B: RIMOZIONE (Il giocatore è già seduto al tavolo -> Toggle Deselezione)

                                                    // Gestione feedback aptico di allerta se si sta tentando di rimuovere
                                                    // l'ultimo elemento rimasto nella lista dei partecipanti.
                                                    if (viewModel.players.size == 1) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }

                                                    // Identificazione del target: '.find {}' restituisce il primo oggetto Player
                                                    // la cui proprietà 'name' corrisponde alla query, restituendo null se non trovato.
                                                    val playerToRemove = viewModel.players.find {
                                                        it.name.equals(fav, ignoreCase = true)
                                                    }

                                                    // Esecuzione in Safe-Call (?): la rimozione viene propagata al ViewModel
                                                    // esclusivamente se l'oggetto playerToRemove non è null.
                                                    playerToRemove?.let { viewModel.removePlayer(it) }
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    stringResource(R.string.msg_nessun_giocatore_rapido),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ) // Sostituzione Empty State giocatori
                            }

                            Spacer(modifier = Modifier.height(15.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.2f
                                )
                            )
                            Spacer(modifier = Modifier.height(7.dp))

                            // SEZIONE AGGIUNTA MANUALE E COLORE
                            Text(
                                stringResource(R.string.label_scegli_colore),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            ) // Traduzione istruzione colore

                            ColorPickerRow(
                                selectedColor = selectedColor,
                                onColorSelected = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    selectedColor = it
                                },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.label_aggiungi_manualmente),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            ) // Traduzione label manuale
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlayerName,
                                    onValueChange = { newPlayerName = it },
                                    modifier = Modifier.weight(1f).height(63.dp),
                                    // Sostituiamo 'label' con 'placeholder' per eliminare il padding
                                    // invisibile superiore e far combaciare l'ingombro logico con quello visivo
                                    placeholder = { Text(stringResource(R.string.hint_nome_giocatore)) }, // Traduzione segnaposto input utente
                                    shape = RoundedCornerShape(20.dp),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Person,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    )
                                )

                                val isAddPlayerEnabled = newPlayerName.trim()
                                    .isNotEmpty() && viewModel.players.none {
                                    it.name.equals(
                                        newPlayerName.trim(),
                                        ignoreCase = true
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (isAddPlayerEnabled) {
                                            val finalColor =
                                                if (selectedColor == Color.Unspecified) {
                                                    playerPalette.random()
                                                } else {
                                                    selectedColor
                                                }
                                            viewModel.addPlayer(newPlayerName, finalColor.toArgb())
                                            newPlayerName = ""
                                            selectedColor = Color.Unspecified
                                        }
                                    },
                                    modifier = Modifier.height(63.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    border = if (isAddPlayerEnabled) null else BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.12f
                                        ),
                                        contentColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        )
                                    )
                                ) { Text(stringResource(R.string.btn_aggiungi)) } // Traduzione bottone aggiunta
                            }
                        }
                    }

                    // SEZIONE 3: TAVOLO PARTECIPANTI (Elenco Attivo)
                    Card(
                        // 1. La spaziatura è gestita dallo 'spacedBy' della Colonna madre
                        modifier = Modifier.fillMaxWidth(),
                        // 2. SMUSSATURA RIDOTTA: Una card interna deve avere angoli leggermente più piccoli
                        // di quella esterna (20.dp interno vs 24.dp esterno) per essere geometricamente piacevole.
                        shape = RoundedCornerShape(20.dp),
                        // 3. CONTRASTO COLORI: Usiamo 'surface' (colore pulito) per staccare dal 'surfaceVariant' del tavolo.
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        // 4. BORDO DELICATO: Usiamo 'outline' invece di 'primary' per non rendere l'interfaccia troppo pesante
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Groups,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.titolo_tavolo_partecipanti), // Traduzione intestazione tabella
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        text = "${viewModel.players.size}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 2.dp
                                        ),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            // EMPTY STATE: Visualizzazione di cortesia in assenza di partecipanti
                            if (viewModel.players.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Filled.PersonAdd,
                                            null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AutoResizedText(
                                            stringResource(R.string.msg_tavolo_vuoto),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.Bold
                                        ) // Sostituzione warning
                                        AutoResizedText(
                                            stringResource(R.string.desc_tavolo_vuoto),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                                        ) // Sostituzione descrizione
                                    }
                                }
                            } else {
                                // Generazione dinamica delle card individuali per i partecipanti
                                viewModel.players.forEachIndexed { index, player ->

                                    // ESTRAZIONE STRINGHE SNACKBAR PRIMA DELLA LAMBDA OCLICK
                                    val msgRimozione = stringResource(
                                        R.string.msg_giocatore_rimosso,
                                        player.name
                                    ) // Inserimento stringa dinamica tradotta
                                    val btnAnnullaUndo =
                                        stringResource(R.string.btn_annulla_undo) // Recupero testo tradotto

                                    PlayerAtTableCard(
                                        player = player,
                                        isFirst = index == 0,
                                        isLast = index == viewModel.players.size - 1,
                                        onMoveUp = { viewModel.movePlayer(index, index - 1) },
                                        onMoveDown = { viewModel.movePlayer(index, index + 1) },
                                        onEdit = { playerToEdit = player },
                                        // Proprietà Lambda (Callback): Eseguita quando l'utente tocca il cestino
                                        onRemove = {
                                            // Costrutto Logico (Guard Statement):
                                            // Blocca l'esecuzione se l'app sta già cambiando pagina
                                            if (!isNavigating) {
                                                val removedIndex = index
                                                val removedPlayer = player
                                                // Metodo della Classe MatchViewModel: Elimina il dato dalla memoria
                                                viewModel.removePlayer(player)

                                                coroutineScope.launch {
                                                    launch { delay(3000L); snackbarHostState.currentSnackbarData?.dismiss() }
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = msgRimozione, // Utilizzo variabile tradotta
                                                        actionLabel = btnAnnullaUndo, // Utilizzo variabile tradotta
                                                        duration = SnackbarDuration.Indefinite
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.restorePlayer(
                                                            removedIndex,
                                                            removedPlayer
                                                        )
                                                    }
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
    }

    // MODALE UNIFICATO: MODIFICA GIOCATORE (Nome e Colore)
    if (playerToEdit != null) {
        var editedName by remember { mutableStateOf(playerToEdit!!.name) }
        var editedColor by remember { mutableIntStateOf(playerToEdit!!.color) }

        AlertDialog(
            onDismissRequest = { playerToEdit = null },
            title = {
                AutoResizedText(
                    text = stringResource(R.string.titolo_modifica_giocatore), // Traduzione intestazione dialogo
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AutoResizedText(
                        text = stringResource(R.string.label_nuovo_colore), // Traduzione label colore
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
                        label = { Text(stringResource(R.string.hint_nuovo_nome)) }, // Traduzione input field
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
                        Text(stringResource(R.string.btn_annulla), color = MaterialTheme.colorScheme.onSurface) // Traduzione bottone annulla
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
                        Text(stringResource(R.string.btn_salva), fontWeight = FontWeight.Bold) // Traduzione bottone salvataggio
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
            onDismissRequest = { showFavoritesDialog = false },
            sheetState = sheetState,

            // ====================================================================
            // EDGE-TO-EDGE: Override delle Window Insets di Sistema
            // ====================================================================
            // Il ModalBottomSheet di Material 3 inietta automaticamente uno spazio vuoto
            // sul fondo (WindowInsets.navigationBars) per evitare sovrapposizioni.
            // Questo spazio esterno sollevava la nostra Surface, mostrando il colore
            // del container (surfaceVariant) nel "buco" rimasto sotto, creando il taglio netto.
            // Passando WindowInsets(0, 0, 0, 0), annulliamo questo cuscino forzato.
            // Il pannello si estenderà fino all'ultimo pixel in basso, e il colore del nostro
            // Dock riempirà tutto lo spazio. L'ingombro di sicurezza è gestito internamente
            // dal '.navigationBarsPadding()' applicato alla Column del pulsante.
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            // ====================================================================
            // IL CONTENITORE BOX (Z-Index e Allineamento 2D)
            // ====================================================================
            // ERRORE SPIEGATO: Il ModalBottomSheet crea implicitamente una 'Column'.
            // Avvolgendo tutto il nostro layout in un 'Box', creiamo un "piano di appoggio" 2D.
            // Solo operando all'interno di un 'BoxScope', il modificatore '.align()'
            // sblocca la capacità di accettare direzioni verticali come 'BottomCenter',
            // permettendoci di sovrapporre la Snackbar in basso.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.80f) // Vincolo di altezza costante
            ) {
                // 🧠 GERARCHIA VERTICALE: Struttura a Layout Frazionato
                // Dividiamo lo spazio del BottomSheet in due layer non sovrapposti.
                Column(modifier = Modifier.fillMaxSize()) {

                    // LAYER 1: CONTENUTO SCORREVOLE (Area Dinamica)
                    Column(
                        modifier = Modifier
                            .weight(1f) // Espansione per occupare lo spazio sovrapposto al dock
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp) // Piccolo respiro dall'alto
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            // L'allineamento a sinistra distribuisce il peso visivo verso il titolo
                            horizontalArrangement = Arrangement.Absolute.Left,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.titolo_giocatori_rapidi_sheet), // Sostituzione titolo pannello tradotto
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // INSERIMENTO RAPIDO
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
                                placeholder = { Text(stringResource(R.string.hint_nuovo_nome)) }, // Sostituzione con segnaposto tradotto
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
                            ) { Text(stringResource(R.string.btn_aggiungi)) } // Traduzione del testo del bottone
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // AREA ELENCO PREFERITI: Struttura a "Tavolo" coerente con il design system dell'app
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
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
                                            // CONTROLLI DI RIORDINO
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
                                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                                            )

                                            // GESTIONE RECORD
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                favToEdit = fav
                                            }) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    stringResource(R.string.desc_modifica_icon), // Testo descrittivo dell'azione tradotto
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // ESTRAZIONE STRINGHE SNACKBAR (Dentro contesto LazyColumn Composable)
                                            val msgFavRimosso = stringResource(R.string.msg_rapido_rimosso, fav) // Stringa dinamica tradotta
                                            val btnAnnullaFav = stringResource(R.string.btn_annulla_undo) // Azione tradotta

                                            // Gestione eliminazione
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                                                // Salvataggio dei riferimenti puntuali per la closure
                                                val removedIndex = index
                                                val removedFav = fav
                                                // Mutazione dello stato (viene riflessa istantaneamente dalla UI)
                                                viewModel.removeFavorite(fav)

                                                // Avvio dell'orchestrazione della notifica modale
                                                coroutineScope.launch {
                                                    // Chiusura auto-temporizzata di sicurezza
                                                    launch {
                                                        delay(3000L)
                                                        // Utilizziamo lo stato dedicato al Bottom Sheet!
                                                        sheetSnackbarHostState.currentSnackbarData?.dismiss()
                                                    }

                                                    // Invocazione bloccante (suspend): attende input dell'utente o timeout
                                                    val result =
                                                        sheetSnackbarHostState.showSnackbar(
                                                            message = msgFavRimosso, // Utilizzo della variabile dinamica
                                                            actionLabel = btnAnnullaFav, // Utilizzo della label per undo
                                                            duration = SnackbarDuration.Indefinite
                                                        )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.restoreFavorite(removedIndex, removedFav)
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Filled.Delete, stringResource(R.string.desc_elimina_icon), tint = MaterialTheme.colorScheme.error) // Traduzione label elimina
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }



                    // LAYER 2: DOCK DI CHIUSURA (Area Statica Ancorata)
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        // Geometria smussata solo in alto per integrare il componente al flusso sovrastante
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding() // Prevenzione occlusioni da barra di sistema
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Sequenza di chiusura orchestrata per preservare l'integrità delle animazioni
                                    coroutineScope.launch {
                                        canDismissSheet = true
                                        sheetState.hide()
                                        showFavoritesDialog = false
                                        canDismissSheet = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp), // Altezza imposta a standard Expressive
                                shape = RoundedCornerShape(20.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.desc_chiudi_icon), modifier = Modifier.padding(end = 8.dp).size(28.dp)) // Sostituzione label chiusura
                                Text(text = stringResource(R.string.btn_chiudi_gestione), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) // Sostituzione nome pulsante
                            }
                        }
                    }
                }

                // LAYER 3: HOST NOTIFICHE (Z-Index 1)
                SnackbarHost(
                    hostState = sheetSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Offset Y Positivo: Solleviamo la snackbar di 100 pixel per impedire
                        // che venga oscurata e resa inaccessibile dal volume del Dock sottostante.
                        .padding(bottom = 100.dp)
                )
            }
        }
    }

    // MODALE: MODIFICA NOME PREFERITO
    if (favToEdit != null) {
        var editedFavName by remember { mutableStateOf(favToEdit!!) }
        AlertDialog(
            onDismissRequest = { favToEdit = null },
            title = {
                Text(stringResource(R.string.titolo_modifica_nome_rapido), // Traduzione intestazione box modale
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
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(stringResource(R.string.btn_annulla), color = MaterialTheme.colorScheme.onSurface) // Sostituzione label annulla
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (editedFavName.isNotBlank()) {
                                viewModel.editFavorite(favToEdit!!, editedFavName.trim())
                                favToEdit = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.btn_salva), fontWeight = FontWeight.Bold) // Sostituzione label salvataggio
                    }
                }
            },
            dismissButton = null
        )
    }

    // DIALOG: IMPOSTAZIONI DADO
    if (showDiceSettingsDialog) {
        DiceSettingsDialog(
            currentSides = viewModel.diceSides,
            onSidesChanged = { newSides -> viewModel.diceSides = newSides },
            onDismiss = { showDiceSettingsDialog = false }
        )
    }
}
