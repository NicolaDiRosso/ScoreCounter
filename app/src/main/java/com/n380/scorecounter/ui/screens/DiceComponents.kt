package com.n380.scorecounter.ui.screens

// ====================================================================
// 1. AREA IMPORTAZIONI E LIBRERIE
// ====================================================================
import android.content.res.Configuration // Import per rilevare l'orientamento dello schermo (Portrait o Landscape)
import androidx.compose.animation.core.Animatable // Gestore nativo per variabili numeriche animate ad alte prestazioni
import androidx.compose.animation.core.FastOutSlowInEasing // Curva di accelerazione/decelerazione fluida per l'animazione
import androidx.compose.animation.core.LinearEasing // Curva di velocità costante senza accelerazione
import androidx.compose.animation.core.Spring // Costanti fisiche per le animazioni a molla di Jetpack Compose
import androidx.compose.animation.core.spring // Funzione per creare un'animazione con comportamento elastico (Spring)
import androidx.compose.animation.core.tween // Funzione per configurare la durata in millisecondi delle transizioni
import androidx.compose.foundation.BorderStroke // Strumento per tracciare bordi attorno ai componenti Surface e Card
import androidx.compose.foundation.background // Modificatore per applicare colori o sfumature di sfondo
import androidx.compose.foundation.clickable // Modificatore per rendere un elemento della UI reattivo ai tocchi
import androidx.compose.foundation.layout.* // Import di massa per layout (Box, Row, Column, Size, Padding, Alignment)
import androidx.compose.foundation.rememberScrollState // Ricorda lo stato dello scorrimento per contenitori scorrevoli
import androidx.compose.foundation.shape.RoundedCornerShape // Permette di creare angoli arrotondati o forme "squircle"
import androidx.compose.foundation.text.KeyboardOptions // Opzioni per personalizzare il tipo di tastiera (es. numerica)
import androidx.compose.foundation.verticalScroll // Modificatore per abilitare lo scorrimento verticale
import androidx.compose.material.icons.Icons // Catalogo di icone vettoriali standard di Google Material Design
import androidx.compose.material.icons.filled.Settings // Icona specifica dell'ingranaggio per le impostazioni
import androidx.compose.material3.* // Design System Google Material 3 (Surface, Card, Text, Button, AlertDialog)
import androidx.compose.runtime.* // Stato reattivo di Compose (remember, mutableStateOf, LaunchedEffect, getValue, setValue)
import androidx.compose.ui.Alignment // Strumento per allineare gli elementi negli assi X e Y (Center, TopStart, ecc.)
import androidx.compose.ui.Modifier // Il "pennello" di Compose per modificare dimensioni, bordi, gesti e grafica
import androidx.compose.ui.draw.clip // Taglia la grafica di un elemento secondo una specifica forma geometrica
import androidx.compose.ui.graphics.Color // Rappresentazione dei colori nei formati RGB o ARGB
import androidx.compose.ui.graphics.graphicsLayer // Layer grafico avanzato per applicare rotazioni 3D, scala, opacità e cameraDistance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType // Tipi di vibrazioni tattili del sistema operativo (Confirm, LongPress, TextHandleMove)
import androidx.compose.ui.platform.LocalConfiguration // Fornisce informazioni sulla configurazione corrente del dispositivo (DPI, Orientamento)
import androidx.compose.ui.platform.LocalHapticFeedback // Accede al motore di vibrazione della tastiera/schermo del dispositivo
import androidx.compose.ui.res.stringResource // Permette di leggere le stringhe tradotte dai file XML delle risorse
import androidx.compose.ui.text.font.FontWeight // Definizione del peso del testo (Bold, ExtraBold, Black)
import androidx.compose.ui.text.input.KeyboardType // Tipo di input della tastiera (Number, Text, ecc.)
import androidx.compose.ui.text.style.TextAlign // Allineamento del testo (Center, Start, End)
import androidx.compose.ui.unit.dp // Unità di misura scalabile "Density-independent Pixels"
import androidx.compose.ui.unit.sp // Unità di misura scalabile "Scale-independent Pixels" per la tipografia
import androidx.compose.ui.window.Dialog // Componente per creare finestre sovrapposte (Dialoghi modali)
import androidx.compose.ui.window.DialogProperties // Proprietà avanzate per configurare il comportamento del Dialog
import com.n380.scorecounter.R // Riferimento generato agli ID delle risorse del progetto
import com.n380.scorecounter.ui.components.AutoResizedText // Componente custom dell'app che scala il testo per non farlo uscire dai bordi
import com.n380.scorecounter.ui.components.AnimatedSelectableChip // Componente animato riutilizzabile per i preset di selezione rapida
import kotlinx.coroutines.delay // Funzione di sospensione per mettere in pausa la coroutine per X millisecondi
import kotlinx.coroutines.launch // Innesca l'esecuzione concorrente di un blocco di codice in background

