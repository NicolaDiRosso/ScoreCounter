package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * STATO DELLA NAVIGAZIONE INTERNA AL DIALOGO DI BACKUP
 * ====================================================================
 * - MAIN: Vista principale con le schede di esportazione, importazione e privacy.
 * - EXPORT_CHOICE: Sotto-vista animata per la scelta tra "Salva sul dispositivo" e "Condividi file".
 */
private enum class ExportScreen { MAIN, EXPORT_CHOICE }

/**
 * ====================================================================
 * COMPONENTE UI: DIALOGO BACKUP E RIPRISTINO DATI (JSON)
 * ====================================================================
 * Gestisce l'intero flusso di esportazione e importazione dei salvataggi dell'app
 * con navigazione interna a scorrimento orizzontale (AnimatedContent) e altezza dinamica.
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

    // `showExportChoiceDialog`: Flag booleano per aprire il sotto-dialogo di scelta tra "Salva sul dispositivo" e "Condividi file"
    var showExportChoiceDialog by remember { mutableStateOf(false) }

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

    // RENDER PRINCIPALE IN BASE ALL'ORIENTAMENTO
    if (isLandscape) {
        BackupRestoreDialogLandscape(
            onExportSaveClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("ScoreCounter_Backup_$timeStamp.json")
            },
            onExportShareClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFileName = "ScoreCounter_Backup_$timeStamp.json"
                val jsonString = viewModel.generateBackupJsonString()
                shareBackupJsonFile(context, jsonString, backupFileName)
            },
            onImportClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = onDismiss
        )
    } else {
        BackupRestoreDialogPortrait(
            onExportSaveClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("ScoreCounter_Backup_$timeStamp.json")
            },
            onExportShareClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFileName = "ScoreCounter_Backup_$timeStamp.json"
                val jsonString = viewModel.generateBackupJsonString()
                shareBackupJsonFile(context, jsonString, backupFileName)
            },
            onImportClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = onDismiss
        )
    }

    // SOTTO-DIALOGO 1: SCELTA MODALITÀ DI RIPRISTINO (Unisci vs Sostituisci)
    pendingImportUri?.let { uri ->
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
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        AutoResizedText(text = stringResource(R.string.btn_unisci_dati), fontWeight = FontWeight.Bold)
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
                        AutoResizedText(text = stringResource(R.string.btn_sostituisci_dati), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
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
                        AutoResizedText(text = stringResource(R.string.btn_annulla), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // SOTTO-DIALOGO 2: AVVISO SOSTITUZIONE DATI ESISTENTI
    showReplaceWarningUri?.let { uri ->
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
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // SOTTO-DIALOGO 3: AVVISO FILE CORROTTO / NON COMPATIBILE
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
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/**
 * LAYOUT VERTICALE (Portrait) PER IL DIALOGO PRINCIPALE
 * Con navigazione interna animata (AnimatedContent) tra vista principale e sotto-menu di esportazione.
 */
