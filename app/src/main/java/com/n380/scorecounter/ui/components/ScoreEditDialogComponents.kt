package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
 * Dialog adattivo (Landscape e Portrait) per la modifica manuale del punteggio
 * con supporto alle espressioni aritmetiche a catena (es. 45+15-5), anteprima live
 * e tastierino numerico ottimizzato. In landscape adotta un layout a 2 colonne
 * per impedire qualsiasi sovrapposizione con la tastiera virtuale.
 */
@Composable
fun ScoreEditDialog(
    player: Player,
    viewModel: MatchViewModel,
    onDismiss: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ScoreEditDialogLandscape(player = player, viewModel = viewModel, onDismiss = onDismiss)
    } else {
        ScoreEditDialogPortrait(player = player, viewModel = viewModel, onDismiss = onDismiss)
    }
}

/**
 * LAYOUT ORIZZONTALE (Landscape 2-Column Split Dialog)
 */
@Composable
fun ScoreEditDialogLandscape(
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

    // 2. FOCUS TREE (Albero del Focus)
    // FocusRequester è un proxy che ci permette di inviare comandi all'albero del focus di Compose.
    // Lo agganceremo al Modifier dell'OutlinedTextField più in basso.
    val focusRequester = remember { FocusRequester() }

    // 3. SIDE EFFECTS (Effetti Collaterali)
    // LaunchedEffect entra nel ciclo di vita della composizione. La chiave 'Unit' significa
    // che la coroutine al suo interno verrà lanciata una sola volta (al momento dell'attacco del nodo).
    LaunchedEffect(Unit) {
        // Sospendiamo la coroutine per 100 millisecondi.
        // Motivo tecnico: L'AlertDialog ha un'animazione di entrata (Fade-In/Scale-In).
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

    val applyChanges: () -> Unit = {
        if (evaluatedScore != null) {
            // Calcolo del delta per non distruggere le statistiche/combo basate sull'aggiunta.
            val diff = evaluatedScore - player.score
            viewModel.updatePlayerScore(player, diff)

            // Azzera lo stato, innescando la recomposition che distruggerà il Dialog.
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            // 'containerColor' definisce il colore del "corpo" del dialog.
            // Si possono usare:
            // - MaterialTheme.colorScheme.surface (Classico)
            // - MaterialTheme.colorScheme.surfaceVariant (Leggermente più grigio/scuro)
            // - Color.White (Bianco puro, se si vuole massima luminosità)
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // COLONNA SINISTRA (42%): Titolo, Descrizione e Pulsanti Annulla/Salva
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            AutoResizedText(
                                text = stringResource(R.string.titolo_modifica_punteggio), // 🌍 I18N
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // ====================================================================
                        // 🧠 LEZIONE TEORICA I18N: Stringhe Dinamiche nel Testo Descrittivo
                        // ====================================================================
                        // Anche qui, nel testo discorsivo, usiamo il placeholder '%s'.
                        // Nel file XML: <string name="msg_inserisci_nuovo_punteggio">Inserisci il nuovo punteggio totale per %s:</string>
                        // Passiamo il nome del giocatore in modo pulito alla funzione stringResource.
                        Text(
                            text = stringResource(R.string.msg_inserisci_nuovo_punteggio, player.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Layout orizzontale per i pulsanti (Pattern Material 3: Tasti affiancati a peso uguale)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                applyChanges()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(stringResource(R.string.btn_salva), fontWeight = FontWeight.Bold) // 🌍 I18N: Riutilizzo chiave Salva
                        }
                    }
                }

                // COLONNA DESTRA (58%): Campo di testo, operatori rapidi e anteprima
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // 5. COMPONENTE UI: CAMPO TESTUALE PER ESPRESSIONI A CATENA
                    OutlinedTextField(
                        value = scoreInput, // Associa lo stato (TextFieldValue) alla UI
                        onValueChange = { newValue ->
                            // Sanitizziamo e normalizziamo l'espressione (accetta cifre, '+' e '-')
                            val sanitizedText = normalizeExpressionInput(newValue.text)
                            val adjustedCursor = newValue.selection.end.coerceIn(0, sanitizedText.length)
                            scoreInput = TextFieldValue(
                                text = sanitizedText,
                                selection = TextRange(adjustedCursor)
                            )
                        },
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
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scoreInput = TextFieldValue(text = "", selection = TextRange(0))
                                        focusRequester.requestFocus()
                                    },
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
                            onDone = { applyChanges() }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. BARRA OPERATORI RAPIDI (+ / -) E ANTEPRIMA RISULTATO IN TEMPO REALE
                    // 🧠 UX DESIGN: Poiché molte tastiere numeriche su Android nascondono il tasto '+',
                    // affianchiamo due pulsanti tattili '+' e '-' alla pillola con il risultato live.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsante rapido '-' a sinistra (asse numerico coerente con le card giocatori)
                        FilledTonalButton(
                            onClick = { insertOperator('-') },
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
                            onClick = { insertOperator('+') },
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
            }
        }
    }
}

/**
 * LAYOUT VERTICALE (Portrait)
 */
@Composable
fun ScoreEditDialogPortrait(
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

    // 2. FOCUS TREE (Albero del Focus)
    // FocusRequester è un proxy che ci permette di inviare comandi all'albero del focus di Compose.
    // Lo agganceremo al Modifier dell'OutlinedTextField più in basso.
    val focusRequester = remember { FocusRequester() }

    // 3. SIDE EFFECTS (Effetti Collaterali)
    // LaunchedEffect entra nel ciclo di vita della composizione. La chiave 'Unit' significa
    // che la coroutine al suo interno verrà lanciata una sola volta (al momento dell'attacco del nodo).
    LaunchedEffect(Unit) {
        // Sospendiamo la coroutine per 100 millisecondi.
        // Motivo tecnico: L'AlertDialog ha un'animazione di entrata (Fade-In/Scale-In).
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

    val applyChanges: () -> Unit = {
        if (evaluatedScore != null) {
            // Calcolo del delta per non distruggere le statistiche/combo basate sull'aggiunta.
            val diff = evaluatedScore - player.score
            viewModel.updatePlayerScore(player, diff)

            // Azzera lo stato, innescando la recomposition che distruggerà l'AlertDialog.
            onDismiss()
        }
    }

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
            Column {
                // ====================================================================
                // 🧠 LEZIONE TEORICA I18N: Stringhe Dinamiche nel Testo Descrittivo
                // ====================================================================
                // Anche qui, nel testo discorsivo, usiamo il placeholder '%s'.
                // Nel file XML: <string name="msg_inserisci_nuovo_punteggio">Inserisci il nuovo punteggio totale per %s:</string>
                // Passiamo il nome del giocatore in modo pulito alla funzione stringResource.
                Text(
                    text = stringResource(R.string.msg_inserisci_nuovo_punteggio, player.name),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 5. COMPONENTE UI: CAMPO TESTUALE PER ESPRESSIONI A CATENA
                OutlinedTextField(
                    value = scoreInput, // Associa lo stato (TextFieldValue) alla UI
                    onValueChange = { newValue ->
                        // Sanitizziamo e normalizziamo l'espressione (accetta cifre, '+' e '-')
                        val sanitizedText = normalizeExpressionInput(newValue.text)
                        val adjustedCursor = newValue.selection.end.coerceIn(0, sanitizedText.length)
                        scoreInput = TextFieldValue(
                            text = sanitizedText,
                            selection = TextRange(adjustedCursor)
                        )
                    },
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
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scoreInput = TextFieldValue(text = "", selection = TextRange(0))
                                    focusRequester.requestFocus()
                                },
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
                        onDone = { applyChanges() }
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. BARRA OPERATORI RAPIDI (+ / -) E ANTEPRIMA RISULTATO IN TEMPO REALE
                // 🧠 UX DESIGN: Poiché molte tastiere numeriche su Android nascondono il tasto '+',
                // affianchiamo due pulsanti tattili '+' e '-' alla pillola con il risultato live.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsante rapido '-' a sinistra (asse numerico coerente con le card giocatori)
                    FilledTonalButton(
                        onClick = { insertOperator('-') },
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
                        onClick = { insertOperator('+') },
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
                    modifier = Modifier.weight(1f).height(48.dp),
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
                        applyChanges()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(stringResource(R.string.btn_salva), fontWeight = FontWeight.Bold) // 🌍 I18N: Riutilizzo chiave Salva
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

/**
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
