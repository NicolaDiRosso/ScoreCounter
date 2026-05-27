    package com.n380.scorecounter.ui.components

    import android.content.Context
    import android.content.Intent
    import androidx.compose.animation.core.*
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.foundation.Canvas
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border // <-- IMPORTANTE: Serve per disegnare il bordo del cerchio selezionato
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.horizontalScroll
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.*
    import androidx.compose.material3.FilterChip
    import androidx.compose.material3.FilterChipDefaults
    import androidx.compose.material3.Icon
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.alpha
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.draw.drawWithContent
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.unit.Dp
    import androidx.compose.ui.graphics.Brush // Serve per il pallino arcobaleno
    import androidx.compose.ui.graphics.nativeCanvas // PERMETTE DI DISEGNARE TESTI NEL CANVAS
    import androidx.compose.ui.graphics.toArgb
    import androidx.compose.ui.hapticfeedback.HapticFeedbackType
    import androidx.compose.ui.platform.LocalHapticFeedback
    import androidx.compose.ui.res.stringResource
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import com.n380.scorecounter.R // 🌍 I18N: Import fondamentale per accedere agli ID del dizionario
    import com.n380.scorecounter.model.PlayerRecord
    import java.text.SimpleDateFormat
    import java.util.*
    import kotlin.math.cos
    import kotlin.math.sin
    import kotlin.random.Random

    // FUNZIONE DI SUPPORTO: Formatta i secondi in "Minuti:Secondi" (Es. 05:12)
    fun formatTime(totalSeconds: Long): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    // 🧠 LEZIONE TEORICA I18N: Formattazione Universale
    // Non "forziamo" più la lingua italiana fissa (Locale.ITALIAN). Usiamo `Locale.getDefault()`.
    // Il telefono capirà da solo se l'utente è americano, e stamperà la data come piace a lui!
    // FUNZIONE DI SUPPORTO: Formatta i millisecondi in una Data (Es. 8 Nov 2026)
    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // ====================================================================
    // COMPONENTE: SFONDO DECORATIVO GLOBALE
    // ====================================================================
    @Composable
    fun PatternedBackground() {
        // La lista delle nostre icone a tema (Giochi, Anime, Carte, Coppe, Timer, Medaglie)
        val icons = listOf(
            // Le tue originali
            Icons.Filled.VideogameAsset,   // Controller Classico
            Icons.Filled.Style,            // Carte
            Icons.Filled.Tv,               // Anime/Schermo
            Icons.Filled.EmojiEvents,      // Coppa del vincitore
            Icons.Filled.Timer,            // Cronometro
            Icons.Filled.WorkspacePremium, // Medaglia/Corona

            // ICONE TEMATICHE ESCLUSIVE PER LO SFONDO
            Icons.Filled.Casino,           // Dadi (Perfetto per i giochi da tavolo!)
            Icons.Filled.SportsEsports,    // Controller Moderno (Per tornei e console)
            Icons.Filled.Extension,        // Pezzo di Puzzle (Per giochi di strategia e logica)
            Icons.Filled.Star,             // Stella (Il classico simbolo dei punti)
            Icons.Filled.FlashOn,          // Fulmine (Richiama la combo "On Fire" e la velocità)
        )

        // ---> IL MOTORE CASUALE BLOCCATO <---
        // Creiamo una "mappa" bidimensionale fissa. Viene calcolata a caso la prima volta,
        // ma grazie a 'remember' lo schermo non sfarfallerà mai durante la partita!
        val randomGrid = remember {
            List(34) { // 34 righe
                List(11) { // 11 colonne
                    icons.random() // Sceglie un'icona totalmente a caso per ogni singola cella
                }
            }
        }
        // Un contenitore grande quanto tutto lo schermo
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {//mette 15 pixel di vuoto tra un'icona e l'altra in verticale.
                // Disegniamo 33 righe per coprire anche gli schermi più lunghi
                for (row in 0..33) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(43.dp),//mette 43 pixel di vuoto tra un'icona e l'altra in orizzontale.
                        // IL TRUCCO: Sfalsiamo leggermente le righe dispari per fare l'effetto "muro di mattoni" sfalsato
                        modifier = if (row % 2 == 0) Modifier else Modifier.padding(start = 36.dp)
                    ) {
                        for (col in 0..10) {val icon = randomGrid[row][col] // Andiamo a leggere l'icona salvata nella nostra mappa casuale

                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                //Usiamo il colore del testo, ma con opacità al 5% (0.05f). Sarà un'ombra elegantissima!
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ====================================================================
// 🧠 COMPONENTE WRAPPER: SFUMATURA LATERALE (Faded Right Edge)
// ====================================================================
// Questo componente agisce come una "cornice magica". Applica il principio DRY (Don't Repeat Yourself).
// Qualsiasi cosa tu ci metta dentro (il parametro 'content'), riceverà automaticamente
// un elegante effetto sfumato sul bordo destro, prevenendo il problema del "Falso Fondo" nelle liste scorrevoli.
    @Composable
    fun FadedRightEdgeWrapper(
        modifier: Modifier = Modifier,
        fadeWidth: Dp = 40.dp, // Larghezza predefinita del velo sfumato
        fadeColor: Color = MaterialTheme.colorScheme.surface, // Il colore verso cui sfumare
        content: @Composable () -> Unit // Questo è lo "Slot API": il buco dove infileremo le nostre LazyRow
    ) {
        // 1. Il Box principale che tiene insieme tutto
        Box(modifier = modifier) {

            // 2. Disegniamo prima il contenuto passato da chi usa la funzione (es. la lista dei titoli)
            content()

            // 3. Disegniamo sopra al contenuto il velo sfumato
            Box(
                modifier = Modifier.matchParentSize() // Si adatta alle esatte dimensioni del contenuto
            ) {
                Spacer(
                    modifier = Modifier
                        .width(fadeWidth)
                        .fillMaxHeight() // Copre tutta l'altezza del contenitore
                        .align(Alignment.CenterEnd) // Lo blocchiamo a destra
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent, // Parte trasparente che mostra gli elementi sotto
                                    fadeColor // Colore solido che copre l'ultimo pezzetto
                                )
                            )
                        )
                )
            }
        }
    }


    // ====================================================================
    // IL MOTORE DEL GRAFICO A LINEE (ScoreChart / Game Stats)
    // ====================================================================

    /**
     * ---> LEZIONE: PARAMETRI OPZIONALI <---
     * Abbiamo aggiunto 'isDetailed: Boolean = false'.
     * Il "= false" significa che è un parametro opzionale. Se non lo scrivi quando chiami
     * la funzione (come fai nella card piccola), lui assume che sia falso.
     * Se invece scrivi 'isDetailed = true' (come faremo nel grafico gigante), attiverà gli assi!
     */
    /**
     * COMPONENTE DIDATTICO: ScoreChart(GRAFICO DEI PUNTEGGI)
     * Disegna il grafico cartesiano dei punteggi usando la geometria vettoriale (Canvas).
     * * @param players Lista dei giocatori con i loro storici punti.
     * @param modifier Modificatore per gestire dimensioni e padding esterni.
     * @param isDetailed Se VERO, il grafico disegna la griglia di sfondo, l'Asse Y (Punteggi) e l'Asse X (Cronologia).
     */
    /**
     * FUNZIONE: ScoreChart
     * SCOPO: Rendering grafico dell'andamento dei punteggi nel tempo.
     * LOGICA: Utilizza un sistema di coordinate cartesiane dove l'asse X rappresenta il tempo
     * e l'asse Y il punteggio. Il sistema calcola dinamicamente i rapporti di scala
     * per far rientrare i dati all'interno della dimensione del Canvas.
     */

    // ====================================================================
