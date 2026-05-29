package com.n380.scorecounter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.n380.scorecounter.R
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Search//icone per la barra di ricerca
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.model.MatchRecord
import com.n380.scorecounter.model.getHistoricalCecchino
import com.n380.scorecounter.model.getHistoricalFenice
import com.n380.scorecounter.model.getHistoricalGambero
import com.n380.scorecounter.model.getHistoricalInarrestabile
import com.n380.scorecounter.ui.components.AboutAppDialog
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.DonationDialog
import com.n380.scorecounter.ui.components.FilledActionPill
import com.n380.scorecounter.ui.components.ScoreChart
import com.n380.scorecounter.ui.components.SlideUpAnimatedVisibility
import com.n380.scorecounter.ui.components.TonalActionPill
import com.n380.scorecounter.ui.components.buildMatchShareText
import com.n380.scorecounter.ui.components.formatDate
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.ui.components.launchShareIntent
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// L'annotazione @OptIn serve a sopprimere gli avvisi del compilatore quando si utilizzano
// API che Google considera ancora "sperimentali" o soggette a modifiche future.
// Qui è necessaria per poter utilizzare Modifier.combinedClickable().
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MatchViewModel,
    // I parametri di tipo "() -> Unit" sono funzioni di callback (Lambda).
    // Invece di passare un dato, passiamo un'azione. Quando l'utente preme un tasto qui,
    // eseguiamo questa funzione per delegare la navigazione al file MainActivity che gestisce le rotte.
    onNavigateToCreate: () -> Unit,
    onNavigateToCounter: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    // --------------------------------------------------------------------
    // COMPOSITION LOCALS E COROUTINES
    // --------------------------------------------------------------------
    // CompositionLocal è un meccanismo di Compose per passare dati impliciti attraverso l'albero della UI
    // senza doverli passare manualmente in ogni singola funzione come parametro.
    val haptic = LocalHapticFeedback.current // Fornisce l'accesso al motore di vibrazione hardware.
    val context =
        LocalContext.current // Il contesto Android base, necessario per lanciare Intent (es. condivisione).

    // SnackbarHostState gestisce la coda dei messaggi a comparsa (Snackbar).
    // remember fa sì che l'oggetto non venga ricreato a ogni ricomposizione della UI.
    val snackbarHostState = remember { SnackbarHostState() }

    // rememberCoroutineScope apre un canale sicuro per lanciare processi asincroni (background)
    // legati al ciclo di vita di questa schermata. Se la schermata muore, i processi si fermano.
    val coroutineScope = rememberCoroutineScope()

    // PROPRIETÀ: Formattatore per l'ora (HH:mm -> es. 14:30)
    // Utilizziamo la Classe SimpleDateFormat
    val timeFormatter = remember {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    }

    // --------------------------------------------------------------------
    // GESTIONE DELLO STATO (STATE MANAGEMENT)
    // --------------------------------------------------------------------
    // mutableIntStateOf crea una variabile "osservabile". Quando il suo valore cambia,
    // Compose ricalcola (ricompone) automaticamente solo le parti di UI che la stanno leggendo.
    // rememberSaveable fa in modo che il dato sopravviva non solo alle ricomposizioni,
    // ma anche ai cambi di configurazione del sistema (come la rotazione del display).
    var expandedMatchIndex by rememberSaveable { mutableIntStateOf(-1) }


    // Stato per la visibilità del dialogo "Informazioni App"
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    // GESTIONE DI STATO (UI State):
    // Variabile booleana che governa l'iniezione nel grafo grafico del dialogo "Supporta il progetto".
    // Utilizziamo 'rememberSaveable' (non un semplice 'remember') affinché il booleano venga
    // salvato nel 'Bundle' di sistema. Se l'utente ruota lo schermo mentre sta leggendo
    // il tuo messaggio, il sistema operativo non distruggerà e chiuderà il popup inavvertitamente.
    var showDonationDialog by rememberSaveable { mutableStateOf(false) }

    // Valutazione reattiva: Se l'indice è valido (>= 0), recuperiamo i dati della partita dal ViewModel.
    val expandedMatch =
        if (expandedMatchIndex >= 0) viewModel.history.getOrNull(expandedMatchIndex) else null

    // Variabile di cache (Memoria Fantasma). Usa solo 'remember' perché deve sopravvivere solo
    // per pochi millisecondi durante l'animazione di uscita, non serve salvarla nel disco.
    var lastMatch by remember { mutableStateOf<MatchRecord?>(null) }

    // Sincronizzazione: ogni volta che expandedMatch riceve una partita, aggiorniamo la cache.
    if (expandedMatch != null) {
        lastMatch = expandedMatch
    }

    // ====================================================================
    // Animazione Anti-Crash (Lifecycle Observer)
    // ====================================================================
    val lifecycleOwner = LocalLifecycleOwner.current
    var showNewMatchButton by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Quando la schermata torna in primo piano, INNESCA L'ANIMAZIONE
                showNewMatchButton = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                // Quando l'utente va via (es. entra nella partita), NASCONDI il bottone
                // così sarà pronto per essere ri-animato al suo ritorno.
                showNewMatchButton = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --------------------------------------------------------------------
    // INTERCETTAZIONE EVENTI DI SISTEMA
    // --------------------------------------------------------------------
    // BackHandler cattura l'evento "Pulsante Indietro" (fisico o gesture) a livello di sistema.
    // L'argomento 'enabled' è una condizione booleana: se è false, il BackHandler si disattiva e
    // cede il controllo ad Android (che chiuderà l'app). Se è true, lo intercetta e spara la sua lambda.
    BackHandler(enabled = expandedMatch != null) {
        // Riportando l'indice a -1, inneschiamo una reazione a catena: expandedMatch diventa null,
        // e AnimatedVisibility in fondo al file lancia l'animazione di uscita.
        expandedMatchIndex = -1
    }

    // --------------------------------------------------------------------
    // DIALOGO MODALE: RIPRISTINO PARTITA IN SOSPESO
    // --------------------------------------------------------------------
    if (viewModel.showResumeMatchDialog) {
        AlertDialog(
            // Assegnando una lambda vuota { } a onDismissRequest, stiamo intenzionalmente
            // bloccando la chiusura del popup tramite tocco esterno o tasto indietro.
            // Questo forza l'utente a prendere una decisione esplicita (Riprendi o Cancella).
            onDismissRequest = { },

            // L'inserimento dell'icona sposta automaticamente il layout del dialogo
            // secondo le specifiche Material 3, centrando icona e titolo per dare enfasi.
            icon = {
                Icon(
                    imageVector = Icons.Filled.Restore, // Oppure .PauseCircle o .SportsEsports
                    contentDescription = stringResource(R.string.desc_ripristina_salvataggio), // Descrizione accessibilità per l'icona di ripristino
                    // Essendo un'opportunità per l'utente (non un'azione distruttiva),
                    // usiamo il colore primario dell'app e non quello di errore.
                    tint = MaterialTheme.colorScheme.primary,
                    // L'uso di Modifier.size permette di alterare il bounding box (la scatola invisibile) dell'icona.
                    // Passando a 36.dp (o 48.dp se la vuoi gigantesca), aumentiamo l'ingombro visivo del 50%.
                    // Essendo un'immagine vettoriale (imageVector), si ingrandirà senza sgranare o perdere qualità.
                    modifier = Modifier.size(36.dp)

                )
            },

            title = {
                AutoResizedText(
                    text = stringResource(R.string.titolo_partita_in_sospeso), // Titolo del dialogo partita pendente
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.msg_riprendi_partita)) // Messaggio che chiede se riprendere la partita
            },

            // Per uniformare il Design System dell'app, bypassiamo l'allineamento
            // a destra predefinito di Material 3 raggruppando le azioni nel confirmButton.
            confirmButton = {
                Row(
                    // fillMaxWidth() forza il contenitore a occupare tutta la larghezza disponibile.
                    modifier = Modifier.fillMaxWidth(),
                    // spacedBy(12.dp) garantisce una separazione esatta e immutabile di 12 pixel
                    // tra i due pulsanti, creando un "respiro" visivo coerente.
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // AZIONE SECONDARIA / DISTRUTTIVA
                    // Utilizziamo un OutlinedButton per indicare che questa è l'opzione
                    // secondaria. Dal momento che l'azione comporta la perdita dei dati (cancellazione
                    // del backup), applichiamo la semantica 'Error' al contenuto e al bordo.
                    OutlinedButton(
                        onClick = {
                            // Invocazione della logica di business: purga il database dal salvataggio temporaneo
                            viewModel.clearBackup()
                            // Modifica dello stato osservato per smontare (nascondere) l'AlertDialog
                            viewModel.showResumeMatchDialog = false
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        },
                        modifier = Modifier
                            // Il weight(1f) su entrambi i pulsanti istruisce l'engine di rendering
                            // a dividere equamente lo spazio orizzontale rimanente (50/50).
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // Sovrascrittura mirata dei colori del bottone delineato per indicare pericolo
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        // Applichiamo un bordo spesso 1 pixel usando lo stesso colore di errore
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error
                        )
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.btn_cancella),
                            fontWeight = FontWeight.Bold,
                        ) // Testo pulsante per cancellare il backup
                    }

                    // AZIONE PRIMARIA / COSTRUTTIVA
                    // Il pulsante Filled rappresenta l'azione suggerita o principale (Happy Path).
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            // Invocazione della logica di business: carica i dati dal DB al ViewModel
                            viewModel.resumeBackupMatch()
                            // Callback di navigazione passata dal livello superiore per cambiare schermata
                            onNavigateToCounter()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.btn_riprendi), // Testo pulsante per riprendere la partita
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    // --------------------------------------------------------------------
    // DIALOGO MODALE: INFORMAZIONI APP (ABOUT)
    // --------------------------------------------------------------------
    // Essendo stato estratto in un file dedicato (HomeDialogComponents.kt),
    // qui applichiamo il principio dello "State Hoisting": passiamo solo la lambda di spegnimento.
    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    // --------------------------------------------------------------------
    // DIALOGO MODALE: MESSAGGIO DEL PROGRAMMATORE (DONAZIONI E SUPPORTO)
    // --------------------------------------------------------------------
    if (showDonationDialog) {
        DonationDialog(onDismiss = { showDonationDialog = false })
    }

    // --------------------------------------------------------------------
    // SCAFFOLD: L'ARCHITETTURA BASE E GLI "SLOT" MATERIAL
    // --------------------------------------------------------------------
    // Lo Scaffold non è un semplice contenitore, ma uno schema pre-fabbricato da Google.
    // Possiede degli "Slot" (buchi) specifici per posizionare gli elementi (TopBar, BottomBar, FAB).
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },


        // ====================================================================
        // NUOVO DOCK INFERIORE (Ancorato ai bordi dello schermo)
        // ====================================================================
        bottomBar = {
            // ---> LEZIONE: SURFACE ANCORATA (Senza margini esterni) <---
            // Abbiamo rimosso il 'modifier = Modifier.padding(...)' dalla Surface.
            // Senza margini esterni, la Surface si espande automaticamente fino a toccare
            // i bordi fisici laterali e il bordo inferiore dello schermo del telefono.
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                // ---> FIX GEOMETRICO: ARROTONDAMENTO PARZIALE <---
                // Usiamo topStart e topEnd a 24.dp per creare la curva morbida solo in alto.
                // Non specificando bottomStart e bottomEnd, essi rimangono a 0.dp (angoli retti),
                // permettendo al dock di "incollarsi" perfettamente alla base dello schermo.
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // ---> LEZIONE: PROTEZIONE DI SISTEMA <---
                        .navigationBarsPadding() //"spinge" in alto il contenuto interno solo di quel
                        // tanto che basta per non finire sotto la riga orizzontale bianca di Android.
                        //.navigationBarsPadding()
                        // ---> FIX ALTEZZA: PADDING INTERNO <---
                        // Qui decidiamo quanto il bottone è distante dai bordi del dock (la Surface).
                        // Usando 'horizontal = 16.dp' teniamo il bottone allineato con le card sopra.
                        // Usando 'vertical = 16.dp' riduciamo lo spazio vuoto sotto e sopra il bottone,
                        // evitando che risulti "troppo rialzato" rispetto alla base dello schermo.
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {

                    // -------------------------------------------
                    // IL GARAGE FISSO
                    // -------------------------------------------
                    // Definizione di un Box ad altezza invariabile (72dp) per stabilizzare il layout.
                    // La rimozione del padding orizzontale in questo livello assicura che il pulsante
                    // occupi l'intera larghezza consentita dal contenitore padre (Column),
                    // garantendo simmetria geometrica con la schermata CreateMatchScreen.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp), // Altezza bloccata per prevenire oscillazioni del tavolo soprastante
                        contentAlignment = Alignment.TopCenter // Posizionamento del contenuto al vertice centrale
                    ) {
                        // ====================================================================
                        // IL BOTTONE ANIMATO (Ora usa il componente globale DRY)
                        // ====================================================================
                        // Invochiamo il nostro guscio riutilizzabile. Tutta la logica di scorrimento,
                        // tempistiche e trasparenze è nascosta e sicura nel file AnimationComponents.kt
                        SlideUpAnimatedVisibility(
                            visible = showNewMatchButton
                        ) {
                            // Questo è il "content" che viene iniettato nello slot!
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Facciamo comunque la pulizia di sicurezza
                                    viewModel.clearMatch()
                                    onNavigateToCreate()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                shape = RoundedCornerShape(24.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp).size(28.dp)
                                )
                                Text(
                                    text = stringResource(R.string.btn_nuova_sfida), // Testo del pulsante flottante per una nuova sfida
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding -> // <-- L'innerPadding contiene le misure degli ingombri di sistema (orologio, barra di navigazione in basso e il nostro FAB)

        // --------------------------------------------------------------------
        // IL CONTENITORE PRINCIPALE E IL CONCETTO DI "EDGE-TO-EDGE"
        // --------------------------------------------------------------------
        // LEZIONE CRITICA: Abbiamo rimosso '.padding(innerPadding)' da questo Column.
        // Se lo avessimo lasciato, il Column si sarebbe fermato PRIMA del FAB, creando un vuoto.
        // Togliendolo, diciamo al Column di ignorare gli ingombri e di espandersi al 100%
        // dello schermo, "infilandosi" fisicamente anche sotto il FAB e la status bar.
        Column(modifier = Modifier.fillMaxSize()) {

            // INTESTAZIONE (Titolo e Bottone Statistiche)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Poiché il genitore (Column) ora ignora gli ingombri, dobbiamo dire manualmente
                    // all'intestazione di non finire "sotto" l'orologio e la batteria del telefono.
                    // calculateTopPadding() recupera i pixel esatti della barra di sistema superiore.
                    .padding(
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 24.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -------------------------------------------------
                // IL MODIFICATORE WEIGHT E IL CONTROLLO SPAZIALE
                // -------------------------------------------------
                // In una 'Row', gli elementi senza 'weight' prendono tutto lo spazio di cui hanno bisogno.
                // Se il titolo è troppo lungo, rischierebbe di finire "sotto" le icone a destra.
                //
                // LA SOLUZIONE:
                // 1. Modifier.weight(1f): Obbliga il titolo a occupare SOLO lo spazio che avanza
                //    dopo che le icone a destra si sono posizionate. Crea un confine invalicabile.
                // 2. padding(end = 16.dp): Garantisce una "zona di rispetto" tra la fine del testo
                //    e l'inizio della prima icona (il cuore), evitando che si tocchino.
                // 3. AutoResizedText: Avendo ora un confine preciso (il weight), il componente
                //    può calcolare quanto deve rimpicciolire il font per far stare tutto in una riga.
                // ====================================================================
                AutoResizedText(
                    text = stringResource(R.string.titolo_storico_sfide),//Titolo principale della schermata Home
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )

                // UX: Raggruppiamo i pulsanti in alto a destra in una sotto-riga (Row)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {



                    // ==========================================================
                    // 1. PULSANTE SUPPORTO (CUORE) - Terziario Pieno (Rosa)
                    // ==========================================================
                    FilledActionPill(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showDonationDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = stringResource(R.string.desc_donazione_icon),
                            tint = MaterialTheme.colorScheme.onTertiary, // Contrasto sul rosa
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // ==========================================================
                    // 2. PULSANTE INFO - Primario Pieno (Colore del Tema)
                    // ==========================================================
                    FilledActionPill(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showAboutDialog = true
                        },
                        // 🎨 Diciamo alla pillola di riempirsi con il colore Primario solido
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.desc_informazioni_app_icon),
                            // 🧠 FONDAMENTALE: onPrimary garantisce la leggibilità dell'icona!
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // ==========================================================
                    // 3. PULSANTE STATISTICHE - Primario Pieno (Colore del Tema)
                    // ==========================================================
                    if (viewModel.history.isNotEmpty()) {
                        FilledActionPill(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToStats()
                            },
                            // 🎨 Anche questo riempito con il colore Primario solido
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = stringResource(R.string.desc_statistiche_globali),
                                tint = MaterialTheme.colorScheme.onPrimary, // 🧠 Stessa regola di contrasto
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

                // ====================================================================
                // ---> IL TAVOLO DELLO STORICO (Scatola Grigia Contenitiva) <---
                // ====================================================================
                // Usiamo weight(1f) per dire alla Card: "Espanditi prendendo tutto lo spazio verticale vuoto"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp) // Allinea il tavolo all'intestazione in alto
                        // 🧠 FIX GEOMETRICO (L'illusione ottica svelata):
                        // Prima il tavolo scivolava DIETRO il dock inferiore. Aggiungendo 'innerPadding.calculateBottomPadding()'
                        // "appoggiamo" il fondo del tavolo esattamente sopra il dock, mantenendo 16.dp di respiro,
                        // e svelando i bordi inferiori stondati.
                        .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp) // Stondatura Material 3 massiccia
                ) {
                    // ISTANZIAMO IL FOCUS MANAGER
                    // Estrae il gestore globale del focus (che sa chi ha il cursore lampeggiante in questo momento)
                    val focusManager = LocalFocusManager.current
                    // --------------------------------------------------------------------
                    // MOTORE DI RICERCA: GESTIONE DELLO STATO (UI STATE)
                    // --------------------------------------------------------------------
                    // Dichiarazione dello stato locale per l'input di testo della barra di ricerca.
                    // - 'by': È un delegato Kotlin. Estrae direttamente la stringa dal wrapper MutableState.
                    // - 'rememberSaveable': Salva il dato nel 'Bundle' nativo di Android. Se l'utente
                    //   ruota lo schermo, la stringa digitata non viene distrutta. Questa cosa non accade con il solo remebre
                    // - 'mutableStateOf("")': Crea un "nodo osservabile". Quando cambia, Compose ridisegna.
                    var searchQuery by rememberSaveable { mutableStateOf("") }

                    // --------------------------------------------------------------------
                    // LOGICA DI FILTRAGGIO E MEMOIZATION (CACHE)
                    // --------------------------------------------------------------------
                    // 'remember(chiave1, chiave2)' ordina a Compose di eseguire il calcolo
                    // SOLO se cambia 'searchQuery' o 'viewModel.history'.
                    // Risparmia tantissima CPU evitando ricalcoli inutili durante lo scorrimento (Memoization).
                    val filteredHistory = remember(searchQuery, viewModel.history) {
                        if (searchQuery.isBlank()) {
                            // Nessuna ricerca = mostra tutto il database
                            viewModel.history
                        } else {
                            // Filtro attivo: analizziamo ogni partita
                            viewModel.history.filter { match ->
                                // 1. Titolo della partita contiene la stringa? (ignoreCase = ignora maiuscole)
                                val titleMatches =
                                    match.title.contains(searchQuery, ignoreCase = true)

                                // 2. Almeno un giocatore ha un nome che contiene la stringa?
                                val playerMatches = match.allPlayers.any { player ->
                                    player.name.contains(searchQuery, ignoreCase = true)
                                }

                                // Se una delle due è vera, il match viene tenuto nella lista visibile
                                titleMatches || playerMatches
                            }
                        }
                    }

                    // Struttura verticale per impilare la barra di ricerca sopra la lista scorrevole.
                    // Questa Colum incorpora l'INTERCETTAZIONE DEI TOCCHI A VUOTO SULLA COLUMN PADRE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            // pointerInput crea un recettore di eventi tattili su tutta l'area di questa colonna.
                            // detectTapGestures ascolta i tap: se l'utente tocca uno spazio vuoto (non una card),
                            // innesca 'focusManager.clearFocus()', spegnendo il cursore e abbassando la tastiera.
                            // N.B: Questo non bloccherà i click sulle singole partite, perché Compose è intelligente
                            // e dà la precedenza ai click sui "figli" prima che ai "padri".
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    focusManager.clearFocus()
                                })
                            }
                    ) {

                        // --------------------------------------------------------------------
                        // COMPONENTE UI: BARRA DI RICERCA (TextField)
                        // --------------------------------------------------------------------
                        // Appare solo se il database non è totalmente vuoto
                        if (viewModel.history.isNotEmpty()) {
                            OutlinedTextField(
                                value = searchQuery, // Legge il testo dalla variabile di stato
                                onValueChange = {
                                    searchQuery = it
                                }, // Aggiorna lo stato quando si digita
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp,
                                        bottom = 4.dp
                                    ),
                                placeholder = { AutoResizedText(stringResource(R.string.hint_cerca_sfida)) }, // Placeholder della barra di ricerca
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.desc_cerca_icon) // Descrizione accessibilità icona ricerca
                                    )
                                },
                                trailingIcon = {
                                    // Tasto "X" dinamico: esiste solo se c'è testo da cancellare
                                    if (searchQuery.isNotEmpty()) {
                                        TonalActionPill(
                                            onClick = {
                                                searchQuery = ""
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            },
                                            modifier = Modifier.padding(end = 8.dp) // Leggero margine dal bordo destro
                                        ) {
                                            // Nella HomeScreen mettiamo SOLO l'icona!
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = stringResource(R.string.btn_cancella),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp) // Dimensione proporzionata
                                            )
                                        }
                                    }
                                },

                                // OPZIONI DELLA TASTIERA E AZIONI
                                // 1. keyboardOptions = Trasforma il tasto "Invio" in basso a destra nella tastiera
                                // in un tasto con la lente d'ingrandimento (ImeAction.Search).
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),

                                // 2. keyboardActions = Cattura il momento esatto in cui l'utente preme
                                // quel tasto "Cerca" e innesca la funzione di rimozione del focus.
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        focusManager.clearFocus()
                                    }
                                ),

                                // 🧠 DESIGN CHANGE: Stondatura ridotta a 12.dp per coerenza con i nuovi campi di testo (Module style)
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true, // Impedisce di andare a capo premendo "Invio"
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.3f
                                    )
                                )
                            )
                        }

                        // --------------------------------------------------------------------
                        // GESTIONE DEGLI EMPTY STATE (STATI VUOTI) E RENDERING DELLA LISTA
                        // --------------------------------------------------------------------
                        if (viewModel.history.isEmpty()) {
                            // ====================================================================
                            // CASO 1: EMPTY STATE ASSOLUTO (Database Totalmente Vuoto)
                            // Innescato quando l'app è al primo avvio o dopo una pulizia dati.
                            // ====================================================================
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center // Allineamento baricentrico rispetto al contenitore padre
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally, // Centratura orizzontale dei figli
                                    verticalArrangement = Arrangement.Center // Compattazione verticale al centro
                                ) {
                                    //  Usiamo 'EmojiEvents' (il trofeo) per stimolare l'idea della vittoria.
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(80.dp) // Dimensione maggiorata rispetto alla ricerca per dare più peso visivo
                                            .padding(bottom = 16.dp),
                                        // 🧠 ALPHA CHANNEL: Portiamo l'opacità al 30% (0.3f).
                                        // In design, una trasparenza così alta indica uno "stato latente" o un "segnaposto".
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )

                                    // TITOLO PRINCIPALE: Usa il tuo stile Typography per coerenza
                                    AutoResizedText(
                                        text = stringResource(R.string.titolo_cronologia_vuota), // Messaggio quando lo storico è vuoto
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // CALL TO ACTION (INVITO ALL'AZIONE): Spieghiamo all'utente cosa fare.
                                    Text(
                                        text = stringResource(R.string.desc_inizia_sfida), // Invito a iniziare una nuova sfida
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center, // Centra il testo se va su due righe
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        modifier = Modifier.padding(
                                            top = 8.dp,
                                            start = 32.dp,
                                            end = 32.dp
                                        )
                                    )
                                }
                            }
                        } else if (filteredHistory.isEmpty()) {
                            // ====================================================================
                            // CASO 2: EMPTY STATE RELATIVO (Nessun Risultato di Ricerca)
                            // Innescato quando esistono dati, ma il filtro 'searchQuery' li esclude tutti.
                            // ====================================================================
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Usiamo 'Search' per indicare esplicitamente un fallimento del filtro.
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .padding(bottom = 16.dp),
                                        // Alpha al 50% per differenziarlo dallo stato assoluto (è un vuoto meno "grave")
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )

                                    // Il componente AutoResizedText, perfetto per gestire stringhe lunghe
                                    AutoResizedText(
                                        text = stringResource(R.string.titolo_nessun_risultato), // Messaggio quando la ricerca non produce risultati
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    AutoResizedText(
                                        text = stringResource(R.string.desc_prova_altra_ricerca), // Suggerimento per affinare la ricerca
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        } else {
                            // ====================================================================
                            // CASO 3: RENDERING DELLA LISTA (Dati Presenti e/o Filtrati)
                            // ====================================================================
                            // LAZYCOLUMN E IL SEGRETO DEL "CONTENT PADDING"
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),

                                // LEZIONE CRITICA: Cos'è il contentPadding?
                                // A differenza del 'modifier.padding' (che stringe la finestra dall'esterno),
                                // il 'contentPadding' aggiunge spazio *dentro* la fine della lista scorrrevole.
                                // Risultato visivo: le Card scorreranno liberamente "dietro" al FAB trasparente.
                                // Ma quando arrivi all'ultimo elemento della lista, questo non rimarrà nascosto
                                // sotto il bottone, perché la lista sa di dover aggiungere un margine finale
                                // pari all'ingombro del FAB in basso (calculateBottomPadding) più 16dp extra.
                                contentPadding = PaddingValues(
                                    top = 8.dp, // <--- Ridotto rispetto a prima perché c'è già la barra di ricerca sopra!
                                    start = 12.dp, // Leggermente ridotto perché ci pensa già il padding esterno della Card
                                    end = 12.dp,
                                    bottom = 16.dp
                                )
                            ) {
                                380
                                // MODIFICA FONDAMENTALE: Iteriamo su 'filteredHistory' invece che su 'viewModel.history'
                                items(filteredHistory) { record ->
                                    // Variabile di stato locale per gestire l'apertura/chiusura della singola card
                                    var expanded by remember { mutableStateOf(false) }

                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            expanded = !expanded
                                        },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(20.dp) // ARMONIA: Livello Moduli
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {

                                            // --- PARTE SEMPRE VISIBILE DELLA CARD ---
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // COMPONENTE: Testo del Titolo
                                                Text(
                                                    // PROPRIETÀ: La stringa letta dal database
                                                    text = record.title,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,

                                                    // LOGICA DI STATO PER L'ESPANSIONE:
                                                    // 'expanded' è una Variabile Booleana di Stato (MutableState).
                                                    // Se la card è aperta (true), assegniamo la Costante 'Int.MAX_VALUE' (Spazio infinito).
                                                    // Se la card è chiusa (false), limitiamo rigorosamente l'altezza a 1 singola riga.
                                                    maxLines = if (expanded) Int.MAX_VALUE else 1,

                                                    // PROPRIETÀ ENUM: TextOverflow
                                                    // Istruisce il motore grafico ad applicare i tre puntini (...)
                                                    // qualora il testo superi il limite imposto da maxLines.
                                                    overflow = TextOverflow.Ellipsis,

                                                    // MODIFICATORE: weight(1f)
                                                    // Fondamentale! Impone al Titolo di calcolare prima lo spazio occupato
                                                    // dal cronometro a destra, e poi di occupare SOLO lo spazio rimanente,
                                                    // impedendo al testo di spingere il timer fuori dallo schermo.
                                                    // Aggiungiamo padding(end = 12.dp) per non far incollare i tre puntini all'orologio.
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 12.dp)
                                                )

                                                if (record.durationSeconds > 0) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Filled.Timer, null,
                                                            modifier = Modifier.size(16.dp)
                                                                .padding(end = 4.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = formatTime(record.durationSeconds),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(top = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.label_vincitore,
                                                        record.winnerName
                                                    ), // Testo che indica il vincitore della partita
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Icon(
                                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // --- PARTE ESPANDIBILE (DETTAGLI E GRAFICO) ---
                                            AnimatedVisibility(visible = expanded) {
                                                Column(modifier = Modifier.padding(top = 20.dp)) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(
                                                            bottom = 12.dp
                                                        )
                                                    )

                                                    // Ciclo che genera la classifica dei giocatori
                                                    record.allPlayers.forEachIndexed { index, playerRecord ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            if (index == 0) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        Icons.Filled.EmojiEvents,
                                                                        null,
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.padding(
                                                                            end = 8.dp
                                                                        )
                                                                    )
                                                                    Text(
                                                                        text = stringResource(
                                                                            R.string.label_classifica_primo,
                                                                            playerRecord.name
                                                                        ), // Etichetta per il primo classificato
                                                                        style = MaterialTheme.typography.titleLarge,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            } else {
                                                                Text(
                                                                    text = stringResource(
                                                                        R.string.label_classifica_generico,
                                                                        index + 1,
                                                                        playerRecord.name
                                                                    ), // Etichetta per gli altri classificati (2°, 3°, ecc.)
                                                                    style = MaterialTheme.typography.bodyLarge
                                                                )
                                                            }

                                                            Text(
                                                                text = stringResource(
                                                                    R.string.label_punti,
                                                                    playerRecord.score
                                                                ), // Testo che mostra i punti del giocatore
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }

                                                    val validHistory = record.allPlayers.any {
                                                        (it.scoreHistory ?: emptyList()).size > 1
                                                    }
                                                    if (validHistory) {
                                                        Spacer(modifier = Modifier.height(16.dp))

                                                        // ==========================================================
                                                        // AREA DI INTERAZIONE (HITBOX) AMPLIATA
                                                        // Avvolgiamo Intestazione e Grafico in una singola Column.
                                                        // Spostando il modifier 'combinedClickable' qui sopra,
                                                        // l'utente potrà premere SIA sulla scritta, SIA sull'icona,
                                                        // SIA sul grafico per aprire i dettagli della partita!
                                                        // ==========================================================
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                // Aggiungiamo un leggero bordo invisibile/padding
                                                                // per rendere l'area tattile ancora più comoda
                                                                .padding(vertical = 4.dp)
                                                                .combinedClickable(
                                                                    onClick = {
                                                                        haptic.performHapticFeedback(
                                                                            HapticFeedbackType.LongPress
                                                                        )
                                                                        expandedMatchIndex =
                                                                            viewModel.history.indexOf(
                                                                                record
                                                                            )
                                                                    },
                                                                    onLongClick = {
                                                                        haptic.performHapticFeedback(
                                                                            HapticFeedbackType.LongPress
                                                                        )
                                                                        expandedMatchIndex =
                                                                            viewModel.history.indexOf(
                                                                                record
                                                                            )
                                                                    }
                                                                )
                                                        ) {
                                                            // 1. INTESTAZIONE (Titolo + Icona Espandi)
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    stringResource(R.string.titolo_andamento_punteggi), // Intestazione sezione grafico punti
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Icon(
                                                                    Icons.Filled.Fullscreen,
                                                                    contentDescription = stringResource(
                                                                        R.string.desc_espandi_grafico
                                                                    ), // Descrizione accessibilità per espandere il grafico
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }

                                                            // 2. IL MINI-GRAFICO (Ora privato del clickable, che è gestito dal Padre)
                                                            ScoreChart(
                                                                players = record.allPlayers,
                                                                modifier = Modifier
                                                                    .height(120.dp)
                                                                    .fillMaxWidth()
                                                                    .padding(top = 8.dp)
                                                            )
                                                        }

                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(
                                                                top = 16.dp,
                                                                bottom = 8.dp
                                                            )
                                                        )
                                                    }

                                                    // Data della partita e pulsanti di Azione (Condividi / Elimina)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth()
                                                            .padding(top = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // ==========================================================
                                                        // 1. BLOCCO SINISTRO: ORA E DATA (Raggruppati in una Row)
                                                        // ==========================================================
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            // MODIFICATORE CHIAVE: weight(1f) protegge le icone a destra
                                                            // limitando l'espansione di questo blocco di testo.
                                                            modifier = Modifier.weight(1f)
                                                                .padding(end = 12.dp)
                                                        ) {
                                                            // ==========================================================
                                                            // 1. BLOCCO SINISTRO: UNICO TESTO PER ORA E DATA
                                                            // ==========================================================
                                                            // COMPONENTE CUSTOM: AutoResizedText
                                                            AutoResizedText(
                                                                // PROPRIETÀ text: Usiamo l'interpolazione ${} per eseguire entrambe le funzioni
                                                                // (formattazione dell'ora e formattazione della data) dentro la stessa stringa.
                                                                text = stringResource(
                                                                    R.string.label_data_ora_partita, // Stringa formattata per data e ora della partita
                                                                    timeFormatter.format(
                                                                        java.util.Date(
                                                                            record.timestamp
                                                                        )
                                                                    ),
                                                                    if (record.timestamp > 0L) formatDate(
                                                                        record.timestamp
                                                                    ) else ""
                                                                ),

                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,

                                                                // MODIFICATORE: Essendo l'unico elemento a sinistra, gli diamo il weight(1f)
                                                                // per occupare tutto lo spazio libero, e un padding per tenerlo staccato dalle icone.
                                                                modifier = Modifier.weight(1f)
                                                                    .padding(end = 12.dp)
                                                            )
                                                        }

                                                        // ==========================================================
                                                        // 2. BLOCCO DESTRO: ICONE CONDIVIDI ED ELIMINA
                                                        // ==========================================================
                                                        // Essendo senza "weight", questa Row prende solo i pixel
                                                        // strettamente necessari per disegnare le due icone affiancate.
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            // PROPRIETÀ: distanzia leggermente le due icone tra di loro
                                                            // per non farle sembrare un unico blocco.
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                4.dp
                                                            )
                                                        ) {

                                                            // Logica di Condivisione (Intent) delegata all'Utility
                                                            IconButton(onClick = {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.LongPress
                                                                )

                                                                // ====================================================================
                                                                // DATA MAPPING DA RECORD STORICO
                                                                // ====================================================================
                                                                // Mappiamo la lista dei giocatori estraendo solo Nome e Punteggio
                                                                val playersData =
                                                                    record.allPlayers.map {
                                                                        Pair(
                                                                            it.name,
                                                                            it.score
                                                                        )
                                                                    }

                                                                // ====================================================================
                                                                // Invece di cercare variabili esterne inaccessibili, chiediamo
                                                                // all'oggetto 'record' di calcolare i premi in questo esatto millisecondo.
                                                                // Essendo dentro 'onClick', questo calcolo avviene SOLO se l'utente
                                                                // preme il bottone. Zero spreco di RAM quando l'utente scorre la lista!
                                                                // ====================================================================

                                                                // 1. Chiamiamo la funzione di calcolo 'record.getHistoricalCecchino()'
                                                                // 2. Se restituisce un dato, '?let' lo "spacchetta"
                                                                // 3. Creiamo la nostra Pair universale isolando il nome (it.first.name) e i punti (it.second)
                                                                val mappedCecchino =
                                                                    record.getHistoricalCecchino()
                                                                        ?.let {
                                                                            Pair(
                                                                                it.first.name,
                                                                                it.second
                                                                            )
                                                                        }
                                                                val mappedInarrestabile =
                                                                    record.getHistoricalInarrestabile()
                                                                        ?.let {
                                                                            Pair(
                                                                                it.first.name,
                                                                                it.second
                                                                            )
                                                                        }
                                                                val mappedGambero =
                                                                    record.getHistoricalGambero()
                                                                        ?.let {
                                                                            Pair(
                                                                                it.first.name,
                                                                                it.second
                                                                            )
                                                                        }
                                                                val mappedFenice =
                                                                    record.getHistoricalFenice()
                                                                        ?.let {
                                                                            Pair(
                                                                                it.first.name,
                                                                                it.second
                                                                            )
                                                                        }

                                                                // Costruzione delegata del report testuale chiamando il file SharedUtils
                                                                val shareText = buildMatchShareText(
                                                                    // Passiamo il Context per consentire alla funzione non-Composable di accedere alle risorse di sistema e ai file strings.xml
                                                                    context = context,
                                                                    title = record.title,
                                                                    durationSeconds = record.durationSeconds,
                                                                    timestamp = record.timestamp,
                                                                    rankedPlayersData = playersData,
                                                                    cecchinoData = mappedCecchino, // Passiamo i dati appena calcolati!
                                                                    inarrestabileData = mappedInarrestabile,
                                                                    gamberoData = mappedGambero,
                                                                    feniceData = mappedFenice
                                                                )

                                                                // Esecuzione dell'Intent per aprire WhatsApp/Telegram/ecc.
                                                                launchShareIntent(
                                                                    context,
                                                                    shareText
                                                                )
                                                            }) {
                                                                Icon(
                                                                    Icons.Filled.Share,
                                                                    null,
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            }

                                                            // Logica di Eliminazione con possibilità di annullamento (Undo)
                                                            IconButton(onClick = {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.LongPress
                                                                )
                                                                val removedIndex =
                                                                    viewModel.history.indexOf(record)
                                                                viewModel.deleteMatch(record)

                                                                coroutineScope.launch {
                                                                    launch { delay(2500L); snackbarHostState.currentSnackbarData?.dismiss() }
                                                                    val result =
                                                                        snackbarHostState.showSnackbar(
                                                                            context.getString(R.string.msg_partita_eliminata), // Messaggio Snackbar: partita eliminata
                                                                            context.getString(R.string.btn_annulla_undo), // Testo pulsante Snackbar: annulla eliminazione
                                                                            duration = SnackbarDuration.Indefinite
                                                                        )
                                                                    if (result == SnackbarResult.ActionPerformed) {
                                                                        viewModel.restoreMatch(
                                                                            removedIndex,
                                                                            record
                                                                        )
                                                                    }
                                                                }
                                                            }) {
                                                                Icon(
                                                                    Icons.Filled.Delete,
                                                                    null,
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // --- LEZIONE: COPYRIGHT NEL FLUSSO SCORREVOLE ---
                                // Inserendo il copyright come 'item' finale della LazyColumn,
                                // beneficerà automaticamente del 'contentPadding' che abbiamo impostato sopra.
                                // Non serve più forzare un padding enorme dal basso, si posizionerà da solo
                                // in modo perfetto sotto all'ultima card e sopra all'ingombro del FAB.
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.testo_copyright), // Testo del copyright a fondo pagina
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        ),
                                        // Abbiamo rimosso padding(bottom = 80.dp), mettiamo solo 24.dp per staccarlo dall'ultima card
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // ============================================================================
        // LAYER MODALE: OVERLAY DEL GRAFICO DETTAGLIATO (ESTRATTO)
        // ============================================================================
        // LEZIONE Z-INDEX: In Jetpack Compose, l'ordine in cui scrivi il codice determina
        // l'ordine in cui gli elementi vengono impilati l'uno sull'altro (Asse Z).
        // --------------------------------------------------------------------
        // COMPONENTE: AnimatedVisibility (Gestore delle Transizioni di Stato)
        // --------------------------------------------------------------------
        // Questo Composable osserva un booleano e gestisce l'aggiunta/rimozione del contenuto
        // dal grafo della UI. A differenza di un 'if' standard, mantiene il componente
        // in memoria durante tutta la durata dell'animazione di uscita (ExitTransition).
        AnimatedVisibility(
            // TRIGGER DI STATO: Valuta la nullabilità dell'oggetto.
            // Se 'expandedMatch' contiene un'istanza, 'visible' diventa true.
            // Compose attiva una "Recomposition" e innesca la 'EnterTransition'.
            visible = expandedMatch != null,

            // --------------------------------------------------------------------
            // FASE DI ENTRATA (EnterTransition)
            // --------------------------------------------------------------------
            // Combiniamo due trasformazioni distinte tramite l'operatore '+'.
            enter = fadeIn(
                // Tween (Interpolazione Temporale): Definisce una durata fissa di 150ms.
                animationSpec = tween(200)
            ) + slideInVertically(
                // OFFSET INIZIALE: Determina la coordinata Y di partenza.
                // 'it' rappresenta l'altezza totale (in pixel) del contenuto dell'overlay.
                // Dividendo per 10, il componente non parte dal fondo dello schermo,
                // ma "slitta" verso l'alto solo per l'ultimo 10% della sua altezza,
                // creando un effetto di comparsa più elegante e meno invasivo.
                initialOffsetY = { it / 10 },

                // EASING (Curva di Velocità): FastOutSlowInEasing.
                // Utilizza una curva di Bezier cubica (0.4, 0.0, 0.2, 1.0).
                // L'animazione parte velocemente e decelera verso la fine,
                // simulando il comportamento fisico di un oggetto che si ferma.
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),

            // --------------------------------------------------------------------
            // FASE DI USCITA (ExitTransition)
            // --------------------------------------------------------------------
            exit = fadeOut(
                // Aumentiamo la durata a 200ms per rendere la sparizione meno brusca.
                animationSpec = tween(200)
            ) + slideOutVertically(
                // TARGET OFFSET: Punto di arrivo della coordinata Y durante l'uscita.
                // Muove il componente verso il basso del 10% della sua altezza prima di rimuoverlo.
                targetOffsetY = { it / 10 },

                // EASING: FastOutLinearInEasing.
                // Parte velocemente e mantiene un'accelerazione costante fino alla scomparsa.
                // Ideale per componenti che lasciano lo schermo, poiché suggerisce
                // che l'oggetto stia acquisendo slancio per uscire dal campo visivo.
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            )
        ) {
            lastMatch?.let { match ->
                // Invochiamo il nostro nuovo file esterno, passandogli i dati e
                // dicendogli cosa fare quando l'utente preme il tasto "Chiudi".
                MatchAnalysisOverlay(
                    match = match,
                    onClose = {
                        expandedMatchIndex =
                            -1 // 🧠 STATE: Cambiando l'indice a -1, la Home nasconde l'overlay.
                    }
                )
            }
        }
    }

