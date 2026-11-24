package com.n380.scorecounter.ui.screens.landscape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R
import com.n380.scorecounter.model.Player
import com.n380.scorecounter.ui.components.AppBackground
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.ui.components.BackgroundStyle
import com.n380.scorecounter.ui.components.PlayerScoreCard
import com.n380.scorecounter.viewmodel.MatchViewModel

/**
 * ====================================================================
 * SCHERMATA SELEZIONE SFONDO ORIZZONTALE (BackgroundSettingsScreenLandscape.kt)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA ARCHITETTURA LANDSCAPE:
 * Layout asimmetrico a 2 colonne ottimizzato per Tablet e schermi orizzontali:
 * - Colonna Sinistra (~42%): Titolo, Anteprima Live interattiva con card segnapunti e pulsante Indietro in basso.
 * - Colonna Destra (~58%): Sezioni scorrevoli "Sfondi animati" e "Sfondi statici" con griglia a 3 colonne.
 */
@Composable
fun BackgroundSettingsScreenLandscape(
    viewModel: MatchViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isClosing by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ====================================================================
            // 🏛️ COLONNA SINISTRA (42%): Intestazione, Anteprima Live & Indietro
            // ====================================================================
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Intestazione con freccia indietro e titolo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (!isClosing) {
                                    isClosing = true
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AutoResizedText(
                            text = stringResource(R.string.label_sfondo_app),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Riquadro Anteprima Live
                    LiveBackgroundPreviewCardLandscape(viewModel = viewModel)
                }

                // Pulsante Indietro in basso a sinistra
                Button(
                    onClick = {
                        if (!isClosing) {
                            isClosing = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.btn_indietro),
                        modifier = Modifier.padding(end = 6.dp).size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.btn_indietro),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ====================================================================
            // 📋 COLONNA DESTRA (58%): Sezioni Sfondi Animati e Statici a tutta altezza
            // ====================================================================
            Card(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BackgroundSectionsContentLandscape(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun LiveBackgroundPreviewCardLandscape(
    viewModel: MatchViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.titolo_anteprima_sfondo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.desc_anteprima_sfondo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppBackground(
                    style = viewModel.backgroundStyle,
                    animate = true
                )

                val samplePlayer = remember {
                    Player("Nicola").apply {
                        score = 12
                        color = 0xFF1E88E5.toInt()
                    }
                }

                PlayerScoreCard(
                    player = samplePlayer,
                    isLeader = true,
                    totalPlayers = 1,
                    isExpandedMode = false,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(64.dp),
                    onScoreChange = { },
                    onScoreClick = { }
                )
            }
        }
    }
}

@Composable
private fun BackgroundSectionsContentLandscape(
    viewModel: MatchViewModel
) {
    val animatedOptions = listOf(
        BackgroundStyle.ANIMATED_ICONS to Pair(stringResource(R.string.sfondo_icone_animate), Icons.Filled.Animation),
        BackgroundStyle.ANIMATED_NUMBERS to Pair(stringResource(R.string.sfondo_numeri_animati), Icons.Filled.Pin),
        BackgroundStyle.ANIMATED_PARTICLES to Pair(stringResource(R.string.sfondo_particelle_animate), Icons.Filled.BlurOn),
        BackgroundStyle.ANIMATED_WAVES to Pair(stringResource(R.string.sfondo_onde_animate), Icons.Filled.Waves),
        BackgroundStyle.ANIMATED_AURORA to Pair(stringResource(R.string.sfondo_aurora_animata), Icons.Filled.Gradient),
        BackgroundStyle.ANIMATED_STARS to Pair(stringResource(R.string.sfondo_stelle_animate), Icons.Filled.AutoAwesome)
    )

    val staticOptions = listOf(
        BackgroundStyle.ICONS to Pair(stringResource(R.string.sfondo_icone), Icons.Filled.Palette),
        BackgroundStyle.SOLID to Pair(stringResource(R.string.sfondo_tinta_unita), Icons.Filled.Visibility),
        BackgroundStyle.GRADIENT to Pair(stringResource(R.string.sfondo_sfumatura), Icons.Filled.Gradient),
        BackgroundStyle.DOTS to Pair(stringResource(R.string.sfondo_puntini), Icons.Filled.MoreHoriz),
        BackgroundStyle.GEOMETRIC to Pair(stringResource(R.string.sfondo_geometrico), Icons.Filled.GridOn),
        BackgroundStyle.WAVES to Pair(stringResource(R.string.sfondo_onde), Icons.Filled.Waves)
    )

    BackgroundCategoryCardLandscape(
        title = stringResource(R.string.sez_sfondi_animati),
        icon = Icons.Filled.AutoAwesome,
        options = animatedOptions,
        selectedStyle = viewModel.backgroundStyle,
        onSelectStyle = { style -> viewModel.setBackgroundStylePreference(style.ordinal) }
    )

    BackgroundCategoryCardLandscape(
        title = stringResource(R.string.sez_sfondi_statici),
        icon = Icons.Filled.Palette,
        options = staticOptions,
        selectedStyle = viewModel.backgroundStyle,
        onSelectStyle = { style -> viewModel.setBackgroundStylePreference(style.ordinal) }
    )
}

@Composable
private fun BackgroundCategoryCardLandscape(
    title: String,
    icon: ImageVector,
    options: List<Pair<BackgroundStyle, Pair<String, ImageVector>>>,
    selectedStyle: BackgroundStyle,
    onSelectStyle: (BackgroundStyle) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rows = options.chunked(3)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AutoResizedText(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { (style, labelAndIcon) ->
                        val isSelected = (selectedStyle == style)
                        val (label, _) = labelAndIcon

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onSelectStyle(style)
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = if (isSelected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppBackground(
                                            style = style,
                                            animate = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    AutoResizedText(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                } else if (style.isAnimated) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(16.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
