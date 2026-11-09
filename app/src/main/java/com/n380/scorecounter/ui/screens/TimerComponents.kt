package com.n380.scorecounter.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.n380.scorecounter.R
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.viewmodel.MatchViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * FUNZIONE D'AIUTO PER FORMATTARE IL TESTO GREZZO DEL TIMER IN FORMATO MM:SS
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Prende una stringa di cifre (es. "0030" o "0500"), ne assicura il padding a 4 cifre
 * e la separa con i due punti per la visualizzazione grafica ("00:30", "05:00").
 */
private fun formatRawTimeToMMSS(raw: String): String {
    val padded = raw.padStart(4, '0')
    val m = padded.substring(0, 2)
    val s = padded.substring(2, 4)
    return "$m:$s"
}

/**
 * ====================================================================
 * COMPONENTE: TimerSettingsDialog (Versione con 4 Preset Personalizzabili)
 * ====================================================================
 */
@Composable
fun TimerSettingsDialog(
    initialRawInput: String,
    onInputChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val vibratorManager = remember { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager }
    val vibrator = remember { vibratorManager.defaultVibrator }

    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableLongStateOf(0L) }
    var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }
    var rawInput by remember { mutableStateOf(initialRawInput) }

    // Lista dei 4 preset caricata reattivamente dal ViewModel
    val presets = viewModel.timerPresets

    // Stato per la gestione del popup di modifica del singolo preset
    var presetToEditIndex by remember { mutableStateOf<Int?>(null) }
    var editingRawValue by remember { mutableStateOf("") }

    LaunchedEffect(rawInput) {
        onInputChanged(rawInput)
    }

    var isTextFieldFocused by remember { mutableStateOf(false) }

    val stopAllAlarms = {
        vibrator.cancel()
        activeRingtone?.stop()
        activeRingtone = null
    }

    BackHandler {
        stopAllAlarms()
        onDismiss()
    }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning) {
            delay(1000L) // Attende esattamente 1 secondo (1000 millisecondi)
            if (!isRunning) return@LaunchedEffect // Se nel frattempo il timer è stato stoppato, interrompe l'esecuzione

            timeLeft -= 1 // Decrementa il tempo rimanente di un secondo

            // 🧠 GESTIONE CONTO ALLA ROVESCIA E VIBRAZIONE CRESCENTE (Crescendo Haptic Feedback):
            // Ottimizzato sugli ultimi 3 secondi (3, 2, 1) per mantenere intatta la suspense 
            // evitando al contempo qualsiasi affaticamento aptico nei turni ripetuti.
            // L'ampiezza (amplitude) cresce da un sussurro a una vibrazione decisa.
            if (timeLeft in 1..3 && activeRingtone == null) {
                // Calcolo dinamico dell'intensità (da 60 = sussurro aptico a 255 = massima intensità)
                val amplitude = when (timeLeft) {
                    3L -> 60  // Sussurro iniziale (delicato)
                    2L -> 140 // Avviso medio (crescente)
                    1L -> 255 // Impatto finale (massima potenza prima dello zero)
                    else -> 255
                }

                // Emettiamo la vibrazione scalata tramite VibrationEffect
                vibrator.vibrate(VibrationEffect.createOneShot(180L, amplitude))
            } else if (timeLeft <= 0 && activeRingtone == null) {
                // Quando il timer arriva a zero, attiva la sveglia continua e riproduce il suono di allarme
                triggerContinuousAlarm(vibrator)
                activeRingtone = riproduciSuonoTimer(context)
            }
        }
    }

    val displayColor by animateColorAsState(
        targetValue = when {
            !isRunning && !isPaused -> MaterialTheme.colorScheme.primary
            timeLeft <= 0 -> Color(0xFFE8514C)
            timeLeft <= 10 -> Color(0xFFEE9850)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 1000),
        label = "ColorAnimation"
    )

    // ====================================================================
    // DIALOGO MODALE DI MODIFICA DEL SINGOLO PRESET TIMER
    // ====================================================================
    if (presetToEditIndex != null) {
        // 🧠 FOCUS E TASTIERA AUTOMATICA (Architettura del Focus in Jetpack Compose):
        // 1. 'FocusRequester' funge da "bersaglio" programmabile per indirizzare il focus del sistema
        //    direttamente sul campo di testo non appena il componente viene montato a schermo.
        // 2. 'LocalSoftwareKeyboardController' fornisce l'interfaccia con l'Input Method Framework (IMF)
        //    di sistema di Android, consentendo di forzare l'innalzamento programmatico della tastiera.
        val focusRequesterPreset = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        // 🧠 SIDE EFFECT DIDATTICO (Gestione Ciclo di Vita della Vista):
        // Usiamo LaunchedEffect(Unit) per attivare una Coroutine esattamente nel momento in cui il dialogo
        // entra nella composizione. Il ritardo di 150ms è indispensabile perché l'AlertDialog ha un'animazione
        // di ingresso nativa (fade/scale); richiedere il focus prima che il nodo sia misurato e renderizzato
        // porterebbe il sistema a ignorare la richiesta (Dropping Focus Event).
        LaunchedEffect(Unit) {
            delay(150)
            focusRequesterPreset.requestFocus()
            keyboardController?.show()
        }

        AlertDialog(
            onDismissRequest = { presetToEditIndex = null },
            title = {
                // 🧠 AUTO-ADAPTIVE TEXT (AutoResizedText):
                // Utilizziamo il componente custom condiviso 'AutoResizedText' per evitare qualsiasi troncamento
                // del titolo del dialogo su schermi a risoluzione ridotta o con font di sistema ingranditi.
                AutoResizedText(
                    text = stringResource(R.string.titolo_modifica_preset),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val padded = editingRawValue.padStart(4, '0')
                    val formattedDisplay = "${padded.substring(0, 2)}:${padded.substring(2, 4)}"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = editingRawValue,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }
                                editingRawValue = digits.takeLast(4)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                // 🎯 Agganciamo il puntatore FocusRequester al campo per catturare il focus all'apertura
                                .focusRequester(focusRequesterPreset),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                presetToEditIndex?.let { index ->
                                    val sanitized = editingRawValue.filter { it.isDigit() }.padStart(4, '0').takeLast(4)
                                    viewModel.updateTimerPreset(index, sanitized)
                                }
                                presetToEditIndex = null
                            }),
                            textStyle = TextStyle(color = Color.Transparent),
                            cursorBrush = SolidColor(Color.Transparent),
                            decorationBox = {
                                Text(
                                    text = formattedDisplay,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                }
            },
            // 🧠 SIMMETRIA GEOMETRICA E CONTROLLO TOTALE DELLO SPAZIO:
            // Material 3 AlertDialog posiziona i pulsanti in basso a destra.
            // Per aggirare questo comportamento e renderli entrambi simmetrici al 50% di larghezza ciascuno,
            // iniettiamo una Row con 'fillMaxWidth()' all'interno di 'confirmButton' e impostiamo 'dismissButton = null'.
            // In questo modo i pulsanti sfruttano 'weight(1f)' per dividersi equamente l'intera ampiezza del dialogo.
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pulsante Secondario: ANNULLA (OutlinedButton con bordo)
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            presetToEditIndex = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.btn_annulla),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Pulsante Primario: SALVA (Filled Button)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            presetToEditIndex?.let { index ->
                                val sanitized = editingRawValue.filter { it.isDigit() }.padStart(4, '0').takeLast(4)
                                viewModel.updateTimerPreset(index, sanitized)
                            }
                            presetToEditIndex = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.btn_salva),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = null
        )
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // ====================================================================
        // ORIENTAMENTO ORIZZONTALE (2-Column Split Dialog per Timer)
        // ====================================================================
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.85f),
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
                    // COLONNA SINISTRA (48%): TITOLO, DISPLAY TEMPO & TASTI D'AZIONE
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.titolo_timer_sfida),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            // DISPLAY DIGITALE
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                    .border(
                                        border = if (isTextFieldFocused && !isRunning && !isPaused)
                                            BorderStroke(2.dp, displayColor)
                                        else
                                            BorderStroke(0.dp, Color.Transparent),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRunning || isPaused) {
                                    val totalSeconds = abs(timeLeft)
                                    val m = totalSeconds / 60
                                    val s = totalSeconds % 60
                                    val sign = if (timeLeft < 0) "-" else ""

                                    Text(
                                        text = "$sign%02d:%02d".format(m, s),
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Black,
                                            color = displayColor
                                        )
                                    )
                                } else {
                                    val paddedInput = rawInput.padStart(4, '0')
                                    val formattedDisplay = "${paddedInput.substring(0, 2)}:${paddedInput.substring(2, 4)}"

                                    BasicTextField(
                                        value = rawInput,
                                        onValueChange = { newValue ->
                                            val digits = newValue.filter { it.isDigit() }
                                            rawInput = digits.takeLast(4)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState -> isTextFieldFocused = focusState.isFocused },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        textStyle = TextStyle(color = Color.Transparent),
                                        cursorBrush = SolidColor(Color.Transparent),
                                        decorationBox = {
                                            Text(
                                                text = formattedDisplay,
                                                style = MaterialTheme.typography.displayMedium.copy(
                                                    fontSize = 42.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isTextFieldFocused) displayColor else displayColor.copy(alpha = 0.6f)
                                                ),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // TASTI AVVIA / PAUSA / FERMA / CHIUDI ANCORATI IN BASSO
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isRunning || isPaused) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimatedVisibility(
                                        visible = timeLeft > 0,
                                        enter = fadeIn(tween(300)) + expandHorizontally(tween(300)),
                                        exit = fadeOut(tween(300)) + shrinkHorizontally(tween(300)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                stopAllAlarms()
                                                if (isRunning) { isRunning = false; isPaused = true } else { isRunning = true; isPaused = false }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isRunning = false
                                            isPaused = false
                                            stopAllAlarms()
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Filled.Stop, null, modifier = Modifier.size(if (timeLeft > 0) 24.dp else 20.dp))
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        focusManager.clearFocus()
                                        val padded = rawInput.padStart(4, '0')
                                        val m = padded.substring(0, 2).toLongOrNull() ?: 0L
                                        val s = padded.substring(2, 4).toLongOrNull() ?: 0L
                                        val totalSeconds = (m * 60) + s

                                        if (totalSeconds > 0) {
                                            timeLeft = totalSeconds
                                            isRunning = true
                                            isPaused = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_avvia_timer), fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    stopAllAlarms()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Close, stringResource(R.string.desc_chiudi_icon), modifier = Modifier.padding(end = 6.dp).size(20.dp))
                                Text(stringResource(R.string.btn_chiudi), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // COLONNA DESTRA (52%): 4 PRESET RAPIDI EDITABILI IN GRIGLIA 2x2
                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.titolo_preset_rapidi),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 🧠 UX FIX: Column con weight(1f) per far allungare le 3 righe in altezza
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val rows = presets.chunked(2)
                            rows.forEachIndexed { rowIndex, rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEachIndexed { colIndex, rawPreset ->
                                        val actualIndex = rowIndex * 2 + colIndex
                                        val formattedText = formatRawTimeToMMSS(rawPreset)

                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                rawInput = rawPreset
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(), // 🌟 La card si allunga per riempire la riga!
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AutoResizedText(
                                                    text = formattedText,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )

                                                // TASTO MATITA IN ALTO A DESTRA PER MODIFICARE IL PRESET
                                                // size 28.dp per impostare la dimensione del pulsante di modifica
                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                        presetToEditIndex = actualIndex
                                                        editingRawValue = rawPreset
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(28.dp)
                                                        .padding(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = stringResource(R.string.desc_modifica_icon),
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                        // size 16.dp per impostare la dimensione dell'icona della matita
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ====================================================================
        // ORIENTAMENTO VERTICALE (Layout Standard Portrait per AlertDialog)
        // ====================================================================
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = stringResource(R.string.titolo_timer_sfida),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .border(
                                border = if (isTextFieldFocused && !isRunning && !isPaused)
                                    BorderStroke(2.dp, displayColor)
                                else
                                    BorderStroke(0.dp, Color.Transparent),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRunning || isPaused) {
                            val totalSeconds = abs(timeLeft)
                            val m = totalSeconds / 60
                            val s = totalSeconds % 60
                            val sign = if (timeLeft < 0) "-" else ""

                            Text(
                                text = "$sign%02d:%02d".format(m, s),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.Black,
                                    color = displayColor
                                )
                            )
                        } else {
                            val paddedInput = rawInput.padStart(4, '0')
                            val formattedDisplay = "${paddedInput.substring(0, 2)}:${paddedInput.substring(2, 4)}"

                            BasicTextField(
                                value = rawInput,
                                onValueChange = { newValue ->
                                    val digits = newValue.filter { it.isDigit() }
                                    rawInput = digits.takeLast(4)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState -> isTextFieldFocused = focusState.isFocused },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                textStyle = TextStyle(color = Color.Transparent),
                                cursorBrush = SolidColor(Color.Transparent),
                                decorationBox = {
                                    Text(
                                        text = formattedDisplay,
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 52.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isTextFieldFocused) displayColor else displayColor.copy(alpha = 0.6f)
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            )
                        }
                    }

                    if (!isRunning && !isPaused) {
                        // 🧠 GRIGLIA 2x2 DEI 4 PRESET TIMER EDITABILI (TUTTI VISIBILI SENZA CAROSELLO O SFUMATURE)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.titolo_preset_rapidi),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 2.dp)
                            )

                            val rows = presets.chunked(2)
                            rows.forEachIndexed { rowIndex, rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEachIndexed { colIndex, rawPreset ->
                                        val actualIndex = rowIndex * 2 + colIndex
                                        val formattedText = formatRawTimeToMMSS(rawPreset)

                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                rawInput = rawPreset
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AutoResizedText(
                                                    text = formattedText,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )

                                                // TASTO MATITA IN ALTO A DESTRA PER MODIFICARE IL PRESET
                                                // size 28.dp per impostare la dimensione del pulsante di modifica
                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                        presetToEditIndex = actualIndex
                                                        editingRawValue = rawPreset
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(28.dp)
                                                        .padding(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = stringResource(R.string.desc_modifica_icon),
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                        // size 16.dp per impostare la dimensione dell'icona della matita
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isRunning || isPaused) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnimatedVisibility(
                                visible = timeLeft > 0,
                                enter = fadeIn(tween(300)) + expandHorizontally(tween(300)),
                                exit = fadeOut(tween(300)) + shrinkHorizontally(tween(300)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        stopAllAlarms()

                                        if (isRunning) {
                                            isRunning = false
                                            isPaused = true
                                        } else {
                                            isRunning = true
                                            isPaused = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isRunning = false
                                    isPaused = false
                                    stopAllAlarms()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (timeLeft > 0) 28.dp else 24.dp)
                                )
                                AnimatedVisibility(
                                    visible = timeLeft <= 0,
                                    enter = fadeIn(tween(300)) + expandHorizontally(tween(300)),
                                    exit = fadeOut(tween(300)) + shrinkHorizontally(tween(300))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.btn_ferma_timer),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                focusManager.clearFocus()

                                val padded = rawInput.padStart(4, '0')
                                val m = padded.substring(0, 2).toLongOrNull() ?: 0L
                                val s = padded.substring(2, 4).toLongOrNull() ?: 0L
                                val totalSeconds = (m * 60) + s

                                if (totalSeconds > 0) {
                                    timeLeft = totalSeconds
                                    isRunning = true
                                    isPaused = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_avvia_timer),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            stopAllAlarms()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.desc_chiudi_icon),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_chiudi),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }
}

/**
 * ====================================================================
 * LOGICA DI VIBRAZIONE CONTINUA
 * ====================================================================
 */
private fun triggerContinuousAlarm(vibrator: android.os.Vibrator) {
    val timings = longArrayOf(0, 400, 200, 400, 200)
    val amplitudes = intArrayOf(0, 255, 0, 255, 0)
    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
}

/**
 * ====================================================================
 * GESTORE AUDIO: Suono di Allarme (RingtoneManager)
 * ====================================================================
 */
private fun riproduciSuonoTimer(context: Context): Ringtone? {
    return try {
        var suonoUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (suonoUri == null) {
            suonoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val ringtone = RingtoneManager.getRingtone(context, suonoUri)
        ringtone?.play()
        ringtone
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