/**
 * ====================================================================
 * COMPONENTE: DiceSettingsDialog
 * ====================================================================
 * Questo popup appare nella schermata di "Nuova Sfida".
 * Serve per far scegliere all'utente quante facce avrà il dado per
 * gli spareggi o per decidere chi inizia.
 * ====================================================================
 */
@Composable
fun DiceSettingsDialog(
    currentSides: Int,
    onSidesChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var pendingDiceSides by remember { mutableIntStateOf(currentSides) }
    var customDiceInput by remember { mutableStateOf("") }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // ====================================================================
        // 🧠 LEZIONE TEORICA ORIZZONTALE: 2-Column Split Dialog Reattivo (75% Larghezza)
        // ====================================================================
        // 1. LARGHEZZA PERCENTUALE (fillMaxWidth = 0.75f, fillMaxHeight = 0.85f):
        //    Invece di usare dimensioni fisse in dp (che sui tablet risultano piccole e compresse),
        //    usiamo il 75% della larghezza e l'90% dell'altezza totale dello schermo.
        //    In questo modo il dialog scala perfettamente sia su smartphone che su tablet!
        //
        // 2. DISPOSIZIONE A 2 COLONNE:
        //    - Sinistra (42%): Titolo, Descrizione e Pulsanti Annulla/Applica ancorati in basso a sinistra.
        //    - Destra (58%): Formati dadi rapidi e Campo d'inserimento manuale a TUTTA ALTEZZA.
        //
        // 3. PADDING INTERNO DELLE CARTE DADI:
        //    Le scritte (es. "Dado (D6)") hanno 12.dp di padding orizzontale interno per non essere soffocate.
        Dialog(
            onDismissRequest = { }, // 🧠 BLOCCO CHIUSURA ACCIDENTALE: Si chiude solo coi pulsanti espliciti
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.90f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // COLONNA SINISTRA (42%): TITOLO, DESCRIZIONE & PULSANTI ANNULLA/APPLICA
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.titolo_impostazioni_dado),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.desc_impostazioni_dado),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // PULSANTI ANNULLA & APPLICA (Ancorati in basso a sinistra sotto la descrizione)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onDismiss()
                                }
                            ) {
                                AutoResizedText(stringResource(R.string.btn_annulla))
                            }

                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    val manualSides = customDiceInput.toIntOrNull()
                                    val finalSides = if (manualSides != null && manualSides > 0) manualSides else pendingDiceSides

                                    onSidesChanged(finalSides)
                                    onDismiss()
                                }
                            ) {
                                AutoResizedText(stringResource(R.string.btn_applica))
                            }
                        }
                    }

                    // COLONNA DESTRA (58%): FORMATIONI RAPIDI & INSERIMENTO MANUALE (TUTTA ALTEZZA)
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_scegli_formato_dado),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        // GRIGLIA DADI RAPIDI (2x2) CON PADDING INTERNO SPAZIOSO (12dp)
                        val diceOptions = listOf(6, 10, 20, 100)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            diceOptions.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { sides ->
                                        val isSelected = pendingDiceSides == sides && customDiceInput.isEmpty()
                                        AnimatedSelectableChip(
                                            text = stringResource(R.string.label_dado, sides),
                                            isSelected = isSelected,
                                            onClick = {
                                                pendingDiceSides = sides
                                                customDiceInput = ""
                                            },
                                            height = 40.dp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // INSERIMENTO MANUALE
                        Text(
                            text = stringResource(R.string.label_inserimento_manuale),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = customDiceInput,
                            onValueChange = { if (it.all { char -> char.isDigit() }) customDiceInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.hint_es_facce_dado), style = MaterialTheme.typography.bodySmall) },
                            label = { Text(stringResource(R.string.hint_n_facce), style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
        }
    } else {
        // ====================================================================
        // ORIENTAMENTO VERTICALE (Layout Standard Portrait per AlertDialog)
        // ====================================================================
        AlertDialog(
            onDismissRequest = { }, // 🧠 BLOCCO CHIUSURA ACCIDENTALE: Si chiude solo coi pulsanti espliciti
            title = {
                Text(stringResource(R.string.titolo_impostazioni_dado), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.desc_impostazioni_dado),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(stringResource(R.string.label_scegli_formato_dado), style = MaterialTheme.typography.bodyMedium)

                    val diceOptions = listOf(6, 10, 20, 100)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        diceOptions.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { sides ->
                                    val isSelected = pendingDiceSides == sides && customDiceInput.isEmpty()
                                    AnimatedSelectableChip(
                                        text = stringResource(R.string.label_dado, sides),
                                        isSelected = isSelected,
                                        onClick = {
                                            pendingDiceSides = sides
                                            customDiceInput = ""
                                        },
                                        height = 44.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = stringResource(R.string.label_inserimento_manuale),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = customDiceInput,
                            onValueChange = { if (it.all { char -> char.isDigit() }) customDiceInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.hint_es_facce_dado)) },
                            label = { Text(stringResource(R.string.hint_n_facce)) },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.btn_annulla), maxLines = 1)
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            val manualSides = customDiceInput.toIntOrNull()
                            val finalSides = if (manualSides != null && manualSides > 0) manualSides else pendingDiceSides

                            onSidesChanged(finalSides)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.btn_applica))
                    }
                }
            },
            dismissButton = null
        )
    }
}

