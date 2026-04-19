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
import com.n380.scorecounter.ui.theme.ScoreCounterTheme

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
                // ---> IL TRUCCO ARCHITETTURALE (Il Livello Base) <---
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
                    // Creiamo lo "Chef" (Il Cervello dell'app) che ricorda i dati
                    val matchViewModel: MatchViewModel = viewModel()

                    // LIVELLO 2 (Le Stanze): Sopra allo sfondo a icone, montiamo il NavHost.
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
                                onNavigateHome = { navController.popBackStack("home", inclusive = false) }
                            )
                        }

                        // ROTTA 5: IL LIBRO D'ORO (Statistiche Globali)
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