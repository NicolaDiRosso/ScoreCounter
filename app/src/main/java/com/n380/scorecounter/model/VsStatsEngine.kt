package com.n380.scorecounter.model

import kotlin.math.abs

/**
 * ====================================================================
 * MOTORE STATISTICO TESTA A TESTA: VsStatsEngine
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE (CLEAN ARCHITECTURE & KOTLIN FUNZIONALE):
 *
 * 1. SEPARAZIONE DELLE RESPONSABILITÀ (Pure Business Logic):
 *    Questo file contiene esclusivamente funzioni pure Kotlin e strutture dati
 *    immutabili (`data class`). Non importa nulla da Android UI o Jetpack Compose:
 *    in questo modo la matematica è isolata, leggibile e riutilizzabile sia
 *    dalla vista Portrait sia da quella Landscape.
 *
 * 2. RETROCOMPATIBILITÀ ZERO-MIGRATION:
 *    Tutti i calcoli leggono esclusivamente i campi già salvati su disco in
 *    `MatchRecord` e `PlayerRecord` (`allPlayers`, `isLowestScoreWins`,
 *    `durationSeconds`, `fireComboCount`, `scoreHistory`), riutilizzando
 *    direttamente le Extension Functions dei premi definite in `Models.kt`.
 * ====================================================================
 */

/**
 * Rappresenta un giocatore selezionabile nei menu a tendina del confronto VS,
 * corredato del suo colore ARGB più recente e del totale partite disputate.
 */
data class VsPlayerOption(
    val name: String,
    val color: Int,
    val totalMatchesPlayed: Int
)

/**
 * 🧠 TEORIA KOTLIN (enum class vs Stringhe hardcoded):
 * Usiamo un `enum class` per identificare i 9 premi storici di ScoreCounter.
 * Perché non usare direttamente le stringhe tradotte qui dentro?
 * Perché il livello `model` non deve dipendere dal `Context` Android né da `R.string`:
 * sarà poi la UI (`VsComparisonContent.kt`) a tradurre ogni voce tramite `stringResource()`.
 */
enum class VsAwardType {
    CECCHINO,
    INARRESTABILE,
    GAMBERO,
    FENICE,
    DITTATORE,
    PREDATORE,
    TESTA_A_TESTA,
    LUMACA,
    DIESEL
}

/**
 * Riga comparativa di un singolo premio conquistato da P1 e P2 nelle sfide condivise.
 */
data class VsAwardComparisonRow(
    val type: VsAwardType,
    val p1Count: Int,
    val p2Count: Int
)

/**
 * Contenitore immutabile con tutti i risultati calcolati del confronto diretto tra P1 e P2.
 */
data class HeadToHeadStats(
    val player1Name: String,
    val player1Color: Int,
    val player2Name: String,
    val player2Color: Int,
    val sharedMatchesCount: Int,
    val p1DirectWins: Int,
    val p2DirectWins: Int,
    val draws: Int,
    val p1OverallMatchWins: Int,
    val p2OverallMatchWins: Int,
    val p1AverageScore: Double,
    val p2AverageScore: Double,
    val biggestMargin: Int,
    val biggestMarginWinnerName: String?,
    val biggestMarginMatchTitle: String?,
    val totalSharedSeconds: Long,
    val p1OnFireTotal: Int,
    val p2OnFireTotal: Int,
    val awardsComparison: List<VsAwardComparisonRow>
)

/**
 * Estrae tutti i giocatori unici presenti nello storico partite, ordinati dal più attivo al meno attivo.
 *
 * 🧠 PASSAGGI DELL'ALGORITMO:
 * 1. `flatMap { it.allPlayers }`: appiattisce tutte le liste giocatori di tutte le partite in un'unica lista.
 * 2. `groupBy { it.name.trim().lowercase() }`: raggruppa per nome ignorando maiuscole/minuscole,
 *    così "Nicola" e "nicola" vengono riconosciuti come la stessa persona.
 * 3. `records.first()`: poiché `history` salva le partite più recenti in cima (indice 0),
 *    il primo record del gruppo contiene automaticamente il colore più aggiornato del giocatore.
 */
fun extractAvailableVsPlayers(history: List<MatchRecord>): List<VsPlayerOption> {
    if (history.isEmpty()) return emptyList()

    val groupedByName = history
        .flatMap { it.allPlayers }
        .filter { it.name.isNotBlank() }
        .groupBy { it.name.trim().lowercase() }

    return groupedByName.values
        .map { records ->
            val latestRecord = records.first()
            VsPlayerOption(
                name = latestRecord.name.trim(),
                color = latestRecord.color,
                totalMatchesPlayed = records.size
            )
        }
        .sortedWith(
            compareByDescending<VsPlayerOption> { it.totalMatchesPlayed }
                .thenBy { it.name.lowercase() }
        )
}

/**
 * Calcola tutte le statistiche incrociate tra `p1Name` e `p2Name` analizzando
 * esclusivamente le partite in cui entrambi erano presenti contemporaneamente al tavolo.
 */
fun calculateHeadToHeadStats(
    p1Name: String,
    p2Name: String,
    history: List<MatchRecord>
): HeadToHeadStats {
    // 1. FILTRO SFIDE CONDIVISE:
    // Teniamo solo i MatchRecord in cui compaiono SIA il Giocatore 1 SIA il Giocatore 2.
    val sharedMatches = history.filter { match ->
        val hasP1 = match.allPlayers.any { it.name.equals(p1Name, ignoreCase = true) }
        val hasP2 = match.allPlayers.any { it.name.equals(p2Name, ignoreCase = true) }
        hasP1 && hasP2
    }

    // Recuperiamo i colori più recenti dei due sfidanti dallo storico generale (con paracadute ?: se assenti)
    val p1Color = history.flatMap { it.allPlayers }
        .firstOrNull { it.name.equals(p1Name, ignoreCase = true) }?.color ?: 0xFF2196F3.toInt()
    val p2Color = history.flatMap { it.allPlayers }
        .firstOrNull { it.name.equals(p2Name, ignoreCase = true) }?.color ?: 0xFFF44336.toInt()

    // Se non hanno mai giocato insieme, restituiamo subito un oggetto azzerato evitando divisioni per zero
    if (sharedMatches.isEmpty()) {
        return HeadToHeadStats(
            player1Name = p1Name,
            player1Color = p1Color,
            player2Name = p2Name,
            player2Color = p2Color,
            sharedMatchesCount = 0,
            p1DirectWins = 0,
            p2DirectWins = 0,
            draws = 0,
            p1OverallMatchWins = 0,
            p2OverallMatchWins = 0,
            p1AverageScore = 0.0,
            p2AverageScore = 0.0,
            biggestMargin = 0,
            biggestMarginWinnerName = null,
            biggestMarginMatchTitle = null,
            totalSharedSeconds = 0L,
            p1OnFireTotal = 0,
            p2OnFireTotal = 0,
            awardsComparison = emptyList()
        )
    }

    var p1DirectWins = 0
    var p2DirectWins = 0
    var draws = 0
    var p1OverallMatchWins = 0
    var p2OverallMatchWins = 0

    var p1ScoreSum = 0L
    var p2ScoreSum = 0L

    var biggestMargin = 0
    var biggestMarginWinnerName: String? = null
    var biggestMarginMatchTitle: String? = null

    var p1OnFireTotal = 0
    var p2OnFireTotal = 0

    // 🧠 TEORIA KOTLIN (`withDefault { 0 }`):
    // Creando una mappa con valore predefinito 0, `p1Awards.getValue(type)` non restituisce mai `null`
    // anche se quel premio non è ancora stato vinto, semplificando l'incremento `+ 1`.
    val p1Awards = mutableMapOf<VsAwardType, Int>().withDefault { 0 }
    val p2Awards = mutableMapOf<VsAwardType, Int>().withDefault { 0 }

    // Funzione locale di supporto: assegna +1 premio al giocatore corrispondente se presente
    fun registerSingleAward(type: VsAwardType, winnerPlayerName: String?) {
        if (winnerPlayerName == null) return
        if (winnerPlayerName.equals(p1Name, ignoreCase = true)) {
            p1Awards[type] = p1Awards.getValue(type) + 1
        } else if (winnerPlayerName.equals(p2Name, ignoreCase = true)) {
            p2Awards[type] = p2Awards.getValue(type) + 1
        }
    }

    for (match in sharedMatches) {
        val p1Record = match.allPlayers.first { it.name.equals(p1Name, ignoreCase = true) }
        val p2Record = match.allPlayers.first { it.name.equals(p2Name, ignoreCase = true) }

        // A. SCONTRO DIRETTO TRA I DUE (Chi si è piazzato meglio tra P1 e P2?):
        // Rispettiamo fedelmente la regola della singola partita (`isLowestScoreWins`:
        // se true vince il punteggio minore, altrimenti vince il punteggio maggiore).
        when {
            p1Record.score == p2Record.score -> draws++
            match.isLowestScoreWins -> {
                if (p1Record.score < p2Record.score) p1DirectWins++ else p2DirectWins++
            }
            else -> {
                if (p1Record.score > p2Record.score) p1DirectWins++ else p2DirectWins++
            }
        }

        // B. VITTORIE ASSOLUTE AL TAVOLO (1° posto generale nella partita condivisa):
        // Utile nelle partite con 3 o più giocatori per distinguere chi ha vinto l'intero tavolo.
        val overallWinners = match.getWinnerNames()
        if (overallWinners.any { it.equals(p1Name, ignoreCase = true) }) p1OverallMatchWins++
        if (overallWinners.any { it.equals(p2Name, ignoreCase = true) }) p2OverallMatchWins++

        // C. ACCUMULO PUNTEGGI E COMBO "ON FIRE"
        p1ScoreSum += p1Record.score
        p2ScoreSum += p2Record.score
        p1OnFireTotal += p1Record.fireComboCount
        p2OnFireTotal += p2Record.fireComboCount

        // D. SCARTO MASSIMO DI PUNTI NELLO STESSO MATCH:
        // `abs(...)` calcola la distanza assoluta tra i due punteggi, indipendentemente dal segno.
        val margin = abs(p1Record.score - p2Record.score)
        if (margin > biggestMargin) {
            biggestMargin = margin
            biggestMarginMatchTitle = match.title
            val p1WonDuel = if (match.isLowestScoreWins) {
                p1Record.score < p2Record.score
            } else {
                p1Record.score > p2Record.score
            }
            biggestMarginWinnerName = if (p1WonDuel) p1Name else p2Name
        }

        // E. CONTEGGIO DEI 9 PREMI STORICI NELLE SOLE PARTITE CONDIVISE:
        // Riutilizziamo direttamente le Extension Functions di MatchRecord definite in Models.kt!
        registerSingleAward(VsAwardType.CECCHINO, match.getHistoricalCecchino()?.first?.name)
        registerSingleAward(VsAwardType.INARRESTABILE, match.getHistoricalInarrestabile()?.first?.name)
        registerSingleAward(VsAwardType.GAMBERO, match.getHistoricalGambero()?.first?.name)
        registerSingleAward(VsAwardType.FENICE, match.getHistoricalFenice()?.first?.name)
        registerSingleAward(VsAwardType.DITTATORE, match.getHistoricalDittatore()?.first?.name)
        registerSingleAward(VsAwardType.PREDATORE, match.getHistoricalPredatore()?.first?.name)
        registerSingleAward(VsAwardType.LUMACA, match.getHistoricalLumaca()?.first?.name)
        registerSingleAward(VsAwardType.DIESEL, match.getHistoricalDiesel()?.first?.name)

        // Il premio Testa a Testa è l'unico premio condiviso: viene assegnato a entrambi i finalisti
        match.getHistoricalTestaATesta()?.let { (first, second) ->
            registerSingleAward(VsAwardType.TESTA_A_TESTA, first.name)
            registerSingleAward(VsAwardType.TESTA_A_TESTA, second.name)
        }
    }

    val count = sharedMatches.size.toDouble()

    // Filtriamo solo i premi conquistati almeno una volta da uno dei due per evitare righe "0 - 0"
    val awardsList = VsAwardType.entries.map { awardType ->
        VsAwardComparisonRow(
            type = awardType,
            p1Count = p1Awards.getValue(awardType),
            p2Count = p2Awards.getValue(awardType)
        )
    }.filter { it.p1Count > 0 || it.p2Count > 0 }

    return HeadToHeadStats(
        player1Name = p1Name,
        player1Color = p1Color,
        player2Name = p2Name,
        player2Color = p2Color,
        sharedMatchesCount = sharedMatches.size,
        p1DirectWins = p1DirectWins,
        p2DirectWins = p2DirectWins,
        draws = draws,
        p1OverallMatchWins = p1OverallMatchWins,
        p2OverallMatchWins = p2OverallMatchWins,
        p1AverageScore = p1ScoreSum / count,
        p2AverageScore = p2ScoreSum / count,
        biggestMargin = biggestMargin,
        biggestMarginWinnerName = biggestMarginWinnerName,
        biggestMarginMatchTitle = biggestMarginMatchTitle,
        totalSharedSeconds = sharedMatches.sumOf { it.durationSeconds },
        p1OnFireTotal = p1OnFireTotal,
        p2OnFireTotal = p2OnFireTotal,
        awardsComparison = awardsList
    )
}

