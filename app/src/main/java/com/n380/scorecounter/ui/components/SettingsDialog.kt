/*package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R

/**
 * ====================================================================
 * COMPONENTE UI: DIALOGO IMPOSTAZIONI E PREFERENZE (M3 ADATTIVO)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA RIGOROSA:
 * Questo dialogo rappresenta l'hub centrale per la personalizzazione dell'app.
 * Adotta il nuovo Design System unificato di ScoreCounter con:
 * - Finestra principale Surface con angoli arrotondati a 24.dp e bordo traslucido.
 * - Sotto-moduli di sezione organizzati in schede Surface a 12.dp.
 * - Supporto reattivo per Portrait (Modal Dialog) e Landscape (Split-Screen).
 *
 * Le opzioni sono suddivise in 3 sezioni logiche:
 * A. Interfaccia e aspetto (Tema, Lingua, Orientamento, Stile grafico)
 * B. Schermo e comportamento (Schermo sempre attivo, Conferma azioni)
 * C. Personalizzazione gioco (Passo punteggio, Ordinamento classifica)
 *
 * @param onDismiss Lambda di callback per chiudere la schermata.
 */
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        SettingsDialogLandscape(onDismiss = onDismiss)
    } else {
        SettingsDialogPortrait(onDismiss = onDismiss)
    }
}

/**
 * LAYOUT VERTICALE (Portrait)
 */
@Composable
private fun SettingsDialogPortrait(
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
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
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.titolo_impostazioni_dialog),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.desc_impostazioni_dialog),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // CORPO SCORREVOLE DELLE IMPOSTAZIONI
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // SEZIONE A: INTERFACCIA E ASPETTO
                        SettingsSectionHeader(stringResource(R.string.sez_interfaccia_aspetto))
                        SettingsRowCard(
                            icon = Icons.Filled.Palette,
                            title = stringResource(R.string.opt_tema_app),
                            subtitle = stringResource(R.string.desc_opt_tema_app)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.Language,
                            title = stringResource(R.string.opt_lingua_app),
                            subtitle = stringResource(R.string.desc_opt_lingua_app)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.ScreenRotation,
                            title = stringResource(R.string.opt_orientamento_schermo),
                            subtitle = stringResource(R.string.desc_opt_orientamento_schermo)
                        )
                        SettingsRowCard(
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = stringResource(R.string.opt_stile_grafico),
                            subtitle = stringResource(R.string.desc_opt_stile_grafico)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // SEZIONE B: SCHERMO E COMPORTAMENTO
                        SettingsSectionHeader(stringResource(R.string.sez_schermo_comportamento))
                        SettingsRowCard(
                            icon = Icons.Filled.Lightbulb,
                            title = stringResource(R.string.opt_schermo_attivo),
                            subtitle = stringResource(R.string.desc_opt_schermo_attivo)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.CheckCircle,
                            title = stringResource(R.string.opt_conferma_azioni),
                            subtitle = stringResource(R.string.desc_opt_conferma_azioni)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // SEZIONE C: PERSONALIZZAZIONE GIOCO
                        SettingsSectionHeader(stringResource(R.string.sez_personalizzazione_gioco))
                        SettingsRowCard(
                            icon = Icons.Filled.PlusOne,
                            title = stringResource(R.string.opt_passo_punteggio),
                            subtitle = stringResource(R.string.desc_opt_passo_punteggio)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.FormatListNumbered,
                            title = stringResource(R.string.opt_ordinamento_classifica),
                            subtitle = stringResource(R.string.desc_opt_ordinamento_classifica)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TASTO CHIUDI ANCORATO
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
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
 * LAYOUT ORIZZONTALE (Landscape Split-Screen)
 */
@Composable
private fun SettingsDialogLandscape(
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(44.dp)
                                .padding(bottom = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.titolo_impostazioni_dialog),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.desc_impostazioni_dialog),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
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

                // PANNELLO DESTRO (~72%): Lista delle Sezioni di Impostazione
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // SEZIONE A
                        SettingsSectionHeader(stringResource(R.string.sez_interfaccia_aspetto))
                        SettingsRowCard(
                            icon = Icons.Filled.Palette,
                            title = stringResource(R.string.opt_tema_app),
                            subtitle = stringResource(R.string.desc_opt_tema_app)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.Language,
                            title = stringResource(R.string.opt_lingua_app),
                            subtitle = stringResource(R.string.desc_opt_lingua_app)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.ScreenRotation,
                            title = stringResource(R.string.opt_orientamento_schermo),
                            subtitle = stringResource(R.string.desc_opt_orientamento_schermo)
                        )
                        SettingsRowCard(
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = stringResource(R.string.opt_stile_grafico),
                            subtitle = stringResource(R.string.desc_opt_stile_grafico)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // SEZIONE B
                        SettingsSectionHeader(stringResource(R.string.sez_schermo_comportamento))
                        SettingsRowCard(
                            icon = Icons.Filled.Lightbulb,
                            title = stringResource(R.string.opt_schermo_attivo),
                            subtitle = stringResource(R.string.desc_opt_schermo_attivo)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.CheckCircle,
                            title = stringResource(R.string.opt_conferma_azioni),
                            subtitle = stringResource(R.string.desc_opt_conferma_azioni)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // SEZIONE C
                        SettingsSectionHeader(stringResource(R.string.sez_personalizzazione_gioco))
                        SettingsRowCard(
                            icon = Icons.Filled.PlusOne,
                            title = stringResource(R.string.opt_passo_punteggio),
                            subtitle = stringResource(R.string.desc_opt_passo_punteggio)
                        )
                        SettingsRowCard(
                            icon = Icons.Filled.FormatListNumbered,
                            title = stringResource(R.string.opt_ordinamento_classifica),
                            subtitle = stringResource(R.string.desc_opt_ordinamento_classifica)
                        )
                    }
                }
            }
        }
    }
}

/**
 * COMPONENTE DIDATTICO: Intestazione di Sezione Impostazioni
 */
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

/**
 * COMPONENTE DIDATTICO: Singola Scheda Opzione Impostazioni (M3 Surface Card)
 */
@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}*/