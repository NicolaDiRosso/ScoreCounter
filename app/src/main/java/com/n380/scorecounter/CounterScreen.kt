package com.n380.scorecounter

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        bottomBar = {
            Row(
                // Modella la posizione: solleviamo la riga dal bordo inferiore (bottom=32.dp) e le diamo margini laterali.
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                // Modella lo spazio vuoto in mezzo ai due bottoni (16dp)
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ---> Pulsante AZZERA <---
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showResetDialog = true
                    },
                    // weight(1f) divide lo schermo esattamente a metà tra i due bottoni (simmetria perfetta).
                    // height(56.dp) forza l'altezza ad essere identica a quella dei bottoni FAB fluttuanti.
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Azzera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ---> Pulsante FINE MATCH <---
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Blocchiamo il cronometro un attimo prima di cambiare pagina!
                        viewModel.pauseTimer()
                        // L'animazione esplosiva scatta istantaneamente non appena si apre la nuova pagina dei risultati.
                        onNavigateToResults()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Fine Match",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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

            // LazyColumn: La "lista intelligente" che renderizza graficamente solo i giocatori attualmente visibili sullo schermo
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // items() scorre la lista dei giocatori e per ognuno richiama la nostra funzione grafica "PlayerScoreCard"
                items(viewModel.players) { p ->

                    // Capiamo se questo specifico giocatore merita la corona (Deve avere il maxScore e almeno 1 punto in attivo)
                    val isLeader = p.score == maxScore && maxScore > 0

                    // ---> MODIFICA COMBO: Passiamo la logica aggiornata alla Carta <---
                    PlayerScoreCard(
                        player = p,
                        isLeader = isLeader,
                        isFireEnabled = isFireEnabled, // Passiamo il verdetto del filtro anti-carte
                        onScoreChange = { amount ->
                            viewModel.updatePlayerScore(
                                p,
                                amount
                            )
                        }, // Usiamo la logica centralizzata del ViewModel per le combo!
                        onScoreClick = { playerForManualEdit = p }
                    )
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


/**
 * COMPONENTE: Card personalizzata per la singola riga del giocatore nella fase di punteggio.
 * ---> MODIFICA UI: Stile "Gamepad", Numeri Giganti, Pulsanti Tattili, Bordi Marcati e FIX anti-schiacciamento (Ellipsis) <---
 * ---> NUOVA MODIFICA IDENTITY VISUAL: Sfondo Sfumato (Tonal Surface) per un'estetica Material 3 più pulita e leggibile <---
 * * @OptIn(ExperimentalFoundationApi::class) serve perché stiamo usando 'combinedClickable',
 * una funzione avanzata di Compose che gestisce sia il tocco normale che la pressione lunga.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScoreCard(
    player: Player,
    isLeader: Boolean = false,
    // ---> NUOVI PARAMETRI PER LA COMBO ON FIRE <---
    isFireEnabled: Boolean = true, // Se falso (Sfida Carte), blocca il colore arancione
    onScoreChange: (Int) -> Unit,  // Il "Tubo" che invia l'azione (+1, -1, ecc.) al ViewModel per fargli contare la combo
    onScoreClick: () -> Unit
) {
    // MOTORE APTICO: Prepariamo il sistema di vibrazione del telefono per il feedback tattile
    val haptic = LocalHapticFeedback.current

    // ---> ESTRAZIONE DEL COLORE <---
    // Trasformiamo il numero intero salvato nel database in un oggetto Colore utilizzabile dalla grafica
    val playerColor = Color(player.color)

    // ---> IL CONTENITORE PRINCIPALE (La riga del giocatore) <---
    // Card è un contenitore bellissimo del Material Design (sfondo leggero, bordi arrotondati e una leggera ombra invisibile)
    Card(
        // fillMaxWidth() gli fa occupare tutta la larghezza dello schermo.
        // padding(vertical = 4.dp) aggiunge una piccola spaziatura tra un giocatore e l'altro per non appiccicarli.
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        // ---> LA TUA IDEA UX (TONAL SURFACE) <---
        // Usiamo il colore del giocatore ma con opacità al 15% (0.15f).
        // Questo crea un elegantissimo "bagliore" di fondo senza accecare l'utente.
        colors = CardDefaults.cardColors(containerColor = playerColor.copy(alpha = 0.7f)),
        // Sostituiamo il vecchio bordo marcato con uno leggerissimo (10% di opacità) per dare solo un lieve senso di profondità
        border = BorderStroke(1.dp, playerColor.copy(alpha = 0.1f)),
        // Arrotondiamo pesantemente gli angoli (24.dp) per un look moderno in stile Material 3
        shape = RoundedCornerShape(24.dp)
    ) {
        // Row allinea gli elementi in orizzontale.
        // Arrangement.SpaceBetween spinge il Nome tutto a sinistra e i Pulsanti tutti a destra.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ==========================================================
            // BLOCCO SINISTRO: NOME DEL GIOCATORE E CORONA DEL LEADER
            // ==========================================================
            // Raggruppo Nome e Corona in una riga interna per farli stare assieme a sinistra.
            // weight(1f) è fondamentale qui: dice a questo blocco "prenditi tutto lo spazio vuoto che avanza".
            // Così facendo, spinge prepotentemente il blocco dei pulsanti (a destra) contro il bordo del telefono.
            // Aggiungiamo un padding 'end' per non far mai incollare il nome al bottone Meno nel caso di numeri giganti.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {

                // ---> IL NOME DEL GIOCATORE <---
                Text(
                    text = player.name,
                    // LEZIONE TIPOGRAFIA: 'headlineSmall' è uno stile di testo più grande e imponente del normale.
                    style = MaterialTheme.typography.headlineSmall,
                    // FontWeight.Bold forza la scritta in Grassetto per massimizzare la leggibilità durante il gioco.
                    fontWeight = FontWeight.Bold,
                    // ---> FIX LEGGIBILITÀ <---
                    // Avendo colorato lo sfondo (Tonal Surface), il testo torna ad essere 'onSurface' (Bianco o Nero puro a seconda del tema del telefono)
                    color = MaterialTheme.colorScheme.onSurface,
                    // ---> FIX ANTI-SCHIACCIAMENTO <---
                    // maxLines = 1: Forza il testo a rimanere sempre e solo su una singola riga, impedendo che la grafica si rompa in verticale.
                    maxLines = 1,
                    // overflow = TextOverflow.Ellipsis: Se il nome è troppo lungo e viene schiacciato dai numeri grandi, taglia le lettere finali e mette "..." (Es. "Dani...").
                    overflow = TextOverflow.Ellipsis,
                    // Diamo un 'weight' interno al testo per farlo restringere dolcemente se manca spazio, senza spingere fuori o schiacciare la corona del leader!
                    modifier = Modifier.weight(1f, fill = false)
                )

                // ---> DISEGNO DELLA CORONA DEL LEADER <---
                // (Solo per chi è in vantaggio)
                if (isLeader) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium, // L'icona a medaglia/stella molto in stile Material 3
                        contentDescription = "In Vantaggio",
                        // La corona è il nostro "Accento Grafico": la facciamo brillare del colore puro del giocatore!
                        tint = playerColor,
                        // Padding 'start' la stacca leggermente dal nome, 'size' la rende bella grande
                        modifier = Modifier.padding(start = 8.dp).size(28.dp)
                    )
                }
            }

            // ==========================================================
            // BLOCCO DESTRO: CONTROLLI STILE "GAMEPAD" E PUNTEGGIO
            // ==========================================================
            // Raggruppiamo i controlli matematici (Meno, Numero, Più) in un'altra mini-Row a destra
            Row(verticalAlignment = Alignment.CenterVertically) {

                // ---> PULSANTE MENO CON PUNTEGGIO RAPIDO (Long Press) <---
                // Usiamo un 'Box' vuoto che poi "mascheriamo" da pulsante hardware.
                Box(
                    modifier = Modifier
                        // Grandezza fissa di 56x56 pixel (Bersaglio touch molto grande per non mancarlo, Legge di Fitts)
                        .size(56.dp)
                        // LEZIONE FORME: Invece di un cerchio, usiamo un quadrato con angoli smussati a 16.dp.
                        // In UI Design questa forma si chiama "Squircle" e ricorda i tasti fisici dei joypad!
                        .clip(RoundedCornerShape(16.dp))
                        // Colore di sfondo tenue: usiamo 'errorContainer' (di solito un rosso slavato) con trasparenza al 70%
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                        // Disegniamo un bordino microscopico attorno al tasto per dargli un finto effetto 3D (Rilievo)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        // LEZIONE RIPPLE EFFECT: combinedClickable non solo gestisce i click, ma genera
                        // in automatico l'ombra grigia che si espande dal dito (L'onda tattile o Ripple Effect)!
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibra
                                // MODIFICA COMBO: Usiamo onScoreChange(-1) invece del diretto player.changeScore(-1).
                                // In questo modo avvisiamo il cervello dell'app (ViewModel) che deve spegnere il fuoco!
                                onScoreChange(-1)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Pressione Lunga: Toglie 5 punti in un colpo solo e resetta le combo!
                                onScoreChange(-5)
                            }
                        ),
                    contentAlignment = Alignment.Center // Centra l'icona "-" perfettamente in mezzo al Box
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Diminuisci",
                        tint = MaterialTheme.colorScheme.onErrorContainer, // Colore scuro per fare contrasto col rosso tenue
                        modifier = Modifier.size(32.dp) // Icona ingrandita per riempire bene il tasto hardware
                    )
                }

                // ---> PUNTEGGIO GIGANTE CLICCABILE <---
                Text(
                    text = player.score.toString(),
                    // LEZIONE TIPOGRAFIA: 'displayMedium' è uno degli stili più enormi di Android. Ideale per i numeri.
                    style = MaterialTheme.typography.displayMedium,
                    // FontWeight.Black è il livello massimo di grassetto esistente! Rende il font "cicciotto" e massiccio.
                    fontWeight = FontWeight.Black,
                    // ---> LOGICA COLORE PUNTEGGIO <---
                    // Se On Fire: Arancione scoppiettante. Altrimenti: colore neutro 'onSurface' per integrarsi con il nome.
                    color = if (player.isOnFire && isFireEnabled) Color(0xFFF3AF38) else MaterialTheme.colorScheme.onSurface,
                    // Mettiamo maxLines = 1 anche qui. Se il numero diventa assurdamente lungo (es. 10 milioni), non andrà a capo rompendo la card.
                    maxLines = 1,
                    modifier = Modifier
                        // MODIFICA ANTI-SCHIACCIAMENTO: Imposto un padding a 16.dp. per evitare che il numero risulti schiacciato tra i pulsanti
                        // In questo modo i numeri hanno molto più spazio fisico per crescere prima di dare fastidio al nome del giocatore a sinistra!
                        .padding(horizontal = 16.dp)
                        // Usiamo 'modifier' per dirgli che ora non è più solo un testo da leggere, ma un bottone da cliccare!
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onScoreClick() // Esegue il comando di apertura del popup tastiera passato dalla CounterScreen
                        }
                )

                // ---> PULSANTE PIÙ CON PUNTEGGIO RAPIDO (Long Press) <---
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        // Sfondo tenue: usiamo 'primaryContainer' (un azzurro chiaro) per indicare positività
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // MODIFICA COMBO: Inviamo +1 al ViewModel. Se succede 3 volte di fila, scatta la Combo!
                                onScoreChange(1)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Pressione lunga: Aggiunge 10 punti istantanei e conta per la Combo!
                                onScoreChange(10)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Aumenta",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}