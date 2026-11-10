package com.n380.scorecounter.ui.screens

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n380.scorecounter.R
import com.n380.scorecounter.ui.components.AutoResizedText
import com.n380.scorecounter.viewmodel.MatchViewModel

/**
 * ====================================================================
 * SCHERMATA IMPOSTAZIONI E PREFERENZE (SettingsScreen)
 * ====================================================================
 * 🧠 ARCHITETTURA E PATTERN COMPOSE:
 * Questa schermata dedicata (non-dialog) è integrata direttamente nel NavHost
 * con animazioni fluide di transizione slide da destra a sinistra e ritorno.
 *
 * Principi applicati:
 * 1. DESIGN SYSTEM COERENTE: Utilizza lo sfondo trasparente dello Scaffold per
 *    mostrare il pattern grafico globale dell'app presente in MainActivity.
 * 2. SCHELETRO MODULARE (UI Skeleton): Prepara ordinatamente tutte le sezioni
 *    e le opzioni richieste con stati reattivi locali (remember), predisponendo
 *    l'infrastruttura per il futuro salvataggio persistente su DataStore.
 * 3. TIPOGRAFIA ADATTIVA: Impiega [AutoResizedText] per garantire che titoli,
 *    descrizioni ed etichette scalino fluidamente senza troncamenti indesiderati.
 * 4. FEEDBACK TATTILE: Ogni interazione utente (tap, selezione, toggle) attiva
 *    il feedback aptico coerente con gli standard dell'applicazione.
 * 5. ERGONOMIA E DOCK INFERIORE: Presenta sia la freccia indietro in testata
 *    sia un pulsante di chiusura ancorato alla base della schermata (bottomBar).
 * ====================================================================
 */