@Composable
private fun BackupRestoreDialogPortrait(
    onExportSaveClick: () -> Unit,
    onExportShareClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val haptic = LocalHapticFeedback.current

    var currentScreen by remember { mutableStateOf(ExportScreen.MAIN) }

    // Intercettazione del tasto Back di sistema per tornare alla vista principale invece di chiudere il dialogo
    BackHandler(enabled = currentScreen == ExportScreen.EXPORT_CHOICE) {
        currentScreen = ExportScreen.MAIN
    }

    Dialog(
        onDismissRequest = onDismiss,
        // 🧠 PROTEZIONE TOCCO INVOLONTARIO:
        // `dismissOnClickOutside = false` impedisce la chiusura accidentale del dialogo se l'utente tocca lo sfondo scuro esterno.
        // L'unico modo per uscire è premere il tasto Chiudi/Indietro o il comando Indietro di Android.
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = screenHeight * 0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            AnimatedContent(
                targetState = currentScreen,
                // 🧠 OTTIMIZZAZIONE PRESTAZIONI (Accelerazione Hardware GPU):
                // `Modifier.graphicsLayer()` forza il rendering off-screen su GPU durante lo slide,
                // evitando ricalcoli di layout (measure/layout pass) superflui e garantendo 60/120 FPS stabili.
                modifier = Modifier.graphicsLayer(),
                transitionSpec = {
                    // 🧠 TRANSIZIONE FLUIDA OTTIMIZZATA:
                    // Usiamo una durata ridotta a 220ms con curva di accelerazione 'FastOutSlowInEasing'
                    // e uno spostamento parziale (width / 3) anziché a schermo intero (width),
                    // eliminando qualsiasi effetto di scatto (jank) al ritorno alla vista principale.
                    if (targetState == ExportScreen.EXPORT_CHOICE) {
                        (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeOut(animationSpec = tween(220)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeOut(animationSpec = tween(220)))
                    }
                },
                label = "BackupPortraitAnimation"
            ) { screen ->
                when (screen) {
                    ExportScreen.MAIN -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Intestazione
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp).padding(end = 10.dp)
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


                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Scheda 1: Esporta salvataggi
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
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                currentScreen = ExportScreen.EXPORT_CHOICE
                                            },
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Filled.Upload, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            AutoResizedText(
                                                text = stringResource(R.string.btn_esporta_backup),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Scheda 2: Importa salvataggi
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
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
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
                            }

                            // Tasto Chiudi
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.btn_chiudi_backup),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    ExportScreen.EXPORT_CHOICE -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Intestazione centrata
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Upload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp).padding(bottom = 4.dp)
                                )
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AutoResizedText(
                                        text = stringResource(R.string.titolo_dialog_esporta_scelta),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Corpo opzioni
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Opzione 1: Salva sul dispositivo
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        currentScreen = ExportScreen.MAIN
                                        onExportSaveClick()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Download,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.opt_salva_dispositivo),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.desc_salva_dispositivo),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Opzione 2: Condividi file
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        currentScreen = ExportScreen.MAIN
                                        onExportShareClick()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Share,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.opt_condividi_backup),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.desc_condividi_backup),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Tasto Indietro
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    currentScreen = ExportScreen.MAIN
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                AutoResizedText(
                                    text = stringResource(R.string.btn_indietro),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * LAYOUT ORIZZONTALE (Landscape Split-Screen) PER IL DIALOGO PRINCIPALE
 * Con navigazione interna animata (AnimatedContent) tra vista principale e sotto-menu di esportazione.
 */
@Composable
private fun BackupRestoreDialogLandscape(
    onExportSaveClick: () -> Unit,
    onExportShareClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val haptic = LocalHapticFeedback.current

    var currentScreen by remember { mutableStateOf(ExportScreen.MAIN) }

    BackHandler(enabled = currentScreen == ExportScreen.EXPORT_CHOICE) {
        currentScreen = ExportScreen.MAIN
    }

    Dialog(
        onDismissRequest = onDismiss,
        // 🧠 PROTEZIONE TOCCO INVOLONTARIO:
        // `dismissOnClickOutside = false` impedisce la chiusura accidentale del dialogo se l'utente tocca lo sfondo scuro esterno in Landscape.
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = screenHeight * 0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PANNELLO SINISTRO (~28%): Intestazione + Tasto chiusura
                Column(
                    modifier = Modifier
                        .weight(0.28f)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
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

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        AutoResizedText(
                            text = stringResource(R.string.btn_chiudi_backup),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // PANNELLO DESTRO (~72%): AnimatedContent tra Vista Principale e Sottomenu Esportazione (Senza table wrapper superfluo)
                Surface(
                    modifier = Modifier
                        .weight(0.72f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        // 🧠 OTTIMIZZAZIONE PRESTAZIONI (Accelerazione Hardware GPU) in Landscape:
                        // Forza il rendering off-screen su GPU per azzerare qualsiasi scatto (jank) durante la transizione orizzontale.
                        modifier = Modifier.graphicsLayer(),
                        transitionSpec = {
                            if (targetState == ExportScreen.EXPORT_CHOICE) {
                                (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeOut(animationSpec = tween(220)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeOut(animationSpec = tween(220)))
                            }
                        },
                        label = "BackupLandscapeAnimation"
                    ) { screen ->
                        when (screen) {
                            ExportScreen.MAIN -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Scheda Esportazione
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
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
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    currentScreen = ExportScreen.EXPORT_CHOICE
                                                },
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Icon(Icons.Filled.Upload, null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.btn_esporta_backup),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }

                                    // Scheda Importazione
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
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
                                }
                            }
                            ExportScreen.EXPORT_CHOICE -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Upload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp).padding(bottom = 2.dp)
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AutoResizedText(
                                                text = stringResource(R.string.titolo_dialog_esporta_scelta),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Opzione 1: Salva sul dispositivo
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            currentScreen = ExportScreen.MAIN
                                            onExportSaveClick()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.opt_salva_dispositivo),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(R.string.desc_salva_dispositivo),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Opzione 2: Condividi file
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            currentScreen = ExportScreen.MAIN
                                            onExportShareClick()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Share,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.opt_condividi_backup),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(R.string.desc_condividi_backup),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Tasto Indietro
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            currentScreen = ExportScreen.MAIN
                                        },
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        AutoResizedText(
                                            text = stringResource(R.string.btn_indietro),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
