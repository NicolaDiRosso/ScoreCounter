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
      <img src="https://github.com/user-attachments/assets/df7741bb-2b94-440a-b965-d610b47dc48c" width="250" alt="Home Screen con sfide rapide"/>
    </td>
     <td align="center">
      <b>Storico per ogni partita</b><br>
      <img src="https://github.com/user-attachments/assets/ccd5de6b-24e9-427c-a597-1b149f8f7e5f" width="250" alt="Storico partite e record assoluti"/>
    </td>
    <td align="center">
      <b>Statistiche globali</b><br>
      <img src="https://github.com/user-attachments/assets/ee48b1dc-3f4c-43c4-a7d2-dbea1757c582" width="250" alt="Storico partite e record assoluti"/>
    </td>
  </tr>
</table>

### ⚙️ 2. Creazione della sfida
*Aggiungi il nome della sfida (anche con il completamento automatico), scegli le tue regole, poi inserisci i giocatori e scegli i colori.*
<table>
  <tr>
    <td align="center">
      <b>Configurazione Partita</b><br>
      <img src="https://github.com/user-attachments/assets/05e27699-671b-415b-bb9f-c959a237ed23" width="250" alt="Schermata di creazione match"/>
    </td>
    <td align="center">
      <b>Personalizzazione del dado</b><br>
      <img src="https://github.com/user-attachments/assets/e34acb2b-0309-4ac6-996d-6668cb54226b" width="250" alt="Storico partite e record assoluti"/>
    </td>
    <td align="center">
      <b>Gestione Giocatori</b><br>
      <img src="https://github.com/user-attachments/assets/df6aa4cc-b24b-440e-b531-ceca79a3409c" width="250" alt="Pannello gestione giocatori preferiti"/>
    </td>
  </tr>
</table>

### 🎲 3. La Sfida (Contatore)
*Design "Table-First" per non perdere mai il focus: tieni i punti con ergonomia, tieni d'occhio le combo e usa gli strumenti integrati.*
<table>
  <tr>
    <td align="center">
      <b>Contatore e Combo</b><br>
      <img src="https://github.com/user-attachments/assets/b7fbe4c9-f7b7-4270-b2a4-0264a6a1ab6f" width="250" alt="Match in corso con effetto On Fire"/>
    </td>
    <td align="center">
      <b>Strumenti integrati</b><br>
      <img src="https://github.com/user-attachments/assets/802a9101-cd97-42aa-9426-c4840f869f46" width="250" alt="Popup del dado" />
    </td>
  </tr>
</table>

### 🏆 4. Risultati e Analisi
*Molto più di un semplice punteggio: scopri chi si aggiudica i premi speciali e analizza l'andamento della gara sul grafico temporale.*
<table>
  <tr>
    <td align="center">
      <b>Classifica finale con grafico della partita</b><br>
      <img src="https://github.com/user-attachments/assets/843155fa-271c-4549-a7b8-9cb3fc681bbe" width="250" alt="Classifica finale con grafico della partita"/>
    </td>
    <td align="center">
      <b>Gratification con medaglie scherzose</b><br>
      <img src="https://github.com/user-attachments/assets/3d179504-7782-4f0b-bcbf-120a1e01566b" width="250" alt="Gratification con medaglie scherzose"/>
    </td>
  </tr>
</table>

© 2026 Creato da NicolA380
