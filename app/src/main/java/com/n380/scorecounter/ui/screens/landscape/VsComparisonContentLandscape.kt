package com.n380.scorecounter.ui.screens.landscape

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n380.scorecounter.R
import com.n380.scorecounter.model.HeadToHeadStats
import com.n380.scorecounter.model.VsPlayerOption
import com.n380.scorecounter.ui.screens.VsAwardsComparisonCard
import com.n380.scorecounter.ui.screens.VsBiggestMarginCard
import com.n380.scorecounter.ui.screens.VsDualMetricCard
import com.n380.scorecounter.ui.screens.VsEmptyStateCard
import com.n380.scorecounter.ui.screens.VsPlayerSelectorCard
import com.n380.scorecounter.ui.screens.VsScoreboardCard
import java.util.Locale

/**
 * ====================================================================
 * VISTA CONFRONTO DIRETTO "VS" IN ORIZZONTALE (MASTER-DETAIL LANDSCAPE)
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE DI JETPACK COMPOSE (MASTER-DETAIL):
 *
 * 1. ARCHITETTURA A 2 PANNELLI COERENTE CON GlobalStatsScreenLandscape:
 *    - PANNELLO SINISTRO (`VsComparisonLeftPanel`, weight = 0.36f):
 *      Sostituisce la tavola "Campione Assoluto + Riepilogo Generale" con i
 *      selettori P1 <-> P2 e il tabellone sintetico degli scontri diretti.
 *    - PANNELLO DESTRO (`VsComparisonRightPanel`, weight = 0.64f):
 *      Sostituisce la griglia dei record storici con la griglia adattiva
 *      (`GridCells.Adaptive`) delle metriche comparative e dei premi condivisi.
 *
 * 2. GRID ITEM SPAN (`GridItemSpan(maxLineSpan)`):
 *    Permette alla tabella dei premi condivisi di occupare l'intera larghezza
 *    della griglia a 2 colonne in fondo all'elenco, evitando compressioni.
 * ====================================================================
 */

/**
 * Pannello Master (Sinistra): ospita la scelta dei due giocatori e il tabellone principale VS.
 */
@Composable
fun VsComparisonLeftPanel(
    availablePlayers: List<VsPlayerOption>,
    selectedPlayer1: String?,
    onPlayer1Selected: (String) -> Unit,
    selectedPlayer2: String?,
    onPlayer2Selected: (String) -> Unit,
    stats: HeadToHeadStats?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        if (availablePlayers.size < 2 || selectedPlayer1 == null || selectedPlayer2 == null || stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                VsEmptyStateCard(
                    title = stringResource(R.string.titolo_vs_pochi_giocatori),
                    description = stringResource(R.string.desc_vs_pochi_giocatori),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VsPlayerSelectorCard(
                    availablePlayers = availablePlayers,
                    selectedPlayer1 = selectedPlayer1,
                    onPlayer1Selected = onPlayer1Selected,
                    selectedPlayer2 = selectedPlayer2,
                    onPlayer2Selected = onPlayer2Selected
                )

                if (stats.sharedMatchesCount > 0) {
                    VsScoreboardCard(stats = stats)
                }
            }
        }
    }
}

/**
 * Pannello Detail (Destra): ospita la griglia adattiva delle metriche incrociate e i premi condivisi.
 */
@Composable
fun VsComparisonRightPanel(
    availablePlayers: List<VsPlayerOption>,
    stats: HeadToHeadStats?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        if (availablePlayers.size < 2 || stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                VsEmptyStateCard(
                    title = stringResource(R.string.titolo_vs_pochi_giocatori),
                    description = stringResource(R.string.desc_vs_pochi_giocatori),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (stats.sharedMatchesCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                VsEmptyStateCard(
                    title = stringResource(R.string.titolo_vs_nessuna_sfida),
                    description = stringResource(
                        R.string.desc_vs_nessuna_sfida,
                        stats.player1Name,
                        stats.player2Name
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Media punti negli scontri diretti
                item {
                    VsDualMetricCard(
                        title = stringResource(R.string.titolo_media_punti_vs),
                        p1Name = stats.player1Name,
                        p1Color = stats.player1Color,
                        p1Value = String.format(Locale.getDefault(), "%.1f", stats.p1AverageScore),
                        p2Name = stats.player2Name,
                        p2Color = stats.player2Color,
                        p2Value = String.format(Locale.getDefault(), "%.1f", stats.p2AverageScore),
                        p1Highlights = stats.p1AverageScore > stats.p2AverageScore,
                        p2Highlights = stats.p2AverageScore > stats.p1AverageScore
                    )
                }

                // 2. Vittorie assolute al tavolo
                item {
                    VsDualMetricCard(
                        title = stringResource(R.string.titolo_vittorie_tavolo_vs),
                        subtitle = stringResource(R.string.desc_vittorie_tavolo_vs),
                        p1Name = stats.player1Name,
                        p1Color = stats.player1Color,
                        p1Value = "${stats.p1OverallMatchWins}",
                        p2Name = stats.player2Name,
                        p2Color = stats.player2Color,
                        p2Value = "${stats.p2OverallMatchWins}",
                        p1Highlights = stats.p1OverallMatchWins > stats.p2OverallMatchWins,
                        p2Highlights = stats.p2OverallMatchWins > stats.p1OverallMatchWins
                    )
                }

                // 3. Scarto record nello stesso match
                item {
                    VsBiggestMarginCard(stats = stats)
                }

                // 4. Combo On Fire nelle sfide condivise
                item {
                    VsDualMetricCard(
                        title = stringResource(R.string.titolo_combo_fuoco_vs),
                        p1Name = stats.player1Name,
                        p1Color = stats.player1Color,
                        p1Value = "${stats.p1OnFireTotal}",
                        p2Name = stats.player2Name,
                        p2Color = stats.player2Color,
                        p2Value = "${stats.p2OnFireTotal}",
                        p1Highlights = stats.p1OnFireTotal > stats.p2OnFireTotal,
                        p2Highlights = stats.p2OnFireTotal > stats.p1OnFireTotal
                    )
                }

                // 5. Premi conquistati insieme (a tutta larghezza con GridItemSpan(maxLineSpan))
                item(span = { GridItemSpan(maxLineSpan) }) {
                    VsAwardsComparisonCard(stats = stats)
                }
            }
        }
    }
}

