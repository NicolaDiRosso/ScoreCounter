package com.n380.scorecounter.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource // Import per abilitare la lettura dal file strings.xml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R // Import per accedere agli ID univoci (es. R.string...)

//animazione dei numeri casuali del dado
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
    // ====================================================================
    // 🧠 LEZIONE TEORICA: STATE HOISTING (Sollevamento dello Stato)
    // ====================================================================
    // In Jetpack Compose, l'architettura perfetta prevede che i componenti "figli" (come questo popup)
    // non modifichino mai direttamente i dati centrali dell'app. Devono essere componenti "stupidi" e reattivi.
    // I dati e le decisioni vengono "sollevati" (hoisted) verso il "padre" (CreateMatchScreen).
    //
    // 1. DATO IN INGRESSO (Sola Lettura):
    currentSides: Int, // Riceve il numero di facce attuale (es. 6) dal padre. Non può modificarlo.

    // 2. EVENTO IN USCITA (Callback / Lambda Expression):
    // La sintassi '(Int) -> Unit' definisce una funzione che accetta un numero intero e non restituisce nulla.
    // Funge da "walkie-talkie": quando l'utente preme "Applica", questo componente figlio chiama la funzione
    // per avvisare il padre: "Ehi, l'utente ha scelto questo nuovo numero! Pensaci tu a salvarlo nel database!".
    onSidesChanged: (Int) -> Unit, // Invia indietro il nuovo numero scelto quando si preme "Applica"

    // 3. EVENTO DI CHIUSURA:
    onDismiss: () -> Unit // Chiude il popup (anche questa è una funzione delegata al padre per togliere la visibilità)
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
        title = {
            // 🧠 LEZIONE I18N: Utilizzo Base
            // Sostituiamo il testo hardcoded (es. "Dado Spareggio") con stringResource().
            // Il sistema va nel file strings.xml, cerca la riga 'titolo_impostazioni_dado' e stampa
            // la stringa associata in base alla lingua del dispositivo, senza bisogno di scrivere logiche if/else.
            Text(stringResource(R.string.titolo_impostazioni_dado), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // 🧠 LEZIONE I18N: Testi lunghi descrittivi
                // Anche i testi lunghi e complessi vengono estratti. Questo rende il codice Kotlin
                // molto più pulito da leggere, delegando i "muri di testo" ai file XML.
                Text(
                    text = stringResource(R.string.desc_impostazioni_dado),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 🧠 LEZIONE I18N: Sostituzione singola riga
                // Qui peschiamo la stringa "Scegli un formato rapido..." in italiano, o "Choose a quick format..." in inglese.
                Text(stringResource(R.string.label_scegli_formato_dado), style = MaterialTheme.typography.bodyMedium)

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
                                    shape = RoundedCornerShape(12.dp),
                                    // 🎨 UI DESIGN: Aggiungiamo un bordo per definire meglio la forma delle card.
                                    // Utilizzare un BorderStroke aiuta a far risaltare il componente e a dare coerenza
                                    // con il resto del design system (es. campi di testo e pulsanti secondari).
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        // ==========================================================
                                        // 🧠 LEZIONE TEORICA I18N: I SEGNAPOSTO FORMATTATI (Formatted Placeholders)
                                        // ==========================================================
                                        // Nel vecchio codice avevi la stringa interpolata: text = "D$sides"
                                        // Questo approccio hardcoded è il nemico numero uno delle traduzioni. In altre lingue,
                                        // la lettera 'D' potrebbe andare DOPO il numero, oppure la parola potrebbe essere diversa.
                                        //
                                        // LA SOLUZIONE: Nel file strings.xml scriviamo <string name="label_dado">D%d</string>
                                        // - '%d' (Decimal): È un "buco" che avvisa il sistema che lì in mezzo andrà un numero intero.
                                        // (Se avessimo dovuto inserirci un testo, avremmo usato '%s' per String).
                                        //
                                        // Quando chiamiamo stringResource passandogli 'sides' come parametro aggiuntivo,
                                        // la funzione inietterà automaticamente il valore esattamente al posto del '%d',
                                        // rispettando magicamente la sintassi e l'ordine della lingua in uso!
                                        // ==========================================================
                                        Text(
                                            stringResource(R.string.label_dado, sides),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                    // 🧠 LEZIONE I18N: Traduzione dell'intestazione dell'input manuale ("Inserimento manuale")
                    Text(
                        text = stringResource(R.string.label_inserimento_manuale),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = customDiceInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) customDiceInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        // 🧠 LEZIONE I18N: Parametri dinamici di un TextField
                        // I testi di aiuto all'interno dei campi compilabili (placeholder e label)
                        // si traducono esattamente come un normale componente Text.
                        placeholder = { Text(stringResource(R.string.hint_es_facce_dado)) },
                        label = { Text(stringResource(R.string.hint_n_facce)) },
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
                ) {
                    // 🧠 LEZIONE I18N: Riutilizzo Intelligente (Reusability)
                    // La chiave R.string.btn_annulla ("Annulla"/"Cancel") è usata in tante schermate.
                    // Invece di creare un ID nuovo per ogni bottone, usiamo sempre lo stesso riferimento.
                    // Così, se un domani vorremo cambiare "Annulla" in "Cancella", basterà modificare
                    // il file XML una volta sola e l'app si aggiornerà ovunque!
                    Text(stringResource(R.string.btn_annulla), maxLines = 1)
                }

                Button(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        val manualSides = customDiceInput.toIntOrNull()
                        val finalSides = if (manualSides != null && manualSides > 0) manualSides else pendingDiceSides

                        onSidesChanged(finalSides)
                        onDismiss()
                    }
                ) {
                    // 🧠 LEZIONE I18N: Estrazione etichetta "Applica"
                    Text(stringResource(R.string.btn_applica))
                }
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
    result: Int, // Il numero casuale generato, in sola lettura
    diceSides: Int, // Riceviamo il numero di facce del dado (ci serve per l'intestazione testuale)

    // 🧠 GESTIONE DELLO STATO: CHIAVE DI FORZATURA ANIMAZIONE
    // Se l'utente lancia il dado e ottiene 4, e poi rilancia ottenendo ancora 4,
    // Compose ignorerebbe il cambiamento perché "result" è identico.
    // Incrementando 'rollCount' a ogni click dal padre, cambiamo le carte in tavola
    // e costringiamo Compose a ri-eseguire l'animazione per forza!
    rollCount: Int, // Indice univoco del lancio per forzare la reattività di Compose

    onRollAgain: () -> Unit, // Callback per urlare al padre: "L'utente rivuole lanciare!"
    onDismiss: () -> Unit    // Callback per urlare al padre: "Nascondi questa finestra!"
) {
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // 🧠 LEZIONE I18N: Segnaposto (Placeholder) Avanzato
            // Anche qui utilizziamo un parametro aggiuntivo (diceSides).
            // Nel file strings.xml abbiamo: <string name="titolo_lancio_dado">Lancio del dado (D%d)</string>
            // La funzione stringResource rimpiazzerà il '%d' con il numero contenuto in 'diceSides'.
            Text(
                text = stringResource(R.string.titolo_lancio_dado, diceSides),
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
                    // (Qui non serve I18N perché è un numero nudo e crudo!)
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
                ) {
                    // 🧠 LEZIONE I18N: Riutilizzo chiave di chiusura.
                    Text(stringResource(R.string.btn_chiudi), fontWeight = FontWeight.Bold)
                }

                Button(
                    modifier = Modifier.weight(1f).height(55.dp),
                    shape = RoundedCornerShape(24.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRollAgain()
                    }
                ) {
                    // 🧠 LEZIONE I18N: Lettura dell'etichetta "Rilancia" dal dizionario
                    AutoResizedText(stringResource(R.string.btn_rilancia), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null
    )
}