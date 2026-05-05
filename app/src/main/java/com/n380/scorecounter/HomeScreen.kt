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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val context =
        LocalContext.current // Il contesto Android base, necessario per lanciare Intent (es. condivisione).

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
    val expandedMatch =
        if (expandedMatchIndex >= 0) viewModel.history.getOrNull(expandedMatchIndex) else null

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
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToCreate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp), // Altezza Expressive massiccia (72dp)
                        shape = RoundedCornerShape(20.dp), // Stondatura interna del bottone (20dp)
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Nuova Sfida",
                            modifier = Modifier.padding(end = 8.dp).size(28.dp)
                        )
                        Text(
                            text = "Nuova Sfida",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
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
                // STATO VUOTO (Empty State)
                Text(
                    text = "Nessuna sfida salvata al momento.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                // --------------------------------------------------------------------
                // LAZYCOLUMN E IL SEGRETO DEL "CONTENT PADDING"
                // --------------------------------------------------------------------
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
                        start = 16.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 20.dp//aggiungiamo un padding di 96 per superare il bottone
                    )
                ) {
                    items(viewModel.history) { record ->
                        // Variabile di stato locale per gestire l'apertura/chiusura della singola card
                        var expanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expanded = !expanded
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {

                                // --- PARTE SEMPRE VISIBILE DELLA CARD ---
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

                              // --- PARTE ESPANDIBILE (DETTAGLI E GRAFICO) ---
                                AnimatedVisibility(visible = expanded) {
                                    Column(modifier = Modifier.padding(top = 20.dp)) {
                                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

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

                                        val validHistory = record.allPlayers.any {
                                            (it.scoreHistory ?: emptyList()).size > 1
                                        }
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

                                            // Mini-grafico vettoriale
                                            ScoreChart(
                                                players = record.allPlayers,
                                                modifier = Modifier
                                                    .height(120.dp)
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                                    // Gestione dei tap lunghi e corti per aprire l'overlay a schermo intero
                                                    .combinedClickable(
                                                        onClick = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            expandedMatchIndex =
                                                                viewModel.history.indexOf(record)
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            expandedMatchIndex =
                                                                viewModel.history.indexOf(record)
                                                        }
                                                    )
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier.padding(
                                                    top = 16.dp,
                                                    bottom = 8.dp
                                                )
                                            )
                                        }

                                        // Data della partita e pulsanti di Azione (Condividi / Elimina)
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
                                                // Logica di Condivisione (Intent)
                                                IconButton(onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                                    var shareText =
                                                        "🏆 Risultati Storici: ${record.title}\n"
                                                    if (record.timestamp > 0L) shareText += "📅 Data: ${
                                                        formatDate(
                                                            record.timestamp
                                                        )
                                                    }\n"
                                                    if (record.durationSeconds > 0) shareText += "⏱️ Durata: ${
                                                        formatTime(
                                                            record.durationSeconds
                                                        )
                                                    }\n\n"
                                                    record.allPlayers.forEachIndexed { index, player ->
                                                        val medal = when (index) {
                                                            0 -> "🥇 1°"; 1 -> "🥈 2°"; 2 -> "🥉 3°"; else -> "${index + 1}°"
                                                        }
                                                        shareText += "$medal ${player.name} - ${player.score} pt\n"
                                                    }
                                                    shareText += "\nGenerato con ScoreCounter 🎮\n© 2026 Creato da Nicola"

                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(
                                                        Intent.createChooser(
                                                            sendIntent,
                                                            "Condividi partita"
                                                        )
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
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val removedIndex =
                                                        viewModel.history.indexOf(record)
                                                    viewModel.deleteMatch(record)

                                                    coroutineScope.launch {
                                                        launch { delay(2500L); snackbarHostState.currentSnackbarData?.dismiss() }
                                                        val result = snackbarHostState.showSnackbar(
                                                            "Partita eliminata",
                                                            "ANNULLA",
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
                            text = "© 2026 Creato da NicolA380✈️\nTutti i diritti sono riservati",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            // Abbiamo rimosso padding(bottom = 80.dp), mettiamo solo 24.dp per staccarlo dall'ultima card
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    // --------------------------------------------------------------------
    // LAYER MODALE: OVERLAY DEL GRAFICO DETTAGLIATO
    // --------------------------------------------------------------------
    // LEZIONE Z-INDEX: In Jetpack Compose, l'ordine in cui scrivi il codice determina
    // l'ordine in cui gli elementi vengono impilati l'uno sull'altro (Asse Z).
    // Inserendo questa AnimatedVisibility alla FINE della funzione HomeScreen (fuori dallo Scaffold),
    // ci assicuriamo che quando appare venga disegnata *SOPRA* a tutto il resto (lista, bottoni, topbar).
    AnimatedVisibility(
        // Il "Grilletto": l'animazione parte solo quando expandedMatch ha un valore (non è null)
        visible = expandedMatch != null,
        // Animazione di entrata: Svanimento (fadeIn) + scivolamento dal basso verso l'alto (slideInVertically)
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 10 },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        // Animazione di uscita: Svanimento veloce + scivolamento verso il basso
        exit = fadeOut(tween(200)) + slideOutVertically(
            targetOffsetY = { it / 10 },
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        )
    ) {
        //-------------------------------------------------------------------------------------------------------------------------------------------
        //AGGIUNGIAMO LA SEZIONE DEI PREMI MA PRIMA DI QUESTO IMPOSTAIMO LOL SFONDO CON LE ICONE
        lastMatch?.let { match ->
            // Usiamo lastMatch (la memoria fantasma) per assicurarci che i dati non spariscano
            // improvvisamente mentre l'animazione di uscita sta ancora finendo di scivolare via.
            Surface(
                modifier = Modifier.fillMaxSize(),
                // Riportiamo il colore a solido (senza alpha) perché lo sfondo a icone
                // riempirà visivamente lo spazio.
                color = MaterialTheme.colorScheme.background
            ) {
                // 🧠 LEZIONE Z-INDEX: Il Box ci permette di sovrapporre i livelli.
                Box(modifier = Modifier.fillMaxSize()) {

                    // LIVELLO 0 (Sfondo): Dipingiamo la griglia di icone dinamiche
                    // per coerenza con la Home e la ResultsScreen.
                    PatternedBackground()

                    // LIVELLO 1 (Contenuto): La colonna con tutte le informazioni.
                    Column(
                        // systemBarsPadding() è vitale in un overlay a schermo intero: impedisce
                        // che i nostri testi finiscano sotto l'orologio di Android in alto.
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(16.dp)
                    ) {
                        // --- INTESTAZIONE OVERLAY ---
                        Text(
                            text = "Analisi Partita",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Sfida: ${match.title}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = "Vinta da ${match.winnerName} con ${match.winningScore} pt",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // ====================================================================
                        // ---> IL TAVOLO (Struttura verticale identica ai Risultati) <---
                        // ====================================================================
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // 🧠 COMPOSE: Il weight(1f) spinge tutto ciò che segue verso il basso.
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            // 🧠 RECOMPOSITION: Usiamo Column + verticalScroll invece di LazyColumn.
                            // Forziamo il caricamento immediato di tutti gli elementi per avere animazioni fluide.
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()) // Rende il contenuto del Tavolo scorrevole.
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp) // Spazio automatico tra i figli.
                            ) {
                                // --- IL GRAFICO ESPANSO ---
                                Text(
                                    "Andamento Punteggi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(350.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    ScoreChart(
                                        players = match.allPlayers,
                                        isDetailed = true, // Attiva la griglia e le etichette nel Canvas.
                                        modifier = Modifier.fillMaxSize().padding(12.dp)
                                    )
                                }

                                // --- AREA PREMI (Retrocompatibile) ---
                                // 🧠 KOTLIN EXTENSION: Usiamo i metodi creati nel file Models per calcolare i dati.
                                // 'remember(match)' assicura che il calcolo avvenga solo quando cambia la partita selezionata.
                                val storiciCecchino =
                                    remember(match) { match.getHistoricalCecchino() }
                                val storiciInarrestabile =
                                    remember(match) { match.getHistoricalInarrestabile() }
                                val storiciGambero =
                                    remember(match) { match.getHistoricalGambero() }
                                val storiciFenice = remember(match) { match.getHistoricalFenice() }

                                // 🧠 LOGICA CONDIZIONALE: Se tutti i calcoli sono 'null' (partite vecchie), il blocco sparisce.
                                if (storiciCecchino != null || storiciInarrestabile != null || storiciGambero != null || storiciFenice != null) {

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Premi Partita",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // --- CARD PREMIO: CECCHINO 🎯 ---
                                    storiciCecchino?.let { (player, punti) ->
                                        val playerColor = Color(player.color)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            // 🎨 DESIGN: Alpha 0.15f crea uno sfondo tenue basato sul colore del giocatore.
                                            colors = CardDefaults.cardColors(
                                                containerColor = playerColor.copy(
                                                    alpha = 0.15f
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                playerColor.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "🎯",
                                                    style = MaterialTheme.typography.displaySmall,
                                                    modifier = Modifier.padding(end = 16.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "Il Cecchino",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = playerColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        player.name,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = playerColor),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        "+$punti pt",
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 6.dp
                                                        ),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // --- CARD PREMIO: INARRESTABILE 🔥 ---
                                    storiciInarrestabile?.let { (player, combo) ->
                                        val playerColor = Color(player.color)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = playerColor.copy(
                                                    alpha = 0.15f
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                playerColor.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "🔥",
                                                    style = MaterialTheme.typography.displaySmall,
                                                    modifier = Modifier.padding(end = 16.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "L'Inarrestabile",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = playerColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        player.name,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = playerColor),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        "$combo Combo",
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 6.dp
                                                        ),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // --- CARD PREMIO: IL GAMBERO 🦞 ---
                                    storiciGambero?.let { (player, punti) ->
                                        val playerColor = Color(player.color)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = playerColor.copy(
                                                    alpha = 0.15f
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                playerColor.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "🦞",
                                                    style = MaterialTheme.typography.displaySmall,
                                                    modifier = Modifier.padding(end = 16.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "Il Gambero",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = playerColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        player.name,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = playerColor),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        "-$punti pt",
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 6.dp
                                                        ),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // --- CARD PREMIO: LA FENICE 🦅 ---
                                    storiciFenice?.let { (player, punti) ->
                                        val playerColor = Color(player.color)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = playerColor.copy(
                                                    alpha = 0.15f
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                playerColor.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "🦅",
                                                    style = MaterialTheme.typography.displaySmall,
                                                    modifier = Modifier.padding(end = 16.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "La Fenice",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = playerColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        player.name,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = playerColor),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        "+$punti pt",
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 6.dp
                                                        ),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Footer informativo sulla durata
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Timer,
                                        null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = " Durata totale: ${formatTime(match.durationSeconds)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }

                        // --------------------------------------------------------------------
                        // PULSANTE CHIUDI ANALISI (Versione Full Width)
                        // --------------------------------------------------------------------
                        ExtendedFloatingActionButton(
                            onClick = {
                                // Aggiunta la vibrazione per coerenza tattile
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expandedMatchIndex = -1 // 🧠 STATE: Cambiando l'indice a -1, l'overlay scompare.
                            },
                            modifier = Modifier
                                .fillMaxWidth() // 🧠 UI: Rimosso lo 0.8f per occupare tutto lo spazio disponibile
                                .height(72.dp)  // Altezza Expressive massiccia (72dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Chiudi",
                                    modifier = Modifier.size(28.dp) // Icona maggiorata
                                )
                            },
                            text = {
                                Text(
                                    text = "Chiudi Analisi",
                                    // Tipografia imponente (Headline) per richiamare la Home
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
