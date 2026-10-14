package com.n380.scorecounter.model

    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableIntStateOf
    import androidx.compose.runtime.mutableStateListOf
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.setValue
    import kotlin.math.abs

// 1. IL MODELLO DATI (I Giocatori Attivi durante la partita corrente)
    // Usiamo "var name by mutableStateOf" invece del semplice "val".
    // Se fosse stato "val", Kotlin avrebbe vietato le modifiche testuali una volta creato l'oggetto.
    // "mutableStateOf" è lo Stato Magico di Compose: se tu cambi questo valore nel ViewModel,
    // Compose si accorge del cambiamento e ridisegna immediatamente l'interfaccia ovunque appaia questo nome.
    class Player(initialName: String) {
        var name by mutableStateOf(initialName)
        var color by mutableStateOf(0xFF757575.toInt()) // Grigio di default
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
            //scoreHistory.add(score) non salviamo più qui la cronologia perchè è un copito che spetta al ViewModel
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
        val fireComboCount: Int = 0, // Serve per salvare il conteggio sul disco fisso
        val color: Int = 0xFF757575.toInt()//campo colore per far capiere a Gson dove solvare il dato
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

    // ====================================================================
    // MODELLO DATI PER IL BACKUP ED ESPORTAZIONE COMPLETA (JSON IN CHIARO)
    // ====================================================================
    // 🧠 SPIEGAZIONE DIDATTICA:
    // Questa data class racchiude l'intera memoria dell'applicazione in un unico contenitore.
    // Viene usata sia per generare il file di backup .json leggibile esternamente, sia per ripristinare
    // lo storico completo, i giocatori preferiti e i titoli personalizzati quando si reinstalla l'app.
    data class ScoreCounterBackup(
        val version: Int = 1,                             // Versione del formato per controlli di compatibilità
        val timestamp: Long = System.currentTimeMillis(), // Millisecondi al momento della creazione del backup
        val history: List<MatchRecord> = emptyList(),    // Lista di tutte le partite storiche concluse
        val favoriteNames: List<String> = emptyList(),   // Lista dei nomi dei giocatori salvati tra i preferiti
        val matchTitleHistory: List<String> = emptyList()// Lista dei titoli delle sfide usati di frequente
    )

    // ====================================================================
    // LOGICA STORICA: CALCOLO PREMI SUI SALVATAGGI (EXTENSION FUNCTIONS)
    // ====================================================================
    // 🧠 TEORIA KOTLIN: Le Extension Functions
    // Invece di sporcare il ViewModel con funzioni duplicate, Kotlin ci permette di "estendere"
    // una classe esistente (MatchRecord) aggiungendole nuove capacità.
    // Ora ogni variabile di tipo MatchRecord avrà i metodi .getHistoricalCecchino(), ecc.

    fun MatchRecord.getHistoricalCecchino(): Pair<PlayerRecord, Int>? {
        var sniper: PlayerRecord? = null
        var maxJump = 0

        for (player in allPlayers) {
            // 🧠 NULL SAFETY E RETROCOMPATIBILITÀ:
            // L'operatore Elvis '?:' dice: "Se scoreHistory è null (perché il salvataggio è vecchio),
            // non crashare, usa semplicemente una lista vuota e vai avanti".
            val history = player.scoreHistory ?: emptyList()
            if (history.size < 2) continue

            val maxPlayerJump = history.zipWithNext { prev, curr -> curr - prev }.maxOrNull() ?: 0
            if (maxPlayerJump > maxJump && maxPlayerJump > 0) {
                maxJump = maxPlayerJump
                sniper = player
            }
        }
        return if (sniper != null) Pair(sniper, maxJump) else null
    }

    fun MatchRecord.getHistoricalInarrestabile(): Pair<PlayerRecord, Int>? {
        var mvp: PlayerRecord? = null
        var maxCombo = 0
        for (player in allPlayers) {
            // fireComboCount ha un valore di default a 0 nei vecchi salvataggi,
            // quindi non rischiamo crash, varrà semplicemente 0.
            if (player.fireComboCount > maxCombo) {
                maxCombo = player.fireComboCount
                mvp = player
            }
        }
        return if (mvp != null) Pair(mvp, maxCombo) else null
    }

    fun MatchRecord.getHistoricalGambero(): Pair<PlayerRecord, Int>? {
        var worstPlayer: PlayerRecord? = null
        var maxPointsLost = 0
        for (player in allPlayers) {
            val history = player.scoreHistory ?: emptyList()
            if (history.size < 2) continue

            val pointsLost = history.zipWithNext { prev, curr -> curr - prev }
                .filter { it < 0 }
                .sum()

            val absoluteLost = abs(pointsLost)
            if (absoluteLost > maxPointsLost) {
                maxPointsLost = absoluteLost
                worstPlayer = player
            }
        }
        return if (worstPlayer != null) Pair(worstPlayer, maxPointsLost) else null
    }

    fun MatchRecord.getHistoricalFenice(): Pair<PlayerRecord, Int>? {
        var comebackPlayer: PlayerRecord? = null
        var maxRecovery = 0
        for (player in allPlayers) {
            val history = player.scoreHistory ?: emptyList()
            if (history.isEmpty()) continue

            val lowestScore = history.minOrNull() ?: 0
            val recovery = player.score - lowestScore

            if (recovery > maxRecovery && recovery > 0) {
                maxRecovery = recovery
                comebackPlayer = player
            }
        }
        return if (comebackPlayer != null) Pair(comebackPlayer, maxRecovery) else null
    }

    /**
     * ====================================================================
     * LOGICA STORICA: IL DITTATORE 👑 (Extension Function per MatchRecord)
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Il premio "Il Dittatore" viene assegnato al giocatore che è rimasto in 1ª posizione
     * (da solo o a pari merito) per il maggior numero di turni durante l'intera sfida.
     *
     * Passo-passo:
     * 1. Verifichiamo che la lista dei giocatori non sia vuota e che vi siano almeno 2 turni.
     * 2. Inizializziamo una mappa `turnsInFirst` dove la chiave è il giocatore e il valore è il contatore di turni in testa.
     * 3. Per ciascun turno `t` da 0 a `numTurns - 1`:
     *    - Calcoliamo il punteggio massimo registrato al turno `t` con `maxOfOrNull`.
     *    - Assegniamo +1 punto in classifica a tutti i giocatori che al turno `t` avevano quel punteggio massimo.
     * 4. Estraiamo il valore massimo di turni in testa (`maxTurns`). Se `maxTurns` è 0, nessuno ha guidato la partita.
     * 5. In caso di parità di turni in prima posizione tra due o più giocatori, applichiamo la regola di
     *    spareggio ufficiale: vince chi ha il punteggio finale complessivo più alto (`maxByOrNull { it.score }`).
     *
     * @return Una coppia `Pair(PlayerRecord, Int)` col giocatore e il numero di turni in testa, oppure `null`.
     */
    fun MatchRecord.getHistoricalDittatore(): Pair<PlayerRecord, Int>? {
        if (allPlayers.isEmpty()) return null
        val numTurns = allPlayers.first().scoreHistory?.size ?: 0
        if (numTurns < 2) return null

        val turnsInFirst = mutableMapOf<PlayerRecord, Int>()
        allPlayers.forEach { turnsInFirst[it] = 0 }

        for (t in 0 until numTurns) {
            val maxScore = allPlayers.maxOfOrNull { it.scoreHistory?.getOrNull(t) ?: 0 } ?: 0
            allPlayers.forEach { player ->
                if ((player.scoreHistory?.getOrNull(t) ?: 0) == maxScore) {
                    turnsInFirst[player] = turnsInFirst[player]!! + 1
                }
            }
        }

        val maxTurns = turnsInFirst.values.maxOrNull() ?: 0
        if (maxTurns == 0) return null

        val candidates = turnsInFirst.filterValues { it == maxTurns }.keys
        val dictator = candidates.maxByOrNull { it.score }

        return if (dictator != null) Pair(dictator, maxTurns) else null
    }

    /**
     * ====================================================================
     * LOGICA STORICA: IL PREDATORE 🐺 (Extension Function per MatchRecord)
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Il premio "Il Predatore" celebra l'aggressività tattica: premia il giocatore che ha
     * compiuto il maggior numero di "sorpassi" diretti in classifica durante la partita.
     *
     * Definizione matematica di Sorpasso al turno `t`:
     * Un giocatore A effettua un sorpasso sul giocatore B al turno `t` se e solo se:
     * - Al turno precedente `t - 1`, il punteggio di A era STRETTAMENTE MINORE del punteggio di B (`A < B`).
     * - Al turno attuale `t`, il punteggio di A è diventato STRETTAMENTE MAGGIORE del punteggio di B (`A > B`).
     *
     * Passo-passo:
     * 1. Iteriamo su tutti i turni dal secondo in poi (`t` da 1 a `numTurns - 1`).
     * 2. Confrontiamo ogni coppia ordinata di giocatori `(A, B)`.
     * 3. Se la condizione di sorpasso si verifica, incrementiamo il contatore dei sorpassi per il giocatore A.
     * 4. Troviamo il numero massimo di sorpassi totalizzati. In caso di parità, vince chi ha il punteggio finale più alto.
     *
     * @return Una coppia `Pair(PlayerRecord, Int)` col giocatore e il numero di sorpassi effettuati, oppure `null`.
     */
    fun MatchRecord.getHistoricalPredatore(): Pair<PlayerRecord, Int>? {
        if (allPlayers.size < 2) return null
        val numTurns = allPlayers.first().scoreHistory?.size ?: 0
        if (numTurns < 2) return null

        val overtakes = mutableMapOf<PlayerRecord, Int>()
        allPlayers.forEach { overtakes[it] = 0 }

        for (t in 1 until numTurns) {
            for (playerA in allPlayers) {
                for (playerB in allPlayers) {
                    if (playerA == playerB) continue
                    val scoreAtT = playerA.scoreHistory?.getOrNull(t) ?: 0
                    val scoreAtTMinus1 = playerA.scoreHistory?.getOrNull(t-1) ?: 0
                    val scoreBAtT = playerB.scoreHistory?.getOrNull(t) ?: 0
                    val scoreBAtTMinus1 = playerB.scoreHistory?.getOrNull(t-1) ?: 0
                    if (scoreAtT > scoreBAtT && scoreAtTMinus1 < scoreBAtTMinus1) {
                        overtakes[playerA] = overtakes[playerA]!! + 1
                    }
                }
            }
        }

        val maxOvertakes = overtakes.values.maxOrNull() ?: 0
        if (maxOvertakes == 0) return null

        val predator = overtakes.filterValues { it == maxOvertakes }.keys.maxByOrNull { it.score }
        
        return if (predator != null) Pair(predator, maxOvertakes) else null
    }

    /**
     * ====================================================================
     * LOGICA STORICA: TESTA A TESTA 🤝 (Extension Function per MatchRecord)
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Questo è l'unico PREMIO CONDIVISO della partita. Viene assegnato CONTEMPORANEAMENTE
     * al 1° e al 2° classificato se e solo se la partita è stata tiratissima fino all'ultimo punto!
     *
     * Requisito Matematico:
     * Distacco finale tra il primo classificato (`p1`) e il secondo classificato (`p2`) <= 1 punto
     * (incluso il caso di pareggio perfetto dove il distacco è 0).
     *
     * @return Una coppia `Pair(PlayerRecord, PlayerRecord)` con i due giocatori del duello, oppure `null`.
     */
    fun MatchRecord.getHistoricalTestaATesta(): Pair<PlayerRecord, PlayerRecord>? {
        if (allPlayers.size < 2) return null
        val sortedPlayers = allPlayers.sortedByDescending { it.score }
        val p1 = sortedPlayers[0]
        val p2 = sortedPlayers[1]
        
        if (p1.score - p2.score <= 1) {
            return Pair(p1, p2)
        }
        return null
    }

    /**
     * ====================================================================
     * LOGICA STORICA: LA LUMACA 🐌 (Extension Function per MatchRecord)
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Il premio "La Lumaca" identifica il giocatore più lento o passivo della partita.
     *
     * Criteri di calcolo sequenziali:
     * 1. Minor numero di variazioni di punteggio (conteggio delle volte in cui `scoreHistory[t] != scoreHistory[t-1]`).
     * 2. Primo criterio di spareggio: Maggior numero di turni CONSECUTIVI trascorsi fermo a zero variazioni.
     * 3. Secondo criterio di spareggio: Punteggio finale complessivo più basso (ritmo di crescita inferiore).
     *
     * @return Una coppia `Pair(PlayerRecord, Int)` col giocatore e il numero di variazioni effettuate, oppure `null`.
     */
    fun MatchRecord.getHistoricalLumaca(): Pair<PlayerRecord, Int>? {
        if (allPlayers.isEmpty()) return null
        val numTurns = allPlayers.first().scoreHistory?.size ?: 0
        if (numTurns < 2) return null

        var bestSnail: PlayerRecord? = null
        var minVariations = Int.MAX_VALUE
        var maxConsecutiveZerosTieBreak = -1
        var lowestScoreTieBreak = Int.MAX_VALUE

        for (player in allPlayers) {
            val history = player.scoreHistory ?: emptyList()
            var variations = 0
            var currentConsecutiveZeros = 0
            var maxConsecutiveZeros = 0

            for (t in 1 until history.size) {
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
     * LOGICA STORICA: IL DIESEL 🚂 (Extension Function per MatchRecord)
     * ====================================================================
     * 🧠 SPIEGAZIONE DIDATTICA DELL'ALGORITMO:
     * Premiamo il giocatore "a scoppio ritardato": colui che ha carburato tardi ma ha fatto
     * un'esplosione nel finale.
     *
     * Condizione vincolante:
     * Almeno il 75% dei punti totali guadagnati durante la partita deve essere stato
     * ottenuto nella SECONDA METÀ della cronologia dei turni.
     *
     * Calcolo Matematico:
     * - `halfIndex = numTurns / 2`
     * - `secondHalfPoints = history.last() - history[halfIndex]`
     * - `totalPoints = history.last() - history[0]`
     * - `percentage = secondHalfPoints / totalPoints`
     * - Se `percentage >= 0.75` (75%), il giocatore è idoneo!
     *
     * @return Una coppia `Pair(PlayerRecord, Int)` col giocatore e la percentuale in numero intero (es. 85%), oppure `null`.
     */
    fun MatchRecord.getHistoricalDiesel(): Pair<PlayerRecord, Int>? {
        if (allPlayers.isEmpty()) return null
        val numTurns = allPlayers.first().scoreHistory?.size ?: 0
        if (numTurns < 4) return null

        var bestDiesel: PlayerRecord? = null
        var maxSecondHalfPercentage = 0.0
        var maxSecondHalfPoints = 0
        
        val halfIndex = numTurns / 2

        for (player in allPlayers) {
            val history = player.scoreHistory ?: emptyList()
            if (history.size < numTurns) continue
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