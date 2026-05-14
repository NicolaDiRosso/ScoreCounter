package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.model.*
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.AwardCard
import com.n380.scorecounter.ui.components.PatternedBackground
import com.n380.scorecounter.ui.components.ScoreChart
import com.n380.scorecounter.ui.components.formatTime

/**
 * ====================================================================
 * COMPONENTE SCHERMATA: OVERLAY ANALISI PARTITA
 * ====================================================================
 * Gestisce esclusivamente la visualizzazione in primo piano del grafico
 * e dei premi di una partita passata. Estratto da HomeScreen per pulizia.
 */
@Composable
fun MatchAnalysisOverlay(
    match: MatchRecord,
    onClose: () -> Unit // Funzione lambda chiamata quando si preme "Chiudi Analisi"
) {
    val haptic = LocalHapticFeedback.current

    // Stato per la visibilità del dialogo informativo sui premi nella schermata di analisi.
    // 🧠 REFACTORING: Spostato qui dalla Home! Appartiene solo a questa schermata.
    var showAwardsInfoDialog by rememberSaveable { mutableStateOf(false) }

    //Controllo visibilità della guida alla lettura del grafico.
    var showChartInfoDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        // Riportiamo il colore a solido (senza alpha) perché lo sfondo a icone
        // riempirà visivamente lo spazio.
        color = MaterialTheme.colorScheme.background
    ) {
        // 🧠 LEZIONE Z-INDEX: Il Box ci permette di sovrapporre i livelli.
        Box(modifier = Modifier.fillMaxSize()) {

            // LIVELLO 0 (Sfondo): Dipingiamo la griglia di icone dinamiche
            // per coerenza con la Home e la ResultsScreen.
            PatternedBackground()

            // LIVELLO 1 (Contenuto): La struttura a colonna che separa area dati e dock comandi.
            Column(modifier = Modifier.fillMaxSize()) {

                // AREA DATI: Contiene Header e Tavolo.
                Column(
                    modifier = Modifier
                        .weight(1f) // Prende tutto lo spazio tranne il dock inferiore
                        // 🧠 LEZIONE SPAZIATURA: Usiamo 'statusBarsPadding' invece di 'systemBarsPadding'.
                        // 'systemBars' aggiungerebbe spazio anche in basso (barra navigazione),
                        // raddoppiando il vuoto dato che il Dock ha già il suo 'navigationBarsPadding'.
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                    // ====================================================================
                    // --- INTESTAZIONE OVERLAY ---
                    // 🧠 UX & MATERIAL 3 (Gerarchia Visiva e Colori):
                    // 1. "Analisi Partita" torna a essere il titolo principale (displaySmall, primary).
                    // 2. Il nome della sfida diventa un sottotitolo ordinato (titleLarge, onSurface).
                    // 3. Rimuoviamo l'effetto grigio (alpha) dal vincitore, dandogli un colore
                    //    'secondary' per farlo risaltare in modo vibrante ed elegante.
                    // ====================================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp), // Diamo più respiro prima del Tavolo
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Analisi Partita",
                            style = MaterialTheme.typography.displaySmall, // <-- Tornato gigante!
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        AutoResizedText(
                            text = match.title,
                            style = MaterialTheme.typography.titleLarge, // <-- Grandezza equilibrata
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "🏆 Vinta da ${match.winnerName} con ${match.winningScore} pt",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary, // <-- Niente più grigio! Colore d'accento
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // ====================================================================
                    // ---> IL TAVOLO (Struttura verticale identica ai Risultati) <---
                    // ====================================================================
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            // 🧠 FIX GEOMETRICO: Aggiungiamo 'padding(bottom = 16.dp)'.
                            // In questo modo, la distanza tra la fine del tavolo grigio e l'inizio del dock bianco
                            // è di esattamente 16.dp, rispecchiando perfettamente il layout della Homepage
                            // dove il tavolo è distanziato dal dock principale della stessa misura.
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        // 🧠 RECOMPOSITION: Usiamo Column + verticalScroll invece di LazyColumn.
                        // Forziamo il caricamento immediato di tutti gli elementi per avere animazioni fluide.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()) // Rende il contenuto del Tavolo scorrevole.
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp) // Spazio automatico tra i figli.
                        ) {
                            // ====================================================================
                            // --- SEZIONE: ANDAMENTO PUNTEGGI (CON ICONA) ---
                            // ====================================================================
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                // Inserimento icona tematica per il grafico a linee
                                Icon(
                                    imageVector = Icons.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AutoResizedText(
                                    "Andamento Punteggi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                // Molla spaziale (Weight): Spinge il pulsante seguente a destra
                                Spacer(modifier = Modifier.weight(1f))

                                // Pulsante Informativo ancorato a destra
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showChartInfoDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Info Grafico",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            // =========================================================
                            // POPUP INFORMATIVO SULLA LETTURA DEL GRAFICO
                            // =========================================================
                            if (showChartInfoDialog) {
                                AlertDialog(
                                    onDismissRequest = { showChartInfoDialog = false },
                                    title = {
                                        Text(
                                            "Come leggere il grafico",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    text = {
                                        Column(
                                            modifier = Modifier.verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                "📈 Punti (Asse Verticale)",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 2.dp, top = 8.dp)
                                            )
                                            Text(
                                                "Più la linea sale in alto, maggiore è il punteggio del giocatore.",
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Text(
                                                "⏱️ Tempo (Asse Orizzontale)",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                                            )
                                            Text(
                                                "Mostra lo scorrere della partita, dall'inizio (a sinistra) fino alla fine (a destra).",
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Text(
                                                "🔵 Pallini e Linee Piatte",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                                            )
                                            Text(
                                                "Ogni pallino è un punto segnato. Se la linea va dritta (piatta), significa che in quel momento nessuno ha fatto punti.",
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Text(
                                                "⚔️ Sorpassi",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                                            )
                                            Text(
                                                "Quando due linee si incrociano, significa che c'è stato un pareggio o un sorpasso in classifica!",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                showChartInfoDialog = false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            AutoResizedText(
                                                "Ho capito",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                )
                            }
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(350.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    // Invochiamo il componente ScoreChart passando i dati necessari per la cronologia.
                                    ScoreChart(
                                        // Passiamo la lista dei record dei giocatori salvati nel match.
                                        players = match.allPlayers,

                                        // Attiviamo la visualizzazione degli assi e delle etichette (dettagli).
                                        isDetailed = true,

                                        // PASSAGGIO DELLA DURATA: Questo e' il dato fondamentale per l' asse X.
                                        // match.durationSeconds contiene i secondi totali registrati dal cronometro
                                        // durante la sessione di gioco.
                                        durationSeconds = match.durationSeconds,
                                        modifier = Modifier.fillMaxSize().padding(12.dp)
                                    )
                                }

                                // --- AREA PREMI (Retrocompatibile) ---
                                //  KOTLIN EXTENSION: Usiamo i metodi creati nel file Models per calcolare i dati.
                                // 'remember(match)' assicura che il calcolo avvenga solo quando cambia la partita selezionata.
                                val storiciCecchino =
                                    remember(match) { match.getHistoricalCecchino() }
                                val storiciInarrestabile =
                                    remember(match) { match.getHistoricalInarrestabile() }
                                val storiciGambero =
                                    remember(match) { match.getHistoricalGambero() }
                                val storiciFenice = remember(match) { match.getHistoricalFenice() }

                                // LOGICA CONDIZIONALE: Se tutti i calcoli sono 'null' (partite vecchie), il blocco sparisce.
                                if (storiciCecchino != null || storiciInarrestabile != null || storiciGambero != null || storiciFenice != null) {

                                    // ====================================================================
                                    // --- SEZIONE: PREMI PARTITA (ICONIZZATA E PULSANTE A DESTRA) ---
                                    // ====================================================================
                                    // --------------------------------------------------------------------
                                    // 🧠 Intestazione con Icona Informativa
                                    // Usiamo una Row per allineare perfettamente al centro l'icona e il titolo.
                                    // Icons.Outlined.Info è molto elegante e non appesantisce la UI.
                                    // --------------------------------------------------------------------
                                    Row(
                                        modifier = Modifier.fillMaxWidth(), // Occupa tutta la larghezza per spingere l'info a destra
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // 1. ICONA PREMI
                                        Icon(
                                            imageVector = Icons.Filled.EmojiEvents, // Icona trofeo/premi
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 2. TITOLO SEZIONE
                                        AutoResizedText(
                                            text = "Premi Partita",
                                            // Usiamo lo stesso stile tipografico di "Andamento Partita" per mantenere coerenza visiva
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        // 3. MOLLA LOGICA (Weight):
                                        // Prende tutto lo spazio rimanente tra il testo e il prossimo componente,
                                        // di fatto "spingendo" l'IconButton verso il bordo destro della Row.
                                        Spacer(modifier = Modifier.weight(1f))

                                        // 4. PULSANTE INFORMATIVO (Ancorato a destra)
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showAwardsInfoDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Info Premi",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
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
                                            title = {
                                                Text(
                                                    "Guida ai Premi",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            text = {
                                                // verticalScroll permette di scorrere il testo col dito se lo schermo del telefono è troppo piccolo
                                                Column(
                                                    modifier = Modifier.verticalScroll(
                                                        rememberScrollState()
                                                    )
                                                ) {

                                                    Text(
                                                        "🎯 Il Cecchino",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(
                                                            bottom = 2.dp,
                                                            top = 8.dp
                                                        )
                                                    )
                                                    Text(
                                                        "Assegnato a chi effettua il singolo salto positivo di punti più alto in un colpo solo.",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )

                                                    Text(
                                                        "🔥 L'Inarrestabile",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(
                                                            bottom = 2.dp,
                                                            top = 16.dp
                                                        )
                                                    )
                                                    Text(
                                                        "Assegnato a chi innesca più volte la combo consecutiva 'On Fire'.",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )

                                                    Text(
                                                        "🦞 Il Gambero",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(
                                                            bottom = 2.dp,
                                                            top = 16.dp
                                                        )
                                                    )
                                                    Text(
                                                        "Assegnato al giocatore che accumula la maggior quantità di punti negativi totali nella partita.",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )

                                                    Text(
                                                        "🦅 La Fenice",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(
                                                            bottom = 2.dp,
                                                            top = 16.dp
                                                        )
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
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                                        showAwardsInfoDialog =
                                                            false// Chiude il popup quando si preme il bottone
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp),
                                                    shape = RoundedCornerShape(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(22.dp),

                                                        )
                                                    Spacer(Modifier.width(8.dp))
                                                    AutoResizedText(
                                                        "Ho capito",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    //showAwardsInfoDialog

                                    // ====================================================================
                                    // 🧠 REFACTORING: UTILIZZO DEL COMPONENTE 'AwardCard'
                                    // Grazie al nostro nuovo mattoncino, abbiamo eliminato centinaia di righe
                                    // di codice duplicato. Passiamo solo i dati grezzi, e la grafica si autogenera!
                                    // ====================================================================

                                    // --- CARD PREMIO: CECCHINO 🎯 ---
                                    storiciCecchino?.let { (player, punti) ->
                                        AwardCard(
                                            icon = "🎯",
                                            title = "Il Cecchino",
                                            playerName = player.name,
                                            playerColorInt = player.color,
                                            valueText = "+$punti pt"
                                        )
                                    }

                                    // --- CARD PREMIO: INARRESTABILE 🔥 ---
                                    storiciInarrestabile?.let { (player, combo) ->
                                        AwardCard(
                                            icon = "🔥",
                                            title = "L'Inarrestabile",
                                            playerName = player.name,
                                            playerColorInt = player.color,
                                            valueText = "$combo Combo"
                                        )
                                    }

                                    // --- CARD PREMIO: IL GAMBERO 🦞 ---
                                    storiciGambero?.let { (player, punti) ->
                                        AwardCard(
                                            icon = "🦞",
                                            title = "Il Gambero",
                                            playerName = player.name,
                                            playerColorInt = player.color,
                                            valueText = "-$punti pt"
                                        )
                                    }

                                    // --- CARD PREMIO: LA FENICE 🦅 ---
                                    storiciFenice?.let { (player, punti) ->
                                        AwardCard(
                                            icon = "🦅",
                                            title = "La Fenice",
                                            playerName = player.name,
                                            playerColorInt = player.color,
                                            valueText = "+$punti pt"
                                        )
                                    }
                                }

                                /// ====================================================================
                                // Footer informativo sulla durata
                                // 🧠 UX & MATERIAL 3: Coerenza dei Colori. L'utente ha giustamente
                                // notato che il grigio "spegne" questa informazione. Usiamo il colore
                                // 'primary' per legarlo visivamente al bottone di chiusura sottostante!
                                // ====================================================================
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.Center, // Bello centrato
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = "Durata",
                                        tint = MaterialTheme.colorScheme.primary, // <-- Niente più grigio
                                        modifier = Modifier.size(18.dp)
                                    )
                                    AutoResizedText(
                                        text = " Durata totale: ${formatTime(match.durationSeconds)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary, // <-- Niente più grigio
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ====================================================================
                    // ---> NUOVO DOCK INFERIORE (Uguale alla Home e Stats) <---
                    // ====================================================================
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        // Arrotondamento solo in alto per incollarlo al fondo dello schermo
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding() // Rispetta lo spazio della barra di navigazione Android
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 16.dp
                                ) // Spaziatura interna per il bottone
                        ) {
                            // --------------------------------------------------------------------
                            // PULSANTE CHIUDI ANALISI (Stile Nuova Sfida)
                            // --------------------------------------------------------------------
                            Button(
                                onClick = {
                                    // Aggiunta la vibrazione per coerenza tattile
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClose() // 🧠 TRIGGER DELLA LAMBDA: Segnala alla Home di chiudere
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp), // Manteniamo l'altezza massiccia (72dp) richiesta
                                shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Chiudi",
                                    modifier = Modifier.padding(end = 8.dp)
                                        .size(28.dp) // Icona maggiorata
                                )
                                Text(
                                    text = "Chiudi Analisi",
                                    // Tipografia imponente (Headline) per richiamare la Home
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }


