package com.n380.scorecounter.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n380.scorecounter.R

/**
 * Modello interno per la singola riga di elenco puntato guidato (Icona + Parola chiave in grassetto + Descrizione).
 */
data class CreateMatchBulletItem(
    val icon: ImageVector,
    val keywordRes: Int,
    val descRes: Int
)

/**
 * Modello interno per la scheda card della nuova sfida.
 */
data class CreateMatchCardModel(
    val sectionTitleRes: Int,
    val sectionIcon: ImageVector,
    val bullets: List<CreateMatchBulletItem>
)

/**
 * Lista completa delle 3 card informative della nuova sfida con icone tematiche e parole chiave in grassetto.
 */
private val createMatchCardsList = listOf(
    // CARD 1: Regole del gioco
    CreateMatchCardModel(
        sectionTitleRes = R.string.titolo_regole_gioco,
        sectionIcon = Icons.AutoMirrored.Filled.MenuBook,
        bullets = listOf(
            CreateMatchBulletItem(Icons.Filled.SportsEsports, R.string.label_regola_nome, R.string.desc_regola_nome),
            CreateMatchBulletItem(Icons.Filled.History, R.string.label_regola_recenti, R.string.desc_regola_recenti),
            CreateMatchBulletItem(Icons.AutoMirrored.Filled.TrendingUp, R.string.label_regola_punteggio, R.string.desc_regola_punteggio),
            CreateMatchBulletItem(Icons.Filled.EmojiEvents, R.string.label_regola_traguardo, R.string.desc_regola_traguardo)
        )
    ),
    // CARD 2: Gestione partecipanti
    CreateMatchCardModel(
        sectionTitleRes = R.string.titolo_gestione_partecipanti,
        sectionIcon = Icons.Filled.PersonAddAlt1,
        bullets = listOf(
            CreateMatchBulletItem(Icons.Filled.Group, R.string.label_partecipanti_rapidi, R.string.desc_partecipanti_rapidi),
            CreateMatchBulletItem(Icons.Filled.PersonAdd, R.string.label_partecipanti_manuale, R.string.desc_partecipanti_manuale)
        )
    ),
    // CARD 3: Tavolo partecipanti
    CreateMatchCardModel(
        sectionTitleRes = R.string.titolo_tavolo_partecipanti,
        sectionIcon = Icons.Filled.Groups,
        bullets = listOf(
            CreateMatchBulletItem(Icons.Filled.Edit, R.string.label_tavolo_partecipanti, R.string.desc_tavolo_partecipanti)
        )
    )
)

/**
 * ====================================================================
 * DIALOG INFO NUOVA SFIDA ADATTIVO (Landscape & Portrait)
 * ====================================================================
 */
@Composable
fun CreateMatchInfoDialog(
    onDismissRequest: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        CreateMatchInfoDialogLandscape(onDismissRequest = onDismissRequest)
    } else {
        CreateMatchInfoDialogPortrait(onDismissRequest = onDismissRequest)
    }
}

/**
 * LAYOUT ORIZZONTALE (Landscape Split-Screen) con altezza massima estesa (85%-90%) e intestazione pulita.
 */
@Composable
fun CreateMatchInfoDialogLandscape(
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // 🧠 ALTEZZA MASSIMA 85%-90% DELLO SCHERMO:
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxDialogHeight = screenHeight * 0.88f

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = maxDialogHeight),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PANNELLO SINISTRO: Intestazione pulita (senza sottotitolo) + Pulsante Chiudi
                Column(
                    modifier = Modifier
                        .weight(0.32f)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.desc_info_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(44.dp)
                            .padding(bottom = 6.dp)
                    )
                    Text(
                        text = stringResource(R.string.titolo_info_nuova_sfida),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

                // PANNELLO DESTRO: Lista delle 3 card informative (senza riquadro esterno)
                LazyColumn(
                    modifier = Modifier
                        .weight(0.68f)
                        .heightIn(max = maxDialogHeight - 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(createMatchCardsList) { cardModel ->
                        CreateMatchFeatureCard(cardModel = cardModel)
                    }
                }
            }
        }
    }
}

/**
 * LAYOUT VERTICALE (Portrait) con altezza massima estesa (85%-90%) e intestazione pulita.
 */
@Composable
fun CreateMatchInfoDialogPortrait(
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // 🧠 ALTEZZA MASSIMA 85%-90% DELLO SCHERMO:
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxDialogHeight = screenHeight * 0.88f

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = maxDialogHeight),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. INTESTAZIONE PULITA (Senza sottotitolo per massimizzare lo spazio verticale)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 10.dp)
                    )
                    Text(
                        text = stringResource(R.string.titolo_info_nuova_sfida),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 2. CONTENITORE SCORREVOLE DELLE 3 CARD (Senza riquadro esterno)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(createMatchCardsList) { cardModel ->
                        CreateMatchFeatureCard(cardModel = cardModel)
                    }
                }

                // 3. PULSANTE INFERIORE DI CHIUSURA.
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
 * SCHEDA SINGOLA INFORMATIVA (Con titolo principale, icona e righe di elenchi puntati formattate).
 */
@Composable
fun CreateMatchFeatureCard(
    cardModel: CreateMatchCardModel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Intestazione della Card con icona della sezione
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = cardModel.sectionIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(cardModel.sectionTitleRes),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // Elenco delle sotto-voci con icone tematiche e parole chiave in grassetto
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cardModel.bullets.forEach { bullet ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Icona tematica della sotto-voce
                        Icon(
                            imageVector = bullet.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )

                        // Testo formattato con parola chiave in grassetto tramite buildAnnotatedString
                        val annotatedText = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(bullet.keywordRes))
                            }
                            append(" ")
                            append(stringResource(bullet.descRes))
                        }

                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