@Composable
fun SettingsScreen(
    viewModel: MatchViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Prevenzione del doppio click accidentale sul pulsante di chiusura
    var isClosing by remember { mutableStateOf(false) }

    // ====================================================================
    // 🧠 GESTIONE REATTIVA PREFERENZE & STATI LOCALI
    // ====================================================================
    // Le seguenti preferenze sono collegate in tempo reale al MatchViewModel e al DataStore:
    // - Tema app (`viewModel.appTheme`: 0 = Sistema, 1 = Scuro, 2 = Chiaro) con Material You nativo
    // - Lingua app (`viewModel.appLanguage` sincronizzata con `LocaleManager`)
    // - Blocco orientamento (`viewModel.appOrientation` sincronizzata con `Activity.requestedOrientation`)
    // - Schermo sempre attivo (`viewModel.keepScreenOnMode`: 0 = Solo in partita, 1 = Sempre, 2 = Disattivato)
    // Le restanti opzioni (conferma azioni, passo punteggio, riordino dinamico) restano gestite
    // provvisoriamente con stati locali 'remember', pronte per i prossimi step.

    // --- SEZIONE A: INTERFACCIA E ASPETTO ---
    // Tema app: tendina a discesa
    var showThemeDropdown by remember { mutableStateOf(false) }

    // Lingua app: tendina a discesa
    var showLanguageDropdown by remember { mutableStateOf(false) }

    // Blocco orientamento: tendina a discesa
    var showOrientationDropdown by remember { mutableStateOf(false) }

    // --- SEZIONE B: SCHERMO E COMPORTAMENTO ---
    // Schermo sempre attivo: tendina a discesa
    var showScreenKeepOnDropdown by remember { mutableStateOf(false) }

    // Conferma azioni rapide ("Azzera" / "Concludi"): default true
    var confirmQuickActions by remember { mutableStateOf(true) }

    // --- SEZIONE C: PERSONALIZZAZIONE GIOCO ---
    // Passo predefinito punteggio: ±1, ±5, ±10, ±20 (default 1)
    var selectedScoreStep by remember { mutableIntStateOf(1) }

    // Ordinamento dinamico classifica: default false
    var dynamicLeaderboardSorting by remember { mutableStateOf(false) }

    // ====================================================================
    // SCAFFOLD PRINCIPALE CON SFONDO TRASPARENTE
    // ====================================================================
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .displayCutoutPadding(),
        containerColor = Color.Transparent,

        // ====================================================================
        // DOCK INFERIORE ANCORATO (Pulsante Chiudi ad alto impatto visivo)
        // ====================================================================
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = if (isLandscape) 10.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (!isClosing) {
                                isClosing = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .height(if (isLandscape) 52.dp else 64.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.btn_chiudi_impostazioni),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                        )
                        AutoResizedText(
                            text = stringResource(R.string.btn_chiudi_impostazioni),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==============================================================
            // TESTATA SUPERIORE FISSA: FRECCIA INDIETRO + TITOLO SCREEN
            // ==============================================================
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pillola / Tasto indietro con ripple e feedback tattile
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (!isClosing) {
                                isClosing = true
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_chiudi_impostazioni),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AutoResizedText(
                        text = stringResource(R.string.titolo_impostazioni_esteso),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AutoResizedText(
                        text = stringResource(R.string.desc_impostazioni_menu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // ==============================================================
            // AREA SCORREVOLE DELLE PREFERENZE
            // ==============================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ------------------------------------------------------------
                // SEZIONE A: 📱 INTERFACCIA E ASPETTO
                // ------------------------------------------------------------
                SettingsSectionCard(
                    title = stringResource(R.string.sez_interfaccia_aspetto),
                    icon = Icons.Filled.Palette
                ) {
                    // 1. TEMA APP (Sistema | Scuro | Chiaro)
                    // 🧠 ARCHITETTURA E DIDATTICA:
                    // - 0 = Sistema (segue l'impostazione chiara o scura di Android)
                    // - 1 = Scuro (forza il tema scuro con Material You nativo - dynamicDarkColorScheme)
                    // - 2 = Chiaro (forza il tema chiaro con Material You nativo - dynamicLightColorScheme)
                    // Non applichiamo un nero assoluto o bianco statico: manteniamo le sfumature dinamiche
                    // estratte dallo sfondo del dispositivo (Dynamic Colors di Android 12+ / 14+),
                    // garantendo l'eleganza grafica e il rispetto del Material Design.
                    val themeLabels = listOf(
                        stringResource(R.string.opt_tema_sistema) to Icons.Filled.BrightnessAuto,
                        stringResource(R.string.opt_tema_scuro) to Icons.Filled.DarkMode,
                        stringResource(R.string.opt_tema_chiaro) to Icons.Filled.LightMode
                    )
                    val safeThemeIndex = viewModel.appTheme.coerceIn(0, themeLabels.size - 1)

                    SettingsDropdownItem(
                        title = stringResource(R.string.opt_tema_app),
                        subtitle = stringResource(R.string.desc_tema_app),
                        currentSelectionText = themeLabels[safeThemeIndex].first,
                        currentSelectionIcon = themeLabels[safeThemeIndex].second,
                        isExpanded = showThemeDropdown,
                        onExpandRequest = { showThemeDropdown = true },
                        onDismissRequest = { showThemeDropdown = false }
                    ) {
                        themeLabels.forEachIndexed { index, (label, icon) ->
                            val isSelected = safeThemeIndex == index
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    // 🧠 DIDATTICA: Feedback tattile e salvataggio istantaneo nel ViewModel
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showThemeDropdown = false
                                    viewModel.setAppThemePreference(index)
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // 2. LINGUA APP (7 lingue supportate + Lingua di sistema)
                    // 🧠 ARCHITETTURA E DIDATTICA (Per-App Language API - Android 13+ / API 33+):
                    // L'elenco include le 7 lingue con i rispettivi codici ISO e bandiere.
                    // Se l'utente non ha impostato alcuna preferenza ("system"), rileviamo la lingua
                    // attualmente attiva a livello di sistema operativo tramite Locale.getDefault().language.
                    val supportedLanguages = listOf(
                        Triple("it", "Italiano", "🇮🇹"),
                        Triple("en", "English", "🇬🇧"),
                        Triple("de", "Deutsch", "🇩🇪"),
                        Triple("es", "Español", "🇪🇸"),
                        Triple("fr", "Français", "🇫🇷"),
                        Triple("hi", "हिन्दी", "🇮🇳"),
                        Triple("ja", "日本語", "🇯🇵")
                    )

                    // Risoluzione reattiva della lingua corrente:
                    // 🧠 DIDATTICA: Utilizziamo `configuration.locales[0].language` che osserva lo stato di Compose,
                    // invece del non-osservabile `Locale.getDefault()`.
                    val currentSystemLang = configuration.locales[0]?.language ?: "it"
                    val effectiveLangCode = if (viewModel.appLanguage == "system") {
                        if (supportedLanguages.any { it.first == currentSystemLang }) currentSystemLang else "it"
                    } else {
                        viewModel.appLanguage
                    }

                    val currentLang = supportedLanguages.firstOrNull { it.first == effectiveLangCode }
                        ?: supportedLanguages[0]

                    SettingsDropdownItem(
                        title = stringResource(R.string.opt_lingua_app),
                        subtitle = stringResource(R.string.desc_lingua_app),
                        currentSelectionText = "${currentLang.second} ${currentLang.third}",
                        currentSelectionIcon = Icons.Filled.Language,
                        isExpanded = showLanguageDropdown,
                        onExpandRequest = { showLanguageDropdown = true },
                        onDismissRequest = { showLanguageDropdown = false }
                    ) {
                        supportedLanguages.forEach { (code, name, flag) ->
                            val isSelected = effectiveLangCode == code
                            DropdownMenuItem(
                                leadingIcon = {
                                    Text(
                                        text = flag,
                                        fontSize = 18.sp
                                    )
                                },
                                text = {
                                    Text(
                                        text = name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    // 🧠 DIDATTICA: Feedback aptico alla selezione
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showLanguageDropdown = false

                                    // 1. Salvataggio della preferenza nel ViewModel e su DataStore
                                    viewModel.setAppLanguagePreference(code)

                                    // 2. Applicazione in tempo reale della lingua tramite LocaleManager (API 33+)
                                    // minSdk è 34, pertanto LocaleManager è sempre disponibile sul dispositivo.
                                    val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                                    localeManager?.applicationLocales = LocaleList.forLanguageTags(code)
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // 3. BLOCCO ORIENTAMENTO (Auto | Orizzontale | Verticale)
                    // 🧠 ARCHITETTURA E DIDATTICA:
                    // - 0: Auto (segue il sensore del giroscopio e la rotazione di Android)
                    // - 1: Orizzontale (forza la schermata in Landscape)
                    // - 2: Verticale (forza la schermata in Portrait)
                    // Modificando `viewModel.setAppOrientationPreference(index)`:
                    // 1. Viene aggiornato il DataStore (persistenza al riavvio).
                    // 2. Il LaunchedEffect in MainActivity reagisce istantaneamente impostando `requestedOrientation`.
                    val orientationOptions = listOf(
                        stringResource(R.string.opt_orientamento_auto) to Icons.Filled.ScreenRotation,
                        stringResource(R.string.opt_orientamento_orizzontale) to Icons.Filled.StayCurrentLandscape,
                        stringResource(R.string.opt_orientamento_verticale) to Icons.Filled.StayCurrentPortrait
                    )
                    val safeOrientationIndex = viewModel.appOrientation.coerceIn(0, orientationOptions.size - 1)

                    SettingsDropdownItem(
                        title = stringResource(R.string.opt_orientamento_schermo),
                        subtitle = stringResource(R.string.desc_orientamento_schermo),
                        currentSelectionText = orientationOptions[safeOrientationIndex].first,
                        currentSelectionIcon = orientationOptions[safeOrientationIndex].second,
                        isExpanded = showOrientationDropdown,
                        onExpandRequest = { showOrientationDropdown = true },
                        onDismissRequest = { showOrientationDropdown = false }
                    ) {
                        orientationOptions.forEachIndexed { index, (label, icon) ->
                            val isSelected = safeOrientationIndex == index
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    // 🧠 DIDATTICA: Feedback tattile e aggiornamento istantaneo dell'orientamento
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showOrientationDropdown = false
                                    viewModel.setAppOrientationPreference(index)
                                }
                            )
                        }
                    }
                }

                // ------------------------------------------------------------
                // SEZIONE B: 💡 SCHERMO E COMPORTAMENTO
                // ------------------------------------------------------------
                SettingsSectionCard(
                    title = stringResource(R.string.sez_schermo_comportamento),
                    icon = Icons.Filled.WbSunny
                ) {
                    // 1. SCHERMO SEMPRE ATTIVO (Solo in partita | Sempre attivo | Disattivato)
                    // 🧠 ARCHITETTURA E DIDATTICA:
                    // - 0 = Solo in partita: lo schermo rimane acceso in CounterScreen durante il gioco
                    // - 1 = Sempre attivo: FLAG_KEEP_SCREEN_ON applicata a tutta l'app
                    // - 2 = Disattivato: il display segue le normali regole di spegnimento/timeout del sistema
                    val screenKeepOnOptions = listOf(
                        stringResource(R.string.opt_schermo_solo_partita) to Icons.Filled.SportsEsports,
                        stringResource(R.string.opt_schermo_sempre) to Icons.Filled.Visibility,
                        stringResource(R.string.opt_schermo_disattivato) to Icons.Filled.Close
                    )
                    val safeKeepOnIndex = viewModel.keepScreenOnMode.coerceIn(0, screenKeepOnOptions.size - 1)

                    SettingsDropdownItem(
                        title = stringResource(R.string.opt_schermo_attivo),
                        subtitle = stringResource(R.string.desc_schermo_attivo),
                        currentSelectionText = screenKeepOnOptions[safeKeepOnIndex].first,
                        currentSelectionIcon = screenKeepOnOptions[safeKeepOnIndex].second,
                        isExpanded = showScreenKeepOnDropdown,
                        onExpandRequest = { showScreenKeepOnDropdown = true },
                        onDismissRequest = { showScreenKeepOnDropdown = false }
                    ) {
                        screenKeepOnOptions.forEachIndexed { index, (label, icon) ->
                            val isSelected = safeKeepOnIndex == index
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    // 🧠 DIDATTICA: Feedback aptico e aggiornamento istantaneo
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    showScreenKeepOnDropdown = false
                                    viewModel.setKeepScreenOnPreference(index)
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // 2. CONFERMA AZIONI RAPIDE (Switch Toggle)
                    SettingsSwitchItem(
                        title = stringResource(R.string.opt_conferma_azioni),
                        subtitle = stringResource(R.string.desc_conferma_azioni),
                        isChecked = confirmQuickActions,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            confirmQuickActions = checked
                        }
                    )
                }

                // ------------------------------------------------------------
                // SEZIONE C: 🎮 PERSONALIZZAZIONE GIOCO
                // ------------------------------------------------------------
                SettingsSectionCard(
                    title = stringResource(R.string.sez_personalizzazione_gioco),
                    icon = Icons.Filled.SportsEsports
                ) {
                    // 1. PASSO PREDEFINITO PUNTEGGIO (±1 | ±5 | ±10 | ±20)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoResizedText(
                            text = stringResource(R.string.opt_passo_punteggio),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AutoResizedText(
                            text = stringResource(R.string.desc_passo_punteggio),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        // Riga di FilterChips per la selezione rapida del passo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val steps = listOf(1, 5, 10, 20)
                            steps.forEach { step ->
                                val isSelected = selectedScoreStep == step
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        selectedScoreStep = step
                                    },
                                    label = {
                                        Text(
                                            text = "±$step",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // 2. ORDINAMENTO DINAMICO CLASSIFICA (Switch Toggle)
                    SettingsSwitchItem(
                        title = stringResource(R.string.opt_ordinamento_classifica),
                        subtitle = stringResource(R.string.desc_ordinamento_classifica),
                        isChecked = dynamicLeaderboardSorting,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            dynamicLeaderboardSorting = checked
                        }
                    )
                }

                // Margine inferiore per non far impattare l'ultima card col dock
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE SCHEDA SEZIONE IMPOSTAZIONI
 * ====================================================================
 * Raggruppa visivamente le opzioni in un Card M3 con containerColor coerente.
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Intestazione sezione con icona tematica
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AutoResizedText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contenuto delle opzioni
            content()
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE VOCE IMPOSTAZIONI CON TENDINA A DISCESA (Dropdown)
 * ====================================================================
 */
@Composable
private fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    currentSelectionText: String,
    currentSelectionIcon: ImageVector,
    isExpanded: Boolean,
    onExpandRequest: () -> Unit,
    onDismissRequest: () -> Unit,
    dropdownContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandRequest() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Colonna con Titolo e Sottotitolo descrittivo
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            AutoResizedText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AutoResizedText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // Pillola interattiva con valore corrente e freccia a tendina
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier.clickable { onExpandRequest() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = currentSelectionIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = currentSelectionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = onDismissRequest,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                dropdownContent()
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTE VOCE IMPOSTAZIONI CON INTERRUTTORE (Switch)
 * ====================================================================
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            AutoResizedText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AutoResizedText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}
