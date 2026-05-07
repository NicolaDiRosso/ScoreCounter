package com.n380.scorecounter.ui.screens

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
import com.n380.scorecounter.model.MatchRecord
import com.n380.scorecounter.model.getHistoricalCecchino
import com.n380.scorecounter.model.getHistoricalFenice
import com.n380.scorecounter.model.getHistoricalGambero
import com.n380.scorecounter.model.getHistoricalInarrestabile
import com.n380.scorecounter.ui.components.AwardCard
import com.n380.scorecounter.ui.components.PatternedBackground
import com.n380.scorecounter.ui.components.ScoreChart
import com.n380.scorecounter.ui.components.formatDate
import com.n380.scorecounter.ui.components.formatTime
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

    // --------------------------------------------------------------------
    // GESTIONE DELLO STATO (STATE MANAGEMENT)
    // --------------------------------------------------------------------
    // mutableIntStateOf crea una variabile "osservabile". Quando il suo valore cambia,
    // Compose ricalcola (ricompone) automaticamente solo le parti di UI che la stanno leggendo.
    // rememberSaveable fa in modo che il dato sopravviva non solo alle ricomposizioni,
    // ma anche ai cambi di configurazione del sistema (come la rotazione del display).
    var expandedMatchIndex by rememberSaveable { mutableIntStateOf(-1) }

    // Stato per la visibilità del dialogo informativo sui premi nella schermata di analisi.
    var showAwardsInfoDialog by rememberSaveable { mutableStateOf(false) }


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
                        border = BorderStroke(
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
                if (viewModel.history.isEmpty()) {
                    // STATO VUOTO (Empty State)
                    // Usiamo un Box per centrare perfettamente la scritta in mezzo al tavolo gigante
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nessuna sfida salvata al momento.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
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
                            top = 16.dp, // <--- Stacca la prima card dal bordo superiore del tavolo
                            start = 12.dp, // Leggermente ridotto perché ci pensa già il padding esterno della Card
                            end = 12.dp,
                            bottom = 16.dp
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp) // Forziamo una stondatura morbida ed evidente
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
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (record.timestamp > 0L) formatDate(
                                                        record.timestamp
                                                    ) else "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row {
                                                    // Logica di Condivisione (Intent)
                                                    IconButton(onClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )

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

                    // LIVELLO 1 (Contenuto): La struttura a colonna che separa area dati e dock comandi.
                    Column(modifier = Modifier.fillMaxSize()) {

                        // AREA DATI: Contiene Header e Tavolo.
                        Column(
                            modifier = Modifier
                                .weight(1f) // Prende tutto lo spazio tranne il dock inferiore
                                // 🧠 LEZIONE SPAZIATURA: Usiamo 'statusBarsPadding' invece di 'systemBarsPadding'.
                                // 'systemBars' aggiungerebbe spazio anche in basso (barra navigazione),
                                // raddoppiando il vuoto dato che il Dock ha già il suo 'navigationBarsPadding'.
                                .statusBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        ) {
                            // ====================================================================
                            // --- INTESTAZIONE OVERLAY ---
                            // 🧠 UX & MATERIAL 3 (Gerarchia Visiva e Colori):
                            // 1. "Analisi Partita" torna a essere il titolo principale (displaySmall, primary).
                            // 2. Il nome della sfida diventa un sottotitolo ordinato (titleLarge, onSurface).
                            // 3. Rimuoviamo l'effetto grigio (alpha) dal vincitore, dandogli un colore
                            //    'secondary' per farlo risaltare in modo vibrante ed elegante.
                            // ====================================================================
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp), // Diamo più respiro prima del Tavolo
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Analisi Partita",
                                    style = MaterialTheme.typography.displaySmall, // <-- Tornato gigante!
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = match.title,
                                    style = MaterialTheme.typography.titleLarge, // <-- Grandezza equilibrata
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "🏆 Vinta da ${match.winnerName} con ${match.winningScore} pt",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary, // <-- Niente più grigio! Colore d'accento
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // ====================================================================
                            // ---> IL TAVOLO (Struttura verticale identica ai Risultati) <---
                            // ====================================================================
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    // 🧠 FIX GEOMETRICO: Aggiungiamo 'padding(bottom = 16.dp)'.
                                    // In questo modo, la distanza tra la fine del tavolo grigio e l'inizio del dock bianco
                                    // è di esattamente 16.dp, rispecchiando perfettamente il layout della Homepage
                                    // dove il tavolo è distanziato dal dock principale della stessa misura.
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

                                        // ====================================================================
                                        // 🧠 UX: Intestazione con Icona Informativa
                                        // Usiamo una Row per allineare perfettamente al centro l'icona e il titolo.
                                        // Icons.Outlined.Info è molto elegante e non appesantisce la UI.
                                        // ====================================================================
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // Pulsante icona che inverte la variabile di stato per aprire il popup
                                            IconButton(
                                                onClick = { showAwardsInfoDialog = true },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Info,//icona delle info piena
                                                    contentDescription = "Info Premi",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(end = 8.dp) // Spazietto per staccare l'icona dal testo

                                                )
                                            }

                                            Text(
                                                text = "Premi Partita",
                                                // Usiamo lo stesso stile tipografico di "Andamento Partita" per mantenere coerenza visiva
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // =========================================================
                                        // POPUP INFORMATIVO SUI PREMI (AlertDialog)
                                        // =========================================================
                                        // 🧠 COMPOSE STATE: Questo blocco reagisce alla variabile 'showAwardsInfoDialog'.
                                        if (showAwardsInfoDialog) {
                                            AlertDialog(
                                                // onDismissRequest scatta se l'utente tocca fuori dal popup o preme "Indietro" sul telefono
                                                onDismissRequest = { showAwardsInfoDialog = false },
                                                title = {
                                                    Text(
                                                        "Guida ai Premi",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                text = {
                                                    // verticalScroll permette di scorrere il testo col dito se lo schermo del telefono è troppo piccolo
                                                    Column(
                                                        modifier = Modifier.verticalScroll(
                                                            rememberScrollState()
                                                        )
                                                    ) {

                                                        Text(
                                                            "🎯 Il Cecchino",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(
                                                                bottom = 2.dp,
                                                                top = 8.dp
                                                            )
                                                        )
                                                        Text(
                                                            "Assegnato a chi effettua il singolo salto positivo di punti più alto in un colpo solo.",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )

                                                        Text(
                                                            "🔥 L'Inarrestabile",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(
                                                                bottom = 2.dp,
                                                                top = 16.dp
                                                            )
                                                        )
                                                        Text(
                                                            "Assegnato a chi innesca più volte la combo consecutiva 'On Fire'.",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )

                                                        Text(
                                                            "🦞 Il Gambero",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(
                                                                bottom = 2.dp,
                                                                top = 16.dp
                                                            )
                                                        )
                                                        Text(
                                                            "Assegnato al giocatore che accumula la maggior quantità di punti negativi totali nella partita.",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )

                                                        Text(
                                                            "🦅 La Fenice",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(
                                                                bottom = 2.dp,
                                                                top = 16.dp
                                                            )
                                                        )
                                                        Text(
                                                            "Assegnato a chi compie la rimonta più epica, calcolata tra il suo punto più basso e il punteggio finale.",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                },
                                                confirmButton = {
                                                    // Pulsante pieno (Button) al posto del TextButton, con la nostra stondatura ufficiale a 20.dp
                                                    Button(
                                                        onClick = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            showAwardsInfoDialog =
                                                                false // Chiude il popup quando si preme il bottone
                                                        },
                                                        shape = RoundedCornerShape(20.dp)
                                                    ) {
                                                        Text("Ho capito")
                                                    }
                                                }
                                            )
                                        }

                                        // ====================================================================
                                        // 🧠 REFACTORING: UTILIZZO DEL COMPONENTE 'AwardCard'
                                        // Grazie al nostro nuovo mattoncino, abbiamo eliminato centinaia di righe
                                        // di codice duplicato. Passiamo solo i dati grezzi, e la grafica si autogenera!
                                        // ====================================================================

                                        // --- CARD PREMIO: CECCHINO 🎯 ---
                                        storiciCecchino?.let { (player, punti) ->
                                            AwardCard(
                                                icon = "🎯",
                                                title = "Il Cecchino",
                                                playerName = player.name,
                                                playerColorInt = player.color,
                                                valueText = "+$punti pt"
                                            )
                                        }

                                        // --- CARD PREMIO: INARRESTABILE 🔥 ---
                                        storiciInarrestabile?.let { (player, combo) ->
                                            AwardCard(
                                                icon = "🔥",
                                                title = "L'Inarrestabile",
                                                playerName = player.name,
                                                playerColorInt = player.color,
                                                valueText = "$combo Combo"
                                            )
                                        }

                                        // --- CARD PREMIO: IL GAMBERO 🦞 ---
                                        storiciGambero?.let { (player, punti) ->
                                            AwardCard(
                                                icon = "🦞",
                                                title = "Il Gambero",
                                                playerName = player.name,
                                                playerColorInt = player.color,
                                                valueText = "-$punti pt"
                                            )
                                        }

                                        // --- CARD PREMIO: LA FENICE 🦅 ---
                                        storiciFenice?.let { (player, punti) ->
                                            AwardCard(
                                                icon = "🦅",
                                                title = "La Fenice",
                                                playerName = player.name,
                                                playerColorInt = player.color,
                                                valueText = "+$punti pt"
                                            )
                                        }
                                    }

                                    /// ====================================================================
                                    // Footer informativo sulla durata
                                    // 🧠 UX & MATERIAL 3: Coerenza dei Colori. L'utente ha giustamente
                                    // notato che il grigio "spegne" questa informazione. Usiamo il colore
                                    // 'primary' per legarlo visivamente al bottone di chiusura sottostante!
                                    // ====================================================================
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.Center, // Bello centrato
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Timer,
                                            contentDescription = "Durata",
                                            tint = MaterialTheme.colorScheme.primary, // <-- Niente più grigio
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = " Durata totale: ${formatTime(match.durationSeconds)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary, // <-- Niente più grigio
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ====================================================================
                        // ---> NUOVO DOCK INFERIORE (Uguale alla Home e Stats) <---
                        // ====================================================================
                        Surface(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            // Arrotondamento solo in alto per incollarlo al fondo dello schermo
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding() // Rispetta lo spazio della barra di navigazione Android
                                    .padding(horizontal = 16.dp, vertical = 16.dp) // Spaziatura interna per il bottone // Spaziatura interna per il bottone
                            ) {
                                // --------------------------------------------------------------------
                                // PULSANTE CHIUDI ANALISI (Stile Nuova Sfida)
                                // --------------------------------------------------------------------
                                Button(
                                    onClick = {
                                        // Aggiunta la vibrazione per coerenza tattile
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expandedMatchIndex = -1 // 🧠 STATE: Cambiando l'indice a -1, l'overlay scompare.
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp), // Manteniamo l'altezza massiccia (72dp) richiesta
                                    shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Chiudi",
                                        modifier = Modifier.padding(end = 8.dp).size(28.dp) // Icona maggiorata
                                    )
                                    Text(
                                        text = "Chiudi Analisi",
                                        // Tipografia imponente (Headline) per richiamare la Home
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
