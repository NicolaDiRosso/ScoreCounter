package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.* // Strumenti per l'animazione infinita
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawWithContent // Per disegnare la luce
import androidx.compose.ui.geometry.Offset // Per le coordinate del raggio luminoso
import androidx.compose.ui.graphics.Brush // Per creare la sfumatura di luce
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.viewmodel.MatchViewModel

/**
 * ====================================================================
 * SCHERMATA DELLE STATISTICHE GLOBALI (Data Analysis)
 * Questa schermata analizza l'intero database dello storico per estrarre curiosità e record.
 * ====================================================================
 */
@Composable
fun GlobalStatsScreen(
    viewModel: MatchViewModel,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // ====================================================================
    // ---> LA LOGICA DEI CALCOLI (MATEMATICA DIETRO LE QUINTE) <---
    // Qui usiamo le funzioni "Collection" di Kotlin per analizzare l'intero Database (history).
    // ====================================================================

    // 1. PARTITE TOTALI: Semplicemente la dimensione (size) della lista dello storico.
    //.size' conta semplicemente quanti elementi (partite) ci sono nella lista dello storico.
    val totalMatches = viewModel.history.size

    // 2. TEMPO TOTALE GIOCATO:
    // '.sumOf' scorre automaticamente tutte le partite, prende la variabile 'durationSeconds'
    // di ognuna di esse e le somma tutte insieme in un colpo solo.
    val totalSeconds = viewModel.history.sumOf { it.durationSeconds }

    // 3. CAMPIONE ASSOLUTO (Chi ha vinto più partite in assoluto?):
    // Questo calcolo si fa in due passaggi.
    // PASSAGGIO A: groupingBy { it.winnerName }.eachCount()
    // Prende le partite, le raggruppa creando delle "scatole" col nome del vincitore e conta quante partite ci sono in ogni scatola.
    // Il risultato è una Mappa (Dizionario) fatta così -> ["Nicola": 5, "Marco": 2, "Anna": 8]
    val winsMap = viewModel.history.groupingBy { it.winnerName }.eachCount()
    // PASSAGGIO B: maxByOrNull { it.value }
    // Guarda dentro la Mappa appena creata, cerca il valore (.value) più alto (l'8 di Anna) e salva quell'elemento.
    // Se la lista è vuota, 'OrNull' evita che l'app crashi e restituisce semplicemente "niente".
    val bestPlayer = winsMap.maxByOrNull { it.value }

    // 4. RECORD DI PUNTI (Il punteggio più alto mai registrato da un vincitore):
    // maxByOrNull scansiona tutto lo storico e trova LA PARTITA in cui 'winningScore' era il più alto in assoluto.
    val highestScoreRecord = viewModel.history.maxByOrNull { it.winningScore }


    // 5. LA PARTITA INFINITA (La partita più lunga in assoluto)
    // 1. filter: Filtriamo lo storico tenendo SOLO le partite che hanno almeno 1 secondo (ignoriamo quelle finite subito).
    // 2. maxByOrNull: Tra queste, troviamo quella con il numero di 'durationSeconds' più alto in assoluto!
    val longestMatch = viewModel.history.filter { it.durationSeconds > 0 }.maxByOrNull { it.durationSeconds }


    // 6. IL DITTATORE (Vittoria col maggior distacco)
    // 1. Filtriamo le partite tenendo solo quelle che hanno almeno 2 giocatori.
    // 2. maxByOrNull calcola la differenza (distacco) tra il 1° e il 2° classificato e trova il valore più alto!
    val dictatorMatch = viewModel.history
        .filter { it.allPlayers.size >= 2 }
        .maxByOrNull { match ->
            // Essendo la lista già ordinata per punteggio durante il salvataggio,
            // il 1° è sempre allPlayers[0] e il 2° è sempre allPlayers[1]
            match.allPlayers[0].score - match.allPlayers[1].score
        }

    // ====================================================================
    // ---> NOVITÀ: 7. IL PIROMANE (Più combo "On Fire" in assoluto) <---
    // ====================================================================
    // 1. Creiamo una lista piatta di TUTTI i giocatori che sono mai esistiti in ogni partita dello storico
    val allHistoricalPlayers = viewModel.history.flatMap { it.allPlayers }

    // 2. Raggruppiamo per nome e sommiamo tutti i loro 'fireComboCount'
    val fireStatsMap = allHistoricalPlayers
        .groupBy { it.name }
        .mapValues { entry -> entry.value.sumOf { it.fireComboCount } }

    // 3. Troviamo chi ha il totale più alto (Il Piromane Supremo)
    val topArsonist = fireStatsMap.maxByOrNull { it.value }

    // Calcoliamo e salviamo in memoria di quanti punti esatti è stato questo distacco record
    val dictatorMargin = if (dictatorMatch != null) {
        dictatorMatch.allPlayers[0].score - dictatorMatch.allPlayers[1].score
    } else 0



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Usiamo il nostro solito trucco per far vedere il pattern di icone sullo sfondo!
        containerColor = Color.Transparent,

        ) { innerPadding ->

        // Usiamo una LazyColumn per permettere lo scorrimento se gli schermi sono piccoli.
        // Arrangement.spacedBy(16.dp) separa elegantemente le Card tra di loro.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)//12 corrisponde allo spazio tra le carte
        ) {

            // ==============================================================
            // ---> INTESTAZIONE DELLA PAGINA (Ora è al sicuro sotto l'orologio!) <---
            // ==============================================================
            item {
                // ---> MODIFICA ESTETICA: ALLINEAMENTO E SPAZIATURA <---
                // 1. Aggiungiamo uno Spacer di 16.dp in altezza. Questo garantisce che la distanza
                // dal centro notifiche (orologio/batteria) sia IDENTICA a quella di HomeScreen e CreateMatchScreen.
                Spacer(modifier = Modifier.height(16.dp))

                // Abbiamo rimosso la Row() che non serviva a nulla.
                Text(
                    text = "Statistiche partite",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    // 2. Rimosso 'start = 16.dp' per evitare il "doppio padding" e allinearlo perfettamente a sinistra.
                    // 3. Aggiunto 'bottom = 24.dp' per distaccarlo elegantemente dalla prima carta sottostante.
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // ==============================================================
            // --- PRIMA CARD: IL CAMPIONE ASSOLUTO (Chi vince di più) ---
            // ==============================================================
            item {
                // ---> EFFETTO CARTA RARA (Shimmer Sweep) <---
                // 1. IL MOTORE DELL'ANIMAZIONE INFINITA
                val infiniteTransition = rememberInfiniteTransition(label = "shimmer_campione")
                val translateAnim by infiniteTransition.animateFloat(
                    initialValue = -500f,
                    targetValue = 2000f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 4500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmer_translation_campione"
                )

                // 2. IL FASCIO DI LUCE: Usiamo il colore primario del tema per farlo brillare
                val shimmerColor = MaterialTheme.colorScheme.primary
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        shimmerColor.copy(alpha = 0.3f), // Raggio luminoso al 30%
                        Color.Transparent
                    ),
                    start = Offset(translateAnim, translateAnim),
                    end = Offset(translateAnim + 400f, translateAnim + 400f)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // ---> MODIFICA ESTETICA: Spennelliamo la luce sopra la carta <---
                        .drawWithContent {
                            drawContent() // Disegna testo e icone normalmente
                            drawRect(brush = shimmerBrush) // Passa il raggio di luce sopra a tutto!
                        },
                    // Colore speciale: Usiamo il primaryContainer per farla risaltare e darle un effetto "Oro/Premio"
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally // Centra tutto perfettamente
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Text("CAMPIONE ASSOLUTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.padding(top = 8.dp))

                        // Stampiamo il NOME del giocatore con più vittorie.
                        // '?.' è una protezione: se 'bestPlayer' è nullo (nessuno ha mai giocato), stampa "Nessuno".
                        Text(
                            text = bestPlayer?.key ?: "Nessuno",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Stampiamo il NUMERO di vittorie di quel giocatore.
                        Text(
                            text = "Con ${bestPlayer?.value ?: 0} vittorie totali",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            // ==============================================================
            // --- SECONDA CARD: I NUMERI GENERALI (Partite e Tempo) ---
            // ==============================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Riepilogo Generale", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Riga Partite Giocate (Icona + Testo a sx, Numero gigante a dx)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Style, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(" Partite Giocate:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                            }
                            Text("$totalMatches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }

                        // Divisore sottile tra le due statistiche
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Riga Tempo Speso sul campo
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(" Tempo sul campo:", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                            }
                            // Ricicliamo la nostra utilissima funzione 'formatTime' per trasformare i secondi grezzi in "05:12"
                            Text(formatTime(totalSeconds), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }



            // ==============================================================
            // --- TERZA CARD: IL RECORD DI PUNTI (La partita migliore) ---
            // ==============================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Record di Punti (Singola Partita)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Se esiste almeno una partita nel record...
                        if (highestScoreRecord != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                // A sinistra: Chi ha fatto il record e in che partita lo ha fatto
                                Column {
                                    Text(text = highestScoreRecord.winnerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "in '${highestScoreRecord.title}'", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // A destra: Il numero di punti gigante
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(text = "${highestScoreRecord.winningScore}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    Text(" pt", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                                }
                            }
                        } else {
                            // Se lo storico è completamente vuoto, mostra un messaggio di fallback
                            Text("Ancora nessun record stabilito.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ==============================================================
            // ---> QUARTA CARD: LA PARTITA INFINITA (La più lunga) <---
            // ==============================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("La Partita Infinita", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Se abbiamo trovato una partita che è durata almeno 1 secondo...
                        if (longestMatch != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                // A sinistra: Il nome della partita e chi l'ha vinta
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(text = longestMatch.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "Vinta da ${longestMatch.winnerName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // A destra: L'icona del cronometro e il tempo gigante!
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp))
                                    Text(text = formatTime(longestMatch.durationSeconds), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Text("Nessuna partita cronometrata.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ==============================================================
            // ---> QUINTA CARD: IL DITTATORE (Maggior Distacco) <---
            // ==============================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Il Dittatore (Vittoria Schiacciante)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Se abbiamo trovato una partita e il distacco è maggiore di 0...
                        if (dictatorMatch != null && dictatorMargin > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                                // A sinistra: Chi è il dittatore e in che partita ha dominato
                                // ---> FIX TESTO TAGLIATO <---
                                // Lasciamo il weight(1f) per dargli la priorità di spazio, ma RIMUOVIAMO i blocchi "maxLines" e "overflow"
                                // dalla scritta inferiore, così se è troppo lunga andrà dolcemente a capo su due righe!
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {//padding destro a 12.dp per staccarlo bene dai numeri
                                    Text(
                                        text = dictatorMatch.winnerName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ha dominato in '${dictatorMatch.title}'",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                        // Rimossi maxLines e overflow qui! Ora respira!
                                    )
                                }

                                // A destra: Il numero di punti di scarto gigante
                                // Usiamo Alignment.End per allineare i numeri a destra come una vera colonna
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+$dictatorMargin pt",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "dal 2° posto",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Se tutte le partite sono state dei pareggi (o si è giocato solo da soli)
                            Text("Nessun dominio registrato. Le partite sono state molto equilibrate!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ==============================================================
            // ---> SESTA CARD: IL PIROMANE (Combo On Fire) <---
            // ==============================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Il Piromane 🔥", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Se esiste qualcuno che è andato "a fuoco" almeno una volta...
                        if (topArsonist != null && topArsonist.value > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(text = topArsonist.key, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "È il giocatore con più combo fatte in una partita", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "${topArsonist.value}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color(0xFFF3AF38))
                                    Text(text = "volte On Fire", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            Text("Nessuno ha ancora scatenato l'inferno (3 punti di fila).", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ---> LEZIONE FAB FIX <---
            // Cuscinetto finale per non incollare l'ultima card in fondo allo schermo,
            // permettendo uno scorrimento piacevole fino in fondo.
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}