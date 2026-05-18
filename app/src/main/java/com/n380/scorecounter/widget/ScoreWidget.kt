package com.n380.scorecounter.widget

// ====================================================================
// SISTEMA ANDROID DI BASE
// ====================================================================
import android.content.Context // Lo "zaino" dell'app: fornisce accesso alle risorse di sistema e alle informazioni del telefono.
import android.content.Intent  // Il "biglietto" o messaggio usato per far comunicare componenti diversi (es. far aprire l'app al widget).

// ====================================================================
// UNITA' DI MISURA
// ====================================================================
import androidx.compose.ui.unit.dp // Definisce i "Density-independent Pixels", l'unità di misura flessibile che adatta le grandezze a tutti gli schermi.

// ====================================================================
// MOTORE GRAFICO GLANCE (Stile e Identificazione)
// ====================================================================
import androidx.glance.ColorFilter // Strumento per tingere dinamicamente immagini o icone (es. per adattarle al tema chiaro/scuro).
import androidx.glance.GlanceId    // Il codice identificativo univoco che il sistema assegna a ogni singolo widget piazzato sulla Home.
import androidx.glance.GlanceModifier // L'equivalente del Modifier di Compose, ma specifico per modellare l'aspetto in Glance.
import androidx.glance.GlanceTheme // Fornisce l'accesso ai colori dinamici di sistema (Material 3) per adattarsi al tema del telefono.

// ====================================================================
// COMPONENTI GRAFICI GLANCE (Immagini e Testi)
// ====================================================================
import androidx.glance.Image         // Il componente grafico che disegna fisicamente un'immagine o un'icona nel widget.
import androidx.glance.ImageProvider // Il "traduttore" che prende una risorsa di Android (es. un'icona nativa) e la rende leggibile al widget.
import androidx.glance.text.FontWeight // Permette di definire lo spessore visivo del testo (es. Grassetto, Normale).
import androidx.glance.text.Text       // Il componente grafico che stampa fisicamente le stringhe di testo a schermo.
import androidx.glance.text.TextStyle  // Il "pennello" per personalizzare il Text, definendone colore, stile e grandezza.

// ====================================================================
// STRUTTURA DEL WIDGET (Layout e Azioni)
// ====================================================================
import androidx.glance.action.clickable // Rende un elemento visibile interattivo, permettendo all'utente di cliccarlo o toccarlo.
import androidx.glance.appwidget.GlanceAppWidget // La classe madre assoluta da cui tutti i widget creati con Glance devono ereditare.
import androidx.glance.appwidget.action.actionStartActivity // L'azione specifica che dice al widget di lanciare un Intent (quindi aprire un'app).
import androidx.glance.appwidget.cornerRadius // Modificatore che taglia via gli spigoli di un elemento, creando l'effetto arrotondato.
import androidx.glance.appwidget.provideContent // Il "motore di rendering" che trasforma il tuo codice in elementi grafici reali sul display.

// ====================================================================
// MODIFICATORI E POSIZIONAMENTO (Layout)
// ====================================================================
import androidx.glance.background // Modificatore usato per "verniciare" lo sfondo di un elemento con un colore o un'immagine.
import androidx.glance.layout.Alignment // Serve per allineare gli elementi (es. centrarli perfettamente) all'interno del loro contenitore.
import androidx.glance.layout.Row       // Il contenitore strutturale che dispone i suoi elementi figli affiancati in riga orizzontale.
import androidx.glance.layout.fillMaxSize // Modificatore che ordina a un elemento di gonfiarsi per occupare tutto lo spazio che gli è concesso.
import androidx.glance.layout.padding   // Modificatore che aggiunge spazio "cuscinetto" interno per non far appiccicare il contenuto ai bordi.
import androidx.glance.layout.size      // Modificatore usato per forzare manualmente la larghezza e l'altezza di un elemento (es. l'icona).
import androidx.compose.ui.unit.sp
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
// ====================================================================
// LA TUA APP
// ====================================================================
import com.n380.scorecounter.MainActivity // Importa la schermata principale della tua app, così l'Intent sa esattamente quale destinazione "puntare".

/**
 * ====================================================================
 * IL NOSTRO WIDGET (La Grafica)
 * ====================================================================
 * Estendiamo 'GlanceAppWidget'. Questa classe dice ad Android:
 * "Ehi, non sono una schermata normale, sono un Widget per la Home!"
 * Il Widget, vive sulla schermata Home del telefono (il Launcher).
 * Il Launcher è un programma vecchio e rigido che capisce solo una lingua primordiale di Android chiamata RemoteViews.
 * Di conseguenza usiamo il linguaggio di Jetpack Glance (il nostro traduttore simultaneo).
 * Ci permette di scrivere codice molto simile a Compose (usando funzioni, colonne e testi),
 * ma sotto il cofano Glance prende quel codice e lo traduce in RemoteViews in modo che il Launcher possa stamparlo a schermo.
 */

/*
 * ====================================================================
 * ANALISI DELLA SINTASSI: LE PAROLE CHIAVE DI KOTLIN
 * ====================================================================
 * * class ScoreWidget : GlanceAppWidget()
 * - class: E' il "progetto" o lo "stampo". Definisce che stiamo creando un nuovo tipo di oggetto.
 * - ScoreWidget: E' il nome inventato da noi per questo specifico widget.
 * - : GlanceAppWidget(): I due punti significano "eredita da". Significa che non partiamo da zero,
 * ma prendiamo in prestito tutte le regole e i meccanismi di base che Google ha gia' scritto
 * dentro la classe madre GlanceAppWidget.
 */
