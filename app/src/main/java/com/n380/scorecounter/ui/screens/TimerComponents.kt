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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.n380.scorecounter.R
import com.n380.scorecounter.ui.components.FadedRightEdgeWrapper
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * ====================================================================
 * COMPONENTE: TimerSettingsDialog (Versione Definitiva Audio/Vibrazione & Responsive)
 * ====================================================================
 */
@Composable
fun TimerSettingsDialog(
    initialRawInput: String,
    onInputChanged: (String) -> Unit,
    onDismiss: () -> Unit
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
            delay(1000L)
            if (!isRunning) return@LaunchedEffect

            timeLeft -= 1

            if (timeLeft <= 0 && activeRingtone == null) {
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

    // Lista unificata dei 6 Preset Rapidi del Timer
    val quickPresets = listOf(
        Pair("00:30", "30"),
        Pair("01:00", "100"),
        Pair("02:00", "200"),
        Pair("05:00", "500"),
        Pair("10:00", "1000"), // 10 Minuti (Nuovo!)
        Pair("15:00", "1500")  // 15 Minuti (Nuovo!)
    )

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // ====================================================================
        // 🧠 LEZIONE TEORICA ORIZZONTALE: 2-Column Split Dialog per Timer
        // ====================================================================
        // 1. MOTIVAZIONE UX: Nello smartphone in orizzontale l'altezza è ridotta (~360dp).
        //    Un AlertDialog standard impila titolo, display, preset e bottoni in verticale,
        //    nascondendo o tagliando i preset rapidi del timer sotto i bottoni!
        // 2. DISPOSIZIONE A 2 COLONNE (75% Larghezza, 85% Altezza):
        //    - Sinistra (48%): Titolo, Display Digitale del tempo ("00:00") e Pulsanti d'Azione.
        //    - Destra (52%): Griglia 2x3 a TUTTA ALTEZZA con i 6 Preset Rapidi (00:30 fino a 15:00).
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

                    // COLONNA DESTRA (52%): 6 PRESET RAPIDI ADATTIVI A TUTTA ALTEZZA (SENZA SPAZI VUOTI!)
                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Preset Rapidi",
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
                            quickPresets.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f), // 🌟 Ogni riga prende 1/3 dell'altezza totale!
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { preset ->
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                rawInput = preset.second
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(), // 🌟 La card si allunga per riempire la riga!
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = preset.first,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
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
                    verticalArrangement = Arrangement.spacedBy(20.dp),
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
                            .padding(vertical = 24.dp),
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
                                    fontSize = 60.sp,
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
                                            fontSize = 60.sp,
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
                        // 🧠 LEZIONE TEORICA VERTICALE: FadedRightEdgeWrapper + LazyRow
                        // Avvolgiamo i 6 preset in un 'FadedRightEdgeWrapper' con scorrimento orizzontale LazyRow.
                        // Usiamo contentPadding(end = 40.dp) così l'ultimo chip (15:00) scivola DEL TUTTO oltre la sfumatura quando si arriva in fondo!
                        FadedRightEdgeWrapper(
                            modifier = Modifier.fillMaxWidth(),
                            fadeColor = MaterialTheme.colorScheme.surface
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 40.dp) // 🧠 UX FIX: Abbondante margine finale per far scorrere l'ultimo chip oltre la sfumatura
                            ) {
                                items(quickPresets) { preset ->
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            rawInput = preset.second
                                        },
                                        modifier = Modifier
                                            .width(88.dp)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = preset.first,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
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
                                        .height(56.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
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
                                    .height(56.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (timeLeft > 0) 32.dp else 24.dp)
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
                            modifier = Modifier.fillMaxWidth().height(56.dp),
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
                        modifier = Modifier.fillMaxWidth().height(56.dp),
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
