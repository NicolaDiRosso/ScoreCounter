package com.n380.scorecounter

// ====================================================================
// 1. AREA IMPORTAZIONI (Solo quelle necessarie per l'avvio!)
// ====================================================================
import android.app.LocaleManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.n380.scorecounter.ui.screens.SettingsScreen
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
                    // 🧠 SINCRONIZZAZIONE REATTIVA: BLOCCO ORIENTAMENTO SCHERMO
                    // ====================================================================
                    // LaunchedEffect osserva la proprietà reattiva 'matchViewModel.appOrientation'.
                    // Ogni volta che l'utente seleziona una voce nella schermata Impostazioni:
                    // - Valore 0: Auto -> SCREEN_ORIENTATION_UNSPECIFIED (segue i sensori del telefono e le rotazioni dell'utente).
                    // - Valore 1: Orizzontale -> SCREEN_ORIENTATION_SENSOR_LANDSCAPE (forza la modalità Landscape orizzontale).
                    // - Valore 2: Verticale -> SCREEN_ORIENTATION_USER_PORTRAIT (forza la modalità Portrait verticale).
                    // Essendo eseguito a livello di Activity, il cambio di orientamento si applica
                    // istantaneamente all'intero hardware del display senza ritardi né sfarfallii.
                    androidx.compose.runtime.LaunchedEffect(matchViewModel.appOrientation) {
                        requestedOrientation = when (matchViewModel.appOrientation) {
                            1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            2 -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }

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

                    // LIVELLO 2 (Le Stanze): Il NavHost gestisce il grafo di navigazione con animazioni predittive (Predictive Back).
                    // 🧠 INTEGRAZIONE PREDICTIVE BACK GESTURE (ANDROID 14+ / API 34+):
                    // Grazie all'attributo `android:enableOnBackInvokedCallback="true"` nel Manifest e alle transizioni
                    // configurate qui sotto, il NavHost di Jetpack Compose si collega al `OnBackInvokedDispatcher` di sistema.
                    // Quando l'utente esegue lo swipe "Indietro" dal bordo del display:
                    // 1. Android OS cattura e mostra in anteprima la schermata di destinazione sottostante in tempo reale.
                    // 2. La schermata corrente risponde traslando verso destra (popExitTransition) in coordinazione col movimento del dito.
                    // 3. Se il gesto viene rilasciato, la navigazione si completa; se viene annullato, la schermata torna al 100%.
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            // Avanzamento verso nuova schermata: scivola da destra a sinistra (+fullWidth -> 0)
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            // Uscita verso nuova schermata: la schermata attuale scivola a sinistra (0 -> -fullWidth/3)
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        },
                        popEnterTransition = {
                            // Predictive Back Rientro: la schermata precedente ricompare da sinistra (-fullWidth/3 -> 0)
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            // Predictive Back Uscita: la schermata corrente scivola a destra (0 -> +fullWidth) svelando l'anteprima
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    ) {

                        // ROTTA 1: La Home (Storico)
                        composable("home") {
                            HomeScreen(
                                viewModel = matchViewModel,
                                onNavigateToCreate = {
                                    matchViewModel.clearMatch()
                                    navController.navigate("create")
                                },
                                onNavigateToCounter = { navController.navigate("counter") },
                                onNavigateToStats = { navController.navigate("stats") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        // ROTTA 2: Creazione Partita
                        composable("create") {
                            CreateMatchScreen(
                                viewModel = matchViewModel,
                                onNavigateToCounter = { navController.navigate("counter") }
                            )
                        }

                        // ROTTA 3: Il Contatore della Partita
                        composable("counter") {
                            CounterScreen(
                                viewModel = matchViewModel,
                                onNavigateToResults = {
                                    // IRREVERSIBILITÀ DELLA NAVIGAZIONE:
                                    // Navighiamo verso i risultati, ma diciamo al NavController di
                                    // distruggere tutte le schermate intermedie fino alla Home.
                                    // In questo modo la gesture "Indietro" salterà il Counter!
                                    navController.navigate("results") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onNavigateHome = {
                                    matchViewModel.clearMatch()
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
                        }

                        // ROTTA 4: I Risultati (Podio Read-Only)
                        composable("results") {
                            ResultsScreen(
                                viewModel = matchViewModel,
                                onNavigateHome = {
                                    // 🧹 PULIZIA MEMORIA
                                    // Dato che la partita è già salvata, quando l'utente lascia
                                    // i risultati per tornare alla Home, ripuliamo il ViewModel
                                    // in modo che sia vergine e pronto per la prossima sfida!
                                    matchViewModel.clearMatch()

                                    navController.popBackStack(
                                        "home",
                                        inclusive = false
                                    )
                                }
                            )
                            // 🛡️ DATA LOSS PREVENTION (Hardware Back Button / Predictive Back Gesture)
                            // 🧠 INTEGRITA' DATI: Se l'utente nei Risultati esegue la gesture "Indietro" predittiva,
                            // l'evento OnBackInvoked intercetta il completamento del gesto per ripulire la RAM nel
                            // ViewModel prima di rientrare alla schermata Home.
                            BackHandler {
                                matchViewModel.clearMatch()
                                navController.popBackStack("home", inclusive = false)
                            }
                        }

                        // ROTTA 5: Statistiche Globali
                        composable("stats") {
                            GlobalStatsScreen(
                                viewModel = matchViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // ROTTA 6: Impostazioni e Preferenze
                        composable("settings") {
                            SettingsScreen(
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