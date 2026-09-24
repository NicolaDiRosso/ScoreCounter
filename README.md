# 🎮 ScoreCounter - Il segnapunti che mancava

ScoreCounter è un'applicazione Android, sviluppata nativamente in Kotlin con Jetpack Compose. Progettata con un estetica in Material You che quindi cambia con i colori di sistema del proprio smarphone, l'app trasforma il modo in cui gestisci i punteggi di giochi da tavolo, tornei di carte o sfide amichevoli, eliminando carta, penna e calcoli errati.

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
- 🛡️ **Affidabilità & UX Fluida:**
  - **Atomic Navigation Guard:** Protezione contro i click accidentali per evitare l'avvio di partite senza giocatori.
  - **Gestione Preferiti:** Pannello di gestione rapida dei giocatori con salvataggio persistente e blocco delle gesture per prevenire chiusure accidentali.
  - **Universal Undo:** Ripristino istantaneo di punteggi azzerati o giocatori rimossi per errore.

## 🛠️ Tecnologie Utilizzate
- **Linguaggio:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3 Design)
- **Persistence:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) & [Gson](https://github.com/google/gson)
- **Grafica:** Canvas API per i coriandoli e le statistiche.

## 📸 Galleria dell'Applicazione

### 🏠 1. Home e Storico
*Il centro di comando: avvia sfide rapide con un tocco o rivivi le glorie passate esplorando la tua Hall of Fame personale.*
<table>
  <tr>
    <td align="center">
      <b>Homepage con storico</b><br>
      <img src="https://github.com/user-attachments/assets/b44950ca-88a9-4a41-8a04-b12302891a9e" width="250" alt="Home Screen con sfide rapide"/>
    </td>
     <td align="center">
      <b>Storico per ogni partita</b><br>
      <img src="https://github.com/user-attachments/assets/7e1b3ab3-dee8-4dbe-8bdb-303f9e445b4f" width="250" alt="Storico partite e record assoluti"/>
    </td>
    <td align="center">
      <b>Statistiche globali</b><br>
      <img src="https://github.com/user-attachments/assets/b3d643d4-c0c9-413c-9e8a-e4f36ff28b01" width="250" alt="Storico partite e record assoluti"/>
    </td>
  </tr>
</table>

### ⚙️ 2. Creazione della sfida
*Aggiungi il nome della sfida (anche con il completamento automatico), scegli le tue regole, poi inserisci i giocatori e scegli i colori.*
<table>
  <tr>
    <td align="center">
      <b>Configura la partita</b><br>
      <img src="https://github.com/user-attachments/assets/1c6c53cb-6970-4330-a173-8cca3ec6de0c" width="250" alt="Schermata di creazione match"/>
    </td>
    <td align="center">
      <b>Personalizza ogni giocatore</b><br>
      <img src="https://github.com/user-attachments/assets/3ab525cf-1d63-4a7a-96bf-a5cc16bf1d29" width="250" alt="Storico partite e record assoluti"/>
    </td>
    <td align="center">
      <b>Gestisci i giocatori salvati</b><br>
      <img src="https://github.com/user-attachments/assets/6c345fdb-e0a2-46e6-9bb1-f0814dd4f445" width="250" alt="Pannello gestione giocatori preferiti"/>
    </td>
  </tr>
</table>

### 🎲 3. La Sfida (Contatore)
*Design "Table-First" per non perdere mai il focus: tieni i punti con ergonomia, tieni d'occhio le combo e usa gli strumenti integrati.*
<table>
  <tr>
    <td align="center">
      <b>Tieni il conto dei punti</b><br>
      <img src="https://github.com/user-attachments/assets/f715bbae-0cb8-4657-b3d7-e80fa3af5c2a" width="250" alt="Match in corso con effetto On Fire"/>
    </td>
    <td align="center">
      <b>Metti pressione con il timer</b><br>
      <img src="https://github.com/user-attachments/assets/e907778b-5c94-4e69-aa69-34ff862fc59b" width="250" alt="Popup del dado" />
    </td>
  <td align="center">
      <b>Risolvi le dispute con il dado</b><br>
      <img src="https://github.com/user-attachments/assets/1c88bbe4-e734-4e55-ad4a-4a4c03d19b09" width="250" alt="Popup del dado" />
    </td>
  </tr>
</table>

### 🏆 4. Risultati e Analisi
*Molto più di un semplice punteggio: scopri chi si aggiudica i premi speciali e analizza l'andamento della gara sul grafico temporale e condividilo con gli amici.*
<table>
  <tr>
    <td align="center">
      <b>Osserva la i risultati con il grafico della partita</b><br>
      <img src="https://github.com/user-attachments/assets/b2a1f603-88b2-444a-a206-5ec46777c449" width="250" alt="Classifica finale con grafico della partita"/>
    </td>
    <td align="center">
      <b>Gratification con medaglie scherzose</b><br>
      <img src="https://github.com/user-attachments/assets/9b057635-275f-43ff-b18a-307d49fb62a3" width="250" alt="Gratification con medaglie scherzose"/>
    </td>
  <td align="center">
      <b>Comprendi le medaglie</b><br>
      <img src="https://github.com/user-attachments/assets/0162adee-4cae-4db4-9292-2442d4148da2" width="250" alt="Gratification con medaglie scherzose"/>
    </td>
  </tr>
</table>

© 2026 Creato da NicolA380
