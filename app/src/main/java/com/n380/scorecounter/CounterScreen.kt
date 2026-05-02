package com.n380.scorecounter

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ====================================================================
// ====================================================================
// SCHERMATA DEL CONTATORE: Gestione dei punteggi in tempo reale durante la partita.
// ====================================================================

@Composable
fun CounterScreen(
    viewModel: MatchViewModel,
    onNavigateToResults: () -> Unit,
    // Aggiungiamo un parametro per gestire l'uscita d'emergenza verso la Home
    onNavigateHome: () -> Unit
) {
    // Stato per decidere se mostrare il popup di conferma azzeramento
    var showResetDialog by remember { mutableStateOf(false) }

    // Variabile che indica se mostrare l'avviso di uscita "Salva-Vita" (se si preme Indietro per sbaglio)
    var showExitWarning by remember { mutableStateOf(false) }

    // ---> STATI PER IL DADO VIRTUALE <---
    var showDiceDialog by remember { mutableStateOf(false) }
    var diceResult by remember { mutableIntStateOf(1) }

    // --->Ricorda QUALE giocatore stiamo modificando manualmente con la tastiera <---
    // Se è "null", il popup per l'inserimento manuale è nascosto.
    var playerForManualEdit by remember { mutableStateOf<Player?>(null) }

    // STATO TUTORIAL: Controlla l'apertura del popup centrale con le regole nascoste (Manuale)
    var showInfoDialog by remember { mutableStateOf(false) }

    // SISTEMA SNACKBAR (Tasto Annulla Azzeramento)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // ---> AVVIO CRONOMETRO AUTOMATICO <---
    // LaunchedEffect fa partire un blocco di codice non appena questa pagina viene "disegnata" sullo schermo.
    LaunchedEffect(Unit) {
        viewModel.startTimer()
    }

    // ---> SCHERMO SEMPRE ACCESO (Senza modalità immersiva problematica) <---
    // DisposableEffect è magico: esegue un codice quando la pagina si APRE (init) e un altro quando si CHIUDE (onDispose).
    DisposableEffect(Unit) {
        val activity = context as? Activity
        // Appena si apre il contatore, diciamo ad Android di attaccare il "Cartello Divieto di Standby"
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // Appena usciamo da questa pagina, togliamo il cartello e permettiamo allo schermo di riposare.
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ---> IL SALVA-VITA (BackHandler) <---
    // Intercetta il tasto "Indietro" fisico o lo swipe back del telefono. Invece di uscire, fa apparire un avviso!
    BackHandler {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        showExitWarning = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,

        // Montiamo la Snackbar in questa schermata per gli avvisi
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Usiamo la "bottomBar" (barra inferiore) dello Scaffold per i tasti principali d'azione
        // --------------------------------------------------------------------
        // BOTTOM BAR: IL "DOCK" DEI COMANDI
        // --------------------------------------------------------------------
        bottomBar = {
            // ---> LEZIONE: LA PIATTAFORMA VISIVA (Surface) <---
            // Invece di lasciare i bottoni sospesi nel vuoto, li avvolgiamo in una Surface.
            // Questa agirà come un "muro" semi-trasparente che nasconde le carte che scorrono sotto,
            // eliminando il rumore visivo e dando risalto ai bottoni.
            Surface(
                // Sfondo scuro (colore di background del tema) quasi solido al 95%
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                // ANGOLI SUPERIORI ARROTONDATI
                // Usiamo RoundedCornerShape specificando SOLO gli angoli in alto (topStart e topEnd).
                // Mettendo 24.dp creiamo una curva morbida che "abbraccia" visivamente i bottoni da 20.dp,
                // mentre gli angoli in basso restano a 0.dp (piatti) per aderire al vetro del telefono.
                shape = RoundedCornerShape(24.dp),
                // Bordo superiore sottilissimo per staccare nettamente la barra dalla lista
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // navigationBarsPadding() assicura che i bottoni non finiscano MAI sotto
                        // la barra di navigazione nativa di Android (quella con la linea bianca in basso).
                        .navigationBarsPadding()
                        // Padding interno della barra
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ========================================================
                    // TASTO SECONDARIO: AZZERA (GHOST BUTTON)
                    // ========================================================
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showResetDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp), // Altezza Expressive massiccia
                        shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Azzera",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ========================================================
                    // TASTO PRIMARIO: FINE MATCH (CALL TO ACTION)
                    // ========================================================
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.pauseTimer()
                            onNavigateToResults()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp), // Altezza Expressive massiccia
                        shape = RoundedCornerShape(20.dp), // Angoli coerenti col Design System
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = "Fine Match",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            // Intestazione con TITOLO/CRONOMETRO a sinistra e PULSANTE DADO a destra
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Raggruppo Titolo, Cronometro e Info in una Colonna per tenerli vicini
                Column(modifier = Modifier.weight(1f)) {
                    // Titolo della sfida (Con salvagente se l'utente l'ha lasciato vuoto)
                    Text(
                        text = if (viewModel.matchTitle.isEmpty()) "Sfida" else viewModel.matchTitle,
                        // Usiamo l'esatto stile gigante della schermata Home e Nuova Partita
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold, // Grassetto massiccio
                        color = MaterialTheme.colorScheme.primary // Lo coloriamo a tema
                    )
                    // ---> CRONOMETRO DI PARTITA E INFO <---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Tempo di gioco",
                            modifier = Modifier.size(18.dp).padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        // Richiamiamo la nostra funzione di supporto per stampare il tempo bello "00:00"
                        Text(
                            text = formatTime(viewModel.matchDurationSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )

                        // ---> SPOSTATO QUI: L'icona delle Informazioni <---
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showInfoDialog =
                                    true // Cambia lo stato a VERO e fa apparire il popup!
                            },
                            // Riduciamo la grandezza del bottone invisibile per non sformare la riga del cronometro
                            modifier = Modifier.padding(start = 4.dp).size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Informazioni App",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp) // Icona leggermente più piccola per allinearsi al testo
                            )
                        }
                    }
                }

                // ---> PULSANTE LANCIA DADO (Dinamico) <---
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // ---> LA MAGIA <---
                        // Invece di (1..6), usiamo (1..viewModel.diceSides).
                        // Se hai scelto il D20, calcolerà un numero casuale da 1 a 20!
                        diceResult = (1..viewModel.diceSides).random()
                        showDiceDialog = true
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    // Cambiamo il testo del bottone per mostrare SEMPRE quale dado stiamo usando (Es. "Lancia D20")
                    Text("Lancia D${viewModel.diceSides} 🎲")
                }
            }


            // ---> CALCOLO DELLA CORONA DEL LEADER IN TEMPO REALE <---
            // Per assegnare la corona, dobbiamo sapere chi ha il punteggio più alto in questo esatto millisecondo.
            // La funzione maxOfOrNull scansiona tutti i giocatori e trova il punteggio massimo (se sono tutti a 0, assegna 0 di default).
            val maxScore = viewModel.players.maxOfOrNull { it.score } ?: 0

            // ---> CONTROLLO VITTORIA AUTOMATICA <---
            // LaunchedEffect(maxScore) è un "osservatore spia". Gli diciamo di tenere d'occhio la variabile "maxScore".
            // Ogni volta che i punti di un giocatore cambiano alzando l'asticella, lui esegue questo blocco alla velocità della luce.
            LaunchedEffect(maxScore) {
                // Trasformiamo il testo dell'obiettivo in un numero vero (Int). Se il campo è vuoto, diventa "null".
                val target = viewModel.targetScore.toIntOrNull()

                // Regola della Vittoria: Se l'obiettivo esiste, è maggiore di 0, e il leader ha raggiunto o superato l'obiettivo...
                if (target != null && target > 0 && maxScore >= target) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibrazione della vittoria!
                    viewModel.pauseTimer() // Fermiamo l'orologio
                    onNavigateToResults() // Passiamo automaticamente alla schermata finale dei coriandoli!
                }
            }

            // ---> CONTROLLO ANTI-CARTE <---
            // Trasformiamo il titolo in minuscolo e cerchiamo la parola "carte".
            // Se trovata, 'isFireEnabled' sarà false e il colore arancione della combo non apparirà mai.
            val isFireEnabled = !viewModel.matchTitle.lowercase()
                .contains("carte")//Questa variabile è VERA se il titolo NON contiene la parola "carte".

            // ---> IL NUOVO CONTENITORE DEL TAVOLO (Box di Sfondo) <---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // weight(1f) dice alla Card: "Allungati occupando tutto lo spazio vuoto che c'è tra
                    // l'intestazione in alto e i bottoni in basso"
                    .weight(1f)
                    // Aggiungiamo un margine inferiore per non farla incollare ai tasti "Azzera/Fine Match"
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                // ---> MODIFICA 1: Stondatura completa <---
                // Scrivendo solo 24.dp (senza specificare top o bottom), Android stonda tutti e 4 gli angoli!
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    // Abbiamo ridotto il padding 'top' perché non essendoci più il titolo,
                    // non serve più tutto quello spazio vuoto in alto.
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 12.dp, end = 12.dp)
                ) {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        // ---> MODIFICA 3: Cuscinetto ridotto <---
                        // Siccome la scatola grigia ora finisce PRIMA dei bottoni e non ci scivola più dietro,
                        // non ci serve più quel trucco del padding a 100.dp. Bastano 16.dp per far
                        // scorrere bene l'ultima carta senza farla incollare al bordo inferiore.
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(viewModel.players) { p ->

                            val isLeader = p.score == maxScore && maxScore > 0

                            PlayerScoreCard(
                                player = p,
                                isLeader = isLeader,
                                isFireEnabled = isFireEnabled,
                                onScoreChange = { amount ->
                                    viewModel.updatePlayerScore(p, amount)
                                },
                                onScoreClick = { playerForManualEdit = p }
                            )
                        }
                    }
                }
            }
        }
    }


    // ---> NUOVO: POPUP PER L'INSERIMENTO MANUALE DEL PUNTEGGIO DA TASTIERA <---
    if (playerForManualEdit != null) {
        // Stringa temporanea per ricordare cosa l'utente sta scrivendo. La pre-compiliamo col punteggio attuale!
        var scoreInput by remember { mutableStateOf(playerForManualEdit!!.score.toString()) }

        AlertDialog(
            onDismissRequest = {
                playerForManualEdit = null
            }, // Se l'utente clicca fuori, si chiude annullando
            title = { Text("Inserisci Punti per ${playerForManualEdit!!.name}") },
            text = {
                OutlinedTextField(
                    value = scoreInput,
                    onValueChange = { newValue ->
                        // Controllo di Sicurezza: Consentiamo all'utente di scrivere SOLO numeri.
                        // Accettiamo anche il segno meno "-" da solo, così l'utente può digitare punteggi negativi (es. "-10")
                        if (newValue.isEmpty() || newValue == "-" || newValue.toIntOrNull() != null) {
                            scoreInput = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    // ---> LEZIONE TASTIERA: Forza l'apertura del Tastierino Numerico del telefono (Niente lettere!) <---
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Trasformiamo il testo digitato in un numero vero. Se l'utente ha scritto cavolate o ha lasciato vuoto, mettiamo 0 per sicurezza.
                    val newScore = scoreInput.toIntOrNull() ?: 0

                    // ---> IL TRUCCO PER IL GRAFICO E LA COMBO <---
                    // Invece di dirgli "Il tuo nuovo punteggio è 50" (che romperebbe il grafico perché mancherebbe uno step),
                    // Calcoliamo la DIFFERENZA: (Nuovo Punteggio - Vecchio Punteggio).
                    // Es: Se aveva 10 e scrive 50, la differenza è +40.
                    val diff = newScore - playerForManualEdit!!.score

                    // Passiamo la differenza alla funzione updatePlayerScore!
                    // Così l'inserimento manuale da tastiera vale anche per scatenare (o rompere) la combo "On Fire"!
                    viewModel.updatePlayerScore(playerForManualEdit!!, diff)

                    playerForManualEdit = null // Chiudiamo il popup soddisfatti
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { playerForManualEdit = null }) { Text("Annulla") }
            }
        )
    }

    // --------------------------------------------------------------------
    // DIALOGO DI CONFERMA: AZZERAMENTO PUNTEGGI
    // --------------------------------------------------------------------
    // L'espressione 'if (showResetDialog)' agisce come un interruttore di rendering.
    // In Compose, se questa condizione è falsa, il codice al suo interno non viene
    // ignorato, ma non viene proprio disegnato sullo schermo.
    if (showResetDialog) {

        // AlertDialog è il componente standard Material 3 per creare popup modali.
        // Un popup modale blocca tutte le interazioni con la schermata sottostante.
        AlertDialog(
            // onDismissRequest è un evento che viene scatenato da Android quando l'utente
            // clicca lo sfondo scuro fuori dal popup, oppure preme il tasto indietro fisico.
            onDismissRequest = {
                showResetDialog = false
            },

            // icon posiziona un'immagine in alto al centro del popup.
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning, // L'icona vettoriale predefinita di sistema
                    contentDescription = "Attenzione", // Testo invisibile letto dagli screen reader per ciechi
                    // tint colora l'icona. Usiamo MaterialTheme.colorScheme.error per
                    // estrarre il colore rosso dinamico previsto dal tema del telefono.
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp),
                )
            },

            // title è l'intestazione in grassetto.
            title = {
                Text(
                    text = "Sei proprio sicuro?",
                    fontWeight = FontWeight.Bold
                )
            },

            // text è il corpo del messaggio.
            text = {
                Text(
                    text = "Sei sicurissimo di voler azzerare tutto?\nQuesta azione non può essere annullata."
                )
            },

            // confirmButton è l'area in basso a destra destinata ai pulsanti.
            // Per aggirare il suo allineamento nativo a destra, passiamo come parametro
            // una Row (Riga) che prenderà il controllo totale dello spazio.
            confirmButton = {
                Row(
                    // Modifier.fillMaxWidth() impone alla riga di allargarsi orizzontalmente al 100%.
                    modifier = Modifier.fillMaxWidth(),
                    // horizontalArrangement definisce la gestione dello spazio vuoto.
                    // spacedBy(12.dp) inietta esattamente 12 pixel di spazio rigido e non
                    // comprimibile tra i pulsanti che inseriremo all'interno della riga.
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // OutlinedButton è un pulsante trasparente delineato solo da un bordo.
                    // Visivamente indica un'azione secondaria (di fuga o annullamento).
                    OutlinedButton(
                        // onClick è una lambda (una funzione): definisce il codice che
                        // verrà eseguito nel millisecondo esatto in cui il pulsante viene premuto.
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showResetDialog = false
                        },
                        // I Modifiers in Compose sono a cascata e definiscono forma e spazio.
                        modifier = Modifier
                            // weight(1f) è una proporzione. Essendoci due bottoni con weight(1f),
                            // Compose calcola lo spazio totale, toglie i 12dp centrali,
                            // e divide il resto esattamente in due metà uguali (50% e 50%).
                            .weight(1f)
                            // height(48.dp) forza l'altezza verticale a 48 pixel (standard accessibilità touch).
                            .height(48.dp),
                        // shape definisce i contorni. RoundedCornerShape smussa gli angoli.
                        // 20.dp è un raggio molto alto, creando il tipico effetto a "pillola".
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(text = "Annulla")
                    }

                    // Button è il pulsante pieno (Filled) standard. Indica l'azione primaria.
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // viewModel è l'oggetto che contiene le logiche di calcolo.
                            // Invochiamo la sua funzione per manipolare la lista punti in background.
                            viewModel.resetScoresWithUndo()
                            showResetDialog = false
                        },
                        // Riapplichiamo l'esatto stesso Modifier del pulsante precedente
                        // per garantire la perfetta simmetria geometrica.
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // colors permette di sovrascrivere lo schema colori predefinito del pulsante.
                        colors = ButtonDefaults.buttonColors(
                            // containerColor stabilisce il colore di riempimento.
                            // Applichiamo la semantica 'error' (rosso) per indicare azioni irreversibili.
                            containerColor = MaterialTheme.colorScheme.error,
                            // contentColor stabilisce il colore di ciò che sta dentro il bottone (es: il testo).
                            // 'onError' è un colore generato da sistema per essere sempre leggibile sul rosso.
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            text = "Sì, azzera",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    // --------------------------------------------------------------------
    // DIALOGO SALVA-VITA: USCITA ACCIDENTALE
    // --------------------------------------------------------------------
    if (showExitWarning) {
        AlertDialog(
            onDismissRequest = { showExitWarning = false },

            // L'icona in alto al centro cattura l'attenzione e contestualizza l'azione.
            icon = {
                Icon(
                    // ExitToApp è l'icona standard Material per l'abbandono di una schermata o app.
                    // Usiamo AutoMirrored per garantire il corretto orientamento in lingue RTL (Right-to-Left).
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Attenzione Uscita",
                    // Essendo un'azione potenzialmente distruttiva, applichiamo il colore di errore.
                    tint = MaterialTheme.colorScheme.error,
                    // Ingrandiamo l'icona per darle maggiore peso visivo, come fatto nei dialoghi precedenti.
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Abbandonare la partita?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Se torni alla Home, i progressi attuali andranno persi per sempre. Sei sicuro di voler uscire?")
            },
            // Replicando l'architettura del popup precedente, creiamo coerenza per l'utente.
            // Imparerà che due bottoni larghi colorati in un certo modo significano sempre la stessa cosa.
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExitWarning = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Annulla")
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExitWarning = false
                            // onNavigateHome è una lambda passata dal livello superiore dell'app.
                            // Serve per istruire il "NavHost" a distruggere questa schermata e mostrare la Home.
                            onNavigateHome()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            text = "Sì, esci",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }

    // ---> POPUP DEL DADO VIRTUALE <---
    if (showDiceDialog) {
        AlertDialog(
            onDismissRequest = { showDiceDialog = false },
            title = { Text("Lancio del Dado") },
            text = {
                Text(
                    text = "🎲 $diceResult",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                // Tasto per rullare di nuovo
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // ---> FIX: Usiamo la variabile del ViewModel invece del numero fisso 6! <---
                    diceResult = (1..viewModel.diceSides).random()
                }) { Text("Tira di nuovo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDiceDialog = false
                }) { Text("Chiudi") }
            }
        )
    }

    // POPUP TUTORIAL: SPIEGAZIONE DELLE MECCANICHE NASCOSTE DELL'APP
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = {
                showInfoDialog = false
            }, // Se l'utente clicca fuori dal popup, si chiude
            title = { Text("Info funzionalità", fontWeight = FontWeight.Bold) },
            text = {
                // verticalScroll permette di scorrere il testo col dito se lo schermo del telefono è troppo piccolo
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    Text(
                        "🚀 Punteggio Rapido",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 8.dp)
                    )
                    Text(
                        "Tieni premuto il tasto '+' per aggiungere 10 punti o il tasto '-' per toglierne 5 istantaneamente.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "🔥 Stato On Fire",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                    )
                    Text(
                        "Se un giocatore segna 3 volte di fila senza interruzioni da parte degli altri, il suo punteggio diventa arancione. \nNon si applica nelle sfide a carte",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "⌨️ Modifica Manuale",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                    )
                    Text(
                        "Clicca direttamente sul numero del punteggio per aprire la tastiera e inserire un valore preciso a piacere.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "🎲 Dado Fortunato",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp, top = 16.dp)
                    )
                    Text(
                        "Usa il tasto 'Lancia Dado' per decidere chi inizia tra le dispute con amici",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfoDialog = false // Chiude il popup quando si preme il bottone
                }) { Text("Ho capito") }
            }
        )
    }
}



