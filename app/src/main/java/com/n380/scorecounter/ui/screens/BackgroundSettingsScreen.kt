package com.n380.scorecounter.ui.screens

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
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
 * SCHERMATA DEDICATA SELEZIONE SFONDO APP (BackgroundSettingsScreen.kt)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA ARCHITETTURA:
 * Questa schermata consente all'utente di selezionare in tempo reale lo stile di sfondo dell'app.
 * - Trasparenza Root: Il contenitore principale usa `containerColor = Color.Transparent` in modo che
 *   lo sfondo reale dell'app (`AppBackground`), posizionato al livello inferiore in `MainActivity`,
 *   cambi dal vivo direttamente dietro questa pagina man mano che l'utente seleziona le opzioni.
 * - Struttura speculare a SettingsScreen: Titolo ancorato in alto a sinistra, grande card
 *   contenitiva (`surfaceVariant`) al centro e dock inferiore con pulsante "Indietro".
 * - Ottimizzazione energetica: Le 12 card sono divise in due schede ("Sfondi animati" in
 *   evidenza in alto + "Sfondi statici" sotto) con miniature compatte a 3 colonne (`animate = false`),
 *   mentre l'animazione reale gira unicamente nell'anteprima live grande e sullo sfondo dell'app!
 */
@Composable
fun BackgroundSettingsScreen(
    viewModel: MatchViewModel,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Prevenzione del doppio click accidentale sul pulsante Indietro
    var isClosing by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding(),
        containerColor = Color.Transparent, // Mantiene la trasparenza dello Scaffold per mostrare lo sfondo live

        // ====================================================================
        // DOCK INFERIORE ANCORATO (Pulsante Indietro, identico al dock di SettingsScreen)
        // ====================================================================
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = if (isLandscape) 10.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            .widthIn(max = 700.dp)
                            .height(if (isLandscape) 52.dp else 64.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_indietro),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                        AutoResizedText(
                            text = stringResource(R.string.btn_indietro),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TITOLO PRINCIPALE FISSO IN ALTO A SINISTRA (Coerente con tutte le schermate)
            Spacer(modifier = Modifier.height(16.dp))
            AutoResizedText(
                text = stringResource(R.string.label_sfondo_app),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .padding(bottom = 16.dp)
            )

            // 2. GRANDE TAVOLO CONTENITIVO (Outer Card speculare a SettingsScreen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (isLandscape) 950.dp else 700.dp)
                    .weight(1f)
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isLandscape) {
                    // LAYOUT LANDSCAPE / TABLET: 2 colonne affiancate dentro il tavolo
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Colonna Sinistra (42% W): Anteprima Live con Mini-Card Segnapunti
                        Box(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                            LiveBackgroundPreviewCard(viewModel = viewModel)
                        }

                        // Colonna Destra (58% W): Sezioni Sfondi Animati e Statici
                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BackgroundSectionsContent(viewModel = viewModel)
                        }
                    }
                } else {
                    // LAYOUT PORTRAIT: Disposizione verticale scorrevole
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 2a. Riquadro Anteprima Live
                        LiveBackgroundPreviewCard(viewModel = viewModel)

                        // 2b. Sezioni Sfondi Animati e Statici
                        BackgroundSectionsContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: RIQUADRO ANTEPRIMA LIVE (LiveBackgroundPreviewCard)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Mostra una card interna (`surface`) contenente lo sfondo reale attivo (`AppBackground`
 * con `animate = true`) e una mini-card segnapunti di esempio nello stile nativo di ScoreCounter.
 */
@Composable
private fun LiveBackgroundPreviewCard(
    viewModel: MatchViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Intestazione dell'anteprima
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.titolo_anteprima_sfondo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.desc_anteprima_sfondo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Box di Anteprima con Mini-Card Esempio (qui l'animazione è sempre attiva!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Sfondo reale attivo
                AppBackground(
                    style = viewModel.backgroundStyle,
                    animate = true
                )

                // Mini-Card Segnapunti di Esempio
                val samplePlayer = remember {
                    Player("Nicola").apply {
                        score = 12
                        color = 0xFF1E88E5.toInt() // Blu vibrante
                    }
                }

                PlayerScoreCard(
                    player = samplePlayer,
                    isLeader = true,
                    totalPlayers = 1,
                    isExpandedMode = false,
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .height(72.dp),
                    onScoreChange = { },
                    onScoreClick = { }
                )
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: CONTENUTO SEZIONI SFONDI (BackgroundSectionsContent)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Organizza i 12 stili di sfondo in due schede distinte (speculari alle tabelle
 * di `SettingsScreen`):
 * 1. "Sfondi animati" (in evidenza in cima, 6 opzioni su 2 righe da 3 card compatte).
 * 2. "Sfondi statici" (subito sotto, 6 opzioni su 2 righe da 3 card compatte).
 */
@Composable
private fun BackgroundSectionsContent(
    viewModel: MatchViewModel
) {
    // 1. Gruppo Sfondi Animati (6 stili)
    val animatedOptions = listOf(
        BackgroundStyle.ANIMATED_ICONS to Pair(stringResource(R.string.sfondo_icone_animate), Icons.Filled.Animation),
        BackgroundStyle.ANIMATED_NUMBERS to Pair(stringResource(R.string.sfondo_numeri_animati), Icons.Filled.Pin),
        BackgroundStyle.ANIMATED_PARTICLES to Pair(stringResource(R.string.sfondo_particelle_animate), Icons.Filled.BlurOn),
        BackgroundStyle.ANIMATED_WAVES to Pair(stringResource(R.string.sfondo_onde_animate), Icons.Filled.Waves),
        BackgroundStyle.ANIMATED_AURORA to Pair(stringResource(R.string.sfondo_aurora_animata), Icons.Filled.Gradient),
        BackgroundStyle.ANIMATED_STARS to Pair(stringResource(R.string.sfondo_stelle_animate), Icons.Filled.AutoAwesome)
    )

    // 2. Gruppo Sfondi Statici (6 stili)
    val staticOptions = listOf(
        BackgroundStyle.ICONS to Pair(stringResource(R.string.sfondo_icone), Icons.Filled.Palette),
        BackgroundStyle.SOLID to Pair(stringResource(R.string.sfondo_tinta_unita), Icons.Filled.Visibility),
        BackgroundStyle.GRADIENT to Pair(stringResource(R.string.sfondo_sfumatura), Icons.Filled.Gradient),
        BackgroundStyle.DOTS to Pair(stringResource(R.string.sfondo_puntini), Icons.Filled.MoreHoriz),
        BackgroundStyle.GEOMETRIC to Pair(stringResource(R.string.sfondo_geometrico), Icons.Filled.GridOn),
        BackgroundStyle.WAVES to Pair(stringResource(R.string.sfondo_onde), Icons.Filled.Waves)
    )

    // Scheda Sezione 1: Sfondi Animati (in evidenza)
    BackgroundCategoryCard(
        title = stringResource(R.string.sez_sfondi_animati),
        icon = Icons.Filled.AutoAwesome,
        options = animatedOptions,
        selectedStyle = viewModel.backgroundStyle,
        onSelectStyle = { style -> viewModel.setBackgroundStylePreference(style.ordinal) }
    )

    // Scheda Sezione 2: Sfondi Statici
    BackgroundCategoryCard(
        title = stringResource(R.string.sez_sfondi_statici),
        icon = Icons.Filled.Palette,
        options = staticOptions,
        selectedStyle = viewModel.backgroundStyle,
        onSelectStyle = { style -> viewModel.setBackgroundStylePreference(style.ordinal) }
    )
}

/**
 * ====================================================================
 * COMPONENTE: SCHEDA CATEGORIA SFONDI (BackgroundCategoryCard)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Card contenitrice (`surface`) con intestazione e griglia compatta a 3 colonne
 * (`height = 96.dp`). Per non sovraccaricare GPU e batteria su dispositivi
 * medio/bassi, le miniature passano `animate = false` ad [AppBackground]
 * e mostrano un discreto badge scintilla (`✦`) per identificare gli stili animati.
 */
@Composable
private fun BackgroundCategoryCard(
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Intestazione della sezione (coerente con SettingsSectionCard)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AutoResizedText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Righe compatte a 3 colonne
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (style, labelAndIcon) ->
                        val isSelected = (selectedStyle == style)
                        val (label, _) = labelAndIcon

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(96.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onSelectStyle(style)
                                },
                            shape = RoundedCornerShape(16.dp),
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
                                        .padding(7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Miniatura compatta dello sfondo (animate = false per zero lag)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppBackground(
                                            style = style,
                                            animate = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Etichetta compatta del nome sfondo
                                    AutoResizedText(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Badge spunta selezionato (✓) in alto a destra
                                if (isSelected) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(20.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                } else if (style.isAnimated) {
                                    // Discreto badge scintilla per riconoscere subito gli stili animati
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(18.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bilanciamento automatico se una riga avesse meno di 3 elementi in futuro
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}