package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

//animazione dei numeri casuali del dado
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith



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
    currentSides: Int, // Riceve il numero di facce attuale (es. 6)
    onSidesChanged: (Int) -> Unit, // Invia indietro il nuovo numero scelto quando si preme "Applica"
    onDismiss: () -> Unit // Chiude il popup
) {
    val haptic = LocalHapticFeedback.current

    // ---> INCAPSULAMENTO DELLO STATO <---
    // Abbiamo spostato queste variabili da CreateMatchScreen a qui dentro!
    // Perché? Perché alla schermata principale non interessa sapere cosa l'utente sta
    // digitando temporaneamente nella casella di testo. Le interessa solo il risultato finale.
    var pendingDiceSides by remember { mutableIntStateOf(currentSides) }
    var customDiceInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dado Spareggio 🎲", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {


                Text(
                    text = "Imposta il dado che potrai lanciare durante la partita per decidere chi inizia o per risolvere i pareggi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Scegli un formato rapido o creane uno tuo:", style = MaterialTheme.typography.bodyMedium)

                // GRIGLIA DADI STANDARD
                val diceOptions = listOf(6, 12, 20, 100)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    diceOptions.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { sides ->
                                val isSelected = pendingDiceSides == sides && customDiceInput.isEmpty()
                                Card(
                                    modifier = Modifier.weight(1f).height(60.dp).clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        pendingDiceSides = sides
                                        customDiceInput = ""
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text("D$sides", fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // --->Divisore Orizzontale<---
                // In questo modo sfrutta solo lo spazio naturale (spacedBy) senza aggiungerne altro!
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // --->Raggruppamento Logico <---
                // Chiudiamo il titolo e la casella in una "mini-colonna" separata.
                // Così tra loro ci saranno solo 4 pixel (spacedBy(4.dp)) invece di 16!
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Inserimento manuale:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = customDiceInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) customDiceInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Es. 380") },
                        label = { Text("N° Facce") },
                        shape = RoundedCornerShape(20.dp),
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
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                    }
                ) { Text("Annulla", maxLines = 1) }

                Button(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        val manualSides = customDiceInput.toIntOrNull()
                        val finalSides = if (manualSides != null && manualSides > 0) manualSides else pendingDiceSides

                        // Comunichiamo il risultato finale alla schermata padre
                        onSidesChanged(finalSides)
                        onDismiss()
                    }
                ) { Text("Applica") }
            }
        },
        dismissButton = null
    )
}

/**
 * ====================================================================
 * COMPONENTE: DiceRollDialog
 * ====================================================================
 * Mostra l'animazione del lancio del dado (Effetto Slot Machine).
 * Utilizza una chiave univoca (Pair) per forzare l'animazione ad ogni click.
 * ====================================================================
 */
@Composable
fun DiceRollDialog(
    result: Int,
    diceSides: Int,//Riceviamo il numero di facce del dado
    rollCount: Int, // Indice univoco del lancio per forzare la reattività di Compose
    onRollAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                // Aggiungiamo dinamicamente il tipo di dado al titolo
                // Es. diventerà "Lancio del dado (D6)" o "Lancio del dado (D20)"
                text = "Lancio del dado (D$diceSides)",//ncon $diceSides facce",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            // Row per mantenere l'emoji ferma e animare solo il numero al suo fianco
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎲 ", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)

                /*
                Recomposition (Ricomposizione) e AnimatedContent:
                In Jetpack Compose, l'interfaccia grafica si aggiorna solo quando lo Stato (State) che sta osservando subisce una mutazione.
                Il componente AnimatedContent ascolta il parametro targetState.
                Quando rileva una mutazione, innesca la transitionSpec (le animazioni di entrata e uscita).
                 */

                // AnimatedContent gestisce la transizione fluida tra il vecchio e il nuovo stato
                AnimatedContent(
                    // Utilizziamo un Pair come chiave: anche se il 'result' è uguale,
                    // il 'rollCount' cambia sempre, forzando l'esecuzione dell'animazione.
                    targetState = Pair(result, rollCount),

                    /**La Tupla (La classe Pair in Kotlin):
                    Un Pair è una struttura dati (una tupla di due elementi) che impacchetta due variabili in un singolo oggetto formattato come (A, B).
                    Passando ad AnimatedContent la chiave Pair(result, rollCount), noi cambiamo l'oggetto dell'uguaglianza strutturale.
                    Analizziamo due lanci consecutivi che danno come risultato 4:
                    -Lancio 1: result è 4, rollCount è 1. Lo stato è Pair(4, 1).
                    -Lancio 2: result è 4, rollCount è 2 (perché il contatore dei click sale sempre). Lo stato diventa Pair(4, 2).

                    Ora Compose esegue il confronto: Pair(4, 1) == Pair(4, 2).
                    Poiché il secondo elemento del Pair è diverso, l'uguaglianza strutturale restituisce false.
                    Compose registra ufficialmente una mutazione di stato, distrugge il nodo precedente e genera un nuovo nodo nel composition tree,
                    innescando l'animazione in modo garantito al 100%.
                     */

                    transitionSpec = {
                        // Animazione di entrata: il nuovo numero arriva dal basso verso l'alto con dissolvenza
                        val enterAnimation = slideInVertically(animationSpec = tween(300)) { height -> height } +
                                fadeIn(animationSpec = tween(300))

                        // Animazione di uscita: il vecchio numero scorre verso l'alto e scompare
                        val exitAnimation = slideOutVertically(animationSpec = tween(300)) { height -> -height } +
                                fadeOut(animationSpec = tween(300))

                        // Esegue entrata e uscita contemporaneamente (effetto Slot Machine)
                        enterAnimation.togetherWith(exitAnimation)
                    },
                    label = "DiceRollAnimation"
                ) { targetPair ->
                    // Mostriamo solo il primo valore della coppia (il risultato del dado)
                    Text(
                        text = "${targetPair.first}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(24.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                    }
                ) { Text("Chiudi",fontWeight = FontWeight.Bold,) }

                Button(
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(24.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRollAgain()
                    }
                ) { Text("Rilancia",fontWeight = FontWeight.Bold,) }
            }
        },
        dismissButton = null
    )
}