package com.n380.scorecounter.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb // Conversione cromatica per persistenza dati
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource // Import aggiunto per la traduzione dinamica delle stringhe
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R // Import del file R (Resources) per accedere all'ID delle traduzioni
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.CustomSelectableChip
import com.n380.scorecounter.ui.components.FadedRightEdgeWrapper
import com.n380.scorecounter.ui.components.PlayerAtTableCard
import com.n380.scorecounter.ui.components.TonalActionPill
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
    // STATI LOCALI: GESTIONE GIOCATORI MANUALE
    var newPlayerName by remember { mutableStateOf("") }

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

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }


    val coroutineScope = rememberCoroutineScope()

    val keyboardController =
        LocalSoftwareKeyboardController.current // Controller per la gestione programmatica della tastiera
    val focusManager =
        LocalFocusManager.current // Gestore del focus per la rimozione del cursore attivo dalle aree di testo
    // ==========================================================
    // 🧠 FIX UX: GESTIONE DELLO SCORRIMENTO E ANIMAZIONE
    // ==========================================================
    // 1. Estraiamo lo stato dello scorrimento per poterlo comandare
    val scrollState = rememberScrollState()

    // 2. LaunchedEffect osserva la grandezza della lista giocatori.
    // Ogni volta che il numero cambia, esegue il codice all'interno.
    LaunchedEffect(viewModel.players.size) {
        // Se il tavolo è passato esattamente a 1 giocatore (il primo aggiunto)
        if (viewModel.players.size in 1..3) {
            // Aspettiamo 100 millisecondi: questo è FONDAMENTALE.
            // Dà il tempo a Compose di renderizzare graficamente la Sezione 3 e
            // calcolare quanto si è allungata la pagina, prima di iniziare a scorrere.
            delay(100)

            // Animazione fluida verso il fondo assoluto della pagina (maxValue)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // ====================================================================
    // ESTRAZIONE STRINGHE DI ERRORE PRE-ONCLICK (Regola Composable Context)
    // Estraiamo le stringhe tradotte qui, fuori dal bottone, per poterle usare liberamente in onClick
    // ====================================================================
    val errTitoloGiocatori =
        stringResource(R.string.err_titolo_e_giocatori) // Recupero testo tradotto per errore combinato
    val errSoloTitolo =
        stringResource(R.string.err_solo_titolo) // Recupero testo tradotto per errore titolo vuoto
    val errSoloGiocatori =
        stringResource(R.string.err_solo_giocatori) // Recupero testo tradotto per errore tavolo vuoto

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
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
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

                        // ---> MODIFICA QUI: Colleghiamo lo stato estratto in cima! <---
                        .verticalScroll(scrollState)

                        .padding(12.dp), // Padding interno per distaccare le scritte dai bordi della Card
                    // Distanzia automaticamente le 3 sezioni in modo uniforme di 10.dp
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // SEZIONE 1: REGOLE E DADO
                    Card(
                        // 1. La spaziatura è gestita dallo 'spacedBy' della Colonna madre
                        modifier = Modifier.fillMaxWidth(),
                        // 2. SMUSSATURA MORBIDA: Livello 2 (Moduli)
                        shape = RoundedCornerShape(20.dp),
                        // 3. CONTRASTO COLORI: Usiamo 'surface' (colore pulito) per staccare dal 'surfaceVariant' del tavolo.
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        // 4. BORDO DELICATO: Usiamo 'outline' invece di 'primary' per non rendere l'interfaccia troppo pesante
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
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
                                shape = RoundedCornerShape(20.dp),
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
                                    // ====================================================================
                                    // FADE OUT GRADIENT (Segnale visivo di scorrimento)
                                    // ====================================================================
                                    // Utilizziamo il nostro "Wrapper" personalizzato per aggiungere l'ombra
                                    // in modo pulito con una sola riga di codice.
                                    FadedRightEdgeWrapper(modifier = Modifier.fillMaxWidth()){

                                        // LIVELLO INFERIORE: La lista scorrevole
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            // PADDING FONDAMENTALE: Diamo 40.dp di spazio extra alla fine della corsa,
                                            // così l'ultimo elemento può essere tirato fuori dall'ombra.
                                            contentPadding = PaddingValues(end = 35.dp)
                                        ) {
                                            // Cicliamo i titoli salvati nel ViewModel per permettere una selezione immediata da parte dell'utente.
                                            items(viewModel.matchTitleHistory) { recentTitle ->
                                                // Valutazione dello stato derivato:
                                                // Verifichiamo se il titolo di questo specifico elemento dell'elenco (recentTitle)
                                                // coincide con la stringa attualmente registrata nel ViewModel (matchTitle).
                                                // Questo parametro booleano guiderà le decisioni grafiche.
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
                                        } // Fine LazyRow
                                    } // Fine Box Principale
                                }
                            } else {
                                // Spaziatore di sicurezza per mantenere le proporzioni verticali costanti nel caso in cui la cronologia sia vuota.
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            //liena orizzontale semi trasparente che funge sa separatore
                            Spacer(modifier = Modifier.height(7.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.2f
                                )
                            )
                            Spacer(modifier = Modifier.height(7.dp))

                            // INPUT: PUNTEGGIO OBIETTIVO (Filtro numerico)
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
                                        overflow = TextOverflow.Ellipsis)
                                        }, // Sostituzione label tradotta
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                shape = RoundedCornerShape(20.dp),
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
                        // 2. SMUSSATURA MORBIDA: Livello 2 (Moduli)
                        shape = RoundedCornerShape(20.dp),
                        // 3. CONTRASTO COLORI: Usiamo 'surface' (colore pulito) per staccare dal 'surfaceVariant' del tavolo.
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        // 4. BORDO DELICATO: Usiamo 'outline' invece di 'primary' per non rendere l'interfaccia troppo pesante
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
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
                                // ====================================================================
                                // PULSANTE "GESTISCI" CON TESTO E ICONA (Affordance chiara)
                                // ====================================================================
                                TonalActionPill(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        showFavoritesDialog = true
                                    }
                                ) {
                                    // Usiamo lo Slot API: passiamo l'icona...
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )

                                    // testo "Gestisci" formattato su misura
                                    AutoResizedText(
                                        text = stringResource(R.string.btn_gestisci),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (viewModel.favoriteNames.isNotEmpty()) {
                                // ====================================================================
                                // FADE OUT GRADIENT SUI GIOCATORI RAPIDI
                                // ====================================================================
                                // Riutilizziamo lo stesso Wrapper. Il codice diventa immensamente più leggibile.
                                FadedRightEdgeWrapper(modifier = Modifier.fillMaxWidth()) {

                                    // LIVELLO INFERIORE
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        // PADDING: Aumentato da 16 a 40.dp per superare comodamente l'ombra
                                        contentPadding = PaddingValues(end = 40.dp)
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

                                            // Utilizzo del componente custom centralizzato per uniformita' estetica.
                                            // La configurazione cromatica (testo e sfondo) e' ora ereditata dalla funzione unica.
                                            CustomSelectableChip(
                                                text = fav,
                                                isSelected = isAlreadyAtTable,
                                                onClick = {
                                                    // Logica di interruttore (toggle) per l'aggiunta o la rimozione del giocatore.
                                                    if (!isAlreadyAtTable) {
                                                        // Assegnazione automatica del colore.
                                                        // 1. Estrazione in un nuovo array di tutti i codici colore attualmente in uso.
                                                        val usedColors = viewModel.players.map { it.color }
                                                        // 2. Filtraggio della palette master: si tengono solo i colori NON presenti in usedColors.
                                                        val availableColors = playerPalette.filter { it.toArgb() !in usedColors }
                                                        // 3. Fallback: se ci sono colori intonsi se ne pesca uno, altrimenti
                                                        // la palette è esaurita e si pesca randomicamente accettando il duplicato visivo.
                                                        val finalColor = if (availableColors.isNotEmpty()) availableColors.random() else playerPalette.random()

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
                                    } // Fine LazyRow
                                } // Fine Box Principale
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

                            // SEZIONE AGGIUNTA MANUALE
                            Text(
                                stringResource(R.string.label_aggiungi_manualmente),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            ) // Traduzione label manuale
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 🧠 FIX UI: Usiamo IntrinsicSize.Min per mantenere il bottone e il campo
                                    // di testo allineati in altezza anche con font di sistema ingranditi.
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlayerName,
                                    onValueChange = { newPlayerName = it },
                                    modifier = Modifier.weight(1f),
                                    // Sostituiamo 'label' con 'placeholder' per eliminare il padding
                                    // invisibile superiore e far combaciare l'ingombro logico con quello visivo.
                                    // 🧠 FIX UX: Forziamo il testo a rimanere su una riga sola per evitare
                                    // che spinga in alto i bordi del componente.
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.hint_nome_giocatore),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
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
                                            // Assegnazione automatica del colore per l'aggiunta manuale.
                                            val usedColors = viewModel.players.map { it.color }
                                            val availableColors =
                                                playerPalette.filter { it.toArgb() !in usedColors }
                                            val finalColor =
                                                if (availableColors.isNotEmpty()) availableColors.random() else playerPalette.random()

                                            viewModel.addPlayer(newPlayerName, finalColor.toArgb())
                                            newPlayerName = ""
                                        }
                                    },
                                    // 🧠 FIX GEOMETRIA: fillMaxHeight() permette al bottone di seguire
                                    // l'altezza del campo di testo, garantendo simmetria visiva.
                                    modifier = Modifier.fillMaxHeight(),
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
                        // 2. SMUSSATURA MORBIDA: Livello 2 (Moduli)
                        shape = RoundedCornerShape(20.dp),
                        // 3. CONTRASTO COLORI: Usiamo 'surface' (colore pulito) per staccare dal 'surfaceVariant' del tavolo.
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        // 4. BORDO DELICATO: Usiamo 'outline' invece di 'primary' per non rendere l'interfaccia troppo pesante
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
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

                                // ====================================================================
                                // CONTATORE PARTECIPANTI SQUADRATO
                                // ====================================================================
                                // Usiamo una Surface per fare in icona "squadrata" e coerente a cui applichiamo una stondatura fissa.
                                Surface(
                                    modifier = Modifier.size(28.dp), // Forza larghezza e altezza uguali (Quadrato)
                                    shape = RoundedCornerShape(8.dp), // Smussatura per coerenza geometrica col design
                                    color = MaterialTheme.colorScheme.primary, // Sfondo primario pieno
                                    // Aggiungiamo un'elevazione minima per far risaltare il contatore sulla card
                                    //shadowElevation = 2.dp
                                ) {
                                    // Avvolgiamo il testo in un Box per imporgli di stare perfettamente al centro del quadrato
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${viewModel.players.size}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold // Grassetto per dare importanza al numero
                                        )
                                    }
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
    // mettiamo il '.let'. In questo modo la variabile 'player'
    // nasce già sicura e non-nullabile per tutto l'ambito interno al Dialog.
    playerToEdit?.let { player ->
        // Ora possiamo usare 'player.name' senza il doppio punto esclamativo!
        var editedName by remember { mutableStateOf(player.name) }
        var editedColor by remember { mutableIntStateOf(player.color) }

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
                        Text(
                            stringResource(R.string.btn_annulla),
                            color = MaterialTheme.colorScheme.onSurface
                        ) // Traduzione bottone annulla
                    }

                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                // ------------------------------
                                // Regola 2: Safe Call con .let
                                // ------------------------------
                                // Se l'utente ha premuto contemporaneamente "Annulla", playerToEdit sarà 'null'.
                                // Di conseguenza, questo intero blocco '{...}' verrà elegantemente ignorato.
                                playerToEdit?.let { playerSicuro ->
                                    playerSicuro.name = editedName
                                    viewModel.updatePlayerColor(playerSicuro, editedColor)
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                }

                                // Finito tutto, chiudiamo il dialog svuotando la variabile
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
                        Text(
                            stringResource(R.string.btn_salva),
                            fontWeight = FontWeight.Bold
                        ) // Traduzione bottone salvataggio
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ====================================================================
    // DIALOG: GESTIONE PREFERITI (Sostituisce il ModalBottomSheet)
    // ====================================================================
    // 1. Componente Dialog Nativo: Apre una finestra centrale e oscura lo sfondo
    if (showFavoritesDialog) {
        var newFavName by remember { mutableStateOf("") }

        // ---> STATO NOTIFICHE LOCALE <---
        // Creiamo un gestore di notifiche ESCLUSIVO per questo Dialog.
        // Essendo il Dialog una finestra a sé stante (Z-Index superiore),
        // non possiamo usare le notifiche dello Scaffold base.
        val dialogSnackbarHostState = remember { SnackbarHostState() }
        Dialog(
            onDismissRequest = { showFavoritesDialog = false },
            // ====================================================================
            // ESPANDERE IL DIALOG IN ORIZZONTALE ROMPENDO I CONFINI NATIVI
            // ====================================================================
            // 'usePlatformDefaultWidth = false' disabilita il padding laterale gigante che
            // Android applica di default a tutti i Dialog, permettendoci di allargarlo a piacimento.
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // 2. Surface: Agisce come "sfondo fisico" della nostra finestra popup.
            Surface(
                modifier = Modifier
                    // ====================================================================
                    // LARGHEZZA PROPORZIONATA
                    // ====================================================================
                    // Ora che siamo liberi dai vincoli, chiediamo al dialog di occupare
                    // esattamente il 95% della larghezza dello schermo (0.95f).
                    // Questo lascia un margine simmetrico del 2.5% a destra e a sinistra,
                    // risultando premium ed evitando che tocchi i bordi fisici del telefono.
                    .fillMaxWidth(0.95f)

                    // Limitiamo l'altezza all'85% dello schermo per non farlo sbordare mai e renderlo proporzionato
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp), // Angoli molto arrotondati, coerenti con la tua UI
                // Usiamo il 'background' puro per staccarci dai grigi impastati
                color = MaterialTheme.colorScheme.background,
                // 1. Ripristiniamo l'ombra fisica nera classica per staccare il popup
                shadowElevation = 12.dp,
                tonalElevation = 2.dp,
                // 2. Aggiungiamo un bordino perimetrale per definire nettamente la finestra
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                // ---> IL BOX COME PIANO 2D <---
                // Usiamo un Box come genitore assoluto del Dialog per poter sovrapporre
                // la Snackbar (le notifiche) sopra la Column principale, in basso al centro.
                Box(modifier = Modifier.fillMaxSize()) {
                    // 3. Struttura verticale del Dialog
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // INTESTAZIONE: ICONA + TITOLO
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icona esplicativa per indicare la "Gestione"
                            Icon(
                                imageVector = Icons.Filled.ManageAccounts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            AutoResizedText(
                                stringResource(R.string.desc_gestisci_rapidi_icon),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // INPUT: NUOVO PREFERITO
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                // 🧠 FIX UI: Usiamo IntrinsicSize.Min per "agganciare" l'altezza del bottone
                                // a quella del campo di testo, indipendentemente da quanto ingrandisce il font.
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newFavName,
                                onValueChange = { newFavName = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(stringResource(R.string.hint_nuovo_nome)) },
                                shape = RoundedCornerShape(20.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Person,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )

                            val isAddFavEnabled = newFavName.trim()
                                .isNotEmpty() && viewModel.favoriteNames.none {
                                it.equals(
                                    newFavName.trim(),
                                    ignoreCase = true
                                )
                            }

                            Button(
                                onClick = {
                                    if (isAddFavEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        viewModel.addFavorite(newFavName.trim())
                                        newFavName = ""
                                    }
                                },
                                // 🧠 FIX GEOMETRIA: fillMaxHeight() assicura che il bottone sia alto
                                // esattamente quanto l'OutlinedTextField adiacente.
                                modifier = Modifier.fillMaxHeight(),
                                shape = RoundedCornerShape(20.dp),
                                border = if (isAddFavEnabled) null else BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAddFavEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.12f
                                    ),
                                    contentColor = if (isAddFavEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.38f
                                    )
                                )
                            ) { Text(stringResource(R.string.btn_aggiungi)) }
                        }

                        // LISTA DEI PREFERITI (Scorrevole)
                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            // 3. EFFETTO INCAVO: Sfondo opacizzato e bordo interno
                            // per far capire che questa è un'area separata in cui si scorre.
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(viewModel.favoriteNames) { index, fav ->

                                    // ====================================================================
                                    // ESTRAZIONE STRINGHE FUORI DALL'ONCLICK
                                    // Estraiamo le traduzioni qui, nel contesto Composable visivo.
                                    // ====================================================================
                                    val msgFavRimosso = stringResource(R.string.msg_rapido_rimosso, fav)
                                    val btnAnnullaFav = stringResource(R.string.btn_annulla_undo)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        // 4. ELEMENTI IN RILIEVO: Colore puro, bordo e ombra
                                        // per far sembrare ogni riga un "tassello" fisico premibile.
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // CONTROLLI ORDINAMENTO
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        viewModel.moveFavorite(index, index - 1)
                                                    },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.KeyboardArrowUp,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        viewModel.moveFavorite(index, index + 1)
                                                    },
                                                    enabled = index < viewModel.favoriteNames.size - 1,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.KeyboardArrowDown,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            // NOME GIOCATORE
                                            Text(
                                                text = fav,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                                    .padding(start = 12.dp)
                                            )

                                            // ==========================================================
                                            // AZIONE: MODIFICA
                                            // Non specifichiamo "baseColor", quindi in automatico diventerà Blu!
                                            // ==========================================================
                                            TonalActionPill(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    favToEdit = fav
                                                },
                                                modifier = Modifier.padding(end = 5.dp) // 🧠 FIX UI: Leggero margine per non farlo incollare al cestino
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = stringResource(R.string.desc_modifica_icon),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp) // 🧠 FIX UI: Ridotto a 20dp per farlo calzare nella pillola
                                                )
                                            }

                                            // ==========================================================
                                            // AZIONE: ELIMINAZIONE (CON SOVRASCRITTURA DEL COLORE)
                                            // ==========================================================
                                            TonalActionPill(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                                                    // Salvataggio dei riferimenti per eventuale ripristino
                                                    val removedIndex = index
                                                    val removedFav = fav

                                                    // Eliminazione effettiva: Rimuove immediatamente l'elemento visivo
                                                    viewModel.removeFavorite(fav)

                                                    // Lancia una Coroutine per gestire il timing della Snackbar
                                                    coroutineScope.launch {
                                                        // Disabilita la vecchia snackbar se l'utente clicca velocemente
                                                        dialogSnackbarHostState.currentSnackbarData?.dismiss()

                                                        val result =
                                                            dialogSnackbarHostState.showSnackbar(
                                                                message = msgFavRimosso,
                                                                actionLabel = btnAnnullaFav,
                                                                duration = SnackbarDuration.Short
                                                            )
                                                        // Se l'utente preme "Annulla", ripristiniamo il giocatore
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restoreFavorite(
                                                                removedIndex,
                                                                removedFav
                                                            )
                                                        }
                                                    }
                                                },
                                                // 🎨 LA MAGIA: Forziamo il bottone a usare il Rosso (Error) invece dell'azzurro!
                                                baseColor = MaterialTheme.colorScheme.error
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.desc_elimina_icon),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // PULSANTE CHIUSURA DIALOG
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                showFavoritesDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.desc_chiudi_icon),
                                modifier = Modifier.padding(end = 8.dp).size(28.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_chiudi_gestione),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // ==========================================================
                    // 🧠 HOST DELLE NOTIFICHE (Ancorato in basso al Box)
                    // ==========================================================
                    SnackbarHost(
                        hostState = dialogSnackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter) // Poggia sul fondo del Dialog
                            .padding(bottom = 16.dp) // Leggero rialzo di sicurezza per non toccare i bordi
                    )
                } // Fine Box
            }
        }
    }


    // MODALE: MODIFICA NOME PREFERITO
    /**
     * Usare favToEdit?.let  è il modo "Elegante e Sicuro" per lavorare con scatole che potrebbero essere vuote.
     * È diviso in due parti che lavorano in squadra:
     *
     * Parte A: Il Safe Call ?. (La Chiamata Sicura)
     * Il punto interrogativo seguito dal punto ?. significa:
     * "Bussa alla scatola. Se è vuota (null), fermati immediatamente e ignora tutto quello che c'è scritto dopo. Se c'è qualcosa, procedi."
     *
     * Parte B: La Scope Function let (Lascia Fare)
     * La parola let in inglese significa "lascia", "permetti".
     * In Kotlin è una funzione speciale che crea una Stanza di Sicurezza (le parentesi graffe { }).
     */
    favToEdit?.let { favName ->
        // Usiamo favName pulito!
        var editedFavName by remember { mutableStateOf(favName) }

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
}
