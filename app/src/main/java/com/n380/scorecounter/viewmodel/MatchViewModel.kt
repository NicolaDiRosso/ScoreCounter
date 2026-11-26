package com.n380.scorecounter.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.n380.scorecounter.dataStore
import com.n380.scorecounter.model.FavoritePlayer
import com.n380.scorecounter.model.MatchBackup
import com.n380.scorecounter.model.MatchRecord
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.model.PlayerRecord
import com.n380.scorecounter.model.ScoreAction
import com.n380.scorecounter.model.ScoreActionRecord
import com.n380.scorecounter.model.ScoreCounterBackup
import com.n380.scorecounter.ui.components.BackgroundStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

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

    // Criterio di vittoria della partita in corso: false = Vince il punteggio più alto (default), true = Vince il punteggio più basso
    var isLowestScoreWins by mutableStateOf(false)

    // 🧠 LOGICA DI VALIDAZIONE: FINE PARTITA
    // Questa proprietà calcolata controlla se il pulsante "Fine Partita" deve essere attivo.
    // Restituisce true se ALMENO UN giocatore ha una cronologia punteggi superiore a 1.
    // Nota: 'scoreHistory' nasce con un solo valore (lo zero iniziale). Se la dimensione è > 1,
    // significa che qualcuno ha segnato un punto (positivo o negativo) o che la partita non è vergine.
    val canEndMatch: Boolean
        get() = players.any { it.scoreHistory.size > 1 }

    // 🧠 CONTROLLO DATI SALVATI LOCALI
    // Questa proprietà controlla se l'applicazione contiene già dei dati salvati in memoria
    // (partite concluse nello storico, giocatori salvati nei preferiti, titoli personalizzati o preset timer modificati).
    // Usata per mostrare un avviso prima di sovrascrivere tutto durante l'importazione.
    val hasSavedData: Boolean
        get() = history.isNotEmpty() || favoriteNames.isNotEmpty() || matchTitleHistory.isNotEmpty() || timerPresets.toList() != defaultTimerPresets

    val players = mutableStateListOf<Player>()
    val history = mutableStateListOf<MatchRecord>()
    val favoritePlayers = mutableStateListOf<FavoritePlayer>()
    val favoriteNames: List<String> get() = favoritePlayers.map { it.name }

    // CRONOLOGIA TITOLI: Lista reattiva che memorizza gli ultimi titoli inseriti manualmente
    val matchTitleHistory = mutableStateListOf<String>()

    // ---> STATI PER IL CRONOMETRO <---
    // Variabile che conta i secondi (Osservata dalla UI grafica per aggiornare lo schermo in tempo reale)
    var matchDurationSeconds by mutableLongStateOf(0L)

    // ---> STATI SALVA-VITA <---
    // Mostra o nasconde il popup all'apertura dell'app
    var showResumeMatchDialog by mutableStateOf(false)

    // Previene la collisione tra fine partita manuale e fine partita automatica.
    var isNavigationLocked = false
        private set // Visibile all'esterno, ma modificabile solo dal ViewModel

    // Variabile che conserva la partita "fantasma" trovata nella memoria
    private var pendingBackup: MatchBackup? = null

    // Oggetto che contiene il "processo in background" del timer per poterlo fermare quando vogliamo (Pause/Reset)
    private var timerJob: Job? = null

    // Nomi dei "File di Testo" che verranno creati nella memoria fisica del telefono dal DataStore
    private val historyKey = stringPreferencesKey("history_list")
    private val favoritesKey = stringPreferencesKey("favorites_list")
    private val titleHistoryKey = stringPreferencesKey("match_title_history")

    // ---> NUOVA CHIAVE DATABASE PER IL BACKUP <---
    private val backupKey = stringPreferencesKey("current_match_backup")

    // ---> CHIAVE DATABASE PER I PRESET CUSTOM DEL TIMER <---
    private val timerPresetsKey = stringPreferencesKey("custom_timer_presets")

    // ====================================================================
    // CHIAVI DATABASE E STATI REATTIVI PER LE IMPOSTAZIONI DELL'APP
    // ====================================================================
    // 🧠 SPIEGAZIONE DIDATTICA:
    // Le chiavi DataStore fungono da "indirizzi" univoci nella memoria persistente del telefono.
    // Gli stati `mutableStateOf` / `mutableIntStateOf` consentono a Compose di reagire
    // istantaneamente quando l'utente modifica una preferenza, ri-renderizzando la UI senza lag.

    // 1. LINGUA APP: Codice ISO della lingua selezionata ("system", "it", "en", "de", "es", "fr", "hi", "ja")
    private val appLanguageKey = stringPreferencesKey("app_language_preference")
    var appLanguage by mutableStateOf("system")
        private set

    // 2. BLOCCO ORIENTAMENTO:
    // 0 = Auto (sensori di sistema), 1 = Orizzontale (Landscape), 2 = Verticale (Portrait)
    private val appOrientationKey = intPreferencesKey("app_orientation_preference")
    var appOrientation by mutableIntStateOf(0)
        private set

    // 3. TEMA APP:
    // 0 = Sistema (segue il tema chiaro/scuro di Android), 1 = Scuro (Dark), 2 = Chiaro (Light)
    // Entrambi i temi mantengono la tavolozza dinamica nativa Material You (Dynamic Colors) di Android
    private val appThemeKey = intPreferencesKey("app_theme_preference")
    var appTheme by mutableIntStateOf(0)
        private set

    // 4. SCHERMO SEMPRE ATTIVO (FLAG_KEEP_SCREEN_ON):
    // 0 = Solo in partita (CounterScreen), 1 = Sempre attivo (in tutta l'app), 2 = Disattivato (mai attivo)
    private val keepScreenOnKey = intPreferencesKey("keep_screen_on_preference")
    var keepScreenOnMode by mutableIntStateOf(0)
        private set

    // 4.5 FEEDBACK APTICO GLOBALE:
    // Memorizza e gestisce l'abilitazione delle vibrazioni tattili dell'app (true di default al primo avvio)
    private val hapticFeedbackKey = booleanPreferencesKey("haptic_feedback_preference")
    var isHapticFeedbackEnabled by mutableStateOf(true)
        private set

    // 5. PASSO PRESSIONE PROLUNGATA (+ e -):
    private val longPressPositiveStepKey = intPreferencesKey("long_press_positive_step")
    var longPressPositiveStep by mutableIntStateOf(10)
        private set

    private val longPressNegativeStepKey = intPreferencesKey("long_press_negative_step")
    var longPressNegativeStep by mutableIntStateOf(5)
        private set

    // 6. CONFERMA AZIONI RAPIDE (Azzera e Concludi):
    /**
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Memorizza se l'utente desidera visualizzare i dialoghi di conferma prima di eseguire
     * azioni distruttive o definitive come "Azzera" i punteggi o "Concludi" la partita in corso.
     * - Se TRUE (default): I tasti aprono il rispettivo AlertDialog di conferma.
     * - Se FALSE: I tasti eseguono l'azione istantaneamente senza chiedere conferme.
     */
    private val confirmQuickActionsKey = booleanPreferencesKey("confirm_quick_actions_preference")
    var confirmQuickActions by mutableStateOf(true)
        private set

    // 7. ORDINAMENTO DINAMICO DELLA CLASSIFICA:
    /**
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Memorizza se la classifica in tempo reale deve riordinarsi automaticamente
     * posizionando in alto i giocatori con punteggio maggiore e, in caso di parità,
     * l'ultimo che ha ottenuto punti più recentemente.
     * - Di default è impostato a FALSE (disattivato) al primo avvio/installazione dell'app.
     */
    private val dynamicLeaderboardSortingKey = booleanPreferencesKey("dynamic_leaderboard_sorting_preference")
    var dynamicLeaderboardSorting by mutableStateOf(false)
        private set

    // 8. NASCONDI PREMI DI PARTITA:
    /**
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Memorizza se l'utente desidera nascondere la sezione e le card dei premi assegnati
     * a fine sfida nelle schermate di analisi e risultati della partita.
     * - Di default è FALSE (disattivato al primo avvio) per mostrare sempre i premi di sfida.
     * - Non influenza GlobalStatsScreen che continua a mostrare tutti i dati e i record generali.
     */
    private val hideMatchAwardsKey = booleanPreferencesKey("hide_match_awards_preference")
    var hideMatchAwards by mutableStateOf(false)
        private set

    // 9. COLORE PERSONALIZZATO SALVATO (DataStore HSL):
    /**
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Memorizza il valore int ARGB dell'ultimo colore personalizzato scelto dall'utente
     * nella tavolozza HSL con selettore del dialogo "Modifica Giocatore".
     * Di default è impostato su un colore viola/vibrante (0xFF9C27B0).
     */
    private val savedCustomColorKey = intPreferencesKey("saved_custom_color_preference")
    var savedCustomColor by mutableIntStateOf(0xFF9C27B0.toInt())
        private set

    // 10. STILE SFONDO APP:
    /**
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Memorizza lo stile decorativo dello sfondo dell'applicazione.
     * Di default è impostato su BackgroundStyle.ICONS (Icone tematiche in filigrana).
     */
    private val appBackgroundKey = intPreferencesKey("app_background_preference")
    var backgroundStyle by mutableStateOf(BackgroundStyle.ICONS)
        private set

    // 5.5 PUNTEGGIO INIZIALE GIOCO:
    /**
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Memorizza il punteggio di partenza assegnato a ciascun giocatore quando
     * viene aggiunto al tavolo di gioco o quando i punteggi vengono azzerati.
     * Di default è 0.
     */
    private val initialScoreKey = intPreferencesKey("initial_score_preference")
    var initialScore by mutableIntStateOf(0)
        private set

    // Preset predefiniti di fabbrica per il timer (30 sec, 1 min, 5 min, 10 min)
    private val defaultTimerPresets = listOf("0030", "0100", "0500", "1000")

    // Lista reattiva dei 4 preset per il timer (di default: "0030" [30 sec], "0100" [1 min], "0500" [5 min], "1000" [10 min])
    val timerPresets = mutableStateListOf("0030", "0100", "0500", "1000")

    // Gson è il traduttore. Trasforma array, oggetti complessi e liste in lunghissime stringhe di testo (JSON) e viceversa per poterle salvare.
    private val gson = Gson()

    //variabile per tenere il conto dei punti per la combo della tripletta di punti consecutivi
    private var lastScorer: Player? = null
    private var comboCount = 0

    // Cronologia delle variazioni di punteggio della partita attiva per consultazione e annullamento mosse.
    val scoreActions = mutableStateListOf<ScoreAction>()

    // Copia di riserva della cronologia mosse per consentire il ripristino in caso di annullamento dell'azzeramento punteggi.
    private val undoScoreActionsBackup = mutableListOf<ScoreAction>()

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

            // 2. CARICAMENTO DEI GIOCATORI PREFERITI (Con retrocompatibilità per vecchi salvataggi List<String>)
            val jsonFavsString = preferences[favoritesKey]
            if (jsonFavsString != null) {
                try {
                    val richType = object : TypeToken<List<FavoritePlayer>>() {}.type
                    val savedFavs: List<FavoritePlayer> = gson.fromJson(jsonFavsString, richType)
                    favoritePlayers.clear()
                    if (savedFavs.isNotEmpty() && savedFavs.first().name != null) {
                        favoritePlayers.addAll(savedFavs)
                    } else {
                        val legacyType = object : TypeToken<List<String>>() {}.type
                        val legacyFavs: List<String> = gson.fromJson(jsonFavsString, legacyType)
                        favoritePlayers.addAll(legacyFavs.map { FavoritePlayer(it, null) })
                    }
                } catch (_: Exception) {
                    try {
                        val legacyType = object : TypeToken<List<String>>() {}.type
                        val legacyFavs: List<String> = gson.fromJson(jsonFavsString, legacyType)
                        favoritePlayers.clear()
                        favoritePlayers.addAll(legacyFavs.map { FavoritePlayer(it, null) })
                    } catch (_: Exception) { }
                }
            }

            // CARICAMENTO CRONOLOGIA TITOLI
            val jsonTitlesString = preferences[titleHistoryKey]
            if (jsonTitlesString != null) {
                val type = object : TypeToken<List<String>>() {}.type
                val savedTitles: List<String> = gson.fromJson(jsonTitlesString, type)
                matchTitleHistory.clear()
                matchTitleHistory.addAll(savedTitles)
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

            // CARICAMENTO PRESET TIMER PERSONALIZZATI
            val jsonPresetsString = preferences[timerPresetsKey]
            if (jsonPresetsString != null) {
                val type = object : TypeToken<List<String>>() {}.type
                val savedPresets: List<String> = gson.fromJson(jsonPresetsString, type)
                if (savedPresets.size == 4) {
                    timerPresets.clear()
                    timerPresets.addAll(savedPresets)
                }
            }

            // CARICAMENTO PREFERENZE LINGUA E ORIENTAMENTO
            // Se non sono mai state salvate, usano i default di sicurezza ("system" e 0 = Auto)
            val savedLanguage = preferences[appLanguageKey]
            if (savedLanguage != null) {
                appLanguage = savedLanguage
            }

            val savedOrientation = preferences[appOrientationKey]
            if (savedOrientation != null) {
                appOrientation = savedOrientation
            }

            // CARICAMENTO PREFERENZA TEMA APP (0 = Sistema, 1 = Scuro, 2 = Chiaro)
            val savedTheme = preferences[appThemeKey]
            if (savedTheme != null) {
                appTheme = savedTheme
            }

            // CARICAMENTO PREFERENZA SCHERMO SEMPRE ATTIVO (0 = Solo in partita, 1 = Sempre, 2 = Disattivato)
            val savedKeepScreenOn = preferences[keepScreenOnKey]
            if (savedKeepScreenOn != null) {
                keepScreenOnMode = savedKeepScreenOn
            }

            // CARICAMENTO PREFERENZE PASSO PRESSIONE PROLUNGATA (+ e -) CON PROTEZIONE ANTI-CRASH
            try {
                val savedPosStep = preferences[longPressPositiveStepKey]
                if (savedPosStep != null && savedPosStep > 0) {
                    longPressPositiveStep = savedPosStep
                }
            } catch (_: Exception) {
                longPressPositiveStep = 10
            }

            try {
                val savedNegStep = preferences[longPressNegativeStepKey]
                if (savedNegStep != null && savedNegStep > 0) {
                    longPressNegativeStep = savedNegStep
                }
            } catch (_: Exception) {
                longPressNegativeStep = 5
            }

            // CARICAMENTO PREFERENZA CONFERMA AZIONI RAPIDE
            try {
                val savedConfirm = preferences[confirmQuickActionsKey]
                if (savedConfirm != null) {
                    confirmQuickActions = savedConfirm
                }
            } catch (_: Exception) {
                confirmQuickActions = true
            }

            // CARICAMENTO PREFERENZA FEEDBACK APTICO GLOBALE
            try {
                val savedHaptic = preferences[hapticFeedbackKey]
                if (savedHaptic != null) {
                    isHapticFeedbackEnabled = savedHaptic
                }
            } catch (_: Exception) {
                isHapticFeedbackEnabled = true
            }

            // CARICAMENTO PREFERENZA ORDINAMENTO DINAMICO CLASSIFICA
            try {
                val savedDynamicSorting = preferences[dynamicLeaderboardSortingKey]
                if (savedDynamicSorting != null) {
                    dynamicLeaderboardSorting = savedDynamicSorting
                }
            } catch (_: Exception) {
                dynamicLeaderboardSorting = false
            }

            // CARICAMENTO PREFERENZA NASCONDI PREMI DI PARTITA
            try {
                val savedHideAwards = preferences[hideMatchAwardsKey]
                if (savedHideAwards != null) {
                    hideMatchAwards = savedHideAwards
                }
            } catch (_: Exception) {
                hideMatchAwards = false
            }

            // CARICAMENTO PREFERENZA COLORE PERSONALIZZATO SALVATO
            try {
                val savedCustom = preferences[savedCustomColorKey]
                if (savedCustom != null) {
                    savedCustomColor = savedCustom
                }
            } catch (_: Exception) {
                savedCustomColor = 0xFF9C27B0.toInt()
            }

            // CARICAMENTO PREFERENZA STILE SFONDO APP
            try {
                val savedBackgroundIndex = preferences[appBackgroundKey]
                if (savedBackgroundIndex != null) {
                    backgroundStyle = BackgroundStyle.fromInt(savedBackgroundIndex)
                }
            } catch (_: Exception) {
                backgroundStyle = BackgroundStyle.ICONS
            }

            // CARICAMENTO PREFERENZA PUNTEGGIO INIZIALE GIOCO CON PROTEZIONE ANTI-CRASH
            try {
                val savedInitScore = preferences[initialScoreKey]
                if (savedInitScore != null && savedInitScore >= 0) {
                    initialScore = savedInitScore
                }
            } catch (_: Exception) {
                initialScore = 0
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA PUNTEGGIO INIZIALE GIOCO
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Valida che il valore sia >= 0.
     * 2. Aggiorna lo stato `initialScore` in memoria.
     * 3. Scrive asincronamente il valore int nel DataStore su disco.
     */
    fun updateInitialScore(score: Int) {
        if (score >= 0) {
            initialScore = score
            viewModelScope.launch {
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[initialScoreKey] = score
                }
            }
        }
    }

    /**
     * RIPRISTINO DELLE IMPOSTAZIONI E PREFERENZE AI VALORI PREDEFINITI DI FABBRICA
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Reimposta tutti gli stati delle preferenze in memoria sui rispettivi valori di fabbrica.
     * 2. Scrive asincronamente i valori predefiniti nel DataStore su disco.
     * 3. Mantiene intatti e inalterati lo storico delle partite (`history`), i giocatori preferiti
     *    (`favoriteNames`), la cronologia titoli (`matchTitleHistory`) e i preset del timer (`timerPresets`).
     */
    fun resetDefaultSettings() {
        // 1. Ripristino in memoria delle preferenze dell'interfaccia e del gioco
        appLanguage = "system"
        appOrientation = 0
        appTheme = 0
        keepScreenOnMode = 0
        isHapticFeedbackEnabled = true
        longPressPositiveStep = 10
        longPressNegativeStep = 5
        initialScore = 0
        confirmQuickActions = true
        dynamicLeaderboardSorting = false
        hideMatchAwards = false
        backgroundStyle = BackgroundStyle.ICONS
        savedCustomColor = 0xFF9C27B0.toInt()

        // Applicazione immediata della lingua di sistema per sincronizzare l'interfaccia OS
        setAppLanguagePreference("system")

        // 2. Persistenza asincrona su DataStore
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[appLanguageKey] = "system"
                prefs[appOrientationKey] = 0
                prefs[appThemeKey] = 0
                prefs[keepScreenOnKey] = 0
                prefs[hapticFeedbackKey] = true
                prefs[longPressPositiveStepKey] = 10
                prefs[longPressNegativeStepKey] = 5
                prefs[initialScoreKey] = 0
                prefs[confirmQuickActionsKey] = true
                prefs[dynamicLeaderboardSortingKey] = false
                prefs[hideMatchAwardsKey] = false
                prefs[appBackgroundKey] = 0
                prefs[savedCustomColorKey] = 0xFF9C27B0.toInt()
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA STILE SFONDO APP
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato `backgroundStyle` in memoria.
     * 2. Scrive asincronamente la preferenza numerica nel DataStore su disco.
     */
    fun setBackgroundStylePreference(styleIndex: Int) {
        backgroundStyle = BackgroundStyle.fromInt(styleIndex)
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[appBackgroundKey] = styleIndex
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA COLORE PERSONALIZZATO
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato `savedCustomColor` in memoria.
     * 2. Scrive il nuovo valore int ARGB asincronamente nel DataStore su disco.
     */
    fun saveCustomColor(colorInt: Int) {
        savedCustomColor = colorInt
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[savedCustomColorKey] = colorInt
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA NASCONDI PREMI DI PARTITA
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato booleano `hideMatchAwards` in memoria.
     * 2. Scrive la preferenza asincronamente nel DataStore su disco.
     */
    fun setHideMatchAwardsPreference(enabled: Boolean) {
        hideMatchAwards = enabled
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[hideMatchAwardsKey] = enabled
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA FEEDBACK APTICO
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato booleano `isHapticFeedbackEnabled` in memoria.
     * 2. Scrive la preferenza asincronamente nel DataStore su disco.
     */
    fun setHapticFeedbackPreference(enabled: Boolean) {
        isHapticFeedbackEnabled = enabled
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[hapticFeedbackKey] = enabled
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA ORDINAMENTO DINAMICO CLASSIFICA
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato booleano in memoria (`dynamicLeaderboardSorting`).
     * 2. Salva la preferenza asincronamente nel DataStore su disco.
     */
    fun setDynamicLeaderboardSortingPreference(enabled: Boolean) {
        dynamicLeaderboardSorting = enabled
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[dynamicLeaderboardSortingKey] = enabled
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA CONFERMA AZIONI RAPIDE
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna immediatamente lo stato booleano in memoria (`confirmQuickActions`).
     * 2. Scrive asincronamente la preferenza nel DataStore Preferences su disco tramite `dataStore.edit`.
     */
    fun setConfirmQuickActionsPreference(enabled: Boolean) {
        confirmQuickActions = enabled
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[confirmQuickActionsKey] = enabled
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA DEL TEMA DELL'APP
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato `appTheme` (0 = Sistema, 1 = Scuro nativo, 2 = Chiaro nativo).
     * 2. Scrive la preferenza in background nel DataStore.
     * 3. ScoreCounterTheme ricompone istantaneamente la tavolozza colori Material You
     *    nativa (dynamicDarkColorScheme / dynamicLightColorScheme) senza alterare le sfumature di sistema.
     */
    fun setAppThemePreference(newThemeIndex: Int) {
        appTheme = newThemeIndex
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[appThemeKey] = newThemeIndex
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA DELLA MODALITÀ SCHERMO SEMPRE ATTIVO
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato `keepScreenOnMode`:
     *    - 0: Solo durante la partita attiva (CounterScreen)
     *    - 1: Sempre attivo (il display non va mai in standby mentre l'app è aperta)
     *    - 2: Disattivato (segue le normali impostazioni di standby del sistema Android)
     * 2. Persiste la scelta nel DataStore asincronamente.
     * 3. L'Activity sincronizza reattivamente la flag WindowManager `FLAG_KEEP_SCREEN_ON`.
     */
    fun setKeepScreenOnPreference(newModeIndex: Int) {
        keepScreenOnMode = newModeIndex
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[keepScreenOnKey] = newModeIndex
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA DELLA LINGUA DELL'APP
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna immediatamente lo stato osservabile `appLanguage` in memoria, garantendo reattività immediata alla UI.
     * 2. Salva in background la stringa ISO (es. "it", "en", "system") all'interno del DataStore Preferences.
     * 3. Permette all'app di ricaricare la lingua corretta ad ogni successivo riavvio.
     */
    fun setAppLanguagePreference(newLanguageCode: String) {
        appLanguage = newLanguageCode
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[appLanguageKey] = newLanguageCode
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA DEL BLOCCO ORIENTAMENTO SCHERMO
     * 🧠 SPIEGAZIONE DIDATTICA:
     * 1. Aggiorna lo stato osservabile `appOrientation` (0 = Auto, 1 = Landscape, 2 = Portrait).
     * 2. Scrive il nuovo valore numerico nel DataStore asincronamente.
     * 3. La MainActivity o i listener dell'Activity applicano immediatamente il `requestedOrientation`.
     */
    fun setAppOrientationPreference(newOrientation: Int) {
        appOrientation = newOrientation
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[appOrientationKey] = newOrientation
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA INCREMENTO PROLUNGATO (+)
     */
    fun updateLongPressPositiveStep(step: Int) {
        if (step > 0) {
            longPressPositiveStep = step
            viewModelScope.launch {
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[longPressPositiveStepKey] = step
                }
            }
        }
    }

    /**
     * IMPOSTAZIONE E PERSISTENZA DECREMENTO PROLUNGATO (-)
     */
    fun updateLongPressNegativeStep(step: Int) {
        if (step > 0) {
            longPressNegativeStep = step
            viewModelScope.launch {
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[longPressNegativeStepKey] = step
                }
            }
        }
    }

    /**
     * AGGIORNAMENTO E PERSISTENZA PRESET TIMER
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Aggiorna il valore grezzo (es. "0300" per 3 minuti) del preset all'indice specificato (0..3)
     * e salva asincronamente la nuova lista in formato JSON nel DataStore del dispositivo.
     * Applichiamo una sanitizzazione a 4 cifre numeriche (padStart/takeLast) per garantire integrità formale.
     */
    fun updateTimerPreset(index: Int, newRawValue: String) {
        if (index in 0 until timerPresets.size) {
            val sanitized = newRawValue.filter { it.isDigit() }.padStart(4, '0').takeLast(4)
            timerPresets[index] = sanitized
            viewModelScope.launch {
                getApplication<Application>().dataStore.edit { prefs ->
                    prefs[timerPresetsKey] = gson.toJson(timerPresets.toList())
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
            isLowestScoreWins = backup.isLowestScoreWins
            comboCount = backup.comboCount
            players.clear() // Puliamo il tavolo
            scoreActions.clear()
            undoScoreActionsBackup.clear()
            // Rimettiamo i giocatori al loro posto con tutta la loro storia intatta per i grafici
            backup.players.forEach { record ->
                val p = Player(record.name)
                p.score = record.score
                p.color = record.color//ricordiamo anche il colore del giocatore
                p.fireComboCount = record.fireComboCount // ---> FIX: Recuperiamo il numero di combo
                p.scoreHistory.clear()
                p.scoreHistory.addAll(record.scoreHistory ?: listOf(0))
                players.add(p)
            }

            // Ripristiniamo l'ultimo marcatore attivo se l'indice è valido
            if (backup.lastScorerIndex in 0 until players.size) {
                lastScorer = players[backup.lastScorerIndex]
            } else {
                lastScorer = null
            }

            // 🧠 RIPRISTINO CRONOLOGIA AZIONI (ScoreHistory):
            // Mappiamo ciascun ScoreActionRecord salvato su disco ricreando l'oggetto runtime `ScoreAction`.
            // Colleghiamo i riferimenti dei giocatori ripristinati per abilitare lo storico completo e la funzione Undo.
            backup.scoreActions?.forEach { record ->
                val targetPlayer = if (record.playerIndex in 0 until players.size) {
                    players[record.playerIndex]
                } else {
                    players.firstOrNull { it.name == record.playerName } ?: Player(record.playerName).apply { color = record.playerColor }
                }

                val prevScorer = if (record.prevLastScorerIndex in 0 until players.size) {
                    players[record.prevLastScorerIndex]
                } else if (record.prevLastScorerName != null) {
                    players.firstOrNull { it.name == record.prevLastScorerName }
                } else null

                scoreActions.add(
                    ScoreAction(
                        player = targetPlayer,
                        playerName = record.playerName,
                        playerColor = record.playerColor,
                        delta = record.delta,
                        scoreAfter = record.scoreAfter,
                        matchDurationSeconds = record.matchDurationSeconds,
                        previousScore = record.previousScore,
                        prevLastScoreUpdateTime = record.prevLastScoreUpdateTime,
                        prevComboCount = record.prevComboCount,
                        prevLastScorer = prevScorer,
                        prevLastScorerWasOnFire = record.prevLastScorerWasOnFire,
                        prevPlayerFireComboCount = record.prevPlayerFireComboCount
                    )
                )
            }
        }
        showResumeMatchDialog = false // Chiude il popup
    }

    // ---> FUNZIONE PER SALVARE IL BACKUP <---
    fun saveBackup() {
        if (players.isEmpty()) return

        // 🧠 SERIALIZZAZIONE CRONOLOGIA MOSSE:
        // Convertiamo la lista di `ScoreAction` in oggetti `ScoreActionRecord` serializzabili su JSON.
        val actionRecords = scoreActions.map { action ->
            val playerIdx = players.indexOf(action.player)
            val prevLastScorerIdx = action.prevLastScorer?.let { players.indexOf(it) } ?: -1
            ScoreActionRecord(
                playerIndex = playerIdx,
                playerName = action.playerName,
                playerColor = action.playerColor,
                delta = action.delta,
                scoreAfter = action.scoreAfter,
                matchDurationSeconds = action.matchDurationSeconds,
                previousScore = action.previousScore,
                prevLastScoreUpdateTime = action.prevLastScoreUpdateTime,
                prevComboCount = action.prevComboCount,
                prevLastScorerIndex = prevLastScorerIdx,
                prevLastScorerName = action.prevLastScorer?.name,
                prevLastScorerWasOnFire = action.prevLastScorerWasOnFire,
                prevPlayerFireComboCount = action.prevPlayerFireComboCount
            )
        }

        val lastScorerIdx = lastScorer?.let { players.indexOf(it) } ?: -1

        val backup = MatchBackup(
            title = matchTitle,
            target = targetScore,
            players = players.map {
                PlayerRecord(
                    it.name,
                    it.score,
                    it.scoreHistory.toList(),
                    fireComboCount = it.fireComboCount, // ---> Salviamo il numero di combo
                    color = it.color,
                )
            },
            seconds = matchDurationSeconds,
            isLowestScoreWins = isLowestScoreWins,
            scoreActions = actionRecords,
            comboCount = comboCount,
            lastScorerIndex = lastScorerIdx
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
    fun addPlayer(name: String, color: Int) {
        // 1. Pulizia: Togliamo eventuali spazi vuoti iniziali o finali inseriti per sbaglio (es. " Marco " diventa "Marco")
        val cleanName = name.trim()

        // 2. Controllo: Cerchiamo se nella lista c'è GIÀ qualcuno con questo esatto nome.
        // ignoreCase = true fa sì che "Marco" e "marco" vengano considerati la stessa identica persona!
        val alreadyExists = players.any { it.name.equals(cleanName, ignoreCase = true) }

        // 3. Esecuzione: Aggiungiamo il giocatore SOLO SE non è vuoto E non esiste già al tavolo.
        if (cleanName.isNotEmpty() && !alreadyExists) {
            val newPlayer = Player(cleanName, initialScore)
            newPlayer.color = color // Assegniamo il colore scelto
            players.add(newPlayer)
            saveBackup() // Salviamo subito il nuovo giocatore nel salva-vita
        }
    }

    /**
     * Aggiorna il colore di un giocatore già seduto al tavolo.
     * Questa funzione viene chiamata dal popup della tavolozza colori in CreateMatchScreen.
     */
    fun updatePlayerColor(player: Player, newColor: Int) {
        // 1. Cerchiamo in che posizione si trova questo giocatore nella lista del tavolo
        val index = players.indexOf(player)

        // Se lo troviamo (index è diverso da -1)
        if (index != -1) {
            // 2. Gli assegniamo il nuovo colore
            players[index].color = newColor

            // 3. TRUCCO COMPOSE: Per forzare la grafica ad aggiornarsi istantaneamente,
            // "sostituiamo" il giocatore con se stesso nella lista. Questo fa capire a
            // Jetpack Compose che c'è stata una modifica e deve ricaricare i colori.
            players[index] = players[index]
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

    // Sposta fisicamente un giocatore su o giù nella lista per il riordino grafico
    fun movePlayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex in players.indices && toIndex in players.indices) {
            val player = players.removeAt(fromIndex)
            players.add(toIndex, player)
            saveBackup()
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

    // ---> FUNZIONI GESTIONE GIOCATORI RAPIDI (PREFERITI) CON COLORE OPZIONALE <---
    fun addFavorite(name: String, color: Int? = null) {
        val cleanName = name.trim()

        // Controllo intelligente: verifichiamo se il nome esiste già ignorando le maiuscole
        val alreadyExists = favoritePlayers.any { it.name.equals(cleanName, ignoreCase = true) }

        if (cleanName.isNotEmpty() && !alreadyExists) {
            favoritePlayers.add(FavoritePlayer(cleanName, color)) // Salviamo l'oggetto ricco
            saveFavorites() // Dopo ogni modifica alla lista, salva fisicamente sul disco
        }
    }

    fun removeFavorite(player: FavoritePlayer) {
        favoritePlayers.remove(player)
        saveFavorites()
    }

    fun removeFavorite(name: String) {
        favoritePlayers.removeAll { it.name.equals(name, ignoreCase = true) }
        saveFavorites()
    }

    fun editFavorite(oldName: String, newName: String, newColor: Int? = null) {
        val cleanNewName = newName.trim()
        val index = favoritePlayers.indexOfFirst { it.name.equals(oldName, ignoreCase = true) }
        if (index != -1 && cleanNewName.isNotEmpty()) {
            val conflict = favoritePlayers.indexOfFirst { it.name.equals(cleanNewName, ignoreCase = true) }
            if (conflict == -1 || conflict == index) {
                favoritePlayers[index] = FavoritePlayer(cleanNewName, newColor)
                saveFavorites()
            }
        }
    }

    fun restoreFavorite(index: Int, player: FavoritePlayer) {
        if (index in 0..favoritePlayers.size) {
            favoritePlayers.add(index, player)
        } else {
            favoritePlayers.add(player)
        }
        saveFavorites()
    }

    fun restoreFavorite(index: Int, name: String) {
        restoreFavorite(index, FavoritePlayer(name, null))
    }

    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        if (fromIndex in favoritePlayers.indices && toIndex in favoritePlayers.indices) {
            val fav = favoritePlayers.removeAt(fromIndex)
            favoritePlayers.add(toIndex, fav)
            saveFavorites()
        }
    }

    private fun saveFavorites() {
        viewModelScope.launch {
            val jsonString = gson.toJson(favoritePlayers)
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[favoritesKey] = jsonString
            }
        }
    }

    /**
     * LOGICA COMBO ROVENTE E SINCRONIZZAZIONE GLOBALE:
     * Gestisce l'assegnazione punti controllando le serie consecutive.
     * Implementa il "Global Tick" per la sincronizzazione dei grafici a linee.
     */
    fun updatePlayerScore(player: Player, amount: Int) {
        // FILTRO MOVIMENTO: Se amount è 0, non è un vero cambiamento.
        // Interrompiamo subito la funzione per non sporcare la cronologia con dati inutili
        // e per non attivare il pulsante "Fine Partita" se non c'è stato spostamento reale.
        if (amount == 0) return

        // Memorizzazione dello stato precedente per consentire l'annullamento della mossa
        val prevScore = player.score
        val prevLastScoreUpdateTime = player.lastScoreUpdateTime
        val prevComboCount = comboCount
        val prevLastScorer = lastScorer
        val prevLastScorerWasOnFire = lastScorer?.isOnFire ?: false
        val prevFireComboCount = player.fireComboCount

        // ============================================================================
        // MACCHINA A STATI: SISTEMA COMBO E STATUS "ON FIRE"
        // ============================================================================
        // Fase 1: Classificazione dell'input.
        // Verifichiamo se l'azione in corso è positiva (punti guadagnati) o negativa (penalità).
        if (amount > 0) {

            // STATO 1: CONTINUITA' (Il giocatore mantiene l'iniziativa)
            if (lastScorer == player) {
                // Il giocatore che sta segnando è lo stesso che ha segnato l'ultimo punto.
                // Incrementiamo la catena della combo corrente.
                comboCount++
            }
            // STATO 2: ROTTURA DELLA CATENA (Un avversario interrompe la serie)
            else {
                // Un giocatore diverso ha segnato, rubando l'iniziativa.
                // 1. Spegniamo forzatamente lo stato "On Fire" a tutti i giocatori al tavolo.
                players.forEach { it.isOnFire = false }

                // 2. Aggiorniamo il puntatore 'lastScorer' al nuovo giocatore.
                lastScorer = player

                // 3. Inizializziamo la nuova catena combo partendo da 1 (il punto attuale).
                comboCount = 1
            }

            // Fase 2: Valutazione della Soglia di Innesco (Trigger)
            if (comboCount == 3) {
                // Il giocatore ha colpito esattamente 3 volte di fila.
                // Attiviamo l'indicatore visivo di "serie positiva".
                player.isOnFire = true

                // Incrementiamo il tracciatore statistico storico.
                // Questo dato servirà a fine partita per assegnare il premio "Inarrestabile".
                player.fireComboCount++
            }
            // Fase 3: Mantenimento dello Stato
            else if (comboCount > 3) {
                // Il giocatore è al 4°, 5° o n-esimo punto consecutivo.
                // Manteniamo il flag 'isOnFire' attivo per assicurarci che la UI resti accesa.
                // NOTA STRUTTURALE: Non incrementiamo 'fireComboCount' qui. Quel contatore
                // traccia quante "volte" ci si accende, non quanti punti si fanno da accesi.
                player.isOnFire = true
            }

        }
        // STATO 3: GESTIONE DELLE PENALITA' E AZZERAMENTI
        else {
            // Il punteggio inserito è <= 0 (penalità subita o correzione di un errore).
            // Questo evento agisce come un "reset termico" della partita.

            // 1. Estinguiamo ogni status "On Fire" globale.
            players.forEach { it.isOnFire = false }

            // 2. Azzeriamo il contatore delle serie consecutive.
            comboCount = 0

            // 3. Rimuoviamo il riferimento all'ultimo marcatore.
            // Il prossimo che segnerà punti positivi inizierà una combo totalmente nuova.
            lastScorer = null
        }

        // 1. APPLICAZIONE MATEMATICA: Modifica il punteggio grezzo del giocatore
        player.changeScore(amount)

        // Aggiorniamo il timestamp di ultimo aggiornamento per il tie-breaker dell'ordinamento dinamico classifica
        player.lastScoreUpdateTime = System.currentTimeMillis()

        // 2. SINCRONIZZAZIONE GRAFICA (GLOBAL TICK)
        // Iteriamo su tutti i giocatori presenti al tavolo, non solo su chi ha segnato.
        // Aggiungiamo alla cronologia di ognuno il proprio punteggio corrente.
        // Se un giocatore non ha segnato in questo turno, registrerà un valore identico
        // al precedente, generando una linea piatta (orizzontale) nel tracciato vettoriale.
        players.forEach { p ->
            p.scoreHistory.add(p.score)
        }

        // Registrazione dell'azione nella cronologia attiva della partita
        scoreActions.add(
            ScoreAction(
                player = player,
                playerName = player.name,
                playerColor = player.color,
                delta = amount,
                scoreAfter = player.score,
                matchDurationSeconds = matchDurationSeconds,
                previousScore = prevScore,
                prevLastScoreUpdateTime = prevLastScoreUpdateTime,
                prevComboCount = prevComboCount,
                prevLastScorer = prevLastScorer,
                prevLastScorerWasOnFire = prevLastScorerWasOnFire,
                prevPlayerFireComboCount = prevFireComboCount
            )
        )

        // Applichiamo la modifica al punteggio e salviamo il backup salva-vita
     //   player.changeScore(amount)
        saveBackup()
    }

    /**
     * Annulla l'ultima variazione di punteggio registrata nella partita in corso.
     * Ripristina il punteggio precedente del giocatore, lo stato delle serie combo e
     * rimuove l'ultimo tick temporale sincronizzato dalle cronologie di tutti i giocatori.
     * Restituisce l'azione annullata se l'operazione è andata a buon fine, null se non vi erano mosse.
     */
    fun undoLastScoreAction(): ScoreAction? {
        if (scoreActions.isEmpty()) return null
        val lastAction = scoreActions.removeAt(scoreActions.lastIndex)

        // Ripristino del punteggio precedente e dei metadati del giocatore
        lastAction.player.score = lastAction.previousScore
        lastAction.player.lastScoreUpdateTime = lastAction.prevLastScoreUpdateTime
        lastAction.player.fireComboCount = lastAction.prevPlayerFireComboCount

        // Ripristino della macchina a stati per le combo e lo status rovente
        players.forEach { it.isOnFire = false }
        comboCount = lastAction.prevComboCount
        lastScorer = lastAction.prevLastScorer
        if (lastAction.prevLastScorer != null && lastAction.prevLastScorerWasOnFire) {
            lastAction.prevLastScorer.isOnFire = true
        }

        // Rimozione sincronizzata dell'ultimo punto registrato nel grafico per ciascun partecipante
        players.forEach { p ->
            if (p.scoreHistory.size > 1) {
                p.scoreHistory.removeAt(p.scoreHistory.lastIndex)
            }
        }

        // Aggiornamento del salvataggio locale della partita
        saveBackup()
        return lastAction
    }

    /**
     * Ripristina un'azione di punteggio precedentemente annullata (Undo dell'annullamento mossa).
     */
    fun restoreScoreAction(action: ScoreAction) {
        action.player.score = action.scoreAfter
        action.player.lastScoreUpdateTime = System.currentTimeMillis()
        // Ripristino sincronizzato del punto nel grafico per ciascun partecipante
        players.forEach { p ->
            p.scoreHistory.add(p.score)
        }
        scoreActions.add(action)
        saveBackup()
    }

    /**
     * 🧠 TEORIA KOTLIN: La classe 'Pair'
     * Un 'Pair' (Coppia) è una struttura dati nativa di Kotlin progettata per
     * contenere esattamente due valori (anche di tipo diverso), accessibili
     * tramite le proprietà '.first' e '.second'.
     * Il suo scopo principale è permettere a una funzione di restituire DUE
     * risultati contemporaneamente, evitandoci di dover creare una classe
     * personalizzata solo per impacchettare i dati (come avremmo fatto in Java/C++).
     */
    // Funzione furba per la Snackbar dell'azzeramento! Invece di azzerare e basta, fa prima una "Copia di Sicurezza"
    fun resetScoresWithUndo(): List<Pair<Int, List<Int>>> {
        // Mappa e salva una lista di "Coppie" (Pair): Il punteggio finale del giocatore e TUTTA la sua lunga lista di mosse passate
        val oldData = players.map { Pair(it.score, it.scoreHistory.toList()) }

        // Salvataggio temporaneo delle mosse registrate per consentire l'eventuale ripristino
        undoScoreActionsBackup.clear()
        undoScoreActionsBackup.addAll(scoreActions)
        scoreActions.clear()

        // Fatta la copia, azzera o ripristina i punti al punteggio iniziale configurato (initialScore)
        players.forEach {
            it.score = initialScore
            it.scoreHistory.clear()
            it.scoreHistory.add(initialScore) // Registra il ripristino al punteggio iniziale nel grafico
            it.isOnFire = false // Spegne visivamente il cerchio infuocato
            it.fireComboCount = 0 // Cancella la memoria per il premio "Inarrestabile"
        }
        //  AZZERAMENTO SERIE
        // Dobbiamo far dimenticare al motore chi stava segnando prima dell'azzeramento,
        // altrimenti il prossimo punto si aggancerà alla vecchia serie fantasma!
        comboCount = 0
        lastScorer = null
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
        // Ripristino della cronologia delle mosse registrate prima dell'azzeramento
        scoreActions.clear()
        scoreActions.addAll(undoScoreActionsBackup)
        undoScoreActionsBackup.clear()
        saveBackup() // Ri-aggiorna il salva-vita
    }

    //funzione per bloccare la navigazione dopo aver premuto la fine della partita
    fun lockNavigation(): Boolean {
        if (isNavigationLocked) return false
        isNavigationLocked = true
        return true
    }

    /**
     * ====================================================================
     * RESET INTEGRALE DELLA SESSIONE
     * ====================================================================
     * Questa funzione garantisce che ogni traccia della partita precedente
     * venga eliminata sia dalla RAM che dal Disco Fisso prima di permettere
     * l'avvio di una nuova sfida.
     */
    fun clearMatch() {
        isNavigationLocked = false // Sblocchiamo il lucchetto per le nuove partite!
        diceSides = 6 //impostiamo come dado di default quello a 6 facce
        matchTitle = ""
        targetScore = "" // Puliamo anche l'obiettivo di vittoria precedente per non portarcelo nelle sfide future
        players.clear()
        resetTimer() // Assicuriamoci che il timer parta da 0 nella prossima partita
        clearBackup() // Fondamentale: se iniziamo da zero, il vecchio backup fantasma deve sparire
        // Pulizia della cronologia delle mosse della partita precedente
        scoreActions.clear()
        undoScoreActionsBackup.clear()
    }

    // ---> LA FUNZIONE DI SALVATAGGIO DEFINITIVA <---
    fun saveCurrentMatch() {
        if (players.isEmpty()) return

        // 🧠 GESTIONE COMPLETA DEL PAREGGIO (EX-AEQUO):
        // Non scegliamo arbitrariamente un solo giocatore in base all'ordine di lista.
        // Identifichiamo il punteggio migliore in base al criterio di vittoria attivo:
        val winningScore = if (isLowestScoreWins) {
            players.minOfOrNull { it.score }
        } else {
            players.maxOfOrNull { it.score }
        } ?: return

        // Tutti i giocatori che hanno totalizzato il punteggio vincente sono riconosciuti a pari merito come vincitori!
        val winners = players.filter { it.score == winningScore }
        val winnersNames = winners.joinToString(", ") { it.name }

        val finalTitle = if (matchTitle.isEmpty()) "Sfida Senza Nome" else matchTitle

        // Congelamento della classifica finale ordinata correttamente in base al criterio di vittoria
        val snapshotOfPlayers = players.map { activePlayer ->
            PlayerRecord(
                name = activePlayer.name,
                score = activePlayer.score,
                scoreHistory = activePlayer.scoreHistory.toList(),
                // ---> IMPORTANTE: Passiamo il conteggio delle combo al database permanente
                fireComboCount = activePlayer.fireComboCount,
                color = activePlayer.color// ---> FIX DATABASE: Salviamo il colore su disco per lo Storico
            )
        }.run {
            if (isLowestScoreWins) sortedBy { it.score } else sortedByDescending { it.score }
        }

        // Creazione del record della partita completo includendo tutti i vincitori e lo stato isLowestScoreWins
        val record = MatchRecord(
            title = finalTitle,
            winnerName = winnersNames, // Se c'è pareggio, conterrà i nomi separati da virgola (es. "Marco, Daniele")
            winningScore = winningScore,
            allPlayers = snapshotOfPlayers, // Passiamo al database l'intera classifica congelata
            durationSeconds = matchDurationSeconds, // Salviamo i secondi del cronometro!
            timestamp = System.currentTimeMillis(), // Salviamo il millisecondo esatto della chiusura per la data
            isLowestScoreWins = isLowestScoreWins // Salviamo il criterio di vittoria sul disco
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
    /**
     * ====================================================================
     * STATISTICHE AVANZATE: IL CECCHINO 🎯
     * ====================================================================
     * Trova il giocatore che ha ottenuto più punti positivi in una singola mossa.
     * Ritorna un 'Pair' (Coppia) contenente il Giocatore e il valore del salto.
     * Se nessuno ha fatto punti, ritorna 'null'.
     */
    fun getCecchino(): Pair<Player, Int>? {
        // Se non ci sono giocatori, non c'è nessun cecchino
        if (players.isEmpty()) return null

        var bestPlayer: Player? = null
        var absoluteMaxJump = 0

        for (player in players) {
            // ---> MAGIA DI KOTLIN (Programmazione Funzionale) <---
            // zipWithNext { a, b -> b - a } crea le coppie contigue e calcola la differenza.
            // maxOrNull() trova la differenza più alta. Se la lista è vuota, ritorna null (gestito con l'operatore Elvis ?: 0).
            val playerMaxJump = player.scoreHistory
                .zipWithNext { previousScore, currentScore -> currentScore - previousScore }
                .maxOrNull() ?: 0

            // Se il salto di questo giocatore è il più alto registrato finora, diventa lui il "bestPlayer"
            if (playerMaxJump > absoluteMaxJump) {
                absoluteMaxJump = playerMaxJump
                bestPlayer = player
            }
        }

        // Ritorna il vincitore solo se ha effettivamente guadagnato punti (salto > 0)
        return if (bestPlayer != null && absoluteMaxJump > 0) {
            Pair(bestPlayer, absoluteMaxJump)
        } else {
            null
        }
    }
    /**
     * ====================================================================
     * STATISTICHE AVANZATE: L'INARRESTABILE 🔥
     * ====================================================================
     * Trova il giocatore che ha innescato più volte lo stato "On Fire".
     * Ritorna un 'Pair' contenente il Giocatore e il numero di combo effettuate.
     * Se nessuno ha fatto combo, ritorna 'null'.
     */
    fun getInarrestabile(): Pair<Player, Int>? {
        // Controllo di sicurezza: se la partita è vuota, annulla tutto
        if (players.isEmpty()) return null

        // ---> MAGIA DI KOTLIN: maxByOrNull e 'it' <---
        // maxByOrNull scansiona tutta la lista e ci restituisce direttamente
        // L'OGGETTO (Player) che possiede il valore più alto.
        // 'it' rappresenta "il giocatore corrente" durante l'iterazione interna,
        // si può usare se il lambda ha un solo parametro, altrimenti si scriverebbe { player -> player.score } ma { it.score } è la stessa cosa
        /*Una Lambda in Kotlin è semplicemente una funzione anonima (senza nome) che puoi trattare come se fosse una variabile.
        La si racchiude sempre tra parentesi graffe {
         */
        val bestPlayer = players.maxByOrNull { it.fireComboCount }//con maxByOrNull andiamo a trovare il valore massimo guardando la proprietà it.fireComboCount

        // Il giocatore vince il titolo SOLO se ha fatto almeno 1 combo ( > 0 )
        return if (bestPlayer != null && bestPlayer.fireComboCount > 0) {
            Pair(bestPlayer, bestPlayer.fireComboCount)
        } else {
            null
        }
    }

    /**
     * ====================================================================
     * STATISTICHE AVANZATE: IL GAMBERO 🦞
     * ====================================================================
     * Trova il giocatore che ha perso più punti in totale durante l'intera partita.
     * Ritorna un 'Pair' contenente il Giocatore e il numero (positivo) totale di punti persi.
     * Se nessuno ha mai perso punti, ritorna 'null'.
     */
    fun getGambero(): Pair<Player, Int>? {
        // Controllo di sicurezza
        if (players.isEmpty()) return null

        var worstPlayer: Player? = null
        var maxPointsLost = 0

        for (player in players) {
            // ---> MAGIA DI KOTLIN: filter & sum <---
            // 1. zipWithNext: calcola la differenza tra ogni punteggio consecutivo.
            // 2. filter { it < 0 }: agisce come un setaccio, tiene SOLO i numeri negativi (i malus).
            // 3. sum(): somma tutti i malus rimasti (es. -5 e -10 diventa -15).
            val totalNegativePoints = player.scoreHistory
                .zipWithNext { previousScore, currentScore -> currentScore - previousScore }
                .filter { it < 0 }
                .sum()

            // La variabile 'totalNegativePoints' ora è un numero negativo (es. -20).
            // Per comodità visiva, usiamo la matematica assoluta (abs) per trasformarlo
            // in positivo (es. 20) così sarà più facile stamparlo a schermo ("Hai perso 20 punti").
            val pointsLost = abs(totalNegativePoints)

            // Se questo giocatore ha perso più punti del record precedente, diventa lui il Gambero
            if (pointsLost > maxPointsLost) {
                maxPointsLost = pointsLost
                worstPlayer = player
            }
        }

        // Ritorna il risultato solo se qualcuno ha effettivamente perso almeno un punto
        return if (worstPlayer != null && maxPointsLost > 0) {
            Pair(worstPlayer, maxPointsLost)
        } else {
            null
        }
    }

    /**
     * ====================================================================
     * STATISTICHE AVANZATE: IL RITORNO DI FIAMMA 🚀
     * ====================================================================
     * Premia il giocatore che ha effettuato la rimonta più grande.
     * Calcola la differenza (delta) tra il punteggio più basso toccato nella sua
     * cronologia e il suo punteggio finale.
     */
    fun getRitornoDiFiamma(): Pair<Player, Int>? {
        if (players.isEmpty()) return null

        var comebackPlayer: Player? = null
        var maxRecovery = 0

        for (player in players) {
            // Se il giocatore non ha una cronologia, non c'è partita da analizzare
            if (player.scoreHistory.isEmpty()) continue

            // 🧠 TEORIA KOTLIN: La funzione 'minOrNull()' e l'operatore Elvis '?:'
            // In Kotlin, 'minOrNull()' esegue queste operazioni:
            // 1. Scansiona la lista e restituisce il numero più piccolo.
            // 2. (Null Safety) Se la lista dovesse essere completamente vuota, invece di lanciare
            //    una letale Exception (crash), restituisce educatamente 'null'.
            // L'operatore Elvis '?: 0' agisce come piano B: se la parte di sinistra restituisce null,
            // assegna automaticamente il valore 0 alla variabile lowestScore.
            val lowestScore = player.scoreHistory.minOrNull() ?: 0

            // Il "recupero" è la distanza matematica tra il finale e il punto più basso
            val recovery = player.score - lowestScore

            // Se il recupero è maggiore di 0 ed è il migliore visto finora, aggiorniamo il record
            if (recovery > maxRecovery) {
                maxRecovery = recovery
                comebackPlayer = player
            }
        }

        // Ritorna il Pair solo se c'è stato effettivamente un recupero valido
        return if (comebackPlayer != null && maxRecovery > 0) {
            Pair(comebackPlayer, maxRecovery)
        } else {
            null
        }
    }

    /**
     * GESTIONE CRONOLOGIA TITOLI:
     * Aggiunge un nuovo titolo alla memoria storica se soddisfa i requisiti di unicità
     * e non appartiene ai titoli rapidi predefiniti.
     */
    fun addTitleToHistory(title: String) {
        val cleanTitle = title.trim()
        
        // Filtro di esclusione: i titoli rapidi predefiniti non devono entrare in cronologia
        if (cleanTitle.isEmpty()) return

        // Gestione unicità: se il titolo esiste già, lo rimuoviamo per riportarlo in cima (posizione 0)
        matchTitleHistory.remove(cleanTitle)
        matchTitleHistory.add(0, cleanTitle)

        // Vincolo di capacità: manteniamo solo le ultime 5 voci inserite
        // Quando la lista accumula il sesto elemento,
        // l'algoritmo cancella automaticamente l'ultimo in fondo (il più vecchio rimasto),
        // assicurando che l'utente veda sempre e solo le ultime 5 sfide uniche giocate.
        if (matchTitleHistory.size > 5) {
            matchTitleHistory.removeAt(matchTitleHistory.size - 1)
        }

        // Persistenza immediata su disco
        saveTitleHistory()
    }

    /**
     * SCRITTURA FISICA CRONOLOGIA TITOLI:
     * Serializza la lista in JSON e la scrive nel DataStore in modo asincrono.
     */
    private fun saveTitleHistory() {
        viewModelScope.launch {
            val jsonString = gson.toJson(matchTitleHistory)
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[titleHistoryKey] = jsonString
            }
        }
    }

    /**
     * ====================================================================
     * STATISTICHE AVANZATE: IL DITTATORE 👑
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Trova il giocatore che ha trascorso il maggior numero di turni in prima posizione
     * durante la partita corrente.
     *
     * Logica di Calcolo:
     * 1. Verifica che la lista dei giocatori non sia vuota e che la partita abbia almeno 2 turni.
     * 2. Inizializza una mappa delle presenze in testa `turnsInFirst`.
     * 3. Scansiona ogni turno `t` e trova il punteggio massimo globale `maxScore`.
     * 4. Assegna +1 punto in classifica di leadership a chi possiede il punteggio massimo al turno `t`.
     * 5. Se più giocatori sono a pari merito per turni in testa, si applica lo spareggio
     *    in base al punteggio finale più alto (`maxByOrNull { it.score }`).
     */
    fun getDittatore(): Pair<Player, Int>? {
        if (players.isEmpty()) return null
        val numTurns = players.first().scoreHistory.size
        if (numTurns < 2) return null

        val turnsInFirst = mutableMapOf<Player, Int>()
        players.forEach { turnsInFirst[it] = 0 }

        for (t in 0 until numTurns) {
            // 🧠 ADATTAMENTO PUNTEGGIO MINIMO: Se vince chi fa meno punti, la 1ª posizione al turno t è chi possiede il punteggio MINIMO.
            val bestScoreAtT = if (isLowestScoreWins) {
                players.minOfOrNull { it.scoreHistory[t] } ?: 0
            } else {
                players.maxOfOrNull { it.scoreHistory[t] } ?: 0
            }

            players.forEach { player ->
                if (player.scoreHistory[t] == bestScoreAtT) {
                    turnsInFirst[player] = turnsInFirst[player]!! + 1
                }
            }
        }

        val maxTurns = turnsInFirst.values.maxOrNull() ?: 0
        if (maxTurns == 0) return null

        val candidates = turnsInFirst.filterValues { it == maxTurns }.keys
        val dictator = if (isLowestScoreWins) {
            candidates.minByOrNull { it.score }
        } else {
            candidates.maxByOrNull { it.score }
        }

        return if (dictator != null) Pair(dictator, maxTurns) else null
    }

    /**
     * ====================================================================
     * STATISTICHE AVANZATE: IL PREDATORE 🐺
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Trova il giocatore che ha compiuto il maggior numero di sorpassi in classifica.
     *
     * Definizione di Sorpasso:
     * Un sorpasso avviene al turno `t` quando un giocatore A passa da avere una posizione
     * peggiore rispetto ad un avversario B al turno `t - 1`, ad una posizione migliore al turno `t`.
     */
    fun getPredatore(): Pair<Player, Int>? {
        if (players.size < 2) return null
        val numTurns = players.first().scoreHistory.size
        if (numTurns < 2) return null

        val overtakes = mutableMapOf<Player, Int>()
        players.forEach { overtakes[it] = 0 }

        for (t in 1 until numTurns) {
            for (playerA in players) {
                for (playerB in players) {
                    if (playerA == playerB) continue
                    val scoreAAtT = playerA.scoreHistory[t]
                    val scoreAAtTMinus1 = playerA.scoreHistory[t-1]
                    val scoreBAtT = playerB.scoreHistory[t]
                    val scoreBAtTMinus1 = playerB.scoreHistory[t-1]

                    val isOvertake = if (isLowestScoreWins) {
                        scoreAAtT < scoreBAtT && scoreAAtTMinus1 > scoreBAtTMinus1
                    } else {
                        scoreAAtT > scoreBAtT && scoreAAtTMinus1 < scoreBAtTMinus1
                    }

                    if (isOvertake) {
                        overtakes[playerA] = overtakes[playerA]!! + 1
                    }
                }
            }
        }

        val maxOvertakes = overtakes.values.maxOrNull() ?: 0
        if (maxOvertakes == 0) return null

        val predator = if (isLowestScoreWins) {
            overtakes.filterValues { it == maxOvertakes }.keys.minByOrNull { it.score }
        } else {
            overtakes.filterValues { it == maxOvertakes }.keys.maxByOrNull { it.score }
        }

        return if (predator != null) Pair(predator, maxOvertakes) else null
    }

    /**
     * ====================================================================
     * STATISTICHE AVANZATE: TESTA A TESTA 🤝
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Premio condiviso assegnato esclusivamente al 1° e 2° classificato se lo scarto
     * finale tra loro è <= 1 punto (incluso il pareggio).
     */
    fun getTestaATesta(): Pair<Player, Player>? {
        if (players.size < 2) return null
        val sortedPlayers = if (isLowestScoreWins) {
            players.sortedBy { it.score }
        } else {
            players.sortedByDescending { it.score }
        }
        val p1 = sortedPlayers[0]
        val p2 = sortedPlayers[1]

        val diff = if (isLowestScoreWins) p2.score - p1.score else p1.score - p2.score

        if (diff in 0..1) {
            return Pair(p1, p2)
        }
        return null
    }

    /**
     * ====================================================================
     * STATISTICHE AVANZATE: LA LUMACA 🐌
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Assegnato al giocatore col minor numero di variazioni di punteggio o rimasto
     * fermo per più turni consecutivi.
     */
    fun getLumaca(): Pair<Player, Int>? {
        if (players.isEmpty()) return null
        val numTurns = players.first().scoreHistory.size
        if (numTurns < 2) return null

        var bestSnail: Player? = null
        var minVariations = Int.MAX_VALUE
        var maxConsecutiveZerosTieBreak = -1
        var lowestScoreTieBreak = Int.MAX_VALUE

        for (player in players) {
            val history = player.scoreHistory
            var variations = 0
            var currentConsecutiveZeros = 0
            var maxConsecutiveZeros = 0

            for (t in 1 until numTurns) {
                if (history[t] != history[t-1]) {
                    variations++
                    currentConsecutiveZeros = 0
                } else {
                    currentConsecutiveZeros++
                    if (currentConsecutiveZeros > maxConsecutiveZeros) {
                        maxConsecutiveZeros = currentConsecutiveZeros
                    }
                }
            }
            
            if (variations < minVariations || 
               (variations == minVariations && maxConsecutiveZeros > maxConsecutiveZerosTieBreak) ||
               (variations == minVariations && maxConsecutiveZeros == maxConsecutiveZerosTieBreak && player.score < lowestScoreTieBreak)) {
                
                minVariations = variations
                maxConsecutiveZerosTieBreak = maxConsecutiveZeros
                lowestScoreTieBreak = player.score
                bestSnail = player
            }
        }
        return if (bestSnail != null) Pair(bestSnail, minVariations) else null
    }

    /**
     * ====================================================================
     * STATISTICHE AVANZATE: IL DIESEL 🚂
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Assegnato al giocatore che ha ottenuto almeno il 75% dei suoi punti totali
     * nella seconda metà della partita.
     */
    fun getDiesel(): Pair<Player, Int>? {
        if (players.isEmpty()) return null
        val numTurns = players.first().scoreHistory.size
        if (numTurns < 4) return null

        var bestDiesel: Player? = null
        var maxSecondHalfPercentage = 0.0
        var maxSecondHalfPoints = 0
        
        val halfIndex = numTurns / 2

        for (player in players) {
            val history = player.scoreHistory
            val secondHalfPoints = history.last() - history[halfIndex]
            val totalPoints = history.last() - history[0]
            
            if (totalPoints > 0 && secondHalfPoints > 0) {
                val percentage = secondHalfPoints.toDouble() / totalPoints.toDouble()
                if (percentage >= 0.75) {
                    if (percentage > maxSecondHalfPercentage || 
                       (percentage == maxSecondHalfPercentage && secondHalfPoints > maxSecondHalfPoints)) {
                        maxSecondHalfPercentage = percentage
                        maxSecondHalfPoints = secondHalfPoints
                        bestDiesel = player
                    }
                }
            }
        }
        
        return if (bestDiesel != null) Pair(bestDiesel, (maxSecondHalfPercentage * 100).toInt()) else null
    }

    /**
     * ====================================================================
     * SALVATAGGIO COMPLETO DI TUTTI I DATI NEL DATASTORE
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Questa funzione scrive in modo asincrono nel DataStore di Android sia lo storico delle partite,
     * sia la lista dei giocatori preferiti, sia la cronologia dei titoli personalizzati,
     * sia i preset personalizzati del timer.
     * Viene chiamata subito dopo un'importazione da file di backup per garantire la persistenza locale.
     */
    private fun saveAllToDataStore() {
        viewModelScope.launch {
            val historyJson = gson.toJson(history)
            val favsJson = gson.toJson(favoriteNames)
            val titlesJson = gson.toJson(matchTitleHistory)
            val presetsJson = gson.toJson(timerPresets.toList())

            getApplication<Application>().dataStore.edit { prefs ->
                prefs[historyKey] = historyJson
                prefs[favoritesKey] = favsJson
                prefs[titleHistoryKey] = titlesJson
                prefs[timerPresetsKey] = presetsJson
            }
        }
    }

    /**
     * GENERAZIONE STRINGA JSON DI BACKUP
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Genera la stringa JSON formattata (Pretty-Printing) contenente la fotografia
     * istantanea di tutte le partite, preferiti, titoli e preset del timer dell'app.
     */
    fun generateBackupJsonString(): String {
        val backupData = ScoreCounterBackup(
            version = 1,
            timestamp = System.currentTimeMillis(),
            history = history.toList(),
            favoriteNames = favoriteNames.toList(),
            favoritePlayers = favoritePlayers.toList(), // 🎨 SALVATAGGIO COLORI PREFERITI
            matchTitleHistory = matchTitleHistory.toList(),
            timerPresets = timerPresets.toList()
        )
        val prettyGson = GsonBuilder().setPrettyPrinting().create()
        return prettyGson.toJson(backupData)
    }

    /**
     * ====================================================================
     * ESPORTAZIONE SALVATAGGI IN FILE JSON ESTERNO
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Questa funzione raccoglie l'intera memoria dell'app (storico partite, preferiti, titoli, preset timer)
     * e la trasforma in un file di testo JSON in chiaro e formattato con rientri (Pretty-Printing).
     *
     * @param context Il contesto Android per accedere al ContentResolver di sistema.
     * @param uri L'indirizzo URI del file selezionato o creato dall'utente tramite SAF.
     * @param onSuccess Callback invocata quando l'esportazione si conclude con successo.
     * @param onError Callback invocata in caso di errore di scrittura.
     */
    fun exportBackupToUri(
        context: Context,
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // 🧠 FOTOGRAFIA STATO SUL MAIN THREAD (Snapshot State Isolation):
        // Catturiamo gli elementi delle SnapshotStateList sul thread principale prima di
        // passare al thread I/O. Questo garantisce che le modifiche più recenti (come i preset)
        // siano lette in modo coerente e sicuro senza problemi di concorrenza tra thread.
        val currentHistory = history.toList()
        val currentFavorites = favoriteNames.toList()
        val currentFavoritePlayers = favoritePlayers.toList()
        val currentTitles = matchTitleHistory.toList()
        val currentPresets = timerPresets.toList()

        // Avviamo una coroutine sul thread I/O per non bloccare l'interfaccia grafica durante la scrittura su disco
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Creiamo l'oggetto contenitore del backup con i dati congelati dal Main thread
                val backupData = ScoreCounterBackup(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    history = currentHistory,
                    favoriteNames = currentFavorites,
                    favoritePlayers = currentFavoritePlayers, // 🎨 SALVATAGGIO COLORI PREFERITI
                    matchTitleHistory = currentTitles,
                    timerPresets = currentPresets
                )

                // 2. Usiamo GsonBuilder con PrettyPrinting per creare un file JSON leggibile e pulito
                val prettyGson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = prettyGson.toJson(backupData)

                // 3. Scriviamo il testo nel file esterno tramite lo Storage Access Framework (SAF)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }

                // Torniamo sul thread principale per aggiornare la UI e notificare l'utente
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Errore durante l'esportazione del file")
                }
            }
        }
    }

    /**
     * ====================================================================
     * IMPORTAZIONE E RIPRISTINO SALVATAGGI DA FILE JSON ESTERNO
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA:
     * Questa funzione legge un file JSON esterno selezionato dall'utente e ripristina la memoria dell'app.
     *
     * Supporta due modalità d'importazione:
     * - `isMerge == true` (UNIONE): Aggiunge le partite e i preferiti del file che non sono ancora presenti
     *   nella memoria locale, senza cancellare o duplicare nulla.
     * - `isMerge == false` (SOSTITUZIONE): Cancella i dati attuali e li rimpiazza con quelli del file.
     *
     * @param context Il contesto Android per accedere al ContentResolver.
     * @param uri L'indirizzo URI del file .json selezionato dall'utente.
     * @param isMerge Se true effettua l'unione dei dati, altrimenti sovrascrive tutto.
     * @param onSuccess Callback con il conteggio delle nuove partite importate.
     * @param onError Callback invocata in caso di file corrotto o errore di lettura.
     */
    fun importBackupFromUri(
        context: Context,
        uri: Uri,
        isMerge: Boolean,
        onSuccess: (importedCount: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Lettura dello stream di testo dal file JSON tramite ContentResolver
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw IllegalArgumentException("Impossibile aprire il file di backup")

                // 2. Conversione da stringa JSON a oggetto Kotlin ScoreCounterBackup tramite Gson
                val backupData = gson.fromJson(jsonString, ScoreCounterBackup::class.java)
                    ?: throw IllegalArgumentException("File di backup corrotto o non riconosciuto")

                withContext(Dispatchers.Main) {
                    var newMatchesCount = 0

                    if (isMerge) {
                        // UNIONE INTELLIGENTE (MERGE)
                        // Identifichiamo le partite già presenti usando la combinazione univoca (timestamp + titolo)
                        val existingKeys = history.map { Pair(it.timestamp, it.title) }.toSet()

                        backupData.history.forEach { record ->
                            val key = Pair(record.timestamp, record.title)
                            if (key !in existingKeys) {
                                history.add(0, record) // Inseriamo in cima alla lista dello storico
                                newMatchesCount++
                            }
                        }

                        // Uniamo i giocatori preferiti evitando duplicati (ignorando maiuscole/minuscole)
                        if (!backupData.favoritePlayers.isNullOrEmpty()) {
                            backupData.favoritePlayers.forEach { favPlayer ->
                                val cleanName = favPlayer.name.trim()
                                if (cleanName.isNotEmpty() && favoritePlayers.none { it.name.equals(cleanName, ignoreCase = true) }) {
                                    favoritePlayers.add(FavoritePlayer(cleanName, favPlayer.color))
                                }
                            }
                        } else if (!backupData.favoriteNames.isNullOrEmpty()) {
                            backupData.favoriteNames.forEach { favName ->
                                val cleanName = favName.trim()
                                if (cleanName.isNotEmpty() && favoritePlayers.none { it.name.equals(cleanName, ignoreCase = true) }) {
                                    favoritePlayers.add(FavoritePlayer(cleanName, null))
                                }
                            }
                        }

                        // Uniamo la cronologia dei titoli
                        backupData.matchTitleHistory.forEach { title ->
                            val cleanTitle = title.trim()
                            if (cleanTitle.isNotEmpty() && matchTitleHistory.none { it.equals(cleanTitle, ignoreCase = true) }) {
                                matchTitleHistory.add(cleanTitle)
                            }
                        }

                        // Uniamo/Ripristiniamo i preset personalizzati del timer se presenti nel backup
                        backupData.timerPresets?.let { importedPresets ->
                            if (importedPresets.size == 4) {
                                // Se i preset attuali sono quelli di default oppure se il backup ha preset personalizzati
                                if (timerPresets.toList() == defaultTimerPresets || importedPresets != defaultTimerPresets) {
                                    timerPresets.clear()
                                    timerPresets.addAll(importedPresets)
                                }
                            }
                        }
                    } else {
                        // SOSTITUZIONE COMPLETA (REPLACE)
                        history.clear()
                        history.addAll(backupData.history)

                        favoritePlayers.clear()
                        if (!backupData.favoritePlayers.isNullOrEmpty()) {
                            favoritePlayers.addAll(backupData.favoritePlayers)
                        } else if (!backupData.favoriteNames.isNullOrEmpty()) {
                            favoritePlayers.addAll(backupData.favoriteNames.map { FavoritePlayer(it, null) })
                        }

                        matchTitleHistory.clear()
                        matchTitleHistory.addAll(backupData.matchTitleHistory)

                        // Sostituiamo i preset del timer con quelli del file, o ripristiniamo i default se assenti
                        if (backupData.timerPresets != null && backupData.timerPresets.size == 4) {
                            timerPresets.clear()
                            timerPresets.addAll(backupData.timerPresets)
                        } else {
                            timerPresets.clear()
                            timerPresets.addAll(defaultTimerPresets)
                        }

                        newMatchesCount = backupData.history.size
                    }

                    // 3. Salva immediatamente le novità nel DataStore locale
                    saveAllToDataStore()

                    onSuccess(newMatchesCount)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Formato del file di backup non valido")
                }
            }
        }
    }
}

