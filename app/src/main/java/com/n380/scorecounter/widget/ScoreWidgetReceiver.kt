package com.n380.scorecounter.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/*
 * ====================================================================
 * ANALISI DELLA TEORIA: IL BROADCAST RECEIVER
 * ====================================================================
 * Perche' serve questo file?
 * Il Widget vive sulla schermata Home (il Launcher)
 * e non dentro la nostra app. Per risparmiare la batteria del telefono,
 * Android "congela" le app in background.
 * * Se l'utente accende lo schermo o ridimensiona il widget, il sistema Android
 * urla un messaggio nell'etere (chiamato "Broadcast"). Il Launcher non puo'
 * parlare direttamente col nostro file grafico 'ScoreWidget' perche' e' dormiente.
 * * E' qui che entra in gioco il "Broadcast Receiver" (L'Antenna).
 * I Receiver sono componenti Android speciali che rimangono sempre in ascolto,
 * anche se l'app e' chiusa. Quando l'Antenna capta il segnale di Android,
 * si sveglia istantaneamente, prende il nostro disegno (ScoreWidget) e
 * lo passa al sistema per stamparlo a schermo.
 *
 * ====================================================================
 * ANALISI DELLA SINTASSI
 * ====================================================================
 * * class ScoreWidgetReceiver : GlanceAppWidgetReceiver()
 * - class ScoreWidgetReceiver: Il nome della nostra antenna.
 * - : GlanceAppWidgetReceiver(): Ereditiamo dalla super-antenna creata da Google
 * per Jetpack Glance. Sotto il cofano, questa super-antenna gestisce da sola
 * un sacco di burocrazia noiosa (come capire quando il widget viene creato,
 * aggiornato o cestinato dall'utente).
 */
class ScoreWidgetReceiver : GlanceAppWidgetReceiver() {
    /*
    * override val glanceAppWidget: GlanceAppWidget = ScoreWidget()
    * - override val: Sovrascriviamo una variabile obbligatoria richiesta dalla super-antenna.
    * - glanceAppWidget: E' come la "presa video" dell'antenna.
    * - = ScoreWidget(): Colleghiamo fisicamente l'antenna al file grafico che abbiamo
    * creato nello stesso package. Stiamo dicendo: "Quando ricevi un segnale per accenderti,
    * usa ESATTAMENTE il design scritto dentro ScoreWidget()".
    */
    // Collega l'antenna (Receiver) al disegno (Widget)
    override val glanceAppWidget: GlanceAppWidget = ScoreWidget()

}