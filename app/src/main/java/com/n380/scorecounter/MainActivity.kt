package com.n380.scorecounter

// ====================================================================
// 1. AREA IMPORTAZIONI (Solo quelle necessarie per l'avvio!)
// ====================================================================
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.n380.scorecounter.ui.components.PatternedBackground
import com.n380.scorecounter.ui.screens.CounterScreen
import com.n380.scorecounter.ui.screens.CreateMatchScreen
import com.n380.scorecounter.ui.screens.GlobalStatsScreen
import com.n380.scorecounter.ui.screens.HomeScreen
import com.n380.scorecounter.ui.screens.ResultsScreen
import com.n380.scorecounter.ui.theme.ScoreCounterTheme
import com.n380.scorecounter.viewmodel.MatchViewModel


import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ====================================================================
// 2. INIZIALIZZAZIONE DEL DATABASE (DataStore) E FUNZIONI DI SUPPORTO
// ====================================================================
// LEZIONE: Questa riga crea un "collegamento" globale alla memoria fisica del telefono.
val Context.dataStore by preferencesDataStore(name = "score_counter_prefs")

// ====================================================================
// 3. MAIN ACTIVITY (Il punto d'ingresso e Vigile Urbano)
// ====================================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScoreCounterTheme {

                // ====================================================================
                // ---> Il Livello Base <---
                // ====================================================================
                // In Jetpack Compose, il 'Box' serve a sovrapporre gli elementi l'uno sopra l'altro,
                // come i livelli (layer) di Photoshop. Il primo elemento scritto sta SOTTO, l'ultimo sta SOPRA.
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {

                    // LIVELLO 1 (Lo Sfondo): Dipingiamo il muro con la nostra griglia di icone giganti e trasparenti.
                    PatternedBackground()

                    // INIZIALIZZAZIONE STRUMENTI:
                    // Creiamo il "Vigile Urbano" che sposta l'utente da una stanza all'altra
                    val navController = rememberNavController()
                    // Creiamo lo Il Cervello dell'app che ricorda i dati
                    val matchViewModel: MatchViewModel = viewModel()

                    // ====================================================================
                    // CONTROLLO DI SICUREZZA PER LA ROTAZIONE DELLO SCHERMO
                    // ====================================================================
                    // In Android, se l'utente gira il telefono di lato, la MainActivity viene
                    // distrutta e ricreata. Usiamo 'rememberSaveable' per creare una variabile
                    // booleana (true/false) che sopravvive alla rotazione. Ci serve per ricordare
                    // se abbiamo gia' eseguito il salto dal widget, evitando che l'app continui
                    // a riaprire la schermata 'create' da sola ogni volta che si gira lo schermo.
                    var widgetNavigated by rememberSaveable {
                        androidx.compose.runtime.mutableStateOf(false)
                    }

                    // ====================================================================
                    // GESTIONE DEL REINDIRIZZAMENTO AUTOMATICO (WIDGET LAUNCH)
                    // ====================================================================
                    // 'LaunchedEffect' e' un componente speciale di Compose che esegue il codice
                    // al suo interno una sola volta quando la schermata viene preparata.
                    androidx.compose.runtime.LaunchedEffect(intent?.action) {
                        // Controlliamo se l'azione dell'intent corrisponde alla parola d'ordine del widget
                        // e ci assicuriamo (tramite la variabile sopra) di non averlo gia' fatto.
                        if (intent?.action == "ACTION_NEW_MATCH" && !widgetNavigated) {
                            // 1. Puliamo la memoria da eventuali partite rimaste in sospeso
                            matchViewModel.clearMatch()
                            // 2. Ordiniamo al navController di fare immediatamente un passo avanti verso la creazione
                            navController.navigate("create")
                            // 3. Segnamo come "fatto" il passaggio, cosi' blocchiamo futuri loop alla rotazione
                            widgetNavigated = true
                        }
                    }

                    // LIVELLO 2 (Le Stanze): Il NavHost torna ad avere come 'startDestination' fissa la "home".
                    // Questo garantisce che la Home sia sempre il pavimento della nostra pila di navigazione.
                    NavHost(navController = navController, startDestination = "home") {

                        // ROTTA 1: La Home (Storico)
                        composable("home") {
                            HomeScreen(
                                viewModel = matchViewModel,
                                onNavigateToCreate = {
                                    matchViewModel.clearMatch()
                                    navController.navigate("create")
                                },
                                onNavigateToCounter = { navController.navigate("counter") },
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
                                    matchViewModel.clearMatch()
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
                        }

                        // ROTTA 4: I Risultati (Podio)
                        composable("results") {
                            ResultsScreen(
                                viewModel = matchViewModel,
                                onNavigateHome = {
                                    navController.popBackStack(
                                        "home",
                                        inclusive = false
                                    )
                                }
                            )
                        }

                        // ROTTA 5: Statistiche Globali
                        composable("stats") {
                            GlobalStatsScreen(
                                viewModel = matchViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}