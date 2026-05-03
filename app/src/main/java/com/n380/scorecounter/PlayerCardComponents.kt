package com.n380.scorecounter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * ====================================================================
 * COMPONENTE: PlayerAtTableCard (Stile Material You Accent)
 * ====================================================================
 * Questa card viene usata nella schermata di Creazione Sfida.
 * Presenta uno sfondo neutro e una "Accent Bar" laterale che
 * mostra il colore del giocatore, per un design minimale e pulito.
 * ====================================================================
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerAtTableCard(
    player: Player,
    isFirst: Boolean, // Serve per disabilitare la freccia "Su" se è il primo
    isLast: Boolean,  // Serve per disabilitare la freccia "Giù" se è l'ultimo
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onColorChange: (Int) -> Unit
) {
    var haptic = LocalHapticFeedback.current //QUESTA VARIABILE DA PROBLEMI, LO DEVO RISOLVERE ALTRIMENTI MI RITROIVO DELLE VIBRAZIONI DOPPIE
    var showColorPalette by remember { mutableStateOf(false) }

    // Trasforma il colore salvato (Int) in un colore grafico (Color)
    val playerColor = Color(player.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            // combinedClickable gestisce il tocco lungo sulla card per aprire la tavolozza dei colori
            .combinedClickable(
                onClick = {
                    showColorPalette = true
                }
            ),
        // ---> Sfondo Colorato Trasparente (Tonal Surface) <---
        // Prendiamo il colore del giocatore e lo rendiamo trasparente al 12% (0.12f)
        // Questo crea un "bagliore" del colore del giocatore molto elegante.
        colors = CardDefaults.cardColors(
            containerColor = playerColor.copy(alpha = 0.30f)
        ),

        // ---> Bordo coordinato <---
        // Anche il bordo lo facciamo del colore del giocatore ma un po' più visibile (20%)
        border = BorderStroke(1.dp, playerColor.copy(alpha = 0.5f)),

        shape = RoundedCornerShape(16.dp)
    ) {
        // La riga principale che contiene tutto
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp), // Altezza fissa per coerenza
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ---> IL BORDO LATERALE DI ACCENTO <---
            // Questo è il rettangolo colorato a sinistra (L'Accent Bar).
            // Lo ritagliamo (clip) in modo che segua la curva della Card, ma solo sul lato sinistro!
            Box(
                modifier = Modifier
                    .width(12.dp) // Spessore della barra laterale
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(playerColor) // Colore puro del giocatore
            )

            // Il resto dei controlli (Frecce, Nome, Bottoni)
            Row(
                modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. FRECCE SU/GIU
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        Icon(Icons.Filled.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // 2. IL NOME DEL GIOCATORE
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, // Testo dello stesso colore della barra laterale!
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )

                // 3. TASTO MODIFICA (Matita)
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onEdit()
                }) {
                    Icon(Icons.Filled.Edit, "Modifica", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 4. TASTO ELIMINA (Cestino)
                IconButton(onClick = {
                    //haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                }) {
                    Icon(Icons.Filled.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // LAYER MODALE: POPUP SCELTA COLORE
    if (showColorPalette) {
        AlertDialog(
            onDismissRequest = { showColorPalette = false },
            title = {
                Text(
                    text = "Colore per ${player.name}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // ---> MODIFICA 1: LA GRIGLIA INTELLIGENTE <---
                // Usiamo LazyVerticalGrid per creare una scacchiera di colori
                LazyVerticalGrid(
                    // Adaptive(48.dp): Calcola in automatico quante palline larghe 48dp
                    // riescono a stare in una riga in base alla larghezza dello schermo.
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    // Spazio verticale e orizzontale tra un pallino e l'altro
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    // Mettiamo un limite di altezza, così se hai 30 colori la finestra
                    // non esce fuori dallo schermo ma diventa scorrevole internamente.
                    modifier = Modifier.padding(top = 16.dp).heightIn(max = 250.dp)
                ) {
                    // Generiamo i pallini
                    items(playerPalette) { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp) // Leggermente più grandi di prima per un tap più facile
                                .clip(RoundedCornerShape(20.dp))//// Applicando 20.dp manteniamo la coerenza esatta con la tavolozza principale
                                .background(color)
                                .clickable {
                                    onColorChange(color.toArgb())
                                    showColorPalette = false
                                }
                        )
                    }
                }
            },
            // ---> MODIFICA 2: IL VERO BOTTONE <---
            // Spostiamo il bottone nel parametro 'confirmButton'
            confirmButton = {
                Button(
                    onClick = {
                        showColorPalette = false
                    },
                    modifier = Modifier
                        .fillMaxWidth() // Si allarga per tutta la base del popup
                        .height(48.dp), // Altezza comoda da premere
                    shape = RoundedCornerShape(20.dp) // La tua stondatura classica!
                ) {
                    Text("Chiudi", fontWeight = FontWeight.Bold)
                }
            },
            // Diciamo ad Android di non usare il bottone secondario invisibile
            dismissButton = null
        )
    }
}


/**
 * ====================================================================
 * COMPONENTE: PlayerScoreCard (Stile Material You Accent)
 * ====================================================================
 * Questa card viene usata nella schermata della Sfida in corso.
 * Mantiene il look minimale con la Accent Bar, ma include i controlli
 * "Gamepad" (+ e -) per gestire i punteggi.
 * ====================================================================
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScoreCard(
    player: Player,
    isLeader: Boolean = false,
    isFireEnabled: Boolean = true,
    onScoreChange: (Int) -> Unit,
    onScoreClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val playerColor = Color(player.color)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = playerColor.copy(alpha = 0.30f)),
        border = BorderStroke(1.dp, playerColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ---> IL BORDO LATERALE DI ACCENTO <---
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(playerColor)
            )

            // ---> CONTENUTO DELLA CARD <---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. BLOCCO NOME E CORONA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(
                        text = player.name,
                        // ---> MODIFICA 1: Font del nome ridotto da headlineSmall a titleLarge <---
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isLeader) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "In Vantaggio",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp).size(28.dp)
                        )
                    }
                }

                // 2. BLOCCO PULSANTI E PUNTEGGIO
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // PULSANTE MENO
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    onScoreChange(-1)
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onScoreChange(-5)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            "Diminuisci",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // ---> MODIFICA 2: PUNTEGGIO DINAMICO <---
                    val scoreText = player.score.toString()
                    val scoreStyle = when {
                        scoreText.length <= 2 -> MaterialTheme.typography.displayMedium // Fino a 99: Gigante
                        scoreText.length == 3 -> MaterialTheme.typography.headlineMedium // Fino a 999: Medio
                        else -> MaterialTheme.typography.titleLarge // Oltre 1000: Piccolo
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            // ---> MODIFICA 3: Zona di rispetto (Stabilità) <---
                            // Fissiamo una larghezza minima (70dp) così i pulsanti + e - non "ballano"
                            .widthIn(min = 70.dp)
                            .padding(horizontal = 8.dp) // Ridotto padding da 16 a 8
                            .clickable { onScoreClick() }
                    ) {
                        Text(
                            text = scoreText,
                            style = scoreStyle, // Applichiamo lo stile che cambia da solo!
                            fontWeight = FontWeight.Black,
                            color = if (player.isOnFire && isFireEnabled) Color(0xFFF3AF38) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    // PULSANTE PIÙ
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    onScoreChange(1)
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onScoreChange(10)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            "Aumenta",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

