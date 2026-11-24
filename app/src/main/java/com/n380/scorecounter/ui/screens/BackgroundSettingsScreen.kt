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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
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
                        // Colonna Sinistra (45% W): Anteprima Live con Mini-Card Segnapunti
                        Box(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
                            LiveBackgroundPreviewCard(viewModel = viewModel)
                        }

                        // Colonna Destra (55% W): Griglia delle Opzioni Sfondo
                        Box(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            BackgroundOptionsGrid(viewModel = viewModel)
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

                        // 2b. Griglia delle Opzioni Sfondo
                        BackgroundOptionsGrid(viewModel = viewModel)
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
 * Mostra una card interna (`surface`) contenente lo sfondo reale attivo (`AppBackground`)
 * e una mini-card segnapunti di esempio nello stile nativo di ScoreCounter.
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

            // Box di Anteprima con Mini-Card Esempio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Sfondo reale attivo
                AppBackground(style = viewModel.backgroundStyle)

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
                        .height(74.dp),
                    onScoreChange = { },
                    onScoreClick = { }
                )
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE: GRIGLIA OPZIONI SFONDO (BackgroundOptionsGrid)
 * ====================================================================
 * 🧠 SPIEGAZIONE DIDATTICA:
 * Mappa dinamica di tutti i 6 stili dell'enum `BackgroundStyle`,
 * organizzata in 3 righe simmetriche da 2 colonne ciascuna.
 */
@Composable
private fun BackgroundOptionsGrid(
    viewModel: MatchViewModel
) {
    val haptic = LocalHapticFeedback.current

    // Mappatura completa dei 6 stili di sfondo con titolo localizzato e icona rappresentativa
    val backgroundOptions = listOf(
        BackgroundStyle.ICONS to Pair(stringResource(R.string.sfondo_icone), Icons.Filled.Palette),
        BackgroundStyle.SOLID to Pair(stringResource(R.string.sfondo_tinta_unita), Icons.Filled.Visibility),
        BackgroundStyle.GRADIENT to Pair(stringResource(R.string.sfondo_sfumatura), Icons.Filled.Gradient),
        BackgroundStyle.DOTS to Pair(stringResource(R.string.sfondo_puntini), Icons.Filled.MoreHoriz),
        BackgroundStyle.GEOMETRIC to Pair(stringResource(R.string.sfondo_geometrico), Icons.Filled.GridOn),
        BackgroundStyle.WAVES to Pair(stringResource(R.string.sfondo_onde), Icons.Filled.Waves)
    )

    // Suddividiamo i 6 sfondi in 3 righe perfettamente pari da 2 colonne ciascuna
    val rows = backgroundOptions.chunked(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { (style, labelAndIcon) ->
                    val isSelected = (viewModel.backgroundStyle == style)
                    val (label, _) = labelAndIcon

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(132.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                viewModel.setBackgroundStylePreference(style.ordinal)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected)
                            BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                        else
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Miniatura dello Sfondo
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppBackground(style = style)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Etichetta del Nome Sfondo
                                AutoResizedText(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Badge Spunta Selezionato (✓) in alto a destra
                            if (isSelected) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Se una riga avesse un solo elemento in futuro (numero dispari), bilanciamo lo spazio
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}