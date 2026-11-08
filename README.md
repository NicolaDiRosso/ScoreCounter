# 🎮 ScoreCounter

> Un segnapunti universale, trasparente e in perfetto stile Material You.

ScoreCounter nasce con una filosofia chiara: offrire uno strumento universale, rapido e senza fronzoli per tenere traccia dei punteggi di qualsiasi gioco da tavolo, torneo di carte o sfida in compagnia. L'obiettivo è permetterti di concentrarti sulla partita, delegando i calcoli a un'interfaccia progettata per essere immediata e intuitiva. 

Interamente sviluppata seguendo le linee guida del **Material Design 3 (Material You)**, l'app si adatta dinamicamente ai colori di sistema del tuo dispositivo, garantendo un'esperienza visiva coerente, moderna e arricchita da feedback aptici (vibrazioni) per ogni interazione.

## 🎯 Esperienza di Gioco
L'applicazione evita l'inserimento di opzioni superflue che potrebbero distrarre dal gioco, offrendo esattamente ciò che serve quando serve:
* **Gestione Flessibile:** Che sia una partita a due o un torneo di gruppo, puoi aggiungere un numero illimitato di giocatori. È possibile richiamare istantaneamente i giocatori già salvati in rubrica o crearne di nuovi sul momento, assegnando punteggi senza alcun limite numerico.
* **Strumenti al Tavolo:** Durante la partita, hai a disposizione strumenti essenziali e non invasivi, come un **timer personalizzabile** per gestire i turni e un **dado virtuale** (anch'esso configurabile) per risolvere spareggi o decidere chi inizia. 
* **Tutto Sotto Controllo:** L'interfaccia principale è studiata per permettere tocchi rapidi e sicuri, riducendo al minimo gli errori di digitazione durante le fasi più concitate.

## 📊 Analisi e Condivisione
Alla fine della partita, ScoreCounter trasforma i semplici numeri in un vero e proprio resoconto sportivo:
* **Grafici Interattivi:** Analizza l'andamento della sfida attraverso un grafico vettoriale dettagliato. La funzione di zoom temporale permette di ingrandire e analizzare quarti specifici della partita per studiare rimonte e sorpassi.
* **Assegnazione Premi:** Il motore interno valuta matematicamente la partita e assegna automaticamente titoli divertenti in base alle prestazioni reali (es. il colpo più alto, la striscia positiva, la rimonta più grande).
* **Statistiche Globali:** Una sezione dedicata permette di consultare lo storico di tutte le partite passate e visualizzare le statistiche generali accumulate nel tempo.
* **Condivisione:** I risultati finali, i premi e l'andamento della partita possono essere condivisi facilmente con gli amici al termine della sfida.

## 🛡️ Etica, Privacy e Trasparenza
ScoreCounter è costruita attorno al rispetto assoluto per l'utente. 
* **100% Offline e Privata:** L'app non richiede permessi di rete, non utilizza cookie di terze parti e non effettua alcun tracciamento. I tuoi dati rimangono esclusivamente sul tuo dispositivo.
* **Zero Pubblicità, per sempre:** Non sono presenti banner, pop-up o inserzioni pubblicitarie di alcun tipo.
* **Backup Trasparenti:** In linea con la filosofia open, il sistema di salvataggio permette di esportare un backup completo dei propri dati sotto forma di file **JSON locale**. I file non sono criptati e risultano completamente leggibili, garantendo la massima trasparenza su quali dati l'app sta effettivamente salvando.

## 🛠️ Architettura Tecnica
Dietro la semplicità dell'interfaccia si nasconde un'architettura Android moderna e solida:
* **Linguaggio:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) nativo (No XML).
* **Persistenza:** DataStore e Gson per una gestione snella e affidabile dello stato.
* **Filesystem:** Storage Access Framework (SAF) per l'esportazione sicura dei backup JSON.

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
