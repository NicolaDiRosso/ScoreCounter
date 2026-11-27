package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.delay

/**
 * ====================================================================
 * COMPONENTE: ScoreEditDialog (Modale Modifica Punteggio Manuale)
 * ====================================================================
 * Dialog adattivo per la modifica manuale del punteggio con supporto alle
 * espressioni aritmetiche a catena (es. 45+15-5), anteprima live e tastierino
 * numerico ottimizzato.
 *
 * 🧠 RESPONSIVE ROUTING:
 * - Smartphone in orizzontale (isCompactLandscape): adotta un layout orizzontale
 *   a 3 fasce (intestazione compatta, area input+operatori+anteprima affiancati
 *   su una riga, pulsanti Annulla/Salva a tutta larghezza) con scroll attivo.
 * - Smartphone verticale e Tablet (Portrait & Landscape): adotta l'AlertDialog
 *   standard Material 3, dove lo spazio verticale è ampiamente sufficiente.
 */
@Composable
fun ScoreEditDialog(
    player: Player,
    viewModel: MatchViewModel,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // =========================================================================================
    // MODALE INSERIMENTO MANUALE: GESTIONE AVANZATA DEL FOCUS, TESTO ED ESPRESSIONI A CATENA
    // =========================================================================================
    // 1. STATE MANAGEMENT (Gestione dello Stato e del Cursore)
    // Usiamo TextFieldValue invece di String. Una String contiene solo i caratteri.
    // TextFieldValue è una data class complessa che memorizza:
    // - text: La stringa attuale (es. "45" oppure "45+15-5").
    // - selection: L'oggetto TextRange che definisce dove si trova il cursore o la selezione evidenziata.
    // - composition: Usato per le tastiere predittive.
    var scoreInput by remember {
        // dichiariamo la variabile immutabile initialText con il punteggio attuale convertito in stringa
        val initialText = player.score.toString()
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                // TextRange mappa un intervallo. Passando un solo valore (la lunghezza della stringa),
                // diciamo al motore di rendering del testo di piazzare il 'Caret' (cursore) alla fine.
                selection = TextRange(initialText.length)
            )
        )
    }

    // 🧠 CALCOLO IN TEMPO REALE DELL'ESPRESSIONE A CATENA:
    // Valutiamo live la stringa digitata (es. "45+15+20-5" -> 75).
    // Se il campo è vuoto o contiene solo un segno isolato ("+" o "-"), restituisce null.
    val evaluatedScore = remember(scoreInput.text) {
        evaluateChainedExpression(scoreInput.text)
    }

    // Calcoliamo anche la differenza rispetto al punteggio iniziale del giocatore per mostrarla nell'anteprima
    val currentPlayerScore = player.score
    val previewDelta = evaluatedScore?.let { it - currentPlayerScore }

    // Stringa formattata per la pillola con anteprima live del punteggio totale e dello scarto (delta)
    val previewText = if (evaluatedScore != null) {
        val deltaSuffix = when {
            previewDelta == null || previewDelta == 0 -> ""
            previewDelta > 0 -> " (+$previewDelta)"
            else -> " ($previewDelta)"
        }
        "= $evaluatedScore$deltaSuffix"
    } else {
        "= —"
    }

    // 2. FOCUS TREE (Albero del Focus)
    // FocusRequester è un proxy che ci permette di inviare comandi all'albero del focus di Compose.
    // Lo agganceremo al Modifier dell'OutlinedTextField più in basso.
    val focusRequester = remember { FocusRequester() }

    // 3. SIDE EFFECTS (Effetti Collaterali)
    // LaunchedEffect entra nel ciclo di vita della composizione. La chiave 'Unit' significa
    // che la coroutine al suo interno verrà lanciata una sola volta (al momento dell'attacco del nodo).
    LaunchedEffect(Unit) {
        // Sospendiamo la coroutine per 100 millisecondi.
        // Motivo tecnico: L'AlertDialog/Dialog ha un'animazione di entrata (Fade-In/Scale-In).
        // Se richiediamo il focus prima che il layout node sia stato completato e misurato dal sistema
        // (Window Manager), la richiesta viene scartata (Ignored). Il delay garantisce che il nodo sia pronto.
        delay(100)
        focusRequester.requestFocus() // Spinge programmaticamente l'evento di focus sul nodo collegato.
    }

    // 🧠 FUNZIONE DI SUPPORTO PER I TASTI RAPIDI (+ e -):
    // Inserisce l'operatore nella posizione corrente del cursore, evitando doppi operatori consecutivi
    // (es. se prima del cursore c'è già un '+' e premi '-', lo sostituisce con '-').
    val insertOperator: (Char) -> Unit = { op ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val currentText = scoreInput.text
        val start = scoreInput.selection.min.coerceIn(0, currentText.length)
        val end = scoreInput.selection.max.coerceIn(0, currentText.length)

        val beforeCursor = currentText.substring(0, start)
        val afterCursor = currentText.substring(end)

        // Se subito prima del cursore c'è già un operatore (+ o -), lo rimpiazziamo invece di affiancarlo
        val cleanedBefore = if (beforeCursor.lastOrNull() == '+' || beforeCursor.lastOrNull() == '-') {
            beforeCursor.dropLast(1)
        } else {
            beforeCursor
        }

        val updatedText = normalizeExpressionInput(cleanedBefore + op + afterCursor)
        val newCursorPos = (cleanedBefore.length + 1).coerceAtMost(updatedText.length)

        scoreInput = TextFieldValue(
            text = updatedText,
            selection = TextRange(newCursorPos)
        )
        focusRequester.requestFocus()
    }

    val onScoreInputChange: (TextFieldValue) -> Unit = { newValue ->
        // Sanitizziamo e normalizziamo l'espressione (accetta cifre, '+' e '-')
        val sanitizedText = normalizeExpressionInput(newValue.text)
        val adjustedCursor = newValue.selection.end.coerceIn(0, sanitizedText.length)
        scoreInput = TextFieldValue(
            text = sanitizedText,
            selection = TextRange(adjustedCursor)
        )
    }

    val onClearInput: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scoreInput = TextFieldValue(text = "", selection = TextRange(0))
        focusRequester.requestFocus()
    }

    val applyChanges: () -> Unit = {
        if (evaluatedScore != null) {
            // Calcolo del delta per non distruggere le statistiche/combo basate sull'aggiunta.
            val diff = evaluatedScore - player.score
            viewModel.updatePlayerScore(player, diff)

            // Azzera lo stato, innescando la recomposition che distruggerà il Dialog.
            onDismiss()
        }
    }

    // Rilevamento orientamento e tipologia dispositivo:
    // Sui Tablet (smallestScreenWidthDp >= 600) usiamo il dialog standard anche in landscape
    // per evitare un dialog eccessivamente largo con spazi vuoti.
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isCompactLandscape = isLandscape && !isTablet

    if (isCompactLandscape) {
        ScoreEditDialogLandscape(
            playerName = player.name,
            scoreInput = scoreInput,
            evaluatedScore = evaluatedScore,
            previewText = previewText,
            focusRequester = focusRequester,
            haptic = haptic,
            onScoreInputChange = onScoreInputChange,
            onClearInput = onClearInput,
            onInsertOperator = insertOperator,
            onApplyChanges = applyChanges,
            onDismiss = onDismiss
        )
    } else {
        ScoreEditDialogPortrait(
            playerName = player.name,
            scoreInput = scoreInput,
            evaluatedScore = evaluatedScore,
            previewText = previewText,
            focusRequester = focusRequester,
            haptic = haptic,
            onScoreInputChange = onScoreInputChange,
            onClearInput = onClearInput,
            onInsertOperator = insertOperator,
            onApplyChanges = applyChanges,
            onDismiss = onDismiss
        )
    }
}

