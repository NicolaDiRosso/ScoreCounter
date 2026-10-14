package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R
import com.n380.scorecounter.viewmodel.MatchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ====================================================================
 * COMPONENTE UI: DIALOGO BACKUP E RIPRISTINO DATI (JSON)
 * ====================================================================
 * 🧠 LEZIONE TEORICA E ARCHITETTURA ANDROID COMPOSE:
 * Questo file gestisce l'intero flusso di esportazione e importazione dei salvataggi dell'app.
 *
 * Concetti Chiave Spiegati nel Dettaglio:
 * 1. STORAGE ACCESS FRAMEWORK (SAF): Sistema di sicurezza nativo di Android che consente
 *    all'utente di salvare o leggere file in qualunque cartella del dispositivo (Download, Google Drive, ecc.)
 *    senza che l'app debba richiedere permessi speciali o invasivi di lettura memoria.
 * 2. rememberLauncherForActivityResult: Registro di Compose che ascolta l'apertura e il ritorno
 *    dalle schermate di sistema di selezione dei file (CreateDocument per creare, OpenDocument per aprire).
 * 3. State Hoisting (Sollevamento dello Stato): Il dialogo notifica alla schermata padre (HomeScreen)
 *    l'avvenuto successo tramite la lambda `onImportSuccess(String)`, che chiude i popup e mostra la Snackbar in Home.
 * 4. Responsive UI per Smartphone Landscape: Su schermi piccoli in orizzontale, i sotto-dialoghi
 *    utilizzano un layout a 2 colonne affiancate (Row) per evitare che i testi vengano tagliati dai pulsanti.
 *
 * @param viewModel Il cervello reattivo dell'applicazione contenente i dati e la logica di I/O.
 * @param onDismiss Lambda di callback per chiudere semplicemente il dialogo principali.
 * @param onImportSuccess Lambda invocata al completamento dell'importazione per notificare la Home e mostrare il banner.
 */
