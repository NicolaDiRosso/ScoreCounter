package com.n380.scorecounter

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current // Il contesto Android base, necessario per lanciare Intent (es. condivisione).

    // SnackbarHostState gestisce la coda dei messaggi a comparsa (Snackbar).
    // remember fa sì che l'oggetto non venga ricreato a ogni ricomposizione della UI.
    val snackbarHostState = remember { SnackbarHostState() }

    // rememberCoroutineScope apre un canale sicuro per lanciare processi asincroni (background)
    // legati al ciclo di vita di questa schermata. Se la schermata muore, i processi si fermano.
    val coroutineScope = rememberCoroutineScope()

    // --------------------------------------------------------------------
    // GESTIONE DELLO STATO (STATE MANAGEMENT)
    // --------------------------------------------------------------------
    // mutableIntStateOf crea una variabile "osservabile". Quando il suo valore cambia,
    // Compose ricalcola (ricompone) automaticamente solo le parti di UI che la stanno leggendo.
    // rememberSaveable fa in modo che il dato sopravviva non solo alle ricomposizioni,
    // ma anche ai cambi di configurazione del sistema (come la rotazione del display).
    var expandedMatchIndex by rememberSaveable { mutableIntStateOf(-1) }

    // Valutazione reattiva: Se l'indice è valido (>= 0), recuperiamo i dati della partita dal ViewModel.
    val expandedMatch = if (expandedMatchIndex >= 0) viewModel.history.getOrNull(expandedMatchIndex) else null

    // Variabile di cache (Memoria Fantasma). Usa solo 'remember' perché deve sopravvivere solo
    // per pochi millisecondi durante l'animazione di uscita, non serve salvarla nel disco.
    var lastMatch by remember { mutableStateOf<MatchRecord?>(null) }

    // Sincronizzazione: ogni volta che expandedMatch riceve una partita, aggiorniamo la cache.
    if (expandedMatch != null) {
        lastMatch = expandedMatch
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
                    contentDescription = "Ripristina salvataggio",
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
                Text(
                    text = "Partita in sospeso",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Hai lasciato una sfida a metà.\nVuoi riprenderla da dove l'avevi lasciata?")
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
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(text = "Cancella")
                    }

                    // AZIONE PRIMARIA / COSTRUTTIVA
                    // Il pulsante Filled rappresenta l'azione suggerita o principale (Happy Path).
                    Button(
                        onClick = {
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
                        Text(
                            text = "Riprendi",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    // --------------------------------------------------------------------
    // SCAFFOLD: L'OSSATURA DELLA SCHERMATA MATERIAL
    // --------------------------------------------------------------------
    // Scaffold implementa la struttura base del Material Design (TopBar, BottomBar, FloatingActionButton, ecc.).
    // Gestisce automaticamente la sovrapposizione degli elementi e calcola i margini di sicurezza.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToCreate()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Nuova Sfida",
                        modifier = Modifier.padding(end = 8.dp).size(24.dp)
                    )
                    Text(
                        text = "Nuova Sfida",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        // innerPadding calcolato dallo Scaffold contiene lo spazio occupato dalle barre di sistema
        // e dalla nostra bottomBar. Applicandolo al Column, garantiamo che la lista non finisca sotto il pulsante.
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                // Arrangement.SpaceBetween posiziona il primo elemento a sinistra e l'ultimo a destra,
                // distribuendo lo spazio vuoto in mezzo.
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storico Sfide",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (viewModel.history.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToStats()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Statistiche Globali",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (viewModel.history.isEmpty()) {
                Text(
                    text = "Nessuna sfida salvata al momento.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // --------------------------------------------------------------------
                // LAZY COLUMN: RENDERING OTTIMIZZATO
                // --------------------------------------------------------------------
                // A differenza di una Column normale che calcola e disegna tutti i suoi figli subito,
                // LazyColumn istanzia (crea) solo gli elementi attualmente visibili a schermo.
                // Quando l'utente scorre, gli elementi che escono dallo schermo vengono distrutti e riciclati
                // per disegnare quelli nuovi, garantendo alte prestazioni anche con migliaia di dati.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // La funzione "items" itera la nostra lista di dati e crea un blocco UI per ciascuno.
                    items(viewModel.history) { record ->
                        // Questo stato è "locale" alla singola iterazione: ogni Card ha la sua variabile
                        // indipendente che controlla se la sua tendina interna è aperta o chiusa.
                        var expanded by remember { mutableStateOf(false) }

                        Card(
                            // Modificatore di interazione base. Il click inverte lo stato booleano locale.
                            modifier = Modifier.fillMaxWidth().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expanded = !expanded
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = record.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (record.durationSeconds > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Timer, null,
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp),
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
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏆 Vincitore: ${record.winnerName}",
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

                                // AnimatedVisibility interno. Se 'expanded' diventa true, il contenuto
                                // all'interno delle sue parentesi graffe entra in scena espandendosi in altezza.
                                AnimatedVisibility(visible = expanded) {
                                    Column(modifier = Modifier.padding(top = 20.dp)) {
                                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                                        record.allPlayers.forEachIndexed { index, playerRecord ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (index == 0) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Filled.EmojiEvents, null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                        Text(
                                                            text = "1° ${playerRecord.name}",
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = "${index + 1}° ${playerRecord.name}",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }

                                                Text(
                                                    text = "${playerRecord.score} pt",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }

                                        val validHistory = record.allPlayers.any { (it.scoreHistory ?: emptyList()).size > 1 }
                                        if (validHistory) {
                                            Spacer(modifier = Modifier.height(16.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Andamento Punteggi",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    Icons.Filled.Fullscreen,
                                                    contentDescription = "Espandi Grafico",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            ScoreChart(
                                                players = record.allPlayers,
                                                modifier = Modifier
                                                    .height(120.dp)
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                                    // Modifier.combinedClickable abilita l'ascolto di interazioni multiple
                                                    // (doppio tocco, tocco prolungato) fornendo callback separati.
                                                    .combinedClickable(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            // Recuperiamo dinamicamente l'indice corrente dell'elemento nel database
                                                            // e lo assegniamo alla variabile di stato root, innescando l'overlay.
                                                            expandedMatchIndex = viewModel.history.indexOf(record)
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            expandedMatchIndex = viewModel.history.indexOf(record)
                                                        }
                                                    )
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (record.timestamp > 0L) formatDate(record.timestamp) else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row {
                                                IconButton(onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                                    var shareText = "🏆 Risultati Storici: ${record.title}\n"
                                                    if (record.timestamp > 0L) shareText += "📅 Data: ${formatDate(record.timestamp)}\n"
                                                    if (record.durationSeconds > 0) shareText += "⏱️ Durata: ${formatTime(record.durationSeconds)}\n\n"
                                                    record.allPlayers.forEachIndexed { index, player ->
                                                        val medal = when (index) {
                                                            0 -> "🥇 1°"; 1 -> "🥈 2°"; 2 -> "🥉 3°"; else -> "${index + 1}°"
                                                        }
                                                        shareText += "$medal ${player.name} - ${player.score} pt\n"
                                                    }
                                                    shareText += "\nGenerato con ScoreCounter 🎮\n© 2026 Creato da Nicola"

                                                    // Intent.ACTION_SEND demanda al sistema operativo la gestione
                                                    // del testo, aprendo il foglio di condivisione nativo (ShareSheet).
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Condividi partita"))
                                                }) {
                                                    Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.primary)
                                                }

                                                IconButton(onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val removedIndex = viewModel.history.indexOf(record)
                                                    viewModel.deleteMatch(record)

                                                    // Esecuzione asincrona (Coroutine)
                                                    // Permette di eseguire operazioni ritardate (delay) senza bloccare il Main Thread.
                                                    coroutineScope.launch {
                                                        launch { delay(2500L); snackbarHostState.currentSnackbarData?.dismiss() }

                                                        // La funzione showSnackbar sospende l'esecuzione di questo blocco finché
                                                        // la snackbar non viene scartata o non viene premuta l'azione associata.
                                                        val result = snackbarHostState.showSnackbar("Partita eliminata", "ANNULLA", duration = SnackbarDuration.Indefinite)

                                                        // Se il risultato è ActionPerformed (l'utente ha premuto "ANNULLA"),
                                                        // si procede al ripristino dell'oggetto rimosso.
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restoreMatch(removedIndex, record)
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "© 2026 Creato da NicolA380✈️\nTutti i diritti sono riservati",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------
    // LAYER MODALE (OVERLAY GRAFICO DETTAGLIATO)
    // --------------------------------------------------------------------
    // Essendo dichiarato alla fine della gerarchia della funzione HomeScreen (dopo lo Scaffold),
    // questo blocco viene disegnato sull'asse Z al di sopra di tutti gli altri elementi (Z-Index implicito).
    AnimatedVisibility(
        visible = expandedMatch != null,
        // Configurazione delle specifiche di animazione (AnimationSpec).
        // Il parametro 'tween' definisce un'interpolazione lineare basata sul tempo (es. 300ms).
        // 'easing' descrive la curva di accelerazione dell'animazione.
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 10 },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(tween(200)) + slideOutVertically(
            targetOffsetY = { it / 10 },
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
        // Accesso sicuro (let) alla cache dei dati per prevenire la corruzione visiva durante l'exit transition
        lastMatch?.let { match ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
            ) {
                // systemBarsPadding() impedisce al contenuto interno di collidere
                // con la StatusBar (in alto) e la NavigationBar (in basso).
                Column(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)
                ) {

                    Text(
                        text = "Analisi Partita",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Text(
                        text = "Sfida: ${match.title}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Vinta da ${match.winnerName} con ${match.winningScore} pt",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        // Invocazione della logica vettoriale nativa tramite il parametro 'isDetailed'
                        ScoreChart(
                            players = match.allPlayers,
                            isDetailed = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, null, tint = Color.LightGray)
                        Text(
                            text = " Durata totale: ${formatTime(match.durationSeconds)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.LightGray
                        )
                    }

                    // Il modificatore weight(1f) impone allo Spacer di espandersi fino ad assorbire
                    // interamente lo spazio rimanente non assegnato nel Column genitore.
                    // Risultato visivo: gli elementi successivi vengono ancorati al limite inferiore del layout.
                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // La modifica dello stato osservato innesca l'Exit Transition definita nell'AnimatedVisibility.
                            expandedMatchIndex = -1
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = "Chiudi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}