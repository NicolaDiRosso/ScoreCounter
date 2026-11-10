    package com.n380.scorecounter.ui.components

    import androidx.compose.animation.AnimatedContent
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.ExperimentalFoundationApi
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.combinedClickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.hapticfeedback.HapticFeedbackType
    import android.content.res.Configuration
    import androidx.compose.ui.platform.LocalConfiguration
    import androidx.compose.ui.platform.LocalHapticFeedback
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.foundation.shape.RoundedCornerShape

    // ---> IMPORT PER ANIMAZIONI E SHIMMER <---
    import androidx.compose.animation.core.*
    import androidx.compose.animation.fadeIn
    import androidx.compose.animation.fadeOut
    import androidx.compose.animation.slideInVertically
    import androidx.compose.animation.slideOutVertically
    import androidx.compose.animation.togetherWith
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.zIndex
    import com.n380.scorecounter.model.Player

    /**
     * ====================================================================
     * COMPONENTE: PlayerAtTableCard (Stile Material You Accent)
     * ====================================================================
     * Questa card viene usata nella schermata di Creazione Sfida.
     * Presenta uno sfondo neutro e una "Accent Bar" laterale che
     * mostra il colore del giocatore, per un design minimale e pulito.
     * ====================================================================
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun PlayerAtTableCard(
        player: Player,
        isFirst: Boolean, // Serve per disabilitare la freccia "Su" se è il primo
        isLast: Boolean,  // Serve per disabilitare la freccia "Giù" se è l'ultimo
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        onEdit: () -> Unit,
        onRemove: () -> Unit,
        //onColorChange: (Int) -> Unit
    ) {
        var haptic =
            LocalHapticFeedback.current //QUESTA VARIABILE DA PROBLEMI, LO DEVO RISOLVERE ALTRIMENTI MI RITROIVO DELLE VIBRAZIONI DOPPIE
        var showColorPalette by remember { mutableStateOf(false) }

        // Trasforma il colore salvato (Int) in un colore grafico (Color)
        val playerColor = Color(player.color)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            /*combinedClickable gestisce il tocco lungo sulla card per aprire la tavolozza dei colori
                .combinedClickable(
                    onClick = {
                        showColorPalette = true
                    }
                ),*/
            // ---> Sfondo Colorato Trasparente (Tonal Surface) <---
            // Prendiamo il colore del giocatore e lo rendiamo trasparente al 12% (0.12f)
            // Questo crea un "bagliore" del colore del giocatore molto elegante.
            colors = CardDefaults.cardColors(
                containerColor = playerColor.copy(alpha = 0.30f)
            ),

            // ---> Bordo coordinato <---
            // Anche il bordo lo facciamo del colore del giocatore ma un po' più visibile (20%)
            border = BorderStroke(1.dp, playerColor.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            // La riga principale che contiene tutto
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp), // Altezza fissa per coerenza
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ---> IL BORDO LATERALE DI ACCENTO <---
                // Questo è il rettangolo colorato a sinistra (L'Accent Bar).
                // Lo ritagliamo (clip) in modo che segua la curva della Card, ma solo sul lato sinistro!
                Box(
                    modifier = Modifier
                        .width(12.dp) // Spessore della barra laterale
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(playerColor) // Colore puro del giocatore
                )

                // Il resto dei controlli (Frecce, Nome, Bottoni)
                // padding 12.dp per aggiungere un margine bilanciato tra la barra colorata di accento e i pulsanti delle frecce
                Row(
                    modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. FRECCE SU/GIU STILIZZATE (Surface con bordo arrotondato e coerenza M3)
                    // 🧠 SPIEGAZIONE DIDATTICA:
                    // Sostituiamo i semplici IconButton nudi con componenti `Surface` interattivi dotati di:
                    // - `RoundedCornerShape(8.dp)`: Stondatura coerente con il design system dell'app.
                    // - `background`: Superficie traslucida per fondersi armoniosamente con il colore della card.
                    // - `border`: Sottile bordo per evidenziare la cliccabilità dei pulsanti di riordinamento.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onMoveUp()
                            },
                            enabled = !isFirst,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Sposta su",
                                    tint = if (isFirst) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onMoveDown()
                            },
                            enabled = !isLast,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Sposta giù",
                                    tint = if (isLast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // 2. IL NOME DEL GIOCATORE
                    AutoResizedText(
                        text = player.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        // padding 12.dp per aggiungere un margine di distanziamento simmetrico tra il nome e i pulsanti
                        modifier = Modifier.weight(1f).padding(start = 12.dp, end = 12.dp)
                    )

                    // ==========================================================
                    // BOTTONI DISTINTI E ARMONIOSI
                    // ==========================================================
                    // Usiamo 'horizontalArrangement' per staccare nettamente le due azioni
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // 3. TASTO MODIFICA (Matita)
                        // 🧠 LEZIONE DI GEOMETRIA UI (I Raggi Concentrici):
                        // Se il contenitore padre (La Card) ha una stondatura di 16.dp,
                        // i bottoni interni devono avere una stondatura leggermente inferiore
                        // (in questo caso 12.dp) per risultare armoniosi e paralleli all'occhio umano.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp) // 🧠 DESIGN CHANGE: Aumentata dimensione da 42 a 48 per facilità di tocco
                                .clip(RoundedCornerShape(12.dp)) // Forma del bottone interno
                                // ---> IL SEGRETO DEL COLORE UNIVERSALE <---
                                // Usiamo il colore di superficie (Bianco/Nero a seconda del tema)
                                // rendendolo leggermente traslucido (Alpha 70%). Questo fa sì che
                                // il bottone sia leggibile e bellissimo QUALUNQUE sia il colore del giocatore sotto!
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onEdit()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Modifica",
                                tint = MaterialTheme.colorScheme.primary, // Icona primario per azione costruttiva
                                modifier = Modifier.size(24.dp)// (misura standard Material) per proporzionarla al nuovo riquadro.
                            )
                        }

                        // 4. TASTO ELIMINA (Cestino)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp) // 🧠 DESIGN CHANGE: Aumentata dimensione da 42 a 48 per facilità di tocco
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    // Nessun haptic qui, perché la logica onRemove ha già la sua Snackbar di allerta
                                    onRemove()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Elimina",
                                tint = MaterialTheme.colorScheme.error, // Icona rossa per azione distruttiva
                                modifier = Modifier.size(24.dp)// (misura standard Material) per proporzionarla al nuovo riquadro.
                            )
                        }
                    }
                }
            }
        }
    }


    /**
     * ====================================================================
     * COMPONENTE: PlayerScoreCard (Stile Material You Accent)
     * ====================================================================
     * Questa card viene usata nella schermata della Sfida in corso.
     * Mantiene il look minimale con la Accent Bar, ma include i controlli
     * "Gamepad" (+ e -) per gestire i punteggi.
     * ====================================================================
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun PlayerScoreCard(
        player: Player,
        isLeader: Boolean = false,
        positiveStep: Int = 10,
        negativeStep: Int = 5,
        onScoreChange: (Int) -> Unit,
        onScoreClick: () -> Unit
    ) {
        // Estraiamo i motori di vibrazione e il colore originale del giocatore
        val haptic = LocalHapticFeedback.current
        val playerColor = Color(player.color)

        // 🧠 MISURAZIONE DINAMICA E RESPONSIVE DEI DPI DEL DISPOSITIVO
        // Utilizziamo `BoxWithConstraints` per misurare la larghezza esatta (maxWidth) assegnata alla card
        // sia nei layout verticali che nelle griglie orizzontali a colonne.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            val configuration = LocalConfiguration.current
            val fontScale = configuration.fontScale
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            // 🧠 LOGICA ADATTIVA CON ANCORAGGIO FISSO:
            // Se la card si trova in uno stato compatto (es. griglia landscape o larghezza < 320.dp),
            // attiviamo la modalità compattata ricalcolando le misure in modo proporzionale.
            val isCompact = availableWidth < 320.dp || isLandscape

            // size 42.dp per impostare la dimensione fissa del pulsante nei layout compatti e 48.dp nei layout estesi
            val buttonSize = if (isCompact) 42.dp else 48.dp
            // size 20.dp per impostare la dimensione fissa dell'icona del pulsante
            val iconSize = if (isCompact) 20.dp else 24.dp
            // width 46.dp per ancorare la larghezza fissa della zona punteggio ed evitare lo spostamento del pulsante meno
            val scoreBoxWidth = if (isCompact) 46.dp else 54.dp
            // width 8.dp per impostare lo spessore della barra laterale d'accento
            val accentWidth = if (isCompact) 8.dp else 10.dp
            // padding 10.dp per il margine interno orizzontale della card
            val horizontalPadding = if (isCompact) 10.dp else 12.dp
            // padding 10.dp per il margine interno verticale della card
            val verticalPadding = if (isCompact) 10.dp else 12.dp

            Card(
                // Il Modifier gestisce lo spazio fisico: occupa tutta la larghezza e metti un margine di 4dp sopra e sotto
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),

                // Lo sfondo della card prende il colore del giocatore sbiadito al 30% (Effetto Vetro/Pastello)
                colors = CardDefaults.cardColors(containerColor = playerColor.copy(alpha = 0.30f)),

                // ----------------------------------------
                // Assegnazione Dinamica con "Inline If"
                // ----------------------------------------
                // Diciamo al parametro 'border' di prendere il risultato di questa scelta logica:
                border = if (isLeader) {
                    // CASO VERO: Se il giocatore è in testa, crea un bordo spesso (3.dp)
                    // e usa 'playerColor' puro al 100% di saturazione. Contrasto perfetto!
                    BorderStroke(4.dp, playerColor)
                } else {
                    // CASO FALSO: Se non è in testa, mantieni il design originale dell'app:
                    // un bordo sottile (1.dp) e semi-trasparente (50%) per dare sobrietà.
                    BorderStroke(1.dp, playerColor.copy(alpha = 0.5f))
                },

                // Arrotondiamo gli angoli della card a 20 pixel di densità
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ---> IL BORDO LATERALE DI ACCENTO <---
                    // width 8.dp per adattare lo spessore dell'accent bar nei layout compatti e 10.dp in quelli estesi
                    Box(
                        modifier = Modifier
                            .width(accentWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                            .background(playerColor)
                    )

                    // ---> CONTENUTO DELLA CARD <---
                    // padding 10.dp per aggiungere un margine interno proporzionato all'ingombro della card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. BLOCCO NOME E CORONA
                        // padding 8.dp per garantire un distanziamento minimo prima dei controlli del punteggio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            val nameStyle = when {
                                isCompact || fontScale > 1.15f -> MaterialTheme.typography.titleMedium
                                else -> MaterialTheme.typography.titleLarge
                            }

                            AutoResizedText(
                                text = player.name,
                                style = nameStyle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        // 2. BLOCCO CONTROLLI ANCORATO E OMOGENEO (Limite Fisso Insuperabile)
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            // PULSANTE MENO (Ancorato)
                            // size 42.dp per impostare una dimensione fissa del pulsante ed evitare disomogeneità visive
                            Box(
                                modifier = Modifier
                                    .size(buttonSize)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onScoreChange(-1)
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onScoreChange(-negativeStep)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Diminuisci",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    // size 20.dp per impostare la dimensione fissa dell'icona
                                    modifier = Modifier.size(iconSize)
                                )
                            }

                            // ---> PUNTEGGIO CON ANCORAGGIO FISSO (Zero Spostamenti) <---
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    // width 46.dp per bloccare rigidamente la larghezza del contenitore senza spingere il pulsante meno
                                    .width(scoreBoxWidth)
                                    // padding 2.dp per aggiungere un margine minimo di sicurezza tra punteggio e pulsanti
                                    .padding(horizontal = 2.dp)
                                    .clickable { onScoreClick() }
                            ) {
                                AnimatedContent(
                                    targetState = player.score,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically(animationSpec = tween(300)) { height -> height } + fadeIn(
                                                animationSpec = tween(300)
                                            )) togetherWith
                                                    (slideOutVertically(animationSpec = tween(300)) { height -> -height } + fadeOut(
                                                        animationSpec = tween(300)
                                                    ))
                                        } else {
                                            (slideInVertically(animationSpec = tween(300)) { height -> -height } + fadeIn(
                                                animationSpec = tween(300)
                                            )) togetherWith
                                                    (slideOutVertically(animationSpec = tween(300)) { height -> height } + fadeOut(
                                                        animationSpec = tween(300)
                                                    ))
                                        }
                                    },
                                    label = "ScoreAnimation"
                                ) { animatedScore ->
                                    val scoreText = animatedScore.toString()
                                    val scoreStyle = when {
                                        isCompact || fontScale > 1.15f -> when {
                                            scoreText.length <= 2 -> MaterialTheme.typography.titleLarge
                                            scoreText.length == 3 -> MaterialTheme.typography.titleMedium
                                            else -> MaterialTheme.typography.bodyMedium
                                        }
                                        else -> when {
                                            scoreText.length <= 2 -> MaterialTheme.typography.headlineMedium
                                            scoreText.length == 3 -> MaterialTheme.typography.titleLarge
                                            else -> MaterialTheme.typography.titleMedium
                                        }
                                    }

                                    Text(
                                        text = scoreText,
                                        style = scoreStyle,
                                        fontWeight = FontWeight.Black,
                                        color = if (player.isOnFire) Color(0xFFF3AF38) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }

                            // PULSANTE PIÙ (Ancorato)
                            // size 42.dp per impostare una dimensione fissa del pulsante nei layout compatti e 48.dp in quelli estesi
                            Box(
                                modifier = Modifier
                                    .size(buttonSize)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onScoreChange(1)
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onScoreChange(positiveStep)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Aumenta",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    // size 20.dp per impostare la dimensione fissa dell'icona
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * ====================================================================
     * COMPONENTE: PlayerResultCard (Stile Prestigio & Material You Accent)
     * ====================================================================
     * Card usata nella schermata della Classifica Finale.
     * Al 1° posto applica un effetto "Luce Passante" (Shimmer), elevation
     * e design pieno. Agli altri applica lo stile accent bar.
     * ====================================================================
     */
    @Composable
    fun PlayerResultCard(
        player: Player,
        position: Int, // 1 per il vincitore, 2 per il secondo, ecc.
        modifier: Modifier = Modifier
    ) {
        val playerColor = Color(player.color)
        val isWinner = position == 1

        // Il vincitore ha uno sfondo solido, gli altri hanno l'alpha al 30% (coerenza con la sfida)
        val cardBackground = if (isWinner) playerColor else playerColor.copy(alpha = 0.30f)
        // Ora tutti i giocatori (vincitore compreso) useranno lo stesso colore di testo!
        val textColor = MaterialTheme.colorScheme.onSurface

        // ====================================================================
        // LOGICA ANIMAZIONE SHIMMER (Riflesso di Luce)
        // ====================================================================
        // Questa animazione viene definita e calcolata SOLO se è il vincitore.
        val shimmerBrush = if (isWinner) {

            // rememberInfiniteTransition: Gestisce animazioni cicliche che non finiscono mai.
            val infiniteTransition = rememberInfiniteTransition(label = "WinnerLightAnimation")

            // animateFloat: Definisce una variabile numerica che fluttua nel tempo.
            val translationX by infiniteTransition.animateFloat(
                initialValue = -500f, // Parte molto a sinistra (fuori dalla card)
                targetValue = 1000f, // Arriva molto a destra (oltre la card)
                animationSpec = infiniteRepeatable(
                    // tween(1500): La luce impiega 1 secondo e mezzo a passare.
                    animation = tween(1500, delayMillis = 1000, easing = LinearEasing),
                    // Restart: Quando finisce, ricomincia da sinistra.
                    repeatMode = RepeatMode.Restart
                ),
                label = "XTranslation"
            )

            // ---> MODIFICA 2: LUCE UNIVERSALE <---
            // Usiamo un bianco puro (Color.White) semi-trasparente per l'effetto luce.
            // In questo modo il riflesso sarà sempre visibile e brillante,
            // indipendentemente dal colore di testo che abbiamo scelto sopra.
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.4f), // Luce più marcata al centro
                    Color.White.copy(alpha = 0.0f),
                ),
                start = Offset(translationX, 0f),
                end = Offset(translationX + 300f, 300f)
            )
        } else {
            null // Se non è il vincitore, non creiamo nessun gradiente per risparmiare risorse
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = if (isWinner) 6.dp else 4.dp) // Più spazio verticale per il vincitore
                // zIndex(1f): Alza il livello di renderizzazione del vincitore per sovrapporre l'ombra perfettamente
                .zIndex(if (isWinner) 1f else 0f),

            colors = CardDefaults.cardColors(containerColor = cardBackground),
            // Il vincitore non ha bordo (è pieno), gli altri hanno il bordo al 50%
            border = if (isWinner) null else BorderStroke(1.dp, playerColor.copy(alpha = 0.5f)),

            // RISALTO STRUTTURALE: Forma più grande per il vincitore (24.dp vs 20.dp)
            shape = RoundedCornerShape(if (isWinner) 24.dp else 20.dp),

            // RISALTO STRUTTURALE (Elevation): Il vincitore è "alzato" (ombra). Gli altri sono "piatti".
            elevation = if (isWinner) CardDefaults.cardElevation(defaultElevation = 8.dp)
            else CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            // Usiamo Box per sovrapporre il gradiente della luce sopra il contenuto
            Box(modifier = Modifier.fillMaxWidth()) {

                // 1. IL CONTENUTO DELLA CARD (Identico a prima, ma con padding ritoccato per il vincitore)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // L'ACCENT BAR LATERALE (Visibile solo per chi NON vince)
                    if (!isWinner) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                                .background(playerColor)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Il vincitore ha molto più "respiro" interno (padding)
                            .padding(horizontal = 16.dp, vertical = if (isWinner) 24.dp else 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // BLOCCO SINISTRA: Posizione + Nome + Trofeo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text(
                                text = "$position°",
                                style = if (isWinner) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            Text(
                                text = player.name,
                                style = if (isWinner) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (isWinner) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = "Vincitore",
                                    tint = Color(0xFFFFD700), // Trofeo Oro (Questo sta bene su tutti gli sfondi perché è un'icona, non un bordo!)
                                    modifier = Modifier.padding(start = 8.dp).size(28.dp)
                                )
                            }
                        }

                        // BLOCCO DESTRA: Punteggio Gigante
                        Text(
                            text = "${player.score} pt",
                            style = if (isWinner) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            maxLines = 1
                        )
                    }
                } // <-- Fine Row contenuto

                // ====================================================================
                // 2. LO STRATO DELLA LUCE (Shimmer Overlay)
                // ====================================================================
                // Sovrapponiamo un Box vuoto che disegna solo il gradiente diagonale.
                if (isWinner && shimmerBrush != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize() // Si adatta perfettamente alle dimensioni della card
                            // .then(if (isWinner)...) Applica lo sfondo del gradiente, che è animato!
                            .background(shimmerBrush)
                            // Aggiungiamo un leggero effetto blur lenticolare sul gradiente stesso (Premium feel)
                            .graphicsLayer { alpha = 0.7f }
                    )
                }
            }
        }
    }

    // ====================================================================
    // 🧠 COMPOSABLE DECOMPOSITION: LA CARD DEI PREMI (AwardCard)
    // ====================================================================
    // Abbiamo estratto la logica delle card dei premi (Cecchino, Gambero, ecc.)
    // per renderla riutilizzabile sia in HomeScreen che in ResultsScreen.
    // Questo si chiama "Principio DRY" (Don't Repeat Yourself).

    @Composable
    fun AwardCard(
        icon: String,           // L'emoji (es. "🎯")
        title: String,          // Il nome del premio (es. "Il Cecchino")
        playerName: String,     // Il nome del giocatore vincente
        playerColorInt: Int,    // Il colore del giocatore (Int ARGB)
        valueText: String,      // Il testo del punteggio da stampare nel badge (es. "+10 pt")
        modifier: Modifier = Modifier
    ) {
        val playerColor = Color(playerColorInt)

        // 🎨 MATERIAL 3 EXPRESSIVE: La Card dei Premi
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = playerColor.copy(alpha = 0.30f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, playerColor.copy(alpha = 0.3f))
        ) {
            // ====================================================================
            // 🧠 LEZIONE TEORICA COMPOSE: LAYOUT A PESI PERCENTUALI REATTIVI (18% / 54% / 28%)
            // ====================================================================
            // Per garantire che la card mantenga proporzioni impeccabili sia su smartphone (colonne strette)
            // che su tablet (colonne ampie), non usiamo dimensioni fisse in dp per le icone o i badge.
            // Dividiamo la Row interna in 3 blocchi con pesi percentuali flessibili:
            // - 18% (weight 0.18f): Box dell'Icona Emoji
            // - 54% (weight 0.54f): Colonna Testi (Titolo Premio + Nome Giocatore)
            // - 28% (weight 0.28f): Card Badge Punteggio/Combo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. BLOCCO ICONA EMOJI (~18% di larghezza della card)
                Box(
                    modifier = Modifier.weight(0.18f),
                    contentAlignment = Alignment.Center
                ) {
                    AutoResizedText(
                        text = icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // 2. BLOCCO TESTI E DETTAGLI (~54% di larghezza della card)
                Column(
                    modifier = Modifier
                        .weight(0.54f)
                        .padding(horizontal = 6.dp)
                ) {
                    AutoResizedText(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    AutoResizedText(
                        text = playerName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                }

                // 3. BLOCCO BADGE PUNTEGGIO/COMBO (~28% di larghezza della card)
                Card(
                    modifier = Modifier.weight(0.28f),
                    colors = CardDefaults.cardColors(containerColor = playerColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AutoResizedText(
                            text = valueText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