/**
 * ====================================================================
 * COMPONENTE DADO FISICO MATERIAL YOU 3D (Tumbling Animation & Pips)
 * ====================================================================
 * 🧠 SPIEGAZIONE TEORICA E TECNICA DETTAGLIATA:
 * Questo componente ricrea la fisica tridimensionale di un dado reale.
 * Quando l'utente preme "Rilancia" (incrementando `rollCount`):
 * 1. `graphicsLayer`: Applica trasformazioni 3D sull'asse Z (rotazione) e sull'asse X (ribaltamento),
 *    con una prospettiva di profondità `cameraDistance`.
 * 2. `Animatable`: Gestisce l'accelerazione e il rimbalzo con la curva elastica `spring()`.
 * 3. Effetto "Slot Machine": Durante i ~600ms di ruzzolamento, una Coroutine fa scorrere numeri casuali
 *    ogni 50ms accompagnati da micro-vibrazioni tattili (`Haptic Ticks`).
 * 4. 🛡️ SISTEMA ANTI-INCLINAZIONE (Anti-Tilt Safety Net):
 *    I target di rotazione sono calibrati come multipli interi esatti di 360° (`rollCount * 720f` e `rollCount * 360f`).
 *    Nel blocco `finally`, anche se l'utente clicca freneticamente "Rilancia", gli angoli vengono forzati
 *    a riallinearsi in modo perfettamente piatto rispetto alla vista dell'utente (`angle % 360 == 0`),
 *    risolvendo definitivamente qualsiasi bug di inclinazione o capovolgimento.
 */
