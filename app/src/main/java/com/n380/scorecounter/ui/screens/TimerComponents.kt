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

// Import per la suoneria di sistema
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

/**
 * ====================================================================
 * COMPONENTE: TimerSettingsDialog (Versione Definitiva Audio/Vibrazione)
 * ====================================================================
 * Novità introdotte:
 * 1. Focus Detection: Il display cambia bordo quando la tastiera è attiva.
 * 2. ImeAction.Done: Il tasto "Check" ora chiude forzatamente la tastiera.
 * 3. Alert Color: Arancione per i 10 secondi finali, Rosso per l'overtime.
 * 4. Gestione Audio Sicura: Previene la sovrapposizione di suoni multipli.
 * ====================================================================
 */
@Composable
fun TimerSettingsDialog(
    //STATE HOISTING (Sollevamento dello Stato)
    // Il Dialog riceve la stringa iniziale dal Padre (CounterScreen)
    // e usa 'onInputChanged' per avvisare il Padre di ogni modifica.
    initialRawInput: String,          // 1. Riceve il testo salvato dal padre
    onInputChanged: (String) -> Unit, // 2. "Walkie-talkie" per avvisare il padre delle modifiche
    onDismiss: () -> Unit
) {
    // ----------------------------------------------------------------
    // VARIABILI DI SISTEMA (Context e Manager)
    // ----------------------------------------------------------------
    // 'LocalContext.current' ci dà l'accesso alle risorse fisiche e di sistema del telefono.
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Il FocusManager ci permette di dire alla tastiera: "Nasconditi, l'utente ha finito".
    val focusManager = LocalFocusManager.current

    // Gestore vibrazione: Usiamo il Context per chiedere ad Android il permesso di usare la vibrazione.
    val vibratorManager = remember { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager }
    val vibrator = remember { vibratorManager.defaultVibrator }

    // ================================================================
    // 🧠 TEORIA DELLO STATO (State Management) IN COMPOSE
    // ================================================================
    // 'isRunning': Booleana. Ci dice se il cronometro sta andando (true) o è fermo (false).
    var isRunning by remember { mutableStateOf(false) }

    // 'timeLeft': Usa 'mutableLongStateOf' perché è ottimizzato per i numeri grandi (Long).
    var timeLeft by remember { mutableLongStateOf(0L) }

    // 🎵 STATO AUDIO: Memorizza la suoneria in riproduzione per poterla fermare.
    // Inizialmente è 'null' perché non c'è nessuna musica in riproduzione.
    var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }

    // 'rawInput': La stringa che l'utente digita fisicamente sulla tastiera (es. "1500")
    var rawInput by remember { mutableStateOf(initialRawInput) }

    // Sincronizza ogni tasto premuto inviandolo al padre
    LaunchedEffect(rawInput) {
        onInputChanged(rawInput)
    }

    // 🎯 STATO FOCUS: Registra se l'utente ha il cursore dentro il timer
    var isTextFieldFocused by remember { mutableStateOf(false) }

    // 🧹 HELPER GLOBALE: Spegne tutto l'hardware (Vibrazione + Suono)
    // Creiamo questa funzione una volta sola per poterla usare nei pulsanti e nel tasto "Back"
    // senza dover riscrivere sempre le stesse tre righe.
    val stopAllAlarms = {
        vibrator.cancel() // Ferma il motorino fisico
        activeRingtone?.stop() // Ferma il lettore musicale (se esiste)
        activeRingtone = null // 🧠 CRITICO: Resetta la variabile a null così il sistema sa che c'è silenzio
    }

    // Gestione tasto Back del telefono (gesture o freccia indietro fisica)
    BackHandler {
        stopAllAlarms() // Spegne tutto se l'utente scappa via con la gesture indietro
        onDismiss()
    }

    // ================================================================
    // 🚀 MOTORE DEL TIMER (EFFETTI COLLATERALI / SIDE EFFECTS)
    // ================================================================
    LaunchedEffect(isRunning, timeLeft) {
        // Se il timer è attivo...
        if (isRunning) {
            delay(1000L) // Pausa di 1 secondo esatto
            timeLeft -= 1 // Togliamo 1 secondo

            // Con "&& activeRingtone == null", creiamo un cancello.
            // Quando il tempo arriva a 0, la musica non c'è (è null), quindi entra e la fa partire.
            // Quando il tempo arriva a -1, la musica c'è GIA' (non è null), quindi NON entra
            // e non sovrappone un altro suono!
            if (timeLeft <= 0 && activeRingtone == null) {
                triggerContinuousAlarm(vibrator) // Fa partire la vibrazione a loop
                activeRingtone = riproduciSuonoTimer(context) // Fa partire la suoneria e la SALVA nella variabile
            }
        }
    }

    // ====================================================================
    // 🎨 LOGICA COLORI DINAMICI E ANIMAZIONI DI STATO
    // ====================================================================
    // 🧠 TEORIA: 'animateColorAsState' è una magia di Compose.
    // Se tu dicessi a Compose "cambia colore da Blu a Rosso", lui lo farebbe in 1 millisecondo (un flash brutto da vedere).
    // Usando 'animateColorAsState', tu gli dai il colore di destinazione (targetValue), e Compose crea
    // automaticamente tutti i fotogrammi intermedi per sfumare dolcemente dal vecchio al nuovo colore!
    val displayColor by animateColorAsState(
        targetValue = when {
            // L'istruzione 'when' è come una cascata. Scende dall'alto e si ferma alla PRIMA condizione vera.

            // 1. Se il timer è FERMO (!isRunning), l'utente sta digitando. Colore Blu standard.
            !isRunning -> MaterialTheme.colorScheme.primary

            // 2. Se il tempo è <= 0. La L (0L) indica che è un numero di tipo Long (più capiente di un Int normale).
            // Usiamo il rosso per indicare il pericolo/overtime.
            timeLeft <= 0 -> Color(0xFFE8514C) // Rosso Overtime

            // 3. Se mancano 10 o meno secondi. È un avviso, usiamo l'arancione.
            timeLeft <= 10 -> Color(0xFFEE9850) // Arancione Warning

            // 4. 'else' è il caso base. Se nessuna delle precedenti è vera (es. mancano 50 secondi), colore standard.
            else -> MaterialTheme.colorScheme.primary
        },
        // 🛠️ PRATICA: 'tween' è l'animazione. durationMillis = 500 significa che la sfumatura di colore
        // impiegherà esattamente mezzo secondo (500 millisecondi) per completarsi.
        animationSpec = tween(durationMillis = 1000),
        label = "ColorAnimation"
    )

    // ====================================================================
    // 📦 LA FINESTRA DI DIALOGO (Popup)
    // ====================================================================
    AlertDialog(
        // 🧠 TEORIA: onDismissRequest è l'evento che scatta quando l'utente tocca lo schermo nero FUORI dal popup.
        // Lasciando le parentesi graffe vuote { }, annulliamo questo evento. Risultato?
        // Il popup non si chiude se clicchi fuori per sbaglio. Si chiude SOLO coi nostri pulsanti.
        onDismissRequest = { },
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
                verticalArrangement = Arrangement.spacedBy(20.dp), // Distanzia i figli di 20dp l'uno dall'altro
                modifier = Modifier.fillMaxWidth()
            ) {

                // ====================================================================
                // 🖥️ IL DISPLAY DIGITALE CON FEEDBACK DEL FOCUS
                // ====================================================================
                // Un 'Box' è un contenitore in cui gli elementi si sovrappongono l'uno sull'altro come fogli.
                Box(
                    // I modificatori (Modifier) in Compose si leggono a cascata (dall'alto in basso):
                    modifier = Modifier
                        .fillMaxWidth() // 1. Prima prendi tutta la larghezza
                        .clip(RoundedCornerShape(16.dp)) // 2. Poi arrotonda i tuoi angoli
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)) // 3. Poi colorati lo sfondo (con trasparenza al 20%)

                        // 🛠️ PRATICA: Il Bordo Dinamico
                        // Se l'utente sta scrivendo (Focus = true) E il timer è fermo, disegna un bordo di 2dp col colore sfumato!
                        // Altrimenti, disegna un bordo invisibile (Color.Transparent).
                        .border(
                            border = if (isTextFieldFocused && !isRunning)
                                BorderStroke(2.dp, displayColor)
                            else
                                BorderStroke(0.dp, Color.Transparent),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 24.dp), // 4. Infine, allargati dall'interno creando molto spazio sopra e sotto il testo.
                    contentAlignment = Alignment.Center
                ) {
                    // 🧠 TEORIA DEL RENDERING CONDIZIONALE:
                    // In Compose, un 'if' non nasconde semplicemente un pezzo di grafica... decide letteralmente
                    // se COSTRUIRLO nella memoria del telefono o no.
                    if (isRunning) {
                        // ==========================================================
                        // MODO 1: IL TIMER STA ANDANDO (Sola lettura)
                        // ==========================================================

                        // 🛠️ MATEMATICA DEL TEMPO:
                        // abs() sta per Absolute Value (Valore Assoluto). Trasforma i numeri negativi in positivi (es: -5 diventa 5).
                        // Perché? Perché non possiamo fare la divisione del tempo con i numeri negativi, altrimenti sballa.
                        val totalSeconds = abs(timeLeft)
                        val m = totalSeconds / 60 // Dividendo per 60 troviamo i minuti interi
                        val s =
                            totalSeconds % 60 // Il 'modulo' (%) ci dà il RESTO della divisione. Quei sono i secondi!

                        // Se il tempo reale era sceso sotto zero, prepariamo il segno Meno da appiccicare davanti al testo.
                        val sign = if (timeLeft < 0) "-" else ""

                        // %02d è un format testuale. Dice: "Infila qui il numero, e se è di una sola cifra (es. 5), mettici uno zero davanti (05)".
                        Text(
                            text = "$sign%02d:%02d".format(m, s),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Black,
                                color = displayColor
                            )
                        )
                    } else {
                        // ==========================================================
                        // MODO 2: IL TIMER E' FERMO (Modalità Inserimento)
                        // ==========================================================
                        // padStart(4, '0') assicura che la stringa abbia sempre 4 caratteri. Se l'utente digita "15", diventa "0015".
                        val paddedInput = rawInput.padStart(4, '0')
                        // substring(0, 2) ritaglia i primi due numeri. Mettiamo i due punti in mezzo e abbiamo "00:15"!
                        val formattedDisplay =
                            "${paddedInput.substring(0, 2)}:${paddedInput.substring(2, 4)}"

                        // 🧠 TEORIA DEL BASIC TEXT FIELD:
                        // È il "motore grezzo" che gestisce la tastiera e l'input in Android, ma è privo di grafica.
                        BasicTextField(
                            value = rawInput, // Il testo attualmente digitato
                            onValueChange = { newValue ->
                                // Questa lambda scatta ogni volta che premi un tasto sulla tastiera.
                                // 1. filter { it.isDigit() } butta via lettere o simboli strani.
                                val digits = newValue.filter { it.isDigit() }
                                // 2. takeLast(4) scarta i numeri vecchi spingendoli fuori, tenendo solo gli ultimi 4.
                                // Ecco come si crea l'effetto "Microonde"!
                                rawInput = digits.takeLast(4)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                // 🎯 FOCUS: Questo "Sensore" avverte quando l'utente tocca il testo per aprire la tastiera.
                                // Quando succede, cambiamo la variabile di stato 'isTextFieldFocused', che a sua volta farà accendere il bordo!
                                .onFocusChanged { focusState ->
                                    isTextFieldFocused = focusState.isFocused
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number, // Richiede ad Android la tastiera solo numerica
                                imeAction = ImeAction.Done // Trasforma il tasto Invio (a capo) nella "Spunta" di conferma
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Quando l'utente preme la Spunta, usiamo il FocusManager per "togliere l'attenzione"
                                    // dal campo. Togliendo l'attenzione, Android chiude la tastiera da solo.
                                    focusManager.clearFocus()
                                }
                            ),
                            // 🛠️ IL TRUCCO VISIVO: Rendiamo il testo nativo e la stanghetta lampeggiante TRASPARENTI (Invisibili)
                            textStyle = TextStyle(color = Color.Transparent),
                            cursorBrush = SolidColor(Color.Transparent),

                            // decorationBox ci permette di disegnare sopra al campo invisibile.
                            // L'utente digita nel vuoto, ma noi gli facciamo credere di digitare qui dentro,
                            // disegnando la nostra bellissima scritta formattata "00:00" gigante!
                            decorationBox = {
                                Text(
                                    text = formattedDisplay,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 60.sp,
                                        fontWeight = FontWeight.Black,
                                        // Se hai il focus è luminoso, altrimenti applichiamo alpha=0.6f (semi trasparente) per sembrare "spento"
                                        color = if (isTextFieldFocused) displayColor else displayColor.copy(
                                            alpha = 0.6f
                                        )
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                }

                // ====================================================================
                // 🔘 I PULSANTI RAPIDI PREDEFINITI
                // ====================================================================
                // ====================================================================
                // 🔘 I PULSANTI RAPIDI PREDEFINITI (Versione Flessibile)
                // ====================================================================
                if (!isRunning) {
                    // 🧠 TEORIA DEI PAIR (Coppie):
                    // Pair("Testo Visivo", "Dato Grezzo")
                    // In questo modo svincoliamo la grafica (ciò che legge l'utente)
                    // dalla logica (ciò che serve al nostro trucco del microonde).
                    val quickPresets = listOf(
                        Pair("00:30", "30"),    // Il microonde leggerà "0030" -> 00:30
                        Pair("01:00", "100"),    // Il microonde leggerà "0200" -> 02:00
                        Pair("02:00", "200"),    // Il microonde leggerà "0500" -> 05:00
                        Pair("05:00", "500")   // Il microonde leggerà "1000" -> 10:00
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 🧠 CICLO DINAMICO DEI PULSANTI RAPIDI
                        // .forEach esegue questo blocco di codice per ogni elemento nella lista,
                        // creando i bottoni automaticamente senza dover fare copia-incolla.
                        // Usiamo 'preset ->' per dare un nome chiaro all'elemento corrente (invece del generico 'it').
                        // - preset.first  = L'etichetta visibile sul bottone (es. "30s")
                        // - preset.second = Il valore matematico passato al timer (es. "30")
                        quickPresets.forEach { preset ->
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    // Assegniamo a 'rawInput' esattamente il secondo elemento della coppia (preset.second).
                                    // Non serve più fare moltiplicazioni strane!
                                    rawInput = preset.second
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        // Mostriamo sul bottone il primo elemento della coppia (preset.first).
                                        text = preset.first,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        // ====================================================================
        // 🏁 BOTTONI PRINCIPALI DI CONFERMA
        // ====================================================================
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TASTO START / STOP
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Feedback tattile prolungato

                        if (isRunning) {
                            // Se andava, ora lo SPEGNI.
                            isRunning = false
                            stopAllAlarms() // Spegne subito vibrazione e musica!
                        } else {
                            // Se era fermo, ora lo ACCENDI.
                            focusManager.clearFocus() // Nasconde la tastiera per fare pulizia a schermo

                            // Convertiamo il testo (es. "0130") in matematica vera per il countdown.
                            val padded = rawInput.padStart(4, '0') // Assicura 4 caratteri
                            val m = padded.substring(0, 2).toLongOrNull() ?: 0L // I primi due sono Minuti
                            val s = padded.substring(2, 4).toLongOrNull() ?: 0L // Gli ultimi due sono Secondi
                            val totalSeconds = (m * 60) + s // Formula: (Minuti * 60) + Secondi

                            // Avvia il motore SOLO se l'utente ha messo un tempo maggiore di zero.
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

                // TASTO CHIUDI E ANNULLA
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        stopAllAlarms() // Spegne musica e vibrazioni
                        onDismiss()     // Chiude il popup chiamando la funzione dal file padre
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
 * ====================================================================
 * LOGICA DI VIBRAZIONE CONTINUA
 * ====================================================================
 * Questa funzione comunica direttamente col motorino fisico (Hardware) del telefono.
 */
private fun triggerContinuousAlarm(vibrator: android.os.Vibrator) {
    // 🧠 TEORIA DELL'ONDA (Waveform):
    // I timings dicono al motorino QUANTO TEMPO in millisecondi (ms) deve stare in un certo stato.
    val timings = longArrayOf(0, 400, 200, 400, 200)

    // Le amplitudes dicono al motorino LA POTENZA per ogni tempo. 0 = Spento. 255 = Potenza Massima.
    // Leggendoli insieme: Aspetta 0ms, Vibra al max per 400ms, Spegniti per 200ms, Vibra per 400ms, ecc.
    val amplitudes = intArrayOf(0, 255, 0, 255, 0)

    // L'ultimo parametro '0' è il "Repeat Index" (Indice di ripetizione).
    // Diciamo ad Android di finire l'array e poi ricominciare da capo (posizione 0) all'infinito!
    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
}

/**
 * ====================================================================
 * GESTORE AUDIO: Suono di Allarme (RingtoneManager)
 * ====================================================================
 * Cerca la suoneria di sistema e la riproduce.
 * 🧠 CRITICO: Restituisce l'oggetto Ringtone (il lettore musicale).
 * Noi ce lo salviamo in una variabile nel Compose, così poi possiamo usare il comando .stop() per zittirlo!
 */
private fun riproduciSuonoTimer(context: Context): Ringtone? {
    return try {
        // Uri (Uniform Resource Identifier) è un indirizzo. Chiediamo la suoneria "SVEGLIA" predefinita dal telefono.
        var suonoUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Se il telefono ha un bug e non trova la sveglia, passiamo al Piano B: Suono della NOTIFICA (es. i messaggi).
        if (suonoUri == null) {
            suonoUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // Carichiamo il file audio nel lettore (Ringtone)
        val ringtone = RingtoneManager.getRingtone(context, suonoUri)
        // Lo facciamo partire
        ringtone?.play()

        // Restituiamo il "telecomando" del lettore a chi ha chiamato questa funzione.
        ringtone
    } catch (e: Exception) {
        // I blocchi try-catch evitano che l'app crashi in caso di errori critici di sistema o permessi mancanti.
        e.printStackTrace()
        null // Se fallisce, restituisce 'null', dicendo che non c'è nessun audio in riproduzione.
    }
}