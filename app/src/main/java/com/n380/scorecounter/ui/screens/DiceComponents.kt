package com.n380.scorecounter.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.n380.scorecounter.ui.components.AutoResizedText

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
            onDismissRequest = onDismiss,
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
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    pendingDiceSides = sides
                                                    customDiceInput = ""
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                // 🧠 UX FIX: Padding orizzontale da 12dp per evitare che il testo "Dado (D6)" tocchi i bordi
                                                AutoResizedText(
                                                    stringResource(R.string.label_dado, sides),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
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
            onDismissRequest = onDismiss,
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        diceOptions.chunked(2).forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowItems.forEach { sides ->
                                    val isSelected = pendingDiceSides == sides && customDiceInput.isEmpty()
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(60.dp)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                pendingDiceSides = sides
                                                customDiceInput = ""
                                            },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            AutoResizedText(
                                                stringResource(R.string.label_dado, sides),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
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
 * COMPONENTE: DiceRollDialog
 * ====================================================================
 * Mostra l'animazione del lancio del dado (Effetto Slot Machine).
 * Utilizza una chiave univoca (Pair) per forzare l'animazione ad ogni click.
 * ====================================================================
 */
@Composable
fun DiceRollDialog(
    result: Int,
    diceSides: Int,
    rollCount: Int,
    onRollAgain: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoResizedText(
                    text = stringResource(R.string.titolo_lancio_dado),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎲 ", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)

                AnimatedContent(
                    targetState = Pair(result, rollCount),
                    transitionSpec = {
                        val enterAnimation = slideInVertically(animationSpec = tween(300)) { height -> height } +
                                fadeIn(animationSpec = tween(300))
                        val exitAnimation = slideOutVertically(animationSpec = tween(300)) { height -> -height } +
                                fadeOut(animationSpec = tween(300))
                        enterAnimation.togetherWith(exitAnimation)
                    },
                    label = "DiceRollAnimation"
                ) { targetPair ->
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
