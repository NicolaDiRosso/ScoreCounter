package com.n380.scorecounter.ui.components

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R

// ====================================================================================
// 🧠 COMPONENTE UI: DIALOGO INFORMAZIONI APP (ABOUT)
// ====================================================================================
/**
 * Componente scorporato dalla HomeScreen per alleggerire il Main File (Principio DRY e Clean Architecture).
 *
 * 🧠 TEORIA RESPONSIVE LANDSCAPE:
 * In modalità orizzontale (Landscape), la dimensione verticale dello schermo cala a ~360dp.
 * Per evitare che l'AlertDialog venga tagliato in alto e in basso, usiamo `LocalConfiguration`
 * per commutare tra il layout verticale standard (Portrait) ed un layout a 2 colonne affiancate (Landscape).
 *
 * @param onDismiss Lambda invocata quando l'utente richiede la chiusura del popup.
 */
@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // 🧠 TEORIA ORIENTAMENTO: Rilevamento in tempo reale dell'orientamento dello schermo
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // ====================================================================
        // 🖥️ LAYOUT ORIZZONTALE LANDSCAPE (A 2 COLONNE)
        // ====================================================================
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.92f) // Allarga il popup in orizzontale
                .heightIn(max = 320.dp), // Blocca l'altezza massima per non finire fuori dallo schermo
            properties = DialogProperties(usePlatformDefaultWidth = false),
            confirmButton = {}, // I pulsanti sono incorporati nella colonna destra del contenuto
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PANNELLO DI SINISTRA: TITOLO, DESCRIZIONE E LISTA FEATURE SCORREVOLE
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VideogameAsset,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Text(
                            text = stringResource(R.string.desc_informazioni_app),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = stringResource(R.string.sottotitolo_cosa_puoi_fare),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        val features = listOf(
                            stringResource(R.string.feature_registra_partite),
                            stringResource(R.string.feature_analizza_grafici),
                            stringResource(R.string.feature_assegna_titoli)
                        )
                        features.forEach { feature ->
                            Row(modifier = Modifier.padding(bottom = 2.dp)) {
                                Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(feature, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // PANNELLO DI DESTRA: CALLOUT E PULSANTI D'AZIONE
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(R.string.msg_vinca_il_migliore),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.msg_segnalazione_bug),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        val emailSubject = stringResource(R.string.oggetto_email_feedback)
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:scorecounter.feedback@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                                }
                                try { context.startActivity(emailIntent) } catch (e: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_invia_segnalazione),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            AutoResizedText(
                                text = stringResource(R.string.btn_inizia_a_giocare),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        )
    } else {
        // ====================================================================
        // 📱 LAYOUT VERTICALE PORTRAIT STANDARD
        // ====================================================================
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Filled.VideogameAsset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp) // Leggermente più piccola e raffinata
                )
            },
            title = {
                Text(
                    stringResource(R.string.app_name), // Nome dell'applicazione nel titolo del dialogo About
                    fontWeight = FontWeight.Black, // Più "pesante" per un look da titolo vero
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                // ==========================================================
                // 🧠 FIX TESTO TAGLIATO E FORMATTAZIONE
                // 1. Usiamo 'verticalScroll' per far scorrere il contenuto se lo schermo è piccolo.
                // 2. Dividiamo le frasi in componenti 'Text' separati per gestire spazi e stili.
                // ==========================================================
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        stringResource(R.string.desc_informazioni_app), // Descrizione lunga dell'applicazione
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify, // l'oggetto TextAlign con Justify ci permette di giustificare il testo
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Sottotitolo colorato
                    Text(
                        stringResource(R.string.sottotitolo_cosa_puoi_fare), // Intestazione sezione funzionalità
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 🧠 FORMATTAZIONE PRO: Creiamo la lista puntata graficamente
                    val features = listOf(
                        stringResource(R.string.feature_registra_partite), // Feature 1: Registrazione partite
                        stringResource(R.string.feature_analizza_grafici), // Feature 2: Analisi grafici
                        stringResource(R.string.feature_assegna_titoli) // Feature 3: Assegnazione titoli
                    )
                    features.forEach { feature ->
                        Row(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                "• ",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Justify
                            )
                            Text(
                                feature,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Intestazione sezione "C'è qualcosa che non funziona?" evidenziata
                    Text(
                        stringResource(R.string.msg_vinca_il_migliore), // Intestazione sezione bug report
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        stringResource(R.string.msg_segnalazione_bug), // Istruzioni per feedback e bug report
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            // ==========================================================
            // SCOMPARTIMENTO FISSO IN BASSO (confirmButton)
            // ==========================================================
            confirmButton = {
                // IL CONTENITORE: Impila i bottoni uno sopra l'altro
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // 🧠 SPAZIATURA VERTICALE: Applichiamo Arrangement.spacedBy(12.dp)
                    // per distanziare i bottoni in modo armonioso e coerente con il DonationDialog,
                    // evitando che risultino "attaccati" l'uno all'altro.
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. PRIMO BOTTONE (Azione Secondaria: Email)

                    // Estraiamo la stringa QUI, nel contesto del Composable (@Composable scope),
                    // utilizzando la funzione nativa 'stringResource'. In questo modo Compose
                    // mantiene l'osservabilità sulla lingua del dispositivo.
                    val emailSubject = stringResource(R.string.oggetto_email_feedback)
                    // COMPONENTE: OutlinedButton per il contatto
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            // DEFINIZIONE DELL'INTENT
                            // Ora passiamo semplicemente la variabile 'emailSubject' che abbiamo pre-calcolato fuori.
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:scorecounter.feedback@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                            }

                            // Metodo della Classe Context: Avvia l'applicazione esterna
                            try {
                                context.startActivity(emailIntent)
                            } catch (e: Exception) {
                                // Gestione dell'eccezione nel caso non esistano app email installate
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        AutoResizedText(stringResource(
                            R.string.btn_invia_segnalazione),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    // 2. SECONDO BOTTONE (Azione Primaria: Gioca)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onDismiss() // Invochiamo la lambda del genitore per chiudere
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        AutoResizedText(
                            stringResource(R.string.btn_inizia_a_giocare),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        )
    }
}


// ====================================================================================
// 🧠 COMPONENTE UI: DIALOGO DONAZIONI E SUPPORTO (DONATION)
// ====================================================================================
/**
 * Componente isolato che ospita il messaggio personale del programmatore.
 *
 * 🧠 TEORIA RESPONSIVE LANDSCAPE:
 * In orizzontale, la finestra di dialogo viene formattata a 2 colonne (Row):
 * - Sinistra: Saluto e messaggio personale di Nicola (scorrevole).
 * - Destra: Box evidenziato Ad-Free, Bottone PayPal e Bottone di chiusura cortese.
 *
 * @param onDismiss Lambda di callback per il Sollevamento dello Stato (State Hoisting).
 */
@Composable
fun DonationDialog(onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    // 🧠 TEORIA ORIENTAMENTO: Rilevamento in tempo reale dell'orientamento dello schermo
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // ====================================================================
        // 🖥️ LAYOUT ORIZZONTALE LANDSCAPE (A 2 COLONNE)
        // ====================================================================
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 320.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            confirmButton = {},
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PANNELLO DI SINISTRA: SALUTO E TESTO DEL MESSAGGIO (SCORREVOLE)
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(28.dp)
                            )
                            AutoResizedText(
                                text = stringResource(R.string.titolo_donazione),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Text(
                            text = stringResource(R.string.msg_donazione_saluto),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = stringResource(R.string.msg_donazione_intro),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = stringResource(R.string.msg_donazione_scopo),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = stringResource(R.string.msg_donazione_grazie),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Justify,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // PANNELLO DI DESTRA: BOX PROMESSA AD-FREE E PULSANTI DONAZIONE
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.msg_donazione_promessa),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        val context = LocalContext.current
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                val paypalUrl = "https://www.paypal.com/donate/?hosted_button_id=XJF97D5YQ5SXE"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_paypal_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(Modifier.width(6.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.btn_donazione_paypal),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.btn_donazione_chiudi),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        )
    } else {
        // ====================================================================
        // 📱 LAYOUT VERTICALE PORTRAIT STANDARD
        // ====================================================================
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    // Sovrascrittura cromatica assoluta: forziamo un Rosa/Rosso vibrante
                    // a prescindere dal tema (Light/Dark) per l'impatto emotivo dell'icona.
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                AutoResizedText(
                    text = stringResource(R.string.titolo_donazione), // I18n: "Sostieni ScoreCounter"
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                // 🧠 GESTIONE OVERFLOW VERTICALE:
                // Trattandosi di un testo emozionale e lungo, è matematicamente certo che
                // su display piccoli (o font ingranditi dall'utente) eccederà i confini dello schermo.
                // Il 'verticalScroll' garantisce che il bottone di conferma resti sempre raggiungibile.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    // Titolo di benvenuto testuale
                    Text(
                        text = stringResource(R.string.msg_donazione_saluto),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Corpo principale del messaggio
                    Text(
                        text = stringResource(R.string.msg_donazione_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        // 'TextAlign.Justify' allinea il testo ai margini, tipico dei testi di lettura
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 🧠 ACCENT BOX (Focus Visivo):
                    // Creiamo una sub-Surface per "incorniciare" la tua promessa più importante
                    // (l'assenza di pubblicità). L'utente capta subito questo blocco staccato dal testo.
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.msg_donazione_promessa),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Secondo blocco di testo: Scopo dei fondi
                    Text(
                        text = stringResource(R.string.msg_donazione_scopo),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Conclusione e ringraziamenti finali
                    Text(
                        text = stringResource(R.string.msg_donazione_grazie),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify,
                        fontWeight = FontWeight.Medium // Leggermente più spesso per chiudere con impatto
                    )
                }
            },
            confirmButton = {
                // ACCESSO AL CONTESTO PER INTENT DI RETE
                // 'LocalContext' ci serve per poter "chiedere" ad Android di lanciare un'app esterna (il browser o l'app PayPal)
                val context = LocalContext.current

                // STACK VERTICALE DEI BOTTONI (Colonna)
                // Sostituiamo il singolo bottone con una Column per poterne impilare due:
                // 1. Bottone primario (colorato) per la donazione.
                // 2. Bottone secondario (solo contorno o trasparente) per la chiusura cortese.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Spaziatura elegante tra i due bottoni
                ) {

                    // 1. BOTTONE PRIMARIO: DONAZIONE PAYPAL
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)

                            // 🧠 DEFINIZIONE DELL'INTENT (Navigazione Esterna)
                            // Creiamo un 'Intent.ACTION_VIEW' passandogli un Uri web.
                            // Questo dice ad Android: "Cerca un'app in grado di aprire questo indirizzo web".
                            val paypalUrl = "https://www.paypal.com/donate/?hosted_button_id=XJF97D5YQ5SXE"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Blocco catch di sicurezza: previene crash rarissimi in cui il telefono
                                // non possiede nemmeno un browser installato (improbabile, ma best practice).
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(20.dp)),//Forziamo il ritaglio della maschera visiva (clip)
                        // con la stessa forma geometrica (RoundedCornerShape) passata al parametro 'shape'.
                        // Questo impedisce a qualsiasi elemento interno (Row o PNG) di sbordare o alterare la curvatura.
                        shape = RoundedCornerShape(20.dp),
                        // Usiamo un colore diverso (es. secondario) per dare risalto all'azione di donazione?
                        // Oppure lasciamo il primary di default rimuovendo 'colors'.
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        // ALLINEAMENTO ORIZZONTALE E VERTICALE BILANCIATO
                        // Il corpo del bottone in Compose è implicitamente una Row.
                        // Per far sì che l'icona PNG e il testo AutoResizedText viaggino sulla stessa linea
                        // baricentrica senza oscillazioni dovute ai margini del file PNG,
                        // l'icona viene dimensionata con cura e privata di filtri cromatici.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // 🧠 TEORIA DEL RENDERING DELLE IMMAGINI IN COMPOSE:
                            // Per evitare che Android colori tutto il logo di bianco o di nero distruggendolo,
                            // impostiamo il parametro 'tint = Color.Unspecified'.
                            // Questo dice al motore grafico Skia di Android: "Disegna il file PNG esattamente con i suoi colori originali".
                            Icon(
                                painter = painterResource(id = R.drawable.ic_paypal_logo), // Nome del tuo file PNG dentro la cartella res/drawable
                                contentDescription = null,
                                modifier = Modifier.size(34.dp), // Dimensione standard bilanciata per i touch target delle icone nei bottoni
                                tint = Color.Unspecified // 👈 FONDAMENTALE: Mantiene i colori blu/azzurro nativi del PNG di PayPal
                            )

                            // Spaziatore fisso a 12.dp per distaccare l'immagine dal testo dell'ascoltatore,
                            // evitando che i due elementi collidano visivamente.
                            Spacer(Modifier.width(6.dp))

                            AutoResizedText(
                                text = stringResource(R.string.btn_donazione_paypal), // Es. "Dona con PayPal"
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. BOTTONE SECONDARIO: CHIUSURA CORTESE ("Grazie Nicola!")
                    OutlinedButton(
                        onClick = {
                            // Feedback aptico differenziato (LongPress) per denotare un'azione secondaria/uscita
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss() // Invoca la lambda per distruggere il dialogo
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        // Creiamo un bordo sottile per renderlo un bottone secondario meno invasivo
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.btn_donazione_chiudi), // Es. "Grazie Nicola!"
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )
    }
}
