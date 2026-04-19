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
    //Importo per l'animazione in stile carta rara nella sezione dei risultati finali
    import androidx.compose.animation.core.* // Importa tutti gli strumenti per le animazioni (tween, infiniteRepeatable, ecc.)
    import androidx.compose.ui.graphics.Brush // Il pennello per sfumare i colori
    import androidx.compose.ui.graphics.graphicsLayer//
    import androidx.compose.ui.draw.drawWithContent // Per disegnare la luce "sopra" alla Card
    // Import per i Bordi dei bottoni dinamici
    import androidx.compose.foundation.border
    import androidx.compose.foundation.BorderStroke
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
    // Import per i margini e gli spazi vuoti
    import androidx.compose.foundation.layout.PaddingValues
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
    // ---> IMPORT PER LA TASTIERA NUMERICA <---
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.ui.text.input.KeyboardType
    // Tutte le icone usate nell'app
    import androidx.compose.foundation.verticalScroll // Per far scorrere il testo del tutorial se lo schermo è piccolo
    import androidx.compose.material.icons.filled.Info // L'icona della "i" cerchiata per le informazioni
    import androidx.compose.material.icons.filled.Delete
    import androidx.compose.material.icons.filled.Edit
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material.icons.filled.Close // NUOVA ICONA: La 'X' per chiudere il BottomSheet
    import androidx.compose.material.icons.filled.EmojiEvents // Trofeo
    import androidx.compose.material.icons.filled.ExpandLess // Freccia in su per lo storico
    import androidx.compose.material.icons.filled.ExpandMore // Freccia in giù per lo storico
    import androidx.compose.material.icons.filled.Group // Icona del Gruppo per lo Stato Vuoto
    import androidx.compose.material.icons.filled.KeyboardArrowDown // Freccia giù per riordino
    import androidx.compose.material.icons.filled.KeyboardArrowUp   // Freccia su per riordino
    import androidx.compose.material.icons.filled.Person // Icona Utente/Contatto
    import androidx.compose.material.icons.filled.Remove
    import androidx.compose.material.icons.filled.Share // L'icona della condivisione
    import androidx.compose.material.icons.filled.Style // Icona delle Carte
    import androidx.compose.material.icons.filled.Tv // Icona TV per gli Anime
    import androidx.compose.material.icons.filled.VideogameAsset // Icona Controller
    import androidx.compose.material.icons.filled.WorkspacePremium // La medaglia/corona del Leader
    import androidx.compose.material.icons.filled.Timer // L'icona del cronometro
    import androidx.compose.material.icons.filled.Settings// Icona delle Impostazioni
    import androidx.compose.material.icons.filled.BarChart // Icona per le Statistiche Globali
    // Componenti grafici Material 3
    import androidx.compose.material3.AlertDialog
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults // Per cambiare colore ai bottoni in modo dinamico
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.ExperimentalMaterial3Api // NUOVO: Serve per attivare il BottomSheet sperimentale
    import androidx.compose.material3.ExtendedFloatingActionButton
    import androidx.compose.material3.HorizontalDivider
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.ModalBottomSheet // NUOVO: Il pannello a scorrimento dal basso
    import androidx.compose.material3.OutlinedButton
    import androidx.compose.material3.OutlinedTextField
    import androidx.compose.material3.rememberModalBottomSheetState // NUOVO: Ricorda in che stato è il pannello
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
    import androidx.compose.ui.text.style.TextOverflow // serve per mettere i puntini di "..." alla fine del testo nella sezione di Sfida in caso il numero sia enorme
    import androidx.compose.ui.unit.dp
    // Feedback tattile (Vibrazione)
    import androidx.compose.ui.hapticfeedback.HapticFeedbackType
    import androidx.compose.ui.platform.LocalHapticFeedback
    import androidx.compose.ui.platform.LocalContext // Serve per ottenere il "Contesto" della pagina
    import androidx.compose.foundation.layout.fillMaxHeight
    import androidx.compose.material.icons.filled.Casino
    import androidx.compose.material.icons.filled.Extension
    import androidx.compose.material.icons.filled.FlashOn
    import androidx.compose.material.icons.filled.SportsEsports
    import androidx.compose.material.icons.filled.Star
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
    import kotlin.math.cos
    import kotlin.math.sin
    import kotlin.random.Random

    // ====================================================================
    // COMPONENTE: SFONDO DECORATIVO GLOBALE
    // ====================================================================
        @Composable
        fun PatternedBackground() {
            // La lista delle nostre icone a tema (Giochi, Anime, Carte, Coppe, Timer, Medaglie)
        val icons = listOf(
            // Le tue originali
            Icons.Filled.VideogameAsset,   // Controller Classico
            Icons.Filled.Style,            // Carte
            Icons.Filled.Tv,               // Anime/Schermo
            Icons.Filled.EmojiEvents,      // Coppa del vincitore
            Icons.Filled.Timer,            // Cronometro
            Icons.Filled.WorkspacePremium, // Medaglia/Corona

            // ICONE TEMATICHE ESCLUSIVE PER LO SFONDO
            Icons.Filled.Casino,           // Dadi (Perfetto per i giochi da tavolo!)
            Icons.Filled.SportsEsports,    // Controller Moderno (Per tornei e console)
            Icons.Filled.Extension,        // Pezzo di Puzzle (Per giochi di strategia e logica)
            Icons.Filled.Star,             // Stella (Il classico simbolo dei punti)
            Icons.Filled.FlashOn,          // Fulmine (Richiama la combo "On Fire" e la velocità)
        )

            // ---> IL MOTORE CASUALE BLOCCATO <---
            // Creiamo una "mappa" bidimensionale fissa. Viene calcolata a caso la prima volta,
            // ma grazie a 'remember' lo schermo non sfarfallerà mai durante la partita!
            val randomGrid = remember {
                List(34) { // 34 righe
                    List(11) { // 11 colonne
                        icons.random() // Sceglie un'icona totalmente a caso per ogni singola cella
                    }
                }
            }
            // Un contenitore grande quanto tutto lo schermo
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {//mette 15 pixel di vuoto tra un'icona e l'altra in verticale.
                    // Disegniamo 33 righe per coprire anche gli schermi più lunghi
                    for (row in 0..33) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(43.dp),//mette 43 pixel di vuoto tra un'icona e l'altra in orizzontale.
                            // IL TRUCCO: Sfalsiamo leggermente le righe dispari per fare l'effetto "muro di mattoni" sfalsato
                            modifier = if (row % 2 == 0) Modifier else Modifier.padding(start = 36.dp)
                        ) {
                            for (col in 0..10) {val icon = randomGrid[row][col] // Andiamo a leggere l'icona salvata nella nostra mappa casuale

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    //Usiamo il colore del testo, ma con opacità al 5% (0.05f). Sarà un'ombra elegantissima!
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            }
                        }
                    }
                }
            }


    // ====================================================================
    // 2. INIZIALIZZAZIONE DEL DATABASE (DataStore) E FUNZIONI DI SUPPORTO
    // ====================================================================
    // LEZIONE: Questa riga crea un "collegamento" globale alla memoria fisica del telefono.
    val Context.dataStore by preferencesDataStore(name = "score_counter_prefs")

    // FUNZIONE DI SUPPORTO: Formatta i secondi in "Minuti:Secondi" (Es. 05:12)
    fun formatTime(totalSeconds: Long): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    // FUNZIONE DI SUPPORTO: Formatta i millisecondi in una Data (Es. 8 Nov 2026)
    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }

    // ====================================================================
    // 3. MAIN ACTIVITY (Il punto d'ingresso)
    // ====================================================================
    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContent {
                ScoreCounterTheme {

                    // ====================================================================
                    // ---> IL TRUCCO ARCHITETTURALE (Il Livello Base) <---
                    // ====================================================================
                    // In Jetpack Compose, il 'Box' serve a sovrapporre gli elementi l'uno sopra l'altro,
                    // come i livelli (layer) di Photoshop. Il primo elemento scritto sta SOTTO, l'ultimo sta SOPRA.
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    ) {

                        // LIVELLO 1 (Lo Sfondo): Dipingiamo il muro con la nostra griglia di icone giganti e trasparenti.
                        // Essendo il primo elemento del Box, resterà sempre bloccato in fondo.
                        PatternedBackground()

                        // INIZIALIZZAZIONE STRUMENTI:
                        // Creiamo il "Vigile Urbano" che sposta l'utente da una stanza all'altra
                        val navController = rememberNavController()
                        // Creiamo lo "Chef" (Il Cervello dell'app) che ricorda i dati
                        val matchViewModel: MatchViewModel = viewModel()

                        // LIVELLO 2 (Le Stanze): Sopra allo sfondo a icone, montiamo il NavHost.
                        // Il NavHost è il contenitore che carica la schermata giusta in base a dove stiamo andando.
                        NavHost(navController = navController, startDestination = "home") {

                            // ROTTA 1: La Home (Storico)
                            composable("home") {
                                HomeScreen(
                                    viewModel = matchViewModel, // <-- Passiamo correttamente il cervello!
                                    onNavigateToCreate = {
                                        matchViewModel.clearMatch()
                                        navController.navigate("create")
                                    },
                                    onNavigateToCounter = { navController.navigate("counter") },
                                    // ---> NUOVA ISTRUZIONE <---
                                    // Quando la HomeScreen chiama "onNavigateToStats", il Vigile Urbano la manda alla rotta "stats"
                                    onNavigateToStats = { navController.navigate("stats") }
                                )
                            }

                            // ROTTA 2: Creazione Partita
                            composable("create") {
                                CreateMatchScreen(
                                    viewModel = matchViewModel,
                                    onNavigateToCounter = { navController.navigate("counter") }
                                )
                            }

                            // ROTTA 3: Il Contatore (Campo di battaglia)
                            composable("counter") {
                                CounterScreen(
                                    viewModel = matchViewModel,
                                    onNavigateToResults = { navController.navigate("results") },
                                    onNavigateHome = {
                                        matchViewModel.clearMatch() // Pulisce i dati prima di scappare
                                        navController.popBackStack("home", inclusive = false) // Torna alla home
                                    }
                                )
                            }

                            // ROTTA 4: I Risultati (Podio)
                            composable("results") {
                                ResultsScreen(
                                    viewModel = matchViewModel,
                                    onNavigateHome = { navController.popBackStack("home", inclusive = false) }
                                )
                            }

                            // ==========================================================
                            // ---> ROTTA 5: IL LIBRO D'ORO (Statistiche Globali) <---
                            // ==========================================================
                            composable("stats") {
                                GlobalStatsScreen(
                                    viewModel = matchViewModel, // Diamo al Libro d'Oro l'accesso a tutta la memoria dell'app
                                    // Quando premiamo la X di chiusura, 'popBackStack' distrugge la schermata Stats e ci fa ricadere nella Home
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        /**
         * ====================================================================
         * SCHERMATA HOME: Mostra lo storico delle sfide salvate.
         * ====================================================================
         */
        @Composable
        fun HomeScreen(
            viewModel: MatchViewModel, // Il Cervello dell'app che contiene tutti i dati
            onNavigateToCreate: () -> Unit, // Comando per andare alla schermata "Nuova Partita"
            onNavigateToCounter: () -> Unit, // Comando per andare al Contatore (Serve se riprendiamo un backup)
            // ---> NUOVO PARAMETRO DI NAVIGAZIONE <---
            // Serve per aprire la porta del "Libro d'Oro" (Statistiche Globali)
            onNavigateToStats: () -> Unit
        ) {
            // Strumenti di sistema necessari per questa schermata
            val haptic = LocalHapticFeedback.current // Motore di vibrazione
            val context = LocalContext.current // Contesto dell'app (Serve per condividere su WhatsApp)
            val snackbarHostState = remember { SnackbarHostState() } // Gestore dei messaggi neri in basso ("Partita eliminata")
            val coroutineScope = rememberCoroutineScope() // Gestore dei processi in background (Serve per i timer)

            // ====================================================================
            // ---> LOGICA SALVA-VITA: IL POPUP DI BACKUP <---
            // ====================================================================
            // Se il ViewModel (Cervello) all'avvio ha trovato una partita non finita, showResumeMatchDialog sarà VERO.
            if (viewModel.showResumeMatchDialog) {
                AlertDialog(
                    // onDismissRequest scatta quando l'utente tocca fuori dal popup.
                    // Lasciando le graffe vuote { }, diciamo ad Android di NON fare nulla.
                    // Il popup rimane lì "bloccato" finché non si preme esplicitamente uno dei due bottoni.
                    onDismissRequest = { },
                    title = { Text("Partita in sospeso") },
                    text = { Text("Hai lasciato una sfida a metà.\nVuoi riprenderla da dove l'avevi lasciata?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.resumeBackupMatch() // 1. Ricostruisce i dati sul tavolo
                            onNavigateToCounter() // 2. Ci teletrasporta nella stanza del contatore
                        }) { Text("Riprendi") }
                    },
                    dismissButton = {
                        // Se l'utente non la vuole più, cancelliamo la memoria fantasma
                        TextButton(onClick = {
                            viewModel.clearBackup() // Cancella definitivamente il backup dal disco fisso
                            viewModel.showResumeMatchDialog = false // Chiude il popup
                        }) { Text("Cancella", color = MaterialTheme.colorScheme.error) }
                    }
                )
            }

            // ====================================================================
            // ---> LA STRUTTURA DELLA PAGINA (Scaffold) <---
            // ====================================================================
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                // Sfondo trasparente: FONDAMENTALE per permettere di vedere il "Muro a Icone" che abbiamo messo come Livello 1 nel Main!
                containerColor = Color.Transparent,
                // Agganciamo il gestore delle Snackbar alla struttura della pagina
                snackbarHost = { SnackbarHost(snackbarHostState) },
                // Il FAB (Floating Action Button): Il bottone gigante fluttuante in basso a destra
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToCreate() // Viaggia alla stanza per creare una partita
                        },
                        modifier = Modifier.padding(bottom = 32.dp, end = 8.dp),
                        icon = {
                            Icon(Icons.Filled.Add, contentDescription = "Nuova Sfida", modifier = Modifier.size(28.dp))
                        },
                        text = {
                            Text("Nuova Sfida", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    )
                }
            ) { innerPadding ->
                // innerPadding è lo spazio occupato dalle barre di sistema (batteria in alto, gesture in basso).
                // Diamo un padding globale a tutta la pagina per non far finire i testi sotto le barre.
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {

                    // ==============================================================
                    // ---> INTESTAZIONE HOME (Titolo + Tasto Statistiche) <---
                    // ==============================================================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        // Arrangement.SpaceBetween: Il segreto per spingere il testo a sinistra e l'icona a destra!
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically // Allinea le due cose esattamente al centro sulla riga
                    ) {
                        Text(
                            text = "Storico Sfide",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // ---> IL BOTTONE DEL LIBRO D'ORO <---
                        // Appare SOLO SE la lista dello storico nel cervello NON è vuota (isNotEmpty).
                        if (viewModel.history.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigateToStats() // Viaggia alla nuova stanza!
                                },
                                modifier = Modifier
                                    .size(40.dp) // Dimensioni ridotte per l'elegante bottone d'angolo
                                    // Sfondo circolare con colore 'primaryContainer' per farlo risaltare
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BarChart, // L'icona del grafico a barre
                                    contentDescription = "Statistiche Globali",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer, // Colore ottimizzato per il contrasto
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // ==============================================================
                    // ---> IL MOTORE DELLO STORICO (LA LISTA DELLE PARTITE) <---
                    // ==============================================================
                    if (viewModel.history.isEmpty()) {
                        // ---> STATO VUOTO (Empty State) <---
                        // Se hai appena scaricato l'app, mostra un semplice messaggio.
                        Text(
                            text = "Nessuna sfida salvata al momento.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // ---> LA LISTA INTELLIGENTE (LazyColumn) <---
                        // A differenza della Column normale, LazyColumn carica in memoria SOLO le carte che vedi in quel momento sullo schermo.
                        // Se hai 1000 partite nello storico, il telefono non esplode perché ne disegna solo 4 o 5 alla volta.
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Mette 12 pixel di spazio tra una Card e l'altra
                        ) {
                            // Cicliano tutte le partite salvate
                            items(viewModel.history) { record ->
                                // Variabile Spia Locale: Ogni carta ricorda se in questo momento è aperta (true) o chiusa (false)
                                var expanded by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Quando clicchi sulla carta, inverti lo stato (Se è chiusa si apre, se è aperta si chiude)
                                        expanded = !expanded
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {

                                        // --- PARTE SEMPRE VISIBILE: Titolo e Cronometro ---
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

                                            // Se la partita ha un tempo registrato, lo mostriamo in alto a destra
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

                                        // --- PARTE SEMPRE VISIBILE: Vincitore e Freccia a Scomparsa ---
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
                                            // Se la carta è aperta (expanded), disegna la freccia verso l'alto, altrimenti verso il basso.
                                            Icon(
                                                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // ==============================================================
                                        // ---> SEZIONE NASCOSTA: I DETTAGLI (Visibili solo se 'expanded' è VERO) <---
                                        // ==============================================================
                                        // AnimatedVisibility fa "srotolare" dolcemente il contenuto quando expanded diventa vero.
                                        AnimatedVisibility(visible = expanded) {
                                            Column(modifier = Modifier.padding(top = 20.dp)) {
                                                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                                                // 1. LA CLASSIFICA COMPLETA
                                                record.allPlayers.forEachIndexed { index, playerRecord ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Se l'indice è 0 (Prima posizione in array = 1° classificato)
                                                        if (index == 0) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                                                                Text(
                                                                    text = "1° ${playerRecord.name}",
                                                                    style = MaterialTheme.typography.titleLarge,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        } else {
                                                            // Tutti gli altri si stampano con testo normale
                                                            Text(
                                                                text = "${index + 1}° ${playerRecord.name}",
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                        }

                                                        // Il punteggio a destra
                                                        Text(
                                                            text = "${playerRecord.score} pt",
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }

                                                // 2. IL GRAFICO IN MINIATURA
                                                // Verifica che la partita abbia avuto uno svolgimento reale (più di 1 punto nella storia)
                                                val validHistory = record.allPlayers.any { (it.scoreHistory ?: emptyList()).size > 1 }
                                                if (validHistory) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text("Andamento Punteggi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                    // Disegna il grafico passandogli i dati di questo record specifico!
                                                    ScoreChart(
                                                        players = record.allPlayers,
                                                        modifier = Modifier.height(120.dp).fillMaxWidth().padding(top = 8.dp)
                                                    )
                                                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                                }

                                                // 3. LA DATA E LE AZIONI (Condividi e Elimina)
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
                                                        // ---> BOTTONE CONDIVIDI <---
                                                        IconButton(onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            // Costruisce la stringa con le statistiche della partita da inviare
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
                                                            // Lancia il menù di sistema di Android
                                                            val sendIntent = Intent().apply {
                                                                action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain"
                                                            }
                                                            context.startActivity(Intent.createChooser(sendIntent, "Condividi partita"))
                                                        }) {
                                                            Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.primary)
                                                        }

                                                        // ---> BOTTONE ELIMINA <---
                                                        IconButton(onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            // Ci salviamo la posizione prima di distruggerlo (serve per poterlo ripristinare!)
                                                            val removedIndex = viewModel.history.indexOf(record)
                                                            viewModel.deleteMatch(record)

                                                            // Lanciamo il processo in background (Coroutine) per la barra inferiore "Annulla"
                                                            coroutineScope.launch {
                                                                // Sotto-processo timer: Dopo 2.5 secondi, uccidi il messaggio
                                                                launch { delay(2500L); snackbarHostState.currentSnackbarData?.dismiss() }
                                                                // Mostriamo il messaggio e aspettiamo di vedere se preme il tasto
                                                                val result = snackbarHostState.showSnackbar("Partita eliminata", "ANNULLA", duration = SnackbarDuration.Indefinite)

                                                                // Se l'utente ha premuto "ANNULLA" in tempo, invochiamo la magia del ripristino
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

                            // ---> IL COPYRIGHT <---
                            // Essendo l'ultimo 'item' della LazyColumn, starà sempre in fondo a tutto
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
        }

        /**
         * SCHERMATA CREAZIONE SFIDA: Impostazioni iniziali e aggiunta giocatori.
         * ---> MODIFICHE: Implementato BottomSheet per gestione Preferiti <---
         */
        @OptIn(ExperimentalMaterial3Api::class) // Serve per indicare ad Android che stiamo usando il ModalBottomSheet (funzione avanzata)
        @Composable
        fun CreateMatchScreen(
            viewModel: MatchViewModel,
            onNavigateToCounter: () -> Unit
        ) {
            // Stato per il nome del giocatore in fase di digitazione
            var newPlayerName by remember { mutableStateOf("") }
            // Stato che ricorda QUALE giocatore stiamo modificando (per il popup).
            var playerToEdit by remember { mutableStateOf<Player?>(null) }

            // STATI PER I NOMI RAPIDI (Preferiti)
            var showFavoritesDialog by remember { mutableStateOf(false) }
            var favToEdit by remember { mutableStateOf<String?>(null) }

            // ---> LOGICA DI VALIDAZIONE (Prevenzione Errori) <---
            // 1. Controlla se il nome NON è vuoto e i giocatori NON sono zero
            val canStart = viewModel.matchTitle.isNotBlank() && viewModel.players.isNotEmpty()
            // 2. Ricorda se l'utente ha provato a cliccare "Inizia" facendo il furbo (senza aver messo i dati)
            var showError by remember { mutableStateOf(false) }

            // STATO PER IL POPUP DEL DADO PERSONALIZZATO
            var showDiceSettingsDialog by remember { mutableStateOf(false) }
            // Stato per gestire il testo scritto nel campo del dado personalizzato
            var customDiceInput by remember { mutableStateOf("") }

            // --->STATO DEL BOTTOM SHEET <---
            // Il "foglio che scorre dal basso" ha bisogno di una sua memoria per gestire le animazioni fluide di apertura/chiusura.
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

            val haptic = LocalHapticFeedback.current
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // IL BLOCCO: Se i requisiti ci sono, andiamo alla partita...
                            if (canStart) {
                                onNavigateToCounter()
                            } else {
                                // ...altrimenti, accendiamo la spia dell'errore visivo!
                                showError = true
                            }
                        },
                        modifier = Modifier.padding(bottom = 32.dp, end = 8.dp),
                        // IL COLORE: Se può partire è acceso (Primary), altrimenti è grigio scuro trasparente al 50%
                        containerColor = if (canStart) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        ),
                        // Anche il colore del testo e dell'icona sbiadisce se non si può partire
                        contentColor = if (canStart) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.5f
                        ),
                        icon = {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Inizia",
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        text = {
                            Text(
                                "Inizia Sfida",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            ) { innerPadding ->

                // Usiamo LazyColumn affinché la pagina sia scorrevole.
                // LEZIONE SOVRAPPOSIZIONE: Aggiungiamo 'innerPadding' per rispettare le barre di sistema.
                // Per evitare che il FAB (Pulsante Fluttuante) copra i contenuti, aggiungeremo uno Spacer
                // gigante alla fine della lista (riga 595).
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        // TITOLO PRINCIPALE
                        Text(
                            text = "Nuova Partita",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // ====================================================================
                        // ---> PRIMA CARD: LE REGOLE DEL GIOCO <---
                        // ====================================================================
                        // Raggruppa le impostazioni della partita in un riquadro per migliorare l'ordine visivo
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 12.dp),//24 è il padding tra le card
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {//
                                // INTESTAZIONE REGOLE CON INGRANAGGIO DADO
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Regole del Gioco",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Il bottone delle impostazioni per il Dado
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showDiceSettingsDialog = true // Apre il popup!
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Casino,
                                            contentDescription = "Impostazioni Dado",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // --- NOME DELLA PARTITA (Con validazione) ---
                                OutlinedTextField(
                                    value = viewModel.matchTitle,
                                    onValueChange = {
                                        viewModel.matchTitle = it
                                        // Se l'utente inizia a scrivere, spegniamo subito l'allarme rosso per premiarlo
                                        if (it.isNotBlank()) showError = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Nome della Sfida") },
                                    placeholder = { Text("Es. Sfida Epica") },
                                    shape = RoundedCornerShape(20.dp),
                                    // ---> LA MAGIA DEL BORDO ROSSO <---
                                    // Diventa rosso SOLO SE l'utente ha provato a cliccare start (showError) E il campo è ancora vuoto
                                    isError = showError && viewModel.matchTitle.isBlank(),
                                    // Se c'è un errore, facciamo apparire anche una micro-scritta rossa sotto al campo!
                                    supportingText = {
                                        if (showError && viewModel.matchTitle.isBlank()) {
                                            Text(
                                                "Il nome della sfida è obbligatorio",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    // L'icona del joypad per dare un feedback visivo immediato ("Affordance")
                                    leadingIcon = {
                                        // Se c'è un errore, anche l'icona diventa rossa, altrimenti resta del colore a tema
                                        val iconColor =
                                            if (showError && viewModel.matchTitle.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        Icon(
                                            Icons.Filled.VideogameAsset,
                                            contentDescription = null,
                                            tint = iconColor
                                        )
                                    }
                                )

                                // ---> LEZIONE: BOTTONI INTELLIGENTI <---
                                // Salviamo in due variabili se il testo attuale è ESATTAMENTE quello dei temi predefiniti.
                                val isAnimeTheme = viewModel.matchTitle == "Sfida Anime"
                                val isCarteTheme = viewModel.matchTitle == "Sfida Carte"

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(top = 2.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // BOTTONE TEMA ANIME
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.matchTitle = "Sfida Anime"
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        // Cambio Colore Dinamico: Se isAnimeTheme è vero, coloralo di blu (Primario).
                                        // Altrimenti fallo diventare trasparente (Outlined).
                                        colors = if (isAnimeTheme) ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                        else ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                        // Cambio Bordo Dinamico: Se è vero togliamo i bordi, se è falso disegniamo un contorno sottile.
                                        border = if (isAnimeTheme) null else BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.Tv,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text("Anime")
                                    }

                                    // BOTTONE TEMA CARTE
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.matchTitle = "Sfida Carte"
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = if (isCarteTheme) ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                        else ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                        border = if (isCarteTheme) null else BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.Style,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text("Carte")
                                    }
                                }

                                // --- PUNTEGGIO OBIETTIVO ---
                                OutlinedTextField(
                                    value = viewModel.targetScore,
                                    onValueChange = { newValue ->
                                        // Accettiamo solo testo vuoto o composto da numeri
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                            viewModel.targetScore = newValue
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    // LEZIONE TESTO: Ho accorciato la scritta da "Punteggio Obiettivo" a "Traguardo"
                                    // per evitare che, insieme all'icona, le parole vadano a capo rovinando l'estetica.
                                    label = { Text("Traguardo (Opzionale)") },
                                    placeholder = { Text("Es. 50") },
                                    shape = RoundedCornerShape(20.dp),
                                    // Forza l'apertura del tastierino numerico sul telefono
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            }
                        }

                        // ====================================================================
                        // ---> SECONDA CARD: AGGIUNTA E GESTIONE GIOCATORI (Ordine Invertito) <---
                        // ====================================================================
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Gestione Partecipanti",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )

                                // ==============================================================
                                // --- 1. GIOCATORI RAPIDI (PREFERITI) (Ora spostato in cima!) ---
                                // ==============================================================
                                // Abbiamo spostato questo blocco sopra per rendere più veloci le azioni comuni.
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Giocatori Rapidi:", style = MaterialTheme.typography.titleSmall)

                                    // ---> MODIFICA PULSANTE "GESTISCI" (Solo Icona) <---
                                    // Usiamo IconButton per avere un pulsante invisibile che contiene solo l'icona.
                                    // Questo rende l'interfaccia molto più pulita e minimalista.
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showFavoritesDialog = true // Apre il nuovo BottomSheet!
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            // Dato che abbiamo tolto il testo, è vitale mettere una descrizione per chi usa gli screen reader (Accessibilità)
                                            contentDescription = "Gestisci Giocatori Rapidi",
                                            // Coloriamo l'ingranaggio col colore primario per far capire che è un elemento interattivo
                                            tint = MaterialTheme.colorScheme.primary,
                                            // Lo ingrandiamo a 28.dp (prima era 16.dp) per renderlo facile da cliccare
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                }

                                // Se ci sono preferiti salvati, crea una lista orizzontale scorrevole
                                if (viewModel.favoriteNames.isNotEmpty()) {
                                    LazyRow(
                                        // Aumentiamo lo spazio tra un bottone e l'altro da 8 a 12
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        // ---> MODIFICA PEEK EFFECT (Capolino) <---
                                        // Aggiungiamo un padding interno a destra di 32.dp.
                                        // Questo inganna le dimensioni dello schermo: spinge i bottoni in modo irregolare,
                                        // forzando il quarto bottone a venire "tagliato a metà" dal bordo dello schermo!
                                        contentPadding = PaddingValues(end = 32.dp)
                                    ) {
                                        items(viewModel.favoriteNames) { fav ->
                                            // ---> CONTROLLO PRESENZA AL TAVOLO <---
                                            // Verifichiamo se questo preferito è già in partita
                                            val isAlreadyAtTable = viewModel.players.any { it.name.equals(fav, ignoreCase = true) }

                                            OutlinedButton(
                                                onClick = {
                                                    // Permette l'aggiunta e la vibrazione SOLO se non è già al tavolo
                                                    if (!isAlreadyAtTable) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.addPlayer(fav)
                                                    }
                                                },
                                                shape = RoundedCornerShape(20.dp),
                                                // Se è già al tavolo, sbiadisce il testo e il bordo per farlo sembrare "esaurito"
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = if (!isAlreadyAtTable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                ),
                                                border = BorderStroke(1.dp, if (!isAlreadyAtTable) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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

                                // ==============================================================
                                // --- 2. DIVISORE VISIVO ---
                                // ==============================================================
                                // Separa visivamente i preferiti dall'inserimento manuale per non creare confusione
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 1.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(5.dp))

                                // ==============================================================
                                // --- 3. INSERIMENTO NUOVO GIOCATORE (MANUALE) (Ora in fondo!) ---
                                // ==============================================================
                                Text("Aggiungi manualmente:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))

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
                                        leadingIcon = {
                                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    )

                                    // ---> LOGICA DEL BOTTONE "SPENTO" <---
                                    // Il bottone si "accende" SOLO SE: Il testo non è vuoto E il nome non esiste già al tavolo
                                    val isAddPlayerEnabled = newPlayerName.trim().isNotEmpty() && viewModel.players.none { it.name.equals(newPlayerName.trim(), ignoreCase = true) }

                                    Button(
                                        onClick = {
                                            if (isAddPlayerEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.addPlayer(newPlayerName)
                                                newPlayerName = "" // Svuota il campo dopo aver aggiunto
                                            }
                                        },
                                        modifier = Modifier.padding(top = 6.dp).height(56.dp),
                                        // ---> LOGICA DEL BORDO <---
                                        // Se il pulsante è spento, disegnamo un bordo sottile da 1.dp
                                        // con il colore 'outline' (grigio neutro) leggermente trasparente.
                                        border = if (isAddPlayerEnabled) null else BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = if (isAddPlayerEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) { Text("Aggiungi") }
                                }
                            }
                        }
                    }

                    // ====================================================================
                    // ---> TERZA CARD FISSA: IL "TAVOLO" (Empty State / Lista Giocatori) <---
                    // ====================================================================
                    // LEZIONE: Questa singola Card fa da contenitore generale. Se è vuota mostra
                    // l'icona gigante, se ci sono giocatori disegna le righe al suo interno.
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            // Colore leggermente diverso dalle altre due Card per distinguerla
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(

                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {

                                Text(
                                    text = "Giocatori al Tavolo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )

                                // SEZIONE CONDIZIONALE (IF / ELSE)
                                if (viewModel.players.isEmpty()) {
                                    // SE VUOTO: Disegniamo lo stato "Vuoto" con un'illustrazione centrale
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Group,
                                            contentDescription = "Vuoto",
                                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        Text(
                                            text = "Il tavolo è vuoto!",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.8f
                                            ),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Aggiungi qualcuno per iniziare la sfida.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                } else {
                                    // SE PIENO: Disegniamo la lista dei giocatori usando forEachIndexed
                                    // Nota: in una Card non possiamo usare 'items()', quindi usiamo un ciclo for.
                                    viewModel.players.forEachIndexed { index, player ->

                                        // Creiamo una piccola "Sotto-Card" bianca (Surface) per ogni riga giocatore
                                        Card(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // --- Frecce di Riordino ---
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            viewModel.movePlayer(index, index - 1)
                                                        },
                                                        enabled = index > 0,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.KeyboardArrowUp,
                                                            contentDescription = "Sposta su",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            viewModel.movePlayer(index, index + 1)
                                                        },
                                                        enabled = index < viewModel.players.size - 1,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.KeyboardArrowDown,
                                                            contentDescription = "Sposta giù",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                // --- Nome Giocatore ---
                                                Text(
                                                    text = player.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f)
                                                        .padding(start = 8.dp)
                                                )

                                                // --- Bottone Modifica ---
                                                IconButton(onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    playerToEdit = player
                                                }) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        contentDescription = "Modifica",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                // --- Bottone Elimina (Con Snackbar Annulla) ---
                                                IconButton(onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val removedIndex = index
                                                    val removedPlayer = player
                                                    viewModel.removePlayer(player)

                                                    coroutineScope.launch {
                                                        launch {
                                                            delay(2500L)
                                                            snackbarHostState.currentSnackbarData?.dismiss()
                                                        }
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "${player.name} rimosso",
                                                            actionLabel = "ANNULLA",
                                                            duration = SnackbarDuration.Indefinite
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restorePlayer(
                                                                removedIndex,
                                                                removedPlayer
                                                            )
                                                        }
                                                    }
                                                }) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "Elimina",
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

                    // ---> LEZIONE FAB FIX: IL GRANDE CUSCINETTO FINALE <---
                    // Questo blocco trasparente alto 120 pixel viene disegnato sempre in fondo alla pagina.
                    // Quando scrollerai la lista fino in fondo, questo spazio si infilerà SOTTO il bottone "Inizia Sfida",
                    // spingendo le Card verso l'alto ed evitando per sempre che vengano coperte!
                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }

            // === POPUP MODIFICA GIOCATORE IN PARTITA ===
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
                                playerToEdit = null
                            }
                        }) { Text("Salva") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            playerToEdit = null
                        }) { Text("Annulla") }
                    }
                )
            }

            // ====================================================================
            // ---> NUOVO: BOTTOM SHEET GESTIONE PREFERITI <---
            // ====================================================================
            // Questo sostituisce il vecchio AlertDialog. Scorre dolcemente dal basso
            // e si può chiudere con uno swipe verso il basso o cliccando la "X".
            if (showFavoritesDialog) {
                var newFavName by remember { mutableStateOf("") }

                ModalBottomSheet(//il "Pannello Inferiore Modale"
                    onDismissRequest = {
                        showFavoritesDialog = false
                    }, // Si attiva se tocchi lo sfondo scuro fuori dal pannello
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.93f) // <--- forza l'altezza del menù a tendina dei Giocatori rapidi al massimo al 93% di schermon
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp)
                    ) {


                        // INTESTAZIONE PANNELLO (Titolo + Bottone X)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Giocatori Rapidi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            // Bottone X per chiudere esplicitamente il pannello
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showFavoritesDialog = false
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                            }
                        }

                        // RIGA DI AGGIUNTA (Testo + Bottone "Aggiungi" affiancato)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newFavName,
                                onValueChange = { newFavName = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Nuovo nome") },
                                shape = RoundedCornerShape(16.dp)
                            )

                            // ---> LOGICA DEL BOTTONE "SPENTO" PER I PREFERITI <---
                            // Si accende solo se il testo non è vuoto e il nome non è già salvato tra i preferiti!
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
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.addFavorite(newFavName.trim())
                                        newFavName = ""
                                    }
                                },
                                modifier = Modifier.height(56.dp).padding(top = 6.dp),
                                // ---> LOGICA DEL BORDO ANCHE QUI <---
                                border = if (isAddFavEnabled) null else BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAddFavEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    ),
                                    contentColor = if (isAddFavEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            ) { Text("Aggiungi") }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()

                        // LISTA DEI NOMI SALVATI (Formato Card Compattato)
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            items(viewModel.favoriteNames) { fav ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    // Bordi leggermente meno arrotondati (12dp) rispetto al tavolo principale per risparmiare spazio
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        // Padding interno ridotto in altezza per far entrare più nomi nello schermo
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            fav,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            favToEdit = fav
                                        }) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Modifica",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.removeFavorite(fav)
                                        }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Elimina",
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

            // === POPUP MODIFICA SINGOLO PREFERITO ===
            // Questo popup (AlertDialog) compare "sopra" al pannello BottomSheet in modo perfettamente legale e pulito.
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
                    dismissButton = {
                        TextButton(onClick = {
                            favToEdit = null
                        }) { Text("Annulla") }
                    }
                )
            }

            // Stato per gestire il testo scritto nel campo del dado personalizzato (Deve stare fuori dal popup!)


            // ====================================================================
            // ---> POPUP SCELTA DADO (VERSIONE DEFINITIVA CON UX PROFESSIONALE) <---
            // ====================================================================
            // Questo blocco si attiva SOLO SE la variabile showDiceSettingsDialog diventa 'true' (quando clicchi l'ingranaggio)
            if (showDiceSettingsDialog) {

                // ---> 1. LA BOZZA TEMPORANEA (Pending State) <---
                // Questa variabile vive solo finché il popup è aperto.
                // Serve per l'UX "Conferma Esplicita": puoi cambiare idea quante volte vuoi,
                // ma il vero valore del dado cambierà solo quando premerai "Applica".
                var pendingDiceSides by remember { mutableIntStateOf(viewModel.diceSides) }

                AlertDialog(
                    // onDismissRequest scatta se l'utente tocca lo schermo fuori dal popup scuro.
                    // Invece di far crashare l'app, semplicemente chiude il popup buttando via la bozza.
                    onDismissRequest = { showDiceSettingsDialog = false },

                    // IL TITOLO IN ALTO
                    title = { Text("Seleziona il Dado 🎲", fontWeight = FontWeight.Bold) },

                    // IL CORPO CENTRALE DEL POPUP
                    text = {
                        // Column per impilare: Testo -> Griglia -> Divisore -> Input Manuale
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                            Text(
                                text = "Scegli un formato rapido o creane uno tuo:",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // --- PARTE 1: GRIGLIA DADI STANDARD ---
                            val diceOptions = listOf(6, 12, 20, 100)

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Spezziamo la lista in righe da 2 (es: [6, 12] e [20, 100])
                                diceOptions.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEach { sides ->

                                            // ---> LOGICA DI ILLUMINAZIONE INTELLIGENTE <---
                                            // La piastrella si accende SOLO SE corrisponde alla bozza (pendingDiceSides)
                                            // E SE l'utente NON sta scrivendo nulla a mano (customDiceInput.isEmpty()).
                                            // Questo evita che l'utente veda accesi contemporaneamente D20 e il campo "45".
                                            val isSelected = pendingDiceSides == sides && customDiceInput.isEmpty()

                                            Card(
                                                modifier = Modifier
                                                    .weight(1f) // Ogni card prende metà riga
                                                    .height(60.dp)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        // Aggiorniamo la bozza
                                                        pendingDiceSides = sides
                                                        // TRUCCHETTO UX: Se clicchi una piastrella, svuotiamo il campo di testo manuale!
                                                        customDiceInput = ""
                                                    },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text(
                                                        text = "D$sides",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Divisore estetico tra selezione rapida e manuale
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            // --- PARTE 2: INSERIMENTO MANUALE DIREtto ---
                            Text(text = "Inserimento manuale:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                            // Abbiamo rimosso la Row che conteneva il tasto "+".
                            // Ora il TextField è da solo e regna sovrano.
                            OutlinedTextField(
                                value = customDiceInput,
                                onValueChange = {
                                    // Accetta solo numeri
                                    if (it.all { char -> char.isDigit() }) customDiceInput = it
                                },
                                // fillMaxWidth() gli fa occupare tutta la larghezza dato che non c'è più il tasto "+"
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Es. 45") },
                                label = { Text("N° Facce") },
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    },

                    // ==========================================
                    // --- BOTTONI DI AZIONE GLOBALE (APPLICA E ANNULLA)
                    // ==========================================
                    // Svuotiamo il dismissButton e mettiamo TUTTO nel confirmButton
                    // usando una Row personalizzata per forzarli sulla stessa riga!
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Spazio in mezzo
                        ) {
                            // IL TASTO ANNULLA (A sinistra, si prende metà spazio)
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    customDiceInput = "" // Puliamo in caso di annullamento
                                    showDiceSettingsDialog = false
                                }
                            ) {
                                Text("Annulla", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            // IL TASTO APPLICA (A destra, si prende l'altra metà)
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                    // ---> MAGIA LOGICA: CHI VINCE? <---
                                    // Proviamo a convertire il testo in numero
                                    val manualSides = customDiceInput.toIntOrNull()

                                    // Se l'utente ha scritto un numero valido ed è maggiore di zero...
                                    if (manualSides != null && manualSides > 0) {
                                        // ...Vince l'inserimento manuale!
                                        viewModel.diceSides = manualSides
                                    } else {
                                        // ...Altrimenti, se il campo era vuoto, vince la piastrella selezionata (la bozza)!
                                        viewModel.diceSides = pendingDiceSides
                                    }

                                    customDiceInput = "" // Puliamo il campo per la prossima volta
                                    showDiceSettingsDialog = false // Chiudiamo il popup
                                }
                            ) {
                                // Testo pulito e senza puntini fastidiosi
                                Text("Applica")
                            }
                        }
                    },
                    // Cancelliamo del tutto il dismissButton standard, non ci serve più!
                    dismissButton = null
                )
            }

        }

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

            // ---> STATI PER IL DADO VIRTUALE <---
            var showDiceDialog by remember { mutableStateOf(false) }
            var diceResult by remember { mutableIntStateOf(1) }

            // --->Ricorda QUALE giocatore stiamo modificando manualmente con la tastiera <---
            // Se è "null", il popup per l'inserimento manuale è nascosto.
            var playerForManualEdit by remember { mutableStateOf<Player?>(null) }

            // STATO TUTORIAL: Controlla l'apertura del popup centrale con le regole nascoste (Manuale)
            var showInfoDialog by remember { mutableStateOf(false) }

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
                containerColor = Color.Transparent,

                // Montiamo la Snackbar in questa schermata per gli avvisi
                snackbarHost = { SnackbarHost(snackbarHostState) },
                // Usiamo la "bottomBar" (barra inferiore) dello Scaffold per i tasti principali d'azione
                bottomBar = {
                    Row(
                        // Modella la posizione: solleviamo la riga dal bordo inferiore (bottom=32.dp) e le diamo margini laterali.
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
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
                            Text(
                                text = "Azzera",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
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
                            Text(
                                text = "Fine Match",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {

                    // Intestazione con TITOLO/CRONOMETRO a sinistra e PULSANTE DADO a destra
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Raggruppo Titolo, Cronometro e Info in una Colonna per tenerli vicini
                        Column(modifier = Modifier.weight(1f)) {
                            // Titolo della sfida (Con salvagente se l'utente l'ha lasciato vuoto)
                            Text(
                                text = if (viewModel.matchTitle.isEmpty()) "Sfida" else viewModel.matchTitle,
                                // Usiamo l'esatto stile gigante della schermata Home e Nuova Partita
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold, // Grassetto massiccio
                                color = MaterialTheme.colorScheme.primary // Lo coloriamo a tema
                            )
                            // ---> CRONOMETRO DI PARTITA E INFO <---
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

                                // ---> SPOSTATO QUI: L'icona delle Informazioni <---
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showInfoDialog =
                                            true // Cambia lo stato a VERO e fa apparire il popup!
                                    },
                                    // Riduciamo la grandezza del bottone invisibile per non sformare la riga del cronometro
                                    modifier = Modifier.padding(start = 4.dp).size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = "Informazioni App",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp) // Icona leggermente più piccola per allinearsi al testo
                                    )
                                }
                            }
                        }

                        // ---> PULSANTE LANCIA DADO (Dinamico) <---
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // ---> LA MAGIA <---
                                // Invece di (1..6), usiamo (1..viewModel.diceSides).
                                // Se hai scelto il D20, calcolerà un numero casuale da 1 a 20!
                                diceResult = (1..viewModel.diceSides).random()
                                showDiceDialog = true
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            // Cambiamo il testo del bottone per mostrare SEMPRE quale dado stiamo usando (Es. "Lancia D20")
                            Text("Lancia D${viewModel.diceSides} 🎲")
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

                    // LazyColumn: La "lista intelligente" che renderizza graficamente solo i giocatori attualmente visibili sullo schermo
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // items() scorre la lista dei giocatori e per ognuno richiama la nostra funzione grafica "PlayerScoreCard"
                        items(viewModel.players) { p ->

                            // Capiamo se questo specifico giocatore merita la corona (Deve avere il maxScore e almeno 1 punto in attivo)
                            val isLeader = p.score == maxScore && maxScore > 0

                            // ---> MODIFICA COMBO: Passiamo la logica aggiornata alla Carta <---
                            PlayerScoreCard(
                                player = p,
                                isLeader = isLeader,
                                isFireEnabled = isFireEnabled, // Passiamo il verdetto del filtro anti-carte
                                onScoreChange = { amount ->
                                    viewModel.updatePlayerScore(
                                        p,
                                        amount
                                    )
                                }, // Usiamo la logica centralizzata del ViewModel per le combo!
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
                    onDismissRequest = {
                        playerForManualEdit = null
                    }, // Se l'utente clicca fuori, si chiude annullando
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

                            // ---> IL TRUCCO PER IL GRAFICO E LA COMBO <---
                            // Invece di dirgli "Il tuo nuovo punteggio è 50" (che romperebbe il grafico perché mancherebbe uno step),
                            // Calcoliamo la DIFFERENZA: (Nuovo Punteggio - Vecchio Punteggio).
                            // Es: Se aveva 10 e scrive 50, la differenza è +40.
                            val diff = newScore - playerForManualEdit!!.score

                            // Passiamo la differenza alla funzione updatePlayerScore!
                            // Così l'inserimento manuale da tastiera vale anche per scatenare (o rompere) la combo "On Fire"!
                            viewModel.updatePlayerScore(playerForManualEdit!!, diff)

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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
                            showExitWarning =
                                false // Falso allarme, l'utente chiude il popup e continua a giocare serenamente!
                        }) { Text("Annulla") }
                    }
                )
            }

            // ---> POPUP DEL DADO VIRTUALE <---
            if (showDiceDialog) {
                AlertDialog(
                    onDismissRequest = { showDiceDialog = false },
                    title = { Text("Lancio del Dado") },
                    text = {
                        Text(
                            text = "🎲 $diceResult",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        // Tasto per rullare di nuovo
                        Button(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // ---> FIX: Usiamo la variabile del ViewModel invece del numero fisso 6! <---
                            diceResult = (1..viewModel.diceSides).random()
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
                                "Tieni premuto il tasto '+' per aggiungere 10 punti o il tasto '-' per toglierne 5 istantaneamente.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                "🔥 Stato On Fire",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                            )
                            Text(
                                "Se un giocatore segna 3 volte di fila senza interruzioni da parte degli altri, il suo punteggio diventa arancione. \nNon si applica nelle sfide a carte",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                "⌨️ Modifica Manuale",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                            )
                            Text(
                                "Clicca direttamente sul numero del punteggio per aprire la tastiera e inserire un valore preciso a piacere.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                "🎲 Dado Fortunato",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                            )
                            Text(
                                "Usa il tasto 'Lancia Dado' per decidere chi inizia tra le dispute con amici",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showInfoDialog = false // Chiude il popup quando si preme il bottone
                        }) { Text("Ho capito") }
                    }
                )
            }
        }


        /**
         * COMPONENTE: Card personalizzata per la singola riga del giocatore nella fase di punteggio.
         * ---> MODIFICA UI: Stile "Gamepad", Numeri Giganti, Pulsanti Tattili, Bordi Marcati e FIX anti-schiacciamento (Ellipsis) <---
         * ---> NUOVA MODIFICA: Aggiunto Punteggio Arancione Soft (On Fire) e intercettazione logica Combo <---
         * * @OptIn(ExperimentalFoundationApi::class) serve perché stiamo usando 'combinedClickable',
         * una funzione avanzata di Compose che gestisce sia il tocco normale che la pressione lunga.
         */
        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun PlayerScoreCard(
            player: Player,
            isLeader: Boolean = false,
            // ---> NUOVI PARAMETRI PER LA COMBO ON FIRE <---
            isFireEnabled: Boolean = true, // Se falso (Sfida Carte), blocca il colore arancione
            onScoreChange: (Int) -> Unit,  // Il "Tubo" che invia l'azione (+1, -1, ecc.) al ViewModel per fargli contare la combo
            onScoreClick: () -> Unit
        ) {
            // MOTORE APTICO: Prepariamo il sistema di vibrazione del telefono per il feedback tattile
            val haptic = LocalHapticFeedback.current

            // ---> IL CONTENITORE PRINCIPALE (La riga del giocatore) <---
            // Card è un contenitore bellissimo del Material Design (sfondo leggero, bordi arrotondati e una leggera ombra invisibile)
            Card(
                // fillMaxWidth() gli fa occupare tutta la larghezza dello schermo.
                // padding(vertical = 4.dp) aggiunge una piccola spaziatura tra un giocatore e l'altro per non appiccicarli.
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                // ---> LEZIONE BORDI MARCATI <---
                // Usiamo BorderStroke per disegnare un contorno spesso 1 pixel.
                // Usiamo il colore 'primary' (quello principale del tuo tema), ma con '.copy(alpha = 0.3f)'
                // lo rendiamo trasparente al 30%. Questo crea un effetto "evidenziato" ma molto elegante e non invadente.
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                // Arrotondiamo pesantemente gli angoli (24.dp) per un look moderno in stile Material 3
                shape = RoundedCornerShape(24.dp)
            ) {
                // Row allinea gli elementi in orizzontale.
                // Arrangement.SpaceBetween spinge il Nome tutto a sinistra e i Pulsanti tutti a destra.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ==========================================================
                    // BLOCCO SINISTRO: NOME DEL GIOCATORE E CORONA DEL LEADER
                    // ==========================================================
                    // Raggruppo Nome e Corona in una riga interna per farli stare assieme a sinistra.
                    // weight(1f) è fondamentale qui: dice a questo blocco "prenditi tutto lo spazio vuoto che avanza".
                    // Così facendo, spinge prepotentemente il blocco dei pulsanti (a destra) contro il bordo del telefono.
                    // Aggiungiamo un padding 'end' per non far mai incollare il nome al bottone Meno nel caso di numeri giganti.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {

                        // ---> IL NOME DEL GIOCATORE <---
                        Text(
                            text = player.name,
                            // LEZIONE TIPOGRAFIA: 'headlineSmall' è uno stile di testo più grande e imponente del normale.
                            style = MaterialTheme.typography.headlineSmall,
                            // FontWeight.Bold forza la scritta in Grassetto per massimizzare la leggibilità durante il gioco.
                            fontWeight = FontWeight.Bold,
                            // ---> FIX ANTI-SCHIACCIAMENTO <---
                            // maxLines = 1: Forza il testo a rimanere sempre e solo su una singola riga, impedendo che la grafica si rompa in verticale.
                            maxLines = 1,
                            // overflow = TextOverflow.Ellipsis: Se il nome è troppo lungo e viene schiacciato dai numeri grandi, taglia le lettere finali e mette "..." (Es. "Dani...").
                            overflow = TextOverflow.Ellipsis,
                            // Diamo un 'weight' interno al testo per farlo restringere dolcemente se manca spazio, senza spingere fuori o schiacciare la corona del leader!
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // ---> DISEGNO DELLA CORONA DEL LEADER <---
                        // (Solo per chi è in vantaggio)
                        if (isLeader) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium, // L'icona a medaglia/stella molto in stile Material 3
                                contentDescription = "In Vantaggio",
                                tint = MaterialTheme.colorScheme.primary, // La stilizziamo col colore principale del tema
                                // Padding 'start' la stacca leggermente dal nome, 'size' la rende bella grande
                                modifier = Modifier.padding(start = 8.dp).size(28.dp)
                            )
                        }
                    }

                    // ==========================================================
                    // BLOCCO DESTRO: CONTROLLI STILE "GAMEPAD" E PUNTEGGIO
                    // ==========================================================
                    // Raggruppiamo i controlli matematici (Meno, Numero, Più) in un'altra mini-Row a destra
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // ---> PULSANTE MENO CON PUNTEGGIO RAPIDO (Long Press) <---
                        // Usiamo un 'Box' vuoto che poi "mascheriamo" da pulsante hardware.
                        Box(
                            modifier = Modifier
                                // Grandezza fissa di 56x56 pixel (Bersaglio touch molto grande per non mancarlo, Legge di Fitts)
                                .size(56.dp)
                                // LEZIONE FORME: Invece di un cerchio, usiamo un quadrato con angoli smussati a 16.dp.
                                // In UI Design questa forma si chiama "Squircle" e ricorda i tasti fisici dei joypad!
                                .clip(RoundedCornerShape(16.dp))
                                // Colore di sfondo tenue: usiamo 'errorContainer' (di solito un rosso slavato) con trasparenza al 70%
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                                // Disegniamo un bordino microscopico attorno al tasto per dargli un finto effetto 3D (Rilievo)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                )
                                // LEZIONE RIPPLE EFFECT: combinedClickable non solo gestisce i click, ma genera
                                // in automatico l'ombra grigia che si espande dal dito (L'onda tattile o Ripple Effect)!
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibra
                                        // MODIFICA COMBO: Usiamo onScoreChange(-1) invece del diretto player.changeScore(-1).
                                        // In questo modo avvisiamo il cervello dell'app (ViewModel) che deve spegnere il fuoco!
                                        onScoreChange(-1)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Pressione Lunga: Toglie 5 punti in un colpo solo e resetta le combo!
                                        onScoreChange(-5)
                                    }
                                ),
                            contentAlignment = Alignment.Center // Centra l'icona "-" perfettamente in mezzo al Box
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Diminuisci",
                                tint = MaterialTheme.colorScheme.onErrorContainer, // Colore scuro per fare contrasto col rosso tenue
                                modifier = Modifier.size(32.dp) // Icona ingrandita per riempire bene il tasto hardware
                            )
                        }

                        // ---> PUNTEGGIO GIGANTE CLICCABILE <---
                        Text(
                            text = player.score.toString(),
                            // LEZIONE TIPOGRAFIA: 'displayMedium' è uno degli stili più enormi di Android. Ideale per i numeri.
                            style = MaterialTheme.typography.displayMedium,
                            // FontWeight.Black è il livello massimo di grassetto esistente! Rende il font "cicciotto" e massiccio.
                            fontWeight = FontWeight.Black,
                            // ---> LOGICA COLORE ON FIRE 🔥 <---
                            // Se la variabile del giocatore 'isOnFire' è vera E la sfida non è a carte (isFireEnabled),
                            // coloriamo il numero di Arancione Soft. Altrimenti, usiamo il normale colore primario dell'app.
                            color = if (player.isOnFire && isFireEnabled) Color(0xFFF3AF38) else MaterialTheme.colorScheme.primary,
                            // Mettiamo maxLines = 1 anche qui. Se il numero diventa assurdamente lungo (es. 10 milioni), non andrà a capo rompendo la card.
                            maxLines = 1,
                            modifier = Modifier
                                // MODIFICA ANTI-SCHIACCIAMENTO: Imposto un padding a 16.dp. per evitare che il numero risulti schiacciato tra i pulsanti
                                // In questo modo i numeri hanno molto più spazio fisico per crescere prima di dare fastidio al nome del giocatore a sinistra!
                                .padding(horizontal = 16.dp)
                                // Usiamo 'modifier' per dirgli che ora non è più solo un testo da leggere, ma un bottone da cliccare!
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onScoreClick() // Esegue il comando di apertura del popup tastiera passato dalla CounterScreen
                                }
                        )

                        // ---> PULSANTE PIÙ CON PUNTEGGIO RAPIDO (Long Press) <---
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                // Sfondo tenue: usiamo 'primaryContainer' (un azzurro chiaro) per indicare positività
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // MODIFICA COMBO: Inviamo +1 al ViewModel. Se succede 3 volte di fila, scatta la Combo!
                                        onScoreChange(1)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Pressione lunga: Aggiunge 10 punti istantanei e conta per la Combo!
                                        onScoreChange(10)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Aumenta",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
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

            // ---> STATO ON FIRE: Determina se il punteggio deve diventare arancione.
            // Viene gestito dal ViewModel in base alla sequenza di punti.
            var isOnFire by mutableStateOf(false)

            // ---> Il pallottoliere delle combo attivate in QUESTA partita
            var fireComboCount by mutableIntStateOf(0)

            // LA MEMORIA STORICA PER IL GRAFICO
            val scoreHistory = mutableStateListOf<Int>(0)

            // Cambia il punteggio e aggiorna la cronologia per il grafico a linee.
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
            val scoreHistory: List<Int>? = emptyList(),
            val fireComboCount: Int = 0 // Serve per salvare il conteggio sul disco fisso
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

            // ---> La memoria del Dado <---
            // Di default è un classico dado a 6 facce (D6).
            var diceSides by mutableIntStateOf(6)

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

            //variabile per tenere il conto dei punti per la combo della tripletta di punti consecutivi
            private var lastScorer: Player? = null
            private var comboCount = 0

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
                    players = players.map {
                        PlayerRecord(
                            it.name,
                            it.score,
                            it.scoreHistory.toList()
                        )
                    },
                    seconds = matchDurationSeconds
                )
                viewModelScope.launch {
                    getApplication<Application>().dataStore.edit {
                        it[backupKey] = gson.toJson(backup)
                    }
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

            // Aggiunge un nuovo oggetto Player alla lista attiva della partita (Evitando i Cloni)
            fun addPlayer(name: String) {
                // 1. Pulizia: Togliamo eventuali spazi vuoti iniziali o finali inseriti per sbaglio (es. " Marco " diventa "Marco")
                val cleanName = name.trim()

                // 2. Controllo: Cerchiamo se nella lista c'è GIÀ qualcuno con questo esatto nome.
                // ignoreCase = true fa sì che "Marco" e "marco" vengano considerati la stessa identica persona!
                val alreadyExists = players.any { it.name.equals(cleanName, ignoreCase = true) }

                // 3. Esecuzione: Aggiungiamo il giocatore SOLO SE non è vuoto E non esiste già al tavolo.
                if (cleanName.isNotEmpty() && !alreadyExists) {
                    players.add(Player(cleanName))
                    saveBackup() // Salviamo subito il nuovo giocatore nel salva-vita
                }
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
                val cleanName = name.trim()

                // Controllo intelligente: verifichiamo se il nome esiste già ignorando le maiuscole
                val alreadyExists = favoriteNames.any { it.equals(cleanName, ignoreCase = true) }

                if (cleanName.isNotEmpty() && !alreadyExists) {
                    favoriteNames.add(cleanName) // Salviamo il nome "pulito"
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
                        favoriteNames[index] =
                            newName // Sostituzione diretta tramite indice della stringa
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

            /**
             * LOGICA COMBO ROVENTE:
             * Gestisce l'assegnazione punti controllando se sono consecutivi.
             * Se un altro giocatore segna, il contatore del precedente si azzera.
             */
            fun updatePlayerScore(player: Player, amount: Int) {
                if (amount > 0) {
                    // Se l'ultimo ad aver segnato è lo stesso di adesso...
                    if (lastScorer == player) {
                        comboCount++ // ...la striscia continua
                    } else {
                        // ...altrimenti, qualcuno ha interrotto la sequenza!
                        // Spegniamo il fuoco a tutti e ricominciamo il conteggio da 1 per il nuovo giocatore.
                        players.forEach { it.isOnFire = false }
                        lastScorer = player
                        comboCount = 1
                    }

                    // ---> LOGICA PIROMANE: Se arrivi ESATTAMENTE a 3 punti consecutivi...
                    if (comboCount == 3) {
                        player.isOnFire = true
                        // ...aggiungiamo +1 al suo record personale di questa partita!
                        player.fireComboCount++
                    } else if (comboCount > 3) {
                        // Se continua a segnare (4, 5, 6...), resta On Fire ma non contiamo combo extra
                        player.isOnFire = true
                    }
                } else {
                    players.forEach { it.isOnFire = false }
                    comboCount = 0
                    lastScorer = null
                }
                // Applichiamo la modifica al punteggio e salviamo il backup salva-vita
                player.changeScore(amount)
                saveBackup()
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
                    if (index < oldData.size) {
                        player.score = oldData[index].first // Ripristina i punti correnti
                        player.scoreHistory.clear() // Pulisce il grafico sbagliato
                        player.scoreHistory.addAll(oldData[index].second) // Ripristina tutta la storia originale del grafico!
                    }
                }
                saveBackup() // Ri-aggiorna il salva-vita
            }

            fun clearMatch() {
                diceSides = 6 //impostiamo come dado di default quello a 6 facce
                matchTitle = ""
                targetScore =
                    "" // Puliamo anche l'obiettivo di vittoria precedente per non portarcelo nelle sfide future
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
                            scoreHistory = activePlayer.scoreHistory.toList(),
                            // ---> IMPORTANTE: Passiamo il conteggio delle combo al database permanente
                            fireComboCount = activePlayer.fireComboCount
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
                                else path.lineTo(
                                    x,
                                    y
                                ) // Altri giri, struscia il pennarello verso le nuove coordinate (tirando la linea)

                                // Alla fine di ogni riga tracciata, disegniamo anche un pallino di giuntura per evidenziare il punto esatto!
                                drawCircle(color, 4.dp.toPx(), Offset(x, y))
                            }
                        }

                        // Eseguiamo il disegno definitivo della linea "Path" creata per questo giocatore.
                        // Usiamo StrokeCap.Round e StrokeJoin.Round per fare in modo che le linee e le curve siano morbide e non "spigolose"
                        drawPath(
                            path,
                            color,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(10.dp).clip(CircleShape)
                                    .background(lineColors[index % lineColors.size])
                            )
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }


        // ====================================================================
// LA SCHERMATA DELLA CLASSIFICA (RISULTATI) - VERSIONE ANIMATA
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
             */
            val rankedPlayers = viewModel.players.sortedByDescending { player -> player.score }

            val haptic = LocalHapticFeedback.current

            // Recuperiamo il Contesto per poter lanciare l'Intento di Condivisione
            val context = LocalContext.current

            // Appena entriamo in questa schermata, l'esplosione è VERA di default!
            // In questo modo, l'animazione partirà all'istante in cui compare la grafica.
            var showConfetti by remember { mutableStateOf(true) }

            // ======================================================
            // ---> LOGICA ANIMAZIONE A CASCATA (Staggered) <---
            // ======================================================
            // Creiamo un "interruttore" che parte su 'false' e diventa 'true' appena entriamo nella pagina.
            // Questo innesca tutte le animazioni di entrata simultaneamente.
            var startAnimation by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                startAnimation = true
            }

            // FUNZIONE DI SUPPORTO INTERNA: Crea l'effetto "comparsa e scivolamento"
            // Spiegazione: prende un 'indice' (la posizione dell'oggetto) e calcola un ritardo basato su di esso.
            @Composable
            fun staggeredModifier(index: Int): Modifier {
                // Animiamo la trasparenza (da 0 a 1)
                val alpha by animateFloatAsState(
                    targetValue = if (startAnimation) 1f else 0f,
                    // Ogni elemento aspetta 150ms moltiplicato per la sua posizione (0, 150, 300...) per creare la "cascata"
                    animationSpec = tween(
                        durationMillis = 1000,
                        delayMillis = index * 150,
                        easing = FastOutSlowInEasing
                    ),
                    label = "alpha"
                )
                // Animiamo la posizione verticale (scivola verso l'alto di 40 pixel)
                val translateY by animateFloatAsState(
                    targetValue = if (startAnimation) 0f else 40f,
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = index * 150,
                        easing = FastOutSlowInEasing
                    ),
                    label = "y"
                )

                // Modifier.graphicsLayer applica gli effetti calcolati sopra all'elemento finale senza far ricalcolare l'intera pagina ad Android
                return Modifier.graphicsLayer(alpha = alpha, translationY = translateY)
            }
            // ======================================================

            // Avvolgiamo lo Scaffold in un Box (Scatola). Il Box serve per sovrapporre il "livello"
            // dei coriandoli sopra il "livello" della classifica (lo Scaffold).
            Box(modifier = Modifier.fillMaxSize()) {

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    bottomBar = {
                        // LA CONDIVISIONE TESTUALE E IL SALVATAGGIO
                        // Usiamo una Column per impilare i bottoni.
                        // Applichiamo l'animazione a cascata anche a questi bottoni, dando loro un indice alto
                        // in modo che appaiano per ultimi, dopo che la classifica e il grafico sono stati disegnati.
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 32.dp
                            ).then(staggeredModifier(rankedPlayers.size + 3))
                        ) {

                            // IL PULSANTE DI CONDIVISIONE
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // 1. Costruiamo il testo magico che l'utente invierà su WhatsApp!
                                    val finalTitle =
                                        if (viewModel.matchTitle.isEmpty()) "Sfida Senza Nome" else viewModel.matchTitle
                                    var shareText = "🏆 Risultati: $finalTitle\n"
                                    // Mostriamo il cronometro nella condivisione
                                    if (viewModel.matchDurationSeconds > 0) shareText += "⏱️ Durata: ${
                                        formatTime(
                                            viewModel.matchDurationSeconds
                                        )
                                    }\n"
                                    // Aggiungiamo anche la data di oggi
                                    shareText += "📅 Data: ${formatDate(System.currentTimeMillis())}\n\n"

                                    // Cicliamo tutti i giocatori e aggiungiamo le medagliette testuali
                                    rankedPlayers.forEachIndexed { index, player ->
                                        val medal = when (index) {
                                            0 -> "🥇 1°"; 1 -> "🥈 2°"; 2 -> "🥉 3°"; else -> "${index + 1}°"
                                        }
                                        shareText += "$medal ${player.name} - ${player.score} pt\n"
                                    }
                                    shareText += "\nGenerato con ScoreCounter 🎮\n© 2026 Creato da Nicola" // Firma

                                    // 2. Prepariamo l'"Intent" (Il Messaggero Interno di Android)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND; putExtra(
                                        Intent.EXTRA_TEXT,
                                        shareText
                                    ); type = "text/plain"
                                    }
                                    // 3. Facciamo apparire il menu nativo del telefono
                                    context.startActivity(
                                        Intent.createChooser(
                                            sendIntent,
                                            "Condividi classifica"
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Condividi",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Condividi Risultati",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // IL PULSANTE SALVA E TORNA ALLA HOME
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // FUNZIONE SALVATAGGIO: Scriviamo la partita nel DataStore prima di sparire!
                                    viewModel.saveCurrentMatch()
                                    onNavigateHome()
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
                    // Ora la schermata base è una LazyColumn. Con l'aggiunta del grafico in fondo, lo schermo diventa molto alto.
                    // Con una Column statica, su telefoni piccoli parte della classifica finirebbe fuori dallo schermo.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            // --- INTESTAZIONE TITOLO E CRONOMETRO (Indice 0, appare per primo) ---
                            Column(
                                modifier = staggeredModifier(0),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (viewModel.matchTitle.isEmpty()) "Sfida" else viewModel.matchTitle,
                                    // Usiamo l'esatto stile gigante della schermata Home e Nuova Partita
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                // Mostriamo il cronometro di gioco sotto il titolo
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
                        }

                        // Controlliamo preventivamente che la classifica non sia vuota per evitare crash
                        if (rankedPlayers.isNotEmpty()) {
                            item {
                                // --- IL VINCITORE (Indice 1, appare per secondo) ---
                                // Il primo elemento della lista (ormai ordinata!) è indubbiamente il vincitore assoluto
                                val winner = rankedPlayers[0]

                                // ======================================================
                                // ---> EFFETTO CARTA RARA (Shimmer Sweep) <---
                                // ======================================================
                                // 1. IL MOTORE DELL'ANIMAZIONE: Crea un timer che va in loop continuo (infinito).
                                val infiniteTransition =
                                    rememberInfiniteTransition(label = "shimmer")

                                // 2. LA COORDINATA IN MOVIMENTO: Calcola un numero che viaggia da -500 a 2000 in 4.5 secondi.
                                val translateAnim by infiniteTransition.animateFloat(
                                    initialValue = -500f, // Parte da fuori lo schermo a sinistra
                                    targetValue = 2000f,  // Viaggia fino a fuori lo schermo a destra
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = 4500,
                                            easing = LinearEasing
                                        ),
                                        repeatMode = RepeatMode.Restart // Quando finisce, ricomincia da capo istantaneamente
                                    ),
                                    label = "shimmer_translation"
                                )

                                // 3. IL FASCIO DI LUCE (Pennello Gradiente):
                                // Sfuma dal trasparente, al bianco semitrasparente (il riflesso con opacità 0.25f), di nuovo al trasparente.
                                val shimmerBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.25f), // Opacità scelta dall'utente
                                        Color.Transparent
                                    ),
                                    // Colleghiamo inizio e fine del gradiente alle coordinate in movimento per far "scivolare" la luce in diagonale
                                    start = Offset(translateAnim, translateAnim),
                                    end = Offset(
                                        translateAnim + 400f,
                                        translateAnim + 400f
                                    ) // 400f è lo spessore logico del raggio
                                )
                                // ======================================================

                                Card(
                                    modifier = staggeredModifier(1) // Applica l'animazione di entrata a cascata
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp)
                                        // ---> APPLICAZIONE DELLA LUCE SULLA CARD <---
                                        // drawWithContent permette di disegnare strati sovrapposti:
                                        // prima disegna il contenuto normale della Card, poi ci "spennella" sopra la luce animata.
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(brush = shimmerBrush)
                                        },
                                    colors = CardDefaults.cardColors(
                                        // Diamo il colore 'primaryContainer' affinché la carta del vincitore risalti dorata/colorata
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Filled.EmojiEvents,
                                            "Vincitore",
                                            Modifier.padding(bottom = 8.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "VINCITORE",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = winner.name,
                                            // Usiamo "displayMedium" che è un testo davvero gigantesco per fare impatto
                                            style = MaterialTheme.typography.displayMedium,
                                            // Il "fontWeight" modella il peso del font rendendolo Extra Grassetto
                                            fontWeight = FontWeight.ExtraBold,
                                            // Regola del contrasto di Material 3: usiamo 'onPrimaryContainer' perché il testo
                                            // si trova sopra uno sfondo 'primaryContainer', garantendo la massima leggibilità.
                                            color = MaterialTheme.colorScheme.onPrimaryContainer // Contrasto ottimizzato
                                        )
                                        Text(
                                            text = "${winner.score} Punti",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }

                            item {
                                // --- TITOLO POSIZIONI (Indice 2, appare per terzo) ---
                                Text(
                                    text = "Posizioni successive:",
                                    style = MaterialTheme.typography.titleMedium,
                                    // ---> Forziamo un colore brillante a contrasto! <---
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = staggeredModifier(2).fillMaxWidth().padding(bottom = 8.dp),
                                    textAlign = TextAlign.Start
                                )
                            }

                            // --- ALTRI GIOCATORI (Indice 3 + la loro posizione) ---
                            // Cicliamo il resto della classifica per creare le card arrotondate per il 2°, 3° posto ecc.
                            itemsIndexed(rankedPlayers) { index, player ->
                                // Condizione IF geniale: "Salta la generazione se l'indice è 0" (il vincitore lo abbiamo già stampato)
                                if (index > 0) {
                                    Card(
                                        // staggeredModifier(index + 3) fa sì che il ritardo aumenti in base alla posizione in classifica
                                        modifier = staggeredModifier(index + 3)
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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

                            item {
                                // --- GRAFICO FINALE (Indice dimensione classifica + 4) ---
                                // Appare per ultimo dopo tutti i giocatori
                                Spacer(modifier = Modifier.height(16.dp)) // Diamo respiro prima del grafico
                                Card(
                                    modifier = staggeredModifier(rankedPlayers.size + 4)
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Andamento Partita",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                        // Convertiamo i "Player" in "PlayerRecord" per darli in pasto al motore grafico
                                        val recordsForChart = viewModel.players.map {
                                            PlayerRecord(
                                                it.name,
                                                it.score,
                                                it.scoreHistory.toList()
                                            )
                                        }
                                        // ALTEZZA DEL GRAFICO FISSATA: lo rendiamo alto 200 pixel, bello spazioso.
                                        ScoreChart(
                                            players = recordsForChart,
                                            modifier = Modifier.fillMaxWidth().height(200.dp)
                                        )
                                    }
                                }
                            }
                            // Spazio vuoto gigante inserito in fondo alla lista per non coprire mai la fine del grafico coi bottoni
                            item { Spacer(modifier = Modifier.height(180.dp)) }
                        }
                    }
                }

                // =========================================================
                // ESECUZIONE DELL'ANIMAZIONE CORIANDOLI (Sovrapposta in alto)
                // =========================================================
                // Se la variabile è "true" scoppiano i coriandoli!
                if (showConfetti) {
                    ConfettiExplosion(
                        // Forniamo i colori ufficiali del Material Theme
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.error
                        ),
                        onAnimationFinished = {
                            // Quando l'animazione ha finito, settiamo a false così smette di disegnare
                            showConfetti = false
                        }
                    )
                }
            }
        }

        /**
         * ====================================================================
         * SCHERMATA DELLE STATISTICHE GLOBALI (Data Analysis)
         * Questa schermata analizza l'intero database dello storico per estrarre curiosità e record.
         * ====================================================================
         */
        @Composable
        fun GlobalStatsScreen(
            viewModel: MatchViewModel,
            onNavigateBack: () -> Unit
        ) {
            val haptic = LocalHapticFeedback.current

            // ====================================================================
            // ---> LA LOGICA DEI CALCOLI (MATEMATICA DIETRO LE QUINTE) <---
            // Qui usiamo le funzioni "Collection" di Kotlin per analizzare l'intero Database (history).
            // ====================================================================

            // 1. PARTITE TOTALI: Semplicemente la dimensione (size) della lista dello storico.
            //.size' conta semplicemente quanti elementi (partite) ci sono nella lista dello storico.
            val totalMatches = viewModel.history.size

            // 2. TEMPO TOTALE GIOCATO:
            // '.sumOf' scorre automaticamente tutte le partite, prende la variabile 'durationSeconds'
            // di ognuna di esse e le somma tutte insieme in un colpo solo.
            val totalSeconds = viewModel.history.sumOf { it.durationSeconds }

            // 3. CAMPIONE ASSOLUTO (Chi ha vinto più partite in assoluto?):
            // Questo calcolo si fa in due passaggi.
            // PASSAGGIO A: groupingBy { it.winnerName }.eachCount()
            // Prende le partite, le raggruppa creando delle "scatole" col nome del vincitore e conta quante partite ci sono in ogni scatola.
            // Il risultato è una Mappa (Dizionario) fatta così -> ["Nicola": 5, "Marco": 2, "Anna": 8]
            val winsMap = viewModel.history.groupingBy { it.winnerName }.eachCount()
            // PASSAGGIO B: maxByOrNull { it.value }
            // Guarda dentro la Mappa appena creata, cerca il valore (.value) più alto (l'8 di Anna) e salva quell'elemento.
            // Se la lista è vuota, 'OrNull' evita che l'app crashi e restituisce semplicemente "niente".
            val bestPlayer = winsMap.maxByOrNull { it.value }

            // 4. RECORD DI PUNTI (Il punteggio più alto mai registrato da un vincitore):
            // maxByOrNull scansiona tutto lo storico e trova LA PARTITA in cui 'winningScore' era il più alto in assoluto.
            val highestScoreRecord = viewModel.history.maxByOrNull { it.winningScore }


            // 5. LA PARTITA INFINITA (La partita più lunga in assoluto)
            // 1. filter: Filtriamo lo storico tenendo SOLO le partite che hanno almeno 1 secondo (ignoriamo quelle finite subito).
            // 2. maxByOrNull: Tra queste, troviamo quella con il numero di 'durationSeconds' più alto in assoluto!
            val longestMatch = viewModel.history.filter { it.durationSeconds > 0 }.maxByOrNull { it.durationSeconds }


            // 6. IL DITTATORE (Vittoria col maggior distacco)
            // 1. Filtriamo le partite tenendo solo quelle che hanno almeno 2 giocatori.
            // 2. maxByOrNull calcola la differenza (distacco) tra il 1° e il 2° classificato e trova il valore più alto!
            val dictatorMatch = viewModel.history
                .filter { it.allPlayers.size >= 2 }
                .maxByOrNull { match ->
                    // Essendo la lista già ordinata per punteggio durante il salvataggio,
                    // il 1° è sempre allPlayers[0] e il 2° è sempre allPlayers[1]
                    match.allPlayers[0].score - match.allPlayers[1].score
                }

            // ====================================================================
            // ---> NOVITÀ: 7. IL PIROMANE (Più combo "On Fire" in assoluto) <---
            // ====================================================================
            // 1. Creiamo una lista piatta di TUTTI i giocatori che sono mai esistiti in ogni partita dello storico
            val allHistoricalPlayers = viewModel.history.flatMap { it.allPlayers }

            // 2. Raggruppiamo per nome e sommiamo tutti i loro 'fireComboCount'
            val fireStatsMap = allHistoricalPlayers
                .groupBy { it.name }
                .mapValues { entry -> entry.value.sumOf { it.fireComboCount } }

            // 3. Troviamo chi ha il totale più alto (Il Piromane Supremo)
            val topArsonist = fireStatsMap.maxByOrNull { it.value }

            // Calcoliamo e salviamo in memoria di quanti punti esatti è stato questo distacco record
            val dictatorMargin = if (dictatorMatch != null) {
                dictatorMatch.allPlayers[0].score - dictatorMatch.allPlayers[1].score
            } else 0



            Scaffold(
                modifier = Modifier.fillMaxSize(),
                // Usiamo il nostro solito trucco per far vedere il pattern di icone sullo sfondo!
                containerColor = Color.Transparent,

            ) { innerPadding ->

                // Usiamo una LazyColumn per permettere lo scorrimento se gli schermi sono piccoli.
                // Arrangement.spacedBy(16.dp) separa elegantemente le Card tra di loro.
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)//12 corrisponde allo spazio tra le carte
                ) {

                    // ==============================================================
                    // ---> INTESTAZIONE DELLA PAGINA (Ora è al sicuro sotto l'orologio!) <---
                    // ==============================================================
                    item {
                        Row() {

                            // Il tuo Titolo
                            Text(
                                text = "Statistiche partite",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }

                    // ==============================================================
                    // --- PRIMA CARD: I NUMERI GENERALI (Partite e Tempo) ---
                    // ==============================================================
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Riepilogo Generale", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(16.dp))

                                // Riga Partite Giocate (Icona + Testo a sx, Numero gigante a dx)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Style, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(" Partite Giocate:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    Text("$totalMatches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }

                                // Divisore sottile tra le due statistiche
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                // Riga Tempo Speso sul campo
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(" Tempo sul campo:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    // Ricicliamo la nostra utilissima funzione 'formatTime' per trasformare i secondi grezzi in "05:12"
                                    Text(formatTime(totalSeconds), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // ==============================================================
                    // --- SECONDA CARD: IL CAMPIONE ASSOLUTO (Chi vince di più) ---
                    // ==============================================================
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            // Colore speciale: Usiamo il primaryContainer per farla risaltare e darle un effetto "Oro/Premio"
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally // Centra tutto perfettamente
                            ) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Text("CAMPIONE ASSOLUTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.padding(top = 8.dp))

                                // Stampiamo il NOME del giocatore con più vittorie.
                                // '?.' è una protezione: se 'bestPlayer' è nullo (nessuno ha mai giocato), stampa "Nessuno".
                                Text(
                                    text = bestPlayer?.key ?: "Nessuno",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                // Stampiamo il NUMERO di vittorie di quel giocatore.
                                Text(
                                    text = "Con ${bestPlayer?.value ?: 0} vittorie totali",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // ==============================================================
                    // --- TERZA CARD: IL RECORD DI PUNTI (La partita migliore) ---
                    // ==============================================================
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Record di Punti (Singola Partita)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(16.dp))

                                // Se esiste almeno una partita nel record...
                                if (highestScoreRecord != null) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        // A sinistra: Chi ha fatto il record e in che partita lo ha fatto
                                        Column {
                                            Text(text = highestScoreRecord.winnerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                            Text(text = "in '${highestScoreRecord.title}'", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // A destra: Il numero di punti gigante
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(text = "${highestScoreRecord.winningScore}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                            Text(" pt", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                                        }
                                    }
                                } else {
                                    // Se lo storico è completamente vuoto, mostra un messaggio di fallback
                                    Text("Ancora nessun record stabilito.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // ==============================================================
                    // ---> QUARTA CARD: LA PARTITA INFINITA (La più lunga) <---
                    // ==============================================================
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("La Partita Infinita", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Se abbiamo trovato una partita che è durata almeno 1 secondo...
                                if (longestMatch != null) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        // A sinistra: Il nome della partita e chi l'ha vinta
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(text = longestMatch.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(text = "Vinta da ${longestMatch.winnerName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // A destra: L'icona del cronometro e il tempo gigante!
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp))
                                            Text(text = formatTime(longestMatch.durationSeconds), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                } else {
                                    Text("Nessuna partita cronometrata.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // ==============================================================
                    // ---> QUINTA CARD: IL DITTATORE (Maggior Distacco) <---
                    // ==============================================================
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Il Dittatore (Vittoria Schiacciante)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Se abbiamo trovato una partita e il distacco è maggiore di 0...
                                if (dictatorMatch != null && dictatorMargin > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                                        // A sinistra: Chi è il dittatore e in che partita ha dominato
                                        // ---> FIX TESTO TAGLIATO <---
                                        // Lasciamo il weight(1f) per dargli la priorità di spazio, ma RIMUOVIAMO i blocchi "maxLines" e "overflow"
                                        // dalla scritta inferiore, così se è troppo lunga andrà dolcemente a capo su due righe!
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {//padding destro a 12.dp per staccarlo bene dai numeri
                                            Text(
                                                text = dictatorMatch.winnerName,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "ha dominato in '${dictatorMatch.title}'",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                // Rimossi maxLines e overflow qui! Ora respira!
                                            )
                                        }

                                        // A destra: Il numero di punti di scarto gigante
                                        // Usiamo Alignment.End per allineare i numeri a destra come una vera colonna
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "+$dictatorMargin pt",
                                                style = MaterialTheme.typography.displaySmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "dal 2° posto",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    // Se tutte le partite sono state dei pareggi (o si è giocato solo da soli)
                                    Text("Nessun dominio registrato. Le partite sono state molto equilibrate!", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // ==============================================================
                    // ---> SESTA CARD: IL PIROMANE (Combo On Fire) <---
                    // ==============================================================
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Il Piromane 🔥", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Se esiste qualcuno che è andato "a fuoco" almeno una volta...
                                if (topArsonist != null && topArsonist.value > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(text = topArsonist.key, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                            Text(text = "È il giocatore più combo fatte in una partita", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "${topArsonist.value}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color(0xFFF3AF38))
                                            Text(text = "volte On Fire", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    Text("Nessuno ha ancora scatenato l'inferno (3 punti di fila).", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // ---> LEZIONE FAB FIX <---
                    // Cuscinetto finale per non incollare l'ultima card in fondo allo schermo,
                    // permettendo uno scorrimento piacevole fino in fondo.
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
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
    }