class ScoreWidget : GlanceAppWidget() {

    /*
        * override suspend fun provideGlance(context: Context, id: GlanceId)
        * - override: Poiche' ereditiamo da GlanceAppWidget, andiamo a "sovrascrivere" (override) una
        * sua funzione vuota chiamata provideGlance, mettendoci dentro il nostro codice personalizzato.
        * - suspend: Significa "sospendibile". E' una funzione speciale che puo' mettere in pausa il
        * suo lavoro (ad esempio per fare calcoli o leggere la memoria) e riprenderlo in un secondo
        * momento senza mai far bloccare o "congelare" lo schermo del telefono.
        * - fun: Abbreviazione di "function" (funzione). Indica un blocco di codice che esegue un'azione.
        * - context: Context: E' lo "zaino" dell'app. Contiene il necessario per comunicare con il
        * sistema Android (permettendo ad esempio di usare Intent e risorse grafiche).
        * - id: GlanceId: E' il "codice identificativo" di questo specifico rettangolo. Se l'utente
        * mette due widget uguali sulla Home, avranno id diversi per essere distinti.
         */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        /*
        * provideContent { ... }
        * - Questa funzione e' il "motore grafico". Prende tutto il codice scritto nelle parentesi graffe
        * e lo converte in elementi grafici che verranno stampati sulla schermata Home dell'utente.
        */
        provideContent {
            // GlanceTheme avvolge il widget permettendogli di leggere i colori dinamici
            // impostati dall'utente sul suo telefono (es. Modalita' Chiara/Scura e colori sfondo)
            GlanceTheme {

                // IL BIGLIETTO VIP (Intent)
                // Diamo un'istruzione ad Android: "Quando il widget viene cliccato,
                // apri il file MainActivity della nostra app".
                // L'azione "ACTION_NEW_MATCH" e' una parola d'ordine che inviamo alla
                // MainActivity. Piu' avanti le insegneremo a riconoscere questa parola d'ordine
                // per fargli saltare la schermata iniziale e andare direttamente alla creazione sfida.
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = "ACTION_NEW_MATCH"
                }

                // ==========================================================
                // 📱 SCHEDINA CONTENITORE (Segue il Material You dello sfondo)
                // ==========================================================
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize() // Prende tutto lo spazio del widget (3x1)
                        .cornerRadius(24.dp) // Angoli arrotondati standard per i widget moderni
                        .background(GlanceTheme.colors.background) // Sfondo dinamico Material You
                        .padding(12.dp), // Spazio interno di sicurezza
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ==========================================================
                    // 1. INTESTAZIONE: ICONA + TITOLO (Affiancati in una Row)
                    // ==========================================================
                    Row(
                        modifier = GlanceModifier
                        .fillMaxWidth() // Si allarga su tutto il widget
                        .padding(bottom = 8.dp, start = 4.dp), // Aggiunto un briciolo di spazio a sinistra per simmetria
                        verticalAlignment = Alignment.CenterVertically, // Centra verticalmente icona e testo
                        horizontalAlignment = Alignment.Start // Mantiene il blocco unito al centro
                    ) {

                        // L'ICONA UFFICIALE DELLA TUA APP
                        // Usiamo ImageProvider per pescare la risorsa 'mipmap' (l'icona di lancio dell'app)
                        Image(
                            provider = ImageProvider(com.n380.scorecounter.R.mipmap.ic_launcher),
                            contentDescription = "Logo App",
                            modifier = GlanceModifier
                                .size(20.dp) // Dimensione piccola e discreta per l'intestazione
                                .padding(end = 6.dp) // Spazio di distacco dal testo alla sua destra
                        )

                        // IL TESTO DEL TITOLO
                        Text(
                            text = "ScoreCounter",
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onBackground,
                            ),
                            maxLines = 1, // Impedisce tassativamente di andare a capo in verticale
                        )
                    }

                    // ==========================================================
                    // 🕹️ IL PULSANTE ADATTIVO (Si allarga/stringe col widget)
                    // ==========================================================
                    // Usiamo una Row come pulsante. Avendo ".fillMaxWidth()", se l'utente
                    // allarga il widget sulla Home, il pulsante si adattera' allungandosi da solo!
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth() // Si adatta orizzontalmente alla larghezza della griglia
                            .defaultWeight() // Altezza standard comoda per il tocco del dito
                            .cornerRadius(16.dp) // Angoli del pulsante leggermente meno curvi della scheda esterna
                            .background(GlanceTheme.colors.primaryContainer) // Colore del pulsante dinamico basato sul Material You
                            .padding(horizontal = 12.dp)
                            // La cliccabilità la diamo SOLO ed esclusivamente al pulsante, non a tutto il widget!
                            .clickable(actionStartActivity(intent)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally // Centra perfettamente il testo "+ Nuova Sfida" al suo interno
                    ) {

                        // Il testo unico dentro al pulsante, come mi hai chiesto
                        Text(
                            text = "+ Nuova Sfida",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,// Dimensione di partenza ottimale
                                color = GlanceTheme.colors.onPrimaryContainer // Testo a contrasto dentro al pulsante
                            ),
                            maxLines = 1, // Impedisce tassativamente di andare a capo in verticale
                        )
                    }
                }
            }
        }
    }
}