[🇮🇹 Leggi in Italiano](README-it.md) 

# 🎮 ScoreCounter

> A universal, transparent score counter in perfect Material You style.

ScoreCounter was born with a clear philosophy: to offer a universal, quick, and fuss-free tool to track scores for any board game, card tournament, or friendly challenge. The goal is to let you focus on the game, delegating calculations to an interface designed to be immediate and intuitive.

Fully developed following the **Material Design 3 (Material You)** guidelines, the app dynamically adapts to your device's system colors, ensuring a consistent, modern visual experience enriched with haptic feedback (vibrations) for every interaction.

## 🎯 Game Experience
The app avoids unnecessary options that might distract from the game, offering exactly what you need when you need it:
- **Flexible Management:** Whether it is a two-player game or a group tournament, you can add an unlimited number of players. You can instantly recall players already saved in your contacts or create new ones on the spot, assigning scores without any numerical limit.
- **Tabletop Tools:** During the game, you have essential and non-intrusive tools at your disposal, such as a customizable timer to manage turns and a virtual die (also configurable) to resolve tie-breakers or decide who starts.
- **Everything Under Control:** The main interface is designed to allow quick and safe taps, minimizing typing errors during the most hectic phases.

## 📊 Analysis and Sharing
At the end of the game, ScoreCounter transforms simple numbers into a true match report:
- **Interactive Charts:** Analyze the game's progress through a detailed vector chart. The temporal zoom feature allows you to enlarge and analyze specific quarters of the game to study comebacks and overtaking.
- **Awards Assignment:** The internal engine mathematically evaluates the match and automatically awards fun titles based on actual performance (e.g., the highest hit, positive streak, biggest comeback).
- **Global Stats:** A dedicated section allows you to consult the history of all past games and view general statistics accumulated over time.
- **Sharing:** Final results, awards, and match progress can be easily shared with friends at the end of the challenge.

## 🛡️ Ethics, Privacy, and Transparency
ScoreCounter is built around absolute respect for the user.
- **100% Offline and Private:** The app does not require network permissions, does not use third-party cookies, and does not perform any tracking. Your data stays exclusively on your device.
- **Zero Ads, forever:** There are no banners, pop-ups, or ads of any kind.
- **Transparent Backups:** In line with the open philosophy, the save system allows you to export a complete backup of your data as a local **JSON file**. The files are unencrypted and fully readable, ensuring maximum transparency on what data the app is actually saving.

## 🛠️ Technical Architecture
Behind the simplicity of the interface lies a modern and solid Android architecture:
- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI:** Native [Jetpack Compose](https://developer.android.com/jetpack/compose) (No XML).
- **Persistence:** DataStore and Gson for a lean and reliable state management.
- **Filesystem:** Storage Access Framework (SAF) for the secure export of JSON backups.

## 📸 Application Gallery

### 🏠 1. Home and History
*The command center: start quick challenges with a tap or relive past glories by exploring your personal Hall of Fame.*
<table>
  <tr>
    <td align="center">
      <b>Home page with history</b><br>
      <img src="https://github.com/user-attachments/assets/cd9057bb-efcb-414f-966a-9e37c65829f7" width="250" alt="Home Screen with quick challenges"/>
    </td>
     <td align="center">
      <b>History for each game</b><br>
      <img src="https://github.com/user-attachments/assets/7a0413c3-8c18-44c7-b6cf-585b07665362" width="250" alt="Game history and absolute records"/>
    </td>
    <td align="center">
      <b>Global stats</b><br>
      <img src="https://github.com/user-attachments/assets/93c887f7-6208-4e8b-a6e9-89f9de038c69" width="250" alt="Global stats and records"/>
    </td>
  </tr>
</table>

### ⚙️ 2. Creating a challenge
*Add the challenge name, set your rules, add players and customize their colors.*
<table>
  <tr>
    <td align="center">
      <b>Configure the match</b><br>
      <img src="https://github.com/user-attachments/assets/1c6c53cb-6970-4330-a173-8cca3ec6de0c" width="250" alt="Match creation screen"/>
    </td>
    <td align="center">
      <b>Customize players</b><br>
      <img src="https://github.com/user-attachments/assets/3ab525cf-1d63-4a7a-96bf-a5cc16bf1d29" width="250" alt="Player customization"/>
    </td>
    <td align="center">
      <b>Manage favorites</b><br>
      <img src="https://github.com/user-attachments/assets/cf836352-8d5d-4357-b00d-951c03e24f72" width="250" alt="Favorite players management panel"/>
    </td>
  </tr>
</table>

### 🎲 3. The challenge (counter)
*"Table-First" design to never lose focus: manage points with ergonomics and use integrated tools.*
<table>
  <tr>
    <td align="center">
      <b>Keep track of points</b><br>
      <img src="https://github.com/user-attachments/assets/d4042687-c7ca-4792-8352-abb2a27ffe4c" width="250" alt="Match in progress with On Fire effect"/>
    </td>
    <td align="center">
      <b>Manage game times</b><br>
      <img src="https://github.com/user-attachments/assets/ca8f31bb-49e9-45f7-a476-7bca8d4cb653" width="250" alt="Timer setup" />
    </td>
  <td align="center">
      <b>Resolve disputes</b><br>
      <img src="https://github.com/user-attachments/assets/1c88bbe4-e734-4e55-ad4a-4a4c03d19b09" width="250" alt="Virtual die pop-up" />
    </td>
  </tr>
</table>

### 🏆 4. Results and analysis
*Much more than a simple score: discover special awards, analyze progress on the temporal chart and share the outcome.*
<table>
  <tr>
    <td align="center">
      <b>Analyze the match chart</b><br>
      <img src="https://github.com/user-attachments/assets/b2a1f603-88b2-444a-a206-5ec46777c449" width="250" alt="Final standings with match chart"/>
    </td>
    <td align="center">
      <b>Discover awarded medals</b><br>
      <img src="https://github.com/user-attachments/assets/9b057635-275f-43ff-b18a-307d49fb62a3" width="250" alt="Special awards and medals"/>
    </td>
  <td align="center">
      <b>Understand awards</b><br>
      <img src="https://github.com/user-attachments/assets/cc8ef7f8-f9ba-4865-95cd-178c60d16221" width="250" alt="Detail of awarded prizes"/>
    </td>
  </tr>
</table>

### 🖥️ 5. Landscape and tablet layout
*Fully adaptive interface leveraging grids and split-screen to maximize space in landscape mode.*
<table>
  <tr>
    <td align="center">
      <b>Home in landscape</b><br>
      <img src="https://github.com/user-attachments/assets/dc987757-c633-4e29-90f6-ab34a87d1f6a" width="300" alt="Home in landscape"/>
    </td>
    <td align="center">
      <b>Counter in landscape</b><br>
      <img src="https://github.com/user-attachments/assets/c0c63d81-5334-46f1-bbf6-c1bc380e7460" width="300" alt="Challenge in landscape"/>
    </td>
  <td align="center">
      <b>Expanded chart</b><br>
      <img src="https://github.com/user-attachments/assets/786c89bc-74ca-42b8-b25d-5e46124987a0" width="300" alt="Results in landscape"/>
    </td>
  </tr>
</table>

<br>
<p align="center">© 2026 Created by NicolA380</p>
