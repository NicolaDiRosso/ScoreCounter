# 🎮 ScoreCounter - Il segnapunti che mancava

ScoreCounter è un'applicazione Android d'avanguardia, sviluppata nativamente in Kotlin con Jetpack Compose. Progettata con un estetica in Material You, l'app trasforma il modo in cui gestisci i punteggi di giochi da tavolo, tornei di carte o sfide amichevoli, eliminando carta, penna e calcoli errati.

## ✨ Caratteristiche Principali
- 💎 **Design System "Table-First":** Un'interfaccia coerente e moderna basata su una gerarchia visiva pulita, angoli stondati armonizzati (24dp/20dp) e un dock comandi ergonomico sempre a portata di pollice.
- 👑 **Corona del Leader & Hall of Fame:** Identificazione istantanea del vincitore in tempo reale con icone dinamiche e un effetto "Shimmer" (riflesso di luce) sulla card del campione a fine partita.
- 📈 **Analisi Vettoriale dei Punteggi:** Grafici lineari dinamici (ScoreChart) che mostrano l'andamento della sfida dallo 0% al 100% della durata, permettendo di visualizzare sorpassi e rimonte epiche.
- 🏅 **Sistema Premi Speciali:** L'engine di analisi assegna automaticamente titoli leggendari come:
  - 🎯 **Il Cecchino:** Per il colpo più letale (salto di punti maggiore).
  - 🔥 **L'Inarrestabile:** Per la striscia di punti consecutivi più lunga.
  - 🦞 **Il Gambero:** Per chi ha subito il maggior numero di penalità.
  - 🦅 **La Fenice:** Per la rimonta più incredibile dal punto più basso.
- 🎲 **Motore Dadi Universale:** Non un semplice D6. Configura il tuo dado virtuale (D6, D20, D100 o facce personalizzate) per spareggi o per decidere chi inizia.
- 📏 **Tipografia Adattiva (DPI-Ready):** Grazie alla tecnologia AutoResizedText, ogni titolo e punteggio si adatta matematicamente alla larghezza dello schermo, garantendo una leggibilità perfetta su qualsiasi dispositivo, indipendentemente dai DPI o dalle impostazioni dei font di sistema.
- 📊 **Statistiche Globali:** Una sezione dedicata ai record storici con card espandibili per analizzare ogni singola sessione passata nei minimi dettagli.
- ⏱️ **Cronometro Intelligente:** Tracciamento preciso della durata, con pausa automatica intelligente quando l'app va in background per preservare la batteria e l'integrità dei dati.
- 🛡️ **Sicurezza Atomica & UX Fluida:**
  - **Atomic Navigation Guard:** Protezione contro i click accidentali per evitare l'avvio di partite senza giocatori.
  - **Gestione Preferiti:** Pannello di gestione rapida dei giocatori con salvataggio persistente e blocco delle gesture per prevenire chiusure accidentali.
  - **Universal Undo:** Ripristino istantaneo di punteggi azzerati o giocatori rimossi per errore.

## 🛠️ Tecnologie Utilizzate
- **Linguaggio:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 Design)
- **Persistence:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) & [Gson](https://github.com/google/gson)
- **Grafica:** Canvas API per i coriandoli e le statistiche.

## 📸 Screenshot
<p align="center">
  <img width="250" alt="Home e Storico" src="https://github.com/user-attachments/assets/f82d8f3b-da7d-47a0-91cc-dfff905d33d5" />
 
  <img width="250" alt="Screenshot_20260415-002935" src="https://github.com/user-attachments/assets/5fe589db-c3d6-41e5-b02f-398b4078d1dd" />

</p>

<p align="center">
  <img width="250" alt="Popup Dado" src="https://github.com/user-attachments/assets/80ccafef-3aed-40a2-8e3a-f172f0beb355" />
  <img width="250" alt="Modifica Punteggio" src="https://github.com/user-attachments/assets/16b77a54-14b3-4fef-8f30-40c01098af60" />
  <img width="250" alt="Classifica Finale" src="https://github.com/user-attachments/assets/8625a19f-3108-48f3-8fd4-9d312c06e527" />
</p>

<p align="center">
  <img width="250" alt="Grafico Andamento" src="https://github.com/user-attachments/assets/c3bc9a2b-fb15-499e-9832-16dfda48fe99" />
  <img width="250" alt="Condivisione Storico" src="https://github.com/user-attachments/assets/9c37ffc3-7d7c-4b16-b45a-92ced0069d73" />
</p>


© 2026 Creato da NicolA380
