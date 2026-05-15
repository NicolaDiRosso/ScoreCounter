package com.n380.scorecounter.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * ====================================================================
 * COMPONENTE: TimerSettingsDialog (Versione Ultra-Feedback)
 * ====================================================================
 * Novità introdotte:
 * 1. Focus Detection: Il display cambia bordo quando la tastiera è attiva.
 * 2. ImeAction.Done: Il tasto "Check" ora chiude forzatamente la tastiera.
 * 3. Alert Color: Arancione per i 10 secondi finali, Rosso per l'overtime.
 * ====================================================================
 */
@Composable
fun TimerSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    // Il FocusManager è l'oggetto di sistema che gestisce "chi sta guardando chi".
    // Lo usiamo per dire alla tastiera: "Nasconditi, l'utente ha finito".
    val focusManager = LocalFocusManager.current

    // Gestore vibrazione
    val vibratorManager = remember { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager }
    val vibrator = remember { vibratorManager.defaultVibrator }

    // STATI
    var isRunning by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableLongStateOf(0L) }
    var rawInput by remember { mutableStateOf("") }

    // 🎯 STATO FOCUS: Registra se l'utente ha il cursore dentro il timer
    var isTextFieldFocused by remember { mutableStateOf(false) }

    // Gestione tasto Back del telefono
    BackHandler {
        vibrator.cancel()
        onDismiss()
    }

    // 🚀 MOTORE DEL TIMER (LOGICA)
    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning) {
            delay(1000L)
            timeLeft -= 1
            if (timeLeft <= 0) {
                // Ripetizione infinita (indice 0) finché non fermiamo il vibratore
                triggerContinuousAlarm(vibrator)
            }
        }
    }

    // 🎨 LOGICA COLORI DINAMICI
    // L'istruzione 'when' di Kotlin valuta le condizioni partendo dall'alto verso il basso.
    // La prima condizione che risulta 'Vera' vince e ferma la cascata.
    val displayColor by animateColorAsState(
        targetValue = when {
            // 1. Se il timer è FERMO, l'utente è in modalità "Inserimento".
            // Il colore DEVE essere il Primary dell'app. Ignoriamo i vecchi valori negativi di timeLeft.
            !isRunning -> MaterialTheme.colorScheme.primary

            // 2. OVERTIME: Il timer sta girando (lo sappiamo perché ha superato la regola 1)
            // ed è sceso a zero o sottozero. Scatta l'Allarme Rosso.
            timeLeft <= 0 -> Color(0xFFE8514C)

            // 3. WARNING: Il timer sta girando, mancano 10 secondi o meno. Scatta l'Arancione.
            timeLeft <= 10 -> Color(0xFFEE9850)

            // 4. NORMALITÀ: Il timer sta girando e mancano più di 10 secondi. Colore standard.
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 500), //'animateColorAsState' rendere la transizione tra colori fluida essa dura 0,5 secondi.
        label = "ColorAnimation"
    )

    AlertDialog(
        onDismissRequest = { }, // Blocca la chiusura cliccando fuori
        title = {
            Text(
                "⏱️ Timer Sfida",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                // ====================================================================
                // DISPLAY DIGITALE CON FEEDBACK FOCUS
                // ====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        // 🎯 FEEDBACK VISIVO: Se il campo ha il focus, disegniamo un bordo colorato
                        .border(
                            border = if (isTextFieldFocused && !isRunning)
                                BorderStroke(2.dp, displayColor)
                            else
                                BorderStroke(0.dp, Color.Transparent),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRunning) {
                        // MODALITÀ COUNTDOWN
                        val totalSeconds = abs(timeLeft)
                        val m = totalSeconds / 60
                        val s = totalSeconds % 60
                        val sign = if (timeLeft < 0) "-" else ""

                        Text(
                            text = "$sign%02d:%02d".format(m, s),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Black,
                                color = displayColor
                            )
                        )
                    } else {
                        // MODALITÀ EDIT (MICROONDE)
                        val paddedInput = rawInput.padStart(4, '0')
                        val formattedDisplay = "${paddedInput.substring(0, 2)}:${paddedInput.substring(2, 4)}"

                        BasicTextField(
                            value = rawInput,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }
                                rawInput = digits.takeLast(4)
                            },
                            // 🎯 GESTIONE FOCUS: Questo modifier "sente" quando l'utente entra o esce dal campo
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isTextFieldFocused = focusState.isFocused
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done // Mostra la spunta (Check)
                            ),
                            // 🎯 AZIONE TASTIERA: Quando premi "Done", chiudi tutto
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus() // Nasconde la tastiera rimuovendo il focus
                                }
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

                // PULSANTI RAPIDI
                if (!isRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 5, 10).forEach { mins ->
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    rawInput = (mins * 100).toString()
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("%02d:00".format(mins), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
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
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isRunning) {
                            isRunning = false
                            vibrator.cancel()
                        } else {
                            // Chiudiamo la tastiera se è aperta premendo Avvia
                            focusManager.clearFocus()

                            val padded = rawInput.padStart(4, '0')
                            val m = padded.substring(0, 2).toLongOrNull() ?: 0L
                            val s = padded.substring(2, 4).toLongOrNull() ?: 0L
                            val totalSeconds = (m * 60) + s

                            if (totalSeconds > 0) {
                                timeLeft = totalSeconds
                                isRunning = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRunning) "FERMA TIMER" else "AVVIA TIMER", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        vibrator.cancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("CHIUDI", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

/**
 * LOGICA DI VIBRAZIONE CONTINUA
 * Innesca il pattern hardware e lo ripete finché non viene invocato cancel().
 */
private fun triggerContinuousAlarm(vibrator: android.os.Vibrator) {
    val timings = longArrayOf(0, 400, 200, 400, 200)
    val amplitudes = intArrayOf(0, 255, 0, 255, 0)

    // Lo '0' istruisce il sistema a ricominciare il ciclo dall'indice 0 all'infinito.
    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
}