package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R

/**
 * Modello interno per le schede della guida al grafico
 */
private data class ChartGuideItem(
    val titleRes: Int,
    val msgRes: Int
)

/**
 * Lista completa dei 5 punti guida per la lettura del grafico
 */
private val allChartItems = listOf(
    ChartGuideItem(R.string.titolo_guida_punti, R.string.msg_guida_punti),
    ChartGuideItem(R.string.titolo_guida_tempo, R.string.msg_guida_tempo),
    ChartGuideItem(R.string.titolo_guida_pallini, R.string.msg_guida_pallini),
    ChartGuideItem(R.string.titolo_guida_sorpassi, R.string.msg_guida_sorpassi),
    ChartGuideItem(R.string.titolo_guida_zoom, R.string.msg_guida_zoom)
)

/**
 * ====================================================================
 * DIALOG GUIDA GRAFICO ADATTIVO (Landscape & Portrait)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Schede sempre visibili ed espanse per consultazione immediata dei 4 concetti chiave:
 * - Orizzontale (Landscape): Split-Screen a 2 pannelli con griglia a 2 colonne
 * - Verticale (Portrait): Modal Dialog con schede direttamente visibili.
 */
@Composable
fun ChartGuideDialog(
    onDismissRequest: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ChartGuideDialogLandscape(onDismissRequest = onDismissRequest)
    } else {
        ChartGuideDialogPortrait(onDismissRequest = onDismissRequest)
    }
}

/**
 * LAYOUT ORIZZONTALE (Landscape Split-Screen)
 */
@Composable
fun ChartGuideDialogLandscape(
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = { }, // Blocca la chiusura accidentale toccando fuori
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
                // PANNELLO SINISTRO (~28%): Intestazione + Pulsante ancorato in basso
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
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = stringResource(R.string.desc_info_grafico),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(44.dp)
                                .padding(bottom = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.titolo_guida_grafico),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.desc_guida_grafico),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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

                // PANNELLO DESTRO (~72%): Griglia a 2 colonne delle schede del grafico con testo direttamente visibile
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
                        items(allChartItems) { item ->
                            ChartGuideCard(
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
 * LAYOUT VERTICALE (Portrait)
 */
@Composable
fun ChartGuideDialogPortrait(
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
                // Intestazione
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.titolo_guida_grafico),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.desc_guida_grafico),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lista verticale di schede con testo direttamente visibile
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
                        items(allChartItems) { item ->
                            ChartGuideCard(
                                titleRes = item.titleRes,
                                msgRes = item.msgRes
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pulsante inferiore
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
 * SCHEDA SEMPRE ESPANSA CON INFORMAZIONI SUBITO VISIBILI
 */
@Composable
fun ChartGuideCard(
    titleRes: Int,
    msgRes: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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