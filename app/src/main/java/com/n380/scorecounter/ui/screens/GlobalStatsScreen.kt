package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke // Per il bordo del dock inferiore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType // Per il feedback tattile al click
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
    val longestMatch =
        viewModel.history.filter { it.durationSeconds > 0 }.maxByOrNull { it.durationSeconds }


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

        // ====================================================================
        // NUOVO DOCK INFERIORE (Esattamente uguale a HomeScreen)
        // ====================================================================
        // 🧠 TEORIA COMPOSE (Scaffold Slots):
        // Lo 'Scaffold' ci offre degli slot predefiniti. 'bottomBar' è lo slot inferiore.
        // Tutto ciò che mettiamo qui dentro viene sganciato dalla lista scorrevole e
        // rimane FISSO in fondo allo schermo, galleggiando in primo piano.
        bottomBar = {
            // ---> DOCK ANCORATO: Surface con bordi superiori arrotondati e bordo sottile <---
            // Abbiamo rimosso il 'modifier = Modifier.padding(...)' dalla Surface.
            // Senza margini esterni, la Surface si espande automaticamente fino a toccare
            // i bordi fisici laterali e il bordo inferiore dello schermo del telefono.
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                // 🧠 FIX GEOMETRICO: ARROTONDAMENTO PARZIALE
                // Usiamo topStart e topEnd a 24.dp per creare la curva morbida solo in alto.
                // Permette al dock di "incollarsi" perfettamente alla base dello schermo.
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 🧠 PROTEZIONE DI SISTEMA: navigationBarsPadding()
                        // "spinge" in alto il contenuto interno solo di quel tanto che basta per
                        // non finire sotto la riga orizzontale bianca di Android.
                        .navigationBarsPadding() 
                        .padding(horizontal = 16.dp, vertical = 16.dp) // Spaziatura interna per il bottone
                ) {
                    // ====================================================================
                    // --- BOTTONE DI CHIUSURA (Estetica Nuova Sfida della Home) ---
                    // ====================================================================
                    Button(
                        onClick = {
                            // 🧠 FIX BUG: PREVENZIONE DOPPIO CLICK (Debounce)
                            if (!isClosing) {
                                isClosing = true
                                // Feedback tattile premium come nella Home
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack() // Ritorno alla schermata precedente
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // 🛠️ FIX ALTEZZA: 56.dp come richiesto dall'utente
                        shape = RoundedCornerShape(20.dp), // Stessa stondatura coerente dell'app
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        // Icona Close (X) dimensionata a 28.dp per impatto visivo
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Chiudi",
                            modifier = Modifier.padding(end = 8.dp).size(28.dp)
                        )
                        // Testo "Chiudi" con tipografia HeadlineSmall per massima leggibilità
                        Text(
                            text = "Chiudi Statistiche",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // 🧠 FIX ARCHITETTURALE: Scroll del Contenuto vs Scroll della Pagina
        // Sostituiamo la LazyColumn principale con una semplice Column.
        // In questo modo, l'intestazione resta FISSA in alto.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            // ==============================================================
            // ---> INTESTAZIONE DELLA PAGINA (Ora FISSA!) <---
            // ==============================================================
            // ---> MODIFICA ESTETICA: ALLINEAMENTO E SPAZIATURA <---
            // 1. Aggiungiamo uno Spacer di 16.dp in altezza. Questo garantisce che la distanza
            // dal centro notifiche (orologio/batteria) sia IDENTICA a quella di HomeScreen.
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Statistiche partite",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                // 2. Rimosso 'start = 16.dp' per evitare il "doppio padding" e allinearlo perfettamente a sinistra.
                // 3. Aggiunto 'bottom = 24.dp' per distaccarlo elegantemente dalla prima carta sottostante.
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ====================================================================
            // ---> IL TAVOLO DELLE STATISTICHE (Ora include anche il Campione!) <---
            // ====================================================================
            Card(
                // 🧠 TEORIA COMPOSE (Weight): Usando weight(1f) diciamo al Tavolo di
                // espandersi verticalmente per occupare tutto lo spazio rimasto sullo schermo!
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
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

                // 🧠 TEORIA COMPOSE: Il trucco del singolo 'item'!
                // Trasformiamo il Tavolo in una scatola che scorre (LazyColumn).
                // Invece di modificare ogni singola carta aggiungendo 'item { }', creiamo
                // una LazyColumn che contiene un solo gigantesco elemento 'item'.
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp), // Padding interno del tavolo
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ==============================================================
                            // --- PRIMA CARD: IL CAMPIONE ASSOLUTO (Ora "Inglobata"!) ---
                            // ==============================================================
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 🎨 EFFETTO LUCE: drawWithContent ci permette di disegnare
                                    // il fascio luminoso SOPRA il contenuto normale della Card.
                                    .drawWithContent {
                                        drawContent() // 1. Disegna normalmente
                                        drawRect(brush = shimmerBrush) // 2. Ci passa sopra la luce
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                // 🧠 TEORIA COMPOSE (Il Layout Box):
                                // Il Box funziona a STRATI (Z-Index): il primo elemento scritto sta sul fondo,
                                // i successivi gli vengono stampati sopra. Ottimo per sfondi e filigrane!
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp, horizontal = 24.dp)
                                ) {
                                    // ----------------------------------------------------------------
                                    // STRATO 1 (Sfondo): Medaglia Gigante in Filigrana
                                    // ----------------------------------------------------------------
                                    Icon(
                                        imageVector = Icons.Filled.WorkspacePremium,
                                        contentDescription = "Medaglia",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                        modifier = Modifier
                                            .size(120.dp)
                                            .align(Alignment.CenterEnd)
                                            .offset(x = 24.dp) // La spingiamo fuori dal bordo per tagliarla
                                    )

                                    // ----------------------------------------------------------------
                                    // STRATO 2 (Primo Piano): Testi e Badge
                                    // ----------------------------------------------------------------
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "CAMPIONE ASSOLUTO",
                                            style = MaterialTheme.typography.labelLarge,
                                            // Regola d'oro: su 'primaryContainer' usiamo 'onPrimaryContainer'
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = bestPlayer?.key ?: "Nessuno",
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )

                                        // BADGE (La "Pillola" col numero di vittorie)
                                        Card(
                                            modifier = Modifier.padding(top = 8.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text(
                                                text = "${bestPlayer?.value ?: 0} VITTORIE",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // ==============================================================
                            // --- SECONDA CARD: I NUMERI GENERALI (Partite e Tempo) ---
                            // ==============================================================
                            // 🧠 UX/UI: Cambiamo containerColor in 'surface' per evitare l'effetto
                            // mimetico col Tavolo. Inoltre, riduciamo la stondatura a 20.dp per
                            // farla entrare elegantemente nei 24.dp del Tavolo esterno.
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Riepilogo Generale",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Riga Partite Giocate (Icona + Testo a sx, Numero gigante a dx)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Style,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                " Partite Giocate:",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                        Text(
                                            "$totalMatches",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Divisore sottile tra le due statistiche
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )

                                    // Riga Tempo Speso sul campo
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Timer,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                " Tempo sul campo:",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                        // Ricicliamo la nostra utilissima funzione 'formatTime' per trasformare i secondi grezzi in "05:12"
                                        Text(
                                            formatTime(totalSeconds),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // ==============================================================
                            // --- TERZA CARD: IL RECORD DI PUNTI (La partita migliore) ---
                            // ==============================================================
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Record di Punti (Singola Partita)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Se esiste almeno una partita nel record...
                                    if (highestScoreRecord != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // A sinistra: Chi ha fatto il record e in che partita lo ha fatto
                                            Column {
                                                Text(
                                                    text = highestScoreRecord.winnerName,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "in '${highestScoreRecord.title}'",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // A destra: Il numero di punti gigante
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                Text(
                                                    text = "${highestScoreRecord.winningScore}",
                                                    style = MaterialTheme.typography.displayMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    " pt",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(
                                                        bottom = 6.dp,
                                                        start = 4.dp
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        // Se lo storico è completamente vuoto, mostra un messaggio di fallback
                                        Text(
                                            "Ancora nessun record stabilito.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // ==============================================================
                            // ---> QUARTA CARD: LA PARTITA INFINITA (La più lunga) <---
                            // ==============================================================
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "La Partita Infinita",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Se abbiamo trovato una partita che è durata almeno 1 secondo...
                                    if (longestMatch != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // A sinistra: Il nome della partita e chi l'ha vinta
                                            Column(
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = longestMatch.title,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Vinta da ${longestMatch.winnerName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // A destra: L'icona del cronometro e il tempo gigante!
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Timer,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                                Text(
                                                    text = formatTime(longestMatch.durationSeconds),
                                                    style = MaterialTheme.typography.displaySmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            "Nessuna partita cronometrata.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // ==============================================================
                            // ---> QUINTA CARD: IL DITTATORE (Maggior Distacco) <---
                            // ==============================================================
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Il Dittatore (Vittoria Schiacciante)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Se abbiamo trovato una partita e il distacco è maggiore di 0...
                                    if (dictatorMatch != null && dictatorMargin > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            // A sinistra: Chi è il dittatore e in che partita ha dominato
                                            // ---> FIX TESTO TAGLIATO <---
                                            // Lasciamo il weight(1f) per dargli la priorità di spazio, ma RIMUOVIAMO i blocchi "maxLines" e "overflow"
                                            // dalla scritta inferiore, così se è troppo lunga andrà dolcemente a capo su due righe!
                                            Column(
                                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                                            ) {//padding destro a 12.dp per staccarlo bene dai numeri
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
                                        Text(
                                            "Nessun dominio registrato. Le partite sono state molto equilibrate!",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // ==============================================================
                            // ---> SESTA CARD: IL PIROMANE (Combo On Fire) <---
                            // ==============================================================
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Il Piromane 🔥",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Se esiste qualcuno che è andato "a fuoco" almeno una volta...
                                    if (topArsonist != null && topArsonist.value > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    text = topArsonist.key,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "È il giocatore con più combo fatte in una partita",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${topArsonist.value}",
                                                    style = MaterialTheme.typography.displaySmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFF3AF38)
                                                )
                                                Text(
                                                    text = "volte On Fire",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            "Nessuno ha ancora scatenato l'inferno (3 punti di fila).",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
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
                                // 🧠 Niente più padding(top = 16.dp) qui, perché ci pensa già la Column esterna!
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Miglior Cecchino 🎯",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            // 💡 UX/UI: Breve descrizione per chiarire l'obiettivo
                                            Text(
                                                text = "Maggior punteggio fatto in un singolo turno",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.8f
                                                ),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Text(
                                                text = globalSniper.first.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Text(
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
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Il Re dei Gamberi 🦞",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            // 💡 UX/UI: Breve descrizione
                                            Text(
                                                text = "Maggior numero di punti persi in una sola mossa",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.8f
                                                ),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Text(
                                                text = globalCrab.first.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Text(
                                            text = "${globalCrab.second} pt",
                                            style = MaterialTheme.typography.displayMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 🦅 LA FENICE SUPREMA (Miglior recupero di sempre)
                            if (globalPhoenix != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "La Fenice Suprema 🦅",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            // 💡 UX/UI: Breve descrizione
                                            Text(
                                                text = "La rimonta più leggendaria dall'ultimo posto",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.8f
                                                ),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Text(
                                                text = globalPhoenix.first.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Black
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

                        }
                    }

                    // Cuscinetto finale per non incollare l'ultima card in fondo allo schermo,
                    // permettendo uno scorrimento piacevole fino in fondo.
                    //item { Spacer(modifier = Modifier.height(1.dp)) }
                }
            }
        }
    }
}
