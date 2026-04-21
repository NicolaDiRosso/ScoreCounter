package com.n380.scorecounter

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
                                if (viewModel.matchDurationSeconds > 0) shareText += "⏱️ Durata: ${formatTime(viewModel.matchDurationSeconds)}\n"
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
                                .height(52.dp), // Altezza Expressive massiccia (72.dp)
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
            // Ora la schermata base è una LazyColumn. Con l'aggiunta del grafico in fondo, lo schermo diventa molto alto.
            // Con una Column statica, su telefoni piccoli parte della classifica finirebbe fuori dallo schermo.
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    // --- INTESTAZIONE TITOLO E CRONOMETRO (Indice 0, appare per primo) ---
                    Column(
                        modifier = staggeredModifier(0),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (viewModel.matchTitle.isEmpty()) "Sfida" else viewModel.matchTitle,
                            // Usiamo l'esatto stile gigante della schermata Home e Nuova Partita
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Mostriamo il cronometro di gioco sotto il titolo
                        if (viewModel.matchDurationSeconds > 0) {
                            Text(
                                text = "⏱️ Tempo di gioco: ${formatTime(viewModel.matchDurationSeconds)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            // Se non c'è il cronometro, usiamo uno Spacer per mantenere bilanciata la grafica
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Controlliamo preventivamente che la classifica non sia vuota per evitare crash
                if (rankedPlayers.isNotEmpty()) {
                    item {
                        // --- IL VINCITORE (Indice 1, appare per secondo) ---
                        // Il primo elemento della lista (ormai ordinata!) è indubbiamente il vincitore assoluto
                        val winner = rankedPlayers[0]
                        val winnerColor = Color(winner.color)

                        // ======================================================
                        // ---> EFFETTO CARTA RARA (Shimmer Sweep) <---
                        // ======================================================
                        // 1. IL MOTORE DELL'ANIMAZIONE: Crea un timer che va in loop continuo (infinito).
                        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

                        // 2. LA COORDINATA IN MOVIMENTO: Calcola un numero che viaggia da -500 a 2000 in 4.5 secondi.
                        val translateAnim by infiniteTransition.animateFloat(
                            initialValue = -500f, // Parte da fuori lo schermo a sinistra
                            targetValue = 2000f,  // Viaggia fino a fuori lo schermo a destra
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 4500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart // Quando finisce, ricomincia da capo istantaneamente
                            ),
                            label = "shimmer_translation"
                        )

                        // 3. IL FASCIO DI LUCE (Pennello Gradiente):
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                winnerColor.copy(alpha = 0.4f), // Luce sfumata col colore del vincitore
                                Color.Transparent
                            ),
                            // Colleghiamo inizio e fine del gradiente alle coordinate in movimento per far "scivolare" la luce in diagonale
                            start = Offset(translateAnim, translateAnim),
                            end = Offset(translateAnim + 400f, translateAnim + 400f) // 400f è lo spessore logico del raggio
                        )
                        // ======================================================

                        Card(
                            modifier = staggeredModifier(1) // Applica l'animazione di entrata a cascata
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                // ---> APPLICAZIONE DELLA LUCE SULLA CARD <---
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = shimmerBrush)
                                },
                            // ---> MODIFICA IDENTITY VISUAL: Tonal Surface (Sfondo sfumato al 50%) <---
                            colors = CardDefaults.cardColors(containerColor = winnerColor.copy(alpha = 0.7f)),
                            border = BorderStroke(2.dp, winnerColor.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.EmojiEvents,
                                    "Vincitore",
                                    Modifier.padding(bottom = 8.dp).size(48.dp),
                                    tint = winnerColor // La coppa esplode col colore puro
                                )
                                Text(
                                    text = "VINCITORE",
                                    style = MaterialTheme.typography.labelLarge,
                                    // ---> FIX LEGGIBILITÀ <--- Testo Neutro
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = winner.name,
                                    // Usiamo "displayMedium" che è un testo davvero gigantesco per fare impatto
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    // ---> FIX LEGGIBILITÀ <--- Testo Neutro per contrasto ottimale
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${winner.score} Punti",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        // --- TITOLO POSIZIONI (Indice 2, appare per terzo) ---
                        Text(
                            text = "Posizioni successive:",
                            style = MaterialTheme.typography.titleMedium,
                            // ---> Forziamo un colore brillante a contrasto! <---
                            color = MaterialTheme.colorScheme.primary,
                            modifier = staggeredModifier(2).fillMaxWidth().padding(bottom = 8.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    // --- ALTRI GIOCATORI (Indice 3 + la loro posizione) ---
                    // Cicliamo il resto della classifica per creare le card arrotondate per il 2°, 3° posto ecc.
                    itemsIndexed(rankedPlayers) { index, player ->
                        // Condizione IF geniale: "Salta la generazione se l'indice è 0" (il vincitore lo abbiamo già stampato)
                        if (index > 0) {
                            val playerColor = Color(player.color)

                            Card(
                                // staggeredModifier(index + 3) fa sì che il ritardo aumenti in base alla posizione in classifica
                                modifier = staggeredModifier(index + 3)
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                // ---> MODIFICA IDENTITY VISUAL: Tonal Surface al 50% <---
                                colors = CardDefaults.cardColors(containerColor = playerColor.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Mettiamo 'index + 1' perché gli array nella programmazione partono da 0,
                                    // ma la classifica umana parte logicamente dal 1° posto!
                                    Text(
                                        text = "${index + 1}° ${player.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        // ---> FIX LEGGIBILITÀ <--- Testo Neutro
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${player.score} pt",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        // ---> FIX LEGGIBILITÀ <--- Testo Neutro
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // --- GRAFICO FINALE (Indice dimensione classifica + 4) ---
                        // Appare per ultimo dopo tutti i giocatori
                        Spacer(modifier = Modifier.height(16.dp)) // Diamo respiro prima del grafico
                        Card(
                            modifier = staggeredModifier(rankedPlayers.size + 4)
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Andamento Partita",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                // Convertiamo i "Player" in "PlayerRecord" per darli in pasto al motore grafico
                                val recordsForChart = viewModel.players.map {
                                    PlayerRecord(
                                        it.name,
                                        it.score,
                                        it.scoreHistory.toList(),
                                        it.fireComboCount,
                                        it.color // Assicuriamoci che il colore venga passato al grafico!
                                    )
                                }
                                // ALTEZZA DEL GRAFICO FISSATA: lo rendiamo alto 200 pixel, bello spazioso.
                                ScoreChart(
                                    players = recordsForChart,
                                    modifier = Modifier.fillMaxWidth().height(200.dp)
                                )
                            }
                        }
                    }
                    // Spazio vuoto gigante inserito in fondo alla lista per non coprire mai la fine del grafico coi bottoni
                    item { Spacer(modifier = Modifier.height(180.dp)) }
                }
            }
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