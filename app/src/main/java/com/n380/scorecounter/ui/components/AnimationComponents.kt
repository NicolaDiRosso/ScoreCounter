package com.n380.scorecounter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ====================================================================
// LO "SLOT API" PATTERN E LE ANIMAZIONI RIUTILIZZABILI
// ====================================================================
// Creiamo un "guscio" animato.
// Il parametro `content: @Composable () -> Unit` è la "fessura" (slot) in cui
// potremo infilare qualsiasi bottone o testo vogliamo animare.
@Composable
fun SlideUpAnimatedVisibility(
    visible: Boolean, // L'interruttore: se è true appare, se è false scompare
    modifier: Modifier = Modifier, // Permette di applicare padding o dimensioni dall'esterno
    content: @Composable () -> Unit // Il blocco di grafica che andrà "dentro" l'animazione
) {
    // AnimatedVisibility è il motore di base di Compose per far apparire/scomparire le cose
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,

        // ----------------------------------------------------------------
        // ANIMAZIONE DI ENTRATA (Quando visible diventa true)
        // ----------------------------------------------------------------
        // Uniamo due effetti col simbolo '+': Scorrimento dal basso + Dissolvenza (Fade)
        enter = slideInVertically(
            // initialOffsetY ci chiede: "Da quale posizione verticale (Y) devo far partire l'oggetto?"
            // Compose ci fornisce in ingresso l'altezza in pixel calcolata dell'oggetto (altezzaTotale).
            // Dicendogli di partire da 'altezzaTotale', l'oggetto partirà esattamente un intero
            // "piano" più in basso rispetto a dove dovrebbe stare, risultando invisibile fuori dallo schermo.
            initialOffsetY = { altezzaTotale ->
                altezzaTotale
            },
            animationSpec = tween(
                durationMillis = 200,            // L'animazione dura 0.2 secondi
                easing = FastOutSlowInEasing     // Parte scattante e frena dolcemente alla fine
            )
        ) + fadeIn(
            // Contemporaneamente allo scorrimento, l'oggetto passa da trasparente a opaco
            animationSpec = tween(durationMillis = 200)
        ),

        // ----------------------------------------------------------------
        // ANIMAZIONE DI USCITA (Quando visible diventa false)
        // ----------------------------------------------------------------
        // Uniamo due effetti: Scorrimento verso il basso + Sparizione
        exit = slideOutVertically(
            // targetOffsetY ci chiede: "Verso quale posizione (Y) devo spingere l'oggetto per nasconderlo?"
            // Gli diciamo di spingerlo verso il basso di una distanza pari alla sua 'altezzaTotale'.
            targetOffsetY = { altezzaTotale ->
                altezzaTotale
            },
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutLinearInEasing // Parte veloce e mantiene velocità costante uscendo
            )
        ) + fadeOut(
            // Contemporaneamente allo scorrimento, l'oggetto svanisce diventando trasparente
            animationSpec = tween(durationMillis = 200)
        ),

        // ----------------------------------------------------------------
        // L'INIEZIONE DEL CONTENUTO (Lo Slot)
        // ----------------------------------------------------------------
        // Eseguiamo la funzione grafica che ci è stata passata in ingresso.
        content = {
            content()
        }
    )
}