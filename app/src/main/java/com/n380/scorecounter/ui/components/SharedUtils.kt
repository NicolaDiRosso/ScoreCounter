package com.n380.scorecounter.ui.components

    import android.content.Intent
    import android.graphics.Paint
    import android.graphics.Typeface
    import androidx.compose.animation.core.*
    import androidx.compose.foundation.Canvas
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border // <-- IMPORTANTE: Serve per disegnare il bordo del cerchio selezionato
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.horizontalScroll
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyRow // <-- IMPORTANTE: Serve per la riga dei colori scorrevole
    import androidx.compose.foundation.lazy.items // <-- IMPORTANTE: Serve per ciclare la lista dei colori
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.*
    import androidx.compose.material3.Icon
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.draw.drawWithContent
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.Path
    import androidx.compose.ui.graphics.StrokeCap
    import androidx.compose.ui.graphics.StrokeJoin
    import androidx.compose.ui.graphics.drawscope.Stroke
    import androidx.compose.ui.graphics.Brush // Serve per il pallino arcobaleno
    import androidx.compose.ui.graphics.nativeCanvas // PERMETTE DI DISEGNARE TESTI NEL CANVAS
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
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

    // FUNZIONE DI SUPPORTO: Formatta i millisecondi in una Data (Es. 8 Nov 2026)
    fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN)
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
     * COMPONENTE DIDATTICO: ScoreChart
     * Disegna il grafico cartesiano dei punteggi usando la geometria vettoriale (Canvas).
     * * @param players Lista dei giocatori con i loro storici punti.
     * @param modifier Modificatore per gestire dimensioni e padding esterni.
     * @param isDetailed Se VERO, il grafico disegna la griglia di sfondo, l'Asse Y (Punteggi) e l'Asse X (Cronologia).
     */
    @Composable
    fun ScoreChart(
        players: List<PlayerRecord>,
        modifier: Modifier = Modifier,
        isDetailed: Boolean = false
    ) {
        // PROTEZIONE: Se non ci sono giocatori o non ci sono punteggi, fermati (evita crash)
        if (players.isEmpty()) return
        val validPlayers = players.filter { (it.scoreHistory ?: emptyList()).isNotEmpty() }
        if (validPlayers.isEmpty()) return

        // MATEMATICA DI BASE: Trova il punteggio massimo (Soffitto) e minimo (Pavimento)
        // maxOrNull() controlla tutta la lista di un giocatore. maxOf() confronta tutti i giocatori.
        val maxScore = validPlayers.maxOf { (it.scoreHistory ?: emptyList()).maxOrNull() ?: 0 }
        val minScore = validPlayers.minOf { (it.scoreHistory ?: emptyList()).minOrNull() ?: 0 }

        Column(modifier = modifier) {
            // Il Canvas è la "Tela" dove usiamo coordinate X (sinistra/destra) e Y (alto/basso)
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {

                // ==============================================================
                // FASE 1: SPAZIO DI LAVORO E MARGINI (Padding)
                // ==============================================================
                // padYTop: Spazio in alto per non far sbattere la linea contro il bordo
                val padYTop = 10.dp.toPx()
                // padYBottom: Se dettagliato, lasciamo molto spazio sotto (50.dp) per le scritte dell'Asse X
                val padYBottom = if (isDetailed) 50.dp.toPx() else 16.dp.toPx()
                // padX: Se dettagliato, lasciamo tanto spazio a sinistra (20.dp) per farci stare i numeri dell'Asse Y!
                val padX = if (isDetailed) 20.dp.toPx() else 16.dp.toPx()

                // drawW (Larghezza) e drawH (Altezza): Lo spazio EFFETTIVO in cui possiamo tracciare le linee
                val drawW = (size.width - padX * 2).coerceAtLeast(1f)
                val drawH = (size.height - padYTop - padYBottom).coerceAtLeast(1f)

                // yRange: L'escursione totale dei punti (Es. se il min è -5 e il max è 20, il range è 25)
                val yRange = (maxScore - minScore).coerceAtLeast(1).toFloat()

                // ==============================================================
                // FASE 2: DISEGNO DELLA GRIGLIA E DEGLI ASSI
                // ==============================================================
                if (isDetailed) {
                    // IL PENNELLO DI TESTO: Serve per comunicare con il sistema grafico base di Android
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.LTGRAY // Grigio chiaro, ottimo per sfondi scuri
                        textSize = 32f // Dimensione del font
                        textAlign = Paint.Align.RIGHT // Allinea i numeri a destra (contro l'asse)
                        typeface = Typeface.DEFAULT_BOLD // Grassetto
                    }

                    // --- ASSE Y (VERTICALE: I PUNTEGGI) E GRIGLIA ---
                    // Disegniamo la riga principale verticale (Il "Palo" dell'asse Y)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(padX, padYTop),
                        end = Offset(padX, size.height - padYBottom),
                        strokeWidth = 3f
                    )

                    // Creiamo 4 righe orizzontali di riferimento (0%, 25%, 50%, 75%, 100%)
                    val steps = 4
                    for (i in 0..steps) {
                        // Calcolo della Y: Partiamo dal basso (padYTop + drawH) e saliamo sottraendo pixel
                        val y = padYTop + drawH - (i * (drawH / steps))

                        // Disegniamo la riga della griglia (sottile e molto trasparente)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(padX, y),
                            end = Offset(padX + drawW, y),
                            strokeWidth = 2f
                        )

                        // Calcoliamo quale valore numerico rappresenta questa riga
                        val value = minScore + (yRange / steps) * i

                        // Disegniamo il testo (Es. "15", "30") a sinistra dell'asse Y
                        drawContext.canvas.nativeCanvas.drawText(
                            value.toInt().toString(), // Arrotonda a intero
                            padX - 24f, // Lo stacca di 24 pixel verso sinistra
                            y + 10f, // Lo abbassa leggermente per centrarlo con la riga
                            textPaint
                        )
                    }

                    // --- ASSE X (ORIZZONTALE: IL PROGRESSO TEMPORALE) ---
                    // Disegniamo la riga principale orizzontale (Il "Pavimento")
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(padX, size.height - padYBottom),
                        end = Offset(padX + drawW, size.height - padYBottom),
                        strokeWidth = 3f
                    )

                    // Etichette per l'asse X (Rappresentano la cronologia della partita)
                    val xLabels = listOf("Inizio", "Metà", "Fine")
                    textPaint.textAlign = Paint.Align.CENTER // Cambiamo allineamento al centro per l'Asse X

                    xLabels.forEachIndexed { i, label ->
                        // Calcoliamo la posizione orizzontale (0, Metà schermo, Fine schermo)
                        val xPos = padX + i * (drawW / 2)

                        // Tacca grigia indicativa sull'asse
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(xPos, size.height - padYBottom),
                            end = Offset(xPos, size.height - padYBottom + 12f),
                            strokeWidth = 3f
                        )

                        // Scritta temporale ("Inizio", "Metà", "Fine") sotto la tacca
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            xPos,
                            size.height - padYBottom + 45f, // Più in basso per non toccare la riga
                            textPaint
                        )
                    }
                }

                // ==============================================================
                // FASE 3: IL DISEGNO DELLE LINEE DEI GIOCATORI
                // ==============================================================
                validPlayers.forEachIndexed { index, player ->
                    val color = Color(player.color)
                    val path = Path()
                    val history = player.scoreHistory ?: emptyList()

                    if (history.size == 1) {
                        // Se c'è un solo punto (partita appena iniziata), disegna una linea dritta
                        val y = padYTop + drawH - ((history[0] - minScore) / yRange * drawH)
                        path.moveTo(padX, y)
                        path.lineTo(padX + drawW, y)
                        drawCircle(color, 6.dp.toPx(), Offset(padX, y))
                        drawCircle(color, 6.dp.toPx(), Offset(padX + drawW, y))
                    } else {
                        // LA MAGIA: Calcoliamo il "Passo" orizzontale specifico per questo giocatore.
                        // Facendo così, la sua linea partirà sempre dallo 0% (Inizio) e si stirerà fino al 100% (Fine),
                        // risolvendo il bug grafico della linea storta!
                        val localXStep = drawW / (history.size - 1).toFloat()

                        history.forEachIndexed { turn, score ->
                            val x = padX + (turn * localXStep) // Avanzamento orizzontale nel tempo
                            val y = padYTop + drawH - ((score - minScore) / yRange * drawH) // Altezza del punteggio

                            if (turn == 0) path.moveTo(x, y) // Primo punto, poggia il pennarello
                            else path.lineTo(x, y) // Tira la riga

                            // ====================================================================
                            // 🧠 TEORIA VISIVA: IL "PESO" DEI PUNTI (Ink-to-Data Ratio)
                            // Quando un grafico ha molti punti ravvicinati, pallini troppo grandi
                            // creano un effetto "collana di perle" che spezza la continuità della linea.
                            // Tecnicamente, riducendo il raggio (radius), abbassiamo il rumore visivo
                            // sui vertici, permettendo all'occhio di seguire meglio la "rotta" (il Path).
                            // 64.dp -> 3.dp (Dettagliato) e 2.dp (Semplice) è il bilanciamento ideale.
                            // ====================================================================
                            val radius = if (isDetailed) 3.dp.toPx() else 2.dp.toPx()
                            drawCircle(color, radius, Offset(x, y))
                        }
                    }

                    // Eseguiamo il disegno fisico del tracciato salvato nel 'path'
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = if (isDetailed) 3.dp.toPx() else 2.dp.toPx(), // Linea proporzionata ai nuovi pallini
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // ---> LA LEGENDA IN FONDO (Invariata) <---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                validPlayers.forEachIndexed { index, player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp, bottom = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(player.color)))
                        Text(text = player.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
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

    // ====================================================================
    // COMPONENTE: SELETTORE COLORI (ColorPicker)
    // ====================================================================

    /**
     * playerPalette: Una lista fissa di colori vibranti in stile Material 3 Expressive.
     * Questi colori sono stati scelti per garantire un ottimo contrasto visivo tra i giocatori.
     */
    val playerPalette = listOf(
        Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA), Color(0xFF5E35B1),
        Color(0xFF1E88E5), Color(0xFF039BE5), Color(0xFF00ACC1), Color(0xFF00897B),
        Color(0xFF43A047), Color(0xFFFDD835), Color(0xFFFB8C00), Color(0xFFF4511E)
    )

    /**
     * ColorPickerRow: Crea una riga scorrevole di pulsanti circolari colorati.
     * @param selectedColor: Il colore che l'utente ha attualmente cliccato (per disegnare il bordo di selezione).
     * @param onColorSelected: Una funzione (lambda) che avvisa l'app quando l'utente cambia scelta cromatica.
     */
    @Composable
    fun ColorPickerRow(
        selectedColor: Color,
        onColorSelected: (Color) -> Unit,
        modifier: Modifier = Modifier
    ) {
        // ---> 1. LA LISTA UNITA (Il Jolly + I Colori Normali) <---
        // Creiamo una nuova lista mettendo Color.Unspecified al primo posto,
        // seguito da tutti gli altri colori della nostra palette.
        val paletteWithRandom = listOf(Color.Unspecified) + playerPalette

        // LazyRow: Disegna graficamente solo i cerchi visibili
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            // ---> FIX LOGICO: Usiamo la nuova lista 'paletteWithRandom'! <---
            items(paletteWithRandom) { color ->

                val isSelected = color == selectedColor

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(15.dp))// RoundedCornerShape serve a dare quell'effetto "squadrato ma morbido" perfetto.
                        // ---> 2. IL DISEGNO INTELLIGENTE (Modifier.then) <---
                        // .then() ci permette di applicare modifiche grafiche diverse in base a una condizione
                        .then(
                            if (color == Color.Unspecified) {
                                // Se è il Jolly: Disegna un gradiente arcobaleno a ruota
                                Modifier.background(
                                    Brush.sweepGradient(//definisce come un'area viene riempita. Invece della classica tinta unita, Jetpack Compose offre i Gradienti.
                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                    )
                                )
                            } else {
                                // Se è un colore normale: Disegna la tinta unita
                                Modifier.background(color)
                            }
                        )
                        // ---> 3. IL BORDO DI SELEZIONE <---
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            // Selezionato = Bordo Blu (Primary). Non selezionato = Bordo invisibile.
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(15.dp)
                        /* * 💡 CURIOSITÀ DI DESIGN: Le Forme e lo "Squircle"
                         * Il raggio di stondatura (es. 12.dp) definisce la forma geometrica finale:
                         * - 0.dp: Quadrato perfetto, spigoloso e netto.
                         * - 20.dp: Cerchio perfetto (la metà esatta della grandezza totale, che qui è 40.dp).
                         * - 8.dp ~ 16.dp: "Squircle" (Square + Circle). È la moderna forma a "mattonella"
                         * morbidamente arrotondata, standard del Material Design 3 e delle icone smartphone!
                         */

                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }

    // ====================================================================
    // MOTORE DI GENERAZIONE TESTO CONDIVISIONE (BUILDER ASTRATTO)
    // ====================================================================
    /**
     * Questa funzione applica il principio di Astrazione: è completamente agnostica
     * rispetto allo stato dell'app (non sa se la partita è in corso o finita da mesi).
     * Accetta solo tipi di dati primitivi e costrutti standard (String, Long, Pair).
     */
    fun buildMatchShareText(
        title: String,
        durationSeconds: Long,
        timestamp: Long,
        // Pair<String, Int> è il nostro "formato universale".
        // Il campo 'first' sarà sempre il Nome, il campo 'second' sarà il Punteggio.
        rankedPlayersData: List<Pair<String, Int>>,
        cecchinoData: Pair<String, Int>?,
        inarrestabileData: Pair<String, Int>?,
        gamberoData: Pair<String, Int>?,
        feniceData: Pair<String, Int>?
    ): String {
        return buildString {
            appendLine("🏆 Risultati: $title")

            // Logica condizionale per l'inclusione del tempo di gioco
            if (durationSeconds > 0) {
                appendLine("⏱️ Durata: ${formatTime(durationSeconds)}")
            }

            // Il timestamp a 0L indica una partita legacy (vecchia) salvata prima
            // che introducessimo la registrazione delle date. Lo saltiamo per retrocompatibilità.
            if (timestamp > 0L) {
                appendLine("📅 Data: ${formatDate(timestamp)}")
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
                appendLine("$medal $name - $score pt")
            }

            // GESTIONE PREMI: Operatore logico OR (||) globale.
            // Il blocco viene renderizzato solo se l'engine rileva almeno un'istanza valida di premio.
            if (cecchinoData != null || inarrestabileData != null || gamberoData != null || feniceData != null) {
                appendLine("\n🏅 PREMI SPECIALI:")

                // DESTRUTTURAZIONE SCOPE FUNCTION:
                // .let estrae il valore dal Nullable. (name, value) destruttura la Pair in due variabili locali.
                cecchinoData?.let { (name, value) ->
                    appendLine("  🎯 Cecchino: $name \n        (+$value pt in un colpo)")
                }

                inarrestabileData?.let { (name, value) ->
                    appendLine("  🔥 Inarrestabile: $name \n         ($value combo On Fire)")
                }

                gamberoData?.let { (name, value) ->
                    appendLine("  🦞 Il Gambero: $name \n        (-$value pt tolti)")
                }

                feniceData?.let { (name, value) ->
                    appendLine("  🦅 La Fenice: $name \n         (rimonta da +$value pt)")
                }
            }

            appendLine("\nGenerato con ScoreCounter 🎮")
            append("© 2026 Creato da NicolA380")
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
        val shareIntent = Intent.createChooser(sendIntent, "Condividi Classifica")
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

        // 1. ALLOCAZIONE DELLO STATO TIPOGRAFICO (State Hoisting)
        // Inizializza un MutableState contenente l'oggetto TextStyle originale.
        // L'utilizzo di 'remember(text)' agisce come Cache Invalidation Key:
        // istruisce il framework a distruggere lo stato corrente e a riallocarlo
        // dal valore iniziale ('style') se e solo se la reference della stringa 'text' muta.
        var resizedStyle by remember(text) {
            mutableStateOf(style)
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