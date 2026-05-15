package com.n380.scorecounter.ui.screens

import android.app.Activity
import com.n380.scorecounter.ui.components.AutoResizedText
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
//Importiamo la classe Lifecycle per gestire il ciclo di vita della schermata.
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.ui.components.PlayerScoreCard
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.delay

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

    // Variabile che indica se mostrare l'avviso di uscita "Salva-Vita" (se si preme Indietro per sbaglio)
    var showExitWarning by remember { mutableStateOf(false) }

    // Variabile per ricordare il tempo impostato manualemnte nel timer:
    var savedTimerMemory by rememberSaveable { mutableStateOf("") }

    // ---> STATI PER IL DADO VIRTUALE <---
    var showDiceDialog by remember { mutableStateOf(false) }

    // ---> STATI PER IL PULSANTE TIMER VIRTUALE <---
    var showTimerDialog by remember { mutableStateOf(false) }

    var diceResult by remember { mutableIntStateOf(1) }
    // Contatore univoco per forzare l'aggiornamento dell'interfaccia ad ogni click
    var diceRollCount by remember { mutableIntStateOf(0) }

    // --->Ricorda QUALE giocatore stiamo modificando manualmente con la tastiera <---
    // Se è "null", il popup per l'inserimento manuale è nascosto.
    var playerForManualEdit by remember { mutableStateOf<Player?>(null) }

    // STATO TUTORIAL: Controlla l'apertura del popup centrale con le regole nascoste (Manuale)
    var showInfoDialog by remember { mutableStateOf(false) }

    // SISTEMA SNACKBAR (Tasto Annulla Azzeramento)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current // Recuperiamo il gestore del focus, ci serve altrimenti anche se chiudiamo la tastiera la text area rimane sempre su OnFocus (quindi attiva)

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current


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

    // ---> SCHERMO SEMPRE ACCESO (Senza modalità immersiva problematica) <---
    // DisposableEffect è magico: esegue un codice quando la pagina si APRE (init) e un altro quando si CHIUDE (onDispose).
    DisposableEffect(Unit) {
        val activity = context as? Activity
        // Appena si apre il contatore, diciamo ad Android di attaccare il "Cartello Divieto di Standby"
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // Appena usciamo da questa pagina, togliamo il cartello e permettiamo allo schermo di riposare.
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

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
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            diceRollCount++//serve per incrementare il contatore che ricorda il punteggio precendente del dado
                            showResetDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp), // Altezza Expressive massiccia
                        shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    ) {
                        AutoResizedText(
                            text = "Azzera",
                            // Utilizziamo lo stile titleLarge (Proprietà di MaterialTheme)
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ========================================================
                    // TASTO PRIMARIO: FINE MATCH (CALL TO ACTION)
                    // ========================================================
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.pauseTimer()
                            onNavigateToResults()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp), // Altezza Expressive massiccia
                        shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        AutoResizedText(
                            text = "Fine Match",
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
            AutoResizedText(
                text = if (viewModel.matchTitle.isEmpty()) "Sfida" else viewModel.matchTitle,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
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
                shape = RoundedCornerShape(24.dp),
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

                    // --- BLOCCO SINISTRO: Informazioni e Tempo di Gioco ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ICONA INFO: Pulsante compatto per le regole
                        FilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showInfoDialog = true
                            },
                            modifier = Modifier.size(38.dp), // Ingrandito per facilità di tocco (Standard Accessibilità)
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // DISPLAY CRONOMETRO: Tempo trascorso dall'inizio (Durata Match)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                // Abbiamo sostituito Icons.Filled.Timer con Icons.Filled.Schedule (un orologio classico)
                                // per distinguere visivamente il tempo trascorso dal pulsante "Timer" (conto alla rovescia).
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp).padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatTime(viewModel.matchDurationSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // --- BLOCCO DESTRO: Azioni Rapide (Timer e Dado) ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PULSANTE TIMER: Per impostare conti alla rovescia
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showTimerDialog = true
                            },
                            modifier = Modifier.height(48.dp), // Altezza portata a 48dp per standard touch target
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Filled.Timer, null, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                            Text("Timer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        // PULSANTE DADO: Per estrazioni casuali
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                diceResult = (1..viewModel.diceSides).random()
                                showDiceDialog = true
                            },
                            modifier = Modifier.height(48.dp), // Altezza portata a 48dp per standard touch target
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Filled.Casino, null, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                            AutoResizedText("Dado", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ---> CALCOLO DELLA CORONA DEL LEADER IN TEMPO REALE <---
            // Per assegnare la corona, dobbiamo sapere chi ha il punteggio più alto in questo esatto millisecondo.
            // La funzione maxOfOrNull scansiona tutti i giocatori e trova il punteggio massimo (se sono tutti a 0, assegna 0 di default).
            val maxScore = viewModel.players.maxOfOrNull { it.score } ?: 0

            // ---> CONTROLLO VITTORIA AUTOMATICA <---
            // LaunchedEffect(maxScore) è un "osservatore spia". Gli diciamo di tenere d'occhio la variabile "maxScore".
            // Ogni volta che i punti di un giocatore cambiano alzando l'asticella, lui esegue questo blocco alla velocità della luce.
            LaunchedEffect(maxScore) {
                // Trasformiamo il testo dell'obiettivo in un numero vero (Int). Se il campo è vuoto, diventa "null".
                val target = viewModel.targetScore.toIntOrNull()

                // Regola della Vittoria: Se l'obiettivo esiste, è maggiore di 0, e il leader ha raggiunto o superato l'obiettivo...
                if (target != null && target > 0 && maxScore >= target) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibrazione della vittoria!
                    viewModel.pauseTimer() // Fermiamo l'orologio
                    onNavigateToResults() // Passiamo automaticamente alla schermata finale dei coriandoli!
                }
            }

            // ---> CONTROLLO ANTI-CARTE <---
            // Trasformiamo il titolo in minuscolo e cerchiamo la parola "carte".
            // Se trovata, 'isFireEnabled' sarà false e il colore arancione della combo non apparirà mai.
            val isFireEnabled = !viewModel.matchTitle.lowercase()
                .contains("carte")//Questa variabile è VERA se il titolo NON contiene la parola "carte".

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
                Column(
                    // Abbiamo ridotto il padding 'top' perché non essendoci più il titolo,
                    // non serve più tutto quello spazio vuoto in alto.
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 12.dp, end = 12.dp)
                ) {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        // ---> MODIFICA 3: Cuscinetto ridotto <---
                        // Siccome la scatola grigia ora finisce PRIMA dei bottoni e non ci scivola più dietro,
                        // non ci serve più quel trucco del padding a 100.dp. Bastano 16.dp per far
                        // scorrere bene l'ultima carta senza farla incollare al bordo inferiore.
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(viewModel.players) { p ->

                            val isLeader = p.score == maxScore && maxScore > 0

                            PlayerScoreCard(
                                player = p,
                                isLeader = isLeader,
                                isFireEnabled = isFireEnabled,
                                onScoreChange = { amount ->
                                    viewModel.updatePlayerScore(p, amount)
                                },
                                onScoreClick = { playerForManualEdit = p }
                            )
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
                    text = "Modifica Punteggio",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Inserisci il nuovo punteggio totale per ${playerForManualEdit!!.name}:",
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

                        shape = RoundedCornerShape(16.dp),

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
                        AutoResizedText("Annulla", style = MaterialTheme.typography.labelLarge,color = MaterialTheme.colorScheme.onSurface,fontWeight = FontWeight.Bold,)
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
                        Text("Salva", fontWeight = FontWeight.Bold)
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
                    contentDescription = "Attenzione", // Testo invisibile letto dagli screen reader per ciechi
                    // tint colora l'icona. Usiamo MaterialTheme.colorScheme.error per
                    // estrarre il colore rosso dinamico previsto dal tema del telefono.
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp),
                )
            },

            // title è l'intestazione in grassetto.
            title = {
                Text(
                    text = "Vuoi proprio azzerare?",
                    fontWeight = FontWeight.Bold
                )
            },

            // text è il corpo del messaggio.
            text = {
                Text(
                    text = "Sei sicuro di voler azzerare tutto?\nQuesta azione non può essere annullata.",
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
                        AutoResizedText(text = "Annulla",style = MaterialTheme.typography.labelLarge,fontWeight = FontWeight.Bold,)
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
                            text = "Sì, azzera",
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
            onDismissRequest = { showExitWarning = false },

            // L'icona in alto al centro cattura l'attenzione e contestualizza l'azione.
            icon = {
                Icon(
                    // ExitToApp è l'icona standard Material per l'abbandono di una schermata o app.
                    // Usiamo AutoMirrored per garantire il corretto orientamento in lingue RTL (Right-to-Left).
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Attenzione Uscita",
                    // Essendo un'azione potenzialmente distruttiva, applichiamo il colore di errore.
                    tint = MaterialTheme.colorScheme.error,
                    // Ingrandiamo l'icona per darle maggiore peso visivo, come fatto nei dialoghi precedenti.
                    modifier = Modifier.size(36.dp),
                    )
            },
            title = {
                AutoResizedText(
                    text = "Abbandonare la partita?",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Se torni alla Home, i progressi attuali andranno persi per sempre. Sei sicuro di voler uscire?",
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
                        AutoResizedText("Annulla",style = MaterialTheme.typography.labelLarge,fontWeight = FontWeight.Bold,)
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
                            text = "Sì, esci",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    // ---> POPUP DEL DADO VIRTUALE (Esternalizzato in DiceComponents.kt) <---
    if (showDiceDialog) {
        DiceRollDialog(
            result = diceResult,
            diceSides = viewModel.diceSides,
            rollCount = diceRollCount, // <-- Passiamo il nuovo parametro
            onRollAgain = {
                // Genera un nuovo numero casuale e lo salva
                diceResult = (1..viewModel.diceSides).random()
                diceRollCount++ // <-- Incrementiamo ad ogni nuovo lancio
            },
            onDismiss = { showDiceDialog = false }
        )
    }

    // POPUP TUTORIAL: SPIEGAZIONE DELLE MECCANICHE NASCOSTE DELL'APP
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = {
                showInfoDialog = false
            }, // Se l'utente clicca fuori dal popup, si chiude
            title = { Text("Info funzionalità", fontWeight = FontWeight.Bold) },
            text = {
                // verticalScroll permette di scorrere il testo col dito se lo schermo del telefono è troppo piccolo
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    Text(
                        "🚀 Punteggio Rapido",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 8.dp)
                    )
                    Text(
                        "Tieni premuto il tasto '+' per aggiungere 10 punti o il tasto '-' per toglierne 5 in modo rapido.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )

                    Text(
                        "🔥 Stato On Fire",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                    )
                    Text(
                        "Se un giocatore segna 3 volte di fila senza interruzioni da parte degli altri, il suo punteggio diventa arancione. \nNon si applica nelle sfide a carte.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )

                    Text(
                        "⌨️ Modifica Manuale",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                    )
                    Text(
                        "Clicca direttamente sul numero del punteggio per aprire la tastiera e inserire un valore preciso a piacere.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )

                    Text(
                        "🎲 Dado Fortunato",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                    )
                    Text(
                        "Usa il tasto 'Dado' per generare un numero casuale e decidere chi inizia o risolvere dispute tra amici.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )

                    Text(
                        "⏱️ Timer Personalizzato",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                    )
                    Text(
                        "Usa il tasto 'Timer' per impostare un conto alla rovescia. L'app ti avviserà con una vibrazione e un suono al termine del tempo.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )
                }
            },
            confirmButton = {
                // Pulsante pieno (Button) al posto del TextButton, con la nostra stondatura ufficiale a 20.dp
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                        showInfoDialog = false// Chiude il popup quando si preme il bottone
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),

                        )
                    Spacer(Modifier.width(8.dp))
                    AutoResizedText("Ho capito", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }

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
            }
        )
    }
    }

