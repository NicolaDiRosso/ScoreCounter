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