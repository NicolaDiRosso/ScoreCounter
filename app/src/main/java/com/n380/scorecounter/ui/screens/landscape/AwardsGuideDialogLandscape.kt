package com.n380.scorecounter.ui.screens.landscape

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R
import com.n380.scorecounter.ui.components.AutoResizedText

/**
 * ====================================================================
 * MODELLO DATI INTERNO PER LA GUIDA AI PREMI
 * ====================================================================
 * Contiene l'emoji e i riferimenti ai file di risorse XML per titolo e descrizione.
 */
private data class AwardInfoItem(
    val icon: String,
    val titleRes: Int,
    val msgRes: Int
)

/**
 * Lista completa dei 9 premi esistenti e nuovi
 */
private val allGuideAwards = listOf(
    AwardInfoItem("🎯", R.string.premio_cecchino, R.string.msg_cecchino),
    AwardInfoItem("🔥", R.string.premio_inarrestabile, R.string.msg_inarrestabile),
    AwardInfoItem("🦞", R.string.premio_gambero, R.string.msg_gambero),
    AwardInfoItem("🦅", R.string.premio_fenice, R.string.msg_fenice),
    AwardInfoItem("👑", R.string.premio_dittatore, R.string.msg_dittatore),
    AwardInfoItem("🐺", R.string.premio_predatore, R.string.msg_predatore),
    AwardInfoItem("🤝", R.string.premio_testa_a_testa, R.string.msg_testa_a_testa),
    AwardInfoItem("🐌", R.string.premio_lumaca, R.string.msg_lumaca),
    AwardInfoItem("🚂", R.string.premio_diesel, R.string.msg_diesel)
)

/**
 * ====================================================================
 * DIALOG GUIDA AI PREMI ADATTIVO (Supporta sia Landscape che Portrait)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Questo composable agisce da "Smart Dispatcher": rileva l'orientamento dello
 * schermo (`LocalConfiguration.current.orientation`) e smista l'interfaccia verso:
 * - `AwardsGuideDialogLandscape` se lo schermo è in orizzontale.
 * - `AwardsGuideDialogPortrait` se lo schermo è in verticale.
 */
@Composable
fun AwardsGuideDialog(
    onDismissRequest: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        AwardsGuideDialogLandscape(onDismissRequest = onDismissRequest)
    } else {
        AwardsGuideDialogPortrait(onDismissRequest = onDismissRequest)
    }
}

/**
 * ====================================================================
 * DIALOG GUIDA AI PREMI - VERSIONE ORIZZONTALE (Landscape Split-Screen)
 * ====================================================================
 * 🧠 DESIGN SYSTEM RESPONSIVO:
 * - Pannello Sinistro (~28%): Contiene icona, titolo e descrizione. La parte testuale
 *   è avvolta in una Column con `.weight(1f).verticalScroll(...)` così da adattarsi a
 *   qualsiasi altezza di smartphone senza spingere o tagliare il pulsante "✓ Ho capito",
 *   il quale rimane fisse e ancorato in basso.
 * - Pannello Destro (~72%): Bacheca premi a 2 colonne fisse scorrevole, composta
 *   da micro-card espandibili al click (`ExpandableAwardCard`).
 */
@Composable
fun AwardsGuideDialogLandscape(
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = { }, // Blocca la chiusura accidentale toccando fuori dal dialog
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
                // ------------------------------------------------------------
                // PANNELLO SINISTRO (~28%): Intestazione + Pulsante Ancorato
                // ------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Area testuale superiore scorrevole per schermi a bassa risoluzione verticale
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.desc_info_premi),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(44.dp)
                                .padding(bottom = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.titolo_guida_premi),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.desc_guida_premi),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pulsante "✓ Ho capito": Ancorato in basso, visibile al 100% su qualunque dispositivo
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        AutoResizedText(
                            stringResource(R.string.btn_ho_capito),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // ------------------------------------------------------------
                // PANNELLO DESTRO (~72%): Bacheca dei Premi a 2 Colonne Espandibili
                // ------------------------------------------------------------
                Surface(
                    modifier = Modifier
                        .weight(0.72f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(allGuideAwards) { item ->
                            ExpandableAwardCard(
                                icon = item.icon,
                                titleRes = item.titleRes,
                                msgRes = item.msgRes
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ====================================================================
 * DIALOG GUIDA AI PREMI - VERSIONE VERTICALE (Portrait)
 * ====================================================================
 * 🧠 DESIGN SYSTEM VERTICALE:
 * Mostra le 9 schede dei premi in una colonna verticale LazyColumn.
 * Ogni scheda è compressa e si espande al tocco dell'utente.
 */
@Composable
fun AwardsGuideDialogPortrait(
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = { },
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
                // Intestazione con icona e titolo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.titolo_guida_premi),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.desc_guida_premi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lista scorrevole delle 9 schede premio espandibili
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(allGuideAwards) { item ->
                            ExpandableAwardCard(
                                icon = item.icon,
                                titleRes = item.titleRes,
                                msgRes = item.msgRes
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pulsante di conferma inferiore
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    AutoResizedText(
                        stringResource(R.string.btn_ho_capito),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE MICRO-CARD PREMIO ESPANDIBILE (ExpandableAwardCard)
 * ====================================================================
 * 🧠 MECCANICA DI ESPANSIONE:
 * - Di default la card è in uno stato "compresso": mostra solo l'emoji,
 *   il titolo in grassetto e la freccia d'espansione.
 * - Al click/tap, lo stato `isExpanded` commuta a `true`, attivando l'animazione
 *   `AnimatedVisibility` (espansione verticale e sfumatura di comparsa)
 *   che rivela la spiegazione dettagliata del premio.
 */
@Composable
fun ExpandableAwardCard(
    icon: String,
    titleRes: Int,
    msgRes: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isExpanded = !isExpanded
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Intestazione Card (Sempre Visibile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(titleRes),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Descrizione Espandibile con Animazione Fluida
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 6.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    Text(
                        text = stringResource(msgRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}