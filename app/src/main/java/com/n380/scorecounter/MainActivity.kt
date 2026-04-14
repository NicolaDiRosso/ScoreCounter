    package com.n380.scorecounter

    // ====================================================================
    // 1. AREA IMPORTAZIONI (Tutte le librerie necessarie)
    // ====================================================================
    import android.app.Activity // Serve per controllare la finestra dell'app (es. per tenere lo schermo acceso)
    import android.app.Application
    import android.content.Context
    // Import per inviare dati ad altre app (WhatsApp, Telegram, ecc.)
    import android.content.Intent
    import android.os.Bundle
    import android.view.WindowManager // Serve per impedire allo schermo di spegnersi durante il gioco
    import androidx.activity.ComponentActivity
    // Per intercettare il tasto "Indietro" del telefono (Il Salva-Vita)
    import androidx.activity.compose.BackHandler
    import androidx.activity.compose.setContent
    import androidx.activity.enableEdgeToEdge
    import androidx.compose.animation.AnimatedVisibility
    // Import per l'animazione esplosiva (Motore Grafico dei Coriandoli)
    import androidx.compose.animation.core.Animatable
    import androidx.compose.animation.core.FastOutSlowInEasing
    import androidx.compose.animation.core.tween
    import androidx.compose.foundation.Canvas // La "Tela" su cui disegniamo coriandoli e il grafico delle stats
    import androidx.compose.foundation.clickable
    // Per riconoscere la "Pressione Prolungata" (Long Click per i +10 e -5)
    import androidx.compose.foundation.ExperimentalFoundationApi
    import androidx.compose.foundation.background
    import androidx.compose.foundation.combinedClickable
    import androidx.compose.foundation.horizontalScroll // Per scorrere orizzontalmente la legenda del grafico
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box // La "Scatola" per sovrapporre l'animazione allo schermo
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.heightIn // Per limitare l'altezza del popup dei preferiti
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.lazy.LazyColumn // Lista verticale "intelligente" che scorre
    import androidx.compose.foundation.lazy.LazyRow // Lista orizzontale dei nomi rapidi
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.lazy.itemsIndexed
    import androidx.compose.foundation.rememberScrollState // Ricorda a che punto sei arrivato a scorrere
    // Forme e stili per arrotondare i bottoni
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    // ---> NUOVI IMPORT PER LA TASTIERA NUMERICA <---
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.ui.text.input.KeyboardType
    // Tutte le icone usate nell'app
    import androidx.compose.material.icons.filled.Delete
    import androidx.compose.material.icons.filled.Edit
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material.icons.filled.EmojiEvents
    import androidx.compose.material.icons.filled.ExpandLess // Freccia in su per lo storico
    import androidx.compose.material.icons.filled.ExpandMore // Freccia in giù per lo storico
    import androidx.compose.material.icons.filled.KeyboardArrowDown // Freccia giù per riordino
    import androidx.compose.material.icons.filled.KeyboardArrowUp   // Freccia su per riordino
    import androidx.compose.material.icons.filled.Remove
    import androidx.compose.material.icons.filled.Share // L'icona della condivisione
    import androidx.compose.material.icons.filled.WorkspacePremium // La medaglia/corona del Leader
    import androidx.compose.material.icons.filled.Timer // L'icona del cronometro
    import androidx.compose.material.icons.filled.VideogameAsset
    import androidx.compose.material.icons.filled.Person
    // Componenti grafici Material 3
    import androidx.compose.material3.AlertDialog
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults // Per cambiare colore ai bottoni di avviso (es. rosso)
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.ExtendedFloatingActionButton
    import androidx.compose.material3.HorizontalDivider
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.OutlinedButton
    import androidx.compose.material3.OutlinedTextField
    import androidx.compose.material3.Scaffold
    // Componenti per la Snackbar (Il messaggio temporaneo nero in basso)
    import androidx.compose.material3.SnackbarDuration
    import androidx.compose.material3.SnackbarHost
    import androidx.compose.material3.SnackbarHostState
    import androidx.compose.material3.SnackbarResult
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextButton
    // Stati e ricordi di Compose
    import androidx.compose.runtime.* // Importa tutti gli stati in blocco (remember, getValue, ecc.)
    // Strumenti grafici avanzati
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip // Per tagliare i bordi grafici del bottone a pressione lunga
    import androidx.compose.ui.geometry.Offset // Serve per calcolare le coordinate X e Y sul Canvas
    import androidx.compose.ui.graphics.Color
    // Strumenti per disegnare le linee vettoriali del Grafico delle Stats
    import androidx.compose.ui.graphics.Path
    import androidx.compose.ui.graphics.StrokeCap
    import androidx.compose.ui.graphics.StrokeJoin
    import androidx.compose.ui.graphics.drawscope.Stroke
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign // Per centrare il testo (es. del dado)
    import androidx.compose.ui.unit.dp
    // Feedback tattile (Vibrazione)
    import androidx.compose.ui.hapticfeedback.HapticFeedbackType
    import androidx.compose.ui.platform.LocalHapticFeedback
    import androidx.compose.ui.platform.LocalContext // Serve per ottenere il "Contesto" della pagina (es. per la condivisione)
    // Database e salvataggio
    import androidx.datastore.preferences.core.edit
    import androidx.datastore.preferences.core.stringPreferencesKey
    import androidx.datastore.preferences.preferencesDataStore
    import androidx.lifecycle.AndroidViewModel
    import androidx.lifecycle.viewModelScope
    import androidx.lifecycle.viewmodel.compose.viewModel
    // Navigazione tra le schermate
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.rememberNavController
    // Traduttore JSON
    import com.google.gson.Gson
    import com.google.gson.reflect.TypeToken
    import com.n380.scorecounter.ui.theme.ScoreCounterTheme
    // Strumenti Asincroni (Coroutine)
    import kotlinx.coroutines.Job // Gestisce il processo del timer in background
    import kotlinx.coroutines.delay // Fa aspettare un tempo preciso al timer o alla Snackbar
    import kotlinx.coroutines.flow.first
    import kotlinx.coroutines.launch
    // Strumenti per la data e la matematica
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import kotlin.math.cos // Calcoli trigonometrici per l'esplosione dei coriandoli
    import kotlin.math.sin
    import kotlin.random.Random // Generatore di numeri casuali per i coriandoli e il dado

    // ====================================================================
    // 2. INIZIALIZZAZIONE DEL DATABASE (DataStore) E FUNZIONI DI SUPPORTO
    // ====================================================================
    // LEZIONE: Questa riga crea un "collegamento" globale alla memoria fisica del telefono.
    // Usiamo 'by preferencesDataStore' che gestisce la creazione del file "score_counter_prefs"
    // in modo automatico e sicuro.
    val Context.dataStore by preferencesDataStore(name = "score_counter_prefs")

    // FUNZIONE DI SUPPORTO: Formatta i secondi in "Minuti:Secondi" (Es. 05:12)
    fun formatTime(totalSeconds: Long): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        // padStart aggiunge uno '0' davanti se il numero è a una sola cifra (es. 5 diventa 05)
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    // FUNZIONE DI SUPPORTO: Formatta i millisecondi in una Data (Es. 8 Nov 2025)
    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "" // Salva-vita per le vecchie partite che non avevano la data salvata
        // Creiamo il formattatore usando il formato "Giorno Mese Anno" con il dizionario Italiano
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }

    // ====================================================================
    // 3. MAIN ACTIVITY (Il punto d'ingresso)
    // ====================================================================
    /**
     * MAIN ACTIVITY
     * Pensa a questa classe come al tuo "int main()" in C++.
     * È il punto di ingresso dell'applicazione quando l'utente clicca l'icona sul telefono.
     */
    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Questa funzione permette all'app di disegnare anche sotto la barra della batteria
            // e sotto la barra di navigazione in basso, per un look molto moderno.
            enableEdgeToEdge()

            // setContent è il "ponte" magico. Qui finisce il mondo Android classico
            // ed entriamo nel mondo "dichiarativo" di Jetpack Compose.
            setContent {
                // Questo è il "Tema". Applica in automatico i colori Material 3 (Expressive)
                // basandosi sullo sfondo del telefono dell'utente (Dynamic Colors).
                ScoreCounterTheme {

                    // LEZIONE: Il NavController è il nostro "Vigile Urbano" che gestisce lo stack delle schermate.
                    val navController = rememberNavController()

                    // LEZIONE: Creiamo il ViewModel!
                    // Questa funzione magica crea il ViewModel la prima volta, e poi
                    // continua a restituirci sempre lo stesso se cambiamo schermata.
                    // Essendo un AndroidViewModel, capisce automaticamente che deve passargli l'Application Context.
                    val matchViewModel: MatchViewModel = viewModel()

                    // Definiamo la mappa stradale delle nostre schermate
                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            HomeScreen(
                                viewModel = matchViewModel,
                                onNavigateToCreate = {
                                    // Quando creiamo una nuova sfida, puliamo i vecchi dati (Puliamo la RAM)
                                    matchViewModel.clearMatch()
                                    navController.navigate("create")
                                },
                                onNavigateToCounter = {
                                    // Per riprendere la partita dal salva-vita
                                    navController.navigate("counter")
                                }
                            )
                        }

                        composable("create") {
                            CreateMatchScreen(
                                viewModel = matchViewModel,
                                onNavigateToCounter = { navController.navigate("counter") }
                            )
                        }

                        // --- ROTTA: IL CONTATORE ---
                        composable("counter") {
                            CounterScreen(
                                viewModel = matchViewModel,
                                onNavigateToResults = { navController.navigate("results") },
                                // Passiamo la funzione per tornare indietro in caso di uscita d'emergenza (Salva-Vita)
                                onNavigateHome = {
                                    matchViewModel.clearMatch() // Puliamo la RAM
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
                        }

                        // --- ROTTA: LA CLASSIFICA FINALE ---
                        composable("results") {
                            ResultsScreen(
                                viewModel = matchViewModel,
                                onNavigateHome = {
                                    // LEZIONE: popBackStack("home", false) è fondamentale!
                                    // Chiude la classifica, il contatore e la schermata di creazione,
                                    // togliendole dalla memoria e riportandoci direttamente alla Home pulita!
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ====================================================================
    // 4. LE SCHERMATE DELL'APP (Composables)
    // Nota: In Kotlin, le funzioni grafiche Compose si scrivono sempre con l'Iniziale Maiuscola!
    // ====================================================================

    /**
     * SCHERMATA HOME: Mostra lo storico delle sfide salvate.
     * ---> MODIFICA: Aggiunto popup Salva-Vita <---
     */
    @Composable
    fun HomeScreen(
        viewModel: MatchViewModel,
        onNavigateToCreate: () -> Unit,
        onNavigateToCounter: () -> Unit // Serve per riprendere la partita dal backup
    ) {
        // MOTORE APTICO: Prepariamo il sistema di vibrazione per questa schermata
        val haptic = LocalHapticFeedback.current

        // Recuperiamo il Contesto per poter lanciare l'Intento di Condivisione dalla Home
        val context = LocalContext.current

        // Stati per la Snackbar nella Home (per il cestino dello storico)
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // ---> LOGICA SALVA-VITA: Mostriamo il popup se c'è un backup <---
        if (viewModel.showResumeMatchDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showResumeMatchDialog = false },
                title = { Text("Partita in sospeso") },
                text = { Text("Hai lasciato una sfida a metà.\nVuoi riprenderla da dove l'avevi lasciata?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.resumeBackupMatch() // Carica i dati
                        onNavigateToCounter() // Vai al contatore
                    }) { Text("Riprendi") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.clearBackup() // Cancella definitivamente il backup
                        viewModel.showResumeMatchDialog = false
                    }) { Text("Cancella", color = MaterialTheme.colorScheme.error) }
                }
            )
        }

        // Scaffold è l'impalcatura pre-costruita del Material Design.
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // Montiamo la Snackbar fisicamente all'interno della pagina Home
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Lo Scaffold ha uno "slot" speciale già pronto per il pulsante fluttuante.
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        // EFFETTO APTICO: Vibrazione al tocco
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Esegue la funzione passata come parametro per navigare
                        onNavigateToCreate()
                    },
                    // Modella la posizione del pulsante aggiungendo un margine inferiore e laterale per sollevarlo dal bordo
                    modifier = Modifier.padding(bottom = 32.dp, end = 8.dp),
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Nuova Sfida",
                            // Modella la dimensione dell'icona forzandola a 28dp (rispetto allo standard di 24dp)
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Nuova Sfida",
                            // Modella l'aspetto del testo applicando lo stile titleLarge e forzando il grassetto
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        ) { innerPadding -> // innerPadding è lo spazio occupato dalle barre di sistema (batteria, ecc.)

            // Column ordina i suoi figli uno sotto l'altro (in verticale).
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Storico Sfide",
                    // Modella il titolo principale assegnandogli una dimensione prominente, il grassetto e il colore del tema
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // ---> GESTIONE DELLO STORICO <---
                if (viewModel.history.isEmpty()) {
                    Text(
                        text = "Nessuna sfida salvata al momento.",
                        // Modella il testo di stato vuoto con una dimensione media
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // LEZIONE: LazyColumn disegna solo gli elementi visibili sullo schermo.
                    // Ottimo per le performance se lo storico diventa lunghissimo.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Modella la spaziatura verticale tra gli elementi della lista (12dp di distanza l'uno dall'altro)
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.history) { record ->

                            // STATO: QUESTA SPECIFICA CARD È ESPANSA? (Ricordato in modo indipendente per ogni carta)
                            var expanded by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Rende la Card cliccabile, fa vibrare il telefono e inverte lo stato dell'animazione
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expanded = !expanded // Se era true diventa false, e viceversa
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                // Modella i margini interni della Card per dare più respiro al testo (20dp)
                                Column(modifier = Modifier.padding(20.dp)) {

                                    // ---> IL TITOLO E IL CRONOMETRO NELLO STORICO <---
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Intestazione sempre visibile
                                        Text(
                                            text = record.title,
                                            // Modella il titolo della singola sfida in grassetto
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Se la partita ha una durata registrata (> 0), mostra l'orologio
                                        if (record.durationSeconds > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Timer,
                                                    contentDescription = "Durata",
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

                                    // Modella la riga che contiene il nome del vincitore e l'icona a forma di freccia
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🏆 Vincitore: ${record.winnerName}",
                                            // Modella il testo del vincitore nel sommario con un peso medio
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        // Modella l'indicatore di espansione: se expanded è true mostra la freccia in su, altrimenti in giù
                                        Icon(
                                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = "Espandi dettagli",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // ---> IL CONTENUTO A SCOMPARSA ANIMATO <---
                                    // AnimatedVisibility gestisce da sola l'animazione fluida di entrata/uscita!
                                    AnimatedVisibility(visible = expanded) {
                                        Column(modifier = Modifier.padding(top = 16.dp)) {
                                            // Una linea divisoria sottile
                                            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                                            // Scorriamo tutti i giocatori salvati in questa partita (forEachIndexed ci dà l'indice)
                                            record.allPlayers.forEachIndexed { index, playerRecord ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Se è il primo (indice 0), mettiamo la coppa!
                                                    if (index == 0) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Filled.EmojiEvents,
                                                                contentDescription = "Vincitore",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(end = 8.dp)
                                                            )
                                                            Text(
                                                                text = "1° ${playerRecord.name}",
                                                                // Modella il nome del vincitore assegnando il grassetto
                                                                style = MaterialTheme.typography.titleLarge,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "${index + 1}° ${playerRecord.name}",
                                                            // Nomi degli altri giocatori senza grassetto
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                    }

                                                    // Punteggio
                                                    Text(
                                                        text = "${playerRecord.score} pt",
                                                        // Punteggio in grassetto solo se è il vincitore
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }

                                            // ---> MINI-GRAFICO NELLO STORICO (Game Stats) <---
                                            // Protezione: Disegna il grafico solo se c'è una storia e se è lunga almeno 2 step (per fare una riga)
                                            // Il "?: emptyList()" serve a NON far crashare l'app se apri una partita vecchia che non aveva la history salvata!
                                            val validHistory = record.allPlayers.any { (it.scoreHistory ?: emptyList()).size > 1 }
                                            if (validHistory) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Andamento Punteggi",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                // Richiamiamo il motore del grafico riducendone l'altezza a 120dp per farlo stare carino nella Card
                                                ScoreChart(players = record.allPlayers, modifier = Modifier.height(120.dp).fillMaxWidth().padding(top = 8.dp))
                                                HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                            }

                                            // ---> RIGA PER LA DATA (A SINISTRA) E LE AZIONI (A DESTRA) <---
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                // SpaceBetween allontana la data dai bottoni il più possibile
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. LA DATA DELLA PARTITA
                                                Text(
                                                    text = if (record.timestamp > 0L) formatDate(record.timestamp) else "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                // 2. I BOTTONI RAGGRUPPATI A DESTRA
                                                Row {
                                                    // ---> PULSANTE CONDIVIDI STORICO <---
                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                                            // Costruiamo il testo per WhatsApp leggendo direttamente da "record" (la partita salvata)
                                                            var shareText = "🏆 Risultati Storici: ${record.title}\n"
                                                            if (record.timestamp > 0L) shareText += "📅 Data: ${formatDate(record.timestamp)}\n"
                                                            if (record.durationSeconds > 0) shareText += "⏱️ Durata: ${formatTime(record.durationSeconds)}\n"
                                                            shareText += "\n"

                                                            record.allPlayers.forEachIndexed { index, player ->
                                                                val medal = when(index) {
                                                                    0 -> "🥇 1°"
                                                                    1 -> "🥈 2°"
                                                                    2 -> "🥉 3°"
                                                                    else -> "${index + 1}°"
                                                                }
                                                                shareText += "$medal ${player.name} - ${player.score} pt\n"
                                                            }
                                                            shareText += "\nGenerato con ScoreCounter 🎮\n© 2026 Creato da Nicola"

                                                            // Lanciamo l'intento di condivisione di Android
                                                            val sendIntent = Intent().apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                                type = "text/plain"
                                                            }
                                                            val shareIntent = Intent.createChooser(sendIntent, "Condividi partita passata")
                                                            context.startActivity(shareIntent)
                                                        },
                                                        // Modella il colore per distinguerlo dal cestino
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Share, contentDescription = "Condividi partita", tint = MaterialTheme.colorScheme.primary)
                                                    }

                                                    // ---> PULSANTE ELIMINA (Cestino) CON SNACKBAR E TIMER CUSTOM <---
                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                                            // 1. Salviamo i dati per poterli ripristinare se l'utente si pente
                                                            val removedIndex = viewModel.history.indexOf(record)
                                                            val removedRecord = record

                                                            // 2. Eliminiamo subito
                                                            viewModel.deleteMatch(record)

                                                            // 3. Lanciamo la Snackbar in background
                                                            coroutineScope.launch {
                                                                // IL NOSTRO TIMER PERSONALIZZATO (Uccide la Snackbar dopo 2.5 secondi)
                                                                launch {
                                                                    delay(2500L)
                                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                                }

                                                                // Mostriamo la Snackbar bloccandola su Infinita (la ucciderà il nostro timer qui sopra)
                                                                val result = snackbarHostState.showSnackbar(
                                                                    message = "Partita eliminata",
                                                                    actionLabel = "ANNULLA",
                                                                    duration = SnackbarDuration.Indefinite
                                                                )

                                                                // 4. Se l'utente clicca Annulla in tempo, resuscita la partita!
                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                    viewModel.restoreMatch(removedIndex, removedRecord)
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Filled.Delete, contentDescription = "Elimina partita", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // ---> FINE CONTENUTO A SCOMPARSA <---
                                }
                            }
                        }

                        // ---> FIRMA DIGITALE <---
                        item {
                            Spacer(modifier = Modifier.height(32.dp)) // Diamo un po' di respiro dopo l'ultima partita

                            Text(
                                text = "© 2026 Creato da NicolA380✈️\nTutti i diritti sono riservati.",
                                // Usiamo uno stile piccolo e "discreto" (labelSmall)
                                style = MaterialTheme.typography.labelSmall,
                                // Lo facciamo leggermente trasparente (alpha = 0.6f) per non renderlo invadente
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 80.dp), // Aggiungiamo spazio vitale in basso per non farla coprire dal pulsante "+" fluttuante
                                textAlign = TextAlign.Center // Centriamo il testo perfettamente
                            )
                        }

                    }
                }
            }
        }
    }

    /**
     * SCHERMATA CREAZIONE SFIDA: Impostazioni iniziali e aggiunta giocatori.
     */
    /**
     * SCHERMATA CREAZIONE SFIDA: Impostazioni iniziali e aggiunta giocatori.
     * ---> MODIFICA UI: Raggruppamento in Card (Material 3) e Icone nei campi di testo <---
     */
    @Composable
    fun CreateMatchScreen(
        viewModel: MatchViewModel,
        onNavigateToCounter: () -> Unit
    ) {
        // Stato per il nome del giocatore in fase di digitazione (rimane locale alla schermata)
        var newPlayerName by remember { mutableStateOf("") }

        // Stato che ricorda QUALE giocatore stiamo modificando. Se è "null", il popup di modifica è chiuso.
        var playerToEdit by remember { mutableStateOf<Player?>(null) }

        // STATI PER I NOMI RAPIDI (Preferiti)
        // Mostra o nasconde il popup principale di gestione dei preferiti
        var showFavoritesDialog by remember { mutableStateOf(false) }
        // Ricorda QUALE nome rapido stiamo modificando (apre un popup secondario sopra al primo)
        var favToEdit by remember { mutableStateOf<String?>(null) }

        // MOTORE APTICO: Prepariamo il sistema di vibrazione
        val haptic = LocalHapticFeedback.current

        // SISTEMA SNACKBAR (Il messaggio nero in basso con il tasto "Annulla" per i cestini)
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // Montiamo la Snackbar fisicamente all'interno della pagina
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Il grande pulsante fluttuante (FAB) per avviare la partita
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToCounter() // Quando clicco, vado al contatore!
                    },
                    // Solleva il bottone dal fondo e lo stacca leggermente da destra
                    modifier = Modifier.padding(bottom = 32.dp, end = 8.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Inizia", modifier = Modifier.size(28.dp)) },
                    text = { Text("Inizia Sfida", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                )
            }
        ) { innerPadding ->

            // Usiamo LazyColumn invece di Column affinché la pagina sia interamente scorrevole.
            // Questo previene fastidiosi bug visivi (elementi tagliati) su telefoni con schermi piccoli.
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // TITOLO PRINCIPALE DELLA SCHERMATA
                    Text(
                        text = "Nuova Partita",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // ====================================================================
                    // ---> PRIMA CARD: LE REGOLE DEL GIOCO (Raggruppamento Logico) <---
                    // ====================================================================
                    // LEZIONE: Usiamo una Card per raggruppare elementi simili. Questo riduce
                    // il carico cognitivo dell'utente (Legge della Vicinanza della Gestalt).
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        // Diamo alla Card uno sfondo che risalta leggermente rispetto alla pagina (Surface Variant)
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        // Bordi molto arrotondati, tipici del nuovo Material Design 3
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Regole del Gioco",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // --- NOME DELLA PARTITA ---
                            OutlinedTextField(
                                value = viewModel.matchTitle,
                                onValueChange = { viewModel.matchTitle = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nome della Sfida") },
                                placeholder = { Text("Es. Sfida Epica") },
                                shape = RoundedCornerShape(20.dp),
                                // ---> LEZIONE ICONE: 'leadingIcon' inserisce un'icona DENTRO il campo di testo, a sinistra.
                                // Questo si chiama "Affordance visiva": l'utente capisce cosa deve inserire ancor prima di leggere.
                                leadingIcon = {
                                    Icon(Icons.Filled.VideogameAsset, contentDescription = "Icona controller", tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            // Pulsanti rapidi per riempire il titolo velocemente
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.matchTitle = "Sfida Anime"
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f) // Entrambi i bottoni si dividono lo spazio a metà
                                ) { Text("Tema Anime") }

                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.matchTitle = "Sfida Carte"
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Tema Carte") }
                            }

                            // --- PUNTEGGIO OBIETTIVO ---
                            OutlinedTextField(
                                value = viewModel.targetScore,
                                onValueChange = { newValue ->
                                    // Accettiamo il nuovo testo SOLO se è vuoto oppure se è composto esclusivamente da numeri!
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        viewModel.targetScore = newValue
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Punteggio Obiettivo (Opzionale)") },
                                placeholder = { Text("Es. 50") },
                                shape = RoundedCornerShape(20.dp),
                                // ---> LEZIONE TASTIERA: Forza Android ad aprire il Tastierino Numerico invece della tastiera QWERTY completa! <---
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                // Icona della Coppa inserita a sinistra
                                leadingIcon = {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = "Icona trofeo", tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }

                    // ====================================================================
                    // ---> SECONDA CARD: LA GESTIONE DEI GIOCATORI <---
                    // ====================================================================
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Aggiungi Partecipanti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // --- CAMPO INSERIMENTO NUOVO GIOCATORE E BOTTONE AGGIUNGI ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlayerName,
                                    onValueChange = { newPlayerName = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Nome giocatore") },
                                    shape = RoundedCornerShape(20.dp),
                                    // Icona del "Profilo Utente" inserita a sinistra
                                    leadingIcon = {
                                        Icon(Icons.Filled.Person, contentDescription = "Icona utente", tint = MaterialTheme.colorScheme.primary)
                                    }
                                )

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Controllo di sicurezza: non aggiungiamo nomi vuoti composti solo da spazi
                                        if (newPlayerName.isNotBlank()) {
                                            viewModel.addPlayer(newPlayerName)
                                            newPlayerName = "" // Svuotiamo la casella dopo l'aggiunta per essere pronti al prossimo
                                        }
                                    },
                                    modifier = Modifier.padding(top = 6.dp).height(56.dp) // Altezza forzata per pareggiare la casella di testo
                                ) { Text("Aggiungi") }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            // Divisore grafico trasparente
                            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                            // --- SEZIONE GIOCATORI RAPIDI (PREFERITI) ---
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Giocatori Rapidi:", style = MaterialTheme.typography.titleSmall)

                                // Bottone testuale che fa aprire il popup (Dialog) di gestione dei nomi
                                TextButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showFavoritesDialog = true
                                }) { Text("Gestisci") }
                            }

                            // Se ci sono nomi salvati nel database, mostriamo lo scroll orizzontale
                            if (viewModel.favoriteNames.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(viewModel.favoriteNames) { fav ->
                                        OutlinedButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                // Aggiunge istantaneamente il preferito alla partita in corso!
                                                viewModel.addPlayer(fav)
                                            },
                                            shape = RoundedCornerShape(20.dp)
                                        ) { Text(fav) }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Nessun giocatore rapido salvato.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Titolo dinamico che cambia se l'elenco è vuoto o meno
                    Text(
                        text = if (viewModel.players.isEmpty()) "Nessun giocatore al tavolo." else "Giocatori pronti:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ====================================================================
                // === LA LISTA INFERIORE DEI GIOCATORI AGGIUNTI (MODIFICA E RIORDINA) ===
                // ====================================================================
                // itemsIndexed ci passa sia il numero di riga (index) che l'oggetto stesso (player)
                itemsIndexed(viewModel.players) { index, player ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // --- Frecce SU e GIÙ per ordinare i giocatori ---
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Sposta l'oggetto all'indice precedente (più in alto)
                                        viewModel.movePlayer(index, index - 1)
                                    },
                                    // Spento se siamo già al primo posto
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Sposta su", tint = MaterialTheme.colorScheme.primary) }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Sposta l'oggetto all'indice successivo (più in basso)
                                        viewModel.movePlayer(index, index + 1)
                                    },
                                    // Spento se siamo all'ultimo posto
                                    enabled = index < viewModel.players.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Sposta giù", tint = MaterialTheme.colorScheme.primary) }
                            }

                            // --- Nome Inserito ---
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )

                            // --- Bottone Matita (Modifica al volo) ---
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Salvando l'oggetto 'player' qui, il Compose rileva che non è più null
                                // e disegna istantaneamente il Popup di modifica!
                                playerToEdit = player
                            }) { Icon(Icons.Filled.Edit, contentDescription = "Modifica nome", tint = MaterialTheme.colorScheme.primary) }

                            // --- Bottone Cestino (Elimina con Snackbar) ---
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                // 1. Salvataggio di emergenza prima dell'eliminazione
                                val removedIndex = index
                                val removedPlayer = player

                                // 2. Eliminazione immediata per feeling visivo
                                viewModel.removePlayer(player)

                                // 3. Esecuzione asincrona della scritta nera in basso (Snackbar)
                                coroutineScope.launch {
                                    // TIMER CUSTOM: Dopo 2.5 secondi, la scritta scompare forzatamente
                                    launch {
                                        delay(2500L)
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                    }

                                    // Attendiamo la risposta dell'utente ("Ha cliccato Annulla?")
                                    val result = snackbarHostState.showSnackbar(
                                        message = "${player.name} rimosso",
                                        actionLabel = "ANNULLA",
                                        duration = SnackbarDuration.Indefinite
                                    )

                                    // 4. Se ha cliccato Annulla prima del timeout, ripristiniamo tutto!
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restorePlayer(removedIndex, removedPlayer)
                                    }
                                }
                            }) { Icon(Icons.Filled.Delete, contentDescription = "Elimina giocatore", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }

                // Aggiungiamo uno spazio vuoto enorme in fondo. Questo evita che l'ultimo
                // giocatore della lista finisca "nascosto" sotto il pulsante Inizia Sfida!
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        // === POPUP (DIALOG) MODIFICA GIOCATORE ===
        if (playerToEdit != null) {
            var editedName by remember { mutableStateOf(playerToEdit!!.name) }
            AlertDialog(
                onDismissRequest = { playerToEdit = null },
                title = { Text("Modifica Nome") },
                text = {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (editedName.isNotBlank()) {
                            playerToEdit!!.name = editedName
                            playerToEdit = null // Chiude il popup
                        }
                    }) { Text("Salva") }
                },
                dismissButton = { TextButton(onClick = { playerToEdit = null }) { Text("Annulla") } }
            )
        }

        // === POPUP GESTIONE PREFERITI ===
        // Questo popup serve a scrivere permanentemente i nomi nel database del telefono
        if (showFavoritesDialog) {
            var newFavName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showFavoritesDialog = false },
                title = { Text("Gestisci Giocatori Rapidi") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newFavName,
                                onValueChange = { newFavName = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Nuovo nome rapido") },
                                shape = RoundedCornerShape(16.dp)
                            )
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (newFavName.isNotBlank()) {
                                    viewModel.addFavorite(newFavName.trim())
                                    newFavName = ""
                                }
                            }) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp).padding(top = 8.dp)) {
                            items(viewModel.favoriteNames) { fav ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(fav, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); favToEdit = fav }) { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.removeFavorite(fav) }) { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showFavoritesDialog = false }) { Text("Chiudi") } }
            )
        }

        // === POPUP MODIFICA SINGOLO PREFERITO ===
        if (favToEdit != null) {
            var editedFavName by remember { mutableStateOf(favToEdit!!) }
            AlertDialog(
                onDismissRequest = { favToEdit = null },
                title = { Text("Modifica Nome Rapido") },
                text = {
                    OutlinedTextField(
                        value = editedFavName,
                        onValueChange = { editedFavName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (editedFavName.isNotBlank()) {
                            viewModel.editFavorite(favToEdit!!, editedFavName.trim())
                            favToEdit = null
                        }
                    }) { Text("Salva") }
                },
                dismissButton = { TextButton(onClick = { favToEdit = null }) { Text("Annulla") } }
            )
        }
    }

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

        // ---> STATI PER IL DADO VIRTUALE <---
        var showDiceDialog by remember { mutableStateOf(false) }
        var diceResult by remember { mutableIntStateOf(1) }

        // ---> NUOVO STATO: Ricorda QUALE giocatore stiamo modificando manualmente con la tastiera <---
        // Se è "null", il popup per l'inserimento manuale è nascosto.
        var playerForManualEdit by remember { mutableStateOf<Player?>(null) }

        // SISTEMA SNACKBAR (Tasto Annulla Azzeramento)
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        // ---> AVVIO CRONOMETRO AUTOMATICO <---
        // LaunchedEffect fa partire un blocco di codice non appena questa pagina viene "disegnata" sullo schermo.
        LaunchedEffect(Unit) {
            viewModel.startTimer()
        }

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
        // Intercetta il tasto "Indietro" fisico o lo swipe back del telefono. Invece di uscire, fa apparire un avviso!
        BackHandler {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showExitWarning = true
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // Montiamo la Snackbar in questa schermata per gli avvisi
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Usiamo la "bottomBar" (barra inferiore) dello Scaffold per i tasti principali d'azione
            bottomBar = {
                Row(
                    // Modella la posizione: solleviamo la riga dal bordo inferiore (bottom=32.dp) e le diamo margini laterali.
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    // Modella lo spazio vuoto in mezzo ai due bottoni (16dp)
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ---> Pulsante AZZERA <---
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showResetDialog = true
                        },
                        // weight(1f) divide lo schermo esattamente a metà tra i due bottoni (simmetria perfetta).
                        // height(56.dp) forza l'altezza ad essere identica a quella dei bottoni FAB fluttuanti.
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(text = "Azzera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    // ---> Pulsante FINE MATCH <---
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // Blocchiamo il cronometro un attimo prima di cambiare pagina!
                            viewModel.pauseTimer()
                            // L'animazione esplosiva scatta istantaneamente non appena si apre la nuova pagina dei risultati.
                            onNavigateToResults()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(text = "Fine Match", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
            ) {

                // Intestazione con TITOLO/CRONOMETRO a sinistra e PULSANTE DADO a destra
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Raggruppo Titolo e Cronometro in una Colonna per tenerli vicini
                    Column(modifier = Modifier.weight(1f)) {
                        // Titolo della sfida (Con salvagente se l'utente l'ha lasciato vuoto)
                        Text(
                            text = if (viewModel.matchTitle.isEmpty()) "Sfida" else viewModel.matchTitle,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        // ---> CRONOMETRO DI PARTITA IN TEMPO REALE <---
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Tempo di gioco",
                                modifier = Modifier.size(18.dp).padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            // Richiamiamo la nostra funzione di supporto per stampare il tempo bello "00:00"
                            Text(
                                text = formatTime(viewModel.matchDurationSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // ---> PULSANTE LANCIA DADO <---
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // Matematica pura: (1..6) crea un intervallo numerico da 1 a 6.
                            // Il comando '.random()' ne pesca uno a caso sfruttando l'entropia del processore!
                            diceResult = (1..6).random()
                            // Mostriamo il popup
                            showDiceDialog = true
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Lancia 🎲")
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

                // LazyColumn: La "lista intelligente" che renderizza graficamente solo i giocatori attualmente visibili sullo schermo
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // items() scorre la lista dei giocatori e per ognuno richiama la nostra funzione grafica "PlayerScoreCard"
                    items(viewModel.players) { p ->

                        // Capiamo se questo specifico giocatore merita la corona (Deve avere il maxScore e almeno 1 punto in attivo)
                        val isLeader = p.score == maxScore && maxScore > 0

                        // ---> MODIFICA: Passiamo il giocatore alla Carta, e le diciamo che se viene cliccato il numero,
                        // deve aggiornare il nostro stato "playerForManualEdit" per far aprire il popup!
                        PlayerScoreCard(
                            player = p,
                            isLeader = isLeader,
                            onScoreClick = { playerForManualEdit = p }
                        )
                    }
                }
            }
        }

        // ---> NUOVO: POPUP PER L'INSERIMENTO MANUALE DEL PUNTEGGIO DA TASTIERA <---
        if (playerForManualEdit != null) {
            // Stringa temporanea per ricordare cosa l'utente sta scrivendo. La pre-compiliamo col punteggio attuale!
            var scoreInput by remember { mutableStateOf(playerForManualEdit!!.score.toString()) }

            AlertDialog(
                onDismissRequest = { playerForManualEdit = null }, // Se l'utente clicca fuori, si chiude annullando
                title = { Text("Inserisci Punti per ${playerForManualEdit!!.name}") },
                text = {
                    OutlinedTextField(
                        value = scoreInput,
                        onValueChange = { newValue ->
                            // Controllo di Sicurezza: Consentiamo all'utente di scrivere SOLO numeri.
                            // Accettiamo anche il segno meno "-" da solo, così l'utente può digitare punteggi negativi (es. "-10")
                            if (newValue.isEmpty() || newValue == "-" || newValue.toIntOrNull() != null) {
                                scoreInput = newValue
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        // ---> LEZIONE TASTIERA: Forza l'apertura del Tastierino Numerico del telefono (Niente lettere!) <---
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        // Trasformiamo il testo digitato in un numero vero. Se l'utente ha scritto cavolate o ha lasciato vuoto, mettiamo 0 per sicurezza.
                        val newScore = scoreInput.toIntOrNull() ?: 0

                        // ---> IL TRUCCO PER IL GRAFICO <---
                        // Invece di dirgli "Il tuo nuovo punteggio è 50" (che romperebbe il grafico perché mancherebbe uno step),
                        // Calcoliamo la DIFFERENZA: (Nuovo Punteggio - Vecchio Punteggio).
                        // Es: Se aveva 10 e scrive 50, la differenza è +40. Passiamo +40 alla funzione changeScore!
                        val diff = newScore - playerForManualEdit!!.score
                        playerForManualEdit!!.changeScore(diff) // Aggiorna il numero E la memoria storica!

                        playerForManualEdit = null // Chiudiamo il popup soddisfatti
                    }) { Text("Salva") }
                },
                dismissButton = {
                    TextButton(onClick = { playerForManualEdit = null }) { Text("Annulla") }
                }
            )
        }

        // Popup di conferma AZZERAMENTO con SNACKBAR ANNULLA E TIMER CUSTOM
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Sei proprio sicuro?") },
                text = { Text("Sei sicurissimo di voler azzerare tutto?\nQuesta azione non può essere annullata.") },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            // 1. Azzeriamo ma CI SALVIAMO una fotografia dei vecchi punteggi grazie a questa nuova funzione!
                            val oldScores = viewModel.resetScoresWithUndo()
                            showResetDialog = false // Chiude il popup

                            // 2. Mostriamo la Snackbar di "Ops, ho sbagliato" in background
                            coroutineScope.launch {

                                // ---> IL NOSTRO TIMER PERSONALIZZATO DELLA SNACKBAR <---
                                launch {
                                    delay(2500L) // Regola qui i millisecondi! (2500 = 2.5 secondi di permanenza)
                                    snackbarHostState.currentSnackbarData?.dismiss() // Passato il tempo, uccide la Snackbar
                                }

                                // Mostriamo il messaggio bloccandolo temporaneamente su "Indefinite" per far agire il nostro timer qui sopra
                                val result = snackbarHostState.showSnackbar(
                                    message = "Punteggi azzerati",
                                    actionLabel = "ANNULLA",
                                    duration = SnackbarDuration.Indefinite
                                )

                                // 3. Se l'utente clicca Annulla in tempo utile, la logica interviene e ripristina la "fotografia" dei punti!
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreScores(oldScores)
                                }
                            }
                        }
                    ) { Text("Sì, azzera") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showResetDialog = false
                    }) { Text("Annulla") }
                }
            )
        }

        // Popup di conferma per il SALVA-VITA (Uscita accidentale)
        if (showExitWarning) {
            AlertDialog(
                onDismissRequest = { showExitWarning = false },
                title = { Text("Abbandonare la partita?") },
                text = { Text("Se torni alla Home, i progressi attuali andranno persi per sempre. Sei sicuro sicuro di voler uscire?") },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExitWarning = false
                            onNavigateHome() // Esegue la funzione di chiusura drastica che abbiamo passato dal NavHost
                        },
                        // Usiamo il colore d'errore (Rosso) del tema per far intuire istintivamente che è un'azione distruttiva
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Sì, esci") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showExitWarning = false // Falso allarme, l'utente chiude il popup e continua a giocare serenamente!
                    }) { Text("Annulla") }
                }
            )
        }

        // ---> POPUP DEL DADO VIRTUALE <---
        if (showDiceDialog) {
            AlertDialog(
                onDismissRequest = { showDiceDialog = false }, // Chiudi il popup se si tocca lo schermo grigio fuori
                title = { Text("Lancio del Dado") },
                text = {
                    // Mostriamo il risultato del dado in modo gigante e ben centrato
                    Text(
                        text = "🎲 $diceResult",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    // Tasto per rullare di nuovo senza chiudere la finestra e riaprirla
                    Button(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        diceResult = (1..6).random() // Rulla di nuovo generando un nuovo numero!
                    }) { Text("Tira di nuovo") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDiceDialog = false
                    }) { Text("Chiudi") }
                }
            )
        }
    }

    /**
     * COMPONENTE: Card personalizzata per la singola riga del giocatore nella fase di punteggio.
     * ---> NUOVO PARAMETRO: 'onScoreClick' è la funzione che esegue quando tocchiamo il numerone
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun PlayerScoreCard(player: Player, isLeader: Boolean = false, onScoreClick: () -> Unit) {
        val haptic = LocalHapticFeedback.current

        // Card è un contenitore bellissimo del Material Design (sfondo leggero, bordi arrotondati e una leggera ombra invisibile)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Row allinea gli elementi (Nome e Corona a sinistra, punteggi a destra) in orizzontale
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Raggruppo Nome e Corona in una riga interna per farli stare assieme a sinistra
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Nome del giocatore
                    Text(text = player.name, style = MaterialTheme.typography.titleLarge)

                    // ---> DISEGNO DELLA CORONA DEL LEADER <---
                    if (isLeader) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium, // Un'icona a medaglia/stella molto in stile Material 3
                            contentDescription = "In Vantaggio",
                            // La stilizziamo usando il colore Primario di accento del tuo tema utente!
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp).size(24.dp)
                        )
                    }
                }

                // Raggruppiamo i controlli matematici (Meno, Numero, Più) in un'altra mini-Row a destra
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // ---> PULSANTE MENO CON PUNTEGGIO RAPIDO (Long Press) <---
                    Box(
                        modifier = Modifier
                            .size(48.dp) // Grandezza standard dei bottoni touch per un dito umano
                            .clip(CircleShape) // Ritaglia l'effetto dell'onda grigia del tocco a forma di cerchio perfetto
                            .combinedClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // MODIFICA GRAFICO: Usiamo la nuova funzione changeScore() invece di un banale score--.
                                    // In questo modo, l'app sa che deve salvare questo -1 anche nella memoria storica del grafico!
                                    player.changeScore(-1)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Pressione Lunga: -5 Punti in un colpo solo (Modifica Utente Mantenuta!)
                                    player.changeScore(-5)
                                }
                            ),
                        contentAlignment = Alignment.Center // Mette l'icona "-" perfettamente al centro del Box
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Diminuisci")
                    }

                    // ---> PUNTEGGIO GIGANTE CLICCABILE <---
                    Text(
                        text = player.score.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        // Usiamo 'modifier' per dirgli che ora non è più solo un testo da leggere, ma un bottone da cliccare!
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onScoreClick() // Esegue il comando di apertura del popup passato dalla CounterScreen
                            }
                    )

                    // ---> PULSANTE PIÙ CON PUNTEGGIO RAPIDO (Long Press) <---
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    player.changeScore(1) // +1 e salva nella memoria del grafico!
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    player.changeScore(10) // +10 in un colpo solo per velocizzare i giochi a punti alti!
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Aumenta")
                    }
                }
            }
        }
    }


    // ====================================================================
    // 5. MODELLO DATI E VIEWMODEL
    // ====================================================================

    // 1. IL MODELLO DATI (I Giocatori Attivi durante la partita corrente)
    // Usiamo "var name by mutableStateOf" invece del semplice "val".
    // Se fosse stato "val", Kotlin avrebbe vietato le modifiche testuali una volta creato l'oggetto.
    // "mutableStateOf" è lo Stato Magico di Compose: se tu cambi questo valore nel ViewModel,
    // Compose si accorge del cambiamento e ridisegna immediatamente l'interfaccia ovunque appaia questo nome.
    class Player(initialName: String) {
        var name by mutableStateOf(initialName)
        var score by mutableStateOf(0)

        // ---> LA MEMORIA STORICA DEL GIOCATORE PER IL GRAFICO <---
        // È una lista di numeri. Inizia sempre con uno [0] non appena il giocatore viene creato e siede al tavolo.
        val scoreHistory = mutableStateListOf<Int>(0)

        // Questa funzione magica cambia il punteggio attuale e LO APPUNTA nel diario segreto!
        fun changeScore(amount: Int) {
            score += amount
            scoreHistory.add(score)
        }
    }

    // ---> FOTOGRAFIA DEL GIOCATORE PER IL DATABASE <---
    // DataStore & Gson (il traduttore in file di testo) non supportano gli Stati animati di Compose.
    // Quindi creiamo una classe "Data" nuda e cruda che serve solo per essere scritta su disco e non per giocare in tempo reale.
    data class PlayerRecord(
        val name: String,
        val score: Int,
        // CORREZIONE SICUREZZA GSON: "scoreHistory" ha un punto di domanda (?).
        // Significa che per le vecchie partite salvate un anno fa, questo dato può essere "null" senza far crashare il telefono.
        val scoreHistory: List<Int>? = emptyList()
    )

    // ---> IL MODELLO DATI DELLO STORICO (MatchRecord) <---
    // Una "data class" leggera che fotografa lo stato dell'intera partita al momento della fine.
    data class MatchRecord(
        val title: String,
        val winnerName: String,
        val winningScore: Int,
        val allPlayers: List<PlayerRecord>,
        // Salviamo la durata della partita (Il default 0L previene errori coi vecchi salvataggi!)
        val durationSeconds: Long = 0L,
        // ---> NUOVO: Salviamo il millisecondo esatto della chiusura della partita per ricavare la Data
        val timestamp: Long = 0L
    )

    // ---> NUOVO: Modello per il backup salva-vita della partita in corso <---
    data class MatchBackup(
        val title: String,
        val target: String,
        val players: List<PlayerRecord>,
        val seconds: Long
    )

    // 2. IL VIEWMODEL è il VERO cervello dell'app.
    // A differenza della grafica che viene distrutta e ricreata se ad esempio giri lo schermo del telefono in orizzontale,
    // il ViewModel è immortale finché l'app è aperta. Inoltre è un "AndroidViewModel",
    // il che significa che gli viene passato in automatico il contesto fisico del telefono (per leggere la sua memoria locale).
    class MatchViewModel(application: Application) : AndroidViewModel(application) {
        var matchTitle by mutableStateOf("")

        // Stato per memorizzare l'obiettivo di vittoria (Stringa per il campo di testo della Creation Screen)
        var targetScore by mutableStateOf("")

        val players = mutableStateListOf<Player>()
        val history = mutableStateListOf<MatchRecord>()
        val favoriteNames = mutableStateListOf<String>()

        // ---> STATI PER IL CRONOMETRO <---
        // Variabile che conta i secondi (Osservata dalla UI grafica per aggiornare lo schermo in tempo reale)
        var matchDurationSeconds by mutableLongStateOf(0L)

        // ---> STATI SALVA-VITA <---
        // Mostra o nasconde il popup all'apertura dell'app
        var showResumeMatchDialog by mutableStateOf(false)
        // Variabile che conserva la partita "fantasma" trovata nella memoria
        private var pendingBackup: MatchBackup? = null

        // Oggetto che contiene il "processo in background" del timer per poterlo fermare quando vogliamo (Pause/Reset)
        private var timerJob: Job? = null

        // Nomi dei "File di Testo" che verranno creati nella memoria fisica del telefono dal DataStore
        private val historyKey = stringPreferencesKey("history_list")
        private val favoritesKey = stringPreferencesKey("favorites_list")
        // ---> NUOVA CHIAVE DATABASE PER IL BACKUP <---
        private val backupKey = stringPreferencesKey("current_match_backup")

        // Gson è il traduttore. Trasforma array, oggetti complessi e liste in lunghissime stringhe di testo (JSON) e viceversa per poterle salvare.
        private val gson = Gson()

        // L'init viene eseguito UNA sola volta, appena il cervello si "accende" aprendo l'app
        init {
            loadData()
        }

        private fun loadData() {
            // viewModelScope.launch avvia un "Filo di esecuzione secondario" (Coroutine).
            // Serve per non far laggare o bloccare l'interfaccia grafica mentre l'app cerca e legge faticosamente sul disco rigido.
            viewModelScope.launch {
                val preferences = getApplication<Application>().dataStore.data.first()

                // 1. CARICAMENTO DELLO STORICO (Lettura JSON -> Gson traduce la stringa -> Lista Oggetti in Kotlin)
                val jsonHistoryString = preferences[historyKey]
                if (jsonHistoryString != null) {
                    // TypeToken è una furbata di Gson per aiutarlo a capire in cosa deve tradurre la stringa complessa
                    val type = object : TypeToken<List<MatchRecord>>() {}.type
                    val savedList: List<MatchRecord> = gson.fromJson(jsonHistoryString, type)
                    history.clear()
                    history.addAll(savedList)
                }

                // 2. CARICAMENTO DEI GIOCATORI PREFERITI
                val jsonFavsString = preferences[favoritesKey]
                if (jsonFavsString != null) {
                    val type = object : TypeToken<List<String>>() {}.type
                    val savedFavs: List<String> = gson.fromJson(jsonFavsString, type)
                    favoriteNames.clear()
                    favoriteNames.addAll(savedFavs)
                }

                // ---> 3. LOGICA CARICAMENTO BACKUP SALVA-VITA <---
                val jsonBackupString = preferences[backupKey]
                if (jsonBackupString != null) {
                    pendingBackup = gson.fromJson(jsonBackupString, MatchBackup::class.java)
                    // Se abbiamo trovato un backup non vuoto, "accendiamo" la variabile per mostrare il popup in Home!
                    if (pendingBackup != null && pendingBackup!!.players.isNotEmpty()) {
                        showResumeMatchDialog = true
                    }
                }
            }
        }

        // ---> FUNZIONE PER RIPRENDERE LA PARTITA DAL BACKUP <---
        fun resumeBackupMatch() {
            pendingBackup?.let { backup ->
                matchTitle = backup.title
                targetScore = backup.target
                matchDurationSeconds = backup.seconds
                players.clear() // Puliamo il tavolo
                // Rimettiamo i giocatori al loro posto con tutta la loro storia intatta per i grafici
                backup.players.forEach { record ->
                    val p = Player(record.name)
                    p.score = record.score
                    p.scoreHistory.clear()
                    p.scoreHistory.addAll(record.scoreHistory ?: listOf(0))
                    players.add(p)
                }
            }
            showResumeMatchDialog = false // Chiude il popup
        }

        // ---> FUNZIONE PER SALVARE IL BACKUP <---
        fun saveBackup() {
            if (players.isEmpty()) return
            val backup = MatchBackup(
                title = matchTitle,
                target = targetScore,
                players = players.map { PlayerRecord(it.name, it.score, it.scoreHistory.toList()) },
                seconds = matchDurationSeconds
            )
            viewModelScope.launch {
                getApplication<Application>().dataStore.edit { it[backupKey] = gson.toJson(backup) }
            }
        }

        // ---> FUNZIONE PER CANCELLARE IL BACKUP (Partita finita o rifiutata) <---
        fun clearBackup() {
            viewModelScope.launch {
                getApplication<Application>().dataStore.edit { it.remove(backupKey) }
            }
        }

        // ---> GESTIONE CRONOMETRO <---
        fun startTimer() {
            // Avvia il timer in background SOLO se non è già partito per non sovrapporli
            if (timerJob == null || timerJob?.isActive == false) {
                timerJob = viewModelScope.launch {
                    while (true) {
                        delay(1000L) // Blocca il processo silente per 1000 millisecondi (1 secondo perfetto)
                        matchDurationSeconds++ // Aggiunge un secondo alla variabile che la grafica sta guardando
                        // ---> SALVATAGGIO BACKUP PERIODICO <---
                        // Salviamo il backup in silenzio ogni 5 secondi per non stressare il disco fisso,
                        // garantendo però una sicurezza di recupero quasi totale.
                        if (matchDurationSeconds % 5 == 0L) saveBackup()
                    }
                }
            }
        }

        fun pauseTimer() {
            timerJob?.cancel() // Uccide il processo asincrono in background (il tempo si ferma)
        }

        fun resetTimer() {
            pauseTimer()
            matchDurationSeconds = 0L // Riporta l'orologio a zero
        }

        // Aggiunge un nuovo oggetto Player (che partirà da 0 punti e 0 storico) alla lista attiva della partita
        fun addPlayer(name: String) {
            players.add(Player(name))
            saveBackup() // Salviamo subito il nuovo giocatore nel salva-vita
        }

        // Diamo al cervello dell'app il potere di eliminare un giocatore attivo
        fun removePlayer(player: Player) {
            players.remove(player)
            saveBackup() // Aggiorniamo il salva-vita
        }

        // Funzione per la Snackbar che resuscita un giocatore eliminato dalla partita corrente (Se l'utente preme Annulla)
        fun restorePlayer(index: Int, player: Player) {
            // Se possibile, lo rimettiamo matematicamente nella esatta posizione (index) in cui era!
            if (index in 0..players.size) {
                players.add(index, player)
            } else {
                players.add(player) // Piano B in caso di bug: lo mettiamo in fondo alla lista
            }
            saveBackup() // Ri-aggiorniamo il salva-vita
        }

        // Funzione per la Snackbar che resuscita una partita appena eliminata dallo storico
        fun restoreMatch(index: Int, record: MatchRecord) {
            if (index in 0..history.size) {
                history.add(index, record)
            } else {
                history.add(record)
            }
            // Dato che lo storico è persistente, se resuscitiamo una partita dobbiamo RI-SALVARE subito su disco fisico!
            viewModelScope.launch {
                val jsonString = gson.toJson(history)
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[historyKey] = jsonString
                }
            }
        }

        // ---> FUNZIONI GESTIONE GIOCATORI RAPIDI (PREFERITI) <---
        fun addFavorite(name: String) {
            if (!favoriteNames.contains(name)) { // Controllo IF logico per evitare che l'utente inserisca un duplicato identico
                favoriteNames.add(name)
                saveFavorites() // Dopo ogni modifica alla lista, salva fisicamente sul disco
            }
        }

        fun removeFavorite(name: String) {
            favoriteNames.remove(name)
            saveFavorites()
        }

        fun editFavorite(oldName: String, newName: String) {
            // Anche in modifica evitiamo di sovrascrivere un nome con uno che esiste già
            if (!favoriteNames.contains(newName)) {
                val index = favoriteNames.indexOf(oldName)
                if (index != -1) {
                    favoriteNames[index] = newName // Sostituzione diretta tramite indice della stringa
                    saveFavorites()
                }
            }
        }

        // La logica di scrittura fisica dei preferiti (sempre asincrona 'launch' per non bloccare l'interfaccia grafica utente)
        private fun saveFavorites() {
            viewModelScope.launch {
                val jsonString = gson.toJson(favoriteNames)
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[favoritesKey] = jsonString
                }
            }
        }

        // Elimina una partita intera dallo storico visualizzato e la cancella dal file fisico.
        fun deleteMatch(record: MatchRecord) {
            history.remove(record)
            viewModelScope.launch {
                val jsonString = gson.toJson(history)
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[historyKey] = jsonString
                }
            }
        }

        // Sposta fisicamente un giocatore su o giù nella lista per il riordino grafico (Drag & Drop fittizio)
        fun movePlayer(fromIndex: Int, toIndex: Int) {
            // Controllo di sicurezza vitale: verifichiamo che l'indice di partenza (from) e di arrivo (to) esistano davvero.
            // Altrimenti, se cerco di spostare il giocatore all'indice 5, ma la lista ha solo 3 giocatori, l'app va in Crash letale (IndexOutOfBoundsException)!
            if (fromIndex in players.indices && toIndex in players.indices) {
                // Rimuoviamo il giocatore dalla vecchia posizione temporaneamente
                val player = players.removeAt(fromIndex)
                // Lo reinseriamo subito nella posizione desiderata
                players.add(toIndex, player)
                saveBackup() // Salviamo il nuovo ordine nel salva-vita
            }
        }

        // Funzione furba per la Snackbar dell'azzeramento! Invece di azzerare e basta, fa prima una "Copia di Sicurezza"
        fun resetScoresWithUndo(): List<Pair<Int, List<Int>>> {
            // Mappa e salva una lista di "Coppie" (Pair): Il punteggio finale del giocatore e TUTTA la sua lunga lista di mosse passate
            val oldData = players.map { Pair(it.score, it.scoreHistory.toList()) }

            // Fatta la copia, azzera brutalmente i punti
            players.forEach {
                it.score = 0
                it.scoreHistory.clear()
                it.scoreHistory.add(0) // Registra l'azzeramento nel grafico come se fosse un tuffo verticale verso il basso!
            }
            saveBackup() // Aggiorniamo il salva-vita con i punti a zero
            return oldData // Restituisce i vecchi punti alla grafica in caso l'utente premesse Annulla!
        }

        // Se l'utente clicca "Annulla" sulla Snackbar, riceve la copia di sicurezza e la re-inietta!
        fun restoreScores(oldData: List<Pair<Int, List<Int>>>) {
            players.forEachIndexed { index, player ->
                if(index < oldData.size) {
                    player.score = oldData[index].first // Ripristina i punti correnti
                    player.scoreHistory.clear() // Pulisce il grafico sbagliato
                    player.scoreHistory.addAll(oldData[index].second) // Ripristina tutta la storia originale del grafico!
                }
            }
            saveBackup() // Ri-aggiorna il salva-vita
        }

        fun clearMatch() {
            matchTitle = ""
            targetScore = "" // Puliamo anche l'obiettivo di vittoria precedente per non portarcelo nelle sfide future
            players.clear()
            resetTimer() // Assicuriamoci che il timer parta da 0 nella prossima partita
            clearBackup() // Fondamentale: se iniziamo da zero, il vecchio backup fantasma deve sparire
        }

        // ---> LA FUNZIONE DI SALVATAGGIO DEFINITIVA <---
        fun saveCurrentMatch() {
            // Kotlin scansiona tutta la lista e mi trova subito l'oggetto (Player) che ha la variabile 'score' più alta in assoluto!
            val winner = players.maxByOrNull { it.score }

            // Salviamo solo se c'è effettivamente un vincitore (non possiamo salvare partite vuote)
            if (winner != null) {
                val finalTitle = if (matchTitle.isEmpty()) "Sfida Senza Nome" else matchTitle

                // 1. Prima di salvare nel DB, congeliamo i dati calcolati:
                // "map" cicla tutta la lista di Player(attivi per la partita) e per ognuno restituisce un PlayerRecord(statico, non modificabile).
                // Subito dopo li ordina (sortedByDescending) mettendo in cima chi ha più punti, in modo che il file su disco sia già perfettamente in ordine!
                val snapshotOfPlayers = players.map { activePlayer ->
                    PlayerRecord(
                        name = activePlayer.name,
                        score = activePlayer.score,
                        scoreHistory = activePlayer.scoreHistory.toList() // Copiamo la memoria nel record
                    )
                }.sortedByDescending { it.score }

                // 2. Creiamo il record della Partita completo
                val record = MatchRecord(
                    title = finalTitle,
                    winnerName = winner.name,
                    winningScore = winner.score,
                    allPlayers = snapshotOfPlayers, // Passiamo al database l'intera classifica congelata
                    durationSeconds = matchDurationSeconds, // Salviamo i secondi del cronometro!
                    timestamp = System.currentTimeMillis() // Salviamo il millisecondo esatto della chiusura per la data
                )

                // history.add(0, ...) Inseriamo la nuova partita salvata IN CIMA alla lista grafica (Posizione 0), non in fondo!
                history.add(0, record)

                // Scrittura fisica finale su Android
                viewModelScope.launch {
                    val jsonString = gson.toJson(history)
                    getApplication<Application>().dataStore.edit { prefs ->
                        prefs[historyKey] = jsonString
                    }
                }
                // Partita finita e salvata correttamente: distruggiamo il file temporaneo salva-vita!
                clearBackup()
            }
        }
    }

    // ====================================================================
    // IL MOTORE DEL GRAFICO A LINEE (ScoreChart / Game Stats)
    // ====================================================================

    /**
     * Questo componente speciale Canvas disegna fisicamente le linee del punteggio nel tempo (le statistiche).
     * È scritto da zero usando la geometria vettoriale e la trigonometria di base,
     * ed è stato corretto per mostrare il grafico in ogni situazione prevenendo i crash matematici.
     */
    @Composable
    fun ScoreChart(players: List<PlayerRecord>, modifier: Modifier = Modifier) {

        // Sicurezza 1: Se non ci sono giocatori salvati nella lista, fermati e non provare a disegnare il nulla.
        if (players.isEmpty()) return

        // Sicurezza 2: Filtriamo e teniamo in memoria solo i giocatori che hanno effettivamente dei dati nello storico.
        // Usiamo "?: emptyList()" come salvagente per proteggerci dai vecchi salvataggi GSON fatti prima di questo aggiornamento.
        val validPlayers = players.filter { (it.scoreHistory ?: emptyList()).isNotEmpty() }
        if (validPlayers.isEmpty()) return

        // Troviamo il punteggio più alto mai raggiunto (per capire quanto fare "alto" il soffitto del grafico)
        val maxScore = validPlayers.maxOf { (it.scoreHistory ?: emptyList()).maxOrNull() ?: 0 }
        // Troviamo quello più basso (per il pavimento)
        val minScore = validPlayers.minOf { (it.scoreHistory ?: emptyList()).minOrNull() ?: 0 }

        // Colori pastello, fighi e vibranti per distinguere facilmente le linee!
        val lineColors = listOf(
            Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
            Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFF00ACC1)
        )

        Column(modifier = modifier) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {

                // ---> IL PADDING DI SICUREZZA <---
                // Per evitare che i pallini tocchino i bordi dello schermo e vengano brutalmente tagliati a metà dal telefono,
                // diciamo al grafico di tenersi a "16 pixel" di distanza interna dai margini!
                val padY = 16.dp.toPx()
                val padX = 8.dp.toPx()

                // Calcoliamo lo spazio *effettivamente disegnabile* in modo blindato.
                // coerceAtLeast(1f) vieta al Canvas di essere grande 0 pixel per evitare errori grafici.
                val drawW = (size.width - padX * 2).coerceAtLeast(1f)
                val drawH = (size.height - padY * 2).coerceAtLeast(1f)

                // Spazio verticale logico (range): es. da punteggio -5 a 20 ci sono 25 "piani" o step.
                // coerceAtLeast(1) è VITALE: previene divisioni per zero mortali nel caso in cui nessuno abbia fatto punti!
                val yRange = (maxScore - minScore).coerceAtLeast(1).toFloat()

                // Tracciamo la fatidica "Linea dello Zero" grigia sottile (se i punteggi scendono in negativo)
                if (maxScore > 0 && minScore < 0) {
                    val zeroY = padY + drawH - ((0 - minScore) / yRange * drawH)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(padX, zeroY),
                        end = Offset(padX + drawW, zeroY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Ciclo Maestro: Per ogni giocatore valido, disegniamo la sua "montagna russa" di punti
                validPlayers.forEachIndexed { index, player ->
                    // Assegniamo un colore (usiamo modulo % per farlo ripetere se ci sono più di 6 giocatori)
                    val color = lineColors[index % lineColors.size]
                    // L'oggetto Path è la "penna" che struscia sullo schermo
                    val path = Path()
                    val history = player.scoreHistory ?: emptyList()

                    // ---> LA CORREZIONE "ANTI-VUOTO" <---
                    // Se la partita finisce con un solo click (es. premo "Fine Match" subito o gioco 1 mossa sola),
                    // non abbiamo 2 punti cartesiani necessari per tirare una riga in diagonale.
                    if (history.size == 1) {
                        // Quindi tiriamo noi manualmente una riga piatta orizzontale da sinistra fino alla fine destra.
                        val y = padY + drawH - ((history[0] - minScore) / yRange * drawH)
                        path.moveTo(padX, y) // Poggia la penna a sinistra
                        path.lineTo(padX + drawW, y) // Tira la linea dritta a destra

                        // Facciamo due bei pallini colorati agli estremi
                        drawCircle(color, 4.dp.toPx(), Offset(padX, y))
                        drawCircle(color, 4.dp.toPx(), Offset(padX + drawW, y))
                    } else {
                        // Se invece la partita è normale e ci sono 2 o più mosse...
                        // Dividiamo la larghezza dello schermo in "X" passettini temporali uguali
                        val xStep = drawW / (history.size - 1).toFloat()

                        history.forEachIndexed { turn, score ->
                            // Calcolo Posizione Orizzontale (Turno)
                            val x = padX + (turn * xStep)
                            // Calcolo Posizione Verticale (Altezza del punteggio in base alle proporzioni)
                            val y = padY + drawH - ((score - minScore) / yRange * drawH)

                            if (turn == 0) path.moveTo(x, y) // Primo giro, poggia il pennarello
                            else path.lineTo(x, y) // Altri giri, struscia il pennarello verso le nuove coordinate (tirando la linea)

                            // Alla fine di ogni riga tracciata, disegniamo anche un pallino di giuntura per evidenziare il punto esatto!
                            drawCircle(color, 4.dp.toPx(), Offset(x, y))
                        }
                    }

                    // Eseguiamo il disegno definitivo della linea "Path" creata per questo giocatore.
                    // Usiamo StrokeCap.Round e StrokeJoin.Round per fare in modo che le linee e le curve siano morbide e non "spigolose"
                    drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // ---> LA LEGENDA SICURA (horizontalScroll) <---
            // Abbiamo creato una barra sotto al grafico che ti dice a chi appartiene ogni colore (es. Pallino Rosso: Pier).
            // Usiamo horizontalScroll(rememberScrollState()) per far sì che, se i giocatori sono tantissimi,
            // la riga non si rompa o scompari dallo schermo, ma tu possa farla scorrere lateralmente con il dito!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                validPlayers.forEachIndexed { index, player ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp, bottom = 4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(lineColors[index % lineColors.size]))
                        Text(text = player.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }


    // ====================================================================
    // LA SCHERMATA DELLA CLASSIFICA (RISULTATI) E ANIMAZIONE ESPLOSIONE
    // ====================================================================

    @Composable
    fun ResultsScreen(
        viewModel: MatchViewModel,
        onNavigateHome: () -> Unit
    ) {
        /**
         * IL COMMENTO DELLA LAMBDA EXPRESSION
         * * Dove:
         * val rankedPlayers = Stiamo creando una nuova variabile immutabile (val) che chiamiamo "giocatori classificati".
         * Nota importante: questa operazione non va a scombinare la lista originale dentro il ViewModel;
         * prende l'elenco originale, lo ordina e salva il risultato ordinato dentro questo nuovo cassetto.
         *
         * viewModel.players = Questa è la nostra lista di partenza.
         * Immagina che contenga: [Marco(Punti: 5), Andrea(Punti: 12), Daniele(Punti: -2)].
         *
         * sortedByDescending = Questa è una funzione integrata (già pronta) di Kotlin che lavora sulle liste.
         * Si traduce letteralmente come:
         * sorted: Ordina
         * By: In base a...
         * Descending: Dal più grande al più piccolo.
         * * { player -> player.score } (La vera magia: La Lambda)
         * Le parentesi graffe indicano una Lambda Expression (una funzione passata al volo).
         * La funzione sortedByDescending è stupida: sa come ordinare i numeri, ma non sa cos'è un "Giocatore".
         * Quindi ti chiede: "Ehi, mi hai dato una lista di oggetti Giocatore. Io non so come confrontarli.
         * Qual è il numero che devo guardare per metterli in ordine?"
         * Questa formula risponde a quella domanda. Si legge così in italiano:
         * "Prendi ogni singolo player, guarda la freccia -> e restituiscimi il suo player.score".
         * In C++ avresti dovuto scrivere un "Comparator" personalizzato. Qui basta un rigo!
         */
        val rankedPlayers = viewModel.players.sortedByDescending { player -> player.score }

        val haptic = LocalHapticFeedback.current

        // Recuperiamo il Contesto per poter lanciare l'Intento di Condivisione
        val context = LocalContext.current

        // Appena entriamo in questa schermata, l'esplosione è VERA di default!
        // In questo modo, l'animazione partirà all'istante (nello stesso millesimo di secondo)
        // in cui compare la grafica della classifica. Addio scatti!
        var showConfetti by remember { mutableStateOf(true) }

        // Avvolgiamo lo Scaffold in un Box (Scatola). Il Box serve per sovrapporre il "livello"
        // dei coriandoli sopra il "livello" della classifica (lo Scaffold).
        Box(modifier = Modifier.fillMaxSize()) {

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    // LA CONDIVISIONE TESTUALE
                    // Usiamo una Column per impilare il bottone "Condividi" sopra a quello "Salva e Torna"
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp)) {

                        // IL PULSANTE DI CONDIVISIONE
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                // 1. Costruiamo il testo magico che l'utente invierà su WhatsApp!
                                val finalTitle = if (viewModel.matchTitle.isEmpty()) "Sfida Senza Nome" else viewModel.matchTitle
                                var shareText = "🏆 Risultati: $finalTitle\n"

                                // Mostriamo il cronometro nella condivisione!
                                if (viewModel.matchDurationSeconds > 0) {
                                    shareText += "⏱️ Durata: ${formatTime(viewModel.matchDurationSeconds)}\n"
                                }
                                // Aggiungiamo anche la data di oggi nella condivisione (visto che stiamo salvando in questo istante)
                                shareText += "📅 Data: ${formatDate(System.currentTimeMillis())}\n\n"

                                // Cicliamo tutti i giocatori e aggiungiamo le medagliette
                                rankedPlayers.forEachIndexed { index, player ->
                                    val medal = when(index) {
                                        0 -> "🥇 1°"
                                        1 -> "🥈 2°"
                                        2 -> "🥉 3°"
                                        else -> "${index + 1}°" // Dal 4° posto in poi mettiamo solo il numero
                                    }
                                    shareText += "$medal ${player.name} - ${player.score} pt\n"
                                }
                                shareText += "\nGenerato con ScoreCounter 🎮\n© 2026 Creato da Nicola" // Una piccola firma finale dell'app!

                                // 2. Prepariamo l'"Intent" (Il Messaggero Interno di Android)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText) // Inseriamo il nostro testo nel pacco
                                    type = "text/plain" // Diciamo ad Android che stiamo inviando testo crudo, non immagini
                                }

                                // 3. Facciamo apparire il menu nativo del telefono (Chiedi all'utente quale app social usare)
                                val shareIntent = Intent.createChooser(sendIntent, "Condividi classifica")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            // Inseriamo anche la nuova icona di Condivisione (Share) a sinistra del testo
                            Icon(Icons.Filled.Share, contentDescription = "Condividi", modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = "Condividi Risultati",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Aggiungiamo un po' di spazio tra i due bottoni
                        Spacer(modifier = Modifier.height(12.dp))

                        // IL PULSANTE SALVA E TORNA ALLA HOME
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // FUNZIONE SALVATAGGIO: Scriviamo la partita nel DataStore del ViewModel prima di sparire!
                                viewModel.saveCurrentMatch()
                                onNavigateHome() // Diamo ordine al NavController di distruggere questa pagina e portarci alla Home
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Salva e Torna alla Home",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

            ) { innerPadding ->
                // ---> MODIFICA IMPORTANTE DI LAYOUT: Ora la schermata base è diventata una LazyColumn <---
                // Lo facciamo perché con l'aggiunta del grafico in fondo, lo schermo diventa molto alto in verticale.
                // Con una Column statica, su telefoni piccoli parte della classifica finirebbe fuori dallo schermo senza possibilità di scorrerla giù!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // Centra le Card orizzontalmente
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Classifica Finale",
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Mostriamo il cronometro di gioco sotto il titolo dei risultati
                        if (viewModel.matchDurationSeconds > 0) {
                            Text(
                                text = "⏱️ Tempo di gioco: ${formatTime(viewModel.matchDurationSeconds)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            // Se non c'è il cronometro, usiamo uno Spacer per mantenere bilanciata la grafica
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Controlliamo preventivamente che la classifica non sia vuota, altrimenti crasherebbe cercando di leggere [0] (che non esiste)
                    if (rankedPlayers.isNotEmpty()) {

                        item {
                            // --- IL VINCITORE ---
                            // Il primo elemento della lista (ormai ordinata!) è indubbiamente il vincitore assoluto
                            val winner = rankedPlayers[0]

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                colors = CardDefaults.cardColors(
                                    // Diamo il colore 'primaryContainer' affinché la carta del vincitore risalti dorata/colorata rispetto allo sfondo grigio
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = "Vincitore",
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = "VINCITORE", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = winner.name,
                                        // Usiamo "displayMedium" che è un testo davvero gigantesco per fare impatto
                                        style = MaterialTheme.typography.displayMedium,
                                        // Il "fontWeight" modella il peso del font rendendolo Extra Grassetto
                                        fontWeight = FontWeight.ExtraBold,
                                        // Diamo al nome lo stesso colore del tema per farlo risaltare al massimo
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(text = "${winner.score} Punti", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        item {
                            // --- GLI ALTRI GIOCATORI ---
                            Text(
                                text = "Posizioni successive:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                textAlign = TextAlign.Start // Lo allinea a sinistra invece che al centro!
                            )
                        }

                        // Cicliamo il resto della classifica (creiamo le card arrotondate per il 2°, 3° posto ecc.)
                        itemsIndexed(rankedPlayers) { index, player ->
                            // Condizione IF geniale: "Salta la generazione grafica se l'indice è 0".
                            // In questo modo non stampiamo il Vincitore due volte (la prima l'abbiamo già disegnata in gigante qui sopra!)
                            if (index > 0) {
                                // Trasformiamo le vecchie righe spoglie in Card arrotondate moderne (Surface Variant)
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Mettiamo 'index + 1' perché gli array nella programmazione partono da 0,
                                        // ma la classifica umana parte logicamente dal 1° posto!
                                        Text(
                                            text = "${index + 1}° ${player.name}",
                                            // Testo un po' più grande (titleMedium invece di bodyLarge)
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${player.score} pt",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // ---> IL NUOVO GRAFICO (Game Stats) SPOSTATO ALLA FINE <---
                        // Come concordato, l'abbiamo messo come ultimo "item" della pagina per chiudere in bellezza!
                        // Ora non ci sono più controlli restrittivi anti-vuoto, lo mostriamo in modo sicuro.
                        item {
                            Spacer(modifier = Modifier.height(16.dp)) // Diamo respiro prima del grafico

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Andamento Partita",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    // Convertiamo i "Player" (attivi di questa partita) in "PlayerRecord" per darli in pasto al motore grafico
                                    val recordsForChart = viewModel.players.map { PlayerRecord(it.name, it.score, it.scoreHistory.toList()) }
                                    ScoreChart(
                                        players = recordsForChart,
                                        // ALTEZZA DEL GRAFICO FISSATA: lo rendiamo alto 200 pixel, bello spazioso.
                                        modifier = Modifier.fillMaxWidth().height(200.dp)
                                    )
                                }
                            }
                        }

                        // Spazio vuoto gigante inserito in fondo alla lista per non coprire mai la fine del grafico coi bottoni di salvataggio "Fluttuanti"
                        item { Spacer(modifier = Modifier.height(180.dp)) }
                    }
                }
            }

            // =========================================================
            // ESECUZIONE DELL'ANIMAZIONE CORIANDOLI (Sovrapposta in alto al Box)
            // Se la variabile è "true" (ed è vera appena si apre la pagina), scoppiano i coriandoli!
            if (showConfetti) {
                ConfettiExplosion(
                    // Forniamo alla funzione i colori "ufficiali" che stiamo usando nel nostro Material Theme per abbinare
                    colors = listOf(
                        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error
                    ),
                    onAnimationFinished = {
                        // Quando l'animazione ha finito i suoi calcoli matematici e scompare dallo schermo,
                        // settiamo la variabile a false così smette di disegnare. L'utente rimarrà comunque e leggerà questa pagina!
                        showConfetti = false
                    }
                )
            }
            // =========================================================
        }
    }

    // ====================================================================
    // IL MOTORE GRAFICO DEI CORIANDOLI
    // ====================================================================

    @Composable
    fun ConfettiExplosion(colors: List<Color>, onAnimationFinished: () -> Unit) {
        // Animatable: Il "timer/percentuale" dell'animazione. Parte da 0.0f (0%) e arriverà a 1.0f (100%).
        val animationProgress = remember { Animatable(0f) }

        // LaunchedEffect fa partire il codice interno asincrono solo UNA VOLTA appena la funzione appare sullo schermo del telefono.
        LaunchedEffect(Unit) {
            animationProgress.animateTo(
                targetValue = 1f, // L'obiettivo è arrivare a 1
                // tween: Stabilisce che ci vorranno esattamente 1200 millisecondi (1.2 secondi) di orologio reale.
                // FastOutSlowInEasing fa sì che l'animazione parta "col botto" scattante in modo realistico e poi rallenti dolcemente cadendo.
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
            // Quando la funzione animateTo ha finito di viaggiare verso l'1 (l'animazione è morta), diciamo all'app madre che abbiamo terminato!
            onAnimationFinished()
        }

        // Generazione della FISICA VETTORIALE dei coriandoli.
        // Il "remember" ci garantisce che generiamo i 60 pallini casuali solo una singola volta all'inizio della scena,
        // e non 60 volte al secondo per ogni frame video (cosa che distruggerebbe il processore fondendo il telefono)!
        val particles = remember {
            List(60) { // Creiamo 60 elementi (coriandoli fisici)
                // 1. Direzione (Angolo Geometrico): Calcoliamo un angolo casuale da 0 a 360°.
                // Nella matematica di Kotlin si usa il "Radiante" e non il grado centigrado. L'angolo giro completo (360°) equivale a 2 volte il Pi Greco.
                val angle = Random.nextDouble(0.0, 2 * Math.PI)

                // 2. Velocità Esplosiva: Una velocità sparata a caso tra 600 e 1800 per dare l'effetto di un'esplosione caotica e irregolare (non circolare perfetta).
                val speed = Random.nextFloat() * 1200f + 600f

                // 3. Colore Decorativo: Peschiamo un colore a caso dalla lista che ci hanno passato dai Material Colors poco fa!
                val color = colors.random()

                // Restituiamo in modo raggruppato una "Triple", ovvero un super-oggetto contenente queste tre specifiche caratteristiche vitali.
                Triple(angle, speed, color)
            }
        }

        // Canvas: La tela digitale trasparente (nuda e cruda) che occupa tutto l'intero schermo in primo piano, usata per il rendering super-veloce in 2D.
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Identifichiamo la coordinata del punto esatto centrale dello schermo, dividendone semplicemente larghezza e altezza a metà.
            val center = Offset(size.width / 2, size.height / 2)

            // Progress avanzerà continuamente nel tempo, frame dopo frame (0.1, 0.2, ... 1.0)
            val progress = animationProgress.value

            // Per OGNUNO dei 60 pallini memorizzati (coriandoli), disegniamo e aggiorniamo la sua posizione esatta in questo preciso millisecondo logico.
            particles.forEach { (angle, speed, color) ->
                // Distanza viaggiata dal centro = velocità di base moltiplicata per il tempo percentuale trascorso.
                val distance = speed * progress

                // Gravità Terrestre: Un numero finto che cresce in modo esponenziale per far "cadere" la Y del pallino verso il basso simulando il suo peso!
                val gravity = progress * progress * 800f

                // Calcolo effettivo della posizione (X e Y cartesiane):
                // - il Coseno trigonometrico di un angolo calcola la distanza e lo spostamento orizzontale (X)
                // - il Seno trigonometrico dell'angolo calcola la distanza logica verticale (Y) aggiungendo anche il peso in basso della gravità.
                val x = center.x + (cos(angle) * distance).toFloat()
                val y = center.y + (sin(angle) * distance).toFloat() + gravity

                // Trasparenza visiva o sfumatura (Alpha): 1.0 è solido e opaco, 0.0 è invisibile e trasparente come il vetro.
                // Sottraendo matematicamente 'progress' a 1, i coriandoli svaniranno gradualmente come fumo man mano che il tempo passa alla fine dell'esplosione.
                val alpha = (1f - progress).coerceIn(0f, 1f)

                // Finiti i calcoli, diciamo fisicamente alla tela in C++ (Canvas) di dipingere un cerchio solido con quelle esatte coordinate appena trovate.
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = 18f, // Raggio: La grandezza totale misurata in pixel fisici del nostro coriandolo
                    center = Offset(x, y)
                )
            }
        }
    }