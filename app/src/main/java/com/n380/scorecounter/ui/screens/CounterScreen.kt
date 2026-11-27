    package com.n380.scorecounter.ui.screens

    import com.n380.scorecounter.ui.components.AutoResizedText
    import androidx.activity.compose.BackHandler
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.KeyboardActions
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ExitToApp
    import androidx.compose.material.icons.filled.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.runtime.saveable.rememberSaveable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.focus.FocusRequester
    import androidx.compose.ui.focus.focusRequester
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.hapticfeedback.HapticFeedbackType
    import android.content.res.Configuration
    import androidx.compose.ui.platform.LocalConfiguration
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.LocalHapticFeedback
    import androidx.lifecycle.compose.LocalLifecycleOwner
    import androidx.compose.ui.res.stringResource // 🌍 I18N: Import fondamentale per le traduzioni UI
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.input.KeyboardType
    import androidx.compose.ui.unit.dp
    //Importiamo la classe Lifecycle per gestire il ciclo di vita della schermata.
    import androidx.lifecycle.Lifecycle
    import androidx.lifecycle.LifecycleEventObserver
    import androidx.compose.ui.text.TextRange
    import androidx.compose.ui.text.input.ImeAction
    import androidx.compose.ui.text.input.TextFieldValue
    import androidx.compose.ui.text.style.TextAlign
    import com.n380.scorecounter.R // 🌍 I18N: Import per l'accesso ai file di risorsa XML
    import com.n380.scorecounter.model.Player
    import com.n380.scorecounter.ui.components.PlayerScoreCard
    import com.n380.scorecounter.ui.components.FeatureInfoDialog
    import com.n380.scorecounter.ui.components.ScoreHistoryDialog
    import com.n380.scorecounter.ui.components.formatTime
    import com.n380.scorecounter.ui.screens.landscape.CounterScreenLandscape
    import com.n380.scorecounter.viewmodel.MatchViewModel
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch

    // ====================================================================
    // ====================================================================
    // SCHERMATA DEL CONTATORE: Gestione dei punteggi in tempo reale durante la partita.
    // ====================================================================

    @Composable
    fun CounterScreen(
        viewModel: MatchViewModel,
        onNavigateToResults: () -> Unit,
        // Aggiungiamo un parametro per gestire l'uscita d'emergenza verso la Home
        onNavigateHome: () -> Unit
    ) {
        // Stato per decidere se mostrare il popup di conferma azzeramento
        var showResetDialog by remember { mutableStateOf(false) }

        // ---> Stato per decidere se mostrare il popup di Fine Partita
        var showEndMatchDialog by remember { mutableStateOf(false) }

        // Variabile che indica se mostrare l'avviso di uscita "Salva-Vita" (se si preme Indietro per sbaglio)
        var showExitWarning by remember { mutableStateOf(false) }

        // Variabile per ricordare il tempo impostato manualemnte nel timer:
        var savedTimerMemory by rememberSaveable { mutableStateOf("") }

        // ---> STATI PER IL DADO VIRTUALE <---
        var showDiceDialog by rememberSaveable { mutableStateOf(false) }

        // ---> STATI PER IL PULSANTE TIMER VIRTUALE <---
        var showTimerDialog by rememberSaveable { mutableStateOf(false) }

        // Stato per mostrare il popup delle impostazioni del dado
        var showDiceSettingsDialog by rememberSaveable { mutableStateOf(false) }

        var diceResult by rememberSaveable { mutableIntStateOf(1) }
        // Contatore univoco per forzare l'aggiornamento dell'interfaccia ad ogni click
        var diceRollCount by rememberSaveable { mutableIntStateOf(0) }

        // --->Ricorda QUALE giocatore stiamo modificando manualmente con la tastiera <---
        // Se è "null", il popup per l'inserimento manuale è nascosto.
        var playerForManualEdit by remember { mutableStateOf<Player?>(null) }




        // STATO TUTORIAL: Controlla l'apertura del popup centrale con le regole nascoste (Manuale)
        var showInfoDialog by remember { mutableStateOf(false) }

        // Stato per la gestione della visibilità del dialogo della cronologia punteggi
        var showScoreHistoryDialog by rememberSaveable { mutableStateOf(false) }

        // SISTEMA SNACKBAR (Tasto Annulla Azzeramento)
        val snackbarHostState = remember { SnackbarHostState() }

        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // 🎲 STATO E LOGICA SORTEGGIO PRIMO GIOCATORE ("Roulette" casuale)
        // Memorizza l'indice del giocatore attualmente illuminato durante i salti della roulette
        var highlightedPlayerIndex by remember { mutableStateOf<Int?>(null) }
        // Memorizza l'indice del giocatore estratto vincitore per mostrare il badge "Inizia!"
        var selectedFirstPlayerIndex by remember { mutableStateOf<Int?>(null) }
        // Flag booleano di protezione che disabilita il pulsante "Sorteggio" durante l'animazione per evitare click sovrapposti
        var isRaffleAnimating by remember { mutableStateOf(false) }

        // 🧠 FUNZIONE AZIONE SORTEGGIO (Estrazione casuale con animazione a rallentamento progressivo):
        val startRaffle: () -> Unit = {
            val playerCount = viewModel.players.size
            if (playerCount >= 2 && !isRaffleAnimating) {
                isRaffleAnimating = true
                selectedFirstPlayerIndex = null
                highlightedPlayerIndex = null

                coroutineScope.launch {
                    // 1. Estrazione del vincitore in modo puramente casuale tramite Random.nextInt
                    val winnerIndex = kotlin.random.Random.nextInt(playerCount)

                    // 2. Calcolo dei passi della roulette per durare circa 1,8 secondi (14 passi da 50ms a 220ms)
                    val minSteps = 14
                    val extraSteps = (winnerIndex - (minSteps % playerCount) + playerCount) % playerCount
                    val totalSteps = minSteps + extraSteps

                    var currentDelay = 50L
                    val delayIncrement = (220L - 50L) / totalSteps

                    for (step in 0..totalSteps) {
                        val currentIndex = step % playerCount
                        highlightedPlayerIndex = currentIndex
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        delay(currentDelay)
                        currentDelay += delayIncrement
                    }

                    // 3. Arresto sul vincitore estratto con vibrazione tattile di conferma
                    highlightedPlayerIndex = winnerIndex
                    selectedFirstPlayerIndex = winnerIndex
                    isRaffleAnimating = false
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
        }

        // Funzione di utilità per azzerare l'evidenziazione del sorteggio al tocco o alla modifica del punteggio
        val clearRaffle: () -> Unit = {
            highlightedPlayerIndex = null
            selectedFirstPlayerIndex = null
        }


        // ---> AVVIO CRONOMETRO AUTOMATICO <---
        // LaunchedEffect fa partire un blocco di codice non appena questa pagina viene "disegnata" sullo schermo.
        LaunchedEffect(Unit) {
            viewModel.startTimer()
        }

        // ====================================================================
        // LOGICA CRONOMETRO INTELLIGENTE (Pausa automatica in background)
        // ====================================================================

        // 1. LocalLifecycleOwner: Recupera il "proprietario" del ciclo di vita.
        // In Android, ogni schermata ha un ciclo di vita (nasce, diventa visibile, va in pausa, muore).
        // Con questo comando otteniamo l'oggetto che "governa" queste fasi per la pagina attuale.
        val lifecycleOwner = LocalLifecycleOwner.current

        // 2. LA VARIABILE: isTimerRunning
        // Creiamo una variabile locale per ricordare se il cronometro sta attualmente girando.
        // Siccome poco sopra abbiamo eseguito viewModel.startTimer(), la facciamo partire a 'true'.
        var isTimerRunning by remember { mutableStateOf(true) }

        // 3. LA MEMORIA STORICA: wasTimerRunningBeforeBackground
        // Serve per ricordare se il cronometro stava girando nell'istante PRIMA che l'utente bloccasse lo schermo.
        // (Se il cronometro era GIA' in pausa prima di bloccare lo schermo, non vogliamo che riparta da solo sbloccandolo!).
        var wasTimerRunningBeforeBackground by remember { mutableStateOf(false) }

        // 4. DisposableEffect: Un effetto speciale di Compose.
        // "Disposable" significa "usa e getta". Serve per attivare una funzione quando la schermata
        // appare, e DEVE avere una funzione 'onDispose' alla fine per "pulire" tutto quando la schermata si chiude.
        DisposableEffect(lifecycleOwner) {

            // Creiamo la nostra "spia" (Observer) che ascolta i cambiamenti del telefono.
            val observer = LifecycleEventObserver { _, event ->
                // Controlliamo quale evento è appena successo
                when (event) {

                    // EVENTO A: L'app va in background o lo schermo si spegne
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                        // Se il cronometro in questo momento stava scorrendo...
                        if (isTimerRunning) {
                            wasTimerRunningBeforeBackground = true // ...ce lo appuntiamo nella memoria
                            isTimerRunning = false                 // ...aggiorniamo la variabile locale a falso
                            viewModel.pauseTimer()                 // ...e Diciamo al ViewModel di STOPPARE il cronometro
                        }
                    }

                    // EVENTO B: L'utente riapre l'app o sblocca lo schermo
                    Lifecycle.Event.ON_RESUME -> {
                        // Controlliamo la nostra memoria: il cronometro stava girando prima dell'interruzione?
                        if (wasTimerRunningBeforeBackground) {
                            isTimerRunning = true                  // ...aggiorniamo la variabile locale a vero
                            viewModel.startTimer()                 // ...e Diciamo al ViewModel di FAR RIPARTIRE il cronometro
                            wasTimerRunningBeforeBackground = false // Svuotiamo la memoria
                        }
                    }

                    // Tutti gli altri eventi di Android non ci interessano, li ignoriamo.
                    else -> {}
                }
            }

            // Dopo aver configurato la spia, la attacchiamo ufficialmente al telefono.
            lifecycleOwner.lifecycle.addObserver(observer)

            // COMANDO DI PULIZIA (obbligatorio nel DisposableEffect)
            onDispose {
                // Quando l'utente preme "Fine Match" ed esce definitivamente dalla schermata,
                // stacchiamo la spia. Se non lo facessimo, rimarrebbe accesa in memoria rallentando il telefono!
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        // ====================================================================

        // ---> SCHERMO SEMPRE ACCESO (Centralizzato in MainActivity tramite preferenze utente) <---
        // 🧠 NOTA ARCHITETTURALE: La gestione della flag 'FLAG_KEEP_SCREEN_ON' è ora gestita
        // reattivamente a livello di 'MainActivity' in base all'impostazione scelta dall'utente:
        // - "Solo in partita" (attiva durante 'counter')
        // - "Sempre attivo" (attiva in tutta l'app)
        // - "Disattivato" (disattivata ovunque)

        // ---> IL SALVA-VITA (BackHandler) <---
        // Intercetta il tasto "Indietro" fisico o lo swipe back del telefono.
        // Invece di uscire direttamente, cambia lo stato per far apparire il dialogo di conferma.
        BackHandler {
            // 🧠 BUG FIX: DOPPIA VIBRAZIONE
            // Abbiamo rimosso 'haptic.performHapticFeedback(HapticFeedbackType.LongPress)' da qui.
            // Motivo tecnico: I sistemi Android moderni forniscono già un feedback tattile nativo
            // quando viene eseguita la gesture "Back". Aggiungendone uno manuale, l'utente
            // percepiva un fastidioso "doppio colpo". Lasciando fare al sistema, il feedback
            // rimane singolo, pulito e coerente con il resto del sistema operativo.
            showExitWarning = true
        }

        // --------------------------------------------------------------------
        // RILEVAMENTO ORIENTAMENTO SCHERMO (RESPONSIVE ROUTING)
        // --------------------------------------------------------------------
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            // ====================================================================
            // ORIENTAMENTO ORIZZONTALE (LANDSCAPE) -> DELEGATO A CounterScreenLandscape.kt
            // ====================================================================
            CounterScreenLandscape(
                viewModel = viewModel,
                onNavigateToResults = onNavigateToResults,
                onNavigateHome = onNavigateHome,
                showResetDialog = showResetDialog,
                onShowResetDialogChange = { showResetDialog = it },
                showEndMatchDialog = showEndMatchDialog,
                onShowEndMatchDialogChange = { showEndMatchDialog = it },
                showInfoDialog = showInfoDialog,
                onShowInfoDialogChange = { showInfoDialog = it },
                showScoreHistoryDialog = showScoreHistoryDialog,
                onShowScoreHistoryDialogChange = { showScoreHistoryDialog = it },
                showTimerDialog = showTimerDialog,
                onShowTimerDialogChange = { showTimerDialog = it },
                showDiceDialog = showDiceDialog,
                onShowDiceDialogChange = { showDiceDialog = it },
                diceResult = diceResult,
                onDiceResultChange = { diceResult = it },
                diceRollCount = diceRollCount,
                onDiceRollCountChange = { diceRollCount = it },
                playerForManualEdit = playerForManualEdit,
                onPlayerForManualEditChange = { playerForManualEdit = it },
                haptic = haptic,
                snackbarHostState = snackbarHostState,
                highlightedPlayerIndex = highlightedPlayerIndex,
                selectedFirstPlayerIndex = selectedFirstPlayerIndex,
                isRaffleAnimating = isRaffleAnimating,
                onStartRaffle = startRaffle,
                onClearRaffle = clearRaffle
            )
        } else {
            // ====================================================================
            // ORIENTAMENTO VERTICALE (PORTRAIT) -> SCAFFOLD STANDARD
            // ====================================================================
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,

            // Montiamo la Snackbar in questa schermata per gli avvisi
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Usiamo la "bottomBar" (barra inferiore) dello Scaffold per i tasti principali d'azione
            // --------------------------------------------------------------------
            // BOTTOM BAR: IL "DOCK" DEI COMANDI
            // --------------------------------------------------------------------
            bottomBar = {
                // ---> LEZIONE: LA PIATTAFORMA VISIVA (Surface) <---
                // Invece di lasciare i bottoni sospesi nel vuoto, li avvolgiamo in una Surface.
                // Questa agirà come un "muro" semi-trasparente che nasconde le carte che scorrono sotto,
                // eliminando il rumore visivo e dando risalto ai bottoni.
                Surface(
                    // Sfondo scuro (colore di background del tema) quasi solido al 95%
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    // ANGOLI SUPERIORI ARROTONDATI
                    // Usiamo RoundedCornerShape specificando SOLO gli angoli in alto (topStart e topEnd).
                    // Mettendo 24.dp creiamo una curva morbida che "abbraccia" visivamente i bottoni da 20.dp,
                    // mentre gli angoli in basso restano a 0.dp (piatti) per aderire al vetro del telefono.
                    shape = RoundedCornerShape(24.dp),
                    // Bordo superiore sottilissimo per staccare nettamente la barra dalla lista
                    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // navigationBarsPadding() assicura che i bottoni non finiscano MAI sotto
                            // la barra di navigazione nativa di Android (quella con la linea bianca in basso).
                            .navigationBarsPadding()
                            // Padding interno della barra
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ========================================================
                        // TASTO SECONDARIO: AZZERA (GHOST BUTTON)
                        // ========================================================
                        OutlinedButton(
                            onClick = {
                                if (viewModel.isNavigationLocked || showEndMatchDialog) return@OutlinedButton
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                diceRollCount++

                                // 🧠 LOGICA DIDATTICA (Conferma Azioni Rapide):
                                // Verifichiamo se l'utente ha attivato la richiesta di conferma nelle impostazioni.
                                if (viewModel.confirmQuickActions) {
                                    showResetDialog = true // Mostra il dialogo di conferma azzeramento
                                } else {
                                    viewModel.resetScoresWithUndo() // Esegue l'azzeramento istantaneamente senza dialoghi
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp), // Altezza Expressive massiccia
                            shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        ) {
                            // 🧠 LEZIONE UI: Aggiunta dell'Ancora Visiva (Icona)
                            Icon(
                                imageVector = Icons.Filled.Refresh, // L'icona universale di ripristino
                                contentDescription = null, // Già spiegato dal testo a fianco, non serve per i non vedenti
                                modifier = Modifier
                                    .padding(end = 8.dp) // Spaziatura aurea di Material Design
                                    .size(28.dp) // Dimensione decisa e massiccia in linea con il testo
                            )

                            AutoResizedText(
                                // 🌍 I18N: Lettura nativa.
                                text = stringResource(R.string.btn_azzera),
                                // Utilizziamo lo stile titleLarge (Proprietà di MaterialTheme)
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // ========================================================
                        // TASTO PRIMARIO: FINE MATCH (CALL TO ACTION)
                        // ========================================================
                        // Usiamo 'enabled' collegandolo alla logica del ViewModel.
                        // Il pulsante sarà grigio e non cliccabile se nessuno ha ancora giocato.
                        Button(
                            enabled = viewModel.canEndMatch,
                            onClick = {
                                // PROTEZIONE LUCCHETTO: Ignora se stiamo azzerando
                                if (viewModel.isNavigationLocked || showResetDialog) return@Button
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                // 🧠 LOGICA DIDATTICA (Conferma Azioni Rapide):
                                // Se attivo, mette in pausa il timer e apre il dialogo di fine partita.
                                // Se disattivo, chiude istantaneamente la partita, salva e naviga ai risultati.
                                if (viewModel.confirmQuickActions) {
                                    viewModel.pauseTimer()
                                    showEndMatchDialog = true
                                } else {
                                    if (viewModel.lockNavigation()) {
                                        viewModel.pauseTimer()
                                        viewModel.saveCurrentMatch()
                                        onNavigateToResults()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp), // Altezza Expressive massiccia
                            shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                            // L'elevazione (ombra) sparisce se il tasto è disattivato per non sembrare "premibile"
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = if (viewModel.canEndMatch) 8.dp else 0.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                // Colori per lo stato disattivato (Material 3 Standard: 12% sfondo, 38% testo)
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        ) {
                            // 🧠 LEZIONE UI: L'icona erediterà dinamicamente il colore 'onPrimaryContainer'
                            // o 'disabledContentColor' in automatico da ButtonDefaults!
                            Icon(
                                imageVector = Icons.Filled.Flag, // La bandiera del traguardo per indicare la fine
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp) // Spaziatura aurea
                                    .size(28.dp)
                            )

                            AutoResizedText(
                                // 🌍 I18N: Testo estratto.
                                text = stringResource(R.string.btn_fine_match),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {

                // ====================================================================
                // 1. RIGA IN ALTO: SOLO IL TITOLO (Auto-Adattivo)
                // ====================================================================
                // Sostituito "Text" normale con il nostro nuovo "AutoResizedText".
                // Ora, se inserisci un titolo chilometrico, si restringerà automaticamente
                // pur di rimanere su una singola riga pulita!
                // ====================================================================
                // 1. RIGA IN ALTO: TITOLO A SINISTRA E AZIONI SECONDARIE A DESTRA
                // ====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AutoResizedText(
                        // ====================================================================
                        // 🧠 LEZIONE TEORICA I18N: Condizioni Logiche e Traduzioni
                        // ====================================================================
                        // Il Kotlin valuta lespressioni condizionali in linea (Inline If).
                        // Se il ViewModel non ha un titolo (l'utente ha skippato la compilazione),
                        // passiamo la traduzione ufficiale (es. "Sfida" in IT, "Challenge" in EN).
                        // Altrimenti, stampiamo esattamente ciò che l'utente ha digitato in CreateMatchScreen!
                        text = if (viewModel.matchTitle.isEmpty()) stringResource(R.string.titolo_sfida_default) else viewModel.matchTitle,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    )

                    // Pulsanti d'azione secondari circolari ad ampiezza discreta: Cronologia e Info
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsante circolare Cronologia (abilitato solo se è stato assegnato almeno un punto)
                        val isHistoryEnabled = viewModel.scoreActions.isNotEmpty()
                        FilledIconButton(
                            onClick = {
                                if (isHistoryEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showScoreHistoryDialog = true
                                }
                            },
                            enabled = isHistoryEnabled,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isHistoryEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                contentColor = if (isHistoryEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = stringResource(R.string.desc_cronologia_icon),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Pulsante circolare Info
                        FilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showInfoDialog = true
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(R.string.desc_info_icon),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                // ====================================================================
                // 2. BARRA DEGLI STRUMENTI (Toolbar Card)
                // ====================================================================
                // 🧠 DESIGN CHOICE: Abbiamo incapsulato gli strumenti in una Card dedicata.
                // Questo crea un "ponte" visivo tra il titolo e la lista giocatori,
                // dando ordine a pulsanti che prima sembravano "galleggiare" nel vuoto.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    // Usiamo surfaceVariant con un tocco di trasparenza per non appesantire troppo la parte alta
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp), // Padding interno calibrato
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // --- BLOCCO SINISTRO: Indicatore del Tempo trascorso e Obiettivo ---
                        // 🧠 RESPIRO PER IL TEMPO DI GIOCO (ARMONIA GRAFICA):
                        // Aumentiamo il padding a destra (padding end = 12.dp) per la Column del cronometro (🕒 32:10).
                        // In questo modo le cifre del tempo che scorre hanno un generoso respiro visivo e non risultano mai schiacciate contro il pulsante Timer!
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp).padding(end = 4.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                // Configurazione delle cifre tabulari (fontFeatureSettings = "tnum")
                                // Garantisce larghezza fissa uniforme per tutte le cifre (0-9), mantenendo perfetto l'allineamento.
                                Text(
                                    text = formatTime(viewModel.matchDurationSeconds),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // RIGA OBIETTIVO: Traguardo (se impostato)
                            if (viewModel.targetScore.isNotBlank()) {
                                AutoResizedText(
                                    text = stringResource(R.string.desc_obbiettivo, viewModel.targetScore),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }

                        // --- BLOCCO DESTRO: I TRE STRUMENTI (Timer, Dado, Sorteggio) A TUTTA LARGHEZZA ---
                        // 🧠 ESPANSIONE DINAMICA A TUTTA LARGHEZZA:
                        // Assegnando Modifier.weight(1f) alla Row genitore dei 3 pulsanti, questa si espande fino a toccare il bordo destro della Card.
                        // Ciascuno dei 3 pulsanti riceve Modifier.weight(1f), distribuendo il 100% dello spazio orizzontale rimanente in modo uniforme!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // PULSANTE TIMER: Per impostare conti alla rovescia
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showTimerDialog = true
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                            ) {
                                Icon(Icons.Filled.Timer, null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.btn_timer),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // PULSANTE DADO: Per estrazioni casuali
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    diceResult = (1..viewModel.diceSides).random()
                                    showDiceDialog = true
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                            ) {
                                Icon(Icons.Filled.Casino, null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                                AutoResizedText(stringResource(R.string.btn_dado), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }

                            // PULSANTE SORTEGGIO: Per la scelta casuale del primo giocatore che inizia la partita
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    startRaffle()
                                },
                                enabled = viewModel.players.size >= 2 && !isRaffleAnimating,
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Filled.Shuffle, null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.btn_sorteggio),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ---> CALCOLO DELLA CORONA DEL LEADER IN TEMPO REALE <---
                // Calcoliamo il punteggio rilevante per il leader in base al criterio di vittoria attivo (minimo se vince il più basso, massimo altrimenti)
                val winningScore = if (viewModel.isLowestScoreWins) {
                    viewModel.players.minOfOrNull { it.score } ?: 0
                } else {
                    viewModel.players.maxOfOrNull { it.score } ?: 0
                }
                val maxScore = viewModel.players.maxOfOrNull { it.score } ?: 0

                // ---> CONTROLLO VITTORIA AUTOMATICA <---
                // LaunchedEffect(maxScore) è un "osservatore spia". Gli diciamo di tenere d'occhio la variabile "maxScore".
                // Ogni volta che i punti di un giocatore cambiano alzando l'asticella, lui esegue questo blocco alla velocità della luce.
                LaunchedEffect(maxScore) {
                    // Trasformiamo il testo dell'obiettivo in un numero vero (Int). Se il campo è vuoto, diventa "null".
                    val target = viewModel.targetScore.toIntOrNull()

                    // Regola della Vittoria: Se l'obiettivo esiste, è maggiore di 0, e il leader ha raggiunto o superato l'obiettivo...
                    if (target != null && target > 0 && maxScore >= target) {
                        // Usiamo il lucchetto del ViewModel per l'autopilota!
                        // In questo modo se l'utente preme "Azzera" lo stesso istante, non esplode nulla.
                        if (viewModel.lockNavigation()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibrazione della vittoria!
                            viewModel.pauseTimer() // Fermiamo l'orologio
                            // 💾 SALVATAGGIO AUTOMATICO (AUTO-SAVE)
                            // Nel momento esatto in cui l'autopilota dichiara la fine,
                            viewModel.saveCurrentMatch()// sigilliamo i dati nel database prima di cambiare pagina!
                            onNavigateToResults() // Passiamo automaticamente alla schermata finale dei coriandoli!
                        }
                    }
                }

                // ---> IL NUOVO CONTENITORE DEL TAVOLO (Box di Sfondo) <---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // weight(1f) dice alla Card: "Allungati occupando tutto lo spazio vuoto che c'è tra
                        // l'intestazione in alto e i bottoni in basso"
                        .weight(1f)
                        // Aggiungiamo un margine inferiore per non farla incollare ai tasti "Azzera/Fine Match"
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    // ---> MODIFICA 1: Stondatura completa <---
                    // Scrivendo solo 24.dp (senza specificare top o bottom), Android stonda tutti e 4 gli angoli!
                    shape = RoundedCornerShape(24.dp)
                ) {
                    // 🧠 ARCHITETTURA RESPONSIVA ADATTIVA (BoxWithConstraints):
                    // Misuriamo l'altezza e la larghezza reale a disposizione del tavolo.
                    // Questo ci consente di calcolare matematicamente se le card possono espandersi
                    // ed equispaziarsi su tutto lo schermo (senza lasciare vuoti inferiori asimmetrici)
                    // o se devono passare alla visualizzazione compatta a scorrimento.
                    val tablePadding = 12.dp // Margine perimetrale uniforme su tutti e 4 i lati del tavolo
                    val spacingBetweenCards = tablePadding // 🧠 Distanza tra le varie card IDENTICA al margine perimetrale!

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(tablePadding) // 🧠 Margine esterno perfettamente uniforme sui 4 lati (top, bottom, start, end)
                    ) {
                        val availableWidth = maxWidth
                        val availableHeight = maxHeight

                        // 🧠 LOGICA (Ordinamento Dinamico Classifica):
                        // Se l'ordinamento dinamico è attivo, ordiniamo per punteggio in base al criterio di vittoria
                        // (crescente se vince il punteggio minore, decrescente altrimenti) e per timestamp di ultimo aggiornamento.
                        val displayedPlayers = if (viewModel.dynamicLeaderboardSorting) {
                            viewModel.players.sortedWith(
                                if (viewModel.isLowestScoreWins) {
                                    compareBy<Player> { it.score }
                                        .thenByDescending { it.lastScoreUpdateTime }
                                } else {
                                    compareByDescending<Player> { it.score }
                                        .thenByDescending { it.lastScoreUpdateTime }
                                }
                            )
                        } else {
                            viewModel.players
                        }

                        val playerCount = displayedPlayers.size

                        // 🧠 METRICHE E SOGLIE RESPONSIVE:
                        // L'altezza minima confortevole per una card espansa affinché non risulti schiacciata è ~105.dp.
                        val minExpandedCardHeight = 105.dp

                        // Schermi ampi (tablet / foldable aperto con larghezza >= 540.dp)
                        val isTabletOrWide = availableWidth >= 540.dp

                        // Calcoliamo se lo spazio reale disponibile permette la visualizzazione espansa
                        val (isExpandedMode, useTwoColumns) = when {
                            isTabletOrWide -> {
                                val rows = (playerCount + 1) / 2
                                val heightNeeded = (minExpandedCardHeight * rows) + (spacingBetweenCards * (rows - 1).coerceAtLeast(0))
                                val fits = heightNeeded <= availableHeight && playerCount <= 8
                                Pair(fits, fits)
                            }
                            else -> {
                                // Su smartphone standard a colonna singola:
                                // Attiviamo la modalità espansa per 1-4 giocatori come standard,
                                // oppure fino a 5 giocatori solo su display molto alti/ampi dove heightNeeded <= availableHeight.
                                // Con 6 o più giocatori (o se lo spazio verticale non basta), passiamo alla modalità compatta.
                                val heightNeeded = (minExpandedCardHeight * playerCount) + (spacingBetweenCards * (playerCount - 1).coerceAtLeast(0))
                                val fits = (playerCount in 1..4 && heightNeeded <= availableHeight) ||
                                        (playerCount == 5 && heightNeeded <= availableHeight)
                                Pair(fits, false)
                            }
                        }

                        if (isExpandedMode) {
                            if (useTwoColumns) {
                                // 🌟 MODALITÀ ESPANSA PER TABLET (Griglia bilanciata a 2 colonne)
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                                ) {
                                    val half = (playerCount + 1) / 2
                                    val leftPlayers = displayedPlayers.take(half)
                                    val rightPlayers = displayedPlayers.drop(half)

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                                    ) {
                                        leftPlayers.forEach { p ->
                                            key(p.name) {
                                                val isLeader = if (viewModel.isLowestScoreWins) p.score == winningScore else (p.score == winningScore && winningScore > 0)
                                                val playerIndex = viewModel.players.indexOf(p)
                                                val isRaffleHighlighted = (highlightedPlayerIndex != null && highlightedPlayerIndex == playerIndex)
                                                val showStartBadge = (!isRaffleAnimating && selectedFirstPlayerIndex != null && selectedFirstPlayerIndex == playerIndex)

                                                PlayerScoreCard(
                                                    player = p,
                                                    isLeader = isLeader,
                                                    isRaffleHighlighted = isRaffleHighlighted,
                                                    showStartBadge = showStartBadge,
                                                    isScoreButtonsEnabled = !isRaffleAnimating,
                                                    positiveStep = viewModel.longPressPositiveStep,
                                                    negativeStep = viewModel.longPressNegativeStep,
                                                    totalPlayers = playerCount,
                                                    isExpandedMode = true,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxWidth(),
                                                    onScoreChange = { amount ->
                                                        clearRaffle()
                                                        viewModel.updatePlayerScore(p, amount)
                                                    },
                                                    onScoreClick = { playerForManualEdit = p },
                                                    onCardClick = { clearRaffle() }
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                                    ) {
                                        rightPlayers.forEach { p ->
                                            key(p.name) {
                                                val isLeader = if (viewModel.isLowestScoreWins) p.score == winningScore else (p.score == winningScore && winningScore > 0)
                                                val playerIndex = viewModel.players.indexOf(p)
                                                val isRaffleHighlighted = (highlightedPlayerIndex != null && highlightedPlayerIndex == playerIndex)
                                                val showStartBadge = (!isRaffleAnimating && selectedFirstPlayerIndex != null && selectedFirstPlayerIndex == playerIndex)

                                                PlayerScoreCard(
                                                    player = p,
                                                    isLeader = isLeader,
                                                    isRaffleHighlighted = isRaffleHighlighted,
                                                    showStartBadge = showStartBadge,
                                                    isScoreButtonsEnabled = !isRaffleAnimating,
                                                    positiveStep = viewModel.longPressPositiveStep,
                                                    negativeStep = viewModel.longPressNegativeStep,
                                                    totalPlayers = playerCount,
                                                    isExpandedMode = true,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxWidth(),
                                                    onScoreChange = { amount ->
                                                        clearRaffle()
                                                        viewModel.updatePlayerScore(p, amount)
                                                    },
                                                    onScoreClick = { playerForManualEdit = p },
                                                    onCardClick = { clearRaffle() }
                                                )
                                            }
                                        }
                                        if (rightPlayers.size < leftPlayers.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                // 🌟 MODALITÀ ESPANSA SMARTPHONE (1-4, o 5 su display alti)
                                // 🧠 DISTRIBUZIONE UNIFORME DELLO SPAZIO:
                                // Assegnando `Modifier.weight(1f)` a tutte le card, queste assumono
                                // esattamente la stessa altezza e riempiono perfettamente l'intero tavolo.
                                // In questo modo viene eliminato qualsiasi vuoto asimmetrico in basso!
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                                ) {
                                    displayedPlayers.forEach { p ->
                                        key(p.name) {
                                            val isLeader = if (viewModel.isLowestScoreWins) p.score == winningScore else (p.score == winningScore && winningScore > 0)
                                            val playerIndex = viewModel.players.indexOf(p)
                                            val isRaffleHighlighted = (highlightedPlayerIndex != null && highlightedPlayerIndex == playerIndex)
                                            val showStartBadge = (!isRaffleAnimating && selectedFirstPlayerIndex != null && selectedFirstPlayerIndex == playerIndex)

                                            val cardModifier = if (playerCount == 1) {
                                                Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 130.dp, max = 190.dp)
                                            } else {
                                                Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                            }

                                            PlayerScoreCard(
                                                player = p,
                                                isLeader = isLeader,
                                                isRaffleHighlighted = isRaffleHighlighted,
                                                showStartBadge = showStartBadge,
                                                isScoreButtonsEnabled = !isRaffleAnimating,
                                                positiveStep = viewModel.longPressPositiveStep,
                                                negativeStep = viewModel.longPressNegativeStep,
                                                totalPlayers = playerCount,
                                                isExpandedMode = true,
                                                modifier = cardModifier,
                                                onScoreChange = { amount ->
                                                    clearRaffle()
                                                    viewModel.updatePlayerScore(p, amount)
                                                },
                                                onScoreClick = { playerForManualEdit = p },
                                                onCardClick = { clearRaffle() }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 📊 MODALITÀ COMPATTA (5 o più giocatori su mobile, o altezza insufficiente)
                            // Utilizziamo LazyColumn per consentire lo scorrimento fluido ad alta densità.
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(spacingBetweenCards)
                            ) {
                                items(
                                    items = displayedPlayers,
                                    key = { player -> player.name } // Chiave stabile per animazioni fluide
                                ) { p ->
                                    val isLeader = if (viewModel.isLowestScoreWins) p.score == winningScore else (p.score == winningScore && winningScore > 0)
                                    val playerIndex = viewModel.players.indexOf(p)
                                    val isRaffleHighlighted = (highlightedPlayerIndex != null && highlightedPlayerIndex == playerIndex)
                                    val showStartBadge = (!isRaffleAnimating && selectedFirstPlayerIndex != null && selectedFirstPlayerIndex == playerIndex)

                                    PlayerScoreCard(
                                        player = p,
                                        isLeader = isLeader,
                                        isRaffleHighlighted = isRaffleHighlighted,
                                        showStartBadge = showStartBadge,
                                        isScoreButtonsEnabled = !isRaffleAnimating,
                                        positiveStep = viewModel.longPressPositiveStep,
                                        negativeStep = viewModel.longPressNegativeStep,
                                        totalPlayers = playerCount,
                                        isExpandedMode = false,
                                        onScoreChange = { amount ->
                                            clearRaffle()
                                            viewModel.updatePlayerScore(p, amount)
                                        },
                                        onScoreClick = { playerForManualEdit = p },
                                        onCardClick = { clearRaffle() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        /*
         * =========================================================================================
         * TEORIA POPUP INSERIMENTO MANUALE (SMART DIALOG)
         * =========================================================================================
         * Questo blocco condizionale genera un AlertDialog e applica 3 concetti avanzati di UX/UI
         * per garantire un'esperienza utente fluida e professionale:
         *
         * 1. CONTROLLO DEL CURSORE (State Management avanzato):
         * Al posto di una semplice String, usiamo un oggetto 'TextFieldValue'. Questo ci permette
         * non solo di memorizzare il testo inserito, ma di forzare la posizione iniziale del
         * cursore (Caret) alla fine della cifra tramite 'TextRange', evitando il fastidioso
         * posizionamento predefinito a sinistra.
         *
         * 2. TASTIERA AUTOMATICA (Focus Management & Side Effects):
         * Usiamo un 'FocusRequester' per creare un "bersaglio" sul campo di testo.
         * Tramite un 'LaunchedEffect' (che scatta solo all'apertura) e un piccolo delay per
         * attendere l'animazione del Dialog, inviamo il comando di focus.
         * Risultato: la tastiera sale da sola senza costringere l'utente a cliccare di nuovo.
         *
         * 3. SMART CLOSE (Hardware-Software Integration):
         * Istruiamo la tastiera Android a mostrare il tasto Spunta (ImeAction.Done).
         * Tramite 'keyboardActions', intercettiamo quel tasto: togliamo il focus (facendo
         * scendere la tastiera con un'animazione fluida) ed eseguiamo istantaneamente il
         * salvataggio e la chiusura del popup.
         * =========================================================================================
         */


        // =========================================================================================
        // MODALE INSERIMENTO MANUALE: GESTIONE AVANZATA DEL FOCUS E DEL TESTO (di MODFICA PUNTEGGIO)
        // =========================================================================================
        if (playerForManualEdit != null) {

            // 1. STATE MANAGEMENT (Gestione dello Stato e del Cursore)
            // Usiamo TextFieldValue invece di String. Una String contiene solo i caratteri.
            // TextFieldValue è una data class complessa che memorizza:
            // - text: La stringa attuale.
            // - selection: L'oggetto TextRange che definisce dove si trova il cursore o la selezione evidenziata.
            // - composition: Usato per le tastiere predittive.
            var scoreInput by remember {
                //dichiaro la variabile immutabile initialText e dico che è uguale alla variabile di stato che contiene il giocatore con un valore non nullo e lo converto in una stringa con ".toString()"
                val initialText = playerForManualEdit!!.score.toString()
                mutableStateOf(
                    TextFieldValue(
                        text = initialText,
                        // TextRange mappa un intervallo. Passando un solo valore (la lunghezza della stringa),
                        // diciamo al motore di rendering del testo di piazzare il 'Caret' (cursore) alla fine.
                        selection = TextRange(initialText.length)
                    )
                )
            }

            // 2. FOCUS TREE (Albero del Focus)
            // FocusRequester è un proxy che ci permette di inviare comandi all'albero del focus di Compose.
            // Lo agganceremo al Modifier dell'OutlinedTextField più in basso.
            val focusRequester = remember { FocusRequester() }

            // 3. SIDE EFFECTS (Effetti Collaterali)
            // LaunchedEffect entra nel ciclo di vita della composizione. La chiave 'Unit' significa
            // che la coroutine al suo interno verrà lanciata una sola volta (al momento dell'attacco del nodo).
            LaunchedEffect(Unit) {
                // Sospendiamo la coroutine per 100 millisecondi.
                // Motivo tecnico: L'AlertDialog ha un'animazione di entrata (Fade-In/Scale-In).
                // Se richiediamo il focus prima che il layout node sia stato completato e misurato dal sistema
                // (Window Manager), la richiesta viene scartata (Ignored). Il delay garantisce che il nodo sia pronto.
                delay(100)
                focusRequester.requestFocus() // Spinge programmaticamente l'evento di focus sul nodo collegato.
            }

            // 4. COMPONENTE UI: ALERT DIALOG (Material 3)
            AlertDialog(
                // Callback invocata dal sistema se l'utente tocca lo 'Scrim' (sfondo oscurato) o preme il tasto Back.
                onDismissRequest = { playerForManualEdit = null },

                icon = {
                    Icon(
                        imageVector = Icons.Filled.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.titolo_modifica_punteggio), // 🌍 I18N
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column {
                        // ====================================================================
                        // 🧠 LEZIONE TEORICA I18N: Stringhe Dinamiche nel Testo Descrittivo
                        // ====================================================================
                        // Anche qui, nel testo discorsivo, usiamo il placeholder '%s'.
                        // Nel file XML: <string name="msg_inserisci_nuovo_punteggio">Inserisci il nuovo punteggio totale per %s:</string>
                        // Passiamo il nome del giocatore in modo pulito alla funzione stringResource.
                        Text(
                            text = stringResource(R.string.msg_inserisci_nuovo_punteggio, playerForManualEdit!!.name),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 5. COMPONENTE UI: CAMPO TESTUALE E TASTIERA
                        OutlinedTextField(
                            value = scoreInput, // Associa lo stato (TextFieldValue) alla UI
                            onValueChange = { newValue ->
                                // newValue è il nuovo stato emesso dalla tastiera.
                                // Estraiamo la proprietà 'text' per eseguire la nostra Regex/Validazione.
                                val newText = newValue.text
                                // Condizione logica: Accetta vuoto, segno meno isolato, o un intero valido.
                                if (newText.isEmpty() || newText == "-" || newText.toIntOrNull() != null) {
                                    // Se la validazione passa, sovrascriviamo lo stato con il NUOVO oggetto TextFieldValue
                                    // Questo mantiene sincronizzata sia la stringa che la posizione aggiornata del cursore.
                                    scoreInput = newValue
                                }
                            },
                            // Binding del FocusRequester al modificatore di questo specifico componente.
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),

                            // 🧠 DESIGN CHANGE: Stondatura ridotta a 12.dp per coerenza con i nuovi campi di testo (Module style)
                            shape = RoundedCornerShape(12.dp),

                            // singleLine disabilita il comportamento "multiline" (non crea a capo "\n")
                            // ed è propedeutico affinché imeAction venga rispettata dalla tastiera (IME).
                            singleLine = true,

                            // Configurazione dell'Input Method Editor (IME) del sistema Android.
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number, // Forza il layout numerico dell'hardware/software.
                                imeAction = ImeAction.Done          // Setta l'action button della tastiera su "Fatto/Spunta".
                            ),

                            // Intercettazione dell'evento emesso dalla tastiera (quando l'utente preme la Spunta).
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Rimuove il focus dal nodo attivo. Di conseguenza, il Window Manager di Android
                                    // nasconderà la tastiera automaticamente (Trigger implicito).
                                    //focusManager.clearFocus()

                                    // Casting sicuro: converte la stringa in Int, se fallisce (es. vuoto o "-") usa 0 (Elvis Operator).
                                    val newScore = scoreInput.text.toIntOrNull() ?: 0
                                    // Calcolo del delta per non distruggere le statistiche/combo basate sull'aggiunta.
                                    val diff = newScore - playerForManualEdit!!.score
                                    viewModel.updatePlayerScore(playerForManualEdit!!, diff)

                                    // Azzera lo stato, innescando la recomposition che distruggerà l'AlertDialog.
                                    playerForManualEdit = null
                                }
                            )
                        )
                    }
                },
                confirmButton = {
                    // Layout orizzontale per i pulsanti (Pattern Material 3: Tasti affiancati a peso uguale)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // OutlinedButton è usato per azioni secondarie/distruttive deboli (Cancel/Annulla).
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                playerForManualEdit = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            // 🧠 FIX UI: Rimossa virgola finale ridondante che sporcava il codice.
                            AutoResizedText(
                                text = stringResource(R.string.btn_annulla),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            ) // 🌍 I18N: Riutilizzo chiave Annulla
                        }

                        // Filled Button è usato per l'azione primaria e affermativa (Confirm/Salva).
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                // Replicazione esatta della business logic presente nella KeyboardActions(onDone)
                                val newScore = scoreInput.text.toIntOrNull() ?: 0
                                val diff = newScore - playerForManualEdit!!.score
                                viewModel.updatePlayerScore(playerForManualEdit!!, diff)
                                playerForManualEdit = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(stringResource(R.string.btn_salva), fontWeight = FontWeight.Bold) // 🌍 I18N: Riutilizzo chiave Salva
                        }
                    }
                },

                dismissButton = null,// Spegniamo il tasto di default perché abbiamo integrato "Annulla" nella Row
                shape = RoundedCornerShape(24.dp),// Angoli arrotondati dell'intero popup
                containerColor = MaterialTheme.colorScheme.surfaceVariant//'containerColor' definisce il colore del "corpo" del dialog.
                // Si possono usare:
                // - MaterialTheme.colorScheme.surface (Classico)
                // - MaterialTheme.colorScheme.surfaceVariant (Leggermente più grigio/scuro)
                // - Color.White (Bianco puro, se si vuole massima luminosità)
            )
        }

        // --------------------------------------------------------------------
        // DIALOGO DI CONFERMA: AZZERAMENTO PUNTEGGI
        // --------------------------------------------------------------------
        // L'espressione 'if (showResetDialog)' agisce come un interruttore di rendering.
        // In Compose, se questa condizione è falsa, il codice al suo interno non viene
        // ignorato, ma non viene proprio disegnato sullo schermo.
        if (showResetDialog) {

            // AlertDialog è il componente standard Material 3 per creare popup modali.
            // Un popup modale blocca tutte le interazioni con la schermata sottostante.
            AlertDialog(
                // onDismissRequest è un evento che viene scatenato da Android quando l'utente
                // clicca lo sfondo scuro fuori dal popup, oppure preme il tasto indietro fisico.
                onDismissRequest = {
                    showResetDialog = false
                },

                // icon posiziona un'immagine in alto al centro del popup.
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Warning, // L'icona vettoriale predefinita di sistema
                        contentDescription = stringResource(R.string.desc_attenzione_icon), // 🌍 I18N: Testo invisibile letto dagli screen reader per ciechi
                        // tint colora l'icona. Usiamo MaterialTheme.colorScheme.error per
                        // estrarre il colore rosso dinamico previsto dal tema del telefono.
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp),
                    )
                },

                // title è l'intestazione in grassetto.
                title = {
                    Text(
                        text = stringResource(R.string.titolo_conferma_azzeramento), // 🌍 I18N
                        fontWeight = FontWeight.Bold
                    )
                },

                // text è il corpo del messaggio.
                text = {
                    Text(
                        text = stringResource(R.string.msg_conferma_azzeramento), // 🌍 I18N
                        textAlign = TextAlign.Justify,//l'oggetto TextAlign con Justify ci permette di giustificare il testo
                    )
                },

                // confirmButton è l'area in basso a destra destinata ai pulsanti.
                // Per aggirare il suo allineamento nativo a destra, passiamo come parametro
                // una Row (Riga) che prenderà il controllo totale dello spazio.
                confirmButton = {
                    Row(
                        // Modifier.fillMaxWidth() impone alla riga di allargarsi orizzontalmente al 100%.
                        modifier = Modifier.fillMaxWidth(),
                        // horizontalArrangement definisce la gestione dello spazio vuoto.
                        // spacedBy(12.dp) inietta esattamente 12 pixel di spazio rigido e non
                        // comprimibile tra i pulsanti che inseriremo all'interno della riga.
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // OutlinedButton è un pulsante trasparente delineato solo da un bordo.
                        // Visivamente indica un'azione secondaria (di fuga o annullamento).
                        OutlinedButton(
                            // onClick è una lambda (una funzione): definisce il codice che
                            // verrà eseguito nel millisecondo esatto in cui il pulsante viene premuto.
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showResetDialog = false
                            },
                            // I Modifiers in Compose sono a cascata e definiscono forma e spazio.
                            modifier = Modifier
                                // weight(1f) è una proporzione. Essendoci due bottoni con weight(1f),
                                // Compose calcola lo spazio totale, toglie i 12dp centrali,
                                // e divide il resto esattamente in due metà uguali (50% e 50%).
                                .weight(1f)
                                // height(48.dp) forza l'altezza verticale a 48 pixel (standard accessibilità touch).
                                .height(48.dp),
                            // shape definisce i contorni. RoundedCornerShape smussa gli angoli.
                            // 20.dp è un raggio molto alto, creando il tipico effetto a "pillola".
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            AutoResizedText(text = stringResource(R.string.btn_annulla),style = MaterialTheme.typography.labelLarge,fontWeight = FontWeight.Bold,) // 🌍 I18N: Riutilizzo chiave Annulla
                        }

                        // Button è il pulsante pieno (Filled) standard. Indica l'azione primaria.
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // viewModel è l'oggetto che contiene le logiche di calcolo.
                                // Invochiamo la sua funzione per manipolare la lista punti in background.
                                viewModel.resetScoresWithUndo()
                                showResetDialog = false
                            },
                            // Riapplichiamo l'esatto stesso Modifier del pulsante precedente
                            // per garantire la perfetta simmetria geometrica.
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(20.dp),
                            // colors permette di sovrascrivere lo schema colori predefinito del pulsante.
                            colors = ButtonDefaults.buttonColors(
                                // containerColor stabilisce il colore di riempimento.
                                // Applichiamo la semantica 'error' (rosso) per indicare azioni irreversibili.
                                containerColor = MaterialTheme.colorScheme.error,
                                // contentColor stabilisce il colore di ciò che sta dentro il bottone (es: il testo).
                                // 'onError' è un colore generato da sistema per essere sempre leggibile sul rosso.
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.btn_conferma_azzera), // 🌍 I18N
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }

        // --------------------------------------------------------------------
        // DIALOGO DI CONFERMA: FINE PARTITA E SALVATAGGIO AUTOMATICO
        // --------------------------------------------------------------------
        if (showEndMatchDialog) {
            AlertDialog(
                onDismissRequest = {
                    // Se l'utente tocca fuori dal popup, consideriamolo come un "Annulla"
                    showEndMatchDialog = false
                    viewModel.startTimer() // 🕒 FIX: Facciamo ripartire il tempo!
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.titolo_conferma_fine), // 🌍 I18N
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.msg_conferma_fine), // 🌍 I18N
                        textAlign = TextAlign.Justify,
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // TASTO DI SINISTRA: ANNULLA E RIPRENDI
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEndMatchDialog = false
                                // 🕒 FIX: L'utente ha cambiato idea. La partita continua, il tempo riparte!
                                viewModel.startTimer()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            AutoResizedText(text = stringResource(R.string.btn_annulla), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        // TASTO DI DESTRA: CONCLUDI E SALVA
                        Button(
                            onClick = {
                                // IL MOMENTO DELLA VERITÀ: Tentiamo di chiudere il lucchetto!
                                if (viewModel.lockNavigation()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showEndMatchDialog = false
                                    viewModel.pauseTimer()

                                    // 💾 LA LEZIONE: SALVATAGGIO AUTOMATICO (AUTO-SAVE)
                                    // Salviamo silenziosamente e istantaneamente i dati.
                                    viewModel.saveCurrentMatch()

                                    onNavigateToResults()
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.btn_concludi), // 🌍 I18N
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }

        // --------------------------------------------------------------------
        // DIALOGO SALVA-VITA: USCITA ACCIDENTALE
        // --------------------------------------------------------------------
        if (showExitWarning) {
            AlertDialog(
                // 🧠 FIX UI: Sistemata la lambda onDismissRequest. 
                // Se l'utente clicca fuori dal popup, resettiamo correttamente lo stato.
                onDismissRequest = { showExitWarning = false },

                // L'icona in alto al centro cattura l'attenzione e contestualizza l'azione.
                icon = {
                    Icon(
                        // ExitToApp è l'icona standard Material per l'abbandono di una schermata o app.
                        // Usiamo AutoMirrored per garantire il corretto orientamento in lingue RTL (Right-to-Left).
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.desc_uscita_icon), // 🌍 I18N: Descrizione per non vedenti
                        // Essendo un'azione potenzialmente distruttiva, applichiamo il colore di errore.
                        tint = MaterialTheme.colorScheme.error,
                        // Ingrandiamo l'icona per darle maggiore peso visivo, come fatto nei dialoghi precedenti.
                        modifier = Modifier.size(36.dp),
                    )
                },
                title = {
                    AutoResizedText(
                        text = stringResource(R.string.titolo_conferma_uscita), // 🌍 I18N
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.msg_conferma_uscita), // 🌍 I18N
                        textAlign = TextAlign.Justify,//l'oggetto TextAlign con Justify ci permette di giustificare il testo
                    )
                },
                // Replicando l'architettura del popup precedente, creiamo coerenza per l'utente.
                // Imparerà che due bottoni larghi colorati in un certo modo significano sempre la stessa cosa.
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showExitWarning = false
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            AutoResizedText(stringResource(R.string.btn_annulla),style = MaterialTheme.typography.labelLarge,fontWeight = FontWeight.Bold,) // 🌍 I18N: Riutilizzo chiave
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showExitWarning = false
                                // onNavigateHome è una lambda passata dal livello superiore dell'app.
                                // Serve per istruire il "NavHost" a distruggere questa schermata e mostrare la Home.
                                onNavigateHome()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.btn_conferma_uscita), // 🌍 I18N
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }

        // ====================================================================
        // POPUP IMPOSTAZIONI DADO
        // ====================================================================
        if (showDiceSettingsDialog) {
            DiceSettingsDialog(
                currentSides = viewModel.diceSides,
                onSidesChanged = { newSides -> viewModel.diceSides = newSides },
                onDismiss = {
                    showDiceSettingsDialog = false
                    // Quando chiudo le impostazioni, riapro automaticamente il dado per lanciare!
                    showDiceDialog = true
                }
            )
        }

        // ---> POPUP DEL DADO E SUE IMPOSTAZIONI <---
        if (showDiceDialog) {
            DiceRollDialog(
                // Parametri obbligatori per l'animazione e il calcolo del numero
                result = diceResult,
                rollCount = diceRollCount,
                diceSides = viewModel.diceSides,

                // Logica del rilancio
                onRollAgain = {
                    diceResult = (1..viewModel.diceSides).random()
                    diceRollCount++
                },
                onDismiss = { showDiceDialog = false },

                // Logica per aprire le impostazioni
                onOpenSettings = {
                    showDiceDialog = false
                    showDiceSettingsDialog = true
                }
            )
        }

        // POPUP TUTORIAL: SPIEGAZIONE DELLE MECCANICHE NASCOSTE DELL'APP
        if (showInfoDialog) {
            FeatureInfoDialog(
                onDismissRequest = {
                    showInfoDialog = false
                }
            )
        }
        // ---> POPUP DEL TIMER <---
        // Viene invocato dal file TimerComponents.kt che abbiamo creato a parte
        if (showTimerDialog) {
            TimerSettingsDialog(
                // 1. Passagli la memoria salvata
                initialRawInput = savedTimerMemory,

                // 2. Quando l'utente digita, salva il nuovo numero nella cassaforte
                onInputChanged = { nuovoValore ->
                    savedTimerMemory = nuovoValore
                },

                onDismiss = {
                    showTimerDialog = false
                },

                // 🧠 CONDIVISIONE DELLO STATO (Single Source of Truth):
                // Passiamo esplicitamente l'istanza condivisa di MatchViewModel. In questo modo
                // i preset del timer modificati nel dialogo aggiornano la memoria dell'app
                // e vengono inclusi immediatamente nell'esportazione del file di backup JSON.
                viewModel = viewModel
            )
        }

        // Dialogo modale per la visualizzazione della cronologia punteggi e annulla ultima mossa
        if (showScoreHistoryDialog) {
            ScoreHistoryDialog(
                viewModel = viewModel,
                onDismissRequest = {
                    showScoreHistoryDialog = false
                }
            )
        }
    }