@Composable
fun BackupRestoreDialog(
    viewModel: MatchViewModel,
    onDismiss: () -> Unit,
    onImportSuccess: (String) -> Unit
) {
    // LocalContext.current fornisce l'accesso al contesto fisico di Android per avviare Activity o Toast
    val context = LocalContext.current

    // LocalHapticFeedback.current fornisce il motore delle vibrazioni del dispositivo per un feedback tattile fluido
    val haptic = LocalHapticFeedback.current

    // LocalConfiguration.current legge le informazioni hardware dello schermo in tempo reale (orientamento, altezza, larghezza)
    val configuration = LocalConfiguration.current

    // Identifichiamo se lo schermo è in orientamento Orizzontale (Landscape)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Identifichiamo se siamo su uno Smartphone in Landscape (altezza ristretta < 500dp), dove serve il layout a 2 colonne
    val isSmallScreenLandscape = isLandscape && configuration.screenHeightDp < 500

    // --------------------------------------------------------------------
    // STATI REATTIVI LOCALI PER IL FLUSSO DEI SOTTO-DIALOGHI
    // --------------------------------------------------------------------
    // `pendingImportUri`: Conserva l'URI del file selezionato dall'utente fino a quando non sceglie la modalità di ripristino
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    // `showReplaceWarningUri`: Conserva l'URI se l'utente sceglie di sovrascrivere i dati e l'app contiene già delle partite
    var showReplaceWarningUri by remember { mutableStateOf<Uri?>(null) }

    // `showIncompatibleFileError`: Flag booleano attivato se il file selezionato dall'utente non è un JSON valido di ScoreCounter
    var showIncompatibleFileError by remember { mutableStateOf(false) }

    // Stringhe di notifica pre-calcolate nel contesto Composable per la formattazione dei messaggi
    val msgExportOk = stringResource(R.string.msg_backup_esportato_ok)
    val msgImportOkTemplate = stringResource(R.string.msg_backup_importato_ok)
    val msgErrorTemplate = stringResource(R.string.msg_backup_errore)

    // --------------------------------------------------------------------
    // 🧠 LAUNCHER DI SISTEMA PER L'ESPORTAZIONE DEL FILE (CreateDocument)
    // --------------------------------------------------------------------
    // `ActivityResultContracts.CreateDocument("application/json")` chiede ad Android di aprire
    // il gestore file nativo con il tasto "Salva". Restituisce l'URI del nuovo file creato.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        // Se l'utente non ha annullato l'operazione ed ha scelto dove salvare il file
        if (uri != null) {
            viewModel.exportBackupToUri(
                context = context,
                uri = uri,
                onSuccess = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    Toast.makeText(context, msgExportOk, Toast.LENGTH_LONG).show()
                },
                onError = { errorMsg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, String.format(msgErrorTemplate, errorMsg), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // --------------------------------------------------------------------
    // 🧠 LAUNCHER DI SISTEMA PER L'IMPORTAZIONE DEL FILE (OpenDocument)
    // --------------------------------------------------------------------
    // `ActivityResultContracts.OpenDocument()` apre il selettore file nativo di Android per scegliere un file esistente.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        // Se l'utente ha selezionato un file, salviamo il suo indirizzo URI nello stato per aprire la schermata di scelta
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    // ====================================================================
    // RENDER DEL DIALOGO PRINCIPALE (In base all'orientamento dello schermo)
    // ====================================================================
    if (isLandscape) {
        BackupRestoreDialogLandscape(
            onExportClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("ScoreCounter_Backup_$timeStamp.json")
            },
            onImportClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = onDismiss
        )
    } else {
        BackupRestoreDialogPortrait(
            onExportClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("ScoreCounter_Backup_$timeStamp.json")
            },
            onImportClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = onDismiss
        )
    }

    // ====================================================================
    // SOTTO-DIALOGO 1: SCELTA MODALITÀ DI RIPRISTINO (Unisci vs Sostituisci vs Annulla)
    // ====================================================================
    pendingImportUri?.let { uri ->
        if (isSmallScreenLandscape) {
            // --- VARIANTE PER SMARTPHONE IN ORIZZONTALE (A 2 Colonne Affiancate) ---
            Dialog(
                onDismissRequest = { pendingImportUri = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .heightIn(max = 280.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // COLONNA SINISTRA: Icona, Titolo "Modalità di ripristino" e Descrizione Dettagliata
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = stringResource(R.string.titolo_conferma_import),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.desc_dettagliata_import),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // COLONNA DESTRA: I 3 Pulsanti di Azione
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Tasto 1 (Primario Solido): Unisci ai dati attuali
                            Button(
                                onClick = {
                                    val importUri = uri
                                    pendingImportUri = null
                                    viewModel.importBackupFromUri(
                                        context = context,
                                        uri = importUri,
                                        isMerge = true,
                                        onSuccess = { count ->
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            onImportSuccess(String.format(msgImportOkTemplate, count))
                                        },
                                        onError = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showIncompatibleFileError = true
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                AutoResizedText(
                                    text = stringResource(R.string.btn_unisci_dati),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            // Tasto 2 (Bordo Rosso Error): Sostituisci tutti i dati
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (viewModel.hasSavedData) {
                                        showReplaceWarningUri = uri
                                        pendingImportUri = null
                                    } else {
                                        val importUri = uri
                                        pendingImportUri = null
                                        viewModel.importBackupFromUri(
                                            context = context,
                                            uri = importUri,
                                            isMerge = false,
                                            onSuccess = { count ->
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                onImportSuccess(String.format(msgImportOkTemplate, count))
                                            },
                                            onError = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showIncompatibleFileError = true
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                AutoResizedText(
                                    text = stringResource(R.string.btn_sostituisci_dati),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            // Tasto 3 (Neutro Contornato): Annulla
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pendingImportUri = null
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                AutoResizedText(
                                    text = stringResource(R.string.btn_annulla),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- VARIANTE STANDARD PER PORTRAIT E TABLET (AlertDialog Centrato) ---
            AlertDialog(
                onDismissRequest = { pendingImportUri = null },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.titolo_conferma_import),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.desc_dettagliata_import),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val importUri = uri
                                pendingImportUri = null
                                viewModel.importBackupFromUri(
                                    context = context,
                                    uri = importUri,
                                    isMerge = true,
                                    onSuccess = { count ->
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        onImportSuccess(String.format(msgImportOkTemplate, count))
                                    },
                                    onError = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showIncompatibleFileError = true
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.btn_unisci_dati),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (viewModel.hasSavedData) {
                                    showReplaceWarningUri = uri
                                    pendingImportUri = null
                                } else {
                                    val importUri = uri
                                    pendingImportUri = null
                                    viewModel.importBackupFromUri(
                                        context = context,
                                        uri = importUri,
                                        isMerge = false,
                                        onSuccess = { count ->
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            onImportSuccess(String.format(msgImportOkTemplate, count))
                                        },
                                        onError = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showIncompatibleFileError = true
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.btn_sostituisci_dati),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pendingImportUri = null
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            AutoResizedText(
                                text = stringResource(R.string.btn_annulla),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    }

    // ====================================================================
    // SOTTO-DIALOGO 2: ULTIMO AVVISO PRIMA DELLA SOVRASCRITTURA DATI
    // ====================================================================
    showReplaceWarningUri?.let { uri ->
        if (isSmallScreenLandscape) {
            Dialog(
                onDismissRequest = {
                    pendingImportUri = uri
                    showReplaceWarningUri = null
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.88f).heightIn(max = 260.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                                Text(stringResource(R.string.titolo_warning_sostituisci), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(stringResource(R.string.msg_warning_sostituisci), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    val importUri = uri
                                    showReplaceWarningUri = null
                                    viewModel.importBackupFromUri(
                                        context = context,
                                        uri = importUri,
                                        isMerge = false,
                                        onSuccess = { count ->
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            onImportSuccess(String.format(msgImportOkTemplate, count))
                                        },
                                        onError = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showIncompatibleFileError = true
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                AutoResizedText(stringResource(R.string.btn_procedi_sostituisci), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pendingImportUri = uri
                                    showReplaceWarningUri = null
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                AutoResizedText(stringResource(R.string.btn_annulla), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = {
                    pendingImportUri = uri
                    showReplaceWarningUri = null
                },
                icon = {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(38.dp))
                },
                title = {
                    Text(text = stringResource(R.string.titolo_warning_sostituisci), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Text(text = stringResource(R.string.msg_warning_sostituisci), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val importUri = uri
                                showReplaceWarningUri = null
                                viewModel.importBackupFromUri(
                                    context = context,
                                    uri = importUri,
                                    isMerge = false,
                                    onSuccess = { count ->
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        onImportSuccess(String.format(msgImportOkTemplate, count))
                                    },
                                    onError = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showIncompatibleFileError = true
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            AutoResizedText(text = stringResource(R.string.btn_procedi_sostituisci), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pendingImportUri = uri
                                showReplaceWarningUri = null
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            AutoResizedText(text = stringResource(R.string.btn_annulla), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        }
    }

    // ====================================================================
    // SOTTO-DIALOGO 3: AVVISO FILE CORROTTO / NON COMPATIBILE
    // ====================================================================
    if (showIncompatibleFileError) {
        AlertDialog(
            onDismissRequest = { showIncompatibleFileError = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(38.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.titolo_errore_import),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.msg_errore_import_file),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        showIncompatibleFileError = false
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AutoResizedText(
                        text = stringResource(R.string.btn_ho_capito),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

/**
 * LAYOUT VERTICALE (Portrait) PER IL DIALOGO PRINCIPALE
 */
@Composable
private fun BackupRestoreDialogPortrait(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // INTESTAZIONE (Header)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.titolo_backup_dialog),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.desc_backup_dialog),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // CORPO SCORREVOLE IN SCHEDE
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Scheda 1: ESPORTAZIONE
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.titolo_esporta_backup),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = stringResource(R.string.msg_esporta_backup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(
                                    onClick = onExportClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Filled.Upload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    AutoResizedText(
                                        text = stringResource(R.string.btn_esporta_file),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Scheda 2: IMPORTAZIONE
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.titolo_importa_backup),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = stringResource(R.string.msg_importa_backup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedButton(
                                    onClick = onImportClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    AutoResizedText(
                                        text = stringResource(R.string.btn_importa_file),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Scheda 3: PRIVACY & FORMATO APERTO
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.titolo_privacy_backup),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = stringResource(R.string.msg_privacy_backup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TASTO DI CHIUSURA ANCORATO
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    AutoResizedText(
                        text = stringResource(R.string.btn_chiudi_statistiche),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * LAYOUT ORIZZONTALE (Landscape Split-Screen) PER IL DIALOGO PRINCIPALE
 */
@Composable
private fun BackupRestoreDialogLandscape(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PANNELLO SINISTRO (~28%): Intestazione + Tasto ancorato in basso
                Column(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(44.dp)
                                .padding(bottom = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.titolo_backup_dialog),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.desc_backup_dialog),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        AutoResizedText(
                            text = stringResource(R.string.btn_chiudi_statistiche),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // PANNELLO DESTRO (~72%): Schede Esportazione / Importazione
                Surface(
                    modifier = Modifier
                        .weight(0.72f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Scheda 1: ESPORTAZIONE
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = stringResource(R.string.titolo_esporta_backup),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.msg_esporta_backup),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = onExportClick,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Filled.Upload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_esporta_file),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        // Scheda 2: IMPORTAZIONE
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = stringResource(R.string.titolo_importa_backup),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.msg_importa_backup),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = onImportClick,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_importa_file),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        // Scheda 3: PRIVACY
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(R.string.titolo_privacy_backup),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.msg_privacy_backup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}