/**
 * ====================================================================
 * 4A. LAYOUT ORIZZONTALE A 3 FASCE (Smartphone Landscape Dialog)
 * ====================================================================
 * 🧠 UX & UI DESIGN:
 * Evita la divisione a 2 colonne che comprime i pulsanti Annulla/Salva a sinistra.
 * Sfrutta invece la larghezza orizzontale affiancando su una singola riga:
 * [ Campo di testo + X ]  [ - ]  [ + ]  [ = Anteprima Live ]
 * lasciando l'intestazione compatta in alto e i tasti Annulla/Salva simmetrici in basso.
 */
@Composable
private fun ScoreEditDialogLandscape(
    playerName: String,
    scoreInput: TextFieldValue,
    evaluatedScore: Int?,
    previewText: String,
    focusRequester: FocusRequester,
    haptic: HapticFeedback,
    onScoreInputChange: (TextFieldValue) -> Unit,
    onClearInput: () -> Unit,
    onInsertOperator: (Char) -> Unit,
    onApplyChanges: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        // Callback invocata dal sistema se l'utente tocca lo 'Scrim' (sfondo oscurato) o preme il tasto Back.
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp), // Angoli arrotondati dell'intero popup
            // 'containerColor' definisce il colore del "corpo" del dialog (coerente con AlertDialog).
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            // verticalScroll permette di scorrere se la tastiera copre parzialmente lo schermo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FASCIA 1: INTESTAZIONE COMPATTA (Badge icona + Titolo + Sottotitolo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        AutoResizedText(
                            text = stringResource(R.string.titolo_modifica_punteggio), // 🌍 I18N
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // ====================================================================
                        // 🧠 LEZIONE TEORICA I18N: Stringhe Dinamiche nel Testo Descrittivo
                        // ====================================================================
                        AutoResizedText(
                            text = stringResource(R.string.msg_inserisci_nuovo_punteggio, playerName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // FASCIA 2: AREA DI CALCOLO UNIFICATA (Campo di testo + Tasti - / + + Anteprima Live)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 5. COMPONENTE UI: CAMPO TESTUALE PER ESPRESSIONI A CATENA
                    OutlinedTextField(
                        value = scoreInput, // Associa lo stato (TextFieldValue) alla UI
                        onValueChange = onScoreInputChange,
                        // Binding del FocusRequester al modificatore di questo specifico componente.
                        modifier = Modifier
                            .weight(1.35f)
                            .focusRequester(focusRequester),
                        // Stondatura coerente con i campi di testo (Module style)
                        shape = RoundedCornerShape(12.dp),
                        // singleLine disabilita il comportamento "multiline" (non crea a capo "\n")
                        // ed è propedeutico affinché imeAction venga rispettata dalla tastiera (IME).
                        singleLine = true,
                        // Pulsante rapido di pulizia coerente con la barra di ricerca della Home
                        trailingIcon = {
                            if (scoreInput.text.isNotEmpty()) {
                                ClearInputButton(
                                    onClick = onClearInput,
                                    contentDescription = stringResource(R.string.btn_cancella),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        },
                        // Configurazione dell'Input Method Editor (IME) del sistema Android.
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number, // Forza il layout numerico dell'hardware/software.
                            imeAction = ImeAction.Done          // Setta l'action button della tastiera su "Fatto/Spunta".
                        ),
                        // Intercettazione dell'evento emesso dalla tastiera (quando l'utente preme la Spunta).
                        keyboardActions = KeyboardActions(
                            onDone = { onApplyChanges() }
                        )
                    )

                    // Pulsante rapido '-' a sinistra (asse numerico coerente con le card giocatori)
                    FilledTonalButton(
                        onClick = { onInsertOperator('-') },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pulsante rapido '+' a destra, accanto all'anteprima del risultato
                    FilledTonalButton(
                        onClick = { onInsertOperator('+') },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pillola con anteprima live del punteggio totale e dello scarto (delta)
                    Surface(
                        modifier = Modifier
                            .weight(0.85f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            AutoResizedText(
                                text = previewText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (evaluatedScore != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }

                // FASCIA 3: PULSANTI ANNULLA / SALVA A TUTTA LARGHEZZA (Pattern Material 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // OutlinedButton è usato per azioni secondarie/distruttive deboli (Cancel/Annulla).
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.btn_annulla),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        ) // 🌍 I18N: Riutilizzo chiave Annulla
                    }

                    // Filled Button è usato per l'azione primaria e affermativa (Confirm/Salva).
                    Button(
                        enabled = evaluatedScore != null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onApplyChanges()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(stringResource(R.string.btn_salva), fontWeight = FontWeight.Bold) // 🌍 I18N: Riutilizzo chiave Salva
                    }
                }
            }
        }
    }
}

