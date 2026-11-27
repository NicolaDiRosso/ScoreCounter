package com.n380.scorecounter.ui.screens.landscape

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
 * VISTA CONFRONTO DIRETTO "DUELLI" IN ORIZZONTALE (MASTER-DETAIL LANDSCAPE)
 * ====================================================================
 * 🧠 CONCETTI TEORICI CHIAVE DI JETPACK COMPOSE (MASTER-DETAIL):
 *
 * 1. ARCHITETTURA A 2 PANNELLI COERENTE CON GlobalStatsScreenLandscape:
 *    - PANNELLO SINISTRO (`VsComparisonLeftPanel`, weight = 0.36f):
 *      Sostituisce la tavola "Campione Assoluto + Riepilogo Generale" con i
 *      selettori P1 <-> P2 e il tabellone sintetico degli scontri diretti.
 *    - PANNELLO DESTRO (`VsComparisonRightPanel`, weight = 0.64f):
 *      Sostituisce la griglia dei record storici con coppie di card affiancate
 *      ad altezza sincronizzata e la tabella dei premi condivisi in basso.
 *
 * 2. SINCRONIZZAZIONE ALTEZZA RIGHE (`IntrinsicSize.Max` + `fillMaxHeight()`):
 *    In una griglia tradizionale (`LazyVerticalGrid`), card con sottotitoli di
 *    lunghezza diversa assumono altezze differenti lasciando gradini vuoti.
 *    Raggruppando le card a coppie dentro `Row(Modifier.height(IntrinsicSize.Max))`,
 *    ogni card si adatta all'altezza della sua vicina creando una griglia simmetrica.
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
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Modalità compact = true per lasciare visibile anche l'intero tabellone sottostante
                VsPlayerSelectorCard(
                    availablePlayers = availablePlayers,
                    selectedPlayer1 = selectedPlayer1,
                    onPlayer1Selected = onPlayer1Selected,
                    selectedPlayer2 = selectedPlayer2,
                    onPlayer2Selected = onPlayer2Selected,
                    compact = true
                )

                if (stats.sharedMatchesCount > 0) {
                    VsScoreboardCard(
                        stats = stats,
                        compact = true
                    )
                }
            }
        }
    }
}

/**
 * Pannello Detail (Destra): ospita le metriche incrociate in righe ad altezza sincronizzata
 * e la tabella comparativa dei premi condivisi a tutta larghezza.
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // RIGA 1: Media punti + Vittorie assolute al tavolo (altezza sincronizzata via IntrinsicSize.Max)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VsDualMetricCard(
                            title = stringResource(R.string.titolo_media_punti_vs),
                            subtitle = stringResource(R.string.desc_media_punti_vs),
                            p1Name = stats.player1Name,
                            p1Color = stats.player1Color,
                            p1Value = String.format(Locale.getDefault(), "%.1f", stats.p1AverageScore),
                            p2Name = stats.player2Name,
                            p2Color = stats.player2Color,
                            p2Value = String.format(Locale.getDefault(), "%.1f", stats.p2AverageScore),
                            p1Highlights = stats.p1AverageScore > stats.p2AverageScore,
                            p2Highlights = stats.p2AverageScore > stats.p1AverageScore,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

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
                            p2Highlights = stats.p2OverallMatchWins > stats.p1OverallMatchWins,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // RIGA 2: Scarto record + Combo On Fire (altezza sincronizzata via IntrinsicSize.Max)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VsBiggestMarginCard(
                            stats = stats,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        VsDualMetricCard(
                            title = stringResource(R.string.titolo_combo_fuoco_vs),
                            subtitle = stringResource(R.string.desc_combo_fuoco_vs),
                            p1Name = stats.player1Name,
                            p1Color = stats.player1Color,
                            p1Value = "${stats.p1OnFireTotal}",
                            p2Name = stats.player2Name,
                            p2Color = stats.player2Color,
                            p2Value = "${stats.p2OnFireTotal}",
                            p1Highlights = stats.p1OnFireTotal > stats.p2OnFireTotal,
                            p2Highlights = stats.p2OnFireTotal > stats.p1OnFireTotal,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // RIGA 3: Premi conquistati insieme (a tutta larghezza in fondo)
                item {
                    VsAwardsComparisonCard(stats = stats)
                }
            }
        }
    }
}