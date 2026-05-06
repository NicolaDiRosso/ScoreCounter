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
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawWithContent // Per disegnare la luce
import androidx.compose.ui.geometry.Offset // Per le coordinate del raggio luminoso
import androidx.compose.ui.graphics.Brush // Per creare la sfumatura di luce
import com.n380.scorecounter.model.getHistoricalCecchino
import com.n380.scorecounter.model.getHistoricalFenice
import com.n380.scorecounter.model.getHistoricalGambero
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.viewmodel.MatchViewModel
//per misurare un solo tocco alla volta per il pulsante chiudi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
    // 🧠 FIX BUG: PREVENZIONE DOPPIO CLICK (Debounce)
    // ====================================================================
    // TEORIA COMPOSE: 'remember' dice a Compose di non dimenticarsi questo valore
    // quando la UI si ricarica (Recomposition). 'mutableStateOf' crea un contenitore
    // reattivo. Parte da 'false' (non ho ancora cliccato).
    var isClosing by remember { mutableStateOf(false) }

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

    // ====================================================================
    // ---> I RECORD GLOBALI DEI PREMI <---
    // ====================================================================
    // mapNotNull estrae i premi dalle partite ma scarta automaticamente
    // e silenziosamente tutte le partite in cui il premio non c'era (null).
    // Dopodiché, maxByOrNull trova la partita in cui il punteggio di quel premio è stato il più alto in assoluto!
    val globalSniper = viewModel.history
        .mapNotNull { it.getHistoricalCecchino() }
        .maxByOrNull { it.second } // it.second è il punteggio del salto

    val globalCrab = viewModel.history
        .mapNotNull { it.getHistoricalGambero() }
        .maxByOrNull { it.second } // it.second sono i punti persi

    val globalPhoenix = viewModel.history
        .mapNotNull { it.getHistoricalFenice() }
        .maxByOrNull { it.second } // it.second sono i punti recuperati



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
                        .drawWithContent {
                            drawContent() // Disegna testo e icone normalmente
                            drawRect(brush = shimmerBrush) // Passa il raggio di luce animato sopra a tutto!
                        },
                    // Usiamo il primaryContainer per dare un effetto "Oro/Premio" che si stacca dalle altre card
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    // ====================================================================
                    // --- PRIMA CARD: IL CAMPIONE ASSOLUTO (Chi vince di più) ---
                    // ====================================================================
                    Card(
                        // 'modifier' serve per alterare l'aspetto o il comportamento del componente.
                        // 'fillMaxWidth()' dice alla Card di allargarsi fino a toccare i bordi (meno il padding esterno).
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                drawContent() // 1. Disegna normalmente il contenuto della Card
                                drawRect(brush = shimmerBrush) // 2. Ci passa sopra il "pennello" animato (Shimmer)
                            },
                        // 'colors' in Material 3 definisce la palette della Card.
                        // 'primaryContainer' dà un colore di sfondo forte ed elegante (es. azzurro pastello o oro).
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        // 'shape' definisce i bordi. 24.dp crea angoli molto arrotondati, tipici del Material 3.
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        // 🧠 TEORIA COMPOSE (Il Layout Box):
                        // Invece di 'Row' (elementi in fila) o 'Column' (elementi impilati), usiamo 'Box'.
                        // Il Box funziona a STRATI (Z-Index): il primo elemento scritto sta sul fondo,
                        // i successivi gli vengono stampati sopra. Ottimo per sfondi e filigrane!
                        Box(
                            // Mettiamo un padding INTERNO al Box, così i testi non toccano i bordi della Card.
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 24.dp)
                        ) {

                            // ----------------------------------------------------------------
                            // STRATO 1 (Sfondo): Medaglia Gigante in Filigrana
                            // Questo elemento viene disegnato per primo, quindi sta "sotto".
                            // ----------------------------------------------------------------
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium, // L'icona vettoriale della medaglia
                                contentDescription = "Medaglia", // Testo per chi usa lo screen reader (Accessibilità)

                                // 🎨 UX/UI: La tua intuizione! Usiamo il colore 'primary' (es. Blu acceso).
                                // In Kotlin, usiamo la funzione '.copy(alpha = 0.3f)' per clonare il colore
                                // abbassandone l'opacità al 30%. Così risalta, ma non nasconde il testo.
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 1f),

                                modifier = Modifier
                                    .size(120.dp) // 'size' forza l'icona a diventare gigantesca
                                    .align(Alignment.CenterEnd) // La calamitiamo al centro-destra del Box
                                    .offset(x = 24.dp) // La spingiamo 24 pixel fuori dal bordo destro per tagliarla
                            )

                            // ----------------------------------------------------------------
                            // STRATO 2 (Primo Piano): Testi e Badge
                            // Viene disegnato per secondo, quindi si appoggia SOPRA la medaglia.
                            // ----------------------------------------------------------------
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // SOTTOTITOLO (Il Sopracciglio)
                                Text(
                                    text = "CAMPIONE ASSOLUTO",
                                    style = MaterialTheme.typography.labelLarge, // Font piccolo e leggibile
                                    // Se lo sfondo è 'primaryContainer', la regola d'oro di Material 3 impone
                                    // di usare 'onPrimaryContainer' per i testi, per garantire il massimo contrasto.
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold // Grassetto per dargli carattere
                                )

                                // TITOLO PRINCIPALE (Il Nome del Giocatore)
                                Text(
                                    // 'bestPlayer?.key' usa il Null Safety di Kotlin: se bestPlayer è null,
                                    // usa la stringa dopo il '?:' (chiamato operatore Elvis).
                                    text = bestPlayer?.key ?: "Nessuno",
                                    style = MaterialTheme.typography.displayMedium, // Font gigantesco
                                    fontWeight = FontWeight.Black, // Font extrabold (più doppio del Bold)
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                // BADGE (La "Pillola" col numero di vittorie)
                                // Una Card dentro un'altra Card! Serve solo per creare lo sfondo colorato attorno al testo.
                                Card(
                                    modifier = Modifier.padding(top = 8.dp), // Spazio dal nome del giocatore
                                    shape = RoundedCornerShape(12.dp), // Angoli molto stondati (effetto pillola)
                                    // Questo badge usa il colore 'primary' puro come sfondo
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        // Stampiamo il numero di vittorie ('value' della mappa)
                                        text = "${bestPlayer?.value ?: 0} VITTORIE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        // Essendo su sfondo 'primary', il testo DEVE essere 'onPrimary' (es. bianco)
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ) // Padding interno della pillola
                                    )
                                }
                            }
                        } // Fine Box
                    } // Fine Card
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

                // ====================================================================
                // --- NUOVE STATISTICHE GLOBALI (Premi Partita) ---
                // ====================================================================

                // 🎯 IL CECCHINO D'ORO (Record assoluto)
                // 🧠 TEORIA KOTLIN (Smart Cast): La variabile 'globalSniper' potrebbe essere null
                // se nessuno ha mai vinto questo premio in tutto lo storico.
                // Usando l'if, Kotlin attiva la magia dello "Smart Cast": capisce matematicamente
                // che qui dentro la variabile esiste di sicuro e ci permette di usare i suoi dati (.first e .second).
                if (globalSniper != null) {
                    Card(
                        // modifier.padding(top = 16.dp) stacca questa card da quella precedente
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        // 'surfaceVariant' crea un colore di fondo leggermente diverso dalla pagina (grigio dinamico M3)
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp) // Stondatura morbida e coerente
                    ) {
                        // 🧠 TEORIA COMPOSE: 'Row' allinea gli elementi da sinistra a destra.
                        // Arrangement.SpaceBetween è il trucco per incollare la Column (coi testi) a sinistra
                        // e il numerone del punteggio tutto a destra.
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 🧠 TEORIA COMPOSE: 'Column' impila gli elementi dall'alto in basso.
                            // 'weight(1f)' dice alla Column: "Espanditi occupando tutto lo spazio orizzontale
                            // che avanza, così spingi il punteggio sul bordo destro".
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Miglior Cecchino 🎯",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary, // Colore vibrante per il titolo
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    // '.first' accede all'oggetto Player salvato, da cui prendiamo il nome
                                    text = globalSniper.first.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black
                                )
                                // 💡 UX/UI: Breve descrizione per chiarire l'obiettivo
                                Text(
                                    text = "Maggior punteggio fatto in un singolo turno",
                                    style = MaterialTheme.typography.bodySmall, // Font piccolo da didascalia
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), // Grigio leggibile
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                // '.second' accede al valore numerico del record (il punteggio)
                                text = "+${globalSniper.second} pt",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 🦞 IL RE DEI GAMBERI (Record negativo assoluto)
                if (globalCrab != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Il Re dei Gamberi 🦞",
                                    style = MaterialTheme.typography.labelLarge,
                                    // 🎨 UX/UI: Usiamo 'error' (rosso) perché è una statistica negativa (malus)
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = globalCrab.first.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black
                                )
                                // 💡 UX/UI: Breve descrizione
                                Text(
                                    text = "Maggior numero di punti persi in una sola mossa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                // Non stampiamo il '+' perché globalCrab.second è già un numero negativo
                                text = "${globalCrab.second} pt",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.error, // Rosso anche per il numero
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 🦅 LA FENICE SUPREMA (Miglior recupero di sempre)
                if (globalPhoenix != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "La Fenice Suprema 🦅",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = globalPhoenix.first.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black
                                )
                                // 💡 UX/UI: Breve descrizione
                                Text(
                                    text = "La rimonta più leggendaria dall'ultimo posto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = "+${globalPhoenix.second} pt",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ====================================================================
                // --- BOTTONE DI CHIUSURA ---
                // ====================================================================
                // 🧠 UX & MATERIAL 3: Coerenza visiva e "Thumb Zone".
                // Riprendiamo lo stesso identico bottone usato per chiudere l'Analisi Partita.
                // Posizionandolo in fondo, lo rendiamo facilissimo da cliccare col pollice.

                // Spacer funge da "cuscinetto" per non incollare il bottone all'ultima card
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    // 🧠 TEORIA KOTLIN: Protezione dallo State
                    // Quando clicchiamo, controlliamo 'isClosing'. Se è false,
                    // lo facciamo diventare true e scateniamo onNavigateBack().
                    // Eventuali tocchi accidentali successivi troveranno isClosing a true
                    // e non faranno assolutamente nulla!
                    onClick = {
                        if (!isClosing) {
                            isClosing = true
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth() // Il bottone si allarga per tutta la larghezza disponibile
                        .height(56.dp), // Altezza standard M3 per i bottoni "Call to Action"
                    colors = ButtonDefaults.buttonColors(
                        // Usiamo il 'primary' per far capire che è l'azione principale
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp) // Stondatura massiccia a "Pillola"
                ) {
                    // 🧠 TEORIA COMPOSE: Il contenuto del Button è una Row invisibile.
                    // Possiamo affiancare icone e testi con facilità.
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Chiudi Statistiche"
                    )

                    Spacer(modifier = Modifier.width(8.dp)) // Spazietto orizzontale tra icona e testo

                    Text(
                        text = "Chiudi Statistiche",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Aggiungiamo un ultimo cuscinetto sotto al bottone per non farlo
                // appoggiare fisicamente sul bordo inferiore dello schermo del telefono
                Spacer(modifier = Modifier.height(32.dp))


            }



            // ---> LEZIONE FAB FIX <---
            // Cuscinetto finale per non incollare l'ultima card in fondo allo schermo,
            // permettendo uno scorrimento piacevole fino in fondo.
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}