/**
 * ====================================================================
 * 4B. LAYOUT STANDARD (Smartphone Portrait & Tablet - AlertDialog M3)
 * ====================================================================
 */
@Composable
private fun ScoreEditDialogPortrait(
    playerName: String,
    scoreInput: TextFieldValue,
    evaluatedScore: Int?,
    previewText: String,
    focusRequester: FocusRequester,
    haptic: HapticFeedback,
    onScoreInputChange: (TextFieldValue) -> Unit,
    onClearInput: () -> Unit,
    onInsertOperator: (Char) -> Unit,
    onApplyChanges: () -> Unit,
    onDismiss: () -> Unit
) {
    // 4. COMPONENTE UI: ALERT DIALOG (Material 3)
    AlertDialog(
        // Callback invocata dal sistema se l'utente tocca lo 'Scrim' (sfondo oscurato) o preme il tasto Back.
        onDismissRequest = onDismiss,
        // icon posiziona un'immagine in alto al centro del popup.
        icon = {
            Icon(
                imageVector = Icons.Filled.Calculate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        // title è l'intestazione in grassetto.
        title = {
            Text(
                text = stringResource(R.string.titolo_modifica_punteggio), // 🌍 I18N
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        // text è il corpo del messaggio.
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // ====================================================================
                // 🧠 LEZIONE TEORICA I18N: Stringhe Dinamiche nel Testo Descrittivo
                // ====================================================================
                // Anche qui, nel testo discorsivo, usiamo il placeholder '%s'.
                // Nel file XML: <string name="msg_inserisci_nuovo_punteggio">Inserisci il nuovo punteggio totale per %s:</string>
                // Passiamo il nome del giocatore in modo pulito alla funzione stringResource.
                Text(
                    text = stringResource(R.string.msg_inserisci_nuovo_punteggio, playerName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 5. COMPONENTE UI: CAMPO TESTUALE PER ESPRESSIONI A CATENA
                OutlinedTextField(
                    value = scoreInput, // Associa lo stato (TextFieldValue) alla UI
                    onValueChange = onScoreInputChange,
                    // Binding del FocusRequester al modificatore di questo specifico componente.
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    // Stondatura coerente con i campi di testo (Module style)
                    shape = RoundedCornerShape(12.dp),
                    // singleLine disabilita il comportamento "multiline" (non crea a capo "\n")
                    // ed è propedeutico affinché imeAction venga rispettata dalla tastiera (IME).
                    singleLine = true,
                    // Pulsante rapido di pulizia coerente con la barra di ricerca della Home
                    trailingIcon = {
                        if (scoreInput.text.isNotEmpty()) {
                            ClearInputButton(
                                onClick = onClearInput,
                                contentDescription = stringResource(R.string.btn_cancella),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    },
                    // Configurazione dell'Input Method Editor (IME) del sistema Android.
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, // Forza il layout numerico dell'hardware/software.
                        imeAction = ImeAction.Done          // Setta l'action button della tastiera su "Fatto/Spunta".
                    ),
                    // Intercettazione dell'evento emesso dalla tastiera (quando l'utente preme la Spunta).
                    keyboardActions = KeyboardActions(
                        onDone = { onApplyChanges() }
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. BARRA OPERATORI RAPIDI (- / +) E ANTEPRIMA RISULTATO IN TEMPO REALE
                // 🧠 UX DESIGN: Poiché molte tastiere numeriche su Android nascondono il tasto '+',
                // affianchiamo due pulsanti tattili '-' e '+' (ordinati lungo l'asse numerico)
                // alla pillola con il risultato calcolato in tempo reale.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsante rapido '-' a sinistra (asse numerico coerente con le card giocatori)
                    FilledTonalButton(
                        onClick = { onInsertOperator('-') },
                        modifier = Modifier
                            .width(52.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pulsante rapido '+' a destra, accanto all'anteprima del risultato
                    FilledTonalButton(
                        onClick = { onInsertOperator('+') },
                        modifier = Modifier
                            .width(52.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pillola con anteprima live del punteggio totale e dello scarto (delta)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            AutoResizedText(
                                text = previewText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (evaluatedScore != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Layout orizzontale per i pulsanti (Pattern Material 3: Tasti affiancati a peso uguale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // OutlinedButton è usato per azioni secondarie/distruttive deboli (Cancel/Annulla).
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    AutoResizedText(
                        text = stringResource(R.string.btn_annulla),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ) // 🌍 I18N: Riutilizzo chiave Annulla
                }

                // Filled Button è usato per l'azione primaria e affermativa (Confirm/Salva).
                Button(
                    enabled = evaluatedScore != null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onApplyChanges()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(stringResource(R.string.btn_salva), fontWeight = FontWeight.Bold) // 🌍 I18N: Riutilizzo chiave Salva
                }
            }
        },
        dismissButton = null, // Spegniamo il tasto di default perché abbiamo integrato "Annulla" nella Row
        shape = RoundedCornerShape(24.dp), // Angoli arrotondati dell'intero popup
        containerColor = MaterialTheme.colorScheme.surfaceVariant // 'containerColor' definisce il colore del "corpo" del dialog.
    )
}

/**
 * ====================================================================
 * HELPER PARSER ESPRESSIONI A CATENA (Addizioni e Sottrazioni)
 * ====================================================================
 * Filtra e normalizza l'input dell'utente ammettendo solo cifre e segni (+ / -),
 * evitando due operatori consecutivi (es. "45+-" viene normalizzato in "45-").
 */
private fun normalizeExpressionInput(raw: String): String {
    val result = StringBuilder()
    for (ch in raw) {
        if (ch.isDigit()) {
            result.append(ch)
        } else if (ch == '+' || ch == '-') {
            if (result.isNotEmpty() && (result.last() == '+' || result.last() == '-')) {
                // Sostituisce l'operatore precedente anziché accodarlo
                result.setCharAt(result.length - 1, ch)
            } else {
                result.append(ch)
            }
        }
    }
    return result.toString()
}

/**
 * Valuta da sinistra a destra un'espressione composta da somme e sottrazioni (es. "45+15+20-5" -> 75).
 * Se l'utente ha appena digitato un operatore finale in sospeso (es. "45+"), calcola comunque
 * il risultato parziale valido ("45") per mantenere fluida l'anteprima live.
 * Restituisce null se non è presente alcuna cifra valida.
 */
private fun evaluateChainedExpression(input: String): Int? {
    val cleaned = input.trim().trimEnd('+', '-')
    if (cleaned.isEmpty()) return null

    var total = 0L
    var currentSign = 1
    var currentNumber = StringBuilder()
    var hasAnyDigit = false

    for (i in cleaned.indices) {
        val ch = cleaned[i]
        when {
            ch.isDigit() -> {
                currentNumber.append(ch)
                hasAnyDigit = true
            }
            ch == '+' || ch == '-' -> {
                if (currentNumber.isNotEmpty()) {
                    val value = currentNumber.toString().toLongOrNull() ?: return null
                    total += currentSign * value
                    currentNumber = StringBuilder()
                }
                currentSign = if (ch == '-') -1 else 1
            }
        }
    }

    if (currentNumber.isNotEmpty()) {
        val value = currentNumber.toString().toLongOrNull() ?: return null
        total += currentSign * value
    }

    return if (hasAnyDigit) total.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt() else null
}