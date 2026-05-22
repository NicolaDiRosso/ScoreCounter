    package com.n380.scorecounter.ui.components

    import com.n380.scorecounter.ui.components.AboutAppDialog
    import com.n380.scorecounter.ui.components.DonationDialog
    import android.content.Intent
    import android.net.Uri
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.background
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
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.hapticfeedback.HapticFeedbackType
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.LocalHapticFeedback
    import androidx.compose.ui.res.stringResource
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import com.n380.scorecounter.R

    // ====================================================================================
    // 🧠 COMPONENTE UI: DIALOGO INFORMAZIONI APP (ABOUT)
    // ====================================================================================
    /**
     * Componente scorporato dalla HomeScreen per alleggerire il Main File (Principio DRY e Clean Architecture).
     *
     * @param onDismiss Lambda invocata quando l'utente richiede la chiusura del popup.
     * Segue il pattern dello "State Hoisting": il componente non modifica il proprio stato
     * di visibilità, ma delega l'azione al genitore (HomeScreen) che detiene la Single Source of Truth.
     */
    @Composable
    fun AboutAppDialog(onDismiss: () -> Unit) {
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current

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

                    Text(
                        stringResource(R.string.msg_vinca_il_migliore), // Messaggio di augurio "Vinca il migliore"
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                    horizontalAlignment = Alignment.CenterHorizontally
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
                                data = Uri.parse("mailto:emailditest100@gmail.com")
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
                        AutoResizedText(stringResource(R.string.btn_invia_segnalazione), style = MaterialTheme.typography.labelLarge) // Testo pulsante invio feedback
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
                        AutoResizedText(stringResource(R.string.btn_inizia_a_giocare), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge) // Testo pulsante per iniziare a giocare
                    }
                }
            }
        )
    }

    /**
     * Un componente Text personalizzato che riduce automaticamente la dimensione del font
     * per adattarsi allo spazio orizzontale disponibile, evitando troncamenti indesiderati.
     */
    /*
    @Composable
    fun AutoResizedText(
        text: String,
        style: TextStyle = MaterialTheme.typography.bodyMedium,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        fontWeight: FontWeight? = null,
        textAlign: TextAlign? = null,
    ) {
        var resizedTextStyle by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(style) }
        var shouldDraw by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        Text(
            text = text,
            color = color,
            modifier = modifier.androidx.compose.ui.draw.drawWithContent {
                if (shouldDraw) drawContent()
            },
            fontWeight = fontWeight,
            textAlign = textAlign,
            softWrap = false,
            style = resizedTextStyle,
            onTextLayout = { result ->
                if (result.didOverflowWidth) {
                    resizedTextStyle = resizedTextStyle.copy(
                        fontSize = resizedTextStyle.fontSize * 0.95
                    )
                } else {
                    shouldDraw = true
                }
            }
        )
    }*/

    // ====================================================================================
    // 🧠 COMPONENTE UI: DIALOGO DONAZIONI E SUPPORTO (CUORE)
    // ====================================================================================
    /**
     * Componente isolato che ospita il messaggio personale del programmatore.
     *
     * @param onDismiss Lambda di callback per il Sollevamento dello Stato (State Hoisting).
     */
    @Composable
    fun DonationDialog(onDismiss: () -> Unit) {
        val haptic = LocalHapticFeedback.current

        AlertDialog(
            // Dismissione standard tramite tap sullo scrim (sfondo scuro) o tasto "Back" nativo
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
                // Call to Action (CTA): Il bottone per chiudere o (in futuro) aprire PayPal
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        // IN FUTURO QUI POTREMO INSERIRE L'INTENT AL LINK DELLE DONAZIONI
                        onDismiss() // Chiudiamo il dialogo comunicandolo al genitore
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    AutoResizedText(
                        text = stringResource(R.string.btn_donazione_chiudi),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }