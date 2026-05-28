package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke // Per il bordo del dock inferiore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType // Per il feedback tattile al click
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource // Import fondamentale per la lettura dei dizionari XML
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.* // Per AnimatedVisibility e transizioni
import androidx.compose.animation.core.* // Strumenti per l'animazione infinita
import androidx.compose.foundation.clickable // Per rendere le card interattive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess // Icona freccia su
import androidx.compose.material.icons.filled.ExpandMore // Icona freccia giù
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawWithContent // Per disegnare la luce
import androidx.compose.ui.geometry.Offset // Per le coordinate del raggio luminoso
import androidx.compose.ui.graphics.Brush // Per creare la sfumatura di luce
import com.n380.scorecounter.R // Import per accedere agli ID delle risorse del progetto
import com.n380.scorecounter.model.getHistoricalCecchino
import com.n380.scorecounter.model.getHistoricalFenice
import com.n380.scorecounter.model.getHistoricalGambero
import com.n380.scorecounter.ui.components.formatDate // Per mostrare la data nelle card espanse
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.ui.components.AutoResizedText
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
    // COMPONENTE DI SUPPORTO: AutoScalingScoreText
    // ====================================================================
    // Questa piccola funzione interna ci permette di disegnare i punteggi in modo intelligente.
    // L'obiettivo è mantenere sempre lo stesso spazio visivo:
    // - Se il numero ha 1 o 2 cifre, usa una dimensione grande.
    // - Se supera le 2 cifre, si rimpicciolisce automaticamente per non allargare la Card.
    @Composable
    fun AutoScalingScoreText(
        text: String,
        suffix: String = "",
        color: Color = MaterialTheme.colorScheme.primary,
        maxSize: TextUnit = 45.sp // Dimensione standard per 2 cifre
    ) {
        // Calcoliamo la dimensione in base alla lunghezza del testo (numero + eventuale " pt")
        val fullText = text + suffix
        val scaledSize = when {
            fullText.length <= 3 -> maxSize // Es: "99" o "5 pt" -> Grande
            fullText.length <= 5 -> maxSize * 0.8f // Es: "150 pt" -> Medio
            else -> maxSize * 0.6f // Es: "+1200 pt" -> Piccolo
        }

        Text(
            text = fullText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = scaledSize,
                lineHeight = scaledSize
            ),
            fontWeight = FontWeight.Black,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }

    // ====================================================================
    // FIX BUG: PREVENZIONE DOPPIO CLICK (Debounce)
    // ====================================================================
    // TEORIA COMPOSE: 'remember' dice a Compose di non dimenticarsi questo valore
    // quando la UI si ricarica (Recomposition). 'mutableStateOf' crea un contenitore
    // reattivo. Parte da 'false' (non ho ancora cliccato).
    var isClosing by remember { mutableStateOf(false) }

    // ====================================================================
    // STATI DI ESPANSIONE PER LE CARD (UX simile alla Home)
    // ====================================================================
    // Questi stati controllano se le card dei record mostrano solo un riassunto
    // o se si espandono per mostrare il titolo completo e la data della partita.
    var isExpRecord by remember { mutableStateOf(false) }
    var isExpInfinita by remember { mutableStateOf(false) }
    var isExpDittatore by remember { mutableStateOf(false) }
    var isExpPiromane by remember { mutableStateOf(false) }


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
        .groupBy { it.name }//raggruppiamo (in cassetti) tutti i dati delle partite di un giocatore per il suo nome
        .mapValues { entry -> entry.value.sumOf { it.fireComboCount } }
    /*
    La funzione mapValues { ... } passa in rassegna il contenuto di ogni singolo cassetto (entry.value).
    Nel nostro caso, usa sumOf per prendere tutte le carte di quel giocatore,
    sommare il loro valore fireComboCount e sostituire l'intero cassetto con un singolo numero finale (il totale delle combo).
     */

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
        // TEORIA COMPOSE (Scaffold Slots):
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
                // FIX GEOMETRICO: ARROTONDAMENTO PARZIALE
                // Usiamo topStart e topEnd a 24.dp per creare la curva morbida solo in alto.
                // Permette al dock di "incollarsi" perfettamente alla base dello schermo.
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // PROTEZIONE DI SISTEMA: navigationBarsPadding()
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
                            // FIX BUG: PREVENZIONE DOPPIO CLICK (Debounce)
                            if (!isClosing) {
                                isClosing = true
                                // Feedback tattile premium come nella Home
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack() // Ritorno alla schermata precedente
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp), // FIX ALTEZZA: 56.dp come richiesto dall'utente
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
                            // Traduce la descrizione per gli screen reader del pulsante di chiusura
                            contentDescription = stringResource(R.string.desc_chiudi_icon),
                            modifier = Modifier.padding(end = 8.dp).size(28.dp)
                        )
                        // Testo "Chiudi" con tipografia HeadlineSmall per massima leggibilità
                        Text(
                            // Assegna l'etichetta al pulsante principale per uscire dalle statistiche
                            text = stringResource(R.string.btn_chiudi_statistiche),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // FIX ARCHITETTURALE: Scroll del Contenuto vs Scroll della Pagina
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
                // Fornisce il titolo testuale fisso situato in cima a questa pagina
                text = stringResource(R.string.titolo_statistiche_partite),
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
                // TEORIA COMPOSE (Weight): Usando weight(1f) diciamo al Tavolo di
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

                // TEORIA COMPOSE: Il trucco del singolo 'item'!
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
                                    // EFFETTO LUCE: drawWithContent ci permette di disegnare
                                    // il fascio luminoso SOPRA il contenuto normale della Card.
                                    .drawWithContent {
                                        drawContent() // 1. Disegna normalmente
                                        drawRect(brush = shimmerBrush) // 2. Ci passa sopra la luce
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                // TEORIA COMPOSE (Il Layout Box):
                                // Il Box funziona a STRATI (Z-Index): il primo elemento scritto sta sul fondo,
                                // i successivi gli vengono stampati sopra. Ottimo per sfondi e filigrane!
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp, horizontal = 24.dp)
                                ) {
                                    // ----------------------------------------------------------------
                                    // STRATO 1 (Sfondo): Medaglia Gigante in Filigrana (Watermark)
                                    // ----------------------------------------------------------------
                                    Icon(
                                        imageVector = Icons.Filled.WorkspacePremium,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 1f),
                                        modifier = Modifier
                                            .size(140.dp)
                                            .align(Alignment.CenterEnd)
                                            .offset(x = 24.dp) // La spingiamo fuori dal bordo per tagliarla
                                        //.offset(x = 30.dp, y = 10.dp) // Leggero sfalsamento dinamico
                                    )

                                    // ----------------------------------------------------------------
                                    // STRATO 2 (Primo Piano): Testi e Badge
                                    // ----------------------------------------------------------------
                                    // Limitiamo la larghezza della colonna di testo
                                    // al 65% dello spazio totale (fillMaxWidth(0.65f)). In questo modo creiamo
                                    // un "muro invisibile". Anche con il font gigante, il testo non scriverà
                                    // MAI sopra la medaglia a destra, ma andrà elegantemente a capo.
                                    Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                                        AutoResizedText(
                                            // Traduce l'intestazione della carta del giocatore che ha vinto di più
                                            text = stringResource(R.string.titolo_campione_assoluto),
                                            style = MaterialTheme.typography.titleSmall,
                                            // Garantiamo massima leggibilità con opacità piena per il titolo
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.ExtraBold,
                                            //letterSpacing = 1.sp // Un tocco di spaziatura per un look "Pro"
                                        )

                                        // LEZIONE TEORICA KOTLIN: Elvis Operator (?:)
                                        // Questa riga tenta di leggere il nome del giocatore migliore. Se il database
                                        // è vuoto, bestPlayer sarà nullo. L'operatore '?:' dice: "Se la variabile di
                                        // sinistra è nulla, usa la stringa che ti passo a destra come paracadute".
                                        AutoResizedText(
                                            // Utilizza la stringa "Nessuno" dal dizionario se il giocatore è nullo
                                            text = bestPlayer?.key ?: stringResource(R.string.nessuno),
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        // BADGE (La "Pillola" col numero di vittorie)
                                        Surface(
                                            modifier = Modifier.padding(top = 12.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            shadowElevation = 4.dp // Elevazione per farlo risaltare come un premio fisico
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(25.dp).padding(end = 6.dp)
                                                )
                                                Text(
                                                    // Utilizza un segnaposto numerico (%d) per formattare il badge "X VITTORIE"
                                                    text = stringResource(R.string.label_vittorie, bestPlayer?.value ?: 0),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ==============================================================
                            // --- SECONDA CARD: I NUMERI GENERALI (Partite e Tempo) ---
                            // ==============================================================
                            // UX/UI: Cambiamo containerColor in 'surface' per evitare l'effetto
                            // mimetico col Tavolo. Inoltre, riduciamo la stondatura a 20.dp per
                            // farla entrare elegantemente nei 24.dp del Tavolo esterno.
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        // Titolo per la sezione dei conteggi totali
                                        text = stringResource(R.string.titolo_riepilogo_generale),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(5.dp))

                                    // Riga Partite Giocate (Icona + Testo a sx, Numero gigante a dx)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Aggiungiamo anche un weight(1f) a questa riga interna e un
                                        // padding a destra. Così diamo l'ordine a Compose: "Prenditi lo spazio,
                                        // ma NON bullizzare il numero a destra, piuttosto vai a capo!"
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        ){
                                            Icon(
                                                Icons.Filled.Style,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            AutoResizedText(
                                                // Etichetta "Partite Giocate:" che affianca l'icona
                                                text = stringResource(R.string.label_partite_giocate),
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Timer,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            AutoResizedText(
                                                // Etichetta "Tempo sul campo:" che affianca il timer
                                                text = stringResource(R.string.label_tempo_giocato),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // UX: Rendiamo la card cliccabile per espanderla e vedere il titolo completo
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExpRecord = !isExpRecord
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Intestazione della Card con indicatore visivo di espansione
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            // Traduce l'intestazione della terza carta statistica
                                            text = stringResource(R.string.titolo_record_punti),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        // Icona che ruota o cambia per segnalare l'interattività
                                        Icon(
                                            imageVector = if (isExpRecord) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(5.dp))

                                    // Se esiste almeno una partita nel record...
                                    if (highestScoreRecord != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // A sinistra: Chi ha fatto il record e in che partita lo ha fatto
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = highestScoreRecord.winnerName,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                // FIX TESTO TAGLIATO: Se espanso (isExpRecord), mostriamo tutto il titolo.
                                                // Altrimenti, limitiamo a 1 riga con i tre puntini.
                                                Text(
                                                    // Inietta il titolo della partita in un segnaposto per formattare la dicitura "in 'Nome Partita'"
                                                    text = stringResource(R.string.label_in_partita, highestScoreRecord.title),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = if (isExpRecord) Int.MAX_VALUE else 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                // UX/UI: Breve descrizione
                                                Text(
                                                    // Testo descrittivo discorsivo per chiarire di che record stiamo parlando
                                                    text = stringResource(R.string.desc_record_punti),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.8f
                                                    ),
                                                    modifier = Modifier.padding(bottom = 4.dp),
                                                    //Se la card è espansa dà spazio infinito (Int.MAX_VALUE), altrimenti forza il testo su 1 sola riga
                                                    maxLines = if (isExpRecord) Int.MAX_VALUE else 1,
                                                    //Se il testo supera il limite di righe (maxLines), taglia l'eccesso e aggiunge i "..." alla fine
                                                    overflow = TextOverflow.Ellipsis //Ellipsis si potrebbe tradurre come ellissi
                                                )
                                            }

                                            // A destra: Il numero di punti gigante (Ora Auto-Scaling!)
                                            AutoScalingScoreText(
                                                text = "${highestScoreRecord.winningScore}",
                                                // Sostituisce il suffisso fisso con la traduzione dinamica prelevata dall'XML
                                                suffix = stringResource(R.string.suffix_pt)
                                            )
                                        }

                                        // AREA DETTAGLI EXTRA: Appare solo quando la card viene cliccata
                                        AnimatedVisibility(
                                            visible = isExpRecord,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                                )
                                                Text(
                                                    // Formatta la stringa dinamica della data incapsulandola nel segnaposto del dizionario
                                                    text = stringResource(R.string.label_data_record, formatDate(highestScoreRecord.timestamp)),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        // Se lo storico è completamente vuoto, mostra un messaggio di fallback
                                        Text(
                                            // Mostra una frase di stato vuoto se non ci sono ancora partite giocate
                                            text = stringResource(R.string.msg_nessun_record),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // ==============================================================
                            // ---> QUARTA CARD: LA PARTITA INFINITA (La più lunga) <---
                            // ==============================================================
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExpInfinita = !isExpInfinita
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            // Traduce l'intestazione della quarta carta
                                            text = stringResource(R.string.titolo_partita_infinita),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (isExpInfinita) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp)) //separatore tra il titolo e il nome dell'utente

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
                                                // FIX TESTO TAGLIATO: Espandiamo il titolo al click
                                                Text(
                                                    text = longestMatch.title,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = if (isExpInfinita) Int.MAX_VALUE else 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    // Inserisce il nome del vincitore nella stringa formattata "Vinta da [Nome]"
                                                    text = stringResource(R.string.label_vinta_da, longestMatch.winnerName),
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

                                        // Area dettagli extra
                                        AnimatedVisibility(visible = isExpInfinita) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                                )
                                                Text(
                                                    // Impagina la data estratta all'interno del costrutto di testo per i dettagli estesi
                                                    text = stringResource(R.string.label_giocata_il, formatDate(longestMatch.timestamp)),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            // Fallback text in caso di database carente di log dei tempi
                                            text = stringResource(R.string.msg_nessuna_partita_cronometrata),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // ==============================================================
                            // ---> QUINTA CARD: IL DITTATORE (Maggior Distacco) <---
                            // ==============================================================
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExpDittatore = !isExpDittatore
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            // Traduce l'intestazione della carta del distacco record
                                            text = stringResource(R.string.titolo_dittatore),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (isExpDittatore) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp)) //separatore tra il titolo e il nome dell'utente

                                    // Se abbiamo trovato una partita e il distacco è maggiore di 0...
                                    if (dictatorMatch != null && dictatorMargin > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            // A sinistra: Chi è il dittatore e in che partita ha dominato
                                            Column(
                                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                                            ) {
                                                Text(
                                                    text = dictatorMatch.winnerName,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                // FIX TESTO TAGLIATO: Espandiamo la descrizione se cliccato
                                                Text(
                                                    // Incapsula il nome della partita nel segnaposto del dizionario per indicare il dominio
                                                    text = stringResource(R.string.label_ha_dominato_in, dictatorMatch.title),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = if (isExpDittatore) Int.MAX_VALUE else 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // A destra: Il numero di punti di scarto gigante (Ora Auto-Scaling!)
                                            Column(horizontalAlignment = Alignment.End) {
                                                AutoScalingScoreText(
                                                    text = "+$dictatorMargin",
                                                    // Sostituisce il suffisso fisso con la traduzione dinamica
                                                    suffix = stringResource(R.string.suffix_pt)
                                                )
                                                Text(
                                                    // Recupera la nota testuale che chiarisce il confronto col "2° posto"
                                                    text = stringResource(R.string.label_dal_secondo_posto),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Area dettagli extra
                                        AnimatedVisibility(visible = isExpDittatore) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                                )
                                                Text(
                                                    // Passa la data al sistema di traduzione per mostrare quando è avvenuto il record
                                                    text = stringResource(R.string.label_data_dominio, formatDate(dictatorMatch.timestamp)),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        // Se tutte le partite sono state dei pareggi (o si è giocato solo da soli)
                                        Text(
                                            // Testo di fallback se la condizione matematica per questo premio fallisce
                                            text = stringResource(R.string.msg_nessun_dominio),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // ==============================================================
                            // --- SESTA CARD: IL PIROMANE (Record di Combo totali) ---
                            // ==============================================================
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // FIX Rimosso padding(bottom = 12.dp) perché
                                    // la Column esterna gestisce già lo spazio tra le card (spacedBy 12.dp).
                                    // Lasciandolo, lo spazio risultava raddoppiato rispetto alle altre carte.
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExpPiromane = !isExpPiromane
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {

                                    // --- INTESTAZIONE DELLA CARD ---
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            // Titolo dell'award basato sul sistema di combo infuocate
                                            text = stringResource(R.string.titolo_piromane),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        // Icona di espansione (aggiunta per coerenza visiva)
                                        Icon(
                                            imageVector = if (isExpPiromane) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(5.dp)) //separatore tra il titolo e il nome dell'utente

                                    // Verifichiamo che esista un record
                                    if (topArsonist != null && topArsonist.value > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // A SINISTRA: Nome e Descrizione
                                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                                Text(
                                                    text = topArsonist.key, // .key è il Nome del giocatore
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )

                                                Text(
                                                    // Spiegazione dettagliata della meccanica di questo record accumulativo
                                                    text = stringResource(R.string.desc_piromane),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    // Espansione del testo
                                                    maxLines = if (isExpPiromane) Int.MAX_VALUE else 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // A DESTRA: Il numero di combo
                                            Column(horizontalAlignment = Alignment.End) {
                                                AutoScalingScoreText(
                                                    text = "${topArsonist.value}",
                                                    color = Color(0xFFF3AF38) // Colore Fuoco/Arancione
                                                )
                                                Text(
                                                    // Etichetta specifica per il conteggio combo
                                                    text = stringResource(R.string.label_volte_on_fire),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // AREA DETTAGLI EXTRA
                                        // Nota: Qui non mettiamo la data perché 'topArsonist' è un totale di carriera.
                                        AnimatedVisibility(visible = isExpPiromane) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                                )
                                                Text(
                                                    // Nota informativa visibile espandendo la card, chiarisce che è un record storico globale
                                                    text = stringResource(R.string.msg_record_storico),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            // Messaggio che appare se nessuno ha mai innescato lo stato On Fire nel database
                                            text = stringResource(R.string.msg_nessun_piromane),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            // ====================================================================
                            // --- NUOVE STATISTICHE GLOBALI (Premi Partita) ---
                            // ====================================================================

                            // 🎯 IL CECCHINO D'ORO (Record assoluto)
                            // TEORIA KOTLIN (Smart Cast): La variabile 'globalSniper' potrebbe essere null
                            // se nessuno ha mai vinto questo premio in tutto lo storico.
                            // Usando l'if, Kotlin attiva la magia dello "Smart Cast": capisce matematicamente
                            // che qui dentro la variabile esiste di sicuro e ci permette di usare i suoi dati (.first e .second).
                            if (globalSniper != null) {
                                // Niente più padding(top = 16.dp) qui, perché ci pensa già la Column esterna!
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
                                                // Testo per l'intestazione del premio Cecchino a livello globale
                                                text = stringResource(R.string.titolo_miglior_cecchino),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(5.dp))

                                            Text(
                                                text = globalSniper.first.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Black
                                            )

                                            // UX/UI: Breve descrizione per chiarire l'obiettivo
                                            Text(
                                                // Specifica qual è stata la prodezza compiuta per questo record
                                                text = stringResource(R.string.desc_miglior_cecchino),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.8f
                                                ),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        // Record Cecchino (Ora Auto-Scaling!)
                                        AutoScalingScoreText(
                                            text = "+${globalSniper.second}",
                                            // Ricicla la traduzione del suffisso standard per i punteggi
                                            suffix = stringResource(R.string.suffix_pt)
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
                                                // Intestazione per la statistica del punteggio peggiore in assoluto
                                                text = stringResource(R.string.titolo_re_gamberi),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(5.dp))

                                            Text(
                                                text = globalCrab.first.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Black
                                            )
                                            // UX/UI: Breve descrizione
                                            Text(
                                                // Descrive la natura negativa di questo curioso record
                                                text = stringResource(R.string.desc_re_gamberi),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.8f
                                                ),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        // Record Gambero (Ora Auto-Scaling!)
                                        AutoScalingScoreText(
                                            text = "${globalCrab.second}",
                                            // Applica il suffisso puntuale tradotto
                                            suffix = stringResource(R.string.suffix_pt)
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
                                                // Definisce l'intestazione per il premio globale del miglior recupero punti
                                                text = stringResource(R.string.titolo_fenice_suprema),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(5.dp))

                                            Text(
                                                text = globalPhoenix.first.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Black
                                            )
                                            // UX/UI: Breve descrizione
                                            Text(
                                                // Sintetizza il significato epico di aver ottenuto la Fenice Suprema
                                                text = stringResource(R.string.desc_fenice_suprema),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.8f
                                                ),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        // Record Fenice (Ora Auto-Scaling!)
                                        AutoScalingScoreText(
                                            text = "+${globalPhoenix.second}",
                                            // Applica la desinenza punti formattata dal file di risorse
                                            suffix = stringResource(R.string.suffix_pt)
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