// 📊 COMPONENTE: GRAFICO INTERATTIVO (ScoreChart)
// ====================================================================
// Questa è una funzione "Composable", ovvero disegna UI.
// Riceve la lista dei giocatori, la grandezza (isDetailed) e la durata.
    @Composable
    fun ScoreChart(
        players: List<PlayerRecord>,
        modifier: Modifier = Modifier,
        isDetailed: Boolean = false, // 🧠 L'INTERRUTTORE: Se è false, disegna in piccolo. Se è true, in grande.
        durationSeconds: Long = 0L
    ) {
        // Estraiamo il motore di vibrazione del telefono (Serve per far vibrare i quadratini)
        val haptic = LocalHapticFeedback.current

        // Traduzioni base
        val labelInizio = stringResource(R.string.label_chart_inizio)
        val labelMeta = stringResource(R.string.label_chart_meta)
        val labelFine = stringResource(R.string.label_chart_fine)

        // ====================================================================
        // 🧠 1. GESTIONE DELLO STATO (La Memoria)
        // ====================================================================
        // 'hiddenPlayers' è una scatola magica che ricorda i nomi dei giocatori spenti.
        // Usiamo 'Set' (Insieme matematico) e non 'List' perché in un Set ogni nome
        // può esserci una volta sola (non possono esserci due "Marco" nascosti).
        var hiddenPlayers by remember { mutableStateOf(setOf<String>()) }

        // PROTEZIONE ANTI-CRASH: Se non ci sono giocatori, fermati e non disegnare nulla.
        if (players.isEmpty()) return

        // Filtriamo via i giocatori che hanno 0 round giocati (evita calcoli su grafici vuoti)
        val validPlayers = players.filter { (it.scoreHistory ?: emptyList()).isNotEmpty() }
        if (validPlayers.isEmpty()) return

        // 🧠 IL FILTRAGGIO REATTIVO
        // Creiamo una lista 'visiblePlayers' prendendo i validPlayers, MA escludendo (!)
        // quelli il cui nome è finito dentro la nostra scatola 'hiddenPlayers'.
        val visiblePlayers = validPlayers.filter { !hiddenPlayers.contains(it.name) }

        // ====================================================================
        // 🧠 2. LA MATEMATICA DELL'AUTO-ZOOM
        // ====================================================================
        // Per far sì che il grafico faccia "Zoom", deve sapere qual è il tetto massimo e minimo.
        // Invece di guardare TUTTI i giocatori, gli diciamo di guardare SOLO 'visiblePlayers'.
        // In questo modo, se spegni il giocatore primo in classifica, il 'maxScore' si abbassa
        // istantaneamente, e il grafico per magia si adatta ai giocatori rimanenti!
        val maxScore = visiblePlayers.mapNotNull { it.scoreHistory?.maxOrNull() }.maxOrNull() ?: 0
        val minScore = visiblePlayers.mapNotNull { it.scoreHistory?.minOrNull() }.minOrNull() ?: 0

        // ====================================================================
        // 3. I COLORI ADATTIVI (Chiaro/Scuro)
        // ====================================================================
        val nativeAdaptiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        // Se il grafico è piccolo (isDetailed = false), facciamo la griglia più invisibile (alpha 0.1f)
        // per non confondere l'occhio. Se è grande, la calchiamo un po' di più (alpha 0.2f).
        val adaptiveGridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDetailed) 0.2f else 0.1f)
        val adaptiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

        // Colonna principale che conterrà il Canvas (sopra) e la Legenda (sotto)
        Column(modifier = modifier) {

            // ====================================================================
            // 🎨 4. LA TELA DA DISEGNO (CANVAS)
            // ====================================================================
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {

                // 🧠 I MARGINI DI SICUREZZA (Padding)
                // Usiamo ".toPx()" per convertire i "dp" (unità di misura dello schermo) in "Pixel" matematici.
                // Se il grafico è piccolo, diamo margini stretti (es. padYTop = 4). Se è grande, margini larghi.
                val padYTop = if (isDetailed) 10.dp.toPx() else 4.dp.toPx()
                val padYBottom = if (isDetailed) 50.dp.toPx() else 24.dp.toPx()
                val padX = if (isDetailed) 20.dp.toPx() else 12.dp.toPx()

                // 'drawW' (Larghezza) e 'drawH' (Altezza) sono la misura effettiva in pixel
                // dove possiamo disegnare le linee senza sbattere sui bordi.
                val drawW = (size.width - padX * 2).coerceAtLeast(1f)
                val drawH = (size.height - padYTop - padYBottom).coerceAtLeast(1f)

                // L'escursione termica dei punti (es. massimo 50, minimo 0 = range di 50).
                val yRange = (maxScore - minScore).coerceAtLeast(1).toFloat()

                // 🧠 IL PENNELLO DEL TESTO
                val textPaint = android.graphics.Paint().apply {
                    color = nativeAdaptiveTextColor
                    // Se grafico grande -> font 32f. Se piccolo -> font 22f.
                    textSize = if (isDetailed) 32f else 22f
                    textAlign = android.graphics.Paint.Align.RIGHT // Allineato a destra (verso l'asse)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                // --------------------------------------------------------------------
                // ASSE Y (Le fasce orizzontali dei Punteggi)
                // --------------------------------------------------------------------
                // Quante righe vogliamo? 4 se grande, 2 se piccolo (altrimenti sarebbero tutte accavallate).
                val ySteps = if (isDetailed) 4 else 2
                for (i in 0..ySteps) {
                    // Calcolo della posizione Y (Dall'alto verso il basso)
                    val y = padYTop + drawH - (i * (drawH / ySteps))

                    // Disegna la linea grigia da sinistra (start) a destra (end)
                    drawLine(
                        color = adaptiveGridColor,
                        start = Offset(padX, y),
                        end = Offset(padX + drawW, y),
                        strokeWidth = if (isDetailed) 2f else 1f // Linea sottile se grafico piccolo
                    )

                    // Calcola il numero da scrivere (Es. riga 1 = 10 punti, riga 2 = 20 punti)
                    val value = minScore + (yRange / ySteps) * i

                    // Sposta il testo un po' a sinistra e un po' in basso per non coprire la linea
                    val textOffsetX = if (isDetailed) 24f else 16f
                    val textOffsetY = if (isDetailed) 10f else 8f

                    // Disegna fisicamente il numero (es. "20") sulla tela!
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toInt().toString(),
                        padX - textOffsetX,
                        y + textOffsetY,
                        textPaint
                    )
                }

                // --------------------------------------------------------------------
                // ASSE X (Le fasce verticali del Tempo)
                // --------------------------------------------------------------------
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                // Quante colonne? 4 se grande, 2 se piccolo (Inizio e Fine).
                val xSteps = if (isDetailed) 4 else 2
                for (i in 0..xSteps) {
                    val xPos = padX + i * (drawW / xSteps) // Posizione orizzontale

                    // Linea grigia verticale di sfondo
                    drawLine(
                        color = adaptiveGridColor,
                        start = Offset(xPos, padYTop),
                        end = Offset(xPos, padYTop + drawH),
                        strokeWidth = if (isDetailed) 2f else 1f
                    )
                    // Piccola tacchetta scura che sporge sotto il grafico
                    drawLine(
                        color = adaptiveTickColor,
                        start = Offset(xPos, size.height - padYBottom),
                        end = Offset(xPos, size.height - padYBottom + (if(isDetailed) 12f else 6f)),
                        strokeWidth = if (isDetailed) 3f else 2f
                    )

                    // 🧠 LOGICA DELLE ETICHETTE DEL TEMPO
                    val label = if (durationSeconds > 0) {
                        // Se c'è un timer vero, calcola il tempo esatto di quel segmento
                        val fractionSeconds = (durationSeconds.toFloat() / xSteps) * i
                        formatTime(fractionSeconds.toLong())
                    } else {
                        // Se NON c'è timer, mette le scritte manuali (Inizio, 1/4, Metà...)
                        if (isDetailed) {
                            when(i) {
                                0 -> labelInizio
                                1 -> "1/4"
                                2 -> labelMeta
                                3 -> "3/4"
                                else -> labelFine
                            }
                        } else {
                            // Se il grafico è piccolo ha solo 3 tacche (0, 1, 2)
                            when(i) {
                                0 -> labelInizio
                                1 -> labelMeta
                                else -> labelFine
                            }
                        }
                    }
                    val textBottomOffset = if (isDetailed) 45f else 30f
                    // Scrive la parola sul fondo
                    drawContext.canvas.nativeCanvas.drawText(label, xPos, size.height - padYBottom + textBottomOffset, textPaint)
                }

                // --------------------------------------------------------------------
                // 🧠 5. DISEGNO DEI SENTIERI (Le linee dei giocatori)
                // --------------------------------------------------------------------
                // NOTA BENE: Questo ciclo analizza SOLO 'visiblePlayers'.
                // I giocatori spenti sono ignorati, quindi non vengono disegnati!
                visiblePlayers.forEach { player ->
                    val color = Color(player.color) // Prende il colore del giocatore
                    val path = androidx.compose.ui.graphics.Path() // Crea un "sentiero" vuoto
                    val history = player.scoreHistory ?: emptyList()

                    if (history.size >= 1) {
                        // Distanza orizzontale esatta tra ogni round
                        val localXStep = drawW / (history.size - 1).coerceAtLeast(1).toFloat()

                        history.forEachIndexed { turn, score ->
                            // Matematica cartesiana: X (avanza col tempo), Y (sale coi punti)
                            val x = padX + (turn * localXStep)
                            val y = padYTop + drawH - ((score - minScore) / yRange * drawH)

                            // moveTo posiziona la penna all'inizio, lineTo traccia la linea fino al punto successivo
                            if (turn == 0) path.moveTo(x, y)
                            else path.lineTo(x, y)

                            // Disegna un pallino su ogni round
                            val radius = if (isDetailed) 2.dp.toPx() else 1.dp.toPx()
                            drawCircle(color, radius, Offset(x, y))
                        }
                    }

                    // Stampa l'intero "sentiero" (Path) sulla tela, con angoli arrotondati (StrokeCap.Round)
                    drawPath(
                        path = path,
                        color = color,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = if (isDetailed) 3.dp.toPx() else 1.5.dp.toPx(), // Linea spessa o sottile
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            } // <-- Fine Canvas

            // ====================================================================
            // 🧠 6. LA LEGENDA INTERATTIVA (Evoluzione in Badge / Tonal Chips)
            // ====================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if(isDetailed) 12.dp else 4.dp)
                    // Permette di scorrere i bottoni lateralmente se ci sono molti giocatori
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                // Iteriamo su TUTTI i giocatori validi per mostrarli in fondo al grafico
                validPlayers.forEach { player ->
                    // Controlliamo la memoria: questo specifico giocatore è spento?
                    val isHidden = hiddenPlayers.contains(player.name)

                    // 🧠 IL BADGE COERENTE (La riga diventa un pulsante a pillola)
                    // Usiamo i modificatori per ricreare lo stile Tonal ad onde con bordo dell'app.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            // Spazio esterno tra una pillola e l'altra
                            .padding(end = 10.dp, bottom = 4.dp)

                            // 1. STONDATURA: Taglia l'effetto onda del click a raggio 20.dp (stilo Soft Pill)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))

                            // 2. SFUMATURA DI SFONDO (BACKGROUND):
                            // - Se attivo: Colore Primario molto sfumato (12% opacità), stile TonalButton.
                            // - Se spento: Sfondo trasparente per far "svuotare" la casella.
                            .background(
                                if (isHidden) Color.Transparent
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )

                            // 3. IL BORDO COERENTE (BORDER):
                            // - Se attivo: Bordino sottile azzurro/blu primario (30% opacità).
                            // - Se spento: Bordino grigio neutro sbiadito (20% opacità).
                            .border(
                                width = 1.dp,
                                color = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                            )

                            // 🧠 LA HITBOX (Area Cliccabile): Abilitata su tutto il riquadro
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm) // Innesca micro-vibrazione

                                // Logica degli insiemi: accende o spegne inserendo/rimuovendo dal Set
                                hiddenPlayers = if (isHidden) {
                                    hiddenPlayers - player.name
                                } else {
                                    hiddenPlayers + player.name
                                }
                            }

                            // Padding interno: fa respirare quadratino e testo dentro la scatola
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // --------------------------------------------------------
                        // 🎨 IL QUADRATINO DELLA LEGENDA (Chiave di lettura fissa)
                        // --------------------------------------------------------
                        Box(
                            modifier = Modifier
                                .size(if(isDetailed) 14.dp else 10.dp) // Più piccolo nel widget della Home
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) // Quadratino smussato dolce

                                // 🧠 LA TUA RICHIESTA: Il colore di sfondo resta fisso al 100%!
                                .background(Color(player.color))

                                // Sfumiamo leggermente l'opacità complessiva del quadratino solo se spento (alpha 40%)
                                // per accordarsi elegantemente con il testo cancellato, ma senza nascondere il colore.
                                .let { modificatore ->
                                    if (isHidden) modificatore.alpha(0.4f) else modificatore
                                }
                        )

                        // --------------------------------------------------------
                        // 📝 IL NOME DEL GIOCATORE (Grassetto!)
                        // --------------------------------------------------------
                        Text(
                            text = player.name,
                            style = if(isDetailed) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,

                            // Forza il testo ad essere in GRASSETTO per massima leggibilità
                            fontWeight = FontWeight.Bold,

                            modifier = Modifier.padding(start = 6.dp),

                            // GESTIONE DINAMICA DEL COLORE E DELLA LINEA:
                            // - Se attivo: Prende il colore Primario (Blu/Azzurro del tema) per staccare dallo sfondo.
                            // - Se spento: Sbiadisce in un grigio neutro al 40% di opacità.
                            color = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.primary,

                            // Applica la cancellatura (LineThrough) orizzontale solo quando escluso dal grafico
                            textDecoration = if (isHidden) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }
    }

    // ====================================================================
    // IL MOTORE GRAFICO DEI CORIANDOLI
    // ====================================================================
    @Composable
    fun ConfettiExplosion(colors: List<Color>, onAnimationFinished: () -> Unit) {
        // Animatable: Il "timer/percentuale" dell'animazione. Parte da 0.0f (0%) e arriverà a 1.0f (100%).
        val animationProgress = remember { Animatable(0f) }

        // LaunchedEffect fa partire il codice interno asincrono solo UNA VOLTA appena la funzione appare sullo schermo del telefono.
        LaunchedEffect(Unit) {
            animationProgress.animateTo(
                targetValue = 1f, // L'obiettivo è arrivare a 1
                // tween: Stabilisce che ci vorranno esattamente 1200 millisecondi (1.2 secondi) di orologio reale.
                // FastOutSlowInEasing fa sì che l'animazione parta "col botto" scattante in modo realistico e poi rallenti dolcemente cadendo.
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
            // Quando la funzione animateTo ha finito di viaggiare verso l'1 (l'animazione è morta), diciamo all'app madre che abbiamo terminato!
            onAnimationFinished()
        }

        // Generazione della FISICA VETTORIALE dei coriandoli.
        // Il "remember" ci garantisce che generiamo i 60 pallini casuali solo una singola volta all'inizio della scena,
        // e non 60 volte al secondo per ogni frame video (cosa che distruggerebbe il processore fondendo il telefono)!
        val particles = remember {
            List(60) { // Creiamo 60 elementi (coriandoli fisici)
                // 1. Direzione (Angolo Geometrico): Calcoliamo un angolo casuale da 0 a 360°.
                // Nella matematica di Kotlin si usa il "Radiante" e non il grado centigrado. L'angolo giro completo (360°) equivale a 2 volte il Pi Greco.
                val angle = Random.nextDouble(0.0, 2 * Math.PI)

                // 2. Velocità Esplosiva: Una velocità sparata a caso tra 600 e 1800 per dare l'effetto di un'esplosione caotica e irregolare (non circolare perfetta).
                val speed = Random.nextFloat() * 1200f + 600f

                // 3. Colore Decorativo: Peschiamo un colore a caso dalla lista che ci hanno passato dai Material Colors poco fa!
                val color = colors.random()

                // Restituiamo in modo raggruppato una "Triple", ovvero un super-oggetto contenente queste tre specifiche caratteristiche vitali.
                Triple(angle, speed, color)
            }
        }

        // Canvas: La tela digitale trasparente (nuda e cruda) che occupa tutto l'intero schermo in primo piano, usata per il rendering super-veloce in 2D.
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Identifichiamo la coordinata del punto esatto centrale dello schermo, dividendone semplicemente larghezza e altezza a metà.
            val center = Offset(size.width / 2, size.height / 2)

            // Progress avanzerà continuamente nel tempo, frame dopo frame (0.1, 0.2, ... 1.0)
            val progress = animationProgress.value

            // Per OGNUNO dei 60 pallini memorizzati (coriandoli), disegniamo e aggiorniamo la sua posizione esatta in questo preciso millisecondo logico.
            particles.forEach { (angle, speed, color) ->
                // Distanza viaggiata dal centro = velocità di base moltiplicata per il tempo percentuale trascorso.
                val distance = speed * progress

                // Gravità Terrestre: Un numero finto che cresce in modo esponenziale per far "cadere" la Y del pallino verso il basso simulando il suo peso!
                val gravity = progress * progress * 800f

                // Calcolo effettivo della posizione (X e Y cartesiane):
                // - il Coseno trigonometrico di un angolo calcola la distanza e lo spostamento orizzontale (X)
                // - il Seno trigonometrico dell'angolo calcola la distanza logica verticale (Y) aggiungendo anche il peso in basso della gravità.
                val x = center.x + (cos(angle) * distance).toFloat()
                val y = center.y + (sin(angle) * distance).toFloat() + gravity

                // Trasparenza visiva o sfumatura (Alpha): 1.0 è solido e opaco, 0.0 è invisibile e trasparente come il vetro.
                // Sottraendo matematicamente 'progress' a 1, i coriandoli svaniranno gradualmente come fumo man mano che il tempo passa alla fine dell'esplosione.
                val alpha = (1f - progress).coerceIn(0f, 1f)

                // Finiti i calcoli, diciamo fisicamente alla tela in C++ (Canvas) di dipingere un cerchio solido con quelle esatte coordinate appena trovate.
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = 18f, // Raggio: La grandezza totale misurata in pixel fisici del nostro coriandolo
                    center = Offset(x, y)
                )
            }
        }
    }

    // ======================================================================================
    //  TonalActionPill (Bottone per micro-azioni come quello "GESTISCI" di gestisci giocatori rapidi)
    // ======================================================================================
    /**
     * Crea un pulsante "Tonal" (sfondo semi-trasparente e bordo leggero).
     * È lo standard del nostro Design System per le azioni secondarie o le impostazioni.
     * * @param text Il testo da mostrare dentro il bottone.
     * @param onClick L'azione da eseguire quando viene premuto.
     * @param modifier Permette di aggiungere padding o allineamenti dall'esterno.
     * @param icon L'icona opzionale da mostrare a sinistra del testo.
     */
    @Composable
    fun TonalActionPill(
        onClick: () -> Unit,//funzione onClick che non richiede dati in ingresso, ed esegue un'azione senza restituire un risultato matematico
        modifier: Modifier = Modifier,
        // PARAMETRO CON VALORE DI DEFAULT:
        // La sintassi "= MaterialTheme..." significa: "Questo è il colore base.
        // Se lo sviluppatore che mi usa non inserisce nulla, io sarò (Primary).
        // Ma se mi passa un colore diverso, io obbedirò e cambierò!"
        baseColor: Color = MaterialTheme.colorScheme.primary,
        // SLOT API E LAMBDA:
        // 1. "@Composable": Dichiara che questo parametro accetta codice che disegna UI (es. Icon, Text).
        // 2. "RowScope.()": Dona al codice che verrà inserito i "superpoteri" di una Row
        //    (permettendo a chi usa il bottone di usare ad esempio Modifier.weight).
        // 3. "-> Unit": È una funzione Lambda. Significa "Esegui un blocco di istruzioni senza restituire dati matematici".
        content: @Composable RowScope.() -> Unit
    ) {
        // 1. LA SCATOLA ESTERNA (Il comportamento fisico e i colori)
        Surface(
            onClick = onClick,
            // 🧠 DESIGN CHANGE: Stondatura aumentata a 20.dp per coerenza con i pulsanti d'azione (Soft Pill)
            shape = RoundedCornerShape(20.dp),
            // COLORE: Azzurrino pastello (Colore primario con 12% di opacità)
            color = baseColor.copy(alpha = 0.12f),
            // BORDO: Sottile e semi-trasparente
            border = BorderStroke(1.dp, baseColor.copy(alpha = 0.3f)),
            modifier = modifier
        ) {
            // 2. L'IMPALCATURA INTERNA (Come vengono disposti gli elementi)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                // PADDING INTERNO: Il "vestito su misura" del bottone
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                // Qui dentro viene "iniettato" il contenuto personalizzato
                // - Il "content" a SINISTRA è il parametro ufficiale richiesto dal componente Row di Jetpack Compose.
                // - Il "content" a DESTRA è la variabile che abbiamo definito noi qui sopra,
                //   che contiene la grafica (es. Icona e Testo) passata dalla schermata che usa questo bottone.
                // Stiamo dicendo: "Prendi la grafica che ci hanno fornito e stampala esattamente in questo punto".
                content = content
            )
        }
    }

    // ====================================================================
    // COMPONENTE RIUSABILE: FilledActionPill (Bottone Solido e Pieno)
    // ====================================================================
    @Composable
    fun FilledActionPill(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        // Di default usa il colore Primario solido
        containerColor: Color = MaterialTheme.colorScheme.primary,
        content: @Composable RowScope.() -> Unit
    ) {
        Surface(
            onClick = onClick,
            // 🧠 DESIGN CHANGE: Stondatura aumentata a 20.dp per coerenza
            shape = RoundedCornerShape(20.dp),
            // 🎨 Niente '.copy(alpha = ...)'! Il colore qui è pieno al 100%
            color = containerColor,
            // Niente bordino, non serve su un pulsante pieno
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                content = content
            )
        }
    }



    // ====================================================================
    // COMPONENTE: SELETTORE COLORI (ColorPicker)
    // ====================================================================

    /**
     * playerPalette: Una lista fissa di colori vibranti in stile Material 3 Expressive.
     * Questi colori sono stati scelti per garantire un ottimo contrasto visivo tra i giocatori.
     */
    val playerPalette = listOf(
        Color(0xFFE53935), Color(0xFFD81B60), Color(0xFFC972F5), Color(0xFF8349EF),
        Color(0xFF1E88E5), Color(0xFF039BE5), Color(0xFF00ACC1), Color(0xFF00897B),
        Color(0xFF43A047), Color(0xFFC0AC29), Color(0xFFFB8C00), Color(0xFFF4511E)
    )

    // ====================================================================
    // MOTORE DI GENERAZIONE TESTO CONDIVISIONE (BUILDER ASTRATTO)
    // ====================================================================
    /**
     * 🧠 LEZIONE TEORICA I18N: Dependency Injection e Funzioni di Business
     * Questa NON è una funzione @Composable, quindi non possiamo usare il comodo stringResource() qui dentro.
     * Ma abbiamo bisogno delle frasi tradotte (es. "Risultati", "Durata", ecc.) per inviare il testo corretto!
     * * SOLUZIONE (Dependency Injection):
     * Modifichiamo l'intestazione della funzione richiedendo il 'Context' di Android come parametro obbligatorio.
     * Così, chiunque invocherà questa funzione (es. la ResultsScreen), dovrà passargli il context in modo
     * che questa funzione possa "aprire il dizionario" dall'esterno usando 'context.getString(R.string...)'.
     *
     * (Inoltre, per non impazzire con i segnaposto misti tra stringhe costruite e dizionario,
     * abbiamo optato per spezzettare le etichette principali).
     */
    fun buildMatchShareText(
        context: Context, // 🌍 I18N: Parametro aggiunto per l'accesso ai file di sistema!
        title: String,
        durationSeconds: Long,
        timestamp: Long,
        rankedPlayersData: List<Pair<String, Int>>,
        cecchinoData: Pair<String, Int>?,
        inarrestabileData: Pair<String, Int>?,
        gamberoData: Pair<String, Int>?,
        feniceData: Pair<String, Int>?
    ): String {
        return buildString {
            // Appende il titolo della sfida
            appendLine("🏆 ${context.getString(R.string.share_title_prefix)} $title")

            // Logica condizionale per l'inclusione del tempo di gioco
            if (durationSeconds > 0) {
                appendLine("⏱️ ${context.getString(R.string.share_duration_prefix)} ${formatTime(durationSeconds)}")
            }

            if (timestamp > 0L) {
                appendLine("📅 ${context.getString(R.string.share_date_prefix)} ${formatDate(timestamp)}")
            }

            appendLine() // Separatore visivo dell'intestazione

            // ITERAZIONE CLASSIFICA: Trasforma la lista di Coppie in testo formattato
            rankedPlayersData.forEachIndexed { index, (name, score) ->
                val medal = when (index) {
                    0 -> "🥇 1°"
                    1 -> "🥈 2°"
                    2 -> "🥉 3°"
                    else -> "- ${index + 1}°"
                }
                appendLine("$medal $name - $score ${context.getString(R.string.share_points_suffix)}")
            }

            // GESTIONE PREMI
            if (cecchinoData != null || inarrestabileData != null || gamberoData != null || feniceData != null) {
                appendLine("\n🏅 ${context.getString(R.string.share_awards_title)}")

                cecchinoData?.let { (name, value) ->
                    appendLine("  🎯 ${context.getString(R.string.premio_cecchino)}: $name")
                    appendLine("        (+$value ${context.getString(R.string.share_sniper_detail)})")
                }

                inarrestabileData?.let { (name, value) ->
                    appendLine("  🔥 ${context.getString(R.string.premio_inarrestabile)}: $name")
                    appendLine("         ($value ${context.getString(R.string.share_fire_detail)})")
                }

                gamberoData?.let { (name, value) ->
                    appendLine("  🦞 ${context.getString(R.string.premio_gambero)}: $name")
                    appendLine("        (-$value ${context.getString(R.string.share_crab_detail)})")
                }

                feniceData?.let { (name, value) ->
                    appendLine("  🦅 ${context.getString(R.string.premio_fenice)}: $name")
                    appendLine("         (${context.getString(R.string.share_phoenix_detail_start)} +$value ${context.getString(R.string.share_phoenix_detail_end)})")
                }
            }

            appendLine("\n${context.getString(R.string.share_footer_generated)}")
            append(context.getString(R.string.testo_copyright))
        }
    }

    // ====================================================================
    // GESTORE INTENT DI SISTEMA (OS COMMUNICATION)
    // ====================================================================
    /**
     * Isola l'implementazione specifica di Android (Context e Intent).
     * Mantenere la logica di UI separata dalla logica di Sistema Operativo
     * è un pilastro della Clean Architecture.
     */
    fun launchShareIntent(context: android.content.Context, shareText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        // 🌍 I18N: Usa context.getString invece di un testo fisso come "Condividi Classifica"
        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_intent_chooser))
        context.startActivity(shareIntent)
    }

    // ====================================================================
    // COMPONENTE CUSTOM: TESTO AUTO-ADATTIVO (Architettura Reattiva Iterativa)
    // ====================================================================
    @Composable
    fun AutoResizedText(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = MaterialTheme.typography.displaySmall,
        color: Color = style.color,
        fontWeight: FontWeight? = style.fontWeight
    ) {

        /// 1. ALLOCAZIONE DELLO STATO TIPOGRAFICO CON NEUTRALIZZAZIONE LINE-HEIGHT
        // Inizializza un MutableState (Oggetto per la gestione reattiva della memoria).
        var resizedStyle by remember(text) {
            // La funzione .copy() della classe TextStyle permette di sovrascrivere parametri specifici.
            // Assegnando la Costante 'TextUnit.Unspecified' alla Proprietà 'lineHeight',
            // annulliamo i vincoli verticali rigidi del Material Design.
            // In questo modo, l'ingombro sull'asse Y scalerà proporzionalmente alla Proprietà 'fontSize',
            // scongiurando l'overflow verticale irreversibile.
            mutableStateOf(
                style.copy(
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                )
            )
        }

        // 2. SEMAFORO DI RENDERING (Deferred Painting)
        // Variabile di stato booleana che funge da gatekeeper per l'invio dei pixel alla GPU.
        // Viene inizializzata a 'false' per impedire il rendering del componente
        // finché l'algoritmo di misurazione non raggiunge la convergenza matematica.
        var readyToDraw by remember {
            mutableStateOf(false)
        }

        Text(
            text = text,

            // 3. INTERCETTAZIONE DELLA DRAW PHASE
            // Il modificatore drawWithContent si inserisce nell'ultima fase del ciclo
            // di rendering (Composition -> Layout -> Draw).
            // Se 'readyToDraw' è false, il componente occupa spazio computazionale
            // (permettendo i calcoli metrici) ma non esegue 'drawContent()',
            // evitando il fenomeno del flickering (sfarfallio a schermo).
            modifier = modifier.drawWithContent {
                if (readyToDraw) {
                    drawContent()
                }
            },

            // Assegnazione dinamica dello stile. Essendo legata a uno State,
            // ogni sua mutazione forzerà la Ricomposizione (Recomposition) di questo nodo.
            style = resizedStyle,
            color = color,
            fontWeight = fontWeight,

            // 4. VINCOLI SPAZIALI (Constraints)
            // softWrap = false inibisce la segmentazione automatica delle stringhe (line-wrapping).
            // maxLines = 1 obbliga l'engine a generare un singolo vettore orizzontale.
            // Questi due parametri sono necessari per forzare la collisione con il Bounding Box genitore.
            softWrap = false,
            maxLines = 1,

            // 5. OBSERVER DELLA LAYOUT PHASE (Motore Iterativo)
            // Callback asincrona triggerata al termine del calcolo degli ingombri da parte del motore Skia.(motore grafico di Android)
            onTextLayout = { result ->

                // Valutazione della metrica di collisione spaziale.
                // Si usa hasVisualOverflow che è una variabile booleana appartenente alla classe TextLayoutResult
                // Se la dimensione orizzontale calcolata eccede il maxWidth allocato dal parent:
                if (result.hasVisualOverflow) {

                    // Mutazione di stato.
                    // Sovrascrive l'oggetto resizedStyle clonandolo tramite .copy() e applicando
                    // un fattore di degradazione del 5% (0.95) al fontSize corrente.
                    // Questa assegnazione invalida lo stato e innesca immediatamente una nuova
                    // Ricomposizione del componente Text, creando un loop ricorsivo invisibile all'utente.
                    resizedStyle = resizedStyle.copy(
                        fontSize = resizedStyle.fontSize * 0.95
                    )

                } else {

                    // Condizione di uscita dal loop (Convergenza dell'Algoritmo).
                    // Il testo rientra matematicamente nei vincoli imposti.
                    // La mutazione di 'readyToDraw' innesca l'ultima Ricomposizione,
                    // aprendo il gatekeeper nel drawWithContent e permettendo il flushing dei pixel a schermo.
                    readyToDraw = true
                }
            }
        )
    }

    // ====================================================================================
    // GRAFICA E FUNZIONAMENTO BOTTONI DEI TITOLI E GIOCATORI RAPIDI IN CREATEMATCHSCREEN
    // ====================================================================================
    /**
     * Funzione Composable riutilizzabile che incapsula e personalizza il FilterChip nativo.
     * Utilizziamo questo componente per garantire che tutti i pulsanti di selezione rapida
     * abbiano lo stesso stile, altezza e comportamento tipografico in tutta l'applicazione.
     * 
     * @param text La scritta da mostrare (es. il nome del giocatore o del titolo)
     * @param isSelected Booleana che determina se il chip è colorato (attivo) o vuoto
     * @param onClick La funzione da eseguire quando l'utente tocca il chip
     * @param leadingIcon Un'icona opzionale da mostrare a sinistra del testo
     */
    @Composable
    fun CustomSelectableChip(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        leadingIcon: ImageVector? = null
    ) {
        // ====================================================================================
        // 🧠 FIX UI: SURFACE WRAPPER PER L'EFFETTO "SOLLEVATO"
        // ====================================================================
        // Avvolgiamo il FilterChip in una Surface. In Jetpack Compose, la Surface è il modo
        // migliore per dare "corpo fisico" a un componente e proiettare un'ombra reale
        // (Elevation) sul bordo esterno, proprio come abbiamo fatto per le card di gestione.
        Surface(
            // Usiamo il colore 'surface' pulito per far risaltare il pulsante sopra
            // lo sfondo 'surfaceVariant' più scuro del tavolo.
            color = MaterialTheme.colorScheme.surface,
            // 🧠 DESIGN CHANGE: Stondatura aumentata a 20.dp (stile Input precedente) per un look più tondo.
            shape = RoundedCornerShape(20.dp),
            // 🎨 EFFETTO OMBRA: Impostiamo un'elevazione di 4.dp per creare un distacco
            // visibile e premium dallo sfondo, dando l'effetto che la card sia "sollevata".
            shadowElevation = 4.dp,
            // tonalElevation aggiunge una leggera tinta del colore primario allo sfondo (Material 3)
            tonalElevation = 2.dp
        ) {
            // IL COMPONENTE NATIVO FILTERCHIP (Material 3):
            // È un costrutto standard di Google nato specificamente per gestire selezioni binarie (on/off).
            // Riceve il parametro 'selected' e gestisce in autonomia le transizioni cromatiche e le animazioni
            // di riempimento, sollevando lo sviluppatore dal dover calcolare manualmente i cambi di layout visivi.
            FilterChip(
                // Sincronizziamo lo stato visivo interno del chip con la variabile booleana passata come argomento.
                selected = isSelected,

                // Colleghiamo l'evento di pressione fisica dello schermo alla Lambda ricevuta dall'esterno.
                onClick = onClick,

                // Specifichiamo una dimensione minima verticale di 48.dp. Questa misura rispetta le regole
                // globali di accessibilità Android (Touch Target) per garantire una pressione comoda con il pollice.
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),

                // Slot dedicato al contenuto testuale. Iniettiamo l'AutoResizedText custom del progetto:
                // se l'utente scrive un nome o un titolo molto lungo, il font scala matematicamente verso il basso
                // impedendo la collisione visiva o la rottura dei confini fisici del chip.
                label = {
                    // Utilizziamo AutoResizedText per gestire nomi o titoli lunghi,
                    // evitando che il testo esca dai confini del pulsante o si sovrapponga.
                    AutoResizedText(
                        text = text,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },

                // 🧠 DESIGN CHANGE: Ripetiamo la forma a 20.dp per far combaciare perfettamente il chip alla Surface.
                shape = RoundedCornerShape(20.dp),

                // 🧠 RENDERING CONDIZIONALE DELL'ELEMENTO ICONA:
                // Eseguiamo una valutazione logica 'if' direttamente all'interno dello slot della proprietà.
                // - Se leadingIcon contiene dati (not null), Compose alloca la struttura grafica 'Icon' a 18.dp.
                // - Se leadingIcon è null, la proprietà riceve il valore null e Compose salta completamente
                //   il disegno dell'oggetto, ottimizzando l'uso della memoria e lo spazio a schermo.
                leadingIcon = if (leadingIcon != null) {
                    { Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,

                // Configurazione dei colori: abbiamo rimosso l'override del labelColor
                // per ripristinare il colore predefinito del tema (onSurfaceVariant/onSurface),
                // mantenendo invece il colore di accento per lo stato selezionato.
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),

                // 🧠 MODULAZIONE DINAMICA DEL CONTORNO (Micro-interazione visiva):
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    // Se il chip è disattivato, disegna una linea perimetrale sottile del colore primario dell'app.
                    borderColor = MaterialTheme.colorScheme.primary,
                    // Se il chip viene selezionato, il bordo diventa totalmente trasparente, poiché il corpo
                    // del pulsante si riempie visivamente in modalità solida (Color.Transparent evita sovrapposizioni).
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
