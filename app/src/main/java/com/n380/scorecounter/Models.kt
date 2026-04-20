package com.n380.scorecounter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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