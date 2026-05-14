package com.n380.scorecounter.viewmodel

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
import com.n380.scorecounter.dataStore
import com.n380.scorecounter.model.MatchBackup
import com.n380.scorecounter.model.MatchRecord
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.model.PlayerRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    val players = mutableStateListOf<Player>()
    val history = mutableStateListOf<MatchRecord>()
    val favoriteNames = mutableStateListOf<String>()

    // CRONOLOGIA TITOLI: Lista reattiva che memorizza gli ultimi titoli inseriti manualmente
    val matchTitleHistory = mutableStateListOf<String>()

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
    private val titleHistoryKey = stringPreferencesKey("match_title_history")

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

    // ---> FUNZIONI GESTIONE GIOCATORI RAPIDI (PREFERITI) <---
    /**
     * ====================================================================
     * LOGICA DI RIPRISTINO (UNDO) PER I GIOCATORI RAPIDI (PREFERITI)
     * ====================================================================
     * Questa funzione permette di reinserire un nome precedentemente rimosso
     * in una posizione specifica della lista, garantendo che l'ordine originale
     * venga preservato quando l'utente preme "Annulla" sulla Snackbar.
     */
    fun restoreFavorite(index: Int, name: String) {
        // Controllo di sicurezza strutturale: verifichiamo che l'indice rientri nei
        // limiti geometrici attuali della lista. Il range '0..favoriteNames.size'
        // garantisce che possiamo inserire in testa (0), in mezzo, o in coda (size).
        if (index in 0..favoriteNames.size) {
            // L'overload del metodo .add(indice, elemento) sposta tutti gli elementi
            // successivi in avanti di un posto per fare spazio al dato ripristinato.
            favoriteNames.add(index, name)
        } else {
            // Logica di Fallback: se l'indice risulta invalido (es. la lista ha subito
            // altre mutazioni nel frattempo), accodiamo l'elemento alla fine usando
            // il metodo .add(elemento) base. Questo previene crash irreversibili.
            favoriteNames.add(name)
        }

        // PERSISTENZA DEL DATO: La lista osservata dalla UI è stata modificata in RAM.
        // Dobbiamo immediatamente invocare la scrittura fisica su disco per allineare
        // il DataStore, altrimenti chiudendo l'app il ripristino verrebbe perduto.
        saveFavorites()
    }

    /**
     * ====================================================================
     * PERSISTENZA DEI DATI / SALVATTAGGIO IN MEMORIA: LA PIPELINE I/O (Input/Output)
     * ====================================================================
     * Questa funzione illustra i tre pilastri del salvataggio moderno in Android:
     * Asincronicità, Serializzazione e Transazioni Atomiche.
     */
    private fun saveFavorites() {
        // 1. ASINCRONICITÀ (viewModelScope.launch)
        // Scrivere dati sul disco fisico (Memoria Flash del telefono) è un'operazione "lenta"
        // (parliamo di millisecondi, ma in ambito grafico 16ms di ritardo fanno "scattare" l'app).
        // viewModelScope apre un Thread di Background: delega il salvataggio a un "operaio"
        // in modo che il Main Thread (quello che disegna i bottoni) possa continuare a scorrere fluido a 60fps.
        viewModelScope.launch {

            // 2. SERIALIZZAZIONE (gson.toJson)
            // Trasformiamo la MutableStateList (oggetto complesso) in una Stringa JSON.
            // Questo processo è necessario perché il DataStore Preferences è un
            // database "Key-Value" che accetta solo tipi primitivi (String, Int, Boolean).
            // 'favoriteNames' è un oggetto vivo nella RAM (una List<String>). Il disco fisso non capisce
            // cosa sia un oggetto Kotlin, sa solo scrivere puro testo.
            // Gson prende l'oggetto complesso e lo "appiattisce" in una stringa JSON universale.
            // Esempio: ["Marco", "Lucia", "Giovanni"]
            val jsonString = gson.toJson(favoriteNames)

            // 3. TRANSAZIONE ATOMICA (dataStore.edit)
            // 'edit' è una funzione "suspend" che garantisce la Thread-Safety. Se tentiamo di salvare
            // 10 volte nello stesso decimo di secondo, DataStore le mette in coda (queue) evitando file corrotti.
            // Usiamo il paradigma Chiave-Valore (Dictionary):
            // - La Chiave: 'favoritesKey' (Il nome dell'etichetta sul cassetto)
            // - Il Valore: 'jsonString' (Il contenuto del cassetto)
            // - DataStore.edit assicura che il dato venga scritto "tutto o niente", senza errori a metà.
            getApplication<Application>().dataStore.edit { prefs ->

                // Assocciamo la stringa JSON alla chiave univoca definita nel ViewModel.
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

    // Sposta un nome nella lista dei preferiti su o giù
    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        // Controllo di sicurezza: verifichiamo che entrambi gli indici siano validi
        // (Compresi tra 0 e la fine della lista dei preferiti)
        if (fromIndex in favoriteNames.indices && toIndex in favoriteNames.indices) {

            // 1. Estraiamo il nome dalla posizione attuale
            val fav = favoriteNames.removeAt(fromIndex)

            // 2. Lo inseriamo nella nuova posizione desiderata
            favoriteNames.add(toIndex, fav)

            // 3. Salviamo il cambiamento (se hai una funzione specifica per i preferiti,
            // altrimenti qui è dove l'app memorizza l'ordine per il futuro)
            saveFavorites()
        }
    }

    /**
     * LOGICA COMBO ROVENTE E SINCRONIZZAZIONE GLOBALE:
     * Gestisce l'assegnazione punti controllando le serie consecutive.
     * Implementa il "Global Tick" per la sincronizzazione dei grafici a linee.
     */
    fun updatePlayerScore(player: Player, amount: Int) {
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

        // 2. SINCRONIZZAZIONE GRAFICA (GLOBAL TICK)
        // Iteriamo su tutti i giocatori presenti al tavolo, non solo su chi ha segnato.
        // Aggiungiamo alla cronologia di ognuno il proprio punteggio corrente.
        // Se un giocatore non ha segnato in questo turno, registrerà un valore identico
        // al precedente, generando una linea piatta (orizzontale) nel tracciato vettoriale.
        players.forEach { p ->
            p.scoreHistory.add(p.score)
        }
        // Applichiamo la modifica al punteggio e salviamo il backup salva-vita
     //   player.changeScore(amount)
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

    /**
     * ====================================================================
     * RESET INTEGRALE DELLA SESSIONE
     * ====================================================================
     * Questa funzione garantisce che ogni traccia della partita precedente
     * venga eliminata sia dalla RAM che dal Disco Fisso prima di permettere
     * l'avvio di una nuova sfida.
     */
    fun clearMatch() {
        diceSides = 6 //impostiamo come dado di default quello a 6 facce
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
        val excluded = listOf("Sfida Anime", "Sfida Carte")
        if (cleanTitle.isEmpty() || cleanTitle in excluded) return

        // Gestione unicità: se il titolo esiste già, lo rimuoviamo per riportarlo in cima (posizione 0)
        matchTitleHistory.remove(cleanTitle)
        matchTitleHistory.add(0, cleanTitle)

        // Vincolo di capacità: manteniamo solo le ultime 3 voci inserite
        if (matchTitleHistory.size > 3) {
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
}

