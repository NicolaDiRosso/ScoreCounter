package com.n380.scorecounter

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                p.color = record.color//ricordiamo anche il colore del giocatore
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
                    it.scoreHistory.toList(),
                    color = it.color,
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
    fun addPlayer(name: String, color: Int) {
        // 1. Pulizia: Togliamo eventuali spazi vuoti iniziali o finali inseriti per sbaglio (es. " Marco " diventa "Marco")
        val cleanName = name.trim()

        // 2. Controllo: Cerchiamo se nella lista c'è GIÀ qualcuno con questo esatto nome.
        // ignoreCase = true fa sì che "Marco" e "marco" vengano considerati la stessa identica persona!
        val alreadyExists = players.any { it.name.equals(cleanName, ignoreCase = true) }

        // 3. Esecuzione: Aggiungiamo il giocatore SOLO SE non è vuoto E non esiste già al tavolo.
        if (cleanName.isNotEmpty() && !alreadyExists) {
            val newPlayer = Player(cleanName)
            newPlayer.color = color // Assegniamo il colore scelto
            players.add(newPlayer)
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
                    fireComboCount = activePlayer.fireComboCount,
                    color = activePlayer.color// ---> FIX DATABASE: Salviamo il colore su disco per lo Storico
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