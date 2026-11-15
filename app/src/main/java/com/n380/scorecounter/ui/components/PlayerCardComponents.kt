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
    import androidx.compose.ui.platform.LocalConfiguration
    import androidx.compose.ui.platform.LocalHapticFeedback
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.Dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.unit.TextUnit
    import androidx.compose.ui.hapticfeedback.HapticFeedback
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
     * Supporta una DOPPIA MODALITÀ ADATTIVA in base alle dimensioni e partecipanti:
     * - MODALITÀ ESPANSA (1-4 giocatori o 5 su display alti): Layout verticale arioso
     *   con nome ben visibile in alto a sinistra, comandi ampi ed ergonomici centrati
     *   e punteggio gigante in rilievo. Riempie uniformemente lo spazio disponibile.
     * - MODALITÀ COMPATTA (5+ giocatori o spazio ridotto): Layout su singola riga
     *   ad alta densità informativa con ancoraggio fisso per evitare scrolling eccessivo.
     * ====================================================================
     */
    @OptIn(ExperimentalFoundationApi::class)
    /**
     * ====================================================================
     * METRICHE CARD ESPANSA: Scala Proporzionale Adattiva
     * ====================================================================
     * Raggruppa i valori dimensionali dinamici per pulsanti, icone,
     * spessore accent bar e tipografia in base all'altezza della card / numero giocatori.
     * ====================================================================
     */
    private data class ExpandedCardMetrics(
        val buttonWidth: Dp,
        val buttonHeight: Dp,
        val iconSize: Dp,
        val buttonCorner: Dp,
        val accentWidth: Dp,
        val nameStyle: TextStyle,
        val baseScoreFontSize: TextUnit
    )

    /**
     * ====================================================================
     * COMPONENTE: PlayerScoreCard (Stile Material You Accent)
     * ====================================================================
     * Questa card viene usata nella schermata della Sfida in corso.
     * Supporta una DOPPIA MODALITÀ ADATTIVA in base alle dimensioni e partecipanti:
     * - MODALITÀ ESPANSA (1-4 giocatori o 5 su display alti): Layout verticale arioso
     *   con nome ben visibile in alto a sinistra, comandi ampi ed ergonomici centrati
     *   e punteggio gigante in rilievo. Riempie uniformemente lo spazio disponibile.
     * - MODALITÀ COMPATTA (5+ giocatori o spazio ridotto): Layout su singola riga
     *   ad alta densità informativa con ancoraggio fisso per evitare scrolling eccessivo.
     * ====================================================================
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun PlayerScoreCard(
        player: Player,
        isLeader: Boolean = false,
        positiveStep: Int = 10,
        negativeStep: Int = 5,
        totalPlayers: Int = 0,
        isExpandedMode: Boolean = totalPlayers in 1..4,
        modifier: Modifier = Modifier,
        onScoreChange: (Int) -> Unit,
        onScoreClick: () -> Unit
    ) {
        // Estraiamo i motori di vibrazione e il colore originale del giocatore
        val haptic = LocalHapticFeedback.current
        val playerColor = Color(player.color)

        // 🧠 MISURAZIONE DINAMICA E RESPONSIVE DEI DPI DEL DISPOSITIVO
        // Utilizziamo `BoxWithConstraints` per misurare la larghezza e l'altezza assegnate alla card
        // sia nei layout verticali che nelle griglie orizzontali a colonne.
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight
            val configuration = LocalConfiguration.current
            val fontScale = configuration.fontScale

            // 🧠 RILEVAZIONE ALTEZZA VINCOLATA:
            // Rileviamo se l'altezza assegnata alla card è finita/vincolata (es. tramite Modifier.weight(1f))
            val hasBoundedHeight = availableHeight < Dp.Infinity

            // 🧠 LOGICA ADATTIVA (RISOLUZIONE BUG LANDSCAPE):
            // In precedenza 'isCompact = availableWidth < 320.dp || isLandscape' forzava la modalità compatta
            // in orizzontale anche con 2 o 3 giocatori (dove l'altezza è 160-320dp!), causando card schiacciate
            // e pulsanti minuscoli (44dp).
            // Ora 'isCompact' valuta le reali dimensioni geometriche della card:
            // è compatta solo se la larghezza è < 240.dp o se l'altezza reale assegnata è < 110.dp.
            val isCompact = availableWidth < 240.dp || (hasBoundedHeight && availableHeight < 110.dp)

            // ====================================================================
            // 📏 MISURE E TOKEN DI DESIGN PER IL LAYOUT COMPATTO
            // ====================================================================
            // Scaliamo proporzionalmente i pulsanti se la card compatta riceve un'altezza confortevole
            val buttonSize = when {
                !hasBoundedHeight -> if (isCompact) 42.dp else 48.dp
                availableHeight >= 90.dp -> 48.dp
                availableHeight >= 70.dp -> 44.dp
                else -> 38.dp
            }
            val iconSize = when {
                !hasBoundedHeight -> if (isCompact) 20.dp else 24.dp
                availableHeight >= 90.dp -> 24.dp
                availableHeight >= 70.dp -> 22.dp
                else -> 18.dp
            }
            val scoreBoxWidth = when {
                !hasBoundedHeight -> if (isCompact) 46.dp else 54.dp
                availableHeight >= 90.dp -> 58.dp
                availableHeight >= 70.dp -> 50.dp
                else -> 44.dp
            }
            val accentWidth = if (isCompact) 8.dp else 10.dp
            val horizontalPadding = if (isCompact) 10.dp else 12.dp
            val verticalPadding = if (isCompact) 8.dp else 12.dp

            // ====================================================================
            // 🎨 SCALA PROPORZIONALE ADATTIVA PER LA MODALITÀ ESPANSA (1-4 GIOCATORI)
            // ====================================================================
            // 🧠 RISOLUZIONE PROBLEMA SPATIALE & ERGONOMICO (DISCIPLINA ORIZZONTALE):
            // Quando ci sono 2 o 3 giocatori (o display molto alti), le card diventano
            // notevolmente più alte (150-240dp). Tuttavia, la larghezza del display rimane identica!
            // I pulsanti devono espandersi verticalmente per un'ergonomia touch ideale senza
            // allargarsi eccessivamente in orizzontale, garantendo ampio respiro al punteggio centrale
            // ed evitando che punteggi a 3 o 4 cifre (es. 148, 1000, -25) vengano troncati.
            // Calcoliamo dinamicamente 3 livelli di scala (Tier):
            // - TIER 3 (2 Giocatori o h >= 190dp): Pulsanti squircle (86x74dp), icone 34dp, punteggio base 56sp
            // - TIER 2 (3 Giocatori o h in 135..189dp): Pulsanti medi (80x64dp), icone 30dp, punteggio base 48sp
            // - TIER 1 (4 Giocatori o h < 135dp): Pulsanti compatti (74x52dp), icone 26dp, punteggio base 38sp
            val expandedMetrics = when {
                isCompact -> ExpandedCardMetrics(
                    buttonWidth = 66.dp,
                    buttonHeight = 44.dp,
                    iconSize = 22.dp,
                    buttonCorner = 14.dp,
                    accentWidth = 8.dp,
                    nameStyle = MaterialTheme.typography.titleLarge,
                    baseScoreFontSize = 34.sp
                )
                totalPlayers == 2 || availableHeight >= 190.dp -> ExpandedCardMetrics(
                    // Larghezza contenuta a 86.dp (invece di 112.dp) per liberare oltre 50.dp al punteggio
                    buttonWidth = 86.dp,
                    // Altezza generosa a 74.dp per touch target comodissimo con i pollici
                    buttonHeight = 74.dp,
                    iconSize = 34.dp,
                    buttonCorner = 20.dp,
                    accentWidth = 12.dp,
                    nameStyle = when {
                        fontScale > 1.15f -> MaterialTheme.typography.headlineSmall
                        else -> MaterialTheme.typography.headlineMedium
                    },
                    baseScoreFontSize = 56.sp
                )
                totalPlayers == 3 || (availableHeight in 135.dp..189.dp) -> ExpandedCardMetrics(
                    buttonWidth = 80.dp,
                    buttonHeight = 64.dp,
                    iconSize = 30.dp,
                    buttonCorner = 18.dp,
                    accentWidth = 10.dp,
                    nameStyle = when {
                        fontScale > 1.15f -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.headlineSmall
                    },
                    baseScoreFontSize = 48.sp
                )
                else -> ExpandedCardMetrics(
                    buttonWidth = 74.dp,
                    buttonHeight = 52.dp,
                    iconSize = 26.dp,
                    buttonCorner = 16.dp,
                    accentWidth = 8.dp,
                    nameStyle = when {
                        fontScale > 1.15f -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleLarge
                    },
                    baseScoreFontSize = 38.sp
                )
            }

            // Lo spessore dell'Accent Bar laterale si adatta alla scala attiva
            val actualAccentWidth = if (isExpandedMode) expandedMetrics.accentWidth else accentWidth

            Card(
                // 🧠 GESTIONE DIMENSIONE FISICA:
                // Se la card riceve un vincolo di altezza (es. weight(1f)), occupa tutto lo spazio (fillMaxSize)
                // garantendo che tutte le card abbiano esattamente la stessa altezza senza vuoti asimmetrici,
                // sia in modalità espansa che in modalità compatta!
                // In una lista a scorrimento (unbounded height), usa invece le dimensioni intrinseche.
                modifier = if (hasBoundedHeight) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                },

                // Lo sfondo della card prende il colore del giocatore sbiadito al 30% (Effetto Vetro/Pastello)
                colors = CardDefaults.cardColors(containerColor = playerColor.copy(alpha = 0.30f)),

                // ----------------------------------------
                // Assegnazione Dinamica con "Inline If"
                // ----------------------------------------
                // Diciamo al parametro 'border' di prendere il risultato di questa scelta logica:
                border = if (isLeader) {
                    // CASO VERO: Se il giocatore è in testa, crea un bordo spesso (4.dp)
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
                    modifier = if (hasBoundedHeight) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ---> IL BORDO LATERALE DI ACCENTO <---
                    // Si allinea allo spessore dinamico per mantenere proporzioni perfette con la card
                    Box(
                        modifier = Modifier
                            .width(actualAccentWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                            .background(playerColor)
                    )

                    // 🧠 SWITCH LOGICO DI RENDERING: MODALITÀ ESPANSA vs COMPATTA
                    if (isExpandedMode) {
                        ExpandedScoreCardContent(
                            player = player,
                            isCompact = isCompact,
                            horizontalPadding = horizontalPadding,
                            buttonWidth = expandedMetrics.buttonWidth,
                            buttonHeight = expandedMetrics.buttonHeight,
                            buttonCorner = expandedMetrics.buttonCorner,
                            iconSize = expandedMetrics.iconSize,
                            nameStyle = expandedMetrics.nameStyle,
                            baseScoreFontSize = expandedMetrics.baseScoreFontSize,
                            positiveStep = positiveStep,
                            negativeStep = negativeStep,
                            haptic = haptic,
                            onScoreChange = onScoreChange,
                            onScoreClick = onScoreClick
                        )
                    } else {
                        CompactScoreCardContent(
                            player = player,
                            isCompact = isCompact,
                            hasBoundedHeight = hasBoundedHeight,
                            availableHeight = availableHeight,
                            fontScale = fontScale,
                            horizontalPadding = horizontalPadding,
                            verticalPadding = verticalPadding,
                            buttonSize = buttonSize,
                            iconSize = iconSize,
                            scoreBoxWidth = scoreBoxWidth,
                            positiveStep = positiveStep,
                            negativeStep = negativeStep,
                            haptic = haptic,
                            onScoreChange = onScoreChange,
                            onScoreClick = onScoreClick
                        )
                    }
                }
            }
        }
    }

    /**
     * ====================================================================
     * SUB-COMPONENTE: ExpandedScoreCardContent (Design Material 3 Expressive)
     * ====================================================================
     * Layout verticale arioso per la modalità espansa (1-4 giocatori o display ampi).
     * Dispone:
     * 1. Nome giocatore in alto a sinistra con font ingrandito e dinamico
     *    per maggiore bilanciamento visivo con le dimensioni della card.
     * 2. Riga gamepad con pulsanti (+/-) e punteggio centrale che scalano proporzionalmente
     *    in base all'altezza della card, centrati verticalmente nello spazio restante
     *    tramite Box(Modifier.weight(1f)).
     * ====================================================================
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ExpandedScoreCardContent(
        player: Player,
        isCompact: Boolean,
        horizontalPadding: Dp,
        buttonWidth: Dp,
        buttonHeight: Dp,
        buttonCorner: Dp,
        iconSize: Dp,
        nameStyle: TextStyle,
        baseScoreFontSize: TextUnit,
        positiveStep: Int,
        negativeStep: Int,
        haptic: HapticFeedback,
        onScoreChange: (Int) -> Unit,
        onScoreClick: () -> Unit
    ) {
        val buttonShape = RoundedCornerShape(buttonCorner)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = if (isCompact) 10.dp else 12.dp,
                    bottom = if (isCompact) 10.dp else 12.dp
                )
        ) {
            // ---> 1. NOME DEL GIOCATORE (In alto a sinistra, proporzionato e leggibile) <---
            // Utilizziamo nameStyle calcolato dinamicamente e AutoResizedText per scalare
            // automaticamente se il nome è molto lungo, evitando overflow o a capo sgradevoli.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoResizedText(
                    text = player.name,
                    style = nameStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---> 2. FASCIA PRINCIPALE: CONTROLLI GAMEPAD E PUNTEGGIO ADATTIVI <---
            // 🧠 CENTRATURA VERTICALE ARMONICA:
            // Box con weight(1f) garantisce che i pulsanti e il punteggio rimangano
            // sempre al centro dell'area vitale della card, mentre la loro scala aumentata
            // garantisce un perfetto equilibrio visivo ed ergonomico.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PULSANTE MENO (Dinamico e proporzionato all'altezza della card)
                    // 🧠 CORREZIONE VIVIDIZZA COLORE (TABLET & SMARTPHONE):
                    // Usiamo 'MaterialTheme.colorScheme.error' e 'onError' (colore solido e saturo) al posto di 'errorContainer'
                    // per evitare che il pulsante appaia sbiadito/grigio quando sovrapposto allo sfondo della card.
                    Box(
                        modifier = Modifier
                            .width(buttonWidth)
                            .height(buttonHeight)
                            .clip(buttonShape)
                            .background(MaterialTheme.colorScheme.error)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onError.copy(alpha = 0.3f),
                                shape = buttonShape
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
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    // PUNTEGGIO CENTRALE (Dinamico, enfatico, perfettamente centrato e protetto da troncamenti)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable { onScoreClick() }
                    ) {
                        AnimatedContent(
                            targetState = player.score,
                            modifier = Modifier.fillMaxWidth(),
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
                            label = "ScoreAnimationExpanded"
                        ) { animatedScore ->
                            val scoreText = animatedScore.toString()
                            // 🧠 RIDUZIONE PROPORZIONALE PREVENTIVA DEL FONT:
                            // Calcoliamo la dimensione iniziale in base al numero di caratteri (gestendo valori negativi o 3+ cifre).
                            val targetFontSize = when {
                                scoreText.length <= 2 -> baseScoreFontSize
                                scoreText.length == 3 -> (baseScoreFontSize.value * 0.72f).sp
                                scoreText.length == 4 -> (baseScoreFontSize.value * 0.55f).sp
                                else -> (baseScoreFontSize.value * 0.42f).sp
                            }

                            // 🧠 PROTEZIONE ANTI-TRONCAMENTO (AutoResizedText):
                            // Se anche con la riduzione preventiva il testo supera la larghezza disponibile
                            // (es. display stretti o accessibility font scale alto), AutoResizedText riduce il font
                            // in modo reattivo garantendo che NESSUNA cifra (es. il '148' di Marco) venga mai tagliata!
                            AutoResizedText(
                                text = scoreText,
                                style = TextStyle(
                                    fontSize = targetFontSize,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                ),
                                color = if (player.isOnFire) Color(0xFFF3AF38) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // PULSANTE PIÙ (Dinamico e proporzionato all'altezza della card)
                    // 🧠 CORREZIONE VIVIDIZZA COLORE (TABLET & SMARTPHONE):
                    // Usiamo 'MaterialTheme.colorScheme.primary' e 'onPrimary' (colore solido e saturo) al posto di 'primaryContainer'
                    // per evitare che il pulsante appaia sbiadito/grigio quando sovrapposto allo sfondo della card.
                    Box(
                        modifier = Modifier
                            .width(buttonWidth)
                            .height(buttonHeight)
                            .clip(buttonShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                shape = buttonShape
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
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }

    /**
     * ====================================================================
     * SUB-COMPONENTE: CompactScoreCardContent (Layout Compatto ad Alta Densità)
     * ====================================================================
     * Layout orizzontale a riga singola per 5 o più giocatori o schermi ridotti.
     * Mantiene rigorosamente l'ancoraggio fisso dei controlli per garantire
     * la massima densità informativa ed evitare disomogeneità visive.
     * ====================================================================
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun CompactScoreCardContent(
        player: Player,
        isCompact: Boolean,
        hasBoundedHeight: Boolean = false,
        availableHeight: Dp = Dp.Infinity,
        fontScale: Float,
        horizontalPadding: Dp,
        verticalPadding: Dp,
        buttonSize: Dp,
        iconSize: Dp,
        scoreBoxWidth: Dp,
        positiveStep: Int,
        negativeStep: Int,
        haptic: HapticFeedback,
        onScoreChange: (Int) -> Unit,
        onScoreClick: () -> Unit
    ) {
        Row(
            modifier = if (hasBoundedHeight) {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            },
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
                    hasBoundedHeight && availableHeight >= 90.dp -> MaterialTheme.typography.titleLarge
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
                // 🧠 CORREZIONE VIVIDIZZA COLORE (TABLET & SMARTPHONE):
                // Usiamo 'MaterialTheme.colorScheme.error' e 'onError' (colore solido e saturo) al posto di 'errorContainer'
                // per evitare che il pulsante appaia sbiadito/grigio quando sovrapposto allo sfondo della card.
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onError.copy(alpha = 0.3f),
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
                        tint = MaterialTheme.colorScheme.onError,
                        // size 20.dp per impostare la dimensione fissa dell'icona
                        modifier = Modifier.size(iconSize)
                    )
                }

                // ---> PUNTEGGIO CON ANCORAGGIO FISSO (Zero Spostamenti e Auto-Resize) <---
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        // width bloccata con scoreBoxWidth calibrata dinamicamente in base all'altezza della card
                        .width(scoreBoxWidth)
                        // padding 2.dp per aggiungere un margine minimo di sicurezza tra punteggio e pulsanti
                        .padding(horizontal = 2.dp)
                        .clickable { onScoreClick() }
                ) {
                    AnimatedContent(
                        targetState = player.score,
                        modifier = Modifier.fillMaxWidth(),
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
                            hasBoundedHeight && availableHeight >= 90.dp -> when {
                                scoreText.length <= 2 -> MaterialTheme.typography.headlineMedium
                                scoreText.length == 3 -> MaterialTheme.typography.titleLarge
                                else -> MaterialTheme.typography.titleMedium
                            }
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

                        AutoResizedText(
                            text = scoreText,
                            style = scoreStyle.copy(textAlign = TextAlign.Center),
                            fontWeight = FontWeight.Black,
                            color = if (player.isOnFire) Color(0xFFF3AF38) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // PULSANTE PIÙ (Ancorato)
                // 🧠 CORREZIONE VIVIDIZZA COLORE (TABLET & SMARTPHONE):
                // Usiamo 'MaterialTheme.colorScheme.primary' e 'onPrimary' (colore solido e saturo) al posto di 'primaryContainer'
                // per evitare che il pulsante appaia sbiadito/grigio quando sovrapposto allo sfondo della card.
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
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
                        tint = MaterialTheme.colorScheme.onPrimary,
                        // size 20.dp per impostare la dimensione fissa dell'icona
                        modifier = Modifier.size(iconSize)
                    )
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

        // Verifica booleana per determinare se il testo del badge esiste e non è vuoto (es. esclude il premio Testa a testa che non ha un valore numerico)
        val hasValue = valueText.isNotBlank()

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
            // 🧠 LEZIONE TEORICA COMPOSE: LAYOUT A PESI PERCENTUALI FISSI (18% / 54% / 28%)
            // ====================================================================
            // Per garantire che la colonna dei testi si trovi sempre esattamente alla stessa
            // coordinata orizzontale su qualsiasi card e risoluzione (anche quando manca il badge numerico
            // come nel premio "Testa a testa"), manteniamo pesi percentuali fissi e costanti (18% / 54% / 28%).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. BLOCCO ICONA EMOJI (18% di larghezza della card)
                Box(
                    modifier = Modifier.weight(0.18f),
                    contentAlignment = Alignment.Center
                ) {
                    AutoResizedText(
                        text = icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // 2. BLOCCO TESTI E DETTAGLI (54% di larghezza della card, rigorosamente allineato)
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

                // 3. BLOCCO BADGE PUNTEGGIO/COMBO o Spaziatore Neutro (28% di larghezza della card)
                if (hasValue) {
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
                } else {
                    // Spaziatore invisibile con lo stesso peso (0.28f) per preservare l'allineamento geometrico perfetto della griglia
                    Spacer(modifier = Modifier.weight(0.28f))
                }
            }
        }
    }