@Composable
fun MaterialYouDice3D(
    result: Int,                  // Il valore reale estratto dal generatore di numeri casuali
    diceSides: Int,               // Il numero totale di facce del dado (D6, D10, D20, D100)
    rollCount: Int,               // Contatore progressivo che incrementa ad ogni click per forzare il re-trigger dell'animazione
    modifier: Modifier = Modifier // Modificatore opzionale per personalizzare margini o posizionamento esterno
) {
    // Accediamo al motore di vibrazione aptica del dispositivo
    val haptic = LocalHapticFeedback.current

    // 🧠 ANIMATABLE COMPOSE: Creo tre contenitori per gestire le variabili animate ad alta frequenza (60/120 FPS)
    val rotationZ = remember { Animatable(0f) } // Gestisce la rotazione oraria/antioraria sul piano 2D dello schermo
    val rotationX = remember { Animatable(0f) } // Gestisce il ribaltamento 3D in avanti/indietro lungo l'asse X
    val scale = remember { Animatable(1f) }     // Gestisce lo schiacciamento e l'effetto rimbalzo all'impatto sul tavolo

    // Stato reattivo per memorizzare il numero temporaneo mostrato a schermo durante il rotolamento
    var displayedNumber by remember { mutableIntStateOf(result) }

    // 🧠 LAUNCHED EFFECT: Si attiva ad ogni cambiamento del parametro 'rollCount'
    LaunchedEffect(rollCount) {
        // Eseguiamo la sequenza di lancio solo se rollCount è maggiore di 0 (ovvero al primo o ai successivi lanci)
        if (rollCount > 0) {
            try {
                // 1. Calcolo dei target di rotazione come multipli interi esatti di 360°
                // In questo modo, qualsiasi punto di arrivo finale è matematicamente equivalente a 0° (vista perfettamente piatta)
                val targetZ = rollCount * 720f // 2 rotazioni complete a 360° sull'asse Z
                val targetX = rollCount * 360f // 1 ribaltamento completo a 360° sull'asse X

                // 2. Avvio concorrente in parallelo delle tre animazioni (Rotazione Z, Rotazione X e Scale)
                launch {
                    rotationZ.animateTo(
                        targetValue = targetZ,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing) // Accelerazione parte veloce e frena dolce
                    )
                }
                launch {
                    rotationX.animateTo(
                        targetValue = targetX,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    // Sequenza di schiacciamento e rimbalzo elastico all'impatto col tavolo (Impact Bounce)
                    scale.animateTo(1.18f, tween(180, easing = FastOutSlowInEasing)) // Si espande leggermente nella fase iniziale
                    scale.animateTo(0.88f, tween(150, easing = LinearEasing))            // Si schiaccia all'impatto
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) // Molla elastica
                }

                // 3. Ciclo di Flicker rapido dei numeri casuali ("Effetto Slot Machine")
                val startTime = System.currentTimeMillis() // Salva il tempo di inizio in millisecondi
                val duration = 600L                        // Durata complessiva del ruzzolamento (600 ms)
                while (System.currentTimeMillis() - startTime < duration) {
                    displayedNumber = (1..diceSides).random()            // Estrae un numero casuale temporaneo tra 1 e N facce
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Micro-vibrazione al cambio di cifra
                    delay(50L)                                           // Pausa di 50ms tra una cifra e l'altra
                }

                // 4. Assestamento finale sul valore reale ed emissione della vibrazione di impatto finale
                displayedNumber = result
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            } finally {
                // 🛡️ CLAUSOLA FINALLY (Anti-Tilt Safety Net):
                // Questo blocco viene eseguito SEMPRE, sia alla conclusione naturale che in caso di cancellazione prematura
                // (es. quando l'utente clicca freneticamente "Rilancia" interrompendo la Coroutine precedente).
                // Garanzia Matematica: Riportiamo istantaneamente la rotazione ai multipli esatti di 360° (targetZ e targetX)
                // e la scala a 1.0f, impedendo al dado di rimanere storto, ruotato o inclinato sullo schermo!
                val finalZ = rollCount * 720f
                val finalX = rollCount * 360f
                rotationZ.snapTo(finalZ)
                rotationX.snapTo(finalX)
                scale.snapTo(1f)
                displayedNumber = result
            }
        } else {
            // Se rollCount è 0 (stato iniziale), mostra direttamente il risultato senza animazione
            displayedNumber = result
        }
    }

    // 🎨 FACCIA DEL DADO IN STILE MATERIAL YOU 3D (Squircle con Elevation e Bordo)
    Surface(
        modifier = modifier
            .size(115.dp) // Dimensione quadrata del dado (115 x 115 dp)
            .graphicsLayer {
                // Assegniamo le rotazioni e gli attributi di scala correnti calcolati dagli Animatable
                this.rotationZ = rotationZ.value
                this.rotationX = rotationX.value
                this.scaleX = scale.value
                this.scaleY = scale.value
                // Prospettiva 3D: 'cameraDistance' simula la distanza della telecamera virtuale in pixel di densità
                this.cameraDistance = 14f * density
            },
        shape = RoundedCornerShape(28.dp), // Forma a "Squircle" caratteristica del design Material 3
        color = MaterialTheme.colorScheme.primaryContainer, // Colore del contenitore primario
        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary), // Bordo marcato del colore primario
        shadowElevation = 8.dp, // Ombra reale proiettata sullo sfondo del dialogo
        tonalElevation = 6.dp  // Tinta del colore primario applicata allo sfondo (Material 3)
    ) {
        Box(
            contentAlignment = Alignment.Center, // Centra perfettamente il contenuto interno (testo e pallini)
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Se si gioca con il dado standard a 6 facce (D6), disegniamo i pallini tradizionali sulla faccia
            if (diceSides == 6 && displayedNumber in 1..6) {
                DicePipsOverlay(
                    number = displayedNumber,
                    pipColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
                )
            }

            // Testo del numero principale in primo piano
            Text(
                text = "$displayedNumber",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if ("$displayedNumber".length > 2) 32.sp else 52.sp // Se il numero ha 3+ cifre scala il font
                ),
                fontWeight = FontWeight.Black, // Font ultra-bold per massima leggibilità
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * COMPONENTE PER I PALLINI TRADIZIONALI DEL DADO (PIPS D6)
 * 🧠 SPIEGAZIONE TECNICA:
 * Questo composable secondario calcola e posiziona nello spazio 2D del Box i pallini bianchi/scuri
 * del dado tradizionale a 6 facce (D6) in base al valore attualmente visualizzato (1..6).
 */
@Composable
private fun DicePipsOverlay(
    number: Int,     // Il valore del numero corrente (1..6)
    pipColor: Color  // Il colore da applicare ai pallini
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val dotSize = 12.dp // Dimensione di ogni singolo pallino
        val margin = 10.dp  // Margine di distanza dagli angoli della faccia del dado

        // Calcolo logico booleano per attivare o disattivare ogni singola posizione dei pallini
        val showTopLeft = number in listOf(2, 3, 4, 5, 6)
        val showTopRight = number in listOf(4, 5, 6)
        val showCenter = number in listOf(1, 3, 5)
        val showMiddleLeft = number == 6
        val showMiddleRight = number == 6
        val showBottomLeft = number in listOf(4, 5, 6)
        val showBottomRight = number in listOf(2, 3, 4, 5, 6)

        // Rendering condizionale dei pallini con allineamento preciso (Alignment)
        if (showTopLeft) Box(modifier = Modifier.align(Alignment.TopStart).padding(margin).size(dotSize).clip(RoundedCornerShape(6.dp)).background(pipColor))
        if (showTopRight) Box(modifier = Modifier.align(Alignment.TopEnd).padding(margin).size(dotSize).clip(RoundedCornerShape(6.dp)).background(pipColor))
        if (showMiddleLeft) Box(modifier = Modifier.align(Alignment.CenterStart).padding(margin).size(dotSize).clip(RoundedCornerShape(6.dp)).background(pipColor))
        if (showCenter) Box(modifier = Modifier.align(Alignment.Center).size(dotSize).clip(RoundedCornerShape(6.dp)).background(pipColor))
        if (showMiddleRight) Box(modifier = Modifier.align(Alignment.CenterEnd).padding(margin).size(dotSize).clip(RoundedCornerShape(6.dp)).background(pipColor))
        if (showBottomLeft) Box(modifier = Modifier.align(Alignment.BottomStart).padding(margin).size(dotSize).clip(RoundedCornerShape(6.dp)).background(pipColor))
        if (showBottomRight) Box(modifier = Modifier.align(Alignment.BottomEnd).padding(margin).size(dotSize).clip(RoundedCornerShape(6.dp)).background(pipColor))
    }
}

/**
 * ====================================================================
 * COMPONENTE: DiceRollDialog
 * ====================================================================
 * Mostra l'animazione del lancio del dado con fisica 3D in stile Material You.
 * Utilizza una chiave univoca per forzare l'animazione ad ogni click di rilancio.
 * ====================================================================
 */
@Composable
fun DiceRollDialog(
    result: Int,               // Il risultato intero estratto del lancio
    diceSides: Int,            // Il numero totale di facce del dado (D6, D10, D20, D100)
    rollCount: Int,            // Contatore incrementale dei lanci per innescare le animazioni
    onRollAgain: () -> Unit,   // Callback per richiedere un nuovo lancio
    onDismiss: () -> Unit,     // Callback per chiudere il dialogo modale
    onOpenSettings: () -> Unit // Callback per aprire le impostazioni del dado
) {
    // Otteniamo il riferimento al motore di vibrazione aptica
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss, // Esegue onDismiss quando si clicca all'esterno o si preme indietro
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Titolo principale del dialogo ("Lancio del dado")
                AutoResizedText(
                    text = stringResource(R.string.titolo_lancio_dado),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Pillola in alto a destra per mostrare il tipo di dado corrente (es. "D6") e aprire le impostazioni
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onOpenSettings()
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.desc_gestisci_rapidi_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        AutoResizedText(
                            text = "D$diceSides",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.widthIn(max = 50.dp),
                        )
                    }
                }
            }
        },
        text = {
            // Contenitore centrale che ospita il dado fisico 3D animato
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                MaterialYouDice3D(
                    result = result,
                    diceSides = diceSides,
                    rollCount = rollCount
                )
            }
        },
        confirmButton = {
            // Riga orizzontale contenente i pulsanti d'azione "CHIUDI" e "RILANCIA"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Pulsante Chiudi (Outlined)
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(55.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.btn_chiudi), fontWeight = FontWeight.Bold)
                }

                // Pulsante Rilancia (Pieno in Primary Container)
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(55.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRollAgain()
                    }
                ) {
                    AutoResizedText(stringResource(R.string.btn_rilancia), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null
    )
}
