package com.n380.scorecounter.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Info
import com.n380.scorecounter.model.PlayerRecord
import com.n380.scorecounter.ui.components.ConfettiExplosion
import com.n380.scorecounter.ui.components.PlayerResultCard
import com.n380.scorecounter.ui.components.ScoreChart
import com.n380.scorecounter.ui.components.formatDate
import com.n380.scorecounter.ui.components.formatTime
import com.n380.scorecounter.viewmodel.MatchViewModel

// ====================================================================
// LA SCHERMATA DELLA CLASSIFICA (RISULTATI) - VERSIONE ANIMATA
// ====================================================================

@Composable
fun ResultsScreen(
    viewModel: MatchViewModel,
    onNavigateHome: () -> Unit
) {
    /**
     * IL COMMENTO DELLA LAMBDA EXPRESSION
     * * Dove:
     * val rankedPlayers = Stiamo creando una nuova variabile immutabile (val) che chiamiamo "giocatori classificati".
     * Nota importante: questa operazione non va a scombinare la lista originale dentro il ViewModel;
     * prende l'elenco originale, lo ordina e salva il risultato ordinato dentro questo nuovo cassetto.
     *
     * viewModel.players = Questa è la nostra lista di partenza.
     * Immagina che contenga: [Marco(Punti: 5), Andrea(Punti: 12), Daniele(Punti: -2)].
     *
     * sortedByDescending = Questa è una funzione integrata (già pronta) di Kotlin che lavora sulle liste.
     * Si traduce letteralmente come:
     * sorted: Ordina
     * By: In base a...
     * Descending: Dal più grande al più piccolo.
     * * { player -> player.score } (La vera magia: La Lambda)
     * Le parentesi graffe indicano una Lambda Expression (una funzione passata al volo).
     * La funzione sortedByDescending è stupida: sa come ordinare i numeri, ma non sa cos'è un "Giocatore".
     * Quindi ti chiede: "Ehi, mi hai dato una lista di oggetti Giocatore. Io non so come confrontarli.
     * Qual è il numero che devo guardare per metterli in ordine?"
     * Questa formula risponde a quella domanda. Si legge così in italiano:
     * "Prendi ogni singolo player, guarda la freccia -> e restituiscimi il suo player.score".
     */
    val rankedPlayers = viewModel.players.sortedByDescending { player -> player.score }

    val haptic = LocalHapticFeedback.current

    // Recuperiamo il Contesto per poter lanciare l'Intento di Condivisione
    val context = LocalContext.current

    // Appena entriamo in questa schermata, l'esplosione è VERA di default!
    // In questo modo, l'animazione partirà all'istante in cui compare la grafica.
    var showConfetti by remember { mutableStateOf(true) }

    // ======================================================
    // ---> LOGICA ANIMAZIONE A CASCATA (Staggered) <---
    // ======================================================
    // Creiamo un "interruttore" che parte su 'false' e diventa 'true' appena entriamo nella pagina.
    // Questo innesca tutte le animazioni di entrata simultaneamente.
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // Variabile di Stato per controllare la visibilità del popup informativo sui premi
    // Parte su 'false' così il popup è nascosto di default.
    var showAwardsInfoDialog by remember { mutableStateOf(false) }

    // ---> CALCOLO STATISTICHE <---
    //-----> CECCHINO <-----
    // Usiamo 'remember' così il calcolo viene fatto UNA SOLA VOLTA quando si apre la schermata,
    // e non viene ricalcolato ogni volta che Compose ridisegna un frame dell'animazione dei coriandoli!
    val cecchinoStat = remember { viewModel.getCecchino() }

    //-----> INARRESATBILE <-----
    val inarrestabileStat = remember { viewModel.getInarrestabile() }
    //-----> IL GAMBERO <-----
    val gamberoStat = remember { viewModel.getGambero() }

    //-----> LA FENICE <-----
    val feniceStat = remember { viewModel.getRitornoDiFiamma() }


    // FUNZIONE DI SUPPORTO INTERNA: Crea l'effetto "comparsa e scivolamento"
    // Spiegazione: prende un 'indice' (la posizione dell'oggetto) e calcola un ritardo basato su di esso.
    @Composable
    fun staggeredModifier(index: Int): Modifier {
        // Animiamo la trasparenza (da 0 a 1)
        val alpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            // Ogni elemento aspetta 150ms moltiplicato per la sua posizione (0, 150, 300...) per creare la "cascata"
            animationSpec = tween(
                durationMillis = 1000,
                delayMillis = index * 150,
                easing = FastOutSlowInEasing
            ),
            label = "alpha"
        )
        // Animiamo la posizione verticale (scivola verso l'alto di 40 pixel)
        val translateY by animateFloatAsState(
            targetValue = if (startAnimation) 0f else 40f,
            animationSpec = tween(
                durationMillis = 600,
                delayMillis = index * 150,
                easing = FastOutSlowInEasing
            ),
            label = "y"
        )

        // Modifier.graphicsLayer applica gli effetti calcolati sopra all'elemento finale senza far ricalcolare l'intera pagina ad Android
        return Modifier.graphicsLayer(alpha = alpha, translationY = translateY)
    }
    // ======================================================

    // Avvolgiamo lo Scaffold in un Box (Scatola). Il Box serve per sovrapporre il "livello"
    // dei coriandoli sopra il "livello" della classifica (lo Scaffold).
    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                // ---> LEZIONE: IL "DOCK" ANCORATO AI BORDI <---
                // Rimuoviamo il padding esterno (start, end, bottom) per far aderire
                // la Surface ai bordi fisici dello schermo, esattamente come nella CounterScreen.
                Surface(
                    modifier = staggeredModifier(rankedPlayers.size + 3), // Manteniamo solo l'animazione!
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),

                    // Modifichiamo la forma: arrotondiamo SOLO gli angoli superiori (24.dp).
                    // Gli angoli inferiori resteranno a 0.dp (piatti) per combaciare con il vetro del telefono.
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding() // Protezione dalla barra bianca di sistema Android
                            // Il padding INTERNO a 16.dp garantisce che i bottoni non tocchino
                            // i bordi dello schermo, rimanendo larghi esattamente quanto le card sopra!
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {

                        // ========================================================
                        // TASTO SECONDARIO: CONDIVIDI RISULTATI (GHOST BUTTON)
                        // ========================================================
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // --- LA LOGICA DI CONDIVISIONE RIMANE INVARIATA ---
                                val finalTitle = if (viewModel.matchTitle.isEmpty()) "Sfida Senza Nome" else viewModel.matchTitle
                                var shareText = "🏆 Risultati: $finalTitle\n"
                                if (viewModel.matchDurationSeconds > 0) shareText += "⏱️ Durata: ${
                                    formatTime(
                                        viewModel.matchDurationSeconds
                                    )
                                }\n"
                                shareText += "📅 Data: ${formatDate(System.currentTimeMillis())}\n\n"

                                rankedPlayers.forEachIndexed { index, player ->
                                    val medal = when (index) {
                                        0 -> "🥇 1°"; 1 -> "🥈 2°"; 2 -> "🥉 3°"; else -> "${index + 1}°"
                                    }
                                    shareText += "$medal ${player.name} - ${player.score} pt\n"
                                }
                                shareText += "\nGenerato con ScoreCounter 🎮\n© 2026 Creato da Nicola"

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Condividi classifica"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp), // Altezza Expressive massiccia (52.dp)
                            shape = RoundedCornerShape(20.dp), // Angoli coerenti per i bottoni (20.dp)
                            // Bordo rinforzato a 2.dp come fatto per il tasto "Azzera"
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Condividi",
                                modifier = Modifier.padding(end = 8.dp).size(28.dp) // Icona leggermente ingrandita
                            )
                            Text(
                                text = "Condividi Risultati",
                                style = MaterialTheme.typography.titleLarge, // Aumentato a titleLarge
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Distanziatore tra i due bottoni impilati
                        Spacer(modifier = Modifier.height(12.dp))

                        // ========================================================
                        // TASTO PRIMARIO: SALVA E TORNA ALLA HOME (CALL TO ACTION)
                        // ========================================================
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.saveCurrentMatch()
                                onNavigateHome()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp), // Altezza Expressive massiccia (72.dp)
                            shape = RoundedCornerShape(20.dp),
                            // Diamo un'ombra forte per farlo "emergere" come tasto principale
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                            // Colori 'Container' per massima leggibilità
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            // ---> COME RICHIESTO: Aggiunta un'icona coerente per il rientro alla Home <---
                            Icon(
                                imageVector = Icons.Filled.Home, // L'icona della casetta
                                contentDescription = "Home",
                                modifier = Modifier.padding(end = 8.dp).size(28.dp)
                            )
                            Text(
                                // Ho abbreviato leggermente il testo per non farlo sbordare
                                // ora che c'è l'icona, mantenendo però il significato intatto
                                text = "Salva e chiudi",
                                style = MaterialTheme.typography.titleLarge, // Aumentato a titleLarge
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {

                // --- INTESTAZIONE (Titolo e Cronometro) ---
                Column(
                    // modificato il padding: invece di 'vertical = 16.dp' (che dava 16 sopra e 16 sotto),
                    // diamo 16.dp in alto, ma solo 10.dp in basso per avvicinare il blocco al Tavolo.
                    modifier = staggeredModifier(0).fillMaxWidth().padding(top = 16.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (viewModel.matchTitle.isEmpty()) "Sfida" else viewModel.matchTitle,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (viewModel.matchDurationSeconds > 0) {
                        Text(
                            text = "⏱️ Tempo di gioco: ${formatTime(viewModel.matchDurationSeconds)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                            // Rimosso il padding(bottom = 8.dp) che c'era qui prima, per compattare il testo
                        )
                    }
                }

                // ====================================================================
                // ---> IL TAVOLO (Scatola Grigia Contenitiva) <---
                // ====================================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // weight(1f) permette alla card di occupare tutto lo spazio fino ai bottoni
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    // Stondatura completa a 24.dp per coerenza con la CounterScreen
                    shape = RoundedCornerShape(24.dp)
                ) {
                // LazyColumn rimossa: usiamo una Column DENTRO il tavolo, per scorrere i giocatori e il grafico.
                // 🧠 KOTLIN & COMPOSE TEORIA: Column vs LazyColumn
                // Sostituendo la LazyColumn con una Column dotata di verticalScroll, forziamo Compose
                // a generare immediatamente anche le card fuori schermo all'apertura della pagina.
                // In questo modo il timer dell'animazione a cascata scatterà per tutte in perfetta sequenza!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Aggiungiamo lo scorrimento manuale fluido al posto della LazyColumn
                        .verticalScroll(rememberScrollState())
                        // Il padding interno sostituisce il 'contentPadding' che avevamo prima
                        .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // Controllo di sicurezza
                    if (rankedPlayers.isNotEmpty()) {

                        // --- LA CLASSIFICA GIOCATORI ---
                        // Utilizziamo forEachIndexed al posto di itemsIndexed (che è esclusivo delle LazyColumn)
                        rankedPlayers.forEachIndexed { index, player ->
                            PlayerResultCard(
                                player = player,
                                position = index + 1, // L'indice parte da 0, la classifica da 1
                                modifier = staggeredModifier(index + 1)
                            )
                        }

                        // --- GRAFICO FINALE ---
                        // Avendo rimosso la LazyColumn, non siamo più costretti a racchiudere i blocchi
                        // dentro 'item { ... }'. I componenti possono essere inseriti liberamente.

                        // Ridotto da 24.dp a 8.dp per avvicinare il grafico alla classifica
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = staggeredModifier(rankedPlayers.size + 2) // Animazione finale
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Andamento Partita",
                                style = MaterialTheme.typography.titleMedium, // Più discreto rispetto alla classifica
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            val recordsForChart = viewModel.players.map {
                                PlayerRecord(
                                    it.name,
                                    it.score,
                                    it.scoreHistory.toList(),
                                    it.fireComboCount,
                                    it.color
                                )
                            }

                            // Sfondo del grafico
                            Card(
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                ScoreChart(
                                    players = recordsForChart,
                                    modifier = Modifier.fillMaxSize().padding(12.dp)
                                )
                            }
                        }

                        // 🧠 KOTLIN LOGICA REATTIVA:
                        // Aggiunto anche il controllo sul quarto premio (ritornoDiFiammaStat)
                        if (cecchinoStat != null || inarrestabileStat != null || gamberoStat != null || feniceStat != null) {

                            // Diamo un respiro di 12.dp per separare bene il blocco del grafico da quello dei premi
                            Spacer(modifier = Modifier.height(12.dp))

                            // ====================================================================
                            // 🧠 UX: Intestazione con Icona Informativa
                            // Usiamo una Row per allineare perfettamente al centro l'icona e il titolo.
                            // Icons.Outlined.Info è molto elegante e non appesantisce la UI.
                            // ====================================================================
                            Row(modifier = staggeredModifier(rankedPlayers.size + 3),//diamo un ritardo per la comparsa dell'icona
                                verticalAlignment = Alignment.CenterVertically,
                            ){
                                // Pulsante icona che inverte la variabile di stato per aprire il popup
                                IconButton(
                                    onClick = { showAwardsInfoDialog = true },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,//icona delle info piena
                                        contentDescription = "Info Premi",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(end = 8.dp) // Spazietto per staccare l'icona dal testo

                                    )
                                }

                                Text(
                                    text = "Premi Partita",
                                    // Usiamo lo stesso stile tipografico di "Andamento Partita" per mantenere coerenza visiva
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }


                        // ====================================================================
                        // STATISTICHE AVANZATE: IL CECCHINO 🎯
                        // ====================================================================
                        // 🧠 KOTLIN NULL SAFETY & SCOPE FUNCTIONS:
                        // In Kotlin usiamo l'operatore '?.let'. che dice "Se cecchinoStat esiste,
                        // esegui questo blocco di codice e chiamalo 'stat'". È un approccio molto più
                        // sicuro ed elegante per gestire variabili che potrebbero essere vuote.
                        cecchinoStat?.let { stat ->

                            // 🧠 KOTLIN DESTRUCTURING (Spacchettamento):
                            // Invece di scrivere stat.first e stat.second come in C++, Kotlin capisce
                            // che 'stat' è una coppia (Pair) e la divide in due variabili al volo.
                            val (sniperPlayer, maxJump) = stat
                            val playerColor = Color(sniperPlayer.color)

                            // Ridotto da 16.dp a 8.dp per avvicinare la card statistica al grafico
                            //Spacer(modifier = Modifier.height(1.dp))

                            // 🎨 MATERIAL 3 EXPRESSIVE: La Card
                            Card(
                                // staggeredModifier è la nostra animazione di entrata. Usiamo 'rankedPlayers.size + 3'
                                // per far apparire questa statistica in ritardo, esattamente dopo l'ultimo giocatore in classifica.
                                modifier = staggeredModifier(rankedPlayers.size + 3)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(
                                    // 🎨 COLORI ADATTIVI: Usiamo il colore del giocatore ma con opacità al 30% (alpha = 0.30f).
                                    // Questo crea uno sfondo tenue che richiama l'identità del giocatore senza rubare
                                    // l'attenzione cromatica alle card della classifica principale.
                                    containerColor = playerColor.copy(alpha = 0.30f)
                                ),
                                // 🎨 FORME DINAMICHE: Un raggio di 20.dp crea un bordo molto "rotondo" e moderno
                                shape = RoundedCornerShape(20.dp),
                                // Aggiungiamo un bordo leggermente più scuro dello sfondo per delineare i contorni
                                border = BorderStroke(1.dp, playerColor.copy(alpha = 0.3f))
                            ) {
                                // Row dispone gli elementi in riga (orizzontalmente)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically // Centra tutto verticalmente
                                ) {
                                    // Icona a tema (Emoji usata come testo)
                                    Text(
                                        text = "🎯",
                                        style = MaterialTheme.typography.displaySmall,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )

                                    // Column dispone i testi in colonna (verticalmente)
                                    // 🧠 COMPOSE MODIFIER WEIGHT:
                                    // .weight(1f) dice a questa colonna: "Spingiti per occupare tutto lo spazio orizzontale
                                    // vuoto a disposizione". Questo trucco schiaccia l'emoji tutta a sinistra e
                                    // il punteggio da record tutto a destra!
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Il Cecchino",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary, // Colore vibrante e sempre leggibile
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = sniperPlayer.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface, // Bianco o Nero dinamico
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // Il punteggio da record: racchiuso in una mini-Card per sembrare un "Badge"
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = playerColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "+$maxJump pt",
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            ),
                                            // Usiamo onPrimary per garantire un contrasto assoluto sul colore pieno
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }

                        // ====================================================================
                        // STATISTICHE AVANZATE: L'INARRESTABILE 🔥
                        // ====================================================================
                        // Ripetiamo la stessa logica di sicurezza: disegniamo la card SOLO se
                        // il ViewModel ha effettivamente trovato qualcuno che ha fatto delle combo.
                        inarrestabileStat?.let { stat ->

                            // Spacchettiamo il Pair: chi è il giocatore e quante combo ha fatto
                            val (firePlayer, comboCount) = stat
                            val playerColor = Color(firePlayer.color)

                            // Ridotto da 16.dp a 8.dp per raggruppare le card
                            Spacer(modifier = Modifier.height(4.dp))

                            // 🎨 MATERIAL 3 EXPRESSIVE: Manteniamo la coerenza visiva!
                            // Usiamo lo stesso identico design della card del Cecchino per creare
                            // una sezione "Statistiche" uniforme e professionale.
                            Card(
                                // Aumentiamo l'indice dell'animazione a cascata (staggeredModifier)
                                // così questa card entra un istante DOPO quella del Cecchino.
                                modifier = staggeredModifier(rankedPlayers.size + 4)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = playerColor.copy(alpha = 0.30f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, playerColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icona dedicata all'Inarrestabile
                                    Text(
                                        text = "🔥",
                                        style = MaterialTheme.typography.displaySmall,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )

                                    // 🧠 ACCESSIBILITÀ (A11y) & UX: Manteniamo la coerenza visiva dei premi
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "L'Inarrestabile",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = firePlayer.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // Il conteggio delle combo
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = playerColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            // Scriviamo "X Combo" (es. "3 Combo")
                                            text = "$comboCount Combo",
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }

                        // ====================================================================
                        // STATISTICHE AVANZATE: IL GAMBERO 🦞
                        // ====================================================================
                        // Mostriamo la card solo se qualcuno ha effettivamente perso dei punti
                        gamberoStat?.let { stat ->
                            // Destructuring: estraiamo il giocatore sfortunato e i punti totali persi
                            val (gamberoPlayer, pointsLost) = stat
                            val playerColor = Color(gamberoPlayer.color)

                            // Ridotto da 16.dp a 8.dp per raggruppare le card
                            Spacer(modifier = Modifier.height(4.dp))

                            // 🎨 MATERIAL 3 EXPRESSIVE: Stessa identica struttura delle card precedenti.
                            Card(
                                // Aumentiamo ancora di 1 l'indice dell'animazione a cascata (size + 5)
                                // così apparirà per terza, dopo l'Inarrestabile.
                                modifier = staggeredModifier(rankedPlayers.size + 5)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = playerColor.copy(alpha = 0.30f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, playerColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icona dedicata al Gambero
                                    Text(
                                        text = "🦞",
                                        style = MaterialTheme.typography.displaySmall,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )

                                    // 🧠 ACCESSIBILITÀ (A11y) & UX: Manteniamo la coerenza visiva dei premi
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Il Gambero",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = gamberoPlayer.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // Il punteggio negativo
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = playerColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            // Aggiungiamo il segno meno '-' davanti per indicare la perdita
                                            text = "-$pointsLost pt",
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }

                        // ====================================================================
                        // STATISTICHE AVANZATE: LA FENICE 🦅 (Ex Ritorno di Fiamma)
                        // ====================================================================
                        // Manteniamo il nome della variabile logica intatto, cambiamo solo la UI!
                        feniceStat?.let { stat ->
                            // Spacchettamento (Destructuring): Estraiamo il giocatore e i punti recuperati
                            val (comebackPlayer, recoveryPoints) = stat
                            val playerColor = Color(comebackPlayer.color)

                            // Spazio ridotto (8.dp) per raggruppare visivamente le card dei premi
                            Spacer(modifier = Modifier.height(8.dp))

                            // 🎨 MATERIAL 3 EXPRESSIVE: Stessa struttura delle card precedenti
                            Card(
                                // Incrementiamo ancora l'indice dell'animazione a cascata (size + 6)
                                // in modo che appaia per quarta, con un timing perfetto.
                                modifier = staggeredModifier(rankedPlayers.size + 6)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = playerColor.copy(alpha = 0.30f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, playerColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icona a tema per la rimonta
                                    Text(
                                        text = "🦅",
                                        style = MaterialTheme.typography.displaySmall,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )

                                    // 🧠 ACCESSIBILITÀ (A11y) & UX: Manteniamo la coerenza visiva dei premi
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "La Fenice",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = comebackPlayer.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // Il punteggio della rimonta (Delta)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = playerColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "+$recoveryPoints pt",
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } // <-- Fine della Column (ex LazyColumn)
                } // <-- FINE DEL TAVOLO (Card contenitiva)
            }
        }
        // =========================================================
        // POPUP INFORMATIVO SUI PREMI (AlertDialog)
        // =========================================================
        // 🧠 COMPOSE STATE: Questo blocco reagisce alla variabile 'showAwardsInfoDialog'.
        if (showAwardsInfoDialog) {
            AlertDialog(
                // onDismissRequest scatta se l'utente tocca fuori dal popup o preme "Indietro" sul telefono
                onDismissRequest = { showAwardsInfoDialog = false },
                title = { Text("Guida ai Premi", fontWeight = FontWeight.Bold) },
                text = {
                    // verticalScroll permette di scorrere il testo col dito se lo schermo del telefono è troppo piccolo
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                        Text(
                            "🎯 Il Cecchino",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp, top = 8.dp)
                        )
                        Text(
                            "Assegnato a chi effettua il singolo salto positivo di punti più alto in un colpo solo.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            "🔥 L'Inarrestabile",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                        )
                        Text(
                            "Assegnato a chi innesca più volte la combo consecutiva 'On Fire'.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            "🦞 Il Gambero",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                        )
                        Text(
                            "Assegnato al giocatore che accumula la maggior quantità di punti negativi totali nella partita.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            "🦅 La Fenice",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                        )
                        Text(
                            "Assegnato a chi compie la rimonta più epica, calcolata tra il suo punto più basso e il punteggio finale.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    // Pulsante pieno (Button) al posto del TextButton, con la nostra stondatura ufficiale a 20.dp
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAwardsInfoDialog = false // Chiude il popup quando si preme il bottone
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Ho capito")
                    }
                }
            )
        }

        // =========================================================
        // ESECUZIONE DELL'ANIMAZIONE CORIANDOLI (Sovrapposta in alto)
        // =========================================================
        // Se la variabile è "true" scoppiano i coriandoli!
        if (showConfetti) {
            ConfettiExplosion(
                // Forniamo i colori ufficiali del Material Theme
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.error
                ),
                onAnimationFinished = {
                    // Quando l'animazione ha finito, settiamo a false così smette di disegnare
                    showConfetti = false
                }
            )
        }
    }
}